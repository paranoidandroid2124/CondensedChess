package lila.chessjudgment.analysis.opening

import java.util.Locale

import scala.io.Source
import scala.util.Using

import lila.chessjudgment.model.judgment.*

final class OpeningThemePriorIndex private (
    entries: List[OpeningThemePrior]
):

  private val byLineage: Map[String, OpeningThemePrior] =
    entries.flatMap(entry => entry.lineage.map(_ -> entry)).toMap

  private val byFamily: Map[OpeningFamily, OpeningThemePrior] =
    entries.flatMap(entry => entry.family.map(_ -> entry)).toMap

  def allEntries: List[OpeningThemePrior] =
    entries

  def priorFor(lineage: Option[String], family: Option[OpeningFamily]): Option[OpeningThemePrior] =
    lineage
      .map(OpeningRecognitionIndex.normalizeLineage)
      .flatMap(byLineage.get)
      .orElse(family.flatMap(byFamily.get))

object OpeningThemePriorIndex:

  val TsvHeader: String =
    "lineage\tfamily\tthemes\ttypical_pawn_structures\tcenter_breaks\tdevelopment_priorities\tgambit_compensation\tstrategic_plan_priors"

  private val ResourcePath = "lila/chessjudgment/openings/opening_theme_prior_index.tsv"

  lazy val default: OpeningThemePriorIndex =
    fromEntries(loadResourceRows(ResourcePath))

  val empty: OpeningThemePriorIndex =
    fromEntries(Nil)

  def fromEntries(entries: IterableOnce[OpeningThemePrior]): OpeningThemePriorIndex =
    OpeningThemePriorIndex(entries.iterator.toList)

  def fromTsvLines(lines: IterableOnce[String]): OpeningThemePriorIndex =
    fromEntries(parseRows(lines))

  def tsvRow(entry: OpeningThemePrior): String =
    List(
      entry.lineage.getOrElse(""),
      entry.family.map(_.toString).getOrElse(""),
      entry.themes.map(_.toString).mkString("|"),
      entry.typicalPawnStructures.mkString("|"),
      entry.centerBreaks.mkString("|"),
      entry.developmentPriorities.mkString("|"),
      entry.gambitCompensation.toString,
      entry.strategicPlanPriors.mkString("|")
    ).mkString("\t")

  private def loadResourceRows(path: String): List[OpeningThemePrior] =
    Option(getClass.getClassLoader.getResourceAsStream(path)).toList.flatMap: stream =>
      Using.resource(Source.fromInputStream(stream, "UTF-8")): source =>
        parseRows(source.getLines()).toList

  private def parseRows(lines: IterableOnce[String]): Iterable[OpeningThemePrior] =
    lines.iterator
      .map(_.trim)
      .filter(line => line.nonEmpty && !line.startsWith("#"))
      .dropWhile(_.toLowerCase(Locale.ROOT).startsWith("lineage\t"))
      .flatMap(parseLine)
      .toList

  private def parseLine(line: String): Option[OpeningThemePrior] =
    line.split("\t", -1).toList match
      case lineageRaw :: familyRaw :: themesRaw :: structuresRaw :: breaksRaw :: developmentRaw ::
          gambitRaw :: plansRaw :: _ =>
        val themes = splitList(themesRaw).flatMap(theme)
        val lineage = OpeningRecognitionIndex.cleanText(lineageRaw).map(OpeningRecognitionIndex.normalizeLineage)
        val family = OpeningRecognitionIndex.cleanText(familyRaw).flatMap(OpeningFamily.fromRaw)
        Option.when(lineage.nonEmpty || family.nonEmpty)(
          OpeningThemePrior(
            lineage = lineage,
            family = family,
            themes = themes.distinct,
            typicalPawnStructures = splitList(structuresRaw),
            centerBreaks = splitList(breaksRaw),
            developmentPriorities = splitList(developmentRaw),
            gambitCompensation = gambitRaw.trim.equalsIgnoreCase("true"),
            strategicPlanPriors = splitList(plansRaw)
          )
        )
      case _ => None

  private def splitList(raw: String): List[String] =
    Option(raw).getOrElse("").split("[|]").toList.map(_.trim).filter(_.nonEmpty)

  private def theme(raw: String): Option[OpeningTheme] =
    val normalized = raw.trim
    OpeningTheme.values.find(_.toString == normalized)
      .orElse:
        val key = normalized.toLowerCase(Locale.ROOT).replace("-", "").replace("_", "")
        OpeningTheme.values.find(_.toString.toLowerCase(Locale.ROOT) == key)
