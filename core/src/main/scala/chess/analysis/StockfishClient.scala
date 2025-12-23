package chess
package analysis

import java.io.{ BufferedReader, BufferedWriter, InputStreamReader, OutputStreamWriter }
import java.util.concurrent.atomic.{AtomicBoolean, AtomicReference}
import java.util.concurrent.ConcurrentHashMap
import scala.concurrent.{Future, Promise, ExecutionContext}
import scala.concurrent.duration._
import scala.compiletime.uninitialized
import org.slf4j.LoggerFactory

/** Persistent UCI Stockfish wrapper (Async Version).
  *
  * - Keeps the engine process alive to avoid startup overhead.
  * - Reads output asynchronously via a dedicated reader thread.
  * - Uses promises to complete evaluation requests.
  */
class StockfishClient(
    command: String = EnvLoader.getOrElse("STOCKFISH_BIN", "stockfish"),
    threads: Int = 1,
    hash: Int = 16
)(using ec: ExecutionContext) extends AutoCloseable:

  import StockfishClient.*

  private val logger = LoggerFactory.getLogger("chess.engine.client")

  private val processBuilder = new ProcessBuilder(command)
  processBuilder.redirectErrorStream(true) 
  
  private var process: Process = uninitialized
  private var writer: BufferedWriter = uninitialized
  private val isClosed = new AtomicBoolean(false)
  private val activePromise = new AtomicReference[Promise[EvalResult]](null)
  
  // Accumulated info lines for the current request
  // We use a simplified thread-safe accumulation since reader thread is single.
  // We'll reset this on each new request.
  private val currentInfo = new ConcurrentHashMap[Int, Line]() // multiPv -> Line

  // Initialize immediately
  start()

  def isAlive(): Boolean =
    process != null && process.isAlive

  protected def start(): Unit =
    this.synchronized {
      try
        process = processBuilder.start()
        // Log the PID for crash diagnostics (User Request)
        // Note: process.pid() is available since Java 9
        if (process.isAlive) {
           logger.info(s"Started Stockfish process. PID: ${process.pid()}")
        }
        writer = new BufferedWriter(new OutputStreamWriter(process.getOutputStream))
        
        // Start Reader Thread
        val readerThread = new Thread(() => readLoop(new BufferedReader(new InputStreamReader(process.getInputStream))), "stockfish-reader")
        readerThread.setDaemon(true)
        readerThread.start()
        
        writeLine("uci")
        // We wait for uciok/readyok synchronously during startup to ensure stability?
        // Or we can just fire them. Startup is rare.
        // Let's rely on the reader thread to discard initial noise or we can just send configs.
        // For robustness, let's just wait a bit (ugly) or assume uciok comes fast.
        // Actually, we can't easily wait synchronously here if we are pure async, but constructor is sync.
        // Let's trust local Stockfish speed or implementation details.
        // Standard Uci protocol: engine waits for "isready" before doing anything heavy.
        
        Thread.sleep(50) // Tiny startup buffer
        writeLine(s"setoption name Threads value $threads")
        writeLine(s"setoption name Hash value $hash")
        writeLine("isready")
        // The reader loop will handle output, but we are not waiting for "readyok". 
        // Requests sent before readyok might be buffered by OS pipe.
      catch
        case e: Throwable =>
          logger.error(s"Failed to start: ${e.getMessage}")
          throw e
    }

  private def writeLine(cmd: String): Unit =
    if writer != null then
      try 
        writer.write(cmd)
        writer.newLine()
        writer.flush()
      catch
        case e: Throwable => logger.error(s"Write error: ${e.getMessage}")

  private def readLoop(reader: BufferedReader): Unit =
    try
      var line = ""
      while { line = reader.readLine(); line != null } do
        if line.startsWith("bestmove") then
          val p = activePromise.getAndSet(null)
          if p != null then
             val best = line.split("\\s+").lift(1)
             // Collect results
             import scala.jdk.CollectionConverters._
             val lines = currentInfo.values().asScala.toList.sortBy(_.multiPv) // sort by multiPV index? or score?
             // Score desc usually, but multiPV index is safer.
             // Usually stockfish prints multipv 1 as best.
             val sortedLines = lines.sortBy(_.multiPv)
             p.success(EvalResult(sortedLines, best))
        else if line.startsWith("info") then
          parseInfo(line).foreach { l => 
            currentInfo.put(l.multiPv, l) 
          }
        // else ignore (uciok, readyok, etc)
      
      // Loop finished (EOF)
      if process.isAlive then
         logger.warn("Stockfish reader loop finished but process IS ALIVE. This is unexpected.")
      else
         logger.warn(s"Stockfish process terminated. Exit Code: ${process.exitValue()}")
    catch
      case e: Throwable => 
        logger.error(s"Reader loop error: ${e.getMessage}")
        val p = activePromise.getAndSet(null)
        if p != null then p.failure(e)

  def restart(): Future[Unit] =
    Future {
      this.synchronized {
        logger.warn("Restarting engine process...")
        closeProcess()
        start()
      }
    }

  private def closeProcess(): Unit =
    try
      if writer != null then
        try { writer.write("quit"); writer.newLine(); writer.flush(); writer.close() } catch { case _: Throwable => }
      if process != null then process.destroy()
    catch
      case e: Throwable => logger.warn(s"Error closing process: ${e.getMessage}")

  override def close(): Unit =
    if isClosed.compareAndSet(false, true) then
      val p = activePromise.getAndSet(null)
      if p != null then p.failure(new Exception("Client closed"))
      closeProcess()

  def evaluateFen(
      fen: String,
      depth: Int,
      multiPv: Int = 1,
      moveTimeMs: Option[Int] = Some(500),
      moves: List[String] = Nil
  ): Future[Either[String, EvalResult]] =
    if isClosed.get() then return Future.successful(Left("Client closed"))

    val p = Promise[EvalResult]()
    
    // We need to ensure only one request is active at a time?
    // EngineService usually manages the pool, ensuring exclusive access to this client instance.
    // If not, race conditions on `activePromise`. We assume exclusive access.
    
    if !activePromise.compareAndSet(null, p) then
       return Future.failed(new IllegalStateException("Engine client busy"))

    currentInfo.clear()
    
    val goCmd = moveTimeMs match
      case Some(ms) => s"go depth $depth movetime $ms"
      case None     => s"go depth $depth"

    // Execute
    try
      writeLine(s"setoption name MultiPV value $multiPv")
      writeLine(if moves.isEmpty then s"position fen $fen" else s"position fen $fen moves ${moves.mkString(" ")}")
      writeLine(goCmd)
    catch
       case e: Throwable => 
          activePromise.set(null)
          return Future.failed(e)

    // Timeout safety
    // We add a safety buffer to the expected movetime
    val timeoutDuration = moveTimeMs.map(_ + 3000).getOrElse(30000).millis
    
    AsyncUtils.timeout(p.future, timeoutDuration).map(Right(_)).recover {
      case _: java.util.concurrent.TimeoutException =>
        // Try to stop engine
        writeLine("stop")
        // If it doesn't respond quickly, we might want to kill/restart, 
        // but for now let's just fail and let wrapper handle restart
        activePromise.compareAndSet(p, null) 
        Left("Timeout")
      case e: Throwable =>
        activePromise.compareAndSet(p, null)
        Left(s"Error: ${e.getMessage}")
    }

  private def parseInfo(line: String): Option[Line] =
    try
      val tokens = line.split("\\s+").toList
      def idx(key: String) = tokens.indexOf(key)
      val depthIdx = idx("depth")
      val multipvIdx = idx("multipv")
      val scoreIdx = idx("score")
      val pvIdx = idx("pv")
      if depthIdx == -1 || scoreIdx == -1 || pvIdx == -1 then None
      else
        val depth = tokens.lift(depthIdx + 1).flatMap(s => s.toIntOption).getOrElse(0)
        val multiPv = if multipvIdx != -1 then tokens.lift(multipvIdx + 1).flatMap(_.toIntOption).getOrElse(1) else 1
        val (cp, mate) =
          tokens.lift(scoreIdx + 1) match
            case Some("cp") =>
              val cpVal = tokens.lift(scoreIdx + 2).flatMap(_.toIntOption)
              cpVal -> None
            case Some("mate") =>
              val mateVal = tokens.lift(scoreIdx + 2).flatMap(_.toIntOption)
              None -> mateVal
            case _ => None -> None
        val pv = tokens.drop(pvIdx + 1)
        Some(Line(multiPv = multiPv, depth = depth, cp = cp, mate = mate, pv = pv))
    catch
      case _: Throwable => None

object StockfishClient:
  case class Line(
      multiPv: Int,
      depth: Int,
      cp: Option[Int],
      mate: Option[Int],
      pv: List[String]
  ):
    lazy val cpOrMate: Int = cp.getOrElse {
      mate.fold(0)(m => if m > 0 then 32000 - m else -32000 - m)
    }
    lazy val winPercent: Double = cpToWinPercent(cpOrMate)

  case class EvalResult(lines: List[Line], bestmove: Option[String])

  def cpToWinPercent(cp: Int): Double =
    100.0 / (1.0 + math.exp(-0.004 * cp))

