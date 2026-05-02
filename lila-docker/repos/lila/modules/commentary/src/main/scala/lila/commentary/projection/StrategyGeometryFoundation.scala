package lila.commentary.projection

import chess.{ Color, Ply, Square }
import chess.format.{ Fen, Uci }
import chess.variant

import scala.util.control.NonFatal

import lila.commentary.certification.{ CertificationEnginePolicyFingerprint, CertificationEngineRole }
import lila.commentary.root.RootStateVector
import lila.commentary.witness.{ WitnessAnchor, WitnessPolarity, WitnessValue }
import lila.commentary.witness.seed.{ StrategySupportSeed, StrategySupportSeedExtractor, StrategySupportSeedScopeContract }

object StrategyGeometryFoundation:

  enum CertifiedFactFamily(val key: String):
    case BoardTransition extends CertifiedFactFamily("board_transition")
    case MoveCausalTactic extends CertifiedFactFamily("move_causal_tactic")
    case KingSafetyAttack extends CertifiedFactFamily("king_safety_attack")
    case LineAccessActivity extends CertifiedFactFamily("line_access_activity")
    case PawnStructurePasser extends CertifiedFactFamily("pawn_structure_passer")
    case SpaceRestrictionMobility extends CertifiedFactFamily("space_restriction_mobility")
    case InitiativeCounterplay extends CertifiedFactFamily("initiative_counterplay")
    case PieceQualityMaterialTrade extends CertifiedFactFamily("piece_quality_material_trade")
    case EndgameConversionHolding extends CertifiedFactFamily("endgame_conversion_holding")

  object CertifiedFactFamily:
    def fromKey(key: String): Option[CertifiedFactFamily] =
      CertifiedFactFamily.values.find(_.key == key.trim)

  enum PublicPathDisposition(
      val key: String,
      val entersSxxAdmission: Boolean,
      val canOwnPublicClaim: Boolean
  ):
    case Certified extends PublicPathDisposition("certified", true, true)
    case SupportOnly extends PublicPathDisposition("support_only", false, false)
    case ContextOnly extends PublicPathDisposition("context_only", false, false)
    case Deferred extends PublicPathDisposition("deferred", false, false)
    case Rejected extends PublicPathDisposition("rejected", false, false)
    case AntiCase extends PublicPathDisposition("anti_case", false, false)

  object PublicPathDisposition:
    def fromKey(key: String): Option[PublicPathDisposition] =
      PublicPathDisposition.values.find(_.key == key.trim)

  enum ExactIdentityKind(val key: String):
    case ExactCurrentBoard extends ExactIdentityKind("exact_current_board")
    case ExactTransition extends ExactIdentityKind("exact_transition")

  final case class ExactTransitionIdentity(
      beforeFen: String,
      playedMove: String,
      currentFen: String,
      nodeId: String,
      ply: Int,
      variant: String
  ):
    require(beforeFen.trim.nonEmpty, "Exact transition identity requires beforeFen")
    require(playedMove.trim.nonEmpty, "Exact transition identity requires playedMove")
    require(currentFen.trim.nonEmpty, "Exact transition identity requires currentFen")
    require(nodeId.trim.nonEmpty, "Exact transition identity requires nodeId")
    require(ply >= 0, "Exact transition identity requires non-negative ply")
    require(variant.trim.nonEmpty, "Exact transition identity requires variant")

    val kind: ExactIdentityKind = ExactIdentityKind.ExactTransition

    def cacheKey: String =
      Vector(kind.key, variant.trim, nodeId.trim, ply.toString, beforeFen.trim, playedMove.trim, currentFen.trim)
        .mkString("|")

    def legalReplayMatchesCurrent: Boolean =
      ExactTransitionIdentity.legalReplayMatchesCurrent(this)

  object ExactTransitionIdentity:
    def legalReplayMatchesCurrent(identity: ExactTransitionIdentity): Boolean =
      if identity.variant.trim != "standard" then false
      else
        val replayed =
          for
            start <- readPosition(identity.beforeFen)
            move <- readUciMove(identity.playedMove)
            after <- start.move(move).toOption.map(_.after.position)
            fen <- safe(Fen.write(after, Ply(identity.ply).fullMoveNumber).value)
            normalized <- normalizeFen(fen)
          yield normalized
        replayed.exists(_ == normalizeFen(identity.currentFen).getOrElse(identity.currentFen.trim))

    private def readPosition(fen: String): Option[chess.Position] =
      safe(Fen.read(variant.Standard, Fen.Full.clean(fen))).flatten

    private def readUciMove(rawUci: String): Option[Uci.Move] =
      Uci(rawUci.trim) match
        case Some(move: Uci.Move) => Some(move)
        case _                    => None

    private def normalizeFen(fen: String): Option[String] =
      safe((Fen.Full.clean(fen): Fen.Full).value)

    private def safe[A](body: => A): Option[A] =
      try Some(body)
      catch case NonFatal(_) => None

  final case class ProofBurden(
      family: CertifiedFactFamily,
      owner: Option[Color],
      anchor: WitnessAnchor,
      route: String,
      scope: String,
      requiredEngineRoles: Set[CertificationEngineRole] = Set.empty,
      requiredCarrierKinds: Set[StrategyProjectionCarrierKind] = Set.empty,
      requiresExactTransition: Boolean = false,
      disposition: PublicPathDisposition = PublicPathDisposition.Certified
  ):
    require(route.trim.nonEmpty, "Proof burden requires route")
    require(scope.trim.nonEmpty, "Proof burden requires scope")

    def canEnterSxxAdmission: Boolean =
      disposition.entersSxxAdmission &&
        (scope == "exact_current_board" || scope == "exact_transition" || scope == "position_local" || scope == "move_local") &&
        (requiredCarrierKinds.nonEmpty || requiredEngineRoles.nonEmpty || requiresExactTransition)

  enum CarrierBindingLaw(val key: String):
    case SameOwnerAnchorRouteScope extends CarrierBindingLaw("same_owner_anchor_route_scope")
    case SameEntrySquare extends CarrierBindingLaw("same_entry_square")
    case SameContactSquare extends CarrierBindingLaw("same_contact_square")
    case SameTarget extends CarrierBindingLaw("same_target")
    case SamePiece extends CarrierBindingLaw("same_piece")
    case DefenderResource extends CarrierBindingLaw("defender_resource")
    case ExactTransition extends CarrierBindingLaw("exact_transition")

  final case class EngineProofIdentity(
      probeRequestId: Option[String],
      probePolicyFingerprint: Option[String],
      engineConfigFingerprint: Option[String],
      targetDepth: Option[Int],
      floorDepth: Option[Int],
      multiPv: Option[Int],
      certificationEvidenceId: Option[String],
      satisfiedRoleInvariants: Set[CertificationEngineRole] = Set.empty,
      semanticCoverageRoles: Set[CertificationEngineRole] = Set.empty,
      publicCaps: Set[String] = Set.empty
  ):
    def completeFor(requiredRoles: Set[CertificationEngineRole]): Boolean =
      requiredRoles.isEmpty ||
        (probeRequestId.exists(isServerIssuedQRequestId) &&
          probePolicyFingerprint.exists(policy =>
            expectedPolicyFingerprint(requiredRoles).contains(policy.trim)
          ) &&
          engineConfigFingerprint.exists(_.trim.nonEmpty) &&
          targetDepth.exists(_ > 0) &&
          floorDepth.exists(_ > 0) &&
          multiPv.exists(_ > 0) &&
          certificationEvidenceId.exists(_.trim.nonEmpty) &&
          requiredRoles.subsetOf(satisfiedRoleInvariants) &&
          qRequestRoleMatches(requiredRoles) &&
          bestDefenseCoverageSatisfied(requiredRoles) &&
          publicCaps.isEmpty)

    def matchesProofBurden(
        burden: ProofBurden,
        requiredRoles: Set[CertificationEngineRole]
    ): Boolean =
      requiredRoles.isEmpty ||
        (requiredRoles.toVector.sortBy(_.key) match
          case Vector(role) =>
            val expectedPrefix =
              Vector(
                "q",
                stableToken(role.key),
                stableToken(burden.family.key),
                burden.owner.map(colorKey).getOrElse("none"),
                stableToken(burden.anchor.key),
                stableToken(burden.route)
              ).mkString("-")
            probeRequestId.exists(_.trim.startsWith(expectedPrefix))
          case _ => false)

    private def bestDefenseCoverageSatisfied(requiredRoles: Set[CertificationEngineRole]): Boolean =
      !requiredRoles.contains(CertificationEngineRole.BestDefenseSurvival) ||
        semanticCoverageRoles.contains(CertificationEngineRole.BestDefenseSurvival)

    private def expectedPolicyFingerprint(requiredRoles: Set[CertificationEngineRole]): Option[String] =
      requiredRoles.toVector.sortBy(_.key) match
        case Vector(role) =>
          engineConfigFingerprint.map(engine =>
            CertificationEnginePolicyFingerprint.defaultForRole(engine.trim, role)
          )
        case _ => None

    private def qRequestRoleMatches(requiredRoles: Set[CertificationEngineRole]): Boolean =
      requiredRoles.toVector.sortBy(_.key) match
        case Vector(role) =>
          probeRequestId.exists(_.trim.contains(s"q-${stableToken(role.key)}-"))
        case _ => false

    private def isServerIssuedQRequestId(value: String): Boolean =
      value.trim.matches("^q-[a-z0-9][a-z0-9_-]*$")

    private def stableToken(value: String): String =
      value
        .replaceAll("([a-z0-9])([A-Z])", "$1-$2")
        .replaceAll("[^A-Za-z0-9]+", "-")
        .stripPrefix("-")
        .stripSuffix("-")
        .toLowerCase

    private def colorKey(color: Color): String =
      if color.white then "white" else "black"

  final case class EngineProofIdentityPolicy(
      requiresRequestId: Boolean,
      requiresPolicyFingerprint: Boolean,
      requiresEngineFingerprint: Boolean,
      requiresDepthAndMultiPv: Boolean,
      requiresRoleInvariantReport: Boolean,
      requiresSemanticCoverageForBestDefense: Boolean,
      terminalPublicCapsFailClosed: Boolean
  )

  object EngineProofIdentityPolicy:
    val NoneRequired: EngineProofIdentityPolicy =
      EngineProofIdentityPolicy(
        requiresRequestId = false,
        requiresPolicyFingerprint = false,
        requiresEngineFingerprint = false,
        requiresDepthAndMultiPv = false,
        requiresRoleInvariantReport = false,
        requiresSemanticCoverageForBestDefense = false,
        terminalPublicCapsFailClosed = true
      )

    val EngineBackedPublicK: EngineProofIdentityPolicy =
      EngineProofIdentityPolicy(
        requiresRequestId = true,
        requiresPolicyFingerprint = true,
        requiresEngineFingerprint = true,
        requiresDepthAndMultiPv = true,
        requiresRoleInvariantReport = true,
        requiresSemanticCoverageForBestDefense = true,
        terminalPublicCapsFailClosed = true
      )

  final case class FalsificationBurden(keys: Vector[String]):
    require(keys.nonEmpty, "Public strategy slice requires falsification burden keys")
    require(
      keys.forall(_.matches("^[a-z][a-z0-9_]*$")),
      s"Invalid falsification burden key in ${keys.mkString(", ")}"
    )

  final case class PhraseCapabilityTemplate(
      allowedPredicateKey: String,
      forbiddenTerms: Vector[String],
      sanOnlyVariationEvidence: Boolean,
      allowsResultLanguage: Boolean = false,
      allowsBestForcedLanguage: Boolean = false,
      allowsEngineLanguage: Boolean = false,
      allowsFallbackText: Boolean = false
  ):
    require(allowedPredicateKey.trim.nonEmpty, "Phrase capability requires a predicate key")
    require(forbiddenTerms.nonEmpty, "Phrase capability requires forbidden terms")

  final case class CenterPrototype(
      key: String,
      primaryFamily: CertifiedFactFamily,
      routeDistances: Map[String, Double],
      scopeDistances: Map[String, Double]
  ):
    require(key.matches("^mu_S[0-9]{2}_[a-z0-9_]+$"), s"Invalid center prototype key: $key")
    require(routeDistances.nonEmpty, "Center prototype requires at least one route")
    require(scopeDistances.nonEmpty, "Center prototype requires at least one scope")
    require(routeDistances.values.forall(value => value >= 0.0 && value.isFinite), "Route fit values must be finite and non-negative")
    require(scopeDistances.values.forall(value => value >= 0.0 && value.isFinite), "Scope fit values must be finite and non-negative")

  final class CertifiedTruth private (
      val id: String,
      val family: CertifiedFactFamily,
      val owner: Option[Color],
      val anchor: WitnessAnchor,
      val route: String,
      val scope: String,
      val burden: ProofBurden,
      val engineRoles: Set[CertificationEngineRole],
      val exactTransitionIdentity: Option[ExactTransitionIdentity],
      val engineProofIdentity: Option[EngineProofIdentity],
      val projectionEvidenceKinds: Vector[StrategyProjectionEvidenceKind],
      val rootState: Option[RootStateVector],
      val lowerCarrierRefs: Vector[StrategyProjectionCarrierRef]
  ):
    require(id.trim.nonEmpty, "Certified truth id must be non-empty")
    require(route.trim.nonEmpty, "Certified truth route must be non-empty")
    require(scope.trim.nonEmpty, "Certified truth scope must be non-empty")

    def disposition: PublicPathDisposition = burden.disposition

    def canEnterGeometry: Boolean =
      burden.canEnterSxxAdmission &&
        burden.family == family &&
        burden.owner == owner &&
        burden.anchor == anchor &&
        burden.route == route &&
        burden.scope == scope &&
        hasRequiredEngineRoles &&
        hasRequiredExactTransition &&
        hasRequiredEngineProofIdentity

    def sameRootAs(current: RootStateVector): Boolean =
      rootState.contains(current)

    def hasRequiredLowerCarrierKinds: Boolean =
      burden.requiredCarrierKinds.subsetOf(lowerCarrierRefs.map(_.kind).toSet)

    def hasBoundLowerCarrierRefs: Boolean =
      owner.exists(resolvedOwner =>
        lowerCarrierRefs.nonEmpty &&
          lowerCarrierRefs.forall(ref =>
            ref.owner == colorKey(resolvedOwner) &&
              ref.anchor == anchor.key &&
              ref.route == route &&
              ref.scope == scope
          )
      )

    def hasConsistentCarrierBinding(key: String): Boolean =
      val values = lowerCarrierRefs.flatMap(_.bindingValue(key))
      lowerCarrierRefs.nonEmpty &&
        values.size == lowerCarrierRefs.size &&
        values.distinct.size == 1

    def hasRequiredEngineRoles: Boolean =
      burden.requiredEngineRoles.subsetOf(engineRoles)

    def hasRequiredExactTransition: Boolean =
      !burden.requiresExactTransition ||
        (scope == "exact_transition" &&
          exactTransitionIdentity.exists(identity =>
            identity.kind == ExactIdentityKind.ExactTransition && identity.legalReplayMatchesCurrent
          ))

    def hasRequiredEngineProofIdentity: Boolean =
      engineRoles.isEmpty || engineProofIdentity.exists(identity =>
        identity.completeFor(engineRoles) &&
          identity.matchesProofBurden(burden, engineRoles) &&
          identity.certificationEvidenceId.exists(id =>
            lowerCarrierRefs.exists(ref => ref.kind == StrategyProjectionCarrierKind.Certification && ref.id == id)
          )
      )

    private def colorKey(color: Color): String =
      if color.white then "white" else "black"

  object CertifiedTruth:
    def fromTrustedCarriers(
        id: String,
        rootState: Option[RootStateVector],
        burden: ProofBurden,
        engineRoles: Set[CertificationEngineRole] = Set.empty,
        exactTransitionIdentity: Option[ExactTransitionIdentity] = None,
        engineProofIdentity: Option[EngineProofIdentity] = None,
        projectionEvidenceKinds: Vector[StrategyProjectionEvidenceKind] = Vector.empty,
        lowerCarrierRefs: Vector[StrategyProjectionCarrierRef] = Vector.empty
    ): Option[CertifiedTruth] =
      val truth =
        new CertifiedTruth(
          id = id,
          family = burden.family,
          owner = burden.owner,
          anchor = burden.anchor,
          route = burden.route,
          scope = burden.scope,
          burden = burden,
          engineRoles = engineRoles,
          exactTransitionIdentity = exactTransitionIdentity,
          engineProofIdentity = engineProofIdentity,
          projectionEvidenceKinds = projectionEvidenceKinds.distinct,
          rootState = rootState,
          lowerCarrierRefs = lowerCarrierRefs
        )
      Option.when(
        rootState.nonEmpty &&
          burden.requiredEngineRoles == engineRoles &&
          truth.hasRequiredLowerCarrierKinds &&
          truth.hasBoundLowerCarrierRefs &&
          truth.hasRequiredExactTransition &&
          truth.hasRequiredEngineProofIdentity &&
          engineRoleCarrierBound(truth)
      )(truth)

    private def engineRoleCarrierBound(truth: CertifiedTruth): Boolean =
      truth.engineRoles.isEmpty ||
        (truth.burden.requiredCarrierKinds.contains(StrategyProjectionCarrierKind.Certification) &&
          truth.lowerCarrierRefs.exists(_.kind == StrategyProjectionCarrierKind.Certification))

  final case class AdmittedRegion(
      regionId: String,
      bandId: StrategyProjectionBandId,
      families: Vector[CertifiedFactFamily],
      prototypeKey: String,
      requiresHardAdmission: Boolean = true,
      enabledForDefaultProducer: Boolean = false
  ):
    require(regionId.matches("^A_S[0-9]{2}_[a-z0-9_]+$"), s"Invalid admitted region id: $regionId")
    require(families.nonEmpty, "Admitted region requires at least one certified fact family")
    require(prototypeKey.trim.nonEmpty, "Admitted region requires a prototype key")

  final case class GeometryDistance private (value: Option[Double]):
    def isFinite: Boolean = value.exists(_.isFinite)
    def valueOption: Option[Double] = value

  object GeometryDistance:
    val outsideHardAdmission: GeometryDistance = GeometryDistance(None)
    def finite(value: Double): GeometryDistance =
      require(value >= 0.0 && value.isFinite, "Geometry distance must be a finite non-negative value")
      GeometryDistance(Some(value))

  enum DistanceAlgebra(val key: String):
    case RouteScopeFamilyAdditive extends DistanceAlgebra("route_scope_family_additive")

    def distance(
        center: CenterPrototype,
        family: CertifiedFactFamily,
        route: String,
        scope: String
    ): Option[GeometryDistance] =
      distance(center, Set(center.primaryFamily), family, route, scope)

    def distance(
        center: CenterPrototype,
        admittedFamilies: Set[CertifiedFactFamily],
        family: CertifiedFactFamily,
        route: String,
        scope: String
    ): Option[GeometryDistance] =
      this match
        case RouteScopeFamilyAdditive =>
          for
            routeDistance <- center.routeDistances.get(route)
            scopeDistance <- center.scopeDistances.get(scope)
            familyDistance <- familyDistanceFor(center, admittedFamilies, family)
          yield GeometryDistance.finite(routeDistance + scopeDistance + familyDistance)

    private def familyDistanceFor(
        center: CenterPrototype,
        admittedFamilies: Set[CertifiedFactFamily],
        family: CertifiedFactFamily
    ): Option[Double] =
      Option.when(admittedFamilies.contains(family)):
        if family == center.primaryFamily then 0.0 else 0.25

  enum OverlapPolicy(
      val key: String,
      val canStrengthenTruth: Boolean,
      val canSelectPublicPrimary: Boolean
  ):
    case AllowMultipleMemberships extends OverlapPolicy("allow_multiple_memberships", false, true)

  final case class StrategyProjectionSliceDescriptor(
      region: AdmittedRegion,
      center: CenterPrototype,
      evidenceKinds: Vector[StrategyProjectionEvidenceKind],
      proofBurden: ProofBurden,
      carrierBindingLaws: Vector[CarrierBindingLaw],
      engineProofIdentityPolicy: EngineProofIdentityPolicy,
      distanceAlgebra: DistanceAlgebra,
      overlapPolicy: OverlapPolicy,
      phraseCapability: PhraseCapabilityTemplate,
      falsificationBurden: FalsificationBurden,
      rivalBands: Vector[StrategyProjectionBandId],
      rolloutEnabled: Boolean = false
  ):
    require(region.prototypeKey == center.key, "Slice descriptor region prototype must match center key")
    require(region.families.contains(center.primaryFamily), "Slice descriptor center primary family must be covered")
    require(region.families.contains(proofBurden.family), "Slice descriptor proof burden family must be covered")
    require(evidenceKinds.nonEmpty, "Slice descriptor requires projection evidence kinds")
    require(carrierBindingLaws.nonEmpty, "Slice descriptor requires carrier binding laws")
    require(
      evidenceKinds.forall(StrategyProjectionScopeContract.isAllowedEvidenceKind(region.bandId, _)),
      s"Slice descriptor evidence kinds must be allowed for ${region.bandId.value}"
    )

    def carrierBindingSatisfiedBy(truth: CertifiedTruth): Boolean =
      carrierBindingLaws.forall:
        case CarrierBindingLaw.SameOwnerAnchorRouteScope => truth.hasBoundLowerCarrierRefs
        case CarrierBindingLaw.ExactTransition => truth.hasRequiredExactTransition
        case CarrierBindingLaw.SameEntrySquare => truth.hasConsistentCarrierBinding("entry_square")
        case CarrierBindingLaw.SameContactSquare => truth.hasConsistentCarrierBinding("contact_square")
        case CarrierBindingLaw.SameTarget => truth.hasConsistentCarrierBinding("target_square")
        case CarrierBindingLaw.SamePiece => truth.hasConsistentCarrierBinding("piece")
        case CarrierBindingLaw.DefenderResource => truth.hasConsistentCarrierBinding("defender_resource")

    def engineProofIdentitySatisfiedBy(truth: CertifiedTruth): Boolean =
      val policyRequiresEngineIdentity =
        engineProofIdentityPolicy.requiresRequestId ||
          engineProofIdentityPolicy.requiresPolicyFingerprint ||
          engineProofIdentityPolicy.requiresEngineFingerprint ||
          engineProofIdentityPolicy.requiresDepthAndMultiPv ||
          engineProofIdentityPolicy.requiresRoleInvariantReport ||
          engineProofIdentityPolicy.requiresSemanticCoverageForBestDefense
      if policyRequiresEngineIdentity then
        truth.engineRoles.nonEmpty && truth.hasRequiredEngineProofIdentity
      else if truth.engineRoles.isEmpty then engineProofIdentityPolicy.terminalPublicCapsFailClosed
      else truth.hasRequiredEngineProofIdentity

    def descriptorBurdenSatisfiedBy(truth: CertifiedTruth): Boolean =
      region.families.contains(truth.family) &&
        proofBurden.owner.forall(owner => truth.owner.contains(owner)) &&
        truth.anchor == proofBurden.anchor &&
        center.routeDistances.contains(truth.route) &&
        center.scopeDistances.contains(truth.scope) &&
        proofBurden.requiredCarrierKinds.subsetOf(truth.lowerCarrierRefs.map(_.kind).toSet) &&
        (!proofBurden.requiresExactTransition || truth.hasRequiredExactTransition)

    def evidenceKindsForRoute(route: String): Vector[StrategyProjectionEvidenceKind] =
      evidenceKinds.filter(kind => StrategyGeometryFoundation.evidenceKindMatchesRoute(kind, route))

    def projectionEvidenceKindsSatisfiedBy(
        projectionEvidenceKinds: Vector[StrategyProjectionEvidenceKind],
        route: String
    ): Boolean =
      val expected = evidenceKindsForRoute(route).toSet
      val actual = projectionEvidenceKinds.toSet
      expected.nonEmpty &&
        actual.nonEmpty &&
        actual.subsetOf(expected) &&
        actual.forall(kind => StrategyProjectionScopeContract.isAllowedEvidenceKind(region.bandId, kind))

    def exactBoardEvidenceSatisfiedBy(
        currentRootState: RootStateVector,
        truth: CertifiedTruth
    ): Boolean =
      exactBoardEvidenceSatisfiedBy(
        currentRootState = currentRootState,
        owner = truth.owner,
        route = truth.route,
        lowerCarrierRefs = truth.lowerCarrierRefs,
        projectionEvidenceKinds = truth.projectionEvidenceKinds
      )

    def exactBoardEvidenceSatisfiedBy(
        currentRootState: RootStateVector,
        owner: Option[Color],
        route: String,
        lowerCarrierRefs: Vector[StrategyProjectionCarrierRef],
        projectionEvidenceKinds: Vector[StrategyProjectionEvidenceKind]
    ): Boolean =
      owner.exists: resolvedOwner =>
        projectionEvidenceKindsSatisfiedBy(projectionEvidenceKinds, route) &&
          (route match
            case "king_entry_conversion" =>
              exactKingEntryEvidence(currentRootState, resolvedOwner, lowerCarrierRefs)
            case "king_opposition" =>
              exactKingOppositionEvidence(currentRootState, resolvedOwner, lowerCarrierRefs)
            case _ => false
          )

  private def evidenceKindMatchesRoute(kind: StrategyProjectionEvidenceKind, route: String): Boolean =
    kind match
      case StrategyProjectionScopeContract.KingEntryConversionCertified => route == "king_entry_conversion"
      case StrategyProjectionScopeContract.KingOppositionCertified => route == "king_opposition"
      case _ => false

  private def exactKingEntryEvidence(
      currentRootState: RootStateVector,
      owner: Color,
      lowerCarrierRefs: Vector[StrategyProjectionCarrierRef]
  ): Boolean =
    val seeds = StrategySupportSeedExtractor.fromRoot(currentRootState).seeds
    val entry = commonBindingSquare(lowerCarrierRefs, "entry_square")
    val contact = commonBindingSquare(lowerCarrierRefs, "contact_square")
    (entry, contact) match
      case (Some(entrySquare), Some(contactSquare)) =>
        val entrySeed =
          seeds.forSeedId(StrategySupportSeedScopeContract.S23KingEntrySquare).exists: seed =>
            ownerSeed(seed, owner) &&
              seed.anchor == WitnessAnchor.SquareAnchor(entrySquare) &&
              payloadSquareListContains(seed, "adjacent_enemy_pawns", contactSquare)
        val routeSeed =
          seeds.forSeedId(StrategySupportSeedScopeContract.S23KingAccessRoute).exists: seed =>
            ownerSeed(seed, owner) &&
              payloadSquare(seed, "entry_square").contains(entrySquare)
        entrySeed && routeSeed
      case _ => false

  private def exactKingOppositionEvidence(
      currentRootState: RootStateVector,
      owner: Color,
      lowerCarrierRefs: Vector[StrategyProjectionCarrierRef]
  ): Boolean =
    val seeds = StrategySupportSeedExtractor.fromRoot(currentRootState).seeds
    commonBindingSquare(lowerCarrierRefs, "contact_square").exists: contactSquare =>
      seeds.forSeedId(StrategySupportSeedScopeContract.S23KingOppositionContact).exists: seed =>
        ownerSeed(seed, owner) &&
          seed.anchor == WitnessAnchor.SquareAnchor(contactSquare) &&
          payloadToken(seed, "relation").contains("direct_opposition")

  private def ownerSeed(seed: StrategySupportSeed, owner: Color): Boolean =
    seed.polarity == WitnessPolarity.Owner && seed.color.contains(owner)

  private def commonBindingSquare(
      refs: Vector[StrategyProjectionCarrierRef],
      key: String
  ): Option[Square] =
    refs.flatMap(_.bindingValue(key)).distinct match
      case Vector(value) => Square.fromKey(value)
      case _ => None

  private def payloadSquare(seed: StrategySupportSeed, field: String): Option[Square] =
    seed.payload.get(field).collect:
      case WitnessValue.SquareValue(square) => square

  private def payloadSquareListContains(
      seed: StrategySupportSeed,
      field: String,
      square: Square
  ): Boolean =
    seed.payload.get(field).exists:
      case WitnessValue.SquareListValue(squares) => squares.contains(square)
      case _ => false

  private def payloadToken(seed: StrategySupportSeed, field: String): Option[String] =
    seed.payload.get(field).collect:
      case WitnessValue.Token(value) => value

  final case class AntiCausalityBarrier(key: String):
    require(key.matches("^[a-z][a-z0-9_]*$"), s"Invalid anti-causality barrier key: $key")

  final case class AntiCaseFamily(key: String, regressionId: String):
    require(key.matches("^[a-z][a-z0-9_]*$"), s"Invalid anti-case family key: $key")
    require(regressionId.trim.nonEmpty, "Anti-case family requires regression id")
    val disposition: PublicPathDisposition = PublicPathDisposition.AntiCase
    val mayEmitPublicText: Boolean = false

  val certifiedFactFamilies: Vector[CertifiedFactFamily] =
    CertifiedFactFamily.values.toVector

  val publicPathDispositions: Vector[PublicPathDisposition] =
    PublicPathDisposition.values.toVector

  val antiCausalityBarriers: Vector[AntiCausalityBarrier] =
    Vector(
      "pre_existing_fact",
      "already_loose_or_pinned",
      "pawn_move_shape_shift",
      "king_move",
      "mate_dominated",
      "material_collapse",
      "coincidental_standing_tactic"
    ).map(AntiCausalityBarrier.apply)

  val antiCaseFamilies: Vector[AntiCaseFamily] =
    Vector(
      AntiCaseFamily("starting_position_home_pawn_xray", "root-starting-position-home-pawn-xray"),
      AntiCaseFamily("pre_existing_loose_piece", "transition-pre-existing-loose-piece"),
      AntiCaseFamily("pre_existing_pin_or_xray", "transition-pre-existing-pin-or-xray"),
      AntiCaseFamily("mate_dominated_tactical_smell", "transition-mate-dominated-tactical-smell"),
      AntiCaseFamily("material_collapse_tactical_smell", "transition-material-collapse-tactical-smell")
    )

  val certifiedFactFamiliesByBand: Map[String, Vector[CertifiedFactFamily]] =
    import CertifiedFactFamily.*
    Map(
      "S01" -> Vector(KingSafetyAttack),
      "S02" -> Vector(KingSafetyAttack, LineAccessActivity),
      "S03" -> Vector(KingSafetyAttack, LineAccessActivity),
      "S04" -> Vector(KingSafetyAttack),
      "S05" -> Vector(PawnStructurePasser, InitiativeCounterplay),
      "S06" -> Vector(SpaceRestrictionMobility),
      "S07" -> Vector(InitiativeCounterplay),
      "S08" -> Vector(InitiativeCounterplay),
      "S09" -> Vector(LineAccessActivity),
      "S10" -> Vector(SpaceRestrictionMobility, LineAccessActivity),
      "S11" -> Vector(PawnStructurePasser),
      "S12" -> Vector(LineAccessActivity, SpaceRestrictionMobility),
      "S13" -> Vector(PawnStructurePasser),
      "S14" -> Vector(PawnStructurePasser),
      "S15" -> Vector(PawnStructurePasser),
      "S16" -> Vector(PawnStructurePasser, EndgameConversionHolding),
      "S17" -> Vector(PieceQualityMaterialTrade),
      "S18" -> Vector(PieceQualityMaterialTrade),
      "S19" -> Vector(PieceQualityMaterialTrade),
      "S20" -> Vector(SpaceRestrictionMobility),
      "S21" -> Vector(InitiativeCounterplay, PawnStructurePasser),
      "S22" -> Vector(EndgameConversionHolding),
      "S23" -> Vector(EndgameConversionHolding, LineAccessActivity),
      "S24" -> Vector(MoveCausalTactic, PieceQualityMaterialTrade),
      "S25" -> Vector(LineAccessActivity)
    )

  private val ExactBoardScopeFit: Map[String, Double] =
    Map(
      "exact_current_board" -> 0.0,
      "exact_transition" -> 0.05
    )

  private val StrategyProjectionForbiddenTerms: Vector[String] =
    Vector("best", "forced", "winning", "drawn", "result", "oracle", "theory", "recommend", "engine")

  val sliceDescriptors: Vector[StrategyProjectionSliceDescriptor] =
    import CertifiedFactFamily.*
    Vector(
      StrategyProjectionSliceDescriptor(
        region = AdmittedRegion(
          regionId = "A_S23_endgame_entry_or_opposition",
          bandId = StrategyProjectionScopeContract.S23,
          families = Vector(EndgameConversionHolding, LineAccessActivity),
          prototypeKey = "mu_S23_endgame_entry_or_opposition"
        ),
        center = CenterPrototype(
          key = "mu_S23_endgame_entry_or_opposition",
          primaryFamily = EndgameConversionHolding,
          routeDistances = Map(
            "king_entry_conversion" -> 0.0,
            "king_opposition" -> 0.05
          ),
          scopeDistances = ExactBoardScopeFit
        ),
        evidenceKinds = Vector(
          StrategyProjectionScopeContract.KingEntryConversionCertified,
          StrategyProjectionScopeContract.KingOppositionCertified
        ),
        proofBurden = ProofBurden(
          family = EndgameConversionHolding,
          owner = None,
          anchor = WitnessAnchor.BoardAnchor,
          route = "king_entry_conversion",
          scope = "exact_current_board",
          requiredCarrierKinds = Set(StrategyProjectionCarrierKind.ExactBoard)
        ),
        carrierBindingLaws = Vector(
          CarrierBindingLaw.SameOwnerAnchorRouteScope,
          CarrierBindingLaw.SameEntrySquare,
          CarrierBindingLaw.SameContactSquare
        ),
        engineProofIdentityPolicy = EngineProofIdentityPolicy.NoneRequired,
        distanceAlgebra = DistanceAlgebra.RouteScopeFamilyAdditive,
        overlapPolicy = OverlapPolicy.AllowMultipleMemberships,
        phraseCapability = PhraseCapabilityTemplate(
          allowedPredicateKey = "strategy_projection",
          forbiddenTerms = StrategyProjectionForbiddenTerms,
          sanOnlyVariationEvidence = true
        ),
        falsificationBurden = FalsificationBurden(
          Vector(
            "wrong_owner",
            "wrong_anchor",
            "wrong_route",
            "wrong_evidence_kind",
            "missing_exact_board_fact",
            "support_only_carrier",
            "sibling_band_rival",
            "stale_root"
          )
        ),
        rivalBands = Vector(
          StrategyProjectionScopeContract.S09,
          StrategyProjectionScopeContract.S16,
          StrategyProjectionScopeContract.S22
        )
      ),
      StrategyProjectionSliceDescriptor(
        region = AdmittedRegion(
          regionId = "A_S23_line_access_activity",
          bandId = StrategyProjectionScopeContract.S23,
          families = Vector(LineAccessActivity),
          prototypeKey = "mu_S23_line_access_activity"
        ),
        center = CenterPrototype(
          key = "mu_S23_line_access_activity",
          primaryFamily = LineAccessActivity,
          routeDistances = Map("king_entry_conversion" -> 0.0),
          scopeDistances = ExactBoardScopeFit
        ),
        evidenceKinds = Vector(StrategyProjectionScopeContract.KingEntryConversionCertified),
        proofBurden = ProofBurden(
          family = LineAccessActivity,
          owner = None,
          anchor = WitnessAnchor.BoardAnchor,
          route = "king_entry_conversion",
          scope = "exact_current_board",
          requiredCarrierKinds = Set(StrategyProjectionCarrierKind.ExactBoard)
        ),
        carrierBindingLaws = Vector(
          CarrierBindingLaw.SameOwnerAnchorRouteScope,
          CarrierBindingLaw.SameEntrySquare
        ),
        engineProofIdentityPolicy = EngineProofIdentityPolicy.NoneRequired,
        distanceAlgebra = DistanceAlgebra.RouteScopeFamilyAdditive,
        overlapPolicy = OverlapPolicy.AllowMultipleMemberships,
        phraseCapability = PhraseCapabilityTemplate(
          allowedPredicateKey = "strategy_projection",
          forbiddenTerms = StrategyProjectionForbiddenTerms,
          sanOnlyVariationEvidence = true
        ),
        falsificationBurden = FalsificationBurden(
          Vector(
            "wrong_owner",
            "wrong_anchor",
            "wrong_route",
            "wrong_evidence_kind",
            "missing_exact_board_fact",
            "support_only_carrier",
            "sibling_band_rival",
            "stale_root"
          )
        ),
        rivalBands = Vector(
          StrategyProjectionScopeContract.S09,
          StrategyProjectionScopeContract.S23,
          StrategyProjectionScopeContract.S25
        )
      )
    )

  val sliceDescriptorsByRegionId: Map[String, StrategyProjectionSliceDescriptor] =
    sliceDescriptors.map(descriptor => descriptor.region.regionId -> descriptor).toMap

  val admittedRegionsByBand: Map[String, Vector[AdmittedRegion]] =
    sliceDescriptors
      .groupBy(_.region.bandId.value)
      .view
      .mapValues(_.sortBy(_.region.regionId).map(_.region))
      .toMap
      .withDefaultValue(Vector.empty)

  require(
    certifiedFactFamiliesByBand.keySet == StrategyProjectionScopeContract.startReadyBandIds.map(_.value).toSet,
    "The strategy geometry finite-cover envelope must cover exactly S01-S25"
  )
  require(
    certifiedFactFamiliesByBand.values.forall(_.nonEmpty),
    "Every strategy band must belong to at least one certified fact family"
  )
  require(
    admittedRegionsByBand.values.flatten.forall(region =>
      certifiedFactFamiliesByBand.getOrElse(region.bandId.value, Vector.empty).toSet.intersect(region.families.toSet).nonEmpty
    ),
    "Every admitted region must be covered by its Sxx finite-cover family"
  )
  require(
    sliceDescriptorsByRegionId.size == sliceDescriptors.size,
    "Strategy projection slice region ids must be unique"
  )
  require(
    sliceDescriptors.forall(descriptor =>
      descriptor.phraseCapability.sanOnlyVariationEvidence &&
        !descriptor.phraseCapability.allowsFallbackText &&
        !descriptor.phraseCapability.allowsBestForcedLanguage &&
        !descriptor.phraseCapability.allowsEngineLanguage &&
        !descriptor.phraseCapability.allowsResultLanguage
    ),
    "Initial public strategy slices must use the no-overclaim phrase capability"
  )
