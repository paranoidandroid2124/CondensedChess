package lila.user

import com.softwaremill.macwire.*
import com.softwaremill.tagging.*

import lila.core.config.*
import lila.common.Bus

@Module
final class Env(
    db: lila.db.Db,
    cacheApi: lila.memo.CacheApi
)(using Executor, Scheduler):

  val repo = UserRepo(db(CollName("user4")))

  val api = wire[UserApi]

  val lightUserApi: LightUserApi = wire[LightUserApi]

  export lightUserApi.{
    async as lightUser,
    sync as lightUserSync
  }

  // Mock isOnline as always false since socket is gone
  private val isOnlineFunc: UserId => Boolean = _ => false
  lazy val jsonView = new JsonView(isOnlineFunc)

  lazy val cached: Cached = wire[Cached]

  // Moderation/Cheater markings removed
