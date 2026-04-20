package lila.commentary.delta

import scala.annotation.targetName

import chess.Color
import chess.format.Uci

import lila.commentary.strategic.StrategicObjectExtraction
import lila.commentary.witness.*

opaque type StrategicDeltaId = String
object StrategicDeltaId:

  def apply(value: String): StrategicDeltaId =
    val normalized = value.trim
    require(
      normalized.matches("^[A-Z][A-Za-z0-9]*$"),
      s"Invalid strategic delta id: '$value'"
    )
    normalized

  extension (id: StrategicDeltaId) def value: String = id

opaque type StrategicDeltaTag = String
object StrategicDeltaTag:

  def apply(value: String): StrategicDeltaTag =
    val normalized = value.trim
    require(
      normalized.matches("^[a-z][a-z0-9_]*$"),
      s"Invalid strategic delta tag: '$value'"
    )
    normalized

  extension (tag: StrategicDeltaTag) def value: String = tag

enum StrategicDeltaScope(val key: String):
  case MoveLocal extends StrategicDeltaScope("move_local")
  case PositionLocal extends StrategicDeltaScope("position_local")
  case Comparative extends StrategicDeltaScope("comparative")

object StrategicDeltaScope:

  def fromKey(key: String): Option[StrategicDeltaScope] =
    StrategicDeltaScope.values.find(_.key == key.trim)

final case class StrategicDelta(
    familyId: StrategicDeltaId,
    scope: StrategicDeltaScope,
    deltaTag: StrategicDeltaTag,
    anchor: WitnessAnchor,
    color: Option[Color] = None,
    payload: WitnessPayload = WitnessPayload.empty,
    support: WitnessSupport = WitnessSupport.empty
):

  def merge(other: StrategicDelta): StrategicDelta =
    require(
      mergeKey == other.mergeKey,
      s"Cannot merge different strategic delta identities: $mergeKey vs ${other.mergeKey}"
    )
    copy(
      payload = payload.merge(other.payload),
      support = support.merge(other.support)
    )

  def sortKey: String =
    f"${familyId.value}|${scope.key}|${deltaTag.value}|${colorSortOrder}%02d|${anchor.kind.sortOrder}%02d|${anchor.key}"

  private[delta] def mergeKey: StrategicDelta.MergeKey =
    StrategicDelta.MergeKey(familyId, scope, deltaTag, anchor, color)

  private def colorSortOrder: Int =
    color match
      case Some(c) if c.white => 0
      case Some(_) => 1
      case None => 2

object StrategicDelta:

  private[delta] final case class MergeKey(
      familyId: StrategicDeltaId,
      scope: StrategicDeltaScope,
      deltaTag: StrategicDeltaTag,
      anchor: WitnessAnchor,
      color: Option[Color]
  )

final case class StrategicDeltaSet private (all: Vector[StrategicDelta]):

  lazy val byFamilyId: Map[StrategicDeltaId, Vector[StrategicDelta]] =
    all.groupBy(_.familyId).view.mapValues(_.sortBy(_.sortKey)).toMap

  def forFamilyId(familyId: StrategicDeltaId): Vector[StrategicDelta] =
    byFamilyId.getOrElse(familyId, Vector.empty)

  @targetName("forFamilyIdString")
  def forFamilyId(familyId: String): Vector[StrategicDelta] =
    forFamilyId(StrategicDeltaId(familyId))

  def contains(
      familyId: StrategicDeltaId,
      anchor: WitnessAnchor,
      color: Option[Color] = None,
      scope: Option[StrategicDeltaScope] = None,
      deltaTag: Option[StrategicDeltaTag] = None
  ): Boolean =
    all.exists(delta =>
      delta.familyId == familyId &&
        delta.anchor == anchor &&
        color.forall(delta.color.contains) &&
        scope.forall(_ == delta.scope) &&
        deltaTag.forall(_ == delta.deltaTag)
    )

  @targetName("containsString")
  def contains(
      familyId: String,
      anchor: WitnessAnchor,
      color: Option[Color],
      scope: Option[StrategicDeltaScope],
      deltaTag: Option[StrategicDeltaTag]
  ): Boolean =
    contains(StrategicDeltaId(familyId), anchor, color, scope, deltaTag)

  def isEmpty: Boolean = all.isEmpty

object StrategicDeltaSet:

  val empty: StrategicDeltaSet = new StrategicDeltaSet(Vector.empty)

  def apply(deltas: IterableOnce[StrategicDelta]): StrategicDeltaSet =
    val merged =
      deltas.iterator.foldLeft(Map.empty[StrategicDelta.MergeKey, StrategicDelta]):
        case (acc, delta) =>
          acc.updatedWith(delta.mergeKey):
            case Some(existing) => Some(existing.merge(delta))
            case None => Some(delta)

    new StrategicDeltaSet(merged.values.toVector.sortBy(_.sortKey))

final case class StrategicDeltaExtraction(
    before: StrategicObjectExtraction,
    after: StrategicObjectExtraction,
    playedMove: Uci.Move,
    deltas: StrategicDeltaSet
)
