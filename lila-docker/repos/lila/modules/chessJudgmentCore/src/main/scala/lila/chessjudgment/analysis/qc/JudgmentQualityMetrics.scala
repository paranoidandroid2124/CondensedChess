package lila.chessjudgment.analysis.qc

import lila.chessjudgment.analysis.assembly.{ JudgmentPacketValidationResult, JudgmentPacketValidator }
import lila.chessjudgment.model.judgment.*

enum EvidenceLossExpectation:
  case Expected
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
      EvidenceLayer.MoveTransition
    )

  def classify(report: EvidenceLossReport): List[EvidenceLossClassification] =
    report.diagnostics.map { diagnostic =>
      EvidenceLossClassification(
        diagnostic = diagnostic,
        expectation = expectationFor(diagnostic)
      )
    }

  private def expectationFor(diagnostic: EvidenceLossDiagnostic): EvidenceLossExpectation =
    diagnostic.reason match
      case EvidenceLossReason.EvidenceAvailableWithoutIdea
          if diagnostic.layer.exists(supportOnlyLayers.contains) =>
        EvidenceLossExpectation.Expected
      case _ =>
        EvidenceLossExpectation.Unexpected

final case class GraphLossMetrics(
    totalRegistered: Int,
    promotedToIdea: Int,
    promotedToClaim: Int,
    unpromoted: Int,
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
    GraphLossMetrics(
      totalRegistered = totalRegistered,
      promotedToIdea = promotedToIdea,
      promotedToClaim = promotedToClaim,
      unpromoted = unpromoted,
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

final case class SemanticCoverageMetrics(
    tacticalIdeas: Int,
    strategicIdeas: Int,
    pawnStructureIdeas: Int,
    defensiveIdeas: Int,
    evaluationIdeas: Int,
    conversionIdeas: Int,
    claimFamilies: Set[ClaimFamily],
    hasRelativeAssessment: Boolean,
    hasVerdict: Boolean,
    hasCandidateSetComparison: Boolean,
    hasOnlyMoveSignal: Boolean
)

object SemanticCoverageMetrics:
  def from(packet: EvidenceBackedJudgmentPacket): SemanticCoverageMetrics =
    SemanticCoverageMetrics(
      tacticalIdeas = countIdeas(packet, ChessIdeaFamily.Tactical),
      strategicIdeas = countIdeas(packet, ChessIdeaFamily.Strategic),
      pawnStructureIdeas = countIdeas(packet, ChessIdeaFamily.PawnStructure),
      defensiveIdeas = countIdeas(packet, ChessIdeaFamily.Defensive),
      evaluationIdeas = countIdeas(packet, ChessIdeaFamily.Evaluation),
      conversionIdeas = countIdeas(packet, ChessIdeaFamily.Conversion),
      claimFamilies = packet.claims.map(_.family).toSet,
      hasRelativeAssessment = packet.relativeAssessments.nonEmpty,
      hasVerdict = packet.ideaVerdict.flatMap(_.verdict).nonEmpty,
      hasCandidateSetComparison = packet.relativeAssessments.exists(_.comparison.candidateSet.nonEmpty),
      hasOnlyMoveSignal = packet.relativeAssessments.exists(_.comparison.candidateSet.exists(_.onlyMove))
    )

  private def countIdeas(packet: EvidenceBackedJudgmentPacket, family: ChessIdeaFamily): Int =
    packet.ideas.count(_.ref.family == family)

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
  case SinglePositionFact
  case BeforeSinglePositionFact
  case AfterPlayedSinglePositionFact
  case AfterReferenceSinglePositionFact
  case PawnStructureFact
  case ThreatPressureFact
  case LineFact
  case EvalFact
  case LegalReplayLineFact
  case MoveMotifFact
  case MoveTransitionFact
  case RelationFact
  case StructuralDeltaFact
  case StrategicFact
  case OpeningRouteFact
  case PlanPressureFact
  case PlanTransitionFact
  case RelativeAssessmentFact
  case CounterfactualFact
  case TacticalIdea
  case StrategicIdea
  case PawnStructureIdea
  case OpeningIdea
  case DefensiveIdea
  case ConversionIdea
  case EvaluationIdea
  case TacticalClaim
  case StrategicClaim
  case PawnStructureClaim
  case OpeningClaim
  case PlanClaim
  case DefensiveClaim
  case ConversionClaim
  case EvaluationClaim
  case IdeaVerdictSplit
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
  case OpeningRouteFactNormalizer
  case TransitionFactNormalizer
  case RelativeAssessmentAssembler
  case ChessIdeaAssembler
  case ClaimSeedAssembler
  case JudgmentPacketBuilder
  case EvidenceLossDiagnostics

final case class JudgmentGraphSlotStatus(
    slot: JudgmentGraphSlot,
    owner: JudgmentGraphOwner,
    present: Boolean
)

final case class JudgmentLayerGapMetric(
    layer: JudgmentGraphLayer,
    slots: List[JudgmentGraphSlotStatus]
):
  def totalSlots: Int = slots.size
  def presentSlots: Int = slots.count(_.present)
  def missingSlots: List[JudgmentGraphSlotStatus] = slots.filterNot(_.present)
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
          evidenceSlot(packet, JudgmentGraphSlot.BoardFact, JudgmentGraphOwner.PositionFactNormalizer, EvidenceLayer.Board),
          evidenceSlot(
            packet,
            JudgmentGraphSlot.SinglePositionFact,
            JudgmentGraphOwner.SinglePositionFactNormalizer,
            EvidenceLayer.SinglePosition
          ),
          positionEvidenceSlot(
            packet,
            JudgmentGraphSlot.BeforeSinglePositionFact,
            JudgmentGraphOwner.SinglePositionFactNormalizer,
            EvidenceLayer.SinglePosition,
            PositionNodeRole.Before
          ),
          positionEvidenceSlot(
            packet,
            JudgmentGraphSlot.AfterPlayedSinglePositionFact,
            JudgmentGraphOwner.SinglePositionFactNormalizer,
            EvidenceLayer.SinglePosition,
            PositionNodeRole.AfterPlayed
          ),
          positionEvidenceSlot(
            packet,
            JudgmentGraphSlot.AfterReferenceSinglePositionFact,
            JudgmentGraphOwner.SinglePositionFactNormalizer,
            EvidenceLayer.SinglePosition,
            PositionNodeRole.AfterReference
          ),
          evidenceSlot(
            packet,
            JudgmentGraphSlot.PawnStructureFact,
            JudgmentGraphOwner.StrategicFactNormalizer,
            EvidenceLayer.PawnStructure
          ),
          evidenceSlot(
            packet,
            JudgmentGraphSlot.ThreatPressureFact,
            JudgmentGraphOwner.StrategicFactNormalizer,
            EvidenceLayer.ThreatPressure
          )
        ),
        layer(
          JudgmentGraphLayer.LineEvaluationEvidence,
          evidenceSlot(packet, JudgmentGraphSlot.LineFact, JudgmentGraphOwner.LineFactNormalizer, EvidenceLayer.Line),
          evidenceSlot(packet, JudgmentGraphSlot.EvalFact, JudgmentGraphOwner.EvalFactNormalizer, EvidenceLayer.Eval),
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
          evidenceSlot(
            packet,
            JudgmentGraphSlot.MoveMotifFact,
            JudgmentGraphOwner.MoveMotifNormalizer,
            EvidenceLayer.MoveMotif
          ),
          evidenceSlot(
            packet,
            JudgmentGraphSlot.MoveTransitionFact,
            JudgmentGraphOwner.MoveTransitionNormalizer,
            EvidenceLayer.MoveTransition
          ),
          evidenceSlot(packet, JudgmentGraphSlot.RelationFact, JudgmentGraphOwner.RelationFactNormalizer, EvidenceLayer.Relation),
          evidenceSlot(
            packet,
            JudgmentGraphSlot.StructuralDeltaFact,
            JudgmentGraphOwner.TransitionFactNormalizer,
            EvidenceLayer.StructuralDelta
          )
        ),
        layer(
          JudgmentGraphLayer.StrategicPlanOpeningEvidence,
          evidenceSlot(packet, JudgmentGraphSlot.StrategicFact, JudgmentGraphOwner.StrategicFactNormalizer, EvidenceLayer.Strategic),
          evidenceSlot(
            packet,
            JudgmentGraphSlot.OpeningRouteFact,
            JudgmentGraphOwner.OpeningRouteFactNormalizer,
            EvidenceLayer.OpeningRoute
          ),
          evidenceSlot(
            packet,
            JudgmentGraphSlot.PlanPressureFact,
            JudgmentGraphOwner.StrategicFactNormalizer,
            EvidenceLayer.PlanPressure
          ),
          evidenceSlot(
            packet,
            JudgmentGraphSlot.PlanTransitionFact,
            JudgmentGraphOwner.TransitionFactNormalizer,
            EvidenceLayer.PlanTransition
          )
        ),
        layer(
          JudgmentGraphLayer.RelativeChoiceAssessment,
          evidenceSlot(
            packet,
            JudgmentGraphSlot.RelativeAssessmentFact,
            JudgmentGraphOwner.RelativeAssessmentAssembler,
            EvidenceLayer.RelativeAssessment
          ),
          evidenceSlot(
            packet,
            JudgmentGraphSlot.CounterfactualFact,
            JudgmentGraphOwner.RelativeAssessmentAssembler,
            EvidenceLayer.Counterfactual
          )
        ),
        layer(
          JudgmentGraphLayer.IdeaSynthesis,
          ideaSlot(packet, JudgmentGraphSlot.TacticalIdea, ChessIdeaFamily.Tactical),
          ideaSlot(packet, JudgmentGraphSlot.StrategicIdea, ChessIdeaFamily.Strategic),
          ideaSlot(packet, JudgmentGraphSlot.PawnStructureIdea, ChessIdeaFamily.PawnStructure),
          ideaSlot(packet, JudgmentGraphSlot.OpeningIdea, ChessIdeaFamily.Opening),
          ideaSlot(packet, JudgmentGraphSlot.DefensiveIdea, ChessIdeaFamily.Defensive),
          ideaSlot(packet, JudgmentGraphSlot.ConversionIdea, ChessIdeaFamily.Conversion),
          ideaSlot(packet, JudgmentGraphSlot.EvaluationIdea, ChessIdeaFamily.Evaluation)
        ),
        layer(
          JudgmentGraphLayer.ClaimSynthesis,
          claimSlot(packet, JudgmentGraphSlot.TacticalClaim, ClaimFamily.Tactical),
          claimSlot(packet, JudgmentGraphSlot.StrategicClaim, ClaimFamily.Strategic),
          claimSlot(packet, JudgmentGraphSlot.PawnStructureClaim, ClaimFamily.PawnStructure),
          claimSlot(packet, JudgmentGraphSlot.OpeningClaim, ClaimFamily.Opening),
          claimSlot(packet, JudgmentGraphSlot.PlanClaim, ClaimFamily.Plan),
          claimSlot(packet, JudgmentGraphSlot.DefensiveClaim, ClaimFamily.Defensive),
          claimSlot(packet, JudgmentGraphSlot.ConversionClaim, ClaimFamily.Conversion),
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
      present: Boolean
  ): JudgmentGraphSlotStatus =
    JudgmentGraphSlotStatus(graphSlot, owner, present)

  private def evidenceSlot(
      packet: EvidenceBackedJudgmentPacket,
      graphSlot: JudgmentGraphSlot,
      owner: JudgmentGraphOwner,
      evidenceLayer: EvidenceLayer
  ): JudgmentGraphSlotStatus =
    slot(graphSlot, owner, packet.evidenceGraph.records.exists(_.ref.layer == evidenceLayer))

  private def positionEvidenceSlot(
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
      family: ChessIdeaFamily
  ): JudgmentGraphSlotStatus =
    slot(
      graphSlot,
      JudgmentGraphOwner.ChessIdeaAssembler,
      packet.ideas.exists(_.ref.family == family)
    )

  private def claimSlot(
      packet: EvidenceBackedJudgmentPacket,
      graphSlot: JudgmentGraphSlot,
      family: ClaimFamily
  ): JudgmentGraphSlotStatus =
    slot(
      graphSlot,
      JudgmentGraphOwner.ClaimSeedAssembler,
      packet.claims.exists(_.family == family)
    )

enum ChessQualityIssueKind:
  case PacketValidationFailed
  case MissingRelativeAssessment
  case MissingReferenceLine
  case MissingPlayedLine
  case MissingLegalReplayLine
  case MissingClaim
  case MissingTacticalOrStrategicCoverage
  case UnexpectedEvidenceLoss

final case class ChessQualityIssue(
    kind: ChessQualityIssueKind,
    subjectId: String
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
        claimCoverage(packet),
        ideaCoverage(semantic),
        unexpectedEvidenceLoss(graphLoss)
      )
    ChessQualityAudit(issues.distinct)

  private def validationIssues(validation: JudgmentPacketValidationResult): List[ChessQualityIssue] =
    validation.issues.map(issue =>
      ChessQualityIssue(
        kind = ChessQualityIssueKind.PacketValidationFailed,
        subjectId = issue.subjectId
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
        semantic.defensiveIdeas > 0
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
    val evidenceLoss = ExpectedEvidenceLossPolicy.classify(packet.diagnostics)
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
