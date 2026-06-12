package lila.commentary.analysis

import munit.FunSuite
import lila.commentary.model.Motif
import lila.commentary.model.authoring.EvidenceBranch
import lila.commentary.model.strategic.{ PvMove, VariationLine }

class LineScopedCitationTest extends FunSuite:

  test("SAN citation derives from raw engine PV before stale parsed metadata") {
    val line =
      VariationLine(
        moves = List("e2e4", "e7e5", "g1f3"),
        scoreCp = 24,
        parsedMoves = List(
          PvMove("d2d4", "d4", "d2", "d4", "P", false, None, false),
          PvMove("d7d5", "...d5", "d7", "d5", "p", false, None, false),
          PvMove("c2c4", "c4", "c2", "c4", "P", false, None, false)
        )
      )

    assertEquals(
      LineScopedCitation.sanMoves("rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1", line),
      List("e4", "e5", "Nf3")
    )
  }

  test("tactical citation reaches the motif-trigger SAN and keeps at least three plies") {
    val motifs =
      List(
        Motif.Pin(
          pinningPiece = _root_.chess.Bishop,
          pinnedPiece = _root_.chess.Knight,
          targetBehind = _root_.chess.Queen,
          color = _root_.chess.White,
          plyIndex = 2,
          move = Some("g5d8"),
          pinningSq = Some(_root_.chess.Square.D8),
          pinnedSq = Some(_root_.chess.Square.F6),
          behindSq = Some(_root_.chess.Square.D8)
        )
      )

    val citation =
      LineScopedCitation.tacticalCitationFromSanMoves(
        startPly = 11,
        sans = List("Bg5", "Nc6", "Qd8"),
        motifs = motifs
      )

    assertEquals(citation, Some("6. Bg5 Nc6 7. Qd8"))
  }

  test("strategic citation requires at least three SAN tokens") {
    assertEquals(
      LineScopedCitation.strategicCitationFromSanMoves(11, List("Bf5", "Nc3", "Qa5")),
      Some("6. Bf5 Nc3 7. Qa5")
    )
    assertEquals(LineScopedCitation.strategicCitationFromSanMoves(11, List("Bf5", "Nc3")), None)
  }

  test("first concrete SAN token accepts sentence-final move punctuation") {
    assertEquals(
      LineScopedCitation.firstConcreteSanToken("Short line: Nb4."),
      Some("Nb4")
    )
    assertEquals(
      LineScopedCitation.firstConcreteSanToken("12... Nb4."),
      Some("Nb4")
    )
  }

  test("evidence branch signature keeps same-head branches distinct by SAN prefix") {
    val first =
      EvidenceBranch(
        keyMove = "12...Bf5",
        line = "12...Bf5 13.Nc3 Qa5 14.Bd2",
        evalCp = Some(40)
      )
    val second =
      EvidenceBranch(
        keyMove = "12...Bf5",
        line = "12...Bf5 13.Nc3 Rc8 14.Bd2",
        evalCp = Some(18)
      )

    val display = LineScopedCitation.evidenceBranchDisplayLine(first).getOrElse(fail("missing evidence display line"))
    assert(display.contains("12... Bf5"), clue(display))
    assert(display.contains("Qa5"), clue(display))
    assertNotEquals(
      LineScopedCitation.evidenceBranchSignature(first),
      LineScopedCitation.evidenceBranchSignature(second)
    )
  }

  test("citation helpers distinguish inline SAN citations from source-label-only prose") {
    assert(LineScopedCitation.hasInlineCitation("After 12...Bf5 13.Nc3 Qa5, Black keeps the cleaner continuation."))
    assert(!LineScopedCitation.hasSourceLabelOnly("After 12...Bf5 13.Nc3 Qa5, Black keeps the cleaner continuation."))
    assert(LineScopedCitation.hasSourceLabelOnly("The engine line keeps the cleaner continuation."))
    assert(!LineScopedCitation.hasInlineCitation("The engine line keeps the cleaner continuation."))
  }
