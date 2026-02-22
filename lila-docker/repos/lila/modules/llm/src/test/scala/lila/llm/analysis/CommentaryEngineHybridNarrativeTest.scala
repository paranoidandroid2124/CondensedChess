package lila.llm.analysis

import lila.llm.PgnAnalysisHelper
import lila.llm.model.{ ExplorerGame, ExplorerMove, ExplorerPlayer, OpeningReference }
import lila.llm.model.strategic.VariationLine
import munit.FunSuite

class CommentaryEngineHybridNarrativeTest extends FunSuite:

  test("full-game moment narrative uses hybrid prose instead of hierarchical section dump"):
    val pgn = "1. e4 e5 2. Nf3 Nc6 3. Bb5 a6"
    val evals = Map(
      3 -> List(
        VariationLine(
          moves = List("g1f3", "b8c6"),
          scoreCp = 320,
          mate = None,
          depth = 12
        ),
        VariationLine(
          moves = List("d2d4", "e5d4"),
          scoreCp = 90,
          mate = None,
          depth = 12
        )
      )
    )

    val full = CommentaryEngine.generateFullGameNarrative(pgn, evals)
    val moment = full.keyMomentNarratives.headOption.getOrElse(fail("Expected at least one key moment narrative"))
    val text = moment.narrative

    assert(!text.contains("=== CONTEXT ["), clue(text))
    assert(text.toLowerCase.contains("block"), clue(text))
    assert(text.toLowerCase.contains("branch"), clue(text))

  test("opening branch moment includes precedent snippet when sample game data is valid"):
    val pgn = "1. e4 c5 2. Nf3 d6 3. d4 cxd4 4. Nxd4 Nf6 5. Nc3 a6 6. g4"
    val fenBefore6 = PgnAnalysisHelper.extractPlyData(pgn) match
      case Right(plies) =>
        plies.find(_.ply == 11).map(_.fen).getOrElse(fail("Expected ply 11 FEN in PGN extraction"))
      case Left(err) => fail(s"PGN extraction failed: $err")

    val openingRef = OpeningReference(
      eco = Some("B99"),
      name = Some("Sicilian Najdorf"),
      totalGames = 5000,
      topMoves = List(
        ExplorerMove("c1e3", "Be3", 1400, 560, 420, 420, 2700),
        ExplorerMove("c1g5", "Bg5", 1200, 480, 360, 360, 2680),
        ExplorerMove("f2f3", "f3", 900, 360, 270, 270, 2660)
      ),
      sampleGames = List(
        ExplorerGame(
          id = "sample1",
          winner = Some(chess.White),
          white = ExplorerPlayer("Carlsen, Magnus", 2864),
          black = ExplorerPlayer("Anand, Viswanathan", 2775),
          year = 2019,
          month = 6,
          event = Some("Norway Chess 2019"),
          pgn = Some("6. g4 e5 7. Nde2 h5")
        )
      )
    )

    val evals = Map(
      11 -> List(
        VariationLine(
          moves = List("g2g4", "e7e5"),
          scoreCp = -50,
          mate = None,
          depth = 14
        )
      )
    )

    val full = CommentaryEngine.generateFullGameNarrative(
      pgn = pgn,
      evals = evals,
      openingRefsByFen = Map(fenBefore6 -> openingRef)
    )

    val openingMoment = full.keyMomentNarratives.find(_.momentType == "OpeningOutOfBook").getOrElse {
      fail(s"Expected OpeningOutOfBook moment, got: ${full.keyMomentNarratives.map(_.momentType).mkString(",")}")
    }
    val text = openingMoment.narrative
    val lower = text.toLowerCase

    assert(lower.contains("in magnus carlsen-viswanathan anand"), clue(text))
    assert(lower.contains("after 6. g4 e5 7. nde2 h5"), clue(text))
    assert(lower.contains("won (1-0)"), clue(text))
