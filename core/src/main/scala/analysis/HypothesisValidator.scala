package chess
package analysis


object HypothesisValidator:

  final case class Hypothesis(
    move: String, 
    eval: Double, 
    refutation: List[String], 
    label: String, 
    diff: Double
  )

  /**
   * Finds "Plausible but Bad" moves to answer "Why not?"
   * @param client Stockfish client
   * @param fen Current position FEN
   * @param bestMoveUci The actual best move (to exclude)
   * @param currentEval The evaluation of the best move (to calculate delta)
   * @param depth Search depth for candidates (fast)
   */
  def findHypotheses(
      client: StockfishClient,
      fen: String,
      bestMoveUci: String,
      currentEval: Double,
      depth: Int = 10
  ): List[Hypothesis] =
    // 1. Shallow search to find candidates
    val candidatesEval = EngineProbe.evalFen(client, fen, depth, multiPv = 5, moveTimeMs = Some(300))
    
    // 2. Identify "Trap" moves:
    // - Not the best move
    // - Bad enough (delta > 1.0) to be a mistake
    // - But "looks" natural (Capture, Check, or just the 2nd best engine move if 2nd best is bad)
    val candidates = candidatesEval.lines
      .filter(_.move != bestMoveUci)
      .filter(l => (currentEval - l.winPct).abs > 15.0) // Delta > 15% win probability (approx 1.0 pawn)
      .take(2) // Max 2 hypotheses

    candidates.map { line =>
      val label = "Hypothesis" // TODO: Detect 'Greedy Capture' via board logic once API is clear
      
      Hypothesis(
        move = line.move,
        eval = line.winPct,
        refutation = line.pv,
        label = label,
        diff = (currentEval - line.winPct).abs
      )
    }
