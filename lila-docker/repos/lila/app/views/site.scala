package views.site

import lila.app.UiEnv.{ *, given }

object message:
  def temporarilyDisabled(using Context) = Page("Disabled").wrap(_ =>
    div(cls := "box-pad")("Temporarily Disabled"))
  def apply(title: String, icon: Option[Any] = None, back: Option[String] = None)(content: scalatags.Text.all.Frag)(using Context) = 
    Page(title).wrap(_ => div(content))
  def rateLimited(msg: Option[String] = None)(using Context) = Page("Rate Limited").wrap(_ =>
    div(cls := "box-pad")("Rate limited. " + msg.getOrElse("")))
  def authFailed(using Context) = Page("Auth Failed").wrap(_ =>
    div(cls := "box-pad")("Authentication failed"))
  def serverError(msg: String)(using Context) = Page("Server Error").wrap(_ =>
    div(cls := "box-pad")(s"Server error: $msg"))
  def noBot(using Context) = Page("No Bot").wrap(_ =>
    div(cls := "box-pad")("Bot accounts not allowed"))
  def noLame(using Context) = Page("Restricted").wrap(_ =>
    div(cls := "box-pad")("Account restricted"))

object ui:
  def lag(using Context) = Page("Lag").wrap(_ =>
    div(cls := "box-pad")("Lag"))
  def getFishnet(using Context) = Page("Fishnet").wrap(_ =>
    div(cls := "box-pad")("Fishnet"))
  def SitePage(title: String, active: String, contentCls: String)(using Context) = 
    Page(title).wrap(_ => div(cls := contentCls))
  def notFound(msg: Option[String])(using Context) = Page("Not Found").wrap(_ => div(msg.getOrElse("Not Found")))

object page:
  def withMenu(active: String, title: String, content: String)(using Context) =
    Page(title).wrap: _ =>
      div(cls := "page box box-pad")(raw(content))

  def contact(using Context) = Page("Contact").wrap: _ =>
    div(cls := "box-pad")("Contact")

  def webmasters(using Context) = Page("Webmasters").wrap: _ =>
    div(cls := "box-pad")("Webmasters")

  val faq = new:
    def apply(using Context) = Page("FAQ").wrap: _ =>
      div(cls := "box-pad")("FAQ")

def SitePages(helpers: lila.app.UiEnv.type) = ui
def SiteMessage(helpers: lila.app.UiEnv.type) = message
def FaqUi(helpers: lila.app.UiEnv.type, ui: Any)(
    standardRankableDeviation: Double,
    variantRankableDeviation: Double
) = page.faq
def contact(email: String)(using Context) = div("Contact " + email)
