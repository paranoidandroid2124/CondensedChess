package views

import lila.app.UiEnv.{ *, given }
import lila.pref.{ PieceSet, PieceSet3d, Pref, Theme, Theme3d }
import scala.annotation.unused

object account:

  private def flashMessages(using ctx: Context) =
    val flashes = List(
      ctx.flash("success").map(msg => div(cls := "flash flash-success")(msg)),
      ctx.flash("warning").map(msg => div(cls := "flash flash-warning")(msg)),
      ctx.flash("error").map(msg => div(cls := "flash flash-error")(msg)),
      ctx.flash("failure").map(msg => div(cls := "flash flash-error")(msg))
    ).flatten
    frag(flashes*)

  private def settingsPage(title: String, activeTab: String)(content: Context ?=> scalatags.Text.all.Frag)(using ctx: Context) =
    Page(s"$title - Chesstory")
      .css("account")
      .wrap: _ =>
        main(cls := "account-page")(
          div(cls := "account-container")(
            div(cls := "account-sidebar")(
              div(cls := "account-title")("Settings"),
              st.nav(cls := "account-nav")(
                a(href := routes.Account.profile.url, cls := (if activeTab == "profile" then "active" else ""))("Profile"),
                a(href := routes.Pref.form("display").url, cls := (if activeTab == "preferences" then "active" else ""))("Preferences"),
                a(href := routes.Account.close.url, cls := (if activeTab == "close" then "active" else ""))("Close Account"),
                a(href := routes.Account.delete.url, cls := (if activeTab == "delete" then "active" else ""))("Delete All Data (GDPR)")
              )
            ),
            div(cls := "account-content")(
              div(cls := "account-card")(
                content
              )
            )
          )
        )

  def profile(user: User, form: play.api.data.Form[lila.core.userId.UserName])(using ctx: Context) =
    settingsPage("Profile Settings", "profile"):
      frag(
        h2("Profile Settings"),
        p(cls := "desc")("Manage your Chesstory identity."),
        hr,
        flashMessages,
        form.globalError.map: e =>
          div(cls := "form-error")(e.message)
        ,
        postForm(action := routes.Account.profileApply, cls := "account-form")(
          div(cls := "form-group")(
            label(attr("for") := "username")("Username"),
            input(
              id := "username",
              name := "username",
              tpe := "text",
              value := form("username").value | user.username.value,
              placeholder := "New username",
              required := true
            ),
            form("username").error.map: e =>
              span(cls := "field-error")(e.message)
            ,
            p(cls := "help")("You can change your username casing or minor formatting. Full name changes are not allowed.")
          ),
          div(cls := "form-group")(
            label("Email Address"),
            input(tpe := "email", value := user.email.map(_.value) | "", disabled := true),
            p(cls := "help")("Email is used for Magic Link login and cannot be changed.")
          ),
          div(cls := "form-actions")(
            button(tpe := "submit", cls := "btn-primary")("Save Changes")
          )
        )
      )

  def pref(
      @unused user: User,
      @unused form: play.api.data.Form[lila.pref.Pref],
      @unused categSlug: String
  )(using ctx: Context) =
    settingsPage("Preferences", "preferences"):
      val pref = ctx.pref
      frag(
        h2("Preferences"),
        p(cls := "desc")("Appearance settings for Chesstory."),
        hr,
        flashMessages,
        postForm(action := routes.Pref.formApply, cls := "account-form")(
          div(cls := "form-group")(
            label(attr("for") := "bg")("Background"),
            select(id := "bg", name := "bg")(
              Pref.Bg.choices.map: (bg, labelText) =>
                val v = Pref.Bg.asString.getOrElse(bg, "dark")
                option(value := v, if v == pref.currentBg then selected := true else emptyFrag)(labelText)
            ),
            p(cls := "help")("Controls the overall site background style.")
          ),
          div(cls := "form-group")(
            label(attr("for") := "theme")("Board theme (2D)"),
            select(id := "theme", name := "theme")(
              Theme.all.map: t =>
                option(value := t.name, if t.name == pref.theme then selected := true else emptyFrag)(t.name)
            )
          ),
          div(cls := "form-group")(
            label(attr("for") := "pieceSet")("Pieces (2D)"),
            select(id := "pieceSet", name := "pieceSet")(
              PieceSet.all.map: ps =>
                option(value := ps.name, if ps.name == pref.pieceSet then selected := true else emptyFrag)(ps.name)
            )
          ),
          div(cls := "form-group")(
            label(attr("for") := "theme3d")("Board theme (3D)"),
            select(id := "theme3d", name := "theme3d")(
              Theme3d.all.map: t =>
                option(value := t.name, if t.name == pref.theme3d then selected := true else emptyFrag)(t.name)
            )
          ),
          div(cls := "form-group")(
            label(attr("for") := "pieceSet3d")("Pieces (3D)"),
            select(id := "pieceSet3d", name := "pieceSet3d")(
              PieceSet3d.all.map: ps =>
                option(value := ps.name, if ps.name == pref.pieceSet3d then selected := true else emptyFrag)(ps.name)
            )
          ),
          div(cls := "form-actions")(
            button(tpe := "submit", cls := "btn-primary")("Save")
          )
        )
      )

  def close(@unused user: User)(using ctx: Context) =
    settingsPage("Close Account", "close"):
      frag(
        h2(cls := "danger")("Close Account"),
        p("Closing your account will disable your ability to log in and hide your profile."),
        div(cls := "warning-box")(
          strong("Warning: "),
          "You can re-enable your account later by logging in again within 30 days. After that, it might be permanently deactivated."
        ),
        postForm(action := routes.Account.closeConfirm, cls := "mt-2")(
          button(tpe := "submit", cls := "btn-danger")("I understand, close my account")
        )
      )

  def delete(@unused user: User)(using ctx: Context) =
    settingsPage("Permanent Data Deletion", "delete"):
      frag(
        h2(cls := "danger")("Permanent Data Deletion (GDPR)"),
        p("This action will permanently delete your account and all associated data from our servers."),
        div(cls := "danger-box")(
          strong("CRITICAL WARNING: "),
          "This action cannot be undone. All your analysis, studies, and account history will be wiped forever."
        ),
        postForm(action := routes.Account.deleteConfirm, cls := "mt-2")(
          div(cls := "form-group")(
            label(attr("for") := "confirm")("Type \"DELETE\" to confirm:"),
            input(id := "confirm", name := "confirm", tpe := "text", required := true, pattern := "DELETE")
          ),
          button(tpe := "submit", cls := "btn-danger")("Permanently delete everything")
        )
      )
