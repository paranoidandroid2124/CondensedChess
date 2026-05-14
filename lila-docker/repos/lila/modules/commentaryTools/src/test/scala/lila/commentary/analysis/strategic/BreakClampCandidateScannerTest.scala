package lila.commentary.analysis.strategic

import java.nio.charset.StandardCharsets
import java.nio.file.Files

import chess.Color
import lila.commentary.model.strategic.VariationLine
import munit.FunSuite

class BreakClampCandidateScannerTest extends FunSuite:

  test("reports a clean route clamp when the played move removes a break route with no transform") {
    val report =
      BreakClampCandidateScanner.scan(
        games = List(cleanClampGame),
        engine = Some(FixedEngine(List(VariationLine(List("b2b4", "e8e7", "e1e2", "e7e6"), scoreCp = 0, depth = 12))))
      )

    assertEquals(report.cleanRows.map(_.classification), List("clean_route_clamp"), clues(report.rows))
    val row = report.cleanRows.headOption.getOrElse(fail("missing clean route clamp"))
    assertEquals(row.gameId, "clean-route")
    assertEquals(row.playedUci, "b2b4")
    assertEquals(row.engineGate, BreakClampCandidateScanner.EngineGate.SourceMoveTopPv)
    assertEquals(row.routeId, "black:b5-b4:quiet_push")
    assertEquals(row.routeToken, "...b5-b4")
    assertEquals(row.destinationToken, "...b4")
    assertEquals(row.transformRisk, "None")
    assertEquals(row.transformVerdicts, "-")
    assertEquals(row.blockers, "none")
    assertEquals(row.suggestedPlyRange, "1-1")
  }

  test("keeps a recapturable same-destination transform as a blocker instead of a clean candidate") {
    val report =
      BreakClampCandidateScanner.scan(
        games = List(transformClampGame),
        engine = Some(FixedEngine(List(VariationLine(List("b2b4", "e8e7", "e1e2", "e7e6"), scoreCp = 0, depth = 12))))
      )

    assertEquals(report.cleanRows, Nil)
    val row = report.transformBlockedRows.headOption.getOrElse(fail("missing transform blocker"))
    assertEquals(row.routeId, "black:b5-b4:quiet_push")
    assertEquals(row.transformRisk, "CaptureTransform")
    assertEquals(row.transformVerdicts, "RecaptureAvailableUnproven")
    assertEquals(row.blockers, "owner:break_prevention_capture_transform_recapture_unproven")
  }

  test("does not mark engine-missing or absent-source-move rows as clean") {
    val missingEngine =
      BreakClampCandidateScanner.scan(games = List(cleanClampGame), engine = None)
    assertEquals(missingEngine.cleanRows, Nil)
    assert(missingEngine.rows.exists(_.blockers == "engine:missing"), clues(missingEngine.rows))

    val absentSourceMove =
      BreakClampCandidateScanner.scan(
        games = List(cleanClampGame),
        engine = Some(FixedEngine(List(VariationLine(List("e1e2", "e8e7", "e2e3", "e7e6"), scoreCp = 0, depth = 12))))
      )
    assertEquals(absentSourceMove.cleanRows, Nil)
    assert(absentSourceMove.rows.exists(_.blockers == "engine:source_move_absent_from_multipv"), clues(absentSourceMove.rows))
  }

  test("requires enough played-branch plies before reporting a clean candidate") {
    val report =
      BreakClampCandidateScanner.scan(
        games = List(cleanClampGame),
        engine = Some(FixedEngine(List(VariationLine(List("b2b4", "e8e7", "e1e2"), scoreCp = 0, depth = 12))))
      )

    assertEquals(report.cleanRows, Nil)
    assert(report.rows.exists(_.blockers == "branch:too_short"), clues(report.rows))
  }

  test("reads RealPgn-style corpus JSON and single PGN files with stable ids and labels") {
    val corpusPath = Files.createTempFile("break-clamp-corpus", ".json")
    val pgnPath = Files.createTempFile("break-clamp-single", ".pgn")
    Files.writeString(
      corpusPath,
      s"""{"version":1,"games":[{"id":"corpus-clean","label":"Corpus Clean","pgn":${jsonString(cleanPgn)}}]}""",
      StandardCharsets.UTF_8
    )
    Files.writeString(pgnPath, cleanPgn, StandardCharsets.UTF_8)

    val corpusGames = BreakClampCandidateScanner.readCorpus(corpusPath)
    val singleGame = BreakClampCandidateScanner.readPgnFile(pgnPath, id = "single-clean", label = "Single Clean")

    assertEquals(
      corpusGames.map(game => game.id -> game.label),
      List("corpus-clean" -> "Corpus Clean"),
      clues(corpusGames)
    )
    assertEquals(singleGame.id, "single-clean")
    assertEquals(singleGame.label, "Single Clean")
    assertEquals(singleGame.pgn, cleanPgn)
  }

  test("source-game scope can restrict scanning to a candidate ply range and focus side") {
    val outOfFocus =
      cleanClampGame.copy(plyRange = Some(1 -> 1), focusColor = Some(Color.Black))
    val report =
      BreakClampCandidateScanner.scan(
        games = List(outOfFocus),
        engine = Some(FixedEngine(List(VariationLine(List("b2b4", "e8e7", "e1e2", "e7e6"), scoreCp = 0, depth = 12))))
      )

    assertEquals(report.scannedPlies, 0)
    assertEquals(report.rows, Nil)
  }

  test("markdown lists clean candidates before transform-blocked diagnostics") {
    val cleanReport =
      BreakClampCandidateScanner.scan(
        games = List(cleanClampGame, transformClampGame),
        engine = Some(FixedEngine(List(VariationLine(List("b2b4", "e8e7", "e1e2", "e7e6"), scoreCp = 0, depth = 12))))
      )
    val markdown = BreakClampCandidateScanner.markdown(cleanReport)

    assert(markdown.contains("## Clean Candidates"), clues(markdown))
    assert(markdown.contains("## Transform-Blocked Diagnostics"), clues(markdown))
    assert(markdown.indexOf("clean-route") < markdown.indexOf("transform-route"), clues(markdown))
    assert(markdown.contains("cleanCandidates=1"), clues(markdown))
    assert(markdown.contains("transformBlockers=1"), clues(markdown))
  }

  private val cleanPgn =
    """[Event "Clean route test"]
      |[Site "?"]
      |[Date "2026.05.14"]
      |[Round "?"]
      |[White "White"]
      |[Black "Black"]
      |[Result "*"]
      |[SetUp "1"]
      |[FEN "4k3/8/8/1p6/8/2P5/1P6/4K3 w - - 0 1"]
      |
      |1. b4 Ke7 2. Ke2 Ke6 *
      |""".stripMargin.trim

  private val transformPgn =
    """[Event "Transform route test"]
      |[Site "?"]
      |[Date "2026.05.14"]
      |[Round "?"]
      |[White "White"]
      |[Black "Black"]
      |[Result "*"]
      |[SetUp "1"]
      |[FEN "4k3/8/8/1pp5/8/2P5/1P6/4K3 w - - 0 1"]
      |
      |1. b4 Ke7 2. Ke2 Ke6 *
      |""".stripMargin.trim

  private def cleanClampGame =
    BreakClampCandidateScanner.SourceGame("clean-route", "Clean Route", cleanPgn)

  private def transformClampGame =
    BreakClampCandidateScanner.SourceGame("transform-route", "Transform Route", transformPgn)

  private final case class FixedEngine(lines: List[VariationLine]) extends BreakClampCandidateScanner.Engine:
    override def newGame(): Unit = ()
    override def analyze(fen: String, depth: Int, multiPv: Int): List[VariationLine] = lines

  private def jsonString(value: String): String =
    "\"" + value.flatMap {
      case '\\' => "\\\\"
      case '"'  => "\\\""
      case '\n' => "\\n"
      case '\r' => "\\r"
      case '\t' => "\\t"
      case c    => c.toString
    } + "\""
