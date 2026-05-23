package lila.commentary.analysis

import munit.FunSuite
import lila.commentary.*
import lila.commentary.model.{ ConfidenceLevel, NarrativeContext, ProbeRequest }
import lila.commentary.model.authoring.{ PlanHypothesis, PlanViability }
import lila.commentary.model.authoring.{ AuthorQuestion, AuthorQuestionKind }
import lila.commentary.analysis.PlanEvidenceEvaluator.{ EvaluatedPlan, PlanEvidenceStatus, UserFacingPlanEligibility }

final class MoveReviewPlayerPayloadBuilderTest extends FunSuite:

  private val emptyAuthoringSurface =
    AuthoringEvidenceSurface(questions = Nil, evidence = Nil, headline = None)

  private def build(
      ctx: NarrativeContext = MoveReviewProseGoldenFixtures.rookPawnMarch.ctx,
      moveReviewExplanation: Option[MoveReviewExplanation] = None,
      moveReviewLedger: Option[MoveReviewStrategicLedger] = None,
      evaluatedPlans: List[EvaluatedPlan] = Nil,
      authoringSurface: AuthoringEvidenceSurface = emptyAuthoringSurface,
      supportedLocalRows: List[MoveReviewPlayerSurfaceRow] = Nil
  ): MoveReviewPlayerSurface =
    MoveReviewPlayerPayloadBuilder.build(
      ctx = ctx,
      moveReviewExplanation = moveReviewExplanation,
      moveReviewLedger = moveReviewLedger,
      refs = None,
      evaluatedPlans = evaluatedPlans,
      authoringSurface = authoringSurface,
      supportedLocalRows = supportedLocalRows
    )

  private def plan(
      name: String,
      evidenceSources: List[String] = Nil,
      executionSteps: List[String] = Nil,
      preconditions: List[String] = Nil,
      failureModes: List[String] = Nil,
      themeL1: String = PlanTaxonomy.PlanTheme.PieceRedeployment.id,
      subplanId: Option[String] = None
  ): PlanHypothesis =
    PlanHypothesis(
      planId = name.toLowerCase.replaceAll("\\s+", "_"),
      planName = name,
      rank = 1,
      score = 0.82,
      preconditions = preconditions,
      executionSteps = executionSteps,
      failureModes = failureModes,
      viability = PlanViability(score = 0.82, label = "high", risk = "needs proof"),
      evidenceSources = evidenceSources,
      themeL1 = themeL1,
      subplanId = subplanId
    )

  private def evaluated(
      hypothesis: PlanHypothesis,
      eligibility: UserFacingPlanEligibility,
      supportProbeIds: List[String] = Nil
  ): EvaluatedPlan =
    EvaluatedPlan(
      hypothesis = hypothesis,
      status =
        if eligibility == UserFacingPlanEligibility.Refuted then PlanEvidenceStatus.Refuted
        else if eligibility == UserFacingPlanEligibility.PvCoupledOnly then PlanEvidenceStatus.PlayablePvCoupled
        else if eligibility == UserFacingPlanEligibility.Deferred then PlanEvidenceStatus.Deferred
        else PlanEvidenceStatus.PlayableEvidenceBacked,
      userFacingEligibility = eligibility,
      reason = "test",
      supportProbeIds = supportProbeIds,
      themeL1 = hypothesis.themeL1,
      subplanId = hypothesis.subplanId
    )

  test("does not build player decision comparison without certified surface input") {
    val surface =
      build(
        moveReviewExplanation =
          Some(MoveReviewExplanation(title = "Move review title", prose = "Short explanation"))
      )

    assertEquals(surface.schema, "chesstory.move_review.player_surface.v1")
    assertEquals(surface.title, Some("Move review title"))
    assertEquals(surface.decisionComparison, None)
  }

  test("probe rows come from the certified ledger only") {
    val surface =
      build(
        moveReviewLedger =
          Some(
            MoveReviewStrategicLedger(
              motifKey = "validated_plan",
              motifLabel = "Validated plan",
              stageKey = "conversion",
              stageLabel = "Conversion",
              carryOver = false,
              primaryLine =
                Some(
                  MoveReviewLedgerLine(
                    title = "Aligned line",
                    sanMoves = List("Nf3", "Nc6"),
                    note = Some("certified line note"),
                    source = "ledger"
                  )
                )
            )
          )
      )

    assert(!surface.probeRows.exists(_.source.contains("probe_request")), clue(surface.probeRows))
    assert(surface.probeRows.exists(_.text.contains("certified line note")), clue(surface.probeRows))
  }

  test("raw context probe requests do not create player rows or author meta") {
    val rawPurpose = "raw purpose from outbound request"
    val rawObjective = "raw objective from outbound request"
    val rawPlan = "Raw request-only plan"
    val rawSeed = "raw_request_seed"
    val question =
      AuthorQuestion(
        id = "why_this_raw_probe",
        kind = AuthorQuestionKind.WhyThis,
        priority = 1,
        question = "Why choose this route now?",
        why = Some("Needs certified evidence before surfacing request details."),
        confidence = ConfidenceLevel.Probe
      )
    val rawRequest =
      ProbeRequest(
        id = "raw_probe_request_1",
        fen = MoveReviewProseGoldenFixtures.rookPawnMarch.ctx.fen,
        moves = List("g2g4"),
        depth = 18,
        purpose = Some(rawPurpose),
        questionId = Some(question.id),
        questionKind = Some("WhyThis"),
        planName = Some(rawPlan),
        objective = Some(rawObjective),
        seedId = Some(rawSeed)
      )
    val ctx =
      MoveReviewProseGoldenFixtures.rookPawnMarch.ctx.copy(
        probeRequests = List(rawRequest),
        authorQuestions = List(question)
      )
    val surface =
      build(
        ctx = ctx,
        authoringSurface = AuthoringEvidenceSummaryBuilder.build(ctx)
      )
    val rendered =
      (
        surface.summaryRows.flatMap(row => List(row.label, row.text) ++ row.source.toList) ++
          surface.advancedRows.flatMap(row => List(row.label, row.text) ++ row.source.toList) ++
          surface.probeRows.flatMap(row => List(row.label, row.text) ++ row.source.toList) ++
          surface.authorRows.flatMap(row => List(row.title, row.status, row.question) ++ row.meta)
      ).mkString(" ")

    assertEquals(surface.probeRows, Nil)
    assert(!rendered.contains(rawPurpose), clue(rendered))
    assert(!rendered.contains(rawObjective), clue(rendered))
    assert(!rendered.contains(rawPlan), clue(rendered))
    assert(!rendered.contains(rawSeed), clue(rendered))
  }

  test("authoring branch source ids are not exposed as player surface row sources") {
    val rawSourceId = "probe_result_raw_id"
    val surface =
      build(
        authoringSurface =
          AuthoringEvidenceSurface(
            questions = Nil,
            evidence =
              List(
                AuthorEvidenceSummary(
                  questionId = "why_this_1",
                  questionKind = "WhyThis",
                  question = "Why does this branch matter?",
                  status = "resolved",
                  branchCount = 1,
                  branches =
                    List(
                      EvidenceBranchSummary(
                        keyMove = "Nf3",
                        line = "Nf3 Nc6",
                        sourceId = Some(rawSourceId)
                      )
                    )
                )
              ),
            headline = None
          )
      )
    val sources = surface.authorRows.flatMap(_.branches.flatMap(_.source))

    assert(!sources.contains(rawSourceId), clue(sources))
    assertEquals(sources, Nil)
  }

  test("only typed probe-backed evaluated plans are promoted into player support rows") {
    val unverified =
      build(
        evaluatedPlans =
          List(evaluated(plan("Raw kingside pressure"), UserFacingPlanEligibility.Deferred))
      )
    assert(!unverified.summaryRows.exists(_.label == "Main plans"))
    assert(!unverified.advancedRows.exists(_.label == "Execution"))

    val verified =
      build(
        evaluatedPlans =
          List(
            evaluated(
              plan("Validated rook lift", evidenceSources = Nil),
              UserFacingPlanEligibility.ProbeBacked,
              supportProbeIds = List("probe_1")
            )
          )
      )
    assert(
      verified.summaryRows.exists(row =>
        row.label == "Main plans" && row.text.contains("Validated rook lift")
      ),
      clue(verified.summaryRows)
    )
  }

  test("supported-local rows append to summary rows without leaking source metadata") {
    val supported =
      MoveReviewPlayerSurfaceRow(
        label = "Counterplay break",
        text = "A local reading is that this move stops the d5 break.",
        source = Some("counterplay_axis_suppression"),
        refSans = List("exd5")
      )
    val surface =
      build(
        evaluatedPlans =
          List(
            evaluated(
              plan("Validated rook lift"),
              UserFacingPlanEligibility.ProbeBacked,
              supportProbeIds = List("probe_1")
            )
          ),
        supportedLocalRows = List(supported, supported)
      )

    assertEquals(surface.summaryRows.map(_.label), List("Main plans", "Counterplay break"))
    assertEquals(surface.summaryRows.count(_.label == "Counterplay break"), 1)
    assertEquals(surface.summaryRows.flatMap(_.source), Nil)
    assert(surface.summaryRows.exists(_.text.contains("stops the d5 break")), clue(surface.summaryRows))
  }

  test("probe-backed plans only release rows from the promoted plan") {
    val surface =
      build(
        evaluatedPlans =
          List(
            evaluated(
              plan("Validated central plan"),
              UserFacingPlanEligibility.ProbeBacked,
              supportProbeIds = List("probe_1")
            )
          )
      )

    assert(!surface.advancedRows.exists(_.label == "Execution"), clue(surface.advancedRows))
    assert(!surface.advancedRows.exists(_.label == "Objective"), clue(surface.advancedRows))
    assert(!surface.advancedRows.exists(_.label == "Idea"), clue(surface.advancedRows))
    assert(!surface.advancedRows.exists(_.label == "Prophylaxis"), clue(surface.advancedRows))
  }

  test("execution objective and prophylaxis rows come from the promoted plan itself") {
    val promoted =
      plan(
        name = "Deny the counter-break",
        executionSteps = List("Keep the rook on d1", "Answer ...d5 with exd5"),
        preconditions = List("The d-file remains controlled"),
        failureModes = List("If ...d5 works, counterplay returns"),
        themeL1 = PlanTaxonomy.PlanTheme.RestrictionProphylaxis.id,
        subplanId = Some(PlanTaxonomy.PlanKind.BreakPrevention.id)
      )
    val surface =
      build(
        evaluatedPlans =
          List(
            evaluated(
              promoted,
              UserFacingPlanEligibility.ProbeBacked,
              supportProbeIds = List("probe_1")
            )
          )
      )

    assert(surface.advancedRows.exists(row => row.label == "Execution" && row.text.contains("Keep the rook on d1")))
    assert(surface.advancedRows.exists(row => row.label == "Objective" && row.text.contains("d-file remains controlled")))
    assert(surface.advancedRows.exists(row => row.label == "Prophylaxis" && row.text.contains("Deny the counter-break")))
  }
