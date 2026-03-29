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

  test("gameIntro drops placeholder broadcast metadata") {
    val intro = NarrativeLexicon.gameIntro(
      white = "WhitePlayer",
      black = "BlackPlayer",
      event = "Weekend Swiss",
      date = "????.??.??",
      result = "1-0",
      totalPlies = 47,
      keyMomentsCount = 1
    )

    assertEquals(
      intro,
      "This review covers WhitePlayer vs BlackPlayer (Weekend Swiss). WhitePlayer won after 24 moves. The review focuses on one highlighted moment."
    )
    assert(!intro.contains("????.??.??"), clue(intro))
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

  test("gameConclusion prefers concrete whole-game binder sentences over count-only practical tail") {
    val conclusion =
      NarrativeLexicon.gameConclusion(
        winner = Some("WhitePlayer"),
        themes = List("Queenside pressure", "Improving piece placement"),
        blunders = 2,
        missedWins = 1,
        mainContest = Some("White was mainly playing for pressure on b2, while Black was mainly playing for control of the d-file."),
        decisiveShift = Some("The decisive shift came through pressure on b2."),
        payoff = Some("The punishment story ran through pressure on b2.")
      )

    assert(conclusion.contains("White was mainly playing for pressure on b2"), clue(conclusion))
    assert(conclusion.contains("The decisive shift came through pressure on b2."), clue(conclusion))
    assert(conclusion.contains("The punishment story ran through pressure on b2."), clue(conclusion))
    assert(!conclusion.contains("around Queenside pressure"), clue(conclusion))
    assert(!conclusion.contains("2 blunders"), clue(conclusion))
    assert(!conclusion.contains("1 missed win"), clue(conclusion))
  }

  test("gameConclusion keeps balanced games compact when the binder only has a contest sentence") {
    val conclusion =
      NarrativeLexicon.gameConclusion(
        winner = None,
        themes = List("Queenside pressure"),
        blunders = 0,
        missedWins = 0,
        mainContest = Some("White was mainly playing for queenside targets, while Black was mainly playing for central files."),
        decisiveShift = None,
        payoff = None
      )

    assert(conclusion.startsWith("The game stayed balanced and neither side forced a decisive breakthrough."), clue(conclusion))
    assert(conclusion.contains("White was mainly playing for queenside targets"), clue(conclusion))
    assert(!conclusion.contains("around Queenside pressure"), clue(conclusion))
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

  test("generic negative annotations without a verified benchmark stay vague and truthful") {
    val text = NarrativeLexicon.getAnnotationNegativeWithoutBenchmark(
      bead = 0,
      playedSan = "Qc6",
      cpLoss = 180
    ).toLowerCase

    assert(!text.contains("better is"), clue(text))
    assert(!text.contains("engine reference"), clue(text))
    assert(!text.contains("benchmark"), clue(text))
    assert(!text.contains("qd3"), clue(text))
    assert(
      text.contains("loses control") ||
        text.contains("concedes") ||
        text.contains("lets the position drift") ||
        text.contains("hands over the initiative") ||
        text.contains("inaccurate in practical terms"),
      clue(text)
    )
  }
