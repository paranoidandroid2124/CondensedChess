package lila.beta

import java.time.Instant

import reactivemongo.api.bson.*

import lila.core.email.EmailAddress
import lila.core.user.UserApi
import lila.core.userId.UserId
import lila.db.dsl.*

final class BetaFeedbackApi(
    feedbackEventColl: Coll,
    waitlistLeadColl: Coll,
    userApi: UserApi
)(using Executor):

  import BetaFeedbackApi.*

  def submit(
      userId: Option[UserId],
      submission: Submission
  ): Fu[SubmitResult] =
    val normalized = submission.normalize
    resolveWaitlistEmail(userId, normalized).flatMap: waitlistEmail =>
      val now = Instant.now()
      val eventDoc = BSONDocument(
        "surface" -> normalized.surface,
        "feature" -> normalized.feature,
        "entrypoint" -> normalized.entrypoint,
        "willingness" -> normalized.willingness.key,
        "priceBand" -> normalized.priceBand.map(_.key),
        "notify" -> normalized.notifyRequested,
        "email" -> waitlistEmail.map(_.value),
        "notes" -> normalized.notes,
        "userId" -> userId.map(_.value),
        "createdAt" -> now
      )
      feedbackEventColl.insert.one(eventDoc).void >>
        storeLead(userId, normalized, waitlistEmail, now)

  private def resolveWaitlistEmail(
    userId: Option[UserId],
    submission: Submission
  ): Fu[Option[EmailAddress]] =
    submission.email match
      case Some(email) => fuccess(email.some)
      case None if submission.notifyRequested =>
        userId.fold(fuccess(none[EmailAddress]))(userApi.email)
      case None => fuccess(none[EmailAddress])

  private def storeLead(
      userId: Option[UserId],
      submission: Submission,
      resolvedEmail: Option[EmailAddress],
      now: Instant
  ): Fu[SubmitResult] =
    if !submission.notifyRequested then
      fuccess(SubmitResult(waitlist = WaitlistState.NotRequested, storedEmail = none))
    else
      leadKey(userId, resolvedEmail) match
        case None =>
          fuccess(SubmitResult(waitlist = WaitlistState.NeedsEmail, storedEmail = none))
        case Some(key) =>
          val selector = $id(key)
          val update = $set(
            "userId" -> userId.map(_.value),
            "email" -> resolvedEmail.map(_.value),
            "surface" -> submission.surface,
            "feature" -> submission.feature,
            "entrypoint" -> submission.entrypoint,
            "willingness" -> submission.willingness.key,
            "priceBand" -> submission.priceBand.map(_.key),
            "notes" -> submission.notes,
            "updatedAt" -> now
          ) ++ $doc(
            "$setOnInsert" -> $doc(
              "_id" -> key,
              "createdAt" -> now
            )
          )
          waitlistLeadColl.update.one(selector, update, upsert = true).map: _ =>
            SubmitResult(waitlist = WaitlistState.Enrolled, storedEmail = resolvedEmail)

  private def leadKey(userId: Option[UserId], email: Option[EmailAddress]): Option[String] =
    email.map(e => s"email:${e.normalize.value}") orElse userId.map(u => s"user:${u.value}")

object BetaFeedbackApi:

  enum Willingness(val key: String):
    case WouldPay extends Willingness("would_pay")
    case Maybe extends Willingness("maybe")
    case NotNow extends Willingness("not_now")

  object Willingness:
    val default = Maybe
    def from(value: String): Option[Willingness] = values.find(_.key == value.trim.toLowerCase)
    val keys = values.map(_.key).toSet

  enum PriceBand(val key: String):
    case UnderFive extends PriceBand("under_5")
    case FiveToTen extends PriceBand("5_10")
    case TenToTwenty extends PriceBand("10_20")
    case TwentyPlus extends PriceBand("20_plus")

  object PriceBand:
    def from(value: String): Option[PriceBand] = values.find(_.key == value.trim.toLowerCase)
    val keys = values.map(_.key).toSet

  enum WaitlistState(val key: String):
    case NotRequested extends WaitlistState("not_requested")
    case Enrolled extends WaitlistState("enrolled")
    case NeedsEmail extends WaitlistState("needs_email")

  final case class Submission(
      surface: String,
      feature: String,
      entrypoint: String,
      willingness: Willingness,
      priceBand: Option[PriceBand],
      notifyRequested: Boolean,
      email: Option[EmailAddress],
      notes: Option[String]
  ):
    def normalize: Submission =
      copy(
        surface = sanitize(surface, fallback = "general"),
        feature = sanitize(feature, fallback = "general"),
        entrypoint = sanitize(entrypoint, fallback = "direct"),
        notes = notes.map(_.trim).filter(_.nonEmpty).map(_.take(800))
      )

  final case class SubmitResult(
      waitlist: WaitlistState,
      storedEmail: Option[EmailAddress]
  )

  private def sanitize(value: String, fallback: String): String =
    value.trim.toLowerCase
      .replaceAll("[^a-z0-9_-]+", "_")
      .stripPrefix("_")
      .stripSuffix("_")
      .take(48) match
      case "" => fallback
      case s  => s
