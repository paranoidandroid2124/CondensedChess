package lila.llm.analysis

import lila.llm.model.{ PlanMatch, PlanScoringResult }
import lila.llm.model.authoring.{ PlanHypothesis, PlanViability }
import lila.llm.analysis.prior.{ StrategicPrior, StrategicPriorFeatures }

/**
 * Plan Proposal Layer v2:
 * Convert plan matches into structured hypotheses and rank them with
 * structural fit + execution difficulty + counterplay risk.
 */
object HypothesisGenerator:

  case class RankedHypothesis(
      hypothesis: PlanHypothesis,
      structuralFit: Double,
      executionDifficulty: Double,
      counterplayRisk: Double
  )

  def generate(
      planScoring: PlanScoringResult,
      ctx: IntegratedContext,
      maxItems: Int = 3
  ): List[PlanHypothesis] =
    val raw = planScoring.topPlans.take(maxItems).map(pm => scoreHypothesis(pm, ctx))
    raw.sortBy(r => -r.hypothesis.score).zipWithIndex.map { case (rh, idx) =>
      rh.hypothesis.copy(rank = idx + 1)
    }

  def generateWithPrior(
      planScoring: PlanScoringResult,
      ctx: IntegratedContext,
      maxItems: Int = 3,
      priorWeight: Double = 0.35,
      usePrior: Boolean = true
  ): List[PlanHypothesis] =
    val base = generate(planScoring, ctx, maxItems = maxItems)
    if !usePrior then base
    else
      val priorMap = buildPriorMap(planScoring.topPlans.take(maxItems), ctx)
      blendWithPrior(base, priorMap, priorWeight = priorWeight)

  def blendWithPrior(
      hypotheses: List[PlanHypothesis],
      prior: Map[String, Double],
      priorWeight: Double = 0.35
  ): List[PlanHypothesis] =
    if hypotheses.isEmpty then Nil
    else
      val w = priorWeight.max(0.0).min(0.8)
      hypotheses
        .map { h =>
          val p = prior.getOrElse(h.planId, 0.5).max(0.0).min(1.0)
          val blended = ((1.0 - w) * h.score) + (w * p)
          h.copy(score = blended)
        }
        .sortBy(h => -h.score)
        .zipWithIndex
        .map { case (h, idx) => h.copy(rank = idx + 1) }

  private def scoreHypothesis(pm: PlanMatch, ctx: IntegratedContext): RankedHypothesis =
    val structuralFit = structuralFitScore(pm, ctx)
    val executionDifficulty = executionDifficultyScore(pm, ctx)
    val counterplayRisk = counterplayRiskScore(pm, ctx)
    val basePlanScore = pm.score.max(0.0).min(1.5) / 1.5
    val viabilityScore =
      ((basePlanScore * 0.45) + (structuralFit * 0.35) + ((1.0 - counterplayRisk) * 0.20))
        .max(0.0)
        .min(1.0)
    val finalScore =
      (viabilityScore * 0.7 + (1.0 - executionDifficulty) * 0.3)
        .max(0.0)
        .min(1.0)
    val viability = PlanViability(
      score = viabilityScore,
      label = viabilityLabel(viabilityScore),
      risk = riskSummary(counterplayRisk, executionDifficulty)
    )
    val preconditions =
      (pm.supports.take(3) ++ pm.missingPrereqs.take(1).map(mp => s"needs $mp")).distinct
    val executionSteps =
      pm.evidence.take(3).map(_.description).filter(_.nonEmpty) match
        case Nil => List("improve piece coordination before committing")
        case xs  => xs
    val failureModes =
      (pm.blockers.take(2) ++ pm.missingPrereqs.take(2).map(mp => s"precondition miss: $mp")).distinct

    val hypothesis = PlanHypothesis(
      planId = pm.plan.id.toString,
      planName = pm.plan.name,
      rank = 0,
      score = finalScore,
      preconditions = preconditions,
      executionSteps = executionSteps,
      failureModes = failureModes,
      viability = viability,
      refutation = failureModes.headOption,
      evidenceSources = pm.evidence.take(2).map(_.description)
    )
    RankedHypothesis(
      hypothesis = hypothesis,
      structuralFit = structuralFit,
      executionDifficulty = executionDifficulty,
      counterplayRisk = counterplayRisk
    )

  private def buildPriorMap(plans: List[PlanMatch], ctx: IntegratedContext): Map[String, Double] =
    plans.map { pm =>
      val features = priorFeatures(pm, ctx)
      pm.plan.id.toString -> StrategicPrior.score(pm.plan.id.toString, features)
    }.toMap

  private def priorFeatures(pm: PlanMatch, ctx: IntegratedContext): Map[String, Double] =
    val executionEase = 1.0 - executionDifficultyScore(pm, ctx)
    StrategicPriorFeatures.fromContext(pm.plan.color, ctx, executionEase)

  private def structuralFitScore(pm: PlanMatch, ctx: IntegratedContext): Double =
    val supports = pm.supports.size.min(4).toDouble / 4.0
    val blockersPenalty = pm.blockers.size.min(3).toDouble * 0.08
    val missingPenalty = pm.missingPrereqs.size.min(3).toDouble * 0.10
    val planAlignmentBoost =
      ctx.planAlignment.map { pa =>
        if pa.band.toString == "OnBook" then 0.12
        else if pa.band.toString == "Playable" then 0.05
        else 0.0
      }.getOrElse(0.0)
    (0.45 + supports + planAlignmentBoost - blockersPenalty - missingPenalty)
      .max(0.0)
      .min(1.0)

  private def executionDifficultyScore(pm: PlanMatch, ctx: IntegratedContext): Double =
    val blockers = pm.blockers.size.min(4).toDouble / 4.0
    val missing = pm.missingPrereqs.size.min(4).toDouble / 4.0
    val tacticalPressure = if ctx.tacticalThreatToUs then 0.25 else 0.0
    (0.2 + blockers * 0.45 + missing * 0.35 + tacticalPressure).max(0.0).min(1.0)

  private def counterplayRiskScore(pm: PlanMatch, ctx: IntegratedContext): Double =
    val direct =
      if ctx.tacticalThreatToThem then 0.25
      else if ctx.strategicThreatToThem then 0.15
      else 0.05
    val defensiveDebt =
      if ctx.tacticalThreatToUs then 0.35
      else if ctx.strategicThreatToUs then 0.2
      else 0.0
    val fragilePlan = if pm.blockers.nonEmpty then 0.12 else 0.0
    (direct + defensiveDebt + fragilePlan).max(0.0).min(1.0)

  private def viabilityLabel(score: Double): String =
    if score >= 0.7 then "high"
    else if score >= 0.45 then "medium"
    else "low"

  private def riskSummary(counterplayRisk: Double, executionDifficulty: Double): String =
    if counterplayRisk >= 0.65 then "high counterplay risk"
    else if executionDifficulty >= 0.65 then "high execution burden"
    else if counterplayRisk >= 0.45 || executionDifficulty >= 0.45 then "manageable but sensitive"
    else "stable practical path"
