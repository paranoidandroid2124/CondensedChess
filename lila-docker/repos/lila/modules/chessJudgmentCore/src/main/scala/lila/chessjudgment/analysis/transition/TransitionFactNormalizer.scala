package lila.chessjudgment.analysis.transition

import lila.chessjudgment.analysis.structure.StructuralDelta
import lila.chessjudgment.model.PlanSequenceSummary
import lila.chessjudgment.model.judgment.*

object TransitionFactNormalizer:

  def fromStructuralDelta(
      id: String,
      delta: StructuralDelta,
      position: PositionNodeRef,
      line: Option[LineNodeRef],
      scope: EvidenceScope,
      parents: List[EvidenceRef] = Nil
  ): EvidenceRecord =
    val ref =
      EvidenceRef(
        id = id,
        producer = EvidenceProducer.StructuralDeltaProducer,
        layer = EvidenceLayer.StructuralDelta,
        position = position,
        line = line,
        scope = scope,
        confidence = EvidenceConfidence.BoardDerived
      )
    EvidenceRecord(
      ref = ref,
      payload = StructuralDeltaEvidence(delta),
      parents = parents
    )

  def fromPlanTransition(
      id: String,
      transition: PlanSequenceSummary,
      position: PositionNodeRef,
      line: Option[LineNodeRef],
      scope: EvidenceScope,
      parents: List[EvidenceRef] = Nil
  ): EvidenceRecord =
    val ref =
      EvidenceRef(
        id = id,
        producer = EvidenceProducer.PlanTransitionProducer,
        layer = EvidenceLayer.PlanTransition,
        position = position,
        line = line,
        scope = scope,
        confidence = EvidenceConfidence.Mixed
      )
    EvidenceRecord(
      ref = ref,
      payload = PlanTransitionEvidence(transition),
      parents = parents
    )

  def fromCounterfactual(
      id: String,
      referenceLine: LineNodeRef,
      candidateLine: LineNodeRef,
      comparison: EvalComparison,
      position: PositionNodeRef,
      scope: EvidenceScope,
      parents: List[EvidenceRef] = Nil
  ): EvidenceRecord =
    val ref =
      EvidenceRef(
        id = id,
        producer = EvidenceProducer.RelativeMoveProducer,
        layer = EvidenceLayer.Counterfactual,
        position = position,
        line = Some(candidateLine),
        scope = scope,
        confidence = EvidenceConfidence.EngineBacked
      )
    EvidenceRecord(
      ref = ref,
      payload = CounterfactualFactEvidence(
        referenceLine = referenceLine,
        candidateLine = candidateLine,
        comparison = comparison
      ),
      parents = parents
    )

  def fromRelativeAssessment(assessment: RelativeMoveAssessment): EvidenceRecord =
    val parents =
      (
        List(
          assessment.played.evidence,
          assessment.reference.evidence,
          assessment.candidate.evidence
        ) ++
          assessment.referenceTransition.toList.map(_.evidence) ++
          assessment.counterfactualEvidence
      ).distinctBy(_.id)
    EvidenceRecord(
      ref = assessment.evidence,
      payload = RelativeAssessmentEvidence(assessment),
      parents = parents
    )
