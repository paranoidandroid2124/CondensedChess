package lila.llm.analysis

import munit.FunSuite
import lila.llm.analysis._
import chess.variant.Standard
import chess.format.Fen
import chess.Color

class Layer1VerificationTest extends FunSuite {

  // Helper to extract and print features for expert review
  def verifyFeatures(fen: String, title: String): Unit = {
    println(s"\n=== $title ===")
    println(s"FEN: $fen")
    
    // Create board/position from FEN to verify raw data against Features
    val position = Fen.read(Standard, Fen.Full(fen)).getOrElse(throw new RuntimeException("Invalid FEN"))
    val board = position.board
    
    // Extract features using the main entry point
    val features = PositionAnalyzer.extractFeatures(fen, 20).getOrElse(throw new RuntimeException("Failed to extract"))
    
    // --- Detailed Verification Output ---
    val wPawns = board.pawns & board.white
    val bPawns = board.pawns & board.black
    
    // 1. Pawn Structure (Lists)
    val wIso = PositionAnalyzer.isolatedPawns(wPawns)
    val bIso = PositionAnalyzer.isolatedPawns(bPawns)
    val wDbl = PositionAnalyzer.doubledPawns(wPawns)
    val bDbl = PositionAnalyzer.doubledPawns(bPawns)
    val wBackward = PositionAnalyzer.backwardPawns(Color.White, wPawns, bPawns, board)
    val bBackward = PositionAnalyzer.backwardPawns(Color.Black, bPawns, wPawns, board)
    val wPassed = PositionAnalyzer.passedPawns(Color.White, wPawns, bPawns)
    val bPassed = PositionAnalyzer.passedPawns(Color.Black, bPawns, wPawns)
    
    println(s"### 1. Pawn Structure")
    println(s"- Isolated: W=${wIso.size} ${wIso.mkString(", ")} | B=${bIso.size} ${bIso.mkString(", ")}")
    println(s"- Backward: W=${wBackward.size} ${wBackward.mkString(", ")} | B=${bBackward.size} ${bBackward.mkString(", ")}")
    println(s"- Passed:   W=${wPassed.size} ${wPassed.mkString(", ")} | B=${bPassed.size} ${bPassed.mkString(", ")}")
    println(s"- Doubled:  W=${wDbl.size} ${wDbl.mkString(", ")} | B=${bDbl.size} ${bDbl.mkString(", ")}")
    
    // 2. Activity (L1 Atomic - Raw mobility, not Trapped interpretation)
    println(s"### 2. Activity (L1 Atomic)")
    println(s"- PseudoMobility: W=${features.activity.whitePseudoMobility} B=${features.activity.blackPseudoMobility}")
    println(s"- LowMobPieces:   W=${features.activity.whiteLowMobilityPieces} B=${features.activity.blackLowMobilityPieces}")  
    println(s"- AttackedPieces: W=${features.activity.whiteAttackedPieces} B=${features.activity.blackAttackedPieces}")
    println(s"- DevLag:         W=${features.activity.whiteDevelopmentLag} B=${features.activity.blackDevelopmentLag}")

    // 3. King Safety
    println(s"### 3. King Safety")
    println(s"- Castling Rights: W=${features.kingSafety.whiteCastlingRights} B=${features.kingSafety.blackCastlingRights}")
    println(s"- Castled Side:    W=${features.kingSafety.whiteCastledSide} B=${features.kingSafety.blackCastledSide}")
    println(s"- Shield:          W=${features.kingSafety.whiteKingShield} B=${features.kingSafety.blackKingShield}")
    println(s"- Exposed Files:   W=${features.kingSafety.whiteKingExposedFiles} B=${features.kingSafety.blackKingExposedFiles}")
    
    println("-" * 50)
  }

  // --- Test Cases ---

