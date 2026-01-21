package lila.llm.tools

import scala.concurrent.{ Await, ExecutionContext }
import scala.concurrent.duration.*

import akka.actor.ActorSystem

import play.api.libs.ws.ahc.StandaloneAhcWSClient
import play.shaded.ahc.org.asynchttpclient.DefaultAsyncHttpClient

import lila.llm.{ CommentResponse, LlmApi, LlmClient, LlmConfig }
import lila.llm.analysis.OpeningExplorerClient
import lila.llm.model.strategic.VariationLine

/**
 * Manual end-to-end smoke test for Stage 2 Bookmaker:
 * - runs the rule-based pipeline
 * - fetches Masters Opening Explorer data (network)
 * - renders book-style prose including Masters citations when available
 *
 * Usage (from `lila-docker/repos/lila`):
 *   sbt "llm/runMain lila.llm.tools.BookmakerLiveSmokeTest"
 */
object BookmakerLiveSmokeTest:

  def main(args: Array[String]): Unit =
    given ExecutionContext = ExecutionContext.global
    given ActorSystem = ActorSystem("bookmaker-live-smoke-test")

    val ws = new StandaloneAhcWSClient(new DefaultAsyncHttpClient())
    try
      val config = LlmConfig(apiKey = "", model = "disabled", enabled = false)
      val client = LlmClient(ws, config)
      val explorer = OpeningExplorerClient(ws)
      val api = LlmApi(client, explorer)

      // Corpus case: ruy_c3_prepare_d4 (ply 13)
      val fen =
        "r1bqk2r/pppn1ppp/2p5/2b1p3/4P3/3P1N2/PPP2PPP/RNBQ1RK1 w kq - 2 1"

      val playedMove = "c2c3"
      val variations = List(
        VariationLine(
          moves = List("c2c3", "e8g8", "d3d4", "c5d6", "d4e5", "d6e5"),
          scoreCp = 40,
          depth = 18
        ),
        VariationLine(
          moves = List("b1d2", "e8g8", "d3d4", "e5d4"),
          scoreCp = 25,
          depth = 18
        ),
        VariationLine(
          moves = List("h2h3", "h7h6", "b1d2", "e8g8"),
          scoreCp = 15,
          depth = 18
        )
      )

      val fut: scala.concurrent.Future[Option[CommentResponse]] =
        api.bookmakerCommentPosition(
          fen = fen,
          lastMove = Some(playedMove),
          eval = None,
          variations = Some(variations),
          probeResults = None,
          opening = Some("Ruy Lopez, Exchange Variation"),
          phase = "Opening",
          ply = 13
        )

      val res = Await.result(fut, 10.seconds)

      res match
        case None =>
          System.err.println("[smoke] No commentary produced (unexpected).")
          sys.exit(2)
        case Some(r) =>
          println(r.commentary.trim)
          println()
          val hasMasters = r.commentary.toLowerCase.contains("masters games")
          println(s"[smoke] masters_paragraph=${if hasMasters then "yes" else "no"}")

    finally ws.close()
    summon[ActorSystem].terminate()
