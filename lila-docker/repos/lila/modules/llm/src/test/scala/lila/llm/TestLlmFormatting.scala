package lila.llm

import scala.concurrent.{ Await, ExecutionContext, ExecutionContextExecutor }
import scala.concurrent.duration.*
import akka.actor.ActorSystem
import munit.FunSuite
import play.api.libs.ws.ahc.StandaloneAhcWSClient
import play.shaded.ahc.org.asynchttpclient.DefaultAsyncHttpClient
import lila.llm.model.strategic.StrategicSalience

class TestLlmFormatting extends FunSuite:
  override val munitTimeout: FiniteDuration = 15.minutes
  given ExecutionContextExecutor = ExecutionContext.global

  test("OpenAI model should preserve frontend structure anchors (MV, MK, etc) in Full Review mode") {
    val apiKey = sys.env.get("OPENAI_API_KEY")
    if apiKey.isEmpty || apiKey.get.trim.isEmpty then
      assert(true) // skip if no key
    else
      given ActorSystem = ActorSystem("llm-formatting-test")
      val ws = new StandaloneAhcWSClient(new DefaultAsyncHttpClient())

      try
        val config = OpenAiConfig(
          apiKey = apiKey.get,
          endpoint = "https://api.openai.com/v1/chat/completions",
          modelSync = "gpt-4o-mini", // Fallback / Older generation model
          modelFallback = "gpt-4o-mini",
          modelAsync = "gpt-4o-mini",
          promptCacheKeyPrefix = "test",
          enabled = true,
          temperature = 0.2,
          maxOutputTokens = 256,
          requestTimeoutSeconds = 30
        )
        val client = OpenAiClient(ws, config)

        val originalText = "A critical moment! White played [[MV_1]], aggressively challenging the center. Black's response, [[MV_2]], was a severe inaccuracy because it allows [[MV_3]] winning material."
        
        val f = client.polishSync(
          prose = originalText,
          phase = "middlegame",
          evalDelta = Some(-250),
          concepts = List("blunder", "central_tension", "tactical_oversight"),
          fen = "r1bqk2r/pppn1ppp/2p5/2b1p3/4P3/3P1N2/PPP2PPP/RNBQ1RK1 w kq - 2 1",
          salience = Some(StrategicSalience.High),
          momentType = Some("Blunder"),
          lang = "en"
        )

        val result = Await.result(f, 30.seconds)
        
        assert(result.isDefined, "OpenAI API should return a result")
        val polished = result.get.commentary
        
        println("--- ORIGINAL ---")
        println(originalText)
        println("--- POLISHED ---")
        println(polished)

        assert(polished.contains("[[MV_1]]"), "Must preserve [[MV_1]]")
        assert(polished.contains("[[MV_2]]"), "Must preserve [[MV_2]]")
        assert(polished.contains("[[MV_3]]"), "Must preserve [[MV_3]]")
      finally
        ws.close()
        summon[ActorSystem].terminate()
  }
