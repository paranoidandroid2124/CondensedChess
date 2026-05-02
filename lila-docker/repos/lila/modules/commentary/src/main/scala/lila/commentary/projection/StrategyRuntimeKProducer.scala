package lila.commentary.projection

import chess.Color

import lila.commentary.certification.{ CertificationEngineRole, CertificationEvidenceBundle }
import lila.commentary.delta.StrategicDeltaExtraction
import lila.commentary.strategic.StrategicObjectExtraction
import lila.commentary.witness.WitnessAnchor

object StrategyRuntimeKProducer:

  sealed abstract class RuntimeK:
    def regionId: String
    def truth: StrategyGeometryFoundation.CertifiedTruth

  private final case class MintedRuntimeK(
      regionId: String,
      truth: StrategyGeometryFoundation.CertifiedTruth
  ) extends RuntimeK:
    require(regionId.trim.nonEmpty, "Runtime K region id must be non-empty")

  final case class Candidate(
      id: String,
      regionId: String,
      family: StrategyGeometryFoundation.CertifiedFactFamily,
      owner: Color,
      anchor: WitnessAnchor,
      route: String,
      scope: String,
      evidenceKinds: Vector[StrategyProjectionEvidenceKind],
      lowerCarrierRefs: Vector[StrategyProjectionCarrierRef],
      engineRoles: Set[CertificationEngineRole] = Set.empty,
      exactTransitionIdentity: Option[StrategyGeometryFoundation.ExactTransitionIdentity] = None,
      engineProofIdentity: Option[StrategyGeometryFoundation.EngineProofIdentity] = None,
      disposition: StrategyGeometryFoundation.PublicPathDisposition =
        StrategyGeometryFoundation.PublicPathDisposition.Certified,
      falsificationReasons: Set[String] = Set.empty
  )

  final case class Input(
      currentExtraction: StrategicObjectExtraction,
      deltaExtraction: Option[StrategicDeltaExtraction],
      certificationEvidence: CertificationEvidenceBundle,
      candidates: Vector[Candidate] = Vector.empty
  )

  def produce(input: Input): Vector[RuntimeK] =
    if !input.certificationEvidence.matches(input.currentExtraction.rootState) then Vector.empty
    else input.candidates.flatMap(candidate => mint(input, candidate))

  private def mint(
      input: Input,
      candidate: Candidate
  ): Option[RuntimeK] =
    for
      descriptor <- StrategyGeometryFoundation.sliceDescriptorsByRegionId.get(candidate.regionId)
      _ <- Option.when(falsificationReasons(input, descriptor, candidate).isEmpty)(())
      requiredEngineRoles = descriptor.proofBurden.requiredEngineRoles ++ candidate.engineRoles
      _ <- Option.when(engineCarrierShapeMatches(candidate, requiredEngineRoles))(())
      truth <- StrategyGeometryFoundation.CertifiedTruth.fromTrustedCarriers(
        id = candidate.id,
        rootState = Some(input.currentExtraction.rootState),
        burden = StrategyGeometryFoundation.ProofBurden(
          family = candidate.family,
          owner = descriptor.proofBurden.owner.orElse(Some(candidate.owner)),
          anchor = descriptor.proofBurden.anchor,
          route = candidate.route,
          scope = candidate.scope,
          requiredEngineRoles = requiredEngineRoles,
          requiredCarrierKinds = requiredCarrierKinds(descriptor, requiredEngineRoles),
          requiresExactTransition = descriptor.proofBurden.requiresExactTransition || candidate.exactTransitionIdentity.nonEmpty,
          disposition = candidate.disposition
        ),
        engineRoles = requiredEngineRoles,
        exactTransitionIdentity = candidate.exactTransitionIdentity,
        engineProofIdentity = candidate.engineProofIdentity,
        projectionEvidenceKinds = candidate.evidenceKinds,
        lowerCarrierRefs = candidate.lowerCarrierRefs
      )
      _ <- Option.when(descriptor.descriptorBurdenSatisfiedBy(truth))(())
      _ <- Option.when(descriptor.carrierBindingSatisfiedBy(truth))(())
      _ <- Option.when(descriptor.engineProofIdentitySatisfiedBy(truth))(())
      _ <- Option.when(descriptor.exactBoardEvidenceSatisfiedBy(input.currentExtraction.rootState, truth))(())
    yield MintedRuntimeK(descriptor.region.regionId, truth)

  private[projection] def falsificationReasons(
      input: Input,
      descriptor: StrategyGeometryFoundation.StrategyProjectionSliceDescriptor,
      candidate: Candidate
  ): Set[String] =
    val declared = descriptor.falsificationBurden.keys.toSet
    def declaredReason(key: String, failed: Boolean): Option[String] =
      Option.when(failed && declared.contains(key))(key)
    val descriptorOwnerMismatch =
      descriptor.proofBurden.owner.exists(_ != candidate.owner)
    val missingRequiredCarrier =
      !descriptor.proofBurden.requiredCarrierKinds.subsetOf(candidate.lowerCarrierRefs.map(_.kind).toSet)
    val missingExactTransition =
      candidate.scope == "exact_transition" && candidate.exactTransitionIdentity.isEmpty
    val familyOutsideDescriptor =
      !descriptor.region.families.contains(candidate.family)
    val wrongEvidenceKind =
      !descriptor.projectionEvidenceKindsSatisfiedBy(candidate.evidenceKinds, candidate.route)
    val missingExactBoardFact =
      !descriptor.exactBoardEvidenceSatisfiedBy(
        currentRootState = input.currentExtraction.rootState,
        owner = Some(candidate.owner),
        route = candidate.route,
        lowerCarrierRefs = candidate.lowerCarrierRefs,
        projectionEvidenceKinds = candidate.evidenceKinds
      )
    Set(
      declaredReason("wrong_owner", descriptorOwnerMismatch),
      declaredReason("wrong_anchor", candidate.anchor != descriptor.proofBurden.anchor),
      declaredReason("wrong_route", !descriptor.center.routeDistances.contains(candidate.route)),
      declaredReason("wrong_evidence_kind", wrongEvidenceKind),
      declaredReason("missing_exact_board_fact", missingExactBoardFact),
      declaredReason("sibling_band_rival", familyOutsideDescriptor),
      declaredReason("support_only_carrier", candidate.disposition != StrategyGeometryFoundation.PublicPathDisposition.Certified || missingRequiredCarrier),
      declaredReason("stale_root", !input.certificationEvidence.matches(input.currentExtraction.rootState)),
      Option.when(missingExactTransition)("missing_exact_transition")
    ).flatten ++ candidate.falsificationReasons

  private def requiredCarrierKinds(
      descriptor: StrategyGeometryFoundation.StrategyProjectionSliceDescriptor,
      requiredEngineRoles: Set[CertificationEngineRole]
  ): Set[StrategyProjectionCarrierKind] =
    if requiredEngineRoles.nonEmpty then descriptor.proofBurden.requiredCarrierKinds + StrategyProjectionCarrierKind.Certification
    else descriptor.proofBurden.requiredCarrierKinds

  private def engineCarrierShapeMatches(
      candidate: Candidate,
      requiredEngineRoles: Set[CertificationEngineRole]
  ): Boolean =
    requiredEngineRoles.isEmpty ||
      (candidate.lowerCarrierRefs.exists(_.kind == StrategyProjectionCarrierKind.Certification) &&
        candidate.engineProofIdentity.exists(identity =>
          identity.completeFor(requiredEngineRoles) &&
            identity.certificationEvidenceId.exists(id =>
              candidate.lowerCarrierRefs.exists(ref => ref.kind == StrategyProjectionCarrierKind.Certification && ref.id == id)
            )
        ))
