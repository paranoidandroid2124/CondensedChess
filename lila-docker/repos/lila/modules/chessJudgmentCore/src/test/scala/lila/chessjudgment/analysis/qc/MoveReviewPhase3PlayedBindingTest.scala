package lila.chessjudgment.analysis.qc

import chess.Color
import lila.chessjudgment.model.judgment.*
import lila.chessjudgment.model.strategic.VariationLine

class MoveReviewPhase3PlayedBindingTest extends munit.FunSuite:

  private val root = PositionNodeRef(
    fen = "8/8/8/8/8/8/8/8 w - - 0 1",
    ply = 12
  )
  private val playedLine = LineNodeRef("played-line", "d1d3", 1, LineNodeRole.Played)
  private val referenceLine = LineNodeRef("reference-line", "e2e4", 1, LineNodeRole.BestReference)
  private val alternativeLine = LineNodeRef("alternative-line", "f1b5", 2, LineNodeRole.Alternative)

  test("played-bound summary ignores tactical evidence that only belongs to another candidate line"):
    val alternativeIdea =
      idea(
        id = "idea-alt",
        family = ChessIdeaFamily.Tactical,
        line = Some(alternativeLine),
        move = Some("f1b5")
      )
    val alternativeClaim =
      claim(
        id = "claim-alt",
        family = ClaimFamily.Tactical,
        line = Some(alternativeLine),
        move = Some("f1b5")
      )

    val summary = MoveReviewPhase3PlayedBindingSummary.from(
      playedMoveUci = "d1d3",
      ideas = List(alternativeIdea),
      claims = List(alternativeClaim)
    )

    assertEquals(summary.playedBoundIdeaFamilies, Nil)
    assertEquals(summary.playedBoundClaimFamilies, Nil)
    assertEquals(summary.playedBoundFamilies, Nil)

  test("played-bound summary counts subject move and played line bindings"):
    val subjectMoveIdea =
      idea(
        id = "idea-subject",
        family = ChessIdeaFamily.Evaluation,
        line = Some(alternativeLine),
        move = Some("d1d3")
      )
    val playedLineClaim =
      claim(
        id = "claim-played-line",
        family = ClaimFamily.Tactical,
        line = Some(playedLine),
        move = None
      )

    val summary = MoveReviewPhase3PlayedBindingSummary.from(
      playedMoveUci = "d1d3",
      ideas = List(subjectMoveIdea),
      claims = List(playedLineClaim)
    )

    assertEquals(summary.playedBoundIdeaFamilies, List("Evaluation"))
    assertEquals(summary.playedBoundClaimFamilies, List("Tactical"))
    assertEquals(summary.playedBoundFamilies, List("Evaluation", "Tactical"))

  test("semantic coverage exposes comparison-level unexplained engine gap diagnostics"):
    val referenceLineRef = evidenceRef("line-reference", EvidenceLayer.Line, EvidenceProducer.LegalLineProducer, referenceLine)
    val referenceEvalRef = evidenceRef("eval-reference", EvidenceLayer.Eval, EvidenceProducer.EngineEvalProducer, referenceLine)
    val playedLineRef = evidenceRef("line-played", EvidenceLayer.Line, EvidenceProducer.LegalLineProducer, playedLine)
    val playedEvalRef = evidenceRef("eval-played", EvidenceLayer.Eval, EvidenceProducer.EngineEvalProducer, playedLine)
    val comparisonRef =
      evidenceRef("comparison-played-best", EvidenceLayer.CandidateComparison, EvidenceProducer.RelativeMoveProducer, playedLine)
    val comparison =
      EvalComparison(
        mover = Color.White,
        referenceLine = referenceLine,
        candidateLine = playedLine,
        candidateDeltaForMover = -270,
        candidateWinPercentDeltaForMover = 0.0,
        cpLossForMover = 270,
        winPercentLossForMover = 12.5,
        verdict = MoveChoiceVerdict.Mistake
      )
    val comparisonFact =
      CandidateComparisonFact(
        kind = CandidateComparisonKind.PlayedVsBest,
        referenceLine = referenceLine,
        candidateLine = playedLine,
        comparison = comparison
      )
    val packet =
      EvidenceBackedJudgmentPacket(
        root = root,
        positions = Nil,
        candidateLines = List(
          candidateLine(referenceLine, evalCp = 90, depth = 18),
          candidateLine(playedLine, evalCp = -180, depth = 18)
        ),
        transitions = Nil,
        relativeAssessments = Nil,
        evidenceGraph = TypedEvidenceGraph(
          List(
            EvidenceRecord(referenceLineRef, LineFactEvidence(referenceLine, Some("e2e4"), None, Nil)),
            EvidenceRecord(referenceEvalRef, EvalFactEvidence(referenceLine, evalCp = 90, mate = None, depth = 18)),
            EvidenceRecord(playedLineRef, LineFactEvidence(playedLine, Some("d1d3"), None, Nil)),
            EvidenceRecord(playedEvalRef, EvalFactEvidence(playedLine, evalCp = -180, mate = None, depth = 18)),
            EvidenceRecord(
              comparisonRef,
              CandidateComparisonEvidence(comparisonFact),
              parents = List(referenceLineRef, referenceEvalRef, playedLineRef, playedEvalRef)
            )
          )
        ),
        ideas = Nil,
        claims = Nil,
        ideaVerdict = None
      )

    val semantic = SemanticCoverageMetrics.from(packet)
    val diagnostic = semantic.comparisonDiagnostics.find(_.id == "comparison-played-best").get

    assert(semantic.hasPlayedUnexplainedEngineGap)
    assertEquals(semantic.hasContextUnexplainedEngineGap, false)
    assertEquals(semantic.contextUnexplainedComparisonIds, Nil)
    assertEquals(diagnostic.comparisonKind, CandidateComparisonKind.PlayedVsBest)
    assertEquals(diagnostic.referenceLine.id, "reference-line")
    assertEquals(diagnostic.referenceLine.evalCp, Some(90))
    assertEquals(diagnostic.candidateLine.id, "played-line")
    assertEquals(diagnostic.candidateLine.evalCp, Some(-180))
    assertEquals(diagnostic.verdict, MoveChoiceVerdict.Mistake)
    assertEquals(diagnostic.winPercentLossForMover, 12.5)
    assertEquals(diagnostic.candidateWinPercentDeltaForMover, 0.0)
    assertEquals(diagnostic.causeKinds, Nil)
    assert(diagnostic.hasUnexplainedEngineGap)
    assertEquals(diagnostic.evidenceLayers.reference.directLayers.contains(EvidenceLayer.Eval), true)
    assertEquals(
      diagnostic.evidenceLayers.reference.parentLayerCounts.getOrElse(EvidenceLayer.CandidateComparison, 0),
      0
    )
    assertEquals(
      diagnostic.failedCauseCandidates.contains(RelativeCauseKind.MaterialSwing),
      false
    )
    assert(diagnostic.failureReasons.contains(CandidateComparisonFailureReason.NoCauseGenerated))
    assertEquals(diagnostic.failureClass, CandidateComparisonFailureClass.FailedCauseTemplate)

  test("played-vs-alternative unexplained gaps remain context diagnostics"):
    val referenceLineRef = evidenceRef("line-reference-alt", EvidenceLayer.Line, EvidenceProducer.LegalLineProducer, referenceLine)
    val referenceEvalRef = evidenceRef("eval-reference-alt", EvidenceLayer.Eval, EvidenceProducer.EngineEvalProducer, referenceLine)
    val playedLineRef = evidenceRef("line-played-alt", EvidenceLayer.Line, EvidenceProducer.LegalLineProducer, playedLine)
    val playedEvalRef = evidenceRef("eval-played-alt", EvidenceLayer.Eval, EvidenceProducer.EngineEvalProducer, playedLine)
    val comparisonRef =
      evidenceRef("comparison-played-alternative", EvidenceLayer.CandidateComparison, EvidenceProducer.RelativeMoveProducer, playedLine)
    val comparison =
      EvalComparison(
        mover = Color.White,
        referenceLine = referenceLine,
        candidateLine = playedLine,
        candidateDeltaForMover = -180,
        candidateWinPercentDeltaForMover = 0.0,
        cpLossForMover = 180,
        winPercentLossForMover = 8.5,
        verdict = MoveChoiceVerdict.Mistake
      )
    val comparisonFact =
      CandidateComparisonFact(
        kind = CandidateComparisonKind.PlayedVsAlternative,
        referenceLine = referenceLine,
        candidateLine = playedLine,
        comparison = comparison
      )
    val packet =
      EvidenceBackedJudgmentPacket(
        root = root,
        positions = Nil,
        candidateLines = List(
          candidateLine(referenceLine, evalCp = 40, depth = 18),
          candidateLine(playedLine, evalCp = -140, depth = 18)
        ),
        transitions = Nil,
        relativeAssessments = Nil,
        evidenceGraph = TypedEvidenceGraph(
          List(
            EvidenceRecord(referenceLineRef, LineFactEvidence(referenceLine, Some("e2e4"), None, Nil)),
            EvidenceRecord(referenceEvalRef, EvalFactEvidence(referenceLine, evalCp = 40, mate = None, depth = 18)),
            EvidenceRecord(playedLineRef, LineFactEvidence(playedLine, Some("d1d3"), None, Nil)),
            EvidenceRecord(playedEvalRef, EvalFactEvidence(playedLine, evalCp = -140, mate = None, depth = 18)),
            EvidenceRecord(
              comparisonRef,
              CandidateComparisonEvidence(comparisonFact),
              parents = List(referenceLineRef, referenceEvalRef, playedLineRef, playedEvalRef)
            )
          )
        ),
        ideas = Nil,
        claims = Nil,
        ideaVerdict = None
      )

    val semantic = SemanticCoverageMetrics.from(packet)
    val diagnostic = semantic.comparisonDiagnostics.find(_.id == "comparison-played-alternative").get

    assertEquals(semantic.hasPlayedUnexplainedEngineGap, false)
    assertEquals(semantic.hasContextUnexplainedEngineGap, true)
    assertEquals(semantic.hasSecondaryContextEngineGap, true)
    assertEquals(semantic.secondaryContextComparisonIds, List("comparison-played-alternative"))
    assertEquals(semantic.secondaryContextWithPrimaryCoverageIds, Nil)
    assertEquals(semantic.secondaryContextWithoutPrimaryCoverageIds, List("comparison-played-alternative"))
    assertEquals(semantic.contextUnexplainedComparisonIds, List("comparison-played-alternative"))
    assertEquals(diagnostic.hasUnexplainedEngineGap, false)
    assert(diagnostic.hasSecondaryContextEngineGap)
    assertEquals(diagnostic.comparisonKind, CandidateComparisonKind.PlayedVsAlternative)
    assertEquals(diagnostic.failureClass, CandidateComparisonFailureClass.SecondaryContextGap)

  test("secondary context gaps stay visible when primary played comparison is covered"):
    val referenceLineRef =
      evidenceRef("line-reference-covered", EvidenceLayer.Line, EvidenceProducer.LegalLineProducer, referenceLine)
    val referenceEvalRef =
      evidenceRef("eval-reference-covered", EvidenceLayer.Eval, EvidenceProducer.EngineEvalProducer, referenceLine)
    val playedLineRef = evidenceRef("line-played-covered", EvidenceLayer.Line, EvidenceProducer.LegalLineProducer, playedLine)
    val playedEvalRef = evidenceRef("eval-played-covered", EvidenceLayer.Eval, EvidenceProducer.EngineEvalProducer, playedLine)
    val alternativeLineRef =
      evidenceRef("line-alternative-secondary", EvidenceLayer.Line, EvidenceProducer.LegalLineProducer, alternativeLine)
    val alternativeEvalRef =
      evidenceRef("eval-alternative-secondary", EvidenceLayer.Eval, EvidenceProducer.EngineEvalProducer, alternativeLine)
    val primaryComparisonRef =
      evidenceRef("comparison-played-best-covered", EvidenceLayer.CandidateComparison, EvidenceProducer.RelativeMoveProducer, playedLine)
    val primaryCauseRef =
      evidenceRef("cause-played-best-covered", EvidenceLayer.RelativeCause, EvidenceProducer.RelativeMoveProducer, playedLine)
    val secondaryComparisonRef =
      evidenceRef(
        "comparison-reference-alternative-secondary",
        EvidenceLayer.CandidateComparison,
        EvidenceProducer.RelativeMoveProducer,
        alternativeLine
      )
    val primaryComparison =
      EvalComparison(
        mover = Color.White,
        referenceLine = referenceLine,
        candidateLine = playedLine,
        candidateDeltaForMover = -150,
        candidateWinPercentDeltaForMover = 0.0,
        cpLossForMover = 150,
        winPercentLossForMover = 7.0,
        verdict = MoveChoiceVerdict.Mistake
      )
    val secondaryComparison =
      EvalComparison(
        mover = Color.White,
        referenceLine = referenceLine,
        candidateLine = alternativeLine,
        candidateDeltaForMover = -140,
        candidateWinPercentDeltaForMover = -9.0,
        cpLossForMover = 140,
        winPercentLossForMover = 9.0,
        verdict = MoveChoiceVerdict.Mistake
      )
    val primaryComparisonFact =
      CandidateComparisonFact(
        kind = CandidateComparisonKind.PlayedVsBest,
        referenceLine = referenceLine,
        candidateLine = playedLine,
        comparison = primaryComparison
      )
    val secondaryComparisonFact =
      CandidateComparisonFact(
        kind = CandidateComparisonKind.ReferenceVsAlternative,
        referenceLine = referenceLine,
        candidateLine = alternativeLine,
        comparison = secondaryComparison
      )
    val primaryCause =
      RelativeCauseFact(
        kind = RelativeCauseKind.MissedTacticalResource,
        comparisonKind = CandidateComparisonKind.PlayedVsBest,
        referenceLine = referenceLine,
        candidateLine = playedLine,
        verdict = MoveChoiceVerdict.Mistake,
        winPercentLossForMover = 7.0,
        candidateWinPercentDeltaForMover = 0.0,
        evidenceLines = List(referenceLine, playedLine)
      )
    val packet =
      EvidenceBackedJudgmentPacket(
        root = root,
        positions = Nil,
        candidateLines = List(
          candidateLine(referenceLine, evalCp = 80, depth = 18),
          candidateLine(playedLine, evalCp = -70, depth = 18),
          candidateLine(alternativeLine, evalCp = -60, depth = 18)
        ),
        transitions = Nil,
        relativeAssessments = Nil,
        evidenceGraph = TypedEvidenceGraph(
          List(
            EvidenceRecord(referenceLineRef, LineFactEvidence(referenceLine, Some("e2e4"), None, Nil)),
            EvidenceRecord(referenceEvalRef, EvalFactEvidence(referenceLine, evalCp = 80, mate = None, depth = 18)),
            EvidenceRecord(playedLineRef, LineFactEvidence(playedLine, Some("d1d3"), None, Nil)),
            EvidenceRecord(playedEvalRef, EvalFactEvidence(playedLine, evalCp = -70, mate = None, depth = 18)),
            EvidenceRecord(alternativeLineRef, LineFactEvidence(alternativeLine, Some("f1b5"), None, Nil)),
            EvidenceRecord(alternativeEvalRef, EvalFactEvidence(alternativeLine, evalCp = -60, mate = None, depth = 18)),
            EvidenceRecord(
              primaryComparisonRef,
              CandidateComparisonEvidence(primaryComparisonFact),
              parents = List(referenceLineRef, referenceEvalRef, playedLineRef, playedEvalRef)
            ),
            EvidenceRecord(
              primaryCauseRef,
              RelativeCauseFactEvidence(primaryCause),
              parents = List(primaryComparisonRef)
            ),
            EvidenceRecord(
              secondaryComparisonRef,
              CandidateComparisonEvidence(secondaryComparisonFact),
              parents = List(referenceLineRef, referenceEvalRef, alternativeLineRef, alternativeEvalRef)
            )
          )
        ),
        ideas = Nil,
        claims = Nil,
        ideaVerdict = None
      )

    val semantic = SemanticCoverageMetrics.from(packet)
    val primaryDiagnostic = semantic.comparisonDiagnostics.find(_.id == "comparison-played-best-covered").get
    val secondaryDiagnostic =
      semantic.comparisonDiagnostics.find(_.id == "comparison-reference-alternative-secondary").get

    assertEquals(primaryDiagnostic.causeKinds, List(RelativeCauseKind.MissedTacticalResource))
    assertEquals(primaryDiagnostic.hasUnexplainedEngineGap, false)
    assertEquals(secondaryDiagnostic.hasSecondaryContextEngineGap, true)
    assertEquals(semantic.hasPlayedUnexplainedEngineGap, false)
    assertEquals(semantic.hasContextUnexplainedEngineGap, false)
    assertEquals(semantic.hasSecondaryContextEngineGap, true)
    assertEquals(semantic.secondaryContextComparisonIds, List("comparison-reference-alternative-secondary"))
    assertEquals(semantic.secondaryContextWithPrimaryCoverageIds, List("comparison-reference-alternative-secondary"))
    assertEquals(semantic.secondaryContextWithoutPrimaryCoverageIds, Nil)
    assertEquals(semantic.contextUnexplainedWithPrimaryCoverageIds, List("comparison-reference-alternative-secondary"))
    assertEquals(semantic.contextUnexplainedWithoutPrimaryCoverageIds, Nil)

  test("semantic coverage exposes local concrete claim details"):
    val relationRef =
      evidenceRef("relation-local-concrete", EvidenceLayer.Relation, EvidenceProducer.TacticalRelationProducer, referenceLine)
    val comparison =
      EvalComparison(
        mover = Color.White,
        referenceLine = referenceLine,
        candidateLine = playedLine,
        candidateDeltaForMover = -120,
        candidateWinPercentDeltaForMover = 0.0,
        cpLossForMover = 120,
        winPercentLossForMover = 6.0,
        verdict = MoveChoiceVerdict.Inaccuracy
      )
    val localClaim =
      claim(
        id = "claim-local-concrete",
        family = ClaimFamily.Tactical,
        line = Some(referenceLine),
        move = Some(referenceLine.rootMove)
      ).copy(evidence = List(relationRef), engineComparison = Some(comparison))
    val packet =
      EvidenceBackedJudgmentPacket(
        root = root,
        positions = Nil,
        candidateLines = List(candidateLine(referenceLine, evalCp = 40, depth = 18)),
        transitions = Nil,
        relativeAssessments = Nil,
        evidenceGraph = TypedEvidenceGraph(
          List(
            EvidenceRecord(
              relationRef,
              RelationFactEvidence(
                kind = RelationFactKind.Fork,
                focusSquares = Nil,
                targetSquare = None,
                lineMoves = List(referenceLine.rootMove),
                participants = Nil
              )
            )
          )
        ),
        ideas = Nil,
        claims = List(localClaim),
        ideaVerdict = None
      )

    val semantic = SemanticCoverageMetrics.from(packet)
    val detail = semantic.localConcreteClaimDiagnostics.head

    assertEquals(semantic.localConcreteClaims, 1)
    assertEquals(detail.id, localClaim.id)
    assertEquals(detail.family, ClaimFamily.Tactical)
    assertEquals(detail.localKind, LocalConcreteClaimKind.TacticalLine)
    assert(detail.evidenceLayers.contains(EvidenceLayer.Relation))
    assertEquals(detail.engineVerdict, Some(MoveChoiceVerdict.Inaccuracy))

  private def idea(
      id: String,
      family: ChessIdeaFamily,
      line: Option[LineNodeRef],
      move: Option[String]
  ): ChessIdea =
    ChessIdea(
      ref = ChessIdeaRef(id, family),
      subject = IdeaSubject.CandidateLine,
      primaryPosition = root,
      primaryLine = line,
      moveUci = move,
      evidence = Nil,
      requiredLayers = Nil,
      scope = EvidenceScope.CandidateLine,
      confidence = EvidenceConfidence.LegalReplayVerified
    )

  private def candidateLine(
      ref: LineNodeRef,
      evalCp: Int,
      depth: Int
  ): CandidateLineNode =
    CandidateLineNode(
      role = ref.role,
      ref = ref,
      line = VariationLine(moves = List(ref.rootMove), scoreCp = evalCp, depth = depth),
      evalCp = evalCp,
      mate = None,
      depth = depth,
      evidence = evidenceRef(s"candidate-${ref.id}", EvidenceLayer.Line, EvidenceProducer.LegalLineProducer, ref)
    )

  private def evidenceRef(
      id: String,
      layer: EvidenceLayer,
      producer: EvidenceProducer,
      line: LineNodeRef
  ): EvidenceRef =
    EvidenceRef(
      id = id,
      producer = producer,
      layer = layer,
      position = root,
      line = Some(line),
      scope = EvidenceScope.CandidateLine,
      confidence = EvidenceConfidence.EngineBacked
    )

  private def claim(
      id: String,
      family: ClaimFamily,
      line: Option[LineNodeRef],
      move: Option[String]
  ): ClaimSeed =
    ClaimSeed(
      id = id,
      family = family,
      idea = None,
      subject = IdeaSubject.CandidateLine,
      primaryPosition = root,
      primaryLine = line,
      subjectMove = move,
      evidence = Nil,
      supportingFacts = Nil,
      engineComparison = None,
      scope = EvidenceScope.CandidateLine,
      confidence = EvidenceConfidence.LegalReplayVerified
    )
