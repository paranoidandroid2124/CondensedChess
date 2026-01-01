package controllers

import play.api.mvc._
import play.api.libs.json._
import lila.api.Context
import lila.app._
import lila.llm.{ LlmClient, FullAnalysisRequest, AnalysisOptions, MoveEval, AnalysisVariation }
import lila.llm.model.strategic.VariationLine
import scala.concurrent.Future

final class LlmController(
    api: lila.llm.LlmApi,
    env: Env
) extends LilaController(env):

  def analyzeGameLocal = Action.async(parse.json) { implicit req =>
    req.body.validate[FullAnalysisRequest].fold(
      errors => Future.successful(BadRequest(JsError.toJson(errors))),
      analysisReq => {
        api.analyzeGameLocal(analysisReq).map {
          case Some(response) => Ok(Json.toJson(response))
          case None => ServiceUnavailable("LLM Analysis unavailable")
        }
      }
    )
  }

  def analyzeGame = Action.async(parse.json) { implicit req =>
    // Map raw JSON to FullAnalysisRequest
    // The Frontend sends: { pgn: "...", evals: [...], options: {...} }
    // We rely on the implicit Reads in LlmClient or define them here if they differ.
    // LlmClient has `given Reads[FullAnalysisRequest]`.
    
    req.body.validate[FullAnalysisRequest].fold(
      errors => Future.successful(BadRequest(JsError.toJson(errors))),
      analysisReq => {
        api.analyzeGame(analysisReq).map {
          case Some(response) => Ok(Json.toJson(response))
          case None => ServiceUnavailable("LLM Analysis unavailable or disabled")
        }
      }
    )
  }

  def analyzePosition = Action.async(parse.json) { implicit req =>
    import lila.llm.CommentRequest
    req.body.validate[CommentRequest].fold(
      errors => Future.successful(BadRequest(JsError.toJson(errors))),
      commentReq => {
        api.commentPosition(commentReq).map {
          case Some(response) => Ok(Json.toJson(response))
          case None => ServiceUnavailable("LLM Commentary unavailable")
        }
      }
    )
  }
