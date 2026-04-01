package lila.llm.analysis

import chess.Square
import munit.FunSuite

import lila.llm.*
import lila.llm.model.*
import lila.llm.model.authoring.*
import lila.llm.model.strategic.PreventedPlan

class DualAxisBindBroadValidationTest extends FunSuite:

  private enum PhaseCell:
    case LateMiddlegame
    case HeavyPieceMiddlegame
    case TransitionEndgameAdjacent
    case PureEndgame

  private enum EvalPostureCell:
    case ClearlyBetter
    case SlightlyBetter
    case EqualOrUnclear
    case DefendingSide

  private enum TextureCell:
    case BreakPlusEntryDualAxis
    case RestrictionFirstThenConversion
    case AxisIndependenceNearMiss
    case SingleAxisStrengthFake
    case TacticalReleaseFake
    case ValidationOnlyShell
    case WaitingMoveOnly
    case FortressLikeStaticHold
    case StitchedBranchShell
    case ColorComplexPretty
    case MobilityCageNoProgress

  private enum CriticismCell:
    case AxisIndependenceNotProven
    case DualAxisBurdenMissing
    case HiddenFreeingBreak
    case HiddenTacticalRelease
    case DirectBestDefenseMissing
    case MoveOrderFragility
    case PvRestatementOnly
    case WaitingMoveDisguisedAsBind
    case FortressLikeButNotWinning
    case LocalToGlobalOverreach
    case RouteContinuityMissing
    case StitchedDefendedBranch
    case SurfaceReinflation

  private enum SurfaceCell:
    case PlannerWhyThis
    case Bookmaker
    case Chronicle
    case Active
    case WholeGame

  private enum CoverageState:
    case Covered
    case Deferred
    case Unsafe

  private final case class CorpusScenario(
      id: String,
      planId: String,
      planName: String,
      ontologyFamily: PlayerFacingClaimOntologyFamily,
      phaseCell: PhaseCell,
      evalPosture: EvalPostureCell,
      texture: TextureCell,
      criticisms: Set[CriticismCell],
      phase: String,
      ply: Int,
      fen: String,
      probes: List[ProbeResult],
      preventedPlans: List[PreventedPlan],
      evalCp: Int,
      expectedCertified: Boolean,
      expectedFails: Set[String]
  )

  private val QueenlessLateMiddlegameFen =
    "2r2rk1/pp3pp1/2n1p2p/3p4/1p1P1P2/P1P1PN1P/1P4P1/2R2RK1 w - - 0 24"
  private val HeavyPieceMiddlegameFen =
    "r1bq1rk1/pp3ppp/2n1pn2/2bp4/3P4/2N1PN2/PPQ2PPP/R1B2RK1 w - - 0 12"
  private val TransitionFen =
    "8/5pk1/3b2p1/3P4/1P3P2/6P1/5BK1/8 w - - 0 45"
  private val emptyParts =
    CommentaryEngine.HybridNarrativeParts(
      lead = "Lead",
      defaultBridge = "Bridge",
      criticalBranch = None,
      body = "Body",
      primaryPlan = None,
      focusedOutline = NarrativeOutline(beats = Nil),
      phase = "Middlegame",
      tacticalPressure = false,
      cpWhite = Some(40),
      bead = 1
    )

  private def bindPlan(
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
          executionSteps = List("Stop the break first and keep the entry square closed."),
          failureModes = List("If either route reopens, the bind disappears."),
          viability = PlanViability(score = 0.8, label = "high", risk = "B3b broad"),
          evidenceSources = List("theme:restriction_prophylaxis"),
          themeL1 = ThemeTaxonomy.ThemeL1.RestrictionProphylaxis.id,
          subplanId = Some(ThemeTaxonomy.SubplanId.BreakPrevention.id)
        ),
      status = PlanEvidenceEvaluator.PlanEvidenceStatus.PlayableEvidenceBacked,
      userFacingEligibility = PlanEvidenceEvaluator.UserFacingPlanEligibility.ProbeBacked,
      reason = "B3b broad validation",
      supportProbeIds = scenario.probes.map(_.id),
      refuteProbeIds = Nil,
      missingSignals = Nil,
      pvCoupled = false,
      themeL1 = ThemeTaxonomy.ThemeL1.RestrictionProphylaxis.id,
      subplanId = Some(ThemeTaxonomy.SubplanId.BreakPrevention.id),
      claimCertification =
        PlanEvidenceEvaluator.ClaimCertification(
          certificateStatus = PlayerFacingCertificateStatus.Valid,
          quantifier = PlayerFacingClaimQuantifier.BestResponse,
          modalityTier = PlayerFacingClaimModalityTier.Advances,
          attributionGrade = PlayerFacingClaimAttributionGrade.Distinctive,
          stabilityGrade = PlayerFacingClaimStabilityGrade.Stable,
          provenanceClass = PlayerFacingClaimProvenanceClass.ProbeBacked,
          ontologyFamily = scenario.ontologyFamily
        )
    )

  private def directReplyProbe(
      id: String,
      bestReplyPv: List[String] = List("f8e8", "c1c8"),
      replyPvs: Option[List[List[String]]] =
        Some(
          List(
            List("f8e8", "c1c8"),
            List("a7a5", "g2g4")
          )
        ),
      collapseReason: Option[String] = None,
      futureSnapshot: Option[FutureSnapshot] =
        Some(
          FutureSnapshot(
            resolvedThreatKinds = List("Counterplay"),
            newThreatKinds = Nil,
            targetsDelta = TargetsDelta(Nil, Nil, List("c-file pressure"), List("b4")),
            planBlockersRemoved = List("...c5 break stays shut"),
            planPrereqsMet = List("b4 stays unavailable")
          )
        ),
      keyMotifs: List[String] = List("...c5 break denied", "b4 entry denied"),
      evalCp: Int = 190
  ): ProbeResult =
    ProbeResult(
      id = id,
      evalCp = evalCp,
      bestReplyPv = bestReplyPv,
      replyPvs = replyPvs,
      deltaVsBaseline = 12,
      keyMotifs = keyMotifs,
      purpose = Some("defense_reply_multipv"),
      l1Delta =
        Some(
          L1DeltaSnapshot(
            materialDelta = 0,
            kingSafetyDelta = 0,
            centerControlDelta = 1,
            openFilesDelta = 0,
            mobilityDelta = -2,
            collapseReason = collapseReason
          )
        ),
      futureSnapshot = futureSnapshot
    )

  private def validationProbe(
      id: String,
      purpose: String = ThemePlanProbePurpose.RouteDenialValidation,
      bestReplyPv: List[String] = List("f8e8", "c1c8"),
      replyPvs: Option[List[List[String]]] =
        Some(
          List(
            List("f8e8", "c1c8"),
            List("a7a5", "g2g4")
          )
        ),
      futureSnapshot: Option[FutureSnapshot] =
        Some(
          FutureSnapshot(
            resolvedThreatKinds = List("Counterplay"),
            newThreatKinds = Nil,
            targetsDelta = TargetsDelta(Nil, Nil, List("c-file pressure"), List("b4")),
            planBlockersRemoved = List("...c5 break stays shut"),
            planPrereqsMet = List("conversion route stabilizes once b4 stays unavailable")
          )
        ),
      keyMotifs: List[String] = List("...c5 break denied", "b4 entry denied", "conversion route stabilizes"),
      evalCp: Int = 188
  ): ProbeResult =
    ProbeResult(
      id = id,
      evalCp = evalCp,
      bestReplyPv = bestReplyPv,
      replyPvs = replyPvs,
      deltaVsBaseline = 10,
      keyMotifs = keyMotifs,
      purpose = Some(purpose),
      l1Delta =
        Some(
          L1DeltaSnapshot(
            materialDelta = 0,
            kingSafetyDelta = 0,
            centerControlDelta = 1,
            openFilesDelta = 0,
            mobilityDelta = -2,
            collapseReason = None
          )
        ),
      futureSnapshot = futureSnapshot
    )

  private def preventedBreakPlan(
      label: String = "...c5",
      counterplayScoreDrop: Int = 145,
      breakNeutralizationStrength: Option[Int] = Some(84),
      defensiveSufficiency: Option[Int] = Some(80)
  ): PreventedPlan =
    PreventedPlan(
      planId = "deny_break",
      deniedSquares = List(Square.fromKey("c5").get),
      breakNeutralized = Some(label),
      mobilityDelta = -2,
      counterplayScoreDrop = counterplayScoreDrop,
      preventedThreatType = Some("counterplay"),
      deniedResourceClass = Some("break"),
      breakNeutralizationStrength = breakNeutralizationStrength,
      defensiveSufficiency = defensiveSufficiency,
      sourceScope = FactScope.Now
    )

  private def preventedEntryPlan(
      square: String = "b4",
      counterplayScoreDrop: Int = 130,
      breakNeutralizationStrength: Option[Int] = Some(76),
      defensiveSufficiency: Option[Int] = Some(74)
  ): PreventedPlan =
    PreventedPlan(
      planId = "deny_entry",
      deniedSquares = List(Square.fromKey(square).get),
      breakNeutralized = None,
      mobilityDelta = -2,
      counterplayScoreDrop = counterplayScoreDrop,
      preventedThreatType = Some("counterplay"),
      deniedResourceClass = Some("entry_square"),
      breakNeutralizationStrength = breakNeutralizationStrength,
      defensiveSufficiency = defensiveSufficiency,
      sourceScope = FactScope.Now
    )

  private val corpusScenarios =
    List(
      CorpusScenario(
        id = "true_dual_axis_clamp",
        planId = "true_dual_axis_clamp",
        planName = "Stop the ...c5 break and keep b4 closed",
        ontologyFamily = PlayerFacingClaimOntologyFamily.RouteDenial,
        phaseCell = PhaseCell.LateMiddlegame,
        evalPosture = EvalPostureCell.ClearlyBetter,
        texture = TextureCell.BreakPlusEntryDualAxis,
        criticisms = Set.empty,
        phase = "middlegame",
        ply = 30,
        fen = QueenlessLateMiddlegameFen,
        probes = List(directReplyProbe("probe_true_reply"), validationProbe("probe_true_validation")),
        preventedPlans = List(preventedBreakPlan(), preventedEntryPlan()),
        evalCp = 190,
        expectedCertified = true,
        expectedFails = Set.empty
      ),
      CorpusScenario(
        id = "axis_independence_not_proven",
        planId = "axis_independence_not_proven",
        planName = "Close the c-file bind completely",
        ontologyFamily = PlayerFacingClaimOntologyFamily.RouteDenial,
        phaseCell = PhaseCell.LateMiddlegame,
        evalPosture = EvalPostureCell.ClearlyBetter,
        texture = TextureCell.AxisIndependenceNearMiss,
        criticisms = Set(CriticismCell.AxisIndependenceNotProven),
        phase = "middlegame",
        ply = 29,
        fen = QueenlessLateMiddlegameFen,
        probes = List(directReplyProbe("probe_axis_reply"), validationProbe("probe_axis_validation")),
        preventedPlans = List(preventedBreakPlan(), preventedEntryPlan(square = "c5")),
        evalCp = 188,
        expectedCertified = false,
        expectedFails = Set("axis_independence_not_proven")
      ),
      CorpusScenario(
        id = "single_axis_strength_fake",
        planId = "single_axis_strength_fake",
        planName = "Stop the break and hint at b4 without really denying it",
        ontologyFamily = PlayerFacingClaimOntologyFamily.RouteDenial,
        phaseCell = PhaseCell.LateMiddlegame,
        evalPosture = EvalPostureCell.ClearlyBetter,
        texture = TextureCell.SingleAxisStrengthFake,
        criticisms = Set(CriticismCell.DualAxisBurdenMissing),
        phase = "middlegame",
        ply = 30,
        fen = QueenlessLateMiddlegameFen,
        probes = List(directReplyProbe("probe_single_axis_reply"), validationProbe("probe_single_axis_validation")),
        preventedPlans =
          List(
            preventedBreakPlan(),
            preventedEntryPlan(
              counterplayScoreDrop = 32,
              breakNeutralizationStrength = Some(38),
              defensiveSufficiency = Some(34)
            )
          ),
        evalCp = 190,
        expectedCertified = false,
        expectedFails = Set("dual_axis_burden_missing")
      ),
      CorpusScenario(
        id = "hidden_freeing_break",
        planId = "hidden_freeing_break",
        planName = "Bind the queenside shell",
        ontologyFamily = PlayerFacingClaimOntologyFamily.RouteDenial,
        phaseCell = PhaseCell.LateMiddlegame,
        evalPosture = EvalPostureCell.ClearlyBetter,
        texture = TextureCell.BreakPlusEntryDualAxis,
        criticisms = Set(CriticismCell.HiddenFreeingBreak),
        phase = "middlegame",
        ply = 28,
        fen = QueenlessLateMiddlegameFen,
        probes = List(directReplyProbe("probe_hidden_reply"), validationProbe("probe_hidden_validation")),
        preventedPlans =
          List(
            preventedBreakPlan(),
            preventedEntryPlan(),
            preventedBreakPlan(label = "...e5")
          ),
        evalCp = 185,
        expectedCertified = false,
        expectedFails = Set("hidden_freeing_break")
      ),
      CorpusScenario(
        id = "hidden_tactical_release",
        planId = "hidden_tactical_release",
        planName = "Restrain first, then convert",
        ontologyFamily = PlayerFacingClaimOntologyFamily.RouteDenial,
        phaseCell = PhaseCell.LateMiddlegame,
        evalPosture = EvalPostureCell.ClearlyBetter,
        texture = TextureCell.TacticalReleaseFake,
        criticisms = Set(CriticismCell.HiddenTacticalRelease),
        phase = "middlegame",
        ply = 29,
        fen = QueenlessLateMiddlegameFen,
        probes =
          List(
            directReplyProbe(
              "probe_tactical_reply",
              futureSnapshot =
                Some(
                  FutureSnapshot(
                    resolvedThreatKinds = List("Counterplay"),
                    newThreatKinds = List("Perpetual"),
                    targetsDelta = TargetsDelta(List("g2"), Nil, List("c-file pressure"), List("b4")),
                    planBlockersRemoved = List("...c5 break stays shut"),
                    planPrereqsMet = List("b4 stays unavailable")
                  )
                ),
              keyMotifs = List("...c5 break denied", "b4 entry denied", "exchange sac resource")
            ),
            validationProbe("probe_tactical_validation")
          ),
        preventedPlans = List(preventedBreakPlan(), preventedEntryPlan()),
        evalCp = 182,
        expectedCertified = false,
        expectedFails = Set("hidden_tactical_release")
      ),
      CorpusScenario(
        id = "move_order_fragile_bind",
        planId = "move_order_fragile_bind",
        planName = "Hold the bind in the right order",
        ontologyFamily = PlayerFacingClaimOntologyFamily.RouteDenial,
        phaseCell = PhaseCell.LateMiddlegame,
        evalPosture = EvalPostureCell.ClearlyBetter,
        texture = TextureCell.RestrictionFirstThenConversion,
        criticisms = Set(CriticismCell.MoveOrderFragility),
        phase = "middlegame",
        ply = 28,
        fen = QueenlessLateMiddlegameFen,
        probes =
          List(
            directReplyProbe("probe_fragile_reply", collapseReason = Some("wrong order reopens the queenside")),
            validationProbe("probe_fragile_validation")
          ),
        preventedPlans = List(preventedBreakPlan(), preventedEntryPlan()),
        evalCp = 188,
        expectedCertified = false,
        expectedFails = Set("move_order_fragility", "cooperative_defense")
      ),
      CorpusScenario(
        id = "pv_restatement_only_bind",
        planId = "pv_restatement_only_bind",
        planName = "A quiet bind shell",
        ontologyFamily = PlayerFacingClaimOntologyFamily.RouteDenial,
        phaseCell = PhaseCell.LateMiddlegame,
        evalPosture = EvalPostureCell.ClearlyBetter,
        texture = TextureCell.WaitingMoveOnly,
        criticisms = Set(CriticismCell.PvRestatementOnly),
        phase = "middlegame",
        ply = 27,
        fen = QueenlessLateMiddlegameFen,
        probes =
          List(
            directReplyProbe("probe_pv_reply"),
            validationProbe(
              "probe_pv_theme",
              purpose = ThemePlanProbePurpose.ThemePlanValidation,
              futureSnapshot = None,
              keyMotifs = List("quiet move")
            )
          ),
        preventedPlans = List(preventedBreakPlan(), preventedEntryPlan()),
        evalCp = 178,
        expectedCertified = false,
        expectedFails = Set("pv_restatement_only", "route_continuity_missing", "surface_reinflation")
      ),
      CorpusScenario(
        id = "validation_only_shell",
        planId = "validation_only_shell",
        planName = "Quietly freeze both routes before converting",
        ontologyFamily = PlayerFacingClaimOntologyFamily.RouteDenial,
        phaseCell = PhaseCell.LateMiddlegame,
        evalPosture = EvalPostureCell.ClearlyBetter,
        texture = TextureCell.ValidationOnlyShell,
        criticisms = Set(CriticismCell.DirectBestDefenseMissing),
        phase = "middlegame",
        ply = 30,
        fen = QueenlessLateMiddlegameFen,
        probes =
          List(
            validationProbe("probe_validation_only"),
            validationProbe(
              "probe_convert_only",
              purpose = "convert_reply_multipv",
              keyMotifs = List("...c5 break denied", "b4 entry denied", "conversion route stabilizes")
            )
          ),
        preventedPlans = List(preventedBreakPlan(), preventedEntryPlan()),
        evalCp = 186,
        expectedCertified = false,
        expectedFails =
          Set(
            "direct_best_defense_missing",
            "cooperative_defense",
            "route_continuity_missing",
            "surface_reinflation"
          )
      ),
      CorpusScenario(
        id = "waiting_move_only",
        planId = "waiting_move_only",
        planName = "Improve quietly and wait",
        ontologyFamily = PlayerFacingClaimOntologyFamily.RouteDenial,
        phaseCell = PhaseCell.LateMiddlegame,
        evalPosture = EvalPostureCell.ClearlyBetter,
        texture = TextureCell.WaitingMoveOnly,
        criticisms = Set(CriticismCell.WaitingMoveDisguisedAsBind),
        phase = "middlegame",
        ply = 30,
        fen = QueenlessLateMiddlegameFen,
        probes =
          List(
            directReplyProbe(
              "probe_waiting_reply",
              futureSnapshot =
                Some(
                  FutureSnapshot(
                    resolvedThreatKinds = Nil,
                    newThreatKinds = Nil,
                    targetsDelta = TargetsDelta(Nil, Nil, Nil, Nil),
                    planBlockersRemoved = Nil,
                    planPrereqsMet = Nil
                  )
                ),
              keyMotifs = List("quiet move")
            ),
            validationProbe(
              "probe_waiting_validation",
              futureSnapshot =
                Some(
                  FutureSnapshot(
                    resolvedThreatKinds = Nil,
                    newThreatKinds = Nil,
                    targetsDelta = TargetsDelta(Nil, Nil, Nil, Nil),
                    planBlockersRemoved = Nil,
                    planPrereqsMet = Nil
                  )
                ),
              keyMotifs = List("quiet move")
            )
          ),
        preventedPlans =
          List(
            preventedBreakPlan(counterplayScoreDrop = 35, breakNeutralizationStrength = Some(38), defensiveSufficiency = Some(36)),
            preventedEntryPlan(counterplayScoreDrop = 30, breakNeutralizationStrength = Some(40), defensiveSufficiency = Some(34))
          ),
        evalCp = 186,
        expectedCertified = false,
        expectedFails = Set("waiting_move_disguised_as_bind", "route_continuity_missing")
      ),
      CorpusScenario(
        id = "fortress_like_static_hold",
        planId = "fortress_like_static_hold",
        planName = "Freeze the queenside forever",
        ontologyFamily = PlayerFacingClaimOntologyFamily.RouteDenial,
        phaseCell = PhaseCell.LateMiddlegame,
        evalPosture = EvalPostureCell.ClearlyBetter,
        texture = TextureCell.FortressLikeStaticHold,
        criticisms = Set(CriticismCell.FortressLikeButNotWinning, CriticismCell.RouteContinuityMissing),
        phase = "middlegame",
        ply = 31,
        fen = QueenlessLateMiddlegameFen,
        probes =
          List(
            directReplyProbe("probe_fortress_reply"),
            validationProbe(
              "probe_fortress_validation",
              futureSnapshot =
                Some(
                  FutureSnapshot(
                    resolvedThreatKinds = List("Counterplay"),
                    newThreatKinds = Nil,
                    targetsDelta = TargetsDelta(Nil, Nil, Nil, List("b4")),
                    planBlockersRemoved = List("the shell just keeps holding"),
                    planPrereqsMet = List("the bind stays intact")
                  )
                ),
              keyMotifs = List("static hold", "bind stays intact")
            )
          ),
        preventedPlans = List(preventedBreakPlan(), preventedEntryPlan()),
        evalCp = 190,
        expectedCertified = false,
        expectedFails = Set("route_continuity_missing", "fortress_like_but_not_winning", "surface_reinflation")
      ),
      CorpusScenario(
        id = "route_continuity_missing",
        planId = "route_continuity_missing",
        planName = "Keep the shell but do not rush",
        ontologyFamily = PlayerFacingClaimOntologyFamily.RouteDenial,
        phaseCell = PhaseCell.LateMiddlegame,
        evalPosture = EvalPostureCell.ClearlyBetter,
        texture = TextureCell.RestrictionFirstThenConversion,
        criticisms = Set(CriticismCell.RouteContinuityMissing),
        phase = "middlegame",
        ply = 30,
        fen = QueenlessLateMiddlegameFen,
        probes =
          List(
            directReplyProbe("probe_route_reply"),
            validationProbe(
              "probe_route_validation",
              futureSnapshot =
                Some(
                  FutureSnapshot(
                    resolvedThreatKinds = List("Counterplay"),
                    newThreatKinds = Nil,
                    targetsDelta = TargetsDelta(Nil, Nil, Nil, List("b4")),
                    planBlockersRemoved = List("...c5 break stays shut"),
                    planPrereqsMet = List("counterplay stays muted")
                  )
                ),
              keyMotifs = List("...c5 break denied", "b4 entry denied")
            )
          ),
        preventedPlans = List(preventedBreakPlan(), preventedEntryPlan()),
        evalCp = 188,
        expectedCertified = false,
        expectedFails = Set("route_continuity_missing", "surface_reinflation")
      ),
      CorpusScenario(
        id = "stitched_defended_branch",
        planId = "stitched_defended_branch",
        planName = "Claim the bind from one defense branch and continue it on another",
        ontologyFamily = PlayerFacingClaimOntologyFamily.RouteDenial,
        phaseCell = PhaseCell.LateMiddlegame,
        evalPosture = EvalPostureCell.ClearlyBetter,
        texture = TextureCell.StitchedBranchShell,
        criticisms = Set(CriticismCell.StitchedDefendedBranch),
        phase = "middlegame",
        ply = 30,
        fen = QueenlessLateMiddlegameFen,
        probes =
          List(
            directReplyProbe("probe_stitched_reply"),
            validationProbe(
              "probe_stitched_validation",
              bestReplyPv = List("a7a5", "g2g4"),
              replyPvs =
                Some(
                  List(
                    List("a7a5", "g2g4"),
                    List("f8e8", "c1c8")
                  )
                )
            )
          ),
        preventedPlans = List(preventedBreakPlan(), preventedEntryPlan()),
        evalCp = 188,
        expectedCertified = false,
        expectedFails =
          Set(
            "stitched_defended_branch",
            "route_continuity_missing",
            "surface_reinflation"
          )
      ),
      CorpusScenario(
        id = "surface_reinflation_case",
        planId = "surface_reinflation_case",
        planName = "Leave Black with no counterplay in the whole position forever",
        ontologyFamily = PlayerFacingClaimOntologyFamily.RouteDenial,
        phaseCell = PhaseCell.LateMiddlegame,
        evalPosture = EvalPostureCell.ClearlyBetter,
        texture = TextureCell.BreakPlusEntryDualAxis,
        criticisms = Set(CriticismCell.SurfaceReinflation),
        phase = "middlegame",
        ply = 30,
        fen = QueenlessLateMiddlegameFen,
        probes = List(directReplyProbe("probe_surface_reply"), validationProbe("probe_surface_validation")),
        preventedPlans = List(preventedBreakPlan(), preventedEntryPlan()),
        evalCp = 188,
        expectedCertified = false,
        expectedFails = Set("surface_reinflation")
      ),
      CorpusScenario(
        id = "slightly_better_not_certifiable",
        planId = "slightly_better_not_certifiable",
        planName = "Restrain the break and b4 first",
        ontologyFamily = PlayerFacingClaimOntologyFamily.RouteDenial,
        phaseCell = PhaseCell.LateMiddlegame,
        evalPosture = EvalPostureCell.SlightlyBetter,
        texture = TextureCell.BreakPlusEntryDualAxis,
        criticisms = Set(CriticismCell.LocalToGlobalOverreach),
        phase = "middlegame",
        ply = 30,
        fen = QueenlessLateMiddlegameFen,
        probes = List(directReplyProbe("probe_slight_reply"), validationProbe("probe_slight_validation")),
        preventedPlans = List(preventedBreakPlan(counterplayScoreDrop = 120), preventedEntryPlan(counterplayScoreDrop = 105)),
        evalCp = 115,
        expectedCertified = false,
        expectedFails = Set("local_to_global_overreach")
      ),
      CorpusScenario(
        id = "heavy_piece_overreach_shell",
        planId = "heavy_piece_overreach_shell",
        planName = "Clamp the whole position",
        ontologyFamily = PlayerFacingClaimOntologyFamily.RouteDenial,
        phaseCell = PhaseCell.HeavyPieceMiddlegame,
        evalPosture = EvalPostureCell.ClearlyBetter,
        texture = TextureCell.BreakPlusEntryDualAxis,
        criticisms = Set(CriticismCell.LocalToGlobalOverreach),
        phase = "middlegame",
        ply = 18,
        fen = HeavyPieceMiddlegameFen,
        probes = List(directReplyProbe("probe_heavy_reply"), validationProbe("probe_heavy_validation")),
        preventedPlans = List(preventedBreakPlan(), preventedEntryPlan()),
        evalCp = 190,
        expectedCertified = false,
        expectedFails = Set("local_to_global_overreach")
      ),
      CorpusScenario(
        id = "transition_adjacent_shell",
        planId = "transition_adjacent_shell",
        planName = "Fix the bind before converting",
        ontologyFamily = PlayerFacingClaimOntologyFamily.RouteDenial,
        phaseCell = PhaseCell.TransitionEndgameAdjacent,
        evalPosture = EvalPostureCell.ClearlyBetter,
        texture = TextureCell.RestrictionFirstThenConversion,
        criticisms = Set(CriticismCell.LocalToGlobalOverreach),
        phase = "endgame",
        ply = 44,
        fen = TransitionFen,
        probes = List(directReplyProbe("probe_transition_reply"), validationProbe("probe_transition_validation")),
        preventedPlans = List(preventedBreakPlan(label = "...e5"), preventedEntryPlan(square = "d5")),
        evalCp = 170,
        expectedCertified = false,
        expectedFails = Set("local_to_global_overreach")
      ),
      CorpusScenario(
        id = "color_complex_pretty_but_uncertified",
        planId = "color_complex_pretty_but_uncertified",
        planName = "Control the dark squares forever",
        ontologyFamily = PlayerFacingClaimOntologyFamily.ColorComplexSqueeze,
        phaseCell = PhaseCell.LateMiddlegame,
        evalPosture = EvalPostureCell.ClearlyBetter,
        texture = TextureCell.ColorComplexPretty,
        criticisms = Set(CriticismCell.LocalToGlobalOverreach),
        phase = "middlegame",
        ply = 30,
        fen = QueenlessLateMiddlegameFen,
        probes = List(directReplyProbe("probe_color_reply"), validationProbe("probe_color_validation")),
        preventedPlans = List(preventedBreakPlan(), preventedEntryPlan()),
        evalCp = 186,
        expectedCertified = false,
        expectedFails = Set("local_to_global_overreach", "surface_reinflation")
      ),
      CorpusScenario(
        id = "mobility_cage_without_progress",
        planId = "mobility_cage_without_progress",
        planName = "Cage the pieces and wait",
        ontologyFamily = PlayerFacingClaimOntologyFamily.LongTermRestraint,
        phaseCell = PhaseCell.LateMiddlegame,
        evalPosture = EvalPostureCell.ClearlyBetter,
        texture = TextureCell.MobilityCageNoProgress,
        criticisms = Set(CriticismCell.RouteContinuityMissing),
        phase = "middlegame",
        ply = 30,
        fen = QueenlessLateMiddlegameFen,
        probes =
          List(
            directReplyProbe("probe_cage_reply"),
            validationProbe(
              "probe_cage_validation",
              futureSnapshot =
                Some(
                  FutureSnapshot(
                    resolvedThreatKinds = List("Counterplay"),
                    newThreatKinds = Nil,
                    targetsDelta = TargetsDelta(Nil, Nil, List("mobility clamp"), List("b4")),
                    planBlockersRemoved = List("...c5 break stays shut"),
                    planPrereqsMet = List("counterplay stays muted")
                  )
                ),
              keyMotifs = List("mobility clamp", "b4 entry denied")
            )
          ),
        preventedPlans = List(preventedBreakPlan(), preventedEntryPlan()),
        evalCp = 184,
        expectedCertified = false,
        expectedFails = Set("route_continuity_missing", "surface_reinflation")
      )
    )

  private def whyThisSurfaceCtx(evidenceTier: String, planName: String): NarrativeContext =
    BookmakerProseGoldenFixtures.prophylacticCut.ctx.copy(
      authorQuestions =
        List(
          AuthorQuestion(
            id = "q_b3b_why_this",
            kind = AuthorQuestionKind.WhyThis,
            priority = 100,
            question = "Why is a3 the right dual-axis bind move here?",
            evidencePurposes = List("reply_multipv")
          )
        ),
      authorEvidence =
        List(
          QuestionEvidence(
            questionId = "q_b3b_why_this",
            purpose = "reply_multipv",
            branches =
              List(
                EvidenceBranch(
                  keyMove = "line_1",
                  line = "24.a3 keeps ...c5 shut and b4 unavailable before White doubles on the c-file.",
                  evalCp = Some(60)
                )
              )
          )
        ),
      mainStrategicPlans =
        List(
          PlanHypothesis(
            planId = "dual_axis_bind",
            planName = planName,
            rank = 1,
            score = 0.84,
            preconditions = Nil,
            executionSteps = List("Stop the ...c5 break and keep b4 unavailable."),
            failureModes = List("If either route reopens, the bind disappears."),
            viability = PlanViability(score = 0.8, label = "high", risk = "surface"),
            evidenceSources = List("theme:restriction_prophylaxis"),
            themeL1 = ThemeTaxonomy.ThemeL1.RestrictionProphylaxis.id,
            subplanId = Some(ThemeTaxonomy.SubplanId.BreakPrevention.id)
          )
        ),
      strategicPlanExperiments =
        List(
          StrategicPlanExperiment(
            planId = "dual_axis_bind",
            themeL1 = ThemeTaxonomy.ThemeL1.RestrictionProphylaxis.id,
            subplanId = Some(ThemeTaxonomy.SubplanId.BreakPrevention.id),
            evidenceTier = evidenceTier,
            supportProbeCount = 2,
            bestReplyStable = evidenceTier == "evidence_backed",
            futureSnapshotAligned = evidenceTier == "evidence_backed",
            counterBreakNeutralized = true,
            moveOrderSensitive = evidenceTier != "evidence_backed",
            experimentConfidence = if evidenceTier == "evidence_backed" then 0.90 else 0.30
          )
        )
    )

  private def surfaceReinflationCtx: NarrativeContext =
    whyThisSurfaceCtx("deferred", "Leave Black with no counterplay in the whole position forever").copy(
      semantic = None,
      decision = None
    )

  private def computeCoverage[T](
      cells: Set[T],
      covered: Set[T],
      unsafe: Set[T] = Set.empty[T]
  ): Map[T, CoverageState] =
    cells.toList.sorted(using Ordering.by(_.toString)).map { cell =>
      val state =
        if unsafe.contains(cell) then CoverageState.Unsafe
        else if covered.contains(cell) then CoverageState.Covered
        else CoverageState.Deferred
      cell -> state
    }.toMap

  private def assertNoBindInflation(text: String): Unit =
    val low = BookmakerProseContract.stripMoveHeader(text).toLowerCase
    List("no counterplay", "completely bound", "wins by force", "whole position", "totally squeezed").foreach {
      fragment =>
        assert(!low.contains(fragment), clues(text, fragment))
    }

  corpusScenarios.foreach { scenario =>
    test(s"${scenario.id} broad validation contract and builder gate stay aligned") {
      val plan = bindPlan(scenario)
      val certification =
        DualAxisBindCertification
          .evaluate(
            plan = plan,
            probeResultsById = scenario.probes.map(probe => probe.id -> probe).toMap,
            preventedPlans = scenario.preventedPlans,
            evalCp = scenario.evalCp,
            isWhiteToMove = true,
            phase = scenario.phase,
            ply = scenario.ply,
            fen = scenario.fen
          )
          .getOrElse(fail(s"expected B3b contract for ${scenario.id}"))
      val experiments =
        NarrativeContextBuilder.buildStrategicPlanExperiments(
          evaluated = List(plan),
          validatedProbeResults = scenario.probes,
          preventedPlans = scenario.preventedPlans,
          evalCp = scenario.evalCp,
          isWhiteToMove = true,
          phase = scenario.phase,
          ply = scenario.ply,
          fen = scenario.fen
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

  test("planner-owned WhyThis parity holds for a certified dual-axis bind without stronger bind inflation") {
    val ctx = whyThisSurfaceCtx("evidence_backed", "Stop the ...c5 break and keep b4 closed")
    val outline = BookStyleRenderer.validatedOutline(ctx, strategyPack = None, truthContract = None)
    val plannerInputs = QuestionPlannerInputsBuilder.build(ctx, strategyPack = None, truthContract = None)
    val rankedPlans = QuestionFirstCommentaryPlanner.plan(ctx, plannerInputs, truthContract = None)
    val chronicleSelection =
      GameChronicleCompressionPolicy.selectPlannerSurface(rankedPlans, plannerInputs)
    val chronicleArtifact =
      GameChronicleCompressionPolicy.renderWithTrace(
        ctx = ctx,
        parts = emptyParts.copy(focusedOutline = outline),
        strategyPack = None,
        truthContract = None
      )
    val activeSelection =
      ActiveStrategicCoachingBriefBuilder.selectPlannerSurface(
        ActiveStrategicCoachingBriefBuilder.PlannerReplay(
          authorQuestions = ctx.authorQuestions,
          inputs = plannerInputs,
          rankedPlans = rankedPlans
        )
      )
    val bookmakerSlots =
      BookmakerLiveCompressionPolicy.buildSlots(
        ctx,
        outline,
        refs = None,
        strategyPack = None,
        truthContract = None
      )
    val bookmakerFallback =
      BookmakerLiveCompressionPolicy.buildSlotsOrFallback(
        ctx,
        outline,
        refs = None,
        strategyPack = None,
        truthContract = None
      )
    val bookmakerParagraphs =
      BookmakerProseContract.splitParagraphs(
        LiveNarrativeCompressionCore.deterministicProse(bookmakerFallback)
      )

    assertEquals(
      rankedPlans.primary.map(_.questionKind),
      Some(AuthorQuestionKind.WhyThis),
      clues(rankedPlans, plannerInputs.decisionFrame)
    )
    assert(chronicleSelection.nonEmpty, clues(chronicleSelection, rankedPlans))
    assert(chronicleArtifact.exists(_.narrative.nonEmpty), clues(chronicleArtifact))
    assert(activeSelection.forall(_.primary.questionKind == AuthorQuestionKind.WhyThis), clues(activeSelection, rankedPlans))
    assert(bookmakerSlots.nonEmpty, clues(bookmakerSlots, rankedPlans))
    assert(bookmakerParagraphs.size >= 2, clues(bookmakerParagraphs, bookmakerFallback))
    bookmakerSlots.foreach { slots =>
      assertNoBindInflation(slots.claim)
      slots.supportPrimary.foreach(assertNoBindInflation)
      slots.supportSecondary.foreach(assertNoBindInflation)
    }
  }

  test("uncertified dual-axis shell cannot re-inflate across planner, replay, active, or whole-game reuse") {
    val ctx = surfaceReinflationCtx
    val outline = BookStyleRenderer.validatedOutline(ctx, strategyPack = None, truthContract = None)
    val plannerInputs = QuestionPlannerInputsBuilder.build(ctx, strategyPack = None, truthContract = None)
    val rankedPlans = QuestionFirstCommentaryPlanner.plan(ctx, plannerInputs, truthContract = None)
    val chronicleSelection =
      GameChronicleCompressionPolicy.selectPlannerSurface(rankedPlans, plannerInputs)
    val activeSelection =
      ActiveStrategicCoachingBriefBuilder.selectPlannerSurface(
        ActiveStrategicCoachingBriefBuilder.PlannerReplay(
          authorQuestions = ctx.authorQuestions,
          inputs = plannerInputs,
          rankedPlans = rankedPlans
        )
      )
    val bookmakerSlots =
      BookmakerLiveCompressionPolicy.buildSlots(
        ctx,
        outline,
        refs = None,
        strategyPack = None,
        truthContract = None
      )
    val fallbackSlots =
      BookmakerLiveCompressionPolicy.buildSlotsOrFallback(
        ctx,
        outline,
        refs = None,
        strategyPack = None,
        truthContract = None
      )
    val fallbackClaim = BookmakerProseContract.stripMoveHeader(fallbackSlots.claim)
    val wholeGameSupport =
      CommentaryEngine.buildWholeGameConclusionSupport(
        moments =
          List(
            GameArcMoment(
              ply = 48,
              momentType = "QuietMove",
              narrative = fallbackClaim,
              analysisData =
                ExtendedAnalysisData(
                  fen = ctx.fen,
                  nature = PositionNature(NatureType.Dynamic, 0.5, 0.5, "Bounded strategic edge"),
                  motifs = Nil,
                  plans = Nil,
                  preventedPlans = Nil,
                  pieceActivity = Nil,
                  structuralWeaknesses = Nil,
                  compensation = None,
                  endgameFeatures = None,
                  practicalAssessment = None,
                  prevMove = ctx.playedMove,
                  ply = ctx.ply,
                  evalCp = 180,
                  isWhiteToMove = true
                ),
              moveClassification = Some("Best"),
              cpBefore = Some(160),
              cpAfter = Some(180),
              transitionType = Some("support_only"),
              strategyPack = None,
              signalDigest = None,
              truthPhase = None,
              surfacedMoveOwnsTruth = false,
              verifiedPayoffAnchor = None,
              compensationProseAllowed = false
            )
          ),
        strategicThreads = Nil,
        themes = List("Prophylaxis"),
        result = "1-0"
      )

    assertEquals(rankedPlans.primary, None, clues(rankedPlans, plannerInputs.decisionFrame))
    assertEquals(chronicleSelection, None, clues(chronicleSelection, rankedPlans))
    assertEquals(activeSelection, None, clues(activeSelection, rankedPlans))
    assertEquals(bookmakerSlots, None, clues(bookmakerSlots, rankedPlans))
    assertEquals(fallbackSlots.paragraphPlan, List("p1=claim"), clues(fallbackSlots))
    assertNoBindInflation(fallbackClaim)
    assertEquals(wholeGameSupport.decisiveShift, None, clues(wholeGameSupport))
    assertEquals(wholeGameSupport.payoff, None, clues(wholeGameSupport))
  }

  test("B3b targeted scope matrix stays explicit about covered, deferred, and unsafe cells") {
    val phaseCoverage =
      computeCoverage(
        PhaseCell.values.toSet,
        covered = Set(PhaseCell.LateMiddlegame),
        unsafe = Set(PhaseCell.PureEndgame)
      )
    val evalCoverage =
      computeCoverage(
        EvalPostureCell.values.toSet,
        covered = Set(EvalPostureCell.ClearlyBetter),
        unsafe = Set(EvalPostureCell.EqualOrUnclear, EvalPostureCell.DefendingSide)
      )
    val textureCoverage =
      computeCoverage(
        TextureCell.values.toSet,
        covered =
          Set(
            TextureCell.BreakPlusEntryDualAxis,
            TextureCell.RestrictionFirstThenConversion
          ),
        unsafe =
          Set(
            TextureCell.WaitingMoveOnly,
            TextureCell.FortressLikeStaticHold
          )
      )
    val criticismCoverage =
      computeCoverage(
        CriticismCell.values.toSet,
        covered = CriticismCell.values.toSet
      )
    val surfaceCoverage =
      computeCoverage(
        SurfaceCell.values.toSet,
        covered =
          Set(
            SurfaceCell.PlannerWhyThis,
            SurfaceCell.Bookmaker,
            SurfaceCell.Chronicle
          ),
        unsafe = Set(SurfaceCell.WholeGame)
      )

    assertEquals(
      phaseCoverage,
      Map(
        PhaseCell.LateMiddlegame -> CoverageState.Covered,
        PhaseCell.HeavyPieceMiddlegame -> CoverageState.Deferred,
        PhaseCell.TransitionEndgameAdjacent -> CoverageState.Deferred,
        PhaseCell.PureEndgame -> CoverageState.Unsafe
      )
    )
    assertEquals(
      evalCoverage,
      Map(
        EvalPostureCell.ClearlyBetter -> CoverageState.Covered,
        EvalPostureCell.SlightlyBetter -> CoverageState.Deferred,
        EvalPostureCell.EqualOrUnclear -> CoverageState.Unsafe,
        EvalPostureCell.DefendingSide -> CoverageState.Unsafe
      )
    )
    assertEquals(
      textureCoverage,
      Map(
        TextureCell.BreakPlusEntryDualAxis -> CoverageState.Covered,
        TextureCell.RestrictionFirstThenConversion -> CoverageState.Covered,
        TextureCell.AxisIndependenceNearMiss -> CoverageState.Deferred,
        TextureCell.SingleAxisStrengthFake -> CoverageState.Deferred,
        TextureCell.TacticalReleaseFake -> CoverageState.Deferred,
        TextureCell.ValidationOnlyShell -> CoverageState.Deferred,
        TextureCell.WaitingMoveOnly -> CoverageState.Unsafe,
        TextureCell.FortressLikeStaticHold -> CoverageState.Unsafe,
        TextureCell.StitchedBranchShell -> CoverageState.Deferred,
        TextureCell.ColorComplexPretty -> CoverageState.Deferred,
        TextureCell.MobilityCageNoProgress -> CoverageState.Deferred
      )
    )
    assertEquals(
      criticismCoverage,
      Map(
        CriticismCell.AxisIndependenceNotProven -> CoverageState.Covered,
        CriticismCell.DualAxisBurdenMissing -> CoverageState.Covered,
        CriticismCell.HiddenFreeingBreak -> CoverageState.Covered,
        CriticismCell.HiddenTacticalRelease -> CoverageState.Covered,
        CriticismCell.DirectBestDefenseMissing -> CoverageState.Covered,
        CriticismCell.MoveOrderFragility -> CoverageState.Covered,
        CriticismCell.PvRestatementOnly -> CoverageState.Covered,
        CriticismCell.WaitingMoveDisguisedAsBind -> CoverageState.Covered,
        CriticismCell.FortressLikeButNotWinning -> CoverageState.Covered,
        CriticismCell.LocalToGlobalOverreach -> CoverageState.Covered,
        CriticismCell.RouteContinuityMissing -> CoverageState.Covered,
        CriticismCell.StitchedDefendedBranch -> CoverageState.Covered,
        CriticismCell.SurfaceReinflation -> CoverageState.Covered
      )
    )
    assertEquals(
      surfaceCoverage,
      Map(
        SurfaceCell.PlannerWhyThis -> CoverageState.Covered,
        SurfaceCell.Bookmaker -> CoverageState.Covered,
        SurfaceCell.Chronicle -> CoverageState.Covered,
        SurfaceCell.Active -> CoverageState.Deferred,
        SurfaceCell.WholeGame -> CoverageState.Unsafe
      )
    )
  }
