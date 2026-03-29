package lila.llm.analysis

import munit.FunSuite
import lila.llm.{ DirectionalTargetReadiness, GameChronicleMoment, NarrativeSignalDigest, RouteSurfaceMode, StrategicIdeaGroup, StrategicIdeaKind, StrategicIdeaReadiness, StrategyDirectionalTarget, StrategyPack, StrategyPieceMoveRef, StrategyPieceRoute }
import lila.llm.model.*
import lila.llm.model.strategic.{ EngineEvidence, PvMove, VariationLine }

class PlayerFacingTruthModePolicyTest extends FunSuite:

  private def baseCtx(): NarrativeContext =
    NarrativeContext(
      fen = "r2q1rk1/pp2bppp/2np1n2/2p1p3/2P1P3/2NP1NP1/PP2QPBP/R1B2RK1 w - - 0 10",
      header = ContextHeader("Middlegame", "Normal", "StyleChoice", "Medium", "ExplainPlan"),
      ply = 20,
      playedMove = Some("e2e3"),
      playedSan = Some("Qe2"),
      summary = NarrativeSummary("Central restraint", None, "StyleChoice", "Maintain", "0.20"),
      threats = ThreatTable(Nil, Nil),
      pawnPlay = PawnPlayTable(false, None, "Low", "Maintain", "Quiet", "Background", None, false, "quiet"),
      plans = PlanTable(Nil, Nil),
      delta = None,
      phase = PhaseContext("Middlegame", "Normal middlegame"),
      candidates = Nil,
      renderMode = NarrativeRenderMode.Bookmaker
    )

  private def truthContract(
      truthClass: DecisiveTruthClass,
      reasonFamily: DecisiveReasonFamily
  ): DecisiveTruthContract =
    DecisiveTruthContract(
      playedMove = Some("d1d5"),
      verifiedBestMove = Some("d1d5"),
      truthClass = truthClass,
      cpLoss = if truthClass == DecisiveTruthClass.Blunder then 280 else 0,
      swingSeverity = if truthClass == DecisiveTruthClass.Blunder then 280 else 0,
      reasonFamily = reasonFamily,
      allowConcreteBenchmark = false,
      chosenMatchesBest = true,
      compensationAllowed = false,
      truthPhase = None,
      ownershipRole = TruthOwnershipRole.NoneRole,
      visibilityRole = TruthVisibilityRole.PrimaryVisible,
      surfaceMode =
        if truthClass == DecisiveTruthClass.Blunder then TruthSurfaceMode.FailureExplain
        else TruthSurfaceMode.Neutral,
      exemplarRole = TruthExemplarRole.NonExemplar,
      surfacedMoveOwnsTruth = false,
      verifiedPayoffAnchor = None,
      compensationProseAllowed = false,
      benchmarkProseAllowed = false,
      investmentTruthChainKey = None,
      maintenanceExemplarCandidate = false,
      failureMode =
        if truthClass == DecisiveTruthClass.Blunder then FailureInterpretationMode.TacticalRefutation
        else FailureInterpretationMode.NoClearPlan,
      failureIntentConfidence = 0.0,
      failureIntentAnchor = None,
      failureInterpretationAllowed = false
    )

  test("quiet shell-only support resolves to Minimal") {
    val ctx = baseCtx().copy(strategicSalience = lila.llm.model.strategic.StrategicSalience.Low)
    val pack =
      Some(
        StrategyPack(
          sideToMove = "white",
          strategicIdeas = List(
            lila.llm.StrategyIdeaSignal(
              ideaId = "shell_only",
              ownerSide = "white",
              kind = StrategicIdeaKind.TargetFixing,
              group = StrategicIdeaGroup.StructuralChange,
              readiness = StrategicIdeaReadiness.Build,
              confidence = 0.62
            )
          ),
          longTermFocus = List("Carlsbad pressure")
        )
      )

    assertEquals(
      PlayerFacingTruthModePolicy.classify(ctx, pack, None),
      PlayerFacingTruthMode.Minimal
    )
  }

  test("forcing truth contracts resolve to Tactical") {
    val ctx = baseCtx()
    assertEquals(
      PlayerFacingTruthModePolicy.classify(
        ctx,
        None,
        Some(truthContract(DecisiveTruthClass.Blunder, DecisiveReasonFamily.TacticalRefutation))
      ),
      PlayerFacingTruthMode.Tactical
    )
  }

  test("criticality and cp swing alone do not resolve to Tactical") {
    val ctx =
      baseCtx().copy(
        header = ContextHeader("Middlegame", "Critical", "OnlyMove", "High", "ExplainTactics"),
        meta = Some(
          MetaSignals(
            choiceType = ChoiceType.OnlyMove,
            targets = Targets(Nil, Nil),
            planConcurrency = PlanConcurrency("Attack", None, "independent"),
            errorClass = Some(
              ErrorClassification(
                isTactical = true,
                missedMotifs = List("fork"),
                errorSummary = "generic tactical pressure"
              )
            )
          )
        )
      )

    assertEquals(
      PlayerFacingTruthModePolicy.classify(ctx, None, None),
      PlayerFacingTruthMode.Minimal
    )

    val moment =
      GameChronicleMoment(
        momentId = "ply_22_advantageswing",
        ply = 22,
        moveNumber = 11,
        side = "white",
        moveClassification = Some("mistake"),
        momentType = "AdvantageSwing",
        fen = baseCtx().fen,
        narrative = "The move keeps some pressure.",
        concepts = Nil,
        variations = Nil,
        cpBefore = -20,
        cpAfter = 220,
        mateBefore = None,
        mateAfter = None,
        wpaSwing = None,
        strategicSalience = Some("High"),
        transitionType = None,
        transitionConfidence = None,
        activePlan = None,
        topEngineMove = None,
        collapse = None,
        strategyPack = None,
        signalDigest = None
      )

    assertEquals(PlayerFacingTruthModePolicy.classify(moment), PlayerFacingTruthMode.Minimal)
  }

  test("concrete route and target evidence resolves to Strategic") {
    val ctx =
      baseCtx().copy(
        decision = Some(
          DecisionRationale(
            focalPoint = Some(TargetSquare("e5")),
            logicSummary = "Pressure against e5 is the concrete point.",
            delta = PVDelta(
              resolvedThreats = Nil,
              newOpportunities = List("e5 pressure"),
              planAdvancements = List("queen and rook coordination"),
              concessions = Nil
            ),
            confidence = ConfidenceLevel.Probe
          )
        )
      )
    val pack =
      Some(
        StrategyPack(
          sideToMove = "white",
          pieceRoutes = List(
            StrategyPieceRoute(
              ownerSide = "white",
              piece = "Q",
              from = "d1",
              route = List("d1", "f3", "f5"),
              purpose = "kingside pressure",
              strategicFit = 0.86,
              tacticalSafety = 0.8,
              surfaceConfidence = 0.85,
              surfaceMode = RouteSurfaceMode.Exact,
              evidence = List("probe")
            )
          ),
          directionalTargets = List(
            StrategyDirectionalTarget(
              targetId = "target_e5",
              ownerSide = "white",
              piece = "Q",
              from = "d1",
              targetSquare = "e5",
              readiness = DirectionalTargetReadiness.Build,
              strategicReasons = List("pressure on e5"),
              evidence = List("probe")
            )
          ),
          signalDigest = Some(NarrativeSignalDigest(decision = Some("pressure on e5")))
        )
      )

    assertEquals(
      PlayerFacingTruthModePolicy.classify(ctx, pack, None),
      PlayerFacingTruthMode.Strategic
    )
  }

  test("plan-advance delta claim remains strategic when the move makes a concrete break available") {
    val ctx =
      baseCtx().copy(
        strategicPlanExperiments = List(
          StrategicPlanExperiment(
            planId = "central_space_bind",
            themeL1 = "pawn_break_preparation",
            evidenceTier = "evidence_backed",
            supportProbeCount = 1,
            refuteProbeCount = 0,
            bestReplyStable = true,
            futureSnapshotAligned = true,
            counterBreakNeutralized = false,
            moveOrderSensitive = false,
            experimentConfidence = 0.86
          )
        ),
        decision = Some(
          DecisionRationale(
            focalPoint = None,
            logicSummary = "Keeps the center stable -> improves coordination",
            delta = PVDelta(
              resolvedThreats = List("central tension"),
              newOpportunities = List("e4 push"),
              planAdvancements = List("queen and rook coordination"),
              concessions = Nil
            ),
            confidence = ConfidenceLevel.Probe
          )
        )
      )
    val pack =
      Some(
        StrategyPack(
          sideToMove = "white",
          pieceRoutes = List(
            StrategyPieceRoute(
              ownerSide = "white",
              piece = "Q",
              from = "d1",
              route = List("d1", "e2", "e4"),
              purpose = "support the e4 break",
              strategicFit = 0.84,
              tacticalSafety = 0.78,
              surfaceConfidence = 0.88,
              surfaceMode = RouteSurfaceMode.Exact,
              evidence = List("probe")
            )
          ),
          directionalTargets = List(
            StrategyDirectionalTarget(
              targetId = "target_e4",
              ownerSide = "white",
              piece = "Q",
              from = "d1",
              targetSquare = "e4",
              readiness = DirectionalTargetReadiness.Build,
              strategicReasons = List("support the e4 break"),
              evidence = List("probe")
            )
          ),
          signalDigest = Some(NarrativeSignalDigest(decision = Some("support the e4 break")))
        )
      )

    assertEquals(
      PlayerFacingTruthModePolicy.classify(ctx, pack, None),
      PlayerFacingTruthMode.Strategic
    )
    assert(
      PlayerFacingTruthModePolicy.allowsStrategicClaimText(
        "Qe2 keeps the e4 push available while covering the c4 pawn.",
        ctx,
        pack,
        None
      )
    )
  }

  test("draw results disable whole-game decisive narration") {
    assert(!PlayerFacingTruthModePolicy.allowWholeGameDecisiveNarrative("1/2-1/2"))
    assert(PlayerFacingTruthModePolicy.allowWholeGameDecisiveNarrative("1-0"))
  }

  test("quiet chronicle moments suppress active notes") {
    val moment =
      GameChronicleMoment(
        momentId = "ply_20_quiet",
        ply = 20,
        moveNumber = 10,
        side = "white",
        moveClassification = None,
        momentType = "StrategicBridge",
        fen = baseCtx().fen,
        narrative = "A quiet move.",
        concepts = Nil,
        variations = List(VariationLine(List("Qe2", "...Re8"), 20, None, 18)),
        cpBefore = 20,
        cpAfter = 20,
        mateBefore = None,
        mateAfter = None,
        wpaSwing = None,
        strategicSalience = Some("Low"),
        transitionType = None,
        transitionConfidence = None,
        activePlan = None,
        topEngineMove = None,
        collapse = None,
        strategyPack = None,
        signalDigest = None
      )

    assert(!PlayerFacingTruthModePolicy.allowsActiveNote(moment))
  }

  test("tactical sacrifice with immediate recoup resolves to Tactical") {
    val ctx =
      baseCtx().copy(
        semantic = Some(
          SemanticSection(
            structuralWeaknesses = Nil,
            pieceActivity = Nil,
            positionalFeatures = Nil,
            compensation = Some(
              CompensationInfo(
                investedMaterial = 300,
                returnVector = Map("attack" -> 0.9),
                expiryPly = Some(4),
                conversionPlan = "recover material by force"
              )
            ),
            endgameFeatures = None,
            practicalAssessment = None,
            preventedPlans = Nil,
            conceptSummary = Nil
          )
        ),
        engineEvidence = Some(
          EngineEvidence(
            depth = 18,
            variations = List(
              VariationLine(
                moves = List("b5d7", "e8d7", "d1g4"),
                scoreCp = 75,
                depth = 18,
                parsedMoves = List(
                  PvMove("b5d7", "Bxd7+", "b5", "d7", "B", isCapture = true, capturedPiece = Some("n"), givesCheck = true),
                  PvMove("e8d7", "Kxd7", "e8", "d7", "K", isCapture = true, capturedPiece = Some("b"), givesCheck = false),
                  PvMove("d1g4", "Qg4+", "d1", "g4", "Q", isCapture = false, capturedPiece = None, givesCheck = true)
                )
              )
            )
          )
        )
      )
    val contract =
      truthContract(DecisiveTruthClass.CompensatedInvestment, DecisiveReasonFamily.InvestmentSacrifice).copy(
        surfaceMode = TruthSurfaceMode.InvestmentExplain,
        compensationProseAllowed = true
      )

    assertEquals(
      PlayerFacingTruthModePolicy.classify(ctx, None, Some(contract)),
      PlayerFacingTruthMode.Tactical
    )
  }

  test("strategic sacrifice without immediate recoup resolves to Strategic") {
    val ctx =
      baseCtx().copy(
        semantic = Some(
          SemanticSection(
            structuralWeaknesses = Nil,
            pieceActivity = Nil,
            positionalFeatures = Nil,
            compensation = Some(
              CompensationInfo(
                investedMaterial = 300,
                returnVector = Map("initiative" -> 0.8),
                expiryPly = Some(8),
                conversionPlan = "long-term pressure"
              )
            ),
            endgameFeatures = None,
            practicalAssessment = None,
            preventedPlans = Nil,
            conceptSummary = Nil
          )
        ),
        engineEvidence = Some(
          EngineEvidence(
            depth = 18,
            variations = List(
              VariationLine(
                moves = List("g2g4", "h7h6", "a2a3", "a7a6"),
                scoreCp = 20,
                depth = 18,
                parsedMoves = List(
                  PvMove("g2g4", "g4", "g2", "g4", "P", isCapture = false, capturedPiece = None, givesCheck = false),
                  PvMove("h7h6", "h6", "h7", "h6", "P", isCapture = false, capturedPiece = None, givesCheck = false),
                  PvMove("a2a3", "a3", "a2", "a3", "P", isCapture = false, capturedPiece = None, givesCheck = false),
                  PvMove("a7a6", "a6", "a7", "a6", "P", isCapture = false, capturedPiece = None, givesCheck = false)
                )
              )
            )
          )
        )
      )
    val contract =
      truthContract(DecisiveTruthClass.CompensatedInvestment, DecisiveReasonFamily.InvestmentSacrifice).copy(
        surfaceMode = TruthSurfaceMode.InvestmentExplain,
        verifiedPayoffAnchor = Some("pressure on g7"),
        compensationProseAllowed = true
      )
    val pack =
      Some(
        StrategyPack(
          sideToMove = "white",
          pieceRoutes = List(
            StrategyPieceRoute(
              ownerSide = "white",
              piece = "Q",
              from = "d1",
              route = List("d1", "g4", "h5"),
              purpose = "pressure on g7",
              strategicFit = 0.84,
              tacticalSafety = 0.72,
              surfaceConfidence = 0.88,
              surfaceMode = RouteSurfaceMode.Exact,
              evidence = List("probe")
            )
          ),
          directionalTargets = List(
            StrategyDirectionalTarget(
              targetId = "target_g7",
              ownerSide = "white",
              piece = "Q",
              from = "d1",
              targetSquare = "g7",
              readiness = DirectionalTargetReadiness.Build,
              strategicReasons = List("pressure on g7"),
              evidence = List("probe")
            )
          ),
          signalDigest = Some(NarrativeSignalDigest(compensation = Some("pressure on g7")))
        )
      )

    assertEquals(
      PlayerFacingTruthModePolicy.classify(ctx, pack, Some(contract)),
      PlayerFacingTruthMode.Strategic
    )
  }

  test("unsupported sacrifice romance resolves to Minimal") {
    val ctx =
      baseCtx().copy(
        semantic = Some(
          SemanticSection(
            structuralWeaknesses = Nil,
            pieceActivity = Nil,
            positionalFeatures = Nil,
            compensation = Some(
              CompensationInfo(
                investedMaterial = 300,
                returnVector = Map.empty,
                expiryPly = None,
                conversionPlan = ""
              )
            ),
            endgameFeatures = None,
            practicalAssessment = None,
            preventedPlans = Nil,
            conceptSummary = Nil
          )
        ),
        engineEvidence = Some(
          EngineEvidence(
            depth = 16,
            variations = List(VariationLine(List("g2g4", "h7h6"), scoreCp = 0, depth = 16))
          )
        )
      )

    assertEquals(
      PlayerFacingTruthModePolicy.classify(ctx, None, None),
      PlayerFacingTruthMode.Minimal
    )
  }

  test("main-path exchange forcing requires both an anchored line and move-linked exchange evidence") {
    val ctx =
      baseCtx().copy(
        decision = Some(
          DecisionRationale(
            focalPoint = Some(TargetSquare("c4")),
            logicSummary = "The exchange theme is in the air.",
            delta = PVDelta(
              resolvedThreats = Nil,
              newOpportunities = List("exchange on c4"),
              planAdvancements = Nil,
              concessions = Nil
            ),
            confidence = ConfidenceLevel.Probe
          )
        )
      )
    val pack =
      Some(
        StrategyPack(
          sideToMove = "white",
          pieceMoveRefs = List(
            StrategyPieceMoveRef(
              ownerSide = "white",
              piece = "B",
              from = "g2",
              target = "c4",
              idea = "exchange on c4",
              tacticalTheme = Some("exchange"),
              evidence = List("probe")
            )
          ),
          directionalTargets = List(
            StrategyDirectionalTarget(
              targetId = "target_c4",
              ownerSide = "white",
              piece = "B",
              from = "g2",
              targetSquare = "c4",
              readiness = DirectionalTargetReadiness.Build,
              strategicReasons = List("exchange on c4"),
              evidence = List("probe")
            )
          )
        )
      )

    assertEquals(
      PlayerFacingTruthModePolicy.mainPathMoveDeltaEvidence(
        ctx,
        StrategyPackSurface.from(pack),
        None
      ),
      None
    )

    val exchangeCtx =
      ctx.copy(
        engineEvidence = Some(
          EngineEvidence(
            depth = 18,
            variations = List(
              VariationLine(
                moves = List("g2c4", "d5c4", "d1d8"),
                scoreCp = 34,
                depth = 18,
                parsedMoves = List(
                  PvMove("g2c4", "Bxc4", "g2", "c4", "B", isCapture = true, capturedPiece = Some("p"), givesCheck = false),
                  PvMove("d5c4", "dxc4", "d5", "c4", "P", isCapture = true, capturedPiece = Some("b"), givesCheck = false),
                  PvMove("d1d8", "Qd8+", "d1", "d8", "Q", isCapture = false, capturedPiece = None, givesCheck = true)
                )
              )
            )
          )
        )
      )

    val provingOnlyCtx =
      baseCtx().copy(
        engineEvidence = Some(
          EngineEvidence(
            depth = 18,
            variations = List(
              VariationLine(
                moves = List("g2c4", "d5c4", "d1d8"),
                scoreCp = 34,
                depth = 18,
                parsedMoves = List(
                  PvMove("g2c4", "Bxc4", "g2", "c4", "B", isCapture = true, capturedPiece = Some("p"), givesCheck = false),
                  PvMove("d5c4", "dxc4", "d5", "c4", "P", isCapture = true, capturedPiece = Some("b"), givesCheck = false),
                  PvMove("d1d8", "Qd8+", "d1", "d8", "Q", isCapture = false, capturedPiece = None, givesCheck = true)
                )
              )
            )
          )
        )
      )

    assertEquals(
      PlayerFacingTruthModePolicy.mainPathMoveDeltaEvidence(
        provingOnlyCtx,
        StrategyPackSurface.from(pack),
        None
      ),
      None
    )

    assertEquals(
      PlayerFacingTruthModePolicy.mainPathMoveDeltaEvidence(
        exchangeCtx,
        StrategyPackSurface.from(pack),
        None
      ).map(_.deltaClass),
      Some(PlayerFacingMoveDeltaClass.ExchangeForcing)
    )

    val directCaptureCtx =
      baseCtx().copy(
        playedMove = Some("g2c4"),
        playedSan = Some("Bxc4"),
        engineEvidence = exchangeCtx.engineEvidence
      )

    assertEquals(
      PlayerFacingTruthModePolicy.mainPathMoveDeltaEvidence(
        directCaptureCtx,
        StrategyPackSurface.from(pack),
        None
      ),
      None
    )
  }

  test("main-path resource removal requires a specific prevented resource") {
    val basePrevented =
      PreventedPlanInfo(
        planId = "Queenside counterplay",
        deniedSquares = List("c4"),
        breakNeutralized = None,
        mobilityDelta = -1,
        counterplayScoreDrop = 80,
        preventedThreatType = None
      )
    val pack =
      Some(
        StrategyPack(
          sideToMove = "white",
          directionalTargets = List(
            StrategyDirectionalTarget(
              targetId = "target_c4",
              ownerSide = "white",
              piece = "N",
              from = "e3",
              targetSquare = "c4",
              readiness = DirectionalTargetReadiness.Build,
              strategicReasons = List("control c4"),
              evidence = List("probe")
            )
          ),
          signalDigest = Some(NarrativeSignalDigest(decision = Some("control c4")))
        )
      )

    val genericCtx =
      baseCtx().copy(
        semantic = Some(
          SemanticSection(
            structuralWeaknesses = Nil,
            pieceActivity = Nil,
            positionalFeatures = Nil,
            compensation = None,
            endgameFeatures = None,
            practicalAssessment = None,
            preventedPlans = List(basePrevented),
            conceptSummary = Nil
          )
        )
      )

    assertEquals(
      PlayerFacingTruthModePolicy.mainPathMoveDeltaEvidence(
        genericCtx,
        StrategyPackSurface.from(pack),
        None
      ).map(_.deltaClass),
      Some(PlayerFacingMoveDeltaClass.CounterplayReduction)
    )

    val specificCtx =
      genericCtx.copy(
        semantic = genericCtx.semantic.map(_.copy(
          preventedPlans = List(
            basePrevented.copy(
              preventedThreatType = Some("entry_square"),
              citationLine = Some("...Qa5 can no longer enter on c4.")
            )
          )
        )),
        meta = Some(
          MetaSignals(
            choiceType = ChoiceType.NarrowChoice,
            targets = Targets(Nil, Nil),
            planConcurrency = PlanConcurrency("Attack", None, "independent"),
            whyNot = Some("Black can no longer use c4 as an entry square.")
          )
        )
      )

    assertEquals(
      PlayerFacingTruthModePolicy.mainPathMoveDeltaEvidence(
        specificCtx,
        StrategyPackSurface.from(pack),
        None
      ).map(_.deltaClass),
      Some(PlayerFacingMoveDeltaClass.ResourceRemoval)
    )
  }
