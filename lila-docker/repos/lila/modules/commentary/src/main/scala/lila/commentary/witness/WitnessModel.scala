package lila.commentary.witness

import chess.{ Color, File, Role, Square }

opaque type WitnessDescriptorId = String
object WitnessDescriptorId:

  def apply(value: String): WitnessDescriptorId =
    val normalized = value.trim
    require(
      normalized.matches("^[a-z][a-z0-9_]*$"),
      s"Invalid witness descriptor id: '$value'"
    )
    normalized

  extension (id: WitnessDescriptorId) def value: String = id

opaque type WitnessVariantId = String
object WitnessVariantId:

  def apply(value: String): WitnessVariantId =
    val normalized = value.trim
    require(
      normalized.matches("^[a-z][a-z0-9_]*$"),
      s"Invalid witness variant id: '$value'"
    )
    normalized

  extension (id: WitnessVariantId) def value: String = id

enum WitnessPolarity:
  case Neutral
  case Owner
  case Beneficiary
  case Host

enum WitnessSector(val key: String):
  case Center extends WitnessSector("center")
  case Kingside extends WitnessSector("kingside")
  case Queenside extends WitnessSector("queenside")

enum WitnessDirection(val deltaFile: Int, val deltaRank: Int, val key: String):
  case North extends WitnessDirection(0, 1, "north")
  case NorthEast extends WitnessDirection(1, 1, "north_east")
  case East extends WitnessDirection(1, 0, "east")
  case SouthEast extends WitnessDirection(1, -1, "south_east")
  case South extends WitnessDirection(0, -1, "south")
  case SouthWest extends WitnessDirection(-1, -1, "south_west")
  case West extends WitnessDirection(-1, 0, "west")
  case NorthWest extends WitnessDirection(-1, 1, "north_west")

  def step(square: Square): Option[Square] =
    Square.at(square.file.value + deltaFile, square.rank.value + deltaRank)

  def raySquaresFrom(source: Square): Vector[Square] =
    val builder = Vector.newBuilder[Square]
    var next = step(source)
    while next.nonEmpty do
      val square = next.get
      builder += square
      next = step(square)
    builder.result()

  def isDiagonal: Boolean = deltaFile != 0 && deltaRank != 0

  def isOrthogonal: Boolean = !isDiagonal

object WitnessDirection:

  val orthogonal: Vector[WitnessDirection] =
    Vector(WitnessDirection.North, WitnessDirection.East, WitnessDirection.South, WitnessDirection.West)

  val diagonal: Vector[WitnessDirection] =
    Vector(
      WitnessDirection.NorthEast,
      WitnessDirection.SouthEast,
      WitnessDirection.SouthWest,
      WitnessDirection.NorthWest
    )

  val slider: Vector[WitnessDirection] = orthogonal ++ diagonal

  def between(source: Square, target: Square): Option[WitnessDirection] =
    val fileDelta = target.file.value - source.file.value
    val rankDelta = target.rank.value - source.rank.value

    if fileDelta == 0 && rankDelta > 0 then Some(WitnessDirection.North)
    else if fileDelta == 0 && rankDelta < 0 then Some(WitnessDirection.South)
    else if rankDelta == 0 && fileDelta > 0 then Some(WitnessDirection.East)
    else if rankDelta == 0 && fileDelta < 0 then Some(WitnessDirection.West)
    else if math.abs(fileDelta) == math.abs(rankDelta) && fileDelta > 0 && rankDelta > 0 then
      Some(WitnessDirection.NorthEast)
    else if math.abs(fileDelta) == math.abs(rankDelta) && fileDelta > 0 && rankDelta < 0 then
      Some(WitnessDirection.SouthEast)
    else if math.abs(fileDelta) == math.abs(rankDelta) && fileDelta < 0 && rankDelta < 0 then
      Some(WitnessDirection.SouthWest)
    else if math.abs(fileDelta) == math.abs(rankDelta) && fileDelta < 0 && rankDelta > 0 then
      Some(WitnessDirection.NorthWest)
    else None

