package lila.llm.model

import lila.llm.model.{ Motif, PlanMatch, PositionNature }
import lila.llm.model.strategic._

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
    planSequence: Option[PlanSequence] = None,
    tacticalThreatToUs: Boolean = false,
    tacticalThreatToThem: Boolean = false,
    
    // Full IntegratedContext (preserves classification, threats, pawnAnalysis, features)
    integratedContext: Option[lila.llm.analysis.IntegratedContext] = None
) {
  /** Returns stored IntegratedContext or constructs minimal fallback */
  def toContext: lila.llm.analysis.IntegratedContext = {
    integratedContext.getOrElse {
      lila.llm.analysis.IntegratedContext(
        evalCp = evalCp,
        isWhiteToMove = isWhiteToMove,
        threatsToUs = None,
        threatsToThem = None
      )
    }
  }
}

object ExtendedAnalysisData {
  import play.api.libs.json._
  
  implicit val writes: OWrites[ExtendedAnalysisData] = OWrites { data =>
    Json.obj(
      "fen" -> data.fen,
      "ply" -> data.ply,
      "nature" -> data.nature.natureType.toString
    )
  }
}
