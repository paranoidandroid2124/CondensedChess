package lila.commentary.line

import play.api.libs.json.Json

import lila.commentary.api.{ CommentaryBackendSeam, CommentaryRequest }
import lila.commentary.api.CommentaryApiJson.given
import lila.commentary.certification.CertificationEngineRuntimeIntake

class CandidateLineNormalizerContractTest extends munit.FunSuite:

  private val initialFen = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1"
  private val afterE4Fen = "rnbqkbnr/pppppppp/8/8/4P3/8/PPPP1PPP/RNBQKBNR b KQkq - 0 1"
  private val nowEpochMs = 20_000L

  test("legal UCI PV from exact start FEN becomes matching SAN line"):
    val line = normalize(Vector(rawLine("root-candidate-1", Vector("e2e4", "e7e5", "g1f3")))).head

    assertEquals(line.lineUci, Vector("e2e4", "e7e5", "g1f3"))
    assertEquals(line.lineSan, Vector("e4", "e5", "Nf3"))
    assertEquals(line.legalReplay, CandidateLineReplayStatus.Legal)
    assertEquals(line.publicLineEligibleAt(nowEpochMs), true)

  test("depth 16 is accepted and depth 15 is non-public"):
    val depth16 = normalize(Vector(rawLine("root-candidate-1", Vector("e2e4"))), realizedDepth = 16).head
    val depth15 = normalize(Vector(rawLine("root-candidate-1", Vector("e2e4"))), realizedDepth = 15).head

    assertEquals(depth16.publicLineEligibleAt(nowEpochMs), true)
    assertEquals(depth15.publicLineEligibleAt(nowEpochMs), false)
    assertEquals(depth15.rejectionReasonsAt(nowEpochMs).contains(CandidateLineRejectionReason.InsufficientDepth), true)

  test("stale generatedAt and maxAge are non-public"):
    val stale =
      normalize(
        Vector(rawLine("root-candidate-1", Vector("e2e4"))),
        generatedAtEpochMs = 1_000L,
        maxAgeMs = 1_000L
      ).head

    assertEquals(stale.publicLineEligibleAt(nowEpochMs), false)
    assertEquals(stale.rejectionReasonsAt(nowEpochMs).contains(CandidateLineRejectionReason.Stale), true)

  test("malformed UCI is rejected with an explicit failure reason"):
    val malformed = normalize(Vector(rawLine("root-candidate-1", Vector("not-uci")))).head

    assertEquals(malformed.legalReplay, CandidateLineReplayStatus.Illegal)
    assertEquals(malformed.publicLineEligibleAt(nowEpochMs), false)
    assertEquals(malformed.rejectionReasonsAt(nowEpochMs).contains(CandidateLineRejectionReason.MalformedUci), true)
    assertEquals(malformed.lineSan, Vector.empty)
    assertEquals(malformed.lineUci, Vector.empty)

  test("illegal UCI sequence is rejected and keeps only the legal prefix"):
    val illegal = normalize(Vector(rawLine("root-candidate-1", Vector("e2e4", "e2e5")))).head

    assertEquals(illegal.legalReplay, CandidateLineReplayStatus.Illegal)
    assertEquals(illegal.publicLineEligibleAt(nowEpochMs), false)
    assertEquals(illegal.rejectionReasonsAt(nowEpochMs).contains(CandidateLineRejectionReason.IllegalReplay), true)
    assertEquals(illegal.lineSan, Vector("e4"))
    assertEquals(illegal.lineUci, Vector("e2e4"))

  test("wrong start FEN is rejected fail-closed"):
    val invalidFen =
      normalize(
        Vector(rawLine("root-candidate-1", Vector("e2e4"))),
        startFen = "not a fen"
      ).head

    assertEquals(invalidFen.legalReplay, CandidateLineReplayStatus.Illegal)
    assertEquals(invalidFen.publicLineEligibleAt(nowEpochMs), false)
    assertEquals(invalidFen.rejectionReasonsAt(nowEpochMs).contains(CandidateLineRejectionReason.InvalidFen), true)
    assertEquals(invalidFen.lineSan, Vector.empty)
    assertEquals(invalidFen.lineUci, Vector.empty)

  test("root MultiPV 2 preserves two candidates with distinct first moves"):
    val lines =
      normalize(
        Vector(
          rawLine("root-candidate-1", Vector("e2e4", "e7e5"), rank = 1, multiPvIndex = 1),
          rawLine("root-candidate-2", Vector("d2d4", "d7d5"), rank = 2, multiPvIndex = 2)
        ),
        multiPv = 2
      )

    assertEquals(lines.map(_.branchId.value), Vector("root-candidate-1", "root-candidate-2"))
    assertEquals(lines.map(_.rank), Vector(1, 2))
    assertEquals(lines.map(_.search.multiPvIndex), Vector(1, 2))
    assertEquals(lines.map(_.lineSan), Vector(Vector("e4", "e5"), Vector("d4", "d5")))
    assertEquals(lines.forall(_.publicLineEligibleAt(nowEpochMs)), true)

  test("duplicate root first move is rejected when branch diversity is required"):
    val lines =
      normalize(
        Vector(
          rawLine("root-candidate-1", Vector("e2e4", "e7e5"), rank = 1, multiPvIndex = 1),
          rawLine("root-candidate-2", Vector("e2e4", "c7c5"), rank = 2, multiPvIndex = 2)
        ),
        multiPv = 2
      )

    assertEquals(lines.size, 2)
    assertEquals(lines.forall(!_.publicLineEligibleAt(nowEpochMs)), true)
    assertEquals(lines.forall(_.rejectionReasonsAt(nowEpochMs).contains(CandidateLineRejectionReason.DuplicateRootFirstMove)), true)

  test("MultiPV mismatch is rejected when root candidates claim multiple branches"):
    val line =
      normalize(
        Vector(rawLine("root-candidate-1", Vector("e2e4"), rank = 1, multiPvIndex = 1)),
        multiPv = 2
      ).head

    assertEquals(line.publicLineEligibleAt(nowEpochMs), false)
    assertEquals(line.rejectionReasonsAt(nowEpochMs).contains(CandidateLineRejectionReason.InvalidMultiPv), true)

  test("child defender branch normalizes from child start FEN and keeps parent branch id"):
    val parent = CandidateBranchId("root-candidate-1")
    val child =
      normalize(
        Vector(
          rawLine(
            "defender-reply-1",
            Vector("e7e5", "g1f3"),
            parentBranchId = Some(parent),
            role = CandidateBranchRole.DefenderReply,
            provenance = CandidateLineProvenance(CandidateLineProvenanceKind.EngineChild)
          )
        ),
        startFen = afterE4Fen,
        ply = 1,
        multiPv = 1
      ).head

    assertEquals(child.parentBranchId, Some(parent))
    assertEquals(child.role, CandidateBranchRole.DefenderReply)
    assertEquals(child.provenance.kind, CandidateLineProvenanceKind.EngineChild)
    assertEquals(child.sideToMove, CandidateLineSide.Black)
    assertEquals(child.lineSan, Vector("e5", "Nf3"))
    assertEquals(child.publicLineEligibleAt(nowEpochMs), true)

  test("source hint metadata does not make the line proof-owned by source"):
    val sourceHint =
      normalize(
        Vector(
          rawLine(
            "source-hint-1",
            Vector("e2e4"),
            provenance = CandidateLineProvenance(
              CandidateLineProvenanceKind.SourceHint,
              sourceHintRefs = Vector("opening-line-test:king-pawn:context")
            )
          )
        )
      ).head

    assertEquals(sourceHint.lineSan, Vector("e4"))
    assertEquals(sourceHint.provenance.proofOwnerAllowed, false)
    assertEquals(sourceHint.publicLineEligibleAt(nowEpochMs), false)
    assertEquals(sourceHint.rejectionReasonsAt(nowEpochMs).contains(CandidateLineRejectionReason.SourceHintOnly), true)

  test("raw engine packet PV does not appear in public render or backend JSON"):
    val response =
      CommentaryBackendSeam
        .withClaimProvider(_ => Vector.empty, nowEpochMs = () => nowEpochMs)
        .renderDebug(
          CommentaryRequest(
            currentFen = initialFen,
            beforeFen = None,
            playedMove = None,
            nodeId = "mainline:0",
            ply = 0,
            enginePacket = Some(enginePacket(Vector(Vector("e2e4", "e7e5"))))
          )
        )
    val json = Json.toJson(response).toString

    assert(!json.contains("e2e4"), clues(json))
    assert(!json.contains("e7e5"), clues(json))
    assert(!json.contains("pvLines"), clues(json))

  private def normalize(
      rawLines: Vector[CandidateLineRawLine],
      startFen: String = initialFen,
      nodeId: String = "mainline:0",
      ply: Int = 0,
      requestedDepth: Int = 18,
      realizedDepth: Int = 18,
      multiPv: Int = 1,
      generatedAtEpochMs: Long = 10_000L,
      maxAgeMs: Long = 20_000L
  ): Vector[CandidateLineEvidence] =
    CandidateLineNormalizer.normalize(
      CandidateLineNormalizationInput(
        startFen = startFen,
        startNodeId = nodeId,
        startPly = ply,
        rawLines = rawLines,
        requestedDepth = requestedDepth,
        realizedDepth = realizedDepth,
        multiPv = multiPv,
        generatedAtEpochMs = generatedAtEpochMs,
        maxAgeMs = maxAgeMs,
        engineConfigFingerprint = "stockfish-test-depth18",
        requireDistinctRootFirstMoves = true
      ),
      nowEpochMs = nowEpochMs
    )

  private def rawLine(
      branchId: String,
      uciLine: Vector[String],
      parentBranchId: Option[CandidateBranchId] = None,
      role: CandidateBranchRole = CandidateBranchRole.RootCandidate,
      rank: Int = 1,
      multiPvIndex: Int = 1,
      provenance: CandidateLineProvenance = CandidateLineProvenance(CandidateLineProvenanceKind.EngineRoot)
  ): CandidateLineRawLine =
    CandidateLineRawLine(
      branchId = CandidateBranchId(branchId),
      parentBranchId = parentBranchId,
      role = role,
      rank = rank,
      multiPvIndex = multiPvIndex,
      uciLine = uciLine,
      provenance = provenance
    )

  private def enginePacket(pvLines: Vector[Vector[String]]): CertificationEngineRuntimeIntake.RuntimeEnginePacket =
    CertificationEngineRuntimeIntake.RuntimeEnginePacket(
      fen = initialFen,
      nodeId = "mainline:0",
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
