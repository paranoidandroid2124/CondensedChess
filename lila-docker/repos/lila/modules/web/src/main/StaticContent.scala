package lila.web

import play.api.libs.json.{ JsArray, Json }
import play.api.mvc.RequestHeader

import lila.common.HTTPRequest
import lila.common.Json.given
import lila.core.config.NetConfig

object StaticContent:

  val robotsTxt = """User-agent: *
Allow: /
Disallow: /game/export/
Disallow: /games/export/
Disallow: /api/
Disallow: /opening/config/
Allow: /game/export/gif/thumbnail/
"""

  def manifest(net: NetConfig) =
    Json.obj(
      "name" -> net.siteName,
      "short_name" -> net.siteName,
      "start_url" -> "/",
      "display" -> "standalone",
      "background_color" -> "#161A25",
      "theme_color" -> "#161A25",
      "description" -> "AI chess analysis, simplified.",
      "icons" -> List(32, 64, 128, 192, 256, 512).map: size =>
        Json.obj(
          "src" -> s"//${net.assetDomain}/assets/logo/chesstory-favicon-$size.png",
          "sizes" -> s"${size}x$size",
          "type" -> "image/png"
        ),
      "related_applications" -> Json.arr()
    )

  val mobileAndroidUrl = "https://chesstory.com"
  val mobileIosUrl = "https://chesstory.com"

  def appStoreUrl(using req: RequestHeader) =
    if HTTPRequest.isAndroid(req) then mobileAndroidUrl else mobileIosUrl

  val swagStoreTlds = Map(
    "US" -> "com",
    "CA" -> "ca",
    "DE" -> "de",
    "FR" -> "fr",
    "UK" -> "co.uk",
    "IT" -> "it",
    "ES" -> "es",
    "NL" -> "nl",
    "PL" -> "pl",
    "BE" -> "be",
    "DK" -> "dk",
    "AU" -> "com.au",
    "IE" -> "ie",
    "NO" -> "no",
    "CH" -> "ch",
    "FI" -> "fi",
    "SE" -> "se",
    "AT" -> "at"
  )
  def swagUrl(countryCode: Option[String]) =
    val tld = swagStoreTlds.getOrElse(countryCode.getOrElse(""), "net")
    s"https://chesstory.com"

  val variantsJson =
    JsArray(chess.variant.Variant.list.all.map { v =>
      Json.obj(
        "id" -> v.id,
        "key" -> v.key,
        "name" -> v.name
      )
    })

  val externalLinks = Map.empty[String, String]

  def legacyQaQuestion(id: Int) =
    val analysis = routes.UserAnalysis.index.url
    id match
      case 103 => analysis
      case 258 => analysis
      case 13 => analysis
      case 87 => analysis
      case 110 => analysis
      case 29 => analysis
      case 4811 => analysis
      case 216 => analysis
      case 340 => analysis
      case 6 => analysis
      case 207 => analysis
      case 547 => analysis
      case 259 => analysis
      case 342 => analysis
      case 50 => analysis
      case 46 => analysis
      case 122 => analysis
      case _ => analysis
