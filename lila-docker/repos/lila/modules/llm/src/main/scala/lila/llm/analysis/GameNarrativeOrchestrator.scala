package lila.llm.analysis

import lila.llm.MoveEval

case class KeyMoment(
    ply: Int, 
    momentType: String, // "Blunder", "TensionPeak", "MissedWin", "EndgameTransition", "OpeningNovelty"
    score: Int,
    description: String,
    cpBefore: Int = 0,
    cpAfter: Int = 0,
    mateBefore: Option[Int] = None,
    mateAfter: Option[Int] = None,
    wpaSwing: Option[Double] = None,
    selectionKind: String = "key",
    selectionLabel: Option[String] = Some("Key Moment"),
    selectionReason: Option[String] = None
)

object GameNarrativeOrchestrator {

  private def winChance(cp: Int): Double = 
    50.0 + 50.0 * (2.0 / (1.0 + math.exp(-0.00368208 * cp)) - 1.0)

  // NOTE: Lichess CP is ALWAYS from White's perspective (positive = White winning)
  // We convert CP to Win Probability (WPA) to accurately gauge if a blunder is decisive.

  def selectKeyMoments(evals: List[MoveEval]): List[KeyMoment] = {
    // 1. Identify Swings using WPA
    val swings = evals.zip(evals.drop(1)).flatMap { case (prev, curr) =>
      // Convert mates to large cp values for WPA calculation (+/- 10000)
      val prevScore = prev.mate.map(m => math.signum(m) * 10000).getOrElse(prev.cp)
      val currScore = curr.mate.map(m => math.signum(m) * 10000).getOrElse(curr.cp)
      
      val wpPrev = winChance(prevScore)
      val wpCurr = winChance(currScore)
      val wpDiff = wpCurr - wpPrev
      val absWpDiff = wpDiff.abs
      
      if (absWpDiff > 18.0) {
        // High advantage to completely balanced or losing
        if (wpPrev > 75.0 && wpCurr < 60.0) Some(KeyMoment(curr.ply, "MissedWin", curr.cp, "White squandered advantage", prev.cp, curr.cp, prev.mate, curr.mate, Some(wpDiff)))
        else if (wpPrev < 25.0 && wpCurr > 40.0) Some(KeyMoment(curr.ply, "MissedWin", curr.cp, "Black squandered advantage", prev.cp, curr.cp, prev.mate, curr.mate, Some(wpDiff)))
        // Balanced to losing
        else if (wpPrev > 40.0 && wpPrev < 60.0 && (wpCurr < 30.0 || wpCurr > 70.0)) Some(KeyMoment(curr.ply, "Blunder", curr.cp, f"Decisive swing (${absWpDiff}%.1f%% WPA)", prev.cp, curr.cp, prev.mate, curr.mate, Some(wpDiff)))
        // Other major errors
        else Some(KeyMoment(curr.ply, "Blunder", curr.cp, f"Major error (${absWpDiff}%.1f%% WPA)", prev.cp, curr.cp, prev.mate, curr.mate, Some(wpDiff)))
      } else if (absWpDiff > 12.0 && (wpCurr - 50.0).abs < 10.0 && (wpPrev - 50.0).abs > 15.0) {
        Some(KeyMoment(curr.ply, "Equalization", curr.cp, "Position stabilized", prev.cp, curr.cp, prev.mate, curr.mate, Some(wpDiff)))
      } else {
        None
      }
    }
    
    // 2. Identify Sustained Pressure (gradual drift in one direction)
    val pressure = evals.sliding(6).flatMap { window =>
      val start = window.head
      val end = window.last
      
      val wpStart = winChance(start.mate.map(math.signum(_) * 10000).getOrElse(start.cp))
      val wpEnd = winChance(end.mate.map(math.signum(_) * 10000).getOrElse(end.cp))
      val totalWpDrift = wpEnd - wpStart
      
      // Calculate max single-step WP change
      val steps = window.zip(window.drop(1)).map { case (a, b) => 
        val wpA = winChance(a.mate.map(math.signum(_) * 10000).getOrElse(a.cp))
        val wpB = winChance(b.mate.map(math.signum(_) * 10000).getOrElse(b.cp))
        (wpB - wpA).abs
      }
      
      // Drift > 18% WPA over 6 plies, with no single blunder > 8% WPA
      if (steps.nonEmpty && totalWpDrift.abs > 18.0 && steps.max < 8.0) {
        val gainer = if (totalWpDrift > 0) "White" else "Black"
        Some(KeyMoment(end.ply, "SustainedPressure", end.cp, f"$gainer gradually outplayed (${totalWpDrift.abs}%.1f%% WPA)", start.cp, end.cp, start.mate, end.mate, Some(totalWpDrift)))
      } else None
    }

    // 2. Identify Mate Pivots (Separate from CP)
    val matePivots = evals.zip(evals.drop(1)).flatMap { case (prev, curr) =>
      val wpDiff = winChance(curr.mate.map(math.signum(_) * 10000).getOrElse(curr.cp)) - winChance(prev.mate.map(math.signum(_) * 10000).getOrElse(prev.cp))
      (prev.mate, curr.mate) match {
        // Case A: Mate Found (None -> Some)
        case (None, Some(m)) =>
          Some(KeyMoment(curr.ply, "MateFound", m * 1000, s"Mate in $m discovered", prev.cp, curr.cp, prev.mate, curr.mate, Some(wpDiff)))
        // Case B: Mate Lost (Some -> None)
        case (Some(prevM), None) =>
          Some(KeyMoment(curr.ply, "MateLost", curr.cp, s"Mate in $prevM missed", prev.cp, curr.cp, prev.mate, curr.mate, Some(wpDiff)))
        // Case C: Mate Distance Change (e.g., M10 -> M3, significant tightening)
        case (Some(prevM), Some(currM)) if (prevM - currM).abs >= 3 =>
          val direction = if (currM < prevM) "accelerated" else "delayed"
          Some(KeyMoment(curr.ply, "MateShift", currM * 1000, s"Mate $direction: M$prevM -> M$currM", prev.cp, curr.cp, prev.mate, curr.mate, Some(wpDiff)))
        case _ => None
      }
    }

    // 4. Identify Tension (Close candidates, middlegame/endgame only)
    val tension = evals.filter(_.ply >= 16).flatMap { e =>
      val vars = e.getVariations
      if (vars.size >= 2) {
        val wp1 = winChance(vars(0).scoreCp)
        val wp2 = winChance(vars(1).scoreCp)
        val wpCurrent = winChance(e.cp)
        
        val wpDiff = (wp1 - wp2).abs
        // Ensure tension is critical: diff < 5% WPA AND absolute position is relatively balanced (30% to 70% WPA)
        if (wpDiff < 5.0 && wpCurrent > 30.0 && wpCurrent < 70.0) {
          Some(KeyMoment(e.ply, "TensionPeak", e.cp, "Critical decision point", e.cp, e.cp, e.mate, e.mate, Some(0.0))) // Use 0.0 WPA swing for steady tension peaks or compute actual prev
        } else None
      } else None
    }

    // 4. Return ALL important moments, sorted by ply
    val allMoments = (matePivots ++ swings ++ pressure ++ tension).sortBy(_.ply)
    
    // Deduplicate by ply
    allMoments.distinctBy(_.ply)
  }
}
