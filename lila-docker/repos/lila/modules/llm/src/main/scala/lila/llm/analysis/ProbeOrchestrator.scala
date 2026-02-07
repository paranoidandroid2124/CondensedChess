package lila.llm.analysis

import scala.concurrent.{ ExecutionContext, Future }
import java.util.concurrent.ConcurrentHashMap
import lila.llm.model._
import lila.llm.{ CommentRequest, CommentResponse }

/**
 * P2: Orchestrates the 2-call Probe loop for production UX.
 * 
 * Flow:
 * 1. getInitialCommentary(req) → (DraftResponse, ProbeRequests) [immediate]
 * 2. refineWithProbes(req, probeResults) → FinalResponse [after client completes probes]
 * 
 * Features:
 * - In-memory cache for draft commentary
 * - Timeout fallback (returns draft if probes don't arrive)
 * - Graceful degradation when probes fail
 */
final class ProbeOrchestrator(
    llmClient: lila.llm.LlmClient,
    config: ProbeOrchestratorConfig = ProbeOrchestratorConfig()
)(using ec: ExecutionContext):

  // In-memory cache: FEN → (Draft, ProbeRequests, timestamp)
  private val draftCache = new ConcurrentHashMap[String, CachedDraft]()

  /**
   * Phase 1: Generate initial commentary immediately (without waiting for probes).
   * Returns draft commentary + list of probe requests for the client.
   */
  def getInitialCommentary(req: CommentRequest): Future[InitialCommentaryResult] =
    llmClient.commentPosition(req).map {
      case Some(response) =>
        val probeRequests = response.probeRequests
        val cached = CachedDraft(
          draft = response,
          probeRequests = probeRequests,
          timestamp = System.currentTimeMillis()
        )
        draftCache.put(req.fen, cached)
        cleanupOldEntries()
        
        InitialCommentaryResult(
          draft = response,
          probeRequests = probeRequests,
          stage = CommentaryStage.Draft
        )
      case None =>
        InitialCommentaryResult(
          draft = emptyResponse,
          probeRequests = Nil,
          stage = CommentaryStage.Failed
        )
    }.recover { case e: Throwable =>
      lila.log("llm").warn(s"ProbeOrchestrator.getInitialCommentary failed: ${e.getMessage}")
      InitialCommentaryResult(
        draft = emptyResponse,
        probeRequests = Nil,
        stage = CommentaryStage.Failed
      )
    }

  /**
   * Phase 2: Refine commentary with probe results.
   * Uses FutureSnapshot from probes for accurate PVDelta.
   * Falls back to cached draft if refinement fails.
   */
  def refineWithProbes(req: CommentRequest, probeResults: List[ProbeResult]): Future[RefinedCommentaryResult] =
    val cachedOpt = Option(draftCache.get(req.fen))
    
    if (probeResults.isEmpty) {
      // No probes completed - return cached draft
      Future.successful(RefinedCommentaryResult(
        response = cachedOpt.map(_.draft).getOrElse(emptyResponse),
        stage = CommentaryStage.Draft,
        probeStatus = ProbeStatus.Timeout
      ))
    } else {
      llmClient.commentPositionRefined(req, probeResults).map {
        case Some(refined) =>
          draftCache.remove(req.fen) // Clear cache after successful refinement
          RefinedCommentaryResult(
            response = refined,
            stage = CommentaryStage.Refined,
            probeStatus = ProbeStatus.Completed
          )
        case None =>
          // Refinement failed - fallback to draft
          RefinedCommentaryResult(
            response = cachedOpt.map(_.draft).getOrElse(emptyResponse),
            stage = CommentaryStage.Draft,
            probeStatus = ProbeStatus.Failed
          )
      }.recover { case e: Throwable =>
        lila.log("llm").warn(s"ProbeOrchestrator.refineWithProbes failed: ${e.getMessage}")
        RefinedCommentaryResult(
          response = cachedOpt.map(_.draft).getOrElse(emptyResponse),
          stage = CommentaryStage.Draft,
          probeStatus = ProbeStatus.Failed
        )
      }
    }

  /**
   * Get cached draft if available (for timeout scenarios).
   */
  def getCachedDraft(fen: String): Option[CachedDraft] =
    Option(draftCache.get(fen)).filter { cached =>
      System.currentTimeMillis() - cached.timestamp < config.cacheTtlMs
    }

  /**
   * Check probe status for a FEN.
   */
  def getProbeStatus(fen: String): ProbeStatus =
    getCachedDraft(fen) match {
      case Some(_) => ProbeStatus.Pending
      case None    => ProbeStatus.Unknown
    }

  private def cleanupOldEntries(): Unit =
    val now = System.currentTimeMillis()
    val keysToRemove = scala.collection.mutable.ListBuffer[String]()
    draftCache.forEach { (k, v) =>
      if (now - v.timestamp > config.cacheTtlMs) keysToRemove += k
    }
    keysToRemove.foreach(draftCache.remove)

  private val emptyResponse = CommentResponse(
    commentary = "Analysis pending...",
    concepts = Nil,
    variations = Nil,
    probeRequests = Nil
  )

/**
 * Configuration for ProbeOrchestrator.
 */
case class ProbeOrchestratorConfig(
  cacheTtlMs: Long = 60000,      // 60 seconds cache TTL
  probeTimeoutMs: Long = 5000,   // 5 seconds probe timeout
  maxCacheSize: Int = 100        // Max cached drafts
)

/**
 * Cached draft commentary with metadata.
 */
case class CachedDraft(
  draft: CommentResponse,
  probeRequests: List[ProbeRequest],
  timestamp: Long
)

/**
 * Result of Phase 1 (initial commentary).
 */
case class InitialCommentaryResult(
  draft: CommentResponse,
  probeRequests: List[ProbeRequest],
  stage: CommentaryStage
)

/**
 * Result of Phase 2 (refined commentary).
 */
case class RefinedCommentaryResult(
  response: CommentResponse,
  stage: CommentaryStage,
  probeStatus: ProbeStatus
)

/**
 * Commentary pipeline stage.
 */
enum CommentaryStage:
  case Draft, Refined, Failed

/**
 * Probe execution status.
 */
enum ProbeStatus:
  case Pending, Completed, Timeout, Failed, Unknown
