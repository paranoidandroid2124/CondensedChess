package lila.commentary.projection

import lila.commentary.projection.StrategyGeometryFoundation.AdmittedRegion
import lila.commentary.projection.StrategyGeometryFoundation.CertifiedFactFamily
import lila.commentary.projection.StrategyGeometryFoundation.CertifiedTruth
import lila.commentary.projection.StrategyGeometryFoundation.GeometryDistance
import lila.commentary.projection.StrategyGeometryFoundation.OverlapPolicy
import lila.commentary.projection.StrategyGeometryFoundation.PublicPathDisposition
import lila.commentary.projection.StrategyGeometryFoundation.StrategyProjectionSliceDescriptor
import lila.commentary.root.RootStateVector

object StrategyGeometryEngine:

  final case class GeometryCenter(
      key: String,
      bandId: StrategyProjectionBandId,
      families: Set[CertifiedFactFamily],
      primaryFamily: CertifiedFactFamily,
      routeDistances: Map[String, Double],
      scopeDistances: Map[String, Double]
  ):
    require(key.matches("^mu_S[0-9]{2}_[a-z0-9_]+$"), s"Invalid geometry center key: $key")
    require(families.contains(primaryFamily), "Geometry center primary family must be covered by its families")
    require(routeDistances.nonEmpty, "Geometry center requires at least one route")
    require(scopeDistances.nonEmpty, "Geometry center requires at least one scope")

  enum GeometryClassification(val key: String):
    case ExactCenter extends GeometryClassification("exact_center")
    case FiniteNeighborhood extends GeometryClassification("finite_neighborhood")

  enum OverlapRole(val key: String, val canOwnPublicPrimary: Boolean):
    case PublicPrimary extends OverlapRole("public_primary", true)
    case SupportMembership extends OverlapRole("support_membership", false)

  final case class GeometryMembership(
      region: AdmittedRegion,
      truth: CertifiedTruth,
      center: GeometryCenter,
      distance: GeometryDistance,
      classification: GeometryClassification,
      overlapRole: OverlapRole,
      rank: Int
  ):
    require(distance.isFinite, "Geometry membership requires finite distance")
    require(rank >= 1, "Geometry membership rank is one-based")

  final case class ClassificationResult(memberships: Vector[GeometryMembership]):
    def publicPrimaryMemberships: Vector[GeometryMembership] =
      memberships.filter(_.overlapRole.canOwnPublicPrimary)

  private final case class CandidateMembership(
      descriptor: StrategyProjectionSliceDescriptor,
      region: AdmittedRegion,
      truth: CertifiedTruth,
      center: GeometryCenter,
      distance: GeometryDistance,
      classification: GeometryClassification,
      overlapPolicy: OverlapPolicy,
      canOwnPublicPrimary: Boolean
  )

  private val configuredCentersByRegionId: Map[String, GeometryCenter] =
    StrategyGeometryFoundation.sliceDescriptors.map: descriptor =>
      descriptor.region.regionId ->
        GeometryCenter(
          key = descriptor.center.key,
          bandId = descriptor.region.bandId,
          families = descriptor.region.families.toSet,
          primaryFamily = descriptor.center.primaryFamily,
          routeDistances = descriptor.center.routeDistances,
          scopeDistances = descriptor.center.scopeDistances
        )
    .toMap

  val centers: Vector[GeometryCenter] =
    configuredCentersByRegionId.toVector.sortBy(_._1).map(_._2)

  def centerFor(region: AdmittedRegion): Option[GeometryCenter] =
    configuredCentersByRegionId.get(region.regionId).filter(center =>
      center.bandId == region.bandId && center.key == region.prototypeKey
    )

  def classify(
      currentRootState: RootStateVector,
      certifiedTruths: Vector[CertifiedTruth],
      enabledRegionIds: Vector[String]
  ): ClassificationResult =
    val enabled = enabledRegionIds.toSet
    val enabledDescriptors =
      StrategyGeometryFoundation.sliceDescriptors.filter(descriptor => enabled.contains(descriptor.region.regionId))
    val candidates =
      enabledDescriptors.flatMap(descriptor =>
        centerFor(descriptor.region).toVector.flatMap(center =>
          certifiedTruths.flatMap(truth => classifyCandidate(currentRootState, descriptor, center, truth))
        )
      )
    ClassificationResult(resolveOverlap(candidates))

  private def classifyCandidate(
      currentRootState: RootStateVector,
      descriptor: StrategyProjectionSliceDescriptor,
      center: GeometryCenter,
      truth: CertifiedTruth
  ): Option[CandidateMembership] =
    val region = descriptor.region
    for
      _ <- Option.when(center.bandId == region.bandId)(())
      _ <- Option.when(center.key == region.prototypeKey)(())
      _ <- Option.when(region.families.toSet.subsetOf(center.families))(())
      _ <- Option.when(descriptor.descriptorBurdenSatisfiedBy(truth))(())
      _ <- Option.when(descriptor.carrierBindingSatisfiedBy(truth))(())
      _ <- Option.when(descriptor.engineProofIdentitySatisfiedBy(truth))(())
      _ <- Option.when(descriptor.exactBoardEvidenceSatisfiedBy(currentRootState, truth))(())
      _ <- Option.when(region.families.contains(truth.family))(())
      _ <- Option.when(truth.owner.nonEmpty)(())
      _ <- Option.when(truth.disposition == PublicPathDisposition.Certified)(())
      _ <- Option.when(truth.canEnterGeometry)(())
      _ <- Option.when(truth.sameRootAs(currentRootState))(())
      _ <- Option.when(truth.hasRequiredLowerCarrierKinds)(())
      _ <- Option.when(truth.hasBoundLowerCarrierRefs)(())
      _ <- Option.when(truth.lowerCarrierRefs.nonEmpty)(())
      _ <- Option.when(hasRequiredEngineRoleCarrier(truth))(())
      distance <- distanceToCenter(descriptor, center, truth).filter(_.isFinite)
    yield
      CandidateMembership(
        descriptor = descriptor,
        region = region,
        truth = truth,
        center = center,
        distance = distance,
        classification = classificationFor(distance),
        overlapPolicy = descriptor.overlapPolicy,
        canOwnPublicPrimary = truth.family == center.primaryFamily
      )

  private def distanceToCenter(
      descriptor: StrategyProjectionSliceDescriptor,
      center: GeometryCenter,
      truth: CertifiedTruth
  ): Option[GeometryDistance] =
    descriptor.distanceAlgebra.distance(
      descriptor.center,
      center.families,
      truth.family,
      truth.route,
      truth.scope
    )

  private def hasRequiredEngineRoleCarrier(truth: CertifiedTruth): Boolean =
    truth.engineRoles.isEmpty ||
      truth.lowerCarrierRefs.exists(_.kind == StrategyProjectionCarrierKind.Certification)

  private def classificationFor(distance: GeometryDistance): GeometryClassification =
    if distance.valueOption.contains(0.0) then GeometryClassification.ExactCenter
    else GeometryClassification.FiniteNeighborhood

  private def resolveOverlap(candidates: Vector[CandidateMembership]): Vector[GeometryMembership] =
    val resolved =
      candidates
        .groupBy(_.truth.id)
        .values
        .toVector
        .flatMap(resolveTruthOverlap)
    resolved
      .sortBy(membership =>
        (
          membership.distance.valueOption.getOrElse(Double.MaxValue),
          membership.region.regionId,
          membership.center.key,
          membership.truth.id
        )
      )
      .zipWithIndex
      .map((membership, index) => membership.copy(rank = index + 1))

  private def resolveTruthOverlap(candidates: Vector[CandidateMembership]): Vector[GeometryMembership] =
    val sorted =
      candidates
        .sortBy(candidate =>
          (
            candidate.distance.valueOption.getOrElse(Double.MaxValue),
            candidate.region.regionId,
            candidate.center.key,
            candidate.truth.id
          )
        )
    val publicPrimaryIndex =
      if sorted.exists(_.overlapPolicy.canSelectPublicPrimary) then
        sorted.indexWhere(_.canOwnPublicPrimary)
      else -1
    sorted
      .zipWithIndex
      .map: (candidate, index) =>
        GeometryMembership(
          region = candidate.region,
          truth = candidate.truth,
          center = candidate.center,
          distance = candidate.distance,
          classification = candidate.classification,
          overlapRole = if index == publicPrimaryIndex then OverlapRole.PublicPrimary else OverlapRole.SupportMembership,
          rank = 1
        )
