package lila.llm.analysis

import lila.llm.MoveEval
import lila.llm.model.{ CollapseAnalysis, ExtendedAnalysisData }

object CausalCollapseAnalyzer:

  def winChance(cp: Int, mate: Option[Int] = None): Double =
    val score = mate.map(m => math.signum(m) * 10000).getOrElse(cp)
    50.0 + 50.0 * (2.0 / (1.0 + math.exp(-0.00368208 * score)) - 1.0)

  // Calculate WPA from the POV of the player who blundered
  def povWinChance(blunderingColorIsWhite: Boolean, cp: Int, mate: Option[Int]): Double =
    val whiteWpa = winChance(cp, mate)
    if (blunderingColorIsWhite) whiteWpa else 100.0 - whiteWpa

  def analyze(
      blunderPly: Int,
      evals: List[MoveEval],
      blunderData: ExtendedAnalysisData
  ): Option[CollapseAnalysis] =
    val blunderingColorIsWhite = blunderData.isWhiteToMove
    
    // Strictly only consider evals that actually have variations (meaning they were evaluated)
    val history = evals.filter(e => e.ply <= blunderPly && e.variations.nonEmpty).sortBy(_.ply)
    
    // If the blunder itself wasn't evaluated, or we have no history, abort
    if (history.isEmpty || history.last.ply != blunderPly) return None

    // If the eval coverage leading up to the blunder is very sparse, abort
    // We want at least 3 of the last 6 plies to have evals to trust the "collapse" trajectory
    val expectedPlies = math.min(blunderPly, 6)
    val recentPliesCount = history.count(_.ply > blunderPly - 6)
    if (expectedPlies > 3 && recentPliesCount < 3) return None


    val finalWpa = povWinChance(blunderingColorIsWhite, history.last.cp, history.last.mate)
    
    var earliestPly = blunderPly
    var recoverabilityTurns = 0
    var foundSafe = false

    val revereseHistory = history.reverse.drop(1)

    for (state <- revereseHistory if !foundSafe) {
      val wpa = povWinChance(blunderingColorIsWhite, state.cp, state.mate)
      
      // We consider the position "safe" or "stable" if WPA > 40% limits, or if it was > 25% better than final
      if (wpa >= 40.0 || (wpa - finalWpa) >= 20.0) {
        val nextTurnForBlunderer = if ((state.ply % 2) == (blunderPly % 2)) state.ply + 2 else state.ply + 1
        earliestPly = math.min(nextTurnForBlunderer, blunderPly)
        foundSafe = true
      } else {
        if ((state.ply % 2) == (blunderPly % 2)) {
          recoverabilityTurns += 1
        }
      }
    }

    val patchLine = history.find(_.ply == earliestPly - 1)
      .flatMap(_.getVariations.headOption.map(_.moves.take(3)))
      .getOrElse(Nil)
      
    val rootCause = determineRootCause(blunderData)

    Some(CollapseAnalysis(
      interval = s"$earliestPly-$blunderPly",
      rootCause = rootCause,
      earliestPreventablePly = earliestPly,
      patchLineUci = patchLine,
      recoverabilityPlies = recoverabilityTurns + 1
    ))

  private def determineRootCause(data: ExtendedAnalysisData): String =
    val motifNames = data.motifs.map(_.getClass.getSimpleName)
    
    if (motifNames.exists(Seq("Pin", "Fork", "DiscoveredAttack", "Skewer").contains))
      "Tactical Miss"
    else if (data.integratedContext.exists(_.threatsToUs.exists(_.threats.nonEmpty)))
      "Ignoring Opponent Threats"
    else if (data.nature.tension > 0.7)
      "Tension Miscalculation"
    else if (data.planSequence.exists(_.transitionType.toString == "ForcedPivot"))
      "Plan Deviation (Forced)"
    else
      "Positional Misjudgment"
