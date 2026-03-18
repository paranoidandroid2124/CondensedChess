package lila.accountintel.source

import play.api.Configuration
import play.api.libs.json.*
import play.api.libs.ws.DefaultBodyReadables.*
import play.api.libs.ws.StandaloneWSClient

import java.time.{ Instant, ZoneOffset }
import java.time.format.DateTimeFormatter
import scala.concurrent.duration.*
import scala.util.control.NonFatal

import lila.llm.MoveEval
import lila.llm.model.strategic.VariationLine
import lila.accountintel.*
import lila.accountintel.AccountIntel.{ ExternalGame, normalizeProvider }

final class AccountGameSource(
    appConfig: Configuration,
    ws: StandaloneWSClient,
    cacheApi: lila.memo.CacheApi
)(using Executor)
    extends AccountIntel.AccountGameFetcher:

  private val logger = lila.log("accountIntelSource")
  private val recentGameTarget = 40
  private val chessComArchiveScanLimit = 8
  private val requestTimeout = 12.seconds
  private val fetchRetryCount =
    appConfig.getOptional[Int]("accountIntel.fetchRetryCount").getOrElse(2).max(0)
  private val fetchCacheTtl =
    appConfig.getOptional[FiniteDuration]("accountIntel.fetchCacheTtl").getOrElse(30.minutes)
  private val utcDateFmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm").withZone(ZoneOffset.UTC)
  private val userAgent =
    appConfig
      .getOptional[String]("accountIntel.userAgent")
      .map(_.trim)
      .filter(_.nonEmpty)
      .getOrElse("Chesstory AccountIntel")

  private def configured(path: String): Option[String] =
    appConfig.getOptional[String](path).map(_.trim).filter(_.nonEmpty)

  private val lichessImportApiBase =
    configured("external.import.lichess.api_base")
      .orElse(sys.env.get("LICHESS_IMPORT_API_BASE").map(_.trim).filter(_.nonEmpty))
      .getOrElse("https://lichess.org")
      .stripSuffix("/")

  private val lichessWebBase =
    configured("external.import.lichess.web_base")
      .orElse(sys.env.get("LICHESS_WEB_BASE").map(_.trim).filter(_.nonEmpty))
      .getOrElse("https://lichess.org")
      .stripSuffix("/")

  private val chessComApiBase =
    configured("external.import.chesscom.api_base")
      .orElse(sys.env.get("CHESSCOM_API_BASE").map(_.trim).filter(_.nonEmpty))
      .getOrElse("https://api.chess.com")
      .stripSuffix("/")

  private val recentGamesCache =
    cacheApi[(String, String), List[ExternalGame]](64, "accountIntel.recentGames"):
      _.expireAfterWrite(fetchCacheTtl).buildAsyncFuture: (provider, username) =>
        loadRecentGames(provider, username)

  def fetchRecentGames(provider: String, username: String): Fu[List[ExternalGame]] =
    normalizeProvider(provider).fold(fuccess(Nil)): normalized =>
      recentGamesCache.get((normalized, username.trim.toLowerCase))

  private def loadRecentGames(provider: String, username: String): Fu[List[ExternalGame]] =
    provider match
      case "lichess" => fetchRecentLichessGames(username)
      case "chesscom" => fetchRecentChessComGames(username)
      case _ => fuccess(Nil)

  private def fetchRecentLichessGames(username: String): Fu[List[ExternalGame]] =
    val url =
      s"$lichessImportApiBase/api/games/user/$username?max=$recentGameTarget&pgnInJson=true&opening=true&evals=true&analysed=true"
    getWithRetry(url, accept = Some("application/x-ndjson"))
      .map: res =>
        if res.status == 200 then
          Option(res.body[String]).toList
            .flatMap(_.split('\n').toList)
            .map(_.trim)
            .filter(_.nonEmpty)
            .flatMap(parseLichessNdjsonLine)
            .take(recentGameTarget)
        else
          logger.warn(s"accountintel lichess import failed username=$username status=${res.status}")
          Nil
      .recover { case NonFatal(err) =>
        logger.warn(s"accountintel lichess import exception username=$username err=${err.getMessage}")
        Nil
      }

  private def parseLichessNdjsonLine(line: String): Option[ExternalGame] =
    try
      val js = Json.parse(line)
      (js \ "pgn")
        .asOpt[String]
        .map(_.trim)
        .filter(_.nonEmpty)
        .map: pgn =>
          val id = (js \ "id").asOpt[String].getOrElse(s"lichess-${Math.abs(pgn.hashCode)}")
          val winner = (js \ "winner").asOpt[String]
          ExternalGame(
            provider = "lichess",
            gameId = id,
            playedAt = formatEpochMs((js \ "lastMoveAt").asOpt[Long].orElse((js \ "createdAt").asOpt[Long])),
            white = playerName(js, "white"),
            black = playerName(js, "black"),
            result =
              if winner.contains("white") then "1-0"
              else if winner.contains("black") then "0-1"
              else "1/2-1/2",
            sourceUrl = Some(s"$lichessWebBase/$id"),
            pgn = pgn,
            moveEvals = parseLichessMoveEvals(js)
          )
    catch case NonFatal(_) => None

  private def parseLichessMoveEvals(js: JsValue): List[MoveEval] =
    (js \ "analysis").asOpt[List[JsObject]].getOrElse(Nil).zipWithIndex.flatMap { case (node, idx) =>
      val cp = (node \ "eval").asOpt[Int]
      val mate = (node \ "mate").asOpt[Int]
      val best = (node \ "best").asOpt[String].filter(_.nonEmpty)
      val variations =
        best.toList.map(move => VariationLine(moves = List(move), scoreCp = cp.getOrElse(0), mate = mate))
      cp.orElse(mate.map(m => if m > 0 then 10000 - m else -10000 + m)).map { score =>
        MoveEval(
          ply = idx + 1,
          cp = cp.getOrElse(score),
          mate = mate,
          pv = best.toList,
          variations = variations
        )
      }
    }

  private def playerName(js: JsValue, side: String): String =
    (js \ "players" \ side \ "user" \ "name")
      .asOpt[String]
      .orElse((js \ "players" \ side \ "name").asOpt[String])
      .getOrElse(side.capitalize)

  private def fetchRecentChessComGames(username: String): Fu[List[ExternalGame]] =
    val archivesUrl = s"$chessComApiBase/pub/player/${username.toLowerCase}/games/archives"
    getWithRetry(archivesUrl)
      .flatMap: res =>
        if res.status == 200 then
          val archives =
            (Json.parse(res.body[String]) \ "archives")
              .asOpt[List[String]]
              .getOrElse(Nil)
              .reverse
              .take(chessComArchiveScanLimit)
          collectChessComGames(archives, Nil)
        else
          logger.warn(s"accountintel chesscom archives failed username=$username status=${res.status}")
          fuccess(Nil)
      .recover { case NonFatal(err) =>
        logger.warn(s"accountintel chesscom archives exception username=$username err=${err.getMessage}")
        Nil
      }

  private def collectChessComGames(archives: List[String], acc: List[ExternalGame]): Fu[List[ExternalGame]] =
    if acc.size >= recentGameTarget || archives.isEmpty then fuccess(acc.take(recentGameTarget))
    else
      getWithRetry(archives.head)
        .flatMap: res =>
          val merged =
            if res.status == 200 then acc ++ parseChessComArchive(res.body[String])
            else acc
          collectChessComGames(archives.tail, merged.take(recentGameTarget))
        .recoverWith { case NonFatal(_) =>
          collectChessComGames(archives.tail, acc)
        }

  private def getWithRetry(
      url: String,
      accept: Option[String] = None,
      retriesLeft: Int = fetchRetryCount
  ): Fu[play.api.libs.ws.StandaloneWSResponse] =
    ws.url(url)
      .withHttpHeaders((List("User-Agent" -> userAgent) ::: accept.toList.map("Accept" -> _))*)
      .withRequestTimeout(requestTimeout)
      .get()
      .flatMap: res =>
        if retryableStatus(res.status) && retriesLeft > 0 then getWithRetry(url, accept, retriesLeft - 1)
        else fuccess(res)
      .recoverWith { case NonFatal(err) =>
        if retriesLeft > 0 then getWithRetry(url, accept, retriesLeft - 1)
        else
          logger.warn(s"accountintel request failed url=$url err=${err.getMessage}")
          fufail(err)
      }

  private def retryableStatus(status: Int): Boolean =
    status == 408 || status == 425 || status == 429 || status >= 500

  private def parseChessComArchive(jsonText: String): List[ExternalGame] =
    (Json.parse(jsonText) \ "games")
      .asOpt[List[JsObject]]
      .getOrElse(Nil)
      .sortBy(g => (g \ "end_time").asOpt[Long].getOrElse(0L))
      .reverse
      .flatMap(parseChessComGame)

  private def parseChessComGame(game: JsObject): Option[ExternalGame] =
    (game \ "pgn")
      .asOpt[String]
      .map(_.trim)
      .filter(_.nonEmpty)
      .map: pgn =>
        val url = (game \ "url").asOpt[String]
        ExternalGame(
          provider = "chesscom",
          gameId = url
            .flatMap(_.split('/').lastOption)
            .filter(_.nonEmpty)
            .getOrElse(s"chesscom-${Math.abs(pgn.hashCode)}"),
          playedAt = formatEpochMs((game \ "end_time").asOpt[Long].map(_ * 1000L)),
          white = (game \ "white" \ "username").asOpt[String].getOrElse("White"),
          black = (game \ "black" \ "username").asOpt[String].getOrElse("Black"),
          result = chessComResult(game),
          sourceUrl = url,
          pgn = pgn
        )

  private def chessComResult(game: JsObject): String =
    val w = (game \ "white" \ "result").asOpt[String].getOrElse("").toLowerCase
    val b = (game \ "black" \ "result").asOpt[String].getOrElse("").toLowerCase
    if w == "win" then "1-0" else if b == "win" then "0-1" else "1/2-1/2"

  private def formatEpochMs(millisOpt: Option[Long]): String =
    millisOpt.map(ms => utcDateFmt.format(Instant.ofEpochMilli(ms))).getOrElse("-")
