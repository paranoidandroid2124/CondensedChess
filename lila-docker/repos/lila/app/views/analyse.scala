package views

import lila.app.UiEnv
import lila.app.UiEnv.*
import lila.analyse.ui.AnalyseUi
import scala.annotation.unused

object analyse:

  lazy val analyseUi = AnalyseUi(UiEnv)(UiEnv.analyseEndpoints)

  object ui:
    def userAnalysis(
        data: play.api.libs.json.JsObject,
        pov: lila.core.game.Pov
    )(using Context) =
      analyseUi.userAnalysis(data, pov)

    val explorerAndCevalConfig = play.api.libs.json.Json.obj()

    object bits:
      def cspExternalEngine = new:
        def compose(_f: Any => Any) = lila.web.ContentSecurityPolicy.default

  object embed:
    def lpv(@unused pgn: Any, @unused board: Boolean = true, @unused title: String = "") = emptyFrag
    def userAnalysis(_args: Any*) = emptyFrag
