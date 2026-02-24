package lila.llm

case class LlmProviderConfig(
    provider: String,
    promptVersion: String,
    polishGateThreshold: Double,
    premiumOnly: Boolean,
    defaultLang: String
):
  def isOpenAi: Boolean = provider == "openai"
  def isGemini: Boolean = provider == "gemini"
  def isNone: Boolean = provider == "none"

object LlmProviderConfig:

  private def boolEnv(name: String, default: Boolean): Boolean =
    sys.env
      .get(name)
      .map(_.trim.toLowerCase)
      .flatMap {
        case "1" | "true" | "yes" | "on" => Some(true)
        case "0" | "false" | "no" | "off" => Some(false)
        case _ => None
      }
      .getOrElse(default)

  private def normalizeProvider(raw: String): String =
    raw.trim.toLowerCase match
      case "openai" => "openai"
      case "gemini" => "gemini"
      case "none"   => "none"
      case _        => "none"

  def fromEnv: LlmProviderConfig =
    val threshold =
      sys.env
        .get("LLM_POLISH_GATE_THRESHOLD")
        .flatMap(_.toDoubleOption)
        .filter(v => v >= 0.0 && v <= 1.0)
        .getOrElse(0.90)

    LlmProviderConfig(
      provider = normalizeProvider(sys.env.getOrElse("LLM_PROVIDER", "openai")),
      promptVersion = sys.env.getOrElse("LLM_PROMPT_VERSION", "v1"),
      polishGateThreshold = threshold,
      premiumOnly = boolEnv("LLM_POLISH_PREMIUM_ONLY", default = true),
      defaultLang = sys.env
        .get("LLM_DEFAULT_LANG")
        .map(_.trim.toLowerCase)
        .filter(_.nonEmpty)
        .getOrElse("en")
    )
