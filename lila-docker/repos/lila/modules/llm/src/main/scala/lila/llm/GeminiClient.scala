package lila.llm

import play.api.libs.json.*
import play.api.libs.ws.StandaloneWSClient
import play.api.libs.ws.JsonBodyWritables.*
import play.api.libs.ws.DefaultBodyReadables.*
import scala.concurrent.{ ExecutionContext, Future }
import scala.concurrent.duration.*
import java.util.concurrent.atomic.{ AtomicLong, AtomicReference }

/** Gemini API client with context caching and graceful fallback.
  *
  * When `config.enabled = false`, all public methods return `None` immediately.
  * When enabled, the system prompt is cached via Gemini's Context Caching API
  * to reduce per-request input costs by ~90%.
  *
  * All errors are caught and logged — Gemini failure never breaks the pipeline.
  * The caller always receives either polished text or None (→ falls back to rule-based).
  */
final class GeminiClient(ws: StandaloneWSClient, config: GeminiConfig)(using Executor):

  private val logger = lila.log("gemini")

  // ── Endpoints ──────────────────────────────────────────────────────────
  private val generateEndpoint =
    s"https://generativelanguage.googleapis.com/v1beta/models/${config.model}:generateContent"
  private val cacheCreateEndpoint =
    "https://generativelanguage.googleapis.com/v1beta/cachedContents"

  // ── Context Cache State ────────────────────────────────────────────────
  // Cached content name (e.g., "cachedContents/abc123") — thread-safe
  private val cachedContentName = new AtomicReference[String](null)
  private val cacheExpiresAt = new AtomicLong(0L)

  // Flag to prevent concurrent cache creation
  @volatile private var cacheCreating = false

  if config.enabled then
    logger.info(s"Gemini polish enabled: model=${config.model}, temp=${config.temperature}")
  else
    logger.info("Gemini polish disabled (no API key)")

  // ── Public API ─────────────────────────────────────────────────────────

  /** Polish rule-based commentary using Gemini.
    *
    * @return Some(polished) if Gemini succeeds, None if disabled or on error
    */
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

  /** Check if Gemini is enabled. */
  def isEnabled: Boolean = config.enabled

  /** Get estimated token count for cost monitoring. */
  def estimateTokens(prose: String): GeminiTokenEstimate =
    GeminiTokenEstimate(
      systemTokens = PolishPrompt.estimatedSystemTokens,
      inputTokens = PolishPrompt.estimateRequestTokens(prose),
      cachedDiscount = 0.9,
      outputTokens = 100 // typical polish output
    )

  // ── Context Cache Management ───────────────────────────────────────────

  /** Ensure the system prompt is cached on Gemini's side.
    * Creates or refreshes the cache as needed.
    */
  private def ensureCachedContent(): Future[Option[String]] =
    val now = System.currentTimeMillis()
    val existing = cachedContentName.get()
    val expiresAt = cacheExpiresAt.get()

    // Cache is valid — reuse it
    if existing != null && now < expiresAt - 120_000 then // 2-min safety margin
      Future.successful(Some(existing))
    // Cache expired or doesn't exist — create new one
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
    else
      // Another thread is creating the cache — skip caching for this request
      Future.successful(None)

  /** POST to cachedContents endpoint to create a new cache. */
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

  // ── Core API Call ──────────────────────────────────────────────────────

  /** Call Gemini with the cached system prompt + per-request user prompt.
    *
    * If context cache is available, references it via `cachedContent`.
    * If not, falls back to inline system instruction (higher cost).
    */
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
          // Use cached context — saves ~90% on system prompt tokens
          Json.obj(
            "cachedContent" -> cacheName,
            "contents" -> contents,
            "generationConfig" -> generationConfig
          )
        case None =>
          // Fallback: inline system instruction (full cost)
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

  /** Extract text from Gemini generateContent response. */
  private def extractText(body: String): Option[String] =
    try
      val json = Json.parse(body)
      val textOpt = (json \ "candidates" \ 0 \ "content" \ "parts" \ 0 \ "text").asOpt[String]
      textOpt.map(_.trim).filter(_.nonEmpty)
    catch
      case e: Throwable =>
        logger.warn(s"Failed to parse Gemini response: ${e.getMessage}")
        None

/** Token estimate for cost monitoring. */
case class GeminiTokenEstimate(
    systemTokens: Int,
    inputTokens: Int,
    cachedDiscount: Double,
    outputTokens: Int
):
  /** Effective input tokens after context caching discount. */
  def effectiveInputTokens: Int =
    (systemTokens * (1.0 - cachedDiscount)).toInt + inputTokens

  /** Estimated cost in USD per request (Gemini 2.0 Flash pricing). */
  def estimatedCostUsd: Double =
    val inputCost = effectiveInputTokens * 0.10 / 1_000_000  // $0.10 per 1M tokens
    val outputCost = outputTokens * 0.40 / 1_000_000         // $0.40 per 1M tokens
    inputCost + outputCost
