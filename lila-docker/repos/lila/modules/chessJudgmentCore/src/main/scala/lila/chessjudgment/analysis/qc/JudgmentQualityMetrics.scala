package lila.chessjudgment.analysis.qc

import chess.{ Color, File }
import lila.chessjudgment.analysis.assembly.{ JudgmentPacketValidationIssueKind, JudgmentPacketValidationResult, JudgmentPacketValidator }
import lila.chessjudgment.analysis.evaluation.JudgmentThresholds
import lila.chessjudgment.analysis.policy.ClaimTruthPolicy
import lila.chessjudgment.analysis.singlePosition.{ PawnPlayDriver, ThreatDriver, ThreatSeverity }
import lila.chessjudgment.analysis.tactical.TacticalMotifClassifier
import lila.chessjudgment.model.{ Motif, ProbeAdmissionStatus, ProbePurpose, TransitionType }
import lila.chessjudgment.model.structure.AlignmentBand
import lila.chessjudgment.model.judgment.*

enum EvidenceLossExpectation:
  case Expected
  case Secondary
  case Deferred
  case Unexpected

final case class EvidenceLossClassification(
    diagnostic: EvidenceLossDiagnostic,
    expectation: EvidenceLossExpectation
)

object ExpectedEvidenceLossPolicy:
  private val supportOnlyLayers: Set[EvidenceLayer] =
    Set(
      EvidenceLayer.Board,
      EvidenceLayer.SinglePosition,
      EvidenceLayer.Line,
      EvidenceLayer.Eval,
      EvidenceLayer.MoveMotif,
      EvidenceLayer.MoveTransition,
      EvidenceLayer.Relation,
      EvidenceLayer.OpeningContext,
      EvidenceLayer.FeatureAnchor,
      EvidenceLayer.ApplicabilityAssessment,
      EvidenceLayer.ChessIdea,
      EvidenceLayer.Claim
    )

  def classify(report: EvidenceLossReport): List[EvidenceLossClassification] =
    classify(report, None)

  def classify(report: EvidenceLossReport, packet: EvidenceBackedJudgmentPacket): List[EvidenceLossClassification] =
    classify(report, Some(packet))

  private def classify(
      report: EvidenceLossReport,
      packet: Option[EvidenceBackedJudgmentPacket]
  ): List[EvidenceLossClassification] =
    report.diagnostics.map { diagnostic =>
      EvidenceLossClassification(
        diagnostic = diagnostic,
        expectation = expectationFor(diagnostic, packet)
      )
    }

  private def expectationFor(
      diagnostic: EvidenceLossDiagnostic,
      packet: Option[EvidenceBackedJudgmentPacket]
  ): EvidenceLossExpectation =
    diagnostic.reason match
      case EvidenceLossReason.EvidenceAvailableWithoutIdea
          if diagnostic.layer.exists(supportOnlyLayers.contains) =>
        EvidenceLossExpectation.Expected
      case EvidenceLossReason.EvidenceAvailableWithoutIdea
          if packet.exists(packet => isNonProofSignalThreatSupport(diagnostic, packet)) =>
        EvidenceLossExpectation.Expected
      case EvidenceLossReason.EvidenceAvailableWithoutIdea
          if isReferencePawnStructureSupport(diagnostic) =>
        EvidenceLossExpectation.Expected
      case EvidenceLossReason.EvidenceAvailableWithoutIdea
          if packet.exists(packet => isGenericStrategicSupport(diagnostic, packet)) =>
        EvidenceLossExpectation.Expected
      case EvidenceLossReason.EvidenceAvailableWithoutIdea
          if packet.exists(packet => isExpectedCandidateComparisonSupport(diagnostic, packet)) =>
        EvidenceLossExpectation.Expected
      case EvidenceLossReason.EvidenceAvailableWithoutIdea
          if packet.exists(packet => isSecondaryCandidateComparisonSupport(diagnostic, packet)) =>
        EvidenceLossExpectation.Secondary
      case EvidenceLossReason.EvidenceAvailableWithoutIdea
          if packet.exists(packet => isDeferredCandidateComparisonSupport(diagnostic, packet)) =>
        EvidenceLossExpectation.Deferred
      case EvidenceLossReason.IdeaAvailableWithoutClaim
          if packet.exists(packet => isDeferredIdeaWithoutCertifiedClaim(diagnostic, packet)) =>
        EvidenceLossExpectation.Deferred
      case _ =>
        EvidenceLossExpectation.Unexpected

  private def isReferencePawnStructureSupport(diagnostic: EvidenceLossDiagnostic): Boolean =
    diagnostic.layer.contains(EvidenceLayer.PawnStructure) &&
      diagnostic.evidence.exists(ref =>
        ref.scope == EvidenceScope.AfterReferencePosition ||
          ref.scope == EvidenceScope.ReferenceTransition ||
          ref.scope == EvidenceScope.AlternativeTransition ||
          ref.scope == EvidenceScope.BestLine ||
          ref.scope == EvidenceScope.CandidateLine
      )

  private def isNonProofSignalThreatSupport(
      diagnostic: EvidenceLossDiagnostic,
      packet: EvidenceBackedJudgmentPacket
  ): Boolean =
    diagnostic.layer.contains(EvidenceLayer.ThreatPressure) &&
      diagnostic.evidence
        .flatMap(ref => packet.evidenceGraph.byId.get(ref.id))
        .exists {
          case EvidenceRecord(_, ThreatPressureEvidence(_, threats), _) =>
            !threats.isProofSignalDefensivePressure
          case _ =>
            false
        }

  private def isGenericStrategicSupport(
      diagnostic: EvidenceLossDiagnostic,
      packet: EvidenceBackedJudgmentPacket
  ): Boolean =
    diagnostic.layer.contains(EvidenceLayer.Strategic) &&
      diagnostic.evidence
        .flatMap(ref => packet.evidenceGraph.byId.get(ref.id))
        .exists {
          case EvidenceRecord(_, payload @ StrategicFactEvidence(kind, _, _, _), _) =>
            !payload.hasTypedSupport &&
              genericStrategicSupportKind(kind)
          case _ =>
            false
        }

  private def genericStrategicSupportKind(kind: StrategicFactKind): Boolean =
    kind match
      case StrategicFactKind.FileControl | StrategicFactKind.Space | StrategicFactKind.Activity |
          StrategicFactKind.CounterplayRestraint =>
        true
      case _ =>
        false

  private def isExpectedCandidateComparisonSupport(
      diagnostic: EvidenceLossDiagnostic,
      packet: EvidenceBackedJudgmentPacket
  ): Boolean =
    candidateComparisonFor(diagnostic, packet).exists(fact =>
      fact.kind != CandidateComparisonKind.PlayedVsBest &&
        !comparisonRequiresExplanatoryCause(fact)
    )

  private def isDeferredCandidateComparisonSupport(
      diagnostic: EvidenceLossDiagnostic,
      packet: EvidenceBackedJudgmentPacket
  ): Boolean =
    candidateComparisonFor(diagnostic, packet).exists(fact =>
      fact.kind != CandidateComparisonKind.PlayedVsBest &&
        !comparisonTouchesPlayedLine(packet, fact) &&
        comparisonRequiresExplanatoryCause(fact)
    )

  private def isSecondaryCandidateComparisonSupport(
      diagnostic: EvidenceLossDiagnostic,
      packet: EvidenceBackedJudgmentPacket
  ): Boolean =
    candidateComparisonFor(diagnostic, packet).exists(fact =>
      fact.kind != CandidateComparisonKind.PlayedVsBest &&
        comparisonTouchesPlayedLine(packet, fact) &&
        comparisonRequiresExplanatoryCause(fact)
    )

  private def candidateComparisonFor(
      diagnostic: EvidenceLossDiagnostic,
      packet: EvidenceBackedJudgmentPacket
  ): Option[CandidateComparisonFact] =
    diagnostic.evidence
      .filter(_.layer == EvidenceLayer.CandidateComparison)
      .flatMap(ref => packet.evidenceGraph.byId.get(ref.id))
      .collect { case EvidenceRecord(_, CandidateComparisonEvidence(fact), _) => fact }

  private def comparisonTouchesPlayedLine(
      packet: EvidenceBackedJudgmentPacket,
      fact: CandidateComparisonFact
  ): Boolean =
    val playedLines =
      packet.candidateLines.filter(_.role == LineNodeRole.Played).map(_.ref).toSet
    val playedMoves =
      playedLines.map(line => normalizeMove(line.rootMove)) ++
        packet.playedTransition.map(transition => normalizeMove(transition.moveUci)).toSet
    playedLines.contains(fact.referenceLine) ||
      playedLines.contains(fact.candidateLine) ||
      playedMoves.contains(normalizeMove(fact.referenceLine.rootMove)) ||
      playedMoves.contains(normalizeMove(fact.candidateLine.rootMove))

  private def normalizeMove(raw: String): String =
    Option(raw).getOrElse("").trim.toLowerCase

  private def comparisonRequiresExplanatoryCause(fact: CandidateComparisonFact): Boolean =
    comparisonSignificantEnginePreference(fact) && !comparisonPositiveContextAlternative(fact)

  private def comparisonSignificantEnginePreference(fact: CandidateComparisonFact): Boolean =
    fact.comparison.winPercentLossForMover >= JudgmentThresholds.INACCURACY_WP ||
      fact.comparison.candidateWinPercentDeltaForMover >= JudgmentThresholds.PLAYABLE_LOSS_WP ||
      fact.comparison.candidateSet.exists(_.onlyMove)

  private def comparisonPositiveContextAlternative(fact: CandidateComparisonFact): Boolean =
    fact.kind == CandidateComparisonKind.PlayedVsAlternative &&
      fact.comparison.winPercentLossForMover < JudgmentThresholds.INACCURACY_WP &&
      fact.comparison.candidateWinPercentDeltaForMover >= JudgmentThresholds.PLAYABLE_LOSS_WP

  private def isDeferredIdeaWithoutCertifiedClaim(
      diagnostic: EvidenceLossDiagnostic,
      packet: EvidenceBackedJudgmentPacket
  ): Boolean =
    packet.claims.exists(claim =>
      claim.idea.exists(_.id == diagnostic.subjectId) &&
        claim.supportStatus.exists(_.status == ClaimSupportStatus.Deferred)
    )

final case class GraphLossMetrics(
    totalRegistered: Int,
    promotedToIdea: Int,
    promotedToClaim: Int,
    unpromoted: Int,
    secondaryLoss: Int,
    deferredLoss: Int,
    unexpectedLoss: Int,
    coverage: List[EvidenceLayerCoverage]
)

object GraphLossMetrics:
  def from(report: EvidenceLossReport, classifications: List[EvidenceLossClassification]): GraphLossMetrics =
    val totalRegistered = report.coverage.map(_.registered).sum
    val promotedToIdea = report.coverage.map(_.promotedToIdea).sum
    val promotedToClaim = report.coverage.map(_.promotedToClaim).sum
    val unpromoted =
      classifications.count(_.diagnostic.reason == EvidenceLossReason.EvidenceAvailableWithoutIdea)
    val unexpectedLoss =
      classifications.count(_.expectation == EvidenceLossExpectation.Unexpected)
    val deferredLoss =
      classifications.count(_.expectation == EvidenceLossExpectation.Deferred)
    val secondaryLoss =
      classifications.count(_.expectation == EvidenceLossExpectation.Secondary)
    GraphLossMetrics(
      totalRegistered = totalRegistered,
      promotedToIdea = promotedToIdea,
      promotedToClaim = promotedToClaim,
      unpromoted = unpromoted,
      secondaryLoss = secondaryLoss,
      deferredLoss = deferredLoss,
      unexpectedLoss = unexpectedLoss,
      coverage = report.coverage
    )

final case class ClaimPromotionMetrics(
    ideas: Int,
    claims: Int,
    claimsWithEngineComparison: Int,
    claimPromotionRate: Double
)

object ClaimPromotionMetrics:
  def from(packet: EvidenceBackedJudgmentPacket): ClaimPromotionMetrics =
    val ideas = packet.ideas.size
    val claims = packet.claims.size
    ClaimPromotionMetrics(
      ideas = ideas,
      claims = claims,
      claimsWithEngineComparison = packet.claims.count(_.engineComparison.nonEmpty),
      claimPromotionRate = if ideas == 0 then 0d else claims.toDouble / ideas.toDouble
    )

enum CandidateComparisonFailureClass:
  case NoFailure
  case LowSignalEnginePreference
  case SecondaryContextGap
  case IncompleteCauseCoverage
  case MissingEvidence
  case UnboundEvidence
  case FailedCauseTemplate
  case UnknownChessPattern

enum CandidateComparisonFailureReason:
  case LowSignalEnginePreference
  case MissingLineEvidence
  case MissingEvalEvidence
  case NoCauseGenerated
  case GeneratedOnlyUnexplained
  case TacticalEvidenceUnbound
  case DefensiveEvidenceUnbound
  case ConversionEvidenceUnbound
  case StrategicEvidenceUnbound
  case LatentTacticalEvidence
  case LatentMaterialEvidence
  case LatentStrategicEvidence
  case TacticalEvidenceBelowThreshold
  case MaterialEvidenceBelowThreshold
  case StrategicEvidenceBelowThreshold
  case TacticalSignalNeedsWidthDepth
  case StrategicContextBelowSignalFloor
  case StrategicEvidenceBelowCauseThreshold
  case PrimaryStrategicNearThresholdUnderbinding
  case PrimaryStrategicSignalNeedsWidthDepth
  case ContextAlternativeStrategicNearThreshold
  case LowSignalTacticalContext
  case LowSignalMaterialContext
  case LowSignalStrategicContext
  case LowDepthGeneratedCause
  case ContextAlternativeComparison
  case MissingExpectedCause
  case NoSpecificEvidence
  case UnknownPattern

enum CandidateComparisonSignificanceReason:
  case PlayedLoss
  case CandidateImproves
  case OnlyMove

enum CandidateComparisonLowSignalReason:
  case QuietNoDirection
  case BelowSignalFloor
  case BelowBindingThreshold
  case NearThresholdPrimaryPlayed
  case NearThresholdContextAlternative
  case ContextAlternative

enum CandidateComparisonDedupeClass:
  case Unique
  case DuplicateEpisode

enum LocalConcreteClaimKind:
  case DefensiveThreat
  case TacticalLine
  case TacticalEngineContext
  case ConversionContext
  case MaterialContext
  case ConcreteContext

final case class LocalConcreteClaimDiagnostic(
    id: String,
    family: ClaimFamily,
    subject: IdeaSubject,
    primaryLine: Option[LineNodeRef],
    subjectMove: Option[String],
    scope: EvidenceScope,
    localKind: LocalConcreteClaimKind,
    evidenceLayers: Set[EvidenceLayer],
    evidenceIds: List[String],
    engineVerdict: Option[MoveChoiceVerdict],
    engineWinPercentLossForMover: Option[Double],
    threatSeverity: Option[ThreatSeverity],
    threatCount: Option[Int],
    threatDefenseRequired: Option[Boolean],
    threatProphylaxisNeeded: Option[Boolean],
    threatOnlyDefense: Option[String],
    threatMaxWinPercentLossIfIgnored: Option[Double],
    threatPrimaryDriver: Option[ThreatDriver],
    threatInsufficientData: Option[Boolean],
    nearbyEventClusterIds: List[String]
)

final case class CandidateLineDiagnostic(
    id: String,
    rootMove: String,
    role: LineNodeRole,
    rank: Int,
    moves: List[String],
    whitePovEvalCp: Option[Int],
    mate: Option[Int],
    depth: Option[Int]
)

final case class EvidenceLayerNeighborhood(
    directLayers: Set[EvidenceLayer],
    directLayerCounts: Map[EvidenceLayer, Int],
    parentLayerCounts: Map[EvidenceLayer, Int]
)

final case class ComparisonEvidenceLayerDiagnostics(
    reference: EvidenceLayerNeighborhood,
    candidate: EvidenceLayerNeighborhood
)

private final case class CandidateComparisonEvidenceNeighborhood(
    referenceRecords: List[EvidenceRecord],
    candidateRecords: List[EvidenceRecord]
)

final case class CandidateCauseDecisionTrace(
    badLoss: Boolean,
    tacticalLoss: Boolean,
    majorLoss: Boolean,
    candidateBetter: Boolean,
    requiresExplanatoryCause: Boolean,
    positiveContextAlternative: Boolean,
    referenceForcing: Boolean,
    candidateForcing: Boolean,
    referenceCauseEligibleTactical: Boolean,
    candidateCauseEligibleTactical: Boolean,
    referenceConcreteLine: Boolean,
    candidateConcreteLine: Boolean,
    candidateTacticalRefutationBridge: Boolean,
    referenceTacticalRisk: Boolean,
    hasOnlyDefense: Boolean,
    hasThreatResource: Boolean,
    referenceProphylacticResource: Boolean,
    referenceKingStepResource: Boolean,
    candidateKingStepResource: Boolean,
    referenceCastlingResource: Boolean,
    candidateCastlingResource: Boolean,
    referencePreventivePawnResource: Boolean,
    candidatePreventivePawnResource: Boolean,
    hasConversionWindow: Boolean,
    referenceConversionWindow: Boolean,
    candidateConversionWindow: Boolean,
    referenceStructuralTargetRelease: Boolean,
    referenceReleasedTargets: List[String],
    candidateReleasedTargets: List[String],
    referenceCreatedTargets: List[String],
    candidateCreatedTargets: List[String],
    referenceStructuralImprovement: Boolean,
    candidateStructuralImprovement: Boolean,
    candidatePawnStructureImprovement: Boolean,
    candidateTargetPressureGain: Boolean,
    candidateCenterControlGain: Boolean,
    candidateDevelopmentActivation: Boolean,
    candidatePieceActivityGain: Boolean,
    sameDestinationCaptureChoice: Boolean,
    referenceStructuralSignals: List[String],
    candidateStructuralSignals: List[String],
    candidatePawnStructureSignals: List[String],
    referenceStrategicImprovementScore: Int,
    candidateStrategicImprovementScore: Int,
    referenceStrategicImprovementOverCandidate: Boolean,
    candidatePlanEvidence: Boolean,
    candidateStrategicEvidence: Boolean,
    candidateStrategicConcessionEvidence: Boolean,
    candidateStrongStrategicConcessionEvidence: Boolean,
    candidateKingHomeStepConcession: Boolean,
    materialSwingEvidence: Boolean,
    referenceMaterialNetCp: Int,
    candidateMaterialNetCp: Int,
    referenceMaterialMaxGainCp: Int,
    candidateMaterialMaxGainCp: Int,
    referenceMaterialPromotionGainCp: Int,
    candidateMaterialPromotionGainCp: Int,
    referenceMaterialRecapture: Boolean,
    candidateMaterialRecapture: Boolean,
    referenceMaterialRecovery: Boolean,
    candidateMaterialRecovery: Boolean,
    referenceMaterialComplete: Boolean,
    candidateMaterialComplete: Boolean,
    referenceRelationKinds: List[RelationFactKind],
    candidateRelationKinds: List[RelationFactKind],
    relationKinds: List[RelationFactKind],
    referenceMotifs: List[String],
    candidateMotifs: List[String]
)

final case class CandidateComparisonDiagnostic(
    id: String,
    comparisonFingerprint: String,
    dedupeKey: String,
    dedupeClass: CandidateComparisonDedupeClass,
    subjectBinding: SubjectBindingClass,
    comparisonKind: CandidateComparisonKind,
    referenceLine: CandidateLineDiagnostic,
    candidateLine: CandidateLineDiagnostic,
    verdict: MoveChoiceVerdict,
    mover: String,
    cpLossForMover: Int,
    candidateDeltaForMover: Int,
    winPercentLossForMover: Double,
    candidateWinPercentDeltaForMover: Double,
    causeKinds: List[RelativeCauseKind],
    causeSupport: List[CandidateCauseSupportDiagnostic],
    failedCauseCandidates: List[RelativeCauseKind],
    significanceReasons: List[CandidateComparisonSignificanceReason],
    lowSignalReasons: List[CandidateComparisonLowSignalReason],
    comparisonConfidence: EvidenceConfidence,
    causeConfidences: List[EvidenceConfidence],
    hasLowDepthCause: Boolean,
    hasUnexplainedEngineGap: Boolean,
    hasSecondaryContextEngineGap: Boolean,
    evidenceLayers: ComparisonEvidenceLayerDiagnostics,
    decisionTrace: CandidateCauseDecisionTrace,
    failureClass: CandidateComparisonFailureClass,
    failureReasons: List[CandidateComparisonFailureReason]
)

