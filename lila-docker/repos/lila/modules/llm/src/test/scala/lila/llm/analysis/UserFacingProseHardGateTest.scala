package lila.llm.analysis

import munit.FunSuite

class UserFacingProseHardGateTest extends FunSuite:

  test("sanitize scrubs helper notation into player-facing prose") {
    val evaluation = UserFacingProseHardGate.validate("Pin(rook, c7, queen) keeps the idea clear.")

    assertEquals(evaluation.reasons, Nil)
    assert(!evaluation.text.contains("Pin("), clue(evaluation))
    assert(evaluation.text.toLowerCase.contains("pin pressure"), clue(evaluation))
  }

  test("placeholder metadata is rejected by the hard gate") {
    val evaluation = UserFacingProseHardGate.validateSanitized("This review covers Weekend Swiss (????.??.??).")

    assert(evaluation.reasons.contains("placeholder_leak_detected"), clue(evaluation))
  }

  test("broken fragments are rejected by the hard gate") {
    val evaluation = UserFacingProseHardGate.validateSanitized("After 13.")

    assert(evaluation.reasons.contains("broken_fragment_detected"), clue(evaluation))
  }

  test("duplicate sentences are rejected by the hard gate") {
    val evaluation =
      UserFacingProseHardGate.validateSanitized(
        "Pressure on b2 became the decisive shift. Pressure on b2 became the decisive shift."
      )

    assert(evaluation.reasons.contains("duplicate_sentence_detected"), clue(evaluation))
  }
