package lila.commentary

import chess.format.{ Fen, Uci }

import lila.commentary.certification.{
  CertificationEvidenceBundle,
  CertificationExtraction,
  CertificationExtractor,
  CertificationScopeContract
}
import lila.commentary.delta.{ StrategicDeltaExtraction, StrategicDeltaExtractor, StrategicDeltaScopeContract }
import lila.commentary.root.RootStateVector
import lila.commentary.strategic.{
  StrategicObjectExtraction,
  StrategicObjectExtractor,
  StrategicObjectScopeContract
}
import lila.commentary.witness.u.{
  UAttachedExtraction,
  UAttachedExtractor,
  UAttachedScopeContract,
  UScopeContract,
  UWitnessExtraction,
  UWitnessExtractor
}

/** Public entry point for the experimental commentary backend reset. */
object CommentaryCore:

  val activeUPrimaryDescriptorIds: Vector[String] =
    UScopeContract.activePrimaryDescriptorIds.map(_.value)

  val activeUAttachedDescriptorIds: Vector[String] =
    UAttachedScopeContract.activeAttachedDescriptorIds.map(_.value)

  val activeObjectFamilyIds: Vector[String] =
    StrategicObjectScopeContract.activeObjectFamilyIds.map(_.value)

  val activeDeltaFamilyIds: Vector[String] =
    StrategicDeltaScopeContract.activeDeltaFamilyIds.map(_.value)

  val activeCertificationFamilyIds: Vector[String] =
    CertificationScopeContract.activeCertificationFamilyIds.map(_.value)

  def extractUWitnesses(rootState: RootStateVector): UWitnessExtraction =
    UWitnessExtractor.fromRoot(rootState)

  def extractUWitnesses(fen: Fen.Full): Either[String, UWitnessExtraction] =
    UWitnessExtractor.fromFen(fen)

  def extractUWitnessesFromFen(fen: String): Either[String, UWitnessExtraction] =
    extractUWitnesses(Fen.Full.clean(fen))

  def extractUWitnessesFailClosed(fen: Fen.Full): Either[String, UWitnessExtraction] =
    UWitnessExtractor.fromFenFailClosed(fen)

  def extractUWitnessesFromFenFailClosed(fen: String): Either[String, UWitnessExtraction] =
    extractUWitnessesFailClosed(Fen.Full.clean(fen))

  def extractUAttachedWitnesses(rootState: RootStateVector): UAttachedExtraction =
    UAttachedExtractor.fromRoot(rootState)

  def extractUAttachedWitnesses(fen: Fen.Full): Either[String, UAttachedExtraction] =
    UAttachedExtractor.fromFen(fen)

  def extractUAttachedWitnessesFromFen(fen: String): Either[String, UAttachedExtraction] =
    extractUAttachedWitnesses(Fen.Full.clean(fen))

  def extractUAttachedWitnessesFailClosed(fen: Fen.Full): Either[String, UAttachedExtraction] =
    UAttachedExtractor.fromFenFailClosed(fen)

  def extractUAttachedWitnessesFromFenFailClosed(
      fen: String
  ): Either[String, UAttachedExtraction] =
    extractUAttachedWitnessesFailClosed(Fen.Full.clean(fen))

  def extractStrategicObjects(rootState: RootStateVector): StrategicObjectExtraction =
    StrategicObjectExtractor.fromRoot(rootState)

  def extractStrategicObjects(fen: Fen.Full): Either[String, StrategicObjectExtraction] =
    StrategicObjectExtractor.fromFen(fen)

  def extractStrategicObjectsFromFen(fen: String): Either[String, StrategicObjectExtraction] =
    extractStrategicObjects(Fen.Full.clean(fen))

  def extractStrategicObjectsFailClosed(
      fen: Fen.Full
  ): Either[String, StrategicObjectExtraction] =
    StrategicObjectExtractor.fromFenFailClosed(fen)

  def extractStrategicObjectsFromFenFailClosed(
      fen: String
  ): Either[String, StrategicObjectExtraction] =
    extractStrategicObjectsFailClosed(Fen.Full.clean(fen))

  def extractStrategicDeltas(
      beforeExtraction: StrategicObjectExtraction,
      afterExtraction: StrategicObjectExtraction,
      playedMove: Uci.Move
  ): Either[String, StrategicDeltaExtraction] =
    StrategicDeltaExtractor.fromExtractions(beforeExtraction, afterExtraction, playedMove)

  def extractStrategicDeltas(
      fenBefore: Fen.Full,
      playedMove: Uci.Move,
      fenAfter: Fen.Full
  ): Either[String, StrategicDeltaExtraction] =
    StrategicDeltaExtractor.fromFens(fenBefore, playedMove, fenAfter)

  def extractStrategicDeltasFromFens(
      fenBefore: String,
      playedMove: String,
      fenAfter: String
  ): Either[String, StrategicDeltaExtraction] =
    for
      move <- parseMove(playedMove)
      extraction <- extractStrategicDeltas(
        Fen.Full.clean(fenBefore),
        move,
        Fen.Full.clean(fenAfter)
      )
    yield extraction

  def extractStrategicDeltasFailClosed(
      fenBefore: Fen.Full,
      playedMove: Uci.Move,
      fenAfter: Fen.Full
  ): Either[String, StrategicDeltaExtraction] =
    StrategicDeltaExtractor.fromFensFailClosed(fenBefore, playedMove, fenAfter)

  def extractStrategicDeltasFromFensFailClosed(
      fenBefore: String,
      playedMove: String,
      fenAfter: String
  ): Either[String, StrategicDeltaExtraction] =
    for
      move <- parseMove(playedMove)
      extraction <- extractStrategicDeltasFailClosed(
        Fen.Full.clean(fenBefore),
        move,
        Fen.Full.clean(fenAfter)
      )
    yield extraction

  def extractCertifications(
      currentExtraction: StrategicObjectExtraction,
      deltaExtraction: Option[StrategicDeltaExtraction],
      evidenceBundle: CertificationEvidenceBundle
  ): Either[String, CertificationExtraction] =
    CertificationExtractor.fromExtractions(currentExtraction, deltaExtraction, evidenceBundle)

  def extractCertifications(
      currentExtraction: StrategicObjectExtraction,
      evidenceBundle: CertificationEvidenceBundle
  ): Either[String, CertificationExtraction] =
    CertificationExtractor.fromObjectExtraction(currentExtraction, evidenceBundle)

  def extractCertifications(
      deltaExtraction: StrategicDeltaExtraction,
      evidenceBundle: CertificationEvidenceBundle
  ): Either[String, CertificationExtraction] =
    CertificationExtractor.fromDeltaExtraction(deltaExtraction, evidenceBundle)

  def extractCertificationsFailClosed(
      currentExtraction: StrategicObjectExtraction,
      evidenceBundle: CertificationEvidenceBundle
  ): Either[String, CertificationExtraction] =
    CertificationExtractor.fromObjectExtractionFailClosed(currentExtraction, evidenceBundle)

  def extractCertificationsFailClosed(
      deltaExtraction: StrategicDeltaExtraction,
      evidenceBundle: CertificationEvidenceBundle
  ): Either[String, CertificationExtraction] =
    CertificationExtractor.fromDeltaExtractionFailClosed(deltaExtraction, evidenceBundle)

  private def parseMove(playedMove: String): Either[String, Uci.Move] =
    Uci(playedMove) match
      case Some(move: Uci.Move) => Right(move)
      case Some(_) => Left(s"Unsupported non-move UCI: $playedMove")
      case None => Left(s"Invalid UCI move: $playedMove")
