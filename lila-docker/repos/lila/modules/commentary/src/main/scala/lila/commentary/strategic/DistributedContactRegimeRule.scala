package lila.commentary.strategic

import chess.Color

import lila.commentary.root.RootAtomRegistry.SchemaId
import lila.commentary.witness.*
import lila.commentary.strategic.StrategicObjectHelpers.*

private[strategic] object DistributedContactRegimeRule extends StrategicObjectRule:

  val familyId: StrategicObjectId = StrategicObjectId("DistributedContactRegime")

  def extract(
      context: StrategicObjectContext,
      _extractedSoFar: StrategicObjectSet
  ): Vector[StrategicObject] =
    val admittedComponents = distributedContactRegimeComponents(context)

    val admittedSectors = admittedComponents.map(_._1).distinct
    val admittedSquares = admittedComponents.flatMap(_._2.squares).distinct.sortBy(_.value)

    Option.when(admittedComponents.nonEmpty)(
      neutral(
        anchor = WitnessAnchor.BoardAnchor,
        payload = WitnessPayload(
          "admitted_sector_squares" -> WitnessValue.SquareListValue(admittedSquares),
          "admitted_sectors" -> WitnessValue.TokenListValue(admittedSectors.map(_.key))
        ),
        support = support(
          indices = admittedComponents.flatMap { case (_, component) =>
            component.contestedSquares.flatMap(square =>
              Vector(context.neutralSquareRootIndex(SchemaId.Contested, square)).flatten
            ) ++ component.occupiedContactSquares.flatMap(square =>
              Vector(
                context.colorSquareRootIndex(SchemaId.ControlledBy, Color.White, square),
                context.colorSquareRootIndex(SchemaId.ControlledBy, Color.Black, square)
              ).flatten
            )
          },
          targetSquares = admittedSquares
        )
      )
    ).toVector
