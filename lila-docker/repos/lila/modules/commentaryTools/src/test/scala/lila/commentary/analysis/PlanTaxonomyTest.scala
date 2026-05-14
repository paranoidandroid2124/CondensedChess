package lila.commentary.analysis

import munit.FunSuite

class PlanTaxonomyTest extends FunSuite:

  import PlanTaxonomy.PlanKind
  import PlanTaxonomy.ThemeResolver

  private def seed(id: String) =
    LatentSeedLibrary.byId(id).getOrElse(fail(s"missing seed: $id"))

  test("subplanFromSeed prefers explicit seed taxonomy over generic plan defaults") {
    assertEquals(ThemeResolver.subplanFromSeed(seed("Trade_Queens_Defensive")), Some(PlanKind.QueenTradeShield))
    assertEquals(ThemeResolver.subplanFromSeed(seed("Battery_Formation")), Some(PlanKind.BatteryPressure))
    assertEquals(ThemeResolver.subplanFromSeed(seed("CreatePassedPawn")), Some(PlanKind.PassedPawnManufacture))
    assertEquals(ThemeResolver.subplanFromSeed(seed("OpenFile_Doubling")), Some(PlanKind.OpenFilePressure))
  }

  test("subplanFromEvidenceSource resolves structural-state aliases") {
    assertEquals(
      ThemeResolver.subplanFromEvidenceSource("structural_state:entrenched_piece"),
      Some(PlanKind.OutpostEntrenchment)
    )
    assertEquals(
      ThemeResolver.subplanFromEvidenceSource("structural_state:generic_center_plan"),
      Some(PlanKind.CentralBreakTiming)
    )
  }
