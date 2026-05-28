package lila.commentary.tools.opening

import chess.format.Fen
import chess.format.pgn.{ Parser, PgnStr }
import lila.commentary.analysis.OpeningNameLookup

object OpeningPoolAudit:

  final case class RawRow(
      lineNo: Int,
      eco: String,
      name: String,
      pgn: String,
      explicitUci: Option[String],
      explicitEpd: Option[String]
  )

  final case class AuditedRow(
      lineNo: Int,
      eco: String,
      name: String,
      pgn: String,
      endpointKey: Option[String],
      uciPlay: List[String],
      plyCount: Int,
      issues: List[String]
  ):
    def stableId: String = s"$eco:$name:${endpointKey.getOrElse("missing-endpoint")}"
    def legacyStableId: String = s"$lineNo:$eco:$name"

  final case class DuplicateEndpointGroup(endpointKey: String, rows: List[AuditedRow])

  private final case class ParsedPgn(endpointKey: String, uciPlay: List[String], plyCount: Int)

  def auditLines(lines: IterableOnce[String]): List[AuditedRow] =
    parseRawRows(lines).map(auditRow)

  def duplicateEndpointGroups(rows: Iterable[AuditedRow]): List[DuplicateEndpointGroup] =
    rows
      .flatMap(row => row.endpointKey.map(_ -> row))
      .groupBy(_._1)
      .toList
      .flatMap { case (endpoint, keyedRows) =>
        val rows = keyedRows.map(_._2).toList.sortBy(_.lineNo)
        Option.when(rows.sizeCompare(1) > 0)(DuplicateEndpointGroup(endpoint, rows))
      }
      .sortBy(group => (group.rows.head.lineNo, group.endpointKey))

  private def parseRawRows(lines: IterableOnce[String]): List[RawRow] =
    lines.iterator.zipWithIndex.toList.flatMap { case (rawLine, zeroIdx) =>
      val lineNo = zeroIdx + 1
      val line = rawLine.trim.stripPrefix("\uFEFF")
      if line.isEmpty || line.startsWith("#") || line.toLowerCase.startsWith("eco\tname\t") then None
      else
        line.split('\t').toList match
          case eco :: name :: pgn :: uci :: epd :: _ if eco.nonEmpty && name.nonEmpty =>
            Some(RawRow(lineNo, eco.trim.toUpperCase, name.trim, pgn.trim, Some(uci.trim), Some(epd.trim)))
          case eco :: name :: pgnParts if eco.nonEmpty && name.nonEmpty && pgnParts.nonEmpty =>
            Some(RawRow(lineNo, eco.trim.toUpperCase, name.trim, pgnParts.mkString("\t").trim, None, None))
          case _ => None
    }

  private def auditRow(row: RawRow): AuditedRow =
    val explicitEndpoint =
      row.explicitEpd
        .filter(_.nonEmpty)
        .flatMap(OpeningNameLookup.normalizedKey)
    val parsed = parsePgn(row.pgn)
    val sourcePlyCount = pgnTokenPlyCount(row.pgn)
    val endpoint = explicitEndpoint.orElse(parsed.toOption.map(_.endpointKey))
    val issues =
      List(
        Option.when(row.explicitEpd.exists(_.nonEmpty) && explicitEndpoint.isEmpty)("invalid-epd"),
        parsed.left.toOption.map(error => s"invalid-pgn:$error"),
        parsed.toOption
          .filter(parsedPgn => sourcePlyCount > parsedPgn.plyCount)
          .map(parsedPgn => s"invalid-pgn:parsed ${parsedPgn.plyCount} of $sourcePlyCount plies"),
        Option.when(
          explicitEndpoint.isDefined &&
            parsed.toOption.exists(parsedPgn => explicitEndpoint.exists(_ != parsedPgn.endpointKey))
        )("epd-pgn-mismatch"),
        Option.when(endpoint.isEmpty)("missing-endpoint")
      ).flatten

    AuditedRow(
      lineNo = row.lineNo,
      eco = row.eco,
      name = row.name,
      pgn = row.pgn,
      endpointKey = endpoint,
      uciPlay = parsed.toOption.map(_.uciPlay).getOrElse(Nil),
      plyCount = parsed.toOption.map(_.plyCount).getOrElse(0),
      issues = issues
    )

  private def parsePgn(pgn: String): Either[String, ParsedPgn] =
    Parser.mainline(PgnStr(pgn)) match
      case Left(error) => Left(error.toString)
      case Right(parsed) =>
        val game = parsed.toGame
        val replay = chess.Replay.makeReplay(game, parsed.moves).replay.chronoMoves
        val uci = replay.collect {
          case move: chess.Move => move.toUci.uci
          case drop: chess.Drop => drop.toUci.uci
        }
        val endpoint =
          replay.lastOption
          .flatMap {
            case move: chess.Move => OpeningNameLookup.normalizedKey(Fen.write(move.after).value)
            case drop: chess.Drop => OpeningNameLookup.normalizedKey(Fen.write(drop.after).value)
          }
        endpoint
          .toRight("missing-final-position")
          .map(endpoint => ParsedPgn(endpoint, uci, parsed.moves.size))

  private def pgnTokenPlyCount(pgn: String): Int =
    pgn
      .replaceAll("""\{[^}]*\}""", " ")
      .replaceAll("""\([^)]*\)""", " ")
      .split("\\s+")
      .toList
      .map(_.trim)
      .filter(_.nonEmpty)
      .filterNot(token => token.matches("""\d+\.(\.\.)?"""))
      .filterNot(token => Set("1-0", "0-1", "1/2-1/2", "*").contains(token))
      .size
