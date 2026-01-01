package views.study

import chess.format.pgn.PgnStr
import play.api.libs.json.Json

import lila.app.UiEnv.{ *, given }
import lila.common.Json.given
import lila.core.socket.SocketVersion
import lila.core.study.{ IdName, StudyOrder }

lazy val bits = lila.study.ui.StudyBits(helpers)
lazy val ui = lila.study.ui.StudyUi(helpers)
lazy val list = lila.study.ui.ListUi(helpers, bits)

def show(
    s: lila.study.Study,
    chapter: lila.study.Chapter,
    data: lila.study.JsonView.JsData
)(using ctx: Context) =
  Page(s.name.value)
    .css("analyse.study")
    .js(
      PageModule(
        "analyse.study",
        Json.obj(
          "study" -> data.study,
          "data" -> data.analysis,
          "userId" -> ctx.userId
        ) ++ views.analyse.ui.explorerAndCevalConfig
      )
    )
    .i18n(_.study):
      main(cls := "analyse")

def privateStudy(study: lila.study.Study)(using Context) =
  views.site.message(
    title = s"${titleNameOrId(study.ownerId)}'s study"
  ):
    div("Sorry! This study is private, you cannot access it.")

object embed:
  def apply(s: lila.study.Study, chapterId: lila.core.id.StudyChapterId, pgn: PgnStr)(using ctx: EmbedContext) =
    views.analyse.embed.lpv(
      pgn,
      true,
      title = s.name.value
    )
