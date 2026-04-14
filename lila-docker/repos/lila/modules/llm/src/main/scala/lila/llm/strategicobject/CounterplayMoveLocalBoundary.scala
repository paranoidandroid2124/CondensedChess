package lila.llm.strategicobject

import chess.{ File, Square }

enum CounterplayMoveLocalBlocker:
  case ProvisionalScopeClosed
  case CertificationClosed
  case MissingExactRivalRelation
  case MissingMoveEdgeTouch

final case class CounterplayMoveLocalRelationMatch(
    targetId: String,
    operator: StrategicRelationOperator,
    touchedSquares: List[Square],
    touchedFiles: List[File]
):
  def normalized: CounterplayMoveLocalRelationMatch =
    copy(
      touchedSquares = touchedSquares.distinct.sortBy(_.key),
      touchedFiles = touchedFiles.distinct.sortBy(_.char.toString)
    )

final case class CounterplayMoveLocalAssessment(
    exactRivalAdmission: CounterplayAxisRivalRelationBoundary.ExactRivalAdmission,
    relationMatches: List[CounterplayMoveLocalRelationMatch],
    moveTouchesCore: Boolean,
    blockedByProvisionalScope: Boolean,
    blockedByCertification: Boolean,
    blocker: Option[CounterplayMoveLocalBlocker]
):
  def exactRivalAdmitted: Boolean =
    exactRivalAdmission.admitted

  def relationTouch: Boolean =
    relationMatches.nonEmpty

  def matchedSquares: List[Square] =
    relationMatches.flatMap(_.touchedSquares).distinct.sortBy(_.key)

  def matchedFiles: List[File] =
    relationMatches.flatMap(_.touchedFiles).distinct.sortBy(_.char.toString)

  def relationWitnesses: Set[StrategicRelationOperator] =
    relationMatches.map(_.operator).toSet

  def moveWitnessSatisfied: Boolean =
    moveTouchesCore && relationTouch

  def narrowMoveLocalEligible: Boolean =
    exactRivalAdmitted &&
      moveWitnessSatisfied &&
      !blockedByProvisionalScope &&
      !blockedByCertification

object CounterplayMoveLocalBoundary:

  private val MoveTransitionRelationOps = Set(
    StrategicRelationOperator.Enables,
    StrategicRelationOperator.Preserves,
    StrategicRelationOperator.TransformsTo,
    StrategicRelationOperator.DependsOn,
    StrategicRelationOperator.Denies,
    StrategicRelationOperator.OverloadsOrUndermines
  )

  def assess(
      current: StrategicObject,
      move: StrategicPlayedMoveTrace,
      relatedObjects: Map[String, StrategicObject]
  ): Option[CounterplayMoveLocalAssessment] =
    current.profile match
      case _: StrategicObjectProfile.CounterplayAxis =>
        val exactRivalAdmission =
          CounterplayAxisRivalRelationBoundary.exactRivalAdmission(current, relatedObjects)
        val moveTouchesCore = touchesCounterplayCore(current, move)
        val relationMatches =
          exactRivalAdmission.witnesses
            .filter(witness =>
              MoveTransitionRelationOps.contains(witness.operator) &&
                relatedObjects.contains(witness.targetId)
            )
            .flatMap { witness =>
              val touchedSquares =
                move.touchedSquares.intersect(witness.sharedSquares).distinct.sortBy(_.key)
              val touchedFiles =
                move.touchedFiles.intersect(witness.sharedFiles).distinct.sortBy(_.char.toString)

              Option.when(
                touchedSquares.nonEmpty ||
                  admitsFileOnlyWitness(current, witness, touchedFiles)
              )(
                CounterplayMoveLocalRelationMatch(
                  targetId = witness.targetId,
                  operator = witness.operator,
                  touchedSquares = touchedSquares,
                  touchedFiles = touchedFiles
                ).normalized
              )
            }
            .groupBy(matchItem => (matchItem.targetId, matchItem.operator))
            .values
            .map(matches =>
              CounterplayMoveLocalRelationMatch(
                targetId = matches.head.targetId,
                operator = matches.head.operator,
                touchedSquares = matches.flatMap(_.touchedSquares),
                touchedFiles = matches.flatMap(_.touchedFiles)
              ).normalized
            )
            .toList
            .sortBy(matchItem =>
              s"${matchItem.targetId}-${matchItem.operator.toString}-${matchItem.touchedSquares.map(_.key).mkString("-")}-${matchItem.touchedFiles.map(_.char).mkString}"
            )
        val relationTouch = relationMatches.nonEmpty
        val moveWitnessSatisfied = moveTouchesCore && relationTouch
        val blockedByCertification =
          moveWitnessSatisfied &&
            !hasExactBoardCertificationSupport(current)
        val blockedByProvisionalScope =
          moveWitnessSatisfied &&
            current.readiness == StrategicObjectReadiness.Provisional &&
            !CounterplayMoveLocalSlice.exactPositiveDescriptorMatched(current, move, relationMatches)

        val blocker =
          if !exactRivalAdmission.admitted then Some(CounterplayMoveLocalBlocker.MissingExactRivalRelation)
          else if !moveWitnessSatisfied then Some(CounterplayMoveLocalBlocker.MissingMoveEdgeTouch)
          else if blockedByProvisionalScope then
            Some(CounterplayMoveLocalBlocker.ProvisionalScopeClosed)
          else if blockedByCertification then
            Some(CounterplayMoveLocalBlocker.CertificationClosed)
          else None

        Some(
          CounterplayMoveLocalAssessment(
            exactRivalAdmission = exactRivalAdmission,
            relationMatches = relationMatches,
            moveTouchesCore = moveTouchesCore,
            blockedByProvisionalScope = blockedByProvisionalScope,
            blockedByCertification = blockedByCertification,
            blocker = blocker
          )
        )
      case _ =>
        None

  private def touchesCounterplayCore(
      current: StrategicObject,
      move: StrategicPlayedMoveTrace
  ): Boolean =
    current.profile match
      case StrategicObjectProfile.CounterplayAxis(resourceSquares, breakSquares, pressureSquares, _) =>
        val coreSquares =
          (resourceSquares ++ breakSquares ++ pressureSquares).distinct.sortBy(_.key)
        val coreFiles =
          coreSquares.map(_.file).distinct.sortBy(_.char.toString)

        move.touchedSquares.exists(coreSquares.contains) ||
          move.touchedFiles.exists(coreFiles.contains)
      case _ =>
        false

  private def admitsFileOnlyWitness(
      current: StrategicObject,
      witness: CounterplayAxisRivalRelationBoundary.RivalRelationWitness,
      touchedFiles: List[File]
  ): Boolean =
    touchedFiles.nonEmpty &&
      witness.sharedSquares.isEmpty &&
      (current.profile match
        case StrategicObjectProfile.CounterplayAxis(_, _, _, typedAxes) =>
          typedAxes.contains(CounterplayAxisType.File)
        case _ =>
          false
      )

  private def hasExactBoardCertificationSupport(
      current: StrategicObject
  ): Boolean =
    current.anchors.nonEmpty &&
      current.supportingPrimitives.exists(ref =>
        ref.anchorSquares.nonEmpty || ref.contestedSquares.nonEmpty || ref.lane.nonEmpty
      )
