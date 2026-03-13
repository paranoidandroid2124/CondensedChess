package lila.llm

import munit.FunSuite
import play.api.libs.ws.StandaloneWSClient

import scala.concurrent.ExecutionContext

class ActiveNoteCrossProviderRoutingTest extends FunSuite:

  given Executor = ExecutionContext.global

  private def api(openAiEnabled: Boolean): LlmApi =
    val openAi = OpenAiClient(
      ws = null.asInstanceOf[StandaloneWSClient],
      config = OpenAiConfig(
        apiKey = if openAiEnabled then "test" else "",
        endpoint = "https://api.openai.com/v1/chat/completions",
        modelSync = "gpt-5-mini",
        modelFallback = "gpt-4.1-mini",
        modelAsync = "gpt-5-mini",
        modelProSync = "gpt-5-mini",
        modelProFallback = "gpt-5-mini",
        modelProAsync = "gpt-5-mini",
        modelActiveSync = "gpt-5-mini",
        modelActiveFallback = "gpt-4.1-mini",
        modelActiveAsync = "gpt-5-mini",
        reasoningEffortActive = "none",
        promptCacheKeyPrefix = "bookmaker:test",
        enabled = openAiEnabled,
        temperature = 0.2,
        maxOutputTokens = 256,
        requestTimeoutSeconds = 30
      )
    )
    val gemini = GeminiClient(
      ws = null.asInstanceOf[StandaloneWSClient],
      config = GeminiConfig(
        apiKey = "test",
        model = "gemini-2.5-flash-latest",
        modelActive = "gemini-3-flash-preview",
        enabled = true,
        temperature = 0.4,
        maxOutputTokens = 256,
        contextCacheTtlMinutes = 60,
        requestTimeoutSeconds = 30
      )
    )

    LlmApi(
      openingExplorer = null,
      geminiClient = gemini,
      openAiClient = openAi,
      commentaryCache = CommentaryCache(),
      llmConfig = LlmConfig.fromEnv,
      providerConfig =
        LlmProviderConfig(
          provider = "openai",
          providerActiveNote = "gemini",
          promptVersion = "v1",
          polishGateThreshold = 0.9,
          premiumOnly = true,
          defaultLang = "en"
        ),
      ccaHistoryRepo = None
    )

  private def resolvedRouteMeta(api: LlmApi): Product =
    val m = classOf[LlmApi].getDeclaredMethod(
      "resolvedActiveNoteRouteMeta",
      classOf[Boolean],
      classOf[String],
      classOf[String]
    )
    m.setAccessible(true)
    m.invoke(api, Boolean.box(false), PlanTier.Pro, LlmLevel.Active).asInstanceOf[Product]

  test("gemini active note route exposes openai fallback model when openai is enabled") {
    val meta = resolvedRouteMeta(api(openAiEnabled = true))

    assertEquals(meta.productElement(0), "gemini")
    assertEquals(meta.productElement(1), Some("gemini-3-flash-preview"))
    assertEquals(meta.productElement(2), Some("gpt-5-mini"))
    assertEquals(meta.productElement(3), None)
  }

  test("gemini active note route leaves fallback model empty when openai is disabled") {
    val meta = resolvedRouteMeta(api(openAiEnabled = false))

    assertEquals(meta.productElement(0), "gemini")
    assertEquals(meta.productElement(1), Some("gemini-3-flash-preview"))
    assertEquals(meta.productElement(2), None)
    assertEquals(meta.productElement(3), None)
  }
