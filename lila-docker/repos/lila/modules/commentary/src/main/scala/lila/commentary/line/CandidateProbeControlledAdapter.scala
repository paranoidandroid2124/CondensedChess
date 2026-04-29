package lila.commentary.line

import lila.commentary.selection.PreparedVariationEvidence

final case class CandidateProbeResultLine(
    branchId: CandidateBranchId,
    rank: Int,
    multiPvIndex: Int,
    uciLine: Vector[String]
):
  require(uciLine.forall(_.trim.nonEmpty), "Probe result UCI entries must be non-empty")

final case class CandidateProbeResultPayload(
    request: CandidateProbeRequest,
    lines: Vector[CandidateProbeResultLine],
    realizedDepth: Int,
    generatedAtEpochMs: Long,
    maxAgeMs: Long,
    completed: Boolean = true
):
  require(realizedDepth >= 0, "Probe realized depth must be non-negative")

final case class CandidateProbeCacheLookupResult(
    request: CandidateProbeRequest,
    entry: CandidateProbeCacheEntry,
    payload: CandidateProbeResultPayload
)

final case class CandidateProbeCacheWriteCandidate(
    key: CandidateProbeCacheKey,
    writeSource: CandidateProbeCacheWriteSource,
    payload: CandidateProbeResultPayload
)

