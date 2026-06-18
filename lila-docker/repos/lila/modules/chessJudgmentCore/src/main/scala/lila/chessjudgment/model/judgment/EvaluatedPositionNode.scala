package lila.chessjudgment.model.judgment

import chess.Color
import play.api.libs.json._
import lila.chessjudgment.model.strategic.VariationLine

case class EvaluatedPositionNode(
  ply: Int,
  cp: Int,
  mate: Option[Int] = None,
  pv: List[String] = Nil,
  variations: List[VariationLine] = Nil
) {
  def effectiveCp: Int =
    mate.map(m => if m > 0 then 10000 - m else -10000 + m).getOrElse(cp)

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
    cpLossForMover: Int,
    verdict: MoveChoiceVerdict
)

object MoveChoiceAssessment:
  def compare(
      mover: Color,
      reference: EvaluatedPositionNode,
      candidate: EvaluatedPositionNode
  ): MoveChoiceAssessment =
    val candidateDeltaForMover =
      if mover.white then candidate.effectiveCp - reference.effectiveCp
      else reference.effectiveCp - candidate.effectiveCp
    val loss = (-candidateDeltaForMover).max(0)
    MoveChoiceAssessment(
      mover = mover,
      reference = reference,
      candidate = candidate,
      candidateDeltaForMover = candidateDeltaForMover,
      cpLossForMover = loss,
      verdict = verdictFromDelta(candidateDeltaForMover, loss)
    )

  private def verdictFromDelta(candidateDeltaForMover: Int, cpLossForMover: Int): MoveChoiceVerdict =
    if candidateDeltaForMover > 0 then MoveChoiceVerdict.ImprovesOnReference
    else if cpLossForMover == 0 then MoveChoiceVerdict.MatchesReference
    else if cpLossForMover <= 25 then MoveChoiceVerdict.PlayableLoss
    else if cpLossForMover <= 80 then MoveChoiceVerdict.Inaccuracy
    else if cpLossForMover <= 180 then MoveChoiceVerdict.Mistake
    else MoveChoiceVerdict.Blunder
