package lila.chessjudgment.analysis.assembly

import lila.chessjudgment.analysis.singlePosition.{ PawnPlayDriver, ThreatSeverity }
import lila.chessjudgment.analysis.evaluation.JudgmentThresholds
import lila.chessjudgment.analysis.policy.{ ClaimTruthDecision, ClaimTruthPolicy, ClaimTruthStatus }
import lila.chessjudgment.analysis.tactical.TacticalMotifClassifier
import lila.chessjudgment.model.Motif
import lila.chessjudgment.model.structure.{ AlignmentBand, StructureId }
import lila.chessjudgment.model.judgment.*

final case class ClaimCandidateGraph(
    decisions: List[ClaimTruthDecision],
    evidenceGraph: TypedEvidenceGraph
):
  def certified: List[ClaimTruthDecision] =
    decisions.filter(_.status == ClaimTruthStatus.Certified)

  def deferred: List[ClaimTruthDecision] =
    decisions.filter(_.status == ClaimTruthStatus.Deferred)

  def rejected: List[ClaimTruthDecision] =
    decisions.filter(_.status == ClaimTruthStatus.Rejected)

object ClaimCandidateGraphAssembler:

  def fromClaims(
      claims: List[ClaimSeed],
      graph: TypedEvidenceGraph
  ): ClaimCandidateGraph =
    ClaimCandidateGraph(claims.map(ClaimTruthPolicy.evaluate(_, graph)), graph)

object ClaimDeduplicator:

  private def aggregateCertified(
      decisions: List[ClaimTruthDecision],
      graph: TypedEvidenceGraph
  ): List[ClaimTruthDecision] =
    val grouped =
      decisions.zipWithIndex
        .collect { case (decision, index) if aggregationEligible(decision.claim, graph) =>
          aggregationKey(decision.claim, graph) -> (decision, index)
        }
        .groupBy(_._1)
    val aggregated =
      grouped.values.map { entries =>
        val decisionsInGroup = entries.map(_._2._1)
        entries.map(_._2._2).min -> mergeDecisions(decisionsInGroup)
      }.toList
    val passthrough =
      decisions.zipWithIndex.collect { case (decision, index) if !aggregationEligible(decision.claim, graph) =>
        index -> decision
      }
    (passthrough ++ aggregated).sortBy(_._1).map(_._2)

  def deduplicate(
      decisions: List[ClaimTruthDecision],
      graph: TypedEvidenceGraph
  ): List[ClaimTruthDecision] =
    aggregateCertified(decisions, graph)
      .groupBy(decision => key(decision.claim))
      .values
      .map(_.maxBy(score))
      .toList

  private def key(claim: ClaimSeed): (ClaimFamily, IdeaSubject, Option[LineNodeRef], Option[String], Option[ChessIdeaRef]) =
    (
      claim.family,
      claim.subject,
      claim.primaryLine,
      claim.subjectMove,
      claim.idea
    )

  private final case class AggregationKey(
      family: ClaimFamily,
      subject: IdeaSubject,
      primaryPosition: PositionNodeRef,
      primaryLine: Option[LineNodeRef],
      subjectMove: Option[String],
      scope: Option[EvidenceScope],
      semanticAnchors: List[EvidenceSemanticAnchor]
  )

  private def aggregationEligible(
      claim: ClaimSeed,
      graph: TypedEvidenceGraph
  ): Boolean =
    claim.family.isLongTerm &&
      claim.engineComparison.isEmpty &&
      claim.evidence.nonEmpty &&
      !claim.evidence.exists(ref => ClaimSupportCluster.causeBoundLayer(ref.layer)) &&
      ClaimSupportCluster.semanticAnchors(claim, graph).nonEmpty

  private def aggregationKey(
      claim: ClaimSeed,
      graph: TypedEvidenceGraph
  ): AggregationKey =
    AggregationKey(
      family = claim.family,
      subject = claim.subject,
      primaryPosition = claim.primaryPosition,
      primaryLine = claim.primaryLine,
      subjectMove = claim.subjectMove,
      scope = scopeAnchor(claim),
      semanticAnchors = ClaimSupportCluster.semanticAnchors(claim, graph)
    )

  private def scopeAnchor(claim: ClaimSeed): Option[EvidenceScope] =
    Option.when(claim.primaryLine.nonEmpty || claim.subjectMove.nonEmpty)(claim.scope)

  private def mergeDecisions(decisions: Iterable[ClaimTruthDecision]): ClaimTruthDecision =
    val list = decisions.toList
    val mergedClaim = mergeSeeds(list.map(_.claim))
    val mergedPresentLayers = list.flatMap(_.presentLayers).toSet
    val mergedMissingLayerGroups = list.flatMap(_.missingLayerGroups).distinct
    val mergedMissingEvidence = list.flatMap(_.missingEvidence).distinctBy(_.id)
    list.maxBy(score).copy(
      claim = mergedClaim,
      presentLayers = mergedPresentLayers,
      missingLayerGroups = mergedMissingLayerGroups,
      missingEvidence = mergedMissingEvidence
    )

  private def mergeSeeds(claims: Iterable[ClaimSeed]): ClaimSeed =
    val seeds = claims.toList
    val representative = seeds.maxBy(seedScore)
    if seeds.size == 1 then representative
    else
      representative.copy(
        evidence = seeds.flatMap(_.evidence).distinctBy(_.id),
        confidence = seeds.map(_.confidence).maxBy(confidenceScore),
        relatedIdeas = seeds.flatMap(_.ideaRefs).distinctBy(_.id),
        supportStatus = None,
        salience = None
      )

  private def seedScore(claim: ClaimSeed): Int =
    claim.evidence.size * 10 +
      claim.engineComparison.map(_ => 50).getOrElse(0) +
      confidenceScore(claim.confidence)

  private def score(decision: ClaimTruthDecision): Int =
    truthScore(decision.status) +
      decision.claim.evidence.size * 10 +
      confidenceScore(decision.claim.confidence)

  private def truthScore(status: ClaimTruthStatus): Int =
    status match
      case ClaimTruthStatus.Certified => 1000
      case ClaimTruthStatus.Deferred  => 100
      case ClaimTruthStatus.Rejected  => 0

  private def confidenceScore(confidence: EvidenceConfidence): Int =
    confidence match
      case EvidenceConfidence.LegalReplayVerified => 40
      case EvidenceConfidence.EngineBacked        => 30
      case EvidenceConfidence.BoardDerived        => 25
      case EvidenceConfidence.Mixed               => 15
      case EvidenceConfidence.Heuristic           => 5

