package lila.llm.analysis

import munit.FunSuite
import lila.llm.model.*
import lila.llm.model.authoring.*

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
      candidates = Nil
    )

  test("context beat carries strategic flow, opponent plan, and meta choice signals into prose") {
    val ctx = baseContext.copy(
      strategicFlow = Some(
        "White makes a natural strategic shift toward kingside expansion. The continuation remains structurally coherent. This idea has held for 3 plies."
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
      contextBeat.text.contains("natural strategic shift toward kingside expansion"),
      s"expected strategic flow text in context beat, got: ${contextBeat.text}"
    )
    assert(
      contextBeat.text.contains("opponent's main counterplan is Queenside counterplay"),
      s"expected opponent plan text in context beat, got: ${contextBeat.text}"
    )
    assert(
      contextBeat.text.contains("The margins are narrow, so move order matters."),
      s"expected meta choice text in context beat, got: ${contextBeat.text}"
    )
    assert(
      contextBeat.text.contains("Kingside expansion and central pressure reinforce each other."),
      s"expected plan concurrency text in context beat, got: ${contextBeat.text}"
    )

    val prose = BookStyleRenderer.render(ctx)
    assert(
      prose.contains("opponent's main counterplan is Queenside counterplay"),
      s"expected opponent plan to reach final prose, got: $prose"
    )
    assert(
      prose.contains("The margins are narrow, so move order matters."),
      s"expected meta choice to reach final prose, got: $prose"
    )
  }

  test("decision beat is emitted from decision rationale and meta signals even without author questions") {
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
    val decisionBeat = outline.beats.find(_.kind == OutlineBeatKind.DecisionPoint).getOrElse(fail("missing decision beat"))

    assert(
      decisionBeat.text.contains("The idea is straightforward: resolves back-rank mate"),
      s"expected decision logic in decision beat, got: ${decisionBeat.text}"
    )
    assert(
      decisionBeat.text.contains("The focal point is g7."),
      s"expected focal point in decision beat, got: ${decisionBeat.text}"
    )
    assert(
      decisionBeat.text.contains("Probe evidence says 'g4' is refuted losing 220 cp."),
      s"expected why-not meta in decision beat, got: ${decisionBeat.text}"
    )

    val prose = BookStyleRenderer.render(ctx)
    assert(
      prose.contains("The idea is straightforward: resolves back-rank mate"),
      s"expected decision logic to reach final prose, got: $prose"
    )
    assert(
      prose.contains("Probe evidence says 'g4' is refuted losing 220 cp."),
      s"expected why-not meta to reach final prose, got: $prose"
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
    assert(prose.toLowerCase.contains("mobility"), s"expected practical drivers in prose, got: $prose")
    assert(prose.contains("Mating Attack"), s"expected compensation plan in prose, got: $prose")
  }
