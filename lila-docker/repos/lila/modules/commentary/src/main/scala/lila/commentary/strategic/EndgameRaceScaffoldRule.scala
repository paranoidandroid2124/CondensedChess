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
    endgameRaceScaffoldSnapshot(context).map { snapshot =>
      neutral(
        anchor = WitnessAnchor.BoardAnchor,
        payload = WitnessPayload(
          "white_advanced_run_squares" -> WitnessValue.SquareListValue(snapshot.whiteAdvancedRunSquares),
          "black_advanced_run_squares" -> WitnessValue.SquareListValue(snapshot.blackAdvancedRunSquares),
          "white_clear_run_squares" -> WitnessValue.SquareListValue(snapshot.whiteClearRunSquares),
          "black_clear_run_squares" -> WitnessValue.SquareListValue(snapshot.blackClearRunSquares)
        ),
        support = support(
          indices =
            snapshot.whiteAdvancedRunSquares.flatMap(square => advancedRunRootIndices(context, Color.White, square)) ++
              snapshot.blackAdvancedRunSquares.flatMap(square => advancedRunRootIndices(context, Color.Black, square)),
          targetSquares = snapshot.whiteClearRunSquares ++ snapshot.blackClearRunSquares
        )
      )
    }.toVector

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
