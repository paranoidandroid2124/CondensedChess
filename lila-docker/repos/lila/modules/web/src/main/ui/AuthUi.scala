package lila.web
package ui

import play.api.data.{ Field, Form }

import lila.common.HTTPRequest
import lila.ui.*

import ScalatagsTemplate.{ *, given }

final class AuthUi(helpers: Helpers):
  import helpers.{ *, given }

  def login(form: Form[?], referrer: Option[String], isRememberMe: Boolean = true)(using Context) =
    def addReferrer(url: String): String = referrer.fold(url)(addQueryParam(url, "referrer", _))
    Page("Sign in")
      .css("bits.auth")
      .hrefLangs(lila.ui.LangPath(routes.Auth.login)):
        main(cls := "auth auth-login box box-pad")(
          h1(cls := "box__top")("Sign in"),
          postForm(
            cls := "form3",
            action := addReferrer(routes.Auth.authenticate.url)
          )(
            div(cls := "one-factor")(
              if form.globalError.exists(_.messages.contains("blankedPassword")) then
                div(cls := "auth-login__blanked")(
                  p("Your password was reset for security reasons. Please choose a new one."),
                  a(href := routes.Auth.passwordReset, cls := "button button-no-upper")(
                    "Password reset"
                  )
                )
              else form3.globalError(form),
              formFields(form("username"), form("password"), none, register = false),
              form3.submit("Sign in", icon = none),
              label(cls := "login-remember")(
                input(
                  name := "remember",
                  value := "true",
                  tpe := "checkbox",
                  isRememberMe.option(checked)
                ),
                "Remember me"
              )
            ),
            div(cls := "two-factor none")(
              form3.group(
                form("token"),
                "Authentication code",
                help = Some(span(dataIcon := Icon.PhoneMobile)("Open your two-factor app"))
              )(
                form3.input(_)(
                  attr("inputmode") := "numeric",
                  autocomplete := "one-time-code",
                  pattern := "[0-9]{6}"
                )
              ),
              p(cls := "error none")("Invalid code."),
              form3.submit("Sign in", icon = none)
            )
          ),
          div(cls := "alternative")(
            a(href := addReferrer(langHref(routes.Auth.signup)))("Sign up"),
            a(href := routes.Auth.passwordReset)("Password reset"),
            a(href := routes.Auth.magicLink)("Log in by email")
          )
        )

  def signup(form: lila.core.security.HcaptchaForm[?])(using ctx: Context) =
    Page("Sign up")
      .css("bits.auth")
      .csp(_.withHcaptcha)
      .hrefLangs(lila.ui.LangPath(routes.Auth.signup)):
        main(cls := "auth auth-signup box box-pad")(
          h1(cls := "box__top")("Sign up"),
          postForm(
            id := "signup-form",
            cls := List(
              "form3" -> true,
              "h-captcha-enabled" -> form.enabled
            ),
            action := HTTPRequest.queryStringGet(ctx.req, "referrer").foldLeft(routes.Auth.signupPost.url) {
              (url, ref) => addQueryParam(url, "referrer", ref)
            }
          )(
            formFields(form("username"), form("password"), form("email").some, register = true),
            globalErrorNamed(form.form, "error.namePassword"),
            input(id := "signup-fp-input", name := "fp", tpe := "hidden"),
            div(cls := "form-group text", dataIcon := Icon.InfoCircle)(
              "Computers and engine assistance are not allowed to play.",
              br,
              small(
                "By registering, you agree to be bound by our Terms of Service.",
                br,
                "Read about our Privacy Policy.",
                br
              )
            ),
            agreement(form("agreement"), form.form.errors.exists(_.key.startsWith("agreement."))),
            lila.ui.bits.hcaptcha(form),
            button(cls := "submit button text big")("Sign up")
          )
        )

  def checkYourEmail(
      email: Option[lila.core.email.EmailAddress],
      form: Option[Form[?]] = None
  )(using ctx: Context) =
    Page("Check your email")
      .css("bits.email-confirm"):
        main(
          cls := s"page-small box box-pad email-confirm ${
              if form.exists(_.hasErrors) then "error" else "anim"
            }"
        )(
          boxTop(h1(cls := "is-green text", dataIcon := Icon.Checkmark)("Check your email")),
          p("We have sent you an email. Click the link in the email to activate your account."),
          h2("Not receiving it?"),
          ol(
            li(h3("If you do not see the email, check other places it might be, like your junk, spam, social, or other folders.")),
            email.map { email =>
              li(
                h3("Make sure your email address is correct:"),
                br,
                br,
                postForm(action := routes.Auth.fixEmail)(
                  input(
                    id := "new-email",
                    tpe := "email",
                    required,
                    name := "email",
                    value := form.flatMap(_("email").value).getOrElse(email.value),
                    pattern := s"^((?!^${email.value}$$).)*$$"
                  ),
                  submitButton(cls := "button")("Change it"),
                  form.map: f =>
                    errMsg(f("email"))
                )
              )
            },
            li(
              h3("Wait up to 5 minutes."),
              br,
              "Depending on your email provider, it can take a while to arrive."
            ),
            li(
              h3("Still not getting it?"),
              br,
              "Did you make sure your email address is correct?",
              br,
              "Did you wait 5 minutes?",
              br,
              "If so, please contact support."
            )
          )
        )

  def signupConfirm(user: lila.core.user.User, token: String)(using Context) =
    Page("Sign up")
      .css("bits.email-confirm"):
        main(cls := "page-small box box-pad signup-confirm")(
          h1(iconFlair("activity.party-popper"), "Welcome to Lichess!"),
          postForm(action := routes.Auth.signupConfirmEmailPost(token)):
            submitButton(cls := "button button-fat button-no-upper")(
              s"Log in as ${user.username}"
            )
        )

  def passwordReset(form: lila.core.security.HcaptchaForm[?], fail: Option[String])(using Context) =
    Page("Password reset")
      .css("bits.auth")
      .csp(_.withHcaptcha):
        main(cls := "auth auth-signup box box-pad")(
          boxTop(
            h1(
              fail.isDefined.option(span(cls := "is-red", dataIcon := Icon.X)),
              "Password reset"
            )
          ),
          postForm(cls := "form3", action := routes.Auth.passwordResetApply)(
            fail.map(p(cls := "error")(_)),
            form3.group(form("email"), "Email", help = None)(
              form3.input(_, typ = "email")(autofocus, required, autocomplete := "email")
            ),
            lila.ui.bits.hcaptcha(form),
            form3.action(form3.submit("Email me a link"))
          )
        )

  def passwordResetSent(email: String)(using Context) =
    Page("Password reset").css("bits.auth"):
      main(cls := "page-small box box-pad")(
        boxTop(h1(cls := "is-green text", dataIcon := Icon.Checkmark)("Check your email")),
        p(s"We have sent you an email to $email."),
        p("If you do not get the email:"),
        ul(cls := "checklist")(
          li("Check all your email folders, including spam."),
          li(s"Verify that your email address is correct: $email")
        )
      )

  def passwordResetConfirm(token: String, form: Form[?], ok: Option[Boolean] = None)(using
      Context
  )(using me: Me) =
    Page(s"${me.username} - Change password")
      .css("bits.auth"):
        main(cls := "page-small box box-pad")(
          boxTop(
            (ok match
              case Some(true) => h1(cls := "is-green text", dataIcon := Icon.Checkmark)
              case Some(false) => h1(cls := "is-red text", dataIcon := Icon.X)
              case _ => h1
            )(
              userLink(me, withOnline = false),
              " - ",
              "Change password"
            )
          ),
          postForm(cls := "form3", action := routes.Auth.passwordResetConfirmApply(token))(
            form3.hidden(form("token")),
            form3.passwordModified(form("newPasswd1"), "New password")(
              autofocus,
              autocomplete := "new-password"
            ),
            form3.passwordComplexityMeter("New password strength"),
            form3.passwordModified(form("newPasswd2"), "New password again")(
              autocomplete := "new-password"
            ),
            form3.globalError(form),
            form3.action(form3.submit("Change password"))
          )
        )

  def magicLink(form: lila.core.security.HcaptchaForm[?], fail: Boolean)(using Context) =
    Page("Log in by email")
      .css("bits.auth")
      .csp(_.withHcaptcha):
        main(cls := "auth auth-signup box box-pad")(
          boxTop(
            h1(
              fail.option(span(cls := "is-red", dataIcon := Icon.X)),
              "Log in by email"
            )
          ),
          p("We will send you an email containing a link to log you in."),
          postForm(cls := "form3", action := routes.Auth.magicLinkApply)(
            form3.group(form("email"), "Email", help = None)(
              form3.input(_, typ = "email")(autofocus, required, autocomplete := "email")
            ),
            lila.ui.bits.hcaptcha(form),
            form3.action(form3.submit("Email me a link"))
          )
        )

  def magicLinkSent(using Context) =
    Page("Log in by email"):
      main(cls := "page-small box box-pad")(
        boxTop(h1(cls := "is-green text", dataIcon := Icon.Checkmark)("Check your email")),
        p("We've sent you an email with a link."),
        p("If you do not see the email, check other places it might be.")
      )

  def tokenLoginConfirmation(user: lila.core.user.User, token: String, referrer: Option[String])(using Context) =
    Page(s"Log in as ${user.username}").css("bits.form3"):
      main(cls := "page-small box box-pad")(
        boxTop(h1("Log in as ", userLink(user))),
        postForm(action := routes.Auth.loginWithTokenPost(token, referrer))(
          form3.actions(
            a(href := routes.UserAnalysis.index)("Cancel"),
            submitButton(cls := "button")(s"${user.username} is my Lichess username, log me in")
          )
        )
      )

  def checkYourEmailBanner(user: lila.core.userId.UserName, email: lila.core.email.EmailAddress) =
    div(cls := "email-confirm-banner")(
      s"Almost there, $user! Now check your email (${email.conceal}) for signup confirmation.",
      a(href := routes.Auth.checkYourEmail)("Need help?")
    )

  def pubOrTor =
    Page("Public proxy"):
      main(cls := "page-small box box-pad")(
        boxTop(h1(cls := "text")("Ooops")),
        p("Sorry, you can't signup to Lichess through Tor or public proxies!"),
        p("You can play, train and use almost all Lichess features as an anonymous user.")
      )

  def logout(using Context) =
    Page("Log out"):
      main(cls := "page-small box box-pad")(
        h1(cls := "box__top")("Log out"),
        form(action := routes.Auth.logout, method := "post")(
          button(cls := "button button-red", tpe := "submit")("Log out")
        )
      )

  private def agreement(form: play.api.data.Field, error: Boolean)(using Context) =
    div(cls := "agreement")(
      error.option(p:
        strong(cls := "error"):
          "You must agree to the Lichess policies listed below:"),
      agreements.map: (field, text) =>
        form3.checkbox(form(field), text)
    )

  private def agreements(using Context) = List(
    "assistance" -> "I agree that I will never receive any help during my games (from a chess engine, a database, another player, etc).",
    "nice" -> "I agree that I will always be nice and respectful to other players.",
    "account" -> "I agree that I will not create multiple accounts, except as permitted by our Terms of Service.",
    "policy" -> "I agree that I will follow all Lichess policies."
  )

  private def formFields(username: Field, password: Field, email: Option[Field], register: Boolean)(using
      Context
  ) =
    frag(
      form3.group(
        username,
        if register then "Username" else "Username or Email",
        help = if register then Some("Letters and numbers only, 2 to 20 characters.") else None
      ): f =>
        frag(
          form3.input(f)(autofocus, required, autocomplete := "username"),
          register.option(p(cls := "error username-exists none")("This username is already used."))
        ),
      form3.passwordModified(password, "Password")(
        autocomplete := (if register then "new-password" else "current-password")
      ),
      register.option(form3.passwordComplexityMeter("New password strength")),
      email.map: email =>
        form3.group(email, "Email", help = Some("Required for account activation and password recovery."))(
          form3.input(_, typ = "email")(required)
        )
    )
