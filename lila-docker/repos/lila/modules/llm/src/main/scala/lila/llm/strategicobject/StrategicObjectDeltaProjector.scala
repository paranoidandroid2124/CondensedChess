package lila.llm.strategicobject

import lila.llm.analysis.DecisiveTruthContract

enum StrategicDeltaScope:
  case MoveLocal
  case PositionLocal
  case Comparative

final case class StrategicObjectDelta(
    objectId: String,
    scope: StrategicDeltaScope,
    summary: String,
    before: Option[String] = None,
    after: Option[String] = None
)

trait StrategicObjectDeltaProjector:
  def project(
      contract: DecisiveTruthContract,
      objects: List[StrategicObject]
  ): List[StrategicObjectDelta]
