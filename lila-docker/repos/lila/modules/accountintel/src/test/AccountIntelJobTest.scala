package lila.accountintel

import java.time.Instant
import scala.concurrent.duration.*

import lila.accountintel.AccountIntel.*

class AccountIntelJobTest extends munit.FunSuite:

  test("reuse succeeds for fresh successful jobs and stale failed jobs become retryable"):
    val now = Instant.parse("2026-03-17T03:00:00Z")
    val success = AccountIntelJob
      .newQueued("u1", "chesscom", "ych24", ProductKind.MyAccountIntelligenceLite)
      .copy(
        status = JobStatus.Succeeded,
        updatedAt = now.minusSeconds(60)
      )
    val failed = AccountIntelJob
      .newQueued("u1", "chesscom", "ych24", ProductKind.MyAccountIntelligenceLite)
      .copy(
        status = JobStatus.Failed,
        attemptCount = 1,
        updatedAt = now.minusSeconds(3600)
      )

    assert(AccountIntelJob.canReuse(success, now, 24.hours, 15.minutes))
    assert(!AccountIntelJob.canReuse(failed, now, 24.hours, 15.minutes))
    assert(AccountIntelJob.retryableFailed(failed, now, 15.minutes, retryLimit = 3))

  test("running and queued jobs always dedupe"):
    val now = Instant.parse("2026-03-17T03:00:00Z")
    val running = AccountIntelJob
      .newQueued("u1", "lichess", "ych24", ProductKind.OpponentPrep)
      .copy(
        status = JobStatus.Running,
        updatedAt = now.minusSeconds(10)
      )
    assert(AccountIntelJob.canReuse(running, now, 24.hours, 15.minutes))

  test("timed out running jobs are identifiable for recovery"):
    val now = Instant.parse("2026-03-17T03:00:00Z")
    val running = AccountIntelJob
      .newQueued("u1", "lichess", "ych24", ProductKind.OpponentPrep)
      .copy(
        status = JobStatus.Running,
        updatedAt = now.minusSeconds(3600)
      )
    assert(AccountIntelJob.timedOutRunning(running, now, 8.minutes))

  test("status responses use run-specific result urls by default"):
    val job = AccountIntelJob.newQueued("u1", "lichess", "ych24", ProductKind.OpponentPrep)
    assertEquals(
      job.toStatusResponse().url,
      Some(s"/account-intel/lichess/ych24?kind=${ProductKind.OpponentPrep.key}&jobId=${job.id}")
    )
