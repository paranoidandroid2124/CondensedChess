package lila.llm.analysis

import munit.FunSuite

import lila.llm.*
import lila.llm.model.authoring.AuthorQuestionKind

class ActiveStrategicNoteValidatorTest extends FunSuite:

  private val routeRefs = List(
    ActiveStrategicRouteRef(
      routeId = "route_1",
      ownerSide = "white",
      piece = "N",
      route = List("d2", "f1", "e3"),
      purpose = "kingside clamp",
      strategicFit = 0.82,
      tacticalSafety = 0.78,
      surfaceConfidence = 0.81,
      surfaceMode = RouteSurfaceMode.Toward
    )
  )

  private val moveRefs = List(
    ActiveStrategicMoveRef(
      label = "Engine preference",
      source = "top_engine_move",
      uci = "d2f1",
      san = Some("Nf1")
    )
  )

  private val strategyPack = Some(
    StrategyPack(
      sideToMove = "white",
      pieceRoutes = List(
        StrategyPieceRoute(
          ownerSide = "white",
          piece = "N",
          from = "d2",
          route = List("d2", "f1", "e3"),
          purpose = "kingside clamp",
          strategicFit = 0.82,
          tacticalSafety = 0.78,
          surfaceConfidence = 0.81,
          surfaceMode = RouteSurfaceMode.Exact,
          evidence = List("probe")
        )
      ),
      directionalTargets = List(
        StrategyDirectionalTarget(
          targetId = "target_e3",
          ownerSide = "white",
          piece = "N",
          from = "d2",
          targetSquare = "e3",
          readiness = DirectionalTargetReadiness.Build,
          strategicReasons = List("supports kingside clamp"),
          evidence = List("probe")
        )
      ),
      signalDigest = Some(
        NarrativeSignalDigest(
          decisionComparison = Some(
            DecisionComparisonDigest(
              chosenMove = Some("Nf1"),
              engineBestMove = Some("Nf1"),
              deferredMove = Some("g4"),
              deferredReason = Some("it keeps the initiative without opening the center too early")
            )
          )
        )
      )
    )
  )

  private val dossier = Some(
    ActiveBranchDossier(
      dominantLens = "planadvance",
      chosenBranchLabel = "plan advance -> e3",
      whyChosen = Some("This move advances the plan toward e3."),
      whyDeferred = Some("If White drifts, ...c5 counterplay returns."),
      practicalRisk = Some("If White drifts, ...c5 counterplay returns."),
      routeCue = Some(
        ActiveBranchRouteCue(
          routeId = "route_1",
          ownerSide = "white",
          piece = "N",
          route = List("d2", "f1", "e3"),
          purpose = "kingside clamp",
          strategicFit = 0.82,
          tacticalSafety = 0.78,
          surfaceConfidence = 0.81,
          surfaceMode = RouteSurfaceMode.Toward
        )
      ),
      moveCue = Some(
        ActiveBranchMoveCue(
          label = "Engine preference",
          uci = "d2f1",
          san = Some("Nf1"),
          source = "top_engine_move"
        )
      )
    )
  )

  private val compensationPack = Some(
    StrategyPack(
      sideToMove = "black",
      strategicIdeas = List(
        StrategyIdeaSignal(
          ideaId = "idea_comp",
          ownerSide = "black",
          kind = StrategicIdeaKind.TargetFixing,
          group = StrategicIdeaGroup.StructuralChange,
          readiness = StrategicIdeaReadiness.Build,
          focusSquares = List("b2"),
          focusFiles = List("b"),
          focusZone = Some("queenside"),
          beneficiaryPieces = List("Q"),
          confidence = 0.84
        )
      ),
      pieceMoveRefs = List(
        StrategyPieceMoveRef(
          ownerSide = "black",
          piece = "Q",
          from = "d8",
          target = "b6",
          idea = "fix the queenside targets",
          evidence = List("target_pawn")
        )
      ),
      longTermFocus = List("fix the queenside targets before recovering the pawn"),
      signalDigest = Some(
        NarrativeSignalDigest(
          compensation = Some("return vector through line pressure and delayed recovery"),
          compensationVectors = List("Line Pressure (0.7)", "Delayed Recovery (0.6)", "Fixed Targets (0.5)"),
          investedMaterial = Some(100),
          dominantIdeaKind = Some(StrategicIdeaKind.TargetFixing),
          dominantIdeaGroup = Some(StrategicIdeaGroup.StructuralChange),
          dominantIdeaReadiness = Some(StrategicIdeaReadiness.Build),
          dominantIdeaFocus = Some("b2")
        )
      )
    )
  )

  private def validate(
      candidateText: String,
      baseNarrative: String = "White improves the position without changing the structure.",
      dossierValue: Option[ActiveBranchDossier] = dossier,
      strategyPackValue: Option[StrategyPack] = strategyPack,
      strategyReasons: List[String] = Nil,
      plannerPrimaryKind: Option[AuthorQuestionKind] = None,
      plannerPrimary: Option[QuestionPlan] = None
  ) =
    ActiveStrategicNoteValidator.validate(
      candidateText = candidateText,
      baseNarrative = baseNarrative,
      dossier = dossierValue,
      strategyPack = strategyPackValue,
      routeRefs = routeRefs,
      moveRefs = moveRefs,
      strategyReasons = strategyReasons,
      plannerPrimaryKind = plannerPrimaryKind,
      plannerPrimary = plannerPrimary
    )

  private def plannerPrimary(
      questionKind: AuthorQuestionKind,
      claim: String,
      ownerFamily: OwnerFamily,
      contrast: Option[String] = None,
      evidence: Option[String] = None,
      consequence: Option[String] = None
  ): QuestionPlan =
    QuestionPlan(
      questionId = s"primary_${ownerFamily.wireName}",
      questionKind = questionKind,
      priority = 100,
      claim = claim,
      evidence = evidence.map(text =>
        QuestionPlanEvidence(
          text = text,
          purposes = List("test"),
          sourceKinds = List("test"),
          branchScoped = true
        )
      ),
      contrast = contrast,
      consequence = consequence.map(text =>
        QuestionPlanConsequence(
          text = text,
          beat = QuestionPlanConsequenceBeat.TeachingPoint
        )
      ),
      fallbackMode = QuestionPlanFallbackMode.PlannerOwned,
      strengthTier = QuestionPlanStrengthTier.Moderate,
      sourceKinds = List("test"),
      admissibilityReasons = List("test"),
      ownerFamily = ownerFamily,
      ownerSource = "test"
    )

  test("accepts a dossier-backed delta note with route anchor and trigger") {
    val result =
      validate(
        candidateText =
          "This move advances the plan toward e3. If White drifts, ...c5 counterplay returns."
      )

    assert(result.isAccepted, clue(result))
    assertEquals(result.hardReasons, Nil)
  }

  test("accepts a tactical lead note with an immediate proving line") {
    val result =
      validate(
        candidateText =
          "This is a blunder, and the tactical point has to come first. Qxd6 wins a pawn immediately.",
        dossierValue = None,
        strategyPackValue = None
      )

    assert(result.isAccepted, clue(result))
    assertEquals(result.hardReasons, Nil)
  }

  test("rejects generic state-summary coaching notes") {
    val result =
      validate(
        candidateText =
          "The key idea is space on the kingside. A likely follow-up is expanding there before Black reacts.",
        dossierValue = None
      )

    assert(result.hardReasons.contains("dominant_idea_missing"), clue(result))
    assert(ActiveStrategicNoteValidator.shouldRepair(result))
  }

  test("accepts compact compensation notes with family mention and concrete anchor") {
    val result =
      validate(
        candidateText =
          "The compensation comes from queenside pressure against fixed targets. This keeps queenside pressure against fixed targets in play. That pressure is anchored on b2.",
        baseNarrative = "Black keeps the queenside pressure alive.",
        dossierValue = None,
        strategyPackValue = compensationPack
      )

    assert(result.isAccepted, clue(result))
    assert(!result.hardReasons.contains("compensation_family_missing"), clue(result))
  }

  test("accepts planner-owned WhyNow notes with concrete timing evidence even without plan coverage") {
    val primary =
      plannerPrimary(
        questionKind = AuthorQuestionKind.WhyNow,
        claim = "The timing matters now because drifting lets Qe2 take over.",
        ownerFamily = OwnerFamily.DecisionTiming,
        contrast = Some("If delayed, the cleaner version runs through Qe2.")
      )
    val result =
      validate(
        candidateText =
          "The timing matters now because drifting lets Qe2 take over and costs about 84cp. If delayed, the cleaner version runs through Qe2.",
        dossierValue = None,
        strategyPackValue = None,
        plannerPrimaryKind = Some(AuthorQuestionKind.WhyNow),
        plannerPrimary = Some(primary)
      )

    assert(result.isAccepted, clue(result))
    assertEquals(result.hardReasons, Nil)
  }

  test("planner-owned WhyNow may reuse a prior proof sentence when the timing lead is new") {
    val candidate =
      "The timing matters now because delaying costs about 97cp. Maintain color complex clamp and restrict counterplay through Bd4 and Ba5."
    val prior =
      "The timing matters now because Other moves allow the position to slip away. a) Further probe work still targets Maintain color complex clamp and restrict counterplay through Bd4 and Ba5."

    val accepted =
      validate(
        candidateText = candidate,
        baseNarrative = prior,
        dossierValue = None,
        strategyPackValue = None,
        plannerPrimaryKind = Some(AuthorQuestionKind.WhyNow),
        plannerPrimary =
          Some(
            plannerPrimary(
              questionKind = AuthorQuestionKind.WhyNow,
              claim = "The timing matters now because other moves allow the position to slip away.",
              ownerFamily = OwnerFamily.DecisionTiming,
              evidence = Some("Maintain color complex clamp and restrict counterplay through Bd4 and Ba5.")
            )
          )
      )
    val rejected =
      validate(
        candidateText = candidate,
        baseNarrative = prior,
        dossierValue = None,
        strategyPackValue = None,
        plannerPrimaryKind = None
      )

    assert(accepted.isAccepted, clue(accepted))
    assertEquals(accepted.hardReasons, Nil)
    assert(rejected.hardReasons.contains("active_note_prior_phrase_reuse"), clue(rejected))
  }

  test("broken fragment notes are rejected by the shared user-facing hard gate") {
    val result = validate(candidateText = "After 13.", dossierValue = None)

    assert(result.hardReasons.contains("broken_fragment_detected"), clue(result))
    assert(ActiveStrategicNoteValidator.shouldRepair(result))
  }

  test("duplicate sentence notes are rejected by the shared user-facing hard gate") {
    val result =
      validate(
        candidateText =
          "This move advances the plan toward e3. This move advances the plan toward e3."
      )

    assert(result.hardReasons.contains("duplicate_sentence_detected"), clue(result))
    assert(ActiveStrategicNoteValidator.shouldRepair(result))
  }

  test("planner-owned tactical failure notes can attach on claim plus anchored local consequence") {
    val primary =
      plannerPrimary(
        questionKind = AuthorQuestionKind.WhyThis,
        claim = "This is a blunder because the queen runs into Qxd6.",
        ownerFamily = OwnerFamily.TacticalFailure,
        evidence = Some("Qxd6 wins a pawn immediately.")
      )
    val result =
      validate(
        candidateText =
          "This is a blunder because the queen runs into Qxd6. Qxd6 wins a pawn immediately.",
        dossierValue = None,
        strategyPackValue = strategyPack,
        strategyReasons = List("strategy_coverage_low", "strategy_plan_missing", "strategy_focus_missing"),
        plannerPrimaryKind = Some(AuthorQuestionKind.WhyThis),
        plannerPrimary = Some(primary)
      )

    assert(result.isAccepted, clue(result))
    assertEquals(result.hardReasons, Nil)
    assert(result.warningReasons.contains("side_plan_missing"), clue(result))
    assert(result.warningReasons.contains("side_focus_missing"), clue(result))
  }

  test("planner-owned move delta notes can attach without route or focus completeness") {
    val primary =
      plannerPrimary(
        questionKind = AuthorQuestionKind.WhyThis,
        claim = "The rook lift keeps the kingside pressure rolling.",
        ownerFamily = OwnerFamily.MoveDelta,
        consequence = Some("That immediately increases pressure on g7.")
      )
    val result =
      validate(
        candidateText =
          "The rook lift keeps the kingside pressure rolling. That immediately increases pressure on g7.",
        dossierValue = None,
        strategyPackValue = strategyPack,
        strategyReasons = List("strategy_coverage_low", "strategy_route_missing", "strategy_focus_missing"),
        plannerPrimaryKind = Some(AuthorQuestionKind.WhyThis),
        plannerPrimary = Some(primary)
      )

    assert(result.isAccepted, clue(result))
    assertEquals(result.hardReasons, Nil)
    assert(result.warningReasons.contains("side_route_missing"), clue(result))
    assert(result.warningReasons.contains("side_focus_missing"), clue(result))
  }
