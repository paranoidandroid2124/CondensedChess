package chess
package analysis

object AnalysisStage extends Enumeration {
  type AnalysisStage = Value
  
  val ENGINE_EVALUATION = Value("engine_evaluation")
  val CRITICAL_DETECTION = Value("critical_detection")
  val LLM_GENERATION = Value("llm_generation")
  val FINALIZATION = Value("finalization")
  
  def labelFor(stage: AnalysisStage): String = stage match {
    case ENGINE_EVALUATION => "Evaluating positions with Stockfish..."
    case CRITICAL_DETECTION => "Detecting critical moments..."
    case LLM_GENERATION => "Generating AI narratives..."
    case FINALIZATION => "Building final review..."
  }
  
  // Stage weights for progress calculation
  val weights: Map[AnalysisStage, Double] = Map(
    ENGINE_EVALUATION -> 0.4,
    CRITICAL_DETECTION -> 0.1,
    LLM_GENERATION -> 0.45,
    FINALIZATION -> 0.05
  )
  
  def totalProgress(stage: AnalysisStage, stageProgress: Double): Double = {
    val completedStages = values.filter(_.id < stage.id)
    val completedWeight = completedStages.map(weights).sum
    val currentWeight = weights(stage) * stageProgress
    completedWeight + currentWeight
  }
}
