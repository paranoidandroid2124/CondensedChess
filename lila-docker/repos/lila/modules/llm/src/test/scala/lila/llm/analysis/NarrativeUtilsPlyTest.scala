package lila.llm.analysis

import munit.FunSuite

class NarrativeUtilsPlyTest extends FunSuite:

  test("plyFromFen parses side-to-move and fullmove correctly"):
    assertEquals(NarrativeUtils.plyFromFen("rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1"), Some(0))
    assertEquals(NarrativeUtils.plyFromFen("rnbqkbnr/pppppppp/8/8/4P3/8/PPPP1PPP/RNBQKBNR b KQkq - 0 1"), Some(1))
    assertEquals(NarrativeUtils.plyFromFen("rnbqkbnr/pppp1ppp/4p3/8/2P5/8/PP1PPPPP/RNBQKBNR w KQkq - 0 2"), Some(2))

  test("resolveAnnotationPly uses fen as source of truth and adds played move offset"):
    val fenBeforeBlackFirstMove = "rnbqkbnr/pppppppp/8/8/2P5/8/PP1PPPPP/RNBQKBNR b KQkq - 0 1"
    assertEquals(
      NarrativeUtils.resolveAnnotationPly(fenBeforeBlackFirstMove, Some("e7e6"), fallbackPly = 99),
      2
    )
    assertEquals(
      NarrativeUtils.resolveAnnotationPly(fenBeforeBlackFirstMove, None, fallbackPly = 99),
      1
    )

  test("resolveAnnotationPly falls back when fen is malformed"):
    assertEquals(NarrativeUtils.resolveAnnotationPly("invalid fen", Some("e2e4"), fallbackPly = 42), 42)
