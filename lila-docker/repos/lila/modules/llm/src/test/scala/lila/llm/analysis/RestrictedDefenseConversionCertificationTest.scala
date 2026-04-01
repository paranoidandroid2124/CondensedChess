package lila.llm.analysis

import chess.Square
import munit.FunSuite
import lila.llm.model.*
import lila.llm.model.authoring.*
import lila.llm.model.strategic.PreventedPlan

class RestrictedDefenseConversionCertificationTest extends FunSuite:

  private def conversionHypothesis(
      id: String = "simplification_conversion",
      name: String = "Simplify into a winning pawn ending",
      score: Double = 0.82
  ): PlanHypothesis =
    PlanHypothesis(
      planId = id,
      planName = name,
      rank = 1,
      score = score,
      preconditions = Nil,
      executionSteps = List("Trade the last active defender and keep the passer rolling."),
      failureModes = List("Wrong move order allows counterplay."),
      viability = PlanViability(score = score, label = "high", risk = "conversion test"),
      refutation = None,
      evidenceSources = List("theme:advantage_transformation"),
      themeL1 = ThemeTaxonomy.ThemeL1.AdvantageTransformation.id,
      subplanId = Some(ThemeTaxonomy.SubplanId.SimplificationConversion.id)
    )

  private def evaluatedConversionPlan(
      supportProbeIds: List[String],
      claimCertification: PlanEvidenceEvaluator.ClaimCertification =
        PlanEvidenceEvaluator.ClaimCertification(
          certificateStatus = PlayerFacingCertificateStatus.Valid,
          quantifier = PlayerFacingClaimQuantifier.Universal,
          modalityTier = PlayerFacingClaimModalityTier.Advances,
          attributionGrade = PlayerFacingClaimAttributionGrade.Distinctive,
          stabilityGrade = PlayerFacingClaimStabilityGrade.Stable,
          provenanceClass = PlayerFacingClaimProvenanceClass.ProbeBacked,
          ontologyFamily = PlayerFacingClaimOntologyFamily.PlanAdvance
        ),
      status: PlanEvidenceEvaluator.PlanEvidenceStatus =
        PlanEvidenceEvaluator.PlanEvidenceStatus.PlayableEvidenceBacked,
      userFacingEligibility: PlanEvidenceEvaluator.UserFacingPlanEligibility =
        PlanEvidenceEvaluator.UserFacingPlanEligibility.ProbeBacked,
      missingSignals: List[String] = Nil,
      pvCoupled: Boolean = false
  ): PlanEvidenceEvaluator.EvaluatedPlan =
    PlanEvidenceEvaluator.EvaluatedPlan(
      hypothesis = conversionHypothesis(),
      status = status,
      userFacingEligibility = userFacingEligibility,
      reason = "conversion evidence test",
      supportProbeIds = supportProbeIds,
      refuteProbeIds = Nil,
      missingSignals = missingSignals,
      pvCoupled = pvCoupled,
      themeL1 = ThemeTaxonomy.ThemeL1.AdvantageTransformation.id,
      subplanId = Some(ThemeTaxonomy.SubplanId.SimplificationConversion.id),
      claimCertification = claimCertification
    )

  private def replyProbe(
      id: String,
      purpose: String = "convert_reply_multipv",
      bestReplyPv: List[String] = List("g7g6", "g3g7"),
      replyPvs: Option[List[List[String]]] = Some(
        List(
          List("g7g6", "g3g7"),
          List("c7c6", "g3g7")
        )
      ),
      collapseReason: Option[String] = None,
      futureSnapshot: Option[FutureSnapshot] =
        Some(
          FutureSnapshot(
            resolvedThreatKinds = List("Counterplay"),
            newThreatKinds = Nil,
            targetsDelta = TargetsDelta(Nil, Nil, List("g7"), Nil),
            planBlockersRemoved = List("defender_trade_complete"),
            planPrereqsMet = List("pawn_ending_ready")
          )
        )
  ): ProbeResult =
    ProbeResult(
      id = id,
      evalCp = 260,
      bestReplyPv = bestReplyPv,
      replyPvs = replyPvs,
      deltaVsBaseline = 18,
      keyMotifs = List("conversion_window", "defender_trade"),
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
      sourceScope = FactScope.Now
    )

  private def certification(
      plan: PlanEvidenceEvaluator.EvaluatedPlan,
      probes: List[ProbeResult],
      preventedPlans: List[PreventedPlan] = List(preventedPlan()),
      evalCp: Int = 260,
      isWhiteToMove: Boolean = true
  ): RestrictedDefenseConversionCertification.Contract =
    RestrictedDefenseConversionCertification
      .evaluate(
        plan = plan,
        probeResultsById = probes.map(result => result.id -> result).toMap,
        preventedPlans = preventedPlans,
        evalCp = evalCp,
        isWhiteToMove = isWhiteToMove
      )
      .getOrElse(fail("expected restricted-defense certification contract"))

  test("true_restricted_defense_conversion passes certification") {
    val cert =
      certification(
        plan = evaluatedConversionPlan(List("probe_true")),
        probes = List(replyProbe(id = "probe_true"))
      )

    assert(cert.certified, clue(cert))
    assertEquals(cert.bestDefenseFound, Some("g7g6"))
    assertEquals(cert.defenderResources, List("g7g6", "c7c6"))
    assertEquals(cert.restrictedDefenseEvidence.defenderResourceCount, 2)
    assertEquals(cert.restrictedDefenseEvidence.counterplayScoreDrop, 120)
    assert(cert.routePersistence.bestDefenseStable, clue(cert))
    assert(cert.routePersistence.futureSnapshotPersistent, clue(cert))
    assert(!cert.moveOrderFragility.fragile, clue(cert))
  }

  test("cooperative_plan_fake fails certification when best defense is not actually identified") {
    val cert =
      certification(
        plan = evaluatedConversionPlan(List("probe_cooperative")),
        probes =
          List(
            replyProbe(
              id = "probe_cooperative",
              bestReplyPv = Nil,
              replyPvs = Some(List(List("g7g6", "g3g7"))),
              futureSnapshot = None
            )
          ),
        preventedPlans = Nil
      )

    assert(!cert.certified, clue(cert))
    assert(cert.failsIf.contains("cooperative_defense"), clue(cert))
    assert(cert.failsIf.contains("pv_restatement_only"), clue(cert))
    assert(cert.failsIf.contains("route_persistence_missing"), clue(cert))
  }

  test("hidden_defense_resource fails certification when defender resources stay too broad") {
    val cert =
      certification(
        plan = evaluatedConversionPlan(List("probe_hidden")),
        probes =
          List(
            replyProbe(
              id = "probe_hidden",
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
        preventedPlans = Nil
      )

    assert(!cert.certified, clue(cert))
    assertEquals(cert.restrictedDefenseEvidence.defenderResourceCount, 3, clue(cert))
    assert(cert.failsIf.contains("hidden_defensive_resource"), clue(cert))
    assert(cert.failsIf.contains("insufficient_counterplay_suppression"), clue(cert))
  }

  test("move_order_fragile_conversion fails certification when best defense collapses the route") {
    val cert =
      certification(
        plan = evaluatedConversionPlan(List("probe_fragile")),
        probes =
          List(
            replyProbe(
              id = "probe_fragile",
              collapseReason = Some("wrong order leaks the back-rank defense")
            )
          ),
        preventedPlans = List(preventedPlan(counterplayScoreDrop = 90))
      )

    assert(!cert.certified, clue(cert))
    assert(cert.moveOrderFragility.fragile, clue(cert))
    assert(cert.moveOrderFragility.reasons.contains("collapse_under_best_defense"), clue(cert))
    assert(cert.failsIf.contains("move_order_fragility"), clue(cert))
  }

  test("pv_restatement_only fails certification without direct reply-multipv evidence") {
    val cert =
      certification(
        plan = evaluatedConversionPlan(List("probe_theme_only")),
        probes =
          List(
            replyProbe(
              id = "probe_theme_only",
              purpose = "theme_plan_validation"
            )
          )
      )

    assert(!cert.certified, clue(cert))
    assert(cert.failsIf.contains("pv_restatement_only"), clue(cert))
  }

  test("local_truth_overreach fails certification when the edge is not conversion ready") {
    val cert =
      certification(
        plan = evaluatedConversionPlan(List("probe_small_edge")),
        probes = List(replyProbe(id = "probe_small_edge")),
        evalCp = 70
      )

    assert(!cert.certified, clue(cert))
    assert(cert.failsIf.contains("local_to_global_overreach"), clue(cert))
  }