final case class CandidateCauseSupportDiagnostic(
    id: String,
    kind: RelativeCauseKind,
    parentEvidenceIds: List[String],
    parentLayers: List[EvidenceLayer],
    parentLayerSignature: String,
    semanticSupportKinds: List[String],
    semanticSupportSignature: String,
    proofHasTypedDepth: Boolean,
    proofBoardAnchors: List[BoardAnchorKind],
    proofLineEvents: List[LineEventKind],
    proofLineConsequences: List[LineConsequenceKind],
    proofRelationKinds: List[RelationFactKind],
    proofSupportLayers: List[EvidenceLayer]
)

final case class OpeningApplicabilityDiagnostic(
    id: String,
    applicability: FeatureApplicability,
    status: ApplicabilityStatus,
    observedThemes: List[OpeningTheme],
    supportedThemes: List[OpeningTheme],
    unverifiedPriorThemes: List[OpeningTheme],
    observedOnlyThemes: List[OpeningTheme],
    anchorSourceLayers: List[EvidenceLayer],
    anchorSignals: List[FeatureAnchorSignal],
    supportedAnchorSourceLayers: List[EvidenceLayer],
    supportedAnchorSignals: List[FeatureAnchorSignal],
    internalAnchorAligned: Boolean
)

object CandidateComparisonDiagnostic:
  def fromPacket(packet: EvidenceBackedJudgmentPacket): List[CandidateComparisonDiagnostic] =
    val playedMoves = JudgmentSubjectBinding.packetPlayedMoves(packet)
    val causeRecords = packet.evidenceGraph.records.collect {
      case record @ EvidenceRecord(_, RelativeCauseFactEvidence(cause), _) => record -> cause
    }
    val diagnostics = packet.evidenceGraph.records.collect {
      case record @ EvidenceRecord(ref, CandidateComparisonEvidence(fact), _) =>
        val neighborhood = comparisonNeighborhood(packet, fact)
        val matchingCauses =
          causeRecords.collect {
            case (_, cause) if causeMatchesComparison(cause, fact) => cause
          }
        val matchingCauseRecords =
          causeRecords.collect {
            case (record, cause) if causeMatchesComparison(cause, fact) => record
          }
        val evidenceLayers =
          ComparisonEvidenceLayerDiagnostics(
            reference = evidenceNeighborhood(packet.evidenceGraph, neighborhood.referenceRecords),
            candidate = evidenceNeighborhood(packet.evidenceGraph, neighborhood.candidateRecords)
          )
        val causeKinds = matchingCauses.map(_.kind).distinct
        val causeSupport = causeSupportDiagnostics(matchingCauseRecords, packet.evidenceGraph)
        val trace = decisionTraceFor(fact, neighborhood.referenceRecords, neighborhood.candidateRecords)
        val failedCauseCandidates = failedCauseCandidatesFor(packet, fact, causeKinds, trace)
        val secondaryContextGap =
          requiresExplanatoryCause(fact) && causeKinds.isEmpty && contextAlternativeComparison(fact)
        val referenceLine = lineDiagnostic(packet, fact.referenceLine)
        val candidateLine = lineDiagnostic(packet, fact.candidateLine)
        val hasLowDepthCause =
          lowDepthCauseShouldBeAudited(fact, causeKinds) &&
            matchingCauseRecords.nonEmpty &&
            comparisonLowDepth(referenceLine, candidateLine)
        CandidateComparisonDiagnostic(
          id = ref.id,
          comparisonFingerprint = comparisonFingerprint(packet, fact),
          dedupeKey = comparisonDedupeKey(packet, fact),
          dedupeClass = CandidateComparisonDedupeClass.Unique,
          subjectBinding = JudgmentSubjectBinding.recordBinding(record, playedMoves),
          comparisonKind = fact.kind,
          referenceLine = referenceLine,
          candidateLine = candidateLine,
          verdict = fact.comparison.verdict,
          mover = fact.comparison.mover.name,
          cpLossForMover = fact.comparison.cpLossForMover,
          candidateDeltaForMover = fact.comparison.candidateDeltaForMover,
          winPercentLossForMover = fact.comparison.winPercentLossForMover,
          candidateWinPercentDeltaForMover = fact.comparison.candidateWinPercentDeltaForMover,
          causeKinds = causeKinds,
          causeSupport = causeSupport,
          failedCauseCandidates = failedCauseCandidates,
          significanceReasons = significanceReasons(fact),
          lowSignalReasons = lowSignalReasons(fact),
          comparisonConfidence = ref.confidence,
          causeConfidences = matchingCauseRecords.map(_.ref.confidence).distinct,
          hasLowDepthCause = hasLowDepthCause,
          hasUnexplainedEngineGap = requiresExplanatoryCause(fact) && causeKinds.isEmpty && !secondaryContextGap,
          hasSecondaryContextEngineGap = secondaryContextGap,
          evidenceLayers = evidenceLayers,
          decisionTrace = trace,
          failureClass = classifyFailure(fact, causeKinds, evidenceLayers, trace, failedCauseCandidates),
          failureReasons = failureReasonsFor(fact, causeKinds, evidenceLayers, trace, failedCauseCandidates, hasLowDepthCause)
        )
    }
    val duplicateKeys =
      diagnostics.groupMapReduce(_.dedupeKey)(_ => 1)(_ + _).collect { case (key, count) if count > 1 => key }.toSet
    diagnostics.map(diagnostic =>
      if duplicateKeys.contains(diagnostic.dedupeKey) then
        diagnostic.copy(dedupeClass = CandidateComparisonDedupeClass.DuplicateEpisode)
      else diagnostic
    )

  private def causeSupportDiagnostics(
      causeRecords: List[EvidenceRecord],
      graph: TypedEvidenceGraph
  ): List[CandidateCauseSupportDiagnostic] =
    causeRecords.collect { case record @ EvidenceRecord(ref, RelativeCauseFactEvidence(cause), _) =>
      val parentRecords = record.parents.flatMap(parent => graph.byId.get(parent.id))
      val parentLayers = parentRecords.map(_.ref.layer).distinctBy(_.ordinal).sortBy(_.ordinal)
      val proof = cause.proof.getOrElse(RelativeCauseProof())
      val semanticKinds = semanticSupportKinds(cause.kind, parentRecords, proof)
      CandidateCauseSupportDiagnostic(
        id = ref.id,
        kind = cause.kind,
        parentEvidenceIds = record.parents.map(_.id).distinct.sorted,
        parentLayers = parentLayers,
        parentLayerSignature = parentLayers.map(_.toString).mkString("+"),
        semanticSupportKinds = semanticKinds,
        semanticSupportSignature = semanticKinds.mkString("+"),
        proofHasTypedDepth = proof.hasTypedDepth,
        proofBoardAnchors = proof.boardAnchors,
        proofLineEvents = proof.lineEvents,
        proofLineConsequences = proof.lineConsequences,
        proofRelationKinds = proof.relationKinds,
        proofSupportLayers = proof.supportLayers
      )
    }

  private def semanticSupportKinds(
      kind: RelativeCauseKind,
      parentRecords: List[EvidenceRecord],
      proof: RelativeCauseProof
  ): List[String] =
    kind match
      case RelativeCauseKind.OnlyMoveNecessity =>
        val kinds = onlyMoveNecessitySupportKinds(parentRecords) ++ typedProofSupportKinds(proof)
        if kinds.nonEmpty then kinds.distinct.sorted else List("GenericComparisonOnly")
      case RelativeCauseKind.OnlyDefenseNecessity | RelativeCauseKind.DefensiveResource =>
        val kinds = defensiveResourceSupportKinds(parentRecords) ++ typedProofSupportKinds(proof)
        if kinds.nonEmpty then kinds.distinct.sorted else List("GenericComparisonOnly")
      case RelativeCauseKind.MaterialSwing | RelativeCauseKind.SacrificeCompensation =>
        val kinds = materialSupportKinds(parentRecords) ++ typedProofSupportKinds(proof)
        if kinds.nonEmpty then kinds.distinct.sorted else List("GenericComparisonOnly")
      case causeKind if tacticalCause(causeKind) =>
        val kinds = tacticalSupportKinds(causeKind, parentRecords) ++ typedProofSupportKinds(proof)
        if kinds.nonEmpty then kinds.distinct.sorted else List("GenericComparisonOnly")
      case RelativeCauseKind.CastlingRightsConcession =>
        val kinds = castlingRightsConcessionSupportKinds(parentRecords) ++ typedProofSupportKinds(proof)
        if kinds.nonEmpty then kinds.distinct.sorted else List("GenericComparisonOnly")
      case causeKind if strategicCause(causeKind) =>
        val kinds = strategicSupportKinds(parentRecords) ++ typedProofSupportKinds(proof)
        if kinds.nonEmpty then kinds.distinct.sorted else List("GenericComparisonOnly")
      case _ =>
        Nil

  private def typedProofSupportKinds(proof: RelativeCauseProof): List[String] =
    proof.boardAnchors.map(kind => s"ProofBoardAnchor:$kind") ++
      proof.lineEvents.map(kind => s"ProofLineEvent:$kind") ++
      proof.lineConsequences.map(kind => s"ProofLineConsequence:$kind") ++
      proof.relationKinds.map(kind => s"ProofRelation:$kind")

  private def onlyMoveNecessitySupportKinds(parentRecords: List[EvidenceRecord]): List[String] =
    val comparisonKinds = parentRecords.collect { case EvidenceRecord(_, CandidateComparisonEvidence(fact), _) =>
      List(
        Option.when(fact.kind == CandidateComparisonKind.BestVsSecond)("BestVsSecond"),
        Option.when(fact.comparison.candidateSet.exists(_.onlyMove))("OnlyMoveGap"),
        fact.comparison.candidateSet
          .flatMap(_.bestToSecondWinPercentGapForMover)
          .map(gap => s"BestSecondWinPercentGap:${winPercentGapBand(gap)}"),
        fact.comparison.candidateSet
          .flatMap(_.bestToSecondGapForMover)
          .map(gap => s"BestSecondCpGap:${cpGapBand(gap)}"),
        Option.when(fact.comparison.candidateSet.exists(_.candidateCount >= 3))("MultiCandidateSet")
      ).flatten
    }.flatten
    val lineKinds =
      List(
        Option.when(parentRecords.exists { case EvidenceRecord(_, _: LineFactEvidence, _) => true; case _ => false })(
          "LineBacked"
        ),
        Option.when(parentRecords.exists { case EvidenceRecord(_, EvalFactEvidence(_, _, _, depth), _) => depth > 0; case _ => false })(
          "EngineEvalBacked"
        )
      ).flatten
    (comparisonKinds ++ lineKinds).distinct

  private def winPercentGapBand(gap: Double): String =
    if gap >= JudgmentThresholds.BLUNDER_WP then "blunder"
    else if gap >= JudgmentThresholds.ONLY_MOVE_GAP_WP then "only-move"
    else if gap >= JudgmentThresholds.INACCURACY_WP then "inaccuracy"
    else "playable"

  private def cpGapBand(gap: Int): String =
    val abs = gap.abs
    if abs >= 300 then "300+"
    else if abs >= 150 then "150+"
    else if abs >= 75 then "75+"
    else "small"

  private def defensiveResourceSupportKinds(parentRecords: List[EvidenceRecord]): List[String] =
    List(
      Option.when(parentRecords.exists {
        case EvidenceRecord(_, EvalFactEvidence(_, _, mate, _), _) =>
          mate.nonEmpty
        case EvidenceRecord(_, RelationFactEvidence(kind, _, _, lineMoves, _), _) =>
          lineMoves.nonEmpty && TacticalMotifClassifier.isRiskRelation(kind)
        case _ =>
          false
      })("MateOrTacticalRisk"),
      Option.when(parentRecords.exists {
        case EvidenceRecord(_, ThreatPressureEvidence(_, threats), _) =>
          !threats.insufficientData && threats.defense.onlyDefense.nonEmpty
        case _ =>
          false
      })("OnlyDefense"),
      Option.when(parentRecords.exists {
        case EvidenceRecord(_, ThreatPressureEvidence(_, threats), _) =>
          !threats.insufficientData && threats.defenseRequired
        case _ =>
          false
      })("ThreatDefenseRequired"),
      Option.when(parentRecords.exists {
        case EvidenceRecord(_, ThreatPressureEvidence(_, threats), _) =>
          !threats.insufficientData && threats.maxWinPercentLossIfIgnored.exists(_ >= JudgmentThresholds.MATERIAL_THREAT_WP)
        case _ =>
          false
      })("MaterialThreatIfIgnored"),
      Option.when(hasProphylacticResource(parentRecords))("Prophylaxis"),
      Option.when(hasKingStepResource(parentRecords))("KingStep"),
      Option.when(hasCastlingResource(parentRecords))("Castling"),
      Option.when(hasPreventivePawnResource(parentRecords))("PreventivePawn")
    ).flatten

  private def materialSupportKinds(parentRecords: List[EvidenceRecord]): List[String] =
    parentRecords.collect { case EvidenceRecord(_, payload: LineFactEvidence, _) =>
      payload.consequenceProfile.proofSignalKinds.map(kind => s"LineConsequence:$kind")
    }.flatten

  private def strategicSupportKinds(parentRecords: List[EvidenceRecord]): List[String] =
    val structuralKinds = parentRecords.collect { case EvidenceRecord(_, StructuralDeltaEvidence(delta), _) =>
      List(
        Option.when(delta.developmentMoves.nonEmpty)("DevelopmentChoice"),
        Option.when(delta.targetPressureDelta > 0 || delta.createdTargetPressure.nonEmpty)("TargetPressure"),
        Option.when(delta.centerControlDelta > 0)("CenterControl"),
        Option.when(delta.mobilityDelta > 0)("Mobility"),
        Option.when(delta.lineUnlockDelta > 0)("LineUnlock"),
        Option.when(delta.fileAccessDelta > 0 || delta.fileOccupation.nonEmpty)("FileAccess"),
        Option.when(delta.createdTension.nonEmpty || delta.pawnTensionDelta > 0)("PawnTension")
      ).flatten
    }.flatten
    val pawnStructureKinds = parentRecords.collect { case EvidenceRecord(_, PawnStructureFactEvidence(_, alignment, pawnPlay), _) =>
      List(
        Option.when(alignment.exists(alignment => alignment.band == AlignmentBand.OnBook || alignment.band == AlignmentBand.Playable))(
          "PawnStructureAlignment"
        ),
        Option.when(pawnPlay.exists(_.primaryDriver != PawnPlayDriver.Quiet))("PawnPlayDriver")
      ).flatten
    }.flatten
    val lineKinds =
      List(Option.when(hasDevelopmentChoice(parentRecords) && hasCastlingLine(parentRecords))("CastlingPath")).flatten
    (structuralKinds ++ pawnStructureKinds ++ lineKinds).distinct

  private def castlingRightsConcessionSupportKinds(parentRecords: List[EvidenceRecord]): List[String] =
    List(
      Option.when(hasKingStepResource(parentRecords))("KingStepForfeitsCastling"),
      Option.when(hasCastlingResource(parentRecords))("CastlingResource")
    ).flatten

  private def hasDevelopmentChoice(parentRecords: List[EvidenceRecord]): Boolean =
    parentRecords.exists {
      case EvidenceRecord(_, StructuralDeltaEvidence(delta), _) => delta.developmentMoves.nonEmpty
      case _                                                    => false
    }

  private def hasCastlingLine(parentRecords: List[EvidenceRecord]): Boolean =
    parentRecords.exists {
      case EvidenceRecord(_, payload: LineFactEvidence, _) =>
        payload.lineEventKinds.contains(LineEventKind.Castling)
      case _ =>
        false
    }

  private def tacticalSupportKinds(
      kind: RelativeCauseKind,
      parentRecords: List[EvidenceRecord]
  ): List[String] =
    (
      tacticalRelationSupportKinds(parentRecords) ++
        tacticalMotifSupportKinds(parentRecords) ++
        tacticalLineSupportKinds(parentRecords) ++
        tacticalEvalSupportKinds(parentRecords) ++
        tacticalCauseShapeSupportKinds(kind, parentRecords)
    ).distinct

  private def tacticalRelationSupportKinds(parentRecords: List[EvidenceRecord]): List[String] =
    parentRecords.collect { case EvidenceRecord(_, RelationFactEvidence(kind, _, _, lineMoves, _), _) =>
      List(
        Some(s"Relation:$kind"),
        Option.when(lineMoves.nonEmpty && TacticalMotifClassifier.isRiskRelation(kind))("LineBoundRelationRisk"),
        kind match
          case RelationFactKind.DefenderTrade =>
            Some("RecaptureRelation")
          case RelationFactKind.Zwischenzug =>
            Some("TempoRelation")
          case RelationFactKind.DoubleCheck | RelationFactKind.BackRankMate | RelationFactKind.MateNet |
              RelationFactKind.GreekGift =>
            Some("KingForcingRelation")
          case RelationFactKind.StalemateTrap | RelationFactKind.PerpetualCheck =>
            Some("DrawRelation")
          case RelationFactKind.BadPieceLiquidation =>
            Some("ConversionRelation")
          case _ =>
            None
      ).flatten
    }.flatten

  private def tacticalMotifSupportKinds(parentRecords: List[EvidenceRecord]): List[String] =
    parentRecords.collect { case EvidenceRecord(_, MoveMotifEvidence(moveUci, motifs), _) =>
      motifs.flatMap { motif =>
        val rootBound = TacticalMotifClassifier.isRootMoveMotif(moveUci, motif)
        List(
          Option.when(rootBound && TacticalMotifClassifier.isForcing(motif))(s"ForcingMotif:${motifSupportName(motif)}"),
          Option.when(rootBound && TacticalMotifClassifier.isCauseEligible(motif))(s"CauseEligibleMotif:${motifSupportName(motif)}"),
          Option.when(rootBound && TacticalMotifClassifier.isTactical(motif))(s"TacticalMotif:${motifSupportName(motif)}")
        ).flatten
      }
    }.flatten

  private def tacticalLineSupportKinds(parentRecords: List[EvidenceRecord]): List[String] =
    parentRecords.collect { case EvidenceRecord(_, payload: LineFactEvidence, _) =>
      payload.consequenceProfile.proofSignalKinds.map(kind => s"LineConsequence:$kind")
    }.flatten

  private def tacticalEvalSupportKinds(parentRecords: List[EvidenceRecord]): List[String] =
    parentRecords.collect {
      case EvidenceRecord(_, EvalFactEvidence(_, _, mate, _), _) if mate.nonEmpty =>
        "MateLine"
    }

  private def tacticalCauseShapeSupportKinds(
      kind: RelativeCauseKind,
      parentRecords: List[EvidenceRecord]
  ): List[String] =
    kind match
      case RelativeCauseKind.WrongRecapturer
          if parentRecords.exists {
            case EvidenceRecord(_, MoveMotifEvidence(_, motifs), _) =>
              motifs.exists {
                case m: Motif.Capture => m.captureType == Motif.CaptureType.Recapture
                case _                => false
              }
            case _ =>
              false
          } =>
        List("RecaptureChoice")
      case RelativeCauseKind.RecaptureRecoveryWindow
          if materialSupportKinds(parentRecords).exists(kind => kind == "RecaptureChain" || kind == "RecoveryWindow") =>
        List("RecoveryWindowShape")
      case _ =>
        Nil

  private def motifSupportName(motif: Motif): String =
    motif match
      case _: Motif.Check                => "Check"
      case _: Motif.DoubleCheck          => "DoubleCheck"
      case _: Motif.BackRankMate         => "BackRankMate"
      case _: Motif.MateNet              => "MateNet"
      case _: Motif.SmotheredMate        => "SmotheredMate"
      case _: Motif.Capture              => "Capture"
      case _: Motif.Zwischenzug          => "Zwischenzug"
      case _: Motif.Fork                 => "Fork"
      case _: Motif.Pin                  => "Pin"
      case _: Motif.Skewer               => "Skewer"
      case _: Motif.DiscoveredAttack     => "DiscoveredAttack"
      case _: Motif.RemovingTheDefender  => "RemovingTheDefender"
      case _: Motif.Deflection           => "Deflection"
      case _: Motif.Decoy                => "Decoy"
      case _: Motif.XRay                 => "XRay"
      case _: Motif.Overloading          => "Overloading"
      case _: Motif.Interference         => "Interference"
      case _: Motif.Clearance            => "Clearance"
      case _: Motif.TrappedPiece         => "TrappedPiece"
      case _: Motif.PawnPromotion        => "PawnPromotion"
      case _: Motif.PassedPawnPush       => "PassedPawnPush"
      case _: Motif.StalemateThreat      => "StalemateThreat"
      case _: Motif.WeakBackRank         => "WeakBackRank"
      case other                         => other.getClass.getSimpleName.replace("$", "")

  private def causeMatchesComparison(
      cause: RelativeCauseFact,
      fact: CandidateComparisonFact
  ): Boolean =
    cause.comparisonKind == fact.kind &&
      cause.referenceLine == fact.referenceLine &&
      cause.candidateLine == fact.candidateLine

  private def lineDiagnostic(
      packet: EvidenceBackedJudgmentPacket,
      line: LineNodeRef
  ): CandidateLineDiagnostic =
    val node = packet.candidateLines.find(_.ref == line)
    CandidateLineDiagnostic(
      id = line.id,
      rootMove = line.rootMove,
      role = line.role,
      rank = line.rank,
      moves = node.map(_.line.moves).getOrElse(Nil),
      whitePovEvalCp = node.map(_.whitePovEvalCp),
      mate = node.flatMap(_.mate),
      depth = node.map(_.depth)
    )

  private def comparisonLowDepth(
      reference: CandidateLineDiagnostic,
      candidate: CandidateLineDiagnostic
  ): Boolean =
    List(reference, candidate).exists(line =>
      !JudgmentThresholds.engineBackedByDepth(line.depth.getOrElse(0), line.mate)
    )

  private def comparisonFingerprint(
      packet: EvidenceBackedJudgmentPacket,
      fact: CandidateComparisonFact
  ): String =
    val reference = lineDiagnostic(packet, fact.referenceLine)
    val candidate = lineDiagnostic(packet, fact.candidateLine)
    List(
      packet.positions.find(_.role == PositionNodeRole.Before).map(_.ref.fen).getOrElse(""),
      fact.kind.toString,
      fact.referenceLine.rootMove,
      fact.candidateLine.rootMove,
      reference.whitePovEvalCp.map(_.toString).getOrElse(""),
      reference.depth.map(_.toString).getOrElse(""),
      candidate.whitePovEvalCp.map(_.toString).getOrElse(""),
      candidate.depth.map(_.toString).getOrElse("")
    ).mkString("|")

  private def comparisonDedupeKey(
      packet: EvidenceBackedJudgmentPacket,
      fact: CandidateComparisonFact
  ): String =
    val reference = lineDiagnostic(packet, fact.referenceLine)
    val candidate = lineDiagnostic(packet, fact.candidateLine)
    List(
      packet.positions.find(_.role == PositionNodeRole.Before).map(_.ref.fen).getOrElse(""),
      fact.referenceLine.rootMove,
      fact.candidateLine.rootMove,
      reference.whitePovEvalCp.map(_.toString).getOrElse(""),
      reference.depth.map(_.toString).getOrElse(""),
      candidate.whitePovEvalCp.map(_.toString).getOrElse(""),
      candidate.depth.map(_.toString).getOrElse("")
    ).mkString("|")

  private def evidenceNeighborhood(
      graph: TypedEvidenceGraph,
      records: List[EvidenceRecord]
  ): EvidenceLayerNeighborhood =
    val directLayerCounts = layerCounts(records.map(_.ref.layer))
    val parentLayers =
      records
        .flatMap(_.parents)
        .map(parent => graph.byId.get(parent.id).map(_.ref.layer).getOrElse(parent.layer))
        .filter(causeInputLayer)
    EvidenceLayerNeighborhood(
      directLayers = directLayerCounts.keySet,
      directLayerCounts = directLayerCounts,
      parentLayerCounts = layerCounts(parentLayers)
    )

  private def comparisonNeighborhood(
      packet: EvidenceBackedJudgmentPacket,
      fact: CandidateComparisonFact
  ): CandidateComparisonEvidenceNeighborhood =
    val referenceTransition = transitionForLine(packet, fact.referenceLine)
    val candidateTransition = transitionForLine(packet, fact.candidateLine)
    CandidateComparisonEvidenceNeighborhood(
      referenceRecords =
        (
          recordsForLineEndpoint(packet, fact.referenceLine) ++
            recordsForTransition(packet, referenceTransition) ++
            recordsForAfterPosition(packet, referenceTransition)
        ).distinctBy(_.ref.id),
      candidateRecords =
        (
          recordsForLineEndpoint(packet, fact.candidateLine) ++
            recordsForTransition(packet, candidateTransition) ++
            recordsForAfterPosition(packet, candidateTransition)
        ).distinctBy(_.ref.id)
    )

  private def recordsForLineEndpoint(
      packet: EvidenceBackedJudgmentPacket,
      line: LineNodeRef
  ): List[EvidenceRecord] =
    packet.evidenceGraph.records.filter(record =>
      endpointLayer(record.ref.layer) &&
        (record.ref.line.contains(line) || payloadReferencesLine(record.payload, line))
    )

  private def recordsForTransition(
      packet: EvidenceBackedJudgmentPacket,
      transition: Option[MoveTransitionEdge]
  ): List[EvidenceRecord] =
    transition.toList.flatMap { edge =>
      packet.evidenceGraph.records.filter(record =>
        transitionLayer(record.ref.layer) &&
          record.ref.position == edge.from &&
          recordMatchesTransition(record, edge)
      )
    }

  private def recordsForAfterPosition(
      packet: EvidenceBackedJudgmentPacket,
      transition: Option[MoveTransitionEdge]
  ): List[EvidenceRecord] =
    transition.toList.flatMap(edge =>
      packet.evidenceGraph.records.filter(record =>
        afterPositionLayer(record.ref.layer) &&
          record.ref.position == edge.to
      )
    )

  private def transitionForLine(
      packet: EvidenceBackedJudgmentPacket,
      line: LineNodeRef
  ): Option[MoveTransitionEdge] =
    packet.transitions.find(edge =>
      edge.moveUci == line.rootMove &&
        line.role == edge.role.lineRole
    )

  private def recordMatchesTransition(record: EvidenceRecord, edge: MoveTransitionEdge): Boolean =
    record.payload match
      case MoveTransitionEvidence(moveUci, _, _) =>
        moveUci == edge.moveUci
      case _: StructuralDeltaEvidence | _: PlanTransitionEvidence =>
        record.ref.line.exists(_.rootMove == edge.moveUci) ||
          record.parents.exists(_.id == edge.evidence.id)
      case _ =>
        false

  private def endpointLayer(layer: EvidenceLayer): Boolean =
    layer match
      case EvidenceLayer.Line | EvidenceLayer.Eval | EvidenceLayer.MoveMotif | EvidenceLayer.Relation |
          EvidenceLayer.StructuralDelta =>
        true
      case _ =>
        false

  private def transitionLayer(layer: EvidenceLayer): Boolean =
    layer match
      case EvidenceLayer.MoveTransition | EvidenceLayer.StructuralDelta | EvidenceLayer.PlanTransition =>
        true
      case _ =>
        false

  private def afterPositionLayer(layer: EvidenceLayer): Boolean =
    layer match
      case EvidenceLayer.Board | EvidenceLayer.SinglePosition | EvidenceLayer.PawnStructure |
          EvidenceLayer.Strategic | EvidenceLayer.ThreatPressure | EvidenceLayer.StructuralDelta | EvidenceLayer.PlanTransition =>
        true
      case _ =>
        false

  private def causeInputLayer(layer: EvidenceLayer): Boolean =
    layer match
      case EvidenceLayer.CandidateComparison | EvidenceLayer.Counterfactual | EvidenceLayer.RelativeAssessment |
          EvidenceLayer.RelativeCause | EvidenceLayer.MoveVerdictCertification | EvidenceLayer.ChessIdea | EvidenceLayer.Claim =>
        false
      case _ =>
        true

  private def payloadReferencesLine(payload: EvidencePayload, line: LineNodeRef): Boolean =
    payload match
      case LineFactEvidence(payloadLine, _, _, _, _, _) =>
        payloadLine == line
      case EvalFactEvidence(payloadLine, _, _, _) =>
        payloadLine == line
      case CandidateComparisonEvidence(fact) =>
        fact.referenceLine == line || fact.candidateLine == line
      case CounterfactualFactEvidence(referenceLine, candidateLine, _) =>
        referenceLine == line || candidateLine == line
      case RelativeAssessmentEvidence(assessment) =>
        assessment.reference.ref == line || assessment.candidate.ref == line
      case RelativeCauseFactEvidence(cause) =>
        cause.referenceLine == line || cause.candidateLine == line
      case MoveVerdictCertificationEvidence(certification) =>
        certification.primaryComparison.referenceLine == line ||
          certification.primaryComparison.candidateLine == line ||
          certification.causes.exists(cause => cause.referenceLine == line || cause.candidateLine == line)
      case _ =>
        false

  private def classifyFailure(
      fact: CandidateComparisonFact,
      causeKinds: List[RelativeCauseKind],
      evidenceLayers: ComparisonEvidenceLayerDiagnostics,
      trace: CandidateCauseDecisionTrace,
      failedCauseCandidates: List[RelativeCauseKind]
  ): CandidateComparisonFailureClass =
    val significantEnginePreference = CandidateComparisonDiagnostic.significantEnginePreference(fact)
    val requiresCause = CandidateComparisonDiagnostic.requiresExplanatoryCause(fact)
    val hasKnownCause =
      causeKinds.nonEmpty
    if !significantEnginePreference && causeKinds.isEmpty && latentComparisonEvidence(trace) then
      CandidateComparisonFailureClass.LowSignalEnginePreference
    else if !significantEnginePreference && causeKinds.isEmpty then
      CandidateComparisonFailureClass.NoFailure
    else if !requiresCause then
      CandidateComparisonFailureClass.NoFailure
    else if hasKnownCause && failedCauseCandidates.nonEmpty then
      CandidateComparisonFailureClass.IncompleteCauseCoverage
    else if hasKnownCause then
      CandidateComparisonFailureClass.NoFailure
    else if contextAlternativeComparison(fact) then
      CandidateComparisonFailureClass.SecondaryContextGap
    else if missingLineOrEval(evidenceLayers) then
      CandidateComparisonFailureClass.MissingEvidence
    else if causeKinds.isEmpty then
      CandidateComparisonFailureClass.FailedCauseTemplate
    else if hasAvailableExplanatoryEvidence(evidenceLayers) then
      CandidateComparisonFailureClass.UnboundEvidence
    else
      CandidateComparisonFailureClass.UnknownChessPattern

  private def missingLineOrEval(evidenceLayers: ComparisonEvidenceLayerDiagnostics): Boolean =
    List(evidenceLayers.reference, evidenceLayers.candidate).exists(neighborhood =>
      !neighborhood.directLayers.contains(EvidenceLayer.Line) ||
        !neighborhood.directLayers.contains(EvidenceLayer.Eval)
    )

  private def hasAvailableExplanatoryEvidence(evidenceLayers: ComparisonEvidenceLayerDiagnostics): Boolean =
    availableLayers(evidenceLayers).exists(explanatoryLayers.contains)

  private def decisionTraceFor(
      fact: CandidateComparisonFact,
      referenceRecords: List[EvidenceRecord],
      candidateRecords: List[EvidenceRecord]
  ): CandidateCauseDecisionTrace =
    val involvedRecords = (referenceRecords ++ candidateRecords).distinctBy(_.ref.id)
    val referenceStrategicScore = strategicImprovementScore(referenceRecords)
    val candidateStrategicScore = strategicImprovementScore(candidateRecords)
    val materialSwing = hasMaterialSwingEvidence(referenceRecords, candidateRecords)
    val candidateRelations = relationKinds(candidateRecords)
    CandidateCauseDecisionTrace(
      badLoss = fact.comparison.winPercentLossForMover >= JudgmentThresholds.INACCURACY_WP,
      tacticalLoss = fact.comparison.winPercentLossForMover >= JudgmentThresholds.SIGNIFICANT_THREAT_WP,
      majorLoss = fact.comparison.winPercentLossForMover >= JudgmentThresholds.MATERIAL_THREAT_WP,
      candidateBetter = fact.comparison.candidateWinPercentDeltaForMover >= JudgmentThresholds.PLAYABLE_LOSS_WP,
      requiresExplanatoryCause = requiresExplanatoryCause(fact),
      positiveContextAlternative = positiveContextAlternative(fact),
      referenceForcing = hasForcingMotif(referenceRecords),
      candidateForcing = hasForcingMotif(candidateRecords),
      referenceCauseEligibleTactical = hasCauseEligibleTacticalMotif(referenceRecords),
      candidateCauseEligibleTactical = hasCauseEligibleTacticalMotif(candidateRecords),
      referenceConcreteLine = hasConcreteLineConsequence(referenceRecords),
      candidateConcreteLine = hasConcreteLineConsequence(candidateRecords),
      candidateTacticalRefutationBridge =
        candidateConcreteTacticalBridge(referenceRecords, relationKinds(referenceRecords)) ||
          candidateConcreteTacticalBridge(candidateRecords, candidateRelations),
      referenceTacticalRisk = referenceHasTacticalRisk(referenceRecords),
      hasOnlyDefense = hasOnlyDefense(involvedRecords),
      hasThreatResource = hasThreatResource(involvedRecords),
      referenceProphylacticResource = hasProphylacticResource(referenceRecords),
      referenceKingStepResource = hasKingStepResource(referenceRecords),
      candidateKingStepResource = hasKingStepResource(candidateRecords),
      referenceCastlingResource = hasCastlingResource(referenceRecords),
      candidateCastlingResource = hasCastlingResource(candidateRecords),
      referencePreventivePawnResource = hasPreventivePawnResource(referenceRecords),
      candidatePreventivePawnResource = hasPreventivePawnResource(candidateRecords),
      hasConversionWindow = hasConversionWindow(involvedRecords),
      referenceConversionWindow = hasConversionWindow(referenceRecords),
      candidateConversionWindow = hasConversionWindow(candidateRecords),
      referenceStructuralTargetRelease = hasStructuralTargetRelease(referenceRecords),
      referenceReleasedTargets = releasedTargetPressure(referenceRecords),
      candidateReleasedTargets = releasedTargetPressure(candidateRecords),
      referenceCreatedTargets = createdTargetPressure(referenceRecords),
      candidateCreatedTargets = createdTargetPressure(candidateRecords),
      referenceStructuralImprovement = hasStructuralImprovementEvidence(referenceRecords),
      candidateStructuralImprovement = hasStructuralImprovementEvidence(candidateRecords),
      candidatePawnStructureImprovement = hasPawnStructureImprovementEvidence(candidateRecords),
      candidateTargetPressureGain = hasTargetPressureGainEvidence(candidateRecords),
      candidateCenterControlGain = hasCenterControlGainEvidence(candidateRecords),
      candidateDevelopmentActivation = hasDevelopmentActivationEvidence(candidateRecords),
      candidatePieceActivityGain = hasPieceActivityGainEvidence(candidateRecords),
      sameDestinationCaptureChoice = sameDestinationCaptureChoice(fact, referenceRecords, candidateRecords),
      referenceStructuralSignals = structuralImprovementSignals(referenceRecords),
      candidateStructuralSignals = structuralImprovementSignals(candidateRecords),
      candidatePawnStructureSignals = pawnStructureImprovementSignals(candidateRecords),
      referenceStrategicImprovementScore = referenceStrategicScore,
      candidateStrategicImprovementScore = candidateStrategicScore,
      referenceStrategicImprovementOverCandidate =
        (referenceStrategicScore >= 3 && referenceStrategicScore >= candidateStrategicScore + 2) ||
          (referenceStrategicScore >= 2 && candidateStrategicScore <= 1),
      candidatePlanEvidence = hasPlanCauseEvidence(candidateRecords),
      candidateStrategicEvidence = hasStrategicCauseEvidence(candidateRecords),
      candidateStrategicConcessionEvidence = hasStrategicConcessionEvidence(candidateRecords),
      candidateStrongStrategicConcessionEvidence = hasStrongStrategicConcessionEvidence(candidateRecords),
      candidateKingHomeStepConcession = hasKingHomeStepConcession(candidateRecords),
      materialSwingEvidence = materialSwing,
      referenceMaterialNetCp = materialBestNet(referenceRecords),
      candidateMaterialNetCp = materialBestNet(candidateRecords),
      referenceMaterialMaxGainCp = materialBestGain(referenceRecords),
      candidateMaterialMaxGainCp = materialBestGain(candidateRecords),
      referenceMaterialPromotionGainCp = materialBestPromotionGain(referenceRecords),
      candidateMaterialPromotionGainCp = materialBestPromotionGain(candidateRecords),
      referenceMaterialRecapture = materialSummaries(referenceRecords).exists(_.hasRecaptureChain),
      candidateMaterialRecapture = materialSummaries(candidateRecords).exists(_.hasRecaptureChain),
      referenceMaterialRecovery = materialSummaries(referenceRecords).exists(_.hasRecoveryWindow),
      candidateMaterialRecovery = materialSummaries(candidateRecords).exists(_.hasRecoveryWindow),
      referenceMaterialComplete = materialSummaries(referenceRecords).forall(_.materialWindowComplete),
      candidateMaterialComplete = materialSummaries(candidateRecords).forall(_.materialWindowComplete),
      referenceRelationKinds = relationKinds(referenceRecords),
      candidateRelationKinds = candidateRelations,
      relationKinds = relationKinds(involvedRecords),
      referenceMotifs = motifNames(referenceRecords),
      candidateMotifs = motifNames(candidateRecords)
    )

  private def failedCauseCandidatesFor(
      packet: EvidenceBackedJudgmentPacket,
      fact: CandidateComparisonFact,
      causeKinds: List[RelativeCauseKind],
      trace: CandidateCauseDecisionTrace
  ): List[RelativeCauseKind] =
    val generated = causeKinds.toSet
    val interactionCovered = interactionCoveredCauseCandidates(packet, fact)
    val primaryPlayedPositive =
      fact.kind == CandidateComparisonKind.PlayedVsBest && trace.candidateBetter
    val primaryPlayedSignificantLoss =
      fact.kind == CandidateComparisonKind.PlayedVsBest && trace.tacticalLoss
    val candidates = List(
      Option.when(fact.kind == CandidateComparisonKind.BestVsSecond && trace.majorLoss)(RelativeCauseKind.OnlyMoveNecessity),
      Option.when(fact.comparison.candidateSet.exists(_.onlyMove))(RelativeCauseKind.OnlyMoveNecessity),
      Option.when(trace.hasThreatResource)(RelativeCauseKind.DefensiveResource),
      Option.when(trace.hasOnlyDefense)(RelativeCauseKind.OnlyDefenseNecessity),
      Option.when(trace.referenceCastlingResource && trace.badLoss && !trace.candidateCastlingResource)(
        RelativeCauseKind.DefensiveResource
      ),
      Option.when(trace.referencePreventivePawnResource && trace.badLoss && !trace.candidatePreventivePawnResource)(
        RelativeCauseKind.DefensiveResource
      ),
      Option.when(
        primaryPlayedSignificantLoss &&
          trace.referencePreventivePawnResource &&
          !trace.candidatePreventivePawnResource
      )(
        RelativeCauseKind.DefensiveResource
      ),
      Option.when(trace.referenceForcing && trace.tacticalLoss)(RelativeCauseKind.MissedTacticalResource),
      Option.when(trace.candidateForcing && trace.tacticalLoss && playedMoveCandidateSideComparison(fact.kind))(
        RelativeCauseKind.TacticalRefutationOfPlayed
      ),
      Option.when(trace.candidateForcing && trace.tacticalLoss && !playedMoveCandidateSideComparison(fact.kind))(
        RelativeCauseKind.CandidateTacticalLiability
      ),
      Option.when(trace.candidateForcing && !trace.badLoss)(RelativeCauseKind.MissedTacticalResource),
      Option.when(trace.referenceCauseEligibleTactical && trace.referenceConcreteLine && trace.tacticalLoss)(
        RelativeCauseKind.MissedTacticalResource
      ),
      Option.when(
        trace.candidateCauseEligibleTactical && trace.candidateConcreteLine && trace.tacticalLoss &&
          playedMoveCandidateSideComparison(fact.kind)
      )(
        RelativeCauseKind.TacticalRefutationOfPlayed
      ),
      Option.when(
        trace.candidateCauseEligibleTactical && trace.candidateConcreteLine && trace.tacticalLoss &&
          !playedMoveCandidateSideComparison(fact.kind)
      )(
        RelativeCauseKind.CandidateTacticalLiability
      ),
      Option.when(trace.candidateCauseEligibleTactical && trace.candidateConcreteLine && trace.candidateBetter)(
        RelativeCauseKind.MissedTacticalResource
      ),
      Option.when(trace.referenceTacticalRisk && trace.candidateBetter)(RelativeCauseKind.DefensiveResource),
      Option.when(trace.referenceKingStepResource && trace.badLoss && !trace.candidateKingStepResource)(
        RelativeCauseKind.DefensiveResource
      ),
      Option.when(
        fact.kind == CandidateComparisonKind.PlayedVsBest &&
          trace.tacticalLoss &&
          trace.referenceProphylacticResource
      )(RelativeCauseKind.DefensiveResource),
      Option.when(trace.referenceConversionWindow && trace.badLoss)(RelativeCauseKind.ConversionMiss),
      Option.when(trace.candidateConversionWindow && trace.candidateBetter)(RelativeCauseKind.ConversionSecured),
      Option.when(
        trace.candidateBetter &&
          (trace.referenceStructuralTargetRelease ||
            trace.candidateStructuralImprovement ||
            trace.candidatePawnStructureImprovement)
      )(RelativeCauseKind.StructuralImprovement),
      Option.when(primaryPlayedPositive && trace.candidateTargetPressureGain)(RelativeCauseKind.TargetPressureGain),
      Option.when(primaryPlayedPositive && trace.candidateCenterControlGain)(RelativeCauseKind.CenterControlGain),
      Option.when(primaryPlayedPositive && trace.candidateDevelopmentActivation)(RelativeCauseKind.DevelopmentActivation),
      Option.when(primaryPlayedPositive && trace.candidatePieceActivityGain)(RelativeCauseKind.PieceActivityGain),
      Option.when(trace.candidatePlanEvidence && trace.candidateBetter)(RelativeCauseKind.PlanImprovement),
      Option.when(trace.candidatePlanEvidence && trace.badLoss)(RelativeCauseKind.PlanContradiction),
      Option.when(
        playedMoveCandidateSideComparison(fact.kind) &&
          trace.tacticalLoss &&
          trace.candidateStrategicImprovementScore >= 3 &&
          trace.candidateStrategicImprovementScore >= trace.referenceStrategicImprovementScore + 2 &&
          trace.candidateTacticalRefutationBridge
      )(RelativeCauseKind.StrategicIdeaRefuted),
      Option.when(
        (trace.badLoss || primaryPlayedSignificantLoss) && trace.candidateKingHomeStepConcession
      )(RelativeCauseKind.CastlingRightsConcession),
      Option.when(
        (trace.badLoss || primaryPlayedSignificantLoss) &&
          !trace.tacticalLoss &&
          trace.candidateStrategicConcessionEvidence
      )(RelativeCauseKind.StrategicConcession),
      Option.when(trace.badLoss && trace.referenceStrategicImprovementOverCandidate)(
        RelativeCauseKind.MissedStrategicImprovement
      ),
      Option.when(
        trace.badLoss &&
          trace.referenceMaterialComplete &&
          (trace.referenceMaterialRecovery || trace.referenceMaterialRecapture) &&
          !(trace.candidateMaterialComplete && (trace.candidateMaterialRecovery || trace.candidateMaterialRecapture))
      )(
        RelativeCauseKind.RecaptureRecoveryWindow
      ),
      Option.when(trace.badLoss && trace.sameDestinationCaptureChoice)(RelativeCauseKind.WrongRecapturer),
      Option.when((trace.majorLoss || primaryPlayedSignificantLoss) && trace.materialSwingEvidence)(RelativeCauseKind.MaterialSwing)
    ).flatten.distinct
    val filteredCandidates = suppressGenericStrategicCompanions(candidates, generated)
    if requiresExplanatoryCause(fact) then
      filteredCandidates.filterNot(kind => generated.contains(kind) || interactionCovered.contains(kind))
    else Nil

  private def interactionCoveredCauseCandidates(
      packet: EvidenceBackedJudgmentPacket,
      fact: CandidateComparisonFact
  ): Set[RelativeCauseKind] =
    val strategicRefutationCovered =
      packet.claimEventClusters.exists(cluster =>
        cluster.comparisonKind == fact.kind &&
          cluster.referenceLine == fact.referenceLine &&
          cluster.candidateLine == fact.candidateLine &&
          cluster.kind == ClaimEventClusterKind.TacticalEvent &&
          cluster.interactions.exists(interaction =>
            interaction.kind == ClaimInteractionKind.TacticalRefutesStrategicPlan ||
              interaction.kind == ClaimInteractionKind.BadVerdictPreservesLocalIdea
          )
      )
    Option.when(strategicRefutationCovered)(RelativeCauseKind.StrategicIdeaRefuted).toSet

  private def suppressGenericStrategicCompanions(
      candidates: List[RelativeCauseKind],
      generated: Set[RelativeCauseKind]
  ): List[RelativeCauseKind] =
    val hasShortTermCause = candidates.exists(shortTermCause) || generated.exists(shortTermCause)
    candidates.filterNot {
      case RelativeCauseKind.StrategicConcession =>
        hasShortTermCause
      case RelativeCauseKind.MissedStrategicImprovement =>
        hasShortTermCause
      case _ =>
        false
    }

  private def failureReasonsFor(
      fact: CandidateComparisonFact,
      causeKinds: List[RelativeCauseKind],
      evidenceLayers: ComparisonEvidenceLayerDiagnostics,
      trace: CandidateCauseDecisionTrace,
      failedCauseCandidates: List[RelativeCauseKind],
      hasLowDepthCause: Boolean
  ): List[CandidateComparisonFailureReason] =
    val missingLine =
      List(evidenceLayers.reference, evidenceLayers.candidate).exists(neighborhood =>
        !neighborhood.directLayers.contains(EvidenceLayer.Line)
      )
    val missingEval =
      List(evidenceLayers.reference, evidenceLayers.candidate).exists(neighborhood =>
        !neighborhood.directLayers.contains(EvidenceLayer.Eval)
      )
    val significantEnginePreference = CandidateComparisonDiagnostic.significantEnginePreference(fact)
    val requiresCause = CandidateComparisonDiagnostic.requiresExplanatoryCause(fact)
    val generatedKnownCause = causeKinds.nonEmpty
    val generatedOnlyUnexplained =
      requiresCause && causeKinds.isEmpty
    val incompleteKnownCause =
      generatedKnownCause && failedCauseCandidates.nonEmpty
    val secondaryContextGap =
      generatedOnlyUnexplained && contextAlternativeComparison(fact)
    if !significantEnginePreference && causeKinds.isEmpty && !latentComparisonEvidence(trace) then
      return Nil
    if generatedKnownCause && !generatedOnlyUnexplained && failedCauseCandidates.isEmpty then
      return Option.when(hasLowDepthCause)(CandidateComparisonFailureReason.LowDepthGeneratedCause).toList
    if positiveContextAlternative(fact) && causeKinds.isEmpty then
      return Nil
    if secondaryContextGap then
      return List(CandidateComparisonFailureReason.ContextAlternativeComparison)
    val reasons = List(
      Option.when(hasLowDepthCause)(CandidateComparisonFailureReason.LowDepthGeneratedCause),
      Option.when(
        requiresCause &&
          causeKinds.isEmpty &&
          contextAlternativeComparison(fact)
      )(CandidateComparisonFailureReason.ContextAlternativeComparison),
      Option.when(!significantEnginePreference)(CandidateComparisonFailureReason.LowSignalEnginePreference),
      latentReason(
        !significantEnginePreference && latentTacticalEvidence(trace),
        tacticalLatentReason(fact, trace)
      ),
      latentReason(
        !significantEnginePreference && trace.materialSwingEvidence,
        CandidateComparisonFailureReason.MaterialEvidenceBelowThreshold
      ),
      Option.when(!significantEnginePreference && latentStrategicEvidence(trace))(
        strategicLatentReason(fact, trace)
      ),
      Option.when(missingLine)(CandidateComparisonFailureReason.MissingLineEvidence),
      Option.when(missingEval)(CandidateComparisonFailureReason.MissingEvalEvidence),
      Option.when(requiresCause && causeKinds.isEmpty)(CandidateComparisonFailureReason.NoCauseGenerated),
      Option.when(generatedOnlyUnexplained)(CandidateComparisonFailureReason.GeneratedOnlyUnexplained),
      Option.when(incompleteKnownCause)(CandidateComparisonFailureReason.MissingExpectedCause),
      Option.when(
        (generatedOnlyUnexplained &&
          (trace.referenceCauseEligibleTactical || trace.candidateCauseEligibleTactical)
        ) || failedCauseCandidates.exists(tacticalCause)
      )(
        CandidateComparisonFailureReason.TacticalEvidenceUnbound
      ),
      Option.when((generatedOnlyUnexplained && trace.hasThreatResource) || failedCauseCandidates.exists(defensiveCause))(
        CandidateComparisonFailureReason.DefensiveEvidenceUnbound
      ),
      Option.when(
        (generatedOnlyUnexplained && (trace.referenceConversionWindow || trace.candidateConversionWindow)) ||
          failedCauseCandidates.exists(conversionCause)
      )(
        CandidateComparisonFailureReason.ConversionEvidenceUnbound
      ),
      Option.when(
        (generatedOnlyUnexplained &&
          fact.kind == CandidateComparisonKind.PlayedVsBest &&
          (trace.candidateStrategicEvidence ||
            trace.candidatePlanEvidence ||
            trace.referenceStructuralTargetRelease ||
            trace.candidateStructuralImprovement ||
            trace.candidatePawnStructureImprovement ||
            trace.candidateTargetPressureGain ||
            trace.candidateCenterControlGain ||
            trace.candidateDevelopmentActivation ||
            trace.candidatePieceActivityGain)
        ) || failedCauseCandidates.exists(strategicCause)
      )(
        CandidateComparisonFailureReason.StrategicEvidenceUnbound
      ),
      Option.when(requiresCause && failedCauseCandidates.isEmpty && !hasAvailableExplanatoryEvidence(evidenceLayers))(
        CandidateComparisonFailureReason.NoSpecificEvidence
      )
    ).flatten.distinct
    if reasons.nonEmpty then reasons else List(CandidateComparisonFailureReason.UnknownPattern)

  private def tacticalCause(kind: RelativeCauseKind): Boolean =
    ClaimEventCluster.kindForCause(kind).contains(ClaimEventClusterKind.TacticalEvent)

  private def shortTermCause(kind: RelativeCauseKind): Boolean =
    kind != RelativeCauseKind.DrawResource && ClaimEventCluster.kindForCause(kind).nonEmpty

  private def defensiveCause(kind: RelativeCauseKind): Boolean =
    ClaimEventCluster.kindForCause(kind).contains(ClaimEventClusterKind.DefensiveEvent)

  private def conversionCause(kind: RelativeCauseKind): Boolean =
    ClaimEventCluster.kindForCause(kind).contains(ClaimEventClusterKind.ConversionEvent)

  private def strategicCause(kind: RelativeCauseKind): Boolean =
    kind match
      case RelativeCauseKind.StructuralImprovement | RelativeCauseKind.CastlingRightsConcession |
          RelativeCauseKind.StrategicConcession | RelativeCauseKind.StrategicIdeaRefuted |
          RelativeCauseKind.MissedStrategicImprovement | RelativeCauseKind.TargetPressureGain |
          RelativeCauseKind.CenterControlGain | RelativeCauseKind.DevelopmentActivation |
          RelativeCauseKind.PieceActivityGain | RelativeCauseKind.PlanImprovement | RelativeCauseKind.PlanContradiction =>
        true
      case _ =>
        false

  private def significantEnginePreference(fact: CandidateComparisonFact): Boolean =
    significanceReasons(fact).nonEmpty

  private def significanceReasons(fact: CandidateComparisonFact): List[CandidateComparisonSignificanceReason] =
    List(
      Option.when(fact.comparison.winPercentLossForMover >= JudgmentThresholds.INACCURACY_WP)(
        CandidateComparisonSignificanceReason.PlayedLoss
      ),
      Option.when(fact.comparison.candidateWinPercentDeltaForMover >= JudgmentThresholds.PLAYABLE_LOSS_WP)(
        CandidateComparisonSignificanceReason.CandidateImproves
      ),
      Option.when(fact.comparison.candidateSet.exists(_.onlyMove))(CandidateComparisonSignificanceReason.OnlyMove)
    ).flatten

  private def requiresExplanatoryCause(fact: CandidateComparisonFact): Boolean =
    significantEnginePreference(fact) && !positiveContextAlternative(fact)

  private def contextAlternativeComparison(fact: CandidateComparisonFact): Boolean =
    fact.kind == CandidateComparisonKind.PlayedVsAlternative ||
      fact.kind == CandidateComparisonKind.ReferenceVsAlternative

  private def positiveContextAlternative(fact: CandidateComparisonFact): Boolean =
    fact.kind == CandidateComparisonKind.PlayedVsAlternative &&
      fact.comparison.winPercentLossForMover < JudgmentThresholds.INACCURACY_WP &&
      fact.comparison.candidateWinPercentDeltaForMover >= JudgmentThresholds.PLAYABLE_LOSS_WP

  private def comparisonSignal(fact: CandidateComparisonFact): Double =
    math.max(
      fact.comparison.winPercentLossForMover,
      math.abs(fact.comparison.candidateWinPercentDeltaForMover)
    )

  private def lowSignalReasons(fact: CandidateComparisonFact): List[CandidateComparisonLowSignalReason] =
    val signal = comparisonSignal(fact)
    List(
      Option.when(!significantEnginePreference(fact) && signal == 0.0)(CandidateComparisonLowSignalReason.QuietNoDirection),
      Option.when(
        !significantEnginePreference(fact) &&
          signal > 0.0 &&
          signal < JudgmentThresholds.ONLY_DEFENSE_TOLERANCE_WP
      )(CandidateComparisonLowSignalReason.BelowSignalFloor),
      Option.when(
        !significantEnginePreference(fact) &&
          signal >= JudgmentThresholds.ONLY_DEFENSE_TOLERANCE_WP &&
          signal < JudgmentThresholds.SIGNIFICANT_THREAT_WP
      )(CandidateComparisonLowSignalReason.BelowBindingThreshold),
      Option.when(
        !significantEnginePreference(fact) &&
          signal >= JudgmentThresholds.SIGNIFICANT_THREAT_WP &&
          fact.kind == CandidateComparisonKind.PlayedVsBest
      )(CandidateComparisonLowSignalReason.NearThresholdPrimaryPlayed),
      Option.when(
        !significantEnginePreference(fact) &&
          signal >= JudgmentThresholds.SIGNIFICANT_THREAT_WP &&
          fact.kind != CandidateComparisonKind.PlayedVsBest
      )(CandidateComparisonLowSignalReason.NearThresholdContextAlternative),
      Option.when(
        fact.kind == CandidateComparisonKind.PlayedVsAlternative ||
          fact.kind == CandidateComparisonKind.ReferenceVsAlternative
      )(CandidateComparisonLowSignalReason.ContextAlternative)
    ).flatten.distinct

  private def latentReason(
      condition: Boolean,
      reason: CandidateComparisonFailureReason
  ): Option[CandidateComparisonFailureReason] =
    Option.when(condition)(reason)

  private def tacticalLatentReason(
      fact: CandidateComparisonFact,
      trace: CandidateCauseDecisionTrace
  ): CandidateComparisonFailureReason =
    if tacticalSignalNeedsWidthDepth(fact, trace) then CandidateComparisonFailureReason.TacticalSignalNeedsWidthDepth
    else if comparisonSignal(fact) >= JudgmentThresholds.IGNORABLE_THREAT_WP then
      CandidateComparisonFailureReason.TacticalEvidenceBelowThreshold
    else CandidateComparisonFailureReason.LowSignalTacticalContext

  private def tacticalSignalNeedsWidthDepth(
      fact: CandidateComparisonFact,
      trace: CandidateCauseDecisionTrace
  ): Boolean =
    comparisonSignal(fact) >= JudgmentThresholds.SIGNIFICANT_THREAT_WP &&
      trace.relationKinds.isEmpty &&
      (trace.referenceCauseEligibleTactical || trace.candidateCauseEligibleTactical) &&
      !trace.referenceConcreteLine &&
      !trace.candidateConcreteLine

  private def strategicLatentReason(
      fact: CandidateComparisonFact,
      trace: CandidateCauseDecisionTrace
  ): CandidateComparisonFailureReason =
    val signal = comparisonSignal(fact)
    if signal < JudgmentThresholds.ONLY_DEFENSE_TOLERANCE_WP then
      CandidateComparisonFailureReason.StrategicContextBelowSignalFloor
    else if signal < JudgmentThresholds.SIGNIFICANT_THREAT_WP then
      CandidateComparisonFailureReason.StrategicEvidenceBelowCauseThreshold
    else if fact.kind == CandidateComparisonKind.PlayedVsBest &&
        (trace.candidateStrategicConcessionEvidence ||
          trace.candidateKingHomeStepConcession ||
          trace.materialSwingEvidence ||
          trace.candidatePlanEvidence)
    then
      CandidateComparisonFailureReason.PrimaryStrategicNearThresholdUnderbinding
    else if fact.kind == CandidateComparisonKind.PlayedVsBest then
      CandidateComparisonFailureReason.PrimaryStrategicSignalNeedsWidthDepth
    else CandidateComparisonFailureReason.ContextAlternativeStrategicNearThreshold

  private def latentTacticalEvidence(trace: CandidateCauseDecisionTrace): Boolean =
    trace.relationKinds.nonEmpty ||
      trace.referenceCauseEligibleTactical ||
      trace.candidateCauseEligibleTactical ||
      trace.referenceTacticalRisk

  private def latentComparisonEvidence(trace: CandidateCauseDecisionTrace): Boolean =
    latentTacticalEvidence(trace) ||
      trace.materialSwingEvidence ||
      latentStrategicEvidence(trace)

  private def latentStrategicEvidence(trace: CandidateCauseDecisionTrace): Boolean =
    trace.candidatePlanEvidence ||
      trace.referenceStructuralTargetRelease ||
      trace.referenceStructuralImprovement ||
      trace.candidateStructuralImprovement ||
      trace.candidatePawnStructureImprovement ||
      trace.candidateTargetPressureGain ||
      trace.candidateCenterControlGain ||
      trace.candidateDevelopmentActivation ||
      trace.candidatePieceActivityGain

  private def lowDepthCauseShouldBeAudited(
      fact: CandidateComparisonFact,
      causeKinds: List[RelativeCauseKind]
  ): Boolean =
    requiresExplanatoryCause(fact) ||
      (!positiveContextAlternative(fact) &&
        causeKinds.exists(tacticalCause) &&
        (fact.comparison.winPercentLossForMover >= JudgmentThresholds.SIGNIFICANT_THREAT_WP ||
          fact.comparison.candidateWinPercentDeltaForMover >= JudgmentThresholds.SIGNIFICANT_THREAT_WP))

  private def playedMoveCandidateSideComparison(kind: CandidateComparisonKind): Boolean =
    kind == CandidateComparisonKind.PlayedVsBest || kind == CandidateComparisonKind.PlayedVsAlternative

  private def hasForcingMotif(records: List[EvidenceRecord]): Boolean =
    records.exists {
      case EvidenceRecord(_, MoveMotifEvidence(moveUci, motifs), _) =>
        motifs.exists(motif =>
          TacticalMotifClassifier.isRootMoveMotif(moveUci, motif) &&
            TacticalMotifClassifier.isForcing(motif)
        )
      case _ => false
    }

  private def hasCauseEligibleTacticalMotif(records: List[EvidenceRecord]): Boolean =
    records.exists {
      case EvidenceRecord(_, MoveMotifEvidence(moveUci, motifs), _) =>
        motifs.exists(motif =>
          TacticalMotifClassifier.isRootMoveMotif(moveUci, motif) &&
            TacticalMotifClassifier.isCauseEligible(motif)
        )
      case _ => false
    }

  private def hasConcreteLineConsequence(records: List[EvidenceRecord]): Boolean =
    records.exists {
      case EvidenceRecord(_, payload: LineFactEvidence, _) =>
        payload.consequenceProfile.hasConcreteProofSignal
      case EvidenceRecord(_, EvalFactEvidence(_, _, mate, _), _) =>
        mate.nonEmpty
      case EvidenceRecord(_, RelationFactEvidence(_, _, _, lineMoves, _), _) =>
        lineMoves.nonEmpty
      case _ =>
        false
    }

  private def candidateConcreteTacticalBridge(
      records: List[EvidenceRecord],
      relationKinds: List[RelationFactKind]
  ): Boolean =
    hasForcingMotif(records) ||
      (hasCauseEligibleTacticalMotif(records) && hasConcreteLineConsequence(records)) ||
      relationKinds.exists(TacticalMotifClassifier.isRiskRelation)

  private def hasOnlyDefense(records: List[EvidenceRecord]): Boolean =
    records.exists {
      case EvidenceRecord(_, ThreatPressureEvidence(_, threats), _) =>
        threats.defense.onlyDefense.nonEmpty
      case _ => false
    }

  private def referenceHasTacticalRisk(records: List[EvidenceRecord]): Boolean =
    records.exists {
      case EvidenceRecord(_, EvalFactEvidence(_, _, mate, _), _) =>
        mate.nonEmpty
      case EvidenceRecord(_, RelationFactEvidence(kind, _, _, lineMoves, _), _) =>
        lineMoves.nonEmpty && TacticalMotifClassifier.isRiskRelation(kind)
      case _ =>
        false
    }

  private def hasThreatResource(records: List[EvidenceRecord]): Boolean =
    records.exists {
      case EvidenceRecord(_, ThreatPressureEvidence(_, threats), _) =>
        threats.defense.onlyDefense.nonEmpty ||
          (!threats.insufficientData &&
            (threats.defenseRequired || threats.maxWinPercentLossIfIgnored.exists(_ >= JudgmentThresholds.MATERIAL_THREAT_WP)))
      case _ => false
    }

  private def hasProphylacticResource(records: List[EvidenceRecord]): Boolean =
    records.exists {
      case EvidenceRecord(_, ThreatPressureEvidence(_, threats), _) =>
        !threats.insufficientData &&
          (threats.prophylaxisNeeded ||
            threats.defense.prophylaxisNeeded ||
            threats.maxWinPercentLossIfIgnored.exists(_ >= JudgmentThresholds.SIGNIFICANT_THREAT_WP))
      case _ => false
    }

  private def hasCastlingResource(records: List[EvidenceRecord]): Boolean =
    records.exists {
      case EvidenceRecord(ref, MoveMotifEvidence(moveUci, motifs), _) if rootMoveBound(ref, moveUci) =>
        motifs.exists {
          case Motif.Castling(_, _, _, move) if motifMoveBound(move, moveUci) => true
          case _                                                             => false
        }
      case _ =>
        false
    }

  private def hasKingStepResource(records: List[EvidenceRecord]): Boolean =
    records.exists {
      case EvidenceRecord(ref, MoveMotifEvidence(moveUci, motifs), _) if rootMoveBound(ref, moveUci) =>
        motifs.exists {
          case Motif.KingStep(stepType, _, _, move) if motifMoveBound(move, moveUci) =>
            stepType != Motif.KingStepType.Activation
          case _ =>
            false
        }
      case _ =>
        false
    }

  private def hasPreventivePawnResource(records: List[EvidenceRecord]): Boolean =
    records.exists {
      case EvidenceRecord(ref, MoveMotifEvidence(moveUci, motifs), _) if rootMoveBound(ref, moveUci) =>
        val hasFlankPawnMove = motifs.exists {
          case Motif.PawnAdvance(file, _, _, color, _, move) if motifMoveBound(move, moveUci) =>
            kingFlankPawnAdvance(records, file, color)
          case _ =>
            false
        }
        val hasKingSafetyCue = motifs.exists {
          case _: Motif.Pin | _: Motif.WeakBackRank | _: Motif.BackRankMate | _: Motif.MateNet => true
          case _                                                                                => false
        }
        hasFlankPawnMove && hasKingSafetyCue
      case _ =>
        false
    }

  private def hasKingHomeStepConcession(records: List[EvidenceRecord]): Boolean =
    records.exists {
      case EvidenceRecord(ref, MoveMotifEvidence(moveUci, motifs), _) if rootMoveBound(ref, moveUci) =>
        motifs.exists {
          case Motif.KingStep(_, color, _, move) if motifMoveBound(move, moveUci) =>
            kingHomeStep(moveUci, color) && hasCastlingRight(records, color)
          case _ =>
            false
        }
      case _ =>
        false
    }

  private def sameDestinationCaptureChoice(
      fact: CandidateComparisonFact,
      referenceRecords: List[EvidenceRecord],
      candidateRecords: List[EvidenceRecord]
  ): Boolean =
    val referenceMove = normalizeMove(fact.referenceLine.rootMove)
    val candidateMove = normalizeMove(fact.candidateLine.rootMove)
    sameDestinationDifferentOrigin(referenceMove, candidateMove) &&
      hasRootCapture(referenceRecords, referenceMove) &&
      hasRootCapture(candidateRecords, candidateMove)

  private def sameDestinationDifferentOrigin(left: String, right: String): Boolean =
    left.length >= 4 &&
      right.length >= 4 &&
      left.take(2) != right.take(2) &&
      left.slice(2, 4) == right.slice(2, 4)

  private def hasRootCapture(records: List[EvidenceRecord], rootMove: String): Boolean =
    records.exists {
      case EvidenceRecord(ref, MoveMotifEvidence(moveUci, motifs), _)
          if normalizeMove(moveUci) == rootMove || rootMoveBound(ref, rootMove) =>
        motifs.exists {
          case Motif.Capture(_, _, _, _, _, _, move, _) if motifMoveBound(move, rootMove) =>
            true
          case _ =>
            false
        }
      case _ =>
        false
    }

  private def kingHomeStep(moveUci: String, color: Color): Boolean =
    val move = normalizeMove(moveUci)
    val from = if color.white then "e1" else "e8"
    val castleDestinations = if color.white then Set("g1", "c1") else Set("g8", "c8")
    move.length >= 4 && move.take(2) == from && !castleDestinations.contains(move.slice(2, 4))

  private def hasCastlingRight(records: List[EvidenceRecord], color: Color): Boolean =
    records.exists(record => castlingRights(record.ref.position.fen).exists(rights => colorCastlingRight(rights, color)))

  private def castlingRights(fen: String): Option[String] =
    Option(fen).flatMap(_.split(" ").lift(2)).filter(_ != "-")

  private def colorCastlingRight(rights: String, color: Color): Boolean =
    if color.white then rights.exists(ch => ch == 'K' || ch == 'Q')
    else rights.exists(ch => ch == 'k' || ch == 'q')

  private def rootMoveBound(ref: EvidenceRef, moveUci: String): Boolean =
    ref.line.exists(line => normalizeMove(line.rootMove) == normalizeMove(moveUci))

  private def motifMoveBound(move: Option[String], moveUci: String): Boolean =
    move.exists(value => normalizeMove(value) == normalizeMove(moveUci))

  private def normalizeMove(raw: String): String =
    Option(raw).getOrElse("").trim.toLowerCase

  private def kingFlankPawnAdvance(records: List[EvidenceRecord], pawnFile: File, color: Color): Boolean =
    kingFile(records, color).exists(file => (fileIndex(file) - fileIndex(pawnFile)).abs <= 1)

  private def kingFile(records: List[EvidenceRecord], color: Color): Option[File] =
    records.view.flatMap(record => kingFileFromFen(record.ref.position.fen, color)).headOption

  private def kingFileFromFen(fen: String, color: Color): Option[File] =
    val king = if color.white then 'K' else 'k'
    Option(fen)
      .map(_.takeWhile(_ != ' '))
      .flatMap: board =>
        var file = 0
        var found = Option.empty[File]
        board.iterator.foreach {
          case '/' =>
            file = 0
          case ch if ch.isDigit =>
            file += ch.asDigit
          case ch =>
            val current = file
            file += 1
            if ch == king && found.isEmpty then found = Some(fileFromIndex(current))
        }
        found

  private def fileFromIndex(index: Int): File =
    List(File.A, File.B, File.C, File.D, File.E, File.F, File.G, File.H)(index.max(0).min(7))

  private def fileIndex(file: File): Int =
    file match
      case File.A => 0
      case File.B => 1
      case File.C => 2
      case File.D => 3
      case File.E => 4
      case File.F => 5
      case File.G => 6
      case File.H => 7

  private def hasConversionWindow(records: List[EvidenceRecord]): Boolean =
    records.exists {
      case EvidenceRecord(_, SinglePositionEvidence(assessment), _) =>
        assessment.simplifyBias.shouldSimplify
      case EvidenceRecord(_, RelationFactEvidence(RelationFactKind.BadPieceLiquidation, _, _, _, _), _) =>
        true
      case _ => false
    }

  private def hasStructuralTargetRelease(records: List[EvidenceRecord]): Boolean =
    records.exists {
      case EvidenceRecord(_, StructuralDeltaEvidence(delta), _) =>
        delta.releasedTargetPressure.nonEmpty && delta.targetPressureRelease > delta.targetPressureGain
      case _ =>
        false
    }

  private def hasStructuralImprovementEvidence(records: List[EvidenceRecord]): Boolean =
    records.exists {
      case EvidenceRecord(_, StructuralDeltaEvidence(delta), _) =>
        hasStructuralAnchor(delta)
      case _ =>
        false
    }

  private def hasStructuralAnchor(delta: lila.chessjudgment.analysis.structure.StructuralDelta): Boolean =
    delta.fileAccessDelta > 0 ||
      delta.fileOccupation.nonEmpty ||
      delta.openedFiles.nonEmpty ||
      delta.semiOpenedFiles.nonEmpty ||
      delta.newWeakPawns.nonEmpty ||
      delta.newWeakSquares.nonEmpty ||
      delta.createdTension.nonEmpty ||
      delta.pawnTensionDelta > 0

  private def hasTargetPressureGainEvidence(records: List[EvidenceRecord]): Boolean =
    records.exists {
      case EvidenceRecord(_, StructuralDeltaEvidence(delta), _) =>
        delta.createdTargetPressure.nonEmpty && delta.targetPressureGain > delta.targetPressureRelease ||
          delta.targetPressureDelta > 0
      case _ =>
        false
    }

  private def hasCenterControlGainEvidence(records: List[EvidenceRecord]): Boolean =
    records.exists {
      case EvidenceRecord(_, StructuralDeltaEvidence(delta), _) =>
        delta.centerControlDelta > 0
      case _ =>
        false
    }

  private def hasDevelopmentActivationEvidence(records: List[EvidenceRecord]): Boolean =
    records.exists {
      case EvidenceRecord(_, StructuralDeltaEvidence(delta), _) =>
        delta.developmentDelta > 0
      case _ =>
        false
    }

  private def hasPieceActivityGainEvidence(records: List[EvidenceRecord]): Boolean =
    records.exists {
      case EvidenceRecord(_, StructuralDeltaEvidence(delta), _) =>
        delta.lineUnlockDelta > 0 || delta.mobilityDelta > 0
      case _ =>
        false
    }

  private def hasPawnStructureImprovementEvidence(records: List[EvidenceRecord]): Boolean =
    pawnStructureImprovementSignals(records).nonEmpty

  private def strategicImprovementScore(records: List[EvidenceRecord]): Int =
    records.map {
      case EvidenceRecord(_, StructuralDeltaEvidence(delta), _) =>
        structuralImprovementScore(delta)
      case EvidenceRecord(_, PlanTransitionEvidence(transition), _) =>
        if transition.primaryPlanId.nonEmpty && transition.transitionType != TransitionType.Opening then 2 else 0
      case EvidenceRecord(_, PlanPressureEvidence(scoring, activePlans), _) =>
        if scoring.confidence >= 0.35 &&
          (activePlans.primary.evidence.nonEmpty || scoring.compatibilityEvents.nonEmpty || activePlans.compatibilityEvents.nonEmpty)
        then 2
        else 0
      case EvidenceRecord(_, PawnStructureFactEvidence(_, alignment, pawnPlay), _) =>
        alignment.count(alignment => alignment.band == AlignmentBand.OnBook || alignment.band == AlignmentBand.Playable) * 2 +
          pawnPlay.count(_.primaryDriver != PawnPlayDriver.Quiet)
      case _ =>
        0
    }.sum

  private def structuralImprovementScore(delta: lila.chessjudgment.analysis.structure.StructuralDelta): Int =
    delta.createdTargetPressure.size * 3 +
      (delta.targetPressureGain - delta.targetPressureRelease).max(0) * 2 +
      delta.targetPressureDelta.max(0) * 2 +
      delta.centerControlDelta.max(0) +
      delta.developmentDelta.max(0) +
      delta.mobilityDelta.max(0) +
      delta.lineUnlockDelta.max(0) +
      delta.fileAccessDelta.max(0) +
      delta.openedFiles.size +
      delta.semiOpenedFiles.size +
      delta.fileOccupation.size * 2 +
      delta.newWeakPawns.size +
      delta.newWeakSquares.size +
      delta.createdTension.size

  private def structuralImprovementSignals(records: List[EvidenceRecord]): List[String] =
    records.collect { case EvidenceRecord(_, StructuralDeltaEvidence(delta), _) =>
      List(
        Option.when(delta.createdTargetPressure.nonEmpty)("created_target_pressure"),
        Option.when(delta.targetPressureGain > delta.targetPressureRelease)("target_pressure_gain"),
        Option.when(delta.targetPressureDelta > 0)("target_pressure_delta"),
        Option.when(delta.fileAccessDelta > 0)("file_access_gain"),
        Option.when(delta.kingShelterDelta > 0)("king_shelter_pressure"),
        Option.when(delta.mobilityDelta > 0)("mobility_gain"),
        Option.when(delta.developmentDelta > 0)("development_gain"),
        Option.when(delta.developmentMoves.nonEmpty)("development_choice"),
        Option.when(delta.centerControlDelta > 0)("center_control_gain"),
        Option.when(delta.lineUnlockDelta > 0)("line_unlock"),
        Option.when(delta.fileOccupation.nonEmpty)("file_occupation"),
        Option.when(delta.openedFiles.nonEmpty)("opened_file"),
        Option.when(delta.semiOpenedFiles.nonEmpty)("semi_opened_file"),
        Option.when(delta.newWeakPawns.nonEmpty)("new_weak_pawn"),
        Option.when(delta.newWeakSquares.nonEmpty)("new_weak_square"),
        Option.when(delta.createdTension.nonEmpty)("created_tension"),
        Option.when(delta.pawnTensionDelta > 0)("pawn_tension_gain")
      ).flatten
    }.flatten.distinct.sorted

  private def pawnStructureImprovementSignals(records: List[EvidenceRecord]): List[String] =
    records.collect { case EvidenceRecord(_, PawnStructureFactEvidence(_, alignment, pawnPlay), _) =>
      List(
        Option.when(alignment.exists(alignment => alignment.band == AlignmentBand.OnBook || alignment.band == AlignmentBand.Playable))(
          "plan_alignment"
        ),
        Option.when(pawnPlay.exists(_.primaryDriver != PawnPlayDriver.Quiet))("pawn_play_driver")
      ).flatten
    }.flatten.distinct.sorted

  private def releasedTargetPressure(records: List[EvidenceRecord]): List[String] =
    records.collect { case EvidenceRecord(_, StructuralDeltaEvidence(delta), _) =>
      delta.releasedTargetPressure
    }.flatten.distinct.sorted

  private def createdTargetPressure(records: List[EvidenceRecord]): List[String] =
    records.collect { case EvidenceRecord(_, StructuralDeltaEvidence(delta), _) =>
      delta.createdTargetPressure
    }.flatten.distinct.sorted

  private def hasPlanCauseEvidence(records: List[EvidenceRecord]): Boolean =
    records.exists {
      case EvidenceRecord(_, PlanTransitionEvidence(transition), _) =>
        transition.primaryPlanId.nonEmpty &&
          transition.transitionType != TransitionType.Opening &&
          transition.momentum >= 0.55
      case EvidenceRecord(_, PlanPressureEvidence(scoring, activePlans), _) =>
        scoring.confidence >= 0.35 &&
          (activePlans.primary.evidence.nonEmpty || scoring.compatibilityEvents.nonEmpty || activePlans.compatibilityEvents.nonEmpty)
      case _ =>
        false
    }

  private def hasStrategicCauseEvidence(records: List[EvidenceRecord]): Boolean =
    records.exists {
      case EvidenceRecord(_, payload: StrategicFactEvidence, _) =>
        payload.hasTypedSupport
      case _ =>
        false
    }

  private def hasStrategicConcessionEvidence(records: List[EvidenceRecord]): Boolean =
    records.exists {
      case EvidenceRecord(_, StructuralDeltaEvidence(delta), _) =>
        delta.releasedTargetPressure.nonEmpty && delta.targetPressureRelease > delta.targetPressureGain ||
          delta.targetPressureDelta < 0 ||
          delta.fileAccessDelta < 0 ||
          delta.kingShelterDelta < 0
      case _ =>
        false
    }

  private def hasStrongStrategicConcessionEvidence(records: List[EvidenceRecord]): Boolean =
    records.exists {
      case EvidenceRecord(_, StructuralDeltaEvidence(delta), _) =>
        delta.releasedTargetPressure.nonEmpty && delta.targetPressureRelease > delta.targetPressureGain ||
          delta.targetPressureDelta < 0 ||
          delta.fileAccessDelta < 0 ||
          delta.kingShelterDelta < 0
      case _ =>
        false
    }

  private def hasMaterialSwingEvidence(
      referenceRecords: List[EvidenceRecord],
      candidateRecords: List[EvidenceRecord]
  ): Boolean =
    typedMaterialConsequenceSwing(referenceRecords, candidateRecords)

  private def typedMaterialConsequenceSwing(
      referenceRecords: List[EvidenceRecord],
      candidateRecords: List[EvidenceRecord]
  ): Boolean =
    materialGainMagnitude(referenceRecords).ordinal > materialGainMagnitude(candidateRecords).ordinal ||
      materialLossMagnitude(candidateRecords).ordinal > materialLossMagnitude(referenceRecords).ordinal

  private def materialBestNet(records: List[EvidenceRecord]): Int =
    materialSummaries(records).map(_.netCaptureCpForMover).maxOption.getOrElse(0)

  private def materialBestGain(records: List[EvidenceRecord]): Int =
    materialSummaries(records).map(_.maxGainCpForMover).maxOption.getOrElse(0)

  private def materialBestPromotionGain(records: List[EvidenceRecord]): Int =
    materialSummaries(records).map(_.promotionGainCpForMover).maxOption.getOrElse(0)

  private def materialGainMagnitude(records: List[EvidenceRecord]): LineMaterialOutcomeMagnitude =
    materialOutcomeProfile(records).gainMagnitude

  private def materialLossMagnitude(records: List[EvidenceRecord]): LineMaterialOutcomeMagnitude =
    materialOutcomeProfile(records).lossMagnitude

  private def materialOutcomeProfile(records: List[EvidenceRecord]): LineMaterialOutcomeProfile =
    records.collect { case EvidenceRecord(_, payload: LineFactEvidence, _) =>
      payload.materialOutcomeProfile
    }.foldLeft(LineMaterialOutcomeProfile.empty) { (merged, profile) =>
      LineMaterialOutcomeProfile(
        gainSignals = merged.gainSignals ++ profile.gainSignals,
        lossSignals = merged.lossSignals ++ profile.lossSignals
      )
    }

  private def materialSummaries(records: List[EvidenceRecord]): List[LineMaterialSummary] =
    records.collect { case EvidenceRecord(_, payload: LineFactEvidence, _) => payload.lineMaterialSummary }.flatten

  private def motifNames(records: List[EvidenceRecord]): List[String] =
    records.collect { case EvidenceRecord(_, MoveMotifEvidence(moveUci, motifs), _) =>
      motifs
        .filter(TacticalMotifClassifier.isRootMoveMotif(moveUci, _))
        .map(motif => motif.getClass.getSimpleName.stripSuffix("$"))
    }.flatten.distinct

  private def relationKinds(records: List[EvidenceRecord]): List[RelationFactKind] =
    records.collect { case EvidenceRecord(_, RelationFactEvidence(kind, _, _, _, _), _) => kind }.distinct

  private val explanatoryLayers: Set[EvidenceLayer] =
    Set(
      EvidenceLayer.SinglePosition,
      EvidenceLayer.PawnStructure,
      EvidenceLayer.Strategic,
      EvidenceLayer.OpeningContext,
      EvidenceLayer.FeatureAnchor,
      EvidenceLayer.ApplicabilityAssessment,
      EvidenceLayer.ThreatPressure,
      EvidenceLayer.MoveMotif,
      EvidenceLayer.Relation,
      EvidenceLayer.StructuralDelta,
      EvidenceLayer.PlanPressure,
      EvidenceLayer.PlanTransition
    )

  private def availableLayers(evidenceLayers: ComparisonEvidenceLayerDiagnostics): Set[EvidenceLayer] =
    List(evidenceLayers.reference, evidenceLayers.candidate).flatMap(neighborhood =>
      neighborhood.directLayers ++ neighborhood.parentLayerCounts.keySet
    ).toSet

  private def layerCounts(layers: List[EvidenceLayer]): Map[EvidenceLayer, Int] =
    layers.groupMapReduce(identity)(_ => 1)(_ + _)

