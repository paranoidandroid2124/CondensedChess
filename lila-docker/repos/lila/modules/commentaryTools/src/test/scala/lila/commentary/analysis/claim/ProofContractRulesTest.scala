package lila.commentary.analysis.claim

import lila.commentary.analysis.*
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
            PlayerFacingExactSliceProof(
              proofSource = PlayerFacingTruthModePolicy.ColorComplexSqueezeProbeProofSource,
              proofFamily = "color_complex_squeeze",
              kind = PlayerFacingTruthModePolicy.ColorComplexSqueezeProbeProofSource,
              target = "e5",
              terms = exactProbeTerms
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
                PlayerFacingExactSliceProof(
                  proofSource = PlayerFacingTruthModePolicy.ExactTargetFixationProofSource,
                  proofFamily = PlanTaxonomy.PlanKind.StaticWeaknessFixation.id,
                  kind = PlayerFacingTruthModePolicy.ExactTargetFixationProofSource,
                  target = "d6",
                  terms = List("d6", "fixed_target:d6", "backward_pawn_target", "weak_complex:d6")
                )
              )
          )
      )

    assert(!ProofContractRules.certifiedOwnerAdmissible(packet), clues(ProofContractRules.failureCodes(packet)))
    assert(ProofContractRules.failureCodes(packet).contains("witness:exact_slice_missing"))
    assertEquals(ProofContractRules.failureCodes(typed), Nil)
    assert(ProofContractRules.certifiedOwnerAdmissible(typed), clues(ProofContractRules.failureCodes(typed)))
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
