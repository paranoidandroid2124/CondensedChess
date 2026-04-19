package lila.commentary.witness.u

import chess.Color

import lila.commentary.witness.WitnessAnchor
import lila.commentary.witness.u.UWitnessTestSupport.*

class PieceConfigurationRulesTest extends munit.FunSuite:

  test("weak outpost square prioritizes outpost over residual weak-square emission"):
    val outpostFen = "4k3/8/8/8/5N2/2P5/8/4K3 w - - 0 1"
    val weakFen = "4k3/8/8/8/5N2/8/8/4K3 w - - 0 1"

    val outpostWitness =
      findSquare(outpostFen, "weak_outpost_square_state", "d5", Some(Color.White), Some("outpost_square_state")).getOrElse(fail("missing outpost witness"))
    val weakWitness =
      findSquare(weakFen, "weak_outpost_square_state", "d5", Some(Color.White), Some("weak_square_state")).getOrElse(fail("missing weak-square witness"))

    assertEquals(token(outpostWitness.payload, "state"), Some("outpost"))
    assertEquals(token(weakWitness.payload, "state"), Some("weak"))
    assert(findSquare(outpostFen, "weak_outpost_square_state", "d5", Some(Color.White), Some("weak_square_state")).isEmpty)

  test("bishop pair is owner-side board configuration and allows promoted extras"):
    val pairFen = "4k3/8/8/8/8/8/6B1/2B1K3 w - - 0 1"
    val extraFen = "B3k3/8/8/8/8/8/6B1/2B1K3 w - - 0 1"
    val singleFen = "4k3/8/8/8/8/8/6B1/4K3 w - - 0 1"

    val pairWitnesses = descriptor(pairFen, "bishop_pair_state").filter(_.color.contains(Color.White))
    val extraWitnesses = descriptor(extraFen, "bishop_pair_state").filter(_.color.contains(Color.White))
    val pairWitness = pairWitnesses.headOption.getOrElse(fail("missing bishop-pair witness"))
    val extraWitness = extraWitnesses.headOption.getOrElse(fail("missing promoted-extra bishop-pair witness"))

    assertEquals(pairWitnesses.size, 1)
    assertEquals(pairWitness.anchor, WitnessAnchor.BoardAnchor)
    assertEquals(squareList(pairWitness.payload, "bishop_member_squares").map(_.key), Vector("c1", "g2"))
    assertEquals(extraWitnesses.size, 1)
    assertEquals(extraWitness.anchor, WitnessAnchor.BoardAnchor)
    assertEquals(squareList(extraWitness.payload, "bishop_member_squares").map(_.key), Vector("c1", "g2", "a8"))
    assert(descriptor(singleFen, "bishop_pair_state").forall(witness => !witness.color.contains(Color.White)))

  test("knight on outpost square requires an actual outpost root"):
    val positiveFen = "4k3/8/8/3N4/8/2P5/8/4K3 w - - 0 1"
    val negativeFen = "4k3/8/8/3N4/8/8/8/4K3 w - - 0 1"

    assert(findPieceSquare(positiveFen, "knight_on_outpost_square", "d5", Some(Color.White)).nonEmpty)
    assert(findPieceSquare(negativeFen, "knight_on_outpost_square", "d5", Some(Color.White)).isEmpty)

  test("loose piece target stays separate from defended exchange parity"):
    val positiveFen = "4k3/8/8/3r4/5N2/8/8/4K3 w - - 0 1"
    val negativeFen = "3rk3/8/8/3r4/8/8/8/3RK3 w - - 0 1"

    assert(findPieceSquare(positiveFen, "loose_piece_target_state", "d5", Some(Color.White)).nonEmpty)
    assert(findPieceSquare(negativeFen, "loose_piece_target_state", "d5", Some(Color.White)).isEmpty)
