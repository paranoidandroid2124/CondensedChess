package lila.commentary.analysis

import chess.{ Color, Knight, Queen, Rook, Square }
import lila.commentary.*
import lila.commentary.analysis.practical.ContrastiveSupportAdmissibility
import lila.commentary.analysis.semantic.StrategicObservationIds.{ ProofFamilyId, ProofSourceId }
import lila.commentary.model.*
import lila.commentary.model.authoring.{ AuthorQuestion, AuthorQuestionKind, PlanHypothesis, PlanViability }
import lila.commentary.model.strategic.{ CounterfactualMatch, VariationLine }
import munit.FunSuite

final class MoveReviewCompressionPolicyTest extends FunSuite:

  private def quietH3Ctx: NarrativeContext =
    NarrativeContext(
      fen = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1",
      header = ContextHeader("Opening", "Normal", "StyleChoice", "Low", "ExplainPlan"),
      ply = 1,
      playedMove = Some("h2h3"),
      playedSan = Some("h3"),
      summary = NarrativeSummary("quiet move", None, "StyleChoice", "Maintain", "0.00"),
      threats = ThreatTable(Nil, Nil),
      pawnPlay = PawnPlayTable(false, None, "Low", "Maintain", "Quiet", "Background", None, false, "quiet"),
      plans = PlanTable(Nil, Nil),
      delta = None,
      phase = PhaseContext("Opening", "quiet opening move"),
      candidates = Nil,
      renderMode = NarrativeRenderMode.MoveReview
    )

  private val italianBeforeBc4 =
    "r1bqkbnr/pppp1ppp/2n5/4p3/4P3/5N2/PPPP1PPP/RNBQKB1R w KQkq - 2 3"

  private val exchangeLineFen =
    "rnbqkbnr/pppp1ppp/8/4p3/4P3/8/PPPP1PPP/RNBQKBNR w KQkq - 0 2"

  private def italianCtx: NarrativeContext =
    NarrativeContext(
      fen = italianBeforeBc4,
      header = ContextHeader("Opening", "Normal", "StyleChoice", "Low", "ExplainPlan"),
      ply = 5,
      playedMove = Some("f1c4"),
      playedSan = Some("Bc4"),
      summary = NarrativeSummary("Italian Game development", None, "StyleChoice", "Maintain", "0.00"),
      threats = ThreatTable(Nil, Nil),
      pawnPlay = PawnPlayTable(false, None, "Low", "Maintain", "Quiet", "Background", None, false, "quiet"),
      plans = PlanTable(Nil, Nil),
      delta = None,
      phase = PhaseContext("Opening", "Italian Game development"),
      candidates = Nil,
      openingData = Some(
        OpeningReference(
          eco = Some("C50"),
          name = Some("Italian Game"),
          totalGames = 420000,
          topMoves = List(ExplorerMove("f1c4", "Bc4", 210000, 93000, 52000, 65000, 2460)),
          sampleGames = Nil
        )
      ),
      openingGoalEvaluation = Some(
        OpeningGoals.Evaluation(
          goalName = "Development Logic",
          status = OpeningGoals.Status.Achieved,
          supportedEvidence = List("Minor piece developed"),
          missingEvidence = Nil,
          confidence = 0.86
        )
      ),
      renderMode = NarrativeRenderMode.MoveReview
    )

  private def exchangeLineCtx: NarrativeContext =
    NarrativeContext(
      fen = exchangeLineFen,
      header = ContextHeader("Middlegame", "Normal", "NarrowChoice", "Medium", "ExplainPlan"),
      ply = NarrativeUtils.plyFromFen(exchangeLineFen).getOrElse(1),
      playedMove = Some("g1f3"),
      playedSan = Some("Nf3"),
      summary = NarrativeSummary("line consequence", None, "NarrowChoice", "Maintain", "0.00"),
      threats = ThreatTable(Nil, Nil),
      pawnPlay = PawnPlayTable(false, None, "Low", "Maintain", "Quiet", "Background", None, false, "quiet"),
      plans = PlanTable(Nil, Nil),
      delta = None,
      phase = PhaseContext("Middlegame", "Line consequence"),
      candidates = Nil,
      renderMode = NarrativeRenderMode.MoveReview
    )

  private def refsForLine(startFen: String, ucis: List[String], sans: List[String]): MoveReviewRefs =
    val fens = ucis.indices.toList.map(idx => NarrativeUtils.uciListToFen(startFen, ucis.take(idx + 1)))
    MoveReviewRefs(
      startFen = startFen,
      startPly = NarrativeUtils.plyFromFen(startFen).map(_ + 1).getOrElse(1),
      variations = List(
        MoveReviewVariationRef(
          lineId = "line_01",
          scoreCp = 16,
          mate = None,
          depth = 16,
          moves =
            ucis.zip(sans).zipWithIndex.map { case ((uci, san), idx) =>
              val ply = NarrativeUtils.plyFromFen(startFen).map(_ + 1 + idx).getOrElse(idx + 1)
              MoveReviewMoveRef(
                refId = s"line_01_m${idx + 1}",
                san = san,
                uci = uci,
                fenAfter = fens(idx),
                ply = ply,
                moveNo = (ply + 1) / 2,
                marker = None
              )
            }
          )
      )
    )

  private def whatChangedQuestion(id: String = "q_changed"): AuthorQuestion =
    AuthorQuestion(
      id = id,
      kind = AuthorQuestionKind.WhatChanged,
      priority = 100,
      question = "What changed?"
    )

  private def pvCoupledPlanEvidence(
      planName: String,
      rank: Int = 1
  ): PlanEvidenceEvaluator.StrategicPlanEvidenceView =
    val hypothesis =
      PlanHypothesis(
        planId = planName.toLowerCase.replaceAll("[^a-z0-9]+", "_").stripPrefix("_").stripSuffix("_"),
        planName = planName,
        rank = rank,
        score = 0.62,
        preconditions = Nil,
        executionSteps = Nil,
        failureModes = Nil,
        viability = PlanViability(0.62, "medium", "pv-coupled test fixture"),
        evidenceSources = List("pv_coupled"),
        themeL1 = "piece_redeployment"
      )
    val evaluated =
      PlanEvidenceEvaluator.EvaluatedPlan(
        hypothesis = hypothesis,
        status = PlanEvidenceEvaluator.PlanEvidenceStatus.PlayablePvCoupled,
        userFacingEligibility = PlanEvidenceEvaluator.UserFacingPlanEligibility.PvCoupledOnly,
        reason = "test fixture pv-coupled plan",
        pvCoupled = true,
        themeL1 = hypothesis.themeL1,
        subplanId = hypothesis.subplanId,
        claimCertification =
          PlanEvidenceEvaluator.ClaimCertification(
            certificateStatus = PlayerFacingCertificateStatus.WeaklyValid,
            attributionGrade = PlayerFacingClaimAttributionGrade.AnchoredButShared,
            stabilityGrade = PlayerFacingClaimStabilityGrade.Unstable,
            provenanceClass = PlayerFacingClaimProvenanceClass.PvCoupled,
            ontologyFamily = PlayerFacingClaimOntologyKind.PlanAdvance
          )
      )
    PlanEvidenceEvaluator.StrategicPlanEvidenceView(evaluatedPlans = List(evaluated))

  private def tacticalBlunderContract(
      playedMove: String = "h2h3",
      bestMove: String = "e2e4"
  ): DecisiveTruthContract =
    DecisiveTruthContract(
      playedMove = Some(playedMove),
      verifiedBestMove = Some(bestMove),
      truthClass = DecisiveTruthClass.Blunder,
      cpLoss = 300,
      swingSeverity = 300,
      reasonFamily = DecisiveReasonKind.TacticalRefutation,
      allowConcreteBenchmark = false,
      chosenMatchesBest = false,
      compensationAllowed = false,
      truthPhase = None,
      ownershipRole = TruthOwnershipRole.BlunderOwner,
      visibilityRole = TruthVisibilityRole.PrimaryVisible,
      surfaceMode = TruthSurfaceMode.FailureExplain,
      exemplarRole = TruthExemplarRole.NonExemplar,
      surfacedMoveOwnsTruth = true,
      verifiedPayoffAnchor = None,
      compensationProseAllowed = false,
      benchmarkProseAllowed = false,
      investmentTruthChainKey = None,
      maintenanceExemplarCandidate = false,
      benchmarkCriticalMove = false,
      failureMode = FailureInterpretationMode.TacticalRefutation,
      failureIntentConfidence = 0.9,
      failureIntentAnchor = None,
      failureInterpretationAllowed = true
    )

  test("basic lane stays closed when no primitive is safe and exact factual fallback remains") {
    val ctx = quietH3Ctx
    val outline = BookStyleRenderer.validatedOutline(ctx)
    val explanation = MoveReviewExplanationBuilder.build(ctx, None)
    val slots =
      MoveReviewPolishSlotsBuilder.buildOrFallback(
        ctx,
        outline,
        refs = None,
        strategyPack = None,
        truthContract = None
      )

    assertEquals(explanation, None, clues(explanation))
    assertEquals(
      MoveReviewProseContract.stripMoveHeader(slots.claim),
      "This moves the pawn to h3.",
      clues(slots)
    )
    assertEquals(slots.paragraphPlan, List("p1=claim"), clues(slots))
    assertEquals(slots.moveReviewExplanation, None, clues(slots))
  }

  test("live narrative filter allows anchored normal chess phrases") {
    val phrases = List(
      "This cuts out counterplay on b5.",
      "This keeps the pieces coordinated around d4.",
      "This holds the position together after Qd8."
    )

    phrases.foreach { phrase =>
      assertEquals(LiveNarrativeCompressionCore.playerLanguageHits(phrase), Nil, clues(phrase))
      assert(LiveNarrativeCompressionCore.keepPlayerFacingSentence(phrase), clues(phrase))
      assert(!LiveNarrativeCompressionCore.isLowValueNarrativeSentence(phrase), clues(phrase))
    }
  }

  test("MoveReviewLocalFact admits planner timing only from typed timing evidence") {
    val timedPlan =
      QuestionPlan(
        questionId = "q_typed_timing",
        questionKind = AuthorQuestionKind.WhyNow,
        priority = 100,
        claim = "The timing matters because the direct reply would otherwise arrive.",
        evidence = None,
        contrast = None,
        consequence = None,
        fallbackMode = QuestionPlanFallbackMode.PlannerOwned,
        strengthTier = QuestionPlanStrengthTier.Strong,
        sourceKinds = List("only_move_defense"),
        admissibilityReasons = List("timing_owner"),
        plannerOwnerKind = PlannerOwnerKind.ForcingDefense,
        plannerSource = "only_move_defense",
        timingWitness = Some(
          QuestionPlanTimingWitness(
            proofFamily = "only_move_defense",
            source = "only_move_defense",
            witnessTokens = List("reply_loss")
          )
        )
      )
    val proseOnlyPlan =
      timedPlan.copy(
        questionId = "q_prose_timing",
        claim = "This supports the plan and keeps pressure in time.",
        sourceKinds = List("move_delta"),
        plannerOwnerKind = PlannerOwnerKind.MoveDelta,
        plannerSource = "move_delta",
        timingWitness = None
      )

    val typed =
      MoveReviewLocalFact.admitPlanner(
        timedPlan,
        evidenceKinds = List("timing_tension", "timing_witness"),
        relationKinds = List("timing_constraint"),
        lineConsequenceBacked = false,
        lineBinding = MoveReviewLocalFact.LineBinding.Replayed
      )
    val proseOnly =
      MoveReviewLocalFact.admitPlanner(
        proseOnlyPlan,
        evidenceKinds = Nil,
        relationKinds = Nil,
        lineConsequenceBacked = false
      )

    assertEquals(typed.admission.map(_.family), Some(MoveReviewLocalFact.Family.Timing))
    assertEquals(typed.admission.map(_.authority), Some(MoveReviewLocalFact.Authority.OnlyMoveDefense))
    assertEquals(typed.admission.map(_.strictFallbackEligible), Some(true))
    assertEquals(typed.admission.map(_.lineBinding), Some(MoveReviewLocalFact.LineBinding.Replayed))
    assertEquals(
      typed.admission.map(_.evidenceRefs),
      Some(List("evidence_kind:timing_tension", "evidence_kind:timing_witness"))
    )
    assertEquals(proseOnly.admission, None)
    assert(proseOnly.rejectReasons.contains("local_fact_family_missing"), clues(proseOnly))
  }

  test("MoveReviewCausalClaim admission ignores causal words without typed evidence") {
    val proseOnlyPlan =
      QuestionPlan(
        questionId = "q_prose_only_causal_words",
        questionKind = AuthorQuestionKind.WhyNow,
        priority = 100,
        claim = "This supports the plan, keeps pressure, and forces the reply right now.",
        evidence = None,
        contrast = None,
        consequence = None,
        fallbackMode = QuestionPlanFallbackMode.PlannerOwned,
        strengthTier = QuestionPlanStrengthTier.Strong,
        sourceKinds = List("move_delta"),
        admissibilityReasons = List("wording_only"),
        plannerOwnerKind = PlannerOwnerKind.MoveDelta,
        plannerSource = "move_delta"
      )
    val typedPlan =
      proseOnlyPlan.copy(
        questionId = "q_typed_timing_claim",
        sourceKinds = List("only_move_defense"),
        plannerOwnerKind = PlannerOwnerKind.ForcingDefense,
        plannerSource = "only_move_defense",
        timingWitness = Some(
          QuestionPlanTimingWitness(
            proofFamily = "only_move_defense",
            source = "only_move_defense",
            continuationMove = Some("Qd8"),
            witnessTokens = List("Qd8")
          )
        )
      )

    val proseOnly =
      MoveReviewCausalClaim.admit(
        MoveReviewCausalClaim.Candidate(
          plan = proseOnlyPlan,
          renderedClaim = proseOnlyPlan.claim,
          evidences = Nil
        )
      )
    val typed =
      MoveReviewCausalClaim.admit(
        MoveReviewCausalClaim.Candidate(
          plan = typedPlan,
          renderedClaim = typedPlan.claim,
          evidences =
            List(
              MoveReviewCausalClaim.TypedEvidence(
                kind = MoveReviewCausalClaim.EvidenceKind.TimingWitness,
                source = MoveReviewCausalClaim.EvidenceSource.TimingWitness,
                text = Some("Qd8 is the direct reply witness."),
                subjectRole = MoveReviewCausalClaim.SubjectRole.LineOrReply,
                lineBinding = MoveReviewLocalFact.LineBinding.Replayed
              )
            )
        )
      )

    assertEquals(proseOnly.claim, None)
    assert(proseOnly.rejectReasons.contains("causal_support_missing"), clues(proseOnly))
    assert(proseOnly.rejectReasons.contains("causal_relation_missing"), clues(proseOnly))
    assertEquals(typed.claim.map(_.questionKind), Some(AuthorQuestionKind.WhyNow), clues(typed))
    assertEquals(typed.frame.map(_.intent), Some(MoveReviewCausalClaim.CausalIntent.WhyNow), clues(typed))
    assertEquals(
      typed.frame.map(_.surfaceContract.mayUseTimingClaim),
      Some(true),
      clues(typed)
    )
    assertEquals(typed.claim.map(_.relationKinds), Some(List(MoveReviewCausalClaim.RelationKind.TimingConstraint)), clues(typed))
    assertEquals(typed.claim.flatMap(_.localFact.map(_.family)), Some(MoveReviewLocalFact.Family.Timing), clues(typed))
    assertEquals(typed.claim.flatMap(_.localFact.map(_.lineBinding)), Some(MoveReviewLocalFact.LineBinding.Replayed), clues(typed))
  }

  test("PlanRace causal admission accepts typed branch-line evidence") {
    val plan =
      QuestionPlan(
        questionId = "q_plan_race_branch",
        questionKind = AuthorQuestionKind.WhosePlanIsFaster,
        priority = 100,
        claim = "23...Rc8 24.Rg3 shows the counterplay race.",
        evidence = None,
        contrast = None,
        consequence = None,
        fallbackMode = QuestionPlanFallbackMode.PlannerOwned,
        strengthTier = QuestionPlanStrengthTier.Strong,
        sourceKinds = List("reply_multipv"),
        admissibilityReasons = List("plan_race_primary_in_plan_clash"),
        plannerOwnerKind = PlannerOwnerKind.PlanRace,
        plannerSource = "directional_target"
      )
    val decision =
      MoveReviewCausalClaim.admit(
        MoveReviewCausalClaim.Candidate(
          plan = plan,
          renderedClaim = plan.claim,
          evidences =
            List(
              MoveReviewCausalClaim.TypedEvidence(
                kind = MoveReviewCausalClaim.EvidenceKind.BranchLine,
                source = MoveReviewCausalClaim.EvidenceSource.BranchLine,
                text = Some("23...Rc8 24.Rg3 Rc7 25.Qxg7+"),
                subjectRole = MoveReviewCausalClaim.SubjectRole.LineOrReply,
                lineBinding = MoveReviewLocalFact.LineBinding.BranchScoped
              )
            )
        )
      )

    assertEquals(decision.claim.map(_.relationKinds), Some(List(MoveReviewCausalClaim.RelationKind.PlanRace)), clues(decision))
    assertEquals(
      decision.frame.map(_.surfaceContract.mayUseCheckedLine),
      Some(true),
      clues(decision)
    )
    assertEquals(decision.frame.map(_.roles.lineOrReply), Some(true), clues(decision))
    assertEquals(decision.claim.flatMap(_.localFact.map(_.family)), Some(MoveReviewLocalFact.Family.Timing), clues(decision))
    assertEquals(decision.claim.flatMap(_.localFact.map(_.lineBinding)), Some(MoveReviewLocalFact.LineBinding.BranchScoped), clues(decision))
    assertEquals(
      decision.claim.flatMap(_.surfacePacket.evidenceHook),
      Some("23...Rc8 24.Rg3 Rc7 25.Qxg7+"),
      clues(decision)
    )
    assert(decision.claim.exists(_.surfacePacket.guardrails.contains("surface_checked_line=true")), clues(decision))
  }

  test("claim-only causal planner slots fall back to exact factual surface") {
    val primary =
      QuestionPlan(
        questionId = "q_claim_only",
        questionKind = AuthorQuestionKind.WhyNow,
        priority = 100,
        claim = "The timing matters now because other moves allow the position to slip away.",
        evidence = None,
        contrast = None,
        consequence = None,
        fallbackMode = QuestionPlanFallbackMode.PlannerOwned,
        strengthTier = QuestionPlanStrengthTier.Strong,
        sourceKinds = List("only_move_defense"),
        admissibilityReasons = List("timing_owner"),
        plannerOwnerKind = PlannerOwnerKind.ForcingDefense,
        plannerSource = "only_move_defense"
      )
    val slots =
      MoveReviewCompressionPolicy.buildSlotsOrFallbackFromPlannerRuntime(
        quietH3Ctx,
        QuestionPlannerInputs(
          mainBundle = None,
          quietIntent = None,
          decisionFrame = CertifiedDecisionFrame(),
          decisionComparison = None,
          alternativeNarrative = None,
          truthMode = PlayerFacingTruthMode.Strategic,
          preventedPlansNow = Nil,
          pvDelta = None,
          counterfactual = None,
          practicalAssessment = None,
          opponentThreats = Nil,
          forcingThreats = Nil,
          evidenceByQuestionId = Map.empty,
          candidateEvidenceLines = Nil,
          evidenceBackedPlans = Nil,
          opponentPlan = None,
          factualFallback = None
        ),
        RankedQuestionPlans(primary = Some(primary), secondary = None, rejected = Nil),
        strategyPack = None,
        truthContract = None
      )

    assertEquals(slots.sourceKind, MoveReviewPolishSlots.Source.ExactFactualFallback)
    assertEquals(slots.paragraphPlan, List("p1=claim"))
    assert(slots.claim.startsWith("1. h3:"), clues(slots.claim))
    assertEquals(
      MoveReviewProseContract.stripMoveHeader(slots.claim),
      "This moves the pawn to h3."
    )
  }

  test("WhyNow generic urgency tension without typed timing support stays fallback") {
    val primary =
      QuestionPlan(
        questionId = "q_generic_timing",
        questionKind = AuthorQuestionKind.WhyNow,
        priority = 100,
        claim = "h3 stops counterplay on b5 before the reply arrives.",
        evidence = None,
        contrast = None,
        consequence = None,
        fallbackMode = QuestionPlanFallbackMode.PlannerOwned,
        strengthTier = QuestionPlanStrengthTier.Strong,
        sourceKinds = List("decision_comparison"),
        admissibilityReasons = List("timing_owner"),
        plannerOwnerKind = PlannerOwnerKind.DecisionTiming,
        plannerSource = "decision_comparison"
      )
    val slots =
      MoveReviewCompressionPolicy.buildSlotsOrFallbackFromPlannerRuntime(
        quietH3Ctx,
        QuestionPlannerInputs(
          mainBundle = None,
          quietIntent = None,
          decisionFrame =
            CertifiedDecisionFrame(
              urgency =
                Some(
                  CertifiedDecisionSupport(
                    axis = CertifiedDecisionFrameAxis.Urgency,
                    sentence = "The immediate counterplay on b5 is live now.",
                    priority = 80,
                    sourceKind = "generic_urgency"
                  )
                )
            ),
          decisionComparison = None,
          alternativeNarrative = None,
          truthMode = PlayerFacingTruthMode.Strategic,
          preventedPlansNow = Nil,
          pvDelta = None,
          counterfactual = None,
          practicalAssessment = None,
          opponentThreats = Nil,
          forcingThreats = Nil,
          evidenceByQuestionId = Map.empty,
          candidateEvidenceLines = Nil,
          evidenceBackedPlans = Nil,
          opponentPlan = None,
          factualFallback = None
        ),
        RankedQuestionPlans(primary = Some(primary), secondary = None, rejected = Nil),
        strategyPack = None,
        truthContract = None
      )
    val causalTrace =
      MoveReviewCompressionPolicy.causalClaimTrace(
        quietH3Ctx,
        QuestionPlannerInputs(
          mainBundle = None,
          quietIntent = None,
          decisionFrame =
            CertifiedDecisionFrame(
              urgency =
                Some(
                  CertifiedDecisionSupport(
                    axis = CertifiedDecisionFrameAxis.Urgency,
                    sentence = "The immediate counterplay on b5 is live now.",
                    priority = 80,
                    sourceKind = "generic_urgency"
                  )
                )
            ),
          decisionComparison = None,
          alternativeNarrative = None,
          truthMode = PlayerFacingTruthMode.Strategic,
          preventedPlansNow = Nil,
          pvDelta = None,
          counterfactual = None,
          practicalAssessment = None,
          opponentThreats = Nil,
          forcingThreats = Nil,
          evidenceByQuestionId = Map.empty,
          candidateEvidenceLines = Nil,
          evidenceBackedPlans = Nil,
          opponentPlan = None,
          factualFallback = None
        ),
        RankedQuestionPlans(primary = Some(primary), secondary = None, rejected = Nil),
        truthContract = None,
        refs = None
      )

    assertEquals(slots.sourceKind, MoveReviewPolishSlots.Source.ExactFactualFallback)
    assertEquals(
      MoveReviewProseContract.stripMoveHeader(slots.claim),
      "This moves the pawn to h3."
    )
    assertEquals(causalTrace.map(_.status), Some("rejected"))
    assertEquals(causalTrace.map(_.questionKind), Some("WhyNow"))
    assertEquals(
      causalTrace.map(_.rejectReasons),
      Some(List("causal_relation_missing", "why_now_timing_authority_missing"))
    )
    assertEquals(causalTrace.flatMap(_.frameIntent), Some("why_now"))
    assert(causalTrace.exists(_.frameSurfaceContract.contains("surface_timing=false")), clues(causalTrace))
  }

  test("WhyNow timing witness authorizes a timing constraint surface") {
    val primary =
      QuestionPlan(
        questionId = "q_typed_timing_witness",
        questionKind = AuthorQuestionKind.WhyNow,
        priority = 100,
        claim = "The move has to happen now because otherwise Qd8 is demanded immediately.",
        evidence = None,
        contrast = None,
        consequence = None,
        fallbackMode = QuestionPlanFallbackMode.PlannerOwned,
        strengthTier = QuestionPlanStrengthTier.Strong,
        sourceKinds = List("threat"),
        admissibilityReasons = List("timing_owner", "delay_sensitive_proof"),
        plannerOwnerKind = PlannerOwnerKind.ForcingDefense,
        plannerSource = "threat",
        timingWitness =
          Some(
            QuestionPlanTimingWitness(
              proofFamily = "neutralize_key_break",
              source = "threat",
              continuationMove = Some("Qd8"),
              witnessTokens = List("Qd8")
            )
          )
      )
    val inputs =
      QuestionPlannerInputs(
        mainBundle = None,
        quietIntent = None,
        decisionFrame =
          CertifiedDecisionFrame(
            urgency =
              Some(
                CertifiedDecisionSupport(
                  axis = CertifiedDecisionFrameAxis.Urgency,
                  sentence = "The immediate Qd8 threat gives this move a concrete timing window.",
                  priority = 90,
                  sourceKind = "threat"
                )
              )
          ),
        decisionComparison = None,
        alternativeNarrative = None,
        truthMode = PlayerFacingTruthMode.Strategic,
        preventedPlansNow = Nil,
        pvDelta = None,
        counterfactual = None,
        practicalAssessment = None,
        opponentThreats = Nil,
        forcingThreats = Nil,
        evidenceByQuestionId = Map.empty,
        candidateEvidenceLines = Nil,
        evidenceBackedPlans = Nil,
        opponentPlan = None,
        factualFallback = None
      )
    val slots =
      MoveReviewCompressionPolicy.buildSlotsOrFallbackFromPlannerRuntime(
        quietH3Ctx,
        inputs,
        RankedQuestionPlans(primary = Some(primary), secondary = None, rejected = Nil),
        strategyPack = None,
        truthContract = None
      )
    val causalTrace =
      MoveReviewCompressionPolicy.causalClaimTrace(
        quietH3Ctx,
        inputs,
        RankedQuestionPlans(primary = Some(primary), secondary = None, rejected = Nil),
        truthContract = None,
        refs = None
      )

    assertEquals(slots.sourceKind, MoveReviewPolishSlots.Source.Planner)
    assert(MoveReviewProseContract.stripMoveHeader(slots.claim).contains("Qd8"), clues(slots.claim))
    assert(slots.factGuardrails.exists(_.contains("evidence=timing_tension,timing_witness")), clues(slots.factGuardrails))
    assert(slots.factGuardrails.exists(_.contains("relations=timing_constraint")), clues(slots.factGuardrails))
    assert(slots.factGuardrails.exists(_.contains("local_fact=timing/certified_strategy")), clues(slots.factGuardrails))
    assertEquals(slots.tension, Some("The immediate Qd8 threat gives this move a concrete timing window."))
    assertEquals(slots.evidenceHook, None)
    assertEquals(causalTrace.flatMap(_.localFactFamily), Some("timing"))
    assertEquals(causalTrace.flatMap(_.localFactAuthority), Some("certified_strategy"))
    assertEquals(causalTrace.flatMap(_.localFactStrictFallbackEligible), Some(true))
  }

  test("opening relation WhyThis needs admissible contrast before renderer release") {
    val primary =
      QuestionPlan(
        questionId = "q_opening_relation",
        questionKind = AuthorQuestionKind.WhyThis,
        priority = 100,
        claim = "The current move leaves the established opening branch, so that idea has to work in the concrete line.",
        evidence =
          Some(
            QuestionPlanEvidence(
              text = "a) h3 h6 Nf3",
              purposes = List("reply_multipv"),
              sourceKinds = List("opening_relation_translator"),
              branchScoped = true
            )
          ),
        contrast = Some("The practical alternative e4 keeps the more familiar opening route."),
        consequence = Some(QuestionPlanConsequence("That changes which opening script the position follows.", QuestionPlanConsequenceBeat.WrapUp)),
        fallbackMode = QuestionPlanFallbackMode.PlannerOwned,
        strengthTier = QuestionPlanStrengthTier.Moderate,
        sourceKinds = List("opening_relation_translator"),
        admissibilityReasons = List("opening_relation_owner", "scene_first_domain_owner"),
        plannerOwnerKind = PlannerOwnerKind.OpeningRelation,
        plannerSource = "opening_relation_translator"
      )
    val slots =
      MoveReviewCompressionPolicy.buildSlotsOrFallbackFromPlannerRuntime(
        quietH3Ctx,
        QuestionPlannerInputs(
          mainBundle = None,
          quietIntent = None,
          decisionFrame = CertifiedDecisionFrame(),
          decisionComparison = None,
          alternativeNarrative = None,
          truthMode = PlayerFacingTruthMode.Strategic,
          preventedPlansNow = Nil,
          pvDelta = None,
          counterfactual = None,
          practicalAssessment = None,
          opponentThreats = Nil,
          forcingThreats = Nil,
          evidenceByQuestionId = Map.empty,
          candidateEvidenceLines = Nil,
          evidenceBackedPlans = Nil,
          opponentPlan = None,
          factualFallback = None
        ),
        RankedQuestionPlans(primary = Some(primary), secondary = None, rejected = Nil),
        strategyPack = None,
        truthContract = None
      )

    assertEquals(slots.sourceKind, MoveReviewPolishSlots.Source.ExactFactualFallback)
    assertEquals(
      MoveReviewProseContract.stripMoveHeader(slots.claim),
      "This moves the pawn to h3."
    )
  }

  test("WhyThis branch evidence alone does not replace contrast or played-move ownership") {
    val primary =
      QuestionPlan(
        questionId = "q_branch_only_why_this",
        questionKind = AuthorQuestionKind.WhyThis,
        priority = 100,
        claim = "h3 keeps the counterplay reply under control in the checked line.",
        evidence =
          Some(
            QuestionPlanEvidence(
              text = "a) h3 h6 Nf3",
              purposes = List("reply_multipv"),
              sourceKinds = List("reply_multipv"),
              branchScoped = true
            )
          ),
        contrast = None,
        consequence = Some(QuestionPlanConsequence("That removes the immediate problem of counterplay.", QuestionPlanConsequenceBeat.WrapUp)),
        fallbackMode = QuestionPlanFallbackMode.PlannerOwned,
        strengthTier = QuestionPlanStrengthTier.Moderate,
        sourceKinds = List("reply_multipv"),
        admissibilityReasons = List("branch_line_support"),
        plannerOwnerKind = PlannerOwnerKind.DecisionTiming,
        plannerSource = "reply_multipv"
      )
    val slots =
      MoveReviewCompressionPolicy.buildSlotsOrFallbackFromPlannerRuntime(
        quietH3Ctx,
        QuestionPlannerInputs(
          mainBundle = None,
          quietIntent = None,
          decisionFrame = CertifiedDecisionFrame(),
          decisionComparison = None,
          alternativeNarrative = None,
          truthMode = PlayerFacingTruthMode.Strategic,
          preventedPlansNow = Nil,
          pvDelta = None,
          counterfactual = None,
          practicalAssessment = None,
          opponentThreats = Nil,
          forcingThreats = Nil,
          evidenceByQuestionId = Map.empty,
          candidateEvidenceLines = Nil,
          evidenceBackedPlans = Nil,
          opponentPlan = None,
          factualFallback = None
        ),
        RankedQuestionPlans(primary = Some(primary), secondary = None, rejected = Nil),
        strategyPack = None,
        truthContract = None
      )

    assertEquals(slots.sourceKind, MoveReviewPolishSlots.Source.ExactFactualFallback)
    assertEquals(
      MoveReviewProseContract.stripMoveHeader(slots.claim),
      "This moves the pawn to h3."
    )
  }

  test("WhyThis admissible alternative contrast becomes the certified surface claim") {
    val contrastSentence =
      "Both candidate branches are viable: the played h3 follows 1. h3 h6, whereas e4 opts for 1. e4 e5."
    val primary =
      QuestionPlan(
        questionId = "q_certified_alternative",
        questionKind = AuthorQuestionKind.WhyThis,
        priority = 100,
        claim = "h3 keeps the counterplay reply under control in the checked line.",
        evidence = None,
        contrast = Some("The practical alternative e4 is a close candidate."),
        consequence = None,
        fallbackMode = QuestionPlanFallbackMode.PlannerOwned,
        strengthTier = QuestionPlanStrengthTier.Moderate,
        sourceKinds = List("decision_comparison"),
        admissibilityReasons = List("enriched_close_candidate"),
        plannerOwnerKind = PlannerOwnerKind.DecisionTiming,
        plannerSource = "decision_comparison"
      )
    val slots =
      MoveReviewCompressionPolicy.buildSlotsOrFallbackFromPlannerRuntime(
        quietH3Ctx,
        QuestionPlannerInputs(
          mainBundle = None,
          quietIntent = None,
          decisionFrame = CertifiedDecisionFrame(),
          decisionComparison =
            Some(
              DecisionComparison(
                chosenMove = Some("h3"),
                engineBestMove = Some("h3"),
                engineBestScoreCp = Some(12),
                engineBestPv = List("h3", "h6"),
                cpLossVsChosen = None,
                deferredMove = Some("e4"),
                deferredReason = Some("different candidate branches"),
                deferredSource = Some("close_candidate"),
                evidence = None,
                practicalAlternative = true,
                chosenMatchesBest = true
              )
            ),
          alternativeNarrative =
            Some(
              AlternativeNarrative(
                move = Some("e4"),
                reason = "different candidate branches",
                sentence = contrastSentence,
                source = "close_candidate"
              )
            ),
          truthMode = PlayerFacingTruthMode.Strategic,
          preventedPlansNow = Nil,
          pvDelta = None,
          counterfactual = None,
          practicalAssessment = None,
          opponentThreats = Nil,
          forcingThreats = Nil,
          evidenceByQuestionId = Map.empty,
          candidateEvidenceLines = Nil,
          evidenceBackedPlans = Nil,
          opponentPlan = None,
          factualFallback = None
        ),
        RankedQuestionPlans(primary = Some(primary), secondary = None, rejected = Nil),
        strategyPack = None,
        truthContract = None
      )

    assertEquals(slots.sourceKind, MoveReviewPolishSlots.Source.Planner)
    assertEquals(MoveReviewProseContract.stripMoveHeader(slots.claim), contrastSentence)
    assertEquals(slots.supportPrimary, None)
    assertEquals(slots.paragraphPlan, List("p1=claim"))
    assert(slots.factGuardrails.exists(_.contains("relations=alternative_contrast")), clues(slots.factGuardrails))
    assert(!slots.claim.contains("keeps the counterplay reply under control"), clues(slots.claim))
  }

  test("WhyThis role-aware line consequence admits a line-consequence local fact") {
    val consequence =
      "e4 reaches a central pawn advance on the engine-best branch 1. e4 e5; h3 stays on the played branch 1. h3 h6 without that concrete central pawn advance."
    val primary =
      QuestionPlan(
        questionId = "q_role_aware_line_consequence",
        questionKind = AuthorQuestionKind.WhyThis,
        priority = 100,
        claim = s"The concrete difference is that $consequence",
        evidence = None,
        contrast = None,
        consequence = None,
        fallbackMode = QuestionPlanFallbackMode.DemotedToWhyThis,
        strengthTier = QuestionPlanStrengthTier.Strong,
        sourceKinds = List("decision_comparison", DecisionComparisonComparativeSupport.RoleAwareLineConsequenceSource),
        admissibilityReasons = List(DecisionComparisonComparativeSupport.RoleAwareLineConsequenceSource),
        plannerOwnerKind = PlannerOwnerKind.AlternativeComparison,
        plannerSource = "decision_comparison"
      )
    val slots =
      MoveReviewCompressionPolicy.buildSlotsOrFallbackFromPlannerRuntime(
        quietH3Ctx,
        QuestionPlannerInputs(
          mainBundle = None,
          quietIntent = None,
          decisionFrame = CertifiedDecisionFrame(),
          decisionComparison =
            Some(
              DecisionComparison(
                chosenMove = Some("h3"),
                engineBestMove = Some("e4"),
                engineBestScoreCp = Some(40),
                engineBestPv = List("e4", "e5"),
                cpLossVsChosen = None,
                deferredMove = Some("e4"),
                deferredReason = None,
                deferredSource = Some("verified_best"),
                evidence = Some("e4 e5"),
                practicalAlternative = false,
                chosenMatchesBest = false,
                comparedMove = Some("h3"),
                comparativeConsequence = Some(consequence),
                comparativeSource = Some(DecisionComparisonComparativeSupport.RoleAwareLineConsequenceSource)
              )
            ),
          alternativeNarrative = None,
          truthMode = PlayerFacingTruthMode.Strategic,
          preventedPlansNow = Nil,
          pvDelta = None,
          counterfactual = None,
          practicalAssessment = None,
          opponentThreats = Nil,
          forcingThreats = Nil,
          evidenceByQuestionId = Map.empty,
          candidateEvidenceLines = Nil,
          evidenceBackedPlans = Nil,
          opponentPlan = None,
          factualFallback = None
        ),
        RankedQuestionPlans(primary = Some(primary), secondary = None, rejected = Nil),
        strategyPack = None,
        truthContract = Some(tacticalBlunderContract(playedMove = "h2h3", bestMove = "e2e4"))
      )

    assertEquals(slots.sourceKind, MoveReviewPolishSlots.Source.Planner)
    assertEquals(MoveReviewProseContract.stripMoveHeader(slots.claim), consequence)
    assert(slots.factGuardrails.exists(_.contains("local_fact=line_consequence/pv_coupled_line")), clues(slots.factGuardrails))
    assert(slots.factGuardrails.exists(_.contains("relations=alternative_contrast")), clues(slots.factGuardrails))
  }

  test("WhatChanged role-aware alternative contrast is not replaced by played-move line consequence") {
    val consequence =
      "e4 reaches a central pawn advance on the engine-best branch 1. e4 e5; h3 stays on the played branch 1. h3 h6 without that concrete central pawn advance."
    val refs =
      refsForLine(
        quietH3Ctx.fen,
        List("h2h3", "a7a6", "g1f3"),
        List("h3", "a6", "Nf3")
      )
    val primary =
      QuestionPlan(
        questionId = "q_role_aware_line_consequence_what_changed",
        questionKind = AuthorQuestionKind.WhatChanged,
        priority = 100,
        claim = s"The concrete difference is that $consequence",
        evidence = None,
        contrast = None,
        consequence = None,
        fallbackMode = QuestionPlanFallbackMode.DemotedToWhyThis,
        strengthTier = QuestionPlanStrengthTier.Strong,
        sourceKinds = List("decision_comparison", DecisionComparisonComparativeSupport.RoleAwareLineConsequenceSource),
        admissibilityReasons = List(DecisionComparisonComparativeSupport.RoleAwareLineConsequenceSource),
        plannerOwnerKind = PlannerOwnerKind.AlternativeComparison,
        plannerSource = "decision_comparison"
      )
    val slots =
      MoveReviewCompressionPolicy.buildSlotsOrFallbackFromPlannerRuntime(
        quietH3Ctx,
        QuestionPlannerInputs(
          mainBundle = None,
          quietIntent = None,
          decisionFrame = CertifiedDecisionFrame(),
          decisionComparison =
            Some(
              DecisionComparison(
                chosenMove = Some("h3"),
                engineBestMove = Some("e4"),
                engineBestScoreCp = Some(40),
                engineBestPv = List("e4", "e5"),
                cpLossVsChosen = None,
                deferredMove = Some("e4"),
                deferredReason = None,
                deferredSource = Some("verified_best"),
                evidence = Some("e4 e5"),
                practicalAlternative = false,
                chosenMatchesBest = false,
                comparedMove = Some("h3"),
                comparativeConsequence = Some(consequence),
                comparativeSource = Some(DecisionComparisonComparativeSupport.RoleAwareLineConsequenceSource)
              )
            ),
          alternativeNarrative = None,
          truthMode = PlayerFacingTruthMode.Strategic,
          preventedPlansNow = Nil,
          pvDelta = None,
          counterfactual = None,
          practicalAssessment = None,
          opponentThreats = Nil,
          forcingThreats = Nil,
          evidenceByQuestionId = Map.empty,
          candidateEvidenceLines = Nil,
          evidenceBackedPlans = Nil,
          opponentPlan = None,
          factualFallback = None
        ),
        RankedQuestionPlans(primary = Some(primary), secondary = None, rejected = Nil),
        strategyPack = None,
        truthContract = Some(tacticalBlunderContract(playedMove = "h2h3", bestMove = "e2e4")),
        refs = Some(refs)
      )

    assertEquals(slots.sourceKind, MoveReviewPolishSlots.Source.Planner)
    assert(MoveReviewProseContract.stripMoveHeader(slots.claim).contains(consequence), clues(slots.claim))
    assert(!slots.claim.contains("On the checked line"), clues(slots.claim))
    assert(slots.factGuardrails.exists(_.contains("relations=alternative_contrast")), clues(slots.factGuardrails))
    assert(!slots.factGuardrails.exists(_.contains("played_move_consequence")), clues(slots.factGuardrails))
    assertEquals(slots.coda, None)
  }

  test("WhyThis counterfactual causal threat admits a threat local fact") {
    val primary =
      QuestionPlan(
        questionId = "q_counterfactual_causal_threat",
        questionKind = AuthorQuestionKind.WhyThis,
        priority = 100,
        claim = "Bd7 keeps the move order precise.",
        evidence = None,
        contrast = None,
        consequence = Some(
          QuestionPlanConsequence(
            "If the move is missed, Qxd4 becomes the cleaner continuation instead.",
            QuestionPlanConsequenceBeat.WrapUp
          )
        ),
        fallbackMode = QuestionPlanFallbackMode.PlannerOwned,
        strengthTier = QuestionPlanStrengthTier.Strong,
        sourceKinds = List("decision_comparison", ContrastiveSupportAdmissibility.SourceKind.CounterfactualCausalThreat),
        admissibilityReasons = List(ContrastiveSupportAdmissibility.SourceKind.CounterfactualCausalThreat),
        plannerOwnerKind = PlannerOwnerKind.ConcreteTactical,
        plannerSource = "decision_comparison"
      )
    val fork =
      Motif.Fork(
        Knight,
        List(Rook, Queen),
        Square.F5,
        List(Square.E7, Square.H4),
        Color.White,
        1,
        Some("Nf5")
      )
    val ctx = quietH3Ctx.copy(playedSan = Some("Bd7"), playedMove = Some("c8d7"))
    val inputs =
      QuestionPlannerInputs(
        mainBundle = None,
        quietIntent = None,
        decisionFrame = CertifiedDecisionFrame(),
        decisionComparison =
          Some(
            DecisionComparison(
              chosenMove = Some("Bd7"),
              engineBestMove = Some("Bd7"),
              engineBestScoreCp = Some(40),
              engineBestPv = List("Bd7", "Nxc6", "Bxc6"),
              cpLossVsChosen = None,
              deferredMove = None,
              deferredReason = None,
              deferredSource = None,
              evidence = None,
              practicalAlternative = false,
              chosenMatchesBest = true
            )
          ),
        alternativeNarrative = None,
        truthMode = PlayerFacingTruthMode.Strategic,
        preventedPlansNow = Nil,
        pvDelta = None,
        counterfactual =
          Some(
            CounterfactualMatch(
              userMove = "Bd7",
              bestMove = "Qxd4",
              cpLoss = 90,
              missedMotifs = List(fork),
              userMoveMotifs = Nil,
              severity = "moderate",
              userLine = VariationLine(Nil, 0),
              causalThreat =
                Some(
                  ThreatExtractor.CausalThreat(
                    concept = "Material Loss",
                    severity = 85,
                    narrative = "allows a devastating fork on the rook and queen",
                    motifs = List(fork)
                  )
                )
            )
          ),
        practicalAssessment = None,
        opponentThreats = Nil,
        forcingThreats = Nil,
        evidenceByQuestionId = Map.empty,
        candidateEvidenceLines = Nil,
        evidenceBackedPlans = Nil,
        opponentPlan = None,
        factualFallback = None
      )
    val ranked = RankedQuestionPlans(primary = Some(primary), secondary = None, rejected = Nil)
    val slots =
      MoveReviewCompressionPolicy.buildSlotsOrFallbackFromPlannerRuntime(
        ctx,
        inputs,
        ranked,
        strategyPack = None,
        truthContract = None
      )
    val causalTrace =
      MoveReviewCompressionPolicy.causalClaimTrace(
        ctx,
        inputs,
        ranked,
        truthContract = None,
        refs = None
      )

    assertEquals(slots.sourceKind, MoveReviewPolishSlots.Source.Planner)
    assertEquals(
      MoveReviewProseContract.stripMoveHeader(slots.claim),
      "The move Bd7 stays best because missing it allows a devastating fork on the rook and queen."
    )
    assert(slots.factGuardrails.exists(_.contains("local_fact=threat/certified_strategy")), clues(slots.factGuardrails))
    assert(slots.factGuardrails.exists(_.contains("local_fact_producer=planner_causal_claim")), clues(slots.factGuardrails))
    assert(causalTrace.exists(_.evidenceSources.contains("counterfactual_causal_threat")), clues(causalTrace))
    assert(
      causalTrace.exists(_.localFactGuardrails.exists(_.contains("counterfactual_causal_threat:motif_backed"))),
      clues(causalTrace)
    )
  }

  test("WhyThis played-move line consequence becomes the certified surface claim") {
    val rawTemplateClaim = "Nf3 keeps the structure under control in the checked line."
    val refs =
      refsForLine(
        exchangeLineFen,
        List("g1f3", "b8c6", "f1b5", "a7a6", "b5c6", "d7c6"),
        List("Nf3", "Nc6", "Bb5", "a6", "Bxc6", "dxc6")
      )
    val primary =
      QuestionPlan(
        questionId = "q_line_consequence_why_this",
        questionKind = AuthorQuestionKind.WhyThis,
        priority = 100,
        claim = rawTemplateClaim,
        evidence = None,
        contrast = None,
        consequence = None,
        fallbackMode = QuestionPlanFallbackMode.PlannerOwned,
        strengthTier = QuestionPlanStrengthTier.Moderate,
        sourceKinds = List("move_delta"),
        admissibilityReasons = List("move_owner"),
        plannerOwnerKind = PlannerOwnerKind.MoveDelta,
        plannerSource = "move_delta"
      )
    val slots =
      MoveReviewCompressionPolicy.buildSlotsOrFallbackFromPlannerRuntime(
        exchangeLineCtx,
        QuestionPlannerInputs(
          mainBundle = None,
          quietIntent = None,
          decisionFrame = CertifiedDecisionFrame(),
          decisionComparison = None,
          alternativeNarrative = None,
          truthMode = PlayerFacingTruthMode.Strategic,
          preventedPlansNow = Nil,
          pvDelta = None,
          counterfactual = None,
          practicalAssessment = None,
          opponentThreats = Nil,
          forcingThreats = Nil,
          evidenceByQuestionId = Map.empty,
          candidateEvidenceLines = Nil,
          evidenceBackedPlans = Nil,
          opponentPlan = None,
          factualFallback = None
        ),
        RankedQuestionPlans(primary = Some(primary), secondary = None, rejected = Nil),
        strategyPack = None,
        truthContract = None,
        refs = Some(refs)
      )

    val claim = MoveReviewProseContract.stripMoveHeader(slots.claim)
    assertEquals(slots.sourceKind, MoveReviewPolishSlots.Source.Planner)
    assert(claim.startsWith("On the checked line"), clues(claim))
    assert(claim.contains("exchange sequence"), clues(claim))
    assert(!claim.contains(rawTemplateClaim), clues(claim))
    assertEquals(slots.supportPrimary, None)
    assertEquals(slots.coda, None)
    assertEquals(slots.paragraphPlan, List("p1=claim"))
    assert(slots.factGuardrails.exists(_.contains("evidence=certified_consequence")), clues(slots.factGuardrails))
    assert(slots.factGuardrails.exists(_.contains("relations=played_move_consequence")), clues(slots.factGuardrails))
    assert(slots.factGuardrails.exists(_.contains("support_embedded=true")), clues(slots.factGuardrails))
  }

  test("surface plan-support wording cannot mint a local fact family") {
    val primary =
      QuestionPlan(
        questionId = "q_surface_plan_support_only",
        questionKind = AuthorQuestionKind.WhyThis,
        priority = 100,
        claim = "This supports the plan toward g7.",
        evidence = None,
        contrast = None,
        consequence = Some(QuestionPlanConsequence("This keeps pressure on g7.", QuestionPlanConsequenceBeat.WrapUp)),
        fallbackMode = QuestionPlanFallbackMode.PlannerOwned,
        strengthTier = QuestionPlanStrengthTier.Moderate,
        sourceKinds = List("move_delta"),
        admissibilityReasons = List("move_owner"),
        plannerOwnerKind = PlannerOwnerKind.MoveDelta,
        plannerSource = "move_delta"
      )
    val inputs =
      QuestionPlannerInputs(
        mainBundle = None,
        quietIntent = None,
        decisionFrame = CertifiedDecisionFrame(),
        decisionComparison = None,
        alternativeNarrative = None,
        truthMode = PlayerFacingTruthMode.Strategic,
        preventedPlansNow = Nil,
        pvDelta = None,
        counterfactual = None,
        practicalAssessment = None,
        opponentThreats = Nil,
        forcingThreats = Nil,
        evidenceByQuestionId = Map.empty,
        candidateEvidenceLines = Nil,
        evidenceBackedPlans = Nil,
        opponentPlan = None,
        factualFallback = None
      )
    val ranked = RankedQuestionPlans(primary = Some(primary), secondary = None, rejected = Nil)
    val slots =
      MoveReviewCompressionPolicy.buildSlotsOrFallbackFromPlannerRuntime(
        quietH3Ctx,
        inputs,
        ranked,
        strategyPack = None,
        truthContract = None
      )
    val causalTrace =
      MoveReviewCompressionPolicy.causalClaimTrace(
        quietH3Ctx,
        inputs,
        ranked,
        truthContract = None,
        refs = None
      )

    assertEquals(slots.sourceKind, MoveReviewPolishSlots.Source.ExactFactualFallback)
    assertEquals(MoveReviewProseContract.stripMoveHeader(slots.claim), "This moves the pawn to h3.")
    assertEquals(causalTrace.map(_.status), Some("rejected"))
    assert(causalTrace.exists(_.rejectReasons.contains("local_fact_admission_missing")), clues(causalTrace))
    assertEquals(causalTrace.flatMap(_.localFactFamily), None)
  }

  test("pv-coupled plan support admits a plan-support local fact") {
    val primary =
      QuestionPlan(
        questionId = "q_pv_plan_support",
        questionKind = AuthorQuestionKind.WhyThis,
        priority = 100,
        claim = "The checked line from h3 keeps Improving piece placement viable as a practical plan.",
        evidence =
          Some(
            QuestionPlanEvidence(
              text = "a) h3 e4 d5",
              purposes = List("planner_line_proof"),
              sourceKinds = List("pv_coupled_plan_support"),
              branchScoped = true
            )
          ),
        contrast = None,
        consequence = None,
        fallbackMode = QuestionPlanFallbackMode.PlannerOwned,
        strengthTier = QuestionPlanStrengthTier.Moderate,
        sourceKinds = List("pv_delta", "pv_coupled_plan_support"),
        admissibilityReasons = List("move_attributed_change"),
        plannerOwnerKind = PlannerOwnerKind.MoveDelta,
        plannerSource = "pv_delta"
      )
    val inputs =
      QuestionPlannerInputs(
        mainBundle = None,
        quietIntent = None,
        decisionFrame = CertifiedDecisionFrame(),
        decisionComparison = None,
        alternativeNarrative = None,
        truthMode = PlayerFacingTruthMode.Strategic,
        preventedPlansNow = Nil,
        pvDelta = Some(PVDelta(Nil, Nil, Nil, Nil)),
        counterfactual = None,
        practicalAssessment = None,
        opponentThreats = Nil,
        forcingThreats = Nil,
        evidenceByQuestionId = Map.empty,
        candidateEvidenceLines = List("h3 e4 d5"),
        evidenceBackedPlans = Nil,
        opponentPlan = None,
        factualFallback = None,
        pvCoupledPlanSupport =
          Some(
            PvCoupledPlanSupport(
              planName = "Improving piece placement",
              playedSan = "h3",
              evidenceLine = "h3 e4 d5"
            )
          )
      )
    val ranked = RankedQuestionPlans(primary = Some(primary), secondary = None, rejected = Nil)
    val slots =
      MoveReviewCompressionPolicy.buildSlotsOrFallbackFromPlannerRuntime(
        quietH3Ctx,
        inputs,
        ranked,
        strategyPack = None,
        truthContract = None
      )
    val causalTrace =
      MoveReviewCompressionPolicy.causalClaimTrace(
        quietH3Ctx,
        inputs,
        ranked,
        truthContract = None,
        refs = None
      )

    assertEquals(slots.sourceKind, MoveReviewPolishSlots.Source.Planner)
    assertEquals(causalTrace.flatMap(_.localFactFamily), Some("plan_support"), clues(causalTrace))
    assertEquals(causalTrace.flatMap(_.localFactAuthority), Some("pv_coupled_line"), clues(causalTrace))
    assertEquals(causalTrace.flatMap(_.localFactProducer), Some("planner_causal_claim"), clues(causalTrace))
    assert(causalTrace.exists(_.evidenceKinds.contains("branch_line")), clues(causalTrace))
    assert(causalTrace.exists(_.relationKinds.contains("played_move_consequence")), clues(causalTrace))
  }

  test("candidate evidence includes reviewed move PV refs for planner coupling") {
    val refs = refsForLine(quietH3Ctx.fen, List("h2h3", "a7a6", "g1f3"), List("h3", "a6", "Nf3"))
    val lines = MoveReviewCompressionPolicy.candidateEvidenceLines(Some(refs), quietH3Ctx)

    assert(lines.contains("Short line: h3 a6 Nf3."), clues(lines))
  }

  test("planner builder connects reviewed move PV refs to pv-coupled plan support") {
    val refs = refsForLine(quietH3Ctx.fen, List("h2h3", "a7a6", "g1f3"), List("h3", "a6", "Nf3"))
    val ctx =
      quietH3Ctx.copy(
        authorQuestions = List(whatChangedQuestion()),
        decision = Some(DecisionRationale(None, "checked line", PVDelta(Nil, Nil, Nil, Nil), ConfidenceLevel.Engine)),
        strategicPlanEvidence = pvCoupledPlanEvidence("Improving piece placement")
      )
    val candidateEvidence = MoveReviewCompressionPolicy.candidateEvidenceLines(Some(refs), ctx)
    val inputs = QuestionPlannerInputsBuilder.build(ctx, strategyPack = None, truthContract = None, candidateEvidence)
    val ranked = QuestionFirstCommentaryPlanner.plan(ctx, inputs, truthContract = None)
    val causalTrace =
      MoveReviewCompressionPolicy.causalClaimTrace(
        ctx,
        inputs,
        ranked,
        truthContract = None,
        refs = Some(refs)
      )

    val primary = ranked.primary.getOrElse(fail(clues(ranked.rejected, ranked.ownerTrace.ownerCandidateLabels).toString))
    assert(
      inputs.pvCoupledPlanSupport.exists(support =>
        LineScopedCitation.firstConcreteSanToken(support.evidenceLine).contains("h3")
      ),
      clues(inputs.pvCoupledPlanSupport)
    )
    assertEquals(primary.questionKind, AuthorQuestionKind.WhatChanged)
    assert(primary.sourceKinds.contains("pv_coupled_plan_support"), clues(primary.sourceKinds))
    assert(primary.claim.contains("Improving piece placement"), clues(primary.claim))
    assertEquals(causalTrace.flatMap(_.localFactFamily), Some("plan_support"), clues(causalTrace))
    assertEquals(causalTrace.flatMap(_.localFactAuthority), Some("pv_coupled_line"), clues(causalTrace))
  }

  test("admitted claim packet promotes into CausalFrame evidence before local fact admission") {
    val packet = supportedNeutralizePacketForPromotion
    val primary =
      QuestionPlan(
        questionId = "q_promoted_packet_defense",
        questionKind = AuthorQuestionKind.WhyThis,
        priority = 100,
        claim = "h3 keeps the counterplay break under control.",
        evidence = None,
        contrast = None,
        consequence = None,
        fallbackMode = QuestionPlanFallbackMode.PlannerOwned,
        strengthTier = QuestionPlanStrengthTier.Strong,
        sourceKinds = List(ProofSourceId.CounterplayAxisSuppression.wireKey),
        admissibilityReasons = List("move_owner"),
        plannerOwnerKind = PlannerOwnerKind.MoveDelta,
        plannerSource = ProofSourceId.CounterplayAxisSuppression.wireKey
      )
    val promotedClaim =
      MainPathScopedClaim(
        scope = PlayerFacingClaimScope.MoveLocal,
        mode = PlayerFacingTruthMode.Strategic,
        deltaClass = Some(PlayerFacingMoveDeltaClass.CounterplayReduction),
        claimText = "h3 restrains the ...c5 counterplay break before it lands.",
        anchorTerms = List("...c5"),
        evidenceLines = List("h3 e4d5 c6d5"),
        sourceKind = ProofSourceId.CounterplayAxisSuppression.wireKey,
        tacticalOwnership = None,
        packet = Some(packet)
      )
    val inputs =
      QuestionPlannerInputs(
        mainBundle = Some(MainPathClaimBundle(mainClaim = Some(promotedClaim), lineScopedClaim = None)),
        quietIntent = None,
        decisionFrame = CertifiedDecisionFrame(),
        decisionComparison = None,
        alternativeNarrative = None,
        truthMode = PlayerFacingTruthMode.Strategic,
        preventedPlansNow = Nil,
        pvDelta = None,
        counterfactual = None,
        practicalAssessment = None,
        opponentThreats = Nil,
        forcingThreats = Nil,
        evidenceByQuestionId = Map.empty,
        candidateEvidenceLines = Nil,
        evidenceBackedPlans = Nil,
        opponentPlan = None,
        factualFallback = None
      )
    val ranked = RankedQuestionPlans(primary = Some(primary), secondary = None, rejected = Nil)
    val slots =
      MoveReviewCompressionPolicy.buildSlotsOrFallbackFromPlannerRuntime(
        quietH3Ctx,
        inputs,
        ranked,
        strategyPack = None,
        truthContract = None
      )
    val causalTrace =
      MoveReviewCompressionPolicy.causalClaimTrace(
        quietH3Ctx,
        inputs,
        ranked,
        truthContract = None,
        refs = None
      )

    assertEquals(slots.sourceKind, MoveReviewPolishSlots.Source.Planner)
    assertEquals(causalTrace.map(_.status), Some("accepted"))
    assertEquals(causalTrace.flatMap(_.localFactFamily), Some("defense"))
    assertEquals(causalTrace.flatMap(_.localFactAuthority), Some("pv_coupled_line"))
    assert(causalTrace.exists(_.evidenceKinds.contains("defense")), clues(causalTrace))
    assert(causalTrace.exists(_.evidenceSources.contains("supported_local_packet")), clues(causalTrace))
    assert(causalTrace.exists(_.relationKinds.contains("played_move_consequence")), clues(causalTrace))
    assert(causalTrace.exists(_.localFactEvidenceRefs.exists(_.contains("authority_tier:SupportedLocal"))), clues(causalTrace))
    assertEquals(slots.supportPrimary, Some("h3 restrains the ...c5 counterplay break before it lands."))
  }

  test("preview-only line consequence cannot authorize a WhyThis surface claim") {
    val primary =
      QuestionPlan(
        questionId = "q_preview_only_why_this",
        questionKind = AuthorQuestionKind.WhyThis,
        priority = 100,
        claim = "h3 keeps the checked line under control.",
        evidence = None,
        contrast = None,
        consequence = None,
        fallbackMode = QuestionPlanFallbackMode.PlannerOwned,
        strengthTier = QuestionPlanStrengthTier.Moderate,
        sourceKinds = List("move_delta"),
        admissibilityReasons = List("move_owner"),
        plannerOwnerKind = PlannerOwnerKind.MoveDelta,
        plannerSource = "move_delta"
      )
    val slots =
      MoveReviewCompressionPolicy.buildSlotsOrFallbackFromPlannerRuntime(
        quietH3Ctx,
        QuestionPlannerInputs(
          mainBundle = None,
          quietIntent = None,
          decisionFrame = CertifiedDecisionFrame(),
          decisionComparison = None,
          alternativeNarrative = None,
          truthMode = PlayerFacingTruthMode.Strategic,
          preventedPlansNow = Nil,
          pvDelta = None,
          counterfactual = None,
          practicalAssessment = None,
          opponentThreats = Nil,
          forcingThreats = Nil,
          evidenceByQuestionId = Map.empty,
          candidateEvidenceLines = Nil,
          evidenceBackedPlans = Nil,
          opponentPlan = None,
          factualFallback = None
        ),
        RankedQuestionPlans(primary = Some(primary), secondary = None, rejected = Nil),
        strategyPack = None,
        truthContract = None,
        refs = Some(refsForLine(quietH3Ctx.fen, List("h2h3"), List("h3")))
      )

    assertEquals(slots.sourceKind, MoveReviewPolishSlots.Source.ExactFactualFallback)
    assertEquals(
      MoveReviewProseContract.stripMoveHeader(slots.claim),
      "This moves the pawn to h3."
    )
  }

  test("WhatChanged coda without played-move ownership stays fallback") {
    val primary =
      QuestionPlan(
        questionId = "q_what_changed_coda_only",
        questionKind = AuthorQuestionKind.WhatChanged,
        priority = 100,
        claim = "h3 changes which reply matters next.",
        evidence =
          Some(
            QuestionPlanEvidence(
              text = "a) h3 h6 Nf3",
              purposes = List("reply_multipv"),
              sourceKinds = List("reply_multipv"),
              branchScoped = true
            )
          ),
        contrast = None,
        consequence = Some(QuestionPlanConsequence("That changes the reply order in the checked line.", QuestionPlanConsequenceBeat.WrapUp)),
        fallbackMode = QuestionPlanFallbackMode.PlannerOwned,
        strengthTier = QuestionPlanStrengthTier.Moderate,
        sourceKinds = List("decision_comparison"),
        admissibilityReasons = List("branch_line_support"),
        plannerOwnerKind = PlannerOwnerKind.DecisionTiming,
        plannerSource = "decision_comparison"
      )
    val slots =
      MoveReviewCompressionPolicy.buildSlotsOrFallbackFromPlannerRuntime(
        quietH3Ctx,
        QuestionPlannerInputs(
          mainBundle = None,
          quietIntent = None,
          decisionFrame = CertifiedDecisionFrame(),
          decisionComparison = None,
          alternativeNarrative = None,
          truthMode = PlayerFacingTruthMode.Strategic,
          preventedPlansNow = Nil,
          pvDelta = None,
          counterfactual = None,
          practicalAssessment = None,
          opponentThreats = Nil,
          forcingThreats = Nil,
          evidenceByQuestionId = Map.empty,
          candidateEvidenceLines = Nil,
          evidenceBackedPlans = Nil,
          opponentPlan = None,
          factualFallback = None
        ),
        RankedQuestionPlans(primary = Some(primary), secondary = None, rejected = Nil),
        strategyPack = None,
        truthContract = None
      )

    assertEquals(slots.sourceKind, MoveReviewPolishSlots.Source.ExactFactualFallback)
    assertEquals(
      MoveReviewProseContract.stripMoveHeader(slots.claim),
      "This moves the pawn to h3."
    )
  }

  test("WhatChanged decision comparison with concrete consequence becomes a line-consequence fact") {
    val primary =
      QuestionPlan(
        questionId = "q_what_changed_decision_comparison",
        questionKind = AuthorQuestionKind.WhatChanged,
        priority = 100,
        claim = "h3 changes which concrete resource matters next.",
        evidence = None,
        contrast = None,
        consequence = None,
        fallbackMode = QuestionPlanFallbackMode.PlannerOwned,
        strengthTier = QuestionPlanStrengthTier.Moderate,
        sourceKinds = List("decision_comparison"),
        admissibilityReasons = List("decision_comparison"),
        plannerOwnerKind = PlannerOwnerKind.DecisionTiming,
        plannerSource = "decision_comparison"
      )
    val inputs =
      QuestionPlannerInputs(
        mainBundle = None,
        quietIntent = None,
        decisionFrame = CertifiedDecisionFrame(),
        decisionComparison =
          Some(
            DecisionComparison(
              chosenMove = Some("h3"),
              engineBestMove = Some("h3"),
              engineBestScoreCp = Some(80),
              engineBestPv = List("h3", "...Qf3"),
              cpLossVsChosen = Some(80),
              deferredMove = Some("Qf3"),
              deferredReason = Some("it wins the d5 pawn by force"),
              deferredSource = Some("top_engine_move"),
              evidence = None,
              practicalAlternative = false,
              chosenMatchesBest = true
            )
          ),
        alternativeNarrative = None,
        truthMode = PlayerFacingTruthMode.Strategic,
        preventedPlansNow = Nil,
        pvDelta = None,
        counterfactual = None,
        practicalAssessment = None,
        opponentThreats = Nil,
        forcingThreats = Nil,
        evidenceByQuestionId = Map.empty,
        candidateEvidenceLines = Nil,
        evidenceBackedPlans = Nil,
        opponentPlan = None,
        factualFallback = None
      )
    val ranked = RankedQuestionPlans(primary = Some(primary), secondary = None, rejected = Nil)
    val slots =
      MoveReviewCompressionPolicy.buildSlotsOrFallbackFromPlannerRuntime(
        quietH3Ctx,
        inputs,
        ranked,
        strategyPack = None,
        truthContract = None
      )
    val causalTrace =
      MoveReviewCompressionPolicy.causalClaimTrace(
        quietH3Ctx,
        inputs,
        ranked,
        truthContract = None,
        refs = None
      )

    assertEquals(causalTrace.map(_.rejectReasons), Some(Nil))
    assertEquals(causalTrace.map(_.status), Some("accepted"))
    assertEquals(causalTrace.flatMap(_.frameIntent), Some("what_changed"))
    assertEquals(causalTrace.flatMap(_.localFactFamily), Some("line_consequence"))
    assertEquals(causalTrace.flatMap(_.localFactAuthority), Some("pv_coupled_line"))
    assertEquals(causalTrace.flatMap(_.localFactProducer), Some("planner_causal_claim"))
    assert(causalTrace.exists(_.relationKinds.contains("change_consequence")), clues(causalTrace))
    assert(
      causalTrace.exists(_.evidenceSources.contains("top_engine_move_with_concrete_consequence")),
      clues(causalTrace)
    )
    assert(
      causalTrace.exists(_.localFactEvidenceRefs.contains("evidence_source:top_engine_move_with_concrete_consequence")),
      clues(causalTrace)
    )
    assertEquals(slots.sourceKind, MoveReviewPolishSlots.Source.Planner)
    assert(slots.supportPrimary.exists(_.contains("Qf3")), clues(slots))
  }

  test("WhatChanged played-move line consequence becomes the certified surface claim") {
    val rawTemplateClaim = "Nf3 changes the structure in the checked line."
    val refs =
      refsForLine(
        exchangeLineFen,
        List("g1f3", "b8c6", "f1b5", "a7a6", "b5c6", "d7c6"),
        List("Nf3", "Nc6", "Bb5", "a6", "Bxc6", "dxc6")
      )
    val primary =
      QuestionPlan(
        questionId = "q_line_consequence_what_changed",
        questionKind = AuthorQuestionKind.WhatChanged,
        priority = 100,
        claim = rawTemplateClaim,
        evidence = None,
        contrast = None,
        consequence = None,
        fallbackMode = QuestionPlanFallbackMode.PlannerOwned,
        strengthTier = QuestionPlanStrengthTier.Moderate,
        sourceKinds = List("move_delta"),
        admissibilityReasons = List("move_attributed_change"),
        plannerOwnerKind = PlannerOwnerKind.MoveDelta,
        plannerSource = "move_delta"
      )
    val slots =
      MoveReviewCompressionPolicy.buildSlotsOrFallbackFromPlannerRuntime(
        exchangeLineCtx,
        QuestionPlannerInputs(
          mainBundle = None,
          quietIntent = None,
          decisionFrame = CertifiedDecisionFrame(),
          decisionComparison = None,
          alternativeNarrative = None,
          truthMode = PlayerFacingTruthMode.Strategic,
          preventedPlansNow = Nil,
          pvDelta = None,
          counterfactual = None,
          practicalAssessment = None,
          opponentThreats = Nil,
          forcingThreats = Nil,
          evidenceByQuestionId = Map.empty,
          candidateEvidenceLines = Nil,
          evidenceBackedPlans = Nil,
          opponentPlan = None,
          factualFallback = None
        ),
        RankedQuestionPlans(primary = Some(primary), secondary = None, rejected = Nil),
        strategyPack = None,
        truthContract = None,
        refs = Some(refs)
      )

    val claim = MoveReviewProseContract.stripMoveHeader(slots.claim)
    assertEquals(slots.sourceKind, MoveReviewPolishSlots.Source.Planner)
    assert(claim.startsWith("On the checked line"), clues(claim))
    assert(claim.contains("exchange sequence"), clues(claim))
    assert(!claim.contains(rawTemplateClaim), clues(claim))
    assertEquals(slots.paragraphPlan, List("p1=claim"))
    assert(slots.factGuardrails.exists(_.contains("relations=played_move_consequence")), clues(slots.factGuardrails))
    assert(slots.factGuardrails.exists(_.contains("change_consequence")), clues(slots.factGuardrails))
  }

  test("basic lane carries the same move review explanation used for visible slots") {
    val ctx = italianCtx
    val slots =
      MoveReviewPolishSlotsBuilder.buildOrFallback(
        ctx,
        BookStyleRenderer.validatedOutline(ctx),
        refs = Some(refsForLine(italianBeforeBc4, List("f1c4", "g8f6", "d2d3"), List("Bc4", "Nf6", "d3"))),
        strategyPack = None,
        truthContract = None
      )

    assertEquals(slots.sourceKind, MoveReviewPolishSlots.Source.BasicMoveExplanation, clues(slots))
    assert(slots.moveReviewExplanation.exists(_.source == "opening_goal"), clues(slots))
    assert(slots.moveReviewExplanation.exists(_.reasonTags.contains("review_intent:normal_development")), clues(slots))
    assert(slots.factGuardrails.exists(_ == "MoveReview review intent: normal_development"), clues(slots.factGuardrails))
    assert(slots.factGuardrails.exists(_ == "MoveReview character band: neutral"), clues(slots.factGuardrails))
    assert(slots.factGuardrails.exists(_ == "MoveReview line proof: opening_goal"), clues(slots.factGuardrails))
    assert(slots.factGuardrails.exists(_ == "MoveReview PV subject: f1c4"), clues(slots.factGuardrails))
    assert(slots.moveReviewExplanation.exists(explanation => slots.claim.contains(explanation.prose.take(24).trim)), clues(slots))
  }

  test("basic opening explanation is blocked when truth contract blocks strategic support") {
    val ctx = italianCtx
    val slots =
      MoveReviewPolishSlotsBuilder.buildOrFallback(
        ctx,
        BookStyleRenderer.validatedOutline(ctx),
        refs = Some(refsForLine(italianBeforeBc4, List("f1c4", "g8f6", "d2d3"), List("Bc4", "Nf6", "d3"))),
        strategyPack = None,
        truthContract = Some(tacticalBlunderContract(playedMove = "f1c4", bestMove = "d2d4"))
      )

    assertNotEquals(slots.sourceKind, MoveReviewPolishSlots.Source.BasicMoveExplanation, clues(slots))
    assertEquals(slots.moveReviewExplanation, None, clues(slots))
    assertEquals(
      MoveReviewProseContract.stripMoveHeader(slots.claim),
      "This puts the bishop on c4.",
      clues(slots)
    )
  }

  test("existing planner-positive fixture still outranks the new basic lane") {
    val fixture =
      MoveReviewProseGoldenFixtures.plannerRuntimeFixtures.find(_.expectedClaimFragment.nonEmpty).get
    val outline =
      BookStyleRenderer.validatedOutline(
        fixture.ctx,
        strategyPack = fixture.strategyPack,
        truthContract = fixture.truthContract
      )
    val slots =
      MoveReviewPolishSlotsBuilder.buildOrFallback(
        fixture.ctx,
        outline,
        refs = None,
        strategyPack = fixture.strategyPack,
        truthContract = fixture.truthContract
      )
    val claim = MoveReviewProseContract.stripMoveHeader(slots.claim).toLowerCase

    assertNotEquals(slots.sourceKind, "basic_move_explanation", clues(fixture.id, slots))
    assertEquals(slots.moveReviewExplanation, None, clues(fixture.id, slots))
    assert(
      claim.contains(fixture.expectedClaimFragment.get.toLowerCase),
      clues(fixture.id, claim, slots)
    )
    assertNotEquals(slots.paragraphPlan, List("p1=claim"), clues(fixture.id, slots))
  }

  test("exact factual fallback remains the surface when generic strategic fallback would have matched") {
    val ctx = quietH3Ctx.copy(
      plans = PlanTable(
        top5 = List(
          PlanRow(
            rank = 1,
            name = "OpenFilePressure",
            score = 0.92,
            evidence = List("rook doubling on open d-file"),
            supports = Nil,
            blockers = Nil,
            missingPrereqs = Nil
          )
        ),
        suppressed = Nil
      )
    )
    val outline = BookStyleRenderer.validatedOutline(ctx)
    val slots =
      MoveReviewPolishSlotsBuilder.buildOrFallback(
        ctx,
        outline,
        refs = None,
        strategyPack = None,
        truthContract = None
      )

    assertEquals(slots.sourceKind, MoveReviewPolishSlots.Source.ExactFactualFallback)
    assertEquals(
      MoveReviewProseContract.stripMoveHeader(slots.claim),
      "This moves the pawn to h3."
    )
    assertEquals(slots.paragraphPlan, List("p1=claim"))
  }

  test("removed thematic fallback leaves exact factual fallback as the fail-closed surface for truthContract risk") {
    val ctx = quietH3Ctx.copy(
      plans = PlanTable(
        top5 = List(
          PlanRow(
            rank = 1,
            name = "OpenFilePressure",
            score = 0.92,
            evidence = List("rook doubling on open d-file"),
            supports = Nil,
            blockers = Nil,
            missingPrereqs = Nil
          )
        ),
        suppressed = Nil
      )
    )
    val outline = BookStyleRenderer.validatedOutline(ctx)
    val blunderContract = tacticalBlunderContract()

    val slots =
      MoveReviewPolishSlotsBuilder.buildOrFallback(
        ctx,
        outline,
        refs = None,
        strategyPack = None,
        truthContract = Some(blunderContract)
      )

    assertNotEquals(slots.sourceKind, "thematic_fallback")
    assertEquals(
      MoveReviewProseContract.stripMoveHeader(slots.claim),
      "This moves the pawn to h3."
    )

    val missedWinContract =
      blunderContract.copy(
        truthClass = DecisiveTruthClass.MissedWin,
        cpLoss = 220,
        swingSeverity = 220,
        reasonFamily = DecisiveReasonKind.QuietTechnicalMove,
        failureMode = FailureInterpretationMode.NoClearPlan,
        failureIntentConfidence = 0.0,
        failureInterpretationAllowed = false
      )

    val missedWinSlots =
      MoveReviewPolishSlotsBuilder.buildOrFallback(
        ctx,
        outline,
        refs = None,
        strategyPack = None,
        truthContract = Some(missedWinContract)
      )

    assertNotEquals(missedWinSlots.sourceKind, "thematic_fallback")
    assertEquals(
      MoveReviewProseContract.stripMoveHeader(missedWinSlots.claim),
      "This moves the pawn to h3."
    )
  }

  test("removed thematic fallback leaves exact factual fallback when planner inputs carry tactical ownership") {
    val ctx = quietH3Ctx.copy(
      plans = PlanTable(
        top5 = List(
          PlanRow(
            rank = 1,
            name = "OpenFilePressure",
            score = 0.92,
            evidence = List("rook doubling on open d-file"),
            supports = Nil,
            blockers = Nil,
            missingPrereqs = Nil
          )
        ),
        suppressed = Nil
      )
    )
    val tacticalInputs =
      QuestionPlannerInputs(
        mainBundle = None,
        quietIntent = None,
        decisionFrame = CertifiedDecisionFrame(),
        decisionComparison = None,
        alternativeNarrative = None,
        truthMode = PlayerFacingTruthMode.Tactical,
        preventedPlansNow = Nil,
        pvDelta = None,
        counterfactual = None,
        practicalAssessment = None,
        opponentThreats = Nil,
        forcingThreats = Nil,
        evidenceByQuestionId = Map.empty,
        candidateEvidenceLines = Nil,
        evidenceBackedPlans = Nil,
        opponentPlan = None,
        factualFallback = None
      )
    val tacticalClaim =
      MainPathScopedClaim(
        scope = PlayerFacingClaimScope.MoveLocal,
        mode = PlayerFacingTruthMode.Tactical,
        deltaClass = None,
        claimText = "This move has a concrete tactical problem.",
        anchorTerms = List("h3"),
        evidenceLines = Nil,
        sourceKind = "tactical_contract",
        tacticalOwnership = Some("current_move_tactical")
      )

    List(
      tacticalInputs,
      tacticalInputs.copy(
        truthMode = PlayerFacingTruthMode.Strategic,
        mainBundle = Some(MainPathClaimBundle(mainClaim = Some(tacticalClaim), lineScopedClaim = None))
      )
    ).foreach { inputs =>
      val slots =
        MoveReviewCompressionPolicy.buildSlotsOrFallbackFromPlannerRuntime(
          ctx,
          inputs,
          RankedQuestionPlans(primary = None, secondary = None, rejected = Nil),
          strategyPack = None,
          truthContract = None
        )

      assertNotEquals(slots.sourceKind, "thematic_fallback", clues(slots))
      assertEquals(
        MoveReviewProseContract.stripMoveHeader(slots.claim),
        "This moves the pawn to h3."
      )
    }
  }

  test("removed thematic fallback omits generic strategic prose when exact factual fallback is closed") {
    val ctx = quietH3Ctx.copy(
      playedMove = None,
      playedSan = None,
      plans = PlanTable(
        top5 = List(
          PlanRow(
            rank = 1,
            name = "OpenFilePressure",
            score = 0.92,
            evidence = List("rook doubling on open d-file"),
            supports = Nil,
            blockers = Nil,
            missingPrereqs = Nil
          )
        ),
        suppressed = Nil
      )
    )
    val outline = BookStyleRenderer.validatedOutline(ctx)
    val inaccuracyContract = DecisiveTruthContract(
      playedMove = None,
      verifiedBestMove = Some("e2e4"),
      truthClass = DecisiveTruthClass.Inaccuracy,
      cpLoss = 45,
      swingSeverity = 45,
      reasonFamily = DecisiveReasonKind.QuietTechnicalMove,
      allowConcreteBenchmark = false,
      chosenMatchesBest = false,
      compensationAllowed = false,
      truthPhase = None,
      ownershipRole = TruthOwnershipRole.NoneRole,
      visibilityRole = TruthVisibilityRole.SupportingVisible,
      surfaceMode = TruthSurfaceMode.Neutral,
      exemplarRole = TruthExemplarRole.NonExemplar,
      surfacedMoveOwnsTruth = false,
      verifiedPayoffAnchor = None,
      compensationProseAllowed = false,
      benchmarkProseAllowed = false,
      investmentTruthChainKey = None,
      maintenanceExemplarCandidate = false,
      benchmarkCriticalMove = false,
      failureMode = FailureInterpretationMode.NoClearPlan,
      failureIntentConfidence = 0.0,
      failureIntentAnchor = None,
      failureInterpretationAllowed = false
    )

    val slots =
      MoveReviewPolishSlotsBuilder.buildOrFallback(
        ctx,
        outline,
        refs = None,
        strategyPack = None,
        truthContract = Some(inaccuracyContract)
      )

    assertNotEquals(slots.sourceKind, "thematic_fallback")
    assertEquals(MoveReviewProseContract.stripMoveHeader(slots.claim), "")
    assertEquals(slots.supportPrimary, None)
    assertEquals(slots.supportSecondary, None)
  }

  private def supportedNeutralizePacketForPromotion: PlayerFacingClaimPacket =
    PlayerFacingClaimPacket(
      claimGate =
        PlanEvidenceEvaluator.ClaimCertification(
          certificateStatus = PlayerFacingCertificateStatus.Valid,
          quantifier = PlayerFacingClaimQuantifier.BestResponse,
          attributionGrade = PlayerFacingClaimAttributionGrade.AnchoredButShared,
          stabilityGrade = PlayerFacingClaimStabilityGrade.Stable,
          provenanceClass = PlayerFacingClaimProvenanceClass.ProbeBacked,
          ontologyFamily = PlayerFacingClaimOntologyKind.CounterplayRestraint
        ),
      proofSource = ProofSourceId.CounterplayAxisSuppression.wireKey,
      proofFamily = ProofFamilyId.NeutralizeKeyBreak.wireKey,
      scope = PlayerFacingPacketScope.MoveLocal,
      triggerKind = PlanTaxonomy.PlanKind.BreakPrevention.id,
      anchorTerms = List("...c5"),
      bestDefenseMove = Some("c5"),
      bestDefenseBranchKey = Some("e4d5|c6d5"),
      sameBranchState = PlayerFacingSameBranchState.Proven,
      persistence = PlayerFacingClaimPersistence.Stable,
      proofPathWitness =
        PlayerFacingProofPathWitness(
          ownerSeedTerms = List("neutralize_key_break", "...c5"),
          continuationTerms = List("counterplay_axis_suppression", "e4d5", "c6d5"),
          structureTransitionTerms = List("...c5"),
          exactSliceProof = Some(PlayerFacingExactSliceProof.CounterplayAxisSuppression("...c5"))
        ),
      fallbackMode = PlayerFacingClaimFallbackMode.WeakMain
    )
