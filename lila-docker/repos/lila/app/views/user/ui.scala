package views.user

import lila.app.UiEnv.{ *, given }
import lila.core.data.SafeJsonStr

val bits = lila.user.ui.UserBits(helpers)
// noteUi removed
val download = lila.user.ui.UserGamesDownload(helpers)

def mini(
    u: User,
    playingGame: Option[Pov],
    blocked: Boolean,
    followable: Boolean,
    relation: Option[Nothing], // Relation removed
    ping: Option[Int],
    ct: Option[lila.game.Crosstable]
)(using ctx: Context) =
  val rel = emptyFrag // Relation view removed
  def crosstable(myId: UserId) = ct
    .flatMap(_.nonEmpty)
    .map: cross =>
      a(
        cls := "upt__score",
        href := s"${routes.User.games(u.username, "me")}#games",
        title := trans.site.nbGames.pluralTxt(cross.nbGames, cross.nbGames.localize)
      ):
        trans.site.yourScore(raw:
          val opponent = ~cross.showOpponentScore(myId)
          s"""<strong>${cross.showScore(myId)}</strong> - <strong>$opponent</strong>""")
  val playing = playingGame.map(views.game.mini(_))
  
  // Simplified usage - logic from external ui helpers moved here or simplified
  div(cls := "user_show")(
    div(cls := "user-infos")(
       // Basic info if needed
    )
  )

val perfStat = emptyFrag // logic removed
def perfStatPage(data: Any, ratingChart: Option[SafeJsonStr])(using Context) = emptyFrag
