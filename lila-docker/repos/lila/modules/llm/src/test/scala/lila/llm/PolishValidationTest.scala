package lila.llm

class PolishValidationTest extends munit.FunSuite:

  test("accepts numbered move sequences that preserve order and tokens") {
    val original = "Main line: 14 Ne6 Nxe5 15 Bxe5 Qf2! wins material."
    val polished = "Concrete route: 14 Ne6 Nxe5 15 Bxe5 Qf2!, forcing practical problems."
    assert(PolishValidation.isPolishedCommentaryValid(polished, original, allowedSans = Nil))
  }

  test("rejects invented SAN tokens") {
    val original = "17... d5! challenges the center."
    val polished = "17... Nf6! is strongest here."
    assert(!PolishValidation.isPolishedCommentaryValid(polished, original, allowedSans = Nil))
  }

  test("rejects numbered sequence when move numbers are dropped") {
    val original = "Main line: 14 Ne6 Nxe5 15 Bxe5 Qf2! wins material."
    val polished = "Ne6 Nxe5 Bxe5 Qf2! wins material."
    assert(!PolishValidation.isPolishedCommentaryValid(polished, original, allowedSans = Nil))
  }

  test("rejects SAN order inversion that can break mini-board mapping") {
    val original = "Main line: 14 Ne6 Nxe5 15 Bxe5 Qf2! wins material."
    val polished = "15 Bxe5 Qf2! and only then 14 Ne6 appears."
    assert(!PolishValidation.isPolishedCommentaryValid(polished, original, allowedSans = Nil))
  }

  test("rejects prose-only polish when original contains SAN tokens") {
    val original = "Main line: 14 Ne6 Nxe5 15 Bxe5 Qf2! wins material."
    val polished = "Black keeps the initiative and practical pressure."
    assert(!PolishValidation.isPolishedCommentaryValid(polished, original, allowedSans = Nil))
  }

  test("allows extension lines only when they are in allowed SAN list") {
    val original = "17... d5! challenges the center."
    val polished = "17... d5! and after 18 exd5 Qxd5, Black keeps active pieces."
    val allowed = List("d5", "exd5", "Qxd5")
    assert(PolishValidation.isPolishedCommentaryValid(polished, original, allowedSans = allowed))
  }

  test("rejects black-to-move ellipsis marker mutation") {
    val original = "17... d5! is the thematic equalizer."
    val polished = "17. d5! is the thematic equalizer."
    assert(!PolishValidation.isPolishedCommentaryValid(polished, original, allowedSans = Nil))
  }

  test("allows extra move numbers for allowed extension line while preserving original marker style") {
    val original = "17... d5! challenges the center."
    val polished = "17... d5! and if 18 exd5, then 18... Qxd5 keeps activity."
    val allowed = List("d5", "exd5", "Qxd5")
    assert(PolishValidation.isPolishedCommentaryValid(polished, original, allowedSans = allowed))
  }

  test("accepts black marker notation without whitespace after ellipsis") {
    val original = "17...d5! is the thematic equalizer."
    val polished = "17... d5! is the thematic equalizer and keeps counterplay."
    assert(PolishValidation.isPolishedCommentaryValid(polished, original, allowedSans = Nil))
  }

  test("returns explicit reason for marker style mismatch") {
    val original = "17... d5! is the thematic equalizer."
    val polished = "17. d5! is the thematic equalizer."
    val result = PolishValidation.validatePolishedCommentary(polished, original, allowedSans = Nil)
    assertEquals(result.isValid, false)
    assert(result.reasons.contains("marker_style_mismatch"))
  }

  test("keeps multi-pv eval tokens unchanged") {
    val original = "a) Bxh7+ Kxh7 Qh5+ (+1.5)\nb) O-O b6 Re1 (+0.3)"
    val polished = "a) Bxh7+ Kxh7 Qh5+ (+1.5), with pressure.\nb) O-O b6 Re1 (+0.3), more stable."
    assert(PolishValidation.isPolishedCommentaryValid(polished, original, allowedSans = Nil))
  }

  test("rejects changed multi-pv eval tokens") {
    val original = "a) Bxh7+ Kxh7 Qh5+ (+1.5)\nb) O-O b6 Re1 (+0.3)"
    val polished = "a) Bxh7+ Kxh7 Qh5+ (+0.8)\nb) O-O b6 Re1 (+0.3)"
    val result = PolishValidation.validatePolishedCommentary(polished, original, allowedSans = Nil)
    assertEquals(result.isValid, false)
    assert(result.reasons.contains("eval_order_violation"))
  }

  test("rejects dropping variation branch labels") {
    val original = "a) Bxh7+ Kxh7 Qh5+ (+1.5)\nb) O-O b6 Re1 (+0.3)"
    val polished = "Bxh7+ Kxh7 Qh5+ (+1.5)\nO-O b6 Re1 (+0.3)"
    val result = PolishValidation.validatePolishedCommentary(polished, original, allowedSans = Nil)
    assertEquals(result.isValid, false)
    assert(result.reasons.contains("variation_branch_violation"))
  }
