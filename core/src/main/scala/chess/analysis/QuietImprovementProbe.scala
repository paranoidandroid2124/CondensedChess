package chess.analysis

import scala.concurrent.{Future, ExecutionContext}
import chess.analysis.AnalysisTypes.ExperimentResult
import chess.analysis.AnalysisTypes.ExperimentType

object QuietImprovementProbe:

  /**
   * Probe: "Find a good move that is NOT a capture or check."
   * 
   * Useful for:
   * 1. Strategic Planning: Finding "Piece Improvement" or "Prophylaxis".
   * 2. Filtering Tactical Noise: Ignoring evident captures to see the deeper position.
   * 
   * Requires:
   * - EngineInterface supporting `searchMoves` parameter.
   */
  def probe(
      fen: String,
      legalMoves: List[String], // All legal moves (UCI)
      uciCaptures: Set[String], // Set of moves that are captures
      uciChecks: Set[String],   // Set of moves that are checks
      engine: EngineInterface,
      depth: Int = 10,
      timeoutMs: Int = 800
  )(using ExecutionContext): Future[Option[ExperimentResult]] =
    
    // Filter for Quiet Moves
    val quietMoves = legalMoves.filter(m => !uciCaptures.contains(m) && !uciChecks.contains(m))
    
    if quietMoves.isEmpty then return Future.successful(None)
    
    // Search only these moves
    // Limit to top 5-10 to save engine overhead if list is huge?
    // For now pass all quiet moves. Engine handles search efficiency.
    
    engine.evaluate(
      fen, 
      depth, 
      multiPv = 1, 
      timeoutMs = timeoutMs, 
      searchMoves = quietMoves
    ).map { eval =>
      eval.lines.headOption.map { bestLine =>
        ExperimentResult(
          expType = ExperimentType.StructureAnalysis,
          fen = fen,
          move = bestLine.pv.headOption,
          eval = eval,
          metadata = Map(
            "candidateType" -> "QuietImprovement",
            "score" -> bestLine.cp.map(_.toString).getOrElse("?"),
            "pv" -> bestLine.pv.mkString(" ")
          )
        )
      }
    }
