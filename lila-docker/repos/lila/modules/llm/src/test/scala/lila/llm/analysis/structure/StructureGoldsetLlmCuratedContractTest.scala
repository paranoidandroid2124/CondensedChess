package lila.llm.analysis.structure

import munit.FunSuite
import play.api.libs.json.*
import lila.llm.model.structure.{ StructureGoldRow, StructureId }

import scala.io.Source

class StructureGoldsetLlmCuratedContractTest extends FunSuite:

  private def readRows: List[StructureGoldRow] =
    Source
      .fromResource("structure_goldset_v1_llm_curated.jsonl")
      .getLines()
      .toList
      .map(_.stripPrefix("\uFEFF").trim)
      .filter(_.nonEmpty)
      .zipWithIndex
      .map { case (line, idx) =>
        Json.parse(line).validate[StructureGoldRow].asEither match
          case Left(err) => fail(s"invalid row at line ${idx + 1}: $err")
          case Right(row) => row
      }

  test("llm curated goldset should keep 300 rows and target distribution") {
    val rows = readRows
    assertEquals(rows.size, 300)
    val counts = rows.groupBy(_.primary).view.mapValues(_.size).toMap
    StructureId.taxonomyV1.foreach(id => assertEquals(counts.getOrElse(id, 0), 14))
    assertEquals(counts.getOrElse(StructureId.Unknown, 0), 48)
  }

  test("llm curated goldset should satisfy required metadata fields") {
    val rows = readRows
    assert(rows.forall(_.id.nonEmpty))
    assert(rows.forall(_.fen.nonEmpty))
    assert(rows.forall(_.annotators.size >= 2))
    assert(rows.forall(_.adjudicatedBy.exists(_.nonEmpty)))
    assert(rows.forall(_.sourceGameId.exists(_.nonEmpty)))
    assert(rows.forall(_.sourcePly.exists(_ > 0)))
    assert(rows.forall(_.notes.exists(_.nonEmpty)))
    assert(rows.filter(_.primary != StructureId.Unknown).forall(_.expectedTopPlanIds.nonEmpty))
  }

  test("llm curated goldset should have high board diversity") {
    val rows = readRows
    val boardOnlyUnique = rows.map(_.fen.split("\\s+").headOption.getOrElse("")).toSet.size
    assert(boardOnlyUnique >= 250, clues(boardOnlyUnique))
  }
