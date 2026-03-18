package lila.accountintel

import java.time.Instant

import lila.accountintel.AccountIntel.*
import lila.accountintel.service.{
  AccountNotebookEngine,
  SelectiveEvalLookup,
  SelectiveEvalProbe,
  SelectiveEvalRefiner
}
import play.api.libs.json.JsObject

class AccountNotebookEngineTest extends munit.FunSuite:

  given Executor = scala.concurrent.ExecutionContext.global

  private val noEvalLookup = new SelectiveEvalLookup:
    def lookup(fen: String) = fuccess(none[SelectiveEvalProbe])

  private val engine =
    new AccountNotebookEngine(
      null.asInstanceOf[AccountGameFetcher],
      new SelectiveEvalRefiner(noEvalLookup, noEvalLookup)
    )

  private def samplePgn(white: String, black: String, opening: String, moves: String, result: String) =
    s"""[Event "Account Notebook Fixture"]
[Site "?"]
[Date "2026.03.17"]
[White "$white"]
[Black "$black"]
[Result "$result"]
[UTCDate "2026.03.17"]
[UTCTime "12:00:00"]
[Variant "Standard"]
[Opening "$opening"]

$moves $result
"""

  private val qgdMoves =
    "1. d4 d5 2. c4 e6 3. Nc3 Nf6 4. Nf3 Be7 5. Bg5 O-O 6. e3 h6 7. Bh4 b6 8. cxd5 exd5 9. Bd3 c5 10. O-O Nc6 11. Rc1 Be6 12. Qa4"

  private val sicilianMoves =
    "1. e4 c5 2. Nf3 d6 3. d4 cxd4 4. Nxd4 Nf6 5. Nc3 a6 6. Be3 e6 7. f3 b5 8. Qd2 Nbd7 9. O-O-O Bb7 10. g4 h6 11. h4 b4 12. Nce2"

  private val games =
    List(
      ExternalGame(
        "chesscom",
        "g1",
        "2026-03-17 00:01",
        "ych24",
        "opp1",
        "0-1",
        Some("https://example.com/g1"),
        samplePgn("ych24", "opp1", "Queen's Gambit Declined", qgdMoves, "0-1")
      ),
      ExternalGame(
        "chesscom",
        "g2",
        "2026-03-17 00:02",
        "ych24",
        "opp2",
        "1/2-1/2",
        Some("https://example.com/g2"),
        samplePgn("ych24", "opp2", "Queen's Gambit Declined", qgdMoves, "1/2-1/2")
      ),
      ExternalGame(
        "chesscom",
        "g3",
        "2026-03-17 00:03",
        "ych24",
        "opp3",
        "0-1",
        Some("https://example.com/g3"),
        samplePgn("ych24", "opp3", "Queen's Gambit Declined", qgdMoves, "0-1")
      ),
      ExternalGame(
        "chesscom",
        "g4",
        "2026-03-17 00:04",
        "ych24",
        "opp4",
        "1/2-1/2",
        Some("https://example.com/g4"),
        samplePgn("ych24", "opp4", "Queen's Gambit Declined", qgdMoves, "1/2-1/2")
      ),
      ExternalGame(
        "chesscom",
        "g5",
        "2026-03-17 00:05",
        "opp5",
        "ych24",
        "1-0",
        Some("https://example.com/g5"),
        samplePgn("opp5", "ych24", "Sicilian Defense", sicilianMoves, "1-0")
      ),
      ExternalGame(
        "chesscom",
        "g6",
        "2026-03-17 00:06",
        "opp6",
        "ych24",
        "1/2-1/2",
        Some("https://example.com/g6"),
        samplePgn("opp6", "ych24", "Sicilian Defense", sicilianMoves, "1/2-1/2")
      ),
      ExternalGame(
        "chesscom",
        "g7",
        "2026-03-17 00:07",
        "opp7",
        "ych24",
        "1-0",
        Some("https://example.com/g7"),
        samplePgn("opp7", "ych24", "Sicilian Defense", sicilianMoves, "1-0")
      ),
      ExternalGame(
        "chesscom",
        "g8",
        "2026-03-17 00:08",
        "opp8",
        "ych24",
        "1/2-1/2",
        Some("https://example.com/g8"),
        samplePgn("opp8", "ych24", "Sicilian Defense", sicilianMoves, "1/2-1/2")
      )
    )

  private val mixedWhiteGames =
    List(
      ExternalGame(
        "chesscom",
        "mw1",
        "2026-03-17 00:01",
        "ych24",
        "opp1",
        "0-1",
        Some("https://example.com/mw1"),
        samplePgn("ych24", "opp1", "Queen's Gambit Declined", qgdMoves, "0-1")
      ),
      ExternalGame(
        "chesscom",
        "mw2",
        "2026-03-17 00:02",
        "ych24",
        "opp2",
        "1-0",
        Some("https://example.com/mw2"),
        samplePgn("ych24", "opp2", "English Opening", qgdMoves, "1-0")
      ),
      ExternalGame(
        "chesscom",
        "mb1",
        "2026-03-17 00:03",
        "opp3",
        "ych24",
        "1-0",
        Some("https://example.com/mb1"),
        samplePgn("opp3", "ych24", "Sicilian Defense", sicilianMoves, "1-0")
      ),
      ExternalGame(
        "chesscom",
        "mb2",
        "2026-03-17 00:04",
        "opp4",
        "ych24",
        "0-1",
        Some("https://example.com/mb2"),
        samplePgn("opp4", "ych24", "Sicilian Defense", sicilianMoves, "0-1")
      ),
      ExternalGame(
        "chesscom",
        "mb3",
        "2026-03-17 00:05",
        "opp5",
        "ych24",
        "1/2-1/2",
        Some("https://example.com/mb3"),
        samplePgn("opp5", "ych24", "Sicilian Defense", sicilianMoves, "1/2-1/2")
      ),
      ExternalGame(
        "chesscom",
        "mb4",
        "2026-03-17 00:06",
        "opp6",
        "ych24",
        "1-0",
        Some("https://example.com/mb4"),
        samplePgn("opp6", "ych24", "Sicilian Defense", sicilianMoves, "1-0")
      ),
      ExternalGame(
        "chesscom",
        "mb5",
        "2026-03-17 00:07",
        "opp7",
        "ych24",
        "0-1",
        Some("https://example.com/mb5"),
        samplePgn("opp7", "ych24", "Sicilian Defense", sicilianMoves, "0-1")
      ),
      ExternalGame(
        "chesscom",
        "mb6",
        "2026-03-17 00:08",
        "opp8",
        "ych24",
        "1/2-1/2",
        Some("https://example.com/mb6"),
        samplePgn("opp8", "ych24", "Sicilian Defense", sicilianMoves, "1/2-1/2")
      )
    )

  test("build my account dossier with required sections"):
    engine
      .buildFromGames(
        "chesscom",
        "ych24",
        ProductKind.MyAccountIntelligenceLite,
        games,
        Instant.parse("2026-03-17T00:00:00Z")
      )
      .map: result =>
        assert(result.isRight)
        val dossier = result.toOption.get.dossier
        assertEquals((dossier \ "productKind").as[String], "my_account_intelligence_lite")
        assertEquals((dossier \ "status").as[String], "ready")
        assertEquals((result.toOption.get.surface \ "schema").as[String], "chesstory.account.surface.v1")
        assert((result.toOption.get.surface \ "patterns").as[List[JsObject]].nonEmpty)
        assertEquals(
          (dossier \ "overview" \ "cards").as[List[JsObject]].map(_("kind").as[String]),
          List("opening_identity", "recurring_leak", "repair_priority")
        )
        assertEquals((dossier \ "sections").as[List[JsObject]].count(_("kind").as[String] == "opening_map"), 1)
        assert((dossier \ "sections").as[List[JsObject]].count(_("kind").as[String] == "pattern_cluster") >= 2)
        assertEquals((dossier \ "sections").as[List[JsObject]].count(_("kind").as[String] == "exemplar_games"), 1)
        assertEquals((dossier \ "sections").as[List[JsObject]].count(_("kind").as[String] == "action_page"), 1)

  test("build opponent prep dossier with steering and checklist"):
    engine
      .buildFromGames(
        "chesscom",
        "ych24",
        ProductKind.OpponentPrep,
        games,
        Instant.parse("2026-03-17T00:00:00Z")
      )
      .map: result =>
        assert(result.isRight)
        val dossier = result.toOption.get.dossier
        assertEquals((dossier \ "productKind").as[String], "opponent_prep")
        assertEquals((result.toOption.get.surface \ "productKind").as[String], "opponent_prep")
        assertEquals(
          (dossier \ "overview" \ "cards").as[List[JsObject]].map(_("kind").as[String]),
          List("opening_identity", "pressure_point", "steering_target")
        )
        assertEquals((dossier \ "sections").as[List[JsObject]].count(_("kind").as[String] == "steering_plan"), 1)
        assertEquals(
          (dossier \ "sections").as[List[JsObject]].count(_("kind").as[String] == "pre_game_checklist"),
          1
        )
        assertEquals((dossier \ "appendix" \ "sampledGames").as[List[JsObject]].size, 8)

  test("weak white opening support downgrades to mixed repertoire wording"):
    engine
      .buildFromGames(
        "chesscom",
        "ych24",
        ProductKind.MyAccountIntelligenceLite,
        mixedWhiteGames,
        Instant.parse("2026-03-17T00:00:00Z")
      )
      .map: result =>
        val dossier = result.toOption.get.dossier
        val openingCards =
          (dossier \ "sections")
            .as[List[JsObject]]
            .find(_("kind").as[String] == "opening_map")
            .map(_("cards").as[List[JsObject]])
            .getOrElse(Nil)
        val whiteCard = openingCards.find(_("side").as[String] == "white").get
        assertEquals((whiteCard \ "openingFamily").as[String], "Mixed White repertoire")
