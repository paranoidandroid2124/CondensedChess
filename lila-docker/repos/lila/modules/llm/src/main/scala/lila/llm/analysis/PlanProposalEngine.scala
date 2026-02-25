package lila.llm.analysis

import chess.*
import chess.Bitboard
import chess.format.Fen
import lila.llm.model.authoring.*

/**
 * Plan-first proposal layer:
 * - Propose strategic hypotheses from static structure + seed library.
 * - Engine-derived hypotheses are merged as supporting evidence, not the entry point.
 */
object PlanProposalEngine:

  private val DefaultMaxItems = 3

  def propose(
      fen: String,
      ply: Int,
      ctx: IntegratedContext,
      maxItems: Int = DefaultMaxItems
  ): List[PlanHypothesis] =
    val features = ctx.features.orElse(PositionAnalyzer.extractFeatures(fen, ply.max(1)))
    val state = PositionAnalyzer.extractStrategicState(fen).getOrElse(StrategicStateFeatures.empty)
    val us = if ctx.isWhiteToMove then Color.White else Color.Black
    val them = !us

    val structural = structuralProposals(state, features, us)
    val seeded = seedProposals(fen, features, ctx, us, them)
    val combined = mergeDuplicates(structural ++ seeded)

    combined.take(maxItems).zipWithIndex.map { case (h, idx) =>
      h.copy(rank = idx + 1)
    }

  def mergePlanFirstWithEngine(
      planFirst: List[PlanHypothesis],
      engineHypotheses: List[PlanHypothesis],
      maxItems: Int = DefaultMaxItems
  ): List[PlanHypothesis] =
    val merged = scala.collection.mutable.LinkedHashMap.empty[String, PlanHypothesis]

    planFirst
      .sortBy(h => -h.score)
      .foreach { h =>
        val withSource =
          h.copy(evidenceSources = (h.evidenceSources :+ "proposal:plan_first").distinct)
        merged.update(keyOf(withSource), withSource)
      }

    engineHypotheses
      .sortBy(h => -h.score)
      .foreach { h =>
        val key = keyOf(h)
        merged.get(key) match
          case Some(prev) =>
            val mergedScore = prev.score.max((prev.score * 0.7) + (h.score * 0.3))
            merged.update(
              key,
              prev.copy(
                score = mergedScore,
                viability = toViability(mergedScore, prev.viability.risk),
                preconditions = (prev.preconditions ++ h.preconditions).distinct.take(5),
                executionSteps = (prev.executionSteps ++ h.executionSteps).distinct.take(5),
                failureModes = (prev.failureModes ++ h.failureModes).distinct.take(4),
                refutation = prev.refutation.orElse(h.refutation),
                evidenceSources =
                  (prev.evidenceSources ++ h.evidenceSources ++ List("support:engine_hypothesis")).distinct
              )
            )
          case None =>
            val withSource =
              h.copy(evidenceSources = (h.evidenceSources :+ "support:engine_hypothesis").distinct)
            merged.update(key, withSource)
      }

    val result = merged.values.toList.sortBy(h => -h.score).take(maxItems)
    result.zipWithIndex.map { case (h, idx) => h.copy(rank = idx + 1) }

  private def structuralProposals(
      state: StrategicStateFeatures,
      features: Option[PositionFeatures],
      us: Color
  ): List[PlanHypothesis] =
    val entrenched =
      if us.white then state.whiteEntrenchedPieces else state.blackEntrenchedPieces
    val rookPawnReady =
      if us.white then state.whiteRookPawnMarchReady else state.blackRookPawnMarchReady
    val hookChance =
      if us.white then state.whiteHookCreationChance else state.blackHookCreationChance
    val clamp =
      if us.white then state.whiteColorComplexClamp else state.blackColorComplexClamp

    val out = scala.collection.mutable.ListBuffer.empty[PlanHypothesis]

    if entrenched > 0 then
      val score = (0.58 + math.min(0.22, entrenched * 0.08)).min(0.9)
      out += PlanHypothesis(
        planId = "MinorPieceManeuver",
        planName = "Entrench a minor piece on a stable outpost",
        rank = 0,
        score = score,
        preconditions = List(
          s"entrenched candidates: $entrenched",
          "support network exists for a durable outpost"
        ),
        executionSteps = List(
          "reinforce the outpost square with pawn and piece support",
          "avoid exchanges that release the entrenched piece pressure"
        ),
        failureModes = List(
          "opponent can challenge the outpost with timely pawn breaks",
          "support pieces become overloaded"
        ),
        viability = toViability(score, "outpost can collapse under pawn lever timing"),
        refutation = Some("if center opens immediately, outpost plan may be too slow"),
        evidenceSources = List("structural_state:entrenched_piece")
      )

    if rookPawnReady then
      val base = if hookChance then 0.76 else 0.67
      out += PlanHypothesis(
        planId = "PawnStorm",
        planName = "Rook-pawn march to gain flank space",
        rank = 0,
        score = base,
        preconditions = List(
          "rook-pawn file is available for expansion",
          "king/flank alignment supports pawn advance"
        ),
        executionSteps = List(
          "advance a/h-pawn to claim space and create hooks",
          "coordinate heavy pieces behind the pawn front"
        ),
        failureModes = List(
          "center breaks punish flank overextension",
          "king safety debt becomes too high"
        ),
        viability = toViability(base, "flank expansion can backfire if center is unstable"),
        refutation = Some("if tactical threats appear in center, postpone pawn march"),
        evidenceSources = List("structural_state:rook_pawn_march")
      )

    if hookChance then
      val score = if rookPawnReady then 0.74 else 0.63
      out += PlanHypothesis(
        planId = "KingsideAttack",
        planName = "Attack the hook and force structural concessions",
        rank = 0,
        score = score,
        preconditions = List(
          "hook contact exists on flank pawn chain",
          "enough attacking pieces can join within two moves"
        ),
        executionSteps = List(
          "fix the hook square and increase piece pressure",
          "open files/diagonals behind the hook"
        ),
        failureModes = List(
          "insufficient attacking mass",
          "counterplay arrives faster on opposite wing"
        ),
        viability = toViability(score, "requires coordinated piece arrival to convert"),
        refutation = Some("if opponent neutralizes hook contact, switch to central plan"),
        evidenceSources = List("structural_state:hook_creation")
      )

    if clamp then
      val clampScore = 0.69
      out += PlanHypothesis(
        planId = "Prophylaxis",
        planName = "Maintain color-complex clamp and restrict counterplay",
        rank = 0,
        score = clampScore,
        preconditions = List(
          "dominant control over key color-complex around enemy king",
          "restriction can be maintained without tactical concessions"
        ),
        executionSteps = List(
          "improve worst-placed piece while preserving clamp squares",
          "trade favorably into structures where clamp remains"
        ),
        failureModes = List(
          "forced simplification dissolves color-complex pressure",
          "defensive resources break the clamp geometry"
        ),
        viability = toViability(clampScore, "clamp may dissipate after major-piece trades"),
        refutation = Some("if clamp squares are no longer contested, transition plan"),
        evidenceSources = List("structural_state:color_complex_clamp")
      )

    if out.isEmpty then
      val genericScore = features
        .map { f =>
          val center = if f.centralSpace.lockedCenter then 0.63 else if f.centralSpace.openCenter then 0.54 else 0.58
          center
        }
        .getOrElse(0.55)
      out += PlanHypothesis(
        planId = "CentralControl",
        planName = "Improve central coordination before commitment",
        rank = 0,
        score = genericScore,
        preconditions = List("no immediate forcing tactic dominates"),
        executionSteps = List(
          "improve central piece placement",
          "prepare flexible pawn break only after coordination"
        ),
        failureModes = List("slow moves allow opponent to seize initiative"),
        viability = toViability(genericScore, "generic plan can lose race to forcing lines"),
        refutation = None,
        evidenceSources = List("structural_state:generic_center_plan")
      )

    out.toList

  private def seedProposals(
      fen: String,
      features: Option[PositionFeatures],
      ctx: IntegratedContext,
      us: Color,
      them: Color
  ): List[PlanHypothesis] =
    Fen
      .read(chess.variant.Standard, Fen.Full(fen))
      .toList
      .flatMap { pos =>
        LatentSeedLibrary.all.flatMap { seed =>
          val moves = SeedMoveGenerator.generate(seed, pos, us)
          if moves.isEmpty then Nil
          else if !requiredPreconditionsMet(seed, pos.board, features, ctx, us, them) then Nil
          else
            val score = seedScore(seed, moves, ctx)
            val steps = moves.take(3).map(movePatternText)
            val preconds = seed.preconditions.take(3).map(pc => preconditionText(pc.condition))
            val failures =
              (seed.typicalCounters.take(2).map(counterText) :+ "fails if tactical refutation appears first").distinct
            List(
              PlanHypothesis(
                planId = seed.mapsToPlan.map(_.toString).getOrElse(seed.id),
                planName = humanizeSeedId(seed.id),
                rank = 0,
                score = score,
                preconditions = preconds,
                executionSteps = if steps.nonEmpty then steps else List("prepare piece coordination first"),
                failureModes = failures,
                viability = toViability(score, "seed idea needs proof against best defense"),
                refutation = Some("requires probe validation before being asserted"),
                evidenceSources = List(s"latent_seed:${seed.id}", "proposal:plan_first")
              )
            )
        }
      }

  private def requiredPreconditionsMet(
      seed: LatentSeed,
      board: Board,
      features: Option[PositionFeatures],
      ctx: IntegratedContext,
      us: Color,
      them: Color
  ): Boolean =
    seed.preconditions.filter(_.required).forall { wp =>
      wp.condition match
        case Precondition.KingPosition(owner, flank) =>
          val color = if owner == SideRef.Us then us else them
          flankOfKing(board, color) == flank
        case Precondition.CenterStateIs(state) =>
          features.exists(f => centerState(f.centralSpace) == state)
        case Precondition.SpaceAdvantage(flank, min) =>
          val diff = spaceScore(board, us, flank) - spaceScore(board, them, flank)
          diff >= min
        case Precondition.FileStatusIs(file, status) =>
          fileStatus(board, file) == status
        case Precondition.PawnStructureIs(tpe) =>
          matchesStructureType(tpe, board, features, us, them)
        case Precondition.NoImmediateDefensiveTask(maxThreatCp) =>
          ctx.maxThreatLossToUs <= maxThreatCp
        case Precondition.PieceRouteExists(_, _, _) =>
          true
    }

  private def matchesStructureType(
      tpe: StructureType,
      board: Board,
      features: Option[PositionFeatures],
      us: Color,
      them: Color
  ): Boolean =
    tpe match
      case StructureType.IQP =>
        features.exists(f => if us.white then f.pawns.whiteIQP else f.pawns.blackIQP)
      case StructureType.HangingPawns =>
        features.exists(f => if us.white then f.pawns.whiteHangingPawns else f.pawns.blackHangingPawns)
      case StructureType.CarlsbadLike =>
        isCarlsbadLike(board)
      case StructureType.FianchettoShell =>
        hasFianchettoShell(board, them)
      case StructureType.MaroczyBindLike =>
        isMaroczyBindLike(board, us)

  private def seedScore(seed: LatentSeed, moves: List[MovePattern], ctx: IntegratedContext): Double =
    val requiredCount = seed.preconditions.count(_.required).max(1)
    val optionalCount = seed.preconditions.count(!_.required)
    val base = 0.42 + (requiredCount * 0.06) + (optionalCount * 0.03) + math.min(0.18, moves.size * 0.04)
    val pressurePenalty =
      if ctx.tacticalThreatToUs then 0.18
      else if ctx.strategicThreatToUs then 0.1
      else 0.0
    (base - pressurePenalty).max(0.25).min(0.9)

  private def mergeDuplicates(items: List[PlanHypothesis]): List[PlanHypothesis] =
    items
      .groupBy(keyOf)
      .values
      .toList
      .map { group =>
        group.reduce { (a, b) =>
          val chosen = if a.score >= b.score then a else b
          chosen.copy(
            preconditions = (a.preconditions ++ b.preconditions).distinct.take(6),
            executionSteps = (a.executionSteps ++ b.executionSteps).distinct.take(6),
            failureModes = (a.failureModes ++ b.failureModes).distinct.take(5),
            evidenceSources = (a.evidenceSources ++ b.evidenceSources).distinct
          )
        }
      }
      .sortBy(h => -h.score)

  private def keyOf(h: PlanHypothesis): String =
    s"${h.planId.toLowerCase}|${h.planName.toLowerCase}"

  private def toViability(score: Double, risk: String): PlanViability =
    val clipped = score.max(0.0).min(1.0)
    val label =
      if clipped >= 0.7 then "high"
      else if clipped >= 0.45 then "medium"
      else "low"
    PlanViability(score = clipped, label = label, risk = risk)

  private def humanizeSeedId(id: String): String =
    id
      .replace("_", " ")
      .replace("  ", " ")
      .trim

  private def movePatternText(p: MovePattern): String =
    p match
      case MovePattern.PawnAdvance(file) =>
        s"advance $file-pawn to gain space"
      case MovePattern.PawnLever(from, to) =>
        s"create a pawn lever from $from toward $to"
      case MovePattern.PieceTo(role, square) =>
        s"reroute $role to ${square.key}"
      case MovePattern.PieceManeuver(role, target, via) =>
        if via.nonEmpty then s"maneuver $role to ${target.key} via ${via.map(_.key).mkString(" -> ")}"
        else s"maneuver $role to ${target.key}"
      case MovePattern.Castle =>
        "castle to improve king safety"
      case MovePattern.Exchange(role, on) =>
        s"exchange $role on ${on.key} under favorable terms"
      case MovePattern.BatteryFormation(front, back, _) =>
        s"form a $front-$back battery on the critical line"

  private def counterText(counter: CounterPattern): String =
    counter match
      case CounterPattern.PawnPushBlock(file) =>
        s"opponent blocks with ...$file-pawn push"
      case CounterPattern.PieceControl(role, square) =>
        s"opponent contests ${square.key} with $role"
      case CounterPattern.CentralStrike =>
        "opponent hits the center before plan matures"
      case CounterPattern.Counterplay(flank) =>
        s"opponent creates faster counterplay on $flank"

  private def preconditionText(pc: Precondition): String =
    pc match
      case Precondition.KingPosition(owner, flank) =>
        s"${owner.toString.toLowerCase} king favors $flank operations"
      case Precondition.CenterStateIs(state) =>
        s"center state is $state"
      case Precondition.SpaceAdvantage(flank, min) =>
        s"space edge on $flank (>= $min)"
      case Precondition.FileStatusIs(file, status) =>
        s"$file-file is $status"
      case Precondition.PieceRouteExists(role, from, to) =>
        s"$role route from ${from.key} to ${to.key} exists"
      case Precondition.PawnStructureIs(tpe) =>
        s"structure resembles $tpe"
      case Precondition.NoImmediateDefensiveTask(maxThreatCp) =>
        s"no immediate defensive emergency (> $maxThreatCp cp)"

  private def flankOfKing(board: Board, color: Color): Flank =
    board.kingPosOf(color) match
      case None => Flank.Center
      case Some(sq) =>
        sq.file match
          case File.A | File.B | File.C => Flank.Queenside
          case File.F | File.G | File.H => Flank.Kingside
          case _                        => Flank.Center

  private def centerState(cs: CentralSpaceFeatures): CenterState =
    if cs.openCenter then CenterState.Open
    else if cs.lockedCenter then CenterState.Locked
    else CenterState.Fluid

  private def spaceScore(board: Board, color: Color, flank: Flank): Int =
    val files =
      flank match
        case Flank.Kingside  => Set(File.F, File.G, File.H)
        case Flank.Queenside => Set(File.A, File.B, File.C)
        case Flank.Center    => Set(File.C, File.D, File.E, File.F)
    (board.pawns & board.byColor(color)).squares
      .filter(sq => files.contains(sq.file))
      .map(sq => if color.white then sq.rank.value else 7 - sq.rank.value)
      .sum

  private def fileStatus(board: Board, file: File): FileStatus =
    val fileBb = Bitboard.file(file)
    val w = (board.pawns & board.white) & fileBb
    val b = (board.pawns & board.black) & fileBb
    if w.isEmpty && b.isEmpty then FileStatus.Open
    else if w.nonEmpty && b.nonEmpty then FileStatus.Closed
    else FileStatus.SemiOpen

  private def hasFianchettoShell(board: Board, color: Color): Boolean =
    val pawnSq = if color.white then Square.G3 else Square.G6
    val bishopSq = if color.white then Square.G2 else Square.G7
    board.pieceAt(pawnSq).contains(Piece(color, Pawn)) && board.pieceAt(bishopSq).contains(Piece(color, Bishop))

  private def isCarlsbadLike(board: Board): Boolean =
    val whiteHasD4 = board.pieceAt(Square.D4).contains(Piece(Color.White, Pawn))
    val blackHasC6 = board.pieceAt(Square.C6).contains(Piece(Color.Black, Pawn))
    val blackHasD5 = board.pieceAt(Square.D5).contains(Piece(Color.Black, Pawn))
    val whiteCFilePawns = (board.pawns & board.white) & Bitboard.file(File.C)
    whiteHasD4 && blackHasC6 && blackHasD5 && whiteCFilePawns.isEmpty

  private def isMaroczyBindLike(board: Board, color: Color): Boolean =
    val (cSq, eSq) =
      if color.white then (Square.C4, Square.E4)
      else (Square.C5, Square.E5)
    board.pieceAt(cSq).contains(Piece(color, Pawn)) &&
      board.pieceAt(eSq).contains(Piece(color, Pawn))
