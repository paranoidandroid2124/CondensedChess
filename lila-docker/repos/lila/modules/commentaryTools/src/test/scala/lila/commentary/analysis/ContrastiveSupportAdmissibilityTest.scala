package lila.commentary.analysis

import chess.{ Color, Knight, Queen, Rook, Square }
import munit.FunSuite
import lila.commentary.MoveReviewExplanation
import lila.commentary.analysis.practical.ContrastiveSupportAdmissibility
import lila.commentary.model.{ Motif, PreventedPlanInfo, ThreatRow }
import lila.commentary.model.authoring.AuthorQuestionKind
import lila.commentary.model.strategic.{ CounterfactualMatch, VariationLine }

class ContrastiveSupportAdmissibilityTest extends FunSuite:

  private def plan(
      kind: AuthorQuestionKind,
      consequence: Option[QuestionPlanConsequence] = None,
      sourceKinds: List[String] = List("planner"),
      plannerOwnerKind: PlannerOwnerKind = PlannerOwnerKind.ForcingDefense,
      plannerSource: String = "fixture_owner"
  ): QuestionPlan =
    QuestionPlan(
      questionId = s"${kind.toString.toLowerCase}_q",
      questionKind = kind,
      priority = 100,
      claim = "placeholder claim.",
      evidence = None,
      contrast = Some("Existing planner contrast."),
      consequence = consequence,
      fallbackMode = QuestionPlanFallbackMode.PlannerOwned,
      strengthTier = QuestionPlanStrengthTier.Strong,
      sourceKinds = sourceKinds,
      admissibilityReasons = List("fixture"),
      plannerOwnerKind = plannerOwnerKind,
      plannerSource = plannerSource
    )

  private def inputs(
      decisionComparison: Option[DecisionComparison] = None,
      opponentThreats: List[ThreatRow] = Nil,
      preventedPlansNow: List[PreventedPlanInfo] = Nil,
      counterfactual: Option[CounterfactualMatch] = None,
      localFactResult: Option[MoveReviewExplanationBuilder.Result] = None
  ): QuestionPlannerInputs =
    QuestionPlannerInputs(
      mainBundle = None,
      quietIntent = None,
      decisionFrame = CertifiedDecisionFrame(),
      decisionComparison = decisionComparison,
      alternativeNarrative = None,
      truthMode = PlayerFacingTruthMode.Strategic,
      preventedPlansNow = preventedPlansNow,
      pvDelta = None,
      counterfactual = counterfactual,
      practicalAssessment = None,
      opponentThreats = opponentThreats,
      forcingThreats = Nil,
      evidenceByQuestionId = Map.empty,
      candidateEvidenceLines = Nil,
      evidenceBackedPlans = Nil,
      opponentPlan = None,
      factualFallback = None,
      localFactResult = localFactResult
    )

  private def localFactResult(
      family: MoveReviewLocalFact.Family,
      prose: String,
      anchor: String,
      evidenceRefs: List[String]
  ): MoveReviewExplanationBuilder.Result =
    val localFact =
      MoveReviewLocalFact.admitted(
        MoveReviewLocalFact.Candidate(
          family = family,
          source = MoveReviewLocalFact.Source.CertifiedStrategy,
          producer = MoveReviewLocalFact.Producer.CertifiedStrategyDelta,
          subject =
            if family == MoveReviewLocalFact.Family.Pressure then MoveReviewLocalFact.Subject.Target
            else MoveReviewLocalFact.Subject.PlanResource,
          strictFallbackCandidate = false,
          anchors = List(MoveReviewLocalFact.Anchor("target", anchor)),
          lineBinding = MoveReviewLocalFact.LineBinding.PvCoupled,
          evidenceRefs = evidenceRefs,
          guardrails = List("practical_position_support", "move_touches_strategy_anchor", "pv_coupled")
        )
      )
    MoveReviewExplanationBuilder.Result(
      explanation =
        MoveReviewExplanation(
          title = "checked local fact",
          prose = prose,
          source = "practical_position_support"
        ),
      localFact = localFact
    )

  test("rejects raw close candidate for WhyThis contrast support") {
    val comparison =
      DecisionComparison(
        chosenMove = Some("Qe2"),
        engineBestMove = Some("Qe4"),
        engineBestScoreCp = Some(65),
        engineBestPv = List("Qe4", "...Qe7"),
        cpLossVsChosen = Some(65),
        deferredMove = Some("Qe4"),
        deferredReason = Some("it keeps pressure on e7 and leaves the reply pinned"),
        deferredSource = Some("close_candidate"),
        evidence = None,
        practicalAlternative = true,
        chosenMatchesBest = false
      )

    val trace = ContrastiveSupportAdmissibility.decide(plan(AuthorQuestionKind.WhyThis), inputs(decisionComparison = Some(comparison)), None)

    assertEquals(trace.contrast_admissible, false)
    assertEquals(trace.contrast_reject_reason, Some(ContrastiveSupportAdmissibility.RejectReason.RawCloseCandidate))
    assertEquals(trace.effectiveSupport(Some("Existing planner contrast.")), Some("Existing planner contrast."))
  }

  test("rejects vague engine preference without a concrete consequence") {
    val comparison =
      DecisionComparison(
        chosenMove = Some("Re1"),
        engineBestMove = Some("Qf3"),
        engineBestScoreCp = Some(80),
        engineBestPv = List("Qf3", "...Re8"),
        cpLossVsChosen = Some(80),
        deferredMove = Some("Qf3"),
        deferredReason = Some("it trails the engine line by about 80 cp"),
        deferredSource = Some("top_engine_move"),
        evidence = None,
        practicalAlternative = false,
        chosenMatchesBest = false
      )

    val trace = ContrastiveSupportAdmissibility.decide(plan(AuthorQuestionKind.WhyThis), inputs(decisionComparison = Some(comparison)), None)

    assertEquals(trace.contrast_admissible, false)
    assertEquals(trace.contrast_reject_reason, Some(ContrastiveSupportAdmissibility.RejectReason.VagueEnginePreference))
    assertEquals(trace.contrast_source_kind, None)
  }

  test("admits certified local consequence for acceptable non-best WhyThis without calling it best") {
    val result =
      localFactResult(
        family = MoveReviewLocalFact.Family.Defense,
        prose = "b4 is tied to checked counterplay restraint around b4.",
        anchor = "b4",
        evidenceRefs = List("strategic_idea_kind:counterplay_suppression", "strategy_ref:counterplay_break_denial")
      )
    val comparison =
      DecisionComparison(
        chosenMove = Some("b4"),
        engineBestMove = Some("Ra4"),
        engineBestScoreCp = Some(42),
        engineBestPv = List("Ra4", "a3"),
        cpLossVsChosen = Some(42),
        deferredMove = Some("Ra4"),
        deferredReason = Some("it trails the engine line by about 42 cp"),
        deferredSource = Some("top_engine_move"),
        evidence = None,
        practicalAlternative = false,
        chosenMatchesBest = false
      )

    val trace =
      ContrastiveSupportAdmissibility.decide(
        plan(
          AuthorQuestionKind.WhyThis,
          sourceKinds = List("typed_local_fact", "practical_position_support"),
          plannerOwnerKind = PlannerOwnerKind.MoveDelta,
          plannerSource = "practical_position_support"
        ).copy(claim = result.explanation.prose),
        inputs(decisionComparison = Some(comparison), localFactResult = Some(result)),
        None
      )
    val localOnlyTrace =
      ContrastiveSupportAdmissibility.decide(
        plan(
          AuthorQuestionKind.WhyThis,
          sourceKinds = List("typed_local_fact", "relation_witness"),
          plannerOwnerKind = PlannerOwnerKind.ConcreteTactical,
          plannerSource = "relation_witness"
        ).copy(claim = result.explanation.prose),
        inputs(localFactResult = Some(result)),
        None
      )

    assert(QuestionFirstCommentaryPlanner.localFactResultWhyThisEligible(result), clues(result.localFact))
    assertEquals(localOnlyTrace.contrast_admissible, true, clues(localOnlyTrace))
    assertEquals(trace.contrast_admissible, true, clues(trace))
    assertEquals(trace.contrast_source_kind, Some(ContrastiveSupportAdmissibility.SourceKind.PlayedMoveWithCertifiedLocalConsequence))
    assertEquals(trace.contrast_anchor, Some("b4"))
    assertEquals(trace.contrast_consequence, Some("That keeps counterplay restrained around b4."))
    assert(trace.contrast_sentence.exists(_.contains("has a checked local point")), clues(trace))
    assert(trace.contrast_sentence.forall(!_.contains("stays best")), clues(trace))
  }

  test("rejects certified local consequence for bad truth instead of making the move sound useful") {
    val result =
      localFactResult(
        family = MoveReviewLocalFact.Family.Defense,
        prose = "b4 is tied to checked counterplay restraint around b4.",
        anchor = "b4",
        evidenceRefs = List("strategic_idea_kind:counterplay_suppression", "strategy_ref:counterplay_break_denial")
      )
    val comparison =
      DecisionComparison(
        chosenMove = Some("b4"),
        engineBestMove = Some("Ra4"),
        engineBestScoreCp = Some(42),
        engineBestPv = List("Ra4", "a3"),
        cpLossVsChosen = Some(80),
        deferredMove = Some("Ra4"),
        deferredReason = Some("it trails the engine line by about 80 cp"),
        deferredSource = Some("top_engine_move"),
        evidence = None,
        practicalAlternative = false,
        chosenMatchesBest = false
      )
    val truthContract =
      DecisiveTruthContract(
        playedMove = Some("b4"),
        verifiedBestMove = Some("Ra4"),
        truthClass = DecisiveTruthClass.Inaccuracy,
        cpLoss = 80,
        swingSeverity = 80,
        reasonFamily = DecisiveReasonKind.QuietTechnicalMove,
        allowConcreteBenchmark = false,
        chosenMatchesBest = false,
        compensationAllowed = false,
        truthPhase = None,
        ownershipRole = TruthOwnershipRole.NoneRole,
        visibilityRole = TruthVisibilityRole.PrimaryVisible,
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

    val trace =
      ContrastiveSupportAdmissibility.decide(
        plan(
          AuthorQuestionKind.WhyThis,
          sourceKinds = List("typed_local_fact", "practical_position_support"),
          plannerOwnerKind = PlannerOwnerKind.MoveDelta,
          plannerSource = "practical_position_support"
        ).copy(claim = result.explanation.prose),
        inputs(decisionComparison = Some(comparison), localFactResult = Some(result)),
        Some(truthContract)
      )

    assertEquals(trace.contrast_admissible, false)
    assertEquals(trace.contrast_reject_reason, Some(ContrastiveSupportAdmissibility.RejectReason.VagueEnginePreference))
    assertEquals(trace.contrast_source_kind, None)
  }

  test("does not promote incomplete relation witness local fact as standalone played-move consequence") {
    val localFact =
      MoveReviewLocalFact.admitted(
        MoveReviewLocalFact.Candidate(
          family = MoveReviewLocalFact.Family.Threat,
          source = MoveReviewLocalFact.Source.PvCoupledLine,
          producer = MoveReviewLocalFact.Producer.RelationWitness,
          subject = MoveReviewLocalFact.Subject.LineOrReply,
          strictFallbackCandidate = true,
          anchors = List(MoveReviewLocalFact.Anchor("relation_kind", "overload")),
          lineBinding = MoveReviewLocalFact.LineBinding.PvCoupled,
          evidenceRefs = List("relation_kind:overload", "overload_relation_witness", "evidence_line_binding:pv_coupled"),
          guardrails = List("relation_witness_typed_details", "pv_coupled")
        )
      )
    val result =
      MoveReviewExplanationBuilder.Result(
        explanation =
          MoveReviewExplanation(
            title = "checked relation witness",
            prose = "Qd6+ overloads the defender on d4 across d5 and f4.",
            source = "relation_witness"
          ),
        localFact = localFact
      )
    val comparison =
      DecisionComparison(
        chosenMove = Some("Qd6+"),
        engineBestMove = Some("Qd6+"),
        engineBestScoreCp = Some(15),
        engineBestPv = List("Qd6+", "Ke4"),
        cpLossVsChosen = Some(0),
        deferredMove = Some("Qd6+"),
        deferredReason = Some("it trails the engine line by about 0 cp"),
        deferredSource = Some("top_engine_move"),
        evidence = None,
        practicalAlternative = false,
        chosenMatchesBest = true
      )

    val trace =
      ContrastiveSupportAdmissibility.decide(
        plan(
          AuthorQuestionKind.WhyThis,
          sourceKinds = List("typed_local_fact", "relation_witness"),
          plannerOwnerKind = PlannerOwnerKind.ConcreteTactical,
          plannerSource = "relation_witness"
        ).copy(claim = result.explanation.prose),
        inputs(decisionComparison = Some(comparison), localFactResult = Some(result)),
        None
    )

    assertEquals(trace.contrast_admissible, false)
    assertEquals(trace.contrast_reject_reason, Some(ContrastiveSupportAdmissibility.RejectReason.VagueEnginePreference))
    assertEquals(trace.contrast_source_kind, None)
  }

  test("promotes typed relation witness local fact when tactical consequence details are present") {
    val localFact =
      MoveReviewLocalFact.admitted(
        MoveReviewLocalFact.Candidate(
          family = MoveReviewLocalFact.Family.Threat,
          source = MoveReviewLocalFact.Source.PvCoupledLine,
          producer = MoveReviewLocalFact.Producer.RelationWitness,
          subject = MoveReviewLocalFact.Subject.LineOrReply,
          strictFallbackCandidate = true,
          anchors = List(MoveReviewLocalFact.Anchor("relation_kind", "overload")),
          lineBinding = MoveReviewLocalFact.LineBinding.PvCoupled,
          evidenceRefs =
            List(
              "evidence_source:relation_witness",
              "typed_local_fact_source:relation_witness",
              "relation_kind:overload",
              "relation_fact:defender:d4",
              "relation_fact:duties:d5|f4",
              "overload_relation_witness",
              "evidence_line_binding:pv_coupled"
            ),
          guardrails = List("relation_witness_typed_details", "pv_coupled")
        )
      )
    val result =
      MoveReviewExplanationBuilder.Result(
        explanation =
          MoveReviewExplanation(
            title = "checked relation witness",
            prose = "Qd6+ overloads the defender on d4 across d5 and f4.",
            source = "relation_witness"
          ),
        localFact = localFact
      )
    val comparison =
      DecisionComparison(
        chosenMove = Some("Qd6+"),
        engineBestMove = Some("Qd6+"),
        engineBestScoreCp = Some(15),
        engineBestPv = List("Qd6+", "Ke4"),
        cpLossVsChosen = Some(0),
        deferredMove = Some("Qd6+"),
        deferredReason = Some("it trails the engine line by about 0 cp"),
        deferredSource = Some("top_engine_move"),
        evidence = None,
        practicalAlternative = false,
        chosenMatchesBest = true
      )

    val trace =
      ContrastiveSupportAdmissibility.decide(
        plan(
          AuthorQuestionKind.WhyThis,
          sourceKinds = List("typed_local_fact", "relation_witness"),
          plannerOwnerKind = PlannerOwnerKind.ConcreteTactical,
          plannerSource = "relation_witness"
        ).copy(claim = result.explanation.prose),
        inputs(decisionComparison = Some(comparison), localFactResult = Some(result)),
        None
      )

    assertEquals(trace.contrast_admissible, true, clues(trace))
    assertEquals(trace.contrast_source_kind, Some(ContrastiveSupportAdmissibility.SourceKind.TopEngineMoveWithConcreteConsequence))
    assertEquals(trace.contrast_consequence, Some("That keeps the defender on d4 overloaded across d5 and f4."))
    assertEquals(
      trace.effectiveSupport(None),
      Some("The move Qd6+ stays best because it keeps the defender on d4 overloaded across d5 and f4.")
    )
  }

  test("matches supported-local proof source when planner selected source differs from explanation source") {
    val localFact =
      MoveReviewLocalFact.admitted(
        MoveReviewLocalFact.Candidate(
          family = MoveReviewLocalFact.Family.Pressure,
          source = MoveReviewLocalFact.Source.PvCoupledLine,
          producer = MoveReviewLocalFact.Producer.CertifiedStrategyDelta,
          subject = MoveReviewLocalFact.Subject.Target,
          strictFallbackCandidate = false,
          anchors = List(MoveReviewLocalFact.Anchor("target", "e3")),
          lineBinding = MoveReviewLocalFact.LineBinding.PvCoupled,
          evidenceRefs =
            List(
              "proof_source:iqp_inducement_probe",
              "claim_source:iqp_inducement_probe",
              "owner_seed:isolated_pawn:d5",
              "continuation:isolated_pawn:d5",
              "anchor:target_pressure:d5"
            ),
          guardrails = List("promoted_authority_tier:SupportedLocal", "packet_scope:MoveLocal")
        )
      )
    val result =
      MoveReviewExplanationBuilder.Result(
        explanation =
          MoveReviewExplanation(
            title = "checked supported local fact",
            prose = "This sequence leaves an isolated pawn as the local target.",
            source = "certified_strategy_support"
          ),
        localFact = localFact
      )
    val comparison =
      DecisionComparison(
        chosenMove = Some("Qxc6"),
        engineBestMove = Some("Qxc6"),
        engineBestScoreCp = Some(35),
        engineBestPv = List("Qxc6", "g4", "Nd2"),
        cpLossVsChosen = Some(0),
        deferredMove = Some("Qxc6"),
        deferredReason = Some("it trails the engine line by about 0 cp"),
        deferredSource = Some("top_engine_move"),
        evidence = None,
        practicalAlternative = false,
        chosenMatchesBest = true
      )

    val trace =
      ContrastiveSupportAdmissibility.decide(
        plan(
          AuthorQuestionKind.WhyThis,
          sourceKinds = List("certified_strategy_delta", "iqp_inducement_probe", "typed_local_fact"),
          plannerOwnerKind = PlannerOwnerKind.MoveDelta,
          plannerSource = "iqp_inducement_probe"
        ).copy(claim = "A key idea is that this sequence leaves an isolated pawn as the local target."),
        inputs(decisionComparison = Some(comparison), localFactResult = Some(result)),
        None
      )

    assertEquals(trace.contrast_admissible, true)
    assertEquals(trace.contrast_consequence, Some("That leaves d5 as an isolated pawn target."))
    assertEquals(trace.effectiveSupport(None), Some("The move Qxc6 stays best because it leaves d5 as an isolated pawn target."))
  }

  test("uses certified local consequence to make chosen best WhyThis concrete") {
    val result =
      localFactResult(
        family = MoveReviewLocalFact.Family.Pressure,
        prose = "Re8 is tied to checked space around e-file.",
        anchor = "e-file",
        evidenceRefs = List("strategic_idea_kind:space_gain_or_restriction", "strategy_ref:plan_spaceadvantage")
      )
    val comparison =
      DecisionComparison(
        chosenMove = Some("Re8"),
        engineBestMove = Some("Re8"),
        engineBestScoreCp = Some(20),
        engineBestPv = List("Re8", "h3", "Rc8"),
        cpLossVsChosen = Some(0),
        deferredMove = Some("Re8"),
        deferredReason = Some("it trails the engine line by about 0 cp"),
        deferredSource = Some("top_engine_move"),
        evidence = None,
        practicalAlternative = false,
        chosenMatchesBest = true
      )

    val trace =
      ContrastiveSupportAdmissibility.decide(
        plan(
          AuthorQuestionKind.WhyThis,
          sourceKinds = List("typed_local_fact", "practical_position_support"),
          plannerOwnerKind = PlannerOwnerKind.MoveDelta,
          plannerSource = "practical_position_support"
        ).copy(claim = result.explanation.prose),
        inputs(decisionComparison = Some(comparison), localFactResult = Some(result)),
        None
      )

    assertEquals(trace.contrast_admissible, true)
    assertEquals(trace.contrast_source_kind, Some(ContrastiveSupportAdmissibility.SourceKind.TopEngineMoveWithConcreteConsequence))
    assertEquals(trace.contrast_anchor, Some("Re8"))
    assertEquals(trace.contrast_consequence, Some("That keeps space pressure anchored around e-file."))
    assertEquals(trace.effectiveSupport(None), Some("The move Re8 stays best because it keeps space pressure anchored around e-file."))
  }

  test("renders canonical pin consequence from typed tactical anchors") {
    val localFact =
      MoveReviewLocalFact.admitted(
        MoveReviewLocalFact.Candidate(
          family = MoveReviewLocalFact.Family.Threat,
          source = MoveReviewLocalFact.Source.CanonicalFact,
          producer = MoveReviewLocalFact.Producer.TacticalMotif,
          subject = MoveReviewLocalFact.Subject.LineOrReply,
          strictFallbackCandidate = true,
          anchors =
            List(
              MoveReviewLocalFact.Anchor("tactical_kind", "pin"),
              MoveReviewLocalFact.Anchor("pinned_square", "a2"),
              MoveReviewLocalFact.Anchor("pinned_role", "pawn"),
              MoveReviewLocalFact.Anchor("behind_square", "a1"),
              MoveReviewLocalFact.Anchor("behind_role", "rook")
            ),
          lineBinding = MoveReviewLocalFact.LineBinding.PvCoupled,
          evidenceRefs = List("tactical_kind:pin", "pinned_role:pawn", "behind_role:rook"),
          guardrails = List("tactical_kind:pin", "current_move_motif_owned", "pv_confirms_tactic")
        )
      )
    val result =
      MoveReviewExplanationBuilder.Result(
        explanation =
          MoveReviewExplanation(
            title = "Qa5+ pins a pawn",
            prose = "Qa5+ pins the a2 pawn to the a1 rook.",
            source = "canonical_fact"
          ),
        localFact = localFact
      )
    val comparison =
      DecisionComparison(
        chosenMove = Some("Qa5+"),
        engineBestMove = Some("Qa5+"),
        engineBestScoreCp = Some(35),
        engineBestPv = List("Qa5+", "Qd2"),
        cpLossVsChosen = Some(0),
        deferredMove = Some("Qa5+"),
        deferredReason = Some("it trails the engine line by about 0 cp"),
        deferredSource = Some("top_engine_move"),
        evidence = None,
        practicalAlternative = false,
        chosenMatchesBest = true
      )

    val trace =
      ContrastiveSupportAdmissibility.decide(
        plan(
          AuthorQuestionKind.WhyThis,
          sourceKinds = List("typed_local_fact", "canonical_fact"),
          plannerOwnerKind = PlannerOwnerKind.ConcreteTactical,
          plannerSource = "canonical_fact"
        ).copy(claim = result.explanation.prose),
        inputs(decisionComparison = Some(comparison), localFactResult = Some(result)),
        None
      )

    assertEquals(trace.contrast_admissible, true)
    assertEquals(trace.contrast_consequence, Some("That keeps the a2 pawn pinned to the a1 rook."))
    assertEquals(trace.effectiveSupport(None), Some("The move Qa5+ stays best because it keeps the a2 pawn pinned to the a1 rook."))
  }

  test("admits explicit reply loss for WhyNow threat support") {
    val threat =
      ThreatRow(
        kind = "counterplay",
        side = "them",
        square = None,
        lossIfIgnoredCp = 140,
        turnsToImpact = 1,
        bestDefense = Some("Qe6"),
        defenseCount = 1,
        insufficientData = false
      )

    val trace = ContrastiveSupportAdmissibility.decide(plan(AuthorQuestionKind.WhyNow), inputs(opponentThreats = List(threat)), None)

    assertEquals(trace.contrast_admissible, true)
    assertEquals(trace.contrast_source_kind, Some(ContrastiveSupportAdmissibility.SourceKind.ExplicitReplyLoss))
    assertEquals(trace.contrast_anchor, Some("Qe6"))
    assertEquals(trace.contrast_forced_reply, true)
    assert(trace.contrast_guardrails.contains("forced_reply_unique"), clues(trace))
    assert(trace.contrast_evidence_refs.contains("reply_defense_count:1"), clues(trace))
    assert(trace.contrast_consequence.exists(_.contains("counterplay threat lands")))
    assert(trace.effectiveSupport(None).exists(_.startsWith("If delayed, Qe6 is the reply")))
  }

  test("keeps UCI best defense as a concrete forced-reply anchor") {
    val threat =
      ThreatRow(
        kind = "material",
        side = "them",
        square = None,
        lossIfIgnoredCp = 180,
        turnsToImpact = 1,
        bestDefense = Some("e8d8"),
        defenseCount = 1,
        insufficientData = false
      )

    val trace = ContrastiveSupportAdmissibility.decide(plan(AuthorQuestionKind.WhyNow), inputs(opponentThreats = List(threat)), None)

    assertEquals(trace.contrast_admissible, true)
    assertEquals(trace.contrast_source_kind, Some(ContrastiveSupportAdmissibility.SourceKind.ExplicitReplyLoss))
    assertEquals(trace.contrast_anchor, Some("d8"))
    assertEquals(trace.contrast_forced_reply, true)
    assert(trace.contrast_guardrails.contains("reply_anchor_kind:uci"), clues(trace))
    assert(trace.contrast_evidence_refs.contains("reply_uci:e8d8"), clues(trace))
    assertEquals(trace.effectiveSupport(None), Some("If delayed, the forced reply goes to d8."))
  }

  test("bounds square-only best defense wording instead of naming the square as the reply") {
    val threat =
      ThreatRow(
        kind = "material",
        side = "them",
        square = Some("d8"),
        lossIfIgnoredCp = 180,
        turnsToImpact = 1,
        bestDefense = Some("d8"),
        defenseCount = 1,
        insufficientData = false
      )

    val trace = ContrastiveSupportAdmissibility.decide(plan(AuthorQuestionKind.WhyNow), inputs(opponentThreats = List(threat)), None)

    assertEquals(trace.contrast_admissible, true)
    assertEquals(trace.contrast_source_kind, Some(ContrastiveSupportAdmissibility.SourceKind.ExplicitReplyLoss))
    assertEquals(trace.contrast_anchor, Some("d8"))
    assertEquals(trace.contrast_forced_reply, false)
    assert(trace.contrast_guardrails.contains("forced_reply_non_unique"), clues(trace))
    assert(trace.contrast_guardrails.contains("reply_anchor_kind:square"), clues(trace))
    assertEquals(trace.effectiveSupport(None), Some("If delayed, the reply has to address d8."))
  }

  test("bounds forced-reply contrast when the defense is not unique") {
    val threat =
      ThreatRow(
        kind = "material",
        side = "them",
        square = None,
        lossIfIgnoredCp = 180,
        turnsToImpact = 1,
        bestDefense = Some("Qe6"),
        defenseCount = 2,
        insufficientData = false
      )

    val trace = ContrastiveSupportAdmissibility.decide(plan(AuthorQuestionKind.WhyNow), inputs(opponentThreats = List(threat)), None)

    assertEquals(trace.contrast_admissible, true)
    assertEquals(trace.contrast_source_kind, Some(ContrastiveSupportAdmissibility.SourceKind.ExplicitReplyLoss))
    assertEquals(trace.contrast_anchor, Some("Qe6"))
    assertEquals(trace.contrast_forced_reply, false)
    assert(trace.contrast_guardrails.contains("forced_reply_non_unique"), clues(trace))
    assert(trace.contrast_evidence_refs.contains("reply_defense_count:2"), clues(trace))
    assertEquals(trace.effectiveSupport(None), Some("If delayed, Qe6 is one defensive reply."))
  }

  test("bounds non-unique UCI reply as a target to address instead of naming a square as a move") {
    val threat =
      ThreatRow(
        kind = "material",
        side = "them",
        square = Some("d6"),
        lossIfIgnoredCp = 180,
        turnsToImpact = 1,
        bestDefense = Some("e7d6"),
        defenseCount = 3,
        insufficientData = false
      )

    val trace = ContrastiveSupportAdmissibility.decide(plan(AuthorQuestionKind.WhyNow), inputs(opponentThreats = List(threat)), None)

    assertEquals(trace.contrast_admissible, true)
    assertEquals(trace.contrast_forced_reply, false)
    assert(trace.contrast_guardrails.contains("reply_anchor_kind:uci"), clues(trace))
    assertEquals(trace.effectiveSupport(None), Some("If delayed, one defensive reply has to address d6."))
  }

  test("non-unique UCI reply support names the threat square, not the reply destination") {
    val threat =
      ThreatRow(
        kind = "material",
        side = "them",
        square = Some("f6"),
        lossIfIgnoredCp = 176,
        turnsToImpact = 1,
        bestDefense = Some("d7d6"),
        defenseCount = 2,
        insufficientData = false
      )

    val trace = ContrastiveSupportAdmissibility.decide(plan(AuthorQuestionKind.WhatMustBeStopped, plannerSource = "threat"), inputs(opponentThreats = List(threat)), None)

    assertEquals(trace.contrast_admissible, true)
    assertEquals(trace.contrast_anchor, Some("d6"))
    assert(trace.contrast_evidence_refs.contains("threat_square:f6"), clues(trace))
    assertEquals(trace.effectiveSupport(None), Some("If delayed, one defensive reply has to answer the material threat on f6."))
  }

  test("renders actual gxf6 only-move fallback as immediate necessity, not delayed availability") {
    val trace =
      ContrastiveSupportAdmissibility.decide(
        plan(AuthorQuestionKind.WhyNow, plannerSource = "only_move_defense"),
        inputs(),
        Some(
          DecisiveTruthContract(
            playedMove = Some("gxf6"),
            verifiedBestMove = Some("gxf6"),
            truthClass = DecisiveTruthClass.Best,
            cpLoss = 342,
            swingSeverity = 342,
            reasonFamily = DecisiveReasonKind.OnlyMoveDefense,
            allowConcreteBenchmark = false,
            chosenMatchesBest = true,
            compensationAllowed = false,
            truthPhase = None,
            ownershipRole = TruthOwnershipRole.NoneRole,
            visibilityRole = TruthVisibilityRole.PrimaryVisible,
            surfaceMode = TruthSurfaceMode.Neutral,
            exemplarRole = TruthExemplarRole.VerifiedExemplar,
            surfacedMoveOwnsTruth = true,
            verifiedPayoffAnchor = None,
            compensationProseAllowed = false,
            benchmarkProseAllowed = false,
            investmentTruthChainKey = None,
            maintenanceExemplarCandidate = false,
            benchmarkCriticalMove = true,
            failureMode = FailureInterpretationMode.NoClearPlan,
            failureIntentConfidence = 0.0,
            failureIntentAnchor = None,
            failureInterpretationAllowed = false
          )
        )
      )

    assertEquals(trace.contrast_admissible, true)
    assertEquals(trace.contrast_source_kind, Some(ContrastiveSupportAdmissibility.SourceKind.DelayedOnlyMove))
    assertEquals(trace.contrast_anchor, Some("gxf6"))
    assertEquals(trace.contrast_consequence, Some("it is the only move that still holds the position together"))
    assertEquals(trace.effectiveSupport(None), Some("Only the played move still keeps the position together now."))
  }

  test("rejects generic only-move fallback without concrete loss magnitude") {
    val trace =
      ContrastiveSupportAdmissibility.decide(
        plan(AuthorQuestionKind.WhyNow, plannerSource = "only_move_defense"),
        inputs(),
        Some(
          DecisiveTruthContract(
            playedMove = Some("d6"),
            verifiedBestMove = Some("d6"),
            truthClass = DecisiveTruthClass.Best,
            cpLoss = 0,
            swingSeverity = 50,
            reasonFamily = DecisiveReasonKind.OnlyMoveDefense,
            allowConcreteBenchmark = false,
            chosenMatchesBest = true,
            compensationAllowed = false,
            truthPhase = None,
            ownershipRole = TruthOwnershipRole.NoneRole,
            visibilityRole = TruthVisibilityRole.PrimaryVisible,
            surfaceMode = TruthSurfaceMode.Neutral,
            exemplarRole = TruthExemplarRole.VerifiedExemplar,
            surfacedMoveOwnsTruth = true,
            verifiedPayoffAnchor = None,
            compensationProseAllowed = false,
            benchmarkProseAllowed = false,
            investmentTruthChainKey = None,
            maintenanceExemplarCandidate = false,
            benchmarkCriticalMove = true,
            failureMode = FailureInterpretationMode.NoClearPlan,
            failureIntentConfidence = 0.0,
            failureIntentAnchor = None,
            failureInterpretationAllowed = false
          )
        )
      )

    assertEquals(trace.contrast_admissible, false)
    assertEquals(trace.contrast_reject_reason, Some(ContrastiveSupportAdmissibility.RejectReason.MissingContrastCandidate))
    assertEquals(trace.effectiveSupport(None), None)
  }

  test("admits chosen-best contrast from certified planner consequence when deferred reason is missing") {
    val comparison =
      DecisionComparison(
        chosenMove = Some("O-O-O"),
        engineBestMove = Some("O-O-O"),
        engineBestScoreCp = Some(-57),
        engineBestPv = List("O-O-O"),
        cpLossVsChosen = None,
        deferredMove = None,
        deferredReason = None,
        deferredSource = None,
        evidence = None,
        practicalAlternative = false,
        chosenMatchesBest = true
      )

    val trace =
      ContrastiveSupportAdmissibility.decide(
        plan(
          AuthorQuestionKind.WhyNow,
          consequence =
            Some(
              QuestionPlanConsequence(
                "That preserves roughly 80cp of engine margin that drifting would give back.",
                QuestionPlanConsequenceBeat.WrapUp
              )
            )
        ),
        inputs(decisionComparison = Some(comparison)),
        None
      )

    assertEquals(trace.contrast_admissible, true)
    assertEquals(
      trace.contrast_source_kind,
      Some(ContrastiveSupportAdmissibility.SourceKind.TopEngineMoveWithConcreteConsequence)
    )
    assertEquals(trace.contrast_anchor, Some("O-O-O"))
    assert(trace.contrast_consequence.exists(_.contains("preserves roughly 80cp")))
    assert(
      trace.effectiveSupport(None).contains(
        "If delayed, O-O-O is still the move that preserves roughly 80cp of engine margin that drifting would give back."
      )
    )
  }

  test("keeps vague engine preference rejected even when planner consequence exists") {
    val comparison =
      DecisionComparison(
        chosenMove = Some("Re1"),
        engineBestMove = Some("Qf3"),
        engineBestScoreCp = Some(80),
        engineBestPv = List("Qf3", "...Re8"),
        cpLossVsChosen = Some(80),
        deferredMove = Some("Qf3"),
        deferredReason = Some("it trails the engine line by about 80 cp"),
        deferredSource = Some("top_engine_move"),
        evidence = None,
        practicalAlternative = false,
        chosenMatchesBest = false
      )

    val trace =
      ContrastiveSupportAdmissibility.decide(
        plan(
          AuthorQuestionKind.WhyThis,
          consequence =
            Some(
              QuestionPlanConsequence(
                "That preserves roughly 80cp of engine margin that drifting would give back.",
                QuestionPlanConsequenceBeat.WrapUp
              )
            )
        ),
        inputs(decisionComparison = Some(comparison)),
        None
      )

    assertEquals(trace.contrast_admissible, false)
    assertEquals(
      trace.contrast_reject_reason,
      Some(ContrastiveSupportAdmissibility.RejectReason.VagueEnginePreference)
    )
  }

  test("admits chosen-best contrast from counterfactual consequence when primary consequence is absent") {
    val comparison =
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
    val counterfactual =
      CounterfactualMatch(
        userMove = "Bd7",
        bestMove = "Qxd4",
        cpLoss = 70,
        missedMotifs = Nil,
        userMoveMotifs = Nil,
        severity = "moderate",
        userLine = VariationLine(Nil, 0)
      )

    val trace =
      ContrastiveSupportAdmissibility.decide(
        plan(AuthorQuestionKind.WhyThis),
        inputs(decisionComparison = Some(comparison), counterfactual = Some(counterfactual)),
        None
      )

    assertEquals(trace.contrast_admissible, true)
    assertEquals(
      trace.contrast_source_kind,
      Some(ContrastiveSupportAdmissibility.SourceKind.TopEngineMoveWithConcreteConsequence)
    )
    assertEquals(trace.contrast_anchor, Some("Bd7"))
    assertEquals(
      trace.effectiveSupport(None),
      Some("The move Bd7 stays best because Qxd4 becomes the cleaner continuation instead.")
    )
  }

  test("admits motif-backed counterfactual causal threat as a concrete what-if source") {
    val comparison =
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
    val counterfactual =
      CounterfactualMatch(
        userMove = "Bd7",
        bestMove = "Qxd4",
        cpLoss = 90,
        missedMotifs =
          List(
            Motif.Fork(
              Knight,
              List(Rook, Queen),
              Square.F5,
              List(Square.E7, Square.H4),
              Color.White,
              1,
              Some("Nf5")
            )
          ),
        userMoveMotifs = Nil,
        severity = "moderate",
        userLine = VariationLine(Nil, 0),
        causalThreat =
          Some(
            ThreatExtractor.CausalThreat(
              concept = "Material Loss",
              severity = 85,
              narrative = "allows a fork on the rook and queen",
              motifs =
                List(
                  Motif.Fork(
                    Knight,
                    List(Rook, Queen),
                    Square.F5,
                    List(Square.E7, Square.H4),
                    Color.White,
                    1,
                    Some("Nf5")
                  )
                )
            )
          )
      )

    val trace =
      ContrastiveSupportAdmissibility.decide(
        plan(AuthorQuestionKind.WhyThis),
        inputs(decisionComparison = Some(comparison), counterfactual = Some(counterfactual)),
        None
      )

    assertEquals(trace.contrast_admissible, true)
    assertEquals(
      trace.contrast_source_kind,
      Some(ContrastiveSupportAdmissibility.SourceKind.CounterfactualCausalThreat)
    )
    assertEquals(trace.contrast_anchor, Some("Bd7"))
    assertEquals(trace.contrast_consequence, Some("Missing it allows a fork on the rook and queen."))
    assertEquals(
      trace.effectiveSupport(None),
      Some("The move Bd7 stays best because missing it allows a fork on the rook and queen.")
    )
  }

  test("keeps counterfactual causal threat support closed without motif proof") {
    val comparison =
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
    val counterfactual =
      CounterfactualMatch(
        userMove = "Bd7",
        bestMove = "Qxd4",
        cpLoss = 90,
        missedMotifs = Nil,
        userMoveMotifs = Nil,
        severity = "moderate",
        userLine = VariationLine(Nil, 0),
        causalThreat =
          Some(
            ThreatExtractor.CausalThreat(
              concept = "Positional Collapse",
              severity = 1,
              narrative = "concedes a positional advantage",
              motifs = Nil
            )
          )
      )

    val trace =
      ContrastiveSupportAdmissibility.decide(
        plan(AuthorQuestionKind.WhyThis),
        inputs(decisionComparison = Some(comparison), counterfactual = Some(counterfactual)),
        None
      )

    assertEquals(trace.contrast_admissible, true)
    assertEquals(
      trace.contrast_source_kind,
      Some(ContrastiveSupportAdmissibility.SourceKind.TopEngineMoveWithConcreteConsequence)
    )
    assertEquals(
      trace.effectiveSupport(None),
      Some("The move Bd7 stays best because Qxd4 becomes the cleaner continuation instead.")
    )
  }

  test("admits WhatChanged decision comparison when the alternative has a concrete consequence") {
    val comparison =
      DecisionComparison(
        chosenMove = Some("Re1"),
        engineBestMove = Some("Re1"),
        engineBestScoreCp = Some(80),
        engineBestPv = List("Re1", "...Rxe1+"),
        cpLossVsChosen = Some(80),
        deferredMove = Some("Qf3"),
        deferredReason = Some("it wins the d5 pawn by force"),
        deferredSource = Some("top_engine_move"),
        evidence = None,
        practicalAlternative = false,
        chosenMatchesBest = true
      )

    val trace =
      ContrastiveSupportAdmissibility.decide(
        plan(
          AuthorQuestionKind.WhatChanged,
          sourceKinds = List("decision_comparison"),
          plannerOwnerKind = PlannerOwnerKind.DecisionTiming,
          plannerSource = "decision_comparison"
        ),
        inputs(decisionComparison = Some(comparison)),
        None
      )

    assertEquals(trace.contrast_admissible, true)
    assertEquals(
      trace.contrast_source_kind,
      Some(ContrastiveSupportAdmissibility.SourceKind.TopEngineMoveWithConcreteConsequence)
    )
    assertEquals(trace.contrast_anchor, Some("Qf3"))
    assert(trace.effectiveSupport(None).exists(_.contains("Qf3")), clues(trace))
  }

  test("admits exact comparative consequence instead of treating it as raw close-candidate prose") {
    val comparison =
      DecisionComparison(
        chosenMove = Some("Nd2"),
        engineBestMove = Some("Nd2"),
        engineBestScoreCp = Some(42),
        engineBestPv = List("Nd2", "...Qd6"),
        cpLossVsChosen = None,
        deferredMove = Some("Qc2"),
        deferredReason = Some("different strategic branches"),
        deferredSource = Some("close_candidate"),
        evidence = None,
        practicalAlternative = true,
        chosenMatchesBest = true,
        comparedMove = Some("Qc2"),
        comparativeConsequence = Some("Nd2 fixes d6 as the target; Qc2 leaves d6 unfixed on the compared branch."),
        comparativeSource = Some(DecisionComparisonComparativeSupport.ExactTargetFixationSource)
      )

    val trace =
      ContrastiveSupportAdmissibility.decide(
        plan(
          AuthorQuestionKind.WhyThis,
          sourceKinds = List("decision_comparison"),
          plannerOwnerKind = PlannerOwnerKind.DecisionTiming,
          plannerSource = "decision_comparison"
        ),
        inputs(decisionComparison = Some(comparison)),
        None
      )

    assertEquals(trace.contrast_admissible, true)
    assertEquals(
      trace.contrast_source_kind,
      Some(ContrastiveSupportAdmissibility.SourceKind.TopEngineMoveWithConcreteConsequence)
    )
    assertEquals(trace.contrast_anchor, Some("Nd2"))
    assertEquals(
      trace.contrast_consequence,
      Some("Nd2 fixes d6 as the target; Qc2 leaves d6 unfixed on the compared branch.")
    )
  }

  test("admits role-aware line consequence as branch-scoped alternative contrast") {
    val consequence =
      "g5 reaches an exchange sequence on the engine-best branch 10... g5 11. b4 11... gxh4 12. bxa5; Nge7 stays on the played branch 10... Nge7 11. O-O 11... Bb6 12. a4 without that concrete exchange sequence."
    val comparison =
      DecisionComparison(
        chosenMove = Some("Nge7"),
        engineBestMove = Some("g5"),
        engineBestScoreCp = Some(207),
        engineBestPv = List("g5", "b4", "gxh4", "bxa5"),
        cpLossVsChosen = Some(21),
        deferredMove = Some("g5"),
        deferredReason = None,
        deferredSource = Some("verified_best"),
        evidence = None,
        practicalAlternative = false,
        chosenMatchesBest = false,
        comparedMove = Some("Nge7"),
        comparativeConsequence = Some(consequence),
        comparativeSource = Some(DecisionComparisonComparativeSupport.RoleAwareLineConsequenceSource),
        roleAwareBranchEvidence =
          Some(
            RoleAwareLineConsequenceEvidence(
              engineBest =
                LineConsequenceEvidence(
                  lineId = Some("best_branch"),
                  sanMoves = List("g5", "b4", "gxh4", "bxa5"),
                  uciMoves = List("g7g5", "b2b4", "g5h4", "b4a5"),
                  scoreCp = Some(207),
                  mate = None,
                  depth = Some(20),
                  windowPly = 20,
                  kind = LineConsequenceKind.ExchangeSequence,
                  triggerSan = Some("g5"),
                  consequence = "g5 reaches an exchange sequence.",
                  whyItMatters = None,
                  release = LineConsequenceRelease.SurfaceCandidate,
                  rejectReasons = Nil
                ),
              played =
                LineConsequenceEvidence(
                  lineId = Some("played_branch"),
                  sanMoves = List("Nge7", "O-O", "Bb6", "a4"),
                  uciMoves = List("g8e7", "e1g1", "b4b6", "a2a4"),
                  scoreCp = Some(228),
                  mate = None,
                  depth = Some(20),
                  windowPly = 20,
                  kind = LineConsequenceKind.PreviewOnly,
                  triggerSan = Some("Nge7"),
                  consequence = "Nge7 stays on the played branch.",
                  whyItMatters = None,
                  release = LineConsequenceRelease.SurfaceCandidate,
                  rejectReasons = Nil
                )
            )
          )
      )

    val trace =
      ContrastiveSupportAdmissibility.decide(
        plan(
          AuthorQuestionKind.WhatChanged,
          sourceKinds = List("decision_comparison"),
          plannerOwnerKind = PlannerOwnerKind.DecisionTiming,
          plannerSource = "decision_comparison"
        ),
        inputs(decisionComparison = Some(comparison)),
        None
      )

    assertEquals(trace.contrast_admissible, true)
    assertEquals(trace.contrast_source_kind, Some(ContrastiveSupportAdmissibility.SourceKind.RoleAwareLineConsequence))
    assertEquals(trace.contrast_anchor, Some("g5"))
    assertEquals(trace.contrast_consequence, Some(consequence))
    assert(trace.contrast_guardrails.contains("alternative_role:engine_best_branch"), clues(trace))
    assert(trace.contrast_guardrails.contains("alternative_role:played_branch"), clues(trace))
    assert(trace.contrast_evidence_refs.contains("engine_best_move:g5"), clues(trace))
    assert(trace.contrast_evidence_refs.contains("played_move:Nge7"), clues(trace))
    assert(trace.contrast_evidence_refs.contains("engine_best:line_consequence_line_id:best_branch"), clues(trace))
    assert(trace.contrast_evidence_refs.contains("played:line_consequence_line_id:played_branch"), clues(trace))
    assert(trace.contrast_guardrails.contains("engine_best:line_consequence_kind:exchange_sequence"), clues(trace))
    assert(trace.contrast_guardrails.contains("played:line_consequence_kind:preview_only"), clues(trace))
    assertEquals(trace.effectiveSupport(None), Some(consequence))
  }

  test("rejects role-aware line consequence text without a concrete branch line") {
    val consequence =
      "g5 reaches an exchange sequence on the engine-best branch; Nge7 stays on the played branch without that concrete exchange sequence."

    assert(!DecisionComparisonComparativeSupport.roleAwareLineConsequenceText(consequence), clues(consequence))
  }
