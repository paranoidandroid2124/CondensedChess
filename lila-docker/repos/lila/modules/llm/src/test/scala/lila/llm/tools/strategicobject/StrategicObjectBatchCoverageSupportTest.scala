package lila.llm.tools.strategicobject

import java.nio.charset.StandardCharsets
import java.nio.file.Files

import munit.FunSuite
import play.api.libs.json.*

class StrategicObjectBatchCoverageSupportTest extends FunSuite:

  test("embedded games json loader extracts actual ply samples with played UCI") {
    val path = Files.createTempFile("strategic-object-batch-games", ".json")
    val pgn =
      """[Event "Mini"]
        |[Site "?"]
        |[Date "2026.03.16"]
        |[Round "?"]
        |[White "White"]
        |[Black "Black"]
        |[Result "1-0"]
        |
        |1.e4 e5 2.Nf3 Nc6 3.Bb5 a6 1-0
        |""".stripMargin
    val payload =
      s"""{"games":[{"id":"mini-game","tier":"test","expectedThemes":["mini"],"pgn":${play.api.libs.json.Json.toJson(pgn)}}]}"""
    Files.writeString(path, payload, StandardCharsets.UTF_8)

    val (summary, rows) =
      StrategicObjectBatchCoverageSupport.loadEmbeddedGamesJson(
        path,
        StrategicObjectBatchCoverageSupport.LoadConfig(
          maxGames = None,
          plyStep = 1,
          maxPliesPerGame = None
        )
      )

    assertEquals(summary.rawGameCount, 1)
    assertEquals(summary.skippedGameCount, 0)
    assert(rows.nonEmpty, clue(rows))
    assert(rows.forall(_.playedUci.nonEmpty), clue(rows))
    assert(rows.exists(_.sampleId == "mini-game:ply:1"))
  }

  test("catalog jsonl loader reads pgnPath games into actual ply samples") {
    val pgnPath = Files.createTempFile("strategic-object-batch-catalog-game", ".pgn")
    val catalogPath = Files.createTempFile("strategic-object-batch-catalog", ".jsonl")
    val pgn =
      """[Event "Mini"]
        |[Site "?"]
        |[Date "2026.03.16"]
        |[Round "?"]
        |[White "White"]
        |[Black "Black"]
        |[Result "1-0"]
        |
        |1.d4 d5 2.c4 e6 3.Nc3 Nf6 1-0
        |""".stripMargin
    Files.writeString(pgnPath, pgn, StandardCharsets.UTF_8)
    Files.writeString(
      catalogPath,
      s"""{"gameKey":"catalog-mini","pgnPath":"${pgnPath.toString.replace("\\", "\\\\")}","mixBucket":"test","familyTags":["mini"]}""" + "\n",
      StandardCharsets.UTF_8
    )

    val (summary, rows) =
      StrategicObjectBatchCoverageSupport.loadCatalogJsonl(
        catalogPath,
        StrategicObjectBatchCoverageSupport.LoadConfig(
          maxGames = None,
          plyStep = 1,
          maxPliesPerGame = Some(4)
        )
      )

    assertEquals(summary.rawGameCount, 1)
    assertEquals(summary.skippedGameCount, 0)
    assert(rows.nonEmpty, clue(rows))
    assert(rows.forall(_.gameKey.contains("catalog-mini")), clue(rows))
    assert(rows.forall(_.pgnPath.contains(pgnPath.toString)), clue(rows))
    assert(rows.forall(_.playedUci.nonEmpty), clue(rows))
  }

  test("fen jsonl batch report reflects actual primary and support activations") {
    val path = Files.createTempFile("strategic-object-batch-fen", ".jsonl")
    val rows =
      List(
        """{"sampleId":"access-1","source":"test","fen":"6k1/7p/8/8/8/3B4/8/6K1 w - - 0 1","playedUci":"d3h7"}""",
        """{"sampleId":"trade-1","source":"test","fen":"r2qr1k1/pp2bpp1/2n1bn1p/3p4/3N4/2N1B1P1/PPQ1PPBP/R4RK1 w - - 4 13","playedUci":"d4e6"}""",
        """{"sampleId":"counterplay-1","source":"test","fen":"6k1/8/8/4p3/3P4/2P5/8/6K1 w - - 0 1"}"""
      ).mkString("", "\n", "\n")
    Files.writeString(path, rows, StandardCharsets.UTF_8)

    val (inputSummary, inputs) = StrategicObjectBatchCoverageSupport.loadFenJsonl(path)
    val (report, activations, auditRows) = StrategicObjectBatchCoverageSupport.report(inputSummary, inputs)

    val access =
      report.families.find(_.family == "AccessNetwork").getOrElse(
        fail("expected AccessNetwork batch summary")
      )
    val tradeInvariant =
      report.families.find(_.family == "TradeInvariant").getOrElse(
        fail("expected TradeInvariant batch summary")
      )
    val counterplay =
      report.families.find(_.family == "CounterplayAxis").getOrElse(
        fail("expected CounterplayAxis batch summary")
      )

    assertEquals(report.input.evaluatedSampleCount, 3)
    assert(access.primaryCount >= 1, clue(access))
    assert(access.ownerRate > 0.0, clue(access))
    assertEquals(access.highestBatchStage, "planner_primary")
    assert(tradeInvariant.primaryCount >= 1, clue(tradeInvariant))
    assertEquals(tradeInvariant.highestBatchStage, "planner_primary")
    assert(counterplay.sampleCount >= 1, clue(counterplay))
    assert(counterplay.shadowRate >= 0.0, clue(counterplay))
    assert(
      Set("object", "certification", "planner_none", "planner_support").contains(counterplay.highestBatchStage),
      clue(counterplay)
    )
    assert(activations.exists(row => row.family == "AccessNetwork" && row.bestAdmission == "primary"))
    assert(activations.exists(row => row.family == "TradeInvariant" && row.bestAdmission == "primary"))
    assert(auditRows.exists(row => row.family == "AccessNetwork" && row.auditBucket == "top_50"), clue(auditRows))
    assert(auditRows.exists(row => row.family == "TradeInvariant" && row.auditBucket == "hard_negative_20"), clue(auditRows))

    val renderedAudit = StrategicObjectBatchCoverageSupport.renderAuditJsonl(auditRows.take(1))
    val parsedAudit = Json.parse(renderedAudit.trim)
    assert((parsedAudit \ "boardTruthVerdict").toOption.contains(JsNull), clue(parsedAudit))
    assert((parsedAudit \ "familyNameFit").toOption.contains(JsNull), clue(parsedAudit))
    assert((parsedAudit \ "stageFit").toOption.contains(JsNull), clue(parsedAudit))
    assert((parsedAudit \ "notes").toOption.contains(JsNull), clue(parsedAudit))
  }
