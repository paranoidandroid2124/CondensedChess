package lila.chessjudgment.analysis.opening

import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.util.Locale

import scala.io.Source
import scala.util.Using

import lila.chessjudgment.model.judgment.*

object OpeningIndexKeys:

  def movePrefixHash(moves: List[String]): String =
    hash(moves.map(normalizeUci).filter(_.nonEmpty).mkString(" "))

  def positionKey(fen: String): String =
    hash(boardStateFen(fen))

  def normalizeUci(uci: String): String =
    Option(uci).getOrElse("").trim.toLowerCase(Locale.ROOT)

  def boardStateFen(fen: String): String =
    Option(fen).getOrElse("").trim.split("\\s+").filter(_.nonEmpty).take(4).mkString(" ")

  private def hash(value: String): String =
    val digest = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8))
    digest.iterator.map(byte => f"${byte & 0xff}%02x").mkString.take(16)

final class OpeningRecognitionIndex private (
    entries: List[OpeningRecognitionIndex.Entry]
):

  private val byPrefixAndPosition: Map[(String, String), List[OpeningRecognitionIndex.Entry]] =
    entries.groupBy(entry => entry.movePrefixHash -> entry.positionKey)

  private val byPosition: Map[String, List[OpeningRecognitionIndex.Entry]] =
    entries.groupBy(_.positionKey)

  def allEntries: List[OpeningRecognitionIndex.Entry] =
    entries

  def recognize(movePrefixUci: List[String], fen: String, ply: Int): Option[OpeningRecognition] =
    val movePrefixHash = OpeningIndexKeys.movePrefixHash(movePrefixUci)
    val positionKey = OpeningIndexKeys.positionKey(fen)
    val exact =
      if movePrefixUci.nonEmpty then byPrefixAndPosition.getOrElse(movePrefixHash -> positionKey, Nil)
      else Nil
    if exact.nonEmpty then
      select(
        candidates = exact,
        movePrefixHash = movePrefixHash,
        positionKey = positionKey,
        ply = ply,
        matchedBy = OpeningRecognitionMatchKind.ExactPrefixAndPosition
      )
    else
      select(
        candidates = byPosition.getOrElse(positionKey, Nil),
        movePrefixHash = movePrefixHash,
        positionKey = positionKey,
        ply = ply,
        matchedBy = OpeningRecognitionMatchKind.PositionTransposition
      )

  private def select(
      candidates: List[OpeningRecognitionIndex.Entry],
      movePrefixHash: String,
      positionKey: String,
      ply: Int,
      matchedBy: OpeningRecognitionMatchKind
  ): Option[OpeningRecognition] =
    val eligible =
      candidates.filter(entry => ply <= 0 || entry.matchedPly <= ply)
    val bestPly = eligible.map(_.matchedPly).maxOption
    bestPly.flatMap { matchedPly =>
      val ranked =
        eligible
          .filter(_.matchedPly == matchedPly)
          .sortBy(entry => (-entry.candidate.confidence, -entry.candidate.frequency, -entry.candidate.sampleCount))
      ranked.headOption.map { best =>
        OpeningRecognition(
          movePrefixHash = movePrefixHash,
          positionKey = positionKey,
          matchedBy = matchedBy,
          candidates = ranked.map(_.candidate).distinctBy(candidate => candidate.identity -> candidate.lineage).take(3),
          matchedPly = matchedPly,
          frequency = best.candidate.frequency,
          sampleCount = best.candidate.sampleCount,
          confidence = best.candidate.confidence
        )
      }
    }

object OpeningRecognitionIndex:

  final case class Entry(
      movePrefixHash: String,
      positionKey: String,
      matchedPly: Int,
      candidate: OpeningCandidate
  )

  val TsvHeader: String =
    "move_prefix_hash\tposition_key\tmatched_ply\teco\tname\tfamily\tlineage\tfrequency\tsample_count\tconfidence"

  private val ResourcePath = "lila/chessjudgment/openings/opening_recognition_index.tsv"

  lazy val default: OpeningRecognitionIndex =
    fromEntries(loadResourceRows(ResourcePath))

  val empty: OpeningRecognitionIndex =
    fromEntries(Nil)

  def fromEntries(entries: IterableOnce[Entry]): OpeningRecognitionIndex =
    OpeningRecognitionIndex(entries.iterator.toList)

  def fromTsvLines(lines: IterableOnce[String]): OpeningRecognitionIndex =
    fromEntries(parseRows(lines))

  def tsvRow(entry: Entry): String =
    val identity = entry.candidate.identity
    List(
      entry.movePrefixHash,
      entry.positionKey,
      entry.matchedPly.toString,
      identity.eco.getOrElse(""),
      identity.name.getOrElse(""),
      identity.family.map(_.toString).getOrElse(""),
      entry.candidate.lineage.getOrElse(""),
      entry.candidate.frequency.toString,
      entry.candidate.sampleCount.toString,
      f"${entry.candidate.confidence}%.4f"
    ).mkString("\t")

  private def loadResourceRows(path: String): List[Entry] =
    Option(getClass.getClassLoader.getResourceAsStream(path)).toList.flatMap: stream =>
      Using.resource(Source.fromInputStream(stream, "UTF-8")): source =>
        parseRows(source.getLines()).toList

  private def parseRows(lines: IterableOnce[String]): Iterable[Entry] =
    lines.iterator
      .map(_.trim)
      .filter(line => line.nonEmpty && !line.startsWith("#"))
      .dropWhile(_.toLowerCase(Locale.ROOT).startsWith("move_prefix_hash\t"))
      .flatMap(parseLine)
      .toList

  private def parseLine(line: String): Option[Entry] =
    line.split("\t", -1).toList match
      case movePrefixHash :: positionKey :: matchedPlyRaw :: ecoRaw :: nameRaw :: familyRaw :: lineageRaw ::
          frequencyRaw :: sampleCountRaw :: confidenceRaw :: _ =>
        val eco = cleanText(ecoRaw).map(_.toUpperCase(Locale.ROOT))
        val family =
          cleanText(familyRaw).flatMap(OpeningFamily.fromRaw)
            .orElse(eco.flatMap(OpeningFamily.fromEco))
        val identity =
          OpeningIdentity(
            eco = eco,
            name = cleanText(nameRaw),
            family = family
          )
        val frequency = frequencyRaw.trim.toIntOption.getOrElse(0).max(0)
        val sampleCount = sampleCountRaw.trim.toIntOption.getOrElse(frequency).max(frequency).max(0)
        Option.when(
          movePrefixHash.trim.nonEmpty &&
            positionKey.trim.nonEmpty &&
            (identity.eco.nonEmpty || identity.name.nonEmpty || identity.family.nonEmpty) &&
            frequency > 0 &&
            sampleCount > 0
        )(
          Entry(
            movePrefixHash = movePrefixHash.trim,
            positionKey = positionKey.trim,
            matchedPly = matchedPlyRaw.trim.toIntOption.getOrElse(0).max(0),
            candidate = OpeningCandidate(
              identity = identity,
              lineage = cleanText(lineageRaw).map(normalizeLineage),
              frequency = frequency,
              sampleCount = sampleCount,
              confidence = boundedConfidence(confidenceRaw.trim.toDoubleOption.getOrElse(0.0))
            )
          )
        )
      case _ => None

  private[opening] def cleanText(raw: String): Option[String] =
    Option(raw).map(_.trim).filter(_.nonEmpty)

  private[opening] def normalizeLineage(raw: String): String =
    raw.trim.toLowerCase(Locale.ROOT)

  private def boundedConfidence(value: Double): Double =
    math.max(0.0, math.min(1.0, value))
