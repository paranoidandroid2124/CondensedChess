package lila.commentary.witness.u

import chess.format.Fen
import chess.{ Color, File, Square }

import lila.commentary.root.{ RootExtractor, RootStateVector }
import lila.commentary.witness.*

object UWitnessTestSupport:

  def extraction(fen: String): UWitnessExtraction =
    UWitnessExtractor.fromFen(Fen.Full.clean(fen)).fold(message => throw RuntimeException(message), identity)

  def extraction(rootState: RootStateVector): UWitnessExtraction =
    UWitnessExtractor.fromRoot(rootState)

  def rootState(fen: String): RootStateVector =
    RootExtractor.fromFen(Fen.Full.clean(fen)).fold(message => throw RuntimeException(message), identity)

  def descriptor(fen: String, descriptorId: String): Vector[Witness] =
    extraction(fen).witnesses.forDescriptorId(WitnessDescriptorId(descriptorId))

  def descriptor(rootState: RootStateVector, descriptorId: String): Vector[Witness] =
    extraction(rootState).witnesses.forDescriptorId(WitnessDescriptorId(descriptorId))

  def findPieceSquare(
      fen: String,
      descriptorId: String,
      squareKey: String,
      color: Option[Color] = None,
      variant: Option[String] = None
  ): Option[Witness] =
    val square = Square.fromKey(squareKey).get
    descriptor(fen, descriptorId).find: witness =>
      witness.anchor == WitnessAnchor.PieceSquareAnchor(square) &&
        color.forall(witness.color.contains) &&
        variant.forall(expected => witness.variant.contains(WitnessVariantId(expected)))

  def findSquare(
      fen: String,
      descriptorId: String,
      squareKey: String,
      color: Option[Color] = None,
      variant: Option[String] = None
  ): Option[Witness] =
    val square = Square.fromKey(squareKey).get
    descriptor(fen, descriptorId).find: witness =>
      witness.anchor == WitnessAnchor.SquareAnchor(square) &&
        color.forall(witness.color.contains) &&
        variant.forall(expected => witness.variant.contains(WitnessVariantId(expected)))

  def findFile(
      fen: String,
      descriptorId: String,
      file: File,
      variant: Option[String] = None
  ): Option[Witness] =
    descriptor(fen, descriptorId).find: witness =>
      witness.anchor == WitnessAnchor.FileAnchor(file) &&
        variant.forall(expected => witness.variant.contains(WitnessVariantId(expected)))

  def findSector(
      fen: String,
      descriptorId: String,
      sector: WitnessSector
  ): Option[Witness] =
    descriptor(fen, descriptorId).find(_.anchor == WitnessAnchor.SectorAnchor(sector))

  def findRay(
      fen: String,
      descriptorId: String,
      sourceKey: String,
      direction: WitnessDirection,
      color: Option[Color] = None
  ): Option[Witness] =
    val source = Square.fromKey(sourceKey).get
    descriptor(fen, descriptorId).find: witness =>
      witness.anchor == WitnessAnchor.RayAnchor(WitnessRay(source, direction)) &&
        color.forall(witness.color.contains)

  def token(payload: WitnessPayload, field: String): Option[String] =
    payload.get(field).collect { case WitnessValue.Token(value) => value }

  def tokenList(payload: WitnessPayload, field: String): Vector[String] =
    payload.get(field).collect { case WitnessValue.TokenListValue(values) => values }.getOrElse(Vector.empty)

  def squareList(payload: WitnessPayload, field: String): Vector[Square] =
    payload.get(field).collect { case WitnessValue.SquareListValue(values) => values }.getOrElse(Vector.empty)

  def colorValue(payload: WitnessPayload, field: String): Option[Color] =
    payload.get(field).collect { case WitnessValue.ColorValue(color) => color }

  def fileValue(payload: WitnessPayload, field: String): Option[File] =
    payload.get(field).collect { case WitnessValue.FileValue(file) => file }
