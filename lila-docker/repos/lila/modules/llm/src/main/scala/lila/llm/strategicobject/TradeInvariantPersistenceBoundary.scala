package lila.llm.strategicobject

import chess.Square

private[strategicobject] object TradeInvariantPersistenceBoundary:

  private val PersistingAnchorFeatures = Set(
    TradeInvariantFeature.FixedTargetAnchor,
    TradeInvariantFeature.BreakAnchor,
    TradeInvariantFeature.AccessAnchor
  )

  def persistenceWitnessCount(
      exchangeSquares: List[Square],
      invariantSquares: List[Square],
      preservedFamilies: Set[StrategicObjectFamily],
      features: Set[TradeInvariantFeature]
  ): Int =
    invariantSquares.diff(exchangeSquares).distinct.size +
      preservedFamilies.intersect(
        Set(
          StrategicObjectFamily.FixedTargetComplex,
          StrategicObjectFamily.BreakAxis,
          StrategicObjectFamily.AccessNetwork
        )
      ).size +
      Option.when(hasPersistingAnchor(features))(1).getOrElse(0)

  def hasPersistenceWitness(
      exchangeSquares: List[Square],
      invariantSquares: List[Square],
      preservedFamilies: Set[StrategicObjectFamily],
      features: Set[TradeInvariantFeature]
  ): Boolean =
    persistenceWitnessCount(exchangeSquares, invariantSquares, preservedFamilies, features) > 0

  def hasPersistingAnchor(
      features: Set[TradeInvariantFeature]
  ): Boolean =
    features.intersect(PersistingAnchorFeatures).nonEmpty

  def eligibleForPrimarySimplification(
      exchangeSquares: List[Square],
      invariantSquares: List[Square],
      preservedFamilies: Set[StrategicObjectFamily],
      features: Set[TradeInvariantFeature]
  ): Boolean =
    exchangeSquares.nonEmpty &&
      invariantSquares.nonEmpty &&
      preservedFamilies.nonEmpty &&
      !preservedFamilies.contains(StrategicObjectFamily.PasserComplex) &&
      !features.contains(TradeInvariantFeature.PasserAnchor) &&
      hasPersistenceWitness(exchangeSquares, invariantSquares, preservedFamilies, features)
