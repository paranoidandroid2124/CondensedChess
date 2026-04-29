package lila.commentary.line

import play.api.libs.json.Json

import lila.commentary.api.*
import lila.commentary.api.CommentaryApiJson.given
import lila.commentary.render.CommentaryRenderer
import lila.commentary.selection.*

class CandidateChildProbeIntegrationContractTest extends munit.FunSuite:

  private val initialFen = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1"
  private val nodeId = "mainline:0"
  private val nowEpochMs = 20_000L
  private val engine = "stockfish-child-integration"

  test("valid root and rank 1 and 2 child payloads produce defender-resource evidence and lower only with binding"):
    val result =
      integrate(
        completedRootProbe = Some(rootPayload),
        completedChildProbes = validChildPayloads,
        loweringBinding = Some(binding)
      )

    val roots = result.assembly.candidateLineEvidence.filter(_.role == CandidateBranchRole.RootCandidate)
    val children = result.assembly.candidateLineEvidence.filter(_.role == CandidateBranchRole.DefenderResource)

    assertEquals(roots.size, 3, clues(result.assembly.candidateLineEvidence))
    assertEquals(
      children.map(_.parentBranchId.map(_.value)).distinct.sorted,
      Vector(Some("root-candidate-1"), Some("root-candidate-2")),
      clues(children)
    )
    assert(children.forall(_.publicLineEligibleAt(nowEpochMs)), clues(children))
    assertEquals(result.satisfiedChildRequests.map(_.parentRootRank), Vector(Some(1), Some(2)), clues(result))
    assertEquals(result.unsatisfiedChildRequests, Vector.empty, clues(result))
    assert(result.assembly.preparedVariationEvidence.exists(_.role == VariationEvidenceRole.DefenderResource), clues(result.assembly.preparedVariationEvidence))

  test("without lowering binding child evidence stays internal and no prepared variation evidence is emitted"):
    val result =
      integrate(
        completedRootProbe = Some(rootPayload),
        completedChildProbes = validChildPayloads,
        loweringBinding = None
      )

    assert(result.assembly.candidateLineEvidence.exists(_.role == CandidateBranchRole.DefenderResource), clues(result.assembly.candidateLineEvidence))
    assertEquals(result.assembly.preparedVariationEvidence, Vector.empty, clues(result.assembly.preparedVariationEvidence))

  test("one missing child payload leaves only that planned child request unsatisfied"):
    val result =
      integrate(
        completedRootProbe = Some(rootPayload),
        completedChildProbes = Vector(validChildPayloads.head),
        loweringBinding = Some(binding)
      )

    assertEquals(result.satisfiedChildRequests.map(_.parentRootRank), Vector(Some(1)), clues(result))
    assertEquals(result.unsatisfiedChildRequests.map(_.parentRootRank), Vector(Some(2)), clues(result))
    assert(result.assembly.candidateLineEvidence.exists(line => line.parentBranchId.contains(CandidateBranchId("root-candidate-1"))), clues(result.assembly.candidateLineEvidence))
    assert(!result.assembly.candidateLineEvidence.exists(line => line.parentBranchId.contains(CandidateBranchId("root-candidate-2"))), clues(result.assembly.candidateLineEvidence))

  test("no child payload returns root evidence and both default child requests unsatisfied"):
    val rootOnly = CandidateRootProbeIntegration.integrate(
      CandidateRootProbeIntegration.Input(
        currentFen = initialFen,
        nodeId = nodeId,
        ply = 0,
        variant = "standard",
        engineFingerprint = engine,
        completedRootProbe = Some(rootPayload),
        loweringBinding = None,
        nowEpochMs = nowEpochMs
      )
    )
    val result = integrate(completedRootProbe = Some(rootPayload), completedChildProbes = Vector.empty, loweringBinding = None)

    assertEquals(result.assembly.candidateLineEvidence.count(_.role == CandidateBranchRole.RootCandidate), 3, clues(result.assembly.candidateLineEvidence))
    assertEquals(result.satisfiedChildRequests, Vector.empty, clues(result))
    assertEquals(result.unsatisfiedChildRequests, rootOnly.plannedChildRequests, clues(result))

  test("wrong or invalid child payload is omitted while valid root remains and no child cache write is emitted"):
    val planned = childRequests
    val invalidCases = Vector(
      validChildPayloads.head.copy(request = planned.head.copy(startFen = initialFen)),
      validChildPayloads.head.copy(request = planned.head.copy(nodeId = "other-node")),
      validChildPayloads.head.copy(request = planned.head.copy(ply = 0)),
      validChildPayloads.head.copy(request = planned.head.copy(parentBranchId = Some(CandidateBranchId("root-candidate-2")))),
      validChildPayloads.head.copy(request = planned.head.copy(parentRootRank = Some(2))),
      validChildPayloads.head.copy(request = planned.head.copy(parentLinePrefix = Vector("d2d4"))),
      validChildPayloads.head.copy(request = planned.head.copy(engineFingerprint = "other-engine")),
      validChildPayloads.head.copy(request = planned.head.copy(multiPv = 1), lines = validChildPayloads.head.lines.take(1)),
      validChildPayloads.head.copy(request = planned.head.copy(role = CandidateProbeRole.RootCandidate)),
      validChildPayloads.head.copy(generatedAtEpochMs = 1_000L, maxAgeMs = 1_000L),
      validChildPayloads.head.copy(realizedDepth = 15),
      validChildPayloads.head.copy(completed = false),
      validChildPayloads.head.copy(lines = Vector(payloadLine("defender-resource-bad", Vector("not-uci"), 1, 1))),
      validChildPayloads.head.copy(lines = validChildPayloads.head.lines.take(1)),
      validChildPayloads.head.copy(lines =
        Vector(
          payloadLine("defender-resource-r1-a", Vector("c7c5", "g1f3"), 1, 2),
          payloadLine("defender-resource-r1-b", Vector("e7e5", "g1f3"), 2, 1)
        )
      )
    )

    invalidCases.foreach: payload =>
      val result =
        integrate(
          completedRootProbe = Some(rootPayload),
          completedChildProbes = Vector(payload),
          loweringBinding = Some(binding)
        )
      assertEquals(result.assembly.candidateLineEvidence.count(_.role == CandidateBranchRole.RootCandidate), 3, clues(payload))
      assertEquals(result.assembly.candidateLineEvidence.exists(_.role == CandidateBranchRole.DefenderResource), false, clues(payload))
      assertEquals(result.assembly.preparedVariationEvidence.exists(_.role == VariationEvidenceRole.DefenderResource), false, clues(payload))
      assertEquals(result.cacheWrites.exists(_.key.role == CandidateProbeRole.DefenderResource), false, clues(payload))
      assertEquals(result.unsatisfiedChildRequests.map(_.parentRootRank), Vector(Some(1), Some(2)), clues(payload))

  test("partly replayed child payload with binding does not leak defender prepared evidence"):
    val result =
      integrate(
        completedRootProbe = Some(rootPayload),
        completedChildProbes = Vector(partialReplayChildPayload(childRequests.find(_.parentRootRank.contains(1)).get)),
        loweringBinding = Some(binding)
      )

    assertEquals(result.assembly.candidateLineEvidence.exists(_.role == CandidateBranchRole.DefenderResource), false, clues(result.assembly.candidateLineEvidence))
    assertEquals(result.assembly.preparedVariationEvidence.exists(_.role == VariationEvidenceRole.DefenderResource), false, clues(result.assembly.preparedVariationEvidence))
    assertEquals(result.cacheWrites.exists(_.key.role == CandidateProbeRole.DefenderResource), false, clues(result.cacheWrites))

  test("child integration commits no child proof-cache write for child group filtered after adapter assembly"):
    val cache = CandidateLineProofCache.InMemory.empty
    val planned = childRequests.find(_.parentRootRank.contains(1)).get

    val result =
      integrate(
        completedRootProbe = Some(rootPayload),
        completedChildProbes = Vector(partialReplayChildPayload(planned)),
        proofCache = Some(cache),
        loweringBinding = Some(binding)
      )

    assertEquals(result.cacheWrites.exists(_.key.role == CandidateProbeRole.DefenderResource), false, clues(result.cacheWrites))
    assertEquals(cache.read(planned, nowEpochMs), None)

  test("proof cache commit follows the filtered child integration receipt after duplicate child assembly"):
    val cache = CandidateLineProofCache.InMemory.empty
    val planned = childRequests.find(_.parentRootRank.contains(1)).get
    val validPayload = validChildPayloads.find(_.request == planned).get
    val duplicatePayload = completeDuplicateChildPayload(planned)

    val result =
      integrate(
        completedRootProbe = Some(rootPayload),
        completedChildProbes = Vector(validPayload, duplicatePayload),
        proofCache = Some(cache),
        loweringBinding = Some(binding)
      )
    val storedPayloadBranchIds = cache.read(planned, nowEpochMs).map(_.payload.lines.map(_.branchId).toSet)
    val returnedWriteBranchIds =
      result.cacheWrites.find(_.key.role == CandidateProbeRole.DefenderResource).map(_.payload.lines.map(_.branchId).toSet)

    assertEquals(returnedWriteBranchIds, Some(validPayload.lines.map(_.branchId).toSet), clues(result.cacheWrites))
    assertEquals(storedPayloadBranchIds, returnedWriteBranchIds, clues(storedPayloadBranchIds, returnedWriteBranchIds))

  test("invalid duplicate reusing accepted child branch ids does not suppress the valid proof-cache write"):
    val cache = CandidateLineProofCache.InMemory.empty
    val planned = childRequests.find(_.parentRootRank.contains(1)).get
    val validPayload = validChildPayloads.find(_.request == planned).get
    val malformedDuplicate =
      validPayload.copy(lines =
        Vector(
          validPayload.lines(0),
          validPayload.lines(1).copy(uciLine = Vector("not-uci"))
        )
      )

    val result =
      integrate(
        completedRootProbe = Some(rootPayload),
        completedChildProbes = Vector(validPayload, malformedDuplicate),
        proofCache = Some(cache),
        loweringBinding = Some(binding)
      )
    val storedPayloadBranchIds = cache.read(planned, nowEpochMs).map(_.payload.lines.map(_.branchId).toSet)
    val acceptedChildEvidence = result.assembly.candidateLineEvidence.filter(_.role == CandidateBranchRole.DefenderResource)
    val acceptedChildWrites = result.cacheWrites.filter(_.key.role == CandidateProbeRole.DefenderResource)

    assertEquals(result.satisfiedChildRequests.map(_.parentRootRank), Vector(Some(1)), clues(result))
    assertEquals(acceptedChildEvidence.map(_.branchId).toSet, validPayload.lines.map(_.branchId).toSet, clues(acceptedChildEvidence))
    assertEquals(acceptedChildWrites.map(_.payload.lines.map(_.branchId).toSet), Vector(validPayload.lines.map(_.branchId).toSet), clues(acceptedChildWrites))
    assertEquals(storedPayloadBranchIds, Some(validPayload.lines.map(_.branchId).toSet), clues(storedPayloadBranchIds))

  test("complete duplicate child arbitration follows accepted payload order instead of branch id lexical order"):
    val cache = CandidateLineProofCache.InMemory.empty
    val planned = childRequests.find(_.parentRootRank.contains(1)).get
    val validPayload = validChildPayloads.find(_.request == planned).get
    val lexicallyEarlierDuplicate = completeDuplicateChildPayload(planned, branchPrefix = "aa-defender-resource-duplicate")

    val result =
      integrate(
        completedRootProbe = Some(rootPayload),
        completedChildProbes = Vector(validPayload, lexicallyEarlierDuplicate),
        proofCache = Some(cache),
        loweringBinding = Some(binding)
      )
    val storedPayloadBranchIds = cache.read(planned, nowEpochMs).map(_.payload.lines.map(_.branchId).toSet)
    val acceptedChildEvidence = result.assembly.candidateLineEvidence.filter(_.role == CandidateBranchRole.DefenderResource)
    val acceptedChildWrites = result.cacheWrites.filter(_.key.role == CandidateProbeRole.DefenderResource)

    assertEquals(acceptedChildEvidence.map(_.branchId).toSet, validPayload.lines.map(_.branchId).toSet, clues(acceptedChildEvidence))
    assertEquals(acceptedChildWrites.map(_.payload.lines.map(_.branchId).toSet), Vector(validPayload.lines.map(_.branchId).toSet), clues(acceptedChildWrites))
    assertEquals(storedPayloadBranchIds, Some(validPayload.lines.map(_.branchId).toSet), clues(storedPayloadBranchIds))

  test("malformed duplicate child payload does not poison an otherwise complete planned child group"):
    val planned = childRequests.find(_.parentRootRank.contains(1)).get
    val validPayload = validChildPayloads.find(_.request == planned).get
    val malformedDuplicate = partialReplayChildPayload(planned, branchPrefix = "defender-resource-r1-duplicate")

    val result =
      integrate(
        completedRootProbe = Some(rootPayload),
        completedChildProbes = Vector(validPayload, malformedDuplicate),
        loweringBinding = Some(binding)
      )
    val acceptedChildEvidence = result.assembly.candidateLineEvidence.filter(_.role == CandidateBranchRole.DefenderResource)
    val acceptedChildWrites = result.cacheWrites.filter(_.key.role == CandidateProbeRole.DefenderResource)

    assertEquals(result.satisfiedChildRequests.map(_.parentRootRank), Vector(Some(1)), clues(result))
    assertEquals(result.unsatisfiedChildRequests.map(_.parentRootRank), Vector(Some(2)), clues(result))
    assertEquals(acceptedChildEvidence.map(_.branchId).toSet, validPayload.lines.map(_.branchId).toSet, clues(acceptedChildEvidence))
    assertEquals(acceptedChildWrites.map(_.payload.lines.map(_.branchId).toSet), Vector(validPayload.lines.map(_.branchId).toSet), clues(acceptedChildWrites))

  test("valid child proof-cache hit satisfies a planned child request without a fresh child payload or new write"):
    val cache = CandidateLineProofCache.InMemory.empty
    val populated =
      integrate(
        completedRootProbe = Some(rootPayload),
        completedChildProbes = Vector(validChildPayloads.head),
        proofCache = Some(cache),
        loweringBinding = Some(binding)
      )
    assert(populated.cacheWrites.exists(_.key.role == CandidateProbeRole.DefenderResource), clues(populated.cacheWrites))

    val result =
      integrate(
        completedRootProbe = None,
        completedChildProbes = Vector.empty,
        proofCache = Some(cache),
        loweringBinding = Some(binding)
      )

    val cachedChild = result.assembly.candidateLineEvidence.filter(_.role == CandidateBranchRole.DefenderResource)
    assert(cachedChild.nonEmpty, clues(result.assembly.candidateLineEvidence))
    assert(cachedChild.forall(_.provenance.kind == CandidateLineProvenanceKind.Cache), clues(cachedChild))
    assertEquals(result.satisfiedChildRequests.map(_.parentRootRank), Vector(Some(1)), clues(result))
    assertEquals(result.unsatisfiedChildRequests.map(_.parentRootRank), Vector(Some(2)), clues(result))
    assertEquals(result.cacheWrites, Vector.empty, clues(result.cacheWrites))

  test("rank 3 live child payload is rejected by default while strong cache rank 3 remains cache-only"):
    val rank3Request =
      childRequestsWithExpandedBudget.find(_.parentRootRank.contains(3)).get
    val rank3Payload =
      CandidateProbeResultPayload(
        request = rank3Request,
        lines = Vector(
          payloadLine("defender-resource-r3-a", Vector("d7d5", "d2d4"), 1, 1),
          payloadLine("defender-resource-r3-b", Vector("g8f6", "d2d4"), 2, 2)
        ),
        realizedDepth = 18,
        generatedAtEpochMs = 10_000L,
        maxAgeMs = 20_000L
      )
    val liveResult =
      integrate(
        completedRootProbe = Some(rootPayload),
        completedChildProbes = Vector(rank3Payload),
        loweringBinding = Some(binding)
      )

    assert(!liveResult.assembly.candidateLineEvidence.exists(line => line.parentBranchId.contains(CandidateBranchId("root-candidate-3"))), clues(liveResult.assembly.candidateLineEvidence))
    assert(!liveResult.satisfiedChildRequests.exists(_.parentRootRank.contains(3)), clues(liveResult.satisfiedChildRequests))
    assertEquals(liveResult.unsatisfiedChildRequests.map(_.parentRootRank), Vector(Some(1), Some(2)), clues(liveResult))

    val strongRank3Request = rank3Request.copy(targetDepth = 20)
    val strongRank3Payload = rank3Payload.copy(request = strongRank3Request, realizedDepth = 20)
    val cache = fakeCache(rootPayload, Vector(strongRank3Payload))
    val cacheResult =
      integrate(
        completedRootProbe = None,
        completedChildProbes = Vector.empty,
        proofCache = Some(cache),
        loweringBinding = Some(binding)
      )
    assert(cacheResult.assembly.candidateLineEvidence.exists(line => line.parentBranchId.contains(CandidateBranchId("root-candidate-3"))), clues(cacheResult.assembly.candidateLineEvidence))
    assertEquals(cacheResult.unsatisfiedChildRequests.map(_.parentRootRank), Vector(Some(1), Some(2)), clues(cacheResult))

  test("public backend and render JSON do not leak child probe cache or planning internals"):
    val proof =
      integrate(
        completedRootProbe = Some(rootPayload),
        completedChildProbes = validChildPayloads,
        loweringBinding = Some(binding)
      ).assembly.preparedVariationEvidence.find(_.role == VariationEvidenceRole.DefenderResource).get
    val claim = boardClaim(Vector(proof))
    val responseText =
      Json
        .toJson(CommentaryBackendSeam.withClaimProvider(_ => Vector(claim), nowEpochMs = () => nowEpochMs).renderDebug(request()))
        .toString
    val renderText = Json.toJson(CommentaryRenderer.render(CommentaryOutlineBuilder.build(ClaimSelector.select(Vector(claim))))).toString

    Vector(
      "CandidateChildProbeIntegration",
      "completedChildProbes",
      "satisfiedChildRequests",
      "unsatisfiedChildRequests",
      "parentLinePrefix",
      "parentBranchId",
      "root-candidate-1",
      "defender-resource-r1-a",
      "engineFingerprint",
      engine,
      "cacheWrites",
      "rawLines"
    ).foreach: token =>
      assert(!responseText.contains(token), clues(token, responseText))
      assert(!renderText.contains(token), clues(token, renderText))

  private def integrate(
      completedRootProbe: Option[CandidateProbeResultPayload],
      completedChildProbes: Vector[CandidateProbeResultPayload],
      proofCache: Option[CandidateLineProofCache] = None,
      loweringBinding: Option[CandidateLineEvidenceLowering.Binding],
      budget: CandidateProbeBudget = CandidateProbeBudget.Default
  ): CandidateChildProbeIntegration.Result =
    CandidateChildProbeIntegration.integrate(
      CandidateChildProbeIntegration.Input(
        currentFen = initialFen,
        nodeId = nodeId,
        ply = 0,
        variant = "standard",
        engineFingerprint = engine,
        budget = budget,
        completedRootProbe = completedRootProbe,
        completedChildProbes = completedChildProbes,
        proofCache = proofCache,
        loweringBinding = loweringBinding,
        nowEpochMs = nowEpochMs
      )
    )

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

  private lazy val rootEvidence: Vector[CandidateLineEvidence] =
    val rootPacket = CandidateProbeControlledAdapter.rootPacketFrom(rootPayload, CandidateLineProvenanceKind.EngineRoot).get
    CandidateLinePacketHandoff.normalize(CandidateLinePacketHandoffInput(initialFen, nodeId, 0, rootPacket), nowEpochMs)

  private lazy val childRequests: Vector[CandidateProbeRequest] =
    CandidateProbePlan.childStage(rootEvidence, nowEpochMs = nowEpochMs).requests

  private lazy val childRequestsWithExpandedBudget: Vector[CandidateProbeRequest] =
    CandidateProbePlan
      .childStage(
        rootEvidence,
        budget = CandidateProbeBudget.Default.copy(allowExpandedThirdRootChildProbe = true),
        nowEpochMs = nowEpochMs
      )
      .requests

  private lazy val validChildPayloads: Vector[CandidateProbeResultPayload] =
    Vector(
      CandidateProbeResultPayload(
        request = childRequests.find(_.parentRootRank.contains(1)).get,
        lines = Vector(
          payloadLine("defender-resource-r1-a", Vector("c7c5", "g1f3"), 1, 1),
          payloadLine("defender-resource-r1-b", Vector("e7e5", "g1f3"), 2, 2)
        ),
        realizedDepth = 18,
        generatedAtEpochMs = 10_000L,
        maxAgeMs = 20_000L
      ),
      CandidateProbeResultPayload(
        request = childRequests.find(_.parentRootRank.contains(2)).get,
        lines = Vector(
          payloadLine("defender-resource-r2-a", Vector("d7d5", "c2c4"), 1, 1),
          payloadLine("defender-resource-r2-b", Vector("g8f6", "c2c4"), 2, 2)
        ),
        realizedDepth = 18,
        generatedAtEpochMs = 10_000L,
        maxAgeMs = 20_000L
      )
    )

  private def partialReplayChildPayload(
      request: CandidateProbeRequest,
      branchPrefix: String = "defender-resource-partial"
  ): CandidateProbeResultPayload =
    CandidateProbeResultPayload(
      request = request,
      lines = Vector(
        payloadLine(s"$branchPrefix-a", Vector("c7c5", "g1f3"), 1, 1),
        payloadLine(s"$branchPrefix-b", Vector("not-uci"), 2, 2)
      ),
      realizedDepth = 18,
      generatedAtEpochMs = 10_000L,
      maxAgeMs = 20_000L
    )

  private def completeDuplicateChildPayload(
      request: CandidateProbeRequest,
      branchPrefix: String = "zz-defender-resource-duplicate"
  ): CandidateProbeResultPayload =
    CandidateProbeResultPayload(
      request = request,
      lines = Vector(
        payloadLine(s"$branchPrefix-a", Vector("c7c5", "g1f3"), 1, 1),
        payloadLine(s"$branchPrefix-b", Vector("e7e5", "g1f3"), 2, 2)
      ),
      realizedDepth = 18,
      generatedAtEpochMs = 10_000L,
      maxAgeMs = 20_000L
    )

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

  private def fakeCache(
      root: CandidateProbeResultPayload,
      children: Vector[CandidateProbeResultPayload]
  ): CandidateLineProofCache =
    new CandidateLineProofCache:
      override def read(request: CandidateProbeRequest, nowEpochMs: Long): Option[CandidateProbeCacheLookupResult] =
        (root +: children).find(_.request == request).map(payload => lookup(request, payload))

      override def read(
          key: CandidateProbeCacheKey,
          request: CandidateProbeRequest,
          nowEpochMs: Long
      ): Option[CandidateProbeCacheLookupResult] =
        read(request, nowEpochMs).filter(_.entry.key == key)

      override def commit(
          receipt: CandidateProbeControlledAdapter.CacheWriteReceipt,
          nowEpochMs: Long
      ): Vector[CandidateLineProofCache.WriteResult] =
        Vector.empty

  private def lookup(
      request: CandidateProbeRequest,
      payload: CandidateProbeResultPayload
  ): CandidateProbeCacheLookupResult =
    CandidateProbeCacheLookupResult(
      request = request,
      entry = CandidateProbeCacheEntry(
        CandidateProbeCacheKey.fromRequest(
          request = request,
          realizedDepth = payload.realizedDepth,
          generatedAtEpochMs = payload.generatedAtEpochMs,
          maxAgeMs = payload.maxAgeMs
        ),
        CandidateProbeCacheWriteSource.EngineProbe
      ),
      payload = payload
    )

  private val binding =
    CandidateLineEvidenceLowering.Binding(
      boundClaimId = "child-integration-claim",
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

  private def boardClaim(variationEvidence: Vector[PreparedVariationEvidence]): CommentaryClaim =
    CommentaryClaim(
      id = "child-integration-claim",
      layer = ClaimLayer.Certification,
      status = ClaimStatus.Admitted,
      owner = Some("white"),
      beneficiary = Some("white"),
      defender = Some("black"),
      sideToMove = Some("white"),
      anchor = Some("board"),
      route = Some("pressure_route"),
      scope = Some("position_local"),
      impact = ClaimImpact(resultMaterialImpact = 60, evidenceConfidence = 80, boardExplainability = 70),
      evidenceRefs = Vector(binding.provenanceRef),
      exactBoardBound = true,
      wordingStrengthCap = WordingStrength.QualifiedSupport,
      variationEvidence = variationEvidence
    )

  private def request(): CommentaryRequest =
    CommentaryRequest(
      currentFen = initialFen,
      beforeFen = None,
      playedMove = None,
      nodeId = nodeId,
      ply = 0
    )
