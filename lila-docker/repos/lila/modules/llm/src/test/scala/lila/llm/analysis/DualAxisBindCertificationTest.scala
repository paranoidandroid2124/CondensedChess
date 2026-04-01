package lila.llm.analysis

import chess.Square
import munit.FunSuite

import lila.llm.model.*
import lila.llm.model.authoring.*
import lila.llm.model.strategic.PreventedPlan

class DualAxisBindCertificationTest extends FunSuite:

  private val QueenlessLateMiddlegameFen =
    "2r2rk1/pp3pp1/2n1p2p/3p4/1p1P1P2/P1P1PN1P/1P4P1/2R2RK1 w - - 0 24"
  private val HeavyPieceMiddlegameFen =
    "r1bq1rk1/pp3ppp/2n1pn2/2bp4/3P4/2N1PN2/PPQ2PPP/R1B2RK1 w - - 0 12"

  private def bindHypothesis(
      id: String = "dual_axis_bind",
      name: String = "Stop the ...c5 break and keep b4 closed",
      executionSteps: List[String] =
        List("Keep the ...c5 break shut while denying the b4 entry square.")
  ): PlanHypothesis =
    PlanHypothesis(
      planId = id,
      planName = name,
      rank = 1,
      score = 0.83,
      preconditions = Nil,
      executionSteps = executionSteps,
      failureModes = List("If either route reopens, the bind disappears."),
      viability = PlanViability(score = 0.81, label = "high", risk = "B3b unit"),
      refutation = None,
      evidenceSources = List("theme:restriction_prophylaxis"),
      themeL1 = ThemeTaxonomy.ThemeL1.RestrictionProphylaxis.id,
      subplanId = Some(ThemeTaxonomy.SubplanId.BreakPrevention.id)
    )

  private def evaluatedRestrictionPlan(
      supportProbeIds: List[String],
      hypothesis: PlanHypothesis = bindHypothesis(),
      claimCertification: PlanEvidenceEvaluator.ClaimCertification =
        PlanEvidenceEvaluator.ClaimCertification(
          certificateStatus = PlayerFacingCertificateStatus.Valid,
          quantifier = PlayerFacingClaimQuantifier.BestResponse,
          modalityTier = PlayerFacingClaimModalityTier.Advances,
          attributionGrade = PlayerFacingClaimAttributionGrade.Distinctive,
          stabilityGrade = PlayerFacingClaimStabilityGrade.Stable,
          provenanceClass = PlayerFacingClaimProvenanceClass.ProbeBacked,
          ontologyFamily = PlayerFacingClaimOntologyFamily.RouteDenial
        ),
      status: PlanEvidenceEvaluator.PlanEvidenceStatus =
        PlanEvidenceEvaluator.PlanEvidenceStatus.PlayableEvidenceBacked,
      missingSignals: List[String] = Nil,
      pvCoupled: Boolean = false
  ): PlanEvidenceEvaluator.EvaluatedPlan =
    PlanEvidenceEvaluator.EvaluatedPlan(
      hypothesis = hypothesis,
      status = status,
      userFacingEligibility = PlanEvidenceEvaluator.UserFacingPlanEligibility.ProbeBacked,
      reason = "B3b unit",
      supportProbeIds = supportProbeIds,
      refuteProbeIds = Nil,
      missingSignals = missingSignals,
      pvCoupled = pvCoupled,
      themeL1 = ThemeTaxonomy.ThemeL1.RestrictionProphylaxis.id,
      subplanId = Some(ThemeTaxonomy.SubplanId.BreakPrevention.id),
      claimCertification = claimCertification
    )

  private def directReplyProbe(
      id: String,
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
      keyMotifs: List[String] = List("...c5 break denied", "b4 entry denied")
  ): ProbeResult =
    ProbeResult(
      id = id,
      evalCp = 195,
      bestReplyPv = List("f8e8", "c1c8"),
      replyPvs =
        Some(
          List(
            List("f8e8", "c1c8"),
            List("a7a5", "g2g4")
          )
        ),
      deltaVsBaseline = 14,
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
      keyMotifs: List[String] = List("...c5 break denied", "b4 entry denied", "conversion route stabilizes")
  ): ProbeResult =
    ProbeResult(
      id = id,
      evalCp = 190,
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

  private def certification(
      plan: PlanEvidenceEvaluator.EvaluatedPlan,
      probes: List[ProbeResult],
      preventedPlans: List[PreventedPlan] = List(preventedBreakPlan(), preventedEntryPlan()),
      evalCp: Int = 190,
      phase: String = "middlegame",
      ply: Int = 30,
      fen: String = QueenlessLateMiddlegameFen
  ): DualAxisBindCertification.Contract =
    DualAxisBindCertification
      .evaluate(
        plan = plan,
        probeResultsById = probes.map(result => result.id -> result).toMap,
        preventedPlans = preventedPlans,
        evalCp = evalCp,
        isWhiteToMove = true,
        phase = phase,
        ply = ply,
        fen = fen
      )
      .getOrElse(fail("expected dual-axis bind contract"))

  test("true_dual_axis_clamp passes certification for a late-middlegame bind shell") {
    val cert =
      certification(
        plan = evaluatedRestrictionPlan(List("probe_reply", "probe_validation")),
        probes = List(directReplyProbe("probe_reply"), validationProbe("probe_validation"))
      )

    assert(cert.certified, clue(cert))
    assertEquals(cert.claimScope, "dual_axis_local")
    assertEquals(cert.bindArchetype, "break_plus_entry")
    assertEquals(cert.primaryAxis.map(_.kind), Some("break_axis"))
    assertEquals(cert.corroboratingAxes.map(_.kind), List("entry_axis"))
    assert(cert.axisIndependence.proven, clue(cert))
    assert(cert.persistenceAfterBestDefense, clue(cert))
    assert(cert.routeContinuity.boundedContinuationVisible, clue(cert))
    assertEquals(cert.counterplayReinflationRisk, "bounded_dual_axis_only")
  }

  test("axis_independence_not_proven fails when the entry axis only restates the break axis") {
    val cert =
      certification(
        plan = evaluatedRestrictionPlan(List("probe_reply", "probe_validation")),
        probes = List(directReplyProbe("probe_reply"), validationProbe("probe_validation")),
        preventedPlans =
          List(
            preventedBreakPlan(),
            preventedEntryPlan(square = "c5")
          )
      )

    assert(!cert.certified, clue(cert))
    assert(cert.failsIf.contains("axis_independence_not_proven"), clue(cert))
  }

  test("dual_axis_burden_missing fails when only the primary axis carries measurable restriction") {
    val cert =
      certification(
        plan = evaluatedRestrictionPlan(List("probe_reply", "probe_validation")),
        probes = List(directReplyProbe("probe_reply"), validationProbe("probe_validation")),
        preventedPlans =
          List(
            preventedBreakPlan(),
            preventedEntryPlan(
              counterplayScoreDrop = 32,
              breakNeutralizationStrength = Some(38),
              defensiveSufficiency = Some(34)
            )
          )
      )

    assert(!cert.certified, clue(cert))
    assert(cert.failsIf.contains("dual_axis_burden_missing"), clue(cert))
    assertEquals(cert.restrictionEvidence.primaryAxisMeasured, true, clue(cert))
    assertEquals(cert.restrictionEvidence.corroboratingAxisMeasured, false, clue(cert))
    assertEquals(cert.restrictionEvidence.restrictionDeltaMeasured, false, clue(cert))
  }

  test("hidden_freeing_break fails when an extra live axis remains outside the chosen pair") {
    val cert =
      certification(
        plan = evaluatedRestrictionPlan(List("probe_reply", "probe_validation")),
        probes = List(directReplyProbe("probe_reply"), validationProbe("probe_validation")),
        preventedPlans =
          List(
            preventedBreakPlan(),
            preventedEntryPlan(),
            preventedBreakPlan(label = "...e5")
          )
      )

    assert(!cert.certified, clue(cert))
    assert(cert.failsIf.contains("hidden_freeing_break"), clue(cert))
    assert(cert.freeingResourcesRemaining.nonEmpty, clue(cert))
  }

  test("hidden_tactical_release fails when best defense revives a forcing resource") {
    val cert =
      certification(
        plan = evaluatedRestrictionPlan(List("probe_reply", "probe_validation")),
        probes =
          List(
            directReplyProbe(
              "probe_reply",
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
            validationProbe("probe_validation")
          )
      )

    assert(!cert.certified, clue(cert))
    assert(cert.failsIf.contains("hidden_tactical_release"), clue(cert))
    assert(cert.tacticalReleasesRemaining.nonEmpty, clue(cert))
  }

  test("move_order_fragile_bind fails when best defense collapses the shell") {
    val cert =
      certification(
        plan = evaluatedRestrictionPlan(List("probe_reply", "probe_validation")),
        probes =
          List(
            directReplyProbe("probe_reply", collapseReason = Some("wrong order reopens the queenside")),
            validationProbe("probe_validation")
          )
      )

    assert(!cert.certified, clue(cert))
    assert(cert.moveOrderFragility.fragile, clue(cert))
    assert(cert.failsIf.contains("move_order_fragility"), clue(cert))
  }

  test("pv_restatement_only_bind fails without explicit validation evidence") {
    val cert =
      certification(
        plan = evaluatedRestrictionPlan(List("probe_reply", "probe_theme")),
        probes =
          List(
            directReplyProbe("probe_reply"),
            validationProbe(
              "probe_theme",
              purpose = ThemePlanProbePurpose.ThemePlanValidation,
              futureSnapshot = None,
              keyMotifs = List("quiet line")
            )
          )
      )

    assert(!cert.certified, clue(cert))
    assert(cert.failsIf.contains("pv_restatement_only"), clue(cert))
  }

  test("direct_best_defense_missing fails when only validation probes carry the dual-axis shell") {
    val cert =
      certification(
        plan = evaluatedRestrictionPlan(List("probe_validation", "probe_convert")),
        probes =
          List(
            validationProbe("probe_validation"),
            validationProbe(
              "probe_convert",
              purpose = "convert_reply_multipv",
              keyMotifs =
                List(
                  "...c5 break denied",
                  "b4 entry denied",
                  "conversion route stabilizes"
                )
            )
          )
      )

    assert(!cert.certified, clue(cert))
    assert(cert.failsIf.contains("direct_best_defense_missing"), clue(cert))
    assertEquals(cert.routeContinuity.directBestDefensePresent, false, clue(cert))
  }

  test("waiting_move_only fails when the shell has no measurable opponent-facing restriction") {
    val cert =
      certification(
        plan = evaluatedRestrictionPlan(List("probe_reply", "probe_validation")),
        probes =
          List(
            directReplyProbe(
              "probe_reply",
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
              "probe_validation",
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
          )
      )

    assert(!cert.certified, clue(cert))
    assert(cert.failsIf.contains("waiting_move_disguised_as_bind"), clue(cert))
  }

  test("route_continuity_missing fails when the shell persists but no bounded follow-through is visible") {
    val cert =
      certification(
        plan = evaluatedRestrictionPlan(List("probe_reply", "probe_validation")),
        probes =
          List(
            directReplyProbe("probe_reply"),
            validationProbe(
              "probe_validation",
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
          )
      )

    assert(!cert.certified, clue(cert))
    assert(cert.failsIf.contains("route_continuity_missing"), clue(cert))
    assert(!cert.fortressRisk, clue(cert))
  }

  test("stitched_defended_branch fails when persistence is borrowed from a different defensive branch") {
    val cert =
      certification(
        plan = evaluatedRestrictionPlan(List("probe_reply", "probe_validation")),
        probes =
          List(
            directReplyProbe("probe_reply"),
            validationProbe(
              "probe_validation",
              bestReplyPv = List("a7a5", "g2g4"),
              replyPvs =
                Some(
                  List(
                    List("a7a5", "g2g4"),
                    List("f8e8", "c1c8")
                  )
                )
            )
          )
      )

    assert(!cert.certified, clue(cert))
    assert(cert.failsIf.contains("stitched_defended_branch"), clue(cert))
    assertEquals(cert.bestDefenseBranchKey, Some("f8e8"), clue(cert))
    assertEquals(cert.routeContinuity.sameDefendedBranch, false, clue(cert))
  }

  test("fortress_like_static_hold fails when the bind is static but not progress-bearing") {
    val cert =
      certification(
        plan = evaluatedRestrictionPlan(List("probe_reply", "probe_validation")),
        probes =
          List(
            directReplyProbe("probe_reply"),
            validationProbe(
              "probe_validation",
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
          )
      )

    assert(!cert.certified, clue(cert))
    assert(cert.failsIf.contains("route_continuity_missing"), clue(cert))
    assert(cert.failsIf.contains("fortress_like_but_not_winning"), clue(cert))
    assert(cert.fortressRisk, clue(cert))
  }

  test("local_overreach_shell fails outside the narrow late-middlegame clearly-better slice") {
    val cert =
      certification(
        plan = evaluatedRestrictionPlan(List("probe_reply", "probe_validation")),
        probes = List(directReplyProbe("probe_reply"), validationProbe("probe_validation")),
        evalCp = 115,
        fen = HeavyPieceMiddlegameFen,
        ply = 18
      )

    assert(!cert.certified, clue(cert))
    assert(cert.failsIf.contains("local_to_global_overreach"), clue(cert))
  }

  test("surface_reinflation_case fails when the hypothesis overclaims whole-position domination") {
    val cert =
      certification(
        plan =
          evaluatedRestrictionPlan(
            List("probe_reply", "probe_validation"),
            hypothesis =
              bindHypothesis(
                id = "surface_reinflation_case",
                name = "Leave Black with no counterplay in the whole position forever",
                executionSteps =
                  List("Stop the ...c5 break, keep b4 closed, and leave Black with no counterplay.")
              )
          ),
        probes = List(directReplyProbe("probe_reply"), validationProbe("probe_validation"))
      )

    assert(!cert.certified, clue(cert))
    assert(cert.failsIf.contains("surface_reinflation"), clue(cert))
    assertEquals(cert.counterplayReinflationRisk, "high")
  }
