package lila.llm.analysis

import lila.llm.model.NarrativeContext
import lila.llm.model.authoring.PlanHypothesis

private[llm] object StrategicNarrativePlanSupport:

  def evidenceBackedMainPlans(ctx: NarrativeContext): List[PlanHypothesis] =
    if ctx.mainStrategicPlans.isEmpty then Nil
    else if ctx.strategicPlanExperiments.isEmpty then ctx.mainStrategicPlans
    else
      val evidenceBackedKeys =
        ctx.strategicPlanExperiments.collect {
          case experiment if experiment.evidenceTier == "evidence_backed" =>
            planKey(experiment.planId, experiment.subplanId)
        }.toSet
      ctx.mainStrategicPlans.filter(plan => evidenceBackedKeys.contains(planKey(plan.planId, plan.subplanId)))

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
