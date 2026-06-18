package lila.chessjudgment.analysis.positionAssessment

import lila.chessjudgment.analysis.PositionFeatures

/**
 * Position Assessor
 * 
 * Classifies a position into 8 fundamental categories using only:
 * - Board feature inputs (PositionFeatures)
 * - MultiPV evaluation data
 * - Current eval
 * 
 * No dependency on tactical motif inputs or downstream analysis.
 */
object PositionAssessor:

  def classify(
    features: PositionFeatures,
    multiPv: List[PvLine],
    currentEval: Int,
    tacticalMotifsCount: Int = 0  // Optional: from tactical motif inputs if available
  ): PositionAssessment =
    val nature = classifyNature(features)
    val criticality = classifyCriticality(multiPv)
    val candidateSet = assessCandidateSet(multiPv)
    val gamePhase = classifyGamePhase(features)
    val simplifyBias = classifySimplifyBias(features, currentEval)
    val drawBias = classifyDrawBias(features)
    val riskProfile = classifyRiskProfile(features, tacticalMotifsCount)
    val judgmentFocus = deriveJudgmentFocus(nature, criticality, simplifyBias, riskProfile)

    PositionAssessment(
      nature = nature,
      criticality = criticality,
      candidateSet = candidateSet,
      gamePhase = gamePhase,
      simplifyBias = simplifyBias,
      drawBias = drawBias,
      riskProfile = riskProfile,
      judgmentFocus = judgmentFocus
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

  private def classifyCriticality(multiPv: List[PvLine]): CriticalityResult =
    val bestLine = multiPv.headOption
    val secondLine = multiPv.lift(1)

    val mateDistance = bestLine.flatMap(_.mate)
    val evalDelta = secondLine.map(line => bestLine.map(_.score).getOrElse(0) - line.score).getOrElse(0)
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
  private def assessCandidateSet(multiPv: List[PvLine]): CandidateSetTopology =
    if multiPv.size < 2 then
      return CandidateSetTopology(
        candidateSetType = CandidateSetType.NarrowChoice, // Unknown, not OnlyMove
        bestLineEvalCp = multiPv.headOption.map(_.score).getOrElse(0),
        secondLineEvalCp = 0,
        thirdLineEvalCp = None,
        gapBestToSecondWp = 0.0,
        spreadTop3Wp = 0.0,
        secondCandidateFailure = Some(CandidateFailureMode.InsufficientData)
      )

    val bestLineEvalCp = multiPv.head.score
    val secondLineEvalCp = multiPv(1).score
    val thirdLineEvalCp = multiPv.lift(2).map(_.score)

    val bestLineWinPercent = winPercentFromCp(bestLineEvalCp)
    val secondLineWinPercent = winPercentFromCp(secondLineEvalCp)
    val thirdLineWinPercent = thirdLineEvalCp.map(winPercentFromCp)

    val gapBestToSecondWp = (bestLineWinPercent - secondLineWinPercent).abs
    val spreadTop3Wp = thirdLineWinPercent.map(third => (bestLineWinPercent - third).abs).getOrElse(gapBestToSecondWp)

    val onlyMoveThreshold = 10.0
    val styleChoiceThreshold = 3.0

    // Determine why the second candidate fails if the best line is mandatory.
    val secondCandidateFailure: Option[CandidateFailureMode] =
      if gapBestToSecondWp > onlyMoveThreshold then
        Some(categorizeCandidateFailure(gapBestToSecondWp, secondLineWinPercent))
      else None

    // Decision logic with WinPercent thresholds
    val candidateSetType =
      if gapBestToSecondWp > onlyMoveThreshold then
        CandidateSetType.OnlyMove
      else if spreadTop3Wp <= styleChoiceThreshold then
        CandidateSetType.StyleChoice
      else
        CandidateSetType.NarrowChoice

    CandidateSetTopology(
      candidateSetType = candidateSetType,
      bestLineEvalCp = bestLineEvalCp,
      secondLineEvalCp = secondLineEvalCp,
      thirdLineEvalCp = thirdLineEvalCp,
      gapBestToSecondWp = gapBestToSecondWp,
      spreadTop3Wp = spreadTop3Wp,
      secondCandidateFailure = secondCandidateFailure
    )

  private def categorizeCandidateFailure(gapWp: Double, secondCandidateWinPercent: Double): CandidateFailureMode =
    if gapWp > 35.0 then CandidateFailureMode.DecisiveEvaluationLoss
    else if gapWp > 20.0 then CandidateFailureMode.LosesMaterial
    else if secondCandidateWinPercent < 32.0 then CandidateFailureMode.PositionCollapses
    else CandidateFailureMode.SignificantDisadvantage

  private def winPercentFromCp(cp: Int): Double =
    val slope = 0.00368208
    50.0 + 50.0 * (2.0 / (1.0 + math.exp(-slope * cp.toDouble)) - 1.0)
  // 4. GAME PHASE CLASSIFICATION

  private def classifyGamePhase(features: PositionFeatures): GamePhaseResult =
    val mat = features.materialPhase
    val imb = features.imbalance

    val totalMaterial = mat.whiteMaterial + mat.blackMaterial
    val queensOnBoard = imb.whiteQueens > 0 || imb.blackQueens > 0
    val minorPiecesCount = imb.whiteKnights + imb.blackKnights + imb.whiteBishops + imb.blackBishops

    // Use existing phase from board feature extraction, but enrich with evidence
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
  // 8. JUDGMENT FOCUS DERIVATION

  private def deriveJudgmentFocus(
    nature: NatureResult,
    criticality: CriticalityResult,
    simplifyBias: SimplifyBiasResult,
    riskProfile: RiskProfileResult
  ): JudgmentFocusResult =
    val (focus, driver) =
      if criticality.isForced then
        (JudgmentFocusType.Tactics, JudgmentDriver.ForcedSequence)
      else if simplifyBias.isSimplificationWindow then
        (JudgmentFocusType.Conversion, JudgmentDriver.ConversionOpportunity)
      else if riskProfile.isHighRisk then
        (JudgmentFocusType.Tactics, JudgmentDriver.TacticalComplexity)
      else if nature.isChaos then
        (JudgmentFocusType.Tactics, JudgmentDriver.ChaoticPosition)
      else if nature.isStatic then
        (JudgmentFocusType.Plan, JudgmentDriver.StrategicStructure)
      else
        (JudgmentFocusType.Plan, JudgmentDriver.DynamicPosition)

    JudgmentFocusResult(
      focus = focus,
      primaryDriver = driver
    )
  // EXTENSION: Int to Boolean conversion
  extension (i: Int)
    def toBoolean: Boolean = i > 0
