package lila.chessjudgment.model.judgment

import chess.Color
import lila.chessjudgment.model.strategic.VariationLine

class MoveJudgmentViewTest extends munit.FunSuite:

  test("exposes played alternative structural improvement as context cause"):
    val root = PositionNodeRef("8/8/8/8/8/8/3P4/8 w - - 0 1", 1, Some(Color.White), Some("root"))
    val afterPlayed = PositionNodeRef("8/8/8/8/3P4/8/8/8 b - - 0 1", 2, Some(Color.Black), Some("after-played"))
    val afterReference = PositionNodeRef("8/8/8/8/8/5N2/3P4/8 b - - 1 1", 2, Some(Color.Black), Some("after-reference"))

    val playedLine = LineNodeRef("played-line", "d2d4", 2, LineNodeRole.Played)
    val referenceLine = LineNodeRef("reference-line", "g1f3", 1, LineNodeRole.BestReference)
    val alternativeLine = LineNodeRef("alternative-line", "c2c4", 3, LineNodeRole.Alternative)

    val playedLineEvidence = evidenceRef(
      id = "line:played",
      producer = EvidenceProducer.LegalLineProducer,
      layer = EvidenceLayer.Line,
      position = afterPlayed,
      line = Some(playedLine),
      scope = EvidenceScope.PlayedLine
    )
    val referenceLineEvidence = evidenceRef(
      id = "line:reference",
      producer = EvidenceProducer.LegalLineProducer,
      layer = EvidenceLayer.Line,
      position = afterReference,
      line = Some(referenceLine),
      scope = EvidenceScope.BestLine
    )
    val playedTransitionEvidence = evidenceRef(
      id = "transition:played",
      producer = EvidenceProducer.MoveTransitionProducer,
      layer = EvidenceLayer.MoveTransition,
      position = root,
      line = Some(playedLine),
      scope = EvidenceScope.PlayedTransition
    )
    val relativeAssessmentEvidence = evidenceRef(
      id = "relative-assessment:played",
      producer = EvidenceProducer.RelativeMoveProducer,
      layer = EvidenceLayer.RelativeAssessment,
      position = root,
      line = Some(playedLine),
      scope = EvidenceScope.Counterfactual
    )

    val played = MoveTransitionEdge(
      role = TransitionEdgeRole.Played,
      id = "played-transition",
      from = root,
      moveUci = "d2d4",
      to = afterPlayed,
      changedFacts = Nil,
      planTransition = None,
      evidence = playedTransitionEvidence
    )
    val reference = CandidateLineNode(
      role = LineNodeRole.BestReference,
      ref = referenceLine,
      line = VariationLine(List("g1f3"), scoreCp = 30, depth = 16),
      whitePovEvalCp = 30,
      mate = None,
      depth = 16,
      evidence = referenceLineEvidence
    )
    val candidate = CandidateLineNode(
      role = LineNodeRole.Played,
      ref = playedLine,
      line = VariationLine(List("d2d4"), scoreCp = 20, depth = 16),
      whitePovEvalCp = 20,
      mate = None,
      depth = 16,
      evidence = playedLineEvidence
    )
    val assessment = RelativeMoveAssessment(
      played = played,
      referenceTransition = None,
      reference = reference,
      candidate = candidate,
      comparison = EvalComparison(
        mover = Color.White,
        referenceLine = referenceLine,
        candidateLine = playedLine,
        rawCandidateDeltaCpForDiagnostics = -10,
        candidateWinPercentDeltaForMover = 0.0,
        rawCpLossForDiagnostics = 10,
        winPercentLossForMover = 0.4,
        verdict = MoveChoiceVerdict.PlayableLoss
      ),
      collapse = None,
      confidence = EvidenceConfidence.EngineBacked,
      evidence = relativeAssessmentEvidence,
      counterfactualEvidence = Nil
    )

    val transition = StructuralTransitionBinding(
      moveUci = "d2d4",
      role = TransitionEdgeRole.Played,
      from = root,
      to = afterPlayed,
      line = Some(playedLine),
      perspective = Color.White
    )
    val consequence = TransitionConsequence(
      kind = TransitionConsequenceKind.CenterControlGain,
      polarity = StructuralSignalPolarity.Gain,
      strength = 2,
      subjects = List("center")
    )
    val structuralRef = evidenceRef(
      id = "structural-delta:played:d2d4",
      producer = EvidenceProducer.StructuralDeltaProducer,
      layer = EvidenceLayer.StructuralDelta,
      position = root,
      line = Some(playedLine),
      scope = EvidenceScope.PlayedTransition
    )
    val structuralRecord = EvidenceRecord(
      ref = structuralRef,
      payload = StructuralDeltaEvidence(
        transition = transition,
        signals = Nil,
        consequences = List(consequence)
      )
    )
    val cause = RelativeCauseFact(
      kind = RelativeCauseKind.StructuralImprovement,
      comparisonKind = CandidateComparisonKind.PlayedVsAlternative,
      referenceLine = alternativeLine,
      candidateLine = playedLine,
      verdict = MoveChoiceVerdict.ImprovesOnReference,
      winPercentLossForMover = 0.0,
      candidateWinPercentDeltaForMover = 1.4,
      supportEvidence = List(structuralRef),
      evidenceLines = List(playedLine),
      role = RelativeCauseRole.PlayedAlternativeContext,
      eventLine = playedLine,
      sourceSide = RelativeCauseSourceSide.Candidate,
      importance = RelativeCauseImportance.Context,
      attribution = CauseAttribution(
        kind = CauseAttributionKind.CandidateCreatesValue,
        ownedEvidence = List(structuralRef),
        rootMoveMatched = true,
        directProofEligible = true
      )
    )(
      proof = Some(
        RelativeCauseProof(
          directProof = RelativeCauseProofSection(
            role = RelativeCauseProofRole.DirectProof,
            strength = RelativeCauseProofStrength.Primary,
            transitionConsequences = List(TransitionConsequenceProof(structuralRef, transition, consequence))
          )
        )
      )
    )
    val causeRef = evidenceRef(
      id = "relative-cause:played-alt:structural-improvement",
      producer = EvidenceProducer.RelativeMoveProducer,
      layer = EvidenceLayer.RelativeCause,
      position = root,
      line = Some(playedLine),
      scope = EvidenceScope.Counterfactual
    )
    val graph = TypedEvidenceGraph(
      List(
        structuralRecord,
        EvidenceRecord(causeRef, RelativeCauseFactEvidence(cause), parents = List(structuralRef))
      )
    )

    val view = MoveJudgmentView
      .from(
        relativeAssessments = List(assessment),
        evidenceGraph = graph,
        ideas = Nil,
        claims = Nil,
        claimLifecycle = Nil,
        ideaVerdict = None,
        claimSupportClusters = Nil,
        claimEventClusters = Nil
      )
      .get

    assertEquals(view.primaryCauses, Nil)
    assertEquals(view.contextCauses.map(_.causeKind), List(RelativeCauseKind.StructuralImprovement))
    assertEquals(view.contextCauses.head.causeRole, RelativeCauseRole.PlayedAlternativeContext)
    assertEquals(view.contextCauses.head.causeSourceSide, RelativeCauseSourceSide.Candidate)
    assertEquals(view.contextCauses.head.evidenceIds, List(causeRef.id, structuralRef.id).sorted)

  test("does not expose playable loss relative cause as primary bad cause"):
    val root = PositionNodeRef("8/8/8/8/8/8/3P4/8 w - - 0 1", 1, Some(Color.White), Some("root"))
    val playedLine = LineNodeRef("played-line", "d2d4", 2, LineNodeRole.Played)
    val referenceLine = LineNodeRef("reference-line", "g1f3", 1, LineNodeRole.BestReference)
    val causeRef = evidenceRef(
      id = "relative-cause:played-best:tactical",
      producer = EvidenceProducer.RelativeMoveProducer,
      layer = EvidenceLayer.RelativeCause,
      position = root,
      line = Some(playedLine),
      scope = EvidenceScope.Counterfactual
    )
    val cause = RelativeCauseFact(
      kind = RelativeCauseKind.TacticalRefutationOfPlayed,
      comparisonKind = CandidateComparisonKind.PlayedVsBest,
      referenceLine = referenceLine,
      candidateLine = playedLine,
      verdict = MoveChoiceVerdict.PlayableLoss,
      winPercentLossForMover = 2.5,
      candidateWinPercentDeltaForMover = 0.0,
      supportEvidence = List(causeRef),
      evidenceLines = List(playedLine),
      role = RelativeCauseRole.PrimaryPlayedCause,
      eventLine = playedLine,
      sourceSide = RelativeCauseSourceSide.Candidate,
      importance = RelativeCauseImportance.Primary
    )(None)
    val view = MoveJudgmentView
      .from(
        relativeAssessments = Nil,
        evidenceGraph = TypedEvidenceGraph(List(EvidenceRecord(causeRef, RelativeCauseFactEvidence(cause)))),
        ideas = Nil,
        claims = Nil,
        claimLifecycle = Nil,
        ideaVerdict = None,
        claimSupportClusters = Nil,
        claimEventClusters = Nil
      )
      .get

    assertEquals(view.primaryCauses, Nil)
    assertEquals(view.secondaryCauses.map(_.causeKind), List(RelativeCauseKind.TacticalRefutationOfPlayed))

  test("marks same-comparison tactical primary as witness when structural root cause exists"):
    val root = PositionNodeRef("8/8/8/8/8/8/3P4/8 w - - 0 1", 1, Some(Color.White), Some("root"))
    val afterPlayed = PositionNodeRef("8/8/8/8/3P4/8/8/8 b - - 0 1", 2, Some(Color.Black), Some("after-played"))
    val playedLine = LineNodeRef("played-line", "d2d4", 2, LineNodeRole.Played)
    val referenceLine = LineNodeRef("reference-line", "g1f3", 1, LineNodeRole.BestReference)
    val floatingLine = LineNodeRef("floating-line", "b1c3", 3, LineNodeRole.Alternative)
    val structuralRef = evidenceRef(
      id = "structural-delta:played:d2d4",
      producer = EvidenceProducer.StructuralDeltaProducer,
      layer = EvidenceLayer.StructuralDelta,
      position = root,
      line = Some(playedLine),
      scope = EvidenceScope.PlayedTransition
    )
    val transition = StructuralTransitionBinding(
      moveUci = "d2d4",
      role = TransitionEdgeRole.Played,
      from = root,
      to = afterPlayed,
      line = Some(playedLine),
      perspective = Color.White
    )
    val consequence = TransitionConsequence(
      kind = TransitionConsequenceKind.CenterControlGain,
      polarity = StructuralSignalPolarity.Gain,
      strength = 2
    )
    val structuralCause = RelativeCauseFact(
      kind = RelativeCauseKind.StructuralImprovement,
      comparisonKind = CandidateComparisonKind.PlayedVsBest,
      referenceLine = referenceLine,
      candidateLine = playedLine,
      verdict = MoveChoiceVerdict.Inaccuracy,
      winPercentLossForMover = 4.0,
      candidateWinPercentDeltaForMover = -4.0,
      supportEvidence = List(structuralRef),
      evidenceLines = List(playedLine),
      role = RelativeCauseRole.PrimaryPlayedCause,
      eventLine = playedLine,
      sourceSide = RelativeCauseSourceSide.Candidate,
      importance = RelativeCauseImportance.Primary,
      attribution = CauseAttribution(
        kind = CauseAttributionKind.CandidateCreatesValue,
        ownedEvidence = List(structuralRef),
        rootMoveMatched = true,
        directProofEligible = true
      )
    )(
      proof = Some(
        RelativeCauseProof(
          directProof = RelativeCauseProofSection(
            role = RelativeCauseProofRole.DirectProof,
            strength = RelativeCauseProofStrength.Primary,
            transitionConsequences = List(TransitionConsequenceProof(structuralRef, transition, consequence))
          )
        )
      )
    )
    val structuralCauseRef = evidenceRef(
      id = "relative-cause:played-best:structural",
      producer = EvidenceProducer.RelativeMoveProducer,
      layer = EvidenceLayer.RelativeCause,
      position = root,
      line = Some(playedLine),
      scope = EvidenceScope.Counterfactual
    )
    val tacticalCauseRef = evidenceRef(
      id = "relative-cause:played-best:tactical",
      producer = EvidenceProducer.RelativeMoveProducer,
      layer = EvidenceLayer.RelativeCause,
      position = root,
      line = Some(playedLine),
      scope = EvidenceScope.Counterfactual
    )
    val tacticalCause = RelativeCauseFact(
      kind = RelativeCauseKind.TacticalRefutationOfPlayed,
      comparisonKind = CandidateComparisonKind.PlayedVsBest,
      referenceLine = referenceLine,
      candidateLine = playedLine,
      verdict = MoveChoiceVerdict.Inaccuracy,
      winPercentLossForMover = 4.0,
      candidateWinPercentDeltaForMover = -4.0,
      supportEvidence = Nil,
      evidenceLines = List(playedLine),
      role = RelativeCauseRole.PrimaryPlayedCause,
      eventLine = playedLine,
      sourceSide = RelativeCauseSourceSide.Candidate,
      importance = RelativeCauseImportance.Primary,
      attribution = CauseAttribution(
        kind = CauseAttributionKind.CandidateAllowsLiability,
        rootMoveMatched = true,
        directProofEligible = true
      )
    )(None)
    val recaptureCauseRef = evidenceRef(
      id = "relative-cause:played-best:recapture",
      producer = EvidenceProducer.RelativeMoveProducer,
      layer = EvidenceLayer.RelativeCause,
      position = root,
      line = Some(referenceLine),
      scope = EvidenceScope.Counterfactual
    )
    val recaptureCause = tacticalCause.copy(
      kind = RelativeCauseKind.RecaptureRecoveryWindow,
      sourceSide = RelativeCauseSourceSide.Reference,
      eventLine = referenceLine,
      attribution = CauseAttribution(
        kind = CauseAttributionKind.ReferenceCreatesResource,
        rootMoveMatched = true,
        directProofEligible = true
      )
    )(None)
    val defensiveCauseRef = evidenceRef(
      id = "relative-cause:played-best:defensive",
      producer = EvidenceProducer.RelativeMoveProducer,
      layer = EvidenceLayer.RelativeCause,
      position = root,
      line = Some(referenceLine),
      scope = EvidenceScope.Counterfactual
    )
    val defensiveCause = tacticalCause.copy(
      kind = RelativeCauseKind.DefensiveResource,
      sourceSide = RelativeCauseSourceSide.Reference,
      eventLine = referenceLine,
      attribution = CauseAttribution(
        kind = CauseAttributionKind.ReferenceCreatesResource,
        rootMoveMatched = true,
        directProofEligible = true
      )
    )(None)
    val sameComparisonOnlyCauseRef = evidenceRef(
      id = "relative-cause:played-best:same-comparison-only",
      producer = EvidenceProducer.RelativeMoveProducer,
      layer = EvidenceLayer.RelativeCause,
      position = root,
      line = Some(floatingLine),
      scope = EvidenceScope.Counterfactual
    )
    val sameComparisonOnlyCause = tacticalCause.copy(
      kind = RelativeCauseKind.MaterialSwing,
      evidenceLines = List(floatingLine),
      eventLine = floatingLine
    )(None)
    val graph = TypedEvidenceGraph(
      List(
        EvidenceRecord(
          structuralRef,
          StructuralDeltaEvidence(transition = transition, signals = Nil, consequences = List(consequence))
        ),
        EvidenceRecord(structuralCauseRef, RelativeCauseFactEvidence(structuralCause), parents = List(structuralRef)),
        EvidenceRecord(tacticalCauseRef, RelativeCauseFactEvidence(tacticalCause)),
        EvidenceRecord(recaptureCauseRef, RelativeCauseFactEvidence(recaptureCause)),
        EvidenceRecord(defensiveCauseRef, RelativeCauseFactEvidence(defensiveCause)),
        EvidenceRecord(sameComparisonOnlyCauseRef, RelativeCauseFactEvidence(sameComparisonOnlyCause))
      )
    )
    val view = MoveJudgmentView
      .from(
        relativeAssessments = Nil,
        evidenceGraph = graph,
        ideas = Nil,
        claims = Nil,
        claimLifecycle = Nil,
        ideaVerdict = None,
        claimSupportClusters = Nil,
        claimEventClusters = Nil
      )
      .get

    val primaryByKind = view.primaryCauses.map(frame => frame.causeKind -> frame).toMap
    val structuralRoot = primaryByKind(RelativeCauseKind.StructuralImprovement)
    assertEquals(structuralRoot.narrativeRole, MoveJudgmentCauseNarrativeRole.RootCause)
    assertEquals(structuralRoot.tacticalWitnessCauseKinds, Nil)
    assertEquals(structuralRoot.punishmentWitnessCauseKinds, Nil)
    assertEquals(
      structuralRoot.contextualTacticalWitnessCauseKinds.toSet,
      Set(
        RelativeCauseKind.TacticalRefutationOfPlayed
      )
    )
    assertEquals(
      primaryByKind(RelativeCauseKind.TacticalRefutationOfPlayed).narrativeRole,
      MoveJudgmentCauseNarrativeRole.TacticalWitness
    )
    assertEquals(
      primaryByKind(RelativeCauseKind.TacticalRefutationOfPlayed).witnessBindingLevel,
      MoveJudgmentCauseWitnessBindingLevel.LineContext
    )
    assert(
      !primaryByKind(RelativeCauseKind.TacticalRefutationOfPlayed).witnessBindingSignals.contains(
        MoveJudgmentCauseWitnessBindingSignal.SharedDirectConsequence
      )
    )
    assertEquals(
      primaryByKind(RelativeCauseKind.RecaptureRecoveryWindow).narrativeRole,
      MoveJudgmentCauseNarrativeRole.ContextCause
    )
    assertEquals(
      primaryByKind(RelativeCauseKind.DefensiveResource).narrativeRole,
      MoveJudgmentCauseNarrativeRole.ContextCause
    )
    assertEquals(
      primaryByKind(RelativeCauseKind.MaterialSwing).narrativeRole,
      MoveJudgmentCauseNarrativeRole.ContextCause
    )
    assertEquals(
      primaryByKind(RelativeCauseKind.MaterialSwing).witnessBindingLevel,
      MoveJudgmentCauseWitnessBindingLevel.SameComparisonOnly
    )
    assertEquals(
      primaryByKind(RelativeCauseKind.MaterialSwing).witnessBindingSignals,
      List(MoveJudgmentCauseWitnessBindingSignal.SameComparison)
    )

  test("classifies tactical witness as punishment only when object consequence and event line bind to structural root"):
    val root = PositionNodeRef("8/8/8/8/8/8/3P4/8 w - - 0 1", 1, Some(Color.White), Some("root"))
    val afterPlayed = PositionNodeRef("8/8/8/8/3P4/8/8/8 b - - 0 1", 2, Some(Color.Black), Some("after-played"))
    val playedLine = LineNodeRef("played-line", "d2d4", 2, LineNodeRole.Played)
    val referenceLine = LineNodeRef("reference-line", "g1f3", 1, LineNodeRole.BestReference)
    val structuralRef = evidenceRef(
      id = "structural-delta:played:d2d4:punishment",
      producer = EvidenceProducer.StructuralDeltaProducer,
      layer = EvidenceLayer.StructuralDelta,
      position = root,
      line = Some(playedLine),
      scope = EvidenceScope.PlayedTransition
    )
    val transition = StructuralTransitionBinding(
      moveUci = "d2d4",
      role = TransitionEdgeRole.Played,
      from = root,
      to = afterPlayed,
      line = Some(playedLine),
      perspective = Color.White
    )
    val consequence = TransitionConsequence(
      kind = TransitionConsequenceKind.CenterControlGain,
      polarity = StructuralSignalPolarity.Gain,
      strength = 2,
      subjects = List("d4")
    )
    val proof = RelativeCauseProof(
      directProof = RelativeCauseProofSection(
        role = RelativeCauseProofRole.DirectProof,
        strength = RelativeCauseProofStrength.Primary,
        transitionConsequences = List(TransitionConsequenceProof(structuralRef, transition, consequence))
      )
    )
    val structuralCause = RelativeCauseFact(
      kind = RelativeCauseKind.StructuralImprovement,
      comparisonKind = CandidateComparisonKind.PlayedVsBest,
      referenceLine = referenceLine,
      candidateLine = playedLine,
      verdict = MoveChoiceVerdict.Blunder,
      winPercentLossForMover = 12.0,
      candidateWinPercentDeltaForMover = -12.0,
      supportEvidence = List(structuralRef),
      evidenceLines = List(playedLine),
      role = RelativeCauseRole.PrimaryPlayedCause,
      eventLine = playedLine,
      sourceSide = RelativeCauseSourceSide.Candidate,
      importance = RelativeCauseImportance.Primary,
      attribution = CauseAttribution(
        kind = CauseAttributionKind.CandidateCreatesValue,
        ownedEvidence = List(structuralRef),
        rootMoveMatched = true,
        directProofEligible = true
      )
    )(Some(proof))
    val tacticalCause = structuralCause.copy(
      kind = RelativeCauseKind.TacticalRefutationOfPlayed,
      supportEvidence = Nil,
      attribution = CauseAttribution(
        kind = CauseAttributionKind.CandidateAllowsLiability,
        rootMoveMatched = true,
        directProofEligible = true
      )
    )(Some(proof))
    val graph = TypedEvidenceGraph(
      List(
        EvidenceRecord(structuralRef, StructuralDeltaEvidence(transition, signals = Nil, consequences = List(consequence))),
        EvidenceRecord(
          evidenceRef(
            id = "relative-cause:played-best:structural-punishment-root",
            producer = EvidenceProducer.RelativeMoveProducer,
            layer = EvidenceLayer.RelativeCause,
            position = root,
            line = Some(playedLine),
            scope = EvidenceScope.Counterfactual
          ),
          RelativeCauseFactEvidence(structuralCause),
          parents = List(structuralRef)
        ),
        EvidenceRecord(
          evidenceRef(
            id = "relative-cause:played-best:tactical-punishment-witness",
            producer = EvidenceProducer.RelativeMoveProducer,
            layer = EvidenceLayer.RelativeCause,
            position = root,
            line = Some(playedLine),
            scope = EvidenceScope.Counterfactual
          ),
          RelativeCauseFactEvidence(tacticalCause),
          parents = List(structuralRef)
        )
      )
    )

    val view = MoveJudgmentView
      .from(
        relativeAssessments = Nil,
        evidenceGraph = graph,
        ideas = Nil,
        claims = Nil,
        claimLifecycle = Nil,
        ideaVerdict = None,
        claimSupportClusters = Nil,
        claimEventClusters = Nil
      )
      .get

    val tacticalFrame = view.primaryCauses.find(_.causeKind == RelativeCauseKind.TacticalRefutationOfPlayed).get
    val structuralFrame = view.primaryCauses.find(_.causeKind == RelativeCauseKind.StructuralImprovement).get
    assertEquals(structuralFrame.tacticalWitnessCauseKinds, List(RelativeCauseKind.TacticalRefutationOfPlayed))
    assertEquals(structuralFrame.punishmentWitnessCauseKinds, List(RelativeCauseKind.TacticalRefutationOfPlayed))
    assertEquals(structuralFrame.contextualTacticalWitnessCauseKinds, Nil)
    assertEquals(tacticalFrame.narrativeRole, MoveJudgmentCauseNarrativeRole.TacticalWitness)
    assertEquals(tacticalFrame.witnessBindingLevel, MoveJudgmentCauseWitnessBindingLevel.Punishment)
    assert(tacticalFrame.witnessBindingSignals.contains(MoveJudgmentCauseWitnessBindingSignal.SameEventLine))
    assert(tacticalFrame.witnessBindingSignals.contains(MoveJudgmentCauseWitnessBindingSignal.SharedDirectConsequence))

  test("demotes plan contradiction when a concrete structural root exists in the same played-vs-best loss"):
    val root = PositionNodeRef("8/8/8/8/8/8/3P4/8 w - - 0 1", 1, Some(Color.White), Some("root"))
    val afterPlayed = PositionNodeRef("8/8/8/8/3P4/8/8/8 b - - 0 1", 2, Some(Color.Black), Some("after-played"))
    val playedLine = LineNodeRef("played-line", "d2d4", 2, LineNodeRole.Played)
    val referenceLine = LineNodeRef("reference-line", "g1f3", 1, LineNodeRole.BestReference)
    val structuralRef = evidenceRef(
      id = "structural-delta:played:d2d4:activity-loss",
      producer = EvidenceProducer.StructuralDeltaProducer,
      layer = EvidenceLayer.StructuralDelta,
      position = root,
      line = Some(playedLine),
      scope = EvidenceScope.PlayedTransition
    )
    val transition = StructuralTransitionBinding(
      moveUci = "d2d4",
      role = TransitionEdgeRole.Played,
      from = root,
      to = afterPlayed,
      line = Some(playedLine),
      perspective = Color.White
    )
    val activityConsequence = TransitionConsequence(
      kind = TransitionConsequenceKind.MobilityLoss,
      polarity = StructuralSignalPolarity.Loss,
      strength = 3,
      subjects = List("d4")
    )
    val planConsequence = TransitionConsequence(
      kind = TransitionConsequenceKind.DevelopmentUnsafePlacement,
      polarity = StructuralSignalPolarity.Loss,
      strength = 1
    )
    val proof = RelativeCauseProof(
      directProof = RelativeCauseProofSection(
        role = RelativeCauseProofRole.DirectProof,
        strength = RelativeCauseProofStrength.Primary,
        transitionConsequences = List(TransitionConsequenceProof(structuralRef, transition, activityConsequence))
      )
    )
    val concreteActivityCause = RelativeCauseFact(
      kind = RelativeCauseKind.ActivityLoss,
      comparisonKind = CandidateComparisonKind.PlayedVsBest,
      referenceLine = referenceLine,
      candidateLine = playedLine,
      verdict = MoveChoiceVerdict.Mistake,
      winPercentLossForMover = 9.0,
      candidateWinPercentDeltaForMover = -9.0,
      supportEvidence = List(structuralRef),
      evidenceLines = List(playedLine),
      role = RelativeCauseRole.PrimaryPlayedCause,
      eventLine = playedLine,
      sourceSide = RelativeCauseSourceSide.Candidate,
      importance = RelativeCauseImportance.Primary,
      attribution = CauseAttribution(
        kind = CauseAttributionKind.CandidateAllowsLiability,
        ownedEvidence = List(structuralRef),
        rootMoveMatched = true,
        directProofEligible = true
      )
    )(Some(proof))
    val planCause = concreteActivityCause.copy(
      kind = RelativeCauseKind.PlanContradiction,
      attribution = CauseAttribution(
        kind = CauseAttributionKind.CandidateAllowsLiability,
        ownedEvidence = List(structuralRef),
        rootMoveMatched = true,
        directProofEligible = true
      )
    )(
      Some(
        RelativeCauseProof(
          directProof = RelativeCauseProofSection(
            role = RelativeCauseProofRole.DirectProof,
            strength = RelativeCauseProofStrength.Primary,
            transitionConsequences = List(TransitionConsequenceProof(structuralRef, transition, planConsequence))
          )
        )
      )
    )
    val graph = TypedEvidenceGraph(
      List(
        EvidenceRecord(
          structuralRef,
          StructuralDeltaEvidence(transition = transition, signals = Nil, consequences = List(activityConsequence, planConsequence))
        ),
        EvidenceRecord(
          evidenceRef(
            id = "relative-cause:played-best:activity-loss-root",
            producer = EvidenceProducer.RelativeMoveProducer,
            layer = EvidenceLayer.RelativeCause,
            position = root,
            line = Some(playedLine),
            scope = EvidenceScope.Counterfactual
          ),
          RelativeCauseFactEvidence(concreteActivityCause),
          parents = List(structuralRef)
        ),
        EvidenceRecord(
          evidenceRef(
            id = "relative-cause:played-best:plan-fallback",
            producer = EvidenceProducer.RelativeMoveProducer,
            layer = EvidenceLayer.RelativeCause,
            position = root,
            line = Some(playedLine),
            scope = EvidenceScope.Counterfactual
          ),
          RelativeCauseFactEvidence(planCause),
          parents = List(structuralRef)
        )
      )
    )

    val view = MoveJudgmentView
      .from(
        relativeAssessments = Nil,
        evidenceGraph = graph,
        ideas = Nil,
        claims = Nil,
        claimLifecycle = Nil,
        ideaVerdict = None,
        claimSupportClusters = Nil,
        claimEventClusters = Nil
      )
      .get

    val activityFrame = view.primaryCauses.find(_.causeKind == RelativeCauseKind.ActivityLoss).get
    val planFrame = view.primaryCauses.find(_.causeKind == RelativeCauseKind.PlanContradiction).get
    assertEquals(activityFrame.narrativeRole, MoveJudgmentCauseNarrativeRole.RootCause)
    assertEquals(planFrame.narrativeRole, MoveJudgmentCauseNarrativeRole.SupportingCause)

  test("keeps tactical root above broad structural root that lacks typed object readiness"):
    val root = PositionNodeRef("8/8/8/8/8/8/3P4/8 w - - 0 1", 1, Some(Color.White), Some("root"))
    val afterPlayed = PositionNodeRef("8/8/8/8/3P4/8/8/8 b - - 0 1", 2, Some(Color.Black), Some("after-played"))
    val playedLine = LineNodeRef("played-line", "d2d4", 2, LineNodeRole.Played)
    val referenceLine = LineNodeRef("reference-line", "g1f3", 1, LineNodeRole.BestReference)
    val structuralRef = evidenceRef(
      id = "structural-delta:played:d2d4:broad-activity",
      producer = EvidenceProducer.StructuralDeltaProducer,
      layer = EvidenceLayer.StructuralDelta,
      position = root,
      line = Some(playedLine),
      scope = EvidenceScope.PlayedTransition
    )
    val transition = StructuralTransitionBinding(
      moveUci = "d2d4",
      role = TransitionEdgeRole.Played,
      from = root,
      to = afterPlayed,
      line = Some(playedLine),
      perspective = Color.White
    )
    val broadConsequence = TransitionConsequence(
      kind = TransitionConsequenceKind.MobilityLoss,
      polarity = StructuralSignalPolarity.Loss,
      strength = 2
    )
    val broadProof = RelativeCauseProof(
      directProof = RelativeCauseProofSection(
        role = RelativeCauseProofRole.DirectProof,
        strength = RelativeCauseProofStrength.Primary,
        transitionConsequences = List(TransitionConsequenceProof(structuralRef, transition, broadConsequence))
      )
    )
    val broadStructuralCause = RelativeCauseFact(
      kind = RelativeCauseKind.ActivityLoss,
      comparisonKind = CandidateComparisonKind.PlayedVsBest,
      referenceLine = referenceLine,
      candidateLine = playedLine,
      verdict = MoveChoiceVerdict.Blunder,
      winPercentLossForMover = 14.0,
      candidateWinPercentDeltaForMover = -14.0,
      supportEvidence = List(structuralRef),
      evidenceLines = List(playedLine),
      role = RelativeCauseRole.PrimaryPlayedCause,
      eventLine = playedLine,
      sourceSide = RelativeCauseSourceSide.Candidate,
      importance = RelativeCauseImportance.Primary,
      attribution = CauseAttribution(
        kind = CauseAttributionKind.CandidateAllowsLiability,
        ownedEvidence = List(structuralRef),
        rootMoveMatched = true,
        directProofEligible = true
      )
    )(Some(broadProof))
    val tacticalCause = broadStructuralCause.copy(
      kind = RelativeCauseKind.TacticalRefutationOfPlayed,
      supportEvidence = Nil,
      attribution = CauseAttribution(
        kind = CauseAttributionKind.CandidateAllowsLiability,
        rootMoveMatched = true,
        directProofEligible = true
      )
    )(None)
    val graph = TypedEvidenceGraph(
      List(
        EvidenceRecord(
          structuralRef,
          StructuralDeltaEvidence(transition = transition, signals = Nil, consequences = List(broadConsequence))
        ),
        EvidenceRecord(
          evidenceRef(
            id = "relative-cause:played-best:broad-activity-root",
            producer = EvidenceProducer.RelativeMoveProducer,
            layer = EvidenceLayer.RelativeCause,
            position = root,
            line = Some(playedLine),
            scope = EvidenceScope.Counterfactual
          ),
          RelativeCauseFactEvidence(broadStructuralCause),
          parents = List(structuralRef)
        ),
        EvidenceRecord(
          evidenceRef(
            id = "relative-cause:played-best:concrete-tactical-root",
            producer = EvidenceProducer.RelativeMoveProducer,
            layer = EvidenceLayer.RelativeCause,
            position = root,
            line = Some(playedLine),
            scope = EvidenceScope.Counterfactual
          ),
          RelativeCauseFactEvidence(tacticalCause)
        )
      )
    )

    val view = MoveJudgmentView
      .from(
        relativeAssessments = Nil,
        evidenceGraph = graph,
        ideas = Nil,
        claims = Nil,
        claimLifecycle = Nil,
        ideaVerdict = None,
        claimSupportClusters = Nil,
        claimEventClusters = Nil
      )
      .get

    val activityFrame = view.primaryCauses.find(_.causeKind == RelativeCauseKind.ActivityLoss).get
    val tacticalFrame = view.primaryCauses.find(_.causeKind == RelativeCauseKind.TacticalRefutationOfPlayed).get
    assertEquals(activityFrame.concreteObjectReady, false)
    assertEquals(activityFrame.narrativeRole, MoveJudgmentCauseNarrativeRole.SupportingCause)
    assertEquals(tacticalFrame.narrativeRole, MoveJudgmentCauseNarrativeRole.RootCause)

  test("keeps unbound recapture label below tactical event root when plan fallback is suppressed"):
    val root = PositionNodeRef("8/8/8/8/8/8/3P4/8 w - - 0 1", 1, Some(Color.White), Some("root"))
    val afterPlayed = PositionNodeRef("8/8/8/8/3P4/8/8/8 b - - 0 1", 2, Some(Color.Black), Some("after-played"))
    val playedLine = LineNodeRef("played-line", "d2d4", 2, LineNodeRole.Played)
    val referenceLine = LineNodeRef("reference-line", "g1f3", 1, LineNodeRole.BestReference)
    val structuralRef = evidenceRef(
      id = "structural-delta:played:d2d4:plan-fallback-with-events",
      producer = EvidenceProducer.StructuralDeltaProducer,
      layer = EvidenceLayer.StructuralDelta,
      position = root,
      line = Some(playedLine),
      scope = EvidenceScope.PlayedTransition
    )
    val transition = StructuralTransitionBinding(
      moveUci = "d2d4",
      role = TransitionEdgeRole.Played,
      from = root,
      to = afterPlayed,
      line = Some(playedLine),
      perspective = Color.White
    )
    val planConsequence = TransitionConsequence(
      kind = TransitionConsequenceKind.DevelopmentUnsafePlacement,
      polarity = StructuralSignalPolarity.Loss,
      strength = 1
    )
    val baseCause = RelativeCauseFact(
      kind = RelativeCauseKind.TacticalRefutationOfPlayed,
      comparisonKind = CandidateComparisonKind.PlayedVsBest,
      referenceLine = referenceLine,
      candidateLine = playedLine,
      verdict = MoveChoiceVerdict.Blunder,
      winPercentLossForMover = 12.0,
      candidateWinPercentDeltaForMover = -12.0,
      supportEvidence = Nil,
      evidenceLines = List(playedLine),
      role = RelativeCauseRole.PrimaryPlayedCause,
      eventLine = playedLine,
      sourceSide = RelativeCauseSourceSide.Candidate,
      importance = RelativeCauseImportance.Primary,
      attribution = CauseAttribution(
        kind = CauseAttributionKind.CandidateAllowsLiability,
        rootMoveMatched = true,
        directProofEligible = true
      )
    )(None)
    val planCause = baseCause.copy(
      kind = RelativeCauseKind.PlanContradiction,
      supportEvidence = List(structuralRef),
      attribution = CauseAttribution(
        kind = CauseAttributionKind.CandidateAllowsLiability,
        ownedEvidence = List(structuralRef),
        rootMoveMatched = true,
        directProofEligible = true
      )
    )(
      Some(
        RelativeCauseProof(
          directProof = RelativeCauseProofSection(
            role = RelativeCauseProofRole.DirectProof,
            strength = RelativeCauseProofStrength.Primary,
            transitionConsequences = List(TransitionConsequenceProof(structuralRef, transition, planConsequence))
          )
        )
      )
    )
    val tacticalCause = baseCause
    val wrongRecapturerCause = baseCause.copy(kind = RelativeCauseKind.WrongRecapturer)(None)
    val onlyDefenseCause = baseCause.copy(
      kind = RelativeCauseKind.OnlyDefenseNecessity,
      sourceSide = RelativeCauseSourceSide.Reference,
      eventLine = referenceLine,
      attribution = CauseAttribution(
        kind = CauseAttributionKind.ReferenceCreatesResource,
        rootMoveMatched = true,
        directProofEligible = true
      )
    )(None)
    val graph = TypedEvidenceGraph(
      List(
        EvidenceRecord(
          structuralRef,
          StructuralDeltaEvidence(transition = transition, signals = Nil, consequences = List(planConsequence))
        ),
        EvidenceRecord(
          evidenceRef(
            id = "relative-cause:played-best:event:tactical-refutation",
            producer = EvidenceProducer.RelativeMoveProducer,
            layer = EvidenceLayer.RelativeCause,
            position = root,
            line = Some(playedLine),
            scope = EvidenceScope.Counterfactual
          ),
          RelativeCauseFactEvidence(tacticalCause)
        ),
        EvidenceRecord(
          evidenceRef(
            id = "relative-cause:played-best:event:wrong-recapturer",
            producer = EvidenceProducer.RelativeMoveProducer,
            layer = EvidenceLayer.RelativeCause,
            position = root,
            line = Some(playedLine),
            scope = EvidenceScope.Counterfactual
          ),
          RelativeCauseFactEvidence(wrongRecapturerCause)
        ),
        EvidenceRecord(
          evidenceRef(
            id = "relative-cause:played-best:event:only-defense",
            producer = EvidenceProducer.RelativeMoveProducer,
            layer = EvidenceLayer.RelativeCause,
            position = root,
            line = Some(referenceLine),
            scope = EvidenceScope.Counterfactual
          ),
          RelativeCauseFactEvidence(onlyDefenseCause)
        ),
        EvidenceRecord(
          evidenceRef(
            id = "relative-cause:played-best:plan-fallback-with-events",
            producer = EvidenceProducer.RelativeMoveProducer,
            layer = EvidenceLayer.RelativeCause,
            position = root,
            line = Some(playedLine),
            scope = EvidenceScope.Counterfactual
          ),
          RelativeCauseFactEvidence(planCause)
        )
      )
    )

    val view = MoveJudgmentView
      .from(
        relativeAssessments = Nil,
        evidenceGraph = graph,
        ideas = Nil,
        claims = Nil,
        claimLifecycle = Nil,
        ideaVerdict = None,
        claimSupportClusters = Nil,
        claimEventClusters = Nil
      )
      .get

    val rolesByKind = view.primaryCauses.map(frame => frame.causeKind -> frame.narrativeRole).toMap
    assertEquals(rolesByKind(RelativeCauseKind.TacticalRefutationOfPlayed), MoveJudgmentCauseNarrativeRole.RootCause)
    assertEquals(rolesByKind(RelativeCauseKind.WrongRecapturer), MoveJudgmentCauseNarrativeRole.SupportingCause)
    assertEquals(rolesByKind(RelativeCauseKind.OnlyDefenseNecessity), MoveJudgmentCauseNarrativeRole.SupportingCause)
    assertEquals(rolesByKind(RelativeCauseKind.PlanContradiction), MoveJudgmentCauseNarrativeRole.SupportingCause)

  test("does not promote unbound wrong recapturer over plan fallback"):
    val root = PositionNodeRef("8/8/8/8/8/8/3P4/8 w - - 0 1", 1, Some(Color.White), Some("root"))
    val afterPlayed = PositionNodeRef("8/8/8/8/3P4/8/8/8 b - - 0 1", 2, Some(Color.Black), Some("after-played"))
    val playedLine = LineNodeRef("played-line", "d2d4", 2, LineNodeRole.Played)
    val referenceLine = LineNodeRef("reference-line", "g1f3", 1, LineNodeRole.BestReference)
    val structuralRef = evidenceRef(
      id = "structural-delta:played:d2d4:plan-vs-unbound-recapture",
      producer = EvidenceProducer.StructuralDeltaProducer,
      layer = EvidenceLayer.StructuralDelta,
      position = root,
      line = Some(playedLine),
      scope = EvidenceScope.PlayedTransition
    )
    val transition = StructuralTransitionBinding(
      moveUci = "d2d4",
      role = TransitionEdgeRole.Played,
      from = root,
      to = afterPlayed,
      line = Some(playedLine),
      perspective = Color.White
    )
    val planConsequence = TransitionConsequence(
      kind = TransitionConsequenceKind.DevelopmentUnsafePlacement,
      polarity = StructuralSignalPolarity.Loss,
      strength = 1
    )
    val planCause = RelativeCauseFact(
      kind = RelativeCauseKind.PlanContradiction,
      comparisonKind = CandidateComparisonKind.PlayedVsBest,
      referenceLine = referenceLine,
      candidateLine = playedLine,
      verdict = MoveChoiceVerdict.Mistake,
      winPercentLossForMover = 8.0,
      candidateWinPercentDeltaForMover = -8.0,
      supportEvidence = List(structuralRef),
      evidenceLines = List(playedLine),
      role = RelativeCauseRole.PrimaryPlayedCause,
      eventLine = playedLine,
      sourceSide = RelativeCauseSourceSide.Candidate,
      importance = RelativeCauseImportance.Primary,
      attribution = CauseAttribution(
        kind = CauseAttributionKind.CandidateAllowsLiability,
        ownedEvidence = List(structuralRef),
        rootMoveMatched = true,
        directProofEligible = true
      )
    )(
      Some(
        RelativeCauseProof(
          directProof = RelativeCauseProofSection(
            role = RelativeCauseProofRole.DirectProof,
            strength = RelativeCauseProofStrength.Primary,
            transitionConsequences = List(TransitionConsequenceProof(structuralRef, transition, planConsequence))
          )
        )
      )
    )
    val wrongRecapturerCause = planCause.copy(
      kind = RelativeCauseKind.WrongRecapturer,
      supportEvidence = Nil,
      attribution = CauseAttribution(
        kind = CauseAttributionKind.CandidateAllowsLiability,
        rootMoveMatched = true,
        directProofEligible = true
      )
    )(None)
    val graph = TypedEvidenceGraph(
      List(
        EvidenceRecord(
          structuralRef,
          StructuralDeltaEvidence(transition = transition, signals = Nil, consequences = List(planConsequence))
        ),
        EvidenceRecord(
          evidenceRef(
            id = "relative-cause:played-best:plan-fallback-vs-unbound-recapture",
            producer = EvidenceProducer.RelativeMoveProducer,
            layer = EvidenceLayer.RelativeCause,
            position = root,
            line = Some(playedLine),
            scope = EvidenceScope.Counterfactual
          ),
          RelativeCauseFactEvidence(planCause),
          parents = List(structuralRef)
        ),
        EvidenceRecord(
          evidenceRef(
            id = "relative-cause:played-best:unbound-wrong-recapturer",
            producer = EvidenceProducer.RelativeMoveProducer,
            layer = EvidenceLayer.RelativeCause,
            position = root,
            line = Some(playedLine),
            scope = EvidenceScope.Counterfactual
          ),
          RelativeCauseFactEvidence(wrongRecapturerCause)
        )
      )
    )

    val view = MoveJudgmentView
      .from(
        relativeAssessments = Nil,
        evidenceGraph = graph,
        ideas = Nil,
        claims = Nil,
        claimLifecycle = Nil,
        ideaVerdict = None,
        claimSupportClusters = Nil,
        claimEventClusters = Nil
      )
      .get

    val rolesByKind = view.primaryCauses.map(frame => frame.causeKind -> frame.narrativeRole).toMap
    assertEquals(rolesByKind(RelativeCauseKind.PlanContradiction), MoveJudgmentCauseNarrativeRole.RootCause)
    assertEquals(rolesByKind(RelativeCauseKind.WrongRecapturer), MoveJudgmentCauseNarrativeRole.SupportingCause)

  test("selects one fallback root when no concrete structural or event root exists"):
    val root = PositionNodeRef("8/8/8/8/8/8/3P4/8 w - - 0 1", 1, Some(Color.White), Some("root"))
    val afterPlayed = PositionNodeRef("8/8/8/8/3P4/8/8/8 b - - 0 1", 2, Some(Color.Black), Some("after-played"))
    val playedLine = LineNodeRef("played-line", "d2d4", 2, LineNodeRole.Played)
    val referenceLine = LineNodeRef("reference-line", "g1f3", 1, LineNodeRole.BestReference)
    val structuralRef = evidenceRef(
      id = "structural-delta:played:d2d4:dual-fallback",
      producer = EvidenceProducer.StructuralDeltaProducer,
      layer = EvidenceLayer.StructuralDelta,
      position = root,
      line = Some(playedLine),
      scope = EvidenceScope.PlayedTransition
    )
    val transition = StructuralTransitionBinding(
      moveUci = "d2d4",
      role = TransitionEdgeRole.Played,
      from = root,
      to = afterPlayed,
      line = Some(playedLine),
      perspective = Color.White
    )
    val consequence = TransitionConsequence(
      kind = TransitionConsequenceKind.DevelopmentUnsafePlacement,
      polarity = StructuralSignalPolarity.Loss,
      strength = 1
    )
    val baseFallbackCause = RelativeCauseFact(
      kind = RelativeCauseKind.PlanContradiction,
      comparisonKind = CandidateComparisonKind.PlayedVsBest,
      referenceLine = referenceLine,
      candidateLine = playedLine,
      verdict = MoveChoiceVerdict.Mistake,
      winPercentLossForMover = 8.0,
      candidateWinPercentDeltaForMover = -8.0,
      supportEvidence = List(structuralRef),
      evidenceLines = List(playedLine),
      role = RelativeCauseRole.PrimaryPlayedCause,
      eventLine = playedLine,
      sourceSide = RelativeCauseSourceSide.Candidate,
      importance = RelativeCauseImportance.Primary,
      attribution = CauseAttribution(
        kind = CauseAttributionKind.CandidateAllowsLiability,
        ownedEvidence = List(structuralRef),
        rootMoveMatched = true,
        directProofEligible = true
      )
    )(
      Some(
        RelativeCauseProof(
          directProof = RelativeCauseProofSection(
            role = RelativeCauseProofRole.DirectProof,
            strength = RelativeCauseProofStrength.Primary,
            transitionConsequences = List(TransitionConsequenceProof(structuralRef, transition, consequence))
          )
        )
      )
    )
    val planImprovementCause = baseFallbackCause.copy(kind = RelativeCauseKind.PlanImprovement)(baseFallbackCause.proof)
    val graph = TypedEvidenceGraph(
      List(
        EvidenceRecord(
          structuralRef,
          StructuralDeltaEvidence(transition = transition, signals = Nil, consequences = List(consequence))
        ),
        EvidenceRecord(
          evidenceRef(
            id = "relative-cause:played-best:plan-contradiction-only-fallback",
            producer = EvidenceProducer.RelativeMoveProducer,
            layer = EvidenceLayer.RelativeCause,
            position = root,
            line = Some(playedLine),
            scope = EvidenceScope.Counterfactual
          ),
          RelativeCauseFactEvidence(baseFallbackCause),
          parents = List(structuralRef)
        ),
        EvidenceRecord(
          evidenceRef(
            id = "relative-cause:played-best:plan-improvement-only-fallback",
            producer = EvidenceProducer.RelativeMoveProducer,
            layer = EvidenceLayer.RelativeCause,
            position = root,
            line = Some(playedLine),
            scope = EvidenceScope.Counterfactual
          ),
          RelativeCauseFactEvidence(planImprovementCause),
          parents = List(structuralRef)
        )
      )
    )

    val view = MoveJudgmentView
      .from(
        relativeAssessments = Nil,
        evidenceGraph = graph,
        ideas = Nil,
        claims = Nil,
        claimLifecycle = Nil,
        ideaVerdict = None,
        claimSupportClusters = Nil,
        claimEventClusters = Nil
      )
      .get

    val roots = view.primaryCauses.filter(_.narrativeRole == MoveJudgmentCauseNarrativeRole.RootCause)
    val rolesByKind = view.primaryCauses.map(frame => frame.causeKind -> frame.narrativeRole).toMap
    assertEquals(roots.map(_.causeKind), List(RelativeCauseKind.PlanContradiction))
    assertEquals(rolesByKind(RelativeCauseKind.PlanImprovement), MoveJudgmentCauseNarrativeRole.SupportingCause)

  test("keeps pure tactical played blunder as root when no proved structural root exists"):
    val root = PositionNodeRef("8/8/8/8/8/8/3P4/8 w - - 0 1", 1, Some(Color.White), Some("root"))
    val playedLine = LineNodeRef("played-line", "d2d4", 2, LineNodeRole.Played)
    val referenceLine = LineNodeRef("reference-line", "g1f3", 1, LineNodeRole.BestReference)
    val tacticalCauseRef = evidenceRef(
      id = "relative-cause:played-best:pure-tactical",
      producer = EvidenceProducer.RelativeMoveProducer,
      layer = EvidenceLayer.RelativeCause,
      position = root,
      line = Some(playedLine),
      scope = EvidenceScope.Counterfactual
    )
    val tacticalCause = RelativeCauseFact(
      kind = RelativeCauseKind.TacticalRefutationOfPlayed,
      comparisonKind = CandidateComparisonKind.PlayedVsBest,
      referenceLine = referenceLine,
      candidateLine = playedLine,
      verdict = MoveChoiceVerdict.Blunder,
      winPercentLossForMover = 12.0,
      candidateWinPercentDeltaForMover = -12.0,
      supportEvidence = Nil,
      evidenceLines = List(playedLine),
      role = RelativeCauseRole.PrimaryPlayedCause,
      eventLine = playedLine,
      sourceSide = RelativeCauseSourceSide.Candidate,
      importance = RelativeCauseImportance.Primary,
      attribution = CauseAttribution(
        kind = CauseAttributionKind.CandidateAllowsLiability,
        rootMoveMatched = true,
        directProofEligible = true
      )
    )(None)

    val view = MoveJudgmentView
      .from(
        relativeAssessments = Nil,
        evidenceGraph = TypedEvidenceGraph(List(EvidenceRecord(tacticalCauseRef, RelativeCauseFactEvidence(tacticalCause)))),
        ideas = Nil,
        claims = Nil,
        claimLifecycle = Nil,
        ideaVerdict = None,
        claimSupportClusters = Nil,
        claimEventClusters = Nil
      )
      .get

    assertEquals(view.primaryCauses.map(_.causeKind), List(RelativeCauseKind.TacticalRefutationOfPlayed))
    assertEquals(view.primaryCauses.head.narrativeRole, MoveJudgmentCauseNarrativeRole.RootCause)

  test("does not demote tactical root when structural cause lacks owned admissible proof"):
    val root = PositionNodeRef("8/8/8/8/8/8/3P4/8 w - - 0 1", 1, Some(Color.White), Some("root"))
    val afterPlayed = PositionNodeRef("8/8/8/8/3P4/8/8/8 b - - 0 1", 2, Some(Color.Black), Some("after-played"))
    val playedLine = LineNodeRef("played-line", "d2d4", 2, LineNodeRole.Played)
    val referenceLine = LineNodeRef("reference-line", "g1f3", 1, LineNodeRole.BestReference)
    val structuralRef = evidenceRef(
      id = "structural-delta:played:d2d4:weak",
      producer = EvidenceProducer.StructuralDeltaProducer,
      layer = EvidenceLayer.StructuralDelta,
      position = root,
      line = Some(playedLine),
      scope = EvidenceScope.PlayedTransition
    )
    val transition = StructuralTransitionBinding(
      moveUci = "d2d4",
      role = TransitionEdgeRole.Played,
      from = root,
      to = afterPlayed,
      line = Some(playedLine),
      perspective = Color.White
    )
    val consequence = TransitionConsequence(
      kind = TransitionConsequenceKind.CenterControlGain,
      polarity = StructuralSignalPolarity.Gain,
      strength = 2
    )
    val structuralCause = RelativeCauseFact(
      kind = RelativeCauseKind.StructuralImprovement,
      comparisonKind = CandidateComparisonKind.PlayedVsBest,
      referenceLine = referenceLine,
      candidateLine = playedLine,
      verdict = MoveChoiceVerdict.Blunder,
      winPercentLossForMover = 12.0,
      candidateWinPercentDeltaForMover = -12.0,
      supportEvidence = List(structuralRef),
      evidenceLines = List(playedLine),
      role = RelativeCauseRole.PrimaryPlayedCause,
      eventLine = playedLine,
      sourceSide = RelativeCauseSourceSide.Candidate,
      importance = RelativeCauseImportance.Primary,
      attribution = CauseAttribution(
        kind = CauseAttributionKind.CandidateCreatesValue,
        ownedEvidence = List(structuralRef),
        rootMoveMatched = true,
        directProofEligible = false
      )
    )(None)
    val structuralCauseRef = evidenceRef(
      id = "relative-cause:played-best:weak-structural",
      producer = EvidenceProducer.RelativeMoveProducer,
      layer = EvidenceLayer.RelativeCause,
      position = root,
      line = Some(playedLine),
      scope = EvidenceScope.Counterfactual
    )
    val tacticalCauseRef = evidenceRef(
      id = "relative-cause:played-best:tactical-with-weak-structural",
      producer = EvidenceProducer.RelativeMoveProducer,
      layer = EvidenceLayer.RelativeCause,
      position = root,
      line = Some(playedLine),
      scope = EvidenceScope.Counterfactual
    )
    val tacticalCause = structuralCause.copy(
      kind = RelativeCauseKind.TacticalRefutationOfPlayed,
      supportEvidence = Nil,
      attribution = CauseAttribution(
        kind = CauseAttributionKind.CandidateAllowsLiability,
        rootMoveMatched = true,
        directProofEligible = true
      )
    )(None)
    val graph = TypedEvidenceGraph(
      List(
        EvidenceRecord(
          structuralRef,
          StructuralDeltaEvidence(transition = transition, signals = Nil, consequences = List(consequence))
        ),
        EvidenceRecord(structuralCauseRef, RelativeCauseFactEvidence(structuralCause), parents = List(structuralRef)),
        EvidenceRecord(tacticalCauseRef, RelativeCauseFactEvidence(tacticalCause))
      )
    )

    val view = MoveJudgmentView
      .from(
        relativeAssessments = Nil,
        evidenceGraph = graph,
        ideas = Nil,
        claims = Nil,
        claimLifecycle = Nil,
        ideaVerdict = None,
        claimSupportClusters = Nil,
        claimEventClusters = Nil
      )
      .get

    assertEquals(view.primaryCauses.map(_.causeKind), List(RelativeCauseKind.TacticalRefutationOfPlayed))
    assertEquals(view.primaryCauses.head.narrativeRole, MoveJudgmentCauseNarrativeRole.RootCause)

  test("binds playable loss played-vs-best evidence as context instead of primary cause"):
    val playedLine = LineNodeRef("played-line", "d2d4", 2, LineNodeRole.Played)
    val referenceLine = LineNodeRef("reference-line", "g1f3", 1, LineNodeRole.BestReference)
    val playedMoves = Set("d2d4")
    val playableComparison = CandidateComparisonFact(
      kind = CandidateComparisonKind.PlayedVsBest,
      referenceLine = referenceLine,
      candidateLine = playedLine,
      comparison = EvalComparison(
        mover = Color.White,
        referenceLine = referenceLine,
        candidateLine = playedLine,
        rawCandidateDeltaCpForDiagnostics = -10,
        candidateWinPercentDeltaForMover = 0.0,
        rawCpLossForDiagnostics = 10,
        winPercentLossForMover = 2.5,
        verdict = MoveChoiceVerdict.PlayableLoss
      )
    )
    val inaccuracyComparison = playableComparison.copy(
      comparison = playableComparison.comparison.copy(
        winPercentLossForMover = 4.0,
        verdict = MoveChoiceVerdict.Inaccuracy
      )
    )
    val playableCause = RelativeCauseFact(
      kind = RelativeCauseKind.TacticalRefutationOfPlayed,
      comparisonKind = CandidateComparisonKind.PlayedVsBest,
      referenceLine = referenceLine,
      candidateLine = playedLine,
      verdict = MoveChoiceVerdict.PlayableLoss,
      winPercentLossForMover = 2.5,
      candidateWinPercentDeltaForMover = 0.0,
      supportEvidence = Nil,
      evidenceLines = List(playedLine),
      role = RelativeCauseRole.PrimaryPlayedCause,
      eventLine = playedLine,
      sourceSide = RelativeCauseSourceSide.Candidate,
      importance = RelativeCauseImportance.Primary
    )()

    assertEquals(JudgmentSubjectBinding.comparisonBinding(playableComparison, playedMoves), SubjectBindingClass.ContextPlayed)
    assertEquals(JudgmentSubjectBinding.comparisonBinding(inaccuracyComparison, playedMoves), SubjectBindingClass.PrimaryPlayedCause)
    assertEquals(JudgmentSubjectBinding.relativeCauseBinding(playableCause, playedMoves), SubjectBindingClass.ContextPlayed)

  private def evidenceRef(
      id: String,
      producer: EvidenceProducer,
      layer: EvidenceLayer,
      position: PositionNodeRef,
      line: Option[LineNodeRef],
      scope: EvidenceScope
  ): EvidenceRef =
    EvidenceRef(
      id = id,
      producer = producer,
      layer = layer,
      position = position,
      line = line,
      scope = scope,
      confidence = EvidenceConfidence.EngineBacked
    )
