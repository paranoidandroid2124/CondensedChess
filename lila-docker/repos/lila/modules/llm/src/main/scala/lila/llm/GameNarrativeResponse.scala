package lila.llm

import play.api.libs.json.*
import lila.llm.model.{ FullGameNarrative, MomentNarrative }
import lila.llm.model.strategic.VariationLine

case class GameNarrativeResponse(
    schema: String,
    intro: String,
    moments: List[GameNarrativeMoment],
    conclusion: String,
    themes: List[String]
)

object GameNarrativeResponse:

  val schemaV1 = "chesstory.gameNarrative.v1"

  def fromNarrative(narrative: FullGameNarrative): GameNarrativeResponse =
    GameNarrativeResponse(
      schema = schemaV1,
      intro = narrative.gameIntro,
      moments = narrative.keyMomentNarratives.map(GameNarrativeMoment.fromMoment),
      conclusion = narrative.conclusion,
      themes = narrative.overallThemes
    )

  given Writes[GameNarrativeResponse] = Json.writes[GameNarrativeResponse]

case class GameNarrativeMoment(
    ply: Int,
    momentType: String,
    fen: String,
    narrative: String,
    concepts: List[String],
    variations: List[VariationLine]
)

object GameNarrativeMoment:

  def fromMoment(moment: MomentNarrative): GameNarrativeMoment =
    GameNarrativeMoment(
      ply = moment.ply,
      momentType = moment.momentType,
      fen = moment.analysisData.fen,
      narrative = moment.narrative,
      concepts = moment.analysisData.conceptSummary,
      variations = moment.analysisData.alternatives
    )

  given Writes[GameNarrativeMoment] = Json.writes[GameNarrativeMoment]

