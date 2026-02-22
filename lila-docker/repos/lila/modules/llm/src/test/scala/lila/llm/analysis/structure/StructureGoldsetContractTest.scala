package lila.llm.analysis.structure

import munit.FunSuite
import play.api.libs.json.*
import lila.llm.model.structure.{ StructureGoldRow, StructureId }

import scala.io.Source

class StructureGoldsetContractTest extends FunSuite:

  private def readLines: List[String] =
    Source
      .fromResource("structure_goldset_v1.jsonl")
      .getLines()
      .toList
      .map(_.stripPrefix("\uFEFF"))
      .filter(_.trim.nonEmpty)

  private def readRows: List[StructureGoldRow] =
    readLines.zipWithIndex.map { case (line, idx) =>
      Json.parse(line).validate[StructureGoldRow].asEither match
        case Left(err) => fail(s"invalid row at line ${idx + 1}: $err")
        case Right(row) => row
    }

  test("goldset v1 should contain 300 rows") {
    val rows = readRows
    assertEquals(rows.size, 300)
  }

  test("goldset v1 should include all 18 target structures and unknown bucket") {
    val rows = readRows
    val labels = rows.map(_.primary.toString).toSet

    val expected = Set(
      "Carlsbad",
      "IQPWhite",
      "IQPBlack",
      "HangingPawnsWhite",
      "HangingPawnsBlack",
      "FrenchAdvanceChain",
      "NajdorfScheveningenCenter",
      "BenoniCenter",
      "KIDLockedCenter",
      "SlavCaroTriangle",
      "MaroczyBind",
      "Hedgehog",
      "FianchettoShell",
      "Stonewall",
      "OpenCenter",
      "LockedCenter",
      "FluidCenter",
      "SymmetricCenter",
      "Unknown"
    )

    assertEquals(labels, expected)
  }

  test("goldset v1 should keep exact class distribution (18x14 + unknown48)") {
    val rows = readRows
    val counts = rows.groupBy(_.primary).view.mapValues(_.size).toMap

    StructureId.taxonomyV1.foreach { id =>
      assertEquals(
        counts.getOrElse(id, 0),
        14,
        clues(s"expected 14 rows for ${id.toString}, got ${counts.getOrElse(id, 0)}")
      )
    }
    assertEquals(counts.getOrElse(StructureId.Unknown, 0), 48)
  }

  test("goldset v1 should have unique FEN strings and no cross-label conflicts") {
    val rows = readRows
    val uniqueFenCount = rows.map(_.fen).toSet.size
    assertEquals(uniqueFenCount, rows.size)

    val conflicts = rows
      .groupBy(_.fen)
      .collect { case (fen, rs) if rs.map(_.primary).distinct.size > 1 => fen -> rs.map(_.primary.toString).distinct }
      .toList
    assert(conflicts.isEmpty, clues(conflicts))
  }

  test("goldset v1 rows should include required v2 metadata fields") {
    val rows = readRows
    assert(rows.forall(_.id.nonEmpty))
    assert(rows.forall(_.fen.nonEmpty))
    assert(rows.forall(_.annotators.size >= 2), clues(rows.filter(_.annotators.size < 2).take(5)))
    assert(rows.forall(_.adjudicatedBy.exists(_.nonEmpty)), clues(rows.filter(_.adjudicatedBy.forall(_.isEmpty)).take(5)))
    assert(rows.forall(_.sourceGameId.exists(_.nonEmpty)))
    assert(rows.forall(_.sourcePly.exists(_ > 0)))
    assert(rows.forall(_.notes.exists(_.nonEmpty)))
    assert(rows.filter(_.primary != StructureId.Unknown).forall(_.expectedTopPlanIds.nonEmpty))
  }
