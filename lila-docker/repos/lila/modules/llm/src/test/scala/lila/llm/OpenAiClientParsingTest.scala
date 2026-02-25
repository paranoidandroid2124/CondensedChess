package lila.llm

import munit.FunSuite
import play.api.libs.ws.StandaloneWSClient

import scala.concurrent.ExecutionContext

class OpenAiClientParsingTest extends FunSuite:

  given Executor = ExecutionContext.global

  private val client = OpenAiClient(
    ws = null.asInstanceOf[StandaloneWSClient],
    config = OpenAiConfig(
      apiKey = "test",
      endpoint = "https://api.openai.com/v1/chat/completions",
      modelSync = "gpt-4.1-mini",
      modelFallback = "",
      modelAsync = "gpt-4.1-mini",
      promptCacheKeyPrefix = "bookmaker:test",
      enabled = true,
      temperature = 0.2,
      maxOutputTokens = 256,
      requestTimeoutSeconds = 30
    )
  )

  private val parseMethod =
    val m = classOf[OpenAiClient].getDeclaredMethod("parsePolishResponse", classOf[String], classOf[String])
    m.setAccessible(true)
    m

  private def parse(body: String): Option[OpenAiPolishResult] =
    parseMethod.invoke(client, body, "gpt-4.1-mini").asInstanceOf[Option[OpenAiPolishResult]]

  test("parses JSON wrapper content into commentary") {
    val body =
      """{
        |  "model":"gpt-4.1-mini-2025-04-14",
        |  "choices":[{"message":{"content":"{\"commentary\":\"8. Bxh7+ (+1.5)\"}"},"finish_reason":"stop"}],
        |  "usage":{"prompt_tokens":10,"prompt_tokens_details":{"cached_tokens":0},"completion_tokens":5}
        |}""".stripMargin

    val result = parse(body).getOrElse(fail("expected parse result"))
    assertEquals(result.commentary, "8. Bxh7+ (+1.5)")
    assert(!result.parseWarnings.contains("json_wrapper_unparsed"))
  }

  test("accepts code-fenced JSON payload in content") {
    val body =
      """{
        |  "model":"gpt-4.1-mini-2025-04-14",
        |  "choices":[{"message":{"content":"```json\n{\"commentary\":\"Keeps SAN e4 e5\"}\n```"},"finish_reason":"stop"}],
        |  "usage":{"prompt_tokens":10,"prompt_tokens_details":{"cached_tokens":0},"completion_tokens":5}
        |}""".stripMargin

    val result = parse(body).getOrElse(fail("expected parse result"))
    assertEquals(result.commentary, "Keeps SAN e4 e5")
  }

  test("returns wrapper text with warning when wrapper parse fails") {
    val body =
      """{
        |  "model":"gpt-4.1-mini-2025-04-14",
        |  "choices":[{"message":{"content":"{\"commentary\":\"broken wrapper\""},"finish_reason":"stop"}],
        |  "usage":{"prompt_tokens":10,"prompt_tokens_details":{"cached_tokens":0},"completion_tokens":5}
        |}""".stripMargin

    val result = parse(body).getOrElse(fail("expected parse result"))
    assert(result.parseWarnings.contains("json_wrapper_unparsed"))
    assert(result.commentary.startsWith("{\"commentary\""))
  }
