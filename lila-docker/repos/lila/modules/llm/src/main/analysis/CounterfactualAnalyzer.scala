package lila.llm

import lila.llm.model.*

import _root_.chess.*

/**
 * Counterfactual Analyzer
 * 
 * Deeply compares two variations (e.g., user's choice vs. engine's best) 
 * to find the exact point of failure and the tactical reason for it.
 */
object CounterfactualAnalyzer:

  case class TacticalDivergence(
      divergencePly: Int,
      punishmentMove: Option[String], // The move that proves the user's move was bad
      reason: String,                // e.g., "Missed a fork", "Pawn structure collapsed"
      materialLoss: Int,             // CP equivalent of material lost
      positionalLoss: Int            // CP equivalent of positional deterioration
  )

  /**
   * Analyzes why userMove is inferior to bestMove by comparing their tactical trajectories.
   */
  def analyzeDivergence(
      fen: String,
      userLine: VariationLine,
      bestLine: VariationLine,
      hypotheses: List[Hypothesis] = Nil
  ): Option[TacticalDivergence] =
    val cpLoss = bestLine.effectiveScore - userLine.effectiveScore
    
    // If CP loss is low, it's not a tactical failure we need to explain deeply
    if cpLoss < 50 then return None

    // Tokenize both lines to compare motifs
    val bestMotifs = MoveAnalyzer.tokenizePv(fen, bestLine.moves)

    // 1. Check for missed tactical opportunities (Motifs in best but not in user)
    val missedTactics = bestMotifs.filter {
      case _: CheckMotif | _: ForkMotif | _: PinMotif | _: Skewer | _: DiscoveredAttack => true
      case m: CaptureMotif if m.captureType == CaptureType.Sacrifice => true
      case _ => false
    }

    // 2. Find the "Punisher" - the move in the user's line that makes it bad
    // Usually, if the user makes a mistake (Move 1), the engine responds (Move 2) 
    // to punish it. Move 2 is the punisher.
    val punishmentMove = userLine.moves.lift(1) 

    // 3. Match against Hypotheses (Human-like mistakes)
    val matchedHypothesis = hypotheses.find(_.move == userLine.moves.headOption.getOrElse(""))

    val reason = if (missedTactics.nonEmpty) {
      val names = missedTactics.map(_.getClass.getSimpleName).distinct.mkString(", ")
      s"Missed a tactical opportunity ($names)"
    } else if (matchedHypothesis.isDefined) {
      s"Fell for a natural but tactically flawed move (${matchedHypothesis.get.rationale})"
    } else if (cpLoss > 300) {
      "Suffered a decisive tactical collapse"
    } else {
      "Positional deterioration"
    }

    Some(TacticalDivergence(
      divergencePly = 1, // Currently simplified to the immediate next move
      punishmentMove = punishmentMove,
      reason = reason,
      materialLoss = if (cpLoss > 100) cpLoss else 0,
      positionalLoss = if (cpLoss <= 100) cpLoss else 0
    ))

  /**
   * Refined Counterfactual Match for the LLM
   */
  def createMatch(
      fen: String,
      userMove: String,
      bestMove: String,
      userLine: VariationLine,
      bestLine: VariationLine
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
      severity = if (cpLoss >= 300) "blunder" else if (cpLoss >= 100) "mistake" else if (cpLoss >= 50) "inaccuracy" else "ok"
    )

  // ============================================================
  // HELPERS
  // ============================================================

    Uci(uciStr).collect { case m: Uci.Move => m }.flatMap(pos.move(_).toOption).map(_.after)
