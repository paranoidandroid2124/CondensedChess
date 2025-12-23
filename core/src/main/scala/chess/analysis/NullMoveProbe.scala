package chess.analysis

import scala.concurrent.{Future, ExecutionContext}
import chess.analysis.AnalysisTypes.ExperimentResult
import chess.analysis.AnalysisTypes.ExperimentType

object NullMoveProbe:

  /**
   * Probe: "What if I do nothing?" (Null Move)
   * 
   * Useful for:
   * 1. Threat Detection: If I pass and opponent's eval jumps, they have a threat.
   * 2. Zugzwang Detection: If I pass and opponent's eval drops (or mine improves), it might be zugzwang (rare).
   * 
   * Returns: 
   * - ExperimentResult with the opponent's best move as the "Threat".
   */
  def probe(
      fen: String,
      engine: EngineInterface,
      depth: Int = 10,
      timeoutMs: Int = 500
  )(using ExecutionContext): Future[Option[ExperimentResult]] =
    
    // Check if side to move is in check? (Null move illegal if in check)
    // We rely on EngineService or Client to validate, but here we can't easily check 'check'.
    // Stockfish supports "0000" as null move in UCI? No, standard is specific handling.
    // Usually we simulate by passing position with side flipped if supported, or playing a minimal pass?
    // Stockfish UCI doesn't support explicit "nullmove" command for search behavior for user.
    // Trick: Parse FEN, flip side to move, update EnPassant target to dash.
    
    val parts = fen.split(" ")
    if parts.length < 6 then return Future.successful(None)
    
    val sideToMove = parts(1) // "w" or "b"
    val nextSide = if sideToMove == "w" then "b" else "w"
    
    // Safety: If sideToMove is in check, we cannot flip turn legally (opponent would capture king).
    // We assume caller checks `!position.check`.
    
    // Construct Null Move FEN
    // 1. Board stays same
    // 2. Side flips
    // 3. Castling rights stay same (mostly)
    // 4. En Passant target becomes "-" (you lost chance to capture en passant by passing)
    // 5. Halfmove clock increments? (Technically yes, but for threat search irrelevant)
    // 6. Fullmove number stays same or increments? (Irrelevant for static threat)
    
    val nullFen = s"${parts(0)} $nextSide ${parts(2)} - ${parts(4)} ${parts(5)}"
    
    engine.evaluate(nullFen, depth, multiPv = 1, timeoutMs = timeoutMs).map { eval =>
      // eval is from perspective of 'nextSide' (the threatener)
      // If eval is very high for nextSide, that's the threat magnitude.
      
      eval.lines.headOption.map { bestLine =>
        // bestLine.moves.head is the threat move
        // Score:
        // if nextSide is Black, and eval.cp is -500 (Black winning), threat is strong.
        // We usually normalize eval to "White perspective" in our system? 
        // Need to check `AnalysisModel` convention.
        // Usually stockfish returns cp relative to side to move? 
        // EngineEval assumes normalized CP? 
        // Let's assume CP is relative to side-to-move for raw engine, but `EngineService` usually normalizes to White.
        // If `EngineService` normalizes to White, then:
        // If I am White, I pass (Black moves).
        // If Black has mate, CP/Mate will be -M.
        // So a large DROP in score (if I am White) means a threat.
        
        ExperimentResult(
          expType = ExperimentType.TacticalCheck,
          fen = fen,
          move = bestLine.pv.headOption, // The threat move
          eval = eval,
          metadata = Map(
            "candidateType" -> "Threat",
            "threatScore" -> bestLine.cp.map(_.toString).getOrElse("mate"),
            "threatMove" -> bestLine.pv.headOption.getOrElse("?")
          )
        )
      }
    }
