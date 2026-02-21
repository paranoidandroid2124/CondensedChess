package lila.study

import chess.format.UciPath
import lila.tree.Node.Comment
import scala.annotation.unused

final private class ExplorerGameApi()(using Executor):

  def quote(@unused gameId: GameId): Fu[Option[Comment]] = fuccess(none)

  def insert(
      @unused study: Study,
      @unused position: Position,
      @unused gameId: GameId
  ): Fu[Option[(Chapter, UciPath)]] = fuccess(none)
