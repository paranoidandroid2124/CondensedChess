package lila.llm.analysis

import munit.FunSuite
import play.api.libs.json.*

import scala.io.Source

class EndgameGoldsetV2ContractTest extends FunSuite:

  private case class PatternCase(
      id: String,
      kind: String,
      fen: String,
      color: String,
      expectedLabel: String,
      signalsOverride: Option[JsObject]
  )

  private case class PatternRow(
      pattern: String,
      defaultSignals: JsObject,
      cases: List[PatternCase]
  )

  private given Reads[PatternCase] = Reads { js =>
    for
      id <- (js \ "id").validate[String]
      kind <- (js \ "kind").validate[String]
      fen <- (js \ "fen").validate[String]
      color <- (js \ "color").validate[String]
      expectedLabel <- (js \ "expectedLabel").validate[String]
      signalsOverride <- (js \ "signalsOverride").validateOpt[JsObject]
    yield PatternCase(
      id = id,
      kind = kind,
      fen = fen,
      color = color,
      expectedLabel = expectedLabel,
      signalsOverride = signalsOverride
    )
  }

  private given Reads[PatternRow] = Reads { js =>
    for
      pattern <- (js \ "pattern").validate[String]
      defaultSignals <- (js \ "defaultSignals").validate[JsObject]
      cases <- (js \ "cases").validate[List[PatternCase]]
    yield PatternRow(pattern = pattern, defaultSignals = defaultSignals, cases = cases)
  }

  private def rows: List[PatternRow] =
    Source
      .fromResource("endgame_goldset_v2_patterns.jsonl")
      .getLines()
      .toList
      .map(_.stripPrefix("\uFEFF").trim)
      .filter(_.nonEmpty)
      .zipWithIndex
      .map { case (line, idx) =>
        Json.parse(line).validate[PatternRow].asEither match
          case Left(err) => fail(s"invalid row at line ${idx + 1}: $err")
          case Right(row) => row
      }

  test("v2 patterns goldset should include 15 unique patterns") {
    val all = rows
    assertEquals(all.size, 15)
    assertEquals(all.map(_.pattern).distinct.size, 15)
  }

  test("each pattern row should include 2 positive, 2 negative, 1 boundary cases") {
    rows.foreach { row =>
      val byKind = row.cases.groupBy(_.kind.toLowerCase).view.mapValues(_.size).toMap
      assertEquals(row.cases.size, 5, clues(row.pattern))
      assertEquals(byKind.getOrElse("positive", 0), 2, clues(row.pattern))
      assertEquals(byKind.getOrElse("negative", 0), 2, clues(row.pattern))
      assertEquals(byKind.getOrElse("boundary", 0), 1, clues(row.pattern))
    }
  }

  test("case IDs should be globally unique and expected labels should be valid") {
    val allCases = rows.flatMap(_.cases)
    assertEquals(allCases.map(_.id).distinct.size, allCases.size)
    val validPatterns = rows.map(_.pattern).toSet + "None"
    assert(allCases.forall(c => validPatterns.contains(c.expectedLabel)), clues(allCases.filterNot(c => validPatterns.contains(c.expectedLabel)).map(_.id)))
  }

  test("all FEN entries should be legal and colors should be white/black") {
    val allCases = rows.flatMap(_.cases)
    allCases.foreach { c =>
      val boardOpt = chess.format.Fen.read(chess.variant.Standard, chess.format.Fen.Full(c.fen)).map(_.board)
      assert(boardOpt.isDefined, clues(c.id))
      assert(c.color.equalsIgnoreCase("white") || c.color.equalsIgnoreCase("black"), clues(c.id))
    }
  }

