package lila.llm.strategicobject

import chess.Color

final case class StrategicObject(
    id: String,
    family: StrategicObjectFamily,
    owner: Color,
    locus: StrategicObjectLocus,
    sector: ObjectSector,
    anchors: List[StrategicObjectAnchor],
    profile: StrategicObjectProfile,
    supportingPrimitives: List[PrimitiveReference],
    supportingPieces: List[StrategicPieceRef],
    rivalResourcesOrObjects: List[StrategicRivalReference],
    relations: List[StrategicRelation],
    stateStrength: StrategicObjectStateStrength,
    readiness: StrategicObjectReadiness,
    horizonClass: ObjectHorizonClass,
    evidenceFootprint: StrategicObjectEvidenceFootprint
):
  require(id.nonEmpty, "strategic object id must be non-empty")
  require(anchors.nonEmpty, "strategic object must carry anchors")
  require(profile.family == family, s"strategic object profile ${profile.family} must match family $family")
  require(supportingPrimitives.nonEmpty, "strategic object must carry supporting primitives")

  def normalized: StrategicObject =
    copy(
      locus = locus.normalized,
      anchors = anchors.map(_.normalized).distinct.sortBy(anchorSortKey),
      supportingPrimitives = supportingPrimitives.map(_.normalized).distinct.sortBy(primitiveSortKey),
      supportingPieces = supportingPieces.map(_.normalized).distinct.sortBy(pieceSortKey),
      rivalResourcesOrObjects = rivalResourcesOrObjects.map(_.normalized).distinct.sortBy(rivalSortKey),
      relations = relations.distinct.sortBy(relationSortKey),
      evidenceFootprint = evidenceFootprint.normalized
    )

private def anchorSortKey(anchor: StrategicObjectAnchor): String =
  val squarePart = anchor.squares.map(_.key).mkString("-")
  val filePart = anchor.file.map(_.char.toString).getOrElse("")
  val rolePart = anchor.piece.toList.flatMap(_.roles).map(_.toString).toList.sorted.mkString("-")
  val routePart = anchor.route.toList.flatMap(_.allSquares).map(_.key).mkString("-")
  s"${anchor.kind.toString}-${anchor.role.toString}-$squarePart-$filePart-$rolePart-$routePart"

private def primitiveSortKey(ref: PrimitiveReference): String =
  val squarePart = ref.allSquares.map(_.key).mkString("-")
  val lanePart = ref.lane.map(_.char.toString).getOrElse("")
  val rolePart = ref.roles.toList.map(_.toString).sorted.mkString("-")
  s"${ref.kind.toString}-${showColor(ref.owner)}-$squarePart-$lanePart-$rolePart"

private def pieceSortKey(piece: StrategicPieceRef): String =
  val squarePart = piece.squares.map(_.key).mkString("-")
  val rolePart = piece.roles.toList.map(_.toString).sorted.mkString("-")
  s"${showColor(piece.owner)}-$squarePart-$rolePart"

private def rivalSortKey(rival: StrategicRivalReference): String =
  val squarePart = rival.squares.map(_.key).mkString("-")
  val rolePart = rival.roles.toList.map(_.toString).sorted.mkString("-")
  s"${rival.kind.toString}-${showColor(rival.owner)}-$squarePart-${rival.file.map(_.char.toString).getOrElse("")}-$rolePart-${rival.primitiveKind.map(_.toString).getOrElse("")}-${rival.objectId.getOrElse("")}"

private def relationSortKey(relation: StrategicRelation): String =
  s"${relation.operator.toString}-${relation.target.objectId}"

private def showColor(color: Color): String =
  if color.white then "white" else "black"
