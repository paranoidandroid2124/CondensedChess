package lila.commentary.api

import chess.format.Fen
import play.api.libs.json.*

import scala.util.control.NonFatal

import lila.commentary.CommentaryCore
import lila.commentary.certification.{ CertificationEngineRuntimeIntake, CertificationEvidenceClaim, EngineNodeIdentity }
import lila.commentary.delta.StrategicDeltaExtraction
import lila.commentary.render.*
import lila.commentary.selection.*
import lila.commentary.strategic.StrategicObjectExtraction

final case class CommentaryRequest(
    currentFen: String,
    beforeFen: Option[String],
    playedMove: Option[String],
    nodeId: String,
    ply: Int,
    enginePacket: Option[CertificationEngineRuntimeIntake.RuntimeEnginePacket] = None,
    debug: Boolean = false
)

enum CommentaryResponseStatus(val key: String):
  case Rendered extends CommentaryResponseStatus("rendered")
  case ContextOnly extends CommentaryResponseStatus("contextOnly")
  case NoCommentary extends CommentaryResponseStatus("noCommentary")
  case InvalidRequest extends CommentaryResponseStatus("invalidRequest")

enum CommentaryEngineIntakeStatus(val key: String):
  case Accepted extends CommentaryEngineIntakeStatus("accepted")
  case Rejected extends CommentaryEngineIntakeStatus("rejected")

final case class CommentaryEngineIntake(
    status: CommentaryEngineIntakeStatus,
    reason: Option[String]
)

final case class CommentaryInternalMetadata(
    suppressions: Vector[RenderSuppression],
    engineIntake: Option[CommentaryEngineIntake],
    invalidReason: Option[String]
)

final case class CommentaryResponse(
    status: CommentaryResponseStatus,
    render: CommentaryRender,
    noCommentary: Boolean,
    internal: Option[CommentaryInternalMetadata]
)

final case class CommentaryPipelineInput(
    node: EngineNodeIdentity,
    currentFen: Fen.Full,
    beforeFen: Option[Fen.Full],
    currentExtraction: StrategicObjectExtraction,
    deltaExtraction: Option[StrategicDeltaExtraction],
    engineIntake: Option[CommentaryEngineIntake]
)

