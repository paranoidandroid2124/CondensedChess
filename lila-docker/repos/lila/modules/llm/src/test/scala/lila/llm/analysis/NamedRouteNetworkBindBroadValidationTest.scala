package lila.llm.analysis

import chess.Square
import munit.FunSuite

import lila.llm.*
import lila.llm.model.*
import lila.llm.model.authoring.*
import lila.llm.model.strategic.PreventedPlan

class NamedRouteNetworkBindBroadValidationTest extends FunSuite:

  private final case class Scenario(
      id: String,
      fen: String,
      planName: String,
      probes: List[ProbeResult],
      preventedPlans: List[PreventedPlan],
      evalCp: Int,
      expectedCertified: Boolean,
      expectedFails: Set[String]
  )

  private val PositiveFen =
    "2r2rk1/pp3pp1/2n1p2p/3p4/1p1P1P2/P1P1PN1P/1P4P1/2R2RK1 w - - 0 24"
  private val HeavyPieceFen =
    "2rq1rk1/pp3ppp/2n1pn2/3p4/3P4/2P1PN2/PPQ2PPP/2R2RK1 w - - 0 24"

  private def routePlan(
      scenario: Scenario
  ): PlanEvidenceEvaluator.EvaluatedPlan =
    PlanEvidenceEvaluator.EvaluatedPlan(
      hypothesis =
        PlanHypothesis(
          planId = scenario.id,
          planName = scenario.planName,
          rank = 1,
          score = 0.86,
          preconditions = Nil,
          executionSteps = List("Take the c-file away, keep b4 closed, and verify the reroute on the same branch."),
          failureModes = List("If the file, b4, or the reroute reopens, the route network shell disappears."),
          viability = PlanViability(score = 0.82, label = "high", risk = "B6b narrow"),
          evidenceSources = List(s"fen:${scenario.fen}", s"fixture:${scenario.id}"),
          themeL1 = ThemeTaxonomy.ThemeL1.RestrictionProphylaxis.id,
          subplanId = Some(ThemeTaxonomy.SubplanId.BreakPrevention.id)
        ),
      status = PlanEvidenceEvaluator.PlanEvidenceStatus.PlayableEvidenceBacked,
      userFacingEligibility = PlanEvidenceEvaluator.UserFacingPlanEligibility.ProbeBacked,
      reason = "B6b narrow validation",
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

  private def routeSnapshot(
      rerouteSquare: String = "a5",
      continuationVisible: Boolean = true,
      routeEdgeVisible: Boolean = true,
      extraPrereqs: List[String] = Nil,
      extraAdded: List[String] = Nil
  ): FutureSnapshot =
    FutureSnapshot(
      resolvedThreatKinds = List("Counterplay"),
      newThreatKinds = Nil,
      targetsDelta =
        TargetsDelta(
          Nil,
          Nil,
          (List(
            Option.when(routeEdgeVisible)(s"the b4 to $rerouteSquare route stays cut off"),
            Option.when(continuationVisible)("conversion route stabilizes")
          ).flatten ++ extraAdded),
          List(rerouteSquare)
        ),
      planBlockersRemoved = List("the c-file stays closed"),
      planPrereqsMet =
        List(
          "b4 stays unavailable",
          s"the $rerouteSquare reroute stays unavailable"
        ) ++ Option.when(continuationVisible)("conversion route stabilizes").toList ++ extraPrereqs
    )

  private def directReplyProbe(
      id: String,
      branch: List[String] = List("a7a5", "b4a5", "c6a5"),
      futureSnapshot: Option[FutureSnapshot] = Some(routeSnapshot()),
      keyMotifs: List[String] = List("c-file denied", "b4 entry denied", "the b4 to a5 route stays cut off"),
      collapseReason: Option[String] = None,
      purpose: String = "defense_reply_multipv",
      evalCp: Int = 205,
      seedId: Option[String] = None,
      variationHash: Option[String] = None
  ): ProbeResult =
    ProbeResult(
      id = id,
      fen = Some(PositiveFen),
      evalCp = evalCp,
      bestReplyPv = branch,
      replyPvs = Some(List(branch)),
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
      futureSnapshot = futureSnapshot,
      seedId = seedId,
      variationHash = variationHash
    )

  private def validationProbe(
      id: String,
      branch: List[String] = List("a7a5", "b4a5"),
      futureSnapshot: Option[FutureSnapshot] = Some(routeSnapshot()),
      keyMotifs: List[String] = List("c-file denied", "b4 entry denied", "the b4 to a5 route stays cut off"),
      purpose: String = ThemePlanProbePurpose.RouteDenialValidation,
      evalCp: Int = 202,
      seedId: Option[String] = None,
      variationHash: Option[String] = None
  ): ProbeResult =
    ProbeResult(
      id = id,
      fen = Some(PositiveFen),
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
      futureSnapshot = futureSnapshot,
      seedId = seedId,
      variationHash = variationHash
    )

  private def continuityProbe(
      id: String,
      branch: List[String] = List("a7a5", "b4a5"),
      futureSnapshot: Option[FutureSnapshot] = Some(routeSnapshot()),
      keyMotifs: List[String] = List("conversion route stabilizes", "the b4 to a5 route stays cut off"),
      evalCp: Int = 200,
      seedId: Option[String] = None,
      variationHash: Option[String] = None
  ): ProbeResult =
    validationProbe(
      id = id,
      branch = branch,
      futureSnapshot = futureSnapshot,
      keyMotifs = keyMotifs,
      purpose = "convert_reply_multipv",
      evalCp = evalCp,
      seedId = seedId,
      variationHash = variationHash
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

  private def preventedReroutePlan(
      square: String = "a5",
      counterplayScoreDrop: Int = 122,
      resourceClass: String = "reroute_square"
  ): PreventedPlan =
    PreventedPlan(
      planId = s"deny_$square",
      deniedSquares = List(Square.fromKey(square).get),
      breakNeutralized = None,
      mobilityDelta = -1,
      counterplayScoreDrop = counterplayScoreDrop,
      preventedThreatType = Some("counterplay"),
      deniedResourceClass = Some(resourceClass),
      deniedEntryScope = Some("single_square"),
      breakNeutralizationStrength = Some(74),
      defensiveSufficiency = Some(73),
      sourceScope = FactScope.Now
    )

  private val scenarios =
    List(
      Scenario(
        id = "same_branch_reroute_denial_positive",
        fen = PositiveFen,
        planName = "Take the c-file away, keep b4 closed, and cut off the a5 reroute",
        probes =
          List(
            directReplyProbe("positive_reply"),
            validationProbe("positive_validation"),
            continuityProbe("positive_continuity")
          ),
        preventedPlans = List(preventedFilePlan(), preventedEntryPlan(), preventedReroutePlan()),
        evalCp = 207,
        expectedCertified = true,
        expectedFails = Set.empty
      ),
      Scenario(
        id = "file_entry_restatement_only",
        fen = PositiveFen,
        planName = "Take the c-file away and keep b4 closed as a route network",
        probes =
          List(
            directReplyProbe("restatement_reply"),
            validationProbe("restatement_validation"),
            continuityProbe("restatement_continuity")
          ),
        preventedPlans = List(preventedFilePlan(), preventedEntryPlan()),
        evalCp = 205,
        expectedCertified = false,
        expectedFails = Set("file_entry_restatement_only")
      ),
      Scenario(
        id = "redundant_square_counting",
        fen = PositiveFen,
        planName = "Take the c-file away, keep b4 closed, and claim c5 as the reroute",
        probes =
          List(
            directReplyProbe("redundant_reply"),
            validationProbe("redundant_validation"),
            continuityProbe("redundant_continuity")
          ),
        preventedPlans = List(preventedFilePlan(), preventedEntryPlan(), preventedReroutePlan(square = "c5")),
        evalCp = 205,
        expectedCertified = false,
        expectedFails = Set("redundant_square_counting")
      ),
      Scenario(
        id = "route_network_mirage",
        fen = PositiveFen,
        planName = "Take the c-file away, keep b4 closed, and deny the a5 reroute",
        probes =
          List(
            directReplyProbe(
              "mirage_reply",
              futureSnapshot = Some(routeSnapshot(routeEdgeVisible = false, continuationVisible = true)),
              keyMotifs = List("c-file denied", "b4 entry denied")
            ),
            validationProbe(
              "mirage_validation",
              futureSnapshot = Some(routeSnapshot(routeEdgeVisible = false, continuationVisible = true)),
              keyMotifs = List("c-file denied", "b4 entry denied")
            ),
            continuityProbe(
              "mirage_continuity",
              futureSnapshot = Some(routeSnapshot(routeEdgeVisible = false, continuationVisible = true)),
              keyMotifs = List("conversion route stabilizes")
            )
          ),
        preventedPlans = List(preventedFilePlan(), preventedEntryPlan(), preventedReroutePlan()),
        evalCp = 205,
        expectedCertified = false,
        expectedFails = Set("route_network_mirage")
      ),
      Scenario(
        id = "same_first_move_divergent_branch",
        fen = PositiveFen,
        planName = "Take the c-file away, keep b4 closed, and deny the a5 reroute on the same branch",
        probes =
          List(
            directReplyProbe("divergent_reply", branch = List("a7a5", "b4a5", "c6a5")),
            validationProbe(
              "divergent_validation",
              branch = List("a7a5", "h4h5"),
              futureSnapshot = Some(routeSnapshot()),
              keyMotifs = List("the b4 to a5 route stays cut off")
            ),
            continuityProbe("divergent_continuity", branch = List("a7a5", "b4a5"))
          ),
        preventedPlans = List(preventedFilePlan(), preventedEntryPlan(), preventedReroutePlan()),
        evalCp = 205,
        expectedCertified = false,
        expectedFails = Set("cross_branch_stitching", "engine_pv_paraphrase")
      ),
      Scenario(
        id = "ambiguous_defended_branch",
        fen = PositiveFen,
        planName = "Take the c-file away, keep b4 closed, and deny the a5 reroute",
        probes =
          List(
            directReplyProbe("ambiguous_reply_a", branch = List("a7a5", "b4a5", "c6a5")),
            directReplyProbe("ambiguous_reply_b", branch = List("h7h5", "g4g5", "h6g5")),
            validationProbe("ambiguous_validation", branch = List("a7a5", "b4a5")),
            continuityProbe("ambiguous_continuity", branch = List("a7a5", "b4a5"))
          ),
        preventedPlans = List(preventedFilePlan(), preventedEntryPlan(), preventedReroutePlan()),
        evalCp = 205,
        expectedCertified = false,
        expectedFails = Set("ambiguous_defended_branch")
      ),
      Scenario(
        id = "untouched_sector_reroute",
        fen = PositiveFen,
        planName = "Take the c-file away, keep b4 closed, and cut off the a5 reroute",
        probes =
          List(
            directReplyProbe("untouched_reply"),
            validationProbe("untouched_validation"),
            continuityProbe("untouched_continuity")
          ),
        preventedPlans =
          List(
            preventedFilePlan(),
            preventedEntryPlan(),
            preventedReroutePlan(square = "a5"),
            preventedReroutePlan(square = "h5", counterplayScoreDrop = 118)
          ),
        evalCp = 205,
        expectedCertified = false,
        expectedFails = Set("untouched_sector_reroute")
      ),
      Scenario(
        id = "color_complex_escape",
        fen = PositiveFen,
        planName = "Take the c-file away, keep b4 closed, and deny the a5 reroute",
        probes =
          List(
            directReplyProbe(
              "color_reply",
              futureSnapshot = Some(routeSnapshot(extraPrereqs = List("opposite-color escape remains"))),
              keyMotifs = List("c-file denied", "b4 entry denied", "the b4 to a5 route stays cut off", "opposite-color escape remains")
            ),
            validationProbe(
              "color_validation",
              futureSnapshot = Some(routeSnapshot(extraPrereqs = List("opposite-color escape remains"))),
              keyMotifs = List("the b4 to a5 route stays cut off", "opposite-color escape remains")
            ),
            continuityProbe("color_continuity")
          ),
        preventedPlans = List(preventedFilePlan(), preventedEntryPlan(), preventedReroutePlan()),
        evalCp = 205,
        expectedCertified = false,
        expectedFails = Set("color_complex_escape")
      ),
      Scenario(
        id = "move_order_fragility",
        fen = PositiveFen,
        planName = "Take the c-file away, keep b4 closed, and cut off the a5 reroute",
        probes =
          List(
            directReplyProbe("fragile_reply", collapseReason = Some("exact move order only")),
            validationProbe("fragile_validation"),
            continuityProbe("fragile_continuity")
          ),
        preventedPlans = List(preventedFilePlan(), preventedEntryPlan(), preventedReroutePlan()),
        evalCp = 205,
        expectedCertified = false,
        expectedFails = Set("move_order_fragility")
      ),
      Scenario(
        id = "cross_branch_stitching",
        fen = PositiveFen,
        planName = "Take the c-file away, keep b4 closed, and deny the a5 reroute",
        probes =
          List(
            directReplyProbe("stitched_reply", branch = List("a7a5", "b4a5", "c6a5")),
            validationProbe(
              "stitched_validation",
              branch = List("c6a5", "b4a5"),
              futureSnapshot = Some(routeSnapshot()),
              keyMotifs = List("the b4 to a5 route stays cut off")
            ),
            continuityProbe("stitched_continuity")
          ),
        preventedPlans = List(preventedFilePlan(), preventedEntryPlan(), preventedReroutePlan()),
        evalCp = 205,
        expectedCertified = false,
        expectedFails = Set("cross_branch_stitching")
      ),
      Scenario(
        id = "static_net_without_progress",
        fen = PositiveFen,
        planName = "Take the c-file away, keep b4 closed, and deny the a5 reroute",
        probes =
          List(
            directReplyProbe("static_reply", futureSnapshot = Some(routeSnapshot(continuationVisible = false))),
            validationProbe("static_validation", futureSnapshot = Some(routeSnapshot(continuationVisible = false))),
            continuityProbe(
              "static_continuity",
              futureSnapshot = Some(routeSnapshot(continuationVisible = false)),
              keyMotifs = List("the b4 to a5 route stays cut off")
            )
          ),
        preventedPlans = List(preventedFilePlan(), preventedEntryPlan(), preventedReroutePlan()),
        evalCp = 205,
        expectedCertified = false,
        expectedFails = Set("static_net_without_progress")
      ),
      Scenario(
        id = "engine_pv_paraphrase",
        fen = PositiveFen,
        planName = "Take the c-file away, keep b4 closed, and deny the a5 reroute",
        probes =
          List(
            directReplyProbe(
              "pv_only_reply",
              futureSnapshot = Some(routeSnapshot()),
              keyMotifs = List("c-file denied", "the b4 to a5 route stays cut off")
            )
          ),
        preventedPlans = List(preventedFilePlan(), preventedEntryPlan(), preventedReroutePlan()),
        evalCp = 205,
        expectedCertified = false,
        expectedFails = Set("engine_pv_paraphrase")
      ),
      Scenario(
        id = "missing_branch_identity_fails_close",
        fen = PositiveFen,
        planName = "Take the c-file away, keep b4 closed, and deny the a5 reroute",
        probes =
          List(
            directReplyProbe("identity_missing_reply", branch = List("a7a5")),
            validationProbe("identity_missing_validation", branch = List("a7a5")),
            continuityProbe("identity_missing_continuity", branch = List("a7a5"))
          ),
        preventedPlans = List(preventedFilePlan(), preventedEntryPlan(), preventedReroutePlan()),
        evalCp = 205,
        expectedCertified = false,
        expectedFails = Set("same_branch_identity_missing")
      ),
      Scenario(
        id = "surface_reinflation",
        fen = PositiveFen,
        planName = "Take the c-file away, keep b4 closed, and completely shut down the whole position via the a5 reroute",
        probes =
          List(
            directReplyProbe("inflation_reply"),
            validationProbe("inflation_validation"),
            continuityProbe("inflation_continuity")
          ),
        preventedPlans = List(preventedFilePlan(), preventedEntryPlan(), preventedReroutePlan()),
        evalCp = 205,
        expectedCertified = false,
        expectedFails = Set("surface_reinflation")
      ),
      Scenario(
        id = "heavy_piece_release_shell",
        fen = HeavyPieceFen,
        planName = "Take the c-file away, keep b4 closed, and cut off the a5 reroute",
        probes =
          List(
            directReplyProbe("heavy_reply"),
            validationProbe("heavy_validation"),
            continuityProbe("heavy_continuity")
          ),
        preventedPlans = List(preventedFilePlan(), preventedEntryPlan(), preventedReroutePlan()),
        evalCp = 205,
        expectedCertified = false,
        expectedFails = Set("heavy_piece_release_shell")
      )
    )

  private def contractFor(
      scenario: Scenario
  ): (
      PlanEvidenceEvaluator.EvaluatedPlan,
      LocalFileEntryBindCertification.Contract,
      NamedRouteNetworkBindCertification.Contract
  ) =
    val plan = routePlan(scenario)
    val probesById = scenario.probes.map(probe => probe.id -> probe).toMap
    val local =
      LocalFileEntryBindCertification
        .evaluate(
          plan = plan,
          probeResultsById = probesById,
          preventedPlans = scenario.preventedPlans,
          evalCp = scenario.evalCp,
          isWhiteToMove = true,
          phase = "middlegame",
          ply = 30,
          fen = scenario.fen
        )
        .getOrElse(fail(s"missing B4 prereq contract for ${scenario.id}"))
    val route =
      NamedRouteNetworkBindCertification
        .evaluate(
          plan = plan,
          probeResultsById = probesById,
          preventedPlans = scenario.preventedPlans,
          evalCp = scenario.evalCp,
          isWhiteToMove = true,
          phase = "middlegame",
          ply = 30,
          fen = scenario.fen,
          localFileEntryBindCertification = Some(local)
        )
        .getOrElse(fail(s"missing B6 contract for ${scenario.id}"))
    (plan, local, route)

  private def toInfo(
      plan: PreventedPlan
  ): PreventedPlanInfo =
    PreventedPlanInfo(
      planId = plan.planId,
      deniedSquares = plan.deniedSquares.map(_.key),
      breakNeutralized = plan.breakNeutralized,
      mobilityDelta = plan.mobilityDelta,
      counterplayScoreDrop = plan.counterplayScoreDrop,
      preventedThreatType = plan.preventedThreatType,
      sourceScope = plan.sourceScope,
      citationLine = None,
      deniedResourceClass = plan.deniedResourceClass,
      deniedEntryScope = plan.deniedEntryScope
    )

  private def question(
      id: String,
      kind: AuthorQuestionKind
  ): AuthorQuestion =
    AuthorQuestion(
      id = id,
      kind = kind,
      priority = 100,
      question = s"placeholder-$id",
      evidencePurposes = Nil
    )

  private def mainClaim(
      text: String
  ): MainPathScopedClaim =
    MainPathScopedClaim(
      scope = PlayerFacingClaimScope.MoveLocal,
      mode = PlayerFacingTruthMode.Strategic,
      deltaClass = Some(PlayerFacingMoveDeltaClass.PlanAdvance),
      claimText = text,
      anchorTerms = List("c-file", "b4"),
      evidenceLines = List("24.a3b4 a7a5 25.bxa5"),
      sourceKind = "main_delta",
      tacticalOwnership = None
    )

  private def lineClaim(
      text: String
  ): MainPathScopedClaim =
    MainPathScopedClaim(
      scope = PlayerFacingClaimScope.LineScoped,
      mode = PlayerFacingTruthMode.Strategic,
      deltaClass = Some(PlayerFacingMoveDeltaClass.PlanAdvance),
      claimText = text,
      anchorTerms = List("a5"),
      evidenceLines = List(text),
      sourceKind = "line_delta",
      tacticalOwnership = None
    )

  private def plannerInputs(
      evidenceBackedPlans: List[PlanHypothesis],
      preventedPlansNow: List[PreventedPlanInfo],
      namedRouteNetworkSurface: Option[NamedRouteNetworkBindCertification.SurfaceNetwork] = None
  ): QuestionPlannerInputs =
    QuestionPlannerInputs(
      mainBundle =
        Some(
          MainPathClaimBundle(
            mainClaim = Some(mainClaim("This keeps b4 closed and takes the c-file away.")),
            lineScopedClaim = Some(lineClaim("24.a3b4 a7a5 25.bxa5"))
          )
        ),
      quietIntent = None,
      decisionFrame = CertifiedDecisionFrame(),
      decisionComparison = None,
      alternativeNarrative = None,
      truthMode = PlayerFacingTruthMode.Strategic,
      preventedPlansNow = preventedPlansNow,
      pvDelta = None,
      counterfactual = None,
      practicalAssessment = None,
      opponentThreats = Nil,
      forcingThreats = Nil,
      evidenceByQuestionId = Map.empty,
      candidateEvidenceLines = List("24.a3b4 a7a5 25.bxa5"),
      evidenceBackedPlans = evidenceBackedPlans,
      opponentPlan = None,
      factualFallback = Some("This keeps the position under control."),
      heavyPieceLocalBindBlocked = false,
      namedRouteNetworkSurface = namedRouteNetworkSurface
    )

  test("exact-position narrow corpus accepts only same-branch reroute denial and rejects route mirages") {
    scenarios.foreach { scenario =>
      val (_, local, route) = contractFor(scenario)

      if scenario.expectedCertified then
        assert(local.certified, clue(scenario.id -> local))
        assert(route.certified, clue(scenario.id -> route))
        assertEquals(route.sameDefendedBranch, true, clue(scenario.id -> route))
        assertEquals(route.routeContinuity.routeEdgeVisible, true, clue(scenario.id -> route))
        assertEquals(route.rerouteDenials.map(_.square), List("a5"), clue(scenario.id -> route))
      else
        assertEquals(route.certified, false, clue(scenario.id -> route))
        assert(
          scenario.expectedFails.subsetOf(route.failsIf.toSet),
          clues(scenario.id, scenario.expectedFails, route.failsIf)
        )
    }
  }

  test("named route-network surface is planner-only and replay selections stay fail-closed") {
    val positive =
      scenarios.find(_.id == "same_branch_reroute_denial_positive").getOrElse(fail("missing positive scenario"))
    val (plan, _, route) = contractFor(positive)
    assert(route.certified, clue(route))

    val surface =
      NamedRouteNetworkBindCertification
        .certifiedSurfaceNetwork(
          preventedPlans = positive.preventedPlans.map(toInfo),
          evidenceBackedPlans = List(plan.hypothesis)
        )
        .getOrElse(fail("missing surface network"))
    val plannerInputs =
      this.plannerInputs(
        evidenceBackedPlans = List(plan.hypothesis),
        preventedPlansNow = positive.preventedPlans.map(toInfo),
        namedRouteNetworkSurface = Some(surface)
      )
    val rankedFromPlanner =
      QuestionFirstCommentaryPlanner.plan(
        ply = 30,
        authorQuestions = List(question("q_why", AuthorQuestionKind.WhyThis)),
        inputs = plannerInputs,
        truthContract = None
      )
    val routeWhyThis =
      rankedFromPlanner.primary.getOrElse(fail("missing planner why-this"))
    val whyThisClaim = routeWhyThis.claim
    val whatChanged =
      QuestionPlan(
        questionId = "q_changed",
        questionKind = AuthorQuestionKind.WhatChanged,
        priority = 90,
        claim = "This changes the position by taking the c-file away as a counterplay route and closing b4.",
        evidence =
          Some(
            QuestionPlanEvidence(
              text = "a) 24...a5 25.bxa5 keeps the entry sealed.",
              purposes = List("reply_multipv"),
              sourceKinds = List("prevented_plan")
            )
          ),
        contrast = Some("Before the move, the c-file and b4 were still available."),
        consequence =
          Some(
            QuestionPlanConsequence(
              "That removes roughly 145cp of counterplay from the local route.",
              QuestionPlanConsequenceBeat.WrapUp
            )
          ),
        fallbackMode = QuestionPlanFallbackMode.PlannerOwned,
        strengthTier = QuestionPlanStrengthTier.Moderate,
        sourceKinds = List("prevented_plan"),
        admissibilityReasons = List("test"),
        ownerFamily = OwnerFamily.MoveDelta,
        ownerSource = "prevented_plan"
      )
    val ranked =
      RankedQuestionPlans(
        primary = Some(routeWhyThis),
        secondary = Some(whatChanged),
        rejected = Nil
      )
    assert(whyThisClaim.contains("c-file"), clue(whyThisClaim))
    assert(whyThisClaim.contains("b4"), clue(whyThisClaim))
    assert(whyThisClaim.contains("a5"), clue(whyThisClaim))
    assert(whyThisClaim.toLowerCase.contains("reroute"), clue(whyThisClaim))
    assert(routeWhyThis.sourceKinds.contains(NamedRouteNetworkBindCertification.OwnerSource), clue(routeWhyThis))
    assertEquals(whatChanged.ownerSource == NamedRouteNetworkBindCertification.OwnerSource, false, clue(whatChanged))
    assertEquals(whatChanged.claim.toLowerCase.contains("reroute"), false, clue(whatChanged.claim))

    val chronicle =
      GameChronicleCompressionPolicy.selectPlannerSurface(ranked, plannerInputs)
    val bookmaker =
      BookmakerLiveCompressionPolicy.renderSelection(plannerInputs, ranked, None)
    val active =
      ActiveStrategicCoachingBriefBuilder.selectPlannerSurface(
        ActiveStrategicCoachingBriefBuilder.PlannerReplay(
          authorQuestions = Nil,
          inputs = plannerInputs,
          rankedPlans = ranked
        )
      )

    assert(
      chronicle.forall(!_.primary.sourceKinds.contains(NamedRouteNetworkBindCertification.OwnerSource)),
      clue(chronicle)
    )
    assert(
      bookmaker.forall(!_.primary.sourceKinds.contains(NamedRouteNetworkBindCertification.OwnerSource)),
      clue(bookmaker)
    )
    assert(
      active.forall(!_.primary.sourceKinds.contains(NamedRouteNetworkBindCertification.OwnerSource)),
      clue(active)
    )
  }

  test("planner route wording uses the exact certified triplet instead of recomposing a stronger network") {
    val positive =
      scenarios.find(_.id == "same_branch_reroute_denial_positive").getOrElse(fail("missing positive scenario"))
    val narrowPlan = routePlan(positive).hypothesis
    val broaderPlan =
      PlanHypothesis(
        planId = "broader_route_shell",
        planName = "Take the c-file away, keep d5 closed, and cut off the f5 reroute",
        rank = 2,
        score = 0.79,
        preconditions = Nil,
        executionSteps =
          List("Take the c-file away, keep d5 closed, and cut off the f5 reroute."),
        failureModes = List("If d5 or f5 reopens, the broader route shell disappears."),
        viability = PlanViability(score = 0.75, label = "medium", risk = "recomposition shell"),
        evidenceSources = List(s"fen:$PositiveFen", "fixture:planner_raw_recomposition_stronger_than_contract"),
        themeL1 = ThemeTaxonomy.ThemeL1.RestrictionProphylaxis.id,
        subplanId = Some(ThemeTaxonomy.SubplanId.BreakPrevention.id)
      )
    val preventedPlansNow =
      List(
        preventedFilePlan(),
        preventedEntryPlan(square = "b4", counterplayScoreDrop = 130),
        preventedEntryPlan(square = "d5", counterplayScoreDrop = 168),
        preventedReroutePlan(square = "a5", counterplayScoreDrop = 122),
        preventedReroutePlan(square = "f5", counterplayScoreDrop = 181)
      ).map(toInfo)
    val carriedTriplet =
      NamedRouteNetworkBindCertification.SurfaceNetwork(
        file = "c-file",
        entrySquare = "b4",
        rerouteSquare = "a5",
        counterplayScoreDrop = 145
      )
    val inputs =
      plannerInputs(
        evidenceBackedPlans = List(broaderPlan, narrowPlan),
        preventedPlansNow = preventedPlansNow,
        namedRouteNetworkSurface = Some(carriedTriplet)
      )
    val ranked =
      QuestionFirstCommentaryPlanner.plan(
        ply = 30,
        authorQuestions = List(question("q_why", AuthorQuestionKind.WhyThis)),
        inputs = inputs,
        truthContract = None
      )
    val primary = ranked.primary.getOrElse(fail("missing why-this primary"))

    assertEquals(
      NamedRouteNetworkBindCertification.certifiedSurfaceNetwork(preventedPlansNow, List(broaderPlan, narrowPlan)),
      None
    )
    assert(primary.claim.contains("c-file"), clue(primary))
    assert(primary.claim.contains("b4"), clue(primary))
    assert(primary.claim.contains("a5"), clue(primary))
    assert(!primary.claim.contains("d5"), clue(primary))
    assert(!primary.claim.contains("f5"), clue(primary))
    assert(primary.sourceKinds.contains(NamedRouteNetworkBindCertification.OwnerSource), clue(primary))
  }
