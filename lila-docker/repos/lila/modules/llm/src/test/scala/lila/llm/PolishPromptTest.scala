package lila.llm

import munit.FunSuite

class PolishPromptTest extends FunSuite:

  test("buildPolishPrompt uses a short structure reminder") {
    val prompt = PolishPrompt.buildPolishPrompt(
      prose = "White improves the knight and keeps pressure on e5.\n\nThe move also prepares f4.",
      phase = "middlegame",
      evalDelta = Some(18),
      concepts = List("space", "piece activity"),
      fen = "r2q1rk1/pp2bppp/2n1pn2/2pp4/3P4/2P1PN2/PPBNBPPP/R2Q1RK1 w - - 0 11",
      openingName = Some("Queen's Gambit Declined"),
      momentType = None
    )

    assert(prompt.contains("Structure reminder: return plain prose only"))
    assert(prompt.contains("do not emit UI section titles"))
    assert(prompt.contains("2-4 short paragraphs"))
    assert(prompt.contains("Context Mode: Isolated Move"))
    assert(!prompt.contains("## BOOKMAKER PROSE CONTRACT"))
  }

  test("buildRepairPrompt keeps a short repair reminder") {
    val prompt = PolishPrompt.buildRepairPrompt(
      originalProse = "White centralizes the rook.\n\nThat keeps the pressure alive.",
      rejectedPolish = "Strategic Signals: White centralizes the rook.",
      phase = "middlegame",
      evalDelta = Some(-35),
      concepts = List("rook activity"),
      fen = "2r2rk1/pp3ppp/2n1pn2/2pp4/3P4/2P1PN2/PP1NBPPP/R1BQ1RK1 w - - 0 10",
      openingName = Some("Catalan"),
      allowedSans = List("Rc1", "Rfd1")
    )

    assert(prompt.contains("Structure reminder: return plain prose only"))
    assert(prompt.contains("do not emit UI section titles"))
    assert(prompt.contains("Repair REJECTED_POLISH into a strict-valid final commentary."))
    assert(prompt.contains("2-4 short paragraphs"))
    assert(!prompt.contains("## BOOKMAKER PROSE CONTRACT"))
  }
