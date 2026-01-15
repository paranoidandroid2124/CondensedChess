package lila.app

import com.softwaremill.macwire.*
import play.api.libs.ws.StandaloneWSClient
import play.api.mvc.{ Call, ControllerComponents }
import play.api.{ Configuration, Environment, Mode }

import lila.core.config.*
import lila.common.config.GetRelativeFile

/* Chesstory: Analysis-only Env
 * Removed: socket, relation, title, chat, playban, shutup, bookmark, round,
 *          search, gameSearch, perfStat, timeline, setup, tv, push, studySearch, plan, opening
 */
final class Env(
    val config: Configuration,
    val controllerComponents: ControllerComponents,
    environment: Environment,
    shutdown: akka.actor.CoordinatedShutdown
)(using val system: akka.actor.ActorSystem, val executor: Executor)(using
    StandaloneWSClient,
    akka.stream.Materializer
):
  val net: NetConfig = lila.web.WebConfig.netConfig(config)
  export net.baseUrl
  val routeUrl: Call => Url = call => Url(s"${baseUrl}${call.url}")

  given mode: Mode = environment.mode
  given scheduler: Scheduler = system.scheduler
  given RateLimit = net.rateLimit
  given NetDomain = net.domain
  val getFile: GetRelativeFile = GetRelativeFile(environment.getFile(_))

  // Chesstory: Analysis-focused modules only
  // val i18n remove
  val mongo: lila.db.Env = wire[lila.db.Env]
  val memo: lila.memo.Env = wire[lila.memo.Env]
  val user: lila.user.Env = new lila.user.Env(
    db = mongo.mainDb,
    cacheApi = memo.cacheApi
  )
  val mailer: lila.mailer.Env = wire[lila.mailer.Env]
  val oAuth: lila.oauth.Env = wire[lila.oauth.Env]
  val security: lila.security.Env = wire[lila.security.Env]
  val pref: lila.pref.Env = wire[lila.pref.Env]
  val game: lila.game.Env = new lila.game.Env(
    db = mongo.mainDb,
    lightUserApi = user.lightUserApi,
    cacheApi = memo.cacheApi,
    mongoCache = memo.mongoCache
  )
  val evalCache: lila.evalCache.Env = wire[lila.evalCache.Env]
  val analyse: lila.analyse.Env = new lila.analyse.Env(
    db = mongo.mainDb,
    gameRepo = game.gameRepo,
    cacheApi = memo.cacheApi,
    net = net
  )
  
  // Chesstory: Explorer dummy implementation
  val explorer: lila.core.game.Explorer = id => game.gameRepo.game(id)

  val study: lila.study.Env = new lila.study.Env(
    appConfig = config,
    ws = summon[StandaloneWSClient],
    lightUserApi = user.lightUserApi,
    gamePgnDump = game.pgnDump,
    divider = game.divider,
    gameRepo = game.gameRepo,
    namer = game.namer,
    userApi = user.api,
    explorer = explorer,
    analyser = analyse.analyser,
    analysisJson = lila.tree.AnalysisJson,
    annotator = analyse.annotator,
    mongo = mongo,
    net = net,
    cacheApi = memo.cacheApi
  )
  
  val llm: lila.llm.Env = wire[lila.llm.Env]
  val web: lila.web.Env = wire[lila.web.Env]
  val api: lila.api.Env = wire[lila.api.Env]
  lazy val apiC: lila.api.Api = wire[lila.api.Api]

  val announceApi = new lila.web.AnnounceApi(mongo.mainDb(CollName("announce")))

  val pageCache = wire[http.PageCache]

end Env
