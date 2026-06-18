package lila.chessjudgment.model.judgment

import chess.Color
import lila.chessjudgment.model.CollapseAnalysis

case class EvalComparison(
    mover: Color,
    referenceLine: LineNodeRef,
    candidateLine: LineNodeRef,
    candidateDeltaForMover: Int,
    cpLossForMover: Int,
    verdict: MoveChoiceVerdict
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
