package lila.commentary.delta

import chess.Color

import lila.commentary.strategic.StrategicObject
import lila.commentary.witness.{ WitnessAnchor, WitnessValue }

private[delta] object TradeInvariantAdmission:

  val persistentCarrierFamily: lila.commentary.strategic.StrategicObjectId =
    lila.commentary.strategic.StrategicObjectId("EndgameRaceScaffold")
  val persistentCarrierAnchor: WitnessAnchor = WitnessAnchor.BoardAnchor
  val helperTags: Vector[String] = Vector(
    "bounded_material_reduction",
    "persistent_object_carrier",
    "trade_invariant_transition"
  )

  def firstSliceEvidence(
      context: StrategicDeltaContext,
      moverColor: Color
  ): Option[CarrierEvidence] =
    Option.when(boundedMaterialReduction(context))(
      persistentCarrierEvidence(context, moverColor)
    ).flatten

  private def boundedMaterialReduction(context: StrategicDeltaContext): Boolean =
    context.capturesNonKingPiece &&
      context.nonKingNonPawnReduction == 1

  private def persistentCarrierEvidence(
      context: StrategicDeltaContext,
      moverColor: Color
  ): Option[CarrierEvidence] =
    val beforeCarriers = anchoredCarrierObjects(context.beforeExtraction)
    val afterCarriers = anchoredCarrierObjects(context.afterExtraction)
    val beforeMoverCarrierSquares = clearRunSquares(beforeCarriers, moverColor)
    val afterMoverCarrierSquares = clearRunSquares(afterCarriers, moverColor)

    Option.when(
      beforeCarriers.nonEmpty &&
        afterCarriers.nonEmpty &&
        moverCarrierPersists(context, moverColor, beforeMoverCarrierSquares, afterMoverCarrierSquares)
    ):
      CarrierEvidence(
        rootIndices =
          (beforeCarriers.flatMap(_.support.rootIndices) ++
            afterCarriers.flatMap(_.support.rootIndices)).distinct.sorted
      )

  private def anchoredCarrierObjects(
      extraction: lila.commentary.strategic.StrategicObjectExtraction
  ): Vector[StrategicObject] =
    extraction.objects
      .forFamilyId(persistentCarrierFamily)
      .filter(_.anchor == persistentCarrierAnchor)

  private def moverCarrierPersists(
      context: StrategicDeltaContext,
      moverColor: Color,
      beforeMoverCarrierSquares: Vector[chess.Square],
      afterMoverCarrierSquares: Vector[chess.Square]
  ): Boolean =
    stationaryCarrierPersists(beforeMoverCarrierSquares, afterMoverCarrierSquares) ||
      movedCarrierPersists(context, moverColor, beforeMoverCarrierSquares, afterMoverCarrierSquares)

  private def stationaryCarrierPersists(
      beforeMoverCarrierSquares: Vector[chess.Square],
      afterMoverCarrierSquares: Vector[chess.Square]
  ): Boolean =
    beforeMoverCarrierSquares.exists(afterMoverCarrierSquares.contains)

  private def movedCarrierPersists(
      context: StrategicDeltaContext,
      moverColor: Color,
      beforeMoverCarrierSquares: Vector[chess.Square],
      afterMoverCarrierSquares: Vector[chess.Square]
  ): Boolean =
    context.movingPieceBefore.exists(piece =>
      piece.color == moverColor &&
        piece.role == chess.Pawn &&
        beforeMoverCarrierSquares.contains(context.playedMove.orig) &&
        afterMoverCarrierSquares.contains(context.playedMove.dest)
    )

  private def clearRunSquares(
      carriers: Vector[StrategicObject],
      color: Color
  ): Vector[chess.Square] =
    val field = if color.white then "white_clear_run_squares" else "black_clear_run_squares"
    carriers
      .flatMap(_.payload.get(field).toVector)
      .flatMap:
        case WitnessValue.SquareListValue(values) => values
        case _ => Vector.empty
      .distinct
      .sortBy(_.value)

  final case class CarrierEvidence(rootIndices: Vector[Int])
