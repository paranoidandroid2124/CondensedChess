package lila.strategicPuzzle

import scala.util.Random

import play.api.libs.json.*
import reactivemongo.api.indexes.{ Index, IndexType }

import lila.db.JSON
import lila.db.dsl.*
import lila.strategicPuzzle.StrategicPuzzle.*

final class PuzzleRepo(
    coll: Coll,
    mongoCache: lila.memo.MongoCache.Api
)(using Executor):

  import PuzzleRepo.*

  private val publicSelector =
    $doc(
      "generationMeta.selectionStatus" -> PublicSelectionStatus,
      "runtimeShell" -> $doc("$exists" -> true)
    )

  ensureIndexes()

  private val publicIdsCache =
    mongoCache.unitNoHeap[List[String]](PublicIdsCacheName, 10.minutes): _ =>
      fetchPublicIds()

  def byId(id: String): Fu[Option[StrategicPuzzleDoc]] =
    coll
      .find(publicSelector ++ $doc("id" -> id))
      .one[Bdoc]
      .map(_.flatMap(readPuzzle))

  def randomPublicId(excludeIds: Set[String] = Set.empty): Fu[Option[String]] =
    listPublicIds().map { ids =>
      val choices = ids.filterNot(excludeIds)
      choices.headOption.map(_ => choices(Random.nextInt(choices.size)))
    }

  def randomPublic(excludeIds: Set[String] = Set.empty): Fu[Option[StrategicPuzzleDoc]] =
    randomPublicId(excludeIds).flatMap(_.fold(fuccess(none[StrategicPuzzleDoc]))(byId))

  def listPublicIds(): Fu[List[String]] =
    publicIdsCache.get(())

  def invalidatePublicIds(): Funit =
    publicIdsCache.invalidate(())

  private[strategicPuzzle] def fetchPublicIds(): Fu[List[String]] =
    coll
      .find(publicSelector, some($doc("id" -> true, "_id" -> false)))
      .cursor[Bdoc]()
      .listAll()
      .map(_.flatMap(_.getAsOpt[String]("id")))

  private[strategicPuzzle] def readPuzzle(doc: Bdoc): Option[StrategicPuzzleDoc] =
    Json.fromJson[StrategicPuzzleDoc](JSON.jval(doc)).asOpt

  private def ensureIndexes(): Unit =
    coll.indexesManager.ensure(
      Index(
        key = Seq("generationMeta.selectionStatus" -> IndexType.Ascending, "id" -> IndexType.Ascending),
        name = Some("public_selection_id"),
        options = $doc(
          "partialFilterExpression" -> $doc(
            "runtimeShell" -> $doc("$exists" -> true)
          )
        )
      )
    )

object PuzzleRepo:

  val PublicIdsCacheName = "strategicPuzzle.publicIds"
  val PublicIdsCacheKey = s"$PublicIdsCacheName:"
