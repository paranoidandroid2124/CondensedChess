package lila.llm.analysis.L3

import chess.Color
import lila.llm.model.Motif

/**
 * Threat Analyzer
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
  
  private val MATERIAL_THREAT_THRESHOLD = 200     // cp
  private val URGENT_THREAT_THRESHOLD = 800      // cp
  private val IGNORABLE_THREAT_THRESHOLD = 120   // cp
  private val ONLY_DEFENSE_TOLERANCE = 50        // cp difference for "adequate" defense
  private val MIN_DEPTH_FOR_RELIABILITY = 16

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
    fen: String,
    motifs: List[Motif],
    multiPv: List[PvLine],
    phase1: PositionClassification,
    sideToMove: String = "white"
  ): ThreatAnalysis =
    val isWhiteToMove = sideToMove.equalsIgnoreCase("white")
    
    val opponentThreats = extractOpponentThreats(motifs, isWhiteToMove)
    val correctedThreats = correctWithMultiPv(opponentThreats, multiPv, isWhiteToMove, fen)
    val withDefenses = populateDefenseEvidence(correctedThreats, multiPv)
    computeAggregates(withDefenses, multiPv, phase1)

  /**
   * Extract opponent threats from L2 motifs. Labeling-only pass:
   * sets lossIfIgnoredCp = 0 (actual severity comes from MultiPV delta in correctWithMultiPv).
   */
  private def extractOpponentThreats(motifs: List[Motif], isWhiteToMove: Boolean): List[Threat] =
    motifs.flatMap { motif =>
      val motifColor = getMotifColor(motif)
      val isOpponentMotif = motifColor.exists { color =>
        (isWhiteToMove && color == Color.Black) || (!isWhiteToMove && color == Color.White)
      }
      
      if !isOpponentMotif then None
      else
        val motifName = motif.getClass.getSimpleName.replace("$", "")
        val lower = motifName.toLowerCase
        
        // Classify threat kind from motif type directly
        val kind = 
          if lower.contains("mate") || lower.contains("checkmate") || lower == "backrankweakness" then ThreatKind.Mate
          else if isTacticalMotifName(lower) then ThreatKind.Material
          else ThreatKind.Positional
        
        // Tactical motifs are immediate (1-2 turns), positional are strategic (3+ turns)
        val turnsToImpact = if kind == ThreatKind.Positional then 3 else 1
        
        Some(Threat(
          kind = kind,
          lossIfIgnoredCp = 0, // Actual severity will be set by correctWithMultiPv
          turnsToImpact = turnsToImpact,
          motifs = List(motifName),
          attackSquares = extractAttackSquares(motif),
          targetPieces = extractTargetPieces(motif),
          bestDefense = None,
          defenseCount = 0
        )).filter(_ => kind == ThreatKind.Mate || kind == ThreatKind.Material) // Drop pure positional labels (no value)
    }

  /** Tactical motif names that indicate material-level threats. */
  private def isTacticalMotifName(lower: String): Boolean =
    List("fork", "pin", "skewer", "discovered", "deflection", "overloading",
         "trappedpiece", "interference", "decoy", "zwischenzug", "doublecheck")
      .exists(lower.contains)

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
      // NOTE: These are included for completeness but typically produce
      // ThreatKind.Positional labels, which are filtered out in extractOpponentThreats.
      case m: Motif.OpenFileControl => Some(m.color)
      case m: Motif.WeakBackRank => Some(m.color)
      case m: Motif.SpaceAdvantage => Some(m.color)
      // Fallback for any other motifs without color field
      case _ => None

  private def extractAttackSquares(motif: Motif): List[String] =
    motif match
      case m: Motif.Fork => List(m.square.key)
      case m: Motif.Check => List(m.targetSquare.key)
      case m: Motif.Capture => List(m.square.key)
      case m: Motif.Deflection => List(m.fromSquare.key)
      case m: Motif.Decoy => List(m.toSquare.key)
      case m: Motif.Pin => m.pinnedSq.map(_.key).toList
      case m: Motif.Skewer => m.frontSq.map(_.key).toList
      case m: Motif.DiscoveredAttack => m.targetSq.map(_.key).toList
      case _ => Nil

  private def extractTargetPieces(motif: Motif): List[String] =
    motif match
      case m: Motif.Fork => m.targets.map(_.name)
      case m: Motif.Pin => List(m.pinnedPiece.name, m.targetBehind.name)
      case m: Motif.Skewer => List(m.frontPiece.name, m.backPiece.name)
      case m: Motif.DiscoveredAttack => List(m.target.name)
      case _: Motif.Check => List("King")
      case m: Motif.Capture => List(m.captured.name)
      case _ => Nil

  /**
   * FIX #1 & #5: Correct MultiPV detection
   * - Use UCI pattern matching (not SAN)
   * - Use SIGNED eval delta (negative = opponent threat)
   * - Classify threats correctly based on mate signals
   */
  private def correctWithMultiPv(
    threats: List[Threat],
    multiPv: List[PvLine],
    isWhiteToMove: Boolean,
    fen: String
  ): List[Threat] =
    if multiPv.size < 2 then
      // Insufficient data - return base threats
      threats
    else
      val pv1 = multiPv.head
      val pv2 = multiPv(1)
      // Positive delta = PV1 is better than PV2 (loss if we don't play PV1)
      val signedDelta = if isWhiteToMove then pv1.score - pv2.score else pv2.score - pv1.score
      
      // If PV2 is significantly worse, opponent has a threat
      val evalLoss = signedDelta.max(0)
      // For other captures, rely on evalLoss threshold instead
      val pv2FirstMove = pv2.moves.headOption
      val pv2IsPawnCapture = pv2FirstMove.exists(isPawnDiagonalCapture)
      val pv2IsMate = pv2.mate.exists(_ < 0)  // Negative mate = opponent mates us
      
      // Detect threat if: mate, pawn capture, OR significant eval loss
      val hasSignificantThreat = pv2IsMate || pv2IsPawnCapture || evalLoss >= MATERIAL_THREAT_THRESHOLD
      
      // P1 FIX: High threshold for implied threats in quiet positions (noise reduction)
      val totalPieces = chess.format.Fen.read(chess.variant.Standard, chess.format.Fen.Full(fen)).map(_.board.occupied.count).getOrElse(32)
      val isSuppressedOpening = totalPieces >= 31 // Start position has 32 pieces
      
      if hasSignificantThreat && evalLoss >= 150 && !isSuppressedOpening then
        val kind = if pv2IsMate then ThreatKind.Mate
                   else if evalLoss >= MATERIAL_THREAT_THRESHOLD then ThreatKind.Material
                   else ThreatKind.Positional
        
        // Extract attack square from PV2's first move (destination) or fallback to best defense destination
        val pv2DestSquare = pv2FirstMove.flatMap(m => if m.length >= 4 then Some(m.substring(2, 4)) else None)
        val pv1DestSquare = pv1.moves.headOption.flatMap(m => if m.length >= 4 then Some(m.substring(2, 4)) else None)
        val attackSquaresList = pv2DestSquare.toList ++ pv1DestSquare.toList
        
        val impliedThreat = Threat(
          kind = kind,
          lossIfIgnoredCp = if pv2IsMate then 10000 else evalLoss,
          turnsToImpact = 1,
          motifs = List("PvDelta"),
          attackSquares = attackSquaresList.distinct.take(1), // Keep only first valid square
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
      depthReliable = avgDepth >= MIN_DEPTH_FOR_RELIABILITY
    )

  /**
   * FIX #3: Actually populate defense evidence from MultiPV
   */
  private def populateDefenseEvidence(
    threats: List[Threat],
    multiPv: List[PvLine]
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
          lossIfIgnoredCp = (t.lossIfIgnoredCp * reliabilityFactor).toInt
        )
      }

  private def computeAggregates(
    threats: List[Threat],
    multiPv: List[PvLine],
    phase1: PositionClassification
  ): ThreatAnalysis =
    val maxLoss = threats.map(_.lossIfIgnoredCp).maxOption.getOrElse(0)
    val hasMate = threats.exists(_.kind == ThreatKind.Mate)
    val hasImmediate = threats.exists(_.isImmediate)
    val hasStrategic = threats.exists(_.isStrategic)
    val thresholds = adjustThresholds(phase1, multiPv) // Re-calculate thresholds inside
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
    val totalDefenses = threats.map(_.defenseCount).sum
    val resourceAvailable = threats.isEmpty || totalDefenses > 0
    
    val primaryDriver = 
      if hasMate then "mate_threat"
      else if maxLoss >= MATERIAL_THREAT_THRESHOLD then "material_threat"
      else if threats.nonEmpty then "positional_threat"
      else "no_threat"
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
  // HELPER: Empty analysis for no-threat positions

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
