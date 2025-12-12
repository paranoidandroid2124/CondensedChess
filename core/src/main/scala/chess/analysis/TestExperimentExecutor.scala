package chess
package analysis

import chess.format.Fen
import chess.variant.Standard
import chess.analysis.MoveGenerator.CandidateMove
import chess.analysis.MoveGenerator.CandidateType
import chess.analysis.FeatureExtractor

object TestExperimentExecutor:
  def main(args: Array[String]): Unit =
    println("--- Testing ExperimentExecutor ---")

    val fen = "r3k2r/ppp2ppp/2n5/3p4/3PP3/2N5/PPP2PPP/R3K2R w KQkq - 0 1"
    val position = Fen.read(Standard, fen.asInstanceOf[chess.format.Fen.Full]).getOrElse(sys.error("Bad FEN"))
    
    // Create dummy candidates
    val candidates = List(
      CandidateMove("e4e5", "e5", CandidateType.CentralBreak, 0.9),
      CandidateMove("e4d5", "exd5", CandidateType.CentralBreak, 0.8)
    )

    // Dummy Engine
    val mockEngine: ExperimentExecutor.EngineProbe = (_, _, _) => {
      // Return fixed result
      ExperimentExecutor.AnalysisResult(Some(50), None, List("e5e4..."))
    }
    
    // Extract initial features
    val featuresBefore = FeatureExtractor.extractPositionFeatures(fen, 20)

    val results = ExperimentExecutor.runExperiments(position, candidates, featuresBefore, mockEngine)
    
    println(s"Ran ${results.size} experiments.")
    results.foreach { res =>
      println(s"Type: ${res.experimentType}, Move: ${res.forcedMove}, Eval: ${res.eval}, Feat(Center): ${res.featuresAfter.activity.whiteCenterControl}")
      if (res.featuresAfter.activity.whiteCenterControl >= 0) println("[PASS] Features extracted.")
      else println("[FAIL] Feature extraction suspicious.")
    }
