package lila.commentary.certification

import chess.Color

import lila.commentary.witness.{ WitnessAnchor, WitnessPayload, WitnessSupport, WitnessValue }

private[certification] final case class CertificationCandidate(
    payload: WitnessPayload = WitnessPayload.empty,
    support: WitnessSupport = WitnessSupport.empty
)

private[certification] trait CertificationRule:
  def familyId: CertificationId
  def scope: CertificationScope
  def burdenTag: CertificationBurdenTag
  protected def helperTags: Vector[String]
  protected def requiredEvidencePurposes: Vector[CertificationEvidencePurpose]
  protected def insufficientEvidenceVerdict: CertificationVerdict
  protected def requiredSupportFamilies: Vector[CertificationSupportFamily] = Vector.empty

  def extract(
      context: CertificationContext,
      extractedSoFar: CertificationSet
  ): Vector[Certification] =
    Vector(Color.White, Color.Black).map(color => evaluate(color, context, extractedSoFar))

  protected def candidateFor(
      color: Color,
      context: CertificationContext,
      extractedSoFar: CertificationSet
  ): Option[CertificationCandidate]

  private def evaluate(
      color: Color,
      context: CertificationContext,
      extractedSoFar: CertificationSet
  ): Certification =
    val anchor = WitnessAnchor.BoardAnchor
    val missingSupportFamilies =
      CertificationAdmissionSupport.missingSupportFamilies(
        requiredSupportFamilies = requiredSupportFamilies,
        color = color,
        current = context.currentExtraction,
        delta = context.deltaExtraction,
        extractedSoFar = extractedSoFar
      )

    if missingSupportFamilies.nonEmpty then
      owned(
        color = color,
        verdict = CertificationVerdict.Rejected,
        anchor = anchor,
        payload = metadataPayload(missingSupportFamilies)
      )
    else
      candidateFor(color, context, extractedSoFar) match
        case None =>
          owned(
            color = color,
            verdict = CertificationVerdict.Rejected,
            anchor = anchor,
            payload = metadataPayload(missingSupportFamilies)
          )
        case Some(candidate) =>
          owned(
            color = color,
            verdict = resolveEvidenceVerdict(color, anchor, context),
            anchor = anchor,
            payload = candidate.payload.merge(metadataPayload(missingSupportFamilies)),
            support = tagSupport(candidate.support)
          )

  private def resolveEvidenceVerdict(
      color: Color,
      anchor: WitnessAnchor,
      context: CertificationContext
  ): CertificationVerdict =
    if requiredEvidencePurposes.isEmpty then CertificationVerdict.Certified
    else
      context.evidenceFor(familyId, color, anchor) match
        case None => CertificationVerdict.Deferred
        case Some(evidence) =>
          val strengths =
            requiredEvidencePurposes.map(purpose => evidence.strengthFor(purpose))
          if strengths.exists(_.isEmpty) then CertificationVerdict.Deferred
          else if strengths.forall(_.contains(CertificationEvidenceStrength.Satisfied)) then
            CertificationVerdict.Certified
          else insufficientEvidenceVerdict

  private def metadataPayload(
      missingSupportFamilies: Vector[CertificationSupportFamily]
  ): WitnessPayload =
    val entries =
      Vector(
        Some("burden_tag" -> WitnessValue.Token(burdenTag.value)),
        Some("helper_tags" -> WitnessValue.TokenListValue(helperTags)),
        Option.when(requiredSupportFamilies.nonEmpty)(
          "supporting_families" -> WitnessValue.TokenListValue(requiredSupportFamilies.map(_.value))
        ),
        Option.when(requiredEvidencePurposes.nonEmpty)(
          "evidence_purposes" -> WitnessValue.TokenListValue(requiredEvidencePurposes.map(_.key))
        ),
        Option.when(missingSupportFamilies.nonEmpty)(
          "missing_support_families" -> WitnessValue.TokenListValue(missingSupportFamilies.map(_.value))
        )
      ).flatten
    WitnessPayload.from(entries)

  private def tagSupport(support: WitnessSupport): WitnessSupport =
    helperTags.foldLeft(support)((acc, tag) => acc.addTag(tag))

  protected final def owned(
      color: Color,
      verdict: CertificationVerdict,
      anchor: WitnessAnchor,
      payload: WitnessPayload = WitnessPayload.empty,
      support: WitnessSupport = WitnessSupport.empty
  ): Certification =
    Certification(
      familyId = familyId,
      scope = scope,
      burdenTag = burdenTag,
      verdict = verdict,
      anchor = anchor,
      owner = Some(color),
      payload = payload,
      support = support
    )
