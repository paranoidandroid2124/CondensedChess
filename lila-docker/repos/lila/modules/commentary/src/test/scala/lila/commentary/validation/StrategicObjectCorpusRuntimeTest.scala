package lila.commentary.validation

import chess.{ Color, Rank, Square }

import lila.commentary.CommentaryCore
import lila.commentary.root.RootAtomRegistry.SchemaId
import lila.commentary.strategic.*
import lila.commentary.strategic.StrategicObjectHelpers.*
import lila.commentary.witness.{ WitnessAnchor, WitnessSector, WitnessValue }

class StrategicObjectCorpusRuntimeTest extends munit.FunSuite:

  private val rows = ObjectExpectationCorpus.loadAll()

  test("public strategic-object families stay aligned with the frozen object corpus"):
    assertEquals(
      CommentaryCore.activeObjectFamilyIds,
      ObjectExpectationCorpus.requiredFamilies
    )

  rows.foreach: row =>
    test(s"strategic object extraction matches ${row.id}"):
      row.validatedCaseType
      row.validatedExpectation
      row.validatedFamily
      row.validatedOwner
      row.anchorKind
      row.validatedPressureTarget
      row.validatedHelpers

      val extraction =
        CommentaryCore.extractStrategicObjects(row.normalizedFen).fold(message => fail(message), identity)
      val familyObjects = extraction.objects.forFamilyId(row.family)
      val expectedAnchor = row.expectedAnchor
      val expectedColor = row.expectedColor
      val context =
        StrategicObjectContext(
          extraction.rootState,
          extraction.primaryWitnesses,
          extraction.attachedWitnesses
        )

      row.expectation match
        case "present" =>
          val matchingObjects =
            familyObjects.filter(obj => obj.anchor == expectedAnchor && obj.color == expectedColor)
          assert(
            familyObjects.nonEmpty,
            s"${row.id} expected ${row.family} to be present but extraction was empty"
          )
          assert(
            matchingObjects.nonEmpty,
            s"${row.id} expected ${row.family} at ${expectedAnchor.kind.key}:${expectedAnchor.key} with color ${row.owner}"
          )
          assertEquals(
            familyObjects,
            matchingObjects,
            clues(s"${row.id} returned extra ${row.family} object identities outside the frozen expectation")
          )
          assertPressureTargetEvidence(row, context, extraction.objects, matchingObjects.headOption)
        case "absent" =>
          assert(
            familyObjects.isEmpty,
            s"${row.id} expected ${row.family} to stay absent but extraction returned ${familyObjects.size} object(s)"
          )
          assertPressureTargetEvidence(row, context, extraction.objects, None)

  private def assertPressureTargetEvidence(
      row: ObjectExpectationCorpus.Row,
      context: StrategicObjectContext,
      objects: StrategicObjectSet,
      matchedObject: Option[StrategicObject]
  ): Unit =
    row.pressureTarget match
      case "positive_window" =>
        assert(openingDevelopmentWindow(context))
      case "no_longer_development" =>
        assert(!openingDevelopmentWindow(context))
        assert(hasNonPawnDevelopmentOffHomeRank(context, Color.White))
        assert(hasNonPawnDevelopmentOffHomeRank(context, Color.Black))
      case "rival_object_preempts_opening" =>
        assert(openingDevelopmentWindow(context))
        assert(
          objects.forFamilyId("DistributedContactRegime").nonEmpty ||
            objects.forFamilyId("CentralContactFront").nonEmpty ||
            objects.forFamilyId("EndgameRaceScaffold").nonEmpty
        )
      case "multi_sector_contact" =>
        val admitted = distributedContactComponents(context)
        assert(admitted.map(_._1).distinct.size >= 2)
        assert(admitted.exists(_._2.liesOutsideCenterSector))
      case "central_only_contact" =>
        val admitted = distributedContactComponents(context)
        assert(admitted.nonEmpty)
        assertEquals(admitted.map(_._1).distinct.toSet, Set(WitnessSector.Center))
      case "multi_sector_contested_without_occupied_contact" =>
        val contestedOnly = distributedContestedOnlyComponents(context)
        assert(contestedOnly.map(_._1).distinct.size >= 2)
        assert(contestedOnly.exists(_._2.liesOutsideCenterSector))
      case "dual_unblocked_run" =>
        assert(noQueensRemain(context))
        assert(advancedRunResources(context, Color.White).nonEmpty)
        assert(advancedRunResources(context, Color.Black).nonEmpty)
        assert(clearRunResources(context, Color.White).nonEmpty)
        assert(clearRunResources(context, Color.Black).nonEmpty)
      case "one_sided_run" =>
        assert(noQueensRemain(context))
        assert(advancedRunResources(context, Color.White).nonEmpty ^ advancedRunResources(context, Color.Black).nonEmpty)
      case "direct_blockade" =>
        assert(noQueensRemain(context))
        assert(advancedRunResources(context, Color.White).nonEmpty)
        assert(advancedRunResources(context, Color.Black).nonEmpty)
        assert(clearRunResources(context, Color.White).isEmpty || clearRunResources(context, Color.Black).isEmpty)
      case "carrier_plus_support_same_king" =>
        val obj = matchedObject.getOrElse(fail(s"${row.id} missing expected object"))
        assert(tokenList(obj.payload, "carrier_fragment_ids").nonEmpty)
        assert(tokenList(obj.payload, "support_fragment_ids").nonEmpty)
      case "off_theater_pressure" =>
        val attacker = row.expectedColor.get
        val defender = !attacker
        assert(carrierSourceSquares(context, attacker).nonEmpty)
        assert(carrierSourceSquares(context, attacker).forall(square =>
          !sameAndAdjacentFiles(context.board.kingSquare(defender).get.file).contains(square.file)
        ))
      case "carrier_only_pressure" =>
        val attacker = row.expectedColor.get
        assert(carrierSourceSquares(context, attacker).nonEmpty)
        assert(theaterSupportSquares(context, attacker, !attacker).isEmpty)
      case "sealed_shell_no_entry" =>
        val obj = matchedObject.getOrElse(fail(s"${row.id} missing expected object"))
        assert(squareList(obj.payload, "occupied_shell_squares").size >= 2)
      case "insufficient_shell_occupancy" =>
        val holder = row.expectedColor.get
        val shellMask = fortressShellMask(context, holder)
        assert(occupiedNonKingSquares(context, holder, shellMask).size < 2)
      case "direct_file_entry" =>
        val holder = row.expectedColor.get
        val shellMask = fortressShellMask(context, holder)
        assert(
          entryAxesIntoMask(context, !holder, shellMask).nonEmpty ||
            neighboringFileMajorEntry(context, holder, shellMask)
        )
      case "adjacent_double_hole" =>
        val defender = row.expectedColor.get
        assert(homeShelterHoles(context, defender).size >= 2)
        assert(bestEdgeAdjacentHolePair(context, defender).nonEmpty)
      case "single_hole_only" =>
        val defender = row.expectedColor.get
        assertEquals(homeShelterHoles(context, defender).size, 1)
      case "non_adjacent_holes_only" =>
        val defender = row.expectedColor.get
        assert(homeShelterHoles(context, defender).nonEmpty)
        assert(bestEdgeAdjacentHolePair(context, defender).isEmpty)
      case "connected_central_front" =>
        val obj = matchedObject.getOrElse(fail(s"${row.id} missing expected object"))
        assert(squareList(obj.payload, "component_squares").size >= 2)
        assert(squareList(obj.payload, "contested_squares").nonEmpty)
        assert(squareList(obj.payload, "occupied_contact_squares").nonEmpty)
      case "single_square_touch" =>
        assert(contactComponents(context, centralSectorMask).exists(_.squares.size == 1))
      case "connected_center_without_occupied_contact" =>
        assert(
          contactComponents(context, centralSectorMask).exists(component =>
            component.squares.size >= 2 &&
              component.contestedSquares.nonEmpty &&
              component.occupiedContactSquares.isEmpty &&
              component.contributingColors.size == 2
          )
        )
      case other =>
        fail(s"Unhandled pressureTarget evidence check: $other")

  private def distributedGeometryComponents(
      context: StrategicObjectContext
  ): Vector[(WitnessSector, ContactComponent)] =
    Vector(WitnessSector.Queenside, WitnessSector.Center, WitnessSector.Kingside).flatMap: sector =>
      contactComponents(context, square => sectorMask(sector, square))
        .filter(component =>
          component.squares.size >= 2 &&
            component.contestedSquares.nonEmpty &&
            component.occupiedContactSquares.nonEmpty &&
            component.contributingColors == Set(Color.White, Color.Black)
        )
        .map(component => sector -> component)

  private def distributedContactComponents(
      context: StrategicObjectContext
  ): Vector[(WitnessSector, ContactComponent)] =
    Option.when(
      hasNonPawnDevelopmentOffHomeRank(context, Color.White) &&
        hasNonPawnDevelopmentOffHomeRank(context, Color.Black)
    )(distributedGeometryComponents(context)).getOrElse(Vector.empty)

  private def distributedContestedOnlyComponents(
      context: StrategicObjectContext
  ): Vector[(WitnessSector, ContactComponent)] =
    Option.when(
      hasNonPawnDevelopmentOffHomeRank(context, Color.White) &&
        hasNonPawnDevelopmentOffHomeRank(context, Color.Black)
    )(
      Vector(WitnessSector.Queenside, WitnessSector.Center, WitnessSector.Kingside).flatMap: sector =>
        contactComponents(context, square => sectorMask(sector, square))
          .filter(component =>
            component.squares.size >= 2 &&
              component.contestedSquares.nonEmpty &&
              component.occupiedContactSquares.isEmpty &&
              component.contributingColors == Set(Color.White, Color.Black)
          )
          .map(component => sector -> component)
    ).getOrElse(Vector.empty)

  private def advancedRunResources(
      context: StrategicObjectContext,
      color: Color
  ): Vector[Square] =
    context.activePieceSquares(color, chess.Pawn).filter: square =>
      if color.white then square.rank >= Rank.Fifth else square.rank <= Rank.Fourth

  private def clearRunResources(
      context: StrategicObjectContext,
      color: Color
  ): Vector[Square] =
    advancedRunResources(context, color).filter(square => forwardRunClear(context, color, square))

  private def carrierSourceSquares(
      context: StrategicObjectContext,
      attacker: Color
  ): Vector[Square] =
    val laneSources =
      context.primaryWitnessesFor("diagonal_lane_only").flatMap(witness =>
        context.squareList(witness.payload, "source_piece_squares").filter(square =>
          context.pieceAt(square).exists(_.color == attacker)
        )
      )
    val rookSources =
      rookOnOpenFileWitnesses(context, attacker).flatMap: witness =>
        witness.anchor match
          case WitnessAnchor.PieceSquareAnchor(square) => Vector(square)
          case _ => Vector.empty
    (laneSources ++ rookSources).distinct.sortBy(_.value)

  private def theaterSupportSquares(
      context: StrategicObjectContext,
      attacker: Color,
      defender: Color
  ): Vector[Square] =
    val theaterMask = kingRingAndShelterMask(context, defender)
    (
      homeShelterHoles(context, defender) ++
        rootSquares(context, SchemaId.XrayTarget, attacker).filter(theaterMask.contains) ++
        rootSquares(context, SchemaId.PinnedPiece, defender).filter(theaterMask.contains) ++
        rootSquares(context, SchemaId.LoosePiece, defender).filter(theaterMask.contains)
    ).distinct.sortBy(_.value)

  private def neighboringFileMajorEntry(
      context: StrategicObjectContext,
      holder: Color,
      shellMask: Set[Square]
  ): Boolean =
    kingNeighborhoodFiles(context, holder).exists: file =>
      fileLaneWitnessOnFile(context, file).nonEmpty &&
        context
          .activePieceSquares(!holder, chess.Rook)
          .concat(context.activePieceSquares(!holder, chess.Queen))
          .filter(_.file == file)
          .exists(origin => shellMask.exists(target => context.board.attacksSquare(origin, target)))

  private def tokenList(payload: lila.commentary.witness.WitnessPayload, field: String): Vector[String] =
    payload.get(field).collect { case WitnessValue.TokenListValue(values) => values }.getOrElse(Vector.empty)

  private def squareList(payload: lila.commentary.witness.WitnessPayload, field: String): Vector[Square] =
    payload.get(field).collect { case WitnessValue.SquareListValue(values) => values }.getOrElse(Vector.empty)
