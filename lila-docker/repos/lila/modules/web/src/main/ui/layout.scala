package lila.web
package ui


import lila.ui.*
import ShellPrimitives.*

import ScalatagsTemplate.{ *, given }

case class PieceSetImages(assetHelper: lila.web.ui.AssetFullHelper):
  def load(name: String): Frag =
    import assetHelper.*

    val safeName = name.filter: c =>
      (c >= 'a' && c <= 'z') || (c >= '0' && c <= '9') || c == '-' || c == '_'

    def piece(file: String) = assetUrl(s"piece/$safeName/$file")

    raw(
      s"""<style id="piece-set-vars">
  body{
    ---white-pawn:url("${piece("wP.svg")}");
    ---white-knight:url("${piece("wN.svg")}");
    ---white-bishop:url("${piece("wB.svg")}");
    ---white-rook:url("${piece("wR.svg")}");
    ---white-queen:url("${piece("wQ.svg")}");
    ---white-king:url("${piece("wK.svg")}");
    ---black-pawn:url("${piece("bP.svg")}");
    ---black-knight:url("${piece("bN.svg")}");
    ---black-bishop:url("${piece("bB.svg")}");
    ---black-rook:url("${piece("bR.svg")}");
    ---black-queen:url("${piece("bQ.svg")}");
    ---black-king:url("${piece("bK.svg")}");
  }
</style>"""
    )

