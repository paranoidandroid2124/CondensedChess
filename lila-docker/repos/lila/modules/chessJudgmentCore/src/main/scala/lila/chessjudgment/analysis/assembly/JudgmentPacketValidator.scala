package lila.chessjudgment.analysis.assembly

import _root_.chess.format.Fen
import _root_.chess.variant.Standard
import lila.chessjudgment.model.{ ProbePurpose, ProbeRequest }
import lila.chessjudgment.model.judgment.*

enum JudgmentPacketValidationIssueKind:
  case MissingRootPosition
  case EmptyEvidenceGraph
  case MissingAttachedEvidence
  case MissingIdeaEvidence
  case MissingClaimEvidence
  case MissingClaimIdea
  case MissingClaimClusterEvidence
  case MissingClaimClusterClaim
  case MissingClaimClusterIdea
  case EmptyClaimClusterAnchor
  case NonLongTermClaimClusterAnchor
  case MissingClaimEventClusterEvidence
  case MissingClaimEventClusterClaim
  case MissingClaimEventClusterIdea
  case EmptyClaimEventClusterMember
  case EmptyClaimEventClusterCause
  case NonConcreteClaimEventClusterMember
  case LongTermClaimEventClusterEvidence
  case MismatchedClaimEventClusterKind
  case MissingRelativeEvidence
  case InvalidProbeRequest

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
    val claimIds = packet.claims.map(_.id).toSet
    val claimsById = packet.claims.map(claim => claim.id -> claim).toMap
    val issues =
      List.concat(
        rootIssues(packet),
        emptyGraphIssues(packet),
        missingAttachedEvidence(packet, graphIds),
        missingIdeaEvidence(packet, graphIds),
        missingClaimEvidence(packet, graphIds),
        missingClaimIdeas(packet, ideaIds),
        missingClaimClusterEvidence(packet, graphIds),
        missingClaimClusterClaims(packet, claimIds),
        missingClaimClusterIdeas(packet, ideaIds),
        emptyClaimClusterAnchors(packet),
        nonLongTermClusterAnchors(packet, claimsById),
        missingClaimEventClusterEvidence(packet, graphIds),
        missingClaimEventClusterClaims(packet, claimIds),
        missingClaimEventClusterIdeas(packet, ideaIds),
        emptyClaimEventClusterMembers(packet),
        emptyClaimEventClusterCauses(packet),
        nonConcreteClaimEventClusterMembers(packet, claimsById),
        longTermClaimEventClusterEvidence(packet),
        mismatchedClaimEventClusterKinds(packet),
        missingRelativeEvidence(packet, graphIds),
        invalidProbeRequests(packet)
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
      packet.relativeAssessments.flatMap(assessment =>
        assessment.evidence ::
          (assessment.counterfactualEvidence ++
            assessment.candidateComparisonEvidence ++
            assessment.relativeCauseEvidence ++
            assessment.verdictCertificationEvidence.toList)
      )
    missing(refs, graphIds, JudgmentPacketValidationIssueKind.MissingRelativeEvidence)

  private def missingClaimIdeas(
      packet: EvidenceBackedJudgmentPacket,
      ideaIds: Set[ChessIdeaRef]
  ): List[JudgmentPacketValidationIssue] =
    packet.claims.flatMap { claim =>
      claim.ideaRefs.filterNot(ideaIds.contains).map { idea =>
        JudgmentPacketValidationIssue(
          kind = JudgmentPacketValidationIssueKind.MissingClaimIdea,
          subjectId = idea.id
        )
      }
    }

  private def missingClaimClusterEvidence(
      packet: EvidenceBackedJudgmentPacket,
      graphIds: Set[String]
  ): List[JudgmentPacketValidationIssue] =
    missing(
      packet.claimSupportClusters.flatMap(_.evidence),
      graphIds,
      JudgmentPacketValidationIssueKind.MissingClaimClusterEvidence
    )

  private def missingClaimClusterClaims(
      packet: EvidenceBackedJudgmentPacket,
      claimIds: Set[String]
  ): List[JudgmentPacketValidationIssue] =
    packet.claimSupportClusters.flatMap { cluster =>
      val ids = cluster.anchorClaimIds ++ cluster.supportingClaimIds ++ cluster.constrainingClaimIds
      ids.distinct.filterNot(claimIds.contains).map { claimId =>
        JudgmentPacketValidationIssue(
          kind = JudgmentPacketValidationIssueKind.MissingClaimClusterClaim,
          subjectId = claimId
        )
      }
    }

  private def missingClaimClusterIdeas(
      packet: EvidenceBackedJudgmentPacket,
      ideaIds: Set[ChessIdeaRef]
  ): List[JudgmentPacketValidationIssue] =
    packet.claimSupportClusters.flatMap { cluster =>
      cluster.ideas.filterNot(ideaIds.contains).map { idea =>
        JudgmentPacketValidationIssue(
          kind = JudgmentPacketValidationIssueKind.MissingClaimClusterIdea,
          subjectId = idea.id
        )
      }
    }

  private def emptyClaimClusterAnchors(
      packet: EvidenceBackedJudgmentPacket
  ): List[JudgmentPacketValidationIssue] =
    packet.claimSupportClusters.filter(_.anchorClaimIds.isEmpty).map { cluster =>
      JudgmentPacketValidationIssue(
        kind = JudgmentPacketValidationIssueKind.EmptyClaimClusterAnchor,
        subjectId = cluster.id
      )
    }

  private def nonLongTermClusterAnchors(
      packet: EvidenceBackedJudgmentPacket,
      claimsById: Map[String, ClaimSeed]
  ): List[JudgmentPacketValidationIssue] =
    val longTermFamilies =
      Set(ClaimFamily.Strategic, ClaimFamily.PawnStructure, ClaimFamily.Opening, ClaimFamily.Plan)
    packet.claimSupportClusters.flatMap { cluster =>
      cluster.anchorClaimIds.flatMap(claimsById.get).filterNot(claim => longTermFamilies.contains(claim.family)).map { claim =>
        JudgmentPacketValidationIssue(
          kind = JudgmentPacketValidationIssueKind.NonLongTermClaimClusterAnchor,
          subjectId = claim.id
        )
      }
    }

  private def missingClaimEventClusterEvidence(
      packet: EvidenceBackedJudgmentPacket,
      graphIds: Set[String]
  ): List[JudgmentPacketValidationIssue] =
    missing(
      packet.claimEventClusters.flatMap(cluster =>
        cluster.evidence ++ cluster.interactions.flatMap(_.evidence)
      ),
      graphIds,
      JudgmentPacketValidationIssueKind.MissingClaimEventClusterEvidence
    )

  private def missingClaimEventClusterClaims(
      packet: EvidenceBackedJudgmentPacket,
      claimIds: Set[String]
  ): List[JudgmentPacketValidationIssue] =
    packet.claimEventClusters.flatMap { cluster =>
      val ids =
          cluster.memberClaimIds ++
          cluster.causeClaimIds ++
          cluster.evaluationClaimIds ++
          cluster.witnessClaimIds ++
          cluster.interactions.flatMap(interaction => List(interaction.sourceClaimId, interaction.targetClaimId))
      ids.distinct.filterNot(claimIds.contains).map { claimId =>
        JudgmentPacketValidationIssue(
          kind = JudgmentPacketValidationIssueKind.MissingClaimEventClusterClaim,
          subjectId = claimId
        )
      }
    }

  private def missingClaimEventClusterIdeas(
      packet: EvidenceBackedJudgmentPacket,
      ideaIds: Set[ChessIdeaRef]
  ): List[JudgmentPacketValidationIssue] =
    packet.claimEventClusters.flatMap { cluster =>
      cluster.ideas.filterNot(ideaIds.contains).map { idea =>
        JudgmentPacketValidationIssue(
          kind = JudgmentPacketValidationIssueKind.MissingClaimEventClusterIdea,
          subjectId = idea.id
        )
      }
    }

  private def emptyClaimEventClusterMembers(
      packet: EvidenceBackedJudgmentPacket
  ): List[JudgmentPacketValidationIssue] =
    packet.claimEventClusters.filter(_.memberClaimIds.isEmpty).map { cluster =>
      JudgmentPacketValidationIssue(
        kind = JudgmentPacketValidationIssueKind.EmptyClaimEventClusterMember,
        subjectId = cluster.id
      )
    }

  private def emptyClaimEventClusterCauses(
      packet: EvidenceBackedJudgmentPacket
  ): List[JudgmentPacketValidationIssue] =
    packet.claimEventClusters.filter(_.causeClaimIds.isEmpty).map { cluster =>
      JudgmentPacketValidationIssue(
        kind = JudgmentPacketValidationIssueKind.EmptyClaimEventClusterCause,
        subjectId = cluster.id
      )
    }

  private def nonConcreteClaimEventClusterMembers(
      packet: EvidenceBackedJudgmentPacket,
      claimsById: Map[String, ClaimSeed]
  ): List[JudgmentPacketValidationIssue] =
    val concreteFamilies =
      Set(ClaimFamily.Tactical, ClaimFamily.Defensive, ClaimFamily.Conversion, ClaimFamily.Material, ClaimFamily.Evaluation)
    packet.claimEventClusters.flatMap { cluster =>
      cluster.memberClaimIds.flatMap(claimsById.get).filterNot(claim => concreteFamilies.contains(claim.family)).map { claim =>
        JudgmentPacketValidationIssue(
          kind = JudgmentPacketValidationIssueKind.NonConcreteClaimEventClusterMember,
          subjectId = claim.id
        )
      }
    }

  private def longTermClaimEventClusterEvidence(
      packet: EvidenceBackedJudgmentPacket
  ): List[JudgmentPacketValidationIssue] =
    val longTermLayers =
      Set(
        EvidenceLayer.PawnStructure,
        EvidenceLayer.Strategic,
        EvidenceLayer.OpeningContext,
        EvidenceLayer.FeatureAnchor,
        EvidenceLayer.ApplicabilityAssessment,
        EvidenceLayer.PlanPressure,
        EvidenceLayer.PlanTransition
      )
    packet.claimEventClusters.flatMap { cluster =>
      cluster.evidence.filter(ref => longTermLayers.contains(ref.layer)).map { ref =>
        JudgmentPacketValidationIssue(
          kind = JudgmentPacketValidationIssueKind.LongTermClaimEventClusterEvidence,
          subjectId = cluster.id,
          evidence = Some(ref)
        )
      }
    }

  private def mismatchedClaimEventClusterKinds(
      packet: EvidenceBackedJudgmentPacket
  ): List[JudgmentPacketValidationIssue] =
    packet.claimEventClusters.flatMap { cluster =>
      ClaimEventCluster.kindForCause(cluster.causeKind).filterNot(_ == cluster.kind).map { _ =>
        JudgmentPacketValidationIssue(
          kind = JudgmentPacketValidationIssueKind.MismatchedClaimEventClusterKind,
          subjectId = cluster.id
        )
      }
    }

  private def invalidProbeRequests(packet: EvidenceBackedJudgmentPacket): List[JudgmentPacketValidationIssue] =
    packet.probeRequests.filterNot(validProbeRequest).map { request =>
      JudgmentPacketValidationIssue(
        kind = JudgmentPacketValidationIssueKind.InvalidProbeRequest,
        subjectId = Option(request.id).map(_.trim).filter(_.nonEmpty).getOrElse("probe-request")
      )
    }

  private def validProbeRequest(request: ProbeRequest): Boolean =
    val idValid = Option(request.id).exists(_.trim.nonEmpty)
    val fenValid =
      Option(request.fen)
        .map(_.trim)
        .exists(fen => fen.nonEmpty && Fen.read(Standard, Fen.Full(fen)).isDefined)
    val depthValid = request.depth > 0
    val purposeValid = request.purpose.nonEmpty
    val requiredSignalsValid = request.requiredSignals.exists(_.trim.nonEmpty)
    val movesValid = request.moves.forall(validUciMove)
    val branchValid =
      request.purpose.filter(branchReplyProbePurpose).forall(_ => validBranchReplyProbeRequest(request))
    idValid && fenValid && depthValid && purposeValid && requiredSignalsValid && movesValid && branchValid

  private def validBranchReplyProbeRequest(request: ProbeRequest): Boolean =
    val signals = request.requiredSignals.map(_.trim).filter(_.nonEmpty).toSet
    Set("replyPvs", "depth", "purpose", "variationHash").subsetOf(signals) &&
      request.candidateMove.exists(validUciMove) &&
      request.multiPv.exists(_ >= 2) &&
      request.depthFloor.exists(floor => floor > 0 && floor <= request.depth) &&
      request.variationHash.exists(_.trim.nonEmpty)

  private def branchReplyProbePurpose(purpose: ProbePurpose): Boolean =
    purpose match
      case ProbePurpose.ReplyMultipv | ProbePurpose.DefenseReplyMultipv | ProbePurpose.ConvertReplyMultipv |
          ProbePurpose.RecaptureBranches | ProbePurpose.KeepTensionBranches | ProbePurpose.FreeTempoBranches =>
        true
      case _ =>
        false

  private def validUciMove(raw: String): Boolean =
    Option(raw).map(_.trim.toLowerCase).exists(_.matches("""[a-h][1-8][a-h][1-8][nbrq]?"""))

  private def missing(
      refs: List[EvidenceRef],
      graphIds: Set[String],
      kind: JudgmentPacketValidationIssueKind
  ): List[JudgmentPacketValidationIssue] =
    refs.distinctBy(_.id).filterNot(ref => graphIds.contains(ref.id)).map { ref =>
      JudgmentPacketValidationIssue(kind = kind, subjectId = ref.id, evidence = Some(ref))
    }
