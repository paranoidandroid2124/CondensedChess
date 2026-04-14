package lila.llm.tools.strategicobject

import java.nio.charset.StandardCharsets
import java.nio.file.Files

import munit.FunSuite
import play.api.libs.json.*

import scala.concurrent.duration.*

class StrategicObjectBatchCoverageSupportTest extends FunSuite:

  override val munitTimeout = 90.seconds

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
        """{"sampleId":"counterplay-1","source":"test","fen":"6k1/8/8/4p3/3P4/2P5/8/6K1 w - - 0 1"}""",
        """{"sampleId":"counterplay-exact","source":"test","fen":"2bk1bnr/4qppp/pr3n2/3B4/3PpB2/7N/1PQ2PPP/R4RK1 w - - 6 17","playedUci":"a1c1"}""",
        """{"sampleId":"counterplay-positive","source":"test","fen":"rn2qrk1/pbp1b1pp/1p1p4/3Ppp2/2P5/2NN2P1/PP1QPPBP/R4RK1 w - - 0 13","playedUci":"f2f4"}""",
        """{"sampleId":"counterplay-near-miss","source":"test","fen":"1r1r2k1/6p1/1qb1p1p1/p1ppPp2/5Q1P/1PP1RN2/P4PP1/1R4K1 w - - 0 25","playedUci":"f3g5"}"""
      ).mkString("", "\n", "\n")
    Files.writeString(path, rows, StandardCharsets.UTF_8)

    val (inputSummary, inputs) = StrategicObjectBatchCoverageSupport.loadFenJsonl(path)
    val (report, activations, auditRows) = StrategicObjectBatchCoverageSupport.report(inputSummary, inputs)
    val (filteredCounterplayReport, filteredCounterplayActivations, _) =
      StrategicObjectBatchCoverageSupport.report(inputSummary, inputs, Set("CounterplayAxis"))

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
    val filteredCounterplay =
      filteredCounterplayReport.families.find(_.family == "CounterplayAxis").getOrElse(
        fail("expected filtered CounterplayAxis batch summary")
      )

    assertEquals(report.input.evaluatedSampleCount, 6)
    assert(access.primaryCount >= 1, clue(access))
    assert(access.ownerRate > 0.0, clue(access))
    assert(access.uniqueOwnerRate.nonEmpty, clue(access))
    assert(access.residualSupportRate.isEmpty, clue(access))
    assert(access.accessDemotionRate >= 0.0, clue(access))
    assert(access.auditBuckets.exists(_.bucket == "top_50"), clue(access))
    assert(access.byAxis.exists(_.axis == "WhyThis"), clue(access))
    assert(access.byPass.exists(pass => pass.scope == "move_local" && pass.ownerCount >= 1), clue(access.byPass))
    assertEquals(access.highestBatchStage, "planner_primary")
    assert(tradeInvariant.primaryCount >= 1, clue(tradeInvariant))
    assertEquals(tradeInvariant.highestBatchStage, "planner_primary")
    assert(counterplay.sampleCount >= 1, clue(counterplay))
    assert(counterplay.shadowRate >= 0.0, clue(counterplay))
    assert(counterplay.uniqueOwnerRate.isEmpty, clue(counterplay))
    assert(counterplay.byPass.exists(pass => pass.scope == "move_local"), clue(counterplay.byPass))
    assert(counterplay.byPass.exists(pass => pass.scope == "move_local" && pass.plannerNoneCount >= 0), clue(counterplay.byPass))
    assert(
      Set("object", "certification", "planner_none", "planner_support").contains(counterplay.highestBatchStage),
      clue(counterplay)
    )
    assertEquals(filteredCounterplay.shadowRate, counterplay.shadowRate, clue(filteredCounterplay))
    assertEquals(filteredCounterplay.accessOverlapAxisCount, counterplay.accessOverlapAxisCount, clue(filteredCounterplay))
    assertEquals(filteredCounterplay.accessDemotedAxisCount, counterplay.accessDemotedAxisCount, clue(filteredCounterplay))
    assert(filteredCounterplayActivations.forall(_.family == "CounterplayAxis"), clue(filteredCounterplayActivations))
    assert(activations.exists(row => row.family == "AccessNetwork" && row.bestAdmission == "primary"))
    assert(activations.exists(row => row.family == "TradeInvariant" && row.bestAdmission == "primary"))
    val exactCounterplayRow =
      activations.find(row => row.family == "CounterplayAxis" && row.sampleId == "counterplay-exact").getOrElse(
        fail("expected exact CounterplayAxis activation row")
      )
    assertEquals(
      exactCounterplayRow.moveLocalBoundary.flatMap(_.blocker),
      Some("ProvisionalScopeClosed"),
      clue(exactCounterplayRow)
    )
    assertEquals(exactCounterplayRow.moveLocalBoundary.map(_.exactRivalAdmitted), Some(true), clue(exactCounterplayRow))
    assertEquals(exactCounterplayRow.moveLocalBoundary.map(_.moveTouchesCore), Some(true), clue(exactCounterplayRow))
    assertEquals(exactCounterplayRow.moveLocalBoundary.map(_.relationTouch), Some(true), clue(exactCounterplayRow))
    assertEquals(exactCounterplayRow.moveLocalBoundary.map(_.moveWitnessSatisfied), Some(true), clue(exactCounterplayRow))
    assertEquals(exactCounterplayRow.moveLocalBoundary.map(_.blockedByProvisionalScope), Some(true), clue(exactCounterplayRow))
    assertEquals(exactCounterplayRow.moveLocalBoundary.map(_.blockedByCertification), Some(false), clue(exactCounterplayRow))
    assert(exactCounterplayRow.moveLocalBoundary.exists(_.objectId.nonEmpty), clue(exactCounterplayRow))
    assert(exactCounterplayRow.moveLocalBoundary.exists(_.admittedRelationWitnesses.nonEmpty), clue(exactCounterplayRow))
    assert(exactCounterplayRow.moveLocalBoundary.exists(_.matchedRelationWitnesses.nonEmpty), clue(exactCounterplayRow))
    assertEquals(
      exactCounterplayRow.moveLocalBoundary.map(_.relationTouchReason),
      Some("MatchedAdmittedRivalWitness"),
      clue(exactCounterplayRow)
    )
    val positiveCounterplayRow =
      activations.find(row => row.family == "CounterplayAxis" && row.sampleId == "counterplay-positive").getOrElse(
        fail("expected positive CounterplayAxis activation row")
      )
    assertEquals(positiveCounterplayRow.moveLocalBoundary.flatMap(_.blocker), None, clue(positiveCounterplayRow))
    assertEquals(positiveCounterplayRow.moveLocalBoundary.map(_.moveWitnessSatisfied), Some(true), clue(positiveCounterplayRow))
    assertEquals(positiveCounterplayRow.moveLocalBoundary.map(_.blockedByProvisionalScope), Some(false), clue(positiveCounterplayRow))
    assertEquals(positiveCounterplayRow.moveLocalBoundary.map(_.blockedByCertification), Some(false), clue(positiveCounterplayRow))
    assert(positiveCounterplayRow.moveLocalBoundary.exists(_.moveLocalDeltaCount > 0), clue(positiveCounterplayRow))
    assert(positiveCounterplayRow.moveLocalBoundary.exists(_.moveLocalClaimCount > 0), clue(positiveCounterplayRow))
    val nearMissCounterplayRow =
      activations.find(row => row.family == "CounterplayAxis" && row.sampleId == "counterplay-near-miss").getOrElse(
        fail("expected near-miss CounterplayAxis activation row")
      )
    assertEquals(
      nearMissCounterplayRow.moveLocalBoundary.flatMap(_.blocker),
      Some("MissingMoveEdgeTouch"),
      clue(nearMissCounterplayRow)
    )
    assertEquals(nearMissCounterplayRow.moveLocalBoundary.map(_.moveTouchesCore), Some(false), clue(nearMissCounterplayRow))
    assertEquals(nearMissCounterplayRow.moveLocalBoundary.map(_.relationTouch), Some(true), clue(nearMissCounterplayRow))
    assertEquals(nearMissCounterplayRow.moveLocalBoundary.map(_.moveWitnessSatisfied), Some(false), clue(nearMissCounterplayRow))
    val counterplayBoundary =
      counterplay.counterplayBoundary.getOrElse(fail("expected counterplay boundary aggregate"))
    assert(counterplayBoundary.exactRivalAdmittedCount >= 3, clue(counterplayBoundary))
    assert(counterplayBoundary.moveWitnessSatisfiedCount >= 2, clue(counterplayBoundary))
    assert(counterplayBoundary.blockedByProvisionalScopeCount >= 1, clue(counterplayBoundary))
    assertEquals(counterplayBoundary.blockedByCertificationCount, 0, clue(counterplayBoundary))
    assert(counterplayBoundary.moveLocalClaimRowCount >= 1, clue(counterplayBoundary))
    assert(auditRows.exists(row => row.family == "AccessNetwork" && row.auditBucket == "top_50"), clue(auditRows))
    assert(auditRows.exists(row => row.family == "TradeInvariant" && row.auditBucket == "hard_negative_20"), clue(auditRows))
    assert(auditRows.forall(row => row.selectedCount <= row.requestedCount), clue(auditRows))

    val manualAuditRows = StrategicObjectBatchCoverageSupport.topManualAuditRows(auditRows, limitPerFamily = 20)
    assert(manualAuditRows.forall(_.auditBucket == "top_20_manual"), clue(manualAuditRows))
    assert(manualAuditRows.forall(_.requestedCount == 20), clue(manualAuditRows))

    val renderedAudit = StrategicObjectBatchCoverageSupport.renderAuditJsonl(auditRows.take(1))
    val parsedAudit = Json.parse(renderedAudit.trim)
    assert((parsedAudit \ "boardTruthVerdict").toOption.contains(JsNull), clue(parsedAudit))
    assert((parsedAudit \ "familyNameFit").toOption.contains(JsNull), clue(parsedAudit))
    assert((parsedAudit \ "stageFit").toOption.contains(JsNull), clue(parsedAudit))
    assert((parsedAudit \ "notes").toOption.contains(JsNull), clue(parsedAudit))

    val renderedReport = Json.toJson(report)
    assert((renderedReport \ "families")(0).as[JsObject].keys.contains("uniqueOwnerRate"), clue(renderedReport))
    assert((renderedReport \ "families")(0).as[JsObject].keys.contains("residualSupportRate"), clue(renderedReport))
    assert((renderedReport \ "families")(0).as[JsObject].keys.contains("accessDemotionRate"), clue(renderedReport))
    assert((renderedReport \ "families")(0).as[JsObject].keys.contains("auditBuckets"), clue(renderedReport))
    assert((renderedReport \ "families")(0).as[JsObject].keys.contains("byPass"), clue(renderedReport))
    assert((renderedReport \ "families")(0).as[JsObject].keys.contains("counterplayBoundary"), clue(renderedReport))
    val renderedExactCounterplay = Json.toJson(exactCounterplayRow)
    assert((renderedExactCounterplay \ "moveLocalBoundary" \ "blocker").asOpt[String].contains("ProvisionalScopeClosed"), clue(renderedExactCounterplay))
    assert((renderedExactCounterplay \ "moveLocalBoundary" \ "exactRivalAdmitted").asOpt[Boolean].contains(true), clue(renderedExactCounterplay))
    assert((renderedExactCounterplay \ "moveLocalBoundary" \ "moveWitnessSatisfied").asOpt[Boolean].contains(true), clue(renderedExactCounterplay))
    assert((renderedExactCounterplay \ "moveLocalBoundary" \ "blockedByCertification").asOpt[Boolean].contains(false), clue(renderedExactCounterplay))
    assert((renderedExactCounterplay \ "moveLocalBoundary" \ "objectId").asOpt[String].contains("CounterplayAxis-white-queenside-a1-abc"), clue(renderedExactCounterplay))
    assert((renderedExactCounterplay \ "moveLocalBoundary" \ "relationTouchReason").asOpt[String].contains("MatchedAdmittedRivalWitness"), clue(renderedExactCounterplay))
  }
