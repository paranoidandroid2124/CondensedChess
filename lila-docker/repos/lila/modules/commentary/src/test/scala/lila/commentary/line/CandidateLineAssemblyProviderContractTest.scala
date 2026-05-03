package lila.commentary.line

import play.api.libs.json.Json

import lila.commentary.api.*
import lila.commentary.api.CommentaryApiJson.given
import lila.commentary.selection.*

class CandidateLineAssemblyProviderContractTest extends munit.FunSuite:

  private val initialFen = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1"
  private val nodeId = "mainline:0"
  private val nowEpochMs = 20_000L
  private val engine = "stockfish-provider-contract"

  test("provider delegates completed root payload to controlled adapter and returns prepared variation evidence with binding"):
    val result = assemble(completedRootProbe = Some(rootPayload))

    assertEquals(result.rootPacket.nonEmpty, true)
    assertEquals(result.candidateLineEvidence.count(_.role == CandidateBranchRole.RootCandidate), 3)
    assertEquals(result.preparedVariationEvidence.map(_.boundClaimId).distinct, Vector(binding.boundClaimId))
    assertEquals(result.preparedVariationEvidence.exists(_.proves == "main_candidate_line"), true)
    assertEquals(result.preparedVariationEvidence.exists(_.lineUci == Vector("e2e4", "e7e5")), true)

  test("provider can assemble candidate-line evidence without lowering when no binding is supplied"):
    val result = assemble(completedRootProbe = Some(rootPayload), loweringBinding = None)

    assertEquals(result.candidateLineEvidence.count(_.role == CandidateBranchRole.RootCandidate), 3)
    assertEquals(result.preparedVariationEvidence, Vector.empty)

  test("provider omits root MultiPV 2 and child MultiPV 1 through controlled adapter policy"):
    val rootMultiPv2Request = rootRequest.copy(multiPv = 2)
    val rootMultiPv2Payload =
      rootPayload.copy(
        request = rootMultiPv2Request,
        lines = rootPayload.lines.take(2)
      )
    val rootRejected = assemble(completedRootProbe = Some(rootMultiPv2Payload))

    val childRequest = childRequestsFrom(rootPayload).head.copy(multiPv = 1)
    val childRejected =
      assemble(
        completedRootProbe = Some(rootPayload),
        completedChildProbes = Vector(
          CandidateProbeResultPayload(
            request = childRequest,
            lines = Vector(payloadLine("defender-resource-under-budget", Vector("c7c5", "g1f3"), 1, 1)),
            realizedDepth = 18,
            generatedAtEpochMs = 10_000L,
            maxAgeMs = 20_000L
          )
        )
      )

    assertEquals(rootRejected.candidateLineEvidence, Vector.empty)
    assertEquals(rootRejected.preparedVariationEvidence, Vector.empty)
    assertEquals(childRejected.candidateLineEvidence.exists(_.branchId == CandidateBranchId("defender-resource-under-budget")), false)
    assertEquals(childRejected.preparedVariationEvidence.exists(_.role == VariationEvidenceRole.DefenderResource), false)

  test("provider has no direct root cache-hit input"):
    val errors =
      compileErrors(
        "providerInput(completedRootProbe = None).copy(rootCacheHit = Some(cacheHit(rootRequest, rootPayload, CandidateProbeCacheWriteSource.RevalidatedCache)))"
      )

    assert(errors.contains("rootCacheHit"), clues(errors))

  test("provider has no direct child cache-hit input"):
    val errors =
      compileErrors(
        "providerInput(completedRootProbe = Some(rootPayload)).copy(childCacheHits = Vector.empty[CandidateProbeCacheLookupResult])"
      )

    assert(errors.contains("childCacheHits"), clues(errors))

  test("provider boundary does not create claims prose or public product fields"):
    val input = providerInput(completedRootProbe = Some(rootPayload))
    val result = CandidateLineAssemblyProvider.assemble(input)
    val productNames = (input.productElementNames ++ result.productElementNames).map(_.toLowerCase).toVector

    Vector("commentaryclaim", "claimprovider", "prose", "publicapi", "controller", "route", "frontend", "ui").foreach: token =>
      assertEquals(productNames.exists(_.contains(token)), false, clues(token, productNames))
    assert(!result.preparedVariationEvidence.exists(_.productElementNames.exists(_.toLowerCase.contains("prose"))))

  test("provider-shaped JSON is ignored by public CommentaryRequest and public response"):
    val rawRequest = Json.obj(
      "currentFen" -> initialFen,
      "nodeId" -> nodeId,
      "ply" -> 0,
      "candidateLineAssemblyProvider" -> Json.obj(
        "engineFingerprint" -> engine,
        "completedRootProbe" -> Json.obj("secret" -> "provider-secret-root"),
        "completedChildProbes" -> Json.arr(Json.obj("secret" -> "provider-secret-child")),
        "rootCacheHit" -> Json.obj("secret" -> "provider-secret-cache"),
        "childCacheHits" -> Json.arr(Json.obj("secret" -> "provider-secret-child-cache"))
      )
    )

    val response =
      CommentaryBackendSeam
        .withClaimProvider(_ => Vector.empty, nowEpochMs = () => nowEpochMs)
        .renderDebug(rawRequest.as[CommentaryRequest])
    val json = Json.toJson(response).toString

    Vector(
      "candidateLineAssemblyProvider",
      "completedRootProbe",
      "completedChildProbes",
      "rootCacheHit",
      "childCacheHits",
      "provider-secret-root",
      "provider-secret-child",
      "provider-secret-cache",
      "provider-secret-child-cache"
    ).foreach(token => assert(!json.contains(token), clues(token, json)))

  test("contract docs name the provider as internal-only and defer execution persistence controller UI and prose"):
    val variation = java.nio.file.Files.readString(java.nio.file.Paths.get("modules/commentary/docs/legacy-pre-semantic-reset/VariationEvidenceContract.md"))
    val core = java.nio.file.Files.readString(java.nio.file.Paths.get("modules/commentary/docs/legacy-pre-semantic-reset/CommentaryCoreSSOT.md"))
    val backend = java.nio.file.Files.readString(java.nio.file.Paths.get("modules/commentary/docs/legacy-pre-semantic-reset/CommentaryBackendSeamContract.md"))
    val validation = java.nio.file.Files.readString(java.nio.file.Paths.get("modules/commentary/docs/legacy-pre-semantic-reset/ValidationMethodology.md"))
    val docs = Vector(variation, core, backend, validation).mkString("\n")

    Vector(
      "CandidateLineAssemblyProvider",
      "internal-only",
      "does not execute Stockfish",
      "does not persist cache",
      "does not wire controller",
      "does not open product frontend UI",
      "does not write prose"
    ).foreach(token => assert(docs.contains(token), token))

  private def assemble(
      completedRootProbe: Option[CandidateProbeResultPayload],
      completedChildProbes: Vector[CandidateProbeResultPayload] = Vector.empty,
      loweringBinding: Option[CandidateLineEvidenceLowering.Binding] = Some(binding),
      budget: CandidateProbeBudget = CandidateProbeBudget.Default
  ): CandidateProbeControlledAdapter.AssemblyResult =
    CandidateLineAssemblyProvider.assemble(
      providerInput(
        completedRootProbe = completedRootProbe,
        completedChildProbes = completedChildProbes,
        loweringBinding = loweringBinding,
        budget = budget
      )
    )

  private def providerInput(
      completedRootProbe: Option[CandidateProbeResultPayload],
      completedChildProbes: Vector[CandidateProbeResultPayload] = Vector.empty,
      loweringBinding: Option[CandidateLineEvidenceLowering.Binding] = Some(binding),
      budget: CandidateProbeBudget = CandidateProbeBudget.Default
  ): CandidateLineAssemblyProvider.Input =
    CandidateLineAssemblyProvider.Input(
      currentFen = initialFen,
      nodeId = nodeId,
      ply = 0,
      variant = "standard",
      engineFingerprint = engine,
      budget = budget,
      completedRootProbe = completedRootProbe,
      completedChildProbes = completedChildProbes,
      loweringBinding = loweringBinding,
      nowEpochMs = nowEpochMs,
      loweringPolicy = CandidateLineEvidenceLowering.Policy.Default
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

  private def childRequestsFrom(payload: CandidateProbeResultPayload): Vector[CandidateProbeRequest] =
    val rootPacket = CandidateProbeControlledAdapter.rootPacketFrom(payload, CandidateLineProvenanceKind.EngineRoot).get
    val rootEvidence =
      CandidateLinePacketHandoff.normalize(
        CandidateLinePacketHandoffInput(initialFen, nodeId, 0, rootPacket),
        nowEpochMs
      )
    CandidateProbePlan.childStage(rootEvidence, nowEpochMs = nowEpochMs).requests

  def cacheHit(
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
      boundClaimId = "provider-claim",
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
