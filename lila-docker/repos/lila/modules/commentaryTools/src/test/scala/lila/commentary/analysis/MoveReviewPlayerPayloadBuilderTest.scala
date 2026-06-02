package lila.commentary.analysis

import munit.FunSuite
import _root_.chess.{ Color, Square, White }
import lila.commentary.*
import lila.commentary.model.*
import lila.commentary.model.authoring.{ PlanHypothesis, PlanViability }
import lila.commentary.model.authoring.{ AuthorQuestion, AuthorQuestionKind }
import lila.commentary.model.strategic.{ EngineEvidence, VariationLine, WeakComplex }
import lila.commentary.analysis.PlanEvidenceEvaluator.{ ClaimCertification, EvaluatedPlan, PlanEvidenceStatus, UserFacingPlanEligibility }
import lila.commentary.analysis.semantic.RelationObservationCatalog
import lila.commentary.analysis.semantic.StrategicObservationIds.EvidenceRef

final class MoveReviewPlayerPayloadBuilderTest extends FunSuite:

  private val emptyAuthoringSurface =
    AuthoringEvidenceSurface(questions = Nil, evidence = Nil, headline = None)

  private val InitialFen =
    "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1"

  private def build(
      ctx: NarrativeContext = MoveReviewProseGoldenFixtures.rookPawnMarch.ctx,
      moveReviewExplanation: Option[MoveReviewExplanation] = None,
      moveReviewLedger: Option[MoveReviewStrategicLedger] = None,
      evaluatedPlans: List[EvaluatedPlan] = Nil,
      authoringSurface: AuthoringEvidenceSurface = emptyAuthoringSurface,
      supportedLocalRows: List[MoveReviewPlayerSurfaceRow] = Nil,
      decisionComparisonSurface: Option[MoveReviewPlayerDecisionComparison] = None,
      strategyPack: Option[StrategyPack] = None
  ): MoveReviewPlayerSurface =
    MoveReviewPlayerPayloadBuilder.build(
      ctx = ctx,
      moveReviewExplanation = moveReviewExplanation,
      moveReviewLedger = moveReviewLedger,
      refs = None,
      evaluatedPlans = evaluatedPlans,
      authoringSurface = authoringSurface,
      supportedLocalRows = supportedLocalRows,
      decisionComparisonSurface = decisionComparisonSurface,
      strategyPack = strategyPack
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

  private def relationOnlyData(fen: String, playedMove: String): ExtendedAnalysisData =
    ExtendedAnalysisData(
      fen = fen,
      nature = PositionNature(NatureType.Dynamic, 0.6, 0.4, "relation test"),
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
      prevMove = Some(playedMove),
      ply = 1,
      evalCp = 42,
      isWhiteToMove = true
    )

  private def relationOnlyContext(fen: String, playedMove: String): NarrativeContext =
    NarrativeContext(
      fen = fen,
      header = ContextHeader("Middlegame", "Normal", "StyleChoice", "Medium", "ExplainPlan"),
      ply = 1,
      playedMove = Some(playedMove),
      playedSan = Some("Be4"),
      summary = NarrativeSummary("Relation test", None, "StyleChoice", "Maintain", "+0.4"),
      threats = ThreatTable(Nil, Nil),
      pawnPlay = PawnPlayTable(false, None, "Low", "Maintain", "none", "Background", None, false, "quiet"),
      plans = PlanTable(top5 = Nil, suppressed = Nil),
      delta = None,
      phase = PhaseContext("Middlegame", "relation test"),
      candidates = Nil,
      engineEvidence = Some(
        EngineEvidence(
          depth = 18,
          variations = List(VariationLine(moves = List(playedMove), scoreCp = 42))
        )
      )
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

  test("promoted plan detail rows use player-facing text instead of probe diagnostics") {
    val surface =
      build(
        evaluatedPlans =
          List(
            evaluated(
              plan(
                "Validated central break",
                executionSteps = List(
                  "test e4e5 as the concrete pawn break",
                  "use probe replies to verify whether the opened structure favors this side"
                ),
                preconditions = List("candidate move e4e5"),
                themeL1 = PlanTaxonomy.PlanTheme.PawnBreakPreparation.id,
                subplanId = Some(PlanTaxonomy.PlanKind.CentralBreakTiming.id)
              ),
              UserFacingPlanEligibility.ProbeBacked,
              supportProbeIds = List("probe_1")
            )
          )
      )
    val rendered = surface.advancedRows.map(row => s"${row.label}: ${row.text}").mkString(" ")

    assertEquals(surface.advancedRows.map(_.label), List("Execution", "Objective"))
    assert(surface.advancedRows.exists(row => row.label == "Execution" && row.text == "Validated central break"), clue(surface.advancedRows))
    assert(
      surface.advancedRows.exists(row => row.label == "Objective" && row.text == "prepare a central break at the right timing"),
      clue(surface.advancedRows)
    )
    assert(!rendered.toLowerCase.contains("probe"), clue(rendered))
    assert(!rendered.toLowerCase.contains("test e4e5"), clue(rendered))
    assert(!rendered.toLowerCase.contains("candidate move"), clue(rendered))
  }

  test("helper-generated restriction plans reach MoveReview rows with natural why and execution text") {
    val fen = "4k3/8/8/2p5/3P4/8/8/4K3 w - - 0 1"
    val restrictionPlan =
      RestrictionPlanEvidence
        .planHypotheses(fen, _root_.chess.White)
        .find(_.subplanId.contains(PlanTaxonomy.PlanKind.BreakPrevention.id))
        .getOrElse(fail("missing break-prevention plan"))
    val surface =
      build(
        evaluatedPlans =
          List(
            evaluated(
              restrictionPlan,
              UserFacingPlanEligibility.ProbeBacked,
              supportProbeIds = List("probe_1")
            )
          )
      )
    val rendered = surface.advancedRows.map(row => s"${row.label}: ${row.text}").mkString(" ").toLowerCase

    assert(rendered.contains("cuts opponent break choices"), clue(surface.advancedRows))
    assert(rendered.contains("before the counter-break becomes easy"), clue(surface.advancedRows))
    assert(!rendered.contains("candidate move"), clue(rendered))
    assert(!rendered.contains("opponent_breaks:"), clue(rendered))
    assert(!rendered.contains("central_control_gain"), clue(rendered))
  }

  test("helper-generated piece redeployment plans reach MoveReview rows with natural why and execution text") {
    val fen = "6k1/8/2p5/8/8/8/8/4R1K1 w - - 0 1"
    val redeploymentPlan =
      PieceRedeploymentEvidence
        .planHypotheses(fen, White)
        .find(_.subplanId.contains(PlanTaxonomy.PlanKind.OpenFilePressure.id))
        .getOrElse(fail("missing open-file pressure plan"))
    val surface =
      build(
        evaluatedPlans =
          List(
            evaluated(
              redeploymentPlan,
              UserFacingPlanEligibility.ProbeBacked,
              supportProbeIds = List("probe_1")
            )
          )
      )
    val rendered = surface.advancedRows.map(row => s"${row.label}: ${row.text}").mkString(" ").toLowerCase

    assert(rendered.contains("place the rook on c1"), clue(surface.advancedRows))
    assert(rendered.contains("c-file"), clue(surface.advancedRows))
    assert(rendered.contains("against c6"), clue(surface.advancedRows))
    assert(!rendered.contains("open_or_semi_open_file"), clue(rendered))
    assert(!rendered.contains("file_targets:"), clue(rendered))
    assert(!rendered.contains("mobility_gain:"), clue(rendered))
  }

  test("helper-generated pawn-break plans reach MoveReview rows with natural why and execution text") {
    val fen = "4k3/8/8/3p4/4P3/8/8/4K3 w - - 0 1"
    val pawnBreakPlan =
      PawnBreakEvidence
        .planHypotheses(fen, White)
        .find(_.subplanId.contains(PlanTaxonomy.PlanKind.CentralBreakTiming.id))
        .getOrElse(fail("missing central-break plan"))
    val surface =
      build(
        evaluatedPlans =
          List(
            evaluated(
              pawnBreakPlan,
              UserFacingPlanEligibility.ProbeBacked,
              supportProbeIds = List("probe_1")
            )
          )
      )
    val rendered = surface.advancedRows.map(row => s"${row.label}: ${row.text}").mkString(" ").toLowerCase

    assert(rendered.contains("play e4-d5 as the concrete central break"), clue(surface.advancedRows))
    assert(rendered.contains("e4-d5 is a concrete central pawn break"), clue(surface.advancedRows))
    assert(!rendered.contains("pawn break move"), clue(rendered))
    assert(!rendered.contains("e4d5"), clue(rendered))
    assert(!rendered.contains("pawn_break"), clue(rendered))
    assert(!rendered.contains("captures_tension_pawn"), clue(rendered))
  }

  test("helper-generated weakness plans reach MoveReview rows with natural why and execution text") {
    val fen = "4k3/8/4p3/3p2N1/3P4/8/8/4K3 w - - 0 1"
    val weaknessPlan =
      WeaknessFixationEvidence
        .planHypotheses(fen, White)
        .find(_.subplanId.contains(PlanTaxonomy.PlanKind.BackwardPawnTargeting.id))
        .getOrElse(fail("missing backward-pawn plan"))
    val surface =
      build(
        evaluatedPlans =
          List(
            evaluated(
              weaknessPlan,
              UserFacingPlanEligibility.ProbeBacked,
              supportProbeIds = List("probe_1")
            )
          )
      )
    val rendered = surface.advancedRows.map(row => s"${row.label}: ${row.text}").mkString(" ").toLowerCase

    assert(rendered.contains("take the target on e6 with g5-e6"), clue(surface.advancedRows))
    assert(rendered.contains("e6 is a backward pawn target"), clue(surface.advancedRows))
    assert(!rendered.contains("candidate move"), clue(rendered))
    assert(!rendered.contains("g5e6"), clue(rendered))
    assert(!rendered.contains("weakness_target:"), clue(rendered))
    assert(!rendered.contains("pressure_delta:"), clue(rendered))
  }

  test("helper-generated flank infrastructure plans reach MoveReview rows with natural why and execution text") {
    val fen = "4k3/8/8/7p/8/7P/8/6K1 w - - 0 1"
    val flankPlan =
      FlankInfrastructureEvidence
        .planHypotheses(fen, White)
        .find(_.subplanId.contains(PlanTaxonomy.PlanKind.HookCreation.id))
        .getOrElse(fail("missing hook-creation plan"))
    val surface =
      build(
        evaluatedPlans =
          List(
            evaluated(
              flankPlan,
              UserFacingPlanEligibility.ProbeBacked,
              supportProbeIds = List("probe_1")
            )
          )
      )
    val rendered = surface.advancedRows.map(row => s"${row.label}: ${row.text}").mkString(" ").toLowerCase

    assert(rendered.contains("play h3-h4 while the center stays controlled"), clue(surface.advancedRows))
    assert(rendered.contains("h3-h4 creates contact on the h-file"), clue(surface.advancedRows))
    assert(!rendered.contains("flank pawn move"), clue(rendered))
    assert(!rendered.contains("h3h4"), clue(rendered))
    assert(!rendered.contains("creates_hook_contact"), clue(rendered))
    assert(!rendered.contains("king_flank_aligned"), clue(rendered))
  }

  test("practical plan detail rows also suppress probe diagnostics") {
    val surface =
      build(
        evaluatedPlans =
          List(
            evaluated(
              plan(
                "Practical central break",
                executionSteps = List("test e4e5 as the concrete pawn break"),
                preconditions = List("candidate move e4e5"),
                themeL1 = PlanTaxonomy.PlanTheme.PawnBreakPreparation.id,
                subplanId = Some(PlanTaxonomy.PlanKind.CentralBreakTiming.id)
              ),
              UserFacingPlanEligibility.StructuralOnly
            )
          )
      )
    val rendered = surface.advancedRows.map(row => s"${row.label}: ${row.text}").mkString(" ")

    assertEquals(surface.advancedRows.map(_.label), List("Practical objective", "Practical steps"))
    assert(surface.advancedRows.exists(row => row.text == "prepare a central break at the right timing"), clue(surface.advancedRows))
    assert(surface.advancedRows.exists(row => row.text == "Practical central break"), clue(surface.advancedRows))
    assert(!rendered.toLowerCase.contains("probe"), clue(rendered))
    assert(!rendered.toLowerCase.contains("test e4e5"), clue(rendered))
    assert(!rendered.toLowerCase.contains("candidate move"), clue(rendered))
  }

  test("promoted plan status row explains stable flow from typed experiment state") {
    val promoted =
      plan(
        "Validated central break",
        executionSteps = List("e4-e5"),
        preconditions = List("The center can be opened"),
        themeL1 = PlanTaxonomy.PlanTheme.PawnBreakPreparation.id,
        subplanId = Some(PlanTaxonomy.PlanKind.CentralBreakTiming.id)
      )
    val ctx =
      MoveReviewProseGoldenFixtures.rookPawnMarch.ctx.copy(
        strategicPlanExperiments =
          List(
            StrategicPlanExperiment(
              planId = promoted.planId,
              themeL1 = PlanTaxonomy.PlanTheme.PawnBreakPreparation.id,
              subplanId = Some(PlanTaxonomy.PlanKind.CentralBreakTiming.id),
              evidenceTier = "evidence_backed",
              supportProbeCount = 1,
              bestReplyStable = true,
              futureSnapshotAligned = true,
              experimentConfidence = 0.82
            )
          )
      )
    val surface =
      build(
        ctx = ctx,
        evaluatedPlans =
          List(
            evaluated(
              promoted,
              UserFacingPlanEligibility.ProbeBacked,
              supportProbeIds = List("probe_1")
            )
          )
      )
    val status =
      surface.advancedRows
        .find(_.label == "Plan status")
        .getOrElse(fail(s"missing status row: ${surface.advancedRows}"))

    assertEquals(surface.advancedRows.map(_.label), List("Execution", "Objective", "Plan status"))
    assert(status.text.contains("Checked replies keep the route stable"), clue(status))
    assert(status.text.contains("later position still points to the same plan"), clue(status))
    assert(!status.text.toLowerCase.contains("probe"), clue(status))
  }

  test("plan status row does not attach flow state from another plan by broad theme or subplan") {
    val promoted =
      plan(
        "Validated central break",
        executionSteps = List("e4-e5"),
        preconditions = List("The center can be opened"),
        themeL1 = PlanTaxonomy.PlanTheme.PawnBreakPreparation.id,
        subplanId = Some(PlanTaxonomy.PlanKind.CentralBreakTiming.id)
      )
    val ctx =
      MoveReviewProseGoldenFixtures.rookPawnMarch.ctx.copy(
        strategicPlanExperiments =
          List(
            StrategicPlanExperiment(
              planId = "other_central_break",
              themeL1 = PlanTaxonomy.PlanTheme.PawnBreakPreparation.id,
              subplanId = Some(PlanTaxonomy.PlanKind.CentralBreakTiming.id),
              evidenceTier = "evidence_backed",
              supportProbeCount = 1,
              bestReplyStable = true,
              futureSnapshotAligned = true,
              experimentConfidence = 0.82
            )
          )
      )
    val surface =
      build(
        ctx = ctx,
        evaluatedPlans =
          List(
            evaluated(
              promoted,
              UserFacingPlanEligibility.ProbeBacked,
              supportProbeIds = List("probe_1")
            )
          )
      )

    assert(!surface.advancedRows.exists(_.label == "Plan status"), clue(surface.advancedRows))
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

  test("weakness practical target requires typed weakness family instead of plan-name inference") {
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
                themeL1 = PlanTaxonomy.PlanTheme.Unknown.id
              ),
              UserFacingPlanEligibility.StructuralOnly
            )
          )
      )

    assert(!surface.advancedRows.exists(_.label == "Practical target"), clue(surface.advancedRows))
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

  test("practical details are not suppressed by promoted plan-name theme inference") {
    val promoted =
      plan(
        "rook file transfer",
        themeL1 = PlanTaxonomy.PlanTheme.Unknown.id
      )
    val practical =
      plan(
        "bishop reanchor",
        preconditions = List("The light squares need cover"),
        executionSteps = List("Improve the bishop before opening the file"),
        themeL1 = PlanTaxonomy.PlanTheme.Unknown.id
      )
    val surface =
      build(
        evaluatedPlans =
          List(
            evaluated(
              promoted,
              UserFacingPlanEligibility.ProbeBacked,
              supportProbeIds = List("probe_1")
            ),
            evaluated(
              practical,
              UserFacingPlanEligibility.StructuralOnly
            )
          )
      )

    assert(surface.advancedRows.exists(row => row.label == "Practical objective"), clue(surface.advancedRows))
    assert(surface.advancedRows.exists(row => row.label == "Practical steps"), clue(surface.advancedRows))
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

  test("opening family row carries bounded public opening book metadata") {
    val fen =
      NarrativeUtils.uciListToFen(InitialFen, List("d2d4", "d7d5", "c2c4"))
    val openingRef =
      OpeningReference(
        eco = Some("D06"),
        name = Some("Queen's Gambit"),
        totalGames = 12345,
        topMoves =
          List(
            ExplorerMove("e7e6", "e6", 6000, 2200, 1600, 2200, 2450),
            ExplorerMove("g8f6", "Nf6", 4000, 1500, 1100, 1400, 2440),
            ExplorerMove("bad", "raw note", 1, 0, 0, 1, 1200)
          ),
        sampleGames = Nil
      )
    val surface =
      build(
        ctx =
          MoveReviewProseGoldenFixtures.rookPawnMarch.ctx.copy(
            fen = fen,
            header = ContextHeader("Opening", "Normal", "NarrowChoice", "Medium", "ExplainPlan"),
            phase = PhaseContext("Opening", "Opening structure"),
            ply = 3,
            openingData = Some(openingRef)
          )
      )

    val authority =
      surface.summaryRows
        .find(_.label == "Opening family")
        .flatMap(_.authority)
        .getOrElse(fail(s"missing opening-family authority: ${surface.summaryRows}"))

    assertEquals(authority.kind, MoveReviewSurfaceAuthority.OpeningFamily)
    assertEquals(authority.openingFamily, Some("queens_gambit"))
    assertEquals(
      authority.openingBook,
      Some(MoveReviewOpeningBookMetadata(eco = Some("D06"), totalGames = Some(12345), topMoves = List("e6", "Nf6")))
    )
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

  test("strategic relation evidence appears as bounded advanced metadata") {
    val relationIdea =
      StrategyIdeaSignal(
        ideaId = "idea_1",
        ownerSide = "white",
        kind = StrategicIdeaKind.LineOccupation,
        group = "line_occupation",
        readiness = StrategicIdeaReadiness.Build,
        focusSquares = List("e4", "f5", "g6"),
        confidence = 0.72,
        evidenceRefs = List("source:xray_relation", "xray_semantic", "blocker:f5"),
        relationKind = Some(MoveReviewExchangeAnalyzer.RelationKind.XRay),
        relationFocusSquares = List("e4", "f5", "g6"),
        relationSupport =
          Some(
            StrategyRelationSupport(
              relationKind = MoveReviewExchangeAnalyzer.RelationKind.XRay,
              focusSquares = List("e4", "f5", "g6"),
              targetSquare = Some("g6"),
              attackerSquare = Some("e4"),
              blockerSquare = Some("f5")
            )
          )
      )
    val surface =
      build(
        strategyPack =
          Some(
            StrategyPack(
              sideToMove = "white",
              strategicIdeas = List(relationIdea)
            )
          )
      )

    val row =
      surface.advancedRows
        .find(_.authority.exists(_.kind == MoveReviewSurfaceAuthority.StrategicRelation))
        .getOrElse(fail(s"missing strategic relation row: ${surface.advancedRows}"))

    assertEquals(row.label, "Line relation")
    assertEquals(row.tone, Some("relation"))
    assertEquals(row.source, None)
    assertEquals(
      row.authority,
      Some(
        MoveReviewSurfaceAuthority(
          kind = MoveReviewSurfaceAuthority.StrategicRelation,
          token = Some("xray"),
          target = Some("g6")
        )
      )
    )
    assert(row.text.contains("x-ray evidence"), clue(row))
  }

  test("strategic relation row requires typed relation support instead of evidenceRef strings") {
    val xray = RelationObservationCatalog.descriptorForKind(MoveReviewExchangeAnalyzer.RelationKind.XRay).get
    val evidenceOnlyIdea =
      StrategyIdeaSignal(
        ideaId = "idea_1",
        ownerSide = "white",
        kind = StrategicIdeaKind.LineOccupation,
        group = "line_occupation",
        readiness = StrategicIdeaReadiness.Build,
        focusSquares = List("e4", "f5", "g6"),
        confidence = 0.72,
        evidenceRefs = xray.wireEvidenceRefs,
        targetSquare = Some("g6"),
        relationKind = Some(xray.relationKind),
        relationFocusSquares = List("e4", "f5", "g6"),
        relationSupport = None
      )
    val typedCarrierIdea =
      evidenceOnlyIdea.copy(
        ideaId = "idea_2",
        evidenceRefs = Nil,
        relationSupport =
          Some(
            StrategyRelationSupport(
              relationKind = xray.relationKind,
              focusSquares = List("e4", "f5", "g6"),
              targetSquare = Some("g6"),
              attackerSquare = Some("e4"),
              blockerSquare = Some("f5")
            )
          )
      )

    val evidenceOnlySurface =
      build(strategyPack = Some(StrategyPack(sideToMove = "white", strategicIdeas = List(evidenceOnlyIdea))))
    val typedCarrierSurface =
      build(strategyPack = Some(StrategyPack(sideToMove = "white", strategicIdeas = List(typedCarrierIdea))))

    assert(
      !evidenceOnlySurface.advancedRows.exists(_.authority.exists(_.kind == MoveReviewSurfaceAuthority.StrategicRelation)),
      clue(evidenceOnlySurface.advancedRows)
    )
    assert(
      typedCarrierSurface.advancedRows.exists(_.authority.exists(_.kind == MoveReviewSurfaceAuthority.StrategicRelation)),
      clue(typedCarrierSurface.advancedRows)
    )
  }

  test("relation surface formatter fails closed without typed relation support") {
    val xray = RelationObservationCatalog.descriptorForKind(MoveReviewExchangeAnalyzer.RelationKind.XRay).get
    val evidenceOnlyIdea =
      StrategyIdeaSignal(
        ideaId = "idea_1",
        ownerSide = "white",
        kind = StrategicIdeaKind.LineOccupation,
        group = "line_occupation",
        readiness = StrategicIdeaReadiness.Build,
        focusSquares = List("e4", "f5", "g6"),
        confidence = 0.72,
        evidenceRefs = xray.wireEvidenceRefs,
        targetSquare = Some("g6"),
        relationKind = Some(xray.relationKind),
        relationFocusSquares = List("e4", "f5", "g6"),
        relationSupport = None
      )

    assertEquals(
      RelationSurfaceText.surfaceRowText(
        descriptor = xray,
        idea = evidenceOnlyIdea,
        focusSquares = List("e4", "f5", "g6"),
        targetSquare = Some("g6")
      ),
      None
    )
  }

  test("strategic relation support creates a core move-review explanation cue") {
    val fork = RelationObservationCatalog.descriptorForKind(MoveReviewExchangeAnalyzer.RelationKind.Fork).get
    val relationIdea =
      StrategyIdeaSignal(
        ideaId = "idea_fork",
        ownerSide = "white",
        kind = fork.ideaKind,
        group = StrategicIdeaGroup.InteractionAndTransformation,
        readiness = fork.readiness,
        focusSquares = List("f5", "e7", "h4"),
        confidence = fork.confidence,
        evidenceRefs = fork.wireEvidenceRefs,
        targetSquare = Some("h4"),
        relationKind = Some(fork.relationKind),
        relationFocusSquares = List("f5", "e7", "h4"),
        relationSupport =
          Some(
            StrategyRelationSupport(
              relationKind = fork.relationKind,
              focusSquares = List("f5", "e7", "h4"),
              targetSquare = Some("h4"),
              attackerSquare = Some("f5"),
              attackerRole = Some("knight"),
              targetSquares = List("e7", "h4"),
              targetRoles = List("rook", "queen")
            )
          )
      )

    val surface =
      build(
        strategyPack =
          Some(
            StrategyPack(
              sideToMove = "white",
              strategicIdeas = List(relationIdea)
            )
          )
      )
    val cue =
      surface.summaryRows
        .find(_.authority.exists(authority =>
          authority.kind == MoveReviewSurfaceAuthority.StrategicRelation &&
            authority.token.contains(MoveReviewExchangeAnalyzer.RelationKind.Fork)
        ))
        .getOrElse(fail(s"missing relation summary cue: ${surface.summaryRows}"))

    assertEquals(cue.label, "Tactical relation")
    assertEquals(cue.tone, Some("relation"))
    assertEquals(cue.source, None)
    assert(cue.text.contains("Why it works: knight on f5 forks the rook on e7 and the queen on h4."), clue(cue))
    assert(cue.text.contains("Next check: whether the rook on e7 and the queen on h4 stay loose after the reply."), clue(cue))
    assertEquals(
      cue.authority,
      Some(
        MoveReviewSurfaceAuthority(
          kind = MoveReviewSurfaceAuthority.StrategicRelation,
          token = Some(MoveReviewExchangeAnalyzer.RelationKind.Fork),
          target = Some("h4")
        )
      )
    )

  }

  test("strategic relation explanation cue leads the summary surface when present") {
    val fork = RelationObservationCatalog.descriptorForKind(MoveReviewExchangeAnalyzer.RelationKind.Fork).get
    val relationIdea =
      StrategyIdeaSignal(
        ideaId = "idea_fork",
        ownerSide = "white",
        kind = fork.ideaKind,
        group = StrategicIdeaGroup.InteractionAndTransformation,
        readiness = fork.readiness,
        focusSquares = List("f5", "e7", "h4"),
        confidence = fork.confidence,
        evidenceRefs = fork.wireEvidenceRefs,
        targetSquare = Some("h4"),
        relationKind = Some(fork.relationKind),
        relationFocusSquares = List("f5", "e7", "h4"),
        relationSupport =
          Some(
            StrategyRelationSupport(
              relationKind = fork.relationKind,
              focusSquares = List("f5", "e7", "h4"),
              targetSquare = Some("h4"),
              attackerSquare = Some("f5"),
              attackerRole = Some("knight"),
              targetSquares = List("e7", "h4"),
              targetRoles = List("rook", "queen")
            )
          )
      )

    val surface =
      build(
        evaluatedPlans =
          List(
            evaluated(
              plan("Validated rook lift", evidenceSources = Nil),
              UserFacingPlanEligibility.ProbeBacked,
              supportProbeIds = List("probe_1")
            ),
            evaluated(
              plan("Carlsbad pressure"),
              UserFacingPlanEligibility.StructuralOnly
            )
          ),
        strategyPack = Some(StrategyPack(sideToMove = "white", strategicIdeas = List(relationIdea)))
      )

    assertEquals(
      surface.summaryRows.map(_.label).take(3),
      List("Tactical relation", "Main plans", "Practical plan"),
      clue(surface.summaryRows)
    )
    assertEquals(
      surface.summaryRows.head.authority.flatMap(_.token),
      Some(MoveReviewExchangeAnalyzer.RelationKind.Fork),
      clue(surface.summaryRows.head)
    )
  }

  test("strategic relation explanation cue fails closed without typed support") {
    val fork = RelationObservationCatalog.descriptorForKind(MoveReviewExchangeAnalyzer.RelationKind.Fork).get
    val evidenceOnlyIdea =
      StrategyIdeaSignal(
        ideaId = "idea_fork",
        ownerSide = "white",
        kind = fork.ideaKind,
        group = StrategicIdeaGroup.InteractionAndTransformation,
        readiness = fork.readiness,
        focusSquares = List("f5", "e7", "h4"),
        confidence = fork.confidence,
        evidenceRefs = fork.wireEvidenceRefs ++ List("target:e7:rook", "target:h4:queen"),
        targetSquare = Some("h4"),
        relationKind = Some(fork.relationKind),
        relationFocusSquares = List("f5", "e7", "h4"),
        relationSupport = None
      )
    val surface =
      build(
        strategyPack =
          Some(
            StrategyPack(
              sideToMove = "white",
              strategicIdeas = List(evidenceOnlyIdea)
            )
          )
      )

    assert(
      !surface.summaryRows.exists(_.authority.exists(_.kind == MoveReviewSurfaceAuthority.StrategicRelation)),
      clue(surface.summaryRows)
    )
  }

  test("strategic relation surface rejects divergent relation focus and support focus") {
    val xray = RelationObservationCatalog.descriptorForKind(MoveReviewExchangeAnalyzer.RelationKind.XRay).get
    val relationIdea =
      StrategyIdeaSignal(
        ideaId = "idea_xray",
        ownerSide = "white",
        kind = xray.ideaKind,
        group = StrategicIdeaGroup.PieceAndLineManagement,
        readiness = xray.readiness,
        focusSquares = List("e4", "f5", "g6"),
        confidence = xray.confidence,
        evidenceRefs = xray.wireEvidenceRefs,
        targetSquare = Some("g6"),
        relationKind = Some(xray.relationKind),
        relationFocusSquares = List("e4", "f5", "g6"),
        relationSupport =
          Some(
            StrategyRelationSupport(
              relationKind = xray.relationKind,
              focusSquares = List("a1", "a2", "a3"),
              targetSquare = Some("a3"),
              attackerSquare = Some("a1"),
              blockerSquare = Some("a2")
            )
          )
      )
    val surface =
      build(
        strategyPack =
          Some(
            StrategyPack(
              sideToMove = "white",
              strategicIdeas = List(relationIdea)
            )
          )
      )

    val surfacedRelations =
      surface.summaryRows ++ surface.advancedRows

    assert(
      !surfacedRelations.exists(_.authority.exists(_.kind == MoveReviewSurfaceAuthority.StrategicRelation)),
      clue(surfacedRelations)
    )
  }

  test("strategic relation row rejects evidenceRef-only source and semantic fact carriers") {
    val sourceOnlyIdea =
      StrategyIdeaSignal(
        ideaId = "idea_1",
        ownerSide = "white",
        kind = StrategicIdeaKind.LineOccupation,
        group = "line_occupation",
        readiness = StrategicIdeaReadiness.Build,
        focusSquares = List("e4", "f5", "g6"),
        confidence = 0.72,
        evidenceRefs = List("source:xray_relation", "blocker:f5")
      )
    val semanticOnlyIdea =
      sourceOnlyIdea.copy(
        ideaId = "idea_2",
        evidenceRefs = List("xray_semantic", "blocker:f5")
      )

    val surface =
      build(
        strategyPack =
          Some(
            StrategyPack(
              sideToMove = "white",
              strategicIdeas = List(sourceOnlyIdea, semanticOnlyIdea)
            )
          )
      )

    assert(!surface.advancedRows.exists(_.authority.exists(_.kind == MoveReviewSurfaceAuthority.StrategicRelation)), clue(surface.advancedRows))
  }

  test("legacy strategic relation projection rejects source-fact strings without typed support") {
    val xray = RelationObservationCatalog.descriptorForKind(MoveReviewExchangeAnalyzer.RelationKind.XRay).get
    val clearance = RelationObservationCatalog.descriptorForKind(MoveReviewExchangeAnalyzer.RelationKind.Clearance).get
    val ambiguousIdea =
      StrategyIdeaSignal(
        ideaId = "idea_1",
        ownerSide = "white",
        kind = StrategicIdeaKind.LineOccupation,
        group = "line_occupation",
        readiness = StrategicIdeaReadiness.Build,
        focusSquares = List("e4", "f5", "g6"),
        confidence = 0.72,
        evidenceRefs = xray.wireEvidenceRefs ++ clearance.wireEvidenceRefs,
        relationKind = None,
        relationFocusSquares = List("e4", "f5", "g6")
      )
    val surface =
      build(
        strategyPack =
          Some(
            StrategyPack(
              sideToMove = "white",
              strategicIdeas = List(ambiguousIdea)
            )
          )
      )

    assert(
      !surface.advancedRows.exists(_.authority.exists(_.kind == MoveReviewSurfaceAuthority.StrategicRelation)),
      clue(surface.advancedRows)
    )
  }

  test("legacy strategic relation projection rejects relation-specific focus without typed support") {
    val xray = RelationObservationCatalog.descriptorForKind(MoveReviewExchangeAnalyzer.RelationKind.XRay).get
    val relationIdea =
      StrategyIdeaSignal(
        ideaId = "idea_1",
        ownerSide = "white",
        kind = StrategicIdeaKind.LineOccupation,
        group = "line_occupation",
        readiness = StrategicIdeaReadiness.Build,
        focusSquares = List("e4", "f5", "g6"),
        confidence = 0.72,
        evidenceRefs = xray.wireEvidenceRefs,
        relationKind = None,
        relationFocusSquares = Nil
      )
    val surface =
      build(
        strategyPack =
          Some(
            StrategyPack(
              sideToMove = "white",
              strategicIdeas = List(relationIdea)
            )
          )
      )

    assert(
      !surface.advancedRows.exists(_.authority.exists(_.kind == MoveReviewSurfaceAuthority.StrategicRelation)),
      clue(surface.advancedRows)
    )
  }

  test("strategic relation surface target prefers analyzer-carried target over focus order") {
    val relationIdea =
      StrategyIdeaSignal(
        ideaId = "idea_1",
        ownerSide = "white",
        kind = StrategicIdeaKind.LineOccupation,
        group = "line_occupation",
        readiness = StrategicIdeaReadiness.Build,
        focusSquares = List("g6", "f5", "e4"),
        confidence = 0.72,
        evidenceRefs = List("source:xray_relation", "xray_semantic"),
        targetSquare = Some("g6"),
        relationKind = Some(MoveReviewExchangeAnalyzer.RelationKind.XRay),
        relationFocusSquares = List("g6", "f5", "e4"),
        relationSupport =
          Some(
            relationSupportFor(
              MoveReviewExchangeAnalyzer.RelationKind.XRay,
              List("g6", "f5", "e4"),
              Some("g6")
            )
          )
      )
    val surface =
      build(
        strategyPack =
          Some(
            StrategyPack(
              sideToMove = "white",
              strategicIdeas = List(relationIdea)
            )
          )
      )

    assertEquals(
      surface.advancedRows.flatMap(_.authority.flatMap(_.target)).headOption,
      Some("g6"),
      clue(surface.advancedRows)
    )
  }

  test("strategic relation surface target ignores targetSquare outside relation focus") {
    val relationIdea =
      StrategyIdeaSignal(
        ideaId = "idea_1",
        ownerSide = "white",
        kind = StrategicIdeaKind.LineOccupation,
        group = "line_occupation",
        readiness = StrategicIdeaReadiness.Build,
        focusSquares = List("e4", "f5", "g6"),
        confidence = 0.72,
        evidenceRefs = List("source:xray_relation", "xray_semantic"),
        targetSquare = Some("a1"),
        relationKind = Some(MoveReviewExchangeAnalyzer.RelationKind.XRay),
        relationFocusSquares = List("e4", "f5", "g6"),
        relationSupport =
          Some(
            relationSupportFor(
              MoveReviewExchangeAnalyzer.RelationKind.XRay,
              List("e4", "f5", "g6"),
              Some("g6")
            )
          )
      )
    val surface =
      build(
        strategyPack =
          Some(
            StrategyPack(
              sideToMove = "white",
              strategicIdeas = List(relationIdea)
            )
          )
      )

    assertEquals(
      surface.advancedRows.flatMap(_.authority.flatMap(_.target)).headOption,
      Some("g6"),
      clue(surface.advancedRows)
    )
  }

  test("legacy strategic relation targetSquare is rejected without typed support") {
    val relationIdea =
      StrategyIdeaSignal(
        ideaId = "idea_1",
        ownerSide = "white",
        kind = StrategicIdeaKind.LineOccupation,
        group = "line_occupation",
        readiness = StrategicIdeaReadiness.Build,
        focusSquares = List("e4", "f5", "g6"),
        confidence = 0.72,
        evidenceRefs = List("source:xray_relation", "xray_semantic"),
        targetSquare = Some("f5"),
        relationKind = None,
        relationFocusSquares = List("e4", "f5", "g6")
      )
    val surface =
      build(
        strategyPack =
          Some(
            StrategyPack(
              sideToMove = "white",
              strategicIdeas = List(relationIdea)
            )
          )
      )

    assert(!surface.advancedRows.exists(_.authority.exists(_.kind == MoveReviewSurfaceAuthority.StrategicRelation)), clue(surface.advancedRows))
  }

  test("strategic relation fallback target policy is catalog-owned") {
    val defenderTrade = RelationObservationCatalog.descriptorForKind(MoveReviewExchangeAnalyzer.RelationKind.DefenderTrade).get
    val decoy = RelationObservationCatalog.descriptorForKind(MoveReviewExchangeAnalyzer.RelationKind.Decoy).get
    val ideas =
      List(
        defenderTrade -> List("c5", "a3"),
        decoy -> List("f4", "d3", "d5")
      ).map { case (descriptor, focus) =>
        StrategyIdeaSignal(
          ideaId = s"idea_${descriptor.relationKind}",
          ownerSide = "white",
          kind = descriptor.ideaKind,
          group = StrategicIdeaGroup.InteractionAndTransformation,
          readiness = descriptor.readiness,
          focusSquares = focus,
          confidence = descriptor.confidence,
          evidenceRefs = List(EvidenceRef.Source(descriptor.source).wireKey, descriptor.observationId.wireKey),
          relationKind = Some(descriptor.relationKind),
          relationFocusSquares = focus,
          relationSupport = Some(relationSupportFor(descriptor.relationKind, focus, None))
        )
      }
    val surface =
      build(
        strategyPack =
          Some(
            StrategyPack(
              sideToMove = "white",
              strategicIdeas = ideas
            )
          )
      )
    val targetsByRelation =
      surface.advancedRows.flatMap(row =>
        row.authority.flatMap(authority => authority.token.map(_ -> authority.target))
      ).toMap

    assertEquals(targetsByRelation.get(MoveReviewExchangeAnalyzer.RelationKind.DefenderTrade).flatten, Some("c5"))
    assertEquals(targetsByRelation.get(MoveReviewExchangeAnalyzer.RelationKind.Decoy).flatten, Some("d3"))

    val root = java.nio.file.Paths.get("").toAbsolutePath
    val builderText =
      java.nio.file.Files.readString(
        root.resolve("modules/commentaryCore/src/main/scala/lila/commentary/analysis/MoveReviewPlayerPayloadBuilder.scala")
      )
    assert(!builderText.contains("MoveReviewExchangeAnalyzer.RelationKind.DefenderTrade"), clues(builderText))
    assert(!builderText.contains("MoveReviewExchangeAnalyzer.RelationKind.Decoy"), clues(builderText))
    assert(!builderText.contains("def relationTarget("), clues(builderText))
  }

  test("strategic relation projection honors selected relationKind before catalog order") {
    val xray = RelationObservationCatalog.descriptorForKind(MoveReviewExchangeAnalyzer.RelationKind.XRay).get
    val clearance = RelationObservationCatalog.descriptorForKind(MoveReviewExchangeAnalyzer.RelationKind.Clearance).get
    val relationIdea =
      StrategyIdeaSignal(
        ideaId = "idea_1",
        ownerSide = "white",
        kind = StrategicIdeaKind.LineOccupation,
        group = "line_occupation",
        readiness = StrategicIdeaReadiness.Build,
        focusSquares = List("e4", "f5", "g6", "d7"),
        confidence = 0.72,
        evidenceRefs =
          List(
            EvidenceRef.Source(xray.source).wireKey,
            xray.observationId.wireKey,
            EvidenceRef.Source(clearance.source).wireKey,
            clearance.observationId.wireKey
          ),
        targetSquare = Some("d7"),
        relationKind = Some(clearance.relationKind),
        relationFocusSquares = List("d1", "d3", "d7"),
        relationSupport =
          Some(
            relationSupportFor(
              clearance.relationKind,
              List("d1", "d3", "d7"),
              Some("d7")
            )
          )
      )
    val surface =
      build(
        strategyPack =
          Some(
            StrategyPack(
              sideToMove = "white",
              strategicIdeas = List(relationIdea)
            )
          )
      )

    val row =
      surface.advancedRows
        .find(_.authority.exists(_.kind == MoveReviewSurfaceAuthority.StrategicRelation))
        .getOrElse(fail(s"missing strategic relation row: ${surface.advancedRows}"))

    assertEquals(row.authority.flatMap(_.token), Some(MoveReviewExchangeAnalyzer.RelationKind.Clearance), clue(row))
    assert(row.text.contains("clearance evidence"), clue(row))
    assert(row.text.contains("d1, d3, d7"), clue(row))
    assert(!row.text.contains("e4, f5, g6"), clue(row))
  }

  test("selected strategic relation rows require relation-specific focus") {
    val clearance = RelationObservationCatalog.descriptorForKind(MoveReviewExchangeAnalyzer.RelationKind.Clearance).get
    val relationIdea =
      StrategyIdeaSignal(
        ideaId = "idea_1",
        ownerSide = "white",
        kind = StrategicIdeaKind.LineOccupation,
        group = "line_occupation",
        readiness = StrategicIdeaReadiness.Build,
        focusSquares = List("e4", "f5", "g6"),
        confidence = 0.72,
        evidenceRefs = clearance.wireEvidenceRefs,
        targetSquare = Some("g6"),
        relationKind = Some(clearance.relationKind),
        relationFocusSquares = Nil
      )
    val surface =
      build(
        strategyPack =
          Some(
            StrategyPack(
              sideToMove = "white",
              strategicIdeas = List(relationIdea)
            )
          )
      )

    assert(
      !surface.advancedRows.exists(_.authority.exists(_.kind == MoveReviewSurfaceAuthority.StrategicRelation)),
      clue(surface.advancedRows)
    )
  }

  test("strategic relation projection preserves deflection and discovered-attack geometry") {
    val relationShapes =
      List(
        (
          MoveReviewExchangeAnalyzer.RelationKind.Deflection,
          List("g7", "f8", "a3"),
          "g7",
          "deflection evidence"
        ),
        (
          MoveReviewExchangeAnalyzer.RelationKind.DiscoveredAttack,
          List("b1", "d3", "h7"),
          "h7",
          "discovered-attack evidence"
        )
      )
    val ideas =
      relationShapes.map { case (kind, focus, target, _) =>
        val descriptor = RelationObservationCatalog.descriptorForKind(kind).get
        StrategyIdeaSignal(
          ideaId = s"idea_$kind",
          ownerSide = "white",
          kind = descriptor.ideaKind,
          group = StrategicIdeaGroup.InteractionAndTransformation,
          readiness = descriptor.readiness,
          focusSquares = List("a1", "a2", "a3"),
          confidence = descriptor.confidence,
          evidenceRefs = descriptor.wireEvidenceRefs,
          targetSquare = Some(target),
          relationKind = Some(kind),
          relationFocusSquares = focus,
          relationSupport = Some(relationSupportFor(kind, focus, Some(target)))
        )
      }
    val surface =
      build(
        strategyPack =
          Some(
            StrategyPack(
              sideToMove = "white",
              strategicIdeas = ideas
            )
          )
      )
    val rowsByToken =
      surface.advancedRows.flatMap(row => row.authority.flatMap(_.token.map(_ -> row))).toMap

    relationShapes.foreach { case (kind, focus, target, labelText) =>
      val row = rowsByToken.getOrElse(kind, fail(s"missing $kind relation row: ${surface.advancedRows}"))

      assert(row.text.contains(labelText), clue(row))
      assert(row.text.contains(focus.mkString(", ")), clue(row))
      assert(!row.text.contains("a1, a2, a3"), clue(row))
      assertEquals(row.authority.flatMap(_.target), Some(target), clue(row))
    }
  }

  test("strategic relation projection does not fall back when selected relationKind lacks matching evidence") {
    val xray = RelationObservationCatalog.descriptorForKind(MoveReviewExchangeAnalyzer.RelationKind.XRay).get
    val clearance = RelationObservationCatalog.descriptorForKind(MoveReviewExchangeAnalyzer.RelationKind.Clearance).get
    val relationIdea =
      StrategyIdeaSignal(
        ideaId = "idea_1",
        ownerSide = "white",
        kind = StrategicIdeaKind.LineOccupation,
        group = "line_occupation",
        readiness = StrategicIdeaReadiness.Build,
        focusSquares = List("e4", "f5", "g6"),
        confidence = 0.72,
        evidenceRefs =
          List(
            EvidenceRef.Source(xray.source).wireKey,
            xray.observationId.wireKey
          ),
        relationKind = Some(clearance.relationKind),
        relationFocusSquares = List("e4", "f5", "g6")
      )
    val surface =
      build(
        strategyPack =
          Some(
            StrategyPack(
              sideToMove = "white",
              strategicIdeas = List(relationIdea)
            )
          )
      )

    assert(!surface.advancedRows.exists(_.authority.exists(_.kind == MoveReviewSurfaceAuthority.StrategicRelation)), clue(surface.advancedRows))
  }

  test("strategic relation surface text uses bounded witness support facts") {
    val cases =
      List(
        (
          MoveReviewExchangeAnalyzer.RelationKind.TrappedPiece,
          List("g6", "f5"),
          Some("g6"),
          Some(
            StrategyRelationSupport(
              relationKind = MoveReviewExchangeAnalyzer.RelationKind.TrappedPiece,
              focusSquares = List("g6", "f5"),
              targetSquare = Some("g6"),
              targetRole = Some("Q"),
              legalEscapeCount = Some(0)
            )
          ),
          List("trapped piece evidence around g6, f5", "queen on g6 has no legal escape square")
        ),
        (
          MoveReviewExchangeAnalyzer.RelationKind.Zwischenzug,
          List("h5", "e8"),
          Some("h5"),
          Some(
            StrategyRelationSupport(
              relationKind = MoveReviewExchangeAnalyzer.RelationKind.Zwischenzug,
              focusSquares = List("h5", "e8"),
              targetSquare = Some("h5"),
              intermediateMove = Some("e1e8"),
              threatType = Some("check"),
              payoffMove = Some("e8h5")
            )
          ),
          List("zwischenzug evidence around h5, e8", "e1-e8 comes before the payoff on e8-h5", "after a check")
        ),
        (
          MoveReviewExchangeAnalyzer.RelationKind.Pin,
          List("e4", "f5", "g6"),
          Some("g6"),
          Some(
            StrategyRelationSupport(
              relationKind = MoveReviewExchangeAnalyzer.RelationKind.Pin,
              focusSquares = List("e4", "f5", "g6"),
              targetSquare = Some("g6"),
              attackerSquare = Some("e4"),
              pinnedSquare = Some("f5"),
              behindSquare = Some("g6"),
              absolutePin = Some(true)
            )
          ),
          List("pin evidence around e4, f5, g6", "attacker on e4 pins the front piece on f5 to the piece behind on g6")
        ),
        (
          MoveReviewExchangeAnalyzer.RelationKind.PerpetualCheck,
          List("h7", "h5"),
          Some("h7"),
          Some(
            StrategyRelationSupport(
              relationKind = MoveReviewExchangeAnalyzer.RelationKind.PerpetualCheck,
              focusSquares = List("h7", "h5"),
              targetSquare = Some("h7"),
              kingSquare = Some("h7"),
              checkingMoves = List("h5h7", "h7h5")
            )
          ),
          List("perpetual check evidence around h7, h5", "checking cycle h5-h7 and h7-h5 repeats against the king on h7")
        )
      )

    cases.foreach { case (kind, focus, target, support, expectedFragments) =>
      val row = relationSurfaceRow(kind, focus, target, support)

      expectedFragments.foreach(fragment => assert(row.text.contains(fragment), clue(row)))
      assertEquals(row.authority.flatMap(_.token), Some(kind), clue(row))
      assertEquals(row.authority.flatMap(_.target), target, clue(row))
    }

    val root = java.nio.file.Paths.get("").toAbsolutePath
    val formatterText =
      java.nio.file.Files.readString(
        root.resolve("modules/commentaryCore/src/main/scala/lila/commentary/analysis/RelationSurfaceText.scala")
      )
    val selectorText =
      java.nio.file.Files.readString(
        root.resolve("modules/commentaryCore/src/main/scala/lila/commentary/analysis/StrategicIdeaSelector.scala")
      )
    val playerFacingIdeaSlice =
      selectorText.substring(
        selectorText.indexOf("def playerFacingIdeaText"),
        selectorText.indexOf("private val PrioritySupportEvidenceRefs")
      )
    assert(formatterText.contains("relationSupport"), clues(formatterText))
    assert(!formatterText.contains("evidenceRefs"), clues(formatterText))
    assert(playerFacingIdeaSlice.contains("RelationSurfaceText.ideaText"), clues(playerFacingIdeaSlice))
    assert(!playerFacingIdeaSlice.contains("evidenceRefs"), clues(playerFacingIdeaSlice))
  }

  test("strategic relation surface text names structured roles and line geometry") {
    val cases =
      List(
        (
          MoveReviewExchangeAnalyzer.RelationKind.Pin,
          List("b4", "c3", "e1"),
          Some("e1"),
          StrategyRelationSupport(
            relationKind = MoveReviewExchangeAnalyzer.RelationKind.Pin,
            focusSquares = List("b4", "c3", "e1"),
            targetSquare = Some("e1"),
            attackerSquare = Some("b4"),
            pinnedSquare = Some("c3"),
            pinnedRole = Some("knight"),
            behindSquare = Some("e1"),
            behindRole = Some("king"),
            absolutePin = Some(true)
          ),
          List("absolute pin", "attacker on b4 pins the knight on c3 to the king behind on e1")
        ),
        (
          MoveReviewExchangeAnalyzer.RelationKind.Skewer,
          List("a1", "e1", "h1"),
          Some("h1"),
          StrategyRelationSupport(
            relationKind = MoveReviewExchangeAnalyzer.RelationKind.Skewer,
            focusSquares = List("a1", "e1", "h1"),
            targetSquare = Some("h1"),
            attackerSquare = Some("a1"),
            frontSquare = Some("e1"),
            frontRole = Some("queen"),
            backSquare = Some("h1"),
            backRole = Some("rook")
          ),
          List("attacker on a1 skewers the queen on e1 to the rook behind on h1")
        ),
        (
          MoveReviewExchangeAnalyzer.RelationKind.Battery,
          List("d3", "b1", "h7"),
          Some("h7"),
          StrategyRelationSupport(
            relationKind = MoveReviewExchangeAnalyzer.RelationKind.Battery,
            focusSquares = List("d3", "b1", "h7"),
            targetSquare = Some("h7"),
            frontSquare = Some("d3"),
            frontRole = Some("queen"),
            backSquare = Some("b1"),
            backRole = Some("bishop"),
            axis = Some("diagonal")
          ),
          List("queen on d3 and bishop on b1 align on the diagonal toward h7")
        ),
        (
          MoveReviewExchangeAnalyzer.RelationKind.Clearance,
          List("f4", "b1", "h7"),
          Some("h7"),
          StrategyRelationSupport(
            relationKind = MoveReviewExchangeAnalyzer.RelationKind.Clearance,
            focusSquares = List("f4", "b1", "h7"),
            targetSquare = Some("h7"),
            clearedSquare = Some("f4"),
            beneficiarySquare = Some("b1"),
            beneficiaryRole = Some("bishop"),
            clearingTo = Some("f4")
          ),
          List("clears f4 for the bishop on b1 toward h7")
        ),
        (
          MoveReviewExchangeAnalyzer.RelationKind.Decoy,
          List("d3", "d5", "e2"),
          Some("d3"),
          StrategyRelationSupport(
            relationKind = MoveReviewExchangeAnalyzer.RelationKind.Decoy,
            focusSquares = List("d3", "d5", "e2"),
            targetSquare = Some("d3"),
            baitSquare = Some("d3"),
            baitRole = Some("knight"),
            luredFromSquare = Some("d5"),
            luredRole = Some("queen"),
            executionFromSquare = Some("e2"),
            executionToSquare = Some("d3")
          ),
          List("knight bait on d3 draws the queen from d5 before e2-d3")
        )
      )

    cases.foreach { case (kind, focus, target, support, expectedFragments) =>
      val row = relationSurfaceRow(kind, focus, target, Some(support))

      expectedFragments.foreach(fragment => assert(row.text.contains(fragment), clue(row)))
    }
  }

  test("strategic relation surface text names tactical duties and king-pattern details") {
    val cases =
      List(
        (
          MoveReviewExchangeAnalyzer.RelationKind.Fork,
          List("f5", "e7", "h4"),
          Some("h4"),
          StrategyRelationSupport(
            relationKind = MoveReviewExchangeAnalyzer.RelationKind.Fork,
            focusSquares = List("f5", "e7", "h4"),
            targetSquare = Some("h4"),
            attackerSquare = Some("f5"),
            attackerRole = Some("knight"),
            targetSquares = List("e7", "h4"),
            targetRoles = List("rook", "queen")
          ),
          List("knight on f5 forks the rook on e7 and the queen on h4")
        ),
        (
          MoveReviewExchangeAnalyzer.RelationKind.Overload,
          List("f6", "d5", "h7"),
          Some("h7"),
          StrategyRelationSupport(
            relationKind = MoveReviewExchangeAnalyzer.RelationKind.Overload,
            focusSquares = List("f6", "d5", "h7"),
            targetSquare = Some("h7"),
            defenderSquare = Some("f6"),
            targetSquares = List("d5", "h7"),
            attackerSquare = Some("d3")
          ),
          List("defender on f6 is overloaded across d5 and h7 under pressure from d3")
        ),
        (
          MoveReviewExchangeAnalyzer.RelationKind.Deflection,
          List("g7", "f8", "a3"),
          Some("g7"),
          StrategyRelationSupport(
            relationKind = MoveReviewExchangeAnalyzer.RelationKind.Deflection,
            focusSquares = List("g7", "f8", "a3"),
            targetSquare = Some("g7"),
            defenderSquare = Some("f8"),
            attackerSquare = Some("a3")
          ),
          List("attack from a3 pulls the defender on f8 away from g7")
        ),
        (
          MoveReviewExchangeAnalyzer.RelationKind.DoubleCheck,
          List("f6", "e1", "e8"),
          Some("e8"),
          StrategyRelationSupport(
            relationKind = MoveReviewExchangeAnalyzer.RelationKind.DoubleCheck,
            focusSquares = List("f6", "e1", "e8"),
            targetSquare = Some("e8"),
            kingSquare = Some("e8"),
            checkerSquares = List("f6", "e1"),
            moverSquare = Some("f6"),
            moverRole = Some("knight")
          ),
          List("knight on f6 gives double check on the king on e8 from f6 and e1")
        ),
        (
          MoveReviewExchangeAnalyzer.RelationKind.BackRankMate,
          List("e8", "g8"),
          Some("g8"),
          StrategyRelationSupport(
            relationKind = MoveReviewExchangeAnalyzer.RelationKind.BackRankMate,
            focusSquares = List("e8", "g8"),
            targetSquare = Some("g8"),
            kingSquare = Some("g8"),
            matingMove = Some("e1e8"),
            patternId = Some("back_rank_mate")
          ),
          List("e1-e8 delivers back-rank mate against the king on g8")
        ),
        (
          MoveReviewExchangeAnalyzer.RelationKind.PerpetualCheck,
          List("h7", "h5"),
          Some("h7"),
          StrategyRelationSupport(
            relationKind = MoveReviewExchangeAnalyzer.RelationKind.PerpetualCheck,
            focusSquares = List("h7", "h5"),
            targetSquare = Some("h7"),
            kingSquare = Some("h7"),
            checkingMoves = List("h5h7", "h7h5"),
            cycleMoves = List("h5h7", "h7h5"),
            repeatedPositionPly = Some(4)
          ),
          List("checking cycle h5-h7 and h7-h5 repeats against the king on h7 by ply 4")
        ),
        (
          MoveReviewExchangeAnalyzer.RelationKind.TrappedPiece,
          List("g6", "f5", "h5"),
          Some("g6"),
          StrategyRelationSupport(
            relationKind = MoveReviewExchangeAnalyzer.RelationKind.TrappedPiece,
            focusSquares = List("g6", "f5", "h5"),
            targetSquare = Some("g6"),
            targetRole = Some("queen"),
            attackerSquares = List("f5", "h5"),
            legalEscapeCount = Some(0)
          ),
          List("queen on g6 has no legal escape square in that replay; attackers include f5 and h5")
        ),
        (
          MoveReviewExchangeAnalyzer.RelationKind.Domination,
          List("g6", "e4"),
          Some("g6"),
          StrategyRelationSupport(
            relationKind = MoveReviewExchangeAnalyzer.RelationKind.Domination,
            focusSquares = List("g6", "e4"),
            targetSquare = Some("g6"),
            targetRole = Some("knight"),
            controllerSquare = Some("e4"),
            controllerRole = Some("bishop"),
            legalMoveCount = Some(1)
          ),
          List("knight on g6 has only one legal move under bishop control from e4")
        )
      )

    cases.foreach { case (kind, focus, target, support, expectedFragments) =>
      val row = relationSurfaceRow(kind, focus, target, Some(support))

      expectedFragments.foreach(fragment => assert(row.text.contains(fragment), clue(row)))
    }
  }

  test("every cataloged relation descriptor can project through the bounded surface row") {
    RelationObservationCatalog.Implemented.foreach { descriptor =>
      val relationIdea =
        StrategyIdeaSignal(
          ideaId = s"idea_${descriptor.relationKind}",
          ownerSide = "white",
          kind = descriptor.ideaKind,
          group =
            if descriptor.ideaKind == StrategicIdeaKind.LineOccupation then StrategicIdeaGroup.PieceAndLineManagement
            else StrategicIdeaGroup.InteractionAndTransformation,
          readiness = descriptor.readiness,
          focusSquares = List("e4", "f5", "g6"),
          confidence = descriptor.confidence,
          evidenceRefs = List(EvidenceRef.Source(descriptor.source).wireKey, descriptor.observationId.wireKey),
          relationKind = Some(descriptor.relationKind),
          relationFocusSquares = List("e4", "f5", "g6"),
          relationSupport =
            Some(
              relationSupportFor(
                descriptor.relationKind,
                List("e4", "f5", "g6"),
                Some("g6")
              )
            )
        )
      val surface =
        build(
          strategyPack =
            Some(
              StrategyPack(
                sideToMove = "white",
                strategicIdeas = List(relationIdea)
              )
            )
        )
      val row =
        surface.advancedRows
          .find(_.authority.exists(_.kind == MoveReviewSurfaceAuthority.StrategicRelation))
          .getOrElse(fail(s"missing ${descriptor.relationKind} relation row: ${surface.advancedRows}"))

      assertEquals(
        row.label,
        descriptor.surfaceRowLabel,
        clue(descriptor.relationKind)
      )
      assert(row.text.contains(s"${descriptor.publicLabel} evidence"), clue(row))
      assertEquals(row.authority.flatMap(_.token), Some(descriptor.relationKind), clue(row))
    }
  }

  test("unknown relation kinds never project through strategic relation surface rows") {
    val xray = RelationObservationCatalog.descriptorForKind(MoveReviewExchangeAnalyzer.RelationKind.XRay).get

    List("unsupported_relation").foreach { relationKind =>
      val relationIdea =
        StrategyIdeaSignal(
          ideaId = s"idea_$relationKind",
          ownerSide = "white",
          kind = StrategicIdeaKind.LineOccupation,
          group = StrategicIdeaGroup.PieceAndLineManagement,
          readiness = StrategicIdeaReadiness.Build,
          focusSquares = List("e4", "f5", "g6"),
          confidence = 0.72,
          evidenceRefs = List(EvidenceRef.Source(xray.source).wireKey, xray.observationId.wireKey),
          relationKind = Some(relationKind),
          relationFocusSquares = List("e4", "f5", "g6")
        )
      val surface =
        build(
          strategyPack =
            Some(
              StrategyPack(
                sideToMove = "white",
                strategicIdeas = List(relationIdea)
              )
            )
        )

      assert(
        !surface.advancedRows.exists(_.authority.exists(_.kind == MoveReviewSurfaceAuthority.StrategicRelation)),
        clue(relationKind)
      )
    }
  }

  test("relation-only StrategyPackBuilder output reaches strategic relation advanced row") {
    val fen = "6k1/5ppp/6q1/5n2/8/8/8/1B5K w - - 0 1"
    val playedMove = "b1e4"
    val ctx = relationOnlyContext(fen, playedMove)
    val strategyPack =
      StrategyPackBuilder
        .build(relationOnlyData(fen, playedMove), ctx)
        .getOrElse(fail("relation-only strategy pack missing"))

    assert(strategyPack.strategicIdeas.exists(_.evidenceRefs.contains("source:xray_relation")), clue(strategyPack))
    assert(strategyPack.strategicIdeas.exists(_.targetSquare.contains("g6")), clue(strategyPack))
    assert(strategyPack.strategicIdeas.exists(_.relationKind.contains(MoveReviewExchangeAnalyzer.RelationKind.XRay)), clue(strategyPack))
    assert(strategyPack.strategicIdeas.exists(_.relationFocusSquares.contains("g6")), clue(strategyPack))
    assert(strategyPack.strategicIdeas.exists(_.relationSupport.exists(_.blockerSquare.contains("f5"))), clue(strategyPack))

    val surface = build(ctx = ctx, strategyPack = Some(strategyPack))
    val row =
      surface.advancedRows
        .find(_.authority.exists(_.kind == MoveReviewSurfaceAuthority.StrategicRelation))
        .getOrElse(fail(s"missing strategic relation row: ${surface.advancedRows}"))

    assertEquals(row.label, "Line relation")
    assertEquals(row.source, None)
    assert(row.text.contains("the line runs from e4 through f5 toward g6"), clue(row))
    assertEquals(
      row.authority,
      Some(
        MoveReviewSurfaceAuthority(
          kind = MoveReviewSurfaceAuthority.StrategicRelation,
          token = Some("xray"),
          target = Some("g6")
        )
      )
    )

    val cue =
      surface.summaryRows
        .find(_.authority.exists(authority =>
          authority.kind == MoveReviewSurfaceAuthority.StrategicRelation &&
            authority.token.contains("xray")
        ))
        .getOrElse(fail(s"missing xray relation summary cue: ${surface.summaryRows}"))

    assertEquals(cue.label, "Line relation")
    assert(cue.text.contains("Why it works: the line runs from e4 through f5 toward g6."), clue(cue))
    assert(cue.text.contains("Next check: the line geometry through e4, f5, and g6."), clue(cue))
  }

  test("relation witness remains player-facing when stronger plan-family evidence also exists") {
    val fen = "6k1/5ppp/6q1/5n2/8/8/8/1B5K w - - 0 1"
    val playedMove = "b1e4"
    val ctx =
      relationOnlyContext(fen, playedMove).copy(
        pawnPlay = PawnPlayTable(
          breakReady = true,
          breakFile = Some("d"),
          breakImpact = "High",
          tensionPolicy = "Maintain",
          tensionReason = "central break is available",
          passedPawnUrgency = "Background",
          passerBlockade = None,
          counterBreak = false,
          primaryDriver = "break_ready"
        )
      )
    val strategyPack =
      StrategyPackBuilder
        .build(relationOnlyData(fen, playedMove), ctx)
        .getOrElse(fail("strategy pack with relation and pawn break missing"))

    assert(strategyPack.strategicIdeas.exists(_.kind == StrategicIdeaKind.PawnBreak), clue(strategyPack.strategicIdeas))
    assert(
      strategyPack.strategicIdeas.exists(_.relationKind.contains(MoveReviewExchangeAnalyzer.RelationKind.XRay)),
      clue(strategyPack.strategicIdeas)
    )

    val surface = build(ctx = ctx, strategyPack = Some(strategyPack))
    val cue =
      surface.summaryRows
        .find(_.authority.flatMap(_.token).contains(MoveReviewExchangeAnalyzer.RelationKind.XRay))
        .getOrElse(fail(s"missing xray relation summary cue: ${surface.summaryRows}"))

    assertEquals(cue.label, "Line relation")
    assert(cue.text.contains("Why it works: the line runs from e4 through f5 toward g6."), clue(cue))
  }

  test("mobility and drawing relation witnesses reach player-facing summary cues from StrategyPackBuilder") {
    val cases =
      List(
        (
          MoveReviewExchangeAnalyzer.RelationKind.TrappedPiece,
          "n3k3/2p5/1p6/8/8/8/4K3/7R w - - 0 1",
          List("h1a1"),
          List("a8"),
          "the knight on a8 has no legal escape square",
          "trapped piece evidence around a8, a1",
          "a8"
        ),
        (
          MoveReviewExchangeAnalyzer.RelationKind.Domination,
          "n3k3/2p5/8/8/8/8/4K3/7R w - - 0 1",
          List("h1a1"),
          List("a8"),
          "the knight on a8 has only one legal move",
          "domination evidence around a8, a1",
          "a8"
        ),
        (
          MoveReviewExchangeAnalyzer.RelationKind.Zwischenzug,
          "6k1/8/8/7r/8/8/8/4Q1K1 w - - 0 1",
          List("e1e8", "g8h7", "e8h5"),
          List("h5"),
          "e1-e8 comes before the payoff on e8-h5",
          "zwischenzug evidence around h5, e8",
          "h5"
        ),
        (
          MoveReviewExchangeAnalyzer.RelationKind.StalemateTrap,
          "7k/5K2/8/6Q1/8/8/8/8 w - - 0 1",
          List("g5g6"),
          List("h8"),
          "stalemate resource around the king on h8",
          "stalemate trap evidence around h8",
          "h8"
        ),
        (
          MoveReviewExchangeAnalyzer.RelationKind.PerpetualCheck,
          "7k/8/8/8/8/8/8/4Q1K1 w - - 0 1",
          List("e1e8", "h8h7", "e8h5", "h7g8", "h5e8", "g8h7", "e8h5"),
          Nil,
          "checking cycle",
          "perpetual check evidence around h7",
          "h7"
        )
      )

    cases.foreach { case (kind, fen, line, targets, summaryFragment, advancedFragment, target) =>
      val playedMove = line.head
      val structuralTargets =
        targets.flatMap(Square.fromKey)
      val data =
        relationOnlyData(fen, playedMove).copy(
          structuralWeaknesses =
            Option.when(structuralTargets.nonEmpty)(
              WeakComplex(
                color = Color.Black,
                squares = structuralTargets,
                isOutpost = false,
                cause = s"$kind target"
              )
            ).toList
        )
      val ctx =
        relationOnlyContext(fen, playedMove).copy(
          playedSan = Some(playedMove),
          engineEvidence =
            Some(
              EngineEvidence(
                depth = 18,
                variations = List(VariationLine(moves = line, scoreCp = 70))
              )
            )
        )
      val strategyPack =
        StrategyPackBuilder
          .build(data, ctx)
          .getOrElse(fail(s"strategy pack missing for $kind"))
      val idea =
        strategyPack.strategicIdeas
          .find(_.relationKind.contains(kind))
          .getOrElse(fail(s"missing $kind relation idea: ${strategyPack.strategicIdeas}"))

      assertEquals(idea.targetSquare, Some(target), clue(idea))
      assert(idea.relationSupport.exists(_.relationKind == kind), clue(idea))

      val surface = build(ctx = ctx, strategyPack = Some(strategyPack))
      val cue =
        surface.summaryRows
          .find(_.authority.flatMap(_.token).contains(kind))
          .getOrElse(fail(s"missing $kind relation summary cue: ${surface.summaryRows}"))
      val row =
        surface.advancedRows
          .find(_.authority.flatMap(_.token).contains(kind))
          .getOrElse(fail(s"missing $kind relation advanced row: ${surface.advancedRows}"))

      assertEquals(cue.authority.flatMap(_.target), Some(target), clue(cue))
      assert(cue.text.contains(summaryFragment), clue(cue))
      assert(row.text.contains(advancedFragment), clue(row))
      assertEquals(row.source, None, clue(row))
    }
  }

  test("implemented relation catalog witnesses reach player-facing surface from exact board fixtures") {
    final case class RelationSurfaceScenario(
        kind: String,
        fen: String,
        line: List[String],
        targets: List[String] = Nil,
        whiteToMove: Boolean = true,
        expectedTarget: Option[String] = None
    )

    val scenarios =
      List(
        RelationSurfaceScenario(
          MoveReviewExchangeAnalyzer.RelationKind.DefenderTrade,
          "3k1b1r/p2b1ppp/1n3n2/4p3/8/1R4P1/P1QPqPBP/2B2RK1 w - - 0 17",
          List("c1a3", "f8a3", "b3a3", "h8h7"),
          targets = List("c5"),
          expectedTarget = Some("c5")
        ),
        RelationSurfaceScenario(
          MoveReviewExchangeAnalyzer.RelationKind.BadPieceLiquidation,
          "5b2/4k1pp/8/8/3P4/1R2P3/P4PPP/2B3K1 w - - 0 1",
          List("c1a3", "e7f7", "a3f8", "f7f8", "h1h2")
        ),
        RelationSurfaceScenario(
          MoveReviewExchangeAnalyzer.RelationKind.Overload,
          "k7/7p/5n2/3p4/8/8/8/3Q2K1 w - - 0 1",
          List("d1d3"),
          targets = List("d5", "h7")
        ),
        RelationSurfaceScenario(
          MoveReviewExchangeAnalyzer.RelationKind.Deflection,
          "3k1b1r/p2b1ppp/1n3n2/4p3/8/1R4P1/P1QPqPBP/2B2RK1 w - - 0 17",
          List("c1a3", "f8a3"),
          targets = List("g7"),
          expectedTarget = Some("g7")
        ),
        RelationSurfaceScenario(
          MoveReviewExchangeAnalyzer.RelationKind.DiscoveredAttack,
          "k7/7q/8/8/8/3N4/8/1B4K1 w - - 0 1",
          List("d3f4"),
          expectedTarget = Some("h7")
        ),
        RelationSurfaceScenario(
          MoveReviewExchangeAnalyzer.RelationKind.DoubleCheck,
          "4k3/8/8/8/4N3/8/8/4R1K1 w - - 0 1",
          List("e4f6"),
          expectedTarget = Some("e8")
        ),
        RelationSurfaceScenario(
          MoveReviewExchangeAnalyzer.RelationKind.BackRankMate,
          "6k1/5ppp/8/8/8/8/8/4R1K1 w - - 0 1",
          List("e1e8"),
          expectedTarget = Some("g8")
        ),
        RelationSurfaceScenario(
          MoveReviewExchangeAnalyzer.RelationKind.MateNet,
          "6rk/6pp/7N/8/8/8/8/6K1 w - - 0 1",
          List("h6f7"),
          expectedTarget = Some("h8")
        ),
        RelationSurfaceScenario(
          MoveReviewExchangeAnalyzer.RelationKind.GreekGift,
          "6k1/7p/8/8/8/3B1N2/8/3QK3 w - - 0 1",
          List("d3h7", "g8h7", "f3g5", "h7g8", "d1h5"),
          expectedTarget = Some("h7")
        ),
        RelationSurfaceScenario(
          MoveReviewExchangeAnalyzer.RelationKind.Zwischenzug,
          "6k1/8/8/7r/8/8/8/4Q1K1 w - - 0 1",
          List("e1e8", "g8h7", "e8h5"),
          targets = List("h5"),
          expectedTarget = Some("h5")
        ),
        RelationSurfaceScenario(
          MoveReviewExchangeAnalyzer.RelationKind.Fork,
          "k7/4r3/8/8/3N3q/8/8/6K1 w - - 0 1",
          List("d4f5"),
          targets = List("e7", "h4")
        ),
        RelationSurfaceScenario(
          MoveReviewExchangeAnalyzer.RelationKind.HangingPiece,
          "k7/8/8/5b2/2B5/8/8/6K1 w - - 0 1",
          List("c4d3"),
          targets = List("f5")
        ),
        RelationSurfaceScenario(
          MoveReviewExchangeAnalyzer.RelationKind.TrappedPiece,
          "n3k3/2p5/1p6/8/8/8/4K3/7R w - - 0 1",
          List("h1a1"),
          targets = List("a8"),
          expectedTarget = Some("a8")
        ),
        RelationSurfaceScenario(
          MoveReviewExchangeAnalyzer.RelationKind.Domination,
          "n3k3/2p5/8/8/8/8/4K3/7R w - - 0 1",
          List("h1a1"),
          targets = List("a8"),
          expectedTarget = Some("a8")
        ),
        RelationSurfaceScenario(
          MoveReviewExchangeAnalyzer.RelationKind.StalemateTrap,
          "7k/5K2/8/6Q1/8/8/8/8 w - - 0 1",
          List("g5g6"),
          targets = List("h8"),
          expectedTarget = Some("h8")
        ),
        RelationSurfaceScenario(
          MoveReviewExchangeAnalyzer.RelationKind.PerpetualCheck,
          "7k/8/8/8/8/8/8/4Q1K1 w - - 0 1",
          List("e1e8", "h8h7", "e8h5", "h7g8", "h5e8", "g8h7", "e8h5"),
          expectedTarget = Some("h7")
        ),
        RelationSurfaceScenario(
          MoveReviewExchangeAnalyzer.RelationKind.XRay,
          "6k1/5ppp/6q1/5n2/8/8/8/1B5K w - - 0 1",
          List("b1e4"),
          expectedTarget = Some("g6")
        ),
        RelationSurfaceScenario(
          MoveReviewExchangeAnalyzer.RelationKind.Clearance,
          "k7/7q/8/8/8/3N4/8/1B4K1 w - - 0 1",
          List("d3f4"),
          targets = List("h7"),
          expectedTarget = Some("h7")
        ),
        RelationSurfaceScenario(
          MoveReviewExchangeAnalyzer.RelationKind.Battery,
          "k7/7p/8/8/8/8/8/1B1Q2K1 w - - 0 1",
          List("d1d3"),
          targets = List("h7"),
          expectedTarget = Some("h7")
        ),
        RelationSurfaceScenario(
          MoveReviewExchangeAnalyzer.RelationKind.Pin,
          "4kb2/8/8/8/8/2N5/8/4K3 b - - 0 1",
          List("f8b4"),
          targets = List("c3"),
          whiteToMove = false
        ),
        RelationSurfaceScenario(
          MoveReviewExchangeAnalyzer.RelationKind.Skewer,
          "r6k/8/8/8/8/8/7K/4Q2R b - - 0 1",
          List("a8a1"),
          targets = List("e1"),
          whiteToMove = false
        ),
        RelationSurfaceScenario(
          MoveReviewExchangeAnalyzer.RelationKind.Interference,
          "k2r4/8/8/3q1N2/8/8/8/3Q2K1 w - - 0 1",
          List("f5d6"),
          targets = List("d5")
        ),
        RelationSurfaceScenario(
          MoveReviewExchangeAnalyzer.RelationKind.Decoy,
          "k7/8/8/3q4/5N2/8/4B3/3Q2K1 w - - 0 1",
          List("f4d3", "d5d3", "e2d3"),
          targets = List("d3"),
          expectedTarget = Some("d3")
        )
      )

    assertEquals(
      scenarios.map(_.kind).toSet,
      RelationObservationCatalog.Implemented.map(_.relationKind).toSet,
      clue("scenario list must stay aligned with the implemented relation catalog")
    )

    scenarios.foreach { scenario =>
      val playedMove = scenario.line.head
      val targetColor = if scenario.whiteToMove then Color.Black else Color.White
      val structuralTargets = scenario.targets.flatMap(Square.fromKey)
      val data =
        relationOnlyData(scenario.fen, playedMove).copy(
          isWhiteToMove = scenario.whiteToMove,
          structuralWeaknesses =
            Option.when(structuralTargets.nonEmpty)(
              WeakComplex(
                color = targetColor,
                squares = structuralTargets,
                isOutpost = false,
                cause = s"${scenario.kind} target"
              )
            ).toList
        )
      val ctx =
        relationOnlyContext(scenario.fen, playedMove).copy(
          playedSan = Some(playedMove),
          engineEvidence =
            Some(
              EngineEvidence(
                depth = 18,
                variations = List(VariationLine(moves = scenario.line, scoreCp = 70))
              )
            )
        )
      val strategyPack =
        StrategyPackBuilder
          .build(data, ctx)
          .getOrElse(fail(s"strategy pack missing for ${scenario.kind}"))
      val idea =
        strategyPack.strategicIdeas
          .find(_.relationKind.contains(scenario.kind))
          .getOrElse(fail(s"missing ${scenario.kind} relation idea: ${strategyPack.strategicIdeas}"))

      assert(idea.relationSupport.exists(_.relationKind == scenario.kind), clue(idea))
      scenario.expectedTarget.foreach(target => assertEquals(idea.targetSquare, Some(target), clue(idea)))

      val surface = build(ctx = ctx, strategyPack = Some(strategyPack))
      val cue =
        surface.summaryRows
          .find(_.authority.flatMap(_.token).contains(scenario.kind))
          .getOrElse(fail(s"missing ${scenario.kind} relation summary cue: ${surface.summaryRows}"))
      val row =
        surface.advancedRows
          .find(_.authority.flatMap(_.token).contains(scenario.kind))
          .getOrElse(fail(s"missing ${scenario.kind} relation advanced row: ${surface.advancedRows}"))

      assertEquals(cue.authority.map(_.kind), Some(MoveReviewSurfaceAuthority.StrategicRelation), clue(cue))
      assertEquals(row.authority.map(_.kind), Some(MoveReviewSurfaceAuthority.StrategicRelation), clue(row))
      assertEquals(row.source, None, clue(row))
      scenario.expectedTarget.foreach { target =>
        assertEquals(cue.authority.flatMap(_.target), Some(target), clue(cue))
        assertEquals(row.authority.flatMap(_.target), Some(target), clue(row))
      }
    }
  }

  private def relationSurfaceRow(
      relationKind: String,
      focus: List[String],
      target: Option[String],
      support: Option[StrategyRelationSupport]
  ): MoveReviewPlayerSurfaceRow =
    val descriptor =
      RelationObservationCatalog
        .descriptorForKind(relationKind)
        .getOrElse(fail(s"missing relation descriptor for $relationKind"))
    val relationIdea =
      StrategyIdeaSignal(
        ideaId = s"idea_$relationKind",
        ownerSide = "white",
        kind = descriptor.ideaKind,
        group =
          if descriptor.ideaKind == StrategicIdeaKind.LineOccupation then StrategicIdeaGroup.PieceAndLineManagement
          else StrategicIdeaGroup.InteractionAndTransformation,
        readiness = descriptor.readiness,
        focusSquares = focus,
        confidence = descriptor.confidence,
        evidenceRefs = descriptor.wireEvidenceRefs,
        targetSquare = target,
        relationKind = Some(relationKind),
        relationFocusSquares = focus,
        relationSupport = support
      )
    build(
      strategyPack =
        Some(
          StrategyPack(
            sideToMove = "white",
            strategicIdeas = List(relationIdea)
          )
        )
    ).advancedRows
      .find(_.authority.exists(_.kind == MoveReviewSurfaceAuthority.StrategicRelation))
      .getOrElse(fail(s"missing strategic relation row for $relationKind"))

  private def relationSupportFor(
      relationKind: String,
      focus: List[String],
      target: Option[String]
  ): StrategyRelationSupport =
    val targetSquare = target.orElse(focus.lastOption)
    StrategyRelationSupport(
      relationKind = relationKind,
      focusSquares = focus,
      targetSquare = targetSquare,
      attackerSquare = focus.headOption,
      defenderSquare = focus.lift(1),
      blockerSquare = focus.lift(1),
      beneficiarySquare = focus.headOption,
      clearedSquare = focus.lift(1),
      frontSquare = focus.headOption,
      backSquare = focus.lift(1),
      pinnedSquare = focus.lift(1),
      behindSquare = targetSquare,
      kingSquare = targetSquare,
      baitSquare = focus.lift(1).orElse(targetSquare),
      luredFromSquare = focus.lastOption,
      executionFromSquare = focus.headOption,
      executionToSquare = targetSquare
    )

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

  test("prophylaxis row requires typed restriction family instead of plan-name inference") {
    val surface =
      build(
        evaluatedPlans =
          List(
            evaluated(
              plan(
                name = "prophylaxis restraint",
                executionSteps = List("Keep the rook on d1"),
                preconditions = List("The d-file remains controlled"),
                themeL1 = PlanTaxonomy.PlanTheme.Unknown.id
              ),
              UserFacingPlanEligibility.ProbeBacked,
              supportProbeIds = List("probe_1")
            )
          )
      )

    assert(surface.advancedRows.exists(_.label == "Execution"), clue(surface.advancedRows))
    assert(surface.advancedRows.exists(_.label == "Objective"), clue(surface.advancedRows))
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
