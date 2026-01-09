package controllers

import akka.stream.scaladsl.*
import akka.util.ByteString
import chess.format.{ Fen, Uci }
import chess.variant.Variant
import play.api.mvc.Result

import lila.app.*
import lila.core.id.PuzzleId
import lila.pref.{ PieceSet, Theme }

final class Export(env: Env) extends LilaController(env):

  def puzzleThumbnail(id: PuzzleId, theme: Option[String], piece: Option[String]) = Anon:
    NotImplemented("Puzzle module removed")
