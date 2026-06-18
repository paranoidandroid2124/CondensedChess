package lila.chessjudgment.model.judgment

import lila.chessjudgment.model.strategic.VariationLine

case class LineNodeRef(
    id: String,
    rootMove: String,
    rank: Int,
    role: LineNodeRole
)

case class CandidateLineNode(
    role: LineNodeRole,
    ref: LineNodeRef,
    line: VariationLine,
    evalCp: Int,
    mate: Option[Int],
    depth: Int,
    evidence: EvidenceRef
)
