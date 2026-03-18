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
  OpsMetricsSnapshot,
  OpsPermissionPreview,
  OpsPlanState,
  OpsRoles
}

final class Ops(env: lila.app.Env) extends LilaController(env):

  private val zoneId = ZoneId.systemDefault
  private val trimmedText = text.transform[String](_.trim, identity)
  private val optionalTrimmedText =
    optional(text.transform[String](_.trim, identity)).transform[Option[String]](_.filter(_.nonEmpty), identity)
  private val reasonField =
    trimmedText
      .verifying("Enter a reason.", _.nonEmpty)
      .verifying("Reason must be at least 8 characters.", _.length >= 8)
      .verifying("Reason must be 500 characters or fewer.", _.length <= 500)

  private case class StatusData(enabled: Boolean, reason: String)
  private case class EmailData(email: String, reason: String)
  private case class ReasonData(reason: String)
  private case class PlanData(tier: String, expiresAt: Option[String], reason: String)
  private case class ManagedRolesData(bundle: Option[String], toggles: List[String], reason: String)
  private case class PermissionsData(
      permissions: List[String],
      reason: String,
      confirm: Boolean,
      intent: String
  )

  private val statusForm = Form(
    mapping("enabled" -> play.api.data.Forms.boolean, "reason" -> reasonField)(
      StatusData.apply
    )(data => Some((data.enabled, data.reason)))
  )
  private val emailForm = Form(
    mapping("email" -> trimmedText.verifying("Enter an email address.", _.nonEmpty), "reason" -> reasonField)(
      EmailData.apply
    )(data => Some((data.email, data.reason)))
  )
  private val reasonForm = Form(single("reason" -> reasonField))
  private val planForm = Form(
    mapping(
      "tier" -> trimmedText.verifying("Choose a tier.", _.nonEmpty),
      "expiresAt" -> optionalTrimmedText,
      "reason" -> reasonField
    )(PlanData.apply)(data => Some((data.tier, data.expiresAt, data.reason)))
  )
  private val managedRolesForm = Form(
    mapping(
      "bundle" -> optionalTrimmedText,
      "toggles" -> list(text),
      "reason" -> reasonField
    )(ManagedRolesData.apply)(data => Some((data.bundle, data.toggles, data.reason)))
  )
  private val permissionsForm = Form(
    mapping(
      "permissions" -> list(text),
      "reason" -> reasonField,
      "confirm" -> default(play.api.data.Forms.boolean, false),
      "intent" -> default(trimmedText, "preview")
    )(PermissionsData.apply)(data => Some((data.permissions, data.reason, data.confirm, data.intent)))
  )

  def landing = Auth { ctx ?=> me ?=>
    val capabilities = currentCapabilities
    if capabilities.hasAnyAccess then Ok.page(views.ops.landing(capabilities))
    else authorizationFailed
  }

  def members = Secure(_.OpsMemberRead) { ctx ?=> _ ?=>
    val capabilities = currentCapabilities
    env.ops.api.search(currentSearch).flatMap: page =>
      val returnTo = membersUrl(page.search, keepPage = true)
      Ok.page(
        views.ops.members(
          page,
          pageNo => membersUrl(page.search.copy(page = pageNo), keepPage = true),
          id => memberUrl(id, returnTo = returnTo.some),
          capabilities
        )
      )
  }

  def member(id: UserStr, advanced: Boolean) = Secure(_.OpsMemberRead) { ctx ?=> me ?=>
    val capabilities = currentCapabilities
    renderMemberPage(
      id.id,
      showAdvanced = advanced && capabilities.canUseAdvanced,
      capabilities = capabilities,
      notice =
        Option.when(advanced && !capabilities.canUseAdvanced)(
          "Raw permission editor requires Ops member advanced. The member detail page is shown instead."
        )
    )
  }

  def audit(id: UserStr, page: Int) = Secure(_.OpsMemberRead) { ctx ?=> _ ?=>
    val capabilities = currentCapabilities
    env.ops.api.memberDetail(id.id).flatMap:
      case None => notFound
      case Some(detail) =>
        env.ops.api.auditPage(id.id, page).flatMap: auditPage =>
          Ok.page(views.ops.audit(detail.member, auditPage, currentReturnTo, capabilities))
  }

  def planLedger(id: UserStr) = Secure(_.OpsMemberRead) { ctx ?=> _ ?=>
    val capabilities = currentCapabilities
    env.ops.api.memberDetail(id.id).flatMap:
      case None => notFound
      case Some(detail) =>
        env.ops.api.planLedger(id.id).flatMap: entries =>
          Ok.page(views.ops.planLedger(detail.member, entries, currentReturnTo, capabilities))
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

  def metrics = Secure(_.OpsViewer) { ctx ?=> _ ?=>
    val capabilities = currentCapabilities
    env.openBetaBindings.snapshot.flatMap: bindings =>
      val metricsSnapshot = OpsMetricsSnapshot.fromScrape(lila.web.PrometheusReporter.latestScrapeData())
      Ok.page(views.ops.metrics(metricsSnapshot, bindings, capabilities))
  }

  def commentary(limit: Int) = Secure(_.OpsViewer) { ctx ?=> _ ?=>
    val capabilities = currentCapabilities
    val normalizedLimit = limit.max(1).min(50)
    val snapshot = env.llm.api.commentaryOpsSnapshot(normalizedLimit)
    Ok.page(views.ops.commentary(snapshot, normalizedLimit, capabilities))
  }

  def updateStatus(id: UserStr) = SecureBody(_.OpsMemberWrite) { ctx ?=> me ?=>
    val bound = statusForm.bindFromRequest()
    bound.fold(
      err =>
        renderMemberFormError(
          id.id,
          forms = memberForms(status = err),
          showAdvanced = currentAdvanced
        ),
      data =>
        handleMutation(
          env.ops.api.changeStatus(me.userId, id.id, data.enabled, data.reason),
          memberUrl(id.id, advanced = currentAdvanced, returnTo = currentReturnTo),
          "Status updated."
        )
    )
  }

  def updateEmail(id: UserStr) = SecureBody(_.OpsMemberWrite) { ctx ?=> me ?=>
    val bound = emailForm.bindFromRequest()
    bound.fold(
      err =>
        renderMemberFormError(
          id.id,
          forms = memberForms(email = err),
          showAdvanced = currentAdvanced
        ),
      data =>
        if !EmailAddress.isValid(data.email) then
          renderMemberFormError(
            id.id,
            forms = memberForms(email = bound.withError("email", "Enter a valid email address.")),
            showAdvanced = currentAdvanced
          )
        else
          handleMutation(
            env.ops.api.changeEmail(me.userId, id.id, EmailAddress(data.email), data.reason),
            memberUrl(id.id, advanced = currentAdvanced, returnTo = currentReturnTo),
            "Email updated."
          )
    )
  }

  def sendPasswordReset(id: UserStr) = SecureBody(_.OpsMemberWrite) { ctx ?=> me ?=>
    val bound = reasonForm.bindFromRequest()
    bound.fold(
      err =>
        renderMemberFormError(
          id.id,
          forms = memberForms(passwordReset = err),
          showAdvanced = currentAdvanced
        ),
      reason =>
        handleMutation(
          env.ops.api.sendPasswordReset(me.userId, id.id, reason),
          memberUrl(id.id, advanced = currentAdvanced, returnTo = currentReturnTo),
          "Password reset email sent."
        )
    )
  }

  def revokeAllSessions(id: UserStr) = SecureBody(_.OpsMemberWrite) { ctx ?=> me ?=>
    val bound = reasonForm.bindFromRequest()
    bound.fold(
      err =>
        renderMemberFormError(
          id.id,
          forms = memberForms(revokeAllSessions = err),
          showAdvanced = currentAdvanced
        ),
      reason =>
        handleMutation(
          env.ops.api.revokeAllSessions(me.userId, id.id, reason),
          memberUrl(id.id, advanced = currentAdvanced, returnTo = currentReturnTo),
          "All active sessions revoked."
        )
    )
  }

  def revokeSession(id: UserStr, sid: String) = SecureBody(_.OpsMemberWrite) { ctx ?=> me ?=>
    val bound = reasonForm.bindFromRequest()
    bound.fold(
      err =>
        renderMemberFormError(
          id.id,
          forms = memberForms(revokeSession = err, revokeSessionSid = sid.some),
          showAdvanced = currentAdvanced
        ),
      reason =>
        handleMutation(
          env.ops.api.revokeSession(me.userId, id.id, SessionId(sid), reason),
          memberUrl(id.id, advanced = currentAdvanced, returnTo = currentReturnTo),
          "Session revoked."
        )
    )
  }

  def clearTwoFactor(id: UserStr) = SecureBody(_.OpsMemberWrite) { ctx ?=> me ?=>
    val bound = reasonForm.bindFromRequest()
    bound.fold(
      err =>
        renderMemberFormError(
          id.id,
          forms = memberForms(clearTwoFactor = err),
          showAdvanced = currentAdvanced
        ),
      reason =>
        handleMutation(
          env.ops.api.clearTwoFactor(me.userId, id.id, reason),
          memberUrl(id.id, advanced = currentAdvanced, returnTo = currentReturnTo),
          "Two-factor removed."
        )
    )
  }

  def updatePlan(id: UserStr) = SecureBody(_.OpsMemberWrite) { ctx ?=> me ?=>
    val bound = planForm.bindFromRequest()
    bound.fold(
      err =>
        renderMemberFormError(
          id.id,
          forms = memberForms(plan = err),
          showAdvanced = currentAdvanced
        ),
      data =>
        parseTier(data.tier).fold(
          renderMemberFormError(
            id.id,
            forms = memberForms(plan = bound.withError("tier", "Choose a valid tier.")),
            showAdvanced = currentAdvanced
          )
        ): tier =>
          val expiresAt = data.expiresAt.flatMap(parseDateTime)
          if data.expiresAt.nonEmpty && expiresAt.isEmpty then
            renderMemberFormError(
              id.id,
              forms = memberForms(plan = bound.withError("expiresAt", "Enter a valid expiration date and time.")),
              showAdvanced = currentAdvanced
            )
          else
            handleMutation(
              env.ops.api.updatePlan(me.userId, id.id, tier, expiresAt, data.reason),
              memberUrl(id.id, advanced = currentAdvanced, returnTo = currentReturnTo),
              "Plan updated."
            )
    )
  }

  def updateRoles(id: UserStr) = SecureBody(_.OpsMemberRoleGrant) { ctx ?=> me ?=>
    val bound = managedRolesForm.bindFromRequest()
    bound.fold(
      err =>
        renderMemberFormError(
          id.id,
          forms = memberForms(managedRoles = err),
          showAdvanced = currentAdvanced
        ),
      data =>
        val bundle = data.bundle.flatMap(parseManagedBundle)
        val toggles = data.toggles.flatMap(parseManagedToggle)
        handleMutation(
          env.ops.api.updateManagedRoles(me.userId, id.id, bundle, toggles, data.reason),
          memberUrl(id.id, advanced = currentAdvanced, returnTo = currentReturnTo),
          "Managed roles updated."
        )
    )
  }

  def updatePermissions(id: UserStr) = SecureBody(_.OpsMemberAdvanced) { ctx ?=> me ?=>
    val bound = permissionsForm.bindFromRequest()
    bound.fold(
      err =>
        renderMemberFormError(
          id.id,
          forms = memberForms(permissions = err),
          showAdvanced = true
        ),
      data =>
        val selected = data.permissions.flatMap(parseKnownRole)
        val selectedSet = selected.toSet
        def renderPermissionError(
            form: Form[?],
            preview: Option[OpsPermissionPreview] = None,
            selectedRoles: Set[RoleDbKey] = selectedSet
        ) =
          renderMemberFormError(
            id.id,
            forms = memberForms(permissions = form),
            showAdvanced = true,
            permissionPreview = preview,
            selectedRawRoles = selectedRoles
          )

        data.intent match
          case "preview" =>
            env.ops.api.previewRawPermissions(id.id, selected).flatMap:
              case Left(err)     => renderPermissionError(bound.withGlobalError(err))
              case Right(preview) =>
                renderMemberPage(
                  id.id,
                  showAdvanced = true,
                  capabilities = currentCapabilities,
                  permissionPreview = preview.some,
                  selectedRawRoles = preview.afterRoles.toSet,
                  forms = memberForms(permissions = bound),
                  returnTo = currentReturnTo
                )
          case "apply" =>
            env.ops.api.previewRawPermissions(id.id, selected).flatMap:
              case Left(err) => renderPermissionError(bound.withGlobalError(err))
              case Right(preview) =>
                if !data.confirm then
                  renderPermissionError(
                    bound.withError("confirm", "Review the diff and confirm before applying raw permissions."),
                    preview.some,
                    preview.afterRoles.toSet
                  )
                else
                  handleMutation(
                    env.ops.api.updatePermissions(me.userId, id.id, selected, data.reason),
                    memberUrl(id.id, advanced = true, returnTo = currentReturnTo),
                    "Raw permissions updated."
                  )
          case _ =>
            renderPermissionError(bound.withGlobalError("Choose preview or apply."))
    )
  }

  private def renderMemberPage(
      id: UserId,
      showAdvanced: Boolean,
      capabilities: views.ops.OpsCapabilities,
      permissionPreview: Option[OpsPermissionPreview] = None,
      selectedRawRoles: Set[RoleDbKey] = Set.empty,
      forms: views.ops.MemberForms = memberForms(),
      returnTo: Option[String] = None,
      isBadRequest: Boolean = false,
      notice: Option[String] = None
  )(using ctx: Context, me: lila.core.user.Me): Fu[Result] =
    env.ops.api.memberDetail(id).flatMap:
      case None => notFound
      case Some(detail) =>
        val page =
          views.ops.member(
            detail = detail,
            showAdvanced = showAdvanced,
            capabilities = capabilities,
            permissionPreview = permissionPreview,
            selectedRawRoles =
              if selectedRawRoles.nonEmpty then selectedRawRoles
              else detail.member.roles.toSet,
            permissionCategories = lila.security.Permission.categorized,
            forms = forms,
            returnTo = returnTo.orElse(currentReturnTo),
            notice = notice
          )
        if isBadRequest then BadRequest.page(page) else Ok.page(page)

  private def handleMutation(
      result: Fu[Either[String, Unit]],
      redirectTo: String,
      successMessage: String
  ): Fu[Result] =
    result.map:
      case Right(_) => Redirect(redirectTo).flashing("success" -> successMessage)
      case Left(err) => Redirect(redirectTo).flashing("error" -> err)

  private def renderMemberFormError(
      id: UserId,
      forms: views.ops.MemberForms,
      showAdvanced: Boolean,
      permissionPreview: Option[OpsPermissionPreview] = None,
      selectedRawRoles: Set[RoleDbKey] = Set.empty
  )(using ctx: Context, me: lila.core.user.Me): Fu[Result] =
    renderMemberPage(
      id = id,
      showAdvanced = showAdvanced,
      capabilities = currentCapabilities,
      permissionPreview = permissionPreview,
      selectedRawRoles = selectedRawRoles,
      forms = forms,
      returnTo = currentReturnTo,
      isBadRequest = true
    )

  private def memberForms(
      status: Form[?] = statusForm,
      email: Form[?] = emailForm,
      passwordReset: Form[?] = reasonForm,
      revokeAllSessions: Form[?] = reasonForm,
      revokeSession: Form[?] = reasonForm,
      revokeSessionSid: Option[String] = None,
      clearTwoFactor: Form[?] = reasonForm,
      plan: Form[?] = planForm,
      managedRoles: Form[?] = managedRolesForm,
      permissions: Form[?] = permissionsForm
  ): views.ops.MemberForms =
    views.ops.MemberForms(
      status = status,
      email = email,
      passwordReset = passwordReset,
      revokeAllSessions = revokeAllSessions,
      revokeSession = revokeSession,
      revokeSessionSid = revokeSessionSid,
      clearTwoFactor = clearTwoFactor,
      plan = plan,
      managedRoles = managedRoles,
      permissions = permissions
    )

  private def currentAdvanced(using ctx: Context, me: lila.core.user.Me): Boolean =
    ctx.req.getQueryString("advanced").contains("true") && isGranted(_.OpsMemberAdvanced)

  private def currentReturnTo(using ctx: Context): Option[String] =
    ctx.req.getQueryString("returnTo").flatMap(sanitizeReturnTo)

  private def sanitizeReturnTo(raw: String): Option[String] =
    val value = raw.trim
    Option.when(
      value.nonEmpty &&
        value.startsWith(routes.Ops.members.url) &&
        !value.startsWith("//") &&
        !value.contains("://")
    )(value)

  private def memberUrl(id: UserId, advanced: Boolean = false, returnTo: Option[String] = None)(using ctx: Context): String =
    withQuery(routes.Ops.member(id, advanced).url, "returnTo" -> returnTo)

  private def currentCapabilities(using me: lila.core.user.Me): views.ops.OpsCapabilities =
    views.ops.OpsCapabilities(
      canReadMembers = isGranted(_.OpsMemberRead),
      canWriteMembers = isGranted(_.OpsMemberWrite),
      canGrantManagedRoles = isGranted(_.OpsMemberRoleGrant),
      canUseAdvanced = isGranted(_.OpsMemberAdvanced),
      canViewOpsViewer = isGranted(_.OpsViewer)
    )

  private def withQuery(base: String, params: (String, Option[String])*)(using ctx: Context): String =
    val filtered = params.collect { case (key, Some(value)) => s"${encode(key)}=${encode(value)}" }
    if filtered.isEmpty then base
    else
      val separator = if base.contains("?") then "&" else "?"
      s"$base$separator${filtered.mkString("&")}"

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
