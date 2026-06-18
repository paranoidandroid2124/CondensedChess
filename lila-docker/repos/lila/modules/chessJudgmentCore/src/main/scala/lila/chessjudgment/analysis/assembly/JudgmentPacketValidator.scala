package lila.chessjudgment.analysis.assembly

import lila.chessjudgment.model.judgment.*

enum JudgmentPacketValidationIssueKind:
  case MissingRootPosition
  case EmptyEvidenceGraph
  case MissingAttachedEvidence
  case MissingIdeaEvidence
  case MissingClaimEvidence
  case MissingClaimIdea
  case MissingRelativeEvidence

final case class JudgmentPacketValidationIssue(
    kind: JudgmentPacketValidationIssueKind,
    subjectId: String,
    evidence: Option[EvidenceRef] = None
)

final case class JudgmentPacketValidationResult(
    issues: List[JudgmentPacketValidationIssue]
):
  def isValid: Boolean = issues.isEmpty

object JudgmentPacketValidationResult:
  val valid: JudgmentPacketValidationResult = JudgmentPacketValidationResult(Nil)

object JudgmentPacketValidator:

  def validate(packet: EvidenceBackedJudgmentPacket): JudgmentPacketValidationResult =
    val graphIds = packet.evidenceGraph.records.map(_.ref.id).toSet
    val ideaIds = packet.ideas.map(_.ref).toSet
    val issues =
      List.concat(
        rootIssues(packet),
        emptyGraphIssues(packet),
        missingAttachedEvidence(packet, graphIds),
        missingIdeaEvidence(packet, graphIds),
        missingClaimEvidence(packet, graphIds),
        missingClaimIdeas(packet, ideaIds),
        missingRelativeEvidence(packet, graphIds)
      )
    JudgmentPacketValidationResult(issues.distinct)

  private def rootIssues(packet: EvidenceBackedJudgmentPacket): List[JudgmentPacketValidationIssue] =
    Option
      .when(!packet.positions.exists(_.ref == packet.root))(
        JudgmentPacketValidationIssue(JudgmentPacketValidationIssueKind.MissingRootPosition, "root-position")
      )
      .toList

  private def emptyGraphIssues(packet: EvidenceBackedJudgmentPacket): List[JudgmentPacketValidationIssue] =
    Option
      .when(packet.evidenceGraph.records.isEmpty)(
        JudgmentPacketValidationIssue(JudgmentPacketValidationIssueKind.EmptyEvidenceGraph, "evidence-graph")
      )
      .toList

  private def missingAttachedEvidence(
      packet: EvidenceBackedJudgmentPacket,
      graphIds: Set[String]
  ): List[JudgmentPacketValidationIssue] =
    val attached =
      packet.positions.flatMap(_.evidence) ++
        packet.candidateLines.map(_.evidence) ++
        packet.transitions.map(_.evidence)
    missing(attached, graphIds, JudgmentPacketValidationIssueKind.MissingAttachedEvidence)

  private def missingIdeaEvidence(
      packet: EvidenceBackedJudgmentPacket,
      graphIds: Set[String]
  ): List[JudgmentPacketValidationIssue] =
    missing(
      packet.ideas.flatMap(_.evidence),
      graphIds,
      JudgmentPacketValidationIssueKind.MissingIdeaEvidence
    )

  private def missingClaimEvidence(
      packet: EvidenceBackedJudgmentPacket,
      graphIds: Set[String]
  ): List[JudgmentPacketValidationIssue] =
    missing(
      packet.claims.flatMap(_.evidence),
      graphIds,
      JudgmentPacketValidationIssueKind.MissingClaimEvidence
    )

  private def missingRelativeEvidence(
      packet: EvidenceBackedJudgmentPacket,
      graphIds: Set[String]
  ): List[JudgmentPacketValidationIssue] =
    val refs =
      packet.relativeAssessments.flatMap(assessment => assessment.evidence :: assessment.counterfactualEvidence)
    missing(refs, graphIds, JudgmentPacketValidationIssueKind.MissingRelativeEvidence)

  private def missingClaimIdeas(
      packet: EvidenceBackedJudgmentPacket,
      ideaIds: Set[ChessIdeaRef]
  ): List[JudgmentPacketValidationIssue] =
    packet.claims.flatMap { claim =>
      claim.idea.filterNot(ideaIds.contains).map { idea =>
        JudgmentPacketValidationIssue(
          kind = JudgmentPacketValidationIssueKind.MissingClaimIdea,
          subjectId = idea.id
        )
      }
    }

  private def missing(
      refs: List[EvidenceRef],
      graphIds: Set[String],
      kind: JudgmentPacketValidationIssueKind
  ): List[JudgmentPacketValidationIssue] =
    refs.distinctBy(_.id).filterNot(ref => graphIds.contains(ref.id)).map { ref =>
      JudgmentPacketValidationIssue(kind = kind, subjectId = ref.id, evidence = Some(ref))
    }
