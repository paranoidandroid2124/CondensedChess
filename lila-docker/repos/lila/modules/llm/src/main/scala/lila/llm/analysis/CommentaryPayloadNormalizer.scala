package lila.llm.analysis

import scala.util.Try
import scala.util.control.NonFatal
import play.api.libs.json.*

object CommentaryPayloadNormalizer:

  private val CommentaryRegex = """"commentary"\s*:\s*"((?:\\.|[^"\\])*)"""".r

  def normalize(raw: String): String =
    normalizeLoop(stripFences(Option(raw).getOrElse("").trim), depth = 0)

  private def normalizeLoop(text: String, depth: Int): String =
    val trimmed = stripFences(text)
    if trimmed.isEmpty || depth >= 4 then trimmed
    else
      parseStructured(trimmed)
        .orElse(extractCommentaryField(trimmed))
        .map(stripFences)
        .filter(_.nonEmpty)
        .filterNot(_ == trimmed)
        .map(next => normalizeLoop(next, depth + 1))
        .getOrElse(trimmed)

  private def parseStructured(text: String): Option[String] =
    try
      Json.parse(text) match
        case JsString(value) =>
          Option(value).map(_.trim).filter(_.nonEmpty)
        case obj: JsObject =>
          selectFromObject(obj)
        case _ => None
    catch
      case NonFatal(_) => None

  private def selectFromObject(obj: JsObject): Option[String] =
    List("commentary", "text", "value", "content")
      .view
      .flatMap(k => (obj \ k).toOption.flatMap(selectFromJson))
      .find(_.nonEmpty)

  private def selectFromJson(json: JsValue): Option[String] =
    json match
      case JsString(value) => Option(value).map(_.trim).filter(_.nonEmpty)
      case obj: JsObject   => selectFromObject(obj)
      case _               => None

  private def extractCommentaryField(text: String): Option[String] =
    CommentaryRegex
      .findFirstMatchIn(text)
      .flatMap(m => Try(Json.parse("\"" + m.group(1) + "\"").as[String].trim).toOption)
      .filter(_.nonEmpty)

  private def stripFences(text: String): String =
    Option(text)
      .getOrElse("")
      .replaceFirst("""(?s)^```(?:json|markdown|text)?\s*""", "")
      .replaceFirst("""(?s)\s*```$""", "")
      .trim
