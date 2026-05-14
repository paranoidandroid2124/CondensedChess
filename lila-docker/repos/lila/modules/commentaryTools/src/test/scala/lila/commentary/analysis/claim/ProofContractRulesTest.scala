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
