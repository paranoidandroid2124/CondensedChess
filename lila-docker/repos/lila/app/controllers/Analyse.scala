package controllers

import play.api.libs.json.{ JsArray, JsError, JsObject, JsValue, Json }

import lila.app.*
import lila.chessjudgment.analysis.assembly.{
  JudgmentPacketValidationIssue,
  MoveReviewJudgmentOrchestrator,
  RawMoveReviewInput
}
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
    bodyCtx.body.body.validate[RawMoveReviewInput].fold(
      errors => BadRequest(Json.obj("ok" -> false, "error" -> "invalid_move_review_input", "details" -> JsError.toJson(errors))).toFuccess,
      raw =>
        MoveReviewJudgmentOrchestrator.build(raw).fold(
          BadRequest(Json.obj("ok" -> false, "error" -> "move_review_not_buildable")).toFuccess
        ): result =>
          JsonOk(
            Json.obj(
              "ok" -> true,
              "valid" -> result.validation.isValid,
              "qualityClean" -> result.quality.audit.isClean,
              "moveJudgmentView" -> result.packet.moveJudgmentView.map(moveJudgmentViewMeaningJson),
              "validationIssues" -> result.validation.issues.map(validationIssueJson)
            )
          ).toFuccess
    )

  private def moveJudgmentViewMeaningJson(view: MoveJudgmentView): JsObject =
    Json.obj(
      "verdict" -> view.verdict.map(verdictJson),
      "moveMeaningHighlights" -> view.moveMeaningHighlights.map(moveMeaningHighlightJson),
      "positionPlanTechniqueFrameCount" -> view.positionPlanTechniqueFrames.size,
      "primaryCauseCount" -> view.primaryCauses.size,
      "secondaryCauseCount" -> view.secondaryCauses.size,
      "contextCauseCount" -> view.contextCauses.size
    )

  private def verdictJson(frame: MoveJudgmentVerdictFrame): JsObject =
    Json.obj(
      "verdict" -> frame.verdict.toString,
      "comparisonKind" -> frame.comparisonKind.toString,
      "referenceLine" -> lineRefJson(frame.referenceLine),
      "candidateLine" -> lineRefJson(frame.candidateLine),
      "winPercentLossForMover" -> frame.winPercentLossForMover,
      "candidateWinPercentDeltaForMover" -> frame.candidateWinPercentDeltaForMover
    )

  private def lineRefJson(ref: LineNodeRef): JsObject =
    Json.obj(
      "id" -> ref.id,
      "rootMove" -> ref.rootMove,
      "rank" -> ref.rank,
      "role" -> ref.role.toString
    )

  private def moveMeaningHighlightJson(highlight: MoveMeaningHighlight): JsObject =
    Json.obj(
      "meaningKind" -> highlight.meaningKind,
      "stance" -> highlight.stance,
      "strength" -> highlight.strength,
      "wordingPolicy" -> highlight.wordingPolicy,
      "lineRole" -> highlight.lineRole,
      "moveUci" -> highlight.moveUci,
      "frameId" -> highlight.frameId,
      "unit" -> highlight.unit.toString,
      "axisKey" -> highlight.axisKey,
      "axisKind" -> highlight.axisKind.map(_.toString),
      "axisPolarity" -> highlight.axisPolarity.map(_.toString),
      "label" -> highlight.label,
      "causeKinds" -> highlight.causeKinds.map(_.toString),
      "causeSourceSides" -> highlight.causeSourceSides.map(_.toString),
      "causeEvidenceIds" -> highlight.causeEvidenceIds,
      "sourceEvidenceIds" -> highlight.sourceEvidenceIds,
      "reasonTokens" -> highlight.reasonTokens,
      "objectBindingSignatureCount" -> highlight.objectBindingSignatures.size,
      "objectBindingSignaturesSample" -> highlight.objectBindingSignatures.take(5)
    )

  private def validationIssueJson(issue: JudgmentPacketValidationIssue): JsObject =
    Json.obj(
      "kind" -> issue.kind.toString,
      "subjectId" -> issue.subjectId,
      "evidenceId" -> issue.evidence.map(_.id)
    )
