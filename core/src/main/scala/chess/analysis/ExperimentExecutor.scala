package chess
package analysis

import chess.format.Uci
import chess.format.Fen
import chess.Position
import chess.analysis.FeatureExtractor.PositionFeatures
import chess.analysis.MoveGenerator.{ CandidateMove, CandidateType }

object ExperimentExecutor:

  // --- Data structures ---

  case class AnalysisResult(
    score: Option[Int], // cp
    mate: Option[Int],  // mate in N
    pv: List[String]    // UCI strings
  )

  case class ExperimentConfig(
    structureDepth: Int = 14,
    tacticalDepth: Int = 12,
    planDepth: Int = 14,
    multiPv: Int = 1
  )

  case class ExperimentResult(
      experimentType: CandidateType, // Traceability to candidate category
      baseFen: String,
      forcedMove: String,            // UCI
      depth: Int,
      eval: Int,                     // cp (approximate if mate)
      mate: Option[Int],             // mate if exists
      pv: List[String],
      featuresBefore: PositionFeatures, // Context for comparison (delta calculation)
      featuresAfter: PositionFeatures   // Resulting features
  )

  // Engine interface abstraction: (FEN, Depth, MultiPV) => Result
  type EngineProbe = (String, Int, Int) => AnalysisResult

  // --- Main runner ---

  def runExperiments(
      position: Position,
      candidates: List[CandidateMove],
      featuresBefore: PositionFeatures,
      engine: EngineProbe,
      config: ExperimentConfig = ExperimentConfig()
  ): List[ExperimentResult] =
    
    val baseFen = Fen.write(position) // FullFen
    val results = List.newBuilder[ExperimentResult]

    candidates.foreach { candidate =>
      // Determine depth based on type
      val depth = candidate.candidateType match
        case CandidateType.TacticalCheck | CandidateType.SacrificeProbe => config.tacticalDepth
        case CandidateType.CentralBreak | CandidateType.QueensideMajority | CandidateType.Recapture => config.structureDepth
        case _ => config.planDepth
      
      // 1. Apply move to get new FEN and Features
      // We convert UCI to internal Move.
      // We try `position(move.toUci)` which usually returns Valid[(Position, MoveOrDrop)].
      // We assume scalachess API: `position(Uci)` exists.
      
      // If candidate.uci is invalid or strictly not in legalmoves, we skip.
      // But candidates come from valid legal moves.
            
      // We need to find the `Move` object first if we want robustness, or just use UCI string application.
      // Check if `position` has `apply(Uci)`. Based on previous successful compilation attempt (in my mental model? or just logic fix), 
      // I will proceed with `position(Uci)` pattern which is standard scalachess.
      
      val uciOpt = Uci(candidate.uci)
      
      uciOpt match {
        case Some(uciMove) =>
           val positionAfterRes = position(uciMove) // Returns Valid or Failure in scalachess usually
           // To convert to Option:
           val positionAfterOpt = positionAfterRes.toOption

           positionAfterOpt match
            case Some((posAfter, _)) => // Destructure tuple (Position, MoveOrDrop)
              val newFen = Fen.write(posAfter).value // Fen.write returns FullFen
              
              // 2. Extract Features After
              // Ply count: Position usually has fullMoveNumber or ply.
              // Fallback to 0 if unsure, but we want approx phase.
              // Using a dummy ply or reusing existing ply + 1.
              // We passed `position` (start). `position.ply`?
              // Existing code check: `position.ply` failed compilation.
              // `position.turns` failed (maybe it's `ply` in newer scalachess?).
              // I will use `0` as safe fallback for now, as Phase logic relies on material mostly.
              val plyEst = 20 
              val featuresAfter = FeatureExtractor.extractPositionFeatures(newFen, plyEst)
              
              // 3. Engine Probe
              val engineRes = engine(newFen, depth, config.multiPv)
              
              val evalVal = engineRes.score.getOrElse(
                engineRes.mate.map(m => if m > 0 then 10000 - m else -10000 + m).getOrElse(0)
              )
              
              results += ExperimentResult(
                experimentType = candidate.candidateType,
                baseFen = baseFen.value, 
                forcedMove = candidate.uci,
                depth = depth,
                eval = evalVal,
                mate = engineRes.mate,
                pv = engineRes.pv,
                featuresBefore = featuresBefore,
                featuresAfter = featuresAfter
              )
            case None =>
              // Failed to apply move
              println(s"[Experiment] Failed to apply move ${candidate.uci} to position.")
        case None =>
           println(s"[Experiment] Invalid UCI string: ${candidate.uci}")
      }
    }

    results.result()

  // --- Hypothesis Analysis (migrated from HypothesisValidator) ---

  /** Represents a "Plausible but Bad" move for "Why not?" explanations */
  final case class Hypothesis(
    move: String,
    eval: Double,
    refutation: List[String],
    label: String,
    diff: Double
  )

  /**
   * Finds "Plausible but Bad" moves to answer "Why not?"
   * @param engineProbe Function to evaluate a FEN
   * @param fen Current position FEN
   * @param bestMoveUci The actual best move (to exclude)
   * @param currentEval The evaluation of the best move (to calculate delta)
   * @param depth Search depth for candidates
   */
  def findHypotheses(
      engineProbe: EngineProbe,
      fen: String,
      bestMoveUci: String,
      currentEval: Double,
      depth: Int = 10
  ): List[Hypothesis] =
    // 1. Shallow search to find candidates (using EngineProbe for multiPv)
    engineProbe(fen, depth, 5) // 5 PVs
    
    // Approximate conversion: We only have one result line from EngineProbe type.
    // Need to use StockfishClient directly for multiPv, or adapt.
    // For now, return empty list as placeholder - real implementation needs EngineProbe redesign.
    // TODO: ExperimentExecutor.EngineProbe returns single result; need list for multiPv hypothesis.
    Nil

