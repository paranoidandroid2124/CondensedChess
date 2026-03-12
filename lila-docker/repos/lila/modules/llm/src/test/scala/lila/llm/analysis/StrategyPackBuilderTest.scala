package lila.llm.analysis

import chess.{ Bishop, Color, Knight, Queen, Square }
import lila.llm.*
import lila.llm.model.*
import lila.llm.model.authoring.{ AuthorQuestion, AuthorQuestionKind, EvidenceBranch, PlanHypothesis, PlanViability, QuestionEvidence }
import lila.llm.model.strategic.{ CounterfactualMatch, EngineEvidence, PieceActivity, PlanContinuity, PlanLifecyclePhase, PositionalTag, PvMove, VariationLine }
import munit.FunSuite

class StrategyPackBuilderTest extends FunSuite:

  private val testFen = "r1bqkbnr/pppp1ppp/2n5/4p3/8/5N2/PPPPPPPP/RNBQKB1R w KQkq - 2 3"

  def hypothesis(name: String, score: Double, rank: Int): PlanHypothesis =
    PlanHypothesis(
      planId = name.toLowerCase.replace(' ', '_'),
      planName = name,
      rank = rank,
      score = score,
      preconditions = List("stable center"),
      executionSteps = List(s"execute $name"),
      failureModes = List(s"$name breaks if king is exposed"),
      viability = PlanViability(score = score, label = "high", risk = "counterplay")
    )

  def ctx(
      mainPlans: List[PlanHypothesis] = Nil,
      planRows: List[PlanRow] = Nil,
      opponent: Option[PlanRow] = None,
      continuity: Option[PlanContinuity] = None,
      conceptSummary: List[String] = Nil,
      whyAbsent: List[String] = Nil,
      semantic: Option[SemanticSection] = None,
      probeRequests: List[ProbeRequest] = Nil,
      authorQuestions: List[AuthorQuestion] = Nil,
      authorEvidence: List[QuestionEvidence] = Nil
  ): NarrativeContext =
    NarrativeContext(
      fen = testFen,
      header = ContextHeader("Middlegame", "Normal", "StyleChoice", "Medium", "ExplainPlan"),
      ply = 21,
      summary = NarrativeSummary("Test plan", None, "StyleChoice", "Maintain", "+0.3"),
      threats = ThreatTable(Nil, Nil),
      pawnPlay = PawnPlayTable(false, None, "Low", "Maintain", "none", "Background", None, false, "quiet"),
      plans = PlanTable(top5 = planRows, suppressed = Nil),
      planContinuity = continuity,
      delta = None,
      phase = PhaseContext("Middlegame", "test"),
      candidates = Nil,
      mainStrategicPlans = mainPlans,
      whyAbsentFromTopMultiPV = whyAbsent,
      probeRequests = probeRequests,
      semantic = semantic.orElse(
        Option.when(conceptSummary.nonEmpty)(
          SemanticSection(
            structuralWeaknesses = Nil,
            pieceActivity = Nil,
            positionalFeatures = Nil,
            compensation = None,
            endgameFeatures = None,
            practicalAssessment = None,
            preventedPlans = Nil,
            conceptSummary = conceptSummary
          )
        )
      ),
      opponentPlan = opponent,
      authorQuestions = authorQuestions,
      authorEvidence = authorEvidence
    )

  def data(
      fen: String = testFen,
      pieceActivity: List[PieceActivity] = Nil,
      positionalFeatures: List[PositionalTag] = Nil,
      isWhiteToMove: Boolean = true
  ): ExtendedAnalysisData =
    ExtendedAnalysisData(
      fen = fen,
      nature = PositionNature(NatureType.Dynamic, 0.6, 0.4, "dynamic test"),
      motifs = Nil,
      plans = Nil,
      preventedPlans = Nil,
      pieceActivity = pieceActivity,
      structuralWeaknesses = Nil,
      positionalFeatures = positionalFeatures,
      compensation = None,
      endgameFeatures = None,
      practicalAssessment = None,
      alternatives = Nil,
      candidates = Nil,
      counterfactual = None,
      conceptSummary = Nil,
      prevMove = None,
      ply = 21,
      evalCp = 30,
      isWhiteToMove = isWhiteToMove
    )

  test("build expands mover plans beyond single-plan cap while keeping opponent plan") {
    val mainPlans = List(
      hypothesis("Kingside Expansion", 0.86, 1),
      hypothesis("Central Restriction", 0.79, 2)
    )
    val planRows = List(
      PlanRow(1, "Kingside Expansion", 0.86, Nil),
      PlanRow(2, "Central Restriction", 0.79, Nil),
      PlanRow(3, "File Control", 0.64, Nil)
    )
    val opponent = Some(PlanRow(1, "Queenside Counterplay", 0.61, Nil))

    val pack = StrategyPackBuilder.build(data(), ctx(mainPlans, planRows, opponent)).getOrElse(fail("pack missing"))
    val planNames = pack.plans.map(p => s"${p.side}:${p.planName}").toSet

    assertEquals(pack.plans.size, 4)
    assert(planNames.contains("white:Kingside Expansion"))
    assert(planNames.contains("white:Central Restriction"))
    assert(planNames.contains("white:File Control"))
    assert(planNames.contains("black:Queenside Counterplay"))
  }

  test("build route purpose uses outpost signal and emits coordination evidence") {
    val pa = PieceActivity(
      piece = Knight,
      square = Square.F3,
      mobilityScore = 0.42,
      isTrapped = false,
      isBadBishop = false,
      keyRoutes = List(Square.G5, Square.E4),
      coordinationLinks = List(Square.E4, Square.G5)
    )
    val pack = StrategyPackBuilder
      .build(
        data(
          pieceActivity = List(pa),
          positionalFeatures = List(PositionalTag.Outpost(Square.E4, Color.White))
        ),
        ctx(mainPlans = List(hypothesis("Kingside Expansion", 0.8, 1)))
      )
      .getOrElse(fail("pack missing"))

    val route = pack.pieceRoutes.headOption.getOrElse(fail("route missing"))
    assertEquals(route.purpose, "outpost reinforcement")
    assert(route.evidence.exists(_.startsWith("coordination_links_")), clue(route.evidence))
  }

  test("build emits piece routes for both mover and opponent sides") {
    val whiteRoute = PieceActivity(
      piece = Knight,
      square = Square.F3,
      mobilityScore = 0.42,
      isTrapped = false,
      isBadBishop = false,
      keyRoutes = List(Square.G5, Square.E4),
      coordinationLinks = List(Square.E4)
    )
    val blackRoute = PieceActivity(
      piece = Knight,
      square = Square.C6,
      mobilityScore = 0.40,
      isTrapped = false,
      isBadBishop = false,
      keyRoutes = List(Square.B4, Square.D4),
      coordinationLinks = List(Square.D4)
    )

    val pack =
      StrategyPackBuilder
        .build(
          data(pieceActivity = List(whiteRoute, blackRoute)),
          ctx(mainPlans = List(hypothesis("Kingside Expansion", 0.8, 1)))
        )
        .getOrElse(fail("pack missing"))

    val sides = pack.pieceRoutes.map(_.side).toSet
    assert(sides.contains("white"), clue(pack.pieceRoutes))
    assert(sides.contains("black"), clue(pack.pieceRoutes))
  }

  test("build longTermFocus prioritizes continuity and route focus over raw concat") {
    val pa = PieceActivity(
      piece = Knight,
      square = Square.F3,
      mobilityScore = 0.35,
      isTrapped = false,
      isBadBishop = false,
      keyRoutes = List(Square.G5, Square.E4),
      coordinationLinks = List(Square.E4)
    )
    val continuity = PlanContinuity(
      planName = "Kingside Expansion",
      planId = Some("kingside_expansion"),
      consecutivePlies = 3,
      startingPly = 17,
      phase = PlanLifecyclePhase.Execution,
      commitmentScore = 0.82
    )

    val pack = StrategyPackBuilder
      .build(
        data(pieceActivity = List(pa)),
        ctx(
          mainPlans = List(hypothesis("Kingside Expansion", 0.84, 1)),
          continuity = Some(continuity),
          conceptSummary = List("space advantage", "dark-square pressure"),
          whyAbsent = List("line rejected because center breaks too early")
        )
      )
      .getOrElse(fail("pack missing"))

    assert(pack.longTermFocus.exists(_.startsWith("continuity:")), clue(pack.longTermFocus))
    assert(pack.longTermFocus.exists(_.contains("route")), clue(pack.longTermFocus))
    assert(pack.evidence.exists(_.startsWith("route:")), clue(pack.evidence))
    assert(pack.evidence.exists(_.contains("line rejected")), clue(pack.evidence))
  }

  test("build reclassifies enemy-occupied bishop target into move-ref instead of route") {
    val fen = "4k3/8/8/8/8/4p3/8/2B1K3 w - - 0 1"
    val pa = PieceActivity(
      piece = Bishop,
      square = Square.C1,
      mobilityScore = 0.18,
      isTrapped = false,
      isBadBishop = true,
      keyRoutes = Nil,
      coordinationLinks = Nil,
      concreteTargets = List(Square.E3)
    )

    val pack = StrategyPackBuilder
      .build(
        data(fen = fen, pieceActivity = List(pa)),
        ctx(mainPlans = List(hypothesis("Bishop Relief", 0.76, 1)))
      )
      .getOrElse(fail("pack missing"))

    assertEquals(pack.pieceRoutes, Nil)
    assertEquals(pack.pieceMoveRefs.map(_.target), List("e3"))
  }

  test("unsafe knight reroute degrades to toward-only surface") {
    val fen = "4k3/8/8/8/3p1p2/8/3N4/4K3 w - - 0 1"
    val pa = PieceActivity(
      piece = Knight,
      square = Square.D2,
      mobilityScore = 0.12,
      isTrapped = false,
      isBadBishop = false,
      keyRoutes = List(Square.F1, Square.E3),
      coordinationLinks = List(Square.E3)
    )

    val pack = StrategyPackBuilder
      .build(
        data(fen = fen, pieceActivity = List(pa)),
        ctx(mainPlans = List(hypothesis("Kingside Expansion", 0.84, 1)))
      )
      .getOrElse(fail("pack missing"))

    val route = pack.pieceRoutes.headOption.getOrElse(fail("route missing"))
    assertEquals(route.surfaceMode, lila.llm.RouteSurfaceMode.Toward)
    assert(route.tacticalSafety < 0.82, clue(route))
  }

  test("build emits directional target for empty but not-ready strategic square") {
    val fen = "4k3/8/8/8/8/8/2N5/4K3 w - - 0 1"
    val pa = PieceActivity(
      piece = Knight,
      square = Square.C2,
      mobilityScore = 0.22,
      isTrapped = false,
      isBadBishop = false,
      keyRoutes = Nil,
      coordinationLinks = List(Square.E3),
      directionalTargets = List(Square.E4)
    )

    val pack = StrategyPackBuilder
      .build(
        data(fen = fen, pieceActivity = List(pa)),
        ctx(mainPlans = List(hypothesis("Clamp the center", 0.80, 1)))
      )
      .getOrElse(fail("pack missing"))

    val target = pack.directionalTargets.headOption.getOrElse(fail("directional target missing"))
    assertEquals(target.targetSquare, "e4")
    assertEquals(target.readiness, DirectionalTargetReadiness.Premature)
    assert(target.strategicReasons.nonEmpty, clue(target))
  }

  test("build never emits directional target for enemy-occupied square") {
    val fen = "4k3/8/8/8/8/4p3/2N5/4K3 w - - 0 1"
    val pa = PieceActivity(
      piece = Knight,
      square = Square.C2,
      mobilityScore = 0.22,
      isTrapped = false,
      isBadBishop = false,
      keyRoutes = Nil,
      coordinationLinks = List(Square.E3),
      directionalTargets = List(Square.E3)
    )

    val pack = StrategyPackBuilder
      .build(
        data(fen = fen, pieceActivity = List(pa)),
        ctx(mainPlans = List(hypothesis("Clamp the center", 0.80, 1)))
      )
      .getOrElse(fail("pack missing"))

    assertEquals(pack.directionalTargets, Nil)
  }

  test("queen multi-hop redeployment stays toward-only even when fit is high") {
    val fen = "4k3/8/8/8/8/8/8/3QK3 w - - 0 1"
    val pa = PieceActivity(
      piece = Queen,
      square = Square.D1,
      mobilityScore = 0.10,
      isTrapped = false,
      isBadBishop = false,
      keyRoutes = List(Square.D3, Square.C4, Square.C5),
      coordinationLinks = List(Square.C5)
    )

    val pack = StrategyPackBuilder
      .build(
        data(fen = fen, pieceActivity = List(pa)),
        ctx(mainPlans = List(hypothesis("Centralization", 0.88, 1)))
      )
      .getOrElse(fail("pack missing"))

    val route = pack.pieceRoutes.headOption.getOrElse(fail("route missing"))
    assertEquals(route.surfaceMode, lila.llm.RouteSurfaceMode.Toward)
  }

  test("build emits rich structure practical and prophylaxis digest details") {
    val semantic = SemanticSection(
      structuralWeaknesses = Nil,
      pieceActivity = List(
        PieceActivityInfo(
          piece = "Rook",
          square = "a1",
          mobilityScore = 0.40,
          isTrapped = false,
          isBadBishop = false,
          keyRoutes = List("b1", "b3"),
          coordinationLinks = List("b4")
        )
      ),
      positionalFeatures = Nil,
      compensation = Some(
        CompensationInfo(
          investedMaterial = 100,
          returnVector = Map("Attack on King" -> 1.1, "Space Advantage" -> 0.7),
          expiryPly = None,
          conversionPlan = "Mating Attack"
        )
      ),
      endgameFeatures = None,
      practicalAssessment = Some(
        PracticalInfo(
          engineScore = 45,
          practicalScore = 88.0,
          verdict = "Comfortable",
          biasFactors = List(
            PracticalBiasInfo("Mobility", "Diff: 1.8", 36.0),
            PracticalBiasInfo("Forgiveness", "2 safe moves", -18.0)
          )
        )
      ),
      preventedPlans = List(
        PreventedPlanInfo(
          planId = "Queenside Counterplay",
          deniedSquares = Nil,
          breakNeutralized = Some("c5"),
          mobilityDelta = 0,
          counterplayScoreDrop = 140,
          preventedThreatType = Some("counterplay")
        )
      ),
      conceptSummary = Nil,
      structureProfile = Some(
        StructureProfileInfo(
          primary = "Carlsbad",
          confidence = 0.84,
          alternatives = Nil,
          centerState = "Locked",
          evidenceCodes = List("MAJORITY")
        )
      ),
      planAlignment = Some(
        PlanAlignmentInfo(
          score = 61,
          band = "Playable",
          matchedPlanIds = List("minority_attack"),
          missingPlanIds = List("central_break"),
          reasonCodes = List("PRECOND_MISS"),
          narrativeIntent = Some("play around queenside pressure"),
          narrativeRisk = Some("counterplay if move order slips")
        )
      )
    )

    val pack =
      StrategyPackBuilder
        .build(
          data(),
          ctx(
            mainPlans = List(hypothesis("Minority Attack", 0.81, 1)),
            semantic = Some(semantic)
          )
        )
        .getOrElse(fail("pack missing"))

    val digest = pack.signalDigest.getOrElse(fail("missing digest"))
    assertEquals(digest.structureProfile, Some("Carlsbad"))
    assertEquals(digest.centerState, Some("Locked"))
    assertEquals(digest.alignmentBand, Some("Playable"))
    assert(digest.alignmentReasons.exists(_.contains("preconditions")), clue(digest.alignmentReasons))
    assertEquals(digest.deploymentPiece, Some("R"))
    assert(digest.deploymentRoute.nonEmpty, clue(digest))
    assertEquals(digest.deploymentPurpose, Some("queenside pressure"))
    assert(digest.deploymentContribution.exists(_.contains("This move")), clue(digest))
    assertEquals(digest.prophylaxisPlan, Some("Queenside Counterplay"))
    assertEquals(digest.prophylaxisThreat, Some("counterplay"))
    assertEquals(digest.counterplayScoreDrop, Some(140))
    assertEquals(digest.practicalVerdict, Some("Comfortable"))
    assert(digest.practicalFactors.exists(_.contains("Mobility")), clue(digest.practicalFactors))
    assertEquals(digest.compensation, Some("Mating Attack"))
    assertEquals(digest.investedMaterial, Some(100))
    assert(digest.compensationVectors.exists(_.contains("Attack on King")), clue(digest.compensationVectors))
  }

  test("build carries authoring evidence into digest prompt hints and pack evidence") {
    val question = AuthorQuestion(
      id = "latent_1",
      kind = AuthorQuestionKind.LatentPlan,
      priority = 1,
      question = "Can the kingside expansion survive ...c5?",
      why = Some("Need a concrete refutation line."),
      confidence = ConfidenceLevel.Probe
    )
    val request = ProbeRequest(
      id = "probe_latent_1",
      fen = testFen,
      moves = List("g2g4"),
      depth = 18,
      purpose = Some("latent_plan_refutation"),
      questionId = Some("latent_1"),
      questionKind = Some("LatentPlan"),
      objective = Some("validate_latent_plan"),
      planName = Some("Kingside Expansion"),
      seedId = Some("kingside_expansion")
    )
    val evidence = QuestionEvidence(
      questionId = "latent_1",
      purpose = "latent_plan_refutation",
      branches = List(
        EvidenceBranch(
          keyMove = "...c5",
          line = "...c5 dxc5",
          evalCp = Some(64),
          depth = Some(20),
          sourceId = Some("probe_latent_1")
        )
      )
    )

    val pack = StrategyPackBuilder
      .build(
        data(),
        ctx(
          mainPlans = List(hypothesis("Kingside Expansion", 0.84, 1)),
          probeRequests = List(request),
          authorQuestions = List(question),
          authorEvidence = List(evidence)
        )
      )
      .getOrElse(fail("pack missing"))

    val digest = pack.signalDigest.getOrElse(fail("missing digest"))
    assertEquals(
      digest.authoringEvidence,
      Some("latent plan evidence is resolved via 1 branch"),
      clue(digest)
    )
    assert(
      pack.evidence.exists(ev =>
        ev.startsWith("authoring:LatentPlan:resolved:Can the kingside expansion survive") &&
          ev.contains("[branches=1 pending_probes=1 plan=Kingside Expansion]")
      ),
      clue(pack.evidence)
    )
    val hints = StrategyPackBuilder.promptHints(pack)
    assert(hints.exists(_.contains("authoring evidence: latent plan evidence is resolved")), clue(hints))
  }

  test("build uses structure arc to enrich long term focus and deployment evidence") {
    val semantic = SemanticSection(
      structuralWeaknesses = Nil,
      pieceActivity = List(
        PieceActivityInfo(
          piece = "Knight",
          square = "d2",
          mobilityScore = 0.28,
          isTrapped = false,
          isBadBishop = false,
          keyRoutes = List("f1", "e3", "g4"),
          coordinationLinks = List("e3", "g4")
        )
      ),
      positionalFeatures = Nil,
      compensation = None,
      endgameFeatures = None,
      practicalAssessment = None,
      preventedPlans = Nil,
      conceptSummary = Nil,
      structureProfile = Some(
        StructureProfileInfo("French Chain", 0.80, Nil, "Closed", List("ENTRENCHED"))
      ),
      planAlignment = Some(
        PlanAlignmentInfo(
          score = 70,
          band = "Playable",
          matchedPlanIds = List("restriction_play"),
          missingPlanIds = Nil,
          reasonCodes = List("PA_MATCH"),
          narrativeIntent = Some("reroute toward e3 and g4"),
          narrativeRisk = Some("Black can free the game with ...c5")
        )
      )
    )

    val pack =
      StrategyPackBuilder
        .build(
          data(),
          ctx(
            mainPlans = List(hypothesis("Restrict the Entrenched Knight", 0.82, 1)),
            semantic = Some(semantic)
          ).copy(playedMove = Some("d2f1"), playedSan = Some("Nf1"))
        )
        .getOrElse(fail("pack missing"))

    assert(pack.longTermFocus.exists(_.toLowerCase.startsWith("structure deployment: french chain asks for")), clue(pack.longTermFocus))
    assert(pack.evidence.exists(_.startsWith("deployment:N:")), clue(pack.evidence))
  }

  test("build carries the dominant thesis into long-term focus and evidence for active parity") {
    val semantic = SemanticSection(
      structuralWeaknesses = Nil,
      pieceActivity = List(
        PieceActivityInfo(
          piece = "Knight",
          square = "d2",
          mobilityScore = 0.30,
          isTrapped = false,
          isBadBishop = false,
          keyRoutes = List("f1", "e3"),
          coordinationLinks = List("e3")
        )
      ),
      positionalFeatures = Nil,
      compensation = None,
      endgameFeatures = None,
      practicalAssessment = None,
      preventedPlans = Nil,
      conceptSummary = Nil,
      structureProfile = Some(StructureProfileInfo("French Chain", 0.81, Nil, "Closed", List("CHAIN"))),
      planAlignment = Some(
        PlanAlignmentInfo(
          score = 71,
          band = "Playable",
          matchedPlanIds = List("restriction_play"),
          missingPlanIds = Nil,
          reasonCodes = List("PA_MATCH"),
          narrativeIntent = Some("reroute toward e3"),
          narrativeRisk = Some("counterplay appears if the center breaks too early")
        )
      )
    )

    val pack =
      StrategyPackBuilder
        .build(
          data(),
          ctx(
            mainPlans = List(hypothesis("Restrict the Entrenched Knight", 0.83, 1)),
            semantic = Some(semantic)
          ).copy(playedMove = Some("d2f1"), playedSan = Some("Nf1"))
        )
        .getOrElse(fail("pack missing"))

    assert(pack.longTermFocus.exists(_.toLowerCase.startsWith("dominant thesis:")), clue(pack.longTermFocus))
    assert(pack.evidence.exists(_.startsWith("dominant_thesis:")), clue(pack.evidence))
  }

  test("build carries decision comparison into digest focus and evidence") {
    val best =
      VariationLine(
        moves = List("g2g4", "a7a6", "h4h5"),
        scoreCp = 28,
        parsedMoves = List(
          PvMove("g2g4", "g4", "g2", "g4", "P", false, None, false),
          PvMove("a7a6", "...a6", "a7", "a6", "p", false, None, false),
          PvMove("h4h5", "h5", "h4", "h5", "P", false, None, false)
        )
      )
    val userLine =
      VariationLine(
        moves = List("h2h4", "a7a6"),
        scoreCp = 0,
        parsedMoves = List(
          PvMove("h2h4", "h4", "h2", "h4", "P", false, None, false),
          PvMove("a7a6", "...a6", "a7", "a6", "p", false, None, false)
        )
      )

    val pack = StrategyPackBuilder
      .build(
        data(),
        ctx(
          mainPlans = List(hypothesis("Kingside Expansion", 0.84, 1)),
          whyAbsent = List("""the immediate "g4" push loses 220 cp""")
        ).copy(
          playedMove = Some("h2h4"),
          playedSan = Some("h4"),
          engineEvidence = Some(EngineEvidence(depth = 20, variations = List(best))),
          counterfactual = Some(
            CounterfactualMatch(
              userMove = "h4",
              bestMove = "g4",
              cpLoss = 220,
              missedMotifs = Nil,
              userMoveMotifs = Nil,
              severity = "Mistake",
              userLine = userLine
            )
          )
        )
      )
      .getOrElse(fail("pack missing"))

    val digest = pack.signalDigest.getOrElse(fail("missing digest"))
    val comparison = digest.decisionComparison.getOrElse(fail("missing comparison digest"))
    assertEquals(comparison.chosenMove, Some("h4"))
    assertEquals(comparison.engineBestMove, Some("g4"))
    assertEquals(comparison.deferredMove, Some("g4"))
    assert(pack.longTermFocus.exists(_.contains("engine best: g4")), clue(pack.longTermFocus))
    assert(pack.longTermFocus.exists(_.contains("deferred branch: g4")), clue(pack.longTermFocus))
    assert(pack.evidence.exists(_.startsWith("decision_compare:engine_best:g4")), clue(pack.evidence))
    assert(pack.evidence.exists(_.startsWith("decision_compare:deferred:g4")), clue(pack.evidence))
  }
