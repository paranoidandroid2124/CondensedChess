package lila.chessjudgment.analysis.assembly

import chess.Color
import lila.chessjudgment.model.judgment.*

class RelativeCauseSignalProfileTest extends munit.FunSuite:

  test("does not mark contrast-only context attribution as root mismatch"):
    val root = PositionNodeRef("8/8/8/8/8/8/8/8 w - - 0 1", 1, Some(Color.White), Some("root"))
    val line = LineNodeRef("line", "g1f3", 1, LineNodeRole.BestReference)
    val contrastRef = EvidenceRef(
      id = "line:contrast",
      producer = EvidenceProducer.LegalLineProducer,
      layer = EvidenceLayer.Line,
      position = root,
      line = Some(line),
      scope = EvidenceScope.Counterfactual,
      confidence = EvidenceConfidence.EngineBacked
    )

    val attribution = CauseAttribution(
      kind = CauseAttributionKind.ContextOnly,
      contrastEvidence = List(contrastRef),
      rootMoveMatched = false,
      directProofEligible = false,
      reason = Some("no-owned-direct-proof")
    )

    assertEquals(attribution.rootMismatch, false)

  test("ignores strategic contrast records from other comparison identities"):
    val root = PositionNodeRef("8/8/8/8/8/8/8/8 w - - 0 1", 1, Some(Color.White), Some("root"))
    val bestLine = LineNodeRef("best-line", "g1f3", 1, LineNodeRole.BestReference)
    val playedLine = LineNodeRef("played-line", "d2d4", 2, LineNodeRole.Played)
    val alternativeLine = LineNodeRef("alternative-line", "c2c4", 3, LineNodeRole.Alternative)
    val contrastRef = EvidenceRef(
      id = "strategic-contrast:reference-vs-alternative",
      producer = EvidenceProducer.StrategicMechanismProducer,
      layer = EvidenceLayer.StrategicMechanism,
      position = root,
      line = Some(alternativeLine),
      scope = EvidenceScope.Counterfactual,
      confidence = EvidenceConfidence.EngineBacked
    )
    val contrastRecord = EvidenceRecord(
      ref = contrastRef,
      payload = StrategicMechanismContrastEvidence(
        comparisonKind = CandidateComparisonKind.ReferenceVsAlternative,
        referenceLine = bestLine,
        candidateLine = alternativeLine,
        axisComparisons = List(
          StrategicAxisComparison(
            axis = StrategicAxisDetail(StrategicAxisKind.Target, StrategicAxisPolarity.Gain, "target-pressure-gain"),
            outcome = StrategicAxisComparisonOutcome.ReferenceOnly,
            referenceStrength = 2,
            candidateStrength = 0,
            referenceSources = Nil,
            candidateSources = Nil
          )
        ),
        planComparison = None,
        sustainability = StrategicSustainabilityAssessment(
          horizon = StrategicSustainabilityHorizon.MediumPv,
          lineMaintained = true,
          pvMaintained = true,
          referencePlyCount = 6,
          candidatePlyCount = 6
        ),
        support = StrategicContrastSupport(Nil, Nil, Nil)
      )
    )
    val profile = RelativeCauseSignalProfile.from(
      fact = CandidateComparisonFact(
        kind = CandidateComparisonKind.PlayedVsBest,
        referenceLine = bestLine,
        candidateLine = playedLine,
        comparison = EvalComparison(
          mover = Color.White,
          referenceLine = bestLine,
          candidateLine = playedLine,
          rawCandidateDeltaCpForDiagnostics = -80,
          candidateWinPercentDeltaForMover = -7.0,
          rawCpLossForDiagnostics = 80,
          winPercentLossForMover = 7.0,
          verdict = MoveChoiceVerdict.Inaccuracy
        )
      ),
      referenceRecords = Nil,
      candidateRecords = Nil,
      sharedRecords = List(contrastRecord)
    )

    assertEquals(RelativeCauseDraftPlanner.drafts(profile).map(_.kind), Nil)

  test("does not draft played bad-cause activity loss for playable loss"):
    val root = PositionNodeRef("8/8/8/8/8/8/3P4/8 w - - 0 1", 1, Some(Color.White), Some("root"))
    val afterPlayed = PositionNodeRef("8/8/8/8/3P4/8/8/8 b - - 0 1", 2, Some(Color.Black), Some("after-played"))
    val playedLine = LineNodeRef("played-line", "d2d4", 2, LineNodeRole.Played)
    val referenceLine = LineNodeRef("reference-line", "g1f3", 1, LineNodeRole.BestReference)
    val activityLossRef = EvidenceRef(
      id = "structural-delta:played:d2d4",
      producer = EvidenceProducer.StructuralDeltaProducer,
      layer = EvidenceLayer.StructuralDelta,
      position = root,
      line = Some(playedLine),
      scope = EvidenceScope.PlayedTransition,
      confidence = EvidenceConfidence.EngineBacked
    )
    val activityLossRecord = EvidenceRecord(
      ref = activityLossRef,
      payload = StructuralDeltaEvidence(
        transition = StructuralTransitionBinding(
          moveUci = "d2d4",
          role = TransitionEdgeRole.Played,
          from = root,
          to = afterPlayed,
          line = Some(playedLine),
          perspective = Color.White
        ),
        signals = Nil,
        consequences = List(
          TransitionConsequence(
            kind = TransitionConsequenceKind.DevelopmentLagIncreased,
            polarity = StructuralSignalPolarity.Loss,
            strength = 2
          )
        )
      )
    )
    val tacticalRef = EvidenceRef(
      id = "tactical-mechanism:played:d2d4",
      producer = EvidenceProducer.TacticalMechanismProducer,
      layer = EvidenceLayer.TacticalMechanism,
      position = root,
      line = Some(playedLine),
      scope = EvidenceScope.PlayedLine,
      confidence = EvidenceConfidence.EngineBacked
    )
    val tacticalRecord = EvidenceRecord(
      ref = tacticalRef,
      payload = TacticalMechanismEvidence(
        kind = TacticalMechanismKind.MaterialGain,
        moveUci = Some("d2d4"),
        line = Some(playedLine),
        signals = List(
          TacticalMechanismSignal(
            kind = TacticalMechanismSignalKind.LineConsequence,
            label = "material-gain",
            sourceLayer = EvidenceLayer.Line
          )
        )
      )
    )
    val profile = RelativeCauseSignalProfile.from(
      fact = CandidateComparisonFact(
        kind = CandidateComparisonKind.PlayedVsBest,
        referenceLine = referenceLine,
        candidateLine = playedLine,
        comparison = EvalComparison(
          mover = Color.White,
          referenceLine = referenceLine,
          candidateLine = playedLine,
          rawCandidateDeltaCpForDiagnostics = -20,
          candidateWinPercentDeltaForMover = 0.0,
          rawCpLossForDiagnostics = 20,
          winPercentLossForMover = 2.5,
          verdict = MoveChoiceVerdict.PlayableLoss
        )
      ),
      referenceRecords = Nil,
      candidateRecords = List(activityLossRecord, tacticalRecord),
      sharedRecords = Nil
    )

    assertEquals(RelativeCauseDraftPlanner.drafts(profile).map(_.kind), Nil)

  test("drafts played-vs-best strategic contrast for near-threshold inaccuracy"):
    val root = PositionNodeRef("8/8/8/8/8/8/3P4/8 w - - 0 1", 1, Some(Color.White), Some("root"))
    val referenceLine = LineNodeRef("reference-line", "g1f3", 1, LineNodeRole.BestReference)
    val playedLine = LineNodeRef("played-line", "d2d4", 2, LineNodeRole.Played)
    val contrastRef = EvidenceRef(
      id = "strategic-contrast:played-vs-best",
      producer = EvidenceProducer.StrategicMechanismProducer,
      layer = EvidenceLayer.StrategicMechanism,
      position = root,
      line = Some(playedLine),
      scope = EvidenceScope.Counterfactual,
      confidence = EvidenceConfidence.EngineBacked
    )
    val contrastRecord = EvidenceRecord(
      ref = contrastRef,
      payload = StrategicMechanismContrastEvidence(
        comparisonKind = CandidateComparisonKind.PlayedVsBest,
        referenceLine = referenceLine,
        candidateLine = playedLine,
        axisComparisons = List(
          StrategicAxisComparison(
            axis = StrategicAxisDetail(
              StrategicAxisKind.PlanCoherence,
              StrategicAxisPolarity.Support,
              "OpeningDevelopment,PieceActivation"
            ),
            outcome = StrategicAxisComparisonOutcome.ReferenceOnly,
            referenceStrength = 2,
            candidateStrength = 0,
            referenceSources = Nil,
            candidateSources = Nil
          )
        ),
        planComparison = None,
        sustainability = StrategicSustainabilityAssessment(
          horizon = StrategicSustainabilityHorizon.MediumPv,
          lineMaintained = true,
          pvMaintained = true,
          referencePlyCount = 6,
          candidatePlyCount = 6
        ),
        support = StrategicContrastSupport(Nil, Nil, Nil)
      )
    )
    val profile = RelativeCauseSignalProfile.from(
      fact = CandidateComparisonFact(
        kind = CandidateComparisonKind.PlayedVsBest,
        referenceLine = referenceLine,
        candidateLine = playedLine,
        comparison = EvalComparison(
          mover = Color.White,
          referenceLine = referenceLine,
          candidateLine = playedLine,
          rawCandidateDeltaCpForDiagnostics = -35,
          candidateWinPercentDeltaForMover = -3.5,
          rawCpLossForDiagnostics = 35,
          winPercentLossForMover = 3.5,
          verdict = MoveChoiceVerdict.Inaccuracy
        )
      ),
      referenceRecords = List(contrastRecord),
      candidateRecords = Nil,
      sharedRecords = Nil
    )

    val drafts = RelativeCauseDraftPlanner.drafts(profile)
    assertEquals(drafts.map(_.kind), List(RelativeCauseKind.PlanContradiction))
    assertEquals(drafts.map(_.sourceSide), List(Some(RelativeCauseSourceSide.Reference)))

  test("prefers reference target pressure root over plan contradiction when axis proof is concrete"):
    val root = PositionNodeRef("8/8/8/8/8/8/3P4/8 w - - 0 1", 1, Some(Color.White), Some("root"))
    val referenceLine = LineNodeRef("reference-line", "g1f3", 1, LineNodeRole.BestReference)
    val playedLine = LineNodeRef("played-line", "d2d4", 2, LineNodeRole.Played)
    val targetSource = EvidenceRef(
      id = "strategic-mechanism:target-pressure:reference",
      producer = EvidenceProducer.StrategicMechanismProducer,
      layer = EvidenceLayer.StrategicMechanism,
      position = root,
      line = Some(referenceLine),
      scope = EvidenceScope.Counterfactual,
      confidence = EvidenceConfidence.EngineBacked
    )
    val contrastRecord = strategicContrastRecord(
      root = root,
      referenceLine = referenceLine,
      candidateLine = playedLine,
      comparisons = List(
        strategicAxisComparison(
          StrategicAxisDetail(StrategicAxisKind.Target, StrategicAxisPolarity.Gain, "target-pressure-gain"),
          StrategicAxisComparisonOutcome.ReferenceOnly,
          referenceSources = List(targetSource)
        ),
        strategicAxisComparison(
          StrategicAxisDetail(
            StrategicAxisKind.PlanCoherence,
            StrategicAxisPolarity.Support,
            "OpeningDevelopment,PieceActivation"
          ),
          StrategicAxisComparisonOutcome.ReferenceOnly
        )
      ),
      planComparison = Some(
        StrategicPlanComparison(
          referencePlanIds = List("OpeningDevelopment", "PieceActivation"),
          candidatePlanIds = Nil,
          outcome = StrategicAxisComparisonOutcome.ReferenceOnly
        )
      )
    )
    val profile = playedVsBestProfile(referenceLine, playedLine, List(contrastRecord))

    val drafts = RelativeCauseDraftPlanner.drafts(profile)
    assertEquals(drafts.map(_.kind), List(RelativeCauseKind.TargetPressureGain))
    assertEquals(drafts.map(_.sourceSide), List(Some(RelativeCauseSourceSide.Reference)))

  test("prefers candidate activity loss root over plan contradiction when axis proof is concrete"):
    val root = PositionNodeRef("8/8/8/8/8/8/3P4/8 w - - 0 1", 1, Some(Color.White), Some("root"))
    val referenceLine = LineNodeRef("reference-line", "g1f3", 1, LineNodeRole.BestReference)
    val playedLine = LineNodeRef("played-line", "d2d4", 2, LineNodeRole.Played)
    val activitySource = EvidenceRef(
      id = "strategic-mechanism:activity-loss:played",
      producer = EvidenceProducer.StrategicMechanismProducer,
      layer = EvidenceLayer.StrategicMechanism,
      position = root,
      line = Some(playedLine),
      scope = EvidenceScope.Counterfactual,
      confidence = EvidenceConfidence.EngineBacked
    )
    val contrastRecord = strategicContrastRecord(
      root = root,
      referenceLine = referenceLine,
      candidateLine = playedLine,
      comparisons = List(
        strategicAxisComparison(
          StrategicAxisDetail(StrategicAxisKind.Activity, StrategicAxisPolarity.Loss, "activity-loss"),
          StrategicAxisComparisonOutcome.CandidateConcession,
          candidateSources = List(activitySource)
        ),
        strategicAxisComparison(
          StrategicAxisDetail(
            StrategicAxisKind.PlanCoherence,
            StrategicAxisPolarity.Support,
            "OpeningDevelopment,PieceActivation"
          ),
          StrategicAxisComparisonOutcome.ReferenceOnly
        )
      ),
      planComparison = Some(
        StrategicPlanComparison(
          referencePlanIds = List("OpeningDevelopment", "PieceActivation"),
          candidatePlanIds = Nil,
          outcome = StrategicAxisComparisonOutcome.ReferenceOnly
        )
      )
    )
    val profile = playedVsBestProfile(referenceLine, playedLine, List(contrastRecord))

    val drafts = RelativeCauseDraftPlanner.drafts(profile)
    assertEquals(drafts.map(_.kind), List(RelativeCauseKind.ActivityLoss))
    assertEquals(drafts.map(_.sourceSide), List(Some(RelativeCauseSourceSide.Candidate)))

  test("does not relabel reference activity gain as candidate activity loss"):
    val root = PositionNodeRef("8/8/8/8/8/8/3P4/8 w - - 0 1", 1, Some(Color.White), Some("root"))
    val referenceLine = LineNodeRef("reference-line", "g1f3", 1, LineNodeRole.BestReference)
    val playedLine = LineNodeRef("played-line", "d2d4", 2, LineNodeRole.Played)
    val contrastRecord = strategicContrastRecord(
      root = root,
      referenceLine = referenceLine,
      candidateLine = playedLine,
      comparisons = List(
        strategicAxisComparison(
          StrategicAxisDetail(StrategicAxisKind.Activity, StrategicAxisPolarity.Gain, "activity-gain"),
          StrategicAxisComparisonOutcome.ReferenceOnly
        )
      ),
      planComparison = None
    )
    val profile = playedVsBestProfile(referenceLine, playedLine, List(contrastRecord))

    val drafts = RelativeCauseDraftPlanner.drafts(profile)
    assertEquals(drafts.map(_.kind), List(RelativeCauseKind.MissedStrategicImprovement))
    assertEquals(drafts.map(_.sourceSide), List(Some(RelativeCauseSourceSide.Reference)))

  test("does not relabel reference target pressure release as target pressure gain"):
    val root = PositionNodeRef("8/8/8/8/8/8/3P4/8 w - - 0 1", 1, Some(Color.White), Some("root"))
    val referenceLine = LineNodeRef("reference-line", "g1f3", 1, LineNodeRole.BestReference)
    val playedLine = LineNodeRef("played-line", "d2d4", 2, LineNodeRole.Played)
    val contrastRecord = strategicContrastRecord(
      root = root,
      referenceLine = referenceLine,
      candidateLine = playedLine,
      comparisons = List(
        strategicAxisComparison(
          StrategicAxisDetail(StrategicAxisKind.Target, StrategicAxisPolarity.Release, "target-pressure-release"),
          StrategicAxisComparisonOutcome.ReferenceOnly
        )
      ),
      planComparison = None
    )
    val profile = playedVsBestProfile(referenceLine, playedLine, List(contrastRecord))

    val drafts = RelativeCauseDraftPlanner.drafts(profile)
    assertEquals(drafts.map(_.kind), Nil)

  test("prefers candidate target pressure release root over plan contradiction when release axis is concrete"):
    val root = PositionNodeRef("8/8/8/8/8/8/3P4/8 w - - 0 1", 1, Some(Color.White), Some("root"))
    val referenceLine = LineNodeRef("reference-line", "g1f3", 1, LineNodeRole.BestReference)
    val playedLine = LineNodeRef("played-line", "d2d4", 2, LineNodeRole.Played)
    val releaseSource = EvidenceRef(
      id = "strategic-mechanism:target-pressure-release:played",
      producer = EvidenceProducer.StrategicMechanismProducer,
      layer = EvidenceLayer.StrategicMechanism,
      position = root,
      line = Some(playedLine),
      scope = EvidenceScope.Counterfactual,
      confidence = EvidenceConfidence.EngineBacked
    )
    val contrastRecord = strategicContrastRecord(
      root = root,
      referenceLine = referenceLine,
      candidateLine = playedLine,
      comparisons = List(
        strategicAxisComparison(
          StrategicAxisDetail(StrategicAxisKind.Target, StrategicAxisPolarity.Release, "target-pressure-release"),
          StrategicAxisComparisonOutcome.CandidateOnly,
          candidateSources = List(releaseSource)
        ),
        strategicAxisComparison(
          StrategicAxisDetail(
            StrategicAxisKind.PlanCoherence,
            StrategicAxisPolarity.Support,
            "OpeningDevelopment,PieceActivation"
          ),
          StrategicAxisComparisonOutcome.ReferenceOnly
        )
      ),
      planComparison = Some(
        StrategicPlanComparison(
          referencePlanIds = List("OpeningDevelopment", "PieceActivation"),
          candidatePlanIds = Nil,
          outcome = StrategicAxisComparisonOutcome.ReferenceOnly
        )
      )
    )
    val profile = playedVsBestProfile(referenceLine, playedLine, List(contrastRecord))

    val drafts = RelativeCauseDraftPlanner.drafts(profile)
    assertEquals(drafts.map(_.kind), List(RelativeCauseKind.TargetPressureRelease))
    assertEquals(drafts.map(_.sourceSide), List(Some(RelativeCauseSourceSide.Candidate)))

  test("does not treat candidate target pressure release as positive target pressure gain"):
    val root = PositionNodeRef("8/8/8/8/8/8/3P4/8 w - - 0 1", 1, Some(Color.White), Some("root"))
    val referenceLine = LineNodeRef("reference-line", "g1f3", 1, LineNodeRole.BestReference)
    val candidateLine = LineNodeRef("candidate-line", "d2d4", 2, LineNodeRole.Alternative)
    val releaseSource = EvidenceRef(
      id = "strategic-mechanism:target-pressure-release:candidate",
      producer = EvidenceProducer.StrategicMechanismProducer,
      layer = EvidenceLayer.StrategicMechanism,
      position = root,
      line = Some(candidateLine),
      scope = EvidenceScope.Counterfactual,
      confidence = EvidenceConfidence.EngineBacked
    )
    val contrastRecord = strategicContrastRecord(
      root = root,
      referenceLine = referenceLine,
      candidateLine = candidateLine,
      comparisons = List(
        strategicAxisComparison(
          StrategicAxisDetail(StrategicAxisKind.Target, StrategicAxisPolarity.Release, "target-pressure-release"),
          StrategicAxisComparisonOutcome.CandidateOnly,
          candidateSources = List(releaseSource)
        )
      ),
      planComparison = None,
      comparisonKind = CandidateComparisonKind.PlayedVsAlternative
    )
    val profile = RelativeCauseSignalProfile.from(
      fact = CandidateComparisonFact(
        kind = CandidateComparisonKind.PlayedVsAlternative,
        referenceLine = referenceLine,
        candidateLine = candidateLine,
        comparison = EvalComparison(
          mover = Color.White,
          referenceLine = referenceLine,
          candidateLine = candidateLine,
          rawCandidateDeltaCpForDiagnostics = 35,
          candidateWinPercentDeltaForMover = 3.5,
          rawCpLossForDiagnostics = -35,
          winPercentLossForMover = -3.5,
          verdict = MoveChoiceVerdict.ImprovesOnReference
        )
      ),
      referenceRecords = Nil,
      candidateRecords = List(contrastRecord),
      sharedRecords = Nil
    )

    val drafts = RelativeCauseDraftPlanner.drafts(profile)
    assertEquals(drafts.map(_.kind), Nil)

  test("uses plan comparison as the single source for plan contradiction"):
    val root = PositionNodeRef("8/8/8/8/8/8/3P4/8 w - - 0 1", 1, Some(Color.White), Some("root"))
    val referenceLine = LineNodeRef("reference-line", "g1f3", 1, LineNodeRole.BestReference)
    val playedLine = LineNodeRef("played-line", "d2d4", 2, LineNodeRole.Played)
    val contrastRecord = strategicContrastRecord(
      root = root,
      referenceLine = referenceLine,
      candidateLine = playedLine,
      comparisons = List(
        strategicAxisComparison(
          StrategicAxisDetail(
            StrategicAxisKind.PlanCoherence,
            StrategicAxisPolarity.Support,
            "OpeningDevelopment,PieceActivation"
          ),
          StrategicAxisComparisonOutcome.ReferenceOnly
        )
      ),
      planComparison = Some(
        StrategicPlanComparison(
          referencePlanIds = List("OpeningDevelopment", "PieceActivation"),
          candidatePlanIds = Nil,
          outcome = StrategicAxisComparisonOutcome.ReferenceOnly
        )
      )
    )
    val profile = playedVsBestProfile(referenceLine, playedLine, List(contrastRecord))

    val drafts = RelativeCauseDraftPlanner.drafts(profile)
    assertEquals(drafts.map(_.kind), List(RelativeCauseKind.PlanContradiction))
    assertEquals(drafts.map(_.sourceSide), List(Some(RelativeCauseSourceSide.Candidate)))

  test("prefers pawn break opportunity root over plan contradiction when pawn break axis is concrete"):
    val root = PositionNodeRef("8/8/8/8/8/8/3P4/8 w - - 0 1", 1, Some(Color.White), Some("root"))
    val referenceLine = LineNodeRef("reference-line", "c2c4", 1, LineNodeRole.BestReference)
    val playedLine = LineNodeRef("played-line", "g1f3", 2, LineNodeRole.Played)
    val contrastRecord = strategicContrastRecord(
      root = root,
      referenceLine = referenceLine,
      candidateLine = playedLine,
      comparisons = List(
        strategicAxisComparison(
          StrategicAxisDetail(StrategicAxisKind.PawnBreak, StrategicAxisPolarity.Support, "BreakReady"),
          StrategicAxisComparisonOutcome.ReferenceOnly
        ),
        strategicAxisComparison(
          StrategicAxisDetail(
            StrategicAxisKind.PlanCoherence,
            StrategicAxisPolarity.Support,
            "PawnBreakPreparation,PieceActivation"
          ),
          StrategicAxisComparisonOutcome.ReferenceOnly
        )
      ),
      planComparison = Some(
        StrategicPlanComparison(
          referencePlanIds = List("PawnBreakPreparation", "PieceActivation"),
          candidatePlanIds = Nil,
          outcome = StrategicAxisComparisonOutcome.ReferenceOnly
        )
      )
    )
    val profile = playedVsBestProfile(referenceLine, playedLine, List(contrastRecord))

    val drafts = RelativeCauseDraftPlanner.drafts(profile)
    assertEquals(drafts.map(_.kind), List(RelativeCauseKind.PawnBreakOpportunity))
    assertEquals(drafts.map(_.sourceSide), List(Some(RelativeCauseSourceSide.Reference)))

  private def playedVsBestProfile(
      referenceLine: LineNodeRef,
      playedLine: LineNodeRef,
      records: List[EvidenceRecord]
  ): RelativeCauseSignalProfile =
    RelativeCauseSignalProfile.from(
      fact = CandidateComparisonFact(
        kind = CandidateComparisonKind.PlayedVsBest,
        referenceLine = referenceLine,
        candidateLine = playedLine,
        comparison = EvalComparison(
          mover = Color.White,
          referenceLine = referenceLine,
          candidateLine = playedLine,
          rawCandidateDeltaCpForDiagnostics = -35,
          candidateWinPercentDeltaForMover = -3.5,
          rawCpLossForDiagnostics = 35,
          winPercentLossForMover = 3.5,
          verdict = MoveChoiceVerdict.Inaccuracy
        )
      ),
      referenceRecords = records,
      candidateRecords = Nil,
      sharedRecords = Nil
    )

  private def strategicContrastRecord(
      root: PositionNodeRef,
      referenceLine: LineNodeRef,
      candidateLine: LineNodeRef,
      comparisons: List[StrategicAxisComparison],
      planComparison: Option[StrategicPlanComparison],
      comparisonKind: CandidateComparisonKind = CandidateComparisonKind.PlayedVsBest
  ): EvidenceRecord =
    EvidenceRecord(
      ref = EvidenceRef(
        id = "strategic-contrast:played-vs-best",
        producer = EvidenceProducer.StrategicMechanismProducer,
        layer = EvidenceLayer.StrategicMechanism,
        position = root,
        line = Some(candidateLine),
        scope = EvidenceScope.Counterfactual,
        confidence = EvidenceConfidence.EngineBacked
      ),
      payload = StrategicMechanismContrastEvidence(
        comparisonKind = comparisonKind,
        referenceLine = referenceLine,
        candidateLine = candidateLine,
        axisComparisons = comparisons,
        planComparison = planComparison,
        sustainability = StrategicSustainabilityAssessment(
          horizon = StrategicSustainabilityHorizon.MediumPv,
          lineMaintained = true,
          pvMaintained = true,
          referencePlyCount = 6,
          candidatePlyCount = 6
        ),
        support = StrategicContrastSupport(Nil, Nil, Nil)
      )
    )

  private def strategicAxisComparison(
      axis: StrategicAxisDetail,
      outcome: StrategicAxisComparisonOutcome,
      referenceSources: List[EvidenceRef] = Nil,
      candidateSources: List[EvidenceRef] = Nil
  ): StrategicAxisComparison =
    StrategicAxisComparison(
      axis = axis,
      outcome = outcome,
      referenceStrength = if referenceSources.nonEmpty || outcome == StrategicAxisComparisonOutcome.ReferenceOnly then 2 else 0,
      candidateStrength = if candidateSources.nonEmpty || outcome == StrategicAxisComparisonOutcome.CandidateConcession then 2 else 0,
      referenceSources = referenceSources,
      candidateSources = candidateSources
    )
