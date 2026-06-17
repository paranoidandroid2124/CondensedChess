package lila.commentary.analysis

import munit.FunSuite
import lila.commentary.model.*
import lila.commentary.model.authoring.*
import lila.commentary.model.strategic.{ EngineEvidence, VariationLine }

class NarrativeSignalConsumptionTest extends FunSuite:

  private def baseContext: NarrativeContext =
    NarrativeContext(
      fen = "4k3/8/8/8/8/8/8/4K3 w - - 0 1",
      header = ContextHeader("Middlegame", "Normal", "NarrowChoice", "Medium", "ExplainPlan"),
      ply = 24,
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
      renderMode = NarrativeRenderMode.FullGame
    )

  test("context beat keeps support-only flow/meta while final prose drops opponent row authority") {
    val ctx = baseContext.copy(
      strategicFlow = Some(
        "White's plan context shifts toward kingside expansion. The continuation remains structurally coherent. This idea has held for 3 plies."
      ),
      opponentPlan = Some(
        PlanRow(
          rank = 1,
          name = "Queenside counterplay",
          score = 0.71,
          evidence = List("pressure on the c-file"),
          confidence = ConfidenceLevel.Heuristic
        )
      ),
      meta = Some(
        MetaSignals(
          choiceType = ChoiceType.NarrowChoice,
          targets = Targets(Nil, Nil),
          planConcurrency = PlanConcurrency(
            primary = "Kingside expansion",
            secondary = Some("central pressure"),
            relationship = "synergy"
          )
        )
      )
    )

    val (outline, _) = NarrativeOutlineBuilder.build(ctx, new TraceRecorder())
    val contextBeat = outline.beats.find(_.kind == OutlineBeatKind.Context).getOrElse(fail("missing context beat"))

    assert(
      contextBeat.text.contains("plan context shifts toward kingside expansion"),
      s"expected strategic flow text in context beat, got: ${contextBeat.text}"
    )
    assert(!contextBeat.text.contains("opponent's main counterplan"), clue(contextBeat.text))
    assert(!contextBeat.text.contains("Queenside counterplay"), clue(contextBeat.text))
    assert(
      contextBeat.text.contains("The margins are narrow, so move order matters."),
      s"expected meta choice text in context beat, got: ${contextBeat.text}"
    )
    assert(
      contextBeat.text.contains("Kingside expansion and central pressure reinforce each other."),
      s"expected plan concurrency text in context beat, got: ${contextBeat.text}"
    )

    val prose = BookStyleRenderer.render(ctx)
    assert(!prose.contains("opponent's main counterplan"), clue(prose))
    assert(!prose.contains("Queenside counterplay"), clue(prose))
    assert(
      prose.contains("not much to claim"),
      s"expected quiet standard fallback without row/meta authority, got: $prose"
    )
    assert(!prose.contains("The margins are narrow, so move order matters."), clue(prose))
  }

  test("context beat carries transposition-aligned main plans without probe-only support") {
    val plan =
      PlanHypothesis(
        planId = "StaticWeakness",
        planName = "Transposed d5 pressure",
        rank = 1,
        score = 0.82,
        preconditions = Nil,
        executionSteps = Nil,
        failureModes = Nil,
        viability = PlanViability(0.7, "medium", "test"),
        evidenceSources = List("weakness_target:d5"),
        themeL1 = PlanTaxonomy.PlanTheme.WeaknessFixation.id,
        subplanId = Some(PlanTaxonomy.PlanKind.StaticWeaknessFixation.id)
      )
    val evaluated =
      PlanEvidenceEvaluator.EvaluatedPlan(
        hypothesis = plan,
        status = PlanEvidenceEvaluator.PlanEvidenceStatus.PlayableTranspositionAligned,
        userFacingEligibility = PlanEvidenceEvaluator.UserFacingPlanEligibility.TranspositionAligned,
        reason = "test transposition proof",
        transpositionProofIds = List("transposition:staticweakness:d5:fixed_pawn"),
        themeL1 = PlanTaxonomy.PlanTheme.WeaknessFixation.id,
        subplanId = Some(PlanTaxonomy.PlanKind.StaticWeaknessFixation.id),
        claimCertification =
          PlanEvidenceEvaluator.ClaimCertification(
            provenanceClass = PlayerFacingClaimProvenanceClass.TranspositionAligned
          )
      )
    val ctx =
      baseContext.copy(
        strategicPlanEvidence =
          PlanEvidenceEvaluator.StrategicPlanEvidenceView(
            selectedPlans = List(evaluated),
            evaluatedPlans = List(evaluated)
          )
      )

    assertEquals(StrategicNarrativePlanSupport.evidenceBackedMainPlans(ctx), Nil)

    val (outline, _) = NarrativeOutlineBuilder.build(ctx, new TraceRecorder())
    val contextBeat = outline.beats.find(_.kind == OutlineBeatKind.Context).getOrElse(fail("missing context beat"))

    assert(
      contextBeat.text.contains("The main plan remains Transposed d5 pressure."),
      s"expected transposition-aligned main plan in context beat, got: ${contextBeat.text}"
    )
  }

  test("context concept aliases do not bypass draw-resource relation gates") {
    val ctx =
      baseContext.copy(
        semantic = Some(
          SemanticSection(
            structuralWeaknesses = Nil,
            pieceActivity = Nil,
            positionalFeatures = Nil,
            compensation = None,
            endgameFeatures = None,
            practicalAssessment = None,
            preventedPlans = Nil,
            conceptSummary = List("stalemate_trap", "perpetual_check")
          )
        )
      )

    val (outline, _) = NarrativeOutlineBuilder.build(ctx, new TraceRecorder())
    val contextBeat = outline.beats.find(_.kind == OutlineBeatKind.Context).getOrElse(fail("missing context beat"))
    val lower = contextBeat.text.toLowerCase

    assert(!lower.contains("stalemate"), clue(contextBeat.text))
    assert(!lower.contains("perpetual"), clue(contextBeat.text))
  }

  test("raw structural motif labels do not become context motif prose") {
    val ctx =
      baseContext.copy(
        semantic = Some(
          SemanticSection(
            structuralWeaknesses = Nil,
            pieceActivity = Nil,
            positionalFeatures = List(
              PositionalTagInfo("ColorComplexWeakness", None, None, "Black", Some("dark squares")),
              PositionalTagInfo("Outpost", Some("d5"), None, "White")
            ),
            compensation = None,
            endgameFeatures = None,
            practicalAssessment = None,
            preventedPlans = Nil,
            conceptSummary = List("color complex", "minority attack", "outpost")
          )
        )
      )

    val (outline, _) = NarrativeOutlineBuilder.build(ctx, new TraceRecorder())
    val contextBeat = outline.beats.find(_.kind == OutlineBeatKind.Context).getOrElse(fail("missing context beat"))
    val lower = contextBeat.text.toLowerCase

    assert(!lower.contains("color complex"), clue(contextBeat.text))
    assert(!lower.contains("minority attack"), clue(contextBeat.text))
    assert(!lower.contains("outpost"), clue(contextBeat.text))
  }

  test("raw positional imbalance tags do not become opening-context conclusions") {
    val ctx =
      baseContext.copy(
        engineEvidence = Some(EngineEvidence(depth = 18, variations = List(VariationLine(Nil, scoreCp = 0, depth = 18)))),
        semantic = Some(
          SemanticSection(
            structuralWeaknesses =
              List(WeakComplexInfo(owner = "White", squareColor = "dark", squares = List("f3", "g2"), isOutpost = false, cause = "holes")),
            pieceActivity = Nil,
            positionalFeatures = List(
              PositionalTagInfo("Outpost", Some("d5"), None, "White"),
              PositionalTagInfo("SpaceAdvantage", None, None, "White")
            ),
            compensation = None,
            endgameFeatures = None,
            practicalAssessment = None,
            preventedPlans = Nil,
            conceptSummary = Nil
          )
        )
      )

    val (outline, _) = NarrativeOutlineBuilder.build(ctx, new TraceRecorder())
    val contextBeat = outline.beats.find(_.kind == OutlineBeatKind.Context).getOrElse(fail("missing context beat"))
    val lower = contextBeat.text.toLowerCase

    assert(!lower.contains("outpost"), clue(contextBeat.text))
    assert(!lower.contains("pressure on white"), clue(contextBeat.text))
    assert(!lower.contains("dark-square weaknesses"), clue(contextBeat.text))
  }

  test("signal digest uses opening names but not opening event diagnostic strings") {
    val introCtx =
      baseContext.copy(
        openingEvent = Some(OpeningEvent.Intro("C20", "Open Game", "central development", List("e4", "e5")))
      )
    val noveltyCtx =
      baseContext.copy(
        openingEvent = Some(OpeningEvent.Novelty("h3", 12, "novelty marker", 16))
      )
    val branchCtx =
      baseContext.copy(
        openingEvent = Some(OpeningEvent.BranchPoint(List("Qc2", "b3"), "Main line shifts", None))
      )

    assertEquals(NarrativeSignalDigestBuilder.build(introCtx).flatMap(_.opening), Some("Open Game"))
    assertEquals(NarrativeSignalDigestBuilder.build(noveltyCtx).flatMap(_.opening), None)
    assertEquals(NarrativeSignalDigestBuilder.build(branchCtx).flatMap(_.opening), None)
  }

  test("decision rationale without author questions stays out of planner-owned decision beats") {
    val ctx = baseContext.copy(
      decision = Some(
        DecisionRationale(
          focalPoint = Some(TargetSquare("g7")),
          logicSummary = "Resolves back-rank mate -> creates pressure on g7",
          delta = PVDelta(
            resolvedThreats = List("back-rank mate"),
            newOpportunities = List("g7"),
            planAdvancements = List("Met: rook lift"),
            concessions = List("dark-square drift")
          ),
          confidence = ConfidenceLevel.Probe
        )
      ),
      meta = Some(
        MetaSignals(
          choiceType = ChoiceType.OnlyMove,
          targets = Targets(Nil, Nil),
          planConcurrency = PlanConcurrency("Kingside attack", None, "independent"),
          whyNot = Some("'g4' is refuted losing 220 cp [Verified]")
        )
      )
    )

    val (outline, _) = NarrativeOutlineBuilder.build(ctx, new TraceRecorder())
    assertEquals(outline.beats.find(_.kind == OutlineBeatKind.DecisionPoint), None, clues(outline.beats))

    val prose = BookStyleRenderer.render(ctx)
    assert(
      !prose.contains("The idea is straightforward: resolves back-rank mate") &&
        !prose.contains("The tradeoff is dark-square drift.") &&
        !prose.contains("'g4' is refuted"),
      s"expected uncited decision rationale to stay out of final prose, got: $prose"
    )
  }

  test("structure prophylaxis practicality and compensation details are promoted into prose") {
    val ctx = baseContext.copy(
      semantic = Some(
        SemanticSection(
          structuralWeaknesses = Nil,
          pieceActivity = Nil,
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
              engineScore = 35,
              practicalScore = 74.0,
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
      ),
      candidates = List(
        CandidateInfo(
          move = "Rb1",
          annotation = "!",
          planAlignment = "Queenside pressure",
          tacticalAlert = None,
          practicalDifficulty = "clean",
          whyNot = None
        )
      )
    )

    val prose = BookStyleRenderer.render(ctx)
    assert(prose.contains("Carlsbad"), s"expected structure profile in prose, got: $prose")
    assert(prose.toLowerCase.contains("locked center"), s"expected center-state clue in prose, got: $prose")
    assert(prose.contains("cuts out counterplay"), s"expected prophylaxis detail in prose, got: $prose")
    assert(prose.contains("pieces have more available squares"), s"expected rendered practical driver in prose, got: $prose")
    assert(!prose.contains("position is comfortable"), s"raw practical verdict should not become public prose: $prose")
    assert(!prose.contains("Mating Attack"), s"raw compensation plan should not become public prose: $prose")
    assert(!prose.contains("Attack on King"), s"raw compensation vector should not become public prose: $prose")
  }

  test("signal digest keeps semantic compensation conversion plan out of public summary") {
    val ctx = baseContext.copy(
      semantic = Some(
        SemanticSection(
          structuralWeaknesses = Nil,
          pieceActivity = Nil,
          positionalFeatures = Nil,
          compensation = Some(
            CompensationInfo(
              investedMaterial = 100,
              returnVector = Map("Line Pressure" -> 0.7, "Delayed Recovery" -> 0.6),
              expiryPly = None,
              conversionPlan = "Mating Attack"
            )
          ),
          endgameFeatures = None,
          practicalAssessment = None,
          preventedPlans = Nil,
          conceptSummary = Nil,
          structureProfile = None,
          planAlignment = None
        )
      )
    )

    val digest = NarrativeSignalDigestBuilder.build(ctx).getOrElse(fail("missing signal digest"))
    assertEquals(digest.compensation, None)
    assertEquals(digest.investedMaterial, Some(100))
    assert(digest.compensationVectors.exists(_.startsWith("Line Pressure")), clue(digest.compensationVectors))
    assert(digest.preservedSignals.contains("practical"), clue(digest.preservedSignals))
  }

  test("signal digest keeps development-lead compensation support-only without pressure carrier") {
    val ctx = baseContext.copy(
      fen = "r1bqkb1r/1p3ppp/p1nppn2/8/2B1P3/2N2N2/PP2QPPP/R1B2RK1 w kq - 0 9",
      ply = 17,
      phase = PhaseContext("Opening", "Development lead"),
      semantic = Some(
        SemanticSection(
          structuralWeaknesses = Nil,
          pieceActivity = Nil,
          positionalFeatures = Nil,
          compensation = Some(
            CompensationInfo(
              investedMaterial = 100,
              returnVector = Map("Development Lead" -> 0.8),
              expiryPly = None,
              conversionPlan = "development lead"
            )
          ),
          endgameFeatures = None,
          practicalAssessment = None,
          preventedPlans = Nil,
          conceptSummary = Nil,
          structureProfile = None,
          planAlignment = None
        )
      )
    )

    val digest = NarrativeSignalDigestBuilder.build(ctx)
    assertEquals(digest.flatMap(_.investedMaterial), None)
    assertEquals(digest.map(_.compensationVectors).getOrElse(Nil), Nil)
    assert(!digest.exists(_.preservedSignals.contains("practical")), clue(digest))
  }
