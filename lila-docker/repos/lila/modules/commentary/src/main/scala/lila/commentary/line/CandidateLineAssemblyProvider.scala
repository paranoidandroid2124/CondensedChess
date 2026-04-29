package lila.commentary.line

object CandidateLineAssemblyProvider:

  final case class Input(
      currentFen: String,
      nodeId: String,
      ply: Int,
      variant: String,
      engineFingerprint: String,
      budget: CandidateProbeBudget,
      completedRootProbe: Option[CandidateProbeResultPayload],
      completedChildProbes: Vector[CandidateProbeResultPayload],
      loweringBinding: Option[CandidateLineEvidenceLowering.Binding],
      nowEpochMs: Long,
      loweringPolicy: CandidateLineEvidenceLowering.Policy = CandidateLineEvidenceLowering.Policy.Default,
      proofCache: Option[CandidateLineProofCache] = None,
      commitProofCacheWrites: Boolean = true
  ):
    require(currentFen.trim.nonEmpty, "Candidate assembly current FEN must be non-empty")
    require(nodeId.trim.nonEmpty, "Candidate assembly node id must be non-empty")
    require(ply >= 0, "Candidate assembly ply must be non-negative")
    require(variant.trim.nonEmpty, "Candidate assembly variant must be non-empty")
    require(engineFingerprint.trim.nonEmpty, "Candidate assembly engine fingerprint must be non-empty")

  def assemble(input: Input): CandidateProbeControlledAdapter.AssemblyResult =
    val rootRequest =
      CandidateProbePlan
        .rootStage(
          startFen = input.currentFen,
          nodeId = input.nodeId,
          ply = input.ply,
          variant = input.variant,
          engineFingerprint = input.engineFingerprint,
          budget = input.budget
        )
        .requests
        .head
    val rootCacheHit = input.proofCache.flatMap(_.read(rootRequest, input.nowEpochMs))
    val rootEvidence =
      CandidateProbeControlledAdapter
        .assemble(
          CandidateProbeControlledAdapter.Input(
            currentFen = input.currentFen,
            nodeId = input.nodeId,
            ply = input.ply,
            variant = input.variant,
            rootRequest = rootRequest,
            rootProbeResult = input.completedRootProbe,
            rootCacheHit = rootCacheHit,
            childRequests = Vector.empty,
            childProbeResults = Vector.empty,
            childCacheHits = Vector.empty,
            loweringBinding = None,
            nowEpochMs = input.nowEpochMs,
            budget = input.budget,
            loweringPolicy = input.loweringPolicy
          )
        )
        .candidateLineEvidence
    if rootEvidence.isEmpty then CandidateProbeControlledAdapter.AssemblyResult.Empty
    else
      val proofCacheChildHits = childCacheHitsFromProofCache(input, rootEvidence)
      val childCacheHits = proofCacheChildHits
      val childRequests =
        CandidateProbePlan.childStage(
          rootEvidence = rootEvidence,
          budget = input.budget,
          validCacheEntries = childCacheHits.map(_.entry),
          nowEpochMs = input.nowEpochMs
        ).requests
      val result = CandidateProbeControlledAdapter.assemble(
        CandidateProbeControlledAdapter.Input(
          currentFen = input.currentFen,
          nodeId = input.nodeId,
          ply = input.ply,
          variant = input.variant,
          rootRequest = rootRequest,
          rootProbeResult = input.completedRootProbe,
          rootCacheHit = rootCacheHit,
          childRequests = childRequests,
          childProbeResults = input.completedChildProbes,
          childCacheHits = childCacheHits,
          loweringBinding = input.loweringBinding,
          nowEpochMs = input.nowEpochMs,
          budget = input.budget,
          loweringPolicy = input.loweringPolicy
        )
      )
      if input.commitProofCacheWrites then
        input.proofCache.foreach: cache =>
          cache.commit(result.cacheWriteReceipt, input.nowEpochMs)
      result

  private def childCacheHitsFromProofCache(
      input: Input,
      rootEvidence: Vector[CandidateLineEvidence]
  ): Vector[CandidateProbeCacheLookupResult] =
    input.proofCache.toVector.flatMap: cache =>
      val lookupBudget =
        input.budget.copy(allowExpandedThirdRootChildProbe = true)
      val lookupRequests =
        CandidateProbePlan.childStage(
          rootEvidence = rootEvidence,
          budget = lookupBudget,
          nowEpochMs = input.nowEpochMs
        ).requests
      lookupRequests.flatMap: request =>
        val effectiveRequest =
          if request.parentRootRank.contains(3) &&
            request.parentRootRank.exists(_ > input.budget.childRootRankLimit) &&
            !input.budget.allowExpandedThirdRootChildProbe
          then request.copy(targetDepth = input.budget.strongCacheTargetDepth)
          else request
        cache.read(effectiveRequest, input.nowEpochMs)
