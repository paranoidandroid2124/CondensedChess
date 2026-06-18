package lila.chessjudgment.model.judgment

import chess.Color
import lila.chessjudgment.model.CollapseAnalysis

case class CandidateSetComparison(
    secondLine: Option[LineNodeRef],
    bestToSecondGapForMover: Option[Int],
    bestToSecondWinPercentGapForMover: Option[Double],
    candidateCount: Int,
    onlyMove: Boolean
)

case class EvalComparison(
    mover: Color,
    referenceLine: LineNodeRef,
    candidateLine: LineNodeRef,
    candidateDeltaForMover: Int,
    candidateWinPercentDeltaForMover: Double,
    cpLossForMover: Int,
    winPercentLossForMover: Double,
    verdict: MoveChoiceVerdict,
    candidateSet: Option[CandidateSetComparison] = None
)

case class RelativeMoveAssessment(
    played: MoveTransitionEdge,
    referenceTransition: Option[MoveTransitionEdge],
    reference: CandidateLineNode,
    candidate: CandidateLineNode,
    comparison: EvalComparison,
    collapse: Option[CollapseAnalysis],
    confidence: EvidenceConfidence,
    evidence: EvidenceRef,
    counterfactualEvidence: List[EvidenceRef]
)
