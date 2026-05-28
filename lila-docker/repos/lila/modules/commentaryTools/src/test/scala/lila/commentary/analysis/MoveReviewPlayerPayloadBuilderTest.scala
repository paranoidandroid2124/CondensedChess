package lila.commentary.analysis

import munit.FunSuite
import lila.commentary.*
import lila.commentary.model.{ ConfidenceLevel, NarrativeContext, ProbeRequest }
import lila.commentary.model.authoring.{ PlanHypothesis, PlanViability }
import lila.commentary.model.authoring.{ AuthorQuestion, AuthorQuestionKind }
import lila.commentary.model.strategic.{ EngineEvidence, VariationLine }
import lila.commentary.analysis.PlanEvidenceEvaluator.{ ClaimCertification, EvaluatedPlan, PlanEvidenceStatus, UserFacingPlanEligibility }

final class MoveReviewPlayerPayloadBuilderTest extends FunSuite:

  private val emptyAuthoringSurface =
    AuthoringEvidenceSurface(questions = Nil, evidence = Nil, headline = None)

  private def build(
      ctx: NarrativeContext = MoveReviewProseGoldenFixtures.rookPawnMarch.ctx,
      moveReviewExplanation: Option[MoveReviewExplanation] = None,
      moveReviewLedger: Option[MoveReviewStrategicLedger] = None,
      evaluatedPlans: List[EvaluatedPlan] = Nil,
      authoringSurface: AuthoringEvidenceSurface = emptyAuthoringSurface,
      supportedLocalRows: List[MoveReviewPlayerSurfaceRow] = Nil,
      decisionComparisonSurface: Option[MoveReviewPlayerDecisionComparison] = None
  ): MoveReviewPlayerSurface =
    MoveReviewPlayerPayloadBuilder.build(
      ctx = ctx,
      moveReviewExplanation = moveReviewExplanation,
      moveReviewLedger = moveReviewLedger,
      refs = None,
      evaluatedPlans = evaluatedPlans,
      authoringSurface = authoringSurface,
      supportedLocalRows = supportedLocalRows,
      decisionComparisonSurface = decisionComparisonSurface
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
      supportProbeIds: List[String] = Nil,
      transpositionProofIds: List[String] = Nil,
      claimCertification: ClaimCertification = ClaimCertification()
  ): EvaluatedPlan =
    EvaluatedPlan(
      hypothesis = hypothesis,
      status =
        if eligibility == UserFacingPlanEligibility.Refuted then PlanEvidenceStatus.Refuted
        else if eligibility == UserFacingPlanEligibility.TranspositionAligned then PlanEvidenceStatus.PlayableTranspositionAligned
        else if eligibility == UserFacingPlanEligibility.PvCoupledOnly then PlanEvidenceStatus.PlayablePvCoupled
        else if eligibility == UserFacingPlanEligibility.Deferred then PlanEvidenceStatus.Deferred
        else if eligibility == UserFacingPlanEligibility.StructuralOnly then PlanEvidenceStatus.PlayableStructuralOnly
        else PlanEvidenceStatus.PlayableEvidenceBacked,
      userFacingEligibility = eligibility,
      reason = "test",
      supportProbeIds = supportProbeIds,
      transpositionProofIds = transpositionProofIds,
      themeL1 = hypothesis.themeL1,
      subplanId = hypothesis.subplanId,
      claimCertification = claimCertification
    )

  test("does not build player decision comparison without certified surface input") {
    val surface =
      build(
        moveReviewExplanation =
          Some(MoveReviewExplanation(title = "Move review title", prose = "Short explanation"))
      )

    assertEquals(surface.schema, "chesstory.move_review.player_surface.v2")
    assertEquals(surface.title, Some("Move review title"))
    assertEquals(surface.decisionComparison, None)
  }

  test("uses only certified decision comparison surface input for the player strip") {
    val certified =
      MoveReviewPlayerDecisionComparison(
        kicker = "Decision point",
        gapLabel = Some("220cp"),
        chosenSan = Some("h4"),
        engineSan = Some("g4"),
        comparedSan = None,
        deferredSan = Some("raw deferred should be stripped later"),
        secondaryText = Some("The checked line reaches an exchange sequence after Bxc6, so the decision is about which structure remains."),
        chosenMatchesBest = false
      )

    val surface = build(decisionComparisonSurface = Some(certified))

    assertEquals(surface.decisionComparison, Some(certified))
  }

  test("builds a player decision strip only from surface-candidate line consequence") {
    val evidence = surfaceLineEvidence()
    val digest =
      DecisionComparisonDigest(
        chosenMove = Some("h4"),
        engineBestMove = Some("g4"),
        engineBestPv = evidence.sanMoves,
        cpLossVsChosen = Some(220),
        evidence = Some(evidence.playerSentence),
        chosenMatchesBest = false
      )

    val surface =
      MoveReviewPlayerPayloadBuilder
        .decisionComparisonSurface(Some(digest), Some(evidence))
        .getOrElse(fail("missing decision surface"))

    assertEquals(surface.kicker, "Decision point")
    assertEquals(surface.gapLabel, Some("220cp"))
    assertEquals(surface.chosenSan, Some("h4"))
    assertEquals(surface.engineSan, Some("g4"))
    assert(surface.secondaryText.exists(_.contains("exchange sequence")), clue(surface))
  }

  test("does not surface diagnostic-only line consequence") {
    val evidence = surfaceLineEvidence()
    val digest =
      DecisionComparisonDigest(
        chosenMove = Some("h4"),
        engineBestMove = Some("g4"),
        cpLossVsChosen = Some(220),
        chosenMatchesBest = false
      )
    val diagnostic = evidence.copy(
      release = LineConsequenceRelease.DiagnosticOnly,
      rejectReasons = List("line_consequence:engine_only")
    )

    assertEquals(MoveReviewPlayerPayloadBuilder.decisionComparisonSurface(Some(digest), Some(diagnostic)), None)
  }

  test("does not surface bare small cp gaps without exact comparison or practical alternative") {
    val digest =
      DecisionComparisonDigest(
        chosenMove = Some("h4"),
        engineBestMove = Some("g4"),
        cpLossVsChosen = Some(30),
        chosenMatchesBest = false
      )

    assertEquals(MoveReviewPlayerPayloadBuilder.decisionComparisonSurface(Some(digest), Some(surfaceLineEvidence())), None)
  }

  test("does not use preview-only line consequence text for the player decision strip") {
    val preview =
      surfaceLineEvidence().copy(
        kind = LineConsequenceKind.PreviewOnly,
        consequence = "This proves this plan.",
        whyItMatters = Some("It wins strategically.")
      )
    val digest =
      DecisionComparisonDigest(
        chosenMove = Some("h4"),
        engineBestMove = Some("g4"),
        cpLossVsChosen = Some(220),
        chosenMatchesBest = false
      )

    assertEquals(MoveReviewPlayerPayloadBuilder.decisionComparisonSurface(Some(digest), Some(preview)), None)
  }

  test("surfaces moderate cp gaps with bounded line consequence wording") {
    val evidence = surfaceLineEvidence()
    val digest =
      DecisionComparisonDigest(
        chosenMove = Some("h4"),
        engineBestMove = Some("g4"),
        cpLossVsChosen = Some(45),
        evidence = Some(evidence.playerSentence),
        chosenMatchesBest = false
      )

    val surface =
      MoveReviewPlayerPayloadBuilder
        .decisionComparisonSurface(Some(digest), Some(evidence))
        .getOrElse(fail("missing moderate-gap decision surface"))

    assertEquals(surface.gapLabel, Some("45cp slight"))
    assert(surface.secondaryText.exists(_.contains("exchange sequence")), clue(surface))
  }

  test("allows same-first-move branch comparison when replayed line diverges later") {
    val evidence = surfaceLineEvidence()
    val digest =
      DecisionComparisonDigest(
        chosenMove = Some("Nf3"),
        engineBestMove = Some("Nf3"),
        comparedMove = Some("Nf3"),
        cpLossVsChosen = Some(45),
        evidence = Some(evidence.playerSentence),
        chosenMatchesBest = true
      )

    val surface =
      MoveReviewPlayerPayloadBuilder
        .decisionComparisonSurface(Some(digest), Some(evidence))
        .getOrElse(fail("missing same-first-move decision surface"))

    assertEquals(surface.chosenSan, Some("Nf3"))
    assertEquals(surface.engineSan, Some("Nf3"))
    assertEquals(surface.comparedSan, Some("Nf3"))
    assert(surface.secondaryText.exists(_.contains("which structure remains")), clue(surface))
  }

  test("non-blocking line consequence diagnostic tags do not close the player strip") {
    val evidence =
      surfaceLineEvidence().copy(rejectReasons = List("line_consequence:low_gap_diagnostic"))
    val digest =
      DecisionComparisonDigest(
        chosenMove = Some("h4"),
        engineBestMove = Some("g4"),
        cpLossVsChosen = Some(220),
        evidence = Some(evidence.playerSentence),
        chosenMatchesBest = false
      )

    val surface =
      MoveReviewPlayerPayloadBuilder
        .decisionComparisonSurface(Some(digest), Some(evidence))
        .getOrElse(fail("missing decision surface with non-blocking diagnostic tag"))

    assertEquals(surface.gapLabel, Some("220cp"))
    assert(!surface.secondaryText.exists(_.contains("line_consequence:")), clue(surface))
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
                    source = "probe"
                  )
                )
            )
          )
      )

    assert(!surface.probeRows.exists(_.source.contains("probe_request")), clue(surface.probeRows))
    assert(surface.probeRows.exists(_.text.contains("certified line note")), clue(surface.probeRows))
  }

  test("malformed ledger lines do not create player probe rows") {
    val rawNote = "raw request purpose should not surface"
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
                    title = "Raw request line",
                    sanMoves = List("Nf3", "Nc6"),
                    note = Some(rawNote),
                    source = "probe_request"
                  )
                ),
              resourceLine =
                Some(
                  MoveReviewLedgerLine(
                    title = "Empty line",
                    sanMoves = Nil,
                    note = Some("note without SAN"),
                    source = "probe"
                  )
                )
            )
          )
      )

    assertEquals(surface.probeRows, Nil)
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

  test("transposition-aligned evaluated plans are promoted without pretending to be probe-backed") {
    val surface =
      build(
        evaluatedPlans =
          List(
            evaluated(
              plan("Transposed d5 pressure", evidenceSources = Nil),
              UserFacingPlanEligibility.TranspositionAligned,
              transpositionProofIds = List("transposition:staticweakness:d5:fixed_pawn"),
              claimCertification =
                ClaimCertification(
                  provenanceClass = PlayerFacingClaimProvenanceClass.TranspositionAligned
                )
            )
          )
      )

    assert(
      surface.summaryRows.exists(row =>
        row.label == "Main plans" && row.text.contains("Transposed d5 pressure")
      ),
      clue(surface.summaryRows)
    )
    assert(!surface.summaryRows.exists(_.label == "Practical plan"), clue(surface.summaryRows))
  }

  test("transposition-aligned evaluated plans require typed transposition provenance") {
    val surface =
      build(
        evaluatedPlans =
          List(
            evaluated(
              plan("Malformed transposition pressure", evidenceSources = Nil),
              UserFacingPlanEligibility.TranspositionAligned,
              transpositionProofIds = List("transposition:staticweakness:d5:fixed_pawn")
            )
          )
      )

    assert(!surface.summaryRows.exists(_.label == "Main plans"), clue(surface.summaryRows))
  }

  test("structural-only evaluated plans create practical plan rows but not main plans") {
    val surface =
      build(
        evaluatedPlans =
          List(
            evaluated(
              plan("Carlsbad pressure"),
              UserFacingPlanEligibility.StructuralOnly
            )
          )
      )

    assert(!surface.summaryRows.exists(_.label == "Main plans"), clue(surface.summaryRows))
    assertEquals(surface.summaryRows.map(_.label), List("Practical plan"))
    assertEquals(
      surface.summaryRows.head.text,
      "The structure points toward Carlsbad pressure as a practical plan."
    )
    assertEquals(
      surface.summaryRows.head.authority,
      Some(MoveReviewSurfaceAuthority(kind = MoveReviewSurfaceAuthority.PracticalPlan))
    )
  }

  test("pv-coupled evaluated plans create practical plan rows but not main plans") {
    val surface =
      build(
        evaluatedPlans =
          List(
            evaluated(
              plan("central pressure"),
              UserFacingPlanEligibility.PvCoupledOnly
            )
          )
      )

    assert(!surface.summaryRows.exists(_.label == "Main plans"), clue(surface.summaryRows))
    assertEquals(surface.summaryRows.map(_.label), List("Practical plan"))
    assertEquals(
      surface.summaryRows.head.text,
      "The checked line keeps central pressure viable as a practical plan."
    )
  }

  test("structural-only evaluated plans create practical advanced detail rows") {
    val surface =
      build(
        evaluatedPlans =
          List(
            evaluated(
              plan(
                "Carlsbad pressure",
                preconditions = List("The queenside pawn chain stays fixed"),
                executionSteps = List("Prepare b4", "Pressure c6")
              ),
              UserFacingPlanEligibility.StructuralOnly
            )
          )
      )

    assertEquals(surface.advancedRows.map(_.label), List("Practical objective", "Practical steps"))
    assert(surface.advancedRows.exists(row => row.text == "The queenside pawn chain stays fixed"))
    assert(surface.advancedRows.exists(row => row.text == "Prepare b4 - Pressure c6"))
    assert(surface.advancedRows.forall(_.tone.contains("practical")), clue(surface.advancedRows))
  }

  test("pv-coupled evaluated plans create practical advanced detail rows") {
    val surface =
      build(
        evaluatedPlans =
          List(
            evaluated(
              plan(
                "central pressure",
                preconditions = List("The e-file remains tense"),
                executionSteps = List("Keep the knight centralized")
              ),
              UserFacingPlanEligibility.PvCoupledOnly
            )
          )
      )

    assertEquals(surface.advancedRows.map(_.label), List("Practical objective", "Practical steps"))
    assert(surface.advancedRows.exists(row => row.text == "The e-file remains tense"))
    assert(surface.advancedRows.exists(row => row.text == "Keep the knight centralized"))
  }

  test("weakness practical plans include dynamic board target detail") {
    val surface =
      build(
        ctx = MoveReviewProseGoldenFixtures.rookPawnMarch.ctx.copy(
          fen = "4k3/8/8/3p4/8/8/8/4K3 w - - 0 1"
        ),
        evaluatedPlans =
          List(
            evaluated(
              plan(
                "Static weakness pressure",
                themeL1 = PlanTaxonomy.PlanTheme.WeaknessFixation.id,
                subplanId = Some(PlanTaxonomy.PlanKind.StaticWeaknessFixation.id)
              ),
              UserFacingPlanEligibility.StructuralOnly
            )
          )
      )

    assertEquals(surface.advancedRows.map(_.label), List("Practical target"))
    assert(surface.advancedRows.exists(_.text.contains("weak isolated queen pawn on d5")), clue(surface.advancedRows))
    assert(surface.advancedRows.forall(_.authority.contains(MoveReviewSurfaceAuthority(kind = MoveReviewSurfaceAuthority.PracticalPlan))))
  }

  test("weakness practical target row is suppressed when the best line liquidates the target") {
    val ctx =
      MoveReviewProseGoldenFixtures.rookPawnMarch.ctx.copy(
        fen = "4k3/8/8/4p3/8/8/8/4K3 w - - 0 1",
        engineEvidence =
          Some(
            EngineEvidence(
              depth = 18,
              variations =
                List(
                  VariationLine(
                    moves = List("e1e2", "e5e4"),
                    scoreCp = 12,
                    depth = 18
                  )
                )
            )
          )
      )
    val surface =
      build(
        ctx = ctx,
        evaluatedPlans =
          List(
            evaluated(
              plan(
                "Static weakness fixation",
                themeL1 = PlanTaxonomy.PlanTheme.WeaknessFixation.id,
                subplanId = Some(PlanTaxonomy.PlanKind.StaticWeaknessFixation.id),
                preconditions = List("Pressure the weak pawn"),
                executionSteps = List("Improve toward the target")
              ),
              UserFacingPlanEligibility.StructuralOnly
            )
          )
      )

    assert(!surface.advancedRows.exists(_.label == "Practical target"), clue(surface.advancedRows))
    assert(surface.advancedRows.exists(_.label == "Practical objective"), clue(surface.advancedRows))
  }

  test("weakness practical target row is suppressed when the best line horizon is too short") {
    val ctx =
      MoveReviewProseGoldenFixtures.rookPawnMarch.ctx.copy(
        fen = "4k3/8/8/4p3/8/8/8/4K3 w - - 0 1",
        engineEvidence =
          Some(
            EngineEvidence(
              depth = 18,
              variations =
                List(
                  VariationLine(
                    moves = List("e1d1", "e8d8", "d1e1", "d8e8"),
                    scoreCp = 12,
                    depth = 18
                  )
                )
            )
          )
      )
    val surface =
      build(
        ctx = ctx,
        evaluatedPlans =
          List(
            evaluated(
              plan(
                "Static weakness fixation",
                themeL1 = PlanTaxonomy.PlanTheme.WeaknessFixation.id,
                subplanId = Some(PlanTaxonomy.PlanKind.StaticWeaknessFixation.id),
                preconditions = List("Pressure the weak pawn")
              ),
              UserFacingPlanEligibility.StructuralOnly
            )
          )
      )

    assert(!surface.advancedRows.exists(_.label == "Practical target"), clue(surface.advancedRows))
  }

  test("decision comparison carries best-vs-chosen target contrast metadata") {
    val ctx =
      MoveReviewProseGoldenFixtures.rookPawnMarch.ctx.copy(
        fen = "4k3/8/8/3pp3/2N2N2/8/8/4K3 w - - 0 1",
        playedMove = Some("f4d5"),
        engineEvidence =
          Some(
            EngineEvidence(
              depth = 18,
              variations =
                List(
                  VariationLine(moves = List("c4e5"), scoreCp = 80, depth = 18),
                  VariationLine(moves = List("f4d5"), scoreCp = 20, depth = 18)
                )
            )
          )
      )

    val surface =
      build(
        ctx = ctx,
        decisionComparisonSurface =
          Some(
            MoveReviewPlayerDecisionComparison(
              kicker = "Decision point",
              chosenSan = Some("Nxd5"),
              engineSan = Some("Nxe5"),
              secondaryText = Some("The checked line changes which pawn remains."),
              chosenMatchesBest = false
            )
          )
      )

    assertEquals(
      surface.decisionComparison.flatMap(_.targetComparison),
      Some(
        MoveReviewDecisionTargetComparison(
          chosenTarget = "e5",
          chosenTargetKind = "isolated_pawn",
          bestTarget = "d5",
          bestTargetKind = "iqp"
        )
      )
    )
  }

  test("practical advanced rows are skipped when practical plans have no details") {
    val surface =
      build(
        evaluatedPlans =
          List(
            evaluated(
              plan("Carlsbad pressure"),
              UserFacingPlanEligibility.StructuralOnly
            )
          )
      )

    assertEquals(surface.advancedRows, Nil)
  }

  test("probe-backed plans do not duplicate as practical plan rows") {
    val surface =
      build(
        evaluatedPlans =
          List(
            evaluated(
              plan("Validated rook lift"),
              UserFacingPlanEligibility.ProbeBacked,
              supportProbeIds = List("probe_1")
            )
          )
      )

    assertEquals(surface.summaryRows.map(_.label), List("Main plans"))
    assert(!surface.summaryRows.exists(row => row.label == "Practical plan"), clue(surface.summaryRows))
  }

  test("probe-backed plans do not duplicate practical advanced detail rows") {
    val promoted =
      plan(
        "Validated rook lift",
        preconditions = List("The third rank is clear"),
        executionSteps = List("Lift the rook", "Swing to g3")
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

    assertEquals(surface.advancedRows.map(_.label), List("Execution", "Objective"))
    assert(!surface.advancedRows.exists(_.label.startsWith("Practical")), clue(surface.advancedRows))
  }

  test("practical advanced rows drop sibling plans with the same promoted theme") {
    val promoted =
      plan(
        "Validated kingside clamp",
        preconditions = List("The kingside dark squares are weak"),
        executionSteps = List("h4-h5", "h5-h6"),
        themeL1 = PlanTaxonomy.PlanTheme.SpaceClamp.id
      )
    val practical =
      plan(
        "Kingside file pressure",
        preconditions = List("The h-file can be opened"),
        executionSteps = List("h4-h5"),
        themeL1 = PlanTaxonomy.PlanTheme.SpaceClamp.id
      )
    val surface =
      build(
        evaluatedPlans =
          List(
            evaluated(promoted, UserFacingPlanEligibility.ProbeBacked, supportProbeIds = List("probe_1")),
            evaluated(practical, UserFacingPlanEligibility.StructuralOnly)
          )
      )

    assert(surface.summaryRows.exists(_.label == "Practical plan"), clue(surface.summaryRows))
    assertEquals(surface.advancedRows.map(_.label), List("Execution", "Objective"))
    assert(!surface.advancedRows.exists(_.label.startsWith("Practical")), clue(surface.advancedRows))
  }

  test("practical advanced rows drop sibling plans with overlapping execution") {
    val promoted =
      plan(
        "Validated rook lift",
        preconditions = List("The rook can swing across"),
        executionSteps = List("h4-h5-h6"),
        themeL1 = PlanTaxonomy.PlanTheme.PieceRedeployment.id
      )
    val practical =
      plan(
        "Kingside target fixation",
        preconditions = List("The h-pawn remains a hook"),
        executionSteps = List("h4-h5"),
        themeL1 = PlanTaxonomy.PlanTheme.WeaknessFixation.id
      )
    val surface =
      build(
        evaluatedPlans =
          List(
            evaluated(promoted, UserFacingPlanEligibility.ProbeBacked, supportProbeIds = List("probe_1")),
            evaluated(practical, UserFacingPlanEligibility.StructuralOnly)
          )
      )

    assertEquals(surface.advancedRows.map(_.label), List("Execution", "Objective"))
    assert(!surface.advancedRows.exists(_.label.startsWith("Practical")), clue(surface.advancedRows))
  }

  test("practical advanced rows remain when promoted plan is not a sibling") {
    val promoted =
      plan(
        "Validated rook lift",
        preconditions = List("The rook can swing across"),
        executionSteps = List("h4-h5"),
        themeL1 = PlanTaxonomy.PlanTheme.PieceRedeployment.id
      )
    val practical =
      plan(
        "Queenside minority pressure",
        preconditions = List("The c-pawn can become fixed"),
        executionSteps = List("b4-b5", "Pressure c6", "Keep queenside tension"),
        themeL1 = PlanTaxonomy.PlanTheme.WeaknessFixation.id
      )
    val surface =
      build(
        evaluatedPlans =
          List(
            evaluated(promoted, UserFacingPlanEligibility.ProbeBacked, supportProbeIds = List("probe_1")),
            evaluated(practical, UserFacingPlanEligibility.StructuralOnly)
          )
      )

    assertEquals(
      surface.advancedRows.map(_.label),
      List("Execution", "Objective", "Practical target", "Practical objective", "Practical steps")
    )
    assert(surface.advancedRows.exists(row => row.text == "b4-b5 - Pressure c6"), clue(surface.advancedRows))
  }

  test("supported-local rows append to summary rows without leaking source metadata") {
    val supported =
      MoveReviewPlayerSurfaceRow(
        label = "Counterplay break",
        text = "A key idea is that this move stops the d5 break.",
        source = Some("counterplay_axis_suppression"),
        refSans = List("exd5"),
        authority = Some(MoveReviewSurfaceAuthority(kind = MoveReviewSurfaceAuthority.CounterplayBreak, token = Some("d5")))
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
    assertEquals(
      surface.summaryRows.find(_.label == "Counterplay break").flatMap(_.authority),
      Some(MoveReviewSurfaceAuthority(kind = MoveReviewSurfaceAuthority.CounterplayBreak, token = Some("d5")))
    )
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

  private def surfaceLineEvidence(): LineConsequenceEvidence =
    LineConsequenceEvidence(
      lineId = Some("exchange"),
      sanMoves = List("Nf3", "Nc6", "Bb5", "a6", "Bxc6", "dxc6"),
      uciMoves = List("g1f3", "b8c6", "f1b5", "a7a6", "b5c6", "d7c6"),
      scoreCp = Some(42),
      mate = None,
      depth = Some(20),
      windowPly = 6,
      kind = LineConsequenceKind.ExchangeSequence,
      triggerSan = Some("Bxc6"),
      consequence = "The checked line reaches an exchange sequence after Bxc6.",
      whyItMatters = Some("The decision is about which structure remains."),
      release = LineConsequenceRelease.SurfaceCandidate,
      rejectReasons = Nil
    )
