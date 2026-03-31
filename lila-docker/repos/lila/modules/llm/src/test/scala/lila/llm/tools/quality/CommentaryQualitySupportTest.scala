package lila.llm.tools.quality

import munit.FunSuite
import lila.llm.tools.review.CommentaryPlayerQcSupport

class CommentaryQualitySupportTest extends FunSuite:

  import CommentaryQualitySupport.*

  private def paritySurface(
      gameKey: String,
      targetPly: Int,
      surface: String,
      sliceKind: String = "strategic_choice",
      snapshot: Option[String] = Some("snapshot"),
      carry: Option[String] = Some("carry"),
      augmentation: Option[String] = Some("augmentation"),
      bundle: Option[String] = Some("bundle"),
      question: Option[String] = Some("WhyNow"),
      ownerFamily: Option[String] = Some("ForcingDefense"),
      ownerSource: Option[String] = Some("threat"),
      replayOutcome: Option[String] = Some("planner_owned")
  ): SurfaceParitySnapshot =
    SurfaceParitySnapshot(
      sampleId = s"$gameKey:$sliceKind:$targetPly:$surface",
      gameKey = gameKey,
      surface = surface,
      sliceKind = sliceKind,
      targetPly = targetPly,
      playedSan = "Re1",
      selectedQuestion = question,
      selectedOwnerFamily = ownerFamily,
      selectedOwnerSource = ownerSource,
      replayOutcome = replayOutcome,
      digests =
        SurfaceDigestHashes(
          snapshotDigestHash = snapshot,
          carryDigestHash = carry,
          augmentationDigestHash = augmentation,
          bundleDigestHash = bundle
        )
    )

  test("same-ply parity report classifies upstream and replay mismatches separately") {
    val report =
      buildSamePlyParityReport(
        List(
          paritySurface("g1", 10, SurfaceName.Bookmaker, snapshot = Some("s1"), carry = Some("c1"), augmentation = Some("a1"), bundle = Some("b1")),
          paritySurface("g1", 10, SurfaceName.Chronicle, snapshot = Some("s2"), carry = Some("c1"), augmentation = Some("a1"), bundle = Some("b2")),
          paritySurface("g2", 20, SurfaceName.Bookmaker, snapshot = Some("s1"), carry = Some("c1"), augmentation = Some("a1"), bundle = Some("b1")),
          paritySurface("g2", 20, SurfaceName.Active, snapshot = Some("s1"), carry = Some("c2"), augmentation = Some("a1"), bundle = Some("b2")),
          paritySurface("g3", 30, SurfaceName.Bookmaker, snapshot = Some("s1"), carry = Some("c1"), augmentation = Some("a1"), bundle = Some("b1")),
          paritySurface("g3", 30, SurfaceName.Active, snapshot = Some("s1"), carry = Some("c1"), augmentation = Some("a2"), bundle = Some("b2")),
          paritySurface("g4", 40, SurfaceName.Bookmaker, snapshot = Some("s1"), carry = Some("c1"), augmentation = Some("a1"), bundle = Some("b1")),
          paritySurface("g4", 40, SurfaceName.Chronicle, snapshot = Some("s1"), carry = Some("c1"), augmentation = Some("a1"), bundle = Some("b1"), question = Some("WhatChanged")),
          paritySurface("g5", 50, SurfaceName.Bookmaker, bundle = None),
          paritySurface("g5", 50, SurfaceName.Active, bundle = Some("b2")),
          paritySurface("g6", 60, SurfaceName.Bookmaker, snapshot = Some("s1"), carry = Some("c1"), augmentation = Some("a1"), bundle = Some("b1")),
          paritySurface("g6", 60, SurfaceName.Active, snapshot = Some("s2"), carry = Some("c2"), augmentation = Some("a1"), bundle = Some("b2"))
        )
      )

    val rowsByKey = report.rows.map(row => s"${row.gameKey}:${row.targetPly}" -> row).toMap

    assertEquals(report.summary.groupedPlies, 6)
    assertEquals(report.summary.mismatchedPlies, 6)
    assertEquals(rowsByKey("g1:10").primaryTaxonomy, MismatchTaxonomy.SnapshotSkew)
    assertEquals(rowsByKey("g2:20").primaryTaxonomy, MismatchTaxonomy.CarryMismatch)
    assertEquals(rowsByKey("g3:30").primaryTaxonomy, MismatchTaxonomy.SurfaceOnlyAugmentation)
    assertEquals(rowsByKey("g3:30").pairwiseMismatches.head.allowanceTag, None)
    assertEquals(rowsByKey("g4:40").primaryTaxonomy, MismatchTaxonomy.ReplayLayerRewrite)
    assertEquals(rowsByKey("g4:40").primaryLayer, MismatchLayer.Replay)
    assertEquals(rowsByKey("g5:50").primaryTaxonomy, MismatchTaxonomy.BundleMissing)
    assertEquals(rowsByKey("g6:60").primaryTaxonomy, MismatchTaxonomy.UpstreamLayerMismatch)
  }

  test("allowed surface-only augmentation pair is tagged separately from review-needed divergence") {
    val report =
      buildSamePlyParityReport(
        List(
          paritySurface(
            "g7",
            70,
            SurfaceName.Active,
            snapshot = Some("s1"),
            carry = Some("c1"),
            augmentation = Some("a2"),
            bundle = Some("b2"),
            question = None,
            ownerFamily = None,
            ownerSource = None,
            replayOutcome = Some("omitted_no_primary")
          ),
          paritySurface(
            "g7",
            70,
            SurfaceName.Chronicle,
            snapshot = Some("s1"),
            carry = Some("c1"),
            augmentation = None,
            bundle = Some("b1"),
            question = None,
            ownerFamily = None,
            ownerSource = None,
            replayOutcome = Some("factual_fallback")
          )
        )
      )

    val mismatch = report.rows.headOption.flatMap(_.pairwiseMismatches.headOption).getOrElse(fail("missing mismatch"))
    assertEquals(mismatch.taxonomy, MismatchTaxonomy.SurfaceOnlyAugmentation)
    assertEquals(mismatch.allowanceTag, Some(SurfaceOnlyAugmentationAllowance.ActiveNoPrimaryAgainstChronicleFallback))
    assert(mismatch.allowedByDesign, clues(mismatch))
    assertEquals(
      report.summary.surfaceOnlyAugmentationAllowanceCounts,
      Map(SurfaceOnlyAugmentationAllowance.ActiveNoPrimaryAgainstChronicleFallback -> 1)
    )
  }

  test("active omitted-no-primary against bookmaker exact factual is an allowed surface-only augmentation") {
    val report =
      buildSamePlyParityReport(
        List(
          paritySurface(
            "g7b",
            71,
            SurfaceName.Active,
            snapshot = Some("s1"),
            carry = Some("c1"),
            augmentation = Some("a2"),
            bundle = Some("b2"),
            question = None,
            ownerFamily = None,
            ownerSource = None,
            replayOutcome = Some("omitted_no_primary")
          ),
          paritySurface(
            "g7b",
            71,
            SurfaceName.Bookmaker,
            snapshot = Some("s1"),
            carry = Some("c1"),
            augmentation = None,
            bundle = Some("b1"),
            question = None,
            ownerFamily = None,
            ownerSource = None,
            replayOutcome = Some("bookmaker_exact_factual")
          )
        )
      )

    val mismatch = report.rows.headOption.flatMap(_.pairwiseMismatches.headOption).getOrElse(fail("missing mismatch"))
    assertEquals(mismatch.taxonomy, MismatchTaxonomy.SurfaceOnlyAugmentation)
    assertEquals(
      mismatch.allowanceTag,
      Some(SurfaceOnlyAugmentationAllowance.ActiveNoPrimaryAgainstBookmakerExactFactual)
    )
    assert(mismatch.allowedByDesign, clues(mismatch))
    assertEquals(
      report.summary.surfaceOnlyAugmentationAllowanceCounts,
      Map(SurfaceOnlyAugmentationAllowance.ActiveNoPrimaryAgainstBookmakerExactFactual -> 1)
    )
  }

  test("row-level primary taxonomy prefers disallowed upstream mismatch over allowed surface-only augmentation") {
    val report =
      buildSamePlyParityReport(
        List(
          paritySurface(
            "g8",
            80,
            SurfaceName.Active,
            snapshot = Some("s1"),
            carry = Some("c1"),
            augmentation = Some("a2"),
            bundle = Some("b2"),
            question = None,
            ownerFamily = None,
            ownerSource = None,
            replayOutcome = Some("omitted_no_primary")
          ),
          paritySurface(
            "g8",
            80,
            SurfaceName.Chronicle,
            snapshot = Some("s1"),
            carry = Some("c1"),
            augmentation = None,
            bundle = Some("b1"),
            question = None,
            ownerFamily = None,
            ownerSource = None,
            replayOutcome = Some("factual_fallback")
          ),
          paritySurface(
            "g8",
            80,
            SurfaceName.Bookmaker,
            snapshot = Some("s2"),
            carry = Some("c1"),
            augmentation = Some("a1"),
            bundle = Some("b3"),
            question = Some("WhyThis"),
            ownerFamily = Some("TacticalFailure"),
            ownerSource = Some("forcing_contract"),
            replayOutcome = Some("bookmaker_planner_owned")
          )
        )
      )

    val row = report.rows.headOption.getOrElse(fail("missing parity row"))
    assertEquals(row.primaryTaxonomy, MismatchTaxonomy.UpstreamLayerMismatch)
    assert(row.pairwiseMismatches.exists(_.allowedByDesign), clues(row))
  }

  test("active omitted-after-primary against planner-owned is an allowed surface-only augmentation") {
    val report =
      buildSamePlyParityReport(
        List(
          paritySurface(
            "g9",
            90,
            SurfaceName.Active,
            augmentation = Some("a2"),
            bundle = Some("b2"),
            replayOutcome = Some("omitted_after_primary")
          ),
          paritySurface(
            "g9",
            90,
            SurfaceName.Bookmaker,
            augmentation = None,
            bundle = Some("b1"),
            replayOutcome = Some("bookmaker_planner_owned")
          ),
          paritySurface(
            "g9",
            90,
            SurfaceName.Chronicle,
            augmentation = None,
            bundle = Some("b3"),
            replayOutcome = Some("planner_owned")
          )
        )
      )

    val row = report.rows.headOption.getOrElse(fail("missing parity row"))
    val activeBookmaker =
      row.pairwiseMismatches.find(m => m.leftSurface == SurfaceName.Active && m.rightSurface == SurfaceName.Bookmaker)
        .getOrElse(fail("missing active/bookmaker mismatch"))
    val activeChronicle =
      row.pairwiseMismatches.find(m => m.leftSurface == SurfaceName.Active && m.rightSurface == SurfaceName.Chronicle)
        .getOrElse(fail("missing active/chronicle mismatch"))

    assertEquals(activeBookmaker.taxonomy, MismatchTaxonomy.SurfaceOnlyAugmentation)
    assertEquals(
      activeBookmaker.allowanceTag,
      Some(SurfaceOnlyAugmentationAllowance.ActiveOmittedAfterPrimaryAgainstPlannerOwned)
    )
    assert(activeBookmaker.allowedByDesign, clues(activeBookmaker))
    assertEquals(
      activeChronicle.allowanceTag,
      Some(SurfaceOnlyAugmentationAllowance.ActiveOmittedAfterPrimaryAgainstPlannerOwned)
    )
    assert(activeChronicle.allowedByDesign, clues(activeChronicle))
    assertEquals(row.primaryTaxonomy, MismatchTaxonomy.SurfaceOnlyAugmentation)
    assertEquals(row.primaryLayer, MismatchLayer.Upstream)
  }

  test("bookmaker planner-owned and chronicle planner-owned normalize to the same replay outcome") {
    val report =
      buildSamePlyParityReport(
        List(
          paritySurface(
            "g10",
            100,
            SurfaceName.Bookmaker,
            replayOutcome = Some("bookmaker_planner_owned")
          ),
          paritySurface(
            "g10",
            100,
            SurfaceName.Chronicle,
            replayOutcome = Some("planner_owned")
          )
        )
      )

    assertEquals(report.summary.mismatchedPlies, 0)
    assertEquals(report.rows, Nil)
  }

  test("bookmaker exact factual and chronicle factual fallback normalize to the same replay outcome") {
    val report =
      buildSamePlyParityReport(
        List(
          paritySurface(
            "g10b",
            101,
            SurfaceName.Bookmaker,
            question = None,
            ownerFamily = None,
            ownerSource = None,
            replayOutcome = Some("bookmaker_exact_factual")
          ),
          paritySurface(
            "g10b",
            101,
            SurfaceName.Chronicle,
            question = None,
            ownerFamily = None,
            ownerSource = None,
            replayOutcome = Some("factual_fallback")
          )
        )
      )

    assertEquals(report.summary.mismatchedPlies, 0)
    assertEquals(report.rows, Nil)
  }

  test("judge prompt names all quality rubric axes") {
    val (records, _) = sampleEvaluationSlice()
    val before = records.find(_.candidateLabel == "before")
    val after = records.find(_.candidateLabel == "after").getOrElse(fail("missing `after` sample"))
    val prompt = renderJudgePrompt(after, before)

    EvalRubric.all.foreach(rubric => assert(prompt.contains(s"`$rubric`"), clues(prompt)))
    assert(prompt.contains("internal filter, not a replacement for human evaluation"), clues(prompt))
    assert(prompt.contains("Return exactly one JSON object"), clues(prompt))
  }

  test("sample evaluation slice exposes before-after summary and markdown scaffold") {
    val (records, summaries) = sampleEvaluationSlice()
    val summary = summaries.headOption.getOrElse(fail("missing comparison summary"))
    val markdown = renderEvaluationSummaryMarkdown(records, summaries)
    val before = records.find(_.candidateLabel == "before").getOrElse(fail("missing before"))
    val after = records.find(_.candidateLabel == "after").getOrElse(fail("missing after"))

    assertEquals(records.map(_.candidateLabel), List("before", "after"))
    assert(summary.improved, clues(summary))
    assert(summary.netDelta > 0, clues(summary))
    assert(!before.selection.moveAttributionGatePassed, clues(before))
    assert(after.selection.moveAttributionGatePassed, clues(after))
    assert(after.selection.thresholdVerdict == EvalVerdict.Keep, clues(after))
    assert(markdown.contains("Commentary Quality Metrics Scaffold"), clues(markdown))
    assert(markdown.contains("before -> after"), clues(markdown))
  }

  test("selector blocks usefulness credit when move attribution fails") {
    val (records, _) = buildRealEvaluationSeedSlice(
      List(
        CommentaryPlayerQcSupport.BookmakerOutputEntry(
          sampleId =
            "2024_03_03_4_1_bochnicka_vladimir_krivoborodov_egor_lichess_broadcast_master_classical_67:strategic_choice:14:bookmaker",
          gameKey = "2024_03_03_4_1_bochnicka_vladimir_krivoborodov_egor_lichess_broadcast_master_classical_67",
          sliceKind = "strategic_choice",
          targetPly = 14,
          fen = "fen",
          playedSan = "cxd4",
          playedUci = "c5d4",
          opening = None,
          commentary = "baseline",
          supportRows = Nil,
          advancedRows = Nil,
          sourceMode = "rule",
          model = None,
          rawResponsePath = "raw",
          variationCount = 1,
          cacheHit = false,
          bookmakerBundleDigestHash = Some("b1")
        ),
        CommentaryPlayerQcSupport.BookmakerOutputEntry(
          sampleId =
            "2024_03_03_4_1_bochnicka_vladimir_krivoborodov_egor_lichess_broadcast_master_classical_67:practical_simplification:20:bookmaker",
          gameKey = "2024_03_03_4_1_bochnicka_vladimir_krivoborodov_egor_lichess_broadcast_master_classical_67",
          sliceKind = "practical_simplification",
          targetPly = 20,
          fen = "fen",
          playedSan = "Be7",
          playedUci = "f8e7",
          opening = None,
          commentary = "baseline",
          supportRows = Nil,
          advancedRows = Nil,
          sourceMode = "rule",
          model = None,
          rawResponsePath = "raw",
          variationCount = 1,
          cacheHit = false,
          bookmakerBundleDigestHash = Some("b2")
        ),
        CommentaryPlayerQcSupport.BookmakerOutputEntry(
          sampleId =
            "2024_03_03_4_1_bochnicka_vladimir_krivoborodov_egor_lichess_broadcast_master_classical_67:long_structural_squeeze:54:bookmaker",
          gameKey = "2024_03_03_4_1_bochnicka_vladimir_krivoborodov_egor_lichess_broadcast_master_classical_67",
          sliceKind = "long_structural_squeeze",
          targetPly = 54,
          fen = "fen",
          playedSan = "Ke7",
          playedUci = "f8e7",
          opening = None,
          commentary = "baseline",
          supportRows = Nil,
          advancedRows = Nil,
          sourceMode = "rule",
          model = None,
          rawResponsePath = "raw",
          variationCount = 1,
          cacheHit = false,
          bookmakerBundleDigestHash = Some("b3")
        )
      ),
      buildSamePlyParityReport(
        List(
          paritySurface(
            "2024_03_03_4_1_bochnicka_vladimir_krivoborodov_egor_lichess_broadcast_master_classical_67",
            14,
            SurfaceName.Bookmaker,
            sliceKind = "strategic_choice"
          ),
          paritySurface(
            "2024_03_03_4_1_bochnicka_vladimir_krivoborodov_egor_lichess_broadcast_master_classical_67",
            20,
            SurfaceName.Bookmaker,
            sliceKind = "practical_simplification"
          ),
          paritySurface(
            "2024_03_03_4_1_bochnicka_vladimir_krivoborodov_egor_lichess_broadcast_master_classical_67",
            54,
            SurfaceName.Bookmaker,
            sliceKind = "long_structural_squeeze"
          )
        )
      )
    ).fold(err => fail(err), identity)

    val gatedAfter =
      records.find(record =>
        record.sampleId.endsWith("practical_simplification:20:bookmaker") && record.candidateLabel == "after"
      ).getOrElse(fail("missing gated after row"))

    assert(!gatedAfter.selection.moveAttributionGatePassed, clues(gatedAfter))
    assertEquals(gatedAfter.selection.usefulnessCredit, 0)
    assert(gatedAfter.selection.usefulnessRewardBlocked, clues(gatedAfter))
    assertEquals(gatedAfter.selection.thresholdVerdict, EvalVerdict.Review)
  }
