package lila.commentary.tools.quality

import munit.FunSuite
import lila.commentary.tools.review.CommentaryPlayerQcSupport

class CommentaryQualityContrastSupportTest extends FunSuite:

  import CommentaryPlayerQcSupport.*
  import CommentaryQualitySupport.*
  import CommentaryQualityContrastSupport.*

  private def moveReviewEntry(
      sampleId: String,
      commentary: String,
      primaryKind: Option[String],
      selectedQuestion: Option[String],
      selectedOwnerKind: Option[String],
      selectedSource: Option[String],
      fallbackMode: String,
      contrastAdmissible: Option[Boolean] = Some(true),
      contrastRejectReason: Option[String] = None
  ): MoveReviewOutputEntry =
    MoveReviewOutputEntry(
      sampleId = sampleId,
      gameKey = "g1",
      sliceKind = "strategic_choice",
      targetPly = 14,
      fen = "fen",
      playedSan = "Re1",
      playedUci = "e2e4",
      opening = None,
      commentary = commentary,
      supportRows = Nil,
      advancedRows = Nil,
      sourceMode = "rule",
      model = None,
      rawResponsePath = "raw",
      variationCount = 1,
      cacheHit = false,
      plannerPrimaryKind = primaryKind,
      plannerPrimaryFallbackMode = primaryKind.map(_ => "PlannerOwned"),
      plannerSecondaryKind = None,
      plannerSecondarySurfaced = false,
      moveReviewFallbackMode = fallbackMode,
      plannerSceneType = Some("forcing_defense"),
      plannerSelectedQuestion = selectedQuestion,
      plannerSelectedOwnerKind = selectedOwnerKind,
      plannerSelectedSource = selectedSource,
      surfaceReplayOutcome =
        Some(if fallbackMode == "planner_owned" then "move_review_planner_owned" else "move_review_exact_factual"),
      contrast_admissible = contrastAdmissible,
      contrast_reject_reason = contrastRejectReason
    )

  test("after moveReview exact factual rows are blocked out of eligible contrast gain metrics") {
    val sampleId = "g1:strategic_choice:14:moveReview"
    val before =
      moveReviewEntry(
        sampleId = sampleId,
        commentary = "14... Re1: This keeps the e-file under control before the reply lands.",
        primaryKind = Some("WhyNow"),
        selectedQuestion = Some("WhyNow"),
        selectedOwnerKind = Some("ForcingDefense"),
        selectedSource = Some("truth_contract"),
        fallbackMode = "planner_owned"
      )
    val after =
      moveReviewEntry(
        sampleId = sampleId,
        commentary = "14... Re1: This puts the rook on e1.",
        primaryKind = None,
        selectedQuestion = None,
        selectedOwnerKind = None,
        selectedSource = None,
        fallbackMode = "exact_factual",
        contrastAdmissible = Some(false)
      )

    val report =
      CommentaryQualityContrastSupport
        .buildContrastReport(
          beforeEntries = List(before),
          afterEntries = List(after)
        )
        .getOrElse(fail("expected contrast report"))

    val row = report.selectorRows.headOption.getOrElse(fail("missing selector row"))
    assertEquals(row.selectionStatus, SelectionStatus.AfterFallbackBlocked, clues(row))
    assertEquals(row.selectionReason, "after_move_review_exact_factual")
    assertEquals(report.evalRows, Nil)
    assertEquals(report.summary.contrastEligibleRows, 0)
    assertEquals(report.summary.afterFallbackCount, 0)
    assertEquals(report.summary.afterFallbackBlockedRows, 1)
    assertEquals(report.summary.degradedCount, 0)
    assertEquals(report.summary.baselineRegressionStatus, "no_baseline_regression")
  }
