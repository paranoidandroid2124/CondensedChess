package lila.commentary.diagnostic

import play.api.libs.json.JsValue

class LowerDiagnosticStandingTacticalAuditTest extends munit.FunSuite:

  test("standing tactical audit classifies all current-only tactical facts"):
    val report = standingAudit()

    assertEquals(report.summary.total, 286)
    assert(report.summary.countsByTag("current_board_only_no_transition") == 286, clues(report.summary))
    assert(report.summary.countsByTag("off_source_schema_smell") > 0, clues(report.summary))
    assert(report.summary.countsByTag("context_missing_plausible_static_tactic") > 0, clues(report.summary))
    assertEquals(report.summary.countsByTag("overbroad_home_pawn_xray"), 0)
    assert(report.summary.countsByTag("immediate_capture_static_fact") > 0, clues(report.summary))
    assert(report.summary.countsByLedgerClass("immediate_capture_available") > 0, clues(report.summary))
    assert(report.summary.countsByPublicDisposition("current_board_claim_candidate_not_move_causal") > 0, clues(report.summary))

  test("standing tactical audit exposes the five hand-read false-positive risks"):
    val byId = standingAudit().rows.map(row => row.rowId -> row).toMap

    assert(!byId.contains("root:r-start-white-king-e1"), clues(byId.get("root:r-start-white-king-e1")))
    assert(byId("root:r-castling-cross-kk").tags.contains("immediate_capture_static_fact"), clues(byId("root:r-castling-cross-kk")))
    assertEquals(byId("root:r-castling-cross-kk").selectedClaimId, Some("exact-board-immediate-capture-h1h8"))
    assert(!byId("root:r-castling-cross-kk").tags.contains("selected_static_owner_not_side_to_move"), clues(byId("root:r-castling-cross-kk")))
    assert(byId("root:r-contested-blocked-file-occupied").tags.contains("off_source_schema_smell"), clues(byId("root:r-contested-blocked-file-occupied")))
    assert(byId("witness:u-fork-pawn-attacker").tags.contains("context_missing_plausible_static_tactic"), clues(byId("witness:u-fork-pawn-attacker")))
    assert(byId("witness:u-fork-pawn-attacker").tags.contains("severe_material_skew_context"), clues(byId("witness:u-fork-pawn-attacker")))
    assert(byId("witness:u-duty-pin-bound-mode").tags.contains("context_missing_plausible_static_tactic"), clues(byId("witness:u-duty-pin-bound-mode")))

  test("standing tactical audit JSON is parseable and carries root facts"):
    val report = standingAudit()
    val summary = LowerDiagnosticStandingAuditJson.summaryJson(report.summary)
    val row = LowerDiagnosticStandingAuditJson.rowJson(report.rows.head)

    assertEquals((summary \ "total").as[Int], 286)
    assert((summary \ "countsByTag").as[Map[String, Int]].nonEmpty)
    assert((summary \ "countsByLedgerClass").as[Map[String, Int]].nonEmpty)
    assert((row \ "rootFacts").as[Vector[JsValue]].nonEmpty)
    assert((row \ "tags").as[Vector[String]].nonEmpty)
    assert((row \ "ledgerClass").as[String].nonEmpty)
    assert((row \ "publicDisposition").as[String].nonEmpty)
    assert((row \ "nextChessAction").as[String].nonEmpty)

  private def standingAudit(): LowerDiagnosticStandingAuditReport =
    LowerDiagnosticStandingAuditReport.fromDiagnostic(
      LowerDiagnosticReport.fromRows(LowerDiagnosticLargeCorpus.loadTrackedRows())
    )
