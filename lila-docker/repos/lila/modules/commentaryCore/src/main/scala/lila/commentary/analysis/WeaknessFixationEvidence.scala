package lila.commentary.analysis

import chess.*
import chess.format.Fen
import lila.commentary.analysis.PlanTaxonomy.{ PlanKind, PlanTheme, ThemeResolver }
import lila.commentary.analysis.PlanMoveEvidenceSupport.*
import lila.commentary.analysis.structure.WeaknessTargetProfile
import lila.commentary.model.authoring.{ PlanHypothesis, PlanViability }

object WeaknessFixationEvidence:

  final case class WeaknessCandidate(
      kind: PlanKind,
      targetSquare: String,
      targetKind: String,
      moveUci: String,
      pressureBefore: Int,
      pressureAfter: Int,
      capturesTarget: Boolean,
      createsTarget: Boolean,
      score: Double,
      reasons: List[String]
  )

  def candidatesFromFen(fen: String, color: Color): List[WeaknessCandidate] =
    Fen
      .read(chess.variant.Standard, Fen.Full(fen))
      .toList
      .flatMap { pos =>
        val oriented = orientedPosition(pos, color)
        combinedCandidates(
          base = structuralWeaknessCandidates(oriented, color),
          minority = minorityAttackCandidates(fen, oriented, color)
        )
      }

  def candidates(pos: Position, color: Color): List[WeaknessCandidate] =
    val oriented = orientedPosition(pos, color)
    combinedCandidates(
      base = structuralWeaknessCandidates(oriented, color),
      minority = minorityAttackCandidates(Fen.write(oriented).value, oriented, color)
    )

  private def combinedCandidates(
      base: List[WeaknessCandidate],
      minority: List[WeaknessCandidate]
  ): List[WeaknessCandidate] =
    (base ++ minority)
      .sortBy(c => (-c.score, c.kind.id, c.targetSquare, c.moveUci))
      .distinctBy(c => s"${c.kind.id}|${c.targetSquare}|${c.moveUci}")

  private def structuralWeaknessCandidates(pos: Position, color: Color): List[WeaknessCandidate] =
    val oriented = orientedPosition(pos, color)
    val board = oriented.board
    val legalMoves = oriented.legalMoves.toList
    val existingTargets = WeaknessTargetProfile.targetsForPressure(board, color)
    val existingKeys = existingTargets.map(_.targetSquare).toSet
    val existing =
      for
        target <- existingTargets
        targetSq <- Square.fromKey(target.targetSquare).toList
        mv <- legalMoves
        candidate <- pressureCandidate(board, color, target, targetSq, mv)
      yield candidate
    val created =
      legalMoves.flatMap { mv =>
        val afterTargets =
          WeaknessTargetProfile
            .targetsForPressure(mv.after.board, color)
            .filterNot(target => existingKeys.contains(target.targetSquare))
        afterTargets.flatMap(createdCandidate(color, mv, _))
      }
    existing ++ created

  private def minorityAttackCandidates(
      fen: String,
      pos: Position,
      color: Color
  ): List[WeaknessCandidate] =
    val oriented = orientedPosition(pos, color)
    val legalMoves = oriented.legalMoves.toList
    val semantic =
      StrategicIdeaSemanticContext(
        sideToMove = if color.white then "white" else "black",
        fen = fen,
        board = Some(oriented.board)
      )
    StrategicConceptSemantics
      .minorityAttackObservations(semantic)
      .filter(observation =>
        observation.status == StrategicConceptSemantics.ConceptStatus.SemanticReady &&
          observation.side == color &&
          observation.primaryBreak.nonEmpty &&
          observation.targets.nonEmpty
      )
      .flatMap(observation => minorityAttackCandidate(observation, legalMoves))
      .sortBy(c => (-c.score, c.kind.id, c.targetSquare, c.moveUci))
      .distinctBy(c => s"${c.kind.id}|${c.targetSquare}|${c.moveUci}")

  private def minorityAttackCandidate(
      observation: StrategicConceptSemantics.StrategicConceptObservation,
      legalMoves: List[Move]
  ): Option[WeaknessCandidate] =
    val candidateUci =
      observation.prepMoves.headOption.orElse(observation.primaryBreak)
    candidateUci.flatMap { uci =>
      legalMoves.find(mv => NarrativeUtils.uciEquivalent(mv.toUci.uci, uci)).map { legalMove =>
        val target = observation.targets.head
        val delta = observation.structuralDelta
        val pressureDelta = delta.map(_.targetPressureDelta).getOrElse(0)
        val reasons =
          (
            List(
              "minority_attack_semantic",
              s"minority_wing:${observation.wing}",
              s"primary_break:${observation.primaryBreak.getOrElse(uci)}",
              s"target_after:$target"
            ) ++
              Option.when(observation.prepMoves.nonEmpty)(s"prep_move:${observation.prepMoves.head}") ++
              delta.toList.flatMap(d =>
                List(
                  Option.when(d.createdTension.nonEmpty)(s"created_tension:${d.createdTension.mkString(",")}"),
                  Option.when(d.newWeakPawns.nonEmpty)(s"new_weak_pawns:${d.newWeakPawns.mkString(",")}"),
                  Option.when(d.targetPressureDelta > 0)(s"target_pressure_delta:${d.targetPressureDelta}")
                ).flatten
              ) ++
              observation.essentialEvidence.map(evidence => s"semantic_evidence:${evidence.id}")
          ).distinct
        WeaknessCandidate(
          kind = PlanKind.MinorityAttackFixation,
          targetSquare = target,
          targetKind = "minority_attack_target",
          moveUci = legalMove.toUci.uci,
          pressureBefore = 0,
          pressureAfter = pressureDelta.max(0),
          capturesTarget = false,
          createsTarget = true,
          score = minorityAttackScore(observation),
          reasons = reasons
        )
      }
    }

  def planHypotheses(fen: String, color: Color): List[PlanHypothesis] =
    candidatesFromFen(fen, color)
      .groupBy(_.kind)
      .values
      .toList
      .flatMap(_.sortBy(c => -c.score).headOption)
      .sortBy(c => -c.score)
      .take(3)
      .map(toHypothesis)

  def movesForSubplan(pos: Position, subplan: PlanKind, legalMoves: List[Move]): List[Move] =
    val byUci =
      candidates(pos, pos.color)
        .filter(_.kind == subplan)
        .map(candidate => candidate.moveUci -> candidate.score)
        .toMap
    legalMoves
      .flatMap(mv => byUci.get(mv.toUci.uci).map(score => mv -> score))
      .sortBy { case (_, score) => -score }
      .map(_._1)
      .distinct

  private def pressureCandidate(
      board: Board,
      color: Color,
      target: WeaknessTargetProfile,
      targetSq: Square,
      mv: Move
  ): Option[WeaknessCandidate] =
    if mv.piece.color != color || mv.promotion.nonEmpty then None
    else
      val beforePressure = board.attackers(targetSq, color).count
      val afterBoard = mv.after.board
      val targetStillThere =
        afterBoard.pieceAt(targetSq).exists(piece => piece.color == !color && piece.role == Pawn)
      val afterPressure = if targetStillThere then afterBoard.attackers(targetSq, color).count else beforePressure
      val capturesTarget =
        mv.captures &&
          mv.dest == targetSq &&
          board.pieceAt(targetSq).exists(piece => piece.color == !color && piece.role == Pawn)
      val addsPressure = targetStillThere && afterPressure > beforePressure
      val linePressure =
        targetStillThere &&
          (mv.piece.role == Rook || mv.piece.role == Queen || mv.piece.role == Bishop || mv.piece.role == Knight) &&
          afterBoard.attackers(targetSq, color).contains(mv.dest)
      val qualifies = capturesTarget || addsPressure || linePressure
      Option.when(qualifies) {
        val kind = kindForTarget(target.kind)
        val reasons =
          (
            target.evidenceTerms ++
              List(
                Option.when(capturesTarget)("captures_target"),
                Option.when(addsPressure)(s"pressure_delta:$beforePressure->$afterPressure"),
                Option.when(linePressure)("piece_attacks_target")
              ).flatten
          ).distinct
        WeaknessCandidate(
          kind = kind,
          targetSquare = target.targetSquare,
          targetKind = target.kind,
          moveUci = mv.toUci.uci,
          pressureBefore = beforePressure,
          pressureAfter = afterPressure,
          capturesTarget = capturesTarget,
          createsTarget = false,
          score = weaknessScore(kind, target.kind, capturesTarget, addsPressure, createsTarget = false),
          reasons = reasons
        )
      }.filter(_.score >= 0.58)

  private def createdCandidate(
      color: Color,
      mv: Move,
      target: WeaknessTargetProfile
  ): Option[WeaknessCandidate] =
    val targetSq = Square.fromKey(target.targetSquare)
    val createsByPawnContact =
      mv.piece.color == color &&
        mv.piece.role == Pawn &&
        targetSq.exists(sq => createsEnemyPawnContact(mv.after.board, sq, !color))
    val sameSector =
      targetSq.exists(sq => (sq.file.value - mv.dest.file.value).abs <= 1)
    Option.when(createsByPawnContact && sameSector) {
      val kind = kindForTarget(target.kind)
      val reasons =
        (target.evidenceTerms ++ List("creates_weakness_target", s"target_after:${target.targetSquare}")).distinct
      WeaknessCandidate(
        kind = kind,
        targetSquare = target.targetSquare,
        targetKind = target.kind,
        moveUci = mv.toUci.uci,
        pressureBefore = 0,
        pressureAfter = 0,
        capturesTarget = false,
        createsTarget = true,
        score = weaknessScore(kind, target.kind, capturesTarget = false, addsPressure = false, createsTarget = true),
        reasons = reasons
      )
    }.filter(_.score >= 0.58)

  private def toHypothesis(candidate: WeaknessCandidate): PlanHypothesis =
    val moveText = displayMove(candidate.moveUci)
    val targetText = targetKindText(candidate.targetKind)
    val planName =
      candidate.kind match
        case PlanKind.BackwardPawnTargeting =>
          s"Pressure the backward pawn on ${candidate.targetSquare}"
        case PlanKind.IQPInducement =>
          s"Fix the isolated queen pawn on ${candidate.targetSquare}"
        case PlanKind.MinorityAttackFixation =>
          s"Use $moveText to induce a minority-attack target on ${candidate.targetSquare}"
        case _ =>
          s"Fix the static weakness on ${candidate.targetSquare}"
    PlanHypothesis(
      planId = candidate.kind.id,
      planName = planName,
      rank = 0,
      score = candidate.score,
      preconditions = weaknessWhyText(candidate, moveText, targetText).distinct.take(5),
      executionSteps = weaknessExecutionText(candidate, moveText),
      failureModes = List(
        "the target may liquidate before pressure accumulates",
        "a tactical reply can make target play too slow"
      ),
      viability = PlanViability(
        score = candidate.score,
        label = if candidate.score >= 0.72 then "high" else "medium",
        risk = "weakness fixation can fail if the target liquidates cleanly"
      ),
      refutation = Some("change plans if the target disappears without concession"),
      evidenceSources = List(
        ThemeResolver.themeTag(PlanTheme.WeaknessFixation),
        ThemeResolver.subplanTag(candidate.kind),
        ThemeResolver.structuralStateTag("weakness_target"),
        "weakness_target_profile"
      ) ++ candidate.reasons,
      themeL1 = PlanTheme.WeaknessFixation.id,
      subplanId = Some(candidate.kind.id)
    )

  private def weaknessWhyText(candidate: WeaknessCandidate, moveText: String, targetText: String): List[String] =
    val pressureGain = candidate.pressureAfter - candidate.pressureBefore
    val primaryBreak = reasonPayload(candidate.reasons, "primary_break:").map(displayMove)
    val prepMove = reasonPayload(candidate.reasons, "prep_move:").map(displayMove)
    val why =
      List(
        Some(s"${candidate.targetSquare} is a $targetText"),
        Option.when(candidate.capturesTarget)(
          s"$moveText removes the target pawn directly"
        ),
        Option.when(pressureGain > 0)(
          s"pressure on ${candidate.targetSquare} rises from ${candidate.pressureBefore} to ${candidate.pressureAfter}"
        ),
        Option.when(candidate.createsTarget && candidate.kind != PlanKind.MinorityAttackFixation)(
          s"$moveText fixes ${candidate.targetSquare} as a new weakness"
        ),
        Option.when(candidate.kind == PlanKind.MinorityAttackFixation)(
          primaryBreak match
            case Some(breakMove) if prepMove.exists(_ != breakMove) =>
              s"the minority attack can stage $moveText before the $breakMove break"
            case Some(breakMove) =>
              s"$breakMove is the board-backed minority break for ${candidate.targetSquare}"
            case None =>
              s"$moveText creates the minority-attack target on ${candidate.targetSquare}"
        ),
        Option.when(candidate.reasons.exists(_.startsWith("created_tension:")))(
          "the move creates pawn tension around the target side"
        ),
        Option.when(candidate.reasons.exists(_.startsWith("new_weak_pawns:")))(
          "the structure creates new weak pawns to pressure"
        )
      ).flatten
    if why.nonEmpty then why
    else List(s"$moveText keeps pressure on ${candidate.targetSquare}")

  private def weaknessExecutionText(candidate: WeaknessCandidate, moveText: String): List[String] =
    val primaryBreak = reasonPayload(candidate.reasons, "primary_break:").map(displayMove)
    val prepMove = reasonPayload(candidate.reasons, "prep_move:").map(displayMove)
    val first =
      candidate.kind match
        case PlanKind.MinorityAttackFixation =>
          primaryBreak match
            case Some(breakMove) if prepMove.exists(_ != breakMove) =>
              s"stage $moveText so the $breakMove break can fix ${candidate.targetSquare}"
            case Some(_) =>
              s"play $moveText as the minority break against ${candidate.targetSquare}"
            case None =>
              s"play $moveText to create the minority-attack target"
        case _ if candidate.capturesTarget =>
          s"take the target on ${candidate.targetSquare} with $moveText"
        case _ if candidate.pressureAfter > candidate.pressureBefore =>
          s"add pressure with $moveText and keep pieces aimed at ${candidate.targetSquare}"
        case _ if candidate.createsTarget =>
          s"use $moveText to fix ${candidate.targetSquare} before it can move"
        case _ =>
          s"use $moveText as the concrete target move"
    val second =
      if candidate.capturesTarget then "keep the recapture from dissolving the structural gain"
      else "keep pressure on the target until it persists or falls"
    List(first, second).distinct

  private def targetKindText(kind: String): String =
    kind match
      case WeaknessTargetProfile.BackwardPawn => "backward pawn target"
      case WeaknessTargetProfile.IQP          => "isolated queen pawn target"
      case WeaknessTargetProfile.IsolatedPawn => "isolated pawn target"
      case WeaknessTargetProfile.DoubledPawn  => "doubled pawn target"
      case WeaknessTargetProfile.FixedPawn    => "fixed pawn target"
      case "minority_attack_target"           => "minority-attack target"
      case other                              => other.replace('_', ' ')

  private def reasonPayload(reasons: List[String], prefix: String): Option[String] =
    reasons.collectFirst {
      case reason if reason.startsWith(prefix) => reason.drop(prefix.length).trim
    }.filter(_.nonEmpty)

  private def displayMove(uci: String): String =
    if uci.length >= 4 then s"${uci.take(2)}-${uci.slice(2, 4)}" else uci

  private def kindForTarget(targetKind: String): PlanKind =
    targetKind match
      case WeaknessTargetProfile.BackwardPawn => PlanKind.BackwardPawnTargeting
      case WeaknessTargetProfile.IQP          => PlanKind.IQPInducement
      case _                                  => PlanKind.StaticWeaknessFixation

  private def weaknessScore(
      kind: PlanKind,
      targetKind: String,
      capturesTarget: Boolean,
      addsPressure: Boolean,
      createsTarget: Boolean
  ): Double =
    val base =
      kind match
        case PlanKind.BackwardPawnTargeting   => 0.65
        case PlanKind.IQPInducement           => 0.63
        case PlanKind.MinorityAttackFixation  => 0.60
        case PlanKind.StaticWeaknessFixation  => 0.62
        case _                                => 0.56
    (base +
      (if capturesTarget then 0.12 else 0.0) +
      (if addsPressure then 0.06 else 0.0) +
      (if createsTarget then 0.07 else 0.0) +
      (if targetKind == WeaknessTargetProfile.BackwardPawn || targetKind == WeaknessTargetProfile.IQP then 0.04 else 0.0))
      .max(0.30)
      .min(0.86)

  private def minorityAttackScore(
      observation: StrategicConceptSemantics.StrategicConceptObservation
  ): Double =
    val delta = observation.structuralDelta
    (0.66 +
      observation.targets.size.min(2) * 0.025 +
      (if observation.prepMoves.isEmpty then 0.04 else 0.0) +
      delta.map(_.targetPressureDelta.max(0).min(3) * 0.025).getOrElse(0.0) +
      delta.map(d => if d.createdTension.nonEmpty then 0.04 else 0.0).getOrElse(0.0) +
      delta.map(d => if d.newWeakPawns.nonEmpty then 0.04 else 0.0).getOrElse(0.0))
      .max(0.30)
      .min(0.84)
