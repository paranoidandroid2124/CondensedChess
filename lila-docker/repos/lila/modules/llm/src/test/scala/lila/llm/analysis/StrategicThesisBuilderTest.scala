package lila.llm.analysis

import munit.FunSuite
import lila.llm.model.*
import lila.llm.model.authoring.*

class StrategicThesisBuilderTest extends FunSuite:

  private def baseContext: NarrativeContext =
    NarrativeContext(
      fen = "4k3/8/8/8/8/8/8/4K3 w - - 0 1",
      header = ContextHeader("Middlegame", "Normal", "NarrowChoice", "Medium", "ExplainPlan"),
      ply = 24,
      playedMove = Some("h2h4"),
      playedSan = Some("h4"),
      summary = NarrativeSummary("Kingside expansion", None, "NarrowChoice", "Maintain", "0.00"),
      threats = ThreatTable(Nil, Nil),
      pawnPlay = PawnPlayTable(false, None, "Low", "Maintain", "Quiet", "Background", None, false, "quiet"),
      plans = PlanTable(
        top5 = List(
          PlanRow(
            rank = 1,
            name = "Kingside expansion",
            score = 0.82,
            evidence = List("space on the kingside"),
            confidence = ConfidenceLevel.Heuristic
          )
        ),
        suppressed = Nil
      ),
      delta = None,
      phase = PhaseContext("Middlegame", "Balanced middlegame"),
      candidates = Nil,
      renderMode = NarrativeRenderMode.Bookmaker
    )

  private def paragraphs(text: String): List[String] =
    Option(text).getOrElse("")
      .split("""\n\s*\n""")
      .map(_.trim)
      .filter(_.nonEmpty)
      .toList

  test("compensation lens drives bookmaker prose for long-term investment motif") {
    val ctx = baseContext.copy(
      semantic = Some(
        SemanticSection(
          structuralWeaknesses = Nil,
          pieceActivity = Nil,
          positionalFeatures = Nil,
          compensation = Some(
            CompensationInfo(
              investedMaterial = 120,
              returnVector = Map("Attack on King" -> 1.2, "Space Advantage" -> 0.8),
              expiryPly = None,
              conversionPlan = "Mating Attack"
            )
          ),
          endgameFeatures = None,
          practicalAssessment = None,
          preventedPlans = Nil,
          conceptSummary = Nil
        )
      ),
      whyAbsentFromTopMultiPV = List("the direct recapture loses the initiative"),
      authorEvidence = List(
        QuestionEvidence(
          questionId = "q1",
          purpose = "free_tempo_branches",
          branches = List(EvidenceBranch("...Qe7", "Qe7 h5 Rh6", Some(68), None, Some(20), Some("probe-1")))
        )
      )
    )

    val thesis = StrategicThesisBuilder.build(ctx).getOrElse(fail("missing compensation thesis"))
    assertEquals(thesis.lens, StrategicLens.Compensation)
    assert(thesis.claim.contains("120cp investment"))
    assert(thesis.claim.toLowerCase.contains("attack on king"))

    val prose = BookStyleRenderer.render(ctx)
    val paras = paragraphs(prose)
    assertEquals(paras.size, 3)
    assert(paras.head.toLowerCase.contains("120cp investment"))
    assert(paras(1).contains("Mating Attack"))
    assert(paras(1).toLowerCase.contains("initiative"))
    assert(paras(2).contains("Probe evidence"))
  }

  test("prophylaxis lens leads when counterplay denial is the key point") {
    val ctx = baseContext.copy(
      semantic = Some(
        SemanticSection(
          structuralWeaknesses = Nil,
          pieceActivity = Nil,
          positionalFeatures = Nil,
          compensation = None,
          endgameFeatures = None,
          practicalAssessment = None,
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
          conceptSummary = Nil
        )
      )
    )

    val thesis = StrategicThesisBuilder.build(ctx).getOrElse(fail("missing prophylaxis thesis"))
    assertEquals(thesis.lens, StrategicLens.Prophylaxis)
    assert(thesis.claim.toLowerCase.contains("cutting out counterplay"))

    val prose = BookStyleRenderer.render(ctx)
    val paras = paragraphs(prose)
    assertEquals(paras.size, 2)
    assert(paras.head.toLowerCase.contains("cutting out counterplay"))
    assert(paras(1).contains("140cp"))
    assert(paras(1).contains("Kingside expansion"))
  }

  test("structure lens names the structure and plan fit instead of flattening it") {
    val ctx = baseContext.copy(
      playedMove = Some("a1b1"),
      playedSan = Some("Rb1"),
      semantic = Some(
        SemanticSection(
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
          compensation = None,
          endgameFeatures = None,
          practicalAssessment = None,
          preventedPlans = Nil,
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
      ),
      mainStrategicPlans = List(
        PlanHypothesis(
          planId = "minority_attack",
          planName = "Minority Attack",
          rank = 1,
          score = 0.88,
          preconditions = Nil,
          executionSteps = Nil,
          failureModes = Nil,
          viability = PlanViability(0.82, "high", "slow"),
          themeL1 = "minority_attack"
        )
      )
    )

    val thesis = StrategicThesisBuilder.build(ctx).getOrElse(fail("missing structure thesis"))
    assertEquals(thesis.lens, StrategicLens.Structure)
    assert(thesis.claim.contains("Carlsbad"))
    assert(thesis.claim.toLowerCase.contains("minority attack"))
    assert(thesis.claim.toLowerCase.contains("rook"))
    assert(thesis.claim.toLowerCase.contains("b-file"))
    assert(thesis.support.exists(_.toLowerCase.contains("queenside pressure")))
    assert(thesis.support.exists(_.toLowerCase.contains("starts that route immediately")))

    val prose = BookStyleRenderer.render(ctx)
    val paras = paragraphs(prose)
    assertEquals(paras.size, 3)
    assert(paras.head.contains("Carlsbad"))
    assert(paras(1).toLowerCase.contains("queenside pressure"))
    assert(paras(1).toLowerCase.contains("starts that route immediately"))
    assert(paras(2).toLowerCase.contains("move order"))
  }

  test("off-plan structure keeps deployment as caution instead of the main claim") {
    val ctx = baseContext.copy(
      playedMove = Some("a1b1"),
      playedSan = Some("Rb1"),
      semantic = Some(
        SemanticSection(
          structuralWeaknesses = Nil,
          pieceActivity = List(
            PieceActivityInfo(
              piece = "Rook",
              square = "a1",
              mobilityScore = 0.38,
              isTrapped = false,
              isBadBishop = false,
              keyRoutes = List("b1", "b3"),
              coordinationLinks = List("b4")
            )
          ),
          positionalFeatures = Nil,
          compensation = None,
          endgameFeatures = None,
          practicalAssessment = None,
          preventedPlans = Nil,
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
              score = 34,
              band = "OffPlan",
              matchedPlanIds = Nil,
              missingPlanIds = List("minority_attack"),
              reasonCodes = List("ANTI_PLAN"),
              narrativeIntent = Some("play around queenside pressure"),
              narrativeRisk = Some("the move order fights the structure")
            )
          )
        )
      ),
      mainStrategicPlans = List(
        PlanHypothesis(
          planId = "minority_attack",
          planName = "Minority Attack",
          rank = 1,
          score = 0.88,
          preconditions = Nil,
          executionSteps = Nil,
          failureModes = Nil,
          viability = PlanViability(0.82, "high", "slow"),
          themeL1 = "minority_attack"
        )
      )
    )

    val thesis = StrategicThesisBuilder.build(ctx).getOrElse(fail("missing structure thesis"))
    assertEquals(thesis.lens, StrategicLens.Structure)
    assert(thesis.claim.contains("Carlsbad"))
    assert(!thesis.claim.toLowerCase.contains("rook belongs on the b-file"))
    assert(thesis.support.exists(_.toLowerCase.contains("still wants the b-file")))
  }

  test("decision lens makes the chosen route and deferred alternative explicit") {
    val ctx = baseContext.copy(
      decision = Some(
        DecisionRationale(
          focalPoint = Some(TargetSquare("g7")),
          logicSummary = "Resolve back-rank mate -> create pressure on g7",
          delta = PVDelta(
            resolvedThreats = List("back-rank mate"),
            newOpportunities = List("g7"),
            planAdvancements = List("Met: rook lift"),
            concessions = List("dark-square drift")
          ),
          confidence = ConfidenceLevel.Probe
        )
      ),
      whyAbsentFromTopMultiPV = List("the immediate 'g4' push loses 220 cp"),
      authorEvidence = List(
        QuestionEvidence(
          questionId = "q2",
          purpose = "latent_plan_refutation",
          branches = List(EvidenceBranch("...Qf6", "Qf6 g4 Qxd4", Some(-220), None, Some(22), Some("probe-2")))
        )
      )
    )

    val thesis = StrategicThesisBuilder.build(ctx).getOrElse(fail("missing decision thesis"))
    assertEquals(thesis.lens, StrategicLens.Decision)
    assert(thesis.claim.toLowerCase.contains("postpone"))

    val prose = BookStyleRenderer.render(ctx)
    val paras = paragraphs(prose)
    assertEquals(paras.size, 3)
    assert(paras.head.toLowerCase.contains("postpone"))
    assert(paras(1).toLowerCase.contains("resolving back-rank mate"))
    assert(paras(2).contains("Probe evidence"))
    assert(paras(2).toLowerCase.contains("immediate"))
  }

  test("practical lens foregrounds workload drivers over tiny eval edges") {
    val ctx = baseContext.copy(
      semantic = Some(
        SemanticSection(
          structuralWeaknesses = Nil,
          pieceActivity = Nil,
          positionalFeatures = Nil,
          compensation = None,
          endgameFeatures = None,
          practicalAssessment = Some(
            PracticalInfo(
              engineScore = 18,
              practicalScore = 74.0,
              verdict = "Comfortable",
              biasFactors = List(
                PracticalBiasInfo("Mobility", "Diff: 1.8", 36.0),
                PracticalBiasInfo("Forgiveness", "2 safe moves", -18.0)
              )
            )
          ),
          preventedPlans = Nil,
          conceptSummary = Nil
        )
      )
    )

    val thesis = StrategicThesisBuilder.build(ctx).getOrElse(fail("missing practical thesis"))
    assertEquals(thesis.lens, StrategicLens.Practical)
    assert(thesis.claim.toLowerCase.contains("practical task"))

    val prose = BookStyleRenderer.render(ctx)
    val paras = paragraphs(prose)
    assertEquals(paras.size, 2)
    assert(paras.head.toLowerCase.contains("practical task"))
    assert(paras(1).toLowerCase.contains("mobility"))
    assert(paras(1).toLowerCase.contains("forgiveness"))
  }

  test("opening lens keeps opening identity tied to the strategic purpose") {
    val ctx = baseContext.copy(
      openingData = Some(
        OpeningReference(
          eco = Some("E04"),
          name = Some("Catalan"),
          totalGames = 42,
          topMoves = Nil,
          sampleGames = Nil
        )
      ),
      openingEvent = Some(OpeningEvent.Intro("E04", "Catalan", "queenside pressure", Nil)),
      mainStrategicPlans = List(
        PlanHypothesis(
          planId = "pressure_c_file",
          planName = "Queenside Pressure",
          rank = 1,
          score = 0.79,
          preconditions = Nil,
          executionSteps = Nil,
          failureModes = Nil,
          viability = PlanViability(0.74, "medium", "slow"),
          themeL1 = "open_file"
        )
      )
    )

    val thesis = StrategicThesisBuilder.build(ctx).getOrElse(fail("missing opening thesis"))
    assertEquals(thesis.lens, StrategicLens.Opening)
    assert(thesis.claim.contains("Catalan"))
    assert(thesis.claim.toLowerCase.contains("queenside pressure"))

    val prose = BookStyleRenderer.render(ctx)
    val paras = paragraphs(prose)
    assertEquals(paras.size, 2)
    assert(paras.head.contains("Catalan"))
    assert(paras(1).toLowerCase.contains("queenside pressure"))
    assert(paras(1).toLowerCase.contains("territory"))
  }

  test("opening lens can cite a representative player game and strategic branch") {
    val ctx = baseContext.copy(
      playedMove = Some("b2b3"),
      playedSan = Some("b3"),
      openingData = Some(
        OpeningReference(
          eco = Some("E04"),
          name = Some("Catalan"),
          totalGames = 120,
          topMoves = List(
            ExplorerMove("b2b3", "b3", 38, 16, 10, 12, 2520),
            ExplorerMove("d4c5", "dxc5", 31, 13, 8, 10, 2510)
          ),
          sampleGames = List(
            ExplorerGame(
              id = "game1",
              winner = Some(chess.White),
              white = ExplorerPlayer("Kramnik, Vladimir", 2810),
              black = ExplorerPlayer("Anand, Viswanathan", 2791),
              year = 2008,
              month = 10,
              event = Some("WCh"),
              pgn = Some("9. b3 Qe7 10. Bb2 Rd8 11. Rc1")
            )
          )
        )
      ),
      openingEvent = Some(OpeningEvent.BranchPoint(List("Qc2", "b3", "dxc5"), "Main line shifts", Some("lichess.org/game1"))),
      mainStrategicPlans = List(
        PlanHypothesis(
          planId = "pressure_c_file",
          planName = "Queenside Pressure",
          rank = 1,
          score = 0.82,
          preconditions = Nil,
          executionSteps = Nil,
          failureModes = Nil,
          viability = PlanViability(0.76, "medium", "slow"),
          themeL1 = "open_file"
        )
      )
    )

    val thesis = StrategicThesisBuilder.build(ctx).getOrElse(fail("missing opening thesis"))
    assertEquals(thesis.lens, StrategicLens.Opening)
    assert(thesis.support.exists(_.contains("Vladimir Kramnik-Viswanathan Anand")))
    assert(thesis.support.exists(_.toLowerCase.contains("queenside pressure branch")))
    assert(thesis.support.exists(_.toLowerCase.contains("keeps the game inside")))

    val prose = BookStyleRenderer.render(ctx)
    val paras = paragraphs(prose)
    assert(paras.head.contains("Catalan"))
    assert(paras(1).contains("Vladimir Kramnik-Viswanathan Anand"))
    assert(paras(1).toLowerCase.contains("queenside pressure branch"))
    assert(paras(1).toLowerCase.contains("keeps the game inside"))
  }
