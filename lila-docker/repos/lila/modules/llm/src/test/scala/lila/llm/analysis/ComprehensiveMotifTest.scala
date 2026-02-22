package lila.llm.analysis

import munit.FunSuite
import chess.{Board, Color}
import chess.format.Fen
import lila.llm.model._
import lila.llm.analysis.FactExtractor
import lila.llm.model.strategic._

class ComprehensiveMotifTest extends FunSuite {

  // Helper to access private detectStateMotifs via reflection
  private def getStateMotifs(fen: String): List[Motif] = {
    val situation = Fen.read(chess.variant.Standard, Fen.Full(fen)).getOrElse(sys.error(s"Invalid FEN: $fen"))
    val analyzer = lila.llm.analysis.MoveAnalyzer // object
    
    // Use runtime class of the situation object to avoid import issues
    val situClass = situation.getClass
    
    // Find method that takes this class (or superclass) and Int
    val method = analyzer.getClass.getDeclaredMethods.find { m => 
      m.getName == "detectStateMotifs" && 
      m.getParameterCount == 2 && 
      m.getParameterTypes.apply(0).isAssignableFrom(situClass)
    }.getOrElse(sys.error("Could not find detectStateMotifs method matching Situation type"))

    method.setAccessible(true)
    method.invoke(analyzer, situation, 0.asInstanceOf[AnyRef]).asInstanceOf[List[Motif]]
  }
  
  // Helper to access StructureAnalyzer for positional tags
  private val structureAnalyzer = new lila.llm.analysis.strategic.StructureAnalyzerImpl
  
  private def getPositionalTags(fen: String, color: Color): List[PositionalTag] = {
    val situation = Fen.read(chess.variant.Standard, Fen.Full(fen)).getOrElse(sys.error(s"Invalid FEN: $fen"))
    structureAnalyzer.detectPositionalFeatures(situation.board, color)
  }

  private def getWeakComplexes(fen: String): List[WeakComplex] = {
    val situation = Fen.read(chess.variant.Standard, Fen.Full(fen)).getOrElse(sys.error(s"Invalid FEN: $fen"))
    structureAnalyzer.analyze(situation.board)
  }

  private def assertMotif(fen: String, condition: Motif => Boolean, name: String): Unit = {
    val motifs = getStateMotifs(fen)
    assert(motifs.exists(condition), s"Expected Motif '$name' in FEN: $fen\nFound detected motifs: ${motifs.map(_.toString).mkString(", ")}")
  }
  
  private def assertPositionalTag(fen: String, color: Color, condition: PositionalTag => Boolean, name: String): Unit = {
    val tags = getPositionalTags(fen, color)
    assert(tags.exists(condition), s"Expected PositionalTag '$name' in FEN: $fen\nFound tags: ${tags.mkString(", ")}")
  }

  private def assertWeakComplex(fen: String, condition: WeakComplex => Boolean, name: String): Unit = {
    val complexes = getWeakComplexes(fen)
    assert(complexes.exists(condition), s"Expected WeakComplex '$name' in FEN: $fen\nFound: ${complexes.mkString(", ")}")
  }

  // Helper to get move-based motifs (using public API)
  private def getMoveMotifs(fen: String, moveUci: String): List[Motif] = {
    lila.llm.analysis.MoveAnalyzer.tokenizePv(fen, List(moveUci))
  }
  
  private def assertMoveMotif(fen: String, move: String, condition: Motif => Boolean, name: String): Unit = {
    val motifs = getMoveMotifs(fen, move)
    assert(motifs.exists(condition), s"Expected MoveMotif '$name' after $move in FEN: $fen\nFound: ${motifs.map(_.toString).mkString(", ")}")
  }

  // =========================================================================================
  // TACTICAL MOTIFS (Requiring a Move)
  // =========================================================================================

  test("Knight: Fork") {
    // White Knight on b5 jumps to c7, forking King e8 and Rook a8
    val fen = "r3k3/8/8/1N6/8/8/8/7K w - - 0 1"
    assertMoveMotif(
      fen, "b5c7",
      { case m: Motif.Fork => true; case _ => false },
      "Knight Fork"
    )
  }

  test("Knight: Rerouting (Positive)") {
    // Nf3-d2 (Retreat/Reroute)
    // Ensure d2 is empty so the knight can legally reroute there.
    val fen = "rnbqkb1r/pppppppp/5n2/8/8/5N2/PPP1PPPP/RNBQKB1R w KQkq - 0 1"
    assertMoveMotif(
      fen, "f3d2",
      { case m: Motif.Maneuver if m.purpose == "rerouting" => true; case _ => false },
      "Knight Rerouting"
    )
  }

  test("Knight: Development (Negative Rerouting)") {
    // Nb1-c3 (Standard Development) - Should NOT be "rerouting"
    val fen = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1"
    val motifs = getMoveMotifs(fen, "b1c3")
    assert(!motifs.exists { case m: Motif.Maneuver if m.purpose == "rerouting" => true; case _ => false }, 
           s"Standard development should not be rerouting. Found: $motifs")
  }
  
