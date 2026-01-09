package views.game
package side

import lila.app.UiEnv.{ *, given }
import lila.i18n.trans

private val separator = " • "
private val dataUserTv = attr("data-user-tv")
private val dataTime = attr("data-time")

def apply(
    pov: Pov,
    initialFen: Option[chess.format.Fen.Full],
    tour: Option[Any],  // tournament module removed
    simul: Option[Any],  // simul module removed
    userTv: Option[User] = None,
    bookmarked: Boolean
)(using ctx: Context): Option[Frag] =
  ctx.noBlind.option:
    frag(
      meta(pov, initialFen, none, none, userTv, bookmarked)
    )

def meta(
    pov: Pov,
    initialFen: Option[chess.format.Fen.Full],
    tour: Option[Any],  // tournament module removed
    simul: Option[Any],  // simul module removed
    userTv: Option[User] = None,
    bookmarked: Boolean
)(using ctx: Context): Option[Frag] =
  ctx.noBlind.option:
    import pov.*
    div(cls := "game__meta")(
      st.section(
        div(cls := "game__meta__infos")(
          div(
            div(cls := "header")(
              div(cls := "setup")(
                div(), // bookmarked
                if game.sourceIs(_.Import) then
                  div(
                    a(href := routes.Importer.importGame, title := "Import")("IMPORT"),
                    separator,
                    variantLink(game.variant, None)
                  )
                else
                  frag(
                    div("clock"),
                    separator,
                    "Analysis",
                    separator,
                    variantLink(game.variant, None)
                  )
              ),
              game.pgnImport.flatMap(_.date).fold(momentFromNowWithPreload(game.createdAt))(frag(_))
            ),
            game.pgnImport
              .flatMap(_.user)
              .map: importedBy =>
                small(
                  trans.site.importedByX(userIdLink(importedBy.some, withOnline = false))
                )
          )
        ),
        div(cls := "game__meta__players")(
          game.players.mapList: p =>
            frag(
              div(cls := s"player color-icon is ${p.color.name} text")(
                playerLink(
                  p,
                  withOnline = false,
                  withDiff = true,
                  withBerserk = true
                )
              )
            )
        )
      ),
      game.finishedOrAborted.option(
        st.section(cls := "status")(
          ui.gameEndStatus(game),
          game.winner.map: winner =>
            frag(
              separator,
              winner.color.fold(trans.site.whiteIsVictorious, trans.site.blackIsVictorious)
            )
        )
      ),
      game.variant.chess960.option:
        chess.variant.Chess960
          .positionNumber(initialFen | chess.format.Fen.initial)
          .map: number =>
            st.section(
              trans.site.chess960StartPosition(
                a(
                  targetBlank,
                  href := "https://chess960.net/wp-content/uploads/2018/02/chess960-starting-positions.pdf"
                )(number)
              )
            )
      ,
      userTv.map: u =>
        st.section(cls := "game__tv"):
          h2(cls := "top user-tv text", dataUserTv := u.id, dataIcon := Icon.AnalogTv)(u.titleUsername)
    )
