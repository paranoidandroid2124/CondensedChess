package lila.chessjudgment.analysis.evaluation

import lila.chessjudgment.model.judgment.MoveChoiceVerdict

object VerdictThresholdPolicy:

  def verdictFromWinPercent(
      candidateWinPercentDeltaForMover: Double,
      winPercentLossForMover: Double
  ): MoveChoiceVerdict =
    if candidateWinPercentDeltaForMover > 0.0 then MoveChoiceVerdict.ImprovesOnReference
    else if winPercentLossForMover == 0.0 then MoveChoiceVerdict.MatchesReference
    else if winPercentLossForMover <= JudgmentThresholds.PLAYABLE_LOSS_WP then MoveChoiceVerdict.PlayableLoss
    else if winPercentLossForMover <= JudgmentThresholds.INACCURACY_WP then MoveChoiceVerdict.Inaccuracy
    else if winPercentLossForMover < JudgmentThresholds.BLUNDER_WP then MoveChoiceVerdict.Mistake
    else MoveChoiceVerdict.Blunder
