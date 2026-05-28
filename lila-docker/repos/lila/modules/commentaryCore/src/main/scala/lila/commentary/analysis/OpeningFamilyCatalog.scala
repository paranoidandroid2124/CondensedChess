package lila.commentary.analysis

import scala.io.Source
import scala.util.Using

private[commentary] final class OpeningFamilyCatalog private (
    familiesByKey: Map[String, OpeningFamilyCatalog.Family]
):

  def family(wireKey: String): Option[OpeningFamilyCatalog.Family] =
    familiesByKey.get(OpeningFamilyCatalog.normalizeKey(wireKey))

  def openingMatchesFamily(opening: String, wireKey: String): Boolean =
    family(wireKey).exists { family =>
      family.aliases.exists(alias => OpeningFamilyCatalog.phraseMatchesAlias(opening, alias))
    }

  def targetAllowed(wireKey: String, targetSquare: String): Boolean =
    family(wireKey).exists(_.targetSquares.contains(OpeningFamilyCatalog.normalizeKey(targetSquare)))

private[commentary] object OpeningFamilyCatalog:

  final case class Family(
      wireKey: String,
      displayName: String,
      structureLabel: String,
      aliases: List[String],
      targetSquares: Set[String]
  )

  private val ResourcePath = "lila/commentary/openings/opening_families.tsv"

  lazy val default: OpeningFamilyCatalog =
    fromRows(loadResourceRows(ResourcePath))

  def fromTsvLines(lines: IterableOnce[String]): OpeningFamilyCatalog =
    fromRows(parseRows(lines))

  private def fromRows(rows: Iterable[Family]): OpeningFamilyCatalog =
    OpeningFamilyCatalog(rows.map(row => row.wireKey -> row).toMap)

  private def loadResourceRows(path: String): List[Family] =
    Option(getClass.getClassLoader.getResourceAsStream(path)).toList.flatMap: stream =>
      Using.resource(Source.fromInputStream(stream, "UTF-8")): source =>
        parseRows(source.getLines()).toList

  private def parseRows(lines: IterableOnce[String]): Iterable[Family] =
    lines.iterator
      .map(_.trim)
      .filter(line => line.nonEmpty && !line.startsWith("#"))
      .dropWhile(_.toLowerCase.startsWith("wire_key\t"))
      .flatMap(parseLine)
      .toList

  private def parseLine(line: String): Option[Family] =
    line.split("\t", -1).toList match
      case wireKey :: displayName :: structureLabel :: aliasesRaw :: targetsRaw :: _ =>
        val key = normalizeKey(wireKey)
        val aliases =
          (splitList(aliasesRaw) ++ List(displayName, structureLabel))
            .map(_.trim.toLowerCase)
            .filter(_.nonEmpty)
            .distinct
        val targets = splitList(targetsRaw).map(normalizeKey).filter(validSquare).toSet
        Option.when(key.nonEmpty && displayName.trim.nonEmpty && structureLabel.trim.nonEmpty && aliases.nonEmpty)(
          Family(
            wireKey = key,
            displayName = displayName.trim,
            structureLabel = structureLabel.trim,
            aliases = aliases,
            targetSquares = targets
          )
        )
      case _ => None

  private def splitList(raw: String): List[String] =
    Option(raw).getOrElse("").split("[|]").toList.map(_.trim).filter(_.nonEmpty)

  private[analysis] def normalizeKey(raw: String): String =
    Option(raw).getOrElse("").trim.toLowerCase

  private def validSquare(square: String): Boolean =
    square.matches("""[a-h][1-8]""")

  private def phraseMatchesAlias(rawPhrase: String, rawAlias: String): Boolean =
    val phraseWords = normalizedWords(rawPhrase)
    val aliasWords = normalizedWords(rawAlias)
    aliasWords.nonEmpty &&
      phraseWords.size >= aliasWords.size &&
      phraseWords.take(aliasWords.size) == aliasWords

  private def normalizedWords(raw: String): List[String] =
    Option(raw).getOrElse("").toLowerCase.replaceAll("""[^a-z0-9]+""", " ").trim
      .split("\\s+")
      .toList
      .filter(_.nonEmpty)
