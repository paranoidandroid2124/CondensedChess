package lila.commentary.certification

import chess.{ Color, Position, Queen, Rook, Square }

import scala.collection.mutable

import lila.commentary.certification.CertificationHelpers.*
import lila.commentary.root.RootPositionSupport
import lila.commentary.witness.{ WitnessPayload, WitnessValue }

private[certification] object PerpetualCheckHoldingRule extends CertificationRule:

  private val MaxRenewalStates = 512

  private final case class RenewalLine(checkingPieceSquares: Set[Square], checkingTargets: Set[Square]):
    def merge(other: RenewalLine): RenewalLine =
      RenewalLine(
        checkingPieceSquares ++ other.checkingPieceSquares,
        checkingTargets ++ other.checkingTargets
      )

  private object RenewalLine:
    val empty: RenewalLine = RenewalLine(Set.empty, Set.empty)

    def fromMove(move: chess.Move): RenewalLine =
      RenewalLine(Set(move.orig), Set(move.dest))

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
    RootPositionSupport
      .positionFor(context.objectExtraction.rootState, color)
      .toOption
      .flatMap(position => forcedRenewalFrom(position, Set.empty, mutable.Map.empty))
      .map: line =>
        val checkingPieceSquares =
          line.checkingPieceSquares.toVector.sortBy(_.value)
        val checkingTargets =
          line.checkingTargets.toVector.sortBy(_.value)
        checkingPieceSquares -> checkingTargets

  private def forcedRenewalFrom(
      position: Position,
      visiting: Set[String],
      memo: mutable.Map[String, Option[RenewalLine]]
  ): Option[RenewalLine] =
    val key = positionKey(position)
    if visiting.contains(key) then Some(RenewalLine.empty)
    else if memo.size > MaxRenewalStates then None
    else if memo.contains(key) then memo(key)
    else
      val result =
        checkingMoves(legalMoves(position), position.board.pieceAt)
        .iterator
        .map: move =>
          val defenderReplies = legalMoves(move.after.position)
          if defenderReplies.isEmpty then None
          else
            val nextVisiting = visiting + key
            defenderReplies.foldLeft(Option(RenewalLine.fromMove(move))):
              case (Some(acc), reply) =>
                forcedRenewalFrom(reply.after.position, nextVisiting, memo).map(acc.merge)
              case (None, _) => None
        .collectFirst { case Some(line) => line }
      memo.update(key, result)
      result

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

  private def positionKey(position: Position): String =
    val pieces =
      Square.all
        .flatMap(square => position.pieceAt(square).map(piece => s"${square.key}:${piece.color}:${piece.role}"))
        .mkString(",")
    val enPassant =
      position.enPassantSquare.map(_.key).getOrElse("-")
    s"$pieces|${position.color}|${position.history.castles.value}|$enPassant"
