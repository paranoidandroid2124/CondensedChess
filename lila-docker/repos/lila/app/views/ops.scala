package views

import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.time.{ Instant, ZoneId }
import java.time.format.DateTimeFormatter

import play.api.data.Form
import play.api.libs.json.Json

import lila.app.UiEnv.{ *, given }
import lila.app.OpenBetaBindingStatus
import lila.core.perm.Permission
import lila.core.user.{ RoleDbKey, UserTier }
import lila.llm.analysis.CommentaryOpsBoard
import lila.ops.*

object ops:

  private val zoneId = ZoneId.systemDefault
  private val instantFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm").withZone(zoneId)
  private val dateTimeLocalFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm")

  final case class MemberForms(
      status: Form[?],
      email: Form[?],
      passwordReset: Form[?],
      revokeAllSessions: Form[?],
      revokeSession: Form[?],
      revokeSessionSid: Option[String],
      clearTwoFactor: Form[?],
      plan: Form[?],
      managedRoles: Form[?],
      permissions: Form[?]
  )

  final case class OpsCapabilities(
      canReadMembers: Boolean,
      canWriteMembers: Boolean,
      canGrantManagedRoles: Boolean,
      canUseAdvanced: Boolean,
      canViewOpsViewer: Boolean
  ):
    def hasAnyAccess = canReadMembers || canViewOpsViewer

    def memberAccessLabel =
      if canUseAdvanced then "Advanced member ops"
      else if canGrantManagedRoles then "Managed role grant"
      else if canWriteMembers then "Member write"
      else if canReadMembers then "Read-only member ops"
      else "No member ops"

  private def pageShell(title: String)(content: Context ?=> Frag)(using ctx: Context): lila.ui.Page =
    Page(s"$title - Chesstory")
      .css("ops")
      .apply:
        main(cls := "ops-page")(content)

  private def flashMessages(using ctx: Context) =
    frag(
      ctx.flash("success").map(msg => div(cls := "ops-flash success")(msg)),
      ctx.flash("error").map(msg => div(cls := "ops-flash error")(msg))
    )

  private def opsNav(active: String, capabilities: OpsCapabilities) =
    st.nav(cls := "ops-nav", attr("aria-label") := "Ops sections")(
      Option.when(capabilities.hasAnyAccess)(
        a(
          cls := List("is-active" -> (active == "overview")),
          href := routes.Ops.landing.url,
          if active == "overview" then attr("aria-current") := "page" else emptyFrag
        )("Overview")
      ),
      Option.when(capabilities.canReadMembers)(
        a(
          cls := List("is-active" -> (active == "members")),
          href := routes.Ops.members.url,
          if active == "members" then attr("aria-current") := "page" else emptyFrag
        )("Members")
      ),
      Option.when(capabilities.canViewOpsViewer)(
        a(
          cls := List("is-active" -> (active == "metrics")),
          href := routes.Ops.metrics.url,
          if active == "metrics" then attr("aria-current") := "page" else emptyFrag
        )("Metrics")
      ),
      Option.when(capabilities.canViewOpsViewer)(
        a(
          cls := List("is-active" -> (active == "commentary")),
          href := routes.Ops.commentary().url,
          if active == "commentary" then attr("aria-current") := "page" else emptyFrag
        )("Commentary Ops")
      )
    )

  private def badge(label: String, kind: String = "") =
    span(cls := s"ops-badge${if kind.nonEmpty then s" $kind" else ""}")(label)

  private def renderInstant(instant: Instant) = instantFormat.format(instant)

  private def renderOptionalInstant(instant: Option[Instant]) = instant.fold("Never")(renderInstant)

  private def localDateTimeValue(instant: Option[Instant]) =
    instant.fold("")(i => dateTimeLocalFormat.format(i.atZone(zoneId)))

  private def planBadge(state: OpsPlanState) = state match
    case OpsPlanState.Free    => badge("Free")
    case OpsPlanState.Active  => badge("Active", "ok")
    case OpsPlanState.Expired => badge("Expired", "warn")

  private def statusBadge(enabled: Boolean) =
    if enabled then badge("Enabled", "ok") else badge("Disabled", "danger")

  private def roleBadges(roles: List[RoleDbKey]) =
    if roles.isEmpty then span(cls := "ops-empty")("No roles")
    else div(cls := "ops-badges")(roles.sortBy(role => role.value: String).map(role => badge(OpsRoles.displayName(role)))*)

  private def currentValue(name: String)(using ctx: Context) =
    ctx.req.getQueryString(name).getOrElse("")

  private def filterOption(labelText: String, optionValue: String, current: String) =
    option(value := optionValue, if optionValue == current then selected := true else emptyFrag)(labelText)

  private def withQuery(base: String, params: (String, Option[String])*) =
    val encoded =
      params.collect { case (key, Some(value)) =>
        s"${URLEncoder.encode(key, StandardCharsets.UTF_8)}=${URLEncoder.encode(value, StandardCharsets.UTF_8)}"
      }
    if encoded.isEmpty then base
    else
      val separator = if base.contains("?") then "&" else "?"
      s"$base$separator${encoded.mkString("&")}"

  private def membersBackUrl(returnTo: Option[String]) =
    returnTo.getOrElse(routes.Ops.members.url)

  private def memberUrl(id: lila.core.userId.UserId, advanced: Boolean = false, returnTo: Option[String] = None) =
    withQuery(routes.Ops.member(id, advanced).url, "returnTo" -> returnTo)

  private def auditUrl(id: lila.core.userId.UserId, page: Int = 1, returnTo: Option[String] = None) =
    withQuery(routes.Ops.audit(id, page).url, "returnTo" -> returnTo)

  private def planLedgerUrl(id: lila.core.userId.UserId, returnTo: Option[String] = None) =
    withQuery(routes.Ops.planLedger(id).url, "returnTo" -> returnTo)

  private def formActionUrl(call: play.api.mvc.Call, showAdvanced: Boolean, returnTo: Option[String]) =
    withQuery(
      call.url,
      "advanced" -> Option.when(showAdvanced)("true"),
      "returnTo" -> returnTo
    )

  private def pagination(page: Int, totalPages: Int, prevUrl: String, nextUrl: String) =
    div(cls := "ops-pagination")(
      span(cls := "ops-timeline-meta")(s"Page $page of $totalPages"),
      div(cls := "ops-foot-links")(
        if page > 1 then a(cls := "ops-button secondary", href := prevUrl)("Previous") else emptyFrag,
        if page < totalPages then a(cls := "ops-button secondary", href := nextUrl)("Next") else emptyFrag
      )
    )

  def landing(capabilities: OpsCapabilities)(using ctx: Context): lila.ui.Page =
    pageShell("Ops"):
      frag(
        div(cls := "ops-hero")(
          div(
            h1(cls := "ops-title")("Ops Console"),
            p(cls := "ops-subtitle")(
              "Start from the surface your account can actually use. Member actions, viewer dashboards, and advanced permission paths are permission-aware here."
            )
          ),
          div(cls := "ops-badges")(
            badge(capabilities.memberAccessLabel),
            badge(if capabilities.canViewOpsViewer then "Viewer boards enabled" else "Viewer boards locked", if capabilities.canViewOpsViewer then "ok" else "warn")
          )
        ),
        opsNav("overview", capabilities),
        flashMessages,
        div(cls := "ops-grid")(
          div(cls := "ops-main")(
            div(cls := "ops-card")(
              h2("Available surfaces"),
              div(cls := "ops-surface-grid")(
                surfaceCard(
                  "Member Ops",
                  if capabilities.canWriteMembers then "Search members, inspect state, and run audited account changes."
                  else "Search members and inspect state in read-only mode.",
                  capabilities.canReadMembers,
                  if capabilities.canReadMembers then Some(a(cls := "ops-button primary", href := routes.Ops.members.url)("Open Member Ops")) else None,
                  if capabilities.canReadMembers then None else Some("Member surfaces require Ops member read.")
                ),
                surfaceCard(
                  "Metrics",
                  "Review operator-facing Prometheus summaries and external binding health.",
                  capabilities.canViewOpsViewer,
                  if capabilities.canViewOpsViewer then Some(a(cls := "ops-button primary", href := routes.Ops.metrics.url)("Open Metrics")) else None,
                  if capabilities.canViewOpsViewer then None else Some("Metrics requires Ops viewer.")
                ),
                surfaceCard(
                  "Commentary Ops",
                  "Inspect commentary/runtime diagnostics, fallback rates, and recent samples.",
                  capabilities.canViewOpsViewer,
                  if capabilities.canViewOpsViewer then Some(a(cls := "ops-button primary", href := routes.Ops.commentary().url)("Open Commentary Ops")) else None,
                  if capabilities.canViewOpsViewer then None else Some("Commentary Ops requires Ops viewer.")
                )
              )
            ),
            div(cls := "ops-card")(
              h2("Permission boundaries"),
              div(cls := "ops-kv")(
                capabilityRow("Member read", capabilities.canReadMembers, "Browse members, audit history, and plan history."),
                capabilityRow("Member write", capabilities.canWriteMembers, "Run account status, email, password reset, session, 2FA, and plan actions."),
                capabilityRow("Managed role grant", capabilities.canGrantManagedRoles, "Update curated ops/member bundles and toggles."),
                capabilityRow("Advanced member ops", capabilities.canUseAdvanced, "Preview and apply raw permission changes."),
                capabilityRow("Ops viewer", capabilities.canViewOpsViewer, "Open metrics and commentary boards.")
              )
            )
          ),
          div(cls := "ops-side")(
            div(cls := "ops-card ops-card--warn")(
              span(cls := "ops-card-eyebrow")("Operator guidance"),
              h2("How this console behaves"),
              p(cls := "ops-subtitle")(
                "Top navigation only shows surfaces your account can reach. Inside member detail, unavailable write, role, and advanced actions are replaced with explicit lock explanations."
              )
            )
          )
        )
      )

  def members(
      page: OpsMemberSearchPage,
      pageUrl: Int => String,
      memberUrlFor: lila.core.userId.UserId => String,
      capabilities: OpsCapabilities
  )(using ctx: Context): lila.ui.Page =
    pageShell("Member Ops"):
      frag(
        div(cls := "ops-hero")(
          div(
            h1(cls := "ops-title")("Member Ops Console"),
            p(cls := "ops-subtitle")(
              "Search accounts, inspect live member state, apply reversible account actions, manage tier and curated roles, and review append-only audit history."
            )
          ),
          div(cls := "ops-badges")(
            badge(s"${page.total} members"),
            badge(s"Page ${page.page}")
          )
        ),
        opsNav("members", capabilities),
        flashMessages,
        if !capabilities.canWriteMembers then
          noticeCard(
            title = "Read-only member ops",
            text = "This account can search members and inspect state, but account-changing actions stay locked until Ops member write is granted.",
            tone = "warn",
            eyebrow = Some("Permission boundary")
          )()
        else if !capabilities.canGrantManagedRoles then
          noticeCard(
            title = "Managed roles locked",
            text = "Routine member writes are available, but curated role changes and raw permission editing remain locked for this account.",
            tone = "warn",
            eyebrow = Some("Permission boundary")
          )()
        else if !capabilities.canUseAdvanced then
          noticeCard(
            title = "Raw permissions locked",
            text = "Managed roles are available here. Advanced raw permission editing still requires Ops member advanced.",
            tone = "warn",
            eyebrow = Some("Permission boundary")
          )()
        else emptyFrag,
        div(cls := "ops-card")(
          h2("Search"),
          form(method := "get", action := routes.Ops.members.url)(
            div(cls := "ops-filter-grid")(
              div(cls := "ops-field")(
                label(attr("for") := "q")("Username or ID prefix"),
                input(id := "q", name := "q", value := currentValue("q"), placeholder := "member name")
              ),
              div(cls := "ops-field")(
                label(attr("for") := "email")("Exact email"),
                input(id := "email", name := "email", value := currentValue("email"), placeholder := "member@email.com")
              ),
              div(cls := "ops-field")(
                label(attr("for") := "enabled")("Status"),
                select(id := "enabled", name := "enabled")(
                  filterOption("Any", "", currentValue("enabled")),
                  filterOption("Enabled", "true", currentValue("enabled")),
                  filterOption("Disabled", "false", currentValue("enabled"))
                )
              ),
              div(cls := "ops-field")(
                label(attr("for") := "tier")("Tier"),
                select(id := "tier", name := "tier")(
                  filterOption("Any", "", currentValue("tier")),
                  UserTier.values.toList.map(t => filterOption(t.name, t.name, currentValue("tier")))
                )
              ),
              div(cls := "ops-field")(
                label(attr("for") := "planState")("Plan state"),
                select(id := "planState", name := "planState")(
                  filterOption("Any", "", currentValue("planState")),
                  OpsPlanState.values.toList.map(state => filterOption(state.label, state.toString, currentValue("planState")))
                )
              ),
              div(cls := "ops-field")(
                label(attr("for") := "role")("Managed role"),
                select(id := "role", name := "role")(
                  filterOption("Any", "", currentValue("role")),
                  OpsRoles.managedDailyRoles.map(role => filterOption(OpsRoles.displayName(role), role.value, currentValue("role")))
                )
              ),
              div(cls := "ops-field")(
                label(attr("for") := "createdFrom")("Created from"),
                input(id := "createdFrom", name := "createdFrom", tpe := "date", value := currentValue("createdFrom"))
              ),
              div(cls := "ops-field")(
                label(attr("for") := "createdTo")("Created to"),
                input(id := "createdTo", name := "createdTo", tpe := "date", value := currentValue("createdTo"))
              ),
              div(cls := "ops-field")(
                label(attr("for") := "seenFrom")("Seen from"),
                input(id := "seenFrom", name := "seenFrom", tpe := "date", value := currentValue("seenFrom"))
              ),
              div(cls := "ops-field")(
                label(attr("for") := "seenTo")("Seen to"),
                input(id := "seenTo", name := "seenTo", tpe := "date", value := currentValue("seenTo"))
              )
            ),
            div(cls := "ops-actions")(
              button(cls := "ops-button primary", tpe := "submit")("Apply filters"),
              a(cls := "ops-button secondary", href := routes.Ops.members.url)("Reset")
            )
          )
        ),
        div(cls := "ops-card")(
          h2("Members"),
          if page.items.nonEmpty then
            div(cls := "ops-table-wrap")(
              table(cls := "ops-table ops-table--members")(
                thead(
                  tr(
                    th("Member"),
                    th("Status"),
                    th("Plan"),
                    th("Email"),
                    th("Roles"),
                    th("Seen")
                  )
                ),
              tbody(
                page.items.map: member =>
                  tr(
                    td(attr("data-label") := "Member")(
                      a(href := memberUrlFor(member.id))(
                        strong(member.username)
                      ),
                        div(cls := "ops-timeline-meta")(member.id.value),
                        div(cls := "ops-badges")(
                        member.hasTwoFactor.option(badge("2FA", "ok"))
                      )
                    ),
                    td(attr("data-label") := "Status")(statusBadge(member.enabled)),
                    td(attr("data-label") := "Plan")(
                      div(cls := "ops-badges")(
                        badge(member.tier.name),
                        planBadge(member.planState())
                      ),
                      member.expiresAt.map(exp => div(cls := "ops-timeline-meta")(s"Expires ${renderInstant(exp)}"))
                    ),
                    td(attr("data-label") := "Email")(member.email.getOrElse("No email")),
                    td(attr("data-label") := "Roles")(roleBadges(member.roles)),
                    td(attr("data-label") := "Seen")(
                      div(cls := "ops-timeline-meta")(renderInstant(member.createdAt)),
                      div(cls := "ops-timeline-meta")(s"Last seen ${renderOptionalInstant(member.seenAt)}")
                    )
                  )
                )
              )
            )
          else
            div(cls := "ops-empty")("No members matched the current filters.")
          ,
          pagination(page.page, page.totalPages, pageUrl(page.prevPage), pageUrl(page.nextPage))
        )
      )

  def member(
      detail: OpsMemberDetail,
      showAdvanced: Boolean,
      capabilities: OpsCapabilities,
      permissionPreview: Option[OpsPermissionPreview],
      selectedRawRoles: Set[RoleDbKey],
      permissionCategories: List[(String, List[Permission])],
      forms: MemberForms,
      returnTo: Option[String],
      notice: Option[String]
  )(using ctx: Context): lila.ui.Page =
    val member = detail.member
    val selectedPermissionRoles: Set[RoleDbKey] =
      selectedValues(forms.permissions, "permissions").flatMap(raw => OpsRoles.rawRoleChoices.find(_.value == raw)).toSet match
        case values if values.nonEmpty => values
        case _                         => selectedRawRoles
    val selectedManagedToggles: Set[RoleDbKey] =
      selectedValues(forms.managedRoles, "toggles").flatMap(raw => OpsRoles.managedToggles.find(_.value == raw)).toSet match
        case values if values.nonEmpty => values
        case _                         => detail.managedRoles.toggles.toSet
    val selectedBundle = fieldValue(forms.managedRoles, "bundle").getOrElse(detail.managedRoles.bundle.map(_.value).getOrElse(""))
    val selectedTier = fieldValue(forms.plan, "tier").getOrElse(member.tier.name)
    val selectedExpiry = fieldValue(forms.plan, "expiresAt").getOrElse(localDateTimeValue(member.expiresAt))
    pageShell(s"Member Ops - ${member.username}"):
      frag(
        st.nav(cls := "ops-breadcrumbs", attr("aria-label") := "Breadcrumb")(
          a(href := membersBackUrl(returnTo))("Member Ops"),
          span("/"),
          span(attr("aria-current") := "page")(member.username),
          span("/"),
          a(href := auditUrl(member.id, returnTo = returnTo))("Full audit"),
          span("/"),
          a(href := planLedgerUrl(member.id, returnTo = returnTo))("Plan ledger")
        ),
        div(cls := "ops-hero")(
          div(
            h1(cls := "ops-title")(member.username),
            p(cls := "ops-subtitle")(
              s"Member ID ${member.id.value}. Apply audited account actions, inspect live security state, and manage curated roles without exposing destructive account paths."
            )
          ),
          div(cls := "ops-badges")(
            statusBadge(member.enabled),
            badge(member.tier.name),
            planBadge(member.planState())
          )
        ),
        opsNav("members", capabilities),
        flashMessages,
        notice.map(message =>
          div(cls := "ops-card ops-card--warn")(
            span(cls := "ops-card-eyebrow")("Permission boundary"),
            h2("Advanced member ops locked"),
            p(cls := "ops-subtitle")(message)
          )
        ),
        div(cls := "ops-card")(
          div(cls := "ops-summary-grid")(
            statCard("Email", member.email.getOrElse("No login email")),
            statCard("Created", renderInstant(member.createdAt)),
            statCard("Last seen", renderOptionalInstant(member.seenAt)),
            statCard("Security", s"${if member.hasPassword then "Password" else "No password"} / ${if member.hasTwoFactor then "2FA on" else "2FA off"}")
          ),
          div(cls := "ops-actions")(
            roleBadges(member.roles)
          )
        ),
        div(cls := "ops-grid ops-grid--member")(
          div(cls := "ops-main")(
            div(cls := "ops-card")(
              sectionHeader("Recent Audit", Some(auditUrl(member.id, returnTo = returnTo))),
              if detail.recentAudit.nonEmpty then renderAuditEntries(detail.recentAudit) else div(cls := "ops-empty")("No audit entries yet.")
            ),
            div(cls := "ops-card")(
              sectionHeader("Recent Plan Ledger", Some(planLedgerUrl(member.id, returnTo = returnTo))),
              if detail.recentPlanLedger.nonEmpty then renderPlanEntries(detail.recentPlanLedger) else div(cls := "ops-empty")("No manual plan ledger entries yet.")
            ),
            div(cls := "ops-card")(
              h2("Active Sessions"),
              if detail.sessions.nonEmpty then
                div(cls := "ops-session-list")(
                  detail.sessions.map: session =>
                    div(cls := "ops-session")(
                      div(cls := "ops-session-head")(
                        strong(session._id.value),
                        span(cls := "ops-session-meta")(renderInstant(session.date))
                      ),
                      div(cls := "ops-session-meta")(s"IP ${session.ip.value}"),
                      div(cls := "ops-session-meta")(session.ua.map(_.value).getOrElse("No user agent captured")),
                      if capabilities.canWriteMembers then
                        postForm(action := formActionUrl(routes.Ops.revokeSession(member.id, session._id.value), showAdvanced, returnTo), cls := "ops-inline-form")(
                          Option.when(forms.revokeSessionSid.contains(session._id.value))(renderFormError(forms.revokeSession)),
                          div(cls := "ops-inline-form-row")(
                            textInput(
                              forms.revokeSession,
                              fieldName = "reason",
                              fieldId = s"revoke-session-${session._id.value}",
                              placeholderText = "Reason (required)",
                              inputType = "text",
                              isRequired = true,
                              minlength = Some(8),
                              maxlength = Some(500),
                              autofocus = forms.revokeSessionSid.contains(session._id.value)
                            ),
                            button(tpe := "submit", cls := "ops-button danger")("Revoke session")
                          )
                        )
                      else div(cls := "ops-timeline-meta")("Read-only access: session revocation requires Ops member write.")
                    )
                )
              else div(cls := "ops-empty")("No active sessions.")
            ),
            if showAdvanced && capabilities.canUseAdvanced then
              div(cls := "ops-card ops-card--danger")(
                span(cls := "ops-card-eyebrow")("Access control"),
                h2("Advanced Permission Editor"),
                p(cls := "ops-subtitle")(
                  "Preview the raw role diff first, then explicitly apply it."
                ),
                permissionPreview.map: preview =>
                  div(cls := "ops-card ops-preview")(
                    h3("Diff Preview"),
                    renderDiffList(preview.diff),
                    div(cls := "ops-details")(
                      span(s"Before: ${preview.beforeRoles.map(OpsRoles.displayName).mkString(", ")}"),
                      span(s"After: ${preview.afterRoles.map(OpsRoles.displayName).mkString(", ")}")
                    )
                  )
                ,
                postForm(action := formActionUrl(routes.Ops.updatePermissions(member.id), showAdvanced = true, returnTo), cls := "ops-inline-form")(
                  renderFormError(forms.permissions),
                  div(cls := "ops-permission-grid")(
                    permissionCategories.map: (category, permissions) =>
                      div(cls := "ops-card")(
                        h3(category),
                        div(cls := "ops-permission-list")(
                          permissions.map: permission =>
                            label(cls := "ops-check")(
                              input(
                                tpe := "checkbox",
                                name := "permissions",
                                value := permission.dbKey.value,
                                if selectedPermissionRoles.contains(permission.dbKey) then checked := true else emptyFrag
                              ),
                              span(permission.name)
                            )
                        )
                      )
                  ),
                  reasonInput(forms.permissions, "raw-reason", "Explain why raw permission changes are required."),
                  label(cls := "ops-check")(
                    input(
                      tpe := "checkbox",
                      name := "confirm",
                      value := "true",
                      if selectedValues(forms.permissions, "confirm").contains("true") then checked := true else emptyFrag
                    ),
                    span("I reviewed the diff and want to apply these raw permissions.")
                  ),
                  renderFieldError(forms.permissions, "confirm"),
                  div(cls := "ops-actions")(
                    button(tpe := "submit", cls := "ops-button primary", name := "intent", value := "preview")("Preview changes"),
                    button(tpe := "submit", cls := "ops-button danger", name := "intent", value := "apply")("Apply raw permissions"),
                    a(cls := "ops-button secondary", href := memberUrl(member.id, returnTo = returnTo))("Close advanced editor")
                  )
                )
              )
            else if capabilities.canUseAdvanced then
              div(cls := "ops-card")(
                h2("Advanced Permission Editor"),
                p(cls := "ops-subtitle")("Raw permission editing is gated separately from the daily role panel."),
                a(cls := "ops-button secondary", href := memberUrl(member.id, advanced = true, returnTo = returnTo))("Open advanced editor")
              )
            else if capabilities.canGrantManagedRoles then
              noticeCard(
                title = "Raw permissions locked",
                text = "Previewing and applying raw permissions requires Ops member advanced.",
                tone = "warn",
                eyebrow = Some("Permission boundary")
              )()
            else emptyFrag
          ),
          div(cls := "ops-side")(
            if capabilities.canWriteMembers then
              frag(
                actionCard(
                  "Account Status",
                  s"Current state: ${if member.enabled then "enabled" else "disabled"}.",
                  formActionUrl(routes.Ops.updateStatus(member.id), showAdvanced, returnTo),
                  forms.status,
                  tone = if member.enabled then "danger" else "primary",
                  eyebrow = Some(if member.enabled then "Security action" else "Routine update")
                )(
                  input(tpe := "hidden", name := "enabled", value := s"${!member.enabled}"),
                  reasonInput(forms.status, "status-reason", "Explain why the member should be enabled or disabled."),
                  button(
                    tpe := "submit",
                    cls := s"ops-button ${if member.enabled then "danger" else "primary"}"
                  )(if member.enabled then "Disable account" else "Enable account")
                ),
                actionCard(
                  "Change Email",
                  s"Current login email: ${member.email.getOrElse("none")}",
                  formActionUrl(routes.Ops.updateEmail(member.id), showAdvanced, returnTo),
                  forms.email,
                  tone = "primary",
                  eyebrow = Some("Routine update")
                )(
                  textInputField(forms.email, "email", "ops-email", "New email", "member@email.com", tpe = "email"),
                  reasonInput(forms.email, "email-reason", "Why is the login email being changed?"),
                  button(tpe := "submit", cls := "ops-button primary")("Update email")
                ),
                actionCard(
                  "Password Reset",
                  "Dispatches a login reset to the member's current email.",
                  formActionUrl(routes.Ops.sendPasswordReset(member.id), showAdvanced, returnTo),
                  forms.passwordReset,
                  tone = "warn",
                  eyebrow = Some("Security action")
                )(
                  reasonInput(forms.passwordReset, "password-reset-reason", "Why is a password reset being dispatched?"),
                  button(tpe := "submit", cls := "ops-button warn")("Send password reset")
                ),
                actionCard(
                  "Two-Factor",
                  if member.hasTwoFactor then "Clear the stored TOTP secret." else "Two-factor is not currently enabled.",
                  formActionUrl(routes.Ops.clearTwoFactor(member.id), showAdvanced, returnTo),
                  forms.clearTwoFactor,
                  tone = "danger",
                  eyebrow = Some("Security action")
                )(
                  reasonInput(forms.clearTwoFactor, "two-factor-reason", "Why must the member's second factor be cleared?"),
                  button(
                    tpe := "submit",
                    cls := "ops-button danger",
                    if !member.hasTwoFactor then disabled := "disabled" else emptyFrag
                  )("Clear 2FA")
                ),
                actionCard(
                  "Plan",
                  s"Current plan: ${member.tier.name}.",
                  formActionUrl(routes.Ops.updatePlan(member.id), showAdvanced, returnTo),
                  forms.plan,
                  tone = "primary",
                  eyebrow = Some("Routine update")
                )(
                  div(cls := "ops-field")(
                    label(attr("for") := "ops-tier")("Tier"),
                    select(id := "ops-tier", name := "tier")(
                      UserTier.values.toList.map(t => filterOption(t.name, t.name, selectedTier))
                    ),
                    renderFieldError(forms.plan, "tier")
                  ),
                  div(cls := "ops-field")(
                    label(attr("for") := "ops-expiry")("Expires at"),
                    input(id := "ops-expiry", name := "expiresAt", tpe := "datetime-local", value := selectedExpiry),
                    renderFieldError(forms.plan, "expiresAt")
                  ),
                  reasonInput(forms.plan, "plan-reason", "Why is the plan being changed?"),
                  button(tpe := "submit", cls := "ops-button primary")("Apply plan")
                ),
                if capabilities.canGrantManagedRoles then
                  actionCard(
                    "Managed Roles",
                    "Daily-use bundles and badges only. Legacy unmanaged roles remain untouched.",
                    formActionUrl(routes.Ops.updateRoles(member.id), showAdvanced, returnTo),
                    forms.managedRoles,
                    tone = "primary",
                    eyebrow = Some("Access control")
                  )(
                    div(cls := "ops-field")(
                      label(attr("for") := "ops-bundle")("Ops bundle"),
                      select(id := "ops-bundle", name := "bundle")(
                        filterOption("No bundle", "", selectedBundle),
                        OpsRoles.opsBundles.map(role => filterOption(OpsRoles.displayName(role), role.value, selectedBundle))
                      )
                    ),
                    div(cls := "ops-field")(
                      label("Managed toggles"),
                      div(cls := "ops-permission-list")(
                        OpsRoles.managedToggles.map: role =>
                          label(cls := "ops-check")(
                            input(
                              tpe := "checkbox",
                              name := "toggles",
                              value := role.value,
                              if selectedManagedToggles.contains(role) then checked := true else emptyFrag
                            ),
                            span(OpsRoles.displayName(role))
                          )
                      )
                    ),
                    div(cls := "ops-field")(
                      label("Unmanaged roles"),
                      if detail.managedRoles.unmanaged.nonEmpty then roleBadges(detail.managedRoles.unmanaged) else div(cls := "ops-empty")("None")
                    ),
                    reasonInput(forms.managedRoles, "roles-reason", "Why are the curated member roles being changed?"),
                    button(tpe := "submit", cls := "ops-button primary")("Save managed roles")
                  )
                else
                  noticeCard(
                    title = "Managed roles locked",
                    text = "Curated role changes require Ops member role grant. You can still inspect current member state and history here.",
                    tone = "warn",
                    eyebrow = Some("Permission boundary")
                  )(),
                actionCard(
                  "Sessions",
                  "Immediately signs the member out on all devices.",
                  formActionUrl(routes.Ops.revokeAllSessions(member.id), showAdvanced, returnTo),
                  forms.revokeAllSessions,
                  tone = "danger",
                  eyebrow = Some("Security action")
                )(
                  reasonInput(forms.revokeAllSessions, "revoke-all-reason", "Why are all sessions being revoked?"),
                  button(tpe := "submit", cls := "ops-button danger")("Revoke all sessions")
                )
              )
            else
              noticeCard(
                title = "Read-only access",
                text = "You can inspect member state, sessions, audit, and plan history here. Ops member write is required for account changes.",
                tone = "warn",
                eyebrow = Some("Permission boundary")
              )()
          )
        )
      )

  def audit(member: OpsMemberState, auditPage: OpsAuditPage, returnTo: Option[String], capabilities: OpsCapabilities)(using ctx: Context): lila.ui.Page =
    pageShell(s"Audit - ${member.username}"):
      frag(
        st.nav(cls := "ops-breadcrumbs", attr("aria-label") := "Breadcrumb")(
          a(href := membersBackUrl(returnTo))("Member Ops"),
          span("/"),
          a(href := memberUrl(member.id, returnTo = returnTo))("Member detail"),
          span("/"),
          span(attr("aria-current") := "page")("Audit")
        ),
        div(cls := "ops-hero")(
          div(
            h1(cls := "ops-title")(s"Audit for ${member.username}"),
            p(cls := "ops-subtitle")("Append-only history of operator actions and state diffs.")
          )
        ),
        opsNav("members", capabilities),
        flashMessages,
        div(cls := "ops-card")(
          if auditPage.entries.nonEmpty then renderAuditEntries(auditPage.entries) else div(cls := "ops-empty")("No audit entries yet."),
          pagination(
            auditPage.page,
            auditPage.totalPages,
            auditUrl(member.id, (auditPage.page - 1).max(1), returnTo),
            auditUrl(member.id, (auditPage.page + 1).min(auditPage.totalPages), returnTo)
          )
        )
      )

  def planLedger(member: OpsMemberState, entries: List[UserPlanLedgerEntry], returnTo: Option[String], capabilities: OpsCapabilities)(using ctx: Context): lila.ui.Page =
    pageShell(s"Plan Ledger - ${member.username}"):
      frag(
        st.nav(cls := "ops-breadcrumbs", attr("aria-label") := "Breadcrumb")(
          a(href := membersBackUrl(returnTo))("Member Ops"),
          span("/"),
          a(href := memberUrl(member.id, returnTo = returnTo))("Member detail"),
          span("/"),
          span(attr("aria-current") := "page")("Plan ledger")
        ),
        div(cls := "ops-hero")(
          div(
            h1(cls := "ops-title")(s"Plan ledger for ${member.username}"),
            p(cls := "ops-subtitle")("Manual plan grants, revocations, and expiry changes recorded from the ops console.")
          )
        ),
        opsNav("members", capabilities),
        flashMessages,
        div(cls := "ops-card")(
          if entries.nonEmpty then renderPlanEntries(entries) else div(cls := "ops-empty")("No plan ledger entries yet.")
        )
      )

  def metrics(snapshot: OpsMetricsSnapshot, bindings: List[OpenBetaBindingStatus], capabilities: OpsCapabilities)(using ctx: Context): lila.ui.Page =
    val requiredBindings = bindings.count(_.required)
    val missingRequiredBindings = bindings.count(status => status.required && !status.configured)
    val unhealthyBindings =
      bindings.count(status =>
        status.required && status.spec.readinessClass == "core" && !status.readyOk
      )
    pageShell("Metrics"):
      frag(
        div(cls := "ops-hero")(
          div(
            h1(cls := "ops-title")("Operator Metrics"),
            p(cls := "ops-subtitle")(
              "Browser-facing metrics view for the operator account. Prometheus machine scraping can stay on the key-based endpoint, while humans use this login-gated page."
            )
          ),
          div(cls := "ops-badges")(
            badge(if snapshot.hasData then "Scrape available" else "No scrape", if snapshot.hasData then "ok" else "warn"),
            badge(s"${snapshot.familyCount} families"),
            badge(s"${snapshot.sampleCount} samples"),
            badge(s"${snapshot.sectionCount} namespaces"),
            badge(s"$requiredBindings required bindings"),
            Option.when(missingRequiredBindings > 0)(badge(s"$missingRequiredBindings missing", "danger")),
            Option.when(unhealthyBindings > 0)(badge(s"$unhealthyBindings unhealthy", "warn"))
          )
        ),
        opsNav("metrics", capabilities),
        flashMessages,
        div(cls := "ops-grid")(
          div(cls := "ops-main")(
            div(cls := "ops-card")(
              h2("How to use this page"),
              div(cls := "ops-kv")(
                div(cls := "ops-kv-item")(
                  strong("Access"),
                  span("Sign in as an operator account and open /ops/metrics.")
                ),
                div(cls := "ops-kv-item")(
                  strong("Purpose"),
                  span("Quick manual inspection of the current Prometheus scrape without exposing a public secret URL in day-to-day operator browsing.")
                ),
                div(cls := "ops-kv-item")(
                  strong("Machine scrape"),
                  span("Infrastructure can keep using the existing key-based scrape endpoint or a Cloud Run / ingress restriction. This page is for humans, not Prometheus itself.")
                ),
                div(cls := "ops-kv-item")(
                  strong("GUI scope"),
                  span("This is a read-only inspection page. Alerting, retention, and dashboards still belong in Prometheus/Grafana or GCP tooling.")
                )
              )
            ),
            div(cls := "ops-card")(
              h2("Scrape overview"),
              p(cls := "ops-subtitle")(
                "Parsed summary comes first. Use the raw scrape at the bottom only when you need to cross-check exact values."
              ),
              div(cls := "ops-summary-grid")(
                statCard("Sample families", snapshot.familyCount.toString),
                statCard("Samples", snapshot.sampleCount.toString),
                statCard("Namespaces", snapshot.sectionCount.toString),
                statCard("Malformed lines", snapshot.parseErrorCount.toString)
              ),
              div(cls := "ops-kv")(
                div(cls := "ops-kv-item")(
                  strong("Scrape lines"),
                  span(snapshot.lineCount.toString)
                ),
                div(cls := "ops-kv-item")(
                  strong("Comment lines"),
                  span(snapshot.commentCount.toString)
                )
              ),
              if !snapshot.hasData then
                div(cls := "ops-empty")("No scrape data has been produced yet.")
              else if snapshot.parseErrorCount > 0 then
                div(cls := "ops-empty")(s"${snapshot.parseErrorCount} scrape lines could not be parsed into metric samples.")
              else emptyFrag
            ),
            div(cls := "ops-card")(
              h2("Namespace summary"),
              if snapshot.sections.nonEmpty then renderMetricNamespaceTable(snapshot.sections) else div(cls := "ops-empty")("No parsed metric families yet.")
            ),
            div(cls := "ops-card")(
              h2("Metric families"),
              p(cls := "ops-subtitle")(
                "Grouped by namespace so operators can scan JVM, process, HTTP, and commentary-related metrics without reading the raw dump first."
              ),
              if snapshot.sections.nonEmpty then
                div(cls := "ops-metric-sections")(
                  snapshot.sections.map: section =>
                    div(cls := "ops-card ops-preview")(
                      h3(section.title),
                      renderMetricFamilyTable(section.families)
                    )
                )
              else div(cls := "ops-empty")("No metric families were parsed from the current scrape.")
            ),
            div(cls := "ops-card")(
              h2("External binding snapshot"),
              p(cls := "ops-subtitle")(
                "These rows show the effective open-beta runtime bindings, whether each one is required in the current mode, and whether readiness sees it as healthy."
              ),
              renderBindingTable(bindings)
            ),
            div(cls := "ops-card")(
              h2("Raw Prometheus scrape"),
              snapshot.raw.fold(div(cls := "ops-empty")("No scrape data has been produced yet."))(data =>
                pre(cls := "ops-code")(code(data))
              )
            )
          ),
          div(cls := "ops-side")(
            div(cls := "ops-card")(
              h2("Operator flow"),
              p(cls := "ops-subtitle")("Use the operator account for read-only inspection here, then jump back into Member Ops for account actions."),
              div(cls := "ops-actions")(
                a(cls := "ops-button secondary", href := routes.Ops.landing.url)("Open Overview"),
                Option.when(capabilities.canReadMembers)(a(cls := "ops-button secondary", href := routes.Ops.members.url)("Open Member Ops")),
                Option.when(capabilities.canViewOpsViewer)(a(cls := "ops-button secondary", href := routes.Ops.commentary().url)("Open Commentary Ops"))
              )
            )
          )
        )
      )

  def commentary(snapshot: CommentaryOpsBoard.Snapshot, limit: Int, capabilities: OpsCapabilities)(using ctx: Context): lila.ui.Page =
    pageShell("Commentary Ops"):
      frag(
        div(cls := "ops-hero")(
          div(
            h1(cls := "ops-title")("Commentary Ops"),
            p(cls := "ops-subtitle")(
              "Read-only operator board for Game Chronicle and commentary-analysis runtime behavior. Use this page to inspect throughput, fallback behavior, prompt usage, and recent captured samples."
            )
          ),
          div(cls := "ops-badges")(
            badge(s"${snapshot.recentSamples.size} samples"),
            badge(s"Limit $limit"),
            badge(renderInstant(Instant.ofEpochMilli(snapshot.generatedAtMs)))
          )
        ),
        opsNav("commentary", capabilities),
        flashMessages,
        div(cls := "ops-main")(
          div(cls := "ops-card")(
            h2("How to use this page"),
            div(cls := "ops-kv")(
              div(cls := "ops-kv-item")(
                strong("Access"),
                span("Sign in as an operator account and open /ops/commentary.")
              ),
              div(cls := "ops-kv-item")(
                strong("Purpose"),
                span("This page is a diagnostic board. It does not change models or prompts; it only shows current behavior and recent captures.")
              ),
              div(cls := "ops-kv-item")(
                strong("GUI scope"),
                span("Use it to inspect compare rates, fallback rates, active-note routing, and prompt usage before going deeper into logs or GCP dashboards.")
              ),
              div(cls := "ops-kv-item")(
                strong("Raw data"),
                span("The full snapshot is rendered below as JSON as well, so the operator can cross-check the summarized cards.")
              )
            )
          ),
          div(cls := "ops-card")(
            h2("Summary"),
            p(cls := "ops-subtitle")(
              "Rates with no underlying observations are shown as no data instead of looking healthy by default."
            ),
            div(cls := "ops-summary-grid")(
              statCard("Bookmaker requests", snapshot.bookmaker.requests.toString),
              statCard("Bookmaker fallback", percentageOrNoData(snapshot.bookmaker.polishFallbackRate, snapshot.bookmaker.polishAttempts, "attempts")),
              statCard("Full-game consistency", percentageOrNoData(snapshot.fullgame.compareConsistencyRate, snapshot.fullgame.compareObserved)),
              statCard("Active attach rate", percentageOrNoData(snapshot.active.attachRate, snapshot.active.attempts, "attempts"))
            )
          ),
          div(cls := "ops-grid")(
            div(cls := "ops-main")(
              div(cls := "ops-card")(
                h2("Bookmaker"),
                renderKeyValues(
                  "Requests" -> snapshot.bookmaker.requests.toString,
                  "Polish attempts" -> snapshot.bookmaker.polishAttempts.toString,
                  "Polish accepted" -> snapshot.bookmaker.polishAccepted.toString,
                  "Fallback rate" -> percentageOrNoData(snapshot.bookmaker.polishFallbackRate, snapshot.bookmaker.polishAttempts, "attempts"),
                  "Soft repair any" -> percentageOrNoData(snapshot.bookmaker.softRepairAnyRate, snapshot.bookmaker.polishAccepted, "accepted"),
                  "Soft repair material" -> percentageOrNoData(snapshot.bookmaker.softRepairMaterialRate, snapshot.bookmaker.polishAccepted, "accepted"),
                  "Compare observed" -> snapshot.bookmaker.compareObserved.toString,
                  "Compare consistency" -> percentageOrNoData(snapshot.bookmaker.compareConsistencyRate, snapshot.bookmaker.compareObserved),
                  "Average cost USD" -> usd(snapshot.bookmaker.avgCostUsd)
                )
              ),
              div(cls := "ops-card")(
                h2("Full game"),
                renderKeyValues(
                  "Compare observed" -> snapshot.fullgame.compareObserved.toString,
                  "Compare consistency" -> percentageOrNoData(snapshot.fullgame.compareConsistencyRate, snapshot.fullgame.compareObserved),
                  "Repair attempts" -> snapshot.fullgame.repairAttempts.toString,
                  "Repair bypassed" -> snapshot.fullgame.repairBypassed.toString,
                  "Soft repair applied" -> snapshot.fullgame.softRepairApplied.toString,
                  "Merged retry skipped" -> snapshot.fullgame.mergedRetrySkipped.toString
                ),
                h3("Invalid reason counts"),
                renderStringLongTable(snapshot.fullgame.invalidReasonCounts)
              ),
              div(cls := "ops-card")(
                h2("Active note / Game Chronicle"),
                renderKeyValues(
                  "Selected moments" -> snapshot.active.selectedMoments.toString,
                  "Attempts" -> snapshot.active.attempts.toString,
                  "Attached" -> snapshot.active.attached.toString,
                  "Omitted" -> snapshot.active.omitted.toString,
                  "Primary accepted" -> snapshot.active.primaryAccepted.toString,
                  "Repair attempts" -> snapshot.active.repairAttempts.toString,
                  "Repair recovered" -> snapshot.active.repairRecovered.toString,
                  "Attach rate" -> percentageOrNoData(snapshot.active.attachRate, snapshot.active.attempts, "attempts"),
                  "Thesis agreement" -> percentageOrNoData(snapshot.active.thesisAgreementRate, snapshot.active.primaryAccepted, "accepted"),
                  "Dossier attach" -> percentageOrNoData(snapshot.active.dossierAttachRate, snapshot.active.selectedMoments, "selected"),
                  "Dossier compare" -> percentageOrNoData(snapshot.active.dossierCompareRate, snapshot.active.attached, "attached"),
                  "Dossier route ref" -> percentageOrNoData(snapshot.active.dossierRouteRefRate, snapshot.active.attached, "attached"),
                  "Dossier ref failure" -> percentageOrNoData(snapshot.active.dossierReferenceFailureRate, snapshot.active.attached, "attached"),
                  "Provider" -> snapshot.active.provider.getOrElse("-"),
                  "Configured model" -> snapshot.active.configuredModel.getOrElse("-"),
                  "Fallback model" -> snapshot.active.fallbackModel.getOrElse("-"),
                  "Reasoning effort" -> snapshot.active.reasoningEffort.getOrElse("-")
                ),
                h3("Observed models"),
                renderStringLongTable(snapshot.active.observedModelDistribution),
                h3("Omit reasons"),
                renderStringLongTable(snapshot.active.omitReasons),
                h3("Warning reasons"),
                renderStringLongTable(snapshot.active.warningReasons),
                h3("Route counters"),
                renderKeyValues(
                  "Redeploy" -> snapshot.active.routeRedeployCount.toString,
                  "Move ref" -> snapshot.active.routeMoveRefCount.toString,
                  "Hidden safety" -> snapshot.active.routeHiddenSafetyCount.toString,
                  "Toward only" -> snapshot.active.routeTowardOnlyCount.toString,
                  "Exact surface" -> snapshot.active.routeExactSurfaceCount.toString,
                  "Opponent hidden" -> snapshot.active.routeOpponentHiddenCount.toString
                )
              ),
              div(cls := "ops-card")(
                h2("Prompt usage"),
                renderPromptUsage(snapshot.promptUsage)
              ),
              div(cls := "ops-card")(
                h2("Recent samples"),
                if snapshot.recentSamples.nonEmpty then
                  div(cls := "ops-sample-list")(
                    snapshot.recentSamples.map: sample =>
                      div(cls := "ops-sample-item")(
                        div(cls := "ops-timeline-head")(
                          div(
                            strong(sample.kind),
                            div(cls := "ops-timeline-meta")(renderInstant(Instant.ofEpochMilli(sample.capturedAtMs)))
                          )
                        ),
                        renderStringStringTable(sample.fields)
                      )
                  )
                else div(cls := "ops-empty")("No samples recorded yet.")
              ),
              div(cls := "ops-card")(
                h2("Raw snapshot"),
                pre(cls := "ops-code")(code(Json.prettyPrint(Json.toJson(snapshot))))
              )
            ),
            div(cls := "ops-side")(
              div(cls := "ops-card")(
              h2("Operator flow"),
              p(cls := "ops-subtitle")("Use this page for read-only diagnostics, then jump to Metrics or Member Ops as needed."),
              div(cls := "ops-actions")(
                a(cls := "ops-button secondary", href := routes.Ops.landing.url)("Open Overview"),
                a(cls := "ops-button secondary", href := routes.Ops.metrics.url)("Open Metrics"),
                Option.when(capabilities.canReadMembers)(a(cls := "ops-button secondary", href := routes.Ops.members.url)("Open Member Ops"))
              )
            )
          )
        )
        )
      )

  private def statCard(labelText: String, valueText: String) =
    div(cls := "ops-stat")(
      span(cls := "ops-stat-label")(labelText),
      span(cls := "ops-stat-value")(valueText)
    )

  private def surfaceCard(
      title: String,
      text: String,
      isAvailable: Boolean,
      action: Option[Frag],
      lockedText: Option[String]
  ) =
    div(cls := s"ops-card ${if isAvailable then "ops-card--primary" else "ops-card--warn"}")(
      span(cls := "ops-card-eyebrow")(if isAvailable then "Available" else "Locked"),
      h2(title),
      p(cls := "ops-subtitle")(text),
      lockedText.map(message => div(cls := "ops-empty")(message)),
      action
    )

  private def capabilityRow(title: String, enabled: Boolean, description: String) =
    div(cls := "ops-kv-item")(
      strong(title),
      span(description),
      div(cls := "ops-badges")(
        badge(if enabled then "Enabled" else "Locked", if enabled then "ok" else "warn")
      )
    )

  private def sectionHeader(title: String, linkHref: Option[String]) =
    div(cls := "ops-hero")(
      h2(title),
      linkHref.map(link => a(cls := "ops-button link", href := link)("View all"))
    )

  private def renderAuditEntries(entries: List[OpsMemberAuditEntry]) =
    div(cls := "ops-timeline")(
      entries.map: entry =>
        st.article(cls := "ops-timeline-item")(
          div(cls := "ops-timeline-head")(
            div(
              strong(entry.action.label),
              div(cls := "ops-timeline-meta")(s"Actor ${entry.actor.value}")
            ),
            span(cls := "ops-timeline-meta")(renderInstant(entry.at))
          ),
          p(entry.reason),
          if entry.diff.nonEmpty then renderDiffList(entry.diff) else div(cls := "ops-timeline-meta")("No snapshot diff."),
          if entry.details.nonEmpty then
            div(cls := "ops-details")(
              entry.details.map(detail => span(s"${detail.key}: ${detail.value}"))
            )
          else emptyFrag
        )
    )

  private def renderDiffList(diff: List[OpsFieldDiff]) =
    ul(cls := "ops-diff")(
      diff.map: item =>
        li(
          strong(item.field),
          s": ${item.before.getOrElse("empty")} -> ${item.after.getOrElse("empty")}"
        )
    )

  private def renderPlanEntries(entries: List[UserPlanLedgerEntry]) =
    div(cls := "ops-timeline")(
      entries.map: entry =>
        st.article(cls := "ops-timeline-item")(
          div(cls := "ops-timeline-head")(
            div(
              strong(s"${entry.oldTier.name} -> ${entry.newTier.name}"),
              div(cls := "ops-timeline-meta")(s"Actor ${entry.actor.value}")
            ),
            span(cls := "ops-timeline-meta")(renderInstant(entry.at))
          ),
          p(entry.reason),
          div(cls := "ops-details")(
            span(s"Old expiry: ${renderOptionalInstant(entry.oldExpiresAt)}"),
            span(s"New expiry: ${renderOptionalInstant(entry.newExpiresAt)}"),
            span(s"Source: ${entry.source}")
          )
        )
    )

  private def renderKeyValues(entries: (String, String)*) =
    if entries.nonEmpty then
      div(cls := "ops-kv")(
        entries.map:
          case (labelText, valueText) =>
          div(cls := "ops-kv-item")(
            strong(labelText),
            span(valueText)
          )
      )
    else div(cls := "ops-empty")("No values recorded.")

  private def renderStringLongTable(entries: Map[String, Long]) =
    if entries.nonEmpty then
      div(cls := "ops-table-wrap")(
        table(cls := "ops-table ops-table--compact")(
          thead(tr(th("Key"), th("Value"))),
          tbody(
            entries.toList.sortBy(_._1).map:
              case (key, value) =>
              tr(
                td(attr("data-label") := "Key")(key),
                td(attr("data-label") := "Value")(value.toString)
              )
          )
        )
      )
    else div(cls := "ops-empty")("No counters recorded.")

  private def renderStringStringTable(entries: Map[String, String]) =
    if entries.nonEmpty then
      div(cls := "ops-table-wrap")(
        table(cls := "ops-table ops-table--compact")(
          thead(tr(th("Field"), th("Value"))),
          tbody(
            entries.toList.sortBy(_._1).map:
              case (key, value) =>
              tr(
                td(attr("data-label") := "Field")(key),
                td(attr("data-label") := "Value")(value)
              )
          )
        )
      )
    else div(cls := "ops-empty")("No fields captured.")

  private def renderPromptUsage(entries: Map[String, CommentaryOpsBoard.PromptUsageMetrics]) =
    if entries.nonEmpty then
      div(cls := "ops-table-wrap")(
        table(cls := "ops-table ops-table--compact")(
          thead(
            tr(
              th("Prompt"),
              th("Attempts"),
              th("Cache hits"),
              th("Prompt tokens"),
              th("Cached tokens"),
              th("Completion tokens"),
              th("Estimated cost")
            )
          ),
          tbody(
            entries.toList.sortBy(_._1).map:
              case (key, usage) =>
              tr(
                td(attr("data-label") := "Prompt")(strong(key)),
                td(attr("data-label") := "Attempts")(usage.attempts.toString),
                td(attr("data-label") := "Cache hits")(usage.cacheHits.toString),
                td(attr("data-label") := "Prompt tokens")(usage.promptTokens.toString),
                td(attr("data-label") := "Cached tokens")(usage.cachedTokens.toString),
                td(attr("data-label") := "Completion tokens")(usage.completionTokens.toString),
                td(attr("data-label") := "Estimated cost")(usd(usage.estimatedCostUsd))
              )
          )
        )
      )
    else div(cls := "ops-empty")("No prompt usage recorded yet.")

  private def renderMetricNamespaceTable(sections: List[OpsMetricSection]) =
    div(cls := "ops-table-wrap")(
      table(cls := "ops-table ops-table--compact")(
        thead(tr(th("Namespace"), th("Families"), th("Samples"))),
        tbody(
          sections.sortBy(section => (-section.sampleCount, section.title)).map: section =>
            tr(
              td(attr("data-label") := "Namespace")(strong(section.title)),
              td(attr("data-label") := "Families")(section.familyCount.toString),
              td(attr("data-label") := "Samples")(section.sampleCount.toString)
            )
          )
      )
    )

  private def renderMetricFamilyTable(families: List[OpsMetricFamily]) =
    div(cls := "ops-table-wrap")(
      table(cls := "ops-table ops-table--compact")(
        thead(
          tr(
            th("Metric"),
            th("Type"),
            th("Samples"),
            th("Labels"),
            th("Help"),
            th("Example")
          )
        ),
        tbody(
          families.map: family =>
            tr(
              td(attr("data-label") := "Metric")(
                span(cls := "ops-metric-family-name")(family.name)
              ),
              td(attr("data-label") := "Type")(family.metricType.label),
              td(attr("data-label") := "Samples")(family.sampleCount.toString),
              td(attr("data-label") := "Labels")(metricLabelKeys(family)),
              td(attr("data-label") := "Help")(family.help.getOrElse("-")),
              td(attr("data-label") := "Example")(metricExample(family))
            )
        )
      )
    )

  private def percentage(value: Double): String =
    f"${value * 100}%.1f%%"

  private def percentageOrNoData(value: Double, observed: Long, basisLabel: String = "observed"): String =
    if observed > 0 then percentage(value)
    else s"No data (0 $basisLabel)"

  private def usd(value: Double): String =
    f"$$${value}%.4f"

  private def renderBindingTable(bindings: List[OpenBetaBindingStatus]) =
    if bindings.nonEmpty then
      div(cls := "ops-table-wrap")(
        table(cls := "ops-table ops-table--compact")(
          thead(
            tr(
              th("Binding"),
              th("Config path"),
              th("Required"),
              th("Configured"),
              th("Reachability"),
              th("Detail")
            )
          ),
          tbody(
            bindings.map: status =>
              tr(
                td(attr("data-label") := "Binding")(
                  strong(status.spec.env),
                  div(cls := "ops-timeline-meta")(status.spec.readinessClass.replace('_', ' '))
                ),
                td(attr("data-label") := "Config path")(status.spec.displayConfigPath),
                td(attr("data-label") := "Required")(if status.required then badge("Required", "ok") else badge("Optional")),
                td(attr("data-label") := "Configured")(if status.configured then badge("Configured", "ok") else badge("Missing", if status.required then "danger" else "warn")),
                td(attr("data-label") := "Reachability")(
                  status.reachable match
                    case Some(true) => badge("Reachable", "ok")
                    case Some(false) => badge("Unreachable", if status.required then "danger" else "warn")
                    case None => badge("Config only")
                ),
                td(attr("data-label") := "Detail")(
                  span(status.detail),
                  div(cls := "ops-timeline-meta")(status.spec.notes)
                )
              )
          )
        )
      )
    else div(cls := "ops-empty")("No binding manifest entries loaded.")

  private def actionCard(
      title: String,
      text: String,
      submitUrl: String,
      form: Form[?],
      tone: String = "primary",
      eyebrow: Option[String] = None
  )(body: Frag*) =
    div(cls := s"ops-card ops-card--$tone")(
      eyebrow.map(labelText => span(cls := "ops-card-eyebrow")(labelText)),
      h2(title),
      p(cls := "ops-subtitle")(text),
      postForm(action := submitUrl, cls := "ops-inline-form")(
        renderFormError(form),
        body
      )
    )

  private def noticeCard(title: String, text: String, tone: String = "warn", eyebrow: Option[String] = None)(body: Frag*) =
    div(cls := s"ops-card ops-card--$tone")(
      eyebrow.map(labelText => span(cls := "ops-card-eyebrow")(labelText)),
      h2(title),
      p(cls := "ops-subtitle")(text),
      body
    )

  private def reasonInput(form: Form[?], fieldId: String, placeholderText: String, fieldName: String = "reason") =
    div(cls := "ops-field")(
      label(attr("for") := fieldId)("Reason"),
      textarea(
        id := fieldId,
        name := fieldName,
        placeholder := placeholderText,
        required := true,
        attr("minlength") := "8",
        attr("maxlength") := "500",
        if fieldHasError(form, fieldName) then attr("aria-invalid") := "true" else emptyFrag,
        fieldErrorId(form, fieldName).map(id => attr("aria-describedby") := id).getOrElse(emptyFrag)
      )(fieldValue(form, fieldName).getOrElse("")),
      renderFieldError(form, fieldName)
    )

  private def textInputField(
      form: Form[?],
      fieldName: String,
      fieldId: String,
      labelText: String,
      placeholderText: String,
      tpe: String = "text"
  ) =
    div(cls := "ops-field")(
      label(attr("for") := fieldId)(labelText),
      textInput(form, fieldName, fieldId, placeholderText, inputType = tpe, isRequired = true),
      renderFieldError(form, fieldName)
    )

  private def textInput(
      form: Form[?],
      fieldName: String,
      fieldId: String,
      placeholderText: String,
      inputType: String = "text",
      isRequired: Boolean = true,
      minlength: Option[Int] = None,
      maxlength: Option[Int] = None,
      autofocus: Boolean = false
  ) =
    input(
      id := fieldId,
      name := fieldName,
      tpe := inputType,
      value := fieldValue(form, fieldName).getOrElse(""),
      placeholder := placeholderText,
      if isRequired then required := "required" else emptyFrag,
      minlength.map(value => attr("minlength") := value.toString).getOrElse(emptyFrag),
      maxlength.map(value => attr("maxlength") := value.toString).getOrElse(emptyFrag),
      if fieldHasError(form, fieldName) then attr("aria-invalid") := "true" else emptyFrag,
      fieldErrorId(form, fieldName).map(idValue => attr("aria-describedby") := idValue).getOrElse(emptyFrag),
      if autofocus then attr("autofocus") := "autofocus" else emptyFrag
    )

  private def renderFormError(form: Form[?]) =
    form.globalErrors.headOption.map(err => div(cls := "ops-form-error")(err.message))

  private def renderFieldError(form: Form[?], name: String) =
    form.error(name).map: err =>
      div(cls := "ops-field-error", id := s"$name-error")(err.message)

  private def fieldHasError(form: Form[?], name: String) =
    form.error(name).isDefined

  private def fieldErrorId(form: Form[?], name: String) =
    Option.when(fieldHasError(form, name))(s"$name-error")

  private def fieldValue(form: Form[?], name: String): Option[String] =
    form(name).value.orElse(form.data.get(name))

  private def selectedValues(form: Form[?], name: String): List[String] =
    form.data.toList.collect:
      case (key, value) if key == name || key.startsWith(s"$name[") => value

  private def metricLabelKeys(family: OpsMetricFamily): String =
    val keys = family.samples.flatMap(_.labels.keys).distinct.sorted
    if keys.nonEmpty then keys.mkString(", ") else "-"

  private def metricExample(family: OpsMetricFamily): String =
    family.samples.headOption.fold("-"): sample =>
      val labels =
        if sample.labels.nonEmpty then
          sample.labels.toList
            .sortBy(_._1)
            .map { case (key, value) => key + "=\"" + value + "\"" }
            .mkString("{", ", ", "}")
        else ""
      s"${sample.name}$labels ${sample.value}"
