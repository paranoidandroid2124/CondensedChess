package lila.commentary.strategic

import lila.commentary.root.RootAtomRegistry.{ SchemaId, canonicalColors }
import lila.commentary.witness.{ WitnessAnchor, WitnessPayload, WitnessSector, WitnessValue }
import lila.commentary.strategic.StrategicObjectHelpers.*

private[strategic] object CentralContactFrontRule extends StrategicObjectRule:

  val familyId: StrategicObjectId = StrategicObjectId("CentralContactFront")

  def extract(
      context: StrategicObjectContext,
      extractedSoFar: StrategicObjectSet
  ): Vector[StrategicObject] =
    selectCanonicalComponent(
      contactComponents(context, centralSectorMask).filter(isCentralContactFrontComponent)
    ).map { component =>
        neutral(
          anchor = WitnessAnchor.SectorAnchor(WitnessSector.Center),
          payload = componentPayload(component),
          support = componentSupport(context, component)
        )
      }.toVector

  private[strategic] def selectCanonicalComponent(
      components: Vector[ContactComponent]
  ): Option[ContactComponent] =
    components.sortBy(component =>
      (
        -component.squares.size,
        -component.contestedSquares.size,
        -component.occupiedContactSquares.size,
        component.squares.head.value
      )
    ).headOption

  private def isCentralContactFrontComponent(component: ContactComponent): Boolean =
    component.squares.size >= 2 &&
      component.contestedSquares.nonEmpty &&
      component.occupiedContactSquares.nonEmpty &&
      component.contributingColors.size == 2

  private def componentPayload(component: ContactComponent): WitnessPayload =
    WitnessPayload(
      "component_squares" -> WitnessValue.SquareListValue(component.squares),
      "contested_squares" -> WitnessValue.SquareListValue(component.contestedSquares),
      "occupied_contact_squares" -> WitnessValue.SquareListValue(component.occupiedContactSquares),
      "contributing_colors" -> WitnessValue.TokenListValue(component.contributingColors.toVector.map(_.name).sorted)
    )

  private def componentSupport(
      context: StrategicObjectContext,
      component: ContactComponent
  ) =
    val indices =
      component.squares.flatMap { square =>
        val contestedIndex =
          context.neutralSquareRootIndex(SchemaId.Contested, square).toVector
        val controlIndices =
          canonicalColors.flatMap { color =>
            context.colorSquareRootIndex(SchemaId.ControlledBy, color, square).toVector
          }
        val occupiedIndices =
          context.pieceAt(square).toVector.flatMap { piece =>
            context.pieceOnRootIndex(piece.color, piece.role, square).toVector
          }
        contestedIndex ++ controlIndices ++ occupiedIndices
      }

    support(indices = indices)
