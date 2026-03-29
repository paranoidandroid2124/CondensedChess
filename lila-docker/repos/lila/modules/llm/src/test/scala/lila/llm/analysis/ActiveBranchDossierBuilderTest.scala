package lila.llm.analysis

import munit.FunSuite

import lila.llm.*
import lila.llm.model.strategic.VariationLine

class ActiveBranchDossierBuilderTest extends FunSuite:

  private val routeRefs = List(
    ActiveStrategicRouteRef(
      routeId = "route_1",
      ownerSide = "white",
      piece = "R",
      route = List("a1", "b1", "b3"),
      purpose = "queenside pressure",
      strategicFit = 0.84,
      tacticalSafety = 0.77,
      surfaceConfidence = 0.82,
      surfaceMode = RouteSurfaceMode.Exact
    )
  )

  private val moveRefs = List(
    ActiveStrategicMoveRef(
      label = "Engine preference",
      source = "top_engine_move",
      uci = "a1b1",
      san = Some("Rb1")
    )
  )

  private def strategicPack =
    Some(
      StrategyPack(
        sideToMove = "white",
        pieceRoutes = List(
          StrategyPieceRoute(
            ownerSide = "white",
            piece = "R",
            from = "a1",
            route = List("a1", "b1", "b3"),
            purpose = "queenside pressure",
            strategicFit = 0.84,
            tacticalSafety = 0.77,
            surfaceConfidence = 0.82,
            surfaceMode = RouteSurfaceMode.Exact,
            evidence = List("probe")
          )
        ),
        directionalTargets = List(
          StrategyDirectionalTarget(
            targetId = "target_b3",
            ownerSide = "white",
            piece = "R",
            from = "a1",
            targetSquare = "b3",
            readiness = DirectionalTargetReadiness.Build,
            strategicReasons = List("queenside pressure"),
            evidence = List("probe")
          )
        )
      )
    )

  private def moment(
      signalDigestOpt: Option[NarrativeSignalDigest],
      strategyPack: Option[StrategyPack] = strategicPack,
      momentType: String = "SustainedPressure",
      moveClassification: Option[String] = None,
      topEngineMove: Option[EngineAlternative] = None,
      narrative: String = "White improves the queenside setup."
  ): GameChronicleMoment =
    GameChronicleMoment(
      momentId = "ply_24_test",
      ply = 24,
      moveNumber = 12,
      side = "white",
      moveClassification = moveClassification,
      momentType = momentType,
      fen = "r2q1rk1/pp2bppp/2n1pn2/2pp4/3P4/2P1PN2/PPBNBPPP/R2Q1RK1 w - - 0 11",
      narrative = narrative,
      concepts = List("queenside pressure"),
      variations = List(VariationLine(moves = List("Rb1", "...a6"), scoreCp = 26)),
      cpBefore = 14,
      cpAfter = 26,
      mateBefore = None,
      mateAfter = None,
      wpaSwing = Some(0.04),
      strategicSalience = Some("High"),
      transitionType = None,
      transitionConfidence = None,
      activePlan = None,
      topEngineMove = topEngineMove,
      collapse = None,
      strategyPack = strategyPack,
      signalDigest = signalDigestOpt
    )

  test("build keeps the active dossier delta-backed and ignores raw structure summaries") {
    val digest = NarrativeSignalDigest(
      structureProfile = Some("Carlsbad"),
      deploymentPurpose = Some("queenside pressure"),
      deploymentContribution = Some("This move connects the rook to b3 immediately."),
      decisionComparison = Some(
        DecisionComparisonDigest(
          chosenMove = Some("Rb1"),
          engineBestMove = Some("Rb1"),
          deferredMove = Some("a4"),
          deferredReason = Some("it fixes the queenside before Black is fully ready"),
          evidence = Some("The engine line begins Rb1 ...a6 Rb3."),
          chosenMatchesBest = true
        )
      )
    )

    val dossier =
      ActiveBranchDossierBuilder
        .build(moment(signalDigestOpt = Some(digest)), routeRefs, moveRefs)
        .getOrElse(fail("missing dossier"))

    assert(clue(dossier.chosenBranchLabel).contains("plan advance"))
    assert(!clue(dossier.chosenBranchLabel).contains("Carlsbad"))
    assertEquals(dossier.routeCue.map(_.routeId), Some("route_1"))
    assertEquals(dossier.moveCue.map(_.label), Some("Engine preference"))
    assertEquals(dossier.evidenceCue, Some("The engine line begins Rb1 ...a6 Rb3."))
    assertEquals(dossier.opponentResource, None)
    assertNotEquals(dossier.dominantLens, "structure")
  }

  test("build preserves tactical truth as the dominant active lens") {
    val digest = NarrativeSignalDigest(
      decisionComparison = Some(
        DecisionComparisonDigest(
          chosenMove = Some("Qe2"),
          engineBestMove = Some("Rc8"),
          deferredMove = Some("Rc8"),
          deferredReason = Some("it kept the c-file pressure alive"),
          evidence = Some("A concrete line is Rc8 ...Qb6."),
          cpLossVsChosen = Some(145)
        )
      )
    )

    val dossier =
      ActiveBranchDossierBuilder
        .build(
          moment(
            signalDigestOpt = Some(digest),
            strategyPack = None,
            momentType = "Blunder",
            moveClassification = Some("Blunder"),
            narrative = "Qe2 is the mistake. It misses the c-file pressure."
          ),
          routeRefs,
          moveRefs
        )
        .getOrElse(fail("missing dossier"))

    assertEquals(dossier.dominantLens, "tactical")
    assert(clue(dossier.chosenBranchLabel).toLowerCase.contains("blunder"))
    assertEquals(dossier.deferredBranchLabel, None)
  }

  test("build omits dossier when only raw prophylaxis or opponent-plan text survives") {
    val digest = NarrativeSignalDigest(
      prophylaxisThreat = Some("...c5 counterplay"),
      opponentPlan = Some("queenside counterplay")
    )

    val dossier =
      ActiveBranchDossierBuilder
        .build(moment(signalDigestOpt = Some(digest), strategyPack = None), routeRefs, moveRefs)
    assertEquals(dossier, None)
  }

  test("build keeps thread badges but does not revive thread summary prose") {
    val digest = NarrativeSignalDigest(
      deploymentContribution = Some("This move clears b3 for the rook."),
      decisionComparison = Some(
        DecisionComparisonDigest(
          chosenMove = Some("Rb1"),
          engineBestMove = Some("Rb1"),
          evidence = Some("The line Rb1 ...a6 Rb3 shows the new file access.")
        )
      )
    )
    val threadRef =
      ActiveStrategicThreadRef(
        threadId = "thread_1",
        themeKey = "whole_board_play",
        themeLabel = "Whole-Board Play",
        stageKey = "switch",
        stageLabel = "Switch"
      )
    val thread =
      ActiveStrategicThread(
        threadId = "thread_1",
        side = "white",
        themeKey = "whole_board_play",
        themeLabel = "Whole-Board Play",
        summary = "White fixes one sector before switching pressure across the board.",
        seedPly = 18,
        lastPly = 30,
        representativePlies = List(18, 24, 30),
        opponentCounterplan = Some("...c5 counterplay"),
        continuityScore = 0.81
      )

    val dossier =
      ActiveBranchDossierBuilder
        .build(
          moment(signalDigestOpt = Some(digest)),
          routeRefs,
          moveRefs,
          threadRef = Some(threadRef),
          thread = Some(thread)
        )
        .getOrElse(fail("missing dossier"))

    assertEquals(dossier.threadLabel, Some("Whole-Board Play"))
    assertEquals(dossier.threadStage, Some("Switch"))
    assertEquals(dossier.threadSummary, None)
    assertEquals(dossier.threadOpponentCounterplan, None)
  }
