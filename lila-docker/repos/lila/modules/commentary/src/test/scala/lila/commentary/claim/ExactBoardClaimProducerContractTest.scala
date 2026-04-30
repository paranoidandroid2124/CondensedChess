package lila.commentary.claim

import lila.commentary.CommentaryCore
import lila.commentary.api.{ CommentaryApiJson, CommentaryBackendSeam, CommentaryRequest, CommentaryResponseStatus }
import lila.commentary.certification.CertificationEngineRuntimeIntake
import lila.commentary.render.RenderStatus
import lila.commentary.selection.{ ClaimLayer, ClaimStatus, EvidenceRefKind }
import chess.format.Fen

class ExactBoardClaimProducerContractTest extends munit.FunSuite:

  test("current FEN produces bounded material and piece-inventory object claims"):
    val response = CommentaryBackendSeam.render(request(currentFen = startingFen))
    val evidenceIds = response.render.evidenceRefs.map(_.id).toSet

    assertEquals(response.status, CommentaryResponseStatus.Rendered)
    assertEquals(response.render.status, RenderStatus.Rendered)
    assert(evidenceIds.contains("piece_inventory"))
    assert(response.render.evidenceRefs.exists(_.kind == EvidenceRefKind.Object))
    assert(response.render.blocks.forall(_.text.publicText.forall(text => !forbiddenWords.exists(text.toLowerCase.contains))))

  test("current FEN produces immediate king check object claims only when exact"):
    val checked = CommentaryBackendSeam.render(request(currentFen = blackInCheckFen))
    val quiet = CommentaryBackendSeam.render(request(currentFen = bareKingsFen))

    assert(checked.render.evidenceRefs.exists(ref => ref.kind == EvidenceRefKind.Object && ref.id == "immediate_check"))
    assert(!quiet.render.evidenceRefs.exists(_.id == "immediate_check"))

  test("current FEN produces mate-in-one object claims only from exact legal mate"):
    val response = CommentaryBackendSeam.render(request(currentFen = mateInOneFen))

    assertEquals(response.status, CommentaryResponseStatus.Rendered)
    assert(response.render.evidenceRefs.exists(ref =>
      ref.kind == EvidenceRefKind.Object &&
        ref.id == "immediate_mate" &&
        ref.owner.contains("white") &&
        ref.anchor.contains("a8")
    ))
    assert(response.render.blocks.exists(_.evidenceIds.contains("immediate_mate")))

  test("current FEN keeps generic tactical liability roots out of public lead claims"):
    val loose = CommentaryBackendSeam.render(request(currentFen = looseWhiteKnightFen))
    val pinned = CommentaryBackendSeam.render(request(currentFen = pinnedWhiteBishopFen))
    val looseClaims = currentClaims(looseWhiteKnightFen)
    val pinnedClaims = currentClaims(pinnedWhiteBishopFen)

    assert(looseClaims.exists(claim =>
      claim.status == ClaimStatus.SupportOnly &&
        claim.evidenceRefs.exists(ref =>
          ref.kind == EvidenceRefKind.Root &&
            ref.id == "loose_piece" &&
            ref.owner.contains("black") &&
            ref.anchor.contains("d4")
        )
    ))
    assert(!loose.render.evidenceRefs.exists(ref => ref.id == "loose_piece" && ref.route.contains("tactical_liability")))
    assert(pinnedClaims.exists(claim =>
      claim.status == ClaimStatus.SupportOnly &&
        claim.evidenceRefs.exists(ref =>
          ref.kind == EvidenceRefKind.Root &&
            ref.id == "pinned_piece" &&
            ref.owner.contains("black") &&
            ref.anchor.contains("e2")
        )
    ))
    assert(!pinned.render.evidenceRefs.exists(ref => ref.id == "pinned_piece" && ref.route.contains("tactical_liability")))

  test("current FEN exposes immediate legal capture separately from loose piece smell"):
    val response = CommentaryBackendSeam.render(request(currentFen = rookCaptureFen))

    assert(response.render.evidenceRefs.exists(ref =>
      ref.kind == EvidenceRefKind.Object &&
        ref.id == "immediate_capture" &&
        ref.owner.contains("white") &&
        ref.anchor.contains("h8") &&
        ref.route.contains("immediate_capture") &&
        ref.scope.contains("position_local")
    ))
    assert(response.render.blocks.exists(_.evidenceIds.contains("immediate_capture")))
    assert(!response.render.evidenceRefs.exists(ref =>
      ref.id == "loose_piece" &&
        ref.route.contains("tactical_liability") &&
        ref.owner.contains("black") &&
        ref.anchor.contains("h1")
    ))

  test("current FEN does not expose xray targets from the starting position"):
    val response = CommentaryBackendSeam.render(request(currentFen = startingFen))

    assert(!response.render.evidenceRefs.exists(_.id == "xray_target"))

  test("standing tactical roots stay support-only in a transition request"):
    val response =
      CommentaryBackendSeam.render(
        request(
          currentFen = tacticalLiabilityAfterKf1Fen,
          beforeFen = Some(tacticalLiabilityBeforeKf1Fen),
          playedMove = Some("e1f1")
        )
    )

    assertEquals(response.status, CommentaryResponseStatus.Rendered)
    val claims = exactTransitionClaims(tacticalLiabilityAfterKf1Fen, tacticalLiabilityBeforeKf1Fen, "e1f1")
    val tacticalLiabilityClaims = claims.filter(_.route.contains("tactical_liability"))

    assert(tacticalLiabilityClaims.nonEmpty)
    assert(tacticalLiabilityClaims.forall(_.status == ClaimStatus.SupportOnly), clues(tacticalLiabilityClaims))
    assert(!response.render.evidenceRefs.exists(ref => ref.route.contains("tactical_liability")))

  test("moved piece capturable by xray only stays a standing tactical anti-case"):
    val response =
      CommentaryBackendSeam.render(
        request(
          currentFen = movedQueenLooseAfterFen,
          beforeFen = Some(movedQueenLooseBeforeFen),
          playedMove = Some("e5f4")
        )
      )

    assertEquals(response.status, CommentaryResponseStatus.Rendered)
    val claims = exactTransitionClaims(movedQueenLooseAfterFen, movedQueenLooseBeforeFen, "e5f4")
    assert(claims.exists(claim =>
      claim.status == ClaimStatus.SupportOnly &&
        claim.evidenceRefs.exists(ref =>
          ref.id == "xray_target" &&
            ref.owner.contains("white") &&
            ref.anchor.contains("f4") &&
            ref.scope.contains("position_local")
        )
    ))
    assert(!response.render.evidenceRefs.exists(ref =>
      ref.id == "xray_target" &&
        ref.owner.contains("white") &&
        ref.anchor.contains("f4") &&
        ref.scope.contains("position_local")
    ))
    assert(!response.render.evidenceRefs.exists(ref =>
      ref.id == "loose_piece" &&
        ref.owner.contains("white") &&
        ref.anchor.contains("f4") &&
        ref.scope.contains("move_local")
    ))

  test("moved piece left loose and capturable becomes move-local tactical reason"):
    val response =
      CommentaryBackendSeam.render(
        request(
          currentFen = movedKnightLooseAfterFen,
          beforeFen = Some(movedKnightLooseBeforeFen),
          playedMove = Some("f5d4")
        )
      )

    assertEquals(response.status, CommentaryResponseStatus.Rendered)
    assert(response.render.evidenceRefs.exists(ref =>
      ref.id == "loose_piece" &&
        ref.owner.contains("white") &&
        ref.anchor.contains("d4") &&
        ref.route.contains("moved_piece_left_loose") &&
        ref.scope.contains("move_local")
    ))
    val claim = exactTransitionClaims(movedKnightLooseAfterFen, movedKnightLooseBeforeFen, "f5d4")
      .find(_.route.contains("moved_piece_left_loose"))
      .getOrElse(fail("missing moved-piece loose claim"))
    assert(claim.evidenceRefs.exists(ref =>
      ref.kind == EvidenceRefKind.Delta &&
        ref.id == "moved_piece_left_loose_transition" &&
        ref.owner.contains("white") &&
        ref.anchor.contains("d4") &&
        ref.route.contains("moved_piece_left_loose") &&
        ref.scope.contains("move_local")
    ))
    assert(claim.lowerCarrierRefs.exists(ref =>
      ref.kind == EvidenceRefKind.Root &&
        ref.id == "loose_piece" &&
        ref.owner.contains("white") &&
        ref.anchor.contains("d4") &&
        ref.route.contains("moved_piece_left_loose") &&
        ref.scope.contains("move_local")
    ))
    assert(claim.lowerCarrierRefs.exists(ref =>
      ref.kind == EvidenceRefKind.Object &&
        ref.id == "immediate_capture" &&
        ref.owner.contains("white") &&
        ref.anchor.contains("d4") &&
        ref.route.contains("moved_piece_left_loose") &&
        ref.scope.contains("move_local")
    ))
    assert(response.render.blocks.exists(_.evidenceIds.contains("loose_piece")))

  test("already-loose moved piece does not become a new move-local loose reason"):
    val response =
      CommentaryBackendSeam.render(
        request(
          currentFen = alreadyLooseKnightAfterFen,
          beforeFen = Some(alreadyLooseKnightBeforeFen),
          playedMove = Some("f5d4")
        )
      )

    assertEquals(response.status, CommentaryResponseStatus.Rendered)
    assert(!response.render.evidenceRefs.exists(ref =>
      ref.id == "moved_piece_left_loose_transition" ||
        (ref.id == "loose_piece" &&
          ref.owner.contains("white") &&
          ref.anchor.contains("d4") &&
          ref.route.contains("moved_piece_left_loose") &&
          ref.scope.contains("move_local"))
    ))

  test("moved pawn left loose does not become move-local tactical reason"):
    val response =
      CommentaryBackendSeam.render(
        request(
          currentFen = movedPawnLooseAfterFen,
          beforeFen = Some(movedPawnLooseBeforeFen),
          playedMove = Some("e4e5")
        )
      )

    assertEquals(response.status, CommentaryResponseStatus.Rendered)
    assert(!response.render.evidenceRefs.exists(ref =>
      ref.id == "loose_piece" &&
        ref.anchor.contains("e5") &&
        ref.route.contains("moved_piece_left_loose") &&
        ref.scope.contains("move_local")
    ))

  test("legal non-slider royal fork after the move becomes move-local tactical reason"):
    val response =
      CommentaryBackendSeam.render(
        request(
          currentFen = nonSliderRoyalForkAfterFen,
          beforeFen = Some(nonSliderRoyalForkBeforeFen),
          playedMove = Some("f1e1")
        )
      )

    assertEquals(response.status, CommentaryResponseStatus.Rendered)
    assert(response.render.evidenceRefs.exists(ref =>
      ref.id == "royal_fork" &&
        ref.owner.contains("black") &&
        ref.anchor.contains("a1") &&
        ref.route.contains("non_slider_royal_fork") &&
        ref.scope.contains("move_local")
    ))
    assert(response.render.blocks.exists(_.evidenceIds.contains("royal_fork")))

  test("pre-existing non-slider royal fork does not become a move-local tactical reason"):
    val response =
      CommentaryBackendSeam.render(
        request(
          currentFen = preExistingRoyalForkAfterFen,
          beforeFen = Some(preExistingRoyalForkBeforeFen),
          playedMove = Some("h2h3")
        )
      )

    assert(!response.render.evidenceRefs.exists(_.id == "royal_fork"))
    assert(!response.render.evidenceRefs.exists(ref =>
      ref.id == "royal_fork" ||
        (ref.route.contains("non_slider_royal_fork") && ref.scope.contains("move_local"))
    ))

  test("non-slider royal fork is not emitted without exact transition context"):
    val response = CommentaryBackendSeam.render(request(currentFen = nonSliderRoyalForkAfterFen))

    assert(!response.render.evidenceRefs.exists(_.id == "royal_fork"))

  private def exactTransitionClaims(
      currentFen: String,
      beforeFen: String,
      playedMove: String
  ) =
    val currentExtraction =
      CommentaryCore.extractStrategicObjectsFailClosed(Fen.Full.clean(currentFen)).fold(fail(_), identity)
    val deltaExtraction =
      CommentaryCore.extractStrategicDeltasFromFensFailClosed(beforeFen, playedMove, currentFen).fold(fail(_), identity)
    ExactBoardClaimProducer.produce(currentExtraction, Some(deltaExtraction))

  private def currentClaims(currentFen: String) =
    val currentExtraction =
      CommentaryCore.extractStrategicObjectsFailClosed(Fen.Full.clean(currentFen)).fold(fail(_), identity)
    ExactBoardClaimProducer.produce(currentExtraction, None)

  test("valid transition produces bounded last-move delta claims"):
    val response = CommentaryBackendSeam.render(
      request(
        currentFen = afterE4Fen,
        beforeFen = Some(startingFen),
        playedMove = Some("e2e4")
      )
    )
    val claims = exactTransitionClaims(afterE4Fen, startingFen, "e2e4")

    assertEquals(response.status, CommentaryResponseStatus.Rendered)
    assert(!response.render.evidenceRefs.exists(ref =>
      ref.id == "last_move_transition" || ref.id == "pawn_structure_transition"
    ))
    assert(claims.exists(claim => claim.status == ClaimStatus.SupportOnly && claim.route.contains("last_move_transition")))
    assert(claims.exists(claim => claim.status == ClaimStatus.SupportOnly && claim.route.contains("pawn_structure_transition")))

  test("valid capture transition produces bounded capture and pawn-transition delta claims"):
    val response = CommentaryBackendSeam.render(
      request(
        currentFen = afterCaptureFen,
        beforeFen = Some(beforeCaptureFen),
        playedMove = Some("e4d5")
      )
    )
    val evidenceIds = response.render.evidenceRefs.map(_.id).toSet
    val claims = exactTransitionClaims(afterCaptureFen, beforeCaptureFen, "e4d5")

    assertEquals(response.status, CommentaryResponseStatus.Rendered)
    assert(evidenceIds.contains("capture_transition"))
    assert(!evidenceIds.contains("pawn_structure_transition"))
    assert(claims.exists(claim => claim.status == ClaimStatus.SupportOnly && claim.route.contains("pawn_structure_transition")))

  test("missing transition pair produces no delta claim"):
    val noTransition = CommentaryBackendSeam.render(request(currentFen = afterE4Fen))
    val invalidPair = CommentaryBackendSeam.render(request(currentFen = afterE4Fen, playedMove = Some("e2e4")))

    assert(!noTransition.render.evidenceRefs.exists(_.kind == EvidenceRefKind.Delta))
    assertEquals(invalidPair.status, CommentaryResponseStatus.InvalidRequest)
    assertEquals(invalidPair.render.status, RenderStatus.NoCommentary)

  test("illegal or non-matching transition produces no delta claim"):
    val response = CommentaryBackendSeam.render(
      request(
        currentFen = startingFen,
        beforeFen = Some(startingFen),
        playedMove = Some("e2e5")
      )
    )

    assertEquals(response.status, CommentaryResponseStatus.InvalidRequest)
    assertEquals(response.render.status, RenderStatus.NoCommentary)
    assert(!response.render.evidenceRefs.exists(_.kind == EvidenceRefKind.Delta))

  test("full-FEN stale transition clock mismatch produces no delta claim"):
    val staleAfterClockFen = "rnbqkbnr/pppppppp/8/8/4P3/8/PPPP1PPP/RNBQKBNR b KQkq e3 5 7"
    val response = CommentaryBackendSeam.render(
      request(
        currentFen = staleAfterClockFen,
        beforeFen = Some(startingFen),
        playedMove = Some("e2e4")
      )
    )

    assertEquals(response.status, CommentaryResponseStatus.InvalidRequest)
    assertEquals(response.render.status, RenderStatus.NoCommentary)
    assert(!response.render.evidenceRefs.exists(_.kind == EvidenceRefKind.Delta))

  test("producer suppresses delta claims when delta after-state differs from current extraction"):
    val currentExtraction =
      CommentaryCore.extractStrategicObjectsFromFenFailClosed(bareKingsFen).fold(fail(_), identity)
    val staleDelta =
      CommentaryCore.extractStrategicDeltasFromFensFailClosed(startingFen, "e2e4", afterE4Fen).fold(fail(_), identity)

    val claims = ExactBoardClaimProducer.produce(currentExtraction, Some(staleDelta))

    assert(!claims.exists(_.layer == ClaimLayer.Delta))

  test("engine packet alone does not create a claim"):
    val response = CommentaryBackendSeam.render(
      request(
        currentFen = bareKingsFen,
        enginePacket = Some(emptyEnginePacket(bareKingsFen))
      )
    )

    assertEquals(response.status, CommentaryResponseStatus.NoCommentary)
    assertEquals(response.render.status, RenderStatus.NoCommentary)
    assertEquals(response.render.evidenceRefs, Vector.empty)

  test("source context alone does not create a claim"):
    val response = CommentaryBackendSeam.render(request(currentFen = bareKingsFen))

    assertEquals(response.status, CommentaryResponseStatus.NoCommentary)
    assertEquals(response.render.status, RenderStatus.NoCommentary)
    assertEquals(response.render.evidenceRefs, Vector.empty)

  test("no safe exact-board facts keeps noCommentary behavior"):
    val response = CommentaryBackendSeam.render(request(currentFen = bareKingsFen))

    assertEquals(response.status, CommentaryResponseStatus.NoCommentary)
    assertEquals(response.render.status, RenderStatus.NoCommentary)
    assertEquals(response.render.blocks, Vector.empty)

  test("public backend response does not leak extraction or internal packets"):
    val response = CommentaryBackendSeam.renderDebug(request(currentFen = startingFen, enginePacket = Some(emptyEnginePacket(startingFen))))
    import CommentaryApiJson.given
    val json = play.api.libs.json.Json.toJson(response).toString

    assertEquals(response.render.status, RenderStatus.Rendered)
    assert(!json.contains("currentExtraction"))
    assert(!json.contains("rootState"))
    assert(!json.contains("primaryWitnesses"))
    assert(!json.contains("attachedWitnesses"))
    assert(!json.contains("objects"))
    assert(!json.contains("pieceMap"))
    assert(!json.contains("pvLines"))
    assert(!json.contains("engineConfigFingerprint"))

  private val forbiddenWords: Vector[String] =
    Vector("best", "forced", "winning", "drawn", "result", "oracle", "theory", "recommend")

  private val startingFen = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1"
  private val afterE4Fen = "rnbqkbnr/pppppppp/8/8/4P3/8/PPPP1PPP/RNBQKBNR b KQkq e3 0 1"
  private val blackInCheckFen = "4k3/8/8/8/8/8/8/K3R3 b - - 0 1"
  private val bareKingsFen = "8/8/8/8/8/8/4k3/7K w - - 0 1"
  private val mateInOneFen = "k7/8/K7/8/8/8/8/1Q6 w - - 0 1"
  private val looseWhiteKnightFen = "4k3/6b1/8/8/3N4/8/8/4K3 w - - 0 1"
  private val pinnedWhiteBishopFen = "4r2k/8/8/8/8/8/4B3/4K3 w - - 0 1"
  private val rookCaptureFen = "4k2r/8/8/8/8/8/8/4K2R w Kk - 0 1"
  private val tacticalLiabilityBeforeKf1Fen = "3qk3/3p4/8/8/8/8/8/3RK3 w - - 0 1"
  private val tacticalLiabilityAfterKf1Fen = "3qk3/3p4/8/8/8/8/8/3R1K2 b - - 1 1"
  private val movedQueenLooseBeforeFen = "r4rk1/1pp3p1/1pnpb2p/4q3/2P5/1N6/PP1QBPPP/4RRK1 b - - 1 17"
  private val movedQueenLooseAfterFen = "r4rk1/1pp3p1/1pnpb2p/8/2P2q2/1N6/PP1QBPPP/4RRK1 w - - 2 18"
  private val movedKnightLooseBeforeFen = "4k3/8/8/5n2/8/8/8/3QK3 b - - 0 1"
  private val movedKnightLooseAfterFen = "4k3/8/8/8/3n4/8/8/3QK3 w - - 1 2"
  private val alreadyLooseKnightBeforeFen = "4k3/8/8/5n2/8/8/8/3QKR2 b - - 0 1"
  private val alreadyLooseKnightAfterFen = "4k3/8/8/8/3n4/8/8/3QKR2 w - - 1 2"
  private val movedPawnLooseBeforeFen = "k3r3/8/8/8/4P3/8/8/7K w - - 0 1"
  private val movedPawnLooseAfterFen = "k3r3/8/8/4P3/8/8/8/7K b - - 0 1"
  private val nonSliderRoyalForkBeforeFen = "7k/8/8/8/1n6/8/8/R4K2 w - - 0 1"
  private val nonSliderRoyalForkAfterFen = "7k/8/8/8/1n6/8/8/R3K3 b - - 1 1"
  private val preExistingRoyalForkBeforeFen = "7k/8/8/8/1n6/8/7P/R3K3 w - - 0 1"
  private val preExistingRoyalForkAfterFen = "7k/8/8/8/1n6/7P/8/R3K3 b - - 0 1"
  private val beforeCaptureFen = "4k3/8/8/3p4/4P3/8/8/4K3 w - - 0 1"
  private val afterCaptureFen = "4k3/8/8/3P4/8/8/8/4K3 b - - 0 1"

  private def request(
      currentFen: String,
      beforeFen: Option[String] = None,
      playedMove: Option[String] = None,
      enginePacket: Option[CertificationEngineRuntimeIntake.RuntimeEnginePacket] = None
  ): CommentaryRequest =
    CommentaryRequest(
      currentFen = currentFen,
      beforeFen = beforeFen,
      playedMove = playedMove,
      nodeId = "producer-node",
      ply = 0,
      enginePacket = enginePacket
    )

  private def emptyEnginePacket(fen: String): CertificationEngineRuntimeIntake.RuntimeEnginePacket =
    CertificationEngineRuntimeIntake.RuntimeEnginePacket(
      fen = fen,
      nodeId = "producer-node",
      ply = 0,
      requestedDepth = 18,
      realizedDepth = 18,
      multiPv = 1,
      completed = true,
      generatedAtEpochMs = 0L,
      maxAgeMs = 60_000L,
      engineConfigFingerprint = "producer-empty-engine",
      score = CertificationEngineRuntimeIntake.RuntimeScore.Centipawns(0),
      scorePerspective = CertificationEngineRuntimeIntake.RuntimeScorePerspective.White,
      pvLines = Vector.empty,
      claims = Vector.empty
    )
