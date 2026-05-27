package lila.commentary

import play.api.libs.json.*

case class EngineAlternative(
    uci: String,
    san: Option[String],
    cpAfterAlt: Option[Int],
    cpLossVsPlayed: Option[Int],
    pv: List[String]
)
object EngineAlternative:
  given Writes[EngineAlternative] = Json.writes[EngineAlternative]
