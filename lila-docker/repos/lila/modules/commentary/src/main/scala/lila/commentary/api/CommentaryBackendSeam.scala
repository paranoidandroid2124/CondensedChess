package lila.commentary.api

import chess.format.Fen
import chess.Pawn
import play.api.libs.json.*
import java.time.Instant

import scala.util.control.NonFatal

import lila.commentary.CommentaryCore
import lila.commentary.certification.{ CertificationEngineRuntimeIntake, CertificationEvidenceClaim, EngineNodeIdentity }
import lila.commentary.claim.{ EvidenceClaimHandoff, EvidenceClaimProducer }
import lila.commentary.delta.StrategicDeltaExtraction
import lila.commentary.line.*
import lila.commentary.render.*
import lila.commentary.root.RootPositionSupport
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

final case class CommentaryCompletedProbeCurrent(
    currentFen: String,
    nodeId: String,
    ply: Int,
    variant: String
)

final case class CommentaryCompletedProbeBudget(
    rootMultiPv: Int,
    childMultiPv: Int,
    depthFloor: Int,
    rootTargetDepth: Option[Int] = None,
    childTargetDepth: Option[Int] = None,
    maxAgeMillis: Option[Long] = None
)

final case class CommentaryCompletedProbeRequest(
    role: String,
    currentFen: String,
    nodeId: String,
    ply: Int,
    variant: String,
    multiPv: Int,
    requestedDepth: Int,
    depthFloor: Int,
    parentBranchId: Option[String] = None,
    parentUciPrefix: Option[Vector[String]] = None,
    parentRootRank: Option[Int] = None
)

final case class CommentaryCompletedProbeLine(
    rank: Int,
    multiPvIndex: Int,
    multiPv: Int,
    uci: Vector[String]
)

final case class CommentaryCompletedRootProbe(
    currentFen: String,
    nodeId: String,
    ply: Int,
    variant: String,
    engineFingerprint: String,
    requestedDepth: Int,
    realizedDepth: Int,
    multiPv: Int,
    generatedAt: String,
    maxAgeMillis: Long,
    completed: Boolean,
    lines: Vector[CommentaryCompletedProbeLine]
)

final case class CommentaryCompletedChildProbe(
    currentFen: String,
    nodeId: String,
    ply: Int,
    variant: String,
    engineFingerprint: String,
    parentBranchId: String,
    parentUciPrefix: Vector[String],
    parentRootRank: Int,
    requestedDepth: Int,
    realizedDepth: Int,
    multiPv: Int,
    generatedAt: String,
    maxAgeMillis: Long,
    completed: Boolean,
    lines: Vector[CommentaryCompletedProbeLine]
)

final case class CommentaryCompletedProbePayload(
    current: CommentaryCompletedProbeCurrent,
    engineFingerprint: String,
    budget: Option[CommentaryCompletedProbeBudget] = None,
    probeRequests: Vector[CommentaryCompletedProbeRequest],
    rootProbe: CommentaryCompletedRootProbe,
    childProbes: Vector[CommentaryCompletedChildProbe] = Vector.empty
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
    engineIntake: Option[CommentaryEngineIntake],
    completedProbe: Option[CommentaryCompletedProbePayload]
)

