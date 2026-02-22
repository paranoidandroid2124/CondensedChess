package lila.llm.analysis

import munit.FunSuite
import _root_.chess.Color

class PerspectiveMathTest extends FunSuite:

  test("cpLossForMover computes white mover loss from White POV scores") {
    assertEquals(PerspectiveMath.cpLossForMover(Color.White, bestWhiteCp = 120, playedWhiteCp = 20), 100)
    assertEquals(PerspectiveMath.cpLossForMover(Color.White, bestWhiteCp = 50, playedWhiteCp = 90), 0)
  }

  test("cpLossForMover computes black mover loss from White POV scores") {
    assertEquals(PerspectiveMath.cpLossForMover(Color.Black, bestWhiteCp = -40, playedWhiteCp = 80), 120)
    assertEquals(PerspectiveMath.cpLossForMover(Color.Black, bestWhiteCp = 20, playedWhiteCp = -60), 0)
  }

  test("improvementForMover computes mover-relative gain for prophylaxis comparisons") {
    assertEquals(PerspectiveMath.improvementForMover(Color.White, defendedWhiteCp = 40, threatWhiteCp = -70), 110)
    assertEquals(PerspectiveMath.improvementForMover(Color.Black, defendedWhiteCp = 40, threatWhiteCp = 260), 220)
  }