final case class SemanticCoverageMetrics(
    tacticalIdeas: Int,
    strategicIdeas: Int,
    pawnStructureIdeas: Int,
    openingIdeas: Int,
    defensiveIdeas: Int,
    evaluationIdeas: Int,
    conversionIdeas: Int,
    materialIdeas: Int,
    claimFamilies: Set[ClaimFamily],
    claimSupportClusters: Int,
    clusteredAnchorClaims: Int,
    clusteredSupportingClaims: Int,
    clusteredConstrainingClaims: Int,
    clusteredLongTermSupportIdeas: Int,
    claimSupportClusterFamilies: Set[ClaimFamily],
    claimSupportClusterLayers: Set[EvidenceLayer],
    claimEventClusters: Int,
    clusteredEventClaims: Int,
    clusteredEventCauseClaims: Int,
    clusteredEventEvaluationClaims: Int,
    clusteredEventWitnessClaims: Int,
    clusteredEventIdeas: Int,
    relatedEventSupportClusters: Int,
    claimEventClusterFamilies: Set[ClaimFamily],
    claimEventClusterLayers: Set[EvidenceLayer],
    claimEventClusterKinds: Set[ClaimEventClusterKind],
    claimEventClusterCauses: Set[RelativeCauseKind],
    claimEventInteractionKinds: Set[ClaimInteractionKind],
    unclusteredEventClaims: Int,
    unclusteredEventClaimFamilies: Set[ClaimFamily],
    verdictCarriersWithoutEventCauseOwner: Int,
    verdictOnlyEvaluationClaims: Int,
    localConcreteClaims: Int,
    localConcreteClaimFamilies: Set[ClaimFamily],
    localConcreteClaimDiagnostics: List[LocalConcreteClaimDiagnostic],
    boardAnchorFacts: Int,
    boardAttackDefenseFacts: Int,
    lineReplayFacts: Int,
    lineEventFacts: Int,
    lineConsequenceFacts: Int,
    relativeCauseProofs: Int,
    hasRelativeAssessment: Boolean,
    candidateComparisonFacts: Int,
    relativeCauseFacts: Int,
    moveVerdictCertifications: Int,
    playedRelatedComparisonFacts: Int,
    playedRelativeCauseFacts: Int,
    branchReplyProbeRequests: Int,
    branchReplyProbeMoves: List[String],
    branchReplyThreatLines: Int,
    branchReplyThreatPressureRecords: Int,
    branchReplyProbeAdmittedResults: Int,
    branchReplyProbeRejectedResults: Int,
    branchReplyProbeIgnoredResults: Int,
    branchReplyProbePendingRequests: Int,
    branchReplyProbeRejectReasons: Map[String, Int],
    branchReplyProbeAdmittedMoves: List[String],
    branchReplyProbeRejectedMoves: List[String],
    branchReplyProbePendingMoves: List[String],
    branchReplyProbeLifecycleState: String,
    hasPendingBranchReplyDepth: Boolean,
    hasUnexplainedEngineGap: Boolean,
    hasPlayedUnexplainedEngineGap: Boolean,
    hasContextUnexplainedEngineGap: Boolean,
    hasSecondaryContextEngineGap: Boolean,
    comparisonDiagnostics: List[CandidateComparisonDiagnostic],
    secondaryContextComparisonIds: List[String],
    secondaryContextWithPrimaryCoverageIds: List[String],
    secondaryContextWithoutPrimaryCoverageIds: List[String],
    contextUnexplainedComparisonIds: List[String],
    contextUnexplainedWithPrimaryCoverageIds: List[String],
    contextUnexplainedWithoutPrimaryCoverageIds: List[String],
    openingApplicabilityDiagnostics: List[OpeningApplicabilityDiagnostic],
    hasVerdict: Boolean,
    hasCandidateSetComparison: Boolean,
    hasOnlyMoveSignal: Boolean,
    hasForcedLineTheme: Boolean
)

