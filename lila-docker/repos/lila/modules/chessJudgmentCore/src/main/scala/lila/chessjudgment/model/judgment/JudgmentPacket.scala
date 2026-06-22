package lila.chessjudgment.model.judgment

import lila.chessjudgment.model.{ ProbeAdmissionDiagnostic, ProbeRequest }
import lila.chessjudgment.model.structure.StructureId

enum ClaimFamily:
  case Tactical
  case Strategic
  case PawnStructure
  case Opening
  case Plan
  case Defensive
  case Conversion
  case Material
  case Evaluation

  private[chessjudgment] def isLongTerm: Boolean =
    this match
      case Strategic | PawnStructure | Opening | Plan => true
      case Tactical | Defensive | Conversion | Material | Evaluation => false

  private[chessjudgment] def isEvent: Boolean =
    !isLongTerm

enum ClaimSalienceDriver:
  case TacticalRelation
  case ForcingLine
  case DefensiveUrgency
  case PlanPressure
  case PawnStructureAlignment
  case StructuralChange
  case StrategicFeature
  case OpeningContext
  case EndgamePattern
  case EngineSwing
  case CandidateConstraint
  case BoardAnchor

enum ClaimInteractionKind:
  case TacticalConstrainsLongTerm
  case LongTermConstrainedByTactic
  case TacticalRefutesStrategicPlan
  case TacticalSupportsStrategicPlan
  case DefensiveNecessityOverridesPlan
  case StrategicCompensationSupportsSacrifice
  case ConversionSecuresAdvantage
  case BadVerdictPreservesLocalIdea

enum SubjectBindingClass:
  case DirectPlayed
  case PrimaryPlayedCause
  case ContextPlayed
  case Other

enum PlayedSubjectBinding:
  case SubjectMove
  case PlayedLine
  case SubjectMoveAndPlayedLine
  case Unbound

enum ClaimLifecycleStage:
  case CandidateCreated
  case TruthCertified
  case TruthRejected
  case TruthDeferred
  case DedupeDropped
  case ArbitrationSuppressed
  case FinalPacketIncluded

enum ClaimLifecycleTruthStatus:
  case Certified
  case Deferred
  case Rejected

final case class ClaimLifecycleRelativeCause(
    id: String,
    kind: RelativeCauseKind,
    role: RelativeCauseRole,
    comparisonKind: CandidateComparisonKind,
    sourceSide: RelativeCauseSourceSide,
    importance: RelativeCauseImportance,
    referenceLine: LineNodeRef,
    candidateLine: LineNodeRef,
    eventLine: LineNodeRef,
    proofDirectSourceIds: List[String],
    proofContrastSourceIds: List[String],
    proofContextSupportSourceIds: List[String],
    proofDirectKinds: List[String],
    proofContrastKinds: List[String],
    proofContextSupportKinds: List[String]
)

final case class ClaimLifecycleDiagnostic(
    candidateId: String,
    claimId: String,
    finalClaimId: Option[String],
    sourceCandidateIds: List[String],
    family: ClaimFamily,
    subject: IdeaSubject,
    subjectBinding: SubjectBindingClass,
    primaryLine: Option[LineNodeRef],
    subjectMove: Option[String],
    ideaIds: List[String],
    finalIdeaIds: List[String],
    evidenceIds: List[String],
    finalEvidenceIds: List[String],
    relativeCauses: List[ClaimLifecycleRelativeCause],
    truthStatus: Option[ClaimLifecycleTruthStatus],
    presentLayers: Set[EvidenceLayer],
    missingLayerGroups: List[Set[EvidenceLayer]],
    missingEvidenceIds: List[String],
    stages: List[ClaimLifecycleStage],
    dedupeWinnerId: Option[String],
    arbitrationRank: Option[Int],
    finalPacketIncluded: Boolean
)

