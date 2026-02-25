package lila.llm.model.authoring

import play.api.libs.json.*

/**
 * PlanHypothesis is a first-class strategic plan proposal.
 * It captures not only a label, but a structured execution model.
 */
case class PlanHypothesis(
    planId: String,
    planName: String,
    rank: Int,
    score: Double,
    preconditions: List[String],
    executionSteps: List[String],
    failureModes: List[String],
    viability: PlanViability,
    refutation: Option[String] = None,
    evidenceSources: List[String] = Nil
)

case class PlanViability(
    score: Double, // 0.0-1.0
    label: String, // "high" | "medium" | "low"
    risk: String   // concise risk summary
)

case class LatentPlanNarrative(
    seedId: String,
    planName: String,
    viabilityScore: Double,
    whyAbsentFromTopMultiPv: String
)

object PlanHypothesis:
  given Writes[PlanHypothesis] = Json.writes[PlanHypothesis]

object PlanViability:
  given Writes[PlanViability] = Json.writes[PlanViability]

object LatentPlanNarrative:
  given Writes[LatentPlanNarrative] = Json.writes[LatentPlanNarrative]
