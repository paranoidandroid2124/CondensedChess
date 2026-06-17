package lila.commentary.analysis

import lila.commentary.model.*
import lila.commentary.model.authoring.{ AuthorQuestion, AuthorQuestionKind }
import munit.FunSuite

class AuthoringEvidenceSummaryBuilderTest extends FunSuite:

  private val testFen = "4k3/8/8/8/8/8/8/4K3 w - - 0 1"

  private def baseContext(
      probeRequests: List[ProbeRequest],
      authorQuestions: List[AuthorQuestion],
      authorEvidence: List[lila.commentary.model.authoring.QuestionEvidence]
  ): NarrativeContext =
    NarrativeContext(
      fen = testFen,
      header = ContextHeader("Middlegame", "Normal", "NarrowChoice", "Medium", "ExplainPlan"),
      ply = 24,
      summary = NarrativeSummary("Hold the kingside", None, "NarrowChoice", "Maintain", "0.00"),
      threats = ThreatTable(Nil, Nil),
      pawnPlay = PawnPlayTable(false, None, "Low", "Maintain", "Quiet", "Background", None, false, "quiet"),
      plans = PlanTable(top5 = Nil, suppressed = Nil),
      delta = None,
      phase = PhaseContext("Middlegame", "Balanced middlegame"),
      candidates = Nil,
      probeRequests = probeRequests,
      authorQuestions = authorQuestions,
      authorEvidence = authorEvidence
    )

  test("summarizeEvidence reports pending probe-backed author questions") {
    val question = AuthorQuestion(
      id = "why_this_1",
      kind = AuthorQuestionKind.WhyThis,
      priority = 1,
      question = "Why choose the kingside bind now?",
      why = Some("Need one probe to validate the latent plan."),
      confidence = ConfidenceLevel.Probe,
      evidencePurposes = List("reply_multipv")
    )
    val request = ProbeRequest(
      id = "probe_why_this_1",
      fen = testFen,
      moves = List("g2g4"),
      depth = 18,
      purpose = Some("reply_multipv"),
      questionId = Some("why_this_1"),
      questionKind = Some("WhyThis"),
      objective = Some("validate_reply_branch"),
      planName = Some("Kingside Bind"),
      seedId = Some("kingside_bind")
    )

    val ctx = baseContext(probeRequests = List(request), authorQuestions = List(question), authorEvidence = Nil)
    val surface = AuthoringEvidenceSummaryBuilder.build(ctx)
    val summary = surface.evidence.headOption.getOrElse(fail("missing author evidence summary"))

    assertEquals(summary.status, "pending")
    assertEquals(summary.pendingProbeIds, List("probe_why_this_1"))
    assertEquals(summary.pendingProbeCount, 1)
    assertEquals(summary.linkedPlans, List("Kingside Bind", "kingside_bind"))
    assertEquals(surface.questions.map(_.id), List("why_this_1"))
    assertEquals(surface.headline, Some("author evidence: 0 line checked, 1 pending"))
  }

  test("summarizeEvidence treats probe lines on a real position as line-checked support") {
    val base = MoveReviewProseGoldenFixtures.openFileFight.ctx
    assertEquals(base.fen, "2r2rk1/pp3pp1/2pq1n1p/3p4/3P4/1QP1PNRP/P4PP1/2R3K1 w - - 0 22")
    val question = AuthorQuestion(
      id = "q-open-file",
      kind = AuthorQuestionKind.WhyThis,
      priority = 1,
      question = "Why does the rook lift keep pressure on g7?",
      why = Some("The branch should stay a checked follow-up line, not a resolved main reason."),
      confidence = ConfidenceLevel.Probe,
      evidencePurposes = List("reply_multipv")
    )
    val ctx = base.copy(authorQuestions = List(question))
    val surface = AuthoringEvidenceSummaryBuilder.build(ctx)
    val summary = surface.evidence.headOption.getOrElse(fail("missing author evidence summary"))

    assertEquals(summary.status, "line_checked")
    assertEquals(summary.branchCount, 1)
    assertEquals(summary.branches.map(_.keyMove), List("...Rc8"))
    assertEquals(summary.branches.flatMap(_.evalCp), List(42))
    assertEquals(summary.branches.flatMap(_.depth), List(23))
    assertEquals(summary.branches.flatMap(_.sourceId), List("probe-open-file"))
    assertEquals(surface.headline, Some("author evidence: 1 line checked, 0 pending"))
  }
