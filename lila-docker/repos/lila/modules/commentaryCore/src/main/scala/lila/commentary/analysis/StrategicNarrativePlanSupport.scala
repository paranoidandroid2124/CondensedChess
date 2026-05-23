package lila.commentary.analysis

import lila.commentary.model.NarrativeContext
import lila.commentary.model.authoring.PlanHypothesis

private[commentary] object StrategicNarrativePlanSupport:

  def evidenceBackedMainPlans(ctx: NarrativeContext): List[PlanHypothesis] =
    ctx.strategicPlanEvidence.probeBackedMainPlans

  def legacyFilterEvidenceBacked(
      plans: List[PlanHypothesis],
      experiments: List[lila.commentary.model.StrategicPlanExperiment]
  ): List[PlanHypothesis] =
    if plans.isEmpty then Nil
    else if experiments.isEmpty then plans
    else
      val evidenceBackedKeys = experimentEvidenceBackedKeys(experiments)
      plans.filter(plan => evidenceBackedKeys.contains(planKey(plan.planId, plan.subplanId)))

  def experimentEvidenceBackedKeys(
      experiments: List[lila.commentary.model.StrategicPlanExperiment]
  ): Set[String] =
    experiments.collect {
      case experiment if normalized(experiment.evidenceTier).contains("evidence_backed") ||
          normalized(experiment.evidenceTier).contains("evidence backed") =>
        planKey(experiment.planId, experiment.subplanId)
    }.toSet

  def evidenceBackedPlanNames(ctx: NarrativeContext): List[String] =
    ctx.strategicPlanEvidence.probeBackedPlanNames

  def evidenceBackedLeadingPlanName(ctx: NarrativeContext): Option[String] =
    ctx.strategicPlanEvidence.leadingProbeBackedPlanName

  private def planKey(planId: String, subplanId: Option[String]): String =
    s"${normalized(planId).getOrElse("")}|${subplanId.flatMap(normalized).getOrElse("")}"

  private def normalized(raw: String): Option[String] =
    Option(raw).map(_.trim).filter(_.nonEmpty).map(_.toLowerCase)
