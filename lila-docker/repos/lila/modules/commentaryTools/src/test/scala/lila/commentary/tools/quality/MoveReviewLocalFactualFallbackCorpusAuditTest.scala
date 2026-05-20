package lila.commentary.tools.quality

import munit.FunSuite
import lila.commentary.tools.review.CommentaryPlayerQcSupport

class MoveReviewLocalFactualFallbackCorpusAuditTest extends FunSuite:

  import CommentaryPlayerQcSupport.*

  private def entry(
      sampleId: String,
      commentary: String,
      fallbackMode: String = "exact_factual"
  ): MoveReviewOutputEntry =
    MoveReviewOutputEntry(
      sampleId = sampleId,
      gameKey = "game",
      sliceKind = "strategic_choice",
      targetPly = 20,
      fen = "fen",
      playedSan = "Qxc6",
      playedUci = "c4c6",
      opening = None,
      commentary = commentary,
      supportRows = Nil,
      advancedRows = Nil,
      sourceMode = "rule",
      model = None,
      rawResponsePath = "raw",
      variationCount = 1,
      cacheHit = false,
      moveReviewFallbackMode = fallbackMode
    )

  test("audit classifies local factual fallback counters and accepts clean richer after rows") {
    val before =
      List(
        entry("literal", "20. Qxc6: This captures."),
        entry("role", "20. Qxc6: This captures the knight on c6."),
        entry("planner", "20. Rc3: This keeps the route available.", fallbackMode = "planner_owned")
      )
    val after =
      List(
        entry(
          "literal",
          "20. Qxc6: This captures the knight on c6.\n\nThe local material change is a captured knight. It also gives check. The checked line begins Qxc6 Kg8."
        ),
        entry(
          "role",
          "20. exd6: This captures the pawn en passant and lands on d6.\n\nThe local material change is a captured pawn."
        ),
        entry("planner", "20. Rc3: This keeps the route available.", fallbackMode = "planner_owned")
      )

    val report = MoveReviewLocalFactualFallbackCorpusAudit.build(before, after)

    assertEquals(report.summary.beforeExactFactualRows, 2)
    assertEquals(report.summary.afterExactFactualRows, 2)
    assertEquals(report.summary.beforePlannerOwnedRows, 1)
    assertEquals(report.summary.afterPlannerOwnedRows, 1)
    assertEquals(report.summary.beforeLiteralCaptureFloorCount, 1)
    assertEquals(report.summary.afterLiteralCaptureFloorCount, 0)
    assertEquals(report.summary.afterRoleAnchoredCaptureCount, 2)
    assertEquals(report.summary.afterEnPassantCount, 1)
    assertEquals(report.summary.afterMaterialSupportCount, 2)
    assertEquals(report.summary.afterTacticalMotifSupportCount, 1)
    assertEquals(report.summary.afterCoupledPvSupportCount, 1)
    assertEquals(report.summary.acceptanceStatus, "accepted")
    assertEquals(report.summary.forbiddenHits, Nil)
  }

  test("audit rejects fallback mode drift and forbidden wording") {
    val before =
      List(
        entry("mode_drift", "20. Qxc6: This captures."),
        entry("forbidden", "20. Qxc6: This captures the knight on c6.")
      )
    val after =
      List(
        entry("mode_drift", "20. Qxc6: This captures the knight on c6.", fallbackMode = "planner_owned"),
        entry("forbidden", "20. Qxc6: This wins material and keeps pressure.", fallbackMode = "exact_factual")
      )

    val report = MoveReviewLocalFactualFallbackCorpusAudit.build(before, after)

    assertEquals(report.summary.acceptanceStatus, "rejected")
    assert(report.summary.modeChangedSampleIds.contains("mode_drift"), clues(report.summary))
    assert(report.summary.forbiddenHits.exists(hit => hit.sampleId == "forbidden" && hit.term == "wins"), clues(report.summary))
    assert(report.summary.forbiddenHits.exists(hit => hit.sampleId == "forbidden" && hit.term == "pressure"), clues(report.summary))
  }