object SemanticCoverageMetrics:
  def from(packet: EvidenceBackedJudgmentPacket): SemanticCoverageMetrics =
    val playedComparisonRecords = playedRelatedComparisonRecords(packet)
    val primaryPlayedComparisonRecords = primaryPlayedCandidateComparisonRecords(packet)
    val playedCauseRecords = playedRelativeCauseRecords(packet)
    val comparisonDiagnostics = CandidateComparisonDiagnostic.fromPacket(packet)
    val clusteredEventClaimIds = packet.claimEventClusters.flatMap(_.memberClaimIds).toSet
    val unclusteredConcreteClaims =
      packet.claims.filter(claim => claim.family.isEvent && !clusteredEventClaimIds.contains(claim.id))
    val directEventUnclusteredClaims = unclusteredConcreteClaims.filter(hasDirectEventCause(_, packet.evidenceGraph))
    val unclusteredEventClaims = directEventUnclusteredClaims.filterNot(_.family == ClaimFamily.Evaluation)
    val verdictCarriersWithoutEventCauseOwner = directEventUnclusteredClaims.filter(_.family == ClaimFamily.Evaluation)
    val noDirectEventClaims = unclusteredConcreteClaims.filterNot(hasDirectEventCause(_, packet.evidenceGraph))
    val verdictOnlyEvaluationClaims = noDirectEventClaims.filter(_.family == ClaimFamily.Evaluation)
    val localConcreteClaims = noDirectEventClaims.filterNot(_.family == ClaimFamily.Evaluation)
    val localConcreteClaimDiagnostics =
      localConcreteClaims.map(localConcreteClaimDiagnostic(packet, _))
    val primaryPlayedComparisonIds = primaryPlayedComparisonRecords.map(_.ref.id).toSet
    val playedHasUnexplained =
      comparisonDiagnostics.exists(diagnostic =>
        primaryPlayedComparisonIds.contains(diagnostic.id) && diagnostic.hasUnexplainedEngineGap
      )
    val contextUnexplainedIds =
      comparisonDiagnostics
        .filter(diagnostic =>
          !primaryPlayedComparisonIds.contains(diagnostic.id) &&
            (diagnostic.hasUnexplainedEngineGap || diagnostic.hasSecondaryContextEngineGap)
        )
        .map(_.id)
    val secondaryContextIds =
      comparisonDiagnostics
        .filter(diagnostic =>
          !primaryPlayedComparisonIds.contains(diagnostic.id) &&
            diagnostic.hasSecondaryContextEngineGap
        )
        .map(_.id)
    val primaryPlayedCovered =
      comparisonDiagnostics.exists(diagnostic =>
        primaryPlayedComparisonIds.contains(diagnostic.id) &&
          !diagnostic.hasUnexplainedEngineGap &&
          diagnostic.causeKinds.nonEmpty
      )
    val contextUnexplainedWithPrimaryCoverageIds =
      if primaryPlayedCovered then contextUnexplainedIds else Nil
    val contextUnexplainedWithoutPrimaryCoverageIds =
      if primaryPlayedCovered then Nil else contextUnexplainedIds
    val secondaryContextWithPrimaryCoverageIds =
      if primaryPlayedCovered then secondaryContextIds else Nil
    val secondaryContextWithoutPrimaryCoverageIds =
      if primaryPlayedCovered then Nil else secondaryContextIds
    val branchReplyProbeRequests =
      packet.probeRequests.filter(request => request.purpose.exists(branchReplyProbePurpose))
    val branchReplyThreatLines =
      packet.candidateLines.count(_.role == LineNodeRole.Threat)
    val branchReplyThreatPressureRecords =
      packet.evidenceGraph.records.count {
        case EvidenceRecord(ref, _: ThreatPressureEvidence, _) =>
          ref.scope == EvidenceScope.ThreatLine || ref.line.exists(_.role == LineNodeRole.Threat)
        case _ =>
          false
      }
    val branchReplyProbeDiagnostics = packet.probeDiagnostics
    val admittedProbeResults =
      branchReplyProbeDiagnostics.filter(_.status == ProbeAdmissionStatus.Admitted)
    val rejectedProbeResults =
      branchReplyProbeDiagnostics.filter(_.status == ProbeAdmissionStatus.Rejected)
    val ignoredProbeResults =
      branchReplyProbeDiagnostics.filter(_.status == ProbeAdmissionStatus.Ignored)
    val diagnosedProbeIds = branchReplyProbeDiagnostics.map(_.probeId).toSet
    val pendingProbeRequests =
      branchReplyProbeRequests.filterNot(request => diagnosedProbeIds.contains(request.id))
    val branchReplyProbeLifecycleState =
      if branchReplyProbeRequests.isEmpty && branchReplyProbeDiagnostics.isEmpty then "none"
      else if pendingProbeRequests.nonEmpty && branchReplyProbeDiagnostics.isEmpty then "request_only_pending"
      else if pendingProbeRequests.nonEmpty then "partial_pending"
      else if admittedProbeResults.nonEmpty && rejectedProbeResults.isEmpty && ignoredProbeResults.isEmpty then "admitted"
      else if admittedProbeResults.isEmpty && rejectedProbeResults.nonEmpty then "rejected"
      else if admittedProbeResults.isEmpty && ignoredProbeResults.nonEmpty then "ignored"
      else "mixed"
    SemanticCoverageMetrics(
      tacticalIdeas = countIdeas(packet, ChessIdeaFamily.Tactical),
      strategicIdeas = countIdeas(packet, ChessIdeaFamily.Strategic),
      pawnStructureIdeas = countIdeas(packet, ChessIdeaFamily.PawnStructure),
      openingIdeas = countIdeas(packet, ChessIdeaFamily.Opening),
      defensiveIdeas = countIdeas(packet, ChessIdeaFamily.Defensive),
      evaluationIdeas = countIdeas(packet, ChessIdeaFamily.Evaluation),
      conversionIdeas = countIdeas(packet, ChessIdeaFamily.Conversion),
      materialIdeas = countIdeas(packet, ChessIdeaFamily.Material),
      claimFamilies = packet.claims.map(_.family).toSet,
      claimSupportClusters = packet.claimSupportClusters.size,
      clusteredAnchorClaims = packet.claimSupportClusters.flatMap(_.anchorClaimIds).distinct.size,
      clusteredSupportingClaims = packet.claimSupportClusters.flatMap(_.supportingClaimIds).distinct.size,
      clusteredConstrainingClaims = packet.claimSupportClusters.flatMap(_.constrainingClaimIds).distinct.size,
      clusteredLongTermSupportIdeas = packet.claimSupportClusters.flatMap(_.ideas.map(_.id)).distinct.size,
      claimSupportClusterFamilies = packet.claimSupportClusters.flatMap(_.families).toSet,
      claimSupportClusterLayers = packet.claimSupportClusters.flatMap(_.presentLayers).toSet,
      claimEventClusters = packet.claimEventClusters.size,
      clusteredEventClaims = packet.claimEventClusters.flatMap(_.memberClaimIds).distinct.size,
      clusteredEventCauseClaims = packet.claimEventClusters.flatMap(_.causeClaimIds).distinct.size,
      clusteredEventEvaluationClaims = packet.claimEventClusters.flatMap(_.evaluationClaimIds).distinct.size,
      clusteredEventWitnessClaims = packet.claimEventClusters.flatMap(_.witnessClaimIds).distinct.size,
      clusteredEventIdeas = packet.claimEventClusters.flatMap(_.ideas.map(_.id)).distinct.size,
      relatedEventSupportClusters = packet.claimEventClusters.flatMap(_.relatedSupportClusterIds).distinct.size,
      claimEventClusterFamilies = packet.claimEventClusters.flatMap(_.families).toSet,
      claimEventClusterLayers = packet.claimEventClusters.flatMap(_.presentLayers).toSet,
      claimEventClusterKinds = packet.claimEventClusters.map(_.kind).toSet,
      claimEventClusterCauses = packet.claimEventClusters.map(_.causeKind).toSet,
      claimEventInteractionKinds = packet.claimEventClusters.flatMap(_.interactions.map(_.kind)).toSet,
      unclusteredEventClaims = unclusteredEventClaims.size,
      unclusteredEventClaimFamilies = unclusteredEventClaims.map(_.family).toSet,
      verdictCarriersWithoutEventCauseOwner = verdictCarriersWithoutEventCauseOwner.size,
      verdictOnlyEvaluationClaims = verdictOnlyEvaluationClaims.size,
      localConcreteClaims = localConcreteClaims.size,
      localConcreteClaimFamilies = localConcreteClaims.map(_.family).toSet,
      localConcreteClaimDiagnostics = localConcreteClaimDiagnostics,
      boardAnchorFacts = boardAnchorCount(packet),
      boardAttackDefenseFacts = boardAttackDefenseCount(packet),
      lineReplayFacts = lineReplayCount(packet),
      lineEventFacts = lineEventCount(packet),
      lineConsequenceFacts = lineConsequenceCount(packet),
      relativeCauseProofs = relativeCauseProofCount(packet),
      hasRelativeAssessment = packet.relativeAssessments.nonEmpty,
      candidateComparisonFacts = packet.evidenceGraph.records.count(_.ref.layer == EvidenceLayer.CandidateComparison),
      relativeCauseFacts = packet.evidenceGraph.records.count(_.ref.layer == EvidenceLayer.RelativeCause),
      moveVerdictCertifications = packet.evidenceGraph.records.count(_.ref.layer == EvidenceLayer.MoveVerdictCertification),
      playedRelatedComparisonFacts = playedComparisonRecords.size,
      playedRelativeCauseFacts = playedCauseRecords.size,
      branchReplyProbeRequests = branchReplyProbeRequests.size,
      branchReplyProbeMoves = branchReplyProbeRequests.flatMap(_.candidateMove).distinct.sorted,
      branchReplyThreatLines = branchReplyThreatLines,
      branchReplyThreatPressureRecords = branchReplyThreatPressureRecords,
      branchReplyProbeAdmittedResults = admittedProbeResults.size,
      branchReplyProbeRejectedResults = rejectedProbeResults.size,
      branchReplyProbeIgnoredResults = ignoredProbeResults.size,
      branchReplyProbePendingRequests = pendingProbeRequests.size,
      branchReplyProbeRejectReasons = rejectedProbeResults.flatMap(_.reasonCodes).groupMapReduce(identity)(_ => 1)(_ + _),
      branchReplyProbeAdmittedMoves = admittedProbeResults.flatMap(_.candidateMove).distinct.sorted,
      branchReplyProbeRejectedMoves = rejectedProbeResults.flatMap(_.candidateMove).distinct.sorted,
      branchReplyProbePendingMoves = pendingProbeRequests.flatMap(_.candidateMove).distinct.sorted,
      branchReplyProbeLifecycleState = branchReplyProbeLifecycleState,
      hasPendingBranchReplyDepth = pendingProbeRequests.nonEmpty,
      hasUnexplainedEngineGap = playedHasUnexplained,
      hasPlayedUnexplainedEngineGap = playedHasUnexplained,
      hasContextUnexplainedEngineGap = contextUnexplainedWithoutPrimaryCoverageIds.nonEmpty,
      hasSecondaryContextEngineGap = secondaryContextIds.nonEmpty,
      comparisonDiagnostics = comparisonDiagnostics,
      secondaryContextComparisonIds = secondaryContextIds,
      secondaryContextWithPrimaryCoverageIds = secondaryContextWithPrimaryCoverageIds,
      secondaryContextWithoutPrimaryCoverageIds = secondaryContextWithoutPrimaryCoverageIds,
      contextUnexplainedComparisonIds = contextUnexplainedIds,
      contextUnexplainedWithPrimaryCoverageIds = contextUnexplainedWithPrimaryCoverageIds,
      contextUnexplainedWithoutPrimaryCoverageIds = contextUnexplainedWithoutPrimaryCoverageIds,
      openingApplicabilityDiagnostics = openingApplicabilityDiagnostics(packet),
      hasVerdict = packet.ideaVerdict.flatMap(_.verdict).nonEmpty,
      hasCandidateSetComparison = packet.relativeAssessments.exists(_.comparison.candidateSet.nonEmpty),
      hasOnlyMoveSignal = packet.relativeAssessments.exists(_.comparison.candidateSet.exists(_.onlyMove)),
      hasForcedLineTheme = packet.evidenceGraph.records.exists {
        case EvidenceRecord(_, payload: LineFactEvidence, _) => payload.hasForcedTheme
        case _                                               => false
      }
    )

  private def branchReplyProbePurpose(purpose: ProbePurpose): Boolean =
    purpose match
      case ProbePurpose.ReplyMultipv | ProbePurpose.DefenseReplyMultipv | ProbePurpose.ConvertReplyMultipv |
          ProbePurpose.RecaptureBranches | ProbePurpose.KeepTensionBranches | ProbePurpose.FreeTempoBranches =>
        true
      case _ =>
        false

  private def countIdeas(packet: EvidenceBackedJudgmentPacket, family: ChessIdeaFamily): Int =
    packet.ideas.count(_.ref.family == family)

  private def boardAnchorCount(packet: EvidenceBackedJudgmentPacket): Int =
    packet.evidenceGraph.records.collect { case EvidenceRecord(_, payload: BoardFactEvidence, _) =>
      payload.boardAnchors.size
    }.sum

  private def boardAttackDefenseCount(packet: EvidenceBackedJudgmentPacket): Int =
    packet.evidenceGraph.records.collect { case EvidenceRecord(_, payload: BoardFactEvidence, _) =>
      payload.attackDefense.size
    }.sum

  private def lineReplayCount(packet: EvidenceBackedJudgmentPacket): Int =
    packet.evidenceGraph.records.collect { case EvidenceRecord(_, payload: LineFactEvidence, _) =>
      payload.lineReplayCount
    }.sum

  private def lineEventCount(packet: EvidenceBackedJudgmentPacket): Int =
    packet.evidenceGraph.records.collect { case EvidenceRecord(_, payload: LineFactEvidence, _) =>
      payload.lineEventCount
    }.sum

  private def lineConsequenceCount(packet: EvidenceBackedJudgmentPacket): Int =
    packet.evidenceGraph.records.collect { case EvidenceRecord(_, payload: LineFactEvidence, _) =>
      payload.lineConsequenceCount
    }.sum

  private def relativeCauseProofCount(packet: EvidenceBackedJudgmentPacket): Int =
    packet.evidenceGraph.records.count {
      case EvidenceRecord(_, RelativeCauseFactEvidence(cause), _) =>
        cause.proof.exists(_.hasTypedDepth)
      case _ =>
        false
    }

  private def hasDirectEventCause(claim: ClaimSeed, graph: TypedEvidenceGraph): Boolean =
    claim.evidence.flatMap(ref => graph.byId.get(ref.id)).exists {
      case EvidenceRecord(_, RelativeCauseFactEvidence(cause), _) =>
        ClaimEventCluster.kindForCause(cause.kind).nonEmpty
      case EvidenceRecord(_, MoveVerdictCertificationEvidence(certification), _) =>
        certification.causes.exists(cause => ClaimEventCluster.kindForCause(cause.kind).nonEmpty)
      case _ =>
        false
    }

  private def localConcreteClaimDiagnostic(
      packet: EvidenceBackedJudgmentPacket,
      claim: ClaimSeed
  ): LocalConcreteClaimDiagnostic =
    val records = claim.evidence.flatMap(ref => packet.evidenceGraph.byId.get(ref.id))
    val layers = records.map(_.ref.layer).toSet
    val threat = records.collectFirst { case EvidenceRecord(_, ThreatPressureEvidence(_, threats), _) =>
      threats
    }
    LocalConcreteClaimDiagnostic(
      id = claim.id,
      family = claim.family,
      subject = claim.subject,
      primaryLine = claim.primaryLine,
      subjectMove = claim.subjectMove,
      scope = claim.scope,
      localKind = localConcreteKind(claim, layers),
      evidenceLayers = layers,
      evidenceIds = claim.evidence.map(_.id),
      engineVerdict = claim.engineComparison.map(_.verdict),
      engineWinPercentLossForMover = claim.engineComparison.map(_.winPercentLossForMover),
      threatSeverity = threat.map(_.threatSeverity),
      threatCount = threat.map(_.threatCount),
      threatDefenseRequired = threat.map(_.defenseRequired),
      threatProphylaxisNeeded = threat.map(_.prophylaxisNeeded),
      threatOnlyDefense = threat.flatMap(_.defense.onlyDefense),
      threatMaxWinPercentLossIfIgnored = threat.flatMap(_.maxWinPercentLossIfIgnored),
      threatPrimaryDriver = threat.map(_.primaryDriver),
      threatInsufficientData = threat.map(_.insufficientData),
      nearbyEventClusterIds = nearbyEventClusters(packet, claim)
    )

  private def localConcreteKind(
      claim: ClaimSeed,
      layers: Set[EvidenceLayer]
  ): LocalConcreteClaimKind =
    claim.family match
      case ClaimFamily.Defensive if layers.contains(EvidenceLayer.ThreatPressure) =>
        LocalConcreteClaimKind.DefensiveThreat
      case ClaimFamily.Tactical if layers.contains(EvidenceLayer.Relation) || layers.contains(EvidenceLayer.MoveMotif) =>
        LocalConcreteClaimKind.TacticalLine
      case ClaimFamily.Tactical =>
        LocalConcreteClaimKind.TacticalEngineContext
      case ClaimFamily.Conversion =>
        LocalConcreteClaimKind.ConversionContext
      case ClaimFamily.Material =>
        LocalConcreteClaimKind.MaterialContext
      case _ =>
        LocalConcreteClaimKind.ConcreteContext

  private def nearbyEventClusters(
      packet: EvidenceBackedJudgmentPacket,
      claim: ClaimSeed
  ): List[String] =
    val lines = (claim.primaryLine.toList ++ claim.evidence.flatMap(_.line)).distinct
    val moves = (claim.subjectMove.toList ++ lines.map(_.rootMove)).map(normalizeMove).toSet
    packet.claimEventClusters
      .filter(cluster =>
        lines.exists(line => line == cluster.eventLine || line == cluster.referenceLine || line == cluster.candidateLine) ||
          moves.contains(normalizeMove(cluster.eventRootMove)) ||
          moves.contains(normalizeMove(cluster.referenceLine.rootMove)) ||
          moves.contains(normalizeMove(cluster.candidateLine.rootMove))
      )
      .map(_.id)
      .distinct
      .sorted

  private def normalizeMove(raw: String): String =
    Option(raw).getOrElse("").trim.toLowerCase

  private def openingApplicabilityDiagnostics(
      packet: EvidenceBackedJudgmentPacket
  ): List[OpeningApplicabilityDiagnostic] =
    packet.evidenceGraph.records.collect {
      case EvidenceRecord(ref, ApplicabilityAssessmentEvidence(assessment), parents) =>
        val anchors =
          parents
            .flatMap(parent => packet.evidenceGraph.byId.get(parent.id))
            .collect { case EvidenceRecord(_, FeatureAnchorEvidence(anchor), _) => anchor }
        val supportedAnchors =
          anchors.filter(anchor =>
            assessment.supportedThemes.contains(anchor.theme) &&
              anchor.sourceLayer != EvidenceLayer.Board
          )
        OpeningApplicabilityDiagnostic(
          id = ref.id,
          applicability = assessment.applicability,
          status = assessment.status,
          observedThemes = assessment.observedThemes,
          supportedThemes = assessment.supportedThemes,
          unverifiedPriorThemes = assessment.unverifiedPriorThemes,
          observedOnlyThemes = assessment.observedOnlyThemes,
          anchorSourceLayers = anchors.map(_.sourceLayer).distinct,
          anchorSignals = anchors.map(_.signal).distinct,
          supportedAnchorSourceLayers = supportedAnchors.map(_.sourceLayer).distinct,
          supportedAnchorSignals = supportedAnchors.map(_.signal).distinct,
          internalAnchorAligned = assessment.canCertifyOpeningClaim
        )
    }

  private def playedRelatedComparisonRecords(packet: EvidenceBackedJudgmentPacket): List[EvidenceRecord] =
    val playedMoves = JudgmentSubjectBinding.packetPlayedMoves(packet)
    packet.evidenceGraph.records.collect {
      case record @ EvidenceRecord(_, CandidateComparisonEvidence(_), _)
          if JudgmentSubjectBinding.playedRelated(JudgmentSubjectBinding.recordBinding(record, playedMoves)) =>
        record
    }

  private def primaryPlayedCandidateComparisonRecords(packet: EvidenceBackedJudgmentPacket): List[EvidenceRecord] =
    val playedMoves = JudgmentSubjectBinding.packetPlayedMoves(packet)
    packet.evidenceGraph.records.collect {
      case record @ EvidenceRecord(_, CandidateComparisonEvidence(_), _)
          if JudgmentSubjectBinding.primaryPlayed(JudgmentSubjectBinding.recordBinding(record, playedMoves)) =>
        record
    }

  private def playedRelativeCauseRecords(packet: EvidenceBackedJudgmentPacket): List[EvidenceRecord] =
    val playedMoves = JudgmentSubjectBinding.packetPlayedMoves(packet)
    packet.evidenceGraph.records.collect {
      case record @ EvidenceRecord(ref, RelativeCauseFactEvidence(cause), _)
          if JudgmentSubjectBinding.playedRelated(JudgmentSubjectBinding.recordBinding(record, playedMoves)) =>
        record
    }

