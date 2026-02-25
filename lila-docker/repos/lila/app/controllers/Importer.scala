package controllers

import lila.app.*
import play.api.libs.json.*
import play.api.libs.ws.StandaloneWSClient
import play.api.libs.ws.DefaultBodyReadables.*
import play.api.mvc.Result

import java.nio.charset.StandardCharsets
import java.time.{ Instant, ZoneOffset }
import java.time.format.DateTimeFormatter
import java.util.Base64
import scala.concurrent.duration.*
import scala.util.control.NonFatal

object Importer:
  case class GameCard(
      provider: String,
      gameId: String,
      playedAt: String,
      white: String,
      black: String,
      result: String,
      speed: String,
      sourceUrl: Option[String],
      pgn64: String
  )

final class Importer(
    env: Env,
    ws: StandaloneWSClient
) extends LilaController(env):
  import Importer.GameCard

  private val logger = lila.log("importer")
  private val providerValues = Set("lichess", "chesscom")
  private val usernamePattern = "^[A-Za-z0-9][A-Za-z0-9_-]{1,29}$".r
  private val requestTimeout = 12.seconds
  private val recentGameTarget = 40
  private val chessComArchiveScanLimit = 8
  private val utcDateFmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm").withZone(ZoneOffset.UTC)

  def importGame = Open:
    def queryParam(name: String): Option[String] =
      req.queryString.get(name).flatMap(_.headOption).map(_.trim).filter(_.nonEmpty)
    val provider = queryParam("provider").map(_.toLowerCase).filter(providerValues).getOrElse("lichess")
    val username = queryParam("username").getOrElse("")
    Ok.page(views.importer.index(provider = provider, username = username))

  // Handles two cases:
  // 1) provider + username => redirect to fetched game list
  // 2) pgn64 => render analysis for selected game
  def sendGame = OpenBodyOf(parse.formUrlEncoded): (ctx: BodyContext[Map[String, Seq[String]]]) ?=>
    val form = ctx.body.body

    def first(name: String): Option[String] =
      form.get(name).flatMap(_.headOption).map(_.trim).filter(_.nonEmpty)

    first("pgn64").flatMap(decodePgn64) match
      case Some(pgn) =>
        renderAnalysisWithInlinePgn(pgn)
      case None =>
        val provider = first("provider").map(_.toLowerCase)
        val username = first("username")
        (provider, username) match
          case (Some("lichess"), Some(user)) => Redirect(routes.Importer.importFromLichess(user)).toFuccess
          case (Some("chesscom"), Some(user)) => Redirect(routes.Importer.importFromChessCom(user)).toFuccess
          case _ =>
            BadRequest.page(
              views.importer.index(
                error = Some("Please choose provider and enter a valid username.")
              )
            )

  def apiSendGame = Open:
    Redirect(routes.Importer.importGame).toFuccess

  def importFromLichess(username: String) = Open:
    normalizedUsername(username).fold[Fu[Result]](
      BadRequest.page(views.importer.index(error = Some("Invalid Lichess username.")))
    ) { normalized =>
      fetchRecentLichessGames(normalized).flatMap { games =>
        Ok.page(
          views.importer.gameList(
            provider = "lichess",
            username = normalized,
            games = games,
            notice = Option.when(games.isEmpty)("No public games found for this Lichess user.")
          )
        )
      }
    }

  def importFromChessCom(username: String) = Open:
    normalizedUsername(username).fold[Fu[Result]](
      BadRequest.page(views.importer.index(error = Some("Invalid Chess.com username.")))
    ) { normalized =>
      fetchRecentChessComGames(normalized).flatMap { games =>
        Ok.page(
          views.importer.gameList(
            provider = "chesscom",
            username = normalized,
            games = games,
            notice = Option.when(games.isEmpty)("No public games found for this Chess.com user.")
          )
        )
      }
    }

  private def normalizedUsername(raw: String): Option[String] =
    Option(raw).map(_.trim).filter(_.nonEmpty).flatMap(usernamePattern.findFirstIn)

  private def renderAnalysisWithInlinePgn(rawPgn: String)(using Context): Fu[Result] =
    AnalysePgnPipeline.normalizedInlinePgn(rawPgn).fold[Fu[Result]](
      BadRequest("Empty PGN payload from upstream provider").toFuccess
    ): inlinePgn =>
      Ok.page(AnalysePgnPipeline.page(inlinePgn = Some(inlinePgn)))

  private def fetchRecentLichessGames(username: String): Fu[List[GameCard]] =
    val url = s"https://lichess.org/api/games/user/$username?max=$recentGameTarget&pgnInJson=true"
    ws.url(url)
      .withHttpHeaders("Accept" -> "application/x-ndjson")
      .withRequestTimeout(requestTimeout)
      .get()
      .map { res =>
        if res.status == 200 then
          Option(res.body[String])
            .toList
            .flatMap(_.split('\n').toList)
            .map(_.trim)
            .filter(_.nonEmpty)
            .flatMap(parseLichessNdjsonLine)
            .take(recentGameTarget)
        else
          logger.warn(s"lichess import list failed username=$username status=${res.status}")
          Nil
      }
      .recover { case NonFatal(err) =>
        logger.warn(s"lichess import list exception username=$username err=${err.getMessage}")
        Nil
      }

  private def parseLichessNdjsonLine(line: String): Option[GameCard] =
    try
      val js = Json.parse(line)
      val pgn = (js \ "pgn").asOpt[String].map(_.trim).filter(_.nonEmpty)
      pgn.map { value =>
        val gameId = (js \ "id").asOpt[String].getOrElse(s"lichess-${Math.abs(value.hashCode)}")
        val white = lichessPlayerName(js, "white")
        val black = lichessPlayerName(js, "black")
        val winner = (js \ "winner").asOpt[String]
        val result = winner match
          case Some("white") => "1-0"
          case Some("black") => "0-1"
          case _             => "1/2-1/2"
        val playedAt = formatEpochMs((js \ "lastMoveAt").asOpt[Long].orElse((js \ "createdAt").asOpt[Long]))
        val speed = (js \ "speed").asOpt[String].orElse((js \ "perf").asOpt[String]).getOrElse("-")
        val sourceUrl = Some(s"https://lichess.org/$gameId")
        GameCard(
          provider = "lichess",
          gameId = gameId,
          playedAt = playedAt,
          white = white,
          black = black,
          result = result,
          speed = speed,
          sourceUrl = sourceUrl,
          pgn64 = encodePgn64(value)
        )
      }
    catch
      case NonFatal(_) => None

  private def lichessPlayerName(js: JsValue, side: String): String =
    (js \ "players" \ side \ "user" \ "name")
      .asOpt[String]
      .orElse((js \ "players" \ side \ "name").asOpt[String])
      .getOrElse(side.capitalize)

  private def fetchRecentChessComGames(username: String): Fu[List[GameCard]] =
    val archivesUrl = s"https://api.chess.com/pub/player/${username.toLowerCase}/games/archives"
    ws.url(archivesUrl)
      .withRequestTimeout(requestTimeout)
      .get()
      .flatMap { res =>
        if res.status == 200 then
          val archives =
            (Json.parse(res.body[String]) \ "archives")
              .asOpt[List[String]]
              .getOrElse(Nil)
              .reverse
              .take(chessComArchiveScanLimit)
          collectChessComGames(archives, acc = Nil)
        else
          logger.warn(s"chesscom archives failed username=$username status=${res.status}")
          fuccess(Nil)
      }
      .recover { case NonFatal(err) =>
        logger.warn(s"chesscom archives exception username=$username err=${err.getMessage}")
        Nil
      }

  private def collectChessComGames(
      archives: List[String],
      acc: List[GameCard]
  ): Fu[List[GameCard]] =
    if acc.size >= recentGameTarget || archives.isEmpty then
      fuccess(acc.take(recentGameTarget))
    else
      val head = archives.head
      val tail = archives.tail
      ws.url(head)
        .withRequestTimeout(requestTimeout)
        .get()
        .flatMap { res =>
          val merged =
            if res.status == 200 then acc ++ parseChessComArchive(res.body[String])
            else acc
          collectChessComGames(tail, merged.take(recentGameTarget))
        }
        .recoverWith { case NonFatal(_) =>
          collectChessComGames(tail, acc)
        }

  private def parseChessComArchive(jsonText: String): List[GameCard] =
    val games = (Json.parse(jsonText) \ "games").asOpt[List[JsObject]].getOrElse(Nil)
    games
      .sortBy(g => (g \ "end_time").asOpt[Long].getOrElse(0L))
      .reverse
      .flatMap(parseChessComGame)

  private def parseChessComGame(game: JsObject): Option[GameCard] =
    val pgn = (game \ "pgn").asOpt[String].map(_.trim).filter(_.nonEmpty)
    pgn.map { value =>
      val sourceUrl = (game \ "url").asOpt[String]
      val gameId = sourceUrl.flatMap(_.split('/').lastOption).filter(_.nonEmpty).getOrElse(s"chesscom-${Math.abs(value.hashCode)}")
      val white = (game \ "white" \ "username").asOpt[String].getOrElse("White")
      val black = (game \ "black" \ "username").asOpt[String].getOrElse("Black")
      val result = chessComResult(game)
      val speed = (game \ "time_class").asOpt[String].getOrElse("-")
      val playedAt = formatEpochMs((game \ "end_time").asOpt[Long].map(_ * 1000L))
      GameCard(
        provider = "chesscom",
        gameId = gameId,
        playedAt = playedAt,
        white = white,
        black = black,
        result = result,
        speed = speed,
        sourceUrl = sourceUrl,
        pgn64 = encodePgn64(value)
      )
    }

  private def chessComResult(game: JsObject): String =
    val w = (game \ "white" \ "result").asOpt[String].getOrElse("").toLowerCase
    val b = (game \ "black" \ "result").asOpt[String].getOrElse("").toLowerCase
    if w == "win" then "1-0"
    else if b == "win" then "0-1"
    else "1/2-1/2"

  private def formatEpochMs(millisOpt: Option[Long]): String =
    millisOpt
      .map(ms => utcDateFmt.format(Instant.ofEpochMilli(ms)))
      .getOrElse("-")

  private def encodePgn64(pgn: String): String =
    Base64.getUrlEncoder.withoutPadding().encodeToString(pgn.getBytes(StandardCharsets.UTF_8))

  private def decodePgn64(encoded: String): Option[String] =
    val normalized = Option(encoded).map(_.trim).filter(_.nonEmpty)
    normalized.flatMap { raw =>
      val padded =
        raw.length % 4 match
          case 0 => raw
          case mod => raw + ("=" * (4 - mod))
      try
        val text = new String(Base64.getUrlDecoder.decode(padded), StandardCharsets.UTF_8).trim
        Option(text).filter(_.nonEmpty)
      catch
        case NonFatal(_) => None
    }
