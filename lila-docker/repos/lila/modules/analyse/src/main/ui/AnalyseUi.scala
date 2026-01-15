package lila.analyse
package ui

import chess.variant.*
import chess.format.{ Uci, Fen }
import play.api.libs.json.*

import lila.ui.*
import lila.ui.ScalatagsTemplate.{ *, given }

final class AnalyseUi(helpers: Helpers)(endpoints: AnalyseEndpoints):
  import helpers.{ *, given }

  def miniSpan(fen: Fen.Board, color: Color = chess.White, lastMove: Option[Uci] = None) =
    chessgroundMini(fen, color, lastMove)(span)

  def explorerAndCevalConfig(using ctx: Context) =
    Json.obj(
      "explorer" -> Json.obj(
        "endpoint" -> endpoints.explorer,
        "tablebaseEndpoint" -> endpoints.tablebase,
        "showRatings" -> ctx.pref.showRatings
      ),
      "externalEngineEndpoint" -> endpoints.externalEngine
    )

  def userAnalysis(
      data: JsObject,
      pov: Pov,
      chess960PositionNum: Option[Int] = None,
      withForecast: Boolean = false,
      inlinePgn: Option[String] = None,
      narrative: Option[Frag] = None
  )(using ctx: Context): Page =
    Page("Analysis")
      .css("analyse.free")
      .css((pov.game.variant == Crazyhouse).option("analyse.zh"))
      .css(withForecast.option("analyse.forecast"))
      .csp(bits.cspExternalEngine.compose(_.withExternalAnalysisApis))
      .js:
        bits.analyseModule(
          "userAnalysis",
          Json
            .obj(
              "data" -> data,
              "bookmaker" -> pov.game.variant.standard
            )
            .add("inlinePgn", inlinePgn) ++
            explorerAndCevalConfig
        )
      .graph(
        title = "Chess analysis board",
        url = lila.ui.Url(routeUrl(routes.UserAnalysis.index)),
        description = "Analyse chess positions and variations on an interactive chess board"
      )
      .flag(_.zoom):
        main(
          cls := List(
            "analyse" -> true,
            "analyse--bookmaker" -> pov.game.variant.standard
          )
        )(
          pov.game.synthetic.option(
            st.aside(cls := "analyse__side")(
              lila.ui.bits.mselect(
                "analyse-variant",
                span(cls := "text", dataIcon := iconByVariant(pov.game.variant))(
                  pov.game.variant.variantTrans()
                ),
                Variant.list.all
                  .filter(FromPosition != _)
                  .map: v =>
                    a(
                      dataIcon := iconByVariant(v),
                      cls := (pov.game.variant == v).option("current"),
                      href := routes.UserAnalysis.parseArg(v.key.value)
                    )(v.variantTrans())
              ),
              pov.game.variant.chess960.option(chess960selector(chess960PositionNum)),
              pov.game.variant.standard.option(
                fieldset(
                  cls := s"analyse__bookmaker ${narrative.isEmpty.option("empty")} toggle-box toggle-box--toggle",
                  id := "bookmaker-field",
                  narrative.isDefined.option(attr("data-bookmaker") := "true")
                )(
                  legend(tabindex := 0)("Bookmaker"),
                  div(cls := "analyse__bookmaker-text")(narrative)
                )
              )
            )
          ),
          div(cls := "analyse__board main-board")(chessgroundBoard),
          div(cls := "analyse__tools"),
          div(cls := "analyse__controls")
        )

  private def chess960selector(num: Option[Int]) =
    div(cls := "jump-960")(
      num.map(pos => label(`for` := "chess960-position")(s"Chess960 position $pos")),
      br,
      form(
        cls := "control-960",
        method := "GET",
        action := routes.UserAnalysis.parseArg("chess960")
      )(
        input(
          id := "chess960-position",
          `type` := "number",
          name := "position",
          min := 0,
          max := 959,
          value := num
        ),
        form3.submit("Load position", icon = none)
      )
    )

  private def iconByVariant(variant: Variant): Icon =
    PerfKey.byVariant(variant).fold(Icon.CrownElite)(_.perfIcon)

  def titleOf(pov: Pov) =
    s"${playerText(pov.game.whitePlayer)} vs ${playerText(pov.game.blackPlayer)}"

  object bits:

    val dataPanel = attr("data-panel")

    def page(title: String): Page =
      Page(title)
        .flag(_.zoom)
        .flag(_.noRobots)
        .csp:
          cspExternalEngine.compose(_.withPeer.withInlineIconFont.withChessDbCn)

    def cspExternalEngine: Update[ContentSecurityPolicy] =
      _.withWebAssembly.withExternalEngine(endpoints.externalEngine)

    def analyseModule(mode: "userAnalysis" | "replay", json: JsObject) =
      PageModule("analyse.user", Json.obj("mode" -> mode, "cfg" -> json))

    val embedUserAnalysisBody = div(id := "main-wrap", cls := "is2d")(
      main(cls := "analyse")(
        div(cls := "analyse__board main-board")(chessgroundBoard),
        div(cls := "analyse__tools"),
        div(cls := "analyse__controls")
      )
    )
