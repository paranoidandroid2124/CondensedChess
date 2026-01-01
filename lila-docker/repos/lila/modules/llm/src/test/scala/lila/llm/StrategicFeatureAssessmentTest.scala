package lila.llm

import munit.FunSuite
import lila.llm.analysis.strategic._
import lila.llm.model._
import lila.llm.model.strategic.{ VariationLine, _ }
import chess.{Board, Color, Role}
import chess.format.Fen
import chess.variant.Standard

class StrategicFeatureAssessmentTest extends FunSuite {

  // Helpers
  def loadBoard(fen: String): Board = 
    Fen.read(Standard, Fen.Full(fen)).map(_.board).getOrElse(throw new Exception(s"Invalid FEN: $fen"))

  val scorer = new PracticalityScorerImpl()
  val activityAnalyzer = new ActivityAnalyzerImpl()
  val structureAnalyzer = new StructureAnalyzerImpl()
  val endgameAnalyzer = new EndgameAnalyzerImpl()

  // TEST CASE A: "The Fortress"
  // White has a Bad Bishop on d3 (blocked by c4, e4). Black Knight on d4 dominating.
  // Engine Eval: 0.00 (Dead Draw/Closed). Practical Eval: Should be negative for White.
  test("PracticalityScorer detects Bad Bishop penalty (Fortress)") {
    val fen = "8/8/2p1k3/2p1p3/2PnP3/3B4/3K4/8 w - - 0 1"
    val board = loadBoard(fen)
    val color = Color.White
    
    // Run Analyzers
    val activity = activityAnalyzer.analyze(board, color)
    val structure = structureAnalyzer.analyze(board) // Finds pawn structure
    val endgame = endgameAnalyzer.analyze(board, color)
    
    // Variations (Engine says 0.00)
    val variations = List(
      VariationLine(List("d3b1"), 0, None, Nil), // Shuffling
      VariationLine(List("d2c1"), 0, None, Nil)
    )

    // Score
    val assessment = scorer.score(board, color, 0, variations, activity, structure, endgame)
    
    println(s"Fortress Assessment: ${assessment.verdict}, Practical Score: ${assessment.practicalScore}")
    
    // Assertions
    // 1. Should detect Bad Bishop
    assert(activity.exists(p => p.piece == chess.Bishop && p.isBadBishop), "Should detect bad bishop")
    
    // 2. Practical Score should be significantly lower than Engine Score (0)
    // -30 for bad bishop, maybe more.
    assert(assessment.practicalScore < -20.0, s"Practical score ${assessment.practicalScore} should be < -20 due to bad bishop")
    assert(assessment.verdict == "Under Pressure" || assessment.verdict == "Balanced") // Depending on thresholds
  }

  // TEST CASE C: "The Minefield" (Opposition)
  // King vs King opposition.
  test("EndgameAnalyzer detects Opposition") {
    val fen = "8/8/8/3k4/8/3K4/8/8 w - - 0 1" // Direct Opposition
    val board = loadBoard(fen)
    val endgame = endgameAnalyzer.analyze(board, Color.White)
    
    assert(endgame.isDefined)
    assert(endgame.get.hasOpposition, "Should detect opposition pattern")
  }
}
