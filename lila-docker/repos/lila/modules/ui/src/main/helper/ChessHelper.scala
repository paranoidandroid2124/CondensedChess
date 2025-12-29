package lila.ui

object ChessHelper:
  def underscoreFen(fen: chess.format.Fen.Full): String = 
    fen.value.replace(" ", "_")
