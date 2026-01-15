package lila.llm

import play.api.libs.json._
import lila.llm.model.strategic.VariationLine

case class MoveEval(
  ply: Int,
  cp: Int,
  mate: Option[Int] = None,
  pv: List[String] = Nil,
  variations: List[VariationLine] = Nil
) {
  // If no detailed variations, construct one from PV
  def getVariations: List[VariationLine] = 
    if (variations.nonEmpty) variations
    else if (pv.nonEmpty) List(VariationLine(moves = pv, scoreCp = cp, mate = mate))
    else Nil
}

object MoveEval:
  given Reads[MoveEval] = Json.reads[MoveEval]
  given Writes[MoveEval] = Json.writes[MoveEval]
