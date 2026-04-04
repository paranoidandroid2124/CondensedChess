package lila.llm.model

import lila.llm.model.{ Motif, PlanMatch, PositionNature }
import lila.llm.model.strategic._
import lila.llm.model.structure.{ PlanAlignment, StructureProfile }
import play.api.libs.json.*

// The SSOT (Single Source of Truth) for LLM Prompt Generation
case class ExtendedAnalysisData(
    fen: String,
    
    // Base Layers
    nature: PositionNature,
    motifs: List[Motif], 
    plans: List[PlanMatch],
    
    // Semantic Layer (New)
    preventedPlans: List[PreventedPlan],
    pieceActivity: List[PieceActivity],
    structuralWeaknesses: List[WeakComplex],
    positionalFeatures: List[PositionalTag] = Nil, // Outpost, OpenFile, etc.
    compensation: Option[Compensation],
    endgameFeatures: Option[EndgameFeature],
    
    // Human-Like Eval
    practicalAssessment: Option[PracticalAssessment],
    alternatives: List[VariationLine] = Nil,
    candidates: List[AnalyzedCandidate] = Nil,
    counterfactual: Option[CounterfactualMatch] = None,
    
    // Strategic Labels (High-level concepts from ConceptLabeler)
    conceptSummary: List[String] = Nil,

    // Context
    prevMove: Option[String],
    ply: Int,
    evalCp: Int,             // White POV centipawns
    isWhiteToMove: Boolean,
    phase: String = "middlegame",
    planContinuity: Option[PlanContinuity] = None,
    planSequence: Option[JsObject] = None,
    tacticalThreatToUs: Boolean = false,
    tacticalThreatToThem: Boolean = false,
    structureProfile: Option[StructureProfile] = None,
    structureEvalLatencyMs: Option[Long] = None,
    planAlignment: Option[PlanAlignment] = None,
    planHypotheses: List[JsObject] = Nil,
    strategicSalience: StrategicSalience = StrategicSalience.High,

    // Endgame pattern continuity
    endgamePatternAge: Int = 0,                  // consecutive plies the same pattern has been active
    endgameTransition: Option[String] = None,     // e.g. "Lucena(Win) → Draw" when pattern shifts
    
    // Full IntegratedContext (preserves classification, threats, pawnAnalysis, features)
    integratedContext: Option[JsValue] = None
) {
  def toContext: JsValue =
    integratedContext.getOrElse(Json.obj("fen" -> fen, "evalCp" -> evalCp, "isWhiteToMove" -> isWhiteToMove))
}

object ExtendedAnalysisData {
  implicit val writes: OWrites[ExtendedAnalysisData] = OWrites { data =>
    Json.obj(
      "fen" -> data.fen,
      "ply" -> data.ply,
      "nature" -> data.nature.natureType.toString
    )
  }
}
