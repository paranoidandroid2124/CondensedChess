package chess.analysis

import scala.concurrent.{Future, ExecutionContext}
import scala.collection.concurrent.TrieMap
import chess.analysis.AnalysisTypes._
import chess.analysis.MoveGenerator
import chess.format.Fen
import chess.analysis.AnalysisModel.EngineEval

// Enum for priority (Phase 2 Refactor)
enum ExperimentPriority:
  case High, Normal, Low

class ExperimentRunner(
    engineService: EngineService
)(using ec: ExecutionContext):

  // Cache: Key -> Future[ExperimentResult] to handle Thundering Herd
  // Key: Hash String
  private val memCache = TrieMap.empty[String, Future[ExperimentResult]]

  private def computeHash(key: (ExperimentType, String, Option[String])): String =
    val (etype, fen, move) = key
    val raw = s"${etype.toString}|$fen|${move.getOrElse("")}"
    java.security.MessageDigest.getInstance("MD5")
      .digest(raw.getBytes)
      .map("%02x".format(_))
      .mkString

  // Engine Interface Adapter
  private val engineInterface = EngineInterface.fromService(engineService)

  /**
   * Run a specific experiment with caching.
   */
  def run(
      expType: ExperimentType,
      fen: String,
      move: Option[String],
      depth: Int = 10,
      multiPv: Int = 1,
      forcedMoves: List[String] = Nil 
  ): Future[ExperimentResult] =
    val fullKey = (expType, fen, move)
    val hash = computeHash(fullKey)
    
    // Memory cache only - no disk persistence
    memCache.getOrElseUpdate(hash, {
      val request = Analyze(
        fen = fen, 
        moves = forcedMoves,
        depth = depth, 
        multiPv = multiPv,
        timeoutMs = depth * 150 
      )
      engineService.submit(request).map { res =>
        ExperimentResult(
          expType = expType,
          fen = fen,
          move = move,
          eval = res.eval
        )
      }
    })

  /**
   * Main entry point for hypothesis generation and testing (Phase 5 + Phase 2 Probes).
   * Generates candidate moves using heuristics, then runs experiments in parallel.
   */
  def findHypotheses(
      position: chess.Position,
      features: FeatureExtractor.PositionFeatures,
      maxExperiments: Int = 5
  ): Future[List[ExperimentResult]] =
    
    val fen = Fen.write(position, chess.FullMoveNumber(1)).value

    // 1. Generate Candidates using Heuristics (The "Coach"'s Intuition)
    val allCandidates = MoveGenerator.generateCandidates(position, features)
    
    // 2. Select Top Candidates (Prioritization)
    val selected = allCandidates
      .distinctBy(_.uci)
      .sortBy(-_.priority)
      .take(maxExperiments)

    // 3. Prepare Standard Experiments
    val stdExperiments = selected.map { candidate =>
      val expType = candidate.candidateType match
        case MoveGenerator.CandidateType.TacticalCheck => ExperimentType.TacticalCheck
        case _ => ExperimentType.StructureAnalysis 
          
      run(
         expType = expType,
         fen = fen,
         move = Some(candidate.uci),
         depth = 10,
         multiPv = 1,
         forcedMoves = List(candidate.uci)
      ).map(res => res.copy(metadata = res.metadata + ("candidateType" -> candidate.candidateType.toString)))
    }

    // 4. Run Special Probes (Phase 2)
    // Only run if not already overloaded
    val probeFutures = List.newBuilder[Future[Option[ExperimentResult]]]
    
    // A. Null Move Probe (Threat Detection) - critical for "Why?" explanations
    probeFutures += NullMoveProbe.probe(fen, engineInterface, depth = 10)

    // B. Strategic Probe (Quiet Moves) - if position is not too tactical
    // Skip if multiple tactical candidates exist
    val isTactical = selected.exists(_.candidateType == MoveGenerator.CandidateType.TacticalCheck)
    if !isTactical then
       // We need set of captures/checks to filter quiet moves.
       // Re-using position context.
       val legalMoves = position.legalMoves
       val captures = legalMoves.filter(m => position.board.isOccupied(m.dest)).map(_.toUci.uci).toSet
       // Checks check? 
       // position.legalMoves doesn't pre-calc checks for next move cheaply?
       // We can just rely on captures for now for "Quiet" approximation or skip checks if expensive.
       // Let's assume captures is enough to filter "obvious" tactical moves for now.
       val uciChecks = Set.empty[String] // TODO: Better check detection if critical
       val allUci = legalMoves.map(_.toUci.uci).toList
       
       probeFutures += QuietImprovementProbe.probe(fen, allUci, captures, uciChecks, engineInterface, depth = 8)

    // Combine all
    val allFutures = stdExperiments ++ probeFutures.result().map(_.map(_.toList))

    Future.foldLeft(allFutures)(List.empty[ExperimentResult]) { (acc, res) =>
      res match
        case r: ExperimentResult => acc :+ r
        case l: List[?] => acc ++ l.asInstanceOf[List[ExperimentResult]]
        case _ => acc // Should not happen with current map logic
    }

  def clearCache(): Unit =
    memCache.clear()

  // --- Phase 3: Role-Specific Experiment Strategies ---

  /**
   * Opening Portrait experiments.
   */
  def runOpeningPortrait(
      position: chess.Position,
      features: FeatureExtractor.PositionFeatures,
      maxExperiments: Int = 3
  ): Future[List[ExperimentResult]] =
    // For opening: prioritize central breaks, development moves
    val allCandidates = MoveGenerator.generateCandidates(position, features)
    val openingCandidates = allCandidates.filter { c =>
      c.candidateType == MoveGenerator.CandidateType.CentralBreak ||
      c.candidateType == MoveGenerator.CandidateType.PieceImprovement ||
      c.candidateType == MoveGenerator.CandidateType.EngineBest ||
      c.candidateType == MoveGenerator.CandidateType.EngineSecond
    }
    runSelectedCandidates(position, openingCandidates, ExperimentType.OpeningStats, maxExperiments, depth = 8)

  /**
   * Turning Point experiments.
   */
  def runTurningPointAnalysis(
      fenBefore: String,
      fenAfter: String,
      playedMove: String,
      depth: Int = 14
  ): Future[TurningPointAnalysis] =
    for
      beforeEval <- run(ExperimentType.TurningPointVerification, fenBefore, None, depth, multiPv = 3)
      afterEval <- run(ExperimentType.TurningPointVerification, fenAfter, None, depth, multiPv = 1)
      playedEval <- run(ExperimentType.TurningPointVerification, fenBefore, Some(playedMove), depth, multiPv = 1, forcedMoves = List(playedMove))
    yield TurningPointAnalysis(
      evalBefore = beforeEval.eval,
      evalAfter = afterEval.eval,
      playedMoveEval = playedEval.eval,
      bestMove = beforeEval.eval.lines.headOption.map(_.move),
      deltaFromBest = beforeEval.eval.lines.headOption.map { best =>
        val bestCp = best.cp.getOrElse(0)
        val playedCp = playedEval.eval.lines.headOption.flatMap(_.cp).getOrElse(0)
        bestCp - playedCp
      }
    )

  /**
   * Endgame experiments.
   */
  def runEndgameExperiments(
      position: chess.Position,
      features: FeatureExtractor.PositionFeatures,
      maxExperiments: Int = 4
  ): Future[List[ExperimentResult]] =
    val allCandidates = MoveGenerator.generateCandidates(position, features)
    
    val legalMoves = position.legalMoves.toList
    val kingMoves = legalMoves.filter(_.piece.role == chess.King)
    val pawnMoves = legalMoves.filter(_.piece.role == chess.Pawn)
    
    val endgameCandidates = allCandidates.take(maxExperiments / 2) ++
      kingMoves.take(2).map(m => MoveGenerator.CandidateMove(m.toUci.uci, m.toUci.uci, MoveGenerator.CandidateType.PieceImprovement, 0.7, "King Activity")) ++
      pawnMoves.take(2).map(m => MoveGenerator.CandidateMove(m.toUci.uci, m.toUci.uci, MoveGenerator.CandidateType.PieceImprovement, 0.6, "Pawn Advance"))
    
    runSelectedCandidates(position, endgameCandidates.distinctBy(_.uci), ExperimentType.EndgameCheck, maxExperiments, depth = 12)

  /**
   * Tactical experiments.
   */
  def runTacticalExperiments(
      position: chess.Position,
      features: FeatureExtractor.PositionFeatures,
      maxExperiments: Int = 5
  ): Future[List[ExperimentResult]] =
    val allCandidates = MoveGenerator.generateCandidates(position, features)
    val tacticalCandidates = allCandidates.filter { c =>
      c.candidateType == MoveGenerator.CandidateType.Fork ||
      c.candidateType == MoveGenerator.CandidateType.Pin ||
      c.candidateType == MoveGenerator.CandidateType.Skewer ||
      c.candidateType == MoveGenerator.CandidateType.DiscoveredAttack ||
      c.candidateType == MoveGenerator.CandidateType.SacrificeProbe ||
      c.candidateType == MoveGenerator.CandidateType.TacticalCheck
    }
    runSelectedCandidates(position, tacticalCandidates, ExperimentType.TacticalCheck, maxExperiments, depth = 12)

  // --- Helper Methods ---

  private def runSelectedCandidates(
      position: chess.Position,
      candidates: List[MoveGenerator.CandidateMove],
      expType: ExperimentType,
      maxExperiments: Int,
      depth: Int
  ): Future[List[ExperimentResult]] =
    val fen = Fen.write(position, chess.FullMoveNumber(1)).value
    val selected = candidates.distinctBy(_.uci).sortBy(-_.priority).take(maxExperiments)
    
    Future.sequence(
      selected.map { candidate =>
        run(
          expType = expType,
          fen = fen,
          move = Some(candidate.uci),
          depth = depth,
          multiPv = 1,
          forcedMoves = List(candidate.uci)
        ).map(res => res.copy(metadata = res.metadata + ("candidateType" -> candidate.candidateType.toString)))
      }
    )

// Result type for TurningPoint analysis
case class TurningPointAnalysis(
    evalBefore: AnalysisModel.EngineEval,
    evalAfter: AnalysisModel.EngineEval,
    playedMoveEval: AnalysisModel.EngineEval,
    bestMove: Option[String],
    deltaFromBest: Option[Int]  // CP difference: best - played (positive = mistake)
)

