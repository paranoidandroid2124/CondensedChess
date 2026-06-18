package lila.chessjudgment.analysis.assembly

import lila.chessjudgment.analysis.policy.{ ClaimTruthDecision, ClaimTruthPolicy, ClaimTruthStatus }
import lila.chessjudgment.model.judgment.*

final case class ClaimCandidateGraph(
    decisions: List[ClaimTruthDecision]
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
    ClaimCandidateGraph(claims.map(ClaimTruthPolicy.evaluate(_, graph)))

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
    decision.claim.evidence.size * 10 +
      decision.claim.supportingFacts.size +
      confidenceScore(decision.claim.confidence)

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
    val verdict = relativeAssessments.headOption.map(_.comparison.verdict)
    ClaimDeduplicator
      .deduplicate(graph.certified)
      .sortBy(decision => -priority(decision.claim, verdict))
      .map(_.claim)

  private def priority(
      claim: ClaimSeed,
      verdict: Option[MoveChoiceVerdict]
  ): Int =
    familyPriority(claim.family, verdict) * 1000 +
      confidencePriority(claim.confidence) * 100 +
      claim.evidence.size

  private def familyPriority(
      family: ClaimFamily,
      verdict: Option[MoveChoiceVerdict]
  ): Int =
    verdict match
      case Some(MoveChoiceVerdict.Blunder | MoveChoiceVerdict.Mistake | MoveChoiceVerdict.Inaccuracy) =>
        family match
          case ClaimFamily.Evaluation => 8
          case ClaimFamily.Tactical   => 7
          case ClaimFamily.Defensive  => 6
          case ClaimFamily.PawnStructure | ClaimFamily.Strategic | ClaimFamily.Plan => 4
          case ClaimFamily.Conversion | ClaimFamily.Opening => 3
      case _ =>
        family match
          case ClaimFamily.Tactical      => 8
          case ClaimFamily.Defensive     => 7
          case ClaimFamily.PawnStructure => 6
          case ClaimFamily.Strategic     => 5
          case ClaimFamily.Conversion    => 4
          case ClaimFamily.Evaluation    => 3
          case ClaimFamily.Plan          => 2
          case ClaimFamily.Opening       => 1

  private def confidencePriority(confidence: EvidenceConfidence): Int =
    confidence match
      case EvidenceConfidence.LegalReplayVerified => 5
      case EvidenceConfidence.EngineBacked        => 4
      case EvidenceConfidence.BoardDerived        => 3
      case EvidenceConfidence.Mixed               => 2
      case EvidenceConfidence.Heuristic           => 1
