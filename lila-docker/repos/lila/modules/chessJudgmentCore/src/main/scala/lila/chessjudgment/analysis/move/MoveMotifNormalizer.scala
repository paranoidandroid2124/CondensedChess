package lila.chessjudgment.analysis.move

import lila.chessjudgment.model.Motif
import lila.chessjudgment.model.judgment.*

object MoveMotifNormalizer:

  def fromMotifs(
      id: String,
      moveUci: String,
      motifs: List[Motif],
      position: PositionNodeRef,
      line: Option[LineNodeRef],
      scope: EvidenceScope,
      parents: List[EvidenceRef] = Nil
  ): EvidenceRecord =
    val ref =
      EvidenceRef(
        id = id,
        producer = EvidenceProducer.MoveMotifProducer,
        layer = EvidenceLayer.MoveMotif,
        position = position,
        line = line,
        scope = scope,
        confidence = EvidenceConfidence.BoardDerived
      )
    EvidenceRecord(
      ref = ref,
      payload = MoveMotifEvidence(
        moveUci = moveUci,
        motifs = motifs
      ),
      parents = parents
    )
