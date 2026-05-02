package lila.commentary.projection

import chess.Color

import lila.commentary.certification.{ CertificationEnginePolicyFingerprint, CertificationEngineRole }
import lila.commentary.projection.StrategyGeometryFoundation.ExactTransitionIdentity
import lila.commentary.projection.StrategyGeometryFoundation.ProofBurden
import lila.commentary.projection.StrategyGeometryFoundation.PublicPathDisposition

object StrategyProbeRequestPlanner:

  enum StartPositionKind(val key: String):
    case CurrentBoard extends StartPositionKind("current_board")
    case BeforeTransition extends StartPositionKind("before_transition")

  final case class RolePolicy(
      engineRole: CertificationEngineRole,
      targetDepth: Int,
      floorDepth: Int,
      multiPv: Int,
      minPvPlies: Int,
      requiresBaseline: Boolean = false,
      requiresTransitionReplay: Boolean = false
  ):
    require(targetDepth >= 0, "Probe target depth must be non-negative")
    require(floorDepth >= 0, "Probe floor depth must be non-negative")
    require(targetDepth >= floorDepth, "Probe target depth must meet floor depth")
    require(multiPv > 0, "Probe MultiPV must be positive")
    require(minPvPlies >= 0, "Probe minimum PV plies must be non-negative")

  final case class Policy(
      engineFingerprint: String,
      rolePolicies: Map[CertificationEngineRole, RolePolicy]
  ):
    require(engineFingerprint.trim.nonEmpty, "Strategy probe policy requires engine fingerprint")
    require(
      rolePolicies.forall((role, policy) => role == policy.engineRole),
      "Strategy probe role policy map must be keyed by its engine role"
    )

    def policyFingerprintFor(rolePolicy: RolePolicy): String =
      CertificationEnginePolicyFingerprint.forRole(
        engineConfigFingerprint = engineFingerprint,
        role = rolePolicy.engineRole,
        targetDepth = rolePolicy.targetDepth,
        floorDepth = rolePolicy.floorDepth,
        multiPv = rolePolicy.multiPv,
        minPvPlies = rolePolicy.minPvPlies,
        requiresBaseline = rolePolicy.requiresBaseline,
        requiresTransitionReplay = rolePolicy.requiresTransitionReplay
      )

  object Policy:
    val Default: Policy =
      Policy(
        engineFingerprint = "default-strategy-probe-engine",
        rolePolicies =
          CertificationEngineRole.values.map(role =>
            role -> RolePolicy(
              engineRole = role,
              targetDepth = CertificationEnginePolicyFingerprint.defaultTargetDepth(role),
              floorDepth = CertificationEnginePolicyFingerprint.defaultFloorDepth(role),
              multiPv = CertificationEnginePolicyFingerprint.defaultMultiPv(role),
              minPvPlies = CertificationEnginePolicyFingerprint.defaultMinPvPlies(role),
              requiresBaseline = CertificationEnginePolicyFingerprint.defaultRequiresBaseline(role),
              requiresTransitionReplay = CertificationEnginePolicyFingerprint.defaultRequiresTransitionReplay(role)
            )
          ).toMap
      )

  final case class Request(
      requestId: String,
      tauCacheKey: String,
      family: StrategyGeometryFoundation.CertifiedFactFamily,
      owner: Color,
      anchorKey: String,
      route: String,
      scope: String,
      engineRole: CertificationEngineRole,
      startPositionKind: StartPositionKind,
      startFen: String,
      currentFen: String,
      playedMove: Option[String],
      nodeId: String,
      ply: Int,
      variant: String,
      targetDepth: Int,
      floorDepth: Int,
      multiPv: Int,
      minPvPlies: Int,
      requiresBaseline: Boolean,
      probePolicyFingerprint: String,
      engineFingerprint: String
  ):
    require(requestId.matches("q-[a-z0-9][a-z0-9_-]*"), s"Invalid strategy probe request id: $requestId")
    require(tauCacheKey.trim.nonEmpty, "Strategy probe request requires tau cache key")
    require(anchorKey.trim.nonEmpty, "Strategy probe request requires anchor key")
    require(route.trim.nonEmpty, "Strategy probe request requires route")
    require(scope.trim.nonEmpty, "Strategy probe request requires scope")
    require(startFen.trim.nonEmpty, "Strategy probe request requires start FEN")
    require(currentFen.trim.nonEmpty, "Strategy probe request requires current FEN")
    require(nodeId.trim.nonEmpty, "Strategy probe request requires node id")
    require(ply >= 0, "Strategy probe request requires non-negative ply")
    require(variant.trim.nonEmpty, "Strategy probe request requires variant")
    require(targetDepth >= floorDepth, "Strategy probe request target depth must meet floor depth")
    require(multiPv > 0, "Strategy probe request MultiPV must be positive")
    require(minPvPlies >= 0, "Strategy probe request minimum PV plies must be non-negative")
    require(probePolicyFingerprint.trim.nonEmpty, "Strategy probe request requires policy fingerprint")
    require(engineFingerprint.trim.nonEmpty, "Strategy probe request requires engine fingerprint")

    def createsCertifiedTruth: Boolean = false
    def mayEmitPublicText: Boolean = false

  final case class Result(requests: Vector[Request])

  def derive(
      burden: ProofBurden,
      tau: ExactTransitionIdentity,
      policy: Policy
  ): Either[String, Result] =
    if burden.disposition != PublicPathDisposition.Certified then Right(Result(Vector.empty))
    else if burden.requiredEngineRoles.isEmpty then Right(Result(Vector.empty))
    else
      for
        owner <- burden.owner.toRight("probe_burden_owner_required")
        _ <- validateExactScope(burden)
        _ <- validateExactTransitionIdentity(burden, tau)
        rolePolicies <- requestedRolePolicies(burden, policy)
        _ <- validateTransitionRoleCompatibility(burden, rolePolicies)
      yield
        Result(
          rolePolicies.map(policyForRole =>
            requestFor(
              burden = burden,
              tau = tau,
              owner = owner,
              rolePolicy = policyForRole,
              probePolicyFingerprint = policy.policyFingerprintFor(policyForRole),
              engineFingerprint = policy.engineFingerprint
            )
          )
        )

  private def validateExactScope(burden: ProofBurden): Either[String, Unit] =
    if burden.scope == "exact_current_board" || burden.scope == "exact_transition" then Right(())
    else Left(s"probe_scope_not_exact:${burden.scope}")

  private def validateExactTransitionIdentity(
      burden: ProofBurden,
      tau: ExactTransitionIdentity
  ): Either[String, Unit] =
    if burden.scope != "exact_transition" then Right(())
    else Either.cond(tau.legalReplayMatchesCurrent, (), "probe_tau_replay_invalid")

  private def requestedRolePolicies(
      burden: ProofBurden,
      policy: Policy
  ): Either[String, Vector[RolePolicy]] =
    burden.requiredEngineRoles.toVector.sortBy(_.key).foldLeft[Either[String, Vector[RolePolicy]]](Right(Vector.empty)):
      case (acc, role) =>
        acc.flatMap: values =>
          policy.rolePolicies.get(role) match
            case Some(rolePolicy) => Right(values :+ rolePolicy)
            case None => Left(s"probe_policy_missing_role:${role.key}")

  private def validateTransitionRoleCompatibility(
      burden: ProofBurden,
      rolePolicies: Vector[RolePolicy]
  ): Either[String, Unit] =
    rolePolicies.find(_.requiresTransitionReplay) match
      case Some(rolePolicy) if burden.scope != "exact_transition" =>
        Left(s"probe_transition_role_requires_exact_transition:${rolePolicy.engineRole.key}")
      case Some(rolePolicy) if !burden.requiresExactTransition =>
        Left(s"probe_transition_role_requires_tau:${rolePolicy.engineRole.key}")
      case _ => Right(())

  private def requestFor(
      burden: ProofBurden,
      tau: ExactTransitionIdentity,
      owner: Color,
      rolePolicy: RolePolicy,
      probePolicyFingerprint: String,
      engineFingerprint: String
  ): Request =
    val startPositionKind =
      if burden.scope == "exact_transition" then StartPositionKind.BeforeTransition
      else StartPositionKind.CurrentBoard
    Request(
      requestId = requestId(burden, owner, rolePolicy.engineRole, tau),
      tauCacheKey = tau.cacheKey,
      family = burden.family,
      owner = owner,
      anchorKey = burden.anchor.key,
      route = burden.route,
      scope = burden.scope,
      engineRole = rolePolicy.engineRole,
      startPositionKind = startPositionKind,
      startFen = if startPositionKind == StartPositionKind.BeforeTransition then tau.beforeFen else tau.currentFen,
      currentFen = tau.currentFen,
      playedMove = Option.when(startPositionKind == StartPositionKind.BeforeTransition)(tau.playedMove),
      nodeId = tau.nodeId,
      ply = tau.ply,
      variant = tau.variant,
      targetDepth = rolePolicy.targetDepth,
      floorDepth = rolePolicy.floorDepth,
      multiPv = rolePolicy.multiPv,
      minPvPlies = rolePolicy.minPvPlies,
      requiresBaseline = rolePolicy.requiresBaseline,
      probePolicyFingerprint = probePolicyFingerprint,
      engineFingerprint = engineFingerprint
    )

  private def requestId(
      burden: ProofBurden,
      owner: Color,
      role: CertificationEngineRole,
      tau: ExactTransitionIdentity
  ): String =
    Vector(
      "q",
      role.key,
      burden.family.key,
      colorKey(owner),
      stableToken(burden.anchor.key),
      stableToken(burden.route),
      stableToken(tau.nodeId),
      tau.ply.toString
    ).mkString("-")

  private def colorKey(color: Color): String =
    if color.white then "white" else "black"

  private def stableToken(value: String): String =
    value
      .replaceAll("([a-z0-9])([A-Z])", "$1-$2")
      .replaceAll("[^A-Za-z0-9]+", "-")
      .stripPrefix("-")
      .stripSuffix("-")
      .toLowerCase
