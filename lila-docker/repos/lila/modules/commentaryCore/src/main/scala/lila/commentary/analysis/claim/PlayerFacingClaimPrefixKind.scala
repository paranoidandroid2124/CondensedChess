package lila.commentary.analysis.claim

private[commentary] enum PlayerFacingClaimPrefixKind:
  case None
  case KeyStrategicFact
  case SupportedLocal

  def render(claimText: String): String =
    this match
      case PlayerFacingClaimPrefixKind.None => claimText
      case KeyStrategicFact =>
        val stripped = claimText
          .stripPrefix("The key strategic fact here is that ")
          .stripPrefix("the key strategic fact here is that ")
        s"The key strategic fact here is that $stripped"
      case SupportedLocal =>
        val stripped = claimText
          .stripPrefix("A local reading is that ")
          .stripPrefix("a local reading is that ")
          .stripPrefix("A key idea is that ")
          .stripPrefix("a key idea is that ")
        val lowered = stripped.headOption match
          case Some(head) => s"${head.toLower}${stripped.drop(1)}"
          case _          => stripped
        s"A key idea is that $lowered"
