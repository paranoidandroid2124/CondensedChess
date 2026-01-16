package lila.analyse
package ui

import chess.variant.*
import chess.format.Fen
import chess.format.pgn.PgnStr
import play.api.libs.json.*
import scala.annotation.unused

import lila.ui.*
import lila.ui.ScalatagsTemplate.{ *, given }
import lila.common.Json.given

final class ReplayUi(helpers: Helpers)(analyseUi: AnalyseUi):
  import helpers.*
  import analyseUi.bits.dataPanel

  def forCrawler(
      pov: Pov,
      pgn: PgnStr,
      graph: OpenGraph,
      gameSide: Option[Frag],
      crosstable: Option[Tag]
  )(using @unused ctx: Context) =
    Page(analyseUi.titleOf(pov))
      .css("analyse.round")
      .graph(graph)
      .csp(analyseUi.bits.cspExternalEngine)
      .flag(_.noRobots):
        main(cls := "analyse")(
          st.aside(cls := "analyse__side")(gameSide),
          div(cls := "analyse__board main-board")(chessgroundBoard),
          div(cls := "analyse__tools")(div(cls := "ceval")),
          div(cls := "analyse__controls"),
          div(cls := "analyse__underboard")(
            div(cls := "analyse__underboard__panels")(
              div(cls := "fen-pgn active")(
                div(
                  strong("FEN"),
                  input(readonly, spellcheck := false, cls := "analyse__underboard__fen")
                ),
                div(cls := "pgn")(pgn)
              ),
              crosstable.map(div(cls := "ctable active")(_))
            )
          )
        )

  def forBrowser(
      pov: Pov,
      data: JsObject,
      pgn: PgnStr,
      analysable: Boolean,
      hasAnalysis: Boolean,
      graph: OpenGraph,
      gameSide: Option[Frag],
      crosstable: Option[Tag],
      chatOption: Option[(JsObject, Frag)]
  )(using ctx: Context) =

    import pov.*

    val imageLinks = frag(
      copyMeLink(
        cdnUrl(s"/export/${pov.gameId}.gif"),
        "Game as GIF"
      )(cls := "game-gif"),
      copyMeLink(
        fenThumbnailUrl(Fen.write(pov.game.position).value, pov.color.some, pov.game.variant),
        "Screenshot current position"
      )(cls := "position-gif")
    )

    val shareLinks = frag(
      a(dataIcon := Icon.Expand, cls := "text embed-howto")("Embed in your website"),
      copyMeInput(s"/$gameId")
    )
    val pgnLinks = frag(
      copyMeContent(pathUrl(s"/game/export/${game.id}.pgn?literate=1"), "Download annotated"),
      copyMeContent(pathUrl(s"/game/export/${game.id}.pgn?evals=0&clocks=0"), "Download raw"),
      game.pgnImport.isDefined.option:
        copyMeContent(pathUrl(s"/game/export/${game.id}.pgn?imported=1"), "Download imported")
    )

    analyseUi.bits
      .page(analyseUi.titleOf(pov))
      .css("analyse.round")
      .css((pov.game.variant == Crazyhouse).option("analyse.zh"))
      .js:
        analyseUi.bits.analyseModule(
          "replay",
          Json
            .obj(
              "data" -> data,
              "userId" -> ctx.userId
            )
            .add("chat", chatOption.map(_._1))
            .add("hunter", false) ++
            analyseUi.explorerAndCevalConfig
        )
      .graph(graph):
        frag(
          main(cls := "analyse")(
            st.aside(cls := "analyse__side")(gameSide),
            chatOption.map(_._2),
            div(cls := "analyse__board main-board")(chessgroundBoard),
            div(cls := "analyse__tools")(div(cls := "ceval")),
            div(cls := "analyse__controls"),
            ctx.blind.not.option(
              frag(
                div(cls := "analyse__underboard")(
                  div(role := "tablist", cls := "analyse__underboard__menu")(
                    analysable.option(
                      span(
                        role := "tab",
                        cls := "computer-analysis",
                        dataPanel := "computer-analysis"
                      )("Computer analysis")
                    ),
                    game.pgnImport.isEmpty.option(
                      frag(
                        (game.ply > 1).option(
                          span(role := "tab", dataPanel := "move-times")("Move times")
                        ),
                        crosstable.isDefined.option:
                          span(role := "tab", dataPanel := "ctable")("Crosstable")
                      )
                    ),
                    span(role := "tab", dataPanel := "fen-pgn")("Share & export")
                  ),
                  div(cls := "analyse__underboard__panels")(
                    analysable.option(
                      div(cls := "computer-analysis")(
                        if hasAnalysis then div(id := "acpl-chart-container")(canvas(id := "acpl-chart"))
                        else
                          postForm(
                            cls := s"future-game-analysis${if (ctx.isAuth) "" else " must-login"}",
                            action := routeUrl(routes.Analyse.requestAnalysis(gameId.value))
                          ):
                            submitButton(cls := "button text"):
                              span(cls := "is3 text", dataIcon := Icon.BarChart)(
                                "Request a computer analysis"
                              )
                      )
                    ),
                    div(cls := "move-times")(
                      (game.ply > 1)
                        .option(div(id := "movetimes-chart-container")(canvas(id := "movetimes-chart")))
                    ),
                    div(cls := "fen-pgn")(
                      div(
                        strong("FEN"),
                        copyMeInput("")(cls := "analyse__underboard__fen")
                      ),
                      div(
                        strong("Image"),
                        imageLinks
                      ),
                      div(
                        strong("Share"),
                        shareLinks
                      ),
                      div(
                        strong("PGN"),
                        pgnLinks
                      ),
                      div(cls := "pgn")(pgn)
                    ),
                    crosstable.map(div(cls := "ctable")(_))
                  )
                )
              )
            )
          ),
          ctx.blind.option:
            div(cls := "blind-content none")(
              h2("PGN & FEN"),
              button(cls := "copy-pgn", attr("data-pgn") := pgn):
                "Copy PGN"
              ,
              button(cls := "copy-fen"):
                "Copy FEN"
              ,
              pgnLinks,
              div(
                "FEN",
                copyMeInput("")(cls := "analyse__underboard__fen")
              )
            )
        )