final case class WitnessRay(source: Square, direction: WitnessDirection):

  def key: String = s"${source.key}:${direction.key}"

  def squares: Vector[Square] = direction.raySquaresFrom(source)

  def contains(target: Square): Boolean =
    WitnessDirection.between(source, target).contains(direction)

object WitnessRay:

  def between(source: Square, target: Square): Option[WitnessRay] =
    WitnessDirection.between(source, target).map(direction => WitnessRay(source, direction))

enum WitnessAnchorKind(val key: String, val sortOrder: Int):
  case Board extends WitnessAnchorKind("board", 0)
  case Sector extends WitnessAnchorKind("sector", 1)
  case File extends WitnessAnchorKind("file", 2)
  case Square extends WitnessAnchorKind("square", 3)
  case PieceSquare extends WitnessAnchorKind("piece_square", 4)
  case Ray extends WitnessAnchorKind("ray", 5)

sealed trait WitnessAnchor:
  def kind: WitnessAnchorKind
  def key: String

object WitnessAnchor:

  case object BoardAnchor extends WitnessAnchor:
    val kind: WitnessAnchorKind = WitnessAnchorKind.Board
    val key: String = kind.key

  final case class FileAnchor(file: File) extends WitnessAnchor:
    val kind: WitnessAnchorKind = WitnessAnchorKind.File
    val key: String = file.char.toString

  final case class PieceSquareAnchor(square: Square) extends WitnessAnchor:
    val kind: WitnessAnchorKind = WitnessAnchorKind.PieceSquare
    val key: String = square.key

  final case class SquareAnchor(square: Square) extends WitnessAnchor:
    val kind: WitnessAnchorKind = WitnessAnchorKind.Square
    val key: String = square.key

  final case class SectorAnchor(sector: WitnessSector) extends WitnessAnchor:
    val kind: WitnessAnchorKind = WitnessAnchorKind.Sector
    val key: String = sector.key

  final case class RayAnchor(ray: WitnessRay) extends WitnessAnchor:
    val kind: WitnessAnchorKind = WitnessAnchorKind.Ray
    val key: String = ray.key

enum WitnessValue:
  case Token(value: String)
  case Number(value: Int)
  case BooleanValue(value: Boolean)
  case ColorValue(color: Color)
  case FileValue(file: File)
  case SquareValue(square: Square)
  case RoleValue(role: Role)
  case SectorValue(sector: WitnessSector)
  case RayValue(ray: WitnessRay)
  case DirectionValue(direction: WitnessDirection)
  case TokenListValue(values: Vector[String])
  case SquareListValue(values: Vector[Square])
  case RoleListValue(values: Vector[Role])
  case DirectionListValue(values: Vector[WitnessDirection])
  case IntListValue(values: Vector[Int])
  case LongMaskValue(value: Long)
  case FileMaskValue(value: Int)
  case ListValue(values: Vector[WitnessValue])
  case ObjectValue(payload: WitnessPayload)

  def merge(other: WitnessValue): WitnessValue =
    (this, other) match
      case (left, right) if left == right => left
      case (WitnessValue.TokenListValue(left), WitnessValue.TokenListValue(right)) =>
        WitnessValue.TokenListValue(WitnessValue.unionDistinct(left ++ right))
      case (WitnessValue.SquareListValue(left), WitnessValue.SquareListValue(right)) =>
        WitnessValue.SquareListValue(WitnessValue.unionDistinct(left ++ right).sortBy(_.value))
      case (WitnessValue.RoleListValue(left), WitnessValue.RoleListValue(right)) =>
        WitnessValue.RoleListValue(WitnessValue.unionDistinct(left ++ right).sortBy(_.toString))
      case (WitnessValue.DirectionListValue(left), WitnessValue.DirectionListValue(right)) =>
        WitnessValue.DirectionListValue(
          WitnessValue.unionDistinct(left ++ right).sortBy(_.ordinal)
        )
      case (WitnessValue.IntListValue(left), WitnessValue.IntListValue(right)) =>
        WitnessValue.IntListValue(WitnessValue.unionDistinct(left ++ right).sorted)
      case (WitnessValue.LongMaskValue(left), WitnessValue.LongMaskValue(right)) =>
        WitnessValue.LongMaskValue(left | right)
      case (WitnessValue.FileMaskValue(left), WitnessValue.FileMaskValue(right)) =>
        WitnessValue.FileMaskValue(left | right)
      case (WitnessValue.ListValue(left), WitnessValue.ListValue(right)) =>
        WitnessValue.ListValue(WitnessValue.unionDistinct(left ++ right))
      case (WitnessValue.ObjectValue(left), WitnessValue.ObjectValue(right)) =>
        WitnessValue.ObjectValue(left.merge(right))
      case _ =>
        throw IllegalArgumentException(s"Conflicting witness payload values: $this vs $other")

