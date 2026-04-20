package lila.commentary.delta

import chess.format.Uci
import chess.{ Bishop, Color, King, Knight, Queen, Rook, Square }

import lila.commentary.strategic.{ StrategicObjectContext, StrategicObjectExtraction }
import lila.commentary.witness.WitnessAnchor

private[commentary] final case class StrategicDeltaContext private (
    beforeExtraction: StrategicObjectExtraction,
    afterExtraction: StrategicObjectExtraction,
    playedMove: Uci.Move
):

  val before = StrategicObjectContext(
    beforeExtraction.rootState,
    beforeExtraction.primaryWitnesses,
    beforeExtraction.attachedWitnesses
  )
  val after = StrategicObjectContext(
    afterExtraction.rootState,
    afterExtraction.primaryWitnesses,
    afterExtraction.attachedWitnesses
  )

  lazy val movingPieceBefore = before.pieceAt(playedMove.orig)
  lazy val movedPieceAfter = after.pieceAt(playedMove.dest)
  lazy val destinationPieceBefore = before.pieceAt(playedMove.dest)

  def moverColor: Option[Color] = movingPieceBefore.map(_.color)

  def captureOccurred: Boolean =
    movingPieceBefore.exists(piece =>
      destinationPieceBefore.exists(target => target.color != piece.color)
    )

  def capturesNonKingPiece: Boolean =
    movingPieceBefore.exists(piece =>
      destinationPieceBefore.exists(target => target.color != piece.color && target.role != King)
    )

  def nonKingNonPawnCount(context: StrategicObjectContext): Int =
    Vector(Knight, Bishop, Rook, Queen)
      .map(role =>
        context.activePieceSquares(Color.White, role).size +
          context.activePieceSquares(Color.Black, role).size
      )
      .sum

  def nonKingNonPawnReduction: Int =
    nonKingNonPawnCount(before) - nonKingNonPawnCount(after)

  def queensRemain(context: StrategicObjectContext): Boolean =
    context.activePieceSquares(Color.White, Queen).nonEmpty ||
      context.activePieceSquares(Color.Black, Queen).nonEmpty

  def hasAnchoredObject(
      extraction: StrategicObjectExtraction,
      family: String,
      anchor: WitnessAnchor
  ): Boolean =
    extraction.objects.forFamilyId(family).exists(_.anchor == anchor)

  def sameAnchoredObjectPersists(
      family: String,
      anchor: WitnessAnchor
  ): Boolean =
    hasAnchoredObject(beforeExtraction, family, anchor) &&
      hasAnchoredObject(afterExtraction, family, anchor)

