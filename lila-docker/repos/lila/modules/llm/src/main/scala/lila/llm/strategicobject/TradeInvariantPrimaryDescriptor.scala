package lila.llm.strategicobject

import chess.{ Color, Square }

enum TradeInvariantPrimaryReason:
  case PacketOwnedFixedTargetSlice
  case BreakBackedInvariant
  case EliteFixedTargetResidue

final case class TradeInvariantPrimaryDescriptor(
    primaryEligible: Boolean,
    packetPrimaryEligible: Boolean,
    timingPrimaryEligible: Boolean,
    primaryReason: Option[TradeInvariantPrimaryReason],
    primaryRelevantPreservedFamilies: Set[StrategicObjectFamily]
)

object TradeInvariantPrimaryDescriptor:

  private[strategicobject] val packetOwner: Color =
    Color.White

  private[strategicobject] val packetAnchor: Square =
    Square.fromKey("e6").getOrElse(
      throw new IllegalStateException("missing packet anchor e6")
    )

  private val PrimaryRelevantFamilies = Set(
    StrategicObjectFamily.FixedTargetComplex,
    StrategicObjectFamily.BreakAxis
  )

  def fromObject(
      obj: StrategicObject
  ): Option[TradeInvariantPrimaryDescriptor] =
    fromProfile(obj.owner, obj.profile).map(descriptor =>
      applyPrimarySliceBoundary(
        descriptor,
        owner = obj.owner,
        anchorSquares = obj.anchors.flatMap(_.squares)
      )
    )

  def fromDelta(
      delta: StrategicObjectDelta
  ): Option[TradeInvariantPrimaryDescriptor] =
    fromProfile(delta.owner, delta.profile).map(descriptor =>
      applyPrimarySliceBoundary(
        descriptor,
        owner = delta.owner,
        anchorSquares = delta.changedAnchors.flatMap(_.squares)
      )
    )

  def fromClaim(
      claim: CertifiedClaim
  ): Option[TradeInvariantPrimaryDescriptor] =
    claim.delta.flatMap(fromDelta)

  def fromProfile(
      owner: Color,
      profile: StrategicObjectProfile
  ): Option[TradeInvariantPrimaryDescriptor] =
    profile match
      case StrategicObjectProfile.TradeInvariant(
            exchangeSquares,
            invariantSquares,
            _,
            preservedFamilies,
            features
          ) =>
        Some(
          describe(
            owner = owner,
            exchangeSquares = exchangeSquares,
            invariantSquares = invariantSquares,
            preservedFamilies = preservedFamilies,
            features = features
          )
        )
      case _ =>
        None

  def describe(
      owner: Color,
      exchangeSquares: List[Square],
      invariantSquares: List[Square],
      preservedFamilies: Set[StrategicObjectFamily],
      features: Set[TradeInvariantFeature]
  ): TradeInvariantPrimaryDescriptor =
    val packetPrimaryEligible =
      owner == packetOwner &&
        exchangeSquares.contains(packetAnchor) &&
        invariantSquares.nonEmpty &&
        preservedFamilies.contains(StrategicObjectFamily.FixedTargetComplex) &&
        features.contains(TradeInvariantFeature.FixedTargetAnchor) &&
        !preservedFamilies.contains(StrategicObjectFamily.PasserComplex) &&
        !features.contains(TradeInvariantFeature.PasserAnchor)
    val generalPrimaryReason =
      generalPrimaryEligibilityReason(
        exchangeSquares = exchangeSquares,
        invariantSquares = invariantSquares,
        preservedFamilies = preservedFamilies,
        features = features
      )
    val primaryReason =
      Option.when(packetPrimaryEligible)(TradeInvariantPrimaryReason.PacketOwnedFixedTargetSlice)
        .orElse(generalPrimaryReason)
    val primaryEligible =
      primaryReason.nonEmpty

    TradeInvariantPrimaryDescriptor(
      primaryEligible = primaryEligible,
      packetPrimaryEligible = packetPrimaryEligible,
      timingPrimaryEligible =
        primaryEligible && features.contains(TradeInvariantFeature.ReleaseOverlap),
      primaryReason = primaryReason,
      primaryRelevantPreservedFamilies =
        preservedFamilies.intersect(PrimaryRelevantFamilies)
    )

  private def generalPrimaryEligibilityReason(
      exchangeSquares: List[Square],
      invariantSquares: List[Square],
      preservedFamilies: Set[StrategicObjectFamily],
      features: Set[TradeInvariantFeature]
  ): Option[TradeInvariantPrimaryReason] =
    val persistenceCount =
      TradeInvariantPersistenceBoundary.persistenceWitnessCount(
        exchangeSquares,
        invariantSquares,
        preservedFamilies,
        features
      )
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
    val passesSharedPrimaryBoundary =
      exchangeSquares.nonEmpty &&
        invariantSquares.nonEmpty &&
        !preservedFamilies.contains(StrategicObjectFamily.PasserComplex) &&
        !features.contains(TradeInvariantFeature.PasserAnchor) &&
        !exchangeCascade &&
        persistenceCount > 0

    if !passesSharedPrimaryBoundary then None
    else if hasBreakInvariant then Some(TradeInvariantPrimaryReason.BreakBackedInvariant)
    else if accessOnlyFixedTarget && eliteAccessOnlyExchange && persistenceCount >= 7 then
      Some(TradeInvariantPrimaryReason.EliteFixedTargetResidue)
    else None

  private def applyPrimarySliceBoundary(
      descriptor: TradeInvariantPrimaryDescriptor,
      owner: Color,
      anchorSquares: List[Square]
  ): TradeInvariantPrimaryDescriptor =
    val onPrimarySliceBoundary =
      owner == packetOwner &&
        anchorSquares.distinct.contains(packetAnchor)

    if onPrimarySliceBoundary then descriptor
    else
      descriptor.copy(
        primaryEligible = false,
        packetPrimaryEligible = false,
        timingPrimaryEligible = false,
        primaryReason = None
      )
