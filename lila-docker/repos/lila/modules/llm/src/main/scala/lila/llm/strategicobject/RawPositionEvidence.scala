package lila.llm.strategicobject

final case class RawPositionEvidence(
    fen: String,
    playedMove: Option[String] = None,
    moveIndex: Option[Int] = None,
    rawFacts: List[String] = Nil,
    rawMotifs: List[String] = Nil,
    probeEvidence: List[String] = Nil,
    engineEvidence: List[String] = Nil
)
