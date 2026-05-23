package lila.commentary

import play.api.libs.json.{ JsObject, Json }

import lila.commentary.analysis.PlayerProseBoundary

object MoveReviewResponseDiagnostics:

  private val FallbackAvailable = "fallback_available"
  private val Ready = "ready"
  private val RetryableFallback = "retryable_fallback"

  def json(response: CommentResponse): JsObject =
    Json.obj(
      "status" -> status(response),
      "sourceModeReason" -> sourceModeReason(response)
    )

  private def status(response: CommentResponse): String =
    val sourceMode = normalizedSourceMode(response)
    if isFallbackMode(sourceMode) && proseBoundaryReasons(response).nonEmpty then RetryableFallback
    else if isFallbackMode(sourceMode) then FallbackAvailable
    else Ready

  private def sourceModeReason(response: CommentResponse): String =
    val sourceMode = normalizedSourceMode(response)
    val boundaryReasons = proseBoundaryReasons(response)
    if isFallbackMode(sourceMode) && boundaryReasons.nonEmpty then boundaryReasons.head
    else
      sourceMode match
        case "rule_circuit_open"     => "polish_circuit_open"
        case "fallback_rule_empty"   => "empty_polish"
        case "fallback_rule_invalid" => firstValidationReason(response).getOrElse("invalid_polish")
        case "ai_polished"           => "ai_polished"
        case "rule"                  => "rule"
        case other if other.startsWith("fallback_rule") =>
          normalizeCode(other.stripPrefix("fallback_rule_")).filter(_.nonEmpty).getOrElse("fallback_rule")
        case other => normalizeCode(other).filter(_.nonEmpty).getOrElse("unknown")

  private def normalizedSourceMode(response: CommentResponse): String =
    normalizeCode(response.sourceMode).filter(_.nonEmpty).getOrElse("rule")

  private def firstValidationReason(response: CommentResponse): Option[String] =
    response.polishMeta
      .toList
      .flatMap(_.validationReasons)
      .map(normalizeCode)
      .collectFirst { case Some(reason) if reason.nonEmpty => reason }

  private def isFallbackMode(sourceMode: String): Boolean =
    sourceMode == "rule_circuit_open" || sourceMode.startsWith("fallback_rule")

  private def proseBoundaryReasons(response: CommentResponse): List[String] =
    PlayerProseBoundary.validateSanitized(Option(response.commentary).getOrElse("")).reasons

  private def normalizeCode(raw: String): Option[String] =
    Option(raw)
      .map(_.trim.toLowerCase.replaceAll("""[^a-z0-9]+""", "_").stripPrefix("_").stripSuffix("_"))
      .filter(_.nonEmpty)
