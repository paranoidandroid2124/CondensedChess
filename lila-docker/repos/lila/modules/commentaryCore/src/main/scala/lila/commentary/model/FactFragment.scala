package lila.commentary.model

import play.api.libs.json.*

/**
 * FactFragment: A structured, language-independent representation of a chess moment's key facts.
 * Used to decouple tactical/strategic logic from raw English prose templates.
 */
sealed trait FactFragment

object FactFragment {

  // --- Concrete Fragment Implementations ---

  case class OpeningGoalFragment(
      san: String,
      openingName: Option[String],
      goalName: String,
      supportedEvidence: List[String]
  ) extends FactFragment

  case class KingSafetyFragment(
      san: String
  ) extends FactFragment

  case class StrategicSupportFragment(
      san: String,
      proofFamily: String,
      proofSource: String,
      purpose: String
  ) extends FactFragment

  case class TacticalThreatFragment(
      san: String,
      kind: String,
      targets: List[String]
  ) extends FactFragment

  case class DirectThreatFragment(
      san: String,
      isDefensive: Boolean,
      reason: String
  ) extends FactFragment

  case class CaptureFragment(
      san: String,
      purpose: String
  ) extends FactFragment

  case class EndgameFragment(
      san: String,
      facts: List[String]
  ) extends FactFragment

  // --- JSON Serialization Writes ---

  given Writes[FactFragment] = Writes {
    case f: OpeningGoalFragment =>
      Json.obj(
        "type" -> "opening_goal",
        "san" -> f.san,
        "openingName" -> f.openingName,
        "goalName" -> f.goalName,
        "supportedEvidence" -> f.supportedEvidence
      )
    case f: KingSafetyFragment =>
      Json.obj(
        "type" -> "king_safety",
        "san" -> f.san
      )
    case f: StrategicSupportFragment =>
      Json.obj(
        "type" -> "strategic_support",
        "san" -> f.san,
        "proofFamily" -> f.proofFamily,
        "proofSource" -> f.proofSource,
        "purpose" -> f.purpose
      )
    case f: TacticalThreatFragment =>
      Json.obj(
        "type" -> "tactical_threat",
        "san" -> f.san,
        "kind" -> f.kind,
        "targets" -> f.targets
      )
    case f: DirectThreatFragment =>
      Json.obj(
        "type" -> "direct_threat",
        "san" -> f.san,
        "isDefensive" -> f.isDefensive,
        "reason" -> f.reason
      )
    case f: CaptureFragment =>
      Json.obj(
        "type" -> "capture",
        "san" -> f.san,
        "purpose" -> f.purpose
      )
    case f: EndgameFragment =>
      Json.obj(
        "type" -> "endgame",
        "san" -> f.san,
        "facts" -> f.facts
      )
  }
}
