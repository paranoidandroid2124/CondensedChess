package lila.commentary.analysis

import java.nio.file.{ Files, Paths }

import lila.commentary.analysis.claim.OpeningFamilyClaimResolver.OpeningFamilyId
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

  test("opening mismatch prose comes from structured family identity") {
    val text =
      NarrativeLexicon.getGoalStatusDescription(
        bead = 0,
        evaluation =
          OpeningGoals.Evaluation(
            goalName = "Sicilian Liberator",
            status = OpeningGoals.Status.Mismatch,
            supportedEvidence = Nil,
            missingEvidence = Nil,
            confidence = 0.8,
            requiredFamily = Some(OpeningFamilyId.Sicilian)
          )
      )

    assert(text.contains("Sicilian structure"), clue(text))
    assert(!text.contains("c5 pawn or traded c-pawn"), clue(text))
  }

  test("motif prefix selection is table-driven rather than an else-if chain") {
    val source =
      Files.readString(Paths.get("modules/commentaryCore/src/main/scala/lila/commentary/analysis/NarrativeLexicon.scala"))
    val methodBody = source.substring(source.indexOf("def getMotifPrefix"), source.indexOf("def getThreatStatement"))
    val tableSource =
      Files.readString(Paths.get("modules/commentaryCore/src/main/scala/lila/commentary/analysis/NarrativeMotifPrefixTable.scala"))

    assert(methodBody.contains("NarrativeMotifPrefixTable.templatesFor"), clue(methodBody))
    assert(!methodBody.contains("MotifPrefixRule"), clue(methodBody))
    assert(!methodBody.contains("else if (hasAny"), clue(methodBody))
    assert(tableSource.contains("MotifPrefixRule"), clue(tableSource))
  }

  test("legacy relation motifs degrade through boundary-safe motif prefixes") {
    val zwischenzug =
      NarrativeLexicon.getMotifPrefix(bead = 0, motifs = List("zwischenzug"), ply = 24).getOrElse("")
    val trapped =
      NarrativeLexicon.getMotifPrefix(bead = 0, motifs = List("trapped_piece_queen"), ply = 24).getOrElse("")
    val domination =
      NarrativeLexicon.getMotifPrefix(bead = 0, motifs = List("domination"), ply = 24).getOrElse("")

    assert(zwischenzug.nonEmpty, clue(zwischenzug))
    assert(!zwischenzug.toLowerCase.contains("zwischenzug"), clue(zwischenzug))
    assert(
      zwischenzug.toLowerCase.contains("move-order") || zwischenzug.toLowerCase.contains("calculation"),
      clue(zwischenzug)
    )
    assert(trapped.nonEmpty, clue(trapped))
    assert(!trapped.toLowerCase.contains("trapped"), clue(trapped))
    assert(trapped.toLowerCase.contains("mobility") || trapped.toLowerCase.contains("safe route"), clue(trapped))
    assert(domination.nonEmpty, clue(domination))
    assert(!domination.toLowerCase.contains("domination"), clue(domination))
    assert(domination.toLowerCase.contains("restriction") || domination.toLowerCase.contains("limiting"), clue(domination))
  }

  test("legacy relation motifs are not generic motif-prefix signals") {
    assert(!NarrativeLexicon.isMotifPrefixSignal("zwischenzug"))
    assert(!NarrativeLexicon.isMotifPrefixSignal("trapped_piece_queen"))
    assert(!NarrativeLexicon.isMotifPrefixSignal("domination"))
    assert(!NarrativeLexicon.isMotifPrefixSignal("stalemate_trap"))
    assert(!NarrativeLexicon.isMotifPrefixSignal("perpetual_check"))
    assert(NarrativeLexicon.isMotifPrefixSignal("stalemate"))
    assert(NarrativeLexicon.isMotifPrefixSignal("knight_domination"))
  }

  test("legacy diagnostic-only relation motifs do not emit motif prefixes") {
    assertEquals(NarrativeLexicon.getMotifPrefix(bead = 0, motifs = List("stalemate_trap"), ply = 52), None)
    assertEquals(NarrativeLexicon.getMotifPrefix(bead = 0, motifs = List("perpetual_check"), ply = 52), None)
    assert(NarrativeLexicon.getMotifPrefix(bead = 0, motifs = List("stalemate"), ply = 52).nonEmpty)
  }

  test("outline canonical motif terms keep relation-shaped legacy labels bounded") {
    val source =
      Files.readString(Paths.get("modules/commentaryCore/src/main/scala/lila/commentary/analysis/NarrativeOutlineBuilder.scala"))
    val body =
      source.substring(source.indexOf("private def canonicalTermForMotif"), source.indexOf("private def buildImbalanceContrast"))

    assert(body.contains("deferredRelationCanonicalTerm"), clue(body))
    assert(body.contains("deferredFallbackForMotifTag"), clue(body))
    assert(!body.contains("deferredFallbackLabelForMotifTag"), clue(body))
  }

  test("outline theme keywords keep relation-shaped legacy labels bounded") {
    val source =
      Files.readString(Paths.get("modules/commentaryCore/src/main/scala/lila/commentary/analysis/NarrativeOutlineBuilder.scala"))
    val body =
      source.substring(source.indexOf("private def buildThemeKeywordSentence"), source.indexOf("private def buildCanonicalMotifTermSentence"))

    assert(body.contains(".flatMap(themeKeywordForMotif)"), clue(body))
    assert(body.contains("private def themeKeywordForMotif"), clue(body))
    assert(body.contains("deferredRelationCanonicalTerm(motif)"), clue(body))
    assert(body.contains("case Some(term) => term"), clue(body))
  }

  test("outline tactical tension does not promote legacy relation motif tags") {
    val source =
      Files.readString(Paths.get("modules/commentaryCore/src/main/scala/lila/commentary/analysis/NarrativeOutlineBuilder.scala"))
    val tensionBody =
      source.substring(source.indexOf("val highTensionByMotif"), source.indexOf("val highTensionByThreat"))

    assert(tensionBody.contains("motifSignals.exists(tacticalTensionMotif)"), clue(tensionBody))
    assert(!tensionBody.contains("zwischenzug"), clue(tensionBody))
    assert(source.contains("private def tacticalTensionMotif"), clue(source))
    assert(source.contains("RelationObservationCatalog.deferredFallbackForMotifTag(motif).isEmpty"), clue(source))
  }

  test("motif delta prose degrades legacy relation labels through the boundary") {
    val appears = NarrativeLexicon.getMotifAppearsStatement(bead = 0, motif = "zwischenzug")
    val fades = NarrativeLexicon.getMotifFadesStatement(bead = 0, motif = "trapped_piece_queen")

    assert(appears.nonEmpty, clue(appears))
    assert(!appears.toLowerCase.contains("zwischenzug"), clue(appears))
    assert(appears.toLowerCase.contains("move-order caution"), clue(appears))
    assert(fades.nonEmpty, clue(fades))
    assert(!fades.toLowerCase.contains("trapped"), clue(fades))
    assert(fades.toLowerCase.contains("piece mobility"), clue(fades))
    assertEquals(NarrativeLexicon.getMotifAppearsStatement(bead = 0, motif = "stalemate_trap"), "")
    assertEquals(NarrativeLexicon.getMotifFadesStatement(bead = 0, motif = "perpetual_check"), "")
    assert(NarrativeLexicon.getMotifAppearsStatement(bead = 0, motif = "fork").toLowerCase.contains("fork"))
  }
