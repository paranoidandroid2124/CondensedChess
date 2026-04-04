package lila.llm.tools.strategicpuzzle

import java.nio.file.Path

import scala.concurrent.{ Await, ExecutionContext }
import scala.concurrent.duration.*

import play.api.libs.json.Json

import lila.common.Executor
import lila.db.JSON
import lila.db.dsl.*
import lila.strategicPuzzle.PuzzleRepo
import lila.strategicPuzzle.StrategicPuzzle.{ PublicSelectionStatus, StrategicPuzzleDoc }

object StrategicPuzzlePublishRunner:

  import StrategicPuzzleStorageSupport.*

  final case class Config(
      inputPath: Path,
      mongoUri: String
  )

  def main(args: Array[String]): Unit =
    parseArgs(args.toList) match
      case Left(err) =>
        System.err.println(s"[strategic-publish] $err")
        sys.exit(2)
      case Right(config) =>
        given Executor = ExecutionContext.global
        val mongo = openMongo("strategic-puzzle-publish", config.mongoUri)
        try
          run(config, mongo)
        finally mongo.close()

  private def run(config: Config, mongo: MongoRuntime)(using Executor): Unit =
    val docs = loadRuntimeDocs(config.inputPath)
    val publishable = docs.filter(isPublishable)
    val skipped = docs.size - publishable.size
    val puzzleColl = mongo.db(puzzleCollName)
    val cacheColl = mongo.db(cacheCollName)

    publishable.foreach { doc =>
      Await.result(
        puzzleColl.update
          .one(
            $doc("id" -> doc.id),
            JSON.bdoc(Json.toJsObject(doc)),
            upsert = true
          ),
        30.seconds
      )
    }

    if publishable.nonEmpty then
      Await.result(cacheColl.delete.one($id(PuzzleRepo.PublicIdsCacheKey)), 30.seconds)

    println(
      s"[strategic-publish] input=${config.inputPath} total=${docs.size} published=${publishable.size} skipped=$skipped cacheInvalidated=${publishable.nonEmpty}"
    )

  private def isPublishable(doc: StrategicPuzzleDoc): Boolean =
    doc.generationMeta.selectionStatus == PublicSelectionStatus && doc.runtimeShell.nonEmpty

  private def parseArgs(args: List[String]): Either[String, Config] =
    val workspaceRoot = detectWorkspaceRoot()
    val defaultInput = workspaceRoot.resolve(Path.of("tools", "strategic_puzzles", "runtime_prompt_sample10_20260319.jsonl"))

    @annotation.tailrec
    def loop(rest: List[String], inputPath: Path, mongoUri: Option[String]): Either[String, Config] =
      rest match
        case Nil =>
          requireMongoUri(mongoUri).map(uri => Config(inputPath = inputPath, mongoUri = uri))
        case head :: tail if head.startsWith("--input=") =>
          loop(tail, Path.of(head.stripPrefix("--input=")).toAbsolutePath.normalize, mongoUri)
        case "--input" :: value :: tail =>
          loop(tail, Path.of(value).toAbsolutePath.normalize, mongoUri)
        case head :: tail if head.startsWith("--mongo-uri=") =>
          loop(tail, inputPath, Some(head.stripPrefix("--mongo-uri=")))
        case "--mongo-uri" :: value :: tail =>
          loop(tail, inputPath, Some(value))
        case unknown :: _ =>
          Left(s"unknown argument: $unknown")

    loop(args, defaultInput, Option.empty[String])
