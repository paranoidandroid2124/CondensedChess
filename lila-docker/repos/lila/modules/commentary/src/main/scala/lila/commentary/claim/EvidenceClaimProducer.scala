package lila.commentary.claim

import chess.Color

import lila.commentary.certification.{ Certification, CertificationExtraction, CertificationVerdict }
import lila.commentary.delta.StrategicDeltaExtraction
import lila.commentary.projection.{
  StrategyProjectionAdmissionResult,
  StrategyProjectionAdmissionAuthority,
  StrategyProjectionAdmissionStatus,
  StrategyProjectionBandId,
  StrategyProjectionCarrierKind,
  StrategyProjectionEvidenceKind,
  StrategyProjectionScopeContract
}
import lila.commentary.root.{ RootPositionSupport, RootStateVector }
import lila.commentary.selection.*
import lila.commentary.strategic.StrategicObjectExtraction
import lila.commentary.witness.WitnessAnchor

final case class EvidenceClaimHandoff(
    certification: Option[CertificationExtraction] = None,
    projection: Vector[ProjectionClaimCandidate] = Vector.empty,
    projectionAdmissions: Vector[StrategyProjectionAdmissionResult] = Vector.empty,
    sourceContext: Vector[SourceContextCandidate] = Vector.empty
)

object EvidenceClaimHandoff:
  val empty: EvidenceClaimHandoff = EvidenceClaimHandoff()

final case class ProjectionClaimCandidate(
    id: String,
    rootState: RootStateVector,
    bandId: StrategyProjectionBandId,
    evidenceKind: StrategyProjectionEvidenceKind,
    owner: Color,
    anchor: WitnessAnchor,
    route: String,
    scope: String,
    lowerCarrierRefs: Vector[EvidenceRef],
    impact: ClaimImpact = ClaimImpact(evidenceConfidence = 40, boardExplainability = 40, pedagogicalClarity = 30)
):
  require(id.trim.nonEmpty, "Projection claim candidate id must be non-empty")
  require(route.trim.nonEmpty, "Projection claim candidate route must be non-empty")
  require(scope.trim.nonEmpty, "Projection claim candidate scope must be non-empty")
  require(
    StrategyProjectionScopeContract.isAllowedEvidenceKind(bandId, evidenceKind),
    s"Projection evidence ${evidenceKind.value} is not allowed for ${bandId.value}"
  )

