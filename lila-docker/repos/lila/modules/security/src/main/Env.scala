package lila.security

import com.softwaremill.macwire.*
import play.api.Configuration
import play.api.libs.ws.StandaloneWSClient
import scala.concurrent.duration.*

import lila.core.config.Secret
import lila.common.Executor

@Module
final class Env(
    config: Configuration,
    db: lila.db.Env,
    userEnv: lila.user.Env,
    oauthEnv: lila.oauth.Env,
    mailerEnv: lila.mailer.Env,
    cacheApi: lila.memo.CacheApi
)(using val system: akka.actor.ActorSystem, val executor: Executor)(using StandaloneWSClient):

  private val settings = config.get[SecurityConfig]("security")
  
  import settings.*

  lazy val securityColl = db.mainDb(collection.security)
  lazy val firewallColl = db.mainDb(collection.firewall)

  lazy val sessionStore = new SessionStore(securityColl, cacheApi)

  lazy val api = new SecurityApi(userEnv.repo, sessionStore)

  lazy val firewall = new Firewall(firewallColl, settings, system.scheduler)
  lazy val flood = new Flood()
  
  // Stubs/Simplified components for analysis-only
  lazy val hcaptcha = new Hcaptcha(settings.hcaptcha)
  lazy val forms = new SecurityForm(hcaptcha, userEnv.repo)
  lazy val passwordHasher = new PasswordHasher(settings.passwordBPassSecret)
  lazy val geoIp = new Ip2Proxy(settings.ip2Proxy, cacheApi)
  lazy val emailConfirm = new EmailConfirm(settings.emailConfirm)
  lazy val userLogins = new UserLogins(securityColl)
  lazy val pwned = new PwnedApi(settings.pwnedRangeUrl)
  lazy val spam = new Spam(userEnv.repo)
  lazy val firewallApi = firewall // simplified alias
  
  lazy val lilaCookie = new LilaCookie(sessionStore, settings.loginTokenSecret)

  lazy val signup = new Signup(
    userRepo = userEnv.repo,
    emailConfirm = emailConfirm,
    timeline = None // Stubbed
  )

  def cli = new Cli(this)
