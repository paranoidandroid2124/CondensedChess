package lila.commentary.witness.u

import chess.{ Bishop, Color, King, Queen, Role, Rook, Square }

import lila.commentary.root.RootAtomRegistry.{ SchemaId, canonicalColors }
import lila.commentary.witness.*
import lila.commentary.witness.u.UWitnessHelpers.*

private[u] object PinRule extends UScopedWitnessRule:

  val descriptorId: WitnessDescriptorId = WitnessDescriptorId("pin")

  def extract(context: UExtractionContext): Vector[Witness] =
    canonicalColors.flatMap: beneficiaryColor =>
      sliderSquares(context, beneficiaryColor).flatMap: attackerSquare =>
        val attackerRole = context.pieceAt(attackerSquare).get.role
        context.board.sliderDirections(attackerRole).flatMap: direction =>
          val ray = WitnessRay(attackerSquare, direction)
          val lineSquares = context.board.rayToEdge(attackerSquare, direction)
          firstOccupiedSquare(context, lineSquares).flatMap: blockerSquare =>
            val defenderColor = !beneficiaryColor
            val blockerPiece = context.pieceAt(blockerSquare).get
            val remainingSquares = lineSquares.dropWhile(_ != blockerSquare).drop(1)

            if blockerPiece.color == defenderColor &&
                blockerPiece.role != King &&
                context.hasColorSquare(SchemaId.PinnedPiece, defenderColor, blockerSquare)
            then
              firstOccupiedSquare(context, remainingSquares).flatMap: anchorSquare =>
                val anchorPiece = context.pieceAt(anchorSquare).get
                val pinMode =
                  if anchorPiece.color == defenderColor && anchorPiece.role == King then Some("absolute_king_pin")
                  else if anchorPiece.color == defenderColor && pieceValue(anchorPiece.role) > pieceValue(blockerPiece.role) then
                    Some("relative_anchor_pin")
                  else None

                pinMode.map: resolvedMode =>
                  beneficiary(
                    color = beneficiaryColor,
                    anchor = WitnessAnchor.RayAnchor(ray),
                    payload = WitnessPayload(
                      "ray" -> WitnessValue.RayValue(ray),
                      "attacker_square" -> WitnessValue.SquareValue(attackerSquare),
                      "blocker_square" -> WitnessValue.SquareValue(blockerSquare),
                      "anchor_square" -> WitnessValue.SquareValue(anchorSquare),
                      "pin_mode" -> WitnessValue.Token(resolvedMode)
                    ),
                    support = rootSupport(
                      indices = Vector(
                        context.pieceOnRootIndex(beneficiaryColor, attackerRole, attackerSquare),
                        context.colorSquareRootIndex(SchemaId.PinnedPiece, defenderColor, blockerSquare),
                        pieceRootIndex(context, defenderColor, anchorPiece.role, anchorSquare)
                      ).flatten,
                      targetSquares = Vector(blockerSquare, anchorSquare)
                    )
                  )
            else None

private[u] object ForkRule extends UScopedWitnessRule:

  val descriptorId: WitnessDescriptorId = WitnessDescriptorId("fork")

  def extract(context: UExtractionContext): Vector[Witness] =
    canonicalColors.flatMap: beneficiaryColor =>
      context.board.squaresOf(beneficiaryColor).flatMap: anchorSquare =>
        context.pieceAt(anchorSquare).flatMap: attackerPiece =>
          val targets =
            context.board.attacksFrom(anchorSquare).squares.toVector
              .filter(target => context.pieceAt(target).exists(_.color == !beneficiaryColor))
              .sortBy(_.value)

          Option.when(targets.size >= 2):
            val targetRoles = targets.map(square => context.pieceAt(square).get.role)
            beneficiary(
              color = beneficiaryColor,
              anchor = WitnessAnchor.PieceSquareAnchor(anchorSquare),
              payload = WitnessPayload(
                "attacker_role" -> WitnessValue.RoleValue(attackerPiece.role),
                "anchor_square" -> WitnessValue.SquareValue(anchorSquare),
                "target_squares" -> WitnessValue.SquareListValue(targets),
                "target_roles" -> WitnessValue.RoleListValue(targetRoles)
              ),
              support = rootSupport(
                indices =
                  Vector(context.pieceOnRootIndex(beneficiaryColor, attackerPiece.role, anchorSquare)).flatten ++
                    targets.flatMap(target => pieceRootIndex(context, !beneficiaryColor, context.pieceAt(target).get.role, target)),
                targetSquares = targets
              )
            )

