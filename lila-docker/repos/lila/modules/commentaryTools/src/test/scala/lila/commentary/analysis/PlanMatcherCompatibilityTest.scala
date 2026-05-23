package lila.commentary.analysis

import chess.Color
import lila.commentary.model.{ Plan, PlanMatch }
import munit.FunSuite

class PlanMatcherCompatibilityTest extends FunSuite:

  private def themed(plan: Plan, score: Double, theme: String): PlanMatch =
    PlanMatch(plan, score, Nil, supports = List(s"theme:$theme"))

  private val quietContext =
    IntegratedContext(
      evalCp = 0,
      isWhiteToMove = true
    )

  test("immediate tactical override downweights each non-tactical theme only once") {
    val restrictionA =
      themed(Plan.Prophylaxis(Color.White, "c-file counterplay"), 0.60, PlanMatcher.Theme.Restriction)
    val restrictionB =
      themed(Plan.Prophylaxis(Color.White, "d-file counterplay"), 0.50, PlanMatcher.Theme.Restriction)
    val redeployment =
      themed(Plan.PieceActivation(Color.White), 0.40, PlanMatcher.Theme.Redeployment)
    val tactical =
      themed(Plan.Sacrifice(Color.White, "knight"), 0.75, PlanMatcher.Theme.ImmediateTacticalGain)

    val (adjusted, events) =
      PlanMatcher.applyCompatWithEvents(
        plans = List(tactical, restrictionA, restrictionB, redeployment),
        ctx = quietContext,
        side = Color.White
      )

    def score(name: String): Double =
      adjusted.find(_.plan.name == name).map(_.score).getOrElse(fail(s"missing plan $name"))

    assertEqualsDouble(score(restrictionA.plan.name), 0.60 * 0.48, 1e-9)
    assertEqualsDouble(score(restrictionB.plan.name), 0.50 * 0.48, 1e-9)
    assertEqualsDouble(score(redeployment.plan.name), 0.40 * 0.48, 1e-9)
    assertEqualsDouble(score(tactical.plan.name), 0.75, 1e-9)
    assertEquals(
      events.count(_.reason == "override: immediate tactical gain"),
      3,
      clues(events)
    )
  }
