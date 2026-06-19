package lila.chessjudgment.analysis.assembly

import lila.chessjudgment.analysis.singlePosition.{ PawnPlayDriver, ThreatSeverity }
import lila.chessjudgment.analysis.policy.{ ClaimTruthDecision, ClaimTruthPolicy, ClaimTruthStatus }
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

  def deduplicate(decisions: List[ClaimTruthDecision]): List[ClaimTruthDecision] =
    decisions
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

  private def score(decision: ClaimTruthDecision): Int =
    truthScore(decision.status) +
      decision.claim.evidence.size * 10 +
      decision.claim.supportingFacts.size +
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
    ClaimDeduplicator
      .deduplicate(graph.certified ++ graph.deferred)
      .map(decision => decision -> claimSalience(decision.claim, graph.evidenceGraph))
      .sortBy { case (decision, salience) => -priority(decision, salience, globalVerdict) }
      .map { case (decision, salience) =>
        decision.claim.copy(
          supportStatus = Some(supportCheck(decision)),
          salience = Some(salience)
        )
      }

  private def priority(
      decision: ClaimTruthDecision,
      salience: ClaimSalience,
      globalVerdict: Option[MoveChoiceVerdict]
  ): Int =
    val claim = decision.claim
    val verdict = claim.engineComparison.map(_.verdict).orElse(globalVerdict)
    salience.score * 1000 +
      truthPriority(decision.status) * 500 +
      verdictFit(claim, verdict) * 200 +
      familyBaseline(claim.family) * 50 +
      confidencePriority(claim.confidence) * 40 +
      decision.presentLayers.size * 10 +
      claim.evidence.size

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
    val comparisonSalience = engineComparisonSalience(claim.engineComparison)
    val drivers =
      (records.flatMap(recordDrivers) ++
        Option.when(comparisonSalience > 0)(ClaimSalienceDriver.EngineSwing) ++
        claim.engineComparison.flatMap(_.candidateSet).toList.flatMap(cs =>
          Option.when(cs.onlyMove || cs.candidateCount <= 2)(ClaimSalienceDriver.CandidateConstraint)
        ) ++
        Option.when(claim.supportingFacts.nonEmpty)(ClaimSalienceDriver.BoardAnchor)).distinct
    ClaimSalience(
      score = (evidenceScore + supportingFactSalience(claim) + comparisonSalience).min(30),
      drivers = drivers
    )

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
      case StrategicFactEvidence(kind, _, _, confidence) =>
        math.round(confidence * 5).toInt + strategicKindSalience(kind)
      case RelativeAssessmentEvidence(assessment) =>
        engineComparisonSalience(Some(assessment.comparison))
      case CounterfactualFactEvidence(_, _, comparison) =>
        engineComparisonSalience(Some(comparison))
      case StructuralDeltaEvidence(delta) =>
        if delta.hasConsequence then 5 else 1
      case OpeningContextEvidence(identity, signals, _, _) =>
        identity.map(id => List(id.eco, id.name, id.family).count(_.nonEmpty)).getOrElse(0) +
          signals.size.min(2)
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
      case BoardFactEvidence(facts, _) =>
        facts.size.min(3)
      case LineFactEvidence(_, firstMove, replyMove, continuationMoves, forcedTheme) =>
        firstMove.size + replyMove.size + continuationMoves.take(2).size + forcedTheme.map(_ => 3).getOrElse(0)
      case EvalFactEvidence(_, _, mate, depth) =>
        mate.map(_ => 5).getOrElse(if depth >= 16 then 2 else 1)
      case MoveMotifEvidence(_, motifs) =>
        motifs.size.min(4)
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
      case StrategicFactEvidence(StrategicFactKind.Endgame, _, _, _) =>
        List(ClaimSalienceDriver.EndgamePattern)
      case StrategicFactEvidence(_, _, _, _) =>
        List(ClaimSalienceDriver.StrategicFeature)
      case StructuralDeltaEvidence(delta) =>
        Option.when(delta.hasConsequence)(ClaimSalienceDriver.StructuralChange).toList
      case FeatureAnchorEvidence(_) =>
        List(ClaimSalienceDriver.BoardAnchor)
      case ApplicabilityAssessmentEvidence(assessment)
          if assessment.applicability == FeatureApplicability.OpeningRelevant && assessment.observedThemes.nonEmpty =>
        List(ClaimSalienceDriver.OpeningContext)
      case RelativeAssessmentEvidence(_) | CounterfactualFactEvidence(_, _, _) =>
        List(ClaimSalienceDriver.EngineSwing)
      case LineFactEvidence(_, _, _, _, Some(_)) =>
        List(ClaimSalienceDriver.ForcingLine)
      case BoardFactEvidence(facts, _) if facts.nonEmpty =>
        List(ClaimSalienceDriver.BoardAnchor)
      case _ =>
        Nil

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

  private def supportingFactSalience(claim: ClaimSeed): Int =
    claim.supportingFacts.size.min(4)

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

  private def verdictFit(
      claim: ClaimSeed,
      verdict: Option[MoveChoiceVerdict]
  ): Int =
    verdict match
      case Some(MoveChoiceVerdict.Blunder | MoveChoiceVerdict.Mistake | MoveChoiceVerdict.Inaccuracy) =>
        claim.family match
          case ClaimFamily.Evaluation => 6
          case ClaimFamily.Tactical | ClaimFamily.Defensive => 5
          case ClaimFamily.PawnStructure | ClaimFamily.Strategic | ClaimFamily.Plan =>
            if claim.engineComparison.nonEmpty then 4 else 2
          case ClaimFamily.Conversion | ClaimFamily.Opening => 2
      case _ =>
        claim.family match
          case ClaimFamily.Tactical      => 5
          case ClaimFamily.Defensive     => 5
          case ClaimFamily.PawnStructure => 4
          case ClaimFamily.Strategic | ClaimFamily.Plan => 4
          case ClaimFamily.Conversion    => 3
          case ClaimFamily.Evaluation    => 2
          case ClaimFamily.Opening       => 2

  private def familyBaseline(family: ClaimFamily): Int =
    family match
      case ClaimFamily.Tactical | ClaimFamily.Defensive => 3
      case ClaimFamily.Strategic | ClaimFamily.PawnStructure | ClaimFamily.Plan => 2
      case ClaimFamily.Evaluation | ClaimFamily.Conversion | ClaimFamily.Opening => 1

  private def confidencePriority(confidence: EvidenceConfidence): Int =
    confidence match
      case EvidenceConfidence.LegalReplayVerified => 5
      case EvidenceConfidence.EngineBacked        => 4
      case EvidenceConfidence.BoardDerived        => 3
      case EvidenceConfidence.Mixed               => 2
      case EvidenceConfidence.Heuristic           => 1
