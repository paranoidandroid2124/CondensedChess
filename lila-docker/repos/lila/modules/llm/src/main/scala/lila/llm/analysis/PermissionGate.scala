package lila.llm.analysis

import lila.core.user.{ User, UserTier }
import lila.core.lilaism.Core.*

object PermissionGate:

  case class AccessDenied(message: String)

  def checkDepth(user: Option[User], requestedDepth: Int): Either[AccessDenied, Unit] =
    val tier = user.map(_.tier) | UserTier.Free
    val (maxDepth, tierName) = tier match
      case UserTier.Free    => (18, "Free")
      case UserTier.Premium => (24, "Premium")
      case UserTier.Pro     => (99, "Pro")

    if requestedDepth > maxDepth then
      Left(AccessDenied(s"Requested depth $requestedDepth exceeds $tierName limit of $maxDepth. Upgrade for deeper analysis."))
    else
      Right(())

  def canRequestNarrative(user: Option[User], dailyCount: Int): Either[AccessDenied, Unit] =
    val tier = user.map(_.tier) | UserTier.Free
    if tier == UserTier.Free && dailyCount >= 3 then
      Left(AccessDenied("Daily limit of 3 narrative summaries reached for Free users. Upgrade to Premium for unlimited summaries."))
    else
      Right(())