private[commentary] object StrategicDeltaContext:

  def build(
      beforeExtraction: StrategicObjectExtraction,
      afterExtraction: StrategicObjectExtraction,
      playedMove: Uci.Move
  ): Either[String, StrategicDeltaContext] =
    val context = StrategicDeltaContext(beforeExtraction, afterExtraction, playedMove)
    validate(context).map(_ => context)

  private def validate(context: StrategicDeltaContext): Either[String, Unit] =
    for
      originPiece <- context.movingPieceBefore.toRight(
        s"No moving piece on ${context.playedMove.orig.key} for ${context.playedMove.uci}"
      )
      movedPiece <- context.movedPieceAfter.toRight(
        s"No moved piece on ${context.playedMove.dest.key} for ${context.playedMove.uci}"
      )
      _ <- Either.cond(
        context.after.pieceAt(context.playedMove.orig).isEmpty,
        (),
        s"Move ${context.playedMove.uci} leaves a piece on ${context.playedMove.orig.key}"
      )
      _ <- Either.cond(
        movedPiece.color == originPiece.color,
        (),
        s"Move ${context.playedMove.uci} changes mover color between boards"
      )
      _ <- Either.cond(
        movedPiece.role == context.playedMove.promotion.getOrElse(originPiece.role),
        (),
        s"Move ${context.playedMove.uci} lands the wrong role on ${context.playedMove.dest.key}"
      )
      _ <- Either.cond(
        context.destinationPieceBefore.forall(_.color != originPiece.color),
        (),
        s"Move ${context.playedMove.uci} tries to land on a same-color piece"
      )
      _ <- Either.cond(
        context.destinationPieceBefore.forall(_.role != King),
        (),
        s"Move ${context.playedMove.uci} must not capture a king"
      )
      _ <- Either.cond(
        pieceMoveShapeIsBoardCoherent(context, originPiece.role, originPiece.color),
        (),
        s"Move ${context.playedMove.uci} is not board-coherent for ${originPiece.role}"
      )
      _ <- Either.cond(
        unrelatedSquaresStayStable(context),
        (),
        s"Move ${context.playedMove.uci} changes unrelated board squares"
      )
    yield ()

  private def pieceMoveShapeIsBoardCoherent(
      context: StrategicDeltaContext,
      role: chess.Role,
      mover: Color
  ): Boolean =
    val captures = context.captureOccurred
    role match
      case chess.Rook =>
        context.playedMove.orig.onSameLine(context.playedMove.dest) &&
          clearSliderPath(context.before, context.playedMove.orig, context.playedMove.dest)
      case chess.Bishop =>
        context.playedMove.orig.onSameDiagonal(context.playedMove.dest) &&
          clearSliderPath(context.before, context.playedMove.orig, context.playedMove.dest)
      case chess.Queen =>
        (context.playedMove.orig.onSameLine(context.playedMove.dest) ||
          context.playedMove.orig.onSameDiagonal(context.playedMove.dest)) &&
          clearSliderPath(context.before, context.playedMove.orig, context.playedMove.dest)
      case chess.King =>
        math.abs(context.playedMove.orig.file.value - context.playedMove.dest.file.value) <= 1 &&
          math.abs(context.playedMove.orig.rank.value - context.playedMove.dest.rank.value) <= 1
      case chess.Pawn =>
        val rankDelta = context.playedMove.dest.rank.value - context.playedMove.orig.rank.value
        val fileDelta = math.abs(context.playedMove.dest.file.value - context.playedMove.orig.file.value)
        val forward = if mover.white then 1 else -1
        if captures then rankDelta == forward && fileDelta == 1
        else
          (rankDelta == forward && fileDelta == 0 && context.before.pieceAt(context.playedMove.dest).isEmpty) ||
            pawnDoublePushIsBoardCoherent(context, mover)
      case chess.Knight =>
        val fileDelta = math.abs(context.playedMove.orig.file.value - context.playedMove.dest.file.value)
        val rankDelta = math.abs(context.playedMove.orig.rank.value - context.playedMove.dest.rank.value)
        Set((1, 2), (2, 1)).contains((fileDelta, rankDelta))

  private def pawnDoublePushIsBoardCoherent(
      context: StrategicDeltaContext,
      mover: Color
  ): Boolean =
    val forward = if mover.white then 1 else -1
    val homeRank = if mover.white then 1 else 6
    val rankDelta = context.playedMove.dest.rank.value - context.playedMove.orig.rank.value
    val fileDelta = math.abs(context.playedMove.dest.file.value - context.playedMove.orig.file.value)
    val intermediate =
      Square.at(
        context.playedMove.orig.file.value,
        context.playedMove.orig.rank.value + forward
      )
    context.playedMove.orig.rank.value == homeRank &&
    rankDelta == 2 * forward &&
    fileDelta == 0 &&
    intermediate.exists(square => context.before.pieceAt(square).isEmpty) &&
    context.before.pieceAt(context.playedMove.dest).isEmpty

  private def clearSliderPath(
      context: StrategicObjectContext,
      origin: Square,
      destination: Square
  ): Boolean =
    val fileStep = Integer.compare(destination.file.value, origin.file.value)
    val rankStep = Integer.compare(destination.rank.value, origin.rank.value)
    LazyList
      .iterate((origin.file.value + fileStep, origin.rank.value + rankStep)) {
        case (fileValue, rankValue) => (fileValue + fileStep, rankValue + rankStep)
      }
      .takeWhile { case (fileValue, rankValue) =>
        fileValue != destination.file.value || rankValue != destination.rank.value
      }
      .forall { case (fileValue, rankValue) =>
        Square.at(fileValue, rankValue).forall(square => context.pieceAt(square).isEmpty)
      }

  private def unrelatedSquaresStayStable(context: StrategicDeltaContext): Boolean =
    Square.all.forall: square =>
      if square == context.playedMove.orig || square == context.playedMove.dest then true
      else context.before.pieceAt(square) == context.after.pieceAt(square)
