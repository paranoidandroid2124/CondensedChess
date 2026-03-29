package lila.llm.analysis

import munit.FunSuite

class ThemeTaxonomyTest extends FunSuite:

  import ThemeTaxonomy.SubplanId
  import ThemeTaxonomy.ThemeResolver

  private def seed(id: String) =
    LatentSeedLibrary.byId(id).getOrElse(fail(s"missing seed: $id"))

  test("subplanFromSeed prefers explicit seed taxonomy over generic plan defaults") {
    assertEquals(ThemeResolver.subplanFromSeed(seed("Trade_Queens_Defensive")), Some(SubplanId.QueenTradeShield))
    assertEquals(ThemeResolver.subplanFromSeed(seed("Battery_Formation")), Some(SubplanId.BatteryPressure))
    assertEquals(ThemeResolver.subplanFromSeed(seed("CreatePassedPawn")), Some(SubplanId.PassedPawnManufacture))
    assertEquals(ThemeResolver.subplanFromSeed(seed("OpenFile_Doubling")), Some(SubplanId.OpenFilePressure))
  }

  test("subplanFromEvidenceSource resolves structural-state aliases") {
    assertEquals(
      ThemeResolver.subplanFromEvidenceSource("structural_state:entrenched_piece"),
      Some(SubplanId.OutpostEntrenchment)
    )
    assertEquals(
      ThemeResolver.subplanFromEvidenceSource("structural_state:generic_center_plan"),
      Some(SubplanId.CentralBreakTiming)
    )
  }
