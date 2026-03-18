package lila.strategicPuzzle

import scala.util.Random

import lila.core.userId.UserId
import lila.strategicPuzzle.StrategicPuzzle.*

final class PuzzleSelector(
    repo: PuzzleRepo,
    progressRepo: ProgressRepo
)(using Executor):

  def nextFor(userId: UserId, excludeId: Option[String]): Fu[Option[StrategicPuzzleDoc]] =
    for
      publicIds <- repo.listPublicIds()
      clearedIds <- progressRepo.clearedPuzzleIds(userId)
      chosenId <- chooseNextId(publicIds, clearedIds, excludeId)
      puzzle <- chosenId.fold(fuccess(none[StrategicPuzzleDoc]))(repo.byId)
    yield puzzle

  def nextAnonymous(excludeId: Option[String]): Fu[Option[StrategicPuzzleDoc]] =
    repo.randomPublic(excludeId.toSet)

  private def chooseNextId(
      publicIds: List[String],
      clearedIds: List[String],
      excludeId: Option[String]
  ): Fu[Option[String]] =
    fuccess(PuzzleSelector.chooseNextId(publicIds, clearedIds, excludeId, Random.nextInt))

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
