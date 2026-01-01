package lila.llm.model.strategic

import play.api.libs.json.*

/**
 * Represents a variation line from engine analysis.
 */
case class VariationLine(
    moves: List[String],
    scoreCp: Int,
    mate: Option[Int] = None,
    resultingFen: Option[String] = None,
    tags: List[VariationTag] = Nil
):
  /** Material + Positional evaluation unified into CP */
  def effectiveScore: Int = 
    mate.map(m => if m > 0 then 10000 - m else -10000 + m).getOrElse(scoreCp)

object VariationLine:
  given Reads[VariationLine] = Json.reads[VariationLine]
  given Writes[VariationLine] = Json.writes[VariationLine]

/**
 * Semantic tags for variations to help labels and narratives.
 */
enum VariationTag:
  case Sharp, Solid, Prophylaxis, Simplification, Mistake, Good, Excellent, Inaccuracy, Blunder, Forced

object VariationTag:
  given Reads[VariationTag] = Reads:
    case JsString(s) => 
      try JsSuccess(VariationTag.valueOf(s))
      catch case _: Exception => JsError(s"Invalid VariationTag: $s")
    case _ => JsError("String expected")
  given Writes[VariationTag] = Writes(t => JsString(t.toString))
