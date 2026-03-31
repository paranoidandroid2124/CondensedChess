package lila.llm.analysis

import munit.FunSuite
import lila.llm.model.authoring.AuthorQuestionKind

class SurfaceReplayParityTest extends FunSuite:

  private final case class FixtureExpectation(
      id: String,
      expectedPlannerPrimary: Option[AuthorQuestionKind],
      expectedActivePrimary: Option[AuthorQuestionKind],
      expectedBookmakerPlannerOwned: Boolean
  )

  private val fixtureExpectations =
    List(
      FixtureExpectation(
        id = "why_this_positive",
        expectedPlannerPrimary = Some(AuthorQuestionKind.WhyThis),
        expectedActivePrimary = Some(AuthorQuestionKind.WhyThis),
        expectedBookmakerPlannerOwned = true
      ),
      FixtureExpectation(
        id = "why_now_tactical_fallback",
        expectedPlannerPrimary = None,
        expectedActivePrimary = None,
        expectedBookmakerPlannerOwned = false
      ),
      FixtureExpectation(
        id = "what_changed_positive",
        expectedPlannerPrimary = Some(AuthorQuestionKind.WhatChanged),
        expectedActivePrimary = Some(AuthorQuestionKind.WhatChanged),
        expectedBookmakerPlannerOwned = true
      ),
      FixtureExpectation(
        id = "what_must_be_stopped_positive",
        expectedPlannerPrimary = Some(AuthorQuestionKind.WhatMustBeStopped),
        expectedActivePrimary = Some(AuthorQuestionKind.WhatMustBeStopped),
        expectedBookmakerPlannerOwned = true
      ),
      FixtureExpectation(
        id = "whose_plan_is_faster_positive",
        expectedPlannerPrimary = Some(AuthorQuestionKind.WhosePlanIsFaster),
        expectedActivePrimary = None,
        expectedBookmakerPlannerOwned = true
      ),
      FixtureExpectation(
        id = "whose_plan_is_faster_negative",
        expectedPlannerPrimary = Some(AuthorQuestionKind.WhatMustBeStopped),
        expectedActivePrimary = Some(AuthorQuestionKind.WhatMustBeStopped),
        expectedBookmakerPlannerOwned = true
      )
    )

  private def fixture(id: String): BookmakerProseGoldenFixtures.PlannerRuntimeFixture =
    BookmakerProseGoldenFixtures.plannerRuntimeFixtures
      .find(_.id == id)
      .getOrElse(fail(s"missing fixture: $id"))

  fixtureExpectations.foreach { expectation =>
    test(s"${expectation.id} keeps replay parity across planner, chronicle, active, and bookmaker") {
      val fixture = this.fixture(expectation.id)
      val outline =
        BookStyleRenderer.validatedOutline(
          fixture.ctx,
          strategyPack = fixture.strategyPack,
          truthContract = fixture.truthContract
        )
      val inputs =
        QuestionPlannerInputsBuilder.build(
          fixture.ctx,
          fixture.strategyPack,
          fixture.truthContract
        )
      val rankedPlans =
        QuestionFirstCommentaryPlanner.plan(
          fixture.ctx,
          inputs,
          fixture.truthContract
        )
      val chronicleSelection =
        GameChronicleCompressionPolicy.selectPlannerSurface(rankedPlans, inputs)
      val activeSelection =
        ActiveStrategicCoachingBriefBuilder.selectPlannerSurface(
          ActiveStrategicCoachingBriefBuilder.PlannerReplay(
            authorQuestions = Nil,
            inputs = inputs,
            rankedPlans = rankedPlans
          )
        )
      val bookmakerSlots =
        BookmakerLiveCompressionPolicy.buildSlots(
          fixture.ctx,
          outline,
          refs = None,
          strategyPack = fixture.strategyPack,
          truthContract = fixture.truthContract
        )

      assertEquals(
        rankedPlans.primary.map(_.questionKind),
        expectation.expectedPlannerPrimary,
        clues(expectation.id, rankedPlans, inputs.decisionFrame)
      )

      chronicleSelection.foreach { selection =>
        assert(
          rankedPlans.primary.contains(selection.primary) ||
            rankedPlans.secondary.contains(selection.primary),
          clues(expectation.id, rankedPlans, selection)
        )
      }

      activeSelection.foreach { selection =>
        assert(
          rankedPlans.primary.contains(selection.primary) ||
            rankedPlans.secondary.contains(selection.primary),
          clues(expectation.id, rankedPlans, selection)
        )
        assertNotEquals(
          selection.primary.questionKind,
          AuthorQuestionKind.WhosePlanIsFaster,
          clues(expectation.id, selection)
        )
      }

      assertEquals(
        activeSelection.map(_.primary.questionKind),
        expectation.expectedActivePrimary,
        clues(expectation.id, rankedPlans, activeSelection)
      )

      assertEquals(
        bookmakerSlots.nonEmpty,
        expectation.expectedBookmakerPlannerOwned,
        clues(expectation.id, bookmakerSlots, rankedPlans.primary)
      )

      if bookmakerSlots.nonEmpty then
        assert(
          rankedPlans.primary.nonEmpty,
          clues(expectation.id, bookmakerSlots, rankedPlans)
        )
      else
        val fallbackSlots =
          BookmakerLiveCompressionPolicy.buildSlotsOrFallback(
            fixture.ctx,
            outline,
            refs = None,
            strategyPack = fixture.strategyPack,
            truthContract = fixture.truthContract
          )
        assertEquals(
          fallbackSlots.paragraphPlan,
          List("p1=claim"),
          clues(expectation.id, fallbackSlots)
        )
    }
  }

