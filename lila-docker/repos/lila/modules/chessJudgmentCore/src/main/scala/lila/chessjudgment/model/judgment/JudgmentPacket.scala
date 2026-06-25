package lila.chessjudgment.model.judgment

import java.nio.charset.StandardCharsets
import java.security.MessageDigest

import lila.chessjudgment.model.{ ProbeAdmissionDiagnostic, ProbeRequest }

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
    proofStrategicAxisKeys: List[String],
    proofStrategicMechanismKinds: List[StrategicMechanismKind],
    proofStrategicMechanismSourceIds: List[String],
    proofStrategicMechanismSignalSourceIds: List[String],
    supportEvidenceSourceIds: List[String],
    proofDirectKinds: List[String],
    proofContrastKinds: List[String],
    proofContextSupportKinds: List[String],
    attributionKind: CauseAttributionKind,
    attributionOwnedEvidenceIds: List[String],
    attributionContrastEvidenceIds: List[String],
    attributionContextEvidenceIds: List[String],
    attributionRootMoveMatched: Boolean,
    attributionDirectProofEligible: Boolean,
    attributionReason: Option[String],
    objectBindingSignatures: List[String] = Nil
)

final case class ClaimStrategicAxisLineage(
    axisKey: String,
    axisKind: StrategicAxisKind,
    axisPolarity: StrategicAxisPolarity,
    axisLabel: String,
    mechanismEvidenceId: String,
    signalSourceEvidenceId: String,
    signalSourceLayer: EvidenceLayer,
    relativeCauseIds: List[String]
)

object ClaimStrategicAxisLineage:
  def fromClaim(claim: ClaimSeed, graph: TypedEvidenceGraph): List[ClaimStrategicAxisLineage] =
    fromRecords(claim.evidence.flatMap(ref => graph.byId.get(ref.id)))

  def fromRecords(records: List[EvidenceRecord]): List[ClaimStrategicAxisLineage] =
    val grouped =
      records
        .flatMap(axisEntries)
        .groupBy(entry => (entry.axisKey, entry.mechanismEvidenceId, entry.signalSourceEvidenceId))
    grouped.values.toList
      .map { entries =>
        val first = entries.head
        first.copy(relativeCauseIds = entries.flatMap(_.relativeCauseIds).distinct.sorted)
      }
      .sortBy(entry => (entry.axisKey, entry.mechanismEvidenceId, entry.signalSourceEvidenceId))

  private def axisEntries(record: EvidenceRecord): List[ClaimStrategicAxisLineage] =
    record.payload match
      case payload: StrategicMechanismContrastEvidence =>
        contrastEntries(record.ref.id, payload, None)
      case RelativeCauseFactEvidence(cause) =>
        causeEntries(record.ref.id, cause)
      case _ =>
        Nil

  private def causeEntries(causeId: String, cause: RelativeCauseFact): List[ClaimStrategicAxisLineage] =
    cause.proof.toList.flatMap(proof =>
      proof.strategicMechanisms.flatMap(mechanism => mechanismEntries(mechanism.source.id, mechanism, Some(causeId))) ++
        proof.strategicMechanismContrasts.flatMap(contrast => contrastEntries(contrast.source.id, contrast, Some(causeId)))
    )

  private def mechanismEntries(
      mechanismEvidenceId: String,
      proof: StrategicMechanismProof,
      causeId: Option[String]
  ): List[ClaimStrategicAxisLineage] =
    proof.signals.flatMap(signal => signal.axis.map(axis => lineage(axis, mechanismEvidenceId, signal, causeId)))

  private def contrastEntries(
      mechanismEvidenceId: String,
      payload: StrategicMechanismContrastEvidence,
      causeId: Option[String]
  ): List[ClaimStrategicAxisLineage] =
    payload.axisComparisons.flatMap(axisComparison =>
      axisComparison.sources.map(source => lineage(axisComparison.axis, mechanismEvidenceId, source, causeId))
    )

  private def contrastEntries(
      mechanismEvidenceId: String,
      proof: StrategicMechanismContrastProof,
      causeId: Option[String]
  ): List[ClaimStrategicAxisLineage] =
    proof.axisComparisons.flatMap(axisComparison =>
      axisComparison.sources.map(source => lineage(axisComparison.axis, mechanismEvidenceId, source, causeId))
    )

  private def lineage(
      axis: StrategicAxisDetail,
      mechanismEvidenceId: String,
      signal: StrategicMechanismSignal,
      causeId: Option[String]
  ): ClaimStrategicAxisLineage =
    ClaimStrategicAxisLineage(
      axisKey = axis.stableKey,
      axisKind = axis.kind,
      axisPolarity = axis.polarity,
      axisLabel = axis.label,
      mechanismEvidenceId = mechanismEvidenceId,
      signalSourceEvidenceId = signal.source.id,
      signalSourceLayer = signal.source.layer,
      relativeCauseIds = causeId.toList
    )

  private def lineage(
      axis: StrategicAxisDetail,
      mechanismEvidenceId: String,
      source: EvidenceRef,
      causeId: Option[String]
  ): ClaimStrategicAxisLineage =
    ClaimStrategicAxisLineage(
      axisKey = axis.stableKey,
      axisKind = axis.kind,
      axisPolarity = axis.polarity,
      axisLabel = axis.label,
      mechanismEvidenceId = mechanismEvidenceId,
      signalSourceEvidenceId = source.id,
      signalSourceLayer = source.layer,
      relativeCauseIds = causeId.toList
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
    strategicAxisLineage: List[ClaimStrategicAxisLineage],
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
        if badVerdict(assessment.comparison.verdict) then SubjectBindingClass.PrimaryPlayedCause
        else SubjectBindingClass.ContextPlayed
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
      case CandidateComparisonKind.PlayedVsBest if candidateIsPlayed && badVerdict(fact.comparison.verdict) =>
        SubjectBindingClass.PrimaryPlayedCause
      case CandidateComparisonKind.PlayedVsBest if candidateIsPlayed =>
        SubjectBindingClass.ContextPlayed
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
          if candidateIsPlayed && cause.importance == RelativeCauseImportance.Primary && badVerdict(cause.verdict) =>
        SubjectBindingClass.PrimaryPlayedCause
      case RelativeCauseRole.PrimaryPlayedCause if candidateIsPlayed =>
        SubjectBindingClass.ContextPlayed
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

  private def badVerdict(verdict: MoveChoiceVerdict): Boolean =
    verdict match
      case MoveChoiceVerdict.Inaccuracy | MoveChoiceVerdict.Mistake | MoveChoiceVerdict.Blunder => true
      case _                                                                                   => false

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
      val baseTier =
        if claim.family == ClaimFamily.Evaluation &&
          evidenceRecords.exists(record => evaluationVerdictCarrierRecord(record, playedMoves))
        then
          PlayerFacingClaimTier.Secondary
        else if evidenceRecords.exists(record => primaryEvidenceRecord(record, playedMoves)) then
          PlayerFacingClaimTier.Primary
        else if evidenceRecords.exists(record => contextEvidenceRecord(record, playedMoves)) then
          PlayerFacingClaimTier.Context
        else if JudgmentSubjectBinding.hasDirectPlayedEvidence(claim, graph, playedMoves) then
          PlayerFacingClaimTier.Secondary
        else
          PlayerFacingClaimTier.Diagnostic
      if promotedTier(baseTier) && requiresConcreteObject(claim.family) &&
        !EvidenceObjectBinding.playerFacingReady(EvidenceObjectBinding.fromClaim(claim, graph))
      then PlayerFacingClaimTier.Diagnostic
      else baseTier

  def requiresConcreteObject(family: ClaimFamily): Boolean =
    family != ClaimFamily.Evaluation

  private def promotedTier(tier: PlayerFacingClaimTier): Boolean =
    tier == PlayerFacingClaimTier.Primary || tier == PlayerFacingClaimTier.Secondary

  private def primaryEvidenceRecord(
      record: EvidenceRecord,
      playedMoves: Set[String]
  ): Boolean =
    record.payload match
      case CandidateComparisonEvidence(fact) =>
        JudgmentSubjectBinding.comparisonBinding(fact, playedMoves) == SubjectBindingClass.PrimaryPlayedCause
      case RelativeCauseFactEvidence(cause) =>
        JudgmentSubjectBinding.relativeCauseBinding(cause, playedMoves) == SubjectBindingClass.PrimaryPlayedCause
      case MoveVerdictCertificationEvidence(certification) =>
        certification.causes.exists(cause =>
          JudgmentSubjectBinding.relativeCauseBinding(cause, playedMoves) == SubjectBindingClass.PrimaryPlayedCause
        )
      case _ =>
        false

  private def evaluationVerdictCarrierRecord(
      record: EvidenceRecord,
      playedMoves: Set[String]
  ): Boolean =
    record.payload match
      case RelativeAssessmentEvidence(assessment) =>
        playedMoves.contains(JudgmentSubjectBinding.normalizeMove(assessment.played.moveUci))
      case MoveVerdictCertificationEvidence(certification) =>
        playedMoves.contains(JudgmentSubjectBinding.normalizeMove(certification.playedMove))
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
    attributionKind: CauseAttributionKind,
    attributionRootMoveMatched: Boolean,
    attributionDirectProofEligible: Boolean,
    referenceLine: LineNodeRef,
    candidateLine: LineNodeRef,
    eventLine: LineNodeRef,
    proofDirectSourceIds: List[String],
    proofContrastSourceIds: List[String],
    proofContextSupportSourceIds: List[String],
    proofStrategicAxisKeys: List[String],
    proofStrategicMechanismKinds: List[StrategicMechanismKind],
    proofStrategicMechanismSourceIds: List[String],
    proofStrategicMechanismSignalSourceIds: List[String],
    supportEvidenceSourceIds: List[String]
)

