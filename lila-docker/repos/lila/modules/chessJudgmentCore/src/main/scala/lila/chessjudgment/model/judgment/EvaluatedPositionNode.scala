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
    rawCandidateDeltaCpForDiagnostics: Int,
    candidateWinPercentDeltaForMover: Double,
    rawCpLossForDiagnostics: Int,
    winPercentLossForMover: Double,
    verdict: MoveChoiceVerdict
)

object MoveChoiceAssessment:
  def compare(
      mover: Color,
      reference: EvaluatedPositionNode,
      candidate: EvaluatedPositionNode
  ): MoveChoiceAssessment =
    val delta =
      PerspectiveMath.compareForMover(
        mover = mover,
        reference = PerspectiveMath.EvalPoint(reference.cp, reference.mate),
        candidate = PerspectiveMath.EvalPoint(candidate.cp, candidate.mate)
      )
    MoveChoiceAssessment(
      mover = mover,
      reference = reference,
      candidate = candidate,
      rawCandidateDeltaCpForDiagnostics = delta.rawCandidateDeltaCpForMover,
      candidateWinPercentDeltaForMover = delta.candidateWinPercentDeltaForMover,
      rawCpLossForDiagnostics = delta.rawCpLossForMover,
      winPercentLossForMover = delta.winPercentLossForMover,
      verdict = VerdictThresholdPolicy.verdictFromWinPercent(
        delta.candidateWinPercentDeltaForMover,
        delta.winPercentLossForMover
      )
    )
