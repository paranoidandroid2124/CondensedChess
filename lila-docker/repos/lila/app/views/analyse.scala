package views

import lila.app.UiEnv
import lila.app.UiEnv.{ *, given }
import lila.analyse.ui.AnalyseUi
import play.api.libs.json.Json
import scala.annotation.unused

object analyse:

  lazy val analyseUi = AnalyseUi(UiEnv)(UiEnv.analyseEndpoints)

  object ui:
    private def shortcutRow(keys: Frag, description: String) =
      tr(td(keys), td(description))

    private def kbd(label: String) = tag("kbd")(label)

    def userAnalysis(
        data: play.api.libs.json.JsObject,
        pov: lila.core.game.Pov,
        inlinePgn: Option[String] = None,
        importHistory: Option[play.api.libs.json.JsObject] = None
    )(using Context) =
      analyseUi.userAnalysis(data, pov, inlinePgn = inlinePgn, importHistory = importHistory)

    def keyboardHelp =
      div(cls := "keyboard-help")(
        h2("Keyboard shortcuts"),
        p("Core shortcuts for board navigation, review, and explorer control on the analysis surface."),
        table(
          tbody(
            shortcutRow(frag(kbd("←"), " / ", kbd("K")), "Previous move"),
            shortcutRow(frag(kbd("→"), " / ", kbd("J")), "Next move"),
            shortcutRow(frag(kbd("↑"), " / ", kbd("0"), " / ", kbd("Home")), "Jump to the start"),
            shortcutRow(frag(kbd("↓"), " / ", kbd("$"), " / ", kbd("End")), "Jump to the end"),
            shortcutRow(frag(kbd("Shift"), " + ", kbd("←"), " / ", kbd("K")), "Previous branch"),
            shortcutRow(frag(kbd("Shift"), " + ", kbd("→"), " / ", kbd("J")), "Next branch"),
            shortcutRow(kbd("F"), "Flip the board"),
            shortcutRow(kbd("L"), "Toggle engine analysis"),
            shortcutRow(kbd("E"), "Toggle explorer"),
            shortcutRow(kbd("V"), "Toggle variation arrows"),
            shortcutRow(kbd("X"), "Toggle threat mode"),
            shortcutRow(frag(kbd("Shift"), " + ", kbd("C")), "Toggle comments"),
            shortcutRow(frag(kbd("Shift"), " + ", kbd("I")), "Toggle move tree mode"),
            shortcutRow(kbd("?"), "Open this help dialog")
          )
        )
      )

    val explorerAndCevalConfig = play.api.libs.json.Json.obj()

    object bits:
      object cspExternalEngine:
        def compose(@unused f: Any => Any) = lila.web.ContentSecurityPolicy.default

  object embed:
    def lpv(@unused pgn: Any, @unused board: Boolean = true, @unused title: String = "") = emptyFrag
    def userAnalysis(
        data: play.api.libs.json.JsObject,
        bookmaker: Boolean,
        inlinePgn: Option[String] = None
    )(using EmbedContext) =
      val cfg =
        Json
          .obj(
            "data" -> data,
            "bookmaker" -> bookmaker,
            "embed" -> true
          )
          .add("inlinePgn", inlinePgn) ++ analyseUi.explorerAndCevalConfig
      views.base.embed.site(
        title = "Analysis board",
        cssKeys = List("analyse.free", "llm.widget"),
        pageModule = Some(analyseUi.bits.analyseModule("userAnalysis", cfg)),
        csp = analyseUi.bits.cspExternalEngine.compose(_.withExternalAnalysisApis)
      )(analyseUi.bits.embedUserAnalysisBody)
