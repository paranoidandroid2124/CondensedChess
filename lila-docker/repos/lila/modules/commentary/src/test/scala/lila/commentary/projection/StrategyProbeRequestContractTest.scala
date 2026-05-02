package lila.commentary.projection

import chess.Color

import lila.commentary.certification.CertificationEngineRole
import lila.commentary.witness.WitnessAnchor

class StrategyProbeRequestContractTest extends munit.FunSuite:

  test("derives typed role-bound probe requests from proof burden and exact transition identity"):
    val result =
      StrategyProbeRequestPlanner
        .derive(transitionBurden(requiredRoles = Set(CertificationEngineRole.AntiCausalityCheck, CertificationEngineRole.BestDefenseSurvival)), tau, StrategyProbeRequestPlanner.Policy.Default)
        .fold(fail(_), identity)

    assertEquals(result.requests.map(_.engineRole.key), Vector("anti_causality_check", "best_defense_survival"))
    assertEquals(result.requests.map(_.startPositionKind), Vector.fill(2)(StrategyProbeRequestPlanner.StartPositionKind.BeforeTransition))
    assertEquals(result.requests.map(_.startFen), Vector.fill(2)(tau.beforeFen))
    assertEquals(result.requests.map(_.currentFen), Vector.fill(2)(tau.currentFen))
    assertEquals(result.requests.map(_.playedMove), Vector.fill(2)(Some(tau.playedMove)))
    assertEquals(result.requests.map(_.tauCacheKey).distinct, Vector(tau.cacheKey))
    assertEquals(result.requests.map(_.engineFingerprint).distinct, Vector(StrategyProbeRequestPlanner.Policy.Default.engineFingerprint))
    assert(result.requests.forall(_.probePolicyFingerprint != StrategyProbeRequestPlanner.Policy.Default.engineFingerprint))
    assertEquals(result.requests.exists(_.requestId.contains("missing-pgn-file")), false)
    assertEquals(result.requests.find(_.engineRole == CertificationEngineRole.BestDefenseSurvival).map(_.multiPv), Some(3))
    assert(result.requests.forall(request => !request.createsCertifiedTruth && !request.mayEmitPublicText))

  test("Q policy fingerprint is independent from engine config and changes with role policy"):
    val defaultResult =
      StrategyProbeRequestPlanner
        .derive(currentBoardBurden(requiredRoles = Set(CertificationEngineRole.BestDefenseSurvival)), tau, StrategyProbeRequestPlanner.Policy.Default)
        .fold(fail(_), identity)
    val changedRolePolicy =
      StrategyProbeRequestPlanner.Policy.Default.rolePolicies(CertificationEngineRole.BestDefenseSurvival)
        .copy(targetDepth = 20, floorDepth = 18)
    val changedPolicy =
      StrategyProbeRequestPlanner.Policy.Default.copy(
        rolePolicies = StrategyProbeRequestPlanner.Policy.Default.rolePolicies.updated(
          CertificationEngineRole.BestDefenseSurvival,
          changedRolePolicy
        )
      )
    val changedResult =
      StrategyProbeRequestPlanner
        .derive(currentBoardBurden(requiredRoles = Set(CertificationEngineRole.BestDefenseSurvival)), tau, changedPolicy)
        .fold(fail(_), identity)

    val defaultRequest = defaultResult.requests.head
    val changedRequest = changedResult.requests.head

    assertNotEquals(defaultRequest.probePolicyFingerprint, defaultRequest.engineFingerprint)
    assertNotEquals(changedRequest.probePolicyFingerprint, defaultRequest.probePolicyFingerprint)
    assertEquals(changedRequest.targetDepth, 20)
    assertEquals(changedRequest.floorDepth, 18)

  test("current-board burdens probe the current FEN and keep transition replay out of the request surface"):
    val result =
      StrategyProbeRequestPlanner
        .derive(
          currentBoardBurden(requiredRoles = Set(CertificationEngineRole.SurfaceCapability)),
          tau,
          StrategyProbeRequestPlanner.Policy.Default
        )
        .fold(fail(_), identity)

    assertEquals(result.requests.map(_.startPositionKind), Vector(StrategyProbeRequestPlanner.StartPositionKind.CurrentBoard))
    assertEquals(result.requests.map(_.startFen), Vector(tau.currentFen))
    assertEquals(result.requests.map(_.playedMove), Vector(None))
    assertEquals(result.requests.map(_.nodeId), Vector(tau.nodeId))
    assertEquals(result.requests.map(_.ply), Vector(tau.ply))

  test("probe derivation is fail-closed for non-public disposition missing roles and transition mismatch"):
    val supportOnly =
      StrategyProbeRequestPlanner
        .derive(
          transitionBurden(
            requiredRoles = Set(CertificationEngineRole.AntiCausalityCheck),
            disposition = StrategyGeometryFoundation.PublicPathDisposition.SupportOnly
          ),
          tau,
          StrategyProbeRequestPlanner.Policy.Default
        )
        .fold(fail(_), identity)
    val noEngineRoles =
      StrategyProbeRequestPlanner
        .derive(transitionBurden(requiredRoles = Set.empty), tau, StrategyProbeRequestPlanner.Policy.Default)
        .fold(fail(_), identity)
    val missingPolicy =
      StrategyProbeRequestPlanner.derive(
        currentBoardBurden(requiredRoles = Set(CertificationEngineRole.SurfaceCapability)),
        tau,
        StrategyProbeRequestPlanner.Policy(
          engineFingerprint = "sf-test",
          rolePolicies = Map.empty
        )
      )
    val transitionRoleOnCurrentBoard =
      StrategyProbeRequestPlanner.derive(
        currentBoardBurden(requiredRoles = Set(CertificationEngineRole.AntiCausalityCheck)),
        tau,
        StrategyProbeRequestPlanner.Policy.Default
      )
    val moveLocalScope =
      StrategyProbeRequestPlanner.derive(
        currentBoardBurden(requiredRoles = Set(CertificationEngineRole.SurfaceCapability), scope = "move_local"),
        tau,
        StrategyProbeRequestPlanner.Policy.Default
      )
    val transitionRoleWithoutTauRequirement =
      StrategyProbeRequestPlanner.derive(
        transitionBurden(
          requiredRoles = Set(CertificationEngineRole.AntiCausalityCheck),
          requiresExactTransition = Some(false)
        ),
        tau,
        StrategyProbeRequestPlanner.Policy.Default
      )
    val missingOwner =
      StrategyProbeRequestPlanner.derive(
        currentBoardBurden(requiredRoles = Set(CertificationEngineRole.SurfaceCapability), owner = None),
        tau,
        StrategyProbeRequestPlanner.Policy.Default
      )
    val illegalTau =
      StrategyProbeRequestPlanner.derive(
        transitionBurden(requiredRoles = Set(CertificationEngineRole.AntiCausalityCheck)),
        tau.copy(playedMove = "h1h3"),
        StrategyProbeRequestPlanner.Policy.Default
      )

    assertEquals(supportOnly.requests, Vector.empty)
    assertEquals(noEngineRoles.requests, Vector.empty)
    assertEquals(missingPolicy.left.toOption, Some("probe_policy_missing_role:surface_capability"))
    assertEquals(transitionRoleOnCurrentBoard.left.toOption, Some("probe_transition_role_requires_exact_transition:anti_causality_check"))
    assertEquals(moveLocalScope.left.toOption, Some("probe_scope_not_exact:move_local"))
    assertEquals(transitionRoleWithoutTauRequirement.left.toOption, Some("probe_transition_role_requires_tau:anti_causality_check"))
    assertEquals(missingOwner.left.toOption, Some("probe_burden_owner_required"))
    assertEquals(illegalTau.left.toOption, Some("probe_tau_replay_invalid"))

  private val tau =
    StrategyGeometryFoundation.ExactTransitionIdentity(
      beforeFen = "8/8/8/8/8/8/4k3/7K w - - 0 1",
      playedMove = "h1g1",
      currentFen = "8/8/8/8/8/8/4k3/6K1 b - - 1 1",
      nodeId = "mainline:1",
      ply = 1,
      variant = "standard"
    )

  private def transitionBurden(
      requiredRoles: Set[CertificationEngineRole],
      requiresExactTransition: Option[Boolean] = None,
      disposition: StrategyGeometryFoundation.PublicPathDisposition = StrategyGeometryFoundation.PublicPathDisposition.Certified
  ): StrategyGeometryFoundation.ProofBurden =
    val resolvedRequiresExactTransition =
      requiresExactTransition.getOrElse:
        requiredRoles.exists(role =>
          role == CertificationEngineRole.AntiCausalityCheck || role == CertificationEngineRole.CausalityCheck
        )
    StrategyGeometryFoundation.ProofBurden(
      family = StrategyGeometryFoundation.CertifiedFactFamily.MoveCausalTactic,
      owner = Some(Color.White),
      anchor = WitnessAnchor.SquareAnchor(square("g1")),
      route = "moved_piece_left_loose",
      scope = "exact_transition",
      requiredEngineRoles = requiredRoles,
      requiredCarrierKinds = Set(StrategyProjectionCarrierKind.Delta, StrategyProjectionCarrierKind.Object),
      requiresExactTransition = resolvedRequiresExactTransition,
      disposition = disposition
    )

  private def currentBoardBurden(
      requiredRoles: Set[CertificationEngineRole],
      scope: String = "exact_current_board",
      owner: Option[Color] = Some(Color.White)
  ): StrategyGeometryFoundation.ProofBurden =
    StrategyGeometryFoundation.ProofBurden(
      family = StrategyGeometryFoundation.CertifiedFactFamily.EndgameConversionHolding,
      owner = owner,
      anchor = WitnessAnchor.BoardAnchor,
      route = "king_entry_conversion",
      scope = scope,
      requiredEngineRoles = requiredRoles,
      requiredCarrierKinds = Set(StrategyProjectionCarrierKind.ExactBoard)
    )

  private def square(key: String): chess.Square =
    chess.Square.fromKey(key).getOrElse(fail(s"bad square $key"))
