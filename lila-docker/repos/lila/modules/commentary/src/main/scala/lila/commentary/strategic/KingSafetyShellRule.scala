package lila.commentary.strategic

import chess.Color

import lila.commentary.root.RootAtomRegistry.SchemaId
import lila.commentary.witness.{ WitnessAnchor, WitnessPayload, WitnessValue }
import lila.commentary.strategic.StrategicObjectHelpers.*

private[strategic] object KingSafetyShellRule extends StrategicObjectRule:

  val familyId: StrategicObjectId = StrategicObjectId("KingSafetyShell")

  def extract(
      context: StrategicObjectContext,
      extractedSoFar: StrategicObjectSet
  ): Vector[StrategicObject] =
    Vector(Color.White, Color.Black).flatMap: defender =>
      homeWingKingSquare(context, defender).toVector.flatMap: kingSquare =>
        val shelterHoles = homeShelterHoles(context, defender)
        bestEdgeAdjacentHolePair(context, defender) match
          case Some((anchorSquare, pairedSquare)) =>
            Vector(
              owned(
                color = defender,
                anchor = WitnessAnchor.SquareAnchor(anchorSquare),
                payload = WitnessPayload(
                  "defender" -> WitnessValue.ColorValue(defender),
                  "king_square" -> WitnessValue.SquareValue(kingSquare),
                  "home_shelter_holes" -> WitnessValue.SquareListValue(shelterHoles),
                  "paired_holes" -> WitnessValue.SquareListValue(Vector(anchorSquare, pairedSquare))
                ),
                support = support(
                  indices = rootIndicesForSquares(context, SchemaId.KingShelterHole, !defender, shelterHoles),
                  targetSquares = shelterHoles
                )
              )
            )
          case None => Vector.empty
