package lila.commentary.line

object CandidateChildProbeIntegration:

  final case class Input(
      currentFen: String,
      nodeId: String,
      ply: Int,
      variant: String,
      engineFingerprint: String,
      budget: CandidateProbeBudget = CandidateProbeBudget.Default,
      completedRootProbe: Option[CandidateProbeResultPayload],
      completedChildProbes: Vector[CandidateProbeResultPayload] = Vector.empty,
      proofCache: Option[CandidateLineProofCache] = None,
      loweringBinding: Option[CandidateLineEvidenceLowering.Binding] = None,
      nowEpochMs: Long,
      loweringPolicy: CandidateLineEvidenceLowering.Policy = CandidateLineEvidenceLowering.Policy.Default
  ):
    require(currentFen.trim.nonEmpty, "Child probe integration current FEN must be non-empty")
    require(nodeId.trim.nonEmpty, "Child probe integration node id must be non-empty")
    require(ply >= 0, "Child probe integration ply must be non-negative")
    require(variant.trim.nonEmpty, "Child probe integration variant must be non-empty")
    require(engineFingerprint.trim.nonEmpty, "Child probe integration engine fingerprint must be non-empty")

  final case class Result(
      assembly: CandidateProbeControlledAdapter.AssemblyResult,
      satisfiedChildRequests: Vector[CandidateProbeRequest],
      unsatisfiedChildRequests: Vector[CandidateProbeRequest],
      cacheWrites: Vector[CandidateProbeCacheWriteCandidate]
  )

  def integrate(input: Input): Result =
    val rawAssembly =
      CandidateLineAssemblyProvider.assemble(
        CandidateLineAssemblyProvider.Input(
          currentFen = input.currentFen,
          nodeId = input.nodeId,
          ply = input.ply,
          variant = input.variant,
          engineFingerprint = input.engineFingerprint,
          budget = input.budget,
          completedRootProbe = input.completedRootProbe,
          completedChildProbes = input.completedChildProbes,
          loweringBinding = input.loweringBinding,
          nowEpochMs = input.nowEpochMs,
          loweringPolicy = input.loweringPolicy,
          proofCache = input.proofCache,
          commitProofCacheWrites = false
        )
      )
    if rawAssembly.candidateLineEvidence.isEmpty then
      Result(CandidateProbeControlledAdapter.AssemblyResult.Empty, Vector.empty, Vector.empty, Vector.empty)
    else
      val filteredAssembly = withOnlyCompleteAcceptedChildren(rawAssembly, input)
      input.proofCache.foreach: cache =>
        cache.commit(filteredAssembly.cacheWriteReceipt, input.nowEpochMs)
      val planned = plannedLiveChildRequests(filteredAssembly, input)
      val satisfied = planned.filter(requestSatisfied(_, filteredAssembly.candidateLineEvidence, input))
      Result(
        assembly = filteredAssembly,
        satisfiedChildRequests = satisfied,
        unsatisfiedChildRequests = planned.filterNot(satisfied.toSet),
        cacheWrites = filteredAssembly.cacheWrites
      )

  private def withOnlyCompleteAcceptedChildren(
      assembly: CandidateProbeControlledAdapter.AssemblyResult,
      input: Input
  ): CandidateProbeControlledAdapter.AssemblyResult =
    val rootEvidence = assembly.candidateLineEvidence.filter(line => line.role == CandidateBranchRole.RootCandidate && line.parentBranchId.isEmpty)
    val acceptedChildGroups = completeAcceptedChildGroups(assembly, input)
    val filteredEvidence =
      rootEvidence ++ acceptedChildGroups.flatMap(_.evidence)
    val filteredChildPackets = acceptedChildGroups.map(_.packet)
    val filteredWrites =
      assembly.cacheWrites.filter(writeAccepted(_, filteredEvidence, input, acceptedChildGroups))
    val filteredPrepared =
      input.loweringBinding.fold(Vector.empty): binding =>
        CandidateLineEvidenceLowering.lower(
          evidence = filteredEvidence,
          binding = binding,
          nowEpochMs = input.nowEpochMs,
          policy = input.loweringPolicy
        )
    assembly.copy(
      childPackets = filteredChildPackets,
      candidateLineEvidence = filteredEvidence,
      preparedVariationEvidence = filteredPrepared,
      cacheWrites = filteredWrites,
      cacheWriteReceipt = CandidateProbeControlledAdapter.CacheWriteReceipt.fromCandidates(filteredWrites)
    )

  private def completeAcceptedChildGroups(
      assembly: CandidateProbeControlledAdapter.AssemblyResult,
      input: Input
  ): Vector[AcceptedChildGroup] =
    assembly.rootPacket.toVector
      .flatMap: root =>
        assembly.childPackets.zipWithIndex.flatMap: (packet, originIndex) =>
          completeAcceptedChildGroup(root, packet, input, originIndex)
      .groupBy(_.key)
      .values
      .toVector
      .map(_.minBy(_.originIndex))
      .sortBy(_.originIndex)

  private def completeAcceptedChildGroup(
      root: CandidateLinePacket,
      packet: CandidateLineChildPacket,
      input: Input,
      originIndex: Int
  ): Option[AcceptedChildGroup] =
    val childEvidence =
      CandidateLinePacketHandoff
        .normalize(
          CandidateLinePacketHandoffInput(
            currentFen = input.currentFen,
            nodeId = input.nodeId,
            ply = input.ply,
            root = root,
            children = Vector(packet)
          ),
          nowEpochMs = input.nowEpochMs
        )
        .filter(line => line.parentBranchId.nonEmpty && childRole(line.role))
        .sortBy(line => (line.rank, line.multiPvIndex, line.branchId.value))
    Option.when(completeChildGroup(childEvidence, input))(AcceptedChildGroup(packet, childEvidence, originIndex))

  private def completeChildGroup(
      group: Vector[CandidateLineEvidence],
      input: Input
  ): Boolean =
    group.size == input.budget.childMultiPv &&
      group.forall(_.publicLineEligibleAt(input.nowEpochMs, input.budget.floorDepth)) &&
      group.map(line => line.rank -> line.multiPvIndex) == (1 to input.budget.childMultiPv).toVector.map(index => index -> index)

  private def plannedLiveChildRequests(
      assembly: CandidateProbeControlledAdapter.AssemblyResult,
      input: Input
  ): Vector[CandidateProbeRequest] =
    val schedulingBudget = input.budget.copy(allowThirdRootChildFromCache = false)
    CandidateProbePlan
      .childStage(
        rootEvidence = assembly.candidateLineEvidence,
        budget = schedulingBudget,
        nowEpochMs = input.nowEpochMs
      )
      .requests
      .filter(_.probeSource == CandidateProbeSource.EngineProbe)

  private def requestSatisfied(
      request: CandidateProbeRequest,
      evidence: Vector[CandidateLineEvidence],
      input: Input
  ): Boolean =
    val children =
      evidence.filter(line =>
        line.parentBranchId == request.parentBranchId &&
          line.role == request.role.branchRole &&
          line.startNodeId == request.nodeId &&
          line.startPly == request.ply &&
          CandidateProbePlan.normalizeFen(line.startFen) == CandidateProbePlan.normalizeFen(request.startFen) &&
          line.engineConfigFingerprint == request.engineFingerprint &&
          line.multiPv == request.multiPv &&
          line.publicLineEligibleAt(input.nowEpochMs, input.budget.floorDepth)
      )
    children.size == request.multiPv &&
      children.sortBy(_.rank).map(line => line.rank -> line.multiPvIndex) == (1 to request.multiPv).toVector.map(index => index -> index)

  private def writeAccepted(
      write: CandidateProbeCacheWriteCandidate,
      evidence: Vector[CandidateLineEvidence],
      input: Input,
      acceptedChildGroups: Vector[AcceptedChildGroup]
  ): Boolean =
    if write.key.role == CandidateProbeRole.RootCandidate then true
    else
      acceptedChildGroups.exists: group =>
        writePayloadMatchesGroup(write.payload, group) &&
          requestSatisfied(write.payload.request, evidence, input)

  private def writePayloadMatchesGroup(
      payload: CandidateProbeResultPayload,
      group: AcceptedChildGroup
  ): Boolean =
    val request = payload.request
    request.parentBranchId.contains(group.packet.parentBranchId) &&
      request.role.branchRole == group.packet.role &&
      request.nodeId == group.packet.nodeId &&
      request.ply == group.packet.ply &&
      CandidateProbePlan.normalizeFen(request.startFen) == CandidateProbePlan.normalizeFen(group.packet.startFen) &&
      request.engineFingerprint == group.packet.engineConfigFingerprint &&
      request.multiPv == group.packet.multiPv &&
      payload.lines.map(_.branchId).toSet == group.branchIds

  private def childRole(role: CandidateBranchRole): Boolean =
    role == CandidateBranchRole.DefenderResource || role == CandidateBranchRole.DefenderReply

  private final case class AcceptedChildGroup(
      packet: CandidateLineChildPacket,
      evidence: Vector[CandidateLineEvidence],
      originIndex: Int
  ):
    val branchIds: Set[CandidateBranchId] = evidence.map(_.branchId).toSet
    val key: ChildGroupKey =
      ChildGroupKey(
        parentBranchId = packet.parentBranchId,
        role = packet.role,
        nodeId = packet.nodeId,
        ply = packet.ply,
        startFen = CandidateProbePlan.normalizeFen(packet.startFen).getOrElse(packet.startFen.trim),
        engineFingerprint = packet.engineConfigFingerprint,
        multiPv = packet.multiPv
      )

  private final case class ChildGroupKey(
      parentBranchId: CandidateBranchId,
      role: CandidateBranchRole,
      nodeId: String,
      ply: Int,
      startFen: String,
      engineFingerprint: String,
      multiPv: Int
  )
