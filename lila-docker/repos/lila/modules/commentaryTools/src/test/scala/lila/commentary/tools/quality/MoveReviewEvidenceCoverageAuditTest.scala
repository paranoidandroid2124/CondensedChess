package lila.commentary.tools.quality

import lila.commentary.tools.review.CommentaryPlayerQcSupport.*
import munit.FunSuite

final class MoveReviewEvidenceCoverageAuditTest extends FunSuite:

  private def entry(
      sampleId: String,
      sourceKind: Option[String],
      basicStatus: Option[String] = Some("blocked"),
      basicReasons: List[String] = Nil,
      supportedCandidates: List[String] = Nil,
      supportedAdmitted: List[String] = Nil,
      supportedRejectReasons: List[String] = Nil,
      playedSan: String = "Qa4",
      playedUci: String = "d1a4",
      supportRows: List[SupportRow] = Nil
  ): MoveReviewOutputEntry =
    MoveReviewOutputEntry(
      sampleId = sampleId,
      gameKey = "game",
      sliceKind = "strategic_choice",
      targetPly = 12,
      fen = "8/8/8/8/8/8/8/8 w - - 0 1",
      playedSan = playedSan,
      playedUci = playedUci,
      opening = None,
      commentary = "12. Qa4: This moves the queen to a4.",
      supportRows = supportRows,
      advancedRows = Nil,
      sourceMode = "rule",
      model = None,
      rawResponsePath = "",
      variationCount = 0,
      cacheHit = false,
      moveReviewSourceKind = sourceKind,
      basicEvidenceStatus = basicStatus,
      basicEvidenceRejectReasons = basicReasons,
      supportedLocalCandidateFamilies = supportedCandidates,
      supportedLocalAdmittedFamilies = supportedAdmitted,
      supportedLocalRejectReasons = supportedRejectReasons
    )

  test("counts source kinds and keeps basic and SupportedLocal candidate buckets disjoint") {
    val report =
      MoveReviewEvidenceCoverageAudit.build(
        List(
          entry("planner", Some("planner"), basicStatus = Some("planner_preempted")),
          entry("basic", Some("basic_move_explanation"), basicStatus = Some("emitted")),
          entry(
            "exact_basic_first",
            Some("exact_factual_fallback"),
            basicReasons = List("no_descriptor_rule_matched"),
            supportedCandidates = List("neutralize_key_break"),
            supportedAdmitted = List("neutralize_key_break")
          ),
          entry(
            "supported_runtime",
            Some("exact_factual_fallback"),
            basicReasons = List("missing_coupled_pv_line"),
            supportedCandidates = List("neutralize_key_break"),
            supportedAdmitted = List("neutralize_key_break")
          ),
          entry(
            "supported_gap",
            Some("exact_factual_fallback"),
            basicReasons = List("missing_coupled_pv_line"),
            supportedCandidates = List("neutralize_key_break"),
            supportedRejectReasons = List("neutralize_key_break:witness:branch_not_proven")
          )
        )
      )

    assertEquals(report.summary.sourceKindCounts("planner"), 1)
    assertEquals(report.summary.sourceKindCounts("basic_move_explanation"), 1)
    assertEquals(report.summary.sourceKindCounts("exact_factual_fallback"), 3)
    assertEquals(report.summary.basicExpansionCandidateSampleIds, List("exact_basic_first"))
    assertEquals(report.summary.supportedLocalRuntimeCandidateSampleIds, List("supported_runtime"))
    assertEquals(report.summary.supportedLocalEvidenceGapSampleIds, List("supported_gap"))
    assert(!report.summary.supportedLocalRuntimeCandidateSampleIds.contains("exact_basic_first"))
  }

  test("marks old JSONL rows without diagnostics as coverage insufficient") {
    val report =
      MoveReviewEvidenceCoverageAudit.build(
        List(
          entry("old", None, basicStatus = None),
          entry("new", Some("basic_move_explanation"), basicStatus = Some("emitted"))
        )
      )

    assertEquals(report.summary.coverageStatus, "coverage_insufficient")
  }

  test("treats after-PV projection availability as runtime expansion, not evidence gap") {
    val report =
      MoveReviewEvidenceCoverageAudit.build(
        List(
          entry(
            "projection",
            Some("exact_factual_fallback"),
            basicReasons = List("coupled_pv_replay_failed", "after_pv_projection_would_admit_basic")
          )
        )
      )

    assertEquals(report.summary.basicExpansionCandidateSampleIds, List("projection"))
    assertEquals(report.summary.evidenceGapCandidates, Nil)
  }

  test("buckets neutralize surface gate rejects as surface rejects") {
    val report =
      MoveReviewEvidenceCoverageAudit.build(
        List(
          entry(
            "surface_reject",
            Some("exact_factual_fallback"),
            supportedCandidates = List("neutralize_key_break"),
            supportedRejectReasons = List("neutralize_key_break:surface:played_move_collision")
          )
        )
      )

    assertEquals(report.summary.supportedLocalRejectBucketCounts.get("surface"), Some(1))
  }

  test("counts Counterplay break row quality separately from row presence") {
    val report =
      MoveReviewEvidenceCoverageAudit.build(
        List(
          entry(
            "named",
            Some("planner"),
            basicStatus = Some("planner_preempted"),
            supportRows = List(
              SupportRow(
                "Counterplay break",
                "On the checked line, this stops the ...c5 break before it appears."
              )
            )
          ),
          entry(
            "generic",
            Some("planner"),
            basicStatus = Some("planner_preempted"),
            supportRows = List(
              SupportRow("Counterplay break", "A key idea is that this keeps c5 from coming right away.")
            )
          ),
          entry(
            "collision",
            Some("planner"),
            basicStatus = Some("planner_preempted"),
            playedSan = "Bg4",
            playedUci = "c8g4",
            supportRows = List(
              SupportRow(
                "Counterplay break",
                "On the checked line, this stops the g4 break before it appears."
              )
            )
          )
        )
      )

    assertEquals(report.summary.counterplayBreakRowCount, 3)
    assertEquals(report.summary.counterplayBreakNamedTokenRowCount, 1)
    assertEquals(report.summary.counterplayBreakGenericFallbackCount, 1)
    assertEquals(report.summary.counterplayBreakPlayedMoveCollisionCount, 1)
  }

  test("counts Central break row quality separately from row presence") {
    val report =
      MoveReviewEvidenceCoverageAudit.build(
        List(
          entry(
            "named",
            Some("planner"),
            basicStatus = Some("planner_preempted"),
            supportRows = List(
              SupportRow(
                "Central break",
                "On the checked line, this also plays the e4-e5 break at this moment."
              )
            )
          ),
          entry(
            "generic",
            Some("planner"),
            basicStatus = Some("planner_preempted"),
            supportRows = List(
              SupportRow("Central break", "A key idea is that this improves the central_break_timing branch.")
            )
          ),
          entry(
            "diagonal",
            Some("planner"),
            basicStatus = Some("planner_preempted"),
            supportRows = List(
              SupportRow(
                "Central break",
                "On the checked line, this also plays the d4-e5 break at this moment."
              )
            )
          )
        )
      )

    assertEquals(report.summary.centralBreakRowCount, 3)
    assertEquals(report.summary.centralBreakNamedTokenRowCount, 1)
    assertEquals(report.summary.centralBreakGenericFallbackCount, 1)
    assertEquals(report.summary.centralBreakDiagonalCaptureVisibleCount, 1)
  }
