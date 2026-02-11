package lila.llm

import com.github.blemale.scaffeine.Cache
import scala.concurrent.duration.*

import lila.memo.CacheApi.*

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
      .expireAfterWrite(10.minutes)
      .maximumSize(2048)
      .build[String, CommentResponse]()

  /** Build cache key from position + move context. */
  private def cacheKey(fen: String, lastMove: Option[String]): String =
    s"$fen|${lastMove.getOrElse("-")}"

  /** Retrieve cached commentary if available. */
  def get(fen: String, lastMove: Option[String]): Option[CommentResponse] =
    cache.getIfPresent(cacheKey(fen, lastMove))

  /** Store commentary in cache. */
  def put(fen: String, lastMove: Option[String], response: CommentResponse): Unit =
    cache.put(cacheKey(fen, lastMove), response)

  /** Invalidate a specific entry. */
  def invalidate(fen: String, lastMove: Option[String]): Unit =
    cache.invalidate(cacheKey(fen, lastMove))

  /** Current cache statistics for monitoring. */
  def estimatedSize: Long = cache.estimatedSize()
