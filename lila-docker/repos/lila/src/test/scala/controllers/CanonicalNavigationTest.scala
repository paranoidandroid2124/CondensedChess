package controllers

import lila.core.id.StudyChapterId

class CanonicalNavigationTest extends munit.FunSuite:

  test("preferences categories collapse to the display route"):
    assertEquals(Pref.canonicalCategory("display"), "display")
    assertEquals(Pref.canonicalCategory("privacy"), "display")
    assertEquals(Pref.canonicalCategory("game-display"), "display")
    assertEquals(Pref.canonicalCategory("game-behavior"), "display")
    assertEquals(Pref.canonicalCategory("garbage"), "display")

  test("study chapter redirect only triggers for non-canonical chapter ids"):
    val requested = StudyChapterId("abcdefgh")
    val actual = StudyChapterId("hgfedcba")

    assertEquals(Study.canonicalChapterRedirect(requested, actual), Some(actual))
    assertEquals(Study.canonicalChapterRedirect(actual, actual), None)
