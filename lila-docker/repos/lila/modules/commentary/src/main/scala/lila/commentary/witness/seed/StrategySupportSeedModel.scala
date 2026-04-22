package lila.commentary.witness.seed

import chess.Color

import lila.commentary.witness.{
  WitnessAnchor,
  WitnessPayload,
  WitnessPolarity,
  WitnessSupport,
  WitnessVariantId
}

opaque type StrategySupportSeedId = String
object StrategySupportSeedId:

  def apply(value: String): StrategySupportSeedId =
    val normalized = value.trim
    require(
      normalized.matches("^[a-z][a-z0-9_]*$"),
      s"Invalid strategy support seed id: '$value'"
    )
    normalized

  extension (id: StrategySupportSeedId) def value: String = id

final case class StrategySupportSeed(
    seedId: StrategySupportSeedId,
    anchor: WitnessAnchor,
    polarity: WitnessPolarity,
    color: Option[Color] = None,
    payload: WitnessPayload = WitnessPayload.empty,
    support: WitnessSupport = WitnessSupport.empty,
    variant: Option[WitnessVariantId] = None
):

  polarity match
    case WitnessPolarity.Neutral =>
      require(color.isEmpty, "Neutral support seeds must not carry a color")
    case WitnessPolarity.Owner | WitnessPolarity.Beneficiary =>
      require(color.nonEmpty, s"$polarity support seeds must carry a color")
    case WitnessPolarity.Host => ()

  def merge(other: StrategySupportSeed): StrategySupportSeed =
    require(
      mergeKey == other.mergeKey,
      s"Cannot merge different strategy support seed identities: $mergeKey vs ${other.mergeKey}"
    )
    copy(
      payload = payload.merge(other.payload),
      support = support.merge(other.support)
    )

  def sortKey: String =
    val variantKey = variant.map(_.value).getOrElse("")
    f"${seedId.value}|${variantKey}|${polarity.ordinal}%02d|${colorSortOrder}%02d|${anchor.kind.sortOrder}%02d|${anchor.key}"

  private[seed] def mergeKey: StrategySupportSeed.MergeKey =
    StrategySupportSeed.MergeKey(seedId, anchor, polarity, color, variant)

  private def colorSortOrder: Int =
    color match
      case Some(c) if c.white => 0
      case Some(_) => 1
      case None => 2

object StrategySupportSeed:

  private[seed] final case class MergeKey(
      seedId: StrategySupportSeedId,
      anchor: WitnessAnchor,
      polarity: WitnessPolarity,
      color: Option[Color],
      variant: Option[WitnessVariantId]
  )

  def neutral(
      seedId: StrategySupportSeedId,
      anchor: WitnessAnchor,
      payload: WitnessPayload = WitnessPayload.empty,
      support: WitnessSupport = WitnessSupport.empty,
      variant: Option[WitnessVariantId] = None
  ): StrategySupportSeed =
    StrategySupportSeed(
      seedId = seedId,
      anchor = anchor,
      polarity = WitnessPolarity.Neutral,
      color = None,
      payload = payload,
      support = support,
      variant = variant
    )

  def owner(
      seedId: StrategySupportSeedId,
      color: Color,
      anchor: WitnessAnchor,
      payload: WitnessPayload = WitnessPayload.empty,
      support: WitnessSupport = WitnessSupport.empty,
      variant: Option[WitnessVariantId] = None
  ): StrategySupportSeed =
    StrategySupportSeed(
      seedId = seedId,
      anchor = anchor,
      polarity = WitnessPolarity.Owner,
      color = Some(color),
      payload = payload,
      support = support,
      variant = variant
    )

  def beneficiary(
      seedId: StrategySupportSeedId,
      color: Color,
      anchor: WitnessAnchor,
      payload: WitnessPayload = WitnessPayload.empty,
      support: WitnessSupport = WitnessSupport.empty,
      variant: Option[WitnessVariantId] = None
  ): StrategySupportSeed =
    StrategySupportSeed(
      seedId = seedId,
      anchor = anchor,
      polarity = WitnessPolarity.Beneficiary,
      color = Some(color),
      payload = payload,
      support = support,
      variant = variant
    )

  def host(
      seedId: StrategySupportSeedId,
      anchor: WitnessAnchor,
      color: Option[Color] = None,
      payload: WitnessPayload = WitnessPayload.empty,
      support: WitnessSupport = WitnessSupport.empty,
      variant: Option[WitnessVariantId] = None
  ): StrategySupportSeed =
    StrategySupportSeed(
      seedId = seedId,
      anchor = anchor,
      polarity = WitnessPolarity.Host,
      color = color,
      payload = payload,
      support = support,
      variant = variant
    )

final case class StrategySupportSeedSet private (all: Vector[StrategySupportSeed]):

  lazy val bySeedId: Map[StrategySupportSeedId, Vector[StrategySupportSeed]] =
    all.groupBy(_.seedId).view.mapValues(_.sortBy(_.sortKey)).toMap

  def forSeedId(seedId: StrategySupportSeedId): Vector[StrategySupportSeed] =
    bySeedId.getOrElse(seedId, Vector.empty)

  def contains(
      seedId: StrategySupportSeedId,
      anchor: WitnessAnchor,
      polarity: Option[WitnessPolarity] = None,
      color: Option[Color] = None,
      variant: Option[WitnessVariantId] = None
  ): Boolean =
    all.exists: seed =>
      seed.seedId == seedId &&
        seed.anchor == anchor &&
        polarity.forall(_ == seed.polarity) &&
        seed.color == color &&
        seed.variant == variant

  def isEmpty: Boolean = all.isEmpty

object StrategySupportSeedSet:

  val empty: StrategySupportSeedSet = new StrategySupportSeedSet(Vector.empty)

  def apply(seeds: IterableOnce[StrategySupportSeed]): StrategySupportSeedSet =
    val merged =
      seeds.iterator.foldLeft(Map.empty[StrategySupportSeed.MergeKey, StrategySupportSeed]):
        case (acc, seed) =>
          acc.updatedWith(seed.mergeKey):
            case Some(existing) => Some(existing.merge(seed))
            case None => Some(seed)

    new StrategySupportSeedSet(merged.values.toVector.sortBy(_.sortKey))
