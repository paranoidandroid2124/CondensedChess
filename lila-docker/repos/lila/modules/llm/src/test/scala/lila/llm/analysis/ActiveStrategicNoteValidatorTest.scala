package lila.llm.analysis

import munit.FunSuite

import lila.llm.*

class ActiveStrategicNoteValidatorTest extends FunSuite:

  private val strategyPack = Some(
    StrategyPack(
      sideToMove = "white",
      strategicIdeas = List(
        StrategyIdeaSignal(
          ideaId = "idea_1",
          ownerSide = "white",
          kind = StrategicIdeaKind.SpaceGainOrRestriction,
          group = StrategicIdeaGroup.StructuralChange,
          readiness = StrategicIdeaReadiness.Build,
          focusSquares = List("e3", "g4"),
          focusZone = Some("kingside"),
          confidence = 0.88
        )
      ),
      directionalTargets = List(
        StrategyDirectionalTarget(
          targetId = "target_white_n_d2_g4",
          ownerSide = "white",
          piece = "N",
          from = "d2",
          targetSquare = "g4",
          readiness = DirectionalTargetReadiness.Build
        )
      ),
      pieceRoutes = List(
        StrategyPieceRoute(
          side = "white",
          piece = "N",
          from = "d2",
          route = List("d2", "f1", "e3"),
          purpose = "kingside clamp",
          confidence = 0.82
        )
      ),
      signalDigest = Some(
        NarrativeSignalDigest(
          dominantIdeaKind = Some(StrategicIdeaKind.SpaceGainOrRestriction),
          dominantIdeaGroup = Some(StrategicIdeaGroup.StructuralChange),
          dominantIdeaReadiness = Some(StrategicIdeaReadiness.Build),
          dominantIdeaFocus = Some("e3, g4"),
          opponentPlan = Some("...c5 counterplay"),
          decisionComparison = Some(
            DecisionComparisonDigest(
              chosenMove = Some("Nf1"),
              engineBestMove = Some("g4"),
              deferredMove = Some("g4"),
              deferredReason = Some("it keeps the initiative without opening the center too early")
            )
          )
        )
      )
    )
  )

  private val routeRefs = List(
    ActiveStrategicRouteRef(
      routeId = "route_1",
      piece = "N",
      route = List("d2", "f1", "e3"),
      purpose = "kingside clamp",
      confidence = 0.82
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

  private val dossier = Some(
    ActiveBranchDossier(
      dominantLens = "strategic",
      chosenBranchLabel = "queenside minority attack branch",
      engineBranchLabel = Some("engine a4 -> immediate queenside expansion"),
      deferredBranchLabel = Some("deferred a4 -> fix the queenside structure first"),
      opponentResource = Some("lever a5"),
      routeCue = Some(
        ActiveBranchRouteCue(
          routeId = "route_1",
          piece = "N",
          route = List("d2", "f1", "e3"),
          purpose = "kingside clamp",
          confidence = 0.82
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

  private def validate(
      candidateText: String,
      baseNarrative: String = "White stabilizes the center before choosing a kingside plan.",
      dossierValue: Option[ActiveBranchDossier] = None,
      strategyReasons: List[String] = Nil
  ) =
    ActiveStrategicNoteValidator.validate(
      candidateText = candidateText,
      baseNarrative = baseNarrative,
      dossier = dossierValue,
      strategyPack = strategyPack,
      routeRefs = routeRefs,
      moveRefs = moveRefs,
      strategyReasons = strategyReasons
    )

  test("accepts idea-led note with no route mention") {
    val result =
      validate(
        candidateText =
          "Because the position is ready for space gain and restriction around e3 and g4, White should keep building that kingside clamp before Black gets ...c5. If the move order drifts, the counterplay comes back immediately."
      )

    assert(result.isAccepted)
    assert(!ActiveStrategicNoteValidator.shouldRepair(result))
  }

  test("accepts ordered route mention without exact routeId") {
    val result =
      validate(
        candidateText =
          "The space gain and restriction plan still runs through the kingside, and the knight can head from d2 through f1 to e3 to support it before Black can untangle. If White hesitates, ...c5 restores the counterplay."
      )

    assert(result.isAccepted)
  }

  test("dossier comparison gaps stay as warnings only") {
    val result =
      validate(
        candidateText =
          "The space gain and restriction plan around e3 and g4 is the real point, because White wants the kingside clamp before Black gets ...c5 counterplay and still needs Nf1 in the right structure. If White drifts, the queenside counterplay revives.",
        dossierValue = dossier
      )

    assert(result.isAccepted)
    assertEquals(result.hardReasons, Nil)
    assert(result.warningReasons.contains("active_opponent_resource_missing"))
    assert(!ActiveStrategicNoteValidator.shouldRepair(result))
  }

  test("prior phrase reuse remains a hard fail") {
    val result =
      validate(
        candidateText =
          "White stabilizes the center before choosing a kingside plan. Nf1 starts the knight route d2 f1 e3.",
        baseNarrative = "White stabilizes the center before choosing a kingside plan."
      )

    assert(result.hardReasons.contains("active_note_prior_phrase_reuse"))
    assert(ActiveStrategicNoteValidator.shouldRepair(result))
  }

  test("strategy coverage low remains a hard fail") {
    val result =
      validate(
        candidateText =
          "White improves the position a little. The move keeps options open.",
        strategyReasons = List("strategy_coverage_low")
      )

    assert(result.hardReasons.contains("strategy_coverage_low"))
    assert(ActiveStrategicNoteValidator.shouldRepair(result))
  }

  test("missing forward plan is a hard fail") {
    val result =
      validate(
        candidateText =
          "The space gain and restriction plan around e3 and g4 is the key strategic idea, and Black still has ...c5 counterplay."
      )

    assert(result.hardReasons.contains("forward_plan_missing"))
    assert(ActiveStrategicNoteValidator.shouldRepair(result))
  }

  test("missing dominant idea is a hard fail") {
    val result =
      validate(
        candidateText =
          "White should keep building toward the kingside before Black gets ...c5. If the move order drifts, the counterplay returns."
      )

    assert(result.hardReasons.contains("dominant_idea_missing"))
    assert(ActiveStrategicNoteValidator.shouldRepair(result))
  }

  test("missing opponent or trigger is a hard fail") {
    val result =
      validate(
        candidateText =
          "Because the position is ready for space gain and restriction around e3 and g4, White should keep building that kingside clamp. The knight can head toward e3 to support it."
      )

    assert(result.hardReasons.contains("opponent_or_trigger_missing"))
    assert(ActiveStrategicNoteValidator.shouldRepair(result))
  }

  test("sentence count outside target range is warning only") {
    val result =
      validate(
        candidateText =
          "Because the position is ready for space gain and restriction around e3 and g4, White should keep building that kingside clamp before Black gets ...c5 counterplay."
      )

    assert(result.isAccepted)
    assertEquals(result.hardReasons, Nil)
    assert(result.warningReasons.contains("active_note_sentence_count"))
    assert(!ActiveStrategicNoteValidator.shouldRepair(result))
  }

  test("owner mismatch without campaign owner mention is a hard fail") {
    val mismatchedPack =
      strategyPack.map(_.copy(
        sideToMove = "black",
        strategicIdeas = strategyPack.get.strategicIdeas.map(_.copy(ownerSide = "white"))
      ))

    val result =
      ActiveStrategicNoteValidator.validate(
        candidateText =
          "Because the space gain and restriction plan around e3 and g4 is ready, the position should keep building that kingside clamp before ...c5 returns.",
        baseNarrative = "Black to move, but White still owns the campaign.",
        dossier = dossier,
        strategyPack = mismatchedPack,
        routeRefs = routeRefs,
        moveRefs = moveRefs,
        strategyReasons = Nil
      )

    assert(result.hardReasons.contains("campaign_owner_missing"))
    assert(ActiveStrategicNoteValidator.shouldRepair(result))
  }
