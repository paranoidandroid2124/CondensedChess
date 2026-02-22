package lila.llm.analysis

import munit.FunSuite
import play.api.libs.json.*
import chess.Color
import lila.llm.analysis.strategic.{ EndgameAnalyzerImpl, EndgamePatternOracle }

import scala.io.Source

class EndgamePatternDetectorTest extends FunSuite:

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
    yield PatternCase(id, kind, fen, color, expectedLabel, signalsOverride)
  }

  private given Reads[PatternRow] = Reads { js =>
    for
      pattern <- (js \ "pattern").validate[String]
      defaultSignals <- (js \ "defaultSignals").validate[JsObject]
      cases <- (js \ "cases").validate[List[PatternCase]]
    yield PatternRow(pattern, defaultSignals, cases)
  }

  private val analyzer = new EndgameAnalyzerImpl()

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

  private def parseColor(raw: String): Color =
    if raw.equalsIgnoreCase("white") then Color.White else Color.Black

  test("pattern detector should match v2 goldset labels") {
    val errors = scala.collection.mutable.ListBuffer.empty[String]
    rows.foreach { row =>
      row.cases.foreach { c =>
        val color = parseColor(c.color)
        val boardOpt = chess.format.Fen.read(chess.variant.Standard, chess.format.Fen.Full(c.fen)).map(_.board)
        if boardOpt.isEmpty then errors += s"${c.id}: invalid FEN"
        else
          val board = boardOpt.get
          val coreOpt = analyzer.analyze(board, color)
          if coreOpt.isEmpty then errors += s"${c.id}: core endgame feature missing"
          else
            val core = coreOpt.get
            val finalFeature = EndgamePatternOracle
              .detect(board = board, color = color, coreFeature = core)
              .map(EndgamePatternOracle.applyPattern(core, _))
              .getOrElse(core)

            val predictedLabel = finalFeature.primaryPattern.getOrElse("None")
            if predictedLabel != c.expectedLabel then
              errors += s"${c.id} (${row.pattern}/${c.kind}) expectedLabel=${c.expectedLabel} predicted=$predictedLabel coreHint=${core.theoreticalOutcomeHint} coreConf=${core.confidence} finalConf=${finalFeature.confidence}"

            ()
      }
    }
    if errors.nonEmpty then fail(errors.mkString("\n"))
  }
