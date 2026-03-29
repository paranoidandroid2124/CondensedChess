package lila.llm.analysis

import munit.FunSuite
import lila.llm.model.authoring.AuthorQuestionKind

class BookmakerPlannerFirstFixtureTest extends FunSuite:

  private def strippedClaim(slots: BookmakerPolishSlots): String =
    BookmakerProseContract.stripMoveHeader(slots.claim)

  BookmakerProseGoldenFixtures.plannerRuntimeFixtures.foreach { fixture =>
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
        BookmakerPolishSlotsBuilder.buildOrFallback(
          fixture.ctx,
          outline,
          refs = None,
          strategyPack = fixture.strategyPack,
          truthContract = fixture.truthContract
        )
      val prose = LiveNarrativeCompressionCore.deterministicProse(slots)
      val paragraphs = BookmakerProseContract.splitParagraphs(prose)

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
          assertNotEquals(slots.paragraphPlan, List("p1=claim"), clues(fixture.id, slots))
          assert(BookmakerProseContract.claimLikeFirstParagraph(paragraphs.headOption.getOrElse(""), slots.claim), clues(fixture.id, prose, slots))
          assert(paragraphs.size >= 2, clues(fixture.id, prose))
          fixture.expectedPrimaryKind.foreach { kind =>
            if kind == AuthorQuestionKind.WhyNow || kind == AuthorQuestionKind.WhosePlanIsFaster || kind == AuthorQuestionKind.WhatMustBeStopped then
              assert(slots.evidenceHook.nonEmpty || slots.supportPrimary.nonEmpty, clues(fixture.id, slots))
            else assert(slots.supportPrimary.nonEmpty || slots.evidenceHook.nonEmpty || slots.coda.nonEmpty, clues(fixture.id, slots))
          }
    }
  }
