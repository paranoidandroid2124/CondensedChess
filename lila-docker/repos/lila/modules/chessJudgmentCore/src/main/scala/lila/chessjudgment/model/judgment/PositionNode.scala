package lila.chessjudgment.model.judgment

import chess.Color
import lila.chessjudgment.analysis.position.PositionFeatures
import lila.chessjudgment.analysis.singlePosition.SinglePositionAssessment
import lila.chessjudgment.model.Fact

case class PositionNodeRef(
    fen: String,
    ply: Int,
    sideToMove: Option[Color] = None,
    id: Option[String] = None
)

case class PositionNode(
    role: PositionNodeRole,
    ref: PositionNodeRef,
    facts: List[Fact],
    features: Option[PositionFeatures],
    assessment: Option[SinglePositionAssessment],
    evidence: List[EvidenceRef]
)
