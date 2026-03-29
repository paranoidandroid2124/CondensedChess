package lila.llm.analysis

import munit.FunSuite

class NarrativeDedupCoreTest extends FunSuite:

  test("exact duplicates collapse to one kept sentence") {
    val candidates = List(
      NarrativeDedupCore.buildCandidate(
        surface = "chronicle",
        role = "plan_lead",
        text = "Pressure on b2 is the main idea.",
        priority = 100,
        order = 0
      ),
      NarrativeDedupCore.buildCandidate(
        surface = "chronicle",
        role = "plan_lead",
        text = "Pressure on b2 is the main idea.",
        priority = 90,
        order = 1
      )
    )

    val kept = NarrativeDedupCore.dedupe(candidates)

    assertEquals(kept.map(_.text), List("Pressure on b2 is the main idea."))
  }

  test("near-duplicate wrappers collapse to one semantic anchor") {
    val candidates = List(
      NarrativeDedupCore.buildCandidate(
        surface = "chronicle",
        role = "support",
        text = "The key idea is pressure on b2.",
        priority = 80,
        order = 0,
        familyOverride = Some(NarrativeDedupCore.NarrativeClaimFamily.PressureAnchor)
      ),
      NarrativeDedupCore.buildCandidate(
        surface = "chronicle",
        role = "support",
        text = "A concrete target is pressure on b2.",
        priority = 80,
        order = 1,
        familyOverride = Some(NarrativeDedupCore.NarrativeClaimFamily.PressureAnchor)
      )
    )

    val kept = NarrativeDedupCore.dedupe(candidates)

    assertEquals(kept.size, 1)
    assertEquals(kept.head.text, "The key idea is pressure on b2.")
  }

  test("whole-game wrapper sentences share the same semantic anchor") {
    assert(NarrativeDedupCore.sameSemanticSentence(
      "White was mainly playing for pressure on b2.",
      "The turning point came through pressure on b2."
    ))
    assert(NarrativeDedupCore.sameSemanticSentence(
      "White was mainly playing for pressure on b2.",
      "The winning route was pressure on b2."
    ))
  }

  test("active note prose must add a distinct claim beyond the base narrative") {
    assert(!NarrativeDedupCore.proseAddsDistinctClaim(
      candidateProse = "The key idea is pressure on b2.",
      baseProse = "Pressure on b2."
    ))
    assert(NarrativeDedupCore.proseAddsDistinctClaim(
      candidateProse = "The next step is to bring the rook to b1 and attack along the b-file.",
      baseProse = "Pressure on b2."
    ))
  }
