package lila.llm.analysis

import _root_.chess.Black
import _root_.chess.format.Fen
import _root_.chess.variant.Standard
import munit.FunSuite
import lila.llm.model.{ Fact, FactScope, Motif }

class FactExtractorTest extends FunSuite:

  private def boardFromFen(fullFen: String) =
    Fen.read(Standard, Fen.Full(fullFen)).getOrElse(fail(s"invalid fen: $fullFen")).board

  test("single-attacked undefended pawn is downgraded from hanging to target") {
    val board = boardFromFen("4k3/8/8/4p3/4Q3/8/8/4K3 b - - 0 1")
    val facts = FactExtractor.extractStaticFacts(board, Black)

    assert(!facts.exists {
      case Fact.HangingPiece(square, _, _, _, _) => square == _root_.chess.Square.E5
      case _                                     => false
    }, clue(facts))
    assert(facts.exists {
      case Fact.TargetPiece(square, _, _, _, _) => square == _root_.chess.Square.E5
      case _                                    => false
    }, clue(facts))
  }

  test("undefended minor piece under attack can still surface as hanging") {
    val board = boardFromFen("4k3/8/8/8/4Q3/5b2/8/4K3 b - - 0 1")
    val facts = FactExtractor.extractStaticFacts(board, Black)

    assert(facts.exists {
      case Fact.HangingPiece(square, _, _, _, _) => square == _root_.chess.Square.F3
      case _                                     => false
    }, clue(facts))
  }

  test("future-only pin motif does not become a current-board fact") {
    val board = boardFromFen("rnbqkbnr/ppp1pppp/8/3p4/3P4/8/PPP1PPPP/RNBQKBNR w KQkq - 0 2")
    val futurePin =
      Motif.Pin(
        pinningPiece = _root_.chess.Bishop,
        pinnedPiece = _root_.chess.Pawn,
        targetBehind = _root_.chess.Rook,
        color = _root_.chess.White,
        plyIndex = 2,
        move = Some("c1f4"),
        pinningSq = Some(_root_.chess.Square.F4),
        pinnedSq = Some(_root_.chess.Square.C7),
        behindSq = Some(_root_.chess.Square.C8)
      )

    val nowFacts = FactExtractor.fromMotifs(board, List(futurePin), FactScope.Now)
    val mainPvFacts = FactExtractor.fromMotifs(board, List(futurePin), FactScope.MainPv)

    assertEquals(nowFacts, Nil)
    assert(mainPvFacts.exists(_.isInstanceOf[Fact.Pin]), clue(mainPvFacts))
  }
