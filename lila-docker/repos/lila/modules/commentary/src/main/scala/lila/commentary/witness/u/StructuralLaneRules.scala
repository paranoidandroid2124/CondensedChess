package lila.commentary.witness.u

import chess.{ Bishop, Queen, Rook }

import lila.commentary.root.RootAtomRegistry.{ SchemaId, canonicalColors, canonicalFiles }
import lila.commentary.witness.*
import lila.commentary.witness.u.UWitnessHelpers.*

private[u] object FileLaneStateRule extends UScopedWitnessRule:

  val descriptorId: WitnessDescriptorId = WitnessDescriptorId("file_lane_state")

  def extract(context: UExtractionContext): Vector[Witness] =
    canonicalFiles.flatMap: file =>
      if context.hasNeutralFile(SchemaId.OpenFile, file) then
        Vector(
          neutral(
            anchor = WitnessAnchor.FileAnchor(file),
            payload = WitnessPayload(
              "file" -> WitnessValue.FileValue(file),
              "state" -> WitnessValue.Token("open")
            ),
            support = rootSupport(
              indices = Vector(context.neutralFileRootIndex(SchemaId.OpenFile, file)).flatten
            ),
            variant = OpenFileStateVariant
          )
        )
      else
        canonicalColors.find(color => context.hasColorFile(SchemaId.HalfOpenFile, color, file)).toVector.map: color =>
          neutral(
            anchor = WitnessAnchor.FileAnchor(file),
            payload = WitnessPayload(
              "file" -> WitnessValue.FileValue(file),
              "state" -> WitnessValue.Token("semi_open"),
              "open_for_color" -> WitnessValue.ColorValue(color)
            ),
            support = rootSupport(
              indices = Vector(context.colorFileRootIndex(SchemaId.HalfOpenFile, color, file)).flatten
            ),
            variant = SemiOpenFileStateVariant
          )

private[u] object DiagonalLaneOnlyRule extends UScopedWitnessRule:

  val descriptorId: WitnessDescriptorId = WitnessDescriptorId("diagonal_lane_only")

  def extract(context: UExtractionContext): Vector[Witness] =
    canonicalColors.flatMap: color =>
      val sourceSquares =
        context.activePieceSquares(color, Bishop) ++ context.activePieceSquares(color, Queen)

      sourceSquares.flatMap: sourceSquare =>
        context.board.sliderDirections(context.pieceAt(sourceSquare).map(_.role).get).filter(_.isDiagonal).flatMap: direction =>
          val clearSquares = context.board.clearRay(sourceSquare, direction)
          val blockerSquare = context.board.firstBlocker(sourceSquare, direction)
          val laneSquares =
            clearSquares ++ blockerSquare.filter(square => context.hasColorSquare(SchemaId.ControlledBy, color, square)).toVector

          Option.when(laneSquares.size >= 2):
            val ray = WitnessRay(sourceSquare, direction)
            val endpoint = laneSquares.last
            neutral(
              anchor = WitnessAnchor.RayAnchor(ray),
              payload = WitnessPayload.from(
                Vector(
                  "ray" -> WitnessValue.RayValue(ray),
                  "source_piece_squares" -> WitnessValue.SquareListValue(Vector(sourceSquare)),
                  "endpoint_squares" -> WitnessValue.SquareListValue(Vector(endpoint))
                ) ++ blockerSquare.toVector.map(square =>
                  "blocker_square" -> WitnessValue.SquareValue(square)
                )
              ),
              support = rootSupport(
                indices =
                  Vector(
                    context.pieceOnRootIndex(color, context.pieceAt(sourceSquare).get.role, sourceSquare),
                    context.colorSquareRootIndex(SchemaId.ControlledBy, color, endpoint)
                  ).flatten,
                targetSquares = laneSquares
              )
            )

private[u] object RookOnOpenFileStateRule extends UScopedWitnessRule:

  val descriptorId: WitnessDescriptorId = WitnessDescriptorId("rook_on_open_file_state")

  def extract(context: UExtractionContext): Vector[Witness] =
    canonicalColors.flatMap: ownerColor =>
      context.activePieceSquares(ownerColor, Rook).collect:
        case rookSquare if context.hasNeutralFile(SchemaId.OpenFile, rookSquare.file) =>
          owner(
            color = ownerColor,
            anchor = WitnessAnchor.PieceSquareAnchor(rookSquare),
            payload = WitnessPayload(
              "rook_square" -> WitnessValue.SquareValue(rookSquare),
              "file" -> WitnessValue.FileValue(rookSquare.file)
            ),
            support = rootSupport(
              indices = Vector(
                context.pieceOnRootIndex(ownerColor, Rook, rookSquare),
                context.neutralFileRootIndex(SchemaId.OpenFile, rookSquare.file)
              ).flatten,
              targetSquares = Vector(rookSquare)
            )
          )
