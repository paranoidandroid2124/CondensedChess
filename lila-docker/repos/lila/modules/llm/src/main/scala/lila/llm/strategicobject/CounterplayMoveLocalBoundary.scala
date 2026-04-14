package lila.llm.strategicobject

import chess.{ File, Square }

enum CounterplayMoveLocalBlocker:
  case ProvisionalScopeClosed
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
    exactRivalAdmitted && moveWitnessSatisfied

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
          current.relations.collect {
            case relation
                if MoveTransitionRelationOps.contains(relation.operator) &&
                  relatedObjects.contains(relation.target.objectId) =>
              val related = relatedObjects(relation.target.objectId)
              val rivalWitnesses =
                CounterplayAxisRivalRelationBoundary
                  .exactRivalRelationWitnessesTo(current, related)
                  .filter(_.operator == relation.operator)
              val touchedSquares =
                move.touchedSquares.intersect(rivalWitnesses.flatMap(_.sharedSquares)).distinct.sortBy(_.key)
              val touchedFiles =
                move.touchedFiles.intersect(rivalWitnesses.flatMap(_.sharedFiles)).distinct.sortBy(_.char.toString)

              Option.when(
                rivalWitnesses.nonEmpty &&
                  (touchedSquares.nonEmpty || touchedFiles.nonEmpty)
              )(
                CounterplayMoveLocalRelationMatch(
                  targetId = related.id,
                  operator = relation.operator,
                  touchedSquares = touchedSquares,
                  touchedFiles = touchedFiles
                ).normalized
              )
          }.flatten
            .distinct
            .sortBy(matchItem =>
              s"${matchItem.targetId}-${matchItem.operator.toString}-${matchItem.touchedSquares.map(_.key).mkString("-")}-${matchItem.touchedFiles.map(_.char).mkString}"
            )
        val relationTouch = relationMatches.nonEmpty
        val blockedByProvisionalScope =
          exactRivalAdmission.admitted &&
            moveTouchesCore &&
            relationTouch &&
            current.readiness == StrategicObjectReadiness.Provisional
        val blockedByCertification =
          exactRivalAdmission.admitted &&
            moveTouchesCore &&
            relationTouch &&
            current.readiness == StrategicObjectReadiness.Provisional

        val blocker =
          if !exactRivalAdmission.admitted then Some(CounterplayMoveLocalBlocker.MissingExactRivalRelation)
          else if !moveTouchesCore || !relationTouch then Some(CounterplayMoveLocalBlocker.MissingMoveEdgeTouch)
          else if blockedByProvisionalScope then
            Some(CounterplayMoveLocalBlocker.ProvisionalScopeClosed)
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
