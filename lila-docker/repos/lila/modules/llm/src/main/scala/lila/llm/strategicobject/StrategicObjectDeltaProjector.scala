package lila.llm.strategicobject

import lila.llm.analysis.DecisiveTruthContract

enum StrategicDeltaScope:
  case MoveLocal
  case PositionLocal
  case Comparative

final case class StrategicObjectDelta(
    objectId: String,
    scope: StrategicDeltaScope,
    evidenceRefs: List[String] = Nil
)

trait StrategicObjectDeltaProjector:
  def project(
      contract: DecisiveTruthContract,
      objects: List[StrategicObject]
  ): List[StrategicObjectDelta]
