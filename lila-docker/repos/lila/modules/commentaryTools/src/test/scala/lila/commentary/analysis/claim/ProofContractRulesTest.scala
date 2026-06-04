package lila.commentary.analysis.claim

import lila.commentary.analysis.*
import lila.commentary.analysis.semantic.StrategicObservationIds.{ ProofFamilyId, ProofSourceId }
import munit.FunSuite

class ProofContractRulesTest extends FunSuite:

  test("every strategic taxonomy entry has a proof contract or explicit deferred status") {
    val missingThemes =
      PlanTaxonomy.PlanTheme.ranked.filter(theme =>
        ProofContractRules.contractForProofFamily(theme.id).isEmpty
      )
    val missingSubplans =
      PlanTaxonomy.PlanKind.values.toList.filter(subplan =>
        ProofContractRules.contractForProofFamily(subplan.id).isEmpty
      )

    assertEquals(missingThemes, Nil)
    assertEquals(missingSubplans, Nil)
    assert(
      ProofContractRules.contracts.exists(_.status == ProofContractStatus.Deferred),
      clues(ProofContractRules.contracts.map(contract => contract.id -> contract.status))
    )
  }

  test("releasable contracts declare concrete witness requirements and legal scopes") {
    val releasable =
      ProofContractRules.contracts.filter(_.authorityEligible)

    assert(releasable.nonEmpty)
    assert(
      releasable.forall(contract =>
        contract.allowedScopes.exists(scope =>
          scope == PlayerFacingPacketScope.MoveLocal || scope == PlayerFacingPacketScope.PositionLocal
        ) &&
          contract.requiredWitnesses.contains(ProofWitness.OwnerSeed) &&
          contract.requiredWitnesses.contains(ProofWitness.NoTacticalVeto) &&
          contract.defaultFailureTaxonomy.nonEmpty
      ),
      clues(releasable)
    )
  }

  test("exact slice contracts are runtime predicates, not source-witness-id gates") {
    val exactContracts =
      ProofContractRules.contracts.filter(_.requiredWitnesses.contains(ProofWitness.ExactSlice))

    assert(exactContracts.nonEmpty)
    assert(
      exactContracts.forall(contract =>
        !contract.id.toLowerCase.contains("source-") &&
          !contract.acceptedSources.exists(_.toLowerCase.contains("source-"))
      ),
      clues(exactContracts)
    )
  }

  test("immediate tactical-gain taxonomy cannot become strategic authority contract") {
    val tacticalContracts =
      ProofContractRules.contracts.filter(_.theme.contains(PlanTaxonomy.PlanTheme.ImmediateTacticalGain))

    assert(tacticalContracts.nonEmpty)
    assert(
      tacticalContracts.forall(contract =>
        !contract.authorityEligible &&
          contract.status == ProofContractStatus.BackendOnly &&
          contract.defaultFailureTaxonomy == "tactical_truth_first"
      ),
      clues(tacticalContracts)
    )
  }

  test("known admitted B/C packets resolve through proof contracts") {
    val carlsbad =
      ProofContractRules
        .contractForProofFamily(PlanTaxonomy.PlanKind.BackwardPawnTargeting.id)
        .getOrElse(fail("missing Carlsbad target contract"))
    val queenTrade =
      ProofContractRules
        .contractForProofFamily(PlanTaxonomy.PlanKind.QueenTradeShield.id)
        .getOrElse(fail("missing queen trade shield contract"))

    assert(carlsbad.certifiedEligible, clues(carlsbad))
    assert(carlsbad.acceptedSources.contains(PlayerFacingTruthModePolicy.CarlsbadFixedTargetProbeProofSource), clues(carlsbad))
    val minority =
      ProofContractRules
        .contractForProofFamily(PlanTaxonomy.PlanKind.MinorityAttackFixation.id)
        .getOrElse(fail("missing minority attack fixation contract"))
    assertEquals(minority.status, ProofContractStatus.Deferred)
    assert(!minority.authorityEligible, clues(minority))
    assert(!minority.acceptedSources.contains(PlayerFacingTruthModePolicy.CarlsbadFixedTargetProbeProofSource), clues(minority))
    assert(!minority.acceptedSources.contains("minority_attack_fixation"), clues(minority))
    assert(queenTrade.supportedLocalEligible, clues(queenTrade))
    assert(!queenTrade.certifiedEligible, clues(queenTrade))
    assert(queenTrade.acceptedSources.contains(PlayerFacingTruthModePolicy.QueenTradeShieldProofSource), clues(queenTrade))
  }

  test("IQP inducement is supported-local releasable only through exact probe proof") {
    val contract =
      ProofContractRules
        .contractForProofFamily(PlanTaxonomy.PlanKind.IQPInducement.id)
        .getOrElse(fail("missing IQP inducement contract"))

    assertEquals(contract.status, ProofContractStatus.Releasable)
    assert(!contract.certifiedEligible, clues(contract))
    assert(contract.supportedLocalEligible, clues(contract))
    assert(contract.acceptedSources.contains(PlayerFacingTruthModePolicy.IQPInducementProbeProofSource), clues(contract))
    assert(!contract.id.toLowerCase.contains("source-"), clues(contract))
    assert(!contract.acceptedSources.exists(_.toLowerCase.contains("source-")), clues(contract))
  }

  test("outpost entrenchment is supported-local only with exact occupation proof and stable branch") {
    val contract =
      ProofContractRules
        .contractForProofFamily(PlanTaxonomy.PlanKind.OutpostEntrenchment.id)
        .getOrElse(fail("missing OutpostEntrenchment contract"))
    val packet =
      exactSliceContractPacket(
        proofSource = PlayerFacingTruthModePolicy.OutpostEntrenchmentProofSource,
        proofFamily = PlayerFacingTruthModePolicy.OutpostEntrenchmentProofFamily,
        scope = PlayerFacingPacketScope.MoveLocal,
        proof = Some(PlayerFacingExactSliceProof.OutpostOccupation("knight", "e5"))
      )
    val missingProof = packet.copy(proofPathWitness = packet.proofPathWitness.copy(exactSliceProof = None))
    val branchMissing = packet.copy(sameBranchState = PlayerFacingSameBranchState.Missing)
    val unstable = packet.copy(persistence = PlayerFacingClaimPersistence.BestDefenseOnly)

    assertEquals(contract.status, ProofContractStatus.Releasable)
    assert(!contract.certifiedEligible, clues(contract))
    assert(contract.supportedLocalEligible, clues(contract))
    assert(contract.requiredWitnesses.contains(ProofWitness.ExactSlice), clues(contract))
    assert(contract.requiredWitnesses.contains(ProofWitness.BranchProof), clues(contract))
    assert(contract.requiredWitnesses.contains(ProofWitness.StablePersistence), clues(contract))
    assertEquals(ProofContractRules.failureCodes(packet), Nil)
    assert(ProofContractRules.supportedLocalAdmissible(packet), clues(packet))
    assert(!ProofContractRules.certifiedOwnerAdmissible(packet), clues(packet))
    assert(ProofContractRules.failureCodes(missingProof).contains("witness:exact_slice_missing"))
    assert(ProofContractRules.failureCodes(branchMissing).contains("witness:branch_not_proven"))
    assert(ProofContractRules.failureCodes(unstable).contains("witness:persistence_not_stable"))
  }

  test("supported-local admission requires accepted source and required witnesses") {
    val packet =
      supportedIqpPacket()
    val wrongSource =
      packet.copy(proofSource = "minority_attack_semantic")
    val missingStructureWitness =
      packet.copy(
        proofPathWitness =
          PlayerFacingProofPathWitness(
            ownerSeedTerms = List("iqp"),
            continuationTerms = List("d5")
          )
      )

    assertEquals(ProofContractRules.failureCodes(packet), Nil)
    assert(ProofContractRules.supportedLocalAdmissible(packet), clues(packet))
    assert(!ProofContractRules.certifiedOwnerAdmissible(packet), clues(packet))
    assert(!ProofContractRules.supportedLocalAdmissible(wrongSource), clues(ProofContractRules.failureCodes(wrongSource)))
    assert(ProofContractRules.failureCodes(wrongSource).contains("contract:source_not_accepted"))
    assert(!ProofContractRules.supportedLocalAdmissible(missingStructureWitness), clues(ProofContractRules.failureCodes(missingStructureWitness)))
    assert(ProofContractRules.failureCodes(missingStructureWitness).contains("witness:structure_transition_missing"))
  }

  test("required no-tactical-veto witness fails closed when tactical suppression is present") {
    val packet =
      supportedIqpPacket().copy(
        suppressionReasons = List("truth_contract_tactical_refutation")
      )

    assert(ProofContractRules.failureCodes(packet).contains("witness:tactical_veto_present"))
    assert(!ProofContractRules.supportedLocalAdmissible(packet), clues(ProofContractRules.failureCodes(packet)))
  }

  test("required claim-only surface witness fails closed outside weak-main surface") {
    val packet =
      supportedIqpPacket().copy(
        fallbackMode = PlayerFacingClaimFallbackMode.LineOnly
      )

    assert(ProofContractRules.failureCodes(packet).contains("witness:claim_only_surface_missing"))
    assert(!ProofContractRules.supportedLocalAdmissible(packet), clues(ProofContractRules.failureCodes(packet)))
  }

  test("semantic support observations have no proof contract authority") {
    assertEquals(ProofContractRules.contractForProofFamily("target_pressure_semantic"), None)
    assertEquals(ProofContractRules.contractForProofFamily("minority_attack_semantic"), None)
  }

  test("color-complex squeeze requires exact board-backed probe authority") {
    val contract =
      ProofContractRules
        .contractForProofFamily("color_complex_squeeze")
        .getOrElse(fail("missing color-complex squeeze contract"))

    assertEquals(contract.status, ProofContractStatus.Releasable)
    assert(contract.certifiedEligible, clues(contract))
    assert(contract.supportedLocalEligible, clues(contract))
    assertEquals(contract.acceptedSources, Set(PlayerFacingTruthModePolicy.ColorComplexSqueezeProbeProofSource))
    assertEquals(contract.allowedScopes, Set(PlayerFacingPacketScope.PositionLocal))
    assert(contract.requiredWitnesses.contains(ProofWitness.ExactSlice), clues(contract))
    assertEquals(contract.defaultFailureTaxonomy, "color_complex_authority_closed")

    val oldStringMatchedPacket = PlayerFacingClaimPacket(
      proofSource = "color_complex_squeeze",
      proofFamily = "color_complex_squeeze",
      scope = PlayerFacingPacketScope.PositionLocal,
      fallbackMode = PlayerFacingClaimFallbackMode.WeakMain,
      anchorTerms = List("e5", "bishop", "knight"),
      proofPathWitness = PlayerFacingProofPathWitness(
        ownerSeedTerms = List("e5", "bishop", "knight"),
        continuationTerms = List("e5"),
        structureTransitionTerms = List("color_complex_squeeze")
      )
    )
    val exactProbeTerms =
      List(
        "e5",
        "weak_square:e5",
        "color_complex:dark",
        "minor_piece:knight_c4",
        "attacks:e5",
        "minor_piece_attack:c4-e5",
        "color_complex_squeeze_probe"
      )
    val exactProbePacket = PlayerFacingClaimPacket(
      claimGate =
        PlanEvidenceEvaluator.ClaimCertification(
          certificateStatus = PlayerFacingCertificateStatus.Valid,
          quantifier = PlayerFacingClaimQuantifier.BestResponse,
          attributionGrade = PlayerFacingClaimAttributionGrade.Distinctive,
          stabilityGrade = PlayerFacingClaimStabilityGrade.Stable,
          provenanceClass = PlayerFacingClaimProvenanceClass.ProbeBacked,
          ontologyFamily = PlayerFacingClaimOntologyKind.ColorComplexSqueeze
        ),
      proofSource = PlayerFacingTruthModePolicy.ColorComplexSqueezeProbeProofSource,
      proofFamily = "color_complex_squeeze",
      scope = PlayerFacingPacketScope.PositionLocal,
      triggerKind = "position_probe",
      anchorTerms = List("e5"),
      bestDefenseMove = Some("e8f8"),
      bestDefenseBranchKey = Some("c4e5|e8f8"),
      sameBranchState = PlayerFacingSameBranchState.Proven,
      persistence = PlayerFacingClaimPersistence.Stable,
      fallbackMode = PlayerFacingClaimFallbackMode.WeakMain,
      proofPathWitness = PlayerFacingProofPathWitness(
        ownerSeedTerms =
          exactProbeTerms.filterNot(_ == "color_complex_squeeze_probe"),
        continuationTerms = List("c4e5|e8f8"),
        structureTransitionTerms = List("color_complex_squeeze_probe", "weak_square:e5", "minor_piece_attack:c4-e5"),
        exactSliceProof =
          Some(
            PlayerFacingExactSliceProof.ColorComplexSqueeze(
              targetSquare = "e5",
              squareColor = "dark",
              minorPieceRole = "knight",
              minorPieceSquare = "c4"
            )
          )
      )
    )
    val exactProbeWithBadGeometry =
      exactProbePacket.copy(
        proofPathWitness =
          exactProbePacket.proofPathWitness.copy(
            exactSliceProof =
              Some(
                PlayerFacingExactSliceProof.ColorComplexSqueeze(
                  targetSquare = "e5",
                  squareColor = "dark",
                  minorPieceRole = "knight",
                  minorPieceSquare = "c5"
                )
              )
          )
      )
    val exactProbeWithWrongRoleMirror =
      exactProbePacket.copy(
        proofPathWitness =
          exactProbePacket.proofPathWitness.copy(
            ownerSeedTerms =
              exactProbePacket.proofPathWitness.ownerSeedTerms
                .filterNot(_.startsWith("minor_piece:")) :+ "minor_piece:bishop_c4"
          )
      )
    val exactProbeWithoutExactTerms =
      exactProbePacket.copy(
        proofPathWitness =
          PlayerFacingProofPathWitness(
            ownerSeedTerms = List("e5", "bishop", "knight"),
            continuationTerms = List("c4e5|e8f8"),
            structureTransitionTerms = List("color_complex_squeeze_probe")
          )
      )

    assert(!ProofContractRules.supportedLocalAdmissible(oldStringMatchedPacket), clues(ProofContractRules.failureCodes(oldStringMatchedPacket)))
    assert(ProofContractRules.failureCodes(oldStringMatchedPacket).contains("contract:source_not_accepted"))
    assert(!ProofContractRules.supportedLocalAdmissible(exactProbeWithoutExactTerms), clues(exactProbeWithoutExactTerms))
    assert(ProofContractRules.failureCodes(exactProbeWithoutExactTerms).contains("witness:exact_slice_missing"))
    assert(ProofContractRules.failureCodes(exactProbeWithBadGeometry).contains("witness:exact_slice_missing"))
    assert(ProofContractRules.failureCodes(exactProbeWithWrongRoleMirror).contains("witness:exact_slice_missing"))
    assertEquals(ProofContractRules.failureCodes(exactProbePacket), Nil)
    assert(ProofContractRules.supportedLocalAdmissible(exactProbePacket), clues(exactProbePacket))
    assert(ProofContractRules.certifiedOwnerAdmissible(exactProbePacket), clues(exactProbePacket))
  }

  test("exact-slice contracts require typed exact-slice proof, not generic witness strings") {
    val packet = PlayerFacingClaimPacket(
      claimGate =
        PlanEvidenceEvaluator.ClaimCertification(
          certificateStatus = PlayerFacingCertificateStatus.Valid,
          quantifier = PlayerFacingClaimQuantifier.BestResponse,
          attributionGrade = PlayerFacingClaimAttributionGrade.Distinctive,
          stabilityGrade = PlayerFacingClaimStabilityGrade.Stable,
          provenanceClass = PlayerFacingClaimProvenanceClass.ProbeBacked,
          ontologyFamily = PlayerFacingClaimOntologyKind.Pressure
        ),
      proofSource = PlayerFacingTruthModePolicy.ExactTargetFixationProofSource,
      proofFamily = PlanTaxonomy.PlanKind.StaticWeaknessFixation.id,
      scope = PlayerFacingPacketScope.MoveLocal,
      triggerKind = "target_fixation",
      anchorTerms = List("d6", "fixed_target:d6"),
      bestDefenseMove = Some("b8a6"),
      bestDefenseBranchKey = Some("f3d2|b8a6"),
      sameBranchState = PlayerFacingSameBranchState.Proven,
      persistence = PlayerFacingClaimPersistence.Stable,
      fallbackMode = PlayerFacingClaimFallbackMode.WeakMain,
      proofPathWitness =
        PlayerFacingProofPathWitness(
          ownerSeedTerms = List("d6", "fixed_target:d6", "backward_pawn_target"),
          continuationTerms = List("exact_target_fixation", "fixed_target:d6", "best_branch:f3d2|b8a6"),
          structureTransitionTerms = List("weak_complex:d6", "backward_pawn_target")
        )
    )
    val typed =
      packet.copy(
        proofPathWitness =
          packet.proofPathWitness.copy(
            exactSliceProof =
              Some(
                PlayerFacingExactSliceProof.ExactTargetFixation("d6")
              )
          )
      )
    val mismatchedTargetTerms =
      typed.copy(
        anchorTerms = List("e5"),
        proofPathWitness =
          typed.proofPathWitness.copy(
            ownerSeedTerms = List("e5", "fixed_target:e5", "backward_pawn_target"),
            continuationTerms = List("exact_target_fixation", "fixed_target:e5", "best_branch:f3d2|b8a6"),
            structureTransitionTerms = List("weak_complex:e5", "backward_pawn_target")
          )
      )

    assert(!ProofContractRules.certifiedOwnerAdmissible(packet), clues(ProofContractRules.failureCodes(packet)))
    assert(ProofContractRules.failureCodes(packet).contains("witness:exact_slice_missing"))
    assert(ProofContractRules.failureCodes(mismatchedTargetTerms).contains("witness:exact_slice_missing"))
    assertEquals(ProofContractRules.failureCodes(typed), Nil)
    assert(ProofContractRules.certifiedOwnerAdmissible(typed), clues(ProofContractRules.failureCodes(typed)))
  }

  test("exact-slice proof facts own typed source family and shape matching") {
    val centralProof =
      PlayerFacingExactSliceProof.CentralBreakTiming("e2e4", "e4", "e2-e4")
    val centralPacket =
      exactSliceContractPacket(
        proofSource = PlanTaxonomy.PlanKind.CentralBreakTiming.id,
        proofFamily = PlanTaxonomy.PlanKind.CentralBreakTiming.id,
        scope = PlayerFacingPacketScope.MoveLocal,
        proof = Some(centralProof)
      )
    val wrongFamily =
      centralPacket.copy(proofFamily = ProofFamilyId.NeutralizeKeyBreak.wireKey)
    val defenderProof =
      PlayerFacingExactSliceProof.DefenderTrade("c5", "d4", "e5")
    val defenderPacket =
      exactSliceContractPacket(
        proofSource = PlayerFacingTruthModePolicy.DefenderTradeProofSource,
        proofFamily = PlanTaxonomy.PlanKind.DefenderTrade.id,
        scope = PlayerFacingPacketScope.MoveLocal,
        proof = Some(defenderProof)
      )
    val mismatchedDefenderTerms =
      defenderPacket.copy(
        proofPathWitness =
          defenderPacket.proofPathWitness.copy(
            ownerSeedTerms = List("defender_trade_branch", "defender:c5", "exchange_square:d4", "defended_target:f7")
          )
      )
    val badPieceProof =
      PlayerFacingExactSliceProof.BadPieceLiquidation("c8", "e6")
    val badPiecePacket =
      exactSliceContractPacket(
        proofSource = PlayerFacingTruthModePolicy.BadPieceLiquidationProofSource,
        proofFamily = PlanTaxonomy.PlanKind.BadPieceLiquidation.id,
        scope = PlayerFacingPacketScope.MoveLocal,
        proof = Some(badPieceProof)
      )
    val mismatchedBadPieceTerms =
      badPiecePacket.copy(
        proofPathWitness =
          badPiecePacket.proofPathWitness.copy(
            ownerSeedTerms = List("bad_piece_liquidation_branch", "bad_piece:c8", "exchange_square:d5")
          )
      )

    assert(PlayerFacingExactSliceProofFacts.matchesPacket(centralPacket, centralProof), clues(centralPacket))
    assert(!PlayerFacingExactSliceProofFacts.matchesPacket(wrongFamily, centralProof), clues(wrongFamily))
    assert(PlayerFacingExactSliceProofFacts.matchesPacket(defenderPacket, defenderProof), clues(defenderPacket))
    assert(!PlayerFacingExactSliceProofFacts.matchesPacket(mismatchedDefenderTerms, defenderProof), clues(mismatchedDefenderTerms))
    assert(PlayerFacingExactSliceProofFacts.matchesPacket(badPiecePacket, badPieceProof), clues(badPiecePacket))
    assert(!PlayerFacingExactSliceProofFacts.matchesPacket(mismatchedBadPieceTerms, badPieceProof), clues(mismatchedBadPieceTerms))
    assert(PlayerFacingExactSliceProofFacts.validShape(centralProof))
    assert(
      !PlayerFacingExactSliceProofFacts.validShape(
        PlayerFacingExactSliceProof.CentralBreakTiming("e2e4", "e5", "e2-e4")
      )
    )
    assertEquals(
      PlayerFacingExactSliceProofFacts.targetSquare(PlayerFacingExactSliceProof.ExactTargetFixation("D6")),
      Some("d6")
    )
    assertEquals(PlayerFacingExactSliceProofFacts.targetSquare(centralProof), None)
    assertEquals(PlayerFacingExactSliceProofFacts.targetSquare(defenderProof), Some("e5"))
    assertEquals(PlayerFacingExactSliceProofFacts.targetSquare(badPieceProof), Some("e6"))
  }

  test("carlsbad fixed-target probe fails closed without matching typed proof") {
    val base =
      exactSliceContractPacket(
        proofSource = PlayerFacingTruthModePolicy.CarlsbadFixedTargetProbeProofSource,
        proofFamily = PlanTaxonomy.PlanKind.BackwardPawnTargeting.id,
        scope = PlayerFacingPacketScope.PositionLocal,
        proof = Some(PlayerFacingExactSliceProof.CarlsbadFixedTarget("c6", minoritySupport = true))
      )
    val missingProof = base.copy(proofPathWitness = base.proofPathWitness.copy(exactSliceProof = None))
    val wrongProof =
      base.copy(
        proofPathWitness =
          base.proofPathWitness.copy(
            exactSliceProof = Some(PlayerFacingExactSliceProof.ExactTargetFixation("c6"))
          )
      )
    val wrongTargetTerms =
      base.copy(
        proofPathWitness =
          base.proofPathWitness.copy(
            ownerSeedTerms = List("c6", "fixed_target:c3"),
            continuationTerms = List("carlsbad_fixed_target_probe", "fixed_target:c3"),
            structureTransitionTerms = List("carlsbad_fixed_target_probe", "fixed_target:c3")
          )
      )

    assertEquals(ProofContractRules.failureCodes(base), Nil)
    assert(ProofContractRules.certifiedOwnerAdmissible(base), clues(base))
    assert(ProofContractRules.failureCodes(missingProof).contains("witness:exact_slice_missing"))
    assert(ProofContractRules.failureCodes(wrongProof).contains("witness:exact_slice_missing"))
    assert(ProofContractRules.failureCodes(wrongTargetTerms).contains("witness:exact_slice_missing"))
  }

  test("carlsbad fixed-target probe accepts mirrored c3 typed proof") {
    val packet =
      exactSliceContractPacket(
        proofSource = PlayerFacingTruthModePolicy.CarlsbadFixedTargetProbeProofSource,
        proofFamily = PlanTaxonomy.PlanKind.BackwardPawnTargeting.id,
        scope = PlayerFacingPacketScope.PositionLocal,
        proof = Some(PlayerFacingExactSliceProof.CarlsbadFixedTarget("c3", minoritySupport = true))
      )

    assertEquals(ProofContractRules.failureCodes(packet), Nil)
    assert(ProofContractRules.certifiedOwnerAdmissible(packet), clues(packet))
  }

  test("target-focused coordination exact proof requires two supports and a target marker") {
    val valid =
      exactSliceContractPacket(
        proofSource = PlayerFacingTruthModePolicy.TargetFocusedCoordinationProofSource,
        proofFamily = PlayerFacingTruthModePolicy.TargetFocusedCoordinationProofFamily,
        scope = PlayerFacingPacketScope.PositionLocal,
        proof =
          Some(
            PlayerFacingExactSliceProof.TargetFocusedCoordination(
              targetSquare = "c6",
              supportFromSquares = List("c1", "b3"),
              targetPieces = List("target_knight")
            )
          )
      )
    val oneSupport =
      valid.copy(
        proofPathWitness =
          valid.proofPathWitness.copy(
            exactSliceProof =
              Some(
                PlayerFacingExactSliceProof.TargetFocusedCoordination(
                  targetSquare = "c6",
                  supportFromSquares = List("c1"),
                  targetPieces = List("target_knight")
                )
              )
          )
      )
    val noTargetMarker =
      valid.copy(
        proofPathWitness =
          valid.proofPathWitness.copy(
            exactSliceProof =
              Some(
                PlayerFacingExactSliceProof.TargetFocusedCoordination(
                  targetSquare = "c6",
                  supportFromSquares = List("c1", "b3"),
                  targetPieces = Nil
                )
              )
          )
      )
    val wrongTargetMarker =
      valid.copy(
        proofPathWitness =
          valid.proofPathWitness.copy(
            exactSliceProof =
              Some(
                PlayerFacingExactSliceProof.TargetFocusedCoordination(
                  targetSquare = "c6",
                  supportFromSquares = List("c1", "b3"),
                  targetPieces = List("knight")
                )
              )
          )
      )
    val wrongTargetTerms =
      valid.copy(
        proofPathWitness =
          valid.proofPathWitness.copy(
            ownerSeedTerms = List("c6", "coordinated_target:e5"),
            continuationTerms = List("target_focused_coordination_probe", "coordinated_target:e5"),
            structureTransitionTerms = List("target_focused_coordination_probe", "coordinated_target:e5")
          )
      )

    assertEquals(ProofContractRules.failureCodes(valid), Nil)
    assert(ProofContractRules.failureCodes(oneSupport).contains("witness:exact_slice_missing"))
    assert(ProofContractRules.failureCodes(noTargetMarker).contains("witness:exact_slice_missing"))
    assert(ProofContractRules.failureCodes(wrongTargetMarker).contains("witness:exact_slice_missing"))
    assert(ProofContractRules.failureCodes(wrongTargetTerms).contains("witness:exact_slice_missing"))
  }

  test("non-position exact-slice contracts require their typed proof cases") {
    val localFileBase =
      exactSliceContractPacket(
        proofSource = ProofSourceId.LocalFileEntryBind.wireKey,
        proofFamily = ProofFamilyId.HalfOpenFilePressure.wireKey,
        scope = PlayerFacingPacketScope.MoveLocal,
        proof = Some(PlayerFacingExactSliceProof.LocalFileEntryBind("c-file", "c6"))
      )
    val localFile =
      localFileBase.copy(
        anchorTerms = List("c-file", "c6"),
        proofPathWitness =
          localFileBase.proofPathWitness.copy(
            ownerSeedTerms = List("c-file", "c6"),
            continuationTerms = List("local_file_entry_bind", "c-file", "c6"),
            structureTransitionTerms = List("file-entry:c-file:c6")
          )
      )
    val offFileLocalFile =
      localFile.copy(
        anchorTerms = List("c-file", "d6"),
        proofPathWitness =
          localFile.proofPathWitness.copy(
            ownerSeedTerms = List("c-file", "d6"),
            continuationTerms = List("local_file_entry_bind", "c-file", "d6"),
            structureTransitionTerms = List("file-entry:c-file:d6"),
            exactSliceProof = Some(PlayerFacingExactSliceProof.LocalFileEntryBind("c-file", "d6"))
          )
      )
    val splitOnlyLocalFile =
      localFile.copy(
        proofPathWitness =
          localFile.proofPathWitness.copy(
            structureTransitionTerms = Nil
          )
      )
    val neutralizeBreak =
      exactSliceContractPacket(
        proofSource = ProofSourceId.CounterplayAxisSuppression.wireKey,
        proofFamily = ProofFamilyId.NeutralizeKeyBreak.wireKey,
        scope = PlayerFacingPacketScope.MoveLocal,
        proof = Some(PlayerFacingExactSliceProof.CounterplayAxisSuppression("...c5"))
      )
    val centralBreak =
      exactSliceContractPacket(
        proofSource = PlanTaxonomy.PlanKind.CentralBreakTiming.id,
        proofFamily = PlanTaxonomy.PlanKind.CentralBreakTiming.id,
        scope = PlayerFacingPacketScope.MoveLocal,
        proof = Some(PlayerFacingExactSliceProof.CentralBreakTiming("e2e4", "e4", "e2-e4"))
      )
    val prophylactic =
      exactSliceContractPacket(
        proofSource = ProofSourceId.ProphylacticMove.wireKey,
        proofFamily = ProofFamilyId.CounterplayRestraint.wireKey,
        scope = PlayerFacingPacketScope.MoveLocal,
        proof = Some(PlayerFacingExactSliceProof.ProphylacticRestraint("denied_resource:break"))
      )
    val queenTrade =
      exactSliceContractPacket(
        proofSource = PlayerFacingTruthModePolicy.QueenTradeShieldProofSource,
        proofFamily = PlanTaxonomy.PlanKind.QueenTradeShield.id,
        scope = PlayerFacingPacketScope.MoveLocal,
        proof = Some(PlayerFacingExactSliceProof.QueenTradeShield(List("d4c6", "d7c6", "d3d8", "e8d8")))
      )
    val outpost =
      exactSliceContractPacket(
        proofSource = PlayerFacingTruthModePolicy.OutpostEntrenchmentProofSource,
        proofFamily = PlayerFacingTruthModePolicy.OutpostEntrenchmentProofFamily,
        scope = PlayerFacingPacketScope.MoveLocal,
        proof = Some(PlayerFacingExactSliceProof.OutpostOccupation("knight", "e5"))
      )
    val genericProphylactic =
      prophylactic.copy(
        proofPathWitness =
          prophylactic.proofPathWitness.copy(
            exactSliceProof = Some(PlayerFacingExactSliceProof.ProphylacticRestraint("counterplay window"))
          )
      )
    val mismatchedNeutralizeTerms =
      neutralizeBreak.copy(
        proofPathWitness =
          neutralizeBreak.proofPathWitness.copy(
            ownerSeedTerms = List("...e5"),
            structureTransitionTerms = List("...e5")
          )
      )
    val mismatchedProphylacticTerms =
      prophylactic.copy(
        proofPathWitness =
          prophylactic.proofPathWitness.copy(
            ownerSeedTerms = List("denied_resource:route"),
            structureTransitionTerms = List("denied_resource:route")
          )
      )
    val mismatchedCentralTerms =
      centralBreak.copy(
        proofPathWitness =
          centralBreak.proofPathWitness.copy(
            ownerSeedTerms = List("e2-e4", "e4"),
            structureTransitionTerms = List("break_token:e2-e4", "break_move:e2e5", "central_break:e5")
          )
      )
    val mismatchedQueenTradeTerms =
      queenTrade.copy(
        proofPathWitness =
          queenTrade.proofPathWitness.copy(
            ownerSeedTerms = List("queen_trade_shield", "d4c6", "d7c6"),
            structureTransitionTerms = List("queenless_branch")
          )
      )
    val mismatchedOutpostTerms =
      outpost.copy(
        proofPathWitness =
          outpost.proofPathWitness.copy(
            ownerSeedTerms = List("e5", "outpost:e5", "piece:knight"),
            structureTransitionTerms = List("outpost_occupation")
          )
      )
    val wrongOutpostRole =
      outpost.copy(
        proofPathWitness =
          outpost.proofPathWitness.copy(
            exactSliceProof = Some(PlayerFacingExactSliceProof.OutpostOccupation("bishop", "e5"))
          )
      )

    List(localFile, neutralizeBreak, centralBreak, prophylactic, queenTrade, outpost).foreach { packet =>
      assertEquals(ProofContractRules.failureCodes(packet), Nil, clues(packet))
    }
    assert(ProofContractRules.failureCodes(genericProphylactic).contains("witness:exact_slice_missing"))
    assert(ProofContractRules.failureCodes(localFileBase).contains("witness:exact_slice_missing"))
    assert(ProofContractRules.failureCodes(offFileLocalFile).contains("witness:exact_slice_missing"))
    assert(ProofContractRules.failureCodes(splitOnlyLocalFile).contains("witness:exact_slice_missing"))
    assert(ProofContractRules.failureCodes(mismatchedNeutralizeTerms).contains("witness:exact_slice_missing"))
    assert(ProofContractRules.failureCodes(mismatchedProphylacticTerms).contains("witness:exact_slice_missing"))
    assert(ProofContractRules.failureCodes(mismatchedCentralTerms).contains("witness:exact_slice_missing"))
    assert(ProofContractRules.failureCodes(mismatchedQueenTradeTerms).contains("witness:exact_slice_missing"))
    assert(ProofContractRules.failureCodes(mismatchedOutpostTerms).contains("witness:exact_slice_missing"))
    assert(ProofContractRules.failureCodes(wrongOutpostRole).contains("witness:exact_slice_missing"))
    assert(
      ProofContractRules
        .failureCodes(localFile.copy(proofPathWitness = localFile.proofPathWitness.copy(exactSliceProof = None)))
        .contains("witness:exact_slice_missing")
    )
    val wrongCentralDestination =
      centralBreak.copy(
        proofPathWitness =
          centralBreak.proofPathWitness.copy(
            exactSliceProof = Some(PlayerFacingExactSliceProof.CentralBreakTiming("e2e4", "e5", "e2-e4"))
          )
      )
    val malformedCentralMove =
      centralBreak.copy(
        proofPathWitness =
          centralBreak.proofPathWitness.copy(
            exactSliceProof = Some(PlayerFacingExactSliceProof.CentralBreakTiming("O-O", "g1", "e1-g1"))
          )
      )
    assert(ProofContractRules.failureCodes(wrongCentralDestination).contains("witness:exact_slice_missing"))
    assert(ProofContractRules.failureCodes(malformedCentralMove).contains("witness:exact_slice_missing"))
  }

  test("DefenderTrade supported-local release requires structure, branch, and stability witnesses") {
    val contract =
      ProofContractRules
        .contractForProofFamily(PlanTaxonomy.PlanKind.DefenderTrade.id)
        .getOrElse(fail("missing DefenderTrade contract"))

    assertEquals(contract.status, ProofContractStatus.Releasable)
    assert(!contract.certifiedEligible, clues(contract))
    assert(contract.supportedLocalEligible, clues(contract))
    assert(contract.acceptedSources.contains(PlayerFacingTruthModePolicy.DefenderTradeProofSource), clues(contract))
    assert(contract.acceptedSources.contains("exchange_forcing_delta"), clues(contract))
    assert(contract.requiredWitnesses.contains(ProofWitness.StructureTransition), clues(contract))
    assert(contract.requiredWitnesses.contains(ProofWitness.BranchProof), clues(contract))
    assert(contract.requiredWitnesses.contains(ProofWitness.StablePersistence), clues(contract))
    assert(!contract.id.toLowerCase.contains("source-"), clues(contract))
    assert(!contract.acceptedSources.exists(_.toLowerCase.contains("source-")), clues(contract))

    val packet =
      exchangeContractPacket(
        proofSource = PlayerFacingTruthModePolicy.DefenderTradeProofSource,
        proofFamily = PlanTaxonomy.PlanKind.DefenderTrade.id,
        structureTransitionTerms =
          List("defender_trade_branch", "defender:c5", "exchange_square:d4", "defended_target:e5")
      )
    assertEquals(ProofContractRules.failureCodes(packet), Nil)
    assert(
      ProofContractRules.failureCodes(packet.copy(sameBranchState = PlayerFacingSameBranchState.Missing))
        .contains("witness:branch_not_proven")
    )
    assert(
      ProofContractRules.failureCodes(packet.copy(persistence = PlayerFacingClaimPersistence.BestDefenseOnly))
        .contains("witness:persistence_not_stable")
    )
  }

  test("BadPieceLiquidation supported-local release requires structure, branch, and stability witnesses") {
    val contract =
      ProofContractRules
        .contractForProofFamily(PlanTaxonomy.PlanKind.BadPieceLiquidation.id)
        .getOrElse(fail("missing BadPieceLiquidation contract"))

    assertEquals(contract.status, ProofContractStatus.Releasable)
    assert(!contract.certifiedEligible, clues(contract))
    assert(contract.supportedLocalEligible, clues(contract))
    assert(contract.acceptedSources.contains(PlayerFacingTruthModePolicy.BadPieceLiquidationProofSource), clues(contract))
    assert(contract.requiredWitnesses.contains(ProofWitness.StructureTransition), clues(contract))
    assert(contract.requiredWitnesses.contains(ProofWitness.BranchProof), clues(contract))
    assert(contract.requiredWitnesses.contains(ProofWitness.StablePersistence), clues(contract))

    val packet =
      exchangeContractPacket(
        proofSource = PlayerFacingTruthModePolicy.BadPieceLiquidationProofSource,
        proofFamily = PlanTaxonomy.PlanKind.BadPieceLiquidation.id,
        structureTransitionTerms = List("bad_piece_liquidation_branch", "bad_piece:c8", "exchange_square:e6")
      )
    assertEquals(ProofContractRules.failureCodes(packet), Nil)
    assert(
      ProofContractRules.failureCodes(packet.copy(sameBranchState = PlayerFacingSameBranchState.Ambiguous))
        .contains("witness:branch_not_proven")
    )
    assert(
      ProofContractRules.failureCodes(packet.copy(persistence = PlayerFacingClaimPersistence.BestDefenseOnly))
        .contains("witness:persistence_not_stable")
    )
  }

  private def exchangeContractPacket(
      proofSource: String,
      proofFamily: String,
      structureTransitionTerms: List[String]
  ): PlayerFacingClaimPacket =
    PlayerFacingClaimPacket(
      proofSource = proofSource,
      proofFamily = proofFamily,
      scope = PlayerFacingPacketScope.MoveLocal,
      fallbackMode = PlayerFacingClaimFallbackMode.WeakMain,
      bestDefenseBranchKey = Some("d4c6|d7c6"),
      sameBranchState = PlayerFacingSameBranchState.Proven,
      persistence = PlayerFacingClaimPersistence.Stable,
      proofPathWitness =
        PlayerFacingProofPathWitness(
          ownerSeedTerms = List(proofFamily, "local_branch"),
          continuationTerms = List("d4c6", "d7c6"),
          structureTransitionTerms = structureTransitionTerms
        )
    )

  private def exactSliceContractPacket(
      proofSource: String,
      proofFamily: String,
      scope: PlayerFacingPacketScope,
      proof: Option[PlayerFacingExactSliceProof]
  ): PlayerFacingClaimPacket =
    val targetSquare =
      proof.flatMap(PlayerFacingExactSliceProofFacts.targetSquare).getOrElse("c6")
    val exactProofTerms =
      proof match
        case Some(PlayerFacingExactSliceProof.TargetFocusedCoordination(_, _, _)) =>
          List(targetSquare, s"coordinated_target:$targetSquare")
        case Some(PlayerFacingExactSliceProof.CounterplayAxisSuppression(breakToken)) =>
          List(breakToken)
        case Some(PlayerFacingExactSliceProof.ProphylacticRestraint(resourceToken)) =>
          List(resourceToken)
        case Some(PlayerFacingExactSliceProof.CentralBreakTiming(breakMove, breakSquare, breakToken)) =>
          List(
            breakToken,
            breakSquare,
            s"break_token:$breakToken",
            s"break_move:$breakMove",
            s"central_break:$breakSquare"
          )
        case Some(PlayerFacingExactSliceProof.QueenTradeShield(lineMoves)) =>
          (List("queen_trade_shield", "queenless_branch", "queen_trade") ++ lineMoves.map(_.trim.toLowerCase)).distinct
        case Some(PlayerFacingExactSliceProof.DefenderTrade(defenderSquare, exchangeSquare, targetSquare)) =>
          List(
            targetSquare,
            "defender_trade_branch",
            s"defender:$defenderSquare",
            s"exchange_square:$exchangeSquare",
            s"defended_target:$targetSquare"
          )
        case Some(PlayerFacingExactSliceProof.BadPieceLiquidation(badPieceSquare, exchangeSquare)) =>
          List(
            exchangeSquare,
            "bad_piece_liquidation_branch",
            s"bad_piece:$badPieceSquare",
            s"exchange_square:$exchangeSquare"
          )
        case Some(PlayerFacingExactSliceProof.OutpostOccupation(pieceRole, square)) =>
          val role = pieceRole.trim.toLowerCase
          val outpost = square.trim.toLowerCase
          List(outpost, s"outpost:$outpost", s"piece:$role", s"outpost_occupation:$role:$outpost")
        case _ =>
          List(targetSquare, s"fixed_target:$targetSquare")
    PlayerFacingClaimPacket(
      proofSource = proofSource,
      proofFamily = proofFamily,
      scope = scope,
      triggerKind = "exact_slice_test",
      anchorTerms = List(targetSquare),
      bestDefenseMove = Some("b7b5"),
      bestDefenseBranchKey = Some("h4f2|b7b5"),
      sameBranchState = PlayerFacingSameBranchState.Proven,
      persistence = PlayerFacingClaimPersistence.Stable,
      fallbackMode = PlayerFacingClaimFallbackMode.WeakMain,
      proofPathWitness =
        PlayerFacingProofPathWitness(
          ownerSeedTerms = exactProofTerms,
          continuationTerms = List("h4f2|b7b5"),
          structureTransitionTerms = List("exact_slice_test"),
          exactSliceProof = proof
        )
    )

  private def supportedIqpPacket(): PlayerFacingClaimPacket =
    PlayerFacingClaimPacket(
      proofSource = PlayerFacingTruthModePolicy.IQPInducementProbeProofSource,
      proofFamily = PlanTaxonomy.PlanKind.IQPInducement.id,
      scope = PlayerFacingPacketScope.PositionLocal,
      fallbackMode = PlayerFacingClaimFallbackMode.WeakMain,
      proofPathWitness =
        PlayerFacingProofPathWitness(
          ownerSeedTerms = List("iqp"),
          continuationTerms = List("d5"),
          structureTransitionTerms = List("isolated_d_pawn")
        )
    )
