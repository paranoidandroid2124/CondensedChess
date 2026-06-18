package lila.chessjudgment.analysis

object FENUtils {
  /**
   * Generates a "Null Move FEN" by swapping the side to move.
   * Also clears the en passant target square, as passing a turn
   * implies no pawn was just pushed two squares.
   * 
   * Example:
   * Input:  "rnbqkbnr/pppppppp/8/8/4P3/8/PPPP1PPP/RNBQKBNR b KQkq e3 0 1"
   * Output: "rnbqkbnr/pppppppp/8/8/4P3/8/PPPP1PPP/RNBQKBNR w KQkq - 0 1"
   * 
   * @param fen The standard FEN string
   * @return A new FEN string with the turn swapped and en passant cleared
   */
  def passTurn(fen: String): String = {
    val parts = fen.split(" ")
    if (parts.length < 4) return fen // Invalid FEN format, return as is

    // 1. Swap active color (w -> b, b -> w)
    val activeColor = parts(1)
    val newColor = if (activeColor == "w") "b" else "w"

    // 2. Keep castling rights as they are (parts(2))
    
    // 3. Clear en passant square (parts(3)) because a null move means no pawn just moved
    val newEnPassant = "-"

    // 4. Keep halfmove and fullmove counters if they exist
    val halfMove = if (parts.length > 4) s" ${parts(4)}" else ""
    val fullMove = if (parts.length > 5) s" ${parts(5)}" else ""

    s"${parts(0)} $newColor ${parts(2)} $newEnPassant$halfMove$fullMove"
  }
}
