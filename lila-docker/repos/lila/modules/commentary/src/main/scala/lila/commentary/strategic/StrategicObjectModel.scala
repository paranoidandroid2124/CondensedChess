package lila.commentary.strategic

import scala.annotation.targetName

import chess.Color

import lila.commentary.witness.*

opaque type StrategicObjectId = String
object StrategicObjectId:

  def apply(value: String): StrategicObjectId =
    val normalized = value.trim
    require(
      normalized.matches("^[A-Z][A-Za-z0-9]*$"),
      s"Invalid strategic object id: '$value'"
    )
    normalized

  extension (id: StrategicObjectId) def value: String = id

final case class StrategicObject(
    familyId: StrategicObjectId,
    anchor: WitnessAnchor,
    color: Option[Color] = None,
    payload: WitnessPayload = WitnessPayload.empty,
    support: WitnessSupport = WitnessSupport.empty
):

  def merge(other: StrategicObject): StrategicObject =
    require(
      mergeKey == other.mergeKey,
      s"Cannot merge different strategic object identities: $mergeKey vs ${other.mergeKey}"
    )
    copy(
      payload = payload.merge(other.payload),
      support = support.merge(other.support)
    )

  def sortKey: String =
    f"${familyId.value}|${colorSortOrder}%02d|${anchor.kind.sortOrder}%02d|${anchor.key}"

  private[strategic] def mergeKey: StrategicObject.MergeKey =
    StrategicObject.MergeKey(familyId, anchor, color)

  private def colorSortOrder: Int =
    color match
      case Some(c) if c.white => 0
      case Some(_) => 1
      case None => 2

object StrategicObject:

  private[strategic] final case class MergeKey(
      familyId: StrategicObjectId,
      anchor: WitnessAnchor,
      color: Option[Color]
  )

final case class StrategicObjectSet private (all: Vector[StrategicObject]):

  lazy val byFamilyId: Map[StrategicObjectId, Vector[StrategicObject]] =
    all.groupBy(_.familyId).view.mapValues(_.sortBy(_.sortKey)).toMap

  def forFamilyId(familyId: StrategicObjectId): Vector[StrategicObject] =
    byFamilyId.getOrElse(familyId, Vector.empty)

  @targetName("forFamilyIdString")
  def forFamilyId(familyId: String): Vector[StrategicObject] =
    forFamilyId(StrategicObjectId(familyId))

  def contains(
      familyId: StrategicObjectId,
      anchor: WitnessAnchor,
      color: Option[Color] = None
  ): Boolean =
    all.exists(obj =>
      obj.familyId == familyId &&
        obj.anchor == anchor &&
        color.forall(obj.color.contains)
    )

  @targetName("containsString")
  def contains(
      familyId: String,
      anchor: WitnessAnchor,
      color: Option[Color]
  ): Boolean =
    contains(StrategicObjectId(familyId), anchor, color)

  def isEmpty: Boolean = all.isEmpty

object StrategicObjectSet:

  val empty: StrategicObjectSet = new StrategicObjectSet(Vector.empty)

  def apply(objects: IterableOnce[StrategicObject]): StrategicObjectSet =
    val merged =
      objects.iterator.foldLeft(Map.empty[StrategicObject.MergeKey, StrategicObject]):
        case (acc, obj) =>
          acc.updatedWith(obj.mergeKey):
            case Some(existing) => Some(existing.merge(obj))
            case None => Some(obj)

    new StrategicObjectSet(merged.values.toVector.sortBy(_.sortKey))
