package lila.commentary.analysis

import lila.commentary.model.*
import munit.FunSuite

final class OpeningNameLookupTest extends FunSuite:

  private val initialFen =
    "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1"

  test("exact FEN hit returns canonical ECO and name") {
    val fen = NarrativeUtils.uciListToFen(initialFen, List("e2e4", "e7e5", "g1f3", "b8c6", "f1c4"))

    val ref = OpeningNameLookup.default.lookup(fen)

    assertEquals(ref.flatMap(_.eco), Some("C50"), clue(ref))
    assertEquals(ref.flatMap(_.name), Some("Italian Game"), clue(ref))
  }

  test("transposed move orders resolve to the same opening reference") {
    val italianMainOrder =
      NarrativeUtils.uciListToFen(initialFen, List("e2e4", "e7e5", "g1f3", "b8c6", "f1c4"))
    val italianTransposed =
      NarrativeUtils.uciListToFen(initialFen, List("g1f3", "b8c6", "e2e4", "e7e5", "f1c4"))

    val mainRef = OpeningNameLookup.default.lookup(italianMainOrder)
    val transposedRef = OpeningNameLookup.default.lookup(italianTransposed)

    assertEquals(mainRef.flatMap(_.eco), Some("C50"), clue(mainRef))
    assertEquals(transposedRef.flatMap(_.eco), mainRef.flatMap(_.eco), clue((mainRef, transposedRef)))
    assertEquals(transposedRef.flatMap(_.name), mainRef.flatMap(_.name), clue((mainRef, transposedRef)))
  }

  test("Queen's Gambit and Sicilian transpositions resolve by board identity") {
    val queensGambitMainOrder = NarrativeUtils.uciListToFen(initialFen, List("d2d4", "d7d5", "c2c4"))
    val queensGambitTransposed = NarrativeUtils.uciListToFen(initialFen, List("c2c4", "d7d5", "d2d4"))
    val sicilianMainOrder = NarrativeUtils.uciListToFen(initialFen, List("e2e4", "c7c5", "g1f3"))
    val sicilianTransposed = NarrativeUtils.uciListToFen(initialFen, List("g1f3", "c7c5", "e2e4"))

    assertEquals(OpeningNameLookup.default.lookup(queensGambitMainOrder).flatMap(_.name), Some("Queen's Gambit"), clue(queensGambitMainOrder))
    assertEquals(OpeningNameLookup.default.lookup(queensGambitTransposed).flatMap(_.name), Some("Queen's Gambit"), clue(queensGambitTransposed))
    assertEquals(OpeningNameLookup.default.lookup(sicilianMainOrder).flatMap(_.name), Some("Sicilian Defense"), clue(sicilianMainOrder))
    assertEquals(OpeningNameLookup.default.lookup(sicilianTransposed).flatMap(_.name), Some("Sicilian Defense"), clue(sicilianTransposed))
  }

  test("halfmove and fullmove counter differences do not affect lookup") {
    val normalCounters =
      "r1bqkbnr/pppp1ppp/2n5/4p3/2B1P3/5N2/PPPP1PPP/RNBQK2R b KQkq - 3 3"
    val differentCounters =
      "r1bqkbnr/pppp1ppp/2n5/4p3/2B1P3/5N2/PPPP1PPP/RNBQK2R b KQkq - 42 99"

    assertEquals(OpeningNameLookup.default.lookup(differentCounters).flatMap(_.name), Some("Italian Game"), clue(differentCounters))
    assertEquals(
      OpeningNameLookup.default.normalizedKey(differentCounters),
      OpeningNameLookup.default.normalizedKey(normalCounters),
      clue((normalCounters, differentCounters))
    )
  }

  test("non-capturable en-passant field is not part of the identity key") {
    val noEp =
      "rnbqkbnr/pppppppp/8/8/4P3/8/PPPP1PPP/RNBQKBNR b KQkq - 0 1"
    val rawNonCapturableEp =
      "rnbqkbnr/pppppppp/8/8/4P3/8/PPPP1PPP/RNBQKBNR b KQkq e3 0 1"

    assertEquals(
      OpeningNameLookup.default.normalizedKey(rawNonCapturableEp),
      OpeningNameLookup.default.normalizedKey(noEp),
      clue((rawNonCapturableEp, noEp))
    )
  }

  test("non-normalizable FEN fails closed") {
    assertEquals(OpeningNameLookup.default.lookup("not a fen"), None, clue("not a fen"))
    assertEquals(OpeningNameLookup.default.normalizedKey("8/8/8/8/8/8/8/8 w - - 0 1"), None, clue("kingless board"))
  }

  test("static name survives missing explorer response") {
    val fen = NarrativeUtils.uciListToFen(initialFen, List("e2e4", "c7c5", "g1f3"))

    val merged = OpeningExplorerClient.mergeStaticName(fen, None, OpeningNameLookup.default)

    assertEquals(merged.flatMap(_.eco), Some("B27"), clue(merged))
    assertEquals(merged.flatMap(_.name), Some("Sicilian Defense"), clue(merged))
    assertEquals(merged.map(_.totalGames), Some(0), clue(merged))
    assertEquals(merged.map(_.topMoves), Some(Nil), clue(merged))
  }

  test("explorer stats merge without replacing canonical static name") {
    val fen = NarrativeUtils.uciListToFen(initialFen, List("d2d4", "d7d5", "c2c4"))
    val explorerRef = OpeningReference(
      eco = Some("D00"),
      name = Some("Explorer sequence label"),
      totalGames = 12345,
      topMoves = List(ExplorerMove("e7e6", "e6", 6000, 2200, 1600, 2200, 2450)),
      sampleGames = List(
        ExplorerGame(
          id = "sample",
          winner = None,
          white = ExplorerPlayer("White", 2500),
          black = ExplorerPlayer("Black", 2500),
          year = 2020,
          month = 1
        )
      )
    )

    val merged = OpeningExplorerClient.mergeStaticName(fen, Some(explorerRef), OpeningNameLookup.default)

    assertEquals(merged.flatMap(_.eco), Some("D06"), clue(merged))
    assertEquals(merged.flatMap(_.name), Some("Queen's Gambit"), clue(merged))
    assertEquals(merged.map(_.totalGames), Some(12345), clue(merged))
    assertEquals(merged.map(_.topMoves.map(_.uci)), Some(List("e7e6")), clue(merged))
    assertEquals(merged.map(_.sampleGames.map(_.id)), Some(List("sample")), clue(merged))
  }
