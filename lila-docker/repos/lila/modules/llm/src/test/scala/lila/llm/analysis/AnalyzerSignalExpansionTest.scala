package lila.llm.analysis

import chess.{ Color, File, Rook }
import lila.llm.analysis.L3.*
import lila.llm.analysis.PlanMatcher.ActivePlans
import lila.llm.model.{ Motif, Plan, PlanMatch, TransitionType }
import lila.llm.model.strategic.PlanContinuity
import munit.FunSuite

class AnalyzerSignalExpansionTest extends FunSuite:

  private def classification(
    simplify: Boolean = false,
    phase: GamePhaseType = GamePhaseType.Middlegame,
    risk: RiskLevel = RiskLevel.Medium
  ): PositionClassification =
    PositionClassification(
      nature = NatureResult(NatureType.Dynamic, tensionScore = 1, openFilesCount = 1, mobilityDiff = 0, lockedCenter = false),
      criticality = CriticalityResult(CriticalityType.Normal, evalDeltaCp = 0, mateDistance = None, forcingMovesInPv = 0),
      choiceTopology = ChoiceTopologyResult(
        topologyType = ChoiceTopologyType.StyleChoice,
        pv1Eval = 0,
        pv2Eval = 0,
        pv3Eval = None,
        gapPv1ToPv2 = 0,
        spreadTop3 = 0,
        pv2FailureMode = None
      ),
      gamePhase = GamePhaseResult(
        phaseType = phase,
        totalMaterial = if phase == GamePhaseType.Endgame then 16 else 56,
        queensOnBoard = phase != GamePhaseType.Endgame,
        minorPiecesCount = if phase == GamePhaseType.Endgame then 2 else 6
      ),
      simplifyBias = SimplifyBiasResult(
        isSimplificationWindow = simplify,
        evalAdvantage = if simplify then 220 else 0,
        isEndgameNear = phase == GamePhaseType.Endgame,
        exchangeAvailable = simplify
      ),
      drawBias = DrawBiasResult(
        isDrawish = false,
        materialSymmetry = false,
        oppositeColorBishops = false,
        fortressLikely = false,
        insufficientMaterial = false
      ),
      riskProfile = RiskProfileResult(risk, evalVolatility = 0, tacticalMotifsCount = 0, kingExposureSum = 0),
      taskMode = TaskModeResult(TaskModeType.ExplainPlan, "test")
    )

  private def featuresFromFen(fen: String): PositionFeatures =
    PositionAnalyzer.extractFeatures(fen, plyCount = 0).getOrElse(fail(s"failed to parse features for $fen"))

  private def activePlans(plan: Plan): ActivePlans =
    val primary = PlanMatch(plan = plan, score = 0.9, evidence = Nil)
    ActivePlans(
      primary = primary,
      secondary = None,
      suppressed = Nil,
      allPlans = List(primary)
    )

  private def threatAnalysis(threats: List[Threat]): ThreatAnalysis =
    val maxLoss = threats.map(_.lossIfIgnoredCp).maxOption.getOrElse(0)
    val severity =
      if threats.exists(_.severity == ThreatSeverity.Urgent) then ThreatSeverity.Urgent
      else if threats.exists(_.severity == ThreatSeverity.Important) then ThreatSeverity.Important
      else ThreatSeverity.Low

    ThreatAnalyzer.noThreat.copy(
      threats = threats,
      defense = ThreatAnalyzer.noThreat.defense.copy(
        necessity = severity,
        notes = "test threat analysis"
      ),
      threatSeverity = severity,
      immediateThreat = threats.exists(_.isImmediate),
      strategicThreat = threats.exists(_.isStrategic),
      threatIgnorable = false,
      defenseRequired = maxLoss >= Thresholds.URGENT_THREAT_CP,
      prophylaxisNeeded = threats.exists(t => t.isStrategic && t.lossIfIgnoredCp >= Thresholds.SIGNIFICANT_THREAT_CP),
      resourceAvailable = true,
      maxLossIfIgnored = maxLoss,
      primaryDriver =
        if threats.exists(_.kind == ThreatKind.Mate) then "mate_threat"
        else if maxLoss >= Thresholds.URGENT_THREAT_CP then "material_threat"
        else if threats.nonEmpty then "positional_threat"
        else "no_threat"
    )

  test("BreakAnalyzer finds wing lever even when the center is locked") {
    val fen = "4k3/8/8/2pp4/1P1Pp3/4P3/8/4K3 w - - 0 1"
    val analysis = BreakAnalyzer.analyze(featuresFromFen(fen), Nil, classification())

    assert(analysis.pawnBreakReady)
    assertEquals(analysis.breakFile, Some("b"))
    assert(analysis.breakImpact > 0)
    assertEquals(analysis.primaryDriver, "break_ready")
  }

  test("BreakAnalyzer downgrades an unsupported blockaded seventh-rank passer") {
    val fen = "3r2k1/3P4/8/8/8/8/8/4K3 w - - 0 1"
    val analysis = BreakAnalyzer.analyze(featuresFromFen(fen), Nil, classification(phase = GamePhaseType.Endgame))

    assertEquals(analysis.passedPawnUrgency, PassedPawnUrgency.Important)
    assert(analysis.passerBlockade)
    assertEquals(analysis.blockadeRole, Some(Rook))
    assert(!analysis.pusherSupport)
  }

  test("BreakAnalyzer counts nearby king escort as passed-pawn support") {
    val fen = "6k1/3P4/4K3/8/8/8/8/8 w - - 0 1"
    val analysis = BreakAnalyzer.analyze(featuresFromFen(fen), Nil, classification(phase = GamePhaseType.Endgame))

    assertEquals(analysis.passedPawnUrgency, PassedPawnUrgency.Critical)
    assert(analysis.pusherSupport)
    assert(!analysis.passerBlockade)
  }

  test("ThreatAnalyzer keeps positional pressure as a strategic threat signal") {
    val analysis = ThreatAnalyzer.analyze(
      fen = "6k1/8/8/8/8/8/8/6K1 w - - 0 1",
      motifs = List(Motif.OpenFileControl(File.D, Color.Black, plyIndex = 12, move = None)),
      multiPv = Nil,
      phase1 = classification(risk = RiskLevel.Low),
      sideToMove = "white"
    )

    assert(analysis.strategicThreat)
    assert(analysis.prophylaxisNeeded)
    assertEquals(analysis.threats.head.kind, ThreatKind.Positional)
    assert(analysis.threats.head.turnsToImpact >= 3)
  }

  test("ThreatAnalyzer uses side-to-move-normalized MultiPV deltas for black") {
    val analysis = ThreatAnalyzer.analyze(
      fen = "6k1/8/8/8/8/8/6q1/6K1 b - - 0 1",
      motifs = Nil,
      multiPv = List(
        PvLine(List("g2g1"), evalCp = 50, mate = None, depth = 18),
        PvLine(List("g2h1"), evalCp = -250, mate = None, depth = 18)
      ),
      phase1 = classification(),
      sideToMove = "black"
    )

    assertEquals(analysis.maxLossIfIgnored, 300)
    assertEquals(analysis.primaryDriver, "material_threat")
    assert(analysis.defenseRequired)
    assert(analysis.threats.exists(_.bestDefense.contains("g2g1")))
  }

  test("TransitionAnalyzer keeps attack-to-conversion shifts as natural realignments") {
    val ctx = IntegratedContext(
      evalCp = 240,
      classification = Some(classification(simplify = true)),
      threatsToThem = Some(
        threatAnalysis(
          List(
            Threat(
              kind = ThreatKind.Material,
              lossIfIgnoredCp = 220,
              turnsToImpact = 1,
              motifs = List("Fork"),
              attackSquares = Nil,
              targetPieces = Nil,
              bestDefense = None,
              defenseCount = 1
            )
          )
        )
      ),
      isWhiteToMove = true
    )

    val sequence = TransitionAnalyzer.analyze(
      currentPlans = activePlans(Plan.Simplification(Color.White)),
      continuityOpt = Some(
        PlanContinuity(
          planName = "Kingside Attack",
          planId = Some(Plan.KingsideAttack(Color.White).id.toString),
          consecutivePlies = 2,
          startingPly = 18
        )
      ),
      ctx = ctx
    )

    assertEquals(sequence.transitionType, TransitionType.NaturalShift)
  }

  test("TransitionAnalyzer escalates sustained defensive pressure into a forced pivot") {
    val ctx = IntegratedContext(
      evalCp = 20,
      classification = Some(classification()),
      threatsToUs = Some(
        threatAnalysis(
          List(
            Threat(
              kind = ThreatKind.Positional,
              lossIfIgnoredCp = 140,
              turnsToImpact = 3,
              motifs = List("OpenFileControl"),
              attackSquares = List("d-file"),
              targetPieces = Nil,
              bestDefense = None,
              defenseCount = 2
            )
          )
        )
      ),
      isWhiteToMove = true
    )

    val sequence = TransitionAnalyzer.analyze(
      currentPlans = activePlans(Plan.Prophylaxis(Color.White, "counterplay")),
      continuityOpt = Some(
        PlanContinuity(
          planName = "Preparing e-break",
          planId = Some(Plan.PawnBreakPreparation(Color.White, "e").id.toString),
          consecutivePlies = 2,
          startingPly = 24
        )
      ),
      ctx = ctx
    )

    assertEquals(sequence.transitionType, TransitionType.ForcedPivot)
  }
