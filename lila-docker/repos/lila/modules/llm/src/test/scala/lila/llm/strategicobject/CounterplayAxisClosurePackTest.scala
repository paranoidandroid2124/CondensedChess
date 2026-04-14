package lila.llm.strategicobject

import scala.concurrent.duration.*
import scala.io.Source
import scala.util.Using

import munit.FunSuite
import play.api.libs.json.*

class CounterplayAxisClosurePackTest extends FunSuite:

  override val munitTimeout = 150.seconds

  private val expectedOfficialStatus = "exact RestrictionShell support narrow-go"

  test("counterplay closure packs pin the official narrow-go metadata and row counts") {
    val official = loadPack("strategic-object-corpus/CounterplayAxis.official-capability-pack.json")
    val frozen = loadPack("strategic-object-corpus/CounterplayAxis.frozen-out-of-scope-pack.json")
    val negative = loadPack("strategic-object-corpus/CounterplayAxis.negative-pack.json")

    List(official, frozen, negative).foreach { pack =>
      assertEquals(pack.family, "CounterplayAxis", clue(pack))
      assertEquals(pack.officialStatus, expectedOfficialStatus, clue(pack))
      assertEquals(pack.batch.rawGameCount, 45, clue(pack))
      assertEquals(pack.batch.evaluatedSampleCount, 360, clue(pack))
      assertEquals(pack.batch.maxPliesPerGame, 8, clue(pack))
      assertEquals(pack.batch.rerunDate, "2026-04-14", clue(pack))
      assert(pack.closureRule.contains("new exact positive evidence pack"), clue(pack))
      assert(pack.closureRule.contains("same-batch 45/360/max8 rerun"), clue(pack))
      assert(pack.closureRule.contains("no other family drift"), clue(pack))
    }

    assertEquals(official.packType, "official_capability", clue(official))
    assertEquals(frozen.packType, "frozen_out_of_scope", clue(frozen))
    assertEquals(negative.packType, "negative", clue(negative))
    assertEquals(official.rows.size, 8, clue(official.rows))
    assertEquals(frozen.rows.size, 4, clue(frozen.rows))
    assertEquals(negative.rows.size, 12, clue(negative.rows))
  }

  test("counterplay closure packs stay exact-board consistent with the runtime boundary") {
    val packs =
      List(
        loadPack("strategic-object-corpus/CounterplayAxis.official-capability-pack.json"),
        loadPack("strategic-object-corpus/CounterplayAxis.frozen-out-of-scope-pack.json"),
        loadPack("strategic-object-corpus/CounterplayAxis.negative-pack.json")
      )

    packs.foreach { pack =>
      pack.rows.foreach { row =>
        val truth = PrimitiveExtractionTest.moveTransitionVisibleTruthFrameFor(row.playedUci)
        val contract = PrimitiveExtractionTest.moveTransitionVisibleContractFor(row.playedUci)
        val objects = StrategicObjectSynthesizerTest.objectsForFen(row.fen, truth)
        val objectsById = objects.map(obj => obj.id -> obj).toMap
        val move = moveTrace(row.playedUci)
        val deltas = CanonicalStrategicObjectDeltaProjector.project(contract, truth, objects)
        val claims = CanonicalClaimCertification.certify(contract, objects, deltas)

        val assessment =
          objects
            .filter(_.id == row.objectId)
            .flatMap(obj => CounterplayMoveLocalBoundary.assess(obj, move, objectsById))
            .headOption
            .getOrElse(fail(s"expected CounterplayAxis assessment for ${row.sampleId} -> ${row.objectId}"))

        val moveLocalDeltas =
          deltas.filter(delta =>
            delta.objectId == row.objectId &&
              delta.scope == StrategicDeltaScope.MoveLocal
          )
        val moveLocalClaims =
          claims.filter(claim =>
            claim.objectId == row.objectId &&
              claim.deltaScope == StrategicDeltaScope.MoveLocal
          )

        assertEquals(assessment.exactRivalAdmitted, true, clue(row.sampleId))
        assertEquals(assessment.moveTouchesCore, row.expectedMoveTouchesCore, clue(assessment))
        assertEquals(assessment.relationTouch, row.expectedRelationTouch, clue(assessment))
        assertEquals(assessment.moveWitnessSatisfied, row.expectedMoveWitnessSatisfied, clue(assessment))
        assertEquals(
          assessment.blocker.map(_.toString),
          row.expectedBlocker,
          clue(assessment)
        )
        assertEquals(
          assessment.relationMatches.map(_.targetId).distinct.sorted,
          row.expectedMatchedTargets.sorted,
          clue(assessment)
        )

        if row.expectedMoveLocalClaim then
          assertEquals(assessment.blockedByProvisionalScope, false, clue(assessment))
          assertEquals(assessment.blockedByCertification, false, clue(assessment))
          assert(moveLocalDeltas.nonEmpty, clue(moveLocalDeltas))
          assert(moveLocalClaims.nonEmpty, clue(moveLocalClaims))
          assert(moveLocalClaims.forall(_.status == ClaimStatus.SupportOnly), clue(moveLocalClaims))
        else
          assert(moveLocalDeltas.isEmpty, clue(moveLocalDeltas))
          assert(moveLocalClaims.isEmpty, clue(moveLocalClaims))
      }
    }
  }

  private def moveTrace(
      playedUci: String
  ): StrategicPlayedMoveTrace =
    StrategicPlayedMoveTrace(
      from = chess.Square.fromKey(playedUci.take(2)).getOrElse(fail(s"invalid move: $playedUci")),
      to = chess.Square.fromKey(playedUci.slice(2, 4)).getOrElse(fail(s"invalid move: $playedUci"))
    )

  private def loadPack(
      resourcePath: String
  ): CounterplayAxisPack =
    val raw =
      Using.resource(
        Option(getClass.getClassLoader.getResourceAsStream(resourcePath))
          .getOrElse(fail(s"missing resource: $resourcePath"))
      ) { stream =>
        Source.fromInputStream(stream, "UTF-8").mkString
      }

    Json.parse(raw).as[CounterplayAxisPack]

  private final case class CounterplayAxisPackBatch(
      rawGameCount: Int,
      evaluatedSampleCount: Int,
      maxPliesPerGame: Int,
      rerunDate: String
  )

  private final case class CounterplayAxisPackRow(
      sampleId: String,
      fen: String,
      playedUci: String,
      objectId: String,
      expectedBlocker: Option[String],
      expectedMatchedTargets: List[String],
      expectedMoveTouchesCore: Boolean,
      expectedRelationTouch: Boolean,
      expectedMoveWitnessSatisfied: Boolean,
      expectedMoveLocalClaim: Boolean,
      note: String
  )

  private final case class CounterplayAxisPack(
      family: String,
      officialStatus: String,
      packType: String,
      batch: CounterplayAxisPackBatch,
      closureRule: String,
      rows: List[CounterplayAxisPackRow]
  )

  private object CounterplayAxisPackBatch:
    given Reads[CounterplayAxisPackBatch] = Json.reads[CounterplayAxisPackBatch]

  private object CounterplayAxisPackRow:
    given Reads[CounterplayAxisPackRow] = Json.reads[CounterplayAxisPackRow]

  private object CounterplayAxisPack:
    given Reads[CounterplayAxisPack] = Json.reads[CounterplayAxisPack]
