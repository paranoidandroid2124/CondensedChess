package lila.commentary.analysis

import munit.FunSuite
import lila.commentary.model.*
import lila.commentary.model.authoring.{ PlanHypothesis, PlanViability }

class QuietMoveIntentBuilderTest extends FunSuite:

  private def baseCtx(): NarrativeContext =
    NarrativeContext(
      fen = "r2q1rk1/pp2bppp/2np1n2/2p1p3/2P1P3/2NP1NP1/PP2QPBP/R1B2RK1 w - - 0 10",
      header = ContextHeader("Middlegame", "Normal", "StyleChoice", "Medium", "ExplainPlan"),
      ply = 20,
      playedMove = Some("g1f3"),
      playedSan = Some("Nf3"),
      summary = NarrativeSummary("Piece improvement", None, "StyleChoice", "Maintain", "0.20"),
      threats = ThreatTable(Nil, Nil),
      pawnPlay = PawnPlayTable(false, None, "Low", "Maintain", "Quiet", "Background", None, false, "quiet"),
      plans = PlanTable(Nil, Nil),
      delta = None,
      phase = PhaseContext("Middlegame", "Normal middlegame"),
      candidates = Nil,
      renderMode = NarrativeRenderMode.MoveReview
    )

  private def typedProbeBackedEvidence(plan: PlanHypothesis): PlanEvidenceEvaluator.StrategicPlanEvidenceView =
    val evaluated =
      PlanEvidenceEvaluator.EvaluatedPlan(
        hypothesis = plan,
        status = PlanEvidenceEvaluator.PlanEvidenceStatus.PlayableEvidenceBacked,
        userFacingEligibility = PlanEvidenceEvaluator.UserFacingPlanEligibility.ProbeBacked,
        reason = "typed probe evidence",
        supportProbeIds = List("probe_piece_improvement"),
        themeL1 = plan.themeL1,
        subplanId = plan.subplanId,
        claimCertification =
          PlanEvidenceEvaluator.ClaimCertification(
            certificateStatus = PlayerFacingCertificateStatus.Valid,
            quantifier = PlayerFacingClaimQuantifier.Universal,
            modalityTier = PlayerFacingClaimModalityTier.Advances,
            attributionGrade = PlayerFacingClaimAttributionGrade.Distinctive,
            stabilityGrade = PlayerFacingClaimStabilityGrade.Stable,
            provenanceClass = PlayerFacingClaimProvenanceClass.ProbeBacked,
            ontologyFamily = PlayerFacingClaimOntologyKind.PlanAdvance
          )
      )
    PlanEvidenceEvaluator.StrategicPlanEvidenceView(
      selectedPlans = List(evaluated),
      evaluatedPlans = List(evaluated)
    )

  private def plan: PlanHypothesis =
    PlanHypothesis(
      planId = "PieceImprovement",
      planName = "Improve the knight",
      rank = 1,
      score = 0.78,
      preconditions = Nil,
      executionSteps = List("Put the knight on f3."),
      failureModes = Nil,
      viability = PlanViability(0.78, "high", "stable"),
      themeL1 = PlanTaxonomy.PlanTheme.PieceRedeployment.id,
      subplanId = Some(PlanTaxonomy.PlanKind.WorstPieceImprovement.id)
    )

  test("legacy evidenceTier alone does not open quiet ProbeBacked provenance") {
    val ctx =
      baseCtx().copy(
        mainStrategicPlans = List(plan),
        strategicPlanExperiments =
          List(
            StrategicPlanExperiment(
              planId = "PieceImprovement",
              themeL1 = PlanTaxonomy.PlanTheme.PieceRedeployment.id,
              subplanId = Some(PlanTaxonomy.PlanKind.WorstPieceImprovement.id),
              evidenceTier = "evidence_backed",
              supportProbeCount = 1,
              bestReplyStable = true,
              futureSnapshotAligned = true
            )
          )
      )

    assertEquals(QuietMoveIntentBuilder.build(ctx), None)
  }

  test("typed probe-backed evidence opens quiet provenance and stability") {
    val ctx =
      baseCtx().copy(
        mainStrategicPlans = List(plan),
        strategicPlanEvidence = typedProbeBackedEvidence(plan)
      )

    val claim = QuietMoveIntentBuilder.build(ctx).getOrElse(fail("missing quiet claim"))
    assertEquals(claim.provenanceClass, PlayerFacingClaimProvenanceClass.ProbeBacked)
    assertEquals(claim.quantifier, PlayerFacingClaimQuantifier.Universal)
    assertEquals(claim.stabilityGrade, PlayerFacingClaimStabilityGrade.Stable)
    assertEquals(claim.certificateStatus, PlayerFacingCertificateStatus.Valid)
  }

  test("endgame quiet piece improvement does not become technical conversion") {
    val ctx =
      baseCtx().copy(
        phase = PhaseContext("Endgame", "Technical ending"),
        mainStrategicPlans = List(plan),
        strategicPlanEvidence = typedProbeBackedEvidence(plan)
      )

    val claim = QuietMoveIntentBuilder.build(ctx).getOrElse(fail("missing quiet claim"))
    assertEquals(claim.intentClass, QuietMoveIntentClass.PieceImprovement)
    assertEquals(claim.packet.proofFamily, QuietMoveIntentClass.PieceImprovement.proofFamily)
    assert(!claim.claimText.toLowerCase.contains("technical"), clue(claim.claimText))
    assert(!claim.claimText.toLowerCase.contains("endgame"), clue(claim.claimText))
  }
