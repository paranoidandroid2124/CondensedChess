package lila.commentary.analysis

import munit.FunSuite
import lila.commentary.model.authoring.AuthorQuestionKind

class MoveReviewPlannerFirstFixtureTest extends FunSuite:

  private def strippedClaim(slots: MoveReviewPolishSlots): String =
    MoveReviewProseContract.stripMoveHeader(slots.claim)

  MoveReviewProseGoldenFixtures.plannerRuntimeFixtures.foreach { fixture =>
    test(s"${fixture.id} ${fixture.expectation} runtime remains planner-first for ${fixture.questionKind}") {
      val outline =
        BookStyleRenderer.validatedOutline(
          fixture.ctx,
          strategyPack = fixture.strategyPack,
          truthContract = fixture.truthContract
        )
      val plannerInputs =
        QuestionPlannerInputsBuilder.build(fixture.ctx, fixture.strategyPack, fixture.truthContract)
      val rankedPlans =
        QuestionFirstCommentaryPlanner.plan(fixture.ctx, plannerInputs, fixture.truthContract)
      val slots =
        MoveReviewPolishSlotsBuilder.buildOrFallback(
          fixture.ctx,
          outline,
          refs = None,
          strategyPack = fixture.strategyPack,
          truthContract = fixture.truthContract
        )
      val prose = LiveNarrativeCompressionCore.deterministicProse(slots)
      val paragraphs = MoveReviewProseContract.splitParagraphs(prose)

      assertEquals(
        rankedPlans.primary.map(_.questionKind),
        fixture.expectedPrimaryKind,
        clues(fixture.id, rankedPlans, plannerInputs.decisionFrame, outline.diagnostics.map(_.summary))
      )

      fixture.expectedFallbackClaim match
        case Some(expectedClaim) =>
          assertEquals(strippedClaim(slots), expectedClaim, clues(fixture.id, slots))
          assertEquals(slots.paragraphPlan, List("p1=claim"), clues(fixture.id, slots))
          assertEquals(paragraphs.size, 1, clues(fixture.id, prose))
        case None =>
          val claim = strippedClaim(slots).toLowerCase
          val expectedFragment = fixture.expectedClaimFragment.map(_.toLowerCase).getOrElse("")
          assert(claim.contains(expectedFragment), clues(fixture.id, claim, slots))
          assert(MoveReviewProseContract.claimLikeFirstParagraph(paragraphs.headOption.getOrElse(""), slots.claim), clues(fixture.id, prose, slots))
          if fixture.expectExpandedProse then
            assertNotEquals(slots.paragraphPlan, List("p1=claim"), clues(fixture.id, slots))
            assert(paragraphs.size >= 2, clues(fixture.id, prose))
            fixture.expectedPrimaryKind.foreach { kind =>
              if kind == AuthorQuestionKind.WhyNow || kind == AuthorQuestionKind.WhosePlanIsFaster || kind == AuthorQuestionKind.WhatMustBeStopped then
                assert(slots.evidenceHook.nonEmpty || slots.supportPrimary.nonEmpty, clues(fixture.id, slots))
              else assert(slots.supportPrimary.nonEmpty || slots.evidenceHook.nonEmpty || slots.coda.nonEmpty, clues(fixture.id, slots))
            }
          else
            assertEquals(slots.paragraphPlan, List("p1=claim"), clues(fixture.id, slots))
            assertEquals(paragraphs.size, 1, clues(fixture.id, prose))
    }
  }
