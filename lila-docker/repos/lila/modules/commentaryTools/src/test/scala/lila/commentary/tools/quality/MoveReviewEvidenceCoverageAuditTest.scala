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
      supportedRejectReasons: List[String] = Nil
  ): MoveReviewOutputEntry =
    MoveReviewOutputEntry(
      sampleId = sampleId,
      gameKey = "game",
      sliceKind = "strategic_choice",
      targetPly = 12,
      fen = "8/8/8/8/8/8/8/8 w - - 0 1",
      playedSan = "Qa4",
      playedUci = "d1a4",
      opening = None,
      commentary = "12. Qa4: This moves the queen to a4.",
      supportRows = Nil,
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
