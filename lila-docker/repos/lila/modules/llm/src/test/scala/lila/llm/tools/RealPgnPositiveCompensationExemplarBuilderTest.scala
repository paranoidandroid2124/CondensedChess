package lila.llm.tools

import java.time.Instant

import munit.FunSuite

import lila.llm.PgnAnalysisHelper

class RealPgnPositiveCompensationExemplarBuilderTest extends FunSuite:

  private val samplePgn =
    """[Event "Sample"]
      |[Site "Local"]
      |[Date "2026.03.21"]
      |[Round "?"]
      |[White "White"]
      |[Black "Black"]
      |[Result "*"]
      |
      |1. d4 Nf6 2. c4 e6 3. Nf3 d5 *
      |""".stripMargin

  test("buildCorpus selects the canonical real-PGN exemplar subset from a source corpus") {
    val source =
      RealPgnNarrativeEvalRunner.Corpus(
        version = 1,
        generatedAt = "2026-03-21T00:00:00Z",
        asOfDate = "2026-03-21",
        title = "Source",
        description = "source corpus",
        games =
          (RealPgnPositiveCompensationExemplarBuilder.SelectedGameIds :+ "noise_game").map(id =>
            RealPgnNarrativeEvalRunner.CorpusGame(
              id = id,
              tier = "master_classical",
              family = "test_family",
              label = id,
              notes = Nil,
              expectedThemes = Nil,
              pgn = samplePgn
            )
          )
      )
    val corpus =
      RealPgnPositiveCompensationExemplarBuilder.buildCorpus(
        sourceCorpus = source,
        generatedAt = Instant.parse("2026-03-21T00:00:00Z")
      )

    assertEquals(corpus.games.map(_.id), RealPgnPositiveCompensationExemplarBuilder.SelectedGameIds)
    corpus.games.foreach { game =>
      val extracted = PgnAnalysisHelper.extractPlyDataStrict(game.pgn)
      assert(extracted.isRight, clue(game.id, extracted))
      assert(extracted.toOption.exists(_.nonEmpty), clue(game.id, game.pgn))
      assertEquals(game.tier, "positive_exemplar")
      assert(game.notes.exists(_.contains("Real-PGN positive compensation exemplar")), clue(game.notes))
      assert(game.expectedThemes.nonEmpty, clue(game.id))
    }
  }
