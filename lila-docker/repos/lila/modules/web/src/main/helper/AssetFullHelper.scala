package lila.web
package ui
import play.api.libs.json.JsValue

import lila.core.config.NetConfig
import lila.core.data.SafeJsonStr
import lila.ui.ScalatagsTemplate.{ *, given }
import lila.ui.{ ContentSecurityPolicy, Context, Nonce, Esm }

trait AssetFullHelper:
  self: lila.ui.AssetHelper =>
  def netConfig: NetConfig
  def manifest: AssetManifest
  def analyseEndpoints: lila.ui.AnalyseEndpoints

  def safeJsonValue(v: play.api.libs.json.JsValue) = lila.common.String.html.safeJsonValue(v)

  private lazy val socketDomains = netConfig.socketDomains ::: netConfig.socketAlts

  def siteName: String =
    if netConfig.siteName == "localhost:9663" then "lichess.dev"
    else netConfig.siteName

  def assetVersion: String = lila.core.net.AssetVersion.current.value

  // Asset URL helpers
  def assetBaseUrl: String = netConfig.assetBaseUrl.value
  def netBaseUrl: String = netConfig.baseUrl.value
  
  override def assetUrl(path: String): String =
    s"$assetBaseUrl/assets/${manifest.hashed(path).getOrElse(s"_$assetVersion/$path")}"
  
  override def staticAssetUrl(path: String): String =
    s"$assetBaseUrl/assets/$path"
  
  override def staticCompiledUrl(path: String): String =
    s"$assetBaseUrl/assets/compiled/$path"

  def preload(url: String, as: String, crossOrigin: Boolean, tpe: Option[String]): Frag =
    link(rel := "preload", href := url, attr("as") := as, tpe.map(t => attr("type") := t))
  
  def embedJsUnsafe(code: String)(nonce: Option[Nonce]): Frag = 
    nonce.fold(script(raw(code)))(n => script(attr("nonce") := n.value, raw(code)))

  override def iifeModule(path: String): Frag = 
    script(src := staticAssetUrl(path))

  private val dataCssKey = attr("data-css-key")
  def cssTag(key: String): Frag =
    link(
      dataCssKey := key,
      href := staticAssetUrl(s"css/${manifest.css(key)}"),
      rel := "stylesheet"
    )

  def jsonScript(json: JsValue | SafeJsonStr) =
    script(tpe := "application/json", st.id := "page-init-data"):
      raw:
        json match
          case json: JsValue => safeJsonValue(json).value
          case json => json.toString

  def roundNvuiTag(using ctx: Context) = ctx.blind.option(Esm("round.nvui"))
  def cashTag: Frag = iifeModule("javascripts/vendor/cash.min.js")
  def chessgroundTag: Frag = script(tpe := "module", src := assetUrl("npm/chessground.min.js"))

  def basicCsp(using ctx: Context): ContentSecurityPolicy =
    val sockets = socketDomains.map { x => s"wss://$x${if !ctx.req.secure then s" ws://$x" else ""}" }
    // include both ws and wss when insecure because requests may come through a secure proxy
    val localDev =
      if !ctx.req.secure then List("http://127.0.0.1:3000", "http://localhost:8666") else Nil
    lila.web.ContentSecurityPolicy.page(
      netConfig.assetDomain,
      netConfig.assetDomain.value :: sockets ::: analyseEndpoints.explorer :: analyseEndpoints.tablebase :: localDev
    )

  def embedCsp: ContentSecurityPolicy =
    lila.web.ContentSecurityPolicy.embed(netConfig.assetDomain)

  def defaultCsp(using nonce: Option[Nonce])(using Context): ContentSecurityPolicy =
    nonce.foldLeft(basicCsp)(_.withNonce(_))
