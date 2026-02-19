package lila.llm

case class GeminiConfig(
    apiKey: String,
    model: String,
    enabled: Boolean,
    temperature: Double,
    maxOutputTokens: Int,
    contextCacheTtlMinutes: Int,
    requestTimeoutSeconds: Int
)

object GeminiConfig:

  def fromEnv: GeminiConfig =
    val apiKey = sys.env.getOrElse("GEMINI_API_KEY", "")
    GeminiConfig(
      apiKey = apiKey,
      model = sys.env.getOrElse("GEMINI_MODEL", "gemini-2.0-flash"),
      enabled = apiKey.nonEmpty,
      temperature = sys.env.getOrElse("GEMINI_TEMPERATURE", "0.4").toDoubleOption.getOrElse(0.4),
      maxOutputTokens = sys.env.getOrElse("GEMINI_MAX_TOKENS", "256").toIntOption.getOrElse(256),
      contextCacheTtlMinutes = sys.env.getOrElse("GEMINI_CACHE_TTL_MIN", "60").toIntOption.getOrElse(60),
      requestTimeoutSeconds = sys.env.getOrElse("GEMINI_TIMEOUT_SEC", "30").toIntOption.getOrElse(30)
    )
