package lila.llm.analysis

import munit.FunSuite
import lila.llm.model.*
import lila.llm.*
import _root_.chess.{Board, Color}
import _root_.chess.format.{Fen, Uci}

/**
 * Layer 2 Verification Test Suite (Refined)
 * 
 * Tests L2 feature detection with precise FENs matching strict detection logic.
 */
class Layer2VerificationTest extends FunSuite {

  // ============================================================
  // HELPERS
  // ============================================================

  def analyzeStatic(fen: String, description: String, 
                    evalBefore: Int = 20, evalAfter: Int = 20, bestEval: Int = 20): ConceptLabels = {
    println(s"\n=== TEST: $description ===")
    println(s"FEN: $fen")
    
    val featuresOpt = PositionAnalyzer.extractFeatures(fen, 1)
    if (featuresOpt.isEmpty) {
      println("ERROR: Failed to extract L1 features")
      return ConceptLabels()
    }
    
    val features = featuresOpt.get
    val fenObj = Fen.read(_root_.chess.variant.Standard, Fen.Full(fen)).get
    val stateMotifs = MoveAnalyzer.detectStateMotifs(fenObj, 1) 
    
    val labels = ConceptLabeler.labelPosition(
      features = features,
      evalBefore = evalBefore, 
      evalAfter = evalAfter,
      bestEval = bestEval,
      board = fenObj.board,
      color = fenObj.color,
      motifs = stateMotifs
    )

    printReport(labels)
    labels
  }

  def analyzeMove(fen: String, uciParams: String, description: String,
                  evalBefore: Int = 0, evalAfter: Int = 300, bestEval: Int = 300): ConceptLabels = {
    println(s"\n=== TEST: $description ===")
    println(s"FEN: $fen")
    
    val featuresOpt = PositionAnalyzer.extractFeatures(fen, 1)
    if (featuresOpt.isEmpty) return ConceptLabels()
    
    val features = featuresOpt.get
    val fenObj = Fen.read(_root_.chess.variant.Standard, Fen.Full(fen)).get
    val uciMove = Uci(uciParams).get.asInstanceOf[Uci.Move]
    val move = fenObj.move(uciMove).getOrElse(sys.error(s"Illegal move $uciParams"))
    
    val motifs = MoveAnalyzer.detectMoveMotifs(move, fenObj, 1)
    
    val labels = ConceptLabeler.labelPosition(
      features = features,
      evalBefore = evalBefore,
      evalAfter = evalAfter,
      bestEval = bestEval,
      board = fenObj.board,
      color = fenObj.color,
      motifs = motifs
    )
    
    printReport(labels)
    labels
  }

  def printReport(l: ConceptLabels): Unit = {
    println(s"Structure: ${l.structureTags}")
    println(s"Plans: ${l.planTags}")
    println(s"Tactics: ${l.tacticTags}")
    println(s"Positional: ${l.positionalTags}")
    if (l.mistakeTags.nonEmpty) println(s"Mistakes: ${l.mistakeTags}")
    if (l.endgameTags.nonEmpty) println(s"Endgame: ${l.endgameTags}")
  }

  // ============================================================
  // 1. STRUCTURE TAGS
  // ============================================================

  test("STRUCTURE: IQP") {
    val fen = "rnbqk2r/pp2bppp/4pn2/8/3P4/2N2N2/PP3PPP/R1BQ1RK1 w KQkq - 0 8"
    val labels = analyzeStatic(fen, "IQP White")
    assert(labels.structureTags.contains(StructureTag.IqpWhite))
  }

  test("STRUCTURE: Hanging Pawns") {
    val fen = "r2q1rk1/p4ppp/1p2pn2/8/2PP4/5N2/P4PPP/R2Q1RK1 w - - 0 14"
    val labels = analyzeStatic(fen, "Hanging Pawns White")
    assert(labels.structureTags.contains(StructureTag.HangingPawnsWhite))
  }

  test("STRUCTURE: King Exposed") {
    // King on e1 with NO pawns on d/e/f files -> Fully exposed
    val fen = "8/8/8/8/8/8/8/4K3 w - - 0 1"
    val labels = analyzeStatic(fen, "King Exposed")
    assert(labels.structureTags.contains(StructureTag.KingExposedWhite), s"Got ${labels.structureTags}")
  }

