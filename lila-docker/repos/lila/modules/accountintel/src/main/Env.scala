package lila.accountintel

import com.softwaremill.macwire.*
import play.api.Configuration
import play.api.libs.ws.StandaloneWSClient
import scala.concurrent.duration.*

import lila.common.LilaScheduler
import lila.core.config.*
import lila.accountintel.source.AccountGameSource
import lila.accountintel.service.{
  AccountIntelEvalRequester,
  AccountNotebookEngine,
  EvalCacheLookup,
  SelectiveEvalRefiner,
  StudyNotebookSink
}

@Module
final class Env(
    appConfig: Configuration,
    ws: StandaloneWSClient,
    mongo: lila.db.Env,
    cacheApi: lila.memo.CacheApi,
    evalCacheApi: lila.evalCache.EvalCacheApi,
    studyApi: lila.study.StudyApi,
    userApi: lila.core.user.UserApi
)(using
    Executor,
    Scheduler,
    akka.stream.Materializer,
    RateLimit
):

  private val jobColl = mongo.mainDb(CollName("account_intel_job"))

  lazy val repo = wire[AccountIntelJobRepo]
  lazy val source = wire[AccountGameSource]
  lazy val evalCacheLookup = wire[EvalCacheLookup]
  lazy val evalRequester = wire[AccountIntelEvalRequester]
  lazy val dispatcher =
    appConfig
      .getOptional[String]("accountIntel.dispatch.baseUrl")
      .map(_.trim)
      .filter(_.nonEmpty)
      .fold[AccountIntel.AccountIntelJobDispatcher](wire[NoopAccountIntelJobDispatcher])( _ =>
        wire[HttpAccountIntelJobDispatcher]
      )
  lazy val selectiveEvalRefiner = new SelectiveEvalRefiner(evalCacheLookup, evalRequester)
  lazy val engine = wire[AccountNotebookEngine]
  lazy val notebookSink = wire[StudyNotebookSink]
  lazy val api = wire[AccountIntelApi]
  lazy val worker = wire[AccountIntelWorker]
  val workerEnabled =
    appConfig.getOptional[Boolean]("accountIntel.worker.enabled").getOrElse(true)
  val internalAuthHeaderName =
    appConfig.getOptional[String]("accountIntel.worker.authHeaderName").getOrElse("X-AccountIntel-Worker-Token")
  val internalAuthHeaderValue =
    appConfig.getOptional[String]("accountIntel.worker.authHeaderValue").map(_.trim).filter(_.nonEmpty)

  private val workerEvery =
    appConfig.getOptional[FiniteDuration]("accountIntel.worker.every").getOrElse(10.seconds)
  private val workerTimeout =
    appConfig.getOptional[FiniteDuration]("accountIntel.worker.timeout").getOrElse(5.minutes)
  private val workerInitialDelay =
    appConfig.getOptional[FiniteDuration]("accountIntel.worker.initialDelay").getOrElse(4.seconds)

  if workerEnabled then
    LilaScheduler(
      name = "account-intel-worker",
      every = _ => Every(workerEvery),
      timeout = _ => AtMost(workerTimeout),
      initialDelay = _ => Delay(workerInitialDelay)
    ):
      worker.tick()
