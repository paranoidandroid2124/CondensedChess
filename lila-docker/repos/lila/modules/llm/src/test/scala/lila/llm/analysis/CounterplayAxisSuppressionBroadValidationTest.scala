package lila.llm.analysis

import chess.Square
import munit.FunSuite

import lila.llm.*
import lila.llm.model.*
import lila.llm.model.authoring.*
import lila.llm.model.strategic.PreventedPlan

class CounterplayAxisSuppressionBroadValidationTest extends FunSuite:

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
    case ProphylacticClamp
    case NamedBreakSuppression
    case RouteDenialShell
    case ValidationOnlyShell
    case StitchedBranchShell
    case HiddenBreakFake
    case TacticalReleaseCase
    case QuietImprovementOnly

  private enum CriticismCell:
    case DirectBestDefenseMissing
    case HiddenFreeingBreak
    case HiddenTacticalRelease
    case MoveOrderFragility
    case PvRestatementOnly
    case StitchedDefendedBranch
    case WaitingMoveDisguisedAsPlan
    case LocalToGlobalOverreach
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
      subplanId: String,
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
    "2r2rk1/pp3pp1/2n1p2p/3p4/3P1P2/2P1PN1P/PP4P1/2R2RK1 w - - 0 23"
  private val HeavyPieceMiddlegameFen =
    "r1bq1rk1/pp3ppp/2n1pn2/2bp4/3P4/2N1PN2/PPQ2PPP/R1B2RK1 w - - 0 12"
  private val TransitionFen =
    "8/5pk1/3b2p1/3P4/5P2/6P1/5BK1/8 w - - 0 45"
  private val PureEndgameFen =
    "8/3k1p2/3p2p1/3P4/4KP2/6P1/8/8 w - - 0 56"

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

  private def restrictionPlan(
      scenario: CorpusScenario
  ): PlanEvidenceEvaluator.EvaluatedPlan =
    PlanEvidenceEvaluator.EvaluatedPlan(
      hypothesis =
        PlanHypothesis(
          planId = scenario.planId,
          planName = scenario.planName,
          rank = 1,
          score = 0.81,
          preconditions = Nil,
          executionSteps = List("Keep the opponent's main counterplay route closed first."),
          failureModes = List("If the clamp slips, counterplay comes back."),
          viability = PlanViability(score = 0.78, label = "high", risk = "B2b broad"),
          evidenceSources = List("theme:restriction_prophylaxis"),
          themeL1 = ThemeTaxonomy.ThemeL1.RestrictionProphylaxis.id,
          subplanId = Some(scenario.subplanId)
        ),
      status = PlanEvidenceEvaluator.PlanEvidenceStatus.PlayableEvidenceBacked,
      userFacingEligibility = PlanEvidenceEvaluator.UserFacingPlanEligibility.ProbeBacked,
      reason = "B2b broad validation",
      supportProbeIds = scenario.probes.map(_.id),
      refuteProbeIds = Nil,
      missingSignals = Nil,
      pvCoupled = false,
      themeL1 = ThemeTaxonomy.ThemeL1.RestrictionProphylaxis.id,
      subplanId = Some(scenario.subplanId),
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

  private def supportProbe(
      id: String,
      purpose: String = ThemePlanProbePurpose.LongTermRestraintValidation,
      bestReplyPv: List[String],
      replyPvs: Option[List[List[String]]],
      collapseReason: Option[String] = None,
      futureSnapshot: Option[FutureSnapshot] =
        Some(
          FutureSnapshot(
            resolvedThreatKinds = List("Counterplay"),
            newThreatKinds = Nil,
            targetsDelta = TargetsDelta(Nil, Nil, Nil, List("c5")),
            planBlockersRemoved = List("...c5 break denied"),
            planPrereqsMet = List("queenside counterplay stays muted")
          )
        ),
      keyMotifs: List[String] = List("...c5 break denied", "counterplay restrained"),
      evalCp: Int = 180
  ): ProbeResult =
    ProbeResult(
      id = id,
      evalCp = evalCp,
      bestReplyPv = bestReplyPv,
      replyPvs = replyPvs,
      deltaVsBaseline = 12,
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
            collapseReason = collapseReason
          )
        ),
      futureSnapshot = futureSnapshot
    )

  private def preventedPlan(
      breakNeutralized: Option[String] = Some("...c5"),
      deniedSquare: String = "c5",
      deniedResourceClass: Option[String] = Some("break"),
      counterplayScoreDrop: Int = 140,
      breakNeutralizationStrength: Option[Int] = Some(82),
      defensiveSufficiency: Option[Int] = Some(78)
  ): PreventedPlan =
    PreventedPlan(
      planId = "deny_counterplay",
      deniedSquares = List(Square.fromKey(deniedSquare).get),
      breakNeutralized = breakNeutralized,
      mobilityDelta = -2,
      counterplayScoreDrop = counterplayScoreDrop,
      preventedThreatType = Some("counterplay"),
      deniedResourceClass = deniedResourceClass,
      breakNeutralizationStrength = breakNeutralizationStrength,
      defensiveSufficiency = defensiveSufficiency,
      sourceScope = FactScope.Now
    )

  private val corpusScenarios =
    List(
      CorpusScenario(
        id = "true_named_break_suppression",
        planId = "named_break_suppression",
        planName = "Clamp the ...c5 break",
        subplanId = ThemeTaxonomy.SubplanId.BreakPrevention.id,
        ontologyFamily = PlayerFacingClaimOntologyFamily.LongTermRestraint,
        phaseCell = PhaseCell.LateMiddlegame,
        evalPosture = EvalPostureCell.ClearlyBetter,
        texture = TextureCell.NamedBreakSuppression,
        criticisms = Set.empty,
        phase = "middlegame",
        ply = 28,
        fen = QueenlessLateMiddlegameFen,
        probes =
          List(
            supportProbe(
              id = "probe_true_break_direct",
              purpose = "defense_reply_multipv",
              bestReplyPv = List("f8e8", "c1c8"),
              replyPvs = Some(List(List("f8e8", "c1c8"), List("a7a5", "g2g4")))
            ),
            supportProbe(
              id = "probe_true_break_validation",
              bestReplyPv = List("f8e8", "c1c8"),
              replyPvs = Some(List(List("f8e8", "c1c8"), List("a7a5", "g2g4")))
            )
          ),
        preventedPlans = List(preventedPlan()),
        evalCp = 185,
        expectedCertified = true,
        expectedFails = Set.empty
      ),
      CorpusScenario(
        id = "true_entry_route_denial",
        planId = "entry_route_denial",
        planName = "Take away the b4 entry square",
        subplanId = ThemeTaxonomy.SubplanId.KeySquareDenial.id,
        ontologyFamily = PlayerFacingClaimOntologyFamily.RouteDenial,
        phaseCell = PhaseCell.LateMiddlegame,
        evalPosture = EvalPostureCell.ClearlyBetter,
        texture = TextureCell.RouteDenialShell,
        criticisms = Set.empty,
        phase = "middlegame",
        ply = 30,
        fen = QueenlessLateMiddlegameFen,
        probes =
          List(
            supportProbe(
              id = "probe_true_entry_direct",
              purpose = "defense_reply_multipv",
              bestReplyPv = List("c8d8", "c1c8"),
              replyPvs = Some(List(List("c8d8", "c1c8"), List("a7a5", "g2g4"))),
              futureSnapshot = None,
              keyMotifs = List("b4 entry denied", "route denial")
            ),
            supportProbe(
              id = "probe_true_entry_validation",
              purpose = ThemePlanProbePurpose.RouteDenialValidation,
              bestReplyPv = List("c8d8", "c1c8"),
              replyPvs = Some(List(List("c8d8", "c1c8"), List("a7a5", "g2g4"))),
              futureSnapshot =
                Some(
                  FutureSnapshot(
                    resolvedThreatKinds = List("Counterplay"),
                    newThreatKinds = Nil,
                    targetsDelta = TargetsDelta(Nil, Nil, Nil, List("b4")),
                    planBlockersRemoved = List("entry route denied"),
                    planPrereqsMet = List("b4 stays unavailable")
                  )
                ),
              keyMotifs = List("b4 entry denied", "route denial")
            )
          ),
        preventedPlans =
          List(
            preventedPlan(
              breakNeutralized = None,
              deniedSquare = "b4",
              deniedResourceClass = Some("entry_square")
            )
          ),
        evalCp = 175,
        expectedCertified = true,
        expectedFails = Set.empty
      ),
      CorpusScenario(
        id = "validation_only_shell",
        planId = "validation_only_shell",
        planName = "Validation-only clamp shell",
        subplanId = ThemeTaxonomy.SubplanId.BreakPrevention.id,
        ontologyFamily = PlayerFacingClaimOntologyFamily.LongTermRestraint,
        phaseCell = PhaseCell.LateMiddlegame,
        evalPosture = EvalPostureCell.ClearlyBetter,
        texture = TextureCell.ValidationOnlyShell,
        criticisms = Set(CriticismCell.DirectBestDefenseMissing),
        phase = "middlegame",
        ply = 28,
        fen = QueenlessLateMiddlegameFen,
        probes =
          List(
            supportProbe(
              id = "probe_validation_only",
              bestReplyPv = List("f8e8", "c1c8"),
              replyPvs = Some(List(List("f8e8", "c1c8"), List("a7a5", "g2g4")))
            )
          ),
        preventedPlans = List(preventedPlan()),
        evalCp = 180,
        expectedCertified = false,
        expectedFails = Set("direct_best_defense_missing", "route_persistence_missing")
      ),
      CorpusScenario(
        id = "stitched_defended_branch",
        planId = "stitched_defended_branch",
        planName = "Clamp shell with stitched persistence",
        subplanId = ThemeTaxonomy.SubplanId.BreakPrevention.id,
        ontologyFamily = PlayerFacingClaimOntologyFamily.LongTermRestraint,
        phaseCell = PhaseCell.LateMiddlegame,
        evalPosture = EvalPostureCell.ClearlyBetter,
        texture = TextureCell.StitchedBranchShell,
        criticisms = Set(CriticismCell.StitchedDefendedBranch),
        phase = "middlegame",
        ply = 28,
        fen = QueenlessLateMiddlegameFen,
        probes =
          List(
            supportProbe(
              id = "probe_stitched_direct",
              purpose = "defense_reply_multipv",
              bestReplyPv = List("f8e8", "c1c8"),
              replyPvs = Some(List(List("f8e8", "c1c8"), List("a7a5", "g2g4"))),
              futureSnapshot = None
            ),
            supportProbe(
              id = "probe_stitched_validation",
              bestReplyPv = List("h7h5", "g2g4"),
              replyPvs = Some(List(List("h7h5", "g2g4"), List("b7b5", "g2g4")))
            )
          ),
        preventedPlans = List(preventedPlan()),
        evalCp = 182,
        expectedCertified = false,
        expectedFails = Set("stitched_defended_branch", "route_persistence_missing")
      ),
      CorpusScenario(
        id = "hidden_freeing_break",
        planId = "hidden_break_shell",
        planName = "Clamp the queenside",
        subplanId = ThemeTaxonomy.SubplanId.BreakPrevention.id,
        ontologyFamily = PlayerFacingClaimOntologyFamily.LongTermRestraint,
        phaseCell = PhaseCell.LateMiddlegame,
        evalPosture = EvalPostureCell.ClearlyBetter,
        texture = TextureCell.HiddenBreakFake,
        criticisms = Set(CriticismCell.HiddenFreeingBreak),
        phase = "middlegame",
        ply = 27,
        fen = QueenlessLateMiddlegameFen,
        probes =
          List(
            supportProbe(
              id = "probe_hidden_break_direct",
              purpose = "defense_reply_multipv",
              bestReplyPv = List("f8e8", "c1c8"),
              replyPvs = Some(List(List("f8e8", "c1c8"), List("a7a5", "g2g4"))),
              futureSnapshot = None
            ),
            supportProbe(
              id = "probe_hidden_break_validation",
              bestReplyPv = List("f8e8", "c1c8"),
              replyPvs = Some(List(List("f8e8", "c1c8"), List("a7a5", "g2g4")))
            )
          ),
        preventedPlans = List(preventedPlan(), preventedPlan(breakNeutralized = Some("...e5"), deniedSquare = "e5")),
        evalCp = 180,
        expectedCertified = false,
        expectedFails = Set("hidden_freeing_break", "surface_reinflation")
      ),
      CorpusScenario(
        id = "hidden_tactical_release",
        planId = "tactical_release_shell",
        planName = "Restrain first",
        subplanId = ThemeTaxonomy.SubplanId.BreakPrevention.id,
        ontologyFamily = PlayerFacingClaimOntologyFamily.LongTermRestraint,
        phaseCell = PhaseCell.LateMiddlegame,
        evalPosture = EvalPostureCell.ClearlyBetter,
        texture = TextureCell.TacticalReleaseCase,
        criticisms = Set(CriticismCell.HiddenTacticalRelease),
        phase = "middlegame",
        ply = 29,
        fen = QueenlessLateMiddlegameFen,
        probes =
          List(
            supportProbe(
              id = "probe_tactical_release_direct",
              purpose = "defense_reply_multipv",
              bestReplyPv = List("f8e8", "c1c8"),
              replyPvs = Some(List(List("f8e8", "c1c8"), List("a7a5", "g2g4"))),
              futureSnapshot =
                Some(
                  FutureSnapshot(
                    resolvedThreatKinds = List("Counterplay"),
                    newThreatKinds = List("Perpetual"),
                    targetsDelta = TargetsDelta(List("g2"), Nil, Nil, List("c5")),
                    planBlockersRemoved = List("...c5 break denied"),
                    planPrereqsMet = List("queenside counterplay stays muted")
                  )
                ),
              keyMotifs = List("...c5 break denied", "exchange sac resource")
            ),
            supportProbe(
              id = "probe_tactical_release_validation",
              bestReplyPv = List("f8e8", "c1c8"),
              replyPvs = Some(List(List("f8e8", "c1c8"), List("a7a5", "g2g4"))),
              futureSnapshot =
                Some(
                  FutureSnapshot(
                    resolvedThreatKinds = List("Counterplay"),
                    newThreatKinds = List("Perpetual"),
                    targetsDelta = TargetsDelta(List("g2"), Nil, Nil, List("c5")),
                    planBlockersRemoved = List("...c5 break denied"),
                    planPrereqsMet = List("queenside counterplay stays muted")
                  )
                ),
              keyMotifs = List("...c5 break denied", "exchange sac resource")
            )
          ),
        preventedPlans = List(preventedPlan()),
        evalCp = 175,
        expectedCertified = false,
        expectedFails = Set("hidden_tactical_release", "route_persistence_missing")
      ),
      CorpusScenario(
        id = "move_order_fragile_clamp",
        planId = "fragile_clamp",
        planName = "Hold the queenside clamp",
        subplanId = ThemeTaxonomy.SubplanId.BreakPrevention.id,
        ontologyFamily = PlayerFacingClaimOntologyFamily.LongTermRestraint,
        phaseCell = PhaseCell.LateMiddlegame,
        evalPosture = EvalPostureCell.ClearlyBetter,
        texture = TextureCell.ProphylacticClamp,
        criticisms = Set(CriticismCell.MoveOrderFragility, CriticismCell.HiddenTacticalRelease),
        phase = "middlegame",
        ply = 28,
        fen = QueenlessLateMiddlegameFen,
        probes =
          List(
            supportProbe(
              id = "probe_fragile_direct",
              purpose = "defense_reply_multipv",
              bestReplyPv = List("f8e8", "c1c8"),
              replyPvs = Some(List(List("f8e8", "c1c8"), List("a7a5", "g2g4"))),
              collapseReason = Some("wrong order lets the c-file open")
            ),
            supportProbe(id = "probe_fragile_validation", bestReplyPv = List("f8e8", "c1c8"), replyPvs = Some(List(List("f8e8", "c1c8"), List("a7a5", "g2g4"))))
          ),
        preventedPlans = List(preventedPlan()),
        evalCp = 180,
        expectedCertified = false,
        expectedFails = Set("move_order_fragility", "hidden_tactical_release")
      ),
      CorpusScenario(
        id = "pv_restatement_only_quiet",
        planId = "quiet_pv_shell",
        planName = "A quiet clamp shell",
        subplanId = ThemeTaxonomy.SubplanId.BreakPrevention.id,
        ontologyFamily = PlayerFacingClaimOntologyFamily.LongTermRestraint,
        phaseCell = PhaseCell.LateMiddlegame,
        evalPosture = EvalPostureCell.EqualOrUnclear,
        texture = TextureCell.QuietImprovementOnly,
        criticisms = Set(CriticismCell.PvRestatementOnly, CriticismCell.LocalToGlobalOverreach),
        phase = "middlegame",
        ply = 24,
        fen = QueenlessLateMiddlegameFen,
        probes =
          List(
            supportProbe(
              id = "probe_pv_only",
              purpose = ThemePlanProbePurpose.ThemePlanValidation,
              bestReplyPv = List("f8e8", "c1c8"),
              replyPvs = Some(List(List("f8e8", "c1c8"))),
              futureSnapshot = None,
              keyMotifs = List("quiet move")
            )
          ),
        preventedPlans = Nil,
        evalCp = 25,
        expectedCertified = false,
        expectedFails = Set("pv_restatement_only", "local_to_global_overreach", "waiting_move_disguised_as_plan", "hidden_freeing_break")
      ),
      CorpusScenario(
        id = "waiting_move_only",
        planId = "waiting_move_shell",
        planName = "Improve quietly",
        subplanId = ThemeTaxonomy.SubplanId.BreakPrevention.id,
        ontologyFamily = PlayerFacingClaimOntologyFamily.LongTermRestraint,
        phaseCell = PhaseCell.LateMiddlegame,
        evalPosture = EvalPostureCell.ClearlyBetter,
        texture = TextureCell.QuietImprovementOnly,
        criticisms = Set(CriticismCell.WaitingMoveDisguisedAsPlan),
        phase = "middlegame",
        ply = 28,
        fen = QueenlessLateMiddlegameFen,
        probes =
          List(
            supportProbe(
              id = "probe_waiting_direct",
              purpose = "defense_reply_multipv",
              bestReplyPv = List("f8e8", "c1c8"),
              replyPvs = Some(List(List("f8e8", "c1c8"), List("a7a5", "g2g4"))),
              futureSnapshot = None,
              keyMotifs = List("quiet move")
            ),
            supportProbe(
              id = "probe_waiting_validation",
              bestReplyPv = List("f8e8", "c1c8"),
              replyPvs = Some(List(List("f8e8", "c1c8"), List("a7a5", "g2g4"))),
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
            preventedPlan(
              counterplayScoreDrop = 30,
              breakNeutralizationStrength = Some(40),
              defensiveSufficiency = Some(35)
            )
          ),
        evalCp = 185,
        expectedCertified = false,
        expectedFails = Set("waiting_move_disguised_as_plan", "route_persistence_missing")
      ),
      CorpusScenario(
        id = "slightly_better_but_not_certifiable",
        planId = "slight_edge_clamp",
        planName = "Restrain the break first",
        subplanId = ThemeTaxonomy.SubplanId.BreakPrevention.id,
        ontologyFamily = PlayerFacingClaimOntologyFamily.LongTermRestraint,
        phaseCell = PhaseCell.LateMiddlegame,
        evalPosture = EvalPostureCell.SlightlyBetter,
        texture = TextureCell.NamedBreakSuppression,
        criticisms = Set(CriticismCell.LocalToGlobalOverreach),
        phase = "middlegame",
        ply = 28,
        fen = QueenlessLateMiddlegameFen,
        probes =
          List(
            supportProbe(
              id = "probe_slight_edge_direct",
              purpose = "defense_reply_multipv",
              bestReplyPv = List("f8e8", "c1c8"),
              replyPvs = Some(List(List("f8e8", "c1c8"), List("a7a5", "g2g4"))),
              futureSnapshot = None
            ),
            supportProbe(
              id = "probe_slight_edge_validation",
              bestReplyPv = List("f8e8", "c1c8"),
              replyPvs = Some(List(List("f8e8", "c1c8"), List("a7a5", "g2g4")))
            )
          ),
        preventedPlans = List(preventedPlan(counterplayScoreDrop = 115)),
        evalCp = 115,
        expectedCertified = false,
        expectedFails = Set("local_to_global_overreach")
      ),
      CorpusScenario(
        id = "heavy_piece_local_overreach_shell",
        planId = "heavy_piece_clamp",
        planName = "Clamp the center completely",
        subplanId = ThemeTaxonomy.SubplanId.BreakPrevention.id,
        ontologyFamily = PlayerFacingClaimOntologyFamily.LongTermRestraint,
        phaseCell = PhaseCell.HeavyPieceMiddlegame,
        evalPosture = EvalPostureCell.ClearlyBetter,
        texture = TextureCell.ProphylacticClamp,
        criticisms = Set(CriticismCell.LocalToGlobalOverreach, CriticismCell.SurfaceReinflation),
        phase = "middlegame",
        ply = 24,
        fen = HeavyPieceMiddlegameFen,
        probes =
          List(
            supportProbe(
              id = "probe_heavy_piece_direct",
              purpose = "defense_reply_multipv",
              bestReplyPv = List("c5d6", "c3b5"),
              replyPvs = Some(List(List("c5d6", "c3b5"), List("a7a6", "c3b5"))),
              futureSnapshot = None
            ),
            supportProbe(
              id = "probe_heavy_piece_validation",
              bestReplyPv = List("c5d6", "c3b5"),
              replyPvs = Some(List(List("c5d6", "c3b5"), List("a7a6", "c3b5")))
            )
          ),
        preventedPlans = List(preventedPlan()),
        evalCp = 190,
        expectedCertified = false,
        expectedFails = Set("local_to_global_overreach")
      ),
      CorpusScenario(
        id = "transition_endgame_adjacent_shell",
        planId = "transition_shell",
        planName = "Keep the king entry closed",
        subplanId = ThemeTaxonomy.SubplanId.KeySquareDenial.id,
        ontologyFamily = PlayerFacingClaimOntologyFamily.RouteDenial,
        phaseCell = PhaseCell.TransitionEndgameAdjacent,
        evalPosture = EvalPostureCell.ClearlyBetter,
        texture = TextureCell.RouteDenialShell,
        criticisms = Set(CriticismCell.LocalToGlobalOverreach),
        phase = "endgame",
        ply = 90,
        fen = TransitionFen,
        probes =
          List(
            supportProbe(
              id = "probe_transition_direct",
              purpose = "defense_reply_multipv",
              bestReplyPv = List("g7g8", "f2e3"),
              replyPvs = Some(List(List("g7g8", "f2e3"), List("d6c5", "f2e3"))),
              futureSnapshot = None,
              keyMotifs = List("dark-square entry denied", "route denial")
            ),
            supportProbe(
              id = "probe_transition_validation",
              purpose = ThemePlanProbePurpose.RouteDenialValidation,
              bestReplyPv = List("g7g8", "f2e3"),
              replyPvs = Some(List(List("g7g8", "f2e3"), List("d6c5", "f2e3"))),
              futureSnapshot =
                Some(
                  FutureSnapshot(
                    resolvedThreatKinds = List("Counterplay"),
                    newThreatKinds = Nil,
                    targetsDelta = TargetsDelta(Nil, Nil, Nil, List("f6")),
                    planBlockersRemoved = List("entry route denied"),
                    planPrereqsMet = List("dark-square entry stays sealed")
                  )
                ),
              keyMotifs = List("dark-square entry denied", "route denial")
            )
          ),
        preventedPlans =
          List(
            preventedPlan(
              breakNeutralized = None,
              deniedSquare = "f6",
              deniedResourceClass = Some("entry_square")
            )
          ),
        evalCp = 175,
        expectedCertified = false,
        expectedFails = Set("local_to_global_overreach")
      ),
      CorpusScenario(
        id = "pure_endgame_defending_shell",
        planId = "defending_endgame_shell",
        planName = "Hold the entry squares",
        subplanId = ThemeTaxonomy.SubplanId.KeySquareDenial.id,
        ontologyFamily = PlayerFacingClaimOntologyFamily.RouteDenial,
        phaseCell = PhaseCell.PureEndgame,
        evalPosture = EvalPostureCell.DefendingSide,
        texture = TextureCell.RouteDenialShell,
        criticisms = Set(CriticismCell.LocalToGlobalOverreach),
        phase = "endgame",
        ply = 112,
        fen = PureEndgameFen,
        probes =
          List(
            supportProbe(
              id = "probe_pure_endgame_direct",
              purpose = "defense_reply_multipv",
              bestReplyPv = List("d7e6", "e4d4"),
              replyPvs = Some(List(List("d7e6", "e4d4"), List("f7f6", "e4d4"))),
              futureSnapshot = None,
              keyMotifs = List("entry denial", "king route denied")
            ),
            supportProbe(
              id = "probe_pure_endgame_validation",
              purpose = ThemePlanProbePurpose.RouteDenialValidation,
              bestReplyPv = List("d7e6", "e4d4"),
              replyPvs = Some(List(List("d7e6", "e4d4"), List("f7f6", "e4d4"))),
              futureSnapshot =
                Some(
                  FutureSnapshot(
                    resolvedThreatKinds = List("Counterplay"),
                    newThreatKinds = Nil,
                    targetsDelta = TargetsDelta(Nil, Nil, Nil, List("d5")),
                    planBlockersRemoved = List("entry route denied"),
                    planPrereqsMet = List("opposition shell stays intact")
                  )
                ),
              keyMotifs = List("entry denial", "king route denied")
            )
          ),
        preventedPlans =
          List(
            preventedPlan(
              breakNeutralized = None,
              deniedSquare = "d5",
              deniedResourceClass = Some("entry_square")
            )
          ),
        evalCp = -35,
        expectedCertified = false,
        expectedFails = Set("local_to_global_overreach")
      )
    )

  private def whyThisSurfaceCtx(evidenceTier: String): NarrativeContext =
    BookmakerProseGoldenFixtures.prophylacticCut.ctx.copy(
      authorQuestions =
        List(
          AuthorQuestion(
            id = "q_b2b_why_this",
            kind = AuthorQuestionKind.WhyThis,
            priority = 100,
            question = "Why is a3 the right prophylactic move here?",
            evidencePurposes = List("reply_multipv")
          )
        ),
      authorEvidence =
        List(
          QuestionEvidence(
            questionId = "q_b2b_why_this",
            purpose = "reply_multipv",
            branches =
              List(
                EvidenceBranch(
                  keyMove = "line_1",
                  line = "23...c5 24.a4 and the queenside break stays shut.",
                  evalCp = Some(55)
                )
              )
          )
        ),
      mainStrategicPlans =
        List(
          PlanHypothesis(
            planId = "named_break_suppression",
            planName = "Clamp the ...c5 break",
            rank = 1,
            score = 0.81,
            preconditions = Nil,
            executionSteps = List("Keep the ...c5 break closed first."),
            failureModes = List("If the c-file opens, Black gets active play."),
            viability = PlanViability(score = 0.8, label = "high", risk = "surface"),
            evidenceSources = List("theme:restriction_prophylaxis"),
            themeL1 = ThemeTaxonomy.ThemeL1.RestrictionProphylaxis.id,
            subplanId = Some(ThemeTaxonomy.SubplanId.BreakPrevention.id)
          )
        ),
      strategicPlanExperiments =
        List(
          StrategicPlanExperiment(
            planId = "named_break_suppression",
            themeL1 = ThemeTaxonomy.ThemeL1.RestrictionProphylaxis.id,
            subplanId = Some(ThemeTaxonomy.SubplanId.BreakPrevention.id),
            evidenceTier = evidenceTier,
            supportProbeCount = 1,
            bestReplyStable = evidenceTier == "evidence_backed",
            futureSnapshotAligned = evidenceTier == "evidence_backed",
            counterBreakNeutralized = true,
            moveOrderSensitive = evidenceTier != "evidence_backed",
            experimentConfidence = if evidenceTier == "evidence_backed" then 0.88 else 0.34
          )
        )
    )

  private def surfaceReinflationCtx: NarrativeContext =
    BookmakerProseGoldenFixtures.prophylacticCut.ctx.copy(
      semantic = None,
      decision = None,
      authorQuestions =
        List(
          AuthorQuestion(
            id = "q_b2b_race",
            kind = AuthorQuestionKind.WhosePlanIsFaster,
            priority = 100,
            question = "Whose plan is faster here?",
            evidencePurposes = List("reply_multipv")
          )
        ),
      authorEvidence =
        List(
          QuestionEvidence(
            questionId = "q_b2b_race",
            purpose = "reply_multipv",
            branches =
              List(
                EvidenceBranch(
                  keyMove = "line_1",
                  line = "23...c5 24.a4 Rc8",
                  evalCp = Some(40)
                )
              )
          )
        ),
      opponentPlan = None,
      mainStrategicPlans =
        List(
          PlanHypothesis(
            planId = "named_break_suppression",
            planName = "Leave Black with no counterplay forever",
            rank = 1,
            score = 0.8,
            preconditions = Nil,
            executionSteps = List("Completely shut Black down and win by force."),
            failureModes = List("none"),
            viability = PlanViability(score = 0.8, label = "high", risk = "surface"),
            evidenceSources = List("theme:restriction_prophylaxis"),
            themeL1 = ThemeTaxonomy.ThemeL1.RestrictionProphylaxis.id,
            subplanId = Some(ThemeTaxonomy.SubplanId.BreakPrevention.id)
          )
        ),
      strategicPlanExperiments =
        List(
          StrategicPlanExperiment(
            planId = "named_break_suppression",
            themeL1 = ThemeTaxonomy.ThemeL1.RestrictionProphylaxis.id,
            subplanId = Some(ThemeTaxonomy.SubplanId.BreakPrevention.id),
            evidenceTier = "deferred",
            supportProbeCount = 1,
            bestReplyStable = false,
            futureSnapshotAligned = false,
            counterBreakNeutralized = true,
            moveOrderSensitive = true,
            experimentConfidence = 0.28
          )
        )
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

  private def assertNoSuppressionInflation(text: String): Unit =
    val low = BookmakerProseContract.stripMoveHeader(text).toLowerCase
    List("no counterplay", "win by force", "completely shut", "squeeze").foreach { fragment =>
      assert(!low.contains(fragment), clues(text, fragment))
    }

  corpusScenarios.foreach { scenario =>
    test(s"${scenario.id} broad validation contract and builder gate stay aligned") {
      val plan = restrictionPlan(scenario)
      val certification =
        CounterplayAxisSuppressionCertification
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
          .getOrElse(fail(s"expected B2b contract for ${scenario.id}"))
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

  test("certified named-break suppression stays line-only and does not reopen planner ownership") {
    val ctx = whyThisSurfaceCtx("evidence_backed")
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

    assertEquals(rankedPlans.primary, None, clues(rankedPlans, plannerInputs.decisionFrame))
    assertEquals(chronicleSelection, None, clues(chronicleSelection, rankedPlans))
    assertEquals(activeSelection, None, clues(activeSelection, rankedPlans))
    chronicleArtifact.foreach(artifact => assertNoSuppressionInflation(artifact.narrative))
    assert(bookmakerParagraphs.nonEmpty, clues(bookmakerParagraphs, bookmakerFallback))
    bookmakerSlots.foreach { slots =>
      assertNoSuppressionInflation(slots.claim)
      slots.supportPrimary.foreach(assertNoSuppressionInflation)
      slots.supportSecondary.foreach(assertNoSuppressionInflation)
    }
  }

  test("uncertified shell cannot re-inflate across planner, replay, or whole-game reuse") {
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
              ply = 46,
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
    assertNoSuppressionInflation(fallbackClaim)
    assertEquals(wholeGameSupport.decisiveShift, None, clues(wholeGameSupport))
    assertEquals(wholeGameSupport.payoff, None, clues(wholeGameSupport))
  }

  test("B2b targeted scope matrix stays explicit about covered, deferred, and unsafe cells") {
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
            TextureCell.ProphylacticClamp,
            TextureCell.NamedBreakSuppression,
            TextureCell.RouteDenialShell
          ),
        unsafe =
          Set(
            TextureCell.ValidationOnlyShell,
            TextureCell.StitchedBranchShell,
            TextureCell.HiddenBreakFake,
            TextureCell.TacticalReleaseCase,
            TextureCell.QuietImprovementOnly
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
            SurfaceCell.Chronicle,
            SurfaceCell.Active
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
        TextureCell.ProphylacticClamp -> CoverageState.Covered,
        TextureCell.NamedBreakSuppression -> CoverageState.Covered,
        TextureCell.RouteDenialShell -> CoverageState.Covered,
        TextureCell.ValidationOnlyShell -> CoverageState.Unsafe,
        TextureCell.StitchedBranchShell -> CoverageState.Unsafe,
        TextureCell.HiddenBreakFake -> CoverageState.Unsafe,
        TextureCell.TacticalReleaseCase -> CoverageState.Unsafe,
        TextureCell.QuietImprovementOnly -> CoverageState.Unsafe
      )
    )
    assertEquals(
      criticismCoverage,
      Map(
        CriticismCell.DirectBestDefenseMissing -> CoverageState.Covered,
        CriticismCell.HiddenFreeingBreak -> CoverageState.Covered,
        CriticismCell.HiddenTacticalRelease -> CoverageState.Covered,
        CriticismCell.MoveOrderFragility -> CoverageState.Covered,
        CriticismCell.PvRestatementOnly -> CoverageState.Covered,
        CriticismCell.StitchedDefendedBranch -> CoverageState.Covered,
        CriticismCell.WaitingMoveDisguisedAsPlan -> CoverageState.Covered,
        CriticismCell.LocalToGlobalOverreach -> CoverageState.Covered,
        CriticismCell.SurfaceReinflation -> CoverageState.Covered
      )
    )
    assertEquals(
      surfaceCoverage,
      Map(
        SurfaceCell.PlannerWhyThis -> CoverageState.Covered,
        SurfaceCell.Bookmaker -> CoverageState.Covered,
        SurfaceCell.Chronicle -> CoverageState.Covered,
        SurfaceCell.Active -> CoverageState.Covered,
        SurfaceCell.WholeGame -> CoverageState.Unsafe
      )
    )
  }
