package lila.strategicPuzzle

import scala.util.Random

import lila.strategicPuzzle.StrategicPuzzle.*

final class PuzzleSelector(
    repo: PuzzleRepo
)(using Executor):

  def nextIdFor(progress: ProgressDoc, excludeId: Option[String]): Fu[Option[String]] =
    repo
      .listPublicIds()
      .map(ids => PuzzleSelector.chooseNextId(ids, progress.clearedPuzzleIds, excludeId, Random.nextInt))

  def nextFor(progress: ProgressDoc, excludeId: Option[String]): Fu[Option[StrategicPuzzleDoc]] =
    nextIdFor(progress, excludeId).flatMap(_.fold(fuccess(none[StrategicPuzzleDoc]))(repo.byId))

  def nextAnonymousId(excludeId: Option[String]): Fu[Option[String]] =
    repo.randomPublicId(excludeId.toSet)

  def nextAnonymous(excludeId: Option[String]): Fu[Option[StrategicPuzzleDoc]] =
    nextAnonymousId(excludeId).flatMap(_.fold(fuccess(none[StrategicPuzzleDoc]))(repo.byId))

object PuzzleSelector:

  def chooseNextId(
      publicIds: List[String],
      clearedIds: List[String],
      excludeId: Option[String],
      randomIndex: Int => Int
  ): Option[String] =
    val filteredPublic = publicIds.filterNot(id => excludeId.contains(id))
    val uncleared = filteredPublic.filterNot(clearedIds.toSet)
    if uncleared.nonEmpty then uncleared.lift(randomIndex(uncleared.size))
    else None
