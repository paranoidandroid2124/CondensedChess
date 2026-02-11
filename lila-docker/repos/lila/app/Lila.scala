package lila.app

import akka.actor.ActorSystem
import com.softwaremill.macwire.*
import play.api.inject.DefaultApplicationLifecycle
import play.api.http.{ FileMimeTypes, HttpRequestHandler }
import play.api.inject.ApplicationLifecycle
import play.api.libs.crypto.DefaultCookieSigner
import play.api.libs.ws.StandaloneWSClient
import play.api.mvc.*
import play.api.mvc.request.*
import play.api.routing.Router
import play.api.{ BuiltInComponents, Configuration, Environment }

// The program entry point.
// To run with bloop:
// /path/to/bloop run lila -m lila.app.Lila -c /path/to/lila/.bloop
object Lila:

  def main(args: Array[String]): Unit =
    lila.web.PlayServer.start(args): env =>
      LilaComponents(
        env,
        DefaultApplicationLifecycle(),
        Configuration.load(env)
      ).application

final class LilaComponents(
    val environment: Environment,
    val applicationLifecycle: ApplicationLifecycle,
    val configuration: Configuration
) extends BuiltInComponents:

  val controllerComponents: ControllerComponents = DefaultControllerComponents(
    defaultActionBuilder,
    playBodyParsers,
    fileMimeTypes,
    executionContext
  )

  given executor: Executor = scala.concurrent.ExecutionContextOpportunistic

  lila
    .log("boot")
    .info:
      val appVersionCommit = ~configuration.getOptional[String]("app.version.commit")
      val appVersionDate = ~configuration.getOptional[String]("app.version.date")
      s"lila version: $appVersionCommit $appVersionDate"

  import _root_.controllers.*

  // we want to use the legacy session cookie baker
  // for compatibility with lila-ws
  lazy val cookieBaker = LegacySessionCookieBaker(httpConfiguration.session, cookieSigner)

  override lazy val requestFactory: RequestFactory =
    val cookieSigner = DefaultCookieSigner(httpConfiguration.secret)
    DefaultRequestFactory(
      DefaultCookieHeaderEncoding(httpConfiguration.cookies),
      cookieBaker,
      LegacyFlashCookieBaker(httpConfiguration.flash, httpConfiguration.secret, cookieSigner)
    )

  given ActorSystem = actorSystem

  given StandaloneWSClient =
    import play.shaded.ahc.org.asynchttpclient.DefaultAsyncHttpClient
    import play.api.libs.ws.WSConfigParser
    import play.api.libs.ws.ahc.{ AhcConfigBuilder, AhcWSClientConfigParser, StandaloneAhcWSClient }
    new StandaloneAhcWSClient(
      DefaultAsyncHttpClient(
        AhcConfigBuilder(
          AhcWSClientConfigParser(
            WSConfigParser(configuration.underlying, environment.classLoader).parse(),
            configuration.underlying,
            environment.classLoader
          ).parse()
        ).modifyUnderlying(_.setIoThreadsCount(8)).build()
      )
    )

  val env: lila.app.Env =
    lila.log("boot").info(s"Start loading lila modules")
    val c = lila.common.Chronometer.sync(wire[lila.app.Env])
    lila.log("boot").info(s"Loaded lila modules in ${c.showDuration}")
    c.result

  val httpFilters = Seq(
    lila.web.HttpFilter(
      env.net,
      env.web.settings.sitewideCoepCredentiallessHeader.get
      // lila.security.Mobile.LichessMobileUa.parse  // Removed - not in constructor
    )
  )

  override lazy val httpErrorHandler =
    lila.app.http.ErrorHandler(
      environment = environment,
      config = configuration,
      router = router,
      mainC = main
    )

  override lazy val httpRequestHandler: HttpRequestHandler =
    lila.app.http.HttpRequestHandler(
      router,
      httpErrorHandler,
      httpConfiguration,
      httpFilters,
      controllerComponents
    )

  lazy val devAssetsController =
    given FileMimeTypes = fileMimeTypes
    wire[ExternalAssets]
  
  // Core controllers only (Chesstory: removed deleted module controllers)
  lazy val account: Account = wire[Account]
  lazy val analyse: Analyse = wire[Analyse]
  // lazy val api: Api = wire[Api]  // Deleted
  lazy val auth: Auth = wire[Auth]
  // lazy val dasher: Dasher = wire[Dasher]  // Deleted
  // lazy val dev: Dev = wire[Dev]  // Deleted
  lazy val editor: Editor = wire[Editor]
  // lazy val fishnet: Fishnet = wire[Fishnet]  // Deleted
  // lazy val game: Game = wire[Game]  // Deleted
  // lazy val github: Github = wire[Github]  // Deleted
  // lazy val i18n: I18n = wire[I18n]  // Deleted
  lazy val importer: Importer = wire[Importer]
  lazy val main: Main = wire[Main]
  lazy val oAuth: OAuth = wire[OAuth]
  lazy val oAuthToken: OAuthToken = wire[OAuthToken]
  // lazy val plan: Plan = wire[Plan]  // Deleted
  lazy val pref: Pref = wire[Pref]
  // lazy val push: Push = wire[Push]  // Deleted
  // lazy val round: Round = wire[Round]  // Deleted
  // lazy val search: Search = wire[Search]  // Deleted
  // lazy val setup: Setup = wire[Setup]  // Deleted
  lazy val study: Study = wire[Study]
  // lazy val timeline: Timeline = wire[Timeline]  // Deleted
  // lazy val tv: Tv = wire[Tv]  // Deleted
  lazy val user: User = wire[User]
  lazy val userAnalysis: UserAnalysis = wire[UserAnalysis]
  lazy val llmApi = env.llm.api
  lazy val creditApi = env.llm.creditApi
  lazy val api = env.apiC
  lazy val llm: LlmController = wire[LlmController]
  lazy val plan: AnalystProPlan = wire[AnalystProPlan]
  lazy val checkout: Checkout = wire[Checkout]
  // lazy val opening: Opening = wire[Opening]  // Deleted

  val router: Router = wire[_root_.router.router.Routes]

  lila.common.Uptime.startedAt
  UiEnv.setEnv(env)

  if configuration.get[Boolean]("kamon.enabled") then
    lila.log("boot").info("Kamon is enabled")
    kamon.Kamon.init()
