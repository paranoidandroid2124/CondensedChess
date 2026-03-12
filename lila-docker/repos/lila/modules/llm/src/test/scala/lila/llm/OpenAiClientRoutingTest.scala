package lila.llm

import munit.FunSuite
import play.api.libs.ws.StandaloneWSClient

import scala.concurrent.ExecutionContext

class OpenAiClientRoutingTest extends FunSuite:

  given Executor = ExecutionContext.global

  private val client = OpenAiClient(
    ws = null.asInstanceOf[StandaloneWSClient],
    config = OpenAiConfig(
      apiKey = "test",
      endpoint = "https://api.openai.com/v1/chat/completions",
      modelSync = "gpt-5-mini",
      modelFallback = "gpt-4.1-mini",
      modelAsync = "gpt-5-mini",
      modelProSync = "gpt-5.2",
      modelProFallback = "gpt-5-mini",
      modelProAsync = "gpt-5.2",
      modelActiveSync = "gpt-5.2",
      modelActiveFallback = "gpt-5-mini",
      modelActiveAsync = "gpt-5.2",
      reasoningEffortActive = "none",
      promptCacheKeyPrefix = "bookmaker:test",
      enabled = true,
      temperature = 0.2,
      maxOutputTokens = 256,
      requestTimeoutSeconds = 30
    )
  )

  private val defaultReasoningMethod =
    val m = classOf[OpenAiClient].getDeclaredMethod("defaultReasoningEffortForModel", classOf[String])
    m.setAccessible(true)
    m

  private def defaultReasoning(model: String): Option[String] =
    defaultReasoningMethod.invoke(client, model).asInstanceOf[Option[String]]

  test("active note route uses dedicated active model config") {
    val route = client.activeNoteRouteSummary(asyncTier = false, planTier = PlanTier.Pro, llmLevel = LlmLevel.Active)

    assertEquals(route.primary, "gpt-5.2")
    assertEquals(route.fallback, Some("gpt-5-mini"))
    assertEquals(route.reasoningEffort, Some("none"))
  }

  test("standard polish route stays on mini defaults") {
    val route = client.standardRouteSummary(asyncTier = false, planTier = PlanTier.Basic, llmLevel = LlmLevel.Polish)

    assertEquals(route.primary, "gpt-5-mini")
    assertEquals(route.fallback, Some("gpt-4.1-mini"))
    assertEquals(route.reasoningEffort, Some("minimal"))
  }

  test("modern GPT-5 routes do not fall back to minimal reasoning effort") {
    assertEquals(defaultReasoning("gpt-5.2"), Some("none"))
    assertEquals(defaultReasoning("gpt-5.4"), Some("none"))
    assertEquals(defaultReasoning("gpt-5-mini"), Some("minimal"))
  }
