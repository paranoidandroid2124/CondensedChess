package lila.commentary.projection

import chess.Color

import lila.commentary.CommentaryCore
import lila.commentary.certification.{ CertificationEnginePolicyFingerprint, CertificationEngineRole, CertificationEvidenceBundle }
import lila.commentary.strategic.StrategicObjectExtraction

object StrategyProjectionFalsificationHarness:

  enum CaseKind(val key: String):
    case Exact extends CaseKind("exact")
    case NearMiss extends CaseKind("near_miss")
    case FalseRival extends CaseKind("false_rival")
    case ShortcutNegative extends CaseKind("shortcut_negative")

  final case class FalsificationCase(
      kind: CaseKind,
      descriptor: StrategyGeometryFoundation.StrategyProjectionSliceDescriptor,
      current: StrategicObjectExtraction,
      candidate: StrategyRuntimeKProducer.Candidate,
      expectedCertifiedTruth: Boolean,
      expectedPublicAdmission: Boolean
  )

  final case class FalsificationResult(
      testCase: FalsificationCase,
      falsificationReasons: Set[String],
      runtimeKs: Vector[StrategyRuntimeKProducer.RuntimeK],
      admissions: Vector[StrategyProjectionAdmissionResult]
  )

  val requiredCaseKinds: Set[CaseKind] =
    Set(CaseKind.Exact, CaseKind.NearMiss, CaseKind.FalseRival, CaseKind.ShortcutNegative)

  def cases: Vector[FalsificationCase] =
    StrategyGeometryFoundation.sliceDescriptors.flatMap(casesFor)

  def run(testCase: FalsificationCase): FalsificationResult =
    val input =
      StrategyRuntimeKProducer.Input(
        currentExtraction = testCase.current,
        deltaExtraction = None,
        certificationEvidence = CertificationEvidenceBundle.empty,
        candidates = Vector(testCase.candidate)
      )
    val runtimeKs = StrategyRuntimeKProducer.produce(input)
    val admissions =
      StrategyProjectionAdmissionProducer.produce(
        StrategyProjectionAdmissionProducer.Input(
          currentExtraction = testCase.current,
          deltaExtraction = None,
          certificationEvidence = CertificationEvidenceBundle.empty,
          runtimeKCandidates = Vector(testCase.candidate)
        ),
        Vector(testCase.descriptor.region.regionId)
      )
    FalsificationResult(
      testCase = testCase,
      falsificationReasons = StrategyRuntimeKProducer.falsificationReasons(
        input,
        testCase.descriptor,
        testCase.candidate
      ),
      runtimeKs = runtimeKs,
      admissions = admissions
    )

  private def casesFor(
      descriptor: StrategyGeometryFoundation.StrategyProjectionSliceDescriptor
  ): Vector[FalsificationCase] =
    val current = currentExtractionFor(descriptor)
    val exact = exactCandidate(descriptor)
    Vector(
      FalsificationCase(
        kind = CaseKind.Exact,
        descriptor = descriptor,
        current = current,
        candidate = exact,
        expectedCertifiedTruth = true,
        expectedPublicAdmission = true
      ),
      FalsificationCase(
        kind = CaseKind.NearMiss,
        descriptor = descriptor,
        current = current,
        candidate = exact.copy(
          id = s"${exact.id}-near-miss",
          route = s"near_miss_${exact.route}",
          lowerCarrierRefs = boundCarrierRefs(descriptor, exact.owner, s"near_miss_${exact.route}", exact.scope)
        ),
        expectedCertifiedTruth = false,
        expectedPublicAdmission = false
      ),
      FalsificationCase(
        kind = CaseKind.FalseRival,
        descriptor = descriptor,
        current = current,
        candidate = exact.copy(
          id = s"${exact.id}-false-rival",
          family = falseRivalFamily(descriptor)
        ),
        expectedCertifiedTruth = false,
        expectedPublicAdmission = false
      ),
      FalsificationCase(
        kind = CaseKind.ShortcutNegative,
        descriptor = descriptor,
        current = current,
        candidate = exact.copy(
          id = s"${exact.id}-shortcut-negative",
          lowerCarrierRefs = Vector.empty
        ),
        expectedCertifiedTruth = false,
        expectedPublicAdmission = false
      )
    )

  private def exactCandidate(
      descriptor: StrategyGeometryFoundation.StrategyProjectionSliceDescriptor
  ): StrategyRuntimeKProducer.Candidate =
    val owner = descriptor.proofBurden.owner.getOrElse(Color.White)
    val route = exactRoute(descriptor)
    val scope = descriptor.proofBurden.scope
    val engineRoles = descriptor.proofBurden.requiredEngineRoles
    StrategyRuntimeKProducer.Candidate(
      id = stableId("k", descriptor.region.regionId, "exact"),
      regionId = descriptor.region.regionId,
      family = descriptor.center.primaryFamily,
      owner = owner,
      anchor = descriptor.proofBurden.anchor,
      route = route,
      scope = scope,
      evidenceKinds = descriptor.evidenceKindsForRoute(route),
      lowerCarrierRefs = boundCarrierRefs(descriptor, owner, route, scope, engineRoles),
      engineRoles = engineRoles,
      exactTransitionIdentity = exactTransitionIdentityFor(descriptor),
      engineProofIdentity = engineProofIdentityFor(descriptor, owner, route, engineRoles)
    )

  private def boundCarrierRefs(
      descriptor: StrategyGeometryFoundation.StrategyProjectionSliceDescriptor,
      owner: Color,
      route: String,
      scope: String,
      engineRoles: Set[CertificationEngineRole] = Set.empty
  ): Vector[StrategyProjectionCarrierRef] =
    val requiredKinds =
      if engineRoles.nonEmpty then
        descriptor.proofBurden.requiredCarrierKinds + StrategyProjectionCarrierKind.Certification
      else descriptor.proofBurden.requiredCarrierKinds
    requiredKinds.toVector.sortBy(_.key).map: kind =>
      StrategyProjectionCarrierRef(
        kind = kind,
        id = stableId("k", descriptor.region.regionId, kind.key),
        owner = colorKey(owner),
        anchor = descriptor.proofBurden.anchor.key,
        route = route,
        scope = scope,
        binding = sharedBinding
      )

  private def engineProofIdentityFor(
      descriptor: StrategyGeometryFoundation.StrategyProjectionSliceDescriptor,
      owner: Color,
      route: String,
      engineRoles: Set[CertificationEngineRole]
  ): Option[StrategyGeometryFoundation.EngineProofIdentity] =
    engineRoles.toVector.sortBy(_.key) match
      case Vector(role) =>
        val engine = "falsification-harness-engine"
        val certificationCarrierId = stableId("k", descriptor.region.regionId, StrategyProjectionCarrierKind.Certification.key)
        Some(
          StrategyGeometryFoundation.EngineProofIdentity(
            probeRequestId = Some(
              Vector(
                "q",
                stableToken(role.key),
                stableToken(descriptor.center.primaryFamily.key),
                colorKey(owner),
                stableToken(descriptor.proofBurden.anchor.key),
                stableToken(route),
                "falsification-node",
                "0"
              ).mkString("-")
            ),
            probePolicyFingerprint = Some(CertificationEnginePolicyFingerprint.defaultForRole(engine, role)),
            engineConfigFingerprint = Some(engine),
            targetDepth = Some(18),
            floorDepth = Some(16),
            multiPv = Some(3),
            certificationEvidenceId = Some(certificationCarrierId),
            satisfiedRoleInvariants = engineRoles,
            semanticCoverageRoles = engineRoles.filter(_ == CertificationEngineRole.BestDefenseSurvival)
          )
        )
      case _ => None

  private def exactTransitionIdentityFor(
      descriptor: StrategyGeometryFoundation.StrategyProjectionSliceDescriptor
  ): Option[StrategyGeometryFoundation.ExactTransitionIdentity] =
    Option.when(descriptor.proofBurden.requiresExactTransition || descriptor.proofBurden.scope == "exact_transition"):
      StrategyGeometryFoundation.ExactTransitionIdentity(
        beforeFen = transitionBeforeFen,
        playedMove = "f3f4",
        currentFen = transitionAfterFen,
        nodeId = "falsification-node",
        ply = 1,
        variant = "standard"
      )

  private def currentExtractionFor(
      descriptor: StrategyGeometryFoundation.StrategyProjectionSliceDescriptor
  ): StrategicObjectExtraction =
    val fen =
      if descriptor.proofBurden.requiresExactTransition || descriptor.proofBurden.scope == "exact_transition" then
        transitionAfterFen
      else currentBoardFen
    CommentaryCore.extractStrategicObjectsFromFenFailClosed(fen).fold(sys.error, identity)

  private def exactRoute(descriptor: StrategyGeometryFoundation.StrategyProjectionSliceDescriptor): String =
    descriptor.center.routeDistances.toVector.sortBy((route, distance) => (distance, route)).head._1

  private def falseRivalFamily(
      descriptor: StrategyGeometryFoundation.StrategyProjectionSliceDescriptor
  ): StrategyGeometryFoundation.CertifiedFactFamily =
    val regionFamilies = descriptor.region.families.toSet
    descriptor.rivalBands
      .flatMap(band => StrategyGeometryFoundation.certifiedFactFamiliesByBand.getOrElse(band.value, Vector.empty))
      .find(family => !regionFamilies.contains(family))
      .orElse(StrategyGeometryFoundation.certifiedFactFamilies.find(family => !regionFamilies.contains(family)))
      .getOrElse(descriptor.center.primaryFamily)

  private val currentBoardFen = "6k1/8/8/3p4/5K2/8/8/8 w - - 0 1"
  private val transitionBeforeFen = "6k1/8/8/3p4/8/5K2/8/8 w - - 0 1"
  private val transitionAfterFen = "6k1/8/8/3p4/5K2/8/8/8 b - - 1 1"

  private val sharedBinding: Map[String, String] =
    Map(
      "entry_square" -> "e5",
      "contact_square" -> "d5",
      "target_square" -> "d5",
      "piece" -> "white_king_f4",
      "defender_resource" -> "black_king_d5"
    )

  private def stableId(parts: String*): String =
    parts.map(stableToken).mkString("-")

  private def stableToken(value: String): String =
    value
      .replaceAll("([a-z0-9])([A-Z])", "$1-$2")
      .replaceAll("[^A-Za-z0-9]+", "-")
      .stripPrefix("-")
      .stripSuffix("-")
      .toLowerCase

  private def colorKey(color: Color): String =
    if color.white then "white" else "black"
