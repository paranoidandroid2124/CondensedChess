package lila.chessjudgment.analysis.transition

import lila.chessjudgment.analysis.structure.StructuralDelta
import lila.chessjudgment.model.PlanSequenceSummary
import lila.chessjudgment.model.judgment.*

object TransitionFactNormalizer:

  def fromMoveTransition(edge: MoveTransitionEdge): EvidenceRecord =
    EvidenceRecord(
      ref = edge.evidence,
      payload = MoveTransitionEvidence(
        moveUci = edge.moveUci,
        from = edge.from,
        to = edge.to
      )
    )

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

  def fromCandidateComparison(
      id: String,
      comparison: CandidateComparisonFact,
      position: PositionNodeRef,
      scope: EvidenceScope,
      confidence: EvidenceConfidence = EvidenceConfidence.EngineBacked,
      parents: List[EvidenceRef] = Nil
  ): EvidenceRecord =
    val ref =
      EvidenceRef(
        id = id,
        producer = EvidenceProducer.RelativeMoveProducer,
        layer = EvidenceLayer.CandidateComparison,
        position = position,
        line = Some(comparison.candidateLine),
        scope = scope,
        confidence = confidence
      )
    EvidenceRecord(
      ref = ref,
      payload = CandidateComparisonEvidence(comparison),
      parents = parents
    )

  def fromCounterfactual(
      id: String,
      referenceLine: LineNodeRef,
      candidateLine: LineNodeRef,
      comparison: EvalComparison,
      position: PositionNodeRef,
      scope: EvidenceScope,
      confidence: EvidenceConfidence = EvidenceConfidence.EngineBacked,
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
        confidence = confidence
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
          assessment.counterfactualEvidence ++
          assessment.candidateComparisonEvidence ++
          assessment.relativeCauseEvidence ++
          assessment.verdictCertificationEvidence.toList
      ).distinctBy(_.id)
    EvidenceRecord(
      ref = assessment.evidence,
      payload = RelativeAssessmentEvidence(assessment),
      parents = parents
    )

  def fromRelativeCause(
      id: String,
      cause: RelativeCauseFact,
      position: PositionNodeRef,
      scope: EvidenceScope,
      confidence: EvidenceConfidence = EvidenceConfidence.EngineBacked,
      parents: List[EvidenceRef] = Nil
  ): EvidenceRecord =
    val ref =
      EvidenceRef(
        id = id,
        producer = EvidenceProducer.RelativeMoveProducer,
        layer = EvidenceLayer.RelativeCause,
        position = position,
        line = Some(cause.eventLine),
        scope = scope,
        confidence = confidence
      )
    EvidenceRecord(
      ref = ref,
      payload = RelativeCauseFactEvidence(cause),
      parents = parents
    )

  def fromMoveVerdictCertification(
      id: String,
      certification: MoveVerdictCertification,
      position: PositionNodeRef,
      scope: EvidenceScope,
      confidence: EvidenceConfidence = EvidenceConfidence.EngineBacked,
      parents: List[EvidenceRef] = Nil
  ): EvidenceRecord =
    val ref =
      EvidenceRef(
        id = id,
        producer = EvidenceProducer.RelativeMoveProducer,
        layer = EvidenceLayer.MoveVerdictCertification,
        position = position,
        line = Some(certification.primaryComparison.candidateLine),
        scope = scope,
        confidence = confidence
      )
    EvidenceRecord(
      ref = ref,
      payload = MoveVerdictCertificationEvidence(certification),
      parents = parents
    )
