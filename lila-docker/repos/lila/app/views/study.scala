package views

import play.api.libs.json.{ JsObject, Json }

import scala.util.Try

import lila.app.UiEnv
import lila.app.UiEnv.*
import lila.study.{ Chapter, Study }

object study:

  private lazy val studyBits = lila.study.ui.StudyBits(UiEnv)
  private lazy val listUi = lila.study.ui.ListUi(UiEnv, studyBits)
  private lazy val analyseUi = lila.analyse.ui.AnalyseUi(UiEnv)(UiEnv.analyseEndpoints)

  private def sectionCountLabel(chapters: List[Chapter.IdName]): String =
    val size = chapters.size
    s"$size ${if size == 1 then "section" else "sections"}"

  private def heroMeta(kind: String, label: String, value: Frag) =
    div(cls := "notebook-hero__meta-pill")(
      studyBits.notebookGlyph(kind, "notebook-hero__meta-icon"),
      span(cls := "notebook-hero__meta-copy")(
        span(cls := "notebook-hero__meta-label")(label),
        strong(value)
      )
    )

  private def notebookDossierJson(study: Study): Option[JsObject] =
    study.notebookDossier.flatMap: raw =>
      Try(Json.parse(raw).as[JsObject]).toOption

  private def notebookLede(study: Study, chapter: Chapter): String =
    val defaultLede =
      s"${chapter.name.value} is open. Save branches, layer commentary, and treat each section like a page in a chess book."
    notebookDossierJson(study)
      .flatMap: dossier =>
        dossier.value
          .get("summary")
          .flatMap(_.asOpt[String])
          .orElse(dossier.value.get("headline").flatMap(_.asOpt[String]))
      .filter(_.nonEmpty)
      .getOrElse(defaultLede)

  object ui:

    def all(
        pag: scalalib.paginator.Paginator[Study.WithChaptersAndLiked],
        order: lila.core.study.StudyOrder
    )(using Context): Page =
      listUi.all(pag, order)

    def mine(
        pag: scalalib.paginator.Paginator[Study.WithChaptersAndLiked],
        order: lila.core.study.StudyOrder
    )(using Context): Page =
      listUi.mine(pag, order)

    def chapter(data: JsObject, study: Study, chapter: Chapter, canWrite: Boolean, chapters: List[Chapter.IdName])(
        using ctx: Context
    ): Page =
      val dossierJson = notebookDossierJson(study)
      val studyCfgBase =
        Json.obj(
          "id" -> study.id.value,
          "chapterId" -> chapter.id.value,
          "name" -> study.name.value,
          "chapterName" -> chapter.name.value,
          "canWrite" -> canWrite,
          "url" -> routes.Study.chapter(study.id, chapter.id).url,
          "visibility" -> study.visibility.toString,
          "chapters" -> chapters.map(c =>
            Json.obj(
              "id" -> c.id.value,
              "name" -> c.name.value,
              "url" -> routes.Study.chapter(study.id, c.id).url
            )
          )
        )
      val studyCfg = dossierJson.fold(studyCfgBase)(dossier => studyCfgBase ++ Json.obj("notebookDossier" -> dossier))
      val cfg =
        Json
          .obj(
            "data" -> data,
            "bookmaker" -> (chapter.setup.variant.standard || chapter.setup.variant.chess960),
            "study" -> studyCfg
          ) ++ analyseUi.explorerAndCevalConfig

      Page(s"${study.name.value} • ${chapter.name.value}")
        .css("analyse.study")
        .csp(analyseUi.bits.cspExternalEngine.compose(_.withExternalAnalysisApis))
        .js(PageModule("analyse.study", Json.obj("cfg" -> cfg)))
        .flag(_.zoom):
          div(cls := "notebook-shell")(
            div(cls := "notebook-hero")(
              div(cls := "notebook-hero__cover-frame")(
                studyBits.coverPreview(
                  study.name.value,
                  chapter.name.value,
                  s"${sectionCountLabel(chapters)} • ${study.visibility.toString}",
                  compact = false
                )
              ),
              div(cls := "notebook-hero__body")(
                div(cls := "notebook-hero__eyebrow")(
                  studyBits.notebookGlyph("bookmark", "notebook-hero__eyebrow-icon"),
                  span("Research notebook")
                ),
                h1(cls := "notebook-hero__title")(study.name.value),
                p(cls := "notebook-hero__lede")(notebookLede(study, chapter)),
                div(cls := "notebook-hero__meta")(
                  heroMeta("page", "Current section", chapter.name.value),
                  heroMeta("section", "Library", sectionCountLabel(chapters)),
                  heroMeta("notebook", "Access", if canWrite then "Editable" else "Read-only"),
                  heroMeta("bookmark", "Visibility", study.visibility.toString)
                ),
                div(cls := "notebook-hero__navigator")(
                  div(cls := "notebook-hero__navigator-head")(
                    studyBits.notebookGlyph("section", "notebook-hero__navigator-icon"),
                    div(
                      strong("Section navigator"),
                      span("Jump to a saved section without losing the notebook context.")
                    )
                  ),
                  div(cls := "notebook-hero__navigator-grid")(
                    chapters.map: c =>
                      a(
                        cls := s"notebook-hero__section${if c.id == chapter.id then " is-active" else ""}",
                        href := routes.Study.chapter(study.id, c.id).url
                      )(
                        studyBits.notebookGlyph(
                          if c.id == chapter.id then "page" else "bookmark",
                          "notebook-hero__section-icon"
                        ),
                        span(cls := "notebook-hero__section-copy")(
                          strong(c.name.value),
                          span(if c.id == chapter.id then "Current section" else "Open section")
                        )
                      )
                  )
                )
              )
            ),
            main(cls := "analyse")
          )
