package lila.llm.tools.strategicpuzzle

import scala.concurrent.{ Await, ExecutionContext }
import scala.concurrent.duration.*

import lila.common.Executor
import lila.core.userId.UserId
import lila.strategicPuzzle.{ AttemptRepo, ProgressRepo }
import lila.strategicPuzzle.StrategicPuzzle.progressFromAttempts

object StrategicPuzzleProgressRepairRunner:

  import StrategicPuzzleStorageSupport.*

  final case class Config(
      mongoUri: String,
      userId: Option[UserId]
  )

  def main(args: Array[String]): Unit =
    parseArgs(args.toList) match
      case Left(err) =>
        System.err.println(s"[strategic-progress-repair] $err")
        sys.exit(2)
      case Right(config) =>
        given Executor = ExecutionContext.global
        val mongo = openMongo("strategic-puzzle-progress-repair", config.mongoUri)
        try
          run(config, mongo)
        finally mongo.close()

  private def run(config: Config, mongo: MongoRuntime)(using Executor): Unit =
    val attemptRepo = AttemptRepo(mongo.db(attemptCollName))
    val progressRepo = ProgressRepo(mongo.db(progressCollName))
    val userIds =
      config.userId match
        case Some(uid) => List(uid)
        case None      => Await.result(attemptRepo.distinctUserIds(), 30.seconds)

    var repaired = 0
    var attemptsSeen = 0
    userIds.foreach { userId =>
      val attempts = Await.result(attemptRepo.all(userId), 30.seconds)
      attemptsSeen += attempts.size
      val progress = progressFromAttempts(userId, attempts)
      Await.result(progressRepo.upsert(progress), 30.seconds)
      repaired += 1
    }

    println(
      s"[strategic-progress-repair] users=$repaired attempts=$attemptsSeen mode=${config.userId.fold("all-users")(_.value)}"
    )

  private def parseArgs(args: List[String]): Either[String, Config] =
    @annotation.tailrec
    def loop(rest: List[String], mongoUri: Option[String], userId: Option[UserId]): Either[String, Config] =
      rest match
        case Nil =>
          requireMongoUri(mongoUri).map(uri => Config(mongoUri = uri, userId = userId))
        case head :: tail if head.startsWith("--mongo-uri=") =>
          loop(tail, Some(head.stripPrefix("--mongo-uri=")), userId)
        case "--mongo-uri" :: value :: tail =>
          loop(tail, Some(value), userId)
        case head :: tail if head.startsWith("--user=") =>
          loop(tail, mongoUri, Some(UserId(head.stripPrefix("--user="))))
        case "--user" :: value :: tail =>
          loop(tail, mongoUri, Some(UserId(value)))
        case "--all-users" :: tail =>
          loop(tail, mongoUri, Option.empty[UserId])
        case unknown :: _ =>
          Left(s"unknown argument: $unknown")

    loop(args, Option.empty[String], Option.empty[UserId])
