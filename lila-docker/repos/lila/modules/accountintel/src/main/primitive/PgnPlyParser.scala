package lila.accountintel.primitive

import chess.Ply
import chess.format.Fen
import chess.format.pgn.{ Parser, PgnStr }

import lila.accountintel.AccountIntel.PlySnap

private[accountintel] object PgnPlyParser:

  def extract(pgn: String): Either[String, List[PlySnap]] =
    Parser.mainline(PgnStr(pgn)) match
      case Left(err) => Left(err.value)
      case Right(parsed) =>
        val game = parsed.toGame
        val replay = chess.Replay.makeReplay(game, parsed.moves)
        Right(
          replay.replay.chronoMoves.zipWithIndex.map { case (move, idx) =>
            val ply = game.ply.value + idx + 1
            val before = if idx == 0 then game.position else replay.replay.chronoMoves.take(idx).last.after
            PlySnap(
              ply = ply,
              fen = Fen.write(before, Ply(ply - 1).fullMoveNumber).value,
              san = move.toSanStr.toString,
              uci = move.toUci.uci,
              color = before.color
            )
          }
        )
