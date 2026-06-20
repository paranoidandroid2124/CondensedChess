package lila.chessjudgment.model.judgment

import chess.Color
import play.api.libs.json._
import lila.chessjudgment.analysis.evaluation.{ PerspectiveMath, VerdictThresholdPolicy }
import lila.chessjudgment.model.strategic.VariationLine

case class EvaluatedPositionNode(
  ply: Int,
  cp: Int,
  mate: Option[Int] = None,
  pv: List[String] = Nil,
  variations: List[VariationLine] = Nil
) {
  def variationLines: List[VariationLine] =
    if (variations.nonEmpty) variations
    else if (pv.nonEmpty) List(VariationLine(moves = pv, scoreCp = cp, mate = mate))
    else Nil
}

object EvaluatedPositionNode:
  given Reads[EvaluatedPositionNode] = Json.reads[EvaluatedPositionNode]
  given Writes[EvaluatedPositionNode] = Json.writes[EvaluatedPositionNode]

enum MoveChoiceVerdict:
  case ImprovesOnReference
  case MatchesReference
  case PlayableLoss
  case Inaccuracy
  case Mistake
  case Blunder

case class MoveChoiceAssessment(
    mover: Color,
    reference: EvaluatedPositionNode,
    candidate: EvaluatedPositionNode,
    candidateDeltaForMover: Int,
    candidateWinPercentDeltaForMover: Double,
    cpLossForMover: Int,
    winPercentLossForMover: Double,
    verdict: MoveChoiceVerdict
)

object MoveChoiceAssessment:
  def compare(
      mover: Color,
      reference: EvaluatedPositionNode,
      candidate: EvaluatedPositionNode
  ): MoveChoiceAssessment =
    val candidateDeltaForMover =
      if mover.white then candidate.cp - reference.cp
      else reference.cp - candidate.cp
    val loss = (-candidateDeltaForMover).max(0)
    val winPercentDelta =
      PerspectiveMath.winPercentImprovementForMover(
        mover = mover,
        defendedWhiteCp = candidate.cp,
        defendedMate = candidate.mate,
        threatWhiteCp = reference.cp,
        threatMate = reference.mate
      )
    val winPercentLoss =
      PerspectiveMath.winPercentLossForMover(
        mover = mover,
        bestWhiteCp = reference.cp,
        bestMate = reference.mate,
        playedWhiteCp = candidate.cp,
        playedMate = candidate.mate
      )
    MoveChoiceAssessment(
      mover = mover,
      reference = reference,
      candidate = candidate,
      candidateDeltaForMover = candidateDeltaForMover,
      candidateWinPercentDeltaForMover = winPercentDelta,
      cpLossForMover = loss,
      winPercentLossForMover = winPercentLoss,
      verdict = VerdictThresholdPolicy.verdictFromWinPercent(winPercentDelta, winPercentLoss)
    )
