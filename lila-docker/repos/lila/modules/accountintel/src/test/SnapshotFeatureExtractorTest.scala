package lila.accountintel

import lila.accountintel.AccountIntel.*
import lila.accountintel.primitive.SnapshotFeatureExtractor

class SnapshotFeatureExtractorTest extends munit.FunSuite:

  private val collapsePgn =
    """[Event "Fixture"]
      |[Site "https://lichess.org/VwZJhurC"]
      |[Date "2026.03.13"]
      |[White "ych24"]
      |[Black "lichess AI level 1"]
      |[Result "0-1"]
      |[Variant "Standard"]
      |[Opening "King's Pawn Game"]
      |
      |1. e4 e5 2. d4 Bb4+ 3. c3 Ba5 4. Nd2 Nc6 0-1
      |""".stripMargin

  private val moveEvals = List(
    MoveEval(1, 207, None, variations = List(VariationLine(List("e2e4"), 207))),
    MoveEval(2, 636, None, variations = List(VariationLine(List("c7c5"), 636))),
    MoveEval(3, -9995, Some(-5), variations = List(VariationLine(List("d1h5"), 0, Some(-5)))),
    MoveEval(4, -9994, Some(-4), variations = List(VariationLine(List("b4c3"), 0, Some(-4)))),
    MoveEval(5, -9993, Some(-3), variations = List(VariationLine(List("c2c3"), 0, Some(-3)))),
    MoveEval(6, 113, None, variations = List(VariationLine(List("b4c3"), 113))),
    MoveEval(7, -515, None, variations = List(VariationLine(List("d1h5"), -515))),
    MoveEval(8, -97, None, variations = List(VariationLine(List("d8h4"), -97)))
  )

  test("extractor attaches local analysis primitives and keeps eval-backed snapshot rows when evals exist"):
    val result =
      SnapshotFeatureExtractor.extract(
        "ych24",
        List(
          ExternalGame(
            provider = "lichess",
            gameId = "g-collapse",
            playedAt = "2026-03-17 00:00",
            white = "ych24",
            black = "lichess AI level 1",
            result = "0-1",
            sourceUrl = Some("https://lichess.org/VwZJhurC"),
            pgn = collapsePgn,
            moveEvals = moveEvals
          )
        )
      )

    assert(result.isRight)
    val bundle = result.toOption.get
    assert(bundle.featureRows.nonEmpty)
    assert(bundle.featureRows.forall(_.analysis.fen.nonEmpty))
    assert(
      bundle.featureRows.exists(row =>
        row.collapseAnalysis.isDefined || row.collapseMomentPly.isDefined || row.triggerHints.nonEmpty
      )
    )

  test("extractor falls back to ECOUrl family buckets and strips variation suffixes"):
    val pgn =
      """[Event "Fixture"]
        |[Site "https://www.chess.com/game/live/1"]
        |[Date "2026.03.17"]
        |[White "ych24"]
        |[Black "opp"]
        |[Result "1-0"]
        |[Variant "Standard"]
        |[ECO "E97"]
        |[ECOUrl "https://www.chess.com/openings/Kings-Indian-Defense-Normal-Variation"]
        |
        |1. d4 Nf6 2. c4 g6 3. Nc3 Bg7 4. e4 d6 5. Nf3 O-O 1-0
        |""".stripMargin

    val result =
      SnapshotFeatureExtractor.extract(
        "ych24",
        List(
          ExternalGame(
            provider = "chesscom",
            gameId = "g-eco-url",
            playedAt = "2026-03-17 00:00",
            white = "ych24",
            black = "opp",
            result = "1-0",
            sourceUrl = Some("https://www.chess.com/game/live/1"),
            pgn = pgn
          )
        )
      )

    assert(result.isRight)
    val parsed = result.toOption.get.parsedGames.head
    assertEquals(parsed.openingFamily, "King's Indian Defense")

  test("extractor does not mark favorable subject-perspective eval swings as collapse"):
    val pgn =
      """[Event "Fixture"]
        |[Site "https://lichess.org/black-subject"]
        |[Date "2026.03.17"]
        |[White "opp"]
        |[Black "ych24"]
        |[Result "0-1"]
        |[Variant "Standard"]
        |[Opening "Sicilian Defense"]
        |
        |1. e4 c5 2. Nf3 d6 3. d4 cxd4 0-1
        |""".stripMargin
    val favorableForBlack = List(
      MoveEval(1, 300, None, variations = List(VariationLine(List("e2e4"), 300))),
      MoveEval(2, -350, None, variations = List(VariationLine(List("c7c5"), -350))),
      MoveEval(3, -420, None, variations = List(VariationLine(List("g1f3"), -420))),
      MoveEval(4, -620, None, variations = List(VariationLine(List("d7d6"), -620)))
    )

    val result =
      SnapshotFeatureExtractor.extract(
        "ych24",
        List(
          ExternalGame(
            provider = "lichess",
            gameId = "g-black-favorable",
            playedAt = "2026-03-17 00:00",
            white = "opp",
            black = "ych24",
            result = "0-1",
            sourceUrl = Some("https://lichess.org/black-subject"),
            pgn = pgn,
            moveEvals = favorableForBlack
          )
        )
      )

    assert(result.isRight)
    val rows = result.toOption.get.featureRows
    assert(rows.nonEmpty)
    assert(rows.forall(_.collapseAnalysis.forall(_.rootCause != "evaluation swing")))
