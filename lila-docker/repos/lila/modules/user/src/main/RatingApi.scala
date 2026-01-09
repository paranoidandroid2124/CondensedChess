package lila.rating

import lila.core.user.User
import lila.core.perf.PerfKey

final class RatingApi:
  def ratingOf(user: User, perf: PerfKey): Option[Int] = None
