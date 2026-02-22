package lila.llm.analysis

import munit.FunSuite
import chess.{ Board, Color }
import chess.format.Fen
import chess.variant.Standard
import lila.llm.analysis.strategic.EndgameAnalyzerImpl
import lila.llm.model.strategic.{ EndgameOppositionType, RookEndgamePattern, RuleOfSquareStatus }

class EndgameAnalyzerOracleTest extends FunSuite:

  private val analyzer = new EndgameAnalyzerImpl()

  private def boardOf(fen: String): Board =
    Fen.read(Standard, Fen.Full(fen)).map(_.board).get

  test("direct opposition is classified correctly") {
    val board = boardOf("8/8/4k3/8/4K3/8/8/8 w - - 0 1")
    val feature = analyzer.analyze(board, Color.White)
    assert(feature.isDefined)
    assertEquals(feature.get.oppositionType, EndgameOppositionType.Direct)
    assertEquals(feature.get.hasOpposition, true)
  }

  test("distant opposition is classified correctly") {
    val board = boardOf("8/8/4k3/8/8/8/4K3/8 w - - 0 1")
    val feature = analyzer.analyze(board, Color.White)
    assert(feature.isDefined)
    assertEquals(feature.get.oppositionType, EndgameOppositionType.Distant)
  }

  test("diagonal opposition is classified correctly") {
    val board = boardOf("8/8/5k2/8/3K4/8/8/8 w - - 0 1")
    val feature = analyzer.analyze(board, Color.White)
    assert(feature.isDefined)
    assertEquals(feature.get.oppositionType, EndgameOppositionType.Diagonal)
  }

  test("rule of square fails when king cannot catch passer") {
    val board = boardOf("k7/8/8/8/7p/8/8/K7 w - - 0 1")
    val feature = analyzer.analyze(board, Color.White)
    assert(feature.isDefined)
    assertEquals(feature.get.ruleOfSquare, RuleOfSquareStatus.Fails)
  }

  test("rule of square holds when king can catch passer") {
    val board = boardOf("k7/8/8/8/7p/8/5K2/8 w - - 0 1")
    val feature = analyzer.analyze(board, Color.White)
    assert(feature.isDefined)
    assertEquals(feature.get.ruleOfSquare, RuleOfSquareStatus.Holds)
  }

  test("triangulation signal is enabled in king opposition endings") {
    val board = boardOf("8/8/4k3/8/4K3/8/8/8 w - - 0 1")
    val feature = analyzer.analyze(board, Color.White)
    assert(feature.isDefined)
    assertEquals(feature.get.triangulationAvailable, true)
  }

  test("rook behind passed pawn pattern is detected") {
    val board = boardOf("8/4k3/8/3P4/8/8/6K1/3R4 w - - 0 1")
    val feature = analyzer.analyze(board, Color.White)
    assert(feature.isDefined)
    assertEquals(feature.get.rookEndgamePattern, RookEndgamePattern.RookBehindPassedPawn)
  }

  test("king cut-off pattern is detected") {
    val board = boardOf("8/8/8/8/R3k3/8/8/7K w - - 0 1")
    val feature = analyzer.analyze(board, Color.White)
    assert(feature.isDefined)
    assertEquals(feature.get.rookEndgamePattern, RookEndgamePattern.KingCutOff)
  }

  test("zugzwang likelihood crosses threshold in sparse opposition endgames") {
    val board = boardOf("8/8/4k3/8/4K3/8/8/8 w - - 0 1")
    val feature = analyzer.analyze(board, Color.White)
    assert(feature.isDefined)
    assert(feature.get.zugzwangLikelihood >= 0.65)
    assertEquals(feature.get.isZugzwang, true)
  }

  test("zugzwang likelihood stays low without endgame pressure signals") {
    val board = boardOf("7k/8/8/8/8/8/pppp4/K7 w - - 0 1")
    val feature = analyzer.analyze(board, Color.White)
    assert(feature.isDefined)
    assert(feature.get.zugzwangLikelihood < 0.65)
  }
