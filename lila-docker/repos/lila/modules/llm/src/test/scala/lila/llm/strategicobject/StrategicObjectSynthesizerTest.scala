package lila.llm.strategicobject

import chess.{ Color, Square }
import munit.FunSuite
import play.api.libs.json.*

import scala.io.Source

class StrategicObjectSynthesizerTest extends FunSuite:

  import StrategicObjectSynthesizerTest.*

  test("canonical vocabulary stays full while runtime synthesis stays board-direct") {
    assertEquals(StrategicObjectFamily.values.length, 24)
    assertEquals(StrategicObjectFamily.boardDirectFamilies.size, 14)
    assertEquals(StrategicObjectFamily.graphDerivedFamilies.size, 10)
  }

  test("each board-direct family has exact, negative, and contrastive fixture coverage") {
    val grouped = rows.groupBy(row => parseFamily(row.family))
    StrategicObjectFamily.boardDirectFamilies.foreach { family =>
      val familyRows = grouped.getOrElse(family, Nil)
      assert(familyRows.exists(_.caseType == "exact"), clue(s"missing exact row for $family"))
      assert(familyRows.exists(_.caseType == "negative"), clue(s"missing negative row for $family"))
      assert(familyRows.exists(_.caseType == "contrastive"), clue(s"missing contrastive row for $family"))
    }
  }

  test("fixture bank only synthesizes board-direct families") {
    val synthesized =
      rows.flatMap(row => objectsForFen(row.fen)).groupBy(_.id).values.map(_.head).toList

    assert(synthesized.nonEmpty, clue("expected non-empty synthesized fixture bank"))
    assert(synthesized.forall(obj => StrategicObjectFamily.boardDirectFamilies.contains(obj.family)))
    assert(synthesized.forall(obj => !StrategicObjectFamily.graphDerivedFamilies.contains(obj.family)))
  }

  rows.foreach { row =>
    test(s"object expectation ${row.id}") {
      val objects = objectsForFen(row.fen)
      val matched = findMatches(row, objects)

      row.expectation match
        case "present" =>
          assert(matched.nonEmpty, clue(s"${row.id} expected ${row.family} for ${row.owner}\n${render(objects)}"))
          matched.foreach(obj => assertRichObject(row, obj, objects))
        case "absent" =>
          assert(matched.isEmpty, clue(s"${row.id} expected no ${row.family} for ${row.owner}\n${render(objects)}"))
        case other =>
          fail(s"${row.id}: unsupported expectation=$other")
    }
  }

  test("contrastive file duel yields bilateral access networks and shell pressure") {
    val objects = objectsForFen("2r3k1/8/8/8/8/8/8/2R3K1 w - - 0 1")

    assert(objects.exists(obj => obj.family == StrategicObjectFamily.AccessNetwork && obj.owner == Color.White))
    assert(objects.exists(obj => obj.family == StrategicObjectFamily.AccessNetwork && obj.owner == Color.Black))
    assert(objects.exists(obj => obj.family == StrategicObjectFamily.KingSafetyShell && obj.owner == Color.White))
    assert(objects.exists(obj => obj.family == StrategicObjectFamily.KingSafetyShell && obj.owner == Color.Black))
  }

  test("contrastive break race yields bilateral structure and break objects") {
    val objects = objectsForFen("6k1/5p2/3p4/4P3/2P5/8/8/6K1 w - - 0 1")

    assert(objects.exists(obj => obj.family == StrategicObjectFamily.PawnStructureRegime && obj.owner == Color.White))
    assert(objects.exists(obj => obj.family == StrategicObjectFamily.PawnStructureRegime && obj.owner == Color.Black))
    assert(objects.exists(obj => obj.family == StrategicObjectFamily.BreakAxis && obj.owner == Color.White))
    assert(objects.exists(obj => obj.family == StrategicObjectFamily.BreakAxis && obj.owner == Color.Black))
  }

  test("contrastive passer race yields bilateral passer complexes") {
    val objects = objectsForFen("6k1/2P5/8/8/8/8/5p2/6K1 w - - 0 1")

    assert(objects.exists(obj => obj.family == StrategicObjectFamily.PasserComplex && obj.owner == Color.White))
    assert(objects.exists(obj => obj.family == StrategicObjectFamily.PasserComplex && obj.owner == Color.Black))
  }

  private def assertRichObject(
      row: ObjectExpectationRow,
      obj: StrategicObject,
      output: List[StrategicObject]
  ): Unit =
    assert(obj.id.nonEmpty, clue(s"${row.id}: expected object id"))
    assert(obj.anchors.nonEmpty, clue(s"${row.id}: expected anchors"))
    assert(obj.supportingPrimitives.nonEmpty, clue(s"${row.id}: expected supporting primitives"))
    assert(obj.stateStrength.coverage == obj.supportingPrimitives.size, clue(s"${row.id}: coverage mismatch"))
    assert(obj.evidenceFootprint.primitiveCount == obj.supportingPrimitives.size, clue(s"${row.id}: primitive count mismatch"))
    assert(obj.evidenceFootprint.supportingPieceCount == obj.supportingPieces.size, clue(s"${row.id}: supporting piece count mismatch"))
    assert(obj.evidenceFootprint.rivalCount == obj.rivalResourcesOrObjects.size, clue(s"${row.id}: rival count mismatch"))
    assert(obj.evidenceFootprint.primitiveKinds.nonEmpty, clue(s"${row.id}: expected primitive kinds"))
    assert(
      obj.locus.route.nonEmpty || obj.locus.allSquares.nonEmpty || obj.locus.files.nonEmpty,
      clue(s"${row.id}: expected non-empty object locus")
    )
    assert(profileMatchesFamily(row, obj), clue(s"${row.id}: profile/family mismatch"))
    row.anchor.flatMap(parseSquare).foreach { anchor =>
      assert(objectSquares(obj).contains(anchor), clue(s"${row.id}: missing anchor $anchor"))
    }
    row.primitiveKind.flatMap(parsePrimitiveKind).foreach { kind =>
      assert(obj.supportingPrimitives.exists(_.kind == kind), clue(s"${row.id}: missing supporting primitive $kind"))
      assert(obj.evidenceFootprint.primitiveKinds.contains(kind), clue(s"${row.id}: missing primitive kind $kind"))
    }
    row.tag.flatMap(parseTag).foreach { tag =>
      assert(obj.evidenceFootprint.tags.contains(tag), clue(s"${row.id}: missing tag $tag"))
    }
    obj.relations.foreach { relation =>
      assert(output.exists(_.id == relation.target.objectId), clue(s"${row.id}: unresolved relation target ${relation.target.objectId}"))
    }

  private def profileMatchesFamily(
      row: ObjectExpectationRow,
      obj: StrategicObject
  ): Boolean =
    (obj.family, obj.profile) match
      case (StrategicObjectFamily.PawnStructureRegime, StrategicObjectProfile.PawnStructureRegime(identity, _, _, _, _))                 => identity.nonEmpty
      case (StrategicObjectFamily.KingSafetyShell, StrategicObjectProfile.KingSafetyShell(_, accessFiles, stressedSquares, pressureSquares)) =>
        accessFiles.nonEmpty || stressedSquares.nonEmpty || pressureSquares.nonEmpty
      case (StrategicObjectFamily.DevelopmentCoordinationState, StrategicObjectProfile.DevelopmentCoordinationState(_, laggingPieces, activeFiles, coordinationSquares)) =>
        laggingPieces.nonEmpty || activeFiles.nonEmpty || coordinationSquares.nonEmpty
      case (StrategicObjectFamily.PieceRoleFitness, StrategicObjectProfile.PieceRoleFitness(_, affectedPiece, _)) =>
        affectedPiece.squares.nonEmpty
      case (StrategicObjectFamily.SpaceClamp, StrategicObjectProfile.SpaceClamp(_, clampSquares, _)) =>
        clampSquares.nonEmpty
      case (StrategicObjectFamily.CriticalSquareComplex, StrategicObjectProfile.CriticalSquareComplex(criticalKinds, _, pressure)) =>
        criticalKinds.nonEmpty && pressure > 0
      case (StrategicObjectFamily.FixedTargetComplex, StrategicObjectProfile.FixedTargetComplex(targetSquare, _, _, _, _)) =>
        row.anchor.flatMap(parseSquare).forall(_ == targetSquare)
      case (StrategicObjectFamily.BreakAxis, StrategicObjectProfile.BreakAxis(sourceSquare, breakSquare, targetSquares, _, _)) =>
        sourceSquare != breakSquare && targetSquares.nonEmpty
      case (StrategicObjectFamily.AccessNetwork, StrategicObjectProfile.AccessNetwork(lane, route, _, _)) =>
        lane.nonEmpty || route.nonEmpty
      case (StrategicObjectFamily.CounterplayAxis, StrategicObjectProfile.CounterplayAxis(resourceSquares, breakSquares, _)) =>
        resourceSquares.nonEmpty || breakSquares.nonEmpty
      case (StrategicObjectFamily.RestrictionShell, StrategicObjectProfile.RestrictionShell(restrictedSquares, _, constraintSquares)) =>
        restrictedSquares.nonEmpty && constraintSquares.nonEmpty
      case (StrategicObjectFamily.MobilityCage, StrategicObjectProfile.MobilityCage(affectedPiece, deniedSquares, _)) =>
        affectedPiece.squares.nonEmpty && deniedSquares.nonEmpty
      case (StrategicObjectFamily.RedeploymentRoute, StrategicObjectProfile.RedeploymentRoute(route, _, _)) =>
        route.origin != route.target
      case (StrategicObjectFamily.PasserComplex, StrategicObjectProfile.PasserComplex(passerSquare, promotionSquare, relativeRank, _, _)) =>
        passerSquare != promotionSquare && relativeRank > 0
      case _ => false

