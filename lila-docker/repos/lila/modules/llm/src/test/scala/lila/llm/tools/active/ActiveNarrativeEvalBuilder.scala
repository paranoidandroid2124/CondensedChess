package lila.llm.tools.active

import java.io.{ BufferedReader, BufferedWriter, InputStreamReader, OutputStreamWriter }
import java.nio.charset.StandardCharsets
import java.nio.file.{ Path, Paths }
import java.time.Instant
import java.util.concurrent.{ LinkedBlockingQueue, TimeUnit }

import scala.util.control.NonFatal

import play.api.libs.json.Json

import lila.llm.{ MoveEval, PgnAnalysisHelper }
import lila.llm.analysis.NarrativeUtils
import lila.llm.model.strategic.VariationLine

object ActiveNarrativeEvalBuilder:

  import ActiveNarrativeCorpusSupport.*

  private val DefaultCorpusPath = Paths.get("modules/llm/docs/ActiveNarrativePlayerCorpus_20260311.json")
  private val DefaultOutPath = Paths.get("modules/llm/docs/ActiveNarrativePlayerCorpus_20260311.evals.json")
  private val DefaultDepth = 12
  private val DefaultMultiPv = 2
  private val DefaultTimeoutMs = 30000L
  private val EngineEnv = "LLM_ACTIVE_CORPUS_ENGINE_PATH"

  final case class Config(
      corpusPath: Path,
      outPath: Path,
      depth: Int,
      multiPv: Int,
      limit: Option[Int],
      enginePath: Path
  )

  def main(args: Array[String]): Unit =
    val config = parseConfig(args.toList)
    val corpus =
      readCorpus(config.corpusPath) match
        case Right(value) => value
        case Left(err) =>
          System.err.println(s"[active-eval-builder] failed to read corpus `${config.corpusPath}`: $err")
          sys.exit(1)

    val entries = flattenCorpus(corpus).take(config.limit.getOrElse(Int.MaxValue))
    if entries.isEmpty then
      System.err.println(s"[active-eval-builder] no games selected from `${config.corpusPath}`")
      sys.exit(1)

    val engine = new LocalUciEngine(config.enginePath, DefaultTimeoutMs)
    try
      val generatedAt = Instant.now().toString
      val games =
        entries.zipWithIndex.map { case (entry, idx) =>
          println(
            s"[active-eval-builder] ${idx + 1}/${entries.size} ${entry.player} vs ${entry.game.opponent} (${entry.game.date}, rank ${entry.game.selectionRank})"
          )
          val evals = buildGameEvals(entry, engine, config.depth, config.multiPv)
          EvalCacheGame(
            gameKey = entry.gameKey,
            player = entry.player,
            date = entry.game.date,
            round = entry.game.round,
            white = entry.game.white,
            black = entry.game.black,
            totalPlies = entry.totalPlies,
            evals = evals
          )
        }

      val cache =
        ActiveNarrativeEvalCache(
          corpusTitle = corpus.title,
          corpusAsOfDate = corpus.asOfDate,
          corpusGeneratedAt = corpus.generatedAt,
          gameCount = games.size,
          engine =
            EvalEngineMeta(
              name = engine.engineName,
              path = config.enginePath.toAbsolutePath.normalize.toString,
              depth = config.depth,
              multiPv = config.multiPv,
              generatedAt = generatedAt
            ),
          games = games
        )

      writeJson(config.outPath, Json.toJson(cache))
      println(
        s"[active-eval-builder] wrote `${config.outPath}` (games=${games.size}, depth=${config.depth}, multiPv=${config.multiPv})"
      )
    finally engine.close()

  private def buildGameEvals(
      entry: CorpusEntry,
      engine: LocalUciEngine,
      depth: Int,
      multiPv: Int
  ): List[MoveEval] =
    val plyData =
      PgnAnalysisHelper.extractPlyData(entry.game.pgn) match
        case Right(value) => value
        case Left(err) =>
          throw new IllegalArgumentException(s"PGN parse failed for `${entry.gameKey}`: $err")

    engine.newGame()
    plyData.map { pd =>
      val afterFen = NarrativeUtils.uciListToFen(pd.fen, List(pd.playedUci))
      val variations = engine.analyze(afterFen, depth, multiPv)
      val best = variations.headOption
      MoveEval(
        ply = pd.ply,
        cp = best.map(_.scoreCp).getOrElse(0),
        mate = best.flatMap(_.mate),
        pv = best.map(_.moves).getOrElse(Nil),
        variations = variations
      )
    }

  private def parseConfig(args: List[String]): Config =
    val positional = positionalArgs(args)
    val corpusPath = positional.headOption.map(Paths.get(_)).getOrElse(DefaultCorpusPath)
    val outPath = positional.lift(1).map(Paths.get(_)).getOrElse(DefaultOutPath)
    val depth = optionInt(args, "--depth").getOrElse(DefaultDepth).max(8)
    val multiPv = optionInt(args, "--multi-pv").orElse(optionInt(args, "--multiPv")).getOrElse(DefaultMultiPv).max(1)
    val limit = optionInt(args, "--limit").filter(_ > 0)
    val enginePath =
      optionString(args, "--engine")
        .orElse(sys.env.get(EngineEnv).map(_.trim).filter(_.nonEmpty))
        .map(Paths.get(_))
        .getOrElse {
          System.err.println(
            s"[active-eval-builder] missing engine path. Set `$EngineEnv` or pass `--engine /path/to/uci-engine`."
          )
          sys.exit(1)
        }

    Config(
      corpusPath = corpusPath,
      outPath = outPath,
      depth = depth,
      multiPv = multiPv,
      limit = limit,
      enginePath = enginePath
    )

  private def positionalArgs(args: List[String]): List[String] =
    val optionsWithValue = Set("--depth", "--multi-pv", "--multiPv", "--limit", "--engine")
    val out = scala.collection.mutable.ListBuffer.empty[String]
    var idx = 0
    while idx < args.length do
      val current = args(idx)
      if current.startsWith("--") then
        idx += (if optionsWithValue.contains(current) then 2 else 1)
      else
        out += current
        idx += 1
    out.toList

  private def optionString(args: List[String], name: String): Option[String] =
    args.sliding(2).collectFirst {
      case List(flag, value) if flag == name => value
    }.map(_.trim).filter(_.nonEmpty)

  private def optionInt(args: List[String], name: String): Option[Int] =
    optionString(args, name).flatMap(_.toIntOption)

  private final case class ParsedInfo(
      depth: Int,
      multiPv: Int,
      scoreType: String,
      scoreValue: Int,
      moves: List[String]
  )

  private final class LocalUciEngine(enginePath: Path, timeoutMs: Long):
    private val process =
      new ProcessBuilder(enginePath.toAbsolutePath.normalize.toString)
        .redirectErrorStream(true)
        .start()
    private val lines = LinkedBlockingQueue[String]()
    private val writer =
      new BufferedWriter(new OutputStreamWriter(process.getOutputStream, StandardCharsets.UTF_8))
    private val reader = new Thread(() => pumpOutput(), "active-narrative-eval-builder-engine")
    @volatile private var closed = false
    private var resolvedEngineName = enginePath.getFileName.toString

    reader.setDaemon(true)
    reader.start()
    initialize()

    def engineName: String = resolvedEngineName

    def newGame(): Unit =
      send("ucinewgame")
      ready()

    def analyze(fen: String, depth: Int, multiPv: Int): List[VariationLine] =
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

      val normalized =
        byPv.toList.sortBy(_._1).map { case (_, info) =>
          normalizeLine(info, perspectiveSign)
        }
      if normalized.nonEmpty then normalized
      else bestMove.toList.map(move => VariationLine(moves = List(move), scoreCp = 0, mate = None, depth = 0))

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

    private def normalizeLine(info: ParsedInfo, perspectiveSign: Int): VariationLine =
      VariationLine(
        moves = info.moves,
        scoreCp = if info.scoreType == "cp" then info.scoreValue * perspectiveSign else 0,
        mate = Option.when(info.scoreType == "mate")(info.scoreValue * perspectiveSign),
        depth = info.depth
      )

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
