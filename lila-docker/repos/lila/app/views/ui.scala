package views

import lila.app.UiEnv.{ *, given }
export lila.web.ui.bits

val captcha = lila.web.ui.CaptchaUi(helpers)

val chat = lila.chat.ChatUi

val boardEditor = lila.web.ui.BoardEditorUi(helpers)

val setup = lila.setup.ui.SetupUi(helpers)

val gathering = lila.gathering.ui.GatheringUi(helpers)(env.web.settings.prizeTournamentMakers.get)

val learn = lila.web.ui.LearnUi(helpers)

val coordinate = lila.coordinate.ui.CoordinateUi(helpers)

val atomUi = lila.ui.AtomUi(helpers.routeUrl)

// irwin removed - mod module deleted
// val irwin = lila.irwin.IrwinUi(helpers)(menu = views.mod.ui.menu)

val dgt = lila.web.ui.DgtUi(helpers)

val relation = lila.relation.ui.RelationUi(helpers)

object oAuth:
  val token = lila.oauth.ui.TokenUi(helpers)(account.ui.AccountPage, env.mode)
  val authorize = lila.oauth.ui.AuthorizeUi(helpers)(lightUserFallback)

val style = lila.plan.ui.PlanStyle(helpers)
val plan = lila.plan.ui.PlanUi(helpers)(style, netConfig.email)
val planPages = lila.plan.ui.PlanPages(helpers)(lila.fishnet.FishnetLimiter.maxPerDay)

val feed =
  lila.feed.ui.FeedUi(helpers, atomUi)(title => _ ?=> site.ui.SitePage(title, "news", ""))(using
    env.executor
  )

// cms removed - module deleted
// val cms = lila.cms.ui.CmsUi(helpers)(views.mod.ui.menu("cms"))

// event removed - mod module deleted
// val event = lila.event.ui.EventUi(helpers)(views.mod.ui.menu("event"))

// userTournament removed - tournament module deleted
// val userTournament = lila.tournament.ui.UserTournament(helpers, tournament.ui)

object account:
  val ui = lila.pref.ui.AccountUi(helpers)
  val pages = lila.pref.ui.AccountPages(helpers, ui, flagApi)
  val pref = lila.pref.ui.AccountPref(helpers, prefHelper, ui)
  val twoFactor = lila.pref.ui.TwoFactorUi(helpers, ui)(netConfig.domain)
  val security = lila.security.ui.AccountSecurity(helpers)(env.net.email, ui.AccountPage)

val practice = lila.practice.ui.PracticeUi(helpers)(
  csp = analyse.ui.bits.cspExternalEngine,
  views.analyse.ui.explorerAndCevalConfig
)

object forum:
  import lila.forum.ui.*
  val bits = ForumBits(helpers)
  val post = PostUi(helpers, bits)
  val categ = CategUi(helpers, bits)
  val topic = TopicUi(helpers, bits, post)(
    captcha.apply,
    List.empty  // msg.Preset removed
  )

val timeline = lila.timeline.ui.TimelineUi(helpers)

object opening:
  val bits = lila.opening.ui.OpeningBits(helpers)
  val wiki = lila.opening.ui.WikiUi(helpers, bits)
  val ui = lila.opening.ui.OpeningUi(helpers, bits, wiki)

val video = lila.video.ui.VideoUi(helpers)

val gameSearch = lila.gameSearch.ui.GameSearchUi(helpers)(views.game.widgets(_))

val auth = lila.web.ui.AuthUi(helpers)

val storm = lila.storm.ui.StormUi(helpers)

// racer removed - module deleted
// val racer = lila.racer.ui.RacerUi(helpers)

// challenge removed - module deleted
// val challenge = lila.challenge.ui.ChallengeUi(helpers)

val dev = lila.web.ui.DevUi(helpers)(_ => scalatags.Text.all.frag())

// jsBot removed - module deleted
// val jsBot = lila.jsBot.ui.JsBotUi(helpers)

// recap removed - module deleted
// val recap = lila.recap.ui.RecapUi(helpers)
