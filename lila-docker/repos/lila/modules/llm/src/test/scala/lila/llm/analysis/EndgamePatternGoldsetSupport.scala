package lila.llm.analysis

import chess.Color
import lila.llm.analysis.strategic.{ EndgameAnalyzerImpl, EndgamePatternOracle }
import lila.llm.model.strategic.EndgameFeature
import play.api.libs.json.*

import scala.io.Source

object EndgamePatternGoldsetSupport:

  final case class PatternCase(
      id: String,
      kind: String,
      fen: String,
      color: String,
      expectedLabel: String,
      signalsOverride: Option[JsObject]
  )

  final case class PatternRow(
      pattern: String,
      defaultSignals: JsObject,
      cases: List[PatternCase]
  )

  final case class EvaluatedCase(
      core: EndgameFeature,
      finalFeature: EndgameFeature
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

  def rows: List[PatternRow] =
    Source
      .fromResource("endgame_goldset_v2_patterns.jsonl")
      .getLines()
      .toList
      .map(_.stripPrefix("\uFEFF").trim)
      .filter(_.nonEmpty)
      .zipWithIndex
      .map { case (line, idx) =>
        Json.parse(line).validate[PatternRow].asEither match
          case Left(err)  => throw new IllegalArgumentException(s"invalid row at line ${idx + 1}: $err")
          case Right(row) => row
      }

  def evaluateCase(c: PatternCase): Either[String, EvaluatedCase] =
    parseColor(c.color) match
      case None => Left(s"${c.id}: invalid color=${c.color}")
      case Some(color) =>
        val boardOpt = chess.format.Fen.read(chess.variant.Standard, chess.format.Fen.Full(c.fen)).map(_.board)
        boardOpt match
          case None => Left(s"${c.id}: invalid FEN")
          case Some(board) =>
            analyzer.analyze(board, color) match
              case None => Left(s"${c.id}: core endgame feature missing")
              case Some(core) =>
                val finalFeature = EndgamePatternOracle
                  .detect(board = board, color = color, coreFeature = core)
                  .map(EndgamePatternOracle.applyPattern(core, _))
                  .getOrElse(core)
                Right(EvaluatedCase(core = core, finalFeature = finalFeature))

  def expectedSignals(row: PatternRow, c: PatternCase): JsObject =
    if c.expectedLabel == row.pattern then mergeSignals(row.defaultSignals, c.signalsOverride.getOrElse(Json.obj()))
    else c.signalsOverride.getOrElse(Json.obj())

  def compareSignals(expected: JsObject, feature: EndgameFeature): (Int, Int, List[String]) =
    var checks = 0
    var matches = 0
    val mismatches = scala.collection.mutable.ListBuffer.empty[String]

    def record(key: String, actual: String, expectedDesc: String, ok: Boolean): Unit =
      checks += 1
      if ok then matches += 1
      else mismatches += s"$key expected=$expectedDesc actual=$actual"

    expected.value.foreach { (key, js) =>
      key match
        case "oppositionType" =>
          js.asOpt[String].foreach { expectedToken =>
            val actual = feature.oppositionType.toString
            record(key, actual, expectedToken, matchesToken(actual, expectedToken))
          }
        case "ruleOfSquare" =>
          js.asOpt[String].foreach { expectedToken =>
            val actual = feature.ruleOfSquare.toString
            record(key, actual, expectedToken, matchesToken(actual, expectedToken))
          }
        case "triangulationAvailable" =>
          js.asOpt[Boolean].foreach { expectedValue =>
            val actual = feature.triangulationAvailable
            record(key, actual.toString, expectedValue.toString, actual == expectedValue)
          }
        case "rookEndgamePattern" =>
          js.asOpt[String].foreach { expectedToken =>
            val actual = feature.rookEndgamePattern.toString
            record(key, actual, expectedToken, matchesToken(actual, expectedToken))
          }
        case "zugzwangLikely" =>
          js.asOpt[Boolean].foreach { expectedValue =>
            val actual = feature.isZugzwang || feature.zugzwangLikelihood >= 0.65
            record(key, actual.toString, expectedValue.toString, actual == expectedValue)
          }
        case "theoreticalOutcomeHint" =>
          js.asOpt[String].foreach { expectedToken =>
            val actual = feature.theoreticalOutcomeHint.toString
            record(key, actual, expectedToken, matchesToken(actual, expectedToken))
          }
        case "confidenceMin" =>
          js.asOpt[Double].foreach { min =>
            record(key, feature.confidence.toString, s">= $min", feature.confidence >= min)
          }
        case "confidenceMax" =>
          js.asOpt[Double].foreach { max =>
            record(key, feature.confidence.toString, s"<= $max", feature.confidence <= max)
          }
        case "kingActivityDeltaSign" =>
          js.asOpt[String].foreach {
            case "positive" =>
              record(key, feature.kingActivityDelta.toString, "positive", feature.kingActivityDelta > 0)
            case "neutral_or_positive" =>
              record(key, feature.kingActivityDelta.toString, "neutral_or_positive", feature.kingActivityDelta >= 0)
            case "neutral_or_negative" =>
              record(key, feature.kingActivityDelta.toString, "neutral_or_negative", feature.kingActivityDelta <= 0)
            case other =>
              record(key, feature.kingActivityDelta.toString, other, ok = false)
          }
        case _ => ()
    }

    (checks, matches, mismatches.toList)

  private def parseColor(raw: String): Option[Color] =
    if raw.equalsIgnoreCase("white") then Some(Color.White)
    else if raw.equalsIgnoreCase("black") then Some(Color.Black)
    else None

  private def mergeSignals(base: JsObject, overrides: JsObject): JsObject =
    JsObject(base.value ++ overrides.value)

  private def matchesToken(actual: String, expectedToken: String): Boolean =
    expectedToken.split("_or_").exists(token => actual.equalsIgnoreCase(token))
