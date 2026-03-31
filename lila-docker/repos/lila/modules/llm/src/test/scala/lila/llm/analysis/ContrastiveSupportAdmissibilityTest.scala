package lila.llm.analysis

import munit.FunSuite
import lila.llm.analysis.practical.ContrastiveSupportAdmissibility
import lila.llm.model.{ PreventedPlanInfo, ThreatRow }
import lila.llm.model.authoring.AuthorQuestionKind
import lila.llm.model.strategic.{ CounterfactualMatch, VariationLine }

class ContrastiveSupportAdmissibilityTest extends FunSuite:

  private def plan(
      kind: AuthorQuestionKind,
      consequence: Option[QuestionPlanConsequence] = None
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
      sourceKinds = List("planner"),
      admissibilityReasons = List("fixture"),
      ownerFamily = OwnerFamily.ForcingDefense,
      ownerSource = "fixture_owner"
    )

  private def inputs(
      decisionComparison: Option[DecisionComparison] = None,
      opponentThreats: List[ThreatRow] = Nil,
      preventedPlansNow: List[PreventedPlanInfo] = Nil,
      counterfactual: Option[CounterfactualMatch] = None
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
      factualFallback = None
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
    assert(trace.contrast_consequence.exists(_.contains("counterplay threat lands")))
    assert(trace.effectiveSupport(None).exists(_.startsWith("If delayed, Qe6 is the reply")))
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
                "That preserves roughly 80cp of practical value that drifting would give back.",
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
        "If delayed, O-O-O is still the move that preserves roughly 80cp of practical value that drifting would give back."
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
                "That preserves roughly 80cp of practical value that drifting would give back.",
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
