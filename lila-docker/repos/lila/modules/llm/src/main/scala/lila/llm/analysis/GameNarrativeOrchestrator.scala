package lila.llm.analysis

import lila.llm.MoveEval

case class KeyMoment(
    ply: Int, 
    momentType: String, // "Blunder", "TensionPeak", "MissedWin", "EndgameTransition", "OpeningNovelty"
    score: Int,
    description: String
)

object GameNarrativeOrchestrator {

  // NOTE: Lichess CP is ALWAYS from White's perspective (positive = White winning)
  // No normalization needed - raw CP values are used directly

  def selectKeyMoments(evals: List[MoveEval]): List[KeyMoment] = {
    // 1. Identify CP Swings (using raw CP - already White's perspective)
    val swings = evals.zip(evals.drop(1)).flatMap { case (prev, curr) =>
      val diff = curr.cp - prev.cp
      val absDiff = diff.abs
      
      if (absDiff > 200) {
        // White was winning (>200) and now isn't (<100)
        if (prev.cp > 200 && curr.cp < 100) Some(KeyMoment(curr.ply, "MissedWin", curr.cp, "White squandered advantage"))
        // Black was winning (<-200) and now isn't (>-100)
        else if (prev.cp < -200 && curr.cp > -100) Some(KeyMoment(curr.ply, "MissedWin", curr.cp, "Black squandered advantage"))
        // Position went from balanced to decisive
        else if (prev.cp.abs < 100 && curr.cp.abs > 200) Some(KeyMoment(curr.ply, "Blunder", curr.cp, s"Decisive swing ($absDiff cp)"))
        else Some(KeyMoment(curr.ply, "Blunder", curr.cp, "Major error"))
      } else if (absDiff > 100 && curr.cp.abs < 50 && prev.cp.abs > 100) {
        Some(KeyMoment(curr.ply, "Equalization", curr.cp, "Position stabilized"))
      } else {
        None
      }
    }
    
    // 2. Identify Sustained Pressure (gradual drift in one direction)
    val pressure = evals.sliding(6).flatMap { window =>
      val start = window.head
      val end = window.last
      val totalDrift = end.cp - start.cp // Positive = trending toward White, Negative = toward Black
      val steps = window.zip(window.drop(1)).map { case (a, b) => (b.cp - a.cp).abs }
      
      if (steps.nonEmpty && totalDrift.abs > 150 && steps.max < 100) {
        val gainer = if (totalDrift > 0) "White" else "Black"
        Some(KeyMoment(end.ply, "SustainedPressure", end.cp, s"$gainer gradually outplayed"))
      } else None
    }

    // 2. Identify Mate Pivots (Separate from CP)
    val matePivots = evals.zip(evals.drop(1)).flatMap { case (prev, curr) =>
      (prev.mate, curr.mate) match {
        // Case A: Mate Found (None -> Some)
        case (None, Some(m)) =>
          Some(KeyMoment(curr.ply, "MateFound", m * 1000, s"Mate in $m discovered"))
        // Case B: Mate Lost (Some -> None)
        case (Some(prevM), None) =>
          Some(KeyMoment(curr.ply, "MateLost", curr.cp, s"Mate in $prevM missed"))
        // Case C: Mate Distance Change (e.g., M10 -> M3, significant tightening)
        case (Some(prevM), Some(currM)) if (prevM - currM).abs >= 3 =>
          val direction = if (currM < prevM) "accelerated" else "delayed"
          Some(KeyMoment(curr.ply, "MateShift", currM * 1000, s"Mate $direction: M$prevM -> M$currM"))
        case _ => None
      }
    }

    // 3. Identify Tension (Close candidates)
    val tension = evals.flatMap { e =>
      val vars = e.getVariations
      if (vars.size >= 2) {
        val scoreDiff = (vars(0).scoreCp - vars(1).scoreCp).abs
        // Ensure tension is critical: diff < 30 AND absolute score < 150 (not winning/losing)
        if (scoreDiff < 30 && e.cp.abs < 150) Some(KeyMoment(e.ply, "TensionPeak", e.cp, "Critical decision point"))
        else None
      } else None
    }

    // 4. Return ALL important moments, sorted by ply
    val allMoments = (matePivots ++ swings ++ pressure ++ tension).sortBy(_.ply)
    
    // Deduplicate by ply
    allMoments.distinctBy(_.ply)
  }
}
