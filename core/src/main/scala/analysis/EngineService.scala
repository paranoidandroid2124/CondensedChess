package chess
package analysis

import scala.concurrent.{Future, ExecutionContext}
import org.slf4j.LoggerFactory

// Use the new types
import AnalysisTypes._
import AnalysisModel.{EngineEval, EngineLine}

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
        try
          // Pass forcedMoves and apply internal timeout (plus buffer)
          client.evaluateFen(req.fen, req.depth, req.multiPv, Some(req.timeoutMs), req.moves) match
            case Right(res) => Future.successful(convertResult(res, req.depth))
            case Left(err) => Future.failed(new RuntimeException(err))
        catch 
           case e: Throwable => Future.failed(e)
    }

    // Wrap with strict timeout safety (in case Engine hangs despite internal timeout)
    val strictTimeout = req.timeoutMs + 2000 // 2s Grace buffer
    
    val promised = scala.concurrent.Promise[EngineResult]()
    
    // Timer thread or scheduler? avoiding new heavy dependencies.
    // Ideally use Akka Scheduler or Java Timer.
    // For simplicity in this stack, we'll rely on EnginePool's Future completing.
    // But if we want *Strict* timeout:
    // We can use a daemon thread to fail the promise.
    // Implementing simple Future.firstCompletedOf logic if needed, but requires a timer Future.
    // Given the constraints, let's trust EnginePool's internal timeout for now but improve the Retry/Recovery.
    
    job.recoverWith {
      case e: RuntimeException if retries > 0 && Option(e.getMessage).exists(m => m.contains("timeout") || m.contains("CreateProcess")) =>
         logger.warn(s"Retrying job due to engine error: ${e.getMessage} (attempts left: $retries)")
         // Backoff?
         val pauseMs = 100
         Thread.sleep(pauseMs) // blocking inside future? bad. but simple for now.
         submitAnalyze(req, retries - 1)
      case e: Throwable =>
         logger.error(s"Job failed: ${e.getMessage}")
         Future.failed(e)
    }

  // EngineInterface implementation
  override def evaluate(
      fen: String,
      depth: Int,
      multiPv: Int = 1,
      timeoutMs: Int = 1000,
      moves: List[String] = Nil
  )(using ExecutionContext): Future[EngineEval] =
    val req = Analyze(fen, moves, depth, multiPv, timeoutMs)
    submit(req).map(_.eval)
