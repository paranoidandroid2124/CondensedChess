package chess
package analysis

import java.sql.{ Connection, DriverManager, ResultSet, Statement }
import java.util.UUID
import scala.util.Using

object Persistence:
  private val dbPath = "jdbc:sqlite:chess_analysis.db"

  def init(): Unit =
    Using(DriverManager.getConnection(dbPath)) { conn =>
      val stmt = conn.createStatement()
      stmt.execute("""
        CREATE TABLE IF NOT EXISTS games (
          id TEXT PRIMARY KEY,
          pgn TEXT,
          created_at INTEGER
        )
      """)
      stmt.execute("""
        CREATE TABLE IF NOT EXISTS analysis (
          game_id TEXT,
          ply INTEGER,
          fen TEXT,
          eval REAL,
          eval_type TEXT,
          judgement TEXT,
          comment TEXT,
          PRIMARY KEY (game_id, ply),
          FOREIGN KEY(game_id) REFERENCES games(id)
        )
      """)
    }.get

  def saveGame(id: String, pgn: String): Unit =
    Using(DriverManager.getConnection(dbPath)) { conn =>
      val pstmt = conn.prepareStatement("INSERT OR REPLACE INTO games (id, pgn, created_at) VALUES (?, ?, ?)")
      pstmt.setString(1, id)
      pstmt.setString(2, pgn)
      pstmt.setLong(3, System.currentTimeMillis())
      pstmt.executeUpdate()
    }.get

  def loadGame(id: String): Option[String] =
    Using(DriverManager.getConnection(dbPath)) { conn =>
      val pstmt = conn.prepareStatement("SELECT pgn FROM games WHERE id = ?")
      pstmt.setString(1, id)
      val rs = pstmt.executeQuery()
      if rs.next() then Some(rs.getString("pgn")) else None
    }.get

  def saveAnalysis(gameId: String, ply: Int, fen: String, eval: Double, evalType: String, judgement: String, comment: Option[String]): Unit =
    Using(DriverManager.getConnection(dbPath)) { conn =>
      val pstmt = conn.prepareStatement(
        "INSERT OR REPLACE INTO analysis (game_id, ply, fen, eval, eval_type, judgement, comment) VALUES (?, ?, ?, ?, ?, ?, ?)"
      )
      pstmt.setString(1, gameId)
      pstmt.setInt(2, ply)
      pstmt.setString(3, fen)
      pstmt.setDouble(4, eval)
      pstmt.setString(5, evalType)
      pstmt.setString(6, judgement)
      pstmt.setString(7, comment.orNull)
      pstmt.executeUpdate()
    }.get

  def loadAnalysis(gameId: String): Map[Int, (Double, String, String, Option[String])] =
    Using(DriverManager.getConnection(dbPath)) { conn =>
      val pstmt = conn.prepareStatement("SELECT ply, eval, eval_type, judgement, comment FROM analysis WHERE game_id = ?")
      pstmt.setString(1, gameId)
      val rs = pstmt.executeQuery()
      val result = scala.collection.mutable.Map.empty[Int, (Double, String, String, Option[String])]
      while rs.next() do
        result += rs.getInt("ply") -> (
          rs.getDouble("eval"),
          rs.getString("eval_type"),
          rs.getString("judgement"),
          Option(rs.getString("comment"))
        )
      result.toMap
    }.get
