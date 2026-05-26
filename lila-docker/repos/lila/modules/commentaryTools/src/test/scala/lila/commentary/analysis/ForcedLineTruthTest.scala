package lila.commentary.analysis

import munit.FunSuite

class ForcedLineTruthTest extends FunSuite:

  test("win-material sequence requires material gain for the final mover") {
    val fen = "4k3/8/8/8/8/8/r3Q3/4K3 w - - 0 1"

    assert(
      ForcedLineTruth.verifySequence(
        fen,
        List("e1f1", "a2e2"),
        ForcedLineTruth.ExpectedResult.WinMaterial
      )
    )
  }

  test("legal non-material line is not enough for win-material truth") {
    val fen = "4k3/8/8/8/8/8/4P3/4K3 w - - 0 1"

    assert(
      !ForcedLineTruth.verifySequence(
        fen,
        List("e2e3", "e8e7"),
        ForcedLineTruth.ExpectedResult.WinMaterial
      )
    )
  }
