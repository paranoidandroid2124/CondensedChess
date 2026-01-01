package lila.app
package mashup

import play.api.data.FormBinding
import play.api.i18n.Lang
import play.api.mvc.Request
import scalalib.paginator.Paginator

import lila.core.game.Game
import lila.core.user.User
import lila.game.{ GameFilter, GameFilterMenu }

// Minimal GameFilterMenu for Study-only system
object GameFilterMenu:

  import GameFilter.*

  def apply(user: User, nbs: UserInfo.NbGames, currentName: String, isAuth: Boolean): GameFilterMenu =
    val filters: NonEmptyList[GameFilter] = NonEmptyList(All, Nil)
    val current = currentOf(filters, currentName)
    lila.game.GameFilterMenu(filters, current)

  def currentOf(filters: NonEmptyList[GameFilter], name: String) =
    filters.find(_.name == name) | filters.head
