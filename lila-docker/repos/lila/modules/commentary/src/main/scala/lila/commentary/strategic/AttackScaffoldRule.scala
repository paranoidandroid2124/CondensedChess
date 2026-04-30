package lila.commentary.strategic

import chess.{ Color, Square }

import lila.commentary.root.RootAtomRegistry.SchemaId
import lila.commentary.witness.{ WitnessAnchor, WitnessPayload, WitnessValue }
import lila.commentary.strategic.StrategicObjectHelpers.*

private[strategic] object AttackScaffoldRule extends StrategicObjectRule:

  val familyId: StrategicObjectId = StrategicObjectId("AttackScaffold")

  def extract(
      context: StrategicObjectContext,
      extractedSoFar: StrategicObjectSet
  ): Vector[StrategicObject] =
    Vector(Color.White, Color.Black).flatMap: attacker =>
      val defender = !attacker
      context.board.kingSquare(defender).toVector.flatMap: kingSquare =>
        val carriers = carrierFragments(context, attacker, defender)
        val supports = supportFragments(context, attacker, defender, carriers)
        val holeOnlySupport = supports.nonEmpty && supports.forall(_.kind == "king_shelter_hole")
        val supportKinds = supports.map(_.kind).toSet
        val pinnedAndShelterOnlySupport =
          supportKinds.nonEmpty && supportKinds.subsetOf(Set("king_shelter_hole", "pinned_piece"))
        val carrierAdmissionUnits = carriers.map(_.admissionUnit).distinct.sorted
        val carrierSourceSquares = carriers.flatMap(_.sourceSquares).distinct.sortBy(_.value)
        val carrierSquares = carriers.flatMap(_.squares).distinct.sortBy(_.value)
        val carrierEntrySquares = carrierSquares.filterNot(carrierSourceSquares.toSet)
        val supportSquares = supports.flatMap(_.squares).distinct.sortBy(_.value)
        val looseSupportSquares = supportSquaresFor(supports, "loose_piece")
        val pinnedSupportSquares = supportSquaresFor(supports, "pinned_piece")

        Option.when(
          carriers.nonEmpty &&
            supports.nonEmpty &&
            (!holeOnlySupport || carrierAdmissionUnits.size >= 2 || carrierEntrySquares.size >= 2) &&
            !pinnedAndShelterOnlySupport
        )(
          owned(
            color = attacker,
            anchor = WitnessAnchor.SquareAnchor(kingSquare),
            payload = WitnessPayload(
              "attacker" -> WitnessValue.ColorValue(attacker),
              "defender" -> WitnessValue.ColorValue(defender),
              "king_square" -> WitnessValue.SquareValue(kingSquare),
              "carrier_fragment_ids" -> WitnessValue.TokenListValue(carriers.map(_.kind).distinct.sorted),
              "carrier_source_squares" -> WitnessValue.SquareListValue(carrierSourceSquares),
              "support_fragment_ids" -> WitnessValue.TokenListValue(supports.map(_.kind).distinct.sorted),
              "carrier_squares" -> WitnessValue.SquareListValue(carrierSquares),
              "support_squares" -> WitnessValue.SquareListValue(supportSquares),
              "loose_support_squares" -> WitnessValue.SquareListValue(looseSupportSquares),
              "pinned_support_squares" -> WitnessValue.SquareListValue(pinnedSupportSquares)
            ),
            support = support(
              indices = (carriers.flatMap(_.rootIndices) ++ supports.flatMap(_.rootIndices)).distinct.sorted,
              targetSquares = carrierSquares ++ supportSquares
            )
          )
        ).toVector

  private final case class AttackFragment(
      kind: String,
      admissionUnit: String,
      sourceSquares: Vector[chess.Square],
      squares: Vector[chess.Square],
      rootIndices: Vector[Int]
  )

  private def supportSquaresFor(supports: Vector[AttackFragment], kind: String): Vector[Square] =
    supports.filter(_.kind == kind).flatMap(_.squares).distinct.sortBy(_.value)

  private def carrierFragments(
      context: StrategicObjectContext,
      attacker: Color,
      defender: Color
  ): Vector[AttackFragment] =
    val kingRing = context.kingRingSquaresFor(defender).toSet
    val entryAxisFragments =
      entryAxesIntoMask(context, attacker, kingRing).map: axis =>
        AttackFragment(
          kind = axis.hostId,
          admissionUnit = carrierAdmissionUnit(axis.hostId, axis.sourcePieceSquares),
          sourceSquares = axis.sourcePieceSquares,
          squares = (axis.sourcePieceSquares ++ axis.entrySquares).distinct.sortBy(_.value),
          rootIndices =
            (occupiedPieceRootIndices(context, axis.sourcePieceSquares) ++
              rootIndicesForSquares(
                context,
                SchemaId.ControlledBy,
                attacker,
                axis.entrySquares ++ axis.feederSquares
              )).distinct.sorted
        )

    val theaterMask = kingRingAndShelterMask(context, defender)
    val rookFragments =
      rookOnOpenFileWitnesses(context, attacker).flatMap: witness =>
        witness.anchor match
          case WitnessAnchor.PieceSquareAnchor(rookSquare)
              if context.board.kingSquare(defender).exists(king =>
                sameAndAdjacentFiles(king.file).contains(rookSquare.file)
              ) && theaterMask.exists(target => context.board.attacksSquare(rookSquare, target)) =>
            Some(
              AttackFragment(
                kind = "rook_on_open_file_state",
                admissionUnit = carrierAdmissionUnit("rook_on_open_file_state", Vector(rookSquare)),
                sourceSquares = Vector(rookSquare),
                squares = Vector(rookSquare),
                rootIndices = witness.support.rootIndices
              )
            )
          case _ => None

    (entryAxisFragments ++ rookFragments).sortBy(fragment => (fragment.squares.head.value, fragment.kind))

  private def supportFragments(
      context: StrategicObjectContext,
      attacker: Color,
      defender: Color,
      carriers: Vector[AttackFragment]
  ): Vector[AttackFragment] =
    val theaterMask = kingRingAndShelterMask(context, defender)

    val shelterHoles = homeShelterHoles(context, defender)
    val holeFragments =
      Option.when(shelterHoles.nonEmpty):
        AttackFragment(
          kind = "king_shelter_hole",
          admissionUnit = "support:king_shelter_hole",
          sourceSquares = Vector.empty,
          squares = shelterHoles,
          rootIndices = rootIndicesForSquares(context, SchemaId.KingShelterHole, attacker, shelterHoles)
        )
      .toVector

    val dutyFragments =
      context.primaryWitnessesFor("duty_bound_defender").flatMap: witness =>
        Option.when(witness.color.contains(attacker)):
          val squares =
            witnessSquares(witness.payload, "assigned_duty_squares", "king_gate_duty_squares")
              .filter(theaterMask.contains)
          AttackFragment(
            kind = "duty_bound_defender",
            admissionUnit = "support:duty_bound_defender",
            sourceSquares = Vector.empty,
            squares = squares,
            rootIndices = witness.support.rootIndices
          )
      .filter(_.squares.nonEmpty)

    val restrictionFragments =
      context.primaryWitnessesFor("short_run_slider_gate_restriction").flatMap: witness =>
        Option.when(witness.color.contains(attacker)):
          val squares =
            witnessSquares(
              witness.payload,
              "beneficiary_occupied_gate_squares",
              "beneficiary_controlled_gate_squares"
            ).filter(theaterMask.contains)
          AttackFragment(
            kind = "short_run_slider_gate_restriction",
            admissionUnit = "support:short_run_slider_gate_restriction",
            sourceSquares = Vector.empty,
            squares = squares,
            rootIndices = witness.support.rootIndices
          )
      .filter(_.squares.nonEmpty)

    val xrayFragments =
      // A standing x-ray root does not prove that this king-attack carrier created,
      // released, or currently owns the target relation.
      Vector.empty[AttackFragment]

    val pinnedFragments =
      val squares =
        absolutePinCarrierBoundSupportSquares(
          context,
          attacker,
          defender,
          carriers,
          rootSquares(context, SchemaId.PinnedPiece, defender).filter(theaterMask.contains)
        )
      Option.when(squares.nonEmpty):
        AttackFragment(
          kind = "pinned_piece",
          admissionUnit = "support:pinned_piece",
          sourceSquares = Vector.empty,
          squares = squares,
          rootIndices = rootIndicesForSquares(context, SchemaId.PinnedPiece, defender, squares)
        )
      .toVector

    val looseFragments =
      val squares =
        directCarrierBoundTacticalSupportSquares(
          context,
          carriers,
          rootSquares(context, SchemaId.LoosePiece, defender).filter(theaterMask.contains)
        )
      Option.when(squares.nonEmpty):
        AttackFragment(
          kind = "loose_piece",
          admissionUnit = "support:loose_piece",
          sourceSquares = Vector.empty,
          squares = squares,
          rootIndices = rootIndicesForSquares(context, SchemaId.LoosePiece, defender, squares)
        )
      .toVector

    (holeFragments ++ dutyFragments ++ restrictionFragments ++ xrayFragments ++ pinnedFragments ++ looseFragments)
      .sortBy(fragment => (fragment.squares.head.value, fragment.kind))

  private def directCarrierBoundTacticalSupportSquares(
      context: StrategicObjectContext,
      carriers: Vector[AttackFragment],
      squares: Vector[Square]
  ): Vector[Square] =
    squares
      .filter(square => carriers.exists(carrier => carrierSourceDirectlyAttacks(context, carrier, square)))
      .distinct
      .sortBy(_.value)

  private def absolutePinCarrierBoundSupportSquares(
      context: StrategicObjectContext,
      attacker: Color,
      defender: Color,
      carriers: Vector[AttackFragment],
      squares: Vector[Square]
  ): Vector[Square] =
    val carrierSources = carriers.flatMap(_.sourceSquares).toSet
    context.board.kingSquare(defender).toVector.flatMap: kingSquare =>
      squares.filter: square =>
        context.primaryWitnessesFor("pin").exists: witness =>
          witness.color.contains(attacker) &&
            context.token(witness.payload, "pin_mode").contains("absolute_king_pin") &&
            witnessSquares(witness.payload, "attacker_square").exists(carrierSources.contains) &&
            witnessSquares(witness.payload, "blocker_square").contains(square) &&
            witnessSquares(witness.payload, "anchor_square").contains(kingSquare)
    .distinct
      .sortBy(_.value)

  private def carrierSourceDirectlyAttacks(
      context: StrategicObjectContext,
      carrier: AttackFragment,
      square: Square
  ): Boolean =
    carrier.sourceSquares.exists(source => context.board.attacksSquare(source, square))

  private def carrierAdmissionUnit(
      kind: String,
      sourceSquares: Vector[chess.Square]
  ): String =
    kind match
      case "file_lane_state" | "rook_on_open_file_state" =>
        s"file:${sourceSquares.headOption.map(_.file.value.toString).getOrElse("none")}"
      case _ =>
        s"$kind:${sourceSquares.map(_.key).mkString("+")}"
