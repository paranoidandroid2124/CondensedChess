package lila.llm.analysis

import chess.Square
import munit.FunSuite

import lila.llm.*
import lila.llm.model.*
import lila.llm.model.authoring.*
import lila.llm.model.strategic.PreventedPlan

class HeavyPieceLocalBindNegativeValidationTest extends FunSuite:

  private final case class ExactFixture(
      id: String,
      source: String,
      fen: String,
      sideToMove: String,
      evalCp: Int,
      planName: String,
      bestDefenseLine: Option[List[String]],
      releaseLine: List[String],
      probes: List[ProbeResult],
      preventedPlans: List[PreventedPlan],
      expectedFails: Set[String],
      expectedReleaseInventory: Set[String] = Set.empty,
      expectedPerpetualRisk: Boolean = false
  )

  private val QueenInfiltrationFen =
    "2rq1rk1/pp3ppp/2n1pn2/3p4/3P4/2P1PN2/PPQ2PPP/2R2RK1 w - - 0 24"
  private val RookLiftFen =
    "2rq1rk1/pp3ppp/2n1pn2/3p4/3P2P1/2P1P3/PPQ2PBP/2RR2K1 w - - 0 24"
  private val PerpetualFen =
    "r4rk1/5ppp/8/8/7q/8/2Q3P1/R5K1 b - - 0 1"
  private val OffSectorFen =
    "2rq1rk1/pp3ppp/2n1pn2/3p4/3P4/2P2N2/PPQ2PPP/2RR2K1 w - - 0 24"
  private val PressureOnlyFen =
    "2rq1rk1/pp3ppp/2n1pn2/3p4/3P4/2P1P3/PPQ2PPP/2RR2K1 w - - 0 24"
  private val StitchedFen =
    "2rq1rk1/pp3ppp/2n1pn2/3p4/3P2P1/2P1PN2/PPQ2P1P/2RR2K1 w - - 0 24"
  private val FragileFen =
    "2rq1rk1/pp3ppp/2n1pn2/3p4/3P4/2P1PNP1/PPQ2P1P/2RR2K1 w - - 0 24"
  private val FortressFen =
    "2rq1rk1/pp3ppp/2n1pn2/3p4/3P4/2P2NP1/PPQ2P1P/2RR2K1 w - - 0 24"
  private val ExchangeSacFen =
    "2rq1rk1/pp3ppp/4pn2/3p4/3P4/4PN2/PP1Q1PPP/2B2RK1 w - - 0 24"
  private val SurfaceBlockFen =
    "2rq1rk1/pp3ppp/2n1pn2/3p4/3P4/P1P1PN2/1PQ2PPP/2R2RK1 w - - 0 24"

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
      cpWhite = Some(20),
      bead = 1
    )

  private def heavyPlan(
      fixture: ExactFixture
  ): PlanEvidenceEvaluator.EvaluatedPlan =
    PlanEvidenceEvaluator.EvaluatedPlan(
      hypothesis =
        PlanHypothesis(
          planId = fixture.id,
          planName = fixture.planName,
          rank = 1,
          score = 0.84,
          preconditions = Nil,
          executionSteps = List("Take the c-file away first and keep b4 closed while the heavy pieces stay active."),
          failureModes = List("If the c-file reopens, b4 becomes available, or the heavy pieces get loose, the shell disappears."),
          viability = PlanViability(score = 0.8, label = "high", risk = "heavy-piece negative"),
          evidenceSources = List("theme:restriction_prophylaxis", s"fixture:${fixture.id}"),
          themeL1 = ThemeTaxonomy.ThemeL1.RestrictionProphylaxis.id,
          subplanId = Some(ThemeTaxonomy.SubplanId.BreakPrevention.id)
        ),
      status = PlanEvidenceEvaluator.PlanEvidenceStatus.PlayableEvidenceBacked,
      userFacingEligibility = PlanEvidenceEvaluator.UserFacingPlanEligibility.ProbeBacked,
      reason = "heavy-piece local bind negative validation",
      supportProbeIds = fixture.probes.map(_.id),
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
      baseFen: String,
      bestDefenseLine: List[String],
      releaseLine: List[String],
      futureSnapshot: Option[FutureSnapshot],
      keyMotifs: List[String],
      collapseReason: Option[String] = None,
      evalCp: Int = 188
  ): ProbeResult =
    ProbeResult(
      id = id,
      fen = Some(baseFen),
      evalCp = evalCp,
      bestReplyPv = bestDefenseLine,
      replyPvs = Some(List(bestDefenseLine, releaseLine)),
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
      baseFen: String,
      branch: List[String],
      futureSnapshot: Option[FutureSnapshot],
      keyMotifs: List[String],
      purpose: String = ThemePlanProbePurpose.RouteDenialValidation,
      evalCp: Int = 186
  ): ProbeResult =
    ProbeResult(
      id = id,
      fen = Some(baseFen),
      evalCp = evalCp,
      bestReplyPv = branch,
      replyPvs = Some(List(branch)),
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

  private def basePositiveSnapshot(
      fileLabel: String = "c-file",
      entrySquare: String = "b4",
      continuation: String = "conversion route stabilizes",
      tacticalAdded: List[String] = Nil,
      newThreatKinds: List[String] = Nil
  ): FutureSnapshot =
    FutureSnapshot(
      resolvedThreatKinds = List("Counterplay"),
      newThreatKinds = newThreatKinds,
      targetsDelta = TargetsDelta(tacticalAdded, Nil, List(continuation), List(entrySquare)),
      planBlockersRemoved = List(s"the $fileLabel stays closed"),
      planPrereqsMet = List(s"$entrySquare stays unavailable", continuation)
    )

  private def pressureOnlySnapshot: FutureSnapshot =
    FutureSnapshot(
      resolvedThreatKinds = List("Counterplay"),
      newThreatKinds = Nil,
      targetsDelta = TargetsDelta(Nil, Nil, List("heavy-piece pressure on the c-file"), Nil),
      planBlockersRemoved = List("White keeps pressure on the c-file"),
      planPrereqsMet = List("b4 is awkward")
    )

  private def fortressSnapshot: FutureSnapshot =
    FutureSnapshot(
      resolvedThreatKinds = List("Counterplay"),
      newThreatKinds = Nil,
      targetsDelta = TargetsDelta(Nil, Nil, Nil, List("b4")),
      planBlockersRemoved = List("the c-file stays closed"),
      planPrereqsMet = List("b4 stays unavailable")
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

  private val exactFixtures =
    List(
      ExactFixture(
        id = "queen_infiltration_shell",
        source = s"fen:$QueenInfiltrationFen",
        fen = QueenInfiltrationFen,
        sideToMove = "white",
        evalCp = 188,
        planName = "Freeze the c-file and keep b4 closed while the queens stay on",
        bestDefenseLine = Some(List("c3c4", "a7a6", "c4d5", "d8d5", "a2a3", "d5d7", "c2b3")),
        releaseLine = List("b2b3", "d8e7", "c3c4", "e7a3"),
        probes =
          List(
            directReplyProbe(
              id = "queen_infiltration_reply",
              baseFen = QueenInfiltrationFen,
              bestDefenseLine = List("c3c4", "a7a6", "c4d5", "d8d5", "a2a3", "d5d7", "c2b3"),
              releaseLine = List("b2b3", "d8e7", "c3c4", "e7a3"),
              futureSnapshot =
                Some(
                  basePositiveSnapshot(
                    tacticalAdded = List("queen infiltration on a3")
                  )
                ),
              keyMotifs = List("c-file denied", "b4 entry denied", "queen infiltration")
            ),
            validationProbe(
              id = "queen_infiltration_validation",
              baseFen = QueenInfiltrationFen,
              branch = List("c3c4", "a7a6"),
              futureSnapshot = Some(basePositiveSnapshot()),
              keyMotifs = List("c-file denied", "b4 entry denied", "conversion route stabilizes")
            )
          ),
        preventedPlans = List(preventedFilePlan(), preventedEntryPlan()),
        expectedFails = Set("heavy_piece_release_illusion", "surface_reinflation"),
        expectedReleaseInventory = Set("queen_infiltration"),
        expectedPerpetualRisk = false
      ),
      ExactFixture(
        id = "rook_lift_switch",
        source = s"fen:$RookLiftFen",
        fen = RookLiftFen,
        sideToMove = "white",
        evalCp = 188,
        planName = "Lock the c-file and b4 even though the rooks can switch wings",
        bestDefenseLine = Some(List("h2h3", "c6a5", "c2e2", "a7a6", "b2b3", "a5c6", "c3c4", "h7h6", "c4d5", "e6d5")),
        releaseLine = List("c2e2", "c6e7", "g2f3", "f8e8"),
        probes =
          List(
            directReplyProbe(
              id = "rook_lift_reply",
              baseFen = RookLiftFen,
              bestDefenseLine = List("h2h3", "c6a5", "c2e2", "a7a6", "b2b3", "a5c6", "c3c4", "h7h6", "c4d5", "e6d5"),
              releaseLine = List("c2e2", "c6e7", "g2f3", "f8e8"),
              futureSnapshot =
                Some(
                  basePositiveSnapshot(
                    tacticalAdded = List("cross-rank activation")
                  )
                ),
              keyMotifs = List("c-file denied", "b4 entry denied", "rook lift", "cross-rank activation")
            ),
            validationProbe(
              id = "rook_lift_validation",
              baseFen = RookLiftFen,
              branch = List("h2h3", "c6a5"),
              futureSnapshot = Some(basePositiveSnapshot()),
              keyMotifs = List("c-file denied", "b4 entry denied", "conversion route stabilizes")
            )
          ),
        preventedPlans = List(preventedFilePlan(), preventedEntryPlan()),
        expectedFails = Set("heavy_piece_release_illusion", "surface_reinflation"),
        expectedReleaseInventory = Set("rook_lift"),
        expectedPerpetualRisk = false
      ),
      ExactFixture(
        id = "perpetual_check_escape",
        source = s"fen:$PerpetualFen",
        fen = PerpetualFen,
        sideToMove = "black",
        evalCp = -188,
        planName = "Freeze the route before Black starts a perpetual check net",
        bestDefenseLine = Some(List("a8a1", "c2b1", "a1b1")),
        releaseLine = List("h4e1", "g1h2", "e1h4", "h2g1", "h4e1"),
        probes =
          List(
            directReplyProbe(
              id = "perpetual_reply",
              baseFen = PerpetualFen,
              bestDefenseLine = List("a8a1", "c2b1", "a1b1"),
              releaseLine = List("h4e1", "g1h2", "e1h4", "h2g1", "h4e1"),
              futureSnapshot =
                Some(
                  basePositiveSnapshot(
                    tacticalAdded = List("perpetual check geometry"),
                    newThreatKinds = List("Perpetual")
                  )
                ),
              keyMotifs = List("c-file denied", "b4 entry denied", "perpetual", "forcing checks")
            ),
            validationProbe(
              id = "perpetual_validation",
              baseFen = PerpetualFen,
              branch = List("a8a1", "c2b1"),
              futureSnapshot = Some(basePositiveSnapshot()),
              keyMotifs = List("c-file denied", "b4 entry denied", "conversion route stabilizes")
            )
          ),
        preventedPlans = List(preventedFilePlan(), preventedEntryPlan()),
        expectedFails = Set("heavy_piece_release_illusion", "surface_reinflation"),
        expectedReleaseInventory = Set("perpetual_check", "forcing_checks"),
        expectedPerpetualRisk = true
      ),
      ExactFixture(
        id = "off_sector_break_release",
        source = s"fen:$OffSectorFen",
        fen = OffSectorFen,
        sideToMove = "white",
        evalCp = 188,
        planName = "Claim a heavy-piece file bind even though the other wing can break",
        bestDefenseLine = Some(List("c3c4", "c8b8", "f3e5", "c6e7", "c4c5", "e7c6", "e5c6", "b7c6")),
        releaseLine = List("b2b3", "e6e5", "d4e5"),
        probes =
          List(
            directReplyProbe(
              id = "off_sector_reply",
              baseFen = OffSectorFen,
              bestDefenseLine = List("c3c4", "c8b8", "f3e5", "c6e7", "c4c5", "e7c6", "e5c6", "b7c6"),
              releaseLine = List("b2b3", "e6e5", "d4e5"),
              futureSnapshot = Some(basePositiveSnapshot()),
              keyMotifs = List("c-file denied", "b4 entry denied", "conversion route stabilizes")
            ),
            validationProbe(
              id = "off_sector_validation",
              baseFen = OffSectorFen,
              branch = List("c3c4", "c8b8"),
              futureSnapshot = Some(basePositiveSnapshot()),
              keyMotifs = List("c-file denied", "b4 entry denied", "conversion route stabilizes")
            )
          ),
        preventedPlans = List(preventedFilePlan(), preventedEntryPlan(), preventedFilePlan(label = "...e5", counterplayScoreDrop = 118)),
        expectedFails = Set("hidden_off_sector_break", "surface_reinflation")
      ),
      ExactFixture(
        id = "pressure_only_waiting_move",
        source = s"fen:$PressureOnlyFen",
        fen = PressureOnlyFen,
        sideToMove = "white",
        evalCp = 188,
        planName = "Double heavy pieces on the c-file and keep the pressure there",
        bestDefenseLine = Some(List("g2g3", "b7b5", "g1g2", "e6e5", "d1e1", "e5d4", "e3d4")),
        releaseLine = List("b2b3", "a7a5"),
        probes =
          List(
            directReplyProbe(
              id = "pressure_only_reply",
              baseFen = PressureOnlyFen,
              bestDefenseLine = List("g2g3", "b7b5", "g1g2", "e6e5", "d1e1", "e5d4", "e3d4"),
              releaseLine = List("b2b3", "a7a5"),
              futureSnapshot = Some(pressureOnlySnapshot),
              keyMotifs = List("heavy-piece pressure on c-file", "b4 is awkward")
            )
          ),
        preventedPlans = List(preventedFilePlan(), preventedEntryPlan()),
        expectedFails = Set("engine_pv_paraphrase", "pressure_only_waiting_move", "surface_reinflation")
      ),
      ExactFixture(
        id = "direct_best_defense_missing",
        source = s"fen:$PressureOnlyFen",
        fen = PressureOnlyFen,
        sideToMove = "white",
        evalCp = 188,
        planName = "Validation-only heavy-piece file shell",
        bestDefenseLine = None,
        releaseLine = List("d8h4", "g2g3"),
        probes =
          List(
            validationProbe(
              id = "validation_only_shell",
              baseFen = PressureOnlyFen,
              branch = List("d8h4"),
              futureSnapshot = Some(basePositiveSnapshot()),
              keyMotifs = List("c-file denied", "b4 entry denied", "conversion route stabilizes")
            )
          ),
        preventedPlans = List(preventedFilePlan(), preventedEntryPlan()),
        expectedFails = Set("direct_best_defense_missing")
      ),
      ExactFixture(
        id = "stitched_heavy_piece_bundle",
        source = s"fen:$StitchedFen",
        fen = StitchedFen,
        sideToMove = "white",
        evalCp = 188,
        planName = "Stitch the heavy-piece bind from two defended branches",
        bestDefenseLine = Some(List("g1h1", "f6g4", "d1g1", "g4f6", "f3g5", "h7h6")),
        releaseLine = List("g4g5", "h7h6"),
        probes =
          List(
            directReplyProbe(
              id = "stitched_reply",
              baseFen = StitchedFen,
              bestDefenseLine = List("g1h1", "f6g4", "d1g1", "g4f6", "f3g5", "h7h6"),
              releaseLine = List("g4g5", "h7h6"),
              futureSnapshot = Some(basePositiveSnapshot()),
              keyMotifs = List("c-file denied", "b4 entry denied", "conversion route stabilizes")
            ),
            validationProbe(
              id = "stitched_validation",
              baseFen = StitchedFen,
              branch = List("g1h1", "h7h5"),
              futureSnapshot = Some(basePositiveSnapshot()),
              keyMotifs = List("c-file denied", "b4 entry denied", "conversion route stabilizes")
            )
          ),
        preventedPlans = List(preventedFilePlan(), preventedEntryPlan()),
        expectedFails = Set("stitched_heavy_piece_bundle", "surface_reinflation")
      ),
      ExactFixture(
        id = "move_order_fragility",
        source = s"fen:$FragileFen",
        fen = FragileFen,
        sideToMove = "white",
        evalCp = 188,
        planName = "Keep the c-file closed only in one exact heavy-piece order",
        bestDefenseLine = Some(List("c3c4", "h7h6", "c4d5", "e6d5")),
        releaseLine = List("g3g4", "h7h5"),
        probes =
          List(
            directReplyProbe(
              id = "fragile_reply",
              baseFen = FragileFen,
              bestDefenseLine = List("c3c4", "h7h6", "c4d5", "e6d5"),
              releaseLine = List("g3g4", "h7h5"),
              futureSnapshot = Some(basePositiveSnapshot()),
              keyMotifs = List("c-file denied", "b4 entry denied", "conversion route stabilizes"),
              collapseReason = Some("wrong order reopens the c-file")
            ),
            validationProbe(
              id = "fragile_validation",
              baseFen = FragileFen,
              branch = List("c3c4", "h7h6"),
              futureSnapshot = Some(basePositiveSnapshot()),
              keyMotifs = List("c-file denied", "b4 entry denied", "conversion route stabilizes")
            )
          ),
        preventedPlans = List(preventedFilePlan(), preventedEntryPlan()),
        expectedFails = Set("move_order_fragility", "surface_reinflation")
      ),
      ExactFixture(
        id = "fortress_like_but_not_progressing",
        source = s"fen:$FortressFen",
        fen = FortressFen,
        sideToMove = "white",
        evalCp = 188,
        planName = "Hold the c-file shell and wait with heavy pieces on",
        bestDefenseLine = Some(List("c3c4", "h7h6", "c4d5", "e6d5")),
        releaseLine = List("b2b3", "a7a5"),
        probes =
          List(
            directReplyProbe(
              id = "fortress_reply",
              baseFen = FortressFen,
              bestDefenseLine = List("c3c4", "h7h6", "c4d5", "e6d5"),
              releaseLine = List("b2b3", "a7a5"),
              futureSnapshot = Some(fortressSnapshot),
              keyMotifs = List("c-file denied", "b4 entry denied", "hold the shell")
            ),
            validationProbe(
              id = "fortress_validation",
              baseFen = FortressFen,
              branch = List("c3c4", "h7h6"),
              futureSnapshot = Some(fortressSnapshot),
              keyMotifs = List("c-file denied", "b4 entry denied", "hold the shell")
            )
          ),
        preventedPlans = List(preventedFilePlan(), preventedEntryPlan()),
        expectedFails = Set("fortress_like_but_not_progressing", "surface_reinflation")
      ),
      ExactFixture(
        id = "exchange_sac_release",
        source = s"fen:$ExchangeSacFen",
        fen = ExchangeSacFen,
        sideToMove = "white",
        evalCp = 188,
        planName = "Freeze the local file shell before the exchange sac lands",
        bestDefenseLine = Some(List("h2h3", "c8c1", "f1c1", "d8d6")),
        releaseLine = List("d2c2", "c8c1", "f1c1"),
        probes =
          List(
            directReplyProbe(
              id = "exchange_sac_reply",
              baseFen = ExchangeSacFen,
              bestDefenseLine = List("h2h3", "c8c1", "f1c1", "d8d6"),
              releaseLine = List("d2c2", "c8c1", "f1c1"),
              futureSnapshot =
                Some(
                  basePositiveSnapshot(
                    tacticalAdded = List("exchange sac release")
                  )
                ),
              keyMotifs = List("c-file denied", "b4 entry denied", "exchange sac release")
            ),
            validationProbe(
              id = "exchange_sac_validation",
              baseFen = ExchangeSacFen,
              branch = List("h2h3", "c8c1"),
              futureSnapshot = Some(basePositiveSnapshot()),
              keyMotifs = List("c-file denied", "b4 entry denied", "conversion route stabilizes")
            )
          ),
        preventedPlans = List(preventedFilePlan(), preventedEntryPlan()),
        expectedFails = Set("heavy_piece_release_illusion", "surface_reinflation"),
        expectedReleaseInventory = Set("exchange_sac_release"),
        expectedPerpetualRisk = false
      )
    )

  private def heavyPieceSurfaceCtx: NarrativeContext =
    BookmakerProseGoldenFixtures.prophylacticCut.ctx.copy(
      fen = SurfaceBlockFen,
      ply = 24,
      playedMove = Some("a2a3"),
      playedSan = Some("a3"),
      phase = PhaseContext("Middlegame", "Heavy pieces still on and the c-file shell looks tempting"),
      authorQuestions =
        List(
          AuthorQuestion(
            id = "q_heavy_shell",
            kind = AuthorQuestionKind.WhyThis,
            priority = 100,
            question = "Why does a3 bind the c-file and b4 even with queens on?",
            evidencePurposes = List("reply_multipv")
          )
        ),
      authorEvidence =
        List(
          QuestionEvidence(
            questionId = "q_heavy_shell",
            purpose = "reply_multipv",
            branches =
              List(
                EvidenceBranch(
                  keyMove = "line_1",
                  line = "24.a3 freezes the c-file and keeps b4 closed while the heavy pieces stay active.",
                  evalCp = Some(36)
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
        List(
          PlanHypothesis(
            planId = "heavy_piece_local_bind",
            planName = "Freeze the c-file and keep b4 closed with the heavy pieces still on",
            rank = 1,
            score = 0.84,
            preconditions = Nil,
            executionSteps = List("Take the c-file away first and keep b4 closed while the heavy pieces stay active."),
            failureModes = List("If queen checks, rook lifts, or the c-file reopen, the shell disappears."),
            viability = PlanViability(score = 0.8, label = "high", risk = "surface"),
            evidenceSources = List("theme:restriction_prophylaxis"),
            themeL1 = ThemeTaxonomy.ThemeL1.RestrictionProphylaxis.id,
            subplanId = Some(ThemeTaxonomy.SubplanId.BreakPrevention.id)
          )
        ),
      strategicPlanExperiments =
        List(
          StrategicPlanExperiment(
            planId = "heavy_piece_local_bind",
            themeL1 = ThemeTaxonomy.ThemeL1.RestrictionProphylaxis.id,
            subplanId = Some(ThemeTaxonomy.SubplanId.BreakPrevention.id),
            evidenceTier = "evidence_backed",
            supportProbeCount = 2,
            bestReplyStable = true,
            futureSnapshotAligned = true,
            counterBreakNeutralized = true,
            experimentConfidence = 0.9
          )
        )
    )

  private def assertNoHeavyPieceInflation(text: String): Unit =
    val low = BookmakerProseContract.stripMoveHeader(text).toLowerCase
    assert(!low.contains("no counterplay"), clues(text))
    assert(!low.contains("whole position"), clues(text))
    assert(!low.contains("completely bound"), clues(text))
    assert(!low.contains("bind"), clues(text))
    assert(!(low.contains("c-file") && low.contains("b4")), clues(text))

  exactFixtures.foreach { fixture =>
    test(s"${fixture.id} keeps the heavy-piece shell negative-only on an exact board and best-defense branch") {
      val plan = heavyPlan(fixture)
      val contract =
        HeavyPieceLocalBindValidation
          .evaluate(
            plan = plan,
            probeResultsById = fixture.probes.map(probe => probe.id -> probe).toMap,
            preventedPlans = fixture.preventedPlans,
            evalCp = fixture.evalCp,
            isWhiteToMove = fixture.sideToMove == "white",
            phase = "middlegame",
            ply = 24,
            fen = fixture.fen
          )
          .getOrElse(fail(s"expected heavy-piece contract for ${fixture.id}"))
      val experiments =
        NarrativeContextBuilder.buildStrategicPlanExperiments(
          evaluated = List(plan),
          validatedProbeResults = fixture.probes,
          preventedPlans = fixture.preventedPlans,
          evalCp = fixture.evalCp,
          isWhiteToMove = fixture.sideToMove == "white",
          phase = "middlegame",
          ply = 24,
          fen = fixture.fen
        )

      fixture.expectedFails.foreach { failure =>
        assert(contract.failsIf.contains(failure), clues(fixture.id, fixture.source, failure, contract))
      }
      fixture.expectedReleaseInventory.foreach { release =>
        assert(
          contract.heavyPieceReleaseInventory.contains(release) ||
            contract.bestDefenseReleaseSurvivors.contains(release),
          clues(fixture.id, fixture.source, release, contract)
        )
      }
      assertEquals(contract.perpetualRisk, fixture.expectedPerpetualRisk, clues(fixture.id, contract))
      assertEquals(
        contract.bestDefenseFound,
        fixture.bestDefenseLine.map(_.mkString(" ")),
        clues(fixture.id, fixture.source, contract)
      )
      assertEquals(experiments.head.evidenceTier, "deferred", clues(fixture.id, experiments.head, contract))
    }
  }

  test("best-defense release survivors stay pinned to PV1 instead of alternative reply lines") {
    val fixture =
      exactFixtures
        .find(_.id == "queen_infiltration_shell")
        .getOrElse(fail("expected queen_infiltration_shell fixture"))
    val contract =
      HeavyPieceLocalBindValidation
        .evaluate(
          plan = heavyPlan(fixture),
          probeResultsById = fixture.probes.map(probe => probe.id -> probe).toMap,
          preventedPlans = fixture.preventedPlans,
          evalCp = fixture.evalCp,
          isWhiteToMove = fixture.sideToMove == "white",
          phase = "middlegame",
          ply = 24,
          fen = fixture.fen
        )
        .getOrElse(fail("expected heavy-piece contract"))

    assert(contract.heavyPieceReleaseInventory.contains("queen_infiltration"), clues(contract))
    assert(!contract.bestDefenseReleaseSurvivors.contains("queen_infiltration"), clues(contract))
  }

  test("same-first-move divergent continuation does not count as the same defended branch") {
    val divergentFixture =
      exactFixtures
        .find(_.id == "stitched_heavy_piece_bundle")
        .getOrElse(fail("expected stitched_heavy_piece_bundle fixture"))
    val contract =
      HeavyPieceLocalBindValidation
        .evaluate(
          plan = heavyPlan(divergentFixture),
          probeResultsById = divergentFixture.probes.map(probe => probe.id -> probe).toMap,
          preventedPlans = divergentFixture.preventedPlans,
          evalCp = divergentFixture.evalCp,
          isWhiteToMove = divergentFixture.sideToMove == "white",
          phase = "middlegame",
          ply = 24,
          fen = divergentFixture.fen
        )
        .getOrElse(fail("expected heavy-piece contract"))

    assertEquals(contract.sameDefendedBranch, false, clues(contract))
    assert(contract.failsIf.contains("stitched_heavy_piece_bundle"), clues(contract))
  }

  test("one-move branch fragments do not provide replayable same-branch identity") {
    val fragmentPlan =
      heavyPlan(
        ExactFixture(
          id = "fragmentary_branch_identity",
          source = s"fen:$PressureOnlyFen",
          fen = PressureOnlyFen,
          sideToMove = "white",
          evalCp = 188,
          planName = "Claim a heavy-piece shell from a one-move fragment",
          bestDefenseLine = None,
          releaseLine = List("d8h4"),
          probes =
            List(
              directReplyProbe(
                id = "fragment_reply",
                baseFen = PressureOnlyFen,
                bestDefenseLine = List("d8h4"),
                releaseLine = List("d8h4"),
                futureSnapshot = Some(basePositiveSnapshot()),
                keyMotifs = List("c-file denied", "b4 entry denied", "conversion route stabilizes")
              ),
              validationProbe(
                id = "fragment_validation",
                baseFen = PressureOnlyFen,
                branch = List("d8h4"),
                futureSnapshot = Some(basePositiveSnapshot()),
                keyMotifs = List("c-file denied", "b4 entry denied", "conversion route stabilizes")
              )
            ),
          preventedPlans = List(preventedFilePlan(), preventedEntryPlan()),
          expectedFails = Set("direct_best_defense_missing")
        )
      )
    val probes =
      List(
        directReplyProbe(
          id = "fragment_reply",
          baseFen = PressureOnlyFen,
          bestDefenseLine = List("d8h4"),
          releaseLine = List("d8h4"),
          futureSnapshot = Some(basePositiveSnapshot()),
          keyMotifs = List("c-file denied", "b4 entry denied", "conversion route stabilizes")
        ),
        validationProbe(
          id = "fragment_validation",
          baseFen = PressureOnlyFen,
          branch = List("d8h4"),
          futureSnapshot = Some(basePositiveSnapshot()),
          keyMotifs = List("c-file denied", "b4 entry denied", "conversion route stabilizes")
        )
      )
    val contract =
      HeavyPieceLocalBindValidation
        .evaluate(
          plan = fragmentPlan,
          probeResultsById = probes.map(probe => probe.id -> probe).toMap,
          preventedPlans = List(preventedFilePlan(), preventedEntryPlan()),
          evalCp = 188,
          isWhiteToMove = true,
          phase = "middlegame",
          ply = 24,
          fen = PressureOnlyFen
        )
        .getOrElse(fail("expected heavy-piece contract"))

    assertEquals(contract.bestDefenseBranchKey, None, clues(contract))
    assert(contract.failsIf.contains("direct_best_defense_missing"), clues(contract))
  }

  test("heavy-piece local bind shell cannot re-inflate across planner, replay, active, bookmaker, or whole-game reuse") {
    val ctx = heavyPieceSurfaceCtx
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
              ply = 24,
              momentType = "QuietMove",
              narrative = fallbackClaim,
              analysisData =
                ExtendedAnalysisData(
                  fen = ctx.fen,
                  nature = PositionNature(NatureType.Dynamic, 0.5, 0.5, "Heavy-piece local bind shell"),
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
                  evalCp = 188,
                  isWhiteToMove = true
                ),
              moveClassification = Some("Best"),
              cpBefore = Some(170),
              cpAfter = Some(188),
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
        themes = List("RestrictionProphylaxis"),
        result = "1-0"
      )
    val surfacedTexts =
      plannerInputs.mainBundle.flatMap(_.mainClaim).map(_.claimText).toList ++
        plannerInputs.quietIntent.map(_.claimText).toList ++
        rankedPlans.primary.map(_.claim).toList ++
        bookmakerSlots.map(_.claim).toList ++
        chronicleArtifact.map(_.narrative).filter(_.nonEmpty).toList

    assertEquals(HeavyPieceLocalBindValidation.blocksPlayerFacingShell(ctx), true, clues(ctx.fen, ctx.mainStrategicPlans, ctx.strategicPlanExperiments))
    assertEquals(plannerInputs.mainBundle, None, clues(plannerInputs))
    assertEquals(plannerInputs.quietIntent, None, clues(plannerInputs))
    assertEquals(rankedPlans.primary, None, clues(rankedPlans, plannerInputs))
    assertEquals(chronicleSelection, None, clues(chronicleSelection, rankedPlans))
    assertEquals(activeSelection, None, clues(activeSelection, rankedPlans))
    assertEquals(bookmakerSlots, None, clues(bookmakerSlots, rankedPlans))
    assertEquals(fallbackSlots.paragraphPlan, List("p1=claim"), clues(fallbackSlots))
    assertNoHeavyPieceInflation(fallbackClaim)
    surfacedTexts.foreach(assertNoHeavyPieceInflation)
    assertEquals(wholeGameSupport.decisiveShift, None, clues(wholeGameSupport))
    assertEquals(wholeGameSupport.payoff, None, clues(wholeGameSupport))
  }
