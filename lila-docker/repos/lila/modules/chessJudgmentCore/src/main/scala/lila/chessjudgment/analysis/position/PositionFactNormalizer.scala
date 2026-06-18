package lila.chessjudgment.analysis.position

import lila.chessjudgment.model.Fact
import lila.chessjudgment.model.judgment.*

object PositionFactNormalizer:

  def fromBoardFacts(
      id: String,
      facts: List[Fact],
      features: Option[PositionFeatures],
      position: PositionNodeRef,
      scope: EvidenceScope
  ): EvidenceRecord =
    val ref =
      EvidenceRef(
        id = id,
        producer = EvidenceProducer.BoardFactProducer,
        layer = EvidenceLayer.Board,
        position = position,
        line = None,
        scope = scope,
        confidence = EvidenceConfidence.BoardDerived
      )
    EvidenceRecord(
      ref = ref,
      payload = BoardFactEvidence(
        facts = facts,
        features = features
      )
    )
