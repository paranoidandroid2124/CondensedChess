package lila.llm

case class OpenAiConfig(
    apiKey: String,
    endpoint: String,
    modelSync: String,
    modelFallback: String,
    modelAsync: String,
    modelProSync: String = "",
    modelProFallback: String = "",
    modelProAsync: String = "",
    modelActiveSync: String = "",
    modelActiveFallback: String = "",
    modelActiveAsync: String = "",
    reasoningEffortActive: String = "none",
    promptCacheKeyPrefix: String,
    enabled: Boolean,
    temperature: Double,
    maxOutputTokens: Int,
    requestTimeoutSeconds: Int
)

object OpenAiConfig:

  def fromEnv: OpenAiConfig =
    val apiKey = sys.env.getOrElse("OPENAI_API_KEY", "").trim
    val proSync = sys.env.getOrElse("OPENAI_MODEL_PRO_SYNC", "gpt-5-mini")
    val proFallback = sys.env.getOrElse("OPENAI_MODEL_PRO_FALLBACK", "gpt-5-mini")
    val proAsync = sys.env.getOrElse("OPENAI_MODEL_PRO_ASYNC", "gpt-5-mini")
    val activeReasoning =
      sys.env
        .get("OPENAI_REASONING_EFFORT_ACTIVE")
        .map(_.trim.toLowerCase)
        .filter(v => Set("none", "low", "medium", "high", "xhigh").contains(v))
        .getOrElse("none")
    val maxOutput = sys.env
      .get("OPENAI_MAX_OUTPUT_TOKENS")
      .flatMap(_.toIntOption)
      .filter(v => v >= 64 && v <= 1024)
      .getOrElse(256)

    OpenAiConfig(
      apiKey = apiKey,
      endpoint = sys.env.getOrElse("OPENAI_CHAT_COMPLETIONS_ENDPOINT", "https://api.openai.com/v1/chat/completions"),
      modelSync = sys.env.getOrElse("OPENAI_MODEL_SYNC", "gpt-5-mini"),
      modelFallback = sys.env.getOrElse("OPENAI_MODEL_FALLBACK", "gpt-5-mini"),
      modelAsync = sys.env.getOrElse("OPENAI_MODEL_ASYNC", "gpt-5-mini"),
      modelProSync = proSync,
      modelProFallback = proFallback,
      modelProAsync = proAsync,
      modelActiveSync = sys.env.getOrElse("OPENAI_MODEL_ACTIVE_SYNC", "gpt-5-mini"),
      modelActiveFallback = sys.env.getOrElse("OPENAI_MODEL_ACTIVE_FALLBACK", proFallback),
      modelActiveAsync = sys.env.getOrElse("OPENAI_MODEL_ACTIVE_ASYNC", "gpt-5-mini"),
      reasoningEffortActive = activeReasoning,
      promptCacheKeyPrefix = sys.env.getOrElse("OPENAI_PROMPT_CACHE_KEY_PREFIX", "bookmaker:polish:v2"),
      enabled = apiKey.nonEmpty,
      temperature = sys.env.getOrElse("OPENAI_TEMPERATURE", "0.2").toDoubleOption.getOrElse(0.2),
      maxOutputTokens = maxOutput,
      requestTimeoutSeconds = sys.env.getOrElse("OPENAI_TIMEOUT_SEC", "30").toIntOption.getOrElse(30)
    )
