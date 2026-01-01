package lila.llm.analysis

import munit.FunSuite
import lila.llm.model._
import lila.llm.model.strategic._
import lila.llm.analysis.strategic._

import chess.{ Color, Role, Square }
import chess.variant.Standard
import chess.format.Fen

class TacticalMotifsVerificationTest extends FunSuite {

  // Analyzers
  val moveAnalyzer = lila.llm.analysis.MoveAnalyzer
  val structureAnalyzer = new StructureAnalyzerImpl()
  val prophylaxisAnalyzer = new ProphylaxisAnalyzerImpl()
  val narrativeGenerator = lila.llm.NarrativeGenerator

  test("Evaluate Motifs: Lasker vs Thomas (Decoy/Deflection)") {
    println("\n=== ANALYSIS REPORT: Lasker vs Thomas (Tactical) ===")
    // Moment: 11. Qxh7+
    val fen = "rnb2rk1/pb1p1ppp/1p2pb2/4N2Q/4N3/3B4/PPP2PPP/R3K2R w KQ - 1 11"
    analyzeMoment(fen, "h5h7", "11. Qxh7+", Color.White, "Lasker vs Thomas (Queen Sac)")
  }

  test("Evaluate Motifs: Petrosian vs Reshevsky (Exchange Sacrifice)") {
    println("\n=== ANALYSIS REPORT: Petrosian vs Reshevsky (Positional) ===")
    // Moment: Constructed Exchange Sac
    val testFen = "r4rk1/pp3ppp/2p1p3/3n4/3R4/2P1P3/P3QPPP/2B2RK1 w - - 0 1"
    analyzeMoment(testFen, "d4d5", "1. Rxd5", Color.White, "Constructed Exchange Sac")
  }

  test("Evaluate Motifs: Karpov vs Kasparov (Color Complex/Pawn Majority)") {
    println("\n=== ANALYSIS REPORT: Karpov vs Kasparov (Structure) ===")
    // Moment: 16... Nd3 (The Octopus Knight)
    // Corrected FEN: Knight on b4
    val fen = "r3r1k1/5ppp/p1np1n2/1pb1p1B1/1n2P3/2N2N2/PPP1BPPP/3R1RK1 b - - 1 16"
    analyzeMoment(fen, "b4d3", "16... Nd3", Color.Black, "Karpov vs Kasparov (Octopus Knight)")
  }

  // Helper method to analyze specific moments using the real pipeline
  private def analyzeMoment(fen: String, uci: String, san: String, color: Color, title: String): Unit = {
    
    val board = Fen.read(Standard, Fen.Full(fen)).map(_.board).getOrElse(throw new Exception("Bad FEN"))
    
    // Use Position(board, variant, color)
    val pos = chess.Position(board, Standard, color)
    
    val uciObj = chess.format.Uci(uci).flatMap {
      case m: chess.format.Uci.Move => Some(m)
      case _ => None
    }.getOrElse(throw new Exception(s"Invalid UCI string: $uci"))
    
    // Generate valid Move object from Position
    val move = pos.move(uciObj.orig, uciObj.dest, uciObj.promotion).toOption.getOrElse(throw new Exception(s"Illegal Move on board: $uci"))
    
    // 1. Move Analysis (Tactics)
    val motifs = moveAnalyzer.detectTacticalMotifs(move, pos, move.after, color, san, 1)
    
    // 2. Structure Analysis (Strategic)
    val structure = structureAnalyzer.detectPositionalFeatures(move.after.board, color)
    val weakComplexes = structureAnalyzer.analyze(move.after.board)
    
    // 3. Prophylaxis (Mocked)
    val prevented = if (uci == "a7b6") List(PreventedPlan("PreventFork", Nil, None, 0, 100, Some("Fork"))) else Nil
    
    // 4. Generate Narrative (Phase 12: Analyze POST-MOVE position for accurate nature)
    val realNature = lila.llm.analysis.PositionCharacterizer.characterize(move.after)

    // Phase 13: Mock Alternatives for Book-Style Output Verification
    val mockAlternatives = if (uci == "d4d5") List(
       lila.llm.model.strategic.VariationLine(List("cxd5", "exd5", "Be7"), -20, None, Nil),
       lila.llm.model.strategic.VariationLine(List("Nf6", "Rfd1", "O-O"), 10, None, Nil),
       lila.llm.model.strategic.VariationLine(List("h6?", "Bh4", "g5"), -250, None, Nil) // Mock Refutation
    ) else Nil
    
    val data = ExtendedAnalysisData(
      fen = fen,
      nature = realNature,
      motifs = motifs,
      plans = Nil,
      preventedPlans = prevented,
      pieceActivity = Nil,
      structuralWeaknesses = weakComplexes,
      positionalFeatures = structure,
      compensation = None, 
      endgameFeatures = None,
      practicalAssessment = None,
      alternatives = mockAlternatives,
      candidates = Nil,
      counterfactual = None,
      conceptSummary = Nil,
      prevMove = Some(uci),
      ply = 1
    )
    
    val narrative = narrativeGenerator.describeExtended(data, lila.llm.NarrativeTemplates.Style.Coach)
    
    println(s"--- $title ($san) ---")
    // Phase 14: Clean output - relying on Narrative generator entirely
    println(s"NARRATIVE:\n$narrative")
    println("---------------------------------------------------")
  }

  test("King Safety: Opening position (Safe)") {
    val fen = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1"
    val board = Fen.read(Standard, Fen.Full(fen)).map(_.board).getOrElse(throw new Exception("Bad FEN"))
    
    // Should NOT have KingStuckCenter
    val features = structureAnalyzer.detectPositionalFeatures(board, chess.Color.White)
    assert(!features.exists { case lila.llm.model.strategic.PositionalTag.KingStuckCenter(_) => true; case _ => false }, "Opening should not be KingStuckCenter")
  }

  test("King Safety: Castling lost and center open (Stuck)") {
    // White King on e1, e-file open, no castling rights (kq -), black rooks present
    // Removed d4 pawn (was 3P4 -> 8) to ensure d-file is OPEN for King on d1.
    val fen = "r3r1k1/ppp2ppp/2n5/3q4/8/5N2/PP3PPP/R2KR3 w - - 0 20" 
    val board = Fen.read(Standard, Fen.Full(fen)).map(_.board).getOrElse(throw new Exception("Bad FEN"))
    
    // Should have KingStuckCenter
    val features = structureAnalyzer.detectPositionalFeatures(board, chess.Color.White)
    assert(features.exists { case lila.llm.model.strategic.PositionalTag.KingStuckCenter(_) => true; case _ => false }, "Should detect KingStuckCenter")
  }

  test("King Safety: Castling rights available but Open Center (Exposed)") {
    // White King on e1, e-file open, BUT castling allowed (KQkq)
    // Even if castling is allowed, an OPEN center makes the King exposed.
    val fen = "rnbqk2r/ppp1bppp/5n2/3p4/3P4/5N2/PPP1BPPP/RNBQK2R w KQkq - 4 6"
    val board = Fen.read(Standard, Fen.Full(fen)).map(_.board).getOrElse(throw new Exception("Bad FEN"))
    
    // Should have KingStuckCenter (Exposed) because no pawn shield
    val features = structureAnalyzer.detectPositionalFeatures(board, chess.Color.White)
    assert(features.exists { case lila.llm.model.strategic.PositionalTag.KingStuckCenter(_) => true; case _ => false }, "Open Center should flag KingStuckCenter even with castling rights")
  }
}