enum JudgmentGraphLayer:
  case InputGraph
  case PositionEvidence
  case LineEvaluationEvidence
  case TacticalTransitionEvidence
  case StrategicPlanOpeningEvidence
  case RelativeChoiceAssessment
  case IdeaSynthesis
  case ClaimSynthesis
  case PacketBoundary

enum JudgmentGraphSlot:
  case BeforePositionNode
  case AfterPlayedPositionNode
  case AfterReferencePositionNode
  case AfterAlternativePositionNode
  case PlayedLineNode
  case BestReferenceLineNode
  case AlternativeLineNode
  case PlayedTransitionEdge
  case ReferenceTransitionEdge
  case AlternativeTransitionEdge
  case BoardFact
  case BoardAnchorFact
  case SinglePositionFact
  case BeforeSinglePositionFact
  case AfterPlayedSinglePositionFact
  case AfterReferenceSinglePositionFact
  case PawnStructureFact
  case ThreatPressureFact
  case LineFact
  case LineReplayFact
  case LineEventFact
  case LineConsequenceFact
  case EvalFact
  case LegalReplayLineFact
  case MoveMotifFact
  case MoveTransitionFact
  case RelationFact
  case StructuralDeltaFact
  case StrategicFact
  case OpeningContextFact
  case FeatureAnchorFact
  case ApplicabilityAssessmentFact
  case PlanPressureFact
  case PlanTransitionFact
  case CandidateComparisonFact
  case RelativeAssessmentFact
  case CounterfactualFact
  case RelativeCauseFact
  case RelativeCauseProof
  case MoveVerdictCertificationFact
  case TacticalIdea
  case StrategicIdea
  case PawnStructureIdea
  case OpeningIdea
  case DefensiveIdea
  case ConversionIdea
  case MaterialIdea
  case EvaluationIdea
  case TacticalClaim
  case StrategicClaim
  case PawnStructureClaim
  case OpeningClaim
  case PlanClaim
  case DefensiveClaim
  case ConversionClaim
  case MaterialClaim
  case EvaluationClaim
  case IdeaVerdictSplit
  case ClaimSupportCluster
  case ClaimEventCluster
  case EvidenceLossDiagnostics

