package lila.commentary.certification

import chess.{ Color, Queen, Rook }

import lila.commentary.certification.CertificationHelpers.*
import lila.commentary.witness.{ WitnessPayload, WitnessValue }

private[certification] object PerpetualCheckHoldingRule extends CertificationRule:

  val familyId: CertificationId = CertificationId("PerpetualCheckHolding")
  val scope: CertificationScope = CertificationScope.CurrentPosition
  val burdenTag: CertificationBurdenTag = CertificationBurdenTag("perpetual_check_hold")
  protected val helperTags: Vector[String] =
    Vector("perpetual_check_loop", "best_defense_survival")
  protected val requiredEvidencePurposes: Vector[CertificationEvidencePurpose] =
    Vector(CertificationEvidencePurpose.BestDefenseSurvival)
  protected val insufficientEvidenceVerdict: CertificationVerdict =
    CertificationVerdict.Deferred

  def candidateFor(
      color: Color,
      context: CertificationContext,
      extractedSoFar: CertificationSet
  ): Option[CertificationCandidate] =
    Option.when(context.sideToMove == color)(renewableCheckingCycle(context, color)).flatten.map:
      case (checkingPieceSquares, checkingTargets) =>
        CertificationCandidate(
          payload = WitnessPayload(
            "owner" -> WitnessValue.ColorValue(color),
            "checking_piece_squares" -> WitnessValue.SquareListValue(checkingPieceSquares)
          ),
          support = support(targetSquares = checkingTargets)
        )

  private def renewableCheckingCycle(
      context: CertificationContext,
      color: Color
  ): Option[(Vector[chess.Square], Vector[chess.Square])] =
    checkingMoves(context.legalMoves(color), context.current.pieceAt)
      .find: move =>
        val defenderReplies = legalMoves(move.after.position)
        defenderReplies.nonEmpty &&
          defenderReplies.forall(reply => checkingMoves(legalMoves(reply.after.position), reply.after.position.board.pieceAt).nonEmpty)
      .map: move =>
        val defenderReplies = legalMoves(move.after.position)
        val renewalMoves =
          defenderReplies.flatMap(reply =>
            checkingMoves(legalMoves(reply.after.position), reply.after.position.board.pieceAt)
          )
        val checkingPieceSquares =
          (Vector(move.orig) ++ renewalMoves.map(_.orig)).distinct.sortBy(_.value)
        val checkingTargets =
          (Vector(move.dest) ++ renewalMoves.map(_.dest)).distinct.sortBy(_.value)
        checkingPieceSquares -> checkingTargets

  private def checkingMoves(
      moves: Vector[chess.Move],
      pieceAt: chess.Square => Option[chess.Piece]
  ): Vector[chess.Move] =
    moves.filter: move =>
      pieceAt(move.orig).exists(piece =>
        (piece.role == Queen || piece.role == Rook) &&
          move.after.check.yes &&
          legalMoves(move.after.position).nonEmpty
      )
