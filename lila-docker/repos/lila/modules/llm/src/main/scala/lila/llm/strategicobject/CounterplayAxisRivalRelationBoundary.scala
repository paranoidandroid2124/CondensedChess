package lila.llm.strategicobject

import chess.{ File, Square }

object CounterplayAxisRivalRelationBoundary:

  final case class RivalRelationWitness(
      targetId: String,
      operator: StrategicRelationOperator,
      sharedSquares: List[Square],
      sharedFiles: List[File]
  )

  final case class ExactRivalAdmission(
      witnesses: List[RivalRelationWitness] = Nil,
      rivalIds: Set[String] = Set.empty,
      relationOperators: Set[StrategicRelationOperator] = Set.empty,
      rivalFamilies: Set[StrategicObjectFamily] = Set.empty,
      typedAxisSupported: Boolean = false
  ):
    def admitted: Boolean =
      witnesses.nonEmpty && typedAxisSupported

  def hasExactRivalRelation(
      current: StrategicObject,
      objectsById: Map[String, StrategicObject]
  ): Boolean =
    exactRivalRelationWitnesses(current, objectsById).nonEmpty

  def exactRivalRelationWitnesses(
      current: StrategicObject,
      objectsById: Map[String, StrategicObject]
  ): List[RivalRelationWitness] =
    objectsById.values.toList.flatMap { target =>
      val operators =
        current.relations.collect {
          case relation if relation.target.objectId == target.id => relation.operator
        } ++
          target.relations.collect {
            case relation if relation.target.objectId == current.id => relation.operator
          }
      operators.distinct.flatMap { operator =>
        val sharedSquares =
          distinctSquares(
            objectSquares(current).intersect(objectSquares(target)) ++
              directRivalReferenceSquaresBetween(current, target)
          )
        val sharedFiles =
          distinctFiles(
            objectFiles(current).intersect(objectFiles(target)) ++
              directRivalReferenceFilesBetween(current, target)
          )
        Option.when(
          target.owner != current.owner &&
            operators.nonEmpty &&
            (sharedSquares.nonEmpty || sharedFiles.nonEmpty)
        )(
          RivalRelationWitness(
            targetId = target.id,
            operator = operator,
            sharedSquares = sharedSquares,
            sharedFiles = sharedFiles
          )
        )
      }
    }.distinct.sortBy(witness =>
      s"${witness.targetId}-${witness.operator.toString}-${witness.sharedSquares.map(_.key).mkString("-")}-${witness.sharedFiles.map(_.char).mkString}"
    )

  def hasExactRivalRelationTo(
      current: StrategicObject,
      target: StrategicObject
  ): Boolean =
    exactRivalRelationWitnessesTo(current, target).nonEmpty

  def exactRivalRelationWitnessesTo(
      current: StrategicObject,
      target: StrategicObject
  ): List[RivalRelationWitness] =
    exactRivalRelationWitnesses(current, Map(target.id -> target)).filter(_.targetId == target.id)

  def exactRivalAdmission(
      current: StrategicObject,
      objectsById: Map[String, StrategicObject]
  ): ExactRivalAdmission =
    val witnesses = exactRivalRelationWitnesses(current, objectsById)
    val rivalIds = witnesses.map(_.targetId).toSet
    val rivalFamilies =
      rivalIds.flatMap(targetId => objectsById.get(targetId).map(_.family))

    ExactRivalAdmission(
      witnesses = witnesses,
      rivalIds = rivalIds,
      relationOperators = witnesses.map(_.operator).toSet,
      rivalFamilies = rivalFamilies,
      typedAxisSupported = typedAxisSupport(current.profile, rivalFamilies)
    )

  private def directRivalReferenceSquaresBetween(
      current: StrategicObject,
      other: StrategicObject
  ): List[Square] =
    distinctSquares(
      rivalReferenceSquaresBetween(current, other) ++
        rivalReferenceSquaresBetween(other, current)
    )

  private def directRivalReferenceFilesBetween(
      current: StrategicObject,
      other: StrategicObject
  ): List[File] =
    distinctFiles(
      rivalReferenceFilesBetween(current, other) ++
        rivalReferenceFilesBetween(other, current)
    )

  private def rivalReferenceSquaresBetween(
      current: StrategicObject,
      other: StrategicObject
  ): List[Square] =
    current.rivalResourcesOrObjects.collect {
      case rival if rivalMatchesObject(rival, other) => rival.squares
    }.flatten

  private def rivalReferenceFilesBetween(
      current: StrategicObject,
      other: StrategicObject
  ): List[File] =
    current.rivalResourcesOrObjects.collect {
      case rival if rivalMatchesObject(rival, other) => rival.file
    }.flatten

  private def rivalMatchesObject(
      rival: StrategicRivalReference,
      other: StrategicObject
  ): Boolean =
    val alignedGeometry =
      rival.squares.exists(square => objectSquares(other).contains(square)) ||
        rival.file.exists(file => objectFiles(other).contains(file))

    alignedGeometry &&
      (
        rival.objectId.contains(other.id) ||
          rival.objectFamily.contains(other.family)
      )

  private def typedAxisSupport(
      profile: StrategicObjectProfile,
      rivalFamilies: Set[StrategicObjectFamily]
  ): Boolean =
    profile match
      case StrategicObjectProfile.CounterplayAxis(_, _, _, typedAxes) =>
        typedAxes.exists {
          case CounterplayAxisType.KingExposure =>
            rivalFamilies.contains(StrategicObjectFamily.KingSafetyShell)
          case _ =>
            rivalFamilies.exists(_ != StrategicObjectFamily.KingSafetyShell)
        }
      case _ =>
        false

  private def objectSquares(
      obj: StrategicObject
  ): List[Square] =
    distinctSquares(
      obj.locus.allSquares ++
        obj.anchors.flatMap(_.squares) ++
        obj.anchors.flatMap(_.route.toList.flatMap(_.allSquares)) ++
        obj.anchors.flatMap(_.piece.toList.flatMap(_.squares)) ++
        obj.supportingPrimitives.flatMap(_.allSquares) ++
        obj.supportingPieces.flatMap(_.squares) ++
        obj.evidenceFootprint.anchorSquares ++
        obj.evidenceFootprint.contestedSquares
    )

  private def objectFiles(
      obj: StrategicObject
  ): List[File] =
    distinctFiles(
      obj.locus.files ++
        obj.anchors.flatMap(_.file) ++
        obj.anchors.flatMap(anchor => anchor.squares.map(_.file)) ++
        obj.anchors.flatMap(_.route.toList.flatMap(_.allSquares.map(_.file))) ++
        obj.supportingPrimitives.flatMap(ref => ref.lane.toList ++ ref.allSquares.map(_.file)) ++
        obj.supportingPieces.flatMap(_.squares.map(_.file)) ++
        obj.evidenceFootprint.lanes ++
        obj.evidenceFootprint.anchorSquares.map(_.file) ++
        obj.evidenceFootprint.contestedSquares.map(_.file)
    )

  private def distinctSquares(
      squares: List[Square]
  ): List[Square] =
    squares.distinct.sortBy(_.key)

  private def distinctFiles(
      files: List[File]
  ): List[File] =
    files.distinct.sortBy(_.char.toString)
