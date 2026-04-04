package lila.llm.tools.strategicpuzzle

import java.nio.charset.StandardCharsets
import java.nio.file.{ Files, Path }

import scala.concurrent.Await
import scala.concurrent.duration.*

import play.api.libs.json.Json
import reactivemongo.api.AsyncDriver

import lila.common.Executor
import lila.core.config.CollName
import lila.strategicPuzzle.StrategicPuzzle.StrategicPuzzleDoc

object StrategicPuzzleStorageSupport:

  final case class MongoRuntime(
      driver: AsyncDriver,
      db: lila.db.Db
  ):
    def close()(using Executor): Unit = Await.result(driver.close(), 10.seconds)

  def openMongo(name: String, uri: String)(using Executor): MongoRuntime =
    val driver = new AsyncDriver()
    MongoRuntime(
      driver = driver,
      db = lila.db.Db(name = name, uri = uri, driver = driver)
    )

  def requireMongoUri(explicit: Option[String]): Either[String, String] =
    explicit
      .orElse(sys.env.get("MONGODB_URI"))
      .filter(_.nonEmpty)
      .toRight("missing MongoDB URI; pass --mongo-uri or set MONGODB_URI")

  def detectWorkspaceRoot(): Path =
    val cwd = Path.of(".").toAbsolutePath.normalize
    if Files.isDirectory(cwd.resolve("tools")) && Files.isDirectory(cwd.resolve("lila-docker")) then cwd
    else
      Option(cwd.getParent)
        .flatMap(parent => Option(parent.getParent))
        .flatMap(grandParent => Option(grandParent.getParent))
        .filter(root => Files.isDirectory(root.resolve("tools")) && Files.isDirectory(root.resolve("lila-docker")))
        .getOrElse(cwd)

  def loadRuntimeDocs(path: Path): List[StrategicPuzzleDoc] =
    Files
      .readAllLines(path, StandardCharsets.UTF_8)
      .toArray(new Array[String](0))
      .toList
      .map(_.trim)
      .filter(_.nonEmpty)
      .map(line => Json.parse(line).as[StrategicPuzzleDoc])

  val puzzleCollName = CollName("strategicPuzzle")
  val attemptCollName = CollName("strategicPuzzleAttempt")
  val progressCollName = CollName("strategicPuzzleProgress")
  val cacheCollName = CollName("cache")
