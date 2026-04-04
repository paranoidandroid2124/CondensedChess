package lila.accountintel

import java.time.Instant

import play.api.Configuration

import lila.accountintel.AccountIntel.*
import lila.accountintel.service.{ AccountNotebookEngine, SelectiveEvalLookup, SelectiveEvalProbe, SelectiveEvalRefiner }

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
    def latestByOwnerScopeKey(ownerScopeKey: String) = fuccess(none)
    def latestSucceededByOwnerScopeKey(ownerScopeKey: String) = fuccess(none)
    def latestActiveByOwnerScopeKey(ownerScopeKey: String) = fuccess(none)
    def latestActiveBuildByDedupeKey(dedupeKey: String) = fuccess(none)
    def recentByOwnerScopeKey(ownerScopeKey: String, limit: Int) = fuccess(Nil)
    def recentSucceededByOwner(ownerId: String, limit: Int) = fuccess(Nil)
    def claimNextQueued(now: Instant) =
      if current.status == JobStatus.Queued && current.buildOwner then
        current = current.copy(status = JobStatus.Running, progressStage = "fetching_games", queueState = "running")
        fuccess(current.some)
      else fuccess(none)
    def claimQueuedById(id: String, now: Instant) =
      if current.id == id && current.status == JobStatus.Queued && current.buildOwner then
        current = current.copy(status = JobStatus.Running, progressStage = "fetching_games", queueState = "running")
        fuccess(current.some)
      else fuccess(none)
    def setProgress(
        id: String,
        progressStage: String,
        snapshotState: String,
        processedGames: Option[Int],
        totalGames: Option[Int],
        etaSec: Option[Int]
    ) =
      current = current.copy(
        progressStage = progressStage,
        snapshotState = snapshotState,
        processedGames = processedGames.getOrElse(current.processedGames),
        totalGames = totalGames.orElse(current.totalGames),
        etaSec = etaSec.orElse(current.etaSec)
      )
      funit
    def markSucceeded(
        id: String,
        surfaceId: String,
        sourceFingerprint: String,
        warnings: List[String],
        surfaceJson: String,
        sampledGameCount: Int,
        totalGames: Int,
        cacheHit: Boolean,
        refreshLockedUntil: Option[Instant]
    ) =
      markedSuccess = true
      current = current.copy(
        status = JobStatus.Succeeded,
        surfaceId = surfaceId.some,
        sourceFingerprint = sourceFingerprint.some,
        warnings = warnings,
        surfaceJson = surfaceJson.some,
        queueState = "ready",
        snapshotState = "ready",
        cacheHit = cacheHit,
        refreshLockedUntil = refreshLockedUntil,
        processedGames = sampledGameCount,
        totalGames = totalGames.some
      )
      funit
    def setPublication(id: String, studyId: String, chapterId: String, notebookUrl: String) = funit
    def markFailed(id: String, code: String, message: String) =
      markedFailed = true
      current = current.copy(status = JobStatus.Failed, errorCode = code.some, errorMessage = message.some)
      funit
    def requeueTimedOutRunning(timeout: scala.concurrent.duration.FiniteDuration, retryLimit: Int) =
      if AccountIntelJob.timedOutRunning(current, Instant.now(), timeout) then
        requeued = true
        current = current.copy(status = JobStatus.Queued, progressStage = "queued", queueState = "queued")
      funit
    def cleanupExpired(successTtl: scala.concurrent.duration.FiniteDuration, failedTtl: scala.concurrent.duration.FiniteDuration) = funit

  private class FakeSurfaceStore(initial: List[AccountIntelSurfaceSnapshot] = Nil) extends AccountIntelSurfaceStore:
    var inserted = List.empty[AccountIntelSurfaceSnapshot]
    private var snapshots = initial.map(snapshot => snapshot.id -> snapshot).toMap

    def byId(id: String) = fuccess(snapshots.get(id))
    def latestByDedupeKey(dedupeKey: String) =
      fuccess(snapshots.values.filter(_.dedupeKey == dedupeKey).toList.sortBy(_.updatedAt.toEpochMilli).lastOption)
    def latestFreshByDedupeKey(dedupeKey: String, freshSince: Instant) =
      fuccess(
        snapshots.values
          .filter(snapshot => snapshot.dedupeKey == dedupeKey && !snapshot.updatedAt.isBefore(freshSince))
          .toList
          .sortBy(_.updatedAt.toEpochMilli)
          .lastOption
      )
    def byDedupeKeyAndFingerprint(dedupeKey: String, sourceFingerprint: String) =
      fuccess(snapshots.values.find(snapshot => snapshot.dedupeKey == dedupeKey && snapshot.sourceFingerprint == sourceFingerprint))
    def insert(snapshot: AccountIntelSurfaceSnapshot) =
      inserted = inserted :+ snapshot
      snapshots = snapshots.updated(snapshot.id, snapshot)
      funit

  private val noEvalLookup = new SelectiveEvalLookup:
    def lookup(fen: String) = fuccess(none[SelectiveEvalProbe])

  private class FakeFetcher(games: List[ExternalGame]) extends AccountGameFetcher:
    def fetchRecentGames(provider: String, username: String) = fuccess(games)

  private class ExplodingFetcher extends AccountGameFetcher:
    def fetchRecentGames(provider: String, username: String) = fufail(new RuntimeException("boom"))

  test("worker processes queued job end-to-end and stores a shared surface snapshot"):
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
    val surfaces = new FakeSurfaceStore()
    val engine = new AccountNotebookEngine(fetcher, new SelectiveEvalRefiner(noEvalLookup, noEvalLookup))
    val worker = new AccountIntelWorker(Configuration.empty, repo, surfaces, fetcher, engine)

    worker.runNext().map: _ =>
      assert(repo.markedSuccess)
      assertEquals(surfaces.inserted.size, 1)
      assert(repo.current.surfaceId.nonEmpty)
      assert(repo.current.surfaceJson.exists(_.contains("chesstory.account.surface.v1")))
      assertEquals(repo.current.notebookUrl, None)

  test("worker cache-hits identical source fingerprints and skips a second surface insert"):
    val games = List(
      sampleGame("g1", "ych24", "opp1", "0-1"),
      sampleGame("g2", "ych24", "opp2", "1/2-1/2"),
      sampleGame("g3", "ych24", "opp3", "0-1"),
      sampleGame("g4", "ych24", "opp4", "1/2-1/2"),
      sampleGame("g5", "opp5", "ych24", "1-0"),
      sampleGame("g6", "opp6", "ych24", "1/2-1/2"),
      sampleGame("g7", "opp7", "ych24", "1-0"),
      sampleGame("g8", "opp8", "ych24", "1/2-1/2")
    )
    val fingerprint = AccountIntelSurfaceSnapshot.fingerprint(games)
    val existingSnapshot = AccountIntelSurfaceSnapshot(
      id = "surface-1",
      dedupeKey = AccountIntelJob.dedupeKey("chesscom", "ych24", ProductKind.MyAccountIntelligenceLite),
      provider = "chesscom",
      username = "ych24",
      kind = ProductKind.MyAccountIntelligenceLite,
      sourceFingerprint = fingerprint,
      surfaceJson = """{"headline":"cached"}""",
      dossierJson = """{"kind":"dossier"}""",
      representativePgn = "1. e4 e5",
      sampledGameCount = games.size,
      eligibleGameCount = games.size
    )
    val job = AccountIntelJob.newQueued("u1", "chesscom", "ych24", ProductKind.MyAccountIntelligenceLite)
    val repo = new FakeStore(job)
    val surfaces = new FakeSurfaceStore(List(existingSnapshot))
    val fetcher = new FakeFetcher(games)
    val engine = new AccountNotebookEngine(fetcher, new SelectiveEvalRefiner(noEvalLookup, noEvalLookup))
    val worker = new AccountIntelWorker(Configuration.empty, repo, surfaces, fetcher, engine)

    worker.runNext().map: _ =>
      assert(repo.markedSuccess)
      assertEquals(repo.current.cacheHit, true)
      assertEquals(surfaces.inserted, Nil)
      assertEquals(repo.current.surfaceId, Some(existingSnapshot.id))

  test("tick requeues timed-out running jobs before picking them up again"):
    val stale = AccountIntelJob
      .newQueued("u1", "chesscom", "ych24", ProductKind.OpponentPrep)
      .copy(
        status = JobStatus.Running,
        updatedAt = Instant.now().minusSeconds(3600)
      )
    val repo = new FakeStore(stale)
    val fetcher = new FakeFetcher(Nil)
    val surfaces = new FakeSurfaceStore()
    val engine = new AccountNotebookEngine(fetcher, new SelectiveEvalRefiner(noEvalLookup, noEvalLookup))
    val worker = new AccountIntelWorker(Configuration.empty, repo, surfaces, fetcher, engine)

    worker.tick().map: _ =>
      assert(repo.requeued)

  test("worker marks unexpected fetch exceptions as worker_exception"):
    val job = AccountIntelJob.newQueued("u1", "chesscom", "ych24", ProductKind.OpponentPrep)
    val repo = new FakeStore(job)
    val surfaces = new FakeSurfaceStore()
    val worker = new AccountIntelWorker(
      Configuration.empty,
      repo,
      surfaces,
      new ExplodingFetcher,
      new AccountNotebookEngine(new ExplodingFetcher, new SelectiveEvalRefiner(noEvalLookup, noEvalLookup))
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
    val surfaces = new FakeSurfaceStore()
    val engine = new AccountNotebookEngine(fetcher, new SelectiveEvalRefiner(noEvalLookup, noEvalLookup))
    val worker = new AccountIntelWorker(Configuration.empty, repo, surfaces, fetcher, engine)

    worker.runJob(job.id).map: outcome =>
      assertEquals(outcome, RunJobOutcome.Started)
      assert(repo.markedSuccess)
      assert(repo.current.surfaceId.nonEmpty)
