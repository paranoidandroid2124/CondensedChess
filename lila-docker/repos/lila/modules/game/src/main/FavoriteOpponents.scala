package lila.game

import lila.core.user.User
import lila.core.userId.UserId

final class FavoriteOpponents()(using Executor):

  def apply(userId: UserId): Fu[List[(User, Int)]] = fuccess(Nil)
