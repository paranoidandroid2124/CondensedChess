package lila.api

import akka.actor.*
import com.softwaremill.macwire.*
import play.api.Mode

import lila.common.Bus

/* Chesstory: Simplified API Env for analysis-only system */
@Module
final class Env(
    net: lila.core.config.NetConfig,
    securityStore: lila.security.SessionStore,
    mailerEnv: lila.mailer.Env,
    gameEnv: lila.game.Env,
    prefApi: lila.pref.PrefApi,
    userEnv: lila.user.Env,
    analyseEnv: lila.analyse.Env,
    picfitUrl: lila.memo.PicfitUrl,
    cacheApi: lila.memo.CacheApi,
    webConfig: lila.web.WebConfig,
    manifest: lila.web.AssetManifest,
    tokenApi: lila.oauth.AccessTokenApi
)(using scheduler: Scheduler)(using
    Mode,
    Executor,
    ActorSystem,
    akka.stream.Materializer
):

  export net.{ baseUrl, domain }

  export webConfig.apiToken
  private lazy val gameRepo = gameEnv.gameRepo
  private lazy val analysisRepo = analyseEnv.analysisRepo
  lazy val gameApi = wire[GameApi]

  lazy val accountTermination = wire[AccountTermination]

  lazy val cli = wire[Cli]
