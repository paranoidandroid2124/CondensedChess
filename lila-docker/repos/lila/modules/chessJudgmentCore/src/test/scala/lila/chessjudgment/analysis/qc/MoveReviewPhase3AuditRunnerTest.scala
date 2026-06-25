package lila.chessjudgment.analysis.qc

import java.nio.charset.StandardCharsets
import java.nio.file.{ Files, Path }

import lila.chessjudgment.analysis.assembly.{ RawMoveReviewInput, RawOpeningContext }
import lila.chessjudgment.model.strategic.VariationLine
import play.api.libs.json.Json

import scala.jdk.CollectionConverters.*

class MoveReviewPhase3AuditRunnerTest extends munit.FunSuite:

  test("main archives replay input when output path is provided"):
    val dir = Files.createTempDirectory("phase3-audit-runner-main")
    try
      val input = dir.resolve("input.jsonl")
      val output = dir.resolve("phase3_audit_output_current_chunk01_rows001-001.jsonl")
      val row =
        Json.obj(
          "sampleId" -> "sample-main",
          "input" -> Json.obj(
            "fen" -> "8/8/8/8/8/8/4P3/4K3 w - - 0 1",
            "playedMoveUci" -> "e2e4",
            "variations" -> Json.arr(
              Json.obj(
                "moves" -> Json.arr("e2e4"),
                "scoreCp" -> 20,
                "depth" -> 16
              )
            ),
            "currentEvalCp" -> 20,
            "ply" -> 1,
            "movePrefixUci" -> Json.arr("g1f3")
          ),
          "opening" -> "Test Opening",
          "targetPly" -> 1,
          "playedSan" -> "e4"
        )
      Files.writeString(input, Json.stringify(row), StandardCharsets.UTF_8)

      MoveReviewPhase3AuditRunner.main(Array(input.toString, output.toString))

      val archive = dir.resolve("phase3_audit_input_replay_current_chunk01_rows001-001.jsonl")
      assert(Files.exists(output))
      assert(Files.exists(archive))
      val replay = Json.parse(Files.readString(archive, StandardCharsets.UTF_8))
      assertEquals((replay \ "sampleId").as[String], "sample-main")
      assertEquals((replay \ "input" \ "playedMoveUci").as[String], "e2e4")
    finally deleteRecursively(dir)

  test("writes replay input archive next to audit output"):
    val dir = Files.createTempDirectory("phase3-audit-runner")
    try
      val output = dir.resolve("phase3_audit_output_current_chunk01_rows001-001.jsonl")
      val raw =
        RawMoveReviewInput(
          fen = "8/8/8/8/8/8/4P3/4K3 w - - 0 1",
          playedMoveUci = "e2e4",
          variations = List(VariationLine(List("e2e4"), scoreCp = 20, depth = 16)),
          currentEvalCp = Some(20),
          ply = Some(1),
          openingContext = Some(RawOpeningContext(eco = Some("A00"), name = Some("Test Opening"), family = Some("A"))),
          movePrefixUci = List("g1f3")
        )
      val sample =
        MoveReviewPhase3AuditRunner.AuditInputSample(
          sampleId = "sample-1",
          raw = raw,
          opening = Some("Test Opening"),
          sliceKind = Some("eco"),
          targetPly = Some(1),
          playedSan = Some("e4")
        )

      val archive = MoveReviewPhase3AuditRunner.writeReplayInputArchive(output, List(sample))

      assertEquals(archive.getFileName.toString, "phase3_audit_input_replay_current_chunk01_rows001-001.jsonl")
      val rows = Files.readAllLines(archive, StandardCharsets.UTF_8).asScala.toList
      assertEquals(rows.size, 1)
      val json = Json.parse(rows.head)
      assertEquals((json \ "schemaVersion").as[String], "move_review_phase3_replay_input.v1")
      assertEquals((json \ "sampleId").as[String], "sample-1")
      assertEquals((json \ "input" \ "playedMoveUci").as[String], "e2e4")
      assertEquals((json \ "input" \ "variations" \ 0 \ "moves" \ 0).as[String], "e2e4")
      assertEquals((json \ "input" \ "movePrefixUci" \ 0).as[String], "g1f3")
      assertEquals((json \ "opening").as[String], "Test Opening")
    finally deleteRecursively(dir)

  private def deleteRecursively(path: Path): Unit =
    if Files.exists(path) then
      Files
        .walk(path)
        .iterator()
        .asScala
        .toList
        .sortWith((left, right) => left.getNameCount > right.getNameCount)
        .foreach(Files.deleteIfExists)
