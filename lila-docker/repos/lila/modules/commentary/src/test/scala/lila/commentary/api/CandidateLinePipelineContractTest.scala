package lila.commentary.api

import play.api.libs.json.Json

import lila.commentary.api.CommentaryApiJson.given
import lila.commentary.line.*
import lila.commentary.render.RenderLineRole
import lila.commentary.selection.*

class CandidateLinePipelineContractTest extends munit.FunSuite:

  private val initialFen = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1"
  private val nodeId = "mainline:0"
  private val nowEpochMs = 20_000L
  private val engine = "stockfish-pipeline-contract"
  private val claimId = "candidate-line-pipeline-claim"

  test("sanitized root and child probe payloads reach backend render only as public-safe bound variation evidence"):
    val assembly = completeAssembly()
    val seam =
      CommentaryBackendSeam.withClaimProviderAndCandidateLineAssembly(
        _ => Vector(boundClaim(claimId)),
        _ => Some(assembly),
        nowEpochMs = () => nowEpochMs
      )

    val response = seam.render(request())
    val responseText = Json.toJson(response).toString
    val rendered = response.render.variationEvidence

    assertEquals(response.status, CommentaryResponseStatus.Rendered)
    assertEquals(assembly.candidateLineEvidence.count(_.role == CandidateBranchRole.RootCandidate), 3)
    assertEquals(assembly.candidateLineEvidence.count(_.role == CandidateBranchRole.DefenderResource), 4)
    assertEquals(assembly.cacheWrites.map(_.key.role), Vector(CandidateProbeRole.RootCandidate, CandidateProbeRole.DefenderResource, CandidateProbeRole.DefenderResource))
    assertEquals(rendered.map(_.boundClaimId).distinct, Vector(claimId))
    assertEquals(rendered.map(_.role).count(_ == RenderLineRole.Pressure), 1)
    assertEquals(rendered.map(_.role).count(_ == RenderLineRole.Resource), 4)
    assertEquals(rendered.exists(evidence => evidence.lineSan == Vector("e4", "e5") && evidence.lineUci == Vector("e2e4", "e7e5")), true)
    assertEquals(rendered.exists(evidence => evidence.lineSan == Vector("d4", "d5") && evidence.lineUci == Vector("d2d4", "d7d5")), false)
    assertEquals(rendered.exists(evidence => evidence.lineSan == Vector("Nf3", "Nf6") && evidence.lineUci == Vector("g1f3", "g8f6")), false)
    assertEquals(rendered.exists(evidence => evidence.role == RenderLineRole.Resource && evidence.lineSan == Vector("c5", "Nf3")), true)
    assertEquals(response.render.blocks.head.variationEvidenceIds, rendered.map(_.proofId))
    assertEquals(rendered.flatMap(_.provenanceRefs).map(_.id).distinct, Vector(binding.provenanceRef.id))
    assertEquals(rendered.flatMap(_.provenanceRefs).forall(_.kind == EvidenceRefKind.Certification), true)
    assertEquals(rendered.forall(_.boundary.depthFloor == 16), true)
    assertEquals(rendered.filter(_.role == RenderLineRole.Pressure).forall(_.boundary.multiPv == 3), true)
    assertEquals(rendered.filter(_.role == RenderLineRole.Resource).forall(_.boundary.multiPv == 2), true)
    Vector(
      "CandidateLineEvidence",
      "CandidateProbeResultPayload",
      "CandidateProbeControlledAdapter",
      "branchId",
      "parentBranchId",
      "multiPvIndex",
      "engineFingerprint",
      engine,
      "rawLines",
      "cacheKey",
      "source_hint",
      "retrieval"
    ).foreach(token => assert(!responseText.contains(token), clues(token, responseText)))

  test("candidate-line payloads stay quiet without a matching safe claim"):
    val assembly = completeAssembly()
    val noClaim =
      CommentaryBackendSeam
        .withClaimProviderAndCandidateLineAssembly(_ => Vector.empty, _ => Some(assembly), nowEpochMs = () => nowEpochMs)
        .render(request())
    val mismatchedClaim =
      CommentaryBackendSeam
        .withClaimProviderAndCandidateLineAssembly(_ => Vector(boundClaim("other-claim")), _ => Some(assembly), nowEpochMs = () => nowEpochMs)
        .render(request())
    val sourceOwned =
      CommentaryBackendSeam
        .withClaimProviderAndCandidateLineAssembly(_ => Vector(sourceContextClaim(claimId)), _ => Some(assembly), nowEpochMs = () => nowEpochMs)
        .render(request())

    Vector(noClaim, mismatchedClaim, sourceOwned).foreach: response =>
      assertEquals(response.render.variationEvidence, Vector.empty)
      assertEquals(response.render.blocks.flatMap(_.variationEvidenceIds), Vector.empty)
    assertEquals(noClaim.noCommentary, true)

  test("stale shallow illegal duplicate-root wrong-node and wrong-binding failures produce no public variation evidence"):
    val badAssemblies = Vector(
      completeAssembly(rootPayload = rootPayload.copy(generatedAtEpochMs = 1_000L, maxAgeMs = 1_000L)),
      completeAssembly(rootPayload = rootPayload.copy(realizedDepth = 15)),
      completeAssembly(rootPayload = rootPayload.copy(lines = rootLines.updated(0, payloadLine("root-candidate-1", Vector("not-uci"), 1, 1)))),
      completeAssembly(rootPayload = rootPayload.copy(lines = rootLines.updated(1, payloadLine("root-candidate-2", Vector("e2e4", "e7e5"), 2, 2)))),
      completeAssembly(rootRequest = rootRequest.copy(nodeId = "other-node")),
      completeAssembly(loweringBinding = badBinding)
    )

    badAssemblies.foreach: assembly =>
      val response =
        CommentaryBackendSeam
          .withClaimProviderAndCandidateLineAssembly(_ => Vector(boundClaim(claimId)), _ => Some(assembly), nowEpochMs = () => nowEpochMs)
          .render(request())
      assertEquals(response.render.variationEvidence, Vector.empty)
      assert(!Json.toJson(response).toString.contains("fallback"), clues(Json.toJson(response).toString))

  private def completeAssembly(
      rootRequest: CandidateProbeRequest = rootRequest,
      rootPayload: CandidateProbeResultPayload = rootPayload,
      loweringBinding: CandidateLineEvidenceLowering.Binding = binding
  ): CandidateProbeControlledAdapter.AssemblyResult =
    val rootPacket = CandidateProbeControlledAdapter.rootPacketFrom(rootPayload, CandidateLineProvenanceKind.EngineRoot)
    val rootEvidence =
      rootPacket.toVector.flatMap(packet =>
        CandidateLinePacketHandoff.normalize(
          CandidateLinePacketHandoffInput(initialFen, nodeId, 0, packet),
          nowEpochMs
        )
      )
    val childRequests = CandidateProbePlan.childStage(rootEvidence, nowEpochMs = nowEpochMs).requests
    val childProbeResults =
      childRequests.filter(_.parentRootRank.exists(rank => rank == 1 || rank == 2)).map: request =>
        CandidateProbeResultPayload(
          request = request,
          lines = childLinesFor(request),
          realizedDepth = 18,
          generatedAtEpochMs = 10_000L,
          maxAgeMs = 20_000L
        )
    CandidateProbeControlledAdapter.assemble(
      CandidateProbeControlledAdapter.Input(
        currentFen = initialFen,
        nodeId = nodeId,
        ply = 0,
        variant = "standard",
        rootRequest = rootRequest,
        rootProbeResult = Some(rootPayload.copy(request = rootRequest)),
        rootCacheHit = None,
        childRequests = childRequests,
        childProbeResults = childProbeResults,
        childCacheHits = Vector.empty,
        loweringBinding = Some(loweringBinding),
        nowEpochMs = nowEpochMs,
        budget = CandidateProbeBudget.Default,
        loweringPolicy = CandidateLineEvidenceLowering.Policy.Default
      )
    )

  private lazy val rootRequest: CandidateProbeRequest =
    CandidateProbePlan.rootStage(initialFen, nodeId, 0, "standard", engine).requests.head

  private lazy val rootPayload: CandidateProbeResultPayload =
    CandidateProbeResultPayload(
      request = rootRequest,
      lines = rootLines,
      realizedDepth = 18,
      generatedAtEpochMs = 10_000L,
      maxAgeMs = 20_000L
    )

  private def rootLines: Vector[CandidateProbeResultLine] =
    Vector(
      payloadLine("root-candidate-1", Vector("e2e4", "e7e5"), 1, 1),
      payloadLine("root-candidate-2", Vector("d2d4", "d7d5"), 2, 2),
      payloadLine("root-candidate-3", Vector("g1f3", "g8f6"), 3, 3)
    )

  private def childLinesFor(request: CandidateProbeRequest): Vector[CandidateProbeResultLine] =
    request.parentBranchId.map(_.value) match
      case Some("root-candidate-1") =>
        Vector(
          payloadLine("defender-resource-e4-1", Vector("c7c5", "g1f3"), 1, 1),
          payloadLine("defender-resource-e4-2", Vector("e7e5", "g1f3"), 2, 2)
        )
      case Some("root-candidate-2") =>
        Vector(
          payloadLine("defender-resource-d4-1", Vector("d7d5", "c2c4"), 1, 1),
          payloadLine("defender-resource-d4-2", Vector("g8f6", "c2c4"), 2, 2)
        )
      case _ => Vector.empty

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

  private def request(): CommentaryRequest =
    CommentaryRequest(
      currentFen = initialFen,
      beforeFen = None,
      playedMove = None,
      nodeId = nodeId,
      ply = 0
    )

  private def boundClaim(id: String): CommentaryClaim =
    CommentaryClaim(
      id = id,
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
      wordingStrengthCap = WordingStrength.QualifiedSupport
    )

  private def sourceContextClaim(id: String): CommentaryClaim =
    boundClaim(id).copy(
      layer = ClaimLayer.SourceContext,
      status = ClaimStatus.Context,
      evidenceRefs = Vector(EvidenceRef(EvidenceRefKind.SourceContext, "retrieval-line-test:example:context")),
      wordingStrengthCap = WordingStrength.ContextOnly,
      sourceContextKind = Some(SourceContextKind.Retrieval)
    )

  private val binding =
    CandidateLineEvidenceLowering.Binding(
      boundClaimId = claimId,
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

  private val badBinding =
    binding.copy(
      provenanceRef = EvidenceRef(
        EvidenceRefKind.SourceContext,
        "retrieval-line-test:bad:context",
        Some("white"),
        Some("board"),
        Some("pressure_route"),
        Some("position_local")
      )
    )
