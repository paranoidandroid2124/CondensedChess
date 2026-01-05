package lila.llm.analysis.L3

import chess.Color
import lila.llm.model.Motif

/**
 * Phase 2: Threat Analyzer
 * 
 * Analyzes threats from the opponent's perspective and assesses
 * defensive requirements for the side to move.
 * 
 * Uses Hybrid approach:
 * 1. L2 Motifs -> base threat detection (filtered by side)
 * 2. MultiPV delta -> loss correction (signed, not absolute)
 * 3. Phase 1 -> threshold adjustment
 * 
 * IMPORTANT: Eval POV Assumption
 * This code assumes that `PvLine.score` is ALREADY normalized to the
 * side-to-move's perspective (positive = good for side to move).
 * If your engine provides eval from White's POV, you must flip the sign
 * for Black before passing to this analyzer.
 */
object ThreatAnalyzer:

  // ============================================================
  // CONFIGURATION
  // ============================================================
  
  private val MATERIAL_THREAT_THRESHOLD = 300    // cp
  private val URGENT_THREAT_THRESHOLD = 800      // cp
  private val IGNORABLE_THREAT_THRESHOLD = 120   // cp
  private val ONLY_DEFENSE_TOLERANCE = 50        // cp difference for "adequate" defense
  private val MIN_DEPTH_FOR_RELIABILITY = 16

  // ============================================================
  // MAIN ENTRY POINT
  // ============================================================

  /**
   * Analyze threats in the position from the opponent's perspective.
   * 
   * @param motifs L2 tactical motifs detected in the position
   * @param multiPv MultiPV lines from engine analysis
   * @param phase1 Phase 1 classification (for threshold adjustment)
   * @param sideToMove Which side is to move ("white" or "black")
   * @return Complete threat analysis
   */
  def analyze(
    motifs: List[Motif],
    multiPv: List[PvLine],
    phase1: PositionClassification,
    sideToMove: String = "white"
  ): ThreatAnalysis =
    val isWhiteToMove = sideToMove.equalsIgnoreCase("white")
    
    // Step 1: Extract threats from OPPONENT's motifs only
    val opponentThreats = extractOpponentThreats(motifs, isWhiteToMove)
    
    // Step 2: Correct with MultiPV data (side-aware)
    val correctedThreats = correctWithMultiPv(opponentThreats, multiPv, isWhiteToMove)
    
    // Step 3: Adjust thresholds based on Phase 1 and depth
    val thresholds = adjustThresholds(phase1, multiPv)
    
    // Step 4: Populate defense evidence from MultiPV
    val threatsWithDefense = populateDefenseEvidence(correctedThreats, multiPv, thresholds)
    
    // Step 5: Compute aggregates
    computeAggregates(threatsWithDefense, multiPv, thresholds, phase1)

  // ============================================================
  // STEP 1: L2 MOTIF -> THREAT EXTRACTION (SIDE-AWARE)
  // ============================================================

  /**
   * FIX #2: Only extract threats made BY the opponent, not our own tactics
   */
  private def extractOpponentThreats(motifs: List[Motif], isWhiteToMove: Boolean): List[Threat] =
    motifs.flatMap { motif =>
      // Get motif color - threats are from opponent's perspective
      val motifColor = getMotifColor(motif)
      val isOpponentMotif = motifColor.exists { color =>
        (isWhiteToMove && color == Color.Black) || (!isWhiteToMove && color == Color.White)
      }
      
      // Only process opponent's motifs as threats
      if !isOpponentMotif then None
      else
        val motifName = motif.getClass.getSimpleName.replace("$", "")
        val (baseLoss, turnsToImpact) = MotifLossTable.getBaseLoss(motifName)
        
        if baseLoss > 0 then
          val kind = 
            if MotifLossTable.isMateMotif(motifName) then ThreatKind.Mate
            else if baseLoss >= MATERIAL_THREAT_THRESHOLD then ThreatKind.Material
            else ThreatKind.Positional
          
          Some(Threat(
            kind = kind,
            lossIfIgnoredCp = baseLoss,
            turnsToImpact = turnsToImpact,
            motifs = List(motifName),
            attackSquares = extractAttackSquares(motif),
            targetPieces = extractTargetPieces(motif),
            bestDefense = None,
            defenseCount = 0  // Will be populated in Step 4
          ))
        else None
    }

  /**
   * Comprehensive motif color extraction.
   * All 31+ motif types now have explicit color fields.
   */
  private def getMotifColor(motif: Motif): Option[Color] =
    motif match
      // Tactical motifs with explicit color field
      case m: Motif.Fork => Some(m.color)
      case m: Motif.Pin => Some(m.color)
      case m: Motif.Skewer => Some(m.color)
      case m: Motif.DiscoveredAttack => Some(m.color)
      case m: Motif.Deflection => Some(m.color)
      case m: Motif.Decoy => Some(m.color)
      case m: Motif.Overloading => Some(m.color)
      case m: Motif.DoubleCheck => Some(m.color)
      case m: Motif.BackRankMate => Some(m.color)
      case m: Motif.TrappedPiece => Some(m.color)
      case m: Motif.Interference => Some(m.color)
      case m: Motif.Clearance => Some(m.color)
      case m: Motif.Check => Some(m.color)
      case m: Motif.Capture => Some(m.color)
      case m: Motif.Zwischenzug => Some(m.color)
      // Pawn motifs with explicit color field
      case m: Motif.PawnAdvance => Some(m.color)
      case m: Motif.PawnBreak => Some(m.color)
      case m: Motif.PawnPromotion => Some(m.color)
      case m: Motif.PassedPawnPush => Some(m.color)
      // Piece motifs with explicit color field
      case m: Motif.RookLift => Some(m.color)
      case m: Motif.Fianchetto => Some(m.color)
      case m: Motif.Outpost => Some(m.color)
      case m: Motif.PieceLift => Some(m.color)
      case m: Motif.Centralization => Some(m.color)
      // King motifs with explicit color field
      case m: Motif.KingStep => Some(m.color)
      case m: Motif.Castling => Some(m.color)
      // Structural motifs with explicit color field
      case m: Motif.DoubledPieces => Some(m.color)
      case m: Motif.Battery => Some(m.color)
      case m: Motif.IsolatedPawn => Some(m.color)
      case m: Motif.BackwardPawn => Some(m.color)
      case m: Motif.PassedPawn => Some(m.color)
      // Positional motifs (strategic, not immediate threats)
      // NOTE: These are included for completeness but typically have low
      // threat values in MotifLossTable since they are strategic, not tactical.
      case m: Motif.OpenFileControl => Some(m.color)
      case m: Motif.WeakBackRank => Some(m.color)
      case m: Motif.SpaceAdvantage => Some(m.color)
      // Fallback for any other motifs without color field
      case _ => None

  private def extractAttackSquares(motif: Motif): List[String] =
    motif match
      case m: Motif.Fork => List(m.square.toString)
      case m: Motif.Check => List(m.targetSquare.toString)
      case m: Motif.Capture => List(m.square.toString)
      case m: Motif.Pin => List(m.pinnedPiece.toString)
      case m: Motif.Skewer => List(m.frontPiece.toString)
      case m: Motif.DiscoveredAttack => List(m.attackingPiece.toString)
      case m: Motif.Deflection => List(m.fromSquare.toString)
      case m: Motif.Decoy => List(m.toSquare.toString)
      case _ => Nil

  private def extractTargetPieces(motif: Motif): List[String] =
    motif match
      case m: Motif.Fork => m.targets.map(_.toString)
      case m: Motif.Pin => List(m.pinnedPiece.toString, m.targetBehind.toString)
      case m: Motif.Skewer => List(m.frontPiece.toString, m.backPiece.toString)
      case m: Motif.DiscoveredAttack => List(m.target.toString)
      case m: Motif.Check => List("King")
      case m: Motif.Capture => List(m.captured.toString)
      case _ => Nil

  // ============================================================
  // STEP 2: MULTIPV DELTA CORRECTION (SIDE-AWARE)
  // ============================================================

  /**
   * FIX #1 & #5: Correct MultiPV detection
   * - Use UCI pattern matching (not SAN)
   * - Use SIGNED eval delta (negative = opponent threat)
   * - Classify threats correctly based on mate signals
   */
  private def correctWithMultiPv(
    threats: List[Threat],
    multiPv: List[PvLine],
    isWhiteToMove: Boolean
  ): List[Threat] =
    if multiPv.size < 2 then
      // Insufficient data - return base threats
      threats
    else
      val pv1 = multiPv.head
      val pv2 = multiPv(1)
      
      // FIX #5: Use SIGNED delta from side-to-move perspective
      // Positive delta = PV1 is better than PV2 (loss if we don't play PV1)
      val signedDelta = if isWhiteToMove then pv1.score - pv2.score else pv2.score - pv1.score
      
      // If PV2 is significantly worse, opponent has a threat
      val evalLoss = signedDelta.max(0)
      
      // FIX v4 #2: Only detect PAWN diagonal captures from UCI
      // For other captures, rely on evalLoss threshold instead
      val pv2FirstMove = pv2.moves.headOption
      val pv2IsPawnCapture = pv2FirstMove.exists(isPawnDiagonalCapture)
      val pv2IsMate = pv2.mate.exists(_ < 0)  // Negative mate = opponent mates us
      
      // Detect threat if: mate, pawn capture, OR significant eval loss
      val hasSignificantThreat = pv2IsMate || pv2IsPawnCapture || evalLoss >= MATERIAL_THREAT_THRESHOLD
      
      if hasSignificantThreat && evalLoss > 50 then
        // FIX #5: Classify correctly based on mate signal
        val kind = if pv2IsMate then ThreatKind.Mate
                   else if evalLoss >= MATERIAL_THREAT_THRESHOLD then ThreatKind.Material
                   else ThreatKind.Positional
        
        val impliedThreat = Threat(
          kind = kind,
          lossIfIgnoredCp = if pv2IsMate then 10000 else evalLoss,
          turnsToImpact = 1,
          motifs = List("PvDelta"),
          attackSquares = pv2FirstMove.map(m => if m.length >= 4 then m.substring(2, 4) else "").filter(_.nonEmpty).toList,
          targetPieces = Nil,
          bestDefense = pv1.moves.headOption,  // Still UCI, will note in defense
          defenseCount = 1
        )
        
        // Keep all threats, don't merge by kind (FIX #5: preserve multiple threats)
        impliedThreat :: threats
      else if evalLoss > 0 then
        // Boost existing threats with eval evidence
        threats.map { t =>
          t.copy(lossIfIgnoredCp = t.lossIfIgnoredCp.max(evalLoss))
        }
      else
        threats

  /**
   * FIX v4 #2: Conservative UCI capture detection
   * 
   * PROBLEM: Without board state, we can't know if a target square is occupied.
   * SOLUTION: Only detect PAWN diagonal moves as definite captures.
   *           For other pieces, rely on evalLoss threshold instead of UCI heuristics.
   * 
   * NOTE: bestDefense and alternatives fields contain UCI notation, not SAN.
   * L4 should convert if human-readable format is needed.
   */
  private def isPawnDiagonalCapture(uci: String): Boolean =
    if uci.length < 4 then false
    else
      val fromFile = uci.charAt(0)
      val toFile = uci.charAt(2)
      val fromRank = uci.charAt(1).asDigit
      val toRank = uci.charAt(3).asDigit
      
      // Pawn move = from rank 2-7, rank change of 1
      val isPawnMove = fromRank >= 2 && fromRank <= 7 && (toRank - fromRank).abs == 1
      
      // Pawn can only change file when capturing (or en passant)
      val fileChange = fromFile != toFile
      
      isPawnMove && fileChange

  // ============================================================
  // STEP 3: THRESHOLD ADJUSTMENT
  // ============================================================

  case class ThresholdConfig(
    ignorableThreshold: Int,
    immediateThreshold: Int,
    overrideDefenseRequired: Boolean,
    depthReliable: Boolean
  )

  /**
   * FIX v3 #4: Actually use MIN_DEPTH_FOR_RELIABILITY and set depthReliable properly
   */
  private def adjustThresholds(phase1: PositionClassification, multiPv: List[PvLine]): ThresholdConfig =
    val avgDepth = if multiPv.isEmpty then 0 else multiPv.map(_.depth).sum / multiPv.size
    
    ThresholdConfig(
      ignorableThreshold = 
        if phase1.simplifyBias.isSimplificationWindow then 150 
        else IGNORABLE_THREAT_THRESHOLD,
      
      immediateThreshold =
        if phase1.riskProfile.isHighRisk then 80
        else IGNORABLE_THREAT_THRESHOLD,
      
      overrideDefenseRequired = phase1.criticality.isForced,
      
      // FIX v3 #4: Set based on actual depth
      depthReliable = avgDepth >= MIN_DEPTH_FOR_RELIABILITY
    )

  // ============================================================
  // STEP 4: POPULATE DEFENSE EVIDENCE
  // ============================================================

  /**
   * FIX #3: Actually populate defense evidence from MultiPV
   */
  private def populateDefenseEvidence(
    threats: List[Threat],
    multiPv: List[PvLine],
    thresholds: ThresholdConfig
  ): List[Threat] =
    if threats.isEmpty then threats
    else
      // Count defenses that keep eval within ONLY_DEFENSE_TOLERANCE of best
      val bestEval = multiPv.headOption.map(_.score).getOrElse(0)
      val adequateDefenses = multiPv.filter { pv =>
        (pv.score - bestEval).abs <= ONLY_DEFENSE_TOLERANCE
      }
      val defenseCount = adequateDefenses.size
      
      // Check depth reliability (FIX #4: use MIN_DEPTH_FOR_RELIABILITY)
      val avgDepth = multiPv.map(_.depth).sum.toDouble / multiPv.size.max(1)
      val reliabilityFactor = if avgDepth >= MIN_DEPTH_FOR_RELIABILITY then 1.0 else 0.8
      
      threats.map { t =>
        t.copy(
          defenseCount = defenseCount,
          bestDefense = multiPv.headOption.flatMap(_.moves.headOption),
          // FIX #4: Apply depth penalty to lossIfIgnoredCp
          lossIfIgnoredCp = (t.lossIfIgnoredCp * reliabilityFactor).toInt
        )
      }

  // ============================================================
  // STEP 5: AGGREGATE COMPUTATION
  // ============================================================

  private def computeAggregates(
    threats: List[Threat],
    multiPv: List[PvLine],
    thresholds: ThresholdConfig,
    phase1: PositionClassification
  ): ThreatAnalysis =
    val maxLoss = threats.map(_.lossIfIgnoredCp).maxOption.getOrElse(0)
    val hasMate = threats.exists(_.kind == ThreatKind.Mate)
    val hasImmediate = threats.exists(_.isImmediate)
    val hasStrategic = threats.exists(_.isStrategic)
    
    // FIX #4: Use immediateThreshold for immediate threat detection
    val hasUrgentImmediate = threats.exists { t =>
      t.isImmediate && t.lossIfIgnoredCp >= thresholds.immediateThreshold
    }
    
    val severity = 
      if hasMate || maxLoss >= URGENT_THREAT_THRESHOLD then ThreatSeverity.Urgent
      else if maxLoss >= MATERIAL_THREAT_THRESHOLD then ThreatSeverity.Important
      else ThreatSeverity.Low
    
    val defenseRequired = thresholds.overrideDefenseRequired ||
                          hasMate ||
                          maxLoss >= MATERIAL_THREAT_THRESHOLD
    
    val threatIgnorable = !defenseRequired &&
                          maxLoss < thresholds.ignorableThreshold &&
                          phase1.riskProfile.isLowRisk
    
    val counterThreatBetter = false  // TODO: Requires analyzing our threats
    
    val prophylaxisNeeded = !hasImmediate && hasStrategic && maxLoss >= 100
    
    // FIX #3: resourceAvailable based on actual defense count
    val totalDefenses = threats.map(_.defenseCount).sum
    val resourceAvailable = threats.isEmpty || totalDefenses > 0
    
    val primaryDriver = 
      if hasMate then "mate_threat"
      else if maxLoss >= MATERIAL_THREAT_THRESHOLD then "material_threat"
      else if threats.nonEmpty then "positional_threat"
      else "no_threat"
    
    // FIX #3: Extract alternatives from MultiPV
    val alternatives = multiPv.take(3).flatMap(_.moves.headOption).distinct
    
    val defense = DefenseAssessment(
      necessity = severity,
      onlyDefense = if totalDefenses == 1 then threats.headOption.flatMap(_.bestDefense) else None,
      alternatives = alternatives,
      counterIsBetter = counterThreatBetter,
      prophylaxisNeeded = prophylaxisNeeded,
      resourceCoverageScore = if resourceAvailable then 80 else 20,
      notes = s"Max loss: ${maxLoss}cp, defenses: $totalDefenses, depth reliable: ${thresholds.depthReliable}"
    )
    
    ThreatAnalysis(
      threats = threats,
      defense = defense,
      threatSeverity = severity,
      immediateThreat = hasImmediate || hasUrgentImmediate,
      strategicThreat = hasStrategic,
      threatIgnorable = threatIgnorable,
      defenseRequired = defenseRequired,
      counterThreatBetter = counterThreatBetter,
      prophylaxisNeeded = prophylaxisNeeded,
      resourceAvailable = resourceAvailable,
      maxLossIfIgnored = maxLoss,
      primaryDriver = primaryDriver,
      insufficientData = multiPv.size < 2
    )

  // ============================================================
  // HELPER: Empty analysis for no-threat positions
  // ============================================================

  def noThreat: ThreatAnalysis = ThreatAnalysis(
    threats = Nil,
    defense = DefenseAssessment(
      necessity = ThreatSeverity.Low,
      onlyDefense = None,
      alternatives = Nil,
      counterIsBetter = false,
      prophylaxisNeeded = false,
      resourceCoverageScore = 100,
      notes = "No threats detected"
    ),
    threatSeverity = ThreatSeverity.Low,
    immediateThreat = false,
    strategicThreat = false,
    threatIgnorable = true,
    defenseRequired = false,
    counterThreatBetter = false,
    prophylaxisNeeded = false,
    resourceAvailable = true,
    maxLossIfIgnored = 0,
    primaryDriver = "no_threat",
    insufficientData = false
  )
