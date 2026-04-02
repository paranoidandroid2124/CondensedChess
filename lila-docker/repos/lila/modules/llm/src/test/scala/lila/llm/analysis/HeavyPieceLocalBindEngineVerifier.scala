package lila.llm.analysis

import java.io.{ BufferedReader, BufferedWriter, InputStreamReader, OutputStreamWriter }
import java.nio.charset.StandardCharsets
import java.nio.file.{ Files, Path, Paths }
import java.util.concurrent.{ LinkedBlockingQueue, TimeUnit }

import scala.util.control.NonFatal

private[analysis] object HeavyPieceLocalBindEngineVerifier:

  private val DefaultDepth = 12
  private val DefaultMultiPv = 3
  private val DefaultTimeoutMs = 30000L
  private val EngineEnvVars = List("STOCKFISH_BIN", "LLM_ACTIVE_CORPUS_ENGINE_PATH")
  private val FallbackEnginePaths =
    List(
      Paths.get("..", "..", "..", "..", "tools", "stockfish", "stockfish", "stockfish-windows-x86-64.exe")
    )

  final case class EngineLine(
      moves: List[String],
      scoreCp: Int,
      mate: Option[Int],
      depth: Int
  )

  final case class EngineAnalysis(
      engineName: String,
      enginePath: Path,
      bestMove: Option[String],
      lines: List[EngineLine]
  )

  def analyze(
      fen: String,
      depth: Int = DefaultDepth,
      multiPv: Int = DefaultMultiPv
  ): Option[EngineAnalysis] =
    resolvedEnginePath().map { enginePath =>
      val engine = new LocalUciEngine(enginePath, timeoutMs = DefaultTimeoutMs)
      try
        engine.newGame()
        val result = engine.analyze(fen, depth, multiPv)
        EngineAnalysis(
          engineName = engine.engineName,
          enginePath = enginePath,
          bestMove = result.bestMove,
          lines = result.lines
        )
      finally engine.close()
    }

  def resolvedEnginePath(): Option[Path] =
    EngineEnvVars.iterator
      .flatMap(name => sys.env.get(name).map(_.trim).filter(_.nonEmpty))
      .map(Paths.get(_))
      .find(path => Files.isRegularFile(path))
      .orElse(
        FallbackEnginePaths
          .map(_.toAbsolutePath.normalize)
          .find(path => Files.isRegularFile(path))
      )

  private final case class ParsedInfo(
      depth: Int,
      multiPv: Int,
      scoreType: String,
      scoreValue: Int,
      moves: List[String]
  )

  private final case class AnalysisResult(
      bestMove: Option[String],
      lines: List[EngineLine]
  )

  private final class LocalUciEngine(enginePath: Path, timeoutMs: Long):
    private val process =
      new ProcessBuilder(enginePath.toAbsolutePath.normalize.toString)
        .redirectErrorStream(true)
        .start()
    private val lines = LinkedBlockingQueue[String]()
    private val writer =
      new BufferedWriter(new OutputStreamWriter(process.getOutputStream, StandardCharsets.UTF_8))
    private val reader = new Thread(() => pumpOutput(), "heavy-piece-local-bind-engine")
    @volatile private var closed = false
    private var resolvedEngineName = enginePath.getFileName.toString

    reader.setDaemon(true)
    reader.start()
    initialize()

    def engineName: String = resolvedEngineName

    def newGame(): Unit =
      send("ucinewgame")
      ready()

    def analyze(fen: String, depth: Int, multiPv: Int): AnalysisResult =
      drainPending()
      send(s"setoption name MultiPV value $multiPv")
      send(s"position fen $fen")
      send(s"go depth $depth")

      val perspectiveSign = whitePerspectiveSign(fen)
      val byPv = scala.collection.mutable.Map.empty[Int, ParsedInfo]
      var bestMove: Option[String] = None
      val deadline = System.nanoTime() + timeoutMs * 1000000L
      var done = false

      while !done do
        val line = awaitLine(deadline)
        if line.startsWith("info ") then
          parseInfoLine(line).foreach { info =>
            val prev = byPv.get(info.multiPv)
            if prev.forall(p => info.depth > p.depth || (info.depth == p.depth && info.moves.size >= p.moves.size)) then
              byPv.update(info.multiPv, info)
          }
        else if line.startsWith("bestmove") then
          bestMove =
            line
              .split("\\s+")
              .lift(1)
              .map(_.trim)
              .filter(move => move.nonEmpty && move != "(none)")
          done = true

      AnalysisResult(
        bestMove = bestMove,
        lines =
          byPv.toList.sortBy(_._1).map { case (_, info) =>
            EngineLine(
              moves = info.moves,
              scoreCp = if info.scoreType == "cp" then info.scoreValue * perspectiveSign else 0,
              mate = Option.when(info.scoreType == "mate")(info.scoreValue * perspectiveSign),
              depth = info.depth
            )
          }
      )

    def close(): Unit =
      if !closed then
        closed = true
        try send("quit")
        catch case _: Throwable => ()
        writer.close()
        if process.isAlive then process.destroy()

    private def initialize(): Unit =
      send("uci")
      val deadline = System.nanoTime() + timeoutMs * 1000000L
      var uciOk = false
      while !uciOk do
        val line = awaitLine(deadline)
        if line.startsWith("id name ") then
          resolvedEngineName = line.stripPrefix("id name ").trim
        else if line == "uciok" then uciOk = true
      send("setoption name Threads value 1")
      send("setoption name Hash value 64")
      ready()

    private def ready(): Unit =
      send("isready")
      val deadline = System.nanoTime() + timeoutMs * 1000000L
      var isReady = false
      while !isReady do
        val line = awaitLine(deadline)
        if line == "readyok" then isReady = true

    private def send(command: String): Unit =
      writer.write(command)
      writer.newLine()
      writer.flush()

    private def awaitLine(deadlineNs: Long): String =
      val remainingMs = math.max(1L, (deadlineNs - System.nanoTime()) / 1000000L)
      val next = lines.poll(remainingMs, TimeUnit.MILLISECONDS)
      if next != null then next
      else throw new IllegalStateException(s"engine timeout waiting for response from `${enginePath}`")

    private def drainPending(): Unit =
      while lines.poll() != null do ()

    private def pumpOutput(): Unit =
      val reader =
        new BufferedReader(new InputStreamReader(process.getInputStream, StandardCharsets.UTF_8))
      try
        var line = reader.readLine()
        while line != null do
          val trimmed = line.trim
          if trimmed.nonEmpty then lines.offer(trimmed)
          line = reader.readLine()
      catch
        case NonFatal(_) if closed => ()
        case NonFatal(e)           => lines.offer(s"info string reader-error ${e.getMessage}")
      finally reader.close()

    private def whitePerspectiveSign(fen: String): Int =
      fen.trim.split("\\s+").lift(1) match
        case Some("b") => -1
        case _         => 1

    private def parseInfoLine(line: String): Option[ParsedInfo] =
      val pvIdx = line.indexOf(" pv ")
      if pvIdx < 0 then None
      else
        val scoreMatch = """\bscore (cp|mate) (-?\d+)""".r.findFirstMatchIn(line)
        scoreMatch.flatMap { score =>
          val moves =
            line
              .substring(pvIdx + 4)
              .trim
              .split("\\s+")
              .toList
              .map(_.trim)
              .filter(_.nonEmpty)
          Option.when(moves.nonEmpty) {
            ParsedInfo(
              depth = """\bdepth (\d+)""".r.findFirstMatchIn(line).flatMap(_.group(1).toIntOption).getOrElse(0),
              multiPv = """\bmultipv (\d+)""".r.findFirstMatchIn(line).flatMap(_.group(1).toIntOption).getOrElse(1),
              scoreType = score.group(1),
              scoreValue = score.group(2).toInt,
              moves = moves
            )
          }
        }
