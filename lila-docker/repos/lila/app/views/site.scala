package views.site

import lila.app.UiEnv.*

object message:
  def temporarilyDisabled = Page("Disabled").wrap(_ =>
    div(cls := "box-pad")("Temporarily Disabled"))
  def apply(title: String)(content: scalatags.Text.all.Frag) =
    Page(title).wrap(_ => div(content))
  def rateLimited(msg: Option[String] = None) = Page("Rate Limited").wrap(_ =>
    div(cls := "box-pad")("Rate limited. " + msg.getOrElse("")))
  def authFailed = Page("Auth Failed").wrap(_ =>
    div(cls := "box-pad")("Authentication failed"))
  def serverError(msg: String) = Page("Server Error").wrap(_ =>
    div(cls := "box-pad")(s"Server error: $msg"))
  def noBot = Page("No Bot").wrap(_ =>
    div(cls := "box-pad")("Bot accounts not allowed"))
  def noLame = Page("Restricted").wrap(_ =>
    div(cls := "box-pad")("Account restricted"))
