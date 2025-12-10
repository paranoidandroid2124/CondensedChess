package chess
package analysis

import java.io.{ BufferedReader, BufferedWriter, InputStreamReader, OutputStreamWriter }
import java.util.concurrent.atomic.AtomicBoolean
import scala.collection.mutable
import scala.compiletime.uninitialized

/** Persistent UCI Stockfish wrapper.
  *
  * - Keeps the engine process alive to avoid startup overhead.
  * - Supports depth + movetime constraints.
  * - Must call close() to release resources.
  */
class StockfishClient(command: String = EnvLoader.getOrElse("STOCKFISH_BIN", "stockfish")) extends AutoCloseable:

  import StockfishClient.*
  import org.slf4j.LoggerFactory

  private val logger = LoggerFactory.getLogger("chess.engine.client")

  private val processBuilder = new ProcessBuilder(command)
  processBuilder.redirectErrorStream(true)
  private var process: Process = uninitialized
  private var reader: BufferedReader = uninitialized
  private var writer: BufferedWriter = uninitialized
  private val isClosed = new AtomicBoolean(false)

  // Initialize immediately
  start()

  protected def start(): Unit =
    try
      process = processBuilder.start()
      reader = new BufferedReader(new InputStreamReader(process.getInputStream))
      writer = new BufferedWriter(new OutputStreamWriter(process.getOutputStream))
      
      writeLine("uci")
      // Consume header until uciok
      var line = ""
      while { line = reader.readLine(); line != null && line != "uciok" } do ()
      
      writeLine("isready")
      while { line = reader.readLine(); line != null && line != "readyok" } do ()
    catch
      case e: Throwable =>
        logger.error(s"Failed to start/restart: ${e.getMessage}")
        throw e



  private def writeLine(cmd: String): Unit =
    if writer != null then
      writer.write(cmd)
      writer.newLine()
      writer.flush()

  private def restart(): Unit =
    this.synchronized {
      logger.warn("Restarting engine process...")
      closeProcess()
      start()
    }

  private def closeProcess(): Unit =
    try
      if writer != null then
        try { writer.write("quit"); writer.newLine(); writer.flush(); writer.close() } catch { case _: Throwable => }
      if reader != null then reader.close()
      if process != null then process.destroy()
    catch
      case e: Throwable => logger.warn(s"Error closing process: ${e.getMessage}")

  def evaluateFen(
      fen: String,
      depth: Int,
      multiPv: Int = 1,
      moveTimeMs: Option[Int] = Some(500),
      moves: List[String] = Nil
  ): Either[String, EvalResult] =
    if isClosed.get() then return Left("Client closed")
    
    try
      this.synchronized {
        try
          doEvaluate(fen, depth, multiPv, moveTimeMs, moves)
        catch
          case e: java.io.IOException =>
            logger.warn(s"IO Error: ${e.getMessage}. Attempting restart...")
            restart()
            doEvaluate(fen, depth, multiPv, moveTimeMs, moves)
      }
    catch
      case e: Throwable => 
        logger.error(s"Exception: ${e.getMessage}", e)
        Left(s"Engine error: ${e.getMessage}")

  private def doEvaluate(
      fen: String,
      depth: Int,
      multiPv: Int,
      moveTimeMs: Option[Int],
      moves: List[String]
  ): Either[String, EvalResult] =
    // System.err.println(s"[StockfishClient] Evaluating FEN: $fen")
    writeLine(s"setoption name MultiPV value $multiPv")
    if moves.isEmpty then
      writeLine(s"position fen $fen")
    else
      writeLine(s"position fen $fen moves ${moves.mkString(" ")}")
    
    val goCmd = moveTimeMs match
      case Some(ms) => s"go depth $depth movetime $ms"
      case None     => s"go depth $depth"
    
    writeLine(goCmd)
    
    val infoByPv = mutable.Map.empty[Int, Line]
    var best: Option[String] = None
    var line = ""
    var done = false
    
    // Timeout safety: wait at most (moveTimeMs + 2000) or 10s if depth-only
    val maxWaitMs = moveTimeMs.map(_ + 3000).getOrElse(15000) 
    val startWait = System.currentTimeMillis()
    
    while !done do
      if System.currentTimeMillis() - startWait > maxWaitMs then
        logger.warn(s"Timeout waiting for bestmove (max ${maxWaitMs}ms)")
        writeLine("stop") // Try to stop it
        // Give it a moment to spit out bestmove
        Thread.sleep(100)
        if !reader.ready() then
          throw new java.io.IOException("Engine timeout (soft)")

      if reader.ready() then
        line = reader.readLine()
        if line == null then throw new java.io.IOException("Stream closed unexpected")
        
        // if line.startsWith("info depth") then System.err.println(s"[StockfishClient] $line")
        if line.startsWith("bestmove") then
          best = line.split("\\s+").lift(1)
          done = true
        else if line.startsWith("info") then
          parseInfo(line).foreach { l => infoByPv.update(l.multiPv, l) }
      else
        Thread.sleep(10)
        
    val lines = infoByPv.toList.sortBy(_._1).map(_._2)
    Right(EvalResult(lines, best))

  override def close(): Unit =
    if isClosed.compareAndSet(false, true) then
      closeProcess()

  private def parseInfo(line: String): Option[Line] =
    // info depth 18 seldepth 30 multipv 1 score cp 23 nodes ... pv e2e4 e7e5 ...
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
