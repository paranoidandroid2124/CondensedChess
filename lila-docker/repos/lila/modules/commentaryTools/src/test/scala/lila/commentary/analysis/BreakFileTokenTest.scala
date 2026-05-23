package lila.commentary.analysis

import munit.FunSuite

class BreakFileTokenTest extends FunSuite:

  test("extracts files only from chess square or explicit file-marker evidence") {
    assertEquals(BreakFileToken.extract("neutralized_break:d5"), Some("d"))
    assertEquals(BreakFileToken.extract("prevent_f5_break"), Some("f"))
    assertEquals(BreakFileToken.extract("deny_c_file"), Some("c"))
    assertEquals(BreakFileToken.extract("c-file"), Some("c"))
    assertEquals(BreakFileToken.extract("b_break"), Some("b"))
    assertEquals(BreakFileToken.extract("e"), Some("e"))
  }

  test("rejects incidental prose letters and natural-language articles") {
    assertEquals(BreakFileToken.extract("neutralized_break"), None)
    assertEquals(BreakFileToken.extract("hidden_freeing_break"), None)
    assertEquals(BreakFileToken.extract("stops a break"), None)
    assertEquals(BreakFileToken.extract("a break"), None)
    assertEquals(BreakFileToken.extract("a pawn"), None)
    assertEquals(BreakFileToken.extract("a file"), None)
  }
