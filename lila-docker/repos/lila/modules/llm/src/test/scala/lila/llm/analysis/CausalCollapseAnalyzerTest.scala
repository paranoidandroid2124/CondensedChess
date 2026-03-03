package lila.llm.analysis

import munit.FunSuite
import lila.llm.MoveEval
import lila.llm.model.strategic.VariationLine
import lila.llm.model._
import lila.llm.model._

class CausalCollapseAnalyzerTest extends FunSuite:

  // Helper to build mock MoveEval
  def move(ply: Int, cp: Int): MoveEval =
    MoveEval(ply, cp, None, Nil, List(VariationLine(List("e2e4"), cp, None)))

  // Helper to build minimal ExtendedAnalysisData
  def buildMockData(isWhite: Boolean, overridePly: Int = 27): ExtendedAnalysisData = ExtendedAnalysisData(
    fen = "mock_fen",
    nature = PositionNature(NatureType.Static, 0.5, 0.5, "Balanced"),
    motifs = Nil,
    plans = Nil,
    preventedPlans = Nil,
    pieceActivity = Nil,
    structuralWeaknesses = Nil,
    compensation = None,
    endgameFeatures = None,
    practicalAssessment = None,
    prevMove = None,
    ply = overridePly,
    evalCp = 0,
    isWhiteToMove = isWhite
  )

  test("correctly trace back early preventability on WPA drop for White") {
    val evals = List(
      move(22, 100),   // Black's turn before, giving +100
      move(23, 80),    // White's turn, giving +80
      move(24, 50),    // Black's turn, giving +50
      move(25, 0),     // White's turn, giving 0 (WPA ~50%)
      move(26, -20),   // Black's turn, giving -20 (WPA ~45%)
      move(27, -400)   // White's turn blunders, -400 (WPA very low)
    )

    val result = CausalCollapseAnalyzer.analyze(27, evals, buildMockData(isWhite = true, overridePly = 27))
    
    assert(result.isDefined)
    val cca = result.get
    assertEquals(cca.earliestPreventablePly, 27)
    assertEquals(cca.interval, "27-27")
  }

  test("compute correct winChance") {
    val cp0 = CausalCollapseAnalyzer.winChance(0)
    assert(math.abs(cp0 - 50.0) < 0.1)
    
    val cp500 = CausalCollapseAnalyzer.winChance(500)
    assert(cp500 > 80.0)
  }

  test("correctly handles Black blunder") {
    val evals = List(
      move(14, 0),    // Black's turn: WPA 50%
      move(15, 0),    // White's turn: WPA 50% (Safe)
      move(16, 400),  // Black's turn: Mistake, CP=400 -> WPA ~15% for Black (Unsafe)
      move(17, 500),  // White's turn: WPA ~10% for Black (Unsafe)
      move(18, 800)   // Black's blunder: WPA < 5% for Black (Unsafe)
    )
    val result = CausalCollapseAnalyzer.analyze(18, evals, buildMockData(isWhite = false, overridePly = 18))
    assert(result.isDefined)
    val cca = result.get
    // Black started bleeding at ply 16.
    assertEquals(cca.earliestPreventablePly, 16)
    assertEquals(cca.interval, "16-18")
  }

  test("correctly handles Custom/SetUp FEN (shifted ply parity)") {
    // A game starting from a custom FEN where ply numbers don't match standard parity.
    // e.g. Black to move on an odd ply number like 41. (ply parity decoupled from sideToMove)
    val evals = List(
      move(38, -50),
      move(39, 0),    
      move(40, -100), 
      move(41, 600)   // Black blunders on ply 41.
    )
    // isWhiteToMove = false is the source of truth, not ply = 41
    val result = CausalCollapseAnalyzer.analyze(41, evals, buildMockData(isWhite = false, overridePly = 41))
    assert(result.isDefined)
    val cca = result.get
    assertEquals(cca.earliestPreventablePly, 41) 
  }

  test("aborts CCA if eval coverage is too sparse") {
    val evals = List(
      MoveEval(22, 100, None, Nil, Nil), // Missing eval (variations empty)
      MoveEval(23, 80, None, Nil, Nil), 
      MoveEval(24, 50, None, Nil, Nil),
      move(25, 0),                       // Evaluated
      MoveEval(26, -20, None, Nil, Nil), // Missing eval
      move(27, -400)                     // Blunder evaluated
    )

    // Coverage is only 2 out of the last 6 plies, which is < 3.
    val result = CausalCollapseAnalyzer.analyze(27, evals, buildMockData(isWhite = true, overridePly = 27))
    assert(result.isEmpty, "CCA should abort when preceding eval coverage is too low")
  }

