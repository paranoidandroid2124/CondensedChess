package lila.llm.analysis

import munit.FunSuite
import lila.llm.*
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

  private def surfaceDrivenPack(
      compensation: Option[String] = None,
      compensationVectors: List[String] = Nil,
      investedMaterial: Option[Int] = None
  ): StrategyPack =
    StrategyPack(
      sideToMove = "white",
      strategicIdeas = List(
        StrategyIdeaSignal(
          ideaId = "idea_attack_g7",
          ownerSide = "white",
          kind = StrategicIdeaKind.KingAttackBuildUp,
          group = StrategicIdeaGroup.InteractionAndTransformation,
          readiness = StrategicIdeaReadiness.Build,
          focusSquares = List("g7"),
          focusZone = Some("kingside"),
          beneficiaryPieces = List("Q", "R"),
          confidence = 0.91
        )
      ),
      pieceRoutes = List(
        StrategyPieceRoute(
          ownerSide = "white",
          piece = "R",
          from = "c3",
          route = List("c3", "g3"),
          purpose = "rook lift",
          strategicFit = 0.88,
          tacticalSafety = 0.74,
          surfaceConfidence = 0.82,
          surfaceMode = RouteSurfaceMode.Toward
        )
      ),
      directionalTargets = List(
        StrategyDirectionalTarget(
          targetId = "target_g7",
          ownerSide = "white",
          piece = "Q",
          from = "d1",
          targetSquare = "g7",
          readiness = DirectionalTargetReadiness.Build,
          strategicReasons = List("mating net")
        )
      ),
      longTermFocus = List("keep pressure on g7"),
      signalDigest = Some(
        NarrativeSignalDigest(
          compensation = compensation,
          compensationVectors = compensationVectors,
          investedMaterial = investedMaterial,
          dominantIdeaKind = Some(StrategicIdeaKind.KingAttackBuildUp),
          dominantIdeaGroup = Some(StrategicIdeaGroup.InteractionAndTransformation),
          dominantIdeaReadiness = Some(StrategicIdeaReadiness.Build),
          dominantIdeaFocus = Some("g7")
        )
      )
    )

  private def quietCompensationPack: StrategyPack =
    StrategyPack(
      sideToMove = "black",
      strategicIdeas = List(
        StrategyIdeaSignal(
          ideaId = "idea_bfile_fixation",
          ownerSide = "black",
          kind = StrategicIdeaKind.TargetFixing,
          group = "slow_structural",
          readiness = StrategicIdeaReadiness.Build,
          focusSquares = List("b2", "b3"),
          focusFiles = List("b"),
          focusZone = Some("queenside"),
          beneficiaryPieces = List("R"),
          confidence = 0.88
        )
      ),
      pieceRoutes = List(
        StrategyPieceRoute(
          ownerSide = "black",
          piece = "R",
          from = "a8",
          route = List("a8", "b8", "b4"),
          purpose = "queenside pressure",
          strategicFit = 0.84,
          tacticalSafety = 0.78,
          surfaceConfidence = 0.80,
          surfaceMode = RouteSurfaceMode.Toward
        )
      ),
      longTermFocus = List("fix the queenside targets before recovering the pawn"),
      signalDigest = Some(
        NarrativeSignalDigest(
          compensation = Some("return vector through line pressure and delayed recovery"),
          compensationVectors = List("Line Pressure (0.7)", "Delayed Recovery (0.6)"),
          investedMaterial = Some(100),
          dominantIdeaKind = Some(StrategicIdeaKind.TargetFixing),
          dominantIdeaGroup = Some("slow_structural"),
          dominantIdeaReadiness = Some(StrategicIdeaReadiness.Build),
          dominantIdeaFocus = Some("b2, b3")
        )
      )
    )

  private def benkoLikeCompensationPack: StrategyPack =
    StrategyPack(
      sideToMove = "black",
      strategicIdeas = List(
        StrategyIdeaSignal(
          ideaId = "idea_benko_line",
          ownerSide = "black",
          kind = StrategicIdeaKind.LineOccupation,
          group = "slow_structural",
          readiness = StrategicIdeaReadiness.Build,
          focusSquares = List("b2", "c4", "d4"),
          focusFiles = List("b", "c", "d"),
          focusZone = Some("queenside"),
          beneficiaryPieces = List("R", "Q"),
          confidence = 0.86
        ),
        StrategyIdeaSignal(
          ideaId = "idea_benko_targets",
          ownerSide = "black",
          kind = StrategicIdeaKind.TargetFixing,
          group = "slow_structural",
          readiness = StrategicIdeaReadiness.Build,
          focusSquares = List("b2", "a6"),
          focusFiles = List("a", "b"),
          focusZone = Some("queenside"),
          beneficiaryPieces = List("R"),
          confidence = 0.79
        )
      ),
      pieceRoutes = List(
        StrategyPieceRoute(
          ownerSide = "black",
          piece = "R",
          from = "a8",
          route = List("a8", "d8", "d3"),
          purpose = "kingside clamp",
          strategicFit = 0.82,
          tacticalSafety = 0.77,
          surfaceConfidence = 0.79,
          surfaceMode = RouteSurfaceMode.Toward
        )
      ),
      pieceMoveRefs = List(
        StrategyPieceMoveRef(
          ownerSide = "black",
          piece = "Q",
          from = "d8",
          target = "b6",
          idea = "fix the queenside targets"
        )
      ),
      directionalTargets = List(
        StrategyDirectionalTarget(
          targetId = "target_b2",
          ownerSide = "black",
          piece = "R",
          from = "d8",
          targetSquare = "b2",
          readiness = DirectionalTargetReadiness.Build,
          strategicReasons = List("backward pawn")
        )
      ),
      longTermFocus = List("fix the queenside targets before recovering the pawn"),
      signalDigest = Some(
        NarrativeSignalDigest(
          compensation = Some("return vector through line pressure and delayed recovery"),
          compensationVectors = List("Line Pressure (0.7)", "Delayed Recovery (0.6)", "Fixed Targets (0.5)"),
          investedMaterial = Some(100),
          dominantIdeaKind = Some(StrategicIdeaKind.LineOccupation),
          dominantIdeaGroup = Some("slow_structural"),
          dominantIdeaReadiness = Some(StrategicIdeaReadiness.Build),
          dominantIdeaFocus = Some("b2, c4, d4")
        )
      )
    )

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
    assert(thesis.claim.contains("120cp compensation investment"))
    assert(thesis.claim.toLowerCase.contains("attack on king"))

    val prose = BookStyleRenderer.render(ctx)
    val paras = paragraphs(prose)
    assertEquals(paras.size, 3)
    assert(paras.head.toLowerCase.contains("120cp compensation investment"))
    assert(paras(1).contains("Mating Attack"))
    assert(paras(1).toLowerCase.contains("initiative"))
    assert(paras(2).contains("Probe evidence"))
  }

  test("strategy-pack compensation fallback survives without semantic compensation") {
    val thesis =
      StrategicThesisBuilder
        .build(baseContext, Some(surfaceDrivenPack(compensation = Some("initiative against the king"), investedMaterial = Some(180))))
        .getOrElse(fail("missing compensation thesis from strategy pack"))

    assertEquals(thesis.lens, StrategicLens.Compensation)
    assert(thesis.claim.contains("180cp compensation investment"))
    assert(thesis.claim.toLowerCase.contains("compensation"))
    assert(thesis.claim.toLowerCase.contains("g7") || thesis.claim.toLowerCase.contains("dominant thesis"))
    assert(
      thesis.claim.toLowerCase.contains("initiative against the king") ||
        thesis.support.exists(text => {
          val low = text.toLowerCase
          low.contains("initiative") || low.contains("cash out") || low.contains("return vector")
        })
    )
    assert(thesis.support.headOption.exists(text => {
      val low = text.toLowerCase
      low.contains("initiative") || low.contains("cash out") || low.contains("return vector") || low.contains("delayed recovery")
    }))
  }

  test("strategy-pack compensation vectors drive thesis wording without semantic compensation") {
    val thesis =
      StrategicThesisBuilder
        .build(
          baseContext,
          Some(
            surfaceDrivenPack(
              compensationVectors = List("Initiative (0.7)", "Line Pressure (0.6)", "Delayed Recovery (0.5)"),
              investedMaterial = Some(200)
            )
          )
        )
        .getOrElse(fail("missing compensation thesis from digest vectors"))

    assertEquals(thesis.lens, StrategicLens.Compensation)
    assert(thesis.claim.contains("200cp compensation investment"))
    assert(
      thesis.claim.toLowerCase.contains("initiative") ||
        thesis.claim.toLowerCase.contains("line pressure"),
      clue(thesis.claim)
    )
    assert(thesis.claim.toLowerCase.contains("g7") || thesis.claim.toLowerCase.contains("dominant thesis"))
    assert(
      thesis.support.exists(text => {
        val low = text.toLowerCase
        low.contains("line pressure") ||
          low.contains("dragged back to the king") ||
          low.contains("defenders keep getting dragged back")
      }),
      clue(thesis.support)
    )
    assert(
      thesis.support.exists(text =>
        text.toLowerCase.contains("delayed recovery") ||
          text.toLowerCase.contains("return vector") ||
          text.toLowerCase.contains("durable") ||
          text.toLowerCase.contains("before recovering the material") ||
          text.toLowerCase.contains("keep the compensation alive")
      ),
      clue(thesis.support)
    )
  }

  test("quiet positional compensation keeps subtype-specific file pressure language") {
    val thesis =
      StrategicThesisBuilder
        .build(baseContext, Some(quietCompensationPack))
        .getOrElse(fail("missing quiet compensation thesis"))

    assertEquals(thesis.lens, StrategicLens.Compensation)
    assert(thesis.claim.toLowerCase.contains("fixed queenside targets") || thesis.claim.toLowerCase.contains("queenside file pressure"))
    assert(!thesis.claim.toLowerCase.contains("attack on king"))
    assert(thesis.support.exists(text => text.toLowerCase.contains("file pressure") || text.toLowerCase.contains("open-line pressure")), clue(thesis.support))
    assert(thesis.support.exists(text => text.toLowerCase.contains("invested") || text.toLowerCase.contains("targets")), clue(thesis.support))
  }

  test("Benko-like compensation stays queenside and target-led despite central support squares") {
    val surface = StrategyPackSurface.from(Some(benkoLikeCompensationPack))
    assertEquals(surface.compensationSubtype.map(_.pressureTheater), Some("queenside"))
    assertEquals(surface.compensationSubtype.map(_.pressureMode), Some("target_fixing"))
    assert(surface.normalizationActive)
    assert(surface.normalizationConfidence >= 6)
    assertEquals(surface.dominantIdeaText, Some("fixed queenside targets"))
    assert(surface.executionText.exists(_.toLowerCase.contains("queenside targets")), clue(surface.executionText))
    assert(!surface.executionText.exists(_.toLowerCase.contains("kingside clamp")), clue(surface.executionText))
    assert(
      surface.focusText.exists(text =>
        text.toLowerCase.contains("queenside targets") || text.toLowerCase.contains("queenside files")
      ),
      clue(surface.focusText)
    )

    val thesis =
      StrategicThesisBuilder
        .build(baseContext, Some(benkoLikeCompensationPack))
        .getOrElse(fail("missing Benko-like compensation thesis"))

    assert(thesis.claim.toLowerCase.contains("fixed queenside targets"), clue(thesis.claim))
    assert(!thesis.claim.toLowerCase.contains("kingside clamp"), clue(thesis.claim))
    assert(
      thesis.support.exists(text =>
        text.toLowerCase.contains("queenside targets") || text.toLowerCase.contains("queenside file pressure")
      ),
      clue(thesis.support)
    )
  }

  test("attack-led compensation keeps raw kingside attack wording when normalization confidence is low") {
    val surface = StrategyPackSurface.from(BookmakerProseGoldenFixtures.exchangeSacrifice.strategyPack)

    assert(!surface.normalizationActive, clue(surface.displayNormalization))
    assert(
      surface.dominantIdeaText.exists(text =>
        text.toLowerCase.contains("king-attack") || text.toLowerCase.contains("king attack")
      ),
      clue(surface.dominantIdeaText)
    )
    assert(
      surface.executionText.exists(text =>
        text.toLowerCase.contains("h5") || text.toLowerCase.contains("mate threats")
      ),
      clue(surface.executionText)
    )
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

  test("decision lens uses strategy-pack thesis before generic route scaffolding when available") {
    val ctx = baseContext.copy(
      decision = Some(
        DecisionRationale(
          focalPoint = Some(TargetSquare("g7")),
          logicSummary = "contest the c-file -> switch the rook to g3 -> pressure g7",
          delta = PVDelta(
            resolvedThreats = List("back-rank mate"),
            newOpportunities = List("g7"),
            planAdvancements = List("Met: rook lift"),
            concessions = List("queenside simplification")
          ),
          confidence = ConfidenceLevel.Probe
        )
      ),
      whyAbsentFromTopMultiPV = List("the immediate 'Qh5' thrust lets Black trade queens and kill the attack"),
      authorEvidence = List(
        QuestionEvidence(
          questionId = "q-open-file",
          purpose = "latent_plan_refutation",
          branches = List(
            EvidenceBranch("...Rc8", "Rc8 Rc3 Rg6", Some(42), None, Some(23), Some("probe-open-file"))
          )
        )
      )
    )

    val thesis =
      StrategicThesisBuilder
        .build(ctx, Some(surfaceDrivenPack()))
        .getOrElse(fail("missing strategy-pack-backed decision thesis"))

    assertEquals(thesis.lens, StrategicLens.Decision)
    assert(!thesis.claim.contains("The key decision is to choose"))
    assert(thesis.claim.toLowerCase.contains("dominant thesis"))
    assert(thesis.claim.toLowerCase.contains("g7"))
    assert(thesis.claim.toLowerCase.contains("execution"))
    assert(thesis.support.exists(_.toLowerCase.contains("the objective is")))
    assert(thesis.support.exists(_.toLowerCase.contains("stays secondary because")))
    assert(!thesis.support.exists(_.toLowerCase.contains("the whole decision turns on")))
  }

  test("compensation-flavored decision surface avoids whole-decision fallback and uses compensation lexicon") {
    val ctx = baseContext.copy(
      decision = Some(
        DecisionRationale(
          focalPoint = Some(TargetSquare("g7")),
          logicSummary = "switch the rook and keep the attack alive",
          delta = PVDelta(
            resolvedThreats = List("trade into a worse ending"),
            newOpportunities = List("g7"),
            planAdvancements = List("Met: line pressure"),
            concessions = Nil
          ),
          confidence = ConfidenceLevel.Probe
        )
      ),
      whyAbsentFromTopMultiPV = List("the direct recapture kills the attack"),
      authorEvidence = List(
        QuestionEvidence(
          questionId = "q-comp-surface",
          purpose = "latent_plan_refutation",
          branches = List(EvidenceBranch("...Qe7", "Qe7 h5 Rh6", Some(68), None, Some(21), Some("probe-comp-surface")))
        )
      )
    )

    val thesis =
      StrategicThesisBuilder
        .build(ctx, Some(surfaceDrivenPack(compensation = Some("initiative against the king"), investedMaterial = Some(180))))
        .getOrElse(fail("missing compensation-flavored decision thesis"))

    val claimLow = thesis.claim.toLowerCase
    assert(!claimLow.contains("the whole decision turns on"))
    assert(claimLow.contains("compensation") || claimLow.contains("initiative"))
    assert(thesis.support.exists(text => {
      val low = text.toLowerCase
      low.contains("cash out") || low.contains("compensation") || low.contains("initiative")
    }))
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
