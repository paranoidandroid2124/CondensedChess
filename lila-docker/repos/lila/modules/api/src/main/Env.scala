package lila.api

import akka.actor.*
import com.softwaremill.macwire.*
import play.api.Mode

import lila.common.Bus

@Module
final class Env(
    net: lila.core.config.NetConfig,
    securityEnv: lila.security.Env,
    mailerEnv: lila.mailer.Env,
    studyEnv: lila.study.Env,
    gameEnv: lila.game.Env,
    prefApi: lila.pref.PrefApi,
    userEnv: lila.user.Env,
    analyseEnv: lila.analyse.Env,
    picfitUrl: lila.memo.PicfitUrl,
    cacheApi: lila.memo.CacheApi,
    webConfig: lila.web.WebConfig,
    manifest: lila.web.AssetManifest,
    tokenApi: lila.oauth.AccessTokenApi,
    webMobile: lila.web.Mobile
)(using scheduler: Scheduler)(using
    Mode,
    Executor,
    ActorSystem,
    akka.stream.Materializer,
    lila.core.i18n.Translator
):

  export net.{ baseUrl, domain }

  export webConfig.apiToken
  lazy val gameApi = wire[GameApi]

  lazy val accountTermination = wire[AccountTermination]

  lazy val cli = wire[Cli]

  scheduler.scheduleWithFixedDelay(1.minute, 1.minute): () =>
    lila.mon.bus.classifiers.update(lila.common.Bus.size())
    lila.mon.jvm.threads()
    userEnv.repo.setSeenAt(UserId.lichess)
