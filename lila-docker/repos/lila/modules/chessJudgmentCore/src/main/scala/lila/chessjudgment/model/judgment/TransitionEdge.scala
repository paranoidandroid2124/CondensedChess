package lila.chessjudgment.model.judgment

import lila.chessjudgment.model.Fact
import lila.chessjudgment.model.PlanSequenceSummary

case class MoveTransitionEdge(
    role: TransitionEdgeRole,
    id: String,
    from: PositionNodeRef,
    moveUci: String,
    to: PositionNodeRef,
    changedFacts: List[Fact],
    planTransition: Option[PlanSequenceSummary],
    evidence: EvidenceRef
)
