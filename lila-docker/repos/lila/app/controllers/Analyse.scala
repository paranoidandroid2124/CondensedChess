package controllers

import play.api.libs.json.{ JsArray, JsObject, JsValue, Json }

import lila.app.*
import lila.chessjudgment.analysis.assembly.{ MoveReviewJudgmentOrchestrator, RawMoveReviewInput }
import lila.chessjudgment.model.judgment.*

final class Analyse(
    env: Env
) extends LilaController(env):

  def externalEngineList = Auth { _ ?=> me ?=>
    env.analyse.externalEngine.list(me).map { list =>
      JsonOk(JsArray(list.map(lila.analyse.ExternalEngine.jsonWrites.writes)))
    }
  }

  def externalEngineShow(id: String) = Auth { _ ?=> me ?=>
    Found(env.analyse.externalEngine.find(me, id)): engine =>
      JsonOk(lila.analyse.ExternalEngine.jsonWrites.writes(engine))
  }

  def externalEngineCreate = AuthBody { _ ?=> me ?=>
    bindForm(lila.analyse.ExternalEngine.form)(
      jsonFormError,
      data =>
        env.analyse.externalEngine.create(me, data).map { engine =>
          Created(lila.analyse.ExternalEngine.jsonWrites.writes(engine))
        }
    )
  }

  def externalEngineUpdate(id: String) = AuthBody { _ ?=> me ?=>
    Found(env.analyse.externalEngine.find(me, id)): engine =>
      bindForm(lila.analyse.ExternalEngine.form)(
        jsonFormError,
        data =>
          env.analyse.externalEngine.update(engine, data).map { engine =>
            JsonOk(lila.analyse.ExternalEngine.jsonWrites.writes(engine))
          }
      )
  }

  def externalEngineDelete(id: String) = Auth { _ ?=> me ?=>
    env.analyse.externalEngine.delete(me, id).elseNotFound(jsonOkResult)
  }

  def moveMeaning = OpenBodyOf(parse.json): (bodyCtx: BodyContext[JsValue]) ?=>
    limit.moveMeaning(bodyCtx.ip, rateLimitedJson.toFuccess):
      bodyCtx.body.body.validate[RawMoveReviewInput].fold(
        _ => BadRequest(Json.obj("ok" -> false, "error" -> "invalid_move_review_input")).toFuccess,
        raw =>
          MoveReviewJudgmentOrchestrator.build(raw).fold(
            BadRequest(Json.obj("ok" -> false, "error" -> "move_review_not_buildable")).toFuccess
          ): result =>
            JsonOk(
              Json.obj(
                "ok" -> true,
                "status" -> publicReviewStatus(result.validation.isValid, result.quality.audit.isClean),
                "move_review" -> result.packet.moveJudgmentView.map(moveJudgmentViewMeaningJson)
              )
            ).toFuccess
      )

  private def moveJudgmentViewMeaningJson(view: MoveJudgmentView): JsObject =
    Json.obj(
      "verdict" -> view.verdict.map(frame => publicVerdictJson(MoveMeaningSurface.verdict(frame))),
      "move_semantics" -> MoveMeaningSurface.from(view).take(12).map(publicMoveSemanticJson)
    )

  private def publicReviewStatus(valid: Boolean, qualityClean: Boolean): String =
    if valid && qualityClean then "ready" else "needs_review"

  private def publicVerdictJson(verdict: MoveMeaningSurfaceVerdict): JsObject =
    Json.obj(
      "verdict_code" -> verdict.verdictCode,
      "move_quality" -> verdict.moveQuality,
      "played_move" -> verdict.playedMove,
      "reference_move" -> verdict.referenceMove
    )

  private def publicMoveSemanticJson(surface: MoveMeaningSurface): JsObject =
    Json.obj(
      "move_uci" -> surface.moveUci,
      "subject" -> surface.subject,
      "move_quality" -> surface.moveQuality,
      "idea_type" -> surface.ideaType,
      "idea_quality" -> surface.ideaQuality,
      "failure_family" -> surface.failureFamily,
      "problem" -> surface.problem,
      "target" -> publicTargetJson(surface.target),
      "priority" -> surface.priority
    )

  private def publicTargetJson(target: MoveMeaningSurfaceTarget): JsObject =
    Json.obj(
      "squares" -> target.squares,
      "files" -> target.files,
      "pieces" -> target.pieces
    )
