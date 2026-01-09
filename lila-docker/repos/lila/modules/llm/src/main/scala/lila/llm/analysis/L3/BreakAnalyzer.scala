package lila.llm.analysis.L3

import chess._
import chess.format.Fen
import chess.variant.Standard
import chess.Color
import lila.llm.analysis.PositionFeatures
import lila.llm.analysis.PositionAnalyzer
import lila.llm.model.Motif

/**
 * Phase 3: Break & Pawn Play Analyzer
 * 
 * Analyzes pawn structure to extract 10 strategic concepts:
 * 1. pawnBreakReady - Is a break immediately available?
 * 2. breakFile - Which file has the break?
 * 3. breakImpact - Estimated cp gain from break
 * 4. advanceOrCapture - Must resolve tension now?
 * 5. passedPawnUrgency - How urgent is passed pawn push?
 * 6. passerBlockade - Is passed pawn blocked?
 * 7. pusherSupport - Does passer have rook/king support?
 * 8. minorityAttack - Is queenside minority attack ready?
 * 9. counterBreak - Does opponent have counter-break?
 * 10. tensionPolicy - Maintain/Release/Ignore?
 */
object BreakAnalyzer:

  // ============================================================
  // CONFIGURATION
  // ============================================================
  
  private val HIGH_TENSION_THRESHOLD = 2      // Tension squares to force resolution
  private val CRITICAL_PASSER_RANK_WHITE = 6  // Rank 7 = critical for white
  private val CRITICAL_PASSER_RANK_BLACK = 3  // Rank 2 = critical for black (0-indexed: 1)
  private val MINORITY_ATTACK_FILES = Set(0, 1, 2)  // a, b, c files
  
  // ============================================================
  // MAIN ENTRY POINT
  // ============================================================

  /**
   * Analyze pawn play concepts from position features.
   * 
   * @param features L1 position features
   * @param motifs L2 motifs (for PawnBreak detection)
   * @param phase1 Phase 1 classification (for context)
   * @param sideToMove Which side is to move
   * @return Complete pawn play analysis with 10 concepts
   */
  def analyze(
    features: PositionFeatures,
    motifs: List[Motif],
    phase1: PositionClassification,
    sideToMove: String = "white"
  ): PawnPlayAnalysis =
    val isWhite = sideToMove.equalsIgnoreCase("white")
    
    // Parse board from FEN to get access to raw bitboards/piece locations
    val board = Fen.read(Standard, Fen.Full(features.fen)).map(_.board).getOrElse(Board.empty)
    
    // Step 1: Analyze break opportunities (Concepts 1-3)
    val (breakReady, breakFile, breakImpact) = analyzeBreaks(features, motifs, isWhite)
    
    // Step 2: Check tension resolution (Concept 4)
    val advanceOrCapture = checkTensionResolution(features, phase1)
    
    // Step 3: Analyze passed pawns (Concepts 5-7)
    val (urgency, blockade, blockadeSq, blockadeRole, support) = analyzePassedPawns(features, board, isWhite)
    
    // Step 4: Strategic posture (Concepts 8-9)
    val minorityAttack = checkMinorityAttack(board, isWhite)
    val counterBreak = checkCounterBreak(features, motifs, isWhite)
    
    // Step 5: Tension policy (Concept 10)
    val tensionPolicy = computeTensionPolicy(features, phase1, breakReady, advanceOrCapture)
    
    // Step 6: Compute evidence and driver
    val tensionSquares = extractTensionSquares(board)
    val driver = computePrimaryDriver(breakReady, urgency, advanceOrCapture, features.centralSpace.pawnTensionCount)
    
    val tensionNote = if (tensionSquares.nonEmpty) s"Active tension: ${tensionSquares.mkString(", ")}" else "No tension"

    PawnPlayAnalysis(
      pawnBreakReady = breakReady,
      breakFile = breakFile,
      breakImpact = breakImpact,
      advanceOrCapture = advanceOrCapture,
      passedPawnUrgency = urgency,
      passerBlockade = blockade,
      blockadeSquare = blockadeSq,
      blockadeRole = blockadeRole,
      pusherSupport = support,
      minorityAttack = minorityAttack,
      counterBreak = counterBreak,
      tensionPolicy = tensionPolicy,
      tensionSquares = tensionSquares,
      primaryDriver = driver,
      notes = s"$tensionNote; Break: $breakReady, Passer: $urgency"
    )

  // ============================================================
  // CONCEPT 1-3: BREAK ANALYSIS
  // ============================================================

  private def analyzeBreaks(
    features: PositionFeatures,
    motifs: List[Motif],
    isWhite: Boolean
  ): (Boolean, Option[String], Int) =
    val central = features.centralSpace
    
    // Check for existing PawnBreak motifs from L2
    val pawnBreakMotifs = motifs.collect {
      case m: Motif.PawnBreak if colorMatches(m.color, isWhite) => m
    }
    
    if pawnBreakMotifs.nonEmpty then
      val primaryBreak = pawnBreakMotifs.head
      val file = primaryBreak.file.toString.toLowerCase
      val impact = estimateBreakImpact(features, file, isWhite)
      (true, Some(file), impact)
    else
      // Detect potential breaks from pawn structure
      detectPotentialBreak(features, isWhite)

  private def detectPotentialBreak(
    features: PositionFeatures,
    isWhite: Boolean
  ): (Boolean, Option[String], Int) =
    val central = features.centralSpace
    val pawns = features.pawns
    
    // Check for d4-d5 or e4-e5 type central breaks
    // If center is locked, no immediate break
    if central.lockedCenter then
      (false, None, 0)
    else if central.pawnTensionCount > 0 then
      // Tension exists - break is potentially ready
      // Heuristic: if we have central pawns and tension, break is ready
      val hasCentralPawns = if isWhite then 
        central.whiteCentralPawns > 0
      else 
        central.blackCentralPawns > 0
      
      if hasCentralPawns then
        val whiteStronger = features.centralSpace.whiteCenterControl > features.centralSpace.blackCenterControl
        // If we are white and stronger, we break on 'd' (d4 push vs e5), else 'e'
        // If we are black, we want to break where we are stronger
        val breakFile = if isWhite then
          if whiteStronger then "d" else "e"
        else
          if !whiteStronger then "d" else "e" // Black stronger in center -> d5
          
        (true, Some(breakFile), 100)
      else
        (false, None, 0)
    else
      (false, None, 0)

  private def estimateBreakImpact(features: PositionFeatures, file: String, isWhite: Boolean): Int =
    // Base impact from opening a file
    val baseImpact = 80
    
    // Bonus if we have rooks to use the file
    val rookBonus = if isWhite then
      if features.imbalance.whiteRooks > 0 then 50 else 0
    else
      if features.imbalance.blackRooks > 0 then 50 else 0
    
    // Bonus for attacking toward enemy king
    val kingAttackBonus = file match
      case "f" | "g" | "h" if isWhite => 30  // Kingside file, attacking black king
      case "a" | "b" | "c" if !isWhite => 30 // Queenside, attacking white queen
      case _ => 0
    
    baseImpact + rookBonus + kingAttackBonus

  // ============================================================
  // CONCEPT 4: TENSION RESOLUTION
  // ============================================================

  private def checkTensionResolution(
    features: PositionFeatures,
    phase1: PositionClassification
  ): Boolean =
    val tension = features.centralSpace.pawnTensionCount
    
    // Must resolve if:
    // 1. High tension count AND critical moment
    // 2. Forced sequence detected
    val isCritical = phase1.criticality.isCritical || phase1.criticality.isForced
    val highTension = tension >= HIGH_TENSION_THRESHOLD
    
    highTension && isCritical

  // ============================================================
  // CONCEPT 5-7: PASSED PAWN ANALYSIS
  // ============================================================

  private def analyzePassedPawns(
    features: PositionFeatures,
    board: Board,
    isWhite: Boolean
  ): (PassedPawnUrgency, Boolean, Option[Square], Option[Role], Boolean) = {
    val pawns = features.pawns
    
    val passedCount = if isWhite then pawns.whitePassedPawns else pawns.blackPassedPawns
    val passedRank = if isWhite then pawns.whitePassedPawnRank else pawns.blackPassedPawnRank
    val protectedPassed = if isWhite then pawns.whiteProtectedPassedPawns else pawns.blackProtectedPassedPawns
    
    if passedCount == 0 then
      (PassedPawnUrgency.Background, false, None, None, false)
    else
      // Concept 5: Urgency based on rank
      val urgency = computePasserUrgency(passedRank, isWhite)
      
      // Get actual passed pawn squares using PositionAnalyzer logic
      val color = if isWhite then Color.White else Color.Black
      val myPawns = board.byPiece(color, Pawn)
      val oppPawns = board.byPiece(!color, Pawn)
      val actualPassedPawns = PositionAnalyzer.passedPawns(color, myPawns, oppPawns)
      
      // Sort by advancement: most advanced first (White: highest rank, Black: lowest rank)
      val sortedPassers = if isWhite then actualPassedPawns.sortBy(-_.rank.value)
                          else actualPassedPawns.sortBy(_.rank.value)
      
      // Find first blockaded passed pawn (prioritizing most advanced)
      val blockadeInfo = sortedPassers.view.flatMap { pSq =>
        val aheadRank = pSq.rank.value + (if isWhite then 1 else -1)
        Square.at(pSq.file.value, aheadRank).flatMap { sq =>
          board.pieceAt(sq).filter(_.color != color).map(p => (sq, p.role))
        }
      }.headOption
      
      val isBlocked = blockadeInfo.isDefined
      
      // Concept 7: Support detection
      val hasRookSupport = if isWhite then features.lineControl.whiteRookOn7th 
                          else features.lineControl.blackRookOn7th
      val detailedSupport = checkRookSupport(board, isWhite)
      val support = hasRookSupport || detailedSupport || protectedPassed > 0

      (urgency, isBlocked, blockadeInfo.map(_._1), blockadeInfo.map(_._2), support)
    }

  private def checkRookSupport(board: Board, isWhite: Boolean): Boolean =
    val color = if isWhite then Color.White else Color.Black
    val oppColor = !color
    val myPawns = board.byPiece(color, Pawn)
    val oppPawns = board.byPiece(oppColor, Pawn)
    
    // Find passed pawns manually to check their files
    val passedPawns = myPawns.squares.filter { pSq =>
      // Check files: p.file, p.file-1, p.file+1
      val fileRange = (math.max(0, pSq.file.value - 1) to math.min(7, pSq.file.value + 1))
      // Check ranks ahead
      !oppPawns.squares.exists { opSq => 
        fileRange.contains(opSq.file.value) && 
        (if isWhite then opSq.rank.value > pSq.rank.value else opSq.rank.value < pSq.rank.value)
      }
    }
    
    // Check for rook behind any passed pawn
    val myRooks = board.byPiece(color, Rook)
    passedPawns.exists { pSq =>
      myRooks.squares.exists { rSq =>
        rSq.file == pSq.file && 
        (if isWhite then rSq.rank.value < pSq.rank.value else rSq.rank.value > pSq.rank.value)
      }
    }

  private def computePasserUrgency(rank: Int, isWhite: Boolean): PassedPawnUrgency =
    // CRITICAL DEPENDENCY WARNING:
    // This value 'rank' must be normalized by L1 (PositionAnalyzer) to represent "advancement".
    // Contract: 0 = Promotion Square (Not possible for pawn), 1 = Start Rank, 7 = Promotion Rank.
    // L1 Implementation verified: 
    //   White: rank.value.max (Index 1 to 7)
    //   Black: 7 - rank.value.min (Index 6->1 becomes 1; Index 1->6 becomes 6)
    // Thus, 'rank' is always 1 (start) to 7 (promotion).
    // DO NOT CHANGE L1 LOGIC WITHOUT UPDATING THIS.
    
    rank match
      case r if r >= 6 => PassedPawnUrgency.Critical   // Rank 7 or 8 (Index 6/7)
      case r if r >= 4 => PassedPawnUrgency.Important  // Rank 5 or 6 (Index 4/5)
      case r if r >= 2 => PassedPawnUrgency.Background // Rank 3 or 4 (Index 2/3)
      case _ => PassedPawnUrgency.Blocked              // Rank 2 (Index 1)

  // ============================================================
  // CONCEPT 8-9: STRATEGIC POSTURE
  // ============================================================

  private def checkMinorityAttack(board: Board, isWhite: Boolean): Boolean =
    val whitePawns = board.byPiece(Color.White, Pawn)
    val blackPawns = board.byPiece(Color.Black, Pawn)
    
    val myPawns = if isWhite then whitePawns else blackPawns
    val oppPawns = if isWhite then blackPawns else whitePawns
    
    // Queenside files: a, b, c
    val qSideMask = Bitboard.file(File.A) | Bitboard.file(File.B) | Bitboard.file(File.C)
    
    val myQSide = (myPawns & qSideMask).count
    val oppQSide = (oppPawns & qSideMask).count

    // Classic minority: we have fewer pawns (but > 0) than opponent on queenside
    // e.g. 2 vs 3
    myQSide > 0 && myQSide < oppQSide

  private def checkCounterBreak(
    features: PositionFeatures,
    motifs: List[Motif],
    isWhite: Boolean
  ): Boolean =
    // Check if opponent has a PawnBreak motif ready
    val opponentBreaks = motifs.collect {
      case m: Motif.PawnBreak if !colorMatches(m.color, isWhite) => m
    }
    
    opponentBreaks.nonEmpty

  // ============================================================
  // CONCEPT 10: TENSION POLICY
  // ============================================================

  private def computeTensionPolicy(
    features: PositionFeatures,
    phase1: PositionClassification,
    breakReady: Boolean,
    advanceOrCapture: Boolean
  ): TensionPolicy =
    val tension = features.centralSpace.pawnTensionCount
    val lockedCenter = features.centralSpace.lockedCenter
    
    // Item 1 fix: Don't ignore tension if tension > 0, even if center is "locked"
    if tension == 0 then
      TensionPolicy.Ignore
    else if advanceOrCapture || (breakReady && phase1.nature.isDynamic) then
      TensionPolicy.Release
    else if phase1.nature.isStatic then
      TensionPolicy.Maintain
    else
      TensionPolicy.Maintain  // Default: keep tension

  // ============================================================
  // EVIDENCE EXTRACTION
  // ============================================================

  private def extractTensionSquares(board: Board): List[String] =
    val whitePawns = board.byPiece(Color.White, Pawn)
    val blackPawns = board.byPiece(Color.Black, Pawn)
    
    // Find white pawns that attack black pawns
    val whiteAttacks = whitePawns.squares.flatMap { wSq =>
      val attacks = wSq.pawnAttacks(Color.White)
      (attacks & blackPawns).squares.map(_.key)
    }
    
    // Find black pawns that attack white pawns
    val blackAttacks = blackPawns.squares.flatMap { bSq =>
      val attacks = bSq.pawnAttacks(Color.Black)
      (attacks & whitePawns).squares.map(_.key)
    }
    
    (whiteAttacks ++ blackAttacks).toList.distinct

  private def computePrimaryDriver(
    breakReady: Boolean,
    urgency: PassedPawnUrgency,
    advanceOrCapture: Boolean,
    tensionCount: Int
  ): String =
    if urgency == PassedPawnUrgency.Critical then "passed_pawn"
    else if advanceOrCapture then "tension_critical"
    else if breakReady then "break_ready"
    else if tensionCount > 0 then "tension_active"
    else "quiet"

  // ============================================================
  // HELPERS
  // ============================================================

  private def colorMatches(motifColor: Color, isWhite: Boolean): Boolean =
    (isWhite && motifColor == Color.White) || (!isWhite && motifColor == Color.Black)

  // ============================================================
  // DEFAULT: No pawn play concerns
  // ============================================================

  def noPawnPlay: PawnPlayAnalysis = PawnPlayAnalysis(
    pawnBreakReady = false,
    breakFile = None,
    breakImpact = 0,
    advanceOrCapture = false,
    passedPawnUrgency = PassedPawnUrgency.Background,
    passerBlockade = false,
    blockadeSquare = None,
    blockadeRole = None,
    pusherSupport = false,
    minorityAttack = false,
    counterBreak = false,
    tensionPolicy = TensionPolicy.Ignore,
    tensionSquares = Nil,
    primaryDriver = "quiet",
    notes = "No significant pawn play"
  )
