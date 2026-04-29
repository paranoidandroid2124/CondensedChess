package lila.commentary.line

import play.api.libs.json.Json

import lila.commentary.api.*
import lila.commentary.api.CommentaryApiJson.given
import lila.commentary.render.CommentaryRenderer
import lila.commentary.selection.*

class CandidateProbeControlledAdapterContractTest extends munit.FunSuite:

  private val initialFen = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1"
  private val nodeId = "mainline:0"
  private val nowEpochMs = 20_000L
  private val engine = "stockfish-test-depth18"
  private val rootRequest =
    CandidateProbePlan.rootStage(initialFen, nodeId, 0, "standard", engine).requests.head
  private val rootPayload =
    CandidateProbeResultPayload(
      request = rootRequest,
      lines = Vector(
        payloadLine("root-candidate-1", Vector("e2e4", "e7e5"), rank = 1, multiPvIndex = 1),
        payloadLine("root-candidate-2", Vector("d2d4", "d7d5"), rank = 2, multiPvIndex = 2),
        payloadLine("root-candidate-3", Vector("g1f3", "g8f6"), rank = 3, multiPvIndex = 3)
      ),
      realizedDepth = 18,
      generatedAtEpochMs = 10_000L,
      maxAgeMs = 20_000L
    )
  private val permutedRootPayload =
    rootPayload.copy(lines =
      Vector(
        payloadLine("root-candidate-1", Vector("e2e4", "e7e5"), rank = 1, multiPvIndex = 3),
        payloadLine("root-candidate-2", Vector("d2d4", "d7d5"), rank = 2, multiPvIndex = 2),
        payloadLine("root-candidate-3", Vector("g1f3", "g8f6"), rank = 3, multiPvIndex = 1)
      )
    )

  test("valid root probe result becomes root packet and candidate-line evidence"):
    val result = assemble(rootProbeResult = Some(rootPayload))

    assertEquals(result.rootPacket.nonEmpty, true)
    assertEquals(result.candidateLineEvidence.count(_.role == CandidateBranchRole.RootCandidate), 3)
    assertEquals(result.candidateLineEvidence.filter(_.role == CandidateBranchRole.RootCandidate).forall(_.publicLineEligibleAt(nowEpochMs)), true)
    assertEquals(result.preparedVariationEvidence.exists(_.proves == "main_candidate_line"), true)

  test("direct root probe payload with MultiPV 2 yields no public-safe evidence or cache write"):
    val underBudgetRootRequest = rootRequest.copy(multiPv = 2)
    val underBudgetRootPayload =
      rootPayload.copy(
        request = underBudgetRootRequest,
        lines = rootPayload.lines.take(2)
      )

    val result =
      assemble(
        rootRequestOverride = Some(underBudgetRootRequest),
        rootProbeResult = Some(underBudgetRootPayload)
      )

    assertEquals(result.candidateLineEvidence, Vector.empty)
    assertEquals(result.preparedVariationEvidence, Vector.empty)
    assertEquals(result.cacheWrites, Vector.empty)

  test("root probe and root cache payloads require each rank to match its MultiPV index"):
    val rootCacheHit = cacheLookup(rootRequest, permutedRootPayload)

    Vector(
      assemble(rootProbeResult = Some(permutedRootPayload)),
      assemble(rootProbeResult = None, rootCacheHit = Some(rootCacheHit))
    ).foreach: result =>
      assertEquals(result.rootPacket, None)
      assertEquals(result.childPackets, Vector.empty)
      assertEquals(result.candidateLineEvidence, Vector.empty)
      assertEquals(result.preparedVariationEvidence, Vector.empty)
      assertEquals(result.cacheWrites, Vector.empty)

  test("valid child probe result for rank 1 lowers to defender resource prepared evidence"):
    val childRequest = childRequestsFrom(rootPayload).head
    val result =
      assemble(
        rootProbeResult = Some(rootPayload),
        childProbeResults = Vector(
          CandidateProbeResultPayload(
            request = childRequest,
            lines = childPayloadLines(childRequest, "defender-resource-1"),
            realizedDepth = 18,
            generatedAtEpochMs = 10_000L,
            maxAgeMs = 20_000L
          )
        )
      )

    assertEquals(result.childPackets.size, 1)
    assertEquals(result.candidateLineEvidence.exists(_.role == CandidateBranchRole.DefenderResource), true)
    assertEquals(result.preparedVariationEvidence.exists(_.role == VariationEvidenceRole.DefenderResource), true)

  test("direct child probe payload with MultiPV 1 cannot lower or emit a child cache write"):
    val underBudgetChildRequest = childRequestsFrom(rootPayload).head.copy(multiPv = 1)
    val underBudgetChildPayload =
      CandidateProbeResultPayload(
        request = underBudgetChildRequest,
        lines = Vector(payloadLine("defender-resource-under-budget", Vector("c7c5", "g1f3"))),
        realizedDepth = 18,
        generatedAtEpochMs = 10_000L,
        maxAgeMs = 20_000L
      )

    val result =
      assemble(
        rootProbeResult = Some(rootPayload),
        childRequestOverride = Some(Vector(underBudgetChildRequest)),
        childProbeResults = Vector(underBudgetChildPayload)
      )

    assertEquals(result.childPackets, Vector.empty)
    assertEquals(result.candidateLineEvidence.exists(_.branchId == CandidateBranchId("defender-resource-under-budget")), false)
    assertEquals(result.preparedVariationEvidence.exists(_.role == VariationEvidenceRole.DefenderResource), false)
    assertEquals(result.cacheWrites.map(_.key.role), Vector(CandidateProbeRole.RootCandidate))

  test("default budget rejects direct rank 3 child probe payload even when supplied in child requests"):
    val rank3Request =
      childRequestsFrom(rootPayload, budget = CandidateProbeBudget.Default.copy(allowExpandedThirdRootChildProbe = true))
        .find(_.parentRootRank.contains(3))
        .get
    val rank3Payload =
      CandidateProbeResultPayload(
        request = rank3Request,
        lines = childPayloadLines(rank3Request, "defender-resource-rank3"),
        realizedDepth = 18,
        generatedAtEpochMs = 10_000L,
        maxAgeMs = 20_000L
      )

    val result =
      assemble(
        rootProbeResult = Some(rootPayload),
        childRequestOverride = Some(Vector(rank3Request)),
        childProbeResults = Vector(rank3Payload)
      )

    assertEquals(result.childPackets.exists(_.parentBranchId == CandidateBranchId("root-candidate-3")), false)
    assertEquals(result.candidateLineEvidence.exists(_.branchId == CandidateBranchId("defender-resource-rank3")), false)
    assertEquals(result.cacheWrites.exists(_.key.parentBranchId.contains(CandidateBranchId("root-candidate-3"))), false)

  test("missing root result yields no candidate-line evidence and no cache write"):
    val result = assemble(rootProbeResult = None)

    assertEquals(result.rootPacket, None)
    assertEquals(result.candidateLineEvidence, Vector.empty)
    assertEquals(result.preparedVariationEvidence, Vector.empty)
    assertEquals(result.cacheWrites, Vector.empty)

  test("invalid root result yields no candidate-line evidence"):
    val invalidRoot =
      rootPayload.copy(lines = Vector(payloadLine("root-candidate-1", Vector("not-uci"))), realizedDepth = 18)
    val result = assemble(rootProbeResult = Some(invalidRoot))

    assertEquals(result.candidateLineEvidence, Vector.empty)
    assertEquals(result.preparedVariationEvidence, Vector.empty)
    assertEquals(result.cacheWrites, Vector.empty)

  test("invalid child result preserves valid root evidence and root cache write"):
    val childRequest = childRequestsFrom(rootPayload).head
    val result =
      assemble(
        rootProbeResult = Some(rootPayload),
        childProbeResults = Vector(
          CandidateProbeResultPayload(
            request = childRequest,
            lines = Vector(payloadLine("defender-resource-1", Vector("not-uci"))),
            realizedDepth = 18,
            generatedAtEpochMs = 10_000L,
            maxAgeMs = 20_000L
          )
        )
      )

    assertEquals(result.candidateLineEvidence.exists(_.role == CandidateBranchRole.RootCandidate), true)
    assertEquals(result.candidateLineEvidence.filter(_.role == CandidateBranchRole.DefenderResource).forall(!_.publicLineEligibleAt(nowEpochMs)), true)
    assertEquals(result.cacheWrites.map(_.key.role), Vector(CandidateProbeRole.RootCandidate))

  test("accepted root and child probe results produce complete cache write candidates"):
    val childRequest = childRequestsFrom(rootPayload).head
    val result =
      assemble(
        rootProbeResult = Some(rootPayload),
        childProbeResults = Vector(
          CandidateProbeResultPayload(
            request = childRequest,
            lines = childPayloadLines(childRequest, "defender-resource-1"),
            realizedDepth = 18,
            generatedAtEpochMs = 10_000L,
            maxAgeMs = 20_000L
          )
        )
      )

    assertEquals(result.cacheWrites.map(_.key.role), Vector(CandidateProbeRole.RootCandidate, CandidateProbeRole.DefenderResource))
    assertEquals(result.cacheWrites.forall(_.key.normalizedFen.nonEmpty), true)
    assertEquals(result.cacheWrites.forall(_.key.variant == "standard"), true)
    assertEquals(result.cacheWrites.forall(_.key.nodeId == nodeId), true)
    assertEquals(result.cacheWrites.forall(_.key.engineFingerprint == engine), true)
    assertEquals(result.cacheWrites.forall(_.key.targetDepth == 18), true)
    assertEquals(result.cacheWrites.forall(_.key.floorDepth == 16), true)

  test("cache hit can substitute for a missing child probe when revalidated"):
    val childRequest = childRequestsFrom(rootPayload).head
    val cacheHit =
      cacheLookup(
        childRequest,
        CandidateProbeResultPayload(
          request = childRequest,
          lines = childPayloadLines(childRequest, "defender-resource-cache"),
          realizedDepth = 18,
          generatedAtEpochMs = 10_000L,
          maxAgeMs = 20_000L
        )
      )
    val result = assemble(rootProbeResult = Some(rootPayload), childCacheHits = Vector(cacheHit))

    val child = result.candidateLineEvidence.find(_.branchId.value.startsWith("defender-resource-cache-")).get
    assertEquals(child.provenance.kind, CandidateLineProvenanceKind.Cache)
    assertEquals(child.publicLineEligibleAt(nowEpochMs), true)
    assertEquals(result.preparedVariationEvidence.exists(_.role == VariationEvidenceRole.DefenderResource), true)

  test("stale root cache hit yields no candidate-line evidence and no cache write"):
    val staleRootCache =
      cacheLookup(rootRequest, rootPayload)
        .copy(entry =
          CandidateProbeCacheEntry(
            key = CandidateProbeCacheKey.fromRequest(rootRequest, realizedDepth = 18, generatedAtEpochMs = 1_000L, maxAgeMs = 1_000L),
            writeSource = CandidateProbeCacheWriteSource.EngineProbe
          )
        )

    val result = assemble(rootProbeResult = None, rootCacheHit = Some(staleRootCache))

    assertEquals(result.candidateLineEvidence, Vector.empty)
    assertEquals(result.cacheWrites, Vector.empty)

  test("source root cache hit cannot become proof-owned candidate-line evidence"):
    val sourceRootCache =
      cacheLookup(rootRequest, rootPayload, writeSource = CandidateProbeCacheWriteSource.SourceContext)

    val result = assemble(rootProbeResult = None, rootCacheHit = Some(sourceRootCache))

    assertEquals(result.candidateLineEvidence, Vector.empty)
    assertEquals(result.cacheWrites, Vector.empty)

  test("rank 3 child cache hit requires strong cache target depth 20 unless expanded budget is explicit"):
    val rank3Request =
      childRequestsFrom(rootPayload, budget = CandidateProbeBudget.Default.copy(allowExpandedThirdRootChildProbe = true))
        .find(_.parentRootRank.contains(3))
        .get
    val target18Hit = cacheLookup(rank3Request, cacheChildPayload(rank3Request))
    val target20Request = rank3Request.copy(targetDepth = CandidateProbeBudget.Default.strongCacheTargetDepth)
    val target20Hit = cacheLookup(target20Request, cacheChildPayload(target20Request))

    val defaultTarget18 = assemble(rootProbeResult = Some(rootPayload), childCacheHits = Vector(target18Hit))
    val defaultTarget20 = assemble(rootProbeResult = Some(rootPayload), childCacheHits = Vector(target20Hit))
    val expandedTarget18 =
      assemble(
        rootProbeResult = Some(rootPayload),
        childCacheHits = Vector(target18Hit),
        budget = CandidateProbeBudget.Default.copy(allowExpandedThirdRootChildProbe = true)
      )

    assertEquals(defaultTarget18.childPackets.exists(_.parentBranchId == CandidateBranchId("root-candidate-3")), false)
    assertEquals(defaultTarget20.childPackets.exists(_.parentBranchId == CandidateBranchId("root-candidate-3")), true)
    assertEquals(expandedTarget18.childPackets.exists(_.parentBranchId == CandidateBranchId("root-candidate-3")), true)

  test("rank 3 child cache hit cannot bypass strong depth by omitting parent root rank"):
    val rank3Request =
      childRequestsFrom(rootPayload, budget = CandidateProbeBudget.Default.copy(allowExpandedThirdRootChildProbe = true))
        .find(_.parentRootRank.contains(3))
        .get
    val missingRankRequest = rank3Request.copy(parentRootRank = None)
    val missingRankHit = cacheLookup(missingRankRequest, cacheChildPayload(missingRankRequest))

    val result = assemble(rootProbeResult = Some(rootPayload), childCacheHits = Vector(missingRankHit))

    assertEquals(result.childPackets.exists(_.parentBranchId == CandidateBranchId("root-candidate-3")), false)

  test("rank 3 child cache hit cannot bypass strong depth by forging parent root rank"):
    val rank3Request =
      childRequestsFrom(rootPayload, budget = CandidateProbeBudget.Default.copy(allowExpandedThirdRootChildProbe = true))
        .find(_.parentRootRank.contains(3))
        .get
    val forgedRankRequest = rank3Request.copy(parentRootRank = Some(1))
    val forgedRankHit = cacheLookup(forgedRankRequest, cacheChildPayload(forgedRankRequest))

    val result = assemble(rootProbeResult = Some(rootPayload), childCacheHits = Vector(forgedRankHit))

    assertEquals(result.childPackets.exists(_.parentBranchId == CandidateBranchId("root-candidate-3")), false)

  test("rank 3 child cache hit cannot pair a strong cache key with a shallow payload request"):
    val rank3Request =
      childRequestsFrom(rootPayload, budget = CandidateProbeBudget.Default.copy(allowExpandedThirdRootChildProbe = true))
        .find(_.parentRootRank.contains(3))
        .get
    val strongRequest = rank3Request.copy(targetDepth = CandidateProbeBudget.Default.strongCacheTargetDepth)
    val mismatchedHit =
      CandidateProbeCacheLookupResult(
        request = rank3Request,
        entry = CandidateProbeCacheEntry(
          key = CandidateProbeCacheKey.fromRequest(
            request = strongRequest,
            realizedDepth = strongRequest.targetDepth,
            generatedAtEpochMs = 10_000L,
            maxAgeMs = 20_000L
          ),
          writeSource = CandidateProbeCacheWriteSource.EngineProbe
        ),
        payload = cacheChildPayload(rank3Request)
      )

    val result = assemble(rootProbeResult = Some(rootPayload), childCacheHits = Vector(mismatchedHit))

    assertEquals(result.childPackets.exists(_.parentBranchId == CandidateBranchId("root-candidate-3")), false)

  test("stale under-depth wrong-FEN wrong-engine and wrong-parent-prefix cache hits are rejected"):
    val childRequest = childRequestsFrom(rootPayload).head
    val accepted = cacheLookup(childRequest, cacheChildPayload(childRequest))
    val stale = accepted.copy(entry = accepted.entry.copy(key = accepted.entry.key.copy(generatedAtEpochMs = 1_000L, maxAgeMs = 1_000L)))
    val underDepth = accepted.copy(entry = accepted.entry.copy(key = accepted.entry.key.copy(realizedDepth = 15)))
    val wrongFen = accepted.copy(entry = accepted.entry.copy(key = accepted.entry.key.copy(normalizedFen = initialFen)))
    val wrongEngine = accepted.copy(entry = accepted.entry.copy(key = accepted.entry.key.copy(engineFingerprint = "other-engine")))
    val wrongParent = accepted.copy(entry = accepted.entry.copy(key = accepted.entry.key.copy(parentLinePrefix = Vector("d2d4"))))

    Vector(stale, underDepth, wrongFen, wrongEngine, wrongParent).foreach: hit =>
      val result = assemble(rootProbeResult = Some(rootPayload), childCacheHits = Vector(hit))
      assertEquals(result.childPackets, Vector.empty)

  test("source and retrieval cache entries cannot become proof-owned candidate-line evidence"):
    val childRequest = childRequestsFrom(rootPayload).head
    val sourceHit =
      cacheLookup(childRequest, cacheChildPayload(childRequest), writeSource = CandidateProbeCacheWriteSource.SourceContext)
    val retrievalHit =
      cacheLookup(childRequest, cacheChildPayload(childRequest), writeSource = CandidateProbeCacheWriteSource.Retrieval)

    assertEquals(assemble(rootProbeResult = Some(rootPayload), childCacheHits = Vector(sourceHit)).childPackets, Vector.empty)
    assertEquals(assemble(rootProbeResult = Some(rootPayload), childCacheHits = Vector(retrievalHit)).childPackets, Vector.empty)

  test("adapter emits no CommentaryClaim or prose"):
    val result = assemble(rootProbeResult = Some(rootPayload))

    assertEquals(result.productElementNames.exists(_.toLowerCase.contains("claim")), false)
    assertEquals(result.productElementNames.exists(_.toLowerCase.contains("prose")), false)
    assertEquals(result.preparedVariationEvidence.exists(_.productElementNames.exists(_ == "text")), false)

  test("adapter produces prepared variation evidence only with explicit lowering binding"):
    val result = assemble(rootProbeResult = Some(rootPayload), loweringBinding = None)

    assertEquals(result.candidateLineEvidence.nonEmpty, true)
    assertEquals(result.preparedVariationEvidence, Vector.empty)

  test("adapter-shaped request JSON fields are ignored and never serialized"):
    val rawRequest = Json.obj(
      "currentFen" -> initialFen,
      "nodeId" -> nodeId,
      "ply" -> 0,
      "rootProbeResult" -> Json.obj("rawLines" -> Json.arr(Json.arr("e2e4")), "engineFingerprint" -> "adapter-secret-engine"),
      "rootCacheHit" -> Json.obj("cacheKey" -> "adapter-secret-root-cache"),
      "childProbeResults" -> Json.arr(Json.obj("parentLinePrefix" -> Json.arr("e2e4"))),
      "childCacheHits" -> Json.arr(Json.obj("cacheKey" -> "adapter-secret-child-cache")),
      "cacheWrites" -> Json.arr(Json.obj("cacheKey" -> "adapter-secret-write"))
    )

    val response =
      CommentaryBackendSeam
        .withClaimProvider(_ => Vector.empty, nowEpochMs = () => nowEpochMs)
        .renderDebug(rawRequest.as[CommentaryRequest])
    val json = Json.toJson(response).toString

    Vector(
      "rootProbeResult",
      "rootCacheHit",
      "childProbeResults",
      "childCacheHits",
      "cacheWrites",
      "adapter-secret-engine",
      "adapter-secret-root-cache",
      "adapter-secret-child-cache",
      "adapter-secret-write"
    ).foreach(token => assert(!json.contains(token), clues(token, json)))

  test("raw probe and cache packet fields do not leak into public backend or render JSON"):
    val proof = assemble(rootProbeResult = Some(rootPayload)).preparedVariationEvidence.head
    val claim = boardClaim(Vector(proof))
    val responseText =
      Json
        .toJson(CommentaryBackendSeam.withClaimProvider(_ => Vector(claim), nowEpochMs = () => nowEpochMs).renderDebug(request()))
        .toString
    val renderText = Json.toJson(CommentaryRenderer.render(CommentaryOutlineBuilder.build(ClaimSelector.select(Vector(claim))))).toString

    Vector(
      "CandidateProbeResultPayload",
      "CandidateProbeCacheLookupResult",
      "CandidateProbeCacheWriteCandidate",
      "CandidateProbeControlledAdapter",
      "parentLinePrefix",
      "engineFingerprint",
      "stockfish-test-depth18",
      "rawLines"
    ).foreach: token =>
      assert(!responseText.contains(token), clues(token, responseText))
      assert(!renderText.contains(token), clues(token, renderText))

  private def assemble(
      rootRequestOverride: Option[CandidateProbeRequest] = None,
      rootProbeResult: Option[CandidateProbeResultPayload],
      rootCacheHit: Option[CandidateProbeCacheLookupResult] = None,
      childProbeResults: Vector[CandidateProbeResultPayload] = Vector.empty,
      childCacheHits: Vector[CandidateProbeCacheLookupResult] = Vector.empty,
      childRequestOverride: Option[Vector[CandidateProbeRequest]] = None,
      budget: CandidateProbeBudget = CandidateProbeBudget.Default,
      loweringBinding: Option[CandidateLineEvidenceLowering.Binding] = Some(binding)
  ): CandidateProbeControlledAdapter.AssemblyResult =
    CandidateProbeControlledAdapter.assemble(
      CandidateProbeControlledAdapter.Input(
        currentFen = initialFen,
        nodeId = nodeId,
        ply = 0,
        variant = "standard",
        rootRequest = rootRequestOverride.getOrElse(rootRequest),
        rootProbeResult = rootProbeResult,
        rootCacheHit = rootCacheHit,
        childRequests = childRequestOverride.getOrElse(childRequestsFrom(rootPayload, budget)),
        childProbeResults = childProbeResults,
        childCacheHits = childCacheHits,
        loweringBinding = loweringBinding,
        nowEpochMs = nowEpochMs,
        budget = budget,
        loweringPolicy = CandidateLineEvidenceLowering.Policy.Default
      )
    )

  private def childRequestsFrom(
      payload: CandidateProbeResultPayload,
      budget: CandidateProbeBudget = CandidateProbeBudget.Default
  ): Vector[CandidateProbeRequest] =
    val rootPacket = CandidateProbeControlledAdapter.rootPacketFrom(payload, CandidateLineProvenanceKind.EngineRoot).get
    val rootEvidence =
      CandidateLinePacketHandoff.normalize(
        CandidateLinePacketHandoffInput(initialFen, nodeId, 0, rootPacket),
        nowEpochMs
      )
    CandidateProbePlan.childStage(rootEvidence, budget, nowEpochMs = nowEpochMs).requests

  private def payloadLine(branchId: String, uciLine: Vector[String], rank: Int = 1, multiPvIndex: Int = 1): CandidateProbeResultLine =
    CandidateProbeResultLine(
      branchId = CandidateBranchId(branchId),
      rank = rank,
      multiPvIndex = multiPvIndex,
      uciLine = uciLine
    )

  private def cacheChildPayload(request: CandidateProbeRequest): CandidateProbeResultPayload =
    CandidateProbeResultPayload(
      request = request,
      lines = childPayloadLines(request, "defender-resource-cache"),
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

  private def cacheLookup(
      request: CandidateProbeRequest,
      payload: CandidateProbeResultPayload,
      writeSource: CandidateProbeCacheWriteSource = CandidateProbeCacheWriteSource.EngineProbe
  ): CandidateProbeCacheLookupResult =
    CandidateProbeCacheLookupResult(
      request = request,
      entry = CandidateProbeCacheEntry(
        key = CandidateProbeCacheKey.fromRequest(
          request = request,
          realizedDepth = payload.realizedDepth,
          generatedAtEpochMs = payload.generatedAtEpochMs,
          maxAgeMs = payload.maxAgeMs
        ),
        writeSource = writeSource
      ),
      payload = payload
    )

  private val binding =
    CandidateLineEvidenceLowering.Binding(
      boundClaimId = "probe-adapter-claim",
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
      id = "probe-adapter-claim",
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