object ClaimArbitrator:

  def rank(
      graph: ClaimCandidateGraph,
      relativeAssessments: List[RelativeMoveAssessment]
  ): List[ClaimSeed] =
    val globalVerdict = relativeAssessments.headOption.map(_.comparison.verdict)
    val playedMoves = relativeAssessments.map(assessment => normalizeMove(assessment.played.moveUci)).toSet
    val decisions = ClaimDeduplicator.deduplicate(graph.certified, graph.evidenceGraph)
    val claims = decisions.map(_.claim)
    decisions
      .map { decision =>
        val salience = claimSalience(decision.claim, graph.evidenceGraph)
        decision -> salience.copy(interactions = claimInteractions(decision.claim, claims, graph.evidenceGraph))
      }
      .sortBy { case (decision, salience) => -priority(decision, salience, globalVerdict, graph.evidenceGraph, playedMoves) }
      .map { case (decision, salience) =>
        decision.claim.copy(
          supportStatus = Some(supportCheck(decision)),
          salience = Some(salience)
        )
      }

  private def priority(
      decision: ClaimTruthDecision,
      salience: ClaimSalience,
      globalVerdict: Option[MoveChoiceVerdict],
      graph: TypedEvidenceGraph,
      playedMoves: Set[String]
  ): Int =
    val claim = decision.claim
    val verdict =
      if claim.family == ClaimFamily.Evaluation then
        claim.engineComparison.map(_.verdict).orElse(globalVerdict)
      else claimRelativeVerdicts(claim, graph).headOption
    val hasRelativeProof = claimRelativeComparisons(claim, graph).nonEmpty || claimRelativeVerdicts(claim, graph).nonEmpty
    salience.score * 1000 +
      truthPriority(decision.status) * 500 +
      verdictFit(claim, verdict, hasRelativeProof) * 200 +
      interactionPriority(salience) * 600 +
      playedMovePriority(claim, graph, playedMoves) * 400 +
      familyBaseline(claim.family) * 50 +
      confidencePriority(claim.confidence) * 40 +
      decision.presentLayers.size * 10 +
      claim.evidence.size

  private def playedMovePriority(
      claim: ClaimSeed,
      graph: TypedEvidenceGraph,
      playedMoves: Set[String]
  ): Int =
    JudgmentSubjectBinding.bindingScore(JudgmentSubjectBinding.claimBinding(claim, graph, playedMoves))

  private def supportCheck(decision: ClaimTruthDecision): ClaimSupportCheck =
    ClaimSupportCheck(
      status =
        decision.status match
          case ClaimTruthStatus.Certified => ClaimSupportStatus.Certified
          case ClaimTruthStatus.Deferred  => ClaimSupportStatus.Deferred
          case ClaimTruthStatus.Rejected  => ClaimSupportStatus.Deferred,
      presentLayers = decision.presentLayers,
      missingLayerGroups = decision.missingLayerGroups,
      missingEvidence = decision.missingEvidence
    )

  private def truthPriority(status: ClaimTruthStatus): Int =
    status match
      case ClaimTruthStatus.Certified => 2
      case ClaimTruthStatus.Deferred  => 0
      case ClaimTruthStatus.Rejected  => -4

  private def claimSalience(claim: ClaimSeed, graph: TypedEvidenceGraph): ClaimSalience =
    val records = claim.evidence.flatMap(ref => graph.byId.get(ref.id))
    val evidenceScore =
      records
        .groupBy(_.payload.layer)
        .values
        .map(recordsForLayer => recordsForLayer.map(recordSalience).maxOption.getOrElse(0))
        .sum
    val topLevelComparisonSalience =
      if claim.family == ClaimFamily.Evaluation then engineComparisonSalience(claim.engineComparison) else 0
    val drivers =
      (records.flatMap(recordDrivers) ++
        Option.when(topLevelComparisonSalience > 0)(ClaimSalienceDriver.EngineSwing) ++
        Option.when(claim.family == ClaimFamily.Evaluation)(claim.engineComparison).flatten.flatMap(_.candidateSet).toList.flatMap(cs =>
          Option.when(cs.onlyMove || cs.candidateCount <= 2)(ClaimSalienceDriver.CandidateConstraint)
        )).distinct
    ClaimSalience(
      score = (evidenceScore + topLevelComparisonSalience).min(30),
      drivers = drivers
    )

  private def claimInteractions(
      claim: ClaimSeed,
      claims: List[ClaimSeed],
      graph: TypedEvidenceGraph
  ): List[ClaimInteraction] =
    val related =
      claims
        .filterNot(_.id == claim.id)
        .filter(other => claimsShareGraphContext(claim, other, graph))
    val causeKinds = relativeCauseKinds(claim, graph)
    val badVerdict = claimHasBadRelativeOutcome(claim, graph)
    List.concat(
      Option
        .when(claim.family == ClaimFamily.Tactical && tacticalConstrainsLongTerm(claim, graph))(
          related.filter(_.family.isLongTerm).map { other =>
            val kind =
              if causeKinds.exists(kind =>
                  kind == RelativeCauseKind.TacticalRefutationOfPlayed ||
                    kind == RelativeCauseKind.MissedTacticalResource ||
                    kind == RelativeCauseKind.CandidateTacticalLiability
                )
              then ClaimInteractionKind.TacticalRefutesStrategicPlan
              else ClaimInteractionKind.TacticalConstrainsLongTerm
            ClaimInteraction(
              kind = kind,
              relatedClaimId = other.id,
              strength = tacticalConstraintStrength(claim, graph),
              interactionEvidence = interactionEvidence(claim, other)
            )
          }
        )
        .getOrElse(Nil),
      Option
        .when(claim.family == ClaimFamily.Tactical && tacticalSupportsLongTerm(claim, graph))(
          related.filter(_.family.isLongTerm).map { other =>
            ClaimInteraction(
              kind = ClaimInteractionKind.TacticalSupportsStrategicPlan,
              relatedClaimId = other.id,
              strength = 3,
              interactionEvidence = interactionEvidence(claim, other)
            )
          }
        )
        .getOrElse(Nil),
      Option
        .when(claim.family.isLongTerm)(
          related.filter(_.family == ClaimFamily.Tactical).flatMap { other =>
            if tacticalConstrainsLongTerm(other, graph) then
              Some(
                ClaimInteraction(
                  kind = ClaimInteractionKind.LongTermConstrainedByTactic,
                  relatedClaimId = other.id,
                  strength = tacticalConstraintStrength(other, graph),
                  interactionEvidence = interactionEvidence(claim, other)
                )
              )
            else if tacticalSupportsLongTerm(other, graph) then
              Some(
                ClaimInteraction(
                  kind = ClaimInteractionKind.TacticalSupportsStrategicPlan,
                  relatedClaimId = other.id,
                  strength = 3,
                  interactionEvidence = interactionEvidence(claim, other)
                )
              )
            else None
          }
        )
        .getOrElse(Nil),
      Option
        .when(claim.family == ClaimFamily.Defensive && causeKinds.exists(defensiveNecessityCause))(
          related.filter(_.family.isLongTerm).map { other =>
            ClaimInteraction(
              kind = ClaimInteractionKind.DefensiveNecessityOverridesPlan,
              relatedClaimId = other.id,
              strength = 6,
              interactionEvidence = interactionEvidence(claim, other)
            )
          }
        )
        .getOrElse(Nil),
      Option
        .when(claim.family == ClaimFamily.Conversion && causeKinds.contains(RelativeCauseKind.ConversionSecured))(
          related.filter(other => other.family.isLongTerm || other.family == ClaimFamily.Evaluation).map { other =>
            ClaimInteraction(
              kind = ClaimInteractionKind.ConversionSecuresAdvantage,
              relatedClaimId = other.id,
              strength = 4,
              interactionEvidence = interactionEvidence(claim, other)
            )
          }
        )
        .getOrElse(Nil),
      Option
        .when(claim.family == ClaimFamily.Material && causeKinds.contains(RelativeCauseKind.SacrificeCompensation))(
          related.filter(_.family.isLongTerm).map { other =>
            ClaimInteraction(
              kind = ClaimInteractionKind.StrategicCompensationSupportsSacrifice,
              relatedClaimId = other.id,
              strength = 4,
              interactionEvidence = interactionEvidence(claim, other)
            )
          }
        )
        .getOrElse(Nil),
      Option
        .when(badVerdict && claim.family != ClaimFamily.Evaluation)(
          related.filter(_.family == ClaimFamily.Evaluation).map { other =>
            ClaimInteraction(
              kind = ClaimInteractionKind.BadVerdictPreservesLocalIdea,
              relatedClaimId = other.id,
              strength = 5,
              interactionEvidence = interactionEvidence(claim, other)
            )
          }
        )
        .getOrElse(Nil)
    )
      .filter(_.interactionEvidence.nonEmpty)
      .distinctBy(interaction => (interaction.kind, interaction.relatedClaimId))

  private def tacticalConstrainsLongTerm(claim: ClaimSeed, graph: TypedEvidenceGraph): Boolean =
    val drivers = claimDrivers(claim, graph)
    val causeKinds = relativeCauseKinds(claim, graph)
    val hasMaterialTactic = causeKinds.exists(materialSwingCause)
    val hasTacticalDriver = drivers.contains(ClaimSalienceDriver.TacticalRelation) || hasMaterialTactic
    val hasForcingOrEngine =
      causeKinds.exists(tacticalRelativeCause) ||
      drivers.contains(ClaimSalienceDriver.ForcingLine) ||
      claimRelativeComparisons(claim, graph).exists(comparisonProvesTacticalConstraint)
    claim.family == ClaimFamily.Tactical && hasTacticalDriver && hasForcingOrEngine

  private def tacticalSupportsLongTerm(claim: ClaimSeed, graph: TypedEvidenceGraph): Boolean =
    val drivers = claimDrivers(claim, graph)
    claim.family == ClaimFamily.Tactical &&
      drivers.contains(ClaimSalienceDriver.TacticalRelation) &&
      !tacticalConstrainsLongTerm(claim, graph) &&
      claimRelativeComparisons(claim, graph).exists(_.winPercentLossForMover < JudgmentThresholds.INACCURACY_WP)

  private def tacticalConstraintStrength(claim: ClaimSeed, graph: TypedEvidenceGraph): Int =
    val drivers = claimDrivers(claim, graph)
    val engineStrength =
      claimRelativeComparisons(claim, graph).map { comparison =>
        if comparison.winPercentLossForMover >= JudgmentThresholds.BLUNDER_WP then 4
        else if comparison.winPercentLossForMover >= JudgmentThresholds.INACCURACY_WP then 3
        else if comparison.candidateWinPercentDeltaForMover >= JudgmentThresholds.PLAYABLE_LOSS_WP then 2
        else 0
      }.maxOption.getOrElse(0)
    List(
      Option.when(drivers.contains(ClaimSalienceDriver.TacticalRelation))(2),
      Option.when(drivers.contains(ClaimSalienceDriver.ForcingLine))(3),
      Option.when(drivers.contains(ClaimSalienceDriver.CandidateConstraint))(1),
      Option.when(drivers.contains(ClaimSalienceDriver.EngineSwing))(1),
      Option.when(engineStrength > 0)(engineStrength)
    ).flatten.sum.max(1).min(10)

  private def claimDrivers(claim: ClaimSeed, graph: TypedEvidenceGraph): List[ClaimSalienceDriver] =
    val records = claimRecords(claim, graph)
    (records.flatMap(recordDrivers) ++
      Option.when(claim.family == ClaimFamily.Evaluation && claim.engineComparison.nonEmpty)(ClaimSalienceDriver.EngineSwing) ++
      Option.when(claim.family == ClaimFamily.Evaluation)(claim.engineComparison).flatten.flatMap(_.candidateSet).toList.flatMap(cs =>
        Option.when(cs.onlyMove || cs.candidateCount <= 2)(ClaimSalienceDriver.CandidateConstraint)
      )).distinct

  private def comparisonProvesTacticalConstraint(comparison: EvalComparison): Boolean =
    comparison.winPercentLossForMover >= JudgmentThresholds.INACCURACY_WP ||
      comparison.candidateWinPercentDeltaForMover >= JudgmentThresholds.PLAYABLE_LOSS_WP ||
      comparison.candidateSet.exists(_.onlyMove)

  private def interactionPriority(salience: ClaimSalience): Int =
    salience.interactions
      .groupBy(_.kind)
      .values
      .map(interactions => interactions.map(interactionPriorityValue).maxOption.getOrElse(0))
      .sum

  private def interactionPriorityValue(interaction: ClaimInteraction): Int =
    interaction match
      case ClaimInteraction(ClaimInteractionKind.TacticalConstrainsLongTerm, _, strength, _) =>
        strength
      case ClaimInteraction(ClaimInteractionKind.LongTermConstrainedByTactic, _, strength, _) =>
        -strength
      case ClaimInteraction(ClaimInteractionKind.TacticalRefutesStrategicPlan, _, strength, _) =>
        if strength > 0 then strength + 2 else strength
      case ClaimInteraction(ClaimInteractionKind.TacticalSupportsStrategicPlan, _, strength, _) =>
        strength
      case ClaimInteraction(ClaimInteractionKind.DefensiveNecessityOverridesPlan, _, strength, _) =>
        strength
      case ClaimInteraction(ClaimInteractionKind.StrategicCompensationSupportsSacrifice, _, strength, _) =>
        strength
      case ClaimInteraction(ClaimInteractionKind.ConversionSecuresAdvantage, _, strength, _) =>
        strength
      case ClaimInteraction(ClaimInteractionKind.BadVerdictPreservesLocalIdea, _, strength, _) =>
        strength

  private def claimsShareGraphContext(left: ClaimSeed, right: ClaimSeed, graph: TypedEvidenceGraph): Boolean =
    left.primaryPosition == right.primaryPosition &&
      (sameSubjectMove(left, right) ||
        sameLineOrRootMove(left, right) ||
        evidenceLineOverlap(left, right) ||
        comparisonConnects(left, right, graph))

  private def sameSubjectMove(left: ClaimSeed, right: ClaimSeed): Boolean =
    left.subjectMove.exists(move => claimRootMoves(right).contains(move)) ||
      right.subjectMove.exists(move => claimRootMoves(left).contains(move))

  private def sameLineOrRootMove(left: ClaimSeed, right: ClaimSeed): Boolean =
    val leftLines = claimLines(left)
    val rightLines = claimLines(right)
    leftLines.exists(line => rightLines.contains(line))

  private def evidenceLineOverlap(left: ClaimSeed, right: ClaimSeed): Boolean =
    val leftLineIds = left.evidence.flatMap(_.line.map(_.id)).toSet
    val rightLineIds = right.evidence.flatMap(_.line.map(_.id)).toSet
    leftLineIds.nonEmpty && rightLineIds.nonEmpty && leftLineIds.intersect(rightLineIds).nonEmpty

  private def comparisonConnects(left: ClaimSeed, right: ClaimSeed, graph: TypedEvidenceGraph): Boolean =
    claimRelativeComparisons(left, graph).exists(comparisonTouchesClaim(_, right)) ||
      claimRelativeComparisons(right, graph).exists(comparisonTouchesClaim(_, left))

  private def comparisonTouchesClaim(comparison: EvalComparison, claim: ClaimSeed): Boolean =
    val comparisonLines = List(comparison.referenceLine, comparison.candidateLine)
    claimLines(claim).exists(line => comparisonLines.contains(line))

  private def claimLines(claim: ClaimSeed): List[LineNodeRef] =
    (claim.primaryLine.toList ++ claim.evidence.flatMap(_.line)).distinct

  private def claimRootMoves(claim: ClaimSeed): Set[String] =
    (claim.subjectMove.toList ++ claimLines(claim).map(_.rootMove)).toSet

  private def normalizeMove(raw: String): String =
    Option(raw).getOrElse("").trim.toLowerCase

  private def exactSharedEvidence(left: ClaimSeed, right: ClaimSeed): List[EvidenceRef] =
    val leftIds = left.evidence.map(_.id).toSet
    right.evidence.filter(ref => leftIds.contains(ref.id)).distinctBy(_.id)

  private def interactionEvidence(left: ClaimSeed, right: ClaimSeed): List[EvidenceRef] =
    val shared = exactSharedEvidence(left, right)
    if shared.nonEmpty then shared
    else
      val leftLines = claimLines(left).toSet
      val rightLines = claimLines(right).toSet
      val connectedLines = leftLines.intersect(rightLines)
      val connectedMoves = claimRootMoves(left).intersect(claimRootMoves(right))
      val lineBound =
        (left.evidence ++ right.evidence).filter(ref =>
          ref.line.exists(line => connectedLines.contains(line) || connectedMoves.contains(line.rootMove))
        )
      val subjectBound =
        (left.evidence ++ right.evidence).filter(ref =>
          left.subjectMove.exists(move => ref.line.exists(_.rootMove == move)) ||
            right.subjectMove.exists(move => ref.line.exists(_.rootMove == move))
        )
      (lineBound ++ subjectBound).distinctBy(_.id)

  private def claimRecords(claim: ClaimSeed, graph: TypedEvidenceGraph): List[EvidenceRecord] =
    claim.evidence.flatMap(ref => graph.byId.get(ref.id))

  private def claimRelativeComparisons(claim: ClaimSeed, graph: TypedEvidenceGraph): List[EvalComparison] =
    val recordComparisons =
      claimRecords(claim, graph).collect {
        case record @ EvidenceRecord(_, RelativeAssessmentEvidence(assessment), _) if comparisonRecordUsable(record) =>
          assessment.comparison
        case record @ EvidenceRecord(_, CounterfactualFactEvidence(_, _, comparison), _) if comparisonRecordUsable(record) =>
          comparison
        case record @ EvidenceRecord(_, CandidateComparisonEvidence(fact), _) if comparisonRecordUsable(record) =>
          fact.comparison
        case record @ EvidenceRecord(_, MoveVerdictCertificationEvidence(certification), _) if comparisonRecordUsable(record) =>
          certification.primaryComparison.comparison
      }
    val topLevelEvaluation =
      Option.when(claim.family == ClaimFamily.Evaluation)(claim.engineComparison).flatten.toList
    (recordComparisons ++ topLevelEvaluation).distinct

  private def comparisonRecordUsable(record: EvidenceRecord): Boolean =
    record.ref.confidence == EvidenceConfidence.EngineBacked

  private def claimRelativeVerdicts(claim: ClaimSeed, graph: TypedEvidenceGraph): List[MoveChoiceVerdict] =
    val recordVerdicts =
      claimRecords(claim, graph).flatMap {
        case EvidenceRecord(_, RelativeCauseFactEvidence(cause), _) =>
          List(cause.verdict)
        case EvidenceRecord(_, MoveVerdictCertificationEvidence(certification), _) =>
          certification.verdict :: certification.causes.map(_.verdict)
        case _ =>
          Nil
      }
    (recordVerdicts ++ claimRelativeComparisons(claim, graph).map(_.verdict)).distinct

  private def claimHasBadRelativeOutcome(claim: ClaimSeed, graph: TypedEvidenceGraph): Boolean =
    claimRelativeVerdicts(claim, graph).exists(badVerdict)

  private def badVerdict(verdict: MoveChoiceVerdict): Boolean =
    verdict == MoveChoiceVerdict.Inaccuracy ||
      verdict == MoveChoiceVerdict.Mistake ||
      verdict == MoveChoiceVerdict.Blunder

  private def relativeCauseKinds(claim: ClaimSeed, graph: TypedEvidenceGraph): List[RelativeCauseKind] =
    claimRecords(claim, graph).flatMap {
      case EvidenceRecord(_, RelativeCauseFactEvidence(cause), _) =>
        List(cause.kind)
      case EvidenceRecord(_, MoveVerdictCertificationEvidence(certification), _) =>
        certification.causes.map(_.kind)
      case _ =>
        Nil
    }.distinct

  private def defensiveNecessityCause(kind: RelativeCauseKind): Boolean =
    kind != RelativeCauseKind.DrawResource &&
      ClaimEventCluster.kindForCause(kind).contains(ClaimEventClusterKind.DefensiveEvent)

  private def tacticalRelativeCause(kind: RelativeCauseKind): Boolean =
    ClaimEventCluster.kindForCause(kind).contains(ClaimEventClusterKind.TacticalEvent)

  private def materialSwingCause(kind: RelativeCauseKind): Boolean =
    kind == RelativeCauseKind.MaterialSwing &&
      ClaimEventCluster.kindForCause(kind).contains(ClaimEventClusterKind.MaterialEvent)

  private def recordSalience(record: EvidenceRecord): Int =
    record.payload match
      case RelationFactEvidence(kind, _, _, lineMoves, participants) =>
        6 + relationKindSalience(kind) + lineMoves.take(3).size.min(2) + Option.when(participants.nonEmpty)(1).getOrElse(0)
      case ThreatPressureEvidence(_, threats) =>
        val severity =
          threats.threatSeverity match
            case ThreatSeverity.Urgent    => 10
            case ThreatSeverity.Important => 6
            case ThreatSeverity.Low       => 2
        severity +
          Option.when(threats.defenseRequired)(2).getOrElse(0) +
          Option.when(threats.defense.onlyDefense.nonEmpty)(2).getOrElse(0) +
          Option.when(threats.prophylaxisNeeded)(1).getOrElse(0)
      case PlanPressureEvidence(scoring, activePlans) =>
        val primary = math.round(activePlans.primary.score * 10).toInt
        primary +
          math.round(scoring.confidence * 4).toInt +
          activePlans.secondary.map(_ => 1).getOrElse(0)
      case PawnStructureFactEvidence(profile, alignment, pawnPlay) =>
        val structure = if profile.primary != StructureId.Unknown then math.round(profile.confidence * 6).toInt else 0
        val planAlignment = alignment.map { a =>
          a.band match
            case AlignmentBand.OnBook   => 5
            case AlignmentBand.Playable => 4
            case AlignmentBand.OffPlan  => 5
            case AlignmentBand.Unknown  => 0
        }.getOrElse(0)
        val pawnDriver = pawnPlay.map { play =>
          play.primaryDriver match
            case PawnPlayDriver.PassedPawn | PawnPlayDriver.BreakReady | PawnPlayDriver.TensionCritical => 5
            case PawnPlayDriver.TensionActive | PawnPlayDriver.Defensive                                => 3
            case PawnPlayDriver.Quiet                                                                   => 0
        }.getOrElse(0)
        structure + planAlignment + pawnDriver
      case payload @ StrategicFactEvidence(kind, _, _, confidence) =>
        if strategicFactHasClaimAnchor(payload) then math.round(confidence * 5).toInt + strategicKindSalience(kind) else 0
      case RelativeAssessmentEvidence(assessment) =>
        engineComparisonSalience(Some(assessment.comparison))
      case CounterfactualFactEvidence(_, _, comparison) =>
        engineComparisonSalience(Some(comparison))
      case CandidateComparisonEvidence(fact) =>
        engineComparisonSalience(Some(fact.comparison)) + candidateComparisonKindSalience(fact.kind)
      case RelativeCauseFactEvidence(cause) =>
        relativeCauseSalience(cause)
      case MoveVerdictCertificationEvidence(certification) =>
        engineComparisonSalience(Some(certification.primaryComparison.comparison)) + certification.causes.size.min(4)
      case StructuralDeltaEvidence(delta) =>
        if delta.hasConsequence then 5 else 1
      case OpeningContextEvidence(_, _, _, _) =>
        0
      case FeatureAnchorEvidence(anchor) =>
        math.round(anchor.strength * 6).toInt + 2
      case ApplicabilityAssessmentEvidence(assessment) =>
        val statusScore =
          assessment.status match
            case ApplicabilityStatus.Supported          => 6
            case ApplicabilityStatus.PartiallySupported => 4
            case ApplicabilityStatus.InternalOnly       => 3
            case ApplicabilityStatus.Unverified         => 1
            case ApplicabilityStatus.Ambiguous          => 1
            case ApplicabilityStatus.Contradicted       => 0
        statusScore + assessment.observedThemes.size.min(4)
      case SinglePositionEvidence(_) =>
        1
      case _: BoardFactEvidence =>
        0
      case _: LineFactEvidence =>
        0
      case EvalFactEvidence(_, _, mate, depth) =>
        mate.map(_ => 5).getOrElse(if depth >= 16 then 2 else 1)
      case MoveMotifEvidence(moveUci, motifs) =>
        rootMoveMotifSalience(moveUci, motifs)
      case MoveTransitionEvidence(_, _, _) | PlanTransitionEvidence(_) | ChessIdeaEvidence(_) | ClaimEvidence(_) =>
        1

  private def recordDrivers(record: EvidenceRecord): List[ClaimSalienceDriver] =
    record.payload match
      case RelationFactEvidence(kind, _, _, _, _) =>
        ClaimSalienceDriver.TacticalRelation ::
          Option.when(isForcingRelation(kind))(ClaimSalienceDriver.ForcingLine).toList
      case ThreatPressureEvidence(_, threats) =>
        Option.when(threats.threatSeverity != ThreatSeverity.Low)(ClaimSalienceDriver.DefensiveUrgency).toList
      case PlanPressureEvidence(_, _) =>
        List(ClaimSalienceDriver.PlanPressure)
      case PawnStructureFactEvidence(_, alignment, pawnPlay) =>
        Option.when(alignment.nonEmpty || pawnPlay.exists(_.primaryDriver != PawnPlayDriver.Quiet))(
          ClaimSalienceDriver.PawnStructureAlignment
        ).toList
      case payload @ StrategicFactEvidence(StrategicFactKind.Endgame, _, _, _) if payload.hasTypedSupport =>
        List(ClaimSalienceDriver.EndgamePattern)
      case payload: StrategicFactEvidence if payload.hasTypedSupport =>
        List(ClaimSalienceDriver.StrategicFeature)
      case StructuralDeltaEvidence(delta) =>
        Option.when(delta.hasConsequence)(ClaimSalienceDriver.StructuralChange).toList
      case FeatureAnchorEvidence(_) =>
        List(ClaimSalienceDriver.BoardAnchor)
      case ApplicabilityAssessmentEvidence(assessment) if assessment.canCertifyOpeningClaim =>
        List(ClaimSalienceDriver.OpeningContext)
      case RelativeAssessmentEvidence(_) | CounterfactualFactEvidence(_, _, _) =>
        List(ClaimSalienceDriver.EngineSwing)
      case CandidateComparisonEvidence(fact) =>
        ClaimSalienceDriver.EngineSwing ::
          Option.when(fact.kind == CandidateComparisonKind.BestVsSecond)(ClaimSalienceDriver.CandidateConstraint).toList
      case RelativeCauseFactEvidence(cause) =>
        relativeCauseDrivers(cause.kind)
      case MoveVerdictCertificationEvidence(certification) =>
        ClaimSalienceDriver.EngineSwing ::
          Option.when(certification.causes.exists(cause => defensiveNecessityCause(cause.kind)))(ClaimSalienceDriver.CandidateConstraint).toList
      case payload: LineFactEvidence if payload.hasConcreteLineConsequence =>
        List(ClaimSalienceDriver.ForcingLine)
      case MoveMotifEvidence(moveUci, motifs) if rootCastlingMotif(moveUci, motifs) =>
        List(ClaimSalienceDriver.StrategicFeature, ClaimSalienceDriver.BoardAnchor)
      case MoveMotifEvidence(moveUci, motifs)
          if motifs.exists(motif =>
            TacticalMotifClassifier.isRootMoveMotif(moveUci, motif) &&
              TacticalMotifClassifier.isCauseEligible(motif)
          ) =>
        ClaimSalienceDriver.TacticalRelation ::
          Option
            .when(motifs.exists(motif =>
              TacticalMotifClassifier.isRootMoveMotif(moveUci, motif) &&
                TacticalMotifClassifier.isForcing(motif)
            ))(ClaimSalienceDriver.ForcingLine)
            .toList
      case _ =>
        Nil

  private def rootMoveMotifSalience(moveUci: String, motifs: List[Motif]): Int =
    if rootCastlingMotif(moveUci, motifs) then 4
    else motifs.count(TacticalMotifClassifier.isRootMoveMotif(moveUci, _)).min(4)

  private def rootCastlingMotif(moveUci: String, motifs: List[Motif]): Boolean =
    motifs.exists {
      case Motif.Castling(_, _, plyIndex, move) =>
        move.contains(moveUci) || (move.isEmpty && plyIndex == 0 && castlingMove(moveUci))
      case _ =>
        false
    }

  private def castlingMove(moveUci: String): Boolean =
    moveUci == "e1g1" || moveUci == "e1c1" || moveUci == "e8g8" || moveUci == "e8c8"

  private def relationKindSalience(kind: RelationFactKind): Int =
    kind match
      case RelationFactKind.BackRankMate | RelationFactKind.MateNet | RelationFactKind.DoubleCheck =>
        6
      case RelationFactKind.Fork | RelationFactKind.Pin | RelationFactKind.Skewer |
          RelationFactKind.Deflection | RelationFactKind.Overload | RelationFactKind.DiscoveredAttack |
          RelationFactKind.XRay | RelationFactKind.Zwischenzug =>
        4
      case RelationFactKind.DefenderTrade | RelationFactKind.Clearance | RelationFactKind.Interference |
          RelationFactKind.Decoy | RelationFactKind.Battery | RelationFactKind.GreekGift =>
        3
      case RelationFactKind.HangingPiece | RelationFactKind.TrappedPiece | RelationFactKind.Domination =>
        2
      case RelationFactKind.BadPieceLiquidation | RelationFactKind.StalemateTrap | RelationFactKind.PerpetualCheck =>
        2

  private def isForcingRelation(kind: RelationFactKind): Boolean =
    kind match
      case RelationFactKind.BackRankMate | RelationFactKind.MateNet | RelationFactKind.DoubleCheck |
          RelationFactKind.Zwischenzug | RelationFactKind.PerpetualCheck | RelationFactKind.StalemateTrap =>
        true
      case _ =>
        false

  private def strategicKindSalience(kind: StrategicFactKind): Int =
    kind match
      case StrategicFactKind.Endgame | StrategicFactKind.CounterplayRestraint | StrategicFactKind.TargetFixation =>
        4
      case StrategicFactKind.PlanPressure | StrategicFactKind.Structure | StrategicFactKind.Compensation =>
        3
      case StrategicFactKind.Outpost | StrategicFactKind.FileControl | StrategicFactKind.Space | StrategicFactKind.Activity =>
        2
      case StrategicFactKind.Practicality =>
        1

  private def strategicFactHasClaimAnchor(payload: StrategicFactEvidence): Boolean =
    payload.hasTypedSupport

  private def engineComparisonSalience(comparison: Option[EvalComparison]): Int =
    comparison.map { cmp =>
      val verdictScore =
        cmp.verdict match
          case MoveChoiceVerdict.Blunder             => 10
          case MoveChoiceVerdict.Mistake             => 8
          case MoveChoiceVerdict.Inaccuracy          => 6
          case MoveChoiceVerdict.PlayableLoss        => 4
          case MoveChoiceVerdict.MatchesReference    => 3
          case MoveChoiceVerdict.ImprovesOnReference => 4
      verdictScore +
        math.round(cmp.winPercentLossForMover.min(20.0) / 4.0).toInt +
        cmp.candidateSet.map(cs => if cs.onlyMove then 3 else if cs.candidateCount <= 2 then 1 else 0).getOrElse(0)
    }.getOrElse(0)

  private def candidateComparisonKindSalience(kind: CandidateComparisonKind): Int =
    kind match
      case CandidateComparisonKind.PlayedVsBest          => 4
      case CandidateComparisonKind.BestVsSecond          => 3
      case CandidateComparisonKind.PlayedVsAlternative   => 2
      case CandidateComparisonKind.ReferenceVsAlternative => 1

  private def relativeCauseSalience(cause: RelativeCauseFact): Int =
    val causeScore =
      cause.kind match
        case RelativeCauseKind.TacticalRefutationOfPlayed | RelativeCauseKind.MissedTacticalResource |
            RelativeCauseKind.CandidateTacticalLiability |
            RelativeCauseKind.WrongRecapturer | RelativeCauseKind.RecaptureRecoveryWindow |
            RelativeCauseKind.TempoLoss | RelativeCauseKind.KingForcing =>
          8
        case RelativeCauseKind.OnlyMoveNecessity | RelativeCauseKind.OnlyDefenseNecessity =>
          7
        case RelativeCauseKind.ConversionMiss | RelativeCauseKind.ConversionSecured |
            RelativeCauseKind.PlanContradiction | RelativeCauseKind.CastlingRightsConcession |
            RelativeCauseKind.StrategicConcession | RelativeCauseKind.StrategicIdeaRefuted |
            RelativeCauseKind.MissedStrategicImprovement | RelativeCauseKind.StructuralImprovement |
            RelativeCauseKind.TargetPressureGain | RelativeCauseKind.CenterControlGain | RelativeCauseKind.DevelopmentActivation |
            RelativeCauseKind.PieceActivityGain =>
          5
        case RelativeCauseKind.DefensiveResource | RelativeCauseKind.DrawResource |
            RelativeCauseKind.PlanImprovement | RelativeCauseKind.SacrificeCompensation =>
          4
        case RelativeCauseKind.MaterialSwing =>
          3
        case RelativeCauseKind.WrongMoveOrder =>
          6
    val proofScore = cause.proof.map(relativeCauseProofSalience).getOrElse(0)
    causeScore + proofScore + math.round(cause.winPercentLossForMover.min(20.0) / 5.0).toInt

  private def relativeCauseProofSalience(proof: RelativeCauseProof): Int =
    val board = Option.when(proof.boardAnchors.nonEmpty)(2).getOrElse(0)
    val line =
      Option.when(proof.lineEvents.nonEmpty)(1).getOrElse(0) +
        Option.when(proof.lineConsequences.nonEmpty)(2).getOrElse(0)
    val relation = Option.when(proof.relationKinds.nonEmpty)(3).getOrElse(0)
    board + line + relation

  private def relativeCauseDrivers(kind: RelativeCauseKind): List[ClaimSalienceDriver] =
    kind match
      case RelativeCauseKind.MissedTacticalResource | RelativeCauseKind.TacticalRefutationOfPlayed |
          RelativeCauseKind.CandidateTacticalLiability |
          RelativeCauseKind.WrongRecapturer | RelativeCauseKind.RecaptureRecoveryWindow |
          RelativeCauseKind.WrongMoveOrder | RelativeCauseKind.TempoLoss | RelativeCauseKind.KingForcing =>
        List(ClaimSalienceDriver.TacticalRelation, ClaimSalienceDriver.EngineSwing)
      case RelativeCauseKind.MaterialSwing =>
        List(ClaimSalienceDriver.StructuralChange, ClaimSalienceDriver.EngineSwing)
      case RelativeCauseKind.SacrificeCompensation =>
        List(ClaimSalienceDriver.StrategicFeature, ClaimSalienceDriver.EngineSwing)
      case RelativeCauseKind.OnlyMoveNecessity =>
        List(ClaimSalienceDriver.CandidateConstraint, ClaimSalienceDriver.EngineSwing)
      case RelativeCauseKind.OnlyDefenseNecessity | RelativeCauseKind.DefensiveResource | RelativeCauseKind.DrawResource =>
        List(ClaimSalienceDriver.DefensiveUrgency, ClaimSalienceDriver.EngineSwing)
      case RelativeCauseKind.ConversionMiss | RelativeCauseKind.ConversionSecured =>
        List(ClaimSalienceDriver.StructuralChange, ClaimSalienceDriver.EngineSwing)
      case RelativeCauseKind.StructuralImprovement =>
        List(ClaimSalienceDriver.StrategicFeature, ClaimSalienceDriver.StructuralChange, ClaimSalienceDriver.EngineSwing)
      case RelativeCauseKind.TargetPressureGain | RelativeCauseKind.CenterControlGain |
          RelativeCauseKind.DevelopmentActivation | RelativeCauseKind.PieceActivityGain =>
        List(ClaimSalienceDriver.StrategicFeature, ClaimSalienceDriver.EngineSwing)
      case RelativeCauseKind.CastlingRightsConcession | RelativeCauseKind.StrategicConcession |
          RelativeCauseKind.StrategicIdeaRefuted | RelativeCauseKind.MissedStrategicImprovement | RelativeCauseKind.PlanImprovement |
          RelativeCauseKind.PlanContradiction =>
        List(ClaimSalienceDriver.StrategicFeature, ClaimSalienceDriver.EngineSwing)

  private def verdictFit(
      claim: ClaimSeed,
      verdict: Option[MoveChoiceVerdict],
      hasRelativeProof: Boolean
  ): Int =
    verdict match
      case Some(MoveChoiceVerdict.Blunder | MoveChoiceVerdict.Mistake | MoveChoiceVerdict.Inaccuracy) =>
        claim.family match
          case ClaimFamily.Evaluation => 6
          case ClaimFamily.Tactical | ClaimFamily.Defensive => 5
          case ClaimFamily.Material => 4
          case ClaimFamily.PawnStructure | ClaimFamily.Strategic | ClaimFamily.Plan =>
            if hasRelativeProof then 4 else 2
          case ClaimFamily.Conversion | ClaimFamily.Opening => 2
      case _ =>
        claim.family match
          case ClaimFamily.Tactical      => 5
          case ClaimFamily.Defensive     => 5
          case ClaimFamily.Material      => 4
          case ClaimFamily.PawnStructure => 4
          case ClaimFamily.Strategic | ClaimFamily.Plan => 4
          case ClaimFamily.Conversion    => 3
          case ClaimFamily.Evaluation    => 2
          case ClaimFamily.Opening       => 2

  private def familyBaseline(family: ClaimFamily): Int =
    family match
      case ClaimFamily.Tactical | ClaimFamily.Defensive => 3
      case ClaimFamily.Material => 2
      case ClaimFamily.Strategic | ClaimFamily.PawnStructure | ClaimFamily.Plan => 2
      case ClaimFamily.Evaluation | ClaimFamily.Conversion | ClaimFamily.Opening => 1

  private def confidencePriority(confidence: EvidenceConfidence): Int =
    confidence match
      case EvidenceConfidence.LegalReplayVerified => 5
      case EvidenceConfidence.EngineBacked        => 4
      case EvidenceConfidence.BoardDerived        => 3
      case EvidenceConfidence.Mixed               => 2
      case EvidenceConfidence.Heuristic           => 1
