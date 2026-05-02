package lila.commentary.projection

import chess.Color

import lila.commentary.CommentaryCore
import lila.commentary.certification.{ CertificationEnginePolicyFingerprint, CertificationEngineRole }
import lila.commentary.witness.WitnessAnchor

class StrategyGeometryFoundationContractTest extends munit.FunSuite:

  test("P1 exact transition identity and proof burden are stable inputs to certification"):
    val tau =
      StrategyGeometryFoundation.ExactTransitionIdentity(
        beforeFen = "8/8/8/8/8/8/4k3/7K w - - 0 1",
        playedMove = "h1g1",
        currentFen = "8/8/8/8/8/8/4k3/6K1 b - - 1 1",
        nodeId = "mainline:1",
        ply = 1,
        variant = "standard"
      )
    val beta =
      StrategyGeometryFoundation.ProofBurden(
        family = StrategyGeometryFoundation.CertifiedFactFamily.MoveCausalTactic,
        owner = Some(Color.White),
        anchor = WitnessAnchor.SquareAnchor(square("g1")),
        route = "moved_piece_left_loose",
        scope = "exact_transition",
        requiredEngineRoles = Set(CertificationEngineRole.AntiCausalityCheck),
        requiredCarrierKinds = Set(StrategyProjectionCarrierKind.Delta, StrategyProjectionCarrierKind.Object),
        requiresExactTransition = true
      )

    assertEquals(tau.kind, StrategyGeometryFoundation.ExactIdentityKind.ExactTransition)
    assertEquals(tau.cacheKey.contains("source-pgn"), false)
    assertEquals(tau.cacheKey.contains("missing_pgn_file"), false)
    assertEquals(tau.legalReplayMatchesCurrent, true)
    assertEquals(tau.copy(playedMove = "h1h3").legalReplayMatchesCurrent, false)
    assertEquals(beta.canEnterSxxAdmission, true)
    assertEquals(beta.disposition, StrategyGeometryFoundation.PublicPathDisposition.Certified)
    assertEquals(beta.requiredEngineRoles.map(_.key), Set("anti_causality_check"))

  test("P1 certified truth K is the only geometry-admissible public truth object"):
    val current =
      CommentaryCore
        .extractStrategicObjectsFromFenFailClosed("8/8/8/8/4K3/8/4k3/8 w - - 0 1")
        .fold(fail(_), identity)
    val engineRoleWithoutCertificationCarrier =
      StrategyGeometryFoundation.CertifiedTruth.fromTrustedCarriers(
        id = "k-s23-white-e4-entry",
        rootState = Some(current.rootState),
        burden = StrategyGeometryFoundation.ProofBurden(
          family = StrategyGeometryFoundation.CertifiedFactFamily.EndgameConversionHolding,
          owner = Some(Color.White),
          anchor = WitnessAnchor.SquareAnchor(square("e4")),
          route = "king_entry_conversion",
          scope = "exact_current_board",
          requiredEngineRoles = Set(CertificationEngineRole.BestDefenseSurvival),
          requiredCarrierKinds = Set(StrategyProjectionCarrierKind.ExactBoard)
        ),
        engineRoles = Set(CertificationEngineRole.BestDefenseSurvival),
        lowerCarrierRefs = Vector(
          StrategyProjectionCarrierRef(
            kind = StrategyProjectionCarrierKind.ExactBoard,
            id = "k-s23-exact-board-carrier",
            owner = "white",
            anchor = "e4",
            route = "king_entry_conversion",
            scope = "exact_current_board"
          )
        )
      )
    val truth =
      StrategyGeometryFoundation.CertifiedTruth
        .fromTrustedCarriers(
          id = "k-s23-white-e4-entry",
          rootState = Some(current.rootState),
          burden = StrategyGeometryFoundation.ProofBurden(
            family = StrategyGeometryFoundation.CertifiedFactFamily.EndgameConversionHolding,
            owner = Some(Color.White),
            anchor = WitnessAnchor.SquareAnchor(square("e4")),
            route = "king_entry_conversion",
            scope = "exact_current_board",
            requiredEngineRoles = Set(CertificationEngineRole.BestDefenseSurvival),
            requiredCarrierKinds = Set(StrategyProjectionCarrierKind.ExactBoard, StrategyProjectionCarrierKind.Certification)
          ),
          engineRoles = Set(CertificationEngineRole.BestDefenseSurvival),
          engineProofIdentity = Some(engineProofIdentity(CertificationEngineRole.BestDefenseSurvival)),
          lowerCarrierRefs = Vector(
            StrategyProjectionCarrierRef(
              kind = StrategyProjectionCarrierKind.ExactBoard,
              id = "k-s23-exact-board-carrier",
              owner = "white",
              anchor = "e4",
              route = "king_entry_conversion",
              scope = "exact_current_board"
            ),
            StrategyProjectionCarrierRef(
              kind = StrategyProjectionCarrierKind.Certification,
              id = "k-s23-certification-carrier",
              owner = "white",
              anchor = "e4",
              route = "king_entry_conversion",
              scope = "exact_current_board"
            )
          )
        )
        .getOrElse(fail("expected certified truth factory to mint engine-role K with certification carrier"))
    val mismatchedIdentity =
      StrategyGeometryFoundation.CertifiedTruth.fromTrustedCarriers(
        id = "k-s23-engine-mismatched-identity",
        rootState = Some(current.rootState),
        burden = truth.burden,
        engineRoles = Set(CertificationEngineRole.BestDefenseSurvival),
        engineProofIdentity = Some(engineProofIdentity(CertificationEngineRole.BestDefenseSurvival).copy(certificationEvidenceId = Some("other-certification-carrier"))),
        lowerCarrierRefs = truth.lowerCarrierRefs
      )

    assertEquals(engineRoleWithoutCertificationCarrier, None)
    assertEquals(mismatchedIdentity, None)
    assertEquals(truth.disposition, StrategyGeometryFoundation.PublicPathDisposition.Certified)
    assertEquals(truth.canEnterGeometry, true)
    assertEquals(truth.sameRootAs(current.rootState), true)
    assertEquals(truth.hasRequiredLowerCarrierKinds, true)
    assertEquals(truth.engineRoles.map(_.key), Set("best_defense_survival"))

  test("F1-F7 slice descriptors are the public Sxx source of truth"):
    val descriptors = StrategyGeometryFoundation.sliceDescriptors

    assertEquals(descriptors.map(_.region.regionId), Vector("A_S23_endgame_entry_or_opposition", "A_S23_line_access_activity"))
    assertEquals(
      StrategyGeometryFoundation.admittedRegionsByBand("S23").map(_.regionId),
      descriptors.map(_.region.regionId)
    )
    assert(descriptors.forall(_.carrierBindingLaws.contains(StrategyGeometryFoundation.CarrierBindingLaw.SameOwnerAnchorRouteScope)))
    assert(descriptors.forall(_.falsificationBurden.keys.contains("sibling_band_rival")))
    assert(descriptors.forall(_.phraseCapability.sanOnlyVariationEvidence))
    assert(descriptors.forall(!_.phraseCapability.allowsFallbackText))
    assert(descriptors.forall(!_.phraseCapability.allowsBestForcedLanguage))
    assert(descriptors.forall(!_.phraseCapability.allowsEngineLanguage))
    assert(descriptors.forall(_.phraseCapability.forbiddenTerms.contains("engine")))

  test("slice descriptors expose the standard algebraic A mu d overlap phrase falsification shape"):
    val descriptors = StrategyGeometryFoundation.sliceDescriptors

    assert(descriptors.nonEmpty)
    descriptors.foreach: descriptor =>
      assertEquals(descriptor.region.regionId.startsWith("A_"), true)
      assertEquals(descriptor.region.prototypeKey, descriptor.center.key)
      assertEquals(descriptor.distanceAlgebra, StrategyGeometryFoundation.DistanceAlgebra.RouteScopeFamilyAdditive)
      assertEquals(descriptor.overlapPolicy, StrategyGeometryFoundation.OverlapPolicy.AllowMultipleMemberships)
      assert(descriptor.projectionEvidenceKindsSatisfiedBy(descriptor.evidenceKindsForRoute(descriptor.proofBurden.route), descriptor.proofBurden.route))
      assert(descriptor.falsificationBurden.keys.toSet.contains("sibling_band_rival"))
      assert(descriptor.falsificationBurden.keys.toSet.contains("wrong_evidence_kind"))
      assert(descriptor.falsificationBurden.keys.toSet.contains("missing_exact_board_fact"))
      assertEquals(
        descriptor.distanceAlgebra
          .distance(
            descriptor.center,
            descriptor.center.primaryFamily,
            descriptor.center.routeDistances.minBy(_._2)._1,
            descriptor.center.scopeDistances.minBy(_._2)._1
          )
          .exists(_.isFinite),
        true
      )
      assertEquals(
        descriptor.distanceAlgebra.distance(
          descriptor.center,
          descriptor.center.primaryFamily,
          "route_not_declared_by_descriptor",
          descriptor.center.scopeDistances.minBy(_._2)._1
        ),
        None
      )

  test("F4-F5 descriptor binding laws and engine proof identity policy are executable"):
    val current =
      CommentaryCore
        .extractStrategicObjectsFromFenFailClosed("8/8/8/8/4K3/8/4k3/8 w - - 0 1")
        .fold(fail(_), identity)
    val descriptor = StrategyGeometryFoundation.sliceDescriptorsByRegionId("A_S23_endgame_entry_or_opposition")
    val truth =
      StrategyGeometryFoundation.CertifiedTruth
        .fromTrustedCarriers(
          id = "k-s23-white-board-entry",
          rootState = Some(current.rootState),
          burden = StrategyGeometryFoundation.ProofBurden(
            family = StrategyGeometryFoundation.CertifiedFactFamily.EndgameConversionHolding,
            owner = Some(Color.White),
            anchor = WitnessAnchor.BoardAnchor,
            route = "king_entry_conversion",
            scope = "exact_current_board",
            requiredCarrierKinds = Set(StrategyProjectionCarrierKind.ExactBoard)
          ),
          lowerCarrierRefs = Vector(
            StrategyProjectionCarrierRef(
              kind = StrategyProjectionCarrierKind.ExactBoard,
              id = "k-s23-exact-board-carrier",
              owner = "white",
              anchor = "board",
              route = "king_entry_conversion",
              scope = "exact_current_board",
              binding = Map("entry_square" -> "e4", "contact_square" -> "d5")
            )
          )
        )
        .getOrElse(fail("expected descriptor-bound K"))
    val missingContact =
      StrategyGeometryFoundation.CertifiedTruth
        .fromTrustedCarriers(
          id = "k-s23-white-board-entry-missing-contact",
          rootState = Some(current.rootState),
          burden = truth.burden,
          lowerCarrierRefs = truth.lowerCarrierRefs.map(_.copy(binding = Map("entry_square" -> "e4")))
        )
        .getOrElse(fail("expected K construction before descriptor binding check"))
    val enginePolicyDescriptor =
      descriptor.copy(engineProofIdentityPolicy = StrategyGeometryFoundation.EngineProofIdentityPolicy.EngineBackedPublicK)

    assertEquals(descriptor.carrierBindingSatisfiedBy(truth), true)
    assertEquals(descriptor.carrierBindingSatisfiedBy(missingContact), false)
    assertEquals(enginePolicyDescriptor.engineProofIdentitySatisfiedBy(truth), false)

  test("carrier binding law rejects cross-carrier payload rebinding"):
    val current =
      CommentaryCore
        .extractStrategicObjectsFromFenFailClosed("8/8/8/8/4K3/8/4k3/8 w - - 0 1")
        .fold(fail(_), identity)
    val descriptor = StrategyGeometryFoundation.sliceDescriptorsByRegionId("A_S23_endgame_entry_or_opposition")
    val burden =
      StrategyGeometryFoundation.ProofBurden(
        family = StrategyGeometryFoundation.CertifiedFactFamily.EndgameConversionHolding,
        owner = Some(Color.White),
        anchor = WitnessAnchor.BoardAnchor,
        route = "king_entry_conversion",
        scope = "exact_current_board",
        requiredEngineRoles = Set(CertificationEngineRole.BestDefenseSurvival),
        requiredCarrierKinds = Set(StrategyProjectionCarrierKind.ExactBoard, StrategyProjectionCarrierKind.Certification)
      )
    val truth =
      StrategyGeometryFoundation.CertifiedTruth
        .fromTrustedCarriers(
          id = "k-s23-cross-carrier-rebinding",
          rootState = Some(current.rootState),
          burden = burden,
          engineRoles = Set(CertificationEngineRole.BestDefenseSurvival),
          engineProofIdentity = Some(engineProofIdentityFor("board", "king_entry_conversion", CertificationEngineRole.BestDefenseSurvival)),
          lowerCarrierRefs = Vector(
            StrategyProjectionCarrierRef(
              kind = StrategyProjectionCarrierKind.ExactBoard,
              id = "k-s23-exact-board-carrier",
              owner = "white",
              anchor = "board",
              route = "king_entry_conversion",
              scope = "exact_current_board",
              binding = Map("entry_square" -> "e4", "contact_square" -> "d5")
            ),
            StrategyProjectionCarrierRef(
              kind = StrategyProjectionCarrierKind.Certification,
              id = "k-s23-certification-carrier",
              owner = "white",
              anchor = "board",
              route = "king_entry_conversion",
              scope = "exact_current_board",
              binding = Map("entry_square" -> "e5", "contact_square" -> "d5")
            )
          )
        )
        .getOrElse(fail("expected K construction before descriptor binding check"))

    assertEquals(descriptor.carrierBindingSatisfiedBy(truth), false)

  test("engine proof identity must bind the Q request id, policy fingerprint, and engine config consistently"):
    val valid = engineProofIdentity(CertificationEngineRole.BestDefenseSurvival)
    val missingRequest = valid.copy(probeRequestId = None)
    val malformedRequest = valid.copy(probeRequestId = Some("not-q-best-defense"))
    val mismatchedPolicy = valid.copy(probePolicyFingerprint = Some("other-policy"))
    val engineFingerprintAliasPolicy = valid.copy(probePolicyFingerprint = valid.engineConfigFingerprint)
    val forgedSameRole = valid.copy(probeRequestId = Some("q-best-defense-survival-forged-family-white-e4-king-entry-conversion"))
    val burden =
      StrategyGeometryFoundation.ProofBurden(
        family = StrategyGeometryFoundation.CertifiedFactFamily.EndgameConversionHolding,
        owner = Some(Color.White),
        anchor = WitnessAnchor.SquareAnchor(square("e4")),
        route = "king_entry_conversion",
        scope = "exact_current_board",
        requiredEngineRoles = Set(CertificationEngineRole.BestDefenseSurvival),
        requiredCarrierKinds = Set(StrategyProjectionCarrierKind.ExactBoard, StrategyProjectionCarrierKind.Certification)
      )

    assertEquals(valid.completeFor(Set(CertificationEngineRole.BestDefenseSurvival)), true)
    assertEquals(missingRequest.completeFor(Set(CertificationEngineRole.BestDefenseSurvival)), false)
    assertEquals(malformedRequest.completeFor(Set(CertificationEngineRole.BestDefenseSurvival)), false)
    assertEquals(mismatchedPolicy.completeFor(Set(CertificationEngineRole.BestDefenseSurvival)), false)
    assertEquals(engineFingerprintAliasPolicy.completeFor(Set(CertificationEngineRole.BestDefenseSurvival)), false)
    assertEquals(valid.matchesProofBurden(burden, Set(CertificationEngineRole.BestDefenseSurvival)), true)
    assertEquals(forgedSameRole.completeFor(Set(CertificationEngineRole.BestDefenseSurvival)), true)
    assertEquals(forgedSameRole.matchesProofBurden(burden, Set(CertificationEngineRole.BestDefenseSurvival)), false)

  test("P1 minimal A mu d overlap contract is explicit and cannot create truth"):
    val s23Regions = StrategyGeometryFoundation.admittedRegionsByBand("S23")

    assert(s23Regions.exists(_.regionId == "A_S23_endgame_entry_or_opposition"))
    assert(s23Regions.forall(_.requiresHardAdmission))
    assertEquals(
      s23Regions.flatMap(_.families.map(_.key)).toSet,
      Set("endgame_conversion_holding", "line_access_activity")
    )
    assertEquals(StrategyGeometryFoundation.GeometryDistance.outsideHardAdmission.isFinite, false)
    assertEquals(StrategyGeometryFoundation.OverlapPolicy.AllowMultipleMemberships.canStrengthenTruth, false)
    assertEquals(StrategyGeometryFoundation.OverlapPolicy.AllowMultipleMemberships.canSelectPublicPrimary, true)

  test("P1 anti-causality barriers and anti-case families are centralized"):
    assertEquals(
      StrategyGeometryFoundation.antiCausalityBarriers.map(_.key),
      Vector(
        "pre_existing_fact",
        "already_loose_or_pinned",
        "pawn_move_shape_shift",
        "king_move",
        "mate_dominated",
        "material_collapse",
        "coincidental_standing_tactic"
      )
    )
    assertEquals(
      StrategyGeometryFoundation.antiCaseFamilies.map(_.key),
      Vector(
        "starting_position_home_pawn_xray",
        "pre_existing_loose_piece",
        "pre_existing_pin_or_xray",
        "mate_dominated_tactical_smell",
        "material_collapse_tactical_smell"
      )
    )
    assert(
      StrategyGeometryFoundation.antiCaseFamilies.forall(_.disposition == StrategyGeometryFoundation.PublicPathDisposition.AntiCase)
    )
    assert(StrategyGeometryFoundation.antiCaseFamilies.forall(!_.mayEmitPublicText))

  private def square(key: String): chess.Square =
    chess.Square.fromKey(key).getOrElse(fail(s"bad square $key"))

  private def engineProofIdentity(
      roles: CertificationEngineRole*
  ): StrategyGeometryFoundation.EngineProofIdentity =
    engineProofIdentityFor("e4", "king_entry_conversion", roles*)

  private def engineProofIdentityFor(
      anchor: String,
      route: String,
      roles: CertificationEngineRole*
  ): StrategyGeometryFoundation.EngineProofIdentity =
    StrategyGeometryFoundation.EngineProofIdentity(
      probeRequestId = Some(Vector("q", "best-defense-survival", "endgame-conversion-holding", "white", anchor, route.replace('_', '-')).mkString("-")),
      probePolicyFingerprint = Some(
        CertificationEnginePolicyFingerprint.defaultForRole(
          "default-strategy-probe-engine",
          CertificationEngineRole.BestDefenseSurvival
        )
      ),
      engineConfigFingerprint = Some("default-strategy-probe-engine"),
      targetDepth = Some(18),
      floorDepth = Some(16),
      multiPv = Some(3),
      certificationEvidenceId = Some("k-s23-certification-carrier"),
      satisfiedRoleInvariants = roles.toSet,
      semanticCoverageRoles = roles.filter(_ == CertificationEngineRole.BestDefenseSurvival).toSet
    )
