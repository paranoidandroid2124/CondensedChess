package lila.commentary.selection

import play.api.libs.json.Json

import lila.commentary.api.*
import lila.commentary.api.CommentaryApiJson.given
import lila.commentary.render.*

class PreparedVariationEvidenceContractTest extends munit.FunSuite:

  private val validFen = "r1bqkbnr/pppp1ppp/2n5/4p3/3PP3/2N2N2/PPP2PPP/R1BQKB1R b KQkq - 3 3"
  private val claimId = "certified-pressure-line"
  private val owner = Some("white")
  private val defender = Some("black")
  private val anchor = Some("board")
  private val route = Some("pressure_route")
  private val scope = Some("position_local")

  test("safe strong prepared variation evidence passes through selection into outline plan"):
    val claim = boardClaim(claimId, strongLineProofs(claimId))

    val outline = ClaimSelector.select(Vector(claim))
    val plan = CommentaryOutlineBuilder.build(outline)

    assertEquals(outline.lead.map(_.claim.id), Some(claimId))
    assertEquals(outline.variationEvidence.map(_.proofId), Vector("line-proof-safe", "line-proof-defender"))
    assertEquals(plan.variationEvidence.map(_.proof.proofId), Vector("line-proof-safe", "line-proof-defender"))
    assertEquals(plan.variationEvidence.head.proof.proves, "pressure_preserved")
    assertEquals(plan.main.toVector.flatMap(_.claims).flatMap(_.claim.variationEvidence).map(_.proofId), Vector("line-proof-safe", "line-proof-defender"))

  test("candidate-only prepared variation evidence stays on the selected claim but not public outline evidence"):
    val claim = boardClaim(claimId, Vector(safeProof(claimId)))

    val outline = ClaimSelector.select(Vector(claim))
    val plan = CommentaryOutlineBuilder.build(outline)
    val render = CommentaryRenderer.render(plan)

    assertEquals(outline.lead.map(_.claim.id), Some(claimId))
    assertEquals(outline.lead.toVector.flatMap(_.claim.variationEvidence).map(_.proofId), Vector("line-proof-safe"))
    assertEquals(outline.variationEvidence, Vector.empty)
    assertEquals(plan.variationEvidence, Vector.empty)
    assertEquals(render.variationEvidence, Vector.empty)
    assertEquals(render.blocks.head.variationEvidenceIds, Vector.empty)

  test("support and negative proofs emit only when a selected strong line exists"):
    val strongLead =
      boardClaim(claimId, Vector(safeProof(claimId), defenderProof(claimId), failedProof(claimId)))
    val support =
      boardClaim("support-line-claim", Vector(supportProof("support-line-claim")))
        .copy(impact = ClaimImpact(resultMaterialImpact = 20, evidenceConfidence = 60, boardExplainability = 60))
    val strongOutline = ClaimSelector.select(Vector(strongLead, support))
    val weakOutline = ClaimSelector.select(Vector(boardClaim(claimId, Vector(safeProof(claimId))), support))

    assertEquals(
      strongOutline.variationEvidence.map(_.proofId),
      Vector("line-proof-safe", "line-proof-defender", "support-line-proof", "failed-line-proof")
    )
    assertEquals(weakOutline.lead.map(_.claim.id), Some(claimId))
    assertEquals(weakOutline.support.map(_.claim.id), Vector("support-line-claim"))
    assertEquals(weakOutline.variationEvidence, Vector.empty)

  test("source line-test refs can select context but cannot render a weak candidate-only proof"):
    val claim = boardClaim(claimId, Vector(safeProof(claimId)))
    val context = sourceLineTestClaim("opening-line-test-context", "line-proof-safe")

    val outline = ClaimSelector.select(Vector(claim, context))
    val render = CommentaryRenderer.render(CommentaryOutlineBuilder.build(outline))

    assertEquals(outline.lead.map(_.claim.id), Some(claimId))
    assertEquals(outline.context.map(_.claim.id), Vector("opening-line-test-context"))
    assertEquals(outline.variationEvidence, Vector.empty)
    assertEquals(render.variationEvidence, Vector.empty)
    assertEquals(render.blocks.flatMap(_.variationEvidenceIds), Vector.empty)

  test("release-risk-only variation evidence is support-only and cannot lead or render"):
    val releaseRisk =
      safeProof("release-risk-only").copy(
        proofId = "release-risk-line",
        boundClaimId = "release-risk-only",
        role = VariationEvidenceRole.ReleaseRisk,
        testResult = VariationTestResult.ReleasesCounterplay,
        proves = "line_releases_counterplay",
        proofPurpose = VariationProofPurpose.ReleasesCounterplay
      )

    val outline = ClaimSelector.select(Vector(boardClaim("release-risk-only", Vector(releaseRisk))))
    val plan = CommentaryOutlineBuilder.build(outline)
    val render = CommentaryRenderer.render(plan)

    assertEquals(outline.lead, None)
    assertEquals(plan.main, None)
    assertEquals(outline.variationEvidence, Vector.empty)
    assertEquals(render.variationEvidence, Vector.empty)
    assertEquals(render.blocks.flatMap(_.variationEvidenceIds), Vector.empty)
    assertSuppressed(outline, "release-risk-only", SuppressionReason.SupportOnly)

  test("internal-shaped source line-test proof ids do not admit context or leak publicly"):
    val unsafeProofId = "branch-id-cache-key-probe-payload"
    val claim =
      boardClaim(claimId, Vector(safeProof(claimId).copy(proofId = unsafeProofId), defenderProof(claimId)))
    val context = sourceLineTestClaim("opening-internal-line-test-context", unsafeProofId)

    val outline = ClaimSelector.select(Vector(claim, context))
    val render = CommentaryRenderer.render(CommentaryOutlineBuilder.build(outline))
    val seam = CommentaryBackendSeam.withClaimProvider(_ => Vector(claim, context))
    val response = seam.renderDebug(request())
    val publicText = Json.toJson(render).toString + Json.toJson(response.render).toString

    assertEquals(outline.context, Vector.empty)
    assertEquals(outline.variationEvidence, Vector.empty)
    assertEquals(render.variationEvidence, Vector.empty)
    assertEquals(response.render.variationEvidence, Vector.empty)
    assert(!publicText.contains(unsafeProofId), clues(publicText))
    assertSuppressed(outline, claimId, SuppressionReason.RawEngineOnly)
    assertSuppressed(outline, claimId, SuppressionReason.NoBoardReason)

  test("unsafe raw-style prepared variation evidence is suppressed before outline"):
    val unsafe = safeProof(claimId).copy(
      publicSafe = false,
      proves = "raw_engine_pv_best_forced_result",
      debug = Some(
        PreparedVariationDebug(
          variationHash = Some("debug-variation-hash"),
          engineConfigFingerprint = Some("debug-engine-config"),
          rawPacketId = Some("raw-engine-packet"),
          rawLineIndex = Some(0)
        )
      )
    )
    val outline = ClaimSelector.select(Vector(boardClaim(claimId, Vector(unsafe))))

    assertEquals(outline.lead, None)
    assertEquals(outline.variationEvidence, Vector.empty)
    assertSuppressed(outline, claimId, SuppressionReason.RawEngineOnly)
    assertSuppressed(outline, claimId, SuppressionReason.NoBoardReason)

  test("outline carries prepared variation evidence without inferring new meaning"):
    val originalImpact = ClaimImpact(evidenceConfidence = 40, boardExplainability = 35)
    val original = boardClaim(claimId, strongLineProofs(claimId)).copy(impact = originalImpact)

    val plan = CommentaryOutlineBuilder.build(ClaimSelector.select(Vector(original)))

    val selected = plan.main.get.claims.head.claim
    assertEquals(selected.impact, originalImpact)
    assertEquals(selected.band, original.band)
    assertEquals(selected.route, original.route)
    assertEquals(plan.variationEvidence.head.proof.proofPurpose, VariationProofPurpose.PreservesPressure)
    assertEquals(plan.variationEvidence.head.proof.wordingCap, WordingStrength.QualifiedSupport)

  test("renderer exposes only public-safe variation fields and never debug internals"):
    val proof = safeProof(claimId).copy(
      debug = Some(
        PreparedVariationDebug(
          variationHash = Some("debug-hash"),
          engineConfigFingerprint = Some("stockfish-private-config"),
          rawPacketId = Some("raw-packet-1"),
          rawLineIndex = Some(2)
        )
      )
    )
    val plan = CommentaryOutlineBuilder.build(ClaimSelector.select(Vector(boardClaim(claimId, Vector(proof, defenderProof(claimId))))))
    val render = CommentaryRenderer.render(plan)

    assertEquals(render.variationEvidence.map(_.proofId), Vector("line-proof-safe", "line-proof-defender"))
    assertEquals(render.variationEvidence.head.lineSan, Vector("Nf6", "Ng5"))
    assertEquals(render.variationEvidence.head.boundary.legalReplayChecked, true)
    assertEquals(render.variationEvidence.head.boundary.freshnessChecked, true)
    assertEquals(render.blocks.head.variationEvidenceIds, Vector("line-proof-safe", "line-proof-defender"))
    val renderedText = Json.toJson(render).toString
    assert(!renderedText.contains("debug-hash"), clues(renderedText))
    assert(!renderedText.contains("stockfish-private-config"), clues(renderedText))
    assert(!renderedText.contains("raw-packet-1"), clues(renderedText))
    assert(!renderedText.contains("rawLineIndex"), clues(renderedText))

  test("backend public response preserves variation evidence without leaking internal fields"):
    val proof = safeProof(claimId).copy(
      debug = Some(
        PreparedVariationDebug(
          variationHash = Some("internal-hash"),
          engineConfigFingerprint = Some("internal-engine-config"),
          rawPacketId = Some("internal-raw-packet")
        )
      )
    )
    val seam = CommentaryBackendSeam.withClaimProvider(_ => Vector(boardClaim(claimId, Vector(proof, defenderProof(claimId)))))

    val response = seam.renderDebug(request())
    val responseText = Json.toJson(response).toString

    assertEquals(response.status, CommentaryResponseStatus.Rendered)
    assertEquals(response.render.variationEvidence.map(_.proofId), Vector("line-proof-safe", "line-proof-defender"))
    assertEquals(response.render.suppressions, Vector.empty)
    assert(!responseText.contains("internal-hash"), clues(responseText))
    assert(!responseText.contains("internal-engine-config"), clues(responseText))
    assert(!responseText.contains("internal-raw-packet"), clues(responseText))

  test("source context and raw engine variation evidence do not become board truth owners"):
    val sourceWithLine =
      SourceContextClaimBoundary
        .toClaim(
          SourceContextCandidate(
            candidateId = "opening-context-with-line",
            kind = SourceContextKind.Opening,
            sourceRefs = Vector(
              "opening-position:catalan-main:canonical",
              "opening-source-use:master_reference"
            )
          )
        )
        .copy(variationEvidence = Vector(safeProof("opening-context-with-line")))
    val rawEngineWithLine = CommentaryClaim(
      id = "raw-engine-with-line",
      layer = ClaimLayer.Engine,
      status = ClaimStatus.Admitted,
      evidenceRefs = Vector(EvidenceRef(EvidenceRefKind.RawEngine, "raw-pv-packet")),
      variationEvidence = Vector(safeProof("raw-engine-with-line")),
      wordingStrengthCap = WordingStrength.AssertiveCertified
    )

    val outline = ClaimSelector.select(Vector(sourceWithLine, rawEngineWithLine))

    assertEquals(outline.lead, None)
    assertEquals(outline.context, Vector.empty)
    assertEquals(outline.variationEvidence, Vector.empty)
    assertSuppressed(outline, "opening-context-with-line", SuppressionReason.SourceContextOnly)
    assertSuppressed(outline, "opening-context-with-line", SuppressionReason.NoBoardReason)
    assertSuppressed(outline, "raw-engine-with-line", SuppressionReason.RawEngineOnly)
    assertSuppressed(outline, "raw-engine-with-line", SuppressionReason.NoBoardReason)

  test("defender-resource evidence is retained only when legal fresh depth-bounded and provenance-bound"):
    val defenderResource =
      safeProof(claimId).copy(
        proofId = "defender-resource-line",
        role = VariationEvidenceRole.DefenderResource,
        moveRole = VariationMoveRole.DefenderResource,
        testedMove = Some(VariationMove("...Qb6", "d8b6")),
        resourceLine = Vector(VariationMove("...Qb6", "d8b6"), VariationMove("Qd2", "d1d2")),
        testResult = VariationTestResult.ResourceFails,
        proves = "defender_resource_fails_to_restore_counterplay",
        proofPurpose = VariationProofPurpose.DeniesResource,
        surfaceAllowance = VariationSurfaceAllowance.PublicLine
      )
    val staleResource =
      defenderResource.copy(
        proofId = "stale-defender-resource",
        boundary = defenderResource.boundary.copy(freshnessChecked = false)
      )
    val illegalResource =
      defenderResource.copy(
        proofId = "illegal-defender-resource",
        boundary = defenderResource.boundary.copy(legalReplayChecked = false)
      )
    val unboundProvenance =
      defenderResource.copy(
        proofId = "unbound-provenance-resource",
        provenanceRefs = Vector(EvidenceRef(EvidenceRefKind.Certification, "CertifiedLine", owner, anchor, route, None))
      )

    val safeRender = CommentaryRenderer.render(
      CommentaryOutlineBuilder.build(ClaimSelector.select(Vector(boardClaim(claimId, Vector(safeProof(claimId), defenderResource)))))
    )
    val staleOutline = ClaimSelector.select(Vector(boardClaim(claimId, Vector(safeProof(claimId), staleResource))))
    val illegalOutline = ClaimSelector.select(Vector(boardClaim(claimId, Vector(safeProof(claimId), illegalResource))))
    val unboundOutline = ClaimSelector.select(Vector(boardClaim(claimId, Vector(safeProof(claimId), unboundProvenance))))

    assertEquals(safeRender.variationEvidence.map(_.proofId), Vector("line-proof-safe", "defender-resource-line"))
    assertEquals(safeRender.variationEvidence.last.role, RenderLineRole.Resource)
    assertEquals(safeRender.variationEvidence.last.testResult, VariationTestResult.ResourceFails)
    assertEquals(safeRender.variationEvidence.last.resourceLine.map(_.uci), Vector("d8b6", "d1d2"))
    assertEquals(safeRender.variationEvidence.last.provenanceRefs.map(_.id), Vector("CertifiedLine"))
    assertEquals(staleOutline.variationEvidence, Vector.empty)
    assertSuppressed(staleOutline, claimId, SuppressionReason.RawEngineOnly)
    assertEquals(illegalOutline.variationEvidence, Vector.empty)
    assertSuppressed(illegalOutline, claimId, SuppressionReason.RawEngineOnly)
    assertEquals(unboundOutline.variationEvidence, Vector.empty)
    assertSuppressed(unboundOutline, claimId, SuppressionReason.NoBoardReason)

  test("failed tempting move evidence cannot become a main recommendation by itself"):
    val failedTemptation =
      safeProof("failed-rush-e5").copy(
        proofId = "failed-rush-e5-line",
        boundClaimId = "failed-rush-e5",
        role = VariationEvidenceRole.FailedTemptingMove,
        testedMove = Some(VariationMove("e5", "e4e5")),
        testedLine = Vector(VariationMove("e5", "e4e5"), VariationMove("...Nd5", "c7d5")),
        replyLine = Vector(VariationMove("...Nd5", "c7d5")),
        testResult = VariationTestResult.MovePremature,
        proves = "tempting_move_is_premature",
        proofPurpose = VariationProofPurpose.Fails,
        wordingCap = WordingStrength.NegativeOnly
      )
    val outline = ClaimSelector.select(Vector(boardClaim("failed-rush-e5", Vector(failedTemptation))))

    assertEquals(outline.lead, None)
    assertEquals(outline.variationEvidence, Vector.empty)
    assertSuppressed(outline, "failed-rush-e5", SuppressionReason.SupportOnly)

  test("variation wording caps and proof tokens block best forced result overclaims"):
    val overclaim =
      safeProof(claimId).copy(
        proofId = "overclaim-line",
        role = VariationEvidenceRole.Conversion,
        testResult = VariationTestResult.Converts,
        proves = "best_forced_result_conversion",
        wordingCap = WordingStrength.AssertiveCertified
      )
    val outline = ClaimSelector.select(Vector(boardClaim(claimId, Vector(overclaim))))

    assertEquals(outline.lead, None)
    assertEquals(outline.variationEvidence, Vector.empty)
    assertSuppressed(outline, claimId, SuppressionReason.RawEngineOnly)
    assertSuppressed(outline, claimId, SuppressionReason.NoBoardReason)

  test("internal-only or stale line evidence is suppressed and not rendered"):
    val internalOnly =
      safeProof(claimId).copy(
        proofId = "internal-only-line",
        surfaceAllowance = VariationSurfaceAllowance.InternalOnly
      )
    val outline = ClaimSelector.select(Vector(boardClaim(claimId, Vector(internalOnly))))
    val render = CommentaryRenderer.render(CommentaryOutlineBuilder.build(outline))

    assertEquals(outline.lead, None)
    assertEquals(outline.variationEvidence, Vector.empty)
    assertEquals(render.variationEvidence, Vector.empty)
    assertSuppressed(outline, claimId, SuppressionReason.RawEngineOnly)

  test("renderer and backend do not expose internal defender-resource proof packets"):
    val proof = safeProof(claimId).copy(
      role = VariationEvidenceRole.DefenderResource,
      testResult = VariationTestResult.DoesNotRestoreCounterplay,
      debug = Some(
        PreparedVariationDebug(
          variationHash = Some("defender-debug-hash"),
          engineConfigFingerprint = Some("defender-engine-config"),
          rawPacketId = Some("defender-raw-packet"),
          rawLineIndex = Some(7)
        )
      )
    )
    val seam = CommentaryBackendSeam.withClaimProvider(_ => Vector(boardClaim(claimId, Vector(safeProof(claimId), proof))))
    val response = seam.renderDebug(request())
    val responseText = Json.toJson(response).toString

    assertEquals(response.render.variationEvidence.map(_.role), Vector(RenderLineRole.Pressure, RenderLineRole.Resource))
    assert(!responseText.contains("defender-debug-hash"), clues(responseText))
    assert(!responseText.contains("defender-engine-config"), clues(responseText))
    assert(!responseText.contains("defender-raw-packet"), clues(responseText))
    assert(!responseText.contains("rawLineIndex"), clues(responseText))

  private def request(): CommentaryRequest =
    CommentaryRequest(
      currentFen = validFen,
      beforeFen = None,
      playedMove = None,
      nodeId = "mainline:0",
      ply = 0
    )

  private def boardClaim(id: String, variationEvidence: Vector[PreparedVariationEvidence]): CommentaryClaim =
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
      impact = ClaimImpact(resultMaterialImpact = 60, evidenceConfidence = 80, boardExplainability = 70),
      evidenceRefs = Vector(
        EvidenceRef(EvidenceRefKind.Certification, "CertifiedLine", owner, anchor, route, scope),
        EvidenceRef(EvidenceRefKind.Delta, "TradeInvariant", owner, anchor, route, scope)
      ),
      exactBoardBound = true,
      wordingStrengthCap = WordingStrength.QualifiedSupport,
      variationEvidence = variationEvidence
    )

  private def safeProof(boundClaimId: String): PreparedVariationEvidence =
    PreparedVariationEvidence(
      proofId = "line-proof-safe",
      boundClaimId = boundClaimId,
      startFen = validFen,
      owner = "white",
      defender = Some("black"),
      anchor = "board",
      route = "pressure_route",
      scope = "position_local",
      moveRole = VariationMoveRole.CandidateMove,
      lineSan = Vector("Nf6", "Ng5"),
      lineUci = Vector("g8f6", "f3g5"),
      playedMove = None,
      candidateMove = Some(VariationMove("Nf6", "g8f6")),
      defenderResource = None,
      continuation = Vector(VariationMove("Ng5", "f3g5")),
      role = VariationEvidenceRole.Persistence,
      testedMove = Some(VariationMove("Nf6", "g8f6")),
      testedLine = Vector(VariationMove("Nf6", "g8f6"), VariationMove("Ng5", "f3g5")),
      replyLine = Vector(VariationMove("Ng5", "f3g5")),
      resourceLine = Vector.empty,
      testResult = VariationTestResult.PressurePersists,
      proves = "pressure_preserved",
      proofPurpose = VariationProofPurpose.PreservesPressure,
      boundary = PreparedVariationBoundary(
        depthFloor = 18,
        realizedDepth = 20,
        multiPv = 3,
        freshnessChecked = true,
        legalReplayChecked = true,
        baselineChecked = false
      ),
      wordingCap = WordingStrength.QualifiedSupport,
      provenanceRefs = Vector(EvidenceRef(EvidenceRefKind.Certification, "CertifiedLine", owner, anchor, route, scope)),
      surfaceAllowance = VariationSurfaceAllowance.PublicLine,
      publicSafe = true
    )

  private def defenderProof(boundClaimId: String): PreparedVariationEvidence =
    safeProof(boundClaimId).copy(
      proofId = "line-proof-defender",
      role = VariationEvidenceRole.DefenderResource,
      moveRole = VariationMoveRole.DefenderResource,
      defenderResource = Some(VariationMove("...Qb6", "d8b6")),
      resourceLine = Vector(VariationMove("...Qb6", "d8b6"), VariationMove("Qd2", "d1d2")),
      testResult = VariationTestResult.DoesNotRestoreCounterplay,
      proves = "defender_resource_does_not_restore_counterplay",
      proofPurpose = VariationProofPurpose.DeniesResource
    )

  private def strongLineProofs(boundClaimId: String): Vector[PreparedVariationEvidence] =
    Vector(safeProof(boundClaimId), defenderProof(boundClaimId))

  private def supportProof(boundClaimId: String): PreparedVariationEvidence =
    safeProof(boundClaimId).copy(
      proofId = "support-line-proof",
      role = VariationEvidenceRole.Conversion,
      moveRole = VariationMoveRole.Continuation,
      candidateMove = None,
      continuation = Vector(VariationMove("Bb5", "f1b5")),
      testResult = VariationTestResult.Converts,
      proves = "bounded_conversion_continues",
      proofPurpose = VariationProofPurpose.Simplifies
    )

  private def failedProof(boundClaimId: String): PreparedVariationEvidence =
    safeProof(boundClaimId).copy(
      proofId = "failed-line-proof",
      role = VariationEvidenceRole.FailedTemptingMove,
      wordingCap = WordingStrength.NegativeOnly,
      testResult = VariationTestResult.MovePremature,
      proves = "tempting_move_fails",
      proofPurpose = VariationProofPurpose.Fails
    )

  private def sourceLineTestClaim(id: String, proofId: String): CommentaryClaim =
    SourceContextClaimBoundary.toClaim(
      SourceContextCandidate(
        candidateId = id,
        kind = SourceContextKind.Opening,
        sourceRefs = Vector(
          "opening-position:catalan-main:canonical",
          s"opening-line-test:$proofId:context"
        )
      )
    )

  private def assertSuppressed(outline: CommentaryOutline, claimId: String, reason: SuppressionReason): Unit =
    assert(
      outline.suppressedClaims.exists(suppressed =>
        suppressed.claim.id == claimId && suppressed.reasons.contains(reason)
      ),
      clues(outline.suppressedClaims)
    )