object CandidateProbeControlledAdapter:

  final case class Input(
      currentFen: String,
      nodeId: String,
      ply: Int,
      variant: String,
      rootRequest: CandidateProbeRequest,
      rootProbeResult: Option[CandidateProbeResultPayload],
      rootCacheHit: Option[CandidateProbeCacheLookupResult],
      childRequests: Vector[CandidateProbeRequest],
      childProbeResults: Vector[CandidateProbeResultPayload],
      childCacheHits: Vector[CandidateProbeCacheLookupResult],
      loweringBinding: Option[CandidateLineEvidenceLowering.Binding],
      nowEpochMs: Long,
      budget: CandidateProbeBudget = CandidateProbeBudget.Default,
      loweringPolicy: CandidateLineEvidenceLowering.Policy = CandidateLineEvidenceLowering.Policy.Default
  )

  final case class AssemblyResult(
      rootPacket: Option[CandidateLinePacket],
      childPackets: Vector[CandidateLineChildPacket],
      candidateLineEvidence: Vector[CandidateLineEvidence],
      preparedVariationEvidence: Vector[PreparedVariationEvidence],
      cacheWrites: Vector[CandidateProbeCacheWriteCandidate],
      cacheWriteReceipt: CacheWriteReceipt = CacheWriteReceipt.Empty
  )

  sealed trait CacheWriteReceipt:
    private[line] def candidates: Vector[CandidateProbeCacheWriteCandidate]

  private final class AdapterCacheWriteReceipt(
      private[line] val candidates: Vector[CandidateProbeCacheWriteCandidate]
  ) extends CacheWriteReceipt

  object CacheWriteReceipt:
    val Empty: CacheWriteReceipt = AdapterCacheWriteReceipt(Vector.empty)
    private[line] def fromCandidates(candidates: Vector[CandidateProbeCacheWriteCandidate]): CacheWriteReceipt =
      AdapterCacheWriteReceipt(candidates)

  object AssemblyResult:
    val Empty: AssemblyResult =
      AssemblyResult(
        rootPacket = None,
        childPackets = Vector.empty,
        candidateLineEvidence = Vector.empty,
        preparedVariationEvidence = Vector.empty,
        cacheWrites = Vector.empty,
        cacheWriteReceipt = CacheWriteReceipt.Empty
      )

  def assemble(input: Input): AssemblyResult =
    val rootSource = rootProbeSource(input).orElse(rootCacheSource(input))
    val rootPacket = rootSource.flatMap(source => rootPacketFrom(source.payload, source.provenanceKind))
    rootPacket match
      case None => AssemblyResult.Empty
      case Some(root) =>
        val rootEvidence =
          CandidateLinePacketHandoff.normalize(
            CandidateLinePacketHandoffInput(
              currentFen = input.currentFen,
              nodeId = input.nodeId,
              ply = input.ply,
              root = root
            ),
            nowEpochMs = input.nowEpochMs
          )
        val validRoots = rootEvidence.filter(line =>
          line.role == CandidateBranchRole.RootCandidate &&
            line.parentBranchId.isEmpty &&
            line.publicLineEligibleAt(input.nowEpochMs, input.budget.floorDepth)
        )
        if validRoots.isEmpty then AssemblyResult.Empty
        else
          val rootRanks = validRoots.map(line => line.branchId -> line.rank).toMap
          val acceptedChildProbeSources = childProbeSources(input, rootRanks)
          val childSources =
            acceptedChildProbeSources ++ childCacheSources(
              input,
              rootRanks,
              probedRequests = acceptedChildProbeSources.map(_.payload.request)
            )
          assembleWithChildren(input, root, rootSource, childSources)

  private def assembleWithChildren(
      input: Input,
      root: CandidateLinePacket,
      rootSource: Option[AcceptedSource],
      childSources: Vector[AcceptedSource]
  ): AssemblyResult =
        val childPackets = childSources.flatMap(source => childPacketFrom(source.payload, source.provenanceKind))
        val evidence =
          CandidateLinePacketHandoff.normalize(
            CandidateLinePacketHandoffInput(
              currentFen = input.currentFen,
              nodeId = input.nodeId,
              ply = input.ply,
              root = root,
              children = childPackets
            ),
            nowEpochMs = input.nowEpochMs
          )
        val validRoots = evidence.filter(line =>
          line.role == CandidateBranchRole.RootCandidate &&
            line.parentBranchId.isEmpty &&
            line.publicLineEligibleAt(input.nowEpochMs, input.budget.floorDepth)
        )
        if validRoots.isEmpty then AssemblyResult.Empty
        else
          val prepared =
            input.loweringBinding.fold(Vector.empty): binding =>
              CandidateLineEvidenceLowering.lower(
                evidence = evidence,
                binding = binding,
                nowEpochMs = input.nowEpochMs,
                policy = input.loweringPolicy
              )
          val writes =
            cacheWritesForAcceptedProbeSources(
              sources = rootSource.toVector ++ childSources.filter(_.fromProbe),
              root = root,
              input = input
            )
          AssemblyResult(
            rootPacket = Some(root),
            childPackets = childPackets,
            candidateLineEvidence = evidence,
            preparedVariationEvidence = prepared,
            cacheWrites = writes,
            cacheWriteReceipt = cacheWriteReceipt(writes)
          )

  def rootPacketFrom(
      payload: CandidateProbeResultPayload,
      provenanceKind: CandidateLineProvenanceKind
  ): Option[CandidateLinePacket] =
    Option.when(payload.completed && payload.request.role == CandidateProbeRole.RootCandidate && payload.lines.nonEmpty):
      CandidateLinePacket(
        startFen = payload.request.startFen,
        nodeId = payload.request.nodeId,
        ply = payload.request.ply,
        rawLines = rawLinesFrom(payload, provenanceKind),
        requestedDepth = payload.request.targetDepth,
        realizedDepth = payload.realizedDepth,
        multiPv = payload.request.multiPv,
        generatedAtEpochMs = payload.generatedAtEpochMs,
        maxAgeMs = payload.maxAgeMs,
        engineConfigFingerprint = payload.request.engineFingerprint
      )

  private def childPacketFrom(
      payload: CandidateProbeResultPayload,
      provenanceKind: CandidateLineProvenanceKind
  ): Option[CandidateLineChildPacket] =
    Option.when(
      payload.completed &&
        payload.request.role.branchRole != CandidateBranchRole.RootCandidate &&
        payload.request.parentBranchId.nonEmpty &&
        payload.lines.nonEmpty
    ):
      CandidateLineChildPacket(
        parentBranchId = payload.request.parentBranchId.get,
        startFen = payload.request.startFen,
        nodeId = payload.request.nodeId,
        ply = payload.request.ply,
        role = payload.request.role.branchRole,
        parentLinePrefix = payload.request.parentLinePrefix,
        rawLines = rawLinesFrom(payload, provenanceKind),
        requestedDepth = payload.request.targetDepth,
        realizedDepth = payload.realizedDepth,
        multiPv = payload.request.multiPv,
        generatedAtEpochMs = payload.generatedAtEpochMs,
        maxAgeMs = payload.maxAgeMs,
        engineConfigFingerprint = payload.request.engineFingerprint
      )

  private def rawLinesFrom(
      payload: CandidateProbeResultPayload,
      provenanceKind: CandidateLineProvenanceKind
  ): Vector[CandidateLineRawLine] =
    payload.lines.map: line =>
      CandidateLineRawLine(
        branchId = line.branchId,
        parentBranchId = payload.request.parentBranchId,
        role = payload.request.role.branchRole,
        rank = line.rank,
        multiPvIndex = line.multiPvIndex,
        uciLine = line.uciLine.map(_.trim.toLowerCase),
        provenance = CandidateLineProvenance(provenanceKind)
      )

  private def rootProbeSource(input: Input): Option[AcceptedSource] =
    input.rootProbeResult
      .filter(payload => sameRequest(payload.request, input.rootRequest))
      .filter(payload => rootRequestMatchesBudget(payload.request, input.budget))
      .filter(payload => rootPayloadMatchesBudget(payload, input.budget))
      .map(payload => AcceptedSource(payload, CandidateLineProvenanceKind.EngineRoot, fromProbe = true))

  private def rootCacheSource(input: Input): Option[AcceptedSource] =
    input.rootCacheHit
      .filter(hit => sameRequest(hit.request, input.rootRequest))
      .filter(hit => rootRequestMatchesBudget(hit.request, input.budget))
      .filter(hit => hit.entry.revalidateFor(input.rootRequest, input.nowEpochMs).accepted)
      .filter(hit => sameRequest(hit.payload.request, hit.request))
      .filter(cachePayloadMatchesEntry)
      .filter(hit => rootPayloadMatchesBudget(hit.payload, input.budget))
      .map(hit => AcceptedSource(hit.payload, CandidateLineProvenanceKind.Cache, fromProbe = false))

  private def rootRequestMatchesBudget(request: CandidateProbeRequest, budget: CandidateProbeBudget): Boolean =
    request.role == CandidateProbeRole.RootCandidate && request.multiPv == budget.rootMultiPv

  private def rootPayloadMatchesBudget(payload: CandidateProbeResultPayload, budget: CandidateProbeBudget): Boolean =
    val expected = (1 to budget.rootMultiPv).toVector
    payload.lines.size == budget.rootMultiPv &&
      payload.lines.map(line => line.rank -> line.multiPvIndex).sortBy(_._1) == expected.map(index => index -> index)

  private def childProbeSources(
      input: Input,
      rootRanks: Map[CandidateBranchId, Int]
  ): Vector[AcceptedSource] =
    input.childProbeResults
      .filter(payload => input.childRequests.exists(request => sameRequest(payload.request, request)))
      .filter(payload => childProbePayloadAccepted(payload, input, rootRanks))
      .map(payload => AcceptedSource(payload, CandidateLineProvenanceKind.EngineChild, fromProbe = true))

  private def childCacheSources(
      input: Input,
      rootRanks: Map[CandidateBranchId, Int],
      probedRequests: Vector[CandidateProbeRequest]
  ): Vector[AcceptedSource] =
    input.childCacheHits
      .filter(hit => !probedRequests.exists(request => sameRequest(request, hit.request)))
      .filter(hit => cacheHitAccepted(hit, input, rootRanks))
      .map(hit => AcceptedSource(hit.payload, CandidateLineProvenanceKind.Cache, fromProbe = false))

  private def childProbePayloadAccepted(
      payload: CandidateProbeResultPayload,
      input: Input,
      rootRanks: Map[CandidateBranchId, Int]
  ): Boolean =
    val request = payload.request
    val actualParentRank = request.parentBranchId.flatMap(rootRanks.get)
    request.probeSource == CandidateProbeSource.EngineProbe &&
      request.multiPv == input.budget.childMultiPv &&
      actualParentRank == request.parentRootRank &&
      actualParentRank.exists(childProbeParentRankAllowed(_, input.budget)) &&
      childPayloadMatchesBudget(payload, input.budget)

  private def childProbeParentRankAllowed(rank: Int, budget: CandidateProbeBudget): Boolean =
    val defaultAllowed = rank >= 1 && rank <= budget.childRootRankLimit
    val expandedAllowed = rank == 3 && budget.allowExpandedThirdRootChildProbe
    defaultAllowed || expandedAllowed

  private def cacheHitAccepted(
      hit: CandidateProbeCacheLookupResult,
      input: Input,
      rootRanks: Map[CandidateBranchId, Int]
  ): Boolean =
    val actualParentRank = hit.request.parentBranchId.flatMap(rootRanks.get)
    val rank3 = actualParentRank.contains(3)
    val rank3NeedsStrongCache =
      rank3 && !input.budget.allowExpandedThirdRootChildProbe && actualParentRank.exists(_ > input.budget.childRootRankLimit)
    val effectiveRequest =
      if rank3NeedsStrongCache then
        hit.request.copy(targetDepth = input.budget.strongCacheTargetDepth)
      else hit.request
    actualParentRank == hit.request.parentRootRank &&
      hit.request.multiPv == input.budget.childMultiPv &&
      actualParentRank.exists(cacheParentRankAllowed(_, input.budget)) &&
      hit.entry.revalidateFor(effectiveRequest, input.nowEpochMs).accepted &&
      sameRequest(hit.request, effectiveRequest) &&
      sameRequest(hit.payload.request, effectiveRequest) &&
      childPayloadMatchesBudget(hit.payload, input.budget) &&
      cachePayloadMatchesEntry(hit)

  private def childPayloadMatchesBudget(payload: CandidateProbeResultPayload, budget: CandidateProbeBudget): Boolean =
    val expected = (1 to budget.childMultiPv).toVector
    payload.lines.size == budget.childMultiPv &&
      payload.lines.map(line => line.rank -> line.multiPvIndex).sortBy(_._1) == expected.map(index => index -> index)

  private def cachePayloadMatchesEntry(hit: CandidateProbeCacheLookupResult): Boolean =
    CandidateProbeCacheKey.fromRequest(
      request = hit.payload.request,
      realizedDepth = hit.payload.realizedDepth,
      generatedAtEpochMs = hit.payload.generatedAtEpochMs,
      maxAgeMs = hit.payload.maxAgeMs
    ) == hit.entry.key

  private def cacheParentRankAllowed(rank: Int, budget: CandidateProbeBudget): Boolean =
    val defaultAllowed = rank >= 1 && rank <= budget.childRootRankLimit
    val expandedAllowed = rank == 3 && budget.allowExpandedThirdRootChildProbe
    val cachedRank3Allowed = rank == 3 && budget.allowThirdRootChildFromCache
    defaultAllowed || expandedAllowed || cachedRank3Allowed

  private def cacheWritesForAcceptedProbeSources(
      sources: Vector[AcceptedSource],
      root: CandidateLinePacket,
      input: Input
  ): Vector[CandidateProbeCacheWriteCandidate] =
    sources
      .filter(_.fromProbe)
      .filter(sourceAccepted(_, root, input))
      .map: source =>
        CandidateProbeCacheWriteCandidate(
          key = CandidateProbeCacheKey.fromRequest(
            request = source.payload.request,
            realizedDepth = source.payload.realizedDepth,
            generatedAtEpochMs = source.payload.generatedAtEpochMs,
            maxAgeMs = source.payload.maxAgeMs
          ),
          writeSource = CandidateProbeCacheWriteSource.EngineProbe,
          payload = source.payload
        )

  private def cacheWriteReceipt(candidates: Vector[CandidateProbeCacheWriteCandidate]): CacheWriteReceipt =
    CacheWriteReceipt.fromCandidates(candidates)

  private def sourceAccepted(
      source: AcceptedSource,
      root: CandidateLinePacket,
      input: Input
  ): Boolean =
    val normalized =
      if source.payload.request.role == CandidateProbeRole.RootCandidate then
        rootPacketFrom(source.payload, source.provenanceKind).toVector.flatMap: packet =>
          CandidateLinePacketHandoff.normalize(
            CandidateLinePacketHandoffInput(
              currentFen = input.currentFen,
              nodeId = input.nodeId,
              ply = input.ply,
              root = packet
            ),
            nowEpochMs = input.nowEpochMs
          )
      else
        childPacketFrom(source.payload, source.provenanceKind).toVector.flatMap: packet =>
          CandidateLinePacketHandoff.normalize(
            CandidateLinePacketHandoffInput(
              currentFen = input.currentFen,
              nodeId = input.nodeId,
              ply = input.ply,
              root = root,
              children = Vector(packet)
            ),
            nowEpochMs = input.nowEpochMs
          ).filter(line => line.parentBranchId.nonEmpty && line.role == source.payload.request.role.branchRole)
    payloadEvidenceAccepted(source.payload, normalized, input)

  private def payloadEvidenceAccepted(
      payload: CandidateProbeResultPayload,
      evidence: Vector[CandidateLineEvidence],
      input: Input
  ): Boolean =
    val expectedLines = payload.lines.map(line => (line.branchId, line.rank, line.multiPvIndex)).toSet
    val matching =
      evidence.filter(line =>
        expectedLines.contains((line.branchId, line.rank, line.multiPvIndex)) &&
          evidenceMatchesRequest(line, payload.request)
      )
    expectedLines.nonEmpty &&
      matching.size == expectedLines.size &&
      matching.forall(_.publicLineEligibleAt(input.nowEpochMs, input.budget.floorDepth))

  private def evidenceMatchesRequest(
      line: CandidateLineEvidence,
      request: CandidateProbeRequest
  ): Boolean =
    line.parentBranchId == request.parentBranchId &&
      line.role == request.role.branchRole &&
      line.startNodeId == request.nodeId &&
      line.startPly == request.ply &&
      CandidateProbePlan.normalizeFen(line.startFen) == CandidateProbePlan.normalizeFen(request.startFen) &&
      line.engineConfigFingerprint == request.engineFingerprint &&
      line.multiPv == request.multiPv

  private def sameRequest(left: CandidateProbeRequest, right: CandidateProbeRequest): Boolean =
    left == right

  private final case class AcceptedSource(
      payload: CandidateProbeResultPayload,
      provenanceKind: CandidateLineProvenanceKind,
      fromProbe: Boolean
  )
