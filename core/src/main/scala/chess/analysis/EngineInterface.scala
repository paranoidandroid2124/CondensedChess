package chess
package analysis

import scala.concurrent.{Future, ExecutionContext}
import chess.analysis.AnalysisModel.EngineEval

/**
 * Pure engine evaluation interface (Student role).
 * 
 * This trait defines the contract for engine-only operations:
 * - FEN + depth + multiPV → Eval (no coaching/analysis logic)
 * 
 * Implementations: EngineService (production), MockEngine (testing)
 * 
 * All higher-level analysis (experiments, concept labeling) should go through
 * ExperimentRunner (Coach role), not directly through this interface.
 */
trait EngineInterface:
  
  /**
   * Evaluate a position.
   * 
   * @param fen       Position in FEN format
   * @param depth     Search depth
   * @param multiPv   Number of principal variations
   * @param timeoutMs Maximum time in milliseconds
   * @param moves     Optional forced moves sequence
   * @return Future containing engine evaluation
   */
  def evaluate(
      fen: String,
      depth: Int,
      multiPv: Int = 1,
      timeoutMs: Int = 1000,
      moves: List[String] = Nil
  )(using ExecutionContext): Future[EngineEval]


object EngineInterface:
  
  /**
   * Adapter to wrap EngineService as EngineInterface.
   * This provides backward compatibility while enforcing interface usage.
   */
  def fromService(service: EngineService)(using ExecutionContext): EngineInterface =
    new EngineInterface:
      def evaluate(
          fen: String,
          depth: Int,
          multiPv: Int,
          timeoutMs: Int,
          moves: List[String]
      )(using ExecutionContext): Future[EngineEval] =
        val req = AnalysisTypes.Analyze(
          fen = fen,
          depth = depth,
          multiPv = multiPv,
          timeoutMs = timeoutMs,
          moves = moves
        )
        service.submit(req).map(_.eval)
