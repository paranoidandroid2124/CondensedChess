package lila.chessjudgment.analysis.assembly

import chess.Color
import lila.chessjudgment.model.judgment.*

class RelativeCauseSignalProfileTest extends munit.FunSuite:

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
