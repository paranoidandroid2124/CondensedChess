package lila.commentary.api

import chess.format.Fen
import chess.Pawn
import play.api.libs.json.*
import java.time.Instant

import scala.util.control.NonFatal

import lila.commentary.CommentaryCore
import lila.commentary.certification.{
  CertificationEngineRuntimeIntake,
  CertificationEvidenceBundle,
  CertificationEvidenceClaim,
  CertificationEvidencePurpose,
  CertificationEvidenceStrength,
  CertificationExtractor,
  EngineNodeIdentity
}
import lila.commentary.claim.{ EvidenceClaimHandoff, EvidenceClaimProducer }
import lila.commentary.delta.StrategicDeltaExtraction
import lila.commentary.line.*
import lila.commentary.projection.StrategyProjectionAdmissionProducer
import lila.commentary.render.*
import lila.commentary.root.RootPositionSupport
import lila.commentary.selection.*
import lila.commentary.selection.semantic.*
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
    invalidReason: Option[String],
    semanticShadow: Option[SemanticShadowSummary] = None
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
    engineCertificationEvidence: Option[CertificationEvidenceBundle],
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
          engineCertificationEvidence = engineIntake.map(_.evidence),
          completedProbe = valid.completedProbe
        )
        val producedClaims = claimProvider(input)
        val engineFilteredClaims = failClosedEngineClaims(producedClaims, engineIntake)
        val candidateLineAssembly = candidateLineAssemblyProvider(input, engineFilteredClaims)
        val claims = attachPreparedVariationEvidence(engineFilteredClaims, candidateLineAssembly)
        val outline = ClaimSelector.select(claims)
        val semanticShadow =
          Option.when(includeDebug)(
            SemanticShadowSummary.from(SemanticClaimSelector.shadow(input, claims, outline))
          )
        val plan = CommentaryOutlineBuilder.build(outline)
        val rendered = CommentaryRenderer.render(plan)
        response(
          status = responseStatus(rendered),
          render = rendered,
          debug = includeDebug,
          engineIntake = engineIntake.map(_.metadata),
          invalidReason = None,
          semanticShadow = semanticShadow
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
        evidence = result.evidence.asBundle,
        evidenceRefs = result.evidence.all.flatMap(engineEvidenceRef).toSet
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

  private def engineEvidenceRef(claim: CertificationEvidenceClaim): Option[BackendEngineEvidenceRef] =
    val owner = if claim.owner.white then "white" else "black"
    enginePublicContract(claim).map: contract =>
      BackendEngineEvidenceRef(
        id = s"engine-certification:${claim.familyId.value}:$owner:${claim.anchor.kind.key}:${claim.anchor.key}",
        owner = owner,
        anchor = claim.anchor.key,
        route = contract.route,
        scope = contract.scope
      )

  private def response(
      status: CommentaryResponseStatus,
      render: CommentaryRender,
      debug: Boolean,
      engineIntake: Option[CommentaryEngineIntake],
      invalidReason: Option[String],
      semanticShadow: Option[SemanticShadowSummary] = None
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
          invalidReason = invalidReason,
          semanticShadow = semanticShadow
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
      evidence: CertificationEvidenceBundle,
      evidenceRefs: Set[BackendEngineEvidenceRef]
  )

  private final case class BackendEngineEvidenceRef(
      id: String,
      owner: String,
      anchor: String,
      route: String,
      scope: String
  ):
    def matches(ref: EvidenceRef): Boolean =
      ref.id == id &&
        ref.owner.contains(owner) &&
        ref.anchor.contains(anchor) &&
        ref.route.contains(route) &&
        ref.scope.contains(scope)

  private final case class EnginePublicContract(
      route: String,
      scope: String,
      requiredPurposes: Set[CertificationEvidencePurpose]
  )

  private def enginePublicContract(claim: CertificationEvidenceClaim): Option[EnginePublicContract] =
    enginePublicContracts.get(claim.familyId.value).filter: contract =>
      contract.requiredPurposes.forall(purpose =>
        claim.purposeStrengths.get(purpose).contains(CertificationEvidenceStrength.Satisfied)
      )

  private val enginePublicContracts: Map[String, EnginePublicContract] =
    import CertificationEvidencePurpose.*
    Map(
      "DevelopmentComparison" -> EnginePublicContract(
        "development_superiority",
        "comparative",
        Set(ComparativeSuperiority)
      ),
      "InitiativeWindow" -> EnginePublicContract(
        "counterplay_denial_window",
        "comparative",
        Set(CounterplayDenial, BestDefenseSurvival)
      ),
      "MobilityComparison" -> EnginePublicContract(
        "mobility_superiority",
        "comparative",
        Set(ComparativeSuperiority)
      ),
      "ComparativeKingFragility" -> EnginePublicContract(
        "king_fragility_asymmetry",
        "comparative",
        Set(ComparativeSuperiority)
      ),
      "CertifiedKingSafetyEdge" -> EnginePublicContract(
        "king_safety_edge_certification",
        "comparative",
        Set(ComparativeSuperiority, BestDefenseSurvival)
      ),
      "MaterialHarvest" -> EnginePublicContract(
        "realized_material_conversion",
        "current_position",
        Set(BestDefenseSurvival, TacticalReleaseDetection)
      ),
      "SpaceBindRestrictionCertification" -> EnginePublicContract(
        "space_bind_persistence",
        "current_position",
        Set(BestDefenseSurvival, TacticalReleaseDetection)
      )
    )

  private def safe[A](body: => A): Either[String, A] =
    try Right(body)
    catch
      case NonFatal(error) =>
        Left(Option(error.getMessage).filter(_.nonEmpty).getOrElse(error.getClass.getSimpleName))

object CommentaryBackendSeam:

  private val proofCache = CandidateLineProofCache.InMemory.empty

  private val default =
    new CommentaryBackendSeam(
      input => EvidenceClaimProducer.produce(input.currentExtraction, input.deltaExtraction, defaultEvidenceHandoff(input)),
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

  private def defaultEvidenceHandoff(input: CommentaryPipelineInput): EvidenceClaimHandoff =
    val certification =
      input.engineCertificationEvidence.filterNot(_.isEmpty).flatMap: evidence =>
        input.deltaExtraction match
          case Some(delta) =>
            CertificationExtractor
              .fromDeltaExtractionFailClosed(delta, evidence)
              .toOption
          case None =>
            CertificationExtractor
              .fromObjectExtractionFailClosed(input.currentExtraction, evidence)
              .toOption
    val projectionAdmissions =
      StrategyProjectionAdmissionProducer.produce(
        StrategyProjectionAdmissionProducer.Input(
          currentExtraction = input.currentExtraction,
          deltaExtraction = input.deltaExtraction,
          certificationEvidence = input.engineCertificationEvidence.getOrElse(CertificationEvidenceBundle.empty)
        )
      )
    EvidenceClaimHandoff(certification = certification, projectionAdmissions = projectionAdmissions)

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
      plannedChildRequests = plannedCompletedChildRequests(input, rootPayload, budget)
      childRequests <- childRequestsForDtos(payload.childProbes, plannedChildRequests)
      _ <- Option.when(probeRequestsMatch(payload.probeRequests, rootRequest +: childRequests))(())
      childPayloads <- completedChildPayloads(payload.childProbes, childRequests)
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

  private def plannedCompletedChildRequests(
      input: CommentaryPipelineInput,
      rootPayload: CandidateProbeResultPayload,
      budget: CandidateProbeBudget
  ): Vector[CandidateProbeRequest] =
    val rootPacket = CandidateProbeControlledAdapter.rootPacketFrom(rootPayload, CandidateLineProvenanceKind.EngineRoot)
    val rootEvidence =
      rootPacket.toVector.flatMap(packet =>
        CandidateLinePacketHandoff.normalize(
          CandidateLinePacketHandoffInput(input.currentFen.value, input.node.nodeId, input.node.ply, packet),
          nowEpochMs = System.currentTimeMillis()
        )
      )
    CandidateProbePlan.childStage(rootEvidence, budget = budget, nowEpochMs = System.currentTimeMillis()).requests

  private def childRequestsForDtos(
      childDtos: Vector[CommentaryCompletedChildProbe],
      plannedRequests: Vector[CandidateProbeRequest]
  ): Option[Vector[CandidateProbeRequest]] =
    val matched =
      childDtos.flatMap(dto => plannedRequests.find(childRequestMatchesDto(_, dto)))
    Option.when(matched.size == childDtos.size)(matched)

  private def completedChildPayloads(
      childDtos: Vector[CommentaryCompletedChildProbe],
      requests: Vector[CandidateProbeRequest]
  ): Option[Vector[CandidateProbeResultPayload]] =
    val payloads =
      childDtos.flatMap: dto =>
        requests
          .find(childRequestMatchesDto(_, dto))
          .flatMap(request => completedChildPayload(dto, request))
    Option.when(payloads.size == childDtos.size)(payloads)

  private def childRequestMatchesDto(
      request: CandidateProbeRequest,
      dto: CommentaryCompletedChildProbe
  ): Boolean =
    request.parentRootRank.contains(dto.parentRootRank) &&
      request.parentBranchId.exists(_.value == dto.parentBranchId) &&
      request.parentLinePrefix == dto.parentUciPrefix.map(_.trim.toLowerCase) &&
      request.startFen == dto.currentFen &&
      request.nodeId == dto.nodeId &&
      request.ply == dto.ply &&
      request.variant == dto.variant &&
      request.engineFingerprint == dto.engineFingerprint &&
      request.targetDepth == dto.requestedDepth &&
      request.floorDepth <= dto.realizedDepth &&
      request.multiPv == dto.multiPv

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
    Option.when(
      lines.size == multiPv &&
        rankPairs == expected.map(index => index -> index) &&
        lines.forall(_.multiPv == multiPv)
    ):
      lines.sortBy(_.rank).map: line =>
        CandidateProbeResultLine(
          branchId = CandidateBranchId(s"$idPrefix-${line.rank}"),
          rank = line.rank,
          multiPvIndex = line.multiPvIndex,
          uciLine = line.uci
        )

  private final case class CompletedProbeRequestKey(
      role: String,
      currentFen: String,
      nodeId: String,
      ply: Int,
      variant: String,
      multiPv: Int,
      requestedDepth: Int,
      depthFloor: Int,
      parentBranchId: Option[String],
      parentUciPrefix: Vector[String],
      parentRootRank: Option[Int]
  )

  private def probeRequestsMatch(
      supplied: Vector[CommentaryCompletedProbeRequest],
      expected: Vector[CandidateProbeRequest]
  ): Boolean =
    val suppliedKeys = supplied.map(requestKey).sortBy(keySort)
    val expectedKeys = expected.map(requestKey).sortBy(keySort)
    suppliedKeys == expectedKeys

  private def requestKey(request: CommentaryCompletedProbeRequest): CompletedProbeRequestKey =
    CompletedProbeRequestKey(
      role = request.role.trim,
      currentFen = request.currentFen.trim,
      nodeId = request.nodeId.trim,
      ply = request.ply,
      variant = request.variant.trim,
      multiPv = request.multiPv,
      requestedDepth = request.requestedDepth,
      depthFloor = request.depthFloor,
      parentBranchId = request.parentBranchId.map(_.trim),
      parentUciPrefix = request.parentUciPrefix.getOrElse(Vector.empty).map(_.trim.toLowerCase),
      parentRootRank = request.parentRootRank
    )

  private def requestKey(request: CandidateProbeRequest): CompletedProbeRequestKey =
    CompletedProbeRequestKey(
      role = request.role.key,
      currentFen = request.startFen,
      nodeId = request.nodeId,
      ply = request.ply,
      variant = request.variant,
      multiPv = request.multiPv,
      requestedDepth = request.targetDepth,
      depthFloor = request.floorDepth,
      parentBranchId = request.parentBranchId.map(_.value),
      parentUciPrefix = request.parentLinePrefix.map(_.trim.toLowerCase),
      parentRootRank = request.parentRootRank
    )

  private def keySort(key: CompletedProbeRequestKey): String =
    Vector(
      key.role,
      key.currentFen,
      key.ply.toString,
      key.nodeId,
      key.variant,
      key.multiPv.toString,
      key.requestedDepth.toString,
      key.depthFloor.toString,
      key.parentBranchId.getOrElse(""),
      key.parentUciPrefix.mkString(" "),
      key.parentRootRank.map(_.toString).getOrElse("")
    ).mkString("|")

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
  given Format[ClaimLayer] = enumFormat(ClaimLayer.values, _.key, "ClaimLayer")
  given Format[ClaimStatus] = enumFormat(ClaimStatus.values, _.key, "ClaimStatus")
  given Format[WordingStrength] = enumFormat(WordingStrength.values, _.key, "WordingStrength")
  given Format[EvidenceRefKind] = enumFormat(EvidenceRefKind.values, _.key, "EvidenceRefKind")
  given Format[SuppressionReason] = enumFormat(SuppressionReason.values, _.key, "SuppressionReason")
  given Format[SemanticGate] = enumFormat(SemanticGate.values, _.key, "SemanticGate")
  given Format[SemanticRole] = enumFormat(SemanticRole.values, _.key, "SemanticRole")
  given Format[PublicClaimPredicate] =
    enumFormat(PublicClaimPredicate.values, _.key, "PublicClaimPredicate")
  given Format[VariationMoveRole] = enumFormat(VariationMoveRole.values, _.key, "VariationMoveRole")
  given Format[VariationProofPurpose] = enumFormat(VariationProofPurpose.values, _.key, "VariationProofPurpose")
  given Format[VariationTestResult] = enumFormat(VariationTestResult.values, _.key, "VariationTestResult")
  given Format[VariationSurfaceAllowance] =
    enumFormat(VariationSurfaceAllowance.values, _.key, "VariationSurfaceAllowance")

  given Format[EvidenceRef] = Json.format[EvidenceRef]
  given Format[RenderText] = Json.format[RenderText]
  given Format[RenderEvidenceRef] = Json.format[RenderEvidenceRef]
  given Format[RenderVariationMove] = Json.format[RenderVariationMove]
  given Format[RenderVariationEvidence] = Json.format[RenderVariationEvidence]
  given Format[RenderBoundary] = Json.format[RenderBoundary]
  given Format[RenderSuppression] = Json.format[RenderSuppression]
  given Format[RenderWording] = Json.format[RenderWording]
  given Format[PhraseCapability] = Json.format[PhraseCapability]
  given Format[RenderBlock] = Json.format[RenderBlock]
  given Format[CommentaryRender] = Json.format[CommentaryRender]
  given Format[SemanticDecisionSummary] = Json.format[SemanticDecisionSummary]
  given Format[SemanticShadowSummary] = Json.format[SemanticShadowSummary]

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
