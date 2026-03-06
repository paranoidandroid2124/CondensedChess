package lila.llm.model

import chess.Color
import munit.FunSuite

class PlanOpeningCategoryTest extends FunSuite:

  test("OpeningDevelopment should use Opening category") {
    val plan = Plan.OpeningDevelopment(Color.White)
    assertEquals(plan.id, PlanId.OpeningDevelopment)
    assertEquals(plan.category, PlanCategory.Opening)
  }

