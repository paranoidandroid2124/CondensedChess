package lila.llm.analysis.L3

import lila.llm.analysis.PositionFeatures

/**
 * Position Classifier
 * 
 * Classifies a position into 8 fundamental categories using only:
 * - L1 Features (PositionFeatures)
 * - MultiPV evaluation data
 * - Current eval
 * 
 * No dependency on L2 motifs or higher-layer analysis.
 */
object PositionClassifier:

  def classify(
    features: PositionFeatures,
    multiPv: List[PvLine],
    currentEval: Int,
    tacticalMotifsCount: Int = 0  // Optional: from L2 if available
  ): PositionClassification =
    val nature = classifyNature(features)
    val criticality = classifyCriticality(multiPv, currentEval)
    val choiceTopology = classifyChoiceTopology(multiPv)
    val gamePhase = classifyGamePhase(features)
    val simplifyBias = classifySimplifyBias(features, currentEval)
    val drawBias = classifyDrawBias(features)
    val riskProfile = classifyRiskProfile(features, tacticalMotifsCount)
    val taskMode = deriveTaskMode(nature, criticality, simplifyBias, riskProfile)

    PositionClassification(
      nature = nature,
      criticality = criticality,
      choiceTopology = choiceTopology,
      gamePhase = gamePhase,
      simplifyBias = simplifyBias,
      drawBias = drawBias,
      riskProfile = riskProfile,
      taskMode = taskMode
    )
  // 1. NATURE CLASSIFICATION

  private def classifyNature(features: PositionFeatures): NatureResult =
    val central = features.centralSpace
    val line = features.lineControl
    val activity = features.activity

    val tensionScore = central.pawnTensionCount
    val openFilesCount = line.openFilesCount
    val mobilityDiff = activity.whitePseudoMobility - activity.blackPseudoMobility
    val lockedCenter = central.lockedCenter

    // Decision logic
    // Locked center strongly favors Static, even with some tension
    val natureType =
      if lockedCenter && tensionScore <= 3 && openFilesCount <= 2 then
        NatureType.Static
      else if tensionScore >= 4 || openFilesCount >= 4 then
        NatureType.Chaos
      else if tensionScore >= 2 || openFilesCount >= 2 then
        NatureType.Dynamic
      else
        NatureType.Static

    NatureResult(
      natureType = natureType,
      tensionScore = tensionScore,
      openFilesCount = openFilesCount,
      mobilityDiff = mobilityDiff,
      lockedCenter = lockedCenter
    )
  // 2. CRITICALITY CLASSIFICATION

  private def classifyCriticality(multiPv: List[PvLine], currentEval: Int): CriticalityResult =
    val pv1 = multiPv.headOption
    val pv2 = multiPv.lift(1)

    val mateDistance = pv1.flatMap(_.mate)
    val evalDelta = pv2.map(pv2Line => pv1.map(_.score).getOrElse(0) - pv2Line.score).getOrElse(0)
    // PV length alone is not a valid proxy for forcing sequences
    val hasMateInLine = mateDistance.isDefined
    val hasLargeEvalJump = evalDelta.abs >= 200
    val forcingMovesInPv = if hasMateInLine then 5 else if hasLargeEvalJump then 3 else 1

    // Decision logic
    val criticalityType =
      if mateDistance.exists(_.abs <= 5) then
        CriticalityType.ForcedSequence
      else if evalDelta.abs >= 150 then
        CriticalityType.CriticalMoment
      else
        CriticalityType.Normal

    CriticalityResult(
      criticalityType = criticalityType,
      evalDeltaCp = evalDelta,
      mateDistance = mateDistance,
      forcingMovesInPv = forcingMovesInPv
    )
  // 3. CHOICE TOPOLOGY CLASSIFICATION

  private def classifyChoiceTopology(multiPv: List[PvLine]): ChoiceTopologyResult =
    if multiPv.size < 2 then
      return ChoiceTopologyResult(
        topologyType = ChoiceTopologyType.NarrowChoice, // Unknown, not OnlyMove
        pv1Eval = multiPv.headOption.map(_.score).getOrElse(0),
        pv2Eval = 0,
        pv3Eval = None,
        gapPv1ToPv2 = 0,
        spreadTop3 = 0,
        pv2FailureMode = Some("insufficient_data")
      )

    val pv1Eval = multiPv.head.score
    val pv2Eval = multiPv(1).score
    val pv3Eval = multiPv.lift(2).map(_.score)

    val gapPv1ToPv2 = (pv1Eval - pv2Eval).abs
    val spreadTop3 = pv3Eval.map(pv3 => (pv1Eval - pv3).abs).getOrElse(gapPv1ToPv2)
    // At extreme evals (|eval| > 500), 100cp gap is less significant
    val evalMagnitude = pv1Eval.abs
    val onlyMoveThreshold = if evalMagnitude > 500 then 150 else 100
    val styleChoiceThreshold = if evalMagnitude > 300 then 50 else 30

    // Determine failure mode for PV2 if OnlyMove
    val pv2FailureMode: Option[String] =
      if gapPv1ToPv2 > onlyMoveThreshold then Some(categorizeFailure(gapPv1ToPv2, pv2Eval))
      else None

    // Decision logic with scaled thresholds
    val topologyType =
      if gapPv1ToPv2 > onlyMoveThreshold then
        ChoiceTopologyType.OnlyMove
      else if spreadTop3 <= styleChoiceThreshold then
        ChoiceTopologyType.StyleChoice
      else
        ChoiceTopologyType.NarrowChoice

    ChoiceTopologyResult(
      topologyType = topologyType,
      pv1Eval = pv1Eval,
      pv2Eval = pv2Eval,
      pv3Eval = pv3Eval,
      gapPv1ToPv2 = gapPv1ToPv2,
      spreadTop3 = spreadTop3,
      pv2FailureMode = pv2FailureMode
    )

  private def categorizeFailure(gap: Int, pv2Eval: Int): String =
    if gap > 500 then "decisive_blunder"
    else if gap > 300 then "loses_material"
    else if pv2Eval < -200 then "position_collapses"
    else "significant_disadvantage"
  // 4. GAME PHASE CLASSIFICATION

  private def classifyGamePhase(features: PositionFeatures): GamePhaseResult =
    val mat = features.materialPhase
    val imb = features.imbalance

    val totalMaterial = mat.whiteMaterial + mat.blackMaterial
    val queensOnBoard = imb.whiteQueens > 0 || imb.blackQueens > 0
    val minorPiecesCount = imb.whiteKnights + imb.blackKnights + imb.whiteBishops + imb.blackBishops

    // Use existing phase from L1, but enrich with evidence
    val phaseType = mat.phase match
      case "opening" => GamePhaseType.Opening
      case "endgame" => GamePhaseType.Endgame
      case _ => GamePhaseType.Middlegame

    GamePhaseResult(
      phaseType = phaseType,
      totalMaterial = totalMaterial,
      queensOnBoard = queensOnBoard,
      minorPiecesCount = minorPiecesCount
    )
  // 5. SIMPLIFY BIAS CLASSIFICATION

  private def classifySimplifyBias(features: PositionFeatures, currentEval: Int): SimplifyBiasResult =
    val isEndgameNear = features.materialPhase.phase == "endgame" ||
                        features.materialPhase.whiteMaterial + features.materialPhase.blackMaterial <= 50
    // Positive eval = White advantage, sideToMove determines who benefits
    // For simplification window, the WINNING side should want to simplify
    val sideToMove = features.sideToMove // "white" or "black" (lowercase from Color.name)
    val isWhiteToMove = sideToMove.equalsIgnoreCase("white")
    val evalFromSideToMove = if isWhiteToMove then currentEval else -currentEval
    val isWinning = evalFromSideToMove >= 200
    val evalAdvantage = evalFromSideToMove.max(0) // Only positive advantage counts
    
    // Check if major piece exchanges are "natural" (rooks/queens on same file, etc.)
    val exchangeAvailable = features.imbalance.whiteQueens > 0 && features.imbalance.blackQueens > 0

    // Decision: Simplification window ONLY if side-to-move is winning AND near endgame
    val isSimplificationWindow = isWinning && (isEndgameNear || evalAdvantage >= 400)

    SimplifyBiasResult(
      isSimplificationWindow = isSimplificationWindow,
      evalAdvantage = evalAdvantage,
      isEndgameNear = isEndgameNear,
      exchangeAvailable = exchangeAvailable
    )
  // 6. DRAW BIAS CLASSIFICATION

  private def classifyDrawBias(features: PositionFeatures): DrawBiasResult =
    val mat = features.materialPhase
    val imb = features.imbalance

    val materialSymmetry = mat.materialDiff.abs <= 1
    // K+N vs K, K+B vs K, K vs K are all theoretical draws
    val totalPawns = features.pawns.whitePawnCount + features.pawns.blackPawnCount
    val whiteMinors = imb.whiteKnights + imb.whiteBishops
    val blackMinors = imb.blackKnights + imb.blackBishops
    val hasMajorPieces = imb.whiteRooks > 0 || imb.blackRooks > 0 || imb.whiteQueens > 0 || imb.blackQueens > 0
    
    // Insufficient material: no pawns, no major pieces, and at most 1 minor piece total
    val insufficientMaterial = totalPawns == 0 && !hasMajorPieces && (whiteMinors + blackMinors) <= 1
    // OCB endgames are drawish even WITH rooks (R+B vs R+B OCB is very drawish)
    val oppositeColorBishops = imb.whiteBishops == 1 && imb.blackBishops == 1 &&
                               imb.whiteQueens == 0 && imb.blackQueens == 0

    // Fortress: blocked center with symmetric pawns
    val fortressLikely = features.centralSpace.lockedCenter && materialSymmetry
    val isDrawish = insufficientMaterial || 
                    (materialSymmetry && (oppositeColorBishops || fortressLikely))

    DrawBiasResult(
      isDrawish = isDrawish,
      materialSymmetry = materialSymmetry,
      oppositeColorBishops = oppositeColorBishops,
      fortressLikely = fortressLikely,
      insufficientMaterial = insufficientMaterial
    )
  // 7. RISK PROFILE CLASSIFICATION

  private def classifyRiskProfile(features: PositionFeatures, tacticalMotifsCount: Int): RiskProfileResult =
    val ks = features.kingSafety
    val kingExposureSum = ks.whiteKingExposedFiles + ks.blackKingExposedFiles +
                          ks.whiteKingRingAttacked + ks.blackKingRingAttacked

    // Eval volatility is hard to measure without multi-depth data; use proxy
    val evalVolatility = tacticalMotifsCount * 20 // Rough proxy

    // Decision logic
    val riskLevel =
      if tacticalMotifsCount >= 4 || kingExposureSum >= 6 then
        RiskLevel.High
      else if tacticalMotifsCount >= 2 || kingExposureSum >= 3 then
        RiskLevel.Medium
      else
        RiskLevel.Low

    RiskProfileResult(
      riskLevel = riskLevel,
      evalVolatility = evalVolatility,
      tacticalMotifsCount = tacticalMotifsCount,
      kingExposureSum = kingExposureSum
    )
  // 8. TASK MODE DERIVATION

  private def deriveTaskMode(
    nature: NatureResult,
    criticality: CriticalityResult,
    simplifyBias: SimplifyBiasResult,
    riskProfile: RiskProfileResult
  ): TaskModeResult =
    val (taskMode, driver) =
      if criticality.isForced then
        (TaskModeType.ExplainTactics, "forced_sequence")
      else if simplifyBias.isSimplificationWindow then
        (TaskModeType.ExplainConvert, "conversion_opportunity")
      else if riskProfile.isHighRisk then
        (TaskModeType.ExplainTactics, "tactical_complexity")
      else if nature.isChaos then
        (TaskModeType.ExplainTactics, "chaotic_position")
      else if nature.isStatic then
        (TaskModeType.ExplainPlan, "strategic_maneuvering")
      else
        (TaskModeType.ExplainPlan, "dynamic_balance")

    TaskModeResult(
      taskMode = taskMode,
      primaryDriver = driver
    )
  // EXTENSION: Int to Boolean conversion
  extension (i: Int)
    def toBoolean: Boolean = i > 0
