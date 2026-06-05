package lila.commentary

import play.api.libs.json.{ JsObject, Json }

import lila.commentary.analysis.PlayerProseBoundary

object MoveReviewResponseDiagnostics:

  private val FallbackAvailable = "fallback_available"
  private val Ready = "ready"
  private val RetryableFallback = "retryable_fallback"

  def json(response: CommentResponse): JsObject =
    val sourceMode = normalizedSourceMode(response)
    val fallbackMode = isFallbackSourceMode(sourceMode)
    val boundaryFailure =
      if fallbackMode then
        PlayerProseBoundary
          .validateSanitized(Option(response.commentary).getOrElse(""))
          .reasons
          .headOption
      else None
    Json.obj(
      "status" -> status(fallbackMode, boundaryFailure),
      "sourceModeReason" -> sourceModeReason(response, sourceMode, boundaryFailure)
    )

  private def status(fallbackMode: Boolean, boundaryFailure: Option[String]): String =
    if fallbackMode && boundaryFailure.nonEmpty then RetryableFallback
    else if fallbackMode then FallbackAvailable
    else Ready

  private def sourceModeReason(
      response: CommentResponse,
      sourceMode: String,
      boundaryFailure: Option[String]
  ): String =
    boundaryFailure.getOrElse {
      sourceMode match
        case "rule_circuit_open"     => "polish_circuit_open"
        case "fallback_rule_empty"   => "empty_polish"
        case "fallback_rule_invalid" => firstValidationReason(response).getOrElse("invalid_polish")
        case "ai_polished"           => "ai_polished"
        case "rule"                  => "rule"
        case other if other.startsWith("fallback_rule") =>
          normalizeCode(other.stripPrefix("fallback_rule_")).filter(_.nonEmpty).getOrElse("fallback_rule")
        case other => normalizeCode(other).filter(_.nonEmpty).getOrElse("unknown")
    }

  private def normalizedSourceMode(response: CommentResponse): String =
    normalizeCode(response.sourceMode).filter(_.nonEmpty).getOrElse("rule")

  private def firstValidationReason(response: CommentResponse): Option[String] =
    response.polishMeta
      .toList
      .flatMap(_.validationReasons)
      .flatMap(normalizeCode)
      .headOption

  private[commentary] def isFallbackSourceMode(sourceMode: String): Boolean =
    sourceMode == "rule_circuit_open" || sourceMode.startsWith("fallback_rule")

  private def normalizeCode(raw: String): Option[String] =
    Option(raw)
      .map(_.trim.toLowerCase.replaceAll("""[^a-z0-9]+""", "_").stripPrefix("_").stripSuffix("_"))
      .filter(_.nonEmpty)
