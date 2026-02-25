package lila.llm

case class LlmConfig(
    structKbEnabled: Boolean,
    structKbShadowMode: Boolean,
    structKbMinConfidence: Double,
    endgameOracleEnabled: Boolean,
    endgameOracleShadowMode: Boolean,
    strategicPriorEnabled: Boolean,
    strategicPriorShadowMode: Boolean,
    strategicPriorCanaryRate: Double,
    strategicPriorWeight: Double
):
  def shouldEvaluateStructureKb: Boolean = structKbEnabled || structKbShadowMode
  def shouldEvaluateEndgameOracle: Boolean = endgameOracleEnabled || endgameOracleShadowMode
  def shouldApplyStrategicPrior(sampleKey: String): Boolean =
    if strategicPriorEnabled then true
    else if strategicPriorCanaryRate > 0.0 then stableBucket(sampleKey) < strategicPriorCanaryRate
    else false

  private def stableBucket(key: String): Double =
    val normalized = key.hashCode & Int.MaxValue
    normalized.toDouble / Int.MaxValue.toDouble

object LlmConfig:

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

  private def doubleEnv(name: String, default: Double, min: Double, max: Double): Double =
    sys.env
      .get(name)
      .flatMap(_.toDoubleOption)
      .map(v => v.max(min).min(max))
      .getOrElse(default)

  def fromEnv: LlmConfig =
    LlmConfig(
      structKbEnabled = boolEnv("LLM_STRUCT_KB_ENABLED", default = false),
      structKbShadowMode = boolEnv("LLM_STRUCT_KB_SHADOW_MODE", default = true),
      structKbMinConfidence = sys.env
        .get("LLM_STRUCT_KB_MIN_CONFIDENCE")
        .flatMap(_.toDoubleOption)
        .filter(v => v > 0.0 && v <= 1.0)
        .getOrElse(0.72),
      endgameOracleEnabled = boolEnv("LLM_BOOKMAKER_ENDGAME_ORACLE_ENABLED", default = false),
      endgameOracleShadowMode = boolEnv("LLM_BOOKMAKER_ENDGAME_ORACLE_SHADOW", default = true),
      strategicPriorEnabled = boolEnv("LLM_STRATEGIC_PRIOR_ENABLED", default = false),
      strategicPriorShadowMode = boolEnv("LLM_STRATEGIC_PRIOR_SHADOW_MODE", default = true),
      strategicPriorCanaryRate = doubleEnv("LLM_STRATEGIC_PRIOR_CANARY_RATE", default = 0.0, min = 0.0, max = 1.0),
      strategicPriorWeight = doubleEnv("LLM_STRATEGIC_PRIOR_WEIGHT", default = 0.35, min = 0.0, max = 0.8)
    )
