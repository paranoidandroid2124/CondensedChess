package chess
package analysis

import com.sun.net.httpserver.{ HttpExchange, HttpServer }

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

  private case class Job(status: String, result: Option[String], error: Option[String])

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
    jobs.put(jobId, Job(status = "queued", result = None, error = None))

    executor.submit(new Runnable:
      override def run(): Unit =
        jobs.put(jobId, Job(status = "processing", result = None, error = None))
        AnalyzePgn.analyze(pgnText, config, llmPlys) match
          case Right(output) =>
            val annotated = LlmAnnotator.annotate(output)
            val json = AnalyzePgn.render(annotated)
            jobs.put(jobId, Job(status = "ready", result = Some(json), error = None))
          case Left(err) =>
            jobs.put(jobId, Job(status = "failed", result = None, error = Some(err)))
    )

    respond(exchange, 202, s"""{"jobId":"$jobId","status":"queued"}""")

  private def handleResult(exchange: HttpExchange): Unit =
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
