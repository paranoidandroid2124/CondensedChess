package lila.chessjudgment.analysis.line

import lila.chessjudgment.model.judgment.*

object LineFactNormalizer:

  def fromCandidateLine(
      id: String,
      line: CandidateLineNode,
      position: PositionNodeRef,
      scope: EvidenceScope,
      parents: List[EvidenceRef] = Nil
  ): EvidenceRecord =
    val ref =
      EvidenceRef(
        id = id,
        producer = EvidenceProducer.LegalLineProducer,
        layer = EvidenceLayer.Line,
        position = position,
        line = Some(line.ref),
        scope = scope,
        confidence = EvidenceConfidence.EngineBacked
      )
    EvidenceRecord(
      ref = ref,
      payload = LineFactEvidence(
        line = line.ref,
        firstMove = line.line.moves.headOption,
        replyMove = line.line.moves.lift(1),
        continuationMoves = line.line.moves.drop(2)
      ),
      parents = parents
    )

  def fromValidatedLine(
      id: String,
      lineRef: LineNodeRef,
      facts: PrincipalVariationEvidence.LineFacts,
      position: PositionNodeRef,
      scope: EvidenceScope,
      parents: List[EvidenceRef] = Nil
  ): EvidenceRecord =
    val ref =
      EvidenceRef(
        id = id,
        producer = EvidenceProducer.LegalLineProducer,
        layer = EvidenceLayer.Line,
        position = position,
        line = Some(lineRef),
        scope = scope,
        confidence = EvidenceConfidence.LegalReplayVerified
      )
    EvidenceRecord(
      ref = ref,
      payload = LineFactEvidence(
        line = lineRef,
        firstMove = Some(facts.first.uci),
        replyMove = facts.reply.map(_.uci),
        continuationMoves = facts.continuation.toList.map(_.uci) ++ facts.continuationTail.map(_.uci)
      ),
      parents = parents
    )
