package lila.llm.analysis

import lila.llm.model.strategic.*

/**
 * Counterfactual Analyzer
 * 
 * Deeply compares two variations (e.g., user's choice vs. engine's best) 
 * to find the exact point of failure and the tactical reason for it.
 */
object CounterfactualAnalyzer:

  /**
   * Refined Counterfactual Match for the LLM
   */
  def createMatch(
      fen: String,
      userMove: String,
      bestMove: String,
      userLine: lila.llm.model.strategic.VariationLine,
      bestLine: lila.llm.model.strategic.VariationLine
  ): CounterfactualMatch =
    val cpLoss = bestLine.effectiveScore - userLine.effectiveScore
    val userMotifs = MoveAnalyzer.tokenizePv(fen, userLine.moves)
    val bestMotifs = MoveAnalyzer.tokenizePv(fen, bestLine.moves)
    
    val missedMotifs = bestMotifs.filterNot(bm => 
      userMotifs.exists(um => um.getClass == bm.getClass)
    )

    CounterfactualMatch(
      userMove = userMove,
      bestMove = bestMove,
      cpLoss = cpLoss,
      missedMotifs = missedMotifs,
      userMoveMotifs = userMotifs,
      severity = Thresholds.classifySeverity(cpLoss),
      userLine = userLine
    )

  /**
   * FIX 2: Counterfactual with pre-normalized cpLoss (handles mate scores properly)
   */
  def createMatchNormalized(
      fen: String,
      userMove: String,
      bestMove: String,
      userLine: lila.llm.model.strategic.VariationLine,
      bestLine: lila.llm.model.strategic.VariationLine,
      cpLoss: Int
  ): CounterfactualMatch =
    val userMotifs = MoveAnalyzer.tokenizePv(fen, userLine.moves)
    val bestMotifs = MoveAnalyzer.tokenizePv(fen, bestLine.moves)
    
    val missedMotifs = bestMotifs.filterNot(bm => 
      userMotifs.exists(um => um.getClass == bm.getClass)
    )

    CounterfactualMatch(
      userMove = userMove,
      bestMove = bestMove,
      cpLoss = cpLoss,
      missedMotifs = missedMotifs,
      userMoveMotifs = userMotifs,
      severity = Thresholds.classifySeverity(cpLoss),
      userLine = userLine
    )
