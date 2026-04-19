package lila.commentary.witness.u

import chess.{ Bishop, Color, Knight, Queen, Rook, Square }

import lila.commentary.root.RootAtomRegistry.{ SchemaId, canonicalColors }
import lila.commentary.witness.*
import lila.commentary.witness.u.UWitnessHelpers.*

private[u] enum RayRunEnd:
  case Edge
  case DefenderBlocker
  case BeneficiaryBlocker(square: Square)

private[u] final case class RayRun(squares: Vector[Square], end: RayRunEnd)

private[u] object DutyBoundDefenderRule extends UScopedWitnessRule:

  val descriptorId: WitnessDescriptorId = WitnessDescriptorId("duty_bound_defender")

  def extract(context: UExtractionContext): Vector[Witness] =
    canonicalColors.flatMap: defenderColor =>
      val beneficiaryColor = !defenderColor
      val pinnedAnchors = context.pinnedPieceSquaresFor(defenderColor).toSet
      val trappedAnchors = context.trappedPieceSquaresFor(defenderColor).toSet

      anchoredDutySquares(context, defenderColor, beneficiaryColor).flatMap:
        case (anchorSquare, occupiedPressureSquares, kingGateSquares, assignedSquares) =>
          val modes =
            Vector(
              Option.when(pinnedAnchors.contains(anchorSquare))("pin_bound_duty"),
              Option.when(trappedAnchors.contains(anchorSquare))("trapped_bound_duty")
            ).flatten

          Option.when(modes.nonEmpty && assignedSquares.nonEmpty):
            val anchorPiece = context.pieceAt(anchorSquare).get
            beneficiary(
              color = beneficiaryColor,
              anchor = WitnessAnchor.PieceSquareAnchor(anchorSquare),
              payload = WitnessPayload(
                "anchor_piece_square" -> WitnessValue.SquareValue(anchorSquare),
                "beneficiary" -> WitnessValue.ColorValue(beneficiaryColor),
                "bound_modes" -> WitnessValue.TokenListValue(modes),
                "assigned_duty_squares" -> WitnessValue.SquareListValue(assignedSquares),
                "occupied_pressure_duty_squares" -> WitnessValue.SquareListValue(occupiedPressureSquares),
                "king_gate_duty_squares" -> WitnessValue.SquareListValue(kingGateSquares)
              ),
              support = rootSupport(
                indices = Vector(
                  context.pieceOnRootIndex(defenderColor, anchorPiece.role, anchorSquare),
                  Option.when(pinnedAnchors.contains(anchorSquare))(context.colorSquareRootIndex(SchemaId.PinnedPiece, defenderColor, anchorSquare)).flatten,
                  Option.when(trappedAnchors.contains(anchorSquare))(context.colorSquareRootIndex(SchemaId.TrappedPiece, defenderColor, anchorSquare)).flatten
                ).flatten ++ dutyRootIndices(context, beneficiaryColor, defenderColor, occupiedPressureSquares, kingGateSquares),
                targetSquares = assignedSquares
              )
            )

  private def anchoredDutySquares(
      context: UExtractionContext,
      defenderColor: Color,
      beneficiaryColor: Color
  ): Vector[(Square, Vector[Square], Vector[Square], Vector[Square])] =
    anchoredDefenders(context, defenderColor).flatMap: anchorSquare =>
      val occupiedPressureSquares =
        context.board.squaresOf(defenderColor).filter: square =>
          context.hasColorSquare(SchemaId.ControlledBy, beneficiaryColor, square) ||
            context.hasColorSquare(SchemaId.XrayTarget, beneficiaryColor, square)

      val kingGateSquares =
        context.kingRingSquaresFor(defenderColor).filter(square =>
          context.hasColorSquare(SchemaId.ControlledBy, beneficiaryColor, square) &&
            context.hasNeutralSquare(SchemaId.Contested, square)
        )

      val assignedSquares =
        (occupiedPressureSquares ++ kingGateSquares)
          .filterNot(_ == anchorSquare)
          .filter(square => coversFromCurrentGeometry(context, anchorSquare, square))
          .distinct
          .sortBy(_.value)

      Option.when(assignedSquares.nonEmpty)(
        (
          anchorSquare,
          occupiedPressureSquares.distinct.sortBy(_.value),
          kingGateSquares.distinct.sortBy(_.value),
          assignedSquares
        )
      )

  private def anchoredDefenders(context: UExtractionContext, defenderColor: Color): Vector[Square] =
    Vector(Knight, Bishop, Rook, Queen).flatMap(role => context.activePieceSquares(defenderColor, role))

  private def dutyRootIndices(
      context: UExtractionContext,
      beneficiaryColor: Color,
      defenderColor: Color,
      occupiedPressureSquares: Vector[Square],
      kingGateSquares: Vector[Square]
  ): Vector[Int] =
    occupiedPressureSquares.flatMap: square =>
      Vector(
        context.colorSquareRootIndex(SchemaId.ControlledBy, beneficiaryColor, square),
        context.colorSquareRootIndex(SchemaId.XrayTarget, beneficiaryColor, square)
      ).flatten
    ++ kingGateSquares.flatMap: square =>
      Vector(
        context.colorSquareRootIndex(SchemaId.ControlledBy, beneficiaryColor, square),
        context.colorSquareRootIndex(SchemaId.KingRingSquare, defenderColor, square),
        context.neutralSquareRootIndex(SchemaId.Contested, square)
      ).flatten

