package lila.commentary.diagnostic

import java.nio.charset.StandardCharsets
import java.nio.file.Files

import play.api.libs.json.Json

class LowerDiagnosticMaterializedTransitionEnrichmentTest extends munit.FunSuite:

  test("enrichment turns materialized decision rows into replayable transition rows"):
    val input = Files.createTempFile("materialized-enrichment-input", ".jsonl")
    val output = Files.createTempFile("materialized-enrichment-output", ".jsonl")
    try
      Files.writeString(
        input,
        """{"sampleId":"sample:1","fen":"rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1","ply":1,"playedUci":"e2e4","family":"DevelopmentCoordinationState","gameKey":"game-1"}""" + System.lineSeparator(),
        StandardCharsets.UTF_8
      )

      val result = LowerDiagnosticMaterializedTransitionEnrichment.enrich(input, output)
      Files.writeString(output, result.enrichedRows.map(Json.stringify).mkString(System.lineSeparator()), StandardCharsets.UTF_8)
      val rows = LowerDiagnosticLargeCorpus.loadExternalRows(output)
      val report = LowerDiagnosticReconstructionReport.fromDiagnostic(LowerDiagnosticReport.fromRows(rows))

      assertEquals(result.summary.enrichedRows, 1)
      assertEquals(result.summary.failedRows, 0)
      assertEquals(rows.head.input.beforeFen, Some("rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1"))
      assertEquals(rows.head.input.playedMove, Some("e2e4"))
      assertEquals(rows.head.input.currentFen, "rnbqkbnr/pppppppp/8/8/4P3/8/PPPP1PPP/RNBQKBNR b KQkq - 0 1")
      assertEquals(report.summary.replayableRows, 1)
    finally
      Files.deleteIfExists(input)
      Files.deleteIfExists(output)

  test("enrichment preserves exact full-FEN clocks for non-pawn non-capture transitions"):
    val input = Files.createTempFile("materialized-enrichment-capture-input", ".jsonl")
    val output = Files.createTempFile("materialized-enrichment-capture-output", ".jsonl")
    try
      Files.writeString(
        input,
        """{"sampleId":"sample:capture","fen":"r1bq1k1r/pppn1p2/8/6p1/1b1PQ2p/2N1PNB1/PP3PPP/R3K2R w KQ - 1 13","ply":25,"playedUci":"g3e5","family":"TacticalLiabilityState","gameKey":"game-capture"}""" + System.lineSeparator(),
        StandardCharsets.UTF_8
      )

      val result = LowerDiagnosticMaterializedTransitionEnrichment.enrich(input, output)
      Files.writeString(output, result.enrichedRows.map(Json.stringify).mkString(System.lineSeparator()), StandardCharsets.UTF_8)
      val rows = LowerDiagnosticLargeCorpus.loadExternalRows(output)
      val report = LowerDiagnosticReconstructionReport.fromDiagnostic(LowerDiagnosticReport.fromRows(rows))

      assertEquals(result.summary.enrichedRows, 1)
      assertEquals(result.summary.failedRows, 0)
      assertEquals(
        rows.head.input.currentFen,
        "r1bq1k1r/pppn1p2/8/4B1p1/1b1PQ2p/2N1PN2/PP3PPP/R3K2R b KQ - 2 13"
      )
      assertEquals(report.summary.replayableRows, 1)
    finally
      Files.deleteIfExists(input)
      Files.deleteIfExists(output)

  test("enrichment resets exact full-FEN clocks for capture transitions"):
    val input = Files.createTempFile("materialized-enrichment-capture-reset-input", ".jsonl")
    val output = Files.createTempFile("materialized-enrichment-capture-reset-output", ".jsonl")
    try
      Files.writeString(
        input,
        """{"sampleId":"sample:capture-reset","fen":"rnbqkbnr/ppp1pppp/8/3p4/4P3/8/PPPP1PPP/RNBQKBNR w KQkq d6 3 2","ply":3,"playedUci":"e4d5","family":"TacticalLiabilityState","gameKey":"game-capture-reset"}""" + System.lineSeparator(),
        StandardCharsets.UTF_8
      )

      val result = LowerDiagnosticMaterializedTransitionEnrichment.enrich(input, output)
      Files.writeString(output, result.enrichedRows.map(Json.stringify).mkString(System.lineSeparator()), StandardCharsets.UTF_8)
      val rows = LowerDiagnosticLargeCorpus.loadExternalRows(output)
      val report = LowerDiagnosticReconstructionReport.fromDiagnostic(LowerDiagnosticReport.fromRows(rows))

      assertEquals(result.summary.enrichedRows, 1)
      assertEquals(result.summary.failedRows, 0)
      assertEquals(
        rows.head.input.currentFen,
        "rnbqkbnr/ppp1pppp/8/3P4/8/8/PPPP1PPP/RNBQKBNR b KQkq - 0 2"
      )
      assertEquals(report.summary.replayableRows, 1)
    finally
      Files.deleteIfExists(input)
      Files.deleteIfExists(output)

  test("enrichment records failures without fabricating transition identity"):
    val input = Files.createTempFile("materialized-enrichment-failure-input", ".jsonl")
    val output = Files.createTempFile("materialized-enrichment-failure-output", ".jsonl")
    try
      Files.writeString(
        input,
        """{"sampleId":"sample:bad","fen":"8/8/8/8/8/8/4k3/7K w - - 0 1","ply":1,"playedUci":"e2e4","family":"DevelopmentCoordinationState"}""" + System.lineSeparator(),
        StandardCharsets.UTF_8
      )

      val result = LowerDiagnosticMaterializedTransitionEnrichment.enrich(input, output)

      assertEquals(result.summary.enrichedRows, 0)
      assertEquals(result.summary.failedRows, 1)
      assertEquals(result.failures.head.state, "failed_replay")
      assert(result.failures.head.reason.nonEmpty)
    finally
      Files.deleteIfExists(input)
      Files.deleteIfExists(output)
