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
import chess.db.{Database, BlobStorage, JobRepo}
import doobie.implicits.*
import chess.auth.*
import dev.profunktor.redis4cats.effect.Log
import scala.concurrent.ExecutionContext.Implicits.global

object ApiServer extends IOApp:

  implicit val redisLog: Log[IO] = new Log[IO] {
    def debug(msg: => String): IO[Unit] = IO.unit
    def error(msg: => String): IO[Unit] = IO.unit
    def info(msg: => String): IO[Unit] = IO.unit
  }

  private val logger = LoggerFactory.getLogger("chess.api")

  // --- Config ---
  private def loadConfig: IO[(AnalyzePgn.EngineConfig, Set[Int])] = IO {
    val config = AnalyzePgn.EngineConfig.fromEnv()
    val llmForce = EnvLoader.get("ANALYZE_FORCE_CRITICAL_PLYS")
      .map(_.split(",").toList.flatMap(_.trim.toIntOption).toSet)
      .getOrElse(Set.empty)
    (config, llmForce)
  }

  // --- Routes ---
  // --- Routes ---
  private def apiRoutes(
      config: AnalyzePgn.EngineConfig, 
      llmForce: Set[Int], 
      jobManager: JobManager, 
      analysisService: AnalysisService,
      progressClient: ProgressClient[IO],
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
                    // 3. If Processing, Get Progress from Redis
                    if job.status == "PROCESSING" then
                       progressClient.get(jobId).flatMap { progOpt =>
                          val p = progOpt.map(_.totalProgress).getOrElse(job.progress.toDouble / 100.0)
                          val stage = progOpt.map(_.stage.toString).getOrElse("Unknown")
                          Accepted(ujson.Obj(
                            "jobId" -> jobId,
                            "status" -> "processing",
                            "progress" -> p,
                            "stage" -> stage
                          ).render())
                       }
                    else if job.status == "FAILED" then
                       InternalServerError(makeErrorJson("JOB_FAILED", job.errorMessage.getOrElse("Unknown error")))
                    else 
                       // QUEUED or other
                       Accepted(ujson.Obj("jobId" -> jobId, "status" -> job.status).render())
                 case None => 
                    NotFound(makeErrorJson("NOT_FOUND", "Job ID not found"))
              }
            case None => BadRequest(makeErrorJson("INVALID_ID", "Invalid UUID format"))
      }

    // DELETE /analyze/:id
    case DELETE -> Root / "analyze" / jobId as _ =>
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
    val redisUrl = EnvLoader.getOrElse("REDIS_URL", "redis://localhost:6379")
    
    Database.make().use { xa =>
      QueueClient.makeRedis(redisUrl).use { queue =>
        ProgressClient.makeRedis(redisUrl).use { progressClient =>
          dev.profunktor.redis4cats.Redis[IO].utf8(redisUrl).use { redisCmd =>
          cats.effect.std.Dispatcher.parallel[IO].use { dispatcher =>
          
          val jobManager = JobManager.make(queue, xa)
          val loadConfig = IO.blocking(AnalyzePgn.EngineConfig.fromEnv()).map(c => (c, EnvLoader.get("ANALYZE_FORCE_CRITICAL_PLYS")
              .map(_.split(",").toList.flatMap(_.trim.toIntOption).toSet)
              .getOrElse(Set.empty)))

          val blob = new BlobStorage.FileBlobStorage("data")
          val analysisService = new AnalysisService(xa, blob)
          val usageService = UsageService.make(redisCmd)
          val engineService = new EngineService() // Worker component

          // --- Worker Loop ---
          val workerLoop = queue.dequeue.flatMap { job =>
             IO(logger.info(s"Processing job ${job.jobId}")) >>
             (for
               // 1. Update Status -> Processing
               _ <- JobRepo.updateStatus(java.util.UUID.fromString(job.jobId), "PROCESSING", 0).transact(xa)

               // 2. Configure Engine
               config = AnalyzePgn.EngineConfig.fromEnv()
               
               // Callback for progress updates
               progCallback = (stage: AnalysisStage.AnalysisStage, p: Double) => 
                  dispatcher.unsafeRunAndForget(
                    progressClient.update(job.jobId, stage, p).handleErrorWith(e => IO(logger.warn(s"Progress update failed: ${e.getMessage}")))
                  )

               // 3. Run Analysis
               output <- IO.blocking(
                 AnalyzePgn.analyze(
                   pgn = job.pgn, 
                   engineService = engineService, 
                   config = config, 
                   llmRequestedPlys = job.options.forceCriticalPlys, 
                   onProgress = progCallback
                 )
               ).flatMap {
                   case Right(out) => IO.pure(out)
                   case Left(err) => IO.raiseError(new Exception(err))
                 }
               
               // 4. Annotate
               _ <- IO(progCallback(AnalysisStage.LLM_GENERATION, 0.0))
               annotated = LlmAnnotator.annotate(output)
               _ <- IO(progCallback(AnalysisStage.FINALIZATION, 1.0))
               
               // 5. Save
               userId = job.userId.flatMap(u => scala.util.Try(java.util.UUID.fromString(u)).toOption)
               _ <- analysisService.save(job.jobId, annotated, userId)
               
               // 6. Complete
               _ <- JobRepo.updateStatus(java.util.UUID.fromString(job.jobId), "COMPLETED", 100).transact(xa)
                >> IO(logger.info(s"Job ${job.jobId} completed"))

             yield ()).handleErrorWith { e =>
                logger.error(s"Job ${job.jobId} failed", e)
                JobRepo.updateStatus(java.util.UUID.fromString(job.jobId), "FAILED", 0).transact(xa).void
             }
          }.foreverM

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
              "/" -> hybridAuth(apiRoutes(config, llmForce, jobManager, analysisService, progressClient, usageService, xa))
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

            // Run Server and Worker concurrently
            _ <- server &> workerLoop
          } yield ExitCode.Success
          }
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