  test("1. IQP Test (Isolated Queen Pawn)") {
    // White d4 is isolated. No c or e pawns for white.
    val fen = "rnbqk2r/pp2bppp/4pn2/8/3P4/2N2N2/PP3PPP/R1BQ1RK1 w KQkq - 0 8"
    verifyFeatures(fen, "IQP Test")
    
    // Assertions
    val f = PositionAnalyzer.extractFeatures(fen, 10).get
    assert(f.pawns.whiteIQP, "White should have IQP")
    assert(!f.pawns.blackIQP, "Black should not have IQP")
  }

  test("2. Hanging Pawns Test") {
    // White pawns on c4, d4. No b or e pawns.
    val fen = "r2q1rk1/p4ppp/1p2pn2/8/2PP4/5N2/P4PPP/R2Q1RK1 w - - 0 14"
    verifyFeatures(fen, "Hanging Pawns Test")
    
    val f = PositionAnalyzer.extractFeatures(fen, 14).get
    assert(f.pawns.whiteHangingPawns, "White should have hanging pawns")
    assert(!f.pawns.blackHangingPawns, "Black should not have hanging pawns")
  }

  test("3. Backward Pawn Test (Sveshnikov/Botvinnik System Structure)") {
    // Black pawn on d6 is backward (behind c5/e5, semi-open file if d-pawn blocked?)
    // Actually the classical backward pawn is on a semi-open file.
    // "r1bqk2r/1p2bppp/p1np1n2/2p1p3/2P1P3/2N2N2/PP1PBPPP/R1BQ1RK1 w kq - 0 8"
    // Black d6 is backward:
    // 1. Adjacent c5, e5 exist (friendly)
    // 2. Adjacent c5, e5 are on rank 5 (d6 is rank 6). Wait, for black, bigger rank is "behind"? No, rank 6 is behind rank 5. 
    //    Black d6 (rank index 5), c5/e5 (rank index 4). 5 > 4. So d6 is "behind" c5/e5. YES.
    // 3. No friendly pawn ahead on d-file? True.
    // 4. Stop square d5 (rank 4) is empty? Yes. Attacked by White e4? Yes.
    val fen = "r1bqk2r/1p2bppp/p1np1n2/2p1p3/2P1P3/2N2N2/PP1PBPPP/R1BQ1RK1 w kq - 0 8"
    verifyFeatures(fen, "Backward Pawn Test (d6)")
    
    val f = PositionAnalyzer.extractFeatures(fen, 10).get
    assert(f.pawns.blackBackwardPawns > 0, "Black should have at least 1 backward pawn (d6)")
  }

  test("4. Passed Pawn & Promotion Rank") {
    // White a7 is a passed pawn, rank 7 (index 6).
    val fen = "8/P7/8/2k5/8/5b2/4K3/8 w - - 0 1"
    verifyFeatures(fen, "Passed Pawn Rank Test")
    
    val f = PositionAnalyzer.extractFeatures(fen, 10).get
    assert(f.pawns.whitePassedPawns > 0, "White should have passed pawn")
    assert(f.pawns.whitePassedPawnRank == 6, s"White passed pawn rank should be 6 (7th rank), was ${f.pawns.whitePassedPawnRank}")
  }

  test("5. Locked Center (French Advance)") {
    // White d4, e5. Black d5, e6.
    // Locked: d4 blocked by d5, e5 blocked by e6? No, e4 blocked by e5. 
    // Locked: d4 blocked by d5, e5 blocked by e6? No, e4 blocked by e5. 
    // In French Advance: White e5, Black e6. Files E. Ranks: W=e5(4), B=e6(5). Not blocked directly.
    // Definition of lockedCenter: wOnD4 && bOnD5 || wOnE4 && bOnE5
    // Let's create a strictly locked center structure: wD4/e4 vs bD5/e5
    // Fixed FEN: removed d7 pawn (User reported 9 pawns). Moved e7->e5.
    // rnbqkbnr/pp1p1ppp/8/3pp3/3PP3/8/PPP2PPP/RNBQKBNR (8 Black Pawns: a7,b7,c7,d7,f7,g7,h7(7) + e5(1) + d5(1) = 9? No.)
    // Wait. d5, e5. (2). a7,b7,c7 (3). d7 (1). f7,g7,h7 (3). Total 9.
    // Correct FEN should have d7 and e7 removed if they moved to d5, e5. 
    // FEN: "rnbqkbnr/ppp2ppp/8/3pp3/3PP3/8/PPP2PPP/RNBQKBNR w KQkq - 0 2"
    // a,b,c (3). f,g,h (3). d5,e5 (2). Total 8.
    val fen = "rnbqkbnr/ppp2ppp/8/3pp3/3PP3/8/PPP2PPP/RNBQKBNR w KQkq - 0 2"
    verifyFeatures(fen, "Locked Center Test")
    
    val f = PositionAnalyzer.extractFeatures(fen, 2).get
    assert(f.centralSpace.lockedCenter, "Center should be locked")
  }

