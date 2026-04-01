package lila.llm.analysis

import chess.Square
import munit.FunSuite

import lila.llm.*
import lila.llm.model.*
import lila.llm.model.authoring.*
import lila.llm.model.strategic.PreventedPlan

class RestrictedDefenseConversionBroadValidationTest extends FunSuite:

  private enum PhaseCell:
    case LateMiddlegame
    case TransitionEndgameAdjacent

  private enum EvalPostureCell:
    case ClearlyWinning
    case SlightlyBetterNonConversionReady
    case EqualOrUnclear

  private enum TextureCell:
    case TechnicalSimplification
    case CounterplaySuppression
    case QuietImprovementBeforeConversion
    case TacticalLookingShouldFailOrDefer
    case MoveOrderSensitive

  private enum CriticismCell:
    case CooperativeDefense
    case PvRestatement
    case HiddenDefensiveResource
    case MoveOrderFragility
    case StitchedDefendedBranch
    case LocalToGlobalOverreach
    case SurfaceReinflation

  private enum SurfaceCell:
    case Bookmaker
    case Chronicle
    case Active
    case WholeGame

  private enum CoverageState:
    case Covered
    case Deferred
    case Failed

  private final case class CorpusScenario(
      id: String,
      planId: String,
      planName: String,
      subplanId: Option[String],
      phase: PhaseCell,
      evalPosture: EvalPostureCell,
      texture: TextureCell,
      criticisms: Set[CriticismCell],
      probes: List[ProbeResult],
      preventedPlans: List[PreventedPlan],
      evalCp: Int,
      expectedCertified: Boolean,
      expectedFails: Set[String]
  )

  private final case class SurfaceFixtureExpectation(
      id: String,
      expectedPlannerPrimary: Option[AuthorQuestionKind],
      expectedChroniclePlannerOwned: Boolean,
      expectedActivePrimary: Option[AuthorQuestionKind],
      expectedBookmakerPlannerOwned: Boolean,
      expectedFallbackClaim: Option[String]
  )

  private val emptyParts =
    CommentaryEngine.HybridNarrativeParts(
      lead = "Lead",
      defaultBridge = "Bridge",
      criticalBranch = None,
      body = "Body",
      primaryPlan = None,
      focusedOutline = NarrativeOutline(beats = Nil),
      phase = "Endgame",
      tacticalPressure = false,
      cpWhite = Some(40),
      bead = 1
    )

  private def conversionPlan(
      scenario: CorpusScenario
  ): PlanEvidenceEvaluator.EvaluatedPlan =
    PlanEvidenceEvaluator.EvaluatedPlan(
      hypothesis =
        PlanHypothesis(
          planId = scenario.planId,
          planName = scenario.planName,
          rank = 1,
          score = 0.84,
          preconditions = Nil,
          executionSteps = List("Convert the edge without giving counterplay back."),
          failureModes = List("If the move order slips, the defense reappears."),
          viability = PlanViability(score = 0.8, label = "high", risk = "b1b validation"),
          evidenceSources = List("theme:advantage_transformation"),
          themeL1 = ThemeTaxonomy.ThemeL1.AdvantageTransformation.id,
          subplanId = scenario.subplanId
        ),
      status = PlanEvidenceEvaluator.PlanEvidenceStatus.PlayableEvidenceBacked,
      userFacingEligibility = PlanEvidenceEvaluator.UserFacingPlanEligibility.ProbeBacked,
      reason = "b1b broad validation",
      supportProbeIds = scenario.probes.map(_.id),
      refuteProbeIds = Nil,
      missingSignals = Nil,
      pvCoupled = false,
      themeL1 = ThemeTaxonomy.ThemeL1.AdvantageTransformation.id,
      subplanId = scenario.subplanId,
      claimCertification =
        PlanEvidenceEvaluator.ClaimCertification(
          certificateStatus = PlayerFacingCertificateStatus.Valid,
          quantifier = PlayerFacingClaimQuantifier.Universal,
          modalityTier = PlayerFacingClaimModalityTier.Advances,
          attributionGrade = PlayerFacingClaimAttributionGrade.Distinctive,
          stabilityGrade = PlayerFacingClaimStabilityGrade.Stable,
          provenanceClass = PlayerFacingClaimProvenanceClass.ProbeBacked,
          ontologyFamily = PlayerFacingClaimOntologyFamily.PlanAdvance
        )
    )

  private def replyProbe(
      id: String,
      purpose: String = "convert_reply_multipv",
      bestReplyPv: List[String],
      replyPvs: Option[List[List[String]]],
      collapseReason: Option[String] = None,
      futureSnapshot: Option[FutureSnapshot] =
        Some(
          FutureSnapshot(
            resolvedThreatKinds = List("Counterplay"),
            newThreatKinds = Nil,
            targetsDelta = TargetsDelta(Nil, Nil, List("f6"), Nil),
            planBlockersRemoved = List("defender_trade_complete"),
            planPrereqsMet = List("conversion_route_stable")
          )
        ),
      evalCp: Int = 260
  ): ProbeResult =
    ProbeResult(
      id = id,
      evalCp = evalCp,
      bestReplyPv = bestReplyPv,
      replyPvs = replyPvs,
      deltaVsBaseline = 18,
      keyMotifs = List("conversion_window"),
      purpose = Some(purpose),
      l1Delta =
        Some(
          L1DeltaSnapshot(
            materialDelta = 0,
            kingSafetyDelta = 0,
            centerControlDelta = 1,
            openFilesDelta = 1,
            mobilityDelta = -1,
            collapseReason = collapseReason
          )
        ),
      futureSnapshot = futureSnapshot
    )

  private def preventedPlan(
      counterplayScoreDrop: Int = 120,
      breakNeutralized: Option[String] = Some("...c5"),
      deniedResourceClass: Option[String] = Some("break")
  ): PreventedPlan =
    PreventedPlan(
      planId = "deny_counterplay",
      deniedSquares = List(Square.fromKey("c5").get),
      breakNeutralized = breakNeutralized,
      mobilityDelta = -2,
      counterplayScoreDrop = counterplayScoreDrop,
      preventedThreatType = Some("counterplay"),
      deniedResourceClass = deniedResourceClass,
      defensiveSufficiency = Some(75),
      breakNeutralizationStrength = Some(75),
      sourceScope = FactScope.Now
    )

  private val corpusScenarios =
    List(
      CorpusScenario(
        id = "endgame_technical_true_conversion",
        planId = "opposite_bishops_conversion",
        planName = "Convert on the dark squares",
        subplanId = Some(ThemeTaxonomy.SubplanId.OppositeBishopsConversion.id),
        phase = PhaseCell.TransitionEndgameAdjacent,
        evalPosture = EvalPostureCell.ClearlyWinning,
        texture = TextureCell.TechnicalSimplification,
        criticisms = Set.empty,
        probes =
          List(
            replyProbe(
              id = "probe_endgame_true",
              bestReplyPv = List("g7g8", "f3d1"),
              replyPvs = Some(List(List("g7g8", "f3d1"), List("d6c5", "f3d1")))
            )
          ),
        preventedPlans = List(preventedPlan(counterplayScoreDrop = 95, deniedResourceClass = Some("entry_square"))),
        evalCp = 260,
        expectedCertified = true,
        expectedFails = Set.empty
      ),
      CorpusScenario(
        id = "late_middlegame_counterplay_suppression",
        planId = "counterplay_suppression_conversion",
        planName = "Shut down queenside counterplay and convert",
        subplanId = Some(ThemeTaxonomy.SubplanId.InvasionTransition.id),
        phase = PhaseCell.LateMiddlegame,
        evalPosture = EvalPostureCell.ClearlyWinning,
        texture = TextureCell.CounterplaySuppression,
        criticisms = Set.empty,
        probes =
          List(
            replyProbe(
              id = "probe_counterplay_true",
              purpose = "defense_reply_multipv",
              bestReplyPv = List("c5c4", "a2a4"),
              replyPvs = Some(List(List("c5c4", "a2a4"), List("b5b4", "a2a4")))
            )
          ),
        preventedPlans = List(preventedPlan(counterplayScoreDrop = 140, breakNeutralized = Some("...c5"))),
        evalCp = 235,
        expectedCertified = true,
        expectedFails = Set.empty
      ),
      CorpusScenario(
        id = "quiet_preparation_non_conversion_ready",
        planId = "quiet_improvement_conversion_shell",
        planName = "Improve first before a conversion exists",
        subplanId = Some(ThemeTaxonomy.SubplanId.SimplificationConversion.id),
        phase = PhaseCell.LateMiddlegame,
        evalPosture = EvalPostureCell.SlightlyBetterNonConversionReady,
        texture = TextureCell.QuietImprovementBeforeConversion,
        criticisms = Set(CriticismCell.LocalToGlobalOverreach, CriticismCell.SurfaceReinflation),
        probes =
          List(
            replyProbe(
              id = "probe_quiet_edge",
              bestReplyPv = List("c5c4", "a2a4"),
              replyPvs = Some(List(List("c5c4", "a2a4"), List("b5b4", "a2a4"))),
              evalCp = 120
            )
          ),
        preventedPlans = List(preventedPlan(counterplayScoreDrop = 90)),
        evalCp = 120,
        expectedCertified = false,
        expectedFails = Set("local_to_global_overreach")
      ),
      CorpusScenario(
        id = "equal_tactical_looking_pv_restatement",
        planId = "tactical_shell_conversion",
        planName = "A tactical-looking conversion shell",
        subplanId = Some(ThemeTaxonomy.SubplanId.SimplificationConversion.id),
        phase = PhaseCell.LateMiddlegame,
        evalPosture = EvalPostureCell.EqualOrUnclear,
        texture = TextureCell.TacticalLookingShouldFailOrDefer,
        criticisms = Set(CriticismCell.PvRestatement, CriticismCell.LocalToGlobalOverreach),
        probes =
          List(
            replyProbe(
              id = "probe_tactical_equal",
              purpose = "theme_plan_validation",
              bestReplyPv = List("g7g6", "g3g7"),
              replyPvs = Some(List(List("g7g6", "g3g7"))),
              evalCp = 20
            )
          ),
        preventedPlans = Nil,
        evalCp = 20,
        expectedCertified = false,
        expectedFails = Set("pv_restatement_only", "local_to_global_overreach", "insufficient_counterplay_suppression")
      ),
      CorpusScenario(
        id = "cooperative_plan_fake",
        planId = "cooperative_conversion",
        planName = "A route that works only if the defender cooperates",
        subplanId = Some(ThemeTaxonomy.SubplanId.SimplificationConversion.id),
        phase = PhaseCell.TransitionEndgameAdjacent,
        evalPosture = EvalPostureCell.ClearlyWinning,
        texture = TextureCell.TechnicalSimplification,
        criticisms = Set(CriticismCell.CooperativeDefense),
        probes =
          List(
            replyProbe(
              id = "probe_cooperative",
              bestReplyPv = Nil,
              replyPvs = Some(List(List("g7g6", "g3g7"))),
              futureSnapshot = None
            )
          ),
        preventedPlans = Nil,
        evalCp = 250,
        expectedCertified = false,
        expectedFails = Set("cooperative_defense", "pv_restatement_only", "route_persistence_missing")
      ),
      CorpusScenario(
        id = "hidden_defense_resource",
        planId = "hidden_resource_conversion",
        planName = "A route with too many hidden holds",
        subplanId = Some(ThemeTaxonomy.SubplanId.SimplificationConversion.id),
        phase = PhaseCell.LateMiddlegame,
        evalPosture = EvalPostureCell.ClearlyWinning,
        texture = TextureCell.CounterplaySuppression,
        criticisms = Set(CriticismCell.HiddenDefensiveResource),
        probes =
          List(
            replyProbe(
              id = "probe_hidden",
              bestReplyPv = List("g7g6", "g3g7"),
              replyPvs =
                Some(
                  List(
                    List("g7g6", "g3g7"),
                    List("c7c6", "g3g7"),
                    List("h7h6", "g3g7")
                  )
                )
            )
          ),
        preventedPlans = Nil,
        evalCp = 240,
        expectedCertified = false,
        expectedFails = Set("hidden_defensive_resource", "insufficient_counterplay_suppression")
      ),
      CorpusScenario(
        id = "move_order_fragile_conversion",
        planId = "move_order_fragile_conversion",
        planName = "A conversion that collapses if the order slips",
        subplanId = Some(ThemeTaxonomy.SubplanId.SimplificationConversion.id),
        phase = PhaseCell.TransitionEndgameAdjacent,
        evalPosture = EvalPostureCell.ClearlyWinning,
        texture = TextureCell.MoveOrderSensitive,
        criticisms = Set(CriticismCell.MoveOrderFragility, CriticismCell.SurfaceReinflation),
        probes =
          List(
            replyProbe(
              id = "probe_fragile",
              bestReplyPv = List("g7g8", "f3d1"),
              replyPvs = Some(List(List("g7g8", "f3d1"), List("d6c5", "f3d1"))),
              collapseReason = Some("wrong order restores the drawing shell")
            )
          ),
        preventedPlans = List(preventedPlan(counterplayScoreDrop = 90)),
        evalCp = 255,
        expectedCertified = false,
        expectedFails = Set("move_order_fragility", "cooperative_defense")
      ),
      CorpusScenario(
        id = "stitched_defended_branch_conversion",
        planId = "stitched_defended_branch_conversion",
        planName = "A conversion shell with stitched persistence",
        subplanId = Some(ThemeTaxonomy.SubplanId.SimplificationConversion.id),
        phase = PhaseCell.TransitionEndgameAdjacent,
        evalPosture = EvalPostureCell.ClearlyWinning,
        texture = TextureCell.TechnicalSimplification,
        criticisms = Set(CriticismCell.StitchedDefendedBranch),
        probes =
          List(
            replyProbe(
              id = "probe_direct_best",
              bestReplyPv = List("g7g6", "g3g7"),
              replyPvs = Some(List(List("g7g6", "g3g7"))),
              futureSnapshot = None
            ),
            replyProbe(
              id = "probe_other_branch",
              bestReplyPv = List("c7c6", "g3g7"),
              replyPvs = Some(List(List("c7c6", "g3g7")))
            )
          ),
        preventedPlans = List(preventedPlan(counterplayScoreDrop = 120)),
        evalCp = 250,
        expectedCertified = false,
        expectedFails = Set("stitched_defended_branch", "route_persistence_missing")
      )
    )

  private val surfaceFixtureExpectations =
    List(
      SurfaceFixtureExpectation(
        id = "restricted_defense_conversion_positive",
        expectedPlannerPrimary = Some(AuthorQuestionKind.WhyThis),
        expectedChroniclePlannerOwned = true,
        expectedActivePrimary = Some(AuthorQuestionKind.WhyThis),
        expectedBookmakerPlannerOwned = true,
        expectedFallbackClaim = None
      ),
      SurfaceFixtureExpectation(
        id = "restricted_defense_conversion_fragile",
        expectedPlannerPrimary = None,
        expectedChroniclePlannerOwned = false,
        expectedActivePrimary = None,
        expectedBookmakerPlannerOwned = false,
        expectedFallbackClaim = Some("This puts the bishop on f3.")
      )
    )

  private def plannerFixture(
      id: String
  ): BookmakerProseGoldenFixtures.PlannerRuntimeFixture =
    BookmakerProseGoldenFixtures.plannerRuntimeFixtures
      .find(_.id == id)
      .getOrElse(fail(s"missing planner runtime fixture: $id"))

  private def computeCoverage[T](
      cells: Set[T],
      covered: Set[T],
      failed: Set[T] = Set.empty[T]
  ): Map[T, CoverageState] =
    cells.iterator.map { cell =>
      val state =
        if failed.contains(cell) then CoverageState.Failed
        else if covered.contains(cell) then CoverageState.Covered
        else CoverageState.Deferred
      cell -> state
    }.toMap

  corpusScenarios.foreach { scenario =>
    test(s"${scenario.id} broad validation contract and builder gate stay aligned") {
      val plan = conversionPlan(scenario)
      val certification =
        RestrictedDefenseConversionCertification
          .evaluate(
            plan = plan,
            probeResultsById = scenario.probes.map(probe => probe.id -> probe).toMap,
            preventedPlans = scenario.preventedPlans,
            evalCp = scenario.evalCp,
            isWhiteToMove = true
          )
          .getOrElse(fail(s"expected restricted-defense contract for ${scenario.id}"))
      val experiments =
        NarrativeContextBuilder.buildStrategicPlanExperiments(
          evaluated = List(plan),
          validatedProbeResults = scenario.probes,
          preventedPlans = scenario.preventedPlans,
          evalCp = scenario.evalCp,
          isWhiteToMove = true
        )

      assertEquals(certification.certified, scenario.expectedCertified, clue(certification))
      scenario.expectedFails.foreach { failure =>
        assert(certification.failsIf.contains(failure), clues(scenario.id, failure, certification))
      }
      assertEquals(experiments.size, 1, clue(experiments))
      assertEquals(
        experiments.head.evidenceTier,
        if scenario.expectedCertified then "evidence_backed" else "deferred",
        clues(scenario.id, certification, experiments)
      )
      assertEquals(
        StrategicNarrativePlanSupport
          .filterEvidenceBacked(List(plan.hypothesis), experiments)
          .nonEmpty,
        scenario.expectedCertified,
        clues(scenario.id, certification, experiments)
      )
    }
  }

  surfaceFixtureExpectations.foreach { expectation =>
    test(s"${expectation.id} real surface validation keeps restricted-defense conversion inside B1 bounds") {
      val fixture = plannerFixture(expectation.id)
      val outline =
        BookStyleRenderer.validatedOutline(
          fixture.ctx,
          strategyPack = fixture.strategyPack,
          truthContract = fixture.truthContract
        )
      val plannerInputs =
        QuestionPlannerInputsBuilder.build(
          fixture.ctx,
          fixture.strategyPack,
          fixture.truthContract
        )
      val rankedPlans =
        QuestionFirstCommentaryPlanner.plan(
          fixture.ctx,
          plannerInputs,
          fixture.truthContract
        )
      val chronicleSelection =
        GameChronicleCompressionPolicy.selectPlannerSurface(rankedPlans, plannerInputs)
      val chronicleArtifact =
        GameChronicleCompressionPolicy.renderWithTrace(
          ctx = fixture.ctx,
          parts = emptyParts.copy(focusedOutline = outline),
          strategyPack = fixture.strategyPack,
          truthContract = fixture.truthContract
        )
      val activeSelection =
        ActiveStrategicCoachingBriefBuilder.selectPlannerSurface(
          ActiveStrategicCoachingBriefBuilder.PlannerReplay(
            authorQuestions = fixture.ctx.authorQuestions,
            inputs = plannerInputs,
            rankedPlans = rankedPlans
          )
        )
      val bookmakerSlots =
        BookmakerLiveCompressionPolicy.buildSlots(
          fixture.ctx,
          outline,
          refs = None,
          strategyPack = fixture.strategyPack,
          truthContract = fixture.truthContract
        )
      val fallbackSlots =
        BookmakerLiveCompressionPolicy.buildSlotsOrFallback(
          fixture.ctx,
          outline,
          refs = None,
          strategyPack = fixture.strategyPack,
          truthContract = fixture.truthContract
        )
      val bookmakerClaim = BookmakerProseContract.stripMoveHeader(fallbackSlots.claim)
      val bookmakerParagraphs =
        BookmakerProseContract.splitParagraphs(
          LiveNarrativeCompressionCore.deterministicProse(fallbackSlots)
        )

      assertEquals(
        rankedPlans.primary.map(_.questionKind),
        expectation.expectedPlannerPrimary,
        clues(expectation.id, rankedPlans, plannerInputs.decisionFrame)
      )
      assertEquals(
        chronicleSelection.nonEmpty,
        expectation.expectedChroniclePlannerOwned,
        clues(expectation.id, chronicleSelection, chronicleArtifact.map(_.narrative))
      )
      assertEquals(
        activeSelection.map(_.primary.questionKind),
        expectation.expectedActivePrimary,
        clues(expectation.id, activeSelection, rankedPlans)
      )
      assertEquals(
        bookmakerSlots.nonEmpty,
        expectation.expectedBookmakerPlannerOwned,
        clues(expectation.id, bookmakerSlots, fallbackSlots)
      )

      expectation.expectedFallbackClaim match
        case Some(claim) =>
          assertEquals(bookmakerClaim, claim, clues(expectation.id, fallbackSlots))
          assertEquals(bookmakerParagraphs.size, 1, clues(expectation.id, bookmakerParagraphs))
        case None =>
          assert(bookmakerParagraphs.size >= 2, clues(expectation.id, bookmakerParagraphs, fallbackSlots))
          assertNotEquals(
            bookmakerClaim,
            "This puts the bishop on f3.",
            clues(expectation.id, bookmakerClaim, fallbackSlots)
          )
          assert(chronicleArtifact.exists(_.narrative.nonEmpty), clues(expectation.id, chronicleArtifact))
    }
  }

  test("restricted-defense conversion whole-game reuse stays support-only when certification failed closed") {
    val fixture = plannerFixture("restricted_defense_conversion_fragile")
    val support =
      CommentaryEngine.buildWholeGameConclusionSupport(
        moments =
          List(
            GameArcMoment(
              ply = 90,
              momentType = "QuietMove",
              narrative = "This puts the bishop on f3.",
              analysisData =
                ExtendedAnalysisData(
                  fen = fixture.ctx.fen,
                  nature = PositionNature(NatureType.Dynamic, 0.5, 0.5, "Dynamic position"),
                  motifs = Nil,
                  plans = Nil,
                  preventedPlans = Nil,
                  pieceActivity = Nil,
                  structuralWeaknesses = Nil,
                  compensation = None,
                  endgameFeatures = None,
                  practicalAssessment = None,
                  prevMove = None,
                  ply = 90,
                  evalCp = 255,
                  isWhiteToMove = true
                ),
              moveClassification = Some("Best"),
              cpBefore = Some(245),
              cpAfter = Some(255),
              transitionType = Some("conversion_followthrough"),
              strategyPack = fixture.strategyPack,
              signalDigest = fixture.strategyPack.flatMap(_.signalDigest),
              truthPhase = Some("conversion_followthrough"),
              surfacedMoveOwnsTruth = false,
              verifiedPayoffAnchor = None,
              compensationProseAllowed = false
            )
          ),
        strategicThreads = Nil,
        themes = List("Opposite-colored bishops")
      )

    assertEquals(support.decisiveShift, None)
    assertEquals(support.payoff, None)
  }

  test("B1b targeted coverage matrix stays fully accounted for") {
    val phaseCoverage =
      computeCoverage(
        PhaseCell.values.toSet,
        corpusScenarios.map(_.phase).toSet
      )
    val evalCoverage =
      computeCoverage(
        EvalPostureCell.values.toSet,
        corpusScenarios.map(_.evalPosture).toSet
      )
    val textureCoverage =
      computeCoverage(
        TextureCell.values.toSet,
        corpusScenarios.map(_.texture).toSet
      )
    val criticismCoverage =
      computeCoverage(
        CriticismCell.values.toSet,
        corpusScenarios.flatMap(_.criticisms).toSet ++ Set(CriticismCell.SurfaceReinflation)
      )
    val surfaceCoverage =
      computeCoverage(
        SurfaceCell.values.toSet,
        Set(SurfaceCell.Bookmaker, SurfaceCell.Chronicle, SurfaceCell.Active, SurfaceCell.WholeGame)
      )

    assertEquals(
      phaseCoverage,
      Map(
        PhaseCell.LateMiddlegame -> CoverageState.Covered,
        PhaseCell.TransitionEndgameAdjacent -> CoverageState.Covered
      )
    )
    assertEquals(
      evalCoverage,
      Map(
        EvalPostureCell.ClearlyWinning -> CoverageState.Covered,
        EvalPostureCell.SlightlyBetterNonConversionReady -> CoverageState.Covered,
        EvalPostureCell.EqualOrUnclear -> CoverageState.Covered
      )
    )
    assertEquals(
      textureCoverage,
      Map(
        TextureCell.TechnicalSimplification -> CoverageState.Covered,
        TextureCell.CounterplaySuppression -> CoverageState.Covered,
        TextureCell.QuietImprovementBeforeConversion -> CoverageState.Covered,
        TextureCell.TacticalLookingShouldFailOrDefer -> CoverageState.Covered,
        TextureCell.MoveOrderSensitive -> CoverageState.Covered
      )
    )
    assertEquals(
      criticismCoverage,
      Map(
        CriticismCell.CooperativeDefense -> CoverageState.Covered,
        CriticismCell.PvRestatement -> CoverageState.Covered,
        CriticismCell.HiddenDefensiveResource -> CoverageState.Covered,
        CriticismCell.MoveOrderFragility -> CoverageState.Covered,
        CriticismCell.StitchedDefendedBranch -> CoverageState.Covered,
        CriticismCell.LocalToGlobalOverreach -> CoverageState.Covered,
        CriticismCell.SurfaceReinflation -> CoverageState.Covered
      )
    )
    assertEquals(
      surfaceCoverage,
      Map(
        SurfaceCell.Bookmaker -> CoverageState.Covered,
        SurfaceCell.Chronicle -> CoverageState.Covered,
        SurfaceCell.Active -> CoverageState.Covered,
        SurfaceCell.WholeGame -> CoverageState.Covered
      )
    )
  }
