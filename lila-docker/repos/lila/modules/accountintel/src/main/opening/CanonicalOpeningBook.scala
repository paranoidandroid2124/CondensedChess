package lila.accountintel.opening

import chess.*
import chess.format.Fen
import chess.format.pgn.{ Parser, PgnStr }

import scala.io.Source
import scala.util.Using

import lila.accountintel.*
object CanonicalOpeningBook:

  case class OpeningClassification(
      canonicalName: String,
      family: String,
      bucket: String,
      relation: String,
      ecoCode: Option[String]
  )

  private case class OpeningEntry(eco: String, epd: String, plyCount: Int)
  private case class RawOpeningRow(eco: String, name: String, pgn: String)

  private val resources =
    List(
      "lila/accountintel/openings/a.tsv",
      "lila/accountintel/openings/b.tsv",
      "lila/accountintel/openings/c.tsv",
      "lila/accountintel/openings/d.tsv",
      "lila/accountintel/openings/e.tsv"
    )

  private lazy val rawRows: List[RawOpeningRow] = resources.flatMap(loadResourceRows)

  private lazy val vendorRootNameByEco: Map[String, String] =
    rawRows
      .groupBy(_.eco)
      .view
      .mapValues: rows =>
        rows.minBy(row => (plyCount(row.pgn), row.name.length, row.name)).name.trim
      .toMap

  private lazy val entries: List[OpeningEntry] =
    rawRows.flatMap: row =>
      epdForPgn(row.pgn).map: epd =>
        OpeningEntry(
          eco = row.eco.trim.toUpperCase,
          epd = epd,
          plyCount = plyCount(row.pgn)
        )

  private lazy val entriesByEpd: Map[String, OpeningEntry] =
    entries
      .groupBy(_.epd)
      .view
      .mapValues(_.maxBy(_.plyCount))
      .toMap

  private[accountintel] lazy val missingEcoPerspectiveCodes: Set[String] =
    val allCodes =
      ('A' to 'E').toSet.flatMap(letter => (0 to 99).map(num => f"$letter$num%02d"))
    allCodes -- EcoPerspectiveTable.entries.keySet

  def classify(
      pgn: String,
      subjectColor: Color,
      providerOpeningName: Option[String],
      providerEcoCode: Option[String],
      providerEcoUrl: Option[String]
  ): OpeningClassification =
    classifyFromPgn(pgn)
      .map(_.eco)
      .orElse(providerEcoCode.map(_.trim.toUpperCase).filter(_.nonEmpty))
      .flatMap(code => classificationForEco(code, subjectColor))
      .getOrElse:
        val rawName =
          providerOpeningName
            .map(_.trim)
            .filter(_.nonEmpty)
            .orElse(providerEcoUrl.flatMap(openingFromEcoUrl))
            .orElse(providerEcoCode.map(code => s"ECO ${code.trim.toUpperCase}"))
            .getOrElse("Recent practical structure")
        OpeningClassification(
          canonicalName = rawName,
          family = rawName,
          bucket = s"entered $rawName structures",
          relation = "entered",
          ecoCode = providerEcoCode.map(_.trim.toUpperCase)
        )

  private def classificationForEco(eco: String, subjectColor: Color): Option[OpeningClassification] =
    EcoPerspectiveTable.entries.get(eco).map: mapped =>
      val canonicalName = vendorRootNameByEco.getOrElse(eco, mapped.family)
      val relation =
        mapped.chooserSide match
          case Some(color) if color == subjectColor => "chose"
          case Some(_) => "faced"
          case None => "entered"
      val bucket =
        relation match
          case "chose" => s"chose ${mapped.family}"
          case "faced" => s"faced ${mapped.family}"
          case _ => s"entered ${mapped.family} structures"
      OpeningClassification(
        canonicalName = canonicalName,
        family = mapped.family,
        bucket = bucket,
        relation = relation,
        ecoCode = Some(eco)
      )

  private def classifyFromPgn(pgn: String): Option[OpeningEntry] =
    Parser.mainline(PgnStr(pgn)) match
      case Left(_) => None
      case Right(parsed) =>
        val game = parsed.toGame
        val replay = Replay.makeReplay(game, parsed.moves)
        replay.replay.chronoMoves.zipWithIndex
          .flatMap { case (move, idx) =>
            val ply = game.ply.value + idx + 1
            val epd = epdKey(Fen.write(move.after, Ply(ply).fullMoveNumber).value)
            entriesByEpd.get(epd)
          }
          .lastOption

  private def loadResourceRows(path: String): List[RawOpeningRow] =
    Option(getClass.getClassLoader.getResourceAsStream(path)).toList.flatMap: stream =>
      Using.resource(Source.fromInputStream(stream, "UTF-8")): source =>
        source
          .getLines()
          .drop(1)
          .flatMap(parseRawLine)
          .toList

  private def parseRawLine(line: String): Option[RawOpeningRow] =
    line.split('\t').toList match
      case eco :: name :: pgnParts if eco.nonEmpty && name.nonEmpty && pgnParts.nonEmpty =>
        Some(
          RawOpeningRow(
            eco = eco.trim.toUpperCase,
            name = name.trim,
            pgn = pgnParts.mkString("\t").trim
          )
        )
      case _ => None

  private def epdForPgn(pgn: String): Option[String] =
    Parser.mainline(PgnStr(pgn)) match
      case Left(_) => None
      case Right(parsed) =>
        val game = parsed.toGame
        val replay = Replay.makeReplay(game, parsed.moves)
        replay.replay.chronoMoves.lastOption.map: move =>
          epdKey(Fen.write(move.after, Ply(game.ply.value + parsed.moves.size).fullMoveNumber).value)

  private def plyCount(pgn: String): Int =
    Parser.mainline(PgnStr(pgn)).fold(_ => 0, _.moves.size)

  private def epdKey(fen: String): String = fen.split(' ').take(4).mkString(" ")

  private def openingFromEcoUrl(raw: String): Option[String] =
    raw
      .split("/openings/")
      .lift(1)
      .map(_.takeWhile(_ != '?'))
      .map(_.replace('-', ' '))
      .map(_.replaceAll("\\s+\\d+\\..*$", ""))
      .map(_.trim)
      .filter(_.nonEmpty)
