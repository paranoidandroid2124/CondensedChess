package lila.commentary.delta

import chess.format.Uci
import chess.{ Bishop, Color, King, Knight, Queen, Rook }

import lila.commentary.root.RootPositionSupport
import lila.commentary.strategic.{ StrategicObjectContext, StrategicObjectExtraction, StrategicObjectExtractor }
import lila.commentary.witness.WitnessAnchor

private[commentary] final case class StrategicDeltaContext private (
    beforeExtraction: StrategicObjectExtraction,
    afterExtraction: StrategicObjectExtraction,
    playedMove: Uci.Move,
    exactMove: chess.Move
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
  lazy val destinationPieceBefore = before.pieceAt(playedMove.dest)
  lazy val capturedPieceBefore = exactMove.capture.flatMap(before.pieceAt)

  def moverColor: Option[Color] = movingPieceBefore.map(_.color)

  def captureOccurred: Boolean =
    capturedPieceBefore.nonEmpty

  def capturesNonKingPiece: Boolean =
    capturedPieceBefore.exists(_.role != King)

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
    validate(beforeExtraction, afterExtraction, playedMove).map:
      (canonicalBefore, canonicalAfter, exactMove) =>
        StrategicDeltaContext(canonicalBefore, canonicalAfter, playedMove, exactMove)

  private def validate(
      beforeExtraction: StrategicObjectExtraction,
      afterExtraction: StrategicObjectExtraction,
      playedMove: Uci.Move
  ): Either[String, (StrategicObjectExtraction, StrategicObjectExtraction, chess.Move)] =
    for
      canonicalBefore <- StrategicObjectExtractor
        .validateCanonical(beforeExtraction)
        .left
        .map(message => s"Before object extraction canonicalization failed: $message")
      canonicalAfter <- StrategicObjectExtractor
        .validateCanonical(afterExtraction)
        .left
        .map(message => s"After object extraction canonicalization failed: $message")
      exactMove <- validateExactTransition(canonicalBefore, canonicalAfter, playedMove)
    yield (canonicalBefore, canonicalAfter, exactMove)

  private def validateExactTransition(
      beforeExtraction: StrategicObjectExtraction,
      afterExtraction: StrategicObjectExtraction,
      playedMove: Uci.Move
  ): Either[String, chess.Move] =
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
    for
      beforePosition <- RootPositionSupport.exactPosition(beforeExtraction.rootState)
        .left
        .map(message => s"Before exact position reconstruction failed: $message")
      afterPosition <- RootPositionSupport.exactPosition(afterExtraction.rootState)
        .left
        .map(message => s"After exact position reconstruction failed: $message")
      originPiece <- before.pieceAt(playedMove.orig).toRight(
        s"No moving piece on ${playedMove.orig.key} for ${playedMove.uci}"
      )
      _ <- Either.cond(
        beforePosition.color == originPiece.color,
        (),
        s"Move ${playedMove.uci} is for ${originPiece.color.name} on the wrong side to move"
      )
      exactMove <- beforePosition
        .move(playedMove)
        .left
        .map(error => s"Move ${playedMove.uci} is not legal on the exact before-state: $error")
      _ <- Either.cond(
        exactMove.after.position.board == afterPosition.board &&
          exactMove.after.position.color == afterPosition.color &&
          exactMove.after.position.history.castles.value == afterPosition.history.castles.value &&
          exactMove.after.position.enPassantSquare == afterPosition.enPassantSquare &&
          after.pieceAt(playedMove.orig).isEmpty,
        (),
        s"Move ${playedMove.uci} yields an exact after-state mismatch"
      )
    yield exactMove
