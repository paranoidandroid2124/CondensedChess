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
        "color_complex:light",
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
              squareColor = "light",
              minorPieceRole = "knight",
              minorPieceSquare = "c4"
            )
          )
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

    assert(!ProofContractRules.certifiedOwnerAdmissible(packet), clues(ProofContractRules.failureCodes(packet)))
    assert(ProofContractRules.failureCodes(packet).contains("witness:exact_slice_missing"))
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

    assert(PlayerFacingExactSliceProofFacts.matchesPacket(centralPacket, centralProof), clues(centralPacket))
    assert(!PlayerFacingExactSliceProofFacts.matchesPacket(wrongFamily, centralProof), clues(wrongFamily))
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

    assertEquals(ProofContractRules.failureCodes(base), Nil)
    assert(ProofContractRules.certifiedOwnerAdmissible(base), clues(base))
    assert(ProofContractRules.failureCodes(missingProof).contains("witness:exact_slice_missing"))
    assert(ProofContractRules.failureCodes(wrongProof).contains("witness:exact_slice_missing"))
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

    assertEquals(ProofContractRules.failureCodes(valid), Nil)
    assert(ProofContractRules.failureCodes(oneSupport).contains("witness:exact_slice_missing"))
    assert(ProofContractRules.failureCodes(noTargetMarker).contains("witness:exact_slice_missing"))
    assert(ProofContractRules.failureCodes(wrongTargetMarker).contains("witness:exact_slice_missing"))
  }

  test("non-position exact-slice contracts require their typed proof cases") {
    val localFile =
      exactSliceContractPacket(
        proofSource = ProofSourceId.LocalFileEntryBind.wireKey,
        proofFamily = ProofFamilyId.HalfOpenFilePressure.wireKey,
        scope = PlayerFacingPacketScope.MoveLocal,
        proof = Some(PlayerFacingExactSliceProof.LocalFileEntryBind("c-file", "c6"))
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
    val genericProphylactic =
      prophylactic.copy(
        proofPathWitness =
          prophylactic.proofPathWitness.copy(
            exactSliceProof = Some(PlayerFacingExactSliceProof.ProphylacticRestraint("counterplay window"))
          )
      )

    List(localFile, neutralizeBreak, centralBreak, prophylactic).foreach { packet =>
      assertEquals(ProofContractRules.failureCodes(packet), Nil, clues(packet))
    }
    assert(ProofContractRules.failureCodes(genericProphylactic).contains("witness:exact_slice_missing"))
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

  test("DefenderTrade is supported-local releasable only through exact defender proof") {
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
    assert(!contract.id.toLowerCase.contains("source-"), clues(contract))
    assert(!contract.acceptedSources.exists(_.toLowerCase.contains("source-")), clues(contract))
  }

  test("BadPieceLiquidation supported-local release requires a structure transition witness") {
    val contract =
      ProofContractRules
        .contractForProofFamily(PlanTaxonomy.PlanKind.BadPieceLiquidation.id)
        .getOrElse(fail("missing BadPieceLiquidation contract"))

    assertEquals(contract.status, ProofContractStatus.Releasable)
    assert(!contract.certifiedEligible, clues(contract))
    assert(contract.supportedLocalEligible, clues(contract))
    assert(contract.acceptedSources.contains(PlayerFacingTruthModePolicy.BadPieceLiquidationProofSource), clues(contract))
    assert(contract.requiredWitnesses.contains(ProofWitness.StructureTransition), clues(contract))
  }

  private def exactSliceContractPacket(
      proofSource: String,
      proofFamily: String,
      scope: PlayerFacingPacketScope,
      proof: Option[PlayerFacingExactSliceProof]
  ): PlayerFacingClaimPacket =
    PlayerFacingClaimPacket(
      proofSource = proofSource,
      proofFamily = proofFamily,
      scope = scope,
      triggerKind = "exact_slice_test",
      anchorTerms = List("c6"),
      bestDefenseMove = Some("b7b5"),
      bestDefenseBranchKey = Some("h4f2|b7b5"),
      sameBranchState = PlayerFacingSameBranchState.Proven,
      persistence = PlayerFacingClaimPersistence.Stable,
      fallbackMode = PlayerFacingClaimFallbackMode.WeakMain,
      proofPathWitness =
        PlayerFacingProofPathWitness(
          ownerSeedTerms = List("c6", "fixed_target:c6"),
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
