package lila.llm.tools

import munit.FunSuite

class RealPgnNarrativeEvalReportMergeTest extends FunSuite:

  private val exemplarKey =
    RealPgnNarrativeEvalRunner.PositiveCompensationExemplars.toList.sorted.head
  private val exemplarGameId = exemplarKey.takeWhile(_ != ':')
  private val exemplarPly = exemplarKey.dropWhile(_ != ':').drop(1).toInt

  private def focusMoment(
      ply: Int,
      compensationPosition: Boolean,
      exemplarVisible: Option[Boolean] = None
  ) =
    val resolvedExemplarVisible = exemplarVisible.getOrElse(compensationPosition)
    RealPgnNarrativeEvalRunner.FocusMomentReport(
      ply = ply,
      moveNumber = (ply + 1) / 2,
      side = if ply % 2 == 1 then "white" else "black",
      momentType = "InvestmentPivot",
      selectionKind = "key",
      dominantIdea = Some("fixed queenside targets"),
      secondaryIdea = None,
      campaignOwner = Some("White"),
      ownerMismatch = false,
      gameArcCompensationPosition = compensationPosition,
      bookmakerCompensationPosition = compensationPosition,
      compensationPosition = compensationPosition,
      exemplarVisible = resolvedExemplarVisible,
      gameArcCompensationSubtype = Some("queenside/target_fixing/intentionally_deferred/durable_pressure"),
      bookmakerCompensationSubtype = Some("queenside/target_fixing/intentionally_deferred/durable_pressure"),
      compensationSubtype = Some("queenside/target_fixing/intentionally_deferred/durable_pressure"),
      gameArcPreparationCompensationSubtype = Some("queenside/target_fixing/intentionally_deferred/durable_pressure"),
      bookmakerPreparationCompensationSubtype = Some("queenside/target_fixing/intentionally_deferred/durable_pressure"),
      gameArcPayoffCompensationSubtype = Some("queenside/target_fixing/intentionally_deferred/durable_pressure"),
      bookmakerPayoffCompensationSubtype = Some("queenside/target_fixing/intentionally_deferred/durable_pressure"),
      gameArcDisplaySubtypeSource = "path",
      bookmakerDisplaySubtypeSource = "path",
      activeCompensationMention = compensationPosition,
      bookmakerCompensationMention = compensationPosition,
      execution = Some("bishop toward c4 to lean on the fixed queenside targets"),
      objective = Some("queenside targets tied down before winning the material back"),
      focus = Some("queenside targets tied down before winning the material back"),
      gameArcNarrative = "Narrative",
      bookmakerCommentary = "Bookmaker",
      bookmakerSourceMode = "rule",
      activeNoteStatus = "rule",
      activeNote = Some("The compensation comes from queenside pressure against fixed targets."),
      probeRequestCount = 0,
      probeRefinementRequestCount = 0
    )

  private def game(id: String, moments: List[RealPgnNarrativeEvalRunner.FocusMomentReport]) =
    RealPgnNarrativeEvalRunner.GameReport(
      id = id,
      tier = "master_classical",
      family = "qgd_slav_qga",
      label = id,
      event = None,
      date = None,
      opening = None,
      result = None,
      totalPlies = 80,
      initialMomentCount = moments.size,
      refinedMomentCount = moments.size,
      strategicMomentCount = moments.size,
      threadCount = 1,
      activeNoteCount = moments.size,
      probeCandidateMoments = 0,
      probeCandidateRequests = 0,
      probeExecutedRequests = 0,
      probeUnsupportedRequests = 0,
      usedProbeRefinement = false,
      overallThemes = Nil,
      visibleMomentPlies = moments.map(_.ply),
      focusMoments = moments
    )

  private def report(
      title: String,
      games: List[RealPgnNarrativeEvalRunner.GameReport],
      exemplarEvaluated: Int,
      exemplarMisses: Int
  ) =
    RealPgnNarrativeEvalRunner.RunReport(
      generatedAt = "2026-03-24T00:00:00Z",
      corpusTitle = title,
      corpusAsOfDate = "2026-03-24",
      depth = 10,
      multiPv = 3,
      enginePath = "stockfish",
      summary = RealPgnNarrativeEvalRunner.buildSummary(games),
      signoff = RealPgnNarrativeEvalRunner.buildSignoff(games, Nil).copy(
        falseNegativeCount = exemplarMisses,
        positiveExemplarExpectedCount = RealPgnNarrativeEvalRunner.PositiveCompensationExemplars.size,
        positiveExemplarEvaluatedCount = exemplarEvaluated
      ),
      games = games
    )

  test("merge preserves shard-level positive exemplar coverage from the dedicated audited set") {
    val merged =
      RealPgnNarrativeEvalReportMerge.mergeReports(
        List(
          report(
            "Commentary Player QC Corpus [master_classical_000]",
            List(game(exemplarGameId, List(focusMoment(exemplarPly, compensationPosition = true)))),
            exemplarEvaluated = 6,
            exemplarMisses = 0
          ),
          report(
            "Commentary Player QC Corpus [master_classical_040]",
            List(game("other_game", List(focusMoment(40, compensationPosition = false)))),
            exemplarEvaluated = 6,
            exemplarMisses = 0
          )
        )
      )

    assertEquals(merged.signoff.positiveExemplarExpectedCount, 6)
    assertEquals(merged.signoff.positiveExemplarEvaluatedCount, 6)
    assertEquals(merged.signoff.falseNegativeCount, 0)
  }
