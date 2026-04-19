package lila.commentary.witness.u

import chess.Color

import lila.commentary.witness.WitnessAnchor
import lila.commentary.witness.u.UWitnessTestSupport.*

class RestrictionDutyRulesTest extends munit.FunSuite:

  test("duty bound defender requires both binding and current duty coverage"):
    val positiveFen = "3q2k1/7p/5n2/6B1/8/3B4/8/4K3 w - - 0 1"
    val noPinFen = "2q3k1/7p/5n2/6B1/8/3B4/8/4K3 w - - 0 1"

    val witness =
      findPieceSquare(positiveFen, "duty_bound_defender", "f6", Some(Color.White)).getOrElse(fail("missing duty-bound defender"))

    assert(tokenList(witness.payload, "bound_modes").contains("pin_bound_duty"))
    assert(findPieceSquare(noPinFen, "duty_bound_defender", "f6", Some(Color.White)).isEmpty)

  test("duty bound defender rejects pinned anchors with no separate duty square"):
    val pinnedBishopOnlyFen = "4k3/4b3/8/8/8/8/8/4R1K1 w - - 0 1"
    val pinnedRookOnlyFen = "4k3/4r3/8/8/8/8/8/4R1K1 w - - 0 1"

    assert(findPieceSquare(pinnedBishopOnlyFen, "duty_bound_defender", "e7", Some(Color.White)).isEmpty)
    assert(findPieceSquare(pinnedRookOnlyFen, "duty_bound_defender", "e7", Some(Color.White)).isEmpty)

  test("short-run slider gate restriction needs partial throttling, not full entrapment"):
    val positiveFen = "6k1/8/3P4/3rB3/5P2/8/8/6K1 w - - 0 1"
    val fullTrapFen = "6k1/8/3P4/2PrB3/2KQ1P2/8/8/8 w - - 0 1"

    val witness =
      findPieceSquare(positiveFen, "short_run_slider_gate_restriction", "d5", Some(Color.White)).getOrElse(fail("missing slider restriction"))

    assertEquals(witness.anchor, WitnessAnchor.PieceSquareAnchor(chess.Square.fromKey("d5").get))
    assert(findPieceSquare(fullTrapFen, "short_run_slider_gate_restriction", "d5", Some(Color.White)).isEmpty)

  test("short-run slider gate restriction rejects self-blocked, edge-shortened, uncontrolled blocker, and remote-wall near misses"):
    val selfBlockedFen = "6k1/8/3p4/3rb3/5P2/8/8/6K1 w - - 0 1"
    val edgeShortenedFen = "6k1/8/1P6/1r6/8/4B3/8/R5K1 w - - 0 1"
    val uncontrolledBlockerFen = "6k1/8/3P4/3r1N2/8/8/7B/6K1 w - - 0 1"
    val remoteWallFen = "6k1/8/3P4/3r2N1/8/8/7B/5RK1 w - - 0 1"

    assert(findPieceSquare(selfBlockedFen, "short_run_slider_gate_restriction", "d5", Some(Color.White)).isEmpty)
    assert(findPieceSquare(edgeShortenedFen, "short_run_slider_gate_restriction", "b5", Some(Color.White)).isEmpty)
    assert(findPieceSquare(uncontrolledBlockerFen, "short_run_slider_gate_restriction", "d5", Some(Color.White)).isEmpty)
    assert(findPieceSquare(remoteWallFen, "short_run_slider_gate_restriction", "d5", Some(Color.White)).isEmpty)
