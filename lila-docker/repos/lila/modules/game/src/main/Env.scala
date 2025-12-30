package lila.game
import lila.core.config.CollName

final class Env(
    db: lila.db.Db,
    lightUserApi: lila.core.user.LightUserApi
)(using Executor):

  lazy val gameRepo = GameRepo(db(CollName("game")))

  lazy val userGameApi = UserGameApi(lightUserApi)

  lazy val pgnDump = PgnDump(lightUserApi)

  lazy val crosstableApi = CrosstableApi()

  lazy val favoriteOpponents = FavoriteOpponents()

  lazy val captchaApi = CaptchaApi()
