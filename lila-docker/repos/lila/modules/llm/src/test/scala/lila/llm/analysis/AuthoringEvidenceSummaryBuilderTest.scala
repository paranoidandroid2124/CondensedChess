package lila.llm.analysis

import lila.llm.{ AuthorEvidenceSummary, AuthorQuestionSummary, GameNarrativeMoment }
import lila.llm.model.*
import lila.llm.model.authoring.{ AuthorQuestion, AuthorQuestionKind }
import munit.FunSuite

class AuthoringEvidenceSummaryBuilderTest extends FunSuite:

  private val testFen = "4k3/8/8/8/8/8/8/4K3 w - - 0 1"

  private def baseContext(
      probeRequests: List[ProbeRequest],
      authorQuestions: List[AuthorQuestion],
      authorEvidence: List[lila.llm.model.authoring.QuestionEvidence]
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

  private def analysisData: ExtendedAnalysisData =
    ExtendedAnalysisData(
      fen = testFen,
      nature = PositionNature(NatureType.Static, 0.1, 0.8, "quiet"),
      motifs = Nil,
      plans = Nil,
      preventedPlans = Nil,
      pieceActivity = Nil,
      structuralWeaknesses = Nil,
      positionalFeatures = Nil,
      compensation = None,
      endgameFeatures = None,
      practicalAssessment = None,
      alternatives = Nil,
      candidates = Nil,
      counterfactual = None,
      conceptSummary = Nil,
      prevMove = None,
      ply = 24,
      evalCp = 0,
      isWhiteToMove = true
    )

  test("summarizeEvidence reports pending probe-backed author questions") {
    val question = AuthorQuestion(
      id = "latent_1",
      kind = AuthorQuestionKind.LatentPlan,
      priority = 1,
      question = "Can white keep the kingside bind after ...c5?",
      why = Some("Need one probe to validate the latent plan."),
      confidence = ConfidenceLevel.Probe
    )
    val request = ProbeRequest(
      id = "probe_latent_1",
      fen = testFen,
      moves = List("g2g4"),
      depth = 18,
      purpose = Some("latent_plan_refutation"),
      questionId = Some("latent_1"),
      questionKind = Some("LatentPlan"),
      objective = Some("validate_latent_plan"),
      planName = Some("Kingside Bind"),
      seedId = Some("kingside_bind")
    )

    val ctx = baseContext(probeRequests = List(request), authorQuestions = List(question), authorEvidence = Nil)
    val summaries = AuthoringEvidenceSummaryBuilder.summarizeEvidence(ctx)
    val summary = summaries.headOption.getOrElse(fail("missing author evidence summary"))

    assertEquals(summary.status, "pending")
    assertEquals(summary.pendingProbeIds, List("probe_latent_1"))
    assertEquals(summary.pendingProbeCount, 1)
    assertEquals(summary.linkedPlans, List("Kingside Bind", "kingside_bind"))
    assertEquals(
      AuthoringEvidenceSummaryBuilder.headline(ctx),
      Some("latent plan evidence is pending across 1 probe")
    )
  }

  test("game narrative moment preserves authoring payload for API transport") {
    val moment = MomentNarrative(
      ply = 24,
      momentType = "TensionPeak",
      narrative = "White keeps the bind.",
      analysisData = analysisData,
      probeRequests = List(
        ProbeRequest(
          id = "probe_latent_1",
          fen = testFen,
          moves = List("g2g4"),
          depth = 16,
          questionId = Some("latent_1")
        )
      ),
      authorQuestions = List(
        AuthorQuestionSummary(
          id = "latent_1",
          kind = "LatentPlan",
          priority = 1,
          question = "Can white keep the kingside bind after ...c5?",
          confidence = "Probe"
        )
      ),
      authorEvidence = List(
        AuthorEvidenceSummary(
          questionId = "latent_1",
          questionKind = "LatentPlan",
          question = "Can white keep the kingside bind after ...c5?",
          status = "pending",
          pendingProbeIds = List("probe_latent_1"),
          pendingProbeCount = 1
        )
      )
    )

    val apiMoment = GameNarrativeMoment.fromMoment(moment)

    assertEquals(apiMoment.probeRequests.map(_.id), List("probe_latent_1"))
    assertEquals(apiMoment.authorQuestions.map(_.id), List("latent_1"))
    assertEquals(apiMoment.authorEvidence.map(_.status), List("pending"))
  }
