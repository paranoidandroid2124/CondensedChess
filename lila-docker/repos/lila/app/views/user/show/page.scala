package views.user.show

import lila.app.UiEnv.{ *, given }
import lila.user.{ User, UserInfo }
import lila.user.JsonView.GameInfo

object page:

  def activity(activity: Vector[Nothing], info: UserInfo)(using Context) =
    main(info.user):
      header(info)
      div(cls := "content-box")(
        p("Activity feed disabled.")
      )

  def games(info: UserInfo, pag: Paginator[GameModel], filter: lila.app.mashup.GameFilterMenu, subject: Option[User], notes: Map[GameId, String])(using Context) =
    main(info.user):
      header(info)
      div(cls := "content-box")(
        gamesContent(info.user, info.nb, pag, filter, filter.current.name, notes)
      )

  def deleted(canCreate: Boolean)(using Context) =
    main("User deleted"):
      div(cls := "content-box")(
        h1("User deleted/not found"),
        if canCreate then p("You can register this username.") else emptyFrag
      )

  private def main(user: User)(content: Frag)(using Context): Tag =
    views.base.layout(
      title = s"${user.username} : Chess Profile",
      moreJs = views.base.asset.js("user-show.js") :: Nil
    )(content)

  private def main(title: String)(content: Frag)(using Context): Tag =
    views.base.layout(title = title)(content)
