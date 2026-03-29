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

  test("build falls back to engine-gap deferred branch when legacy whyAbsent text is present") {
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
      whyAbsentFromTopMultiPV = List("""the immediate "g4" push loses 220 cp"""),
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
