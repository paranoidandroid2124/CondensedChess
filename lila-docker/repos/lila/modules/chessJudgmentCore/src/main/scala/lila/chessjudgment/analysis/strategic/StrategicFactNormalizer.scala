package lila.chessjudgment.analysis.strategic

import lila.chessjudgment.analysis.singlePosition.{ PawnPlayAnalysis, ThreatAnalysis }
import lila.chessjudgment.model.{ ActivePlans, Fact, PlanId, PlanScoringResult }
import lila.chessjudgment.model.judgment.*
import lila.chessjudgment.model.structure.{ PlanAlignment, StructureProfile }

object StrategicFactNormalizer:

  def fromFacts(
      id: String,
      kind: StrategicFactKind,
      facts: List[Fact],
      relatedPlans: List[PlanId],
      confidence: Double,
      position: PositionNodeRef,
      line: Option[LineNodeRef],
      scope: EvidenceScope
  ): EvidenceRecord =
    val ref =
      EvidenceRef(
        id = id,
        producer = EvidenceProducer.StrategicFeatureProducer,
        layer = EvidenceLayer.Strategic,
        position = position,
        line = line,
        scope = scope,
        confidence = if confidence >= 0.75 then EvidenceConfidence.BoardDerived else EvidenceConfidence.Heuristic
      )
    EvidenceRecord(
      ref = ref,
      payload = StrategicFactEvidence(
        kind = kind,
        facts = facts,
        relatedPlans = relatedPlans,
        confidence = confidence
      )
    )

  def fromPawnStructure(
      id: String,
      profile: StructureProfile,
      alignment: Option[PlanAlignment],
      pawnPlay: Option[PawnPlayAnalysis],
      position: PositionNodeRef,
      scope: EvidenceScope
  ): EvidenceRecord =
    val ref =
      EvidenceRef(
        id = id,
        producer = EvidenceProducer.PawnStructureProducer,
        layer = EvidenceLayer.PawnStructure,
        position = position,
        line = None,
        scope = scope,
        confidence = if profile.confidence >= 0.75 then EvidenceConfidence.BoardDerived else EvidenceConfidence.Heuristic
      )
    EvidenceRecord(
      ref = ref,
      payload = PawnStructureFactEvidence(profile, alignment, pawnPlay)
    )

  def fromThreatPressure(
      id: String,
      threats: ThreatAnalysis,
      position: PositionNodeRef,
      line: Option[LineNodeRef],
      scope: EvidenceScope
  ): EvidenceRecord =
    val ref =
      EvidenceRef(
        id = id,
        producer = EvidenceProducer.ThreatPressureProducer,
        layer = EvidenceLayer.ThreatPressure,
        position = position,
        line = line,
        scope = scope,
        confidence = if threats.insufficientData then EvidenceConfidence.Mixed else EvidenceConfidence.EngineBacked
      )
    EvidenceRecord(ref = ref, payload = ThreatPressureEvidence(threats))

  def fromPlanPressure(
      id: String,
      scoring: PlanScoringResult,
      activePlans: ActivePlans,
      position: PositionNodeRef,
      line: Option[LineNodeRef],
      scope: EvidenceScope
  ): EvidenceRecord =
    val ref =
      EvidenceRef(
        id = id,
        producer = EvidenceProducer.PlanPressureProducer,
        layer = EvidenceLayer.PlanPressure,
        position = position,
        line = line,
        scope = scope,
        confidence = if scoring.confidence >= 0.75 then EvidenceConfidence.Mixed else EvidenceConfidence.Heuristic
      )
    EvidenceRecord(ref = ref, payload = PlanPressureEvidence(scoring, activePlans))
