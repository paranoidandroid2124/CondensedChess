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
          beneficiaryPieces = List("R"),
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

  test("grounded note can pass without a separate opponent or trigger sentence") {
    val result =
      validate(
        candidateText =
          "Because the position is ready for space gain and restriction around e3 and g4, White should keep building that kingside clamp. The knight can head toward e3 to support it."
      )

    assert(result.isAccepted, clue(result))
    assert(!result.hardReasons.contains("opponent_or_trigger_missing"), clue(result))
  }

  test("anchorless note without opponent or trigger still fails") {
    val result =
      validate(
        candidateText =
          "The space gain and restriction plan around e3 and g4 is still the key strategic idea."
      )

    assert(
      result.hardReasons.contains("opponent_or_trigger_missing") ||
        result.hardReasons.contains("forward_plan_missing"),
      clue(result)
    )
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

  test("compensation-positive notes must mention the canonical compensation family") {
    val result =
      ActiveStrategicNoteValidator.validate(
        candidateText =
          "The key idea is fixed queenside targets. A likely follow-up is queen toward b6 to lean on those fixed targets before winning the material back. If Black relaxes, the pressure disappears.",
        baseNarrative = "Black keeps the queenside pressure alive.",
        dossier = None,
        strategyPack = compensationPack,
        routeRefs = Nil,
        moveRefs = Nil,
        strategyReasons = Nil
      )

    assert(result.isAccepted, clue(result))
  }

  test("compensation-positive notes can pass without a separate opponent warning when the family is explicit") {
    val result =
      ActiveStrategicNoteValidator.validate(
        candidateText =
          "The key idea is fixed queenside targets. A likely follow-up is queen toward b6 to lean on those fixed targets before winning the material back.",
        baseNarrative = "Black keeps the queenside pressure alive.",
        dossier = None,
        strategyPack = compensationPack,
        routeRefs = Nil,
        moveRefs = Nil,
        strategyReasons = Nil
      )

    assert(result.isAccepted, clue(result))
    assert(!result.hardReasons.contains("opponent_or_trigger_missing"), clue(result))
  }

  test("compensation-positive notes can satisfy parity through family, anchor, and continuation") {
    val result =
      ActiveStrategicNoteValidator.validate(
        candidateText =
          "The compensation comes from queenside pressure against fixed targets. The key idea is fixed queenside targets, with the queen toward b6 as a concrete anchor. The next step is to keep the queenside targets tied down before winning the material back.",
        baseNarrative = "Black keeps the queenside pressure alive.",
        dossier =
          Some(
            ActiveBranchDossier(
              dominantLens = "compensation",
              chosenBranchLabel = "queenside bind",
              continuationFocus = Some("keep the queenside targets tied down before winning the material back")
            )
          ),
        strategyPack = compensationPack,
        routeRefs = Nil,
        moveRefs = Nil,
        strategyReasons = Nil
      )

    assert(result.isAccepted, clue(result))
    assert(!result.hardReasons.contains("opponent_or_trigger_missing"), clue(result))
  }

  test("compensation-positive notes can use from-there continuation wording") {
    val result =
      ActiveStrategicNoteValidator.validate(
        candidateText =
          "The compensation comes from queenside pressure against fixed targets. That pressure is anchored on queen toward b6. From there, keep the queenside targets tied down before winning the material back.",
        baseNarrative = "Black keeps the queenside pressure alive.",
        dossier = None,
        strategyPack = compensationPack,
        routeRefs = Nil,
        moveRefs = Nil,
        strategyReasons = Nil
      )

    assert(result.isAccepted, clue(result))
    assert(!result.hardReasons.contains("forward_plan_missing"), clue(result))
  }

  test("compensation-positive notes accept square-led anchor wording without can-use phrasing") {
    val result =
      ActiveStrategicNoteValidator.validate(
        candidateText =
          "The compensation comes from queenside pressure against fixed targets. The key idea is fixed queenside targets. That pressure is anchored on b2. From there, keep the queenside targets tied down before winning the material back.",
        baseNarrative = "Black keeps the queenside pressure alive.",
        dossier = None,
        strategyPack = compensationPack,
        routeRefs = Nil,
        moveRefs = Nil,
        strategyReasons = Nil
      )

    assert(result.isAccepted, clue(result))
    assert(!result.hardReasons.contains("opponent_or_trigger_missing"), clue(result))
  }

  test("compensation-positive notes with family but no concrete anchor are rejected") {
    val result =
      ActiveStrategicNoteValidator.validate(
        candidateText =
          "The compensation comes from queenside pressure against fixed targets. The key idea is fixed queenside targets. The next step is to keep pressing before winning the material back.",
        baseNarrative = "Black keeps the queenside pressure alive.",
        dossier = None,
        strategyPack = compensationPack,
        routeRefs = Nil,
        moveRefs = Nil,
        strategyReasons = Nil
      )

    assert(result.hardReasons.contains("opponent_or_trigger_missing"), clue(result))
    assert(ActiveStrategicNoteValidator.shouldRepair(result))
  }

  test("compensation-positive notes with family and anchor but no continuation are rejected") {
    val result =
      ActiveStrategicNoteValidator.validate(
        candidateText =
          "The compensation comes from queenside pressure against fixed targets. The key idea is fixed queenside targets, with the queen toward b6 as a concrete anchor.",
        baseNarrative = "Black keeps the queenside pressure alive.",
        dossier = None,
        strategyPack = compensationPack,
        routeRefs = Nil,
        moveRefs = Nil,
        strategyReasons = Nil
      )

    assert(result.hardReasons.contains("forward_plan_missing"), clue(result))
    assert(result.hardReasons.contains("opponent_or_trigger_missing"), clue(result))
    assert(ActiveStrategicNoteValidator.shouldRepair(result))
  }

  test("compensation-positive notes without family mention are rejected") {
    val result =
      ActiveStrategicNoteValidator.validate(
        candidateText =
          "Black should keep improving the position before White gets any activity. If the move order drifts, the edge disappears.",
        baseNarrative = "Black keeps the queenside pressure alive.",
        dossier = None,
        strategyPack = compensationPack,
        routeRefs = Nil,
        moveRefs = Nil,
        strategyReasons = Nil
      )

    assert(result.hardReasons.contains("compensation_family_missing"), clue(result))
    assert(ActiveStrategicNoteValidator.shouldRepair(result))
  }
