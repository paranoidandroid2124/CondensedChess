package lila.llm

import lila.llm.*
import lila.llm.analysis.*
import chess.Color

class ExtendedAnalysisTest extends munit.FunSuite:

  test("detect threat on a hanging piece"):
    // Position where Black has a hanging knight on c6, White to move
    val fen = "r1bqkbnr/pp3ppp/2n5/4p3/3pP3/5N2/PPPP1PPP/RNBQKB1R w KQkq - 0 1"
    
    val threats = MoveAnalyzer.detectThreats(fen, Color.Black)
    val motifNames = threats.map(_.getClass.getSimpleName)
    
    // Opponent (White) can capture the Knight or something else
    assert(motifNames.nonEmpty)

  test("create counterfactual match for a mistake"):
    val fen = "r1bqkbnr/pppp1ppp/2n5/4p3/4P3/5N2/PPPP1PPP/RNBQKB1R w KQkq - 2 3"
    val userMove = "h2h3" // Slow move
    val bestMove = "f1b5" // Ruy Lopez
    
    val userLine = VariationLine("1", List(userMove), Some(-50), None, 20)
    val bestLine = VariationLine("2", List(bestMove), Some(40), None, 20)
    
    val cf = CounterfactualAnalyzer.createMatch(fen, userMove, bestMove, userLine, bestLine)
    
    assert(cf.userMove == userMove)
    assert(cf.bestMove == bestMove)
    assert(cf.cpLoss > 50)

  test("assess extended data in CommentaryEngine"):
    val fen = "r1bqkbnr/pppp1ppp/2n5/4p3/4P3/5N2/PPPP1PPP/RNBQKB1R w KQkq - 2 3"
    val data = ExtendedAnalysisData(
      fen = fen,
      ply = 5,
      color = Color.White,
      variations = List(
        VariationLine("1", List("f1b5", "a7a6"), Some(40), None, 20),
        VariationLine("2", List("d2d4", "e5d4"), Some(30), None, 20)
      ),
      threat = Some(VariationLine("user", List("h2h3"), Some(-50), None, 20))
    )
    
    val assessmentOpt = CommentaryEngine.assessExtended(data)
    
    assert(assessmentOpt.isDefined)
    val assessment = assessmentOpt.get
    assert(assessment.threats.nonEmpty)
    assert(assessment.counterfactuals.nonEmpty)
    
    val formatted = CommentaryEngine.formatExtendedForLlm(assessment)
    assert(formatted.contains("TACTICAL JUSTIFICATIONS"))
    assert(formatted.contains("IMMEDIATE THREATS"))
