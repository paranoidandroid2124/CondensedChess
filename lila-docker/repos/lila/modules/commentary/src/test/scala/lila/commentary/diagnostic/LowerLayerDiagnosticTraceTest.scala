package lila.commentary.diagnostic

class LowerLayerDiagnosticTraceTest extends munit.FunSuite:

  test("trace reports invalid input before extraction or renderer stages"):
    val trace = LowerLayerDiagnostic.trace(
      LowerLayerDiagnostic.Input(
        id = "invalid-fen",
        currentFen = "not a fen",
        beforeFen = None,
        playedMove = None,
        nodeId = "diagnostic-node",
        ply = 0
      )
    )

    assertEquals(trace.input.status, "invalid")
    assertEquals(trace.identity.nodeId, "diagnostic-node")
    assertEquals(trace.identity.ply, 0)
    assert(trace.breaks.contains("input:invalid_current_fen"))
    assertEquals(trace.extraction.rootFacts, Vector.empty)
    assertEquals(trace.claims.produced, Vector.empty)
    assertEquals(trace.selection.lead, None)
    assertEquals(trace.preRendererVerdict, "input_invalid")

  test("trace separates missing transition identity from lower admission gaps"):
    val trace = LowerLayerDiagnostic.trace(
      LowerLayerDiagnostic.Input(
        id = "missing-played-move",
        currentFen = afterE4Fen,
        beforeFen = Some(startingFen),
        playedMove = None,
        nodeId = "diagnostic-node",
        ply = 0
      )
    )

    assertEquals(trace.input.status, "valid")
    assertEquals(trace.transition.status, "missing_pair")
    assert(trace.breaks.contains("transition:missing_pair"))
    assertEquals(trace.preRendererVerdict, "transition_invalid")

  test("trace rejects stale full-FEN transition clocks before lower admission"):
    val trace = LowerLayerDiagnostic.trace(
      LowerLayerDiagnostic.Input(
        id = "stale-clock",
        currentFen = afterE4FenWithWrongClock,
        beforeFen = Some(startingFen),
        playedMove = Some("e2e4"),
        nodeId = "diagnostic-node",
        ply = 12
      )
    )

    assertEquals(trace.input.status, "valid")
    assertEquals(trace.identity.nodeId, "diagnostic-node")
    assertEquals(trace.identity.ply, 12)
    assertEquals(trace.transition.status, "invalid_clock")
    assert(trace.breaks.contains("transition:invalid_clock"))
    assertEquals(trace.preRendererVerdict, "transition_invalid")

  test("transition trace keeps standing tactical roots support-only and exposes no move-local tactical claim"):
    val trace = LowerLayerDiagnostic.trace(
      LowerLayerDiagnostic.Input(
        id = "standing-xray-transition",
        currentFen = standingXrayAfterFen,
        beforeFen = Some(standingXrayBeforeFen),
        playedMove = Some("e1f1"),
        nodeId = "diagnostic-node",
        ply = 0
      )
    )

    assertEquals(trace.input.status, "valid")
    assertEquals(trace.transition.status, "valid")
    assert(trace.extraction.rootFacts.exists(_.id == "xray_target"))
    assert(trace.claims.produced.exists(claim =>
      claim.evidenceIds.contains("xray_target") &&
        claim.scope.contains("position_local") &&
        claim.status == "support_only"
    ))
    assert(!trace.claims.produced.exists(claim =>
      claim.evidenceIds.exists(Set("loose_piece", "pinned_piece", "overloaded_piece", "trapped_piece", "xray_target")) &&
        claim.scope.contains("move_local")
    ))
    assert(trace.breaks.contains("admission:standing_tactical_only"))
    assertEquals(trace.tacticalVerdict, "standing_tactical_only")

  test("trace identifies a move-local tactical claim before renderer wording"):
    val trace = LowerLayerDiagnostic.trace(
      LowerLayerDiagnostic.Input(
        id = "moved-knight-left-loose",
        currentFen = movedKnightLooseAfterFen,
        beforeFen = Some(movedKnightLooseBeforeFen),
        playedMove = Some("f5d4"),
        nodeId = "diagnostic-node",
        ply = 0
      )
    )

    assertEquals(trace.input.status, "valid")
    assertEquals(trace.transition.status, "valid")
    assert(trace.claims.produced.exists(claim =>
      claim.route.contains("moved_piece_left_loose") &&
        claim.scope.contains("move_local") &&
        claim.status == "admitted"
    ))
    assert(trace.selection.lead.exists(_.route.contains("moved_piece_left_loose")))
    assertEquals(trace.tacticalVerdict, "move_local_tactical_claim")
    assertEquals(trace.preRendererVerdict, "selected_move_local_tactical_claim")

  private val startingFen = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1"
  private val afterE4Fen = "rnbqkbnr/pppppppp/8/8/4P3/8/PPPP1PPP/RNBQKBNR b KQkq e3 0 1"
  private val afterE4FenWithWrongClock = "rnbqkbnr/pppppppp/8/8/4P3/8/PPPP1PPP/RNBQKBNR b KQkq e3 1 1"
  private val standingXrayBeforeFen = "3qk3/3p4/8/8/8/8/8/3RK3 w - - 0 1"
  private val standingXrayAfterFen = "3qk3/3p4/8/8/8/8/8/3R1K2 b - - 1 1"
  private val movedKnightLooseBeforeFen = "4k3/8/8/5n2/8/8/8/3QK3 b - - 0 1"
  private val movedKnightLooseAfterFen = "4k3/8/8/8/3n4/8/8/3QK3 w - - 1 2"
