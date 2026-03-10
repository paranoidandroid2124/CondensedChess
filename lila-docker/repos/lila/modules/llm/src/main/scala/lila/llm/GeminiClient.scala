package lila.llm

import play.api.libs.json.*
import play.api.libs.ws.StandaloneWSClient
import play.api.libs.ws.JsonBodyWritables.*
import play.api.libs.ws.DefaultBodyReadables.*
import scala.concurrent.Future
import scala.concurrent.duration.*
import java.util.concurrent.atomic.{ AtomicLong, AtomicReference }
import lila.llm.analysis.BookmakerPolishSlots

/** Gemini API client with context caching.
  * Falls back to `None` when disabled or on error.
  */
final class GeminiClient(ws: StandaloneWSClient, config: GeminiConfig)(using Executor):

  private val logger = lila.log("gemini")


  private val generateEndpoint =
    s"https://generativelanguage.googleapis.com/v1beta/models/${config.model}:generateContent"
  private val cacheCreateEndpoint =
    "https://generativelanguage.googleapis.com/v1beta/cachedContents"


  private val cachedContentName = new AtomicReference[String](null)
  private val cacheExpiresAt = new AtomicLong(0L)


  @volatile private var cacheCreating = false

  if config.enabled then
    logger.info(s"Gemini polish enabled: model=${config.model}, temp=${config.temperature}")
  else
    logger.info("Gemini polish disabled (no API key)")


  /** Polish rule-based commentary using Gemini. */
  def polish(
      prose: String,
      phase: String,
      evalDelta: Option[Int],
      concepts: List[String],
      fen: String,
      openingName: Option[String] = None,
      nature: Option[String] = None,
      tension: Option[Double] = None,
      salience: Option[lila.llm.model.strategic.StrategicSalience] = None,
      momentType: Option[String] = None,
      bookmakerSlots: Option[BookmakerPolishSlots] = None
  ): Future[Option[String]] =
    if !config.enabled then Future.successful(None)
    else if prose.isBlank then Future.successful(None)
    else
      val userPrompt = PolishPrompt.buildPolishPrompt(
        prose = prose,
        phase = phase,
        evalDelta = evalDelta,
        concepts = concepts,
        fen = fen,
        openingName = openingName,
        nature = nature,
        tension = tension,
        salience = salience,
        momentType = momentType,
        bookmakerSlots = bookmakerSlots
      )
      callWithSystemPrompt(userPrompt)
        .recover { case e: Throwable =>
          logger.warn(s"Gemini polish failed, using rule-based fallback: ${e.getMessage}")
          None
        }

  /** Repair pass for strict preservation when first polish output fails validation. */
  def repair(
      originalProse: String,
      rejectedPolish: String,
      phase: String,
      evalDelta: Option[Int],
      concepts: List[String],
      fen: String,
      openingName: Option[String] = None,
      allowedSans: List[String] = Nil,
      bookmakerSlots: Option[BookmakerPolishSlots] = None
  ): Future[Option[String]] =
    if !config.enabled then Future.successful(None)
    else if originalProse.isBlank || rejectedPolish.isBlank then Future.successful(None)
    else
      val repairPrompt = PolishPrompt.buildRepairPrompt(
        originalProse = originalProse,
        rejectedPolish = rejectedPolish,
        phase = phase,
        evalDelta = evalDelta,
        concepts = concepts,
        fen = fen,
        openingName = openingName,
        allowedSans = allowedSans,
        bookmakerSlots = bookmakerSlots
      )
      callWithSystemPrompt(repairPrompt)
        .recover { case e: Throwable =>
          logger.warn(s"Gemini repair failed, using rule-based fallback: ${e.getMessage}")
          None
        }

  def activeStrategicNote(
      baseNarrative: String,
      phase: String,
      momentType: String,
      concepts: List[String],
      fen: String,
      strategyPack: Option[StrategyPack],
      dossier: Option[ActiveBranchDossier] = None,
      routeRefs: List[ActiveStrategicRouteRef] = Nil,
      moveRefs: List[ActiveStrategicMoveRef] = Nil
  ): Future[Option[String]] =
    if !config.enabled then Future.successful(None)
    else if baseNarrative.isBlank then Future.successful(None)
    else
      val prompt = ActiveStrategicPrompt.buildPrompt(
        baseNarrative = baseNarrative,
        phase = phase,
        momentType = momentType,
        fen = fen,
        concepts = concepts,
        strategyPack = strategyPack,
        dossier = dossier,
        routeRefs = routeRefs,
        moveRefs = moveRefs
      )
      callWithSystemPrompt(
        userPrompt = prompt,
        systemPrompt = ActiveStrategicPrompt.systemPrompt,
        allowContextCache = false
      ).recover { case e: Throwable =>
        logger.warn(s"Gemini active note failed, omitting note: ${e.getMessage}")
        None
      }

  def repairActiveStrategicNote(
      baseNarrative: String,
      rejectedNote: String,
      failureReasons: List[String],
      phase: String,
      momentType: String,
      concepts: List[String],
      fen: String,
      strategyPack: Option[StrategyPack],
      dossier: Option[ActiveBranchDossier] = None,
      routeRefs: List[ActiveStrategicRouteRef] = Nil,
      moveRefs: List[ActiveStrategicMoveRef] = Nil
  ): Future[Option[String]] =
    if !config.enabled then Future.successful(None)
    else if baseNarrative.isBlank || rejectedNote.isBlank then Future.successful(None)
    else
      val prompt = ActiveStrategicPrompt.buildRepairPrompt(
        baseNarrative = baseNarrative,
        rejectedNote = rejectedNote,
        failureReasons = failureReasons,
        phase = phase,
        momentType = momentType,
        fen = fen,
        concepts = concepts,
        strategyPack = strategyPack,
        dossier = dossier,
        routeRefs = routeRefs,
        moveRefs = moveRefs
      )
      callWithSystemPrompt(
        userPrompt = prompt,
        systemPrompt = ActiveStrategicPrompt.systemPrompt,
        allowContextCache = false
      ).recover { case e: Throwable =>
        logger.warn(s"Gemini active note repair failed, omitting note: ${e.getMessage}")
        None
      }


  def isEnabled: Boolean = config.enabled
  def modelName: String = config.model


  private def ensureCachedContent(): Future[Option[String]] =
    val now = System.currentTimeMillis()
    val existing = cachedContentName.get()
    val expiresAt = cacheExpiresAt.get()

    if existing != null && now < expiresAt - 120_000 then
      Future.successful(Some(existing))

    else if !cacheCreating then
      cacheCreating = true
      createCachedContent()
        .map { nameOpt =>
          nameOpt.foreach { name =>
            cachedContentName.set(name)
            cacheExpiresAt.set(now + config.contextCacheTtlMinutes.toLong * 60_000)
            logger.info(s"Gemini context cache created: $name (TTL=${config.contextCacheTtlMinutes}min)")
          }
          cacheCreating = false
          nameOpt
        }
        .recover { case e: Throwable =>
          cacheCreating = false
          logger.warn(s"Failed to create context cache: ${e.getMessage}")
          None
        }
    else Future.successful(None)


  private def createCachedContent(): Future[Option[String]] =
    val body = Json.obj(
      "model" -> s"models/${config.model}",
      "contents" -> Json.arr(
        Json.obj(
          "role" -> "user",
          "parts" -> Json.arr(
            Json.obj("text" -> PolishPrompt.systemPrompt)
          )
        )
      ),
      "ttl" -> s"${config.contextCacheTtlMinutes * 60}s"
    )

    ws.url(s"$cacheCreateEndpoint?key=${config.apiKey}")
      .withRequestTimeout(15.seconds)
      .post(body)
      .map { response =>
        if response.status == 200 then
          val json = Json.parse(response.body[String])
          (json \ "name").asOpt[String]
        else
          logger.warn(s"Context cache creation failed: ${response.status} - ${response.body[String].take(200)}")
          None
      }


  private def callWithSystemPrompt(
      userPrompt: String,
      systemPrompt: String = PolishPrompt.systemPrompt,
      allowContextCache: Boolean = true
  ): Future[Option[String]] =
    val cacheFut =
      if allowContextCache && systemPrompt == PolishPrompt.systemPrompt then ensureCachedContent()
      else Future.successful(None)

    cacheFut.flatMap { cachedNameOpt =>
      val contents = Json.arr(
        Json.obj(
          "role" -> "user",
          "parts" -> Json.arr(Json.obj("text" -> userPrompt))
        )
      )

      val generationConfig = Json.obj(
        "temperature" -> config.temperature,
        "maxOutputTokens" -> config.maxOutputTokens
      )

      val body = cachedNameOpt match
        case Some(cacheName) =>
          Json.obj(
            "cachedContent" -> cacheName,
            "contents" -> contents,
            "generationConfig" -> generationConfig
          )
        case None =>
          Json.obj(
            "systemInstruction" -> Json.obj(
              "parts" -> Json.arr(Json.obj("text" -> systemPrompt))
            ),
            "contents" -> contents,
            "generationConfig" -> generationConfig
          )

      ws.url(s"$generateEndpoint?key=${config.apiKey}")
        .withRequestTimeout(config.requestTimeoutSeconds.seconds)
        .post(body)
        .map { response =>
          response.status match
            case 200 =>
              extractText(response.body[String])
            case 429 =>
              logger.warn("Gemini rate limited (429)")
              None
            case status =>
              logger.warn(s"Gemini API error: $status - ${response.body[String].take(200)}")
              None
        }
    }


  private def extractText(body: String): Option[String] =
    try
      val json = Json.parse(body)
      val textOpt = (json \ "candidates" \ 0 \ "content" \ "parts" \ 0 \ "text").asOpt[String]
      textOpt.map(_.trim).filter(_.nonEmpty)
    catch
      case e: Throwable =>
        logger.warn(s"Failed to parse Gemini response: ${e.getMessage}")
        None
