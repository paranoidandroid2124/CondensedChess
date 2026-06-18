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
    val presentLayers =
      claim.evidence
        .flatMap(ref => graph.byId.get(ref.id).map(_.ref.layer))
        .toSet
    val missingGroups =
      requiredLayerGroups(claim.family).filterNot(group => group.exists(presentLayers.contains))
    val status =
      if claim.evidence.isEmpty || missingEvidence.nonEmpty then ClaimTruthStatus.Rejected
      else if missingGroups.isEmpty then ClaimTruthStatus.Certified
      else ClaimTruthStatus.Deferred
    ClaimTruthDecision(
      claim = claim,
      status = status,
      presentLayers = presentLayers,
      missingLayerGroups = missingGroups,
      missingEvidence = missingEvidence
    )

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
          Set(EvidenceLayer.SinglePosition, EvidenceLayer.Strategic, EvidenceLayer.PlanPressure, EvidenceLayer.PawnStructure),
          Set(EvidenceLayer.Board, EvidenceLayer.Line, EvidenceLayer.Eval)
        )
      case ClaimFamily.PawnStructure =>
        List(Set(EvidenceLayer.PawnStructure, EvidenceLayer.StructuralDelta))
      case ClaimFamily.Opening =>
        List(Set(EvidenceLayer.OpeningRoute), Set(EvidenceLayer.Line, EvidenceLayer.Eval))
      case ClaimFamily.Plan =>
        List(Set(EvidenceLayer.PlanPressure, EvidenceLayer.PlanTransition), Set(EvidenceLayer.Line, EvidenceLayer.Eval))
      case ClaimFamily.Defensive =>
        List(Set(EvidenceLayer.ThreatPressure), Set(EvidenceLayer.Line, EvidenceLayer.RelativeAssessment))
      case ClaimFamily.Conversion =>
        List(Set(EvidenceLayer.SinglePosition, EvidenceLayer.RelativeAssessment), Set(EvidenceLayer.Eval, EvidenceLayer.Line))
      case ClaimFamily.Evaluation =>
        List(Set(EvidenceLayer.RelativeAssessment), Set(EvidenceLayer.Counterfactual, EvidenceLayer.Eval))
