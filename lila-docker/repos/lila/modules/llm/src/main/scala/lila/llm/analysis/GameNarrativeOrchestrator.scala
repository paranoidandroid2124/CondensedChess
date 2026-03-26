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

  private val SevereCpSwingThreshold = 280
  private val CatastrophicCpSwingThreshold = 420
  private val DecisiveEdgeCp = 320
  private val BalancedWindowCp = 120
  private val SharpWindowCp = 220

  private def winChance(cp: Int): Double = 
    50.0 + 50.0 * (2.0 / (1.0 + math.exp(-0.00368208 * cp)) - 1.0)

  private def swingScore(eval: MoveEval): Int =
    eval.mate match
      case Some(mateScore) if mateScore > 0 => 10000
      case Some(_)                          => -10000
      case None                             => eval.cp

  private def momentPriority(moment: KeyMoment): Int =
    moment.momentType match
      case "MissedWin"         => 0
      case "Blunder"           => 1
      case "MateLost"          => 2
      case "MateFound"         => 3
      case "MateShift"         => 4
      case "Equalization"      => 5
      case "SustainedPressure" => 6
      case "TensionPeak"       => 7
      case _                   => 8

  private def momentSeverity(moment: KeyMoment): Double =
    math.abs(moment.wpaSwing.getOrElse(0.0)) * 1000.0 + math.abs(moment.cpAfter - moment.cpBefore).toDouble

  private def missedWinningEdge(prevScore: Int, currScore: Int): Boolean =
    (prevScore >= DecisiveEdgeCp && currScore <= SharpWindowCp) ||
      (prevScore <= -DecisiveEdgeCp && currScore >= -SharpWindowCp)

  private def sharpDecisiveRescue(prevScore: Int, currScore: Int, absWpDiff: Double, absCpDiff: Int): Boolean =
    absWpDiff <= 18.0 &&
      absCpDiff >= SevereCpSwingThreshold &&
      (
        missedWinningEdge(prevScore, currScore) ||
          (math.abs(prevScore) <= BalancedWindowCp && math.abs(currScore) >= DecisiveEdgeCp) ||
          (math.signum(prevScore) != math.signum(currScore) &&
            (absCpDiff >= SevereCpSwingThreshold || math.abs(prevScore) >= SharpWindowCp || math.abs(currScore) >= SharpWindowCp)) ||
          absCpDiff >= CatastrophicCpSwingThreshold
      )

  private def severeSwingMoment(
      prev: MoveEval,
      curr: MoveEval,
      prevScore: Int,
      currScore: Int,
      wpDiff: Double,
      absWpDiff: Double
  ): Option[KeyMoment] =
    val absCpDiff = math.abs(currScore - prevScore)
    if absWpDiff > 18.0 then {
      if (winChance(prevScore) > 75.0 && winChance(currScore) < 60.0)
        Some(KeyMoment(curr.ply, "MissedWin", curr.cp, "White squandered advantage", prev.cp, curr.cp, prev.mate, curr.mate, Some(wpDiff)))
      else if (winChance(prevScore) < 25.0 && winChance(currScore) > 40.0)
        Some(KeyMoment(curr.ply, "MissedWin", curr.cp, "Black squandered advantage", prev.cp, curr.cp, prev.mate, curr.mate, Some(wpDiff)))
      else if (winChance(prevScore) > 40.0 && winChance(prevScore) < 60.0 && (winChance(currScore) < 30.0 || winChance(currScore) > 70.0))
        Some(KeyMoment(curr.ply, "Blunder", curr.cp, f"Decisive swing (${absWpDiff}%.1f%% WPA)", prev.cp, curr.cp, prev.mate, curr.mate, Some(wpDiff)))
      else
        Some(KeyMoment(curr.ply, "Blunder", curr.cp, f"Major error (${absWpDiff}%.1f%% WPA)", prev.cp, curr.cp, prev.mate, curr.mate, Some(wpDiff)))
    } else if sharpDecisiveRescue(prevScore, currScore, absWpDiff, absCpDiff) then {
      if missedWinningEdge(prevScore, currScore) then
        Some(
          KeyMoment(
            curr.ply,
            "MissedWin",
            curr.cp,
            s"Winning edge collapsed (${absCpDiff} cp swing)",
            prev.cp,
            curr.cp,
            prev.mate,
            curr.mate,
            Some(wpDiff)
          )
        )
      else
        Some(
          KeyMoment(
            curr.ply,
            "Blunder",
            curr.cp,
            s"Severe engine swing rescued (${absCpDiff} cp)",
            prev.cp,
            curr.cp,
            prev.mate,
            curr.mate,
            Some(wpDiff)
          )
        )
    } else if (absWpDiff > 12.0 && (winChance(currScore) - 50.0).abs < 10.0 && (winChance(prevScore) - 50.0).abs > 15.0) then
      Some(KeyMoment(curr.ply, "Equalization", curr.cp, "Position stabilized", prev.cp, curr.cp, prev.mate, curr.mate, Some(wpDiff)))
    else None

  // NOTE: Lichess CP is ALWAYS from White's perspective (positive = White winning)
  // We convert CP to Win Probability (WPA) to accurately gauge if a blunder is decisive.

  def selectKeyMoments(evals: List[MoveEval]): List[KeyMoment] = {
    // 1. Identify Swings using WPA
    val swings = evals.zip(evals.drop(1)).flatMap { case (prev, curr) =>
      val prevScore = swingScore(prev)
      val currScore = swingScore(curr)
      
      val wpPrev = winChance(prevScore)
      val wpCurr = winChance(currScore)
      val wpDiff = wpCurr - wpPrev
      val absWpDiff = wpDiff.abs

      severeSwingMoment(prev, curr, prevScore, currScore, wpDiff, absWpDiff)
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
    val allMoments = matePivots ++ swings ++ pressure ++ tension

    allMoments
      .groupBy(_.ply)
      .values
      .map(_.toList.sortBy(moment => (momentPriority(moment), -momentSeverity(moment))).head)
      .toList
      .sortBy(_.ply)
  }
}