object EvidenceClaimProducer:

  def produce(
      currentExtraction: StrategicObjectExtraction,
      deltaExtraction: Option[StrategicDeltaExtraction],
      handoff: EvidenceClaimHandoff = EvidenceClaimHandoff.empty
  ): Vector[CommentaryClaim] =
    ExactBoardClaimProducer.produce(currentExtraction, deltaExtraction) ++
      certificationClaims(currentExtraction, deltaExtraction, handoff.certification) ++
      projectionClaims(currentExtraction, handoff.projectionAdmissions) ++
      sourceContextClaims(handoff.sourceContext)

  private def certificationClaims(
      currentExtraction: StrategicObjectExtraction,
      deltaExtraction: Option[StrategicDeltaExtraction],
      extraction: Option[CertificationExtraction]
  ): Vector[CommentaryClaim] =
    extraction.toVector
      .filter(certificationExtractionMatchesInput(currentExtraction, deltaExtraction, _))
      .flatMap(certificationExtraction =>
        certificationExtraction.claims.all.flatMap(certificationToClaim(currentExtraction, certificationExtraction, _))
      )

  private def certificationExtractionMatchesInput(
      currentExtraction: StrategicObjectExtraction,
      deltaExtraction: Option[StrategicDeltaExtraction],
      certificationExtraction: CertificationExtraction
  ): Boolean =
    certificationExtraction.current.rootState == currentExtraction.rootState &&
      certificationExtraction.evidence.matches(currentExtraction.rootState) &&
      certificationExtraction.delta.forall(certifiedDelta => deltaExtraction.contains(certifiedDelta))

  private def certificationToClaim(
      currentExtraction: StrategicObjectExtraction,
      extraction: CertificationExtraction,
      certification: Certification
  ): Option[CommentaryClaim] =
    for
      owner <- certification.owner
      _ <- Option.when(certification.verdict == CertificationVerdict.Certified)(())
      _ <- Option.when(publicCertificationFamily(certification.familyId.value))(())
      evidence <- extraction.evidence.evidenceFor(certification.familyId, owner, certification.anchor)
      sideToMove <- RootPositionSupport.sideToMove(currentExtraction.rootState).toOption
    yield
      val ownerKey = colorKey(owner)
      val defenderKey = colorKey(!owner)
      val anchorKey = certification.anchor.key
      val routeKey = certification.burdenTag.value
      val scopeKey = certification.scope.key
      val engineEvidenceRefs =
        Option.when(evidence.engineBacked)(
          EvidenceRef(
            kind = EvidenceRefKind.EngineCertification,
            id = s"engine-certification:${certification.familyId.value}:$ownerKey:${certification.anchor.kind.key}:$anchorKey",
            owner = Some(ownerKey),
            anchor = Some(anchorKey),
            route = Some(routeKey),
            scope = Some(scopeKey)
          )
        ).toVector
      CommentaryClaim(
        id = s"certification-${stableToken(certification.familyId.value)}-$ownerKey-${stableToken(anchorKey)}",
        layer = ClaimLayer.Certification,
        status = ClaimStatus.Admitted,
        owner = Some(ownerKey),
        beneficiary = Some(ownerKey),
        defender = Some(defenderKey),
        sideToMove = Some(colorKey(sideToMove)),
        anchor = Some(anchorKey),
        route = Some(routeKey),
        scope = Some(scopeKey),
        impact = certificationImpact(certification),
        evidenceRefs = Vector(
          EvidenceRef(
            kind = EvidenceRefKind.Certification,
            id = certification.familyId.value,
            owner = Some(ownerKey),
            anchor = Some(anchorKey),
            route = Some(routeKey),
            scope = Some(scopeKey)
          )
        ) ++ engineEvidenceRefs,
        lowerCarrierRefs = Vector(
          EvidenceRef(
            kind = EvidenceRefKind.ExactBoard,
            id = "certification-current-board",
            owner = Some(ownerKey),
            anchor = Some(anchorKey),
            route = Some(routeKey),
            scope = Some(scopeKey)
          )
        ),
        exactBoardBound = true,
        wordingStrengthCap = WordingStrength.QualifiedSupport
      )

  private def publicCertificationFamily(familyId: String): Boolean =
    Set(
      "DevelopmentComparison",
      "InitiativeWindow",
      "MobilityComparison",
      "ComparativeKingFragility",
      "CertifiedKingSafetyEdge",
      "MaterialHarvest",
      "SpaceBindRestrictionCertification"
    ).contains(familyId)

  private def certificationImpact(certification: Certification): ClaimImpact =
    certification.familyId.value match
      case "MaterialHarvest" =>
        ClaimImpact(
          resultMaterialImpact = 85,
          immediacy = 75,
          evidenceConfidence = 55,
          boardExplainability = 50,
          pedagogicalClarity = 40
        )
      case "CertifiedKingSafetyEdge" =>
        ClaimImpact(forcedness = 55, evidenceConfidence = 55, boardExplainability = 50, pedagogicalClarity = 40)
      case "InitiativeWindow" =>
        ClaimImpact(persistenceAfterDefense = 55, evidenceConfidence = 55, boardExplainability = 45, pedagogicalClarity = 40)
      case _ =>
        ClaimImpact(evidenceConfidence = 45, boardExplainability = 35, pedagogicalClarity = 30)

  private def projectionClaims(
      currentExtraction: StrategicObjectExtraction,
      admissions: Vector[StrategyProjectionAdmissionResult]
  ): Vector[CommentaryClaim] =
    admissions.flatMap(projectionAdmissionToClaim(currentExtraction, _))

  private def projectionAdmissionToClaim(
      currentExtraction: StrategicObjectExtraction,
      admission: StrategyProjectionAdmissionResult
  ): Option[CommentaryClaim] =
    for
      sideToMove <- RootPositionSupport.sideToMove(currentExtraction.rootState).toOption
      _ <- Option.when(admission.authority == StrategyProjectionAdmissionAuthority.DescriptorCertifiedRuntime)(())
      _ <- admission.runtimeKId
      _ <- Option.when(admission.status == StrategyProjectionAdmissionStatus.Admitted)(())
      _ <- Option.when(admission.currentRootState == currentExtraction.rootState)(())
      _ <- Option.when(admission.sourceRootState == currentExtraction.rootState)(())
      _ <- Option.when(admission.lowerCarrierRefs.nonEmpty)(())
      _ <- Option.when(!containsForbiddenToken(admission.projectionId))(())
      _ <- Option.when(!containsForbiddenToken(admission.route))(())
      _ <- Option.when(!containsForbiddenToken(admission.scope))(())
      lowerRefs <- projectionCarrierRefs(admission)
      evidenceRefs <- projectionEvidenceRefs(admission)
    yield
      val ownerKey = colorKey(admission.owner)
      val beneficiaryKey = admission.beneficiary.map(colorKey).getOrElse(ownerKey)
      val defenderKey = admission.defender.map(colorKey).getOrElse(colorKey(!admission.owner))
      val anchorKey = admission.anchor.key
      CommentaryClaim(
        id = admission.projectionId,
        layer = ClaimLayer.Projection,
        status = ClaimStatus.Admitted,
        band = Some(admission.bandId.value),
        owner = Some(ownerKey),
        beneficiary = Some(beneficiaryKey),
        defender = Some(defenderKey),
        sideToMove = Some(colorKey(sideToMove)),
        anchor = Some(anchorKey),
        route = Some(admission.route),
        scope = Some(admission.scope),
        impact = ClaimImpact(evidenceConfidence = 45, boardExplainability = 45, pedagogicalClarity = 35),
        evidenceRefs = evidenceRefs,
        lowerCarrierRefs = lowerRefs,
        exactBoardBound = true,
        wordingStrengthCap = WordingStrength.weaker(admission.wordingStrengthCap, WordingStrength.QualifiedSupport),
        projectionPhraseCapability = Some(admission.phraseCapability),
        projectionRuntimeKId = admission.runtimeKId,
        publicSurfaceForbiddenTerms = admission.publicSurfaceForbiddenTerms
      )

  private def projectionCarrierRefs(admission: StrategyProjectionAdmissionResult): Option[Vector[EvidenceRef]] =
    val refs =
      admission.lowerCarrierRefs.map: carrier =>
        EvidenceRef(
          kind = projectionCarrierKind(carrier.kind),
          id = carrier.id,
          owner = Some(carrier.owner),
          anchor = Some(carrier.anchor),
          route = Some(carrier.route),
          scope = Some(carrier.scope)
        )
    Option.when(refs.nonEmpty && refs.forall(sameProjectionBinding(admission, _)))(refs)

  private def projectionEvidenceRefs(admission: StrategyProjectionAdmissionResult): Option[Vector[EvidenceRef]] =
    val refs =
      admission.evidenceKinds.map: kind =>
        EvidenceRef(
          kind = EvidenceRefKind.Projection,
          id = kind.value,
          owner = Some(colorKey(admission.owner)),
          anchor = Some(admission.anchor.key),
          route = Some(admission.route),
          scope = Some(admission.scope)
        )
    Option.when(refs.nonEmpty)(refs)

  private def sameProjectionBinding(admission: StrategyProjectionAdmissionResult, ref: EvidenceRef): Boolean =
    projectionCarrierOwnerMatches(admission, ref) &&
      ref.anchor.contains(admission.anchor.key) &&
      ref.route.contains(admission.route) &&
      ref.scope.contains(admission.scope)

  private def projectionCarrierOwnerMatches(admission: StrategyProjectionAdmissionResult, ref: EvidenceRef): Boolean =
    ref.owner.contains(colorKey(admission.owner)) ||
      admission.defender.exists(defender =>
        ref.owner.contains(colorKey(defender)) &&
          ((admission.bandId == StrategyProjectionScopeContract.S04 &&
            ref.kind == EvidenceRefKind.Object &&
            ref.id == "KingSafetyShell") ||
            (admission.bandId == StrategyProjectionScopeContract.S16 &&
              ref.kind == EvidenceRefKind.Witness &&
              ref.id == "passed_pawn_entity_state"))
      )

  private def projectionCarrierKind(kind: StrategyProjectionCarrierKind): EvidenceRefKind =
    kind match
      case StrategyProjectionCarrierKind.ExactBoard     => EvidenceRefKind.ExactBoard
      case StrategyProjectionCarrierKind.Root           => EvidenceRefKind.Root
      case StrategyProjectionCarrierKind.Witness        => EvidenceRefKind.Witness
      case StrategyProjectionCarrierKind.Object         => EvidenceRefKind.Object
      case StrategyProjectionCarrierKind.Delta          => EvidenceRefKind.Delta
      case StrategyProjectionCarrierKind.Certification  => EvidenceRefKind.Certification

  private def sourceContextClaims(candidates: Vector[SourceContextCandidate]): Vector[CommentaryClaim] =
    candidates.map(SourceContextClaimBoundary.toClaim)

  private def colorKey(color: Color): String =
    if color.white then "white" else "black"

  private def stableToken(value: String): String =
    value
      .replaceAll("([a-z0-9])([A-Z])", "$1-$2")
      .replaceAll("[^A-Za-z0-9]+", "-")
      .stripPrefix("-")
      .stripSuffix("-")
      .toLowerCase

  private def containsForbiddenToken(value: String): Boolean =
    val normalized = value.toLowerCase.replace('-', '_').replace(':', '_').replace(' ', '_')
    Vector("best", "forced", "winning", "drawn", "result", "oracle", "theory", "recommend").exists(normalized.contains)
