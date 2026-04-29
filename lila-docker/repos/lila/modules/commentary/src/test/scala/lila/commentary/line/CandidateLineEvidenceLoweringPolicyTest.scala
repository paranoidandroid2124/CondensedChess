package lila.commentary.line

import play.api.libs.json.Json

import lila.commentary.api.*
import lila.commentary.api.CommentaryApiJson.given
import lila.commentary.render.CommentaryRenderer
import lila.commentary.selection.*

class CandidateLineEvidenceLoweringPolicyTest extends munit.FunSuite:

  private val startFen = "r1bqkbnr/pppp1ppp/2n5/4p3/3PP3/2N2N2/PPP2PPP/R1BQKB1R b KQkq - 3 3"
  private val claimId = "prepared-line-claim"
  private val nowEpochMs = 20_000L
  private val owner = Some("white")
  private val defender = Some("black")
  private val anchor = Some("board")
  private val route = Some("pressure_route")
  private val scope = Some("position_local")
  private val binding =
    CandidateLineEvidenceLowering.Binding(
      boundClaimId = claimId,
      owner = owner.get,
      defender = defender,
      anchor = anchor.get,
      route = route.get,
      scope = scope.get,
      provenanceRef = EvidenceRef(EvidenceRefKind.Certification, "CertifiedLine", owner, anchor, route, scope)
    )

  test("depth 16 candidate line lowers and depth 15 fails closed"):
    val depth16 = lower(rootSet(realizedDepth = 16))
    val depth15 = lower(rootSet(realizedDepth = 15))

    assertEquals(depth16.map(_.proofId), Vector("prepared-line-claim-line-1", "prepared-line-claim-line-2", "prepared-line-claim-line-3"))
    assertEquals(depth16.head.boundary.depthFloor, 16)
    assertEquals(depth16.head.boundary.realizedDepth, 16)
    assertEquals(depth15, Vector.empty)

  test("root MultiPV 3 lowers rank 1 main and ranks 2 and 3 as alternatives"):
    val proofs =
      lower(
        Vector(
          root(rank = 1, branchId = "root-candidate-1", multiPvIndex = 1, lineSan = Vector("Nf6"), lineUci = Vector("g8f6")),
          root(rank = 2, branchId = "root-candidate-2", multiPvIndex = 2, lineSan = Vector("a6"), lineUci = Vector("a7a6")),
          root(rank = 3, branchId = "root-candidate-3", multiPvIndex = 3, lineSan = Vector("Nge7"), lineUci = Vector("g8e7"))
        )
      )

    assertEquals(proofs.map(_.proofId), Vector("prepared-line-claim-line-1", "prepared-line-claim-line-2", "prepared-line-claim-line-3"))
    assertEquals(proofs.map(_.proves), Vector("main_candidate_line", "alternative_candidate_line", "context_candidate_line"))
    assertEquals(proofs.map(_.moveRole), Vector(VariationMoveRole.CandidateMove, VariationMoveRole.CandidateMove, VariationMoveRole.CandidateMove))
    assertEquals(proofs.map(_.role), Vector(VariationEvidenceRole.Persistence, VariationEvidenceRole.Persistence, VariationEvidenceRole.Persistence))
    assertEquals(proofs.map(_.boundary.multiPv), Vector(3, 3, 3))

  test("duplicate root ranks or MultiPV indexes fail closed before lowering"):
    val duplicateRank =
      lower(
        Vector(
          root(rank = 1, branchId = "root-candidate-1", multiPvIndex = 1),
          root(rank = 1, branchId = "root-candidate-duplicate", multiPvIndex = 2),
          root(rank = 3, branchId = "root-candidate-3", multiPvIndex = 3)
        )
      )
    val duplicateMultiPvIndex =
      lower(
        Vector(
          root(rank = 1, branchId = "root-candidate-1", multiPvIndex = 1),
          root(rank = 2, branchId = "root-candidate-2", multiPvIndex = 1),
          root(rank = 3, branchId = "root-candidate-3", multiPvIndex = 3)
        )
      )

    assertEquals(duplicateRank, Vector.empty)
    assertEquals(duplicateMultiPvIndex, Vector.empty)

  test("permuted root rank and MultiPV index pairs fail closed before lowering"):
    val proofs =
      lower(
        Vector(
          root(rank = 1, branchId = "root-candidate-1", multiPvIndex = 3),
          root(rank = 2, branchId = "root-candidate-2", multiPvIndex = 2),
          root(rank = 3, branchId = "root-candidate-3", multiPvIndex = 1)
        )
      )

    assertEquals(proofs, Vector.empty)

  test("root MultiPV 1 does not lower as full candidate-line exploration"):
    val proofs = lower(Vector(root(rank = 1, multiPv = 1)))

    assertEquals(proofs, Vector.empty)

  test("root MultiPV 2 does not lower at public-safe candidate-line boundary"):
    val proofs =
      lower(
        Vector(
          root(rank = 1, branchId = "root-candidate-1", multiPv = 2, multiPvIndex = 1),
          root(rank = 2, branchId = "root-candidate-2", multiPv = 2, multiPvIndex = 2)
        )
      )

    assertEquals(proofs, Vector.empty)

  test("root MultiPV above 3 is rejected for lowering"):
    val proofs =
      lower(
        Vector(
          root(rank = 1, branchId = "root-candidate-1", multiPv = 4, multiPvIndex = 1),
          root(rank = 2, branchId = "root-candidate-2", multiPv = 4, multiPvIndex = 2),
          root(rank = 3, branchId = "root-candidate-3", multiPv = 4, multiPvIndex = 3),
          root(rank = 4, branchId = "root-candidate-4", multiPv = 4, multiPvIndex = 4)
        )
      )

    assertEquals(proofs, Vector.empty)

  test("child MultiPV 2 lowers two defender replies for eligible top-two root candidate"):
    val parent = CandidateBranchId("root-candidate-2")
    val proofs =
      lower(
        Vector(
          root(rank = 1, branchId = "root-candidate-1", multiPvIndex = 1),
          root(rank = 2, branchId = parent.value, multiPvIndex = 2),
          root(rank = 3, branchId = "root-candidate-3", multiPvIndex = 3),
          child(
            branchId = "defender-reply-1",
            parentBranchId = parent,
            rank = 1,
            multiPv = 2,
            multiPvIndex = 1,
            lineSan = Vector("a6", "Ba4"),
            lineUci = Vector("a7a6", "f1a4")
          ),
          child(
            branchId = "defender-reply-2",
            parentBranchId = parent,
            rank = 2,
            multiPv = 2,
            multiPvIndex = 2,
            lineSan = Vector("Nf6", "O-O"),
            lineUci = Vector("g8f6", "e1g1")
          )
        )
      )
    val replies = proofs.filter(_.role == VariationEvidenceRole.DefenderResource)

    assertEquals(replies.map(_.proofId), Vector("prepared-line-claim-line-4", "prepared-line-claim-line-5"))
    assertEquals(replies.map(_.resourceLine.map(_.uci)), Vector(Vector("a7a6", "f1a4"), Vector("g8f6", "e1g1")))
    assertEquals(replies.map(_.boundary.multiPv), Vector(2, 2))

  test("child MultiPV 1 does not lower at public-safe candidate-line boundary"):
    val parent = CandidateBranchId("root-candidate-1")
    val proofs =
      lower(
        rootSet() :+
          child(branchId = "defender-reply-under-budget", parentBranchId = parent, multiPv = 1, multiPvIndex = 1)
      )

    assertEquals(proofs.count(_.role == VariationEvidenceRole.DefenderResource), 0)

  test("child evidence for rank 3 root is ignored unless explicitly allowed"):
    val rank3 = CandidateBranchId("root-candidate-3")
    val defaultProofs =
      lower(
        Vector(
          root(rank = 1, branchId = "root-candidate-1", multiPvIndex = 1),
          root(rank = 2, branchId = "root-candidate-2", multiPvIndex = 2),
          root(rank = 3, branchId = rank3.value, multiPvIndex = 3),
          child(branchId = "rank-three-child", parentBranchId = rank3)
        )
      )
    val explicitProofs =
      CandidateLineEvidenceLowering.lower(
        Vector(
          root(rank = 1, branchId = "root-candidate-1", multiPvIndex = 1),
          root(rank = 2, branchId = "root-candidate-2", multiPvIndex = 2),
          root(rank = 3, branchId = rank3.value, multiPvIndex = 3),
          child(branchId = "rank-three-child", parentBranchId = rank3)
        ),
        binding = binding,
        nowEpochMs = nowEpochMs,
        policy = CandidateLineEvidenceLowering.Policy.Default.copy(allowThirdRootChildProof = true)
      )

    assertEquals(defaultProofs.count(_.role == VariationEvidenceRole.DefenderResource), 0)
    assertEquals(explicitProofs.count(_.role == VariationEvidenceRole.DefenderResource), 1)

  test("child evidence without publicLineEligibleAt fails closed"):
    val parent = CandidateBranchId("root-candidate-1")
    val roots =
      rootSet().map:
        case line if line.branchId == CandidateBranchId("root-candidate-1") => line.copy(branchId = parent)
        case line                                                           => line
    val proofs =
      lower(
        roots :+ child(branchId = "shallow-child", parentBranchId = parent, realizedDepth = 15)
      )

    assertEquals(proofs.count(_.role == VariationEvidenceRole.DefenderResource), 0)

  test("source_hint candidate evidence cannot lower as proof-owned prepared variation evidence"):
    val proofs =
      lower(
        Vector(
          root(
            rank = 1,
            provenance = CandidateLineProvenance(
              CandidateLineProvenanceKind.SourceHint,
              sourceHintRefs = Vector("opening-line-test:catalan:context")
            )
          ),
          root(rank = 2, branchId = "root-candidate-2", multiPvIndex = 2)
        )
      )

    assertEquals(proofs, Vector.empty)

  test("source-context or raw-engine binding provenance cannot lower as public-safe proof owner"):
    val evidence = rootSet()
    val sourceBinding =
      binding.copy(
        provenanceRef = EvidenceRef(EvidenceRefKind.SourceContext, "retrieval-line-test:example:context", owner, anchor, route, scope)
      )
    val rawEngineBinding =
      binding.copy(
        provenanceRef = EvidenceRef(EvidenceRefKind.RawEngine, "raw-engine-packet", owner, anchor, route, scope)
      )
    val unboundBinding =
      binding.copy(
        provenanceRef = EvidenceRef(EvidenceRefKind.Certification, "unbound-certification", owner, anchor, route, None)
      )

    assertEquals(CandidateLineEvidenceLowering.lower(evidence, sourceBinding, nowEpochMs), Vector.empty)
    assertEquals(CandidateLineEvidenceLowering.lower(evidence, rawEngineBinding, nowEpochMs), Vector.empty)
    assertEquals(CandidateLineEvidenceLowering.lower(evidence, unboundBinding, nowEpochMs), Vector.empty)

  test("unsafe certification provenance ids fail closed before public prepared variation evidence"):
    val evidence = rootSet()
    val branchIdBinding =
      binding.copy(
        provenanceRef = EvidenceRef(EvidenceRefKind.Certification, "branchId:root-candidate-1", owner, anchor, route, scope)
      )
    val cacheKeyBinding =
      binding.copy(
        provenanceRef = EvidenceRef(EvidenceRefKind.Certification, "cacheKey:rank3-depth18", owner, anchor, route, scope)
      )

    assertEquals(CandidateLineEvidenceLowering.lower(evidence, branchIdBinding, nowEpochMs), Vector.empty)
    assertEquals(CandidateLineEvidenceLowering.lower(evidence, cacheKeyBinding, nowEpochMs), Vector.empty)
    assertEquals(
      CandidateLineEvidenceLowering.lower(evidence, binding, nowEpochMs).map(_.provenanceRefs.map(_.id).distinct),
      Vector(Vector("CertifiedLine"), Vector("CertifiedLine"), Vector("CertifiedLine"))
    )

  test("failed premature and release-risk candidate roles cannot lower as lead recommendation"):
    val proofs =
      lower(
        Vector(
          root(rank = 1),
          root(rank = 2, branchId = "root-candidate-2", multiPvIndex = 2),
          root(rank = 1, branchId = "failed-candidate", role = CandidateBranchRole.FailedCandidate),
          root(rank = 1, branchId = "premature-candidate", role = CandidateBranchRole.Premature),
          root(rank = 1, branchId = "release-risk-candidate", role = CandidateBranchRole.ReleaseRisk)
        )
      )

    assertEquals(proofs.exists(_.proofId.contains("failed-candidate")), false)
    assertEquals(proofs.exists(_.proofId.contains("premature-candidate")), false)
    assertEquals(proofs.exists(_.proofId.contains("release-risk-candidate")), false)

  test("raw packet PV and internal CandidateLineEvidence fields do not appear in public backend render JSON"):
    val proof = lower(rootSet()).head
    val seam = CommentaryBackendSeam.withClaimProvider(_ => Vector(boardClaim(Vector(proof))))
    val response = seam.renderDebug(request())
    val responseText = Json.toJson(response).toString
    val renderText = Json.toJson(CommentaryRenderer.render(CommentaryOutlineBuilder.build(ClaimSelector.select(Vector(boardClaim(Vector(proof))))))).toString

    Vector(
      "CandidateLineEvidence",
      "root-candidate-1",
      "root-candidate-2",
      "defender-reply-1",
      "defender-reply-2",
      "branchId",
      "parentBranchId",
      "multiPvIndex",
      "engineConfigFingerprint",
      "rawPacket",
      "rawPv",
      "pvLines",
      "stockfish-private-config",
      "sourceHintRefs"
    ).foreach: token =>
      assert(!responseText.contains(token), clues(token, responseText))
      assert(!renderText.contains(token), clues(token, renderText))

  private def lower(evidence: Vector[CandidateLineEvidence]): Vector[PreparedVariationEvidence] =
    CandidateLineEvidenceLowering.lower(evidence, binding = binding, nowEpochMs = nowEpochMs)

  private def rootSet(realizedDepth: Int = 18): Vector[CandidateLineEvidence] =
    Vector(
      root(rank = 1, branchId = "root-candidate-1", multiPvIndex = 1, realizedDepth = realizedDepth),
      root(rank = 2, branchId = "root-candidate-2", multiPvIndex = 2, realizedDepth = realizedDepth),
      root(rank = 3, branchId = "root-candidate-3", multiPvIndex = 3, realizedDepth = realizedDepth)
    )

  private def root(
      branchId: String = "root-candidate-1",
      role: CandidateBranchRole = CandidateBranchRole.RootCandidate,
      rank: Int,
      multiPvIndex: Int = 1,
      multiPv: Int = 3,
      realizedDepth: Int = 18,
      lineSan: Vector[String] = Vector("Nf6", "Bb5"),
      lineUci: Vector[String] = Vector("g8f6", "f1b5"),
      provenance: CandidateLineProvenance = CandidateLineProvenance(CandidateLineProvenanceKind.EngineRoot)
  ): CandidateLineEvidence =
    line(
      branchId = branchId,
      parentBranchId = None,
      role = role,
      rank = rank,
      multiPvIndex = multiPvIndex,
      multiPv = multiPv,
      realizedDepth = realizedDepth,
      lineSan = lineSan,
      lineUci = lineUci,
      provenance = provenance
    )

  private def child(
      branchId: String,
      parentBranchId: CandidateBranchId,
      rank: Int = 1,
      multiPvIndex: Int = 1,
      multiPv: Int = 2,
      realizedDepth: Int = 18,
      lineSan: Vector[String] = Vector("a6", "Ba4"),
      lineUci: Vector[String] = Vector("a7a6", "f1a4")
  ): CandidateLineEvidence =
    line(
      branchId = branchId,
      parentBranchId = Some(parentBranchId),
      role = CandidateBranchRole.DefenderReply,
      rank = rank,
      multiPvIndex = multiPvIndex,
      multiPv = multiPv,
      realizedDepth = realizedDepth,
      lineSan = lineSan,
      lineUci = lineUci,
      provenance = CandidateLineProvenance(CandidateLineProvenanceKind.EngineChild)
    )

  private def line(
      branchId: String,
      parentBranchId: Option[CandidateBranchId],
      role: CandidateBranchRole,
      rank: Int,
      multiPvIndex: Int,
      multiPv: Int,
      realizedDepth: Int,
      lineSan: Vector[String],
      lineUci: Vector[String],
      provenance: CandidateLineProvenance
  ): CandidateLineEvidence =
    CandidateLineEvidence(
      branchId = CandidateBranchId(branchId),
      parentBranchId = parentBranchId,
      role = role,
      rank = rank,
      multiPvIndex = multiPvIndex,
      startFen = startFen,
      startNodeId = "mainline:0",
      startPly = 3,
      sideToMove = CandidateLineSide.Black,
      lineUci = lineUci,
      lineSan = lineSan,
      requestedDepth = 18,
      realizedDepth = realizedDepth,
      multiPv = multiPv,
      timing = CandidateLineTiming(generatedAtEpochMs = 10_000L, maxAgeMs = 20_000L),
      engineConfigFingerprint = "stockfish-private-config",
      legalReplay = CandidateLineReplayStatus.Legal,
      provenance = provenance,
      surfaceAllowance = VariationSurfaceAllowance.PublicLine,
      wordingCap = WordingStrength.QualifiedSupport
    )

  private def boardClaim(variationEvidence: Vector[PreparedVariationEvidence]): CommentaryClaim =
    CommentaryClaim(
      id = claimId,
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
      evidenceRefs = Vector(binding.provenanceRef),
      exactBoardBound = true,
      wordingStrengthCap = WordingStrength.QualifiedSupport,
      variationEvidence = variationEvidence
    )

  private def request(): CommentaryRequest =
    CommentaryRequest(
      currentFen = startFen,
      beforeFen = None,
      playedMove = None,
      nodeId = "mainline:0",
      ply = 0
    )
