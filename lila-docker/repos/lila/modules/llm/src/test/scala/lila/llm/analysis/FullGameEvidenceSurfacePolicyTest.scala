package lila.llm.analysis

import munit.FunSuite
import lila.llm.{ AuthorEvidenceSummary, AuthorQuestionSummary }
import lila.llm.model.*
import lila.llm.model.authoring.*

class FullGameEvidenceSurfacePolicyTest extends FunSuite:

  private def baseContext: NarrativeContext =
    NarrativeContext(
      fen = "4k3/8/8/8/8/8/8/4K3 w - - 0 1",
      header = ContextHeader("Opening", "Normal", "NarrowChoice", "Medium", "ExplainPlan"),
      ply = 12,
      playedMove = Some("a1b1"),
      playedSan = Some("Rb1"),
      summary = NarrativeSummary("Queenside pressure", None, "NarrowChoice", "Maintain", "0.00"),
      threats = ThreatTable(Nil, Nil),
      pawnPlay = PawnPlayTable(false, None, "Low", "Maintain", "Quiet", "Background", None, false, "quiet"),
      plans = PlanTable(Nil, Nil),
      delta = None,
      phase = PhaseContext("Opening", "Theory branch"),
      candidates = Nil,
      openingEvent = Some(OpeningEvent.BranchPoint(List("...Qc7", "...Qe7"), "branch", None)),
      probeRequests = List(
        ProbeRequest(
          id = "probe_1",
          fen = "4k3/8/8/8/8/8/8/4K3 w - - 0 1",
          moves = List("b2b4"),
          depth = 18,
          purpose = Some("opening_branch_validation")
        ),
        ProbeRequest(
          id = "probe_2",
          fen = "4k3/8/8/8/8/8/8/4K3 w - - 0 1",
          moves = List("d4d5"),
          depth = 16,
          purpose = Some("extra")
        )
      ),
      authorQuestions = List(
        AuthorQuestion(
          id = "q1",
          kind = AuthorQuestionKind.WhyThis,
          priority = 1,
          question = "Why does this branch keep queenside pressure?",
          evidencePurposes = List("reply_multipv")
        )
      ),
      authorEvidence = List(
        QuestionEvidence(
          questionId = "q1",
          purpose = "opening_branch_validation",
          branches = List(EvidenceBranch("...Qc7", "...Qc7 Rb1 b5", Some(24), None, Some(18), Some("probe_1")))
        )
      ),
      renderMode = NarrativeRenderMode.FullGame
    )

  test("surface policy marks opening branch moments eligible and caps requests") {
    val ctx = baseContext
    val outline = NarrativeOutline(
      List(
        OutlineBeat(OutlineBeatKind.Context, "Opening branch.", focusPriority = 100, fullGameEssential = true),
        OutlineBeat(
          OutlineBeatKind.DecisionPoint,
          "The practical alternative ...Qe7 stays secondary because it loses the b-file.",
          questionIds = List("q1"),
          questionKinds = List(AuthorQuestionKind.WhyThis),
          focusPriority = 96,
          fullGameEssential = true
        )
      )
    )

    val eligible = FullGameEvidenceSurfacePolicy.eligible("SustainedPressure", ctx, outline)
    assert(eligible)

    val payload = FullGameEvidenceSurfacePolicy.payload(
      eligible = eligible,
      probeRequests = ctx.probeRequests,
      authorQuestions = List(AuthorQuestionSummary("q1", "WhyThis", 1, "Why does this branch keep queenside pressure?", None, Nil, "Probe", None, None)),
      authorEvidence = List(
        AuthorEvidenceSummary(
          questionId = "q1",
          questionKind = "WhyThis",
          question = "Why does this branch keep queenside pressure?",
          why = None,
          status = "resolved",
          purposes = Nil,
          branchCount = 1,
          branches = Nil,
          pendingProbeIds = Nil,
          pendingProbeCount = 0,
          probeObjectives = Nil,
          linkedPlans = Nil
        )
      )
    )

    assertEquals(payload.probeRequests.map(_.id), List("probe_1"))
    assertEquals(payload.authorQuestions.size, 1)
    assertEquals(payload.authorEvidence.size, 1)
  }

  test("surface policy drops non-eligible moments entirely") {
    val ctx = baseContext.copy(
      openingEvent = Some(OpeningEvent.Intro("D35", "Queen's Gambit", "pressure on c-file", Nil)),
      probeRequests = Nil,
      authorEvidence = Nil
    )
    val outline = NarrativeOutline(List(OutlineBeat(OutlineBeatKind.Context, "Quiet move.", focusPriority = 100, fullGameEssential = true)))

    val eligible = FullGameEvidenceSurfacePolicy.eligible("TensionPeak", ctx, outline)
    assert(!eligible)

    val payload = FullGameEvidenceSurfacePolicy.payload(
      eligible = eligible,
      probeRequests = List.empty,
      authorQuestions = List.empty,
      authorEvidence = List.empty
    )
    assert(!payload.nonEmpty)
  }

  test("runtime payload preserves question-first carry even when probe surface is not eligible") {
    val payload = FullGameEvidenceSurfacePolicy.runtimePayload(
      allowProbeRequests = false,
      probeRequests = baseContext.probeRequests,
      authorQuestions =
        List(
          AuthorQuestionSummary(
            "q1",
            "WhyNow",
            1,
            "Why does this have to be played now?",
            None,
            Nil,
            "Probe",
            None,
            None
          )
        ),
      authorEvidence =
        List(
          AuthorEvidenceSummary(
            questionId = "q1",
            questionKind = "WhyNow",
            question = "Why does this have to be played now?",
            why = None,
            status = "resolved",
            purposes = List("reply_multipv"),
            branchCount = 1,
            branches = Nil,
            pendingProbeIds = Nil,
            pendingProbeCount = 0,
            probeObjectives = Nil,
            linkedPlans = Nil
          )
        )
    )

    assertEquals(payload.probeRequests, Nil)
    assertEquals(payload.authorQuestions.map(_.kind), List("WhyNow"))
    assertEquals(payload.authorEvidence.map(_.questionKind), List("WhyNow"))
  }

  test("runtime payload keeps evidence-linked question kinds aligned with carried evidence") {
    val payload = FullGameEvidenceSurfacePolicy.runtimePayload(
      allowProbeRequests = false,
      probeRequests = Nil,
      authorQuestions =
        List(
          AuthorQuestionSummary("q1", "WhyThis", 1, "Why this?", None, Nil, "Probe", None, None),
          AuthorQuestionSummary("q2", "WhatChanged", 2, "What changed?", None, Nil, "Probe", None, None),
          AuthorQuestionSummary("q3", "WhyNow", 3, "Why now?", None, Nil, "Probe", None, None)
        ),
      authorEvidence =
        List(
          AuthorEvidenceSummary(
            questionId = "q1",
            questionKind = "WhyThis",
            question = "Why this?",
            why = None,
            status = "pending",
            purposes = List("reply_multipv"),
            branchCount = 0,
            branches = Nil,
            pendingProbeIds = List("probe_1"),
            pendingProbeCount = 1,
            probeObjectives = Nil,
            linkedPlans = Nil
          ),
          AuthorEvidenceSummary(
            questionId = "q3",
            questionKind = "WhyNow",
            question = "Why now?",
            why = None,
            status = "pending",
            purposes = List("reply_multipv"),
            branchCount = 0,
            branches = Nil,
            pendingProbeIds = List("probe_2"),
            pendingProbeCount = 1,
            probeObjectives = Nil,
            linkedPlans = Nil
          )
        )
    )

    assertEquals(payload.authorQuestions.map(_.kind), List("WhyThis", "WhyNow"))
    assertEquals(payload.authorQuestions.map(_.id), List("q1", "q3"))
    assertEquals(payload.authorEvidence.map(_.questionKind), List("WhyThis", "WhyNow"))
    assertEquals(payload.authorEvidence.map(_.questionId), List("q1", "q3"))
  }

  test("internal probe planner prioritizes high-salience and compensation moments before generic opening branches") {
    val selected =
      FullGameEvidenceSurfacePolicy.selectInternalProbeMoments(
        List(
          FullGameEvidenceSurfacePolicy.InternalProbeCandidate(
            ply = 18,
            selectionKind = "opening",
            strategicSalienceHigh = false,
            ownerMismatch = false,
            compensation = false,
            hasDeferredAlternative = false,
            hasOpeningBranch = true,
            hasStructureDeferred = false,
            hasBlunderWhyNot = false,
            hasEndgameContinuation = false,
            probeRequests = List(baseContext.probeRequests.head)
          ),
          FullGameEvidenceSurfacePolicy.InternalProbeCandidate(
            ply = 24,
            selectionKind = "thread_bridge",
            strategicSalienceHigh = false,
            ownerMismatch = true,
            compensation = true,
            hasDeferredAlternative = true,
            hasOpeningBranch = false,
            hasStructureDeferred = true,
            hasBlunderWhyNot = false,
            hasEndgameContinuation = false,
            probeRequests = List(baseContext.probeRequests.head)
          ),
          FullGameEvidenceSurfacePolicy.InternalProbeCandidate(
            ply = 12,
            selectionKind = "key",
            strategicSalienceHigh = true,
            ownerMismatch = false,
            compensation = false,
            hasDeferredAlternative = false,
            hasOpeningBranch = false,
            hasStructureDeferred = false,
            hasBlunderWhyNot = false,
            hasEndgameContinuation = false,
            probeRequests = List(baseContext.probeRequests.head)
          ),
          FullGameEvidenceSurfacePolicy.InternalProbeCandidate(
            ply = 36,
            selectionKind = "key",
            strategicSalienceHigh = false,
            ownerMismatch = false,
            compensation = false,
            hasDeferredAlternative = false,
            hasOpeningBranch = false,
            hasStructureDeferred = false,
            hasBlunderWhyNot = true,
            hasEndgameContinuation = false,
            probeRequests = List(baseContext.probeRequests.head)
          )
        )
      )

    assertEquals(selected, List(12, 24, 36))
  }

  test("internal probe planner respects selection-kind tie breaks at the same tier") {
    val selected =
      FullGameEvidenceSurfacePolicy.selectInternalProbeMoments(
        List(
          FullGameEvidenceSurfacePolicy.InternalProbeCandidate(
            ply = 22,
            selectionKind = "active-note-only",
            strategicSalienceHigh = false,
            ownerMismatch = false,
            compensation = true,
            hasDeferredAlternative = false,
            hasOpeningBranch = false,
            hasStructureDeferred = false,
            hasBlunderWhyNot = false,
            hasEndgameContinuation = false,
            probeRequests = List(baseContext.probeRequests.head)
          ),
          FullGameEvidenceSurfacePolicy.InternalProbeCandidate(
            ply = 20,
            selectionKind = "thread_bridge",
            strategicSalienceHigh = false,
            ownerMismatch = false,
            compensation = true,
            hasDeferredAlternative = false,
            hasOpeningBranch = false,
            hasStructureDeferred = false,
            hasBlunderWhyNot = false,
            hasEndgameContinuation = false,
            probeRequests = List(baseContext.probeRequests.head)
          ),
          FullGameEvidenceSurfacePolicy.InternalProbeCandidate(
            ply = 18,
            selectionKind = "key",
            strategicSalienceHigh = false,
            ownerMismatch = false,
            compensation = true,
            hasDeferredAlternative = false,
            hasOpeningBranch = false,
            hasStructureDeferred = false,
            hasBlunderWhyNot = false,
            hasEndgameContinuation = false,
            probeRequests = List(baseContext.probeRequests.head)
          )
        )
      )

    assertEquals(selected, List(18, 20, 22))
  }
