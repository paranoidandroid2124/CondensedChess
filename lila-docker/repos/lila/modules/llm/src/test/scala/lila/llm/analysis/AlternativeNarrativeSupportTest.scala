package lila.llm.analysis

import munit.FunSuite
import lila.llm.model.*
import lila.llm.model.strategic.{ EngineEvidence, PvMove, VariationLine }

class AlternativeNarrativeSupportTest extends FunSuite:

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

  test("returns no alternative narrative when no close candidate support survives") {
    val ctx = baseContext
    assertEquals(AlternativeNarrativeSupport.build(ctx), None)
  }

  test("falls back to a close engine alternative when whyAbsent is missing") {
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
              parsedMoves = List(PvMove("a1c3", "Rc3", "a1", "c3", "R", isCapture = false, capturedPiece = None, givesCheck = false))
            )
          )
        )
      )
    )

    val alt = AlternativeNarrativeSupport.build(ctx).getOrElse(fail("missing alternative support"))
    assertEquals(alt.move, Some("Rc3"))
    assert(alt.sentence.contains("Rc3"))
    assert(alt.sentence.contains("slows the direct attack"))
  }
