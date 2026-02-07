package views

import play.api.libs.json.{ JsObject, Json }

import lila.app.UiEnv
import lila.app.UiEnv.*
import lila.study.{ Chapter, Study }

object study:

  private lazy val studyBits = lila.study.ui.StudyBits(UiEnv)
  private lazy val listUi = lila.study.ui.ListUi(UiEnv, studyBits)
  private lazy val analyseUi = lila.analyse.ui.AnalyseUi(UiEnv)(UiEnv.analyseEndpoints)

  object ui:

    def all(
        pag: scalalib.paginator.Paginator[Study.WithChaptersAndLiked],
        order: lila.core.study.StudyOrder
    )(using Context): Page =
      listUi.all(pag, order)

    def mine(
        pag: scalalib.paginator.Paginator[Study.WithChaptersAndLiked],
        order: lila.core.study.StudyOrder
    )(using ctx: Context, me: Me): Page =
      listUi.mine(pag, order)

    def chapter(data: JsObject, study: Study, chapter: Chapter, canWrite: Boolean, chapters: List[Chapter.IdName])(
        using ctx: Context
    ): Page =
      val cfg =
        Json
          .obj(
            "data" -> data,
            "bookmaker" -> (chapter.setup.variant.standard || chapter.setup.variant.chess960),
            "study" -> Json.obj(
              "id" -> study.id.value,
              "chapterId" -> chapter.id.value,
              "name" -> study.name.value,
              "chapterName" -> chapter.name.value,
              "canWrite" -> canWrite,
              "chapters" -> chapters.map(c => Json.obj("id" -> c.id.value, "name" -> c.name.value))
            )
          ) ++ analyseUi.explorerAndCevalConfig

      Page(s"${study.name.value} • ${chapter.name.value}")
        .css("analyse.study")
        .csp(analyseUi.bits.cspExternalEngine.compose(_.withExternalAnalysisApis))
        .js(PageModule("analyse.study", Json.obj("cfg" -> cfg)))
        .flag(_.zoom):
          main(cls := "analyse")
