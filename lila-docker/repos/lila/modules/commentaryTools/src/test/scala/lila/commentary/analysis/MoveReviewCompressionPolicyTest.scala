package lila.commentary.analysis

import lila.commentary.*
import lila.commentary.model.*
import lila.commentary.model.authoring.AuthorQuestionKind
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
        sourceKinds = List("truth_contract"),
        admissibilityReasons = List("timing_owner"),
        plannerOwnerKind = PlannerOwnerKind.ForcingDefense,
        plannerSource = "truth_contract",
        timingWitness = Some(
          QuestionPlanTimingWitness(
            proofFamily = "only_move_defense",
            source = "truth_contract",
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
    assertEquals(typed.admission.map(_.authority), Some(MoveReviewLocalFact.Authority.TruthContract))
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
        sourceKinds = List("truth_contract"),
        plannerOwnerKind = PlannerOwnerKind.ForcingDefense,
        plannerSource = "truth_contract",
        timingWitness = Some(
          QuestionPlanTimingWitness(
            proofFamily = "only_move_defense",
            source = "truth_contract",
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
    assertEquals(decision.claim.flatMap(_.localFact.map(_.family)), Some(MoveReviewLocalFact.Family.Timing), clues(decision))
    assertEquals(decision.claim.flatMap(_.localFact.map(_.lineBinding)), Some(MoveReviewLocalFact.LineBinding.BranchScoped), clues(decision))
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
        sourceKinds = List("truth_contract"),
        admissibilityReasons = List("timing_owner"),
        plannerOwnerKind = PlannerOwnerKind.ForcingDefense,
        plannerSource = "truth_contract"
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
    assertEquals(causalTrace.map(_.rejectReasons), Some(List("causal_relation_missing")))
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
    assert(slots.factGuardrails.exists(_.contains("local_fact=timing/truth_contract")), clues(slots.factGuardrails))
    assertEquals(causalTrace.flatMap(_.localFactFamily), Some("timing"))
    assertEquals(causalTrace.flatMap(_.localFactAuthority), Some("truth_contract"))
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

    assertNotEquals(slots.sourceKind, MoveReviewPolishSlots.Source.ThematicFallback)
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

    assertNotEquals(missedWinSlots.sourceKind, MoveReviewPolishSlots.Source.ThematicFallback)
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
        claimText = "This is a tactical failure.",
        anchorTerms = List("h3"),
        evidenceLines = Nil,
        sourceKind = "truth_contract",
        tacticalOwnership = Some("tactical_failure")
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

      assertNotEquals(slots.sourceKind, MoveReviewPolishSlots.Source.ThematicFallback, clues(slots))
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

    assertNotEquals(slots.sourceKind, MoveReviewPolishSlots.Source.ThematicFallback)
    assertEquals(MoveReviewProseContract.stripMoveHeader(slots.claim), "")
    assertEquals(slots.supportPrimary, None)
    assertEquals(slots.supportSecondary, None)
  }
