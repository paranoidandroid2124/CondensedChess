package lila.accountintel

import java.time.Instant

import play.api.Configuration

import lila.accountintel.AccountIntel.*

class AccountIntelApiTest extends munit.FunSuite:

  given Executor = scala.concurrent.ExecutionContext.global

  private class FakeStore(initial: List[AccountIntelJob] = Nil) extends AccountIntelJobStore:
    private var jobs = initial.map(job => job.id -> job).toMap
    var inserted = List.empty[AccountIntelJob]
    var markedFailed = List.empty[(String, String, String)]

    def byId(id: String) = fuccess(jobs.get(id))
    def insert(job: AccountIntelJob) =
      val duplicateActiveBuild =
        job.buildOwner && jobs.values.exists(existing =>
          existing.buildOwner && existing.dedupeKey == job.dedupeKey && Set(JobStatus.Queued, JobStatus.Running).contains(existing.status)
        )
      if duplicateActiveBuild then fufail(new RuntimeException("E11000 duplicate key error collection: accountIntel active_build_unique"))
      else
        inserted = inserted :+ job
        jobs = jobs.updated(job.id, job)
        funit
    def latestByOwnerScopeKey(ownerScopeKey: String) =
      fuccess(
        jobs.values
          .filter(_.ownerScopeKey == ownerScopeKey)
          .toList
          .sortBy(_.updatedAt.toEpochMilli)
          .lastOption
      )
    def latestSucceededByOwnerScopeKey(ownerScopeKey: String) =
      fuccess(
        jobs.values
          .filter(job => job.ownerScopeKey == ownerScopeKey && job.status == JobStatus.Succeeded)
          .toList
          .sortBy(_.updatedAt.toEpochMilli)
          .lastOption
      )
    def latestActiveByOwnerScopeKey(ownerScopeKey: String) =
      fuccess(
        jobs.values
          .filter(job => job.ownerScopeKey == ownerScopeKey && Set(JobStatus.Queued, JobStatus.Running).contains(job.status))
          .toList
          .sortBy(_.updatedAt.toEpochMilli)
          .lastOption
      )
    def latestActiveBuildByDedupeKey(dedupeKey: String) =
      fuccess(
        jobs.values
          .filter(job => job.buildOwner && job.dedupeKey == dedupeKey && Set(JobStatus.Queued, JobStatus.Running).contains(job.status))
          .toList
          .sortBy(_.updatedAt.toEpochMilli)
          .lastOption
      )
    def recentByOwnerScopeKey(ownerScopeKey: String, limit: Int) =
      fuccess(
        jobs.values
          .filter(_.ownerScopeKey == ownerScopeKey)
          .toList
          .sortBy(_.updatedAt.toEpochMilli)(Ordering.Long.reverse)
          .take(limit)
      )
    def recentSucceededByOwner(ownerId: String, limit: Int) =
      fuccess(
        jobs.values
          .filter(job => job.ownerId == ownerId && job.status == JobStatus.Succeeded)
          .toList
          .sortBy(_.updatedAt.toEpochMilli)(Ordering.Long.reverse)
          .take(limit)
      )
    def claimNextQueued(now: Instant) = fuccess(none)
    def claimQueuedById(id: String, now: Instant) = fuccess(none)
    def setProgress(
        id: String,
        progressStage: String,
        snapshotState: String,
        processedGames: Option[Int],
        totalGames: Option[Int],
        etaSec: Option[Int]
    ) = funit
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
    ) = funit
    def setPublication(id: String, studyId: String, chapterId: String, notebookUrl: String) = funit
    def markFailed(id: String, code: String, message: String) =
      markedFailed = markedFailed :+ (id, code, message)
      jobs.get(id).foreach: job =>
        jobs = jobs.updated(id, job.copy(status = JobStatus.Failed, errorCode = code.some, errorMessage = message.some))
      funit
    def requeueTimedOutRunning(timeout: scala.concurrent.duration.FiniteDuration, retryLimit: Int) = funit
    def cleanupExpired(successTtl: scala.concurrent.duration.FiniteDuration, failedTtl: scala.concurrent.duration.FiniteDuration) = funit

  private class FakeSurfaceStore(initial: List[AccountIntelSurfaceSnapshot] = Nil) extends AccountIntelSurfaceStore:
    private var snapshots = initial.map(snapshot => snapshot.id -> snapshot).toMap

    def byId(id: String) = fuccess(snapshots.get(id))
    def latestByDedupeKey(dedupeKey: String) =
      fuccess(
        snapshots.values.filter(_.dedupeKey == dedupeKey).toList.sortBy(_.updatedAt.toEpochMilli).lastOption
      )
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
      snapshots = snapshots.updated(snapshot.id, snapshot)
      funit

  private class FakeDispatcher(result: DispatchOutcome = DispatchOutcome.Accepted) extends AccountIntelJobDispatcher:
    var dispatched = List.empty[AccountIntelJob]
    def dispatch(job: AccountIntelJob) =
      dispatched = dispatched :+ job
      fuccess(result)

  private def sampleSurface(
      provider: String = "chesscom",
      username: String = "ych24",
      kind: ProductKind = ProductKind.MyAccountIntelligenceLite,
      updatedAt: Instant = Instant.now()
  ) =
    AccountIntelSurfaceSnapshot(
      id = "surface-1",
      dedupeKey = AccountIntelJob.dedupeKey(provider, username, kind),
      provider = provider,
      username = username,
      kind = kind,
      sourceFingerprint = "fp-1",
      surfaceJson = """{"headline":"cached","source":{"sampledGameCount":12}}""",
      dossierJson = """{"kind":"dossier"}""",
      representativePgn = "1. e4 e5 2. Nf3 Nc6",
      sampledGameCount = 12,
      eligibleGameCount = 12,
      warnings = List("cached"),
      createdAt = updatedAt,
      updatedAt = updatedAt
    )

  test("submit validates provider and username before queueing"):
    val repo = new FakeStore()
    val surfaces = new FakeSurfaceStore()
    val dispatcher = new FakeDispatcher
    val api = new AccountIntelApi(Configuration.empty, repo, surfaces, dispatcher)

    api.submit(UserId("u1"), "bad-provider", "??", ProductKind.OpponentPrep).map: result =>
      assert(result.isLeft)
      assertEquals(repo.inserted, Nil)
      assertEquals(dispatcher.dispatched, Nil)

  test("submit reuses owner active jobs before creating duplicates"):
    val existing = AccountIntelJob.newQueued("u1", "chesscom", "ych24", ProductKind.MyAccountIntelligenceLite)
    val repo = new FakeStore(List(existing))
    val surfaces = new FakeSurfaceStore()
    val dispatcher = new FakeDispatcher
    val api = new AccountIntelApi(Configuration.empty, repo, surfaces, dispatcher)

    api.submit(UserId("u1"), "chesscom", "ych24", ProductKind.MyAccountIntelligenceLite).map: result =>
      assertEquals(result, Right(existing))
      assertEquals(repo.inserted, Nil)
      assertEquals(dispatcher.dispatched, Nil)

  test("submit attaches a cached surface for a different owner without dispatching"):
    val repo = new FakeStore()
    val surfaces = new FakeSurfaceStore(List(sampleSurface(updatedAt = Instant.now().minusSeconds(60))))
    val dispatcher = new FakeDispatcher
    val api = new AccountIntelApi(Configuration.empty, repo, surfaces, dispatcher)

    api.submit(UserId("u2"), "chesscom", "ych24", ProductKind.MyAccountIntelligenceLite).map: result =>
      val job = result.toOption.get
      assertEquals(job.status, JobStatus.Succeeded)
      assertEquals(job.cacheHit, true)
      assertEquals(job.buildOwner, false)
      assertEquals(repo.inserted.size, 1)
      assertEquals(dispatcher.dispatched, Nil)

  test("submit attaches a watcher run to an active shared build from another owner"):
    val active = AccountIntelJob
      .newQueued("u1", "chesscom", "ych24", ProductKind.MyAccountIntelligenceLite)
      .copy(status = JobStatus.Running, queueState = "running", snapshotState = "fetching_games")
    val repo = new FakeStore(List(active))
    val surfaces = new FakeSurfaceStore()
    val dispatcher = new FakeDispatcher
    val api = new AccountIntelApi(Configuration.empty, repo, surfaces, dispatcher)

    api.submit(UserId("u2"), "chesscom", "ych24", ProductKind.MyAccountIntelligenceLite).map: result =>
      val watcher = result.toOption.get
      assertEquals(watcher.buildOwner, false)
      assertEquals(watcher.buildSourceId, Some(active.id))
      assertEquals(watcher.status, JobStatus.Running)
      assertEquals(dispatcher.dispatched, Nil)

  test("force submit respects shared refresh cooldown"):
    val repo = new FakeStore()
    val surfaces = new FakeSurfaceStore(List(sampleSurface(updatedAt = Instant.now().minusSeconds(120))))
    val api = new AccountIntelApi(Configuration.empty, repo, surfaces, new FakeDispatcher)

    api.submit(UserId("u2"), "chesscom", "ych24", ProductKind.MyAccountIntelligenceLite, force = true).map: result =>
      assert(result.left.exists(_.startsWith("Refresh is locked until")))

  test("submit dispatches a newly queued build when no shared surface exists"):
    val repo = new FakeStore()
    val surfaces = new FakeSurfaceStore()
    val dispatcher = new FakeDispatcher
    val api = new AccountIntelApi(Configuration.empty, repo, surfaces, dispatcher)

    api.submit(UserId("u1"), "chesscom", "ych24", ProductKind.MyAccountIntelligenceLite).map: result =>
      val job = result.toOption.get
      assertEquals(job.buildOwner, true)
      assertEquals(repo.inserted.size, 1)
      assertEquals(dispatcher.dispatched.map(_.id), List(job.id))

  test("submit fails and marks the build failed when dispatch is unavailable on a workerless node"):
    val repo = new FakeStore()
    val surfaces = new FakeSurfaceStore()
    val dispatcher = new FakeDispatcher(DispatchOutcome.Failed("worker offline"))
    val config = Configuration.from(Map("accountIntel.worker.enabled" -> false))
    val api = new AccountIntelApi(config, repo, surfaces, dispatcher)

    api.submit(UserId("u1"), "chesscom", "ych24", ProductKind.OpponentPrep).map: result =>
      assertEquals(result, Left("Account notebook worker is unavailable. Please try again later."))
      assertEquals(repo.inserted.size, 1)
      assertEquals(repo.markedFailed.headOption.map(_._2), Some("dispatch_failed"))
