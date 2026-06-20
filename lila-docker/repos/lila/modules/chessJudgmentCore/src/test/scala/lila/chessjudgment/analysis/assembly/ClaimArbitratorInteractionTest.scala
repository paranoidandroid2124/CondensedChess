package lila.chessjudgment.analysis.assembly

import chess.Color
import lila.chessjudgment.analysis.policy.{ ClaimTruthDecision, ClaimTruthPolicy, ClaimTruthStatus }
import lila.chessjudgment.analysis.singlePosition.{
  DefenseAssessment,
  Threat,
  ThreatAnalysis,
  ThreatDriver,
  ThreatEvidenceSource,
  ThreatKind,
  ThreatSeverity
}
import lila.chessjudgment.model.{
  ActivePlans,
  Plan,
  PlanMatch,
  PlanScoringResult,
  ProbeAdmissionStatus,
  ProbeContractValidator,
  ProbePurpose,
  ProbeRequest,
  ProbeResult
}
import lila.chessjudgment.model.judgment.*
import lila.chessjudgment.model.structure.{ CenterState, StructureId, StructureProfile }
import lila.chessjudgment.model.strategic.VariationLine

class ClaimArbitratorInteractionTest extends munit.FunSuite:

  private val root = PositionNodeRef(
    fen = "8/8/8/8/8/8/8/8 w - - 0 1",
    ply = 12
  )
  private val line = LineNodeRef("played-line", "d1d3", 1, LineNodeRole.Played)

  test("tactical and plan claims interact through graph ranking without evidence absorption"):
    val relationRef = ref("relation-pin", EvidenceLayer.Relation, EvidenceProducer.TacticalRelationProducer)
    val lineRef = ref("line-played", EvidenceLayer.Line, EvidenceProducer.LegalLineProducer)
    val evalRef = ref("eval-played", EvidenceLayer.Eval, EvidenceProducer.EngineEvalProducer)
    val comparisonRef = ref("comparison-played", EvidenceLayer.CandidateComparison, EvidenceProducer.RelativeMoveProducer)
    val planRef = ref("plan-pressure", EvidenceLayer.PlanPressure, EvidenceProducer.PlanPressureProducer)

    val tacticalClaim =
      claim(
        id = "claim-tactical",
        family = ClaimFamily.Tactical,
        evidence = List(relationRef, lineRef, evalRef, comparisonRef),
        engineComparison = None
      )
    val planClaim =
      claim(
        id = "claim-plan",
        family = ClaimFamily.Plan,
        evidence = List(planRef, lineRef),
        engineComparison = None
      )
    val graph =
      ClaimCandidateGraph(
        decisions = List(certified(tacticalClaim), certified(planClaim)),
        evidenceGraph = TypedEvidenceGraph(
          List(
            EvidenceRecord(
              relationRef,
              RelationFactEvidence(
                kind = RelationFactKind.Pin,
                focusSquares = Nil,
                targetSquare = None,
                lineMoves = List("d1d3", "d8d3"),
                participants = Nil
              )
            ),
            EvidenceRecord(
              lineRef,
              LineFactEvidence(
                line = line,
                firstMove = Some("d1d3"),
                replyMove = Some("d8d3"),
                continuationMoves = List("e2d3")
              )
            ),
            EvidenceRecord(evalRef, EvalFactEvidence(line, evalCp = 20, mate = None, depth = 18)),
            EvidenceRecord(
              comparisonRef,
              CandidateComparisonEvidence(
                CandidateComparisonFact(
                  kind = CandidateComparisonKind.PlayedVsBest,
                  referenceLine = line,
                  candidateLine = line,
                  comparison = engineComparison
                )
              )
            ),
            EvidenceRecord(planRef, PlanPressureEvidence(planScoring, activePlans))
          )
        )
      )

    val ranked = ClaimArbitrator.rank(graph, Nil)
    val tactical = ranked.find(_.id == tacticalClaim.id).get
    val plan = ranked.find(_.id == planClaim.id).get

    assertEquals(ranked.head.id, tacticalClaim.id)
    assert(tactical.salience.exists(_.interactions.exists(_.kind == ClaimInteractionKind.TacticalConstrainsLongTerm)))
    assert(plan.salience.exists(_.interactions.exists(_.kind == ClaimInteractionKind.LongTermConstrainedByTactic)))
    assert(!plan.evidence.exists(_.layer == EvidenceLayer.Relation))

  test("claims on the same graph context interact without absorbing evidence"):
    val relationRef = ref("relation-fork", EvidenceLayer.Relation, EvidenceProducer.TacticalRelationProducer)
    val evalRef = ref("eval-only-tactical", EvidenceLayer.Eval, EvidenceProducer.EngineEvalProducer)
    val comparisonRef = ref("comparison-only-tactical", EvidenceLayer.CandidateComparison, EvidenceProducer.RelativeMoveProducer)
    val planRef = ref("plan-unshared", EvidenceLayer.PlanPressure, EvidenceProducer.PlanPressureProducer)

    val tacticalClaim =
      claim(
        id = "claim-tactical-unshared",
        family = ClaimFamily.Tactical,
        evidence = List(relationRef, evalRef, comparisonRef),
        engineComparison = None
      )
    val planClaim =
      claim(
        id = "claim-plan-unshared",
        family = ClaimFamily.Plan,
        evidence = List(planRef),
        engineComparison = None
      )
    val graph =
      ClaimCandidateGraph(
        decisions = List(certified(tacticalClaim), certified(planClaim)),
        evidenceGraph = TypedEvidenceGraph(
          List(
            EvidenceRecord(
              relationRef,
              RelationFactEvidence(
                kind = RelationFactKind.Fork,
                focusSquares = Nil,
                targetSquare = None,
                lineMoves = List("d1d3"),
                participants = Nil
              )
            ),
            EvidenceRecord(evalRef, EvalFactEvidence(line, evalCp = -80, mate = None, depth = 18)),
            EvidenceRecord(
              comparisonRef,
              CandidateComparisonEvidence(
                CandidateComparisonFact(
                  kind = CandidateComparisonKind.PlayedVsBest,
                  referenceLine = line,
                  candidateLine = line,
                  comparison = engineComparison
                )
              )
            ),
            EvidenceRecord(planRef, PlanPressureEvidence(planScoring, activePlans))
          )
        )
      )

    val ranked = ClaimArbitrator.rank(graph, Nil)
    val tactical = ranked.find(_.id == tacticalClaim.id).get
    val plan = ranked.find(_.id == planClaim.id).get

    assert(tactical.salience.exists(_.interactions.nonEmpty))
    assert(plan.salience.exists(_.interactions.nonEmpty))
    assert(!plan.evidence.exists(_.layer == EvidenceLayer.Relation))

  test("deferred claims remain outside packet claim ranking"):
    val lineRef = ref("line-certified", EvidenceLayer.Line, EvidenceProducer.LegalLineProducer)
    val evalRef = ref("eval-certified", EvidenceLayer.Eval, EvidenceProducer.EngineEvalProducer)
    val strategicRef = ref("strategic-deferred", EvidenceLayer.Strategic, EvidenceProducer.StrategicFeatureProducer)
    val certifiedClaim =
      claim(
        id = "claim-evaluation-certified",
        family = ClaimFamily.Evaluation,
        evidence = List(lineRef, evalRef),
        engineComparison = Some(engineComparison)
      )
    val deferredClaim =
      claim(
        id = "claim-strategic-deferred",
        family = ClaimFamily.Strategic,
        evidence = List(strategicRef),
        engineComparison = None
      )
    val graph =
      ClaimCandidateGraph(
        decisions = List(certified(certifiedClaim), deferred(deferredClaim)),
        evidenceGraph = TypedEvidenceGraph(
          List(
            EvidenceRecord(
              lineRef,
              LineFactEvidence(
                line = line,
                firstMove = Some("d1d3"),
                replyMove = None,
                continuationMoves = Nil
              )
            ),
            EvidenceRecord(evalRef, EvalFactEvidence(line, evalCp = -80, mate = None, depth = 18)),
            EvidenceRecord(strategicRef, StrategicFactEvidence(StrategicFactKind.Activity, Nil, Nil, 0.4))
          )
        )
      )

    val ranked = ClaimArbitrator.rank(graph, Nil)

    assertEquals(ranked.map(_.id), List(certifiedClaim.id))
    assert(!ranked.exists(_.supportStatus.exists(_.status == ClaimSupportStatus.Deferred)))

  test("certified long-term support claims aggregate without absorbing tactical claims"):
    val lineRef = ref("line-shared", EvidenceLayer.Line, EvidenceProducer.LegalLineProducer)
    val evalRef = ref("eval-tactical", EvidenceLayer.Eval, EvidenceProducer.EngineEvalProducer)
    val relationRef = ref("relation-tactical", EvidenceLayer.Relation, EvidenceProducer.TacticalRelationProducer)
    val comparisonRef = ref("comparison-tactical", EvidenceLayer.CandidateComparison, EvidenceProducer.RelativeMoveProducer)
    val strategicRefA = ref("strategic-a", EvidenceLayer.Strategic, EvidenceProducer.StrategicFeatureProducer)
    val strategicRefB = ref("strategic-b", EvidenceLayer.Strategic, EvidenceProducer.StrategicFeatureProducer)
    val ideaA = ChessIdeaRef("idea-strategic-a", ChessIdeaFamily.Strategic)
    val ideaB = ChessIdeaRef("idea-strategic-b", ChessIdeaFamily.Strategic)
    val strategicClaimA =
      claim(
        id = "claim-strategic-a",
        family = ClaimFamily.Strategic,
        evidence = List(strategicRefA, lineRef),
        engineComparison = None
      ).copy(idea = Some(ideaA), relatedIdeas = List(ideaA))
    val strategicClaimB =
      claim(
        id = "claim-strategic-b",
        family = ClaimFamily.Strategic,
        evidence = List(strategicRefB, lineRef),
        engineComparison = None
      ).copy(idea = Some(ideaB), relatedIdeas = List(ideaB))
    val tacticalClaim =
      claim(
        id = "claim-tactical-distinct",
        family = ClaimFamily.Tactical,
        evidence = List(relationRef, lineRef, evalRef, comparisonRef),
        engineComparison = Some(engineComparison)
      )
    val graph =
      ClaimCandidateGraphAssembler.fromClaims(
        claims = List(strategicClaimA, strategicClaimB, tacticalClaim),
        graph = TypedEvidenceGraph(
          List(
            EvidenceRecord(
              lineRef,
              LineFactEvidence(
                line = line,
                firstMove = Some("d1d3"),
                replyMove = Some("d8d3"),
                continuationMoves = List("e2d3")
              )
            ),
            EvidenceRecord(evalRef, EvalFactEvidence(line, evalCp = -80, mate = None, depth = 18)),
            EvidenceRecord(
              comparisonRef,
              CandidateComparisonEvidence(
                CandidateComparisonFact(
                  kind = CandidateComparisonKind.PlayedVsBest,
                  referenceLine = line,
                  candidateLine = line,
                  comparison = engineComparison
                )
              )
            ),
            EvidenceRecord(
              relationRef,
              RelationFactEvidence(
                kind = RelationFactKind.Fork,
                focusSquares = Nil,
                targetSquare = None,
                lineMoves = List("d1d3", "d8d3"),
                participants = Nil
              )
            ),
            EvidenceRecord(strategicRefA, StrategicFactEvidence(StrategicFactKind.Activity, Nil, Nil, 0.5)),
            EvidenceRecord(strategicRefB, StrategicFactEvidence(StrategicFactKind.FileControl, Nil, Nil, 0.6))
          )
        )
      )

    val certifiedClaims = ClaimArbitrator.rank(graph, Nil)
    val strategicClaims = certifiedClaims.filter(_.family == ClaimFamily.Strategic)
    val tacticalClaims = certifiedClaims.filter(_.family == ClaimFamily.Tactical)

    assertEquals(strategicClaims.size, 2)
    assertEquals(tacticalClaims.size, 1)
    assertEquals(strategicClaims.flatMap(_.ideaRefs.map(_.id)).toSet, Set(ideaA.id, ideaB.id))
    assert(strategicClaims.forall(claim => claim.evidence.exists(_.id == lineRef.id)))
    assert(strategicClaims.forall(claim => !claim.evidence.exists(_.layer == EvidenceLayer.Relation)))

  test("claim support clusters expose long-term support without absorbing tactical or engine-bound claims"):
    val strategicRef = ref("strategic-support", EvidenceLayer.Strategic, EvidenceProducer.StrategicFeatureProducer)
    val structureRef = ref("structure-support", EvidenceLayer.PawnStructure, EvidenceProducer.PawnStructureProducer)
    val relationRef = ref("relation-bound", EvidenceLayer.Relation, EvidenceProducer.TacticalRelationProducer)
    val relativeRef = ref("relative-bound", EvidenceLayer.RelativeCause, EvidenceProducer.RelativeMoveProducer)
    val strategicClaim =
      claim(
        id = "claim-strategic-support",
        family = ClaimFamily.Strategic,
        evidence = List(structureRef),
        engineComparison = None
      ).copy(subject = IdeaSubject.Position, primaryLine = None, subjectMove = None, scope = EvidenceScope.BeforePosition)
    val pawnClaim =
      claim(
        id = "claim-pawn-support",
        family = ClaimFamily.PawnStructure,
        evidence = List(structureRef),
        engineComparison = None
      ).copy(subject = IdeaSubject.Position, primaryLine = None, subjectMove = None, scope = EvidenceScope.BeforePosition)
    val tacticalClaim =
      claim(
        id = "claim-tactical-bound",
        family = ClaimFamily.Tactical,
        evidence = List(relationRef),
        engineComparison = None
      )
    val engineBoundStrategic =
      claim(
        id = "claim-strategic-engine-bound",
        family = ClaimFamily.Strategic,
        evidence = List(relativeRef),
        engineComparison = Some(engineComparison)
      )

    val graph =
      TypedEvidenceGraph(
        List(
          EvidenceRecord(structureRef, PawnStructureFactEvidence(structureProfile, None, None)),
          EvidenceRecord(strategicRef, StrategicFactEvidence(StrategicFactKind.Activity, Nil, Nil, 0.5)),
          EvidenceRecord(
            relationRef,
            RelationFactEvidence(
              kind = RelationFactKind.Pin,
              focusSquares = Nil,
              targetSquare = None,
              lineMoves = List(line.rootMove),
              participants = Nil
            )
          ),
          EvidenceRecord(
            relativeRef,
            RelativeCauseFactEvidence(
              RelativeCauseFact(
                kind = RelativeCauseKind.StrategicConcession,
                comparisonKind = CandidateComparisonKind.PlayedVsBest,
                referenceLine = line,
                candidateLine = line,
                verdict = MoveChoiceVerdict.Inaccuracy,
                winPercentLossForMover = 5.0,
                candidateWinPercentDeltaForMover = 0.0,
                evidenceLines = List(line)
              )
            )
          )
        )
      )
    val clusters =
      ClaimSupportCluster.fromClaims(List(strategicClaim, pawnClaim, tacticalClaim, engineBoundStrategic), graph)

    assertEquals(clusters.size, 1)
    assertEquals(clusters.head.kind, ClaimSupportClusterKind.LongTermSupport)
    assertEquals(clusters.head.anchorClaimIds.toSet, Set(strategicClaim.id, pawnClaim.id))
    assertEquals(clusters.head.families.toSet, Set(ClaimFamily.Strategic, ClaimFamily.PawnStructure))
    assert(!clusters.head.evidence.exists(ref => ref.layer == EvidenceLayer.Relation || ref.layer == EvidenceLayer.RelativeCause))

  test("claim support clusters keep single long-term anchors when tactics constrain them"):
    val strategicRef = ref("strategic-single-anchor", EvidenceLayer.Strategic, EvidenceProducer.StrategicFeatureProducer)
    val relationRef = ref("relation-single-constraint", EvidenceLayer.Relation, EvidenceProducer.TacticalRelationProducer)
    val tacticalClaim =
      claim(
        id = "claim-tactical-constraint",
        family = ClaimFamily.Tactical,
        evidence = List(relationRef),
        engineComparison = Some(engineComparison)
      )
    val strategicClaim =
      claim(
        id = "claim-strategic-constrained",
        family = ClaimFamily.Strategic,
        evidence = List(strategicRef),
        engineComparison = None
      ).copy(
        subject = IdeaSubject.Position,
        primaryLine = None,
        subjectMove = None,
        scope = EvidenceScope.BeforePosition,
        salience = Some(
          ClaimSalience(
            score = 8,
            drivers = List(ClaimSalienceDriver.StrategicFeature),
            interactions = List(
              ClaimInteraction(
                kind = ClaimInteractionKind.LongTermConstrainedByTactic,
                relatedClaimId = tacticalClaim.id,
                strength = 3,
                interactionEvidence = List(relationRef)
              )
            )
          )
        )
      )

    val graph =
      TypedEvidenceGraph(
        List(
          EvidenceRecord(strategicRef, StrategicFactEvidence(StrategicFactKind.Activity, Nil, Nil, 0.5)),
          EvidenceRecord(
            relationRef,
            RelationFactEvidence(
              kind = RelationFactKind.Pin,
              focusSquares = Nil,
              targetSquare = None,
              lineMoves = List(line.rootMove),
              participants = Nil
            )
          )
        )
      )
    val clusters = ClaimSupportCluster.fromClaims(List(strategicClaim, tacticalClaim), graph)

    assertEquals(clusters.size, 1)
    assertEquals(clusters.head.anchorClaimIds, List(strategicClaim.id))
    assertEquals(clusters.head.constrainingClaimIds, List(tacticalClaim.id))
    assertEquals(clusters.head.supportingClaimIds, Nil)
    assertEquals(clusters.head.interactions.map(_.kind), List(ClaimInteractionKind.LongTermConstrainedByTactic))
    assert(!clusters.head.evidence.exists(_.layer == EvidenceLayer.Relation))

  test("claim event clusters bind concrete cause claims without absorbing long-term support"):
    val relativeRef = ref("relative-cause-event", EvidenceLayer.RelativeCause, EvidenceProducer.RelativeMoveProducer)
    val verdictRef = ref("verdict-certification-event", EvidenceLayer.MoveVerdictCertification, EvidenceProducer.RelativeMoveProducer)
    val strategicRef = ref("strategic-event-support", EvidenceLayer.Strategic, EvidenceProducer.StrategicFeatureProducer)
    val cause =
      RelativeCauseFact(
        kind = RelativeCauseKind.TacticalRefutationOfPlayed,
        comparisonKind = CandidateComparisonKind.PlayedVsBest,
        referenceLine = line,
        candidateLine = line,
        verdict = MoveChoiceVerdict.Mistake,
        winPercentLossForMover = 8.0,
        candidateWinPercentDeltaForMover = 0.0,
        evidenceLines = List(line)
      )
    val certification =
      MoveVerdictCertification(
        playedMove = line.rootMove,
        verdict = MoveChoiceVerdict.Mistake,
        primaryComparison = CandidateComparisonFact(
          kind = CandidateComparisonKind.PlayedVsBest,
          referenceLine = line,
          candidateLine = line,
          comparison = engineComparison.copy(verdict = MoveChoiceVerdict.Mistake, winPercentLossForMover = 8.0)
        ),
        causes = List(cause)
      )
    val tacticalClaim =
      claim(
        id = "claim-event-tactical",
        family = ClaimFamily.Tactical,
        evidence = List(relativeRef),
        engineComparison = Some(engineComparison)
      ).copy(
        salience = Some(
          ClaimSalience(
            score = 12,
            drivers = List(ClaimSalienceDriver.TacticalRelation, ClaimSalienceDriver.EngineSwing),
            interactions = List(
              ClaimInteraction(
                kind = ClaimInteractionKind.TacticalRefutesStrategicPlan,
                relatedClaimId = "claim-event-strategic",
                strength = 5,
                interactionEvidence = List(relativeRef)
              )
            )
          )
        )
      )
    val evaluationIdea = ChessIdeaRef("idea-event-evaluation", ChessIdeaFamily.Evaluation)
    val evaluationClaim =
      claim(
        id = "claim-event-evaluation",
        family = ClaimFamily.Evaluation,
        evidence = List(verdictRef),
        engineComparison = Some(engineComparison)
      ).copy(idea = Some(evaluationIdea), relatedIdeas = List(evaluationIdea))
    val strategicClaim =
      claim(
        id = "claim-event-strategic",
        family = ClaimFamily.Strategic,
        evidence = List(strategicRef),
        engineComparison = None
      ).copy(subject = IdeaSubject.Position, primaryLine = None, subjectMove = None, scope = EvidenceScope.BeforePosition)
    val graph =
      TypedEvidenceGraph(
        List(
          EvidenceRecord(relativeRef, RelativeCauseFactEvidence(cause)),
          EvidenceRecord(verdictRef, MoveVerdictCertificationEvidence(certification), parents = List(relativeRef)),
          EvidenceRecord(strategicRef, StrategicFactEvidence(StrategicFactKind.Activity, Nil, Nil, 0.5))
        )
      )
    val supportClusters = ClaimSupportCluster.fromClaims(List(strategicClaim, tacticalClaim, evaluationClaim))
    val eventClusters = ClaimEventCluster.fromClaims(List(strategicClaim, tacticalClaim, evaluationClaim), graph, supportClusters)

    assertEquals(eventClusters.size, 1)
    assertEquals(eventClusters.head.kind, ClaimEventClusterKind.TacticalEvent)
    assertEquals(eventClusters.head.causeKind, RelativeCauseKind.TacticalRefutationOfPlayed)
    assertEquals(eventClusters.head.memberClaimIds.toSet, Set(tacticalClaim.id, evaluationClaim.id))
    assertEquals(eventClusters.head.causeClaimIds, List(tacticalClaim.id))
    assertEquals(eventClusters.head.evaluationClaimIds, List(evaluationClaim.id))
    assertEquals(eventClusters.head.witnessClaimIds, Nil)
    assertEquals(eventClusters.head.relatedSupportClusterIds.toSet, supportClusters.map(_.id).toSet)
    assert(eventClusters.head.interactions.exists(_.kind == ClaimInteractionKind.TacticalRefutesStrategicPlan))
    assert(eventClusters.head.evidence.exists(_.layer == EvidenceLayer.RelativeCause))
    assert(eventClusters.head.evidence.exists(_.layer == EvidenceLayer.MoveVerdictCertification))
    assert(!eventClusters.head.evidence.exists(_.layer == EvidenceLayer.Strategic))

  test("claim event clusters require a non-evaluation direct cause owner"):
    val verdictRef = ref("verdict-certification-only", EvidenceLayer.MoveVerdictCertification, EvidenceProducer.RelativeMoveProducer)
    val cause =
      RelativeCauseFact(
        kind = RelativeCauseKind.MissedTacticalResource,
        comparisonKind = CandidateComparisonKind.PlayedVsBest,
        referenceLine = line,
        candidateLine = line,
        verdict = MoveChoiceVerdict.Mistake,
        winPercentLossForMover = 8.0,
        candidateWinPercentDeltaForMover = 0.0,
        evidenceLines = List(line)
      )
    val certification =
      MoveVerdictCertification(
        playedMove = line.rootMove,
        verdict = MoveChoiceVerdict.Mistake,
        primaryComparison = CandidateComparisonFact(
          kind = CandidateComparisonKind.PlayedVsBest,
          referenceLine = line,
          candidateLine = line,
          comparison = engineComparison.copy(verdict = MoveChoiceVerdict.Mistake, winPercentLossForMover = 8.0)
        ),
        causes = List(cause)
      )
    val evaluationClaim =
      claim(
        id = "claim-evaluation-only-event",
        family = ClaimFamily.Evaluation,
        evidence = List(verdictRef),
        engineComparison = Some(engineComparison)
      )
    val graph =
      TypedEvidenceGraph(
        List(EvidenceRecord(verdictRef, MoveVerdictCertificationEvidence(certification)))
      )

    val eventClusters = ClaimEventCluster.fromClaims(List(evaluationClaim), graph)

    assertEquals(eventClusters, Nil)

  test("indirect defensive threat claims do not become event cause members"):
    val relativeRef = ref("relative-cause-defensive-link", EvidenceLayer.RelativeCause, EvidenceProducer.RelativeMoveProducer)
    val threatRef = ref("threat-context-defensive-link", EvidenceLayer.MoveMotif, EvidenceProducer.MoveMotifProducer)
    val cause =
      RelativeCauseFact(
        kind = RelativeCauseKind.MissedTacticalResource,
        comparisonKind = CandidateComparisonKind.PlayedVsBest,
        referenceLine = line,
        candidateLine = line,
        verdict = MoveChoiceVerdict.Mistake,
        winPercentLossForMover = 8.0,
        candidateWinPercentDeltaForMover = 0.0,
        evidenceLines = List(line)
      )
    val tacticalClaim =
      claim(
        id = "claim-event-cause-owner",
        family = ClaimFamily.Tactical,
        evidence = List(relativeRef),
        engineComparison = Some(engineComparison)
      )
    val defensiveClaim =
      claim(
        id = "claim-defensive-threat-only",
        family = ClaimFamily.Defensive,
        evidence = List(threatRef),
        engineComparison = None
      )
    val graph =
      TypedEvidenceGraph(
        List(
          EvidenceRecord(relativeRef, RelativeCauseFactEvidence(cause), parents = List(threatRef)),
          EvidenceRecord(threatRef, MoveMotifEvidence(line.rootMove, Nil))
        )
      )

    val eventClusters = ClaimEventCluster.fromClaims(List(tacticalClaim, defensiveClaim), graph)

    assertEquals(eventClusters.size, 1)
    assertEquals(eventClusters.head.memberClaimIds, List(tacticalClaim.id))
    assertEquals(eventClusters.head.causeClaimIds, List(tacticalClaim.id))
    assertEquals(eventClusters.head.witnessClaimIds, Nil)

  test("line-aware indirect tactical claims bind as witnesses to same-line event causes"):
    val relativeRefA = ref("relative-cause-line-witness-a", EvidenceLayer.RelativeCause, EvidenceProducer.RelativeMoveProducer)
    val relativeRefB = ref("relative-cause-line-witness-b", EvidenceLayer.RelativeCause, EvidenceProducer.RelativeMoveProducer)
    val relationRef = ref("relation-line-witness", EvidenceLayer.Relation, EvidenceProducer.TacticalRelationProducer)
    val causeA =
      RelativeCauseFact(
        kind = RelativeCauseKind.MissedTacticalResource,
        comparisonKind = CandidateComparisonKind.PlayedVsBest,
        referenceLine = line,
        candidateLine = line,
        verdict = MoveChoiceVerdict.Mistake,
        winPercentLossForMover = 8.0,
        candidateWinPercentDeltaForMover = 0.0,
        evidenceLines = List(line)
      )
    val causeB =
      RelativeCauseFact(
        kind = RelativeCauseKind.MaterialSwing,
        comparisonKind = CandidateComparisonKind.PlayedVsBest,
        referenceLine = line,
        candidateLine = line,
        verdict = MoveChoiceVerdict.Mistake,
        winPercentLossForMover = 8.0,
        candidateWinPercentDeltaForMover = 0.0,
        evidenceLines = List(line)
      )
    val causeOwner =
      claim(
        id = "claim-same-line-cause-owner",
        family = ClaimFamily.Tactical,
        evidence = List(relativeRefA, relativeRefB),
        engineComparison = Some(engineComparison)
      )
    val lineWitness =
      claim(
        id = "claim-same-line-local-witness",
        family = ClaimFamily.Tactical,
        evidence = List(relationRef),
        engineComparison = Some(engineComparison)
      )
    val graph =
      TypedEvidenceGraph(
        List(
          EvidenceRecord(relativeRefA, RelativeCauseFactEvidence(causeA), parents = List(relationRef)),
          EvidenceRecord(relativeRefB, RelativeCauseFactEvidence(causeB), parents = List(relationRef)),
          EvidenceRecord(
            relationRef,
            RelationFactEvidence(
              kind = RelationFactKind.Pin,
              focusSquares = Nil,
              targetSquare = None,
              lineMoves = List(line.rootMove),
              participants = Nil
            )
          )
        )
      )

    val eventClusters = ClaimEventCluster.fromClaims(List(causeOwner, lineWitness), graph)

    assertEquals(eventClusters.size, 2)
    assert(eventClusters.forall(_.causeClaimIds == List(causeOwner.id)))
    assert(eventClusters.forall(_.witnessClaimIds == List(lineWitness.id)))
    assertEquals(eventClusters.map(_.causeKind).toSet, Set(RelativeCauseKind.MissedTacticalResource, RelativeCauseKind.MaterialSwing))

  test("non-played candidate comparisons do not project relative causes as played-move ideas"):
    val raw = RawMoveReviewInput(
      fen = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1",
      playedMoveUci = "d2d4",
      variations = List(
        VariationLine(moves = List("e2e4", "e7e5"), scoreCp = 900, depth = 18),
        VariationLine(moves = List("d2d4", "d7d5"), scoreCp = -900, depth = 18),
        VariationLine(moves = List("g1f3", "g8f6"), scoreCp = -800, depth = 18)
      ),
      currentEvalCp = Some(0),
      ply = Some(0)
    )

    val packet = MoveReviewJudgmentOrchestrator.build(raw).get.packet
    val nonPlayedCausesProjectedAsPlayed =
      packet.ideas
        .filter(_.subject == IdeaSubject.PlayedMove)
        .flatMap(idea => idea.evidence.flatMap(ref => packet.evidenceGraph.byId.get(ref.id)))
        .collect {
          case EvidenceRecord(_, RelativeCauseFactEvidence(cause), _)
              if cause.comparisonKind == CandidateComparisonKind.BestVsSecond ||
                cause.comparisonKind == CandidateComparisonKind.ReferenceVsAlternative =>
            cause
        }
    val verdictCertificationCauses =
      packet.evidenceGraph.records.collect {
        case EvidenceRecord(_, MoveVerdictCertificationEvidence(certification), _) =>
          certification.causes
      }.flatten

    assertEquals(nonPlayedCausesProjectedAsPlayed, Nil)
    assert(verdictCertificationCauses.nonEmpty)
    assert(
      verdictCertificationCauses.forall(cause =>
        cause.eventRootMove == "d2d4" &&
          (
            cause.comparisonKind == CandidateComparisonKind.PlayedVsBest ||
              cause.comparisonKind == CandidateComparisonKind.PlayedVsAlternative
          )
      )
    )

  test("missing played candidate line preserves typed played-transition evidence without fake relative verdict"):
    val raw = RawMoveReviewInput(
      fen = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1",
      playedMoveUci = "b1c3",
      variations = List(
        VariationLine(moves = List("e2e4", "e7e5"), scoreCp = 35, depth = 18),
        VariationLine(moves = List("d2d4", "d7d5"), scoreCp = 20, depth = 18),
        VariationLine(moves = List("g1f3", "g8f6"), scoreCp = 15, depth = 18)
      ),
      currentEvalCp = Some(0),
      ply = Some(0)
    )

    val packet = MoveReviewJudgmentOrchestrator.build(raw).get.packet
    val relativeLayers =
      packet.evidenceGraph.records.collect {
        case EvidenceRecord(ref, _: RelativeAssessmentEvidence, _)         => ref.layer
        case EvidenceRecord(ref, _: CandidateComparisonEvidence, _)        => ref.layer
        case EvidenceRecord(ref, _: RelativeCauseFactEvidence, _)          => ref.layer
        case EvidenceRecord(ref, _: MoveVerdictCertificationEvidence, _)   => ref.layer
      }

    assert(packet.playedTransition.exists(_.moveUci == "b1c3"))
    assert(!packet.candidateLines.exists(_.role == LineNodeRole.Played))
    assertEquals(packet.relativeAssessments, Nil)
    assertEquals(relativeLayers, Nil)

  test("branch reply multipv creates threat lines without contaminating root candidate comparisons"):
    val baseRaw = RawMoveReviewInput(
      fen = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1",
      playedMoveUci = "e2e4",
      variations = List(
        VariationLine(moves = List("e2e4", "c7c5"), scoreCp = 30, depth = 18),
        VariationLine(moves = List("d2d4", "d7d5"), scoreCp = 20, depth = 18),
        VariationLine(moves = List("g1f3", "g8f6"), scoreCp = 10, depth = 18)
      ),
      currentEvalCp = Some(0),
      ply = Some(0)
    )
    val e4Request =
      MoveReviewJudgmentOrchestrator
        .build(baseRaw)
        .get
        .packet
        .probeRequests
        .find(_.candidateMove.contains("e2e4"))
        .get
    val raw = baseRaw.copy(
      probeResults = List(
        ProbeResult(
          id = e4Request.id,
          fen = Some(e4Request.fen),
          evalCp = -20,
          bestReplyPv = List("c7c5", "g1f3"),
          replyLines = Some(
            List(
              VariationLine(moves = List("c7c5", "g1f3"), scoreCp = -20, depth = 16),
              VariationLine(moves = List("e7e5", "g1f3"), scoreCp = 85, depth = 16),
              VariationLine(moves = List("e7e6", "d2d4"), scoreCp = 40, depth = 16)
            )
          ),
          deltaVsBaseline = -50,
          keyMotifs = Nil,
          purpose = Some(ProbePurpose.ReplyMultipv),
          probedMove = Some("e2e4"),
          depth = Some(16),
          depthFloor = e4Request.depthFloor,
          variationHash = e4Request.variationHash
        )
      )
    )

    val result = MoveReviewJudgmentOrchestrator.build(raw).get
    val packet = result.packet
    val threatLines = packet.candidateLines.filter(_.role == LineNodeRole.Threat)
    val rootCandidateComparisons =
      packet.evidenceGraph.records.collect { case EvidenceRecord(_, CandidateComparisonEvidence(fact), _) => fact }
    val branchThreatPressure =
      packet.evidenceGraph.records.collect {
        case EvidenceRecord(ref, ThreatPressureEvidence(_, threats), _)
            if ref.scope == EvidenceScope.ThreatLine =>
          threats
      }

    assertEquals(threatLines.map(_.ref.rootMove).toSet, Set("c7c5", "e7e5", "e7e6"))
    assert(rootCandidateComparisons.nonEmpty)
    assert(rootCandidateComparisons.forall(fact =>
      fact.referenceLine.role != LineNodeRole.Threat && fact.candidateLine.role != LineNodeRole.Threat
    ))
    assert(branchThreatPressure.exists(threats => !threats.insufficientData))
    assertEquals(packet.probeRequests.flatMap(_.candidateMove).toSet, Set("d2d4"))
    assertEquals(packet.probeDiagnostics.count(_.status == ProbeAdmissionStatus.Admitted), 1)
    assertEquals(result.quality.semanticCoverage.branchReplyProbeAdmittedMoves, List("e2e4"))
    assertEquals(result.quality.semanticCoverage.hasPendingBranchReplyDepth, true)
    assertEquals(result.quality.semanticCoverage.branchReplyThreatLines, 3)

  test("branch reply multipv rejects unbound stale or shallow probe results"):
    val baseRaw = RawMoveReviewInput(
      fen = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1",
      playedMoveUci = "e2e4",
      variations = List(
        VariationLine(moves = List("e2e4", "c7c5"), scoreCp = 30, depth = 18),
        VariationLine(moves = List("d2d4", "d7d5"), scoreCp = 20, depth = 18),
        VariationLine(moves = List("g1f3", "g8f6"), scoreCp = 10, depth = 18)
      ),
      currentEvalCp = Some(0),
      ply = Some(0)
    )
    val e4Request =
      MoveReviewJudgmentOrchestrator
        .build(baseRaw)
        .get
        .packet
        .probeRequests
        .find(_.candidateMove.contains("e2e4"))
        .get
    val staleRaw = baseRaw.copy(
      probeResults = List(
        ProbeResult(
          id = e4Request.id,
          fen = Some(e4Request.fen),
          evalCp = -20,
          bestReplyPv = List("c7c5", "g1f3"),
          replyLines = Some(
            List(
              VariationLine(moves = List("c7c5", "g1f3"), scoreCp = -20, depth = 16),
              VariationLine(moves = List("e7e5", "g1f3"), scoreCp = 85, depth = 16)
            )
          ),
          deltaVsBaseline = -50,
          keyMotifs = Nil,
          purpose = Some(ProbePurpose.ReplyMultipv),
          probedMove = Some("e2e4"),
          depth = Some(16),
          depthFloor = e4Request.depthFloor,
          variationHash = Some("stale-variation")
        )
      )
    )

    val result = MoveReviewJudgmentOrchestrator.build(staleRaw).get

    assert(!result.packet.candidateLines.exists(_.role == LineNodeRole.Threat))
    assertEquals(result.packet.probeRequests.flatMap(_.candidateMove).toSet, Set("e2e4", "d2d4"))
    assertEquals(result.quality.semanticCoverage.branchReplyProbeRejectedResults, 1)
    assert(result.quality.semanticCoverage.branchReplyProbeRejectReasons.contains("VARIATION_HASH_MISMATCH"))
    assertEquals(result.quality.semanticCoverage.hasPendingBranchReplyDepth, true)
    val partialRaw = baseRaw.copy(
      probeResults = List(
        ProbeResult(
          id = e4Request.id,
          fen = Some(e4Request.fen),
          evalCp = -20,
          bestReplyPv = List("c7c5", "g1f3"),
          replyLines = Some(
            List(
              VariationLine(moves = List("c7c5", "g1f3"), scoreCp = -20, depth = 16),
              VariationLine(moves = List("e7e5", "g1f3"), scoreCp = 85, depth = 16)
            )
          ),
          deltaVsBaseline = -50,
          keyMotifs = Nil,
          purpose = Some(ProbePurpose.ReplyMultipv),
          probedMove = Some("e2e4"),
          depth = Some(16),
          depthFloor = e4Request.depthFloor,
          variationHash = e4Request.variationHash
        )
      )
    )
    val partialResult = MoveReviewJudgmentOrchestrator.build(partialRaw).get

    assert(!partialResult.packet.candidateLines.exists(_.role == LineNodeRole.Threat))
    assertEquals(partialResult.packet.probeRequests.flatMap(_.candidateMove).toSet, Set("e2e4", "d2d4"))
    assertEquals(partialResult.quality.semanticCoverage.branchReplyProbeRejectedResults, 1)
    assert(partialResult.quality.semanticCoverage.branchReplyProbeRejectReasons.contains("REPLY_MULTIPV_INCOMPLETE"))
    val shallowRaw = baseRaw.copy(
      probeResults = List(
        ProbeResult(
          id = e4Request.id,
          fen = Some(e4Request.fen),
          evalCp = -20,
          bestReplyPv = List("c7c5", "g1f3"),
          replyLines = Some(
            List(
              VariationLine(moves = List("c7c5", "g1f3"), scoreCp = -20, depth = 1),
              VariationLine(moves = List("e7e5", "g1f3"), scoreCp = 85, depth = 1),
              VariationLine(moves = List("e7e6", "d2d4"), scoreCp = 40, depth = 1)
            )
          ),
          deltaVsBaseline = -50,
          keyMotifs = Nil,
          purpose = Some(ProbePurpose.ReplyMultipv),
          probedMove = Some("e2e4"),
          depth = Some(16),
          depthFloor = e4Request.depthFloor,
          variationHash = e4Request.variationHash
        )
      )
    )
    val shallowResult = MoveReviewJudgmentOrchestrator.build(shallowRaw).get

    assert(!shallowResult.packet.candidateLines.exists(_.role == LineNodeRole.Threat))
    assertEquals(shallowResult.packet.probeRequests.flatMap(_.candidateMove).toSet, Set("e2e4", "d2d4"))
    assertEquals(shallowResult.quality.semanticCoverage.branchReplyProbeRejectedResults, 1)
    assert(shallowResult.quality.semanticCoverage.branchReplyProbeRejectReasons.contains("REPLY_LINE_DEPTH_FLOOR_UNMET"))

  test("packet requests scored branch reply multipv when branch evidence is absent"):
    val raw = RawMoveReviewInput(
      fen = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1",
      playedMoveUci = "e2e4",
      variations = List(
        VariationLine(moves = List("e2e4", "c7c5"), scoreCp = 30, depth = 18),
        VariationLine(moves = List("d2d4", "d7d5"), scoreCp = 20, depth = 18),
        VariationLine(moves = List("g1f3", "g8f6"), scoreCp = 10, depth = 18)
      ),
      currentEvalCp = Some(0),
      ply = Some(0)
    )

    val result = MoveReviewJudgmentOrchestrator.build(raw).get
    val requests = result.packet.probeRequests

    assert(requests.nonEmpty)
    assertEquals(requests.map(_.candidateMove).flatten.toSet, Set("e2e4", "d2d4"))
    assert(requests.forall(_.purpose.contains(ProbePurpose.ReplyMultipv)))
    assert(requests.forall(_.requiredSignals.contains("replyPvs")))
    assert(requests.forall(_.multiPv.contains(3)))
    assertEquals(result.quality.semanticCoverage.hasPendingBranchReplyDepth, true)
    assertEquals(result.quality.semanticCoverage.branchReplyProbeRequests, requests.size)
    assertEquals(result.quality.semanticCoverage.branchReplyThreatLines, 0)

  test("reply multipv probe contract requires scored reply lines"):
    val unscored =
      ProbeResult(
        id = "probe-unscored",
        fen = Some("rnbqkbnr/pppppppp/8/8/4P3/8/PPPP1PPP/RNBQKBNR b KQkq - 0 1"),
        evalCp = -20,
        bestReplyPv = List("c7c5", "g1f3"),
        replyPvs = Some(List(List("c7c5", "g1f3"), List("e7e5", "g1f3"))),
        deltaVsBaseline = -50,
        keyMotifs = Nil,
        purpose = Some(ProbePurpose.ReplyMultipv),
        probedMove = Some("e2e4"),
        depth = Some(16)
      )
    val scored =
      unscored.copy(
        id = "probe-scored",
        replyLines = Some(
          List(
            VariationLine(moves = List("c7c5", "g1f3"), scoreCp = -20, depth = 16),
            VariationLine(moves = List("e7e5", "g1f3"), scoreCp = 85, depth = 16),
            VariationLine(moves = List("e7e6", "d2d4"), scoreCp = 40, depth = 16)
          )
        )
      )
    val partialScored =
      scored.copy(
        id = "probe-partial-scored",
        replyLines = Some(
          List(
            VariationLine(moves = List("c7c5", "g1f3"), scoreCp = -20, depth = 16),
            VariationLine(moves = List("e7e5", "g1f3"), scoreCp = 85, depth = 16)
          )
        )
      )
    val partialRequest =
      ProbeRequest(
        id = "probe-partial-scored",
        fen = unscored.fen.get,
        moves = List("e2e4"),
        depth = 16,
        purpose = Some(ProbePurpose.ReplyMultipv),
        multiPv = Some(3),
        requiredSignals = List("replyPvs"),
        candidateMove = Some("e2e4"),
        depthFloor = Some(12)
      )
    val partialRequestValidation =
      ProbeContractValidator.validateAgainstRequest(partialRequest, partialScored)

    assertEquals(ProbeContractValidator.validate(unscored).isValid, false)
    assertEquals(ProbeContractValidator.validate(partialScored).isValid, false)
    assert(ProbeContractValidator.validate(partialScored).reasonCodes.contains("MISSING_REQUIRED_SIGNALS"))
    assertEquals(partialRequestValidation.isValid, false)
    assert(partialRequestValidation.reasonCodes.contains("REPLY_MULTIPV_INCOMPLETE"))
    assertEquals(ProbeContractValidator.validate(scored).isValid, true)

  test("packet validator rejects malformed branch reply probe requests"):
    val raw = RawMoveReviewInput(
      fen = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1",
      playedMoveUci = "e2e4",
      variations = List(
        VariationLine(moves = List("e2e4", "c7c5"), scoreCp = 30, depth = 18),
        VariationLine(moves = List("d2d4", "d7d5"), scoreCp = 20, depth = 18)
      ),
      currentEvalCp = Some(0),
      ply = Some(0)
    )
    val packet = MoveReviewJudgmentOrchestrator.build(raw).get.packet
    val malformed =
      ProbeRequest(
        id = "",
        fen = "not-a-fen",
        moves = List("not-uci"),
        depth = 0,
        purpose = Some(ProbePurpose.ReplyMultipv),
        multiPv = Some(1),
        requiredSignals = List("replyPvs"),
        candidateMove = Some("not-uci"),
        depthFloor = None,
        variationHash = None
      )
    val result = JudgmentPacketValidator.validate(packet.copy(probeRequests = List(malformed)))

    assert(result.issues.exists(_.kind == JudgmentPacketValidationIssueKind.InvalidProbeRequest))

  test("defensive threat claims require claim-grade pressure instead of bare threat hints"):
    val lineRef = ref("line-defensive-proof", EvidenceLayer.Line, EvidenceProducer.LegalLineProducer)
    val weakThreatRef = ref("threat-weak-hint", EvidenceLayer.ThreatPressure, EvidenceProducer.ThreatPressureProducer)
    val onlyDefenseRef =
      ref("threat-bare-only-defense", EvidenceLayer.ThreatPressure, EvidenceProducer.ThreatPressureProducer)
    val urgentInsufficientRef =
      ref("threat-urgent-insufficient", EvidenceLayer.ThreatPressure, EvidenceProducer.ThreatPressureProducer)
    val mateInsufficientRef =
      ref("threat-mate-insufficient", EvidenceLayer.ThreatPressure, EvidenceProducer.ThreatPressureProducer)
    val strongThreatRef =
      ref("threat-claim-grade", EvidenceLayer.ThreatPressure, EvidenceProducer.ThreatPressureProducer)

    val weakClaim =
      claim(
        id = "claim-defensive-weak-hint",
        family = ClaimFamily.Defensive,
        evidence = List(weakThreatRef, lineRef),
        engineComparison = None
      )
    val onlyDefenseClaim =
      claim(
        id = "claim-defensive-bare-only-defense",
        family = ClaimFamily.Defensive,
        evidence = List(onlyDefenseRef, lineRef),
        engineComparison = None
      )
    val strongClaim =
      claim(
        id = "claim-defensive-claim-grade",
        family = ClaimFamily.Defensive,
        evidence = List(strongThreatRef, lineRef),
        engineComparison = None
      )
    val urgentInsufficientClaim =
      claim(
        id = "claim-defensive-urgent-insufficient",
        family = ClaimFamily.Defensive,
        evidence = List(urgentInsufficientRef, lineRef),
        engineComparison = None
      )
    val mateInsufficientClaim =
      claim(
        id = "claim-defensive-mate-insufficient",
        family = ClaimFamily.Defensive,
        evidence = List(mateInsufficientRef, lineRef),
        engineComparison = None
      )
    val graph =
      TypedEvidenceGraph(
        List(
          EvidenceRecord(
            lineRef,
            LineFactEvidence(
              line = line,
              firstMove = Some(line.rootMove),
              replyMove = Some("d8d3"),
              continuationMoves = List("e2d3")
            )
          ),
          EvidenceRecord(
            weakThreatRef,
            ThreatPressureEvidence(
              Color.White,
              threatAnalysis(
                severity = ThreatSeverity.Low,
                winPercentLoss = Some(1.0),
                defenseRequired = false,
                onlyDefense = None,
                insufficientData = true
              )
            )
          ),
          EvidenceRecord(
            onlyDefenseRef,
            ThreatPressureEvidence(
              Color.White,
              threatAnalysis(
                severity = ThreatSeverity.Low,
                winPercentLoss = Some(1.6),
                defenseRequired = false,
                onlyDefense = Some(line.rootMove),
                insufficientData = true
              )
            )
          ),
          EvidenceRecord(
            urgentInsufficientRef,
            ThreatPressureEvidence(
              Color.White,
              threatAnalysis(
                severity = ThreatSeverity.Urgent,
                winPercentLoss = Some(18.0),
                defenseRequired = true,
                onlyDefense = Some(line.rootMove),
                insufficientData = true
              )
            )
          ),
          EvidenceRecord(
            mateInsufficientRef,
            ThreatPressureEvidence(
              Color.White,
              threatAnalysis(
                severity = ThreatSeverity.Urgent,
                winPercentLoss = Some(100.0),
                defenseRequired = true,
                onlyDefense = Some(line.rootMove),
                insufficientData = true,
                mateThreat = true
              )
            )
          ),
          EvidenceRecord(
            strongThreatRef,
            ThreatPressureEvidence(
              Color.White,
              threatAnalysis(
                severity = ThreatSeverity.Important,
                winPercentLoss = Some(6.0),
                defenseRequired = true,
                onlyDefense = Some(line.rootMove),
                insufficientData = false
              )
            )
          )
        )
      )

    assertEquals(ClaimTruthPolicy.evaluate(weakClaim, graph).status, ClaimTruthStatus.Deferred)
    assertEquals(ClaimTruthPolicy.evaluate(onlyDefenseClaim, graph).status, ClaimTruthStatus.Deferred)
    assertEquals(ClaimTruthPolicy.evaluate(urgentInsufficientClaim, graph).status, ClaimTruthStatus.Deferred)
    assertEquals(ClaimTruthPolicy.evaluate(mateInsufficientClaim, graph).status, ClaimTruthStatus.Certified)
    assertEquals(ClaimTruthPolicy.evaluate(strongClaim, graph).status, ClaimTruthStatus.Certified)

  test("branch-local threat pressure does not directly certify root defensive claims"):
    val playedLineRef = ref("line-root-defensive", EvidenceLayer.Line, EvidenceProducer.LegalLineProducer)
    val branchThreatRef =
      playedLineRef.copy(
        id = "threat-branch-local",
        layer = EvidenceLayer.ThreatPressure,
        scope = EvidenceScope.ThreatLine
      )
    val rootDefensiveClaim =
      claim(
        id = "claim-root-defensive-from-branch-pressure",
        family = ClaimFamily.Defensive,
        evidence = List(playedLineRef, branchThreatRef),
        engineComparison = None
      )
    val graph =
      TypedEvidenceGraph(
        List(
          EvidenceRecord(
            playedLineRef,
            LineFactEvidence(
              line = line,
              firstMove = Some(line.rootMove),
              replyMove = Some("d8d3"),
              continuationMoves = List("e2d3")
            )
          ),
          EvidenceRecord(
            branchThreatRef,
            ThreatPressureEvidence(
              Color.White,
              threatAnalysis(
                severity = ThreatSeverity.Important,
                winPercentLoss = Some(8.0),
                defenseRequired = true,
                onlyDefense = Some(line.rootMove),
                insufficientData = false
              )
            )
          )
        )
      )

    assertEquals(ClaimTruthPolicy.evaluate(rootDefensiveClaim, graph).status, ClaimTruthStatus.Deferred)

  private def ref(
      id: String,
      layer: EvidenceLayer,
      producer: EvidenceProducer
  ): EvidenceRef =
    EvidenceRef(
      id = id,
      producer = producer,
      layer = layer,
      position = root,
      line = Some(line),
      scope = EvidenceScope.PlayedLine,
      confidence = EvidenceConfidence.EngineBacked
    )

  private def threatAnalysis(
      severity: ThreatSeverity,
      winPercentLoss: Option[Double],
      defenseRequired: Boolean,
      onlyDefense: Option[String],
      insufficientData: Boolean,
      mateThreat: Boolean = false
  ): ThreatAnalysis =
    val kind =
      if mateThreat then ThreatKind.Mate
      else if severity == ThreatSeverity.Low then ThreatKind.Positional
      else ThreatKind.Material
    val driver =
      if mateThreat then ThreatDriver.MateThreat
      else if severity == ThreatSeverity.Low then ThreatDriver.PositionalThreat
      else ThreatDriver.MaterialThreat
    val lossCp =
      if mateThreat then 0
      else if severity == ThreatSeverity.Low then 120
      else 450
    val threat =
      Threat(
        kind = kind,
        lossIfIgnoredCp = lossCp,
        lossIfIgnoredWinPercent = winPercentLoss,
        turnsToImpact = 1,
        evidenceSource = ThreatEvidenceSource.CandidateLineValueDelta,
        motifs = Nil,
        attackSquares = Nil,
        targetPieces = List("target"),
        bestDefense = onlyDefense,
        defenseCount = if onlyDefense.nonEmpty then 1 else 3
      )
    ThreatAnalysis(
      threats = List(threat),
      defense = DefenseAssessment(
        necessity = severity,
        onlyDefense = onlyDefense,
        alternatives = if onlyDefense.nonEmpty then Nil else List("g1f3"),
        counterIsBetter = false,
        prophylaxisNeeded = false,
        resourceCoverageScore = if defenseRequired then 25 else 80
      ),
      threatSeverity = severity,
      immediateThreat = true,
      strategicThreat = false,
      threatIgnorable = !defenseRequired && severity == ThreatSeverity.Low,
      defenseRequired = defenseRequired,
      counterThreatBetter = false,
      prophylaxisNeeded = false,
      resourceAvailable = true,
      maxLossIfIgnored = lossCp,
      maxWinPercentLossIfIgnored = winPercentLoss,
      primaryDriver = driver,
      insufficientData = insufficientData
    )

  private def claim(
      id: String,
      family: ClaimFamily,
      evidence: List[EvidenceRef],
      engineComparison: Option[EvalComparison]
  ): ClaimSeed =
    ClaimSeed(
      id = id,
      family = family,
      idea = None,
      subject = if family == ClaimFamily.Plan then IdeaSubject.Plan else IdeaSubject.PlayedMove,
      primaryPosition = root,
      primaryLine = Some(line),
      subjectMove = Some(line.rootMove),
      evidence = evidence,
      supportingFacts = Nil,
      engineComparison = engineComparison,
      scope = EvidenceScope.PlayedLine,
      confidence = EvidenceConfidence.EngineBacked
    )

  private def certified(claim: ClaimSeed): ClaimTruthDecision =
    ClaimTruthDecision(
      claim = claim,
      status = ClaimTruthStatus.Certified,
      presentLayers = claim.evidence.map(_.layer).toSet,
      missingLayerGroups = Nil,
      missingEvidence = Nil
    )

  private def deferred(claim: ClaimSeed): ClaimTruthDecision =
    ClaimTruthDecision(
      claim = claim,
      status = ClaimTruthStatus.Deferred,
      presentLayers = claim.evidence.map(_.layer).toSet,
      missingLayerGroups = List(Set(EvidenceLayer.Line)),
      missingEvidence = Nil
    )

  private val engineComparison =
    EvalComparison(
      mover = Color.White,
      referenceLine = line,
      candidateLine = line,
      candidateDeltaForMover = -50,
      candidateWinPercentDeltaForMover = 0.0,
      cpLossForMover = 50,
      winPercentLossForMover = 5.0,
      verdict = MoveChoiceVerdict.Inaccuracy
    )

  private val planMatch =
    PlanMatch(
      plan = Plan.CentralControl(Color.White),
      score = 3.0,
      evidence = Nil
    )
  private val activePlans =
    ActivePlans(
      primary = planMatch,
      secondary = None,
      suppressed = Nil,
      allPlans = List(planMatch)
    )
  private val planScoring =
    PlanScoringResult(
      topPlans = List(planMatch),
      confidence = 1.0,
      phase = "middlegame"
    )

  private val structureProfile =
    StructureProfile(
      primary = StructureId.IQPWhite,
      confidence = 0.8,
      alternatives = Nil,
      centerState = CenterState.Fluid,
      evidenceCodes = List("test-iqp")
    )
