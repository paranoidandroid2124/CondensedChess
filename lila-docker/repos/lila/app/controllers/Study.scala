package controllers

import play.api.libs.json.*
import play.api.mvc.*
import scalalib.Json.given
import scalalib.paginator.Paginator
import scalatags.Text.all.*

import lila.analyse.Analysis
import lila.app.{ *, given }
import lila.common.{ Bus, HTTPRequest }
import lila.core.id.RelayRoundId
import lila.core.misc.lpv.LpvEmbed
import lila.core.net.IpAddress
import lila.core.study.StudyOrder
import lila.core.data.ErrorMsg
import lila.study.JsonView.JsData
import lila.study.PgnDump.WithFlags
import lila.study.Study.WithChapter
import lila.study.{ BecomeStudyAdmin, Who }
import lila.study.{ Chapter, Orders, Settings, Study as StudyModel, StudyForm }
import lila.tree.Node.partitionTreeJsonWriter
import com.fasterxml.jackson.core.JsonParseException
import lila.ui.Page

final class Study(
    env: Env,
    editorC: => Editor,
    userAnalysisC: => UserAnalysis
) extends LilaController(env):

  def search(text: String, page: Int, order: Option[StudyOrder]) =
    allDefault(page)

  def homeLang = LangPage(routes.Study.allDefault())(allResults(StudyOrder.hot, 1))

  def allDefault(page: Int) = all(StudyOrder.hot, page)

  def all(order: StudyOrder, page: Int) = OpenOrScoped(_.Study.Read, _.Web.Mobile):
    allResults(order, page)

  private def allResults(order: StudyOrder, page: Int)(using ctx: Context) =
    Reasonable(page):
      order match
        case order if !Orders.withoutSelector.contains(order) =>
          Redirect(routes.Study.allDefault(page))
        case order =>
          for
            pag <- env.study.pager.all(order, page)
            _ <- preloadMembers(pag)
            res <- negotiate(
              Ok.page(views.study.list.all(pag, order)),
              apiStudies(pag)
            )
          yield res

  def byOwnerDefault(username: UserStr, page: Int) = byOwner(username, Orders.default, page)

  def byOwner(username: UserStr, order: StudyOrder, page: Int) = Open:
    Found(meOrFetch(username)): owner =>
      for
        pag <- env.study.pager.byOwner(owner, order, page)
        _ <- preloadMembers(pag)
        res <- negotiate(Ok.page(views.study.list.byOwner(pag, order, owner)), apiStudies(pag))
      yield res

  def mine = MyStudyPager(
    env.study.pager.mine,
    (pag, order) => env.study.topicApi.userTopics(summon[Me]).map(views.study.list.mine(pag, order, _))
  )

  def minePublic = MyStudyPager(env.study.pager.minePublic, views.study.list.minePublic)

  def minePrivate = MyStudyPager(env.study.pager.minePrivate, views.study.list.minePrivate)

  def mineMember = MyStudyPager(
    env.study.pager.mineMember,
    (pag, order) => env.study.topicApi.userTopics(summon[Me]).map(views.study.list.mineMember(pag, order, _))
  )

  def mineLikes = MyStudyPager(env.study.pager.mineLikes, views.study.list.mineLikes)

  private type StudyPager = Paginator[StudyModel.WithChaptersAndLiked]

  private def MyStudyPager(
      makePager: (StudyOrder, Int) => Me ?=> Fu[StudyPager],
      render: (StudyPager, StudyOrder) => Context ?=> Me ?=> Fu[Page]
  ) = (order: StudyOrder, page: Int) =>
    AuthOrScoped(_.Web.Mobile) { ctx ?=> me ?=>
      for
        pager <- makePager(order, page)
        _ <- preloadMembers(pager)
        res <- negotiate(Ok.async(render(pager, order)), apiStudies(pager))
      yield res
    }

  def byTopic(name: String, order: StudyOrder, page: Int) = Open:
    Found(lila.study.StudyTopic.fromStr(name)): topic =>
      for
        pag <- env.study.pager.byTopic(topic, order, page)
        _ <- preloadMembers(pag)
        res <- negotiate(
          Ok.async:
            ctx.userId
              .traverse(env.study.topicApi.userTopics)
              .map(views.study.list.topic.show(topic, pag, order, _))
          ,
          apiStudies(pag)
        )
      yield res

  private def preloadMembers(pag: Paginator[StudyModel.WithChaptersAndLiked]) =
    env.user.lightUserApi.preloadMany(
      pag.currentPageResults.view
        .flatMap(_.study.members.members.values.take(StudyModel.previewNbMembers))
        .map(_.id)
        .toSeq
    )

  private def apiStudies(pager: Paginator[StudyModel.WithChaptersAndLiked]) =
    given Writes[StudyModel.WithChaptersAndLiked] = Writes(env.study.jsonView.pagerData)
    Ok(Json.obj("paginator" -> pager))

  def show(id: StudyId) = Open:
    orRelayRedirect(id):
      showQuery(env.study.api.byStudyIdAndMaybeChapterId(id, get("chapterId")))

  def chapter(id: StudyId, chapterId: lila.core.id.StudyChapterId) = Open:
    orRelayRedirect(id, chapterId.some):
      showQuery(env.study.api.byStudyIdAndChapterId(id, chapterId))

  private def orRelayRedirect(id: StudyId, chapterId: Option[lila.core.id.StudyChapterId] = None)(
      f: => Fu[Result]
  )(using ctx: Context): Fu[Result] = f

  private def showQuery(query: Fu[Option[WithChapter]])(using ctx: Context): Fu[Result] =
    Found(query): oldSc =>
      CanView(oldSc.study) {
        negotiate(
          html =
            for
              (sc, data) <- getJsonData(oldSc, withChapters = true)
              page <- renderPage(views.study.show(sc.study, sc.chapter, data))
            yield Ok(page)
              .withCanonical(routes.Study.chapter(sc.study.id, sc.chapter.id))
          ,
          json = for
            (sc, data) <- getJsonData(
              oldSc,
              withChapters = getBool("chapters") || HTTPRequest.isLichobile(ctx.req)
            )
          yield Ok:
            Json.obj(
              "study" -> data.study,
              "analysis" -> data.analysis
            )
        )
      }(privateUnauthorizedFu(oldSc.study), privateForbiddenFu(oldSc.study))
    .dmap(_.noCache)

  private[controllers] def getJsonData(sc: WithChapter, withChapters: Boolean)(using
      ctx: Context
  ): Fu[(WithChapter, JsData)] =
    for
      (studyFromDb, chapter) <- env.study.api.maybeResetAndGetChapter(sc.study, sc.chapter)
      study = studyFromDb
      previews <- withChapters.optionFu(env.study.preview.jsonList(study.id))
      data <- env.study.jsonView.full(study, chapter, previews, None, withMembers = true)
    yield sc.copy(study = study, chapter = chapter) -> JsData(data, Json.obj())

  private def privateUnauthorizedFu(study: StudyModel)(using ctx: Context): Fu[Result] =
    renderPage(views.study.privateStudy(study)).map(Forbidden(_))

  private def privateForbiddenFu(study: StudyModel)(using ctx: Context): Fu[Result] =
    renderPage(views.study.privateStudy(study)).map(Forbidden(_))

  private def CanView(study: StudyModel)(f: => Fu[Result])(
      unauthorized: => Fu[Result],
      forbidden: => Fu[Result]
  )(using ctx: Context): Fu[Result] =
    if study.canView(ctx.userId) then f
    else if ctx.isAnon then unauthorized
    else forbidden

  private def renderPage(frag: Context ?=> scalatags.Text.all.Frag)(using ctx: Context): Fu[Page] =
    fuccess(Page("Study").wrap(_ => div(frag)))
