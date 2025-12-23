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
      timeoutMs: Int = 1000,
      searchMoves: List[String] = Nil // Restrict search to these moves (UCI)
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

  // --- Narrative / Evidence Types ---

  case class EvidenceRef(kind: String, id: String)

  enum TagCategory:
    case Tactic, Structure, Plan, Endgame, Psychology, Dynamic

  case class RichTag(
    id: String,
    score: Double,
    category: TagCategory,
    evidenceRefs: List[EvidenceRef]
  )

  case class EvidencePack(
    kingSafety: Map[String, KingSafetyEvidence] = Map.empty,
    structure: Map[String, StructureEvidence] = Map.empty,
    tactics: Map[String, TacticsEvidence] = Map.empty,
    placement: Map[String, PlacementEvidence] = Map.empty,
    plans: Map[String, PlanEvidence] = Map.empty,
    pv: Map[String, PvEvidence] = Map.empty
  )

  case class PlanEvidence(
    concept: String,
    starterMove: String,
    goal: String,
    successScore: Double,
    pv: List[String]
  )


  case class KingSafetyEvidence(
      square: String, 
      openFiles: List[String], 
      attackers: Int, 
      checks: List[String],
      defenders: Int
  )
  
  case class StructureEvidence(
      description: String, // e.g. "Isolated Pawn"
      squares: List[String] // e.g. ["d4"]
  )
  
  case class TacticsEvidence(
      motif: String, 
      sequence: List[String], // SAN moves
      captured: Option[String] // e.g. "N"
  )
  
  case class PlacementEvidence(
      piece: String, 
      reason: String, 
      startSquare: String,
      targetSquare: String,
      delta: Double
  )
  
  case class PvEvidence(
      line: List[String], 
      eval: AnalysisModel.EngineEval
  )

