package lila.commentary.api

import play.api.libs.json.*

import lila.commentary.api.CommentaryApiJson.given
import lila.commentary.render.RenderStatus
import lila.commentary.selection.{ VariationSurfaceAllowance, WordingStrength }

final case class CommentaryLocalProbeRequest(
    request: CommentaryRequest,
    completedProbe: CommentaryCompletedProbePayload
)

object CommentaryLocalProbeJsonTransport:

  private val BadRequestJson = Json.obj("error" -> "invalid_commentary_local_probe_request")
  private val LocalProbeFields = Set("request", "completedProbe")
  private val LocalProbeRequestFields = Set("currentFen", "beforeFen", "playedMove", "nodeId", "ply")
  private val LocalProbeTransitionFields = Set("beforeFen", "playedMove")

  private given Format[CommentaryLocalProbeRequest] =
    Json.format[CommentaryLocalProbeRequest]

  def renderJson(body: JsValue): Either[JsObject, JsValue] =
    parseRequest(body).map: request =>
      val response = CommentaryBackendSeam.renderInternal(request.request, Some(request.completedProbe))
      Json.toJson(if hasPublicLocalProbeLine(response) then response else noCommentary(response))

  def parseRequest(body: JsValue): Either[JsObject, CommentaryLocalProbeRequest] =
    body match
      case obj: JsObject if obj.fields.map(_._1).toSet == LocalProbeFields && cleanNestedRequest(obj) =>
        obj.validate[CommentaryLocalProbeRequest].asEither.left.map(_ => BadRequestJson)
      case _ =>
        Left(BadRequestJson)

  private def cleanNestedRequest(obj: JsObject): Boolean =
    (obj \ "request").asOpt[JsObject].exists: request =>
      val fields = request.fields.map(_._1).toSet
      val hasTransition = LocalProbeTransitionFields.forall(fields.contains)
      val hasPartialTransition = LocalProbeTransitionFields.exists(fields.contains) && !hasTransition
      fields.subsetOf(LocalProbeRequestFields) &&
        Set("currentFen", "nodeId", "ply").subsetOf(fields) &&
        !hasPartialTransition &&
        LocalProbeTransitionFields.forall(field => !fields.contains(field) || (request \ field).toOption.exists(_ != JsNull))

  private def hasPublicLocalProbeLine(response: CommentaryResponse): Boolean =
    val lineProofIds =
      response.render.variationEvidence
        .filter(_.surfaceAllowance == VariationSurfaceAllowance.PublicLine)
        .map(_.proofId)
        .toSet
    lineProofIds.nonEmpty &&
      response.render.blocks.exists: block =>
        block.variationEvidenceIds.exists(lineProofIds.contains)

  private def noCommentary(response: CommentaryResponse): CommentaryResponse =
    response.copy(
      status = CommentaryResponseStatus.NoCommentary,
      noCommentary = true,
      internal = None,
      render = response.render.copy(
        status = RenderStatus.NoCommentary,
        blocks = Vector.empty,
        evidenceRefs = Vector.empty,
        variationEvidence = Vector.empty,
        boundaries = Vector.empty,
        suppressions = Vector.empty,
        wording = response.render.wording.copy(
          maxStrength = WordingStrength.Hidden,
          allowedPublicText = false
        )
      )
    )
