package lila.llm.analysis.prior

import play.api.libs.json.*

/**
 * Lightweight local strategic prior for plan ranking.
 * Designed to be cheap (<1ms) and fail-safe with built-in defaults.
 */
object StrategicPrior:

  case class PlanWeights(
      intercept: Double = 0.0,
      featureWeights: Map[String, Double] = Map.empty
  )

  case class PriorModel(
      version: String,
      plans: Map[String, PlanWeights]
  )

  object PlanWeights:
    given Reads[PlanWeights] = Json.reads[PlanWeights]
    given Writes[PlanWeights] = Json.writes[PlanWeights]

  object PriorModel:
    given Reads[PriorModel] = Json.reads[PriorModel]
    given Writes[PriorModel] = Json.writes[PriorModel]

  private val defaultModel = PriorModel(
    version = "v1",
    plans = Map(
      "KingsideAttack" -> PlanWeights(
        intercept = -0.1,
        featureWeights = Map(
          "space_flank_kingside" -> 0.7,
          "king_exposure_them" -> 0.6,
          "tactical_threat_them" -> 0.4,
          "tactical_threat_us" -> -0.5
        )
      ),
      "MinorityAttack" -> PlanWeights(
        intercept = -0.05,
        featureWeights = Map(
          "space_flank_queenside" -> 0.6,
          "center_stable" -> 0.5,
          "counterplay_risk" -> -0.4
        )
      ),
      "PawnStorm" -> PlanWeights(
        intercept = -0.2,
        featureWeights = Map(
          "space_flank_kingside" -> 0.8,
          "center_stable" -> 0.55,
          "tactical_threat_us" -> -0.45
        )
      ),
      "CentralControl" -> PlanWeights(
        intercept = 0.05,
        featureWeights = Map(
          "center_stable" -> 0.45,
          "counterplay_risk" -> -0.2
        )
      ),
      "PieceActivation" -> PlanWeights(
        intercept = 0.1,
        featureWeights = Map(
          "execution_ease" -> 0.5,
          "counterplay_risk" -> -0.15
        )
      )
    )
  )

  def modelFromJson(raw: String): Option[PriorModel] =
    scala.util.Try(Json.parse(raw)).toOption.flatMap(_.validate[PriorModel].asOpt)

  def score(
      planId: String,
      features: Map[String, Double],
      model: PriorModel = defaultModel
  ): Double =
    val weights = model.plans.getOrElse(planId, PlanWeights())
    val linear =
      weights.intercept + features.map { case (k, v) =>
        weights.featureWeights.getOrElse(k, 0.0) * v
      }.sum
    sigmoid(linear)

  private def sigmoid(x: Double): Double =
    1.0 / (1.0 + Math.exp(-x.max(-10.0).min(10.0)))
