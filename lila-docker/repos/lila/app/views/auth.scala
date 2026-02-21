package views

import lila.app.UiEnv.*
import lila.ui.Page
import play.api.data.Form
import scala.annotation.unused

object auth:

  val pubOrTor = span("This access is from a public or Tor network.")

  private def authHeader()(using @unused ctx: Context) =
    div(cls := "landing-header")(
      a(href := routes.Main.landing.url, cls := "logo")("Chesstory")
    )

  private def authFooter() =
    div(cls := "landing-footer")(
      div(cls := "footer-links")(
        span("© 2026 Chesstory"),
        a(href := routes.Main.privacy.url, cls := "btn-text")("Privacy"),
        a(href := routes.Main.terms.url, cls := "btn-text")("Terms")
      )
    )

  private def fieldError(form: Form[?], name: String): Frag =
    form(name).errors.headOption
      .map(e => div(cls := "auth-field-error")(e.message))
      .getOrElse(emptyFrag)

  private def globalErrors(form: Form[?]): Frag =
    frag(form.globalErrors.map(e => div(cls := "auth-error")(e.message))*)

  private def flashLine(success: Option[String], error: Option[String]): Frag =
    frag(
      success.map(msg => div(cls := "auth-success")(msg)),
      error.map(msg => div(cls := "auth-error")(msg))
    )

  private def authPage(
      title: String,
      captchaCsp: Boolean = false
  )(content: Context ?=> Frag)(using ctx: Context): Page =
    val base = Page(title).css("auth").flag(_.noHeader)
    val page =
      if captchaCsp then
        base.csp: csp =>
          csp.copy(
            scriptSrc = "https://hcaptcha.com" :: "https://*.hcaptcha.com" :: csp.scriptSrc,
            frameSrc = "https://hcaptcha.com" :: "https://*.hcaptcha.com" :: csp.frameSrc,
            styleSrc = "https://hcaptcha.com" :: "https://*.hcaptcha.com" :: csp.styleSrc,
            connectSrc = "https://hcaptcha.com" :: "https://*.hcaptcha.com" :: csp.connectSrc
          )
      else base
    page.wrap: _ =>
      main(cls := "auth-page")(
        div(cls := "landing-container")(
          authHeader(),
          div(cls := "auth-container")(div(cls := "auth-card")(content)),
          authFooter()
        )
      )

  def login(formData: Form[?], success: Option[String] = None)(using ctx: Context): Page =
    authPage("Log In - Chesstory"):
      val rememberChecked = formData("remember").value.forall(v => v == "true" || v == "on" || v == "1")
      frag(
        h1(cls := "auth-title")("Welcome back"),
        p(cls := "auth-subtitle")("Log in with username or email and password."),
        flashLine(success, none),
        globalErrors(formData),
        form(cls := "auth-form", method := "post", action := routes.Auth.authenticate.url)(
          div(cls := "form-group")(
            input(
              tpe := "text",
              name := "username",
              placeholder := "username or email",
              value := (formData("username").value | ""),
              autocomplete := "username",
              required,
              autofocus
            ),
            fieldError(formData, "username")
          ),
          div(cls := "form-group")(
            input(
              tpe := "password",
              name := "password",
              placeholder := "password",
              autocomplete := "current-password",
              required
            ),
            fieldError(formData, "password")
          ),
          label(cls := "auth-remember")(
            input(tpe := "checkbox", name := "remember", value := "true", if rememberChecked then checked := true else emptyFrag),
            span("Remember me")
          ),
          button(cls := "auth-submit", tpe := "submit")(
            "Log In",
            span(cls := "arrow")(" ->")
          )
        ),
        div(cls := "auth-links")(
          a(href := routes.Auth.passwordReset.url)("Forgot password?"),
          a(href := routes.Auth.signup.url)("Create account"),
          a(href := routes.Auth.magicLink.url)("Use magic link instead")
        )
      )

  def signup(formData: Form[?], captchaEnabled: Boolean, captchaSiteKey: Option[String])(using ctx: Context): Page =
    authPage("Sign Up - Chesstory", captchaCsp = captchaEnabled):
      frag(
        h1(cls := "auth-title")("Create your account"),
        p(cls := "auth-subtitle")("Sign up with email and password."),
        globalErrors(formData),
        form(id := "signup-form", cls := "auth-form", method := "post", action := routes.Auth.signupPost.url)(
          div(cls := "form-group")(
            input(
              tpe := "email",
              name := "email",
              placeholder := "your@email.com",
              value := (formData("email").value | ""),
              autocomplete := "email",
              required,
              autofocus
            ),
            fieldError(formData, "email")
          ),
          div(cls := "form-group")(
            input(
              tpe := "text",
              name := "username",
              placeholder := "username",
              value := (formData("username").value | ""),
              autocomplete := "username",
              required
            ),
            fieldError(formData, "username")
          ),
          div(cls := "form-group")(
            input(
              tpe := "password",
              name := "password",
              placeholder := "password",
              autocomplete := "new-password",
              required
            ),
            fieldError(formData, "password")
          ),
          if captchaEnabled then
            div(cls := "form-group captcha-group")(
              div(
                cls := "h-captcha",
                attr("data-sitekey") := (captchaSiteKey | "")
              ),
              fieldError(formData, "h-captcha-response"),
              raw("""<script src="https://hcaptcha.com/1/api.js" async defer></script>""")
            )
          else emptyFrag,
          button(cls := "auth-submit", tpe := "submit")(
            "Create Account",
            span(cls := "arrow")(" ->")
          )
        ),
        div(cls := "auth-links")(
          a(href := routes.Auth.login.url)("Already have an account? Log in"),
          a(href := routes.Auth.magicLink.url)("Use magic link instead")
        )
      )

  def passwordReset(error: Option[String] = None, success: Option[String] = None)(using ctx: Context): Page =
    authPage("Reset Password - Chesstory"):
      frag(
        h1(cls := "auth-title")("Reset your password"),
        p(cls := "auth-subtitle")("Enter your account email and we'll send a reset link."),
        flashLine(success, error),
        form(cls := "auth-form", method := "post", action := routes.Auth.passwordResetApply.url)(
          div(cls := "form-group")(
            input(
              tpe := "email",
              name := "email",
              placeholder := "your@email.com",
              autocomplete := "email",
              required,
              autofocus
            )
          ),
          button(cls := "auth-submit", tpe := "submit")(
            "Send Reset Link",
            span(cls := "arrow")(" ->")
          )
        ),
        div(cls := "auth-links")(
          a(href := routes.Auth.login.url)("Back to login"),
          a(href := routes.Auth.magicLink.url)("Use magic link")
        )
      )

  def passwordResetApply(token: String, error: Option[String] = None)(using ctx: Context): Page =
    authPage("Set New Password - Chesstory"):
      frag(
        h1(cls := "auth-title")("Set a new password"),
        p(cls := "auth-subtitle")("Choose a new password for your account."),
        flashLine(none, error),
        form(cls := "auth-form", method := "post", action := routes.Auth.passwordResetTokenApply(token).url)(
          div(cls := "form-group")(
            input(
              tpe := "password",
              name := "password",
              placeholder := "new password",
              autocomplete := "new-password",
              required,
              autofocus
            )
          ),
          button(cls := "auth-submit", tpe := "submit")(
            "Update Password",
            span(cls := "arrow")(" ->")
          )
        )
      )

  def passwordResetInvalid(message: String)(using ctx: Context): Page =
    authPage("Reset Link Error - Chesstory"):
      div(cls := "text-center")(
        div(cls := "auth-icon auth-error-icon")("x"),
        h1(cls := "auth-title")("Invalid Link"),
        p(cls := "auth-message")(message),
        a(href := routes.Auth.passwordReset.url, cls := "auth-submit")(
          "Request a new reset link",
          span(cls := "arrow")(" ->")
        )
      )

  def checkEmail(
      concealedEmail: String,
      success: Option[String] = None,
      error: Option[String] = None
  )(using ctx: Context): Page =
    authPage("Check Your Email - Chesstory"):
      frag(
        div(cls := "auth-icon auth-success-icon")("✓"),
        h1(cls := "auth-title")("Confirm your email"),
        p(cls := "auth-message")(
          "We sent a confirmation link to ",
          strong(concealedEmail),
          ".",
          br,
          "Open that email and click the link to activate your account."
        ),
        flashLine(success, error),
        form(cls := "auth-form", method := "post", action := routes.Auth.fixEmail.url)(
          div(cls := "form-group")(
            input(
              tpe := "email",
              name := "email",
              placeholder := "wrong email? enter a new one",
              autocomplete := "email",
              required
            )
          ),
          button(cls := "auth-submit", tpe := "submit")(
            "Resend confirmation",
            span(cls := "arrow")(" ->")
          )
        ),
        p(cls := "auth-note")(
          "You can also use ",
          a(href := routes.Auth.magicLink.url)("magic-link login"),
          " anytime."
        )
      )

  def magicLink(error: Option[String] = None)(using ctx: Context): Page =
    authPage("Magic Link - Chesstory"):
      frag(
        h1(cls := "auth-title")("Passwordless login"),
        p(cls := "auth-subtitle")("Enter your email and we'll send a secure magic link."),
        flashLine(none, error),
        form(cls := "auth-form", method := "post", action := routes.Auth.magicLinkApply.url)(
          div(cls := "form-group")(
            input(
              tpe := "email",
              name := "email",
              placeholder := "your@email.com",
              autocomplete := "email",
              required,
              autofocus
            )
          ),
          button(cls := "auth-submit", tpe := "submit")(
            "Send Magic Link",
            span(cls := "arrow")(" ->")
          )
        ),
        div(cls := "auth-links")(
          a(href := routes.Auth.login.url)("Use password login"),
          a(href := routes.Auth.signup.url)("Create account")
        )
      )

  def magicLinkSent()(using ctx: Context): Page =
    authPage("Check Your Email - Chesstory"):
      div(cls := "text-center")(
        div(cls := "auth-icon auth-success-icon")("✓"),
        h1(cls := "auth-title")("Check your email"),
        p(cls := "auth-message")(
          "We've sent a magic link to your email address.",
          br,
          "Click the link to log in."
        ),
        p(cls := "auth-note")(
          "Didn't receive it? Check your spam folder or ",
          a(href := routes.Auth.magicLink.url)("try again"),
          "."
        )
      )

  def magicLinkDev(url: String)(using ctx: Context): Page =
    authPage("Magic Link (Dev) - Chesstory"):
      div(cls := "text-center")(
        div(cls := "auth-icon auth-dev-icon")("D"),
        h1(cls := "auth-title")("Magic Link (Dev Mode)"),
        p(cls := "auth-message")("Email is mocked in this environment. Use the link below:"),
        div(cls := "auth-dev-link")(
          a(href := url, cls := "auth-submit")(
            "Log In Now",
            span(cls := "arrow")(" ->")
          )
        ),
        p(cls := "auth-note auth-url")(code(url))
      )

  def loginError(message: String)(using ctx: Context): Page =
    authPage("Login Error - Chesstory"):
      div(cls := "text-center")(
        div(cls := "auth-icon auth-error-icon")("x"),
        h1(cls := "auth-title")("Link Error"),
        p(cls := "auth-message")(message),
        a(href := routes.Auth.magicLink.url, cls := "auth-submit")(
          "Request New Link",
          span(cls := "arrow")(" ->")
        )
      )
