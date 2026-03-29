package lila.llm.analysis

import munit.FunSuite

class StrategicSentenceRendererTest extends FunSuite:

  test("compensation follow-up keeps piece-head-for language concrete without generic shell phrasing") {
    val text = StrategicSentenceRenderer.renderCompensationFollowUpFromExecution("queen toward c3")

    assertEquals(text, Some("From there, the queen can head for c3."))
  }

  test("compensation follow-up keeps anchored fallback concrete") {
    val text = StrategicSentenceRenderer.renderCompensationFollowUpFromExecution("pressure on c3")

    assertEquals(text, Some("From there, the clearest continuation is pressure on c3."))
  }
