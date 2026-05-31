package lila.commentary.analysis

import chess.format.Fen
import chess.format.pgn.{ Parser, PgnStr }
import lila.commentary.model.OpeningReference

import scala.io.Source
import scala.util.Using

private[commentary] final class OpeningNameLookup private (
    canonicalRowsByEpd: Map[String, OpeningNameLookup.Row],
    rowsByEpd: Map[String, List[OpeningNameLookup.Row]]
):

  def normalizedKey(fenOrEpd: String): Option[String] =
    OpeningNameLookup.normalizedKey(fenOrEpd)

  def lookup(fenOrEpd: String): Option[OpeningReference] =
    normalizedKey(fenOrEpd)
      .flatMap(canonicalRowsByEpd.get)
      .map(OpeningNameLookup.reference)

  def lookupAll(fenOrEpd: String): List[OpeningReference] =
    normalizedKey(fenOrEpd)
      .flatMap(rowsByEpd.get)
      .getOrElse(Nil)
      .map(OpeningNameLookup.reference)
      .distinctBy(ref => (ref.eco, ref.name))

private[commentary] object OpeningNameLookup:

  private[analysis] final case class Row(
      eco: String,
      name: String,
      pgn: String,
      uci: String,
      epd: String
  )

  private val ResourcePaths = List(
    "lila/commentary/openings/openings.tsv",
    "lila/commentary/openings/a.tsv",
    "lila/commentary/openings/b.tsv",
    "lila/commentary/openings/c.tsv",
    "lila/commentary/openings/d.tsv",
    "lila/commentary/openings/e.tsv"
  )

  lazy val default: OpeningNameLookup =
    fromRows(ResourcePaths.flatMap(loadResourceRows))

  def fromRows(rows: Iterable[Row]): OpeningNameLookup =
    val rowsByEpd =
      rows
        .filter(row => row.eco.nonEmpty && row.name.nonEmpty && row.epd.nonEmpty)
        .groupBy(_.epd)
        .view
        .mapValues(_.toList.sortBy(row => rowPriority(row)).reverse)
        .toMap
    val canonicalRowsByEpd =
      rowsByEpd.view.mapValues(_.maxBy(rowPriority)).toMap
    OpeningNameLookup(canonicalRowsByEpd, rowsByEpd)

  def fromTsvLines(lines: IterableOnce[String]): OpeningNameLookup =
    fromRows(parseRows(lines))

  def normalizedKey(fenOrEpd: String): Option[String] =
    val parts = Option(fenOrEpd).map(_.trim).filter(_.nonEmpty).map(_.split("\\s+").toList).getOrElse(Nil)
    val fullFen =
      parts match
        case board :: side :: castling :: ep :: Nil =>
          Some(s"$board $side $castling $ep 0 1")
        case board :: side :: castling :: ep :: halfMove :: fullMove :: Nil
            if halfMove.toIntOption.exists(_ >= 0) && fullMove.toIntOption.exists(_ >= 1) =>
          Some(s"$board $side $castling $ep $halfMove $fullMove")
        case _ => None

    fullFen.flatMap { fen =>
      Fen.read(chess.variant.Standard, Fen.Full(fen)).flatMap { position =>
        val writtenParts = Fen.write(position).value.split("\\s+").toList
        writtenParts match
          case board :: side :: castling :: ep :: _ =>
            val normalizedEp = legalEpField(board, side, ep)
            Option.when(hasExactlyOneKingEach(board))(s"$board $side $castling $normalizedEp")
          case _ => None
      }
    }

  def explorerFen(fenOrEpd: String): Option[String] =
    normalizedKey(fenOrEpd).map(key => s"$key 0 1")

  private def reference(row: Row): OpeningReference =
    OpeningReference(
      eco = Some(row.eco),
      name = Some(row.name),
      totalGames = 0,
      topMoves = Nil,
      sampleGames = Nil
    )

  private def loadResourceRows(path: String): List[Row] =
    Option(getClass.getClassLoader.getResourceAsStream(path)).toList.flatMap: stream =>
      Using.resource(Source.fromInputStream(stream, "UTF-8")): source =>
        parseRows(source.getLines()).toList

  private def parseRows(lines: IterableOnce[String]): Iterable[Row] =
    lines.iterator
      .map(_.trim)
      .filter(line => line.nonEmpty && !line.startsWith("#"))
      .dropWhile(_.toLowerCase.startsWith("eco\tname\t"))
      .flatMap(parseLine)
      .toList

  private def parseLine(line: String): Option[Row] =
    line.split('\t').toList match
      case eco :: name :: pgn :: uci :: epd :: _ if eco.nonEmpty && name.nonEmpty =>
        normalizedKey(epd).map(key =>
          Row(
            eco = eco.trim.toUpperCase,
            name = name.trim,
            pgn = pgn.trim,
            uci = uci.trim,
            epd = key
          )
        )
      case eco :: name :: pgnParts if eco.nonEmpty && name.nonEmpty && pgnParts.nonEmpty =>
        val pgn = pgnParts.mkString("\t").trim
        epdForPgn(pgn).map(key =>
          Row(
            eco = eco.trim.toUpperCase,
            name = name.trim,
            pgn = pgn,
            uci = "",
            epd = key
          )
        )
      case _ => None

  private def epdForPgn(pgn: String): Option[String] =
    Parser.mainline(PgnStr(pgn)) match
      case Left(_) => None
      case Right(parsed) =>
        val game = parsed.toGame
        val replay = chess.Replay.makeReplay(game, parsed.moves)
        replay.replay.chronoMoves.lastOption.flatMap: move =>
          normalizedKey(Fen.write(move.after).value)

  private def plyCount(pgn: String): Int =
    Parser.mainline(PgnStr(pgn)).fold(_ => 0, _.moves.size)

  private def rowPriority(row: Row): (Int, Int, String) =
    (plyCount(row.pgn), row.name.length * -1, row.name)

  private def legalEpField(board: String, side: String, ep: String): String =
    if ep == "-" then "-"
    else
      val epPattern = """^([a-h])([1-8])$""".r
      ep match
        case epPattern(fileRaw, rankRaw) =>
          val file = fileRaw.head - 'a'
          val rank = rankRaw.toInt
          val pieces = boardPieces(board)
          def pieceAt(f: Int, r: Int): Option[Char] =
            pieces.get((f, r))
          def targetEmpty = pieceAt(file, rank).isEmpty
          def adjacentPawn(piece: Char, pawnRank: Int): Boolean =
            List(file - 1, file + 1).exists(f => f >= 0 && f <= 7 && pieceAt(f, pawnRank).contains(piece))
          val legal =
            side match
              case "w" if rank == 6 =>
                targetEmpty && pieceAt(file, 5).contains('p') && adjacentPawn('P', 5)
              case "b" if rank == 3 =>
                targetEmpty && pieceAt(file, 4).contains('P') && adjacentPawn('p', 4)
              case _ => false
          if legal then ep else "-"
        case _ => "-"

  private def hasExactlyOneKingEach(board: String): Boolean =
    board.count(_ == 'K') == 1 && board.count(_ == 'k') == 1

  private def boardPieces(board: String): Map[(Int, Int), Char] =
    val rows = board.split('/').toList
    if rows.size != 8 then Map.empty
    else
      rows.zipWithIndex.flatMap { case (row, rowIdx) =>
        val rank = 8 - rowIdx
        var file = 0
        row.toList.flatMap { ch =>
          if ch.isDigit then
            file += ch.asDigit
            None
          else
            val square = (file, rank) -> ch
            file += 1
            Some(square)
        }
      }.toMap
