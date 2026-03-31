package lila.llm.analysis

import munit.FunSuite
import lila.llm.*
import lila.llm.model.*
import lila.llm.analysis.DecisiveTruth.toContract
import lila.llm.model.strategic.{ EngineEvidence, VariationLine }

class DecisiveTruthContractTest extends FunSuite:

  private def semanticSection(
      compensation: Option[CompensationInfo] = None,
      afterCompensation: Option[CompensationInfo] = None
  ): SemanticSection =
    SemanticSection(
      structuralWeaknesses = Nil,
      pieceActivity = Nil,
      positionalFeatures = Nil,
      compensation = compensation,
      endgameFeatures = None,
      practicalAssessment = None,
      preventedPlans = Nil,
      conceptSummary = Nil,
      afterCompensation = afterCompensation
    )

  private def ctx(
      playedMove: String,
      playedSan: String,
      fen: String = "4k3/8/8/8/8/8/8/4K3 w - - 0 1",
      semantic: Option[SemanticSection] = None,
      criticality: String = "Critical",
      choiceType: String = "StyleChoice",
      engineVariations: List[VariationLine] = Nil
  ): NarrativeContext =
    NarrativeContext(
      fen = fen,
      header = ContextHeader("Middlegame", criticality, choiceType, "Medium", "ExplainPlan"),
      ply = 24,
      playedMove = Some(playedMove),
      playedSan = Some(playedSan),
      summary = NarrativeSummary("Central tension", None, "StyleChoice", "Maintain", "0.00"),
      threats = ThreatTable(Nil, Nil),
      pawnPlay = PawnPlayTable(false, None, "Low", "Maintain", "Quiet", "Background", None, false, "quiet"),
      plans = PlanTable(Nil, Nil),
      delta = None,
      phase = PhaseContext("Middlegame", "Balanced middlegame"),
      candidates = Nil,
      engineEvidence = Option.when(engineVariations.nonEmpty)(EngineEvidence(depth = 18, variations = engineVariations)),
      semantic = semantic,
      renderMode = NarrativeRenderMode.FullGame
    )

  private def comparison(
      chosenMove: String,
      engineBestMove: Option[String],
      cpLoss: Int,
      deferredMove: Option[String] = None,
      deferredReason: Option[String] = None,
      chosenMatchesBest: Boolean = false
  ): DecisionComparison =
    DecisionComparison(
      chosenMove = Some(chosenMove),
      engineBestMove = engineBestMove,
      engineBestScoreCp = Some(220),
      engineBestPv = engineBestMove.toList,
      cpLossVsChosen = Some(cpLoss),
      deferredMove = deferredMove,
      deferredReason = deferredReason,
      deferredSource = Some("heuristic"),
      evidence = Some("engine evidence"),
      practicalAlternative = true,
      chosenMatchesBest = chosenMatchesBest
    )

  private def minimalAnalysisData(ply: Int): ExtendedAnalysisData =
    ExtendedAnalysisData(
      fen = "4k3/8/8/8/8/8/8/4K3 w - - 0 1",
      nature = PositionNature(NatureType.Dynamic, 0.5, 0.5, "Dynamic position"),
      motifs = Nil,
      plans = Nil,
      preventedPlans = Nil,
      pieceActivity = Nil,
      structuralWeaknesses = Nil,
      compensation = None,
      endgameFeatures = None,
      practicalAssessment = None,
      prevMove = None,
      ply = ply,
      evalCp = 0,
      isWhiteToMove = true
    )

  private def arcMoment(
      ply: Int,
      momentType: String,
      moveClassification: Option[String],
      truthPhase: Option[String],
      surfacedMoveOwnsTruth: Boolean,
      verifiedPayoffAnchor: Option[String],
      benchmarkProseAllowed: Boolean,
      investmentTruthChainKey: Option[String]
  ): GameArcMoment =
    GameArcMoment(
      ply = ply,
      momentType = momentType,
      narrative = "Narrative",
      analysisData = minimalAnalysisData(ply),
      moveClassification = moveClassification,
      cpBefore = Some(0),
      cpAfter = Some(0),
      truthPhase = truthPhase,
      surfacedMoveOwnsTruth = surfacedMoveOwnsTruth,
      verifiedPayoffAnchor = verifiedPayoffAnchor,
      benchmarkProseAllowed = benchmarkProseAllowed,
      investmentTruthChainKey = investmentTruthChainKey
    )

  private def chronicleMoment(
      ply: Int,
      momentType: String,
      moveClassification: Option[String] = None,
      transitionType: Option[String] = None
  ): GameChronicleMoment =
    GameChronicleMoment(
      momentId = s"ply_$ply",
      ply = ply,
      moveNumber = (ply + 1) / 2,
      side = if ply % 2 == 1 then "white" else "black",
      moveClassification = moveClassification,
      momentType = momentType,
      fen = "4k3/8/8/8/8/8/8/4K3 w - - 0 1",
      narrative = "Narrative",
      selectionKind = "key",
      selectionLabel = Some("Key Moment"),
      concepts = Nil,
      variations = Nil,
      cpBefore = 0,
      cpAfter = 0,
      mateBefore = None,
      mateAfter = None,
      wpaSwing = None,
      strategicSalience = Some("High"),
      transitionType = transitionType,
      transitionConfidence = None,
      activePlan = None,
      topEngineMove = None,
      collapse = None,
      strategyPack = None
    )

  private def compensationInfo(
      investedMaterial: Int,
      conversionPlan: String,
      returnVector: Map[String, Double]
  ): CompensationInfo =
    CompensationInfo(
      investedMaterial = investedMaterial,
      returnVector = returnVector,
      expiryPly = None,
      conversionPlan = conversionPlan
    )

  test("verified-best played move is downgraded to best and loses benchmark naming") {
    val raw =
      comparison(
        chosenMove = "Rxd5",
        engineBestMove = Some("Rxd5"),
        cpLoss = 180,
        deferredMove = Some("h3"),
        deferredReason = Some("it keeps the structure tighter"),
        chosenMatchesBest = false
      )

    val contract = DecisiveTruth.derive(
      ctx = ctx("d1d5", "Rxd5"),
      comparisonOverride = Some(raw)
    )
    val sanitized = DecisiveTruth.sanitizeDecisionComparison(Some(raw), contract).getOrElse(fail("missing comparison"))

    assertEquals(contract.truthClass, DecisiveTruthClass.Best)
    assertEquals(contract.chosenMatchesBest, true)
    assertEquals(contract.cpLoss, 0)
    assertEquals(contract.benchmarkProseAllowed, false)
    assertEquals(contract.benchmarkMove, None)
    assertEquals(sanitized.cpLossVsChosen, None)
    assertEquals(sanitized.deferredMove, None)
    assertEquals(sanitized.practicalAlternative, false)
    assertEquals(sanitized.chosenMatchesBest, true)
  }

  test("verified blunder strips compensation framing from context and signal digests") {
    val comp = compensationInfo(125, "central pressure", Map("central pressure" -> 0.82))
    val raw =
      comparison(
        chosenMove = "Qc6",
        engineBestMove = Some("a4"),
        cpLoss = 320,
        deferredMove = Some("Qd3"),
        deferredReason = Some("it keeps the center under control")
      )
    val digest =
      NarrativeSignalDigest(
        compensation = Some("central pressure against fixed targets"),
        compensationVectors = List("central files"),
        investedMaterial = Some(125),
        decisionComparison = Some(raw.toDigest)
      )

    val contract = DecisiveTruth.derive(
      ctx = ctx(
        "c7c6",
        "Qc6",
        semantic = Some(semanticSection(compensation = Some(comp), afterCompensation = Some(comp)))
      ),
      momentType = Some("AdvantageSwing"),
      cpBefore = Some(100),
      cpAfter = Some(420),
      comparisonOverride = Some(raw)
    )
    val sanitizedCtx =
      DecisiveTruth.sanitizeContext(
        ctx("c7c6", "Qc6", semantic = Some(semanticSection(compensation = Some(comp), afterCompensation = Some(comp)))),
        contract
      )
    val sanitizedDigest = DecisiveTruth.sanitizeDigest(Some(digest), contract).getOrElse(fail("missing digest"))

    assertEquals(contract.truthClass, DecisiveTruthClass.Blunder)
    assertEquals(contract.compensationProseAllowed, false)
    assertEquals(sanitizedCtx.semantic.flatMap(_.compensation), None)
    assertEquals(sanitizedCtx.semantic.flatMap(_.afterCompensation), None)
    assertEquals(sanitizedDigest.compensation, None)
    assertEquals(sanitizedDigest.compensationVectors, Nil)
    assertEquals(sanitizedDigest.investedMaterial, None)
    assertEquals(sanitizedDigest.decisionComparison.flatMap(_.deferredMove), Some("a4"))
    assertEquals(sanitizedDigest.decisionComparison.exists(_.practicalAlternative), false)
  }

  test("bad moves without a verified best alternative cannot name a benchmark") {
    val raw =
      comparison(
        chosenMove = "Qc6",
        engineBestMove = None,
        cpLoss = 150,
        deferredMove = Some("Qd3"),
        deferredReason = Some("it coordinates more cleanly")
      )

    val contract = DecisiveTruth.derive(
      ctx = ctx("c7c6", "Qc6"),
      comparisonOverride = Some(raw)
    )
    val sanitized = DecisiveTruth.sanitizeDecisionComparison(Some(raw), contract).getOrElse(fail("missing comparison"))

    assertEquals(contract.truthClass, DecisiveTruthClass.Mistake)
    assertEquals(contract.benchmarkProseAllowed, false)
    assertEquals(contract.benchmarkMove, None)
    assertEquals(sanitized.engineBestMove, None)
    assertEquals(sanitized.deferredMove, None)
    assertEquals(sanitized.deferredReason, None)
    assertEquals(sanitized.practicalAlternative, false)
  }

  test("value-down capture with verified payoff becomes first investment commitment") {
    val comp =
      compensationInfo(
        investedMaterial = 200,
        conversionPlan = "open-file pressure on the d-file",
        returnVector = Map("open-file pressure" -> 0.76, "d-file control" -> 0.68)
      )
    val raw =
      comparison(
        chosenMove = "Rxd5",
        engineBestMove = Some("Rxd5"),
        cpLoss = 0,
        chosenMatchesBest = true
      )
    val pack =
      StrategyPack(
        sideToMove = "white",
        longTermFocus = List("open-file pressure on the d-file before recovering the material"),
        signalDigest =
          Some(
            NarrativeSignalDigest(
              compensation = Some("return vector through initiative and continuing pressure"),
              compensationVectors = List("initiative", "continuing pressure", "d-file control"),
              investedMaterial = Some(200),
              dominantIdeaFocus = Some("pressure on d5")
            )
          )
      )

    val contract = DecisiveTruth.derive(
      ctx =
        ctx(
          playedMove = "d1d5",
          playedSan = "Rxd5",
          fen = "3r2k1/8/8/3b4/8/8/8/3R2K1 w - - 0 1",
          semantic = Some(semanticSection(compensation = Some(comp)))
        ),
      cpBefore = Some(20),
      cpAfter = Some(220),
      strategyPack = Some(pack),
      comparisonOverride = Some(raw)
    )
    val sanitizedPack = DecisiveTruth.sanitizeStrategyPack(Some(pack), contract).getOrElse(fail("missing pack"))

    assertEquals(contract.truthPhase, Some(InvestmentTruthPhase.FirstInvestmentCommitment))
    assertEquals(contract.truthClass, DecisiveTruthClass.WinningInvestment)
    assertEquals(contract.ownershipRole, TruthOwnershipRole.CommitmentOwner)
    assertEquals(contract.visibilityRole, TruthVisibilityRole.PrimaryVisible)
    assertEquals(contract.surfaceMode, TruthSurfaceMode.InvestmentExplain)
    assertEquals(contract.exemplarRole, TruthExemplarRole.VerifiedExemplar)
    assertEquals(contract.surfacedMoveOwnsTruth, true)
    assertEquals(contract.compensationProseAllowed, true)
    assertEquals(contract.verifiedPayoffAnchor, Some("pressure on d5"))
    assertEquals(contract.benchmarkMove, None)
    assertEquals(
      sanitizedPack.signalDigest.flatMap(_.compensation),
      Some("return vector through initiative and continuing pressure")
    )
  }

  test("deriveFrame records full move-truth facts for a verified investment commitment") {
    val comp =
      compensationInfo(
        investedMaterial = 200,
        conversionPlan = "open-file pressure on the d-file",
        returnVector = Map("open-file pressure" -> 0.76, "d-file control" -> 0.68)
      )
    val raw =
      comparison(
        chosenMove = "Rxd5",
        engineBestMove = Some("Rxd5"),
        cpLoss = 0,
        chosenMatchesBest = true
      )
    val pack =
      StrategyPack(
        sideToMove = "white",
        longTermFocus = List("open-file pressure on the d-file before recovering the material"),
        signalDigest =
          Some(
            NarrativeSignalDigest(
              compensation = Some("return vector through initiative and continuing pressure"),
              compensationVectors = List("initiative", "continuing pressure", "d-file control"),
              investedMaterial = Some(200),
              dominantIdeaFocus = Some("pressure on d5")
            )
          )
      )

    val frame = DecisiveTruth.deriveFrame(
      ctx =
        ctx(
          playedMove = "d1d5",
          playedSan = "Rxd5",
          fen = "3r2k1/8/8/3b4/8/8/8/3R2K1 w - - 0 1",
          semantic = Some(semanticSection(compensation = Some(comp)))
        ),
      cpBefore = Some(20),
      cpAfter = Some(220),
      strategyPack = Some(pack),
      comparisonOverride = Some(raw)
    )

    assertEquals(frame.moveQuality.verdict, MoveQualityVerdict.Best)
    assertEquals(frame.benchmark.verifiedBestMove, Some("Rxd5"))
    assertEquals(frame.benchmark.chosenMatchesBest, true)
    assertEquals(frame.materialEconomics.sacrificeKind, Some("exchange_sac"))
    assertEquals(frame.materialEconomics.valueDownCapture, true)
    assertEquals(frame.strategicOwnership.truthPhase, Some(InvestmentTruthPhase.FirstInvestmentCommitment))
    assertEquals(frame.strategicOwnership.verifiedPayoffAnchor, Some("pressure on d5"))
    assertEquals(frame.punishConversion.conversionRoute, Some("pressure on d5"))
    assertEquals(frame.difficultyNovelty.verificationTier, "deep_candidate")
    assertEquals(frame.truthClass, DecisiveTruthClass.WinningInvestment)
    assertEquals(frame.ownershipRole, TruthOwnershipRole.CommitmentOwner)
    assertEquals(frame.visibilityRole, TruthVisibilityRole.PrimaryVisible)
    assertEquals(frame.surfaceMode, TruthSurfaceMode.InvestmentExplain)
    assertEquals(frame.exemplarRole, TruthExemplarRole.VerifiedExemplar)
  }

  test("commitment owner can stay primary-visible even when compensation prose is withheld") {
    val raw =
      comparison(
        chosenMove = "Rxd5",
        engineBestMove = Some("Rxd5"),
        cpLoss = 0,
        chosenMatchesBest = true
      )
    val pack =
      StrategyPack(
        sideToMove = "white",
        longTermFocus = List("pressure on d5"),
        signalDigest =
          Some(
            NarrativeSignalDigest(
              investedMaterial = Some(200),
              dominantIdeaFocus = Some("pressure on d5")
            )
          )
      )
    val contract = DecisiveTruth.derive(
      ctx =
        ctx(
          playedMove = "d1d5",
          playedSan = "Rxd5",
          fen = "3r2k1/8/8/3b4/8/8/8/3R2K1 w - - 0 1",
          engineVariations =
            List(
              VariationLine(moves = List("d1e2"), scoreCp = 220),
              VariationLine(moves = List("g1f3"), scoreCp = 205)
            )
        ),
      cpBefore = Some(20),
      cpAfter = Some(180),
      strategyPack = Some(pack),
      comparisonOverride = Some(raw)
    )

    assertEquals(contract.truthPhase, Some(InvestmentTruthPhase.FirstInvestmentCommitment))
    assertEquals(contract.truthClass, DecisiveTruthClass.WinningInvestment)
    assertEquals(contract.ownershipRole, TruthOwnershipRole.CommitmentOwner)
    assertEquals(contract.visibilityRole, TruthVisibilityRole.PrimaryVisible)
    assertEquals(contract.surfaceMode, TruthSurfaceMode.Neutral)
    assertEquals(contract.exemplarRole, TruthExemplarRole.VerifiedExemplar)
    assertEquals(contract.compensationProseAllowed, false)
    assertEquals(contract.surfacedMoveOwnsTruth, true)
    assertEquals(contract.verifiedPayoffAnchor, Some("pressure on d5"))
  }

  test("routine preserving move stays in maintenance truth phase but loses public maintenance visibility") {
    val pack =
      StrategyPack(
        sideToMove = "white",
        longTermFocus = List("central file pressure"),
        signalDigest =
          Some(
            NarrativeSignalDigest(
              compensation = Some("central file pressure"),
              compensationVectors = List("central files"),
              investedMaterial = Some(100),
              dominantIdeaFocus = Some("pressure on e6")
            )
          )
      )
    val raw =
      comparison(
        chosenMove = "Kh2",
        engineBestMove = Some("Kh2"),
        cpLoss = 0,
        chosenMatchesBest = true
      )

    val contract = DecisiveTruth.derive(
      ctx =
        ctx(
          playedMove = "g1h2",
          playedSan = "Kh2",
          fen = "r5k1/8/8/8/1b6/8/8/3R2K1 w - - 0 1"
        ),
      cpBefore = Some(40),
      cpAfter = Some(40),
      strategyPack = Some(pack),
      comparisonOverride = Some(raw)
    )
    val sanitizedPack = DecisiveTruth.sanitizeStrategyPack(Some(pack), contract).getOrElse(fail("missing pack"))

    assertEquals(contract.truthPhase, Some(InvestmentTruthPhase.CompensationMaintenance))
    assertEquals(contract.truthClass, DecisiveTruthClass.Best)
    assertEquals(contract.ownershipRole, TruthOwnershipRole.NoneRole)
    assertEquals(contract.visibilityRole, TruthVisibilityRole.Hidden)
    assertEquals(contract.surfaceMode, TruthSurfaceMode.Neutral)
    assertEquals(contract.exemplarRole, TruthExemplarRole.NonExemplar)
    assertEquals(contract.isInvestment, false)
    assertEquals(contract.surfacedMoveOwnsTruth, false)
    assertEquals(contract.compensationProseAllowed, false)
    assertEquals(contract.verifiedPayoffAnchor, Some("central file pressure"))
    assertEquals(contract.maintenanceExemplarCandidate, false)
    assertEquals(sanitizedPack.signalDigest.flatMap(_.compensation), None)
    assertEquals(sanitizedPack.signalDigest.flatMap(_.investedMaterial), None)
  }

  test("generic route carrier no longer promotes routine maintenance into a maintenance candidate") {
    val pack =
      StrategyPack(
        sideToMove = "white",
        longTermFocus = List("central file pressure"),
        pieceRoutes =
          List(
            StrategyPieceRoute(
              ownerSide = "white",
              piece = "R",
              from = "d1",
              route = List("d1", "d3", "d5"),
              purpose = "queenside pressure",
              strategicFit = 0.87,
              tacticalSafety = 0.82,
              surfaceConfidence = 0.84,
              surfaceMode = RouteSurfaceMode.Exact
            )
          ),
        directionalTargets =
          List(
            StrategyDirectionalTarget(
              targetId = "target_d5",
              ownerSide = "white",
              piece = "R",
              from = "d1",
              targetSquare = "d5",
              readiness = "build",
              strategicReasons = List("queenside pressure")
            )
          ),
        signalDigest =
          Some(
            NarrativeSignalDigest(
              compensation = Some("central file pressure"),
              compensationVectors = List("central files"),
              investedMaterial = Some(100),
              dominantIdeaFocus = Some("pressure on e6")
            )
          )
      )
    val raw =
      comparison(
        chosenMove = "Kh2",
        engineBestMove = Some("Kh2"),
        cpLoss = 0,
        chosenMatchesBest = true
      )

    val frame = DecisiveTruth.deriveFrame(
      ctx =
        ctx(
          playedMove = "g1h2",
          playedSan = "Kh2",
          fen = "r5k1/8/8/8/1b6/8/8/3R2K1 w - - 0 1"
        ),
      cpBefore = Some(40),
      cpAfter = Some(40),
      strategyPack = Some(pack),
      comparisonOverride = Some(raw)
    )

    assertEquals(frame.strategicOwnership.truthPhase, Some(InvestmentTruthPhase.CompensationMaintenance))
    assertEquals(frame.ownershipRole, TruthOwnershipRole.NoneRole)
    assertEquals(frame.visibilityRole, TruthVisibilityRole.Hidden)
    assertEquals(frame.surfaceMode, TruthSurfaceMode.Neutral)
    assertEquals(frame.exemplarRole, TruthExemplarRole.NonExemplar)
    assertEquals(frame.strategicOwnership.currentMoveEvidence, true)
    assertEquals(frame.strategicOwnership.currentConcreteCarrier, true)
    assertEquals(frame.strategicOwnership.legacyVisibleOnly, false)
    assertEquals(frame.strategicOwnership.currentCarrierAnchorMatch, false)
    assertEquals(frame.strategicOwnership.maintenanceExemplarCandidate, false)
    assert(frame.strategicOwnership.evidenceProvenance.contains(CommitmentEvidenceProvenance.CurrentSemantic))
    assert(frame.strategicOwnership.evidenceProvenance.contains(CommitmentEvidenceProvenance.LegacyShell))
    assertEquals(frame.surfacedMoveOwnsTruth, false)
    assertEquals(frame.compensationProseAllowed, false)
  }

  test("critical maintenance keeps maintenance visibility when the best move is a direct only-move hold") {
    val pack =
      StrategyPack(
        sideToMove = "white",
        longTermFocus = List("central file pressure"),
        pieceRoutes =
          List(
            StrategyPieceRoute(
              ownerSide = "white",
              piece = "R",
              from = "d1",
              route = List("d1", "d3", "d5"),
              purpose = "central file pressure",
              strategicFit = 0.87,
              tacticalSafety = 0.82,
              surfaceConfidence = 0.84,
              surfaceMode = RouteSurfaceMode.Exact
            )
          ),
        signalDigest =
          Some(
            NarrativeSignalDigest(
              compensation = Some("central file pressure"),
              compensationVectors = List("central file pressure", "central files"),
              investedMaterial = Some(100),
              dominantIdeaFocus = Some("central file pressure")
            )
          )
      )
    val raw =
      comparison(
        chosenMove = "Kh2",
        engineBestMove = Some("Kh2"),
        cpLoss = 0,
        chosenMatchesBest = true
      )

    val frame = DecisiveTruth.deriveFrame(
      ctx =
        ctx(
          playedMove = "g1h2",
          playedSan = "Kh2",
          fen = "r5k1/8/8/8/1b6/8/8/3R2K1 w - - 0 1",
          choiceType = "OnlyMove"
        ),
      cpBefore = Some(40),
      cpAfter = Some(40),
      strategyPack = Some(pack),
      comparisonOverride = Some(raw)
    )

    assertEquals(frame.strategicOwnership.truthPhase, Some(InvestmentTruthPhase.CompensationMaintenance))
    assertEquals(frame.strategicOwnership.currentCarrierAnchorMatch, true)
    assertEquals(frame.strategicOwnership.maintenancePressureQualified, true)
    assertEquals(frame.strategicOwnership.criticalMaintenance, true)
    assertEquals(frame.ownershipRole, TruthOwnershipRole.MaintenanceEcho)
    assertEquals(frame.visibilityRole, TruthVisibilityRole.SupportingVisible)
    assertEquals(frame.surfaceMode, TruthSurfaceMode.MaintenancePreserve)
    assertEquals(frame.strategicOwnership.maintenanceExemplarCandidate, true)
  }

  test("bare target-square scaffolding cannot authorize a failure intent anchor") {
    val pack =
      StrategyPack(
        sideToMove = "white",
        longTermFocus = List("pressure on b7"),
        directionalTargets =
          List(
            StrategyDirectionalTarget(
              targetId = "target_b7",
              ownerSide = "white",
              piece = "Q",
              from = "d1",
              targetSquare = "b7",
              readiness = "build",
              strategicReasons = Nil
            )
          )
      )
    val raw =
      comparison(
        chosenMove = "Qa4",
        engineBestMove = Some("Qe2"),
        cpLoss = 220
      )

    val frame = DecisiveTruth.deriveFrame(
      ctx =
        ctx(
          playedMove = "d1a4",
          playedSan = "Qa4",
          fen = "rnbqkbnr/pppp1ppp/8/4p3/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1"
        ),
      cpBefore = Some(20),
      cpAfter = Some(0),
      strategyPack = Some(pack),
      comparisonOverride = Some(raw)
    )

    assertEquals(frame.failureInterpretation.failureMode, FailureInterpretationMode.QuietPositionalCollapse)
    assertEquals(frame.failureInterpretation.intentAnchor, None)
    assertEquals(frame.failureInterpretation.interpretationAllowed, false)
  }

  test("legacy shell sacrifice without a fresh matched anchor no longer counts as speculative investment failure") {
    val pack =
      StrategyPack(
        sideToMove = "white",
        longTermFocus = List("open-file pressure"),
        signalDigest =
          Some(
            NarrativeSignalDigest(
              compensation = Some("open-file pressure"),
              compensationVectors = List("initiative"),
              investedMaterial = Some(200)
            )
          )
      )
    val raw =
      comparison(
        chosenMove = "Rxd5",
        engineBestMove = Some("Qe2"),
        cpLoss = 360
      )

    val frame = DecisiveTruth.deriveFrame(
      ctx =
        ctx(
          playedMove = "d1d5",
          playedSan = "Rxd5",
          fen = "3r2k1/8/8/3b4/8/8/8/3R2K1 w - - 0 1"
        ),
      cpBefore = Some(40),
      cpAfter = Some(0),
      strategyPack = Some(pack),
      comparisonOverride = Some(raw)
    )

    assertEquals(frame.truthClass, DecisiveTruthClass.Blunder)
    assertNotEquals(frame.failureInterpretation.failureMode, FailureInterpretationMode.SpeculativeInvestmentFailed)
    assertEquals(frame.failureInterpretation.intentAnchor, None)
    assertEquals(frame.failureInterpretation.interpretationAllowed, false)
  }

  test("forcing-line blunder with a concrete route carrier keeps a tactical failure interpretation") {
    val pack =
      StrategyPack(
        sideToMove = "white",
        pieceRoutes =
          List(
            StrategyPieceRoute(
              ownerSide = "white",
              piece = "Q",
              from = "d1",
              route = List("d1", "h5"),
              purpose = "kingside pressure",
              strategicFit = 0.82,
              tacticalSafety = 0.50,
              surfaceConfidence = 0.78,
              surfaceMode = RouteSurfaceMode.Exact
            )
          )
      )
    val raw =
      comparison(
        chosenMove = "Qh5",
        engineBestMove = Some("Qe2"),
        cpLoss = 360
      )

    val frame = DecisiveTruth.deriveFrame(
      ctx =
        ctx(
          playedMove = "d1h5",
          playedSan = "Qh5",
          fen = "rnb1kbnr/pppp1ppp/8/4p2q/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1",
          engineVariations =
            List(
              VariationLine(moves = List("d1e2"), scoreCp = 220),
              VariationLine(moves = List("g1f3"), scoreCp = 205)
            )
        ),
      momentType = Some("AdvantageSwing"),
      cpBefore = Some(80),
      cpAfter = Some(-160),
      strategyPack = Some(pack),
      comparisonOverride = Some(raw)
    )

    assertEquals(frame.truthClass, DecisiveTruthClass.Blunder)
    assertEquals(frame.ownershipRole, TruthOwnershipRole.BlunderOwner)
    assertEquals(frame.failureInterpretation.failureMode, FailureInterpretationMode.TacticalRefutation)
    assertEquals(frame.failureInterpretation.interpretationAllowed, true)
    assert(frame.failureInterpretation.intentAnchor.contains("kingside pressure"), clue(frame.failureInterpretation))
  }

  test("only-move failure keeps the stronger failure mode when the player plan is concrete") {
    val pack =
      StrategyPack(
        sideToMove = "white",
        pieceMoveRefs =
          List(
            StrategyPieceMoveRef(
              ownerSide = "white",
              piece = "N",
              from = "f3",
              target = "f7",
              idea = "attack f7",
              evidence = List("threatens pressure on f7")
            )
          )
      )
    val raw =
      comparison(
        chosenMove = "Ng5",
        engineBestMove = Some("Qe2"),
        cpLoss = 360
      )

    val frame = DecisiveTruth.deriveFrame(
      ctx =
        ctx(
          playedMove = "f3g5",
          playedSan = "Ng5",
          fen = "rnbqkbnr/pppp1ppp/8/4p3/8/5N2/PPPPPPPP/RNBQKB1R w KQkq - 0 1",
          choiceType = "OnlyMove"
        ),
      cpBefore = Some(30),
      cpAfter = Some(-10),
      strategyPack = Some(pack),
      comparisonOverride = Some(raw)
    )

    assertEquals(frame.truthClass, DecisiveTruthClass.Blunder)
    assertEquals(frame.failureInterpretation.failureMode, FailureInterpretationMode.OnlyMoveFailure)
    assertEquals(frame.failureInterpretation.interpretationAllowed, true)
    assert(frame.failureInterpretation.intentAnchor.exists(_.contains("attack f7")), clue(frame.failureInterpretation))
  }

  test("objective-only quiet collapse is not allowed to over-interpret intent and strips strategy carriers") {
    val pack =
      StrategyPack(
        sideToMove = "white",
        longTermFocus = List("pressure on b7"),
        strategicIdeas =
          List(
            StrategyIdeaSignal(
              ideaId = "idea_space",
              ownerSide = "white",
              kind = StrategicIdeaKind.SpaceGainOrRestriction,
              group = "slow_structural",
              readiness = StrategicIdeaReadiness.Build,
              focusSquares = List("b5"),
              beneficiaryPieces = List("P"),
              confidence = 0.64
            )
          )
      )
    val raw =
      comparison(
        chosenMove = "a4",
        engineBestMove = Some("Qe2"),
        cpLoss = 360
      )

    val frame = DecisiveTruth.deriveFrame(
      ctx =
        ctx(
          playedMove = "a2a4",
          playedSan = "a4",
          fen = "rnbqkbnr/pppp1ppp/8/4p3/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1",
          engineVariations =
            List(
              VariationLine(moves = List("d1e2"), scoreCp = 220),
              VariationLine(moves = List("g1f3"), scoreCp = 205)
            )
        ),
      cpBefore = Some(20),
      cpAfter = Some(0),
      strategyPack = Some(pack),
      comparisonOverride = Some(raw)
    )
    val sanitizedPack = DecisiveTruth.sanitizeStrategyPack(Some(pack), frame.toContract).getOrElse(fail("missing pack"))

    assertEquals(frame.truthClass, DecisiveTruthClass.Blunder)
    assertEquals(frame.failureInterpretation.failureMode, FailureInterpretationMode.QuietPositionalCollapse)
    assertEquals(frame.failureInterpretation.interpretationAllowed, false)
    assertEquals(sanitizedPack.longTermFocus, Nil)
    assertEquals(sanitizedPack.strategicIdeas, Nil)
    assertEquals(sanitizedPack.pieceRoutes, Nil)
    assertEquals(sanitizedPack.directionalTargets, Nil)
    assert(sanitizedPack.signalDigest.flatMap(_.dominantIdeaFocus).isEmpty, clue(sanitizedPack.signalDigest))
  }

  test("failed speculative sacrifice is tagged separately from generic tactical blunders") {
    val pack =
      StrategyPack(
        sideToMove = "white",
        pieceRoutes =
          List(
            StrategyPieceRoute(
              ownerSide = "white",
              piece = "R",
              from = "d1",
              route = List("d1", "d5"),
              purpose = "open-file pressure",
              strategicFit = 0.86,
              tacticalSafety = 0.41,
              surfaceConfidence = 0.76,
              surfaceMode = RouteSurfaceMode.Exact
            )
          ),
        longTermFocus = List("open-file pressure"),
        signalDigest = Some(NarrativeSignalDigest(dominantIdeaFocus = Some("open-file pressure")))
      )
    val raw =
      comparison(
        chosenMove = "Rxd5",
        engineBestMove = Some("Qe2"),
        cpLoss = 360
      )

    val frame = DecisiveTruth.deriveFrame(
      ctx =
        ctx(
          playedMove = "d1d5",
          playedSan = "Rxd5",
          fen = "3r2k1/8/8/3b4/8/8/8/3R2K1 w - - 0 1",
          engineVariations =
            List(
              VariationLine(moves = List("d1e2"), scoreCp = 220),
              VariationLine(moves = List("g1f3"), scoreCp = 205)
            )
        ),
      cpBefore = Some(40),
      cpAfter = Some(-180),
      strategyPack = Some(pack),
      comparisonOverride = Some(raw)
    )

    assertEquals(frame.truthClass, DecisiveTruthClass.Blunder)
    assertEquals(frame.failureInterpretation.failureMode, FailureInterpretationMode.SpeculativeInvestmentFailed)
    assertEquals(frame.failureInterpretation.interpretationAllowed, true)
    assert(frame.failureInterpretation.intentAnchor.contains("open file pressure"), clue(frame.failureInterpretation))
  }

  test("fresh value-down capture can stay provisional without investedMaterial carrier") {
    val pack =
      StrategyPack(
        sideToMove = "white",
        longTermFocus = List("open-file pressure"),
        signalDigest =
          Some(
            NarrativeSignalDigest(
              dominantIdeaFocus = Some("open-file pressure")
            )
          )
      )
    val raw =
      comparison(
        chosenMove = "Rxd5",
        engineBestMove = Some("Rxd5"),
        cpLoss = 0,
        chosenMatchesBest = true
      )

    val contract = DecisiveTruth.derive(
      ctx =
        ctx(
          playedMove = "d1d5",
          playedSan = "Rxd5",
          fen = "3r2k1/8/8/3b4/8/8/8/3R2K1 w - - 0 1"
        ),
      cpBefore = Some(40),
      cpAfter = Some(180),
      strategyPack = Some(pack),
      comparisonOverride = Some(raw)
    )

    assertEquals(contract.truthPhase, Some(InvestmentTruthPhase.FirstInvestmentCommitment))
    assertEquals(contract.truthClass, DecisiveTruthClass.WinningInvestment)
    assertEquals(contract.ownershipRole, TruthOwnershipRole.NoneRole)
    assertEquals(contract.visibilityRole, TruthVisibilityRole.SupportingVisible)
    assertEquals(contract.surfaceMode, TruthSurfaceMode.Neutral)
    assertEquals(contract.exemplarRole, TruthExemplarRole.ProvisionalExemplar)
    assertEquals(contract.compensationProseAllowed, false)
    assertEquals(contract.surfacedMoveOwnsTruth, false)
    assertEquals(contract.verifiedPayoffAnchor, Some("open file pressure"))
    assert(contract.investmentTruthChainKey.exists(_.nonEmpty), clue(contract.investmentTruthChainKey))
  }

  test("accepted current semantic support upgrades the same fresh seed to commitment owner") {
    val pack =
      StrategyPack(
        sideToMove = "white",
        longTermFocus = List("open-file pressure"),
        signalDigest =
          Some(
            NarrativeSignalDigest(
              dominantIdeaFocus = Some("open-file pressure")
            )
          )
      )
    val raw =
      comparison(
        chosenMove = "Rxd5",
        engineBestMove = Some("Rxd5"),
        cpLoss = 0,
        chosenMatchesBest = true
      )

    val contract = DecisiveTruth.derive(
      ctx =
        ctx(
          playedMove = "d1d5",
          playedSan = "Rxd5",
          fen = "3r2k1/8/8/3b4/8/8/8/3R2K1 w - - 0 1",
          semantic =
            Some(
              semanticSection(
                compensation =
                  Some(
                    compensationInfo(
                      investedMaterial = 100,
                      conversionPlan = "open-file pressure",
                      returnVector = Map("open-file pressure" -> 0.72)
                    )
                  )
              )
            )
        ),
      cpBefore = Some(40),
      cpAfter = Some(180),
      strategyPack = Some(pack),
      comparisonOverride = Some(raw)
    )

    assertEquals(contract.truthPhase, Some(InvestmentTruthPhase.FirstInvestmentCommitment))
    assertEquals(contract.truthClass, DecisiveTruthClass.WinningInvestment)
    assertEquals(contract.ownershipRole, TruthOwnershipRole.CommitmentOwner)
    assertEquals(contract.visibilityRole, TruthVisibilityRole.PrimaryVisible)
    assertEquals(contract.surfaceMode, TruthSurfaceMode.Neutral)
    assertEquals(contract.exemplarRole, TruthExemplarRole.VerifiedExemplar)
    assertEquals(contract.compensationProseAllowed, false)
    assertEquals(contract.verifiedPayoffAnchor, Some("open file pressure"))
  }

  test("after-semantic support alone cannot upgrade a fresh seed into commitment owner") {
    val pack =
      StrategyPack(
        sideToMove = "white",
        longTermFocus = List("open-file pressure"),
        signalDigest =
          Some(
            NarrativeSignalDigest(
              dominantIdeaFocus = Some("open-file pressure")
            )
          )
      )
    val raw =
      comparison(
        chosenMove = "Rxd5",
        engineBestMove = Some("Rxd5"),
        cpLoss = 0,
        chosenMatchesBest = true
      )

    val contract = DecisiveTruth.derive(
      ctx =
        ctx(
          playedMove = "d1d5",
          playedSan = "Rxd5",
          fen = "3r2k1/8/8/3b4/8/8/8/3R2K1 w - - 0 1",
          semantic =
            Some(
              semanticSection(
                afterCompensation =
                  Some(
                    compensationInfo(
                      investedMaterial = 100,
                      conversionPlan = "central file pressure",
                      returnVector = Map("central file pressure" -> 0.76)
                    )
                  )
              )
            )
        ),
      cpBefore = Some(40),
      cpAfter = Some(180),
      strategyPack = Some(pack),
      comparisonOverride = Some(raw)
    )

    assertEquals(contract.truthPhase, Some(InvestmentTruthPhase.FirstInvestmentCommitment))
    assertEquals(contract.ownershipRole, TruthOwnershipRole.NoneRole)
    assertEquals(contract.visibilityRole, TruthVisibilityRole.SupportingVisible)
    assertEquals(contract.surfaceMode, TruthSurfaceMode.Neutral)
    assertEquals(contract.exemplarRole, TruthExemplarRole.ProvisionalExemplar)
    assertEquals(contract.verifiedPayoffAnchor, Some("open file pressure"))
  }

  test("legacy shell can preserve maintenance truth phase but cannot reopen public maintenance visibility") {
    val pack =
      StrategyPack(
        sideToMove = "white",
        longTermFocus = List("central file pressure"),
        signalDigest =
          Some(
            NarrativeSignalDigest(
              compensation = Some("central file pressure"),
              compensationVectors = List("central files"),
              investedMaterial = Some(100),
              dominantIdeaFocus = Some("pressure on e6")
            )
          )
      )
    val raw =
      comparison(
        chosenMove = "Kh2",
        engineBestMove = Some("Kh2"),
        cpLoss = 0,
        chosenMatchesBest = true
      )

    val frame = DecisiveTruth.deriveFrame(
      ctx =
        ctx(
          playedMove = "g1h2",
          playedSan = "Kh2",
          fen = "r5k1/8/8/8/1b6/8/8/3R2K1 w - - 0 1"
        ),
      cpBefore = Some(40),
      cpAfter = Some(40),
      strategyPack = Some(pack),
      comparisonOverride = Some(raw)
    )

    assertEquals(frame.strategicOwnership.truthPhase, Some(InvestmentTruthPhase.CompensationMaintenance))
    assertEquals(frame.ownershipRole, TruthOwnershipRole.NoneRole)
    assertEquals(frame.visibilityRole, TruthVisibilityRole.Hidden)
    assertEquals(frame.surfaceMode, TruthSurfaceMode.Neutral)
    assertEquals(frame.exemplarRole, TruthExemplarRole.NonExemplar)
    assertEquals(frame.strategicOwnership.evidenceProvenance, Set(CommitmentEvidenceProvenance.LegacyShell))
    assertEquals(frame.strategicOwnership.ownerEligible, false)
  }

  test("secondary idea, route purpose, and directional target text can each seed the payoff anchor") {
    val raw =
      comparison(
        chosenMove = "Rxd5",
        engineBestMove = Some("Rxd5"),
        cpLoss = 0,
        chosenMatchesBest = true
      )
    val packs =
      List(
        (
          "secondary_idea",
          StrategyPack(
            sideToMove = "white",
            signalDigest =
              Some(
                NarrativeSignalDigest(
                  secondaryIdeaFocus = Some("pressure on b2")
                )
              )
          ),
          Some("pressure on b2")
        ),
        (
          "route_purpose",
          StrategyPack(
            sideToMove = "white",
            pieceRoutes =
              List(
                StrategyPieceRoute(
                  ownerSide = "white",
                  piece = "R",
                  from = "d1",
                  route = List("d1", "d5"),
                  purpose = "open-file pressure",
                  strategicFit = 0.84,
                  tacticalSafety = 0.79,
                  surfaceConfidence = 0.82,
                  surfaceMode = RouteSurfaceMode.Exact
                )
              )
          ),
          Some("open file pressure")
        ),
        (
          "directional_target",
          StrategyPack(
            sideToMove = "white",
            directionalTargets =
              List(
                StrategyDirectionalTarget(
                  targetId = "target_b2",
                  ownerSide = "white",
                  piece = "Q",
                  from = "d1",
                  targetSquare = "b2",
                  readiness = "build",
                  strategicReasons = List("fixed queenside targets")
                )
              )
          ),
          Some("fixed queenside targets")
        )
      )

    packs.foreach { case (label, pack, expectedAnchor) =>
      val contract = DecisiveTruth.derive(
        ctx =
          ctx(
            playedMove = "d1d5",
            playedSan = "Rxd5",
            fen = "3r2k1/8/8/3b4/8/8/8/3R2K1 w - - 0 1"
          ),
        cpBefore = Some(40),
        cpAfter = Some(180),
        strategyPack = Some(pack),
        comparisonOverride = Some(raw)
      )

      assertEquals(contract.ownershipRole, TruthOwnershipRole.NoneRole, clue(label))
      assertEquals(contract.exemplarRole, TruthExemplarRole.ProvisionalExemplar, clue(label))
      assertEquals(contract.verifiedPayoffAnchor, expectedAnchor, clue(label))
    }
  }

  test("recovery move becomes conversion followthrough instead of fresh investment") {
    val raw =
      comparison(
        chosenMove = "exd5",
        engineBestMove = Some("exd5"),
        cpLoss = 0,
        chosenMatchesBest = true
      )
    val pack =
      StrategyPack(
        sideToMove = "white",
        longTermFocus = List("a promotion race"),
        signalDigest =
          Some(
            NarrativeSignalDigest(
              dominantIdeaFocus = Some("a promotion race")
            )
          )
      )

    val contract = DecisiveTruth.derive(
      ctx =
        ctx(
          playedMove = "e4d5",
          playedSan = "exd5",
          fen = "6k1/8/8/3b4/4P3/8/8/6K1 w - - 0 1",
          semantic =
            Some(
              semanticSection(
                compensation =
                  Some(
                    compensationInfo(
                      investedMaterial = 100,
                      conversionPlan = "a promotion race",
                      returnVector = Map("promotion race" -> 0.71)
                    )
                  )
              )
            )
        ),
      transitionType = Some("PromotionConversion"),
      cpBefore = Some(110),
      cpAfter = Some(240),
      strategyPack = Some(pack),
      comparisonOverride = Some(raw)
    )

    assertEquals(contract.truthPhase, Some(InvestmentTruthPhase.ConversionFollowthrough))
    assertEquals(contract.truthClass, DecisiveTruthClass.Best)
    assertEquals(contract.ownershipRole, TruthOwnershipRole.ConversionOwner)
    assertEquals(contract.visibilityRole, TruthVisibilityRole.PrimaryVisible)
    assertEquals(contract.surfaceMode, TruthSurfaceMode.ConversionExplain)
    assertEquals(contract.exemplarRole, TruthExemplarRole.NonExemplar)
    assertEquals(contract.isInvestment, false)
    assertEquals(contract.surfacedMoveOwnsTruth, true)
    assertEquals(contract.compensationProseAllowed, false)
    assertEquals(contract.verifiedPayoffAnchor, Some("a promotion race"))
  }

  test("inherited compensation shell without fresh payoff anchor is not a commitment") {
    val raw =
      comparison(
        chosenMove = "Kh2",
        engineBestMove = Some("Kh2"),
        cpLoss = 0,
        chosenMatchesBest = true
      )
    val pack =
      StrategyPack(
        sideToMove = "white",
        longTermFocus = List("the material can wait while the pressure is still there"),
        signalDigest =
          Some(
            NarrativeSignalDigest(
              compensation = Some("the material can wait while the pressure is still there"),
              compensationVectors = List("return vector"),
              investedMaterial = Some(100)
            )
          )
      )

    val contract = DecisiveTruth.derive(
      ctx =
        ctx(
          playedMove = "g1h2",
          playedSan = "Kh2",
          fen = "r5k1/8/8/8/1b6/8/8/3R2K1 w - - 0 1"
        ),
      cpBefore = Some(30),
      cpAfter = Some(30),
      strategyPack = Some(pack),
      comparisonOverride = Some(raw)
    )

    assertEquals(contract.truthPhase, None)
    assertEquals(contract.truthClass, DecisiveTruthClass.Best)
    assertEquals(contract.ownershipRole, TruthOwnershipRole.NoneRole)
    assertEquals(contract.visibilityRole, TruthVisibilityRole.Hidden)
    assertEquals(contract.surfaceMode, TruthSurfaceMode.Neutral)
    assertEquals(contract.exemplarRole, TruthExemplarRole.NonExemplar)
    assertEquals(contract.compensationProseAllowed, false)
    assertEquals(contract.verifiedPayoffAnchor, None)
  }

  test("deriveFrame records benchmark and punishment facts for a verified blunder") {
    val raw =
      comparison(
        chosenMove = "Qc6",
        engineBestMove = Some("a4"),
        cpLoss = 320,
        deferredMove = Some("Qd3"),
        deferredReason = Some("it keeps the center under control")
      )

    val frame = DecisiveTruth.deriveFrame(
      ctx = ctx("c7c6", "Qc6"),
      momentType = Some("AdvantageSwing"),
      cpBefore = Some(100),
      cpAfter = Some(420),
      comparisonOverride = Some(raw)
    )

    assertEquals(frame.moveQuality.verdict, MoveQualityVerdict.Blunder)
    assertEquals(frame.moveQuality.severityBand, "catastrophic")
    assertEquals(frame.benchmark.verifiedBestMove, Some("a4"))
    assertEquals(frame.benchmark.benchmarkNamingAllowed, true)
    assertEquals(frame.tactical.immediateRefutation, true)
    assertEquals(frame.punishConversion.concessionSummary, Some("hands over the initiative"))
    assertEquals(frame.truthClass, DecisiveTruthClass.Blunder)
    assertEquals(frame.ownershipRole, TruthOwnershipRole.BlunderOwner)
    assertEquals(frame.surfaceMode, TruthSurfaceMode.FailureExplain)
    assertEquals(frame.compensationProseAllowed, false)
  }

  test("proof-backed best only-move defense becomes primary-visible without claiming ownership") {
    val raw =
      comparison(
        chosenMove = "Qe2",
        engineBestMove = Some("Qe2"),
        cpLoss = 0,
        chosenMatchesBest = true
      )

    val contract = DecisiveTruth.derive(
      ctx =
        ctx(
          playedMove = "d1e2",
          playedSan = "Qe2",
          choiceType = "OnlyMove",
          criticality = "Forced"
        ),
      comparisonOverride = Some(raw)
    )

    assertEquals(contract.truthClass, DecisiveTruthClass.Best)
    assertEquals(contract.reasonFamily, DecisiveReasonFamily.OnlyMoveDefense)
    assertEquals(contract.ownershipRole, TruthOwnershipRole.NoneRole)
    assertEquals(contract.visibilityRole, TruthVisibilityRole.PrimaryVisible)
    assertEquals(contract.surfaceMode, TruthSurfaceMode.Neutral)
    assertEquals(contract.benchmarkCriticalMove, true)
    assertEquals(contract.isPromotedBestHold, true)
  }

  test("quiet benchmark-critical hold stays hidden until proof exists") {
    val raw =
      comparison(
        chosenMove = "Qe2",
        engineBestMove = Some("Qe2"),
        cpLoss = 0,
        chosenMatchesBest = true
      )

    val contract = DecisiveTruth.derive(
      ctx =
        ctx(
          playedMove = "d1e2",
          playedSan = "Qe2",
          choiceType = "OnlyMove",
          criticality = "Normal"
        ),
      comparisonOverride = Some(raw)
    )

    assertEquals(contract.truthClass, DecisiveTruthClass.Best)
    assertEquals(contract.reasonFamily, DecisiveReasonFamily.QuietTechnicalMove)
    assertEquals(contract.ownershipRole, TruthOwnershipRole.NoneRole)
    assertEquals(contract.visibilityRole, TruthVisibilityRole.Hidden)
    assertEquals(contract.surfaceMode, TruthSurfaceMode.Neutral)
    assertEquals(contract.benchmarkCriticalMove, true)
    assertEquals(contract.isBenchmarkCriticalQuietHold, true)
  }

  test("best tactical refutation is not treated as a critical best move") {
    val contract =
      DecisiveTruthContract(
        playedMove = Some("c8c7"),
        verifiedBestMove = Some("c8c7"),
        truthClass = DecisiveTruthClass.Best,
        cpLoss = 0,
        swingSeverity = 0,
        reasonFamily = DecisiveReasonFamily.TacticalRefutation,
        allowConcreteBenchmark = true,
        chosenMatchesBest = true,
        compensationAllowed = false,
        truthPhase = None,
        ownershipRole = TruthOwnershipRole.NoneRole,
        visibilityRole = TruthVisibilityRole.Hidden,
        surfaceMode = TruthSurfaceMode.Neutral,
        exemplarRole = TruthExemplarRole.NonExemplar,
        surfacedMoveOwnsTruth = false,
        verifiedPayoffAnchor = None,
        compensationProseAllowed = false,
        benchmarkProseAllowed = true,
        investmentTruthChainKey = None,
        maintenanceExemplarCandidate = false,
        benchmarkCriticalMove = false,
        failureMode = FailureInterpretationMode.NoClearPlan,
        failureIntentConfidence = 0.0,
        failureIntentAnchor = None,
        failureInterpretationAllowed = false
      )

    assertEquals(contract.isCriticalBestMove, false)
  }

  test("fallback moment projection does not recreate investment ownership from serialized fields") {
    val projection =
      DecisiveTruth.momentProjection(
        arcMoment(
          ply = 55,
          momentType = "InvestmentPivot",
          moveClassification = Some("WinningInvestment"),
          truthPhase = Some("FirstInvestmentCommitment"),
          surfacedMoveOwnsTruth = true,
          verifiedPayoffAnchor = Some("open-file pressure"),
          benchmarkProseAllowed = true,
          investmentTruthChainKey = Some("white:open-file pressure")
        ),
        None
      )

    assertEquals(projection.classificationKey, "winninginvestment")
    assertEquals(projection.ownershipRole, TruthOwnershipRole.NoneRole)
    assertEquals(projection.visibilityRole, TruthVisibilityRole.Hidden)
    assertEquals(projection.surfaceMode, TruthSurfaceMode.Neutral)
    assertEquals(projection.exemplarRole, TruthExemplarRole.NonExemplar)
    assertEquals(projection.surfacedMoveOwnsTruth, false)
    assertEquals(projection.verifiedPayoffAnchor, None)
    assertEquals(projection.benchmarkProseAllowed, false)
    assertEquals(projection.chainKey, None)
  }

  test("fallback moment projection ignores raw conversion transitions without a truth contract") {
    val projection =
      DecisiveTruth.momentProjection(
        chronicleMoment(
          ply = 36,
          momentType = "SustainedPressure",
          transitionType = Some("ExchangeConversion")
        ),
        None
      )

    assertEquals(projection.ownershipRole, TruthOwnershipRole.NoneRole)
    assertEquals(projection.visibilityRole, TruthVisibilityRole.Hidden)
    assertEquals(projection.surfaceMode, TruthSurfaceMode.Neutral)
    assertEquals(projection.surfacedMoveOwnsTruth, false)
  }

  test("fallback moment projection preserves blunder failure classification") {
    val projection =
      DecisiveTruth.momentProjection(
        chronicleMoment(
          ply = 18,
          momentType = "AdvantageSwing",
          moveClassification = Some("Blunder")
        ),
        None
      )

    assertEquals(projection.classificationKey, "blunder")
    assertEquals(projection.ownershipRole, TruthOwnershipRole.BlunderOwner)
    assertEquals(projection.visibilityRole, TruthVisibilityRole.PrimaryVisible)
    assertEquals(projection.surfaceMode, TruthSurfaceMode.FailureExplain)
    assertEquals(projection.surfacedMoveOwnsTruth, true)
  }
