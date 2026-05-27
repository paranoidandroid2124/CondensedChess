package lila.commentary

import play.api.libs.json.*

object MoveReviewResponsePayload:

  def json(
      response: CommentResponse,
      html: String,
      cacheHit: Boolean
  ): JsObject =
    val baseJson = Json.obj(
      "schema" -> "chesstory.move_review.v2",
      "html" -> html,
      "commentary" -> response.commentary,
      "variations" -> response.variations,
      "probeRequests" -> Json.arr(),
      "mainStrategicPlanCount" -> response.mainStrategicPlans.size,
      "planStateToken" -> response.planStateToken,
      "endgameStateToken" -> response.endgameStateToken,
      "sourceMode" -> response.sourceMode,
      "model" -> response.model,
      "moveReviewPlayerSurface" -> response.moveReviewPlayerSurface,
      "diagnostics" -> MoveReviewResponseDiagnostics.json(response),
      "cacheHit" -> cacheHit
    )
    val withRefs = response.refs.fold(baseJson)(r => baseJson ++ Json.obj("refs" -> r))
    response.polishMeta.fold(withRefs)(m => withRefs ++ Json.obj("polishMeta" -> publicPolishMetaJson(m)))

  private def publicPolishMetaJson(meta: MoveReviewPolishMeta): JsObject =
    Json.obj(
      "provider" -> meta.provider,
      "model" -> meta.model,
      "sourceMode" -> meta.sourceMode,
      "validationPhase" -> meta.validationPhase,
      "cacheHit" -> meta.cacheHit
    )
