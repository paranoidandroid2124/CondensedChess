package lila.llm

import munit.FunSuite

class ActiveStrategicPromptTest extends FunSuite:

  private val tacticalFen = "r2qk2r/1b1nbppp/pp1ppn2/8/2PQ4/BPN2NP1/P3PPBP/R2R2K1 w kq - 2 11"
  private val tacticalFenAfter = "r2qk2r/1b1nbppp/pp1Qpn2/8/2P5/BPN2NP1/P3PPBP/R2R2K1 b kq - 0 11"

  private val samplePack = StrategyPack(
    sideToMove = "white",
    plans = List(
      StrategySidePlan(
        side = "white",
        horizon = "long",
        planName = "Kingside expansion",
        priorities = List("Push f-pawn", "Reroute knight"),
        riskTriggers = List("Back rank weakness")
      )
    ),
    pieceRoutes = List(
      StrategyPieceRoute(
        side = "white",
        piece = "N",
        from = "d2",
        route = List("d2", "f1", "e3"),
        purpose = "king defense",
        confidence = 0.78,
        evidence = List("piece_activity")
      )
    ),
    directionalTargets = List(
      StrategyDirectionalTarget(
        targetId = "target_white_n_d2_g4",
        ownerSide = "white",
        piece = "N",
        from = "d2",
        targetSquare = "g4",
        readiness = DirectionalTargetReadiness.Build,
        strategicReasons = List("supports kingside expansion"),
        prerequisites = List("prepare the supporting squares first")
      )
    ),
    strategicIdeas = List(
      StrategyIdeaSignal(
        ideaId = "idea_1",
        ownerSide = "white",
        kind = StrategicIdeaKind.SpaceGainOrRestriction,
        group = StrategicIdeaGroup.StructuralChange,
        readiness = StrategicIdeaReadiness.Build,
        focusSquares = List("e3", "g4"),
        focusZone = Some("kingside"),
        beneficiaryPieces = List("N"),
        confidence = 0.89
      ),
      StrategyIdeaSignal(
        ideaId = "idea_2",
        ownerSide = "white",
        kind = StrategicIdeaKind.CounterplaySuppression,
        group = StrategicIdeaGroup.InteractionAndTransformation,
        readiness = StrategicIdeaReadiness.Build,
        focusSquares = List("c5"),
        confidence = 0.80
      )
    ),
    longTermFocus = List("Dark-square control"),
    evidence = List("dominant_thesis:The French Chain calls for a knight reroute toward e3."),
    signalDigest = Some(
      NarrativeSignalDigest(
        practicalVerdict = Some("conversion requires precision"),
        structuralCue = Some("French Chain structure with a semi-open center; plan fit playable"),
        dominantIdeaKind = Some(StrategicIdeaKind.SpaceGainOrRestriction),
        dominantIdeaGroup = Some(StrategicIdeaGroup.StructuralChange),
        dominantIdeaReadiness = Some(StrategicIdeaReadiness.Build),
        dominantIdeaFocus = Some("e3, g4"),
        decision = Some("Resolves back-rank pressure before expanding"),
        opponentPlan = Some("Black still wants ...c5 counterplay.")
      )
    )
  )

  private val sampleRouteRefs = List(
    ActiveStrategicRouteRef(
      routeId = "route_1",
      piece = "N",
      route = List("d2", "f1", "e3"),
      purpose = "king defense",
      confidence = 0.78
    )
  )

  private val sampleMoveRefs = List(
    ActiveStrategicMoveRef(
      label = "Engine preference",
      source = "top_engine_move",
      uci = "d2f1",
      san = Some("Nf1")
    )
  )

  private val sampleDossier = ActiveBranchDossier(
    dominantLens = "structure",
    chosenBranchLabel = "French Chain -> kingside clamp",
    engineBranchLabel = Some("engine g4 -> queenside pressure"),
    deferredBranchLabel = Some("deferred g4 -> keeps the kingside initiative"),
    whyChosen = Some("This move starts the knight reroute demanded by the structure."),
    whyDeferred = Some("g4 was deferred because White first resolves the back rank."),
    opponentResource = Some("Black still hopes for ...c5 counterplay."),
    routeCue = Some(
      ActiveBranchRouteCue(
        routeId = "route_1",
        piece = "N",
        route = List("d2", "f1", "e3"),
        purpose = "kingside clamp",
        confidence = 0.78
      )
    ),
    moveCue = Some(
      ActiveBranchMoveCue(
        label = "Engine preference",
        uci = "d2f1",
        san = Some("Nf1"),
        source = "top_engine_move"
      )
    ),
    continuationFocus = Some("White still wants to clamp the kingside dark squares."),
    practicalRisk = Some("If White drifts, the queenside counterplay revives."),
    threadLabel = Some("Whole-Board Play"),
    threadStage = Some("Switch"),
    threadSummary = Some("White fixes one sector before switching pressure across the board."),
    threadOpponentCounterplan = Some("Black still wants ...c5 counterplay.")
  )

  test("buildPrompt renders a coaching brief instead of raw strategy dumps") {
    val prompt = ActiveStrategicPrompt.buildPrompt(
      baseNarrative = "White stabilizes and prepares kingside play.",
      phase = "middlegame",
      momentType = "TensionPeak",
      fen = "r2q1rk1/pp2bppp/2n1pn2/2pp4/3P4/2P1PN2/PPBNBPPP/R2Q1RK1 w - - 0 11",
      concepts = List("space", "initiative"),
      strategyPack = Some(samplePack),
      dossier = Some(sampleDossier),
      routeRefs = sampleRouteRefs,
      moveRefs = sampleMoveRefs
    )

    assert(prompt.contains("## MOMENT CONTEXT"))
    assert(prompt.contains("## COACHING BRIEF"))
    assert(prompt.contains("## OPENING LENS"))
    assert(prompt.contains("Preferred opening lens"))
    assert(prompt.contains("Primary idea"))
    assert(prompt.contains("Why now"))
    assert(prompt.contains("Opponent reply to watch"))
    assert(prompt.contains("Execution hint"))
    assert(prompt.contains("Long-term objective"))
    assert(prompt.contains("Key trigger or failure mode"))
    assert(prompt.contains("space gain or restriction around e3, g4"))
    assert(prompt.contains("knight toward e3"))
    assert(prompt.contains("work toward making g4 available"))
    assert(!prompt.contains("d2-f1-e3"))
    assert(prompt.contains("the game is pivoting toward a new sector or target"))
    assert(!prompt.contains("White stabilizes and prepares kingside play."))
    assert(!prompt.contains("## ACTIVE DOSSIER"))
    assert(!prompt.contains("## CAMPAIGN THREAD"))
    assert(!prompt.contains("## STRATEGY PACK"))
    assert(!prompt.contains("## REFERENCE CATALOG"))
    assert(!prompt.contains("route_1"))
    assert(!prompt.contains("Engine preference"))
    assert(!prompt.contains("d2f1"))
    assert(!prompt.contains("thread stage: Switch"))
    assert(!prompt.contains("Signal Digest"))
    assert(prompt.contains("avoid bare imperative leads"))
    assert(prompt.contains("dominant idea as the thesis"))
  }

  test("buildRepairPrompt keeps coaching brief and player-facing repair instructions") {
    val prompt = ActiveStrategicPrompt.buildRepairPrompt(
      baseNarrative = "Black must neutralize pressure on e6.",
      rejectedNote = "Play better somehow.",
      failureReasons = List("forward_plan_missing", "strategy_coverage_low"),
      phase = "middlegame",
      momentType = "SustainedPressure",
      fen = "r1bq1rk1/pp3ppp/2n1pn2/2pp4/3P4/2P1PN2/PP1NBPPP/R1BQ1RK1 b - - 0 10",
      concepts = List("space"),
      strategyPack = Some(samplePack),
      dossier = Some(sampleDossier),
      routeRefs = sampleRouteRefs,
      moveRefs = sampleMoveRefs
    )

    assert(prompt.contains("## PRIOR NOTE TO AVOID PARAPHRASING"))
    assert(prompt.contains("## REJECTED NOTE"))
    assert(prompt.contains("## REPAIR REASONS"))
    assert(prompt.contains("forward_plan_missing"))
    assert(prompt.contains("strategy_coverage_low"))
    assert(prompt.contains("## COACHING BRIEF"))
    assert(prompt.contains("## OPENING LENS"))
    assert(prompt.contains("Preferred opening lens"))
    assert(prompt.contains("Do not mirror its wording or sentence structure"))
    assert(prompt.contains("dominant idea as the thesis"))
    assert(prompt.contains("mention at most one of the execution hint or the long-term objective"))
    assert(!prompt.contains("route_1"))
    assert(!prompt.contains("Engine preference"))
    assert(!prompt.contains("d2f1"))
    assert(!prompt.contains("d2-f1-e3"))
    assert(!prompt.contains("## ACTIVE DOSSIER"))
  }

  test("buildPrompt omits empty optional context sections") {
    val prompt = ActiveStrategicPrompt.buildPrompt(
      baseNarrative = "White improves coordination.",
      phase = "middlegame",
      momentType = "Strategic Moment",
      fen = "",
      concepts = Nil,
      strategyPack = None,
      dossier = None,
      routeRefs = Nil,
      moveRefs = Nil
    )

    assert(prompt.contains("## MOMENT CONTEXT"))
    assert(prompt.contains("Phase: middlegame"))
    assert(prompt.contains("Moment Type: Strategic Moment"))
    assert(!prompt.contains("FEN:"))
    assert(!prompt.contains("Concepts:"))
    assert(!prompt.contains("## COACHING BRIEF"))
    assert(!prompt.contains("## OPENING LENS"))
  }

  test("buildPrompt surfaces tactical-first guidance when immediate material gain exists") {
    val prompt = ActiveStrategicPrompt.buildPrompt(
      baseNarrative = "White keeps the bind together.",
      phase = "middlegame",
      momentType = "Strategic Moment",
      fen = tacticalFen,
      concepts = List("space", "bind"),
      strategyPack = Some(samplePack),
      dossier = Some(sampleDossier),
      routeRefs = sampleRouteRefs,
      moveRefs =
        List(
          ActiveStrategicMoveRef(
            label = "Engine preference",
            source = "top_engine_move",
            uci = "d4d6",
            san = Some("Qxd6"),
            fenAfter = Some(tacticalFenAfter)
          )
        )
    )

    assert(prompt.contains("Preferred opening lens: tactical-first"))
    assert(prompt.contains("Qxd6 immediately wins a pawn."))
    assert(prompt.contains("surface any immediate tactical or material gain in the first sentence"))
    assert(prompt.contains("already occupied friendly square"))
  }
