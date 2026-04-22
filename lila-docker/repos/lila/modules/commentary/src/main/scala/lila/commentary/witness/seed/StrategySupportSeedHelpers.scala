package lila.commentary.witness.seed

import chess.{ Bishop, King, Knight, Pawn, Queen, Role, Rook, Square }

import lila.commentary.witness.WitnessSupport

private[seed] object StrategySupportSeedHelpers:

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

  def pieceValue(role: Role): Int =
    role match
      case Pawn => 100
      case Knight | Bishop => 300
      case Rook => 500
      case Queen => 900
      case King => 10_000
