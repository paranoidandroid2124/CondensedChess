package lila.ops

import java.time.Instant

import scala.concurrent.Future
import scala.util.Try

import lila.core.config.BaseUrl
import lila.core.email.EmailAddress
import lila.core.id.SessionId
import lila.core.lilaism.Core.*
import lila.core.perm.Permission
import lila.core.user.{ BSONFields, RoleDbKey, User, UserTier }
import lila.core.userId.UserId
import lila.db.dsl.{ *, given }
import lila.mailer.AutomaticEmail
import lila.security.{ PasswordResetToken, SessionStore }
import lila.user.UserRepo
import play.api.mvc.RequestHeader
import reactivemongo.api.bson.*

final class OpsApi(
    userRepo: UserRepo,
    sessionStore: SessionStore,
    passwordResetToken: PasswordResetToken,
    automaticEmail: AutomaticEmail,
    auditColl: Coll,
    planLedgerColl: Coll,
    baseUrl: BaseUrl
)(using Executor):

  import lila.user.BSONHandlers.{ given BSONDocumentReader[User], given BSONHandler[EmailAddress], given BSONHandler[RoleDbKey], given BSONHandler[UserId], given BSONHandler[UserTier] }

  private val logger = lila.log("ops.member")
  private case class LoadedMember(doc: Bdoc, state: OpsMemberState)

  val pageSize = 20
  val recentAuditSize = 20
  val recentLedgerSize = 20

  def search(criteria: OpsMemberSearch): Fu[OpsMemberSearchPage] =
    val cleaned = criteria.cleaned
    val selector = searchSelector(cleaned)
    val skip = (cleaned.page - 1) * pageSize
    for
      total <- userRepo.coll.countSel(selector)
      users <- userRepo.coll
        .find(selector)
        .sort($doc(BSONFields.seenAt -> -1, BSONFields.createdAt -> -1))
        .skip(skip)
        .cursor[User]()
        .list(pageSize)
    yield OpsMemberSearchPage(
      search = cleaned,
      items = users.map(OpsMemberSummary.fromUser),
      total = total,
      pageSize = pageSize
    )

  def memberDetail(userId: UserId): Fu[Option[OpsMemberDetail]] =
    loadState(userId).flatMap:
      case None => Future.successful(None)
      case Some(member) =>
        for
          sessions <- sessionStore.openSessions(userId, 20)
          audit <- latestAudit(userId, recentAuditSize)
          ledger <- latestPlanLedger(userId, recentLedgerSize)
        yield Some(
          OpsMemberDetail(
            member = member,
            sessions = sessions,
            recentAudit = audit,
            recentPlanLedger = ledger,
            managedRoles = OpsRoles.split(member.roles)
          )
        )

  def auditPage(userId: UserId, page: Int, size: Int = 50): Fu[OpsAuditPage] =
    val normalizedPage = page.max(1)
    val normalizedSize = size.max(1).min(100)
    val selector = $doc("target" -> userId)
    for
      total <- auditColl.countSel(selector)
      entries <- auditColl
        .find(selector)
        .sort($doc("at" -> -1))
        .skip((normalizedPage - 1) * normalizedSize)
        .cursor[OpsMemberAuditEntry]()
        .list(normalizedSize)
    yield OpsAuditPage(userId, entries, normalizedPage, normalizedSize, total)

  def planLedger(userId: UserId, limit: Int = 100): Fu[List[UserPlanLedgerEntry]] =
    latestPlanLedger(userId, limit.max(1).min(200))

  def previewRawPermissions(userId: UserId, requestedRoles: Iterable[RoleDbKey]): Fu[Either[String, OpsPermissionPreview]] =
    loadMember(userId).map:
      case None => Left("Member not found.")
      case Some(member) =>
        val roles = normalizeRequestedRoles(requestedRoles)
        val before = OpsMemberSnapshot.fromState(member.state)
        val after = before.copy(roles = roles.map(_.value))
        Right(
          OpsPermissionPreview(
            target = member.state,
            beforeRoles = member.state.roles.sortBy(_.value),
            afterRoles = roles,
            diff = OpsMemberSnapshot.diff(before, after)
          )
        )

  def changeStatus(
      actor: UserId,
      target: UserId,
      enabled: Boolean,
      reason: String
  )(using req: RequestHeader): Fu[Either[String, Unit]] =
    loadMember(target).flatMap:
      case None => Future.successful(Left("Member not found."))
      case Some(before) =>
        val context = s"ops status change actor=${actor.value} target=${target.value}"
        val affectedSessionsFu =
          if enabled then Future.successful(Nil)
          else sessionStore.openSessions(target, 200).map(_.map(_._id))
        affectedSessionsFu.flatMap: affectedSessions =>
          val rollback =
            restoreUserFields(target, before.doc, List(BSONFields.enabled)) >>
              reopenSessions(affectedSessions)
          val perform =
            if enabled then userRepo.enable(target)
            else userRepo.disable(target).flatMap(_ => sessionStore.closeAllSessionsOf(target))
          perform
            .flatMap: _ =>
              loadMember(target).flatMap:
                case None =>
                  rollbackAfterFailure(
                    context = context,
                    failureMessage = "Status update was rolled back because the member could not be reloaded.",
                    rollbackFailureMessage = "Status update applied, but rollback failed after a consistency error. Check logs."
                  )(rollback)
                case Some(after) =>
                  appendAudit(
                    action = OpsMemberAction.StatusChanged,
                    actor = actor,
                    target = target,
                    reason = reason,
                    before = before.state,
                    after = after.state,
                    details = List(OpsDetailItem("enabled", enabled.toString))
                  ).map(_ => Right(())).recoverWith:
                    case err =>
                      rollbackAfterFailure(
                        context = context,
                        failureMessage = "Status update was rolled back because audit logging failed.",
                        rollbackFailureMessage = "Status update applied, but rollback failed after audit logging failed. Check logs.",
                        cause = err.some
                      )(rollback)
            .recoverWith:
              case err =>
                rollbackAfterFailure(
                  context = context,
                  failureMessage = "Failed to update the member status.",
                  rollbackFailureMessage = "Status update failed and rollback also failed. Check logs.",
                  cause = err.some
                )(rollback)

  def changeEmail(
      actor: UserId,
      target: UserId,
      email: EmailAddress,
      reason: String
  )(using req: RequestHeader): Fu[Either[String, Unit]] =
    val normalized = EmailAddress(email.normalize.value)
    loadMember(target).flatMap:
      case None => Future.successful(Left("Member not found."))
      case Some(before) if before.state.email.contains(normalized.value) =>
        Future.successful(Left("The member already uses this email address."))
      case Some(_) =>
        userRepo.byEmail(normalized).flatMap:
          case Some(existing) if existing.id != target =>
            Future.successful(Left("This email address is already in use."))
          case _ =>
            auditedUserUpdate(
              actor = actor,
              target = target,
              action = OpsMemberAction.EmailChanged,
              reason = reason,
              details = List(OpsDetailItem("email", normalized.value)),
              update = $set(BSONFields.email -> normalized),
              rollbackFields = List(BSONFields.email),
              writeFailureMessage = "Failed to update the member email.",
              completionFailureMessage = "Email update was rolled back because audit logging failed.",
              rollbackFailureMessage = "Email update applied, but rollback failed after audit logging failed. Check logs."
            )

  def sendPasswordReset(
      actor: UserId,
      target: UserId,
      reason: String
  )(using req: RequestHeader): Fu[Either[String, Unit]] =
    loadMember(target).flatMap:
      case None => Future.successful(Left("Member not found."))
      case Some(before) =>
        userRepo.byId(target).flatMap:
          case Some(user) =>
            user.email.fold[Fu[Either[String, Unit]]](Future.successful(Left("This member has no login email."))): email =>
              val token = passwordResetToken.generate(user.id, 30.minutes)
              val url = s"$baseUrl/auth/password-reset/$token"
              automaticEmail.passwordReset(user, email, url).flatMap { _ =>
                appendAudit(
                  action = OpsMemberAction.PasswordResetSent,
                  actor = actor,
                  target = target,
                  reason = reason,
                  before = before.state,
                  after = before.state,
                  details = List(
                    OpsDetailItem("email", email.value),
                    OpsDetailItem("delivery", "password_reset")
                  )
                ).map(_ => Right(())).recover(errorResult("Password reset email was sent, but audit logging failed."))
              }.recover(errorResult("Failed to send the password reset email."))
          case None => Future.successful(Left("Member not found."))

  def revokeAllSessions(
      actor: UserId,
      target: UserId,
      reason: String
  )(using req: RequestHeader): Fu[Either[String, Unit]] =
    loadMember(target).flatMap:
      case None => Future.successful(Left("Member not found."))
      case Some(before) =>
        sessionStore.openSessions(target, 200).flatMap: sessions =>
          val context = s"ops revoke all sessions actor=${actor.value} target=${target.value}"
          val rollback = reopenSessions(sessions.map(_._id))
          sessionStore.closeAllSessionsOf(target).flatMap: _ =>
            appendAudit(
              action = OpsMemberAction.SessionsRevoked,
              actor = actor,
              target = target,
              reason = reason,
              before = before.state,
              after = before.state,
              details = List(OpsDetailItem("revokedSessions", sessions.size.toString))
            ).map(_ => Right(())).recoverWith:
              case err =>
                rollbackAfterFailure(
                  context = context,
                  failureMessage = "Session revocation was rolled back because audit logging failed.",
                  rollbackFailureMessage = "Session revocation applied, but rollback failed after audit logging failed. Check logs.",
                  cause = err.some
                )(rollback)
          .recoverWith:
            case err =>
              rollbackAfterFailure(
                context = context,
                failureMessage = "Failed to revoke the member sessions.",
                rollbackFailureMessage = "Session revocation failed and rollback also failed. Check logs.",
                cause = err.some
              )(rollback)

  def revokeSession(
      actor: UserId,
      target: UserId,
      sessionId: SessionId,
      reason: String
  )(using req: RequestHeader): Fu[Either[String, Unit]] =
    loadMember(target).flatMap:
      case None => Future.successful(Left("Member not found."))
      case Some(before) =>
        sessionStore.openSessions(target, 200).flatMap: sessions =>
          sessions.find(_._id == sessionId).fold[Fu[Either[String, Unit]]](Future.successful(Left("Session not found."))): session =>
            val context = s"ops revoke session actor=${actor.value} target=${target.value} session=${sessionId.value}"
            val rollback = reopenSessions(List(sessionId))
            sessionStore.closeUserAndSessionId(target, sessionId).flatMap: _ =>
              appendAudit(
                action = OpsMemberAction.SessionRevoked,
                actor = actor,
                target = target,
                reason = reason,
                before = before.state,
                after = before.state,
                details = List(
                  OpsDetailItem("sessionId", sessionId.value),
                  OpsDetailItem("sessionIp", session.ip.value)
                )
              ).map(_ => Right(())).recoverWith:
                case err =>
                  rollbackAfterFailure(
                    context = context,
                    failureMessage = "Session revoke was rolled back because audit logging failed.",
                    rollbackFailureMessage = "Session revoke applied, but rollback failed after audit logging failed. Check logs.",
                    cause = err.some
                  )(rollback)
            .recoverWith:
              case err =>
                rollbackAfterFailure(
                  context = context,
                  failureMessage = "Failed to revoke the selected session.",
                  rollbackFailureMessage = "Session revoke failed and rollback also failed. Check logs.",
                  cause = err.some
                )(rollback)

  def clearTwoFactor(
      actor: UserId,
      target: UserId,
      reason: String
  )(using req: RequestHeader): Fu[Either[String, Unit]] =
    loadMember(target).flatMap:
      case None => Future.successful(Left("Member not found."))
      case Some(before) if !before.state.hasTwoFactor =>
        Future.successful(Left("Two-factor is not enabled for this member."))
      case Some(_) =>
        auditedUserUpdate(
          actor = actor,
          target = target,
          action = OpsMemberAction.TwoFactorCleared,
          reason = reason,
          details = List(OpsDetailItem("cleared", "totp")),
          update = $unset(BSONFields.totpSecret),
          rollbackFields = List(BSONFields.totpSecret),
          writeFailureMessage = "Failed to clear two-factor for the member.",
          completionFailureMessage = "Two-factor reset was rolled back because audit logging failed.",
          rollbackFailureMessage = "Two-factor reset applied, but rollback failed after audit logging failed. Check logs."
        )

  def updatePlan(
      actor: UserId,
      target: UserId,
      tier: UserTier,
      expiresAt: Option[Instant],
      reason: String
  )(using req: RequestHeader): Fu[Either[String, Unit]] =
    loadMember(target).flatMap:
      case None => Future.successful(Left("Member not found."))
      case Some(before) =>
        val normalizedExpiresAt = if tier == UserTier.Free then None else expiresAt
        val update = $set(
          BSONFields.tier -> tier,
          BSONFields.expiresAt -> normalizedExpiresAt
        ) ++ normalizedExpiresAt.fold($unset(BSONFields.expiresAt))(_ => $doc())
        val ledger = UserPlanLedgerEntry(
          _id = scalalib.SecureRandom.nextString(14),
          user = target,
          actor = actor,
          oldTier = before.state.tier,
          newTier = tier,
          oldExpiresAt = before.state.expiresAt,
          newExpiresAt = normalizedExpiresAt,
          reason = reason,
          source = "manual_ops",
          at = Instant.now()
        )
        val context = s"ops plan update actor=${actor.value} target=${target.value}"
        val rollback =
          planLedgerColl.delete.one($id(ledger._id)).void >>
            restoreUserFields(target, before.doc, List(BSONFields.tier, BSONFields.expiresAt))
        userRepo.coll.update.one($id(target), update).void.flatMap: _ =>
          loadMember(target).flatMap:
            case None =>
              rollbackAfterFailure(
                context = context,
                failureMessage = "Plan update was rolled back because the member could not be reloaded.",
                rollbackFailureMessage = "Plan update applied, but rollback failed after a consistency error. Check logs."
              )(rollback)
            case Some(after) =>
              (planLedgerColl.insert.one(ledger).void >>
                appendAudit(
                  action = OpsMemberAction.PlanUpdated,
                  actor = actor,
                  target = target,
                  reason = reason,
                  before = before.state,
                  after = after.state,
                  details = List(
                    OpsDetailItem("source", "manual_ops"),
                    OpsDetailItem("tier", tier.name)
                  )
                )).map(_ => Right(())).recoverWith:
                case err =>
                  rollbackAfterFailure(
                    context = context,
                    failureMessage = "Plan update was rolled back because ledger or audit persistence failed.",
                    rollbackFailureMessage = "Plan update applied, but rollback failed after ledger or audit persistence failed. Check logs.",
                    cause = err.some
                  )(rollback)
        .recoverWith:
          case err =>
            rollbackAfterFailure(
              context = context,
              failureMessage = "Failed to update the member plan.",
              rollbackFailureMessage = "Plan update failed and rollback also failed. Check logs.",
              cause = err.some
            )(rollback)

  def updateManagedRoles(
      actor: UserId,
      target: UserId,
      bundle: Option[RoleDbKey],
      toggles: Iterable[RoleDbKey],
      reason: String
  )(using req: RequestHeader): Fu[Either[String, Unit]] =
    loadMember(target).flatMap:
      case None => Future.successful(Left("Member not found."))
      case Some(before) =>
        val mergedRoles = OpsRoles.mergeManaged(before.state.roles, bundle, toggles)
        auditedUserUpdate(
          actor = actor,
          target = target,
          action = OpsMemberAction.RolesUpdated,
          reason = reason,
          details = List(
            OpsDetailItem("bundle", bundle.map(_.value).getOrElse("none")),
            OpsDetailItem("managedRoles", mergedRoles.map(_.value).mkString(", "))
          ),
          update = $set(BSONFields.roles -> mergedRoles.distinct.sortBy(_.value)),
          rollbackFields = List(BSONFields.roles),
          writeFailureMessage = "Failed to update the managed member roles.",
          completionFailureMessage = "Managed role update was rolled back because audit logging failed.",
          rollbackFailureMessage = "Managed role update applied, but rollback failed after audit logging failed. Check logs."
        )

  def updatePermissions(
      actor: UserId,
      target: UserId,
      requestedRoles: Iterable[RoleDbKey],
      reason: String
  )(using req: RequestHeader): Fu[Either[String, Unit]] =
    val normalized = normalizeRequestedRoles(requestedRoles)
    auditedUserUpdate(
      actor = actor,
      target = target,
      action = OpsMemberAction.PermissionsUpdated,
      reason = reason,
      details = List(OpsDetailItem("rawRoleCount", normalized.size.toString)),
      update = $set(BSONFields.roles -> normalized),
      rollbackFields = List(BSONFields.roles),
      writeFailureMessage = "Failed to update the raw permission set.",
      completionFailureMessage = "Raw permission update was rolled back because audit logging failed.",
      rollbackFailureMessage = "Raw permission update applied, but rollback failed after audit logging failed. Check logs."
    )

  private def latestAudit(userId: UserId, limit: Int): Fu[List[OpsMemberAuditEntry]] =
    auditColl
      .find($doc("target" -> userId))
      .sort($doc("at" -> -1))
      .cursor[OpsMemberAuditEntry]()
      .list(limit.max(1).min(200))

  private def latestPlanLedger(userId: UserId, limit: Int): Fu[List[UserPlanLedgerEntry]] =
    planLedgerColl
      .find($doc("user" -> userId))
      .sort($doc("at" -> -1))
      .cursor[UserPlanLedgerEntry]()
      .list(limit.max(1).min(200))

  private def searchSelector(criteria: OpsMemberSearch): Bdoc =
    val now = Instant.now()
    val filters = List.newBuilder[Bdoc]

    criteria.q.foreach: raw =>
      val term = raw.trim
      val escaped = java.util.regex.Pattern.quote(term)
      filters += $or(
        $doc("_id".$regex(s"(?i)^$escaped")),
        $doc(BSONFields.username.$regex(s"(?i)^$escaped"))
      )

    criteria.email.foreach: raw =>
      val normalized = EmailAddress(EmailAddress(raw).normalize.value)
      filters += $doc(BSONFields.email -> normalized)

    criteria.enabled.foreach: enabled =>
      filters += $doc(BSONFields.enabled -> enabled)

    criteria.tier.foreach: tier =>
      filters += $doc(BSONFields.tier -> tier)

    criteria.planState.foreach:
      case OpsPlanState.Free =>
        filters += $or(
          $doc(BSONFields.tier -> UserTier.Free),
          $doc(BSONFields.tier.$exists(false))
        )
      case OpsPlanState.Active =>
        filters += $doc(BSONFields.tier.$ne(UserTier.Free.name))
        filters += $or(
          $doc(BSONFields.expiresAt.$gt(now)),
          $doc(BSONFields.expiresAt.$exists(false))
        )
      case OpsPlanState.Expired =>
        filters += $doc(BSONFields.tier.$ne(UserTier.Free.name))
        filters += $doc(BSONFields.expiresAt.$lte(now))

    criteria.role.foreach: role =>
      filters += $doc(BSONFields.roles -> role)

    val createdRange = lila.db.dsl.dateBetween(BSONFields.createdAt, criteria.createdFrom, criteria.createdTo)
    if !createdRange.isEmpty then filters += createdRange

    val seenRange = lila.db.dsl.dateBetween(BSONFields.seenAt, criteria.seenFrom, criteria.seenTo)
    if !seenRange.isEmpty then filters += seenRange

    filters.result().foldLeft($empty)(_ ++ _)

  private def loadMember(userId: UserId): Fu[Option[LoadedMember]] =
    userRepo.coll.find($id(userId)).one[BSONDocument].map:
      _.flatMap: doc =>
        doc.asOpt[User].map: user =>
          val hasPassword = doc.getAsOpt[BSONBinary](BSONFields.bpass).isDefined
          LoadedMember(doc, OpsMemberState.fromUser(user, hasPassword))

  private def loadState(userId: UserId): Fu[Option[OpsMemberState]] =
    loadMember(userId).map(_.map(_.state))

  private def normalizeRequestedRoles(requestedRoles: Iterable[RoleDbKey]): List[RoleDbKey] =
    requestedRoles
      .filter(Permission.allByDbKey.contains)
      .toList
      .distinct
      .sortBy(_.value)

  private def auditedUserUpdate(
      actor: UserId,
      target: UserId,
      action: OpsMemberAction,
      reason: String,
      details: List[OpsDetailItem],
      update: Bdoc,
      rollbackFields: List[String],
      writeFailureMessage: String,
      completionFailureMessage: String,
      rollbackFailureMessage: String
  )(using req: RequestHeader): Fu[Either[String, Unit]] =
    loadMember(target).flatMap:
      case None => Future.successful(Left("Member not found."))
      case Some(before) =>
        val context = s"ops ${action.toString} actor=${actor.value} target=${target.value}"
        val rollback = restoreUserFields(target, before.doc, rollbackFields)
        userRepo.coll.update.one($id(target), update).void.flatMap: _ =>
          loadMember(target).flatMap:
            case None =>
              rollbackAfterFailure(
                context = context,
                failureMessage = "The member change was rolled back because the member could not be reloaded.",
                rollbackFailureMessage = rollbackFailureMessage
              )(rollback)
            case Some(after) =>
              appendAudit(action, actor, target, reason, before.state, after.state, details)
                .map(_ => Right(()))
                .recoverWith:
                  case err =>
                    rollbackAfterFailure(
                      context = context,
                      failureMessage = completionFailureMessage,
                      rollbackFailureMessage = rollbackFailureMessage,
                      cause = err.some
                    )(rollback)
        .recoverWith:
          case err =>
            rollbackAfterFailure(
              context = context,
              failureMessage = writeFailureMessage,
              rollbackFailureMessage = rollbackFailureMessage,
              cause = err.some
            )(rollback)

  private def restoreUserFields(target: UserId, before: Bdoc, fields: Iterable[String]): Funit =
    val rollback = OpsRollback.userFields(before, fields)
    if rollback.elements.isEmpty then Future.unit
    else userRepo.coll.update.one($id(target), rollback).void

  private def reopenSessions(sessionIds: Iterable[SessionId]): Funit =
    val ids = sessionIds.toList.distinct
    if ids.isEmpty then Future.unit
    else sessionStore.coll.update.one($inIds(ids), $set("up" -> true), multi = true).void

  private def rollbackAfterFailure(
      context: String,
      failureMessage: String,
      rollbackFailureMessage: String,
      cause: Option[Throwable] = None
  )(rollback: => Funit): Fu[Either[String, Unit]] =
    cause.foreach(err => logger.error(s"$context failed; attempting rollback", err))
    rollback.transform:
      case scala.util.Success(_) => scala.util.Success(Left(failureMessage))
      case scala.util.Failure(rollbackErr) =>
        logger.error(s"$context rollback failed", rollbackErr)
        scala.util.Success(Left(rollbackFailureMessage))

  private def appendAudit(
      action: OpsMemberAction,
      actor: UserId,
      target: UserId,
      reason: String,
      before: OpsMemberState,
      after: OpsMemberState,
      details: List[OpsDetailItem]
  )(using req: RequestHeader): Funit =
    val beforeSnapshot = OpsMemberSnapshot.fromState(before)
    val afterSnapshot = OpsMemberSnapshot.fromState(after)
    val entry = OpsMemberAuditEntry(
      _id = scalalib.SecureRandom.nextString(14),
      target = target,
      actor = actor,
      action = action,
      reason = reason,
      actorIp = remoteIp(req),
      actorUa = req.headers.get("User-Agent"),
      before = beforeSnapshot,
      after = afterSnapshot,
      diff = OpsMemberSnapshot.diff(beforeSnapshot, afterSnapshot),
      details = details,
      at = Instant.now()
    )
    auditColl.insert.one(entry).map(_ => ()).recoverWith:
      case err =>
        logger.error(
          s"ops audit insert failed action=${action.toString} actor=${actor.value} target=${target.value}",
          err
        )
        Future.failed(err)

  private def errorResult(message: String): PartialFunction[Throwable, Either[String, Unit]] =
    case err =>
      logger.error(message, err)
      Left(message)

  private def remoteIp(req: RequestHeader): Option[String] =
    Try(req.remoteAddress).toOption.map(_.trim).filter(_.nonEmpty)
