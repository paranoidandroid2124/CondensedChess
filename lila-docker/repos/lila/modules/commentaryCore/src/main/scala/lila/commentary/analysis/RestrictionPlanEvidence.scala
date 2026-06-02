package lila.commentary.analysis

import chess.*
import chess.format.Fen
import lila.commentary.analysis.PlanTaxonomy.{ PlanKind, ThemeResolver }
import lila.commentary.analysis.PlanMoveEvidenceSupport.*
import lila.commentary.model.authoring.{ PlanHypothesis, PlanViability }

object RestrictionPlanEvidence:

  final case class RestrictionCandidate(
      kind: PlanKind,
      moveUci: String,
      destination: Square,
      opponentBreaksBefore: Int,
      opponentBreaksAfter: Int,
      opponentMobilityBefore: Int,
      opponentMobilityAfter: Int,
      score: Double,
      reasons: List[String]
  )

  def candidatesFromFen(fen: String, color: Color): List[RestrictionCandidate] =
    Fen
      .read(chess.variant.Standard, Fen.Full(fen))
      .toList
      .flatMap { pos =>
        val oriented = orientedPosition(pos, color)
        candidates(oriented, color)
      }

  def candidates(pos: Position, color: Color): List[RestrictionCandidate] =
    val oriented = orientedPosition(pos, color)
    val opponent = !color
    val opponentBefore = oriented.withColor(opponent)
    val opponentBreaksBefore = pawnBreakMoves(opponentBefore, opponent)
    val opponentMobilityBefore = nonKingMobility(opponentBefore, opponent)
    val opponentDestinationsBefore = opponentBefore.legalMoves.toList.map(_.dest).toSet

    oriented.legalMoves.toList
      .flatMap { mv =>
        val opponentAfter = mv.after.withColor(opponent)
        val opponentBreaksAfter = pawnBreakMoves(opponentAfter, opponent)
        val opponentMobilityAfter = nonKingMobility(opponentAfter, opponent)
        moveCandidates(
          mv = mv,
          opponentDestinationsBefore = opponentDestinationsBefore,
          opponentBreaksBefore = opponentBreaksBefore,
          opponentBreaksAfter = opponentBreaksAfter,
          opponentMobilityBefore = opponentMobilityBefore,
          opponentMobilityAfter = opponentMobilityAfter
        )
      }
      .sortBy(c => (-c.score, c.kind.id, c.moveUci))
      .distinctBy(c => s"${c.kind.id}|${c.moveUci}")

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

  private def moveCandidates(
      mv: Move,
      opponentDestinationsBefore: Set[Square],
      opponentBreaksBefore: List[Move],
      opponentBreaksAfter: List[Move],
      opponentMobilityBefore: Int,
      opponentMobilityAfter: Int
  ): List[RestrictionCandidate] =
    if mv.promotion.nonEmpty then Nil
    else
      val breakDrop = opponentBreaksBefore.size - opponentBreaksAfter.size
      val mobilityDrop = opponentMobilityBefore - opponentMobilityAfter
      val attackedResources =
        attackedEnemyPieces(mv.after.board, mv.piece, mv.dest)
          .filter(square => mv.after.board.pieceAt(square).exists(piece => piece.color == !mv.piece.color && piece.role != King))
      val deniesKeySquare =
        !mv.captures &&
          isCentralSquare(mv.dest) &&
          opponentDestinationsBefore.contains(mv.dest)
      val breakPrevention =
        breakDrop > 0 &&
          (touchesBreakFiles(mv, opponentBreaksBefore) || mv.piece.role == Pawn || isCentralSquare(mv.dest))
      val mobilitySuppression =
        !mv.captures &&
          mobilityDrop >= 2 &&
          !mv.after.check.yes &&
          (deniesKeySquare || attackedResources.nonEmpty)
      val centralBind =
        mv.piece.role == Pawn &&
          !mv.captures &&
          isCentralFile(mv.dest.file) &&
          centralControlDelta(mv) > 0
      val flankClamp =
        mv.piece.role == Pawn &&
          !mv.captures &&
          isFlankFile(mv.dest.file) &&
          createsEnemyPawnContact(mv.after.board, mv.dest, mv.piece.color)
      val baseReasons =
        List(
          Option.when(breakDrop > 0)(s"opponent_breaks:${opponentBreaksBefore.size}->${opponentBreaksAfter.size}"),
          Option.when(mobilityDrop > 0)(s"opponent_mobility:${opponentMobilityBefore}->${opponentMobilityAfter}"),
          Option.when(deniesKeySquare)(s"denied_square:${mv.dest.key}"),
          Option.when(attackedResources.nonEmpty)(s"restricted_resources:${attackedResources.map(_.key).sorted.mkString(",")}"),
          Option.when(centralBind)("central_control_gain"),
          Option.when(flankClamp)("flank_contact_clamp")
        ).flatten
      List(
        Option.when(breakPrevention)(
          mkCandidate(PlanKind.BreakPrevention, mv, opponentBreaksBefore.size, opponentBreaksAfter.size, opponentMobilityBefore, opponentMobilityAfter, baseReasons, 0.68)
        ),
        Option.when(deniesKeySquare)(
          mkCandidate(PlanKind.KeySquareDenial, mv, opponentBreaksBefore.size, opponentBreaksAfter.size, opponentMobilityBefore, opponentMobilityAfter, baseReasons, 0.66)
        ),
        Option.when(mobilitySuppression)(
          mkCandidate(PlanKind.MobilitySuppression, mv, opponentBreaksBefore.size, opponentBreaksAfter.size, opponentMobilityBefore, opponentMobilityAfter, baseReasons, 0.64)
        ),
        Option.when(centralBind)(
          mkCandidate(PlanKind.CentralSpaceBind, mv, opponentBreaksBefore.size, opponentBreaksAfter.size, opponentMobilityBefore, opponentMobilityAfter, baseReasons, 0.62)
        ),
        Option.when(flankClamp)(
          mkCandidate(PlanKind.FlankClamp, mv, opponentBreaksBefore.size, opponentBreaksAfter.size, opponentMobilityBefore, opponentMobilityAfter, baseReasons, 0.61)
        )
      ).flatten.filter(_.score >= 0.58)

  private def mkCandidate(
      kind: PlanKind,
      mv: Move,
      breakBefore: Int,
      breakAfter: Int,
      mobilityBefore: Int,
      mobilityAfter: Int,
      reasons: List[String],
      base: Double
  ): RestrictionCandidate =
    val breakDrop = (breakBefore - breakAfter).max(0)
    val mobilityDrop = (mobilityBefore - mobilityAfter).max(0)
    val score =
      (base +
        breakDrop.min(3) * 0.035 +
        mobilityDrop.min(6) * 0.015 +
        (if isCentralSquare(mv.dest) then 0.03 else 0.0))
        .max(0.30)
        .min(0.84)
    RestrictionCandidate(
      kind = kind,
      moveUci = mv.toUci.uci,
      destination = mv.dest,
      opponentBreaksBefore = breakBefore,
      opponentBreaksAfter = breakAfter,
      opponentMobilityBefore = mobilityBefore,
      opponentMobilityAfter = mobilityAfter,
      score = score,
      reasons = reasons.distinct
    )

  private def toHypothesis(candidate: RestrictionCandidate): PlanHypothesis =
    val moveText = displayMove(candidate.moveUci)
    val planName =
      candidate.kind match
        case PlanKind.BreakPrevention =>
          s"Prevent the counter-break with $moveText"
        case PlanKind.KeySquareDenial =>
          s"Deny ${candidate.destination.key} with $moveText"
        case PlanKind.MobilitySuppression =>
          s"Reduce opponent mobility with $moveText"
        case PlanKind.CentralSpaceBind =>
          s"Bind central space with $moveText"
        case PlanKind.FlankClamp =>
          s"Clamp the flank with $moveText"
        case _ =>
          s"Use $moveText as a prophylactic restraint"
    PlanHypothesis(
      planId = candidate.kind.id,
      planName = planName,
      rank = 0,
      score = candidate.score,
      preconditions = restrictionWhyText(candidate).distinct.take(5),
      executionSteps = restrictionExecutionText(candidate, moveText),
      failureModes = List(
        "restriction may be too slow if a tactic is available",
        "the opponent may switch to another break or route"
      ),
      viability = PlanViability(
        score = candidate.score,
        label = if candidate.score >= 0.72 then "high" else "medium",
        risk = "restriction can fail if the opponent switches to another resource"
      ),
      refutation = Some("change plans if a replacement counterplay route appears"),
      evidenceSources = List(
        ThemeResolver.themeTag(candidate.kind.theme),
        ThemeResolver.subplanTag(candidate.kind),
        ThemeResolver.structuralStateTag("restriction_candidate"),
        "restriction_evidence"
      ) ++ candidate.reasons,
      themeL1 = candidate.kind.theme.id,
      subplanId = Some(candidate.kind.id)
    )

  private def restrictionWhyText(candidate: RestrictionCandidate): List[String] =
    val breakDrop = candidate.opponentBreaksBefore - candidate.opponentBreaksAfter
    val mobilityDrop = candidate.opponentMobilityBefore - candidate.opponentMobilityAfter
    List(
      Option.when(breakDrop > 0)(
        s"it cuts opponent break choices from ${candidate.opponentBreaksBefore} to ${candidate.opponentBreaksAfter}"
      ),
      Option.when(mobilityDrop > 0)(
        s"it reduces opponent piece mobility from ${candidate.opponentMobilityBefore} to ${candidate.opponentMobilityAfter}"
      ),
      Option.when(candidate.reasons.exists(_.startsWith("denied_square:")))(
        s"${candidate.destination.key} was an available entry square before the move"
      ),
      Option.when(candidate.reasons.contains("central_control_gain"))(
        "the move increases control over central squares"
      ),
      Option.when(candidate.reasons.contains("flank_contact_clamp"))(
        "the pawn move fixes contact on the flank instead of leaving it loose"
      ),
      restrictedResourceText(candidate)
    ).flatten

  private def restrictionExecutionText(candidate: RestrictionCandidate, moveText: String): List[String] =
    val first =
      candidate.kind match
        case PlanKind.BreakPrevention =>
          s"play $moveText before the counter-break becomes easy"
        case PlanKind.KeySquareDenial =>
          s"play $moveText to occupy ${candidate.destination.key}"
        case PlanKind.MobilitySuppression =>
          s"play $moveText to shrink the opponent's active routes"
        case PlanKind.CentralSpaceBind =>
          s"play $moveText to hold more central squares"
        case PlanKind.FlankClamp =>
          s"play $moveText to keep flank contact fixed"
        case _ =>
          s"play $moveText as the restriction move"
    List(first, "then check whether the opponent has a replacement break or route")

  private def restrictedResourceText(candidate: RestrictionCandidate): Option[String] =
    candidate.reasons
      .find(_.startsWith("restricted_resources:"))
      .map(_.stripPrefix("restricted_resources:").split(",").toList.map(_.trim).filter(_.nonEmpty))
      .filter(_.nonEmpty)
      .map(resources => s"it also puts pressure on ${resources.take(3).mkString(", ")}")

  private def displayMove(uci: String): String =
    val clean = Option(uci).getOrElse("").trim
    if clean.length >= 4 then s"${clean.take(2)}-${clean.slice(2, 4)}" else clean

  private def pawnBreakMoves(pos: Position, color: Color): List[Move] =
    val oriented = orientedPosition(pos, color)
    oriented.legalMoves.toList.filter { mv =>
      mv.piece.role == Pawn &&
        advancesTowardPromotion(mv, color) &&
        (mv.captures || createsEnemyPawnContact(mv.after.board, mv.dest, color))
    }

  private def nonKingMobility(pos: Position, color: Color): Int =
    val oriented = orientedPosition(pos, color)
    oriented.legalMoves.toList.count(mv => mv.piece.color == color && mv.piece.role != King)

  private def touchesBreakFiles(mv: Move, breaks: List[Move]): Boolean =
    breaks.exists(br =>
      (br.orig.file.value - mv.dest.file.value).abs <= 1 ||
        (br.dest.file.value - mv.dest.file.value).abs <= 1
    )

  private def centralControlDelta(mv: Move): Int =
    val central = List(Square.C4, Square.D4, Square.E4, Square.F4, Square.C5, Square.D5, Square.E5, Square.F5)
    val color = mv.piece.color
    val before = central.count(square => mv.before.board.attackers(square, color).nonEmpty)
    val after = central.count(square => mv.after.board.attackers(square, color).nonEmpty)
    after - before
