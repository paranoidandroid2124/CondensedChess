package lila.commentary.api

import play.api.libs.json.Json

import lila.commentary.certification.CertificationEngineRuntimeIntake
import lila.commentary.render.{ RenderRole, RenderStatus }
import lila.commentary.selection.*
import lila.commentary.api.CommentaryApiJson.given

class CommentaryBackendSeamContractTest extends munit.FunSuite:

  private val validFen = "r1bqkbnr/pppp1ppp/2n5/4p3/3PP3/2N2N2/PPP2PPP/R1BQKB1R b KQkq - 3 3"
  private val nodeId = "mainline:0"

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

  test("accepted engine intake must carry matching bounded evidence before engine-certified claims can render"):
    val canonicalEngineRef = "engine-certification:CertifiedKingSafetyEdge:white:board:board"
    val emptyAcceptedSeam =
      CommentaryBackendSeam.withClaimProvider(_ => Vector(engineCertifiedLead("api-empty-intake-engine-lead", canonicalEngineRef)), nowEpochMs = () => 12_000L)
    val forgedBindingSeam =
      CommentaryBackendSeam.withClaimProvider(_ => Vector(engineCertifiedLead("api-forged-engine-binding", canonicalEngineRef, ownerValue = "black")), nowEpochMs = () => 12_000L)
    val wrongIdSeam =
      CommentaryBackendSeam.withClaimProvider(_ => Vector(engineCertifiedLead("api-wrong-engine-id", "engine-certification:MaterialHarvest:white:board:board")), nowEpochMs = () => 12_000L)
    val wrongAnchorSeam =
      CommentaryBackendSeam.withClaimProvider(_ => Vector(engineCertifiedLead("api-wrong-engine-anchor", canonicalEngineRef, anchorValue = "file")), nowEpochMs = () => 12_000L)
    val boundedSeam =
      CommentaryBackendSeam.withClaimProvider(_ => Vector(engineCertifiedLead("api-bound-intake-engine-lead", canonicalEngineRef)), nowEpochMs = () => 12_000L)

    val emptyAccepted = emptyAcceptedSeam.renderDebug(request(enginePacket = Some(enginePacket(packetNodeId = nodeId)), debug = true))
    val forgedBinding =
      forgedBindingSeam.renderDebug(request(enginePacket = Some(enginePacket(packetNodeId = nodeId).copy(claims = Vector(runtimeEngineClaim()))), debug = true))
    val wrongId =
      wrongIdSeam.renderDebug(request(enginePacket = Some(enginePacket(packetNodeId = nodeId).copy(claims = Vector(runtimeEngineClaim()))), debug = true))
    val wrongAnchor =
      wrongAnchorSeam.renderDebug(request(enginePacket = Some(enginePacket(packetNodeId = nodeId).copy(claims = Vector(runtimeEngineClaim()))), debug = true))
    val boundedAccepted =
      boundedSeam.renderDebug(request(enginePacket = Some(enginePacket(packetNodeId = nodeId).copy(claims = Vector(runtimeEngineClaim()))), debug = true))

    assertEquals(emptyAccepted.internal.flatMap(_.engineIntake.map(_.status)), Some(CommentaryEngineIntakeStatus.Accepted))
    assertEquals(emptyAccepted.status, CommentaryResponseStatus.NoCommentary)
    assertEquals(emptyAccepted.render.evidenceRefs.exists(_.kind == EvidenceRefKind.EngineCertification), false)

    assertEquals(forgedBinding.internal.flatMap(_.engineIntake.map(_.status)), Some(CommentaryEngineIntakeStatus.Accepted))
    assertEquals(forgedBinding.status, CommentaryResponseStatus.NoCommentary)
    assertEquals(forgedBinding.render.evidenceRefs.exists(_.kind == EvidenceRefKind.EngineCertification), false)
    assertEquals(wrongId.status, CommentaryResponseStatus.NoCommentary)
    assertEquals(wrongId.render.evidenceRefs.exists(_.kind == EvidenceRefKind.EngineCertification), false)
    assertEquals(wrongAnchor.status, CommentaryResponseStatus.NoCommentary)
    assertEquals(wrongAnchor.render.evidenceRefs.exists(_.kind == EvidenceRefKind.EngineCertification), false)

    assertEquals(boundedAccepted.internal.flatMap(_.engineIntake.map(_.status)), Some(CommentaryEngineIntakeStatus.Accepted))
    assertEquals(boundedAccepted.status, CommentaryResponseStatus.Rendered)
    assertEquals(boundedAccepted.render.evidenceRefs.exists(ref => ref.kind == EvidenceRefKind.EngineCertification && ref.id == canonicalEngineRef), true)

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

  test("raw source context fields in request JSON are ignored and cannot become render truth"):
    val rawSourceJson = Json.obj(
      "currentFen" -> validFen,
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
    val response = CommentaryBackendSeam.render(request())

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
    assertEquals((responseJson \ "render" \ "blocks" \ 0 \ "role").as[String], "primary")
    assertEquals((responseJson \ "render" \ "suppressions").as[Vector[String]], Vector.empty)

  test("contract docs name the backend seam and forbid frontend/source live wiring"):
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
      "no frontend wiring"
    ).foreach(token => assert(contract.contains(token), token))
    assert(core.contains("CommentaryBackendSeamContract.md"))
    assert(validation.contains("CommentaryBackendSeamContractTest.scala"))

  private def request(
      currentFen: String = validFen,
      enginePacket: Option[CertificationEngineRuntimeIntake.RuntimeEnginePacket] = None,
      debug: Boolean = false,
      nodeId: String = nodeId,
      ply: Int = 0
  ): CommentaryRequest =
    CommentaryRequest(
      currentFen = currentFen,
      beforeFen = None,
      playedMove = None,
      nodeId = nodeId,
      ply = ply,
      enginePacket = enginePacket,
      debug = debug
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

  private def engineCertifiedLead(
      id: String,
      engineEvidenceId: String = "api-bounded-engine-certification",
      ownerValue: String = "white",
      anchorValue: String = "board"
  ): CommentaryClaim =
    val owner = Some(ownerValue)
    val defender = if ownerValue == "white" then Some("black") else Some("white")
    val anchor = Some(anchorValue)
    val route = Some("api_engine_certified_route")
    val scope = Some("position_local")
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

  private def runtimeEngineClaim(): CertificationEngineRuntimeIntake.RuntimeCertificationClaim =
    CertificationEngineRuntimeIntake.RuntimeCertificationClaim(
      familyId = "CertifiedKingSafetyEdge",
      owner = "white",
      purposes = Map("comparative_superiority" -> "satisfied"),
      minDepth = 18,
      minMultiPv = 1,
      minPvPlies = 1,
      requiredScore = Some(CertificationEngineRuntimeIntake.RuntimeScoreRequirement.CentipawnAtLeast(50))
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
