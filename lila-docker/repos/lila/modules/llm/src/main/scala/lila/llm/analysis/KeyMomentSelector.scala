package lila.llm


object KeyMomentSelector:

  case class ScoredMoment(ply: Int, interestScore: Double, reason: String)

  /**
   * Selects the top N key moments from a list of move evaluations.
   */
  def selectKeyMoments(evals: List[MoveEval], limit: Int = 8): List[Int] =
    val moments = evals.zip(evals.drop(1)).map { case (prev, curr) =>
      scoreMoment(prev, curr)
    }

    // Sort by interest score descending
    val sorted = moments.sortBy(-_.interestScore)

    // Filter to avoid clustering (e.g. don't pick ply 10, 11, 12)
    var selected = List.empty[ScoredMoment]
    sorted.foreach { m =>
      if selected.size < limit then
        if !selected.exists(s => (s.ply - m.ply).abs < 3) then // Min distance 3 plies
          selected = m :: selected
    }

    selected.map(_.ply).sorted

  private def scoreMoment(prev: MoveEval, curr: MoveEval): ScoredMoment =
    var score = 0.0
    var reasons = List.empty[String]

    val delta = (prev.cp - curr.cp).abs // Absolute change (simplification)
    // Note: This assumes white perspective for CP. 
    // For proper analysis we need turn-aware delta, but for "interest" raw volatility works as a heuristic.
    
    // 1. Blunder/Mistake Detection
    if delta > 200 then
      score += 10.0
      reasons = "Blunder" :: reasons
    else if delta > 70 then // Lowered from 100 to catch mistakes
      score += 5.0
      reasons = "Mistake" :: reasons
    else if delta > 30 then // Differentiate minor events from dead draws
      score += 1.0
      reasons = "Inaccuracy" :: reasons

    // 2. Critical Turning Point (Equal -> Won/Lost)
    if prev.cp.abs < 50 && curr.cp.abs > 150 then
      score += 8.0
      reasons = "Turning Point" :: reasons

    // 3. Complexity (Tactical Sharpness)
    // Heuristic: If PV changed completely? (Not available in simple MoveEval list)
    
    // 4. End of Game proximity (usually interesting)
    // Handled by user context usually.

    ScoredMoment(curr.ply, score, reasons.mkString(", "))

  // Extended version if we have full AnalysisData
  // def selectWithTags(...)
