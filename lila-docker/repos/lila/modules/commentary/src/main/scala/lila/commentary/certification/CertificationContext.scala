package lila.commentary.certification

import chess.{ Color, Position }
import chess.variant

import lila.commentary.delta.StrategicDeltaExtraction
import lila.commentary.strategic.{ StrategicObjectContext, StrategicObjectExtraction }
import lila.commentary.witness.WitnessAnchor
import lila.commentary.witness.u.UExtractionContext

private[commentary] final case class CertificationContext(
    objectExtraction: StrategicObjectExtraction,
    evidence: CertificationEvidenceBundle,
    deltaExtraction: Option[StrategicDeltaExtraction] = None
):

  val currentExtraction: StrategicObjectExtraction = objectExtraction

  val objectContext: StrategicObjectContext =
    StrategicObjectContext(
      objectExtraction.rootState,
      objectExtraction.primaryWitnesses,
      objectExtraction.attachedWitnesses
    )

  val current: StrategicObjectContext = objectContext
  val board = objectContext.board

  private val lowLevelContext = UExtractionContext(objectExtraction.rootState)

  lazy val sideToMove: Color =
    lowLevelContext.sideToMove.getOrElse(
      throw IllegalStateException("Certification extraction requires an exact side-to-move bit")
    )

  def legalMoveCount(color: Color): Int =
    legalMoves(color).size

  def legalMoves(color: Color): Vector[chess.Move] =
    val position = Position(board.toBoard, variant.Standard, color)
    board
      .squaresOf(color)
      .flatMap(position.generateMovesAt)
      .sortBy(move => (move.orig.value, move.dest.value))

  def evidenceFor(
      familyId: CertificationId,
      color: Color,
      anchor: WitnessAnchor = WitnessAnchor.BoardAnchor
  ): Option[CertificationEvidenceClaim] =
    evidence.evidenceFor(familyId, color, anchor)

object CertificationContext:

  def build(
      objectExtraction: StrategicObjectExtraction,
      evidence: CertificationEvidenceBundle,
      deltaExtraction: Option[StrategicDeltaExtraction] = None
  ): Either[String, CertificationContext] =
    deltaExtraction match
      case Some(delta) if delta.after.rootState != objectExtraction.rootState =>
        Left("Certification extraction must use the current object extraction that matches delta.after")
      case _ if evidence.isEmpty || evidence.matches(objectExtraction.rootState) =>
        Right(
          CertificationContext(
            objectExtraction = objectExtraction,
            evidence = evidence,
            deltaExtraction = deltaExtraction
          )
        )
      case _ =>
        Left("Non-empty certification evidence bundle must be bound to the same current root state")
