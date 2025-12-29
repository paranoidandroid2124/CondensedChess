package views.user.show

import lila.app.UiEnv.{ *, given }
import lila.user.User
import lila.game.Game

object gamesContent:

  def apply(
    user: User, 
    nb: lila.user.NbGames, 
    pag: Paginator[GameModel], 
    filter: lila.app.mashup.GameFilterMenu, 
    currentFilter: String,
    notes: Map[GameId, String]
  )(using Context) =
    div(cls := "games infinite-scroll")(
      pag.currentPageResults.map { g =>
        div(cls := "game-row")(
          a(href := s"/game/${g.id}", g.id)
        )
      },
      pag.nextPage.map { next =>
        a(cls := "pager", href := s"${routes.User.games(user.username, currentFilter)}?page=$next")("Next")
      }
    )