object ClaimInteractionBasis:
  def stableKey(basis: ClaimInteractionBasis): String =
    List(
      basis.causeKind.toString,
      basis.comparisonKind.toString,
      basis.causeRole.toString,
      basis.causeSourceSide.toString,
      basis.causeImportance.toString,
      basis.attributionKind.toString,
      basis.attributionRootMoveMatched.toString,
      basis.attributionDirectProofEligible.toString,
      basis.referenceLine.id,
      basis.candidateLine.id,
      basis.eventLine.id,
      basis.proofDirectSourceIds.mkString(","),
      basis.proofContrastSourceIds.mkString(","),
      basis.proofContextSupportSourceIds.mkString(","),
      basis.supportEvidenceSourceIds.mkString(",")
    ).mkString("|")

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
    longTermExcludedLayers.contains(layer) ||
      StrategicMechanismEvidence.rawStrategicSourceLayer(layer)

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
      case payload: StrategicMechanismEvidence =>
        payload.semanticGroupingAnchors
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
      case payload: LineFactEvidence =>
        payload.semanticGroupingAnchors
      case payload: BoardFactEvidence =>
        payload.semanticGroupingAnchors
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
    }.distinctBy(interaction =>
      (interaction.kind, interaction.sourceClaimId, interaction.targetClaimId, interaction.basis.map(ClaimInteractionBasis.stableKey).sorted)
    )

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
    attributionKind: CauseAttributionKind,
    attributionRootMoveMatched: Boolean,
    attributionDirectProofEligible: Boolean,
    referenceLine: LineNodeRef,
    candidateLine: LineNodeRef,
    eventLine: LineNodeRef,
    proofDirectSourceIds: List[String],
    proofContrastSourceIds: List[String],
    proofContextSupportSourceIds: List[String],
    proofStrategicAxisKeys: List[String],
    proofStrategicMechanismKinds: List[StrategicMechanismKind],
    proofStrategicMechanismSourceIds: List[String],
    proofStrategicMechanismSignalSourceIds: List[String],
    supportEvidenceSourceIds: List[String],
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
    attributionKind: CauseAttributionKind,
    attributionRootMoveMatched: Boolean,
    attributionDirectProofEligible: Boolean,
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
    proofStrategicMechanisms: List[StrategicMechanismProof],
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
    interactions: List[ClaimEventClusterInteraction],
    objectBindingSignatures: List[String] = Nil
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
          supportClusters = supportClusters,
          graph = graph
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
      attributionKind: CauseAttributionKind,
      attributionRootMoveMatched: Boolean,
      attributionDirectProofEligible: Boolean,
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
      cause: Option[RelativeCauseFact] = None,
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
      lineAwareWitnessKeys(claim, linked).map(key =>
        EventMemberBinding(key = key, claim = claim, role = ClaimEventMemberRole.Witness)
      )
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
          EventMemberBinding(key = key, claim = claim, role = role, cause = Some(cause), proof = cause.proof)
        }
      case MoveVerdictCertificationEvidence(certification) =>
        certification.causes.flatMap { cause =>
          eventKey(record.ref, cause).map { key =>
            val role =
              if claim.family == ClaimFamily.Evaluation then ClaimEventMemberRole.VerdictCarrier
              else ClaimEventMemberRole.CauseOwner
            EventMemberBinding(key = key, claim = claim, role = role, cause = Some(cause), proof = cause.proof)
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
        attributionKind = cause.attribution.kind,
        attributionRootMoveMatched = cause.attribution.rootMoveMatched,
        attributionDirectProofEligible = cause.attribution.directProofEligible,
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
    val eventMoveMatched =
      unique.filter(key => claimMoves.contains(normalizeMove(key.eventRootMove)))
    List(eventLineMatched, eventMoveMatched)
      .find(_.nonEmpty)
      .getOrElse(Nil)
      .distinct

  private def normalizeMove(raw: String): String =
    Option(raw).getOrElse("").trim.toLowerCase

  private def clusterFor(
      key: EventClusterKey,
      bindings: List[EventMemberBinding],
      allClaims: List[ClaimSeed],
      supportClusters: List[ClaimSupportCluster],
      graph: TypedEvidenceGraph
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
        proof.attributionKind.toString,
        proof.attributionRootMoveMatched.toString,
        proof.attributionDirectProofEligible.toString,
        proof.eventLine.id,
        proof.proofDirectSourceIds.mkString(","),
        proof.proofContrastSourceIds.mkString(","),
        proof.proofContextSupportSourceIds.mkString(","),
        proof.proofStrategicAxisKeys.mkString(","),
        proof.proofStrategicMechanismSourceIds.mkString(","),
        proof.proofStrategicMechanismSignalSourceIds.mkString(","),
        proof.supportEvidenceSourceIds.mkString(",")
      )
    )
    val interactions = clusterInteractions(memberIds, allClaims)
    val relatedSupportClusterIds = supportClustersRelatedTo(memberIds, interactions, supportClusters)
    Option.when(causeClaimIds.nonEmpty && evidence.nonEmpty) {
      ClaimEventCluster(
        id = eventMembershipClusterId(key, members),
        kind = clusterKind(key.causeKind),
        causeKind = key.causeKind,
        comparisonKind = key.comparisonKind,
        causeRole = key.causeRole,
        causeSourceSide = key.causeSourceSide,
        causeImportance = key.causeImportance,
        attributionKind = key.attributionKind,
        attributionRootMoveMatched = key.attributionRootMoveMatched,
        attributionDirectProofEligible = key.attributionDirectProofEligible,
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
        proofStrategicMechanisms = claimProof.strategicMechanisms,
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
        interactions = interactions,
        objectBindingSignatures = clusterObjectBindingSignatures(bindings, graph)
      )
    }

  private def clusterObjectBindingSignatures(
      bindings: List[EventMemberBinding],
      graph: TypedEvidenceGraph
  ): List[String] =
    EvidenceObjectBinding.objectSignatures(
      bindings.flatMap(binding =>
        binding.cause.toList.flatMap(cause => EvidenceObjectBinding.fromRelativeCause(cause, graph)) ++
          EvidenceObjectBinding.fromClaim(binding.claim, graph)
      )
    )

  private def mergedProof(proofs: List[RelativeCauseProof]): RelativeCauseProof =
    RelativeCauseProof.merge(proofs)

  private def causeProofForBinding(binding: EventMemberBinding): Option[ClaimEventCauseProof] =
    binding.cause.map { cause =>
      val proof = binding.proof.getOrElse(RelativeCauseProof())
      val strategicProof = proof.strategicProofIdentity
      ClaimEventCauseProof(
        claimId = binding.claim.id,
        family = binding.claim.family,
        memberRole = binding.role,
        causeKind = binding.key.causeKind,
        comparisonKind = binding.key.comparisonKind,
        causeRole = binding.key.causeRole,
        causeSourceSide = binding.key.causeSourceSide,
        causeImportance = binding.key.causeImportance,
        attributionKind = cause.attribution.kind,
        attributionRootMoveMatched = cause.attribution.rootMoveMatched,
        attributionDirectProofEligible = cause.attribution.directProofEligible,
        referenceLine = binding.key.referenceLine,
        candidateLine = binding.key.candidateLine,
        eventLine = binding.key.eventLine,
        proofDirectSourceIds = proof.directProof.sourceRefs.map(_.id).distinct.sorted,
        proofContrastSourceIds = proof.contrastProof.sourceRefs.map(_.id).distinct.sorted,
        proofContextSupportSourceIds = proof.contextSupport.sourceRefs.map(_.id).distinct.sorted,
        proofStrategicAxisKeys = strategicProof.axisKeys,
        proofStrategicMechanismKinds = strategicProof.mechanismKinds,
        proofStrategicMechanismSourceIds = strategicProof.mechanismSourceIds,
        proofStrategicMechanismSignalSourceIds = strategicProof.signalSourceIds,
        supportEvidenceSourceIds = cause.supportEvidence.map(_.id).distinct.sorted,
        proofDirectKinds = proof.directProof.kindLabels.distinct.sorted,
        proofContrastKinds = proof.contrastProof.kindLabels.distinct.sorted,
        proofContextSupportKinds = proof.contextSupport.kindLabels.distinct.sorted
      )
    }

  private def eventMembershipClusterId(key: EventClusterKey, members: List[ClaimSeed]): String =
    val lineIdentity =
      stableIdHash(List(key.referenceLine.id, key.candidateLine.id, key.eventLine.id).mkString("||"))
    val memberIdentity =
      stableIdHash(members.map(_.id).sorted.mkString("||"))
    List(
      "claim-event-membership",
      key.comparisonKind.toString,
      key.causeRole.toString,
      key.causeSourceSide.toString,
      key.causeImportance.toString,
      key.attributionKind.toString,
      key.attributionRootMoveMatched.toString,
      key.attributionDirectProofEligible.toString,
      key.causeKind.toString,
      lineIdentity,
      key.eventRootMove,
      memberIdentity
    ).map(idPart).mkString(":")

  private def stableIdHash(raw: String): String =
    MessageDigest
      .getInstance("SHA-256")
      .digest(raw.getBytes(StandardCharsets.UTF_8))
      .map(byte => f"${byte & 0xff}%02x")
      .mkString
      .take(16)

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
    }.distinctBy(interaction =>
      (interaction.kind, interaction.sourceClaimId, interaction.targetClaimId, interaction.basis.map(ClaimInteractionBasis.stableKey).sorted)
    )

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
    val relativeLines = Set(relative.reference.ref, relative.candidate.ref)
    val relativeEvidence =
      (relative.evidence ::
        (relative.counterfactualEvidence ++
          relative.candidateComparisonEvidence ++
          relative.relativeCauseEvidence ++
          relative.verdictCertificationEvidence.toList)).map(_.id).toSet
    idea.ref.family == ChessIdeaFamily.Evaluation ||
      idea.primaryLine.exists(relativeLines.contains) ||
      idea.moveUci.contains(relative.played.moveUci) ||
      idea.evidence.exists(ref =>
        relativeEvidence.contains(ref.id) ||
          ref.line.exists(relativeLines.contains)
      )