private[u] object ShortRunSliderGateRestrictionRule extends UScopedWitnessRule:

  val descriptorId: WitnessDescriptorId = WitnessDescriptorId("short_run_slider_gate_restriction")

  def extract(context: UExtractionContext): Vector[Witness] =
    canonicalColors.flatMap: defenderColor =>
      val beneficiaryColor = !defenderColor
      anchoredSliders(context, defenderColor).flatMap: anchorSquare =>
        context.pieceAt(anchorSquare).flatMap: anchorPiece =>
          val testableDirections =
            context.board.sliderDirections(anchorPiece.role).filter(direction =>
              direction.step(anchorSquare).exists(square => context.pieceAt(square).forall(_.color != defenderColor))
            )

          val runsByDirection = testableDirections.map(direction => direction -> legalRayRun(context, anchorSquare, defenderColor, direction)).toMap
          val throttledDirections =
            testableDirections.filter(direction => isShortRunThrottled(context, beneficiaryColor, runsByDirection(direction)))
          val nonThrottledDirections = testableDirections.filterNot(throttledDirections.contains)

          Option.when(
            testableDirections.size >= 2 &&
              throttledDirections.size >= 2 &&
              throttledDirections.size * 2 >= testableDirections.size &&
              nonThrottledDirections.nonEmpty
          ):
            val beneficiaryOccupiedGateSquares =
              throttledDirections.flatMap(direction =>
                runsByDirection(direction).squares.filter(square => context.pieceAt(square).exists(_.color == beneficiaryColor))
              ).distinct.sortBy(_.value)
            val beneficiaryControlledGateSquares =
              throttledDirections.flatMap(direction =>
                runsByDirection(direction).squares.filter(square => context.hasColorSquare(SchemaId.ControlledBy, beneficiaryColor, square))
              ).distinct.sortBy(_.value)

            beneficiary(
              color = beneficiaryColor,
              anchor = WitnessAnchor.PieceSquareAnchor(anchorSquare),
              payload = WitnessPayload(
                "anchor_piece_square" -> WitnessValue.SquareValue(anchorSquare),
                "beneficiary" -> WitnessValue.ColorValue(beneficiaryColor),
                "throttled_directions" -> WitnessValue.DirectionListValue(throttledDirections.toVector),
                "testable_directions" -> WitnessValue.DirectionListValue(testableDirections.toVector),
                "ray_mobility_by_direction" -> objectList(
                  testableDirections.map(direction =>
                    WitnessPayload(
                      "direction" -> WitnessValue.DirectionValue(direction),
                      "mobility" -> WitnessValue.Number(runsByDirection(direction).squares.size)
                    )
                  )
                ),
                "beneficiary_occupied_gate_squares" -> WitnessValue.SquareListValue(beneficiaryOccupiedGateSquares),
                "beneficiary_controlled_gate_squares" -> WitnessValue.SquareListValue(beneficiaryControlledGateSquares),
                "open_testable_directions" -> WitnessValue.DirectionListValue(
                  testableDirections.filter(direction => direction.step(anchorSquare).exists(square => context.pieceAt(square).isEmpty)).toVector
                )
              ),
              support = rootSupport(
                indices = Vector(
                  context.pieceOnRootIndex(defenderColor, anchorPiece.role, anchorSquare)
                ).flatten ++ beneficiaryControlledGateSquares.flatMap(square =>
                  context.colorSquareRootIndex(SchemaId.ControlledBy, beneficiaryColor, square)
                ),
                targetSquares = beneficiaryOccupiedGateSquares ++ beneficiaryControlledGateSquares
              )
            )

  private def anchoredSliders(context: UExtractionContext, defenderColor: Color): Vector[Square] =
    Vector(Bishop, Rook, Queen).flatMap(role => context.activePieceSquares(defenderColor, role))

  private def legalRayRun(
      context: UExtractionContext,
      anchorSquare: Square,
      defenderColor: Color,
      direction: WitnessDirection
  ): RayRun =
    val builder = Vector.newBuilder[Square]
    var next = direction.step(anchorSquare)
    var done = false
    var end: RayRunEnd = RayRunEnd.Edge
    while next.nonEmpty && !done do
      val square = next.get
      context.pieceAt(square) match
        case Some(piece) if piece.color == defenderColor =>
          end = RayRunEnd.DefenderBlocker
          done = true
        case Some(_) =>
          builder += square
          end = RayRunEnd.BeneficiaryBlocker(square)
          done = true
        case None =>
          builder += square
          next = direction.step(square)
          if next.isEmpty then end = RayRunEnd.Edge
    RayRun(builder.result(), end)

  private def isShortRunThrottled(
      context: UExtractionContext,
      beneficiaryColor: Color,
      run: RayRun
  ): Boolean =
    run.squares.nonEmpty &&
      run.squares.size <= 2 &&
      run.end != RayRunEnd.Edge &&
      run.squares.forall(square => claimedBy(context, beneficiaryColor, square)) &&
      run.squares.exists(square => context.hasColorSquare(SchemaId.ControlledBy, beneficiaryColor, square)) &&
      (run.end match
        case RayRunEnd.BeneficiaryBlocker(square) =>
          context.hasColorSquare(SchemaId.ControlledBy, beneficiaryColor, square)
        case _ => true)

private[u] def coversFromCurrentGeometry(
    context: UExtractionContext,
    anchorSquare: Square,
    targetSquare: Square
): Boolean =
  targetSquare != anchorSquare &&
    context.pieceAt(anchorSquare).exists: piece =>
      piece.role match
        case Knight =>
          anchorSquare.knightAttacks.contains(targetSquare)
        case Bishop =>
          anchorSquare.onSameDiagonal(targetSquare) && context.board.occupiedBetween(anchorSquare, targetSquare).isEmpty
        case Rook =>
          anchorSquare.onSameLine(targetSquare) && context.board.occupiedBetween(anchorSquare, targetSquare).isEmpty
        case Queen =>
          (anchorSquare.onSameLine(targetSquare) || anchorSquare.onSameDiagonal(targetSquare)) &&
            context.board.occupiedBetween(anchorSquare, targetSquare).isEmpty
        case _ => false

private[u] def claimedBy(
    context: UExtractionContext,
    beneficiaryColor: Color,
    square: Square
): Boolean =
  context.pieceAt(square).exists(_.color == beneficiaryColor) ||
    context.hasColorSquare(SchemaId.ControlledBy, beneficiaryColor, square)