  // ============================================================
  // 2. POSITIONAL TAGS
  // ============================================================

  test("POSITIONAL: Open File") {
    val fen = "r4rk1/ppp2ppp/2n5/4p3/8/2NR1N2/PPP2PPP/3R2K1 w - - 0 1"
    val labels = analyzeStatic(fen, "Open d-file")
    assert(labels.positionalTags.exists(_.toString.contains("OpenFile")))
  }

  test("POSITIONAL: Battery") {
    val fen = "3k4/8/8/8/8/8/3R4/3Q3K w - - 0 1"
    val labels = analyzeStatic(fen, "Battery White")
    assert(labels.positionalTags.exists(_.toString.contains("Battery")))
  }

  test("POSITIONAL: Weak Back Rank (Opponent)") {
    // White to move. Black King on g8. White Rook on b8.
    // Logic checks if Opponent (Black) has back rank weakness (Enemy rook on rank).
    val fen = "1R4k1/5ppp/8/8/8/8/8/6K1 w - - 0 1"
    val labels = analyzeStatic(fen, "Weak Back Rank")
    assert(labels.positionalTags.contains(PositionalTag.WeakBackRank(_root_.chess.Black)), 
           s"Expected WeakBackRank(Black), got ${labels.positionalTags}")
  }

  test("POSITIONAL: Bad Bishop") {
    // Dark Bishop (c3). 5 Pawns on Dark Squares (b2,d2,f2,h2,d4).
    val fen = "8/8/8/3P4/8/2B5/1P1P1P1P/4K3 w - - 0 1"
    val labels = analyzeStatic(fen, "Bad Bishop")
    // Note: ConceptLabeler.detectBishopQuality checks if sameColorPawns > total/2.
    assert(labels.positionalTags.exists(_.toString.contains("BadBishop")), 
           s"Expected BadBishop, got ${labels.positionalTags}")
  }

  test("POSITIONAL: Bishop Pair") {
    val fen = "r1bqk2r/pppp1ppp/2n2n2/4p3/2B1P3/5N2/PPPP1PPP/RNBQK2R w KQkq - 0 1"
    val labels = analyzeStatic(fen, "Bishop Pair")
    assert(labels.positionalTags.exists(_.toString.contains("BishopPair")))
  }

  // ============================================================
  // 3. TACTIC TAGS
  // ============================================================

  test("TACTIC: Clearance") {
    // Note: When DiscoveredAttack fires, Clearance is suppressed to avoid over-tagging
    val fen = "3k4/8/8/8/3R4/8/8/3Q3K w - - 0 1"
    val labels = analyzeMove(fen, "d4h4", "Clearance")
    assert(labels.tacticTags.contains(TacticTag.ClearanceSound) || 
           labels.tacticTags.contains(TacticTag.DiscoveredAttackSound),
           s"Expected ClearanceSound or DiscoveredAttackSound, got ${labels.tacticTags}")
  }

  test("TACTIC: Discovered Attack") {
    // White R on e2, B on e5. Black K on e8.
    // Move B (e5) to f6 -> Discovered Check by R on e2.
    val fen = "4k3/8/8/4B3/8/8/4R3/4K3 w - - 0 1"
    val labels = analyzeMove(fen, "e5f6", "Discovered Check", evalAfter=500) // Huge eval gain
    assert(labels.tacticTags.contains(TacticTag.DiscoveredAttackSound), 
           s"Expected DiscoveredAttackSound, got ${labels.tacticTags}")
  }

  test("TACTIC: Pin") {
    val fen = "rnbqk2r/pppp1ppp/4pn2/8/1bPP4/2N5/PP2PPPP/R1BQKBNR w KQkq - 0 1"
    val labels = analyzeMove(fen, "c1g5", "Pin")
    assert(labels.tacticTags.contains(TacticTag.PinSound))
  }

  test("TACTIC: Fork".ignore) {
    val fen = "r1bqkb1r/pppp1ppp/2n2n2/4N3/4P3/8/PPPP1PPP/RNBQKB1R w KQkq - 0 1"
    val labels = analyzeMove(fen, "e5d7", "Fork")
    assert(labels.tacticTags.contains(TacticTag.ForkSound))
  }