object JudgmentSubjectBinding:

  def claimBinding(packet: EvidenceBackedJudgmentPacket, claim: ClaimSeed): SubjectBindingClass =
    claimBinding(claim, packet.evidenceGraph, packetPlayedMoves(packet))

  def claimBinding(
      claim: ClaimSeed,
      graph: TypedEvidenceGraph,
      playedMoves: Set[String]
  ): SubjectBindingClass =
    if playedMoves.isEmpty then SubjectBindingClass.Other
    else
      val evidenceBinding =
        strongest(
          claim.evidence
            .flatMap(ref => graph.byId.get(ref.id))
            .map(recordBinding(_, playedMoves))
        )
      evidenceBinding match
        case SubjectBindingClass.PrimaryPlayedCause | SubjectBindingClass.ContextPlayed =>
          evidenceBinding
        case SubjectBindingClass.DirectPlayed =>
          SubjectBindingClass.DirectPlayed
        case SubjectBindingClass.Other =>
          if directPlayedClaim(claim, playedMoves) && hasDirectPlayedEvidence(claim, graph, playedMoves) then
            SubjectBindingClass.DirectPlayed
          else SubjectBindingClass.Other

  def recordBinding(record: EvidenceRecord, playedMoves: Set[String]): SubjectBindingClass =
    record.payload match
      case RelativeAssessmentEvidence(assessment) if playedMoves.contains(normalizeMove(assessment.played.moveUci)) =>
        SubjectBindingClass.PrimaryPlayedCause
      case CandidateComparisonEvidence(fact) =>
        comparisonBinding(fact, playedMoves)
      case RelativeCauseFactEvidence(cause) =>
        relativeCauseBinding(cause, playedMoves)
      case MoveVerdictCertificationEvidence(certification) =>
        strongest(certification.causes.map(relativeCauseBinding(_, playedMoves)))
      case _ =>
        SubjectBindingClass.Other

  def comparisonBinding(
      fact: CandidateComparisonFact,
      playedMoves: Set[String]
  ): SubjectBindingClass =
    val candidateIsPlayed = playedMoves.contains(normalizeMove(fact.candidateLine.rootMove))
    val referenceIsPlayed = playedMoves.contains(normalizeMove(fact.referenceLine.rootMove))
    fact.kind match
      case CandidateComparisonKind.PlayedVsBest if candidateIsPlayed =>
        SubjectBindingClass.PrimaryPlayedCause
      case CandidateComparisonKind.PlayedVsAlternative if candidateIsPlayed =>
        SubjectBindingClass.ContextPlayed
      case CandidateComparisonKind.BestVsSecond if referenceIsPlayed || candidateIsPlayed =>
        SubjectBindingClass.ContextPlayed
      case CandidateComparisonKind.ReferenceVsAlternative if referenceIsPlayed || candidateIsPlayed =>
        SubjectBindingClass.ContextPlayed
      case _ =>
        SubjectBindingClass.Other

  def relativeCauseBinding(
      cause: RelativeCauseFact,
      playedMoves: Set[String]
  ): SubjectBindingClass =
    val candidateIsPlayed = playedMoves.contains(normalizeMove(cause.candidateLine.rootMove))
    val referenceIsPlayed = playedMoves.contains(normalizeMove(cause.referenceLine.rootMove))
    cause.role match
      case RelativeCauseRole.PrimaryPlayedCause
          if candidateIsPlayed && cause.importance == RelativeCauseImportance.Primary =>
        SubjectBindingClass.PrimaryPlayedCause
      case RelativeCauseRole.PlayedAlternativeContext if candidateIsPlayed =>
        SubjectBindingClass.ContextPlayed
      case RelativeCauseRole.CandidateSetConstraint | RelativeCauseRole.AlternativeDiagnostic
          if referenceIsPlayed || candidateIsPlayed =>
        SubjectBindingClass.ContextPlayed
      case _ =>
        SubjectBindingClass.Other

  def packetPlayedMoves(packet: EvidenceBackedJudgmentPacket): Set[String] =
    (
      packet.playedTransition.map(_.moveUci).toList ++
        packet.relativeAssessments.map(_.played.moveUci) ++
        packet.candidateLines.filter(_.role == LineNodeRole.Played).map(_.ref.rootMove)
    ).map(normalizeMove).filter(_.nonEmpty).toSet

  def directPlayedClaim(claim: ClaimSeed, playedMoves: Set[String]): Boolean =
    directPlayedSubject(claim.subjectMove, claim.primaryLine, playedMoves)

  def directPlayedSubject(
      subjectMove: Option[String],
      primaryLine: Option[LineNodeRef],
      playedMoves: Set[String]
  ): Boolean =
    playedSubjectBinding(subjectMove, primaryLine, playedMoves) != PlayedSubjectBinding.Unbound

  def playedSubjectBinding(
      subjectMove: Option[String],
      primaryLine: Option[LineNodeRef],
      playedMoves: Set[String]
  ): PlayedSubjectBinding =
    if playedMoves.isEmpty then PlayedSubjectBinding.Unbound
    else
      val subjectMoveBound = subjectMove.exists(move => playedMoves.contains(normalizeMove(move)))
      val playedLineBound =
        primaryLine.exists(line =>
          line.role == LineNodeRole.Played && playedMoves.contains(normalizeMove(line.rootMove))
        )
      (subjectMoveBound, playedLineBound) match
        case (true, true)   => PlayedSubjectBinding.SubjectMoveAndPlayedLine
        case (true, false)  => PlayedSubjectBinding.SubjectMove
        case (false, true)  => PlayedSubjectBinding.PlayedLine
        case (false, false) => PlayedSubjectBinding.Unbound

  def hasDirectPlayedEvidence(
      claim: ClaimSeed,
      graph: TypedEvidenceGraph,
      playedMoves: Set[String]
  ): Boolean =
    claim.evidence.flatMap(ref => graph.byId.get(ref.id)).exists { record =>
      val localLineBinding =
        record.ref.line.exists(line =>
          line.role == LineNodeRole.Played && playedMoves.contains(normalizeMove(line.rootMove))
        )
      val playedTransitionBinding =
        record.ref.scope == EvidenceScope.PlayedTransition &&
          claim.subjectMove.exists(move => playedMoves.contains(normalizeMove(move)) && recordMentionsMove(record, move))
      (localLineBinding || playedTransitionBinding) && directEvidencePayload(record.payload)
    }

  private def recordMentionsMove(record: EvidenceRecord, move: String): Boolean =
    record.payload match
      case payload: MoveMotifEvidence =>
        sameMove(payload.moveUci, move)
      case MoveTransitionEvidence(moveUci, _, _) =>
        sameMove(moveUci, move)
      case payload: StructuralDeltaEvidence =>
        sameMove(payload.moveUci, move)
      case payload: TacticalMechanismEvidence =>
        payload.moveUci.exists(sameMove(_, move)) ||
          payload.line.exists(line => sameMove(line.rootMove, move)) ||
          record.ref.scope == EvidenceScope.PlayedTransition
      case payload: RelationFactEvidence =>
        payload.mentionsLineMove(move) || record.ref.scope == EvidenceScope.PlayedTransition
      case _ =>
        record.ref.line.exists(line => sameMove(line.rootMove, move))

  private def sameMove(left: String, right: String): Boolean =
    normalizeMove(left) == normalizeMove(right)

  private def directEvidencePayload(payload: EvidencePayload): Boolean =
    payload match
      case CandidateComparisonEvidence(_) | CounterfactualFactEvidence(_, _, _) | RelativeAssessmentEvidence(_) |
          RelativeCauseFactEvidence(_) | MoveVerdictCertificationEvidence(_) =>
        false
      case _ =>
        true

  def strongest(bindings: Iterable[SubjectBindingClass]): SubjectBindingClass =
    bindings.foldLeft(SubjectBindingClass.Other) { (best, next) =>
      if bindingScore(next) > bindingScore(best) then next else best
    }

  def bindingScore(binding: SubjectBindingClass): Int =
    binding match
      case SubjectBindingClass.DirectPlayed       => 3
      case SubjectBindingClass.PrimaryPlayedCause => 2
      case SubjectBindingClass.ContextPlayed      => 1
      case SubjectBindingClass.Other              => 0

  def primaryPlayed(binding: SubjectBindingClass): Boolean =
    binding == SubjectBindingClass.DirectPlayed || binding == SubjectBindingClass.PrimaryPlayedCause

  def playedRelated(binding: SubjectBindingClass): Boolean =
    binding != SubjectBindingClass.Other

  def normalizeMove(raw: String): String =
    Option(raw).getOrElse("").trim.toLowerCase

enum PlayerFacingClaimTier:
  case Primary
  case Secondary
  case Context
  case Diagnostic