final class layout(helpers: Helpers, assetHelper: lila.web.ui.AssetFullHelper):
  import helpers.{ *, given }
  import assetHelper.{ *, given }

  val doctype = raw("<!DOCTYPE html>")
  def htmlTag = html()
  val topComment = raw("""<!-- Chesstory Analysis Engine -->""")
  val charset = raw("""<meta charset="utf-8">""")
  val viewport = raw:
    """<meta name="viewport" content="width=device-width,initial-scale=1,viewport-fit=cover">"""
  def metaCsp(csp: ContentSecurityPolicy): Frag = raw:
    s"""<meta http-equiv="Content-Security-Policy" content="${lila.web.ContentSecurityPolicy.render(csp)}">"""
  def metaCsp(csp: Option[ContentSecurityPolicy])(using Context, Option[Nonce]): Frag =
    metaCsp(csp.getOrElse(defaultCsp))
  def systemThemeScript(nonce: Option[Nonce]) =
    embedJsUnsafe(
      "if (window.matchMedia('(prefers-color-scheme: light)')?.matches) " +
        "document.documentElement.classList.add('light');"
    )(nonce)
  val noTranslate = raw("""<meta name="google" content="notranslate">""")

  private def fontPreload(path: String) = preload(assetUrl(s"font/$path"), "font", true, "font/woff2".some)
  def imagePreload(url: String): Frag = preload(url, "image", false, None)

  def fontsPreload(using ctx: Context) = frag(
    fontPreload("chesstory.woff2"),
    fontPreload("noto-sans-latin.woff2"),
    fontPreload("roboto-latin.woff2"),
    ctx.pref.pieceNotationIsLetter.not.option(fontPreload("chesstory-chess.woff2"))
  )

  def clinput(using ctx: Context) =
    val label = "Search"
    div(id := "clinput")(
      a(cls := "link", dataIcon := Icon.Search),
      input(
        spellcheck := "false",
        autocomplete := ctx.blind.toString,
        aria.label := label,
        placeholder := label,
        enterkeyhint := "search"
      )
    )

  def botImage = img(
    src := staticAssetUrl("images/icons/bot.png"),
    title := "Robot chess",
    style := "display:inline;width:34px;height:34px;vertical-align:top;margin-right:5px;vertical-align:text-top"
  )

  val manifests = raw:
    """<link rel="manifest" href="/manifest.json">"""

  val favicons = raw:
    List(512, 256, 192, 128, 64)
      .map: px =>
        s"""<link rel="icon" type="image/png" href="$assetBaseUrl/assets/logo/chesstory-favicon-$px.png" sizes="${px}x$px">"""
      .mkString(
        "",
        "",
        s"""<link id="favicon" rel="icon" type="image/png" href="$assetBaseUrl/assets/logo/chesstory-favicon-32.png" sizes="32x32">"""
      )

  def dasher(me: User) =
    div(cls := "dasher")(
      a(id := "user_tag", cls := "toggle link", href := routes.Auth.logoutGet)(me.username),
      div(id := "dasher_app", cls := "dropdown")
    )

  def anonDasher(using ctx: Context) =
    val prefs = "Preferences"
    frag(
      div(cls := "signin-or-signup")(
        a(href := s"${routes.Auth.magicLink.url}?referrer=${ctx.req.path}", cls := "signin")("Sign in")
      ),
      div(cls := "dasher")(
        button(cls := "toggle anon link", title := prefs, aria.label := prefs, dataIcon := Icon.Gear),
        div(id := "dasher_app", cls := "dropdown")
      )
    )

  def scriptsPreload(keys: List[String]) =
    frag(cashTag, assetHelper.manifest.jsAndDeps("manifest" :: keys).map(jsTag))

  private def jsTag(name: String): Frag =
    script(tpe := "module", src := staticCompiledUrl(name))

  def modulesInit(modules: EsmList, nonce: Optionce) =
    modules.flatMap(_.map(_.init(nonce))) // in body

  def inlineJs(nonce: Nonce, modules: EsmList = Nil): Frag =
    val code =
      (Esm("site").some :: modules)
        .flatMap(_.flatMap(m => assetHelper.manifest.inlineJs(m.key).map(js => s"(function(){${js}})()")))
        .mkString(";")
    embedJsUnsafe(code)(nonce.some)

  private def hrefLang(langStr: String, path: String) =
    s"""<link rel="alternate" hreflang="$langStr" href="$netBaseUrl$path"/>"""

  def hrefLangs(path: LangPath) = raw:
    hrefLang("x-default", path.value) + hrefLang("en", path.value)

  def pageZoom(using ctx: Context): Int = {
    def oldZoom = ctx.req.session.get("zoom2").flatMap(_.toIntOption).map(_ - 100)
    ctx.req.cookies
      .get("zoom")
      .map(_.value)
      .flatMap(_.toIntOption)
      .orElse(oldZoom)
      .filter(0 <=)
      .filter(100 >=)
  } | 80

  val dataVapid = attr("data-vapid")
  def dataSocketDomains = attr("data-socket-domains") := netConfig.socketDomains.mkString(",")
  val dataNonce = attr("data-nonce")
  val dataAnnounce = attr("data-announce")
  val dataSoundSet = attr("data-sound-set")
  val dataTheme = attr("data-theme")
  val dataDirection = attr("data-direction")
  val dataBoard = attr("data-board")
  val dataPieceSet = attr("data-piece-set")
  val dataBoard3d = attr("data-board3d")
  val dataPieceSet3d = attr("data-piece-set3d")
  val dataAssetUrl = attr("data-asset-url") := netConfig.assetBaseUrl.value
  val dataAssetVersion = attr("data-asset-version")

  val spinnerMask = raw:
    """<svg width="0" height="0"><mask id="mask"><path fill="#fff" stroke="#fff" stroke-linejoin="round" d="M38.956.5c-3.53.418-6.452.902-9.286 2.984C5.534 1.786-.692 18.533.68 29.364 3.493 50.214 31.918 55.785 41.329 41.7c-7.444 7.696-19.276 8.752-28.323 3.084C3.959 39.116-.506 27.392 4.683 17.567 9.873 7.742 18.996 4.535 29.03 6.405c2.43-1.418 5.225-3.22 7.655-3.187l-1.694 4.86 12.752 21.37c-.439 5.654-5.459 6.112-5.459 6.112-.574-1.47-1.634-2.942-4.842-6.036-3.207-3.094-17.465-10.177-15.788-16.207-2.001 6.967 10.311 14.152 14.04 17.663 3.73 3.51 5.426 6.04 5.795 6.756 0 0 9.392-2.504 7.838-8.927L37.4 7.171z"/></mask></svg>"""

  val networkAlert = a(id := "network-status", cls := "link text", dataIcon := Icon.ChasingArrows)

  private val spaceRegex = """\s{2,}+""".r
  def spaceless(html: String) = raw(spaceRegex.replaceAllIn(html.replace("\\n", ""), ""))

  def chesstoryFontFaceCss = spaceless:
    s"""
<style>
  @font-face {
    font-family: 'chesstory';
    font-display: block;
    src: url('${assetUrl("font/chesstory.woff2")}') format('woff2')
  }
</style>"""

  def bottomHtml(using ctx: Context) = frag(
    Option.when(netConfig.socketDomains.nonEmpty)(networkAlert),
    spinnerMask
  )

  def sitePreload(modules: EsmList)(using Context): Frag =
    val keys = "site" :: modules.flatMap(_.map(_.key))
    scriptsPreload("manifest" :: keys)


  object siteHeader:

    private val topnavToggle = spaceless:
      s"""
<input type="checkbox" id="$navToggleId" class="$navToggleClass fullscreen-toggle" autocomplete="off" aria-label="Navigation">
<label for="$navToggleId" class="fullscreen-mask"></label>
<label for="$navToggleId" class="$burgerClass"><span class="$burgerInnerClass"></span></label>"""

    private val siteNameFrag: Frag = frag(siteName)

    def apply(
        zenable: Boolean,
        isAppealUser: Boolean,
        challenges: Int,
        notifications: Int,
        error: Boolean,
        topnav: Frag
    )(using ctx: PageContext) =
      header(id := headerId)(
        div(cls := brandBarClass)(
          (!isAppealUser).option(topnavToggle),
          a(cls := brandClass, href := "/")(
            ctx.isBot.option(botImage),
            div(cls := markClass, dataIcon := Icon.Logo),
            div(cls := wordmarkClass)(siteNameFrag)
          ),
          (!isAppealUser).option(topnav),
          ctx.blind.option(h2("Navigation"))
        ),
        div(cls := "site-buttons")(
          (!isAppealUser).option(clinput),
          if isAppealUser then
            postForm(action := routes.Auth.logout):
              submitButton(cls := "button button-red link")("Log out")
          else
            ctx.me
              .map: me =>
                dasher(me)
              .getOrElse:
                error.not.option(anonDasher)
        )
      )
