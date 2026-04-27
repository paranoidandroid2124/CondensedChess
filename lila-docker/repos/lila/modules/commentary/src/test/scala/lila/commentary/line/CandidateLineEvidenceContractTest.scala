package lila.commentary.line

import lila.commentary.selection.{ VariationSurfaceAllowance, WordingStrength }

class CandidateLineEvidenceContractTest extends munit.FunSuite:

  private val startFen = "r1bqkbnr/pppp1ppp/2n5/4p3/3PP3/2N2N2/PPP2PPP/R1BQKB1R b KQkq - 3 3"
  private val nowEpochMs = 20_000L

  test("root MultiPV candidates preserve branch identity rank and MultiPV index"):
    val first = validLine(
      branchId = "root-candidate-1",
      rank = 1,
      multiPvIndex = 1,
      lineSan = Vector("Nf6", "Bb5"),
      lineUci = Vector("g8f6", "f1b5")
    )
    val second = validLine(
      branchId = "root-candidate-2",
      rank = 2,
      multiPvIndex = 2,
      lineSan = Vector("a6", "Ba4"),
      lineUci = Vector("a7a6", "b5a4")
    )

    assertEquals(first.branchId.value, "root-candidate-1")
    assertEquals(second.branchId.value, "root-candidate-2")
    assertEquals(first.rank, 1)
    assertEquals(second.rank, 2)
    assertEquals(first.search.multiPvIndex, 1)
    assertEquals(second.search.multiPvIndex, 2)
    assertEquals(Vector(first, second).forall(_.publicLineEligibleAt(nowEpochMs)), true)

  test("child defender branch can reference a parent root candidate"):
    val root = validLine(branchId = "root-candidate-1")
    val defender = validLine(
      branchId = "defender-resource-1",
      parentBranchId = Some(root.branchId),
      role = CandidateBranchRole.DefenderResource,
      rank = 1,
      provenance = CandidateLineProvenance(CandidateLineProvenanceKind.EngineChild),
      lineSan = Vector("a6", "Ba4", "Nf6"),
      lineUci = Vector("a7a6", "b5a4", "g8f6")
    )

    assertEquals(defender.parentBranchId.map(_.value), Some("root-candidate-1"))
    assertEquals(defender.role, CandidateBranchRole.DefenderResource)
    assertEquals(defender.provenance.kind, CandidateLineProvenanceKind.EngineChild)
    assertEquals(defender.publicLineEligibleAt(nowEpochMs), true)

  test("two defender replies can be represented under the same candidate"):
    val parent = CandidateBranchId("root-candidate-1")
    val replies =
      Vector(
        validLine(
          branchId = "defender-reply-1",
          parentBranchId = Some(parent),
          role = CandidateBranchRole.DefenderReply,
          rank = 1,
          lineSan = Vector("a6", "Ba4"),
          lineUci = Vector("a7a6", "b5a4")
        ),
        validLine(
          branchId = "defender-reply-2",
          parentBranchId = Some(parent),
          role = CandidateBranchRole.DefenderReply,
          rank = 2,
          lineSan = Vector("Nf6", "O-O"),
          lineUci = Vector("g8f6", "e1g1")
        )
      )

    assertEquals(replies.map(_.parentBranchId), Vector(Some(parent), Some(parent)))
    assertEquals(replies.map(_.role), Vector(CandidateBranchRole.DefenderReply, CandidateBranchRole.DefenderReply))
    assertEquals(replies.map(_.rank), Vector(1, 2))
    assertEquals(replies.forall(_.publicLineEligibleAt(nowEpochMs)), true)

  test("SAN and UCI lines are both required for admitted line evidence"):
    val missingSan = validLine(lineSan = Vector.empty)
    val missingUci = validLine(lineUci = Vector.empty)
    val mismatched = validLine(lineSan = Vector("Nf6"), lineUci = Vector("g8f6", "f1b5"))

    assertEquals(missingSan.publicLineEligibleAt(nowEpochMs), false)
    assertEquals(missingSan.rejectionReasonsAt(nowEpochMs).contains(CandidateLineRejectionReason.MissingSan), true)
    assertEquals(missingUci.publicLineEligibleAt(nowEpochMs), false)
    assertEquals(missingUci.rejectionReasonsAt(nowEpochMs).contains(CandidateLineRejectionReason.MissingUci), true)
    assertEquals(mismatched.publicLineEligibleAt(nowEpochMs), false)
    assertEquals(mismatched.rejectionReasonsAt(nowEpochMs).contains(CandidateLineRejectionReason.LineLengthMismatch), true)

  test("depth floor and freshness fail closed"):
    val depth16 = validLine(realizedDepth = 16)
    val depth15 = validLine(realizedDepth = 15)
    val stale = validLine(timing = CandidateLineTiming(generatedAtEpochMs = 1_000L, maxAgeMs = 1_000L))

    assertEquals(depth16.publicLineEligibleAt(nowEpochMs), true)
    assertEquals(depth15.publicLineEligibleAt(nowEpochMs), false)
    assertEquals(depth15.rejectionReasonsAt(nowEpochMs).contains(CandidateLineRejectionReason.InsufficientDepth), true)
    assertEquals(stale.publicLineEligibleAt(nowEpochMs), false)
    assertEquals(stale.rejectionReasonsAt(nowEpochMs).contains(CandidateLineRejectionReason.Stale), true)

  test("illegal replay cannot become public-safe line evidence"):
    val illegal = validLine(legalReplay = CandidateLineReplayStatus.Illegal)

    assertEquals(illegal.publicLineEligibleAt(nowEpochMs), false)
    assertEquals(illegal.rejectionReasonsAt(nowEpochMs).contains(CandidateLineRejectionReason.IllegalReplay), true)

  test("explicit failure reasons are preserved for rejected line evidence"):
    val rejected =
      validLine(failureReasons = Vector(CandidateLineRejectionReason.RetrievalNonAuthoritative))

    assertEquals(rejected.publicLineEligibleAt(nowEpochMs), false)
    assertEquals(
      rejected.rejectionReasonsAt(nowEpochMs).contains(CandidateLineRejectionReason.RetrievalNonAuthoritative),
      true
    )

  test("source hints remain non-proof metadata and retrieval is not a current-position candidate source"):
    val sourceHint = validLine(
      provenance = CandidateLineProvenance(
        CandidateLineProvenanceKind.SourceHint,
        sourceHintRefs = Vector("opening-line-test:catalan-main:context")
      )
    )

    assertEquals(sourceHint.provenance.proofOwnerAllowed, false)
    assertEquals(sourceHint.provenance.candidateSourceAllowed, true)
    assertEquals(sourceHint.publicLineEligibleAt(nowEpochMs), false)
    assertEquals(sourceHint.rejectionReasonsAt(nowEpochMs).contains(CandidateLineRejectionReason.SourceHintOnly), true)
    assertEquals(CandidateLineProvenanceKind.fromKey("retrieval"), None)
    assertEquals(CandidateLineProvenanceKind.fromKey("retrieval_illustration"), None)

  test("failed and premature candidate roles stay internal and cannot become lead recommendations"):
    val failed = validLine(role = CandidateBranchRole.FailedCandidate)
    val premature = validLine(role = CandidateBranchRole.Premature)
    val releaseRisk = validLine(role = CandidateBranchRole.ReleaseRisk)

    Vector(failed, premature, releaseRisk).foreach: line =>
      assertEquals(line.publicLineEligibleAt(nowEpochMs), false)
      assertEquals(line.leadRecommendationEligibleAt(nowEpochMs), false)
      assertEquals(line.rejectionReasonsAt(nowEpochMs).contains(CandidateLineRejectionReason.InternalOnlyRole), true)

  test("prepared variation target link is not a public raw engine packet shape"):
    val target = CandidateLinePreparedTarget(proofId = "line-proof-safe", boundClaimId = "claim-safe")
    val line = validLine(preparedVariationTarget = Some(target))
    val targetFields = target.productElementNames.toVector
    val lineFields = line.productElementNames.toVector

    assertEquals(line.preparedVariationTarget, Some(target))
    assertEquals(targetFields, Vector("proofId", "boundClaimId"))
    assertEquals(lineFields.exists(field => field == "rawPacketId" || field == "rawPv" || field == "pvLines" || field == "score"), false)

  private def validLine(
      branchId: String = "root-candidate-1",
      parentBranchId: Option[CandidateBranchId] = None,
      role: CandidateBranchRole = CandidateBranchRole.RootCandidate,
      rank: Int = 1,
      multiPvIndex: Int = 1,
      multiPv: Int = 3,
      realizedDepth: Int = 18,
      timing: CandidateLineTiming = CandidateLineTiming(generatedAtEpochMs = 10_000L, maxAgeMs = 20_000L),
      provenance: CandidateLineProvenance = CandidateLineProvenance(CandidateLineProvenanceKind.EngineRoot),
      legalReplay: CandidateLineReplayStatus = CandidateLineReplayStatus.Legal,
      lineSan: Vector[String] = Vector("Nf6", "Bb5"),
      lineUci: Vector[String] = Vector("g8f6", "f1b5"),
      failureReasons: Vector[CandidateLineRejectionReason] = Vector.empty,
      preparedVariationTarget: Option[CandidateLinePreparedTarget] = None
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
      timing = timing,
      engineConfigFingerprint = "stockfish-16-depth18-multipv3",
      legalReplay = legalReplay,
      provenance = provenance,
      surfaceAllowance = VariationSurfaceAllowance.PublicLine,
      wordingCap = WordingStrength.QualifiedSupport,
      failureReasons = failureReasons,
      preparedVariationTarget = preparedVariationTarget
    )