final class CommentaryBackendSeam private (
    claimProvider: CommentaryPipelineInput => Vector[CommentaryClaim],
    candidateLineAssemblyProvider: (CommentaryPipelineInput, Vector[CommentaryClaim]) => Option[CandidateProbeControlledAdapter.AssemblyResult],
    nowEpochMs: () => Long
):

  def render(request: CommentaryRequest): CommentaryResponse =
    renderWithDebug(request, completedProbe = None, includeDebug = false)

  def renderDebug(request: CommentaryRequest): CommentaryResponse =
    renderWithDebug(request, completedProbe = None, includeDebug = true)

  private[api] def renderInternal(
      request: CommentaryRequest,
      completedProbe: Option[CommentaryCompletedProbePayload]
  ): CommentaryResponse =
    renderWithDebug(request, completedProbe, includeDebug = false)

  private def renderWithDebug(
      request: CommentaryRequest,
      completedProbe: Option[CommentaryCompletedProbePayload],
      includeDebug: Boolean
  ): CommentaryResponse =
    validate(request, completedProbe) match
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
          engineIntake = engineIntake.map(_.metadata),
          completedProbe = valid.completedProbe
        )
        val producedClaims = claimProvider(input)
        val engineFilteredClaims = failClosedEngineClaims(producedClaims, engineIntake)
        val candidateLineAssembly = candidateLineAssemblyProvider(input, engineFilteredClaims)
        val claims = attachPreparedVariationEvidence(engineFilteredClaims, candidateLineAssembly)
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

  private def validate(
      request: CommentaryRequest,
      completedProbe: Option[CommentaryCompletedProbePayload]
  ): Either[String, ValidRequest] =
    for
      node <- safe(EngineNodeIdentity(request.nodeId, request.ply))
      currentFen <- Right(Fen.Full.clean(request.currentFen): Fen.Full)
      currentExtraction <- CommentaryCore.extractStrategicObjectsFailClosed(currentFen)
      transition <- validateTransition(request, currentFen)
    yield ValidRequest(
      node,
      currentFen,
      currentExtraction,
      transition.beforeFen,
      transition.deltaExtraction,
      request.enginePacket,
      completedProbe
    )

  private def validateTransition(
      request: CommentaryRequest,
      currentFen: Fen.Full
  ): Either[String, ValidTransition] =
    (request.beforeFen, request.playedMove) match
      case (None, None) => Right(ValidTransition(None, None))
      case (Some(beforeFen), Some(playedMove)) =>
        CommentaryCore
          .extractStrategicDeltasFromFensFailClosed(beforeFen, playedMove, currentFen.value)
          .flatMap: delta =>
            val cleanBeforeFen = Fen.Full.clean(beforeFen): Fen.Full
            validateTransitionClocks(cleanBeforeFen, currentFen, delta)
              .map(_ => ValidTransition(Some(cleanBeforeFen), Some(delta)))
      case _ =>
        Left("beforeFen and playedMove must be supplied together")

  private def validateTransitionClocks(
      beforeFen: Fen.Full,
      afterFen: Fen.Full,
      delta: StrategicDeltaExtraction
  ): Either[String, Unit] =
    for
      beforeClock <- fenClock(beforeFen)
      afterClock <- fenClock(afterFen)
      beforePosition <- RootPositionSupport.exactPosition(delta.before.rootState)
      exactMove <- beforePosition.move(delta.playedMove).left.map(_.toString)
      movingPiece <- beforePosition.board.pieceAt(delta.playedMove.orig).toRight("missing transition moving piece")
      expectedHalfMove = if movingPiece.role == Pawn || exactMove.capture.nonEmpty then 0 else beforeClock.halfMove + 1
      expectedFullMove = if beforeClock.sideToMove == "b" then beforeClock.fullMove + 1 else beforeClock.fullMove
      _ <- Either.cond(
        afterClock.halfMove == expectedHalfMove && afterClock.fullMove == expectedFullMove,
        (),
        "transition full-FEN clock mismatch"
      )
    yield ()

  private def fenClock(fen: Fen.Full): Either[String, FenClock] =
    fen.value.trim.split("\\s+").toVector match
      case Vector(_, sideToMove, _, _, halfMove, fullMove) =>
        for
          half <- halfMove.toIntOption.toRight("invalid halfmove clock")
          full <- fullMove.toIntOption.toRight("invalid fullmove clock")
          _ <- Either.cond(sideToMove == "w" || sideToMove == "b", (), "invalid side-to-move field")
          _ <- Either.cond(half >= 0 && full > 0, (), "invalid FEN clock value")
        yield FenClock(sideToMove, half, full)
      case _ =>
        Left("expected full FEN with clock fields")

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

  private def attachPreparedVariationEvidence(
      claims: Vector[CommentaryClaim],
      candidateLineAssembly: Option[CandidateProbeControlledAdapter.AssemblyResult]
  ): Vector[CommentaryClaim] =
    val preparedByClaim =
      candidateLineAssembly.toVector
        .flatMap(_.preparedVariationEvidence)
        .groupBy(_.boundClaimId)
    if preparedByClaim.isEmpty then claims
    else
      claims.map: claim =>
        preparedByClaim.get(claim.id) match
          case None => claim
          case Some(prepared) =>
            claim.copy(variationEvidence = (claim.variationEvidence ++ prepared).distinct)

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
        variationEvidence = Vector.empty,
        wordingRules = WordingRules(WordingStrength.Hidden)
      )
    )

  private final case class ValidRequest(
      node: EngineNodeIdentity,
      currentFen: Fen.Full,
      currentExtraction: StrategicObjectExtraction,
      beforeFen: Option[Fen.Full],
      deltaExtraction: Option[StrategicDeltaExtraction],
      enginePacket: Option[CertificationEngineRuntimeIntake.RuntimeEnginePacket],
      completedProbe: Option[CommentaryCompletedProbePayload]
  ):
    def requestEnginePacket: Option[CertificationEngineRuntimeIntake.RuntimeEnginePacket] =
      enginePacket

  private final case class ValidTransition(
      beforeFen: Option[Fen.Full],
      deltaExtraction: Option[StrategicDeltaExtraction]
  )

  private final case class FenClock(
      sideToMove: String,
      halfMove: Int,
      fullMove: Int
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

  private val proofCache = CandidateLineProofCache.InMemory.empty

  private val default =
    new CommentaryBackendSeam(
      input => EvidenceClaimProducer.produce(input.currentExtraction, input.deltaExtraction, EvidenceClaimHandoff.empty),
      defaultCandidateLineAssembly,
      () => System.currentTimeMillis()
    )

  def render(request: CommentaryRequest): CommentaryResponse =
    default.render(request)

  def renderDebug(request: CommentaryRequest): CommentaryResponse =
    default.renderDebug(request)

  private[api] def renderInternal(
      request: CommentaryRequest,
      completedProbe: Option[CommentaryCompletedProbePayload]
  ): CommentaryResponse =
    default.renderInternal(request, completedProbe)

  def withClaimProvider(
      claimProvider: CommentaryPipelineInput => Vector[CommentaryClaim],
      nowEpochMs: () => Long = () => System.currentTimeMillis()
  ): CommentaryBackendSeam =
    new CommentaryBackendSeam(claimProvider, (_, _) => None, nowEpochMs)

  def withClaimProviderAndCandidateLineAssembly(
      claimProvider: CommentaryPipelineInput => Vector[CommentaryClaim],
      candidateLineAssemblyProvider: (CommentaryPipelineInput, Vector[CommentaryClaim]) => Option[CandidateProbeControlledAdapter.AssemblyResult],
      nowEpochMs: () => Long = () => System.currentTimeMillis()
  ): CommentaryBackendSeam =
    new CommentaryBackendSeam(claimProvider, candidateLineAssemblyProvider, nowEpochMs)

  def withClaimProviderAndCandidateLineAssembly(
      claimProvider: CommentaryPipelineInput => Vector[CommentaryClaim],
      candidateLineAssemblyProvider: CommentaryPipelineInput => Option[CandidateProbeControlledAdapter.AssemblyResult],
      nowEpochMs: () => Long
  ): CommentaryBackendSeam =
    new CommentaryBackendSeam(claimProvider, (input, _) => candidateLineAssemblyProvider(input), nowEpochMs)

  def withEvidenceHandoffProvider(
      evidenceHandoffProvider: CommentaryPipelineInput => EvidenceClaimHandoff,
      nowEpochMs: () => Long = () => System.currentTimeMillis()
  ): CommentaryBackendSeam =
    new CommentaryBackendSeam(
      input => EvidenceClaimProducer.produce(input.currentExtraction, input.deltaExtraction, evidenceHandoffProvider(input)),
      (_, _) => None,
      nowEpochMs
    )

  private def defaultCandidateLineAssembly(
      input: CommentaryPipelineInput,
      claims: Vector[CommentaryClaim]
  ): Option[CandidateProbeControlledAdapter.AssemblyResult] =
    input.completedProbe.flatMap: payload =>
      completedProbeAssemblyInput(input, claims, payload, proofCache)
        .map(CandidateLineAssemblyProvider.assemble)
        .map(stripRootOnlyPublicVariationEvidence)

  private def stripRootOnlyPublicVariationEvidence(
      result: CandidateProbeControlledAdapter.AssemblyResult
  ): CandidateProbeControlledAdapter.AssemblyResult =
    val hasDefenderResource =
      result.preparedVariationEvidence.exists(evidence =>
        evidence.role == VariationEvidenceRole.DefenderResource ||
          evidence.moveRole == VariationMoveRole.DefenderResource
      )
    if hasDefenderResource then result
    else result.copy(preparedVariationEvidence = Vector.empty)

  private[api] def completedProbeAssemblyInput(
      input: CommentaryPipelineInput,
      claims: Vector[CommentaryClaim],
      payload: CommentaryCompletedProbePayload,
      cache: CandidateLineProofCache
  ): Option[CandidateLineAssemblyProvider.Input] =
    for
      budget <- completedProbeBudget(payload.budget)
      _ <- Option.when(payloadCurrentMatches(input, payload.current))(())
      _ <- Option.when(payload.engineFingerprint.trim.nonEmpty)(())
      rootRequest <- CandidateProbePlan
        .rootStage(input.currentFen.value, input.node.nodeId, input.node.ply, payload.current.variant, payload.engineFingerprint.trim, budget)
        .requests
        .headOption
      rootPayload <- completedRootPayload(payload.rootProbe, rootRequest)
      childPayloads <- completedChildPayloads(input, rootPayload, payload.childProbes, budget)
    yield CandidateLineAssemblyProvider.Input(
      currentFen = input.currentFen.value,
      nodeId = input.node.nodeId,
      ply = input.node.ply,
      variant = payload.current.variant,
      engineFingerprint = payload.engineFingerprint.trim,
      budget = budget,
      completedRootProbe = Some(rootPayload),
      completedChildProbes = childPayloads,
      loweringBinding = loweringBinding(claims),
      nowEpochMs = System.currentTimeMillis(),
      proofCache = Some(cache)
    )

  private def completedProbeBudget(
      maybeBudget: Option[CommentaryCompletedProbeBudget]
  ): Option[CandidateProbeBudget] =
    safeOption:
      maybeBudget match
        case None => CandidateProbeBudget.Default
        case Some(budget) =>
          CandidateProbeBudget(
            rootMultiPv = budget.rootMultiPv,
            childMultiPv = budget.childMultiPv,
            targetDepth = budget.rootTargetDepth.getOrElse(18),
            floorDepth = budget.depthFloor,
            strongCacheTargetDepth = budget.rootTargetDepth.getOrElse(18).max(20),
            childRootRankLimit = 2,
            allowExpandedThirdRootChildProbe = false,
            allowThirdRootChildFromCache = true
          )

  private def payloadCurrentMatches(input: CommentaryPipelineInput, current: CommentaryCompletedProbeCurrent): Boolean =
    current.currentFen == input.currentFen.value &&
      current.nodeId == input.node.nodeId &&
      current.ply == input.node.ply &&
      current.variant.trim.nonEmpty

  private def completedRootPayload(
      root: CommentaryCompletedRootProbe,
      request: CandidateProbeRequest
  ): Option[CandidateProbeResultPayload] =
    for
      generatedAt <- epochMillis(root.generatedAt)
      _ <- Option.when(
        root.completed &&
          root.currentFen == request.startFen &&
          root.nodeId == request.nodeId &&
          root.ply == request.ply &&
          root.variant == request.variant &&
          root.engineFingerprint == request.engineFingerprint &&
          root.requestedDepth == request.targetDepth &&
          root.multiPv == request.multiPv
      )(())
      lines <- completedLines(root.lines, request.multiPv, "root-candidate")
    yield CandidateProbeResultPayload(
      request = request,
      lines = lines,
      realizedDepth = root.realizedDepth,
      generatedAtEpochMs = generatedAt,
      maxAgeMs = root.maxAgeMillis,
      completed = true
    )

  private def completedChildPayloads(
      input: CommentaryPipelineInput,
      rootPayload: CandidateProbeResultPayload,
      childDtos: Vector[CommentaryCompletedChildProbe],
      budget: CandidateProbeBudget
  ): Option[Vector[CandidateProbeResultPayload]] =
    val rootPacket = CandidateProbeControlledAdapter.rootPacketFrom(rootPayload, CandidateLineProvenanceKind.EngineRoot)
    val rootEvidence =
      rootPacket.toVector.flatMap(packet =>
        CandidateLinePacketHandoff.normalize(
          CandidateLinePacketHandoffInput(input.currentFen.value, input.node.nodeId, input.node.ply, packet),
          nowEpochMs = System.currentTimeMillis()
        )
      )
    val requests =
      CandidateProbePlan.childStage(rootEvidence, budget = budget, nowEpochMs = System.currentTimeMillis()).requests
    val payloads =
      childDtos.flatMap: dto =>
        requests
          .find(request =>
            request.parentRootRank.contains(dto.parentRootRank) &&
              request.parentBranchId.exists(_.value == dto.parentBranchId) &&
              request.parentLinePrefix == dto.parentUciPrefix.map(_.trim.toLowerCase) &&
              request.startFen == dto.currentFen &&
              request.nodeId == dto.nodeId &&
              request.ply == dto.ply &&
              request.variant == dto.variant &&
              request.engineFingerprint == dto.engineFingerprint &&
              request.targetDepth == dto.requestedDepth &&
              request.multiPv == dto.multiPv
          )
          .flatMap(request => completedChildPayload(dto, request))
    Option.when(payloads.size == childDtos.size)(payloads)

  private def completedChildPayload(
      child: CommentaryCompletedChildProbe,
      request: CandidateProbeRequest
  ): Option[CandidateProbeResultPayload] =
    for
      generatedAt <- epochMillis(child.generatedAt)
      _ <- Option.when(child.completed)(())
      lines <- completedLines(child.lines, request.multiPv, s"defender-resource-${request.parentRootRank.getOrElse(0)}")
    yield CandidateProbeResultPayload(
      request = request,
      lines = lines,
      realizedDepth = child.realizedDepth,
      generatedAtEpochMs = generatedAt,
      maxAgeMs = child.maxAgeMillis,
      completed = true
    )

  private def completedLines(
      lines: Vector[CommentaryCompletedProbeLine],
      multiPv: Int,
      idPrefix: String
  ): Option[Vector[CandidateProbeResultLine]] =
    val expected = (1 to multiPv).toVector
    val rankPairs = lines.map(line => line.rank -> line.multiPvIndex).sortBy(_._1)
    Option.when(lines.size == multiPv && rankPairs == expected.map(index => index -> index)):
      lines.sortBy(_.rank).map: line =>
        CandidateProbeResultLine(
          branchId = CandidateBranchId(s"$idPrefix-${line.rank}"),
          rank = line.rank,
          multiPvIndex = line.multiPvIndex,
          uciLine = line.uci
        )

  private def loweringBinding(claims: Vector[CommentaryClaim]): Option[CandidateLineEvidenceLowering.Binding] =
    claims
      .filter(claim => claim.status == ClaimStatus.Admitted && claim.exactBoardBound)
      .flatMap: claim =>
        claim.evidenceRefs
          .find(ref =>
            ref.kind != EvidenceRefKind.RawEngine &&
              ref.kind != EvidenceRefKind.SourceContext &&
              EvidenceRef.isPublicSafeProvenanceId(ref.id) &&
              ref.owner.isDefined &&
              ref.anchor.isDefined &&
              ref.route.isDefined &&
              ref.scope.isDefined
          )
          .map(ref =>
            CandidateLineEvidenceLowering.Binding(
              boundClaimId = claim.id,
              owner = ref.owner.get,
              defender = claim.defender,
              anchor = ref.anchor.get,
              route = ref.route.get,
              scope = ref.scope.get,
              provenanceRef = ref
            )
          )
      .headOption

  private def epochMillis(raw: String): Option[Long] =
    raw.trim.toLongOption.orElse(safeOption(Instant.parse(raw.trim).toEpochMilli))

  private def safeOption[A](body: => A): Option[A] =
    try Some(body)
    catch case NonFatal(_) => None

object CommentaryApiJson:

  given Format[CommentaryResponseStatus] =
    enumFormat(CommentaryResponseStatus.values, _.key, "CommentaryResponseStatus")
  given Format[CommentaryEngineIntakeStatus] =
    enumFormat(CommentaryEngineIntakeStatus.values, _.key, "CommentaryEngineIntakeStatus")
  given Format[RenderRole] = enumFormat(RenderRole.values, _.key, "RenderRole")
  given Format[RenderStatus] = enumFormat(RenderStatus.values, _.key, "RenderStatus")
  given Format[RenderLineRole] = enumFormat(RenderLineRole.values, _.key, "RenderLineRole")
  given Format[WordingStrength] = enumFormat(WordingStrength.values, _.key, "WordingStrength")
  given Format[EvidenceRefKind] = enumFormat(EvidenceRefKind.values, _.key, "EvidenceRefKind")
  given Format[SuppressionReason] = enumFormat(SuppressionReason.values, _.key, "SuppressionReason")
  given Format[VariationMoveRole] = enumFormat(VariationMoveRole.values, _.key, "VariationMoveRole")
  given Format[VariationProofPurpose] = enumFormat(VariationProofPurpose.values, _.key, "VariationProofPurpose")
  given Format[VariationTestResult] = enumFormat(VariationTestResult.values, _.key, "VariationTestResult")
  given Format[VariationSurfaceAllowance] =
    enumFormat(VariationSurfaceAllowance.values, _.key, "VariationSurfaceAllowance")

  given Format[EvidenceRef] = Json.format[EvidenceRef]
  given Format[RenderText] = Json.format[RenderText]
  given Format[RenderEvidenceRef] = Json.format[RenderEvidenceRef]
  given Format[RenderVariationMove] = Json.format[RenderVariationMove]
  given Format[RenderVariationBoundary] = Json.format[RenderVariationBoundary]
  given Format[RenderVariationEvidence] = Json.format[RenderVariationEvidence]
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

  given Format[CommentaryCompletedProbeCurrent] = Json.format[CommentaryCompletedProbeCurrent]
  given Format[CommentaryCompletedProbeBudget] =
    Json.using[Json.WithDefaultValues].format[CommentaryCompletedProbeBudget]
  given Format[CommentaryCompletedProbeRequest] =
    Json.using[Json.WithDefaultValues].format[CommentaryCompletedProbeRequest]
  given Format[CommentaryCompletedProbeLine] = Json.format[CommentaryCompletedProbeLine]
  given Format[CommentaryCompletedRootProbe] = Json.format[CommentaryCompletedRootProbe]
  given Format[CommentaryCompletedChildProbe] = Json.format[CommentaryCompletedChildProbe]
  given Format[CommentaryCompletedProbePayload] =
    Json.using[Json.WithDefaultValues].format[CommentaryCompletedProbePayload]

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
