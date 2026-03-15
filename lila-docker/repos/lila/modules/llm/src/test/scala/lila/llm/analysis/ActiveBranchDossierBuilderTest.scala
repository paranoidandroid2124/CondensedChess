package lila.llm.analysis

import munit.FunSuite
import lila.llm.*
import lila.llm.model.strategic.VariationLine

class ActiveBranchDossierBuilderTest extends FunSuite:

  private def moment(
      momentType: String = "SustainedPressure",
      moveClassification: Option[String] = None,
      signalDigestOpt: Option[NarrativeSignalDigest],
      strategyPack: Option[StrategyPack] = None,
      authorEvidence: List[AuthorEvidenceSummary] = Nil,
      topEngineMove: Option[EngineAlternative] = None,
      narrative: String = "This structure calls for queenside pressure and a rook lift."
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
      concepts = List("space", "queenside pressure"),
      variations = List(VariationLine(moves = List("a1b1", "a7a6"), scoreCp = 26)),
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
      signalDigest = signalDigestOpt,
      authorEvidence = authorEvidence
    )

  private val routeRefs = List(
    ActiveStrategicRouteRef(
      routeId = "route_1",
      piece = "R",
      route = List("a1", "b1", "b3"),
      purpose = "queenside pressure",
      confidence = 0.84
    )
  )

  private val moveRefs = List(
    ActiveStrategicMoveRef(
      label = "Engine preference",
      source = "top_engine_move",
      uci = "a1b1",
      san = Some("Rb1"),
      fenAfter = Some("r2q1rk1/pp2bppp/2n1pn2/2pp4/3P4/2P1PN2/PPBNBPPP/1R1Q1RK1 b - - 1 11")
    )
  )

  test("build prefers structure deployment cue and comparison data") {
    val digest = NarrativeSignalDigest(
      structureProfile = Some("Carlsbad"),
      deploymentPiece = Some("R"),
      deploymentRoute = List("a1", "b1", "b3"),
      deploymentPurpose = Some("queenside pressure"),
      deploymentContribution = Some("This move connects the b-file immediately."),
      decisionComparison = Some(
        DecisionComparisonDigest(
          chosenMove = Some("Rb1"),
          engineBestMove = Some("Rb1"),
          deferredMove = Some("a4"),
          deferredReason = Some("it fixes the queenside before Black is fully ready"),
          evidence = Some("The engine line begins a4 ...a6 b4."),
          chosenMatchesBest = true
        )
      )
    )

    val dossier = ActiveBranchDossierBuilder.build(moment(signalDigestOpt = Some(digest)), routeRefs, moveRefs)
      .getOrElse(fail("missing dossier"))

    assertEquals(dossier.dominantLens, "structure")
    assert(clue(dossier.chosenBranchLabel).contains("Carlsbad"))
    assertEquals(dossier.deferredBranchLabel, Some("deferred a4 -> it fixes the queenside before Black is fully ready"))
    assertEquals(dossier.routeCue.map(_.routeId), Some("route_1"))
    assertEquals(dossier.routeCue.map(_.ownerSide), Some("white"))
    assertEquals(dossier.moveCue.map(_.label), Some("Engine preference"))
    assertEquals(dossier.evidenceCue, Some("The engine line begins a4 ...a6 b4."))
  }

  test("build uses tactical criticism as dominant lens and keeps comparison subordinate") {
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

    val dossier = ActiveBranchDossierBuilder.build(
      moment(
        momentType = "Blunder",
        moveClassification = Some("Blunder"),
        signalDigestOpt = Some(digest),
        narrative = "Qe2 is the mistake. It misses the c-file pressure."
      ),
      routeRefs,
      moveRefs
    ).getOrElse(fail("missing dossier"))

    assertEquals(dossier.dominantLens, "tactical")
    assert(clue(dossier.chosenBranchLabel).toLowerCase.contains("blunder"))
    assertEquals(dossier.deferredBranchLabel, Some("deferred Rc8 -> it kept the c-file pressure alive"))
  }

  test("build falls back to authoring evidence when comparison evidence is absent") {
    val digest = NarrativeSignalDigest(
      decisionComparison = Some(
        DecisionComparisonDigest(
          chosenMove = Some("Nf3"),
          engineBestMove = Some("Nc3"),
          deferredMove = Some("Nc3"),
          deferredReason = Some("it keeps queenside pressure in reserve")
        )
      )
    )
    val evidence = List(
      AuthorEvidenceSummary(
        questionId = "q1",
        questionKind = "LatentPlan",
        question = "Can White still force queenside pressure?",
        status = "resolved",
        branches = List(
          EvidenceBranchSummary(
            keyMove = "Nc3",
            line = "Nc3 ...a6 0-0",
            evalCp = Some(18),
            depth = Some(18)
          )
        )
      )
    )

    val dossier = ActiveBranchDossierBuilder.build(
      moment(signalDigestOpt = Some(digest), authorEvidence = evidence),
      routeRefs,
      moveRefs
    ).getOrElse(fail("missing dossier"))

    assert(clue(dossier.evidenceCue).exists(_.contains("Nc3")))
    assert(clue(dossier.evidenceCue).exists(_.contains("Nc3 ...a6 0-0")))
  }

  test("build chooses opponent resource from prophylaxis or opponent plan") {
    val digest = NarrativeSignalDigest(
      prophylaxisThreat = Some("...c5 counterplay"),
      opponentPlan = Some("queenside counterplay"),
      decisionComparison = Some(
        DecisionComparisonDigest(
          chosenMove = Some("h3"),
          engineBestMove = Some("a4"),
          deferredMove = Some("a4"),
          deferredReason = Some("it stays inside the queenside pressure branch")
        )
      )
    )

    val dossier = ActiveBranchDossierBuilder.build(moment(signalDigestOpt = Some(digest)), routeRefs, moveRefs)
      .getOrElse(fail("missing dossier"))

    assertEquals(dossier.opponentResource, Some("queenside counterplay"))
  }

  test("build does not fall back to authoring question text for evidence cue") {
    val digest = NarrativeSignalDigest(
      decisionComparison = Some(
        DecisionComparisonDigest(
          chosenMove = Some("Nf3"),
          engineBestMove = Some("Nc3"),
          deferredMove = Some("Nc3")
        )
      )
    )
    val evidence = List(
      AuthorEvidenceSummary(
        questionId = "q1",
        questionKind = "LatentPlan",
        question = "Can White still force queenside pressure?",
        status = "question_only"
      )
    )

    val dossier = ActiveBranchDossierBuilder.build(
      moment(signalDigestOpt = Some(digest), authorEvidence = evidence),
      routeRefs,
      moveRefs
    ).getOrElse(fail("missing dossier"))

    assertEquals(dossier.evidenceCue, None)
  }

  test("build avoids narrative sentence fallback when structured choice is absent") {
    val digest = NarrativeSignalDigest(
      decisionComparison = Some(
        DecisionComparisonDigest(
          engineBestMove = Some("Rb1"),
          deferredMove = Some("a4"),
          deferredReason = Some("it keeps the queenside tension alive")
        )
      )
    )

    val dossier = ActiveBranchDossierBuilder.build(
      moment(
        signalDigestOpt = Some(digest),
        narrative = "This narrative sentence should not become the branch label."
      ),
      routeRefs,
      moveRefs
    ).getOrElse(fail("missing dossier"))

    assert(!clue(dossier.chosenBranchLabel).contains("This narrative sentence should not become the branch label"))
    assert(clue(dossier.chosenBranchLabel).contains("queenside tension"))
  }

  test("build carries campaign thread cues into the dossier") {
    val digest = NarrativeSignalDigest(
      structureProfile = Some("Carlsbad"),
      deploymentPurpose = Some("queenside pressure"),
      decision = Some("White is switching toward the kingside attack")
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

    val dossier = ActiveBranchDossierBuilder.build(
      moment(signalDigestOpt = Some(digest)),
      routeRefs,
      moveRefs,
      threadRef = Some(threadRef),
      thread = Some(thread)
    ).getOrElse(fail("missing dossier"))

    assertEquals(dossier.threadLabel, Some("Whole-Board Play"))
    assertEquals(dossier.threadStage, Some("Switch"))
    assertEquals(dossier.threadSummary, Some("White fixes one sector before switching pressure across the board."))
    assertEquals(dossier.threadOpponentCounterplan, Some("...c5 counterplay"))
    assert(clue(dossier.chosenBranchLabel).contains("Whole-Board Play"))
  }
