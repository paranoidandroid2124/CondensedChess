package lila.game

import lila.core.game.Game
import lila.db.dsl.{ *, given }

final class CrosstableApi()(using Executor):

  import Crosstable.Result
  import Crosstable.BSONFields as F

  def apply(game: Game): Fu[Option[Crosstable]] = fuccess(None)

  def withMatchup(game: Game): Fu[Option[Crosstable.WithMatchup]] = fuccess(None)

  def apply(u1: UserId, u2: UserId): Fu[Crosstable] =
    fuccess(Crosstable.empty(u1, u2))

  def justFetch(u1: UserId, u2: UserId): Fu[Option[Crosstable]] =
    fuccess(None)

  def withMatchup(u1: UserId, u2: UserId): Fu[Crosstable.WithMatchup] = 
    fuccess(Crosstable.WithMatchup(Crosstable.empty(u1, u2), None))

  def nbGames(u1: UserId, u2: UserId): Fu[Int] = fuccess(0)

  def add(game: Game): Funit = funit

  private def select(u1: UserId, u2: UserId) =
    $id(Crosstable.makeKey(u1, u2))