object PlayerFacingClaimPolicy:

  def tier(packet: EvidenceBackedJudgmentPacket, claim: ClaimSeed): PlayerFacingClaimTier =
    tier(claim, packet.evidenceGraph, JudgmentSubjectBinding.packetPlayedMoves(packet))

  def tier(
      claim: ClaimSeed,
      graph: TypedEvidenceGraph,
      playedMoves: Set[String]
  ): PlayerFacingClaimTier =
    if playedMoves.isEmpty then PlayerFacingClaimTier.Diagnostic
    else
      val evidenceRecords =
        claim.evidence
          .flatMap(ref => graph.byId.get(ref.id))
      if evidenceRecords.exists(record => primaryEvidenceRecord(claim, record, playedMoves)) then
        PlayerFacingClaimTier.Primary
      else if JudgmentSubjectBinding.hasDirectPlayedEvidence(claim, graph, playedMoves) then
        PlayerFacingClaimTier.Secondary
      else if evidenceRecords.exists(record => contextEvidenceRecord(record, playedMoves)) then
        PlayerFacingClaimTier.Context
      else
        PlayerFacingClaimTier.Diagnostic

  private def primaryEvidenceRecord(
      claim: ClaimSeed,
      record: EvidenceRecord,
      playedMoves: Set[String]
  ): Boolean =
    record.payload match
      case CandidateComparisonEvidence(fact) =>
        JudgmentSubjectBinding.comparisonBinding(fact, playedMoves) == SubjectBindingClass.PrimaryPlayedCause
      case RelativeCauseFactEvidence(cause) =>
        JudgmentSubjectBinding.relativeCauseBinding(cause, playedMoves) == SubjectBindingClass.PrimaryPlayedCause
      case RelativeAssessmentEvidence(assessment) =>
        claim.family == ClaimFamily.Evaluation &&
          playedMoves.contains(JudgmentSubjectBinding.normalizeMove(assessment.played.moveUci))
      case MoveVerdictCertificationEvidence(certification) =>
        if claim.family == ClaimFamily.Evaluation then
          playedMoves.contains(JudgmentSubjectBinding.normalizeMove(certification.playedMove))
        else
          certification.causes.exists(cause =>
            JudgmentSubjectBinding.relativeCauseBinding(cause, playedMoves) == SubjectBindingClass.PrimaryPlayedCause
          )
      case _ =>
        false

  private def contextEvidenceRecord(
      record: EvidenceRecord,
      playedMoves: Set[String]
  ): Boolean =
    record.payload match
      case CandidateComparisonEvidence(fact) =>
        JudgmentSubjectBinding.comparisonBinding(fact, playedMoves) == SubjectBindingClass.ContextPlayed
      case RelativeCauseFactEvidence(cause) =>
        JudgmentSubjectBinding.relativeCauseBinding(cause, playedMoves) == SubjectBindingClass.ContextPlayed
      case MoveVerdictCertificationEvidence(certification) =>
        certification.causes.exists(cause =>
          JudgmentSubjectBinding.relativeCauseBinding(cause, playedMoves) == SubjectBindingClass.ContextPlayed
        )
      case _ =>
        false

  def rankPriority(tier: PlayerFacingClaimTier): Int =
    tier match
      case PlayerFacingClaimTier.Primary    => 3
      case PlayerFacingClaimTier.Secondary  => 2
      case PlayerFacingClaimTier.Context    => 1
      case PlayerFacingClaimTier.Diagnostic => 0

case class ClaimInteraction(
    kind: ClaimInteractionKind,
    relatedClaimId: String,
    strength: Int,
    interactionEvidence: List[EvidenceRef],
    basis: List[ClaimInteractionBasis] = Nil
)

case class ClaimInteractionBasis(
    causeKind: RelativeCauseKind,
    comparisonKind: CandidateComparisonKind,
    causeRole: RelativeCauseRole,
    causeSourceSide: RelativeCauseSourceSide,
    causeImportance: RelativeCauseImportance,
    referenceLine: LineNodeRef,
    candidateLine: LineNodeRef,
    eventLine: LineNodeRef,
    proofDirectSourceIds: List[String],
    proofContrastSourceIds: List[String],
    proofContextSupportSourceIds: List[String]
)

case class ClaimSalience(
    score: Int,
    drivers: List[ClaimSalienceDriver],
    interactions: List[ClaimInteraction] = Nil
)

enum ClaimSupportStatus:
  case Certified
  case Deferred

case class ClaimSupportCheck(
    status: ClaimSupportStatus,
    presentLayers: Set[EvidenceLayer],
    missingLayerGroups: List[Set[EvidenceLayer]],
    missingEvidence: List[EvidenceRef]
)

enum ClaimSupportClusterKind:
  case LongTermSupport

case class ClaimSupportClusterInteraction(
    kind: ClaimInteractionKind,
    sourceClaimId: String,
    targetClaimId: String,
    strength: Int,
    evidence: List[EvidenceRef],
    basis: List[ClaimInteractionBasis]
)

case class ClaimSupportCluster(
    id: String,
    kind: ClaimSupportClusterKind,
    families: List[ClaimFamily],
    subject: IdeaSubject,
    primaryPosition: PositionNodeRef,
    primaryLine: Option[LineNodeRef],
    subjectMove: Option[String],
    scope: Option[EvidenceScope],
    anchorClaimIds: List[String],
    supportingClaimIds: List[String],
    constrainingClaimIds: List[String],
    ideas: List[ChessIdeaRef],
    evidence: List[EvidenceRef],
    presentLayers: Set[EvidenceLayer],
    confidence: EvidenceConfidence,
    salienceDrivers: List[ClaimSalienceDriver],
    interactions: List[ClaimSupportClusterInteraction]
)

