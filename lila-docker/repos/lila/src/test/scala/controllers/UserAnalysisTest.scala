package controllers

class UserAnalysisTest extends munit.FunSuite:

  test("canonical Chess960 query positions map to canonical FEN args"):
    assertEquals(
      Chess960Analysis.canonicalArgForPosition(Some("0")),
      Some("chess960/bbqnnrkr/pppppppp/8/8/8/8/PPPPPPPP/BBQNNRKR_w_KQkq_-_0_1")
    )
    assertEquals(
      Chess960Analysis.canonicalArgForPosition(Some("959")),
      Some("chess960/rkrnnqbb/pppppppp/8/8/8/8/PPPPPPPP/RKRNNQBB_w_KQkq_-_0_1")
    )

  test("invalid Chess960 query positions are rejected"):
    assertEquals(Chess960Analysis.parsePosition(Some("-1")), None)
    assertEquals(Chess960Analysis.parsePosition(Some("960")), None)
    assertEquals(Chess960Analysis.parsePosition(Some("abc")), None)
    assertEquals(Chess960Analysis.canonicalArgForPosition(Some("960")), None)

  test("position number is recovered from canonical chess960 fen only for chess960 variant"):
    val fen = chess.variant.Chess960.positionToFen(521)
    assertEquals(Chess960Analysis.positionNumber(chess.variant.Chess960, fen), Some(521))
    assertEquals(Chess960Analysis.positionNumber(chess.variant.Standard, fen), None)
