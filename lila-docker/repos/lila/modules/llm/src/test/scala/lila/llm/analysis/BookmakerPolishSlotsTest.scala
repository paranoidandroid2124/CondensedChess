package lila.llm.analysis

import munit.FunSuite

class BookmakerPolishSlotsTest extends FunSuite:

  BookmakerProseGoldenFixtures.all.foreach { fixture =>
    test(s"${fixture.id} builds bookmaker polish slots for ${fixture.expectedLens}") {
      val outline = BookStyleRenderer.validatedOutline(fixture.ctx)
      val slotsOpt = BookmakerPolishSlotsBuilder.build(fixture.ctx, outline, refs = None)
      assert(slotsOpt.isDefined, clues(s"expected slots for ${fixture.id}"))
      val slots = slotsOpt.get
      assertEquals(slots.lens, fixture.expectedLens)
      assert(BookmakerProseContract.stripMoveHeader(slots.claim).nonEmpty)
      assert(slots.paragraphPlan.headOption.contains("p1=claim"))
      assert(slots.paragraphPlan.contains("p2=support_chain"))
      assert(BookmakerProseContract.claimLikeFirstParagraph(slots.claim, slots.claim))
    }
  }

  test("soft repair restores missing claim and strips placeholders") {
    val fixture = BookmakerProseGoldenFixtures.practicalChoice
    val outline = BookStyleRenderer.validatedOutline(fixture.ctx)
    val slots = BookmakerPolishSlotsBuilder.build(fixture.ctx, outline, refs = None).get
    val broken =
      """The move keeps pressure and remains preferable.
        |
        |The direct alternative stays secondary because Piece Activation is deferred as PlayableByPV under strict evidence mode (supported by engine-coupled continuation (probe evidence pending)). Further probe work still targets Piece Activation [subplan:worst piece improvement].""".stripMargin
    val repaired = BookmakerSoftRepair.repair(broken, slots)
    assert(repaired.applied)
    assertEquals(repaired.evaluation.placeholderHits, Nil)
    assertEquals(repaired.evaluation.genericHits, Nil)
    assert(repaired.evaluation.claimLikeFirstParagraph)
    assert(repaired.evaluation.paragraphBudgetOk)
  }

  test("sanitizer preserves chess ellipsis markers in prose") {
    val raw =
      "Probe evidence starts with. Rc8: Rc8 Rc3 Rg6 (+0.42). The alternative is 12.. Qh5, and fixing lines with.. h5 makes.. Rh6-g6 realistic."
    val sanitized = BookmakerSlotSanitizer.sanitizeUserText(raw)
    assertEquals(
      sanitized,
      "Probe evidence starts with ...Rc8: Rc8 Rc3 Rg6 (+0.42). The alternative is 12...Qh5, and fixing lines with ...h5 makes ...Rh6-g6 realistic."
    )
  }
