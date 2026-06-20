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
  case MismatchedEvidenceLayer
  case MismatchedLineEvidenceRef
  case MismatchedEvalEvidenceRef
  case MismatchedRelativeCauseEventLine
  case UnbackedRelativeCauseProof
  case MissingComparisonReferenceParent
  case MissingComparisonCandidateParent
  case MissingCounterfactualReferenceParent
  case MissingCounterfactualCandidateParent
  case MissingVerdictCertificationReferenceParent
  case MissingVerdictCertificationPrimaryParent
  case MismatchedClaimSubjectBinding
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
        graphBindingInvariants(packet),
        claimSubjectBindingInvariants(packet),
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
    packet.claimSupportClusters.flatMap { cluster =>
      cluster.anchorClaimIds.flatMap(claimsById.get).filterNot(_.family.isLongTerm).map { claim =>
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
    packet.claimEventClusters.flatMap { cluster =>
      cluster.memberClaimIds.flatMap(claimsById.get).filterNot(_.family.isEvent).map { claim =>
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

  private def graphBindingInvariants(packet: EvidenceBackedJudgmentPacket): List[JudgmentPacketValidationIssue] =
    packet.evidenceGraph.records.flatMap { record =>
      val layerIssue =
        Option
          .when(record.ref.layer != record.payload.layer)(
            JudgmentPacketValidationIssue(
              JudgmentPacketValidationIssueKind.MismatchedEvidenceLayer,
              record.ref.id,
              Some(record.ref)
            )
          )
          .toList
      val bindingIssues =
        record match
          case EvidenceRecord(ref, LineFactEvidence(line, _, _, _, _, _), _) =>
            Option
              .when(!ref.line.contains(line))(
                JudgmentPacketValidationIssue(
                  JudgmentPacketValidationIssueKind.MismatchedLineEvidenceRef,
                  ref.id,
                  Some(ref)
                )
              )
              .toList
          case EvidenceRecord(ref, EvalFactEvidence(line, _, _, _), _) =>
            Option
              .when(!ref.line.contains(line))(
                JudgmentPacketValidationIssue(
                  JudgmentPacketValidationIssueKind.MismatchedEvalEvidenceRef,
                  ref.id,
                  Some(ref)
                )
              )
              .toList
          case record @ EvidenceRecord(ref, RelativeCauseFactEvidence(cause), _) =>
            List(
              Option.when(!ref.line.contains(cause.eventLine))(
                JudgmentPacketValidationIssue(
                  JudgmentPacketValidationIssueKind.MismatchedRelativeCauseEventLine,
                  ref.id,
                  Some(ref)
                )
              ),
              Option.when(cause.proof.exists(proof => proof.hasTypedDepth && !relativeCauseProofBacked(packet.evidenceGraph, record, proof)))(
                JudgmentPacketValidationIssue(
                  JudgmentPacketValidationIssueKind.UnbackedRelativeCauseProof,
                  ref.id,
                  Some(ref)
                )
              )
            ).flatten
          case record @ EvidenceRecord(ref, CandidateComparisonEvidence(fact), _) =>
            List(
              Option.when(!hasParentLineAndEval(packet.evidenceGraph, record, fact.referenceLine))(
                JudgmentPacketValidationIssue(
                  JudgmentPacketValidationIssueKind.MissingComparisonReferenceParent,
                  ref.id,
                  Some(ref)
                )
              ),
              Option.when(!hasParentLineAndEval(packet.evidenceGraph, record, fact.candidateLine))(
                JudgmentPacketValidationIssue(
                  JudgmentPacketValidationIssueKind.MissingComparisonCandidateParent,
                  ref.id,
                  Some(ref)
                )
              )
            ).flatten
          case record @ EvidenceRecord(ref, CounterfactualFactEvidence(referenceLine, candidateLine, _), _) =>
            List(
              Option.when(!hasParentLineAndEval(packet.evidenceGraph, record, referenceLine))(
                JudgmentPacketValidationIssue(
                  JudgmentPacketValidationIssueKind.MissingCounterfactualReferenceParent,
                  ref.id,
                  Some(ref)
                )
              ),
              Option.when(!hasParentLineAndEval(packet.evidenceGraph, record, candidateLine))(
                JudgmentPacketValidationIssue(
                  JudgmentPacketValidationIssueKind.MissingCounterfactualCandidateParent,
                  ref.id,
                  Some(ref)
                )
              )
            ).flatten
          case record @ EvidenceRecord(ref, MoveVerdictCertificationEvidence(certification), _) =>
            List(
              Option.when(!hasParentLineAndEval(packet.evidenceGraph, record, certification.primaryComparison.referenceLine))(
                JudgmentPacketValidationIssue(
                  JudgmentPacketValidationIssueKind.MissingVerdictCertificationReferenceParent,
                  ref.id,
                  Some(ref)
                )
              ),
              Option.when(
                !hasParentLineAndEval(packet.evidenceGraph, record, certification.primaryComparison.candidateLine) ||
                  !hasMatchingPrimaryComparisonParent(packet.evidenceGraph, record, certification.primaryComparison)
              )(
                JudgmentPacketValidationIssue(
                  JudgmentPacketValidationIssueKind.MissingVerdictCertificationPrimaryParent,
                  ref.id,
                  Some(ref)
                )
              )
            ).flatten
          case _ =>
            Nil
      layerIssue ++ bindingIssues
    }

  private def claimSubjectBindingInvariants(packet: EvidenceBackedJudgmentPacket): List[JudgmentPacketValidationIssue] =
    packet.claims.flatMap { claim =>
      val binding = JudgmentSubjectBinding.claimBinding(packet, claim)
      Option
        .when(
          claim.subject == IdeaSubject.PlayedMove &&
            (binding == SubjectBindingClass.Other || binding == SubjectBindingClass.ContextPlayed)
        )(
          JudgmentPacketValidationIssue(
            JudgmentPacketValidationIssueKind.MismatchedClaimSubjectBinding,
            claim.id
          )
        )
        .toList
    }

  private def hasParentLineAndEval(
      graph: TypedEvidenceGraph,
      record: EvidenceRecord,
      line: LineNodeRef
  ): Boolean =
    hasParentPayloadLine(graph, record, line, EvidenceLayer.Line) &&
      hasParentPayloadLine(graph, record, line, EvidenceLayer.Eval)

  private def hasParentPayloadLine(
      graph: TypedEvidenceGraph,
      record: EvidenceRecord,
      line: LineNodeRef,
      layer: EvidenceLayer
  ): Boolean =
    parentClosure(graph, record).exists {
      case EvidenceRecord(ref, LineFactEvidence(payloadLine, _, _, _, _, _), _) =>
        layer == EvidenceLayer.Line && ref.line.contains(line) && payloadLine == line
      case EvidenceRecord(ref, EvalFactEvidence(payloadLine, _, _, _), _) =>
        layer == EvidenceLayer.Eval && ref.line.contains(line) && payloadLine == line
      case _ =>
        false
    }

  private def relativeCauseProofBacked(
      graph: TypedEvidenceGraph,
      record: EvidenceRecord,
      proof: RelativeCauseProof
  ): Boolean =
    val parents = parentClosure(graph, record)
    proof.boardAnchors.forall(kind => parents.exists(parentHasBoardAnchor(_, kind))) &&
      proof.lineEvents.forall(kind => parents.exists(parentHasLineEvent(_, kind))) &&
      proof.lineConsequences.forall(kind => parents.exists(parentHasLineConsequence(_, kind))) &&
      proof.relationKinds.forall(kind => parents.exists(parentHasRelationKind(_, kind))) &&
      proof.supportLayers.forall(layer => parents.exists(_.ref.layer == layer))

  private def parentHasBoardAnchor(record: EvidenceRecord, kind: BoardAnchorKind): Boolean =
    record match
      case EvidenceRecord(_, payload: BoardFactEvidence, _) =>
        payload.claimGradeAnchorKinds.contains(kind)
      case _ =>
        false

  private def parentHasLineEvent(record: EvidenceRecord, kind: LineEventKind): Boolean =
    record match
      case EvidenceRecord(_, payload: LineFactEvidence, _) =>
        payload.events.exists(_.kind == kind)
      case _ =>
        false

  private def parentHasLineConsequence(record: EvidenceRecord, kind: LineConsequenceKind): Boolean =
    record match
      case EvidenceRecord(_, payload: LineFactEvidence, _) =>
        payload.claimGradeConsequenceKinds.contains(kind)
      case _ =>
        false

  private def parentHasRelationKind(record: EvidenceRecord, kind: RelationFactKind): Boolean =
    record match
      case EvidenceRecord(_, RelationFactEvidence(payloadKind, _, _, _, _), _) =>
        payloadKind == kind
      case _ =>
        false

  private def parentClosure(graph: TypedEvidenceGraph, record: EvidenceRecord): List[EvidenceRecord] =
    def loop(refs: List[EvidenceRef], seen: Set[String]): List[EvidenceRecord] =
      refs.flatMap { ref =>
        if seen.contains(ref.id) then Nil
        else
          graph.byId.get(ref.id).toList.flatMap { parent =>
            parent :: loop(parent.parents, seen + ref.id)
          }
      }
    loop(record.parents, Set.empty).distinctBy(_.ref.id)

  private def hasMatchingPrimaryComparisonParent(
      graph: TypedEvidenceGraph,
      record: EvidenceRecord,
      primary: CandidateComparisonFact
  ): Boolean =
    record.parents.flatMap(parent => graph.byId.get(parent.id)).exists {
      case EvidenceRecord(_, CandidateComparisonEvidence(fact), _) =>
        sameComparisonIdentity(fact, primary)
      case EvidenceRecord(_, CounterfactualFactEvidence(referenceLine, candidateLine, comparison), _) =>
        referenceLine == primary.referenceLine &&
          candidateLine == primary.candidateLine &&
          sameEvalComparison(comparison, primary.comparison)
      case _ =>
        false
    }

  private def sameComparisonIdentity(left: CandidateComparisonFact, right: CandidateComparisonFact): Boolean =
    left.kind == right.kind &&
      left.referenceLine == right.referenceLine &&
      left.candidateLine == right.candidateLine &&
      sameEvalComparison(left.comparison, right.comparison)

  private def sameEvalComparison(left: EvalComparison, right: EvalComparison): Boolean =
    left.referenceLine == right.referenceLine &&
      left.candidateLine == right.candidateLine &&
      left.verdict == right.verdict &&
      left.winPercentLossForMover == right.winPercentLossForMover &&
      left.candidateWinPercentDeltaForMover == right.candidateWinPercentDeltaForMover

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