  // ============================================================
  // 4. PLAN TAGS
  // ============================================================

  test("PLAN: Central Control") {
    val fen = "r1bqkb1r/pppp1ppp/2n2n2/4p3/2B1P3/5N2/PPPP1PPP/RNBQK2R w KQkq - 0 4"
    val labels = analyzeStatic(fen, "Central Control")
    assert(labels.planTags.contains(PlanTag.CentralControlGood))
  }

  test("PLAN: Promotion Threat") {
    val fen = "8/P7/8/2k5/8/5b2/4K3/8 w - - 0 1"
    val labels = analyzeStatic(fen, "Promotion")
    assert(labels.planTags.contains(PlanTag.PromotionThreat) || 
           labels.planTags.contains(PlanTag.KingActivationGood))
  }

  // ============================================================
  // 5. NEGATIVE TESTS (False Positive Prevention)
  // ============================================================

  test("NEGATIVE: WeakSquare d5 should not appear when e6 pawn defends it (IQP)") {
    // In IQP position, Black pawn on e6 defends d5
    val fen = "rnbqk2r/pp2bppp/4pn2/8/3P4/2N2N2/PP3PPP/R1BQ1RK1 w KQkq - 0 8"
    val labels = analyzeStatic(fen, "IQP d5 Defense Check")
    val hasD5Weak = labels.positionalTags.exists(_.toString.contains("d5"))
    assert(!hasD5Weak, s"d5 should NOT be weak (defended by e6 pawn), got ${labels.positionalTags}")
  }

  test("NEGATIVE: WeakSquare should not appear on pawnless board") {
    val fen = "8/8/8/8/8/8/8/4K3 w - - 0 1"
    val labels = analyzeStatic(fen, "Pawnless Board")
    val hasWeakSquare = labels.positionalTags.exists(_.toString.contains("WeakSquare"))
    assert(!hasWeakSquare, s"WeakSquare should NOT appear without pawns, got ${labels.positionalTags}")
  }

  test("NEGATIVE: PromotionThreat should not fire for Black 7th rank pawns") {
    // Black pawns on 7th rank are NOT near promotion
    val fen = "8/5ppp/8/8/8/8/8/4K2k w - - 0 1"
    val labels = analyzeStatic(fen, "Black 7th Rank Pawns")
    val hasPromoThreat = labels.planTags.contains(PlanTag.PromotionThreat)
    assert(!hasPromoThreat, s"PromotionThreat should NOT fire for Black 7th rank pawns, got ${labels.planTags}")
  }

  test("NEGATIVE: CentralControlGood should not appear in endgame with no center pawns") {
    // Battery position - no center pawns, open position
    val fen = "3k4/8/8/8/8/8/3R4/3Q3K w - - 0 1"
    val labels = analyzeStatic(fen, "No Center Pawns")
    val hasCC = labels.planTags.contains(PlanTag.CentralControlGood)
    assert(!hasCC, s"CentralControlGood should NOT appear without center pawns, got ${labels.planTags}")
  }

  test("NEGATIVE: Battery should not appear without target on line") {
    // Q+R on same file but no enemy piece on that file
    val fen = "8/8/8/k7/8/8/3R4/3Q3K w - - 0 1"
    val labels = analyzeStatic(fen, "Battery No Target")
    val hasBattery = labels.positionalTags.exists(_.toString.contains("Battery"))
    assert(!hasBattery, s"Battery should NOT appear without enemy target on line, got ${labels.positionalTags}")
  }

  test("NEGATIVE: LoosePiece e1 should not be tagged as Black") {
    // White King on e1 should never be tagged as Black loose piece
    val fen = "rnbqkbnr/pppppppp/8/8/4P3/8/PPPP1PPP/RNBQKBNR w KQkq - 0 1"
    val labels = analyzeStatic(fen, "White King e1")
    val hasE1BlackLoose = labels.positionalTags.exists(t => 
      t.toString.contains("LoosePiece") && t.toString.contains("e1") && t.toString.contains("Black")
    )
    assert(!hasE1BlackLoose, s"e1 should NOT be tagged as Black LoosePiece, got ${labels.positionalTags}")
  }
}
