package lila.commentary.analysis

import munit.FunSuite
import lila.commentary.model.*
import lila.commentary.model.strategic.{ EngineEvidence, VariationLine }

class AlternativeNarrativeSupportTest extends FunSuite:

  private def sourceStyleDataAndContext: (ExtendedAnalysisData, NarrativeContext) =
    val fen = "r1bq1rk1/pp1nbppp/4pn2/2pp2B1/2PP4/2NBPN2/PP3PPP/R2Q1RK1 b - - 1 8"
    val data =
      CommentaryEngine
        .assessExtended(
          fen = fen,
          variations = List(
            VariationLine(
              List(
                "c5d4",
                "e3d4",
                "d5c4",
                "d3c4",
                "h7h6",
                "g5h4",
                "d7b6",
                "c4b3",
                "c8d7",
                "f3e5",
                "d7c6",
                "f1e1",
                "b6d5",
                "d1d3",
                "d5f4",
                "d3e3",
                "f4d5"
              ),
              scoreCp = -120,
              depth = 16
            ),
            VariationLine(
              List("h7h6", "g5f6", "d7f6", "c4d5", "f6d5", "d3c2", "d5b4", "c2e4", "c5d4", "e3d4", "b4d5"),
              scoreCp = -17,
              depth = 16
            )
          ),
          playedMove = Some("c5d4"),
          phase = Some("opening"),
          ply = 16,
          prevMove = Some("c5d4")
        )
        .getOrElse(fail("analysis missing"))
    data -> NarrativeContextBuilder.build(data, data.toContext, None)

  private def baseContext: NarrativeContext =
    NarrativeContext(
      fen = "4k3/p7/8/8/8/8/7P/2R1K3 w - - 0 1",
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
      renderMode = NarrativeRenderMode.MoveReview
    )

  test("returns no alternative narrative when no close candidate support survives") {
    val ctx = baseContext
    assertEquals(AlternativeNarrativeSupport.build(ctx), None)
  }

  test("generates rich comparative sentence without treating close candidates as blocked inferior moves") {
    val ctx = baseContext.copy(
      candidates = List(
        CandidateInfo("h4", annotation = "!", planAlignment = "Kingside expansion", tacticalAlert = None, practicalDifficulty = "clean", whyNot = None),
        CandidateInfo("Rc3", annotation = "", planAlignment = "Rook lift", tacticalAlert = None, practicalDifficulty = "clean", whyNot = Some("it slows the direct attack"))
      ),
      engineEvidence = Some(
        EngineEvidence(
          depth = 20,
          variations = List(
            VariationLine(
              moves = List("h2h4", "a7a6"),
              scoreCp = 44
            ),
            VariationLine(
              moves = List("c1c3", "a7a6"),
              scoreCp = 30
            )
          )
        )
      )
    )

    val alt = AlternativeNarrativeSupport.build(ctx).getOrElse(fail("missing alternative support"))
    assertEquals(alt.move, Some("Rc3"))
    // FEN move count 1 + w -> startPly = 1 -> "1. h4 a6" vs "1. Rc3 a6"
    assertEquals(
      alt.sentence,
      "Both candidate branches are viable: the played h4 (kingside expansion) follows 1. h4 a6, whereas Rc3 (rook lift) follows 1. Rc3 a6."
    )
    assert(!alt.sentence.contains(" is best"), clues(alt.sentence))
    assert(!alt.sentence.contains("stays secondary"), clues(alt.sentence))
  }

  test("returns no alternative narrative when the alternative branch lacks replayed evidence") {
    val ctx = baseContext.copy(
      candidates = List(
        CandidateInfo("h4", annotation = "!", planAlignment = "Kingside expansion", tacticalAlert = None, practicalDifficulty = "clean", whyNot = None),
        CandidateInfo("Rc3", annotation = "", planAlignment = "Rook lift", tacticalAlert = None, practicalDifficulty = "clean", whyNot = Some("it slows the direct attack"))
      ),
      engineEvidence = Some(
        EngineEvidence(
          depth = 20,
          variations = List(
            VariationLine(
              moves = List("h2h4", "a7a6"),
              scoreCp = 44
            )
          )
        )
      )
    )

    val alt = AlternativeNarrativeSupport.build(ctx)
    assertEquals(alt, None)
  }

  test("builds source-style close alternative from UCI variations without parsed moves") {
    val (_, ctx) = sourceStyleDataAndContext

    val alt = AlternativeNarrativeSupport.build(ctx).getOrElse(fail("missing alternative support"))
    assertEquals(alt.move, Some("h6"))
    assert(alt.sentence.contains("cxd4"), clues(alt.sentence))
    assert(alt.sentence.contains("8... h6"), clues(alt.sentence))
    assert(!alt.sentence.contains(" is best"), clues(alt.sentence))
  }

  test("source-style close alternative reaches move-review prose") {
    val (data, ctx) = sourceStyleDataAndContext
    val pack = StrategyPackBuilder.build(data, ctx).getOrElse(fail("strategy pack missing"))
    val inputs = QuestionPlannerInputsBuilder.build(ctx, Some(pack), truthContract = None)
    val ranked = QuestionFirstCommentaryPlanner.plan(ctx, inputs, truthContract = None)
    val slots =
      MoveReviewCompressionPolicy.buildSlotsOrFallbackFromPlannerRuntime(
        ctx = ctx,
        inputs = inputs,
        rankedPlans = ranked,
        strategyPack = Some(pack),
        truthContract = None
      )
    val prose = LiveNarrativeCompressionCore.deterministicProse(slots)

    assert(prose.contains("8... h6"), clues(prose, slots, inputs.alternativeNarrative, ranked.primary))
  }
