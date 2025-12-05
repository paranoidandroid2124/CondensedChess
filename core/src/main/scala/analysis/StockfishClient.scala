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
final class StockfishClient(command: String = sys.env.getOrElse("STOCKFISH_BIN", "stockfish")) extends AutoCloseable:

  case class Line(
      multiPv: Int,
      depth: Int,
      cp: Option[Int],
      mate: Option[Int],
      pv: List[String]
  ):
    lazy val cpOrMate: Int = cp.getOrElse {
      // mate in N → large value
      mate.fold(0)(m => if m > 0 then 32000 - m else -32000 - m)
    }
    lazy val winPercent: Double = StockfishClient.cpToWinPercent(cpOrMate)

  case class EvalResult(lines: List[Line], bestmove: Option[String])

  private val processBuilder = new ProcessBuilder(command)
  processBuilder.redirectErrorStream(true)
  private var process: Process = uninitialized
  private var reader: BufferedReader = uninitialized
  private var writer: BufferedWriter = uninitialized
  private val isClosed = new AtomicBoolean(false)

  // Initialize immediately
  start()

  private def start(): Unit =
    try
      process = processBuilder.start()
      reader = new BufferedReader(new InputStreamReader(process.getInputStream))
      writer = new BufferedWriter(new OutputStreamWriter(process.getOutputStream))
      
      sendCommand("uci")
      // Consume header until uciok
      var line = ""
      while { line = reader.readLine(); line != null && line != "uciok" } do ()
      
      sendCommand("isready")
      while { line = reader.readLine(); line != null && line != "readyok" } do ()
    catch
      case e: Throwable =>
        System.err.println(s"[StockfishClient] Failed to start: ${e.getMessage}")
        throw e

  def evaluateFen(
      fen: String,
      depth: Int,
      multiPv: Int = 1,
      moveTimeMs: Option[Int] = Some(500)
  ): Either[String, EvalResult] =
    if isClosed.get() then return Left("Client closed")
    
    try
      this.synchronized {
        sendCommand(s"setoption name MultiPV value $multiPv")
        sendCommand(s"position fen $fen")
        
        val goCmd = moveTimeMs match
          case Some(ms) => s"go depth $depth movetime $ms"
          case None     => s"go depth $depth"
        
        sendCommand(goCmd)
        
        val infoByPv = mutable.Map.empty[Int, Line]
        var best: Option[String] = None
        var line = ""
        var done = false
        
        while !done && { line = reader.readLine(); line != null } do
          if line.startsWith("bestmove") then
            best = line.split("\\s+").lift(1)
            done = true
          else if line.startsWith("info") then
            parseInfo(line).foreach { l => infoByPv.update(l.multiPv, l) }
            
        val lines = infoByPv.toList.sortBy(_._1).map(_._2)
        Right(EvalResult(lines, best))
      }
    catch
      case e: Throwable => Left(s"Engine error: ${e.getMessage}")

  private def sendCommand(cmd: String): Unit =
    writer.write(cmd)
    writer.newLine()
    writer.flush()

  override def close(): Unit =
    if isClosed.compareAndSet(false, true) then
      try
        if writer != null then
          writer.write("quit")
          writer.newLine()
          writer.flush()
          writer.close()
        if reader != null then reader.close()
        if process != null then process.destroy()
      catch
        case e: Throwable => System.err.println(s"[StockfishClient] Error closing: ${e.getMessage}")

  private def parseInfo(line: String): Option[Line] =
    // info depth 18 seldepth 30 multipv 1 score cp 23 nodes ... pv e2e4 e7e5 ...
    val tokens = line.split("\\s+").toList
    def idx(key: String) = tokens.indexOf(key)
    val depthIdx = idx("depth")
    val multipvIdx = idx("multipv")
    val scoreIdx = idx("score")
    val pvIdx = idx("pv")
    if depthIdx == -1 || scoreIdx == -1 || pvIdx == -1 then None
    else
      val depth = tokens.lift(depthIdx + 1).flatMap(s => s.toIntOption).getOrElse(0)
      val multiPv = tokens.lift(multipvIdx + 1).flatMap(_.toIntOption).getOrElse(1)
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

object StockfishClient:
  def cpToWinPercent(cp: Int): Double =
    100.0 / (1.0 + math.exp(-0.004 * cp))