enum JudgmentGraphOwner:
  case NodeLineTransitionAssembler
  case PositionFactNormalizer
  case SinglePositionFactNormalizer
  case LineFactNormalizer
  case EvalFactNormalizer
  case MoveMotifNormalizer
  case MoveTransitionNormalizer
  case RelationFactNormalizer
  case StrategicFactNormalizer
  case OpeningContextFactNormalizer
  case FeatureApplicabilityAssembler
  case TransitionFactNormalizer
  case RelativeAssessmentAssembler
  case ChessIdeaAssembler
  case ClaimSeedAssembler
  case JudgmentPacketBuilder
  case EvidenceLossDiagnostics

final case class JudgmentGraphSlotStatus(
    slot: JudgmentGraphSlot,
    owner: JudgmentGraphOwner,
    present: Boolean,
    applicable: Boolean = true
)

final case class JudgmentLayerGapMetric(
    layer: JudgmentGraphLayer,
    slots: List[JudgmentGraphSlotStatus]
):
  def totalSlots: Int = slots.count(_.applicable)
  def presentSlots: Int = slots.count(slot => slot.applicable && slot.present)
  def missingSlots: List[JudgmentGraphSlotStatus] = slots.filter(slot => slot.applicable && !slot.present)
  def gapPercent: Double =
    if totalSlots == 0 then 0d
    else missingSlots.size.toDouble / totalSlots.toDouble * 100d

