package lila.llm.analysis

class FENUtilsTest extends munit.FunSuite {

  test("passTurn should flip White to Black and clear en passant") {
    // 1. e4 (White just played e4, Black to move, en passant on e3)
    val fen = "rnbqkbnr/pppppppp/8/8/4P3/8/PPPP1PPP/RNBQKBNR b KQkq e3 0 1"
    val expected = "rnbqkbnr/pppppppp/8/8/4P3/8/PPPP1PPP/RNBQKBNR w KQkq - 0 1"
    
    assertEquals(FENUtils.passTurn(fen), expected)
  }

  test("passTurn should flip Black to White and keep castling rights") {
    // 1. e4 e5 2. Nf3 Nc6 (Black to move... wait FEN says w to move)
    val fen = "r1bqkbnr/pppp1ppp/2n5/4p3/4P3/5N2/PPPP1PPP/RNBQKB1R w KQkq - 2 3"
    val expected = "r1bqkbnr/pppp1ppp/2n5/4p3/4P3/5N2/PPPP1PPP/RNBQKB1R b KQkq - 2 3"
    
    assertEquals(FENUtils.passTurn(fen), expected)
  }

  test("passTurn should handle FENs with existing no en-passant") {
    val fen = "r1bq1rk1/pppp1ppp/2n2n2/2b1p3/2B1P3/2N2N2/PPPP1PPP/R1BQ1RK1 b - - 5 6"
    val expected = "r1bq1rk1/pppp1ppp/2n2n2/2b1p3/2B1P3/2N2N2/PPPP1PPP/R1BQ1RK1 w - - 5 6"
    
    assertEquals(FENUtils.passTurn(fen), expected)
  }

  test("passTurn should return original if FEN is badly formatted") {
    val badFen = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq" // missing fields
    assertEquals(FENUtils.passTurn(badFen), badFen)
  }
}
