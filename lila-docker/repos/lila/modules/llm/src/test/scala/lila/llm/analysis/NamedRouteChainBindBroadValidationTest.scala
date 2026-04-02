package lila.llm.analysis

import chess.Square
import munit.FunSuite

import lila.llm.*
import lila.llm.model.*
import lila.llm.model.authoring.*
import lila.llm.model.strategic.PreventedPlan

class NamedRouteChainBindBroadValidationTest extends FunSuite:

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
          score = 0.88,
          preconditions = Nil,
          executionSteps =
            List(
              "Take the c-file away, keep b4 closed, and stop the a5 route from continuing to c4."
            ),
          failureModes =
            List(
              "If b4, a5, or c4 reopen on the best defense branch, the route chain no longer holds."
            ),
          viability = PlanViability(score = 0.84, label = "high", risk = "B6 chain narrow"),
          evidenceSources = List(s"fen:${scenario.fen}", s"fixture:${scenario.id}"),
          themeL1 = ThemeTaxonomy.ThemeL1.RestrictionProphylaxis.id,
          subplanId = Some(ThemeTaxonomy.SubplanId.BreakPrevention.id)
        ),
      status = PlanEvidenceEvaluator.PlanEvidenceStatus.PlayableEvidenceBacked,
      userFacingEligibility = PlanEvidenceEvaluator.UserFacingPlanEligibility.ProbeBacked,
      reason = "B6 route-chain broad validation",
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

  private def chainSnapshot(
      intermediateSquare: String = "a5",
      rerouteSquare: String = "c4",
      includeEdges: Boolean = true,
      continuationVisible: Boolean = true,
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
          Option.when(continuationVisible)("conversion route stabilizes").toList ++ extraAdded,
          List(rerouteSquare)
        ),
      planBlockersRemoved =
        List("the c-file stays closed") ++
          Option.when(includeEdges)(s"the $intermediateSquare route cannot continue to $rerouteSquare").toList,
      planPrereqsMet =
        List("b4 stays unavailable") ++
          Option.when(includeEdges)(s"the b4 route dead-ends on $intermediateSquare").toList ++
          Option.when(includeEdges)(s"the $intermediateSquare route cannot continue to $rerouteSquare").toList ++
          Option.when(continuationVisible)("conversion route stabilizes").toList ++
          extraPrereqs
    )

  private def directReplyProbe(
      id: String,
      fen: String = PositiveFen,
      branch: List[String] = List("a7a5", "b4a5", "c6a5", "f3e5"),
      futureSnapshot: Option[FutureSnapshot] = Some(chainSnapshot()),
      keyMotifs: List[String] =
        List(
          "c-file denied",
          "the b4 to a5 route stays cut off",
          "the a5 to c4 reroute stays unavailable",
          "Ne5 keeps c4 covered"
        ),
      collapseReason: Option[String] = None,
      purpose: String = "defense_reply_multipv",
      evalCp: Int = 205
  ): ProbeResult =
    ProbeResult(
      id = id,
      fen = Some(fen),
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
      futureSnapshot = futureSnapshot
    )

  private def validationProbe(
      id: String,
      fen: String = PositiveFen,
      branch: List[String] = List("a7a5", "b4a5", "c6a5", "f3e5"),
      futureSnapshot: Option[FutureSnapshot] = Some(chainSnapshot()),
      keyMotifs: List[String] =
        List(
          "the b4 to a5 route stays cut off",
          "the a5 to c4 reroute stays unavailable",
          "Ne5 keeps c4 covered"
        ),
      purpose: String = ThemePlanProbePurpose.RouteDenialValidation,
      evalCp: Int = 202
  ): ProbeResult =
    ProbeResult(
      id = id,
      fen = Some(fen),
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

  private def continuityProbe(
      id: String,
      fen: String = PositiveFen,
      branch: List[String] = List("a7a5", "b4a5", "c6a5", "f3e5"),
      futureSnapshot: Option[FutureSnapshot] = Some(chainSnapshot()),
      keyMotifs: List[String] =
        List(
          "the a5 to c4 reroute stays unavailable",
          "conversion route stabilizes"
        ),
      evalCp: Int = 200
  ): ProbeResult =
    validationProbe(
      id = id,
      fen = fen,
      branch = branch,
      futureSnapshot = futureSnapshot,
      keyMotifs = keyMotifs,
      purpose = "convert_reply_multipv",
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

  private def preventedRouteNodePlan(
      square: String = "a5",
      counterplayScoreDrop: Int = 118
  ): PreventedPlan =
    PreventedPlan(
      planId = s"route_$square",
      deniedSquares = List(Square.fromKey(square).get),
      breakNeutralized = None,
      mobilityDelta = -1,
      counterplayScoreDrop = counterplayScoreDrop,
      preventedThreatType = Some("counterplay"),
      deniedResourceClass = Some("route_node"),
      deniedEntryScope = Some("single_square"),
      breakNeutralizationStrength = Some(72),
      defensiveSufficiency = Some(71),
      sourceScope = FactScope.Now
    )

  private def preventedReroutePlan(
      square: String = "c4",
      counterplayScoreDrop: Int = 128,
      fenSquare: String = "c4"
  ): PreventedPlan =
    PreventedPlan(
      planId = s"deny_$square",
      deniedSquares = List(Square.fromKey(fenSquare).get),
      breakNeutralized = None,
      mobilityDelta = -1,
      counterplayScoreDrop = counterplayScoreDrop,
      preventedThreatType = Some("counterplay"),
      deniedResourceClass = Some("reroute_square"),
      deniedEntryScope = Some("single_square"),
      breakNeutralizationStrength = Some(75),
      defensiveSufficiency = Some(74),
      sourceScope = FactScope.Now
    )

  private val scenarios =
    List(
      Scenario(
        id = "same_branch_route_chain_backend_control",
        fen = PositiveFen,
        planName = "Take the c-file away, keep b4 closed, and stop the a5 route from continuing to c4",
        probes =
          List(
            directReplyProbe("positive_reply"),
            validationProbe("positive_validation"),
            continuityProbe("positive_continuity")
          ),
        preventedPlans =
          List(preventedFilePlan(), preventedEntryPlan(), preventedRouteNodePlan(), preventedReroutePlan()),
        evalCp = 207,
        expectedCertified = true,
        expectedFails = Set.empty
      ),
      Scenario(
        id = "fake_route_chain",
        fen = PositiveFen,
        planName = "Take the c-file away, keep b4 closed, and claim the a5 route is dead before c4",
        probes =
          List(
            directReplyProbe(
              "fake_reply",
              futureSnapshot = Some(chainSnapshot(includeEdges = false)),
              keyMotifs = List("route shell still looks restricted", "c-file denied")
            ),
            validationProbe(
              "fake_validation",
              futureSnapshot = Some(chainSnapshot(includeEdges = false)),
              keyMotifs = List("route shell still looks restricted")
            ),
            continuityProbe(
              "fake_continuity",
              futureSnapshot = Some(chainSnapshot(includeEdges = false)),
              keyMotifs = List("conversion route stabilizes")
            )
          ),
        preventedPlans =
          List(preventedFilePlan(), preventedEntryPlan(), preventedRouteNodePlan(), preventedReroutePlan()),
        evalCp = 205,
        expectedCertified = false,
        expectedFails = Set("fake_route_chain")
      ),
      Scenario(
        id = "redundant_intermediate_node",
        fen = PositiveFen,
        planName = "Take the c-file away, keep b4 closed, and pretend the a5 reroute is also the route-chain bridge to c4",
        probes =
          List(
            directReplyProbe("redundant_reply"),
            validationProbe("redundant_validation"),
            continuityProbe("redundant_continuity")
          ),
        preventedPlans =
          List(
            preventedFilePlan(),
            preventedEntryPlan(),
            preventedReroutePlan(square = "a5", counterplayScoreDrop = 119, fenSquare = "a5"),
            preventedReroutePlan()
          ),
        evalCp = 205,
        expectedCertified = false,
        expectedFails = Set("redundant_intermediate_node")
      ),
      Scenario(
        id = "chain_only_on_nonbest_branch",
        fen = PositiveFen,
        planName = "Take the c-file away, keep b4 closed, and stop the a5 route from continuing to c4 on the same branch",
        probes =
          List(
            directReplyProbe(
              "branch_reply",
              keyMotifs = List("the b4 to a5 route stays cut off")
            ),
            validationProbe(
              "branch_validation_same",
              keyMotifs = List("the b4 to a5 route stays cut off")
            ),
            validationProbe(
              "branch_validation_other",
              branch = List("h7h5", "g2g4", "h5g4", "a5c4"),
              keyMotifs = List("the a5 to c4 reroute stays unavailable"),
              futureSnapshot =
                Some(
                  chainSnapshot(
                    extraPrereqs = List("other branch only"),
                    extraAdded = List("other branch continuation")
                  )
                )
            ),
            continuityProbe("branch_continuity")
          ),
        preventedPlans =
          List(preventedFilePlan(), preventedEntryPlan(), preventedRouteNodePlan(), preventedReroutePlan()),
        evalCp = 205,
        expectedCertified = false,
        expectedFails = Set("chain_only_on_nonbest_branch", "fake_route_chain")
      ),
      Scenario(
        id = "untouched_sector_escape",
        fen = PositiveFen,
        planName = "Take the c-file away, keep b4 closed, and stop the a5 route from continuing to c4",
        probes =
          List(
            directReplyProbe(
              "sector_reply",
              futureSnapshot = Some(chainSnapshot(extraPrereqs = List("untouched sector route remains"))),
              keyMotifs =
                List(
                  "the b4 to a5 route stays cut off",
                  "the a5 to c4 reroute stays unavailable",
                  "untouched sector route remains"
                )
            ),
            validationProbe("sector_validation"),
            continuityProbe("sector_continuity")
          ),
        preventedPlans =
          List(
            preventedFilePlan(),
            preventedEntryPlan(),
            preventedRouteNodePlan(),
            preventedReroutePlan(),
            preventedReroutePlan(square = "h5", counterplayScoreDrop = 121, fenSquare = "h5")
          ),
        evalCp = 205,
        expectedCertified = false,
        expectedFails = Set("untouched_sector_escape")
      ),
      Scenario(
        id = "posture_inflation",
        fen = PositiveFen,
        planName = "Take the c-file away, keep b4 closed, and stop the a5 route from continuing to c4",
        probes =
          List(
            directReplyProbe("posture_reply"),
            validationProbe("posture_validation"),
            continuityProbe("posture_continuity")
          ),
        preventedPlans =
          List(preventedFilePlan(), preventedEntryPlan(), preventedRouteNodePlan(), preventedReroutePlan()),
        evalCp = 70,
        expectedCertified = false,
        expectedFails = Set("posture_inflation", "slight_edge_overclaim")
      ),
      Scenario(
        id = "surface_reinflation",
        fen = PositiveFen,
        planName = "Take the c-file away, keep b4 closed, stop the a5 route from continuing to c4, and leave Black with no counterplay in the whole position",
        probes =
          List(
            directReplyProbe("surface_reply"),
            validationProbe("surface_validation"),
            continuityProbe("surface_continuity")
          ),
        preventedPlans =
          List(preventedFilePlan(), preventedEntryPlan(), preventedRouteNodePlan(), preventedReroutePlan()),
        evalCp = 205,
        expectedCertified = false,
        expectedFails = Set("surface_reinflation", "replay_reinflation", "whole_game_wrapper_leak")
      ),
      Scenario(
        id = "heavy_piece_route_shell",
        fen = HeavyPieceFen,
        planName = "Take the c-file away, keep b4 closed, and stop the a5 route from continuing to c4 with queens on",
        probes =
          List(
            directReplyProbe("heavy_reply", fen = HeavyPieceFen),
            validationProbe("heavy_validation", fen = HeavyPieceFen),
            continuityProbe("heavy_continuity", fen = HeavyPieceFen)
          ),
        preventedPlans =
          List(preventedFilePlan(), preventedEntryPlan(), preventedRouteNodePlan(), preventedReroutePlan()),
        evalCp = 205,
        expectedCertified = false,
        expectedFails = Set("heavy_piece_route_shell", "heavy_piece_release_shell")
      ),
      Scenario(
        id = "engine_pv_paraphrase",
        fen = PositiveFen,
        planName = "Take the c-file away, keep b4 closed, and stop the a5 route from continuing to c4",
        probes =
          List(
            directReplyProbe(
              "pv_only_reply",
              keyMotifs = List("c-file denied", "the b4 to a5 route stays cut off")
            )
          ),
        preventedPlans =
          List(preventedFilePlan(), preventedEntryPlan(), preventedRouteNodePlan(), preventedReroutePlan()),
        evalCp = 205,
        expectedCertified = false,
        expectedFails = Set("engine_pv_paraphrase")
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
      evidenceLines = List("24.a3b4 a7a5 25.bxa5 Nxa5 26.Ne5"),
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
      anchorTerms = List("a5", "c4"),
      evidenceLines = List(text),
      sourceKind = "line_delta",
      tacticalOwnership = None
    )

  private def plannerInputs(
      evidenceBackedPlans: List[PlanHypothesis],
      preventedPlansNow: List[PreventedPlanInfo],
      namedRouteNetworkSurface: Option[NamedRouteNetworkBindCertification.SurfaceNetwork],
      decisionComparison: Option[DecisionComparison] = None
  ): QuestionPlannerInputs =
    QuestionPlannerInputs(
      mainBundle =
        Some(
          MainPathClaimBundle(
            mainClaim = Some(mainClaim("This keeps b4 closed and takes the c-file away.")),
            lineScopedClaim = Some(lineClaim("24.a3b4 a7a5 25.bxa5 Nxa5 26.Ne5"))
          )
        ),
      quietIntent = None,
      decisionFrame = CertifiedDecisionFrame(),
      decisionComparison = decisionComparison,
      alternativeNarrative = None,
      truthMode = PlayerFacingTruthMode.Strategic,
      preventedPlansNow = preventedPlansNow,
      pvDelta = None,
      counterfactual = None,
      practicalAssessment = None,
      opponentThreats = Nil,
      forcingThreats = Nil,
      evidenceByQuestionId = Map.empty,
      candidateEvidenceLines = List("24.a3b4 a7a5 25.bxa5 Nxa5 26.Ne5"),
      evidenceBackedPlans = evidenceBackedPlans,
      opponentPlan = None,
      factualFallback = Some("This keeps the position under control."),
      heavyPieceLocalBindBlocked = false,
      namedRouteNetworkSurface = namedRouteNetworkSurface
    )

  private def chosenMoveComparison(
      chosenMatchesBest: Boolean
  ): Option[DecisionComparison] =
    Some(
      DecisionComparison(
        chosenMove = Some("b4"),
        engineBestMove = Some(if chosenMatchesBest then "b4" else "Nb4"),
        engineBestScoreCp = Some(190),
        engineBestPv = if chosenMatchesBest then List("b4") else List("Nb4"),
        cpLossVsChosen = Option.when(!chosenMatchesBest)(24),
        deferredMove = Option.when(!chosenMatchesBest)("Nb4"),
        deferredReason = Option.when(!chosenMatchesBest)("The engine keeps a cleaner local route move."),
        deferredSource = Option.when(!chosenMatchesBest)("engine_gap"),
        evidence = Some("local engine comparison"),
        practicalAlternative = false,
        chosenMatchesBest = chosenMatchesBest
      )
    )

  private def assertPlannerRouteChainRemainsBackendOnly(
      chosenMatchesBest: Boolean
  ): Unit =
    val control =
      scenarios
        .find(_.id == "same_branch_route_chain_backend_control")
        .getOrElse(fail("missing backend control scenario"))
    val (plan, _, route) = contractFor(control)
    assert(route.certified, clues(route.failsIf, route.counterplayReinflationRisk, route))

    val surface =
      NamedRouteNetworkBindCertification
        .certifiedSurfaceNetwork(
          preventedPlans = control.preventedPlans.map(toInfo),
          evidenceBackedPlans = List(plan.hypothesis)
        )
        .getOrElse(fail("missing surface network"))
    assertEquals(surface.intermediateSquare, Some("a5"), clue(surface))
    assertEquals(surface.rerouteSquare, "c4", clue(surface))

    val plannerInputs =
      this.plannerInputs(
        evidenceBackedPlans = List(plan.hypothesis),
        preventedPlansNow = control.preventedPlans.map(toInfo),
        namedRouteNetworkSurface = Some(surface),
        decisionComparison = chosenMoveComparison(chosenMatchesBest = chosenMatchesBest)
      )
    val rankedFromPlanner =
      QuestionFirstCommentaryPlanner.plan(
        ply = 30,
        authorQuestions = List(question("q_why", AuthorQuestionKind.WhyThis)),
        inputs = plannerInputs,
        truthContract = None
      )
    val primary =
      rankedFromPlanner.primary.getOrElse(fail("missing planner why-this"))
    val claim = primary.claim.toLowerCase

    assertEquals(primary.sourceKinds.contains(NamedRouteNetworkBindCertification.OwnerSource), false, clue(primary))
    assertEquals(claim.contains("a5"), false, clue(claim))
    assertEquals(claim.contains("c4"), false, clue(claim))
    assertEquals(claim.contains("reroute"), false, clue(claim))

    val chronicle =
      GameChronicleCompressionPolicy.selectPlannerSurface(rankedFromPlanner, plannerInputs)
    val bookmaker =
      BookmakerLiveCompressionPolicy.renderSelection(plannerInputs, rankedFromPlanner, None)
    val active =
      ActiveStrategicCoachingBriefBuilder.selectPlannerSurface(
        ActiveStrategicCoachingBriefBuilder.PlannerReplay(
          authorQuestions = Nil,
          inputs = plannerInputs,
          rankedPlans = rankedFromPlanner
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

  test("exact-position route-chain contract accepts only same-branch intermediate continuation and rejects chain mirages") {
    scenarios.foreach { scenario =>
      val (_, local, route) = contractFor(scenario)

      if scenario.expectedCertified then
        assert(local.certified, clue(scenario.id -> local))
        assert(route.certified, clues(scenario.id, route.failsIf, route.counterplayReinflationRisk, route))
        assertEquals(route.sameDefendedBranch, true, clue(scenario.id -> route))
        assertEquals(route.intermediateNodes.map(_.square), List("a5"), clue(scenario.id -> route))
        assertEquals(route.rerouteDenials.map(_.square), List("c4"), clue(scenario.id -> route))
        assertEquals(route.routeEdges.map(edge => s"${edge.from}->${edge.to}"), List("b4->a5", "a5->c4"), clue(scenario.id -> route))
      else
        assertEquals(route.certified, false, clue(scenario.id -> route))
        assert(
          scenario.expectedFails.subsetOf(route.failsIf.toSet),
          clues(scenario.id, scenario.expectedFails, route.failsIf, route)
        )
    }
  }

  test("planner-owned WhyThis stays fail-closed for route-chain when the chosen move is not root-best") {
    assertPlannerRouteChainRemainsBackendOnly(chosenMatchesBest = false)
  }

  test("planner-owned WhyThis stays fail-closed for route-chain even under synthetic best-move agreement") {
    assertPlannerRouteChainRemainsBackendOnly(chosenMatchesBest = true)
  }

  test("raw route-chain helper may keep the exact chain backend-only while planner wording stays closed") {
    val control =
      scenarios
        .find(_.id == "same_branch_route_chain_backend_control")
        .getOrElse(fail("missing backend control scenario"))
    val exactPlan = routePlan(control).hypothesis
    val broaderPlan =
      PlanHypothesis(
        planId = "broader_route_chain_shell",
        planName = "Take the c-file away, keep d5 active, stop the a5 route from continuing to f5, and squeeze the whole wing",
        rank = 2,
        score = 0.79,
        preconditions = Nil,
        executionSteps =
          List("Take the c-file away, keep d5 active, and stop the a5 route from continuing to f5."),
        failureModes = List("If d5 or f5 reopen, the broader route shell disappears."),
        viability = PlanViability(score = 0.75, label = "medium", risk = "recomposition shell"),
        evidenceSources = List(s"fen:$PositiveFen", "fixture:planner_raw_recomposition_stronger_than_contract"),
        themeL1 = ThemeTaxonomy.ThemeL1.RestrictionProphylaxis.id,
        subplanId = Some(ThemeTaxonomy.SubplanId.BreakPrevention.id)
      )
    val preventedPlansNow =
      List(
        preventedFilePlan(),
        preventedEntryPlan(square = "b4", counterplayScoreDrop = 130),
        preventedRouteNodePlan(square = "a5", counterplayScoreDrop = 118),
        preventedRouteNodePlan(square = "d5", counterplayScoreDrop = 142),
        preventedReroutePlan(square = "c4", counterplayScoreDrop = 128, fenSquare = "c4"),
        preventedReroutePlan(square = "f5", counterplayScoreDrop = 166, fenSquare = "f5")
      ).map(toInfo)
    val carriedChain =
      NamedRouteNetworkBindCertification.SurfaceNetwork(
        file = "c-file",
        entrySquare = "b4",
        rerouteSquare = "c4",
        counterplayScoreDrop = 145,
        intermediateSquare = Some("a5")
      )
    val inputs =
      plannerInputs(
        evidenceBackedPlans = List(broaderPlan, exactPlan),
        preventedPlansNow = preventedPlansNow,
        namedRouteNetworkSurface = Some(carriedChain),
        decisionComparison = chosenMoveComparison(chosenMatchesBest = false)
      )
    val ranked =
      QuestionFirstCommentaryPlanner.plan(
        ply = 30,
        authorQuestions = List(question("q_why", AuthorQuestionKind.WhyThis)),
        inputs = inputs,
        truthContract = None
      )
    val primary = ranked.primary.getOrElse(fail("missing why-this primary"))
    val claim = primary.claim.toLowerCase

    val rawSurface =
      NamedRouteNetworkBindCertification.certifiedSurfaceNetwork(preventedPlansNow, List(broaderPlan, exactPlan))
        .getOrElse(fail("expected the raw helper to keep the exact certified chain, not the broader shell"))
    assertEquals(rawSurface.intermediateSquare, Some("a5"), clue(rawSurface))
    assertEquals(rawSurface.rerouteSquare, "c4", clue(rawSurface))
    assertEquals(primary.sourceKinds.contains(NamedRouteNetworkBindCertification.OwnerSource), false, clue(primary))
    assert(claim.contains("c-file"), clue(primary))
    assert(claim.contains("b4"), clue(primary))
    assert(!claim.contains("a5"), clue(primary))
    assert(!claim.contains("reroute"), clue(primary))
    assert(!claim.contains("d5"), clue(primary))
    assert(!claim.contains("f5"), clue(primary))
    assert(!claim.contains("c4"), clue(primary))
  }
