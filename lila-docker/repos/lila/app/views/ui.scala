package views

import lila.app.UiEnv.{ *, given }

object ui:
  val captcha = new:
    def any = div("Captcha")

  val boardEditor = new:
    def apply(args: Any*) = div("Board Editor")

  val atomUi = new:
    def apply(args: Any*) = div("Atom UI")

  object oAuth:
    val token = new:
      def apply(args: Any*) = div("Token UI")
    val authorize = new:
      def apply(args: Any*) = div("Authorize UI")

  val auth = new:
    def apply(args: Any*) = div("Auth UI")
