package lila.commentary.diagnostic

class LowerDiagnosticHighRiskTacticalAuditTest extends munit.FunSuite:

  test("high-risk audit classifies loose-piece created transitions without treating loose as immediate capture"):
    val report = LowerDiagnosticHighRiskTacticalAuditReport.fromRows(sampleRows("transition-moved-knight-left-loose"))

    assertEquals(report.summary.uniqueTransitions, 1)
    assertEquals(report.summary.countsBySchema("loose_piece"), 1)
    assertEquals(report.rows.head.auditDisposition, "candidate_created_loose_piece_requires_capture_audit")
    assert(report.rows.head.logicalDefects.contains("loose_root_does_not_prove_immediate_capture"))
    assertEquals(report.rows.head.publicClaimReadiness, "not_ready_for_public_claim")
    assert(report.rows.head.loosePayloads.exists(_.bestExchangeGainCp > 0), clues(report.rows.head.loosePayloads))
    assertEquals(report.rows.head.sourceVerificationState, "missing_pgn_path")

  test("high-risk audit keeps pinned-piece standing facts out of move-causal public readiness"):
    val report = LowerDiagnosticHighRiskTacticalAuditReport.fromRows(sampleRows("current-pinned-piece"))

    assertEquals(report.summary.uniqueTransitions, 1)
    assertEquals(report.summary.countsBySchema("pinned_piece"), 1)
    assertEquals(report.rows.head.auditDisposition, "reject_current_board_only_pinned_piece")
    assert(report.rows.head.logicalDefects.contains("missing_transition_identity"))
    assert(report.rows.head.pinnedPayloads.exists(_.pinKind == "absolute"), clues(report.rows.head.pinnedPayloads))
    assertEquals(report.rows.head.publicClaimReadiness, "not_ready_for_public_claim")

  test("high-risk audit covers xray pinned and loose with common source-unverified gating"):
    val report = LowerDiagnosticHighRiskTacticalAuditReport.fromRows(
      sampleRows("transition-standing-xray", "transition-moved-knight-left-loose", "current-pinned-piece")
    )

    assertEquals(report.summary.totalRows, 3)
    assertEquals(report.summary.uniqueTransitions, 4)
    assert(report.summary.countsBySchema.keySet.contains("xray_target"))
    assert(report.summary.countsBySchema.keySet.contains("loose_piece"))
    assert(report.summary.countsBySchema.keySet.contains("pinned_piece"))
    assert(
      report.rows.forall(_.publicClaimReadiness == "not_ready_for_public_claim"),
      clues(report.rows.filter(_.publicClaimReadiness != "not_ready_for_public_claim"))
    )

  test("high-risk audit emits xray geometry payload without making it public-ready"):
    val report = LowerDiagnosticHighRiskTacticalAuditReport.fromRows(
      Vector(
        row(
          id = "synthetic-current-xray",
          currentFen = "3qk3/3p4/8/8/8/8/8/3R1K2 b - - 0 1",
          beforeFen = None,
          playedMove = None
        )
      )
    )

    val xray = report.rows.find(_.schema == "xray_target").get
    assert(xray.xrayPayloads.exists(payload => payload.sliderSquare == "d1" && payload.blockerSquare == "d7" && payload.targetSquare == "d8"), clues(xray))
    assertEquals(xray.publicClaimReadiness, "not_ready_for_public_claim")

  test("created pin payload does not mark absent before-pinner as an existing line"):
    val report = LowerDiagnosticHighRiskTacticalAuditReport.fromRows(
      Vector(
        row(
          id = "synthetic-created-pin",
          currentFen = "rnbqkbnr/ppp2pp1/7p/3p4/3P1B2/2N5/PP2PPPP/R2QKBNR b KQkq - 1 5",
          beforeFen = Some("rnbqkbnr/ppp2pp1/7p/3p4/3P4/2N5/PP2PPPP/R1BQKBNR w KQkq - 0 5"),
          playedMove = Some("c1f4")
        )
      )
    )

    val pin = report.rows.find(_.schema == "pinned_piece").get
    assert(pin.pinnedPayloads.exists(_.beforeLineState == "before_pinner_absent_or_not_aligned"), clues(pin.pinnedPayloads))

  test("created touched classification ignores touched standing facts"):
    val report = LowerDiagnosticHighRiskTacticalAuditReport.fromRows(
      Vector(
        row(
          id = "synthetic-created-non-touch-loose",
          currentFen = "4k2r/8/8/4B3/8/8/8/4K3 b - - 1 1",
          beforeFen = Some("4k2r/8/8/8/6B1/8/8/4K3 w - - 0 1"),
          playedMove = Some("g4e5")
        )
      )
    )

    val loose = report.rows.find(_.schema == "loose_piece").get
    assertEquals(loose.auditDisposition, "candidate_created_loose_piece_requires_causality_audit")
    assertEquals(loose.touchedAnchorSquares, Vector.empty)

  private def sampleRows(ids: String*): Vector[LowerDiagnosticLargeCorpus.Row] =
    LowerDiagnosticSampleCorpus.loadAll().filter(row => ids.contains(row.id)).map: row =>
      LowerDiagnosticLargeCorpus.Row(
        id = s"sample:${row.id}",
        sourceFile = "lower-diagnostic-sample.jsonl",
        sourceKind = "sample",
        sourceSchema = None,
        caseType = None,
        expectation = None,
        input = row.input,
        metadata = Map("sourceId" -> row.id)
      )

  private def row(
      id: String,
      currentFen: String,
      beforeFen: Option[String],
      playedMove: Option[String]
  ): LowerDiagnosticLargeCorpus.Row =
    LowerDiagnosticLargeCorpus.Row(
      id = s"sample:$id",
      sourceFile = "synthetic",
      sourceKind = "sample",
      sourceSchema = None,
      caseType = None,
      expectation = None,
      input = LowerLayerDiagnostic.Input(
        id = id,
        currentFen = currentFen,
        beforeFen = beforeFen,
        playedMove = playedMove,
        nodeId = id,
        ply = 1
      ),
      metadata = Map("sourceId" -> id)
    )
