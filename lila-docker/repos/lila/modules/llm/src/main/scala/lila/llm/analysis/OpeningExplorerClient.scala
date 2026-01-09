package lila.llm.analysis

import play.api.libs.json.*
import play.api.libs.ws.StandaloneWSClient
import play.api.libs.ws.DefaultBodyReadables.*
import scala.collection.concurrent.TrieMap
import scala.concurrent.{ ExecutionContext, Future }
import scala.concurrent.duration.*
import lila.llm.model._

/**
 * P3: Client for Lichess Opening Explorer API (Masters Database).
 * Provides historical context and representative games for the narrative.
 */
final class OpeningExplorerClient(ws: StandaloneWSClient)(using ec: ExecutionContext):
  private val mastersUrl = "https://explorer.lichess.ovh/masters"
  private val requestTimeout = 800.millis
  private val cacheTtl = 10.minutes
  private val maxCacheEntries = 2000

  private val cache = TrieMap.empty[String, (Long, Option[OpeningReference])]
  private val inFlight = TrieMap.empty[String, Future[Option[OpeningReference]]]

  /**
   * Fetches master game statistics and top games for a given FEN.
   */
  def fetchMasters(fen: String): Future[Option[OpeningReference]] =
    val now = System.currentTimeMillis()

    cache.get(fen) match
      case Some((ts, refOpt)) if (now - ts) <= cacheTtl.toMillis =>
        Future.successful(refOpt)
      case _ =>
        inFlight.getOrElseUpdate(
          fen,
          ws.url(mastersUrl)
            .withQueryStringParameters("fen" -> fen)
            .withRequestTimeout(requestTimeout)
            .get()
            .map { response =>
              if response.status == 200 then
                parseExplorerResponse(response.body[String])
              else
                if (response.status != 404) then
                  lila.log("llm").warn(s"Explorer API error: ${response.status} for FEN $fen")
                None
            }
            .recover { case e: Throwable =>
              lila.log("llm").error(s"Explorer API call failed: ${e.getMessage}")
              None
            }
            .map { refOpt =>
              cache.put(fen, System.currentTimeMillis() -> refOpt)
              pruneCache()
              refOpt
            }
            .andThen { case _ =>
              inFlight.remove(fen)
            }
        )

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
          month = (g \ "month").asOpt[Int].getOrElse(1)
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
