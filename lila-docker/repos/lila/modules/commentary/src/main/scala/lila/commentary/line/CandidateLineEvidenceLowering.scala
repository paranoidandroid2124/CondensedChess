package lila.commentary.line

import lila.commentary.selection.*

object CandidateLineEvidenceLowering:

  final case class Policy(
      depthFloor: Int,
      preferredTargetDepth: Int,
      strongTargetDepth: Int,
      rootMultiPvDefaultTarget: Int,
      rootMultiPvMin: Int,
      rootMultiPvMax: Int,
      childMultiPvDefault: Int,
      childMultiPvMin: Int,
      childMultiPvMax: Int,
      childParentRankLimit: Int,
      allowThirdRootChildProof: Boolean
  )

  object Policy:
    val Default: Policy =
      Policy(
        depthFloor = 16,
        preferredTargetDepth = 18,
        strongTargetDepth = 20,
        rootMultiPvDefaultTarget = 3,
        rootMultiPvMin = 2,
        rootMultiPvMax = 3,
        childMultiPvDefault = 1,
        childMultiPvMin = 1,
        childMultiPvMax = 2,
        childParentRankLimit = 2,
        allowThirdRootChildProof = false
      )

  final case class Binding(
      boundClaimId: String,
      owner: String,
      defender: Option[String],
      anchor: String,
      route: String,
      scope: String,
      provenanceRef: EvidenceRef
  ):
    require(boundClaimId.trim.nonEmpty, "Lowering binding claim id must be non-empty")
    require(owner.trim.nonEmpty, "Lowering binding owner must be non-empty")
    require(anchor.trim.nonEmpty, "Lowering binding anchor must be non-empty")
    require(route.trim.nonEmpty, "Lowering binding route must be non-empty")
    require(scope.trim.nonEmpty, "Lowering binding scope must be non-empty")

  def lower(
      evidence: Vector[CandidateLineEvidence],
      binding: Binding,
      nowEpochMs: Long,
      policy: Policy = Policy.Default
  ): Vector[PreparedVariationEvidence] =
    if !publicSafeBinding(binding) then Vector.empty
    else
      val roots = acceptedRoots(evidence, nowEpochMs, policy)
      if roots.isEmpty then Vector.empty
      else
        val rootRanks = roots.map(root => root.branchId -> root.rank).toMap
        val rootProofs = roots.flatMap(root => lowerRoot(root, binding, policy))
        val childProofs =
          evidence
            .filter(isChild)
            .sortBy(line => (line.parentBranchId.map(_.value).getOrElse(""), line.rank, line.multiPvIndex, line.branchId.value))
            .flatMap(child => lowerChild(child, rootRanks, binding, nowEpochMs, policy))
        withPublicProofIds(rootProofs ++ childProofs, binding)

  private def acceptedRoots(
      evidence: Vector[CandidateLineEvidence],
      nowEpochMs: Long,
      policy: Policy
  ): Vector[CandidateLineEvidence] =
    val rootCandidates =
      evidence
        .filter(line => line.role == CandidateBranchRole.RootCandidate && line.parentBranchId.isEmpty)
        .sortBy(line => (line.rank, line.multiPvIndex, line.branchId.value))
    val rootMultiPvAccepted =
      rootCandidates.nonEmpty &&
        rootCandidates.forall(line => line.multiPv >= policy.rootMultiPvMin && line.multiPv <= policy.rootMultiPvMax) &&
        rootCandidates.map(_.multiPv).distinct.size == 1
    if !rootMultiPvAccepted then Vector.empty
    else
      val expectedRanks = (1 to rootCandidates.head.multiPv).toVector
      val expectedIndexes = expectedRanks
      val candidateRanks = rootCandidates.map(_.rank)
      val candidateIndexes = rootCandidates.map(_.multiPvIndex)
      val deterministicRootSet =
        rootCandidates.size == rootCandidates.head.multiPv &&
          candidateRanks.sorted == expectedRanks &&
          candidateIndexes.sorted == expectedIndexes
      val publicRoots =
        rootCandidates
          .filter(line => line.rank >= 1 && line.rank <= policy.rootMultiPvMax)
          .filter(_.publicLineEligibleAt(nowEpochMs, policy.depthFloor))
      val presentRanks = publicRoots.map(_.rank).distinct.sorted
      if deterministicRootSet && presentRanks == expectedRanks then publicRoots.take(policy.rootMultiPvMax) else Vector.empty

  private def lowerRoot(
      line: CandidateLineEvidence,
      binding: Binding,
      policy: Policy
  ): Option[PreparedVariationEvidence] =
    Option.when(line.rank >= 1 && line.rank <= policy.rootMultiPvMax):
      val proofToken =
        line.rank match
          case 1 => "main_candidate_line"
          case 2 => "alternative_candidate_line"
          case _ => "context_candidate_line"
      prepared(
        line = line,
        binding = binding,
        role = VariationEvidenceRole.Persistence,
        moveRole = VariationMoveRole.CandidateMove,
        testResult = VariationTestResult.PressurePersists,
        proofToken = proofToken,
        proofPurpose = VariationProofPurpose.PreservesPressure,
        policy = policy
      )

  private def lowerChild(
      line: CandidateLineEvidence,
      rootRanks: Map[CandidateBranchId, Int],
      binding: Binding,
      nowEpochMs: Long,
      policy: Policy
  ): Option[PreparedVariationEvidence] =
    val parentRank = line.parentBranchId.flatMap(rootRanks.get)
    val parentAllowed =
      parentRank.exists(rank => rank <= policy.childParentRankLimit || (rank == 3 && policy.allowThirdRootChildProof))
    Option
      .when(
        parentAllowed &&
          childRoleEligible(line.role) &&
          line.rank >= 1 &&
          line.rank <= policy.childMultiPvMax &&
          line.multiPv >= policy.childMultiPvMin &&
          line.multiPv <= policy.childMultiPvMax &&
          line.publicLineEligibleAt(nowEpochMs, policy.depthFloor)
      ):
        prepared(
          line = line,
          binding = binding,
          role = VariationEvidenceRole.DefenderResource,
          moveRole = VariationMoveRole.DefenderResource,
          testResult = VariationTestResult.DoesNotRestoreCounterplay,
          proofToken = "defender_resource_line",
          proofPurpose = VariationProofPurpose.DeniesResource,
          policy = policy
        )

  private def prepared(
      line: CandidateLineEvidence,
      binding: Binding,
      role: VariationEvidenceRole,
      moveRole: VariationMoveRole,
      testResult: VariationTestResult,
      proofToken: String,
      proofPurpose: VariationProofPurpose,
      policy: Policy
  ): PreparedVariationEvidence =
    val moves = toMoves(line)
    val firstMove = moves.headOption
    val continuation = moves.drop(1)
    val child = isChild(line)
    PreparedVariationEvidence(
      proofId = s"${binding.boundClaimId.trim}-line",
      boundClaimId = binding.boundClaimId,
      startFen = line.startFen,
      owner = binding.owner,
      defender = binding.defender,
      anchor = binding.anchor,
      route = binding.route,
      scope = binding.scope,
      role = role,
      moveRole = moveRole,
      lineSan = line.lineSan,
      lineUci = line.lineUci,
      playedMove = None,
      candidateMove = if child then None else firstMove,
      defenderResource = if child then firstMove else None,
      continuation = continuation,
      testedMove = firstMove,
      testedLine = moves,
      replyLine = if child then moves else Vector.empty,
      resourceLine = if child then moves else Vector.empty,
      testResult = testResult,
      proves = proofToken,
      proofPurpose = proofPurpose,
      provenanceRefs = Vector(binding.provenanceRef),
      boundary = PreparedVariationBoundary(
        depthFloor = policy.depthFloor,
        realizedDepth = line.realizedDepth,
        multiPv = line.multiPv,
        freshnessChecked = true,
        legalReplayChecked = true,
        baselineChecked = false
      ),
      wordingCap = WordingStrength.weaker(line.wordingCap, WordingStrength.QualifiedSupport),
      surfaceAllowance = line.surfaceAllowance,
      publicSafe = true,
      debug = None
    )

  private def toMoves(line: CandidateLineEvidence): Vector[VariationMove] =
    line.lineSan.zip(line.lineUci).map((san, uci) => VariationMove(san, uci))

  private def withPublicProofIds(
      proofs: Vector[PreparedVariationEvidence],
      binding: Binding
  ): Vector[PreparedVariationEvidence] =
    proofs.zipWithIndex.map((proof, index) => proof.copy(proofId = s"${binding.boundClaimId.trim}-line-${index + 1}"))

  private def isChild(line: CandidateLineEvidence): Boolean =
    line.parentBranchId.nonEmpty

  private def childRoleEligible(role: CandidateBranchRole): Boolean =
    role == CandidateBranchRole.DefenderResource || role == CandidateBranchRole.DefenderReply

  private def publicSafeBinding(binding: Binding): Boolean =
    val ref = binding.provenanceRef
    ref.kind != EvidenceRefKind.RawEngine &&
      ref.kind != EvidenceRefKind.SourceContext &&
      ref.owner.contains(binding.owner) &&
      ref.anchor.contains(binding.anchor) &&
      ref.route.contains(binding.route) &&
      ref.scope.contains(binding.scope)
