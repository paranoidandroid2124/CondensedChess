package lila.llm.tools

import munit.FunSuite
import lila.llm.*
import lila.llm.analysis.{ CommentaryEngine, StrategyPackSurface }

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

  private def maintenanceTrace(ply: Int): CommentaryEngine.TruthTraceMoment =
    CommentaryEngine.TruthTraceMoment(
      ply = ply,
      momentType = "SustainedPressure",
      selectionKind = "key",
      selectionLabel = Some("Key Moment"),
      selectionReason = None,
      traceSource = "canonical_internal",
      anchorMoment = true,
      bridgeCandidate = false,
      selectedBridge = false,
      finalInternal = true,
      visibleMoment = true,
      activeNoteMoment = true,
      wholeGamePromoted = false,
      strategicThreadId = None,
      moveClassification = None,
      playedMove = Some("a8b8"),
      verifiedBestMove = Some("a8b8"),
      truthClass = "Best",
      truthPhase = Some("CompensationMaintenance"),
      reasonFamily = "InvestmentSacrifice",
      ownershipRole = "MaintenanceEcho",
      visibilityRole = "SupportingVisible",
      surfaceMode = "MaintenancePreserve",
      exemplarRole = "NonExemplar",
      surfacedMoveOwnsTruth = false,
      compensationAllowed = false,
      compensationProseAllowed = false,
      benchmarkProseAllowed = false,
      verifiedPayoffAnchor = Some("queenside pressure"),
      chainKey = Some("black:queenside pressure"),
      moveQualityVerdict = "Best",
      cpLoss = 0,
      swingSeverity = 0,
      severityBand = "stable",
      investedMaterialCp = Some(100),
      beforeDeficit = 100,
      afterDeficit = 100,
      deficitDelta = 0,
      movingPieceValue = 0,
      capturedPieceValue = 0,
      sacrificeKind = None,
      valueDownCapture = false,
      increasesDeficit = false,
      recoversDeficit = false,
      overinvestment = false,
      uncompensatedLoss = false,
      forcedRecovery = false,
      createsFreshInvestment = false,
      maintainsInvestment = true,
      convertsInvestment = false,
      durablePressure = true,
      currentMoveEvidence = true,
      currentConcreteCarrier = true,
      freshCommitmentCandidate = false,
      ownerEligible = false,
      legacyVisibleOnly = false,
      maintenanceExemplarCandidate = true,
      evidenceProvenance = List("CurrentSemantic", "LegacyShell"),
      failureMode = "NoClearPlan",
      failureIntentConfidence = 0.0,
      failureIntentAnchor = None,
      failureInterpretationAllowed = false,
      rawDominantIdea = None,
      rawSecondaryIdea = None,
      rawExecution = Some("rook toward b4"),
      rawObjective = Some("queenside pressure"),
      rawFocus = Some("queenside pressure"),
      rawLongTermFocus = List("queenside pressure"),
      rawDirectionalTargets = List("pressure against b2"),
      rawPieceRoutes = List("rook toward b4 for queenside pressure"),
      rawPieceMoveRefs = Nil,
      rawCompensationSummary = Some("queenside pressure"),
      rawCompensationVectors = List("Line Pressure"),
      rawInvestedMaterial = Some(100),
      sanitizedDominantIdea = None,
      sanitizedSecondaryIdea = None,
      sanitizedExecution = Some("rook toward b4"),
      sanitizedObjective = Some("queenside pressure"),
      sanitizedFocus = Some("queenside pressure")
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

  test("signoff accepts exemplar-visible positive investment moments even without compensation-positive prose") {
    val exemplarReports =
      List(
        game(
          exemplarGameId,
          focusMoment(exemplarPly, compensationPosition = false).copy(exemplarVisible = true)
        )
      )

    val report =
      RealPgnNarrativeEvalRunner.buildSignoff(
        games = Nil,
        negativeGuards = Nil,
        positiveExemplarReports = exemplarReports
      )

    assertEquals(report.positiveExemplarEvaluatedCount, 1)
    assertEquals(report.falseNegativeCount, 0)
    assert(!report.mustFixFailures.exists(_.category == "positive_exemplar_missed"), clue(report.mustFixFailures))
  }

  test("maintenance exemplar candidate counts as exemplar-visible in calibration without widening the public role") {
    val strategyPack =
      StrategyPack(
        sideToMove = "black",
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
        directionalTargets = List(
          StrategyDirectionalTarget(
            targetId = "target_b2",
            ownerSide = "black",
            piece = "R",
            from = "b8",
            targetSquare = "b2",
            readiness = DirectionalTargetReadiness.Build,
            strategicReasons = List("queenside pressure")
          )
        ),
        longTermFocus = List("queenside pressure")
      )
    val surface = StrategyPackSurface.from(Some(strategyPack))
    val moment =
      GameChronicleMoment(
        momentId = "ply_36_maintenance",
        ply = 36,
        moveNumber = 18,
        side = "black",
        moveClassification = None,
        momentType = "SustainedPressure",
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
      RealPgnNarrativeEvalCalibration.exemplarEvalPosition(
        moment,
        surface,
        surface,
        Some(maintenanceTrace(moment.ply))
      ),
      clue(moment)
    )
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

  test("positive exemplar rerun corpus is derived from the truth inventory") {
    val exemplarGameIds =
      RealPgnNarrativeEvalRunner.PositiveCompensationExemplars.toList.sorted
        .map(_.takeWhile(_ != ':'))
        .distinct
    val inventory =
      RealPgnNarrativeEvalTruthInventoryBuilder.TruthInventory(
        generatedAt = "2026-03-25T00:00:00Z",
        asOfDate = "2026-03-25",
        title = "inventory",
        description = "inventory",
        sourceManifestRoot = "manifest",
        selection =
          RealPgnNarrativeEvalTruthInventoryBuilder.InventorySelection(
            practicalErrorCpLossThreshold = 70,
            severeErrorCpLossThreshold = 180,
            explicitProblemMomentTypes = Nil,
            explicitProblemClassifications = Nil,
            includesCanonicalPositiveExemplarGate = true,
            includesNegativeGuards = true
          ),
        summary =
          RealPgnNarrativeEvalTruthInventoryBuilder.InventorySummary(
            totalEntries = 0,
            totalGames = exemplarGameIds.size,
            moveClassificationCounts = Map.empty,
            momentTypeCounts = Map.empty,
            diagnosticTagCounts = Map.empty,
            explicitProblemClassifiedCount = 0,
            explicitProblemMomentCount = 0,
            highCpLossCount = 0,
            highCpLossUnclassifiedCount = 0
          ),
        games =
          exemplarGameIds.map(id =>
            RealPgnNarrativeEvalRunner.CorpusGame(
              id = id,
              tier = "positive_exemplar",
              family = "family",
              label = id,
              notes = Nil,
              expectedThemes = List("family"),
              pgn =
                """[Event "Sample"]
                  |[Site "?"]
                  |[Date "2026.03.25"]
                  |[Round "1"]
                  |[White "White"]
                  |[Black "Black"]
                  |[Result "*"]
                  |
                  |1. e4 e5 2. Nf3 Nc6 *
                  |""".stripMargin
            )
          ),
        entries = Nil
      )

    val corpus = RealPgnNarrativeEvalRunner.buildPositiveExemplarCorpus(inventory)

    assertEquals(corpus.games.map(_.id), exemplarGameIds)
  }
