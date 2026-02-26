package lila.llm.model.strategic

import play.api.libs.json.*

/**
 * Parsed PV Move with coordinate-level information.
 * This is the "first-class" representation of a move in the PV.
 */
case class PvMove(
    uci: String,                    // "d1b3"
    san: String,                    // "Qb3"
    from: String,                   // "d1"
    to: String,                     // "b3"
    piece: String,                  // "Q" (uppercase = white, lowercase = black in standard)
    isCapture: Boolean,
    capturedPiece: Option[String],
    givesCheck: Boolean
)

object PvMove:
  given Reads[PvMove] = Json.reads[PvMove]
  given Writes[PvMove] = Json.writes[PvMove]

/**
 * Represents a variation line from engine analysis.
 */
case class VariationLine(
    moves: List[String],            // Raw UCI moves
    scoreCp: Int,
    mate: Option[Int] = None,
    depth: Int = 0,
    resultingFen: Option[String] = None,
    tags: List[VariationTag] = Nil,
    parsedMoves: List[PvMove] = Nil // Parsed moves with metadata
):
  /** Material + Positional evaluation unified into CP */
  def effectiveScore: Int = 
    mate.map(m => if m > 0 then 10000 - m else -10000 + m).getOrElse(scoreCp)
  
  /** Get our move (index 0) */
  def ourMove: Option[PvMove] = parsedMoves.headOption
  
  /** Get opponent's reply (index 1) */
  def theirReply: Option[PvMove] = parsedMoves.lift(1)

object VariationLine:
  given Reads[VariationLine] = Json.reads[VariationLine]
  given Writes[VariationLine] = Json.writes[VariationLine]

/**
 * Container for engine analysis evidence.
 * Preserves raw PV data all the way to the renderer.
 */
case class EngineEvidence(
    depth: Int,
    variations: List[VariationLine]
):
  /** Best variation (rank 1) */
  def best: Option[VariationLine] = variations.headOption
  
  /** Alternatives (rank 2+) that are within threshold of best */
  def alternatives(thresholdCp: Int = 40): List[VariationLine] =
    best match
      case Some(b) =>
        variations.drop(1).filter(v => Math.abs(v.scoreCp - b.scoreCp) <= thresholdCp)
      case None => Nil

object EngineEvidence:
  given Reads[EngineEvidence] = Json.reads[EngineEvidence]
  given Writes[EngineEvidence] = Json.writes[EngineEvidence]

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
