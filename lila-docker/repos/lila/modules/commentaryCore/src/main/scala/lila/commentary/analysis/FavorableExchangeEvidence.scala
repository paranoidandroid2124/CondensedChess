package lila.commentary.analysis

import chess.*
import chess.format.Fen
import lila.commentary.analysis.PlanTaxonomy.{ PlanKind, PlanTheme, ThemeResolver }
import lila.commentary.analysis.PlanMoveEvidenceSupport.*
import lila.commentary.analysis.strategic.ActivityAnalyzerImpl
import lila.commentary.analysis.structure.WeaknessTargetProfile
import lila.commentary.model.strategic.PieceActivity
import lila.commentary.model.authoring.{ PlanHypothesis, PlanViability }

object FavorableExchangeEvidence:

  final case class ExchangeCandidate(
      kind: PlanKind,
      moveUci: String,
      movingRole: Role,
      capturedRole: Role,
      targetSquare: Option[String],
      materialBalance: Int,
      score: Double,
      reasons: List[String]
  )

  private val Analyzer = ActivityAnalyzerImpl()

  def candidatesFromFen(fen: String, color: Color): List[ExchangeCandidate] =
    Fen
      .read(chess.variant.Standard, Fen.Full(fen))
      .toList
      .flatMap { pos =>
        val oriented = orientedPosition(pos, color)
        candidates(oriented, color)
      }

  def candidates(pos: Position, color: Color): List[ExchangeCandidate] =
    val oriented = orientedPosition(pos, color)
    val board = oriented.board
    val weaknessTargets =
      WeaknessTargetProfile
        .targetsForPressure(board, color)
        .flatMap(target => Square.fromKey(target.targetSquare).map(_ -> target))
    val activityBySquare =
      Analyzer.analyze(board, color).map(activity => activity.square -> activity).toMap
    val balance = materialBalance(board, color)
    oriented.legalMoves.toList
      .flatMap(exchangeCandidate(board, color, weaknessTargets, activityBySquare, balance, _))
      .sortBy(c => (-c.score, c.kind.id, c.moveUci))
      .distinctBy(c => s"${c.kind.id}|${c.moveUci}|${c.targetSquare.getOrElse("")}")

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

  private def exchangeCandidate(
      board: Board,
      color: Color,
      weaknessTargets: List[(Square, WeaknessTargetProfile)],
      activityBySquare: Map[Square, PieceActivity],
      balance: Int,
      mv: Move
  ): List[ExchangeCandidate] =
    if mv.piece.color != color || !mv.captures || mv.promotion.nonEmpty then Nil
    else
      board.pieceAt(mv.dest).filter(_.color == !color).toList.flatMap { captured =>
        val recaptureDefenders = nonKingDefendersOf(board, !color, mv.dest)
        val queenTrade =
          Option.when(
            mv.piece.role == Queen &&
              captured.role == Queen &&
              recaptureDefenders.nonEmpty
          ) {
            mkCandidate(
              kind = PlanKind.QueenTradeShield,
              mv = mv,
              captured = captured,
              target = None,
              reasons = List(
                "queen_trade_candidate",
                s"recapture_defenders:${recaptureDefenders.map(_.key).sorted.mkString(",")}"
              ),
              base = 0.74
            )
          }
        val defenderTrades =
          weaknessTargets.flatMap { case (targetSq, target) =>
            val defender = board.attackers(targetSq, !color).intersects(mv.dest.bb)
            Option.when(defender && mv.dest != targetSq) {
              mkCandidate(
                kind = PlanKind.DefenderTrade,
                mv = mv,
                captured = captured,
                target = Some(target.targetSquare),
                reasons = target.evidenceTerms ++ List("captures_target_defender"),
                base = 0.70
              )
            }
          }
        val materialDelta = pieceValue(captured.role) - pieceValue(mv.piece.role)
        val simplificationLike =
          recaptureDefenders.nonEmpty &&
            materialDelta >= 0 &&
            materialDelta <= 3
        val favorableCapture =
          Option.when(simplificationLike) {
            mkCandidate(
              kind = PlanKind.SimplificationWindow,
              mv = mv,
              captured = captured,
              target = None,
              reasons = List(
                "capture_simplifies_material",
                s"captured_role:${captured.role.toString.toLowerCase}",
                s"recapture_defenders:${recaptureDefenders.map(_.key).sorted.mkString(",")}"
              ),
              base = 0.62
            )
          }
        val badPieceLiquidation =
          activityBySquare.get(mv.orig).filter(isBadPieceForLiquidation).flatMap { activity =>
            Option.when(captured.role != Pawn || pieceValue(captured.role) >= pieceValue(mv.piece.role)) {
              mkCandidate(
                kind = PlanKind.BadPieceLiquidation,
                mv = mv,
                captured = captured,
                target = None,
                balance = balance,
                reasons = List(
                  "liquidates_bad_piece",
                  s"bad_piece:${activity.piece.toString.toLowerCase}",
                  Option.when(activity.isBadBishop)("bad_bishop").getOrElse("low_mobility_piece")
                ),
                base = 0.68
              )
            }
          }
        (queenTrade.toList ++ defenderTrades ++ favorableCapture.toList ++ badPieceLiquidation.toList)
          .filter(_.score >= 0.58)
      }

  private def mkCandidate(
      kind: PlanKind,
      mv: Move,
      captured: Piece,
      target: Option[String],
      reasons: List[String],
      base: Double,
      balance: Int = 0
  ): ExchangeCandidate =
    val materialDelta = pieceValue(captured.role) - pieceValue(mv.piece.role)
    val score =
      (base +
        materialDelta.max(0).min(5) * 0.025 +
        (if captured.role == Queen then 0.08 else 0.0) +
        (if target.nonEmpty then 0.05 else 0.0))
        .max(0.30)
        .min(0.88)
    ExchangeCandidate(
      kind = kind,
      moveUci = mv.toUci.uci,
      movingRole = mv.piece.role,
      capturedRole = captured.role,
      targetSquare = target,
      materialBalance = balance,
      score = score,
      reasons = reasons.distinct
    )

  private def toHypothesis(candidate: ExchangeCandidate): PlanHypothesis =
    val moveText = displayMove(candidate.moveUci)
    val movingRole = roleName(candidate.movingRole)
    val capturedRole = roleName(candidate.capturedRole)
    val planName =
      candidate.kind match
        case PlanKind.QueenTradeShield =>
          s"Trade queens with $moveText"
        case PlanKind.DefenderTrade =>
          s"Remove the defender of ${candidate.targetSquare.getOrElse("the target")} with $moveText"
        case PlanKind.BadPieceLiquidation =>
          s"Liquidate the bad $movingRole with $moveText"
        case _ =>
          s"Use $moveText as a simplification window"
    val theme =
      PlanTheme.FavorableExchange
    val structuralTag =
      candidate.kind match
        case PlanKind.BadPieceLiquidation      => "bad_piece_liquidation"
        case _                                 => "favorable_exchange"
    PlanHypothesis(
      planId = candidate.kind.id,
      planName = planName,
      rank = 0,
      score = candidate.score,
      preconditions = exchangeWhyText(candidate, moveText, movingRole, capturedRole).distinct.take(5),
      executionSteps = exchangeExecutionText(candidate, moveText, movingRole, capturedRole),
      failureModes = List(
        "the recapture can activate the opponent's pieces",
        "the target may disappear without the trade winning anything"
      ),
      viability = PlanViability(
        score = candidate.score,
        label = if candidate.score >= 0.72 then "high" else "medium",
        risk = "exchange plan can fail if the recapture activates the opponent"
      ),
      refutation = Some("change plans if the trade improves the opponent's coordination"),
      evidenceSources = List(
        ThemeResolver.themeTag(theme),
        ThemeResolver.subplanTag(candidate.kind),
        ThemeResolver.structuralStateTag(structuralTag),
        "favorable_exchange"
      ) ++ candidate.reasons,
      themeL1 = theme.id,
      subplanId = Some(candidate.kind.id)
    )

  private def exchangeWhyText(
      candidate: ExchangeCandidate,
      moveText: String,
      movingRole: String,
      capturedRole: String
  ): List[String] =
    val recaptureDefenders = reasonTargets(candidate.reasons, "recapture_defenders:")
    val why =
      List(
        Some(s"$moveText is a concrete exchange with the $movingRole"),
        Some(s"the exchange removes a $capturedRole"),
        Option.when(candidate.kind == PlanKind.QueenTradeShield)(
          if recaptureDefenders.nonEmpty then s"queen trade has recapture cover from ${targetsText(recaptureDefenders)}"
          else "queen trade has recapture cover"
        ),
        candidate.targetSquare.map(target =>
          if candidate.reasons.contains("captures_target_defender") then
            s"the captured piece was defending the target on $target"
          else s"the exchange is tied to the target on $target"
        ),
        Option.when(candidate.reasons.contains("capture_simplifies_material"))(
          "the capture simplifies material without relying on a large tactic"
        ),
        Option.when(candidate.reasons.contains("liquidates_bad_piece"))(
          s"the $movingRole was a poor piece before the exchange"
        ),
        Option.when(candidate.reasons.contains("bad_bishop"))(
          "the exchange frees a bad bishop from the position"
        ),
        Option.when(candidate.reasons.contains("low_mobility_piece"))(
          "the exchange converts a low-mobility piece into a trade"
        ),
        Option.when(candidate.materialBalance > 0)(
          s"the side keeps a material edge of ${candidate.materialBalance}"
        )
      ).flatten
    if why.nonEmpty then why else List(s"$moveText makes the exchange terms favorable")

  private def exchangeExecutionText(
      candidate: ExchangeCandidate,
      moveText: String,
      movingRole: String,
      capturedRole: String
  ): List[String] =
    val first =
      candidate.kind match
        case PlanKind.QueenTradeShield =>
          s"trade queens with $moveText while the recapture stays covered"
        case PlanKind.DefenderTrade =>
          candidate.targetSquare match
            case Some(target) => s"capture the defender with $moveText, then keep pressure on $target"
            case None         => s"capture the defender with $moveText"
        case PlanKind.BadPieceLiquidation =>
          s"use $moveText to trade the bad $movingRole for the $capturedRole"
        case _ =>
          s"use $moveText as the concrete exchange"
    val second =
      if candidate.kind == PlanKind.DefenderTrade then
        "make sure the target does not get a replacement defender"
      else "keep the recapture line from improving the opponent's coordination"
    List(first, second).distinct

  private def reasonTargets(reasons: List[String], prefix: String): List[String] =
    reasonPayload(reasons, prefix)
      .toList
      .flatMap(_.split(",").toList)
      .map(_.trim)
      .filter(_.nonEmpty)
      .distinct
      .sorted

  private def targetsText(targets: List[String]): String =
    targets.take(3).mkString(", ")

  private def reasonPayload(reasons: List[String], prefix: String): Option[String] =
    reasons.collectFirst {
      case reason if reason.startsWith(prefix) => reason.drop(prefix.length).trim
    }.filter(_.nonEmpty)

  private def displayMove(uci: String): String =
    if uci.length >= 4 then s"${uci.take(2)}-${uci.slice(2, 4)}" else uci

  private def roleName(role: Role): String =
    role match
      case Pawn   => "pawn"
      case Knight => "knight"
      case Bishop => "bishop"
      case Rook   => "rook"
      case Queen  => "queen"
      case King   => "king"

  private def isBadPieceForLiquidation(activity: PieceActivity): Boolean =
    activity.piece == Bishop && activity.isBadBishop ||
      activity.isTrapped ||
      ((activity.piece == Bishop || activity.piece == Knight) && activity.mobilityScore <= 0.30)

  private def nonKingDefendersOf(board: Board, color: Color, square: Square): List[Square] =
    board.attackers(square, color).squares.toList.filter { defender =>
      board.pieceAt(defender).exists(piece => piece.color == color && piece.role != King)
    }
