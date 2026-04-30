package lila.commentary.diagnostic

import java.nio.charset.StandardCharsets
import java.nio.file.Files

class LowerDiagnosticBottleneckTest extends munit.FunSuite:

  test("phase 3 bottleneck report separates replayable rows from input-blocked rows"):
    val tracked = LowerDiagnosticReport.fromRows(LowerDiagnosticLargeCorpus.loadTrackedRows())
    val materialized = LowerDiagnosticReport.fromRows(externalRows())

    val trackedBottlenecks = LowerDiagnosticBottleneckReport.fromDiagnostic(tracked)
    val materializedBottlenecks = LowerDiagnosticBottleneckReport.fromDiagnostic(materialized)

    assert(trackedBottlenecks.summary.replayableRows > 0, clues(trackedBottlenecks.summary))
    assert(trackedBottlenecks.summary.inputBlockedRows < trackedBottlenecks.summary.total, clues(trackedBottlenecks.summary))
    assertEquals(materializedBottlenecks.summary.total, 3)
    assertEquals(materializedBottlenecks.summary.inputBlockedRows, 3)
    assertEquals(materializedBottlenecks.summary.replayableRows, 0)
    assert(
      materializedBottlenecks.bottlenecks.head.deferredTo.contains("input_reconstruction"),
      clues(materializedBottlenecks.bottlenecks.head)
    )

  test("phase 3 bottlenecks classify owner layer deferred target and fourth-phase slice"):
    val report = LowerDiagnosticBottleneckReport.fromDiagnostic(
      LowerDiagnosticReport.fromRows(LowerDiagnosticLargeCorpus.loadTrackedRows())
    )

    assert(report.bottlenecks.nonEmpty)
    assert(report.bottlenecks.exists(_.ownerLayer == "root_witness_schema"), clues(report.bottlenecks.take(10)))
    assert(report.bottlenecks.exists(_.ownerLayer == "standing_board_fact_only"), clues(report.bottlenecks.take(10)))
    assert(report.bottlenecks.filter(_.ownerLayer == "exact_transition_admission").forall(_.replayable), clues(report.bottlenecks))
    assert(report.bottlenecks.exists(_.resolutionState == "fixed_positive_control"), clues(report.bottlenecks.take(10)))
    assert(report.fourthPhaseSlices.nonEmpty)
    assert(
      report.fourthPhaseSlices.filter(_.ownerLayer == "exact_transition_admission").forall(slice =>
        slice.replayableCount == slice.candidateCount && slice.countsByExpectation.nonEmpty && slice.countsByCaseType.nonEmpty
      ),
      clues(report.fourthPhaseSlices.filter(_.ownerLayer == "exact_transition_admission"))
    )
    assert(
      report.fourthPhaseSlices.filter(_.ownerLayer == "standing_board_fact_only").forall(_.replayableCount == 0),
      clues(report.fourthPhaseSlices.filter(_.ownerLayer == "standing_board_fact_only").take(5))
    )
    assert(report.fourthPhaseSlices.forall(_.completionCondition.nonEmpty), clues(report.fourthPhaseSlices.take(5)))
    assert(report.fourthPhaseSlices.forall(_.minimumEvidence.nonEmpty), clues(report.fourthPhaseSlices.take(5)))
    val transitionHandoff = report.fourthPhaseSlices.filter(slice =>
      slice.ownerLayer == "exact_transition_admission" || slice.ownerLayer == "positive_control"
    )
    assert(transitionHandoff.nonEmpty, clues(report.fourthPhaseSlices.take(10)))
    assert(transitionHandoff.forall(_.replayableCount > 0), clues(transitionHandoff))

  test("phase 3 JSON handoff is reproducible and parseable"):
    val report = LowerDiagnosticBottleneckReport.fromDiagnostic(
      LowerDiagnosticReport.fromRows(LowerDiagnosticLargeCorpus.loadTrackedRows())
    )
    val summaryJson = LowerDiagnosticBottleneckJson.summaryJson(report.summary)
    val bottleneckJson = LowerDiagnosticBottleneckJson.bottleneckJson(report.bottlenecks.head)
    val sliceJson = LowerDiagnosticBottleneckJson.sliceJson(report.fourthPhaseSlices.head)

    assertEquals((summaryJson \ "total").as[Int], report.summary.total)
    assert((summaryJson \ "countsByOwnerLayer").as[Map[String, Int]].nonEmpty, clues(summaryJson))
    assert((bottleneckJson \ "deferredTo").asOpt[String].nonEmpty)
    assert((bottleneckJson \ "countsByCaseType").as[Map[String, Int]].nonEmpty)
    assert((bottleneckJson \ "countsByExpectation").as[Map[String, Int]].nonEmpty)
    assert((sliceJson \ "minimumEvidence").as[Vector[String]].nonEmpty)

  private def externalRows(): Vector[LowerDiagnosticLargeCorpus.Row] =
    val file = Files.createTempFile("phase3-materialized-like", ".jsonl")
    try
      val lines =
        Vector(
          """{"sampleId":"sample:1","fen":"8/8/8/8/8/8/4k3/7K w - - 0 1","ply":1,"playedUci":"e2e4","family":"DevelopmentCoordinationState"}""",
          """{"sampleId":"sample:2","fen":"4k3/6b1/8/8/3N4/8/8/4K3 w - - 0 1","ply":2,"playedUci":"d4f5","family":"FixedTargetComplex"}""",
          """{"sampleId":"sample:3","fen":"4r2k/8/8/8/8/8/4B3/4K3 w - - 0 1","ply":3,"playedUci":"e2d3","family":"DefenderDependencyNetwork"}"""
        ).mkString(System.lineSeparator())
      Files.writeString(file, lines + System.lineSeparator(), StandardCharsets.UTF_8)
      LowerDiagnosticLargeCorpus.loadExternalRows(file)
    finally Files.deleteIfExists(file)
