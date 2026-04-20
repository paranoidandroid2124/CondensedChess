package lila.commentary.strategic

import scala.annotation.targetName

import chess.{ Color, Square }

import lila.commentary.root.RootStateVector
import lila.commentary.witness.*
import lila.commentary.witness.u.UExtractionContext

private[commentary] final case class StrategicObjectContext(
    rootState: RootStateVector,
    primaryWitnesses: WitnessSet,
    attachedWitnesses: WitnessSet
):

  private val lowLevelContext = UExtractionContext(rootState)
  val board = lowLevelContext.board

  def pieceAt(square: Square) = lowLevelContext.pieceAt(square)
  def activePieceSquares(color: Color, role: chess.Role) = lowLevelContext.activePieceSquares(color, role)
  def activeColorSquares(schemaId: String, color: Color) = lowLevelContext.activeColorSquares(schemaId, color)
  def activeColorPawnSquares(schemaId: String, color: Color) =
    lowLevelContext.activeColorPawnSquares(schemaId, color)
  def hasColorSquare(schemaId: String, color: Color, square: Square) =
    lowLevelContext.hasColorSquare(schemaId, color, square)
  def hasColorPawnSquare(schemaId: String, color: Color, square: Square) =
    lowLevelContext.hasColorPawnSquare(schemaId, color, square)
  def hasNeutralSquare(schemaId: String, square: Square) =
    lowLevelContext.hasNeutralSquare(schemaId, square)
  def colorSquareRootIndex(schemaId: String, color: Color, square: Square) =
    lowLevelContext.colorSquareRootIndex(schemaId, color, square)
  def colorPawnSquareRootIndex(schemaId: String, color: Color, square: Square) =
    lowLevelContext.colorPawnSquareRootIndex(schemaId, color, square)
  def neutralSquareRootIndex(schemaId: String, square: Square) =
    lowLevelContext.neutralSquareRootIndex(schemaId, square)
  def pieceOnRootIndex(color: Color, role: chess.Role, square: Square) =
    lowLevelContext.pieceOnRootIndex(color, role, square)
  def forwardSquare(color: Color, from: Square, steps: Int = 1) =
    lowLevelContext.forwardSquare(color, from, steps)
  def kingRingSquaresFor(color: Color) = lowLevelContext.kingRingSquaresFor(color)

  def primaryWitnessesFor(descriptorId: WitnessDescriptorId): Vector[Witness] =
    primaryWitnesses.forDescriptorId(descriptorId)

  @targetName("primaryWitnessesForString")
  def primaryWitnessesFor(descriptorId: String): Vector[Witness] =
    primaryWitnessesFor(WitnessDescriptorId(descriptorId))

  def attachedWitnessesFor(descriptorId: WitnessDescriptorId): Vector[Witness] =
    attachedWitnesses.forDescriptorId(descriptorId)

  @targetName("attachedWitnessesForString")
  def attachedWitnessesFor(descriptorId: String): Vector[Witness] =
    attachedWitnessesFor(WitnessDescriptorId(descriptorId))

  def token(payload: WitnessPayload, field: String): Option[String] =
    payload.get(field).collect { case WitnessValue.Token(value) => value }

  def square(payload: WitnessPayload, field: String): Option[Square] =
    payload.get(field).collect { case WitnessValue.SquareValue(value) => value }

  def squareList(payload: WitnessPayload, field: String): Vector[Square] =
    payload.get(field).collect { case WitnessValue.SquareListValue(values) => values }.getOrElse(Vector.empty)

  def colorValue(payload: WitnessPayload, field: String): Option[Color] =
    payload.get(field).collect { case WitnessValue.ColorValue(value) => value }