enum MoveJudgmentCauseFrameRole:
  case PrimaryCause
  case SecondaryCause
  case ContextCause

enum MoveJudgmentCauseNarrativeRole:
  case RootCause
  case TacticalWitness
  case SupportingCause
  case ContextCause

enum MoveJudgmentCauseWitnessBindingLevel:
  case NotWitness
  case SameComparisonOnly
  case LineContext
  case ObjectContext
  case Punishment

enum MoveJudgmentCauseWitnessBindingSignal:
  case SameComparison
  case SharedExactObjectSignature
  case SharedActor
  case SharedTarget
  case SharedMechanism
  case SharedConsequence
  case SharedWitness
  case SharedDirectObjectSignature
  case SharedDirectActor
  case SharedDirectTarget
  case SharedDirectMechanism
  case SharedDirectConsequence
  case SameEventLine
  case DirectProofSourceOverlap

case class MoveJudgmentVerdictFrame(
    verdict: MoveChoiceVerdict,
    winPercentLossForMover: Double,
    candidateWinPercentDeltaForMover: Double,
    relativeAssessmentEvidenceId: String,
    verdictCertificationEvidenceId: Option[String],
    comparisonKind: CandidateComparisonKind,
    referenceLine: LineNodeRef,
    candidateLine: LineNodeRef
)

case class MoveJudgmentClaimFrame(
    claimId: String,
    family: ClaimFamily,
    tier: PlayerFacingClaimTier,
    subjectBinding: SubjectBindingClass,
    ideaIds: List[String],
    evidenceIds: List[String],
    strategicAxisLineage: List[ClaimStrategicAxisLineage],
    strategicAxisKeys: List[String],
    strategicAxisMechanismEvidenceIds: List[String],
    strategicAxisSourceEvidenceIds: List[String],
    strategicAxisRelativeCauseIds: List[String],
    objectBindingSignatures: List[String],
    concreteObjectReady: Boolean
)

case class MoveJudgmentCauseFrame(
    role: MoveJudgmentCauseFrameRole,
    clusterId: Option[String],
    framed: Boolean,
    causeEvidenceIds: List[String],
    causeKind: RelativeCauseKind,
    comparisonKind: CandidateComparisonKind,
    causeRole: RelativeCauseRole,
    causeSourceSide: RelativeCauseSourceSide,
    causeImportance: RelativeCauseImportance,
    attributionKind: CauseAttributionKind,
    attributionRootMoveMatched: Boolean,
    attributionDirectProofEligible: Boolean,
    referenceLine: LineNodeRef,
    candidateLine: LineNodeRef,
    eventLine: LineNodeRef,
    eventRootMove: String,
    causeClaimIds: List[String],
    evaluationClaimIds: List[String],
    witnessClaimIds: List[String],
    ideaIds: List[String],
    supportIdeaIds: List[String],
    claimCandidateIds: List[String],
    finalClaimIds: List[String],
    relatedSupportClusterIds: List[String],
    evidenceIds: List[String],
    proofDirectSourceIds: List[String],
    proofContrastSourceIds: List[String],
    proofContextSupportSourceIds: List[String],
    proofStrategicAxisLineage: List[StrategicAxisProofLineage],
    proofStrategicAxisKeys: List[String],
    proofStrategicMechanismKinds: List[StrategicMechanismKind],
    proofStrategicMechanismSourceIds: List[String],
    proofStrategicMechanismSignalSourceIds: List[String],
    supportEvidenceSourceIds: List[String],
    objectBindingSignatures: List[String],
    concreteObjectReady: Boolean,
    hasOwnedAdmissibleLongTermProof: Boolean = false,
    narrativeRole: MoveJudgmentCauseNarrativeRole = MoveJudgmentCauseNarrativeRole.RootCause,
    tacticalWitnessCauseEvidenceIds: List[String] = Nil,
    tacticalWitnessCauseKinds: List[RelativeCauseKind] = Nil,
    punishmentWitnessCauseEvidenceIds: List[String] = Nil,
    punishmentWitnessCauseKinds: List[RelativeCauseKind] = Nil,
    contextualTacticalWitnessCauseEvidenceIds: List[String] = Nil,
    contextualTacticalWitnessCauseKinds: List[RelativeCauseKind] = Nil,
    witnessBindingLevel: MoveJudgmentCauseWitnessBindingLevel = MoveJudgmentCauseWitnessBindingLevel.NotWitness,
    witnessBindingSignals: List[MoveJudgmentCauseWitnessBindingSignal] = Nil,
    witnessBindingRootCauseEvidenceIds: List[String] = Nil
)

case class MoveJudgmentLocalIdeaFrame(
    ideaId: String,
    relation: IdeaVerdictRelation,
    claimIds: List[String],
    evidenceIds: List[String]
)

case class MoveJudgmentView(
    verdict: Option[MoveJudgmentVerdictFrame],
    verdictCarriers: List[MoveJudgmentClaimFrame],
    primaryCauses: List[MoveJudgmentCauseFrame],
    secondaryCauses: List[MoveJudgmentCauseFrame],
    contextCauses: List[MoveJudgmentCauseFrame],
    supportContextClusterIds: List[String],
    overriddenLocalIdeas: List[MoveJudgmentLocalIdeaFrame],
    preservedLocalIdeas: List[MoveJudgmentLocalIdeaFrame]
)

