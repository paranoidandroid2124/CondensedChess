package lila.game

import chess.format.pgn.{ InitialComments, Pgn, PgnTree, SanStr, Tag, Tags }
import chess.format.{ Fen, pgn as chessPgn }
import chess.{ Centis, Color, Outcome, Ply, Tree }

import lila.core.LightUser
import lila.core.LightUser
import lila.core.game.PgnDump.WithFlags
import lila.core.game.{ Game, Player }

final class PgnDump(
    lightUserApi: lila.core.user.LightUserApi
)(using Executor)
    extends lila.core.game.PgnDump:

  import PgnDump.*

  def apply(
      game: Game,
      initialFen: Option[Fen.Full],
      flags: WithFlags,
      teams: Option[chess.ByColor[lila.core.id.TeamId]] = None
  ): Fu[Pgn] =
    val tree = flags.moves.so:
      makeTree(
        applyDelay(game.sans, flags.keepDelayIf(game.playable)),
        flags.clocks.so(~game.bothClockStates),
        game.startColor
      )
    tags(game, initialFen, None, flags.opening, teams).map: ts =>
      Pgn(ts, InitialComments.empty, tree, Ply.initial.next)

  private def gameUrl(id: GameId) = s"/$id"

  def player(p: Player, u: Option[LightUser]): String =
    p.aiLevel.fold(
      u.fold(p.name.map(_.value).getOrElse("Anonymous"))(_.name.value)
    )("AI level " + _)

  def tags(
      game: Game,
      initialFen: Option[Fen.Full],
      importedTags: Option[Tags],
      withOpening: Boolean,
      teams: Option[chess.ByColor[lila.core.id.TeamId]] = None
  ): Fu[Tags] =
    val variantName = game.variant.name.capitalize
    val tagList = List[Option[Tag]](
      Tag(_.Event, "Casual Game").some,
      Tag(_.Site, gameUrl(game.id)).some,
      Tag(_.Date, Tag.UTCDate.format.print(game.createdAt)).some,
      Tag(_.Round, "-").some,
      Tag(_.White, player(game.whitePlayer, None)).some,
      Tag(_.Black, player(game.blackPlayer, None)).some,
      Tag(_.Result, result(game)).some,
      Tag(_.UTCDate, Tag.UTCDate.format.print(game.createdAt)).some,
      Tag(_.UTCTime, Tag.UTCTime.format.print(game.createdAt)).some,
      Tag(_.Variant, variantName).some
    ).flatten
    fuccess(Tags(tagList))

object PgnDump:

  export lila.core.game.PgnDump.*

  private[game] def makeTree(
      moves: Seq[SanStr],
      clocks: Vector[Centis],
      startColor: Color
  ): Option[PgnTree] =
    val clockOffset = startColor.fold(0, 1)
    def f(san: SanStr, index: Int) = chessPgn.Move(
      san = san,
      timeLeft = clocks.lift(index - clockOffset).map(_.roundSeconds)
    )
    Tree.buildWithIndex(moves, f)

  def applyDelay[M](moves: Seq[M], flags: WithFlags): Seq[M] = moves

  def result(game: Game) =
    Outcome.showResult(game.finished.option(Outcome(game.winnerColor)))
