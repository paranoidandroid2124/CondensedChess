package lila.llm.strategicobject

import lila.llm.analysis.MoveTruthFrame

final case class StrategicRelation(
    operator: String,
    targetObjectId: String
)

final case class StrategicObject(
    id: String,
    family: String,
    anchors: List[String],
    relations: List[StrategicRelation] = Nil
)

trait StrategicObjectSynthesizer:
  def synthesize(
      primitives: PrimitiveBank,
      truth: MoveTruthFrame
  ): List[StrategicObject]
