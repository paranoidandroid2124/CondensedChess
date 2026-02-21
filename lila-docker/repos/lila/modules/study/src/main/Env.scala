package lila.study

import com.softwaremill.macwire.*
import play.api.Configuration
import play.api.libs.ws.StandaloneWSClient
import scala.annotation.unused

import lila.core.config.*

@Module
final class Env(
    appConfig: Configuration,
    ws: StandaloneWSClient,
    lightUserApi: lila.core.user.LightUserApi,
    userApi: lila.core.user.UserApi,
    analyser: lila.tree.Analyser,
    analysisJson: lila.tree.AnalysisJson,
    annotator: lila.tree.Annotator,
    mongo: lila.db.Env,
    net: lila.core.config.NetConfig,
    cacheApi: lila.memo.CacheApi
)(using
    Executor,
    Scheduler,
    akka.stream.Materializer,
    lila.core.config.RateLimit
):

  // Local type aliases for removed socket types
  private case class SocketVersion(value: Int)

  private lazy val studyDb = mongo.asyncDb("study", appConfig.get[String]("study.mongodb.uri"))

  def version(@unused studyId: StudyId): Fu[Int] =
    fuccess(0) // Socket removed - simplified

  def isConnected(@unused studyId: StudyId, @unused userId: UserId): Fu[Boolean] =
    fuccess(false) // Socket removed - simplified

  val studyRepo = StudyRepo(studyDb(CollName("study")))

  val chapterRepo = ChapterRepo(studyDb(CollName("study_chapter_flat")))
  private val topicRepo = StudyTopicRepo(studyDb(CollName("study_topic")))
  private val userTopicRepo = StudyUserTopicRepo(studyDb(CollName("study_user_topic")))

  lazy val jsonView = wire[JsonView]

  private lazy val chapterMaker = wire[ChapterMaker]

  private lazy val explorerGame = wire[ExplorerGameApi]

  private lazy val studyMaker = wire[StudyMaker]

  private lazy val studyInvite = wire[StudyInvite]

  private lazy val serverEvalRequester = wire[ServerEval.Requester]

  private lazy val sequencer = wire[StudySequencer]

  lazy val serverEvalMerger = wire[ServerEval.Merger]

  lazy val topicApi = wire[StudyTopicApi]

  lazy val api: StudyApi = wire[StudyApi]

  lazy val pager = wire[StudyPager]

  lazy val preview = wire[ChapterPreviewApi]

  lazy val pgnDump = wire[PgnDump]

  lazy val gifExport = GifExport(ws, appConfig.get[String]("game.gifUrl"))

  def findConnectedUsersIn(@unused studyId: StudyId)(@unused filter: Iterable[UserId] => Fu[List[UserId]]): Fu[List[UserId]] =
    fuccess(Nil) // Socket removed - simplified

  lila.common.Cli.handle:
    case "study" :: "rank" :: "reset" :: Nil =>
      studyRepo.resetAllRanks.map: count =>
        s"$count done"

  lila.common.Bus.sub[lila.tree.StudyAnalysisProgress]:
    case lila.tree.StudyAnalysisProgress(analysis, complete) => serverEvalMerger(analysis, complete)
