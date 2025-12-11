package chess
package analysis

import chess.format.pgn.SanStr
import java.sql.{ Connection, DriverManager }
import java.nio.file.{ Paths, Path }

/** SQLite 기반 오프닝 익스플로러.
  * DB 스키마는 scripts/build_opening_db.py에서 생성: positions/moves/games 테이블.
  * 환경변수: OPENING_STATS_DB (기본: opening/masters_stats.db)
  */
object OpeningExplorer:

  private val cache = new java.util.LinkedHashMap[String, Stats](256, 0.75f, true):
    override def removeEldestEntry(eldest: java.util.Map.Entry[String, Stats]): Boolean = this.size() > 512

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
      source: String,
      minYear: Option[Int],
      maxYear: Option[Int],
      yearBuckets: Map[String, Int]
  )

  private def defaultDbCandidates: List[String] =
    val filename = "masters_stats.db"
    val relativePath = s"opening/$filename"
    val cwd = Paths.get("").toAbsolutePath
    
    // 1. Check direct relative path from CWD
    println(s"[opening] DEBUG: CWD = $cwd")
    val candidates = scala.collection.mutable.ListBuffer(cwd.resolve(relativePath))

    // 2. Traverse up from CWD (up to 5 levels) to find project root containing "opening" folder
    var current = cwd
    for (_ <- 1 to 5) {
      if (current.getParent != null) {
        current = current.getParent
        candidates += current.resolve(relativePath)
      }
    }
    
    // 3. Fallback to classpath/codebase locations (existing logic)
    val codeBase: List[Path] =
      Option(getClass.getProtectionDomain)
        .flatMap(pd => Option(pd.getCodeSource))
        .flatMap(cs => Option(cs.getLocation))
        .flatMap(url => scala.util.Try(Paths.get(url.toURI)).toOption)
        .toList
        
    val codeBaseRoots = codeBase.flatMap(p => List(p, p.getParent)).filter(_ != null).distinct
    candidates ++= codeBaseRoots.map(_.resolve(relativePath))

    val finalCandidates = candidates.map(_.normalize.toString).distinct.toList
    println(s"[opening] DEBUG: Candidates = $finalCandidates")
    finalCandidates

  private lazy val dbPaths: List[String] =
    EnvLoader.get("OPENING_STATS_DB_LIST")
      .filter(_.nonEmpty)
      .map(_.split("[,;]").toList.map(_.trim).filter(_.nonEmpty))
      .orElse(EnvLoader.get("OPENING_STATS_DB").map(p => List(p.trim)).filter(_.nonEmpty))
      .getOrElse(defaultDbCandidates)

  private lazy val connections: List[(String, Connection)] =
    dbPaths.flatMap { path =>
      try
        val conn = DriverManager.getConnection(s"jdbc:sqlite:$path")
        Some(path -> conn)
      catch
        case e: Throwable =>
          System.err.println(s"[opening] failed to open db at $path")
          e.printStackTrace()
          None
    }

  def explore(opening: Option[chess.opening.Opening.AtPly], sans: List[SanStr], topGamesLimit: Int = 12, topMovesLimit: Int = 8, gamesOffset: Int = 0): Option[Stats] =
    val key = fenKey(sans).getOrElse(sanKey(sans))
    Option(cache.get(key)).orElse {
      val found = connections.view.flatMap { case (path, conn) =>
        lookupPosition(conn, key, topGamesLimit, topMovesLimit, gamesOffset, path)
      }.headOption
      val fallbackBook = opening.map { op =>
        Stats(
          bookPly = op.ply.value,
          noveltyPly = math.min(op.ply.value + 1, sans.length),
          games = None,
          winWhite = None,
          winBlack = None,
          draw = None,
          topMoves = Nil,
          topGames = Nil,
          source = "book",
          minYear = None,
          maxYear = None,
          yearBuckets = Map.empty
        )
      }
      found.orElse(fallbackBook).map { stats =>
        cache.put(key, stats)
        stats
      }
    }

  private def sanKey(sans: List[SanStr]): String = sans.map(_.value).mkString(" ")

  private def fenKey(sans: List[SanStr]): Option[String] =
    val game = chess.Game(chess.variant.Standard)
    sans
      .foldLeft[Either[chess.ErrorStr, chess.Game]](Right(game)) { (eg, san) =>
        eg.flatMap(g => g.play(san).map { case (next, _) => next })
      }
      .toOption
      // Use opening FEN (board/turn/castling/ep) to merge transpositions; drop ep when no legal ep capture.
      .map { g =>
        val parts = format.Fen.writeOpening(g.position).value.split(" ")
        val epSafe =
          if parts.length >= 4 then
            val ep = parts(3)
            val hasEp = g.position.legalMoves.exists(_.enpassant)
            val epField = if hasEp then ep else "-"
            (parts.take(3) :+ epField).mkString(" ")
          else format.Fen.writeOpening(g.position).value
        epSafe
      }

  private def lookupPosition(conn: Connection, key: String, topGamesLimit: Int, topMovesLimit: Int, gamesOffset: Int, sourceLabel: String): Option[Stats] =
    val posSql = "SELECT id, ply, games, win_w, win_b, draw, min_year, max_year, bucket_pre2012, bucket_2012_2017, bucket_2018_2019, bucket_2020_plus FROM positions WHERE fen = ?"
    val posStmt = conn.prepareStatement(posSql)
    posStmt.setString(1, key)
    val posRs = posStmt.executeQuery()
    if !posRs.next() then None
    else
      val posId = posRs.getInt("id")
      val ply = posRs.getInt("ply")
      val games = Option(posRs.getInt("games")).filter(_ > 0)
      val winW = Option(posRs.getDouble("win_w")).filterNot(_.isNaN)
      val winB = Option(posRs.getDouble("win_b")).filterNot(_.isNaN)
      val drw = Option(posRs.getDouble("draw")).filterNot(_.isNaN)
      val minYear = Option(posRs.getInt("min_year")).filter(_ > 0)
      val maxYear = Option(posRs.getInt("max_year")).filter(_ > 0)
      val yearBuckets = Map(
        "pre2012" -> posRs.getInt("bucket_pre2012"),
        "2012_2017" -> posRs.getInt("bucket_2012_2017"),
        "2018_2019" -> posRs.getInt("bucket_2018_2019"),
        "2020_plus" -> posRs.getInt("bucket_2020_plus")
      ).filter(_._2 > 0)
      posRs.close()
      posStmt.close()

      val moves = lookupMoves(conn, posId, topMovesLimit)
      val gamesList = lookupGames(conn, posId, topGamesLimit, gamesOffset)
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
          source = sourceLabel,
          minYear = minYear,
          maxYear = maxYear,
          yearBuckets = yearBuckets
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

  private def lookupGames(conn: Connection, posId: Int, limit: Int, offset: Int): List[TopGame] =
    val sql = "SELECT white, black, white_elo, black_elo, result, date, event FROM games WHERE position_id = ? LIMIT ? OFFSET ?"
    val stmt = conn.prepareStatement(sql)
    stmt.setInt(1, posId)
    stmt.setInt(2, limit)
    stmt.setInt(3, offset)
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