private[u] object SkewerRule extends UScopedWitnessRule:

  val descriptorId: WitnessDescriptorId = WitnessDescriptorId("skewer")

  def extract(context: UExtractionContext): Vector[Witness] =
    canonicalColors.flatMap: beneficiaryColor =>
      sliderSquares(context, beneficiaryColor).flatMap: attackerSquare =>
        val attackerPiece = context.pieceAt(attackerSquare).get
        context.board.sliderDirections(attackerPiece.role).flatMap: direction =>
          val ray = WitnessRay(attackerSquare, direction)
          val lineSquares = context.board.rayToEdge(attackerSquare, direction)

          firstOccupiedSquare(context, lineSquares).flatMap: frontSquare =>
            val defenderColor = !beneficiaryColor
            val frontPiece = context.pieceAt(frontSquare).get
            val remainingSquares = lineSquares.dropWhile(_ != frontSquare).drop(1)

            if frontPiece.color == defenderColor && frontPiece.role != King then
              firstOccupiedSquare(context, remainingSquares).flatMap: rearSquare =>
                val rearPiece = context.pieceAt(rearSquare).get
                if rearPiece.color == defenderColor &&
                    pieceValue(frontPiece.role) > pieceValue(rearPiece.role)
                then
                  Some(
                    beneficiary(
                      color = beneficiaryColor,
                      anchor = WitnessAnchor.RayAnchor(ray),
                      payload = WitnessPayload(
                        "ray" -> WitnessValue.RayValue(ray),
                        "attacker_square" -> WitnessValue.SquareValue(attackerSquare),
                        "slider_role" -> WitnessValue.RoleValue(attackerPiece.role),
                        "front_square" -> WitnessValue.SquareValue(frontSquare),
                        "rear_square" -> WitnessValue.SquareValue(rearSquare),
                        "front_piece_role" -> WitnessValue.RoleValue(frontPiece.role),
                        "rear_piece_role" -> WitnessValue.RoleValue(rearPiece.role)
                      ),
                      support = rootSupport(
                        indices = Vector(
                          context.pieceOnRootIndex(beneficiaryColor, attackerPiece.role, attackerSquare),
                          pieceRootIndex(context, defenderColor, frontPiece.role, frontSquare),
                          pieceRootIndex(context, defenderColor, rearPiece.role, rearSquare)
                        ).flatten,
                        targetSquares = Vector(frontSquare, rearSquare)
                      )
                    )
                  )
                else None
            else None

private[u] object OverloadRule extends UScopedWitnessRule:

  val descriptorId: WitnessDescriptorId = WitnessDescriptorId("overload")

  def extract(context: UExtractionContext): Vector[Witness] =
    canonicalColors.flatMap: defenderColor =>
      val beneficiaryColor = !defenderColor
      context.overloadedPieceSquaresFor(defenderColor).flatMap: anchorSquare =>
        context.pieceAt(anchorSquare).flatMap: anchorPiece =>
          Option.when(anchorPiece.role != King):
            val boardWithoutAnchor = context.board.without(anchorSquare)
            val dutySquares =
              context.board.squaresOf(defenderColor).filter: square =>
                square != anchorSquare &&
                  context.pieceAt(square).exists(_.role != King) &&
                  context.board.attacksSquare(anchorSquare, square) &&
                  boardWithoutAnchor.attackCountOn(square, defenderColor) < boardWithoutAnchor.attackCountOn(square, beneficiaryColor)

            Option.when(dutySquares.size >= 2):
              beneficiary(
                color = beneficiaryColor,
                anchor = WitnessAnchor.PieceSquareAnchor(anchorSquare),
                payload = WitnessPayload(
                  "anchor_piece_role" -> WitnessValue.RoleValue(anchorPiece.role),
                  "anchor_square" -> WitnessValue.SquareValue(anchorSquare),
                  "duty_squares" -> WitnessValue.SquareListValue(dutySquares.sortBy(_.value))
                ),
                support = rootSupport(
                  indices = Vector(
                    context.pieceOnRootIndex(defenderColor, anchorPiece.role, anchorSquare),
                    context.colorSquareRootIndex(SchemaId.OverloadedPiece, defenderColor, anchorSquare)
                  ).flatten ++ dutySquares.flatMap(square =>
                    pieceRootIndex(context, defenderColor, context.pieceAt(square).get.role, square)
                  ),
                  targetSquares = dutySquares
                )
              )
          .flatten

private[u] def firstOccupiedSquare(
    context: UExtractionContext,
    squares: Vector[Square]
): Option[Square] =
  squares.find(square => context.pieceAt(square).nonEmpty)

private[u] def sliderSquares(
    context: UExtractionContext,
    color: Color
): Vector[Square] =
  Vector(Bishop, Rook, Queen).flatMap(role => context.activePieceSquares(color, role))

private[u] def pieceRootIndex(
    context: UExtractionContext,
    color: Color,
    role: Role,
    square: Square
): Option[Int] =
  context.pieceOnRootIndex(color, role, square)
