package lila.llm.model

import play.api.libs.json.*

case class CollapseAnalysis(
    interval: String,
    rootCause: String,
    earliestPreventablePly: Int,
    patchLineUci: List[String],
    recoverabilityPlies: Int
)

object CollapseAnalysis:
  given Format[CollapseAnalysis] = Json.format[CollapseAnalysis]
