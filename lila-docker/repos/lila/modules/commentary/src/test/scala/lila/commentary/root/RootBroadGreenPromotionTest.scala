package lila.commentary.root

import chess.format.Fen
import chess.variant

import lila.commentary.root.RootAtomRegistry.{ SchemaFamily, SchemaId, canonicalFiles, canonicalSquares }

class RootBroadGreenPromotionTest extends munit.FunSuite:

  private val rows = RootExpectationCorpus.loadAll()

  test("piece_on reaches broad-confidence-green only with the matrix-owned role, topology, and polarity bucket spread"):
    val schemaId = SchemaId.PieceOn
    val policy = RootCoverageMatrix.policyFor(schemaId)
    val schemaRows = rows.filter(_.schema == schemaId)
    val bucketSet = schemaRows.map(pieceOnBucketKey).toSet
    val requiredBuckets = RootCoverageMatrix.greenProofBucketsFor(schemaId).toSet

    assertEquals(policy.status, "broad-confidence-green")
    assert(schemaRows.size >= policy.minimumCorpusFloor, s"$schemaId rows = ${schemaRows.size}, floor = ${policy.minimumCorpusFloor}")
    assertEquals(schemaRows.map(_.caseType).toSet, Set("exact"))
    assertEquals(bucketSet, requiredBuckets)

  test("controlled_by reaches broad-confidence-green only with attacker-family, source-topology, polarity, and occupancy buckets"):
    val schemaId = SchemaId.ControlledBy
    val policy = RootCoverageMatrix.policyFor(schemaId)
    val schemaRows = rows.filter(_.schema == schemaId)
    val bucketSet = schemaRows.map(controlledByBucketKey).toSet
    val requiredBuckets = RootCoverageMatrix.greenProofBucketsFor(schemaId).toSet

    assertEquals(policy.status, "broad-confidence-green")
    assertEquals(schemaRows.size, policy.minimumCorpusFloor, s"$schemaId rows must stay frozen to its exact broad-green inventory")
    assertEquals(schemaRows.map(_.caseType).toSet, Set("exact"))
    assertEquals(bucketSet, requiredBuckets)
    schemaRows.foreach(assertStoredSnapshotMatches)

  test("open_file reaches broad-confidence-green only with the matrix-owned context, topology, and adjacency bucket spread"):
    val schemaId = SchemaId.OpenFile
    val policy = RootCoverageMatrix.policyFor(schemaId)
    val schemaRows = rows.filter(_.schema == schemaId)
    val bucketSet = schemaRows.map(openFileBucketKey).toSet
    val requiredBuckets = RootCoverageMatrix.greenProofBucketsFor(schemaId).toSet

    assertEquals(policy.status, "broad-confidence-green")
    assert(schemaRows.size >= policy.minimumCorpusFloor, s"$schemaId rows = ${schemaRows.size}, floor = ${policy.minimumCorpusFloor}")
    assertEquals(schemaRows.map(_.caseType).toSet, Set("exact"))
    assertEquals(bucketSet, requiredBuckets)

  test("half_open_file reaches broad-confidence-green only with the matrix-owned owner, topology, rival-pawn, and fail-closed bucket spread"):
    val schemaId = SchemaId.HalfOpenFile
    val policy = RootCoverageMatrix.policyFor(schemaId)
    val schemaRows = rows.filter(_.schema == schemaId)
    val bucketSet = schemaRows.map(halfOpenFileBucketKey).toSet
    val requiredBuckets = RootCoverageMatrix.greenProofBucketsFor(schemaId).toSet

    assertEquals(policy.status, "broad-confidence-green")
    assert(schemaRows.size >= policy.minimumCorpusFloor, s"$schemaId rows = ${schemaRows.size}, floor = ${policy.minimumCorpusFloor}")
    assertEquals(schemaRows.map(_.caseType).toSet, Set("exact", "near_miss"))
    assertEquals(bucketSet, requiredBuckets)

  test("contested reaches broad-confidence-green only with carrier, topology, occupancy, and disjoint fail-closed buckets"):
    val schemaId = SchemaId.Contested
    val policy = RootCoverageMatrix.policyFor(schemaId)
    val schemaRows = rows.filter(_.schema == schemaId)
    val bucketSet = schemaRows.map(contestedBucketKey).toSet
    val requiredBuckets = RootCoverageMatrix.greenProofBucketsFor(schemaId).toSet

    assertEquals(policy.status, "broad-confidence-green")
    assertEquals(schemaRows.size, policy.minimumCorpusFloor, s"$schemaId rows must stay frozen to its exact broad-green inventory")
    assertEquals(schemaRows.map(_.caseType).toSet, Set("exact", "near_miss"))
    assertEquals(bucketSet, requiredBuckets)

  test("king_ring_square reaches broad-confidence-green only with the matrix-owned polarity, king-file, and adjacency bucket spread"):
    val schemaId = SchemaId.KingRingSquare
    val policy = RootCoverageMatrix.policyFor(schemaId)
    val schemaRows = rows.filter(_.schema == schemaId)
    val bucketSet = schemaRows.flatMap(kingRingBucketKeys).toSet
    val requiredBuckets = RootCoverageMatrix.greenProofBucketsFor(schemaId).toSet

    assertEquals(policy.status, "broad-confidence-green")
    assertEquals(schemaRows.size, policy.minimumCorpusFloor, s"$schemaId rows must stay frozen to its exact broad-green inventory")
    assertEquals(schemaRows.map(_.caseType).toSet, Set("exact"))
    assertEquals(bucketSet, requiredBuckets)
    schemaRows.foreach(assertStoredSnapshotMatches)

  test("weak_square reaches broad-confidence-green only with topology, occupancy, material-regime, and challenge-restored buckets"):
    val schemaId = SchemaId.WeakSquare
    val policy = RootCoverageMatrix.policyFor(schemaId)
    val schemaRows = rows.filter(_.schema == schemaId)
    val bucketSet = schemaRows.flatMap(weakSquareBucketKeys).toSet
    val requiredBuckets = RootCoverageMatrix.greenProofBucketsFor(schemaId).toSet

    assertEquals(policy.status, "broad-confidence-green")
    assertEquals(schemaRows.size, policy.minimumCorpusFloor, s"$schemaId rows must stay frozen to its exact broad-green inventory")
    assertEquals(schemaRows.map(_.caseType).toSet, Set("exact", "near_miss"))
    assertEquals(bucketSet, requiredBuckets)

  test("pawn_controlled_by reaches broad-confidence-green only with polarity, topology, and near-miss bucket spread"):
    val schemaId = SchemaId.PawnControlledBy
    val policy = RootCoverageMatrix.policyFor(schemaId)
    val schemaRows = rows.filter(_.schema == schemaId)
    val bucketSet = schemaRows.flatMap(pawnControlledByBucketKeys).toSet
    val requiredBuckets = RootCoverageMatrix.greenProofBucketsFor(schemaId).toSet

    assertEquals(policy.status, "broad-confidence-green")
    assert(schemaRows.size >= policy.minimumCorpusFloor, s"$schemaId rows = ${schemaRows.size}, floor = ${policy.minimumCorpusFloor}")
    assertEquals(schemaRows.map(_.caseType).toSet, Set("exact", "near_miss"))
    assertEquals(bucketSet, requiredBuckets)

  test("isolated_pawn reaches broad-confidence-green only with topology, rank-stage, polarity, and neighbor-restored buckets"):
    val schemaId = SchemaId.IsolatedPawn
    val policy = RootCoverageMatrix.policyFor(schemaId)
    val schemaRows = rows.filter(_.schema == schemaId)
    val bucketSet = schemaRows.map(isolatedPawnBucketKey).toSet
    val requiredBuckets = RootCoverageMatrix.greenProofBucketsFor(schemaId).toSet

    assertEquals(policy.status, "broad-confidence-green")
    assert(schemaRows.size >= policy.minimumCorpusFloor, s"$schemaId rows = ${schemaRows.size}, floor = ${policy.minimumCorpusFloor}")
    assertEquals(schemaRows.map(_.caseType).toSet, Set("exact", "near_miss"))
    assertEquals(bucketSet, requiredBuckets)

  test("doubled_file reaches broad-confidence-green only with topology, stack-shape, polarity, and fail-closed buckets"):
    val schemaId = SchemaId.DoubledFile
    val policy = RootCoverageMatrix.policyFor(schemaId)
    val schemaRows = rows.filter(_.schema == schemaId)
    val bucketSet = schemaRows.map(doubledFileBucketKey).toSet
    val requiredBuckets = RootCoverageMatrix.greenProofBucketsFor(schemaId).toSet

    assertEquals(policy.status, "broad-confidence-green")
    assert(schemaRows.size >= policy.minimumCorpusFloor, s"$schemaId rows = ${schemaRows.size}, floor = ${policy.minimumCorpusFloor}")
    assertEquals(schemaRows.map(_.caseType).toSet, Set("exact", "near_miss"))
    assertEquals(bucketSet, requiredBuckets)

  test("fixed_pawn reaches broad-confidence-green only with polarity, topology, and chain-shape buckets"):
    val schemaId = SchemaId.FixedPawn
    val policy = RootCoverageMatrix.policyFor(schemaId)
    val schemaRows = rows.filter(_.schema == schemaId)
    val bucketSet = schemaRows.flatMap(fixedPawnBucketKeys).toSet
    val requiredBuckets = RootCoverageMatrix.greenProofBucketsFor(schemaId).toSet

    assertEquals(policy.status, "broad-confidence-green")
    assert(schemaRows.size >= policy.minimumCorpusFloor, s"$schemaId rows = ${schemaRows.size}, floor = ${policy.minimumCorpusFloor}")
    assertEquals(schemaRows.map(_.caseType).toSet, Set("exact"))
    assertEquals(bucketSet, requiredBuckets)

  test("lever_available reaches broad-confidence-green only with push-type, topology, contact-direction, and blocked-push buckets"):
    val schemaId = SchemaId.LeverAvailable
    val policy = RootCoverageMatrix.policyFor(schemaId)
    val schemaRows = rows.filter(_.schema == schemaId)
    val bucketSet = schemaRows.map(leverAvailableBucketKey).toSet
    val requiredBuckets = RootCoverageMatrix.greenProofBucketsFor(schemaId).toSet

    assertEquals(policy.status, "broad-confidence-green")
    assert(schemaRows.size >= policy.minimumCorpusFloor, s"$schemaId rows = ${schemaRows.size}, floor = ${policy.minimumCorpusFloor}")
    assertEquals(schemaRows.map(_.caseType).toSet, Set("exact", "near_miss"))
    assertEquals(bucketSet, requiredBuckets)

  test("en_passant_state reaches broad-confidence-green only with the matrix-owned exact-state bucket spread"):
    val schemaId = SchemaId.EnPassantState
    val policy = RootCoverageMatrix.policyFor(schemaId)
    val schemaRows = rows.filter(_.schema == schemaId)
    val bucketSet = schemaRows.map(enPassantBucketKey).toSet
    val requiredBuckets = RootCoverageMatrix.greenProofBucketsFor(schemaId).toSet

    assertEquals(policy.status, "broad-confidence-green")
    assert(schemaRows.size >= policy.minimumCorpusFloor, s"$schemaId rows = ${schemaRows.size}, floor = ${policy.minimumCorpusFloor}")
    assertEquals(schemaRows.map(_.caseType).toSet, Set("exact"))
    assertEquals(bucketSet, requiredBuckets)

  test("side_to_move reaches broad-confidence-green only with the matrix-owned state and board-context bucket spread"):
    val schemaId = SchemaId.SideToMove
    val policy = RootCoverageMatrix.policyFor(schemaId)
    val schemaRows = rows.filter(_.schema == schemaId)
    val bucketSet = schemaRows.map(sideToMoveBucketKey).toSet
    val requiredBuckets = RootCoverageMatrix.greenProofBucketsFor(schemaId).toSet

    assertEquals(policy.status, "broad-confidence-green")
    assert(schemaRows.size >= policy.minimumCorpusFloor, s"$schemaId rows = ${schemaRows.size}, floor = ${policy.minimumCorpusFloor}")
    assertEquals(schemaRows.map(_.caseType).toSet, Set("exact"))
    assertEquals(bucketSet, requiredBuckets)

  test("castling_rights reaches broad-confidence-green only with legal state buckets and fail-closed invalid rights"):
    val schemaId = SchemaId.CastlingRights
    val policy = RootCoverageMatrix.policyFor(schemaId)
    val schemaRows = rows.filter(_.schema == schemaId)
    val bucketSet = schemaRows.map(castlingRightsBucketKey).toSet
    val requiredBuckets = RootCoverageMatrix.greenProofBucketsFor(schemaId).toSet

    assertEquals(policy.status, "broad-confidence-green")
    assert(schemaRows.size >= policy.minimumCorpusFloor, s"$schemaId rows = ${schemaRows.size}, floor = ${policy.minimumCorpusFloor}")
    assertEquals(schemaRows.map(_.caseType).toSet, Set("exact"))
    assertEquals(bucketSet, requiredBuckets)
    Vector(
      "4k3/8/8/8/8/8/8/4K2R w KK - 0 1",
      "4k3/8/8/8/8/8/8/4K3 w K - 0 1"
    ).foreach: rawFen =>
      assert(RootExtractor.fromFenFailClosed(Fen.Full.clean(rawFen)).isLeft, s"Castling invalid input should fail closed: $rawFen")

  test("passed_pawn reaches broad-confidence-green only with the matrix-owned polarity, phase, topology, rank, and cone bucket spread"):
    val schemaId = SchemaId.PassedPawn
    val policy = RootCoverageMatrix.policyFor(schemaId)
    val schemaRows = rows.filter(_.schema == schemaId)
    val bucketSet = schemaRows.map(passedPawnBucketKey).toSet
    val requiredBuckets = RootCoverageMatrix.greenProofBucketsFor(schemaId).toSet

    assertEquals(policy.status, "broad-confidence-green")
    assert(schemaRows.size >= policy.minimumCorpusFloor, s"$schemaId rows = ${schemaRows.size}, floor = ${policy.minimumCorpusFloor}")
    assertEquals(schemaRows.map(_.caseType).toSet, Set("exact", "near_miss"))
    assertEquals(bucketSet, requiredBuckets)

  test("backward_pawn reaches broad-confidence-green only with the matrix-owned polarity, denial, topology, support, and regime bucket spread"):
    val schemaId = SchemaId.BackwardPawn
    val policy = RootCoverageMatrix.policyFor(schemaId)
    val schemaRows = rows.filter(_.schema == schemaId)
    val bucketSet = schemaRows.map(backwardPawnBucketKey).toSet
    val requiredBuckets = RootCoverageMatrix.greenProofBucketsFor(schemaId).toSet

    assertEquals(policy.status, "broad-confidence-green")
    assert(schemaRows.size >= policy.minimumCorpusFloor, s"$schemaId rows = ${schemaRows.size}, floor = ${policy.minimumCorpusFloor}")
    assertEquals(schemaRows.map(_.caseType).toSet, Set("exact", "near_miss"))
    assertEquals(bucketSet, requiredBuckets)

  test("outpost_square reaches broad-confidence-green only with the matrix-owned polarity, phase, topology, support-shape, and fail-closed bucket spread"):
    val schemaId = SchemaId.OutpostSquare
    val policy = RootCoverageMatrix.policyFor(schemaId)
    val schemaRows = rows.filter(_.schema == schemaId)
    val bucketSet = schemaRows.map(outpostBucketKey).toSet
    val requiredBuckets = RootCoverageMatrix.greenProofBucketsFor(schemaId).toSet

    assertEquals(policy.status, "broad-confidence-green")
    assert(schemaRows.size >= policy.minimumCorpusFloor, s"$schemaId rows = ${schemaRows.size}, floor = ${policy.minimumCorpusFloor}")
    assertEquals(schemaRows.map(_.caseType).toSet, Set("exact", "near_miss", "nasty_negative"))
    assertEquals(bucketSet, requiredBuckets)

  test("candidate_passer reaches broad-confidence-green only with the matrix-owned polarity, phase, topology, support-shape, and balance bucket spread"):
    val schemaId = SchemaId.CandidatePasser
    val policy = RootCoverageMatrix.policyFor(schemaId)
    val schemaRows = rows.filter(_.schema == schemaId)
    val bucketSet = schemaRows.map(candidatePasserBucketKey).toSet
    val requiredBuckets = RootCoverageMatrix.greenProofBucketsFor(schemaId).toSet

    assertEquals(policy.status, "broad-confidence-green")
    assert(schemaRows.size >= policy.minimumCorpusFloor, s"$schemaId rows = ${schemaRows.size}, floor = ${policy.minimumCorpusFloor}")
    assertEquals(schemaRows.map(_.caseType).toSet, Set("exact", "near_miss", "nasty_negative"))
    assertEquals(bucketSet, requiredBuckets)

  test("loose_piece reaches broad-confidence-green only with the matrix-owned family, exchange-state, and material-regime bucket spread"):
    val schemaId = SchemaId.LoosePiece
    val policy = RootCoverageMatrix.policyFor(schemaId)
    val schemaRows = rows.filter(_.schema == schemaId)
    val bucketSet = schemaRows.map(loosePieceBucketKey).toSet
    val requiredBuckets = RootCoverageMatrix.greenProofBucketsFor(schemaId).toSet

    assertEquals(policy.status, "broad-confidence-green")
    assert(schemaRows.size >= policy.minimumCorpusFloor, s"$schemaId rows = ${schemaRows.size}, floor = ${policy.minimumCorpusFloor}")
    assertEquals(schemaRows.map(_.caseType).toSet, Set("exact", "near_miss"))
    assertEquals(bucketSet, requiredBuckets)

  test("trapped_piece reaches broad-confidence-green only with the matrix-owned family, trap-mode, category-rejection, and engine-probe bucket spread"):
    val schemaId = SchemaId.TrappedPiece
    val policy = RootCoverageMatrix.policyFor(schemaId)
    val schemaRows = rows.filter(_.schema == schemaId)
    val bucketSet = schemaRows.map(trappedPieceBucketKey).toSet
    val requiredBuckets = RootCoverageMatrix.greenProofBucketsFor(schemaId).toSet

    assertEquals(policy.status, "broad-confidence-green")
    assert(schemaRows.size >= policy.minimumCorpusFloor, s"$schemaId rows = ${schemaRows.size}, floor = ${policy.minimumCorpusFloor}")
    assertEquals(schemaRows.map(_.caseType).toSet, Set("exact", "near_miss", "nasty_negative"))
    assertEquals(bucketSet, requiredBuckets)

  test("king_shelter_hole reaches broad-confidence-green only with the matrix-owned phase, home, shield-file, regime, and engine-probe bucket spread"):
    val schemaId = SchemaId.KingShelterHole
    val policy = RootCoverageMatrix.policyFor(schemaId)
    val schemaRows = rows.filter(_.schema == schemaId)
    val bucketSet = schemaRows.map(kingShelterHoleBucketKey).toSet
    val requiredBuckets = RootCoverageMatrix.greenProofBucketsFor(schemaId).toSet

    assertEquals(policy.status, "broad-confidence-green")
    assert(schemaRows.size >= policy.minimumCorpusFloor, s"$schemaId rows = ${schemaRows.size}, floor = ${policy.minimumCorpusFloor}")
    assertEquals(schemaRows.map(_.caseType).toSet, Set("exact", "near_miss", "nasty_negative"))
    assertEquals(bucketSet, requiredBuckets)

  test("pinned_piece reaches broad-confidence-green only with the matrix-owned polarity, pin-state, geometry, and family bucket spread"):
    val schemaId = SchemaId.PinnedPiece
    val policy = RootCoverageMatrix.policyFor(schemaId)
    val schemaRows = rows.filter(_.schema == schemaId)
    val bucketSet = schemaRows.map(pinnedPieceBucketKey).toSet
    val requiredBuckets = RootCoverageMatrix.greenProofBucketsFor(schemaId).toSet

    assertEquals(policy.status, "broad-confidence-green")
    assert(schemaRows.size >= policy.minimumCorpusFloor, s"$schemaId rows = ${schemaRows.size}, floor = ${policy.minimumCorpusFloor}")
    assertEquals(schemaRows.map(_.caseType).toSet, Set("exact", "near_miss"))
    assertEquals(bucketSet, requiredBuckets)

  test("overloaded_piece reaches broad-confidence-green only with defender-family, target-burden, polarity, and fail-closed buckets"):
    val schemaId = SchemaId.OverloadedPiece
    val policy = RootCoverageMatrix.policyFor(schemaId)
    val schemaRows = rows.filter(_.schema == schemaId)
    val bucketSet = schemaRows.map(overloadedPieceBucketKey).toSet
    val requiredBuckets = RootCoverageMatrix.greenProofBucketsFor(schemaId).toSet

    assertEquals(policy.status, "broad-confidence-green")
    assertEquals(schemaRows.size, policy.minimumCorpusFloor, s"$schemaId rows must stay frozen to its exact broad-green inventory")
    assertEquals(schemaRows.map(_.caseType).toSet, Set("exact", "near_miss"))
    assertRowFenInventory(
      schemaRows,
      Vector(
        "r-overloaded-piece-white-d4" -> "k2r3r/8/8/8/3Q3B/8/3N4/K7 w - - 0 1",
        "r-overloaded-piece-white-near-miss" -> "k2r4/8/8/8/3Q4/8/3N4/K7 w - - 0 1",
        "r-overloaded-piece-white-rook-d4" -> "k2r3r/8/8/8/3R3R/8/3N4/K7 w - - 0 1",
        "r-overloaded-piece-white-bishop-d4" -> "1r3r1k/8/8/8/3B4/8/1N3N2/7K w - - 0 1",
        "r-overloaded-piece-white-queen-three-targets" -> "r2r3r/8/8/8/N2Q3B/8/3N4/K7 w - - 0 1",
        "r-overloaded-piece-black-queen-d5" -> "7k/3n4/8/3q3b/8/8/8/K2R3R b - - 0 1",
        "r-overloaded-piece-black-rook-d5" -> "7k/3n4/8/3r3r/8/8/8/K2R3R b - - 0 1",
        "r-overloaded-piece-black-bishop-d5" -> "7k/1n3n2/8/3b4/8/8/1R3R2/K7 b - - 0 1",
        "r-overloaded-piece-black-near-miss" -> "7k/3n4/8/3q4/8/8/8/K2R4 b - - 0 1"
      )
    )
    assertEquals(bucketSet, requiredBuckets)

  test("xray_target reaches broad-confidence-green only with slider-family, line-geometry, blocker, polarity, and fail-closed buckets"):
    val schemaId = SchemaId.XrayTarget
    val policy = RootCoverageMatrix.policyFor(schemaId)
    val schemaRows = rows.filter(_.schema == schemaId)
    val bucketSet = schemaRows.map(xrayTargetBucketKey).toSet
    val requiredBuckets = RootCoverageMatrix.greenProofBucketsFor(schemaId).toSet

    assertEquals(policy.status, "broad-confidence-green")
    assertEquals(schemaRows.size, policy.minimumCorpusFloor, s"$schemaId rows must stay frozen to its exact broad-green inventory")
    assertEquals(schemaRows.map(_.caseType).toSet, Set("exact", "near_miss"))
    assertRowFenInventory(
      schemaRows,
      Vector(
        "r-xray-target-white-d8" -> "3qk3/3p4/8/8/8/8/8/3RK3 w - - 0 1",
        "r-xray-target-white-near-miss" -> "4k3/3p4/8/8/8/8/8/3RK3 w - - 0 1",
        "r-xray-target-white-d8-own-blocker" -> "3qk3/8/8/8/3N4/8/8/3RK3 w - - 0 1",
        "r-xray-target-white-f5-bishop-diagonal" -> "4k3/8/8/5q2/8/3p4/8/1B2K3 w - - 0 1",
        "r-xray-target-white-h4-queen-rank" -> "4k3/8/8/8/Q2N3r/8/8/4K3 w - - 0 1",
        "r-xray-target-black-d1-rook-file" -> "3rk3/8/8/8/8/8/3P4/3QK3 b - - 0 1",
        "r-xray-target-black-c4-bishop-diagonal" -> "4k1b1/8/4P3/8/2Q5/8/8/4K3 b - - 0 1",
        "r-xray-target-black-a5-queen-rank" -> "4k3/8/8/R3n2q/8/8/8/4K3 b - - 0 1",
        "r-xray-target-black-two-blockers-near-miss" -> "4k3/8/8/R1b1n2q/8/8/8/4K3 b - - 0 1"
      )
    )
    assertEquals(bucketSet, requiredBuckets)

  private def assertRowFenInventory(
      schemaRows: Iterable[RootExpectationCorpus.Row],
      expected: Vector[(String, String)]
  ): Unit =
    assertEquals(schemaRows.map(row => row.id -> row.fen).toVector, expected)

  private def enPassantBucketKey(row: RootExpectationCorpus.Row): String =
    val fenParts = row.fen.split(' ')
    val epField = fenParts(3)
    val fileTopology =
      epField.headOption match
        case Some(file) if Set('a', 'b', 'g', 'h').contains(file) => "edge"
        case Some(_) => "center"
        case None => fail(s"Missing en-passant field in ${row.id}")

    val boardContext =
      val piecePlacement = row.fen.takeWhile(_ != ' ')
      val nonKingNonPawnCount = piecePlacement.count(ch => "nbrqNBRQ".contains(ch))
      if nonKingNonPawnCount == 0 then "sparse" else "mixed"

    val extractedState =
      actualEnPassantState(row)

    val (stateFamily, legality) =
      row.trueState match
        case Some("none") =>
          assertEquals(extractedState, "none", s"Extractor en-passant state drifted for ${row.id}")
          ("none", "legal_none")
        case Some(state) if state.startsWith("white_can_capture_on_file_") =>
          assert(extractedState.startsWith("white_can_capture_on_file_"), s"Extractor en-passant state drifted for ${row.id}: $extractedState")
          assertEquals(extractedState, state, s"Extractor en-passant file drifted for ${row.id}")
          assertEquals(epField.headOption.map(_.toString), Some(state.stripPrefix("white_can_capture_on_file_")), s"Declared en-passant file is stale for ${row.id}")
          ("white_capture", "real_capture")
        case Some(state) if state.startsWith("black_can_capture_on_file_") =>
          assert(extractedState.startsWith("black_can_capture_on_file_"), s"Extractor en-passant state drifted for ${row.id}: $extractedState")
          assertEquals(extractedState, state, s"Extractor en-passant file drifted for ${row.id}")
          assertEquals(epField.headOption.map(_.toString), Some(state.stripPrefix("black_can_capture_on_file_")), s"Declared en-passant file is stale for ${row.id}")
          ("black_capture", "real_capture")
        case other => fail(s"Unexpected en-passant state for ${row.id}: $other")

    s"$stateFamily|$fileTopology|$boardContext|$legality"

  private def pieceOnBucketKey(row: RootExpectationCorpus.Row): String =
    val square =
      actualPieceOnSquares(row) match
        case Vector(subject) =>
          assertEquals(row.trueSquares.toVector, Vector(subject), s"Exact piece_on declaration drifted from extractor truth for ${row.id}")
          subject
        case other => fail(s"Expected exactly one extracted piece_on square for ${row.id}: $other")

    val polarity = row.polarityColor.getOrElse(fail(s"Missing polarityColor for ${row.id}"))
    val roleFamily =
      row.requiredRole match
        case chess.Pawn => "pawn"
        case chess.Knight | chess.Bishop => "minor"
        case chess.Rook => "rook"
        case chess.Queen => "queen"
        case chess.King => "king"
    val topology =
      if Set('a', 'h').contains(square.head) || Set('1', '8').contains(square.last) then "edge"
      else "center"

    s"$polarity|$roleFamily|$topology"

  private def controlledByBucketKey(row: RootExpectationCorpus.Row): String =
    val polarity = row.polarityColor.getOrElse(fail(s"Missing polarityColor for ${row.id}"))
    val squares = actualSchemaSquares(row)
    assertEquals(row.trueSquares.toVector, squares, s"Exact controlled_by declaration drifted from extractor truth for ${row.id}")
    val (sourcePiece, sourceSquare) = controlledByPrimarySource(row, polarity)
    val position = Fen.read(variant.Standard, row.normalizedFen).getOrElse(fail(s"Invalid FEN for ${row.id}: ${row.normalizedFen}"))
    val square = chess.Square.fromKey(sourceSquare).getOrElse(fail(s"Invalid controlled_by source square for ${row.id}: $sourceSquare"))
    val attackSquares = controlledByAttackSquares(position, square, sourcePiece)
    assert(attackSquares.nonEmpty, s"Primary controlled_by source has no attack squares for ${row.id}: $sourceSquare")
    val targetOccupancy =
      if attackSquares.exists(target => position.board.pieceAt(target).nonEmpty) then "occupied_target" else "empty_target"

    s"$polarity|${controlledByFamily(sourcePiece)}|${sourceTopology(sourceSquare)}|$targetOccupancy"

  private def contestedBucketKey(row: RootExpectationCorpus.Row): String =
    val squares = actualNeutralSchemaSquares(row)
    assertEquals(row.trueSquares.toVector, squares, s"Contested declaration drifted from extractor truth for ${row.id}")
    row.caseType match
      case "exact" =>
        assert(squares.nonEmpty, s"Exact contested row must extract at least one overlap for ${row.id}")
        s"${contestedCarrier(row)}|${contestedTopology(squares)}|${contestedOccupancy(row, squares)}"
      case "near_miss" =>
        assertEquals(squares, Vector.empty, s"Near-miss contested row extracted overlap for ${row.id}")
        assert(nonKingPieces(row.fen, "white").isEmpty, s"Near-miss contested disjoint-kings row has white non-king material for ${row.id}")
        assert(nonKingPieces(row.fen, "black").isEmpty, s"Near-miss contested disjoint-kings row has black non-king material for ${row.id}")
        "disjoint_kings|no_overlap|empty"
      case other => fail(s"Unexpected contested case type for ${row.id}: $other")

  private def controlledByPrimarySource(row: RootExpectationCorpus.Row, polarity: String): (Char, String) =
    val nonKingSources =
      boardPieces(row.fen).collect:
        case (piece, square) if pieceColor(piece) == polarity && piece.toUpper != 'K' => piece -> square
    nonKingSources.distinct match
      case Vector(source) => source
      case Vector() =>
        val king = if polarity == "white" then 'K' else 'k'
        boardPieces(row.fen).collect:
          case (piece, square) if piece == king => piece -> square
        .distinct match
          case Vector(source) => source
          case other => fail(s"Unable to derive unique king source for controlled_by row ${row.id}: $other")
      case other => fail(s"Unable to derive unique non-king source for controlled_by row ${row.id}: $other")

  private def controlledByAttackSquares(position: chess.Position, square: chess.Square, pieceChar: Char): Vector[chess.Square] =
    val piece = position.board.pieceAt(square).getOrElse(fail(s"Missing controlled_by source on board at ${square.key}"))
    pieceChar.toUpper match
      case 'P' => square.pawnAttacks(piece.color).squares.toVector
      case 'N' => square.knightAttacks.squares.toVector
      case 'B' => square.bishopAttacks(position.board.occupied).squares.toVector
      case 'R' => square.rookAttacks(position.board.occupied).squares.toVector
      case 'Q' => square.queenAttacks(position.board.occupied).squares.toVector
      case 'K' => square.kingAttacks.squares.toVector
      case other => fail(s"Unexpected controlled_by source piece: $other")

  private def controlledByFamily(piece: Char): String =
    piece.toUpper match
      case 'P' => "pawn"
      case 'N' => "knight"
      case 'B' => "bishop"
      case 'R' => "rook"
      case 'Q' => "queen"
      case 'K' => "king"
      case other => fail(s"Unexpected controlled_by source family: $other")

  private def sourceTopology(square: String): String =
    val file = square.head
    val rank = square.last
    if Set("a1", "a8", "h1", "h8").contains(square) then "corner"
    else if Set('a', 'b', 'g', 'h').contains(file) || Set('1', '2', '7', '8').contains(rank) then "edge"
    else "center"

  private def contestedCarrier(row: RootExpectationCorpus.Row): String =
    val whiteRoles = contestedNonKingFamilies(row.fen, "white")
    val blackRoles = contestedNonKingFamilies(row.fen, "black")
    (whiteRoles, blackRoles) match
      case (white, black) if white.contains("rook") && black.contains("rook") =>
        val squares = actualNeutralSchemaSquares(row)
        if squares.size >= 6 && squares.map(_.head).distinct.size == 1 then "open_file_rooks"
        else if squares.size == 1 then "blocked_file_rooks"
        else "rooks_rank"
      case (white, black) if white.contains("knight") && black.contains("knight") => "knights"
      case (white, black) if white.contains("bishop") && black.contains("bishop") => "bishops"
      case (white, black) if white.contains("queen") && black.contains("queen") => "queens"
      case other => fail(s"Unexpected contested carrier for ${row.id}: $other")

  private def contestedNonKingFamilies(fen: String, polarity: String): Vector[String] =
    boardPieces(fen).collect:
      case (piece, _) if pieceColor(piece) == polarity && piece.toUpper != 'K' =>
        controlledByFamily(piece)
    .distinct

  private def contestedTopology(squares: Vector[String]): String =
    if squares.size >= 6 && squares.map(_.head).distinct.size == 1 then "file_lane"
    else if squares.exists(square => !isEdgeSquare(square)) then "central_square"
    else "edge_square"

  private def contestedOccupancy(row: RootExpectationCorpus.Row, squares: Vector[String]): String =
    if squares.exists(square => pieceAt(row.fen, square).nonEmpty) then "occupied" else "empty"

  private def isEdgeSquare(square: String): Boolean =
    Set('a', 'b', 'g', 'h').contains(square.head) || Set('1', '2', '7', '8').contains(square.last)

  private def openFileBucketKey(row: RootExpectationCorpus.Row): String =
    val files = actualOpenFiles(row)
    assertEquals(row.trueFiles.toVector, files, s"Exact open_file declaration drifted from extractor truth for ${row.id}")

    val boardContext =
      if files.size == 8 then "all_open" else "mixed_board"
    val topology =
      if files.size == 8 then "all_files"
      else if files.forall(file => Set("a", "h").contains(file)) then "edge"
      else "center"
    val adjacency =
      val fileIndexes = files.map(file => file.head - 'a').sorted
      if fileIndexes.sliding(2).exists(pair => pair.size == 2 && pair(1) == pair(0) + 1) then "clustered_open"
      else "isolated_open"

    s"$boardContext|$topology|$adjacency"

  private def halfOpenFileBucketKey(row: RootExpectationCorpus.Row): String =
    val polarity = row.polarityColor.getOrElse(fail(s"Missing polarityColor for ${row.id}"))
    val files = actualHalfOpenFiles(row)

    row.caseType match
      case "exact" =>
        assertEquals(row.trueFiles.toVector, files, s"Exact half_open_file declaration drifted from extractor truth for ${row.id}")
        val file =
          files match
            case Vector(subject) => subject.head
            case other => fail(s"Expected exactly one extracted half_open_file for ${row.id}: $other")
        assert(pawnsOnFile(row.fen, polarity, file).isEmpty, s"Half-open file has an own pawn in ${row.id}")
        val rivalPawnCount = pawnsOnFile(row.fen, opposite(polarity), file).size
        val rivalLayout =
          if rivalPawnCount == 1 then "single_enemy_pawn"
          else if rivalPawnCount >= 2 then "stacked_enemy_pawns"
          else fail(s"Half-open file has no rival pawn in ${row.id}")
        s"$polarity|${fileTopology(file)}|$rivalLayout|exact"
      case "near_miss" =>
        assertEquals(files, Vector.empty, s"Near-miss half_open_file row extracted non-empty truth for ${row.id}")
        assert(row.trueFiles.isEmpty, s"Near-miss half_open_file row should not declare true files for ${row.id}")
        val restoredFile =
          canonicalFiles.map(_.char).filter: file =>
            pawnsOnFile(row.fen, polarity, file).nonEmpty &&
              pawnsOnFile(row.fen, opposite(polarity), file).nonEmpty
          match
            case Vector(file) => file
            case other => fail(s"Expected exactly one own-pawn-restored file in ${row.id}: $other")
        s"$polarity|${fileTopology(restoredFile)}|-|own_pawn_restored"
      case other => fail(s"Unexpected half_open_file case type for ${row.id}: $other")

  private def kingRingBucketKeys(row: RootExpectationCorpus.Row): Vector[String] =
    val polarity = row.polarityColor.getOrElse(fail(s"Missing polarityColor for ${row.id}"))
    val squares = actualSchemaSquares(row)
    assertEquals(row.trueSquares.toVector, squares, s"Exact king_ring_square declaration drifted from extractor truth for ${row.id}")
    val kingSquare = uniqueKingSquare(row.fen, polarity, row.id)
    val fileFamily = kingFileFamily(kingSquare.head)
    squares.map(square => s"$polarity|$fileFamily|${kingRingAdjacency(kingSquare, square)}").distinct

  private def pawnControlledByBucketKeys(row: RootExpectationCorpus.Row): Vector[String] =
    val polarity = row.polarityColor.getOrElse(fail(s"Missing polarityColor for ${row.id}"))
    row.caseType match
      case "exact" =>
        val squares = actualSchemaSquares(row)
        assertEquals(row.trueSquares.toVector, squares, s"Exact pawn_controlled_by declaration drifted from extractor truth for ${row.id}")
        squares.map(square => s"$polarity|${fileTopology(square.head)}|exact").distinct
      case "near_miss" =>
        assertEquals(actualSchemaSquares(row), Vector.empty, s"Near-miss pawn_controlled_by row extracted non-empty truth for ${row.id}")
        assert(row.trueSquares.isEmpty, s"Near-miss pawn_controlled_by row should not declare true squares for ${row.id}")
        Vector(s"$polarity|-|near_miss")
      case other => fail(s"Unexpected pawn_controlled_by case type for ${row.id}: $other")

  private def weakSquareBucketKeys(row: RootExpectationCorpus.Row): Vector[String] =
    val polarity = row.polarityColor.getOrElse(fail(s"Missing polarityColor for ${row.id}"))
    val materialRegime = weakSquareMaterialRegime(row.fen, polarity)
    row.caseType match
      case "exact" =>
        val squares = actualSchemaSquares(row)
        assertEquals(row.trueSquares.toVector, squares, s"Exact weak_square declaration drifted from extractor truth for ${row.id}")
        squares.map: subject =>
          assert(isInEnemyTerritory(polarity, subject), s"Weak square is outside enemy territory for ${row.id}: $subject")
          assert(!enemyPawnCanChallengeSquare(row.fen, polarity, subject), s"Exact weak_square row has a restored enemy pawn challenge for ${row.id}: $subject")
          assert(weakSquareCanExploit(row.fen, polarity, subject), s"Exact weak_square row lacks beneficiary piece exploitation for ${row.id}: $subject")
          val occupancy = weakSquareOccupancy(row.fen, polarity, subject, row.id)
          s"$polarity|${weakSquareTopology(subject)}|$occupancy|$materialRegime|challenge_absent"
        .distinct
      case "near_miss" =>
        assertEquals(actualSchemaSquares(row), Vector.empty, s"Near-miss weak_square row extracted non-empty truth for ${row.id}")
        assert(row.trueSquares.isEmpty, s"Near-miss weak_square row should not declare true squares for ${row.id}")
        val challengedTargets = weakSquareChallengeRestoredTargets(row.fen, polarity)
        assert(challengedTargets.nonEmpty, s"Near-miss weak_square row has no challenge-restored candidate target for ${row.id}")
        challengedTargets.foreach: target =>
          assert(pieceAt(row.fen, target).isEmpty, s"Near-miss weak_square challenge-restored target must stay empty for ${row.id}: $target")
          assert(enemyPawnCanChallengeSquare(row.fen, polarity, target), s"Near-miss weak_square target lacks restored enemy pawn challenge for ${row.id}: $target")
          assert(weakSquareCanExploit(row.fen, polarity, target), s"Near-miss weak_square target lacks beneficiary piece exploitation for ${row.id}: $target")
        challengedTargets.map(target => s"$polarity|${weakSquareTopology(target)}|-|$materialRegime|challenge_restored").distinct
      case other => fail(s"Unexpected weak_square case type for ${row.id}: $other")

  private def weakSquareOccupancy(fen: String, polarity: String, subject: String, rowId: String): String =
    pieceAt(fen, subject) match
      case Some(piece) if pieceColor(piece) == polarity && !"PpKk".contains(piece) => "occupied_by_beneficiary_piece"
      case Some(piece) => fail(s"Weak square has illegal occupant for $rowId at $subject: $piece")
      case None => "empty"

  private def weakSquareTopology(square: String): String =
    if isEdgeSquare(square) then "edge" else "center"

  private def weakSquareMaterialRegime(fen: String, polarity: String): String =
    if boardPieces(fen).exists { case (piece, _) => pieceColor(piece) == polarity && "RrQq".contains(piece) } then "heavy_piece"
    else "minor_piece"

  private def weakSquareChallengeRestoredTargets(fen: String, polarity: String): Vector[String] =
    RootAtomRegistry.canonicalSquares.map(_.key).filter: target =>
      isInEnemyTerritory(polarity, target) &&
        weakSquareOccupancyCandidate(fen, polarity, target) &&
        weakSquareCanExploit(fen, polarity, target) &&
        enemyPawnCanChallengeSquare(fen, polarity, target)

  private def weakSquareOccupancyCandidate(fen: String, polarity: String, target: String): Boolean =
    pieceAt(fen, target).forall(piece => pieceColor(piece) == polarity && !"PpKk".contains(piece))

  private def weakSquareCanExploit(fen: String, polarity: String, target: String): Boolean =
    pieceAt(fen, target).exists(piece => pieceColor(piece) == polarity && !"PpKk".contains(piece)) ||
      Fen.read(variant.Standard, Fen.Full.clean(fen)).exists: position =>
        val color = chessColor(polarity)
        val square = chess.Square.fromKey(target).getOrElse(fail(s"Invalid weak_square target: $target"))
        position.board.attackers(square, color).exists: source =>
          position.board.pieceAt(source).exists(piece => piece.color == color && piece.role != chess.Pawn && piece.role != chess.King)

  private def isolatedPawnBucketKey(row: RootExpectationCorpus.Row): String =
    val polarity = row.polarityColor.getOrElse(fail(s"Missing polarityColor for ${row.id}"))
    row.caseType match
      case "exact" =>
        actualSchemaSquares(row) match
          case Vector(subject) =>
            assertEquals(row.trueSquares.toVector, Vector(subject), s"Exact isolated_pawn declaration drifted from extractor truth for ${row.id}")
            assert(!hasAdjacentFriendlyPawn(row.fen, polarity, subject), s"Exact isolated pawn has an adjacent friendly pawn in ${row.id}")
            s"$polarity|${fileTopology(subject.head)}|${pawnRankStage(polarity, subject)}|isolated"
          case other => fail(s"Expected exactly one extracted isolated_pawn square for ${row.id}: $other")
      case "near_miss" =>
        assertEquals(actualSchemaSquares(row), Vector.empty, s"Near-miss isolated_pawn row extracted non-empty truth for ${row.id}")
        assert(row.trueSquares.isEmpty, s"Near-miss isolated_pawn row should not declare true squares for ${row.id}")
        assert(
          pawnsOf(row.fen, polarity).exists(square => hasAdjacentFriendlyPawn(row.fen, polarity, square)),
          s"Near-miss isolated_pawn row must restore an adjacent friendly pawn for ${row.id}"
        )
        s"$polarity|-|-|neighbor_restored"
      case other => fail(s"Unexpected isolated_pawn case type for ${row.id}: $other")

  private def doubledFileBucketKey(row: RootExpectationCorpus.Row): String =
    val polarity = row.polarityColor.getOrElse(fail(s"Missing polarityColor for ${row.id}"))
    val files = actualFileMask(row)
    row.caseType match
      case "exact" =>
        assertEquals(row.trueFiles.toVector, files, s"Exact doubled_file declaration drifted from extractor truth for ${row.id}")
        val file =
          files match
            case Vector(subject) => subject.head
            case other => fail(s"Expected exactly one extracted doubled_file for ${row.id}: $other")
        s"$polarity|${fileTopology(file)}|${doubledFileStackShape(row.fen, polarity, file)}|exact"
      case "near_miss" =>
        assertEquals(files, Vector.empty, s"Near-miss doubled_file row extracted non-empty truth for ${row.id}")
        assert(row.trueFiles.isEmpty, s"Near-miss doubled_file row should not declare true files for ${row.id}")
        s"$polarity|-|-|near_miss"
      case other => fail(s"Unexpected doubled_file case type for ${row.id}: $other")

  private def fixedPawnBucketKeys(row: RootExpectationCorpus.Row): Vector[String] =
    val polarity = row.polarityColor.getOrElse(fail(s"Missing polarityColor for ${row.id}"))
    val squares = actualSchemaSquares(row)
    assertEquals(row.trueSquares.toVector, squares, s"Exact fixed_pawn declaration drifted from extractor truth for ${row.id}")
    squares.map: subject =>
      assert(isFixedPawnSubject(row.fen, polarity, subject), s"Fixed-pawn subject is not structurally fixed in ${row.id}: $subject")
      s"$polarity|${fileTopology(subject.head)}|${fixedPawnChainShape(row.fen, polarity, subject)}"
    .distinct

  private def leverAvailableBucketKey(row: RootExpectationCorpus.Row): String =
    val polarity = row.polarityColor.getOrElse(fail(s"Missing polarityColor for ${row.id}"))
    row.caseType match
      case "exact" =>
        actualSchemaSquares(row) match
          case Vector(subject) =>
            assertEquals(row.trueSquares.toVector, Vector(subject), s"Exact lever_available declaration drifted from extractor truth for ${row.id}")
            val (pushType, contactDirection) = leverPushAndContact(row.fen, polarity, subject, row.id)
            s"$polarity|$pushType|${fileTopology(subject.head)}|$contactDirection|exact"
          case other => fail(s"Expected exactly one extracted lever_available square for ${row.id}: $other")
      case "near_miss" =>
        assertEquals(actualSchemaSquares(row), Vector.empty, s"Near-miss lever_available row extracted non-empty truth for ${row.id}")
        assert(row.trueSquares.isEmpty, s"Near-miss lever_available row should not declare true squares for ${row.id}")
        assert(pawnsOf(row.fen, polarity).exists(square => forwardSquare(polarity, square).exists(pieceAt(row.fen, _).nonEmpty)), s"Near-miss lever_available row must block a pawn push for ${row.id}")
        s"$polarity|-|-|-|blocked_push"
      case other => fail(s"Unexpected lever_available case type for ${row.id}: $other")

  private def sideToMoveBucketKey(row: RootExpectationCorpus.Row): String =
    val expectedState = row.trueState.getOrElse(fail(s"Missing side-to-move state for ${row.id}"))
    val extractedState = actualSideToMoveState(row)
    assertEquals(extractedState, expectedState, s"Extractor side-to-move state drifted for ${row.id}")
    val state =
      expectedState match
        case "white_to_move" => "white"
        case "black_to_move" => "black"
        case other => fail(s"Unexpected side-to-move state for ${row.id}: $other")
    s"$state|${sideToMoveBoardContext(row.fen)}"

  private def castlingRightsBucketKey(row: RootExpectationCorpus.Row): String =
    val expectedState = row.trueState.getOrElse(fail(s"Missing castling-rights state for ${row.id}"))
    val extractedState = actualCastlingRightsState(row)
    assertEquals(extractedState, expectedState, s"Extractor castling-rights state drifted for ${row.id}")
    s"${castlingRightsFamily(expectedState)}|${castlingRightsScope(expectedState)}"

  private def passedPawnBucketKey(row: RootExpectationCorpus.Row): String =
    val polarity =
      row.polarityColor.getOrElse(fail(s"Missing polarityColor for ${row.id}"))

    val squareKey =
      row.caseType match
        case "exact" =>
          actualSchemaSquares(row) match
            case Vector(subject) =>
              assertEquals(row.trueSquares.toVector, Vector(subject), s"Exact passed-pawn declaration drifted from extractor truth for ${row.id}")
              subject
            case other => fail(s"Expected exactly one extracted passed-pawn square for ${row.id}: $other")
        case "near_miss" =>
          assertEquals(actualSchemaSquares(row), Vector.empty, s"Near-miss passed-pawn row extracted non-empty truth for ${row.id}")
          assert(row.trueSquares.isEmpty, s"Near-miss passed-pawn row should not declare true squares for ${row.id}")
          uniquePawnSquare(row.fen, polarity, row.id)
        case other => fail(s"Unexpected case type for ${row.id}: $other")

    val fileChar = squareKey.head
    val rankChar = squareKey.last

    val phase =
      val piecePlacement = row.fen.takeWhile(_ != ' ')
      val nonKingNonPawnCount = piecePlacement.count(ch => "nbrqNBRQ".contains(ch))
      if nonKingNonPawnCount == 0 then "endgame" else "transition"

    val fileTopology =
      if Set('a', 'b', 'g', 'h').contains(fileChar) then "edge" else "center"

    val rankStage =
      if (polarity == "white" && rankChar == '7') || (polarity == "black" && rankChar == '2') then "7th_rank"
      else "midboard"

    val opposition =
      if enemyPawnInFrontCone(row.fen, polarity, squareKey) then "blocked_cone"
      else "clear_cone"

    s"$polarity|$phase|$fileTopology|$rankStage|$opposition"

  private def candidatePasserBucketKey(row: RootExpectationCorpus.Row): String =
    val polarity =
      row.polarityColor.getOrElse(fail(s"Missing polarityColor for ${row.id}"))
    val (subject, supportShape, balanceState) =
      row.caseType match
        case "exact" =>
          actualSchemaSquares(row) match
            case Vector(square) =>
              assertEquals(row.trueSquares.toVector, Vector(square), s"Exact candidate-passer declaration drifted from extractor truth for ${row.id}")
              assert(
                candidatePasserFrontIsEmpty(row.fen, polarity, square),
                s"Exact candidate-passer row does not have an empty front square for ${row.id}"
              )
              val supportCount = candidatePasserSupportPawns(row.fen, polarity, square).size
              val oppositionCount = candidatePasserOppositionPawns(row.fen, polarity, square).size
              assert(oppositionCount > 0, s"Exact candidate-passer row is already passed for ${row.id}")
              assert(
                supportCount >= oppositionCount,
                s"Exact candidate-passer row lost support/opposition balance for ${row.id}: support=$supportCount opposition=$oppositionCount"
              )
              (square, candidatePasserSupportShape(row.fen, polarity, square, row.id), "candidate_exact")
            case other => fail(s"Expected exactly one extracted candidate-passer square for ${row.id}: $other")
        case "near_miss" | "nasty_negative" =>
          assertEquals(actualSchemaSquares(row), Vector.empty, s"Fail-closed candidate-passer row extracted non-empty truth for ${row.id}")
          assert(row.trueSquares.isEmpty, s"Fail-closed candidate-passer row should not declare true squares for ${row.id}")
          val square = candidatePasserSubjectSquare(row, polarity)
          assert(
            candidatePasserFrontIsEmpty(row.fen, polarity, square),
            s"Fail-closed candidate-passer row must keep the front square empty so rejection stays on balance/pass status for ${row.id}"
          )
          val oppositionCount = candidatePasserOppositionPawns(row.fen, polarity, square).size
          val supportCount = candidatePasserSupportPawns(row.fen, polarity, square).size
          val state =
            if oppositionCount == 0 then "already_passed_negative"
            else
              assert(
                supportCount < oppositionCount,
                s"Under-supported candidate-passer row is not actually under-supported for ${row.id}: support=$supportCount opposition=$oppositionCount"
              )
              "under_supported_near_miss"
          (square, "-", state)
        case other => fail(s"Unexpected case type for ${row.id}: $other")

    val phase =
      val piecePlacement = row.fen.takeWhile(_ != ' ')
      val nonKingNonPawnCount = piecePlacement.count(ch => "nbrqNBRQ".contains(ch))
      if nonKingNonPawnCount == 0 then "endgame" else "transition"
    val fileTopology =
      if Set('a', 'b', 'g', 'h').contains(subject.head) then "edge" else "center"

    s"$polarity|$phase|$fileTopology|$supportShape|$balanceState"

  private def enemyPawnInFrontCone(fen: String, polarity: String, squareKey: String): Boolean =
    val pawnFile = squareKey.head - 'a'
    val pawnRank = squareKey.last.asDigit
    val targetPawn = if polarity == "white" then 'p' else 'P'

    boardPieces(fen).exists: (piece, square) =>
      if piece != targetPawn then false
      else
        val file = square.head - 'a'
        val rank = square.last.asDigit
        math.abs(file - pawnFile) <= 1 &&
          (if polarity == "white" then rank > pawnRank else rank < pawnRank)

  private def backwardPawnBucketKey(row: RootExpectationCorpus.Row): String =
    val polarity =
      row.polarityColor.getOrElse(fail(s"Missing polarityColor for ${row.id}"))
    val squareKey =
      row.caseType match
        case "exact" =>
          actualSchemaSquares(row) match
            case Vector(subject) =>
              assertEquals(row.trueSquares.toVector, Vector(subject), s"Exact backward-pawn declaration drifted from extractor truth for ${row.id}")
              subject
            case other => fail(s"Expected exactly one extracted backward-pawn square for ${row.id}: $other")
        case "near_miss" =>
          assertEquals(actualSchemaSquares(row), Vector.empty, s"Near-miss backward-pawn row extracted non-empty truth for ${row.id}")
          assert(row.trueSquares.isEmpty, s"Near-miss backward-pawn row should not declare true squares for ${row.id}")
          backwardPawnSubjectSquare(row, polarity)
        case other => fail(s"Unexpected case type for ${row.id}: $other")
    val front = forwardSquare(polarity, squareKey).getOrElse(fail(s"Missing front square for ${row.id}"))
    val frontDenial =
      pieceAt(row.fen, front) match
        case Some(piece) if pieceColor(piece) != polarity => "direct_blockade"
        case _ => "enemy_control"
    val fileTopology =
      if Set('a', 'b', 'g', 'h').contains(squareKey.head) then "edge" else "center"
    val supportState =
      if hasStructuralPawnSupport(row.fen, polarity, front) then "support_restored" else "support_missing"
    val materialRegime =
      if boardPieces(row.fen).exists { case (piece, _) => !"PpKk".contains(piece) } then "mixed_pieces"
      else "pawn_chain"

    s"$polarity|$frontDenial|$fileTopology|$supportState|$materialRegime"

  private def outpostBucketKey(row: RootExpectationCorpus.Row): String =
    val polarity =
      row.polarityColor.getOrElse(fail(s"Missing polarityColor for ${row.id}"))
    val subject =
      row.caseType match
        case "exact" =>
          actualSchemaSquares(row) match
            case Vector(square) =>
              assertEquals(row.trueSquares.toVector, Vector(square), s"Exact outpost declaration drifted from extractor truth for ${row.id}")
              assert(actualSchemaSquares(row, SchemaId.WeakSquare).contains(square), s"Exact outpost row lost weak-square support for ${row.id}")
              square
            case other => fail(s"Expected exactly one extracted outpost square for ${row.id}: $other")
        case "near_miss" | "nasty_negative" =>
          assertEquals(actualSchemaSquares(row), Vector.empty, s"Fail-closed outpost row extracted non-empty truth for ${row.id}")
          assert(row.trueSquares.isEmpty, s"Fail-closed outpost row should not declare true squares for ${row.id}")
          outpostSubjectSquare(row, polarity)
        case other => fail(s"Unexpected case type for ${row.id}: $other")

    val phase =
      if boardPieces(row.fen).exists { case (piece, _) => "Qq".contains(piece) } then "middlegame"
      else "queenless_transition"
    val topology =
      if Set('a', 'b', 'g', 'h').contains(subject.head) then "edge" else "center"

    row.caseType match
      case "exact" =>
        val supportShape = outpostSupportShape(row.fen, polarity, subject, row.id)
        s"$polarity|$phase|$topology|$supportShape|exact"
      case "near_miss" | "nasty_negative" =>
        val supportAvailable = hasStructuralPawnSupport(row.fen, polarity, subject)
        val failClosedState =
          if !supportAvailable then "no_support"
          else
            assert(
              enemyPawnCanChallengeSquare(row.fen, polarity, subject),
              s"Support-present outpost fail-closed row does not restore enemy pawn challenge for ${row.id}"
            )
            assert(
              !actualSchemaSquares(row, SchemaId.WeakSquare).contains(subject),
              s"Support-present outpost fail-closed row still extracts weak_square for ${row.id}"
            )
            "challenge_restored"
        val supportShape =
          if supportAvailable then outpostSupportShape(row.fen, polarity, subject, row.id)
          else "-"
        s"$polarity|$phase|$topology|$supportShape|$failClosedState"
      case other => fail(s"Unexpected case type for ${row.id}: $other")

  private def pinnedPieceBucketKey(row: RootExpectationCorpus.Row): String =
    val polarity =
      row.polarityColor.getOrElse(fail(s"Missing polarityColor for ${row.id}"))
    val subject =
      row.caseType match
        case "exact" =>
          actualSchemaSquares(row) match
            case Vector(square) =>
              assertEquals(row.trueSquares.toVector, Vector(square), s"Exact pinned-piece declaration drifted from extractor truth for ${row.id}")
              square
            case other => fail(s"Expected exactly one extracted pinned-piece square for ${row.id}: $other")
        case "near_miss" =>
          assertEquals(actualSchemaSquares(row), Vector.empty, s"Near-miss pinned-piece row extracted non-empty truth for ${row.id}")
          assert(row.trueSquares.isEmpty, s"Near-miss pinned-piece row should not declare true squares for ${row.id}")
          pinnedPieceSubjectSquare(row, polarity)
        case other => fail(s"Unexpected case type for ${row.id}: $other")
    val subjectPiece = pieceAt(row.fen, subject).getOrElse(fail(s"Missing pinned-piece subject on board for ${row.id}"))
    val pieceFamily = pinnedPieceFamily(subjectPiece, row.id)

    val (pinState, lineGeometry) =
      row.caseType match
        case "exact" =>
          pinnedStateAndGeometry(row.fen, polarity, subject, row.id)
        case "near_miss" =>
          assert(absolutePinGeometry(row.fen, polarity, subject).isEmpty, s"Near-miss pinned-piece row is actually an absolute pin for ${row.id}")
          assert(relativePinGeometry(row.fen, polarity, subject).isEmpty, s"Near-miss pinned-piece row is actually a relative pin for ${row.id}")
          ("fail_closed", alignedEnemySliderGeometry(row.fen, polarity, subject, row.id))
        case other => fail(s"Unexpected case type for ${row.id}: $other")

    s"$polarity|$pinState|$lineGeometry|$pieceFamily"

  private def overloadedPieceBucketKey(row: RootExpectationCorpus.Row): String =
    val polarity =
      row.polarityColor.getOrElse(fail(s"Missing polarityColor for ${row.id}"))
    val subject =
      row.caseType match
        case "exact" =>
          actualSchemaSquares(row) match
            case Vector(square) =>
              assertEquals(row.trueSquares.toVector, Vector(square), s"Exact overloaded-piece declaration drifted from extractor truth for ${row.id}")
              square
            case other => fail(s"Expected exactly one extracted overloaded-piece square for ${row.id}: $other")
        case "near_miss" =>
          assertEquals(actualSchemaSquares(row), Vector.empty, s"Near-miss overloaded-piece row extracted non-empty truth for ${row.id}")
          assert(row.trueSquares.isEmpty, s"Near-miss overloaded-piece row should not declare true squares for ${row.id}")
          overloadedPieceNearMissSubject(row, polarity)
        case other => fail(s"Unexpected case type for ${row.id}: $other")

    val subjectPiece = pieceAt(row.fen, subject).getOrElse(fail(s"Missing overloaded-piece subject on board for ${row.id}"))
    if row.caseType == "exact" then
      assertEquals(
        overloadedPieceSubjects(row, polarity),
        Vector(subject),
        s"Exact overloaded-piece row must contain exactly one overloaded defender for ${row.id}"
      )
    val criticalTargets = overloadedCriticalTargets(row, subject)
    val countBucket =
      row.caseType match
        case "exact" =>
          assert(criticalTargets.size >= 2, s"Exact overloaded-piece row has fewer than two critical defensive targets for ${row.id}")
          if criticalTargets.size >= 3 then "three_plus" else "two"
        case "near_miss" =>
          assertEquals(criticalTargets.size, 1, s"Near-miss overloaded-piece row must preserve exactly one critical target for ${row.id}")
          "one_near_miss"
        case other => fail(s"Unexpected case type for ${row.id}: $other")

    s"$polarity|${overloadedPieceFamily(subjectPiece, row.id)}|$countBucket|${overloadedTargetMix(criticalTargets)}"

  private def overloadedPieceSubjects(row: RootExpectationCorpus.Row, polarity: String): Vector[String] =
    nonKingPieces(row.fen, polarity).collect:
      case (_, square) if overloadedCriticalTargets(row, square).size >= 2 => square

  private def overloadedPieceNearMissSubject(row: RootExpectationCorpus.Row, polarity: String): String =
    val candidates =
      nonKingPieces(row.fen, polarity).map: (_, square) =>
        square -> overloadedCriticalTargets(row, square)
    assert(
      candidates.forall { case (_, targets) => targets.size < 2 },
      s"Near-miss overloaded-piece row still contains an overloaded defender for ${row.id}: $candidates"
    )
    candidates.collect { case (square, targets) if targets.size == 1 => square } match
      case Vector(subject) => subject
      case other => fail(s"Expected exactly one one-target overloaded-piece near miss subject for ${row.id}: $other")

  private def overloadedCriticalTargets(row: RootExpectationCorpus.Row, subject: String): Vector[(String, Char)] =
    val position = Fen.read(variant.Standard, row.normalizedFen).getOrElse(fail(s"Invalid FEN for ${row.id}: ${row.normalizedFen}"))
    val subjectSquare = chess.Square.fromKey(subject).getOrElse(fail(s"Invalid overloaded-piece subject square for ${row.id}: $subject"))
    val subjectPiece = pieceAt(row.fen, subject).getOrElse(fail(s"Missing overloaded-piece subject on board for ${row.id}: $subject"))
    val boardWithoutDefender = position.board.discard(subjectSquare)
    controlledByAttackSquares(position, subjectSquare, subjectPiece).flatMap: target =>
      position.board.pieceAt(target).flatMap: targetPiece =>
        val enemyAttackers = boardWithoutDefender.attackers(target, !row.requiredColor).count
        val friendlyAttackers = boardWithoutDefender.attackers(target, row.requiredColor).count
        Option.when(
          targetPiece.color == row.requiredColor &&
            targetPiece.role != chess.King &&
            enemyAttackers > 0 &&
            friendlyAttackers < enemyAttackers
        ):
          target.key -> pieceAt(row.fen, target.key).getOrElse(fail(s"Missing overloaded-piece target char for ${row.id}: ${target.key}"))
    .sortBy(_._1)

  private def overloadedPieceFamily(piece: Char, rowId: String): String =
    piece.toUpper match
      case 'N' | 'B' => "minor"
      case 'R' => "rook"
      case 'Q' => "queen"
      case 'P' => "pawn"
      case other => fail(s"Unexpected overloaded-piece family for $rowId: $other")

  private def overloadedTargetMix(targets: Vector[(String, Char)]): String =
    val families = targets.map { case (_, piece) => overloadedTargetFamily(piece) }.sorted
    if targets.size == 1 then "one_target"
    else if targets.size >= 3 && families.forall(_ == "minor") then "three_minor"
    else
      families match
        case Vector("minor", "minor") => "minor_pair"
        case Vector("minor", "rook") => "minor_rook"
        case Vector("minor", "queen") => "minor_queen"
        case Vector("pawn", "minor") => "pawn_minor"
        case other => fail(s"Unexpected overloaded-piece target mix: $other from $targets")

  private def overloadedTargetFamily(piece: Char): String =
    piece.toUpper match
      case 'N' | 'B' => "minor"
      case 'R' => "rook"
      case 'Q' => "queen"
      case 'P' => "pawn"
      case other => fail(s"Unexpected overloaded-piece target family: $other")

  private case class XrayLine(sliderPiece: Char, sliderSquare: String, targetSquare: String, blockers: Vector[String], geometry: String)

  private def xrayTargetBucketKey(row: RootExpectationCorpus.Row): String =
    val polarity =
      row.polarityColor.getOrElse(fail(s"Missing polarityColor for ${row.id}"))
    row.caseType match
      case "exact" =>
        val target =
          actualSchemaSquares(row) match
            case Vector(square) =>
              assertEquals(row.trueSquares.toVector, Vector(square), s"Exact xray_target declaration drifted from extractor truth for ${row.id}")
              square
            case other => fail(s"Expected exactly one extracted xray target for ${row.id}: $other")
        val exactLines =
          xrayPositiveLines(row, polarity).filter(line => line.blockers.size == 1 && line.targetSquare == target)
        val allExactTargets =
          xrayPositiveLines(row, polarity).filter(_.blockers.size == 1).map(_.targetSquare).distinct
        assertEquals(allExactTargets, Vector(target), s"Exact xray_target row must contain exactly one extracted target for ${row.id}")
        val line =
          exactLines match
            case Vector(single) => single
            case other => fail(s"Expected exactly one x-ray carrier line for ${row.id}: $other")
        xrayBucket(polarity, line, xrayBlockerType(row.fen, polarity, line.blockers))
      case "near_miss" =>
        assertEquals(actualSchemaSquares(row), Vector.empty, s"Near-miss xray_target row extracted non-empty truth for ${row.id}")
        assert(row.trueSquares.isEmpty, s"Near-miss xray_target row should not declare true squares for ${row.id}")
        assert(
          xrayPositiveLines(row, polarity).forall(_.blockers.size != 1),
          s"Near-miss xray_target row still has a one-blocker x-ray target for ${row.id}"
        )
        xrayPositiveLines(row, polarity).filter(_.blockers.size == 2) match
          case Vector(line) => xrayBucket(polarity, line, "two_blockers")
          case Vector() =>
            val line = xrayTargetAbsentLine(row, polarity)
            xrayBucket(polarity, line, "target_absent")
          case other => fail(s"Expected at most one two-blocker xray_target near miss for ${row.id}: $other")
      case other => fail(s"Unexpected case type for ${row.id}: $other")

  private def xrayPositiveLines(row: RootExpectationCorpus.Row, polarity: String): Vector[XrayLine] =
    val targets = nonKingPieces(row.fen, opposite(polarity))
    friendlySliders(row.fen, polarity).flatMap: (sliderPiece, sliderSquare) =>
      targets.collect:
        case (_, targetSquare) if sliderUsesLine(sliderPiece, sliderSquare, targetSquare) =>
          val blockers = blockersBetween(row.fen, sliderSquare, targetSquare)
          XrayLine(
            sliderPiece = sliderPiece,
            sliderSquare = sliderSquare,
            targetSquare = targetSquare,
            blockers = blockers,
            geometry = lineGeometry(sliderSquare, targetSquare).getOrElse(fail(s"Missing x-ray line geometry for ${row.id}"))
          )

  private def xrayTargetAbsentLine(row: RootExpectationCorpus.Row, polarity: String): XrayLine =
    val candidates =
      friendlySliders(row.fen, polarity).flatMap: (sliderPiece, sliderSquare) =>
        xrayDirectionsFor(sliderPiece).flatMap: (fileStep, rankStep, geometry) =>
          val ray = raySquares(sliderSquare, fileStep, rankStep)
          val occupied = ray.zipWithIndex.filter { case (square, _) => pieceAt(row.fen, square).exists(piece => piece.toUpper != 'K') }
          occupied match
            case Vector((blocker, blockerIndex)) =>
              ray.drop(blockerIndex + 1).headOption.collect:
                case absentTarget if pieceAt(row.fen, absentTarget).isEmpty =>
                  XrayLine(sliderPiece, sliderSquare, absentTarget, Vector(blocker), geometry)
            case _ => None
    candidates match
      case Vector(line) => line
      case other => fail(s"Expected exactly one target-absent xray_target near miss line for ${row.id}: $other")

  private def friendlySliders(fen: String, polarity: String): Vector[(Char, String)] =
    nonKingPieces(fen, polarity).collect:
      case (piece, square) if "bBrRqQ".contains(piece) => piece -> square

  private def xrayBucket(polarity: String, line: XrayLine, blockerState: String): String =
    s"$polarity|${xraySliderFamily(line.sliderPiece)}|${line.geometry}|$blockerState"

  private def xrayBlockerType(fen: String, polarity: String, blockers: Vector[String]): String =
    blockers match
      case Vector(blocker) =>
        val blockerPiece = pieceAt(fen, blocker).getOrElse(fail(s"Missing x-ray blocker on $blocker"))
        if pieceColor(blockerPiece) == polarity then "own_piece" else "enemy_piece"
      case other => fail(s"Expected exactly one x-ray blocker, found $other")

  private def xraySliderFamily(piece: Char): String =
    piece.toUpper match
      case 'B' => "bishop"
      case 'R' => "rook"
      case 'Q' => "queen"
      case other => fail(s"Unexpected x-ray slider family: $other")

  private def xrayDirectionsFor(piece: Char): Vector[(Int, Int, String)] =
    val diagonals = Vector((1, 1, "diagonal"), (1, -1, "diagonal"), (-1, 1, "diagonal"), (-1, -1, "diagonal"))
    val lines = Vector((1, 0, "rank"), (-1, 0, "rank"), (0, 1, "file"), (0, -1, "file"))
    piece.toUpper match
      case 'B' => diagonals
      case 'R' => lines
      case 'Q' => lines ++ diagonals
      case other => fail(s"Unexpected x-ray slider direction family: $other")

  private def raySquares(from: String, fileStep: Int, rankStep: Int): Vector[String] =
    val fromFile = from.head - 'a'
    val fromRank = from.last.asDigit
    Iterator
      .iterate((fromFile + fileStep, fromRank + rankStep)) { (file, rank) => (file + fileStep, rank + rankStep) }
      .takeWhile { (file, rank) => 0 <= file && file < 8 && 1 <= rank && rank <= 8 }
      .map { (file, rank) => s"${('a' + file).toChar}$rank" }
      .toVector

  private def loosePieceBucketKey(row: RootExpectationCorpus.Row): String =
    val polarity =
      row.polarityColor.getOrElse(fail(s"Missing polarityColor for ${row.id}"))
    val subject =
      row.caseType match
        case "exact" =>
          actualSchemaSquares(row) match
            case Vector(square) =>
              assertEquals(row.trueSquares.toVector, Vector(square), s"Exact loose-piece declaration drifted from extractor truth for ${row.id}")
              square
            case other => fail(s"Expected exactly one extracted loose-piece square for ${row.id}: $other")
        case "near_miss" =>
          assertEquals(actualSchemaSquares(row), Vector.empty, s"Near-miss loose-piece row extracted non-empty truth for ${row.id}")
          assert(row.trueSquares.isEmpty, s"Near-miss loose-piece row should not declare true squares for ${row.id}")
          loosePieceSubjectSquare(row, polarity)
        case other => fail(s"Unexpected case type for ${row.id}: $other")

    assertLoosePieceRuntimeExchangeTruth(row, subject)

    val subjectPiece = pieceAt(row.fen, subject).getOrElse(fail(s"Missing loose-piece subject on board for ${row.id}"))
    val pieceFamily = loosePieceFamily(subjectPiece, row.id)
    val opponentAttackers = independentLegalAttackerKeys(row, subject, opposite(polarity))
    val friendlyDefenders = independentLegalRecapturerKeysAfterOpponentCapture(row, subject, polarity)
    assert(opponentAttackers.nonEmpty, s"Loose-piece row has no opponent attacker on $subject for ${row.id}")

    val exchangeState =
      row.caseType match
        case "exact" =>
          // Runtime extraction above proves positive local exchange loss; defender
          // presence only selects the matrix-owned exact exchange bucket.
          if friendlyDefenders.nonEmpty then "nominally_defended_but_losing" else "undefended"
        case "near_miss" =>
          assert(friendlyDefenders.nonEmpty, s"Near-miss loose-piece row must keep a same-color defender for ${row.id}")
          "stably_defended"
        case other => fail(s"Unexpected case type for ${row.id}: $other")

    s"$polarity|$pieceFamily|$exchangeState|${loosePieceMaterialRegime(row.fen)}"

  private def trappedPieceBucketKey(row: RootExpectationCorpus.Row): String =
    val polarity =
      row.polarityColor.getOrElse(fail(s"Missing polarityColor for ${row.id}"))
    val subject =
      row.caseType match
        case "exact" =>
          actualSchemaSquares(row) match
            case Vector(square) =>
              assertEquals(row.trueSquares.toVector, Vector(square), s"Exact trapped-piece declaration drifted from extractor truth for ${row.id}")
              square
            case other => fail(s"Expected exactly one extracted trapped-piece square for ${row.id}: $other")
        case "near_miss" | "nasty_negative" =>
          assertEquals(actualSchemaSquares(row), Vector.empty, s"Fail-closed trapped-piece row extracted non-empty truth for ${row.id}")
          assert(row.trueSquares.isEmpty, s"Fail-closed trapped-piece row should not declare true squares for ${row.id}")
          trappedPieceSubjectSquare(row, polarity)
        case other => fail(s"Unexpected case type for ${row.id}: $other")

    val position = Fen.read(variant.Standard, row.normalizedFen).getOrElse(fail(s"Invalid FEN for ${row.id}: ${row.normalizedFen}"))
    val square = chess.Square.fromKey(subject).getOrElse(fail(s"Invalid trapped-piece subject square for ${row.id}: $subject"))
    val subjectPiece = position.board.pieceAt(square).getOrElse(fail(s"Missing trapped-piece subject on board for ${row.id} at $subject"))
    val subjectColor = chessColor(polarity)
    assertEquals(subjectPiece.color, subjectColor, s"Trapped-piece subject has wrong polarity for ${row.id}")
    val pieceFamily = trappedPieceFamily(subjectPiece.role)

    row.caseType match
      case "exact" =>
        assert(subjectPiece.role != chess.King && subjectPiece.role != chess.Pawn, s"Exact trapped-piece row must be non-king non-pawn for ${row.id}")
        assert(
          independentLegalAttackers(position.board, square, !subjectColor).nonEmpty,
          s"Exact trapped-piece row must keep the subject currently attacked for ${row.id}"
        )
        val legalMoves = position.withColor(subjectColor).generateMovesAt(square).toVector
        val safeExits = trappedPieceSafeExitKeys(position, square, subjectColor)
        assert(safeExits.isEmpty, s"Exact trapped-piece row has safe exits for ${row.id}: $safeExits")
        val trapMode =
          if legalMoves.isEmpty then "zero_safe_moves" else "all_exits_lose"
        s"$polarity|$pieceFamily|$trapMode"
      case "near_miss" =>
        assert(subjectPiece.role != chess.King && subjectPiece.role != chess.Pawn, s"Near-miss trapped-piece row must be non-king non-pawn for ${row.id}")
        assert(
          independentLegalAttackers(position.board, square, !subjectColor).nonEmpty,
          s"Near-miss trapped-piece row must keep the subject currently attacked for ${row.id}"
        )
        val safeExits = trappedPieceSafeExitKeys(position, square, subjectColor)
        assertEquals(safeExits.size, 1, s"Near-miss trapped-piece row must have exactly one safe exit for ${row.id}: $safeExits")
        s"$polarity|$pieceFamily|one_exit_near_miss"
      case "nasty_negative" =>
        assert(Set(chess.King, chess.Pawn).contains(subjectPiece.role), s"Nasty trapped-piece negative must be excluded by king/pawn category for ${row.id}")
        assert(
          independentLegalAttackers(position.board, square, !subjectColor).nonEmpty,
          s"Nasty trapped-piece negative must keep the excluded piece under attack for ${row.id}"
        )
        s"$polarity|$pieceFamily|nasty_negative"
      case other => fail(s"Unexpected case type for ${row.id}: $other")

  private def trappedPieceSafeExitKeys(position: chess.Position, square: chess.Square, color: chess.Color): Vector[String] =
    position
      .withColor(color)
      .generateMovesAt(square)
      .toVector
      .filter(move => independentOpponentBestExchangeNet(move.after.board, move.dest, !color) <= 0)
      .map(_.dest.key)
      .distinct

  private def kingShelterHoleBucketKey(row: RootExpectationCorpus.Row): String =
    val polarity =
      row.polarityColor.getOrElse(fail(s"Missing polarityColor for ${row.id}"))
    val beneficiary = chessColor(polarity)
    val defender = !beneficiary
    val position = Fen.read(variant.Standard, row.normalizedFen).getOrElse(fail(s"Invalid FEN for ${row.id}: ${row.normalizedFen}"))
    val phase = kingShelterPhase(row.fen)

    row.caseType match
      case "exact" =>
        actualSchemaSquares(row) match
          case Vector(subject) =>
            assertEquals(row.trueSquares.toVector, Vector(subject), s"Exact king-shelter declaration drifted from extractor truth for ${row.id}")
            val square = chess.Square.fromKey(subject).getOrElse(fail(s"Invalid king-shelter square for ${row.id}: $subject"))
            assert(kingShelterHomeMask(position, defender).contains(square), s"Exact king-shelter row is outside home mask for ${row.id}")
            assert(!position.board.pieceAt(square).exists(piece => piece.color == defender && piece.role == chess.Pawn), s"Exact king-shelter row still has defender pawn cover for ${row.id}")
            assert(!kingShelterPawnControlled(position, defender, square), s"Exact king-shelter row is still pawn-controlled by defender for ${row.id}")
            assert(kingShelterPieceAttackOrAccess(position, beneficiary, square), s"Exact king-shelter row lacks attacker piece access for ${row.id}")
            s"$polarity|$phase|${kingShelterDefenderHome(polarity)}|${kingShelterHoleFile(subject)}|home_shelter_exact"
          case other => fail(s"Expected exactly one extracted king-shelter square for ${row.id}: $other")
      case "near_miss" =>
        assertEquals(actualSchemaSquares(row), Vector.empty, s"Near-miss king-shelter row extracted non-empty truth for ${row.id}")
        assert(row.trueSquares.isEmpty, s"Near-miss king-shelter row should not declare true squares for ${row.id}")
        val (subject, failClosedState) = kingShelterNearMissSubject(row, position, beneficiary, defender)
        s"$polarity|$phase|${kingShelterDefenderHome(polarity)}|${kingShelterHoleFile(subject)}|$failClosedState"
      case "nasty_negative" =>
        assertEquals(actualSchemaSquares(row), Vector.empty, s"Nasty king-shelter row extracted non-empty truth for ${row.id}")
        assert(row.trueSquares.isEmpty, s"Nasty king-shelter row should not declare true squares for ${row.id}")
        assert(kingShelterHomeMask(position, defender).isEmpty, s"Nasty king-shelter row must be outside the defender home-shelter regime for ${row.id}")
        s"$polarity|$phase|out_of_regime|out_of_regime|out_of_regime_negative"
      case other => fail(s"Unexpected king-shelter case type for ${row.id}: $other")

  private def kingShelterNearMissSubject(
      row: RootExpectationCorpus.Row,
      position: chess.Position,
      beneficiary: chess.Color,
      defender: chess.Color
  ): (String, String) =
    val mask = kingShelterHomeMask(position, defender)
    assert(mask.nonEmpty, s"Near-miss king-shelter row must remain in the defender home-shelter regime for ${row.id}")
    val pawnCovered =
      mask.filter: square =>
        kingShelterPieceAttackOrAccess(position, beneficiary, square) &&
          position.board.pieceAt(square).exists(piece => piece.color == defender && piece.role == chess.Pawn)
    if pawnCovered.nonEmpty then
      pawnCovered.toVector.map(_.key).distinct match
        case Vector(subject) => subject -> "pawn_cover_near_miss"
        case other => fail(s"Unable to derive unique pawn-cover king-shelter subject for ${row.id}: $other")
    else
      val noAccess =
        mask.filter: square =>
          !position.board.pieceAt(square).exists(piece => piece.color == defender && piece.role == chess.Pawn) &&
            !kingShelterPawnControlled(position, defender, square) &&
            !kingShelterPieceAttackOrAccess(position, beneficiary, square)
      noAccess.toVector.map(_.key).distinct match
        case Vector(subject) => subject -> "no_access_near_miss"
        case other => fail(s"Unable to derive unique no-access king-shelter subject for ${row.id}: $other")

  private def assertLoosePieceRuntimeExchangeTruth(row: RootExpectationCorpus.Row, subject: String): Unit =
    val extracted = actualSchemaSquares(row)
    val exchangeNet = independentLoosePieceExchangeNet(row, subject, opposite(row.requiredColor.name))
    row.caseType match
      case "exact" =>
        assertEquals(extracted, Vector(subject), s"Exact loose-piece row must be exchange-losing under the runtime extractor for ${row.id}")
        assert(exchangeNet > 0, s"Exact loose-piece row must have positive independent exchange loss for ${row.id}: net=$exchangeNet")
      case "near_miss" =>
        assertEquals(extracted, Vector.empty, s"Near-miss loose-piece row must fail closed under the runtime exchange extractor for ${row.id}")
        assert(exchangeNet <= 0, s"Near-miss loose-piece row must not have positive independent exchange loss for ${row.id}: net=$exchangeNet")
      case other => fail(s"Unexpected case type for ${row.id}: $other")

  private def independentLoosePieceExchangeNet(row: RootExpectationCorpus.Row, subject: String, attackerPolarity: String): Int =
    val position = Fen.read(variant.Standard, row.normalizedFen).getOrElse(fail(s"Invalid FEN for ${row.id}: ${row.normalizedFen}"))
    val square = chess.Square.fromKey(subject).getOrElse(fail(s"Invalid loose-piece subject square for ${row.id}: $subject"))
    independentOpponentBestExchangeNet(position.board, square, chessColor(attackerPolarity))

  private def independentLegalAttackerKeys(row: RootExpectationCorpus.Row, subject: String, attackerPolarity: String): Vector[String] =
    val position = Fen.read(variant.Standard, row.normalizedFen).getOrElse(fail(s"Invalid FEN for ${row.id}: ${row.normalizedFen}"))
    val square = chess.Square.fromKey(subject).getOrElse(fail(s"Invalid loose-piece subject square for ${row.id}: $subject"))
    independentLegalAttackers(position.board, square, chessColor(attackerPolarity)).map(_.key)

  private def independentLegalRecapturerKeysAfterOpponentCapture(row: RootExpectationCorpus.Row, subject: String, defenderPolarity: String): Vector[String] =
    val position = Fen.read(variant.Standard, row.normalizedFen).getOrElse(fail(s"Invalid FEN for ${row.id}: ${row.normalizedFen}"))
    val square = chess.Square.fromKey(subject).getOrElse(fail(s"Invalid loose-piece subject square for ${row.id}: $subject"))
    val attacker = chessColor(opposite(defenderPolarity))
    val defender = chessColor(defenderPolarity)
    independentLegalAttackers(position.board, square, attacker)
      .flatMap: origin =>
        position.board.taking(origin, square).toVector.flatMap: afterCapture =>
          independentLegalAttackers(afterCapture, square, defender).map(_.key)
      .distinct

  private def independentOpponentBestExchangeNet(boardState: chess.Board, square: chess.Square, attacker: chess.Color): Int =
    boardState.pieceAt(square).fold(0): occupant =>
      independentLegalAttackers(boardState, square, attacker)
        .map: origin =>
          val afterCapture = boardState.taking(origin, square).get
          roleValue(occupant.role) - independentOpponentBestExchangeNet(afterCapture, square, !attacker)
        .foldLeft(0)(math.max)

  private def independentLegalAttackers(boardState: chess.Board, square: chess.Square, attacker: chess.Color): Vector[chess.Square] =
    val attackerPosition = chess.Position(boardState, variant.Standard, attacker)
    boardState
      .attackers(square, attacker)
      .filter(origin => attackerPosition.generateMovesAt(origin).exists(_.dest == square))
      .toVector

  private def roleValue(role: chess.Role): Int = role match
    case chess.Pawn => 100
    case chess.Knight | chess.Bishop => 300
    case chess.Rook => 500
    case chess.Queen => 900
    case chess.King => 10_000

  private def chessColor(polarity: String): chess.Color =
    if polarity == "white" then chess.Color.White else chess.Color.Black

  private def backwardPawnSubjectSquare(row: RootExpectationCorpus.Row, polarity: String): String =
    val targetPawn = if polarity == "white" then 'P' else 'p'
    val candidates =
      boardPieces(row.fen).collect:
        case (piece, square) if piece == targetPawn => square
    val likelySubjects =
      candidates.filter: square =>
        forwardSquare(polarity, square).exists: front =>
          frontDeniedForBackwardPawn(row.fen, polarity, front) &&
            isHalfOpenForEnemy(row.fen, polarity, square.head)
    likelySubjects.distinct match
      case Vector(subject) => subject
      case _ if candidates.distinct.size == 1 => candidates.distinct.head
      case other => fail(s"Unable to derive unique backward-pawn subject for ${row.id}: $other")

  private def outpostSubjectSquare(row: RootExpectationCorpus.Row, polarity: String): String =
    val candidates =
      boardPieces(row.fen).collect:
        case (piece, square)
            if pieceColor(piece) == polarity &&
              !"PpKk".contains(piece) &&
              isInEnemyTerritory(polarity, square) =>
          square
    candidates.distinct match
      case Vector(subject) => subject
      case other => fail(s"Unable to derive unique outpost subject for ${row.id}: $other")

  private def candidatePasserSubjectSquare(row: RootExpectationCorpus.Row, polarity: String): String =
    val targetPawn = if polarity == "white" then 'P' else 'p'
    boardPieces(row.fen).collect:
      case (piece, square) if piece == targetPawn => square
    .distinct match
      case Vector(subject) => subject
      case other => fail(s"Unable to derive unique candidate-passer subject for ${row.id}: $other")

  private def outpostSupportShape(fen: String, polarity: String, subject: String, rowId: String): String =
    val directPawn = if polarity == "white" then 'P' else 'p'
    if pawnAttackSources(subject, polarity).exists(source => pieceAt(fen, source).contains(directPawn)) then "direct"
    else if hasStructuralPawnSupport(fen, polarity, subject) then "structural_path"
    else fail(s"Missing structural pawn support for outpost row $rowId at $subject")

  private def candidatePasserSupportShape(fen: String, polarity: String, subject: String, rowId: String): String =
    val supportPawns = candidatePasserSupportPawns(fen, polarity, subject)
    if supportPawns.exists(square => adjacentFileChars(subject.head).contains(square.head) && square.last == subject.last) then "same_rank_support"
    else if supportPawns.exists(square => isAheadOf(polarity, square, subject)) then "ahead_support"
    else fail(s"Missing candidate-passer support for $rowId at $subject")

  private def candidatePasserFrontIsEmpty(fen: String, polarity: String, subject: String): Boolean =
    forwardSquare(polarity, subject).exists(square => pieceAt(fen, square).isEmpty)

  private def candidatePasserSupportPawns(fen: String, polarity: String, subject: String): Vector[String] =
    val targetPawn = if polarity == "white" then 'P' else 'p'
    val adjacent = adjacentFileChars(subject.head)
    val relevantFiles = adjacent + subject.head
    boardPieces(fen).collect:
      case (piece, square)
          if piece == targetPawn &&
            square != subject &&
            relevantFiles.contains(square.head) &&
            (isAheadOf(polarity, square, subject) || (adjacent.contains(square.head) && square.last == subject.last)) =>
        square

  private def candidatePasserOppositionPawns(fen: String, polarity: String, subject: String): Vector[String] =
    val targetPawn = if polarity == "white" then 'p' else 'P'
    val relevantFiles = adjacentFileChars(subject.head) + subject.head
    boardPieces(fen).collect:
      case (piece, square)
          if piece == targetPawn &&
            relevantFiles.contains(square.head) &&
            isAheadOf(polarity, square, subject) =>
        square

  private def adjacentFileChars(file: Char): Set[Char] =
    Vector(file - 1, file + 1).collect:
      case value if 'a' <= value.toChar && value.toChar <= 'h' => value.toChar
    .toSet

  private def isAheadOf(polarity: String, candidate: String, reference: String): Boolean =
    if polarity == "white" then candidate.last.asDigit > reference.last.asDigit
    else candidate.last.asDigit < reference.last.asDigit

  private def pinnedPieceSubjectSquare(row: RootExpectationCorpus.Row, polarity: String): String =
    row.trueSquares.headOption.getOrElse:
      val candidates =
        nonKingPieces(row.fen, polarity).map(_._2).distinct
      candidates match
        case Vector(subject) => subject
        case other => fail(s"Unable to derive unique pinned-piece subject for ${row.id}: $other")

  private def loosePieceSubjectSquare(row: RootExpectationCorpus.Row, polarity: String): String =
    val candidates =
      nonKingPieces(row.fen, polarity).filter: (_, square) =>
        independentLegalAttackerKeys(row, square, opposite(polarity)).nonEmpty
    candidates.map(_._2).distinct match
      case Vector(subject) => subject
      case other => fail(s"Unable to derive unique loose-piece subject for ${row.id}: $other")

  private def trappedPieceSubjectSquare(row: RootExpectationCorpus.Row, polarity: String): String =
    val position = Fen.read(variant.Standard, row.normalizedFen).getOrElse(fail(s"Invalid FEN for ${row.id}: ${row.normalizedFen}"))
    val color = chessColor(polarity)
    val candidates =
      RootAtomRegistry.canonicalSquares.collect:
        case square if position.board.pieceAt(square).exists(_.color == color) =>
          val piece = position.board.pieceAt(square).get
          val attacked = independentLegalAttackers(position.board, square, !color).nonEmpty
          val categoryMatches =
            row.caseType match
              case "near_miss" => piece.role != chess.King && piece.role != chess.Pawn
              case "nasty_negative" => piece.role == chess.King || piece.role == chess.Pawn
              case other => fail(s"Unexpected fail-closed trapped-piece case type for ${row.id}: $other")
          Option.when(attacked && categoryMatches)(square.key)
    candidates.flatten.distinct match
      case Vector(subject) => subject
      case other => fail(s"Unable to derive unique trapped-piece subject for ${row.id}: $other")

  private def kingShelterPhase(fen: String): String =
    val nonKingNonPawnCount = boardPieces(fen).count { case (piece, _) => "nbrqNBRQ".contains(piece) }
    if nonKingNonPawnCount >= 2 then "middlegame" else "opening"

  private def kingShelterDefenderHome(polarity: String): String =
    if polarity == "white" then "black_home" else "white_home"

  private def kingShelterHoleFile(square: String): String =
    if Set('d', 'e').contains(square.head) then "center_shield" else "wing_shield"

  private def kingShelterHomeMask(position: chess.Position, defender: chess.Color): Vector[chess.Square] =
    position.board.kingPosOf(defender).toVector.flatMap: king =>
      val homeRegime =
        defender.fold(king.rank <= chess.Rank.Second, king.rank >= chess.Rank.Seventh)
      if !homeRegime then Vector.empty
      else
        (for
          rankStep <- Vector(1, 2)
          fileOffset <- Vector(-1, 0, 1)
          square <- chess.Square.at(
            king.file.value + fileOffset,
            king.rank.value + defender.fold(rankStep, -rankStep)
          )
        yield square).toVector

  private def kingShelterPawnControlled(position: chess.Position, defender: chess.Color, square: chess.Square): Boolean =
    square.pawnAttacks(!defender).exists: source =>
      position.board.pieceAt(source).exists(piece => piece.color == defender && piece.role == chess.Pawn)

  private def kingShelterPieceAttackOrAccess(position: chess.Position, beneficiary: chess.Color, square: chess.Square): Boolean =
    position.board.pieceAt(square).exists(piece => piece.color == beneficiary && piece.role != chess.Pawn && piece.role != chess.King) ||
      position.board.attackers(square, beneficiary).exists: source =>
        position.board.pieceAt(source).exists(piece => piece.color == beneficiary && piece.role != chess.Pawn && piece.role != chess.King)

  private def pinnedStateAndGeometry(fen: String, polarity: String, subject: String, rowId: String): (String, String) =
    absolutePinGeometry(fen, polarity, subject)
      .map("absolute" -> _)
      .orElse(relativePinGeometry(fen, polarity, subject).map("relative" -> _))
      .getOrElse(fail(s"Expected an exact pinned-piece line for $rowId at $subject"))

  private def absolutePinGeometry(fen: String, polarity: String, subject: String): Option[String] =
    kingSquare(fen, polarity).flatMap: king =>
      enemySliders(fen, polarity).collectFirst:
        case (sliderPiece, sliderSquare)
            if sliderUsesLine(sliderPiece, sliderSquare, subject) &&
              sliderUsesLine(sliderPiece, sliderSquare, king) &&
              blockersBetween(fen, sliderSquare, king) == Vector(subject) =>
          lineGeometry(sliderSquare, king).getOrElse(fail(s"Missing line geometry between $sliderSquare and $king"))

  private def relativePinGeometry(fen: String, polarity: String, subject: String): Option[String] =
    val subjectPiece = pieceAt(fen, subject).getOrElse(fail(s"Missing subject piece on board at $subject"))
    enemySliders(fen, polarity).iterator.flatMap: (sliderPiece, sliderSquare) =>
      moreValuableFriendlyAnchors(fen, polarity, subject, subjectPiece).collect:
        case anchor
            if sliderUsesLine(sliderPiece, sliderSquare, subject) &&
              sliderUsesLine(sliderPiece, sliderSquare, anchor) &&
              blockersBetween(fen, sliderSquare, anchor) == Vector(subject) =>
          lineGeometry(sliderSquare, anchor).getOrElse(fail(s"Missing line geometry between $sliderSquare and $anchor"))
    .toVector
    .distinct
    .ensuring(_.size <= 1, s"Expected at most one relative pin geometry for $subject")
    .headOption

  private def alignedEnemySliderGeometry(fen: String, polarity: String, subject: String, rowId: String): String =
    enemySliders(fen, polarity).collect:
      case (sliderPiece, sliderSquare) if sliderUsesLine(sliderPiece, sliderSquare, subject) =>
        lineGeometry(sliderSquare, subject).getOrElse(fail(s"Missing line geometry between $sliderSquare and $subject"))
    .distinct
    .ensuring(_.size == 1, s"Expected exactly one aligned enemy slider for $rowId at $subject")
    .head

  private def enemySliders(fen: String, polarity: String): Vector[(Char, String)] =
    nonKingPieces(fen, opposite(polarity)).collect:
      case (piece, square) if "bBrRqQ".contains(piece) => (piece, square)

  private def moreValuableFriendlyAnchors(fen: String, polarity: String, subject: String, subjectPiece: Char): Vector[String] =
    nonKingPieces(fen, polarity).collect:
      case (piece, square)
          if square != subject &&
            pieceValue(piece) > pieceValue(subjectPiece) =>
        square

  private def nonKingPieces(fen: String, polarity: String): Vector[(Char, String)] =
    boardPieces(fen).collect:
      case (piece, square) if pieceColor(piece) == polarity && !"Kk".contains(piece) => (piece, square)

  private def kingSquare(fen: String, polarity: String): Option[String] =
    val king = if polarity == "white" then 'K' else 'k'
    boardPieces(fen).collectFirst:
      case (piece, square) if piece == king => square

  private def pieceValue(piece: Char): Int =
    piece.toUpper match
      case 'P' => 1
      case 'N' => 3
      case 'B' => 3
      case 'R' => 5
      case 'Q' => 9
      case 'K' => 100
      case other => fail(s"Unexpected piece for value mapping: $other")

  private def pinnedPieceFamily(piece: Char, rowId: String): String =
    piece.toUpper match
      case 'N' | 'B' => "minor"
      case 'R' => "rook"
      case 'Q' => "queen"
      case other => fail(s"Unexpected pinned-piece family for $rowId: $other")

  private def loosePieceFamily(piece: Char, rowId: String): String =
    piece.toUpper match
      case 'P' => "pawn"
      case 'N' | 'B' => "minor"
      case 'R' => "rook"
      case 'Q' => "queen"
      case other => fail(s"Unexpected loose-piece family for $rowId: $other")

  private def trappedPieceFamily(role: chess.Role): String =
    role match
      case chess.Pawn => "pawn"
      case chess.Knight | chess.Bishop => "minor"
      case chess.Rook => "rook"
      case chess.Queen => "queen"
      case chess.King => "king"

  private def loosePieceMaterialRegime(fen: String): String =
    if boardPieces(fen).exists { case (piece, _) => "RrQq".contains(piece) } then "heavy"
    else "mixed"

  private def sliderUsesLine(piece: Char, from: String, to: String): Boolean =
    piece.toUpper match
      case 'B' => lineGeometry(from, to).contains("diagonal")
      case 'R' => lineGeometry(from, to).exists(geometry => geometry == "file" || geometry == "rank")
      case 'Q' => lineGeometry(from, to).nonEmpty
      case _ => false

  private def blockersBetween(fen: String, from: String, to: String): Vector[String] =
    squaresBetween(from, to).filter(square => pieceAt(fen, square).nonEmpty)

  private def squaresBetween(from: String, to: String): Vector[String] =
    lineGeometry(from, to) match
      case None => Vector.empty
      case Some(_) =>
        val fromFile = from.head - 'a'
        val toFile = to.head - 'a'
        val fromRank = from.last.asDigit
        val toRank = to.last.asDigit
        val fileStep = Integer.compare(toFile, fromFile)
        val rankStep = Integer.compare(toRank, fromRank)
        Iterator
          .iterate((fromFile + fileStep, fromRank + rankStep)) { (file, rank) => (file + fileStep, rank + rankStep) }
          .takeWhile(_ != (toFile, toRank))
          .map: (file, rank) =>
            s"${('a' + file).toChar}$rank"
          .toVector

  private def lineGeometry(from: String, to: String): Option[String] =
    val fileDelta = (from.head - to.head).abs
    val rankDelta = (from.last.asDigit - to.last.asDigit).abs
    if from.head == to.head then Some("file")
    else if from.last == to.last then Some("rank")
    else if fileDelta == rankDelta then Some("diagonal")
    else None

  private def frontDeniedForBackwardPawn(fen: String, polarity: String, front: String): Boolean =
    pieceAt(fen, front) match
      case Some(piece) => pieceColor(piece) != polarity
      case None =>
        pawnControlledSquares(fen, opposite(polarity)).contains(front) &&
          !pawnControlledSquares(fen, polarity).contains(front)

  private def enemyPawnCanChallengeSquare(fen: String, beneficiary: String, target: String): Boolean =
    val enemy = opposite(beneficiary)
    pawnAttackSources(target, enemy).exists: source =>
      pawnsOnFile(fen, enemy, source.head).exists(origin => canPawnStructurallyReach(fen, origin, source, enemy))

  private def hasStructuralPawnSupport(fen: String, polarity: String, target: String): Boolean =
    pawnAttackSources(target, polarity).exists: supportSquare =>
      pawnsOnFile(fen, polarity, supportSquare.head).exists(origin => canPawnStructurallyReach(fen, origin, supportSquare, polarity))

  private def pawnsOnFile(fen: String, polarity: String, file: Char): Vector[String] =
    val pawn = if polarity == "white" then 'P' else 'p'
    boardPieces(fen).collect:
      case (piece, square) if piece == pawn && square.head == file => square

  private def pawnsOf(fen: String, polarity: String): Vector[String] =
    val pawn = if polarity == "white" then 'P' else 'p'
    boardPieces(fen).collect:
      case (piece, square) if piece == pawn => square

  private def hasAdjacentFriendlyPawn(fen: String, polarity: String, square: String): Boolean =
    adjacentFileChars(square.head).exists(file => pawnsOnFile(fen, polarity, file).nonEmpty)

  private def pawnRankStage(polarity: String, square: String): String =
    if (polarity == "white" && square.last == '2') || (polarity == "black" && square.last == '7') then "home"
    else "advanced"

  private def actualFileMask(row: RootExpectationCorpus.Row): Vector[String] =
    val mask = extractedVector(row).fileMask8(row.schema, color = Some(row.requiredColor)).getOrElse(0)
    canonicalFiles.collect:
      case file if (mask & (1 << file.value)) != 0 => file.char.toString

  private def doubledFileStackShape(fen: String, polarity: String, file: Char): String =
    val ranks = pawnsOnFile(fen, polarity, file).map(_.last.asDigit).sorted
    assert(ranks.size >= 2, s"Doubled-file row has fewer than two pawns on $file")
    if ranks.sliding(2).exists(pair => pair.size == 2 && pair(1) == pair(0) + 1) then "adjacent"
    else "split"

  private def isFixedPawnSubject(fen: String, polarity: String, square: String): Boolean =
    pieceAt(fen, square).contains(if polarity == "white" then 'P' else 'p') &&
      forwardSquare(polarity, square).exists(pieceAt(fen, _).contains(if polarity == "white" then 'p' else 'P'))

  private def fixedPawnChainShape(fen: String, polarity: String, square: String): String =
    val hasAdjacentFixedPair =
      adjacentFileChars(square.head).exists: file =>
        pawnsOnFile(fen, polarity, file).exists(isFixedPawnSubject(fen, polarity, _))
    if hasAdjacentFixedPair then "mutual_chain" else "single_stop"

  private def leverPushAndContact(fen: String, polarity: String, subject: String, rowId: String): (String, String) =
    val singleArrival = forwardSquare(polarity, subject)
    val single =
      singleArrival.filter(square => pieceAt(fen, square).isEmpty).flatMap: arrival =>
        leverContactDirection(fen, polarity, arrival)
    single match
      case Some(direction) => "single" -> direction
      case None =>
        val homeRank = if polarity == "white" then '2' else '7'
        val double = for
          front <- singleArrival if subject.last == homeRank && pieceAt(fen, front).isEmpty
          arrival <- forwardSquare(polarity, front) if pieceAt(fen, arrival).isEmpty
          direction <- leverContactDirection(fen, polarity, arrival)
        yield "double" -> direction
        double.getOrElse(fail(s"Unable to derive lever push/contact bucket for $rowId at $subject"))

  private def leverContactDirection(fen: String, polarity: String, arrival: String): Option[String] =
    val enemyPawn = if polarity == "white" then 'p' else 'P'
    val targetRank = if polarity == "white" then arrival.last.asDigit + 1 else arrival.last.asDigit - 1
    Vector(arrival.head - 1, arrival.head + 1).flatMap:
      case fileCode if 'a' <= fileCode.toChar && fileCode.toChar <= 'h' && 1 <= targetRank && targetRank <= 8 =>
        val target = s"${fileCode.toChar}$targetRank"
        Option.when(pieceAt(fen, target).contains(enemyPawn)):
          if fileCode < arrival.head then "left" else "right"
      case _ => None
    .headOption

  private def fileTopology(file: Char): String =
    if Set('a', 'b', 'g', 'h').contains(file) then "edge" else "center"

  private def kingFileFamily(file: Char): String =
    if Set('a', 'h').contains(file) then "corner"
    else if Set('b', 'g').contains(file) then "wing"
    else "center"

  private def kingRingAdjacency(kingSquare: String, ringSquare: String): String =
    val fileDelta = (kingSquare.head - ringSquare.head).abs
    val rankDelta = (kingSquare.last.asDigit - ringSquare.last.asDigit).abs
    if fileDelta + rankDelta == 1 then "orthogonal"
    else if fileDelta == 1 && rankDelta == 1 then "diagonal"
    else fail(s"$ringSquare is not adjacent to king square $kingSquare")

  private def pawnAttackSources(target: String, attackerColor: String): Vector[String] =
    val file = target.head - 'a'
    val rank = target.last.asDigit
    val sourceRank = if attackerColor == "white" then rank - 1 else rank + 1
    Vector(file - 1, file + 1)
      .collect:
        case sourceFile if 0 <= sourceFile && sourceFile < 8 && 1 <= sourceRank && sourceRank <= 8 =>
          s"${('a' + sourceFile).toChar}$sourceRank"

  private def canPawnStructurallyReach(fen: String, origin: String, target: String, polarity: String): Boolean =
    if origin.head != target.head then false
    else
      val originRank = origin.last.asDigit
      val targetRank = target.last.asDigit
      val distance = if polarity == "white" then targetRank - originRank else originRank - targetRank
      if distance < 0 then false
      else if distance == 0 then pieceAt(fen, origin).contains(if polarity == "white" then 'P' else 'p')
      else
        pieceAt(fen, target).isEmpty &&
          squaresBetweenOnFile(origin, target).forall(square => pieceAt(fen, square).isEmpty)

  private def squaresBetweenOnFile(origin: String, target: String): Vector[String] =
    val file = origin.head
    val originRank = origin.last.asDigit
    val targetRank = target.last.asDigit
    val step = if targetRank > originRank then 1 else -1
    ((originRank + step) until targetRank by step).map(rank => s"$file$rank").toVector

  private def isHalfOpenForEnemy(fen: String, polarity: String, file: Char): Boolean =
    pawnsOnFile(fen, opposite(polarity), file).isEmpty &&
      pawnsOnFile(fen, polarity, file).nonEmpty

  private def pawnControlledSquares(fen: String, polarity: String): Set[String] =
    val pawn = if polarity == "white" then 'P' else 'p'
    boardPieces(fen).iterator.collect:
      case (piece, square) if piece == pawn =>
        val file = square.head - 'a'
        val rank = square.last.asDigit
        val targetRank = if polarity == "white" then rank + 1 else rank - 1
        Vector(file - 1, file + 1).collect:
          case targetFile if 0 <= targetFile && targetFile < 8 && 1 <= targetRank && targetRank <= 8 =>
            s"${('a' + targetFile).toChar}$targetRank"
    .flatten
    .toSet

  private def forwardSquare(polarity: String, square: String): Option[String] =
    val file = square.head
    val rank = square.last.asDigit
    val targetRank = if polarity == "white" then rank + 1 else rank - 1
    Option.when(1 <= targetRank && targetRank <= 8)(s"$file$targetRank")

  private def actualEnPassantState(row: RootExpectationCorpus.Row): String =
    extractedVector(row).activeIndicesForSchema(row.schema) match
      case Vector(index) =>
        RootAtomRegistry.enPassantState(index).getOrElse(fail(s"Unable to decode en-passant state for ${row.id} from $index"))
      case other => fail(s"Expected exactly one extracted en-passant state for ${row.id}: $other")

  private def actualSideToMoveState(row: RootExpectationCorpus.Row): String =
    extractedVector(row).activeIndicesForSchema(row.schema) match
      case Vector(index) =>
        val schema = RootAtomRegistry.requireSchema(SchemaId.SideToMove)
        val color = RootAtomRegistry.canonicalColors(index - schema.start)
        RootAtomRegistry.sideToMoveState(color)
      case other => fail(s"Expected exactly one extracted side-to-move state for ${row.id}: $other")

  private def actualCastlingRightsState(row: RootExpectationCorpus.Row): String =
    extractedVector(row).activeIndicesForSchema(row.schema) match
      case Vector(index) =>
        val schema = RootAtomRegistry.requireSchema(SchemaId.CastlingRights)
        RootAtomRegistry.castlingRightsState(index - schema.start)
      case other => fail(s"Expected exactly one extracted castling-rights state for ${row.id}: $other")

  private def castlingRightsFamily(state: String): String =
    state match
      case "-" => "none"
      case "KQkq" => "full_kqkq"
      case "K" | "k" => "single_king"
      case "Q" | "q" => "single_queen"
      case "KQ" | "kq" => "same_color_both"
      case other if other.exists(_.isUpper) && other.exists(_.isLower) => "cross_color"
      case other => fail(s"Unexpected castling-rights state family: $other")

  private def castlingRightsScope(state: String): String =
    state match
      case "-" => "none"
      case rights if rights.forall(_.isUpper) => "white"
      case rights if rights.forall(_.isLower) => "black"
      case _ => "mixed"

  private def sideToMoveBoardContext(fen: String): String =
    val nonKingPieceCount = boardPieces(fen).count { case (piece, _) => piece.toUpper != 'K' }
    if nonKingPieceCount == 0 then "sparse" else "opening"

  private def actualPieceOnSquares(row: RootExpectationCorpus.Row): Vector[String] =
    val mask = extractedVector(row)
      .squareMask64(row.schema, color = Some(row.requiredColor), role = Some(row.requiredRole))
      .getOrElse(0L)
    canonicalSquares.collect:
      case square if (mask & square.bl) != 0L => square.key

  private def actualOpenFiles(row: RootExpectationCorpus.Row): Vector[String] =
    val mask = extractedVector(row).fileMask8(row.schema).getOrElse(0)
    canonicalFiles.collect:
      case file if (mask & (1 << file.value)) != 0 => file.char.toString

  private def actualHalfOpenFiles(row: RootExpectationCorpus.Row): Vector[String] =
    val mask = extractedVector(row).fileMask8(row.schema, color = Some(row.requiredColor)).getOrElse(0)
    canonicalFiles.collect:
      case file if (mask & (1 << file.value)) != 0 => file.char.toString

  private def actualSchemaSquares(row: RootExpectationCorpus.Row): Vector[String] =
    actualSchemaSquares(row, row.schema)

  private def actualSchemaSquares(row: RootExpectationCorpus.Row, schemaId: String): Vector[String] =
    RootAtomRegistry.requireSchema(schemaId).family match
      case SchemaFamily.ColorSquare | SchemaFamily.ColorPawnSquare =>
        val mask = extractedVector(row).squareMask64(schemaId, color = Some(row.requiredColor)).getOrElse(0L)
        canonicalSquares.collect:
          case square if (mask & square.bl) != 0L => square.key
      case other => fail(s"actualSchemaSquares is only valid for color-square schemas, not $other")

  private def actualNeutralSchemaSquares(row: RootExpectationCorpus.Row): Vector[String] =
    RootAtomRegistry.requireSchema(row.schema).family match
      case SchemaFamily.NeutralSquare =>
        val mask = extractedVector(row).squareMask64(row.schema).getOrElse(0L)
        canonicalSquares.collect:
          case square if (mask & square.bl) != 0L => square.key
      case other => fail(s"actualNeutralSchemaSquares is only valid for neutral-square schemas, not $other")

  private def extractedVector(row: RootExpectationCorpus.Row): RootStateVector =
    RootExtractor.fromFen(row.normalizedFen).fold(message => fail(s"Failed to extract ${row.id}: $message"), identity)

  private def assertStoredSnapshotMatches(row: RootExpectationCorpus.Row): Unit =
    val vector = extractedVector(row)
    val actualMask = vector.squareMask64(row.schema, color = Some(row.requiredColor)).getOrElse(
      fail(s"Missing square mask for ${row.id}")
    )
    val expectedMask = row.expectedMask64.map(parseUnsignedLong).orElse(row.derivedSquareMask64).getOrElse(
      fail(s"Missing expected or derived mask for ${row.id}")
    )
    assertEquals(actualMask, expectedMask, s"Stored mask drifted for ${row.id}")
    val actualIndices =
      actualSchemaSquares(row).flatMap: squareKey =>
        chess.Square
          .fromKey(squareKey)
          .map(square => RootAtomRegistry.colorSquareIndex(row.schema, row.requiredColor, square))
    assertEquals(actualIndices, row.expectedGlobalIndices.toVector, s"Stored indices drifted for ${row.id}")

  private def parseUnsignedLong(hex: String): Long =
    java.lang.Long.parseUnsignedLong(hex.stripPrefix("0x"), 16)

  private def uniquePawnSquare(fen: String, polarity: String, rowId: String): String =
    val targetPawn = if polarity == "white" then 'P' else 'p'
    boardPieces(fen).collect:
      case (piece, square) if piece == targetPawn => square
    .distinct match
      case Vector(square) => square
      case other => fail(s"Expected exactly one $polarity pawn in $rowId, found $other")

  private def uniqueKingSquare(fen: String, polarity: String, rowId: String): String =
    val targetKing = if polarity == "white" then 'K' else 'k'
    boardPieces(fen).collect:
      case (piece, square) if piece == targetKing => square
    .distinct match
      case Vector(square) => square
      case other => fail(s"Expected exactly one $polarity king in $rowId, found $other")

  private def pieceAt(fen: String, square: String): Option[Char] =
    boardPieces(fen).collectFirst:
      case (piece, `square`) => piece

  private def pieceColor(piece: Char): String =
    if piece.isUpper then "white" else "black"

  private def opposite(polarity: String): String =
    if polarity == "white" then "black" else "white"

  private def isInEnemyTerritory(polarity: String, square: String): Boolean =
    val rank = square.last.asDigit
    if polarity == "white" then rank >= 5 else rank <= 4

  private def boardPieces(fen: String): Vector[(Char, String)] =
    fen
      .split(' ')
      .head
      .split('/')
      .zipWithIndex
      .iterator
      .flatMap: (rankText, rankIndex) =>
        val expanded = rankText.flatMap(ch => if ch.isDigit then "." * ch.asDigit else ch.toString)
        expanded.zipWithIndex.collect:
          case (piece, fileIndex) if piece != '.' => (piece, s"${('a' + fileIndex).toChar}${8 - rankIndex}")
      .toVector
