package lila.commentary.diagnostic

class LowerDiagnosticXrayAuditTest extends munit.FunSuite:

  test("xray audit groups row-amplified materialization by unique transition"):
    val rows = sampleRows("transition-standing-xray", "transition-xray-capturable-anti-case")
    val duplicated = rows ++ rows.map(row => row.copy(id = s"${row.id}:duplicate"))

    val report = LowerDiagnosticXrayAuditReport.fromRows(duplicated)

    assertEquals(report.summary.totalRows, 4)
    assertEquals(report.summary.uniqueXrayTransitions, 2)
    assert(report.summary.rowAmplification > 0, clues(report.summary))
    assert(report.rows.exists(_.auditDisposition == "reject_preexisting_or_standing_xray"), clues(report.rows))
    assert(report.rows.forall(_.requiredNextEvidence.contains("slider identity")), clues(report.rows))

  test("xray audit does not treat legal replay alone as public claim evidence"):
    val report = LowerDiagnosticXrayAuditReport.fromRows(sampleRows("transition-standing-xray", "transition-xray-capturable-anti-case"))
    val xrayRows = report.rows

    assert(xrayRows.nonEmpty)
    assert(
      xrayRows.forall(row => row.publicClaimReadiness != "ready_for_public_claim"),
      clues(xrayRows.filter(_.publicClaimReadiness == "ready_for_public_claim"))
    )
    assert(
      report.summary.countsByLogicalDefect.keySet.contains("xray_root_lacks_slider_blocker_identity"),
      clues(report.summary)
    )

  private def sampleRows(ids: String*): Vector[LowerDiagnosticLargeCorpus.Row] =
    LowerDiagnosticSampleCorpus.loadAll().filter(row => ids.contains(row.id)).map: row =>
      LowerDiagnosticLargeCorpus.Row(
        id = s"sample:${row.id}",
        sourceFile = "lower-diagnostic-sample.jsonl",
        sourceKind = "sample",
        sourceSchema = Some("xray_target"),
        caseType = None,
        expectation = None,
        input = row.input,
        metadata = Map("sourceId" -> row.id)
      )
