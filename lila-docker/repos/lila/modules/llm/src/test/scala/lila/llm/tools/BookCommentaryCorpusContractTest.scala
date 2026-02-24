package lila.llm.tools

import java.nio.charset.StandardCharsets
import java.nio.file.{ Files, Paths }

import munit.FunSuite
import play.api.libs.json.*
import lila.llm.analysis.NarrativeUtils

class BookCommentaryCorpusContractTest extends FunSuite:

  private val corpusPath = Paths.get("modules/llm/docs/BookCommentaryCorpus.json")

  private case class CaseRow(
      id: String,
      phase: String,
      analysisFen: Option[String],
      startFen: Option[String],
      preMovesUci: Option[List[String]]
  )

  private def rows: List[CaseRow] =
    val raw = Files.readString(corpusPath, StandardCharsets.UTF_8)
    val root = Json.parse(raw)
    (root \ "cases").asOpt[List[JsObject]].getOrElse(Nil).map { js =>
      CaseRow(
        id = (js \ "id").as[String],
        phase = (js \ "phase").asOpt[String].getOrElse(""),
        analysisFen = (js \ "analysisFen").asOpt[String],
        startFen = (js \ "startFen").asOpt[String],
        preMovesUci = (js \ "preMovesUci").asOpt[List[String]]
      )
    }

  private def caseFen(row: CaseRow): Option[String] =
    row.analysisFen.orElse {
      for
        start <- row.startFen
        moves <- row.preMovesUci
      yield NarrativeUtils.uciListToFen(start, moves)
    }

  test("corpus case ids should be globally unique") {
    val ids = rows.map(_.id)
    assertEquals(ids.distinct.size, ids.size)
  }

  test("all corpus FEN entries should parse as legal board states") {
    rows.foreach { row =>
      val fen = caseFen(row).getOrElse(fail(s"${row.id}: missing analysisFen and startFen/preMovesUci"))
      val boardOpt = chess.format.Fen.read(chess.variant.Standard, chess.format.Fen.Full(fen)).map(_.board)
      assert(boardOpt.isDefined, clues(row.id))
    }
  }

  test("endgame corpus FEN entries should include both kings") {
    rows.filter(_.phase.equalsIgnoreCase("endgame")).foreach { row =>
      val fen = caseFen(row).getOrElse(fail(s"${row.id}: missing analysisFen and startFen/preMovesUci"))
      val boardOpt = chess.format.Fen.read(chess.variant.Standard, chess.format.Fen.Full(fen)).map(_.board)
      val board = boardOpt.getOrElse(fail(s"${row.id}: invalid FEN"))
      assert(board.kingPosOf(chess.Color.White).nonEmpty, clues(row.id))
      assert(board.kingPosOf(chess.Color.Black).nonEmpty, clues(row.id))
    }
  }

