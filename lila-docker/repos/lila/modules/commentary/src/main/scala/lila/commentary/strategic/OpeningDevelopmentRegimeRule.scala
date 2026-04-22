package lila.commentary.strategic

import chess.{ Color, Square }

import lila.commentary.witness.*
import lila.commentary.strategic.StrategicObjectHelpers.*

private[strategic] object OpeningDevelopmentRegimeRule extends StrategicObjectRule:

  val familyId: StrategicObjectId = StrategicObjectId("OpeningDevelopmentRegime")

  def extract(
      context: StrategicObjectContext,
      extractedSoFar: StrategicObjectSet
  ): Vector[StrategicObject] =
    if preemptedByLiveBoardOwner(context) then Vector.empty
    else
      val whiteHomeMinors = retainedHomeMinors(context, Color.White)
      val blackHomeMinors = retainedHomeMinors(context, Color.Black)
      val whiteHomeRooks = retainedHomeRooks(context, Color.White)
      val blackHomeRooks = retainedHomeRooks(context, Color.Black)

      Option.when(
        openingDevelopmentWindow(context)
      )(
        neutral(
          anchor = WitnessAnchor.BoardAnchor,
          payload = WitnessPayload(
            "white_home_minor_squares" -> WitnessValue.SquareListValue(whiteHomeMinors),
            "black_home_minor_squares" -> WitnessValue.SquareListValue(blackHomeMinors),
            "white_home_rook_squares" -> WitnessValue.SquareListValue(whiteHomeRooks),
            "black_home_rook_squares" -> WitnessValue.SquareListValue(blackHomeRooks)
          ),
          support = support(
            indices =
              rootIndicesForHomeMinors(context, Color.White, whiteHomeMinors) ++
                rootIndicesForHomeMinors(context, Color.Black, blackHomeMinors) ++
                rootIndicesForHomeRooks(context, Color.White, whiteHomeRooks) ++
                rootIndicesForHomeRooks(context, Color.Black, blackHomeRooks),
            targetSquares = whiteHomeMinors ++ blackHomeMinors ++ whiteHomeRooks ++ blackHomeRooks
          )
        )
      ).toVector

  private def preemptedByLiveBoardOwner(context: StrategicObjectContext): Boolean =
    centralContactFrontComponent(context).nonEmpty ||
      distributedContactRegimeComponents(context).nonEmpty ||
      endgameRaceScaffoldSnapshot(context).nonEmpty

  private def rootIndicesForHomeMinors(
      context: StrategicObjectContext,
      color: Color,
      squares: Vector[Square]
  ): Vector[Int] =
    squares.flatMap: square =>
      context.pieceAt(square).flatMap: piece =>
        if piece.color == color && Set(chess.Bishop, chess.Knight).contains(piece.role) then
          context.pieceOnRootIndex(color, piece.role, square)
        else None

  private def rootIndicesForHomeRooks(
      context: StrategicObjectContext,
      color: Color,
      squares: Vector[Square]
  ): Vector[Int] =
    squares.flatMap: square =>
      context.pieceAt(square).flatMap: piece =>
        if piece.color == color && piece.role == chess.Rook then
          context.pieceOnRootIndex(color, piece.role, square)
        else None
