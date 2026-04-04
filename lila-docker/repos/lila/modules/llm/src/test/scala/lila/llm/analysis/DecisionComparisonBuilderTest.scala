package lila.llm.analysis

import munit.FunSuite
import lila.llm.model.*
import lila.llm.model.strategic.{ CounterfactualMatch, EngineEvidence, PvMove, VariationLine }

class DecisionComparisonBuilderTest extends FunSuite:

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
      plans = PlanTable(Nil, Nil),
      delta = None,
      phase = PhaseContext("Middlegame", "Balanced middlegame"),
      candidates = Nil,
      renderMode = NarrativeRenderMode.Bookmaker
    )

  test("build falls back to engine-gap deferred branch when engine evidence and counterfactual diverge") {
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

    val ctx = baseContext.copy(
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

    val comparison = DecisionComparisonBuilder.build(ctx).getOrElse(fail("missing comparison"))
    assertEquals(comparison.chosenMove, Some("h4"))
    assertEquals(comparison.engineBestMove, Some("g4"))
    assertEquals(comparison.deferredMove, Some("g4"))
    assertEquals(comparison.deferredSource, Some("engine_gap"))
    assert(comparison.deferredReason.exists(_.contains("220cp")))
    assertEquals(comparison.cpLossVsChosen, Some(220))
    assertEquals(comparison.engineBestPv, List("g4", "...a6", "h5"))
    assert(!comparison.chosenMatchesBest)
  }

  test("build marks close candidate as practical alternative") {
    val ctx = baseContext.copy(
      candidates = List(
        CandidateInfo("h4", annotation = "!", planAlignment = "Kingside expansion", tacticalAlert = None, practicalDifficulty = "clean", whyNot = None),
        CandidateInfo("Rc3", annotation = "", planAlignment = "Rook lift", tacticalAlert = None, practicalDifficulty = "clean", whyNot = Some("it slows the direct attack"))
      ),
      engineEvidence = Some(
        EngineEvidence(
          depth = 20,
          variations = List(
            VariationLine(moves = List("h2h4", "a7a6"), scoreCp = 44),
            VariationLine(
              moves = List("a1c3", "a7a6"),
              scoreCp = 30,
              parsedMoves = List(PvMove("a1c3", "Rc3", "a1", "c3", "R", false, None, false))
            )
          )
        )
      )
    )

    val comparison = DecisionComparisonBuilder.build(ctx).getOrElse(fail("missing comparison"))
    assertEquals(comparison.deferredMove, Some("Rc3"))
    assertEquals(comparison.deferredSource, Some("close_candidate"))
    assertEquals(comparison.practicalAlternative, true)
    assert(comparison.deferredReason.exists(_.nonEmpty))
  }

  test("exact target-fixation row carries a comparative consequence against the top multipv alternative") {
    val variations =
      List(
        VariationLine(
          moves =
            List(
              "f3d2",
              "b8a6",
              "f2f3",
              "a6c7",
              "a2a4",
              "b7b6",
              "d2c4",
              "c8a6",
              "c1g5",
              "h7h6",
              "g5h4",
              "d8d7",
              "h4e1",
              "a6c4",
              "e2c4",
              "a7a6",
              "d1d3",
              "f6h5"
            ),
          scoreCp = 65,
          depth = 24
        ),
        VariationLine(
          moves =
            List(
              "d1c2",
              "c8g4",
              "c1f4",
              "f6h5",
              "f4e3",
              "h5f6",
              "h2h3",
              "g4f3",
              "e2f3",
              "b8d7",
              "a2a4",
              "a7a6",
              "a4a5",
              "a8b8",
              "a1a2",
              "h7h5",
              "f1e1",
              "d8e7",
              "e3d2",
              "c5c4",
              "d2e3",
              "e8c8"
            ),
          scoreCp = 54,
          depth = 24
        )
      )
    val data =
      CommentaryEngine
        .assessExtended(
          fen = "rnbqr1k1/pp3pbp/3p1np1/2pP4/4P3/2N2N2/PP2BPPP/R1BQ1RK1 w - - 2 1",
          variations = variations,
          phase = Some("middlegame"),
          ply = 20
        )
        .getOrElse(fail("missing exact comparative data"))
    val ctx =
      NarrativeContextBuilder
        .build(data, data.toContext, None)
        .copy(
          playedMove = Some("f3d2"),
          playedSan = Some("Nd2")
        )
    val pack = StrategyPackBuilder.build(data, ctx).getOrElse(fail("missing strategy pack"))
    val raw = DecisionComparisonBuilder.build(ctx).getOrElse(fail("missing comparison"))
    val mainBundle =
      MainPathMoveDeltaClaimBuilder
        .build(ctx, Some(pack), truthContract = None)
        .getOrElse(fail("missing main bundle"))

    val comparison =
      DecisionComparisonComparativeSupport
        .enrich(
          comparison = Some(raw),
          ctx = ctx,
          strategyPack = Some(pack),
          truthContract = None,
          mainBundleOverride = Some(mainBundle)
        )
        .getOrElse(fail("missing comparative support"))

    assertEquals(raw.chosenMove, Some("Nd2"))
    assertEquals(raw.engineBestMove, Some("Nd2"))
    assertEquals(raw.chosenMatchesBest, true)
    assertEquals(comparison.comparedMove, Some("Qc2"))
    assertEquals(comparison.comparativeSource, Some(DecisionComparisonComparativeSupport.ExactTargetFixationSource))
    assertEquals(
      comparison.comparativeConsequence,
      Some("Nd2 fixes d6 as the target; Qc2 leaves d6 unfixed on the compared branch.")
    )
  }

  test("cp-gap-only comparison does not invent a comparative consequence") {
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
    val ctx = baseContext.copy(
      engineEvidence = Some(EngineEvidence(depth = 20, variations = List(best))),
      counterfactual = Some(
        CounterfactualMatch(
          userMove = "h4",
          bestMove = "g4",
          cpLoss = 220,
          missedMotifs = Nil,
          userMoveMotifs = Nil,
          severity = "Mistake",
          userLine = VariationLine(moves = List("h2h4", "a7a6"), scoreCp = 0)
        )
      )
    )

    val comparison =
      DecisionComparisonComparativeSupport
        .enrich(
          comparison = DecisionComparisonBuilder.build(ctx),
          ctx = ctx,
          strategyPack = None,
          truthContract = None
        )
        .getOrElse(fail("missing comparison"))

    assertEquals(comparison.comparedMove, None)
    assertEquals(comparison.comparativeConsequence, None)
    assertEquals(comparison.comparativeSource, None)
  }
