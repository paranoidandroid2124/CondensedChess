package lila.game

import chess.Position
import chess.format.pgn.SanStr
import chess.format.{ BoardFen, Fen }

import lila.core.captcha.{ Captcha, CaptchaApi as ICaptchaApi, Solutions, WithCaptcha }
import lila.core.game.Game

final private class CaptchaApi()(using Executor) extends ICaptchaApi:

  def any: Captcha = Impl.default

  def get(id: GameId): Fu[Captcha] = fuccess(Impl.default)

  def validate(gameId: GameId, move: String): Fu[Boolean] =
    fuccess(Impl.default.solutions.toList contains move)

  def validateSync(data: WithCaptcha): Boolean =
    Impl.default.solutions.toList contains data.move

  def newCaptcha() = ()

  private object Impl:

    val default = Captcha(
      gameId = GameId("00000000"),
      fen = BoardFen("1k3b1r/r5pp/pNQppq2/2p5/4P3/P3B3/1P3PPP/n4RK1"),
      color = chess.White,
      solutions = NonEmptyList.one("c6 c8"),
      moves = Map("c6" -> "c8")
    )
