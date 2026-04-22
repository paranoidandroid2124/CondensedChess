package lila.commentary.witness.seed

import chess.format.Fen
import chess.{ Color, Role, Square }

import lila.commentary.root.{ RootExtractor, RootStateVector }
import lila.commentary.witness.*

object StrategySupportSeedTestSupport:

  def extraction(fen: String): StrategySupportSeedExtraction =
    StrategySupportSeedExtractor.fromFen(Fen.Full.clean(fen)).fold(message => throw RuntimeException(message), identity)

  def extraction(rootState: RootStateVector): StrategySupportSeedExtraction =
    StrategySupportSeedExtractor.fromRoot(rootState)

  def rootState(fen: String): RootStateVector =
    RootExtractor.fromFen(Fen.Full.clean(fen)).fold(message => throw RuntimeException(message), identity)

  def descriptor(fen: String, seedId: String): Vector[StrategySupportSeed] =
    extraction(fen).seeds.forSeedId(StrategySupportSeedId(seedId))

  def findPieceSquare(
      fen: String,
      seedId: String,
      squareKey: String,
      color: Option[Color] = None,
      variant: Option[String] = None
  ): Option[StrategySupportSeed] =
    val square = Square.fromKey(squareKey).get
    descriptor(fen, seedId).find: seed =>
      seed.anchor == WitnessAnchor.PieceSquareAnchor(square) &&
        color.forall(seed.color.contains) &&
        variant.forall(expected => seed.variant.contains(WitnessVariantId(expected)))

  def findSquare(
      fen: String,
      seedId: String,
      squareKey: String,
      color: Option[Color] = None,
      variant: Option[String] = None
  ): Option[StrategySupportSeed] =
    val square = Square.fromKey(squareKey).get
    descriptor(fen, seedId).find: seed =>
      seed.anchor == WitnessAnchor.SquareAnchor(square) &&
        color.forall(seed.color.contains) &&
        variant.forall(expected => seed.variant.contains(WitnessVariantId(expected)))

  def token(payload: WitnessPayload, field: String): Option[String] =
    payload.get(field).collect { case WitnessValue.Token(value) => value }

  def tokenList(payload: WitnessPayload, field: String): Vector[String] =
    payload.get(field).collect { case WitnessValue.TokenListValue(values) => values }.getOrElse(Vector.empty)

  def squareValue(payload: WitnessPayload, field: String): Option[Square] =
    payload.get(field).collect { case WitnessValue.SquareValue(square) => square }

  def squareList(payload: WitnessPayload, field: String): Vector[Square] =
    payload.get(field).collect { case WitnessValue.SquareListValue(values) => values }.getOrElse(Vector.empty)

  def roleValue(payload: WitnessPayload, field: String): Option[Role] =
    payload.get(field).collect { case WitnessValue.RoleValue(role) => role }