object ClaimSupportCluster:

  def fromClaims(claims: List[ClaimSeed]): List[ClaimSupportCluster] =
    fromClaims(claims, TypedEvidenceGraph(Nil))

  def fromClaims(claims: List[ClaimSeed], graph: TypedEvidenceGraph): List[ClaimSupportCluster] =
    val anchors =
      claims
      .filter(claim => longTermSupportEligible(claim, graph))
      .groupBy(claim => clusterKey(claim, graph))
      .toList
    anchors
      .flatMap((key, groupedClaims) => clusterFor(key, groupedClaims, claims))
      .sortBy(cluster => (cluster.primaryPosition.ply, cluster.subject.toString, cluster.id))

  private final case class ClusterKey(
      subject: IdeaSubject,
      primaryPosition: PositionNodeRef,
      primaryLine: Option[LineNodeRef],
      subjectMove: Option[String],
      scope: Option[EvidenceScope],
      semanticAnchors: List[EvidenceSemanticAnchor]
  )

  private val causeBoundLayers: Set[EvidenceLayer] =
    Set(
      EvidenceLayer.Relation,
      EvidenceLayer.MoveMotif,
      EvidenceLayer.TacticalMechanism,
      EvidenceLayer.RelativeCause,
      EvidenceLayer.RelativeAssessment,
      EvidenceLayer.CandidateComparison,
      EvidenceLayer.Counterfactual,
      EvidenceLayer.MoveVerdictCertification
    )

  private val longTermExcludedLayers: Set[EvidenceLayer] =
    causeBoundLayers ++ Set(
      EvidenceLayer.Line,
      EvidenceLayer.Eval,
      EvidenceLayer.ThreatPressure
    )

  private[chessjudgment] def causeBoundLayer(layer: EvidenceLayer): Boolean =
    causeBoundLayers.contains(layer)

  private[chessjudgment] def longTermSupportExcludedLayer(layer: EvidenceLayer): Boolean =
    longTermExcludedLayers.contains(layer)

  private def longTermSupportEligible(claim: ClaimSeed, graph: TypedEvidenceGraph): Boolean =
    claim.family.isLongTerm &&
      claim.engineComparison.isEmpty &&
      claim.evidence.nonEmpty &&
      !claim.evidence.exists(ref => longTermSupportExcludedLayer(ref.layer)) &&
      semanticAnchors(claim, graph).nonEmpty

  private def clusterKey(claim: ClaimSeed, graph: TypedEvidenceGraph): ClusterKey =
    ClusterKey(
      subject = claim.subject,
      primaryPosition = claim.primaryPosition,
      primaryLine = claim.primaryLine,
      subjectMove = claim.subjectMove,
      scope = Option.when(claim.primaryLine.nonEmpty || claim.subjectMove.nonEmpty)(claim.scope),
      semanticAnchors = semanticAnchors(claim, graph)
    )

  def semanticAnchors(claim: ClaimSeed, graph: TypedEvidenceGraph): List[EvidenceSemanticAnchor] =
      claim.evidence
        .flatMap(ref => graph.byId.get(ref.id))
        .flatMap(semanticAnchorsForRecord)
        .distinctBy(_.stableKey)
        .sortBy(_.stableKey)

  private def semanticAnchorsForRecord(record: EvidenceRecord): List[EvidenceSemanticAnchor] =
    import EvidenceSemanticAnchorKind.*
    record.payload match
      case payload @ StrategicFactEvidence(kind, _, relatedPlans, confidence) if confidence >= 0.35 && payload.hasTypedSupport =>
        EvidenceSemanticAnchor.of(StrategicKind, kind.toString) ::
          relatedPlans.map(plan => EvidenceSemanticAnchor.of(Plan, plan.toString)) ++
          payload.semanticGroupingAnchors
      case _: StrategicFactEvidence =>
        Nil
      case PawnStructureFactEvidence(profile, alignment, pawnPlay) =>
        List(
          Option.when(profile.primary != StructureId.Unknown)(
            EvidenceSemanticAnchor.of(PawnStructure, profile.primary.toString)
          ),
          alignment.map(alignment =>
            EvidenceSemanticAnchor.of(
              StructurePlan,
              alignment.band.toString,
              alignment.matchedPlanIds.sorted.mkString(",")
            )
          ),
          pawnPlay.map(play => EvidenceSemanticAnchor.of(PawnPlay, play.primaryDriver.toString))
        ).flatten
      case FeatureAnchorEvidence(anchor) =>
        List(EvidenceSemanticAnchor.of(OpeningAnchor, anchor.theme.toString, anchor.signal.toString))
      case ApplicabilityAssessmentEvidence(assessment) =>
        assessment.supportedThemes.map(theme => EvidenceSemanticAnchor.of(OpeningSupported, theme.toString)) ++
          assessment.observedThemes.map(theme => EvidenceSemanticAnchor.of(OpeningObserved, theme.toString))
      case OpeningContextEvidence(_, _, _, _) =>
        Nil
      case CandidateComparisonEvidence(fact) =>
        List(
          Option.when(fact.kind == CandidateComparisonKind.BestVsSecond)(
            EvidenceSemanticAnchor.of(CandidateComparison, "best-vs-second")
          ),
          Option.when(fact.comparison.candidateSet.exists(_.onlyMove))(
            EvidenceSemanticAnchor.of(CandidateComparison, "only-move")
          ),
          fact.comparison.candidateSet
            .flatMap(_.bestToSecondWinPercentGapForMover)
            .map(gap => EvidenceSemanticAnchor.of(CandidateComparison, "best-second-wp", winPercentGapAnchor(gap)))
        ).flatten
      case PlanPressureEvidence(_, activePlans) =>
        (activePlans.primary :: activePlans.secondary.toList).map(plan =>
          EvidenceSemanticAnchor.of(PlanPressure, plan.plan.id.toString)
        )
      case PlanTransitionEvidence(transition) =>
        transition.primaryPlanId.map(plan => EvidenceSemanticAnchor.of(PlanTransition, plan)).toList
      case payload: LineFactEvidence =>
        payload.semanticGroupingAnchors
      case payload: BoardFactEvidence =>
        payload.semanticGroupingAnchors
      case payload: StructuralDeltaEvidence =>
        (
          payload.signalAnchors.map(anchor => EvidenceSemanticAnchor.of(StructuralDelta, s"signal:$anchor")) ++
            payload.consequenceAnchors.map(anchor => EvidenceSemanticAnchor.of(StructuralDelta, s"consequence:$anchor"))
        ).distinct
      case _ =>
        Nil

  private def winPercentGapAnchor(gap: Double): String =
    if gap >= 20.0 then "20+"
    else if gap >= 10.0 then "10+"
    else if gap >= 5.0 then "5+"
    else "small"

  private def clusterFor(
      key: ClusterKey,
      claims: List[ClaimSeed],
      allClaims: List[ClaimSeed]
  ): Option[ClaimSupportCluster] =
    val members = claims.sortBy(_.id)
    val anchorIds = members.map(_.id).toSet
    val evidence = members.flatMap(_.evidence).distinctBy(_.id)
    val interactions = clusterInteractions(anchorIds, allClaims)
    Option.when(members.size >= 2 || interactions.nonEmpty) {
      val supportingClaimIds = relatedClusterClaimIds(interactions, supportingInteraction, anchorIds)
      val constrainingClaimIds = relatedClusterClaimIds(interactions, constrainingInteraction, anchorIds)
      ClaimSupportCluster(
        id = s"claim-support:long-term:${members.head.id}",
        kind = ClaimSupportClusterKind.LongTermSupport,
        families = members.map(_.family).distinct.sortBy(_.toString),
        subject = key.subject,
        primaryPosition = key.primaryPosition,
        primaryLine = key.primaryLine,
        subjectMove = key.subjectMove,
        scope = key.scope,
        anchorClaimIds = members.map(_.id),
        supportingClaimIds = supportingClaimIds,
        constrainingClaimIds = constrainingClaimIds,
        ideas = members.flatMap(_.ideaRefs).distinctBy(_.id),
        evidence = evidence,
        presentLayers = evidence.map(_.layer).toSet,
        confidence = members.map(_.confidence).maxBy(confidenceScore),
        salienceDrivers = members.flatMap(_.salience.toList.flatMap(_.drivers)).distinct,
        interactions = interactions
      )
    }

  private def clusterInteractions(
      anchorIds: Set[String],
      claims: List[ClaimSeed]
  ): List[ClaimSupportClusterInteraction] =
    claims.flatMap { source =>
      source.salience.toList.flatMap(_.interactions).flatMap { interaction =>
        val sourceIsAnchor = anchorIds.contains(source.id)
        val targetIsAnchor = anchorIds.contains(interaction.relatedClaimId)
        Option.when(sourceIsAnchor != targetIsAnchor)(
          ClaimSupportClusterInteraction(
            kind = interaction.kind,
            sourceClaimId = source.id,
            targetClaimId = interaction.relatedClaimId,
            strength = interaction.strength,
            evidence = interaction.interactionEvidence,
            basis = interaction.basis
          )
        )
      }
    }.distinctBy(interaction => (interaction.kind, interaction.sourceClaimId, interaction.targetClaimId))

  private def relatedClusterClaimIds(
      interactions: List[ClaimSupportClusterInteraction],
      predicate: ClaimInteractionKind => Boolean,
      anchorIds: Set[String]
  ): List[String] =
    interactions
      .filter(interaction => predicate(interaction.kind))
      .flatMap(interaction => List(interaction.sourceClaimId, interaction.targetClaimId))
      .filterNot(anchorIds.contains)
      .distinct
      .sorted

  private def supportingInteraction(kind: ClaimInteractionKind): Boolean =
    kind match
      case ClaimInteractionKind.StrategicCompensationSupportsSacrifice | ClaimInteractionKind.ConversionSecuresAdvantage =>
        true
      case _ =>
        false

  private def constrainingInteraction(kind: ClaimInteractionKind): Boolean =
    kind match
      case ClaimInteractionKind.TacticalConstrainsLongTerm | ClaimInteractionKind.LongTermConstrainedByTactic |
          ClaimInteractionKind.TacticalRefutesStrategicPlan | ClaimInteractionKind.DefensiveNecessityOverridesPlan |
          ClaimInteractionKind.BadVerdictPreservesLocalIdea =>
        true
      case _ =>
        false

  private def confidenceScore(confidence: EvidenceConfidence): Int =
    confidence match
      case EvidenceConfidence.LegalReplayVerified => 5
      case EvidenceConfidence.EngineBacked        => 4
      case EvidenceConfidence.BoardDerived        => 3
      case EvidenceConfidence.Mixed               => 2
      case EvidenceConfidence.Heuristic           => 1

