package lila.llm.analysis

import munit.FunSuite
import lila.llm.model.*
import lila.llm.model.authoring.*

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

  test("tactical blunders promote the negative main-move claim ahead of strategic thesis text") {
    val ctx =
      BookmakerProseGoldenFixtures.rookPawnMarch.ctx.copy(
        meta = Some(
          MetaSignals(
            choiceType = ChoiceType.NarrowChoice,
            targets = Targets(Nil, Nil),
            planConcurrency = PlanConcurrency("Rook-Pawn March", None, "independent"),
            errorClass = Some(
              ErrorClassification(
                isTactical = true,
                missedMotifs = List("Fork"),
                errorSummary = "전술(320cp, Fork)"
              )
            )
          )
        )
      )
    val outline =
      NarrativeOutline(
        beats = List(
          OutlineBeat(
            kind = OutlineBeatKind.Context,
            text = "The move reorganizes the pieces around Kingside Clamp, aiming at Rook-Pawn March."
          ),
          OutlineBeat(
            kind = OutlineBeatKind.MainMove,
            text =
              "**h5??** is a tactical blunder; it misses the idea of a fork on e6 and allows ...Rc1+ by force. Better is **Rc3** to keep tighter control. The forcing reply lands before the kingside plan gets going."
          )
        )
      )
    val slots = BookmakerPolishSlotsBuilder.build(ctx, outline, refs = None).getOrElse(fail("missing slots"))
    val claimCore = BookmakerProseContract.stripMoveHeader(slots.claim).toLowerCase
    assert(claimCore.contains("tactical blunder"))
    assert(!claimCore.contains("reorganizes the pieces around kingside clamp"))
    assert(slots.support.exists(_.toLowerCase.contains("better is **rc3**")))
  }
