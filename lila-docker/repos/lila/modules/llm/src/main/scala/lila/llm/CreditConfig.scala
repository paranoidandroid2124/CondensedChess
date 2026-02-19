package lila.llm

/** Credit system configuration: tier definitions and cost constants. */
object CreditConfig:
  val PerPlyAnalysis: Int  = 1  // bookmakerPosition call
  val FullGameNarrative: Int = 3  // analyzeGameLocal call
  enum Tier(val maxCredits: Int, val key: String):
    case Free extends Tier(150, "free")     // ~5 full games (30 ply avg × 5)
    case Pro  extends Tier(2000, "pro")     // ~70 full games

  object Tier:
    def fromString(s: String): Tier = s.toLowerCase match
      case "pro" => Pro
      case _     => Free

  /** Days between credit resets. */
  val ResetIntervalDays: Int = 30

  /** Default tier for new users. */
  val DefaultTier: Tier = Tier.Free
