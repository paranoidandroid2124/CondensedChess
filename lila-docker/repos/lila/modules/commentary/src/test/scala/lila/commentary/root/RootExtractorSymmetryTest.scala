package lila.commentary.root

import chess.Square
import chess.format.Fen

class RootExtractorSymmetryTest extends munit.FunSuite:

  private val fens = RootExpectationCorpus.loadAll().map(_.fen).distinct

  fens.foreach: fen =>
    test(s"mirror symmetry holds for $fen"):
      val originalFen = Fen.Full.clean(fen)
      val mirroredFen = mirrorColorSwapFen(originalFen)

      val original = RootExtractor.fromFen(originalFen).fold(message => fail(message), identity)
      val mirrored = RootExtractor.fromFen(mirroredFen).fold(message => fail(message), identity)

      assertEquals(mirrored, original.mirrorColorSwap)

  private def mirrorColorSwapFen(fen: Fen.Full): Fen.Full =
    val parts = fen.value.split(" ")
    val mirroredBoard = parts(0).split('/').reverse.map(_.map(swapPieceColor)).mkString("/")
    val mirroredTurn = if parts(1) == "w" then "b" else "w"
    val mirroredCastling =
      RootAtomRegistry
        .castlingRightsMask(parts(2))
        .map(mask => RootAtomRegistry.castlingRightsState(RootAtomRegistry.mirrorColorSwapCastlingMask(mask)))
        .getOrElse("-")
    val mirroredEnPassant =
      if parts(3) == "-" then "-"
      else
        Square
          .fromKey(parts(3))
          .map(RootAtomRegistry.mirrorColorSwapSquare)
          .map(_.key)
          .getOrElse("-")
    Fen.Full.clean(
      List(
        mirroredBoard,
        mirroredTurn,
        mirroredCastling,
        mirroredEnPassant
      ) ++ parts.drop(4) mkString " "
    )

  private def swapPieceColor(ch: Char): Char =
    if ch.isUpper then ch.toLower
    else if ch.isLower then ch.toUpper
    else ch
