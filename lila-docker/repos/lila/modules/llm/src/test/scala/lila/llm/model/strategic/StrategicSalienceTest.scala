package lila.llm.model.strategic

import lila.llm.model.TransitionType
import munit.FunSuite

class StrategicSalienceTest extends FunSuite:

  test("Continuation should yield High salience on Fruition (3 plies)") {
    val salience = StrategicSalience.calculate(
      transitionType = TransitionType.Continuation,
      consecutivePlies = 3,
      evalDeltaCp = 0,
      themeMaxShare = 0.5
    )
    assertEquals(salience, StrategicSalience.High)
  }

  test("Continuation should yield Low salience otherwise (1 ply)") {
    val salience = StrategicSalience.calculate(
      transitionType = TransitionType.Continuation,
      consecutivePlies = 1,
      evalDeltaCp = 0,
      themeMaxShare = 0.5
    )
    assertEquals(salience, StrategicSalience.Low)
  }

  test("ForcedPivot should yield High salience regardless of plies") {
    val salience = StrategicSalience.calculate(
      transitionType = TransitionType.ForcedPivot,
      consecutivePlies = 1,
      evalDeltaCp = 0,
      themeMaxShare = 0.5
    )
    assertEquals(salience, StrategicSalience.High)
  }

  test("NaturalShift should yield High salience regardless of plies") {
    val salience = StrategicSalience.calculate(
      transitionType = TransitionType.NaturalShift,
      consecutivePlies = 1,
      evalDeltaCp = 0,
      themeMaxShare = 0.5
    )
    assertEquals(salience, StrategicSalience.High)
  }

  test("Opportunistic should yield High salience regardless of plies") {
    val salience = StrategicSalience.calculate(
      transitionType = TransitionType.Opportunistic,
      consecutivePlies = 1,
      evalDeltaCp = 0,
      themeMaxShare = 0.5
    )
    assertEquals(salience, StrategicSalience.High)
  }

  test("Continuation with huge eval delta (blunder/tactics) should yield Low salience to suppress strategy") {
    val salience = StrategicSalience.calculate(
      transitionType = TransitionType.Continuation,
      consecutivePlies = 1,
      evalDeltaCp = 400,
      themeMaxShare = 0.5
    )
    assertEquals(salience, StrategicSalience.Low)
  }


