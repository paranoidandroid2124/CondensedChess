package lila.commentary.line

object CandidateRootProbeIntegration:

  final case class Input(
      currentFen: String,
      nodeId: String,
      ply: Int,
      variant: String,
      engineFingerprint: String,
      budget: CandidateProbeBudget = CandidateProbeBudget.Default,
      completedRootProbe: Option[CandidateProbeResultPayload],
      proofCache: Option[CandidateLineProofCache] = None,
      loweringBinding: Option[CandidateLineEvidenceLowering.Binding] = None,
      nowEpochMs: Long,
      loweringPolicy: CandidateLineEvidenceLowering.Policy = CandidateLineEvidenceLowering.Policy.Default
  ):
    require(currentFen.trim.nonEmpty, "Root probe integration current FEN must be non-empty")
    require(nodeId.trim.nonEmpty, "Root probe integration node id must be non-empty")
    require(ply >= 0, "Root probe integration ply must be non-negative")
    require(variant.trim.nonEmpty, "Root probe integration variant must be non-empty")
    require(engineFingerprint.trim.nonEmpty, "Root probe integration engine fingerprint must be non-empty")

  final case class Result(
      assembly: CandidateProbeControlledAdapter.AssemblyResult,
      plannedChildRequests: Vector[CandidateProbeRequest],
      cacheWrites: Vector[CandidateProbeCacheWriteCandidate]
  )

  def integrate(input: Input): Result =
    val assembly =
      CandidateLineAssemblyProvider.assemble(
        CandidateLineAssemblyProvider.Input(
          currentFen = input.currentFen,
          nodeId = input.nodeId,
          ply = input.ply,
          variant = input.variant,
          engineFingerprint = input.engineFingerprint,
          budget = input.budget,
          completedRootProbe = input.completedRootProbe,
          completedChildProbes = Vector.empty,
          loweringBinding = input.loweringBinding,
          nowEpochMs = input.nowEpochMs,
          loweringPolicy = input.loweringPolicy,
          proofCache = input.proofCache
        )
      )
    Result(
      assembly = assembly,
      plannedChildRequests = childRequestsNeeded(input, assembly),
      cacheWrites = assembly.cacheWrites
    )

  private def childRequestsNeeded(
      input: Input,
      assembly: CandidateProbeControlledAdapter.AssemblyResult
  ): Vector[CandidateProbeRequest] =
    if assembly.candidateLineEvidence.isEmpty then Vector.empty
    else
      val schedulingBudget = input.budget.copy(allowThirdRootChildFromCache = false)
      val satisfiedParents =
        assembly.candidateLineEvidence.collect:
          case line
              if line.parentBranchId.nonEmpty &&
                childRole(line.role) &&
                line.publicLineEligibleAt(input.nowEpochMs, input.budget.floorDepth) =>
            line.parentBranchId.get
      CandidateProbePlan
        .childStage(
          rootEvidence = assembly.candidateLineEvidence,
          budget = schedulingBudget,
          nowEpochMs = input.nowEpochMs
        )
        .requests
        .filter(_.probeSource == CandidateProbeSource.EngineProbe)
        .filterNot(request => request.parentBranchId.exists(satisfiedParents.toSet.contains))

  private def childRole(role: CandidateBranchRole): Boolean =
    role == CandidateBranchRole.DefenderResource || role == CandidateBranchRole.DefenderReply
