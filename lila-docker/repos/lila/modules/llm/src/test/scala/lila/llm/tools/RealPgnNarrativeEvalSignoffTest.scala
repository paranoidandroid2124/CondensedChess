package lila.llm.tools

import munit.FunSuite

class RealPgnNarrativeEvalSignoffTest extends FunSuite:

  private val exemplarKey =
    RealPgnNarrativeEvalRunner.PositiveCompensationExemplars.toList.sorted.head
  private val exemplarGameId = exemplarKey.takeWhile(_ != ':')
  private val exemplarPly = exemplarKey.dropWhile(_ != ':').drop(1).toInt

  private def focusMoment(ply: Int, compensationPosition: Boolean) =
    RealPgnNarrativeEvalRunner.FocusMomentReport(
      ply = ply,
      moveNumber = (ply + 1) / 2,
      side = if ply % 2 == 1 then "white" else "black",
      momentType = "SustainedPressure",
      selectionKind = "key",
      dominantIdea = Some("fixed queenside targets"),
      secondaryIdea = None,
      campaignOwner = Some("Black"),
      ownerMismatch = false,
      gameArcCompensationPosition = compensationPosition,
      bookmakerCompensationPosition = compensationPosition,
      compensationPosition = compensationPosition,
      gameArcCompensationSubtype = Some("queenside/target_fixing/delayed/durable_pressure"),
      bookmakerCompensationSubtype = Some("queenside/target_fixing/delayed/durable_pressure"),
      compensationSubtype = Some("queenside/target_fixing/delayed/durable_pressure"),
      gameArcPreparationCompensationSubtype = Some("queenside/target_fixing/delayed/durable_pressure"),
      bookmakerPreparationCompensationSubtype = Some("queenside/target_fixing/delayed/durable_pressure"),
      gameArcPayoffCompensationSubtype = Some("queenside/target_fixing/delayed/durable_pressure"),
      bookmakerPayoffCompensationSubtype = Some("queenside/target_fixing/delayed/durable_pressure"),
      gameArcDisplaySubtypeSource = "path",
      bookmakerDisplaySubtypeSource = "path",
      activeCompensationMention = compensationPosition,
      bookmakerCompensationMention = compensationPosition,
      execution = Some("queen toward b6"),
      objective = Some("queenside targets tied down before winning the material back"),
      focus = Some("queenside pressure"),
      gameArcNarrative = "Narrative",
      bookmakerCommentary = "Bookmaker",
      bookmakerSourceMode = "rule",
      activeNoteStatus = "rule",
      activeNote = Some("The compensation comes from queenside pressure against fixed targets."),
      probeRequestCount = 0,
      probeRefinementRequestCount = 0
    )

  private def game(id: String, moment: RealPgnNarrativeEvalRunner.FocusMomentReport) =
    RealPgnNarrativeEvalRunner.GameReport(
      id = id,
      tier = "master_classical",
      family = "benoni",
      label = id,
      event = None,
      date = None,
      opening = None,
      result = None,
      totalPlies = 60,
      initialMomentCount = 3,
      refinedMomentCount = 3,
      strategicMomentCount = 1,
      threadCount = 1,
      activeNoteCount = 1,
      probeCandidateMoments = 0,
      probeCandidateRequests = 0,
      probeExecutedRequests = 0,
      probeUnsupportedRequests = 0,
      usedProbeRefinement = false,
      overallThemes = Nil,
      visibleMomentPlies = List(moment.ply),
      focusMoments = List(moment)
    )

  test("signoff does not count absent positive exemplars that were not part of the run") {
    val report = RealPgnNarrativeEvalRunner.buildSignoff(games = Nil, negativeGuards = Nil)

    assertEquals(report.falseNegativeCount, 0)
    assertEquals(report.positiveExemplarEvaluatedCount, 0)
    assertEquals(report.positiveExemplarExpectedCount, 6)
  }

  test("signoff counts only evaluated positive exemplars from the dedicated fixture reports") {
    val exemplarReports = List(game(exemplarGameId, focusMoment(exemplarPly, compensationPosition = false)))

    val report =
      RealPgnNarrativeEvalRunner.buildSignoff(
        games = Nil,
        negativeGuards = Nil,
        positiveExemplarReports = exemplarReports
      )

    assertEquals(report.positiveExemplarEvaluatedCount, 1)
    assertEquals(report.falseNegativeCount, 1)
    assert(report.mustFixFailures.exists(_.category == "positive_exemplar_missed"), clue(report.mustFixFailures))
  }
