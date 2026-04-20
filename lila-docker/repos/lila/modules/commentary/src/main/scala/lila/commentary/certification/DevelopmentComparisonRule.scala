package lila.commentary.certification

import chess.Color

import lila.commentary.certification.CertificationHelpers.*
import lila.commentary.witness.{ WitnessPayload, WitnessSupport, WitnessValue }

private[certification] object DevelopmentComparisonRule extends CertificationRule:

  val familyId: CertificationId = CertificationId("DevelopmentComparison")
  val scope: CertificationScope = CertificationScope.Comparative
  val burdenTag: CertificationBurdenTag = CertificationBurdenTag("development_superiority")
  protected val helperTags: Vector[String] =
    Vector("development_balance_count", "development_gap_floor")
  override protected val requiredSupportFamilies: Vector[CertificationSupportFamily] =
    Vector(CertificationSupportFamily("OpeningDevelopmentRegime"))
  protected val requiredEvidencePurposes: Vector[CertificationEvidencePurpose] =
    Vector(CertificationEvidencePurpose.ComparativeSuperiority)
  protected val insufficientEvidenceVerdict: CertificationVerdict =
    CertificationVerdict.SupportOnly

  def candidateFor(
      color: Color,
      context: CertificationContext,
      extractedSoFar: CertificationSet
  ): Option[CertificationCandidate] =
    val ownerCount = developmentCount(context, color)
    val rivalCount = developmentCount(context, !color)
    val gap = ownerCount - rivalCount
    val openingSupport = openingDevelopmentSupport(context)

    Option.when(ownerCount >= 2 && gap >= 1 && openingSupport.isDefined):
      CertificationCandidate(
        payload = WitnessPayload(
          "owner" -> WitnessValue.ColorValue(color),
          "owner_development_count" -> WitnessValue.Number(ownerCount),
          "rival_development_count" -> WitnessValue.Number(rivalCount),
          "development_gap" -> WitnessValue.Number(gap)
        ),
        support = support(
          indices = openingSupport.toVector.flatMap(_.rootIndices),
          targetSquares = openingSupport.toVector.flatMap(_.targetSquares)
        )
      )

  private def openingDevelopmentSupport(
      context: CertificationContext
  ): Option[WitnessSupport] =
    boardObject(context, "OpeningDevelopmentRegime")
      .map(obj =>
        support(
          indices = obj.support.rootIndices,
          targetSquares = obj.support.targetSquares
        )
      )
