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
    assert(prompt.indexOf("## CONTEXT") < prompt.indexOf("## DRAFT COMMENTARY"))
    assert(!prompt.contains("## BOOKMAKER PROSE CONTRACT"))
  }

  test("system prompt preserves the dominant strategic claim and causal chain") {
    assert(PolishPrompt.systemPrompt.contains("dominant strategic claim"))
    assert(PolishPrompt.systemPrompt.contains("cause -> effect chain"))
    assert(PolishPrompt.estimatedSystemTokens <= 1500)
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
    assert(prompt.indexOf("## CONTEXT") < prompt.indexOf("## ORIGINAL_DRAFT"))
    assert(!prompt.contains("## BOOKMAKER PROSE CONTRACT"))
  }

  test("buildSegmentRepairPrompt keeps lock-anchor instructions narrow") {
    val prompt = PolishPrompt.buildSegmentRepairPrompt(
      originalSegment = "White improves [[LK_001]] before [[LK_002]].",
      rejectedPolish = "White improves the pieces quickly.",
      phase = "middlegame",
      evalDelta = Some(12),
      concepts = List("coordination"),
      fen = "2r2rk1/pp3ppp/2n1pn2/2pp4/3P4/2P1PN2/PP1NBPPP/R1BQ1RK1 w - - 0 10",
      openingName = Some("Catalan")
    )

    assert(prompt.contains("Repair REJECTED_POLISH into a strict-valid commentary segment."))
    assert(prompt.contains("Lock anchors like [[LK_001]] must be preserved exactly"))
    assert(prompt.contains("## ORIGINAL_SEGMENT"))
    assert(prompt.contains("## REJECTED_POLISH"))
  }

  test("buildPolishPrompt omits empty optional context fields") {
    val prompt = PolishPrompt.buildPolishPrompt(
      prose = "White keeps the position under control.",
      phase = "opening",
      evalDelta = None,
      concepts = Nil,
      fen = "",
      openingName = None,
      nature = None,
      tension = None,
      salience = None,
      momentType = Some("Game Intro")
    )

    assert(prompt.contains("Phase: opening | Eval Δ: N/A"))
    assert(prompt.contains("Context Mode: Key Moment (Game Intro) - Part of Full Game Review"))
    assert(!prompt.contains("Opening:"))
    assert(!prompt.contains("Concepts:"))
    assert(!prompt.contains("FEN:"))
    assert(!prompt.contains("Salience:"))
    assert(!prompt.contains("Nature:"))
    assert(!prompt.contains("Tension:"))
  }
