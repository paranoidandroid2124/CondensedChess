package lila.commentary.analysis.semantic

import lila.commentary.analysis.structure.StructuralDelta
import lila.commentary.analysis.StrategicConceptSemantics
import lila.commentary.analysis.semantic.StrategicObservationIds.*

private[commentary] enum StrategicSemanticObservationRole:
  case SupportOnly
  case SelectorSource

private[commentary] final case class StrategicSemanticObservation(
    id: SemanticObservationId,
    ownerSide: String,
    focusSquares: List[String] = Nil,
    focusZone: Option[String] = None,
    source: Option[EvidenceSourceId] = None,
    facts: List[FactId] = Nil,
    role: StrategicSemanticObservationRole = StrategicSemanticObservationRole.SupportOnly,
    proofSource: Option[ProofSourceId] = None,
    proofFamily: Option[ProofFamilyId] = None
):
  def supportOnly: Boolean =
    role == StrategicSemanticObservationRole.SupportOnly

  def evidenceRefs: List[EvidenceRef] =
    source.map(EvidenceRef.Source(_)).toList ++ facts.distinct.map(EvidenceRef.Fact(_))

  def wireEvidenceRefs: List[String] =
    evidenceRefs.map(_.wireKey).distinct

private[commentary] object StrategicSemanticObservation:

  def supportOnly(
      id: SemanticObservationId,
      ownerSide: String,
      focusSquares: List[String] = Nil,
      focusZone: Option[String] = None,
      facts: List[FactId] = Nil
  ): StrategicSemanticObservation =
    StrategicSemanticObservation(
      id = id,
      ownerSide = ownerSide,
      focusSquares = focusSquares.distinct,
      focusZone = focusZone,
      facts = facts.distinct,
      role = StrategicSemanticObservationRole.SupportOnly
    )

  def selectorSource(
      id: SemanticObservationId,
      source: EvidenceSourceId,
      ownerSide: String,
      focusSquares: List[String] = Nil,
      focusZone: Option[String] = None,
      facts: List[FactId] = Nil
  ): StrategicSemanticObservation =
    StrategicSemanticObservation(
      id = id,
      ownerSide = ownerSide,
      focusSquares = focusSquares.distinct,
      focusZone = focusZone,
      source = Some(source),
      facts = facts.distinct,
      role = StrategicSemanticObservationRole.SelectorSource
    )

  def minorityAttack(
      ownerSide: String,
      focusSquares: List[String],
      focusZone: Option[String],
      targets: List[String],
      structuralDelta: Option[StructuralDelta],
      supportFacts: List[FactId] = Nil
  ): StrategicSemanticObservation =
    selectorSource(
      id = SemanticObservationId.MinorityAttackSemantic,
      source = EvidenceSourceId.MinorityAttackSemantic,
      ownerSide = ownerSide,
      focusSquares = focusSquares,
      focusZone = focusZone,
      facts =
        FactId.semantic(SemanticObservationId.MinorityAttackSemantic) ::
          (targetPressureFacts(structuralDelta, targets) ++ supportFacts).distinct
    )

  def minorityAttackFromConcept(
      ownerSide: String,
      focusSquares: List[String],
      observation: StrategicConceptSemantics.StrategicConceptObservation
  ): StrategicSemanticObservation =
    val deltaFacts =
      observation.structuralDelta.toList.flatMap(delta =>
        delta.createdTension.flatMap(edge => FactId.dynamic(s"created_tension_$edge")) ++
          delta.resolvedTension.flatMap(edge => FactId.dynamic(s"resolved_tension_$edge")) ++
          delta.newWeakPawns.flatMap(square => FactId.dynamic(s"new_weak_pawn_$square")) ++
          delta.openedFiles.flatMap(file => FactId.dynamic(s"opened_file_$file")) ++
          delta.semiOpenedFiles.flatMap(file => FactId.dynamic(s"semi_opened_file_$file")) ++
          Option
            .when(delta.targetPressureDelta > 0)(s"target_pressure_delta_${delta.targetPressureDelta}")
            .flatMap(FactId.dynamic)
            .toList ++
          Option
            .when(delta.fileAccessDelta > 0)(s"file_access_delta_${delta.fileAccessDelta}")
            .flatMap(FactId.dynamic)
            .toList
      )
    minorityAttack(
      ownerSide = ownerSide,
      focusSquares = focusSquares,
      focusZone = Some(observation.wing),
      targets = observation.targets,
      structuralDelta = observation.structuralDelta,
      supportFacts =
        FactId.dynamic(s"minority_attack_${observation.wing}").toList ++
          observation.primaryBreak.flatMap(move => FactId.dynamic(s"minority_break_$move")).toList ++
          observation.essentialEvidence.flatMap(evidence => FactId.dynamic(s"concept_${evidence.id}")) ++
          deltaFacts
    )

  def targetPressureFacts(
      structuralDelta: Option[StructuralDelta],
      targets: List[String]
  ): List[FactId] =
    structuralDelta.toList
      .filter(delta =>
        delta.createdTension.nonEmpty ||
          delta.newWeakPawns.nonEmpty ||
          delta.newWeakSquares.nonEmpty ||
          delta.targetPressureDelta > 0
      )
      .flatMap(delta =>
        List(FactId.semantic(SemanticObservationId.TargetPressureSemantic)) ++
          targets.take(3).flatMap(target => FactId.dynamic(s"target_pressure_target_$target")) ++
          delta.createdTension.flatMap(edge => FactId.dynamic(s"target_pressure_created_tension_$edge")) ++
          delta.newWeakPawns.flatMap(square => FactId.dynamic(s"target_pressure_new_weak_pawn_$square")) ++
          delta.newWeakSquares.flatMap(square => FactId.dynamic(s"target_pressure_new_weak_square_$square")) ++
          Option
            .when(delta.targetPressureDelta > 0)(s"target_pressure_delta_${delta.targetPressureDelta}")
            .flatMap(FactId.dynamic)
            .toList
      )
      .distinct
