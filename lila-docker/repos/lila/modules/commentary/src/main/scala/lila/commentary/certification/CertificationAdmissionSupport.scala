package lila.commentary.certification

import chess.Color

import lila.commentary.delta.StrategicDeltaExtraction
import lila.commentary.strategic.StrategicObjectExtraction

private[certification] object CertificationAdmissionSupport:

  def missingSupportFamilies(
      requiredSupportFamilies: Vector[CertificationSupportFamily],
      current: StrategicObjectExtraction,
      delta: Option[StrategicDeltaExtraction],
      extractedSoFar: CertificationSet
  ): Vector[CertificationSupportFamily] =
    requiredSupportFamilies.filterNot(family =>
      hasSupportFamily(
        family = family,
        color = None,
        current = current,
        delta = delta,
        extractedSoFar = extractedSoFar
      )
    )

  def missingSupportFamilies(
      requiredSupportFamilies: Vector[CertificationSupportFamily],
      color: Color,
      current: StrategicObjectExtraction,
      delta: Option[StrategicDeltaExtraction],
      extractedSoFar: CertificationSet
  ): Vector[CertificationSupportFamily] =
    requiredSupportFamilies.filterNot(family =>
      hasSupportFamily(
        family = family,
        color = Some(color),
        current = current,
        delta = delta,
        extractedSoFar = extractedSoFar
      )
    )

  private def hasSupportFamily(
      family: CertificationSupportFamily,
      color: Option[Color],
      current: StrategicObjectExtraction,
      delta: Option[StrategicDeltaExtraction],
      extractedSoFar: CertificationSet
  ): Boolean =
    hasObjectFamily(family, color, current) ||
      hasDeltaFamily(family, color, delta) ||
      hasCertificationFamily(family, color, extractedSoFar)

  private def hasObjectFamily(
      family: CertificationSupportFamily,
      color: Option[Color],
      current: StrategicObjectExtraction
  ): Boolean =
    current.objects.forFamilyId(family.value).exists(obj =>
      color match
        case Some(owner) => obj.color.forall(_ == owner)
        case None => true
    )

  private def hasDeltaFamily(
      family: CertificationSupportFamily,
      color: Option[Color],
      delta: Option[StrategicDeltaExtraction]
  ): Boolean =
    delta.exists(_.deltas.forFamilyId(family.value).exists(deltaObj =>
      color match
        case Some(owner) => deltaObj.color.forall(_ == owner)
        case None => true
    ))

  private def hasCertificationFamily(
      family: CertificationSupportFamily,
      color: Option[Color],
      extractedSoFar: CertificationSet
  ): Boolean =
    extractedSoFar.forFamilyId(family.value).exists(cert =>
      cert.verdict != CertificationVerdict.Rejected &&
        (color match
          case Some(owner) => cert.owner.forall(_ == owner)
          case None => true)
    )
