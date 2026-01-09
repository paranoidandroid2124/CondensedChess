package views

import lila.app.UiEnv.{ *, given }

object analyse:
  object ui:
    def userAnalysis(args: Any*)(using Context) = 
      Page("Analysis").wrap(_ => div("Analysis Board"))
    val explorerAndCevalConfig = play.api.libs.json.Json.obj()
    object bits:
      def cspExternalEngine = new:
        def compose(f: Any => Any) = lila.web.ContentSecurityPolicy.default

  object embed:
    def lpv(pgn: Any, board: Boolean = true, title: String = "") = emptyFrag
    def userAnalysis(args: Any*) = emptyFrag
