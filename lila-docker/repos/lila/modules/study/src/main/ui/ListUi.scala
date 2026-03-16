package lila.study
package ui

import scalalib.paginator.Paginator
import lila.core.study.StudyOrder
import lila.study.Study.WithChaptersAndLiked
import lila.ui.*
import ScalatagsTemplate.{ *, given }

// Chesstory: Aggressively simplified Study list UI
final class ListUi(helpers: Helpers, bits: StudyBits):
  import helpers.*

  private def notebookAllUrl(order: StudyOrder, page: Int): String =
    routes.Study.all(order, page).url

  private def notebookMineUrl(order: StudyOrder, page: Int): String =
    routes.Study.mine(order, page).url

  def all(pag: Paginator[WithChaptersAndLiked], order: StudyOrder)(using Context) =
    page(
      title = "Public notebooks",
      active = "all",
      order = order,
      pag = pag,
      url = order => notebookAllUrl(order, 1)
    )

  def mine(pag: Paginator[WithChaptersAndLiked], order: StudyOrder)(using Context) =
    page(
      title = "My notebooks",
      active = "mine",
      order = order,
      pag = pag,
      url = order => notebookMineUrl(order, 1)
    )

  private def page(
      title: String,
      active: String,
      order: StudyOrder,
      pag: Paginator[WithChaptersAndLiked],
      url: StudyOrder => String
  )(using Context): Page =
    val (eyebrow, lede, railTitle, railBody) =
      if active == "mine" then
        (
          "Your research shelf",
          "Reopen the analysis notebooks you built, keep annotating key sections, and keep polished lines ready to share.",
          "Analysis carry-over",
          "Carry over PGN trees, Bookmaker notes, and Chronicle summaries from analysis."
        )
      else
        (
          "Shared research boards",
          "Browse public notebooks built around opening ideas, middlegame plans, and reusable annotated sections.",
          "Section links",
          "Open a notebook directly on the saved section you want to review or share."
        )

    Page(title)
      .css("analyse.study.index")
      .js(Esm("analyse.study.index")):
        main(cls := "page-menu")(
          menu(active, order),
          main(cls := "page-menu__content study-index box")(
            div(cls := "box__top notebook-index__top")(
              div(cls := "notebook-index__intro")(
                span(cls := "notebook-index__eyebrow")(eyebrow),
                h1(title),
                p(cls := "notebook-index__lede")(lede)
              ),
              div(cls := "notebook-index__controls")(
                bits.orderSelect(order, active, url),
                bits.newForm()
              )
            ),
            div(cls := "notebook-index__proof")(
              proofCard("Section-first", "Keep each line as a reusable section instead of a disposable analysis tab."),
              proofCard("Shareable", "Send a direct notebook link to the exact saved section you want to discuss."),
              proofCard(railTitle, railBody)
            ),
            paginate(pag, active, url(order))
          )
        )

  private def proofCard(title: String, body: String) =
    div(cls := "notebook-index__proof-card")(
      strong(title),
      span(body)
    )

  private def paginate(pager: Paginator[WithChaptersAndLiked], active: String, url: String) =
    if pager.currentPageResults.isEmpty then
      div(cls := "nostudies")(
        bits.notebookGlyph("notebook", "notebook-empty__icon"),
        strong(if active == "mine" then "No notebooks yet" else "No public notebooks yet"),
        p(
          if active == "mine" then
            "Create one from analysis to keep sections, notes, and commentary together."
          else "Public notebooks will appear here once authors choose to share them."
        ),
        bits.newForm("Start a notebook")
      )
    else
      div(cls := "studies list infinite-scroll")(
        pager.currentPageResults.map { s =>
          div(cls := "study paginated")(bits.widget(s))
        },
        pagerNext(pager, np => addQueryParam(url, "page", np.toString))
      )

  def menu(active: String, order: StudyOrder)(using ctx: Context) =
    val nonMineOrder = if order == StudyOrder.mine then StudyOrder.hot else order
    def activeCls(key: String) = (active == key).option("active")
    lila.ui.bits.pageMenuSubnav(
      a(cls := activeCls("all"), href := notebookAllUrl(nonMineOrder, 1))("Browse notebooks"),
      ctx.isAuth.option:
        a(cls := activeCls("mine"), href := notebookMineUrl(StudyOrder.mine, 1))("My notebooks")
    )
