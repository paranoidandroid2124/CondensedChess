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
    hasVerdict: Boolean
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
      hasVerdict = packet.ideaVerdict.flatMap(_.verdict).nonEmpty
    )

  private def countIdeas(packet: EvidenceBackedJudgmentPacket, family: ChessIdeaFamily): Int =
    packet.ideas.count(_.ref.family == family)

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
      audit = audit,
      evidenceLoss = evidenceLoss
    )
