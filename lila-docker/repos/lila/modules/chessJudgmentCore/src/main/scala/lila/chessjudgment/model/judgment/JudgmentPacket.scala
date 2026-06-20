package lila.chessjudgment.model.judgment

import chess.Color
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
        if playedMoves.contains(normalizeMove(certification.playedMove)) then SubjectBindingClass.PrimaryPlayedCause
        else strongest(certification.causes.map(relativeCauseBinding(_, playedMoves)))
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
    val eventIsPlayed = playedMoves.contains(normalizeMove(cause.eventRootMove))
    if !eventIsPlayed then SubjectBindingClass.Other
    else
      cause.comparisonKind match
        case CandidateComparisonKind.PlayedVsBest =>
          SubjectBindingClass.PrimaryPlayedCause
        case CandidateComparisonKind.PlayedVsAlternative | CandidateComparisonKind.BestVsSecond |
            CandidateComparisonKind.ReferenceVsAlternative =>
          SubjectBindingClass.ContextPlayed

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
      localLineBinding && directEvidencePayload(record.payload)
    }

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

case class ClaimInteraction(
    kind: ClaimInteractionKind,
    relatedClaimId: String,
    strength: Int,
    interactionEvidence: List[EvidenceRef]
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
    evidence: List[EvidenceRef]
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

enum EvidenceSemanticAnchorKind:
  case StrategicKind
  case Plan
  case BoardAnchor
  case PawnStructure
  case StructurePlan
  case PawnPlay
  case OpeningAnchor
  case OpeningSupported
  case OpeningObserved
  case CandidateComparison
  case PlanPressure
  case PlanTransition
  case LineEvent
  case LineConsequence
  case StructuralDelta

final case class EvidenceSemanticAnchor(
    kind: EvidenceSemanticAnchorKind,
    values: List[String]
):
  def stableKey: String =
    (kind.toString :: values).mkString(":")

object EvidenceSemanticAnchor:
  def of(kind: EvidenceSemanticAnchorKind, values: String*): EvidenceSemanticAnchor =
    EvidenceSemanticAnchor(kind, values.toList)

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
      EvidenceLayer.RelativeCause,
      EvidenceLayer.RelativeAssessment,
      EvidenceLayer.CandidateComparison,
      EvidenceLayer.Counterfactual,
      EvidenceLayer.MoveVerdictCertification
    )

  private[chessjudgment] def causeBoundLayer(layer: EvidenceLayer): Boolean =
    causeBoundLayers.contains(layer)

  private def longTermSupportEligible(claim: ClaimSeed, graph: TypedEvidenceGraph): Boolean =
    claim.family.isLongTerm &&
      claim.engineComparison.isEmpty &&
      claim.evidence.nonEmpty &&
      !claim.evidence.exists(ref => causeBoundLayer(ref.layer)) &&
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
      case payload @ StrategicFactEvidence(kind, _, relatedPlans, _) =>
        EvidenceSemanticAnchor.of(StrategicKind, kind.toString) ::
          relatedPlans.map(plan => EvidenceSemanticAnchor.of(Plan, plan.toString)) ++
          payload.anchorsForSemanticGrouping.map(boardAnchorSemantic)
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
        Option
          .when(payload.hasLineEvent(LineEventKind.Castling))(
            EvidenceSemanticAnchor.of(LineEvent, LineEventKind.Castling.toString)
          )
          .toList ++
          payload.consequenceProfile.proofSignalKinds.map(kind =>
            EvidenceSemanticAnchor.of(LineConsequence, kind.toString)
          )
      case payload: BoardFactEvidence =>
        payload.anchorsForSemanticGrouping.map(boardAnchorSemantic)
      case StructuralDeltaEvidence(delta) =>
        List(
          Option.when(delta.createdTargetPressure.nonEmpty)(EvidenceSemanticAnchor.of(StructuralDelta, "created-target-pressure")),
          Option.when(delta.releasedTargetPressure.nonEmpty)(EvidenceSemanticAnchor.of(StructuralDelta, "released-target-pressure")),
          Option.when(delta.openedFiles.nonEmpty || delta.semiOpenedFiles.nonEmpty || delta.fileOccupation.nonEmpty)(
            EvidenceSemanticAnchor.of(StructuralDelta, "file")
          ),
          Option.when(delta.newWeakPawns.nonEmpty || delta.newWeakSquares.nonEmpty)(
            EvidenceSemanticAnchor.of(StructuralDelta, "weakness")
          ),
          Option.when(delta.createdTension.nonEmpty || delta.pawnTensionDelta != 0)(
            EvidenceSemanticAnchor.of(StructuralDelta, "tension")
          ),
          Option.when(delta.centerControlDelta != 0)(EvidenceSemanticAnchor.of(StructuralDelta, "center")),
          Option.when(delta.developmentDelta != 0)(EvidenceSemanticAnchor.of(StructuralDelta, "development")),
          Option.when(delta.developmentMoves.nonEmpty)(EvidenceSemanticAnchor.of(StructuralDelta, "development-choice")),
          Option.when(delta.mobilityDelta != 0)(EvidenceSemanticAnchor.of(StructuralDelta, "mobility"))
        ).flatten
      case _ =>
        Nil

  private def boardAnchorSemantic(anchor: BoardAnchor): EvidenceSemanticAnchor =
    val side = if anchor.side.white then "white" else "black"
    val detailValues =
      anchor.detail.toList.flatMap(detail =>
        List(
          detail.subjectColor.map(color => s"subject-color:${colorKey(color)}"),
          detail.attackerColor.map(color => s"attacker-color:${colorKey(color)}"),
          detail.subjectSquare.map(square => s"subject-square:${square.key}"),
          detail.targetSquare.map(square => s"target-square:${square.key}"),
          Option
            .when(detail.relatedSquares.nonEmpty)(s"related:${detail.relatedSquares.map(_.key).sorted.mkString(",")}"),
          detail.file.map(file => s"file:${file.key}"),
          detail.axis.map(axis => s"axis:$axis")
        ).flatten
      )
    EvidenceSemanticAnchor.of(
      EvidenceSemanticAnchorKind.BoardAnchor,
      (List(side, anchor.kind.toString, anchor.signal.toString) ++ detailValues)*
    )

  private def colorKey(color: Color): String =
    if color.white then "white" else "black"

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
            evidence = interaction.interactionEvidence
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
      case ClaimInteractionKind.TacticalSupportsStrategicPlan | ClaimInteractionKind.StrategicCompensationSupportsSacrifice |
          ClaimInteractionKind.ConversionSecuresAdvantage =>
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
    evidence: List[EvidenceRef]
)

