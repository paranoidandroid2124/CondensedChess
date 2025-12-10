package chess
package analysis

import munit.FunSuite

class FeatureExtractorTest extends FunSuite:

  // --- Test Positions (FENs) ---

  // 1. IQP (Isolated Queen Pawn) for White at d4
  // Position: White d4, e3; Black d5, e6. White has no c or e pawns.
  // Actually, standard IQP: d4, no c/e pawns.
  // FEN: rnbqkb1r/pp2pppp/2p2n2/8/3P4/2N2N2/PPP2PPP/R1BQKB1R b KQkq - 0 5
  // Wait, let's construct a cleaner one.
  val fenIQP = "rnbqkbnr/pp2pppp/8/2p5/3P4/8/PPP2PPP/RNBQKBNR w KQkq - 0 4" 
  // d4 is isolated? c2 exist? No, usually IQP means d4 is alone.
  // Let's use a synthetic FEN for pure testing.
  val fenPureIQP = "4k3/8/8/8/3P4/8/8/4K3 w - - 0 1"

  // 2. Passed Pawn
  val fenPassed = "4k3/8/8/4P3/8/8/8/4K3 w - - 0 1"

  // 3. Backward Pawn (e.g. c6 backward on semi-open file)
  // White: c4, d4. Black: c6, d5. 
  // If Black has c6, and white has d4, and Black has no b-pawn.
  val fenBackward = "8/8/8/3P4/2P5/2p5/8/4K1k1 b - - 0 1" 

  // 4. King Safety (Castled vs Exposed)
  val fenSafety = "rnbq1rk1/pppp1ppp/8/8/4P3/8/PPPP1PPP/RNBQK2R w KQ - 0 1"

  // 5. Development (Undeveloped Minors)
  // White: Knight at g1, Bishop at f1. (2 undeveloped)
  val fenDev = "rnbq1rk1/pppp1ppp/5n2/4p3/4P3/5N2/PPPP1PPP/RNBQKB1R w KQ - 4 3"

  // 6. Space (Enemy Camp)
  // White Knight at d6.
  val fenSpace = "r1bqkbnr/pppp1ppp/2nN4/4p3/4P3/8/PPPP1PPP/RNBQKBNR b KQkq - 1 3"

  // 7. Coordination (Battery)
  // White Queen at d1, Bishop at g4? No, battery usually means attacking same line.
  // Queen at d2, Bishop at c1 (diagonal).
  val fenBattery = "rnbqkbnr/pppp1ppp/8/8/4p3/3P4/PPPQPPPP/RNB1KBNR w KQkq - 0 3" 
  
  // 8. Tactics (Hanging Piece)
  // White Knight at h3 is attacked by Black Bishop at c8? No.
  // Let's make a clear hanging piece. White Rook at e4, Black Pawn at d5 attacking it.
  val fenHanging = "rnbqkbnr/ppp1pppp/8/3p4/4R3/8/PPPPPPPP/RNBQKBN1 w KQkq - 0 1"

  test("Extract Pawn Structure - IQP") {
    // White d4, no c3/e3. 
    val f = FeatureExtractor.extractPositionFeatures(fenPureIQP, 1)
    val p = f.pawns
    assert(p.whiteIQP, "White should have IQP at d4")
    assertEquals(p.whiteIsolatedPawns, 1)
    assertEquals(p.blackPawnCount, 0)
  }

  test("Extract Pawn Structure - Passed Pawn") {
    val f = FeatureExtractor.extractPositionFeatures(fenPassed, 1)
    assertEquals(f.pawns.whitePassedPawns, 1)
    assertEquals(f.pawns.blackPassedPawns, 0)
  }

  test("Extract King Safety - Castling") {
    val f = FeatureExtractor.extractPositionFeatures(fenSafety, 1)
    
    // castling rights from FEN
    // castling rights from FEN
    // in fenSafety: "w KQ - 0 1". King at e1? RNBQK2R. K is e1.
    // Logic says: if k & q then "can_castle_both".
    assertEquals(f.kingSafety.whiteCastlingState, "can_castle_both")
    
    // Shield
    // White pawns at f2, g2, h2.
    // King at e1. Shield counts pawns *protecting* castled king? 
    // Logic: kingPosOf(color). If at e1, shield is f2, e2, d2?
    // Code check: `kSq.kingAttacks & board.pawns`.
    // e1 attacks d2, e2, f2 ...
    // In fenSafety, pawns are at f2, g2, h2, e4?? "PPPP1PPP". 
    // Row 2: P P P P _ P P P. a2 b2 c2 d2 _ f2 g2 h2.
    // e4 is pushed.
    // King at e1 attacks d2, e2(empty), f2.
    // So shield should be 2 (d2, f2).
    assertEquals(f.kingSafety.whiteKingShieldPawns, 2)
  }

  test("Extract Activity - Mobility") {
    val startFen = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1"
    val f = FeatureExtractor.extractPositionFeatures(startFen, 1)
    // Initial position mobility
    // White Knights: b1->a3,c3; g1->f3,h3. Total 4.
    assertEquals(f.activity.whiteMinorPieceMobility, 4)
  }

  test("Extract Material & Phase") {
    val startFen = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1"
    val f = FeatureExtractor.extractPositionFeatures(startFen, 1)
    
    assertEquals(f.materialPhase.whiteMaterial, 39) // 8P(8) + 2N(6) + 2B(6) + 2R(10) + Q(9) = 39
    assertEquals(f.materialPhase.phase, "opening")
  }
  
  test("Extract Development - Undeveloped") {
    val f = FeatureExtractor.extractPositionFeatures(fenDev, 1)
    // White N at b1, B at c1, B at f1, N at g1? In fenDev: "RNBQKB1R". 
    // Wait, fenDev is "rnbq1rk1/pppp1ppp/5n2/4p3/4P3/5N2/PPPP1PPP/RNBQKB1R w KQ - 4 3"
    // White: R(a1), N(b1), B(c1), Q(d1), K(e1), B(f1), R(h1). 
    // Undeveloped minors: N(b1), B(c1), B(f1). N(g1) moved to f3.
    // So 3 undeveloped minors.
    assertEquals(f.development.whiteUndevelopedMinors, 3)
  }

  test("Extract Space - Enemy Camp") {
    val f = FeatureExtractor.extractPositionFeatures(fenSpace, 1)
    // White Knight at d6. (Rank 6). White enemy camp is 5-8.
    // So 1 piece in enemy camp.
    assertEquals(f.space.whiteEnemyCampPresence, 1)
  }

  test("Extract Tactics - Hanging Piece") {
    val f = FeatureExtractor.extractPositionFeatures(fenHanging, 1)
    // White Rook at e4. Black Pawn at d5 attacks e4.
    // e4 rook has 0 defenders?
    // "RNBQKBN1". e1 is King. d1 Queen. No piece at e2/e3 protecting e4.
    // So e4 Rook is hanging.
    assertEquals(f.tactics.whiteHangingPieces, 1)
  }

  test("Geometry - Rook Behind Passed Pawn") {
    // White passed pawn on e5. White Rook on e1 (behind).
    // FEN: r7/8/8/4P3/8/8/8/4R1K1 w - - 0 1
    val fen = "r7/8/8/4P3/8/8/8/4R1K1 w - - 0 1"
    val f = FeatureExtractor.extractPositionFeatures(fen, 1)
    
    // Check passed pawn count first
    assertEquals(f.pawns.whitePassedPawns, 1, "Should have 1 passed pawn")
    
    // Check rook behind
    // e1 is rank 1. e5 is rank 5. White rook behind white pawn.
    // Logic: white rook rank < white pawn rank. 1 < 5. Yes.
    assertEquals(f.coordination.whiteRooksBehindPassedPawns, 1, "White Rook should be behind passed pawn")
  }

  test("Geometry - Wrong Bishop Draw") {
    // 1. Wrong Bishop: h-pawn (promotes on h8=Dark). Bishop on g2 (Light).
    // FEN: 8/7P/8/8/8/8/6B1/7k w - - 0 1 -> Missing White King!
    // Fixed FEN: 8/7P/8/8/8/8/6B1/K6k w - - 0 1 (White King at a1)
    val fenWrong = "8/7P/8/8/8/8/6B1/K6k w - - 0 1"
    val fWrong = FeatureExtractor.extractPositionFeatures(fenWrong, 1)
    assert(fWrong.geometry.whiteWrongBishop, "Should be wrong bishop (Light bishop vs Dark h8)")

    // 2. Right Bishop: a-pawn (promotes on a8=Light). Bishop on c4 (Light).
    // FEN: 8/P7/8/8/2B5/8/8/k7 w - - 0 1 -> Missing White King!
    // Fixed FEN: 8/P7/8/8/2B5/8/8/k6K w - - 0 1 (White King at h1)
    val fenRight = "8/P7/8/8/2B5/8/8/k6K w - - 0 1"
    val fRight = FeatureExtractor.extractPositionFeatures(fenRight, 1)
    assert(!fRight.geometry.whiteWrongBishop, "Should NOT be wrong bishop (Light bishop vs Light a8)")
  }
