package lila.llm.analysis

import lila.llm.model.*
import munit.FunSuite

class NarrativeStructureToneTest extends FunSuite:

  private val fen = "4k3/8/2p5/3p4/3P4/8/8/4K3 w - - 0 1"

  private def baseContext(alignmentBand: String, guidance: String): NarrativeContext =
    NarrativeContext(
      fen = fen,
      header = ContextHeader("Middlegame", "Normal", "StyleChoice", "Low", "ExplainPlan"),
      ply = 20,
      playedMove = Some("f1b5"),
      playedSan = Some("Bb5"),
      summary = NarrativeSummary("Piece activation (0.80)", None, "StyleChoice", "Maintain", "+0.1"),
      threats = ThreatTable(Nil, Nil),
      pawnPlay = PawnPlayTable(false, None, "Low", "Maintain", "test", "Background", None, false, "quiet"),
      plans = PlanTable(List(PlanRow(1, "Piece activation", 0.8, List("Activity"))), Nil),
      snapshots = Nil,
      delta = None,
      phase = PhaseContext("Middlegame", "Test", None),
      candidates = List(
        CandidateInfo(
          move = "Bb5",
          uci = Some("f1b5"),
          annotation = "!",
          planAlignment = "Piece activation",
          structureGuidance = Some(guidance),
          alignmentBand = Some(alignmentBand),
          downstreamTactic = None,
          tacticalAlert = None,
          practicalDifficulty = "clean",
          whyNot = None
        )
      ),
      probeRequests = Nil,
      facts = Nil
    )

  test("OnBook tone avoids over-warning phrases") {
    val prose = BookStyleRenderer.render(baseContext("OnBook", "keep pieces coordinated behind the pawn shell"))
    assert(!prose.toLowerCase.contains("needs verification"), clues(prose))
    assert(!prose.toLowerCase.contains("route is playable only with precise follow-up"), clues(prose))
  }

  test("OffPlan tone adds caution wording") {
    val prose = BookStyleRenderer.render(baseContext("OffPlan", "reassess pawn commitments before forcing play"))
    assert(prose.toLowerCase.contains("playable only with precise follow-up"), clues(prose))
  }

  test("Unknown tone asks for verification") {
    val prose = BookStyleRenderer.render(baseContext("Unknown", "verify tactical details first"))
    assert(prose.toLowerCase.contains("verify tactical details"), clues(prose))
  }
