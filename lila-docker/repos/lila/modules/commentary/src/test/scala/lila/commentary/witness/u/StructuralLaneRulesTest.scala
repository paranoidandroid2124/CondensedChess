package lila.commentary.witness.u

import chess.{ Color, File, Square }

import lila.commentary.witness.WitnessDirection
import lila.commentary.witness.u.UWitnessTestSupport.*

class StructuralLaneRulesTest extends munit.FunSuite:

  test("file lane state emits open and semi-open variants with neutral polarity"):
    val openFen = "6k1/pppp1ppp/8/8/8/8/PPPP1PPP/6K1 w - - 0 1"
    val semiOpenFen = "6k1/pppppppp/8/8/8/8/PPPP1PPP/6K1 w - - 0 1"

    val openWitness = findFile(openFen, "file_lane_state", File.E, Some("open_file_state")).getOrElse(fail("missing open-file witness"))
    val semiOpenWitness =
      findFile(semiOpenFen, "file_lane_state", File.E, Some("semi_open_file_state")).getOrElse(fail("missing semi-open-file witness"))

    assertEquals(openWitness.color, None)
    assertEquals(token(openWitness.payload, "state"), Some("open"))
    assertEquals(token(semiOpenWitness.payload, "state"), Some("semi_open"))
    assertEquals(colorValue(semiOpenWitness.payload, "open_for_color"), Some(Color.White))

  test("rook on open file requires an open file and does not admit semi-open substitution"):
    val openFen = "6k1/pppp1ppp/8/8/8/8/PPPPRPPP/6K1 w - - 0 1"
    val semiOpenFen = "6k1/pppppppp/8/8/8/8/PPPPRPPP/6K1 w - - 0 1"

    val openWitness =
      findPieceSquare(openFen, "rook_on_open_file_state", "e2", Some(Color.White)).getOrElse(fail("missing rook-on-open-file witness"))

    assertEquals(fileValue(openWitness.payload, "file"), Some(File.E))
    assert(findPieceSquare(semiOpenFen, "rook_on_open_file_state", "e2", Some(Color.White)).isEmpty)

  test("diagonal lane only emits exact ray geometry and rejects underlength diagonals"):
    val positiveFen = "6k1/8/7p/8/8/8/8/2B3K1 w - - 0 1"
    val shortFen = "6k1/8/8/8/8/8/1P6/B5K1 w - - 0 1"

    val witness =
      findRay(positiveFen, "diagonal_lane_only", "c1", WitnessDirection.NorthEast).getOrElse(fail("missing diagonal lane"))

    assertEquals(witness.color, None)
    assertEquals(squareList(witness.payload, "source_piece_squares").map(_.key), Vector("c1"))
    assertEquals(squareList(witness.payload, "endpoint_squares").map(_.key), Vector("h6"))
    assert(findRay(shortFen, "diagonal_lane_only", "a1", WitnessDirection.NorthEast).isEmpty)
