package lila.user

import com.softwaremill.macwire.*
import com.softwaremill.tagging.*

import lila.core.config.*
import lila.common.Bus

@Module
final class Env(
    db: lila.db.Db,
    cacheApi: lila.memo.CacheApi,
    isOnline: lila.core.socket.IsOnline,
    onlineIds: lila.core.socket.OnlineIds
)(using Executor, Scheduler):

  val repo = UserRepo(db(CollName("user4")))

  val api = wire[UserApi]

  val lightUserApi: LightUserApi = wire[LightUserApi]

  export lightUserApi.{
    async as lightUser,
    sync as lightUserSync,
    isBotSync
  }

  lazy val jsonView = wire[JsonView]

  lazy val cached: Cached = wire[Cached]

  Bus.sub[lila.core.mod.MarkCheater]:
    case lila.core.mod.MarkCheater(userId, true) =>
      repo.userIdsWithRoles(List(RoleDbKey("admin"))) // Dummy logic to keep Bus sub for now or remove if possible

  Bus.sub[lila.core.mod.MarkBooster]: m =>
    ()
