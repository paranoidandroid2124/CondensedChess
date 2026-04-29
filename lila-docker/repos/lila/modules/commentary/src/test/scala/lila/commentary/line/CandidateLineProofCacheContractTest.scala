package lila.commentary.line

import lila.commentary.selection.*

class CandidateLineProofCacheContractTest extends munit.FunSuite:

  private val initialFen = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1"
  private val nodeId = "mainline:0"
  private val nowEpochMs = 20_000L
  private val engine = "stockfish-proof-cache-contract"

  private lazy val rootRequest: CandidateProbeRequest =
    CandidateProbePlan.rootStage(initialFen, nodeId, 0, "standard", engine).requests.head

  private lazy val rootPayload: CandidateProbeResultPayload =
    CandidateProbeResultPayload(
      request = rootRequest,
      lines = Vector(
        payloadLine("root-candidate-1", Vector("e2e4", "e7e5"), 1, 1),
        payloadLine("root-candidate-2", Vector("d2d4", "d7d5"), 2, 2),
        payloadLine("root-candidate-3", Vector("g1f3", "g8f6"), 3, 3)
      ),
      realizedDepth = 18,
      generatedAtEpochMs = 10_000L,
      maxAgeMs = 20_000L
    )

  test("accepted adapter cache write candidates can be stored and read as typed lookup results"):
    val assembly = providerAssembly(completedRootProbe = Some(rootPayload))
    val cache = CandidateLineProofCache.InMemory.empty

    assertEquals(cache.commit(assembly.cacheWriteReceipt, nowEpochMs).map(_.stored), Vector(true))
    val lookup = cache.read(rootRequest, nowEpochMs)

    assertEquals(lookup.map(_.request), Some(rootRequest))
    assertEquals(lookup.map(_.payload), Some(rootPayload))
    assertEquals(lookup.exists(_.entry.revalidateFor(rootRequest, nowEpochMs).accepted), true)

  test("arbitrary coherent EngineProbe write candidates cannot be committed without adapter receipt"):
    val errors =
      compileErrors(
        "CandidateLineProofCache.InMemory.empty.write(writeCandidate(rootRequest, rootPayload), nowEpochMs)"
      )

    assert(errors.contains("write") || errors.contains("CandidateProbeCacheWriteCandidate"), clues(errors))

  test("read misses and mismatched FEN engine MultiPV parent prefix stale and under-depth entries fail closed"):
    val cache = CandidateLineProofCache.InMemory.empty
    assertEquals(cache.commit(providerAssembly(completedRootProbe = Some(rootPayload)).cacheWriteReceipt, nowEpochMs).map(_.stored), Vector(true))

    val wrongFen = rootRequest.copy(startFen = initialFen.replace(" w ", " b "))
    val wrongEngine = rootRequest.copy(engineFingerprint = "other-engine")
    val wrongMultiPv = rootRequest.copy(multiPv = 2)

    assertEquals(cache.read(wrongFen, nowEpochMs), None)
    assertEquals(cache.read(wrongEngine, nowEpochMs), None)
    assertEquals(cache.read(wrongMultiPv, nowEpochMs), None)
    assertEquals(cache.read(rootRequest, nowEpochMs + 20_000L), None)

    val rootEvidence = providerAssembly(completedRootProbe = Some(rootPayload)).candidateLineEvidence
    val childRequest = CandidateProbePlan.childStage(rootEvidence, nowEpochMs = nowEpochMs).requests.head
    val childPayload =
      CandidateProbeResultPayload(
        request = childRequest,
        lines = childPayloadLines(childRequest, "defender-resource-cache"),
        realizedDepth = 18,
        generatedAtEpochMs = 10_000L,
        maxAgeMs = 20_000L
      )
    val childCache = CandidateLineProofCache.InMemory.empty
    assertEquals(
      childCache
        .commit(
          adapterAssembly(
            childRequests = Vector(childRequest),
            childProbeResults = Vector(childPayload)
          ).cacheWriteReceipt,
          nowEpochMs
        )
        .exists(_.stored),
      true
    )
    assertEquals(childCache.read(childRequest.copy(parentLinePrefix = Vector("d2d4")), nowEpochMs), None)

    val underDepthAssembly = providerAssembly(completedRootProbe = Some(rootPayload.copy(realizedDepth = 15)))
    assertEquals(underDepthAssembly.cacheWrites, Vector.empty)
    assertEquals(CandidateLineProofCache.InMemory.empty.commit(underDepthAssembly.cacheWriteReceipt, nowEpochMs), Vector.empty)

  test("proof-cache reads reject wrong variant node ply target depth floor depth role and parent binding"):
    val rootCache = CandidateLineProofCache.InMemory.empty
    assertEquals(rootCache.commit(providerAssembly(completedRootProbe = Some(rootPayload)).cacheWriteReceipt, nowEpochMs).map(_.stored), Vector(true))

    val wrongRootRequests = Vector(
      rootRequest.copy(variant = "chess960"),
      rootRequest.copy(nodeId = "other-node"),
      rootRequest.copy(ply = rootRequest.ply + 1),
      rootRequest.copy(targetDepth = rootRequest.targetDepth + 1),
      rootRequest.copy(floorDepth = rootRequest.floorDepth + 1),
      rootRequest.copy(
        role = CandidateProbeRole.DefenderResource,
        multiPv = 2,
        parentBranchId = Some(CandidateBranchId("root-candidate-1")),
        parentLinePrefix = Vector("e2e4"),
        parentRootRank = Some(1)
      )
    )

    wrongRootRequests.foreach: request =>
      assertEquals(rootCache.read(request, nowEpochMs), None, clues(request))

    val childRequest = firstChildRequest()
    val childCache = CandidateLineProofCache.InMemory.empty
    assertEquals(
      childCache
        .commit(
          adapterAssembly(
            childRequests = Vector(childRequest),
            childProbeResults = Vector(childPayload(childRequest))
          ).cacheWriteReceipt,
          nowEpochMs
        )
        .exists(_.stored),
      true
    )

    val wrongChildRequests = Vector(
      childRequest.copy(parentBranchId = Some(CandidateBranchId("other-parent"))),
      childRequest.copy(parentLinePrefix = Vector("d2d4")),
      childRequest.copy(parentRootRank = childRequest.parentRootRank.map(_ + 1))
    )

    wrongChildRequests.foreach: request =>
      assertEquals(childCache.read(request, nowEpochMs), None, clues(request))

  test("proof-cache commits reject candidates whose key mismatches payload variant node ply target floor role or parent binding"):
    val childRequest = firstChildRequest()
    val childPayloadValue = childPayload(childRequest)
    val candidates = Vector(
      writeCandidate(rootRequest.copy(variant = "chess960"), rootPayload),
      writeCandidate(rootRequest.copy(nodeId = "other-node"), rootPayload),
      writeCandidate(rootRequest.copy(ply = rootRequest.ply + 1), rootPayload),
      writeCandidate(rootRequest.copy(targetDepth = rootRequest.targetDepth + 1), rootPayload),
      writeCandidate(rootRequest.copy(floorDepth = rootRequest.floorDepth + 1), rootPayload),
      writeCandidate(
        rootRequest.copy(
          role = CandidateProbeRole.DefenderResource,
          multiPv = 2,
          parentBranchId = Some(CandidateBranchId("root-candidate-1")),
          parentLinePrefix = Vector("e2e4"),
          parentRootRank = Some(1)
        ),
        rootPayload
      ),
      writeCandidate(childRequest.copy(parentBranchId = Some(CandidateBranchId("other-parent"))), childPayloadValue),
      writeCandidate(childRequest.copy(parentLinePrefix = Vector("d2d4")), childPayloadValue),
      writeCandidate(childRequest.copy(parentRootRank = childRequest.parentRootRank.map(_ + 1)), childPayloadValue)
    )

    val results = CandidateLineProofCache.InMemory.empty.commit(cacheReceipt(candidates), nowEpochMs)

    assertEquals(results.map(_.stored), Vector.fill(candidates.size)(false))
    assert(results.forall(_.reasons.contains(CandidateLineProofCache.WriteRejectReason.KeyPayloadMismatch)), clues(results))

  test("source retrieval revalidated-cache and forged candidate writes have no generic cache write path"):
    val sourceErrors =
      compileErrors(
        "CandidateLineProofCache.InMemory.empty.write(writeCandidate(rootRequest, rootPayload, CandidateProbeCacheWriteSource.SourceContext), nowEpochMs)"
      )
    val revalidatedErrors =
      compileErrors(
        "CandidateLineProofCache.InMemory.empty.write(writeCandidate(rootRequest, rootPayload, CandidateProbeCacheWriteSource.RevalidatedCache), nowEpochMs)"
      )
    val engineErrors =
      compileErrors(
        "CandidateLineProofCache.InMemory.empty.write(writeCandidate(rootRequest, rootPayload, CandidateProbeCacheWriteSource.EngineProbe), nowEpochMs)"
      )

    Vector(sourceErrors, revalidatedErrors, engineErrors).foreach: errors =>
      assert(errors.contains("write") || errors.contains("CandidateProbeCacheWriteCandidate"), clues(errors))

  test("cache hit consumption does not emit another write candidate"):
    val cache = CandidateLineProofCache.InMemory.empty
    val probeAssembly = providerAssembly(completedRootProbe = Some(rootPayload))
    cache.commit(probeAssembly.cacheWriteReceipt, nowEpochMs)

    val hitAssembly = providerAssembly(completedRootProbe = None, proofCache = Some(cache))

    assertEquals(hitAssembly.candidateLineEvidence.count(_.provenance.kind == CandidateLineProvenanceKind.Cache), 3)
    assertEquals(hitAssembly.cacheWrites, Vector.empty)

  test("rank 3 child target-depth-18 cache is rejected by default and target-depth-20 cache is accepted"):
    val rootAssembly = providerAssembly(completedRootProbe = Some(rootPayload))
    val rootEvidence = rootAssembly.candidateLineEvidence
    val expandedRank3Request =
      CandidateProbePlan
        .childStage(rootEvidence, CandidateProbeBudget.Default.copy(allowExpandedThirdRootChildProbe = true), nowEpochMs = nowEpochMs)
        .requests
        .find(_.parentRootRank.contains(3))
        .get
    val target18Cache = CandidateLineProofCache.InMemory.empty
    val target20Cache = CandidateLineProofCache.InMemory.empty
    val target20Request = expandedRank3Request.copy(targetDepth = CandidateProbeBudget.Default.strongCacheTargetDepth)

    assertEquals(
      target18Cache
        .commit(
          adapterAssembly(
            childRequests = Vector(expandedRank3Request),
            childProbeResults = Vector(childPayload(expandedRank3Request)),
            budget = CandidateProbeBudget.Default.copy(allowExpandedThirdRootChildProbe = true)
          ).cacheWriteReceipt,
          nowEpochMs
        )
        .exists(_.stored),
      true
    )
    assertEquals(
      target20Cache
        .commit(
          adapterAssembly(
            childRequests = Vector(target20Request),
            childProbeResults = Vector(childPayload(target20Request)),
            budget = CandidateProbeBudget.Default.copy(allowExpandedThirdRootChildProbe = true)
          ).cacheWriteReceipt,
          nowEpochMs
        )
        .exists(_.stored),
      true
    )

    val defaultTarget18 = providerAssembly(completedRootProbe = Some(rootPayload), proofCache = Some(target18Cache))
    val defaultTarget20 = providerAssembly(completedRootProbe = Some(rootPayload), proofCache = Some(target20Cache))

    assertEquals(defaultTarget18.childPackets.exists(_.parentBranchId == CandidateBranchId("root-candidate-3")), false)
    assertEquals(defaultTarget20.childPackets.exists(_.parentBranchId == CandidateBranchId("root-candidate-3")), true)

  test("provider consumes valid proof-cache hits through the facade path"):
    val rootCache = CandidateLineProofCache.InMemory.empty
    assertEquals(rootCache.commit(providerAssembly(completedRootProbe = Some(rootPayload)).cacheWriteReceipt, nowEpochMs).map(_.stored), Vector(true))

    val result = providerAssembly(completedRootProbe = None, proofCache = Some(rootCache))

    assertEquals(result.rootPacket.nonEmpty, true)
    assertEquals(result.candidateLineEvidence.count(_.provenance.kind == CandidateLineProvenanceKind.Cache), 3)
    assertEquals(result.preparedVariationEvidence.exists(_.boundClaimId == binding.boundClaimId), true)

  private def providerAssembly(
      completedRootProbe: Option[CandidateProbeResultPayload],
      proofCache: Option[CandidateLineProofCache] = None
  ): CandidateProbeControlledAdapter.AssemblyResult =
    CandidateLineAssemblyProvider.assemble(
      CandidateLineAssemblyProvider.Input(
        currentFen = initialFen,
        nodeId = nodeId,
        ply = 0,
        variant = "standard",
        engineFingerprint = engine,
        budget = CandidateProbeBudget.Default,
        completedRootProbe = completedRootProbe,
        completedChildProbes = Vector.empty,
        loweringBinding = Some(binding),
        nowEpochMs = nowEpochMs,
        proofCache = proofCache
      )
    )

  private def adapterAssembly(
      childRequests: Vector[CandidateProbeRequest],
      childProbeResults: Vector[CandidateProbeResultPayload],
      budget: CandidateProbeBudget = CandidateProbeBudget.Default
  ): CandidateProbeControlledAdapter.AssemblyResult =
    CandidateProbeControlledAdapter.assemble(
      CandidateProbeControlledAdapter.Input(
        currentFen = initialFen,
        nodeId = nodeId,
        ply = 0,
        variant = "standard",
        rootRequest = rootRequest,
        rootProbeResult = Some(rootPayload),
        rootCacheHit = None,
        childRequests = childRequests,
        childProbeResults = childProbeResults,
        childCacheHits = Vector.empty,
        loweringBinding = Some(binding),
        nowEpochMs = nowEpochMs,
        budget = budget,
        loweringPolicy = CandidateLineEvidenceLowering.Policy.Default
      )
    )

  private def firstChildRequest(): CandidateProbeRequest =
    CandidateProbePlan.childStage(providerAssembly(completedRootProbe = Some(rootPayload)).candidateLineEvidence, nowEpochMs = nowEpochMs).requests.head

  private def cacheReceipt(candidates: Vector[CandidateProbeCacheWriteCandidate]): CandidateProbeControlledAdapter.CacheWriteReceipt =
    CandidateProbeControlledAdapter.CacheWriteReceipt.fromCandidates(candidates)

  def writeCandidate(
      request: CandidateProbeRequest,
      payload: CandidateProbeResultPayload,
      source: CandidateProbeCacheWriteSource = CandidateProbeCacheWriteSource.EngineProbe
  ): CandidateProbeCacheWriteCandidate =
    CandidateProbeCacheWriteCandidate(
      key = CandidateProbeCacheKey.fromRequest(
        request = request,
        realizedDepth = payload.realizedDepth,
        generatedAtEpochMs = payload.generatedAtEpochMs,
        maxAgeMs = payload.maxAgeMs
      ),
      writeSource = source,
      payload = payload
    )

  private def childPayload(request: CandidateProbeRequest): CandidateProbeResultPayload =
    CandidateProbeResultPayload(
      request = request,
      lines = childPayloadLines(request, "defender-resource-rank3-cache"),
      realizedDepth = request.targetDepth,
      generatedAtEpochMs = 10_000L,
      maxAgeMs = 20_000L
    )

  private def childPayloadLines(request: CandidateProbeRequest, branchPrefix: String): Vector[CandidateProbeResultLine] =
    val lines =
      request.parentRootRank match
        case Some(2) =>
          Vector(Vector("d7d5", "c2c4"), Vector("g8f6", "c2c4"))
        case Some(3) =>
          Vector(Vector("d7d5", "d2d4"), Vector("c7c5", "d2d4"))
        case _ =>
          Vector(Vector("c7c5", "g1f3"), Vector("e7e5", "g1f3"))
    lines.zipWithIndex.map: (uciLine, index) =>
      payloadLine(s"$branchPrefix-${index + 1}", uciLine, index + 1, index + 1)

  private def payloadLine(
      branchId: String,
      uciLine: Vector[String],
      rank: Int,
      multiPvIndex: Int
  ): CandidateProbeResultLine =
    CandidateProbeResultLine(
      branchId = CandidateBranchId(branchId),
      rank = rank,
      multiPvIndex = multiPvIndex,
      uciLine = uciLine
    )

  private val binding =
    CandidateLineEvidenceLowering.Binding(
      boundClaimId = "proof-cache-claim",
      owner = "white",
      defender = Some("black"),
      anchor = "board",
      route = "pressure_route",
      scope = "position_local",
      provenanceRef = EvidenceRef(
        EvidenceRefKind.Certification,
        "CertifiedLine",
        Some("white"),
        Some("board"),
        Some("pressure_route"),
        Some("position_local")
      )
    )
