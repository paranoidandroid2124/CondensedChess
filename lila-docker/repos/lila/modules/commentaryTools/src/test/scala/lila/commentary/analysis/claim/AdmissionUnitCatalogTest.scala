package lila.commentary.analysis.claim

import lila.commentary.tools.claim.*

import lila.commentary.analysis.*
import munit.FunSuite

class AdmissionUnitCatalogTest extends FunSuite:

  test("admission unit catalog encodes one-plan-kind one-proof-source work units") {
    val units = AdmissionUnitCatalog.admissionUnits

    assertEquals(
      units.map(unit => unit.planKindId -> unit.proofSource),
      List(
        PlanTaxonomy.PlanKind.BreakPrevention.id -> "counterplay_axis_suppression",
        PlanTaxonomy.PlanKind.ProphylaxisRestraint.id -> "prophylactic_move",
        PlanTaxonomy.PlanKind.OpenFilePressure.id -> "local_file_entry_bind",
        PlanTaxonomy.PlanKind.BadPieceLiquidation.id -> "bad_piece_liquidation",
        PlanTaxonomy.PlanKind.CentralBreakTiming.id -> PlanTaxonomy.PlanKind.CentralBreakTiming.id
      )
    )
    assert(units.forall(_.sourceCandidateTarget == AdmissionUnitCatalog.SourceCandidateTarget(2, 5)), clues(units))
    assert(units.forall(_.maxAuthorityRows == 2), clues(units))
    assert(units.forall(_.controlledPositive.count == 1), clues(units))
    assert(units.forall(_.negativeControls.map(_.kind) == AdmissionUnitCatalog.requiredNegativeKinds), clues(units))
  }

  test("queued admission units resolve through runtime contracts without source-witness-id gates") {
    val byFamily = AdmissionUnitCatalog.admissionUnits.map(unit => unit.planKindId -> unit).toMap

    val releasableUnits =
      List(
        byFamily(PlanTaxonomy.PlanKind.BreakPrevention.id),
        byFamily(PlanTaxonomy.PlanKind.ProphylaxisRestraint.id),
        byFamily(PlanTaxonomy.PlanKind.OpenFilePressure.id),
        byFamily(PlanTaxonomy.PlanKind.BadPieceLiquidation.id)
      )

    releasableUnits.foreach { unit =>
      val contract =
        ProofContractRules
          .contractForProofFamily(unit.proofFamily)
          .getOrElse(fail(s"missing proof contract for ${unit.proofFamily}"))

      assertEquals(contract.status, ProofContractStatus.Releasable, clues(unit, contract))
      assert(contract.supportedLocalEligible, clues(unit, contract))
      assert(contract.acceptedSources.contains(unit.proofSource), clues(unit, contract))
      assert(contract.allowedScopes.contains(unit.acceptedScope), clues(unit, contract))
      assert(!contract.id.toLowerCase.contains("source-"), clues(unit, contract))
      assert(!contract.acceptedSources.exists(_.toLowerCase.contains("source-")), clues(unit, contract))
    }

    val centralBreakTiming = byFamily(PlanTaxonomy.PlanKind.CentralBreakTiming.id)
    val centralBreakTimingContract =
      ProofContractRules
        .contractForProofFamily(centralBreakTiming.proofFamily)
        .getOrElse(fail(s"missing proof contract for ${centralBreakTiming.proofFamily}"))

    assertEquals(centralBreakTiming.acceptedScope, PlayerFacingPacketScope.MoveLocal)
    assertEquals(centralBreakTiming.defaultAuthorityTier, "SupportedLocal")
    assertEquals(centralBreakTimingContract.status, ProofContractStatus.Releasable)
    assert(centralBreakTimingContract.supportedLocalEligible, clues(centralBreakTimingContract))
    assert(centralBreakTimingContract.acceptedSources.contains(centralBreakTiming.proofSource), clues(centralBreakTimingContract))
  }

  test("admission unit catalog report fixes the execution contract for future passes") {
    val markdown = AdmissionUnitCatalog.markdown

    assert(markdown.contains("1 plan kind, 1 proof source"), clues(markdown))
    assert(markdown.contains("0-2 authority rows"), clues(markdown))
    assert(markdown.contains("CertifiedOwner requires PV1 source-move agreement"), clues(markdown))
    assert(markdown.contains("SupportedLocal may use near-top MultiPV"), clues(markdown))
    assert(markdown.contains("counterplay_axis_suppression"), clues(markdown))
    assert(markdown.contains("prophylactic_move"), clues(markdown))
    assert(markdown.contains("local_file_entry_bind"), clues(markdown))
    assert(markdown.contains("bad_piece_liquidation"), clues(markdown))
    assert(markdown.contains("central_break_timing"), clues(markdown))
  }
