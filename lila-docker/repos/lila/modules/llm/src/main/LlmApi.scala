package lila.llm

import play.api.libs.json.*
import scala.concurrent.{ ExecutionContext, Future }
import lila.llm.model.ProbeResult
import lila.llm.analysis.{
  ProbeOrchestrator, 
  ProbeOrchestratorConfig,
  InitialCommentaryResult,
  RefinedCommentaryResult
}

/** High-level API for LLM commentary features */
final class LlmApi(client: LlmClient)(using ec: ExecutionContext):

  // P2: ProbeOrchestrator for 2-call flow
  private val orchestrator = new ProbeOrchestrator(client)

  def commentPosition(req: CommentRequest): Future[Option[CommentResponse]] = 
    client.commentPosition(req)

  def analyzeGame(req: FullAnalysisRequest): Future[Option[FullAnalysisResponse]] = 
    client.analyzeGame(req)

  /** Generate commentary for a single position (real-time analysis) */
  def commentPosition(
      fen: String,
      lastMove: Option[String],
      eval: Option[EvalData],
      opening: Option[String],
      phase: String,
      ply: Int
  ): Future[Option[CommentResponse]] =
    client.commentPosition(CommentRequest(
      fen = fen,
      lastMove = lastMove,
      eval = eval,
      context = PositionContext(opening, phase, ply)
    ))

  // ============================================================
  // P2: 2-CALL PROBE LOOP ENDPOINTS
  // ============================================================

  /**
   * P2 Phase 1: Get initial commentary with probe requests.
   * Returns draft immediately + list of probes for client to execute.
   */
  def commentWithProbes(req: CommentRequest): Future[InitialCommentaryResult] =
    orchestrator.getInitialCommentary(req)

  /**
   * P2 Phase 2: Refine commentary with probe results.
   * Uses FutureSnapshot for accurate PVDelta.
   * Falls back to cached draft if refinement fails.
   */
  def refineWithProbes(req: CommentRequest, probeResults: List[ProbeResult]): Future[RefinedCommentaryResult] =
    orchestrator.refineWithProbes(req, probeResults)

  /**
   * P2: Get cached draft (for timeout scenarios).
   */
  def getCachedDraft(fen: String): Option[analysis.CachedDraft] =
    orchestrator.getCachedDraft(fen)

  // ============================================================
  // EXISTING ENDPOINTS
  // ============================================================

  /** Generate full game analysis with annotations */
  def analyzeFullGame(
      pgn: String,
      evals: List[MoveEval],
      style: String = "book",
      focusOn: List[String] = List("mistakes", "turning_points")
  ): Future[Option[FullAnalysisResponse]] =
    client.analyzeGame(FullAnalysisRequest(
      pgn = pgn,
      evals = evals,
      options = AnalysisOptions(style, focusOn)
    ))

  def analyzeGameLocal(req: FullAnalysisRequest): Future[Option[FullAnalysisResponse]] =
    client.analyzeGameLocal(req)

  /** Quick comment for a critical moment */
  def criticalMomentComment(
      fen: String,
      move: String,
      eval: EvalData,
      prevEval: Option[EvalData],
      phase: String
  ): Future[Option[String]] =
    val evalDiff = prevEval.map(p => eval.cp - p.cp).getOrElse(0)
    val momentType = 
      if evalDiff.abs > 200 then "blunder"
      else if evalDiff.abs > 100 then "mistake"
      else if evalDiff.abs > 50 then "inaccuracy"
      else "normal"

    if momentType == "normal" then Future.successful(None)
    else
      commentPosition(
        fen = fen,
        lastMove = Some(move),
        eval = Some(eval),
        opening = None,
        phase = phase,
        ply = 0
      ).map(_.map(_.commentary))

