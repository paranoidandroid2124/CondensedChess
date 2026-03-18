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
      val duplicateActive =
        jobs.values.exists(existing =>
          existing.dedupeKey == job.dedupeKey && Set(JobStatus.Queued, JobStatus.Running).contains(existing.status)
        )
      if duplicateActive then fufail(new RuntimeException("E11000 duplicate key error collection: accountIntel active_dedupe_unique"))
      else
        inserted = inserted :+ job
        jobs = jobs.updated(job.id, job)
        funit
    def latestByDedupeKey(dedupeKey: String) =
      fuccess(
        jobs.values
          .filter(_.dedupeKey == dedupeKey)
          .toList
          .sortBy(_.updatedAt.toEpochMilli)
          .lastOption
      )
    def latestSucceededByDedupeKey(dedupeKey: String) =
      fuccess(
        jobs.values
          .filter(job => job.dedupeKey == dedupeKey && job.status == JobStatus.Succeeded)
          .toList
          .sortBy(_.updatedAt.toEpochMilli)
          .lastOption
      )
    def latestActiveByDedupeKey(dedupeKey: String) =
      fuccess(
        jobs.values
          .filter(job =>
            job.dedupeKey == dedupeKey && Set(JobStatus.Queued, JobStatus.Running).contains(job.status)
          )
          .toList
          .sortBy(_.updatedAt.toEpochMilli)
          .lastOption
      )
    def recentByDedupeKey(dedupeKey: String, limit: Int) =
      fuccess(
        jobs.values
          .filter(_.dedupeKey == dedupeKey)
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
    def setProgress(id: String, progressStage: String) = funit
    def markSucceeded(
        id: String,
        studyId: String,
        chapterId: String,
        notebookUrl: String,
        warnings: List[String],
        surfaceJson: String
    ) = funit
    def markFailed(id: String, code: String, message: String) =
      markedFailed = markedFailed :+ (id, code, message)
      jobs.get(id).foreach: job =>
        jobs = jobs.updated(id, job.copy(status = JobStatus.Failed, errorCode = code.some, errorMessage = message.some))
      funit
    def requeueTimedOutRunning(timeout: scala.concurrent.duration.FiniteDuration, retryLimit: Int) = funit
    def cleanupExpired(successTtl: scala.concurrent.duration.FiniteDuration, failedTtl: scala.concurrent.duration.FiniteDuration) = funit

  private class FakeDispatcher(result: DispatchOutcome = DispatchOutcome.Accepted) extends AccountIntelJobDispatcher:
    var dispatched = List.empty[AccountIntelJob]
    def dispatch(job: AccountIntelJob) =
      dispatched = dispatched :+ job
      fuccess(result)

  test("submit reuses fresh successful jobs instead of inserting duplicates"):
    val existing = AccountIntelJob
      .newQueued("u1", "chesscom", "ych24", ProductKind.MyAccountIntelligenceLite)
      .copy(
        status = JobStatus.Succeeded,
        updatedAt = Instant.now().minusSeconds(10)
      )
    val repo = new FakeStore(List(existing))
    val dispatcher = new FakeDispatcher
    val api = new AccountIntelApi(Configuration.empty, repo, dispatcher)

    api.submit(UserId("u1"), "chesscom", "ych24", ProductKind.MyAccountIntelligenceLite).map: result =>
      assertEquals(result, Right(existing))
      assertEquals(repo.inserted, Nil)
      assertEquals(dispatcher.dispatched, Nil)

  test("submit validates provider and username before queueing"):
    val repo = new FakeStore()
    val dispatcher = new FakeDispatcher
    val api = new AccountIntelApi(Configuration.empty, repo, dispatcher)

    api.submit(UserId("u1"), "bad-provider", "??", ProductKind.OpponentPrep).map: result =>
      assert(result.isLeft)
      assertEquals(repo.inserted, Nil)
      assertEquals(dispatcher.dispatched, Nil)

  test("submit requeues stale failed jobs after cooldown instead of reusing them"):
    val existing = AccountIntelJob
      .newQueued("u1", "chesscom", "ych24", ProductKind.OpponentPrep)
      .copy(
        status = JobStatus.Failed,
        attemptCount = 1,
        updatedAt = Instant.now().minusSeconds(3600)
      )
    val repo = new FakeStore(List(existing))
    val config = Configuration.from(Map("accountIntel.job.failedCooldown" -> "5 minutes"))
    val dispatcher = new FakeDispatcher
    val api = new AccountIntelApi(config, repo, dispatcher)

    api.submit(UserId("u1"), "chesscom", "ych24", ProductKind.OpponentPrep).map: result =>
      assert(result.isRight)
      assertEquals(repo.inserted.size, 1)
      assertNotEquals(repo.inserted.head.id, existing.id)
      assertEquals(dispatcher.dispatched.size, 1)

  test("status only returns jobs owned by the caller"):
    val existing = AccountIntelJob.newQueued("u2", "chesscom", "ych24", ProductKind.MyAccountIntelligenceLite)
    val repo = new FakeStore(List(existing))
    val api = new AccountIntelApi(Configuration.empty, repo, new FakeDispatcher)

    api.status(UserId("u1"), existing.id).map: result =>
      assertEquals(result, None)

  test("submit dispatches newly queued jobs for external worker pickup"):
    val repo = new FakeStore()
    val dispatcher = new FakeDispatcher
    val api = new AccountIntelApi(Configuration.empty, repo, dispatcher)

    api.submit(UserId("u1"), "chesscom", "ych24", ProductKind.MyAccountIntelligenceLite).map: result =>
      assert(result.isRight)
      assertEquals(repo.inserted.size, 1)
      assertEquals(dispatcher.dispatched.map(_.id), repo.inserted.map(_.id))

  test("submit reuses the already-active job when an active dedupe collision occurs at insert time"):
    val active = AccountIntelJob
      .newQueued("u1", "chesscom", "ych24", ProductKind.MyAccountIntelligenceLite)
      .copy(status = JobStatus.Queued)
    val repo = new FakeStore(List(active))
    val dispatcher = new FakeDispatcher
    val api = new AccountIntelApi(Configuration.empty, repo, dispatcher)

    api.submit(UserId("u1"), "chesscom", "ych24", ProductKind.MyAccountIntelligenceLite).map: result =>
      assertEquals(result, Right(active))
      assertEquals(repo.inserted, Nil)

  test("submit fails and marks the job failed when dispatch is unavailable on a workerless node"):
    val repo = new FakeStore()
    val dispatcher = new FakeDispatcher(DispatchOutcome.Failed("worker offline"))
    val config = Configuration.from(Map("accountIntel.worker.enabled" -> false))
    val api = new AccountIntelApi(config, repo, dispatcher)

    api.submit(UserId("u1"), "chesscom", "ych24", ProductKind.OpponentPrep).map: result =>
      assertEquals(result, Left("Account notebook worker is unavailable. Please try again later."))
      assertEquals(repo.inserted.size, 1)
      assertEquals(repo.markedFailed.headOption.map(_._2), Some("dispatch_failed"))

  test("selectedJob resolves an older job by id outside the truncated history window"):
    val older = AccountIntelJob
      .newQueued("u1", "chesscom", "ych24", ProductKind.MyAccountIntelligenceLite)
      .copy(status = JobStatus.Succeeded)
    val newer = (1 to 12).toList.map { i =>
      AccountIntelJob
        .newQueued("u1", "chesscom", "ych24", ProductKind.MyAccountIntelligenceLite)
        .copy(status = JobStatus.Succeeded, updatedAt = Instant.now().plusSeconds(i.toLong))
    }
    val repo = new FakeStore(older :: newer)
    val api = new AccountIntelApi(Configuration.empty, repo, new FakeDispatcher)

    api.selectedJob(UserId("u1"), "chesscom", "ych24", ProductKind.MyAccountIntelligenceLite, older.id).map: result =>
      assertEquals(result, Right(Some(older)))

  test("submit with force bypasses dedupe reuse"):
    val existing = AccountIntelJob
      .newQueued("u1", "chesscom", "ych24", ProductKind.MyAccountIntelligenceLite)
      .copy(
        status = JobStatus.Succeeded,
        updatedAt = Instant.now().minusSeconds(10)
      )
    val repo = new FakeStore(List(existing))
    val dispatcher = new FakeDispatcher
    val api = new AccountIntelApi(Configuration.empty, repo, dispatcher)

    api.submit(UserId("u1"), "chesscom", "ych24", ProductKind.MyAccountIntelligenceLite, force = true).map: result =>
      assert(result.isRight)
      assertEquals(repo.inserted.size, 1)
      assertNotEquals(repo.inserted.head.id, existing.id)
