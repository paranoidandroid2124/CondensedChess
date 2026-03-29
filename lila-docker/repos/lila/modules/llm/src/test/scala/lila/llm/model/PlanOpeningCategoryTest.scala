package lila.llm.model

import chess.Color
import munit.FunSuite

class PlanOpeningCategoryTest extends FunSuite:

  test("OpeningDevelopment should use Opening category") {
    val plan = Plan.OpeningDevelopment(Color.White)
    assertEquals(plan.id, PlanId.OpeningDevelopment)
    assertEquals(plan.category, PlanCategory.Opening)
  }

  test("OpeningDevelopment stays in the same user-facing category for both colors") {
    val whitePlan = Plan.OpeningDevelopment(Color.White)
    val blackPlan = Plan.OpeningDevelopment(Color.Black)

    assertEquals(whitePlan.category, PlanCategory.Opening)
    assertEquals(blackPlan.category, PlanCategory.Opening)
    assertEquals(whitePlan.name, blackPlan.name)
  }
