package lila.commentary.witness.u

import chess.{ Bishop, Knight }

import lila.commentary.root.RootAtomRegistry.{ SchemaId, canonicalColors }
import lila.commentary.witness.*
import lila.commentary.witness.u.UWitnessHelpers.*

private[u] object WeakOutpostSquareStateRule extends UScopedWitnessRule:

  val descriptorId: WitnessDescriptorId = WitnessDescriptorId("weak_outpost_square_state")

  def extract(context: UExtractionContext): Vector[Witness] =
    canonicalColors.flatMap: beneficiaryColor =>
      val outpostSquares = context.outpostSquaresFor(beneficiaryColor)
      val residualWeakSquares =
        context.weakSquaresFor(beneficiaryColor).filterNot(outpostSquares.contains)

      val outpostWitnesses = outpostSquares.map: square =>
        beneficiary(
          color = beneficiaryColor,
          anchor = WitnessAnchor.SquareAnchor(square),
          payload = WitnessPayload(
            "square" -> WitnessValue.SquareValue(square),
            "state" -> WitnessValue.Token("outpost")
          ),
          support = rootSupport(
            indices = Vector(context.colorSquareRootIndex(SchemaId.OutpostSquare, beneficiaryColor, square)).flatten,
            targetSquares = Vector(square)
          ),
          variant = OutpostSquareStateVariant
        )

      val weakWitnesses = residualWeakSquares.map: square =>
        beneficiary(
          color = beneficiaryColor,
          anchor = WitnessAnchor.SquareAnchor(square),
          payload = WitnessPayload(
            "square" -> WitnessValue.SquareValue(square),
            "state" -> WitnessValue.Token("weak")
          ),
          support = rootSupport(
            indices = Vector(context.colorSquareRootIndex(SchemaId.WeakSquare, beneficiaryColor, square)).flatten,
            targetSquares = Vector(square)
          ),
          variant = WeakSquareStateVariant
        )

      outpostWitnesses ++ weakWitnesses

private[u] object LoosePieceTargetStateRule extends UScopedWitnessRule:

  val descriptorId: WitnessDescriptorId = WitnessDescriptorId("loose_piece_target_state")

  def extract(context: UExtractionContext): Vector[Witness] =
    canonicalColors.flatMap: defenderColor =>
      val beneficiaryColor = !defenderColor
      context.loosePieceSquaresFor(defenderColor).flatMap: square =>
        Option.when(context.pieceAt(square).nonEmpty):
          beneficiary(
            color = beneficiaryColor,
            anchor = WitnessAnchor.PieceSquareAnchor(square),
            payload = WitnessPayload(
              "square" -> WitnessValue.SquareValue(square)
            ),
            support = rootSupport(
              indices = Vector(context.colorSquareRootIndex(SchemaId.LoosePiece, defenderColor, square)).flatten,
              targetSquares = Vector(square)
            )
          )

private[u] object BishopPairStateRule extends UScopedWitnessRule:

  val descriptorId: WitnessDescriptorId = WitnessDescriptorId("bishop_pair_state")

  def extract(context: UExtractionContext): Vector[Witness] =
    canonicalColors.flatMap: ownerColor =>
      val bishopSquares = context.activePieceSquares(ownerColor, Bishop)
      Option.when(bishopSquares.size >= 2):
        owner(
          color = ownerColor,
          anchor = WitnessAnchor.BoardAnchor,
          payload = WitnessPayload(
            "bishop_member_squares" -> WitnessValue.SquareListValue(bishopSquares)
          ),
          support = rootSupport(
            indices = bishopSquares.flatMap(square => context.pieceOnRootIndex(ownerColor, Bishop, square)),
            targetSquares = bishopSquares
          )
        )

private[u] object KnightOnOutpostSquareRule extends UScopedWitnessRule:

  val descriptorId: WitnessDescriptorId = WitnessDescriptorId("knight_on_outpost_square")

  def extract(context: UExtractionContext): Vector[Witness] =
    canonicalColors.flatMap: ownerColor =>
      val outpostSquares = context.outpostSquaresFor(ownerColor).toSet
      context.activePieceSquares(ownerColor, Knight).collect:
        case square if outpostSquares.contains(square) =>
          owner(
            color = ownerColor,
            anchor = WitnessAnchor.PieceSquareAnchor(square),
            payload = WitnessPayload(
              "knight_square" -> WitnessValue.SquareValue(square)
            ),
            support = rootSupport(
              indices = Vector(
                context.pieceOnRootIndex(ownerColor, Knight, square),
                context.colorSquareRootIndex(SchemaId.OutpostSquare, ownerColor, square)
              ).flatten,
              targetSquares = Vector(square)
            )
          )
