package lila.llm

import com.github.blemale.scaffeine.Cache
import java.security.MessageDigest

import lila.llm.analysis.PlanStateTracker
import lila.llm.model.ProbeResult
import play.api.libs.json.*

import lila.memo.CacheApi.*

case class LlmCacheContext(
    model: String,
    promptVersion: String,
    lang: String
)

/** Server-side Caffeine TTL cache for commentary responses.
  *
  * Provides 2nd-layer caching (after client-side Map in bookmaker.ts).
  * Particularly effective for opening positions (ply < 20) where FEN
  * reuse across users is high.
  *
  * Cache key = FEN + lastMove to ensure move-specific commentary.
  */
final class CommentaryCache(using Executor):

  private val cache: Cache[String, CommentResponse] =
    scaffeineNoScheduler
      .expireAfterWrite(60.minutes)
      .maximumSize(2048)
      .build[String, CommentResponse]()

  /** Build cache key from position + move context. */
  private def baseKey(fen: String, lastMove: Option[String]): String =
    s"$fen|${lastMove.getOrElse("-")}"

  private def sha1Hex(input: String): String =
    val md = MessageDigest.getInstance("SHA-1")
    md.digest(input.getBytes("UTF-8")).map("%02x".format(_)).mkString

  private def probeFingerprint(probeResults: List[ProbeResult]): Option[String] =
    Option.when(probeResults.nonEmpty) {
      val normalized = probeResults
        .sortBy(_.id)
        .map { pr =>
          val fen = pr.fen.getOrElse("")
          val move = pr.probedMove.getOrElse("")
          val purpose = pr.purpose.getOrElse("")
          val mate = pr.mate.map(_.toString).getOrElse("")
          val depth = pr.depth.map(_.toString).getOrElse("")
          val pv4 = pr.bestReplyPv.take(4).mkString(",")
          s"${pr.id}|$fen|$move|$purpose|${pr.evalCp}|$mate|$depth|$pv4"
        }
        .mkString("||")
      sha1Hex(normalized)
    }

  private def canonicalJson(value: JsValue): String =
    value match
      case JsObject(fields) =>
        fields.toSeq
          .sortBy(_._1)
          .map { case (k, v) => s""""$k":${canonicalJson(v)}""" }
          .mkString("{", ",", "}")
      case JsArray(values) =>
        values.map(canonicalJson).mkString("[", ",", "]")
      case other => Json.stringify(other)

  private def stateFingerprint(planStateToken: Option[PlanStateTracker]): String =
    planStateToken match
      case Some(token) =>
        sha1Hex(canonicalJson(Json.toJson(token)))
      case None => "-"

  private def cacheKey(
      fen: String,
      lastMove: Option[String],
      probeResults: List[ProbeResult],
      planStateToken: Option[PlanStateTracker],
      llmContext: Option[LlmCacheContext]
  ): String =
    val probePart = probeFingerprint(probeResults).map(fp => s"probe:$fp").getOrElse("probe:-")
    val llmPart = llmContext
      .map(ctx => s"llm:${ctx.model}:${ctx.promptVersion}:${ctx.lang}")
      .getOrElse("llm:-")
    s"${baseKey(fen, lastMove)}|state:${stateFingerprint(planStateToken)}|$probePart|$llmPart"

  /** Retrieve cached commentary if available. */
  def get(fen: String, lastMove: Option[String]): Option[CommentResponse] =
    get(fen, lastMove, Nil, None)

  /** Retrieve cached commentary with probe-aware keying (2-pass safe). */
  def get(fen: String, lastMove: Option[String], probeResults: List[ProbeResult]): Option[CommentResponse] =
    get(fen, lastMove, probeResults, None)

  def get(
      fen: String,
      lastMove: Option[String],
      probeResults: List[ProbeResult],
      planStateToken: Option[PlanStateTracker]
  ): Option[CommentResponse] =
    get(fen, lastMove, probeResults, planStateToken, None)

  def get(
      fen: String,
      lastMove: Option[String],
      probeResults: List[ProbeResult],
      planStateToken: Option[PlanStateTracker],
      llmContext: Option[LlmCacheContext]
  ): Option[CommentResponse] =
    cache.getIfPresent(cacheKey(fen, lastMove, probeResults, planStateToken, llmContext))

  /** Store commentary in cache. */
  def put(fen: String, lastMove: Option[String], response: CommentResponse): Unit =
    put(fen, lastMove, response, Nil, None)

  /** Store commentary with probe-aware keying (2-pass safe). */
  def put(fen: String, lastMove: Option[String], response: CommentResponse, probeResults: List[ProbeResult]): Unit =
    put(fen, lastMove, response, probeResults, None)

  def put(
      fen: String,
      lastMove: Option[String],
      response: CommentResponse,
      probeResults: List[ProbeResult],
      planStateToken: Option[PlanStateTracker]
  ): Unit =
    put(fen, lastMove, response, probeResults, planStateToken, None)

  def put(
      fen: String,
      lastMove: Option[String],
      response: CommentResponse,
      probeResults: List[ProbeResult],
      planStateToken: Option[PlanStateTracker],
      llmContext: Option[LlmCacheContext]
  ): Unit =
    cache.put(cacheKey(fen, lastMove, probeResults, planStateToken, llmContext), response)

  /** Invalidate a specific entry. */
  def invalidate(fen: String, lastMove: Option[String]): Unit =
    invalidate(fen, lastMove, Nil, None)

  /** Invalidate a specific probe-aware entry. */
  def invalidate(fen: String, lastMove: Option[String], probeResults: List[ProbeResult]): Unit =
    invalidate(fen, lastMove, probeResults, None)

  def invalidate(
      fen: String,
      lastMove: Option[String],
      probeResults: List[ProbeResult],
      planStateToken: Option[PlanStateTracker]
  ): Unit =
    invalidate(fen, lastMove, probeResults, planStateToken, None)

  def invalidate(
      fen: String,
      lastMove: Option[String],
      probeResults: List[ProbeResult],
      planStateToken: Option[PlanStateTracker],
      llmContext: Option[LlmCacheContext]
  ): Unit =
    cache.invalidate(cacheKey(fen, lastMove, probeResults, planStateToken, llmContext))

  /** Current cache statistics for monitoring. */
  def estimatedSize: Long = cache.estimatedSize()
