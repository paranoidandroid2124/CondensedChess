package lila.commentary.api

import java.util.Locale

import play.api.libs.json.*

import lila.commentary.api.CommentaryApiJson.given
import lila.commentary.certification.{ CertificationEvidencePurpose, CertificationEvidenceStrength }

object CommentaryPublicJsonTransport:

  private val BadRequestJson = Json.obj("error" -> "invalid_commentary_request")

  private val RequestFields = Set(
    "currentFen",
    "beforeFen",
    "playedMove",
    "nodeId",
    "ply",
    "enginePacket",
    "debug"
  )

  private val RuntimeEnginePacketFields = Set(
    "fen",
    "nodeId",
    "ply",
    "requestedDepth",
    "realizedDepth",
    "multiPv",
    "completed",
    "generatedAtEpochMs",
    "maxAgeMs",
    "engineConfigFingerprint",
    "score",
    "scorePerspective",
    "pvLines",
    "claims",
    "transition",
    "baseline"
  )

  private val RuntimeScoreFields = Set("type", "cp", "plies")

  private val RuntimeClaimFields = Set(
    "familyId",
    "owner",
    "purposes",
    "anchor",
    "minDepth",
    "minMultiPv",
    "minPvPlies",
    "requiredScore"
  )

  private val RuntimeTransitionFields = Set(
    "beforeFen",
    "playedMove",
    "afterFen",
    "beforeNodeId",
    "beforePly"
  )

  private val RuntimeBaselineFields = Set(
    "fen",
    "nodeId",
    "ply",
    "search",
    "score",
    "scorePerspective",
    "pvLines"
  )

  private val RuntimeSearchStateFields = Set(
    "requestedDepth",
    "realizedDepth",
    "multiPv",
    "completed",
    "generatedAtEpochMs",
    "maxAgeMs",
    "engineConfigFingerprint"
  )

  private val PublicFieldNameExceptions = Set(
    "engineconfigfingerprint",
    "pvlines"
  )

  private val CertificationPurposeKeys = CertificationEvidencePurpose.values.map(_.key).toSet
  private val CertificationStrengthKeys = CertificationEvidenceStrength.values.map(_.key).toSet

  private val ForbiddenExactFieldNames = Set(
    "parentbranchid",
    "parentid",
    "parentrootrank",
    "parentuciprefix",
    "rawlines",
    "rawprobes",
    "rawpvs",
    "preparedvariationevidence",
    "openingcontextcandidate"
  )

  private val ForbiddenFieldFragments = Set(
    "branchid",
    "candidateline",
    "cache",
    "completedprobe",
    "enginefingerprint",
    "internal",
    "proofid",
    "probe",
    "proves",
    "rawline",
    "rawprobe",
    "rawpv",
    "sourcerow"
  )

  def renderJson(body: JsValue): Either[JsObject, JsValue] =
    parseRequest(body).map: request =>
      Json.toJson(CommentaryBackendSeam.render(request))

  def parseRequest(body: JsValue): Either[JsObject, CommentaryRequest] =
    if !isPublicRequestShape(body) then Left(BadRequestJson)
    else body.validate[CommentaryRequest].asEither.left.map(_ => BadRequestJson)

  private def isPublicRequestShape(json: JsValue): Boolean =
    json match
      case obj: JsObject =>
        validateObject(obj, RequestFields):
          case ("enginePacket", value) => validateNullableObject(value)(validateRuntimeEnginePacket)
          case (_, value) => validateScalar(value)
      case _ => false

  private def validateRuntimeEnginePacket(obj: JsObject): Boolean =
    validateObject(obj, RuntimeEnginePacketFields):
      case ("score", value) => validateTypedObject(value)(validateRuntimeScore)
      case ("pvLines", value) => validatePvLines(value)
      case ("claims", value) => validateClaims(value)
      case ("transition", value) => validateNullableObject(value)(validateRuntimeTransition)
      case ("baseline", value) => validateNullableObject(value)(validateRuntimeBaseline)
      case (_, value) => validateScalar(value)

  private def validateRuntimeScore(obj: JsObject): Boolean =
    validateObject(obj, RuntimeScoreFields):
      case (_, value) => validateScalar(value)

  private def validateRuntimeScoreRequirement(obj: JsObject): Boolean =
    validateObject(obj, RuntimeScoreFields):
      case (_, value) => validateScalar(value)

  private def validateRuntimeClaim(obj: JsObject): Boolean =
    validateObject(obj, RuntimeClaimFields):
      case ("purposes", value) => validatePurposeMap(value)
      case ("requiredScore", value) => validateNullableObject(value)(validateRuntimeScoreRequirement)
      case (_, value) => validateScalar(value)

  private def validateRuntimeTransition(obj: JsObject): Boolean =
    validateObject(obj, RuntimeTransitionFields):
      case (_, value) => validateScalar(value)

  private def validateRuntimeBaseline(obj: JsObject): Boolean =
    validateObject(obj, RuntimeBaselineFields):
      case ("search", value) => validateTypedObject(value)(validateRuntimeSearchState)
      case ("score", value) => validateTypedObject(value)(validateRuntimeScore)
      case ("pvLines", value) => validatePvLines(value)
      case (_, value) => validateScalar(value)

  private def validateRuntimeSearchState(obj: JsObject): Boolean =
    validateObject(obj, RuntimeSearchStateFields):
      case (_, value) => validateScalar(value)

  private def validateObject(
      obj: JsObject,
      allowedFields: Set[String]
  )(validateField: (String, JsValue) => Boolean): Boolean =
    obj.fields.forall: (field, value) =>
      allowedFields.contains(field) &&
        !isInternalFieldName(field) &&
        validateField(field, value)

  private def validateNullableObject(value: JsValue)(validate: JsObject => Boolean): Boolean =
    value match
      case JsNull => true
      case obj: JsObject => validate(obj)
      case _: JsArray => false
      case _ => true

  private def validateTypedObject(value: JsValue)(validate: JsObject => Boolean): Boolean =
    value match
      case obj: JsObject => validate(obj)
      case _: JsArray => false
      case _ => true

  private def validateClaims(value: JsValue): Boolean =
    value match
      case JsArray(claims) =>
        claims.forall:
          case obj: JsObject => validateRuntimeClaim(obj)
          case _ => false
      case _: JsObject => false
      case _ => true

  private def validatePvLines(value: JsValue): Boolean =
    value match
      case JsArray(lines) =>
        lines.forall:
          case JsArray(moves) => moves.forall(_.isInstanceOf[JsString])
          case _ => false
      case _: JsObject => false
      case _ => true

  private def validatePurposeMap(value: JsValue): Boolean =
    value match
      case JsObject(fields) =>
        fields.forall: (field, purposeValue) =>
          CertificationPurposeKeys.contains(field) &&
            (purposeValue match
              case JsString(strength) => CertificationStrengthKeys.contains(strength)
              case _ => false)
      case _ => false

  private def validateScalar(value: JsValue): Boolean =
    value match
      case _: JsObject | _: JsArray => false
      case _ => true

  private def isInternalFieldName(field: String): Boolean =
    val normalized = normalizeFieldName(field)
    !PublicFieldNameExceptions.contains(normalized) &&
      (normalized.startsWith("proof") ||
        ForbiddenExactFieldNames.contains(normalized) ||
        ForbiddenFieldFragments.exists(normalized.contains))

  private def normalizeFieldName(field: String): String =
    field.toLowerCase(Locale.ROOT).filter: char =>
      (char >= 'a' && char <= 'z') || (char >= '0' && char <= '9')
