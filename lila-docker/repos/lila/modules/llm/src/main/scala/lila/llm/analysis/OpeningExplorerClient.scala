package lila.llm.analysis

import play.api.libs.json.*
import play.api.libs.ws.StandaloneWSClient
import play.api.libs.ws.DefaultBodyReadables.*
import scala.collection.concurrent.TrieMap
import scala.concurrent.{ ExecutionContext, Future }
import scala.concurrent.duration.*
import scala.util.Random
import java.nio.charset.StandardCharsets
import lila.llm.model._
import lila.llm.PgnAnalysisHelper

/**
 * P3: Client for Lichess Opening Explorer API (Masters Database).
 * Provides historical context and representative games for the narrative.
 */
final class OpeningExplorerClient(ws: StandaloneWSClient)(using ec: ExecutionContext):
  private val mastersUrl = "https://explorer.lichess.ovh/masters"
  private val mastersPgnUrl = "https://explorer.lichess.ovh/master/pgn"
  private val requestTimeout = 2000.millis
  private val pgnTimeout = 2000.millis
  private val cacheTtl = 10.minutes
  private val maxCacheEntries = 2000
  private val maxPgnSnippets = 5
  private val snippetMaxPlies = 8 // 4 moves from the matched position

  private val cache = TrieMap.empty[String, (Long, Option[OpeningReference])]
  private val inFlight = TrieMap.empty[String, Future[Option[OpeningReference]]]

  /**
   * Fetches master game statistics and top games for a given FEN.
   */
  def fetchMasters(fen: String): Future[Option[OpeningReference]] =
    val now = System.currentTimeMillis()
    val fenKey = normalizeFen(fen)

    cache.get(fenKey) match
      case Some((ts, refOpt)) if (now - ts) <= cacheTtl.toMillis =>
        Future.successful(refOpt)
      case _ =>
        inFlight.getOrElseUpdate(
          fenKey,
          ws.url(mastersUrl)
            .withQueryStringParameters("fen" -> fenKey)
            .withRequestTimeout(requestTimeout)
            .get()
            .flatMap { response =>
              if response.status == 200 then
                enrichWithSnippets(fenKey, parseExplorerResponse(response.body[String]))
              else
                if (response.status != 404) then
                  lila.log("llm").warn(s"Explorer API error: ${response.status} for FEN $fenKey")
                Future.successful(None)
            }
            .recover { case e: Throwable =>
              lila.log("llm").error(s"Explorer API call failed: ${e.getMessage}")
              None
            }
            .map { refOpt =>
              cache.put(fenKey, System.currentTimeMillis() -> refOpt)
              pruneCache()
              refOpt
            }
            .andThen { case _ =>
              inFlight.remove(fenKey)
            }
        )

  private def normalizeFen(fen: String): String =
    val parts = fen.trim.split("\\s+").toList
    parts match
      case a :: b :: c :: d :: _ => s"$a $b $c $d 0 1"
      case _                     => fen.trim

  private def fenKey4(fen: String): String =
    fen.trim.split("\\s+").take(4).mkString(" ")

  private def enrichWithSnippets(fen: String, refOpt: Option[OpeningReference]): Future[Option[OpeningReference]] =
    refOpt match
      case None => Future.successful(None)
      case Some(ref) if ref.sampleGames.isEmpty => Future.successful(Some(ref))
      case Some(ref) =>
        val targetFenKey = fenKey4(fen)
        val games = Random.shuffle(ref.sampleGames).take(maxPgnSnippets)
        Future
          .traverse(games) { g =>
            fetchPgnSnippet(gameId = g.id, targetFenKey = targetFenKey).map(sn => g.copy(pgn = sn))
          }
          .map { enriched =>
            val updated = ref.sampleGames.zipWithIndex.map { (g, idx) =>
              enriched.lift(idx).getOrElse(g)
            }
            Some(ref.copy(sampleGames = updated))
          }

  /**
   * Enriches an existing OpeningReference with snippets extracted from raw PGNs
   * already present in the sampleGames. (Used for client-side injection).
   */
  def enrichWithLocalPgn(fen: String, ref: OpeningReference): OpeningReference =
    val targetKey = fenKey4(fen)
    val enriched = ref.sampleGames.map { g =>
      g.pgn match
        case Some(raw) if raw.contains("[Event") || raw.contains("1.") => // Looks like raw PGN
          truncateRawPgn(raw, targetKey) match
            case Some(snippet) => g.copy(pgn = Some(snippet))
            case None          => g
        case _ => g
    }
    ref.copy(sampleGames = enriched)

  private def truncateRawPgn(pgn: String, targetKey: String): Option[String] =
    PgnAnalysisHelper.extractPlyData(pgn).toOption.flatMap { plyData =>
      val idxOpt = plyData.indexWhere(p => fenKey4(p.fen) == targetKey) match
        case -1 => None
        case i  => Some(i)
      idxOpt.flatMap { idx =>
        val slice = plyData.drop(idx).take(snippetMaxPlies)
        Option.when(slice.nonEmpty)(formatPlySnippet(slice))
      }
    }

  private def fetchPgnSnippet(gameId: String, targetFenKey: String): Future[Option[String]] =
    ws.url(s"$mastersPgnUrl/$gameId")
      .withRequestTimeout(pgnTimeout)
      .get()
      .map { response =>
        if response.status != 200 then None
        else
          val bytes = response.body[Array[Byte]]
          val pgn = String(bytes, StandardCharsets.UTF_8)
          PgnAnalysisHelper.extractPlyData(pgn).toOption.flatMap { plyData =>
            val idxOpt = plyData.indexWhere(p => fenKey4(p.fen) == targetFenKey) match
              case -1 => None
              case i  => Some(i)
            idxOpt.flatMap { idx =>
              val slice = plyData.drop(idx).take(snippetMaxPlies)
              Option.when(slice.nonEmpty)(formatPlySnippet(slice))
            }
          }
      }
      .recover { case e: Throwable =>
        lila.log("llm").debug(s"Explorer PGN fetch failed for $gameId: ${e.getMessage}")
        None
      }

  private def formatPlySnippet(plys: List[PgnAnalysisHelper.PlyData]): String =
    val sb = new StringBuilder()
    var lastMoveNo: Option[Int] = None
    var lastWasWhite = false

    plys.foreach { p =>
      val moveNo = (p.ply + 1) / 2
      val prefix =
        if (p.ply % 2 == 1) s"$moveNo."
        else if (lastWasWhite && lastMoveNo.contains(moveNo)) "" else s"$moveNo..."

      if (sb.nonEmpty) sb.append(" ")
      if (prefix.nonEmpty) sb.append(prefix).append(" ")
      sb.append(p.playedMove)

      lastMoveNo = Some(moveNo)
      lastWasWhite = (p.ply % 2 == 1)
    }

    sb.toString()

  private def parseExplorerResponse(body: String): Option[OpeningReference] =
    try
      val json = Json.parse(body)
      val white = (json \ "white").asOpt[Int].getOrElse(0)
      val draws = (json \ "draws").asOpt[Int].getOrElse(0)
      val black = (json \ "black").asOpt[Int].getOrElse(0)
      val totalGames = white + draws + black
      
      val moves = (json \ "moves").asOpt[List[JsValue]].getOrElse(Nil).map { m =>
        val mw = (m \ "white").asOpt[Int].getOrElse(0)
        val md = (m \ "draws").asOpt[Int].getOrElse(0)
        val mb = (m \ "black").asOpt[Int].getOrElse(0)
        ExplorerMove(
          uci = (m \ "uci").as[String],
          san = (m \ "san").as[String],
          total = mw + md + mb,
          white = mw,
          draws = md,
          black = mb,
          performance = (m \ "averageRating").asOpt[Int].getOrElse(0)
        )
      }

      val topGames = (json \ "topGames").asOpt[List[JsValue]].getOrElse(Nil).map { g =>
        val monthNum =
          (g \ "month").asOpt[Int].orElse {
            (g \ "month").asOpt[String].flatMap { s =>
              // either "2019-08" or "08"
              s.split("-").lastOption.flatMap(_.toIntOption)
            }
          }.getOrElse(1)

        ExplorerGame(
          id = (g \ "id").as[String],
          winner = (g \ "winner").asOpt[String].flatMap {
            case "white" => Some(chess.White)
            case "black" => Some(chess.Black)
            case _ => None
          },
          white = ExplorerPlayer(
            (g \ "white" \ "name").asOpt[String].getOrElse("?"), 
            (g \ "white" \ "rating").asOpt[Int].getOrElse(0)
          ),
          black = ExplorerPlayer(
            (g \ "black" \ "name").asOpt[String].getOrElse("?"), 
            (g \ "black" \ "rating").asOpt[Int].getOrElse(0)
          ),
          year = (g \ "year").asOpt[Int].getOrElse(0),
          month = monthNum,
          event = (g \ "event").asOpt[String].map(_.trim).filter(_.nonEmpty)
        )
      }

      Some(OpeningReference(
        eco = (json \ "opening" \ "eco").asOpt[String],
        name = (json \ "opening" \ "name").asOpt[String],
        totalGames = totalGames,
        topMoves = moves,
        sampleGames = topGames
      ))
    catch
      case e: Throwable =>
        lila.log("llm").warn(s"Failed to parse Explorer response: ${e.getMessage}")
        None

  private def pruneCache(): Unit =
    if (cache.size <= maxCacheEntries) return

    val now = System.currentTimeMillis()
    // 1) Drop expired entries first
    cache.foreach { case (fen, (ts, _)) =>
      if ((now - ts) > cacheTtl.toMillis) cache.remove(fen)
    }

    // 2) If still too large, drop oldest entries (best-effort LRU)
    val overflow = cache.size - maxCacheEntries
    if (overflow > 0) {
      cache.toList
        .sortBy { case (_, (ts, _)) => ts }
        .take(overflow)
        .foreach { case (fen, _) => cache.remove(fen) }
    }
