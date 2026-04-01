package lila.llm.analysis

import munit.FunSuite
import lila.llm.model.authoring.AuthorQuestionKind
import lila.llm.model.*
import lila.llm.model.authoring.*

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
        id = "restricted_defense_conversion_positive",
        expectedPlannerPrimary = Some(AuthorQuestionKind.WhyThis),
        expectedActivePrimary = Some(AuthorQuestionKind.WhyThis),
        expectedBookmakerPlannerOwned = true
      ),
      FixtureExpectation(
        id = "restricted_defense_conversion_fragile",
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

  test("uncertified restricted-defense conversion stays out of planner and replay primaries") {
    val ctx =
      BookmakerProseGoldenFixtures.openFileFight.ctx.copy(
        playedMove = Some("c3c4"),
        playedSan = Some("Rc4"),
        phase = PhaseContext("Endgame", "Technical conversion edge"),
        authorQuestions =
          List(
            AuthorQuestion(
              id = "q_convert",
              kind = AuthorQuestionKind.WhyNow,
              priority = 2,
              question = "Why is Rc4 the right conversion moment?",
              evidencePurposes = List("convert_reply_multipv")
            )
          ),
        authorEvidence =
          List(
            QuestionEvidence(
              questionId = "q_convert",
              purpose = "convert_reply_multipv",
              branches =
                List(
                  EvidenceBranch(
                    keyMove = "line_1",
                    line = "a) ...g6 Rg7 keeps the defender active.",
                    evalCp = Some(220)
                  )
                )
            )
          ),
        mainStrategicPlans =
          List(
            PlanHypothesis(
              planId = "simplification_conversion",
              planName = "Simplify into a winning pawn ending",
              rank = 1,
              score = 0.82,
              preconditions = Nil,
              executionSteps = List("Trade the last active defender."),
              failureModes = List("Wrong move order restores counterplay."),
              viability = PlanViability(score = 0.8, label = "high", risk = "test"),
              evidenceSources = List("theme:advantage_transformation"),
              themeL1 = ThemeTaxonomy.ThemeL1.AdvantageTransformation.id,
              subplanId = Some(ThemeTaxonomy.SubplanId.SimplificationConversion.id)
            )
          ),
        strategicPlanExperiments =
          List(
            StrategicPlanExperiment(
              planId = "simplification_conversion",
              themeL1 = ThemeTaxonomy.ThemeL1.AdvantageTransformation.id,
              subplanId = Some(ThemeTaxonomy.SubplanId.SimplificationConversion.id),
              evidenceTier = "deferred",
              supportProbeCount = 1,
              bestReplyStable = true,
              futureSnapshotAligned = true
            )
          )
      )
    val outline = BookStyleRenderer.validatedOutline(ctx, strategyPack = None, truthContract = None)
    val inputs = QuestionPlannerInputsBuilder.build(ctx, strategyPack = None, truthContract = None)
    val rankedPlans = QuestionFirstCommentaryPlanner.plan(ctx, inputs, truthContract = None)
    val chronicleSelection =
      GameChronicleCompressionPolicy.selectPlannerSurface(rankedPlans, inputs)
    val activeSelection =
      ActiveStrategicCoachingBriefBuilder.selectPlannerSurface(
        ActiveStrategicCoachingBriefBuilder.PlannerReplay(
          authorQuestions = ctx.authorQuestions,
          inputs = inputs,
          rankedPlans = rankedPlans
        )
      )
    val bookmakerSlots =
      BookmakerLiveCompressionPolicy.buildSlots(
        ctx,
        outline,
        refs = None,
        strategyPack = None,
        truthContract = None
      )
    val fallbackSlots =
      BookmakerLiveCompressionPolicy.buildSlotsOrFallback(
        ctx,
        outline,
        refs = None,
        strategyPack = None,
        truthContract = None
      )

    assertEquals(rankedPlans.primary, None, clues(rankedPlans, inputs.decisionFrame))
    assertEquals(chronicleSelection, None, clues(chronicleSelection, rankedPlans))
    assertEquals(activeSelection, None, clues(activeSelection, rankedPlans))
    assertEquals(bookmakerSlots, None, clues(bookmakerSlots, rankedPlans))
    assertEquals(BookmakerProseContract.stripMoveHeader(fallbackSlots.claim), "This puts the rook on c4.")
  }