object WitnessValue:

  private[witness] def unionDistinct[A](values: IterableOnce[A]): Vector[A] =
    values.iterator.foldLeft(Vector.empty[A]):
      case (acc, value) if acc.contains(value) => acc
      case (acc, value) => acc :+ value

final case class WitnessPayload private (entries: Vector[(String, WitnessValue)]):

  entries.foreach: (field, _) =>
    require(
      WitnessPayload.isValidFieldName(field),
      s"Invalid witness payload field: '$field'"
    )

  private val duplicateFields =
    entries.groupBy(_._1).collect { case (field, values) if values.size > 1 => field }.toVector.sorted

  require(
    duplicateFields.isEmpty,
    s"Duplicate witness payload fields: ${duplicateFields.mkString(", ")}"
  )

  lazy val fieldsByName: Map[String, WitnessValue] = entries.toMap

  def get(field: String): Option[WitnessValue] = fieldsByName.get(field)

  def updated(field: String, value: WitnessValue): WitnessPayload =
    val existingIndex = entries.indexWhere(_._1 == field)
    if existingIndex >= 0 then
      WitnessPayload.from(entries.updated(existingIndex, field -> value))
    else WitnessPayload.from(entries :+ (field -> value))

  def merge(other: WitnessPayload): WitnessPayload =
    other.entries.foldLeft(this):
      case (payload, (field, value)) =>
        payload.get(field) match
          case Some(existing) => payload.updated(field, existing.merge(value))
          case None => payload.updated(field, value)

  def ++(other: WitnessPayload): WitnessPayload = merge(other)

  def isEmpty: Boolean = entries.isEmpty

object WitnessPayload:

  val empty: WitnessPayload = new WitnessPayload(Vector.empty)

  def apply(entries: (String, WitnessValue)*): WitnessPayload = from(entries)

  def from(entries: IterableOnce[(String, WitnessValue)]): WitnessPayload =
    new WitnessPayload(entries.iterator.toVector)

  private[witness] def isValidFieldName(field: String): Boolean =
    field.matches("^[a-z][A-Za-z0-9_]*$")

