package views.user

import lila.app.UiEnv.{ *, given }

object list:

  def apply(
      tourneyWinners: List[Nothing],
      online: List[Nothing],
      leaderboards: List[Nothing],
      nbAllTime: List[Nothing]
  )(using Context) =
    main("Community"):
      div(cls := "content-box")(
        h1("Community"),
        p("Leaderboards and social features are disabled in Analysis Only mode.")
      )
