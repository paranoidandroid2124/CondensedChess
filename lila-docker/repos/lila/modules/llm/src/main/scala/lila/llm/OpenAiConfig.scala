package lila.llm

case class OpenAiConfig(
    apiKey: String,
    endpoint: String,
    modelSync: String,
    modelFallback: String,
    modelAsync: String,
    promptCacheKeyPrefix: String,
    enabled: Boolean,
    temperature: Double,
    maxOutputTokens: Int,
    requestTimeoutSeconds: Int
)

object OpenAiConfig:

  def fromEnv: OpenAiConfig =
    val apiKey = sys.env.getOrElse("OPENAI_API_KEY", "").trim
    val maxOutput = sys.env
      .get("OPENAI_MAX_OUTPUT_TOKENS")
      .flatMap(_.toIntOption)
      .filter(v => v >= 64 && v <= 1024)
      .getOrElse(256)

    OpenAiConfig(
      apiKey = apiKey,
      endpoint = sys.env.getOrElse("OPENAI_CHAT_COMPLETIONS_ENDPOINT", "https://api.openai.com/v1/chat/completions"),
      modelSync = sys.env.getOrElse("OPENAI_MODEL_SYNC", "gpt-4o-mini"),
      modelFallback = sys.env.getOrElse("OPENAI_MODEL_FALLBACK", ""),
      modelAsync = sys.env.getOrElse("OPENAI_MODEL_ASYNC", "gpt-4o-mini"),
      promptCacheKeyPrefix = sys.env.getOrElse("OPENAI_PROMPT_CACHE_KEY_PREFIX", "bookmaker:polish:v2"),
      enabled = apiKey.nonEmpty,
      temperature = sys.env.getOrElse("OPENAI_TEMPERATURE", "0.2").toDoubleOption.getOrElse(0.2),
      maxOutputTokens = maxOutput,
      requestTimeoutSeconds = sys.env.getOrElse("OPENAI_TIMEOUT_SEC", "30").toIntOption.getOrElse(30)
    )
