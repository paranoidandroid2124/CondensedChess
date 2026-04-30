package lila.commentary.diagnostic

class LowerDiagnosticSerialAuditPlanTest extends munit.FunSuite:

  test("serial audit plan preserves the agreed high-risk audit order"):
    val report = LowerDiagnosticSerialAuditPlan.fromRows(
      sampleRows("transition-moved-knight-left-loose", "transition-standing-xray", "current-pinned-piece")
    )

    assertEquals(
      report.slices.map(_.actionId),
      Vector(
        "source-pgn-verification",
        "loose-touched-anchor-capture-audit",
        "loose-non-touch-created-causality-audit",
        "pinned-created-pin-geometry-audit",
        "xray-created-geometry-audit",
        "pre-existing-anti-case-guard",
        "trapped-overloaded-followup"
      )
    )
    assertEquals(report.summary.nextAction, "recover_source_pgn_then_run_loose_touched_anchor_capture_audit")

  test("serial audit plan exposes slice artifacts and candidate counts"):
    val report = LowerDiagnosticSerialAuditPlan.fromRows(sampleRows("transition-moved-knight-left-loose"))
    val looseTouched = report.slices.find(_.actionId == "loose-touched-anchor-capture-audit").get

    assertEquals(looseTouched.candidateTransitions, 1)
    assertEquals(looseTouched.sourceVerifiedEligibleTransitions, 0)
    assertEquals(looseTouched.sourceBlockedTransitions, 1)
    assertEquals(looseTouched.outputArtifact, "01-loose-touched-anchor-capture-audit.jsonl")
    assert(looseTouched.completionGate.contains("source PGN verified"), clues(looseTouched))

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
