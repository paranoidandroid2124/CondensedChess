package lila.game
import lila.core.config.CollName
import com.softwaremill.macwire.*

final class Env(
    db: lila.db.Db,
    lightUserApi: lila.core.user.LightUserApi,
    cacheApi: lila.memo.CacheApi,
    mongoCache: lila.memo.MongoCache.Api
)(using Executor):

  lazy val gameRepo = GameRepo(db(CollName("game")))
  lazy val cached: Cached = wire[Cached]

  lazy val userGameApi = UserGameApi(lightUserApi)
  lazy val divider = Divider()
  lazy val namer = Namer

  lazy val pgnDump = PgnDump(lightUserApi)

  lazy val crosstableApi = CrosstableApi()

  lazy val favoriteOpponents = FavoriteOpponents()

  lazy val captchaApi = CaptchaApi()
