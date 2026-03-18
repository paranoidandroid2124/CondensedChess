package lila.llm

import munit.FunSuite

class StrategicPuzzlePromptTest extends FunSuite:

  test("buildTerminalPrompt stays compact and line-focused") {
    val prompt = StrategicPuzzlePrompt.buildTerminalPrompt(
      StrategicPuzzlePrompt.TerminalInput(
        startFen = "3r2k1/1R4bp/1p1p1nn1/p2Pp3/b1P1P3/1BP2pp1/P1N1BKPP/5R2 w - - 0 30",
        sideToMove = "white",
        outcome = "full",
        dominantFamily = Some("space_gain_or_restriction|c6"),
        lineSan = List("Nb4", "...", "Na3", "...", "Nc2"),
        siblingMoves = List("Na1", "Na3"),
        opening = Some("Bishop's Opening: Philidor Counterattack"),
        eco = Some("C23"),
        draftCommentary =
          "Nb4 begins a route that keeps White's queenside grip intact while limiting Black's breaks. The point is not speed alone, but preserving the clamp so the follow-up knight hop can reinforce c6 and keep counterplay from opening too early."
      )
    )

    assert(prompt.contains("## PUZZLE CONTEXT"))
    assert(prompt.contains("Outcome: full"))
    assert(prompt.contains("Reached line: Nb4 ... Na3 ... Nc2"))
    assert(prompt.contains("Sibling continuations: Na1, Na3"))
    assert(prompt.contains("Refine the draft into reveal text"))
    assert(!prompt.contains("Stockfish"))
    assert(prompt.length < 1600)
  }

  test("buildSummaryPrompt keeps accepted starts and common plan visible") {
    val prompt = StrategicPuzzlePrompt.buildSummaryPrompt(
      StrategicPuzzlePrompt.SummaryInput(
        startFen = "3r2k1/1R4bp/1p1p1nn1/p2Pp3/b1P1P3/1BP2pp1/P1N1BKPP/5R2 w - - 0 30",
        sideToMove = "white",
        dominantFamily = Some("counterplay_suppression|e4"),
        mainLine = List("Nb4", "...", "Na3", "...", "Nc2"),
        acceptedStarts = List("Nb4", "Na3", "Na1"),
        opening = Some("Bishop's Opening: Philidor Counterattack"),
        eco = Some("C23"),
        draftSummary = "White's best routes all revolve around keeping the queenside under control before Black's breaks arrive.",
        draftCommentary = Some("The main line reaches the same clamp with the least drift.")
      )
    )

    assert(prompt.contains("Featured line: Nb4 ... Na3 ... Nc2"))
    assert(prompt.contains("Accepted starts: Nb4, Na3, Na1"))
    assert(prompt.contains("Write a short reveal summary"))
    assert(!prompt.contains("best move"))
    assert(prompt.length < 1500)
  }
