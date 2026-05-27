package lila.commentary.analysis.claim

private[commentary] enum PlayerFacingClaimPrefixKind:
  case None
  case KeyStrategicFact
  case SupportedLocal

  def render(claimText: String): String =
    this match
      case PlayerFacingClaimPrefixKind.None => claimText.trim
      case KeyStrategicFact                 => join("The key strategic fact here is that", claimText)
      case SupportedLocal                   => join("A key idea is that", claimText)

  private def join(prefix: String, claimText: String): String =
    val trimmed = Option(claimText).getOrElse("").trim
    if startsWithPrefix(trimmed, prefix) then trimmed
    else s"$prefix $trimmed".trim

  private def startsWithPrefix(text: String, prefix: String): Boolean =
    text.toLowerCase.startsWith(prefix.toLowerCase)
