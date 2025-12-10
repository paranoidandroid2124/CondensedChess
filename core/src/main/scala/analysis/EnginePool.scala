package chess
package analysis

import java.util.concurrent.{BlockingQueue, LinkedBlockingQueue, Semaphore}
import java.util.concurrent.atomic.AtomicBoolean
import scala.concurrent.{Future, ExecutionContext}

/**
 * Manages a fixed pool of StockfishClient instances.
 * Prevents system overload by limiting concurrent engine processes.
 */
object EnginePool:
  
  private val maxInstances = EnvLoader.get("MAX_ENGINES").flatMap(_.toIntOption).getOrElse(4) // Conservative default
  private val pool: BlockingQueue[StockfishClient] = new LinkedBlockingQueue[StockfishClient](maxInstances)
  private val availablePermits = new Semaphore(maxInstances, true) // Fair semaphore
  private val isShutdown = new AtomicBoolean(false)
  
  // Test hook
  var clientFactory: () => StockfishClient = () => new StockfishClient()

  // Lazy initialization of the pool? No, let's init on demand to avoid startup spike.
  // Actually, standard pool pattern: create when needed up to max, then reuse.
  // For simplicity MVP: Pre-fill or create-on-demand with counter. 
  // Let's use a simple BlockingQueue where we put creates instances.
  
  // Actually, better approach for Scala Future-based:
  // `withEngine[T](block: StockfishClient => Future[T]): Future[T]`
  
  /**
   * Executes a block using an engine from the pool.
   * If no engine is available, waits until one is released.
   */
  def withEngine[T](block: StockfishClient => Future[T])(using ec: ExecutionContext): Future[T] =
    if isShutdown.get() then return Future.failed(new IllegalStateException("EnginePool is shutdown"))

    // Acquire permit (blocking thread? No, we should avoid blocking threads in Future based code)
    // But Semaphore.acquire() blocks. 
    // In reactive streams we'd use a queued request.
    // For MVP with Future: 
    // We can use a recursive function that checks availability or returns a pending Future.
    // OR, just use Java's blocking semaphore if we wrap it in `Future { blocking { ... } }` 
    // but that wastes thread pool.
    
    // Better: Allow strictly blocking for now since our Parallelism is limited by this anyway.
    // If we have 4 engines, we can only do 4 things. 
    // blocking { availablePermits.acquire() } is acceptable if wrapped properly.
    
    Future {
      scala.concurrent.blocking {
        availablePermits.acquire()
      }
    }.flatMap { _ =>
      // We have a permit. Get an engine.
      // If pool is empty but we haven't created max yet? 
      // Simplest: Just use the Queue. Pre-fill it lazily?
      // Let's simplify: Just take from queue. If queue empty, it means all Max are busy (since permits = max).
      // Wait, we need to create them first.
      
      val engine = Option(pool.poll()).getOrElse {
        // Pool was empty, create new one? 
        // Logic: Access permit guarantees we are allowed to use one.
        // If pool is empty, it means we haven't created it yet OR it's being used?
        // Ah, if permit acquire matched creation, we wouldn't need to check.
        // Let's use a "managed" approach.
        clientFactory() 
      }
      
      block(engine).transform { result =>
        // Return engine to pool
        pool.offer(engine)
        availablePermits.release()
        result
      }
    }

  /**
   * Pre-warm the pool (Optional)
   */
  def prewarm(count: Int): Unit =
    // Current logic creates on demand.
    // If we want to recycle, we need to distinguish "new" from "reused".
    // The poll() -> new StockfishClient() above implies we always create new if empty?
    // No, `pool.poll()` returns null if empty. 
    // But if we have a permit, it means "One of the Max slots is yours".
    // If the queue is null, it means there is no *idle* engine.
    // But `availablePermits` tracks *total capacity*.
    // If permits=4, and we acquired 1. 3 left.
    // If pool is empty, it implies we haven't instantiated it yet.
    // CORRECT LOGIC:
    // val engine = Option(pool.poll()).getOrElse(new StockfishClient())
    // This works perfectly for lazy init.
    // When done, pool.offer(engine). Next time poll() gets it.
    ()

  def shutdown(): Unit =
    isShutdown.set(true)
    // Drain and close
    var engine = pool.poll()
    while engine != null do
      engine.close()
      engine = pool.poll()

