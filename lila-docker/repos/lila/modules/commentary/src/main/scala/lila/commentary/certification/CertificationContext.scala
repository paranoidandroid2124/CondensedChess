package lila.commentary.certification

import chess.{ Color, Position }

import lila.commentary.delta.{ StrategicDeltaExtraction, StrategicDeltaExtractor }
import lila.commentary.root.RootPositionSupport
import lila.commentary.strategic.{ StrategicObjectContext, StrategicObjectExtraction, StrategicObjectExtractor }
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
    val position = positionFor(color)
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

  private def positionFor(color: Color): Position =
    RootPositionSupport
      .positionFor(objectExtraction.rootState, color)
      .fold(message => throw IllegalStateException(message), identity)

object CertificationContext:

  def build(
      objectExtraction: StrategicObjectExtraction,
      evidence: CertificationEvidenceBundle,
      deltaExtraction: Option[StrategicDeltaExtraction] = None
  ): Either[String, CertificationContext] =
    val validatedDelta =
      deltaExtraction match
        case Some(delta) if delta.after.rootState != objectExtraction.rootState =>
          Left("Certification extraction must use the current object extraction that matches delta.after")
        case Some(delta) =>
          StrategicDeltaExtractor.validateCanonical(delta).map(Some(_))
        case None => Right(None)

    for
      canonicalCurrent <- StrategicObjectExtractor
        .validateCanonical(objectExtraction)
        .left
        .map(message => s"Certification current extraction canonicalization failed: $message")
      canonicalDelta <- validatedDelta
      _ <- Either.cond(
        evidence.isEmpty || evidence.matches(canonicalCurrent.rootState),
        (),
        "Non-empty certification evidence bundle must be bound to the same current root state"
      )
      _ <- RootPositionSupport.exactPosition(canonicalCurrent.rootState)
    yield CertificationContext(
      objectExtraction = canonicalCurrent,
      evidence = evidence,
      deltaExtraction = canonicalDelta
    )
