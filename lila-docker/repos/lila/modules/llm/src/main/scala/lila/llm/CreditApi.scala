package lila.llm

import reactivemongo.api.bson.*
import lila.db.dsl.{ *, given }
import java.time.{ Duration, Instant }

/** MongoDB-backed credit tracking for LLM analysis features.
  *
  * Schema (collection: llm_credits):
  * {{{
  *   {
  *     _id: "userId",
  *     credits: 150,          // remaining credits this period
  *     maxCredits: 150,       // tier-based maximum
  *     tier: "free",          // "free" | "pro"
  *     resetAt: ISODate(),    // next reset timestamp
  *     totalUsed: 0,          // lifetime usage (analytics)
  *     updatedAt: ISODate()
  *   }
  * }}}
  *
  * Design: lazy reset — credits refresh on next access after `resetAt`,
  * not via background cron. Atomic deduction uses `$inc` with `$gte` guard.
  */
final class CreditApi(coll: Coll)(using Executor):

  import CreditApi.*

  private val logger = lila.log("llm.credit")

  /** Get user's credit state, creating default entry if absent.
    * Automatically resets credits if past the reset date.
    */
  def getOrCreate(userId: String): Fu[CreditEntry] =
    coll
      .one[CreditEntry]($id(userId))
      .flatMap:
        case Some(entry) if entry.resetAt.isAfter(nowInstant) =>
          fuccess(entry)
        case Some(entry) =>
          // Credits have expired — reset them
          val reset = entry.copy(
            credits = entry.maxCredits,
            resetAt = nextResetAt,
            updatedAt = nowInstant
          )
          coll.update.one($id(userId), resetBson(reset), upsert = true).inject(reset)
        case None =>
          val entry = defaultEntry(userId)
          coll.insert.one(entryBson(entry)).inject(entry)

  /** Atomically deduct credits. Returns Right(remaining) or Left(error).
    * Thread-safe via MongoDB's atomic `$inc` with `$gte` precondition.
    */
  def deduct(userId: String, cost: Int): Fu[Either[InsufficientCredits, Int]] =
    if cost < 1 then fuccess(Right(Int.MaxValue))
    else
      getOrCreate(userId).flatMap { entry =>
        if entry.credits < cost then
          fuccess(Left(InsufficientCredits(
            remaining = entry.credits,
            required = cost,
            resetAt = entry.resetAt
          )))
        else
          coll.update
            .one(
              $id(userId) ++ $doc("credits" -> $gte(cost)),
              $inc("credits" -> -cost, "totalUsed" -> cost) ++
                $set("updatedAt" -> nowInstant)
            )
            .map { result =>
              if result.nModified > 0 then Right(entry.credits - cost)
              else
                // Race condition: another request consumed credits between check and update
                Left(InsufficientCredits(
                  remaining = 0,
                  required = cost,
                  resetAt = entry.resetAt
                ))
            }
      }

  /** Query remaining credits for UI display. */
  def remaining(userId: String): Fu[CreditStatus] =
    getOrCreate(userId).map { entry =>
      CreditStatus(
        remaining = entry.credits,
        maxCredits = entry.maxCredits,
        tier = entry.tier,
        resetAt = entry.resetAt
      )
    }

  /** Upgrade (or downgrade) a user's tier. Adjusts maxCredits immediately. */
  def upgradeTier(userId: String, tier: CreditConfig.Tier): Fu[Unit] =
    getOrCreate(userId).flatMap { entry =>
      val updated = entry.copy(
        tier = tier.key,
        maxCredits = tier.maxCredits,
        credits = Math.min(entry.credits + (tier.maxCredits - entry.maxCredits), tier.maxCredits),
        updatedAt = nowInstant
      )
      coll.update.one($id(userId), entryBson(updated), upsert = true).void
    }
  private def nextResetAt: Instant =
    nowInstant.plus(Duration.ofDays(CreditConfig.ResetIntervalDays.toLong))

  private def defaultEntry(userId: String): CreditEntry =
    val tier = CreditConfig.DefaultTier
    CreditEntry(
      _id = userId,
      credits = tier.maxCredits,
      maxCredits = tier.maxCredits,
      tier = tier.key,
      resetAt = nextResetAt,
      totalUsed = 0,
      updatedAt = nowInstant
    )

  private def entryBson(e: CreditEntry): BSONDocument =
    $doc(
      "_id"        -> e._id,
      "credits"    -> e.credits,
      "maxCredits" -> e.maxCredits,
      "tier"       -> e.tier,
      "resetAt"    -> e.resetAt,
      "totalUsed"  -> e.totalUsed,
      "updatedAt"  -> e.updatedAt
    )

  private def resetBson(e: CreditEntry): BSONDocument =
    entryBson(e)

object CreditApi:

  case class CreditEntry(
      _id: String,
      credits: Int,
      maxCredits: Int,
      tier: String,
      resetAt: Instant,
      totalUsed: Int,
      updatedAt: Instant
  )

  private given BSONDocumentHandler[CreditEntry] = Macros.handler[CreditEntry]

  /** Error returned when deduction fails. */
  case class InsufficientCredits(
      remaining: Int,
      required: Int,
      resetAt: Instant
  )

  /** Credit status for API responses / UI. */
  case class CreditStatus(
      remaining: Int,
      maxCredits: Int,
      tier: String,
      resetAt: Instant
  )

  object CreditStatus:
    import play.api.libs.json.*
    given Writes[CreditStatus] = Json.writes[CreditStatus]