final case class WitnessSupport private (
    rootIndices: Vector[Int],
    targetSquares: Vector[Square],
    geometryMasks: Vector[(String, Long)],
    supportingTags: Vector[String]
):

  geometryMasks.foreach: (name, _) =>
    require(
      WitnessPayload.isValidFieldName(name),
      s"Invalid witness geometry mask name: '$name'"
    )

  supportingTags.foreach: tag =>
    require(
      WitnessPayload.isValidFieldName(tag),
      s"Invalid witness support tag: '$tag'"
    )

  private val duplicateMaskNames =
    geometryMasks.groupBy(_._1).collect { case (name, values) if values.size > 1 => name }.toVector.sorted

  require(
    duplicateMaskNames.isEmpty,
    s"Duplicate witness geometry masks: ${duplicateMaskNames.mkString(", ")}"
  )

  lazy val geometryMaskMap: Map[String, Long] = geometryMasks.toMap

  def addRootIndex(index: Int): WitnessSupport =
    copy(rootIndices = WitnessValue.unionDistinct(rootIndices :+ index).sorted)

  def addTargetSquare(square: Square): WitnessSupport =
    copy(targetSquares = WitnessValue.unionDistinct(targetSquares :+ square).sortBy(_.value))

  def addGeometryMask(name: String, mask: Long): WitnessSupport =
    geometryMask(name)
      .map(existing => updatedGeometryMask(name, existing | mask))
      .getOrElse(copy(geometryMasks = geometryMasks :+ (name -> mask)))

  def addTag(tag: String): WitnessSupport =
    copy(supportingTags = WitnessValue.unionDistinct(supportingTags :+ tag).sorted)

  def geometryMask(name: String): Option[Long] = geometryMaskMap.get(name)

  def merge(other: WitnessSupport): WitnessSupport =
    val mergedMasks =
      (geometryMaskMap.keySet ++ other.geometryMaskMap.keySet).toVector.sorted.map: name =>
        name -> (geometryMaskMap.getOrElse(name, 0L) | other.geometryMaskMap.getOrElse(name, 0L))

    WitnessSupport(
      rootIndices = WitnessValue.unionDistinct(rootIndices ++ other.rootIndices).sorted,
      targetSquares = WitnessValue.unionDistinct(targetSquares ++ other.targetSquares).sortBy(_.value),
      geometryMasks = mergedMasks,
      supportingTags = WitnessValue.unionDistinct(supportingTags ++ other.supportingTags).sorted
    )

  def isEmpty: Boolean =
    rootIndices.isEmpty && targetSquares.isEmpty && geometryMasks.isEmpty && supportingTags.isEmpty

  private def updatedGeometryMask(name: String, mask: Long): WitnessSupport =
    val index = geometryMasks.indexWhere(_._1 == name)
    if index >= 0 then copy(geometryMasks = geometryMasks.updated(index, name -> mask))
    else copy(geometryMasks = geometryMasks :+ (name -> mask))

object WitnessSupport:

  val empty: WitnessSupport = WitnessSupport(
    rootIndices = Vector.empty,
    targetSquares = Vector.empty,
    geometryMasks = Vector.empty,
    supportingTags = Vector.empty
  )

  def roots(indices: Int*): WitnessSupport =
    empty.copy(rootIndices = WitnessValue.unionDistinct(indices).sorted)

  def geometryMask(name: String, mask: Long): WitnessSupport =
    empty.addGeometryMask(name, mask)

  def targets(squares: Square*): WitnessSupport =
    empty.copy(targetSquares = WitnessValue.unionDistinct(squares).sortBy(_.value))

  def tags(tags: String*): WitnessSupport =
    empty.copy(supportingTags = WitnessValue.unionDistinct(tags).sorted)

final case class Witness(
    descriptorId: WitnessDescriptorId,
    anchor: WitnessAnchor,
    polarity: WitnessPolarity,
    color: Option[Color] = None,
    payload: WitnessPayload = WitnessPayload.empty,
    support: WitnessSupport = WitnessSupport.empty,
    variant: Option[WitnessVariantId] = None
):

  polarity match
    case WitnessPolarity.Neutral =>
      require(color.isEmpty, "Neutral witnesses must not carry a color")
    case WitnessPolarity.Owner | WitnessPolarity.Beneficiary =>
      require(color.nonEmpty, s"$polarity witnesses must carry a color")
    case WitnessPolarity.Host => ()

  def merge(other: Witness): Witness =
    require(
      mergeKey == other.mergeKey,
      s"Cannot merge different witness identities: $mergeKey vs ${other.mergeKey}"
    )
    copy(
      payload = payload.merge(other.payload),
      support = support.merge(other.support)
    )

  def sortKey: String =
    val variantKey = variant.map(_.value).getOrElse("")
    f"${descriptorId.value}|${variantKey}|${polarity.ordinal}%02d|${colorSortOrder}%02d|${anchor.kind.sortOrder}%02d|${anchor.key}"

  private[witness] def mergeKey: Witness.MergeKey =
    Witness.MergeKey(descriptorId, anchor, polarity, color, variant)

  private def colorSortOrder: Int =
    color match
      case Some(c) if c.white => 0
      case Some(_) => 1
      case None => 2