enum ClaimEventClusterKind:
  case TacticalEvent
  case DefensiveEvent
  case ConversionEvent
  case MaterialEvent

case class ClaimEventClusterInteraction(
    kind: ClaimInteractionKind,
    sourceClaimId: String,
    targetClaimId: String,
    strength: Int,
    evidence: List[EvidenceRef],
    basis: List[ClaimInteractionBasis]
)

enum ClaimEventMemberRole:
  case CauseOwner
  case VerdictCarrier
  case Witness

case class ClaimEventCauseProof(
    claimId: String,
    family: ClaimFamily,
    memberRole: ClaimEventMemberRole,
    causeKind: RelativeCauseKind,
    comparisonKind: CandidateComparisonKind,
    causeRole: RelativeCauseRole,
    causeSourceSide: RelativeCauseSourceSide,
    causeImportance: RelativeCauseImportance,
    referenceLine: LineNodeRef,
    candidateLine: LineNodeRef,
    eventLine: LineNodeRef,
    proofDirectSourceIds: List[String],
    proofContrastSourceIds: List[String],
    proofContextSupportSourceIds: List[String],
    proofDirectKinds: List[String],
    proofContrastKinds: List[String],
    proofContextSupportKinds: List[String]
)

case class ClaimEventCluster(
    id: String,
    kind: ClaimEventClusterKind,
    causeKind: RelativeCauseKind,
    comparisonKind: CandidateComparisonKind,
    causeRole: RelativeCauseRole,
    causeSourceSide: RelativeCauseSourceSide,
    causeImportance: RelativeCauseImportance,
    referenceLine: LineNodeRef,
    candidateLine: LineNodeRef,
    eventLine: LineNodeRef,
    eventRootMove: String,
    verdict: MoveChoiceVerdict,
    winPercentLossForMover: Double,
    candidateWinPercentDeltaForMover: Double,
    families: List[ClaimFamily],
    primaryPosition: PositionNodeRef,
    scope: EvidenceScope,
    memberClaimIds: List[String],
    causeClaimIds: List[String],
    evaluationClaimIds: List[String],
    witnessClaimIds: List[String],
    relatedSupportClusterIds: List[String],
    ideas: List[ChessIdeaRef],
    evidence: List[EvidenceRef],
    presentLayers: Set[EvidenceLayer],
    proofBoardAnchors: List[BoardAnchorKind],
    proofLineEvents: List[LineEventKind],
    proofLineConsequences: List[LineConsequenceKind],
    proofRelationKinds: List[RelationFactKind],
    proofRelationDetails: List[String],
    proofTacticalMechanisms: List[TacticalMechanismProof],
    proofTransitionConsequences: List[TransitionConsequenceProof],
    proofDirectSourceIds: List[String],
    proofContrastSourceIds: List[String],
    proofContextSupportSourceIds: List[String],
    proofDirectKinds: List[String],
    proofContrastKinds: List[String],
    proofContextSupportKinds: List[String],
    causeProofs: List[ClaimEventCauseProof],
    causeContextLayers: Set[EvidenceLayer],
    confidence: EvidenceConfidence,
    salienceDrivers: List[ClaimSalienceDriver],
    interactions: List[ClaimEventClusterInteraction]
)

