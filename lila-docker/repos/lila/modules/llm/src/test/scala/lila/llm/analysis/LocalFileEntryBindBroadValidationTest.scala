package lila.llm.analysis

import chess.Square
import munit.FunSuite

import lila.llm.*
import lila.llm.model.*
import lila.llm.model.authoring.*
import lila.llm.model.strategic.{ EngineEvidence, PreventedPlan, PvMove, VariationLine }

class LocalFileEntryBindBroadValidationTest extends FunSuite:

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
    case FileEntryBind
    case FileOccupancyOnly
    case OffFileReleaseFake
    case EntryIndependenceNearMiss
    case TacticalReleaseFake
    case FortressLikeHold
    case HeavyPieceReleaseShell
    case SurfaceReinflationShell

  private enum CriticismCell:
    case FileOccupancyOnly
    case HiddenOffFileRelease
    case EntryAxisNotIndependent
    case EntryAxisPersistenceMissing
    case DirectBestDefenseMissing
    case StitchedDefendedBranch
    case HiddenTacticalRelease
    case MoveOrderFragility
    case FortressLikeButNotProgressing
    case SlightEdgeOverclaim
    case SurfaceReinflation

  private enum SurfaceCell:
    case PlannerWhyThis
    case PlannerWhatChanged
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
          executionSteps = List("Take the file away first and keep one entry square closed."),
          failureModes = List("If the file or entry reopens, the bind vanishes."),
          viability = PlanViability(score = 0.8, label = "high", risk = "B4b broad"),
          evidenceSources = List("theme:restriction_prophylaxis"),
          themeL1 = ThemeTaxonomy.ThemeL1.RestrictionProphylaxis.id,
          subplanId = Some(ThemeTaxonomy.SubplanId.BreakPrevention.id)
        ),
      status = PlanEvidenceEvaluator.PlanEvidenceStatus.PlayableEvidenceBacked,
      userFacingEligibility = PlanEvidenceEvaluator.UserFacingPlanEligibility.ProbeBacked,
      reason = "B4b broad validation",
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
          ontologyFamily = PlayerFacingClaimOntologyFamily.RouteDenial
        )
    )

  private def directReplyProbe(
      id: String,
      branchHead: String = "f8e8",
      branch: List[String] = Nil,
      collapseReason: Option[String] = None,
      futureSnapshot: Option[FutureSnapshot] =
        Some(
          FutureSnapshot(
            resolvedThreatKinds = List("Counterplay"),
            newThreatKinds = Nil,
            targetsDelta = TargetsDelta(Nil, Nil, List("conversion route stabilizes"), List("b4")),
            planBlockersRemoved = List("the c-file stays closed"),
            planPrereqsMet = List("b4 stays unavailable")
          )
        ),
      keyMotifs: List[String] = List("c-file denied", "b4 entry denied", "conversion route stabilizes"),
      purpose: String = "defense_reply_multipv",
      evalCp: Int = 190
  ): ProbeResult =
    val resolvedBranch =
      Option.when(branch.nonEmpty)(branch).getOrElse(List(branchHead, "c1c8"))
    ProbeResult(
      id = id,
      evalCp = evalCp,
      bestReplyPv = resolvedBranch,
      replyPvs = Some((List(resolvedBranch, List("a7a5", "g2g4"))).distinct),
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

  private def validationProbe(
      id: String,
      branchHead: String = "f8e8",
      branch: List[String] = Nil,
      purpose: String = ThemePlanProbePurpose.RouteDenialValidation,
      futureSnapshot: Option[FutureSnapshot] =
        Some(
          FutureSnapshot(
            resolvedThreatKinds = List("Counterplay"),
            newThreatKinds = Nil,
            targetsDelta = TargetsDelta(Nil, Nil, List("conversion route stabilizes"), List("b4")),
            planBlockersRemoved = List("the c-file stays closed"),
            planPrereqsMet = List("b4 stays unavailable", "conversion route stabilizes")
          )
        ),
      keyMotifs: List[String] = List("c-file denied", "b4 entry denied", "conversion route stabilizes"),
      evalCp: Int = 188
  ): ProbeResult =
    directReplyProbe(
      id = id,
      branchHead = branchHead,
      branch = branch,
      collapseReason = None,
      futureSnapshot = futureSnapshot,
      keyMotifs = keyMotifs,
      purpose = purpose,
      evalCp = evalCp
    )

  private def preventedFilePlan(
      label: String = "...c5",
      counterplayScoreDrop: Int = 145
  ): PreventedPlan =
    PreventedPlan(
      planId = "deny_c_file",
      deniedSquares = List(Square.fromKey("c5").get),
      breakNeutralized = Some(label),
      mobilityDelta = -2,
      counterplayScoreDrop = counterplayScoreDrop,
      preventedThreatType = Some("counterplay"),
      deniedResourceClass = Some("break"),
      deniedEntryScope = Some("file"),
      breakNeutralizationStrength = Some(84),
      defensiveSufficiency = Some(80),
      sourceScope = FactScope.Now
    )

  private def preventedEntryPlan(
      square: String = "b4",
      counterplayScoreDrop: Int = 130
  ): PreventedPlan =
    PreventedPlan(
      planId = "deny_entry",
      deniedSquares = List(Square.fromKey(square).get),
      breakNeutralized = None,
      mobilityDelta = -2,
      counterplayScoreDrop = counterplayScoreDrop,
      preventedThreatType = Some("counterplay"),
      deniedResourceClass = Some("entry_square"),
      deniedEntryScope = Some("single_square"),
      breakNeutralizationStrength = Some(76),
      defensiveSufficiency = Some(74),
      sourceScope = FactScope.Now
    )

  private val corpusScenarios =
    List(
      CorpusScenario(
        id = "true_local_file_entry_bind",
        planId = "true_local_file_entry_bind",
        planName = "Take the c-file away and keep b4 closed",
        phaseCell = PhaseCell.LateMiddlegame,
        evalPosture = EvalPostureCell.ClearlyBetter,
        texture = TextureCell.FileEntryBind,
        criticisms = Set.empty,
        phase = "middlegame",
        ply = 30,
        fen = QueenlessLateMiddlegameFen,
        probes = List(directReplyProbe("probe_true_reply"), validationProbe("probe_true_validation")),
        preventedPlans = List(preventedFilePlan(), preventedEntryPlan()),
        evalCp = 190,
        expectedCertified = true,
        expectedFails = Set.empty
      ),
      CorpusScenario(
        id = "file_occupancy_only",
        planId = "file_occupancy_only",
        planName = "Occupy the c-file and hint at b4",
        phaseCell = PhaseCell.LateMiddlegame,
        evalPosture = EvalPostureCell.ClearlyBetter,
        texture = TextureCell.FileOccupancyOnly,
        criticisms = Set(CriticismCell.FileOccupancyOnly),
        phase = "middlegame",
        ply = 30,
        fen = QueenlessLateMiddlegameFen,
        probes =
          List(
            directReplyProbe(
              "probe_occupancy_reply",
              futureSnapshot =
                Some(
                  FutureSnapshot(
                    resolvedThreatKinds = List("Counterplay"),
                    newThreatKinds = Nil,
                    targetsDelta = TargetsDelta(Nil, Nil, List("c-file pressure"), List("b4")),
                    planBlockersRemoved = List("White keeps pressure on the c-file"),
                    planPrereqsMet = List("b4 stays difficult")
                  )
                ),
              keyMotifs = List("rook pressure on c-file", "b4 is awkward")
            ),
            validationProbe(
              "probe_occupancy_validation",
              futureSnapshot =
                Some(
                  FutureSnapshot(
                    resolvedThreatKinds = List("Counterplay"),
                    newThreatKinds = Nil,
                    targetsDelta = TargetsDelta(Nil, Nil, List("c-file pressure"), List("b4")),
                    planBlockersRemoved = List("White keeps pressure on the c-file"),
                    planPrereqsMet = List("b4 stays difficult")
                  )
                ),
              keyMotifs = List("rook pressure on c-file", "b4 is awkward")
            )
          ),
        preventedPlans = List(preventedFilePlan(), preventedEntryPlan()),
        evalCp = 188,
        expectedCertified = false,
        expectedFails = Set("file_occupancy_only", "surface_reinflation")
      ),
      CorpusScenario(
        id = "hidden_off_file_release",
        planId = "hidden_off_file_release",
        planName = "Bind the c-file shell",
        phaseCell = PhaseCell.LateMiddlegame,
        evalPosture = EvalPostureCell.ClearlyBetter,
        texture = TextureCell.OffFileReleaseFake,
        criticisms = Set(CriticismCell.HiddenOffFileRelease),
        phase = "middlegame",
        ply = 29,
        fen = QueenlessLateMiddlegameFen,
        probes = List(directReplyProbe("probe_off_file_reply"), validationProbe("probe_off_file_validation")),
        preventedPlans = List(preventedFilePlan(), preventedEntryPlan(), preventedFilePlan(label = "...e5", counterplayScoreDrop = 120)),
        evalCp = 186,
        expectedCertified = false,
        expectedFails = Set("hidden_off_file_release", "surface_reinflation")
      ),
      CorpusScenario(
        id = "entry_axis_not_independent",
        planId = "entry_axis_not_independent",
        planName = "Treat c5 as both the file and the entry",
        phaseCell = PhaseCell.LateMiddlegame,
        evalPosture = EvalPostureCell.ClearlyBetter,
        texture = TextureCell.EntryIndependenceNearMiss,
        criticisms = Set(CriticismCell.EntryAxisNotIndependent),
        phase = "middlegame",
        ply = 30,
        fen = QueenlessLateMiddlegameFen,
        probes = List(directReplyProbe("probe_same_file_reply"), validationProbe("probe_same_file_validation")),
        preventedPlans = List(preventedFilePlan(), preventedEntryPlan(square = "c5")),
        evalCp = 188,
        expectedCertified = false,
        expectedFails = Set("entry_axis_not_independent", "surface_reinflation")
      ),
      CorpusScenario(
        id = "entry_axis_persistence_missing",
        planId = "entry_axis_persistence_missing",
        planName = "Take the c-file away before improving further",
        phaseCell = PhaseCell.LateMiddlegame,
        evalPosture = EvalPostureCell.ClearlyBetter,
        texture = TextureCell.FileEntryBind,
        criticisms = Set(CriticismCell.EntryAxisPersistenceMissing),
        phase = "middlegame",
        ply = 30,
        fen = QueenlessLateMiddlegameFen,
        probes =
          List(
            directReplyProbe(
              "probe_entry_missing_reply",
              futureSnapshot =
                Some(
                  FutureSnapshot(
                    resolvedThreatKinds = List("Counterplay"),
                    newThreatKinds = Nil,
                    targetsDelta = TargetsDelta(Nil, Nil, List("conversion route stabilizes"), Nil),
                    planBlockersRemoved = List("the c-file stays closed"),
                    planPrereqsMet = List("the c-file stays unavailable", "conversion route stabilizes")
                  )
                ),
              keyMotifs = List("c-file denied", "conversion route stabilizes")
            ),
            validationProbe(
              "probe_entry_missing_validation",
              futureSnapshot =
                Some(
                  FutureSnapshot(
                    resolvedThreatKinds = List("Counterplay"),
                    newThreatKinds = Nil,
                    targetsDelta = TargetsDelta(Nil, Nil, List("conversion route stabilizes"), Nil),
                    planBlockersRemoved = List("the c-file stays closed"),
                    planPrereqsMet = List("the c-file stays unavailable", "conversion route stabilizes")
                  )
                ),
              keyMotifs = List("c-file denied", "conversion route stabilizes")
            ),
            validationProbe(
              "probe_entry_other_branch_only",
              branchHead = "h7h5",
              futureSnapshot =
                Some(
                  FutureSnapshot(
                    resolvedThreatKinds = List("Counterplay"),
                    newThreatKinds = Nil,
                    targetsDelta = TargetsDelta(Nil, Nil, Nil, List("b4")),
                    planBlockersRemoved = Nil,
                    planPrereqsMet = List("b4 stays unavailable")
                  )
                ),
              keyMotifs = List("b4 entry denied")
            )
          ),
        preventedPlans = List(preventedFilePlan(), preventedEntryPlan()),
        evalCp = 188,
        expectedCertified = false,
        expectedFails = Set("entry_axis_persistence_missing", "surface_reinflation")
      ),
      CorpusScenario(
        id = "direct_best_defense_missing",
        planId = "direct_best_defense_missing",
        planName = "Validation-only file shell",
        phaseCell = PhaseCell.LateMiddlegame,
        evalPosture = EvalPostureCell.ClearlyBetter,
        texture = TextureCell.FileEntryBind,
        criticisms = Set(CriticismCell.DirectBestDefenseMissing),
        phase = "middlegame",
        ply = 30,
        fen = QueenlessLateMiddlegameFen,
        probes = List(validationProbe("probe_validation_only"), validationProbe("probe_convert_only", purpose = "convert_reply_multipv")),
        preventedPlans = List(preventedFilePlan(), preventedEntryPlan()),
        evalCp = 188,
        expectedCertified = false,
        expectedFails = Set("direct_best_defense_missing", "surface_reinflation")
      ),
      CorpusScenario(
        id = "stitched_defended_branch",
        planId = "stitched_defended_branch",
        planName = "Stitch the file proof across branches",
        phaseCell = PhaseCell.LateMiddlegame,
        evalPosture = EvalPostureCell.ClearlyBetter,
        texture = TextureCell.FileEntryBind,
        criticisms = Set(CriticismCell.StitchedDefendedBranch),
        phase = "middlegame",
        ply = 30,
        fen = QueenlessLateMiddlegameFen,
        probes = List(directReplyProbe("probe_stitched_reply"), validationProbe("probe_stitched_validation", branchHead = "h7h5")),
        preventedPlans = List(preventedFilePlan(), preventedEntryPlan()),
        evalCp = 188,
        expectedCertified = false,
        expectedFails = Set("stitched_defended_branch", "surface_reinflation")
      ),
      CorpusScenario(
        id = "same_first_move_divergent_branch",
        planId = "same_first_move_divergent_branch",
        planName = "Borrow same-branch proof from a divergent continuation",
        phaseCell = PhaseCell.LateMiddlegame,
        evalPosture = EvalPostureCell.ClearlyBetter,
        texture = TextureCell.FileEntryBind,
        criticisms = Set(CriticismCell.StitchedDefendedBranch),
        phase = "middlegame",
        ply = 30,
        fen = QueenlessLateMiddlegameFen,
        probes =
          List(
            directReplyProbe(
              "probe_same_first_reply",
              branch = List("f8e8", "c1c8")
            ),
            validationProbe(
              "probe_same_first_validation",
              branch = List("f8e8", "g2g4")
            )
          ),
        preventedPlans = List(preventedFilePlan(), preventedEntryPlan()),
        evalCp = 188,
        expectedCertified = false,
        expectedFails = Set("stitched_defended_branch", "surface_reinflation")
      ),
      CorpusScenario(
        id = "hidden_tactical_release",
        planId = "hidden_tactical_release",
        planName = "Keep the file closed before converting",
        phaseCell = PhaseCell.LateMiddlegame,
        evalPosture = EvalPostureCell.ClearlyBetter,
        texture = TextureCell.TacticalReleaseFake,
        criticisms = Set(CriticismCell.HiddenTacticalRelease),
        phase = "middlegame",
        ply = 30,
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
                    targetsDelta = TargetsDelta(List("g2"), Nil, List("conversion route stabilizes"), List("b4")),
                    planBlockersRemoved = List("the c-file stays closed"),
                    planPrereqsMet = List("b4 stays unavailable")
                  )
                ),
              keyMotifs = List("c-file denied", "b4 entry denied", "rook lift resource")
            ),
            validationProbe("probe_tactical_validation")
          ),
        preventedPlans = List(preventedFilePlan(), preventedEntryPlan()),
        evalCp = 188,
        expectedCertified = false,
        expectedFails = Set("hidden_tactical_release", "surface_reinflation")
      ),
      CorpusScenario(
        id = "move_order_fragile_file_bind",
        planId = "move_order_fragile_file_bind",
        planName = "Only keep the file in one move order",
        phaseCell = PhaseCell.LateMiddlegame,
        evalPosture = EvalPostureCell.ClearlyBetter,
        texture = TextureCell.FileEntryBind,
        criticisms = Set(CriticismCell.MoveOrderFragility),
        phase = "middlegame",
        ply = 30,
        fen = QueenlessLateMiddlegameFen,
        probes = List(directReplyProbe("probe_fragile_reply", collapseReason = Some("wrong order reopens the c-file")), validationProbe("probe_fragile_validation")),
        preventedPlans = List(preventedFilePlan(), preventedEntryPlan()),
        evalCp = 188,
        expectedCertified = false,
        expectedFails = Set("move_order_fragility", "cooperative_defense", "surface_reinflation")
      ),
      CorpusScenario(
        id = "fortress_like_file_hold",
        planId = "fortress_like_file_hold",
        planName = "Hold the file and wait",
        phaseCell = PhaseCell.LateMiddlegame,
        evalPosture = EvalPostureCell.ClearlyBetter,
        texture = TextureCell.FortressLikeHold,
        criticisms = Set(CriticismCell.FortressLikeButNotProgressing),
        phase = "middlegame",
        ply = 30,
        fen = QueenlessLateMiddlegameFen,
        probes =
          List(
            directReplyProbe(
              "probe_fortress_reply",
              futureSnapshot =
                Some(
                  FutureSnapshot(
                    resolvedThreatKinds = List("Counterplay"),
                    newThreatKinds = Nil,
                    targetsDelta = TargetsDelta(Nil, Nil, Nil, List("b4")),
                    planBlockersRemoved = List("the c-file stays closed"),
                    planPrereqsMet = List("b4 stays unavailable")
                  )
                ),
              keyMotifs = List("c-file denied", "b4 entry denied", "hold the bind")
            ),
            validationProbe(
              "probe_fortress_validation",
              futureSnapshot =
                Some(
                  FutureSnapshot(
                    resolvedThreatKinds = List("Counterplay"),
                    newThreatKinds = Nil,
                    targetsDelta = TargetsDelta(Nil, Nil, Nil, List("b4")),
                    planBlockersRemoved = List("the c-file stays closed"),
                    planPrereqsMet = List("b4 stays unavailable")
                  )
                ),
              keyMotifs = List("c-file denied", "b4 entry denied", "hold the bind")
            )
          ),
        preventedPlans = List(preventedFilePlan(), preventedEntryPlan()),
        evalCp = 186,
        expectedCertified = false,
        expectedFails = Set("fortress_like_but_not_progressing", "surface_reinflation")
      ),
      CorpusScenario(
        id = "slightly_better_overclaim_shell",
        planId = "slightly_better_overclaim_shell",
        planName = "Restrict the c-file a bit",
        phaseCell = PhaseCell.LateMiddlegame,
        evalPosture = EvalPostureCell.SlightlyBetter,
        texture = TextureCell.FileEntryBind,
        criticisms = Set(CriticismCell.SlightEdgeOverclaim),
        phase = "middlegame",
        ply = 30,
        fen = QueenlessLateMiddlegameFen,
        probes = List(directReplyProbe("probe_slight_reply"), validationProbe("probe_slight_validation")),
        preventedPlans = List(preventedFilePlan(), preventedEntryPlan()),
        evalCp = 70,
        expectedCertified = false,
        expectedFails = Set("slight_edge_overclaim", "surface_reinflation")
      ),
      CorpusScenario(
        id = "heavy_piece_release_shell",
        planId = "heavy_piece_release_shell",
        planName = "Freeze the c-file with queens on",
        phaseCell = PhaseCell.HeavyPieceMiddlegame,
        evalPosture = EvalPostureCell.ClearlyBetter,
        texture = TextureCell.HeavyPieceReleaseShell,
        criticisms = Set(CriticismCell.HiddenTacticalRelease),
        phase = "middlegame",
        ply = 18,
        fen = HeavyPieceMiddlegameFen,
        probes =
          List(
            directReplyProbe(
              "probe_heavy_reply",
              futureSnapshot =
                Some(
                  FutureSnapshot(
                    resolvedThreatKinds = List("Counterplay"),
                    newThreatKinds = List("Perpetual"),
                    targetsDelta = TargetsDelta(List("g2"), Nil, List("conversion route stabilizes"), List("b4")),
                    planBlockersRemoved = List("the c-file stays closed"),
                    planPrereqsMet = List("b4 stays unavailable")
                  )
                ),
              keyMotifs = List("c-file denied", "b4 entry denied", "queen infiltration")
            ),
            validationProbe("probe_heavy_validation")
          ),
        preventedPlans = List(preventedFilePlan(), preventedEntryPlan()),
        evalCp = 188,
        expectedCertified = false,
        expectedFails = Set("slice_scope_violation", "hidden_tactical_release", "surface_reinflation")
      ),
      CorpusScenario(
        id = "surface_reinflation_case",
        planId = "surface_reinflation_case",
        planName = "Leave Black with no counterplay in the whole position forever",
        phaseCell = PhaseCell.LateMiddlegame,
        evalPosture = EvalPostureCell.ClearlyBetter,
        texture = TextureCell.SurfaceReinflationShell,
        criticisms = Set(CriticismCell.SurfaceReinflation),
        phase = "middlegame",
        ply = 30,
        fen = QueenlessLateMiddlegameFen,
        probes = List(directReplyProbe("probe_surface_reply"), validationProbe("probe_surface_validation")),
        preventedPlans = List(preventedFilePlan(), preventedEntryPlan()),
        evalCp = 188,
        expectedCertified = false,
        expectedFails = Set("surface_reinflation")
      )
    )

  private def whyThisSurfaceCtx(evidenceTier: String): NarrativeContext =
    BookmakerProseGoldenFixtures.prophylacticCut.ctx.copy(
      authorQuestions =
        List(
          AuthorQuestion(
            id = "q_b4b_why_this",
            kind = AuthorQuestionKind.WhyThis,
            priority = 100,
            question = "Why is a3 the right file-entry bind move here?",
            evidencePurposes = List("reply_multipv")
          )
        ),
      authorEvidence =
        List(
          QuestionEvidence(
            questionId = "q_b4b_why_this",
            purpose = "reply_multipv",
            branches =
              List(
                EvidenceBranch(
                  keyMove = "line_1",
                  line = "24.a3 keeps the c-file closed and b4 unavailable before White doubles on the c-file.",
                  evalCp = Some(60)
                )
              )
          )
        ),
      semantic =
        BookmakerProseGoldenFixtures.prophylacticCut.ctx.semantic.map(_.copy(
          preventedPlans =
            List(
              PreventedPlanInfo(
                planId = "deny_c_file",
                deniedSquares = List("c5"),
                breakNeutralized = Some("...c5"),
                mobilityDelta = -2,
                counterplayScoreDrop = 145,
                preventedThreatType = Some("counterplay"),
                deniedResourceClass = Some("break"),
                deniedEntryScope = Some("file")
              ),
              PreventedPlanInfo(
                planId = "deny_entry",
                deniedSquares = List("b4"),
                breakNeutralized = None,
                mobilityDelta = -2,
                counterplayScoreDrop = 130,
                preventedThreatType = Some("counterplay"),
                deniedResourceClass = Some("entry_square"),
                deniedEntryScope = Some("single_square")
              )
            )
        )),
      mainStrategicPlans =
        Option.when(evidenceTier == "evidence_backed") {
          List(
            PlanHypothesis(
              planId = "local_file_entry_bind",
              planName = "Take the c-file away and keep b4 closed",
              rank = 1,
              score = 0.84,
              preconditions = Nil,
              executionSteps = List("Take the file away first and keep one entry square closed."),
              failureModes = List("If the file or entry reopens, the bind vanishes."),
              viability = PlanViability(score = 0.8, label = "high", risk = "surface"),
              evidenceSources = List("theme:restriction_prophylaxis"),
              themeL1 = ThemeTaxonomy.ThemeL1.RestrictionProphylaxis.id,
              subplanId = Some(ThemeTaxonomy.SubplanId.BreakPrevention.id)
            )
          )
        }.getOrElse(Nil),
      strategicPlanExperiments =
        List(
          StrategicPlanExperiment(
            planId = "local_file_entry_bind",
            themeL1 = ThemeTaxonomy.ThemeL1.RestrictionProphylaxis.id,
            subplanId = Some(ThemeTaxonomy.SubplanId.BreakPrevention.id),
            evidenceTier = evidenceTier,
            supportProbeCount = 2,
            bestReplyStable = evidenceTier == "evidence_backed",
            futureSnapshotAligned = evidenceTier == "evidence_backed",
            counterBreakNeutralized = true,
            moveOrderSensitive = evidenceTier != "evidence_backed",
            experimentConfidence = if evidenceTier == "evidence_backed" then 0.91 else 0.28
          )
        ),
      engineEvidence =
        Some(
          EngineEvidence(
            depth = 18,
            variations =
              List(
                VariationLine(
                  moves = List("c1c8", "f8e8", "c8e8"),
                  scoreCp = 90,
                  depth = 18,
                  parsedMoves =
                    List(
                      PvMove("c1c8", "Rc8", "c1", "c8", "R", isCapture = false, capturedPiece = None, givesCheck = false),
                      PvMove("f8e8", "Rfe8", "f8", "e8", "R", isCapture = false, capturedPiece = None, givesCheck = false),
                      PvMove("c8e8", "Rxe8+", "c8", "e8", "R", isCapture = true, capturedPiece = Some("r"), givesCheck = true)
                    )
                )
              )
          )
        )
    )

  private def whatChangedSurfaceCtx: NarrativeContext =
    whyThisSurfaceCtx("evidence_backed").copy(
      authorQuestions =
        List(
          AuthorQuestion(
            id = "q_b4b_what_changed",
            kind = AuthorQuestionKind.WhatChanged,
            priority = 100,
            question = "What changed after a3?",
            evidencePurposes = List("reply_multipv")
          )
        )
    )

  private def whatChangedMissingBranchCtx: NarrativeContext =
    whatChangedSurfaceCtx.copy(
      engineEvidence =
        Some(
          EngineEvidence(
            depth = 18,
            variations =
              List(
                VariationLine(
                  moves = List("c1c8"),
                  scoreCp = 90,
                  depth = 18,
                  parsedMoves =
                    List(
                      PvMove("c1c8", "Rc8", "c1", "c8", "R", isCapture = false, capturedPiece = None, givesCheck = false)
                    )
                )
              )
          )
        )
    )

  private def whatChangedFileOccupancyOnlyCtx: NarrativeContext =
    whatChangedSurfaceCtx.copy(
      semantic =
        whatChangedSurfaceCtx.semantic.map(_.copy(
          preventedPlans =
            List(
              PreventedPlanInfo(
                planId = "occupy_c_file",
                deniedSquares = List("c5"),
                breakNeutralized = Some("c-file"),
                mobilityDelta = -1,
                counterplayScoreDrop = 110,
                preventedThreatType = Some("counterplay"),
                deniedResourceClass = Some("pressure"),
                deniedEntryScope = Some("single_square")
              ),
              PreventedPlanInfo(
                planId = "hint_b4",
                deniedSquares = List("b4"),
                breakNeutralized = None,
                mobilityDelta = -1,
                counterplayScoreDrop = 80,
                preventedThreatType = Some("counterplay"),
                deniedResourceClass = Some("entry_square"),
                deniedEntryScope = Some("single_square")
              )
            )
        ))
    )

  private def whatChangedNonIndependentEntryCtx: NarrativeContext =
    whatChangedSurfaceCtx.copy(
      semantic =
        whatChangedSurfaceCtx.semantic.map(_.copy(
          preventedPlans =
            List(
              PreventedPlanInfo(
                planId = "deny_c_file",
                deniedSquares = List("c5"),
                breakNeutralized = Some("c-file"),
                mobilityDelta = -2,
                counterplayScoreDrop = 145,
                preventedThreatType = Some("counterplay"),
                deniedResourceClass = Some("break"),
                deniedEntryScope = Some("file")
              ),
              PreventedPlanInfo(
                planId = "same_axis_square",
                deniedSquares = List("c5"),
                breakNeutralized = None,
                mobilityDelta = -2,
                counterplayScoreDrop = 130,
                preventedThreatType = Some("counterplay"),
                deniedResourceClass = Some("entry_square"),
                deniedEntryScope = Some("single_square")
              )
            )
        ))
    )

  private def fileEntryStrategyPack: Option[StrategyPack] =
    Some(
      StrategyPack(
        sideToMove = "white",
        directionalTargets =
          List(
            StrategyDirectionalTarget(
              targetId = "target_b4",
              ownerSide = "white",
              piece = "R",
              from = "c1",
              targetSquare = "b4",
              readiness = DirectionalTargetReadiness.Build,
              strategicReasons = List("keep b4 closed while controlling the c-file"),
              evidence = List("probe")
            )
          ),
        signalDigest = Some(NarrativeSignalDigest(decision = Some("keep b4 closed while controlling the c-file")))
      )
    )

  private def surfaceReinflationCtx: NarrativeContext =
    whyThisSurfaceCtx("deferred")

  private def deferredExperimentResidualMainPlanCtx: NarrativeContext =
    val seeded = whyThisSurfaceCtx("evidence_backed")
    seeded.copy(
      strategicPlanExperiments =
        seeded.strategicPlanExperiments.map(_.copy(evidenceTier = "deferred", bestReplyStable = false, futureSnapshotAligned = false)),
      authorQuestions =
        List(
          AuthorQuestion(
            id = "q_b4b_residual_why_this",
            kind = AuthorQuestionKind.WhyThis,
            priority = 100,
            question = "Why is a3 still supposed to bind the c-file?",
            evidencePurposes = List("reply_multipv")
          )
        )
    )

  private def pairOnlyInFailureModeOrRefutationCtx: NarrativeContext =
    whyThisSurfaceCtx("evidence_backed").copy(
      authorEvidence =
        List(
          QuestionEvidence(
            questionId = "q_b4b_why_this",
            purpose = "reply_multipv",
            branches =
              List(
                EvidenceBranch(
                  keyMove = "line_1",
                  line = "24.a3 improves White's setup before the next phase.",
                  evalCp = Some(60)
                )
              )
          )
        ),
      mainStrategicPlans =
        List(
          PlanHypothesis(
            planId = "local_file_entry_bind",
            planName = "Improve the rook before the next phase",
            rank = 1,
            score = 0.84,
            preconditions = Nil,
            executionSteps = List("Improve the rook before pressing further."),
            failureModes = List("If the c-file reopens and b4 becomes available, the bind vanishes."),
            viability = PlanViability(score = 0.8, label = "high", risk = "surface"),
            evidenceSources = List("theme:restriction_prophylaxis"),
            themeL1 = ThemeTaxonomy.ThemeL1.RestrictionProphylaxis.id,
            subplanId = Some(ThemeTaxonomy.SubplanId.BreakPrevention.id),
            refutation = Some("Black can fight back by using the c-file and entering on b4.")
          )
        )
    )

  private def computeCoverage[T](
      cells: Set[T],
      covered: Set[T],
      unsafe: Set[T]
  ): Map[T, CoverageState] =
    cells.toList.sorted(using Ordering.by(_.toString)).map { cell =>
      val state =
        if unsafe.contains(cell) then CoverageState.Unsafe
        else if covered.contains(cell) then CoverageState.Covered
        else CoverageState.Deferred
      cell -> state
    }.toMap

  private def assertNoFileEntryInflation(text: String): Unit =
    val low = BookmakerProseContract.stripMoveHeader(text).toLowerCase
    List("no counterplay", "whole position", "wins by force", "totally squeezed").foreach { fragment =>
      assert(!low.contains(fragment), clues(text, fragment))
    }

  private def assertNoPositiveFileEntryPair(text: String): Unit =
    val low = BookmakerProseContract.stripMoveHeader(text).toLowerCase
    assert(!(low.contains("c-file") && low.contains("b4")), clues(text))

  corpusScenarios.foreach { scenario =>
    test(s"${scenario.id} broad validation contract and builder gate stay aligned") {
      val plan = bindPlan(scenario)
      val certification =
        LocalFileEntryBindCertification
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
          .getOrElse(fail(s"expected B4b contract for ${scenario.id}"))
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
      assertEquals(experiments.head.evidenceTier, if scenario.expectedCertified then "evidence_backed" else "deferred")
    }
  }

  test("certified local file-entry bind stays bounded on supported non-active surfaces") {
    val ctx = whyThisSurfaceCtx("evidence_backed")
    val outline = BookStyleRenderer.validatedOutline(ctx, strategyPack = fileEntryStrategyPack, truthContract = None)
    val plannerInputs = QuestionPlannerInputsBuilder.build(ctx, strategyPack = fileEntryStrategyPack, truthContract = None)
    val rankedPlans = QuestionFirstCommentaryPlanner.plan(ctx, plannerInputs, truthContract = None)
    val chronicleArtifact =
      GameChronicleCompressionPolicy.renderWithTrace(
        ctx = ctx,
        parts = emptyParts.copy(focusedOutline = outline),
        strategyPack = fileEntryStrategyPack,
        truthContract = None
      )
    val activeSelection =
      ActiveStrategicCoachingBriefBuilder.selectPlannerSurface(
        ActiveStrategicCoachingBriefBuilder.PlannerReplay(ctx.authorQuestions, plannerInputs, rankedPlans)
      )
    val bookmakerSlots =
      BookmakerLiveCompressionPolicy.buildSlots(
        ctx,
        outline,
        refs = None,
        strategyPack = fileEntryStrategyPack,
        truthContract = None
      )

    plannerInputs.mainBundle.flatMap(_.mainClaim).foreach { claim =>
      assertNoFileEntryInflation(claim.claimText)
      assert(claim.claimText.toLowerCase.contains("c-file"), clues(claim))
      assert(claim.claimText.toLowerCase.contains("b4"), clues(claim))
    }
    rankedPlans.primary.foreach { plan =>
      assertNoFileEntryInflation(plan.claim)
      assert(plan.claim.toLowerCase.contains("c-file"), clues(plan))
      assert(plan.claim.toLowerCase.contains("b4"), clues(plan))
    }
    assertEquals(activeSelection, None, clues(activeSelection, rankedPlans))
    chronicleArtifact.foreach(artifact => assertNoFileEntryInflation(artifact.narrative))
    bookmakerSlots.foreach { slots =>
      assert(BookmakerProseContract.stripMoveHeader(slots.claim).toLowerCase.contains("c-file"), clues(slots.claim))
      assert(BookmakerProseContract.stripMoveHeader(slots.claim).toLowerCase.contains("b4"), clues(slots.claim))
      assertNoFileEntryInflation(slots.claim)
    }
  }

  test("exact positive control promotes half-open-file pressure only as a bounded WhatChanged move delta") {
    val ctx = whatChangedSurfaceCtx
    val outline = BookStyleRenderer.validatedOutline(ctx, strategyPack = fileEntryStrategyPack, truthContract = None)
    val plannerInputs = QuestionPlannerInputsBuilder.build(ctx, strategyPack = fileEntryStrategyPack, truthContract = None)
    val rankedPlans = QuestionFirstCommentaryPlanner.plan(ctx, plannerInputs, truthContract = None)
    val chronicleArtifact =
      GameChronicleCompressionPolicy.renderWithTrace(
        ctx = ctx,
        parts = emptyParts.copy(focusedOutline = outline),
        strategyPack = fileEntryStrategyPack,
        truthContract = None
      )
    val activeSelection =
      ActiveStrategicCoachingBriefBuilder.selectPlannerSurface(
        ActiveStrategicCoachingBriefBuilder.PlannerReplay(ctx.authorQuestions, plannerInputs, rankedPlans)
      )
    val bookmakerSlots =
      BookmakerLiveCompressionPolicy.buildSlots(
        ctx,
        outline,
        refs = None,
        strategyPack = fileEntryStrategyPack,
        truthContract = None
      )

    assertEquals(
      plannerInputs.mainBundle.flatMap(_.mainClaim).flatMap(_.packet).map(_.ownerFamily),
      Some("half_open_file_pressure"),
      clues(plannerInputs.mainBundle)
    )
    assertEquals(
      plannerInputs.mainBundle.flatMap(_.mainClaim).flatMap(_.packet).map(_.fallbackMode),
      Some(PlayerFacingClaimFallbackMode.WeakMain),
      clues(plannerInputs.mainBundle)
    )
    assertEquals(
      rankedPlans.primary.map(_.questionKind),
      Some(AuthorQuestionKind.WhatChanged),
      clues(rankedPlans)
    )
    assert(
      rankedPlans.primary.map(_.claim).exists(claim =>
        claim.toLowerCase.contains("c-file") && claim.toLowerCase.contains("b4")
      ),
      clues(rankedPlans)
    )
    rankedPlans.primary.foreach(plan => assertNoFileEntryInflation(plan.claim))
    chronicleArtifact.foreach { artifact =>
      assertNoFileEntryInflation(artifact.narrative)
      assert(artifact.narrative.toLowerCase.contains("c-file"), clues(artifact.narrative))
      assert(artifact.narrative.toLowerCase.contains("b4"), clues(artifact.narrative))
    }
    assertEquals(activeSelection, None, clues(activeSelection, rankedPlans))
    bookmakerSlots.foreach { slots =>
      assertNoFileEntryInflation(slots.claim)
      assert(slots.claim.toLowerCase.contains("c-file"), clues(slots.claim))
      assert(slots.claim.toLowerCase.contains("b4"), clues(slots.claim))
    }
  }

  test("file-entry promotion stays fail-closed when the best-defense branch key is missing") {
    val ctx = whatChangedMissingBranchCtx
    val plannerInputs = QuestionPlannerInputsBuilder.build(ctx, strategyPack = fileEntryStrategyPack, truthContract = None)
    val rankedPlans = QuestionFirstCommentaryPlanner.plan(ctx, plannerInputs, truthContract = None)

    assertNotEquals(
      plannerInputs.mainBundle.flatMap(_.mainClaim).flatMap(_.packet).map(_.fallbackMode),
      Some(PlayerFacingClaimFallbackMode.WeakMain),
      clues(plannerInputs.mainBundle)
    )
    assertEquals(rankedPlans.primary, None, clues(rankedPlans, plannerInputs))
  }

  test("file occupancy only cannot reopen the half-open-file move-local owner") {
    val ctx = whatChangedFileOccupancyOnlyCtx
    val plannerInputs = QuestionPlannerInputsBuilder.build(ctx, strategyPack = fileEntryStrategyPack, truthContract = None)
    val rankedPlans = QuestionFirstCommentaryPlanner.plan(ctx, plannerInputs, truthContract = None)

    assertNotEquals(
      plannerInputs.mainBundle.flatMap(_.mainClaim).flatMap(_.packet).map(_.fallbackMode),
      Some(PlayerFacingClaimFallbackMode.WeakMain),
      clues(plannerInputs.mainBundle)
    )
    assertEquals(rankedPlans.primary, None, clues(rankedPlans, plannerInputs))
  }

  test("non-independent entry axis cannot reopen the half-open-file move-local owner") {
    val ctx = whatChangedNonIndependentEntryCtx
    val plannerInputs = QuestionPlannerInputsBuilder.build(ctx, strategyPack = fileEntryStrategyPack, truthContract = None)
    val rankedPlans = QuestionFirstCommentaryPlanner.plan(ctx, plannerInputs, truthContract = None)

    assertNotEquals(
      plannerInputs.mainBundle.flatMap(_.mainClaim).flatMap(_.packet).map(_.fallbackMode),
      Some(PlayerFacingClaimFallbackMode.WeakMain),
      clues(plannerInputs.mainBundle)
    )
    assertEquals(rankedPlans.primary, None, clues(rankedPlans, plannerInputs))
  }

  test("uncertified local file-entry shell cannot re-inflate across planner, replay, active, or whole-game reuse") {
    val ctx = surfaceReinflationCtx
    val outline = BookStyleRenderer.validatedOutline(ctx, strategyPack = None, truthContract = None)
    val plannerInputs = QuestionPlannerInputsBuilder.build(ctx, strategyPack = None, truthContract = None)
    val rankedPlans = QuestionFirstCommentaryPlanner.plan(ctx, plannerInputs, truthContract = None)
    val chronicleSelection = GameChronicleCompressionPolicy.selectPlannerSurface(rankedPlans, plannerInputs)
    val activeSelection =
      ActiveStrategicCoachingBriefBuilder.selectPlannerSurface(
        ActiveStrategicCoachingBriefBuilder.PlannerReplay(ctx.authorQuestions, plannerInputs, rankedPlans)
      )
    val bookmakerSlots =
      BookmakerLiveCompressionPolicy.buildSlots(ctx, outline, refs = None, strategyPack = None, truthContract = None)
    val fallbackSlots =
      BookmakerLiveCompressionPolicy.buildSlotsOrFallback(ctx, outline, refs = None, strategyPack = None, truthContract = None)
    val fallbackClaim = BookmakerProseContract.stripMoveHeader(fallbackSlots.claim)

    assertEquals(rankedPlans.primary, None, clues(rankedPlans, plannerInputs))
    assertEquals(chronicleSelection, None, clues(chronicleSelection, rankedPlans))
    assertEquals(activeSelection, None, clues(activeSelection, rankedPlans))
    assertEquals(bookmakerSlots, None, clues(bookmakerSlots, rankedPlans))
    assertEquals(fallbackSlots.paragraphPlan, List("p1=claim"), clues(fallbackSlots))
    assertNoFileEntryInflation(fallbackClaim)
  }

  test("deferred experiment plus residual main plan cannot stitch a file-entry claim back into builder or planner surfaces") {
    val ctx = deferredExperimentResidualMainPlanCtx
    val plannerInputs = QuestionPlannerInputsBuilder.build(ctx, strategyPack = None, truthContract = None)
    val rankedPlans = QuestionFirstCommentaryPlanner.plan(ctx, plannerInputs, truthContract = None)
    val builderClaims =
      plannerInputs.mainBundle.flatMap(_.mainClaim).map(_.claimText).toList ++
        plannerInputs.quietIntent.map(_.claimText).toList ++
        rankedPlans.primary.map(_.claim).toList

    assertEquals(LocalFileEntryBindCertification.certifiedSurfacePair(ctx), None, clues(ctx.mainStrategicPlans, ctx.strategicPlanExperiments))
    builderClaims.foreach { claim =>
      val low = claim.toLowerCase
      assert(!(low.contains("c-file") && low.contains("b4")), clues(claim, plannerInputs.mainBundle, plannerInputs.quietIntent, rankedPlans))
    }
  }

  test("pair only in failure mode or refutation cannot unlock positive file-entry wording") {
    val ctx = pairOnlyInFailureModeOrRefutationCtx
    val outline = BookStyleRenderer.validatedOutline(ctx, strategyPack = None, truthContract = None)
    val plannerInputs = QuestionPlannerInputsBuilder.build(ctx, strategyPack = None, truthContract = None)
    val rankedPlans = QuestionFirstCommentaryPlanner.plan(ctx, plannerInputs, truthContract = None)
    val bookmakerSlots =
      BookmakerLiveCompressionPolicy.buildSlots(ctx, outline, refs = None, strategyPack = None, truthContract = None)
    val chronicleArtifact =
      GameChronicleCompressionPolicy.renderWithTrace(
        ctx = ctx,
        parts = emptyParts.copy(focusedOutline = outline),
        strategyPack = None,
        truthContract = None
      )
    val surfacedTexts =
      plannerInputs.mainBundle.flatMap(_.mainClaim).map(_.claimText).toList ++
        plannerInputs.quietIntent.map(_.claimText).toList ++
        rankedPlans.primary.map(_.claim).toList ++
        bookmakerSlots.map(_.claim).toList ++
        chronicleArtifact.map(_.narrative).filter(_.nonEmpty).toList

    assertEquals(LocalFileEntryBindCertification.certifiedSurfacePair(ctx), None, clues(ctx.mainStrategicPlans))
    surfacedTexts.foreach(assertNoPositiveFileEntryPair)
  }

  test("B4b scope matrix stays explicit about covered, deferred, and unsafe cells") {
    val phaseCoverage =
      computeCoverage(PhaseCell.values.toSet, covered = Set(PhaseCell.LateMiddlegame), unsafe = Set(PhaseCell.PureEndgame))
    val evalCoverage =
      computeCoverage(
        EvalPostureCell.values.toSet,
        covered = Set(EvalPostureCell.ClearlyBetter),
        unsafe = Set(EvalPostureCell.EqualOrUnclear, EvalPostureCell.DefendingSide)
      )
    val surfaceCoverage =
      computeCoverage(
        SurfaceCell.values.toSet,
        covered = Set(SurfaceCell.PlannerWhyThis, SurfaceCell.PlannerWhatChanged, SurfaceCell.Bookmaker, SurfaceCell.Chronicle),
        unsafe = Set(SurfaceCell.WholeGame)
      )

    assertEquals(phaseCoverage(PhaseCell.HeavyPieceMiddlegame), CoverageState.Deferred)
    assertEquals(evalCoverage(EvalPostureCell.SlightlyBetter), CoverageState.Deferred)
    assertEquals(surfaceCoverage(SurfaceCell.Active), CoverageState.Deferred)
    assertEquals(surfaceCoverage(SurfaceCell.WholeGame), CoverageState.Unsafe)
  }
