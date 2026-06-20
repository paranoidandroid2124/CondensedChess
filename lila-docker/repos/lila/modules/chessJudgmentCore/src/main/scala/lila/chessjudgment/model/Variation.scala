package lila.chessjudgment.model.strategic

import play.api.libs.json.*

/**
 * Parsed PV Move with coordinate-level information.
 * This is the "first-class" representation of a move in the PV.
 */
case class PvMove(
    uci: String,                    // "d1b3"
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
    lineFeatures: List[VariationFeature] = Nil,
    parsedMoves: List[PvMove] = Nil // Parsed moves with metadata
):
  /** Get our move (index 0) */
  def ourMove: Option[PvMove] = parsedMoves.headOption
  
  /** Get opponent's reply (index 1) */
  def theirReply: Option[PvMove] = parsedMoves.lift(1)

object VariationLine:
  given Reads[VariationLine] = Json.reads[VariationLine]
  given Writes[VariationLine] = Json.writes[VariationLine]

/**
 * Container for engine analysis evidence.
 * Preserves raw PV data.
 */
case class EngineEvidence(
    depth: Int,
    variations: List[VariationLine]
):
  /** Best variation (rank 1) */
  def best: Option[VariationLine] = variations.headOption

object EngineEvidence:
  given Reads[EngineEvidence] = Json.reads[EngineEvidence]
  given Writes[EngineEvidence] = Json.writes[EngineEvidence]

enum VariationFeature:
  case Sharp, Solid, Prophylaxis, Simplification

object VariationFeature:
  given Reads[VariationFeature] = Reads:
    case JsString(s) => 
      try JsSuccess(VariationFeature.valueOf(s))
      catch case _: Exception => JsError(s"Invalid VariationFeature: $s")
    case _ => JsError("String expected")
  given Writes[VariationFeature] = Writes(t => JsString(t.toString))
