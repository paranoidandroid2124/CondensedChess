package chess
package analysis

import java.util.concurrent.{Executors, ScheduledExecutorService}
import scala.concurrent.{Future, Promise, ExecutionContext}
import scala.concurrent.duration.FiniteDuration

object AsyncUtils:

  private val scheduler: ScheduledExecutorService = Executors.newSingleThreadScheduledExecutor(
    (r: Runnable) => {
      val t = new Thread(r, "analysis-scheduler")
      t.setDaemon(true)
      t
    }
  )

  /**
   * Returns a Future that completes successfully after the specified duration.
   */
  def delay(duration: FiniteDuration): Future[Unit] =
    val p = Promise[Unit]()
    scheduler.schedule(
      () => p.success(()),
      duration.length,
      duration.unit
    )
    p.future

  /**
   * Returns a Future that completes with the result of the given future,
   * or fails with a TimeoutException if it takes too long.
   */
  def timeout[T](f: Future[T], duration: FiniteDuration)(using ec: ExecutionContext): Future[T] =
    val p = Promise[T]()
    
    // Schedule timeout failure
    val scheduled = scheduler.schedule(
      () => p.tryFailure(new java.util.concurrent.TimeoutException(s"Timed out after $duration")),
      duration.length,
      duration.unit
    )
    
    // Link futures
    f.onComplete { result =>
      // Cancel the timeout task if completed in time
      scheduled.cancel(false)
      p.tryComplete(result)
    }
    
    p.future
