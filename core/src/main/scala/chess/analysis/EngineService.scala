package chess
package analysis

import scala.concurrent.{Future, ExecutionContext}
import org.slf4j.LoggerFactory

// Use the new types
import AnalysisTypes._
import AnalysisModel.{EngineEval, EngineLine}
import scala.concurrent.duration._
import chess.analysis.AsyncUtils

class EngineService(
    // No client passed in, uses Pool
    // maxConcurrent handled by Pool
)(using ec: ExecutionContext) extends EngineInterface:
  
  private val logger = LoggerFactory.getLogger("chess.engine.service")

  // Helper: Convert StockfishClient.Line to AnalysisModel.EngineLine
  private def convertLine(l: StockfishClient.Line): EngineLine =
    EngineLine(
      move = l.pv.headOption.getOrElse(""),
      winPct = l.winPercent,
      cp = l.cp,
      mate = l.mate,
      pv = l.pv
    )

  // Helper: Convert StockfishClient.EvalResult to EngineResult
  private def convertResult(res: StockfishClient.EvalResult, depth: Int): EngineResult =
    val lines = res.lines.map(convertLine)
    val eval = EngineEval(depth, lines)
    EngineResult(eval, res.bestmove)

  /**
   * Submit an analysis request to the engine.
   * Handles concurrency via EnginePool.
   */
  def submit(req: EngineRequest, retries: Int = 2): Future[EngineResult] =
    req match
      case a: Analyze => submitAnalyze(a, retries)
      case ClearHash  => 
        Future.successful {
          EngineResult(EngineEval(0, Nil), None)
        }

  private def submitAnalyze(req: Analyze, retries: Int): Future[EngineResult] =
    val job = EnginePool.withEngine { client =>
        // use async client.evaluateFen
        client.evaluateFen(req.fen, req.depth, req.multiPv, Some(req.timeoutMs), req.moves).flatMap {
             case Right(res) => Future.successful(convertResult(res, req.depth))
             case Left(err) => Future.failed(new RuntimeException(err))
        }.recoverWith {
             case e: Throwable => Future.failed(e)
        }
    }

    job.recoverWith {
      case e: RuntimeException if retries > 0 && Option(e.getMessage).exists(m => m.contains("Timeout") || m.contains("client busy") || m.contains("process")) =>
         logger.warn(s"Retrying job due to engine error: ${e.getMessage} (attempts left: $retries)")
         // Async backoff
         AsyncUtils.delay(100.millis).flatMap { _ =>
            submitAnalyze(req, retries - 1)
         }
      case e: Throwable =>
         logger.error(s"Job failed: ${e.getMessage}")
         Future.failed(e)
    }

  // EngineInterface implementation
  override def evaluate(
      fen: String,
      depth: Int,
      multiPv: Int, // Default inherited from trait
      timeoutMs: Int,
      moves: List[String],
      searchMoves: List[String]
  )(using ExecutionContext): Future[EngineEval] =
    val req = Analyze(fen, moves, depth, multiPv, timeoutMs, searchMoves)
    submit(req).map(_.eval)
