package lila.llm.analysis

import munit.FunSuite

class ActiveNoteIndependenceGuardTest extends FunSuite:

  test("flags direct prior-phrase reuse") {
    val prior =
      "White stabilizes the center and reroutes the knight toward e3 before expanding on the kingside. Black still hopes for ...c5 counterplay."
    val candidate =
      "White stabilizes the center and reroutes the knight toward e3 before expanding on the kingside. route_1 shows why the reroute matters."

    val reasons = ActiveNoteIndependenceGuard.reasons(candidate, prior)

    assert(reasons.contains("active_note_prior_phrase_reuse"))
  }

  test("allows independent restatement of the same strategic facts") {
    val prior =
      "White stabilizes the center and reroutes the knight toward e3 before expanding on the kingside. Black still hopes for ...c5 counterplay."
    val candidate =
      "route_1 highlights a slower plan: White can improve the knight first, then decide whether the kingside clamp is worth committing to. The practical risk is letting Black time ...c5 before that reroute is finished."

    val reasons = ActiveNoteIndependenceGuard.reasons(candidate, prior)

    assertEquals(reasons, Nil)
  }