object StrategicObjectSynthesizerTest:

  final case class ObjectExpectationRow(
      id: String,
      caseType: String,
      source: String,
      fen: String,
      expectation: String,
      family: String,
      owner: String,
      anchor: Option[String],
      sector: Option[String],
      primitiveKind: Option[String],
      tag: Option[String]
  )

  private given Reads[ObjectExpectationRow] = Json.reads[ObjectExpectationRow]

  val rows: List[ObjectExpectationRow] =
    Source
      .fromResource("strategic-object-corpus/object-expectations.jsonl")
      .getLines()
      .toList
      .map(_.trim)
      .filter(_.nonEmpty)
      .zipWithIndex
      .map { case (line, idx) =>
        Json.parse(line).validate[ObjectExpectationRow].asEither match
          case Right(row) => row
          case Left(err)  => throw new IllegalArgumentException(s"invalid object expectation row ${idx + 1}: $err")
      }

  def objectsForFen(fen: String): List[StrategicObject] =
    val evidence = RawPositionEvidence.fromFen(fen).fold(err => throw new IllegalArgumentException(err), identity)
    val primitives =
      CanonicalPrimitiveExtractor.extract(evidence, PrimitiveExtractionTest.neutralTruthFrame, PrimitiveExtractionTest.neutralContract)
    CanonicalStrategicObjectSynthesizer.synthesize(primitives, PrimitiveExtractionTest.neutralTruthFrame)

  def findMatches(
      row: ObjectExpectationRow,
      objects: List[StrategicObject]
  ): List[StrategicObject] =
    val family = parseFamily(row.family)
    val owner = parseColor(row.owner)
    val anchor = row.anchor.flatMap(parseSquare)
    val sector = row.sector.flatMap(parseSector)
    val primitiveKind = row.primitiveKind.flatMap(parsePrimitiveKind)
    val tag = row.tag.flatMap(parseTag)

    objects.filter { obj =>
      obj.family == family &&
      obj.owner == owner &&
      anchor.forall(objectSquares(obj).contains) &&
      sector.forall(_ == obj.sector) &&
      primitiveKind.forall(obj.evidenceFootprint.primitiveKinds.contains) &&
      tag.forall(obj.evidenceFootprint.tags.contains)
    }

  def render(objects: List[StrategicObject]): String =
    objects
      .map { obj =>
        val squares = objectSquares(obj).map(_.key).mkString("[", ",", "]")
        val primitives = obj.evidenceFootprint.primitiveKinds.toList.map(_.toString).sorted.mkString("[", ",", "]")
        val tags = obj.evidenceFootprint.tags.toList.map(_.toString).sorted.mkString("[", ",", "]")
        s"${obj.family}:${showColor(obj.owner)} sector=${obj.sector} squares=$squares primitives=$primitives tags=$tags"
      }
      .mkString("\n")

  private def objectSquares(obj: StrategicObject): List[Square] =
    (obj.locus.allSquares ++ obj.anchors.flatMap(_.squares) ++ obj.anchors.flatMap(_.route.toList.flatMap(_.allSquares))).distinct.sortBy(_.key)

  def parseColor(raw: String): Color =
    raw.toLowerCase match
      case "white" => Color.White
      case "black" => Color.Black
      case _       => throw new IllegalArgumentException(s"unsupported owner=$raw")

  def parseFamily(raw: String): StrategicObjectFamily =
    StrategicObjectFamily.values.find(_.toString == raw).getOrElse(throw new IllegalArgumentException(s"unsupported family=$raw"))

  def parseSquare(raw: String): Option[Square] =
    Square.all.find(_.key.equalsIgnoreCase(raw))

  def parseSector(raw: String): Option[ObjectSector] =
    ObjectSector.values.find(_.toString.equalsIgnoreCase(raw))

  def parsePrimitiveKind(raw: String): Option[PrimitiveKind] =
    PrimitiveKind.values.find(_.toString.equalsIgnoreCase(raw))

  def parseTag(raw: String): Option[StrategicEvidenceTag] =
    StrategicEvidenceTag.values.find(_.toString.equalsIgnoreCase(raw))

  private def showColor(color: Color): String =
    if color.white then "white" else "black"
