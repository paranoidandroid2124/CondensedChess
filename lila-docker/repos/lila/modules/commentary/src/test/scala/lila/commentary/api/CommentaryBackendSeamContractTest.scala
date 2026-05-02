package lila.commentary.api

import chess.format.Fen
import play.api.libs.json.Json

import lila.commentary.CommentaryCore
import lila.commentary.certification.{ CertificationEnginePolicyFingerprint, CertificationEngineRole, CertificationEngineRuntimeIntake, CertificationEvidenceBundle, EngineNodeIdentity }
import lila.commentary.line.*
import lila.commentary.render.{ PublicClaimPredicate, RenderLineRole, RenderRole, RenderStatus }
import lila.commentary.selection.*
import lila.commentary.api.CommentaryApiJson.given

class CommentaryBackendSeamContractTest extends munit.FunSuite:

  private val validFen = "r1bqkbnr/pppp1ppp/2n5/4p3/3PP3/2N2N2/PPP2PPP/R1BQKB1R b KQkq - 3 3"
  private val initialFen = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1"
  private val bareKingsFen = "8/8/8/8/8/8/4k3/7K w - - 0 1"
  private val nodeId = "mainline:0"
  private val nowEpochMs = 20_000L

  private def assert(condition: => Boolean)(using loc: munit.Location): Unit =
    super.assert(condition, clues(""))

  private def assertEquals[A, B](obtained: A, expected: B)(using
      loc: munit.Location,
      compare: munit.Compare[A, B],
      diffOptions: munit.diff.DiffOptions
  ): Unit =
    super.assertEquals(obtained, expected, clues(""))

  test("valid exact-board request returns structured CommentaryRender through selection outline render path"):
    val seam = CommentaryBackendSeam.withClaimProvider(_ => Vector(boardLead("api-selected-lead")))

    val response = seam.render(request())

    assertEquals(response.status, CommentaryResponseStatus.Rendered)
    assertEquals(response.render.status, RenderStatus.Rendered)
    assertEquals(response.render.blocks.map(_.role), Vector(RenderRole.Primary))
    assertEquals(response.render.blocks.map(_.claimId), Vector("api-selected-lead"))
    assertEquals(response.render.wording.maxStrength, WordingStrength.QualifiedSupport)
    assertEquals(response.noCommentary, false)

  test("malformed FEN fails closed without public commentary"):
    val response = CommentaryBackendSeam.render(request(currentFen = "not a fen"))

    assertEquals(response.status, CommentaryResponseStatus.InvalidRequest)
    assertEquals(response.render.status, RenderStatus.NoCommentary)
    assertEquals(response.render.blocks, Vector.empty)
    assertEquals(response.render.evidenceRefs, Vector.empty)
    assertEquals(response.noCommentary, true)

  test("transition request is all-or-nothing at the exact-board seam"):
    val response = CommentaryBackendSeam.render(
      CommentaryRequest(
        currentFen = validFen,
        beforeFen = Some("r1bqkbnr/pppp1ppp/2n5/4p3/3PP3/5N2/PPP2PPP/RNBQKB1R w KQkq - 2 3"),
        playedMove = None,
        nodeId = nodeId,
        ply = 0
      )
    )

    assertEquals(response.status, CommentaryResponseStatus.InvalidRequest)
    assertEquals(response.render.status, RenderStatus.NoCommentary)
    assertEquals(response.internal, None)

  test("malformed node identity fails closed without public commentary"):
    val emptyNode = CommentaryBackendSeam.render(request(nodeId = "  "))
    val negativePly = CommentaryBackendSeam.render(request(ply = -1))

    Vector(emptyNode, negativePly).foreach: response =>
      assertEquals(response.status, CommentaryResponseStatus.InvalidRequest)
      assertEquals(response.render.status, RenderStatus.NoCommentary)
      assertEquals(response.render.blocks, Vector.empty)
      assertEquals(response.render.evidenceRefs, Vector.empty)
      assertEquals(response.internal, None)

  test("stale or wrong-node engine packet is certification intake only and never becomes rendered evidence"):
    val staleEngine = enginePacket(packetNodeId = "other-node")
    val seam = CommentaryBackendSeam.withClaimProvider(_ => Vector(boardLead("api-selected-lead")))

    val response = seam.renderDebug(request(enginePacket = Some(staleEngine), debug = true))

    assertEquals(response.status, CommentaryResponseStatus.Rendered)
    assertEquals(response.render.evidenceRefs.exists(_.kind == EvidenceRefKind.RawEngine), false)
    assertEquals(response.render.evidenceRefs.exists(_.kind == EvidenceRefKind.EngineCertification), false)
    assert(response.internal.exists(_.engineIntake.exists(_.status == CommentaryEngineIntakeStatus.Rejected)))
    assertEquals(response.internal.flatMap(_.engineIntake.flatMap(_.reason)), Some("engine_intake_rejected"))

    val staleResponse =
      CommentaryBackendSeam
        .withClaimProvider(_ => Vector(boardLead("api-selected-lead")), nowEpochMs = () => 30_000L)
        .renderDebug(request(enginePacket = Some(enginePacket(packetNodeId = nodeId).copy(generatedAtEpochMs = 1_000L, maxAgeMs = 1_000L)), debug = true))
    assertEquals(staleResponse.render.evidenceRefs.exists(_.kind == EvidenceRefKind.EngineCertification), false)
    assertEquals(staleResponse.internal.flatMap(_.engineIntake.map(_.status)), Some(CommentaryEngineIntakeStatus.Rejected))

  test("malformed engine packet is rejected as intake metadata and not public render truth"):
    val malformedEngine =
      enginePacket(packetNodeId = nodeId).copy(
        requestedDepth = 0,
        pvLines = Vector(Vector("not-uci"))
      )
    val seam = CommentaryBackendSeam.withClaimProvider(_ => Vector(boardLead("api-selected-lead")))

    val response = seam.renderDebug(request(enginePacket = Some(malformedEngine), debug = true))

    assertEquals(response.status, CommentaryResponseStatus.Rendered)
    assertEquals(response.render.evidenceRefs.exists(_.kind == EvidenceRefKind.RawEngine), false)
    assertEquals(response.render.evidenceRefs.exists(_.kind == EvidenceRefKind.EngineCertification), false)
    assertEquals(response.render.blocks.map(_.role), Vector(RenderRole.Primary))
    assertEquals(response.internal.flatMap(_.engineIntake.map(_.status)), Some(CommentaryEngineIntakeStatus.Rejected))
    assertEquals(response.internal.flatMap(_.engineIntake.flatMap(_.reason)), Some("engine_intake_rejected"))

  test("rejected or absent engine intake fails closed for engine-certified claims before selection"):
    val seam = CommentaryBackendSeam.withClaimProvider(_ => Vector(engineCertifiedLead("api-engine-certified-lead")))

    val absentResponse = seam.renderDebug(request(debug = true))
    val rejectedResponse = seam.renderDebug(request(enginePacket = Some(enginePacket(packetNodeId = "other-node")), debug = true))

    Vector(absentResponse, rejectedResponse).foreach: response =>
      assertEquals(response.status, CommentaryResponseStatus.NoCommentary)
      assertEquals(response.render.blocks, Vector.empty)
      assertEquals(response.render.evidenceRefs.exists(_.kind == EvidenceRefKind.EngineCertification), false)
      assertEquals(response.render.evidenceRefs.exists(_.kind == EvidenceRefKind.RawEngine), false)
      assertEquals(response.noCommentary, true)
    assertEquals(absentResponse.internal.flatMap(_.engineIntake), None)
    assertEquals(rejectedResponse.internal.flatMap(_.engineIntake.map(_.status)), Some(CommentaryEngineIntakeStatus.Rejected))

  test("accepted engine intake must carry full route scope and purpose binding before engine-certified claims can render"):
    val canonicalEngineRef = "engine-certification:MaterialHarvest:white:board:board"
    val emptyAcceptedSeam =
      CommentaryBackendSeam.withClaimProvider(_ => Vector(engineCertifiedLead("api-empty-intake-engine-lead", canonicalEngineRef)), nowEpochMs = () => 12_000L)
    val forgedBindingSeam =
      CommentaryBackendSeam.withClaimProvider(_ => Vector(engineCertifiedLead("api-forged-engine-binding", canonicalEngineRef, ownerValue = "black")), nowEpochMs = () => 12_000L)
    val wrongIdSeam =
      CommentaryBackendSeam.withClaimProvider(_ => Vector(engineCertifiedLead("api-wrong-engine-id", "engine-certification:CertifiedKingSafetyEdge:white:board:board")), nowEpochMs = () => 12_000L)
    val wrongRouteSeam =
      CommentaryBackendSeam.withClaimProvider(
        _ => Vector(engineCertifiedLead("api-wrong-engine-route", canonicalEngineRef, routeValue = "api_engine_certified_route")),
        nowEpochMs = () => 12_000L
      )
    val partialPurposeSeam =
      CommentaryBackendSeam.withClaimProvider(
        _ =>
          Vector(
            engineCertifiedLead(
              "api-partial-purpose-engine-lead",
              canonicalEngineRef,
              routeValue = "realized_material_conversion",
              scopeValue = "current_position"
            )
          ),
        nowEpochMs = () => 12_000L
      )

    val emptyAccepted = emptyAcceptedSeam.renderDebug(request(enginePacket = Some(enginePacket(packetNodeId = nodeId)), debug = true))
    val forgedBinding =
      forgedBindingSeam.renderDebug(request(enginePacket = Some(materialEnginePacket()), debug = true))
    val wrongId =
      wrongIdSeam.renderDebug(request(enginePacket = Some(materialEnginePacket()), debug = true))
    val wrongRoute =
      wrongRouteSeam.renderDebug(request(enginePacket = Some(materialEnginePacket()), debug = true))
    val partialPurpose =
      partialPurposeSeam.renderDebug(request(enginePacket = Some(materialEnginePacket()), debug = true))

    assertEquals(emptyAccepted.internal.flatMap(_.engineIntake.map(_.status)), Some(CommentaryEngineIntakeStatus.Accepted))
    assertEquals(emptyAccepted.status, CommentaryResponseStatus.NoCommentary)
    assertEquals(emptyAccepted.render.evidenceRefs.exists(_.kind == EvidenceRefKind.EngineCertification), false)

    assertEquals(forgedBinding.internal.flatMap(_.engineIntake.map(_.status)), Some(CommentaryEngineIntakeStatus.Accepted))
    assertEquals(forgedBinding.status, CommentaryResponseStatus.NoCommentary)
    assertEquals(forgedBinding.render.evidenceRefs.exists(_.kind == EvidenceRefKind.EngineCertification), false)
    assertEquals(wrongId.status, CommentaryResponseStatus.NoCommentary)
    assertEquals(wrongId.render.evidenceRefs.exists(_.kind == EvidenceRefKind.EngineCertification), false)
    assertEquals(wrongRoute.status, CommentaryResponseStatus.NoCommentary)
    assertEquals(wrongRoute.render.evidenceRefs.exists(_.kind == EvidenceRefKind.EngineCertification), false)
    assertEquals(partialPurpose.internal.flatMap(_.engineIntake.map(_.status)), Some(CommentaryEngineIntakeStatus.Accepted))
    assertEquals(partialPurpose.status, CommentaryResponseStatus.NoCommentary)
    assertEquals(partialPurpose.render.evidenceRefs.exists(ref => ref.kind == EvidenceRefKind.EngineCertification && ref.id == canonicalEngineRef), false)

  test("default backend keeps accepted material certification non-public without concrete board reason"):
    val fen = "4k3/5ppp/8/3n4/3R4/8/5PPP/4K3 w - - 0 1"
    val now = System.currentTimeMillis()
    val response =
      CommentaryBackendSeam.renderDebug(
        request(
          currentFen = fen,
          enginePacket = Some(
            enginePacket(packetNodeId = nodeId, fen = fen).copy(
              multiPv = 3,
              generatedAtEpochMs = now,
              maxAgeMs = 60_000L,
              pvLines = Vector(Vector("d4d5"), Vector("d4d1"), Vector("d4a4")),
              claims = Vector(materialHarvestRuntimeClaim())
            )
          )
        )
      )

    assertEquals(response.status, CommentaryResponseStatus.Rendered)
    assertEquals(response.internal.flatMap(_.engineIntake.map(_.status)), Some(CommentaryEngineIntakeStatus.Accepted))
    assertEquals(response.render.evidenceRefs.exists(ref => ref.kind == EvidenceRefKind.Certification && ref.id == "MaterialHarvest"), false)
    assertEquals(response.render.evidenceRefs.exists(ref => ref.kind == EvidenceRefKind.EngineCertification && ref.id.startsWith("engine-certification:MaterialHarvest")), false)
    assertEquals(response.render.blocks.exists(_.evidenceIds.contains("MaterialHarvest")), false)
    assertEquals(response.internal.exists(_.suppressions.exists(suppression => suppression.claimId == "certification-material-harvest-white-board")), false)

  test("public response for accepted engine intake hides raw engine packet details"):
    val fen = "4k3/5ppp/8/3n4/3R4/8/5PPP/4K3 w - - 0 1"
    val now = System.currentTimeMillis()
    val response =
      CommentaryBackendSeam.render(
        request(
          currentFen = fen,
          enginePacket = Some(
            enginePacket(packetNodeId = nodeId, fen = fen).copy(
              multiPv = 3,
              generatedAtEpochMs = now,
              maxAgeMs = 60_000L,
              engineConfigFingerprint = "public-leak-contract-engine",
              pvLines = Vector(Vector("d4d5"), Vector("d4d1"), Vector("d4a4")),
              claims = Vector(materialHarvestRuntimeClaim())
            )
          )
        )
      )
    val json = Json.toJson(response).toString

    assertEquals(response.status, CommentaryResponseStatus.Rendered)
    Vector(
      "requestedDepth",
      "realizedDepth",
      "multiPv",
      "generatedAtEpochMs",
      "maxAgeMs",
      "engineConfigFingerprint",
      "public-leak-contract-engine",
      "pvLines",
      "scorePerspective",
      "centipawns",
      "mateIn",
      "debug",
      "engineIntake"
    ).foreach(token => assert(!json.contains(token), clues(token, json)))

  test("opening source context remains context-only and keeps master and online refs separate"):
    val opening = CommentaryClaim(
      id = "api-opening-context",
      layer = ClaimLayer.SourceContext,
      status = ClaimStatus.Context,
      evidenceRefs = Vector(
        EvidenceRef(EvidenceRefKind.SourceContext, "opening-position:catalan:canonical"),
        EvidenceRef(EvidenceRefKind.SourceContext, "opening-source-use:master_reference"),
        EvidenceRef(EvidenceRefKind.SourceContext, "opening-source-use:online_trend")
      ),
      wordingStrengthCap = WordingStrength.ContextOnly,
      sourceContextKind = Some(SourceContextKind.Opening)
    )
    val seam = CommentaryBackendSeam.withClaimProvider(_ => Vector(opening))

    val response = seam.render(request())

    assertEquals(response.status, CommentaryResponseStatus.ContextOnly)
    assertEquals(response.render.blocks.map(_.role), Vector(RenderRole.Context))
    assertEquals(response.render.blocks.head.nonAuthoritative, true)
    assertEquals(response.render.evidenceRefs.map(_.id).filter(_.contains("opening-source-use")), Vector("opening-source-use:master_reference", "opening-source-use:online_trend"))
    assertEquals(response.render.evidenceRefs.exists(_.id.contains("merged")), false)
    assertEquals(response.render.wording.maxStrength, WordingStrength.ContextOnly)

  test("internal candidate-line assembly prepared evidence reaches backend render response"):
    val assembly = candidateAssembly()
    val seam =
      CommentaryBackendSeam.withClaimProviderAndCandidateLineAssembly(
        _ => Vector(candidateBoundClaim("backend-line-claim")),
        _ => Some(assembly),
        nowEpochMs = () => nowEpochMs
      )

    val response = seam.render(request(currentFen = initialFen))
    val responseText = Json.toJson(response).toString

    assertEquals(response.status, CommentaryResponseStatus.Rendered)
    assertEquals(response.render.variationEvidence.map(_.boundClaimId).distinct, Vector("backend-line-claim"))
    assertEquals(response.render.variationEvidence.map(_.role).count(_ == RenderLineRole.Pressure), 1)
    assertEquals(response.render.variationEvidence.map(_.role).count(_ == RenderLineRole.Resource), 4)
    assertEquals(response.render.variationEvidence.head.lineSan, Vector("e4", "e5"))
    assertEquals(response.render.blocks.head.variationEvidenceIds, response.render.variationEvidence.map(_.proofId))
    Vector(
      "CandidateLineEvidence",
      "CandidateProbeResultPayload",
      "CandidateProbeControlledAdapter",
      "startFen",
      "lineUci",
      "provenanceRefs",
      "boundary",
      "realizedDepth",
      "root-candidate-1",
      "branchId",
      "parentBranchId",
      "multiPvIndex",
      "multiPv",
      "engineFingerprint",
      "stockfish-backend-seam",
      "rawLines",
      "cacheKey",
      "e2e4",
      "e7e5"
    ).foreach(token => assert(!responseText.contains(token), clues(token, responseText)))

  test("same request without internal candidate-line assembly preserves existing claim render behavior"):
    val seam =
      CommentaryBackendSeam.withClaimProviderAndCandidateLineAssembly(
        _ => Vector(candidateBoundClaim("backend-line-claim")),
        _ => None,
        nowEpochMs = () => nowEpochMs
      )

    val response = seam.render(request(currentFen = initialFen))

    assertEquals(response.status, CommentaryResponseStatus.Rendered)
    assertEquals(response.render.blocks.map(_.claimId), Vector("backend-line-claim"))
    assertEquals(response.render.variationEvidence, Vector.empty)

  test("candidate-line assembly without prepared variation evidence does not change render output"):
    val assemblyWithoutPrepared = candidateAssembly().copy(preparedVariationEvidence = Vector.empty)
    val seam =
      CommentaryBackendSeam.withClaimProviderAndCandidateLineAssembly(
        _ => Vector(candidateBoundClaim("backend-line-claim")),
        _ => Some(assemblyWithoutPrepared),
        nowEpochMs = () => nowEpochMs
      )

    val response = seam.render(request(currentFen = initialFen))

    assertEquals(response.status, CommentaryResponseStatus.Rendered)
    assertEquals(response.render.blocks.map(_.claimId), Vector("backend-line-claim"))
    assertEquals(response.render.variationEvidence, Vector.empty)

  test("mismatched prepared variation binding from candidate assembly is suppressed and not rendered"):
    val mismatchedAssembly = candidateAssembly(bindingOwner = "black", bindingDefender = Some("white"))
    val seam =
      CommentaryBackendSeam.withClaimProviderAndCandidateLineAssembly(
        _ => Vector(candidateBoundClaim("backend-line-claim")),
        _ => Some(mismatchedAssembly),
        nowEpochMs = () => nowEpochMs
      )

    val response = seam.renderDebug(request(currentFen = initialFen))

    assertEquals(response.render.variationEvidence, Vector.empty)
    assertEquals(response.render.blocks.exists(_.claimId == "backend-line-claim"), false)
    assert(response.internal.exists(_.suppressions.exists(_.claimId == "backend-line-claim")))

  test("candidate-line assembly evidence alone does not create CommentaryClaim"):
    val seam =
      CommentaryBackendSeam.withClaimProviderAndCandidateLineAssembly(
        _ => Vector.empty,
        _ => Some(candidateAssembly()),
        nowEpochMs = () => nowEpochMs
      )

    val response = seam.render(request(currentFen = initialFen))

    assertEquals(response.status, CommentaryResponseStatus.NoCommentary)
    assertEquals(response.render.blocks, Vector.empty)
    assertEquals(response.render.variationEvidence, Vector.empty)

  test("claimProvider cannot observe candidate-line assembly to create claims"):
    var observedProductFields = Vector.empty[String]
    var observedAssembly = false
    val seam =
      CommentaryBackendSeam.withClaimProviderAndCandidateLineAssembly(
        input =>
          observedProductFields = input.productElementNames.toVector
          observedAssembly =
            input.productIterator.exists:
              case Some(_: CandidateProbeControlledAdapter.AssemblyResult) => true
              case _ => false
          if observedAssembly then Vector(candidateBoundClaim("backend-line-claim")) else Vector.empty,
        _ => Some(candidateAssembly()),
        nowEpochMs = () => nowEpochMs
      )

    val response = seam.render(request(currentFen = initialFen))

    assertEquals(observedAssembly, false)
    assertEquals(observedProductFields.contains("candidateLineAssembly"), false)
    assertEquals(response.status, CommentaryResponseStatus.NoCommentary)
    assertEquals(response.render.blocks, Vector.empty)
    assertEquals(response.render.variationEvidence, Vector.empty)

  test("claimProvider runs before candidate-line assembly provider"):
    var assemblyProviderAlreadyRan = false
    val seam =
      CommentaryBackendSeam.withClaimProviderAndCandidateLineAssembly(
        _ =>
          if assemblyProviderAlreadyRan then Vector(candidateBoundClaim("backend-line-claim")) else Vector.empty,
        _ =>
          assemblyProviderAlreadyRan = true
          Some(candidateAssembly()),
        nowEpochMs = () => nowEpochMs
      )

    val response = seam.render(request(currentFen = initialFen))

    assertEquals(assemblyProviderAlreadyRan, true)
    assertEquals(response.status, CommentaryResponseStatus.NoCommentary)
    assertEquals(response.render.blocks, Vector.empty)
    assertEquals(response.render.variationEvidence, Vector.empty)

  test("public request JSON ignores candidate-line seam fields and response omits raw internals"):
    val rawRequest = Json.obj(
      "currentFen" -> bareKingsFen,
      "nodeId" -> nodeId,
      "ply" -> 0,
      "candidateLineAssembly" -> Json.obj(
        "candidateLineEvidence" -> Json.arr(Json.obj("branchId" -> "root-candidate-1")),
        "preparedVariationEvidence" -> Json.arr(Json.obj("proofId" -> "backend-line-claim-line-1")),
        "cacheWrites" -> Json.arr(Json.obj("cacheKey" -> "secret-cache-key"))
      ),
      "rootProbeResult" -> Json.obj("engineFingerprint" -> "stockfish-backend-seam"),
      "lineSeamDebug" -> Json.obj("rawPv" -> Json.arr("e2e4"))
    )

    val response = CommentaryBackendSeam.renderDebug(rawRequest.as[CommentaryRequest])
    val json = Json.toJson(response).toString

    assertEquals(response.status, CommentaryResponseStatus.NoCommentary)
    Vector(
      "candidateLineAssembly",
      "candidateLineEvidence",
      "preparedVariationEvidence",
      "rootProbeResult",
      "stockfish-backend-seam",
      "secret-cache-key",
      "rawPv",
      "lineSeamDebug"
    ).foreach(token => assert(!json.contains(token), clues(token, json)))

  test("public request JSON ignores completed-probe helper-shaped root and child payload fields"):
    val rawRequest = Json.obj(
      "currentFen" -> bareKingsFen,
      "nodeId" -> nodeId,
      "ply" -> 0,
      "completedProbePayload" -> Json.obj(
        "rootProbe" -> Json.obj(
          "engineFingerprint" -> "stockfish-completed-probe",
          "completed" -> true,
          "multiPv" -> 3,
          "lines" -> Json.arr(Json.obj("rank" -> 1, "multiPvIndex" -> 1, "uci" -> Json.arr("e2e4")))
        ),
        "childProbes" -> Json.arr(
          Json.obj(
            "parentBranchId" -> "root-candidate-1",
            "parentUciPrefix" -> Json.arr("e2e4"),
            "parentRootRank" -> 1,
            "lines" -> Json.arr(Json.obj("rank" -> 1, "multiPvIndex" -> 1, "uci" -> Json.arr("e7e5")))
          )
        ),
        "probeRequests" -> Json.arr(Json.obj("role" -> "root_candidate"))
      ),
      "rootProbe" -> Json.obj("engineFingerprint" -> "stockfish-root-probe"),
      "childProbes" -> Json.arr(Json.obj("parentBranchId" -> "root-candidate-1")),
      "probeRequests" -> Json.arr(Json.obj("role" -> "defender_resource")),
      "candidateLineRootProbe" -> Json.obj("cacheKey" -> "secret-root-cache"),
      "candidateLineChildProbe" -> Json.obj("cacheKey" -> "secret-child-cache")
    )

    val decoded = rawRequest.as[CommentaryRequest]
    val response = CommentaryBackendSeam.renderDebug(decoded)
    val json = Json.toJson(response).toString

    assertEquals(decoded, request(currentFen = bareKingsFen))
    assertEquals(response.status, CommentaryResponseStatus.NoCommentary)
    Vector(
      "completedProbePayload",
      "rootProbe",
      "childProbes",
      "probeRequests",
      "candidateLineRootProbe",
      "candidateLineChildProbe",
      "stockfish-completed-probe",
      "stockfish-root-probe",
      "root-candidate-1",
      "secret-root-cache",
      "secret-child-cache"
    ).foreach(token => assert(!json.contains(token), clues(token, json)))

  test("internal completed-probe payload cannot attach line evidence to support-only transition carriers"):
    val rootOnly =
      CommentaryBackendSeam.renderInternal(
        request(
          currentFen = afterE4Fen,
          beforeFen = Some(initialFen),
          playedMove = Some("e2e4"),
          ply = 1
        ),
        Some(completedProbePayload(includeChild = false))
      )
    val withChild =
      CommentaryBackendSeam.renderInternal(
        request(
          currentFen = afterE4Fen,
          beforeFen = Some(initialFen),
          playedMove = Some("e2e4"),
          ply = 1
        ),
        Some(completedProbePayload(includeChild = true))
      )
    val json = Json.toJson(withChild).toString

    assertEquals(rootOnly.render.variationEvidence, Vector.empty)
    assertEquals(withChild.status, CommentaryResponseStatus.Rendered)
    assertEquals(withChild.render.variationEvidence, Vector.empty)
    Vector("parentBranchId", "root-candidate", "engineFingerprint", "cacheKey", "rawPv", "CandidateLineEvidence").foreach: token =>
      assert(!json.contains(token), clues(token, json))

  test("internal completed child probe with wrong parent branch id cannot lower public line evidence"):
    val payload = completedProbePayload(includeChild = true)
    val response =
      CommentaryBackendSeam.renderInternal(
        request(
          currentFen = afterE4Fen,
          beforeFen = Some(initialFen),
          playedMove = Some("e2e4"),
          ply = 1
        ),
        Some(payload.copy(childProbes = payload.childProbes.map(_.copy(parentBranchId = "caller-forged-parent-branch"))))
      )

    assertEquals(response.render.variationEvidence, Vector.empty)

  test("internal completed-probe payload must match server-issued root and child requests"):
    val validPayload = completedProbePayload(includeChild = true)
    val validInput =
      CommentaryBackendSeam.completedProbeAssemblyInput(
        input = completedProbePipelineInput(),
        claims = Vector(candidateBoundClaim("backend-line-claim")),
        payload = validPayload,
        cache = CandidateLineProofCache.InMemory.empty
      )

    assert(validInput.nonEmpty)

    val missingChildRequest =
      CommentaryBackendSeam.completedProbeAssemblyInput(
        completedProbePipelineInput(),
        Vector(candidateBoundClaim("backend-line-claim")),
        validPayload.copy(probeRequests = validPayload.probeRequests.filterNot(_.role == "defender_resource")),
        CandidateLineProofCache.InMemory.empty
      )
    val forgedChildRequest =
      CommentaryBackendSeam.completedProbeAssemblyInput(
        completedProbePipelineInput(),
        Vector(candidateBoundClaim("backend-line-claim")),
        validPayload.copy(probeRequests =
          validPayload.probeRequests.map:
            case request if request.role == "defender_resource" => request.copy(requestedDepth = 20)
            case request => request
        ),
        CandidateLineProofCache.InMemory.empty
      )
    val lineMultiPvMismatch =
      CommentaryBackendSeam.completedProbeAssemblyInput(
        completedProbePipelineInput(),
        Vector(candidateBoundClaim("backend-line-claim")),
        validPayload.copy(
          rootProbe = validPayload.rootProbe.copy(
            lines = validPayload.rootProbe.lines.updated(0, validPayload.rootProbe.lines.head.copy(multiPv = 2))
          )
        ),
        CandidateLineProofCache.InMemory.empty
      )

    assertEquals(missingChildRequest, None)
    assertEquals(forgedChildRequest, None)
    assertEquals(lineMultiPvMismatch, None)

  test("CommentaryRequest source shape excludes completed-probe and internal candidate-line types"):
    val source = java.nio.file.Files.readString(java.nio.file.Paths.get("modules/commentary/src/main/scala/lila/commentary/api/CommentaryBackendSeam.scala"))
    val requestStart = source.indexOf("final case class CommentaryRequest(")
    val requestEnd = source.indexOf("final case class CommentaryCompletedProbeCurrent", requestStart)
    val requestShape = source.substring(requestStart, requestEnd)

    Vector(
      "candidateLineRootProbe",
      "candidateLineChildProbe",
      "CandidateProbeResultPayload",
      "CandidateLineEvidence",
      "CandidateProbeControlledAdapter",
      "CandidateLinePacket"
    ).foreach(token => assert(!requestShape.contains(token), clues(token, requestShape)))
    Vector(
      "completedProbe",
      "CommentaryCompletedProbePayload",
      "CommentaryCompletedRootProbe",
      "CommentaryCompletedChildProbe"
    ).foreach(token => assert(!requestShape.contains(token), clues(token, requestShape)))

  test("raw source context fields in request JSON are ignored and cannot become render truth"):
    val rawSourceJson = Json.obj(
      "currentFen" -> bareKingsFen,
      "nodeId" -> nodeId,
      "ply" -> 0,
      "openingContextCandidate" -> Json.obj(
        "ranking" -> "best",
        "source" -> "master_reference",
        "truthClaim" -> "best move"
      )
    )

    val response = CommentaryBackendSeam.render(rawSourceJson.as[CommentaryRequest])

    assertEquals(response.status, CommentaryResponseStatus.NoCommentary)
    assertEquals(response.render.status, RenderStatus.NoCommentary)
    assertEquals(response.render.blocks, Vector.empty)
    assertEquals(response.render.evidenceRefs, Vector.empty)

  test("no admitted claim returns noCommentary without fallback prose"):
    val response = CommentaryBackendSeam.render(request(currentFen = bareKingsFen))

    assertEquals(response.status, CommentaryResponseStatus.NoCommentary)
    assertEquals(response.render.status, RenderStatus.NoCommentary)
    assertEquals(response.render.blocks, Vector.empty)
    assertEquals(response.render.blocks.flatMap(_.text.publicText), Vector.empty)

  test("blocked metadata is hidden by default and debug exposes internal metadata separately"):
    val blocked = CommentaryClaim(
      id = "api-blocked-source-truth",
      layer = ClaimLayer.SourceContext,
      status = ClaimStatus.Rejected,
      evidenceRefs = Vector(EvidenceRef(EvidenceRefKind.SourceContext, "opening-position:catalan:best-move")),
      sourceContextKind = Some(SourceContextKind.Opening)
    )
    val seam = CommentaryBackendSeam.withClaimProvider(_ => Vector(blocked))

    val publicResponse = seam.render(request())
    val callerDebugResponse = seam.render(request(debug = true))
    val debugResponse = seam.renderDebug(request())

    assertEquals(publicResponse.render.suppressions, Vector.empty)
    assertEquals(publicResponse.internal, None)
    assertEquals(callerDebugResponse.internal, None)
    assertEquals(debugResponse.render.suppressions, Vector.empty)
    assert(debugResponse.internal.exists(_.suppressions.map(_.claimId) == Vector("api-blocked-source-truth")))
    assertEquals(debugResponse.render.blocks.exists(_.claimId == "api-blocked-source-truth"), false)

  test("direct claim-provider S23 projection cannot bypass runtime K producer provenance"):
    val seam = CommentaryBackendSeam.withClaimProvider(_ => Vector(forgedS23ProjectionClaim()))

    val publicResponse = seam.render(request(currentFen = bareKingsFen))
    val debugResponse = seam.renderDebug(request(currentFen = bareKingsFen))

    assertEquals(publicResponse.render.blocks.exists(_.claimId == "api-forged-s23-projection"), false)
    assertEquals(publicResponse.render.evidenceRefs.exists(_.id == "king_entry_conversion_certified"), false)
    assert(debugResponse.internal.exists(_.suppressions.exists(_.claimId == "api-forged-s23-projection")))

  test("direct claim-provider S24 projection remains blocker-only and cannot surface publicly"):
    val seam = CommentaryBackendSeam.withClaimProvider(_ => Vector(forgedS24ProjectionClaim()))

    val publicResponse = seam.render(request(currentFen = bareKingsFen))
    val debugResponse = seam.renderDebug(request(currentFen = bareKingsFen))

    assertEquals(publicResponse.status, CommentaryResponseStatus.NoCommentary)
    assertEquals(publicResponse.render.blocks.exists(_.claimId == "api-forged-s24-projection"), false)
    assertEquals(publicResponse.render.evidenceRefs.exists(_.id == "same_target_forcing_realization"), false)
    assertEquals(publicResponse.render.evidenceRefs.exists(_.id == "same_target_conversion_certified"), false)
    assert(debugResponse.internal.exists(_.suppressions.exists(_.claimId == "api-forged-s24-projection")))

  test("request and response JSON serialization round trip uses stable role keys"):
    val seam = CommentaryBackendSeam.withClaimProvider(_ => Vector(boardLead("api-json-lead")))
    val apiRequest = request(debug = true)
    val requestJson = Json.toJson(apiRequest)
    val minimalRequestJson = Json.obj(
      "currentFen" -> validFen,
      "nodeId" -> nodeId,
      "ply" -> 0
    )
    val response = seam.render(requestJson.as[CommentaryRequest])
    val responseJson = Json.toJson(response)
    val decoded = responseJson.as[CommentaryResponse]

    assertEquals(requestJson.as[CommentaryRequest], apiRequest)
    assertEquals(minimalRequestJson.as[CommentaryRequest], request())
    assertEquals(decoded.status, CommentaryResponseStatus.Rendered)
    assertEquals(decoded.render.blocks.map(_.role), Vector(RenderRole.Primary))
    assert(decoded.render.blocks.head.phraseCapability.allowedPredicates.contains(PublicClaimPredicate.BoardFact))
    assertEquals(decoded.render.blocks.head.phraseCapability.allowsLineCommentary, false)
    assertEquals((responseJson \ "render" \ "blocks" \ 0 \ "role").as[String], "primary")
    assertEquals((responseJson \ "render" \ "blocks" \ 0 \ "phraseCapability" \ "allowsLineCommentary").as[Boolean], false)
    assertEquals(
      (responseJson \ "render" \ "blocks" \ 0 \ "phraseCapability" \ "allowedPredicates").as[Vector[String]],
      Vector("board_fact")
    )
    assertEquals((responseJson \ "render" \ "suppressions").as[Vector[String]], Vector.empty)

  test("contract docs name the backend seam and keep public route separate from local probe"):
    val contract = java.nio.file.Files.readString(java.nio.file.Paths.get("modules/commentary/docs/CommentaryBackendSeamContract.md"))
    val core = java.nio.file.Files.readString(java.nio.file.Paths.get("modules/commentary/docs/CommentaryCoreSSOT.md"))
    val validation = java.nio.file.Files.readString(java.nio.file.Paths.get("modules/commentary/docs/ValidationMethodology.md"))

    Vector(
      "CommentaryRequest",
      "CommentaryResponse",
      "CommentaryBackendSeam",
      "CommentaryRender",
      "RuntimeEnginePacket",
      "no source live integration",
      "public route stays narrow",
      "internal/non-production handoff"
    ).foreach(token => assert(contract.contains(token), token))
    assert(core.contains("CommentaryBackendSeamContract.md"))
    assert(validation.contains("CommentaryBackendSeamContractTest.scala"))

  private def request(
      currentFen: String = validFen,
      enginePacket: Option[CertificationEngineRuntimeIntake.RuntimeEnginePacket] = None,
      beforeFen: Option[String] = None,
      playedMove: Option[String] = None,
      debug: Boolean = false,
      nodeId: String = nodeId,
      ply: Int = 0
  ): CommentaryRequest =
    CommentaryRequest(
      currentFen = currentFen,
      beforeFen = beforeFen,
      playedMove = playedMove,
      nodeId = nodeId,
      ply = ply,
      enginePacket = enginePacket,
      debug = debug
    )

  private val afterE4Fen =
    "rnbqkbnr/pppppppp/8/8/4P3/8/PPPP1PPP/RNBQKBNR b KQkq - 0 1"

  private val afterE4E5Fen =
    "rnbqkbnr/pppp1ppp/8/4p3/4P3/8/PPPP1PPP/RNBQKBNR w KQkq - 0 2"

  private def completedProbePipelineInput(): CommentaryPipelineInput =
    val fen = Fen.Full.clean(afterE4Fen): Fen.Full
    CommentaryPipelineInput(
      node = EngineNodeIdentity(nodeId, 1),
      currentFen = fen,
      beforeFen = None,
      currentExtraction = CommentaryCore.extractStrategicObjectsFromFenFailClosed(afterE4Fen).fold(fail(_), identity),
      deltaExtraction = None,
      engineIntake = None,
      engineCertificationEvidence = Some(CertificationEvidenceBundle.empty),
      completedProbe = None
    )

  private def completedProbePayload(includeChild: Boolean): CommentaryCompletedProbePayload =
    val generatedAt = System.currentTimeMillis().toString
    CommentaryCompletedProbePayload(
      current = CommentaryCompletedProbeCurrent(afterE4Fen, nodeId, 1, "standard"),
      engineFingerprint = "stockfish-completed-probe-contract",
      budget = Some(
        CommentaryCompletedProbeBudget(
          rootMultiPv = 3,
          childMultiPv = 2,
          depthFloor = 16,
          rootTargetDepth = Some(18),
          childTargetDepth = Some(18),
          maxAgeMillis = Some(30_000L)
        )
      ),
      probeRequests = Vector(
        CommentaryCompletedProbeRequest(
          role = "root_candidate",
          currentFen = afterE4Fen,
          nodeId = nodeId,
          ply = 1,
          variant = "standard",
          multiPv = 3,
          requestedDepth = 18,
          depthFloor = 16
        )
      ) ++
        Option.when(includeChild)(
          CommentaryCompletedProbeRequest(
            role = "defender_resource",
            currentFen = afterE4E5Fen,
            nodeId = nodeId,
            ply = 2,
            variant = "standard",
            multiPv = 2,
            requestedDepth = 18,
            depthFloor = 16,
            parentBranchId = Some("root-candidate-1"),
            parentUciPrefix = Some(Vector("e7e5")),
            parentRootRank = Some(1)
          )
        ).toVector,
      rootProbe = CommentaryCompletedRootProbe(
        currentFen = afterE4Fen,
        nodeId = nodeId,
        ply = 1,
        variant = "standard",
        engineFingerprint = "stockfish-completed-probe-contract",
        requestedDepth = 18,
        realizedDepth = 18,
        multiPv = 3,
        generatedAt = generatedAt,
        maxAgeMillis = 30_000L,
        completed = true,
        lines = Vector(
          CommentaryCompletedProbeLine(1, 1, 3, Vector("e7e5", "g1f3")),
          CommentaryCompletedProbeLine(2, 2, 3, Vector("c7c5", "g1f3")),
          CommentaryCompletedProbeLine(3, 3, 3, Vector("e7e6", "d2d4"))
        )
      ),
      childProbes =
        if !includeChild then Vector.empty
        else
          Vector(
            CommentaryCompletedChildProbe(
              currentFen = afterE4E5Fen,
              nodeId = nodeId,
              ply = 2,
              variant = "standard",
              engineFingerprint = "stockfish-completed-probe-contract",
              parentBranchId = "root-candidate-1",
              parentUciPrefix = Vector("e7e5"),
              parentRootRank = 1,
              requestedDepth = 18,
              realizedDepth = 18,
              multiPv = 2,
              generatedAt = generatedAt,
              maxAgeMillis = 30_000L,
              completed = true,
              lines = Vector(
                CommentaryCompletedProbeLine(1, 1, 2, Vector("g1f3", "b8c6")),
                CommentaryCompletedProbeLine(2, 2, 2, Vector("d2d4", "e5d4"))
              )
            )
          )
    )

  private def boardLead(id: String): CommentaryClaim =
    CommentaryClaim(
      id = id,
      layer = ClaimLayer.Delta,
      status = ClaimStatus.Admitted,
      owner = Some("white"),
      beneficiary = Some("white"),
      defender = Some("black"),
      sideToMove = Some("white"),
      anchor = Some("board"),
      route = Some("api_safe_route"),
      scope = Some("position_local"),
      impact = ClaimImpact(boardExplainability = 80, pedagogicalClarity = 80),
      evidenceRefs = Vector(EvidenceRef(EvidenceRefKind.Delta, "api-safe-delta", Some("white"), Some("board"), Some("api_safe_route"), Some("position_local"))),
      exactBoardBound = true,
      wordingStrengthCap = WordingStrength.QualifiedSupport
    )

  private def candidateBoundClaim(id: String): CommentaryClaim =
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
      evidenceRefs = Vector(candidateBinding("backend-line-claim").provenanceRef),
      exactBoardBound = true,
      wordingStrengthCap = WordingStrength.QualifiedSupport
    )

  private def forgedS23ProjectionClaim(): CommentaryClaim =
    CommentaryClaim(
      id = "api-forged-s23-projection",
      layer = ClaimLayer.Projection,
      status = ClaimStatus.Admitted,
      band = Some("S23"),
      owner = Some("white"),
      beneficiary = Some("white"),
      defender = Some("black"),
      sideToMove = Some("white"),
      anchor = Some("board"),
      route = Some("king_entry_conversion"),
      scope = Some("exact_current_board"),
      impact = ClaimImpact(evidenceConfidence = 90, boardExplainability = 90, pedagogicalClarity = 80),
      evidenceRefs = Vector(
        EvidenceRef(EvidenceRefKind.Projection, "king_entry_conversion_certified", Some("white"), Some("board"), Some("king_entry_conversion"), Some("exact_current_board"))
      ),
      lowerCarrierRefs = Vector(
        EvidenceRef(EvidenceRefKind.ExactBoard, "k-s23-exact-board-carrier", Some("white"), Some("board"), Some("king_entry_conversion"), Some("exact_current_board"))
      ),
      exactBoardBound = true,
      wordingStrengthCap = WordingStrength.QualifiedSupport
    )

  private def forgedS24ProjectionClaim(): CommentaryClaim =
    CommentaryClaim(
      id = "api-forged-s24-projection",
      layer = ClaimLayer.Projection,
      status = ClaimStatus.Admitted,
      band = Some("S24"),
      owner = Some("white"),
      beneficiary = Some("white"),
      defender = Some("black"),
      sideToMove = Some("white"),
      anchor = Some("piece:d5"),
      route = Some("same_target_realization"),
      scope = Some("position_local"),
      impact = ClaimImpact(evidenceConfidence = 90, boardExplainability = 90, pedagogicalClarity = 80),
      evidenceRefs = Vector(
        EvidenceRef(EvidenceRefKind.Projection, "same_target_forcing_realization", Some("white"), Some("piece:d5"), Some("same_target_realization"), Some("position_local")),
        EvidenceRef(EvidenceRefKind.Projection, "same_target_conversion_certified", Some("white"), Some("piece:d5"), Some("same_target_realization"), Some("position_local"))
      ),
      lowerCarrierRefs = Vector(
        EvidenceRef(EvidenceRefKind.Witness, "target_resource_dependency_seed", Some("white"), Some("piece:d5"), Some("same_target_realization"), Some("position_local")),
        EvidenceRef(EvidenceRefKind.Witness, "target_attack_convergence_seed", Some("white"), Some("piece:d5"), Some("same_target_realization"), Some("position_local"))
      ),
      exactBoardBound = true,
      wordingStrengthCap = WordingStrength.QualifiedSupport
    )

  private def candidateAssembly(
      bindingOwner: String = "white",
      bindingDefender: Option[String] = Some("black")
  ): CandidateProbeControlledAdapter.AssemblyResult =
    val binding = candidateBinding("backend-line-claim", bindingOwner, bindingDefender)
    val rootEvidence =
      CandidateProbeControlledAdapter
        .rootPacketFrom(candidateRootPayload, CandidateLineProvenanceKind.EngineRoot)
        .toVector
        .flatMap(packet =>
          CandidateLinePacketHandoff.normalize(
            CandidateLinePacketHandoffInput(initialFen, nodeId, 0, packet),
            nowEpochMs
          )
        )
    val childRequests = CandidateProbePlan.childStage(rootEvidence, nowEpochMs = nowEpochMs).requests
    CandidateProbeControlledAdapter.assemble(
      CandidateProbeControlledAdapter.Input(
        currentFen = initialFen,
        nodeId = nodeId,
        ply = 0,
        variant = "standard",
        rootRequest = candidateRootRequest,
        rootProbeResult = Some(candidateRootPayload),
        rootCacheHit = None,
        childRequests = childRequests,
        childProbeResults = childRequests.map(request =>
          CandidateProbeResultPayload(
            request = request,
            lines = childLinesFor(request),
            realizedDepth = 18,
            generatedAtEpochMs = 10_000L,
            maxAgeMs = 20_000L
          )
        ),
        childCacheHits = Vector.empty,
        loweringBinding = Some(binding),
        nowEpochMs = nowEpochMs,
        budget = CandidateProbeBudget.Default,
        loweringPolicy = CandidateLineEvidenceLowering.Policy.Default
      )
    )

  private def childLinesFor(request: CandidateProbeRequest): Vector[CandidateProbeResultLine] =
    request.parentBranchId.map(_.value) match
      case Some("root-candidate-1") =>
        Vector(
          candidatePayloadLine("defender-resource-e4-1", Vector("c7c5", "g1f3"), rank = 1, multiPvIndex = 1),
          candidatePayloadLine("defender-resource-e4-2", Vector("e7e5", "g1f3"), rank = 2, multiPvIndex = 2)
        )
      case Some("root-candidate-2") =>
        Vector(
          candidatePayloadLine("defender-resource-d4-1", Vector("d7d5", "c2c4"), rank = 1, multiPvIndex = 1),
          candidatePayloadLine("defender-resource-d4-2", Vector("g8f6", "c2c4"), rank = 2, multiPvIndex = 2)
        )
      case _ => Vector.empty

  private def candidateBinding(
      boundClaimId: String,
      owner: String = "white",
      defender: Option[String] = Some("black")
  ): CandidateLineEvidenceLowering.Binding =
    val route = Some("pressure_route")
    val scope = Some("position_local")
    CandidateLineEvidenceLowering.Binding(
      boundClaimId = boundClaimId,
      owner = owner,
      defender = defender,
      anchor = "board",
      route = route.get,
      scope = scope.get,
      provenanceRef = EvidenceRef(
        EvidenceRefKind.Certification,
        "CertifiedLine",
        Some(owner),
        Some("board"),
        route,
        scope
      )
    )

  private def candidateRootRequest: CandidateProbeRequest =
    CandidateProbePlan.rootStage(initialFen, nodeId, 0, "standard", "stockfish-backend-seam").requests.head

  private def candidateRootPayload: CandidateProbeResultPayload =
    CandidateProbeResultPayload(
      request = candidateRootRequest,
      lines = Vector(
        candidatePayloadLine("root-candidate-1", Vector("e2e4", "e7e5"), rank = 1, multiPvIndex = 1),
        candidatePayloadLine("root-candidate-2", Vector("d2d4", "d7d5"), rank = 2, multiPvIndex = 2),
        candidatePayloadLine("root-candidate-3", Vector("g1f3", "g8f6"), rank = 3, multiPvIndex = 3)
      ),
      realizedDepth = 18,
      generatedAtEpochMs = 10_000L,
      maxAgeMs = 20_000L
    )

  private def candidatePayloadLine(
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

  private def engineCertifiedLead(
      id: String,
      engineEvidenceId: String = "api-bounded-engine-certification",
      ownerValue: String = "white",
      anchorValue: String = "board",
      routeValue: String = "api_engine_certified_route",
      scopeValue: String = "position_local"
  ): CommentaryClaim =
    val owner = Some(ownerValue)
    val defender = if ownerValue == "white" then Some("black") else Some("white")
    val anchor = Some(anchorValue)
    val route = Some(routeValue)
    val scope = Some(scopeValue)
    CommentaryClaim(
      id = id,
      layer = ClaimLayer.Certification,
      status = ClaimStatus.Admitted,
      owner = owner,
      beneficiary = owner,
      defender = defender,
      sideToMove = owner,
      anchor = anchor,
      route = route,
      scope = scope,
      impact = ClaimImpact(resultMaterialImpact = 90, forcedness = 85, immediacy = 85),
      evidenceRefs = Vector(
        EvidenceRef(EvidenceRefKind.Certification, "api-bounded-certification", owner, anchor, route, scope),
        EvidenceRef(EvidenceRefKind.EngineCertification, engineEvidenceId, owner, anchor, route, scope),
        EvidenceRef(EvidenceRefKind.Delta, "api-certified-delta", owner, anchor, route, scope)
      ),
      exactBoardBound = true,
      wordingStrengthCap = WordingStrength.AssertiveCertified
    )

  private def materialHarvestRuntimeClaim(): CertificationEngineRuntimeIntake.RuntimeCertificationClaim =
    CertificationEngineRuntimeIntake.RuntimeCertificationClaim(
      familyId = "MaterialHarvest",
      owner = "white",
      purposes = Map(
        "best_defense_survival" -> "satisfied"
      ),
      minDepth = 18,
      minMultiPv = 3,
      minPvPlies = 1,
      requiredScore = Some(CertificationEngineRuntimeIntake.RuntimeScoreRequirement.CentipawnAtLeast(50)),
      probeRequestId = Some("q-best-defense-survival-material-harvest-white-board-mainline-0-0"),
      probePolicyFingerprint = Some(
        CertificationEnginePolicyFingerprint.defaultForRole(
          "api-seam-test-engine",
          CertificationEngineRole.BestDefenseSurvival
        )
      ),
      roleReports = Some(Map("best_defense_survival" -> "satisfied"))
    )

  private def materialEnginePacket(): CertificationEngineRuntimeIntake.RuntimeEnginePacket =
    enginePacket(packetNodeId = nodeId).copy(
      multiPv = 3,
      pvLines = Vector(Vector("g8f6"), Vector("d7d6"), Vector("e5d4")),
      claims = Vector(materialHarvestRuntimeClaim())
    )

  private def enginePacket(
      packetNodeId: String,
      fen: String = validFen,
      ply: Int = 0
  ): CertificationEngineRuntimeIntake.RuntimeEnginePacket =
    CertificationEngineRuntimeIntake.RuntimeEnginePacket(
      fen = fen,
      nodeId = packetNodeId,
      ply = ply,
      requestedDepth = 18,
      realizedDepth = 18,
      multiPv = 1,
      completed = true,
      generatedAtEpochMs = 1_000L,
      maxAgeMs = 20_000L,
      engineConfigFingerprint = "api-seam-test-engine",
      score = CertificationEngineRuntimeIntake.RuntimeScore.Centipawns(80),
      scorePerspective = CertificationEngineRuntimeIntake.RuntimeScorePerspective.White,
      pvLines = Vector(Vector("g8f6")),
      claims = Vector.empty
    )