final class CommentaryBackendSeam private (
    claimProvider: CommentaryPipelineInput => Vector[CommentaryClaim],
    nowEpochMs: () => Long
):

  def render(request: CommentaryRequest): CommentaryResponse =
    renderWithDebug(request, includeDebug = false)

  def renderDebug(request: CommentaryRequest): CommentaryResponse =
    renderWithDebug(request, includeDebug = true)

  private def renderWithDebug(request: CommentaryRequest, includeDebug: Boolean): CommentaryResponse =
    validate(request) match
      case Left(reason) =>
        response(
          status = CommentaryResponseStatus.InvalidRequest,
          render = noCommentaryRender,
          debug = includeDebug,
          engineIntake = None,
          invalidReason = Some(reason)
        )
      case Right(valid) =>
        val engineIntake = intakeEngine(valid)
        val input = CommentaryPipelineInput(
          node = valid.node,
          currentFen = valid.currentFen,
          beforeFen = valid.beforeFen,
          currentExtraction = valid.currentExtraction,
          deltaExtraction = valid.deltaExtraction,
          engineIntake = engineIntake.map(_.metadata)
        )
        val claims = failClosedEngineClaims(claimProvider(input), engineIntake)
        val outline = ClaimSelector.select(claims)
        val plan = CommentaryOutlineBuilder.build(outline)
        val rendered = CommentaryRenderer.render(plan)
        response(
          status = responseStatus(rendered),
          render = rendered,
          debug = includeDebug,
          engineIntake = engineIntake.map(_.metadata),
          invalidReason = None
        )

  private def validate(request: CommentaryRequest): Either[String, ValidRequest] =
    for
      node <- safe(EngineNodeIdentity(request.nodeId, request.ply))
      currentFen <- Right(Fen.Full.clean(request.currentFen): Fen.Full)
      currentExtraction <- CommentaryCore.extractStrategicObjectsFailClosed(currentFen)
      transition <- validateTransition(request, currentFen)
    yield ValidRequest(node, currentFen, currentExtraction, transition.beforeFen, transition.deltaExtraction, request.enginePacket)

  private def validateTransition(
      request: CommentaryRequest,
      currentFen: Fen.Full
  ): Either[String, ValidTransition] =
    (request.beforeFen, request.playedMove) match
      case (None, None) => Right(ValidTransition(None, None))
      case (Some(beforeFen), Some(playedMove)) =>
        CommentaryCore
          .extractStrategicDeltasFromFensFailClosed(beforeFen, playedMove, currentFen.value)
          .map(delta => ValidTransition(Some(Fen.Full.clean(beforeFen): Fen.Full), Some(delta)))
      case _ =>
        Left("beforeFen and playedMove must be supplied together")

  private def intakeEngine(valid: ValidRequest): Option[BackendEngineIntake] =
    valid.requestEnginePacket.map: packet =>
      val result =
        valid.deltaExtraction match
          case Some(delta) =>
            CertificationEngineRuntimeIntake.forDeltaExtraction(
              delta = delta,
              expectedNode = valid.node,
              expectedBeforeFen = valid.beforeFen.get,
              expectedAfterFen = valid.currentFen,
              packet = Some(packet),
              nowEpochMs = nowEpochMs()
            )
          case None =>
            CertificationEngineRuntimeIntake.forObjectExtraction(
              current = valid.currentExtraction,
              expectedNode = valid.node,
              expectedFen = valid.currentFen,
              packet = Some(packet),
              nowEpochMs = nowEpochMs()
            )
      val status =
        if result.status == CertificationEngineRuntimeIntake.Status.Accepted then CommentaryEngineIntakeStatus.Accepted
        else CommentaryEngineIntakeStatus.Rejected
      BackendEngineIntake(
        metadata = CommentaryEngineIntake(
          status = status,
          reason = Option.when(status == CommentaryEngineIntakeStatus.Rejected)("engine_intake_rejected")
        ),
        evidenceRefs = result.evidence.all.map(engineEvidenceRef).toSet
      )

  private def failClosedEngineClaims(
      claims: Vector[CommentaryClaim],
      engineIntake: Option[BackendEngineIntake]
  ): Vector[CommentaryClaim] =
    claims.filter(engineCertificationRefsBound(_, engineIntake))

  private def engineCertificationRefsBound(
      claim: CommentaryClaim,
      engineIntake: Option[BackendEngineIntake]
  ): Boolean =
    val engineRefs = (claim.evidenceRefs ++ claim.lowerCarrierRefs).filter(_.kind == EvidenceRefKind.EngineCertification)
    engineRefs.isEmpty || engineIntake.exists(intake =>
      intake.metadata.status == CommentaryEngineIntakeStatus.Accepted &&
        engineRefs.forall(ref => intake.evidenceRefs.exists(_.matches(ref)))
    )

  private def engineEvidenceRef(claim: CertificationEvidenceClaim): BackendEngineEvidenceRef =
    val owner = if claim.owner.white then "white" else "black"
    BackendEngineEvidenceRef(
      id = s"engine-certification:${claim.familyId.value}:$owner:${claim.anchor.kind.key}:${claim.anchor.key}",
      owner = owner,
      anchor = claim.anchor.key
    )

  private def response(
      status: CommentaryResponseStatus,
      render: CommentaryRender,
      debug: Boolean,
      engineIntake: Option[CommentaryEngineIntake],
      invalidReason: Option[String]
  ): CommentaryResponse =
    val publicRender = render.copy(suppressions = Vector.empty)
    CommentaryResponse(
      status = status,
      render = publicRender,
      noCommentary = publicRender.status == RenderStatus.NoCommentary,
      internal = Option.when(debug)(
        CommentaryInternalMetadata(
          suppressions = render.suppressions,
          engineIntake = engineIntake,
          invalidReason = invalidReason
        )
      )
    )

  private def responseStatus(render: CommentaryRender): CommentaryResponseStatus =
    render.status match
      case RenderStatus.Rendered => CommentaryResponseStatus.Rendered
      case RenderStatus.ContextOnly => CommentaryResponseStatus.ContextOnly
      case RenderStatus.NoCommentary => CommentaryResponseStatus.NoCommentary

  private def noCommentaryRender: CommentaryRender =
    CommentaryRenderer.render(
      CommentaryPlan(
        main = None,
        support = PlanSection(PlanRole.Support, Vector.empty),
        context = PlanSection(PlanRole.Context, Vector.empty),
        contrast = PlanSection(PlanRole.Contrast, Vector.empty),
        blocked = Vector.empty,
        evidence = Vector.empty,
        wordingRules = WordingRules(WordingStrength.Hidden)
      )
    )

  private final case class ValidRequest(
      node: EngineNodeIdentity,
      currentFen: Fen.Full,
      currentExtraction: StrategicObjectExtraction,
      beforeFen: Option[Fen.Full],
      deltaExtraction: Option[StrategicDeltaExtraction],
      enginePacket: Option[CertificationEngineRuntimeIntake.RuntimeEnginePacket]
  ):
    def requestEnginePacket: Option[CertificationEngineRuntimeIntake.RuntimeEnginePacket] =
      enginePacket

  private final case class ValidTransition(
      beforeFen: Option[Fen.Full],
      deltaExtraction: Option[StrategicDeltaExtraction]
  )

  private final case class BackendEngineIntake(
      metadata: CommentaryEngineIntake,
      evidenceRefs: Set[BackendEngineEvidenceRef]
  )

  private final case class BackendEngineEvidenceRef(
      id: String,
      owner: String,
      anchor: String
  ):
    def matches(ref: EvidenceRef): Boolean =
      ref.id == id &&
        ref.owner.contains(owner) &&
        ref.anchor.contains(anchor)

  private def safe[A](body: => A): Either[String, A] =
    try Right(body)
    catch
      case NonFatal(error) =>
        Left(Option(error.getMessage).filter(_.nonEmpty).getOrElse(error.getClass.getSimpleName))

