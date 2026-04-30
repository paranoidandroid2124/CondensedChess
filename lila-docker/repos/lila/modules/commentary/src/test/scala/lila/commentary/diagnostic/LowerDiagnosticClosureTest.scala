package lila.commentary.diagnostic

import play.api.libs.json.Json

class LowerDiagnosticClosureTest extends munit.FunSuite:

  test("closure report classifies every tracked bottleneck into an existing owner boundary"):
    val report = closureReport(LowerDiagnosticLargeCorpus.loadTrackedRows())

    assert(report.closures.nonEmpty)
    assertEquals(report.summary.totalCandidates, 722)
    assert(report.closures.forall(_.existingLayer.nonEmpty), clues(report.closures.take(5)))
    assert(report.closures.forall(_.ownerPath.startsWith("lila.commentary")), clues(report.closures.take(5)))
    assert(report.closures.forall(_.implementationBoundary.nonEmpty), clues(report.closures.take(5)))
    assertEquals(report.summary.countsByClosureStatus("current_board_only_closed"), 286)
    assertEquals(report.summary.countsByClosureStatus("input_reconstruction_required"), 18)

  test("closure keeps current-board tactical facts out of transition admission work"):
    val report = closureReport(LowerDiagnosticLargeCorpus.loadTrackedRows())
    val currentOnly = report.closures.filter(_.closureStatus == "current_board_only_closed")

    assert(currentOnly.nonEmpty)
    assert(currentOnly.forall(_.workType == "no_admission_change"), clues(currentOnly.take(5)))
    assert(currentOnly.forall(_.blockedBy.contains("no_move_local_causality")), clues(currentOnly.take(5)))
    assert(currentOnly.forall(_.ownerPath == "lila.commentary.claim.EvidenceClaimProducer"), clues(currentOnly.take(5)))
    assert(!currentOnly.exists(_.action.contains("Open a narrow slice")), clues(currentOnly.take(5)))

  test("closure distinguishes positive transition candidates from anti-case-only transition buckets"):
    val report = closureReport(LowerDiagnosticLargeCorpus.loadTrackedRows())
    val positive = report.closures.filter(_.closureStatus == "existing_transition_slice_candidate")
    val antiOnly = report.closures.filter(_.closureStatus == "anti_case_only_no_expansion")

    assertEquals(positive.map(_.candidateCount).sum, 5)
    assertEquals(antiOnly.map(_.candidateCount).sum, 1)
    assert(positive.forall(_.ownerPath == "lila.commentary.claim.ExactBoardClaimProducer"), clues(positive))
    assert(antiOnly.forall(_.workType == "negative_guard"), clues(antiOnly))
    assert(positive.exists(_.countsByExpectation.get("present").contains(1)), clues(positive))
    assert(antiOnly.forall(!_.countsByExpectation.contains("present")), clues(antiOnly))

  test("closure routes non-tactical bottlenecks back to existing domain layers"):
    val report = closureReport(LowerDiagnosticLargeCorpus.loadTrackedRows())
    val routed = report.closures.filter(_.closureStatus == "routed_to_domain_lower_layer")

    assertEquals(routed.map(_.candidateCount).sum, 397)
    assert(routed.exists(c => c.existingLayer == "root" && c.ownerPath == "lila.commentary.root.RootExtractor"), clues(routed.take(20)))
    assert(routed.exists(c => c.existingLayer == "witness" && c.ownerPath.startsWith("lila.commentary.witness")), clues(routed.take(20)))
    assert(routed.exists(c => c.existingLayer == "projection" && c.ownerPath == "lila.commentary.projection.StrategyProjectionAdmission"), clues(routed.take(20)))
    assert(routed.exists(c => c.existingLayer == "certification" && c.ownerPath == "lila.commentary.certification"), clues(routed.take(20)))
    assert(routed.exists(c => c.existingLayer == "delta" && c.ownerPath == "lila.commentary.delta"), clues(routed.take(20)))

  test("closure treats materialized external rows as input reconstruction only"):
    val rows = LowerDiagnosticLargeCorpus.loadExternalRows(
      java.nio.file.Paths.get("tmp/strategic_object/reports/fixed-target-probe-authority-collapse.materialized300.candidate.rows.jsonl"),
      Some(300)
    )
    val report = closureReport(rows)

    assertEquals(report.summary.totalCandidates, 300)
    assertEquals(report.summary.countsByClosureStatus, Map("input_reconstruction_required" -> 300).withDefaultValue(0))
    assert(report.closures.forall(_.existingLayer == "diagnostic_input"), clues(report.closures))
    assert(report.closures.forall(_.workType == "input_replay_reconstruction"), clues(report.closures))

  test("closure JSON and markdown table are parseable handoff artifacts"):
    val report = closureReport(LowerDiagnosticLargeCorpus.loadTrackedRows())
    val summary = LowerDiagnosticClosureJson.summaryJson(report.summary)
    val closure = LowerDiagnosticClosureJson.closureJson(report.closures.head)
    val table = LowerDiagnosticClosureJson.table(report)

    assertEquals((summary \ "totalCandidates").as[Int], 722)
    assert((closure \ "closureStatus").as[String].nonEmpty)
    assert((closure \ "ownerPath").as[String].startsWith("lila.commentary"))
    assert(Json.stringify(closure).contains("falsePositiveControl"))
    assert(table.startsWith("| status | candidates | existing layer | work type | action |"))

  private def closureReport(rows: Vector[LowerDiagnosticLargeCorpus.Row]): LowerDiagnosticClosureReport =
    LowerDiagnosticClosureReport.fromBottlenecks(
      LowerDiagnosticBottleneckReport.fromDiagnostic(
        LowerDiagnosticReport.fromRows(rows)
      )
    )
