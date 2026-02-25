package lila.llm

import play.api.libs.json.*
import play.api.libs.ws.StandaloneWSClient
import play.api.libs.ws.JsonBodyWritables.*
import play.api.libs.ws.DefaultBodyReadables.*
import scala.concurrent.Future
import scala.concurrent.duration.*
import scala.util.control.NonFatal

case class OpenAiPolishResult(
    commentary: String,
    model: String,
    promptCacheHit: Boolean,
    promptTokens: Option[Int] = None,
    cachedTokens: Option[Int] = None,
    completionTokens: Option[Int] = None,
    estimatedCostUsd: Option[Double] = None,
    parseWarnings: List[String] = Nil
)

final class OpenAiClient(ws: StandaloneWSClient, config: OpenAiConfig)(using Executor):

  private val logger = lila.log("openai")

  private val responseSchema = Json.obj(
    "type" -> "object",
    "properties" -> Json.obj(
      "commentary" -> Json.obj("type" -> "string")
    ),
    "required" -> Json.arr("commentary"),
    "additionalProperties" -> false
  )

  if config.enabled then
    logger.info(
      s"OpenAI polish enabled: sync=${config.modelSync}, async=${config.modelAsync}, fallback=${config.modelFallback}, max_output_tokens=${config.maxOutputTokens}"
    )
  else logger.info("OpenAI polish disabled (no API key)")

  def isEnabled: Boolean = config.enabled
  def syncModelName: String = config.modelSync
  def asyncModelName: String = config.modelAsync

  def polishSync(
      prose: String,
      phase: String,
      evalDelta: Option[Int],
      concepts: List[String],
      fen: String,
      openingName: Option[String] = None,
      nature: Option[String] = None,
      tension: Option[Double] = None,
      lang: String = "en",
      maxOutputTokens: Option[Int] = None
  ): Future[Option[OpenAiPolishResult]] =
    polishWithFallback(
      prose = prose,
      phase = phase,
      evalDelta = evalDelta,
      concepts = concepts,
      fen = fen,
      openingName = openingName,
      nature = nature,
      tension = tension,
      model = config.modelSync,
      serviceTier = None,
      lang = lang,
      maxOutputTokens = maxOutputTokens
    )

  def polishAsync(
      prose: String,
      phase: String,
      evalDelta: Option[Int],
      concepts: List[String],
      fen: String,
      openingName: Option[String] = None,
      nature: Option[String] = None,
      tension: Option[Double] = None,
      lang: String = "en",
      maxOutputTokens: Option[Int] = None
  ): Future[Option[OpenAiPolishResult]] =
    polishWithFallback(
      prose = prose,
      phase = phase,
      evalDelta = evalDelta,
      concepts = concepts,
      fen = fen,
      openingName = openingName,
      nature = nature,
      tension = tension,
      model = config.modelAsync,
      serviceTier = Some("flex"),
      lang = lang,
      maxOutputTokens = maxOutputTokens
    )

  def repairSync(
      originalProse: String,
      rejectedPolish: String,
      phase: String,
      evalDelta: Option[Int],
      concepts: List[String],
      fen: String,
      openingName: Option[String] = None,
      allowedSans: List[String] = Nil,
      lang: String = "en",
      maxOutputTokens: Option[Int] = None
  ): Future[Option[OpenAiPolishResult]] =
    repairWithFallback(
      originalProse = originalProse,
      rejectedPolish = rejectedPolish,
      phase = phase,
      evalDelta = evalDelta,
      concepts = concepts,
      fen = fen,
      openingName = openingName,
      allowedSans = allowedSans,
      model = config.modelSync,
      serviceTier = None,
      lang = lang,
      maxOutputTokens = maxOutputTokens
    )

  def repairAsync(
      originalProse: String,
      rejectedPolish: String,
      phase: String,
      evalDelta: Option[Int],
      concepts: List[String],
      fen: String,
      openingName: Option[String] = None,
      allowedSans: List[String] = Nil,
      lang: String = "en",
      maxOutputTokens: Option[Int] = None
  ): Future[Option[OpenAiPolishResult]] =
    repairWithFallback(
      originalProse = originalProse,
      rejectedPolish = rejectedPolish,
      phase = phase,
      evalDelta = evalDelta,
      concepts = concepts,
      fen = fen,
      openingName = openingName,
      allowedSans = allowedSans,
      model = config.modelAsync,
      serviceTier = Some("flex"),
      lang = lang,
      maxOutputTokens = maxOutputTokens
    )

  private def polishWithFallback(
      prose: String,
      phase: String,
      evalDelta: Option[Int],
      concepts: List[String],
      fen: String,
      openingName: Option[String],
      nature: Option[String],
      tension: Option[Double],
      model: String,
      serviceTier: Option[String],
      lang: String,
      maxOutputTokens: Option[Int]
  ): Future[Option[OpenAiPolishResult]] =
    if !config.enabled || prose.isBlank then Future.successful(None)
    else
      callModel(
        prose = prose,
        phase = phase,
        evalDelta = evalDelta,
        concepts = concepts,
        fen = fen,
        openingName = openingName,
        nature = nature,
        tension = tension,
        model = model,
        serviceTier = serviceTier,
        lang = lang,
        maxOutputTokens = maxOutputTokens
      ).flatMap {
        case some @ Some(_) => Future.successful(some)
        case None =>
          val fb = config.modelFallback.trim
          if fb.nonEmpty && fb != model then
            callModel(
              prose = prose,
              phase = phase,
              evalDelta = evalDelta,
              concepts = concepts,
              fen = fen,
              openingName = openingName,
              nature = nature,
              tension = tension,
              model = fb,
              serviceTier = serviceTier,
              lang = lang,
              maxOutputTokens = maxOutputTokens
            )
          else Future.successful(None)
      }

  private def repairWithFallback(
      originalProse: String,
      rejectedPolish: String,
      phase: String,
      evalDelta: Option[Int],
      concepts: List[String],
      fen: String,
      openingName: Option[String],
      allowedSans: List[String],
      model: String,
      serviceTier: Option[String],
      lang: String,
      maxOutputTokens: Option[Int]
  ): Future[Option[OpenAiPolishResult]] =
    if !config.enabled || originalProse.isBlank || rejectedPolish.isBlank then Future.successful(None)
    else
      callRepairModel(
        originalProse = originalProse,
        rejectedPolish = rejectedPolish,
        phase = phase,
        evalDelta = evalDelta,
        concepts = concepts,
        fen = fen,
        openingName = openingName,
        allowedSans = allowedSans,
        model = model,
        serviceTier = serviceTier,
        lang = lang,
        maxOutputTokens = maxOutputTokens
      ).flatMap {
        case some @ Some(_) => Future.successful(some)
        case None =>
          val fb = config.modelFallback.trim
          if fb.nonEmpty && fb != model then
            callRepairModel(
              originalProse = originalProse,
              rejectedPolish = rejectedPolish,
              phase = phase,
              evalDelta = evalDelta,
              concepts = concepts,
              fen = fen,
              openingName = openingName,
              allowedSans = allowedSans,
              model = fb,
              serviceTier = serviceTier,
              lang = lang,
              maxOutputTokens = maxOutputTokens
            )
          else Future.successful(None)
      }

  private def callModel(
      prose: String,
      phase: String,
      evalDelta: Option[Int],
      concepts: List[String],
      fen: String,
      openingName: Option[String],
      nature: Option[String],
      tension: Option[Double],
      model: String,
      serviceTier: Option[String],
      lang: String,
      maxOutputTokens: Option[Int]
  ): Future[Option[OpenAiPolishResult]] =
    val userPrompt = PolishPrompt.buildPolishPrompt(
      prose = prose,
      phase = phase,
      evalDelta = evalDelta,
      concepts = concepts,
      fen = fen,
      openingName = openingName,
      nature = nature,
      tension = tension
    )
    callModelWithPrompt(
      userPrompt = userPrompt,
      model = model,
      serviceTier = serviceTier,
      lang = lang,
      maxOutputTokens = maxOutputTokens
    )

  private def callRepairModel(
      originalProse: String,
      rejectedPolish: String,
      phase: String,
      evalDelta: Option[Int],
      concepts: List[String],
      fen: String,
      openingName: Option[String],
      allowedSans: List[String],
      model: String,
      serviceTier: Option[String],
      lang: String,
      maxOutputTokens: Option[Int]
  ): Future[Option[OpenAiPolishResult]] =
    val repairPrompt = PolishPrompt.buildRepairPrompt(
      originalProse = originalProse,
      rejectedPolish = rejectedPolish,
      phase = phase,
      evalDelta = evalDelta,
      concepts = concepts,
      fen = fen,
      openingName = openingName,
      allowedSans = allowedSans
    )
    callModelWithPrompt(
      userPrompt = repairPrompt,
      model = model,
      serviceTier = serviceTier,
      lang = lang,
      maxOutputTokens = maxOutputTokens
    )

  private def callModelWithPrompt(
      userPrompt: String,
      model: String,
      serviceTier: Option[String],
      lang: String,
      maxOutputTokens: Option[Int]
  ): Future[Option[OpenAiPolishResult]] =
    val completionCap = maxOutputTokens
      .filter(v => v >= 64 && v <= 2048)
      .getOrElse(config.maxOutputTokens)
    val baseBody = Json.obj(
      "model" -> model,
      "max_completion_tokens" -> completionCap,
      "prompt_cache_key" -> promptCacheKey(lang),
      "messages" -> Json.arr(
        Json.obj(
          "role" -> "system",
          "content" -> PolishPrompt.systemPrompt
        ),
        Json.obj(
          "role" -> "user",
          "content" -> userPrompt
        )
      ),
      "response_format" -> Json.obj(
        "type" -> "json_schema",
        "json_schema" -> Json.obj(
          "name" -> "polish_response_v1",
          "strict" -> true,
          "schema" -> responseSchema
        )
      )
    )

    // GPT-5 family currently rejects explicit non-default temperature values.
    // Let provider defaults apply to avoid 400 unsupported_value errors.
    val withTemperature =
      if supportsExplicitTemperature(model) then baseBody + ("temperature" -> JsNumber(config.temperature))
      else baseBody

    val withReasoningEffort =
      if isGpt5Model(model) then withTemperature + ("reasoning_effort" -> JsString("minimal"))
      else withTemperature

    val body = serviceTier.fold(withReasoningEffort)(tier => withReasoningEffort + ("service_tier" -> JsString(tier)))

    ws.url(config.endpoint)
      .withHttpHeaders(
        "Authorization" -> s"Bearer ${config.apiKey}",
        "Content-Type" -> "application/json"
      )
      .withRequestTimeout(config.requestTimeoutSeconds.seconds)
      .post(body)
      .map { response =>
        response.status match
          case 200 => parsePolishResponse(response.body[String], requestedModel = model)
          case 429 =>
            logger.warn(s"OpenAI rate limited for model=$model")
            None
          case status =>
            logger.warn(s"OpenAI API error status=$status model=$model body=${response.body[String].take(220)}")
            None
      }
      .recover { case NonFatal(e) =>
        logger.warn(s"OpenAI request failed for model=$model: ${e.getMessage}")
        None
      }

  private def supportsExplicitTemperature(model: String): Boolean =
    val m = Option(model).getOrElse("").trim.toLowerCase
    !m.startsWith("gpt-5")

  private def isGpt5Model(model: String): Boolean =
    Option(model).getOrElse("").trim.toLowerCase.startsWith("gpt-5")

  private def parsePolishResponse(body: String, requestedModel: String): Option[OpenAiPolishResult] =
    try
      val json = Json.parse(body)
      val directParsed =
        (json \ "choices" \ 0 \ "message" \ "parsed" \ "commentary").asOpt[String].map(_.trim).filter(_.nonEmpty)
      val contentOpt = extractMessageContent(json)
      val parsedFromContent = contentOpt.flatMap(parseCommentaryPayload)
      val wrapperDetected = contentOpt.exists(looksLikeJsonWrapper)
      val fallbackPlain = contentOpt.map(_.trim).filter(_.nonEmpty).filter(_ => !wrapperDetected)
      val wrapperRaw = contentOpt.map(_.trim).filter(_.nonEmpty).filter(_ => wrapperDetected)
      val polished = directParsed.orElse(parsedFromContent).orElse(fallbackPlain)

      val model = (json \ "model").asOpt[String].getOrElse(requestedModel)
      val promptTokens = (json \ "usage" \ "prompt_tokens").asOpt[Int]
      val cachedTokens = (json \ "usage" \ "prompt_tokens_details" \ "cached_tokens").asOpt[Int]
      val completionTokens =
        (json \ "usage" \ "completion_tokens")
          .asOpt[Int]
          .orElse((json \ "usage" \ "output_tokens").asOpt[Int])
      val finishReason = (json \ "choices" \ 0 \ "finish_reason").asOpt[String].getOrElse("")

      def buildResult(commentary: String, extraWarnings: List[String] = Nil): OpenAiPolishResult =
        val warnings = List.newBuilder[String]
        if finishReason.equalsIgnoreCase("length") then warnings += "truncated_output"
        if isLikelyTruncated(commentary) then warnings += "truncated_output"
        extraWarnings.foreach(warnings += _)
        val estimatedCost = estimateCostUsd(
          model = model,
          promptTokens = promptTokens,
          cachedTokens = cachedTokens,
          completionTokens = completionTokens
        )
        OpenAiPolishResult(
          commentary = commentary,
          model = model,
          promptCacheHit = cachedTokens.exists(_ > 0),
          promptTokens = promptTokens,
          cachedTokens = cachedTokens,
          completionTokens = completionTokens,
          estimatedCostUsd = estimatedCost,
          parseWarnings = warnings.result().distinct
        )

      polished
        .map(buildResult(_))
        .orElse {
          // Keep wrapper text to allow strict validation+repair path in LlmApi
          // instead of dropping directly to fallback_rule_empty.
          wrapperRaw.map(raw => buildResult(raw, extraWarnings = List("json_wrapper_unparsed")))
        }
    catch
      case NonFatal(e) =>
        logger.warn(s"Failed to parse OpenAI response: ${e.getMessage}")
        None

  private def extractMessageContent(json: JsValue): Option[String] =
    (json \ "choices" \ 0 \ "message" \ "content").toOption.flatMap {
      case JsString(text) => Some(text.trim).filter(_.nonEmpty)
      case obj: JsObject =>
        List("commentary", "text", "content", "output_text")
          .view
          .flatMap(key => (obj \ key).asOpt[String].map(_.trim))
          .find(_.nonEmpty)
      case JsArray(parts) =>
        val merged = parts
          .flatMap {
            case JsString(text) => Some(text.trim)
            case obj: JsObject =>
              List("commentary", "text", "content", "output_text")
                .view
                .flatMap(key => (obj \ key).asOpt[String].map(_.trim))
                .find(_.nonEmpty)
            case _ => None
          }
          .filter(_.nonEmpty)
          .mkString("\n")
          .trim
        Option(merged).filter(_.nonEmpty)
      case _ => None
    }

  private def parseCommentaryPayload(raw: String): Option[String] =
    val trimmed = Option(raw).map(_.trim).getOrElse("")
    if trimmed.isEmpty then None
    else
      def extractCommentary(parsed: JsValue): Option[String] =
        val direct = (parsed \ "commentary").asOpt[String].map(_.trim).filter(_.nonEmpty)
        val nested =
          (parsed \ "commentary").toOption.flatMap {
            case obj: JsObject =>
              List("text", "value", "content")
                .view
                .flatMap(k => (obj \ k).asOpt[String].map(_.trim))
                .find(_.nonEmpty)
            case _ => None
          }
        direct.orElse(nested)

      def parseJson(text: String): Option[String] =
        try extractCommentary(Json.parse(text))
        catch case NonFatal(_) => None

      val fenceRegex = """(?s)```(?:json)?\s*(\{.*?\})\s*```""".r
      val fenced = fenceRegex.findFirstMatchIn(trimmed).map(_.group(1).trim)
      val objectSlice =
        val start = trimmed.indexOf('{')
        val end = trimmed.lastIndexOf('}')
        if start >= 0 && end > start then Some(trimmed.substring(start, end + 1).trim)
        else None

      List(Some(trimmed), fenced, objectSlice).flatten.distinct.view.flatMap(parseJson).headOption

  private def looksLikeJsonWrapper(text: String): Boolean =
    val t = Option(text).map(_.trim).getOrElse("")
    t.startsWith("{") && t.contains("\"commentary\"")

  private def isLikelyTruncated(text: String): Boolean =
    val t = Option(text).map(_.trim).getOrElse("")
    if t.isEmpty then false
    else
      def balanced(open: Char, close: Char): Boolean =
        t.count(_ == open) == t.count(_ == close)
      val quoteCount = t.count(_ == '"')
      !balanced('{', '}') || !balanced('[', ']') || (quoteCount % 2 != 0) || t.endsWith("\\") ||
        MoveAnchorCodec.hasBrokenAnchorPrefix(t)

  private case class ModelPricing(inputPerM: Double, cachedInputPerM: Double, outputPerM: Double)

  private val pricingByPrefix: List[(String, ModelPricing)] = List(
    "gpt-4o-mini" -> ModelPricing(inputPerM = 0.150, cachedInputPerM = 0.075, outputPerM = 0.600),
    "gpt-5-nano" -> ModelPricing(inputPerM = 0.050, cachedInputPerM = 0.005, outputPerM = 0.400)
  )

  private def pricingForModel(model: String): Option[ModelPricing] =
    val normalized = Option(model).getOrElse("").trim.toLowerCase
    pricingByPrefix.collectFirst { case (prefix, pricing) if normalized.startsWith(prefix) => pricing }

  private def estimateCostUsd(
      model: String,
      promptTokens: Option[Int],
      cachedTokens: Option[Int],
      completionTokens: Option[Int]
  ): Option[Double] =
    for
      pricing <- pricingForModel(model)
      prompt <- promptTokens
      completion <- completionTokens
    yield
      val cached = cachedTokens.getOrElse(0).max(0)
      val nonCachedPrompt = (prompt - cached).max(0)
      val inputCost = (nonCachedPrompt.toDouble / 1000000.0) * pricing.inputPerM
      val cachedCost = (cached.toDouble / 1000000.0) * pricing.cachedInputPerM
      val outputCost = (completion.toDouble / 1000000.0) * pricing.outputPerM
      inputCost + cachedCost + outputCost

  private def promptCacheKey(lang: String): String =
    val normalizedLang = Option(lang).getOrElse("en").trim.toLowerCase match
      case "" => "en"
      case x  => x.take(8)
    s"${config.promptCacheKeyPrefix}:$normalizedLang"
