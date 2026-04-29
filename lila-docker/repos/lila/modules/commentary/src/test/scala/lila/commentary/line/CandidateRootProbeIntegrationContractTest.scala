package lila.commentary.line

import play.api.libs.json.Json

import lila.commentary.api.*
import lila.commentary.api.CommentaryApiJson.given
import lila.commentary.render.CommentaryRenderer
import lila.commentary.selection.*

class CandidateRootProbeIntegrationContractTest extends munit.FunSuite:

  private val initialFen = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1"
  private val nodeId = "mainline:0"
  private val nowEpochMs = 20_000L
  private val engine = "stockfish-root-integration"

  test("valid completed root payload yields three root evidences and schedules rank 1 and 2 child requests"):
    val result = integrate(completedRootProbe = Some(rootPayload), loweringBinding = None)

    val roots = result.assembly.candidateLineEvidence.filter(_.role == CandidateBranchRole.RootCandidate)
    assertEquals(roots.size, 3)
    assertEquals(roots.forall(_.publicLineEligibleAt(nowEpochMs)), true)
    assertEquals(result.plannedChildRequests.map(_.parentRootRank), Vector(Some(1), Some(2)))
    assertEquals(result.plannedChildRequests.map(_.multiPv), Vector(2, 2))
    assertEquals(result.plannedChildRequests.map(_.targetDepth), Vector(18, 18))
    assertEquals(result.plannedChildRequests.map(_.floorDepth), Vector(16, 16))
    assertEquals(result.plannedChildRequests.exists(_.parentRootRank.contains(3)), false)

  test("no lowering binding means no prepared variation evidence"):
    val result = integrate(completedRootProbe = Some(rootPayload), loweringBinding = None)

    assertEquals(result.assembly.candidateLineEvidence.count(_.role == CandidateBranchRole.RootCandidate), 3)
    assertEquals(result.assembly.preparedVariationEvidence, Vector.empty)

  test("explicit lowering binding lowers eligible root proof only through existing policy"):
    val result = integrate(completedRootProbe = Some(rootPayload), loweringBinding = Some(binding))

    assertEquals(result.assembly.preparedVariationEvidence.map(_.boundClaimId).distinct, Vector(binding.boundClaimId))
    assertEquals(result.assembly.preparedVariationEvidence.exists(_.proves == "main_candidate_line"), true)
    assertEquals(result.assembly.preparedVariationEvidence.exists(_.proofId.contains("root-candidate")), false)
    assertEquals(result.assembly.preparedVariationEvidence.exists(_.provenanceRefs.contains(binding.provenanceRef)), true)

  test("valid proof-cache root hit satisfies root path and schedules child requests without new writes"):
    val cache = CandidateLineProofCache.InMemory.empty
    val populate = integrate(completedRootProbe = Some(rootPayload), proofCache = Some(cache), loweringBinding = None)
    assertEquals(populate.assembly.cacheWrites.nonEmpty, true)

    val result = integrate(completedRootProbe = None, proofCache = Some(cache), loweringBinding = None)

    assertEquals(result.assembly.candidateLineEvidence.count(_.provenance.kind == CandidateLineProvenanceKind.Cache), 3)
    assertEquals(result.plannedChildRequests.map(_.parentRootRank), Vector(Some(1), Some(2)))
    assertEquals(result.assembly.cacheWrites, Vector.empty)

  test("permuted root rank and MultiPV index fails closed for direct probe and cache root hit"):
    val cache = fakeCache(permutedRootPayload, CandidateProbeCacheWriteSource.EngineProbe)

    Vector(
      integrate(completedRootProbe = Some(permutedRootPayload), loweringBinding = Some(binding)),
      integrate(completedRootProbe = None, proofCache = Some(cache), loweringBinding = Some(binding))
    ).foreach: result =>
      assertEquals(result.assembly.candidateLineEvidence, Vector.empty)
      assertEquals(result.plannedChildRequests, Vector.empty)
      assertEquals(result.assembly.preparedVariationEvidence, Vector.empty)
      assertEquals(result.assembly.cacheWrites, Vector.empty)

  test("invalid root payloads fail closed with no child requests prepared evidence or cache writes"):
    val invalidCases = Vector(
      rootPayload.copy(request = rootRequest.copy(startFen = initialFen.replace(" w ", " b "))),
      rootPayload.copy(request = rootRequest.copy(nodeId = "other-node")),
      rootPayload.copy(request = rootRequest.copy(ply = 1)),
      rootPayload.copy(request = rootRequest.copy(variant = "chess960")),
      rootPayload.copy(request = rootRequest.copy(engineFingerprint = "other-engine")),
      rootPayload.copy(generatedAtEpochMs = 1_000L, maxAgeMs = 1_000L),
      rootPayload.copy(realizedDepth = 15),
      rootPayload.copy(request = rootRequest.copy(multiPv = 2), lines = rootPayload.lines.take(2)),
      rootPayload.copy(lines = rootPayload.lines.take(2)),
      rootPayload.copy(lines = rootPayload.lines :+ payloadLine("root-candidate-4", Vector("c2c4", "c7c5"), 4, 4)),
      rootPayload.copy(lines =
        Vector(
          payloadLine("root-candidate-1", Vector("e2e4", "e7e5"), 1, 1),
          payloadLine("root-candidate-2", Vector("e2e4", "c7c5"), 2, 2),
          payloadLine("root-candidate-3", Vector("g1f3", "g8f6"), 3, 3)
        )
      ),
      rootPayload.copy(lines =
        Vector(
          payloadLine("root-candidate-1", Vector("not-uci"), 1, 1),
          payloadLine("root-candidate-2", Vector("d2d4", "d7d5"), 2, 2),
          payloadLine("root-candidate-3", Vector("g1f3", "g8f6"), 3, 3)
        )
      )
    )

    invalidCases.foreach: payload =>
      val result = integrate(completedRootProbe = Some(payload), loweringBinding = Some(binding))
      assertEquals(result.assembly.candidateLineEvidence, Vector.empty, clues(payload))
      assertEquals(result.plannedChildRequests, Vector.empty, clues(payload))
      assertEquals(result.assembly.preparedVariationEvidence, Vector.empty, clues(payload))
      assertEquals(result.assembly.cacheWrites, Vector.empty, clues(payload))

  test("source retrieval and root-hint provenance cannot become proof-owned root evidence"):
    val sourceCache = fakeCache(rootPayload, CandidateProbeCacheWriteSource.SourceContext)
    val retrievalCache = fakeCache(rootPayload, CandidateProbeCacheWriteSource.Retrieval)
    val sourceRowCache = fakeCache(rootPayload, CandidateProbeCacheWriteSource.SourceRow)

    Vector(sourceCache, retrievalCache, sourceRowCache).foreach: cache =>
      val result = integrate(completedRootProbe = None, proofCache = Some(cache), loweringBinding = Some(binding))
      assertEquals(result.assembly.candidateLineEvidence, Vector.empty)
      assertEquals(result.plannedChildRequests, Vector.empty)
      assertEquals(result.assembly.preparedVariationEvidence, Vector.empty)
      assertEquals(result.assembly.cacheWrites, Vector.empty)

  test("public backend and render JSON do not leak raw root probe payload or internal branch ids"):
    val proof = integrate(completedRootProbe = Some(rootPayload), loweringBinding = Some(binding)).assembly.preparedVariationEvidence.head
    val claim = boardClaim(Vector(proof))
    val responseText =
      Json
        .toJson(CommentaryBackendSeam.withClaimProvider(_ => Vector(claim), nowEpochMs = () => nowEpochMs).renderDebug(request()))
        .toString
    val renderText = Json.toJson(CommentaryRenderer.render(CommentaryOutlineBuilder.build(ClaimSelector.select(Vector(claim))))).toString

    Vector(
      "CandidateRootProbeIntegration",
      "CandidateProbeResultPayload",
      "completedRootProbe",
      "root-candidate-1",
      "root-candidate-2",
      "root-candidate-3",
      "engineFingerprint",
      engine,
      "rawLines"
    ).foreach: token =>
      assert(!responseText.contains(token), clues(token, responseText))
      assert(!renderText.contains(token), clues(token, renderText))

  private def integrate(
      completedRootProbe: Option[CandidateProbeResultPayload],
      proofCache: Option[CandidateLineProofCache] = None,
      loweringBinding: Option[CandidateLineEvidenceLowering.Binding],
      budget: CandidateProbeBudget = CandidateProbeBudget.Default
  ): CandidateRootProbeIntegration.Result =
    CandidateRootProbeIntegration.integrate(
      CandidateRootProbeIntegration.Input(
        currentFen = initialFen,
        nodeId = nodeId,
        ply = 0,
        variant = "standard",
        engineFingerprint = engine,
        budget = budget,
        completedRootProbe = completedRootProbe,
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

  private lazy val permutedRootPayload: CandidateProbeResultPayload =
    rootPayload.copy(lines =
      Vector(
        payloadLine("root-candidate-1", Vector("e2e4", "e7e5"), 1, 3),
        payloadLine("root-candidate-2", Vector("d2d4", "d7d5"), 2, 2),
        payloadLine("root-candidate-3", Vector("g1f3", "g8f6"), 3, 1)
      )
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
      payload: CandidateProbeResultPayload,
      source: CandidateProbeCacheWriteSource
  ): CandidateLineProofCache =
    new CandidateLineProofCache:
      override def read(request: CandidateProbeRequest, nowEpochMs: Long): Option[CandidateProbeCacheLookupResult] =
        Some(lookup(request, payload.copy(request = request), source))

      override def read(
          key: CandidateProbeCacheKey,
          request: CandidateProbeRequest,
          nowEpochMs: Long
      ): Option[CandidateProbeCacheLookupResult] =
        Some(lookup(request, payload.copy(request = request), source))

      override def commit(
          receipt: CandidateProbeControlledAdapter.CacheWriteReceipt,
          nowEpochMs: Long
      ): Vector[CandidateLineProofCache.WriteResult] =
        Vector.empty

  private def lookup(
      request: CandidateProbeRequest,
      payload: CandidateProbeResultPayload,
      source: CandidateProbeCacheWriteSource
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
        source
      ),
      payload = payload
    )

  private val binding =
    CandidateLineEvidenceLowering.Binding(
      boundClaimId = "root-integration-claim",
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
      id = "root-integration-claim",
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
