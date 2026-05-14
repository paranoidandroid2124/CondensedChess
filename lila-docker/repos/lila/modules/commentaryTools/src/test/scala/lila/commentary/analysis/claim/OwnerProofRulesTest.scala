package lila.commentary.analysis.claim

import lila.commentary.tools.claim.*

import lila.commentary.analysis.*
import munit.FunSuite

class OwnerProofRulesTest extends FunSuite:

  test("every strategic taxonomy family has a owner proof contract or explicit deferred status") {
    val missingThemes =
      ThemeTaxonomy.ThemeL1.ranked.filter(theme =>
        OwnerProofRules.contractForFamily(theme.id).isEmpty
      )
    val missingSubplans =
      ThemeTaxonomy.SubplanId.values.toList.filter(subplan =>
        OwnerProofRules.contractForFamily(subplan.id).isEmpty
      )

    assertEquals(missingThemes, Nil)
    assertEquals(missingSubplans, Nil)
    assert(
      OwnerProofRules.contracts.exists(_.status == OwnerProofStatus.Deferred),
      clues(OwnerProofRules.contracts.map(contract => contract.id -> contract.status))
    )
  }

  test("releasable contracts declare concrete witness requirements and legal scopes") {
    val releasable =
      OwnerProofRules.contracts.filter(_.authorityEligible)

    assert(releasable.nonEmpty)
    assert(
      releasable.forall(contract =>
        contract.allowedScopes.exists(scope =>
          scope == PlayerFacingPacketScope.MoveLocal || scope == PlayerFacingPacketScope.PositionLocal
        ) &&
          contract.requiredWitnesses.contains(OwnerProofWitness.OwnerSeed) &&
          contract.requiredWitnesses.contains(OwnerProofWitness.NoTacticalVeto) &&
          contract.defaultFailureTaxonomy.nonEmpty
      ),
      clues(releasable)
    )
  }

  test("exact slice contracts are runtime predicates, not source-witness-id gates") {
    val exactContracts =
      OwnerProofRules.contracts.filter(_.requiredWitnesses.contains(OwnerProofWitness.ExactSlice))

    assert(exactContracts.nonEmpty)
    assert(
      exactContracts.forall(contract =>
        !contract.id.toLowerCase.contains("source-") &&
          !contract.acceptedSources.exists(_.toLowerCase.contains("source-"))
      ),
      clues(exactContracts)
    )
  }

  test("immediate tactical-gain taxonomy cannot become strategic authority owner") {
    val tacticalContracts =
      OwnerProofRules.contracts.filter(_.theme.contains(ThemeTaxonomy.ThemeL1.ImmediateTacticalGain))

    assert(tacticalContracts.nonEmpty)
    assert(
      tacticalContracts.forall(contract =>
        !contract.authorityEligible &&
          contract.status == OwnerProofStatus.BackendOnly &&
          contract.defaultFailureTaxonomy == "tactical_truth_first"
      ),
      clues(tacticalContracts)
    )
  }

  test("known admitted B/C packets resolve through owner proof contracts") {
    val carlsbad =
      OwnerProofRules
        .contractForFamily(ThemeTaxonomy.SubplanId.BackwardPawnTargeting.id)
        .getOrElse(fail("missing Carlsbad target contract"))
    val queenTrade =
      OwnerProofRules
        .contractForFamily(ThemeTaxonomy.SubplanId.QueenTradeShield.id)
        .getOrElse(fail("missing queen trade shield contract"))

    assert(carlsbad.certifiedEligible, clues(carlsbad))
    assert(carlsbad.acceptedSources.contains(PlayerFacingTruthModePolicy.CarlsbadFixedTargetProbeOwnerSource), clues(carlsbad))
    assert(queenTrade.supportedLocalEligible, clues(queenTrade))
    assert(!queenTrade.certifiedEligible, clues(queenTrade))
    assert(queenTrade.acceptedSources.contains(PlayerFacingTruthModePolicy.QueenTradeShieldOwnerSource), clues(queenTrade))
  }

  test("IQP inducement is supported-local releasable only through exact probe proof") {
    val contract =
      OwnerProofRules
        .contractForFamily(ThemeTaxonomy.SubplanId.IQPInducement.id)
        .getOrElse(fail("missing IQP inducement contract"))

    assertEquals(contract.status, OwnerProofStatus.Releasable)
    assert(!contract.certifiedEligible, clues(contract))
    assert(contract.supportedLocalEligible, clues(contract))
    assert(contract.acceptedSources.contains(PlayerFacingTruthModePolicy.IQPInducementProbeOwnerSource), clues(contract))
    assert(!contract.id.toLowerCase.contains("source-"), clues(contract))
    assert(!contract.acceptedSources.exists(_.toLowerCase.contains("source-")), clues(contract))
  }

  test("DefenderTrade is supported-local releasable only through exact defender proof") {
    val contract =
      OwnerProofRules
        .contractForFamily(ThemeTaxonomy.SubplanId.DefenderTrade.id)
        .getOrElse(fail("missing DefenderTrade contract"))

    assertEquals(contract.status, OwnerProofStatus.Releasable)
    assert(!contract.certifiedEligible, clues(contract))
    assert(contract.supportedLocalEligible, clues(contract))
    assert(contract.acceptedSources.contains(PlayerFacingTruthModePolicy.DefenderTradeOwnerSource), clues(contract))
    assert(contract.acceptedSources.contains("exchange_forcing_delta"), clues(contract))
    assert(contract.requiredWitnesses.contains(OwnerProofWitness.StructureTransition), clues(contract))
    assert(!contract.id.toLowerCase.contains("source-"), clues(contract))
    assert(!contract.acceptedSources.exists(_.toLowerCase.contains("source-")), clues(contract))
  }
