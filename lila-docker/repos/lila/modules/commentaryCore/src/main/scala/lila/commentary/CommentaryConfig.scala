package lila.commentary

case class CommentaryConfig(
    structKbEnabled: Boolean,
    structKbShadowMode: Boolean,
    structKbMinConfidence: Double,
    endgameOracleEnabled: Boolean,
    endgameOracleShadowMode: Boolean
):
  def shouldEvaluateStructureKb: Boolean = structKbEnabled || structKbShadowMode
  def shouldEvaluateEndgameOracle: Boolean = true

object CommentaryConfig:

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

  def fromEnv: CommentaryConfig =
    CommentaryConfig(
      structKbEnabled = boolEnv("AI_STRUCT_KB_ENABLED", default = false),
      structKbShadowMode = boolEnv("AI_STRUCT_KB_SHADOW_MODE", default = true),
      structKbMinConfidence = sys.env
        .get("AI_STRUCT_KB_MIN_CONFIDENCE")
        .flatMap(_.toDoubleOption)
        .filter(v => v > 0.0 && v <= 1.0)
        .getOrElse(0.72),
      endgameOracleEnabled = true,
      endgameOracleShadowMode = false
    )
