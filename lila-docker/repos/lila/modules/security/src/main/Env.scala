package lila.security

import com.softwaremill.macwire.*
import play.api.Configuration
import play.api.libs.ws.StandaloneWSClient
import scala.concurrent.duration.*

import lila.core.i18n.LangPicker
import lila.oauth.OAuthScope
import lila.core.lilaism.Core.*
import lila.core.security.{ ClearPassword, IsProxy }

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

  trait PwnedStub:
    def isPwned(pass: ClearPassword): Fu[IsPwned]
  lazy val pwned: PwnedStub = new PwnedStub:
    def isPwned(pass: ClearPassword): Fu[IsPwned] = Future.successful(IsPwned(false))

  trait IpTrustStub:
    def rateLimitCostFactor(req: play.api.mvc.RequestHeader, f: lila.security.SecurityConfig => Int): Fu[Float]
    def isPubOrTor(req: play.api.mvc.RequestHeader): Fu[Boolean]
  lazy val ipTrust: IpTrustStub = new IpTrustStub:
    def rateLimitCostFactor(req: play.api.mvc.RequestHeader, f: lila.security.SecurityConfig => Int): Fu[Float] = Future.successful(1.0f)
    def isPubOrTor(req: play.api.mvc.RequestHeader): Fu[Boolean] = Future.successful(false)

  trait Ip2ProxyStub:
    def ofReq(req: play.api.mvc.RequestHeader): Fu[IsProxy]
    def ofIp(ip: lila.core.net.IpAddress): Fu[IsProxy]
  lazy val ip2proxy: Ip2ProxyStub = new Ip2ProxyStub:
    def ofReq(req: play.api.mvc.RequestHeader): Fu[IsProxy] = Future.successful(IsProxy.empty)
    def ofIp(ip: lila.core.net.IpAddress): Fu[IsProxy] = Future.successful(IsProxy.empty)

  trait LilaCookieStub extends lila.core.security.LilaCookie:
    def cookie(
        name: String,
        value: String,
        maxAge: Option[Int] = None,
        httpOnly: Option[Boolean] = None
    ): play.api.mvc.Cookie = play.api.mvc.Cookie(name, value, maxAge)

    def withSession(remember: Boolean)(f: play.api.mvc.Session => play.api.mvc.Session): play.api.mvc.Cookie
    def newSession: play.api.mvc.Cookie
    def session(uri: String, req: play.api.mvc.RequestHeader): play.api.mvc.Cookie =
      play.api.mvc.Cookie("lila-session", "TODO")
    def ensure(req: play.api.mvc.RequestHeader)(res: play.api.mvc.Result): play.api.mvc.Result = res
    def isRememberMe(req: play.api.mvc.RequestHeader): Boolean = false
  lazy val lilaCookie: LilaCookieStub = new LilaCookieStub:
    def withSession(remember: Boolean)(f: play.api.mvc.Session => play.api.mvc.Session): play.api.mvc.Cookie =
      play.api.mvc.Cookie("lila", "session")
    def newSession: play.api.mvc.Cookie = play.api.mvc.Cookie("lila", "new")

  lazy val firewallApi = firewall
  
  def cli = new Cli(authenticator, firewall)
