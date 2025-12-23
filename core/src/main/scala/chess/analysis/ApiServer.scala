package chess
package analysis

import cats.effect.*
import com.comcast.ip4s.*
import org.http4s.*
import org.http4s.dsl.io.*
import org.http4s.ember.server.EmberServerBuilder
import org.http4s.implicits.*
import org.http4s.server.middleware.CORS
import chess.analysis.ApiTypes.*
import org.slf4j.LoggerFactory
import ujson.Value
import chess.db.{Database, BlobStorage, JobRepo, GameRepo}
import doobie.implicits.*
import chess.auth.*
import dev.profunktor.redis4cats.effect.Log
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.*

object ApiServer extends IOApp:

  implicit val redisLog: Log[IO] = new Log[IO] {
    def debug(msg: => String): IO[Unit] = IO.unit
    def error(msg: => String): IO[Unit] = IO.unit
    def info(msg: => String): IO[Unit] = IO.unit
  }

  private val logger = LoggerFactory.getLogger("chess.api")

  // --- Config ---

  // --- Routes ---
  private def apiRoutes(
      config: AnalyzePgn.EngineConfig, 
      llmForce: Set[Int], 
      jobManager: JobManager, 
      analysisService: AnalysisService,
      usageService: UsageService[IO],
      xa: doobie.Transactor[IO]
  ): AuthedRoutes[Option[chess.db.DbUser], IO] = AuthedRoutes.of {

    case OPTIONS -> _ as _ => Ok(ujson.Obj("ok" -> true).render())

    case req @ GET -> Root / "status" as _ =>
      val remote = req.req.remoteAddr.map(_.toString).getOrElse("")
      if (remote.startsWith("127.0.0.1") || remote.startsWith("0:0:0:0:0:0:0:1")) then
        Ok(Observability.statusJson)
      else
        Forbidden(makeErrorJson("FORBIDDEN", "Access denied"))

    // POST /analyze (Legacy/Simple)
    case req @ POST -> Root / "analyze" as userOpt => 
      userOpt match
        case None => Forbidden(makeErrorJson("FORBIDDEN", "Login required for analysis"))
        case Some(user) =>
           usageService.checkLimit(user.id, user.tier).flatMap { allowed =>
             if (!allowed) TooManyRequests(makeErrorJson("RATE_LIMIT", "Daily analysis limit reached for Free Tier"))
             else
                req.req.as[String].flatMap { body =>
                  if (body.isEmpty) BadRequest(makeErrorJson("BAD_REQUEST", "Empty body"))
                  else {
                    val (pgnText, llmFromBody) = parseAnalyzeBody(body)
                    val llmPlys = llmFromBody ++ llmForce
                    
                    jobManager.submit(pgnText, config, llmPlys, Some(user.id.toString)).flatMap { jobId =>
                       usageService.increment(user.id) >>
                       Accepted(ujson.Obj("jobId" -> jobId, "status" -> "queued").render())
                    }
                  }
                }
           }

    // GET /result/:id
    case GET -> Root / "result" / jobId as _ =>
      // 1. Check if result exists (Blob/DB)
      analysisService.load(jobId).flatMap {
        case Some(json) => Ok(json)
        case None =>
          // 2. Check Job Status in DB
          val uuidOpt = scala.util.Try(java.util.UUID.fromString(jobId)).toOption
          uuidOpt match
            case Some(uuid) =>
              chess.db.JobRepo.findById(uuid).transact(xa).flatMap {
                 case Some(job) =>
                    // 3. If Processing, Get Progress from Memory OR Recover from DB Status
                    if job.status.startsWith("PROCESSING") then
                        // DB-only progress (no Redis/Memory dependency)
                        val p = job.progress.toDouble / 100.0
                        
                        // Stage from DB status: PROCESSING_ENGINE -> engine_evaluation
                        val dbStageName = if job.status.contains("_") then 
                          job.status.stripPrefix("PROCESSING_").toLowerCase 
                        else "initializing"
                        
                        val stageEnumOpt = scala.util.Try(AnalysisStage.withName(dbStageName.toUpperCase)).toOption
                        val stageLabel = stageEnumOpt.map(AnalysisStage.labelFor).getOrElse(dbStageName)
                        
                        Accepted(ujson.Obj(
                          "jobId" -> jobId,
                          "status" -> "processing",
                          "totalProgress" -> p,
                          "stageProgress" -> p, // Simplified: same as total for DB-only
                          "stage" -> dbStageName,
                          "stageLabel" -> stageLabel,
                          "startedAt" -> job.updatedAt.toEpochMilli
                        ).render())
                    else if job.status == "FAILED" then
                       InternalServerError(makeErrorJson("JOB_FAILED", job.errorMessage.getOrElse("Unknown error")))
                    else 
                       // QUEUED or other
                       if job.status == "COMPLETED" then
                         logger.warn(s"Job $jobId is COMPLETED but result load returned None. Data missing.")
                         NotFound(makeErrorJson("RESULT_MISSING", "Analysis completed but result file is missing"))
                       else
                         Accepted(ujson.Obj("jobId" -> jobId, "status" -> job.status).render())
                 case None => 
                    NotFound(makeErrorJson("NOT_FOUND", "Job ID not found"))
              }
            case None => BadRequest(makeErrorJson("INVALID_ID", "Invalid UUID format"))
      }

    // DELETE /analyze/:id
    case DELETE -> Root / "analyze" / _ as _ =>
        Ok(ujson.Obj("status" -> "cancelled_ignored").render())

    // POST /api/game-review/chapter
    case req @ POST -> Root / "api" / "game-review" / "chapter" as userOpt =>
      userOpt match
        case None => Forbidden(makeErrorJson("FORBIDDEN", "Login required for analysis"))
        case Some(user) =>
           usageService.checkLimit(user.id, user.tier).flatMap { allowed =>
             if (!allowed) TooManyRequests(makeErrorJson("RATE_LIMIT", "Daily analysis limit reached for Free Tier"))
             else
                req.req.as[String].flatMap { body =>
                   IO.fromTry(scala.util.Try(ujson.read(body))).flatMap { json =>
                      val pgn = json.obj.get("pgn").map(_.str).getOrElse("")
                      val optionsObj = json.obj.get("options")
                      val options = optionsObj.map { opt =>
                         ReviewOptions(
                           language = opt.obj.get("language").map(_.str).map(Language.fromString).getOrElse(Language.English),
                           depthProfile = opt.obj.get("depthProfile").map(_.str).map(DepthProfile.fromString).getOrElse(DepthProfile.Fast),
                           maxExperimentsPerPly = opt.obj.get("maxExperimentsPerPly").map(_.num.toInt),
                           forceCriticalPlys = opt.obj.get("forceCriticalPlys").map(_.arr.map(_.num.toInt).toSet).getOrElse(Set.empty)
                         )
                      }
                      val effectiveConfig = options.map(mapToEngineConfig(_, config)).getOrElse(config)
                      val effectiveLlmPlys = options.map(_.forceCriticalPlys).getOrElse(llmForce)
                      
                      jobManager.submit(pgn, effectiveConfig, effectiveLlmPlys, Some(user.id.toString)).flatMap { jobId =>
                         usageService.increment(user.id) >>
                         Accepted(ujson.Obj("jobId" -> jobId, "status" -> "queued").render())
                      }
                   }.handleErrorWith(e => BadRequest(makeErrorJson("INVALID_REQUEST", e.getMessage)))
                }
           }

    // GET /api/game-review/list
    case req @ GET -> Root / "api" / "game-review" / "list" as userOpt =>
      userOpt match
        case None => Forbidden(makeErrorJson("FORBIDDEN", "Login required"))
        case Some(user) =>
           val limit = req.req.uri.query.params.get("limit").flatMap(_.toIntOption).getOrElse(20)
           chess.db.GameRepo.listByUser(user.id, limit).transact(xa).flatMap { games =>
              val json = ujson.Arr.from(games.map { g =>
                ujson.Obj(
                  "id" -> g.id.toString,
                  "white" -> g.whitePlayer.getOrElse("?"),
                  "black" -> g.blackPlayer.getOrElse("?"),
                  "result" -> g.result.getOrElse("*"),
                  "date" -> g.datePlayed.map(_.toString).getOrElse(""),
                  "eco" -> g.eco.getOrElse(""),
                  "createdAt" -> g.createdAt.toString
                )
              })
              Ok(json.render())
           }

    // GET /opening/lookup
    case req @ GET -> Root / "opening" / "lookup" as _ =>
      val params = req.req.uri.query.params
      val sansStr = params.getOrElse("sans", "")
      if (sansStr.isEmpty) BadRequest(makeErrorJson("BAD_REQUEST", "Missing sans"))
      else {
        val gamesLimit = params.get("gamesLimit").flatMap(_.toIntOption).getOrElse(12)
        val movesLimit = params.get("movesLimit").flatMap(_.toIntOption).getOrElse(8)
        val sansList = sansStr.split("\\s+").toList.filter(_.nonEmpty).map(chess.format.pgn.SanStr.apply)
        
        IO.blocking(OpeningExplorer.explore(None, sansList, topGamesLimit = gamesLimit, topMovesLimit = movesLimit)).flatMap {
            case Some(os) => Ok(renderOpeningStats(os))
            case None => NotFound(makeErrorJson("NOT_FOUND", "No opening stats found"))
        }
      }
  }

  override def run(args: List[String]): IO[ExitCode] =
    
    Database.make().use { xa =>
      // Retry policy for Redis connections
    // Redis URL is no longer used for Queue/Progress, but kept for legacy UsageService/Auth if needed or removed later.
    // For this Hotfix, we switched to In-Memory Queue & Progress.
    

    
    // DB-only queue: no QueueClient needed
    UsageService.makeMemory.use { usageService =>
      cats.effect.std.Dispatcher.parallel[IO].use { dispatcher =>
      JobManager.make(xa).flatMap { jobManager =>
      
      val loadConfig = IO.blocking(AnalyzePgn.EngineConfig.fromEnv()).map(c => (c, EnvLoader.get("ANALYZE_FORCE_CRITICAL_PLYS")
          .map(_.split(",").toList.flatMap(_.trim.toIntOption).toSet)
          .getOrElse(Set.empty)))

      val blob = new BlobStorage.DbBlobStorage(xa)
      val analysisService = new AnalysisService(xa, blob)
      val engineService = new EngineService() // Worker component

      // --- Worker Loop: Poll DB for QUEUED jobs ---
      def pollAndProcess: IO[Unit] = 
        JobRepo.claimNextJob.transact(xa).flatMap {
          case Some(job) =>
            IO(logger.info(s"Processing job ${job.id}")) >> {
              // Parse options from JSON - outside for-comprehension
              val optionsJson = scala.util.Try(ujson.read(job.optionsJson)).getOrElse(ujson.Obj())
              val forceCriticalPlys = optionsJson.obj.get("forceCriticalPlys")
                .map(_.arr.map(_.num.toInt).toSet).getOrElse(Set.empty[Int])

              // Configure Engine
              val config = AnalyzePgn.EngineConfig.fromEnv()
              
              // Callback for progress updates
              val progCallback: (AnalysisStage.AnalysisStage, Double) => Unit = 
                (stage, p) => dispatcher.unsafeRunAndForget(
                  JobRepo.updateStatus(job.id, s"PROCESSING:${AnalysisStage.labelFor(stage)}:${(p * 100).toInt}%", (p * 100).toInt).transact(xa)
                    .handleErrorWith(e => IO(logger.warn(s"Progress update failed: ${e.getMessage}")))
                )

              (for
                // Run Analysis
                output <- IO.blocking(
                  AnalyzePgn.analyze(
                    pgn = job.pgnText, 
                    engineService = engineService, 
                    config = config, 
                    llmRequestedPlys = forceCriticalPlys, 
                    onProgress = progCallback
                  )
                ).flatMap {
                    case Right(out) => IO.pure(out)
                    case Left(err) => IO.raiseError(new Exception(err))
                  }
                
                // Annotate
                _ <- IO(progCallback(AnalysisStage.LLM_GENERATION, 0.0))
                annotated = LlmAnnotator.annotate(output)
                _ <- IO(progCallback(AnalysisStage.FINALIZATION, 1.0))
                
                // Save (get userId from games table via gameId)
                gameOpt <- GameRepo.findById(job.gameId).transact(xa)
                userIdOpt = gameOpt.flatMap(_.userId)
                _ <- analysisService.save(job.id.toString, annotated, userIdOpt)
                
                // Complete
                _ <- JobRepo.updateStatus(job.id, "COMPLETED", 100).transact(xa)
                 >> IO(logger.info(s"Job ${job.id} completed"))

              yield ()).handleErrorWith { e =>
                 logger.error(s"Job ${job.id} failed", e)
                 JobRepo.updateStatus(job.id, "FAILED", 0).transact(xa).void
              }
            }
          case None =>
            // No jobs available, wait before polling again
            IO.sleep(1.second)
        }
      
      val workerLoop = pollAndProcess.foreverM

      // Zombie Cleanup Loop: Every 2 minutes, fail jobs stuck in PROCESSING for 8+ minutes
      def zombieCleanup: IO[Unit] = 
        (for {
          threshold <- IO(java.time.Instant.now().minusSeconds(480)) // 8 minutes
          count <- JobRepo.failStuckJobs(threshold).transact(xa)
          _ <- if (count > 0) IO(logger.warn(s"Cleaned up $count stuck jobs")) else IO.unit
        } yield ()).handleErrorWith(e => IO(logger.error(s"Zombie cleanup failed: ${e.getMessage}")))
          >> IO.sleep(2.minutes)

      val cleanupLoop = zombieCleanup.foreverM

      for {
        (config, llmForce) <- loadConfig
        port <- IO.fromOption(EnvLoader.get("PORT").flatMap(_.toIntOption))(new Exception("PORT not set")).handleError(_ => 8080)
        host <- IO.fromOption(EnvLoader.get("BIND").flatMap(Ipv4Address.fromString))(new Exception("Invalid BIND Address")).handleError(_ => ipv4"0.0.0.0")
        
        // Middleware Config
        bucketSize <- IO(EnvLoader.get("RATE_LIMIT_BUCKET").flatMap(_.toIntOption).getOrElse(60))
        refillRate <- IO(EnvLoader.get("RATE_LIMIT_REFILL").flatMap(_.toDoubleOption).getOrElse(1.0))
        jwtSecret = EnvLoader.getOrElse("JWT_SECRET", "dev-jwt-secret")
        apiKey = EnvLoader.getOrElse("API_KEY", "dev-secret-key")
        authService = AuthService.make(xa, jwtSecret)
        userMiddleware = AuthMiddlewareVals(authService, xa)
        userAuthRoutes = AuthRoutes.routes(authService, userMiddleware)
        
        // Initialize Middleware
        rateLimiter <- Middleware.rateLimitMiddleware(bucketSize, refillRate)
        // Use Hybrid Auth (JWT or API Key)
        hybridAuth = chess.auth.AuthMiddlewareVals.optionalAuthMiddleware(authService, xa, apiKey) 
        requestLogger = Middleware.requestLoggingMiddleware(logger)
        
        _ <- IO(logger.info(s"Starting http4s server on $host:$port"))
        
        // Combine Routes
        // /auth/* -> User Auth (Login/Register)
        // /*      -> Hybrid Auth (Analyze, Result, etc)
        combinedRoutes = org.http4s.server.Router(
          "/auth" -> userAuthRoutes,
          "/" -> hybridAuth(apiRoutes(config, llmForce, jobManager, analysisService, usageService, xa))
        )

        finalHttpApp = CORS.policy.withAllowOriginAll(
           requestLogger(
             rateLimiter(
               combinedRoutes
             )
           ).orNotFound
        )
        
        server = EmberServerBuilder.default[IO]
          .withHost(host)
          .withPort(Port.fromInt(port).getOrElse(port"8080")) 
          .withHttpApp(finalHttpApp)
          .build
          .use(_ => IO.never)

        // Run Server, Worker, and Zombie Cleanup concurrently
        _ <- server &> workerLoop &> cleanupLoop
      } yield ExitCode.Success
      }
      }
      }
    }


  // --- Helpers ---
  private def makeErrorJson(code: String, message: String): String =
     ujson.Obj("error" -> ujson.Obj("code" -> code, "message" -> message)).render()

  private def parseAnalyzeBody(body: String): (String, Set[Int]) =
     if body.startsWith("{") then
       try
         val json = ujson.read(body)
         val pgn = json.obj.get("pgn").map(_.str).getOrElse("")
         val llms = json.obj.get("llmPlys").map(_.arr.map(_.num.toInt).toSet).getOrElse(Set.empty)
         (pgn, llms)
       catch case _ => (body, Set.empty)
     else (body, Set.empty)



  private def mapToEngineConfig(opts: ReviewOptions, default: AnalyzePgn.EngineConfig): AnalyzePgn.EngineConfig =
    val (shallow, deep, sTime, dTime) = opts.depthProfile match
      case DepthProfile.Fast => (10, 14, 50, 100)
      case DepthProfile.Deep => (14, 18, 100, 500)
      case DepthProfile.Tournament => (16, 20, 200, 1500)
    
    default.copy(
      shallowDepth = shallow, 
      deepDepth = deep, 
      shallowTimeMs = sTime, 
      deepTimeMs = dTime
    )

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


