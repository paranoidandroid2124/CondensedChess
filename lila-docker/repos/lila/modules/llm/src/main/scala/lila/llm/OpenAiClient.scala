package lila.llm

import play.api.libs.json.*
import play.api.libs.ws.StandaloneWSClient
import play.api.libs.ws.JsonBodyWritables.*
import play.api.libs.ws.DefaultBodyReadables.*
import scala.concurrent.Future
import scala.concurrent.duration.*
import scala.util.control.NonFatal
import lila.llm.analysis.BookmakerPolishSlots

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
      s"OpenAI polish enabled: sync=${config.modelSync}, async=${config.modelAsync}, fallback=${config.modelFallback}, " +
        s"pro_sync=${config.modelProSync}, pro_async=${config.modelProAsync}, pro_fallback=${config.modelProFallback}, " +
        s"max_output_tokens=${config.maxOutputTokens}"
    )
  else logger.info("OpenAI polish disabled (no API key)")

  private case class ModelRoute(
      primary: String,
      fallback: Option[String],
      serviceTier: Option[String]
  )

  def isEnabled: Boolean = config.enabled
  def syncModelName(planTier: String = PlanTier.Basic, llmLevel: String = LlmLevel.Polish): String =
    selectModelRoute(asyncTier = false, planTier = planTier, llmLevel = llmLevel).primary

  def asyncModelName(planTier: String = PlanTier.Basic, llmLevel: String = LlmLevel.Polish): String =
    selectModelRoute(asyncTier = true, planTier = planTier, llmLevel = llmLevel).primary

  private def clean(value: String): Option[String] =
    Option(value).map(_.trim).filter(_.nonEmpty)

  private def selectModelRoute(
      asyncTier: Boolean,
      planTier: String,
      llmLevel: String
  ): ModelRoute =
    val normalizedTier = PlanTier.normalize(planTier)
    val normalizedLevel = LlmLevel.normalize(llmLevel)
    val wantsPro = normalizedTier == PlanTier.Pro && normalizedLevel == LlmLevel.Active
    val defaultPrimary =
      if asyncTier then clean(config.modelAsync)
      else clean(config.modelSync)
    val defaultFallback = clean(config.modelFallback)
    val proPrimary =
      if asyncTier then clean(config.modelProAsync)
      else clean(config.modelProSync)
    val proFallback = clean(config.modelProFallback).orElse(defaultFallback)
    val chosenPrimary = if wantsPro then proPrimary.orElse(defaultPrimary) else defaultPrimary
    val primary = chosenPrimary.getOrElse("gpt-4o-mini")
    val fallback =
      if wantsPro then proFallback.filter(_ != primary)
      else defaultFallback.filter(_ != primary)
    ModelRoute(
      primary = primary,
      fallback = fallback,
      serviceTier = Option.when(asyncTier)("flex")
    )

  def polishSync(
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
      lang: String = "en",
      maxOutputTokens: Option[Int] = None,
      planTier: String = PlanTier.Basic,
      llmLevel: String = LlmLevel.Polish,
      bookmakerSlots: Option[BookmakerPolishSlots] = None
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
      salience = salience,
      momentType = momentType,
      asyncTier = false,
      planTier = planTier,
      llmLevel = llmLevel,
      lang = lang,
      maxOutputTokens = maxOutputTokens,
      bookmakerSlots = bookmakerSlots
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
      salience: Option[lila.llm.model.strategic.StrategicSalience] = None,
      momentType: Option[String] = None,
      lang: String = "en",
      maxOutputTokens: Option[Int] = None,
      planTier: String = PlanTier.Basic,
      llmLevel: String = LlmLevel.Polish,
      bookmakerSlots: Option[BookmakerPolishSlots] = None
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
      salience = salience,
      momentType = momentType,
      asyncTier = true,
      planTier = planTier,
      llmLevel = llmLevel,
      lang = lang,
      maxOutputTokens = maxOutputTokens,
      bookmakerSlots = bookmakerSlots
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
      maxOutputTokens: Option[Int] = None,
      planTier: String = PlanTier.Basic,
      llmLevel: String = LlmLevel.Polish,
      bookmakerSlots: Option[BookmakerPolishSlots] = None
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
      asyncTier = false,
      planTier = planTier,
      llmLevel = llmLevel,
      lang = lang,
      maxOutputTokens = maxOutputTokens,
      bookmakerSlots = bookmakerSlots
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
      maxOutputTokens: Option[Int] = None,
      planTier: String = PlanTier.Basic,
      llmLevel: String = LlmLevel.Polish,
      bookmakerSlots: Option[BookmakerPolishSlots] = None
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
      asyncTier = true,
      planTier = planTier,
      llmLevel = llmLevel,
      lang = lang,
      maxOutputTokens = maxOutputTokens,
      bookmakerSlots = bookmakerSlots
    )

  def activeStrategicNoteSync(
      baseNarrative: String,
      phase: String,
      momentType: String,
      concepts: List[String],
      fen: String,
      strategyPack: Option[StrategyPack],
      dossier: Option[ActiveBranchDossier] = None,
      routeRefs: List[ActiveStrategicRouteRef] = Nil,
      moveRefs: List[ActiveStrategicMoveRef] = Nil,
      lang: String = "en",
      maxOutputTokens: Option[Int] = None,
      planTier: String = PlanTier.Pro,
      llmLevel: String = LlmLevel.Active
  ): Future[Option[OpenAiPolishResult]] =
    activeStrategicNoteWithFallback(
      baseNarrative = baseNarrative,
      phase = phase,
      momentType = momentType,
      concepts = concepts,
      fen = fen,
      strategyPack = strategyPack,
      dossier = dossier,
      routeRefs = routeRefs,
      moveRefs = moveRefs,
      asyncTier = false,
      planTier = planTier,
      llmLevel = llmLevel,
      lang = lang,
      maxOutputTokens = maxOutputTokens
    )

  def activeStrategicNoteAsync(
      baseNarrative: String,
      phase: String,
      momentType: String,
      concepts: List[String],
      fen: String,
      strategyPack: Option[StrategyPack],
      dossier: Option[ActiveBranchDossier] = None,
      routeRefs: List[ActiveStrategicRouteRef] = Nil,
      moveRefs: List[ActiveStrategicMoveRef] = Nil,
      lang: String = "en",
      maxOutputTokens: Option[Int] = None,
      planTier: String = PlanTier.Pro,
      llmLevel: String = LlmLevel.Active
  ): Future[Option[OpenAiPolishResult]] =
    activeStrategicNoteWithFallback(
      baseNarrative = baseNarrative,
      phase = phase,
      momentType = momentType,
      concepts = concepts,
      fen = fen,
      strategyPack = strategyPack,
      dossier = dossier,
      routeRefs = routeRefs,
      moveRefs = moveRefs,
      asyncTier = true,
      planTier = planTier,
      llmLevel = llmLevel,
      lang = lang,
      maxOutputTokens = maxOutputTokens
    )

  def repairActiveStrategicNoteSync(
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
      moveRefs: List[ActiveStrategicMoveRef] = Nil,
      lang: String = "en",
      maxOutputTokens: Option[Int] = None,
      planTier: String = PlanTier.Pro,
      llmLevel: String = LlmLevel.Active
  ): Future[Option[OpenAiPolishResult]] =
    repairActiveStrategicNoteWithFallback(
      baseNarrative = baseNarrative,
      rejectedNote = rejectedNote,
      failureReasons = failureReasons,
      phase = phase,
      momentType = momentType,
      concepts = concepts,
      fen = fen,
      strategyPack = strategyPack,
      dossier = dossier,
      routeRefs = routeRefs,
      moveRefs = moveRefs,
      asyncTier = false,
      planTier = planTier,
      llmLevel = llmLevel,
      lang = lang,
      maxOutputTokens = maxOutputTokens
    )

  def repairActiveStrategicNoteAsync(
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
      moveRefs: List[ActiveStrategicMoveRef] = Nil,
      lang: String = "en",
      maxOutputTokens: Option[Int] = None,
      planTier: String = PlanTier.Pro,
      llmLevel: String = LlmLevel.Active
  ): Future[Option[OpenAiPolishResult]] =
    repairActiveStrategicNoteWithFallback(
      baseNarrative = baseNarrative,
      rejectedNote = rejectedNote,
      failureReasons = failureReasons,
      phase = phase,
      momentType = momentType,
      concepts = concepts,
      fen = fen,
      strategyPack = strategyPack,
      dossier = dossier,
      routeRefs = routeRefs,
      moveRefs = moveRefs,
      asyncTier = true,
      planTier = planTier,
      llmLevel = llmLevel,
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
      salience: Option[lila.llm.model.strategic.StrategicSalience],
      momentType: Option[String],
      asyncTier: Boolean,
      planTier: String,
      llmLevel: String,
      lang: String,
      maxOutputTokens: Option[Int],
      bookmakerSlots: Option[BookmakerPolishSlots]
  ): Future[Option[OpenAiPolishResult]] =
    if !config.enabled || prose.isBlank then Future.successful(None)
    else
      val route = selectModelRoute(asyncTier = asyncTier, planTier = planTier, llmLevel = llmLevel)
      callModel(
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
        model = route.primary,
        serviceTier = route.serviceTier,
        lang = lang,
        maxOutputTokens = maxOutputTokens,
        bookmakerSlots = bookmakerSlots
      ).flatMap {
        case some @ Some(_) => Future.successful(some)
        case None =>
          route.fallback match
            case Some(fb) =>
              callModel(
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
                model = fb,
                serviceTier = route.serviceTier,
                lang = lang,
                maxOutputTokens = maxOutputTokens,
                bookmakerSlots = bookmakerSlots
              )
            case None =>
              Future.successful(None)
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
      asyncTier: Boolean,
      planTier: String,
      llmLevel: String,
      lang: String,
      maxOutputTokens: Option[Int],
      bookmakerSlots: Option[BookmakerPolishSlots]
  ): Future[Option[OpenAiPolishResult]] =
    if !config.enabled || originalProse.isBlank || rejectedPolish.isBlank then Future.successful(None)
    else
      val route = selectModelRoute(asyncTier = asyncTier, planTier = planTier, llmLevel = llmLevel)
      callRepairModel(
        originalProse = originalProse,
        rejectedPolish = rejectedPolish,
        phase = phase,
        evalDelta = evalDelta,
        concepts = concepts,
        fen = fen,
        openingName = openingName,
        allowedSans = allowedSans,
        model = route.primary,
        serviceTier = route.serviceTier,
        lang = lang,
        maxOutputTokens = maxOutputTokens,
        bookmakerSlots = bookmakerSlots
      ).flatMap {
        case some @ Some(_) => Future.successful(some)
        case None =>
          route.fallback match
            case Some(fb) =>
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
                serviceTier = route.serviceTier,
                lang = lang,
                maxOutputTokens = maxOutputTokens,
                bookmakerSlots = bookmakerSlots
              )
            case None =>
              Future.successful(None)
      }

  private def activeStrategicNoteWithFallback(
      baseNarrative: String,
      phase: String,
      momentType: String,
      concepts: List[String],
      fen: String,
      strategyPack: Option[StrategyPack],
      dossier: Option[ActiveBranchDossier],
      routeRefs: List[ActiveStrategicRouteRef],
      moveRefs: List[ActiveStrategicMoveRef],
      asyncTier: Boolean,
      planTier: String,
      llmLevel: String,
      lang: String,
      maxOutputTokens: Option[Int]
  ): Future[Option[OpenAiPolishResult]] =
    if !config.enabled || baseNarrative.isBlank then Future.successful(None)
    else
      val route = selectModelRoute(asyncTier = asyncTier, planTier = planTier, llmLevel = llmLevel)
      callActiveStrategicNoteModel(
        baseNarrative = baseNarrative,
        phase = phase,
        momentType = momentType,
        concepts = concepts,
        fen = fen,
        strategyPack = strategyPack,
        dossier = dossier,
        routeRefs = routeRefs,
        moveRefs = moveRefs,
        model = route.primary,
        serviceTier = route.serviceTier,
        lang = lang,
        maxOutputTokens = maxOutputTokens
      ).flatMap {
        case some @ Some(_) => Future.successful(some)
        case None =>
          route.fallback match
            case Some(fb) =>
              callActiveStrategicNoteModel(
                baseNarrative = baseNarrative,
                phase = phase,
                momentType = momentType,
                concepts = concepts,
                fen = fen,
                strategyPack = strategyPack,
                dossier = dossier,
                routeRefs = routeRefs,
                moveRefs = moveRefs,
                model = fb,
                serviceTier = route.serviceTier,
                lang = lang,
                maxOutputTokens = maxOutputTokens
              )
            case None => Future.successful(None)
      }

  private def repairActiveStrategicNoteWithFallback(
      baseNarrative: String,
      rejectedNote: String,
      failureReasons: List[String],
      phase: String,
      momentType: String,
      concepts: List[String],
      fen: String,
      strategyPack: Option[StrategyPack],
      dossier: Option[ActiveBranchDossier],
      routeRefs: List[ActiveStrategicRouteRef],
      moveRefs: List[ActiveStrategicMoveRef],
      asyncTier: Boolean,
      planTier: String,
      llmLevel: String,
      lang: String,
      maxOutputTokens: Option[Int]
  ): Future[Option[OpenAiPolishResult]] =
    if !config.enabled || baseNarrative.isBlank || rejectedNote.isBlank then Future.successful(None)
    else
      val route = selectModelRoute(asyncTier = asyncTier, planTier = planTier, llmLevel = llmLevel)
      callRepairActiveStrategicNoteModel(
        baseNarrative = baseNarrative,
        rejectedNote = rejectedNote,
        failureReasons = failureReasons,
        phase = phase,
        momentType = momentType,
        concepts = concepts,
        fen = fen,
        strategyPack = strategyPack,
        dossier = dossier,
        routeRefs = routeRefs,
        moveRefs = moveRefs,
        model = route.primary,
        serviceTier = route.serviceTier,
        lang = lang,
        maxOutputTokens = maxOutputTokens
      ).flatMap {
        case some @ Some(_) => Future.successful(some)
        case None =>
          route.fallback match
            case Some(fb) =>
              callRepairActiveStrategicNoteModel(
                baseNarrative = baseNarrative,
                rejectedNote = rejectedNote,
                failureReasons = failureReasons,
                phase = phase,
                momentType = momentType,
                concepts = concepts,
                fen = fen,
                strategyPack = strategyPack,
                dossier = dossier,
                routeRefs = routeRefs,
                moveRefs = moveRefs,
                model = fb,
                serviceTier = route.serviceTier,
                lang = lang,
                maxOutputTokens = maxOutputTokens
              )
            case None => Future.successful(None)
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
      salience: Option[lila.llm.model.strategic.StrategicSalience],
      momentType: Option[String],
      model: String,
      serviceTier: Option[String],
      lang: String,
      maxOutputTokens: Option[Int],
      bookmakerSlots: Option[BookmakerPolishSlots]
  ): Future[Option[OpenAiPolishResult]] =
    val _ = momentType
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
    callModelWithPrompt(
      userPrompt = userPrompt,
      model = model,
      serviceTier = serviceTier,
      lang = lang,
      maxOutputTokens = maxOutputTokens
    )

  private def callActiveStrategicNoteModel(
      baseNarrative: String,
      phase: String,
      momentType: String,
      concepts: List[String],
      fen: String,
      strategyPack: Option[StrategyPack],
      dossier: Option[ActiveBranchDossier],
      routeRefs: List[ActiveStrategicRouteRef],
      moveRefs: List[ActiveStrategicMoveRef],
      model: String,
      serviceTier: Option[String],
      lang: String,
      maxOutputTokens: Option[Int]
  ): Future[Option[OpenAiPolishResult]] =
    val userPrompt = ActiveStrategicPrompt.buildPrompt(
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
    callModelWithPrompt(
      userPrompt = userPrompt,
      model = model,
      serviceTier = serviceTier,
      lang = lang,
      maxOutputTokens = maxOutputTokens,
      systemPrompt = ActiveStrategicPrompt.systemPrompt
    )

  private def callRepairActiveStrategicNoteModel(
      baseNarrative: String,
      rejectedNote: String,
      failureReasons: List[String],
      phase: String,
      momentType: String,
      concepts: List[String],
      fen: String,
      strategyPack: Option[StrategyPack],
      dossier: Option[ActiveBranchDossier],
      routeRefs: List[ActiveStrategicRouteRef],
      moveRefs: List[ActiveStrategicMoveRef],
      model: String,
      serviceTier: Option[String],
      lang: String,
      maxOutputTokens: Option[Int]
  ): Future[Option[OpenAiPolishResult]] =
    val userPrompt = ActiveStrategicPrompt.buildRepairPrompt(
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
    callModelWithPrompt(
      userPrompt = userPrompt,
      model = model,
      serviceTier = serviceTier,
      lang = lang,
      maxOutputTokens = maxOutputTokens,
      systemPrompt = ActiveStrategicPrompt.systemPrompt
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
      maxOutputTokens: Option[Int],
      bookmakerSlots: Option[BookmakerPolishSlots]
  ): Future[Option[OpenAiPolishResult]] =
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
      maxOutputTokens: Option[Int],
      systemPrompt: String = PolishPrompt.systemPrompt
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
          "content" -> systemPrompt
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
