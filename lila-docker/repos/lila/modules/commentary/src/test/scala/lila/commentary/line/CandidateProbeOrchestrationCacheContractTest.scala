package lila.commentary.line

import play.api.libs.json.Json

import lila.commentary.api.*
import lila.commentary.api.CommentaryApiJson.given
import lila.commentary.render.CommentaryRenderer
import lila.commentary.selection.*

class CandidateProbeOrchestrationCacheContractTest extends munit.FunSuite:

  private val initialFen = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1"
  private val afterE4Fen = "rnbqkbnr/pppppppp/8/8/4P3/8/PPPP1PPP/RNBQKBNR b KQkq - 0 1"
  private val afterD4Fen = "rnbqkbnr/pppppppp/8/8/3P4/8/PPP1PPPP/RNBQKBNR b KQkq - 0 1"
  private val nodeId = "mainline:0"
  private val nowEpochMs = 20_000L

  test("default root probe plan requests MultiPV 3 at target depth 18 with floor 16"):
    val plan =
      CandidateProbePlan.rootStage(
        startFen = initialFen,
        nodeId = nodeId,
        ply = 0,
        variant = "standard",
        engineFingerprint = "stockfish-test"
      )

    assertEquals(plan.requests.size, 1)
    val root = plan.requests.head
    assertEquals(root.role, CandidateProbeRole.RootCandidate)
    assertEquals(root.startFen, initialFen)
    assertEquals(root.nodeId, nodeId)
    assertEquals(root.ply, 0)
    assertEquals(root.targetDepth, 18)
    assertEquals(root.floorDepth, 16)
    assertEquals(root.multiPv, 3)
    assertNotEquals(root.multiPv, 2)
    assertNotEquals(root.multiPv, 4)
    assertEquals(root.probeSource, CandidateProbeSource.EngineProbe)

  test("frozen budget rejects root MultiPV 2 or 4 and child MultiPV 3"):
    intercept[IllegalArgumentException]:
      CandidateProbeBudget.Default.copy(rootMultiPv = 2)
    intercept[IllegalArgumentException]:
      CandidateProbeBudget.Default.copy(rootMultiPv = 4)
    intercept[IllegalArgumentException]:
      CandidateProbeBudget.Default.copy(childMultiPv = 3)

  test("default child probe plan creates defender requests only for root ranks 1 and 2"):
    val plan =
      CandidateProbePlan.childStage(
        rootEvidence = Vector(
          rootEvidence("root-candidate-1", rank = 1, lineUci = Vector("e2e4", "e7e5")),
          rootEvidence("root-candidate-2", rank = 2, lineUci = Vector("d2d4", "d7d5")),
          rootEvidence("root-candidate-3", rank = 3, lineUci = Vector("g1f3", "g8f6"))
        ),
        budget = CandidateProbeBudget.Default
      )

    assertEquals(plan.requests.map(_.parentBranchId.map(_.value)), Vector(Some("root-candidate-1"), Some("root-candidate-2")))
    assertEquals(plan.requests.map(_.parentLinePrefix), Vector(Vector("e2e4"), Vector("d2d4")))
    assertEquals(plan.requests.map(_.startFen), Vector(afterE4Fen, afterD4Fen))
    assertEquals(plan.requests.map(_.role), Vector(CandidateProbeRole.DefenderResource, CandidateProbeRole.DefenderResource))
    assertEquals(plan.requests.map(_.multiPv), Vector(2, 2))
    assertEquals(plan.requests.map(_.targetDepth), Vector(18, 18))
    assertEquals(plan.requests.map(_.floorDepth), Vector(16, 16))

  test("rank 3 child request is absent by default but appears with expanded budget"):
    val roots =
      Vector(
        rootEvidence("root-candidate-1", rank = 1, lineUci = Vector("e2e4", "e7e5")),
        rootEvidence("root-candidate-2", rank = 2, lineUci = Vector("d2d4", "d7d5")),
        rootEvidence("root-candidate-3", rank = 3, lineUci = Vector("g1f3", "g8f6"))
      )

    val defaultPlan = CandidateProbePlan.childStage(roots, CandidateProbeBudget.Default)
    val expandedPlan = CandidateProbePlan.childStage(roots, CandidateProbeBudget.Default.copy(allowExpandedThirdRootChildProbe = true))

    assertEquals(defaultPlan.requests.exists(_.parentBranchId.contains(CandidateBranchId("root-candidate-3"))), false)
    assertEquals(expandedPlan.requests.exists(_.parentBranchId.contains(CandidateBranchId("root-candidate-3"))), true)
    assertEquals(expandedPlan.requests.last.probeSource, CandidateProbeSource.EngineProbe)

  test("child probe plan does not derive children from shallow or stale root evidence"):
    val shallowPlan =
      CandidateProbePlan.childStage(
        rootEvidence = Vector(rootEvidence("root-candidate-1", rank = 1, realizedDepth = 15)),
        budget = CandidateProbeBudget.Default,
        nowEpochMs = nowEpochMs
      )
    val stalePlan =
      CandidateProbePlan.childStage(
        rootEvidence = Vector(
          rootEvidence(
            "root-candidate-1",
            rank = 1,
            timing = CandidateLineTiming(generatedAtEpochMs = 1_000L, maxAgeMs = 1_000L)
          )
        ),
        budget = CandidateProbeBudget.Default,
        nowEpochMs = nowEpochMs
      )

    assertEquals(shallowPlan.requests, Vector.empty)
    assertEquals(stalePlan.requests, Vector.empty)

  test("child probe plan does not derive children from invalid root ranks"):
    val plan =
      CandidateProbePlan.childStage(
        rootEvidence = Vector(rootEvidence("root-candidate-0", rank = 0)),
        budget = CandidateProbeBudget.Default,
        nowEpochMs = nowEpochMs
      )

    assertEquals(plan.requests, Vector.empty)

  test("rank 3 child request appears from a valid cache hit without expanded budget"):
    val roots =
      Vector(
        rootEvidence("root-candidate-1", rank = 1, lineUci = Vector("e2e4", "e7e5")),
        rootEvidence("root-candidate-2", rank = 2, lineUci = Vector("d2d4", "d7d5")),
        rootEvidence("root-candidate-3", rank = 3, lineUci = Vector("g1f3", "g8f6"))
      )
    val rank3Request =
      CandidateProbePlan.childStage(roots, CandidateProbeBudget.Default.copy(allowExpandedThirdRootChildProbe = true)).requests.last
    val cacheEntry = cacheEntryFor(rank3Request.copy(targetDepth = CandidateProbeBudget.Default.strongCacheTargetDepth))

    val cachedPlan =
      CandidateProbePlan.childStage(
        rootEvidence = roots,
        budget = CandidateProbeBudget.Default,
        validCacheEntries = Vector(cacheEntry),
        nowEpochMs = nowEpochMs
      )

    val rank3 = cachedPlan.requests.find(_.parentBranchId.contains(CandidateBranchId("root-candidate-3"))).get
    assertEquals(rank3.probeSource, CandidateProbeSource.CacheHit)
    assertEquals(rank3.targetDepth, 20)

  test("rank 3 cache hit requires strong cache target depth 20"):
    val roots =
      Vector(
        rootEvidence("root-candidate-1", rank = 1, lineUci = Vector("e2e4", "e7e5")),
        rootEvidence("root-candidate-2", rank = 2, lineUci = Vector("d2d4", "d7d5")),
        rootEvidence("root-candidate-3", rank = 3, lineUci = Vector("g1f3", "g8f6"))
      )
    val rank3DefaultTargetRequest =
      CandidateProbePlan.childStage(roots, CandidateProbeBudget.Default.copy(allowExpandedThirdRootChildProbe = true)).requests.last
    val target18CacheEntry = cacheEntryFor(rank3DefaultTargetRequest)
    val target20CacheEntry = cacheEntryFor(
      rank3DefaultTargetRequest.copy(targetDepth = CandidateProbeBudget.Default.strongCacheTargetDepth)
    )

    val target18Plan =
      CandidateProbePlan.childStage(roots, CandidateProbeBudget.Default, Vector(target18CacheEntry), nowEpochMs)
    val target20Plan =
      CandidateProbePlan.childStage(roots, CandidateProbeBudget.Default, Vector(target20CacheEntry), nowEpochMs)

    assertEquals(target18Plan.requests.exists(_.parentBranchId.contains(CandidateBranchId("root-candidate-3"))), false)
    val rank3 = target20Plan.requests.find(_.parentBranchId.contains(CandidateBranchId("root-candidate-3"))).get
    assertEquals(rank3.targetDepth, 20)
    assertEquals(rank3.probeSource, CandidateProbeSource.CacheHit)

  test("main candidate without defender resource is insufficient for strong line explanation"):
    val withoutChild =
      CandidateProbeAssemblyResult(Vector(rootEvidence("root-candidate-1", rank = 1)), rootBindingValid = true)
    val withChild =
      CandidateProbeAssemblyResult(
        Vector(
          rootEvidence("root-candidate-1", rank = 1),
          childEvidence("defender-resource-1", CandidateBranchId("root-candidate-1"), CandidateBranchRole.DefenderResource)
        ),
        rootBindingValid = true
      )

    assertEquals(withoutChild.mainCandidateHasDefenderResource(nowEpochMs), false)
    assertEquals(withChild.mainCandidateHasDefenderResource(nowEpochMs), true)

  test("invalid root result prevents candidate-line assembly and invalid child result preserves valid root"):
    val validRoot =
      rootPacket(
        rawLines = Vector(rootLine("root-candidate-1", Vector("e2e4", "e7e5")))
      )
    val invalidRoot =
      rootPacket(
        startFen = "not a fen",
        rawLines = Vector(rootLine("root-candidate-1", Vector("e2e4")))
      )
    val invalidChild =
      childPacket(
        parentBranchId = CandidateBranchId("root-candidate-1"),
        startFen = afterE4Fen,
        parentLinePrefix = Vector("e2e4"),
        rawLines = Vector(childLine("defender-resource-1", CandidateBranchId("root-candidate-1"), Vector("not-uci")))
      )

    val noEvidence =
      CandidateProbeAssemblyResult.fromPackets(initialFen, nodeId, 0, invalidRoot, Vector.empty, nowEpochMs)
    val partialEvidence =
      CandidateProbeAssemblyResult.fromPackets(initialFen, nodeId, 0, validRoot, Vector(invalidChild), nowEpochMs)

    assertEquals(noEvidence.evidence, Vector.empty)
    assertEquals(noEvidence.rootBindingValid, false)
    assertEquals(partialEvidence.evidence.exists(_.role == CandidateBranchRole.RootCandidate), true)
    assertEquals(partialEvidence.evidence.exists(_.role == CandidateBranchRole.DefenderResource), true)
    assertEquals(partialEvidence.evidence.filter(_.role == CandidateBranchRole.DefenderResource).forall(!_.publicLineEligibleAt(nowEpochMs)), true)

  test("cache key changes across FEN binding engine depth MultiPV role and parent prefix"):
    val request =
      CandidateProbePlan
        .rootStage(initialFen, nodeId, 0, "standard", "stockfish-test")
        .requests
        .head
    val key = CandidateProbeCacheKey.fromRequest(request, realizedDepth = 18, generatedAtEpochMs = 10_000L, maxAgeMs = 20_000L)

    assertNotEquals(key.copy(normalizedFen = afterE4Fen), key)
    assertNotEquals(key.copy(variant = "chess960"), key)
    assertNotEquals(key.copy(nodeId = "other-node"), key)
    assertNotEquals(key.copy(ply = 1), key)
    assertNotEquals(key.copy(engineFingerprint = "other-engine"), key)
    assertNotEquals(key.copy(targetDepth = 20), key)
    assertNotEquals(key.copy(floorDepth = 18), key)
    assertNotEquals(key.copy(realizedDepth = 20), key)
    assertNotEquals(key.copy(multiPv = 2), key)
    assertNotEquals(key.copy(role = CandidateProbeRole.DefenderResource), key)
    assertNotEquals(key.copy(parentLinePrefix = Vector("e2e4")), key)

  test("cache hit rejects stale under-depth wrong-FEN wrong-engine and wrong-parent-prefix entries"):
    val childRequest =
      CandidateProbePlan
        .childStage(Vector(rootEvidence("root-candidate-1", rank = 1, lineUci = Vector("e2e4", "e7e5"))), CandidateProbeBudget.Default)
        .requests
        .head
    val accepted = cacheEntryFor(childRequest)

    assertEquals(accepted.revalidateFor(childRequest, nowEpochMs).accepted, true)
    assertEquals(accepted.copy(key = accepted.key.copy(generatedAtEpochMs = 1_000L, maxAgeMs = 1_000L)).revalidateFor(childRequest, nowEpochMs).accepted, false)
    assertEquals(accepted.copy(key = accepted.key.copy(realizedDepth = 15)).revalidateFor(childRequest, nowEpochMs).accepted, false)
    assertEquals(accepted.copy(key = accepted.key.copy(normalizedFen = initialFen)).revalidateFor(childRequest, nowEpochMs).accepted, false)
    assertEquals(accepted.copy(key = accepted.key.copy(engineFingerprint = "other-engine")).revalidateFor(childRequest, nowEpochMs).accepted, false)
    assertEquals(accepted.copy(key = accepted.key.copy(parentLinePrefix = Vector("d2d4"))).revalidateFor(childRequest, nowEpochMs).accepted, false)

  test("source and retrieval cannot populate proof cache or candidate branch proof"):
    assertEquals(CandidateProbeCacheWritePolicy.canPopulateProofCache(CandidateProbeCacheWriteSource.EngineProbe), true)
    assertEquals(CandidateProbeCacheWritePolicy.canPopulateProofCache(CandidateProbeCacheWriteSource.SourceContext), false)
    assertEquals(CandidateProbeCacheWritePolicy.canPopulateProofCache(CandidateProbeCacheWriteSource.Retrieval), false)
    assertEquals(CandidateProbeCacheWritePolicy.canPopulateProofCache(CandidateProbeCacheWriteSource.SourceRow), false)

    val roots =
      Vector(
        rootEvidence("root-candidate-1", rank = 1, lineUci = Vector("e2e4", "e7e5")),
        rootEvidence("root-candidate-2", rank = 2, lineUci = Vector("d2d4", "d7d5")),
        rootEvidence("root-candidate-3", rank = 3, lineUci = Vector("g1f3", "g8f6"))
      )
    val rank3Request =
      CandidateProbePlan.childStage(roots, CandidateProbeBudget.Default.copy(allowExpandedThirdRootChildProbe = true)).requests.last
    val sourceCacheEntry = cacheEntryFor(rank3Request).copy(writeSource = CandidateProbeCacheWriteSource.SourceContext)
    val retrievalCacheEntry = cacheEntryFor(rank3Request).copy(writeSource = CandidateProbeCacheWriteSource.Retrieval)

    assertEquals(sourceCacheEntry.revalidateFor(rank3Request, nowEpochMs).accepted, false)
    assertEquals(retrievalCacheEntry.revalidateFor(rank3Request, nowEpochMs).accepted, false)
    assertEquals(
      CandidateProbePlan
        .childStage(roots, CandidateProbeBudget.Default, Vector(sourceCacheEntry, retrievalCacheEntry), nowEpochMs)
        .requests
        .exists(_.parentBranchId.contains(CandidateBranchId("root-candidate-3"))),
      false
    )

  test("probe planner alone emits no prepared variation evidence"):
    val plan =
      CandidateProbePlan.childStage(
        rootEvidence = Vector(rootEvidence("root-candidate-1", rank = 1, lineUci = Vector("e2e4", "e7e5"))),
        budget = CandidateProbeBudget.Default
      )

    assertEquals(plan.productElementNames.toVector, Vector("requests"))
    assertEquals(plan.requests.exists(_.productElementNames.exists(_ == "proofId")), false)
    assertEquals(plan.requests.exists(_.productElementNames.exists(_ == "boundClaimId")), false)

  test("assembled probe result feeds packet handoff and lowering without exposing raw packets"):
    val parent = CandidateBranchId("root-candidate-1")
    val assembly =
      CandidateProbeAssemblyResult.fromPackets(
        currentFen = initialFen,
        nodeId = nodeId,
        ply = 0,
        root = rootPacket(
          rawLines = Vector(
            rootLine(parent.value, Vector("e2e4", "e7e5"), rank = 1, multiPvIndex = 1),
            rootLine("root-candidate-2", Vector("d2d4", "d7d5"), rank = 2, multiPvIndex = 2),
            rootLine("root-candidate-3", Vector("g1f3", "g8f6"), rank = 3, multiPvIndex = 3)
          ),
          multiPv = 3
        ),
        children = Vector(
          childPacket(
            parentBranchId = parent,
            startFen = afterE4Fen,
            parentLinePrefix = Vector("e2e4"),
            rawLines = Vector(
              childLine("defender-resource-1", parent, Vector("c7c5", "g1f3"), rank = 1, multiPvIndex = 1),
              childLine("defender-resource-2", parent, Vector("e7e5", "g1f3"), rank = 2, multiPvIndex = 2)
            ),
            multiPv = 2
          )
        ),
        nowEpochMs = nowEpochMs
      )
    val proofs =
      CandidateLineEvidenceLowering.lower(
        assembly.evidence,
        binding = binding,
        nowEpochMs = nowEpochMs
      )

    assertEquals(assembly.mainCandidateHasDefenderResource(nowEpochMs), true)
    assertEquals(proofs.exists(_.role == VariationEvidenceRole.DefenderResource), true)
    assertEquals(proofs.exists(_.debug.nonEmpty), false)

  test("probe plan and cache internals do not leak through public backend or render JSON"):
    val proof =
      CandidateLineEvidenceLowering
        .lower(
          Vector(
            rootEvidence("root-candidate-1", rank = 1, multiPvIndex = 1),
            rootEvidence("root-candidate-2", rank = 2, multiPvIndex = 2),
            rootEvidence("root-candidate-3", rank = 3, multiPvIndex = 3)
          ),
          binding = binding,
          nowEpochMs = nowEpochMs
        )
        .head
    val claim = boardClaim(Vector(proof))
    val responseText =
      Json
        .toJson(CommentaryBackendSeam.withClaimProvider(_ => Vector(claim), nowEpochMs = () => nowEpochMs).renderDebug(request()))
        .toString
    val renderText = Json.toJson(CommentaryRenderer.render(CommentaryOutlineBuilder.build(ClaimSelector.select(Vector(claim))))).toString

    Vector(
      "CandidateProbePlan",
      "CandidateProbeRequest",
      "CandidateProbeCacheKey",
      "CandidateProbeCacheEntry",
      "engineFingerprint",
      "parentLinePrefix",
      "probeSource",
      "stockfish-test"
    ).foreach: token =>
      assert(!responseText.contains(token), clues(token, responseText))
      assert(!renderText.contains(token), clues(token, renderText))

  private val binding =
    CandidateLineEvidenceLowering.Binding(
      boundClaimId = "probe-line-claim",
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

  private def cacheEntryFor(request: CandidateProbeRequest): CandidateProbeCacheEntry =
    CandidateProbeCacheEntry(
      key = CandidateProbeCacheKey.fromRequest(request, realizedDepth = request.targetDepth, generatedAtEpochMs = 10_000L, maxAgeMs = 20_000L),
      writeSource = CandidateProbeCacheWriteSource.EngineProbe
    )

  private def rootEvidence(
      branchId: String,
      rank: Int,
      multiPv: Int = 3,
      multiPvIndex: Int = 1,
      lineUci: Vector[String] = Vector("e2e4", "e7e5"),
      realizedDepth: Int = 18,
      timing: CandidateLineTiming = CandidateLineTiming(generatedAtEpochMs = 10_000L, maxAgeMs = 20_000L)
  ): CandidateLineEvidence =
    evidence(
      branchId = branchId,
      parentBranchId = None,
      role = CandidateBranchRole.RootCandidate,
      rank = rank,
      multiPv = multiPv,
      multiPvIndex = multiPvIndex,
      startFen = initialFen,
      startPly = 0,
      lineUci = lineUci,
      realizedDepth = realizedDepth,
      timing = timing
    )

  private def childEvidence(
      branchId: String,
      parentBranchId: CandidateBranchId,
      role: CandidateBranchRole
  ): CandidateLineEvidence =
    evidence(
      branchId = branchId,
      parentBranchId = Some(parentBranchId),
      role = role,
      rank = 1,
      multiPv = 2,
      multiPvIndex = 1,
      startFen = afterE4Fen,
      startPly = 1,
      lineUci = Vector("c7c5", "g1f3"),
      realizedDepth = 18,
      timing = CandidateLineTiming(generatedAtEpochMs = 10_000L, maxAgeMs = 20_000L)
    )

  private def evidence(
      branchId: String,
      parentBranchId: Option[CandidateBranchId],
      role: CandidateBranchRole,
      rank: Int,
      multiPv: Int,
      multiPvIndex: Int,
      startFen: String,
      startPly: Int,
      lineUci: Vector[String],
      realizedDepth: Int,
      timing: CandidateLineTiming
  ): CandidateLineEvidence =
    CandidateLineEvidence(
      branchId = CandidateBranchId(branchId),
      parentBranchId = parentBranchId,
      role = role,
      rank = rank,
      multiPvIndex = multiPvIndex,
      startFen = startFen,
      startNodeId = nodeId,
      startPly = startPly,
      sideToMove = if startFen.contains(" b ") then CandidateLineSide.Black else CandidateLineSide.White,
      lineUci = lineUci,
      lineSan = lineUci,
      requestedDepth = 18,
      realizedDepth = realizedDepth,
      multiPv = multiPv,
      timing = timing,
      engineConfigFingerprint = "stockfish-test",
      legalReplay = CandidateLineReplayStatus.Legal,
      provenance = CandidateLineProvenance(if parentBranchId.isEmpty then CandidateLineProvenanceKind.EngineRoot else CandidateLineProvenanceKind.EngineChild),
      surfaceAllowance = VariationSurfaceAllowance.PublicLine,
      wordingCap = WordingStrength.QualifiedSupport
    )

  private def rootPacket(
      startFen: String = initialFen,
      rawLines: Vector[CandidateLineRawLine],
      multiPv: Int = 1
  ): CandidateLinePacket =
    CandidateLinePacket(
      startFen = startFen,
      nodeId = nodeId,
      ply = 0,
      rawLines = rawLines,
      requestedDepth = 18,
      realizedDepth = 18,
      multiPv = multiPv,
      generatedAtEpochMs = 10_000L,
      maxAgeMs = 20_000L,
      engineConfigFingerprint = "stockfish-test"
    )

  private def childPacket(
      parentBranchId: CandidateBranchId,
      startFen: String,
      parentLinePrefix: Vector[String],
      rawLines: Vector[CandidateLineRawLine],
      multiPv: Int = 1
  ): CandidateLineChildPacket =
    CandidateLineChildPacket(
      parentBranchId = parentBranchId,
      startFen = startFen,
      nodeId = nodeId,
      ply = parentLinePrefix.size,
      role = CandidateBranchRole.DefenderResource,
      parentLinePrefix = parentLinePrefix,
      rawLines = rawLines,
      requestedDepth = 18,
      realizedDepth = 18,
      multiPv = multiPv,
      generatedAtEpochMs = 10_000L,
      maxAgeMs = 20_000L,
      engineConfigFingerprint = "stockfish-test"
    )

  private def rootLine(
      branchId: String,
      uciLine: Vector[String],
      rank: Int = 1,
      multiPvIndex: Int = 1
  ): CandidateLineRawLine =
    CandidateLineRawLine(
      branchId = CandidateBranchId(branchId),
      parentBranchId = None,
      role = CandidateBranchRole.RootCandidate,
      rank = rank,
      multiPvIndex = multiPvIndex,
      uciLine = uciLine,
      provenance = CandidateLineProvenance(CandidateLineProvenanceKind.EngineRoot)
    )

  private def childLine(
      branchId: String,
      parentBranchId: CandidateBranchId,
      uciLine: Vector[String],
      rank: Int = 1,
      multiPvIndex: Int = 1
  ): CandidateLineRawLine =
    CandidateLineRawLine(
      branchId = CandidateBranchId(branchId),
      parentBranchId = Some(parentBranchId),
      role = CandidateBranchRole.DefenderResource,
      rank = rank,
      multiPvIndex = multiPvIndex,
      uciLine = uciLine,
      provenance = CandidateLineProvenance(CandidateLineProvenanceKind.EngineChild)
    )

  private def boardClaim(variationEvidence: Vector[PreparedVariationEvidence]): CommentaryClaim =
    CommentaryClaim(
      id = "probe-line-claim",
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
