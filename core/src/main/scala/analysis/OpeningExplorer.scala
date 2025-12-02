package chess
package analysis

import chess.format.pgn.SanStr
import java.sql.{ Connection, DriverManager, ResultSet }

/** SQLite 기반 오프닝 익스플로러.
  * DB 스키마는 scripts/build_opening_db.py에서 생성: positions/moves/games 테이블.
  * 환경변수: OPENING_STATS_DB (기본: opening/masters_stats.db)
  */
object OpeningExplorer:

  final case class TopMove(san: String, uci: String, games: Int, winPct: Option[Double], drawPct: Option[Double])
  final case class TopGame(
      white: String,
      black: String,
      whiteElo: Option[Int],
      blackElo: Option[Int],
      result: String,
      date: Option[String],
      event: Option[String]
  )
  final case class Stats(
      bookPly: Int,
      noveltyPly: Int,
      games: Option[Int],
      winWhite: Option[Double],
      winBlack: Option[Double],
      draw: Option[Double],
      topMoves: List[TopMove],
      topGames: List[TopGame],
      source: String
  )

  private lazy val dbPath = sys.env.getOrElse("OPENING_STATS_DB", "opening/masters_stats.db")
  private lazy val connection: Option[Connection] =
    try
      val conn = DriverManager.getConnection(s"jdbc:sqlite:$dbPath")
      Some(conn)
    catch
      case _: Throwable => None

  def explore(opening: Option[chess.opening.Opening.AtPly], sans: List[SanStr], topGamesLimit: Int = 12, topMovesLimit: Int = 8): Option[Stats] =
    connection.flatMap { conn =>
      val key = sanKey(sans)
      lookupPosition(conn, key, topGamesLimit, topMovesLimit)
        .orElse(opening.map { op =>
          Stats(
            bookPly = op.ply.value,
            noveltyPly = math.min(op.ply.value + 1, sans.length),
            games = None,
            winWhite = None,
            winBlack = None,
            draw = None,
            topMoves = Nil,
            topGames = Nil,
            source = "book"
          )
        })
    }

  private def sanKey(sans: List[SanStr]): String = sans.map(_.value).mkString(" ")

  private def lookupPosition(conn: Connection, sanSeq: String, topGamesLimit: Int, topMovesLimit: Int): Option[Stats] =
    val posSql = "SELECT id, ply, games, win_w, win_b, draw FROM positions WHERE san_seq = ?"
    val posStmt = conn.prepareStatement(posSql)
    posStmt.setString(1, sanSeq)
    val posRs = posStmt.executeQuery()
    if !posRs.next() then None
    else
      val posId = posRs.getInt("id")
      val ply = posRs.getInt("ply")
      val games = Option(posRs.getInt("games")).filter(_ > 0)
      val winW = Option(posRs.getDouble("win_w")).filterNot(_.isNaN)
      val winB = Option(posRs.getDouble("win_b")).filterNot(_.isNaN)
      val drw = Option(posRs.getDouble("draw")).filterNot(_.isNaN)
      posRs.close()
      posStmt.close()

      val moves = lookupMoves(conn, posId, topMovesLimit)
      val gamesList = lookupGames(conn, posId, topGamesLimit)
      Some(
        Stats(
          bookPly = ply,
          noveltyPly = ply + 1,
          games = games,
          winWhite = winW,
          winBlack = winB,
          draw = drw,
          topMoves = moves,
          topGames = gamesList,
          source = "sqlite"
        )
      )

  private def lookupMoves(conn: Connection, posId: Int, limit: Int): List[TopMove] =
    val sql = "SELECT san, uci, games, win_w, win_b, draw FROM moves WHERE position_id = ? ORDER BY games DESC LIMIT ?"
    val stmt = conn.prepareStatement(sql)
    stmt.setInt(1, posId)
    stmt.setInt(2, limit)
    val rs = stmt.executeQuery()
    val buf = scala.collection.mutable.ListBuffer.empty[TopMove]
    while rs.next() do
      val games = rs.getInt("games")
      val win = Option(rs.getDouble("win_w")).filterNot(_.isNaN)
      val draw = Option(rs.getDouble("draw")).filterNot(_.isNaN)
      buf += TopMove(rs.getString("san"), rs.getString("uci"), games, win, draw)
    rs.close(); stmt.close()
    buf.toList

  private def lookupGames(conn: Connection, posId: Int, limit: Int): List[TopGame] =
    val sql = "SELECT white, black, white_elo, black_elo, result, date, event FROM games WHERE position_id = ? LIMIT ?"
    val stmt = conn.prepareStatement(sql)
    stmt.setInt(1, posId)
    stmt.setInt(2, limit)
    val rs = stmt.executeQuery()
    val buf = scala.collection.mutable.ListBuffer.empty[TopGame]
    while rs.next() do
      val whiteElo = Option(rs.getInt("white_elo")).filter(_ > 0)
      val blackElo = Option(rs.getInt("black_elo")).filter(_ > 0)
      buf += TopGame(
        white = rs.getString("white"),
        black = rs.getString("black"),
        whiteElo = whiteElo,
        blackElo = blackElo,
        result = rs.getString("result"),
        date = Option(rs.getString("date")).filter(_.nonEmpty),
        event = Option(rs.getString("event")).filter(_.nonEmpty)
      )
    rs.close(); stmt.close()
    buf.toList