object CommentaryBackendSeam:

  private val default =
    new CommentaryBackendSeam(_ => Vector.empty, () => System.currentTimeMillis())

  def render(request: CommentaryRequest): CommentaryResponse =
    default.render(request)

  def renderDebug(request: CommentaryRequest): CommentaryResponse =
    default.renderDebug(request)

  def withClaimProvider(
      claimProvider: CommentaryPipelineInput => Vector[CommentaryClaim],
      nowEpochMs: () => Long = () => System.currentTimeMillis()
  ): CommentaryBackendSeam =
    new CommentaryBackendSeam(claimProvider, nowEpochMs)

object CommentaryApiJson:

  given Format[CommentaryResponseStatus] =
    enumFormat(CommentaryResponseStatus.values, _.key, "CommentaryResponseStatus")
  given Format[CommentaryEngineIntakeStatus] =
    enumFormat(CommentaryEngineIntakeStatus.values, _.key, "CommentaryEngineIntakeStatus")
  given Format[RenderRole] = enumFormat(RenderRole.values, _.key, "RenderRole")
  given Format[RenderStatus] = enumFormat(RenderStatus.values, _.key, "RenderStatus")
  given Format[WordingStrength] = enumFormat(WordingStrength.values, _.key, "WordingStrength")
  given Format[EvidenceRefKind] = enumFormat(EvidenceRefKind.values, _.key, "EvidenceRefKind")
  given Format[SuppressionReason] = enumFormat(SuppressionReason.values, _.key, "SuppressionReason")

  given Format[EvidenceRef] = Json.format[EvidenceRef]
  given Format[RenderText] = Json.format[RenderText]
  given Format[RenderEvidenceRef] = Json.format[RenderEvidenceRef]
  given Format[RenderBoundary] = Json.format[RenderBoundary]
  given Format[RenderSuppression] = Json.format[RenderSuppression]
  given Format[RenderWording] = Json.format[RenderWording]
  given Format[RenderBlock] = Json.format[RenderBlock]
  given Format[CommentaryRender] = Json.format[CommentaryRender]

  given Format[CertificationEngineRuntimeIntake.RuntimeScore] = Format(
    Reads: json =>
      (json \ "type").validate[String].flatMap:
        case "centipawns" => (json \ "cp").validate[Int].map(CertificationEngineRuntimeIntake.RuntimeScore.Centipawns.apply)
        case "mateIn" => (json \ "plies").validate[Int].map(CertificationEngineRuntimeIntake.RuntimeScore.MateIn.apply)
        case other => JsError(s"Unknown RuntimeScore: $other"),
    Writes:
      case CertificationEngineRuntimeIntake.RuntimeScore.Centipawns(cp) =>
        Json.obj("type" -> "centipawns", "cp" -> cp)
      case CertificationEngineRuntimeIntake.RuntimeScore.MateIn(plies) =>
        Json.obj("type" -> "mateIn", "plies" -> plies)
  )

  given Format[CertificationEngineRuntimeIntake.RuntimeScorePerspective] =
    enumFormat(
      CertificationEngineRuntimeIntake.RuntimeScorePerspective.values,
      {
        case CertificationEngineRuntimeIntake.RuntimeScorePerspective.SideToMove => "sideToMove"
        case CertificationEngineRuntimeIntake.RuntimeScorePerspective.White => "white"
        case CertificationEngineRuntimeIntake.RuntimeScorePerspective.Black => "black"
      },
      "RuntimeScorePerspective"
    )

  given Format[CertificationEngineRuntimeIntake.RuntimeScoreRequirement] = Format(
    Reads: json =>
      (json \ "type").validate[String].flatMap:
        case "centipawnAtLeast" =>
          (json \ "cp").validate[Int].map(CertificationEngineRuntimeIntake.RuntimeScoreRequirement.CentipawnAtLeast.apply)
        case "centipawnAtMost" =>
          (json \ "cp").validate[Int].map(CertificationEngineRuntimeIntake.RuntimeScoreRequirement.CentipawnAtMost.apply)
        case "centipawnSwingAtLeast" =>
          (json \ "cp").validate[Int].map(CertificationEngineRuntimeIntake.RuntimeScoreRequirement.CentipawnSwingAtLeast.apply)
        case "mateInAtMost" =>
          (json \ "plies").validate[Int].map(CertificationEngineRuntimeIntake.RuntimeScoreRequirement.MateInAtMost.apply)
        case other => JsError(s"Unknown RuntimeScoreRequirement: $other"),
    Writes:
      case CertificationEngineRuntimeIntake.RuntimeScoreRequirement.CentipawnAtLeast(cp) =>
        Json.obj("type" -> "centipawnAtLeast", "cp" -> cp)
      case CertificationEngineRuntimeIntake.RuntimeScoreRequirement.CentipawnAtMost(cp) =>
        Json.obj("type" -> "centipawnAtMost", "cp" -> cp)
      case CertificationEngineRuntimeIntake.RuntimeScoreRequirement.CentipawnSwingAtLeast(cp) =>
        Json.obj("type" -> "centipawnSwingAtLeast", "cp" -> cp)
      case CertificationEngineRuntimeIntake.RuntimeScoreRequirement.MateInAtMost(plies) =>
        Json.obj("type" -> "mateInAtMost", "plies" -> plies)
  )

  given Format[CertificationEngineRuntimeIntake.RuntimeAnchor] =
    enumFormat(
      CertificationEngineRuntimeIntake.RuntimeAnchor.values,
      _ => "board",
      "RuntimeAnchor"
    )

  given Format[CertificationEngineRuntimeIntake.RuntimeTransitionBinding] =
    Json.format[CertificationEngineRuntimeIntake.RuntimeTransitionBinding]
  given Format[CertificationEngineRuntimeIntake.RuntimeSearchState] =
    Json.format[CertificationEngineRuntimeIntake.RuntimeSearchState]
  given Format[CertificationEngineRuntimeIntake.RuntimeBaselinePacket] =
    Json.format[CertificationEngineRuntimeIntake.RuntimeBaselinePacket]
  given Format[CertificationEngineRuntimeIntake.RuntimeCertificationClaim] =
    Json.format[CertificationEngineRuntimeIntake.RuntimeCertificationClaim]
  given Format[CertificationEngineRuntimeIntake.RuntimeEnginePacket] =
    Json.format[CertificationEngineRuntimeIntake.RuntimeEnginePacket]

  given Format[CommentaryEngineIntake] = Json.format[CommentaryEngineIntake]
  given Format[CommentaryInternalMetadata] = Json.format[CommentaryInternalMetadata]
  given Format[CommentaryRequest] = Json.using[Json.WithDefaultValues].format[CommentaryRequest]
  given Format[CommentaryResponse] = Json.format[CommentaryResponse]

  private def enumFormat[A](values: Array[A], key: A => String, label: String): Format[A] =
    Format(
      Reads:
        case JsString(raw) =>
          values.find(value => key(value) == raw) match
            case Some(value) => JsSuccess(value)
            case None => JsError(s"Unknown $label: $raw")
        case _ => JsError(s"$label must be a string"),
      Writes(value => JsString(key(value)))
    )
