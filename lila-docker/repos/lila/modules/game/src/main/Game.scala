package lila.game

import chess.format.Uci
import chess.variant.Variant
import chess.{ ByColor, Castles, Centis, Clock, Color, Game as ChessGame, MoveOrDrop, Ply, Speed, Status }
import scala.annotation.targetName
import scalalib.model.Days

import lila.core.game.{ ClockHistory, Game, Player, Pov, Source }

enum DrawReason:
  case Stalemate, MutualAgreement, InsufficientMaterial, FiftyMoves, ThreefoldRepetition

object GameExt:

  extension (g: Game)

    def playable = g.status < Status.Aborted

    def analysable =
      g.playable && g.playedPlies > 4 &&
      !g.variant.exotic

    def fromPosition = g.variant == chess.variant.FromPosition

    @targetName("drawReasonPosition")
    def drawReason: Option[DrawReason] = g.position.status match
      case Some(Status.Stalemate) => Some(DrawReason.Stalemate)
      case Some(Status.Draw) => Some(DrawReason.MutualAgreement)
      case _ => None

    def startClock: Option[Progress] =
      g.clock.map: c =>
        val newGame = g.copy(chess = g.chess.copy(clock = Some(c.start)))
        Progress(g, newGame)

    def playerHasOfferedDrawRecently(color: Color) =
      g.metadata.drawOffers.lastBy(color).exists(_ >= g.ply - 20)

    def playerCanOfferDraw(color: Color) =
      g.started && g.playable &&
        !g.opponent(color).isAi &&
        !g.playerHasOfferedDrawRecently(color) &&
        !g.metadata.drawOffers.lastBy(color).isDefined

    @targetName("drawReasonHistory")
    def drawReasonHistory =
      if g.variant.isInsufficientMaterial(g.position) then DrawReason.InsufficientMaterial.some
      else if g.variant.fiftyMoves(g.history) then DrawReason.FiftyMoves.some
      else if g.history.threefoldRepetition then DrawReason.ThreefoldRepetition.some
      else None


  def isBoardCompatible(game: Game): Boolean =
    game.clock.map(_.config).forall: c =>
      lila.core.game.isBoardCompatible(c)

  def isBotCompatible(game: Game): Option[Boolean] =
    if !game.clock.map(_.config).forall(lila.core.game.isBotCompatible) then false.some
    else true.some
