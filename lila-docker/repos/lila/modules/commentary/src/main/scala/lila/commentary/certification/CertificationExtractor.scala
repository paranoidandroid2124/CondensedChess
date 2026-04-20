package lila.commentary.certification

import scala.util.Try

import lila.commentary.delta.StrategicDeltaExtraction
import lila.commentary.strategic.StrategicObjectExtraction

object CertificationExtractor:

  def fromExtractions(
      currentExtraction: StrategicObjectExtraction,
      deltaExtraction: Option[StrategicDeltaExtraction],
      evidenceBundle: CertificationEvidenceBundle
  ): Either[String, CertificationExtraction] =
    CertificationContext
      .build(currentExtraction, evidenceBundle, deltaExtraction)
      .map: context =>
        CertificationExtraction(
          current = currentExtraction,
          delta = deltaExtraction,
          evidence = evidenceBundle,
          claims = CertificationInternalRuntime.extract(context)
        )

  def fromObjectExtraction(
      currentExtraction: StrategicObjectExtraction,
      evidenceBundle: CertificationEvidenceBundle
  ): Either[String, CertificationExtraction] =
    fromExtractions(currentExtraction, None, evidenceBundle)

  def fromDeltaExtraction(
      deltaExtraction: StrategicDeltaExtraction,
      evidenceBundle: CertificationEvidenceBundle
  ): Either[String, CertificationExtraction] =
    fromExtractions(deltaExtraction.after, Some(deltaExtraction), evidenceBundle)

  def fromObjectExtractionFailClosed(
      currentExtraction: StrategicObjectExtraction,
      evidenceBundle: CertificationEvidenceBundle
  ): Either[String, CertificationExtraction] =
    closeFailClosed(fromObjectExtraction(currentExtraction, evidenceBundle))

  def fromDeltaExtractionFailClosed(
      deltaExtraction: StrategicDeltaExtraction,
      evidenceBundle: CertificationEvidenceBundle
  ): Either[String, CertificationExtraction] =
    closeFailClosed(fromDeltaExtraction(deltaExtraction, evidenceBundle))

  private def closeFailClosed(
      extraction: => Either[String, CertificationExtraction]
  ): Either[String, CertificationExtraction] =
    Try(extraction).toEither match
      case Right(result) => result
      case Left(error) =>
        val message = Option(error.getMessage).filter(_.nonEmpty).getOrElse(error.getClass.getSimpleName)
        Left(s"Certification extraction failed closed: $message")
