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
      "name" -> net.domain,
      "short_name" -> "Lichess",
      "start_url" -> "/",
      "display" -> "standalone",
      "background_color" -> "#161512",
      "theme_color" -> "#161512",
      "description" -> "The (really) free, no-ads, open source chess server.",
      "icons" -> List(32, 64, 128, 192, 256, 512, 1024).map: size =>
        Json.obj(
          "src" -> s"//${net.assetDomain}/assets/logo/lichess-favicon-$size.png",
          "sizes" -> s"${size}x$size",
          "type" -> "image/png"
        ),
      "related_applications" -> Json.arr(
        Json.obj(
          "platform" -> "play",
          "url" -> "https://play.google.com/store/apps/details?id=org.lichess.mobileapp"
        ),
        Json.obj(
          "platform" -> "itunes",
          "url" -> "https://itunes.apple.com/us/app/lichess-free-online-chess/id968371784"
        )
      )
    )

  val mobileAndroidUrl = "https://play.google.com/store/apps/details?id=org.lichess.mobileV2"
  val mobileIosUrl = "https://apps.apple.com/app/lichess/id1662361230"

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
    s"https://lichess.myspreadshop.$tld/"

  val variantsJson =
    JsArray(chess.variant.Variant.list.all.map { v =>
      Json.obj(
        "id" -> v.id,
        "key" -> v.key,
        "name" -> v.name
      )
    })

  val externalLinks = Map(
    "mastodon" -> "https://mastodon.online/@lichess",
    "github" -> "https://github.com/lichess-org",
    "discord" -> "https://discord.gg/lichess",
    "bluesky" -> "https://bsky.app/profile/lichess.org",
    "youtube" -> "https://youtube.com/@LichessDotOrg",
    "twitch" -> "https://www.twitch.tv/lichessdotorg"
  )

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
