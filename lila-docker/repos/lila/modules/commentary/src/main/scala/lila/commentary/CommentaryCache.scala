package lila.commentary

import com.github.blemale.scaffeine.Cache
import scala.util.hashing.MurmurHash3

import lila.commentary.analysis.PlanStateTracker
import lila.commentary.model.{ FutureSnapshot, L1DeltaSnapshot, OpeningReference, ProbeResult, TargetsDelta }
import lila.commentary.model.strategic.EndgamePatternState

import lila.memo.CacheApi.*

case class CommentaryCacheContext(
    model: String,
    promptVersion: String,
    lang: String,
    planTier: String,
    commentaryMode: String,
    authorityFingerprint: String = ""
)

object CommentaryCache:
  private val HashSeed = 0x43686573

  private[commentary] final case class Key(value: String) extends AnyVal

  private[commentary] def compactHash(parts: Iterable[String]): String =
    java.lang.Integer.toUnsignedString(MurmurHash3.orderedHash(parts, HashSeed), Character.MAX_RADIX)

  private[commentary] def openingFingerprint(opening: Option[OpeningReference]): String =
    opening
      .map { ref =>
        val moveHash =
          compactHash(
            ref.topMoves.take(8).map(move =>
              compactHash(
                List(
                  move.uci,
                  move.san,
                  move.total.toString,
                  move.white.toString,
                  move.draws.toString,
                  move.black.toString,
                  move.performance.toString
                )
              )
            )
          )
        val gameHash =
          compactHash(
            ref.sampleGames.take(4).map(game =>
              compactHash(
                List(
                  game.id,
                  game.winner.map(_.toString).getOrElse(""),
                  game.white.name,
                  game.white.rating.toString,
                  game.black.name,
                  game.black.rating.toString,
                  game.year.toString,
                  game.month.toString,
                  game.event.getOrElse(""),
                  game.pgn.getOrElse("").take(160)
                )
              )
            )
          )
        compactHash(
          List(
            ref.eco.getOrElse(""),
            ref.name.getOrElse(""),
            ref.totalGames.toString,
            ref.description.getOrElse(""),
            moveHash,
            gameHash
          )
        )
      }
      .getOrElse("-")

