package lila.security

import com.softwaremill.macwire.*
import play.api.Configuration
import play.api.libs.ws.StandaloneWSClient

import lila.core.lilaism.Core.*


@Module
final class Env(
    config: Configuration,
    db: lila.db.Env,
    userEnv: lila.user.Env,
    oauthEnv: lila.oauth.Env,
    mailerEnv: lila.mailer.Env,
    cacheApi: lila.memo.CacheApi
)(using val system: akka.actor.ActorSystem, val executor: Executor, mode: play.api.Mode, rateLimit: lila.core.config.RateLimit)(using StandaloneWSClient):

  private val settings = config.get[SecurityConfig]("security")
  
  import settings.*

  lazy val securityColl = db.mainDb(collection.security)
  lazy val firewallColl = db.mainDb(collection.firewall)

  lazy val sessionStore = new SessionStore(securityColl, cacheApi)

  lazy val api = new SecurityApi(userEnv.repo, sessionStore)

  lazy val loginToken = new LoginToken(settings.loginTokenSecret)

  lazy val firewall = new Firewall(firewallColl, settings, system.scheduler)
  lazy val flood = new Flood()
  
  // Simplified components for analysis-only system
  lazy val hcaptcha: Hcaptcha = new HcaptchaSkip()
  lazy val lameNameCheck = LameNameCheck(false)
  lazy val passwordHasher = new PasswordHasher(settings.passwordBPassSecret, logRounds = 10)
  lazy val authenticator = new Authenticator(passwordHasher, userEnv.repo)
  lazy val forms = new SecurityForm(userEnv.repo, authenticator, lameNameCheck)
  lazy val spam = new Spam(() => lila.core.data.Strings(Nil))
  lazy val signup = new Signup(userEnv.repo, authenticator)

  lazy val lilaCookie: lila.core.security.LilaCookie = new lila.core.security.LilaCookie:
    def cookie(
        name: String,
        value: String,
        maxAge: Option[Int] = None,
        httpOnly: Option[Boolean] = None
    ): play.api.mvc.Cookie =
      play.api.mvc.Cookie(
        name = name,
        value = value,
        maxAge = maxAge,
        path = "/",
        secure = false,
        httpOnly = httpOnly.getOrElse(true),
        sameSite = play.api.mvc.Cookie.SameSite.Lax.some
      )

  lazy val firewallApi = firewall
  
  def cli = new Cli(authenticator, firewall)