object ClaimEventCluster:

  def fromClaims(
      claims: List[ClaimSeed],
      graph: TypedEvidenceGraph,
      supportClusters: List[ClaimSupportCluster] = Nil
  ): List[ClaimEventCluster] =
    val keyedBindings =
      claims
        .filter(eventClaimEligible)
        .flatMap(claim => eventBindingsForClaim(claim, graph))
    keyedBindings
      .groupBy(_.key)
      .toList
      .flatMap { case (key, bindings) =>
        clusterFor(
          key = key,
          bindings = bindings,
          allClaims = claims,
          supportClusters = supportClusters
        )
      }
      .sortBy(cluster =>
        (
          cluster.primaryPosition.ply,
          cluster.comparisonKind.toString,
          cluster.causeRole.toString,
          cluster.causeSourceSide.toString,
          cluster.causeImportance.toString,
          cluster.causeKind.toString,
          cluster.eventRootMove,
          cluster.id
        )
      )

  private final case class EventClusterKey(
      primaryPosition: PositionNodeRef,
      scope: EvidenceScope,
      causeKind: RelativeCauseKind,
      comparisonKind: CandidateComparisonKind,
      causeRole: RelativeCauseRole,
      causeSourceSide: RelativeCauseSourceSide,
      causeImportance: RelativeCauseImportance,
      referenceLine: LineNodeRef,
      candidateLine: LineNodeRef,
      eventLine: LineNodeRef,
      verdict: MoveChoiceVerdict,
      winPercentLossForMover: Double,
      candidateWinPercentDeltaForMover: Double
  ):
    def eventRootMove: String = eventLine.rootMove

  private final case class EventMemberBinding(
      key: EventClusterKey,
      claim: ClaimSeed,
      role: ClaimEventMemberRole,
      proof: Option[RelativeCauseProof] = None
  )

  private val eventEvidenceLayers: Set[EvidenceLayer] =
    Set(
      EvidenceLayer.SinglePosition,
      EvidenceLayer.Line,
      EvidenceLayer.Eval,
      EvidenceLayer.ThreatPressure,
      EvidenceLayer.MoveMotif,
      EvidenceLayer.TacticalMechanism,
      EvidenceLayer.MoveTransition,
      EvidenceLayer.Relation,
      EvidenceLayer.RelativeAssessment,
      EvidenceLayer.CandidateComparison,
      EvidenceLayer.Counterfactual,
      EvidenceLayer.RelativeCause,
      EvidenceLayer.MoveVerdictCertification
    )

  private def eventClaimEligible(claim: ClaimSeed): Boolean =
    claim.family.isEvent && claim.evidence.nonEmpty

  private val eventInteractionEvidenceLayers: Set[EvidenceLayer] =
    Set(
      EvidenceLayer.Line,
      EvidenceLayer.Eval,
      EvidenceLayer.ThreatPressure,
      EvidenceLayer.MoveMotif,
      EvidenceLayer.TacticalMechanism,
      EvidenceLayer.MoveTransition,
      EvidenceLayer.Relation,
      EvidenceLayer.RelativeAssessment,
      EvidenceLayer.CandidateComparison,
      EvidenceLayer.Counterfactual,
      EvidenceLayer.RelativeCause,
      EvidenceLayer.MoveVerdictCertification
    )

  private def eventBindingsForClaim(
      claim: ClaimSeed,
      graph: TypedEvidenceGraph
  ): List[EventMemberBinding] =
    val records = recordsFor(claim.evidence, graph)
    val direct = records.flatMap(eventBindingsForRecord(claim, _)).distinct
    if direct.nonEmpty then direct
    else if indirectWitnessEligible(claim) then
      val evidenceIds = claim.evidence.map(_.id).toSet
      val parentRecords = recordsFor(records.flatMap(_.parents), graph)
      val childRecords =
        graph.records.filter(record => record.parents.exists(parent => evidenceIds.contains(parent.id)))
      val linked = (parentRecords ++ childRecords).flatMap(eventKeysForRecord).distinct
      lineAwareWitnessKeys(claim, linked).map(EventMemberBinding(_, claim, ClaimEventMemberRole.Witness))
    else Nil

  private def recordsFor(refs: List[EvidenceRef], graph: TypedEvidenceGraph): List[EvidenceRecord] =
    refs.flatMap(ref => graph.byId.get(ref.id))

  private def eventBindingsForRecord(claim: ClaimSeed, record: EvidenceRecord): List[EventMemberBinding] =
    record.payload match
      case RelativeCauseFactEvidence(cause) =>
        eventKey(record.ref, cause).toList.map { key =>
          val role =
            if claim.family == ClaimFamily.Evaluation then ClaimEventMemberRole.Witness
            else ClaimEventMemberRole.CauseOwner
          EventMemberBinding(key, claim, role, cause.proof)
        }
      case MoveVerdictCertificationEvidence(certification) =>
        certification.causes.flatMap { cause =>
          eventKey(record.ref, cause).map { key =>
            val role =
              if claim.family == ClaimFamily.Evaluation then ClaimEventMemberRole.VerdictCarrier
              else ClaimEventMemberRole.CauseOwner
            EventMemberBinding(key, claim, role, cause.proof)
          }
        }
      case _ =>
        Nil

  private def eventKeysForRecord(record: EvidenceRecord): List[EventClusterKey] =
    record.payload match
      case RelativeCauseFactEvidence(cause) =>
        eventKey(record.ref, cause).toList
      case MoveVerdictCertificationEvidence(certification) =>
        certification.causes.flatMap(cause => eventKey(record.ref, cause))
      case _ =>
        Nil

  private def eventKey(ref: EvidenceRef, cause: RelativeCauseFact): Option[EventClusterKey] =
    Option.when(kindForCause(cause.kind).nonEmpty)(
      EventClusterKey(
        primaryPosition = ref.position,
        scope = ref.scope,
        causeKind = cause.kind,
        comparisonKind = cause.comparisonKind,
        causeRole = cause.role,
        causeSourceSide = cause.sourceSide,
        causeImportance = cause.importance,
        referenceLine = cause.referenceLine,
        candidateLine = cause.candidateLine,
        eventLine = cause.eventLine,
        verdict = cause.verdict,
        winPercentLossForMover = cause.winPercentLossForMover,
        candidateWinPercentDeltaForMover = cause.candidateWinPercentDeltaForMover
      )
    )

  private def indirectWitnessEligible(claim: ClaimSeed): Boolean =
    claim.family match
      case ClaimFamily.Tactical | ClaimFamily.Conversion =>
        claim.evidence.exists(ref =>
          ref.layer == EvidenceLayer.TacticalMechanism ||
            ref.layer == EvidenceLayer.Relation ||
            ref.layer == EvidenceLayer.MoveMotif
        )
      case ClaimFamily.Material =>
        claim.evidence.exists(ref => ref.layer == EvidenceLayer.Line)
      case _ =>
        false

  private def lineAwareWitnessKeys(
      claim: ClaimSeed,
      keys: List[EventClusterKey]
  ): List[EventClusterKey] =
    val unique = keys.distinct
    val claimLines = (claim.primaryLine.toList ++ claim.evidence.flatMap(_.line)).distinct
    val claimMoves = (claim.subjectMove.toList ++ claimLines.map(_.rootMove)).map(normalizeMove).toSet
    val eventLineMatched =
      unique.filter(key => claimLines.contains(key.eventLine))
    val comparisonLineMatched =
      unique.filter(key => claimLines.exists(line => line == key.referenceLine || line == key.candidateLine))
    val eventMoveMatched =
      unique.filter(key => claimMoves.contains(normalizeMove(key.eventRootMove)))
    val comparisonMoveMatched =
      unique.filter(key =>
        claimMoves.contains(normalizeMove(key.referenceLine.rootMove)) ||
          claimMoves.contains(normalizeMove(key.candidateLine.rootMove))
      )
    List(eventLineMatched, comparisonLineMatched, eventMoveMatched, comparisonMoveMatched)
      .find(_.nonEmpty)
      .getOrElse(if unique.size == 1 then unique else Nil)
      .distinct

  private def normalizeMove(raw: String): String =
    Option(raw).getOrElse("").trim.toLowerCase

  private def clusterFor(
      key: EventClusterKey,
      bindings: List[EventMemberBinding],
      allClaims: List[ClaimSeed],
      supportClusters: List[ClaimSupportCluster]
  ): Option[ClaimEventCluster] =
    val members = bindings.map(_.claim).distinctBy(_.id).sortBy(_.id)
    val memberIds = members.map(_.id).toSet
    val causeClaimIds =
      bindings
        .filter(_.role == ClaimEventMemberRole.CauseOwner)
        .map(_.claim)
        .filterNot(_.family == ClaimFamily.Evaluation)
        .distinctBy(_.id)
        .sortBy(_.id)
        .map(_.id)
    val evaluationClaimIds =
      bindings
        .filter(_.role == ClaimEventMemberRole.VerdictCarrier)
        .map(_.claim)
        .filter(_.family == ClaimFamily.Evaluation)
        .distinctBy(_.id)
        .sortBy(_.id)
        .map(_.id)
    val witnessClaimIds =
      bindings
        .filter(_.role == ClaimEventMemberRole.Witness)
        .map(_.claim)
        .filterNot(claim => causeClaimIds.contains(claim.id) || evaluationClaimIds.contains(claim.id))
        .distinctBy(_.id)
        .sortBy(_.id)
        .map(_.id)
    val evidence =
      members.flatMap(_.evidence).filter(ref => eventEvidenceLayers.contains(ref.layer)).distinctBy(_.id)
    val proof =
      mergedProof(bindings.filter(_.role == ClaimEventMemberRole.CauseOwner).flatMap(_.proof))
    val claimProof = proof.depthProof
    val causeProofs = bindings.flatMap(causeProofForBinding).distinctBy(proof =>
      (
        proof.claimId,
        proof.memberRole.toString,
        proof.causeKind.toString,
        proof.comparisonKind.toString,
        proof.causeRole.toString,
        proof.causeSourceSide.toString,
        proof.causeImportance.toString,
        proof.eventLine.id,
        proof.proofDirectSourceIds.mkString(","),
        proof.proofContrastSourceIds.mkString(","),
        proof.proofContextSupportSourceIds.mkString(",")
      )
    )
    val interactions = clusterInteractions(memberIds, allClaims)
    val relatedSupportClusterIds = supportClustersRelatedTo(memberIds, interactions, supportClusters)
    Option.when(causeClaimIds.nonEmpty && evidence.nonEmpty) {
      ClaimEventCluster(
        id = eventClusterId(key, members),
        kind = clusterKind(key.causeKind),
        causeKind = key.causeKind,
        comparisonKind = key.comparisonKind,
        causeRole = key.causeRole,
        causeSourceSide = key.causeSourceSide,
        causeImportance = key.causeImportance,
        referenceLine = key.referenceLine,
        candidateLine = key.candidateLine,
        eventLine = key.eventLine,
        eventRootMove = key.eventRootMove,
        verdict = key.verdict,
        winPercentLossForMover = key.winPercentLossForMover,
        candidateWinPercentDeltaForMover = key.candidateWinPercentDeltaForMover,
        families = members.map(_.family).distinct.sortBy(_.toString),
        primaryPosition = key.primaryPosition,
        scope = key.scope,
        memberClaimIds = members.map(_.id),
        causeClaimIds = causeClaimIds,
        evaluationClaimIds = evaluationClaimIds,
        witnessClaimIds = witnessClaimIds,
        relatedSupportClusterIds = relatedSupportClusterIds,
        ideas = members.flatMap(_.ideaRefs).distinctBy(_.id),
        evidence = evidence,
        presentLayers = evidence.map(_.layer).toSet,
        proofBoardAnchors = claimProof.boardAnchors,
        proofLineEvents = claimProof.lineEvents,
        proofLineConsequences = claimProof.lineConsequences,
        proofRelationKinds = claimProof.relationKinds,
        proofRelationDetails = claimProof.relationDetails,
        proofTacticalMechanisms = claimProof.tacticalMechanisms,
        proofTransitionConsequences = claimProof.transitionConsequences,
        proofDirectSourceIds = proof.directProof.sourceRefs.map(_.id).distinct.sorted,
        proofContrastSourceIds = proof.contrastProof.sourceRefs.map(_.id).distinct.sorted,
        proofContextSupportSourceIds = proof.contextSupport.sourceRefs.map(_.id).distinct.sorted,
        proofDirectKinds = proof.directProof.kindLabels.distinct.sorted,
        proofContrastKinds = proof.contrastProof.kindLabels.distinct.sorted,
        proofContextSupportKinds = proof.contextSupport.kindLabels.distinct.sorted,
        causeProofs = causeProofs,
        causeContextLayers = proof.contextLayers.toSet,
        confidence = members.map(_.confidence).maxBy(confidenceScore),
        salienceDrivers = members.flatMap(_.salience.toList.flatMap(_.drivers)).distinct,
        interactions = interactions
      )
    }

  private def mergedProof(proofs: List[RelativeCauseProof]): RelativeCauseProof =
    RelativeCauseProof.merge(proofs)

  private def causeProofForBinding(binding: EventMemberBinding): Option[ClaimEventCauseProof] =
    binding.proof.map { proof =>
      ClaimEventCauseProof(
        claimId = binding.claim.id,
        family = binding.claim.family,
        memberRole = binding.role,
        causeKind = binding.key.causeKind,
        comparisonKind = binding.key.comparisonKind,
        causeRole = binding.key.causeRole,
        causeSourceSide = binding.key.causeSourceSide,
        causeImportance = binding.key.causeImportance,
        referenceLine = binding.key.referenceLine,
        candidateLine = binding.key.candidateLine,
        eventLine = binding.key.eventLine,
        proofDirectSourceIds = proof.directProof.sourceRefs.map(_.id).distinct.sorted,
        proofContrastSourceIds = proof.contrastProof.sourceRefs.map(_.id).distinct.sorted,
        proofContextSupportSourceIds = proof.contextSupport.sourceRefs.map(_.id).distinct.sorted,
        proofDirectKinds = proof.directProof.kindLabels.distinct.sorted,
        proofContrastKinds = proof.contrastProof.kindLabels.distinct.sorted,
        proofContextSupportKinds = proof.contextSupport.kindLabels.distinct.sorted
      )
    }

  private def eventClusterId(key: EventClusterKey, members: List[ClaimSeed]): String =
    List(
      "claim-event",
      key.comparisonKind.toString,
      key.causeRole.toString,
      key.causeSourceSide.toString,
      key.causeImportance.toString,
      key.causeKind.toString,
      key.eventRootMove,
      members.map(_.id).minOption.getOrElse("empty")
    ).map(idPart).mkString(":")

  private def idPart(raw: String): String =
    raw.toLowerCase.replaceAll("[^a-z0-9]+", "-").stripPrefix("-").stripSuffix("-")

  private def clusterKind(causeKind: RelativeCauseKind): ClaimEventClusterKind =
    kindForCause(causeKind).getOrElse(ClaimEventClusterKind.TacticalEvent)

  def kindForCause(kind: RelativeCauseKind): Option[ClaimEventClusterKind] =
    kind match
      case RelativeCauseKind.MissedTacticalResource | RelativeCauseKind.TacticalRefutationOfPlayed |
          RelativeCauseKind.CandidateTacticalLiability |
          RelativeCauseKind.WrongRecapturer | RelativeCauseKind.RecaptureRecoveryWindow |
          RelativeCauseKind.WrongMoveOrder | RelativeCauseKind.TempoLoss | RelativeCauseKind.KingForcing =>
        Some(ClaimEventClusterKind.TacticalEvent)
      case RelativeCauseKind.OnlyMoveNecessity | RelativeCauseKind.OnlyDefenseNecessity |
          RelativeCauseKind.DefensiveResource | RelativeCauseKind.DrawResource =>
        Some(ClaimEventClusterKind.DefensiveEvent)
      case RelativeCauseKind.ConversionMiss | RelativeCauseKind.ConversionSecured =>
        Some(ClaimEventClusterKind.ConversionEvent)
      case RelativeCauseKind.MaterialSwing | RelativeCauseKind.SacrificeCompensation =>
        Some(ClaimEventClusterKind.MaterialEvent)
      case _ =>
        None

  private def clusterInteractions(
      memberIds: Set[String],
      claims: List[ClaimSeed]
  ): List[ClaimEventClusterInteraction] =
    val claimsById = claims.map(claim => claim.id -> claim).toMap
    claims.flatMap { source =>
      source.salience.toList.flatMap(_.interactions).flatMap { interaction =>
        val sourceIsMember = memberIds.contains(source.id)
        val targetIsMember = memberIds.contains(interaction.relatedClaimId)
        Option.when(sourceIsMember || targetIsMember)(
          ClaimEventClusterInteraction(
            kind = interaction.kind,
            sourceClaimId = source.id,
            targetClaimId = interaction.relatedClaimId,
            strength = interaction.strength,
            evidence =
              if crossesLongTerm(source, interaction.relatedClaimId, memberIds, claimsById) then Nil
              else interaction.interactionEvidence.filter(ref => eventInteractionEvidenceLayers.contains(ref.layer)),
            basis = interaction.basis
          )
        )
      }
    }.distinctBy(interaction => (interaction.kind, interaction.sourceClaimId, interaction.targetClaimId))

  private def crossesLongTerm(
      source: ClaimSeed,
      targetClaimId: String,
      memberIds: Set[String],
      claimsById: Map[String, ClaimSeed]
  ): Boolean =
    val sourceExternalLongTerm = !memberIds.contains(source.id) && source.family.isLongTerm
    val targetExternalLongTerm = !memberIds.contains(targetClaimId) && claimsById.get(targetClaimId).exists(_.family.isLongTerm)
    sourceExternalLongTerm || targetExternalLongTerm

  private def supportClustersRelatedTo(
      memberIds: Set[String],
      interactions: List[ClaimEventClusterInteraction],
      supportClusters: List[ClaimSupportCluster]
  ): List[String] =
    val externallyRelatedClaims =
      interactions
        .flatMap(interaction => List(interaction.sourceClaimId, interaction.targetClaimId))
        .filterNot(memberIds.contains)
        .toSet
    supportClusters
      .filter { cluster =>
        val supportIds =
          (cluster.anchorClaimIds ++ cluster.supportingClaimIds ++ cluster.constrainingClaimIds).toSet
        supportIds.exists(memberIds.contains) || supportIds.exists(externallyRelatedClaims.contains)
      }
      .map(_.id)
      .distinct
      .sorted

  private def confidenceScore(confidence: EvidenceConfidence): Int =
    confidence match
      case EvidenceConfidence.LegalReplayVerified => 5
      case EvidenceConfidence.EngineBacked        => 4
      case EvidenceConfidence.BoardDerived        => 3
      case EvidenceConfidence.Mixed               => 2
      case EvidenceConfidence.Heuristic           => 1

