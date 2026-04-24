package lila.commentary.validation

import java.io.{ BufferedReader, BufferedWriter, InputStreamReader, OutputStreamWriter }
import java.nio.charset.StandardCharsets
import java.nio.file.{ Files, Path, Paths }
import java.util.concurrent.TimeUnit

import scala.concurrent.duration.*
import scala.util.Try

private[validation] object StockfishProbe:

  final case class Result(
      bestMove: String,
      depth: Option[Int],
      cp: Option[Int],
      mate: Option[Int],
      pv: Vector[String],
      rawInfo: Option[String]
  )

  private val repoRoot = Paths.get(System.getProperty("user.dir")).normalize()
  private val engineCandidates = Vector(
    repoRoot.resolve("..").resolve("..").resolve("..")
      .resolve("tools").resolve("stockfish").resolve("stockfish").resolve("stockfish-windows-x86-64.exe")
      .normalize(),
    repoRoot.resolve("tools").resolve("stockfish").resolve("stockfish").resolve("stockfish-windows-x86-64.exe")
      .normalize()
  )

  val enginePath: Path =
    engineCandidates.find(Files.isRegularFile(_)).getOrElse(
      throw IllegalStateException(
        s"Stockfish executable not found. Tried: ${engineCandidates.mkString(", ")}"
      )
    )

  def probeFen(fen: String, movetime: FiniteDuration = 300.millis): Result =
    val attempts = 3
    var lastFailure: Option[StartupHandshakeException] = None
    var attempt = 1

    while attempt <= attempts do
      try return probeFenOnce(fen, movetime)
      catch
        case failure: StartupHandshakeException =>
          lastFailure = Some(failure)
          if attempt == attempts then throw failure
          Thread.sleep(100L * attempt)
          attempt += 1

    throw lastFailure.getOrElse(IllegalStateException(s"Stockfish startup failed for $fen"))

  private def probeFenOnce(fen: String, movetime: FiniteDuration): Result =
    val process = new ProcessBuilder(enginePath.toString).redirectErrorStream(true).start()
    val input =
      BufferedWriter(OutputStreamWriter(process.getOutputStream, StandardCharsets.US_ASCII))
    val output =
      BufferedReader(InputStreamReader(process.getInputStream, StandardCharsets.US_ASCII))
    val transcript = Vector.newBuilder[String]

    def send(command: String): Unit =
      input.write(command)
      input.newLine()
      input.flush()

    def readUntil(
        predicate: String => Boolean,
        onLine: String => Unit = _ => ()
    ): Option[String] =
      val deadline = System.nanoTime() + 10.seconds.toNanos
      var matched: Option[String] = None

      while matched.isEmpty && System.nanoTime() < deadline do
        val line = output.readLine()
        if line == null then
          return None
        transcript += line
        onLine(line)
        if predicate(line) then matched = Some(line)

      matched

    try
      send("uci")
      readUntil(_.startsWith("uciok")).getOrElse(
        throw StartupHandshakeException(
          s"Stockfish produced no uciok for $fen. Transcript: ${transcript.result().mkString(" | ")}"
        )
      )
      send("setoption name Threads value 1")
      send("setoption name Hash value 16")
      send("isready")
      readUntil(_.startsWith("readyok")).getOrElse(
        throw StartupHandshakeException(
          s"Stockfish produced no readyok for $fen. Transcript: ${transcript.result().mkString(" | ")}"
        )
      )
      send(s"position fen $fen")
      send(s"go movetime ${movetime.toMillis}")

      var lastInfo: Option[String] = None
      val bestMoveLine =
        readUntil(
          _.startsWith("bestmove "),
          line =>
            if line.startsWith("info depth") && line.contains(" score ") then
              lastInfo = Some(line)
        ).getOrElse(
          throw IllegalStateException(
            s"Stockfish closed its output before bestmove for $fen. Transcript: ${transcript.result().mkString(" | ")}"
          )
        )

      send("quit")

      val info = lastInfo.getOrElse(
        throw IllegalStateException(
          s"Stockfish produced no scored info line for $fen. Transcript: ${transcript.result().mkString(" | ")}"
        )
      )
      Result(
        bestMove =
          bestMoveLine.split("\\s+")(1),
        depth = extractInt(info, "depth"),
        cp = extractInt(info, "cp"),
        mate = extractInt(info, "mate"),
        pv = extractPv(info),
        rawInfo = lastInfo
      )
    finally
      Try(input.close())
      Try(output.close())
      if process.isAlive then
        process.destroy()
        if !process.waitFor(1, TimeUnit.SECONDS) then process.destroyForcibly()

  private final class StartupHandshakeException(message: String) extends IllegalStateException(message)

  private def extractInt(line: String, key: String): Option[Int] =
    line
      .split("\\s+")
      .sliding(2)
      .collectFirst {
        case Array(label, value) if label == key => value.toInt
      }

  private def extractPv(line: String): Vector[String] =
    val parts = line.split("\\s+").toVector
    parts.indexOf("pv") match
      case -1 => Vector.empty
      case index => parts.drop(index + 1)
