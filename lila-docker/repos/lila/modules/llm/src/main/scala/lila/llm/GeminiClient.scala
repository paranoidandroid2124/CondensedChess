package lila.llm

import play.api.libs.json.*
import play.api.libs.ws.StandaloneWSClient
import play.api.libs.ws.JsonBodyWritables.*
import play.api.libs.ws.DefaultBodyReadables.*
import scala.concurrent.{ ExecutionContext, Future }
import scala.concurrent.duration.*
import java.util.concurrent.atomic.{ AtomicLong, AtomicReference }

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
      nature: Option[String] = None,
      tension: Option[Double] = None
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
        nature = nature,
        tension = tension
      )
      callWithSystemPrompt(userPrompt)
        .recover { case e: Throwable =>
          logger.warn(s"Gemini polish failed, using rule-based fallback: ${e.getMessage}")
          None
        }


  def isEnabled: Boolean = config.enabled


  def estimateTokens(prose: String): GeminiTokenEstimate =
    GeminiTokenEstimate(
      systemTokens = PolishPrompt.estimatedSystemTokens,
      inputTokens = PolishPrompt.estimateRequestTokens(prose),
      cachedDiscount = 0.9,
      outputTokens = 100 // typical polish output
    )


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


  private def callWithSystemPrompt(userPrompt: String): Future[Option[String]] =
    ensureCachedContent().flatMap { cachedNameOpt =>
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
              "parts" -> Json.arr(Json.obj("text" -> PolishPrompt.systemPrompt))
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


case class GeminiTokenEstimate(
    systemTokens: Int,
    inputTokens: Int,
    cachedDiscount: Double,
    outputTokens: Int
):

  def effectiveInputTokens: Int =
    (systemTokens * (1.0 - cachedDiscount)).toInt + inputTokens


  def estimatedCostUsd: Double =
    val inputCost = effectiveInputTokens * 0.10 / 1_000_000  // $0.10 per 1M tokens
    val outputCost = outputTokens * 0.40 / 1_000_000         // $0.40 per 1M tokens
    inputCost + outputCost
