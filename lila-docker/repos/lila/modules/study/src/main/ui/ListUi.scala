package lila.study
package ui

import scalalib.paginator.Paginator
import lila.core.study.StudyOrder
import lila.study.Study.WithChaptersAndLiked
import lila.ui.*
import ScalatagsTemplate.{ *, given }

// Chesstory: Aggressively simplified Study list UI
final class ListUi(helpers: Helpers, bits: StudyBits):
  import helpers.{ *, given }

  def all(pag: Paginator[WithChaptersAndLiked], order: StudyOrder)(using Context) =
    page(
      title = "All studies",
      active = "all",
      order = order,
      pag = pag,
      url = routes.Study.all(_, 1)
    )

  def mine(pag: Paginator[WithChaptersAndLiked], order: StudyOrder)(using ctx: Context, me: Me) =
    page(
      title = "My studies",
      active = "mine",
      order = order,
      pag = pag,
      url = routes.Study.mine(_, 1)
    )

  private def page(
      title: String,
      active: String,
      order: StudyOrder,
      pag: Paginator[WithChaptersAndLiked],
      url: StudyOrder => Call
  )(using Context): Page =
    Page(title)
      .css("analyse.study.index")
      .js(Esm("analyse.study.index")):
        main(cls := "page-menu")(
          menu(active, order),
          main(cls := "page-menu__content study-index box")(
            div(cls := "box__top")(
              h1(title),
              bits.orderSelect(order, active, url),
              bits.newForm()
            ),
            paginate(pag, url(order))
          )
        )

  private def paginate(pager: Paginator[WithChaptersAndLiked], url: Call)(using Context) =
    if pager.currentPageResults.isEmpty then
      div(cls := "nostudies")(
        iconTag(Icon.StudyBoard),
        p("No studies yet")
      )
    else
      div(cls := "studies list infinite-scroll")(
        pager.currentPageResults.map { s =>
          div(cls := "study paginated")(bits.widget(s))
        },
        pagerNext(pager, np => addQueryParam(url.url, "page", np.toString))
      )

  def menu(active: String, order: StudyOrder)(using ctx: Context) =
    val nonMineOrder = if order == StudyOrder.mine then StudyOrder.hot else order
    def activeCls(key: String) = (active == key).option("active")
    lila.ui.bits.pageMenuSubnav(
      a(cls := activeCls("all"), href := routes.Study.all(nonMineOrder, 1))("All studies"),
      ctx.isAuth.option:
        a(cls := activeCls("mine"), href := routes.Study.mine(StudyOrder.mine, 1))("My studies")
    )
