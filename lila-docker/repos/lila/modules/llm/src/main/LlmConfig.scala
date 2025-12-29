package lila.llm

/** LLM module configuration */
case class LlmConfig(
    apiKey: String,
    model: String,
    enabled: Boolean
)

object LlmConfig:
  def fromEnv: LlmConfig =
    val apiKey = sys.env.getOrElse("GEMINI_API_KEY", "")
    LlmConfig(
      apiKey = apiKey,
      model = sys.env.getOrElse("GEMINI_MODEL", "gemini-3-flash-preview"),
      enabled = apiKey.nonEmpty
    )
