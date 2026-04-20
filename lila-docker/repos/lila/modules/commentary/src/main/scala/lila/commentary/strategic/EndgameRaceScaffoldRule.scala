package lila.commentary.strategic

import chess.{ Color, Pawn, Square }

import lila.commentary.root.RootAtomRegistry.SchemaId
import lila.commentary.witness.*
import lila.commentary.strategic.StrategicObjectHelpers.*

private[strategic] object EndgameRaceScaffoldRule extends StrategicObjectRule:

  val familyId: StrategicObjectId = StrategicObjectId("EndgameRaceScaffold")

  def extract(
      context: StrategicObjectContext,
      _extractedSoFar: StrategicObjectSet
  ): Vector[StrategicObject] =
    val whiteAdvancedRunResources = advancedRunResources(context, Color.White)
    val blackAdvancedRunResources = advancedRunResources(context, Color.Black)
    val whiteClearResources = whiteAdvancedRunResources.filter(square => forwardRunClear(context, Color.White, square))
    val blackClearResources = blackAdvancedRunResources.filter(square => forwardRunClear(context, Color.Black, square))

    Option.when(
      noQueensRemain(context) &&
        whiteAdvancedRunResources.nonEmpty &&
        blackAdvancedRunResources.nonEmpty &&
        whiteClearResources.nonEmpty &&
        blackClearResources.nonEmpty
    )(
      neutral(
        anchor = WitnessAnchor.BoardAnchor,
        payload = WitnessPayload(
          "white_advanced_run_squares" -> WitnessValue.SquareListValue(whiteAdvancedRunResources),
          "black_advanced_run_squares" -> WitnessValue.SquareListValue(blackAdvancedRunResources),
          "white_clear_run_squares" -> WitnessValue.SquareListValue(whiteClearResources),
          "black_clear_run_squares" -> WitnessValue.SquareListValue(blackClearResources)
        ),
        support = support(
          indices =
            whiteAdvancedRunResources.flatMap(square => advancedRunRootIndices(context, Color.White, square)) ++
              blackAdvancedRunResources.flatMap(square => advancedRunRootIndices(context, Color.Black, square)),
          targetSquares = whiteClearResources ++ blackClearResources
        )
      )
    ).toVector

  private def advancedRunResources(context: StrategicObjectContext, color: Color): Vector[Square] =
    context.activePieceSquares(color, Pawn).filter: square =>
      if color.white then square.rank >= chess.Rank.Fifth else square.rank <= chess.Rank.Fourth

  private def advancedRunRootIndices(
      context: StrategicObjectContext,
      color: Color,
      square: Square
  ): Vector[Int] =
    Vector(
      context.pieceOnRootIndex(color, Pawn, square),
      context.colorPawnSquareRootIndex(SchemaId.PassedPawn, color, square),
      context.colorPawnSquareRootIndex(SchemaId.CandidatePasser, color, square)
    ).flatten