final case class JudgmentLayerGapProfile(
    layers: List[JudgmentLayerGapMetric]
):
  def totalSlots: Int = layers.map(_.totalSlots).sum
  def presentSlots: Int = layers.map(_.presentSlots).sum
  def missingSlots: List[JudgmentGraphSlotStatus] = layers.flatMap(_.missingSlots)
  def overallGapPercent: Double =
    if totalSlots == 0 then 0d
    else missingSlots.size.toDouble / totalSlots.toDouble * 100d

object JudgmentLayerGapProfile:
  def fromPacket(packet: EvidenceBackedJudgmentPacket): JudgmentLayerGapProfile =
    JudgmentLayerGapProfile(
      layers = List(
        layer(
          JudgmentGraphLayer.InputGraph,
          slot(
            JudgmentGraphSlot.BeforePositionNode,
            JudgmentGraphOwner.NodeLineTransitionAssembler,
            packet.positions.exists(_.role == PositionNodeRole.Before)
          ),
          slot(
            JudgmentGraphSlot.AfterPlayedPositionNode,
            JudgmentGraphOwner.NodeLineTransitionAssembler,
            packet.positions.exists(_.role == PositionNodeRole.AfterPlayed)
          ),
          slot(
            JudgmentGraphSlot.AfterReferencePositionNode,
            JudgmentGraphOwner.NodeLineTransitionAssembler,
            packet.positions.exists(_.role == PositionNodeRole.AfterReference)
          ),
          slot(
            JudgmentGraphSlot.AfterAlternativePositionNode,
            JudgmentGraphOwner.NodeLineTransitionAssembler,
            packet.positions.exists(_.role == PositionNodeRole.AfterAlternative)
          ),
          slot(
            JudgmentGraphSlot.PlayedLineNode,
            JudgmentGraphOwner.NodeLineTransitionAssembler,
            packet.candidateLines.exists(_.role == LineNodeRole.Played)
          ),
          slot(
            JudgmentGraphSlot.BestReferenceLineNode,
            JudgmentGraphOwner.NodeLineTransitionAssembler,
            packet.candidateLines.exists(_.role == LineNodeRole.BestReference)
          ),
          slot(
            JudgmentGraphSlot.AlternativeLineNode,
            JudgmentGraphOwner.NodeLineTransitionAssembler,
            packet.candidateLines.exists(_.role == LineNodeRole.Alternative)
          ),
          slot(
            JudgmentGraphSlot.PlayedTransitionEdge,
            JudgmentGraphOwner.NodeLineTransitionAssembler,
            packet.transitions.exists(_.role == TransitionEdgeRole.Played)
          ),
          slot(
            JudgmentGraphSlot.ReferenceTransitionEdge,
            JudgmentGraphOwner.NodeLineTransitionAssembler,
            packet.transitions.exists(_.role == TransitionEdgeRole.Reference)
          ),
          slot(
            JudgmentGraphSlot.AlternativeTransitionEdge,
            JudgmentGraphOwner.NodeLineTransitionAssembler,
            packet.transitions.exists(_.role == TransitionEdgeRole.Alternative)
          )
        ),
        layer(
          JudgmentGraphLayer.PositionEvidence,
          globalEvidenceLayerSlot(packet, JudgmentGraphSlot.BoardFact, JudgmentGraphOwner.PositionFactNormalizer, EvidenceLayer.Board),
          slot(
            JudgmentGraphSlot.BoardAnchorFact,
            JudgmentGraphOwner.PositionFactNormalizer,
            hasBoardAnchor(packet),
            boardAnchorApplicable(packet)
          ),
          globalEvidenceLayerSlot(
            packet,
            JudgmentGraphSlot.SinglePositionFact,
            JudgmentGraphOwner.SinglePositionFactNormalizer,
            EvidenceLayer.SinglePosition
          ),
          globalPositionEvidenceSlot(
            packet,
            JudgmentGraphSlot.BeforeSinglePositionFact,
            JudgmentGraphOwner.SinglePositionFactNormalizer,
            EvidenceLayer.SinglePosition,
            PositionNodeRole.Before
          ),
          globalPositionEvidenceSlot(
            packet,
            JudgmentGraphSlot.AfterPlayedSinglePositionFact,
            JudgmentGraphOwner.SinglePositionFactNormalizer,
            EvidenceLayer.SinglePosition,
            PositionNodeRole.AfterPlayed
          ),
          globalPositionEvidenceSlot(
            packet,
            JudgmentGraphSlot.AfterReferenceSinglePositionFact,
            JudgmentGraphOwner.SinglePositionFactNormalizer,
            EvidenceLayer.SinglePosition,
            PositionNodeRole.AfterReference
          ),
          globalEvidenceLayerSlot(
            packet,
            JudgmentGraphSlot.PawnStructureFact,
            JudgmentGraphOwner.StrategicFactNormalizer,
            EvidenceLayer.PawnStructure
          ),
          globalEvidenceLayerSlot(
            packet,
            JudgmentGraphSlot.ThreatPressureFact,
            JudgmentGraphOwner.StrategicFactNormalizer,
            EvidenceLayer.ThreatPressure
          )
        ),
        layer(
          JudgmentGraphLayer.LineEvaluationEvidence,
          globalEvidenceLayerSlot(packet, JudgmentGraphSlot.LineFact, JudgmentGraphOwner.LineFactNormalizer, EvidenceLayer.Line),
          slot(
            JudgmentGraphSlot.LineReplayFact,
            JudgmentGraphOwner.LineFactNormalizer,
            hasLineReplay(packet),
            lineFactApplicable(packet)
          ),
          slot(
            JudgmentGraphSlot.LineEventFact,
            JudgmentGraphOwner.LineFactNormalizer,
            hasLineEvent(packet),
            lineEventApplicable(packet)
          ),
          slot(
            JudgmentGraphSlot.LineConsequenceFact,
            JudgmentGraphOwner.LineFactNormalizer,
            hasLineConsequence(packet),
            lineConsequenceApplicable(packet)
          ),
          globalEvidenceLayerSlot(packet, JudgmentGraphSlot.EvalFact, JudgmentGraphOwner.EvalFactNormalizer, EvidenceLayer.Eval),
          slot(
            JudgmentGraphSlot.LegalReplayLineFact,
            JudgmentGraphOwner.LineFactNormalizer,
            packet.evidenceGraph.records.exists(record =>
              record.ref.layer == EvidenceLayer.Line &&
                record.ref.confidence == EvidenceConfidence.LegalReplayVerified
            )
          )
        ),
        layer(
          JudgmentGraphLayer.TacticalTransitionEvidence,
          globalEvidenceLayerSlot(
            packet,
            JudgmentGraphSlot.MoveMotifFact,
            JudgmentGraphOwner.MoveMotifNormalizer,
            EvidenceLayer.MoveMotif
          ),
          globalEvidenceLayerSlot(
            packet,
            JudgmentGraphSlot.MoveTransitionFact,
            JudgmentGraphOwner.MoveTransitionNormalizer,
            EvidenceLayer.MoveTransition
          ),
          globalEvidenceLayerSlot(
            packet,
            JudgmentGraphSlot.RelationFact,
            JudgmentGraphOwner.RelationFactNormalizer,
            EvidenceLayer.Relation,
            relationEvidenceApplicable(packet)
          ),
          globalEvidenceLayerSlot(
            packet,
            JudgmentGraphSlot.StructuralDeltaFact,
            JudgmentGraphOwner.TransitionFactNormalizer,
            EvidenceLayer.StructuralDelta
          )
        ),
        layer(
          JudgmentGraphLayer.StrategicPlanOpeningEvidence,
          globalEvidenceLayerSlot(packet, JudgmentGraphSlot.StrategicFact, JudgmentGraphOwner.StrategicFactNormalizer, EvidenceLayer.Strategic),
          globalEvidenceLayerSlot(
            packet,
            JudgmentGraphSlot.OpeningContextFact,
            JudgmentGraphOwner.OpeningContextFactNormalizer,
            EvidenceLayer.OpeningContext,
            applicable = false
          ),
          globalEvidenceLayerSlot(
            packet,
            JudgmentGraphSlot.FeatureAnchorFact,
            JudgmentGraphOwner.FeatureApplicabilityAssembler,
            EvidenceLayer.FeatureAnchor,
            openingIdeaApplicable(packet)
          ),
          globalEvidenceLayerSlot(
            packet,
            JudgmentGraphSlot.ApplicabilityAssessmentFact,
            JudgmentGraphOwner.FeatureApplicabilityAssembler,
            EvidenceLayer.ApplicabilityAssessment,
            openingIdeaApplicable(packet)
          ),
          globalEvidenceLayerSlot(
            packet,
            JudgmentGraphSlot.PlanPressureFact,
            JudgmentGraphOwner.StrategicFactNormalizer,
            EvidenceLayer.PlanPressure
          ),
          globalEvidenceLayerSlot(
            packet,
            JudgmentGraphSlot.PlanTransitionFact,
            JudgmentGraphOwner.TransitionFactNormalizer,
            EvidenceLayer.PlanTransition
          )
        ),
        layer(
          JudgmentGraphLayer.RelativeChoiceAssessment,
          globalEvidenceLayerSlot(
            packet,
            JudgmentGraphSlot.CandidateComparisonFact,
            JudgmentGraphOwner.RelativeAssessmentAssembler,
            EvidenceLayer.CandidateComparison
          ),
          globalEvidenceLayerSlot(
            packet,
            JudgmentGraphSlot.RelativeAssessmentFact,
            JudgmentGraphOwner.RelativeAssessmentAssembler,
            EvidenceLayer.RelativeAssessment
          ),
          globalEvidenceLayerSlot(
            packet,
            JudgmentGraphSlot.CounterfactualFact,
            JudgmentGraphOwner.RelativeAssessmentAssembler,
            EvidenceLayer.Counterfactual
          ),
          globalEvidenceLayerSlot(
            packet,
            JudgmentGraphSlot.RelativeCauseFact,
            JudgmentGraphOwner.RelativeAssessmentAssembler,
            EvidenceLayer.RelativeCause,
            relativeCauseApplicable(packet)
          ),
          slot(
            JudgmentGraphSlot.RelativeCauseProof,
            JudgmentGraphOwner.RelativeAssessmentAssembler,
            hasRelativeCauseProof(packet),
            relativeCauseApplicable(packet)
          ),
          globalEvidenceLayerSlot(
            packet,
            JudgmentGraphSlot.MoveVerdictCertificationFact,
            JudgmentGraphOwner.RelativeAssessmentAssembler,
            EvidenceLayer.MoveVerdictCertification
          )
        ),
        layer(
          JudgmentGraphLayer.IdeaSynthesis,
          ideaSlot(packet, JudgmentGraphSlot.TacticalIdea, ChessIdeaFamily.Tactical, tacticalIdeaApplicable(packet)),
          ideaSlot(packet, JudgmentGraphSlot.StrategicIdea, ChessIdeaFamily.Strategic),
          ideaSlot(packet, JudgmentGraphSlot.PawnStructureIdea, ChessIdeaFamily.PawnStructure),
          ideaSlot(packet, JudgmentGraphSlot.OpeningIdea, ChessIdeaFamily.Opening, openingIdeaApplicable(packet)),
          ideaSlot(packet, JudgmentGraphSlot.DefensiveIdea, ChessIdeaFamily.Defensive),
          ideaSlot(packet, JudgmentGraphSlot.ConversionIdea, ChessIdeaFamily.Conversion, conversionIdeaApplicable(packet)),
          ideaSlot(packet, JudgmentGraphSlot.MaterialIdea, ChessIdeaFamily.Material, materialIdeaApplicable(packet)),
          ideaSlot(packet, JudgmentGraphSlot.EvaluationIdea, ChessIdeaFamily.Evaluation)
        ),
        layer(
          JudgmentGraphLayer.ClaimSynthesis,
          claimSlot(packet, JudgmentGraphSlot.TacticalClaim, ClaimFamily.Tactical, tacticalIdeaApplicable(packet)),
          claimSlot(packet, JudgmentGraphSlot.StrategicClaim, ClaimFamily.Strategic, strategicClaimApplicable(packet)),
          claimSlot(packet, JudgmentGraphSlot.PawnStructureClaim, ClaimFamily.PawnStructure),
          claimSlot(packet, JudgmentGraphSlot.OpeningClaim, ClaimFamily.Opening, openingIdeaApplicable(packet)),
          claimSlot(packet, JudgmentGraphSlot.PlanClaim, ClaimFamily.Plan, planClaimApplicable(packet)),
          claimSlot(packet, JudgmentGraphSlot.DefensiveClaim, ClaimFamily.Defensive),
          claimSlot(packet, JudgmentGraphSlot.ConversionClaim, ClaimFamily.Conversion, conversionIdeaApplicable(packet)),
          claimSlot(packet, JudgmentGraphSlot.MaterialClaim, ClaimFamily.Material, materialIdeaApplicable(packet)),
          claimSlot(packet, JudgmentGraphSlot.EvaluationClaim, ClaimFamily.Evaluation)
        ),
        layer(
          JudgmentGraphLayer.PacketBoundary,
          slot(
            JudgmentGraphSlot.IdeaVerdictSplit,
            JudgmentGraphOwner.JudgmentPacketBuilder,
            packet.ideaVerdict.nonEmpty
          ),
          slot(
            JudgmentGraphSlot.ClaimSupportCluster,
            JudgmentGraphOwner.JudgmentPacketBuilder,
            packet.claimSupportClusters.nonEmpty,
            longTermSupportClusterApplicable(packet)
          ),
          slot(
            JudgmentGraphSlot.ClaimEventCluster,
            JudgmentGraphOwner.JudgmentPacketBuilder,
            packet.claimEventClusters.nonEmpty,
            claimEventClusterApplicable(packet)
          ),
          slot(
            JudgmentGraphSlot.EvidenceLossDiagnostics,
            JudgmentGraphOwner.EvidenceLossDiagnostics,
            packet.diagnostics.coverage.nonEmpty || packet.evidenceGraph.records.nonEmpty
          )
        )
      )
    )

  private def layer(
      graphLayer: JudgmentGraphLayer,
      slots: JudgmentGraphSlotStatus*
  ): JudgmentLayerGapMetric =
    JudgmentLayerGapMetric(graphLayer, slots.toList)

  private def slot(
      graphSlot: JudgmentGraphSlot,
      owner: JudgmentGraphOwner,
      present: Boolean,
      applicable: Boolean = true
  ): JudgmentGraphSlotStatus =
    JudgmentGraphSlotStatus(graphSlot, owner, present, applicable)

  private def globalEvidenceLayerSlot(
      packet: EvidenceBackedJudgmentPacket,
      graphSlot: JudgmentGraphSlot,
      owner: JudgmentGraphOwner,
      evidenceLayer: EvidenceLayer,
      applicable: Boolean = true
  ): JudgmentGraphSlotStatus =
    slot(graphSlot, owner, packet.evidenceGraph.records.exists(_.ref.layer == evidenceLayer), applicable)

  private def hasBoardAnchor(packet: EvidenceBackedJudgmentPacket): Boolean =
    packet.evidenceGraph.records.exists {
      case EvidenceRecord(_, payload: BoardFactEvidence, _) => payload.boardAnchors.nonEmpty
      case _                                                => false
    }

  private def boardAnchorApplicable(packet: EvidenceBackedJudgmentPacket): Boolean =
    packet.evidenceGraph.records.exists {
      case EvidenceRecord(_, payload: BoardFactEvidence, _) => payload.hasFeatureInput
      case _                                                => false
    }

  private def lineFactApplicable(packet: EvidenceBackedJudgmentPacket): Boolean =
    packet.evidenceGraph.records.exists {
      case EvidenceRecord(_, _: LineFactEvidence, _) => true
      case _                                         => false
    }

  private def hasLineReplay(packet: EvidenceBackedJudgmentPacket): Boolean =
    packet.evidenceGraph.records.exists {
      case EvidenceRecord(_, payload: LineFactEvidence, _) => payload.hasLineReplay
      case _                                               => false
    }

  private def hasLineEvent(packet: EvidenceBackedJudgmentPacket): Boolean =
    packet.evidenceGraph.records.exists {
      case EvidenceRecord(_, payload: LineFactEvidence, _) => payload.hasLineEvents
      case _                                               => false
    }

  private def lineEventApplicable(packet: EvidenceBackedJudgmentPacket): Boolean =
    packet.evidenceGraph.records.exists {
      case EvidenceRecord(_, payload: LineFactEvidence, _) =>
        payload.hasForcedTheme ||
          payload.materialOutcomeProfile.gainSignals.nonEmpty ||
          payload.materialOutcomeProfile.lossSignals.nonEmpty
      case _ =>
        false
    }

  private def hasLineConsequence(packet: EvidenceBackedJudgmentPacket): Boolean =
    packet.evidenceGraph.records.exists {
      case EvidenceRecord(_, payload: LineFactEvidence, _) => payload.hasLineConsequences
      case _                                               => false
    }

  private def lineConsequenceApplicable(packet: EvidenceBackedJudgmentPacket): Boolean =
    packet.evidenceGraph.records.exists {
      case EvidenceRecord(_, payload: LineFactEvidence, _) =>
        payload.hasForcedTheme ||
          payload.consequenceProfile.hasConcreteProofSignal ||
          payload.materialOutcomeProfile.gainSignals.nonEmpty ||
          payload.materialOutcomeProfile.lossSignals.nonEmpty
      case _ =>
        false
    }

  private def hasRelativeCauseProof(packet: EvidenceBackedJudgmentPacket): Boolean =
    packet.evidenceGraph.records.exists {
      case EvidenceRecord(_, RelativeCauseFactEvidence(cause), _) => cause.proof.exists(_.hasTypedDepth)
      case _                                                      => false
    }

  private def globalPositionEvidenceSlot(
      packet: EvidenceBackedJudgmentPacket,
      graphSlot: JudgmentGraphSlot,
      owner: JudgmentGraphOwner,
      evidenceLayer: EvidenceLayer,
      positionRole: PositionNodeRole
  ): JudgmentGraphSlotStatus =
    val positionRefs = packet.positions.collect {
      case position if position.role == positionRole => position.ref
    }.toSet
    slot(
      graphSlot,
      owner,
      packet.evidenceGraph.records.exists(record =>
        record.ref.layer == evidenceLayer && positionRefs.contains(record.ref.position)
      )
    )

  private def ideaSlot(
      packet: EvidenceBackedJudgmentPacket,
      graphSlot: JudgmentGraphSlot,
      family: ChessIdeaFamily,
      applicable: Boolean = true
  ): JudgmentGraphSlotStatus =
    slot(
      graphSlot,
      JudgmentGraphOwner.ChessIdeaAssembler,
      packet.ideas.exists(_.ref.family == family),
      applicable
    )

  private def claimSlot(
      packet: EvidenceBackedJudgmentPacket,
      graphSlot: JudgmentGraphSlot,
      family: ClaimFamily,
      applicable: Boolean = true
  ): JudgmentGraphSlotStatus =
    slot(
      graphSlot,
      JudgmentGraphOwner.ClaimSeedAssembler,
      packet.claims.exists(_.family == family),
      applicable
    )

  private def relationEvidenceApplicable(packet: EvidenceBackedJudgmentPacket): Boolean =
    packet.evidenceGraph.records.exists(_.ref.layer == EvidenceLayer.Relation)

  private def relativeCauseApplicable(packet: EvidenceBackedJudgmentPacket): Boolean =
    packet.evidenceGraph.records.exists(_.ref.layer == EvidenceLayer.RelativeCause) ||
      packet.evidenceGraph.records.exists {
        case EvidenceRecord(_, CandidateComparisonEvidence(fact), _) =>
          fact.comparison.winPercentLossForMover >= JudgmentThresholds.INACCURACY_WP ||
            fact.comparison.candidateSet.exists(_.onlyMove)
        case _ =>
          false
      }

  private def tacticalIdeaApplicable(packet: EvidenceBackedJudgmentPacket): Boolean =
    packet.ideas.exists(_.ref.family == ChessIdeaFamily.Tactical) ||
      packet.claims.exists(_.family == ClaimFamily.Tactical) ||
      packet.evidenceGraph.records.exists {
        case EvidenceRecord(_, RelativeCauseFactEvidence(cause), _) =>
          tacticalCauseApplicable(cause.kind)
        case EvidenceRecord(_, MoveVerdictCertificationEvidence(certification), _) =>
          certification.causes.exists(cause => tacticalCauseApplicable(cause.kind))
        case _ =>
          false
      }

  private def strategicClaimApplicable(packet: EvidenceBackedJudgmentPacket): Boolean =
    packet.claims.exists(_.family == ClaimFamily.Strategic) ||
      packet.ideas.exists(idea => idea.ref.family == ChessIdeaFamily.Strategic && idea.subject != IdeaSubject.Plan)

  private def conversionIdeaApplicable(packet: EvidenceBackedJudgmentPacket): Boolean =
    packet.ideas.exists(_.ref.family == ChessIdeaFamily.Conversion) ||
      packet.claims.exists(_.family == ClaimFamily.Conversion) ||
      packet.evidenceGraph.records.exists {
        case EvidenceRecord(_, RelativeCauseFactEvidence(cause), _) =>
          conversionCauseApplicable(cause.kind)
        case EvidenceRecord(_, MoveVerdictCertificationEvidence(certification), _) =>
          certification.causes.exists(cause => conversionCauseApplicable(cause.kind))
        case EvidenceRecord(_, RelationFactEvidence(RelationFactKind.BadPieceLiquidation, _, _, _, _), _) =>
          true
        case _ =>
          false
      }

  private def materialIdeaApplicable(packet: EvidenceBackedJudgmentPacket): Boolean =
    packet.ideas.exists(_.ref.family == ChessIdeaFamily.Material) ||
      packet.claims.exists(_.family == ClaimFamily.Material) ||
      packet.evidenceGraph.records.exists {
        case EvidenceRecord(_, RelativeCauseFactEvidence(cause), _) =>
          materialResultCause(cause.kind)
        case EvidenceRecord(_, MoveVerdictCertificationEvidence(certification), _) =>
          certification.causes.exists(cause => materialResultCause(cause.kind))
        case _ =>
          false
      }

  private def materialResultCause(kind: RelativeCauseKind): Boolean =
    ClaimEventCluster.kindForCause(kind).contains(ClaimEventClusterKind.MaterialEvent)

  private def longTermSupportClusterApplicable(packet: EvidenceBackedJudgmentPacket): Boolean =
    packet.claims.exists(_.family.isLongTerm)

  private def planClaimApplicable(packet: EvidenceBackedJudgmentPacket): Boolean =
    packet.ideas.exists(_.subject == IdeaSubject.Plan) ||
      ClaimTruthPolicy.planClaimApplicable(packet)

  private def claimEventClusterApplicable(packet: EvidenceBackedJudgmentPacket): Boolean =
    packet.claims.exists(_.family.isEvent) ||
      packet.evidenceGraph.records.exists {
        case EvidenceRecord(_, RelativeCauseFactEvidence(cause), _) =>
          ClaimEventCluster.kindForCause(cause.kind).nonEmpty
        case EvidenceRecord(_, MoveVerdictCertificationEvidence(certification), _) =>
          certification.causes.exists(cause => ClaimEventCluster.kindForCause(cause.kind).nonEmpty)
        case _ =>
          false
      }

  private def openingIdeaApplicable(packet: EvidenceBackedJudgmentPacket): Boolean =
    packet.ideas.exists(_.ref.family == ChessIdeaFamily.Opening) ||
      packet.claims.exists(_.family == ClaimFamily.Opening) ||
      packet.evidenceGraph.records.exists {
        case EvidenceRecord(_, ApplicabilityAssessmentEvidence(assessment), _) =>
          assessment.canCertifyOpeningClaim
        case _ =>
          false
      }

  private def tacticalCauseApplicable(kind: RelativeCauseKind): Boolean =
    ClaimEventCluster.kindForCause(kind).contains(ClaimEventClusterKind.TacticalEvent)

  private def conversionCauseApplicable(kind: RelativeCauseKind): Boolean =
    ClaimEventCluster.kindForCause(kind).contains(ClaimEventClusterKind.ConversionEvent)

