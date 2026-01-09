package views.user.show

import lila.app.UiEnv.{ *, given }
import lila.user.User
import lila.core.game.Game
import scalalib.paginator.Paginator

object gamesContent:

  def apply(
    user: User, 
    nb: NbGames, 
    pag: Paginator[Game], 
    filter: String, 
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
        a(cls := "pager", href := s"${routes.User.gamesAll(user.username, next)}?filter=$currentFilter")("Next")
      }
    )
