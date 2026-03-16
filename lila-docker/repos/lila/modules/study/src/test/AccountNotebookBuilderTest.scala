package lila.study

import java.time.Instant
import play.api.libs.json.JsObject

class AccountNotebookBuilderTest extends munit.FunSuite:

  import AccountNotebookBuilder.*

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
      ExternalGame("chesscom", "g1", "2026-03-17 00:01", "ych24", "opp1", "0-1", Some("https://example.com/g1"), samplePgn("ych24", "opp1", "Queen's Gambit Declined", qgdMoves, "0-1")),
      ExternalGame("chesscom", "g2", "2026-03-17 00:02", "ych24", "opp2", "1/2-1/2", Some("https://example.com/g2"), samplePgn("ych24", "opp2", "Queen's Gambit Declined", qgdMoves, "1/2-1/2")),
      ExternalGame("chesscom", "g3", "2026-03-17 00:03", "ych24", "opp3", "0-1", Some("https://example.com/g3"), samplePgn("ych24", "opp3", "Queen's Gambit Declined", qgdMoves, "0-1")),
      ExternalGame("chesscom", "g4", "2026-03-17 00:04", "ych24", "opp4", "1/2-1/2", Some("https://example.com/g4"), samplePgn("ych24", "opp4", "Queen's Gambit Declined", qgdMoves, "1/2-1/2")),
      ExternalGame("chesscom", "g5", "2026-03-17 00:05", "opp5", "ych24", "1-0", Some("https://example.com/g5"), samplePgn("opp5", "ych24", "Sicilian Defense", sicilianMoves, "1-0")),
      ExternalGame("chesscom", "g6", "2026-03-17 00:06", "opp6", "ych24", "1/2-1/2", Some("https://example.com/g6"), samplePgn("opp6", "ych24", "Sicilian Defense", sicilianMoves, "1/2-1/2")),
      ExternalGame("chesscom", "g7", "2026-03-17 00:07", "opp7", "ych24", "1-0", Some("https://example.com/g7"), samplePgn("opp7", "ych24", "Sicilian Defense", sicilianMoves, "1-0")),
      ExternalGame("chesscom", "g8", "2026-03-17 00:08", "opp8", "ych24", "1/2-1/2", Some("https://example.com/g8"), samplePgn("opp8", "ych24", "Sicilian Defense", sicilianMoves, "1/2-1/2"))
    )

  test("build my account dossier with required sections"):
    val result = buildFromGames("chesscom", "ych24", ProductKind.MyAccountIntelligenceLite, games, generatedAt = Instant.parse("2026-03-17T00:00:00Z"))
    assert(result.isRight)
    val dossier = result.toOption.get.dossier
    assertEquals((dossier \ "productKind").as[String], "my_account_intelligence_lite")
    assertEquals((dossier \ "status").as[String], "ready")
    assertEquals((dossier \ "overview" \ "cards").as[List[JsObject]].map(_("kind").as[String]), List("opening_identity", "recurring_leak", "repair_priority"))
    assertEquals((dossier \ "sections").as[List[JsObject]].count(_("kind").as[String] == "opening_map"), 1)
    assert((dossier \ "sections").as[List[JsObject]].count(_("kind").as[String] == "pattern_cluster") >= 2)
    assertEquals((dossier \ "sections").as[List[JsObject]].count(_("kind").as[String] == "exemplar_games"), 1)
    assertEquals((dossier \ "sections").as[List[JsObject]].count(_("kind").as[String] == "action_page"), 1)

  test("build opponent prep dossier with steering and checklist"):
    val result = buildFromGames("chesscom", "ych24", ProductKind.OpponentPrep, games, generatedAt = Instant.parse("2026-03-17T00:00:00Z"))
    assert(result.isRight)
    val dossier = result.toOption.get.dossier
    assertEquals((dossier \ "productKind").as[String], "opponent_prep")
    assertEquals((dossier \ "overview" \ "cards").as[List[JsObject]].map(_("kind").as[String]), List("opening_identity", "pressure_point", "steering_target"))
    assertEquals((dossier \ "sections").as[List[JsObject]].count(_("kind").as[String] == "steering_plan"), 1)
    assertEquals((dossier \ "sections").as[List[JsObject]].count(_("kind").as[String] == "pre_game_checklist"), 1)
    assertEquals((dossier \ "appendix" \ "sampledGames").as[List[JsObject]].size, 8)
