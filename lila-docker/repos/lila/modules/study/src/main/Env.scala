package lila.study

import com.softwaremill.macwire.*
import play.api.Configuration
import play.api.libs.ws.StandaloneWSClient

import lila.core.config.*
import lila.core.socket.{ GetVersion, SocketVersion }

@Module
final class Env(
    appConfig: Configuration,
    ws: StandaloneWSClient,
    lightUserApi: lila.core.user.LightUserApi,
    gamePgnDump: lila.core.game.PgnDump,
    divider: lila.core.game.Divider,
    gameRepo: lila.core.game.GameRepo,
    namer: lila.core.game.Namer,
    userApi: lila.core.user.UserApi,
    explorer: lila.core.game.Explorer,
    prefApi: lila.core.pref.PrefApi,
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

  private lazy val studyDb = mongo.asyncDb("study", appConfig.get[String]("study.mongodb.uri"))

  def version(studyId: StudyId): Fu[SocketVersion] =
    fuccess(SocketVersion(0)) // Socket removed - simplified

  def isConnected(studyId: StudyId, userId: UserId): Fu[Boolean] =
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

  def findConnectedUsersIn(studyId: StudyId)(filter: Iterable[UserId] => Fu[List[UserId]]): Fu[List[UserId]] =
    fuccess(Nil) // Socket removed - simplified

  lila.common.Cli.handle:
    case "study" :: "rank" :: "reset" :: Nil =>
      studyRepo.resetAllRanks.map: count =>
        s"$count done"

  lila.common.Bus.sub[lila.tree.StudyAnalysisProgress]:
    case lila.tree.StudyAnalysisProgress(analysis, complete) => serverEvalMerger(analysis, complete)

  lila.common.Bus.sub[lila.core.user.UserDelete]: del =>
    for
      studyIds <- studyRepo.deletePrivateByOwner(del.id)
      _ <- chapterRepo.deleteByStudyIds(studyIds)
      _ <- studyRepo.anonymizeAllOf(del.id)
      _ <- topicApi.userTopicsDelete(del.id)
    yield ()
