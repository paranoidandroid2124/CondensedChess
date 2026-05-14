package lila.commentary.analysis.claim

import lila.commentary.tools.claim.*

import lila.commentary.analysis.*
import munit.FunSuite

class OwnerFamilyProofCatalogTest extends FunSuite:

  test("owner family proof queue encodes one-family one-source work units") {
    val units = OwnerFamilyProofCatalog.ownerFamilyProofCatalog

    assertEquals(
      units.map(unit => unit.familyId -> unit.ownerSource),
      List(
        ThemeTaxonomy.SubplanId.BreakPrevention.id -> "counterplay_axis_suppression",
        ThemeTaxonomy.SubplanId.ProphylaxisRestraint.id -> "prophylactic_move",
        ThemeTaxonomy.SubplanId.OpenFilePressure.id -> "local_file_entry_bind",
        ThemeTaxonomy.SubplanId.BadPieceLiquidation.id -> "bad_piece_liquidation",
        ThemeTaxonomy.SubplanId.CentralBreakTiming.id -> ThemeTaxonomy.SubplanId.CentralBreakTiming.id
      )
    )
    assert(units.forall(_.sourceCandidateTarget == OwnerFamilyProofCatalog.SourceCandidateTarget(2, 5)), clues(units))
    assert(units.forall(_.maxAuthorityRows == 2), clues(units))
    assert(units.forall(_.controlledPositive.count == 1), clues(units))
    assert(units.forall(_.negativeControls.map(_.kind) == OwnerFamilyProofCatalog.requiredNegativeKinds), clues(units))
  }

  test("queued family units resolve through runtime contracts without source-witness-id gates") {
    val byFamily = OwnerFamilyProofCatalog.ownerFamilyProofCatalog.map(unit => unit.familyId -> unit).toMap

    val releasableUnits =
      List(
        byFamily(ThemeTaxonomy.SubplanId.BreakPrevention.id),
        byFamily(ThemeTaxonomy.SubplanId.ProphylaxisRestraint.id),
        byFamily(ThemeTaxonomy.SubplanId.OpenFilePressure.id),
        byFamily(ThemeTaxonomy.SubplanId.BadPieceLiquidation.id)
      )

    releasableUnits.foreach { unit =>
      val contract =
        OwnerProofRules
          .contractForFamily(unit.ownerFamily)
          .getOrElse(fail(s"missing owner contract for ${unit.ownerFamily}"))

      assertEquals(contract.status, OwnerProofStatus.Releasable, clues(unit, contract))
      assert(contract.supportedLocalEligible, clues(unit, contract))
      assert(contract.acceptedSources.contains(unit.ownerSource), clues(unit, contract))
      assert(contract.allowedScopes.contains(unit.acceptedScope), clues(unit, contract))
      assert(!contract.id.toLowerCase.contains("source-"), clues(unit, contract))
      assert(!contract.acceptedSources.exists(_.toLowerCase.contains("source-")), clues(unit, contract))
    }

    val deferredPawnBreak = byFamily(ThemeTaxonomy.SubplanId.CentralBreakTiming.id)
    val deferredContract =
      OwnerProofRules
        .contractForFamily(deferredPawnBreak.ownerFamily)
        .getOrElse(fail(s"missing owner contract for ${deferredPawnBreak.ownerFamily}"))

    assertEquals(deferredContract.status, OwnerProofStatus.Deferred)
    assert(!deferredContract.authorityEligible, clues(deferredContract))
  }

  test("owner family proof report fixes the execution contract for future passes") {
    val markdown = OwnerFamilyProofCatalog.markdown

    assert(markdown.contains("1 family, 1 owner source"), clues(markdown))
    assert(markdown.contains("0-2 authority rows"), clues(markdown))
    assert(markdown.contains("CertifiedOwner requires PV1 source-move agreement"), clues(markdown))
    assert(markdown.contains("SupportedLocal may use near-top MultiPV"), clues(markdown))
    assert(markdown.contains("counterplay_axis_suppression"), clues(markdown))
    assert(markdown.contains("prophylactic_move"), clues(markdown))
    assert(markdown.contains("local_file_entry_bind"), clues(markdown))
    assert(markdown.contains("bad_piece_liquidation"), clues(markdown))
  }
