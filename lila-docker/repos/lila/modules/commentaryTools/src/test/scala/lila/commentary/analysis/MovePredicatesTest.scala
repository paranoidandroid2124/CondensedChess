package lila.commentary.analysis

import chess.{ Bishop, Queen }
import lila.commentary.model.authoring.{ LineType, MovePattern }
import munit.FunSuite

class MovePredicatesTest extends FunSuite:

  test("diagonal battery formation requires a same-diagonal partner after the move") {
    val withPartner = "4k3/7Q/8/8/8/8/6B1/4K3 w - - 0 1"
    val withoutPartner = "4k3/8/8/8/8/8/6B1/4K3 w - - 0 1"
    val blockedPartner = "4k3/7Q/8/5P2/8/8/6B1/4K3 w - - 0 1"
    val pattern = MovePattern.BatteryFormation(Bishop, Queen, LineType.DiagonalLine)

    assert(MovePredicates.matchesMovePattern(withPartner, "g2e4", pattern))
    assert(!MovePredicates.matchesMovePattern(withoutPartner, "g2e4", pattern))
    assert(!MovePredicates.matchesMovePattern(blockedPartner, "g2e4", pattern))
  }
