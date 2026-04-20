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
    val whiteDeveloped = hasNonPawnDevelopmentOffHomeRank(context, Color.White)
    val blackDeveloped = hasNonPawnDevelopmentOffHomeRank(context, Color.Black)

    val admittedComponents =
      Vector(WitnessSector.Queenside, WitnessSector.Center, WitnessSector.Kingside).flatMap: sector =>
        contactComponents(context, square => sectorMask(sector, square))
          .filter(component =>
            component.squares.size >= 2 &&
              component.contestedSquares.nonEmpty &&
              component.occupiedContactSquares.nonEmpty &&
              component.contributingColors == Set(Color.White, Color.Black)
          )
          .map(component => sector -> component)

    val admittedSectors = admittedComponents.map(_._1).distinct
    val admittedSquares = admittedComponents.flatMap(_._2.squares).distinct.sortBy(_.value)
    val hasOutsideCenterBand = admittedComponents.exists(_._2.liesOutsideCenterSector)

    Option.when(
      whiteDeveloped &&
        blackDeveloped &&
        admittedSectors.size >= 2 &&
        hasOutsideCenterBand
    )(
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