object MoveJudgmentView:

  def from(
      relativeAssessments: List[RelativeMoveAssessment],
      evidenceGraph: TypedEvidenceGraph,
      ideas: List[ChessIdea],
      claims: List[ClaimSeed],
      claimLifecycle: List[ClaimLifecycleDiagnostic],
      ideaVerdict: Option[IdeaVerdictSplit],
      claimSupportClusters: List[ClaimSupportCluster],
      claimEventClusters: List[ClaimEventCluster]
  ): Option[MoveJudgmentView] =
    val playedMoves = relativeAssessments.map(assessment => JudgmentSubjectBinding.normalizeMove(assessment.played.moveUci)).toSet
    val claimsById = claims.map(claim => claim.id -> claim).toMap
    val clusterFrames =
      claimEventClusters.map(clusterFrame(_, evidenceGraph, claimLifecycle))
    val unframedFrames =
      unframedCauseFrames(evidenceGraph, ideas, claimLifecycle, claimEventClusters, claimSupportClusters)
    val causeFrames =
      (clusterFrames ++ unframedFrames)
        .distinctBy(frame =>
          (
            frame.clusterId,
            frame.causeEvidenceIds.mkString(","),
            frame.causeKind,
            frame.comparisonKind,
            frame.causeRole,
            frame.causeSourceSide,
            frame.causeImportance,
            frame.eventLine.id
          )
        )
    val narratedCauseFrames = withNarrativeRoles(causeFrames)
    val verdictCarrierIds =
      (claimEventClusters.flatMap(_.evaluationClaimIds) ++
        claims.filter(claim => claim.family == ClaimFamily.Evaluation && claimCarriesVerdict(claim, evidenceGraph, playedMoves)).map(_.id))
        .distinct
        .sorted
    val verdictCarriers =
      verdictCarrierIds.flatMap(claimsById.get).map(claimFrame(_, evidenceGraph, playedMoves))
    val supportContextClusterIds =
      (narratedCauseFrames.flatMap(_.relatedSupportClusterIds) ++ supportClustersRelatedTo(claimSupportClusters, narratedCauseFrames))
        .distinct
        .sorted
    val view =
      MoveJudgmentView(
        verdict = relativeAssessments.headOption.map(verdictFrame),
        verdictCarriers = verdictCarriers,
        primaryCauses = narratedCauseFrames.filter(_.role == MoveJudgmentCauseFrameRole.PrimaryCause),
        secondaryCauses = narratedCauseFrames.filter(_.role == MoveJudgmentCauseFrameRole.SecondaryCause),
        contextCauses = narratedCauseFrames.filter(_.role == MoveJudgmentCauseFrameRole.ContextCause),
        supportContextClusterIds = supportContextClusterIds,
        overriddenLocalIdeas = overriddenLocalIdeas(ideaVerdict, claims),
        preservedLocalIdeas = preservedLocalIdeas(claims)
      )
    Option.when(
      view.verdict.nonEmpty ||
        view.verdictCarriers.nonEmpty ||
        view.primaryCauses.nonEmpty ||
        view.secondaryCauses.nonEmpty ||
        view.contextCauses.nonEmpty ||
        view.overriddenLocalIdeas.nonEmpty ||
        view.preservedLocalIdeas.nonEmpty
    )(view)

  private final case class CauseEvidenceEntry(
      id: String,
      cause: RelativeCauseFact,
      evidence: List[EvidenceRef]
  )

  private def verdictFrame(assessment: RelativeMoveAssessment): MoveJudgmentVerdictFrame =
    MoveJudgmentVerdictFrame(
      verdict = assessment.comparison.verdict,
      winPercentLossForMover = assessment.comparison.winPercentLossForMover,
      candidateWinPercentDeltaForMover = assessment.comparison.candidateWinPercentDeltaForMover,
      relativeAssessmentEvidenceId = assessment.evidence.id,
      verdictCertificationEvidenceId = assessment.verdictCertificationEvidence.map(_.id),
      comparisonKind = CandidateComparisonKind.PlayedVsBest,
      referenceLine = assessment.reference.ref,
      candidateLine = assessment.candidate.ref
    )

  private def claimFrame(
      claim: ClaimSeed,
      graph: TypedEvidenceGraph,
      playedMoves: Set[String]
  ): MoveJudgmentClaimFrame =
    val strategicAxisLineage = ClaimStrategicAxisLineage.fromClaim(claim, graph)
    val objectBindings = EvidenceObjectBinding.fromClaim(claim, graph)
    MoveJudgmentClaimFrame(
      claimId = claim.id,
      family = claim.family,
      tier = PlayerFacingClaimPolicy.tier(claim, graph, playedMoves),
      subjectBinding = JudgmentSubjectBinding.claimBinding(claim, graph, playedMoves),
      ideaIds = claim.ideaRefs.map(_.id).distinct.sorted,
      evidenceIds = judgmentVisibleEvidenceIds(claim.evidence),
      strategicAxisLineage = strategicAxisLineage,
      strategicAxisKeys = strategicAxisLineage.map(_.axisKey).distinct.sorted,
      strategicAxisMechanismEvidenceIds = strategicAxisLineage.map(_.mechanismEvidenceId).distinct.sorted,
      strategicAxisSourceEvidenceIds = strategicAxisLineage.map(_.signalSourceEvidenceId).distinct.sorted,
      strategicAxisRelativeCauseIds = strategicAxisLineage.flatMap(_.relativeCauseIds).distinct.sorted,
      objectBindingSignatures = EvidenceObjectBinding.objectSignatures(objectBindings),
      concreteObjectReady = !PlayerFacingClaimPolicy.requiresConcreteObject(claim.family) ||
        EvidenceObjectBinding.playerFacingReady(objectBindings)
    )

  private def judgmentVisibleEvidenceRefs(refs: List[EvidenceRef]): List[EvidenceRef] =
    refs
      .filterNot(ref => StrategicMechanismEvidence.rawStrategicSourceLayer(ref.layer))
      .distinctBy(_.id)

  private def judgmentVisibleEvidenceIds(refs: List[EvidenceRef]): List[String] =
    judgmentVisibleEvidenceRefs(refs).map(_.id).distinct.sorted

  private def judgmentVisibleEvidenceIds(graph: TypedEvidenceGraph, ids: List[String]): List[String] =
    ids
      .distinct
      .filter(id =>
        graph.byId
          .get(id)
          .forall(record => !StrategicMechanismEvidence.rawStrategicSourceLayer(record.ref.layer))
      )
      .sorted

  private def claimCarriesVerdict(
      claim: ClaimSeed,
      graph: TypedEvidenceGraph,
      playedMoves: Set[String]
  ): Boolean =
    claim.evidence.flatMap(ref => graph.byId.get(ref.id)).exists {
      case EvidenceRecord(_, RelativeAssessmentEvidence(assessment), _) =>
        playedMoves.contains(JudgmentSubjectBinding.normalizeMove(assessment.played.moveUci))
      case EvidenceRecord(_, MoveVerdictCertificationEvidence(certification), _) =>
        playedMoves.contains(JudgmentSubjectBinding.normalizeMove(certification.playedMove))
      case _ =>
        false
    }

  private def clusterFrame(
      cluster: ClaimEventCluster,
      graph: TypedEvidenceGraph,
      claimLifecycle: List[ClaimLifecycleDiagnostic]
  ): MoveJudgmentCauseFrame =
    val lifecycleMatches =
      claimLifecycle.filter(diagnostic => diagnostic.relativeCauses.exists(causeMatchesCluster(_, cluster)))
    val strategicProof = RelativeCauseStrategicProofIdentity.fromMechanisms(cluster.proofStrategicMechanisms)
    MoveJudgmentCauseFrame(
      role = frameRoleFor(cluster.causeRole, cluster.comparisonKind, cluster.causeImportance, cluster.verdict),
      clusterId = Some(cluster.id),
      framed = true,
      causeEvidenceIds = causeEvidenceIdsFor(cluster, graph),
      causeKind = cluster.causeKind,
      comparisonKind = cluster.comparisonKind,
      causeRole = cluster.causeRole,
      causeSourceSide = cluster.causeSourceSide,
      causeImportance = cluster.causeImportance,
      attributionKind = cluster.attributionKind,
      attributionRootMoveMatched = cluster.attributionRootMoveMatched,
      attributionDirectProofEligible = cluster.attributionDirectProofEligible,
      referenceLine = cluster.referenceLine,
      candidateLine = cluster.candidateLine,
      eventLine = cluster.eventLine,
      eventRootMove = cluster.eventRootMove,
      causeClaimIds = cluster.causeClaimIds,
      evaluationClaimIds = cluster.evaluationClaimIds,
      witnessClaimIds = cluster.witnessClaimIds,
      ideaIds = cluster.ideas.map(_.id).distinct.sorted,
      supportIdeaIds = Nil,
      claimCandidateIds = lifecycleMatches.map(_.candidateId).distinct.sorted,
      finalClaimIds = (cluster.memberClaimIds ++ lifecycleMatches.flatMap(_.finalClaimId)).distinct.sorted,
      relatedSupportClusterIds = cluster.relatedSupportClusterIds,
      evidenceIds = judgmentVisibleEvidenceIds(graph, cluster.evidence.map(_.id)),
      proofDirectSourceIds = judgmentVisibleEvidenceIds(graph, cluster.proofDirectSourceIds),
      proofContrastSourceIds = judgmentVisibleEvidenceIds(graph, cluster.proofContrastSourceIds),
      proofContextSupportSourceIds = judgmentVisibleEvidenceIds(graph, cluster.proofContextSupportSourceIds),
      proofStrategicAxisLineage = StrategicAxisProofLineage.fromMechanisms(cluster.proofStrategicMechanisms),
      proofStrategicAxisKeys = strategicProof.axisKeys,
      proofStrategicMechanismKinds = strategicProof.mechanismKinds,
      proofStrategicMechanismSourceIds = strategicProof.mechanismSourceIds,
      proofStrategicMechanismSignalSourceIds = strategicProof.signalSourceIds,
      supportEvidenceSourceIds = judgmentVisibleEvidenceIds(graph, cluster.causeProofs.flatMap(_.supportEvidenceSourceIds)),
      objectBindingSignatures = cluster.objectBindingSignatures,
      concreteObjectReady = cluster.objectBindingSignatures.nonEmpty,
      narrativeRole = defaultNarrativeRole(frameRoleFor(cluster.causeRole, cluster.comparisonKind, cluster.causeImportance, cluster.verdict))
    )

  private def unframedCauseFrames(
      graph: TypedEvidenceGraph,
      ideas: List[ChessIdea],
      claimLifecycle: List[ClaimLifecycleDiagnostic],
      clusters: List[ClaimEventCluster],
      supportClusters: List[ClaimSupportCluster]
  ): List[MoveJudgmentCauseFrame] =
    causeEvidenceEntries(graph)
      .filter(entry => unframedCauseEligible(entry.cause))
      .filterNot(entry => clusters.exists(clusterMatchesCause(_, entry.cause)))
      .map(entry => unframedCauseFrame(entry, graph, ideas, claimLifecycle, supportClusters))

  private def unframedCauseEligible(cause: RelativeCauseFact): Boolean =
    ClaimEventCluster.kindForCause(cause.kind).nonEmpty ||
      primaryLongTermRelativeCause(cause) ||
      playedAlternativeLongTermContext(cause)

  private def primaryLongTermRelativeCause(cause: RelativeCauseFact): Boolean =
    ClaimEventCluster.kindForCause(cause.kind).isEmpty &&
      cause.hasOwnedAdmissibleLongTermProof &&
      cause.comparisonKind == CandidateComparisonKind.PlayedVsBest &&
      cause.role == RelativeCauseRole.PrimaryPlayedCause &&
      (cause.sourceSide == RelativeCauseSourceSide.Reference || cause.sourceSide == RelativeCauseSourceSide.Candidate) &&
      cause.importance == RelativeCauseImportance.Primary &&
      lossVerdict(cause.verdict)

  private[chessjudgment] def playedAlternativeLongTermContext(cause: RelativeCauseFact): Boolean =
    ClaimEventCluster.kindForCause(cause.kind).isEmpty &&
      cause.hasOwnedAdmissibleLongTermProof &&
      cause.comparisonKind == CandidateComparisonKind.PlayedVsAlternative &&
      cause.role == RelativeCauseRole.PlayedAlternativeContext &&
      cause.importance == RelativeCauseImportance.Context &&
      cause.eventLine == cause.candidateLine

  private def lossVerdict(verdict: MoveChoiceVerdict): Boolean =
    verdict match
      case MoveChoiceVerdict.Inaccuracy | MoveChoiceVerdict.Mistake | MoveChoiceVerdict.Blunder =>
        true
      case _ =>
        false

  private def unframedCauseFrame(
      entry: CauseEvidenceEntry,
      graph: TypedEvidenceGraph,
      ideas: List[ChessIdea],
      claimLifecycle: List[ClaimLifecycleDiagnostic],
      supportClusters: List[ClaimSupportCluster]
  ): MoveJudgmentCauseFrame =
    val cause = entry.cause
    val lifecycleMatches =
      claimLifecycle.filter(diagnostic => diagnostic.relativeCauses.exists(causeMatchesLifecycle(_, cause)))
    val directIdeaIds =
      ideas
        .filter(idea => idea.evidence.exists(_.id == entry.id))
        .map(_.ref.id)
        .distinct
        .sorted
    val boundIdeaIds =
      (directIdeaIds ++ lifecycleMatches.flatMap(_.ideaIds)).distinct.sorted
    val supportIdeaIds =
      supportIdeaIdsForUnframedCause(ideas, entry.evidence, boundIdeaIds)
    val relatedSupportClusterIds =
      supportClusterIdsForUnframedCause(supportClusters, boundIdeaIds ++ supportIdeaIds, lifecycleMatches, entry.evidence)
    val proof = cause.proof.getOrElse(RelativeCauseProof())
    val strategicProof = proof.strategicProofIdentity
    val objectBindings = EvidenceObjectBinding.fromRelativeCause(cause, graph)
    MoveJudgmentCauseFrame(
      role = frameRoleFor(cause.role, cause.comparisonKind, cause.importance, cause.verdict),
      clusterId = None,
      framed = false,
      causeEvidenceIds = List(entry.id),
      causeKind = cause.kind,
      comparisonKind = cause.comparisonKind,
      causeRole = cause.role,
      causeSourceSide = cause.sourceSide,
      causeImportance = cause.importance,
      attributionKind = cause.attribution.kind,
      attributionRootMoveMatched = cause.attribution.rootMoveMatched,
      attributionDirectProofEligible = cause.attribution.directProofEligible,
      referenceLine = cause.referenceLine,
      candidateLine = cause.candidateLine,
      eventLine = cause.eventLine,
      eventRootMove = cause.eventRootMove,
      causeClaimIds = Nil,
      evaluationClaimIds = Nil,
      witnessClaimIds = Nil,
      ideaIds = boundIdeaIds,
      supportIdeaIds = supportIdeaIds,
      claimCandidateIds = lifecycleMatches.map(_.candidateId).distinct.sorted,
      finalClaimIds = lifecycleMatches.flatMap(_.finalClaimId).distinct.sorted,
      relatedSupportClusterIds = relatedSupportClusterIds,
      evidenceIds = judgmentVisibleCauseEvidenceIds(entry.evidence, cause),
      proofDirectSourceIds = judgmentVisibleCauseEvidenceIds(proof.directProof.sourceRefs, cause),
      proofContrastSourceIds = judgmentVisibleEvidenceIds(proof.contrastProof.sourceRefs),
      proofContextSupportSourceIds = judgmentVisibleEvidenceIds(proof.contextSupport.sourceRefs),
      proofStrategicAxisLineage = proof.strategicAxisLineage,
      proofStrategicAxisKeys = strategicProof.axisKeys,
      proofStrategicMechanismKinds = strategicProof.mechanismKinds,
      proofStrategicMechanismSourceIds = strategicProof.mechanismSourceIds,
      proofStrategicMechanismSignalSourceIds = strategicProof.signalSourceIds,
      supportEvidenceSourceIds = judgmentVisibleCauseEvidenceIds(cause.supportEvidence, cause),
      objectBindingSignatures = EvidenceObjectBinding.objectSignatures(objectBindings),
      concreteObjectReady = EvidenceObjectBinding.playerFacingReady(objectBindings),
      hasOwnedAdmissibleLongTermProof = cause.hasOwnedAdmissibleLongTermProof,
      narrativeRole = defaultNarrativeRole(frameRoleFor(cause.role, cause.comparisonKind, cause.importance, cause.verdict))
    )

  private def supportIdeaIdsForUnframedCause(
      ideas: List[ChessIdea],
      evidence: List[EvidenceRef],
      boundIdeaIds: List[String]
  ): List[String] =
    val evidenceIds = evidence.map(_.id).toSet
    val bound = boundIdeaIds.toSet
    ideas
      .filter(idea => idea.evidence.exists(ref => evidenceIds.contains(ref.id)))
      .map(_.ref.id)
      .filterNot(bound.contains)
      .distinct
      .sorted

  private def supportClusterIdsForUnframedCause(
      supportClusters: List[ClaimSupportCluster],
      ideaIds: List[String],
      lifecycleMatches: List[ClaimLifecycleDiagnostic],
      evidence: List[EvidenceRef]
  ): List[String] =
    val ideaIdSet = ideaIds.toSet
    val evidenceIds = evidence.map(_.id).toSet
    val lifecycleClaimIds =
      lifecycleMatches.flatMap(diagnostic =>
        diagnostic.finalClaimId.toList ++
          diagnostic.sourceCandidateIds ++
          List(diagnostic.claimId, diagnostic.candidateId)
      ).toSet
    supportClusters
      .filter { cluster =>
        cluster.ideas.exists(idea => ideaIdSet.contains(idea.id)) ||
          cluster.evidence.exists(ref => evidenceIds.contains(ref.id)) ||
          (cluster.anchorClaimIds ++ cluster.supportingClaimIds ++ cluster.constrainingClaimIds).exists(lifecycleClaimIds.contains)
      }
      .map(_.id)
      .distinct
      .sorted

  private def frameRoleFor(
      causeRole: RelativeCauseRole,
      comparisonKind: CandidateComparisonKind,
      importance: RelativeCauseImportance,
      verdict: MoveChoiceVerdict
  ): MoveJudgmentCauseFrameRole =
    if causeRole == RelativeCauseRole.PrimaryPlayedCause &&
      comparisonKind == CandidateComparisonKind.PlayedVsBest &&
      importance == RelativeCauseImportance.Primary &&
      lossVerdict(verdict)
    then
      MoveJudgmentCauseFrameRole.PrimaryCause
    else if importance == RelativeCauseImportance.Context ||
      causeRole == RelativeCauseRole.PlayedAlternativeContext ||
      causeRole == RelativeCauseRole.AlternativeDiagnostic
    then MoveJudgmentCauseFrameRole.ContextCause
    else MoveJudgmentCauseFrameRole.SecondaryCause

  private def defaultNarrativeRole(role: MoveJudgmentCauseFrameRole): MoveJudgmentCauseNarrativeRole =
    role match
      case MoveJudgmentCauseFrameRole.PrimaryCause   => MoveJudgmentCauseNarrativeRole.RootCause
      case MoveJudgmentCauseFrameRole.SecondaryCause => MoveJudgmentCauseNarrativeRole.SupportingCause
      case MoveJudgmentCauseFrameRole.ContextCause   => MoveJudgmentCauseNarrativeRole.ContextCause

  private def withNarrativeRoles(frames: List[MoveJudgmentCauseFrame]): List[MoveJudgmentCauseFrame] =
    val longTermRootsByComparison =
      frames
        .filter(longTermRootFrame)
        .groupBy(comparisonFrameKey)
    frames.map { frame =>
      val matchingLongTermRoots = longTermRootsByComparison.getOrElse(comparisonFrameKey(frame), Nil)
      if longTermRootFrame(frame) then
        val witnesses =
          frames
            .filter(candidate => sameComparison(candidate, frame) && tacticalWitnessFrame(candidate))
            .filterNot(_.causeEvidenceIds == frame.causeEvidenceIds)
        val witnessBindings = witnesses.map(witness => witness -> witnessBinding(frame, witness))
        val punishmentWitnesses =
          witnessBindings.collect { case (witness, binding) if binding.level == MoveJudgmentCauseWitnessBindingLevel.Punishment =>
            witness
          }
        val contextualWitnesses =
          witnessBindings.collect {
            case (witness, binding)
                if binding.level != MoveJudgmentCauseWitnessBindingLevel.Punishment &&
                  binding.level != MoveJudgmentCauseWitnessBindingLevel.SameComparisonOnly =>
            witness
          }
        frame.copy(
          narrativeRole = MoveJudgmentCauseNarrativeRole.RootCause,
          tacticalWitnessCauseEvidenceIds = punishmentWitnesses.flatMap(_.causeEvidenceIds).distinct.sorted,
          tacticalWitnessCauseKinds = punishmentWitnesses.map(_.causeKind).distinct,
          punishmentWitnessCauseEvidenceIds = punishmentWitnesses.flatMap(_.causeEvidenceIds).distinct.sorted,
          punishmentWitnessCauseKinds = punishmentWitnesses.map(_.causeKind).distinct,
          contextualTacticalWitnessCauseEvidenceIds = contextualWitnesses.flatMap(_.causeEvidenceIds).distinct.sorted,
          contextualTacticalWitnessCauseKinds = contextualWitnesses.map(_.causeKind).distinct
        )
      else if tacticalWitnessFrame(frame) && matchingLongTermRoots.nonEmpty then
        val binding = strongestWitnessBinding(frame, matchingLongTermRoots)
        if binding.level == MoveJudgmentCauseWitnessBindingLevel.SameComparisonOnly then
          frame.copy(
            narrativeRole = MoveJudgmentCauseNarrativeRole.ContextCause,
            witnessBindingLevel = binding.level,
            witnessBindingSignals = binding.signals,
            witnessBindingRootCauseEvidenceIds = binding.rootCauseEvidenceIds
          )
        else
          frame.copy(
            narrativeRole = MoveJudgmentCauseNarrativeRole.TacticalWitness,
            witnessBindingLevel = binding.level,
            witnessBindingSignals = binding.signals,
            witnessBindingRootCauseEvidenceIds = binding.rootCauseEvidenceIds
          )
      else
        frame.copy(narrativeRole = defaultNarrativeRole(frame.role))
    }

  private final case class TacticalWitnessBinding(
      level: MoveJudgmentCauseWitnessBindingLevel,
      signals: List[MoveJudgmentCauseWitnessBindingSignal],
      rootCauseEvidenceIds: List[String]
  )

  private def strongestWitnessBinding(
      witness: MoveJudgmentCauseFrame,
      roots: List[MoveJudgmentCauseFrame]
  ): TacticalWitnessBinding =
    roots
      .filter(root => sameComparison(root, witness))
      .map(root => witnessBinding(root, witness))
      .sortBy(binding => witnessBindingRank(binding.level))
      .lastOption
      .getOrElse(
        TacticalWitnessBinding(
          MoveJudgmentCauseWitnessBindingLevel.SameComparisonOnly,
          List(MoveJudgmentCauseWitnessBindingSignal.SameComparison),
          Nil
        )
      )

  private def witnessBinding(
      root: MoveJudgmentCauseFrame,
      witness: MoveJudgmentCauseFrame
  ): TacticalWitnessBinding =
    val rootSignatures = root.objectBindingSignatures.toSet
    val witnessSignatures = witness.objectBindingSignatures.toSet
    val rootDirectSignatures = objectSignatures(root, Some(RelativeCauseProofRole.DirectProof))
    val witnessDirectSignatures = objectSignatures(witness, Some(RelativeCauseProofRole.DirectProof))
    val sharedExact = rootSignatures.intersect(witnessSignatures).nonEmpty
    val sharedActor = sharedObjectToken(root, witness, "actor")
    val sharedTarget = sharedObjectToken(root, witness, "target")
    val sharedMechanism = sharedObjectToken(root, witness, "mechanism")
    val sharedConsequence = sharedObjectToken(root, witness, "consequence")
    val sharedWitness = sharedObjectToken(root, witness, "witness")
    val sharedDirectExact = rootDirectSignatures.intersect(witnessDirectSignatures).nonEmpty
    val sharedDirectActor = sharedObjectToken(root, witness, "actor", Some(RelativeCauseProofRole.DirectProof))
    val sharedDirectTarget = sharedObjectToken(root, witness, "target", Some(RelativeCauseProofRole.DirectProof))
    val sharedDirectMechanism = sharedObjectToken(root, witness, "mechanism", Some(RelativeCauseProofRole.DirectProof))
    val sharedDirectConsequence = sharedObjectToken(root, witness, "consequence", Some(RelativeCauseProofRole.DirectProof))
    val sameEvent = root.eventLine == witness.eventLine
    val directProofOverlap = root.proofDirectSourceIds.toSet.intersect(witness.proofDirectSourceIds.toSet).nonEmpty
    val signals =
      List(
        Some(MoveJudgmentCauseWitnessBindingSignal.SameComparison),
        Option.when(sharedExact)(MoveJudgmentCauseWitnessBindingSignal.SharedExactObjectSignature),
        Option.when(sharedActor)(MoveJudgmentCauseWitnessBindingSignal.SharedActor),
        Option.when(sharedTarget)(MoveJudgmentCauseWitnessBindingSignal.SharedTarget),
        Option.when(sharedMechanism)(MoveJudgmentCauseWitnessBindingSignal.SharedMechanism),
        Option.when(sharedConsequence)(MoveJudgmentCauseWitnessBindingSignal.SharedConsequence),
        Option.when(sharedWitness)(MoveJudgmentCauseWitnessBindingSignal.SharedWitness),
        Option.when(sharedDirectExact)(MoveJudgmentCauseWitnessBindingSignal.SharedDirectObjectSignature),
        Option.when(sharedDirectActor)(MoveJudgmentCauseWitnessBindingSignal.SharedDirectActor),
        Option.when(sharedDirectTarget)(MoveJudgmentCauseWitnessBindingSignal.SharedDirectTarget),
        Option.when(sharedDirectMechanism)(MoveJudgmentCauseWitnessBindingSignal.SharedDirectMechanism),
        Option.when(sharedDirectConsequence)(MoveJudgmentCauseWitnessBindingSignal.SharedDirectConsequence),
        Option.when(sameEvent)(MoveJudgmentCauseWitnessBindingSignal.SameEventLine),
        Option.when(directProofOverlap)(MoveJudgmentCauseWitnessBindingSignal.DirectProofSourceOverlap)
      ).flatten.distinct.sortBy(_.toString)
    TacticalWitnessBinding(
      level = witnessBindingLevel(
        objectBound = sharedExact || sharedActor || sharedTarget,
        directObjectBound = sharedDirectExact || sharedDirectActor || sharedDirectTarget,
        mechanismBound = sharedMechanism,
        consequenceBound = sharedConsequence,
        directMechanismBound = sharedDirectMechanism,
        directConsequenceBound = sharedDirectConsequence,
        lineOrEvalBound = sharedWitness || sameEvent || directProofOverlap,
        eventOrEvalBound = sameEvent || directProofOverlap
      ),
      signals = signals,
      rootCauseEvidenceIds = root.causeEvidenceIds.distinct.sorted
    )

  private def witnessBindingLevel(
      objectBound: Boolean,
      directObjectBound: Boolean,
      mechanismBound: Boolean,
      consequenceBound: Boolean,
      directMechanismBound: Boolean,
      directConsequenceBound: Boolean,
      lineOrEvalBound: Boolean,
      eventOrEvalBound: Boolean
  ): MoveJudgmentCauseWitnessBindingLevel =
    if directObjectBound && directMechanismBound && directConsequenceBound && eventOrEvalBound then
      MoveJudgmentCauseWitnessBindingLevel.Punishment
    else if objectBound && (mechanismBound || consequenceBound) then
      MoveJudgmentCauseWitnessBindingLevel.ObjectContext
    else if lineOrEvalBound then MoveJudgmentCauseWitnessBindingLevel.LineContext
    else MoveJudgmentCauseWitnessBindingLevel.SameComparisonOnly

  private def sharedObjectToken(
      left: MoveJudgmentCauseFrame,
      right: MoveJudgmentCauseFrame,
      role: String,
      proofRole: Option[RelativeCauseProofRole] = None
  ): Boolean =
    objectTokens(left, role, proofRole).intersect(objectTokens(right, role, proofRole)).nonEmpty

  private def objectTokens(
      frame: MoveJudgmentCauseFrame,
      role: String,
      proofRole: Option[RelativeCauseProofRole]
  ): Set[String] =
    val prefix = s"$role="
    objectSignatures(frame, proofRole).flatMap { signature =>
      signature
        .split("\\|")
        .toList
        .collect { case part if part.startsWith(prefix) => part.stripPrefix(prefix) }
    }.toSet

  private def objectSignatures(
      frame: MoveJudgmentCauseFrame,
      proofRole: Option[RelativeCauseProofRole]
  ): Set[String] =
    val proofPart = proofRole.map(role => s"proof=$role")
    frame.objectBindingSignatures.filter(signature => proofPart.forall(signature.contains)).toSet

  private def witnessBindingRank(level: MoveJudgmentCauseWitnessBindingLevel): Int =
    level match
      case MoveJudgmentCauseWitnessBindingLevel.NotWitness          => 0
      case MoveJudgmentCauseWitnessBindingLevel.SameComparisonOnly => 1
      case MoveJudgmentCauseWitnessBindingLevel.LineContext        => 2
      case MoveJudgmentCauseWitnessBindingLevel.ObjectContext      => 3
      case MoveJudgmentCauseWitnessBindingLevel.Punishment         => 4

  private def longTermRootFrame(frame: MoveJudgmentCauseFrame): Boolean =
    frame.role == MoveJudgmentCauseFrameRole.PrimaryCause &&
      frame.hasOwnedAdmissibleLongTermProof &&
      ClaimEventCluster.kindForCause(frame.causeKind).isEmpty &&
      frame.comparisonKind == CandidateComparisonKind.PlayedVsBest &&
      frame.causeRole == RelativeCauseRole.PrimaryPlayedCause &&
      frame.causeImportance == RelativeCauseImportance.Primary &&
      frame.attributionDirectProofEligible

  private def tacticalWitnessFrame(frame: MoveJudgmentCauseFrame): Boolean =
    frame.role == MoveJudgmentCauseFrameRole.PrimaryCause &&
      ClaimEventCluster.kindForCause(frame.causeKind).exists(kind =>
        kind == ClaimEventClusterKind.TacticalEvent ||
          kind == ClaimEventClusterKind.DefensiveEvent ||
          kind == ClaimEventClusterKind.ConversionEvent ||
          kind == ClaimEventClusterKind.MaterialEvent
      )

  private def sameComparison(left: MoveJudgmentCauseFrame, right: MoveJudgmentCauseFrame): Boolean =
    comparisonFrameKey(left) == comparisonFrameKey(right)

  private def comparisonFrameKey(frame: MoveJudgmentCauseFrame): (CandidateComparisonKind, LineNodeRef, LineNodeRef) =
    (frame.comparisonKind, frame.referenceLine, frame.candidateLine)

  private def causeEvidenceEntries(graph: TypedEvidenceGraph): List[CauseEvidenceEntry] =
    val standalone =
      graph.records.collect { case EvidenceRecord(ref, RelativeCauseFactEvidence(cause), parents) =>
        CauseEvidenceEntry(ref.id, cause, relativeCauseEvidence(ref, parents, cause))
      }
    val standaloneKeys = standalone.map(_.cause.identityKey).toSet
    val embedded =
      graph.records.collect { case EvidenceRecord(ref, MoveVerdictCertificationEvidence(certification), parents) =>
        certification.causes.zipWithIndex
          .filterNot { case (cause, _) => standaloneKeys.contains(cause.identityKey) }
          .map { case (cause, index) =>
            CauseEvidenceEntry(s"${ref.id}:cause:$index:${cause.kind}", cause, relativeCauseEvidence(ref, parents, cause))
          }
      }.flatten
    (standalone ++ embedded).distinctBy(_.cause.identityKey)

  private def relativeCauseEvidence(
      ref: EvidenceRef,
      parents: List[EvidenceRef],
      cause: RelativeCauseFact
  ): List[EvidenceRef] =
    val proofRefs =
      cause.proof.toList.flatMap(proof =>
        proof.directProof.sourceRefs ++ proof.contrastProof.sourceRefs ++ proof.contextSupport.sourceRefs
      )
    (ref :: (parents ++ cause.supportEvidence ++ proofRefs)).distinctBy(_.id)

  private def judgmentVisibleCauseEvidenceIds(refs: List[EvidenceRef], cause: RelativeCauseFact): List[String] =
    val directProofSourceIds =
      cause.proof.toList.flatMap(_.directProof.sourceRefs.map(_.id)).toSet
    refs
      .filter(ref => directProofSourceIds.contains(ref.id) || !StrategicMechanismEvidence.rawStrategicSourceLayer(ref.layer))
      .map(_.id)
      .distinct
      .sorted

  private def causeEvidenceIdsFor(
      cluster: ClaimEventCluster,
      graph: TypedEvidenceGraph
  ): List[String] =
    graph.records.collect {
      case EvidenceRecord(ref, RelativeCauseFactEvidence(cause), _) if clusterMatchesCause(cluster, cause) =>
        ref.id
    }.distinct.sorted

  private def clusterMatchesCause(cluster: ClaimEventCluster, cause: RelativeCauseFact): Boolean =
    cluster.causeKind == cause.kind &&
      cluster.comparisonKind == cause.comparisonKind &&
      cluster.causeRole == cause.role &&
      cluster.causeSourceSide == cause.sourceSide &&
      cluster.causeImportance == cause.importance &&
      cluster.attributionKind == cause.attribution.kind &&
      cluster.attributionRootMoveMatched == cause.attribution.rootMoveMatched &&
      cluster.attributionDirectProofEligible == cause.attribution.directProofEligible &&
      cluster.referenceLine == cause.referenceLine &&
      cluster.candidateLine == cause.candidateLine &&
      cluster.eventLine == cause.eventLine

  private def causeMatchesCluster(cause: ClaimLifecycleRelativeCause, cluster: ClaimEventCluster): Boolean =
    val clusterStrategicProof = RelativeCauseStrategicProofIdentity.fromMechanisms(cluster.proofStrategicMechanisms)
    cause.kind == cluster.causeKind &&
      cause.comparisonKind == cluster.comparisonKind &&
      cause.role == cluster.causeRole &&
      cause.sourceSide == cluster.causeSourceSide &&
      cause.importance == cluster.causeImportance &&
      cause.attributionKind == cluster.attributionKind &&
      cause.attributionRootMoveMatched == cluster.attributionRootMoveMatched &&
      cause.attributionDirectProofEligible == cluster.attributionDirectProofEligible &&
      cause.referenceLine == cluster.referenceLine &&
      cause.candidateLine == cluster.candidateLine &&
      cause.eventLine == cluster.eventLine &&
      strategicProofCovers(clusterStrategicProof, cause)

  private def causeMatchesLifecycle(
      lifecycleCause: ClaimLifecycleRelativeCause,
      cause: RelativeCauseFact
  ): Boolean =
    val proof = cause.proof
    val strategicProof = cause.strategicProofIdentity
    lifecycleCause.kind == cause.kind &&
      lifecycleCause.comparisonKind == cause.comparisonKind &&
      lifecycleCause.role == cause.role &&
      lifecycleCause.sourceSide == cause.sourceSide &&
      lifecycleCause.importance == cause.importance &&
      lifecycleCause.referenceLine == cause.referenceLine &&
      lifecycleCause.candidateLine == cause.candidateLine &&
      lifecycleCause.eventLine == cause.eventLine &&
      lifecycleCause.attributionKind == cause.attribution.kind &&
      lifecycleCause.attributionOwnedEvidenceIds.toSet == cause.attribution.ownedEvidence.map(_.id).toSet &&
      lifecycleCause.attributionContrastEvidenceIds.toSet == cause.attribution.contrastEvidence.map(_.id).toSet &&
      lifecycleCause.attributionContextEvidenceIds.toSet == cause.attribution.contextEvidence.map(_.id).toSet &&
      lifecycleCause.attributionRootMoveMatched == cause.attribution.rootMoveMatched &&
      lifecycleCause.attributionDirectProofEligible == cause.attribution.directProofEligible &&
      lifecycleCause.attributionReason == cause.attribution.reason &&
      lifecycleCause.supportEvidenceSourceIds.toSet == cause.supportEvidence.map(_.id).toSet &&
      lifecycleCause.proofDirectSourceIds.toSet == proof.toList.flatMap(_.directProof.sourceRefs.map(_.id)).toSet &&
      lifecycleCause.proofContrastSourceIds.toSet == proof.toList.flatMap(_.contrastProof.sourceRefs.map(_.id)).toSet &&
      lifecycleCause.proofContextSupportSourceIds.toSet == proof.toList.flatMap(_.contextSupport.sourceRefs.map(_.id)).toSet &&
      lifecycleCause.proofStrategicAxisKeys == strategicProof.axisKeys &&
      lifecycleCause.proofStrategicMechanismKinds == strategicProof.mechanismKinds &&
      lifecycleCause.proofStrategicMechanismSourceIds == strategicProof.mechanismSourceIds &&
      lifecycleCause.proofStrategicMechanismSignalSourceIds == strategicProof.signalSourceIds &&
      lifecycleCause.proofDirectKinds.toSet == proof.toList.flatMap(_.directProof.kindLabels).toSet &&
      lifecycleCause.proofContrastKinds.toSet == proof.toList.flatMap(_.contrastProof.kindLabels).toSet &&
      lifecycleCause.proofContextSupportKinds.toSet == proof.toList.flatMap(_.contextSupport.kindLabels).toSet

  private def strategicProofCovers(
      cluster: RelativeCauseStrategicProofIdentity,
      cause: ClaimLifecycleRelativeCause
  ): Boolean =
    containsAll(cluster.axisKeys, cause.proofStrategicAxisKeys) &&
      containsAll(cluster.mechanismKinds, cause.proofStrategicMechanismKinds) &&
      containsAll(cluster.mechanismSourceIds, cause.proofStrategicMechanismSourceIds) &&
      containsAll(cluster.signalSourceIds, cause.proofStrategicMechanismSignalSourceIds)

  private def containsAll[A](available: List[A], required: List[A]): Boolean =
    val availableSet = available.toSet
    required.forall(availableSet.contains)

  private def supportClustersRelatedTo(
      supportClusters: List[ClaimSupportCluster],
      frames: List[MoveJudgmentCauseFrame]
  ): List[String] =
    val frameClaimIds =
      frames.flatMap(frame => frame.causeClaimIds ++ frame.evaluationClaimIds ++ frame.witnessClaimIds ++ frame.finalClaimIds).toSet
    supportClusters
      .filter(cluster =>
        (cluster.anchorClaimIds ++ cluster.supportingClaimIds ++ cluster.constrainingClaimIds).exists(frameClaimIds.contains)
      )
      .map(_.id)

  private def overriddenLocalIdeas(
      ideaVerdict: Option[IdeaVerdictSplit],
      claims: List[ClaimSeed]
  ): List[MoveJudgmentLocalIdeaFrame] =
    val claimIdsByIdea =
      claims
        .flatMap(claim => claim.ideaRefs.map(idea => idea.id -> claim.id))
        .groupMap(_._1)(_._2)
    ideaVerdict.toList.flatMap(_.bindings).collect {
      case binding if binding.relation == IdeaVerdictRelation.RefutesIdea =>
        MoveJudgmentLocalIdeaFrame(
          ideaId = binding.idea.id,
          relation = binding.relation,
          claimIds = claimIdsByIdea.getOrElse(binding.idea.id, Nil).distinct.sorted,
          evidenceIds = judgmentVisibleEvidenceIds(binding.evidence)
        )
    }.distinctBy(frame => (frame.ideaId, frame.relation))

  private def preservedLocalIdeas(
      claims: List[ClaimSeed]
  ): List[MoveJudgmentLocalIdeaFrame] =
    val claimsById = claims.map(claim => claim.id -> claim).toMap
    claims.flatMap { claim =>
      claim.salience.toList.flatMap(_.interactions).filter(_.kind == ClaimInteractionKind.BadVerdictPreservesLocalIdea).flatMap {
        interaction =>
          claim.ideaRefs.map { idea =>
            MoveJudgmentLocalIdeaFrame(
              ideaId = idea.id,
              relation = IdeaVerdictRelation.ExplainsIdeaDespiteBadVerdict,
              claimIds = List(claim.id, interaction.relatedClaimId).filter(claimsById.contains).distinct.sorted,
              evidenceIds = judgmentVisibleEvidenceIds(interaction.interactionEvidence)
            )
          }
      }
    }.distinctBy(frame => (frame.ideaId, frame.claimIds.mkString(","), frame.evidenceIds.mkString(",")))

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
    moveJudgmentView: Option[MoveJudgmentView] = None,
    diagnostics: EvidenceLossReport = EvidenceLossReport.empty,
    probeRequests: List[ProbeRequest] = Nil,
    probeDiagnostics: List[ProbeAdmissionDiagnostic] = Nil
):
  def playedTransition: Option[MoveTransitionEdge] =
    transitions.find(_.role == TransitionEdgeRole.Played)

  def referenceTransition: Option[MoveTransitionEdge] =
    transitions.find(_.role == TransitionEdgeRole.Reference)

type LlmJudgmentPacket = EvidenceBackedJudgmentPacket
