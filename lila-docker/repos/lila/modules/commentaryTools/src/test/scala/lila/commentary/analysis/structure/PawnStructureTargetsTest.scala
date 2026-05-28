package lila.commentary.analysis.structure

import chess.format.Fen
import chess.variant.Standard
import munit.FunSuite

class PawnStructureTargetsTest extends FunSuite:

  private def position(fen: String): _root_.chess.Position =
    Fen.read(Standard, Fen.Full(fen)).getOrElse(fail(s"invalid FEN: $fen"))

  test("Carlsbad target accepts minority pawns on home, advanced, and contact squares") {
    val whiteB2 = position("6k1/8/2p5/3p4/3P4/8/1P6/6K1 w - - 0 1")
    val whiteB4 = position("6k1/8/2p5/3p4/1P1P4/8/8/6K1 w - - 0 1")
    val whiteB5 = position("6k1/8/2p5/1P1p4/3P4/8/8/6K1 w - - 0 1")
    val blackB7 = position("6k1/1p6/8/3p4/3P4/2P5/8/6K1 b - - 0 1")
    val blackB5 = position("6k1/8/8/1p1p4/3P4/2P5/8/6K1 b - - 0 1")
    val blackB4 = position("6k1/8/8/3p4/1p1P4/2P5/8/6K1 b - - 0 1")

    assertEquals(PawnStructureTargets.carlsbadTargetForBoard(whiteB2.board, whiteB2.color).map(_.targetSquare), Some("c6"))
    assertEquals(PawnStructureTargets.carlsbadTargetForBoard(whiteB4.board, whiteB4.color).map(_.targetSquare), Some("c6"))
    assertEquals(PawnStructureTargets.carlsbadTargetForBoard(whiteB5.board, whiteB5.color).map(_.targetSquare), Some("c6"))
    assertEquals(PawnStructureTargets.carlsbadTargetForBoard(blackB7.board, blackB7.color).map(_.targetSquare), Some("c3"))
    assertEquals(PawnStructureTargets.carlsbadTargetForBoard(blackB5.board, blackB5.color).map(_.targetSquare), Some("c3"))
    assertEquals(PawnStructureTargets.carlsbadTargetForBoard(blackB4.board, blackB4.color).map(_.targetSquare), Some("c3"))
  }

  test("fixed pawn targets cover Benoni and reversed Benoni without opening-specific names") {
    val benoni = position("4k3/8/3p4/2pP4/8/8/8/4K3 w - - 0 1")
    val reversedBenoni = position("4k3/8/8/8/2pp4/3P4/8/4K3 b - - 0 1")

    assert(PawnStructureTargets.fixedPawnTarget(benoni.board, benoni.color, "d6"))
    assert(PawnStructureTargets.weakPawnTargetsForPressure(benoni.board, benoni.color).contains("d6"))
    assert(PawnStructureTargets.fixedPawnTarget(reversedBenoni.board, reversedBenoni.color, "d3"))
    assert(PawnStructureTargets.weakPawnTargetsForPressure(reversedBenoni.board, reversedBenoni.color).contains("d3"))
  }
