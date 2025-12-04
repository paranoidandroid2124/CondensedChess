package chess
package analysis

import java.util.concurrent.ConcurrentHashMap

object AnalysisProgressTracker {
  
  case class AnalysisProgress(
    stage: AnalysisStage.AnalysisStage,
    stageProgress: Double,  // 0.0 to 1.0
    totalProgress: Double   // 0.0 to 1.0
  )
  
  private val progress = new ConcurrentHashMap[String, AnalysisProgress]()
  
  def update(jobId: String, stage: AnalysisStage.AnalysisStage, stageProgress: Double): Unit = {
    val totalProg = AnalysisStage.totalProgress(stage, stageProgress)
    val prog = AnalysisProgress(stage, stageProgress, totalProg)
    progress.put(jobId, prog)
  }
  
  def get(jobId: String): Option[AnalysisProgress] = {
    Option(progress.get(jobId))
  }
  
  def remove(jobId: String): Unit = {
    progress.remove(jobId)
  }
  
  def clear(): Unit = {
    progress.clear()
  }
}