  test("Bishop: Pin (State)") {
    // Black bishop pins a white knight to the king (static absolute pin).
    val fen = "4k3/8/8/8/1b6/2N5/8/4K3 w - - 0 1"
    assertMotif(
      fen,
      { case m: Motif.Pin => true; case _ => false },
      "Pin (Bishop pinning Pawn)"
    )
  }
  
  test("Rook: Rook Lift") {
    // Rook moves from e1 to e3 (lift)
    val fen = "r4rk1/pp3ppp/2p5/8/4P3/8/1P3PPP/R3R1K1 w - - 0 1"
    assertMoveMotif(
      fen, "e1e3", // UCI
      { case m: Motif.RookLift => true; case _ => false },
      "Rook Lift"
    )
  }
  
  test("Queen: Discovered Attack") {
    // White Queen d1, Knight d4. Black King d8. Knight moves, discovering check.
    val fen = "3k4/8/8/8/3N4/8/8/3Q4 w - - 0 1"
    assertMoveMotif(
      fen, "d4c6",
      { case m: Motif.DiscoveredAttack => true; case _ => false },
      "Discovered Attack"
    )
  }
  
  test("Queen: Battery (Diagonal)") {
    // Queen c3, Bishop b2. Battery on long diag.
    val simpleBattery = "7k/7p/8/8/8/2Q5/1B6/K7 w - - 0 1" 
    assertMotif(
      simpleBattery,
      { case m: Motif.Battery if m.axis == Motif.BatteryAxis.Diagonal => true; case _ => false },
      "Queen+Bishop Battery"
    )
  }

  // =========================================================================================
  // PAWN MOTIFS
  // =========================================================================================

  test("Pawn: Passed Pawn") {
    // White b5 pawn is passed
    assertMotif(
      "8/8/8/1P6/8/8/k7/7K w - - 0 1",
      { case m: Motif.PassedPawn if m.file.char == 'b' => true; case _ => false },
      "Passed Pawn on b-file"
    )
  }

  test("Pawn: Isolated Queen's Pawn (IQP)") {
    // d4 is isolated
    assertMotif(
      "rnbqkb1r/pp3ppp/5n2/2p5/3P4/2N5/PP3PPP/R1BQKBNR w KQkq c6 0 1",
      { case m: Motif.IsolatedPawn if m.file.char == 'd' => true; case _ => false },
      "IQP on d4"
    )
  }

  test("Pawn: Doubled Pawns") {
    // White c3, c4 doubled
    assertMotif(
      "rnbqkbnr/pp1ppppp/2p5/8/2P5/2P5/PP1PPPPP/RNBQKBNR w KQkq - 0 1",
      { case m: Motif.DoubledPawns if m.file.char == 'c' => true; case _ => false },
      "Doubled Pawns on c-file"
    )
  }
  
  test("Pawn: Backward Pawn") {
    // d6 backward (Sveshnikov style structure)
    assertMotif(
      "r1bqkbnr/pp3ppp/2np4/2p1p3/3PP3/2N2N2/PPP2PPP/R1BQKB1R w KQkq - 0 1", // roughly d6 is backward
      { case m: Motif.BackwardPawn if m.file.char == 'd' && m.color == Color.Black => true; case _ => false },
      "Backward Pawn on d6"
    )
  }

  test("Pawn: Pawn Chain") {
    // French Advance chain: d4-e5 vs d5-e6
    assertMotif(
      "rnbqkbnr/pp3ppp/4p3/2ppP3/3P4/8/PPP2PPP/RNBQKBNR w KQkq - 0 4",
      { case m: Motif.PawnChain => true; case _ => false },
      "Pawn Chain"
    )
  }

  test("Pawn: Hanging Pawns") {
    // c4, d4 hanging (no b/e pawns)
    assertWeakComplex(
      "r2q1rk1/pp1n1ppp/4p3/8/2PP4/8/P2Q1PPP/R3KB1R w KQ - 0 1",
      { case w: WeakComplex => w.cause == "Hanging Pawns" && w.color == Color.White }, 
      "Hanging Pawns"
    )
  }

  test("Pawn: Connected Passers") {
    assertMotif(
      "8/8/8/1PP5/8/8/k7/7K w - - 0 1",
      { case m: Motif.PassedPawn => true; case _ => false }, // Should detect 2 passed pawns
      "Connected Passed Pawns"
    )
  }

  // =========================================================================================
  // PIECE MOTIFS (Knight/Bishop/Rook/Queen)
  // =========================================================================================

  test("Knight: Outpost (PositionalTag)") {
    // Knight on e5 supported by d4 pawn; black has pawns but does not control e5 with pawns, so it's a hole/outpost.
    assertPositionalTag(
      "6k1/8/pp5p/4N3/3P4/8/8/6K1 w - - 0 1",
      Color.White,
      { case PositionalTag.Outpost(sq, _) => sq.key == "e5"; case _ => false },
      "Knight Outpost on e5"
    )
  }

