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
    longTermFocus = List("dominant thesis: The French Chain calls for a knight reroute toward e3.", "Dark-square control"),
    evidence = List("dominant_thesis:The French Chain calls for a knight reroute toward e3.", "top_multipv"),
    signalDigest = Some(
      NarrativeSignalDigest(
        opening = Some("Ruy Lopez"),
        practicalVerdict = Some("conversion requires precision"),
        structureProfile = Some("French Chain"),
        alignmentBand = Some("Playable"),
        deploymentPiece = Some("N"),
        deploymentRoute = List("d2", "f1", "e3"),
        deploymentPurpose = Some("kingside clamp"),
        deploymentContribution = Some("This move starts that route immediately."),
        deploymentConfidence = Some(0.81),
        decision = Some("Resolves back-rank pressure before expanding"),
        decisionComparison = Some(
          DecisionComparisonDigest(
            chosenMove = Some("Nf1"),
            engineBestMove = Some("g4"),
            cpLossVsChosen = Some(34),
            deferredMove = Some("g4"),
            deferredReason = Some("it keeps the kingside initiative without conceding the center"),
            evidence = Some("The engine line begins g4 ...Nh5 h4.")
          )
        ),
        preservedSignals = List("opening", "practical", "decision")
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
    evidenceCue = Some("The engine line begins g4 ...Nh5 h4."),
    continuationFocus = Some("White still wants to clamp the kingside dark squares."),
    practicalRisk = Some("If White drifts, the queenside counterplay revives."),
    comparisonGapCp = Some(34)
  )

  test("buildPrompt includes strategy pack plans routes and focus") {
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

    assert(prompt.contains("Kingside expansion"))
    assert(prompt.contains("d2-f1-e3"))
    assert(prompt.contains("Dark-square control"))
    assert(prompt.contains("Signal Digest"))
    assert(prompt.contains("Ruy Lopez"))
    assert(prompt.contains("conversion requires precision"))
    assert(prompt.contains("dominant thesis"))
    assert(prompt.contains("structure deployment: N d2-f1-e3"))
    assert(prompt.contains("deployment purpose: kingside clamp"))
    assert(prompt.contains("chosen move: Nf1"))
    assert(prompt.contains("engine best: g4"))
    assert(prompt.contains("cp loss vs chosen: 34cp"))
    assert(prompt.contains("deferred move: g4"))
    assert(prompt.contains("deferred reason: it keeps the kingside initiative without conceding the center"))
    assert(prompt.contains("deferred evidence: The engine line begins g4 ...Nh5 h4."))
    assert(prompt.contains("## ACTIVE DOSSIER"))
    assert(prompt.contains("French Chain -> kingside clamp"))
    assert(prompt.contains("route cue: route_1 Nd2-f1-e3"))
    assert(prompt.contains("move cue: Engine preference d2f1"))
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
      dossier = Some(sampleDossier),
      routeRefs = sampleRouteRefs,
      moveRefs = sampleMoveRefs
    )

    assert(prompt.contains("Play better somehow."))
    assert(prompt.contains("active_note_sentence_count"))
    assert(prompt.contains("strategy_coverage_low"))
    assert(prompt.contains("## ACTIVE DOSSIER"))
    assert(prompt.contains("deferred g4 -> keeps the kingside initiative"))
    assert(prompt.contains("Signal Digest"))
    assert(prompt.contains("route_1"))
    assert(prompt.contains("Engine preference"))
    assert(prompt.contains("Repair output must cite exact routeId and/or move label"))
  }
