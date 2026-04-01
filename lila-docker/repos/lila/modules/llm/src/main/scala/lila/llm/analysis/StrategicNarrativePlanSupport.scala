package lila.llm.analysis

import lila.llm.model.NarrativeContext
import lila.llm.model.authoring.PlanHypothesis

private[llm] object StrategicNarrativePlanSupport:

  def evidenceBackedMainPlans(ctx: NarrativeContext): List[PlanHypothesis] =
    filterEvidenceBacked(ctx.mainStrategicPlans, ctx.strategicPlanExperiments)

  def filterEvidenceBacked(
      plans: List[PlanHypothesis],
      experiments: List[lila.llm.model.StrategicPlanExperiment]
  ): List[PlanHypothesis] =
    if plans.isEmpty then Nil
    else if experiments.isEmpty then plans
    else
      val evidenceBackedKeys = experimentEvidenceBackedKeys(experiments)
      plans.filter(plan => evidenceBackedKeys.contains(planKey(plan.planId, plan.subplanId)))

  def experimentEvidenceBackedKeys(
      experiments: List[lila.llm.model.StrategicPlanExperiment]
  ): Set[String] =
    experiments.collect {
      case experiment if normalized(experiment.evidenceTier).contains("evidence_backed") ||
          normalized(experiment.evidenceTier).contains("evidence backed") =>
        planKey(experiment.planId, experiment.subplanId)
    }.toSet

  def evidenceBackedPlanNames(ctx: NarrativeContext): List[String] =
    evidenceBackedMainPlans(ctx).flatMap(plan => displayText(plan.planName))

  def evidenceBackedLeadingPlanName(ctx: NarrativeContext): Option[String] =
    evidenceBackedPlanNames(ctx).headOption

  private def planKey(planId: String, subplanId: Option[String]): String =
    s"${normalized(planId).getOrElse("")}|${subplanId.flatMap(normalized).getOrElse("")}"

  private def normalized(raw: String): Option[String] =
    Option(raw).map(_.trim).filter(_.nonEmpty).map(_.toLowerCase)

  private def displayText(raw: String): Option[String] =
    Option(raw).map(_.trim).filter(_.nonEmpty)
