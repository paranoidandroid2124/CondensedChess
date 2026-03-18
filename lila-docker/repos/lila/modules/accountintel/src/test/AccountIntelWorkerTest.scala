package lila.accountintel

import java.time.Instant

import play.api.Configuration

import lila.accountintel.AccountIntel.*
import lila.accountintel.service.{
  AccountNotebookEngine,
  NotebookPersisted,
  NotebookPersistFailure,
  NotebookSink,
  SelectiveEvalLookup,
  SelectiveEvalProbe,
  SelectiveEvalRefiner
}

class AccountIntelWorkerTest extends munit.FunSuite:

  given Executor = scala.concurrent.ExecutionContext.global

  private val qgdMoves =
    "1. d4 d5 2. c4 e6 3. Nc3 Nf6 4. Nf3 Be7 5. Bg5 O-O 6. e3 h6 7. Bh4 b6 8. cxd5 exd5 9. Bd3 c5 10. O-O Nc6 11. Rc1 Be6 12. Qa4"

  private def sampleGame(id: String, white: String, black: String, result: String) =
    ExternalGame(
      "chesscom",
      id,
      "2026-03-17 00:00",
      white,
      black,
      result,
      Some(s"https://example.com/$id"),
      s"""[Event "Fixture"]
         |[Site "https://example.com/$id"]
         |[Date "2026.03.17"]
         |[White "$white"]
         |[Black "$black"]
         |[Result "$result"]
         |[Variant "Standard"]
         |[Opening "Queen's Gambit Declined"]
         |
         |$qgdMoves $result
         |""".stripMargin
    )

  private class FakeStore(job: AccountIntelJob) extends AccountIntelJobStore:
    var current = job
    var markedSuccess = false
    var markedFailed = false
    var requeued = false

    def byId(id: String) = fuccess(Option(current).filter(_.id == id))
    def insert(job: AccountIntelJob) = funit
    def latestByDedupeKey(dedupeKey: String) = fuccess(none)
    def latestSucceededByDedupeKey(dedupeKey: String) = fuccess(none)
    def latestActiveByDedupeKey(dedupeKey: String) = fuccess(none)
    def recentByDedupeKey(dedupeKey: String, limit: Int) = fuccess(Nil)
    def recentSucceededByOwner(ownerId: String, limit: Int) = fuccess(Nil)
    def claimNextQueued(now: Instant) =
      if current.status == JobStatus.Queued then
        current = current.copy(status = JobStatus.Running, progressStage = "fetching_games")
        fuccess(current.some)
      else fuccess(none)
    def claimQueuedById(id: String, now: Instant) =
      if current.id == id && current.status == JobStatus.Queued then
        current = current.copy(status = JobStatus.Running, progressStage = "fetching_games")
        fuccess(current.some)
      else fuccess(none)
    def setProgress(id: String, progressStage: String) =
      current = current.copy(progressStage = progressStage)
      funit
    def markSucceeded(
        id: String,
        studyId: String,
        chapterId: String,
        notebookUrl: String,
        warnings: List[String],
        surfaceJson: String
    ) =
      markedSuccess = true
      current = current.copy(
        status = JobStatus.Succeeded,
        studyId = studyId.some,
        chapterId = chapterId.some,
        notebookUrl = notebookUrl.some,
        warnings = warnings,
        surfaceJson = surfaceJson.some
      )
      funit
    def markFailed(id: String, code: String, message: String) =
      markedFailed = true
      current = current.copy(status = JobStatus.Failed, errorCode = code.some, errorMessage = message.some)
      funit
    def requeueTimedOutRunning(timeout: scala.concurrent.duration.FiniteDuration, retryLimit: Int) =
      if AccountIntelJob.timedOutRunning(current, Instant.now(), timeout) then
        requeued = true
        current = current.copy(status = JobStatus.Queued, progressStage = "queued")
      funit
    def cleanupExpired(successTtl: scala.concurrent.duration.FiniteDuration, failedTtl: scala.concurrent.duration.FiniteDuration) = funit

  private val noEvalLookup = new SelectiveEvalLookup:
    def lookup(fen: String) = fuccess(none[SelectiveEvalProbe])

  private class FakeFetcher(games: List[ExternalGame]) extends AccountGameFetcher:
    def fetchRecentGames(provider: String, username: String) = fuccess(games)

  private class ExplodingFetcher extends AccountGameFetcher:
    def fetchRecentGames(provider: String, username: String) = fufail(new RuntimeException("boom"))

  private class FakeSink extends NotebookSink:
    var persisted = false
    def persist(job: AccountIntelJob, artifact: NotebookBuildArtifact) =
      persisted = true
      fuccess(Right(NotebookPersisted("study-1", "chapter-1", "/notebook/study-1/chapter-1")))

  private class FailingSink(code: String, message: String) extends NotebookSink:
    def persist(job: AccountIntelJob, artifact: NotebookBuildArtifact) =
      fuccess(Left(NotebookPersistFailure(code, message)))

  test("worker processes queued job end-to-end and stores notebook url"):
    val job = AccountIntelJob.newQueued("u1", "chesscom", "ych24", ProductKind.MyAccountIntelligenceLite)
    val repo = new FakeStore(job)
    val fetcher = new FakeFetcher(
      List(
        sampleGame("g1", "ych24", "opp1", "0-1"),
        sampleGame("g2", "ych24", "opp2", "1/2-1/2"),
        sampleGame("g3", "ych24", "opp3", "0-1"),
        sampleGame("g4", "ych24", "opp4", "1/2-1/2"),
        sampleGame("g5", "opp5", "ych24", "1-0"),
        sampleGame("g6", "opp6", "ych24", "1/2-1/2"),
        sampleGame("g7", "opp7", "ych24", "1-0"),
        sampleGame("g8", "opp8", "ych24", "1/2-1/2")
      )
    )
    val engine = new AccountNotebookEngine(fetcher, new SelectiveEvalRefiner(noEvalLookup, noEvalLookup))
    val sink = new FakeSink
    val worker = new AccountIntelWorker(Configuration.empty, repo, fetcher, engine, sink)

    worker.runNext().map: _ =>
      assert(repo.markedSuccess)
      assert(sink.persisted)
      assertEquals(repo.current.notebookUrl, Some("/notebook/study-1/chapter-1"))
      assert(repo.current.surfaceJson.exists(_.contains("chesstory.account.surface.v1")))

  test("tick requeues timed-out running jobs before picking them up again"):
    val stale = AccountIntelJob
      .newQueued("u1", "chesscom", "ych24", ProductKind.OpponentPrep)
      .copy(
        status = JobStatus.Running,
        updatedAt = Instant.now().minusSeconds(3600)
      )
    val repo = new FakeStore(stale)
    val fetcher = new FakeFetcher(Nil)
    val engine = new AccountNotebookEngine(fetcher, new SelectiveEvalRefiner(noEvalLookup, noEvalLookup))
    val sink = new FakeSink
    val worker = new AccountIntelWorker(Configuration.empty, repo, fetcher, engine, sink)

    worker.tick().map: _ =>
      assert(repo.requeued)

  test("worker marks the job failed when notebook persistence fails"):
    val job = AccountIntelJob.newQueued("u1", "chesscom", "ych24", ProductKind.MyAccountIntelligenceLite)
    val repo = new FakeStore(job)
    val fetcher = new FakeFetcher(
      List(
        sampleGame("g1", "ych24", "opp1", "0-1"),
        sampleGame("g2", "ych24", "opp2", "1/2-1/2"),
        sampleGame("g3", "ych24", "opp3", "0-1"),
        sampleGame("g4", "ych24", "opp4", "1/2-1/2"),
        sampleGame("g5", "opp5", "ych24", "1-0"),
        sampleGame("g6", "opp6", "ych24", "1/2-1/2"),
        sampleGame("g7", "opp7", "ych24", "1-0"),
        sampleGame("g8", "opp8", "ych24", "1/2-1/2")
      )
    )
    val engine = new AccountNotebookEngine(fetcher, new SelectiveEvalRefiner(noEvalLookup, noEvalLookup))
    val worker = new AccountIntelWorker(
      Configuration.empty,
      repo,
      fetcher,
      engine,
      new FailingSink("study_create_failed", "Notebook study creation failed.")
    )

    worker.runNext().map: _ =>
      assert(repo.markedFailed)
      assertEquals(repo.current.errorCode, Some("study_create_failed"))

  test("worker marks unexpected fetch exceptions as worker_exception"):
    val job = AccountIntelJob.newQueued("u1", "chesscom", "ych24", ProductKind.OpponentPrep)
    val repo = new FakeStore(job)
    val worker = new AccountIntelWorker(
      Configuration.empty,
      repo,
      new ExplodingFetcher,
      new AccountNotebookEngine(new ExplodingFetcher, new SelectiveEvalRefiner(noEvalLookup, noEvalLookup)),
      new FakeSink
    )

    worker.runNext().map: _ =>
      assert(repo.markedFailed)
      assertEquals(repo.current.errorCode, Some("worker_exception"))

  test("worker can process a specific queued job by id for external dispatch"):
    val job = AccountIntelJob.newQueued("u1", "chesscom", "ych24", ProductKind.MyAccountIntelligenceLite)
    val repo = new FakeStore(job)
    val fetcher = new FakeFetcher(
      List(
        sampleGame("g1", "ych24", "opp1", "0-1"),
        sampleGame("g2", "ych24", "opp2", "1/2-1/2"),
        sampleGame("g3", "ych24", "opp3", "0-1"),
        sampleGame("g4", "ych24", "opp4", "1/2-1/2"),
        sampleGame("g5", "opp5", "ych24", "1-0"),
        sampleGame("g6", "opp6", "ych24", "1/2-1/2"),
        sampleGame("g7", "opp7", "ych24", "1-0"),
        sampleGame("g8", "opp8", "ych24", "1/2-1/2")
      )
    )
    val engine = new AccountNotebookEngine(fetcher, new SelectiveEvalRefiner(noEvalLookup, noEvalLookup))
    val sink = new FakeSink
    val worker = new AccountIntelWorker(Configuration.empty, repo, fetcher, engine, sink)

    worker.runJob(job.id).map: outcome =>
      assertEquals(outcome, RunJobOutcome.Started)
      assert(repo.markedSuccess)
      assertEquals(repo.current.notebookUrl, Some("/notebook/study-1/chapter-1"))

  test("runJob reports missing and non-queued jobs honestly"):
    val job = AccountIntelJob
      .newQueued("u1", "chesscom", "ych24", ProductKind.OpponentPrep)
      .copy(status = JobStatus.Running)
    val repo = new FakeStore(job)
    val worker = new AccountIntelWorker(
      Configuration.empty,
      repo,
      new FakeFetcher(Nil),
      new AccountNotebookEngine(new FakeFetcher(Nil), new SelectiveEvalRefiner(noEvalLookup, noEvalLookup)),
      new FakeSink
    )

    worker.runJob("missing-id").flatMap: missing =>
      worker.runJob(job.id).map: notQueued =>
        assertEquals(missing, RunJobOutcome.Missing)
        assertEquals(notQueued, RunJobOutcome.NotQueued)