case class ClaimEventCluster(
    id: String,
    kind: ClaimEventClusterKind,
    causeKind: RelativeCauseKind,
    comparisonKind: CandidateComparisonKind,
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
    proofSupportLayers: Set[EvidenceLayer],
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
      referenceLine: LineNodeRef,
      candidateLine: LineNodeRef,
      eventLine: LineNodeRef,
      verdict: MoveChoiceVerdict,
      winPercentLossForMover: Double,
      candidateWinPercentDeltaForMover: Double
  ):
    def eventRootMove: String = eventLine.rootMove

  private enum EventMemberRole:
    case CauseOwner
    case VerdictCarrier
    case Witness

  private final case class EventMemberBinding(
      key: EventClusterKey,
      claim: ClaimSeed,
      role: EventMemberRole,
      proof: Option[RelativeCauseProof] = None
  )

  private val eventEvidenceLayers: Set[EvidenceLayer] =
    Set(
      EvidenceLayer.SinglePosition,
      EvidenceLayer.Line,
      EvidenceLayer.Eval,
      EvidenceLayer.ThreatPressure,
      EvidenceLayer.MoveMotif,
      EvidenceLayer.MoveTransition,
      EvidenceLayer.Relation,
      EvidenceLayer.StructuralDelta,
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
      lineAwareWitnessKeys(claim, linked).map(EventMemberBinding(_, claim, EventMemberRole.Witness))
    else Nil

  private def recordsFor(refs: List[EvidenceRef], graph: TypedEvidenceGraph): List[EvidenceRecord] =
    refs.flatMap(ref => graph.byId.get(ref.id))

  private def eventBindingsForRecord(claim: ClaimSeed, record: EvidenceRecord): List[EventMemberBinding] =
    record.payload match
      case RelativeCauseFactEvidence(cause) =>
        eventKey(record.ref, cause).toList.map { key =>
          val role =
            if claim.family == ClaimFamily.Evaluation then EventMemberRole.Witness
            else EventMemberRole.CauseOwner
          EventMemberBinding(key, claim, role, cause.proof)
        }
      case MoveVerdictCertificationEvidence(certification) =>
        certification.causes.flatMap(cause => eventKey(record.ref, cause)).map { key =>
          val role =
            if claim.family == ClaimFamily.Evaluation then EventMemberRole.VerdictCarrier
            else EventMemberRole.Witness
          val proof = certification.causes.find(cause =>
            eventKey(record.ref, cause).contains(key)
          ).flatMap(_.proof)
          EventMemberBinding(key, claim, role, proof)
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
        claim.evidence.exists(ref => ref.layer == EvidenceLayer.Relation || ref.layer == EvidenceLayer.MoveMotif)
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
        .filter(_.role == EventMemberRole.CauseOwner)
        .map(_.claim)
        .filterNot(_.family == ClaimFamily.Evaluation)
        .distinctBy(_.id)
        .sortBy(_.id)
        .map(_.id)
    val evaluationClaimIds =
      bindings
        .filter(_.role == EventMemberRole.VerdictCarrier)
        .map(_.claim)
        .filter(_.family == ClaimFamily.Evaluation)
        .distinctBy(_.id)
        .sortBy(_.id)
        .map(_.id)
    val witnessClaimIds =
      bindings
        .filter(_.role == EventMemberRole.Witness)
        .map(_.claim)
        .filterNot(claim => causeClaimIds.contains(claim.id) || evaluationClaimIds.contains(claim.id))
        .distinctBy(_.id)
        .sortBy(_.id)
        .map(_.id)
    val evidence =
      members.flatMap(_.evidence).filter(ref => eventEvidenceLayers.contains(ref.layer)).distinctBy(_.id)
    val proof = mergedProof(bindings.flatMap(_.proof))
    val interactions = clusterInteractions(memberIds, allClaims)
    val relatedSupportClusterIds = supportClustersRelatedTo(memberIds, interactions, supportClusters)
    Option.when(causeClaimIds.nonEmpty && evidence.nonEmpty) {
      ClaimEventCluster(
        id = eventClusterId(key, members),
        kind = clusterKind(key.causeKind),
        causeKind = key.causeKind,
        comparisonKind = key.comparisonKind,
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
        proofBoardAnchors = proof.boardAnchors,
        proofLineEvents = proof.lineEvents,
        proofLineConsequences = proof.lineConsequences,
        proofRelationKinds = proof.relationKinds,
        proofSupportLayers = proof.supportLayers.toSet,
        confidence = members.map(_.confidence).maxBy(confidenceScore),
        salienceDrivers = members.flatMap(_.salience.toList.flatMap(_.drivers)).distinct,
        interactions = interactions
      )
    }

  private def mergedProof(proofs: List[RelativeCauseProof]): RelativeCauseProof =
    RelativeCauseProof(
      boardAnchors = proofs.flatMap(_.boardAnchors).distinct,
      lineEvents = proofs.flatMap(_.lineEvents).distinct,
      lineConsequences = proofs.flatMap(_.lineConsequences).distinct,
      relationKinds = proofs.flatMap(_.relationKinds).distinct,
      supportLayers = proofs.flatMap(_.supportLayers).distinct
    )

  private def eventClusterId(key: EventClusterKey, members: List[ClaimSeed]): String =
    List(
      "claim-event",
      key.comparisonKind.toString,
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
              else interaction.interactionEvidence.filter(ref => eventInteractionEvidenceLayers.contains(ref.layer))
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
