package lila.llm.strategicobject

import chess.{ Color, Square }
import lila.llm.analysis.MoveTruthFrame
import munit.FunSuite
import play.api.libs.json.*

import scala.io.Source

class StrategicObjectSynthesizerTest extends FunSuite:

  import StrategicObjectSynthesizerTest.*

  test("canonical vocabulary stays full and runtime synthesis reaches all 24 families") {
    val synthesizedFamilies =
      rows.flatMap(objectsForRow).map(_.family).toSet

    assertEquals(StrategicObjectFamily.values.length, 24)
    assertEquals(StrategicObjectFamily.boardDirectFamilies.size, 14)
    assertEquals(StrategicObjectFamily.graphDerivedFamilies.size, 10)
    assertEquals(synthesizedFamilies, StrategicObjectFamily.values.toSet)
  }

  test("each family has exact, negative, and contrastive fixture coverage") {
    val grouped = rows.groupBy(row => parseFamily(row.family))
    StrategicObjectFamily.values.foreach { family =>
      val familyRows = grouped.getOrElse(family, Nil)
      assert(familyRows.exists(_.caseType == "exact"), clue(s"missing exact row for $family"))
      assert(familyRows.exists(_.caseType == "negative"), clue(s"missing negative row for $family"))
      assert(familyRows.exists(_.caseType == "contrastive"), clue(s"missing contrastive row for $family"))
    }
  }

  test("fixture bank keeps full family split and expected minimum row count") {
    assert(rows.size >= 72, clue(s"expected at least 72 object rows, got ${rows.size}"))
    assertEquals(rows.groupBy(_.family).size, 24)
  }

  test("fixture bank synthesizes graph-derived families in the object layer") {
    val synthesized =
      rows.flatMap(objectsForRow).groupBy(_.id).values.map(_.head).toList

    assert(synthesized.nonEmpty, clue("expected non-empty synthesized fixture bank"))
    assert(StrategicObjectFamily.graphDerivedFamilies.forall(family => synthesized.exists(_.family == family)))
  }

  test("graph-derived objects carry relation and rival richness") {
    val graphRows =
      rows.filter(row => row.expectation == "present" && StrategicObjectFamily.graphDerivedFamilies.contains(parseFamily(row.family)))

    assert(graphRows.nonEmpty, clue("expected graph-derived rows"))
    graphRows.foreach { row =>
      val objects = objectsForRow(row)
      val matched = findMatches(row, objects)
      assert(matched.nonEmpty, clue(s"${row.id} expected graph object\n${render(objects)}"))
      matched.foreach { obj =>
        assert(obj.relations.nonEmpty, clue(s"${row.id}: expected graph relations"))
        assert(
          obj.rivalResourcesOrObjects.exists(_.kind == RivalReferenceKind.Object) || obj.stateStrength.coverage >= 2,
          clue(s"${row.id}: expected object rival or multi-primitive support")
        )
      }
    }
  }

  test("graph-derived objects only reuse primitive-bank references") {
    val graphRows =
      rows.filter(row => row.expectation == "present" && StrategicObjectFamily.graphDerivedFamilies.contains(parseFamily(row.family)))

    graphRows.foreach { row =>
      val primitiveRefs = primitiveRefsForRow(row).toSet
      val objects = objectsForRow(row)
      findMatches(row, objects).foreach { obj =>
        obj.supportingPrimitives.foreach { ref =>
          assert(primitiveRefs.contains(ref), clue(s"${row.id}: synthesized primitive ref not present in primitive bank: ${ref.kind}"))
        }
      }
    }
  }

  test("strategic object rejects mismatched family and profile pairs") {
    val sample =
      objectsForFen("6k1/8/8/4p3/3P4/2P5/8/6K1 w - - 0 1")
        .find(_.family == StrategicObjectFamily.BreakAxis)
        .getOrElse(fail("expected break axis sample"))

    intercept[IllegalArgumentException] {
      sample.copy(family = StrategicObjectFamily.TradeInvariant)
    }
  }

  rows.foreach { row =>
    test(s"object expectation ${row.id}") {
      val objects = objectsForRow(row)
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

  test("contrastive file duel yields bilateral access, shell, and race objects") {
    val objects = objectsForFen("2r3k1/8/8/8/8/8/8/2R3K1 w - - 0 1")

    assert(objects.exists(obj => obj.family == StrategicObjectFamily.AccessNetwork && obj.owner == Color.White))
    assert(objects.exists(obj => obj.family == StrategicObjectFamily.AccessNetwork && obj.owner == Color.Black))
    assert(objects.exists(obj => obj.family == StrategicObjectFamily.KingSafetyShell && obj.owner == Color.White))
    assert(objects.exists(obj => obj.family == StrategicObjectFamily.KingSafetyShell && obj.owner == Color.Black))
    assert(objects.exists(obj => obj.family == StrategicObjectFamily.PlanRace && obj.owner == Color.White))
    assert(objects.exists(obj => obj.family == StrategicObjectFamily.PlanRace && obj.owner == Color.Black))
  }

  test("contrastive break race yields bilateral structure, break, and tension objects") {
    val objects = objectsForFen("6k1/5p2/3p4/4P3/2P5/8/8/6K1 w - - 0 1")

    assert(objects.exists(obj => obj.family == StrategicObjectFamily.PawnStructureRegime && obj.owner == Color.White))
    assert(objects.exists(obj => obj.family == StrategicObjectFamily.PawnStructureRegime && obj.owner == Color.Black))
    assert(objects.exists(obj => obj.family == StrategicObjectFamily.BreakAxis && obj.owner == Color.White))
    assert(objects.exists(obj => obj.family == StrategicObjectFamily.BreakAxis && obj.owner == Color.Black))
    assert(objects.exists(obj => obj.family == StrategicObjectFamily.TensionState && obj.owner == Color.White))
    assert(objects.exists(obj => obj.family == StrategicObjectFamily.TensionState && obj.owner == Color.Black))
  }

  test("contrastive passer race yields bilateral passer and plan-race objects") {
    val objects = objectsForFen("6k1/2P5/8/8/8/8/5p2/6K1 w - - 0 1")

    assert(objects.exists(obj => obj.family == StrategicObjectFamily.PasserComplex && obj.owner == Color.White))
    assert(objects.exists(obj => obj.family == StrategicObjectFamily.PasserComplex && obj.owner == Color.Black))
    assert(objects.exists(obj => obj.family == StrategicObjectFamily.PlanRace && obj.owner == Color.White))
    assert(objects.exists(obj => obj.family == StrategicObjectFamily.PlanRace && obj.owner == Color.Black))
  }

  private def assertRichObject(
      row: ObjectExpectationRow,
      obj: StrategicObject,
      output: List[StrategicObject]
  ): Unit =
    assert(obj.id.nonEmpty, clue(s"${row.id}: expected object id"))
    assert(obj.anchors.nonEmpty, clue(s"${row.id}: expected anchors"))
    assert(obj.supportingPrimitives.nonEmpty, clue(s"${row.id}: expected supporting primitives"))
    assertEquals(obj.profile.family, obj.family, clue(s"${row.id}: profile family mismatch"))
    assert(obj.stateStrength.coverage == obj.supportingPrimitives.size, clue(s"${row.id}: coverage mismatch"))
    assert(obj.evidenceFootprint.primitiveCount == obj.supportingPrimitives.size, clue(s"${row.id}: primitive count mismatch"))
    assert(obj.evidenceFootprint.supportingPieceCount == obj.supportingPieces.size, clue(s"${row.id}: supporting piece count mismatch"))
    assert(obj.evidenceFootprint.rivalCount == obj.rivalResourcesOrObjects.size, clue(s"${row.id}: rival count mismatch"))
    assert(obj.evidenceFootprint.primitiveKinds.nonEmpty, clue(s"${row.id}: expected primitive kinds"))
    assert(
      obj.locus.route.nonEmpty || obj.locus.allSquares.nonEmpty || obj.locus.files.nonEmpty,
      clue(s"${row.id}: expected non-empty object locus")
    )
    assert(profileMatchesFamily(obj), clue(s"${row.id}: profile/family mismatch"))
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

  private def profileMatchesFamily(obj: StrategicObject): Boolean =
    (obj.family, obj.profile) match
      case (StrategicObjectFamily.PawnStructureRegime, StrategicObjectProfile.PawnStructureRegime(identity, _, _, _, _)) =>
        identity.nonEmpty
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
      case (StrategicObjectFamily.FixedTargetComplex, StrategicObjectProfile.FixedTargetComplex(_, _, occupantRoles, _, _)) =>
        occupantRoles.nonEmpty
      case (StrategicObjectFamily.BreakAxis, StrategicObjectProfile.BreakAxis(sourceSquare, breakSquare, targetSquares, _, _)) =>
        sourceSquare != breakSquare && targetSquares.nonEmpty
      case (StrategicObjectFamily.AccessNetwork, StrategicObjectProfile.AccessNetwork(lane, route, _, _)) =>
        lane.nonEmpty || route.nonEmpty
      case (StrategicObjectFamily.CounterplayAxis, StrategicObjectProfile.CounterplayAxis(resourceSquares, breakSquares, pressureSquares)) =>
        resourceSquares.nonEmpty || breakSquares.nonEmpty || pressureSquares.nonEmpty
      case (StrategicObjectFamily.RestrictionShell, StrategicObjectProfile.RestrictionShell(restrictedSquares, _, constraintSquares)) =>
        restrictedSquares.nonEmpty && constraintSquares.nonEmpty
      case (StrategicObjectFamily.MobilityCage, StrategicObjectProfile.MobilityCage(affectedPiece, deniedSquares, _)) =>
        affectedPiece.squares.nonEmpty && deniedSquares.nonEmpty
      case (StrategicObjectFamily.RedeploymentRoute, StrategicObjectProfile.RedeploymentRoute(route, _, _)) =>
        route.origin != route.target
      case (StrategicObjectFamily.DefenderDependencyNetwork, StrategicObjectProfile.DefenderDependencyNetwork(defendedSquares, defenderSquares, pressureSquares, defenderRoles, features)) =>
        defendedSquares.nonEmpty && defenderSquares.nonEmpty && pressureSquares.nonEmpty && defenderRoles.nonEmpty && features.nonEmpty
      case (StrategicObjectFamily.TradeInvariant, StrategicObjectProfile.TradeInvariant(exchangeSquares, invariantSquares, preservedFiles, preservedFamilies, features)) =>
        exchangeSquares.nonEmpty && invariantSquares.nonEmpty && preservedFiles.nonEmpty && preservedFamilies.nonEmpty && features.nonEmpty
      case (StrategicObjectFamily.TensionState, StrategicObjectProfile.TensionState(contactSquares, releaseSquares, pressureSquares, breakSquares, features)) =>
        contactSquares.nonEmpty && (releaseSquares.nonEmpty || pressureSquares.nonEmpty || breakSquares.nonEmpty) && features.nonEmpty
      case (StrategicObjectFamily.AttackScaffold, StrategicObjectProfile.AttackScaffold(_, scaffoldSquares, entryFiles, entryRoutes, features)) =>
        scaffoldSquares.nonEmpty && (entryFiles.nonEmpty || entryRoutes.nonEmpty) && features.nonEmpty
      case (StrategicObjectFamily.MaterialInvestmentContract, StrategicObjectProfile.MaterialInvestmentContract(investedMaterialCp, _, _, compensationSquares, compensationFiles, features)) =>
        investedMaterialCp >= 0 && compensationSquares.nonEmpty && compensationFiles.nonEmpty && features.nonEmpty
      case (StrategicObjectFamily.InitiativeWindow, StrategicObjectProfile.InitiativeWindow(windowSquares, triggerFiles, rivalPressureSquares, catalystFamilies, features)) =>
        windowSquares.nonEmpty && triggerFiles.nonEmpty && rivalPressureSquares.nonEmpty && catalystFamilies.nonEmpty && features.nonEmpty
      case (StrategicObjectFamily.PlanRace, StrategicObjectProfile.PlanRace(_, raceSquares, raceFiles, ownGoalSquares, rivalGoalSquares, features)) =>
        raceSquares.nonEmpty && raceFiles.nonEmpty && ownGoalSquares.nonEmpty && rivalGoalSquares.nonEmpty && features.nonEmpty
      case (StrategicObjectFamily.TransitionBridge, StrategicObjectProfile.TransitionBridge(bridgeSquares, bridgeFiles, sourceFamilies, destinationFamilies, features)) =>
        bridgeSquares.nonEmpty && bridgeFiles.nonEmpty && sourceFamilies.nonEmpty && destinationFamilies.nonEmpty && features.nonEmpty
      case (StrategicObjectFamily.ConversionFunnel, StrategicObjectProfile.ConversionFunnel(entrySquares, channelSquares, exitSquares, funnelFiles, features)) =>
        entrySquares.nonEmpty && channelSquares.nonEmpty && exitSquares.nonEmpty && funnelFiles.nonEmpty && features.nonEmpty
      case (StrategicObjectFamily.PasserComplex, StrategicObjectProfile.PasserComplex(passerSquare, promotionSquare, relativeRank, _, _)) =>
        passerSquare != promotionSquare && relativeRank > 0
      case (StrategicObjectFamily.FortressHoldingShell, StrategicObjectProfile.FortressHoldingShell(holdSquares, entryDeniedSquares, blockadeSquares, shellFiles, features)) =>
        holdSquares.nonEmpty && entryDeniedSquares.nonEmpty && (blockadeSquares.nonEmpty || shellFiles.nonEmpty) && features.nonEmpty
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
      tag: Option[String],
      truthCase: Option[String]
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

  def objectsForFen(
      fen: String,
      truth: MoveTruthFrame = PrimitiveExtractionTest.neutralTruthFrame
  ): List[StrategicObject] =
    val evidence = RawPositionEvidence.fromFen(fen).fold(err => throw new IllegalArgumentException(err), identity)
    val primitives =
      CanonicalPrimitiveExtractor.extract(evidence, truth, PrimitiveExtractionTest.neutralContract)
    CanonicalStrategicObjectSynthesizer.synthesize(primitives, truth)

  def objectsForRow(row: ObjectExpectationRow): List[StrategicObject] =
    objectsForFen(row.fen, truthFrame(row.truthCase))

  def primitiveRefsForRow(row: ObjectExpectationRow): List[PrimitiveReference] =
    val truth = truthFrame(row.truthCase)
    val evidence = RawPositionEvidence.fromFen(row.fen).fold(err => throw new IllegalArgumentException(err), identity)
    CanonicalPrimitiveExtractor
      .extract(evidence, truth, PrimitiveExtractionTest.neutralContract)
      .all
      .map(PrimitiveReference.fromPrimitive)
      .map(_.normalized)

  def truthFrame(truthCase: Option[String]): MoveTruthFrame =
    truthCase match
      case None | Some("neutral") =>
        PrimitiveExtractionTest.neutralTruthFrame
      case Some("investment") =>
        PrimitiveExtractionTest.neutralTruthFrame.copy(
          moveQuality = PrimitiveExtractionTest.neutralTruthFrame.moveQuality.copy(swingSeverity = 120, severityBand = "major"),
          materialEconomics =
            PrimitiveExtractionTest.neutralTruthFrame.materialEconomics.copy(
              investedMaterialCp = Some(300),
              beforeDeficit = 0,
              afterDeficit = 300,
              movingPieceValue = 300,
              capturedPieceValue = 0,
              sacrificeKind = Some("exchange"),
              overinvestment = true
            )
        )
      case Some("forcing") =>
        PrimitiveExtractionTest.neutralTruthFrame.copy(
          tactical = PrimitiveExtractionTest.neutralTruthFrame.tactical.copy(forcingLine = true),
          difficultyNovelty = PrimitiveExtractionTest.neutralTruthFrame.difficultyNovelty.copy(depthSensitive = true),
          moveQuality = PrimitiveExtractionTest.neutralTruthFrame.moveQuality.copy(swingSeverity = 60, severityBand = "forcing")
        )
      case Some("forcing-investment") =>
        truthFrame(Some("investment")).copy(
          tactical = truthFrame(Some("investment")).tactical.copy(forcingLine = true),
          difficultyNovelty = truthFrame(Some("investment")).difficultyNovelty.copy(depthSensitive = true)
        )
      case Some(other) =>
        throw new IllegalArgumentException(s"unsupported truthCase=$other")

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
