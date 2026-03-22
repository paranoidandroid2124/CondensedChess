package lila.llm.analysis

import munit.FunSuite

class NarrativeLexiconTest extends FunSuite:

  test("gameIntro avoids unknown placeholders, markdown markers, and ply wording for short unfinished lines") {
    val intro = NarrativeLexicon.gameIntro(
      white = "Unknown",
      black = "Unknown",
      event = "Unknown",
      date = "Unknown",
      result = "*",
      totalPlies = 11,
      keyMomentsCount = 2
    )

    assertNoDiff(
      intro,
      "This review covers this game. The line is still in the opening after 6 moves. The review focuses on 2 highlighted moments."
    )
    assert(!intro.contains("**"))
    assert(!intro.toLowerCase.contains("plies"))
    assert(!intro.toLowerCase.contains("battle"))
  }

  test("gameIntro keeps known player names and uses move wording for finished games") {
    val intro = NarrativeLexicon.gameIntro(
      white = "WhitePlayer",
      black = "BlackPlayer",
      event = "Club Championship",
      date = "2026.03.19",
      result = "1-0",
      totalPlies = 47,
      keyMomentsCount = 1
    )

    assertEquals(
      intro,
      "This review covers WhitePlayer vs BlackPlayer (Club Championship, 2026.03.19). WhitePlayer won after 24 moves. The review focuses on one highlighted moment."
    )
  }

  test("gameConclusion uses the lead theme instead of generic critical-moments boilerplate") {
    val conclusion =
      NarrativeLexicon.gameConclusion(
        winner = Some("WhitePlayer"),
        themes = List("Attacking a fixed pawn", "Improving piece placement", "Development and central control"),
        blunders = 0,
        missedWins = 0
      )

    assertEquals(
      conclusion,
      "**WhitePlayer** kept the game on the right strategic track through Attacking a fixed pawn. Other recurring themes were Improving piece placement, Development and central control."
    )
    assert(!conclusion.toLowerCase.contains("critical moments"), clue(conclusion))
  }

  test("gameConclusion surfaces practical swings when blunders and missed wins exist") {
    val conclusion =
      NarrativeLexicon.gameConclusion(
        winner = Some("BlackPlayer"),
        themes = List("Queenside pressure"),
        blunders = 2,
        missedWins = 1
      )

    assert(conclusion.contains("BlackPlayer"), clue(conclusion))
    assert(conclusion.contains("Queenside pressure"), clue(conclusion))
    assert(conclusion.contains("2 blunders"), clue(conclusion))
    assert(conclusion.contains("1 missed win"), clue(conclusion))
  }

  test("generic negative annotations avoid forced-only wording without explicit evidence") {
    val text = NarrativeLexicon.getAnnotationNegative(
      bead = 0,
      playedSan = "Qe2",
      bestSan = "Qd3",
      cpLoss = 320
    ).toLowerCase

    assert(!text.contains("was required"), clue(text))
    assert(!text.contains("only stable route"), clue(text))
    assert(!text.contains("was necessary"), clue(text))
    assert(!text.contains("forcing sequence"), clue(text))
    assert(text.contains("cleanest defense") || text.contains("resilient route") || text.contains("hold things together"), clue(text))
  }
