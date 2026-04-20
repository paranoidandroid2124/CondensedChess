package lila.commentary.strategic

import chess.format.Fen
import chess.{ Color, Square }

import lila.commentary.root.{ RootExtractor, RootStateVector }
import lila.commentary.witness.{ WitnessAnchor, WitnessPayload, WitnessSector, WitnessValue }

object StrategicObjectTestSupport:

  def extraction(fen: String): StrategicObjectExtraction =
    StrategicObjectExtractor.fromFen(Fen.Full.clean(fen)).fold(message => throw RuntimeException(message), identity)

  def extraction(rootState: RootStateVector): StrategicObjectExtraction =
    StrategicObjectExtractor.fromRoot(rootState)

  def rootState(fen: String): RootStateVector =
    RootExtractor.fromFen(Fen.Full.clean(fen)).fold(message => throw RuntimeException(message), identity)

  def family(fen: String, familyId: String): Vector[StrategicObject] =
    extraction(fen).objects.forFamilyId(familyId)

  def findBoard(fen: String, familyId: String): Option[StrategicObject] =
    family(fen, familyId).find(_.anchor == WitnessAnchor.BoardAnchor)

  def findSector(
      fen: String,
      familyId: String,
      sector: WitnessSector
  ): Option[StrategicObject] =
    family(fen, familyId).find(_.anchor == WitnessAnchor.SectorAnchor(sector))

  def findSquare(
      fen: String,
      familyId: String,
      squareKey: String,
      color: Option[Color] = None
  ): Option[StrategicObject] =
    val square = Square.fromKey(squareKey).get
    family(fen, familyId).find(obj =>
      obj.anchor == WitnessAnchor.SquareAnchor(square) &&
        color.forall(obj.color.contains)
    )

  def squareList(payload: WitnessPayload, field: String): Vector[Square] =
    payload.get(field).collect { case WitnessValue.SquareListValue(values) => values }.getOrElse(Vector.empty)

  def colorValue(payload: WitnessPayload, field: String): Option[Color] =
    payload.get(field).collect { case WitnessValue.ColorValue(value) => value }