case class ClaimSeed(
    id: String,
    family: ClaimFamily,
    idea: Option[ChessIdeaRef],
    subject: IdeaSubject,
    primaryPosition: PositionNodeRef,
    primaryLine: Option[LineNodeRef],
    subjectMove: Option[String],
    evidence: List[EvidenceRef],
    engineComparison: Option[EvalComparison],
    scope: EvidenceScope,
    confidence: EvidenceConfidence,
    supportStatus: Option[ClaimSupportCheck] = None,
    salience: Option[ClaimSalience] = None,
    relatedIdeas: List[ChessIdeaRef] = Nil
):
  def ideaRefs: List[ChessIdeaRef] =
    (idea.toList ++ relatedIdeas).distinctBy(_.id)

case class IdeaVerdictSplit(
    ideas: List[ChessIdeaRef],
    ideaClaims: List[ClaimSeed],
    verdict: Option[EvalComparison],
    bindings: List[IdeaVerdictBinding]
)

object IdeaVerdictSplit:
  def from(
      ideas: List[ChessIdea],
      claims: List[ClaimSeed],
      assessments: List[RelativeMoveAssessment]
  ): Option[IdeaVerdictSplit] =
    val ideaRefs = ideas.map(_.ref)
    val ideaClaims = claims.filter(claim => claim.ideaRefs.exists(ideaRefs.contains))
    val assessment = assessments.headOption
    val verdict = assessment.map(_.comparison)
    val bindings =
      assessments.flatMap { relative =>
        ideas.filter(ideaBindsToRelative(_, relative)).map { idea =>
          IdeaVerdictBinding(
            idea = idea.ref,
            verdict = relative.comparison,
            relation = relationFor(idea, relative.comparison),
            evidence =
              (idea.evidence ++
                (relative.evidence ::
                  (relative.counterfactualEvidence ++
                    relative.candidateComparisonEvidence ++
                    relative.relativeCauseEvidence ++
                    relative.verdictCertificationEvidence.toList))).distinctBy(_.id)
          )
        }
      }
    Option.when(ideaRefs.nonEmpty || ideaClaims.nonEmpty || verdict.nonEmpty)(
      IdeaVerdictSplit(
        ideas = ideaRefs,
        ideaClaims = ideaClaims,
        verdict = verdict,
        bindings = bindings
      )
    )

  private def relationFor(idea: ChessIdea, comparison: EvalComparison): IdeaVerdictRelation =
    comparison.verdict match
      case MoveChoiceVerdict.ImprovesOnReference | MoveChoiceVerdict.MatchesReference | MoveChoiceVerdict.PlayableLoss =>
        if idea.ref.family == ChessIdeaFamily.Defensive then IdeaVerdictRelation.DefensiveNecessity
        else IdeaVerdictRelation.SupportsVerdict
      case MoveChoiceVerdict.Blunder
          if idea.subject == IdeaSubject.PlayedMove || idea.moveUci.nonEmpty =>
        IdeaVerdictRelation.RefutesIdea
      case MoveChoiceVerdict.Mistake
          if idea.ref.family == ChessIdeaFamily.Tactical && (idea.subject == IdeaSubject.PlayedMove || idea.moveUci.nonEmpty) =>
        IdeaVerdictRelation.RefutesIdea
      case MoveChoiceVerdict.Inaccuracy | MoveChoiceVerdict.Mistake | MoveChoiceVerdict.Blunder =>
        IdeaVerdictRelation.ExplainsIdeaDespiteBadVerdict

  private def ideaBindsToRelative(idea: ChessIdea, relative: RelativeMoveAssessment): Boolean =
    val relativeEvidence =
      (relative.evidence ::
        (relative.counterfactualEvidence ++
          relative.candidateComparisonEvidence ++
          relative.relativeCauseEvidence ++
          relative.verdictCertificationEvidence.toList)).map(_.id).toSet
    idea.ref.family == ChessIdeaFamily.Evaluation ||
      idea.primaryLine.contains(relative.candidate.ref) ||
      idea.moveUci.contains(relative.played.moveUci) ||
      idea.evidence.exists(ref =>
        relativeEvidence.contains(ref.id) ||
          ref.line.contains(relative.candidate.ref)
      )

case class EvidenceBackedJudgmentPacket(
    root: PositionNodeRef,
    positions: List[PositionNode],
    candidateLines: List[CandidateLineNode],
    transitions: List[MoveTransitionEdge],
    relativeAssessments: List[RelativeMoveAssessment],
    evidenceGraph: TypedEvidenceGraph,
    ideas: List[ChessIdea],
    claims: List[ClaimSeed],
    claimLifecycle: List[ClaimLifecycleDiagnostic] = Nil,
    ideaVerdict: Option[IdeaVerdictSplit],
    claimSupportClusters: List[ClaimSupportCluster] = Nil,
    claimEventClusters: List[ClaimEventCluster] = Nil,
    diagnostics: EvidenceLossReport = EvidenceLossReport.empty,
    probeRequests: List[ProbeRequest] = Nil,
    probeDiagnostics: List[ProbeAdmissionDiagnostic] = Nil
):
  def playedTransition: Option[MoveTransitionEdge] =
    transitions.find(_.role == TransitionEdgeRole.Played)

  def referenceTransition: Option[MoveTransitionEdge] =
    transitions.find(_.role == TransitionEdgeRole.Reference)

type LlmJudgmentPacket = EvidenceBackedJudgmentPacket
