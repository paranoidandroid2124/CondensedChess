package lila.llm.strategicobject

import chess.Square

private[strategicobject] object TradeInvariantPersistenceBoundary:

  private val PersistingAnchorFeatures = Set(
    TradeInvariantFeature.FixedTargetAnchor,
    TradeInvariantFeature.BreakAnchor
  )

  private val PersistingFamilies = Set(
    StrategicObjectFamily.FixedTargetComplex,
    StrategicObjectFamily.BreakAxis
  )

  def persistenceWitnessCount(
      exchangeSquares: List[Square],
      invariantSquares: List[Square],
      preservedFamilies: Set[StrategicObjectFamily],
      features: Set[TradeInvariantFeature]
  ): Int =
    invariantSquares.diff(exchangeSquares).distinct.size +
      preservedFamilies.intersect(PersistingFamilies).size +
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
    val persistenceCount =
      persistenceWitnessCount(exchangeSquares, invariantSquares, preservedFamilies, features)
    val hasBreakInvariant =
      preservedFamilies.contains(StrategicObjectFamily.BreakAxis) ||
        features.contains(TradeInvariantFeature.BreakAnchor)
    val accessOnlyFixedTarget =
      !hasBreakInvariant &&
        preservedFamilies.contains(StrategicObjectFamily.FixedTargetComplex) &&
        features.contains(TradeInvariantFeature.FixedTargetAnchor)
    val eliteAccessOnlyExchange =
      features.contains(TradeInvariantFeature.ReleaseOverlap) ||
      features.contains(TradeInvariantFeature.QueenExchange) ||
        features.contains(TradeInvariantFeature.DeepDefenderRemoval)
    val exchangeCascade =
      features.contains(TradeInvariantFeature.ExchangeCascade) ||
        exchangeSquares.distinct.size >= 3

    exchangeSquares.nonEmpty &&
      invariantSquares.nonEmpty &&
      !preservedFamilies.contains(StrategicObjectFamily.PasserComplex) &&
      !features.contains(TradeInvariantFeature.PasserAnchor) &&
      !exchangeCascade &&
      persistenceCount > 0 &&
      (
        hasBreakInvariant ||
          (accessOnlyFixedTarget &&
            eliteAccessOnlyExchange &&
            persistenceCount >= 7)
      )
