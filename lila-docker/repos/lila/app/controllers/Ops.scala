package controllers

import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.time.{ Instant, LocalDate, LocalDateTime, ZoneId }

import scala.util.Try

import play.api.data.*
import play.api.data.Forms.*
import play.api.libs.json.*
import play.api.mvc.Result

import lila.app.*
import lila.core.email.EmailAddress
import lila.core.id.SessionId
import lila.core.perm.Permission
import lila.core.user.{ RoleDbKey, UserTier }
import lila.core.userId.{ UserId, UserStr }
import lila.ops.{
  OpsMemberAuditEntry,
  OpsMemberSearch,
  OpsMemberSummary,
  OpsPermissionPreview,
  OpsPlanState,
  OpsRoles
}

final class Ops(env: lila.app.Env) extends LilaController(env):

  private val zoneId = ZoneId.systemDefault
  private val reasonField = nonEmptyText(minLength = 8, maxLength = 500)

  private case class StatusData(enabled: Boolean, reason: String)
  private case class EmailData(email: String, reason: String)
  private case class ReasonData(reason: String)
  private case class PlanData(tier: String, expiresAt: Option[String], reason: String)
  private case class ManagedRolesData(bundle: Option[String], toggles: List[String], reason: String)
  private case class PermissionsData(permissions: List[String], reason: String, confirm: Boolean)

  private val statusForm = Form(
    mapping("enabled" -> play.api.data.Forms.boolean, "reason" -> reasonField)(
      StatusData.apply
    )(data => Some((data.enabled, data.reason)))
  )
  private val emailForm = Form(
    mapping("email" -> nonEmptyText, "reason" -> reasonField)(
      EmailData.apply
    )(data => Some((data.email, data.reason)))
  )
  private val reasonForm = Form(single("reason" -> reasonField))
  private val planForm = Form(
    mapping(
      "tier" -> nonEmptyText,
      "expiresAt" -> optional(text),
      "reason" -> reasonField
    )(PlanData.apply)(data => Some((data.tier, data.expiresAt, data.reason)))
  )
  private val managedRolesForm = Form(
    mapping(
      "bundle" -> optional(text),
      "toggles" -> list(text),
      "reason" -> reasonField
    )(ManagedRolesData.apply)(data => Some((data.bundle, data.toggles, data.reason)))
  )
  private val permissionsForm = Form(
    mapping(
      "permissions" -> list(text),
      "reason" -> reasonField,
      "confirm" -> default(play.api.data.Forms.boolean, false)
    )(PermissionsData.apply)(data => Some((data.permissions, data.reason, data.confirm)))
  )

  def members = Secure(_.OpsMemberRead) { ctx ?=> _ ?=>
    env.ops.api.search(currentSearch).flatMap: page =>
      Ok.page(views.ops.members(page, pageNo => membersUrl(page.search.copy(page = pageNo))))
  }

  def member(id: UserStr, advanced: Boolean) = Secure(_.OpsMemberRead) { ctx ?=> me ?=>
    if advanced && !isGranted(_.OpsMemberAdvanced) then authorizationFailed
    else renderMemberPage(id.id, showAdvanced = advanced)
  }

  def audit(id: UserStr, page: Int) = Secure(_.OpsMemberRead) { ctx ?=> _ ?=>
    env.ops.api.memberDetail(id.id).flatMap:
      case None => notFound
      case Some(detail) =>
        env.ops.api.auditPage(id.id, page).flatMap: auditPage =>
          Ok.page(views.ops.audit(detail.member, auditPage))
  }

  def planLedger(id: UserStr) = Secure(_.OpsMemberRead) { ctx ?=> _ ?=>
    env.ops.api.memberDetail(id.id).flatMap:
      case None => notFound
      case Some(detail) =>
        env.ops.api.planLedger(id.id).flatMap: entries =>
          Ok.page(views.ops.planLedger(detail.member, entries))
  }

  def apiSearch = Secure(_.OpsMemberRead) { ctx ?=> _ ?=>
    env.ops.api.search(currentSearch).map: page =>
      JsonOk(
        Json.obj(
          "page" -> page.page,
          "pageSize" -> page.pageSize,
          "total" -> page.total,
          "totalPages" -> page.totalPages,
          "items" -> page.items.map(memberSummaryJson)
        )
      )
  }

  def apiAudit(id: UserStr, page: Int) = Secure(_.OpsMemberRead) { _ ?=> _ ?=>
    env.ops.api.auditPage(id.id, page).map: result =>
      JsonOk(
        Json.obj(
          "target" -> id.id.value,
          "page" -> result.page,
          "pageSize" -> result.pageSize,
          "total" -> result.total,
          "totalPages" -> result.totalPages,
          "items" -> result.entries.map(auditEntryJson)
        )
      )
  }

  def updateStatus(id: UserStr) = SecureBody(_.OpsMemberWrite) { ctx ?=> me ?=>
    bindForm(statusForm)(
      err => redirectError(routes.Ops.member(id), formError(err)),
      data => handleMutation(env.ops.api.changeStatus(me.userId, id.id, data.enabled, data.reason), routes.Ops.member(id), "Status updated.")
    )
  }

  def updateEmail(id: UserStr) = SecureBody(_.OpsMemberWrite) { ctx ?=> me ?=>
    bindForm(emailForm)(
      err => redirectError(routes.Ops.member(id), formError(err)),
      data =>
        if !EmailAddress.isValid(data.email) then
          redirectError(routes.Ops.member(id), "Enter a valid email address.")
        else
          handleMutation(
            env.ops.api.changeEmail(me.userId, id.id, EmailAddress(data.email), data.reason),
            routes.Ops.member(id),
            "Email updated."
          )
    )
  }

  def sendPasswordReset(id: UserStr) = SecureBody(_.OpsMemberWrite) { ctx ?=> me ?=>
    bindForm(reasonForm)(
      err => redirectError(routes.Ops.member(id), formError(err)),
      reason => handleMutation(env.ops.api.sendPasswordReset(me.userId, id.id, reason), routes.Ops.member(id), "Password reset email sent.")
    )
  }

  def revokeAllSessions(id: UserStr) = SecureBody(_.OpsMemberWrite) { ctx ?=> me ?=>
    bindForm(reasonForm)(
      err => redirectError(routes.Ops.member(id), formError(err)),
      reason => handleMutation(env.ops.api.revokeAllSessions(me.userId, id.id, reason), routes.Ops.member(id), "All active sessions revoked.")
    )
  }

  def revokeSession(id: UserStr, sid: String) = SecureBody(_.OpsMemberWrite) { ctx ?=> me ?=>
    bindForm(reasonForm)(
      err => redirectError(routes.Ops.member(id), formError(err)),
      reason =>
        handleMutation(
          env.ops.api.revokeSession(me.userId, id.id, SessionId(sid), reason),
          routes.Ops.member(id),
          "Session revoked."
        )
    )
  }

  def clearTwoFactor(id: UserStr) = SecureBody(_.OpsMemberWrite) { ctx ?=> me ?=>
    bindForm(reasonForm)(
      err => redirectError(routes.Ops.member(id), formError(err)),
      reason => handleMutation(env.ops.api.clearTwoFactor(me.userId, id.id, reason), routes.Ops.member(id), "Two-factor removed.")
    )
  }

  def updatePlan(id: UserStr) = SecureBody(_.OpsMemberWrite) { ctx ?=> me ?=>
    bindForm(planForm)(
      err => redirectError(routes.Ops.member(id), formError(err)),
      data =>
        parseTier(data.tier).fold(redirectError(routes.Ops.member(id), "Choose a valid tier.")): tier =>
          val expiresAt = data.expiresAt.flatMap(parseDateTime)
          if data.expiresAt.exists(_.trim.nonEmpty) && expiresAt.isEmpty then
            redirectError(routes.Ops.member(id), "Enter a valid expiration date and time.")
          else
            handleMutation(
              env.ops.api.updatePlan(me.userId, id.id, tier, expiresAt, data.reason),
              routes.Ops.member(id),
              "Plan updated."
            )
    )
  }

  def updateRoles(id: UserStr) = SecureBody(_.OpsMemberRoleGrant) { ctx ?=> me ?=>
    bindForm(managedRolesForm)(
      err => redirectError(routes.Ops.member(id), formError(err)),
      data =>
        val bundle = data.bundle.flatMap(parseManagedBundle)
        val toggles = data.toggles.flatMap(parseManagedToggle)
        handleMutation(
          env.ops.api.updateManagedRoles(me.userId, id.id, bundle, toggles, data.reason),
          routes.Ops.member(id),
          "Managed roles updated."
        )
    )
  }

  def updatePermissions(id: UserStr) = SecureBody(_.OpsMemberAdvanced) { ctx ?=> me ?=>
    bindForm(permissionsForm)(
      err => redirectError(routes.Ops.member(id, advanced = true), formError(err)),
      data =>
        val selected = data.permissions.flatMap(parseKnownRole)
        if !data.confirm then
          env.ops.api.previewRawPermissions(id.id, selected).flatMap:
            case Left(err) => redirectError(routes.Ops.member(id, advanced = true), err)
            case Right(preview) =>
              renderMemberPage(
                id.id,
                showAdvanced = true,
                permissionPreview = preview.some,
                selectedRawRoles = preview.afterRoles.toSet
              )
        else
          handleMutation(
            env.ops.api.updatePermissions(me.userId, id.id, selected, data.reason),
            routes.Ops.member(id, advanced = true),
            "Raw permissions updated."
          )
    )
  }

  private def renderMemberPage(
      id: UserId,
      showAdvanced: Boolean,
      permissionPreview: Option[OpsPermissionPreview] = None,
      selectedRawRoles: Set[RoleDbKey] = Set.empty
  )(using ctx: Context, me: lila.core.user.Me): Fu[Result] =
    env.ops.api.memberDetail(id).flatMap:
      case None => notFound
      case Some(detail) =>
        Ok.page(
          views.ops.member(
            detail = detail,
            showAdvanced = showAdvanced,
            canUseAdvanced = isGranted(_.OpsMemberAdvanced),
            permissionPreview = permissionPreview,
            selectedRawRoles =
              if selectedRawRoles.nonEmpty then selectedRawRoles
              else detail.member.roles.toSet,
            permissionCategories = lila.security.Permission.categorized
          )
        )

  private def handleMutation(
      result: Fu[Either[String, Unit]],
      redirectTo: play.api.mvc.Call,
      successMessage: String
  ): Fu[Result] =
    result.map:
      case Right(_) => Redirect(redirectTo).flashing("success" -> successMessage)
      case Left(err) => Redirect(redirectTo).flashing("error" -> err)

  private def currentSearch(using ctx: Context): OpsMemberSearch =
    OpsMemberSearch(
      q = ctx.req.getQueryString("q"),
      email = ctx.req.getQueryString("email"),
      enabled = ctx.req.getQueryString("enabled").flatMap(parseEnabled),
      tier = ctx.req.getQueryString("tier").flatMap(parseTier),
      planState = ctx.req.getQueryString("planState").flatMap(OpsPlanState.fromString),
      role = ctx.req.getQueryString("role").flatMap(parseKnownRole),
      createdFrom = ctx.req.getQueryString("createdFrom").flatMap(parseDateStart),
      createdTo = ctx.req.getQueryString("createdTo").flatMap(parseDateEndExclusive),
      seenFrom = ctx.req.getQueryString("seenFrom").flatMap(parseDateStart),
      seenTo = ctx.req.getQueryString("seenTo").flatMap(parseDateEndExclusive),
      page = ctx.req.getQueryString("page").flatMap(_.toIntOption).getOrElse(1)
    ).cleaned

  private def membersUrl(search: OpsMemberSearch, keepPage: Boolean = false)(using ctx: Context): String =
    val params = List(
      "q" -> search.q,
      "email" -> search.email,
      "enabled" -> search.enabled.map(_.toString),
      "tier" -> search.tier.map(_.name),
      "planState" -> search.planState.map(_.toString),
      "role" -> search.role.map(_.value),
      "createdFrom" -> rawDate("createdFrom"),
      "createdTo" -> rawDate("createdTo"),
      "seenFrom" -> rawDate("seenFrom"),
      "seenTo" -> rawDate("seenTo"),
      "page" -> Option.when(keepPage || search.page > 1)(search.page.toString)
    ).collect { case (key, Some(value)) => key -> value }
    val query =
      if params.isEmpty then ""
      else params.map { case (key, value) => s"${encode(key)}=${encode(value)}" }.mkString("?", "&", "")
    routes.Ops.members.url + query

  private def rawDate(name: String)(using ctx: Context): Option[String] =
    ctx.req.getQueryString(name).map(_.trim).filter(_.nonEmpty)

  private def parseEnabled(raw: String): Option[Boolean] =
    raw.trim.toLowerCase match
      case "true" | "enabled"  => Some(true)
      case "false" | "disabled" => Some(false)
      case _                    => None

  private def parseTier(raw: String): Option[UserTier] =
    UserTier.values.find(_.name.equalsIgnoreCase(raw.trim))

  private def parseKnownRole(raw: String): Option[RoleDbKey] =
    val dbKey = RoleDbKey(raw.trim)
    Option.when(Permission.allByDbKey.contains(dbKey))(dbKey)

  private def parseManagedBundle(raw: String): Option[RoleDbKey] =
    parseKnownRole(raw).filter(OpsRoles.opsBundles.contains)

  private def parseManagedToggle(raw: String): Option[RoleDbKey] =
    parseKnownRole(raw).filter(OpsRoles.managedToggles.contains)

  private def parseDateStart(raw: String): Option[Instant] =
    Try(LocalDate.parse(raw.trim).atStartOfDay(zoneId).toInstant).toOption

  private def parseDateEndExclusive(raw: String): Option[Instant] =
    Try(LocalDate.parse(raw.trim).plusDays(1).atStartOfDay(zoneId).toInstant).toOption

  private def parseDateTime(raw: String): Option[Instant] =
    Try(LocalDateTime.parse(raw.trim).atZone(zoneId).toInstant).toOption

  private def encode(value: String): String =
    URLEncoder.encode(value, StandardCharsets.UTF_8)

  private def formError(form: Form[?]): String =
    form.errors.headOption.fold("Please review the form inputs.")(_.message)

  private def redirectError(call: play.api.mvc.Call, message: String): Fu[Result] =
    fuccess(Redirect(call).flashing("error" -> message))

  private def memberSummaryJson(member: OpsMemberSummary): JsObject =
    Json.obj(
      "id" -> member.id.value,
      "username" -> member.username,
      "email" -> member.email,
      "enabled" -> member.enabled,
      "tier" -> member.tier.name,
      "expiresAt" -> member.expiresAt.map(_.toString),
      "roles" -> member.roles.map(_.value),
      "createdAt" -> member.createdAt.toString,
      "seenAt" -> member.seenAt.map(_.toString),
      "hasTwoFactor" -> member.hasTwoFactor,
      "planState" -> member.planState().toString
    )

  private def auditEntryJson(entry: OpsMemberAuditEntry): JsObject =
    Json.obj(
      "id" -> entry._id,
      "target" -> entry.target.value,
      "actor" -> entry.actor.value,
      "action" -> entry.action.toString,
      "reason" -> entry.reason,
      "actorIp" -> entry.actorIp,
      "actorUa" -> entry.actorUa,
      "diff" -> entry.diff.map: diff =>
        Json.obj(
          "field" -> diff.field,
          "before" -> diff.before,
          "after" -> diff.after
        ),
      "details" -> entry.details.map: detail =>
        Json.obj("key" -> detail.key, "value" -> detail.value),
      "at" -> entry.at.toString
    )
