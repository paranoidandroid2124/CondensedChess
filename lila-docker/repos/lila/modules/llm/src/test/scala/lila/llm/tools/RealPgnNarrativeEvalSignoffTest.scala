package lila.llm.tools

import munit.FunSuite
import lila.llm.*
import lila.llm.analysis.StrategyPackSurface

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
      exemplarVisible = compensationPosition,
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

  test("resolved path/payoff display divergence does not count toward divergence total") {
    val moment =
      focusMoment(exemplarPly, compensationPosition = true).copy(
        gameArcPreparationCompensationSubtype = Some("queenside/line_occupation/intentionally_deferred/durable_pressure"),
        gameArcPayoffCompensationSubtype = Some("queenside/target_fixing/intentionally_deferred/durable_pressure"),
        gameArcCompensationSubtype = Some("queenside/line_occupation/intentionally_deferred/durable_pressure"),
        gameArcDisplaySubtypeSource = "path"
      )

    val report = RealPgnNarrativeEvalRunner.buildSignoff(games = List(game(exemplarGameId, moment)), negativeGuards = Nil)

    assertEquals(report.pathVsPayoffDivergenceCount, 0)
  }

  test("truth-bound investment stays compensation-positive in evaluation calibration") {
    val strategyPack =
      StrategyPack(
        sideToMove = "black",
        strategicIdeas = List(
          StrategyIdeaSignal(
            ideaId = "idea_bfile_fixation",
            ownerSide = "black",
            kind = StrategicIdeaKind.TargetFixing,
            group = "slow_structural",
            readiness = StrategicIdeaReadiness.Build,
            focusSquares = List("b2", "b3"),
            focusFiles = List("b"),
            focusZone = Some("queenside"),
            beneficiaryPieces = List("R"),
            confidence = 0.88
          )
        ),
        pieceRoutes = List(
          StrategyPieceRoute(
            ownerSide = "black",
            piece = "R",
            from = "a8",
            route = List("a8", "b8", "b4"),
            purpose = "queenside pressure",
            strategicFit = 0.84,
            tacticalSafety = 0.78,
            surfaceConfidence = 0.80,
            surfaceMode = RouteSurfaceMode.Toward
          )
        ),
        longTermFocus = List("fix the queenside targets before recovering the pawn"),
        signalDigest = Some(
          NarrativeSignalDigest(
            compensation = Some("return vector through line pressure and delayed recovery"),
            compensationVectors = List("Line Pressure (0.7)", "Delayed Recovery (0.6)"),
            investedMaterial = Some(100),
            dominantIdeaKind = Some(StrategicIdeaKind.TargetFixing),
            dominantIdeaGroup = Some("slow_structural"),
            dominantIdeaReadiness = Some(StrategicIdeaReadiness.Build),
            dominantIdeaFocus = Some("b2, b3")
          )
        )
      )
    val surface = StrategyPackSurface.from(Some(strategyPack))

    assert(surface.compensationPosition, clue(surface))
    assert(StrategyPackSurface.strictCompensationSubtypeLabel(surface).nonEmpty, clue(surface))

    val moment =
      GameChronicleMoment(
        momentId = "ply_34_investmentpivot",
        ply = 34,
        moveNumber = 17,
        side = "white",
        moveClassification = Some("WinningInvestment"),
        momentType = "InvestmentPivot",
        fen = "4k3/8/8/8/8/8/8/4K3 w - - 0 1",
        narrative = "Narrative",
        concepts = Nil,
        variations = Nil,
        cpBefore = 82,
        cpAfter = 82,
        mateBefore = None,
        mateAfter = None,
        wpaSwing = Some(0.0),
        strategicSalience = Some("High"),
        transitionType = None,
        transitionConfidence = None,
        activePlan = None,
        topEngineMove = None,
        collapse = None,
        strategyPack = Some(strategyPack)
      )

    assert(
      RealPgnNarrativeEvalCalibration.compensationEvalPosition(
        moment,
        surface,
        surface,
        playedMove = Some("a8b8")
      ),
      clue(moment)
    )
  }

  test("investment-pivot moment type remains compensation-positive even when the explicit classification is absent") {
    val strategyPack =
      StrategyPack(
        sideToMove = "black",
        strategicIdeas = List(
          StrategyIdeaSignal(
            ideaId = "idea_bfile_fixation",
            ownerSide = "black",
            kind = StrategicIdeaKind.TargetFixing,
            group = "slow_structural",
            readiness = StrategicIdeaReadiness.Build,
            focusSquares = List("b2", "b3"),
            focusFiles = List("b"),
            focusZone = Some("queenside"),
            beneficiaryPieces = List("R"),
            confidence = 0.88
          )
        ),
        pieceRoutes = List(
          StrategyPieceRoute(
            ownerSide = "black",
            piece = "R",
            from = "a8",
            route = List("a8", "b8", "b4"),
            purpose = "queenside pressure",
            strategicFit = 0.84,
            tacticalSafety = 0.78,
            surfaceConfidence = 0.80,
            surfaceMode = RouteSurfaceMode.Toward
          )
        ),
        longTermFocus = List("fix the queenside targets before recovering the pawn"),
        signalDigest = Some(
          NarrativeSignalDigest(
            compensation = Some("return vector through line pressure and delayed recovery"),
            compensationVectors = List("Line Pressure (0.7)", "Delayed Recovery (0.6)"),
            investedMaterial = Some(100),
            dominantIdeaKind = Some(StrategicIdeaKind.TargetFixing),
            dominantIdeaGroup = Some("slow_structural"),
            dominantIdeaReadiness = Some(StrategicIdeaReadiness.Build),
            dominantIdeaFocus = Some("b2, b3")
          )
        )
      )
    val surface = StrategyPackSurface.from(Some(strategyPack))

    val moment =
      GameChronicleMoment(
        momentId = "ply_34_investmentpivot",
        ply = 34,
        moveNumber = 17,
        side = "white",
        moveClassification = None,
        momentType = "InvestmentPivot",
        fen = "4k3/8/8/8/8/8/8/4K3 w - - 0 1",
        narrative = "Narrative",
        concepts = Nil,
        variations = Nil,
        cpBefore = 82,
        cpAfter = 82,
        mateBefore = None,
        mateAfter = None,
        wpaSwing = Some(0.0),
        strategicSalience = Some("High"),
        transitionType = None,
        transitionConfidence = None,
        activePlan = None,
        topEngineMove = None,
        collapse = None,
        strategyPack = Some(strategyPack)
      )

    assert(
      RealPgnNarrativeEvalCalibration.compensationEvalPosition(
        moment,
        surface,
        surface,
        playedMove = Some("a8b8")
      ),
      clue(moment)
    )
  }