  test("6. King Safety (Attacked Ring)") {
    // White King e1. Black Queen h4, Knight f6 attacking around e1?
    val fen = "rnb1k2r/pppp1ppp/5n2/4p3/2B1P2q/5N2/PPPP1bPP/RNBQK2R w KQkq - 2 5"
    verifyFeatures(fen, "King Safety / Early Attack")
    
    val f = PositionAnalyzer.extractFeatures(fen, 5).get
    assert(f.kingSafety.whiteAttackersCount > 0, "White king should have attackers")
    assert(f.kingSafety.whiteKingRingAttacked > 0, "White king ring should be attacked")
    // New Shield Logic Check: e4 pawn is too far (Rank 3 vs King Rank 0), f-file open. 
    // So e and f files should be exposed.
    assert(f.kingSafety.whiteKingExposedFiles >= 2, "Should detect exposed files due to advanced/missing pawns")
  }

  test("7. Line Control & File Ownership (Alekhine's Gun)") {
    // White rooks doubled on open d-file, Black has semi-open c-file
    val fen = "2r2rk1/pp1b1ppp/1q2pn2/3p4/3P4/2N2N2/PP1QPPPP/R2R2K1 w - - 0 1"
    verifyFeatures(fen, "Line Control")
    val f = PositionAnalyzer.extractFeatures(fen, 20).get
    assert(f.lineControl.openFilesCount >= 1, "Should have open files (c-file)")
    assert(f.lineControl.whiteSemiOpenFiles == 0, "White has no semi-open files") // c is open, d is closed
    assert(f.lineControl.blackSemiOpenFiles == 0, "Black has no semi-open files (c is fully open)") 
  }

  test("8. Opposite Castling Race (Sicilian Dragon)") {
    // White Long Castling, Black Short Castling
    val fen = "r1r3k1/pp1bppbp/3p1np1/8/3NPPP1/2N1B3/PPPQ3P/2KR3R b - - 0 12"
    verifyFeatures(fen, "Opposite Castling") 
    val f = PositionAnalyzer.extractFeatures(fen, 24).get
    // Check castling rights (none because castled?) AND castling state (side)
    // In FEN 'b - -', castling rights are gone. So rights="none".
    // But actual state is Castled.
    assert(f.kingSafety.whiteCastledSide == "long", "White should be classified as Long Castled")
    assert(f.kingSafety.blackCastledSide == "short", "Black should be classified as Short Castled")
    
    // Check centrality/tension in such sharp positions
    assert(f.centralSpace.pawnTensionCount > 0 || f.activity.whiteLegalMoves > 20, "Should handle complex position")
    // Sicilian c-file is semi-open for Black (Has White c-pawn, No Black c-pawn)
    assert(f.lineControl.blackSemiOpenFiles >= 1, "Black should have semi-open c-file")
  }

  test("9. Fortress / Blockade (Closed Ruy Lopez)") {
    val fen = "r1bq1rk1/2p1bppp/p1np1n2/1p2p3/4P3/1BP2N2/PP1P1PPP/RNBQR1K1 w - - 0 9"
    // Locked center e4/e5. 
    verifyFeatures(fen, "Fortress/Blockade")
    val f = PositionAnalyzer.extractFeatures(fen, 18).get
    assert(f.centralSpace.lockedCenter, "Center should be locked (e4 vs e5)")
  }
}
// Force Re-Run
