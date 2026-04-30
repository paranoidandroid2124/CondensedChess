package lila.commentary.diagnostic

import java.nio.charset.StandardCharsets
import java.nio.file.Files

import play.api.libs.json.Json

class LowerDiagnosticLargeCorpusTest extends munit.FunSuite:

  test("large lower diagnostic corpus covers every Fen-backed tracked row"):
    val rows = LowerDiagnosticLargeCorpus.loadTrackedRows()

    assert(rows.size >= 700, clues(rows.size))
    assert(rows.forall(_.input.currentFen.nonEmpty), clues(rows.filter(_.input.currentFen.isEmpty).map(_.id)))
    assert(
      rows.exists(row => row.sourceFile == "root-expectations.jsonl" && row.sourceSchema.contains("xray_target")),
      clues(rows.map(_.sourceSchema).distinct.sorted.take(20))
    )
    assert(
      rows.exists(row => row.sourceFile == "witness-expectations.jsonl" && row.sourceSchema.contains("fork")),
      clues(rows.map(_.sourceSchema).distinct.sorted.take(20))
    )
    assert(
      rows.exists(row => row.sourceFile == "delta-expectations.jsonl" && row.input.beforeFen.nonEmpty),
      clues(rows.filter(_.sourceFile == "delta-expectations.jsonl").take(3))
    )
    assertEquals(rows.map(_.id).distinct.size, rows.size)

  test("large lower diagnostic report classifies every row by layer schema risk and handoff group"):
    val rows = LowerDiagnosticLargeCorpus.loadTrackedRows()
    val report = LowerDiagnosticReport.fromRows(rows)

    assertEquals(report.traces.size, rows.size)
    assertEquals(report.summary.total, rows.size)
    assert(report.summary.countsBySourceFile.keySet.contains("root-expectations.jsonl"), clues(report.summary.countsBySourceFile))
    assert(report.summary.countsBySourceFile.keySet.contains("projection-expectations.jsonl"), clues(report.summary.countsBySourceFile))
    assert(report.summary.countsByLayerBreak.keySet.exists(_.startsWith("admission:")), clues(report.summary.countsByLayerBreak))
    assert(report.summary.countsByLayerBreak.keySet.exists(_.startsWith("extraction:")), clues(report.summary.countsByLayerBreak))
    assert(report.summary.countsByTacticalSchema.keySet.contains("xray_target"), clues(report.summary.countsByTacticalSchema))
    assert(report.summary.countsByTacticalSchema.keySet.contains("pinned_piece"), clues(report.summary.countsByTacticalSchema))
    assert(
      report.summary.countsByFalsePositiveRisk.keySet.contains("non_tactical_lead_masks_tactical_gap"),
      clues(report.summary.countsByFalsePositiveRisk)
    )
    assert(report.auditSample.nonEmpty, clues(report.summary.countsByFalsePositiveRisk))
    assert(
      report.auditSample.forall(sample => sample.rowId.nonEmpty && sample.handoffGroup.nonEmpty),
      clues(report.auditSample.take(5))
    )
    assert(report.handoffGroups.nonEmpty, clues(report.summary.countsByHandoffGroup))
    assert(report.handoffGroups.forall(_.nextPhaseQuestion.nonEmpty), clues(report.handoffGroups.take(5)))
    assert(
      report.handoffGroups.forall(group => group.exampleRowIds == group.exampleRowIds.distinct),
      clues(report.handoffGroups.filter(group => group.exampleRowIds != group.exampleRowIds.distinct).take(3))
    )

  test("large lower diagnostic JSON artifacts are reproducible and parseable"):
    val report = LowerDiagnosticReport.fromRows(LowerDiagnosticLargeCorpus.loadTrackedRows())
    val summaryJson = LowerDiagnosticReportJson.summaryJson(report.summary)
    val auditLine = LowerDiagnosticReportJson.auditSampleJson(report.auditSample.head)

    assertEquals((summaryJson \ "total").as[Int], report.summary.total)
    assert((summaryJson \ "countsByLayerBreak").as[Map[String, Int]].nonEmpty, clues(summaryJson))
    assertEquals((auditLine \ "rowId").as[String], report.auditSample.head.rowId)
    assert(Json.stringify(summaryJson).contains("countsByFalsePositiveRisk"), clues(summaryJson))

  test("external candidate loader accepts sampleId fen ply and playedUci metadata"):
    val file = Files.createTempFile("lower-diagnostic-external", ".jsonl")
    try
      Files.writeString(
        file,
        """{"sampleId":"sample-game:ply:1","source":"sample-source","sourceKind":"catalog_jsonl","gameKey":"sample-game","pgnPath":"C:\\tmp\\sample.pgn","opening":"Sample Opening","fen":"8/8/8/8/8/8/4k3/7K w - - 0 1","ply":1,"playedUci":"e2e4","tags":["materialized-300"],"axes":["WhatMattersHere"],"family":"DevelopmentCoordinationState","mixBucket":"sample","bestStage":"certification","bestAdmission":"none"}""" + System.lineSeparator(),
        StandardCharsets.UTF_8
      )

      val rows = LowerDiagnosticLargeCorpus.loadExternalRows(file)

      assertEquals(rows.size, 1)
      assertEquals(rows.head.id, "external_candidate:sample-game:ply:1")
      assertEquals(rows.head.input.ply, 1)
      assertEquals(rows.head.input.playedMove, None)
      assertEquals(rows.head.metadata("playedUci"), "e2e4")
      assertEquals(rows.head.metadata("source"), "sample-source")
      assertEquals(rows.head.metadata("sourceKind"), "external_candidate")
      assertEquals(rows.head.metadata("externalSourceKind"), "catalog_jsonl")
      assertEquals(rows.head.metadata("pgnPath"), "C:\\tmp\\sample.pgn")
      assertEquals(rows.head.metadata("opening"), "Sample Opening")
      assertEquals(rows.head.metadata("tags"), """["materialized-300"]""")
      assertEquals(rows.head.sourceSchema, Some("DevelopmentCoordinationState"))

      val report = LowerDiagnosticReport.fromRows(rows)
      assertEquals(report.traces.head.layerBreak, "transition:missing_before_for_played_move")
      assert(report.traces.head.falsePositiveRisks.contains("transition_identity_incomplete_for_move_claim"))
    finally Files.deleteIfExists(file)
