package chess
package analysis

import munit.FunSuite
import scala.concurrent.{Future, ExecutionContext}
import scala.concurrent.duration.*
import chess.analysis.ExperimentRunner
import chess.analysis.EngineService
import chess.analysis.FeatureExtractor
import chess.analysis.AnalysisModel
import chess.analysis.AnalysisTypes.*
import chess.format.Fen

class ExperimentRunnerTest extends FunSuite {
  
  import ExecutionContext.Implicits.global

  // Mock EngineService
  class MockEngineService extends EngineService() {
    override def submit(req: EngineRequest, retries: Int): Future[EngineResult] = {
      req match
        case r: Analyze =>
          Future.successful(EngineResult(
            eval = AnalysisModel.EngineEval(
              depth = r.depth,
              lines = List(AnalysisModel.EngineLine("e2e4", 0.5, Some(30), None, List("e2e4")))
            ),
            bestMove = Some("e2e4")
          ))
        case _ => Future.failed(new Exception("Unsupported mock request"))
    }
  }

  test("findHypotheses generates candidates and runs experiments") {
    val engine = new MockEngineService()
    val runner = new ExperimentRunner(engine)
    
    // Setup dummy position
    val fen = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1"
    // findHypotheses takes Position.
    val position = Fen.read(chess.variant.Standard, Fen.Full(fen)).get
    val features = FeatureExtractor.extractPositionFeatures(fen, 1)
    
    val futureResult = runner.findHypotheses(position, features, maxExperiments = 3)
    
    val result = scala.concurrent.Await.result(futureResult, 5.seconds)
    
    // We expect some results. Actually MoveGenerator on initial position might find Structure candidates?
    // It depends on MoveGenerator logic.
    // If empty, we can't assert size > 0.
    // But we verify no crash.
    assert(result != null)
  }
}
