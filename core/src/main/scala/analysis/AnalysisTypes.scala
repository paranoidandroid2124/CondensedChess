package chess
package analysis


object AnalysisTypes:

  // --- Engine Service Types ---

  sealed trait EngineRequest
  
  /**
   * Standard UCI analysis request.
   * @param fen The position FEN
   * @param moves Optional list of moves to force before analyzing (UCI position ... moves ...)
   * @param depth Target depth
   * @param multiPv MultiPV setting
   * @param timeoutMs Max time in milliseconds (soft limit for engine)
   */
  final case class Analyze(
      fen: String,
      moves: List[String] = Nil,
      depth: Int = 10,
      multiPv: Int = 1,
      timeoutMs: Int = 1000
  ) extends EngineRequest

  /**
   * Request to stop the engine or clear hash.
   */
  case object ClearHash extends EngineRequest
  
  final case class EngineResult(
      eval: AnalysisModel.EngineEval,
      bestMove: Option[String]
  )

  case class EngineError(message: String, cause: Option[Throwable] = None) extends RuntimeException(message, cause.orNull)

  // --- Experiment Types ---

  enum ExperimentType:
    case OpeningStats
    case TacticalCheck
    case TurningPointVerification
    case StructureAnalysis
    case EndgameCheck
    case GeneralEval

  // Result of a specific experiment execution
  final case class ExperimentResult(
      expType: ExperimentType,
      fen: String,
      move: Option[String], // The move being tested (if any)
      eval: AnalysisModel.EngineEval,
      metadata: Map[String, String] = Map.empty
  )
