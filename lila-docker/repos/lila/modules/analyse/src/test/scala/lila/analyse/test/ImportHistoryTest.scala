package lila.analyse.test

import lila.analyse.ImportHistory

class ImportHistoryTest extends munit.FunSuite:

  test("derive metadata from PGN tags when available") {
    val pgn =
      """[Event "World Championship"]
        |[Site "Test"]
        |[Date "2024.01.05"]
        |[UTCDate "2024.01.05"]
        |[UTCTime "12:30:00"]
        |[White "Carlsen"]
        |[Black "Nepo"]
        |[Result "1-0"]
        |[Opening "Sicilian Defense"]
        |
        |1. e4 c5 2. Nf3 d6 3. d4 cxd4 1-0
        |""".stripMargin

    val meta = ImportHistory.deriveMetadata(pgn)

    assertEquals(meta.white, Some("Carlsen"))
    assertEquals(meta.black, Some("Nepo"))
    assertEquals(meta.result, Some("1-0"))
    assertEquals(meta.playedAtLabel, Some("2024.01.05 12:30:00"))
    assertEquals(meta.event, Some("World Championship"))
    assertEquals(meta.opening, Some("Sicilian Defense"))
    assertEquals(meta.title, "Carlsen vs Nepo")
  }

  test("normalize source fields and reject unsupported provider usernames") {
    val source = ImportHistory
      .AnalysisSource(
        sourceType = "manual",
        provider = Some("ChessCom"),
        username = Some("Hikaru"),
        externalGameId = Some(" 1234567890 "),
        sourceUrl = Some(" https://www.chess.com/game/live/1234567890 "),
        white = Some(" Hikaru "),
        black = Some(" Firouzja "),
        result = Some("1-0"),
        speed = Some(" blitz "),
        playedAtLabel = Some(" 2026-03-10 12:00 ")
      )
      .normalized

    assertEquals(source.sourceType, ImportHistory.sourceAccount)
    assertEquals(source.provider, Some("chesscom"))
    assertEquals(source.username, Some("Hikaru"))
    assertEquals(source.externalGameId, Some("1234567890"))
    assertEquals(source.white, Some("Hikaru"))
    assertEquals(source.black, Some("Firouzja"))
    assertEquals(source.speed, Some("blitz"))

    val invalid = ImportHistory.AnalysisSource(provider = Some("unknown"), username = Some("bad user")).normalized
    assertEquals(invalid.sourceType, ImportHistory.sourceManual)
    assertEquals(invalid.provider, None)
    assertEquals(invalid.username, None)
  }
