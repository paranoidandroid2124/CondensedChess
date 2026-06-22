package lila.chessjudgment.analysis.opening

import java.nio.charset.StandardCharsets
import java.nio.file.{ Files, Path }

import scala.collection.mutable
import scala.io.Source
import scala.util.Using

import chess.Replay
import chess.format.Fen
import chess.format.pgn.{ Parser, PgnStr }
import chess.opening.OpeningDb

import lila.chessjudgment.model.judgment.*

object OpeningIndexBuilder:

  private final case class Config(
      input: String,
      recognitionOutput: Path,
      themePriorOutput: Option[Path],
      maxGames: Int,
      minElo: Int,
      maxPly: Int
  )

  private final case class CountKey(
      movePrefixHash: String,
      positionKey: String,
      matchedPly: Int,
      eco: String,
      name: String,
      family: String,
      lineage: String
  )

  private final case class ReplayScan(
      positionCount: Int,
      openings: List[CountKey]
  )

  private final case class BuildResult(
      entries: List[OpeningRecognitionIndex.Entry],
      acceptedGames: Int,
      scannedPositions: Int,
      recognizedPositions: Int
  ):
    def coverage: Double =
      if scannedPositions <= 0 then 0.0
      else recognizedPositions.toDouble / scannedPositions.toDouble

  def main(args: Array[String]): Unit =
    val config = parseConfig(args).getOrElse:
      throw IllegalArgumentException(
        "usage: OpeningIndexBuilder <input.pgn|-> <recognition-output.tsv> [theme-prior-output.tsv] [maxGames=50000] [minElo=1800] [maxPly=20]"
      )
    val result =
      if config.input == "-" then buildRecognition(Source.stdin.getLines(), config.maxGames, config.minElo, config.maxPly)
      else
        Using.resource(Source.fromFile(config.input, "UTF-8")): source =>
          buildRecognition(source.getLines(), config.maxGames, config.minElo, config.maxPly)
    writeRecognition(config.recognitionOutput, result.entries)
    config.themePriorOutput.foreach(writeThemePrior)
    println(
      f"OpeningIndexBuilder coverage acceptedGames=${result.acceptedGames} scannedPositions=${result.scannedPositions} " +
        f"recognizedPositions=${result.recognizedPositions} coverage=${result.coverage}%.4f entries=${result.entries.size}"
    )

  private def parseConfig(args: Array[String]): Option[Config] =
    Option.when(args.length >= 2)(
      Config(
        input = args(0),
        recognitionOutput = Path.of(args(1)),
        themePriorOutput = args.lift(2).map(Path.of(_)),
        maxGames = args.lift(3).flatMap(_.toIntOption).getOrElse(50_000).max(1),
        minElo = args.lift(4).flatMap(_.toIntOption).getOrElse(1800).max(0),
        maxPly = args.lift(5).flatMap(_.toIntOption).getOrElse(20).max(1)
      )
    )

  def buildRecognitionEntries(
      lines: Iterator[String],
      maxAcceptedGames: Int,
      minElo: Int,
      maxPly: Int
  ): List[OpeningRecognitionIndex.Entry] =
    buildRecognition(lines, maxAcceptedGames, minElo, maxPly).entries

  private def buildRecognition(
      lines: Iterator[String],
      maxAcceptedGames: Int,
      minElo: Int,
      maxPly: Int
  ): BuildResult =
    val counts = mutable.Map.empty[CountKey, Int].withDefaultValue(0)
    val sampleCounts = mutable.Map.empty[(String, String, Int), Int].withDefaultValue(0)
    var accepted = 0
    var scannedPositions = 0
    var recognizedPositions = 0
    foreachPgn(lines, () => accepted >= maxAcceptedGames) { pgn =>
      if accepted < maxAcceptedGames && passesEloFilter(pgn, minElo) then
        accepted += 1
        val scan = replayOpenings(pgn, maxPly)
        scannedPositions += scan.positionCount
        recognizedPositions += scan.openings.size
        scan.openings.foreach { key =>
          counts.update(key, counts(key) + 1)
          val sampleKey = (key.movePrefixHash, key.positionKey, key.matchedPly)
          sampleCounts.update(sampleKey, sampleCounts(sampleKey) + 1)
        }
    }
    val entries = counts.toList
      .map { case (key, frequency) =>
        val sampleCount = sampleCounts((key.movePrefixHash, key.positionKey, key.matchedPly)).max(frequency)
        val confidence = confidenceFor(frequency, sampleCount, key.matchedPly)
        val identity =
          OpeningIdentity(
            eco = Some(key.eco).filter(_.nonEmpty),
            name = Some(key.name).filter(_.nonEmpty),
            family = OpeningFamily.fromRaw(key.family).orElse(OpeningFamily.fromEco(key.eco))
          )
        OpeningRecognitionIndex.Entry(
          movePrefixHash = key.movePrefixHash,
          positionKey = key.positionKey,
          matchedPly = key.matchedPly,
          candidate = OpeningCandidate(
            identity = identity,
            lineage = Some(key.lineage).filter(_.nonEmpty),
            frequency = frequency,
            sampleCount = sampleCount,
            confidence = confidence
          )
        )
      }
      .filter(_.candidate.identity.name.nonEmpty)
      .sortBy(entry => (entry.positionKey, entry.matchedPly, -entry.candidate.frequency, entry.candidate.identity.name))
    BuildResult(entries, accepted, scannedPositions, recognizedPositions)

  private def foreachPgn(lines: Iterator[String], stop: () => Boolean)(f: String => Unit): Unit =
    val current = mutable.ListBuffer.empty[String]
    def flush(): Unit =
      val pgn = current.mkString("\n").trim
      current.clear()
      if pgn.nonEmpty && !stop() then
        f(pgn)
    while lines.hasNext && !stop() do
      val line = lines.next()
      if line.startsWith("[Event ") && current.exists(_.trim.nonEmpty) then flush()
      current += line
    flush()

  private def passesEloFilter(pgn: String, minElo: Int): Boolean =
    if minElo <= 0 then true
    else
      val tags = pgnTags(pgn)
      val white = tags.get("WhiteElo").flatMap(_.toIntOption)
      val black = tags.get("BlackElo").flatMap(_.toIntOption)
      white.exists(_ >= minElo) && black.exists(_ >= minElo)

  private def pgnTags(pgn: String): Map[String, String] =
    val tagPattern = """^\[([A-Za-z0-9_]+)\s+"(.*)"\]\s*$""".r
    pgn.linesIterator.collect { case tagPattern(name, value) => name -> value }.toMap

  private def replayOpenings(pgn: String, maxPly: Int): ReplayScan =
    Parser.full(PgnStr(pgn)).toOption match
      case None => ReplayScan(0, Nil)
      case Some(parsed) =>
        val result = Replay.makeReplay(parsed.toGame, parsed.mainline.take(maxPly))
        if result.failure.nonEmpty then ReplayScan(0, Nil)
        else
          val prefix = mutable.ListBuffer.empty[String]
          val moves = result.replay.chronoMoves.take(maxPly)
          val openings = moves.flatMap { moveOrDrop =>
            val uci = moveOrDrop.toUci.uci
            prefix += OpeningIndexKeys.normalizeUci(uci)
            val fen = Fen.write(moveOrDrop.after)
            OpeningDb.findByFullFen(fen).map { opening =>
              val eco = opening.eco.value
              val name = opening.name.value
              CountKey(
                movePrefixHash = OpeningIndexKeys.movePrefixHash(prefix.toList),
                positionKey = OpeningIndexKeys.positionKey(fen.value),
                matchedPly = prefix.size,
                eco = eco,
                name = name,
                family = eco.headOption.map(_.toString).getOrElse(""),
                lineage = lineageFor(name)
              )
            }
          }
          ReplayScan(moves.size, openings)

  private def lineageFor(name: String): String =
    OpeningThemePriorIndex.lineageForOpeningName(name)

  private def confidenceFor(frequency: Int, sampleCount: Int, matchedPly: Int): Double =
    val share = if sampleCount <= 0 then 0.0 else frequency.toDouble / sampleCount.toDouble
    val depthWeight = math.min(1.0, 0.55 + matchedPly.toDouble / 20.0)
    math.max(0.0, math.min(0.98, share * depthWeight))

  private def writeRecognition(path: Path, entries: List[OpeningRecognitionIndex.Entry]): Unit =
    val rows = OpeningRecognitionIndex.TsvHeader :: entries.map(OpeningRecognitionIndex.tsvRow)
    write(path, rows)

  private def writeThemePrior(path: Path): Unit =
    val entries = OpeningThemePriorIndex.default.allEntries
    val rows = OpeningThemePriorIndex.TsvHeader :: entries.map(OpeningThemePriorIndex.tsvRow)
    write(path, rows)

  private def write(path: Path, rows: List[String]): Unit =
    Option(path.getParent).foreach(parent => Files.createDirectories(parent))
    Files.writeString(path, rows.mkString(System.lineSeparator()) + System.lineSeparator(), StandardCharsets.UTF_8)
