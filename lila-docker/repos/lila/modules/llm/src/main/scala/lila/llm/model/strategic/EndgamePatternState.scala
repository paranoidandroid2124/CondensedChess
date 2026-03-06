package lila.llm.model.strategic

import play.api.libs.json.*

/**
 * Tracks endgame pattern continuity across consecutive moves.
 *
 * Enables three capabilities:
 *  1. Repetition suppression  – patternAge > 0 avoids re-introducing the same pattern.
 *  2. Progress delta reporting – comparing kingActivityDelta / confidence across plies.
 *  3. Transition detection     – activePattern changes signal a structural shift.
 */
case class EndgamePatternState(
    activePattern: Option[String],
    patternAge: Int,
    outcomeHint: TheoreticalOutcomeHint,
    prevKingActivityDelta: Int,
    prevConfidence: Double,
    lastPly: Int
)

object EndgamePatternState:
  private given Reads[TheoreticalOutcomeHint] = Reads {
    case JsString(value) =>
      scala.util.Try(TheoreticalOutcomeHint.valueOf(value)).toOption match
        case Some(v) => JsSuccess(v)
        case None    => JsError(s"invalid theoretical outcome hint: $value")
    case _ => JsError("theoretical outcome hint must be a string")
  }

  private given Writes[TheoreticalOutcomeHint] = Writes(v => JsString(v.toString))

  val empty: EndgamePatternState = EndgamePatternState(
    activePattern = None,
    patternAge = 0,
    outcomeHint = TheoreticalOutcomeHint.Unclear,
    prevKingActivityDelta = 0,
    prevConfidence = 0.0,
    lastPly = 0
  )

  def from(eg: EndgameFeature, ply: Int): EndgamePatternState = EndgamePatternState(
    activePattern = eg.primaryPattern,
    patternAge = 0,
    outcomeHint = eg.theoreticalOutcomeHint,
    prevKingActivityDelta = eg.kingActivityDelta,
    prevConfidence = eg.confidence,
    lastPly = ply
  )

  /** Advance: if the same pattern persists, bump age by ply gap; otherwise reset to 0. */
  def evolve(
      prev: Option[EndgamePatternState],
      current: Option[EndgameFeature],
      ply: Int
  ): Option[EndgamePatternState] =
    current.map { eg =>
      val currentPattern = eg.primaryPattern
      val age = prev match
        case Some(p) if p.activePattern.isDefined && p.activePattern == currentPattern =>
          p.patternAge + Math.max(1, ply - p.lastPly)
        case _ => 0
      EndgamePatternState(
        activePattern = currentPattern,
        patternAge = age,
        outcomeHint = eg.theoreticalOutcomeHint,
        prevKingActivityDelta = eg.kingActivityDelta,
        prevConfidence = eg.confidence,
        lastPly = ply
      )
    }

  given Reads[EndgamePatternState] = Json.reads[EndgamePatternState]
  given Writes[EndgamePatternState] = Json.writes[EndgamePatternState]
