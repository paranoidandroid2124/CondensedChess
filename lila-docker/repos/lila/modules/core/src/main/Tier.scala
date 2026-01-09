package lila.core.user



enum UserTier:
  case Free, Premium, Pro

  def name = toString
  def isPremium = this == Premium || this == Pro
  def isPro = this == Pro

case class UserPlan(
    tier: UserTier,
    expiresAt: Option[java.time.Instant] = None
):
  def active = tier != UserTier.Free && expiresAt.forall(_.isAfter(java.time.Instant.now))
