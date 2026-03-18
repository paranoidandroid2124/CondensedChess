package lila.strategicPuzzle

import scala.util.Random

import play.api.libs.json.*

import lila.db.JSON
import lila.db.dsl.*
import lila.strategicPuzzle.StrategicPuzzle.*

final class PuzzleRepo(
    coll: Coll
)(using Executor):

  private val publicSelector =
    $doc(
      "generationMeta.selectionStatus" -> PublicSelectionStatus,
      "runtimeShell" -> $doc("$exists" -> true)
    )

  def byId(id: String): Fu[Option[StrategicPuzzleDoc]] =
    coll
      .find(publicSelector ++ $doc("id" -> id))
      .one[Bdoc]
      .map(_.flatMap(readPuzzle))

  def randomPublic(excludeIds: Set[String] = Set.empty): Fu[Option[StrategicPuzzleDoc]] =
    listPublicIds().flatMap { ids =>
      val choices = ids.filterNot(excludeIds)
      choices.headOption.fold(fuccess(none[StrategicPuzzleDoc])) { _ =>
        byId(choices(Random.nextInt(choices.size)))
      }
    }

  def listPublicIds(): Fu[List[String]] =
    coll
      .find(publicSelector, some($doc("id" -> true, "_id" -> false)))
      .cursor[Bdoc]()
      .listAll()
      .map(_.flatMap(_.getAsOpt[String]("id")))

  private[strategicPuzzle] def readPuzzle(doc: Bdoc): Option[StrategicPuzzleDoc] =
    Json.fromJson[StrategicPuzzleDoc](JSON.jval(doc)).asOpt
