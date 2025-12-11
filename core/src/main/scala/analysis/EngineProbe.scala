package chess
package analysis

import scala.concurrent.{Future, ExecutionContext}
import AnalysisModel.{ EngineEval, EngineLine }

object EngineProbe:
  def evalFen(client: StockfishClient, fen: String, depth: Int, multiPv: Int, moveTimeMs: Option[Int])(using ec: ExecutionContext): Future[EngineEval] =
    client.evaluateFen(fen, depth = depth, multiPv = multiPv, moveTimeMs = moveTimeMs).map {
      case Right(res) =>
        val lines = res.lines.map { l =>
          EngineLine(
            move = l.pv.headOption.getOrElse(""),
            winPct = l.winPercent,
            cp = l.cp,
            mate = l.mate,
            pv = l.pv
          )
        }
        EngineEval(depth, lines)
      case Left(err) =>
        System.err.println(s"[engine-error] $err")
        EngineEval(depth, Nil)
    }
