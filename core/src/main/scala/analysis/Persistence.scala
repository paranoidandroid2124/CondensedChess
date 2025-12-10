package chess
package analysis

import java.sql.DriverManager
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
          comment TEXT,
          PRIMARY KEY (game_id, ply),
          FOREIGN KEY(game_id) REFERENCES games(id)
        )
      """)
      stmt.execute("""
        CREATE TABLE IF NOT EXISTS analysis_json (
          game_id TEXT PRIMARY KEY,
          json TEXT,
          updated_at INTEGER,
          FOREIGN KEY(game_id) REFERENCES games(id)
        )
      """)
      stmt.execute("""
        CREATE TABLE IF NOT EXISTS analysis_cache (
          key TEXT PRIMARY KEY,
          fen TEXT,
          json TEXT,
          depth INTEGER,
          created_at INTEGER
        )
      """)
      // Create index on timestamp for pseudo-LRU/Cleanup
      stmt.execute("CREATE INDEX IF NOT EXISTS idx_cache_created ON analysis_cache(created_at)")
    }.get

  // ... existing methods ...

  def getCachedExperiment(key: String): Option[String] =
    Using(DriverManager.getConnection(dbPath)) { conn =>
      val pstmt = conn.prepareStatement("SELECT json FROM analysis_cache WHERE key = ?")
      pstmt.setString(1, key)
      val rs = pstmt.executeQuery()
      if rs.next() then Some(rs.getString("json")) else None
    }.toOption.flatten

  def saveCachedExperiment(key: String, fen: String, depth: Int, json: String): Unit =
    // Fire and forget (Try wrapping) to not block critical path on DB error
    scala.util.Try {
      Using(DriverManager.getConnection(dbPath)) { conn =>
        val pstmt = conn.prepareStatement(
          "INSERT OR REPLACE INTO analysis_cache (key, fen, json, depth, created_at) VALUES (?, ?, ?, ?, ?)"
        )
        pstmt.setString(1, key)
        pstmt.setString(2, fen)
        pstmt.setString(3, json)
        pstmt.setInt(4, depth)
        pstmt.setLong(5, System.currentTimeMillis())
        pstmt.executeUpdate()
      }
    }

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

  def saveAnalysisJson(gameId: String, json: String): Unit =
    Using(DriverManager.getConnection(dbPath)) { conn =>
      val pstmt = conn.prepareStatement("INSERT OR REPLACE INTO analysis_json (game_id, json, updated_at) VALUES (?, ?, ?)")
      pstmt.setString(1, gameId)
      pstmt.setString(2, json)
      pstmt.setLong(3, System.currentTimeMillis())
      pstmt.executeUpdate()
    }.get

  def loadAnalysisJson(gameId: String): Option[String] =
    Using(DriverManager.getConnection(dbPath)) { conn =>
      val pstmt = conn.prepareStatement("SELECT json FROM analysis_json WHERE game_id = ?")
      pstmt.setString(1, gameId)
      val rs = pstmt.executeQuery()
      if rs.next() then Some(rs.getString("json")) else None
    }.get

  // Phase 9.5 Hardening: Atomic File Write
  def saveStudyAtomic(studyId: String, json: String): Unit =
    import java.nio.file.{Files, Paths, StandardCopyOption, StandardOpenOption}
    import java.nio.charset.StandardCharsets

    val studiesDir = Paths.get("data/studies")
    if !Files.exists(studiesDir) then Files.createDirectories(studiesDir)

    val targetPath = studiesDir.resolve(s"$studyId.json")
    val tempPath = studiesDir.resolve(s"$studyId.json.tmp")

    try
      Files.write(tempPath, json.getBytes(StandardCharsets.UTF_8), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)
      Files.move(tempPath, targetPath, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING)
    catch
      case e: Throwable =>
        // Cleanup temp if exists
        try Files.deleteIfExists(tempPath) catch case _: Throwable => ()
        throw e