object Witness:

  private[witness] final case class MergeKey(
      descriptorId: WitnessDescriptorId,
      anchor: WitnessAnchor,
      polarity: WitnessPolarity,
      color: Option[Color],
      variant: Option[WitnessVariantId]
  )

  def neutral(
      descriptorId: WitnessDescriptorId,
      anchor: WitnessAnchor,
      payload: WitnessPayload = WitnessPayload.empty,
      support: WitnessSupport = WitnessSupport.empty,
      variant: Option[WitnessVariantId] = None
  ): Witness =
    Witness(
      descriptorId = descriptorId,
      anchor = anchor,
      polarity = WitnessPolarity.Neutral,
      color = None,
      payload = payload,
      support = support,
      variant = variant
    )

  def owner(
      descriptorId: WitnessDescriptorId,
      color: Color,
      anchor: WitnessAnchor,
      payload: WitnessPayload = WitnessPayload.empty,
      support: WitnessSupport = WitnessSupport.empty,
      variant: Option[WitnessVariantId] = None
  ): Witness =
    Witness(
      descriptorId = descriptorId,
      anchor = anchor,
      polarity = WitnessPolarity.Owner,
      color = Some(color),
      payload = payload,
      support = support,
      variant = variant
    )

  def beneficiary(
      descriptorId: WitnessDescriptorId,
      color: Color,
      anchor: WitnessAnchor,
      payload: WitnessPayload = WitnessPayload.empty,
      support: WitnessSupport = WitnessSupport.empty,
      variant: Option[WitnessVariantId] = None
  ): Witness =
    Witness(
      descriptorId = descriptorId,
      anchor = anchor,
      polarity = WitnessPolarity.Beneficiary,
      color = Some(color),
      payload = payload,
      support = support,
      variant = variant
    )

  def host(
      descriptorId: WitnessDescriptorId,
      anchor: WitnessAnchor,
      color: Option[Color] = None,
      payload: WitnessPayload = WitnessPayload.empty,
      support: WitnessSupport = WitnessSupport.empty,
      variant: Option[WitnessVariantId] = None
  ): Witness =
    Witness(
      descriptorId = descriptorId,
      anchor = anchor,
      polarity = WitnessPolarity.Host,
      color = color,
      payload = payload,
      support = support,
      variant = variant
    )

final case class WitnessSet private (all: Vector[Witness]):

  lazy val byDescriptorId: Map[WitnessDescriptorId, Vector[Witness]] =
    all.groupBy(_.descriptorId).view.mapValues(_.sortBy(_.sortKey)).toMap

  def forDescriptorId(descriptorId: WitnessDescriptorId): Vector[Witness] =
    byDescriptorId.getOrElse(descriptorId, Vector.empty)

  def forAnchor(anchor: WitnessAnchor): Vector[Witness] =
    all.filter(_.anchor == anchor)

  def contains(
      descriptorId: WitnessDescriptorId,
      anchor: WitnessAnchor,
      polarity: Option[WitnessPolarity] = None,
      color: Option[Color] = None,
      variant: Option[WitnessVariantId] = None
  ): Boolean =
    all.exists: witness =>
      witness.descriptorId == descriptorId &&
        witness.anchor == anchor &&
        polarity.forall(_ == witness.polarity) &&
        witness.color == color &&
        witness.variant == variant

  def isEmpty: Boolean = all.isEmpty

object WitnessSet:

  val empty: WitnessSet = new WitnessSet(Vector.empty)

  def apply(witnesses: IterableOnce[Witness]): WitnessSet =
    val merged =
      witnesses.iterator.foldLeft(Map.empty[Witness.MergeKey, Witness]):
        case (acc, witness) =>
          acc.updatedWith(witness.mergeKey):
            case Some(existing) => Some(existing.merge(witness))
            case None => Some(witness)

    new WitnessSet(merged.values.toVector.sortBy(_.sortKey))
