package lila.commentary.tools.quality

import munit.FunSuite
import lila.commentary.tools.review.CommentaryPlayerQcSupport

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
      proofFamily: Option[String] = Some("ForcingDefense"),
      proofSource: Option[String] = Some("threat"),
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
      selectedOwnerKind = proofFamily,
      selectedSource = proofSource,
      replayOutcome = replayOutcome,
      digests =
        SurfaceDigestHashes(
          snapshotDigestHash = snapshot,
          carryDigestHash = carry,
          augmentationDigestHash = augmentation,
          bundleDigestHash = bundle
        )
    )

  test("same-ply parity report stays empty for MoveReview-only rows") {
    val report =
      buildSamePlyParityReport(
        List(
          paritySurface(
            "g1",
            10,
            SurfaceName.MoveReview
          ),
          paritySurface(
            "g2",
            20,
            SurfaceName.MoveReview,
            replayOutcome = Some("move_review_exact_factual")
          )
        )
      )

    assertEquals(report.summary.groupedPlies, 2)
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
        CommentaryPlayerQcSupport.MoveReviewOutputEntry(
          sampleId =
            "2024_03_03_4_1_bochnicka_vladimir_krivoborodov_egor_lichess_broadcast_master_classical_67:strategic_choice:14:moveReview",
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
          moveReviewBundleDigestHash = Some("b1")
        ),
        CommentaryPlayerQcSupport.MoveReviewOutputEntry(
          sampleId =
            "2024_03_03_4_1_bochnicka_vladimir_krivoborodov_egor_lichess_broadcast_master_classical_67:practical_simplification:20:moveReview",
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
          moveReviewBundleDigestHash = Some("b2")
        ),
        CommentaryPlayerQcSupport.MoveReviewOutputEntry(
          sampleId =
            "2024_03_03_4_1_bochnicka_vladimir_krivoborodov_egor_lichess_broadcast_master_classical_67:long_structural_squeeze:54:moveReview",
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
          moveReviewBundleDigestHash = Some("b3")
        )
      )
    ).fold(err => fail(err), identity)

    val gatedAfter =
      records.find(record =>
        record.sampleId.endsWith("practical_simplification:20:moveReview") && record.candidateLabel == "after"
      ).getOrElse(fail("missing gated after row"))

    assert(!gatedAfter.selection.moveAttributionGatePassed, clues(gatedAfter))
    assertEquals(gatedAfter.selection.usefulnessCredit, 0)
    assert(gatedAfter.selection.usefulnessRewardBlocked, clues(gatedAfter))
    assertEquals(gatedAfter.selection.thresholdVerdict, EvalVerdict.Review)
  }
