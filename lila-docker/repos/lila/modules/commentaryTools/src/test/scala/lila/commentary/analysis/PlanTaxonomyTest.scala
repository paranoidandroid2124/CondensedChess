package lila.commentary.analysis

import munit.FunSuite

class PlanTaxonomyTest extends FunSuite:

  import PlanTaxonomy.PlanKind
  import PlanTaxonomy.PlanTheme
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

  test("theme resolver owns embedded theme tag projection") {
    assertEquals(
      ThemeResolver.themeTagFromEmbeddedText("owner precondition theme:Pawn Break Preparation"),
      Some(ThemeResolver.themeTag(PlanTheme.PawnBreakPreparation))
    )
  }

  test("theme resolver owns embedded subplan annotation stripping") {
    val raw = "Further probe work still targets Piece Activation [subplan:worst_piece_improvement]."

    assert(ThemeResolver.hasSubplanAnnotation(raw))
    assertEquals(
      ThemeResolver.stripSubplanAnnotations(raw),
      "Further probe work still targets Piece Activation."
    )
  }
