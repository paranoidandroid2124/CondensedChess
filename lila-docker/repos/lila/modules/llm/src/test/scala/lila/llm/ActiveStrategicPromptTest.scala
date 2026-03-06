package lila.llm

import munit.FunSuite

class ActiveStrategicPromptTest extends FunSuite:

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
    longTermFocus = List("Dark-square control"),
    evidence = List("top_multipv")
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

  test("buildPrompt includes strategy pack plans routes and focus") {
    val prompt = ActiveStrategicPrompt.buildPrompt(
      baseNarrative = "White stabilizes and prepares kingside play.",
      phase = "middlegame",
      momentType = "TensionPeak",
      fen = "r2q1rk1/pp2bppp/2n1pn2/2pp4/3P4/2P1PN2/PPBNBPPP/R2Q1RK1 w - - 0 11",
      concepts = List("space", "initiative"),
      strategyPack = Some(samplePack),
      routeRefs = sampleRouteRefs,
      moveRefs = sampleMoveRefs
    )

    assert(prompt.contains("Kingside expansion"))
    assert(prompt.contains("d2-f1-e3"))
    assert(prompt.contains("Dark-square control"))
    assert(prompt.contains("route_1"))
    assert(prompt.contains("Engine preference"))
    assert(prompt.contains("cite exact routeId and/or move label"))
  }

  test("buildRepairPrompt carries rejected note and repair reasons") {
    val prompt = ActiveStrategicPrompt.buildRepairPrompt(
      baseNarrative = "Black must neutralize pressure on e6.",
      rejectedNote = "Play better somehow.",
      failureReasons = List("active_note_sentence_count", "strategy_coverage_low"),
      phase = "middlegame",
      momentType = "SustainedPressure",
      fen = "r1bq1rk1/pp3ppp/2n1pn2/2pp4/3P4/2P1PN2/PP1NBPPP/R1BQ1RK1 b - - 0 10",
      concepts = List("space"),
      strategyPack = Some(samplePack),
      routeRefs = sampleRouteRefs,
      moveRefs = sampleMoveRefs
    )

    assert(prompt.contains("Play better somehow."))
    assert(prompt.contains("active_note_sentence_count"))
    assert(prompt.contains("strategy_coverage_low"))
    assert(prompt.contains("route_1"))
    assert(prompt.contains("Engine preference"))
    assert(prompt.contains("Repair output must cite exact routeId and/or move label"))
  }
