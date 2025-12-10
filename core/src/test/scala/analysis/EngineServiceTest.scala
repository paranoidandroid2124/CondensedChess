package chess
package analysis

import munit.FunSuite
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.*
import scala.concurrent.Await
import AnalysisTypes._

class EngineServiceTest extends FunSuite {

  // Terminating engine pool after tests? MUnit doesn't have afterAll easily in FunSuite without traits.
  // We'll trust the pool to shut down or be reused.

  test("EngineService - Real Engine Smoke Test") {
    val service = new EngineService()
    
    val fen = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1"
    val moves = List("e2e4", "e7e5")
    val req = Analyze(fen = fen, moves = moves, depth = 5, timeoutMs = 500)
    
    val futureResult = service.submit(req)
    // EnginePool might take time to spin up stockfish
    val result = Await.result(futureResult, 10.seconds)
    
    assert(result.bestMove.isDefined)
    assert(result.eval.lines.nonEmpty)
  }
}
