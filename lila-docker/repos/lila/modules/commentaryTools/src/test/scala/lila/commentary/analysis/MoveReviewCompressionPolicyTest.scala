package lila.commentary.analysis

import chess.{ Color, Knight, Queen, Rook, Square }
import lila.commentary.*
import lila.commentary.analysis.practical.ContrastiveSupportAdmissibility
import lila.commentary.analysis.semantic.RelationSurfaceRowKind
import lila.commentary.analysis.semantic.StrategicObservationIds.{ ProofFamilyId, ProofSourceId }
import lila.commentary.model.*
import lila.commentary.model.authoring.{ AuthorQuestion, AuthorQuestionKind, PlanHypothesis, PlanViability }
import lila.commentary.model.strategic.{ CounterfactualMatch, EngineEvidence, VariationLine }
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
    truthContract(
      playedMove = Some(playedMove),
      verifiedBestMove = Some(bestMove),
      truthClass = DecisiveTruthClass.Blunder,
      cpLoss = 300,
      swingSeverity = 300,
      reasonFamily = DecisiveReasonKind.TacticalRefutation,
      ownershipRole = TruthOwnershipRole.BlunderOwner,
      surfaceMode = TruthSurfaceMode.FailureExplain,
      surfacedMoveOwnsTruth = true,
      failureMode = FailureInterpretationMode.TacticalRefutation,
      failureIntentConfidence = 0.9,
      failureInterpretationAllowed = true
    )

  private def truthContract(
      playedMove: Option[String],
      verifiedBestMove: Option[String],
      truthClass: DecisiveTruthClass,
      cpLoss: Int,
      reasonFamily: DecisiveReasonKind,
      swingSeverity: Int = 0,
      allowConcreteBenchmark: Boolean = false,
      chosenMatchesBest: Boolean = false,
      ownershipRole: TruthOwnershipRole = TruthOwnershipRole.NoneRole,
      visibilityRole: TruthVisibilityRole = TruthVisibilityRole.PrimaryVisible,
      surfaceMode: TruthSurfaceMode = TruthSurfaceMode.Neutral,
      exemplarRole: TruthExemplarRole = TruthExemplarRole.NonExemplar,
      surfacedMoveOwnsTruth: Boolean = false,
      benchmarkCriticalMove: Boolean = false,
      failureMode: FailureInterpretationMode = FailureInterpretationMode.NoClearPlan,
      failureIntentConfidence: Double = 0.0,
      failureInterpretationAllowed: Boolean = false
  ): DecisiveTruthContract =
    DecisiveTruthContract(
      playedMove = playedMove,
      verifiedBestMove = verifiedBestMove,
      truthClass = truthClass,
      cpLoss = cpLoss,
      swingSeverity = swingSeverity,
      reasonFamily = reasonFamily,
      allowConcreteBenchmark = allowConcreteBenchmark,
      chosenMatchesBest = chosenMatchesBest,
      compensationAllowed = false,
      truthPhase = None,
      ownershipRole = ownershipRole,
      visibilityRole = visibilityRole,
      surfaceMode = surfaceMode,
      exemplarRole = exemplarRole,
      surfacedMoveOwnsTruth = surfacedMoveOwnsTruth,
      verifiedPayoffAnchor = None,
      compensationProseAllowed = false,
      benchmarkProseAllowed = false,
      investmentTruthChainKey = None,
      maintenanceExemplarCandidate = false,
      benchmarkCriticalMove = benchmarkCriticalMove,
      failureMode = failureMode,
      failureIntentConfidence = failureIntentConfidence,
      failureIntentAnchor = None,
      failureInterpretationAllowed = failureInterpretationAllowed
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

  test("live narrative rewrite does not turn source practical prose into easy-handling authority") {
    val anchored =
      LiveNarrativeCompressionCore.rewritePlayerLanguage(
        "More important than the nominal evaluation is that the move creates an easier practical task through rook activity."
      )
    val generic =
      LiveNarrativeCompressionCore.rewritePlayerLanguage(
        "More important than the nominal evaluation is that the move creates a comfortable practical task."
      )

    assertEquals(anchored, "The supporting detail is rook activity.")
    assert(!anchored.toLowerCase.contains("easier"), clues(anchored))
    assert(!anchored.toLowerCase.contains("nominal evaluation"), clues(anchored))
    assertEquals(generic, "The position still needs concrete follow-up.")
    assert(LiveNarrativeCompressionCore.isLowValueNarrativeSentence(generic), clues(generic))
    assert(LiveNarrativeCompressionCore.playerLanguageHits("The move is easier to handle because the pieces have more room.").nonEmpty)
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

  test("MoveReviewLocalFact keeps role-aware alternative comparison out of generic planner producer") {
    val alternativePlan =
      QuestionPlan(
        questionId = "q_role_aware_alternative",
        questionKind = AuthorQuestionKind.WhyThis,
        priority = 100,
        claim = "The engine-best branch reaches a concrete result while the played branch does not.",
        evidence = None,
        contrast = None,
        consequence = None,
        fallbackMode = QuestionPlanFallbackMode.PlannerOwned,
        strengthTier = QuestionPlanStrengthTier.Strong,
        sourceKinds = List(DecisionComparisonComparativeSupport.RoleAwareLineConsequenceSource),
        admissibilityReasons = List("role_aware_line_consequence"),
        plannerOwnerKind = PlannerOwnerKind.AlternativeComparison,
        plannerSource = "decision_comparison"
      )

    val decision =
      MoveReviewLocalFact.admitPlanner(
        alternativePlan,
        evidenceKinds = List("branch_line"),
        relationKinds = List("alternative_contrast"),
        lineConsequenceBacked = true
      )

    assertEquals(decision.admission.map(_.family), Some(MoveReviewLocalFact.Family.LineConsequence))
    assertEquals(decision.admission.map(_.authority), Some(MoveReviewLocalFact.Authority.AlternativeComparison))
    assertEquals(decision.admission.map(_.producer), Some(MoveReviewLocalFact.Producer.AlternativeComparison))
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

  test("forced-reply contrast becomes typed timing fact only when reply evidence is explicit") {
    val plan =
      QuestionPlan(
        questionId = "q_forced_reply_timing",
        questionKind = AuthorQuestionKind.WhyNow,
        priority = 100,
        claim = "The timing matters because the defensive reply has to be handled now.",
        evidence = None,
        contrast = None,
        consequence = None,
        fallbackMode = QuestionPlanFallbackMode.PlannerOwned,
        strengthTier = QuestionPlanStrengthTier.Strong,
        sourceKinds = List("threat"),
        admissibilityReasons = List("urgent_threat"),
        plannerOwnerKind = PlannerOwnerKind.ForcingDefense,
        plannerSource = "threat"
      )
    def decision(forced: Boolean): MoveReviewCausalClaim.Decision =
      MoveReviewCausalClaim.admit(
        MoveReviewCausalClaim.candidate(
          plan = plan,
          renderedClaim = plan.claim,
          contrastAdmissible = true,
          contrastSourceKind = Some(ContrastiveSupportAdmissibility.SourceKind.ExplicitReplyLoss),
          contrastAnchor = Some("Qe6"),
          contrastForcedReply = forced,
          contrastEvidenceRefs = List("reply_anchor:Qe6", "reply_defense_count:1"),
          contrastGuardrails = List(if forced then "forced_reply_unique" else "forced_reply_non_unique"),
          supportPrimary = Some(if forced then "If delayed, Qe6 is the reply." else "If delayed, Qe6 is one defensive reply."),
          supportSecondary = None,
          tension = None,
          evidenceHook = None,
          coda = None,
          surfaceConsequence = None,
          lineConsequenceEvidence = None,
          pvCoupledPlanSupport = None
        )
      )

    val forced = decision(forced = true)
    val nonUnique = decision(forced = false)

    assertEquals(forced.claim.map(_.relationKinds), Some(List(MoveReviewCausalClaim.RelationKind.TimingConstraint)), clues(forced))
    assertEquals(forced.claim.flatMap(_.localFact.map(_.family)), Some(MoveReviewLocalFact.Family.Timing), clues(forced))
    assertEquals(forced.claim.flatMap(_.localFact.map(_.authority)), Some(MoveReviewLocalFact.Authority.ForcedReply), clues(forced))
    assertEquals(forced.claim.flatMap(_.localFact.map(_.producer)), Some(MoveReviewLocalFact.Producer.ForcedReply), clues(forced))
    assertEquals(forced.frame.map(_.surfaceContract.mayUseForced), Some(true), clues(forced))
    assert(forced.claim.exists(_.surfaceContract.guardrails.contains("surface_forced=true")), clues(forced))
    assert(forced.claim.flatMap(_.localFact).exists(_.guardrails.contains("forced_reply_unique")), clues(forced))

    assertEquals(nonUnique.claim.flatMap(_.localFact.map(_.producer)), Some(MoveReviewLocalFact.Producer.ForcedReply), clues(nonUnique))
    assertEquals(nonUnique.frame.map(_.surfaceContract.mayUseForced), Some(false), clues(nonUnique))
    assert(nonUnique.claim.exists(_.surfaceContract.guardrails.contains("surface_forced=false")), clues(nonUnique))
    assert(nonUnique.claim.flatMap(_.localFact).exists(_.guardrails.contains("forced_reply_non_unique")), clues(nonUnique))
  }

  test("forced-reply contrast can own WhatMustBeStopped as a defense fact") {
    val plan =
      QuestionPlan(
        questionId = "q_forced_reply_defense",
        questionKind = AuthorQuestionKind.WhatMustBeStopped,
        priority = 100,
        claim = "The move has to stop the immediate material reply.",
        evidence = None,
        contrast = None,
        consequence = None,
        fallbackMode = QuestionPlanFallbackMode.PlannerOwned,
        strengthTier = QuestionPlanStrengthTier.Strong,
        sourceKinds = List("threat"),
        admissibilityReasons = List("urgent_threat"),
        plannerOwnerKind = PlannerOwnerKind.ForcingDefense,
        plannerSource = "threat"
      )
    val decision =
      MoveReviewCausalClaim.admit(
        MoveReviewCausalClaim.candidate(
          plan = plan,
          renderedClaim = plan.claim,
          contrastAdmissible = true,
          contrastSourceKind = Some(ContrastiveSupportAdmissibility.SourceKind.ExplicitReplyLoss),
          contrastAnchor = Some("Qe6"),
          contrastForcedReply = true,
          contrastEvidenceRefs = List("reply_anchor:Qe6", "reply_defense_count:1", "threat_kind:material"),
          contrastGuardrails = List("forced_reply_unique", "threat_kind:material"),
          supportPrimary = Some("If delayed, Qe6 is the reply."),
          supportSecondary = None,
          tension = None,
          evidenceHook = None,
          coda = None,
          surfaceConsequence = None,
          lineConsequenceEvidence = None,
          pvCoupledPlanSupport = None
        )
      )

    assertEquals(decision.rejectReasons, Nil, clues(decision))
    assertEquals(decision.claim.map(_.relationKinds), Some(List(MoveReviewCausalClaim.RelationKind.DefensiveResource)), clues(decision))
    assertEquals(decision.claim.flatMap(_.localFact.map(_.family)), Some(MoveReviewLocalFact.Family.Defense), clues(decision))
    assertEquals(decision.claim.flatMap(_.localFact.map(_.authority)), Some(MoveReviewLocalFact.Authority.ForcedReply), clues(decision))
    assertEquals(decision.claim.flatMap(_.localFact.map(_.producer)), Some(MoveReviewLocalFact.Producer.ForcedReply), clues(decision))
    assert(decision.claim.flatMap(_.localFact).exists(_.evidenceRefs.contains("threat_kind:material")), clues(decision))
  }

  test("forced-line truth local fact becomes CausalFrame typed evidence") {
    val plan =
      QuestionPlan(
        questionId = "q_forced_line_truth",
        questionKind = AuthorQuestionKind.WhyThis,
        priority = 100,
        claim = "Bxh7+ is tied to a confirmed Greek Gift Sacrifice sequence.",
        evidence = None,
        contrast = None,
        consequence = None,
        fallbackMode = QuestionPlanFallbackMode.PlannerOwned,
        strengthTier = QuestionPlanStrengthTier.Strong,
        sourceKinds = List("forced_line_truth"),
        admissibilityReasons = List("typed_local_fact"),
        plannerOwnerKind = PlannerOwnerKind.ConcreteTactical,
        plannerSource = "forced_line_truth"
      )
    val localFact =
      MoveReviewLocalFact.admitted(
        MoveReviewLocalFact.Candidate(
          family = MoveReviewLocalFact.Family.Threat,
          source = MoveReviewLocalFact.Source.PvCoupledLine,
          producer = MoveReviewLocalFact.Producer.ForcedLineTruth,
          subject = MoveReviewLocalFact.Subject.Target,
          strictFallbackCandidate = true,
          anchors = List(MoveReviewLocalFact.Anchor("forced_line_theme", "greek_gift")),
          lineBinding = MoveReviewLocalFact.LineBinding.PvCoupled,
          evidenceRefs = List("forced_line_theme:greek_gift"),
          guardrails = List("forced_line_truth_verified", "played_move_first", "pv_coupled")
        )
      )
    val result =
      MoveReviewExplanationBuilder.Result(
        explanation =
          MoveReviewExplanation(
            title = "Bxh7+ has a confirmed Greek Gift Sacrifice sequence",
            prose = "Bxh7+ is tied to the confirmed Greek Gift Sacrifice sequence.",
            source = "forced_line_truth"
          ),
        localFact = localFact
      )
    val decision =
      MoveReviewCausalClaim.admit(
        MoveReviewCausalClaim.candidate(
          plan = plan,
          renderedClaim = plan.claim,
          contrastAdmissible = false,
          contrastSourceKind = None,
          contrastAnchor = None,
          contrastForcedReply = false,
          contrastEvidenceRefs = Nil,
          contrastGuardrails = Nil,
          supportPrimary = None,
          supportSecondary = None,
          tension = None,
          evidenceHook = None,
          coda = None,
          surfaceConsequence = None,
          lineConsequenceEvidence = None,
          pvCoupledPlanSupport = None,
          localFactResult = Some(result)
        )
      )

    assertEquals(decision.rejectReasons, Nil, clues(decision))
    assertEquals(decision.evidences.map(_.source), List(MoveReviewCausalClaim.EvidenceSource.ForcedLineTruth), clues(decision))
    assertEquals(decision.claim.map(_.relationKinds), Some(List(MoveReviewCausalClaim.RelationKind.PlayedMoveConsequence)), clues(decision))
    assertEquals(decision.claim.flatMap(_.localFact.map(_.producer)), Some(MoveReviewLocalFact.Producer.ForcedLineTruth), clues(decision))
    assertEquals(decision.claim.flatMap(_.localFact.map(_.family)), Some(MoveReviewLocalFact.Family.Threat), clues(decision))
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

  test("WhyNow move-order relation local fact authorizes timing constraint surface") {
    val localFact =
      MoveReviewLocalFact.admitted(
        MoveReviewLocalFact.Candidate(
          family = MoveReviewLocalFact.Family.Timing,
          source = MoveReviewLocalFact.Source.PvCoupledLine,
          producer = MoveReviewLocalFact.Producer.RelationWitness,
          subject = MoveReviewLocalFact.Subject.Target,
          strictFallbackCandidate = true,
          anchors = List(MoveReviewLocalFact.Anchor("relation_target", "d4")),
          lineBinding = MoveReviewLocalFact.LineBinding.PvCoupled,
          evidenceRefs = List("relation_kind:zwischenzug", "relation_fact:zwischenzug_relation_witness"),
          guardrails = List(
            "relation_witness_typed_details",
            "fen_validated_line_replayed",
            "played_move_first",
            "relation_kind:zwischenzug"
          ),
          relationSurface = Some(RelationSurfaceRowKind.MoveOrder)
        )
      )
    val relationResult =
      MoveReviewExplanationBuilder.Result(
        explanation =
          MoveReviewExplanation(
            title = "h3 has checked move order",
            prose = "h3 is tied to a checked zwischenzug relation in the PV.",
            source = "relation_witness"
          ),
        localFact = localFact
      )
    val primary =
      QuestionPlan(
        questionId = "q_move_order_timing",
        questionKind = AuthorQuestionKind.WhyNow,
        priority = 100,
        claim = relationResult.explanation.prose,
        evidence = None,
        contrast = None,
        consequence = None,
        fallbackMode = QuestionPlanFallbackMode.PlannerOwned,
        strengthTier = QuestionPlanStrengthTier.Strong,
        sourceKinds = List("timing_proof", "relation_witness"),
        admissibilityReasons = List("timing_owner", "delay_sensitive_proof"),
        plannerOwnerKind = PlannerOwnerKind.DecisionTiming,
        plannerSource = "relation_witness"
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
        factualFallback = None,
        localFactResult = Some(relationResult)
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
    assert(slots.factGuardrails.exists(_.contains("relations=timing_constraint")), clues(slots.factGuardrails))
    assertEquals(causalTrace.map(_.status), Some("accepted"))
    assertEquals(causalTrace.map(_.relationKinds), Some(List("timing_constraint")))
    assertEquals(causalTrace.flatMap(_.localFactFamily), Some("timing"))
    assertEquals(causalTrace.flatMap(_.localFactAuthority), Some("pv_coupled_line"))
    assertEquals(causalTrace.flatMap(_.localFactProducer), Some("relation_witness"))
    assert(causalTrace.exists(_.evidenceSources.contains("relation_witness")), clues(causalTrace))
  }

  test("actual Bg6 only-move line consequence does not re-add generic only-move support") {
    val fen = "1r1q1rk1/ppp2ppp/2n1pb2/3p1b2/3P2P1/1QP1PN1P/PP1N1P2/R3KB1R b KQ - 0 11"
    val ctx =
      quietH3Ctx.copy(
        fen = fen,
        header = ContextHeader("Middlegame", "Normal", "OnlyMove", "High", "MoveReview"),
        ply = 22,
        playedMove = Some("f5g6"),
        playedSan = Some("Bg6"),
        summary = NarrativeSummary("actual Bg6 only move", None, "OnlyMove", "Maintain", "0.00"),
        phase = PhaseContext("Middlegame", "actual Bg6 only move")
      )
    val refs =
      refsForLine(
        fen,
        List("f5g6", "h3h4", "h7h5", "g4h5", "g6h5", "b3c2", "g7g6"),
        List("Bg6", "h4", "h5", "gxh5", "Bxh5", "Qc2", "g6")
      )
    val claim =
      "The timing matters now because the checked line reaches an exchange sequence after gxh5. " +
        "The decision is about the resulting structure: White with an isolated pawn on h4."
    val primary =
      QuestionPlan(
        questionId = "q_bg6_only_move_line_context_surface",
        questionKind = AuthorQuestionKind.WhyNow,
        priority = 100,
        claim = claim,
        evidence = None,
        contrast = None,
        consequence = None,
        fallbackMode = QuestionPlanFallbackMode.PlannerOwned,
        strengthTier = QuestionPlanStrengthTier.Strong,
        sourceKinds = List("only_move_defense", "line_consequence", "line_consequence_kind:ExchangeSequence"),
        admissibilityReasons = List("only_move_supported_local_context", "only_move_line_consequence_context"),
        plannerOwnerKind = PlannerOwnerKind.ForcingDefense,
        plannerSource = "only_move_defense"
      )
    val inputs =
      QuestionPlannerInputs(
        mainBundle = None,
        quietIntent = None,
        decisionFrame = CertifiedDecisionFrame(),
        decisionComparison =
          Some(
            DecisionComparison(
              chosenMove = Some("Bg6"),
              engineBestMove = Some("Bg6"),
              engineBestScoreCp = Some(-35),
              engineBestPv = List("Bg6", "h4", "h5", "gxh5"),
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
    val contract =
      truthContract(
        playedMove = Some("f5g6"),
        verifiedBestMove = Some("f5g6"),
        truthClass = DecisiveTruthClass.Best,
        cpLoss = 0,
        reasonFamily = DecisiveReasonKind.OnlyMoveDefense,
        allowConcreteBenchmark = true,
        chosenMatchesBest = true,
        exemplarRole = TruthExemplarRole.VerifiedExemplar,
        surfacedMoveOwnsTruth = true,
        benchmarkCriticalMove = true
      )
    val ranked = RankedQuestionPlans(primary = Some(primary), secondary = None, rejected = Nil)
    val slots =
      MoveReviewCompressionPolicy.buildSlotsOrFallbackFromPlannerRuntime(
        ctx,
        inputs,
        ranked,
        strategyPack = None,
        truthContract = Some(contract),
        refs = Some(refs)
      )
    val causalTrace =
      MoveReviewCompressionPolicy.causalClaimTrace(
        ctx,
        inputs,
        ranked,
        truthContract = Some(contract),
        refs = Some(refs)
      )

    val rendered = List(Some(slots.claim), slots.supportPrimary, slots.supportSecondary, slots.tension, slots.evidenceHook, slots.coda).flatten
    assertEquals(slots.sourceKind, MoveReviewPolishSlots.Source.Planner)
    assert(rendered.exists(_.contains("gxh5")), clues(slots))
    assert(rendered.exists(_.contains("isolated pawn on h4")), clues(slots))
    assert(rendered.forall(!_.contains("Only the played move still keeps the position together now")), clues(rendered))
    assert(causalTrace.exists(_.evidenceSources.contains("line_consequence_surface")), clues(causalTrace))
  }

  test("planner-owned typed local fact preserves reviewed-move short-line support") {
    val localFact =
      MoveReviewLocalFact.admitted(
        MoveReviewLocalFact.Candidate(
          family = MoveReviewLocalFact.Family.Pressure,
          source = MoveReviewLocalFact.Source.CertifiedStrategy,
          producer = MoveReviewLocalFact.Producer.CertifiedStrategyDelta,
          subject = MoveReviewLocalFact.Subject.Target,
          strictFallbackCandidate = false,
          anchors = List(MoveReviewLocalFact.Anchor("target", "e4")),
          lineBinding = MoveReviewLocalFact.LineBinding.PvCoupled,
          evidenceRefs = List("strategy_ref:source:line_control_features"),
          guardrails = List("practical_position_support", "move_touches_strategy_anchor", "pv_coupled")
        )
      )
    val result =
      MoveReviewExplanationBuilder.Result(
        explanation =
          MoveReviewExplanation(
            title = "h3 has checked local pressure",
            prose = "h3 is tied to checked local pressure around e4.",
            source = "practical_position_support"
          ),
        localFact = localFact
      )
    val primary =
      QuestionPlan(
        questionId = "q_typed_fact_short_line",
        questionKind = AuthorQuestionKind.WhyThis,
        priority = 100,
        claim = result.explanation.prose,
        evidence = None,
        contrast = None,
        consequence = None,
        fallbackMode = QuestionPlanFallbackMode.PlannerOwned,
        strengthTier = QuestionPlanStrengthTier.Moderate,
        sourceKinds = List("typed_local_fact"),
        admissibilityReasons = List("move_attributed_change"),
        plannerOwnerKind = PlannerOwnerKind.MoveDelta,
        plannerSource = "typed_local_fact"
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
        candidateEvidenceLines = Nil,
        evidenceBackedPlans = Nil,
        opponentPlan = None,
        factualFallback = None,
        localFactResult = Some(result)
      )
    val refs = refsForLine(quietH3Ctx.fen, List("h2h3", "a7a6", "g1f3"), List("h3", "a6", "Nf3"))
    val ranked = RankedQuestionPlans(primary = Some(primary), secondary = None, rejected = Nil)
    val slots =
      MoveReviewCompressionPolicy.buildSlotsOrFallbackFromPlannerRuntime(
        quietH3Ctx,
        inputs,
        ranked,
        strategyPack = None,
        truthContract = None,
        refs = Some(refs)
      )
    val causalTrace =
      MoveReviewCompressionPolicy.causalClaimTrace(
        quietH3Ctx,
        inputs,
        ranked,
        truthContract = None,
        refs = Some(refs)
      )

    assertEquals(slots.sourceKind, MoveReviewPolishSlots.Source.Planner)
    assertEquals(slots.evidenceHook, Some("Short line: h3 a6 Nf3."))
    assert(slots.factGuardrails.exists(_.contains("support_embedded=true")), clues(slots.factGuardrails))
    assertEquals(causalTrace.map(_.status), Some("accepted"))
    assert(causalTrace.exists(_.evidenceSources.contains("typed_local_fact")), clues(causalTrace))
    assert(causalTrace.exists(_.evidenceSources.contains("branch_line")), clues(causalTrace))
    assert(causalTrace.exists(_.relationKinds.contains("played_move_consequence")), clues(causalTrace))
  }

  test("line-bound local fact evidence hook uses its checked line id before planner branch text") {
    val localFact =
      MoveReviewLocalFact.admitted(
        MoveReviewLocalFact.Candidate(
          family = MoveReviewLocalFact.Family.LineConsequence,
          source = MoveReviewLocalFact.Source.PvCoupledLine,
          producer = MoveReviewLocalFact.Producer.LineConsequence,
          subject = MoveReviewLocalFact.Subject.PlayedMove,
          strictFallbackCandidate = true,
          lineBinding = MoveReviewLocalFact.LineBinding.PvCoupled,
          evidenceRefs = List("line_consequence_line_id:line_04"),
          guardrails = List("line_consequence_surface", "played_move_first")
        )
      )
    val result =
      MoveReviewExplanationBuilder.Result(
        explanation =
          MoveReviewExplanation(
            title = "Nf3 has a checked exchange sequence",
            prose = "Nf3 is tied to the checked exchange sequence.",
            source = "line_consequence"
          ),
        localFact = localFact
      )
    val primary =
      QuestionPlan(
        questionId = "q_line_fact_checked_line_id",
        questionKind = AuthorQuestionKind.WhyThis,
        priority = 100,
        claim = result.explanation.prose,
        evidence =
          Some(
            QuestionPlanEvidence(
              text = "a) Qxb2 hxg4 Qxa1+",
              purposes = List("author_evidence"),
              sourceKinds = List("author_evidence"),
              branchScoped = true
            )
          ),
        contrast = None,
        consequence = None,
        fallbackMode = QuestionPlanFallbackMode.PlannerOwned,
        strengthTier = QuestionPlanStrengthTier.Moderate,
        sourceKinds = List("typed_local_fact", "line_consequence"),
        admissibilityReasons = List("move_attributed_change"),
        plannerOwnerKind = PlannerOwnerKind.LineConsequence,
        plannerSource = "line_consequence"
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
        candidateEvidenceLines = Nil,
        evidenceBackedPlans = Nil,
        opponentPlan = None,
        factualFallback = None,
        localFactResult = Some(result)
      )
    val checkedLine =
      refsForLine(
        exchangeLineFen,
        List("g1f3", "b8c6", "f1b5", "a7a6", "b5c6"),
        List("Nf3", "Nc6", "Bb5", "a6", "Bxc6")
      ).variations.head.copy(lineId = "line_04")
    val refs =
      MoveReviewRefs(
        startFen = exchangeLineFen,
        startPly = NarrativeUtils.plyFromFen(exchangeLineFen).map(_ + 1).getOrElse(1),
        variations = List(checkedLine)
      )
    val ranked = RankedQuestionPlans(primary = Some(primary), secondary = None, rejected = Nil)
    val slots =
      MoveReviewCompressionPolicy.buildSlotsOrFallbackFromPlannerRuntime(
        exchangeLineCtx,
        inputs,
        ranked,
        strategyPack = None,
        truthContract = None,
        refs = Some(refs)
      )

    assertEquals(slots.sourceKind, MoveReviewPolishSlots.Source.Planner, clues(slots))
    assertEquals(slots.evidenceHook, Some("Short line: Nf3 Nc6 Bb5 a6 Bxc6."))
    assert(!slots.evidenceHook.exists(_.contains("Qxb2")), clues(slots))
    assert(slots.factGuardrails.exists(_.contains("local_fact=line_consequence/pv_coupled_line")), clues(slots.factGuardrails))
  }

  test("opening relation WhyThis needs admissible contrast before renderer release") {
    val primary =
      QuestionPlan(
        questionId = "q_opening_relation",
        questionKind = AuthorQuestionKind.WhyThis,
        priority = 100,
        claim = "The current move leaves the sampled opening branch, so the idea still needs current-line support.",
        evidence =
          Some(
            QuestionPlanEvidence(
              text = "a) h3 h6 Nf3",
              purposes = List("reply_multipv"),
              sourceKinds = List("opening_relation_translator"),
              branchScoped = true
            )
          ),
        contrast = Some("The practical alternative e4 stays closer to the familiar opening reference."),
        consequence = Some(QuestionPlanConsequence("That changes how much opening-reference context still applies.", QuestionPlanConsequenceBeat.WrapUp)),
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
    assert(slots.factGuardrails.exists(_.contains("local_fact=line_consequence/alternative_comparison")), clues(slots.factGuardrails))
    assert(slots.factGuardrails.exists(_.contains("local_fact_producer=alternative_comparison")), clues(slots.factGuardrails))
    assert(slots.factGuardrails.exists(_.contains("relations=alternative_contrast")), clues(slots.factGuardrails))
    assert(slots.factGuardrails.exists(_.contains("alternative_role:engine_best_branch")), clues(slots.factGuardrails))
    assert(slots.factGuardrails.exists(_.contains("alternative_role:played_branch")), clues(slots.factGuardrails))
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
    assert(slots.factGuardrails.exists(_.contains("local_fact=line_consequence/alternative_comparison")), clues(slots.factGuardrails))
    assert(slots.factGuardrails.exists(_.contains("local_fact_producer=alternative_comparison")), clues(slots.factGuardrails))
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
                    narrative = "allows a fork on the rook and queen",
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
      "The move Bd7 stays best because missing it allows a fork on the rook and queen."
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

  test("line-consequence surface evidence hook uses its checked line id before planner branch text") {
    val rawTemplateClaim = "Nf3 keeps the structure under control in the checked line."
    val checkedLine =
      refsForLine(
        exchangeLineFen,
        List("g1f3", "b8c6", "f1b5", "a7a6", "b5c6", "d7c6"),
        List("Nf3", "Nc6", "Bb5", "a6", "Bxc6", "dxc6")
      ).variations.head.copy(lineId = "line_04")
    val refs =
      MoveReviewRefs(
        startFen = exchangeLineFen,
        startPly = NarrativeUtils.plyFromFen(exchangeLineFen).map(_ + 1).getOrElse(1),
        variations = List(checkedLine)
      )
    val primary =
      QuestionPlan(
        questionId = "q_line_consequence_why_this_wrong_branch",
        questionKind = AuthorQuestionKind.WhyThis,
        priority = 100,
        claim = rawTemplateClaim,
        evidence =
          Some(
            QuestionPlanEvidence(
              text = "a) Qxb2 hxg4 Qxa1+",
              purposes = List("author_evidence"),
              sourceKinds = List("author_evidence"),
              branchScoped = true
            )
          ),
        contrast = None,
        consequence = None,
        fallbackMode = QuestionPlanFallbackMode.PlannerOwned,
        strengthTier = QuestionPlanStrengthTier.Moderate,
        sourceKinds = List("move_delta"),
        admissibilityReasons = List("move_owner"),
        plannerOwnerKind = PlannerOwnerKind.LineConsequence,
        plannerSource = "line_consequence"
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

    assertEquals(slots.sourceKind, MoveReviewPolishSlots.Source.Planner)
    assertEquals(slots.evidenceHook, Some("a) Nf3 Nc6 Bb5 a6 Bxc6."))
    assert(!slots.evidenceHook.exists(_.contains("Qxb2")), clues(slots))
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
        claim = "The checked line from h3 keeps Improving piece placement connected to e4 as a practical plan.",
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
              evidenceLine = "h3 e4 d5",
              planAnchorLine = Some("Further probe work still targets Improving piece placement through e4."),
              anchorTokens = List("e4"),
              matchedAnchorTokens = List("e4")
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
    assertEquals(causalTrace.flatMap(_.localFactProducer), Some("certified_strategy_delta"), clues(causalTrace))
    assert(causalTrace.exists(_.evidenceKinds.contains("plan_support")), clues(causalTrace))
    assert(causalTrace.exists(_.evidenceSources.contains("pv_coupled_plan_support")), clues(causalTrace))
    assert(causalTrace.exists(_.relationKinds.contains("played_move_consequence")), clues(causalTrace))
  }

  test("pv-coupled plan support requires a meaningful checked continuation") {
    val primary =
      QuestionPlan(
        questionId = "q_pv_plan_support_one_ply",
        questionKind = AuthorQuestionKind.WhatChanged,
        priority = 100,
        claim = "The checked line from h3 keeps Improving piece placement viable as a practical plan.",
        evidence =
          Some(
            QuestionPlanEvidence(
              text = "a) h3",
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
        candidateEvidenceLines = List("h3"),
        evidenceBackedPlans = Nil,
        opponentPlan = None,
        factualFallback = None,
        pvCoupledPlanSupport =
          Some(
            PvCoupledPlanSupport(
              planName = "Improving piece placement",
              playedSan = "h3",
              evidenceLine = "h3"
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

    assertEquals(slots.sourceKind, MoveReviewPolishSlots.Source.ExactFactualFallback)
    assertEquals(MoveReviewProseContract.stripMoveHeader(slots.claim), "This moves the pawn to h3.")
    assertEquals(causalTrace.map(_.status), Some("rejected"))
    assert(causalTrace.exists(_.rejectReasons.contains("local_fact_admission_missing")), clues(causalTrace))
    assert(
      causalTrace.exists(_.localFactRejectReasons.contains("pv_coupled_plan_support_evidence_missing")),
      clues(causalTrace)
    )
    assertEquals(causalTrace.flatMap(_.localFactFamily), None)
    assert(!causalTrace.exists(_.evidenceSources.contains("pv_coupled_plan_support")), clues(causalTrace))
  }

  test("candidate evidence includes reviewed move PV refs for planner coupling") {
    val refs = refsForLine(quietH3Ctx.fen, List("h2h3", "a7a6", "g1f3"), List("h3", "a6", "Nf3"))
    val lines = MoveReviewCompressionPolicy.candidateEvidenceLines(Some(refs), quietH3Ctx)

    assert(lines.contains("Short line: h3 a6 Nf3."), clues(lines))
  }

  test("candidate evidence prefers a validated reviewed-move continuation over a one-ply reviewed ref") {
    val onePly = refsForLine(quietH3Ctx.fen, List("h2h3"), List("h3"))
    val continuation =
      refsForLine(quietH3Ctx.fen, List("h2h3", "a7a6", "g1f3"), List("h3", "a6", "Nf3"))
    val refs =
      onePly.copy(
        variations =
          onePly.variations ++ continuation.variations.map(_.copy(lineId = "line_02"))
      )
    val lines = MoveReviewCompressionPolicy.candidateEvidenceLines(Some(refs), quietH3Ctx)

    assert(lines.contains("Short line: h3 a6 Nf3."), clues(lines))
  }

  test("planner builder does not authorize unmatched pv-coupled plan support as a local fact") {
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

    assert(
      inputs.pvCoupledPlanSupport.exists(support =>
        LineScopedCitation.firstConcreteSanToken(support.evidenceLine).contains("h3")
      ),
      clues(inputs.pvCoupledPlanSupport)
    )
    assertEquals(inputs.pvCoupledPlanSupport.exists(_.anchorMatched), false, clues(inputs.pvCoupledPlanSupport))
    assertEquals(ranked.primary, None, clues(ranked))
    assertEquals(causalTrace.map(_.status), None, clues(causalTrace))
  }

  test("planner builder does not connect one-ply PV refs to pv-coupled plan support") {
    val refs = refsForLine(quietH3Ctx.fen, List("h2h3"), List("h3"))
    val ctx =
      quietH3Ctx.copy(
        authorQuestions = List(whatChangedQuestion()),
        decision = Some(DecisionRationale(None, "checked line", PVDelta(Nil, Nil, Nil, Nil), ConfidenceLevel.Engine)),
        strategicPlanEvidence = pvCoupledPlanEvidence("Improving piece placement")
      )
    val candidateEvidence = MoveReviewCompressionPolicy.candidateEvidenceLines(Some(refs), ctx)
    val inputs = QuestionPlannerInputsBuilder.build(ctx, strategyPack = None, truthContract = None, candidateEvidence)

    assert(candidateEvidence.exists(_.contains("h3")), clues(candidateEvidence))
    assertEquals(inputs.pvCoupledPlanSupport, None, clues(inputs.pvCoupledPlanSupport))
  }

  test("planner builder records matched plan anchors on pv-coupled plan support") {
    val refs = refsForLine(quietH3Ctx.fen, List("h2h3", "a7a6", "g1f3"), List("h3", "a6", "Nf3"))
    val ctx =
      quietH3Ctx.copy(
        authorQuestions = List(whatChangedQuestion()),
        decision = Some(DecisionRationale(None, "checked line", PVDelta(Nil, Nil, Nil, Nil), ConfidenceLevel.Engine)),
        strategicPlanEvidence = pvCoupledPlanEvidence("Improving piece placement")
      )
    val candidateEvidence =
      MoveReviewCompressionPolicy.candidateEvidenceLines(Some(refs), ctx) :+
        "Further probe work still targets Improving piece placement through a6 and Nf3."
    val inputs = QuestionPlannerInputsBuilder.build(ctx, strategyPack = None, truthContract = None, candidateEvidence)
    val support = inputs.pvCoupledPlanSupport.getOrElse(fail("missing pv-coupled support"))
    val ranked = QuestionFirstCommentaryPlanner.plan(ctx, inputs, truthContract = None)
    val causalTrace =
      MoveReviewCompressionPolicy.causalClaimTrace(
        ctx,
        inputs,
        ranked,
        truthContract = None,
        refs = Some(refs)
      )

    assertEquals(support.anchorMatched, true, clues(support))
    assertEquals(support.matchedAnchorTokens, List("a6", "Nf3"), clues(support))
    assert(support.claim.contains("connected to a6 and Nf3"), clues(support.claim))
    assert(causalTrace.exists(_.localFactGuardrails.contains("local_fact_guardrail:plan_anchor_matched:true")), clues(causalTrace))
    assert(causalTrace.exists(_.localFactEvidenceRefs.contains("plan_anchor_matched_token:a6")), clues(causalTrace))
  }

  test("planner builder can match pv-coupled plan anchors on a later played-first evidence line") {
    val ctx =
      quietH3Ctx.copy(
        playedSan = Some("Nb5"),
        playedMove = Some("c3b5"),
        authorQuestions = List(whatChangedQuestion()),
        decision = Some(DecisionRationale(None, "checked line", PVDelta(Nil, Nil, Nil, Nil), ConfidenceLevel.Engine)),
        strategicPlanEvidence = pvCoupledPlanEvidence("Opening Development and Center Control")
      )
    val candidateEvidence =
      List(
        "The checked line continues Nb5 Na6 Bc4 Nf6.",
        "Short line: Nb5 Na6 Bc4 Nf6 d6.",
        "Further probe work still targets Opening Development and Center Control through d6 and Nb1."
      )
    val inputs = QuestionPlannerInputsBuilder.build(ctx, strategyPack = None, truthContract = None, candidateEvidence)
    val support = inputs.pvCoupledPlanSupport.getOrElse(fail("missing pv-coupled support"))

    assertEquals(support.anchorMatched, true, clues(support))
    assertEquals(support.evidenceLine, "Short line: Nb5 Na6 Bc4 Nf6 d6.", clues(support))
    assertEquals(support.matchedAnchorTokens, List("d6"), clues(support))
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
    assertEquals(causalTrace.flatMap(_.localFactProducer), Some("certified_strategy_delta"))
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
    assert(slots.factGuardrails.exists(_.contains("local_fact=line_consequence/pv_coupled_line")), clues(slots.factGuardrails))
    assert(slots.factGuardrails.exists(_.contains("local_fact_producer=line_consequence")), clues(slots.factGuardrails))
  }

  test("line-consequence primary does not promote broad discovered-attack local fact as support prose") {
    val fen = "rn1qkb1r/pp2pppp/2p2n2/5b2/P1pP4/2N2N2/1P2PPPP/R1BQKB1R w KQkq - 1 6"
    val ctx =
      quietH3Ctx.copy(
        fen = fen,
        ply = 11,
        playedMove = Some("e2e3"),
        playedSan = Some("e3"),
        phase = PhaseContext("Opening", "Slav Defense")
      )
    val consequence =
      LineConsequenceEvidence(
        lineId = Some("line_03"),
        sanMoves = List("e3", "e6", "Bxc4", "Bb4", "O-O"),
        uciMoves = List("e2e3", "e7e6", "f1c4", "f8b4", "e1g1"),
        scoreCp = Some(27),
        mate = None,
        depth = Some(10),
        windowPly = 5,
        kind = LineConsequenceKind.DelayedPawnCapture,
        triggerSan = Some("Bxc4"),
        consequence = "The checked line reaches a delayed pawn capture after Bxc4.",
        whyItMatters = Some("The point is the later pawn capture, not just the first move."),
        release = LineConsequenceRelease.SurfaceCandidate,
        rejectReasons = Nil
      )
    val tacticalLocalFact =
      MoveReviewLocalFact.admitted(
        MoveReviewLocalFact.Candidate(
          family = MoveReviewLocalFact.Family.Threat,
          source = MoveReviewLocalFact.Source.CanonicalFact,
          producer = MoveReviewLocalFact.Producer.TacticalMotif,
          subject = MoveReviewLocalFact.Subject.LineOrReply,
          strictFallbackCandidate = true,
          anchors = List(MoveReviewLocalFact.Anchor("tactical_kind", "discovered_attack")),
          lineBinding = MoveReviewLocalFact.LineBinding.PvCoupled,
          evidenceRefs = List(
            "typed_local_fact_source:canonical_fact",
            "typed_local_fact_family:threat",
            "typed_local_fact_producer:tactical_motif",
            "tactical_kind:discovered_attack"
          ),
          guardrails = List("tactical_kind:discovered_attack", "current_move_motif_owned", "pv_confirms_tactic")
        )
      )
    val tacticalResult =
      MoveReviewExplanationBuilder.Result(
        explanation =
          MoveReviewExplanation(
            title = "e3 opens a discovered attack",
            prose = "e3 opens a discovered attack from the f1 bishop toward the c4 pawn.",
            source = "canonical_fact"
          ),
        localFact = tacticalLocalFact
      )
    val primary =
      QuestionPlan(
        questionId = "q_e3_delayed_capture",
        questionKind = AuthorQuestionKind.WhatChanged,
        priority = 100,
        claim = "The move is connected to a line consequence.",
        evidence =
          Some(
            QuestionPlanEvidence(
              text = "Short line: e3 e6 Bxc4 Bb4 O-O.",
              purposes = List("reply_multipv"),
              sourceKinds = List("line_consequence"),
              branchScoped = true
            )
          ),
        contrast = None,
        consequence = None,
        fallbackMode = QuestionPlanFallbackMode.PlannerOwned,
        strengthTier = QuestionPlanStrengthTier.Exact,
        sourceKinds = List("line_consequence"),
        admissibilityReasons = List("pv_line_consequence", "move_attributed_change"),
        plannerOwnerKind = PlannerOwnerKind.LineConsequence,
        plannerSource = "line_consequence"
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
        factualFallback = None,
        lineConsequence = Some(consequence),
        localFactResult = Some(tacticalResult)
      )
    val slots =
      MoveReviewCompressionPolicy.buildSlotsOrFallbackFromPlannerRuntime(
        ctx,
        inputs,
        RankedQuestionPlans(primary = Some(primary), secondary = None, rejected = Nil),
        strategyPack = None,
        truthContract = None,
        refs = Some(refsForLine(fen, consequence.uciMoves, consequence.sanMoves))
      )

    val claim = MoveReviewProseContract.stripMoveHeader(slots.claim)
    assert(claim.startsWith("On the checked line"), clues(claim, slots))
    assertEquals(slots.supportPrimary, None, clues(slots))
    assert(!slots.factGuardrails.exists(_.contains("opens a discovered attack")), clues(slots.factGuardrails))
    assert(slots.factGuardrails.exists(_.contains("local_fact=line_consequence/pv_coupled_line")), clues(slots.factGuardrails))
  }

  test("opening-goal basic local fact is surfaced through CausalFrame before direct basic rendering") {
    val ctx = italianCtx
    val slots =
      MoveReviewPolishSlotsBuilder.buildOrFallback(
        ctx,
        BookStyleRenderer.validatedOutline(ctx),
        refs = Some(refsForLine(italianBeforeBc4, List("f1c4", "g8f6", "d2d3"), List("Bc4", "Nf6", "d3"))),
        strategyPack = None,
        truthContract = None
      )

    assertEquals(slots.sourceKind, MoveReviewPolishSlots.Source.Planner, clues(slots))
    assert(slots.moveReviewExplanation.exists(_.source == "opening_goal"), clues(slots))
    assert(slots.moveReviewExplanation.exists(_.reasonTags.contains("review_intent:normal_development")), clues(slots))
    assert(slots.factGuardrails.exists(_.contains("local_fact=opening_goal/opening_goal_evidence")), clues(slots.factGuardrails))
    assert(slots.factGuardrails.exists(_.contains("local_fact_producer=opening_goal")), clues(slots.factGuardrails))
    assert(slots.factGuardrails.exists(_.contains("surface_checked_line=true")), clues(slots.factGuardrails))
    assert(slots.moveReviewExplanation.exists(explanation => slots.claim.contains(explanation.prose.take(24).trim)), clues(slots))
  }

  test("basic typed local fact is surfaced through CausalFrame before direct basic rendering") {
    val ctx = italianCtx.copy(openingGoalEvaluation = None)
    val strategyPack =
      StrategyPack(
        sideToMove = "white",
        strategicIdeas =
          List(
            StrategyIdeaSignal(
              ideaId = "idea_c4_pressure",
              ownerSide = "white",
              kind = StrategicIdeaKind.TargetFixing,
              group = "local_pressure",
              readiness = StrategicIdeaReadiness.Ready,
              focusSquares = List("c4"),
              confidence = 0.91,
              evidenceRefs = List("line_control_features"),
              targetSquare = Some("c4")
            )
          )
      )
    val slots =
      MoveReviewPolishSlotsBuilder.buildOrFallback(
        ctx,
        BookStyleRenderer.validatedOutline(ctx, strategyPack = Some(strategyPack)),
        refs = Some(refsForLine(italianBeforeBc4, List("f1c4", "g8f6", "d2d3"), List("Bc4", "Nf6", "d3"))),
        strategyPack = Some(strategyPack),
        truthContract = None
      )

    assertEquals(slots.sourceKind, MoveReviewPolishSlots.Source.Planner, clues(slots))
    assert(slots.moveReviewExplanation.exists(_.source == "practical_position_support"), clues(slots))
    assertEquals(slots.localFact.map(_.family), Some(MoveReviewLocalFact.Family.Pressure), clues(slots))
    assertEquals(slots.localFact.map(_.producer), Some(MoveReviewLocalFact.Producer.CertifiedStrategyDelta), clues(slots))
    assert(slots.factGuardrails.exists(_.contains("local_fact=pressure/certified_strategy")), clues(slots.factGuardrails))
    assert(slots.factGuardrails.exists(_.contains("local_fact_producer=certified_strategy_delta")), clues(slots.factGuardrails))
    assert(slots.evidenceHook.exists(_.startsWith("Short line: Bc4")), clues(slots))
  }

  test("central-break timing basic local fact is surfaced as WhyNow CausalFrame timing") {
    val fen = "rnbqkbnr/pp1ppppp/2p5/8/3PP3/8/PPP2PPP/RNBQKBNR b KQkq - 0 2"
    val bestUcis = List("d7d6", "f2f4", "e7e5", "d4e5", "d6e5")
    val playedUcis = List("d7d5", "e4e5", "e7e6", "f1d3", "c6c5")
    def variation(lineId: String, ucis: List[String], sans: List[String], cp: Int): MoveReviewVariationRef =
      val fens = ucis.indices.toList.map(idx => NarrativeUtils.uciListToFen(fen, ucis.take(idx + 1)))
      MoveReviewVariationRef(
        lineId = lineId,
        scoreCp = cp,
        mate = None,
        depth = 8,
        moves =
          ucis.zip(sans).zipWithIndex.map { case ((uci, san), idx) =>
            val ply = NarrativeUtils.plyFromFen(fen).map(_ + 1 + idx).getOrElse(idx + 1)
            MoveReviewMoveRef(
              refId = s"${lineId}_m${idx + 1}",
              san = san,
              uci = uci,
              fenAfter = fens(idx),
              ply = ply,
              moveNo = (ply + 1) / 2,
              marker = None
            )
          }
      )
    val ctx =
      quietH3Ctx.copy(
        fen = fen,
        ply = 4,
        playedMove = Some("d7d5"),
        playedSan = Some("d5"),
        phase = PhaseContext("Opening", "central break timing"),
        summary = NarrativeSummary("central break timing", None, "NarrowChoice", "Maintain", "0.00"),
        engineEvidence =
          Some(
            EngineEvidence(
              depth = 8,
              variations =
                List(
                  VariationLine(bestUcis, scoreCp = 76, depth = 8),
                  VariationLine(playedUcis, scoreCp = 85, depth = 8)
                )
            )
          )
      )
    val refs =
      MoveReviewRefs(
        startFen = fen,
        startPly = NarrativeUtils.plyFromFen(fen).map(_ + 1).getOrElse(1),
        variations =
          List(
            variation("line_best", bestUcis, List("d6", "f4", "e5", "dxe5", "dxe5"), 76),
            variation("line_played", playedUcis, List("d5", "e5", "e6", "Bd3", "c5"), 85)
          )
      )
    val truth =
      DecisiveTruth.derive(
        ctx = ctx,
        comparisonOverride =
          Some(
            DecisionComparison(
              chosenMove = Some("d5"),
              engineBestMove = Some("d6"),
              engineBestScoreCp = Some(76),
              engineBestPv = List("d6", "f4", "e5", "dxe5"),
              cpLossVsChosen = Some(9),
              deferredMove = None,
              deferredReason = None,
              deferredSource = None,
              evidence = None,
              practicalAlternative = false,
              chosenMatchesBest = false
            )
          )
      )
    val slots =
      MoveReviewPolishSlotsBuilder.buildOrFallback(
        ctx,
        BookStyleRenderer.validatedOutline(ctx, truthContract = Some(truth)),
        refs = Some(refs),
        strategyPack = None,
        truthContract = Some(truth)
      )

    assertEquals(slots.sourceKind, MoveReviewPolishSlots.Source.Planner, clues(slots))
    assert(slots.moveReviewExplanation.exists(_.source == "certified_strategy_support"), clues(slots))
    assertEquals(slots.localFact.map(_.family), Some(MoveReviewLocalFact.Family.Timing), clues(slots))
    assertEquals(slots.localFact.map(_.producer), Some(MoveReviewLocalFact.Producer.CertifiedStrategyDelta), clues(slots))
    assert(slots.factGuardrails.exists(_.contains("question=WhyNow")), clues(slots.factGuardrails))
    assert(slots.factGuardrails.exists(_.contains("relations=played_move_consequence,timing_constraint")), clues(slots.factGuardrails))
    assert(slots.factGuardrails.exists(_.contains("surface_timing=true")), clues(slots.factGuardrails))
    assert(slots.factGuardrails.exists(_.contains("local_fact=timing/certified_strategy")), clues(slots.factGuardrails))
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
      "This moves the bishop from f1 to c4.",
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
    assert(slots.supportPrimary.exists(_.toLowerCase.contains("blunder")), clues(slots))
    assert(slots.supportPrimary.exists(_.toLowerCase.contains("gives up")), clues(slots))

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
    assert(missedWinSlots.supportPrimary.exists(_.toLowerCase.contains("missed win")), clues(missedWinSlots))
    assert(missedWinSlots.supportPrimary.exists(_.toLowerCase.contains("gives up")), clues(missedWinSlots))
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
    val inaccuracyContract =
      truthContract(
        playedMove = None,
        verifiedBestMove = Some("e2e4"),
        truthClass = DecisiveTruthClass.Inaccuracy,
        cpLoss = 45,
        swingSeverity = 45,
        reasonFamily = DecisiveReasonKind.QuietTechnicalMove,
        visibilityRole = TruthVisibilityRole.SupportingVisible
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
