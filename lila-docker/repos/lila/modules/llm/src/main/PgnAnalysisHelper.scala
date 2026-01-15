package lila.llm

import chess.*
import chess.format.{ Fen, Uci }
import chess.format.pgn.{ Parser, PgnStr, San }

/**
 * PGN Analysis Helper
 * 
 * Extracts per-ply data (FEN, played move, color) from a PGN string.
 * This enables proper "played move vs best move" analysis without guessing.
 */
object PgnAnalysisHelper:

  /**
   * Data for a single ply in the game.
   */
  case class PlyData(
      ply: Int,
      fen: String,
      playedMove: String, // The SAN of the move actually played
      playedUci: String,  // The UCI of the move actually played
      color: Color        // Who played this move
  )

  /**
   * Parses a PGN and extracts per-ply data.
   * Returns a list of PlyData for each move in the game.
   */
  def extractPlyData(pgn: String): Either[String, List[PlyData]] =
    Parser.mainline(PgnStr(pgn)) match
      case Left(err) => Left(s"PGN parse error: $err")
      case Right(parsed) =>
        val game = parsed.toGame
        val result = Replay.makeReplay(game, parsed.moves)
        
        val plyDataList = result.replay.chronoMoves.zipWithIndex.map { case (moveOrDrop, idx) =>
          val plyNum = game.ply.value + idx + 1
          val positionBefore = if (idx == 0) game.position else result.replay.chronoMoves.take(idx).last.after
          val fenBefore = Fen.write(positionBefore, Ply(plyNum - 1).fullMoveNumber).value
          val san = moveOrDrop.toSanStr.toString
          val uci = moveOrDrop.toUci.uci
          // Use the actual position's side-to-move, not ply parity calculation
          val color = positionBefore.color
          
          PlyData(
            ply = plyNum,
            fen = fenBefore,
            playedMove = san,
            playedUci = uci,
            color = color
          )
        }
        
        Right(plyDataList)

  /**
   * Builds a divergence note for a specific ply by comparing:
   * - The move actually played (from PGN)
   * - The engine's best move (from evals, converted to SAN)
   * - The CP loss
   */
  def buildDivergenceNote(
      plyData: PlyData,
      evalCp: Int,
      prevEvalCp: Int,
      engineBestMoveUci: String
  ): Option[String] =
    val cpLoss = (prevEvalCp - evalCp).abs
    val isWhite = plyData.color.white
    val actualLoss = if (isWhite) prevEvalCp - evalCp else evalCp - prevEvalCp
    
    // Convert engine UCI to SAN for readability
    val engineBestMoveSan = convertUciToSan(plyData.fen, engineBestMoveUci).getOrElse(engineBestMoveUci)
    
    if (actualLoss >= 300)
      Some(s"Ply ${plyData.ply}: BLUNDER. Played **${plyData.playedMove}** instead of **$engineBestMoveSan**. Lost ~${actualLoss / 100.0} pawns.")
    else if (actualLoss >= 100)
      Some(s"Ply ${plyData.ply}: MISTAKE. Played **${plyData.playedMove}** instead of **$engineBestMoveSan**. Lost ~${actualLoss / 100.0} pawns.")
    else if (actualLoss >= 50)
      Some(s"Ply ${plyData.ply}: INACCURACY. Played **${plyData.playedMove}**, better was **$engineBestMoveSan**.")
    else
      None

  /**
   * Helper to convert UCI move string to SAN string using FEN context.
   */
  private def convertUciToSan(fen: String, uciMove: String): Option[String] =
    for
      pos <- Fen.read(chess.variant.Standard, Fen.Full(fen))
      uci <- Uci(uciMove)
      move <- uci match
        case m: Uci.Move => pos.move(m).toOption
        case d: Uci.Drop => None // Engine won't suggest drops in standard chess analysis usually
    yield move.toSanStr.toString

  /**
   * Helper to get the side-to-move correctly from FEN, not from ply parity.
   */
  def sideToMoveFromFen(fen: String): Option[Color] =
    Fen.read(chess.variant.Standard, Fen.Full(fen)).map(_.color)