  test("Knight: Outpost (Motif MoveAnalyzer)") {
    // White Knight moves from f3 to e5.
    // Must be supported by a pawn and NOT attacked by an enemy pawn.
    val fen = "r1bqk2r/ppp2ppp/2n1pn2/8/3P4/5N2/PPP2PPP/RNBQKB1R w KQkq - 0 1" 
    assertMoveMotif(
      fen, "f3e5",
      { case m: Motif.Outpost => m.square.key == "e5"; case _ => false },
      "Valid Knight Outpost Move"
    )

    // False Outpost: White Knight to e5 without pawn support
    val fenNoSupport = "r1bqk2r/ppp2ppp/2n1pn2/8/8/5N2/PPP2PPP/RNBQKB1R w KQkq - 0 1"
    val motifsNoSupport = getMoveMotifs(fenNoSupport, "f3e5")
    assert(!motifsNoSupport.exists(_.isInstanceOf[Motif.Outpost]), "Should not be an Outpost without pawn support")

    // False Outpost: White Knight to e5, but attacked by enemy pawn
    val motifsAttacked = getMoveMotifs("4k3/8/3p4/8/3P4/5N2/8/4K3 w - - 0 1", "f3e5")
    assert(!motifsAttacked.exists(_.isInstanceOf[Motif.Outpost]), "Should not be an Outpost if attacked by enemy pawn")
  }
  
  test("Knight: Strong Knight (Octopus)") {
    // Strong knight on a supported outpost square
    assertPositionalTag(
      "6k1/8/pp5p/4N3/3P4/8/8/6K1 w - - 0 1",
      Color.White,
      { case PositionalTag.StrongKnight(_, _) => true; case _ => false },
      "Strong Knight"
    )
  }

  test("Bishop: Bishop Pair") {
    assertPositionalTag(
      // Black is missing one bishop, so White has the bishop pair advantage.
      "rn1qkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1",
      Color.White,
      { case PositionalTag.BishopPairAdvantage(_) => true; case _ => false },
      "Bishop Pair"
    )
  }
  
  test("Bishop: Bad Bishop") {
    // White bishop on light square, blocked by many light square pawns
    assertPositionalTag(
      "8/8/3P4/2P1P3/8/4B3/8/k6K w - - 0 1", 
      Color.White,
      { case PositionalTag.BadBishop(_) => true; case _ => false },
      "Bad Bishop"
    )
  }

  test("Rook: Open File") {
     assertPositionalTag(
      "4r2k/8/8/8/8/8/8/4R2K w - - 0 1", // e-file open
      Color.White,
      { case PositionalTag.OpenFile(file, _) => file.char == 'e'; case _ => false },
      "Open File (e-file)"
    )
  }
  
  test("Rook: Rook on Seventh") {
    assertPositionalTag(
      "6k1/R7/8/8/8/8/8/7K w - - 0 1",
      Color.White,
      { case PositionalTag.RookOnSeventh(_) => true; case _ => false },
      "Rook on Seventh"
    )
  }
  
  test("Rook: Connected Rooks") {
    assertPositionalTag(
      "3r3k/8/8/8/8/8/3RR3/7K w - - 0 1",
      Color.White,
      { case PositionalTag.ConnectedRooks(_) => true; case _ => false },
      "Connected Rooks"
    )
  }
  
  test("Rook: Battery") {
     assertMotif(
      "3r3k/8/8/8/8/8/3R4/3R3K w - - 0 1", // Rooks on d1, d2
      { case m: Motif.Battery if m.axis == Motif.BatteryAxis.File => true; case _ => false },
      "Rook Battery on d-file"
    )
  }

  // =========================================================================================
  // KING MOTIFS
  // =========================================================================================

  test("King: Opposition (Direct)") {
    // Direct Opposition: Kings on e4 vs e6 (1 square gap)
    assertMotif(
      "8/8/4k3/8/4K3/8/8/8 w - - 0 1", 
      { case m: Motif.Opposition if m.oppType == Motif.OppositionType.Direct => true; case _ => false }, 
      "Direct Opposition"
    )
  }

  test("King: Zugzwang") {
    val ea = new lila.llm.analysis.strategic.EndgameAnalyzerImpl
    val board = Fen.read(chess.variant.Standard, Fen.Full("8/8/8/8/8/8/4k3/4K3 w - - 0 1")).get.board
    val feature = ea.analyze(board, Color.White)
    assert(feature.isDefined, "Endgame analysis should run")
    assert(feature.get.zugzwangLikelihood >= 0.0)
  }
  
  test("King: King Stuck in Center") {
    // Middlegame, king on e1, open file?
    assertPositionalTag(
      // Remove the e-pawn shield so the king sits on the e-file without pawn cover.
      "rnbqkbnr/pppppppp/8/8/8/8/PPPP1PPP/RNBQKBNR w KQkq - 0 1",
      Color.White,
      { case PositionalTag.KingStuckCenter(_) => true; case _ => false },
      "King Stuck Center"
    )
  }
}
