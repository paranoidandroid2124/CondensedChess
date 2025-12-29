package lila.app
package mashup

import play.api.data.Form

import lila.core.data.SafeJsonStr
import lila.core.user.User
import lila.game.Crosstable
import lila.core.security.IsProxy

// Simplified UserInfo - many modules deleted for analysis-focused app
case class UserInfo(
    nbs: UserInfo.NbGames,
    user: User,
    ratingChart: Option[SafeJsonStr],
    nbStudies: Int,
    insightVisible: Boolean
):
  def teamIds: List[Nothing] = Nil
  def ranks: Nothing = ???
  export nbs.crosstable

object UserInfo:

  enum Angle(val key: String):
    case Activity extends Angle("activity")
    case Games(searchForm: Option[Form[?]]) extends Angle("games")
    case Other extends Angle("other")

  case class Social(
      notes: List[lila.user.Note],
      followable: Boolean,
      blocked: Boolean
  )

  final class SocialApi(
      noteApi: lila.user.NoteApi,
      prefApi: lila.pref.PrefApi
  ):
    def apply(u: User)(using ctx: Context): Fu[Social] =
      given scala.concurrent.ExecutionContext = scala.concurrent.ExecutionContext.parasitic
      (
        ctx.me.soUse(_ ?=> noteApi.getForMyPermissions(u).mon(_.user.segment("notes"))),
        ctx.isAuth.so(prefApi.followable(u.id).mon(_.user.segment("followable"))),
        fuccess(false) // blocked - relation module deleted
      ).mapN { (notes, followable, blocked) =>
         Social(notes.getOrElse(Nil), followable.getOrElse(false), blocked)
      }

  case class NbGames(
      crosstable: Option[Crosstable.WithMatchup],
      playing: Int,
      imported: Int
  ):
    def withMe: Option[Int] = crosstable.map(_.crosstable.nbGames)

  final class NbGamesApi(
      gameCached: lila.game.Cached,
      crosstableApi: lila.game.CrosstableApi
  ):
    def apply(u: User, withCrosstable: Boolean)(using me: Option[Me]): Fu[NbGames] =
      given scala.concurrent.ExecutionContext = scala.concurrent.ExecutionContext.parasitic
      (
        withCrosstable.so:
          me
            .filter(u.isnt(_))
            .traverse(me => crosstableApi.withMatchup(me.id, u.id).mon(_.user.segment("crosstable")))
        ,
        gameCached.nbPlaying(u.id).mon(_.user.segment("nbPlaying")),
        gameCached.nbImportedBy(u.id).mon(_.user.segment("nbImported"))
      ).mapN(NbGames.apply)

  // Simplified UserInfoApi - many modules deleted
  final class UserInfoApi(
      studyRepo: lila.study.StudyRepo
  )(using Executor):
    def fetch(user: User, nbs: NbGames, withUblog: Boolean = false)(using
        ctx: Context,
        proxy: IsProxy
    ): Fu[UserInfo] =
      (
        fuccess(none[SafeJsonStr]),
        studyRepo.countByOwner(user.id).recoverDefault.mon(_.user.segment("nbStudies")),
        fuccess(false) // insightVisible - module deleted
      ).mapN((chart, studies, insight) => UserInfo(nbs, user, chart, studies, insight))

    def preloadTeams(info: UserInfo) = fuccess(())
