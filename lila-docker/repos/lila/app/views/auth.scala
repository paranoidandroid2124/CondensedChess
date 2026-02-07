package views

import lila.app.UiEnv.{ *, given }
import lila.ui.Page

object auth:

  // Keep existing for backward compatibility
  val pubOrTor = span("This access is from a public or Tor network.")

  /** Common header for auth pages - matches landing page style */
  private def authHeader()(using ctx: Context) =
    div(cls := "landing-header")(
      a(href := routes.Main.landing.url, cls := "logo")("Chesstory")
    )

  /** Common footer for auth pages */
  private def authFooter() =
    div(cls := "landing-footer")(
      div(cls := "footer-links")(
        span("© 2026 Chesstory"),
        a(href := routes.Main.privacy.url, cls := "btn-text")("Privacy"),
        a(href := routes.Main.terms.url, cls := "btn-text")("Terms")
      )
    )

  /** Magic link login form */
  def magicLink(error: Option[String] = None)(using ctx: Context): Page =
    Page("Log In - Chesstory")
      .css("auth")
      .flag(_.noHeader)
      .wrap: _ =>
        main(cls := "auth-page")(
          div(cls := "landing-container")(
            authHeader(),
            div(cls := "auth-container")(
              div(cls := "auth-card")(
                h1(cls := "auth-title")("Welcome back"),
                p(cls := "auth-subtitle")(
                  "Enter your email and we'll send you a magic link to log in."
                ),
                error.map: msg =>
                  div(cls := "auth-error")(msg),
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
                    span(cls := "arrow")(" →")
                  )
                ),
                p(cls := "auth-note")(
                  "No password needed. We'll email you a secure link."
                )
              )
            ),
            authFooter()
          )
        )

  /** Magic link sent confirmation */
  def magicLinkSent()(using ctx: Context): Page =
    Page("Check Your Email - Chesstory")
      .css("auth")
      .flag(_.noHeader)
      .wrap: _ =>
        main(cls := "auth-page")(
          div(cls := "landing-container")(
            authHeader(),
            div(cls := "auth-container")(
              div(cls := "auth-card text-center")(
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
            ),
            authFooter()
          )
        )

  /** Dev mode: shows magic link directly (only in non-prod with mock email) */
  def magicLinkDev(url: String)(using ctx: Context): Page =
    Page("Magic Link (Dev) - Chesstory")
      .css("auth")
      .flag(_.noHeader)
      .wrap: _ =>
        main(cls := "auth-page")(
          div(cls := "landing-container")(
            authHeader(),
            div(cls := "auth-container")(
              div(cls := "auth-card text-center")(
                div(cls := "auth-icon auth-dev-icon")("🔧"),
                h1(cls := "auth-title")("Magic Link (Dev Mode)"),
                p(cls := "auth-message")(
                  "Email sending is in mock mode. Use this link to log in:"
                ),
                div(cls := "auth-dev-link")(
                  a(href := url, cls := "auth-submit")(
                    "Log In Now",
                    span(cls := "arrow")(" →")
                  )
                ),
                p(cls := "auth-note auth-url")(
                  code(url)
                )
              )
            ),
            authFooter()
          )
        )

  /** Login error (expired/invalid token) */
  def loginError(message: String)(using ctx: Context): Page =
    Page("Login Error - Chesstory")
      .css("auth")
      .flag(_.noHeader)
      .wrap: _ =>
        main(cls := "auth-page")(
          div(cls := "landing-container")(
            authHeader(),
            div(cls := "auth-container")(
              div(cls := "auth-card text-center")(
                div(cls := "auth-icon auth-error-icon")("✗"),
                h1(cls := "auth-title")("Link Expired"),
                p(cls := "auth-message")(message),
                a(href := routes.Auth.magicLink.url, cls := "auth-submit")(
                  "Request New Link",
                  span(cls := "arrow")(" →")
                )
              )
            ),
            authFooter()
          )
        )
