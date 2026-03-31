package lila.llm.tools.quality

import munit.FunSuite
import lila.llm.tools.review.{ ChronicleActivePlannerSliceRunner, CommentaryPlayerQcSupport }

class CommentaryQualityContrastSupportTest extends FunSuite:

  import CommentaryPlayerQcSupport.*
  import CommentaryQualitySupport.*
  import CommentaryQualityContrastSupport.*

  private def bookmakerEntry(
      sampleId: String,
      commentary: String,
      primaryKind: Option[String],
      selectedQuestion: Option[String],
      selectedOwnerFamily: Option[String],
      selectedOwnerSource: Option[String],
      fallbackMode: String,
      contrastAdmissible: Option[Boolean] = Some(true),
      contrastRejectReason: Option[String] = None
  ): BookmakerOutputEntry =
    BookmakerOutputEntry(
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
      bookmakerFallbackMode = fallbackMode,
      plannerSceneType = Some("forcing_defense"),
      plannerSelectedQuestion = selectedQuestion,
      plannerSelectedOwnerFamily = selectedOwnerFamily,
      plannerSelectedOwnerSource = selectedOwnerSource,
      surfaceReplayOutcome =
        Some(if fallbackMode == "planner_owned" then "bookmaker_planner_owned" else "bookmaker_exact_factual"),
      contrast_admissible = contrastAdmissible,
      contrast_reject_reason = contrastRejectReason
    )

  private def surfaceEntry(sampleId: String): ChronicleActivePlannerSliceRunner.SliceSurfaceEntry =
    ChronicleActivePlannerSliceRunner.SliceSurfaceEntry(
      sampleId = sampleId.replace(":bookmaker", ":chronicle"),
      gameKey = "g1",
      mixBucket = CommentaryPlayerQcSupport.MixBucket.Club,
      sliceKind = "strategic_choice",
      targetPly = 14,
      playedSan = "Re1",
      momentPresent = true,
      authorQuestionKinds = List("WhyNow"),
      authorEvidenceKinds = List("reply_multipv"),
      chronicleMode = "planner_owned",
      chroniclePrimaryKind = Some("WhyNow"),
      chronicleSecondaryKind = None,
      chronicleSelectedOwnerFamily = Some("ForcingDefense"),
      chronicleSelectedOwnerSource = Some("truth_contract"),
      chronicleNarrative = Some("Chronicle keeps the timing point concrete."),
      chronicleBlankLike = false,
      activeMode = "attached",
      activePrimaryKind = Some("WhyNow"),
      activeSecondaryKind = None,
      activeSelectedOwnerFamily = Some("ForcingDefense"),
      activeSelectedOwnerSource = Some("truth_contract"),
      activePlannerApproved = true,
      activeNoteBuilt = true,
      activeValidatorPassed = true,
      activeFinalizationStage = Some("attached"),
      activeRejectReason = None,
      activeHardReasons = Nil,
      activeWarningReasons = Nil,
      activeNoteStatus = Some("attached"),
      activeNote = Some("Active note stays aligned."),
      activeNoteCandidate = Some("Active note stays aligned."),
      activeBlankLike = false,
      plannerSceneType = Some("forcing_defense"),
      plannerSelectedQuestion = Some("WhyNow"),
      plannerSelectedOwnerFamily = Some("ForcingDefense"),
      plannerSelectedOwnerSource = Some("truth_contract"),
      chronicleReplayMode = "planner_owned",
      chronicleReplayPrimaryKind = Some("WhyNow"),
      chronicleReplaySecondaryKind = None,
      chronicleReplaySelectedOwnerFamily = Some("ForcingDefense"),
      chronicleReplaySelectedOwnerSource = Some("truth_contract"),
      chronicleReplayNarrative = Some("Chronicle replay keeps the timing point concrete."),
      chronicleReplayBlankLike = false,
      activeReplayMode = "attached",
      activeReplayPrimaryKind = Some("WhyNow"),
      activeReplaySecondaryKind = None,
      activeReplaySelectedOwnerFamily = Some("ForcingDefense"),
      activeReplaySelectedOwnerSource = Some("truth_contract"),
      activeReplayPlannerApproved = true,
      activeReplayNoteBuilt = true,
      activeReplayValidatorPassed = true,
      activeReplayFinalizationStage = Some("attached"),
      activeReplayRejectReason = None,
      activeReplayHardReasons = Nil,
      activeReplayWarningReasons = Nil,
      activeReplayNote = Some("Active replay stays aligned."),
      activeReplayNoteCandidate = Some("Active replay stays aligned."),
      activeReplayBlankLike = false,
      chronicleSurfaceReplayOutcome = Some("planner_owned"),
      activeSurfaceReplayOutcome = Some("attached")
    )

  test("after bookmaker exact factual rows are blocked out of eligible contrast gain metrics") {
    val sampleId = "g1:strategic_choice:14:bookmaker"
    val before =
      bookmakerEntry(
        sampleId = sampleId,
        commentary = "14... Re1: This keeps the e-file under control before the reply lands.",
        primaryKind = Some("WhyNow"),
        selectedQuestion = Some("WhyNow"),
        selectedOwnerFamily = Some("ForcingDefense"),
        selectedOwnerSource = Some("truth_contract"),
        fallbackMode = "planner_owned"
      )
    val after =
      bookmakerEntry(
        sampleId = sampleId,
        commentary = "14... Re1: This puts the rook on e1.",
        primaryKind = None,
        selectedQuestion = None,
        selectedOwnerFamily = None,
        selectedOwnerSource = None,
        fallbackMode = "exact_factual",
        contrastAdmissible = Some(false)
      )
    val parityReport =
      SamePlyParityReport(
        summary =
          SamePlyParitySummary(
            groupedPlies = 0,
            mismatchedPlies = 0,
            taxonomyCounts = Map.empty,
            layerCounts = Map.empty
          ),
        rows = Nil
      )

    val report =
      CommentaryQualityContrastSupport
        .buildContrastReport(
          beforeEntries = List(before),
          afterEntries = List(after),
          surfaceEntries = List(surfaceEntry(sampleId)),
          parityReport = parityReport
        )
        .getOrElse(fail("expected contrast report"))

    val row = report.selectorRows.headOption.getOrElse(fail("missing selector row"))
    assertEquals(row.selectionStatus, SelectionStatus.AfterFallbackBlocked, clues(row))
    assertEquals(row.selectionReason, "after_bookmaker_exact_factual")
    assertEquals(report.evalRows, Nil)
    assertEquals(report.summary.contrastEligibleRows, 0)
    assertEquals(report.summary.afterFallbackCount, 0)
    assertEquals(report.summary.afterFallbackBlockedRows, 1)
    assertEquals(report.summary.degradedCount, 0)
    assertEquals(report.summary.baselineRegressionStatus, "no_baseline_regression")
  }
