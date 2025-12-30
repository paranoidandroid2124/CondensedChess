package lila.game

import lila.core.user.LightUserApi
import lila.core.game.Game
import scalalib.paginator.Paginator

final class UserGameApi(
    lightUser: LightUserApi
)(using Executor):

  def apply(
      u: User,
      me: Option[User],
      filter: String,
      page: Int
  ): Fu[Paginator[Game]] =
    fuccess(Paginator.empty[Game])
