package lila.commentary.witness.u

import chess.{ File, Role, Square }
import chess.{ Bishop, King, Knight, Pawn, Queen, Rook }

import lila.commentary.witness.*

private[u] object UWitnessHelpers:

  val OpenFileStateVariant: Option[WitnessVariantId] = Some(WitnessVariantId("open_file_state"))
  val SemiOpenFileStateVariant: Option[WitnessVariantId] = Some(WitnessVariantId("semi_open_file_state"))
  val OutpostSquareStateVariant: Option[WitnessVariantId] = Some(WitnessVariantId("outpost_square_state"))
  val WeakSquareStateVariant: Option[WitnessVariantId] = Some(WitnessVariantId("weak_square_state"))
  val SinglePushLeverStateVariant: Option[WitnessVariantId] = Some(WitnessVariantId("single_push_lever_state"))
  val DoublePushLeverStateVariant: Option[WitnessVariantId] = Some(WitnessVariantId("double_push_lever_state"))

  def sectorOf(file: File): WitnessSector =
    file.value match
      case 0 | 1 | 2 => WitnessSector.Queenside
      case 3 | 4 => WitnessSector.Center
      case _ => WitnessSector.Kingside

  def rootSupport(
      indices: IterableOnce[Int] = Vector.empty,
      targetSquares: IterableOnce[Square] = Vector.empty,
      supportingTags: IterableOnce[String] = Vector.empty
  ): WitnessSupport =
    val withRoots = indices.iterator.foldLeft(WitnessSupport.empty): (support, index) =>
      support.addRootIndex(index)
    val withTargets = targetSquares.iterator.foldLeft(withRoots): (support, square) =>
      support.addTargetSquare(square)
    supportingTags.iterator.foldLeft(withTargets): (support, tag) =>
      support.addTag(tag)

  def objectValue(entries: (String, WitnessValue)*): WitnessValue =
    WitnessValue.ObjectValue(WitnessPayload(entries*))

  def objectList(entries: IterableOnce[WitnessPayload]): WitnessValue =
    WitnessValue.ListValue(entries.iterator.map(WitnessValue.ObjectValue.apply).toVector)

  def pieceValue(role: Role): Int =
    role match
      case Pawn => 100
      case Knight | Bishop => 300
      case Rook => 500
      case Queen => 900
      case King => 10_000
