package lila.llm.analysis

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
    if (parts.length < 6) return fen // Invalid FEN format, return as is

    // 1. Swap active color (w -> b, b -> w)
    val activeColor = parts(1)
    val newColor = if (activeColor == "w") "b" else "w"

    // 2. Keep castling rights as they are (parts(2))
    
    // 3. Clear en passant square (parts(3)) because a null move means no pawn just moved
    val newEnPassant = "-"

    // 4. Keep halfmove (parts(4)) and fullmove (parts(5)) counters as they are, 
    // or optionally increment halfmove. Standard Null Move practice in engines
    // often preserves them or increments halfmove, but for probing, preserving is safest.

    s"${parts(0)} $newColor ${parts(2)} $newEnPassant ${parts(4)} ${parts(5)}"
  }
}
