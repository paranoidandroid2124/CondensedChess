package lila.commentary.line

import play.api.libs.json.Json

import lila.commentary.api.{ CommentaryBackendSeam, CommentaryRequest }
import lila.commentary.api.CommentaryApiJson.given
import lila.commentary.certification.CertificationEngineRuntimeIntake

class CandidateLinePacketHandoffContractTest extends munit.FunSuite:

  private val initialFen = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1"
  private val afterE4Fen = "rnbqkbnr/pppppppp/8/8/4P3/8/PPPP1PPP/RNBQKBNR b KQkq - 0 1"
  private val nodeId = "mainline:0"
  private val nowEpochMs = 20_000L

  test("root packet bound to current FEN node and ply normalizes root candidate evidence"):
    val evidence =
      normalize(
        root = rootPacket(
          rawLines = Vector(rootLine("root-candidate-1", Vector("e2e4", "e7e5")))
        )
      )

    assertEquals(evidence.size, 1)
    assertEquals(evidence.head.branchId.value, "root-candidate-1")
    assertEquals(evidence.head.parentBranchId, None)
    assertEquals(evidence.head.role, CandidateBranchRole.RootCandidate)
    assertEquals(evidence.head.startFen, initialFen)
    assertEquals(evidence.head.startNodeId, nodeId)
    assertEquals(evidence.head.startPly, 0)
    assertEquals(evidence.head.lineSan, Vector("e4", "e5"))
    assertEquals(evidence.head.publicLineEligibleAt(nowEpochMs), true)

  test("root packet with wrong FEN node or ply fails closed"):
    val wrongFen = normalize(root = rootPacket(startFen = afterE4Fen))
    val wrongNode = normalize(root = rootPacket(nodeId = "other-node"))
    val wrongPly = normalize(root = rootPacket(ply = 1))

    assertEquals(wrongFen, Vector.empty)
    assertEquals(wrongNode, Vector.empty)
    assertEquals(wrongPly, Vector.empty)

  test("root MultiPV 2 preserves two root candidates with distinct branch ids"):
    val evidence =
      normalize(
        root = rootPacket(
          rawLines = Vector(
            rootLine("root-candidate-1", Vector("e2e4", "e7e5"), rank = 1, multiPvIndex = 1),
            rootLine("root-candidate-2", Vector("d2d4", "d7d5"), rank = 2, multiPvIndex = 2)
          ),
          multiPv = 2
        )
      )

    assertEquals(evidence.map(_.branchId.value), Vector("root-candidate-1", "root-candidate-2"))
    assertEquals(evidence.map(_.rank), Vector(1, 2))
    assertEquals(evidence.map(_.search.multiPvIndex), Vector(1, 2))
    assertEquals(evidence.forall(_.publicLineEligibleAt(nowEpochMs)), true)

  test("duplicate root branch ids fail closed before child parent binding"):
    val duplicate = CandidateBranchId("root-candidate-1")
    val evidence =
      normalize(
        root = rootPacket(
          rawLines = Vector(
            rootLine(duplicate.value, Vector("e2e4", "e7e5"), rank = 1, multiPvIndex = 1),
            rootLine(duplicate.value, Vector("d2d4", "d7d5"), rank = 2, multiPvIndex = 2)
          ),
          multiPv = 2
        ),
        children = Vector(
          childPacket(
            parentBranchId = duplicate,
            startFen = afterE4Fen,
            role = CandidateBranchRole.DefenderResource,
            parentLinePrefix = Vector("e2e4"),
            rawLines = Vector(childLine("defender-resource-1", duplicate, Vector("c7c5"), CandidateBranchRole.DefenderResource))
          )
        )
      )

    assertEquals(evidence, Vector.empty)

  test("child packet with valid parent branch id and exact child start FEN attaches as defender resource"):
    val parent = CandidateBranchId("root-candidate-1")
    val evidence =
      normalize(
        root = rootPacket(rawLines = Vector(rootLine(parent.value, Vector("e2e4", "e7e5")))),
        children = Vector(
          childPacket(
            parentBranchId = parent,
            startFen = afterE4Fen,
            role = CandidateBranchRole.DefenderResource,
            parentLinePrefix = Vector("e2e4"),
            rawLines = Vector(childLine("defender-resource-1", parent, Vector("c7c5", "g1f3"), CandidateBranchRole.DefenderResource))
          )
        )
      )

    val child = evidence.find(_.branchId.value == "defender-resource-1").get
    assertEquals(child.parentBranchId, Some(parent))
    assertEquals(child.role, CandidateBranchRole.DefenderResource)
    assertEquals(child.provenance.kind, CandidateLineProvenanceKind.EngineChild)
    assertEquals(child.startFen, afterE4Fen)
    assertEquals(child.lineSan, Vector("c5", "Nf3"))
    assertEquals(child.publicLineEligibleAt(nowEpochMs), true)

  test("child packet can attach two defender replies from MultiPV 2"):
    val parent = CandidateBranchId("root-candidate-1")
    val evidence =
      normalize(
        root = rootPacket(rawLines = Vector(rootLine(parent.value, Vector("e2e4", "e7e5")))),
        children = Vector(
          childPacket(
            parentBranchId = parent,
            startFen = afterE4Fen,
            role = CandidateBranchRole.DefenderReply,
            parentLinePrefix = Vector("e2e4"),
            multiPv = 2,
            rawLines = Vector(
              childLine("defender-reply-1", parent, Vector("e7e5", "g1f3"), CandidateBranchRole.DefenderReply, rank = 1, multiPvIndex = 1),
              childLine("defender-reply-2", parent, Vector("c7c5", "g1f3"), CandidateBranchRole.DefenderReply, rank = 2, multiPvIndex = 2)
            )
          )
        )
      )
    val replies = evidence.filter(_.role == CandidateBranchRole.DefenderReply)

    assertEquals(replies.map(_.branchId.value), Vector("defender-reply-1", "defender-reply-2"))
    assertEquals(replies.map(_.parentBranchId), Vector(Some(parent), Some(parent)))
    assertEquals(replies.map(_.rank), Vector(1, 2))
    assertEquals(replies.map(_.search.multiPvIndex), Vector(1, 2))
    assertEquals(replies.forall(_.publicLineEligibleAt(nowEpochMs)), true)

  test("child packet with missing parent branch id is rejected without discarding root evidence"):
    val missingParent = CandidateBranchId("missing-parent")
    val evidence =
      normalize(
        root = rootPacket(rawLines = Vector(rootLine("root-candidate-1", Vector("e2e4", "e7e5")))),
        children = Vector(
          childPacket(
            parentBranchId = missingParent,
            startFen = afterE4Fen,
            role = CandidateBranchRole.DefenderResource,
            parentLinePrefix = Vector("e2e4"),
            rawLines = Vector(childLine("defender-resource-1", missingParent, Vector("c7c5"), CandidateBranchRole.DefenderResource))
          )
        )
      )

    val root = evidence.find(_.role == CandidateBranchRole.RootCandidate).get
    val child = evidence.find(_.branchId.value == "defender-resource-1").get
    assertEquals(root.publicLineEligibleAt(nowEpochMs), true)
    assertEquals(child.publicLineEligibleAt(nowEpochMs), false)
    assertEquals(child.rejectionReasonsAt(nowEpochMs).contains(CandidateLineRejectionReason.MissingParentBranch), true)

  test("child packet with wrong child start FEN is rejected without discarding root evidence"):
    val parent = CandidateBranchId("root-candidate-1")
    val evidence =
      normalize(
        root = rootPacket(rawLines = Vector(rootLine(parent.value, Vector("e2e4", "e7e5")))),
        children = Vector(
          childPacket(
            parentBranchId = parent,
            startFen = initialFen,
            role = CandidateBranchRole.DefenderResource,
            parentLinePrefix = Vector("e2e4"),
            rawLines = Vector(childLine("defender-resource-1", parent, Vector("c7c5"), CandidateBranchRole.DefenderResource))
          )
        )
      )

    val root = evidence.find(_.role == CandidateBranchRole.RootCandidate).get
    val child = evidence.find(_.branchId.value == "defender-resource-1").get
    assertEquals(root.publicLineEligibleAt(nowEpochMs), true)
    assertEquals(child.publicLineEligibleAt(nowEpochMs), false)
    assertEquals(child.rejectionReasonsAt(nowEpochMs).contains(CandidateLineRejectionReason.StartFenMismatch), true)

  test("child packet cannot attach to the wrong existing parent branch"):
    val wrongParent = CandidateBranchId("root-candidate-2")
    val evidence =
      normalize(
        root = rootPacket(
          rawLines = Vector(
            rootLine("root-candidate-1", Vector("e2e4", "e7e5"), rank = 1, multiPvIndex = 1),
            rootLine(wrongParent.value, Vector("d2d4", "d7d5"), rank = 2, multiPvIndex = 2)
          ),
          multiPv = 2
        ),
        children = Vector(
          childPacket(
            parentBranchId = wrongParent,
            startFen = afterE4Fen,
            role = CandidateBranchRole.DefenderResource,
            parentLinePrefix = Vector("e2e4"),
            rawLines = Vector(childLine("defender-resource-1", wrongParent, Vector("c7c5"), CandidateBranchRole.DefenderResource))
          )
        )
      )

    val child = evidence.find(_.branchId.value == "defender-resource-1").get
    assertEquals(evidence.filter(_.role == CandidateBranchRole.RootCandidate).forall(_.publicLineEligibleAt(nowEpochMs)), true)
    assertEquals(child.publicLineEligibleAt(nowEpochMs), false)
    assertEquals(child.rejectionReasonsAt(nowEpochMs).contains(CandidateLineRejectionReason.StartFenMismatch), true)

  test("child packet with correct start FEN but wrong node or ply is rejected"):
    val parent = CandidateBranchId("root-candidate-1")
    val wrongNode =
      normalize(
        root = rootPacket(rawLines = Vector(rootLine(parent.value, Vector("e2e4", "e7e5")))),
        children = Vector(
          childPacket(
            parentBranchId = parent,
            startFen = afterE4Fen,
            nodeId = "other-node",
            role = CandidateBranchRole.DefenderResource,
            parentLinePrefix = Vector("e2e4"),
            rawLines = Vector(childLine("defender-resource-1", parent, Vector("c7c5"), CandidateBranchRole.DefenderResource))
          )
        )
      )
    val wrongPly =
      normalize(
        root = rootPacket(rawLines = Vector(rootLine(parent.value, Vector("e2e4", "e7e5")))),
        children = Vector(
          childPacket(
            parentBranchId = parent,
            startFen = afterE4Fen,
            ply = 2,
            role = CandidateBranchRole.DefenderResource,
            parentLinePrefix = Vector("e2e4"),
            rawLines = Vector(childLine("defender-resource-2", parent, Vector("c7c5"), CandidateBranchRole.DefenderResource))
          )
        )
      )

    val wrongNodeChild = wrongNode.find(_.branchId.value == "defender-resource-1").get
    val wrongPlyChild = wrongPly.find(_.branchId.value == "defender-resource-2").get
    assertEquals(wrongNodeChild.publicLineEligibleAt(nowEpochMs), false)
    assertEquals(wrongNodeChild.rejectionReasonsAt(nowEpochMs).contains(CandidateLineRejectionReason.InvalidNode), true)
    assertEquals(wrongPlyChild.publicLineEligibleAt(nowEpochMs), false)
    assertEquals(wrongPlyChild.rejectionReasonsAt(nowEpochMs).contains(CandidateLineRejectionReason.StartPlyMismatch), true)

  test("child packet with correct start FEN but shallow or stale evidence is rejected"):
    val parent = CandidateBranchId("root-candidate-1")
    val shallow =
      normalize(
        root = rootPacket(rawLines = Vector(rootLine(parent.value, Vector("e2e4", "e7e5")))),
        children = Vector(
          childPacket(
            parentBranchId = parent,
            startFen = afterE4Fen,
            role = CandidateBranchRole.DefenderResource,
            parentLinePrefix = Vector("e2e4"),
            realizedDepth = 15,
            rawLines = Vector(childLine("defender-resource-1", parent, Vector("c7c5"), CandidateBranchRole.DefenderResource))
          )
        )
      )
    val stale =
      normalize(
        root = rootPacket(rawLines = Vector(rootLine(parent.value, Vector("e2e4", "e7e5")))),
        children = Vector(
          childPacket(
            parentBranchId = parent,
            startFen = afterE4Fen,
            role = CandidateBranchRole.DefenderResource,
            parentLinePrefix = Vector("e2e4"),
            generatedAtEpochMs = 1_000L,
            maxAgeMs = 1_000L,
            rawLines = Vector(childLine("defender-resource-2", parent, Vector("c7c5"), CandidateBranchRole.DefenderResource))
          )
        )
      )

    val shallowChild = shallow.find(_.branchId.value == "defender-resource-1").get
    val staleChild = stale.find(_.branchId.value == "defender-resource-2").get
    assertEquals(shallow.exists(_.role == CandidateBranchRole.RootCandidate), true)
    assertEquals(stale.exists(_.role == CandidateBranchRole.RootCandidate), true)
    assertEquals(shallowChild.publicLineEligibleAt(nowEpochMs), false)
    assertEquals(shallowChild.rejectionReasonsAt(nowEpochMs).contains(CandidateLineRejectionReason.InsufficientDepth), true)
    assertEquals(staleChild.publicLineEligibleAt(nowEpochMs), false)
    assertEquals(staleChild.rejectionReasonsAt(nowEpochMs).contains(CandidateLineRejectionReason.Stale), true)

  test("source-hint root line cannot become proof-owned engine-root evidence"):
    val evidence =
      normalize(
        root = rootPacket(
          rawLines = Vector(
            rootLine(
              "root-candidate-1",
              Vector("e2e4", "e7e5"),
              provenance = CandidateLineProvenance(
                CandidateLineProvenanceKind.SourceHint,
                sourceHintRefs = Vector("opening-line-test:king-pawn:context")
              )
            )
          )
        )
      )

    assertEquals(evidence, Vector.empty)

  test("source-hint child line remains non-proof metadata and cannot become public-safe"):
    val parent = CandidateBranchId("root-candidate-1")
    val evidence =
      normalize(
        root = rootPacket(rawLines = Vector(rootLine(parent.value, Vector("e2e4", "e7e5")))),
        children = Vector(
          childPacket(
            parentBranchId = parent,
            startFen = afterE4Fen,
            role = CandidateBranchRole.DefenderResource,
            parentLinePrefix = Vector("e2e4"),
            rawLines = Vector(
              childLine(
                "defender-resource-1",
                parent,
                Vector("c7c5"),
                CandidateBranchRole.DefenderResource,
                provenance = CandidateLineProvenance(
                  CandidateLineProvenanceKind.SourceHint,
                  sourceHintRefs = Vector("retrieval-line-test:example:context")
                )
              )
            )
          )
        )
      )

    val child = evidence.find(_.branchId.value == "defender-resource-1").get
    assertEquals(child.provenance.kind, CandidateLineProvenanceKind.SourceHint)
    assertEquals(child.publicLineEligibleAt(nowEpochMs), false)
    assertEquals(child.rejectionReasonsAt(nowEpochMs).contains(CandidateLineRejectionReason.SourceHintOnly), true)

  test("invalid child packet does not discard valid root candidate evidence"):
    val parent = CandidateBranchId("root-candidate-1")
    val evidence =
      normalize(
        root = rootPacket(rawLines = Vector(rootLine(parent.value, Vector("e2e4", "e7e5")))),
        children = Vector(
          childPacket(
            parentBranchId = parent,
            startFen = afterE4Fen,
            role = CandidateBranchRole.DefenderResource,
            parentLinePrefix = Vector("e2e4"),
            rawLines = Vector(childLine("defender-resource-1", parent, Vector("not-uci"), CandidateBranchRole.DefenderResource))
          )
        )
      )

    val root = evidence.find(_.role == CandidateBranchRole.RootCandidate).get
    val child = evidence.find(_.branchId.value == "defender-resource-1").get
    assertEquals(root.publicLineEligibleAt(nowEpochMs), true)
    assertEquals(child.legalReplay, CandidateLineReplayStatus.Illegal)
    assertEquals(child.publicLineEligibleAt(nowEpochMs), false)
    assertEquals(child.rejectionReasonsAt(nowEpochMs).contains(CandidateLineRejectionReason.MalformedUci), true)

  test("root invalid discards all candidate-line evidence"):
    val evidence =
      normalize(
        root = rootPacket(
          startFen = "not a fen",
          rawLines = Vector(rootLine("root-candidate-1", Vector("e2e4")))
        ),
        children = Vector(
          childPacket(
            parentBranchId = CandidateBranchId("root-candidate-1"),
            startFen = afterE4Fen,
            role = CandidateBranchRole.DefenderResource,
            parentLinePrefix = Vector("e2e4"),
            rawLines = Vector(childLine("defender-resource-1", CandidateBranchId("root-candidate-1"), Vector("c7c5"), CandidateBranchRole.DefenderResource))
          )
        )
      )

    assertEquals(evidence, Vector.empty)

  test("shallow or stale root packet fails closed for the whole candidate-line tree"):
    val parent = CandidateBranchId("root-candidate-1")
    val child =
      childPacket(
        parentBranchId = parent,
        startFen = afterE4Fen,
        role = CandidateBranchRole.DefenderResource,
        parentLinePrefix = Vector("e2e4"),
        rawLines = Vector(childLine("defender-resource-1", parent, Vector("c7c5"), CandidateBranchRole.DefenderResource))
      )

    val shallow = normalize(root = rootPacket(rawLines = Vector(rootLine(parent.value, Vector("e2e4"))), realizedDepth = 15), children = Vector(child))
    val stale =
      normalize(
        root = rootPacket(rawLines = Vector(rootLine(parent.value, Vector("e2e4"))), generatedAtEpochMs = 1_000L, maxAgeMs = 1_000L),
        children = Vector(child)
      )

    assertEquals(shallow, Vector.empty)
    assertEquals(stale, Vector.empty)

  test("raw root and child packet details do not appear in public backend or render JSON"):
    val response =
      CommentaryBackendSeam
        .withClaimProvider(_ => Vector.empty, nowEpochMs = () => nowEpochMs)
        .renderDebug(
          CommentaryRequest(
            currentFen = initialFen,
            beforeFen = None,
            playedMove = None,
            nodeId = nodeId,
            ply = 0,
            enginePacket = Some(enginePacket(Vector(Vector("e2e4", "e7e5"), Vector("d2d4", "d7d5"))))
          )
        )
    val json = Json.toJson(response).toString

    assert(!json.contains("e2e4"), clues(json))
    assert(!json.contains("d2d4"), clues(json))
    assert(!json.contains("pvLines"), clues(json))
    assert(!json.contains("engineConfigFingerprint"), clues(json))
    assert(!json.contains("stockfish-test-depth18"), clues(json))
    assert(!json.contains("candidateLinePackets"), clues(json))

  test("candidate-line handoff-shaped request JSON fields are ignored and never serialized"):
    val rawRequest = Json.obj(
      "currentFen" -> initialFen,
      "nodeId" -> nodeId,
      "ply" -> 0,
      "candidateLinePackets" -> Json.obj(
        "root" -> Json.obj(
          "rawLines" -> Json.arr(Json.obj("uciLine" -> Json.arr("e2e4", "e7e5"))),
          "engineConfigFingerprint" -> "candidate-line-secret-engine"
        ),
        "children" -> Json.arr(
          Json.obj(
            "parentLinePrefix" -> Json.arr("e2e4"),
            "rawLines" -> Json.arr(Json.obj("uciLine" -> Json.arr("c7c5"))),
            "engineConfigFingerprint" -> "candidate-line-secret-child"
          )
        )
      )
    )

    val response =
      CommentaryBackendSeam
        .withClaimProvider(_ => Vector.empty, nowEpochMs = () => nowEpochMs)
        .renderDebug(rawRequest.as[CommentaryRequest])
    val json = Json.toJson(response).toString

    Vector(
      "candidateLinePackets",
      "parentLinePrefix",
      "rawLines",
      "engineConfigFingerprint",
      "candidate-line-secret-engine",
      "candidate-line-secret-child",
      "e2e4",
      "c7c5"
    ).foreach(token => assert(!json.contains(token), clues(json)))

  test("handoff emits no prepared variation evidence or public prose object"):
    val evidence =
      normalize(root = rootPacket(rawLines = Vector(rootLine("root-candidate-1", Vector("e2e4", "e7e5")))))

    assertEquals(evidence.map(_.preparedVariationTarget), Vector(None))
    assertEquals(evidence.exists(_.productElementNames.exists(_ == "proofId")), false)
    assertEquals(evidence.exists(_.productElementNames.exists(_ == "boundClaimId")), false)

  private def normalize(
      root: CandidateLinePacket,
      children: Vector[CandidateLineChildPacket] = Vector.empty
  ): Vector[CandidateLineEvidence] =
    CandidateLinePacketHandoff.normalize(
      CandidateLinePacketHandoffInput(
        currentFen = initialFen,
        nodeId = nodeId,
        ply = 0,
        root = root,
        children = children
      ),
      nowEpochMs = nowEpochMs
    )

  private def rootPacket(
      startFen: String = initialFen,
      nodeId: String = nodeId,
      ply: Int = 0,
      rawLines: Vector[CandidateLineRawLine] = Vector(rootLine("root-candidate-1", Vector("e2e4"))),
      requestedDepth: Int = 18,
      realizedDepth: Int = 18,
      multiPv: Int = 1,
      generatedAtEpochMs: Long = 10_000L,
      maxAgeMs: Long = 20_000L
  ): CandidateLinePacket =
    CandidateLinePacket(
      startFen = startFen,
      nodeId = nodeId,
      ply = ply,
      rawLines = rawLines,
      requestedDepth = requestedDepth,
      realizedDepth = realizedDepth,
      multiPv = multiPv,
      generatedAtEpochMs = generatedAtEpochMs,
      maxAgeMs = maxAgeMs,
      engineConfigFingerprint = "stockfish-test-depth18",
      requireDistinctRootFirstMoves = true
    )

  private def childPacket(
      parentBranchId: CandidateBranchId,
      startFen: String,
      role: CandidateBranchRole,
      parentLinePrefix: Vector[String],
      rawLines: Vector[CandidateLineRawLine],
      nodeId: String = nodeId,
      ply: Int = 1,
      requestedDepth: Int = 18,
      realizedDepth: Int = 18,
      multiPv: Int = 1,
      generatedAtEpochMs: Long = 10_000L,
      maxAgeMs: Long = 20_000L
  ): CandidateLineChildPacket =
    CandidateLineChildPacket(
      parentBranchId = parentBranchId,
      startFen = startFen,
      nodeId = nodeId,
      ply = ply,
      role = role,
      parentLinePrefix = parentLinePrefix,
      rawLines = rawLines,
      requestedDepth = requestedDepth,
      realizedDepth = realizedDepth,
      multiPv = multiPv,
      generatedAtEpochMs = generatedAtEpochMs,
      maxAgeMs = maxAgeMs,
      engineConfigFingerprint = "stockfish-test-depth18"
    )

  private def rootLine(
      branchId: String,
      uciLine: Vector[String],
      rank: Int = 1,
      multiPvIndex: Int = 1,
      provenance: CandidateLineProvenance = CandidateLineProvenance(CandidateLineProvenanceKind.EngineRoot)
  ): CandidateLineRawLine =
    CandidateLineRawLine(
      branchId = CandidateBranchId(branchId),
      parentBranchId = None,
      role = CandidateBranchRole.RootCandidate,
      rank = rank,
      multiPvIndex = multiPvIndex,
      uciLine = uciLine,
      provenance = provenance
    )

  private def childLine(
      branchId: String,
      parentBranchId: CandidateBranchId,
      uciLine: Vector[String],
      role: CandidateBranchRole,
      rank: Int = 1,
      multiPvIndex: Int = 1,
      provenance: CandidateLineProvenance = CandidateLineProvenance(CandidateLineProvenanceKind.EngineChild)
  ): CandidateLineRawLine =
    CandidateLineRawLine(
      branchId = CandidateBranchId(branchId),
      parentBranchId = Some(parentBranchId),
      role = role,
      rank = rank,
      multiPvIndex = multiPvIndex,
      uciLine = uciLine,
      provenance = provenance
    )

  private def enginePacket(pvLines: Vector[Vector[String]]): CertificationEngineRuntimeIntake.RuntimeEnginePacket =
    CertificationEngineRuntimeIntake.RuntimeEnginePacket(
      fen = initialFen,
      nodeId = nodeId,
      ply = 0,
      requestedDepth = 18,
      realizedDepth = 18,
      multiPv = pvLines.size.max(1),
      completed = true,
      generatedAtEpochMs = 10_000L,
      maxAgeMs = 20_000L,
      engineConfigFingerprint = "stockfish-test-depth18",
      score = CertificationEngineRuntimeIntake.RuntimeScore.Centipawns(20),
      scorePerspective = CertificationEngineRuntimeIntake.RuntimeScorePerspective.White,
      pvLines = pvLines,
      claims = Vector.empty
    )
