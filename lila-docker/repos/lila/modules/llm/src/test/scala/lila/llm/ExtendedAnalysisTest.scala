package lila.llm

import lila.llm.*
import lila.llm.analysis.*
import chess.Color

class ExtendedAnalysisTest extends munit.FunSuite:

  test("detect threat on a hanging piece"):
    // Position where White has a forcing tactical threat (Scholar's mate pattern), White to move
    val fen = "r1bqkb1r/pppp1ppp/2n2n2/4p2Q/2B1P3/8/PPPP1PPP/RNB1KBNR w KQkq - 2 4"
    
    val threats = MoveAnalyzer.detectThreats(fen, Color.Black)
    val motifNames = threats.map(_.getClass.getSimpleName)
    
    // Opponent (White) has an immediate tactical threat (check/mate)
    assert(motifNames.nonEmpty)

  test("create counterfactual match for a mistake"):
    val fen = "r1bqkbnr/pppp1ppp/2n5/4p3/4P3/5N2/PPPP1PPP/RNBQKB1R w KQkq - 2 3"
    val userMove = "h2h3" // Slow move
    val bestMove = "f1b5" // Ruy Lopez
    
    val userLine = lila.llm.model.strategic.VariationLine(moves = List(userMove), scoreCp = -50, depth = 20)
    val bestLine = lila.llm.model.strategic.VariationLine(moves = List(bestMove), scoreCp = 40, depth = 20)
    
    val cf = CounterfactualAnalyzer.createMatch(fen, userMove, bestMove, userLine, bestLine)
    
    assert(cf.userMove == userMove)
    assert(cf.bestMove == bestMove)
    assert(cf.cpLoss > 50)

  test("assess extended data in CommentaryEngine"):
    val fen = "r1bqkbnr/pppp1ppp/2n5/4p3/4P3/5N2/PPPP1PPP/RNBQKB1R w KQkq - 2 3"
    val variations = List(
      lila.llm.model.strategic.VariationLine(moves = List("f1b5", "a7a6"), scoreCp = 40, depth = 20),
      lila.llm.model.strategic.VariationLine(moves = List("d2d4", "e5d4"), scoreCp = 30, depth = 20)
    )

    val dataOpt = CommentaryEngine.assessExtended(
      fen = fen,
      variations = variations,
      playedMove = Some("h2h3"),
      ply = 5,
      prevMove = Some("h2h3")
    )

    assert(dataOpt.isDefined)
    assert(dataOpt.exists(_.counterfactual.isDefined))
