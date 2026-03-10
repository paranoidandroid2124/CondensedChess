package views

import java.time.{ Instant, ZoneId }
import java.time.format.DateTimeFormatter

import lila.app.UiEnv.{ *, given }
import lila.core.perm.Permission
import lila.core.user.{ RoleDbKey, UserTier }
import lila.ops.*

object ops:

  private val zoneId = ZoneId.systemDefault
  private val instantFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm").withZone(zoneId)
  private val dateTimeLocalFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm")

  private val styles = raw(
    """<style>
      |.ops-page{max-width:1400px;margin:0 auto;padding:2rem 1.5rem 4rem;}
      |.ops-hero{display:flex;justify-content:space-between;align-items:flex-end;gap:1rem;margin-bottom:1.5rem;}
      |.ops-title{margin:0;font-size:2rem;}
      |.ops-subtitle{margin:.4rem 0 0;color:#6b7280;max-width:58rem;}
      |.ops-breadcrumbs{display:flex;gap:.65rem;flex-wrap:wrap;font-size:.95rem;margin-bottom:1rem;}
      |.ops-breadcrumbs a{color:#1d4ed8;text-decoration:none;}
      |.ops-flash{padding:.85rem 1rem;border-radius:14px;margin-bottom:1rem;border:1px solid transparent;}
      |.ops-flash.success{background:#ecfdf3;border-color:#a7f3d0;color:#166534;}
      |.ops-flash.error{background:#fef2f2;border-color:#fecaca;color:#b91c1c;}
      |.ops-grid{display:grid;grid-template-columns:minmax(0,2.2fr) minmax(320px,1fr);gap:1.25rem;align-items:start;}
      |.ops-main{display:grid;gap:1.25rem;}
      |.ops-side{display:grid;gap:1rem;position:sticky;top:1.5rem;}
      |.ops-card{background:#fff;border:1px solid #e5e7eb;border-radius:20px;padding:1.2rem;box-shadow:0 8px 24px rgba(15,23,42,.06);}
      |.ops-card h2,.ops-card h3{margin:0 0 .75rem;}
      |.ops-card h2{font-size:1.1rem;}
      |.ops-card h3{font-size:1rem;}
      |.ops-summary-grid{display:grid;grid-template-columns:repeat(4,minmax(0,1fr));gap:.9rem;}
      |.ops-stat{padding:.9rem 1rem;border-radius:16px;background:linear-gradient(180deg,#f8fafc,#eef2ff);}
      |.ops-stat-label{display:block;font-size:.8rem;color:#6b7280;margin-bottom:.35rem;text-transform:uppercase;letter-spacing:.04em;}
      |.ops-stat-value{font-weight:700;font-size:1rem;}
      |.ops-filter-grid{display:grid;grid-template-columns:repeat(4,minmax(0,1fr));gap:.8rem;}
      |.ops-field{display:grid;gap:.35rem;}
      |.ops-field label{font-size:.85rem;color:#4b5563;font-weight:600;}
      |.ops-field input,.ops-field select,.ops-field textarea{width:100%;border:1px solid #d1d5db;border-radius:12px;padding:.72rem .8rem;background:#fff;}
      |.ops-field textarea{min-height:84px;resize:vertical;}
      |.ops-actions{display:flex;gap:.75rem;flex-wrap:wrap;align-items:center;margin-top:.9rem;}
      |.ops-button{display:inline-flex;align-items:center;justify-content:center;gap:.35rem;border:none;border-radius:999px;padding:.72rem 1.05rem;font-weight:700;cursor:pointer;text-decoration:none;}
      |.ops-button.primary{background:#0f172a;color:#fff;}
      |.ops-button.secondary{background:#e5e7eb;color:#111827;}
      |.ops-button.danger{background:#991b1b;color:#fff;}
      |.ops-button.link{background:transparent;color:#1d4ed8;padding:0;}
      |.ops-table{width:100%;border-collapse:collapse;}
      |.ops-table th,.ops-table td{padding:.8rem .55rem;border-bottom:1px solid #edf2f7;text-align:left;vertical-align:top;}
      |.ops-table th{font-size:.78rem;text-transform:uppercase;letter-spacing:.05em;color:#6b7280;}
      |.ops-table a{color:#0f172a;text-decoration:none;font-weight:700;}
      |.ops-badges{display:flex;gap:.4rem;flex-wrap:wrap;}
      |.ops-badge{display:inline-flex;align-items:center;padding:.28rem .6rem;border-radius:999px;background:#eef2ff;color:#3730a3;font-size:.78rem;font-weight:700;}
      |.ops-badge.ok{background:#ecfdf3;color:#166534;}
      |.ops-badge.warn{background:#fef3c7;color:#92400e;}
      |.ops-badge.danger{background:#fef2f2;color:#b91c1c;}
      |.ops-timeline{display:grid;gap:.85rem;}
      |.ops-timeline-item{padding:1rem;border-radius:16px;background:#f8fafc;border:1px solid #e5e7eb;}
      |.ops-timeline-head{display:flex;justify-content:space-between;gap:1rem;align-items:flex-start;margin-bottom:.45rem;}
      |.ops-timeline-meta{font-size:.86rem;color:#6b7280;}
      |.ops-diff{margin:.6rem 0 0;padding-left:1rem;color:#475569;}
      |.ops-diff li{margin:.2rem 0;}
      |.ops-details{display:flex;gap:.45rem;flex-wrap:wrap;margin-top:.6rem;}
      |.ops-details span{font-size:.78rem;background:#fff;border:1px solid #dbe4f0;border-radius:999px;padding:.25rem .55rem;}
      |.ops-session-list{display:grid;gap:.75rem;}
      |.ops-session{padding:.95rem;border:1px solid #e5e7eb;border-radius:16px;background:#fff;}
      |.ops-session-head{display:flex;justify-content:space-between;gap:1rem;margin-bottom:.4rem;}
      |.ops-session-meta{font-size:.86rem;color:#6b7280;}
      |.ops-inline-form{display:grid;gap:.55rem;margin-top:.65rem;}
      |.ops-inline-form-row{display:flex;gap:.6rem;align-items:center;flex-wrap:wrap;}
      |.ops-inline-form-row input[type="text"]{flex:1 1 260px;}
      |.ops-empty{padding:1rem;border:1px dashed #d1d5db;border-radius:16px;color:#6b7280;background:#f8fafc;}
      |.ops-permission-grid{display:grid;grid-template-columns:repeat(2,minmax(0,1fr));gap:1rem;}
      |.ops-permission-list{display:grid;gap:.4rem;}
      |.ops-check{display:flex;gap:.55rem;align-items:flex-start;}
      |.ops-preview{border:1px solid #c7d2fe;background:#eef2ff;}
      |.ops-pagination{display:flex;justify-content:space-between;gap:1rem;align-items:center;margin-top:1rem;}
      |.ops-foot-links{display:flex;gap:.75rem;flex-wrap:wrap;}
      |@media (max-width: 1100px){
      |  .ops-grid{grid-template-columns:1fr;}
      |  .ops-side{position:static;}
      |  .ops-summary-grid,.ops-filter-grid,.ops-permission-grid{grid-template-columns:repeat(2,minmax(0,1fr));}
      |}
      |@media (max-width: 700px){
      |  .ops-page{padding:1.1rem .8rem 3rem;}
      |  .ops-hero{flex-direction:column;align-items:flex-start;}
      |  .ops-summary-grid,.ops-filter-grid,.ops-permission-grid{grid-template-columns:1fr;}
      |}
      |</style>""".stripMargin
  )

  private def pageShell(title: String)(content: Context ?=> Frag)(using ctx: Context): lila.ui.Page =
    Page(s"$title - Chesstory")
      .css("account")
      .transformHead(head => frag(head, styles))
      .apply:
        main(cls := "ops-page")(content)

  private def flashMessages(using ctx: Context) =
    frag(
      ctx.flash("success").map(msg => div(cls := "ops-flash success")(msg)),
      ctx.flash("error").map(msg => div(cls := "ops-flash error")(msg))
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

  private def pagination(page: Int, totalPages: Int, prevUrl: String, nextUrl: String) =
    div(cls := "ops-pagination")(
      span(cls := "ops-timeline-meta")(s"Page $page of $totalPages"),
      div(cls := "ops-foot-links")(
        if page > 1 then a(cls := "ops-button secondary", href := prevUrl)("Previous") else emptyFrag,
        if page < totalPages then a(cls := "ops-button secondary", href := nextUrl)("Next") else emptyFrag
      )
    )

  def members(
      page: OpsMemberSearchPage,
      pageUrl: Int => String
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
        flashMessages,
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
            table(cls := "ops-table")(
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
                    td(
                      a(href := routes.Ops.member(member.id))(
                        strong(member.username)
                      ),
                      div(cls := "ops-timeline-meta")(member.id.value),
                      div(cls := "ops-badges")(
                        member.hasTwoFactor.option(badge("2FA", "ok"))
                      )
                    ),
                    td(statusBadge(member.enabled)),
                    td(
                      div(cls := "ops-badges")(
                        badge(member.tier.name),
                        planBadge(member.planState())
                      ),
                      member.expiresAt.map(exp => div(cls := "ops-timeline-meta")(s"Expires ${renderInstant(exp)}"))
                    ),
                    td(member.email.getOrElse("No email")),
                    td(roleBadges(member.roles)),
                    td(
                      div(cls := "ops-timeline-meta")(renderInstant(member.createdAt)),
                      div(cls := "ops-timeline-meta")(s"Last seen ${renderOptionalInstant(member.seenAt)}")
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
      canUseAdvanced: Boolean,
      permissionPreview: Option[OpsPermissionPreview],
      selectedRawRoles: Set[RoleDbKey],
      permissionCategories: List[(String, List[Permission])]
  )(using ctx: Context): lila.ui.Page =
    val member = detail.member
    pageShell(s"Member Ops - ${member.username}"):
      frag(
        div(cls := "ops-breadcrumbs")(
          a(href := routes.Ops.members.url)("Member Ops"),
          span("/"),
          span(member.username),
          span("/"),
          a(href := routes.Ops.audit(member.id))("Full audit"),
          span("/"),
          a(href := routes.Ops.planLedger(member.id))("Plan ledger")
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
        flashMessages,
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
        div(cls := "ops-grid")(
          div(cls := "ops-main")(
            div(cls := "ops-card")(
              sectionHeader("Recent Audit", Some(routes.Ops.audit(member.id).url)),
              if detail.recentAudit.nonEmpty then renderAuditEntries(detail.recentAudit) else div(cls := "ops-empty")("No audit entries yet.")
            ),
            div(cls := "ops-card")(
              sectionHeader("Recent Plan Ledger", Some(routes.Ops.planLedger(member.id).url)),
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
                      postForm(action := routes.Ops.revokeSession(member.id, session._id.value), cls := "ops-inline-form")(
                        div(cls := "ops-inline-form-row")(
                          input(name := "reason", tpe := "text", placeholder := "Reason (required)", required := true),
                          button(tpe := "submit", cls := "ops-button secondary")("Revoke session")
                        )
                      )
                    )
                )
              else div(cls := "ops-empty")("No active sessions.")
            ),
            if showAdvanced && canUseAdvanced then
              div(cls := "ops-card")(
                h2("Advanced Permission Editor"),
                p(cls := "ops-subtitle")(
                  "Break-glass editor for the raw role set. Use preview before applying changes."
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
                postForm(action := routes.Ops.updatePermissions(member.id), cls := "ops-inline-form")(
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
                                if selectedRawRoles.contains(permission.dbKey) then checked := true else emptyFrag
                              ),
                              span(permission.name)
                            )
                        )
                      )
                  ),
                  div(cls := "ops-field")(
                    label(attr("for") := "raw-reason")("Reason"),
                    textarea(id := "raw-reason", name := "reason", placeholder := "Explain why raw permission changes are required.", required := true)
                  ),
                  label(cls := "ops-check")(
                    input(tpe := "checkbox", name := "confirm", value := "true"),
                    span("I reviewed the diff and want to apply these raw permissions.")
                  ),
                  div(cls := "ops-actions")(
                    button(tpe := "submit", cls := "ops-button primary")("Preview or apply"),
                    a(cls := "ops-button secondary", href := routes.Ops.member(member.id))("Close advanced editor")
                  )
                )
              )
            else if canUseAdvanced then
              div(cls := "ops-card")(
                h2("Advanced Permission Editor"),
                p(cls := "ops-subtitle")("Raw permission editing is gated separately from the daily role panel."),
                a(cls := "ops-button secondary", href := routes.Ops.member(member.id, advanced = true))("Open advanced editor")
              )
            else emptyFrag
          ),
          div(cls := "ops-side")(
            actionCard(
              "Account Status",
              s"Current state: ${if member.enabled then "enabled" else "disabled"}.",
              routes.Ops.updateStatus(member.id)
            )(
              input(tpe := "hidden", name := "enabled", value := (!member.enabled).toString),
              reasonInput("Explain why the member should be enabled or disabled."),
              button(
                tpe := "submit",
                cls := s"ops-button ${if member.enabled then "danger" else "primary"}"
              )(if member.enabled then "Disable account" else "Enable account")
            ),
            actionCard(
              "Change Email",
              s"Current login email: ${member.email.getOrElse("none")}",
              routes.Ops.updateEmail(member.id)
            )(
              div(cls := "ops-field")(
                label(attr("for") := "ops-email")("New email"),
                input(id := "ops-email", name := "email", tpe := "email", placeholder := "member@email.com", required := true)
              ),
              reasonInput("Why is the login email being changed?"),
              button(tpe := "submit", cls := "ops-button primary")("Update email")
            ),
            actionCard(
              "Password Reset",
              "Send a reset link to the member's current login email.",
              routes.Ops.sendPasswordReset(member.id)
            )(
              reasonInput("Why is a password reset being dispatched?"),
              button(tpe := "submit", cls := "ops-button primary")("Send password reset")
            ),
            actionCard(
              "Two-Factor",
              if member.hasTwoFactor then "Clear the stored TOTP secret." else "Two-factor is not currently enabled.",
              routes.Ops.clearTwoFactor(member.id)
            )(
              reasonInput("Why must the member's second factor be cleared?"),
              button(
                tpe := "submit",
                cls := "ops-button secondary",
                disabled := !member.hasTwoFactor
              )("Clear 2FA")
            ),
            actionCard(
              "Plan",
              s"Current plan: ${member.tier.name}.",
              routes.Ops.updatePlan(member.id)
            )(
              div(cls := "ops-field")(
                label(attr("for") := "ops-tier")("Tier"),
                select(id := "ops-tier", name := "tier")(
                  UserTier.values.toList.map(t => filterOption(t.name, t.name, member.tier.name))
                )
              ),
              div(cls := "ops-field")(
                label(attr("for") := "ops-expiry")("Expires at"),
                input(id := "ops-expiry", name := "expiresAt", tpe := "datetime-local", value := localDateTimeValue(member.expiresAt))
              ),
              reasonInput("Why is the plan being changed?"),
              button(tpe := "submit", cls := "ops-button primary")("Apply plan")
            ),
            actionCard(
              "Managed Roles",
              "Daily-use bundles and badges only. Legacy unmanaged roles remain untouched.",
              routes.Ops.updateRoles(member.id)
            )(
              div(cls := "ops-field")(
                label(attr("for") := "ops-bundle")("Ops bundle"),
                select(id := "ops-bundle", name := "bundle")(
                  filterOption("No bundle", "", detail.managedRoles.bundle.map(_.value).getOrElse("")),
                  OpsRoles.opsBundles.map(role => filterOption(OpsRoles.displayName(role), role.value, detail.managedRoles.bundle.map(_.value).getOrElse("")))
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
                        if detail.managedRoles.toggles.contains(role) then checked := true else emptyFrag
                      ),
                      span(OpsRoles.displayName(role))
                    )
                )
              ),
              div(cls := "ops-field")(
                label("Unmanaged roles"),
                if detail.managedRoles.unmanaged.nonEmpty then roleBadges(detail.managedRoles.unmanaged) else div(cls := "ops-empty")("None")
              ),
              reasonInput("Why are the curated member roles being changed?"),
              button(tpe := "submit", cls := "ops-button primary")("Save managed roles")
            ),
            actionCard(
              "Sessions",
              "Revoke every active session immediately.",
              routes.Ops.revokeAllSessions(member.id)
            )(
              reasonInput("Why are all sessions being revoked?"),
              button(tpe := "submit", cls := "ops-button secondary")("Revoke all sessions")
            )
          )
        )
      )

  def audit(member: OpsMemberState, auditPage: OpsAuditPage)(using ctx: Context): lila.ui.Page =
    pageShell(s"Audit - ${member.username}"):
      frag(
        div(cls := "ops-breadcrumbs")(
          a(href := routes.Ops.members.url)("Member Ops"),
          span("/"),
          a(href := routes.Ops.member(member.id))("Member detail"),
          span("/"),
          span("Audit")
        ),
        div(cls := "ops-hero")(
          div(
            h1(cls := "ops-title")(s"Audit for ${member.username}"),
            p(cls := "ops-subtitle")("Append-only history of operator actions and state diffs.")
          )
        ),
        flashMessages,
        div(cls := "ops-card")(
          if auditPage.entries.nonEmpty then renderAuditEntries(auditPage.entries) else div(cls := "ops-empty")("No audit entries yet."),
          pagination(
            auditPage.page,
            auditPage.totalPages,
            routes.Ops.audit(member.id, (auditPage.page - 1).max(1)).url,
            routes.Ops.audit(member.id, (auditPage.page + 1).min(auditPage.totalPages)).url
          )
        )
      )

  def planLedger(member: OpsMemberState, entries: List[UserPlanLedgerEntry])(using ctx: Context): lila.ui.Page =
    pageShell(s"Plan Ledger - ${member.username}"):
      frag(
        div(cls := "ops-breadcrumbs")(
          a(href := routes.Ops.members.url)("Member Ops"),
          span("/"),
          a(href := routes.Ops.member(member.id))("Member detail"),
          span("/"),
          span("Plan ledger")
        ),
        div(cls := "ops-hero")(
          div(
            h1(cls := "ops-title")(s"Plan ledger for ${member.username}"),
            p(cls := "ops-subtitle")("Manual plan grants, revocations, and expiry changes recorded from the ops console.")
          )
        ),
        flashMessages,
        div(cls := "ops-card")(
          if entries.nonEmpty then renderPlanEntries(entries) else div(cls := "ops-empty")("No plan ledger entries yet.")
        )
      )

  private def statCard(labelText: String, valueText: String) =
    div(cls := "ops-stat")(
      span(cls := "ops-stat-label")(labelText),
      span(cls := "ops-stat-value")(valueText)
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

  private def actionCard(title: String, text: String, submitCall: play.api.mvc.Call)(body: Frag*) =
    div(cls := "ops-card")(
      h2(title),
      p(cls := "ops-subtitle")(text),
      postForm(action := submitCall, cls := "ops-inline-form")(body)
    )

  private def reasonInput(placeholderText: String) =
    div(cls := "ops-field")(
      label("Reason"),
      textarea(name := "reason", placeholder := placeholderText, required := true)
    )
