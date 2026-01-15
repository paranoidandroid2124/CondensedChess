package lila.study
package ui

import lila.core.study.IdName
import lila.ui.*

import ScalatagsTemplate.{ *, given }

final class StudyUi(helpers: Helpers):
  import helpers.{ *, given }



  private def studyButton(s: IdName, chapterCount: Int) =
    val btn =
      if Study.maxChapters <= chapterCount then submitButton(cls := "disabled", st.disabled)
      else submitButton()

    btn(name := "as", value := s.id, cls := "button submit")(s.name)

  def create(
      data: lila.study.StudyForm.importGame.Data,
      owner: List[(IdName, Int)],
      contrib: List[(IdName, Int)]
  )(using Context) =
    div(cls := "study-create")(
      postForm(action := routes.Study.create)(
        input(tpe := "hidden", name := "gameId", value := data.gameId),
        input(tpe := "hidden", name := "orientation", value := data.orientation.map(_.key)),
        input(tpe := "hidden", name := "fen", value := data.fen.map(_.value)),
        input(tpe := "hidden", name := "pgn", value := data.pgnStr),
        input(tpe := "hidden", name := "variant", value := data.variant.map(_.key)),
        h2("Where do you want to study that?"),
        p(
          submitButton(
            name := "as",
            value := "study",
            cls := "submit button large new text",
            dataIcon := Icon.StudyBoard
          )("Create a new study")
        ),
        div(cls := "studies")(
          div(
            h2("My studies"),
            owner.map(studyButton)
          ),
          div(
            h2("Studies I contribute to"),
            contrib.map(studyButton)
          )
        )
      )
    )

