package lila.llm.analysis

import munit.FunSuite
import lila.llm.model.*

class NarrativeStructureLeakTest extends FunSuite:

  private val fen = "4k3/8/2p5/3p4/3P4/8/8/4K3 w - - 0 1"

  test("render output should not leak internal structure ids or reason codes") {
    val ctx = NarrativeContext(
      fen = fen,
      header = ContextHeader("Middlegame", "Normal", "StyleChoice", "Low", "ExplainPlan"),
      ply = 20,
      summary = NarrativeSummary("Carlsbad (0.90)", None, "StyleChoice", "Maintain", "+0.2"),
      threats = ThreatTable(Nil, Nil),
      pawnPlay = PawnPlayTable(false, None, "Low", "Maintain", "test", "Background", None, false, "quiet"),
      plans = PlanTable(List(PlanRow(1, "Carlsbad", 0.9, List("PA_MATCH", "PRECOND_MISS"))), Nil),
      snapshots = Nil,
      delta = None,
      phase = PhaseContext("Middlegame", "Test", None),
      candidates = List(
        CandidateInfo(
          move = "Bb5",
          uci = Some("f1b5"),
          annotation = "!",
          planAlignment = "Carlsbad",
          structureGuidance = Some("REQ_D_PAWN_TENSION SUP_BLACK_E_SUPPORT"),
          alignmentBand = Some("OffPlan"),
          downstreamTactic = None,
          tacticalAlert = None,
          practicalDifficulty = "clean",
          whyNot = Some("BLK_CONFLICT and PRECOND_MISS")
        )
      ),
      probeRequests = Nil,
      facts = Nil
    )

    val prose = BookStyleRenderer.render(ctx)
    assert(!prose.contains("Carlsbad"), clues(prose))
    assert(!prose.contains("PA_MATCH"), clues(prose))
    assert(!prose.contains("PRECOND_MISS"), clues(prose))
    assert(!prose.contains("REQ_D_PAWN_TENSION"), clues(prose))
    assert(!prose.contains("SUP_BLACK_E_SUPPORT"), clues(prose))
    assert(!prose.contains("BLK_CONFLICT"), clues(prose))
  }
