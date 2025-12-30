package lila.study
package ui

import play.api.data.Form
import scalalib.paginator.Paginator

import lila.core.study.StudyOrder
import lila.study.Study.WithChaptersAndLiked
import lila.ui.*

import ScalatagsTemplate.{ *, given }

final class ListUi(helpers: Helpers, bits: StudyBits):
  import helpers.{ *, given }

  def all(pag: Paginator[WithChaptersAndLiked], order: StudyOrder)(using Context) =
    page(
      title = "All studies",
      active = "all",
      order = order,
      pag = pag,
      searchFilter = "",
      url = routes.Study.all(_, 1)
    )
      .hrefLangs(lila.ui.LangPath(routes.Study.allDefault()))

  def byOwner(pag: Paginator[WithChaptersAndLiked], order: StudyOrder, owner: User)(using Context) =
    page(
      title = s"Studies created by ${owner.username}",
      active = "owner",
      order = order,
      pag = pag,
      searchFilter = s"owner:${owner.username}",
      url = routes.Study.byOwner(owner.username, _, 1)
    )

  def mine(pag: Paginator[WithChaptersAndLiked], order: StudyOrder, topics: StudyTopics)(using
      ctx: Context,
      me: Me
  ) =
    page(
      title = "My studies",
      active = "mine",
      order = order,
      pag = pag,
      searchFilter = s"owner:${me.username}",
      url = routes.Study.mine(_, 1),
      topics = topics.some
    )

  def mineLikes(
      pag: Paginator[WithChaptersAndLiked],
      order: StudyOrder
  )(using Context) =
    page(
      title = "My favorite studies",
      active = "mineLikes",
      order = order,
      pag = pag,
      searchFilter = "",
      url = routes.Study.mineLikes(_, 1)
    )

  def mineMember(pag: Paginator[WithChaptersAndLiked], order: StudyOrder, topics: StudyTopics)(using
      ctx: Context,
      me: Me
  ) =
    page(
      title = "Studies I contribute to",
      active = "mineMember",
      order = order,
      pag = pag,
      searchFilter = s"member:${me.username}",
      url = routes.Study.mineMember(_, 1),
      topics = topics.some
    )

  def minePublic(pag: Paginator[WithChaptersAndLiked], order: StudyOrder)(using Context)(using me: Me) =
    page(
      title = "My public studies",
      active = "minePublic",
      order = order,
      pag = pag,
      searchFilter = s"owner:${me.username}",
      url = routes.Study.minePublic(_, 1)
    )

  def minePrivate(pag: Paginator[WithChaptersAndLiked], order: StudyOrder)(using Context)(using me: Me) =
    page(
      title = "My private studies",
      active = "minePrivate",
      order = order,
      pag = pag,
      searchFilter = s"owner:${me.username}",
      url = routes.Study.minePrivate(_, 1)
    )

  def search(pag: Paginator[WithChaptersAndLiked], order: StudyOrder, text: String)(using Context) =
    Page(text)
      .css("analyse.study.index")
      .js(Esm("analyse.study.index")):
        main(cls := "page-menu")(
          menu("search", Orders.default),
          main(cls := "page-menu__content study-index box")(
            div(cls := "box__top")(
              searchForm("Search studies", text, order),
              bits.orderSelect(order, "search", url = o => routes.Study.search(text, 1, o.some)),
              bits.newForm()
            ),
            paginate(pag, routes.Study.search(text, 1, order.some))
          )
        )

  private def page(
      title: String,
      active: String,
      order: StudyOrder,
      pag: Paginator[WithChaptersAndLiked],
      url: StudyOrder => Call,
      searchFilter: String,
      topics: Option[StudyTopics] = None
  )(using Context): Page =
    Page(title)
      .css("analyse.study.index")
      .js(Esm("analyse.study.index")):
        main(cls := "page-menu")(
          menu(active, order, topics.so(_.value)),
          main(cls := "page-menu__content study-index box")(
            div(cls := "box__top")(
              searchForm(title, s"$searchFilter${searchFilter.nonEmpty.so(" ")}", order),
              bits.orderSelect(order, active, url),
              bits.newForm()
            ),
            topics.map: ts =>
              div(cls := "box__pad")(topic.topicsList(ts, StudyOrder.mine)),
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

  def menu(active: String, order: StudyOrder, topics: List[StudyTopic] = Nil)(using ctx: Context) =
    val nonMineOrder = if order == StudyOrder.mine then StudyOrder.hot else order
    def activeCls(key: String) = (active == key || active.startsWith(s"$key:")).option("active")
    lila.ui.bits.pageMenuSubnav(
      a(cls := activeCls("all"), href := routes.Study.all(nonMineOrder, 1))("All studies"),
      ctx.isAuth.option(bits.authLinks(active, nonMineOrder)),
      a(cls := List("active" -> active.startsWith("topic")), href := routes.Study.topics):
        "Topics"
      ,
      topics.map: topic =>
        a(cls := activeCls(s"topic:$topic"), href := routes.Study.byTopic(topic.value, order, 1))(
          topic.value
        ),
      a(cls := activeCls("staffPicks"), href := routes.Study.staffPicks)("Staff picks"),
      a(
        cls := "text",
        dataIcon := Icon.InfoCircle,
        href := "/@/lichess/blog/study-chess-the-lichess-way/V0KrLSkA"
      )("What are studies?")
    )

  def searchForm(placeholder: String, initValue: String, order: StudyOrder) =
    form(cls := "search", action := routes.Study.search(), method := "get")(
      input(tpe := "hidden", name := "order", value := order.key),
      input(name := "q", st.placeholder := placeholder, st.value := initValue, enterkeyhint := "search"),
      submitButton(cls := "button", dataIcon := Icon.Search)
    )

  object topic:

    def topicsList(topics: StudyTopics, order: StudyOrder = Orders.default) =
      div(cls := "topic-list")(
        topics.value.map: t =>
          a(href := routes.Study.byTopic(t.value, order, 1))(t.value)
      )

    def index(popular: StudyTopics, mine: Option[StudyTopics], myForm: Option[Form[?]])(using Context) =
      Page("Study topics")
        .css("analyse.study.index", "bits.form3", "bits.tagify")
        .js(Esm("analyse.study.topic.form")):
          main(cls := "page-menu")(
            menu("topic", StudyOrder.mine, mine.so(_.value)),
            main(cls := "page-menu__content study-topics box box-pad")(
              h1(cls := "box__top")("Study topics"),
              myForm.map { form =>
                frag(
                  h2("My topics"),
                  postForm(cls := "form3", action := routes.Study.topics)(
                    textarea(
                      name := "topics",
                      rows := 10,
                      attrData("max") := StudyTopics.userMax
                    )(form("topics").value.getOrElse("")),
                    submitButton("Save")
                  )
                )
              },
              h2("Popular topics"),
              topicsList(popular)
            )
          )

    def show(
        topic: StudyTopic,
        pag: Paginator[WithChaptersAndLiked],
        order: StudyOrder,
        myTopics: Option[StudyTopics]
    )(using Context) =
      val active = s"topic:$topic"
      val url = (o: StudyOrder) => routes.Study.byTopic(topic.value, o, 1)
      Page(topic.value)
        .css("analyse.study.index")
        .js(Esm("analyse.study.index")):
          main(cls := "page-menu")(
            menu(active, order, myTopics.so(_.value)),
            main(cls := "page-menu__content study-index box")(
              boxTop(
                h1(topic.value),
                bits.orderSelect(order, active, url),
                bits.newForm()
              ),
              myTopics.ifTrue(order == StudyOrder.mine).map { ts =>
                div(cls := "box__pad")(
                  topicsList(ts, StudyOrder.mine)
                )
              },
              paginate(pag, url(order))
            )
          )
