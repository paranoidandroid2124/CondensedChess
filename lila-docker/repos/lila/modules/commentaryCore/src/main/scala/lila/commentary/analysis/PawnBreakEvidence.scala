package lila.commentary.analysis

import chess.*
import chess.format.Fen
import lila.commentary.analysis.PlanTaxonomy.{ PlanKind, ThemeResolver }
import lila.commentary.analysis.PlanMoveEvidenceSupport.*
import lila.commentary.model.authoring.{ PlanHypothesis, PlanViability }

object PawnBreakEvidence:

  final case class PawnBreakCandidate(
      kind: PlanKind,
      from: Square,
      to: Square,
      captures: Boolean,
      createsContact: Boolean,
      tensionPairsBefore: Int = 0,
      tensionPairsAfter: Int = 0,
      score: Double,
      reasons: List[String]
  ):
    def uci: String = s"${from.key}${to.key}"

  def candidatesFromFen(fen: String, color: Color): List[PawnBreakCandidate] =
    Fen
      .read(chess.variant.Standard, Fen.Full(fen))
      .toList
      .flatMap { pos =>
        val oriented = orientedPosition(pos, color)
        candidates(oriented, color)
      }

  def candidates(pos: Position, color: Color): List[PawnBreakCandidate] =
    val oriented = orientedPosition(pos, color)
    oriented.legalMoves.toList
      .flatMap(moveCandidates)
      .sortBy(c => (-c.score, c.kind.id, c.from.key, c.to.key))
      .distinctBy(c => s"${c.kind.id}|${c.uci}")

  def planHypotheses(fen: String, color: Color): List[PlanHypothesis] =
    candidatesFromFen(fen, color)
      .groupBy(_.kind)
      .values
      .toList
      .flatMap(_.sortBy(c => -c.score).headOption)
      .sortBy(c => -c.score)
      .take(2)
      .map(toHypothesis)

  def movesForSubplan(pos: Position, subplan: PlanKind, legalMoves: List[Move]): List[Move] =
    legalMoves
      .filter(_.piece.color == pos.color)
      .flatMap { mv =>
        moveCandidates(mv).find(_.kind == subplan).map(candidate => mv -> candidate.score)
      }
      .sortBy { case (_, score) => -score }
      .map(_._1)
      .distinct

  private def moveCandidates(mv: Move): List[PawnBreakCandidate] =
    if mv.promotion.nonEmpty then Nil
    else if preservesCentralTension(mv) then
      val tensionBefore = centralTensionPairs(mv.before.board)
      val tensionAfter = centralTensionPairs(mv.after.board)
      List(
        PawnBreakCandidate(
          kind = PlanKind.TensionMaintenance,
          from = mv.orig,
          to = mv.dest,
          captures = false,
          createsContact = false,
          tensionPairsBefore = tensionBefore.size,
          tensionPairsAfter = tensionAfter.size,
          score = tensionScore(mv, tensionBefore.size),
          reasons = List("central_tension_preserved") ++ tensionSupportReasons(mv, tensionBefore)
        )
      )
    else if mv.piece.role != Pawn || !advancesTowardPromotion(mv, mv.piece.color) then Nil
    else
      val color = mv.piece.color
      val contact = createsEnemyPawnContact(mv.after.board, mv.dest, color)
      val qualifies = mv.captures || contact
      if !qualifies then Nil
      else
        val center =
          isCentralFile(mv.orig.file) || isCentralFile(mv.dest.file)
        val wing =
          isFlankFile(mv.orig.file) || isFlankFile(mv.dest.file)
        val centralCandidate =
          Option.when(center) {
            val reasons =
              List(
                Some("pawn_break"),
                Option.when(mv.captures)("captures_tension_pawn"),
                Option.when(contact)("creates_enemy_pawn_contact")
              ).flatten
            PawnBreakCandidate(
              kind = PlanKind.CentralBreakTiming,
              from = mv.orig,
              to = mv.dest,
              captures = mv.captures,
              createsContact = contact,
              tensionPairsBefore = 0,
              tensionPairsAfter = 0,
              score = breakScore(PlanKind.CentralBreakTiming, mv.captures, contact),
              reasons = reasons
            )
          }
        val wingCandidate =
          Option.when(wing && !center) {
            val reasons =
              List(
                Some("wing_pawn_break"),
                Option.when(mv.captures)("captures_flank_tension_pawn"),
                Option.when(contact)("creates_enemy_pawn_contact")
              ).flatten
            PawnBreakCandidate(
              kind = PlanKind.WingBreakTiming,
              from = mv.orig,
              to = mv.dest,
              captures = mv.captures,
              createsContact = contact,
              tensionPairsBefore = 0,
              tensionPairsAfter = 0,
              score = breakScore(PlanKind.WingBreakTiming, mv.captures, contact),
              reasons = reasons
            )
          }
        List(centralCandidate, wingCandidate).flatten.filter(_.score >= 0.58)

  private def toHypothesis(candidate: PawnBreakCandidate): PlanHypothesis =
    val theme = candidate.kind.theme
    val moveText = displayMove(candidate.uci)
    val planName =
      candidate.kind match
        case PlanKind.WingBreakTiming =>
          s"Time the wing break $moveText"
        case PlanKind.TensionMaintenance =>
          s"Maintain central tension with $moveText"
        case _ =>
          s"Time the central break $moveText"
    val preconditions = pawnBreakWhyText(candidate, moveText)
    val executionSteps = pawnBreakExecutionText(candidate, moveText)
    val failureModes =
      candidate.kind match
        case PlanKind.TensionMaintenance =>
          List(
            "the waiting move may let the opponent choose the release on better terms",
            "a forcing tactic can make tension maintenance too slow"
          )
        case _ =>
          List(
            "the pawn break can open lines for the opponent instead",
            "a tactical reply can make the timing premature"
          )
    val structuralState =
      candidate.kind match
        case PlanKind.TensionMaintenance => "tension_maintenance"
        case _                           => "pawn_break_timing"
    val evidenceKind =
      candidate.kind match
        case PlanKind.TensionMaintenance => "tension_maintenance"
        case _                           => "pawn_break"
    PlanHypothesis(
      planId = candidate.kind.id,
      planName = planName,
      rank = 0,
      score = candidate.score,
      preconditions =
        preconditions.distinct.take(5),
      executionSteps = executionSteps,
      failureModes = failureModes,
      viability = PlanViability(
        score = candidate.score,
        label = if candidate.score >= 0.72 then "high" else "medium",
        risk =
          if candidate.kind == PlanKind.TensionMaintenance then "tension maintenance can fail if the opponent forces a favorable release"
          else "pawn break timing can fail if contact opens more counterplay than it gains"
      ),
      refutation =
        Some(
          if candidate.kind == PlanKind.TensionMaintenance then "change plans if the opponent can force a favorable release"
          else "change plans if the break releases more counterplay than it gains"
        ),
      evidenceSources = List(
        ThemeResolver.themeTag(theme),
        ThemeResolver.subplanTag(candidate.kind),
        ThemeResolver.structuralStateTag(structuralState),
        evidenceKind
      ),
      themeL1 = theme.id,
      subplanId = Some(candidate.kind.id)
    )

  private def pawnBreakWhyText(candidate: PawnBreakCandidate, moveText: String): List[String] =
    candidate.kind match
      case PlanKind.TensionMaintenance =>
        val support =
          List(
            Option.when(candidate.reasons.contains("central_tension_preserved"))(
              s"$moveText keeps the existing central pawn contact"
            ),
            Option.when(candidate.tensionPairsBefore > 0)(
              s"central pawn tension stays at ${pairCountText(candidate.tensionPairsAfter)}"
            ),
            Option.when(candidate.reasons.contains("king_safety_while_tension_held"))(
              "king safety improves while the center stays tense"
            ),
            Option.when(candidate.reasons.contains("minor_piece_centralizes"))(
              "the minor piece centralizes without releasing the center"
            ),
            reasonPayload(candidate.reasons, "supports_tension_file:").map(file =>
              s"the major piece supports the $file-file tension"
            ),
            mobilityGain(candidate.reasons).map(gain =>
              s"the supporting piece gains $gain mobility ${if gain == 1 then "square" else "squares"}"
            )
          ).flatten
        if support.nonEmpty then support
        else List(s"$moveText keeps the center closed until the release is clearer")
      case _ =>
        List(
          Some(
            s"$moveText is a concrete ${if candidate.kind == PlanKind.WingBreakTiming then "wing" else "central"} pawn break"
          ),
          Option.when(candidate.captures)("the break captures an opposing pawn"),
          Option.when(candidate.createsContact && !candidate.captures)(
            "the break creates direct pawn contact"
          )
        ).flatten

  private def pawnBreakExecutionText(candidate: PawnBreakCandidate, moveText: String): List[String] =
    candidate.kind match
      case PlanKind.TensionMaintenance =>
        List(
          s"hold the center with $moveText instead of releasing the pawn tension",
          "choose the release only after the exchange terms improve"
        )
      case PlanKind.WingBreakTiming =>
        List(
          s"play $moveText as the concrete wing break",
          "watch that the opened flank does not give the opponent faster counterplay"
        )
      case _ =>
        List(
          s"play $moveText as the concrete central break",
          "watch that the opened center does not give the opponent faster counterplay"
        )

  private def pairCountText(count: Int): String =
    s"$count central tension ${if count == 1 then "pair" else "pairs"}"

  private def mobilityGain(reasons: List[String]): Option[Int] =
    reasonPayload(reasons, "mobility_gain:").flatMap(_.toIntOption)

  private def reasonPayload(reasons: List[String], prefix: String): Option[String] =
    reasons.collectFirst {
      case reason if reason.startsWith(prefix) => reason.drop(prefix.length).trim
    }.filter(_.nonEmpty)

  private def displayMove(uci: String): String =
    if uci.length >= 4 then s"${uci.take(2)}-${uci.slice(2, 4)}" else uci

  private def breakScore(kind: PlanKind, captures: Boolean, contact: Boolean): Double =
    val base =
      kind match
        case PlanKind.WingBreakTiming    => 0.62
        case PlanKind.CentralBreakTiming => 0.64
        case _                           => 0.56
    (base +
      (if captures then 0.06 else 0.0) +
      (if contact then 0.04 else 0.0))
      .max(0.30)
      .min(0.82)

  private def tensionScore(mv: Move, tensionPairs: Int): Double =
    (0.60 +
      tensionPairs.min(3) * 0.03 +
      (if mv.castle.nonEmpty then 0.04 else 0.0) +
      (if mv.piece.role == Knight || mv.piece.role == Bishop then 0.02 else 0.0))
      .max(0.30)
      .min(0.76)

  private def preservesCentralTension(mv: Move): Boolean =
    val quietMove =
      !mv.captures &&
        mv.promotion.isEmpty &&
        !mv.after.check.yes
    if !quietMove then false
    else
      val before = centralTensionPairs(mv.before.board)
      before.nonEmpty &&
        before.subsetOf(centralTensionPairs(mv.after.board)) &&
        tensionSupportReasons(mv, before).nonEmpty

  private def tensionSupportReasons(mv: Move, tensionPairs: Set[(Square, Square)]): List[String] =
    if mv.castle.nonEmpty then List("king_safety_while_tension_held")
    else
      val beforeMobility = pieceMobility(mv.before.board, mv.piece, mv.orig)
      val afterMobility = projectedMobility(mv.before.board, mv.piece, mv.orig, mv.dest)
      val mobilityGain = afterMobility - beforeMobility
      val tensionFiles = tensionPairs.flatMap { case (left, right) => Set(left.file, right.file) }
      mv.piece.role match
        case Knight | Bishop =>
          List(
            Option.when(isCentralSquare(mv.dest))("minor_piece_centralizes"),
            Option.when(mobilityGain > 0)(s"mobility_gain:$mobilityGain")
          ).flatten
        case Rook | Queen =>
          val supportsTensionFile = tensionFiles.contains(mv.dest.file)
          List(
            Option.when(supportsTensionFile)(s"supports_tension_file:${mv.dest.file.char}"),
            Option.when(supportsTensionFile && mobilityGain > 0)(s"mobility_gain:$mobilityGain")
          ).flatten
        case _ => Nil

  private def centralTensionPairs(board: Board): Set[(Square, Square)] =
    board.pawns.squares.toList.flatMap { pawnSq =>
      board.pieceAt(pawnSq).toList.collect { case Piece(color, Pawn) => color }.flatMap { color =>
        val enemyPawns = board.pawns & board.byColor(!color)
        (pawnSq.pawnAttacks(color) & enemyPawns).squares.toList
          .filter(target => isCentralFile(pawnSq.file) || isCentralFile(target.file))
          .map(target => orderedPair(pawnSq, target))
      }
    }.toSet

  private def orderedPair(left: Square, right: Square): (Square, Square) =
    if left.key <= right.key then left -> right else right -> left