/** Server-side Caffeine TTL cache for commentary responses.
  *
  * Provides 2nd-layer caching (after client-side Map in moveReview.ts).
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

  private def probeFingerprint(probeResults: List[ProbeResult]): Option[String] =
    Option.when(probeResults.nonEmpty) {
      val rowHashes = probeResults
        .sortBy(_.id)
        .map { pr =>
          val fen = pr.fen.getOrElse("")
          val move = pr.probedMove.getOrElse("")
          val purpose = pr.purpose.getOrElse("")
          val mate = pr.mate.map(_.toString).getOrElse("")
          val depth = pr.depth.map(_.toString).getOrElse("")
          val pv = pr.bestReplyPv.mkString(",")
          CommentaryCache.compactHash(
            List(
              pr.id,
              fen,
              move,
              purpose,
              pr.questionId.getOrElse(""),
              pr.questionKind.getOrElse(""),
              pr.evalCp.toString,
              pr.deltaVsBaseline.toString,
              mate,
              depth,
              pv,
              replyPvsFingerprint(pr),
              pr.keyMotifs.sorted.mkString(","),
              l1DeltaFingerprint(pr.l1Delta),
              futureSnapshotFingerprint(pr.futureSnapshot),
              pr.objective.getOrElse(""),
              pr.seedId.getOrElse(""),
              pr.requiredSignals.sorted.mkString(","),
              pr.generatedRequiredSignals.sorted.mkString(","),
              pr.motifInferenceMode.getOrElse(""),
              pr.candidateMove.getOrElse(""),
              pr.depthFloor.map(_.toString).getOrElse(""),
              pr.variationHash.getOrElse(""),
              pr.engineConfigFingerprint.getOrElse("")
            )
          )
        }
      CommentaryCache.compactHash(rowHashes)
    }

  private def replyPvsFingerprint(probe: ProbeResult): String =
    probe.replyPvs
      .map(lines => CommentaryCache.compactHash(lines.map(line => line.mkString(","))))
      .getOrElse("")

  private def l1DeltaFingerprint(delta: Option[L1DeltaSnapshot]): String =
    delta
      .map(d =>
        CommentaryCache.compactHash(
          List(
            d.materialDelta.toString,
            d.kingSafetyDelta.toString,
            d.centerControlDelta.toString,
            d.openFilesDelta.toString,
            d.mobilityDelta.toString,
            d.collapseReason.getOrElse("")
          )
        )
      )
      .getOrElse("")

  private def targetsDeltaFingerprint(delta: TargetsDelta): String =
    CommentaryCache.compactHash(
      List(
        delta.tacticalAdded.sorted.mkString(","),
        delta.tacticalRemoved.sorted.mkString(","),
        delta.strategicAdded.sorted.mkString(","),
        delta.strategicRemoved.sorted.mkString(",")
      )
    )

  private def futureSnapshotFingerprint(snapshot: Option[FutureSnapshot]): String =
    snapshot
      .map(s =>
        CommentaryCache.compactHash(
          List(
            s.resolvedThreatKinds.sorted.mkString(","),
            s.newThreatKinds.sorted.mkString(","),
            targetsDeltaFingerprint(s.targetsDelta),
            s.planBlockersRemoved.sorted.mkString(","),
            s.planPrereqsMet.sorted.mkString(",")
          )
        )
      )
      .getOrElse("")

  private def stateFingerprint(
      planStateToken: Option[PlanStateTracker],
      endgameStateToken: Option[EndgamePatternState]
  ): String =
    val pStr = planStateToken.map(_.build_fingerprint).getOrElse("n")
    val eStr = endgameStateToken.map(_.build_fingerprint).getOrElse("n")
    s"$pStr|$eStr"

  private def cacheKeyValue(
      fen: String,
      lastMove: Option[String],
      probeResults: List[ProbeResult],
      planStateToken: Option[PlanStateTracker],
      endgameStateToken: Option[EndgamePatternState],
      commentaryContext: Option[CommentaryCacheContext],
      openingFingerprint: String
  ): String =
    val probePart = probeFingerprint(probeResults).map(fp => s"probe:$fp").getOrElse("probe:-")
    val openingPart = Option(openingFingerprint).map(_.trim).filter(_.nonEmpty).getOrElse("-")
    val commentaryPart = commentaryContext
      .map { ctx =>
        s"commentary:${ctx.model}:${ctx.promptVersion}:${ctx.lang}:${ctx.planTier}:${ctx.commentaryMode}:${ctx.authorityFingerprint}"
      }
      .getOrElse("commentary:-")
    s"${baseKey(fen, lastMove)}|state:${stateFingerprint(planStateToken, endgameStateToken)}|opening:$openingPart|$probePart|$commentaryPart"

  private[commentary] def key(
      fen: String,
      lastMove: Option[String],
      probeResults: List[ProbeResult],
      planStateToken: Option[PlanStateTracker],
      endgameStateToken: Option[EndgamePatternState],
      commentaryContext: Option[CommentaryCacheContext],
      openingFingerprint: String = "-"
  ): CommentaryCache.Key =
    CommentaryCache.Key(cacheKeyValue(fen, lastMove, probeResults, planStateToken, endgameStateToken, commentaryContext, openingFingerprint))

  private[commentary] def get(key: CommentaryCache.Key): Option[CommentResponse] =
    cache.getIfPresent(key.value)

  private[commentary] def put(key: CommentaryCache.Key, response: CommentResponse): Unit =
    cache.put(key.value, response)

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
    get(fen, lastMove, probeResults, planStateToken, None, None)

  def get(
      fen: String,
      lastMove: Option[String],
      probeResults: List[ProbeResult],
      planStateToken: Option[PlanStateTracker],
      endgameStateToken: Option[EndgamePatternState]
  ): Option[CommentResponse] =
    get(fen, lastMove, probeResults, planStateToken, endgameStateToken, None)

  def get(
      fen: String,
      lastMove: Option[String],
      probeResults: List[ProbeResult],
      planStateToken: Option[PlanStateTracker],
      endgameStateToken: Option[EndgamePatternState],
      commentaryContext: Option[CommentaryCacheContext],
      openingFingerprint: String = "-"
  ): Option[CommentResponse] =
    get(key(fen, lastMove, probeResults, planStateToken, endgameStateToken, commentaryContext, openingFingerprint))

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
    put(fen, lastMove, response, probeResults, planStateToken, None, None)

  def put(
      fen: String,
      lastMove: Option[String],
      response: CommentResponse,
      probeResults: List[ProbeResult],
      planStateToken: Option[PlanStateTracker],
      endgameStateToken: Option[EndgamePatternState]
  ): Unit =
    put(fen, lastMove, response, probeResults, planStateToken, endgameStateToken, None)

  def put(
      fen: String,
      lastMove: Option[String],
      response: CommentResponse,
      probeResults: List[ProbeResult],
      planStateToken: Option[PlanStateTracker],
      endgameStateToken: Option[EndgamePatternState],
      commentaryContext: Option[CommentaryCacheContext],
      openingFingerprint: String = "-"
  ): Unit =
    put(key(fen, lastMove, probeResults, planStateToken, endgameStateToken, commentaryContext, openingFingerprint), response)

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
    invalidate(fen, lastMove, probeResults, planStateToken, None, None)

  def invalidate(
      fen: String,
      lastMove: Option[String],
      probeResults: List[ProbeResult],
      planStateToken: Option[PlanStateTracker],
      endgameStateToken: Option[EndgamePatternState]
  ): Unit =
    invalidate(fen, lastMove, probeResults, planStateToken, endgameStateToken, None)

  def invalidate(
      fen: String,
      lastMove: Option[String],
      probeResults: List[ProbeResult],
      planStateToken: Option[PlanStateTracker],
      endgameStateToken: Option[EndgamePatternState],
      commentaryContext: Option[CommentaryCacheContext],
      openingFingerprint: String = "-"
  ): Unit =
    cache.invalidate(key(fen, lastMove, probeResults, planStateToken, endgameStateToken, commentaryContext, openingFingerprint).value)

  /** Current cache statistics for monitoring. */
  def estimatedSize: Long = cache.estimatedSize()
