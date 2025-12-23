package chess.analysis

import munit.FunSuite

class AntiFluffGateTest extends FunSuite {
  
  test("Scrub banned words but stay valid") {
    val text = "This is an amazing and brilliant move!"
    val result = AntiFluffGate.validate(text)
    assert(result.isValid)
    assertEquals(result.cleanedText, "This is an and move!")
  }

  test("Reject missing citations when required") {
    val text = "White has a strong position because of the isolated pawn."
    val result = AntiFluffGate.validate(text, requiredCitations = true)
    assert(!result.isValid)
    assert(result.reasons.exists(_.contains("citation")))
  }

  test("Accept valid text with citations") {
    val text = "White has a strong position due to the isolated pawn (src:struct:iqp_white)."
    val result = AntiFluffGate.validate(text, requiredCitations = true)
    assert(result.isValid)
  }

  test("Auto-clean flowery prefixes") {
    val text = "As a chess coach, White is winning here."
    val cleaned = AntiFluffGate.semiClean(text)
    assertEquals(cleaned, "White is winning here.")
  }

  test("Auto-clean markdown blocks") {
    val text = "```text\nWhite wins!\n```"
    val cleaned = AntiFluffGate.semiClean(text)
    assertEquals(cleaned, "White wins!")
  }

  test("Reject too many sentences") {
    val longText = "One. Two. Three. Four. Five. Six. Seven. Eight. Nine. Ten."
    val result = AntiFluffGate.validate(longText)
    assert(!result.isValid)
    assert(result.reasons.exists(_.contains("Text too long")))
  }
}
