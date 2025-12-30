package lila.game

import lila.core.LightUser
import lila.core.game.{ Game, Player }

object Namer extends lila.core.game.Namer:

  def playerTextBlocking(player: Player)(using
      lightUser: LightUser.GetterSync
  ): String =
    playerTextUser(player, player.userId.flatMap(lightUser))

  def playerText(player: Player)(using lightUser: LightUser.Getter): Fu[String] =
    player.userId
      .so(lightUser)
      .dmap:
        playerTextUser(player, _)

  private def playerTextUser(player: Player, user: Option[LightUser]): String =
    player.aiLevel match
      case Some(level) => s"Stockfish level $level"
      case None =>
        user.fold(player.name.fold("Anon.")(_.value)): u =>
          u.titleName

  def gameVsTextBlocking(game: Game)(using
      lightUser: LightUser.GetterSync
  ): String =
    s"${playerTextBlocking(game.whitePlayer)} - ${playerTextBlocking(game.blackPlayer)}"

  def gameVsText(game: Game)(using lightUser: LightUser.Getter): Fu[String] =
    game.whitePlayer.userId.so(lightUser).zip(game.blackPlayer.userId.so(lightUser)).dmap { (wu, bu) =>
      s"${playerTextUser(game.whitePlayer, wu)} - ${playerTextUser(game.blackPlayer, bu)}"
    }

  def ratingString(p: Player): Option[String] = None
