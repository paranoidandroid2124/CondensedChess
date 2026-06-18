package lila.chessjudgment.analysis.evaluation

import lila.chessjudgment.model.judgment.MoveChoiceVerdict

object VerdictThresholdPolicy:

  def verdictFromDelta(
      candidateDeltaForMover: Int,
      cpLossForMover: Int
  ): MoveChoiceVerdict =
    if candidateDeltaForMover > 0 then MoveChoiceVerdict.ImprovesOnReference
    else if cpLossForMover == 0 then MoveChoiceVerdict.MatchesReference
    else if cpLossForMover <= JudgmentThresholds.PLAYABLE_LOSS_CP then MoveChoiceVerdict.PlayableLoss
    else if cpLossForMover <= JudgmentThresholds.MISTAKE_CP then MoveChoiceVerdict.Inaccuracy
    else if cpLossForMover < JudgmentThresholds.BLUNDER_CP then MoveChoiceVerdict.Mistake
    else MoveChoiceVerdict.Blunder
