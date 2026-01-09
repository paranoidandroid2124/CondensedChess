package lila.llm.analysis

import munit.FunSuite
import lila.llm.model._
import lila.llm.model.Motif._
import lila.llm._
import chess.Color

class EndgameNarrativeTest extends FunSuite {
  
  // Mock classes/factories
  def mockFeatures(fen: String, phase: String = "endgame"): PositionFeatures = {
    PositionAnalyzer.extractFeatures(fen, 1).getOrElse(throw new RuntimeException("Invalid FEN"))
  }

  // Helper to run labeler
  def label(fen: String, bestEval: Int): ConceptLabels = {
    val features = mockFeatures(fen)
    val parsed = chess.format.Fen.read(chess.variant.Standard, chess.format.Fen.Full(fen)).get
    ConceptLabeler.labelPosition(
      features = features,
      evalBefore = bestEval, 
      evalAfter = bestEval, 
      bestEval = bestEval, // Added argument
      board = parsed.board,
      color = parsed.color
    )
  }

  // ============================================================
  // PAWN RACE TESTS
  // ============================================================

  test("ENDGAME: Detects Pawn Race scenario") {
    // Both sides have passed pawns. White a4, Black h5.
    val fen = "8/8/8/7p/P7/8/8/k1K5 w - - 0 1"
    val labels = label(fen, 100)
    
    assert(labels.endgameTags.exists(_.isInstanceOf[EndgameTag.PawnRace]), "Should detect Pawn Race")
  }

  // ============================================================
  // OUTSIDE PASSED PAWN
  // ============================================================

  test("ENDGAME: Detects Outside Passed Pawn (White)") {
    // White passed pawn on a6, kings in center. Black has no pawns.
    val fen = "8/8/P4k2/8/8/5K2/8/8 w - - 0 1"
    val labels = label(fen, 300)
    
    val outsideTag = labels.endgameTags.collectFirst { case t: EndgameTag.OutsidePassedPawn => t }
    assert(outsideTag.isDefined, "Should detect Outside Passed Pawn")
    assertEquals(outsideTag.get.side, Color.White)
  }

  // ============================================================
  // WRONG BISHOP DRAW
  // ============================================================

  test("ENDGAME: Detects Wrong Bishop Draw possibility") {
    // White has a-pawn and light-squared bishop. Promotion square a8 is dark.
    // Evaluation is 0.00 despite material advantage.
    val fen = "K7/P7/8/8/8/8/5k2/6B1 w - - 0 1" 
    // This heuristic in ConceptLabeler depends on low eval + material imbalance
    val labels = label(fen, 0)
    
    assert(labels.endgameTags.contains(EndgameTag.WrongBishopDraw), "Should detect Wrong Bishop Draw")
  }

  // ============================================================
  // ZUGZWANG
  // ============================================================

  test("ENDGAME: Detects Zugzwang (Winning to Losing/Drawish)") {
    // A simplified zugzwang scenario 
    // We simulate the eval drop via arguments
    val features = mockFeatures("8/8/8/8/8/8/8/k1K5 w - - 0 1") // arbitrary FEN
    val parsed = chess.format.Fen.read(chess.variant.Standard, chess.format.Fen.Full("8/8/8/8/8/8/8/k1K5 w - - 0 1")).get
    
    // Previous eval was winning (+300), current best eval is drawish (0) or losing
    val labels = ConceptLabeler.labelPosition(
      features = features, 
      evalBefore = 300, 
      evalAfter = 300, 
      bestEval = 0,
      board = parsed.board,
      color = parsed.color
    )
    
    assert(labels.endgameTags.contains(EndgameTag.Zugzwang), "Should detect Zugzwang")
  }
}
