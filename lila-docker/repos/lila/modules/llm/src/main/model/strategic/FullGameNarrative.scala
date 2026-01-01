package lila.llm.model.strategic

import lila.llm.model.ExtendedAnalysisData

import play.api.libs.json._

case class GameMetadata(
  white: String,
  black: String,
  event: String, 
  date: String,
  result: String
)
object GameMetadata {
  implicit val writes: OWrites[GameMetadata] = Json.writes[GameMetadata]
}

case class MomentNarrative(
  ply: Int,
  momentType: String,          // "Blunder", "MissedWin", "TensionPeak", etc.
  narrative: String,           // The generated Book-Style narrative text
  analysisData: ExtendedAnalysisData
)
object MomentNarrative {
  implicit val writes: OWrites[MomentNarrative] = Json.writes[MomentNarrative]
}

case class FullGameNarrative(
  gameIntro: String,           // e.g. "In this Ruy Lopez encounter..."
  keyMomentNarratives: List[MomentNarrative],
  conclusion: String,          // e.g. "White capitalized on the blunder..."
  overallThemes: List[String]  // e.g. ["King hunt", "Exchange sacrifice"]
)
object FullGameNarrative {
  implicit val writes: OWrites[FullGameNarrative] = Json.writes[FullGameNarrative]
}
