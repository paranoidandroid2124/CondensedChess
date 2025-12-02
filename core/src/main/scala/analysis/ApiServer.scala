package chess
package analysis

import com.sun.net.httpserver.{ HttpExchange, HttpServer }
import chess.format.{ Fen, Uci }

import java.net.InetSocketAddress
import java.nio.charset.StandardCharsets
import java.util.UUID
import java.util.concurrent.{ ConcurrentHashMap, Executors }
import ujson.*

/** 간단한 HTTP API 래퍼.
  *
  * - POST /analyze (body=PGN) → {"jobId": "..."}
  * - GET  /result/:id        → pending이면 202 {"status":"pending"}, 완료 시 Review JSON 본문
  *
  * 환경변수:
  * - PORT (기본 8080)
  * - BIND (기본 0.0.0.0)
  * - ANALYZE_* (엔진 깊이/시간, AnalyzePgn.EngineConfig.fromEnv 참고)
  */
object ApiServer:

  private case class Job(status: String, result: Option[String], error: Option[String], output: Option[AnalyzePgn.Output], createdAt: Long)

  private val jobs = new ConcurrentHashMap[String, Job]()
  private val executor = Executors.newFixedThreadPool(math.max(2, Runtime.getRuntime.availableProcessors()))

  def main(args: Array[String]): Unit =
    val port = sys.env.get("PORT").flatMap(_.toIntOption).filter(_ > 0).getOrElse(8080)
    val bindHost = sys.env.getOrElse("BIND", "0.0.0.0")
    val config = AnalyzePgn.EngineConfig.fromEnv()
    val llmRequestedPlys: Set[Int] =
      sys.env
        .get("ANALYZE_FORCE_CRITICAL_PLYS")
        .map(_.split(",").toList.flatMap(_.trim.toIntOption).toSet)
        .getOrElse(Set.empty)

    val server = HttpServer.create(new InetSocketAddress(bindHost, port), 0)
    server.createContext("/analyze", (exchange: HttpExchange) => handleAnalyze(exchange, config, llmRequestedPlys))
    server.createContext("/result", (exchange: HttpExchange) => handleResult(exchange))
    server.createContext("/opening/lookup", (exchange: HttpExchange) => handleOpeningLookup(exchange))
    server.createContext("/analysis/branch", (exchange: HttpExchange) => handleAddBranch(exchange))
    server.setExecutor(executor)

    println(s"[api] listening on $bindHost:$port (shallow=${config.shallowDepth}/deep=${config.deepDepth}, multipv=${config.maxMultiPv})")
    server.start()

    // JVM이 바로 종료되지 않도록 유지
    Runtime.getRuntime.addShutdownHook(new Thread(() => {
      server.stop(0)
      executor.shutdown()
      println("[api] stopped")
    }))
    // block main thread
    while true do Thread.sleep(3600_000)

  private def handleAnalyze(exchange: HttpExchange, config: AnalyzePgn.EngineConfig, llmRequestedPlys: Set[Int]): Unit =
    if exchange.getRequestMethod.equalsIgnoreCase("OPTIONS") then
      respond(exchange, 200, """{"ok":true}""", corsOnly = true)
      return

    if !exchange.getRequestMethod.equalsIgnoreCase("POST") then
      respond(exchange, 405, """{"error":"method_not_allowed"}""")
      return

    val body = new String(exchange.getRequestBody.readAllBytes(), StandardCharsets.UTF_8).trim
    if body.isEmpty then
      respond(exchange, 400, """{"error":"empty_body"}""")
      return

    val (pgnText, llmFromBody) =
      if body.startsWith("{") then
        try
          val json = ujson.read(body)
          val pgnStr = json.obj.get("pgn").map(_.str).getOrElse("")
          val llmList = json.obj
            .get("llmPlys")
            .map(_.arr.map(v => v.num.toInt).toSet)
            .getOrElse(Set.empty[Int])
          (pgnStr, llmList)
        catch
          case _: Throwable => (body, Set.empty[Int])
      else (body, Set.empty[Int])

    val llmPlysFromQuery: Set[Int] =
      Option(exchange.getRequestURI.getQuery)
        .toList
        .flatMap(_.split("&").toList)
        .flatMap { kv =>
          kv.split("=", 2).toList match
            case key :: value :: Nil if key == "llmPlys" =>
              value.split(",").toList.flatMap(_.trim.toIntOption)
            case _ => Nil
        }
        .toSet

    val llmPlys =
      if llmPlysFromQuery.nonEmpty then llmPlysFromQuery
      else if llmFromBody.nonEmpty then llmFromBody
      else llmRequestedPlys

    val jobId = UUID.randomUUID().toString
    jobs.put(jobId, Job(status = "queued", result = None, error = None, output = None, createdAt = System.currentTimeMillis()))

    executor.submit(new Runnable:
      override def run(): Unit =
        jobs.put(jobId, Job(status = "processing", result = None, error = None, output = None, createdAt = System.currentTimeMillis()))
        AnalyzePgn.analyze(pgnText, config, llmPlys) match
          case Right(output) =>
            val annotated = LlmAnnotator.annotate(output)
            val json = AnalyzePgn.render(annotated)
            jobs.put(jobId, Job(status = "ready", result = Some(withJobId(json, jobId)), error = None, output = Some(annotated), createdAt = System.currentTimeMillis()))
          case Left(err) =>
            jobs.put(jobId, Job(status = "failed", result = None, error = Some(err), output = None, createdAt = System.currentTimeMillis()))
    )

    respond(exchange, 202, s"""{"jobId":"$jobId","status":"queued"}""")

  private def handleResult(exchange: HttpExchange): Unit =
    cleanExpiredJobs()
    if exchange.getRequestMethod.equalsIgnoreCase("OPTIONS") then
      respond(exchange, 200, """{"ok":true}""", corsOnly = true)
      return

    if !exchange.getRequestMethod.equalsIgnoreCase("GET") then
      respond(exchange, 405, """{"error":"method_not_allowed"}""")
      return

    val path = exchange.getRequestURI.getPath // /result/<id>
    val parts = path.split("/").toList.filter(_.nonEmpty)
    parts match
      case _ :: jobId :: Nil =>
        val job = jobs.get(jobId)
        if job == null then
          respond(exchange, 404, """{"error":"not_found"}""")
        else job.status match
          case "ready" =>
            respond(exchange, 200, job.result.getOrElse("""{"error":"missing_result"}"""))
          case "failed" =>
            val msg = job.error.getOrElse("unknown_error").replace("\"", "\\\"")
            respond(exchange, 500, s"""{"status":"failed","error":"$msg"}""")
          case other =>
            respond(exchange, 202, s"""{"status":"$other"}""")
      case _ =>
        respond(exchange, 404, """{"error":"not_found"}""")

  /** 변형 추가: POST /analysis/branch { jobId, ply, uci } */
  private def handleAddBranch(exchange: HttpExchange): Unit =
    cleanExpiredJobs()
    if exchange.getRequestMethod.equalsIgnoreCase("OPTIONS") then
      respond(exchange, 200, """{"ok":true}""", corsOnly = true)
      return

    if !exchange.getRequestMethod.equalsIgnoreCase("POST") then
      respond(exchange, 405, """{"error":"method_not_allowed"}""")
      return

    val body = new String(exchange.getRequestBody.readAllBytes(), StandardCharsets.UTF_8).trim
    if body.isEmpty then
      respond(exchange, 400, """{"error":"empty_body"}""")
      return

    val parsed =
      try ujson.read(body)
      catch
        case _: Throwable =>
          respond(exchange, 400, """{"error":"invalid_json"}""")
          return

    val jobId = parsed.obj.get("jobId").flatMap(v => Option(v.str).filter(_.nonEmpty))
    val ply = parsed.obj.get("ply").flatMap(v => scala.util.Try(v.num.toInt).toOption)
    val uciStr = parsed.obj.get("uci").flatMap(v => Option(v.str).filter(_.nonEmpty))

    (jobId, ply, uciStr) match
      case (Some(jid), Some(p), Some(uciMove)) =>
        val job = jobs.get(jid)
        if job == null then respond(exchange, 404, """{"error":"not_found"}""")
        else if job.status != "ready" then respond(exchange, 202, s"""{"status":"${job.status}"}""")
        else
          job.output match
            case None =>
              respond(exchange, 500, """{"error":"missing_output"}""")
            case Some(output) =>
              output.timeline.find(_.ply.value == p) match
                case None =>
                  respond(exchange, 404, """{"error":"ply_not_found"}""")
                case Some(anchor) =>
                  val fenBefore = anchor.fenBefore
                  val startFen: chess.format.FullFen = chess.format.Fen.Full.clean(fenBefore)
                  val start = chess.Game(chess.variant.Standard, Some(startFen))
                  Uci(uciMove) match
                    case Some(m: Uci.Move) =>
                      start.apply(m) match
                        case Left(_) =>
                          respond(exchange, 400, """{"error":"invalid_move"}""")
                        case Right((nextGame, _)) =>
                          val newFen = Fen.write(nextGame.position, nextGame.ply.fullMoveNumber).value
                          val san = nextGame.sans.lastOption.map(_.value).getOrElse(uciMove)
                          val lineEval = anchor.evalBeforeDeep.lines.find(_.move == uciMove)
                          val evalValue = lineEval.map(_.winPct).getOrElse(anchor.winPctBefore)
                          val newVar = AnalyzePgn.TreeNode(
                            ply = anchor.ply.value,
                            san = san,
                            uci = uciMove,
                            fen = newFen,
                            eval = evalValue,
                            evalType = "win%",
                            judgement = "variation",
                            glyph = "",
                            tags = List("variation", "user"),
                            bestMove = None,
                            bestEval = None,
                            pv = lineEval.map(_.pv).map(_.take(6)).getOrElse(Nil),
                            comment = None,
                            children = Nil
                          )
                          val updatedRoot = output.root.map(r => addVariation(r, p, newVar))
                          updatedRoot match
                            case None =>
                              respond(exchange, 500, """{"error":"missing_root"}""")
                            case Some(rootWithVar) =>
                              val updatedOutput = output.copy(root = Some(rootWithVar))
                              val json = withJobId(AnalyzePgn.render(updatedOutput), jid)
                              jobs.put(jid, job.copy(result = Some(json), output = Some(updatedOutput)))
                              respond(exchange, 200, json)
                    case _ =>
                      respond(exchange, 400, """{"error":"invalid_move"}""")
      case _ =>
        respond(exchange, 400, """{"error":"missing_fields"}""")

  private def addVariation(root: AnalyzePgn.TreeNode, ply: Int, variation: AnalyzePgn.TreeNode): AnalyzePgn.TreeNode =
    if root.ply == ply then
      val (vars, others) = root.children.partition(_.judgement == "variation")
      val deduped = vars.filterNot(_.uci == variation.uci) :+ variation
      root.copy(children = others ++ deduped)
    else
      val updatedMain = root.children.map { c =>
        if c.judgement == "variation" then c else addVariation(c, ply, variation)
      }
      root.copy(children = updatedMain)

  private def handleOpeningLookup(exchange: HttpExchange): Unit =
    if exchange.getRequestMethod.equalsIgnoreCase("OPTIONS") then
      respond(exchange, 200, """{"ok":true}""", corsOnly = true)
      return

    if !exchange.getRequestMethod.equalsIgnoreCase("GET") then
      respond(exchange, 405, """{"error":"method_not_allowed"}""")
      return

    val params = Option(exchange.getRequestURI.getQuery).toList
      .flatMap(_.split("&").toList)
      .flatMap { kv =>
        kv.split("=", 2).toList match
          case k :: v :: Nil => Some(k -> java.net.URLDecoder.decode(v, "UTF-8"))
          case _ => None
      }
      .toMap
    val sansStr = params.getOrElse("sans", "")
    val movesLimit = params.get("movesLimit").flatMap(_.toIntOption).getOrElse(8)
    val gamesLimit = params.get("gamesLimit").flatMap(_.toIntOption).getOrElse(12)
    val gamesOffset = params.get("gamesOffset").flatMap(_.toIntOption).getOrElse(0)
    if sansStr.isEmpty then
      respond(exchange, 400, """{"error":"missing_sans"}""")
      return

    val sansList = sansStr.split("\\s+").toList.filter(_.nonEmpty).map(chess.format.pgn.SanStr.apply)
    OpeningExplorer.explore(None, sansList, topGamesLimit = gamesLimit, topMovesLimit = movesLimit, gamesOffset = gamesOffset) match
      case None => respond(exchange, 404, """{"error":"not_found"}""")
      case Some(os) => respond(exchange, 200, renderOpeningStats(os))

  private def renderOpeningStats(os: OpeningExplorer.Stats): String =
    val sb = new StringBuilder()
    sb.append('{')
    sb.append("\"bookPly\":").append(os.bookPly).append(',')
    sb.append("\"noveltyPly\":").append(os.noveltyPly).append(',')
    os.games.foreach(g => sb.append("\"games\":").append(g).append(','))
    os.winWhite.foreach(w => sb.append("\"winWhite\":").append(f"$w%.4f").append(','))
    os.winBlack.foreach(w => sb.append("\"winBlack\":").append(f"$w%.4f").append(','))
    os.draw.foreach(d => sb.append("\"draw\":").append(f"$d%.4f").append(','))
    if os.topMoves.nonEmpty then
      sb.append("\"topMoves\":[")
      os.topMoves.zipWithIndex.foreach { case (m, idx) =>
        if idx > 0 then sb.append(',')
        sb.append('{')
        sb.append("\"san\":\"").append(AnalyzePgn.escape(m.san)).append("\",")
        sb.append("\"uci\":\"").append(AnalyzePgn.escape(m.uci)).append("\",")
        sb.append("\"games\":").append(m.games)
        m.winPct.foreach(w => sb.append(',').append("\"winPct\":").append(f"$w%.4f"))
        m.drawPct.foreach(d => sb.append(',').append("\"drawPct\":").append(f"$d%.4f"))
        sb.append('}')
      }
      sb.append(']').append(',')
    if os.topGames.nonEmpty then
      sb.append("\"topGames\":[")
      os.topGames.zipWithIndex.foreach { case (g, idx) =>
        if idx > 0 then sb.append(',')
        sb.append('{')
        sb.append("\"white\":\"").append(AnalyzePgn.escape(g.white)).append("\",")
        sb.append("\"black\":\"").append(AnalyzePgn.escape(g.black)).append("\",")
        g.whiteElo.foreach(e => sb.append("\"whiteElo\":").append(e).append(','))
        g.blackElo.foreach(e => sb.append("\"blackElo\":").append(e).append(','))
        sb.append("\"result\":\"").append(AnalyzePgn.escape(g.result)).append("\",")
        g.date.foreach(d => sb.append("\"date\":\"").append(AnalyzePgn.escape(d)).append("\",") )
        g.event.foreach(ev => sb.append("\"event\":\"").append(AnalyzePgn.escape(ev)).append("\",") )
        if sb.length() > 0 && sb.charAt(sb.length - 1) == ',' then sb.setLength(sb.length - 1)
        sb.append('}')
      }
      sb.append(']').append(',')
    if sb.length() > 0 && sb.charAt(sb.length - 1) == ',' then sb.setLength(sb.length - 1)
    sb.append('}')
    sb.result()

  private def respond(
      exchange: HttpExchange,
      status: Int,
      body: String,
      corsOnly: Boolean = false
  ): Unit =
    val headers = exchange.getResponseHeaders
    headers.add("Access-Control-Allow-Origin", "*")
    headers.add("Access-Control-Allow-Methods", "GET,POST,OPTIONS")
    headers.add("Access-Control-Allow-Headers", "Content-Type")
    if !corsOnly then headers.add("Content-Type", "application/json; charset=utf-8")

    val bytes = body.getBytes(StandardCharsets.UTF_8)
    val contentLength = if corsOnly then -1 else bytes.length
    exchange.sendResponseHeaders(status, contentLength)
    if !corsOnly then
      val os = exchange.getResponseBody
      try os.write(bytes)
      finally os.close()
    else exchange.close()

  private def withJobId(json: String, jobId: String): String =
    try
      val obj = ujson.read(json)
      obj("jobId") = jobId
      obj.render()
    catch
      case _: Throwable => json

  private def cleanExpiredJobs(): Unit =
    val ttlMinutes = sys.env.get("JOB_TTL_MINUTES").flatMap(_.toLongOption).getOrElse(360L)
    val ttlMs = math.max(5L, ttlMinutes) * 60_000L
    val now = System.currentTimeMillis()
    val iter = jobs.entrySet().iterator()
    while iter.hasNext do
      val entry = iter.next()
      val job = entry.getValue
      if now - job.createdAt > ttlMs then iter.remove()
