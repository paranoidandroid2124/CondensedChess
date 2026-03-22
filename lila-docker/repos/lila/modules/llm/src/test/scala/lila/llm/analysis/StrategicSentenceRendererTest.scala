package lila.llm.analysis

import munit.FunSuite

class StrategicSentenceRendererTest extends FunSuite:

  test("objective support keeps piece-head-for language conditional") {
    val text = StrategicSentenceRenderer.renderObjectiveSupport("the queen can head for c3")

    assertEquals(text, "A concrete target is c3 for the queen.")
  }

  test("objective claim frames piece-head-for language as preparation rather than current-board fact") {
    val text = StrategicSentenceRenderer.renderObjectiveClaim("the queen can head for c3")

    assertEquals(text, "The move is mainly about bringing the queen to c3.")
  }

  test("compensation support keeps piece-head-for language conditional") {
    val text = StrategicSentenceRenderer.renderCompensationSupportFromObjective("the rook can head for b2")

    assertEquals(text, "Bringing the rook to b2 matters more than grabbing the material back right away.")
  }
