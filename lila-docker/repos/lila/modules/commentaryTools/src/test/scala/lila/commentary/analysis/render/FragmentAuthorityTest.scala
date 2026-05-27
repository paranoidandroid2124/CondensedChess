package lila.commentary.analysis.render

import munit.FunSuite

class FragmentAuthorityTest extends FunSuite:

  test("support-only fragments fail closed without grounding") {
    val fragment = FragmentAuthority.supportFragment("support detail")

    assertEquals(fragment.releasedText, "")
  }

  test("support-only fragments release when explicitly grounded") {
    val fragment = FragmentAuthority.supportFragment("support detail", sceneGrounded = true)

    assertEquals(fragment.releasedText, "support detail")
  }
