package lila.chessjudgment.analysis.policy

import lila.chessjudgment.model.judgment.*

enum ClaimTruthStatus:
  case Certified
  case Deferred
  case Rejected

final case class ClaimTruthDecision(
    claim: ClaimSeed,
    status: ClaimTruthStatus,
    presentLayers: Set[EvidenceLayer],
    missingLayerGroups: List[Set[EvidenceLayer]],
    missingEvidence: List[EvidenceRef]
):
  def certified: Boolean = status == ClaimTruthStatus.Certified

object ClaimTruthPolicy:

  def evaluate(claim: ClaimSeed, graph: TypedEvidenceGraph): ClaimTruthDecision =
    val graphIds = graph.byId.keySet
    val missingEvidence = claim.evidence.filterNot(ref => graphIds.contains(ref.id))
    val boundRecords =
      claim.evidence
        .flatMap(ref => graph.byId.get(ref.id))
        .filter(isBoundToClaim(claim, _, graph))
    val presentLayers =
      boundRecords
        .map(_.ref.layer)
        .toSet
    val missingGroups =
      requiredLayerGroups(claim.family).filterNot(group => group.exists(presentLayers.contains))
    val hasFamilyProof =
      familySpecificProof(claim, boundRecords)
    val status =
      if claim.evidence.isEmpty || missingEvidence.nonEmpty then ClaimTruthStatus.Rejected
      else if boundRecords.isEmpty then ClaimTruthStatus.Rejected
      else if !hasFamilyProof then ClaimTruthStatus.Deferred
      else if missingGroups.isEmpty then ClaimTruthStatus.Certified
      else ClaimTruthStatus.Deferred
    ClaimTruthDecision(
      claim = claim,
      status = status,
      presentLayers = presentLayers,
      missingLayerGroups = missingGroups,
      missingEvidence = missingEvidence
    )

  private def isBoundToClaim(
      claim: ClaimSeed,
      record: EvidenceRecord,
      graph: TypedEvidenceGraph
  ): Boolean =
    val claimEvidenceIds = claim.evidence.map(_.id).toSet
    val samePosition = record.ref.position == claim.primaryPosition
    val sameLine = claim.primaryLine.exists(record.ref.line.contains)
    val sameSubjectMove = claim.subjectMove.exists(move => record.ref.line.exists(_.rootMove == move))
    val sameScope = record.ref.scope == claim.scope
    val parentLinked = record.parents.exists(parent => claimEvidenceIds.contains(parent.id))
    val childLinked =
      graph.records.exists(child =>
        claimEvidenceIds.contains(child.ref.id) &&
          child.parents.exists(parent => parent.id == record.ref.id)
      )
    samePosition || sameLine || sameSubjectMove || sameScope || parentLinked || childLinked

  private def requiredLayerGroups(family: ClaimFamily): List[Set[EvidenceLayer]] =
    family match
      case ClaimFamily.Tactical =>
        List(
          Set(EvidenceLayer.Relation),
          Set(EvidenceLayer.Line),
          Set(EvidenceLayer.Eval)
        )
      case ClaimFamily.Strategic =>
        List(
          Set(EvidenceLayer.Strategic, EvidenceLayer.PlanPressure, EvidenceLayer.PawnStructure, EvidenceLayer.StructuralDelta),
          Set(EvidenceLayer.Board, EvidenceLayer.Line, EvidenceLayer.Eval, EvidenceLayer.SinglePosition)
        )
      case ClaimFamily.PawnStructure =>
        List(
          Set(EvidenceLayer.PawnStructure, EvidenceLayer.StructuralDelta),
          Set(EvidenceLayer.Board, EvidenceLayer.MoveTransition, EvidenceLayer.Line)
        )
      case ClaimFamily.Opening =>
        List(
          Set(EvidenceLayer.FeatureAnchor),
          Set(EvidenceLayer.ApplicabilityAssessment),
          Set(EvidenceLayer.Line, EvidenceLayer.Eval, EvidenceLayer.SinglePosition)
        )
      case ClaimFamily.Plan =>
        List(Set(EvidenceLayer.PlanPressure, EvidenceLayer.PlanTransition), Set(EvidenceLayer.Line, EvidenceLayer.Eval))
      case ClaimFamily.Defensive =>
        List(Set(EvidenceLayer.ThreatPressure), Set(EvidenceLayer.Line, EvidenceLayer.RelativeAssessment))
      case ClaimFamily.Conversion =>
        List(Set(EvidenceLayer.SinglePosition, EvidenceLayer.RelativeAssessment), Set(EvidenceLayer.Eval, EvidenceLayer.Line))
      case ClaimFamily.Evaluation =>
        List(Set(EvidenceLayer.RelativeAssessment), Set(EvidenceLayer.Counterfactual, EvidenceLayer.Eval))

  private def familySpecificProof(claim: ClaimSeed, records: List[EvidenceRecord]): Boolean =
    claim.family match
      case ClaimFamily.Opening =>
        records.exists {
          case EvidenceRecord(_, FeatureAnchorEvidence(anchor), _) =>
            anchor.strength > 0.0
          case _ => false
        } &&
          records.exists {
            case EvidenceRecord(_, ApplicabilityAssessmentEvidence(assessment), _) =>
              assessment.applicability == FeatureApplicability.OpeningRelevant &&
                assessment.observedThemes.nonEmpty &&
                assessment.status != ApplicabilityStatus.Contradicted
            case _ => false
          }
      case _ => true
