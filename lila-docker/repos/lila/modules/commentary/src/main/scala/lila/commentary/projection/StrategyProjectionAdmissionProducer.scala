package lila.commentary.projection

import lila.commentary.certification.CertificationEvidenceBundle
import lila.commentary.delta.StrategicDeltaExtraction
import lila.commentary.selection.WordingStrength
import lila.commentary.strategic.StrategicObjectExtraction

object StrategyProjectionAdmissionProducer:

  final case class Input(
      currentExtraction: StrategicObjectExtraction,
      deltaExtraction: Option[StrategicDeltaExtraction],
      certificationEvidence: CertificationEvidenceBundle,
      runtimeKCandidates: Vector[StrategyRuntimeKProducer.Candidate] = Vector.empty
  )

  val defaultEnabledRegionIds: Vector[String] = Vector.empty

  def produce(input: Input): Vector[StrategyProjectionAdmissionResult] =
    produce(input, defaultEnabledRegionIds)

  def produce(
      input: Input,
      enabledRegionIds: Vector[String]
  ): Vector[StrategyProjectionAdmissionResult] =
    if !input.certificationEvidence.matches(input.currentExtraction.rootState) then Vector.empty
    else
      val runtimeKs =
        StrategyRuntimeKProducer.produce(
          StrategyRuntimeKProducer.Input(
            currentExtraction = input.currentExtraction,
            deltaExtraction = input.deltaExtraction,
            certificationEvidence = input.certificationEvidence,
            candidates = input.runtimeKCandidates
          )
        )
      StrategyGeometryEngine
        .classify(
          currentRootState = input.currentExtraction.rootState,
          certifiedTruths = runtimeKs.map(_.truth),
          enabledRegionIds = enabledRegionIds
        )
        .publicPrimaryMemberships
        .flatMap(membership => admissionFromMembership(input.currentExtraction, runtimeKs, membership))
        .distinct

  private def admissionFromMembership(
      currentExtraction: StrategicObjectExtraction,
      runtimeKs: Vector[StrategyRuntimeKProducer.RuntimeK],
      membership: StrategyGeometryEngine.GeometryMembership
  ): Option[StrategyProjectionAdmissionResult] =
    val region = membership.region
    val truth = membership.truth
    for
      owner <- truth.owner
      _ <- Option.when(membership.overlapRole.canOwnPublicPrimary)(())
      _ <- Option.when(membership.distance.isFinite)(())
      descriptor <- StrategyGeometryFoundation.sliceDescriptorsByRegionId.get(region.regionId)
      runtimeK <- runtimeKs.find(k => k.regionId == region.regionId && k.truth.id == truth.id)
      evidenceKinds <- evidenceKindsFor(currentExtraction, descriptor, truth)
    yield
      StrategyProjectionAdmissionResult.fromDecision(
        projectionId = projectionId(region.bandId, owner, truth),
        authority = StrategyProjectionAdmissionAuthority.DescriptorCertifiedRuntime,
        runtimeK = Some(runtimeK),
        bandId = region.bandId,
        sourceRootState = currentExtraction.rootState,
        currentRootState = currentExtraction.rootState,
        evidenceKinds = evidenceKinds,
        owner = owner,
        beneficiary = Some(owner),
        defender = Some(!owner),
        anchor = truth.anchor,
        route = truth.route,
        scope = truth.scope,
        lowerCarrierRefs = truth.lowerCarrierRefs,
        wordingStrengthCap = WordingStrength.QualifiedSupport,
        phraseCapability = phraseCapabilityFromDescriptor(descriptor),
        publicSurfaceForbiddenTerms = descriptor.phraseCapability.forbiddenTerms,
        decision = Right(true)
      )

  private def phraseCapabilityFromDescriptor(
      descriptor: StrategyGeometryFoundation.StrategyProjectionSliceDescriptor
  ): StrategyProjectionPhraseCapability =
    val capability = descriptor.phraseCapability
    StrategyProjectionPhraseCapability(
      allowedPredicateKey = capability.allowedPredicateKey,
      sanOnlyVariationEvidence = capability.sanOnlyVariationEvidence,
      allowsResultLanguage = capability.allowsResultLanguage,
      allowsBestForcedLanguage = capability.allowsBestForcedLanguage,
      allowsEngineLanguage = capability.allowsEngineLanguage,
      allowsFallbackText = capability.allowsFallbackText,
      forbiddenTerms = capability.forbiddenTerms
    )

  private def evidenceKindsFor(
      currentExtraction: StrategicObjectExtraction,
      descriptor: StrategyGeometryFoundation.StrategyProjectionSliceDescriptor,
      truth: StrategyGeometryFoundation.CertifiedTruth
  ): Option[Vector[StrategyProjectionEvidenceKind]] =
    val kinds =
      truth.projectionEvidenceKinds.filter(kind => descriptor.evidenceKindsForRoute(truth.route).contains(kind))
    Option.when(
      kinds.nonEmpty &&
        descriptor.projectionEvidenceKindsSatisfiedBy(kinds, truth.route) &&
        descriptor.exactBoardEvidenceSatisfiedBy(currentExtraction.rootState, truth)
    )(kinds)

  private def projectionId(
      bandId: StrategyProjectionBandId,
      owner: chess.Color,
      truth: StrategyGeometryFoundation.CertifiedTruth
  ): String =
    Vector(
      "projection",
      bandId.value.toLowerCase,
      colorKey(owner),
      stableToken(truth.anchor.key),
      stableToken(truth.route)
    ).mkString("-")

  private def colorKey(color: chess.Color): String =
    if color.white then "white" else "black"

  private def stableToken(value: String): String =
    value
      .replaceAll("([a-z0-9])([A-Z])", "$1-$2")
      .replaceAll("[^A-Za-z0-9]+", "-")
      .stripPrefix("-")
      .stripSuffix("-")
      .toLowerCase
