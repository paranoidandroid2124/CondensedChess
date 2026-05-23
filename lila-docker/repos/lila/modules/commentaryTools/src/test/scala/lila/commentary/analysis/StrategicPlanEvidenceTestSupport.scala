package lila.commentary.analysis

import lila.commentary.*
import lila.commentary.model.NarrativeContext
import lila.commentary.model.StrategicPlanExperiment
import lila.commentary.model.authoring.PlanHypothesis

private[commentary] object StrategicPlanEvidenceTestSupport:

  def probeBacked(plans: List[PlanHypothesis]): PlanEvidenceEvaluator.StrategicPlanEvidenceView =
    val evaluated = plans.map(probeBackedPlan)
    PlanEvidenceEvaluator.StrategicPlanEvidenceView(
      selectedPlans = evaluated,
      evaluatedPlans = evaluated
    )

  def withProbeBackedPlanEvidence(ctx: NarrativeContext): NarrativeContext =
    ctx.copy(strategicPlanEvidence = probeBacked(ctx.mainStrategicPlans))

  def fromExperiments(
      plans: List[PlanHypothesis],
      experiments: List[StrategicPlanExperiment]
  ): PlanEvidenceEvaluator.StrategicPlanEvidenceView =
    val backedKeys =
      experiments
        .filter(exp => Option(exp.evidenceTier).exists(_.trim.equalsIgnoreCase("evidence_backed")))
        .map(exp => planKey(exp.planId, exp.subplanId))
        .toSet
    probeBacked(plans.filter(plan => backedKeys.contains(planKey(plan.planId, plan.subplanId))))

  private def probeBackedPlan(plan: PlanHypothesis): PlanEvidenceEvaluator.EvaluatedPlan =
    PlanEvidenceEvaluator.EvaluatedPlan(
      hypothesis = plan,
      status = PlanEvidenceEvaluator.PlanEvidenceStatus.PlayableEvidenceBacked,
      userFacingEligibility = PlanEvidenceEvaluator.UserFacingPlanEligibility.ProbeBacked,
      reason = "test fixture probe-backed plan",
      supportProbeIds = List("test_probe"),
      themeL1 = Option(plan.themeL1).map(_.trim).filter(_.nonEmpty).getOrElse(PlanTaxonomy.PlanTheme.Unknown.id),
      subplanId = plan.subplanId,
      claimCertification =
        PlanEvidenceEvaluator.ClaimCertification(
          certificateStatus = PlayerFacingCertificateStatus.Valid,
          quantifier = PlayerFacingClaimQuantifier.BestResponse,
          modalityTier = PlayerFacingClaimModalityTier.Available,
          attributionGrade = PlayerFacingClaimAttributionGrade.Distinctive,
          stabilityGrade = PlayerFacingClaimStabilityGrade.Stable,
          provenanceClass = PlayerFacingClaimProvenanceClass.ProbeBacked
        )
    )

  private def planKey(planId: String, subplanId: Option[String]): String =
    s"${Option(planId).map(_.trim.toLowerCase).getOrElse("")}|${subplanId.map(_.trim.toLowerCase).getOrElse("")}"
