package lila.commentary.diagnostic

import java.nio.charset.StandardCharsets
import java.nio.file.Files

import play.api.libs.json.Json

class LowerDiagnosticReconstructionTest extends munit.FunSuite:

  test("reconstruction report separates replayable current-only and missing-before rows"):
    val report = LowerDiagnosticReconstructionReport.fromDiagnostic(
      LowerDiagnosticReport.fromRows(LowerDiagnosticLargeCorpus.loadTrackedRows() ++ externalRows())
    )

    assert(report.summary.replayableRows > 0, clues(report.summary))
    assert(report.summary.countsByState("current_board_only") > 0, clues(report.summary))
    assert(report.summary.countsByReason("missing_before_fen_for_played_uci") == 2, clues(report.summary))
    assert(report.rows.exists(row => row.reconstructionState == "replayable" && row.requiredInputs.contains("playedMove")))
    assert(report.rows.filter(_.reason == "missing_before_fen_for_played_uci").forall(_.nextAction.contains("Recover beforeFen")))

  test("reconstruction JSON carries source metadata needed for replay recovery"):
    val report = LowerDiagnosticReconstructionReport.fromDiagnostic(LowerDiagnosticReport.fromRows(externalRows()))
    val summary = LowerDiagnosticReconstructionJson.summaryJson(report.summary)
    val row = LowerDiagnosticReconstructionJson.rowJson(report.rows.head)
    val countsByState = (summary \ "countsByState").as[Map[String, Int]]

    assertEquals((summary \ "total").as[Int], 2)
    assertEquals(countsByState("unreconstructable"), 2)
    assert((row \ "metadata" \ "pgnPath").as[String].endsWith("sample.pgn"))
    assert((row \ "metadata" \ "gameKey").as[String].nonEmpty)
    assert((row \ "requiredInputs").as[Vector[String]].contains("beforeFen"))

  private def externalRows(): Vector[LowerDiagnosticLargeCorpus.Row] =
    val file = Files.createTempFile("reconstruction-materialized-like", ".jsonl")
    try
      val lines =
        Vector(
          """{"sampleId":"sample:1","source":"sample-source","gameKey":"game-1","pgnPath":"C:\\tmp\\sample.pgn","fen":"8/8/8/8/8/8/4k3/7K w - - 0 1","ply":1,"playedUci":"e2e4","family":"DevelopmentCoordinationState"}""",
          """{"sampleId":"sample:2","source":"sample-source","gameKey":"game-1","pgnPath":"C:\\tmp\\sample.pgn","fen":"4k3/6b1/8/8/3N4/8/8/4K3 w - - 0 1","ply":2,"playedUci":"d4f5","family":"FixedTargetComplex"}"""
        ).mkString(System.lineSeparator())
      Files.writeString(file, lines + System.lineSeparator(), StandardCharsets.UTF_8)
      LowerDiagnosticLargeCorpus.loadExternalRows(file)
    finally Files.deleteIfExists(file)
