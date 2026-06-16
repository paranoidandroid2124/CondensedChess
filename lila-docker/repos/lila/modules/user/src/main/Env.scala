package lila.user

import com.softwaremill.macwire.*

import lila.core.config.*

@Module
final class Env(
    db: lila.db.Db,
    cacheApi: lila.memo.CacheApi
)(using Executor, Scheduler):

  val repo = UserRepo(db(CollName("user4")))

  val api = wire[UserApi]

  val lightUserApi: LightUserApi = wire[LightUserApi]

  lazy val deleteRequestRepo = new DeleteRequestRepo(db(CollName("user_delete_request")))

  export lightUserApi.{
    async as lightUser,
    sync as lightUserSync,
    fallback as lightUserSyncFallback
  }

  lazy val noteApi = new NoteApi(db(CollName("note")))
  
  lazy val flairApi = new FlairApi(lila.common.config.GetRelativeFile((_: String) => new java.io.File("")))

  private val isOnlineFunc: UserId => Boolean = _ => false
  lazy val jsonView = new JsonView(isOnlineFunc)

  lazy val cached: Cached = wire[Cached]
