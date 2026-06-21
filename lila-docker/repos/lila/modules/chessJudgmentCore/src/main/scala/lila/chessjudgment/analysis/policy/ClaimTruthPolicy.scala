package lila.chessjudgment.analysis.policy

import lila.chessjudgment.analysis.evaluation.JudgmentThresholds
import lila.chessjudgment.analysis.singlePosition.PawnPlayDriver
import lila.chessjudgment.analysis.tactical.TacticalMotifClassifier
import lila.chessjudgment.model.{ ActivePlans, PlanMatch, PlanSupport, PlanScoringResult }
import lila.chessjudgment.model.structure.{ AlignmentBand, StructureId }
import lila.chessjudgment.model.strategic.PlanTaxonomy.PlanTheme
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
    val claimBoundRecords =
      claim.evidence
        .flatMap(ref => graph.byId.get(ref.id))
        .filter(isBoundToClaim(claim, _, graph))
    val claimBoundLayers =
      claimBoundRecords
        .map(_.ref.layer)
        .toSet
    val missingGroups =
      requiredLayerGroups(claim.family).filterNot(group => group.exists(claimBoundLayers.contains))
    val hasFamilyProof =
      familySpecificProof(claim, claimBoundRecords)
    val status =
      if claim.evidence.isEmpty || missingEvidence.nonEmpty then ClaimTruthStatus.Rejected
      else if claimBoundRecords.isEmpty then ClaimTruthStatus.Rejected
      else if !hasFamilyProof then ClaimTruthStatus.Deferred
      else if missingGroups.isEmpty then ClaimTruthStatus.Certified
      else ClaimTruthStatus.Deferred
    ClaimTruthDecision(
      claim = claim,
      status = status,
      presentLayers = claimBoundLayers,
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
    val sameLine = claim.primaryLine.exists(line => recordLineMatches(record, line))
    val sameSubjectMove = claim.subjectMove.exists(move => recordMentionsMove(record, move, claim.primaryLine))
    val comparisonLineSupport =
      comparisonProofLayer(record.ref.layer) &&
        comparisonLinesForClaim(claim, graph).exists(line => recordLineMatches(record, line))
    val transitionDestinationLocal =
      claimEvidenceIds.contains(record.ref.id) &&
        positionLocalLayer(record.ref.layer) &&
        transitionDestinationsForClaim(claim, graph).contains(record.ref.position)
    val samePositionLocal =
      samePosition &&
        claim.subject == IdeaSubject.Position &&
        claim.subjectMove.isEmpty &&
        record.ref.line.isEmpty &&
        positionLocalLayer(record.ref.layer)
    val parentLinked = record.parents.exists(parent => claimEvidenceIds.contains(parent.id))
    val childLinked =
      graph.records.exists(child =>
        claimEvidenceIds.contains(child.ref.id) &&
          child.parents.exists(parent => parent.id == record.ref.id)
      )
    val linkedBound =
      (parentLinked || childLinked) &&
        linkedRecordCompatible(claim, record, graph, samePositionLocal)
    sameLine || sameSubjectMove || comparisonLineSupport || transitionDestinationLocal || samePositionLocal || linkedBound

  private def linkedRecordCompatible(
      claim: ClaimSeed,
      record: EvidenceRecord,
      graph: TypedEvidenceGraph,
      samePositionLocal: Boolean
  ): Boolean =
    claim.primaryLine.exists(line => recordLineMatches(record, line)) ||
      claim.subjectMove.exists(move => recordMentionsMove(record, move, claim.primaryLine)) ||
      samePositionLocal ||
      linkedPositionContext(claim, record) ||
      linkedClaimSupport(claim, record, graph)

  private def linkedPositionContext(claim: ClaimSeed, record: EvidenceRecord): Boolean =
    claim.primaryLine.isEmpty &&
      claim.subjectMove.isEmpty &&
      record.ref.position == claim.primaryPosition &&
      positionContextLayer(record.ref.layer)

  private def linkedClaimSupport(claim: ClaimSeed, record: EvidenceRecord, graph: TypedEvidenceGraph): Boolean =
    val comparisonLineBound =
      comparisonLinesForClaim(claim, graph).exists(line => recordLineMatches(record, line))
    claim.primaryLine.nonEmpty &&
      (record.ref.position == claim.primaryPosition || comparisonLineBound) &&
      linkedSupportLayer(claim.family, record.ref.layer)

  private def linkedSupportLayer(family: ClaimFamily, layer: EvidenceLayer): Boolean =
    family match
      case ClaimFamily.Tactical =>
        layer match
          case EvidenceLayer.Relation | EvidenceLayer.MoveMotif | EvidenceLayer.Line | EvidenceLayer.Eval |
              EvidenceLayer.RelativeCause | EvidenceLayer.RelativeAssessment | EvidenceLayer.CandidateComparison |
              EvidenceLayer.Counterfactual | EvidenceLayer.MoveVerdictCertification =>
            true
          case _ =>
            false
      case ClaimFamily.Defensive =>
        layer match
          case EvidenceLayer.ThreatPressure | EvidenceLayer.RelativeCause | EvidenceLayer.Line |
              EvidenceLayer.RelativeAssessment | EvidenceLayer.CandidateComparison | EvidenceLayer.Counterfactual |
              EvidenceLayer.MoveVerdictCertification =>
            true
          case _ =>
            false
      case ClaimFamily.Plan =>
        layer match
          case EvidenceLayer.PlanPressure | EvidenceLayer.PlanTransition | EvidenceLayer.StructuralDelta |
              EvidenceLayer.PawnStructure | EvidenceLayer.Strategic =>
            true
          case _ =>
            false
      case ClaimFamily.Strategic | ClaimFamily.PawnStructure | ClaimFamily.Opening =>
        layer match
          case EvidenceLayer.StructuralDelta | EvidenceLayer.PawnStructure | EvidenceLayer.Strategic |
              EvidenceLayer.FeatureAnchor | EvidenceLayer.ApplicabilityAssessment | EvidenceLayer.OpeningContext |
              EvidenceLayer.PlanPressure | EvidenceLayer.PlanTransition =>
            true
          case _ =>
            false
      case ClaimFamily.Conversion | ClaimFamily.Material | ClaimFamily.Evaluation =>
        layer match
          case EvidenceLayer.Line | EvidenceLayer.Eval | EvidenceLayer.RelativeCause |
              EvidenceLayer.RelativeAssessment | EvidenceLayer.CandidateComparison | EvidenceLayer.Counterfactual |
              EvidenceLayer.MoveVerdictCertification | EvidenceLayer.SinglePosition =>
            true
          case _ =>
            false

  private def positionContextLayer(layer: EvidenceLayer): Boolean =
    layer match
      case EvidenceLayer.Board | EvidenceLayer.SinglePosition | EvidenceLayer.Line | EvidenceLayer.Eval =>
        true
      case _ =>
        false

  private def comparisonProofLayer(layer: EvidenceLayer): Boolean =
    layer match
      case EvidenceLayer.Line | EvidenceLayer.Eval =>
        true
      case _ =>
        false

  private def comparisonLinesForClaim(claim: ClaimSeed, graph: TypedEvidenceGraph): List[LineNodeRef] =
    val claimEvidenceIds = claim.evidence.map(_.id).toSet
    graph.records
      .filter(record => claimEvidenceIds.contains(record.ref.id))
      .flatMap(comparisonLinesForRecord)
      .distinct

  private def transitionDestinationsForClaim(claim: ClaimSeed, graph: TypedEvidenceGraph): Set[PositionNodeRef] =
    val claimMoves = claim.subjectMove.toSet ++ claim.primaryLine.map(_.rootMove).toSet
    if claimMoves.isEmpty then Set.empty
    else
      val claimEvidenceIds = claim.evidence.map(_.id).toSet
      graph.records
        .filter(record => claimEvidenceIds.contains(record.ref.id))
        .collect {
          case EvidenceRecord(_, MoveTransitionEvidence(moveUci, _, to), _) if claimMoves.contains(moveUci) =>
            to
        }
        .toSet

  private def comparisonLinesForRecord(record: EvidenceRecord): List[LineNodeRef] =
    record.payload match
      case CandidateComparisonEvidence(fact) =>
        List(fact.referenceLine, fact.candidateLine)
      case CounterfactualFactEvidence(referenceLine, candidateLine, _) =>
        List(referenceLine, candidateLine)
      case RelativeCauseFactEvidence(cause) =>
        (cause.referenceLine :: cause.candidateLine :: cause.evidenceLines).distinct
      case MoveVerdictCertificationEvidence(certification) =>
        (
          certification.primaryComparison.referenceLine ::
            certification.primaryComparison.candidateLine ::
            certification.causes.flatMap(cause => cause.referenceLine :: cause.candidateLine :: cause.evidenceLines)
        ).distinct
      case RelativeAssessmentEvidence(assessment) =>
        List(assessment.reference.ref, assessment.candidate.ref)
      case _ =>
        Nil

  private def recordLineMatches(record: EvidenceRecord, line: LineNodeRef): Boolean =
    record.referencesLine(line)

  private def recordMentionsMove(record: EvidenceRecord, move: String, claimLine: Option[LineNodeRef]): Boolean =
    record.payload match
      case MoveMotifEvidence(moveUci, _) =>
        moveUci == move
      case MoveTransitionEvidence(moveUci, _, _) =>
        moveUci == move &&
          claimLine.forall(line =>
            TransitionEdgeRole.fromScope(record.ref.scope).exists(role =>
              role.lineRole == line.role && line.rootMove == move
            )
          )
      case payload: StructuralDeltaEvidence =>
        payload.moveUci == move &&
          claimLine.forall(line => payload.line.contains(line))
      case CandidateComparisonEvidence(fact) =>
        claimLine.exists(line => line == fact.referenceLine || line == fact.candidateLine)
      case CounterfactualFactEvidence(referenceLine, candidateLine, _) =>
        claimLine.exists(line => line == referenceLine || line == candidateLine)
      case RelativeCauseFactEvidence(cause) =>
        claimLine.exists(line => line == cause.referenceLine || line == cause.candidateLine)
      case MoveVerdictCertificationEvidence(certification) =>
        certification.playedMove == move
      case _ =>
        false

  private def positionLocalLayer(layer: EvidenceLayer): Boolean =
    layer match
      case EvidenceLayer.Board | EvidenceLayer.SinglePosition | EvidenceLayer.PawnStructure |
          EvidenceLayer.Strategic | EvidenceLayer.OpeningContext | EvidenceLayer.FeatureAnchor |
          EvidenceLayer.ApplicabilityAssessment | EvidenceLayer.PlanPressure | EvidenceLayer.PlanTransition =>
        true
      case _ =>
        false

  private def requiredLayerGroups(family: ClaimFamily): List[Set[EvidenceLayer]] =
    family match
      case ClaimFamily.Tactical =>
        List(
          Set(EvidenceLayer.Relation, EvidenceLayer.MoveMotif, EvidenceLayer.ThreatPressure, EvidenceLayer.RelativeCause, EvidenceLayer.Line),
          Set(EvidenceLayer.Line),
          Set(EvidenceLayer.Eval, EvidenceLayer.RelativeAssessment, EvidenceLayer.Counterfactual, EvidenceLayer.CandidateComparison)
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
        List(Set(EvidenceLayer.ThreatPressure, EvidenceLayer.RelativeCause), Set(EvidenceLayer.Line, EvidenceLayer.RelativeAssessment))
      case ClaimFamily.Conversion =>
        List(Set(EvidenceLayer.SinglePosition, EvidenceLayer.RelativeAssessment, EvidenceLayer.RelativeCause), Set(EvidenceLayer.Eval, EvidenceLayer.Line))
      case ClaimFamily.Material =>
        List(
          Set(EvidenceLayer.RelativeCause, EvidenceLayer.MoveVerdictCertification),
          Set(EvidenceLayer.Line),
          Set(EvidenceLayer.Eval, EvidenceLayer.RelativeAssessment, EvidenceLayer.CandidateComparison, EvidenceLayer.Counterfactual)
        )
      case ClaimFamily.Evaluation =>
        List(
          Set(EvidenceLayer.RelativeAssessment, EvidenceLayer.MoveVerdictCertification),
          Set(EvidenceLayer.Counterfactual, EvidenceLayer.Eval, EvidenceLayer.CandidateComparison)
        )

  private def familySpecificProof(claim: ClaimSeed, records: List[EvidenceRecord]): Boolean =
    claim.family match
      case ClaimFamily.Tactical =>
        tacticalProof(records)
      case ClaimFamily.Defensive =>
        defensiveProof(claim, records)
      case ClaimFamily.Strategic =>
        strategicProof(records)
      case ClaimFamily.PawnStructure =>
        pawnStructureProof(records)
      case ClaimFamily.Opening =>
        openingProof(records)
      case ClaimFamily.Plan =>
        planProof(records)
      case ClaimFamily.Material =>
        materialProof(records)
      case _ => true

  private def strategicProof(records: List[EvidenceRecord]): Boolean =
    records.exists {
      case EvidenceRecord(_, payload @ StrategicFactEvidence(_, _, _, confidence), _) =>
        confidence >= 0.35 && payload.hasTypedSupport
      case EvidenceRecord(_, payload: PawnStructureFactEvidence, _) =>
        pawnStructureCanAnchorPlan(payload)
      case EvidenceRecord(_, payload: StructuralDeltaEvidence, _) =>
        payload.hasStrategicSupport
      case EvidenceRecord(_, PlanPressureEvidence(scoring, activePlans), _) =>
        planEvidenceCanSupportPlan(scoring, activePlans, records)
      case EvidenceRecord(_, PlanTransitionEvidence(transition), _) =>
        transition.primaryPlanId.nonEmpty && transition.transitionType != lila.chessjudgment.model.TransitionType.Opening
      case _ =>
        false
    }

  private def pawnStructureProof(records: List[EvidenceRecord]): Boolean =
    records.exists {
      case EvidenceRecord(_, payload: PawnStructureFactEvidence, _) =>
        pawnStructureCanAnchorPlan(payload)
      case EvidenceRecord(_, payload: StructuralDeltaEvidence, _) =>
        payload.hasPawnStructureDelta
      case _ =>
        false
    }

  private def openingProof(records: List[EvidenceRecord]): Boolean =
    val assessments = records.collect { case EvidenceRecord(_, ApplicabilityAssessmentEvidence(assessment), _) =>
      assessment
    }
    val proofSignalThemes =
      records.collect {
        case EvidenceRecord(_, FeatureAnchorEvidence(anchor), _)
            if anchor.hasPositiveStrength && anchor.canCorroborateOpeningPrior =>
          anchor.theme
      }.toSet
    assessments.exists(assessment =>
      assessment.canCertifyOpeningClaim &&
        assessment.supportedThemes.exists(proofSignalThemes.contains)
    )

  private[chessjudgment] def planPressureCanSeedIdea(
      scoring: PlanScoringResult,
      activePlans: ActivePlans,
      evidence: List[EvidenceRef],
      graph: TypedEvidenceGraph
  ): Boolean =
    val records = evidence.flatMap(ref => graph.byId.get(ref.id))
    planEvidenceCanSupportPlan(scoring, activePlans, records)

  private[chessjudgment] def planClaimApplicable(packet: EvidenceBackedJudgmentPacket): Boolean =
    packet.claims.exists(_.family == ClaimFamily.Plan) ||
      packet.evidenceGraph.records.exists {
        case EvidenceRecord(_, PlanPressureEvidence(scoring, activePlans), _) =>
          planEvidenceCanSupportPlan(scoring, activePlans, packet.evidenceGraph.records)
        case _ =>
          false
      }

  private def planProof(records: List[EvidenceRecord]): Boolean =
    records.exists {
      case EvidenceRecord(_, PlanPressureEvidence(scoring, activePlans), _) =>
        planEvidenceCanSupportPlan(scoring, activePlans, records)
      case _ =>
        false
    }

  private def planEvidenceCanSupportPlan(
      scoring: PlanScoringResult,
      activePlans: ActivePlans,
      records: List[EvidenceRecord]
  ): Boolean =
    planPressureHasDirectEvidence(scoring, activePlans) &&
      records.exists(independentPlanAnchor)

  private def independentPlanAnchor(record: EvidenceRecord): Boolean =
    record match
      case EvidenceRecord(_, payload @ StrategicFactEvidence(_, _, _, confidence), _) =>
        confidence >= 0.35 && payload.hasTypedSupport
      case EvidenceRecord(_, payload: PawnStructureFactEvidence, _) =>
        pawnStructureCanAnchorPlan(payload)
      case EvidenceRecord(_, payload: StructuralDeltaEvidence, _) =>
        payload.hasPositivePlanAnchor
      case _ =>
        false

  private[chessjudgment] def pawnStructureCanAnchorPlan(payload: PawnStructureFactEvidence): Boolean =
    payload.profile.primary != StructureId.Unknown && payload.profile.confidence >= 0.65 ||
      payload.alignment.exists(alignment =>
        alignment.band == AlignmentBand.OnBook ||
          alignment.band == AlignmentBand.Playable ||
          alignment.band == AlignmentBand.OffPlan
      ) ||
      payload.pawnPlay.exists(_.primaryDriver != PawnPlayDriver.Quiet)

  private def tacticalProof(records: List[EvidenceRecord]): Boolean =
    val hasTacticalAnchor =
      records.exists {
        case EvidenceRecord(_, _: RelationFactEvidence, _) =>
          true
        case EvidenceRecord(_, MoveMotifEvidence(moveUci, motifs), _) =>
          motifs.exists(motif =>
            TacticalMotifClassifier.isRootMoveMotif(moveUci, motif) &&
              TacticalMotifClassifier.isCauseEligible(motif)
          )
        case EvidenceRecord(_, RelativeCauseFactEvidence(cause), _) =>
          tacticalRelativeCause(cause.kind) ||
            (
              materialResultCause(cause.kind) &&
                (hasConcreteTacticalSupport(records) || hasMaterialTacticalSupport(records))
            )
        case EvidenceRecord(_, payload: LineFactEvidence, _) =>
          lineHasTacticalProof(payload)
        case EvidenceRecord(_, EvalFactEvidence(_, _, mate, _), _) =>
          mate.nonEmpty
        case _ =>
          false
      }
    val hasConcreteLine =
      records.exists {
        case EvidenceRecord(_, payload: LineFactEvidence, _) =>
          lineHasTacticalProof(payload)
        case EvidenceRecord(_, RelationFactEvidence(_, _, _, lineMoves, _), _) =>
          lineMoves.nonEmpty
        case EvidenceRecord(_, EvalFactEvidence(_, _, mate, _), _) =>
          mate.nonEmpty
        case _ =>
          false
      }
    val hasEngineProof =
      records.exists {
        case EvidenceRecord(_, EvalFactEvidence(_, _, mate, _), _) =>
          mate.nonEmpty
        case record @ EvidenceRecord(_, RelativeAssessmentEvidence(assessment), _) =>
          recordEngineBacked(record) &&
          engineComparisonProvesTactic(assessment.comparison)
        case record @ EvidenceRecord(_, CounterfactualFactEvidence(_, _, comparison), _) =>
          recordEngineBacked(record) &&
          engineComparisonProvesTactic(comparison)
        case record @ EvidenceRecord(_, CandidateComparisonEvidence(fact), _) =>
          recordEngineBacked(record) &&
          engineComparisonProvesTactic(fact.comparison)
        case record @ EvidenceRecord(_, RelativeCauseFactEvidence(cause), _) =>
          recordEngineBacked(record) &&
            (
              cause.winPercentLossForMover >= JudgmentThresholds.SIGNIFICANT_THREAT_WP ||
                cause.candidateWinPercentDeltaForMover >= JudgmentThresholds.PLAYABLE_LOSS_WP
            )
        case _ =>
          false
      }
    hasTacticalAnchor && hasConcreteLine && hasEngineProof

  private def defensiveProof(claim: ClaimSeed, records: List[EvidenceRecord]): Boolean =
    records.exists {
      case record @ EvidenceRecord(_, ThreatPressureEvidence(_, threats), _) =>
        threats.isProofSignalDefensivePressure &&
          (!branchLocalThreatPressure(record) || claimIsBranchLocal(claim))
      case EvidenceRecord(_, RelativeCauseFactEvidence(cause), _) =>
        defensiveRelativeCause(cause.kind)
      case EvidenceRecord(_, MoveVerdictCertificationEvidence(certification), _) =>
        certification.causes.exists(cause => defensiveRelativeCause(cause.kind))
      case _ =>
        false
    }

  private def claimIsBranchLocal(claim: ClaimSeed): Boolean =
    claim.scope == EvidenceScope.ThreatLine ||
      claim.primaryLine.exists(_.role == LineNodeRole.Threat)

  private def branchLocalThreatPressure(record: EvidenceRecord): Boolean =
    record.ref.scope == EvidenceScope.ThreatLine ||
      record.ref.line.exists(_.role == LineNodeRole.Threat)

  private def materialProof(records: List[EvidenceRecord]): Boolean =
    val hasMaterialCause =
      records.exists {
        case EvidenceRecord(_, RelativeCauseFactEvidence(cause), _) =>
          materialResultCause(cause.kind)
        case EvidenceRecord(_, MoveVerdictCertificationEvidence(certification), _) =>
          certification.causes.exists(cause => materialResultCause(cause.kind))
        case _ =>
          false
      }
    val hasMaterialLine =
      records.exists {
        case EvidenceRecord(_, payload: LineFactEvidence, _) =>
          payload.hasMaterialConsequence
        case _ =>
          false
      }
    val hasComparison =
      records.exists {
        case EvidenceRecord(_, EvalFactEvidence(_, _, mate, _), _) =>
          mate.nonEmpty
        case record @ EvidenceRecord(_, RelativeAssessmentEvidence(_), _) =>
          recordEngineBacked(record)
        case record @ EvidenceRecord(_, CounterfactualFactEvidence(_, _, _), _) =>
          recordEngineBacked(record)
        case record @ EvidenceRecord(_, CandidateComparisonEvidence(_), _) =>
          recordEngineBacked(record)
        case record @ EvidenceRecord(_, MoveVerdictCertificationEvidence(_), _) =>
          recordEngineBacked(record)
        case record @ EvidenceRecord(_, RelativeCauseFactEvidence(_), _) =>
          recordEngineBacked(record)
        case _ =>
          false
      }
    hasMaterialCause && hasMaterialLine && hasComparison

  private[chessjudgment] def planPressureHasDirectEvidence(scoring: PlanScoringResult, activePlans: ActivePlans): Boolean =
    scoring.confidence >= 0.35 &&
      (activePlans.primary :: activePlans.secondary.toList ++ scoring.topPlans)
        .exists(plan => nonTacticalPlan(plan) && plan.evidence.nonEmpty)

  private def nonTacticalPlan(plan: PlanMatch): Boolean =
    plan.support.collectFirst { case PlanSupport.Theme(theme) => theme } match
      case Some(PlanTheme.ImmediateTacticalGain) => false
      case _                                     => true

  private def recordEngineBacked(record: EvidenceRecord): Boolean =
    record.ref.confidence == EvidenceConfidence.EngineBacked ||
      recordHasMate(record)

  private def recordHasMate(record: EvidenceRecord): Boolean =
    record.payload match
      case EvalFactEvidence(_, _, mate, _) =>
        mate.nonEmpty
      case _ =>
        false

  private def engineComparisonProvesTactic(comparison: EvalComparison): Boolean =
    comparison.winPercentLossForMover >= JudgmentThresholds.SIGNIFICANT_THREAT_WP ||
      comparison.candidateWinPercentDeltaForMover >= JudgmentThresholds.PLAYABLE_LOSS_WP ||
      comparison.candidateSet.exists(_.onlyMove)

  private def tacticalRelativeCause(kind: RelativeCauseKind): Boolean =
    ClaimEventCluster.kindForCause(kind).contains(ClaimEventClusterKind.TacticalEvent)

  private def materialResultCause(kind: RelativeCauseKind): Boolean =
    ClaimEventCluster.kindForCause(kind).contains(ClaimEventClusterKind.MaterialEvent)

  private def hasConcreteTacticalSupport(records: List[EvidenceRecord]): Boolean =
    records.exists {
      case EvidenceRecord(_, _: RelationFactEvidence, _) =>
        true
      case EvidenceRecord(_, MoveMotifEvidence(moveUci, motifs), _) =>
        motifs.exists(motif =>
          TacticalMotifClassifier.isRootMoveMotif(moveUci, motif) &&
            TacticalMotifClassifier.isCauseEligible(motif)
        )
      case EvidenceRecord(_, payload: LineFactEvidence, _) =>
        lineHasTacticalProof(payload)
      case EvidenceRecord(_, EvalFactEvidence(_, _, mate, _), _) =>
        mate.nonEmpty
      case _ =>
        false
    }

  private def hasMaterialTacticalSupport(records: List[EvidenceRecord]): Boolean =
    records.exists {
      case EvidenceRecord(_, payload: LineFactEvidence, _) =>
        lineHasTacticalProof(payload)
      case _ =>
        false
    }

  private def lineHasTacticalProof(payload: LineFactEvidence): Boolean =
    payload.hasTacticalLineConsequence

  private def defensiveRelativeCause(kind: RelativeCauseKind): Boolean =
    ClaimEventCluster.kindForCause(kind).contains(ClaimEventClusterKind.DefensiveEvent)
