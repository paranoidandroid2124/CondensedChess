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
          kind = AuthorQuestionKind.TensionDecision,
          priority = 1,
          question = "Which branch keeps queenside pressure?"
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
          questionKinds = List(AuthorQuestionKind.TensionDecision),
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
      authorQuestions = List(AuthorQuestionSummary("q1", "TensionDecision", 1, "Which branch keeps queenside pressure?", None, Nil, "Probe", None, None)),
      authorEvidence = List(
        AuthorEvidenceSummary(
          questionId = "q1",
          questionKind = "TensionDecision",
          question = "Which branch keeps queenside pressure?",
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
