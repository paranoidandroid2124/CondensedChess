package lila.commentary.claim

import lila.commentary.CommentaryCore
import lila.commentary.api.{ CommentaryApiJson, CommentaryBackendSeam, CommentaryRequest, CommentaryResponseStatus }
import lila.commentary.certification.CertificationEngineRuntimeIntake
import lila.commentary.render.RenderStatus
import lila.commentary.selection.{ ClaimLayer, EvidenceRefKind }

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

  test("valid transition produces bounded last-move delta claims"):
    val response = CommentaryBackendSeam.render(
      request(
        currentFen = afterE4Fen,
        beforeFen = Some(startingFen),
        playedMove = Some("e2e4")
      )
    )
    val evidenceIds = response.render.evidenceRefs.map(_.id).toSet

    assertEquals(response.status, CommentaryResponseStatus.Rendered)
    assert(evidenceIds.contains("last_move_transition"))
    assert(response.render.evidenceRefs.exists(_.kind == EvidenceRefKind.Delta))

  test("valid capture transition produces bounded capture and pawn-transition delta claims"):
    val response = CommentaryBackendSeam.render(
      request(
        currentFen = afterCaptureFen,
        beforeFen = Some(beforeCaptureFen),
        playedMove = Some("e4d5")
      )
    )
    val evidenceIds = response.render.evidenceRefs.map(_.id).toSet

    assertEquals(response.status, CommentaryResponseStatus.Rendered)
    assert(evidenceIds.contains("capture_transition"))
    assert(evidenceIds.contains("pawn_structure_transition"))

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
