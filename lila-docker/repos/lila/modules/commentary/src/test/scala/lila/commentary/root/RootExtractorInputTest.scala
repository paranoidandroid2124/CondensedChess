package lila.commentary.root

import chess.format.Fen

class RootExtractorInputTest extends munit.FunSuite:

  private val malformedFens = List(
    "not a fen",
    "8/8/8/8/8/8/8/8/8 w - - 0 1",
    "9/8/8/8/8/8/8/8 w - - 0 1",
    "8/8/8/8/8/8/8//8 w - - 0 1",
    "8/8/8/8/8/8/8/7Z w - - 0 1"
  )

  malformedFens.foreach: rawFen =>
    test(s"malformed fen fails closed for '$rawFen'"):
      assert(RootExtractor.fromFenFailClosed(Fen.Full.clean(rawFen)).isLeft)

  private val illegalButParseableFens = List(
    "duplicate white king" -> "4k3/8/8/8/8/8/4K3/4K3 w - - 0 1",
    "missing black king" -> "8/8/8/8/8/8/4K3/8 w - - 0 1",
    "adjacent kings" -> "8/8/8/8/8/8/4k3/4K3 w - - 0 1",
    "side that just moved remains in check" -> "4k3/8/8/8/8/8/4r3/4K3 b - - 0 1",
    "white pawn on first rank" -> "4k3/8/8/8/8/8/8/P3K3 w - - 0 1",
    "black pawn on eighth rank" -> "4k2p/8/8/8/8/8/8/4K3 w - - 0 1",
    "en passant rank contradicts side to move" -> "4k3/8/8/1p6/8/8/8/4K3 w - b3 0 1",
    "en passant history contradicts board" -> "4k3/8/8/8/8/8/8/4K3 w - b6 0 1",
    "duplicate castling right" -> "4k3/8/8/8/8/8/8/4K2R w KK - 0 1",
    "castling right without rook" -> "4k3/8/8/8/8/8/8/4K3 w K - 0 1",
    "castling right without king on home square" -> "4k3/8/8/8/8/8/8/R5KR w Q - 0 1"
  )

  illegalButParseableFens.foreach: (label, rawFen) =>
    test(s"strict input boundary fails closed for $label"):
      assert(RootExtractor.fromFenFailClosed(Fen.Full.clean(rawFen)).isLeft)

  test("strict input boundary accepts a legal check position"):
    assert(RootExtractor.fromFenFailClosed(Fen.Full.clean("4k3/8/8/8/8/8/4r3/4K3 w - - 0 1")).isRight)

  test("strict input boundary accepts a legal en passant field even when no capture is available"):
    assert(RootExtractor.fromFenFailClosed(Fen.Full.clean("4k3/8/8/1p6/8/8/8/4K3 w - b6 0 1")).isRight)