enum ChessQualityIssueKind:
  case PacketValidationFailed
  case MissingRelativeAssessment
  case MissingReferenceLine
  case MissingPlayedLine
  case MissingLegalReplayLine
  case MissingClaim
  case MissingTacticalOrStrategicCoverage
  case MissingCandidateComparison
  case MissingRelativeCause
  case UnexplainedRelativeCause
  case UnexpectedEvidenceLoss

final case class ChessQualityIssue(
    kind: ChessQualityIssueKind,
    subjectId: String,
    validationKind: Option[JudgmentPacketValidationIssueKind] = None,
    evidence: Option[EvidenceRef] = None
)

final case class ChessQualityAudit(
    issues: List[ChessQualityIssue]
):
  def isClean: Boolean = issues.isEmpty

object ChessQualityAudit:
  def from(
      packet: EvidenceBackedJudgmentPacket,
      validation: JudgmentPacketValidationResult,
      graphLoss: GraphLossMetrics,
      semantic: SemanticCoverageMetrics
  ): ChessQualityAudit =
    val issues =
      List.concat(
        validationIssues(validation),
        requiredRelativeAssessment(packet),
        requiredLines(packet),
        legalReplayCoverage(packet),
        relativeBreadthCoverage(packet, semantic),
        claimCoverage(packet),
        ideaCoverage(semantic),
        unexpectedEvidenceLoss(graphLoss)
      )
    ChessQualityAudit(issues.distinct)

  private def validationIssues(validation: JudgmentPacketValidationResult): List[ChessQualityIssue] =
    validation.issues.map(issue =>
      ChessQualityIssue(
        kind = ChessQualityIssueKind.PacketValidationFailed,
        subjectId = issue.subjectId,
        validationKind = Some(issue.kind),
        evidence = issue.evidence
      )
    )

  private def requiredRelativeAssessment(packet: EvidenceBackedJudgmentPacket): List[ChessQualityIssue] =
    Option
      .when(packet.relativeAssessments.isEmpty)(
        ChessQualityIssue(ChessQualityIssueKind.MissingRelativeAssessment, "relative-assessment")
      )
      .toList

  private def requiredLines(packet: EvidenceBackedJudgmentPacket): List[ChessQualityIssue] =
    List(
      Option.when(!packet.candidateLines.exists(_.role == LineNodeRole.BestReference))(
        ChessQualityIssue(ChessQualityIssueKind.MissingReferenceLine, "best-reference-line")
      ),
      Option.when(!packet.candidateLines.exists(_.role == LineNodeRole.Played))(
        ChessQualityIssue(ChessQualityIssueKind.MissingPlayedLine, "played-line")
      )
    ).flatten

  private def legalReplayCoverage(packet: EvidenceBackedJudgmentPacket): List[ChessQualityIssue] =
    val hasLegalReplayLine =
      packet.evidenceGraph.records.exists(record =>
        record.ref.layer == EvidenceLayer.Line &&
          record.ref.confidence == EvidenceConfidence.LegalReplayVerified
      )
    Option
      .when(packet.candidateLines.nonEmpty && !hasLegalReplayLine)(
        ChessQualityIssue(ChessQualityIssueKind.MissingLegalReplayLine, "legal-replay-line")
      )
      .toList

  private def relativeBreadthCoverage(
      packet: EvidenceBackedJudgmentPacket,
      semantic: SemanticCoverageMetrics
  ): List[ChessQualityIssue] =
    val significantRelative =
      packet.relativeAssessments.exists(assessment =>
        assessment.comparison.winPercentLossForMover >= JudgmentThresholds.INACCURACY_WP ||
          assessment.comparison.candidateWinPercentDeltaForMover >= JudgmentThresholds.PLAYABLE_LOSS_WP ||
          assessment.comparison.candidateSet.exists(_.onlyMove)
      )
    List(
      Option.when(significantRelative && semantic.playedRelatedComparisonFacts == 0)(
        ChessQualityIssue(ChessQualityIssueKind.MissingCandidateComparison, "candidate-comparison")
      ),
      Option.when(significantRelative && semantic.playedRelativeCauseFacts == 0)(
        ChessQualityIssue(ChessQualityIssueKind.MissingRelativeCause, "relative-cause")
      ),
      Option.when(semantic.hasPlayedUnexplainedEngineGap)(
        ChessQualityIssue(ChessQualityIssueKind.UnexplainedRelativeCause, "relative-cause")
      )
    ).flatten

  private def claimCoverage(packet: EvidenceBackedJudgmentPacket): List[ChessQualityIssue] =
    Option
      .when(packet.ideas.nonEmpty && packet.claims.isEmpty)(
        ChessQualityIssue(ChessQualityIssueKind.MissingClaim, "claim")
      )
      .toList

  private def ideaCoverage(semantic: SemanticCoverageMetrics): List[ChessQualityIssue] =
    val hasChessIdeaCoverage =
      semantic.tacticalIdeas > 0 ||
        semantic.strategicIdeas > 0 ||
        semantic.pawnStructureIdeas > 0 ||
        semantic.defensiveIdeas > 0 ||
        semantic.conversionIdeas > 0 ||
        semantic.materialIdeas > 0
    Option
      .when(!hasChessIdeaCoverage)(
        ChessQualityIssue(ChessQualityIssueKind.MissingTacticalOrStrategicCoverage, "idea-coverage")
      )
      .toList

  private def unexpectedEvidenceLoss(graphLoss: GraphLossMetrics): List[ChessQualityIssue] =
    Option
      .when(graphLoss.unexpectedLoss > 0)(
        ChessQualityIssue(ChessQualityIssueKind.UnexpectedEvidenceLoss, "evidence-loss")
      )
      .toList

final case class JudgmentQualityReport(
    validation: JudgmentPacketValidationResult,
    graphLoss: GraphLossMetrics,
    claimPromotion: ClaimPromotionMetrics,
    semanticCoverage: SemanticCoverageMetrics,
    layerGaps: JudgmentLayerGapProfile,
    audit: ChessQualityAudit,
    evidenceLoss: List[EvidenceLossClassification]
)

object JudgmentQualityReport:
  def fromPacket(packet: EvidenceBackedJudgmentPacket): JudgmentQualityReport =
    fromPacket(packet, JudgmentPacketValidator.validate(packet))

  def fromPacket(
      packet: EvidenceBackedJudgmentPacket,
      validation: JudgmentPacketValidationResult
  ): JudgmentQualityReport =
    val evidenceLoss = ExpectedEvidenceLossPolicy.classify(packet.diagnostics, packet)
    val graphLoss = GraphLossMetrics.from(packet.diagnostics, evidenceLoss)
    val claimPromotion = ClaimPromotionMetrics.from(packet)
    val semanticCoverage = SemanticCoverageMetrics.from(packet)
    val layerGaps = JudgmentLayerGapProfile.fromPacket(packet)
    val audit =
      ChessQualityAudit.from(
        packet = packet,
        validation = validation,
        graphLoss = graphLoss,
        semantic = semanticCoverage
      )
    JudgmentQualityReport(
      validation = validation,
      graphLoss = graphLoss,
      claimPromotion = claimPromotion,
      semanticCoverage = semanticCoverage,
      layerGaps = layerGaps,
      audit = audit,
      evidenceLoss = evidenceLoss
    )
