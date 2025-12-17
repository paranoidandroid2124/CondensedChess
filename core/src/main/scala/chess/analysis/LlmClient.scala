package chess
package analysis

import java.net.http.{ HttpClient, HttpRequest, HttpResponse }
import java.net.URI
import java.util.concurrent.{Executors, ScheduledThreadPoolExecutor, TimeUnit}
import scala.concurrent.{Future, Promise, ExecutionContext}
import scala.jdk.FutureConverters.*

import ujson.*

/** Minimal Gemini text client using REST API (Async).
  *
  * Requires GEMINI_API_KEY in env. Model defaults to gemini-1.5-flash.
  */
object LlmClient:
  // Using a small thread pool for HTTP callbacks and JSON parsing
  private given ec: ExecutionContext = ExecutionContext.fromExecutor(Executors.newFixedThreadPool(4))
  private val scheduler = new ScheduledThreadPoolExecutor(1)

  private val apiKey = EnvLoader.get("GEMINI_API_KEY").orElse(sys.props.get("GEMINI_API_KEY"))
  if apiKey.isEmpty then System.err.println("[llm] GEMINI_API_KEY not set; LlmClient is disabled")

  private val model = EnvLoader.getOrElse("GEMINI_MODEL", "gemini-2.5-flash")
  private val endpoint = s"https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent?key="
  private val http = HttpClient.newBuilder().executor(Executors.newFixedThreadPool(4)).build()

  // Non-blocking delay helper
  private def delay(ms: Long): Future[Unit] =
    val p = Promise[Unit]()
    scheduler.schedule((() => p.success(())), ms, TimeUnit.MILLISECONDS)
    p.future

  private def sendWithRetry(req: HttpRequest, retries: Int = 3, delayMs: Int = 1000): Future[HttpResponse[String]] =
    val start = System.currentTimeMillis()
    
    // Using asScala to convert Java CompletableFuture to Scala Future
    http.sendAsync(req, HttpResponse.BodyHandlers.ofString()).asScala.flatMap { res =>
      val duration = System.currentTimeMillis() - start
      if duration > 2000 then
        System.err.println(s"[LlmClient] Request took ${duration}ms (status ${res.statusCode()})")
      
      if (res.statusCode() == 429 || res.statusCode() >= 500) && retries > 0 then
        System.err.println(s"[LlmClient] Rate limit/Server error (${res.statusCode()}). Retrying in ${delayMs}ms...")
        delay(delayMs).flatMap(_ => sendWithRetry(req, retries - 1, delayMs * 2))
      else
        Future.successful(res)
    }.recoverWith {
      case e: Throwable if retries > 0 =>
        System.err.println(s"[LlmClient] Network error: ${e.getMessage}. Retrying in ${delayMs}ms...")
        delay(delayMs).flatMap(_ => sendWithRetry(req, retries - 1, delayMs * 2))
    }

  def summarize(json: String): Future[Option[String]] =
    apiKey match
      case None => Future.successful(None)
      case Some(key) =>
        val prompt =
          s"""You are a chess coach. Summarize this chess review JSON for a human in 3-6 sentences.
             |- Payload contains an "instructions" block; follow it plus these rules.
             |- Use only moves/evals given. Do NOT invent moves/evals or move numbers.
             |- Do NOT use the word 'ply'; refer to moves by standard notation (e.g., "13. Qe2").
             |- When referenceing moves, quote the provided label exactly (e.g., "13. Qe2", "12...Ba6"); never renumber.
             |- Use keySwings/critical arrays to anchor the story. Mention forced/only-move flags, legalMoves, and best-vs-second gaps when present.
             |- Lean on semanticTags/mistakeCategory (tactical_miss/greedy/positional_trade_error/ignored_threat) and conceptShift to explain *why* the eval moved.
             |- Mention novelty/book briefly using openingStats if present.
             |- Avoid generic advice; ground every claim in the provided tags/evals/branches/oppositeColorBishops flags.
             |JSON: $json""".stripMargin
        val body = s"""{"contents":[{"parts":[{"text":${quote(prompt)}}]}]}"""
        
        buildAndSend(key, body, "[llm-summarize]") { text =>
          Future.successful(Some(text))
        }

  // Generic helper to build request, send, and parse text
  private def buildAndSend[T](key: String, body: String, tag: String)(parser: String => Future[Option[T]]): Future[Option[T]] =
    val req = HttpRequest.newBuilder()
      .uri(URI.create(endpoint + key))
      .header("Content-Type", "application/json")
      .timeout(java.time.Duration.ofSeconds(30))
      .POST(HttpRequest.BodyPublishers.ofString(body))
      .build()

    sendWithRetry(req).flatMap { res =>
      if res.statusCode() >= 200 && res.statusCode() < 300 then
        extractText(res.body()) match
          case Some(text) => parser(text)
          case None => Future.successful(None)
      else
        System.err.println(s"$tag status=${res.statusCode()} body=${res.body()}")
        Future.successful(None)
    }.recover {
      case e: Throwable =>
        System.err.println(s"$tag request failed: ${e.getMessage}")
        None
    }

  private def extractText(response: String): Option[String] =
    try
      val json = ujson.read(response)
      val candidates = json.obj.get("candidates").flatMap(_.arrOpt).getOrElse(scala.collection.mutable.ArrayBuffer.empty)
      
      candidates.headOption.flatMap { cand =>
        val finishReason = cand.obj.get("finishReason").map(_.str).getOrElse("UNKNOWN")
        if finishReason != "STOP" then System.err.println(s"[llm] finishReason: $finishReason")

        for
          content <- cand.obj.get("content")
          parts <- content.obj.get("parts").flatMap(_.arrOpt)
          firstPart <- parts.headOption
          text <- firstPart.obj.get("text").map(_.str)
        yield text
      }
    catch
      case e: Throwable =>
        System.err.println(s"[llm] failed to parse response: ${e.getMessage}")
        None

  def quote(in: String): String =
    "\"" + in.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n") + "\""

  def shortComments(payload: String): Future[Map[Int, String]] =
    apiKey match
      case None => Future.successful(Map.empty)
      case Some(key) =>
        val prompt =
          s"""You are a chess engine. Analyze the JSON payload and return a JSON array of objects:
             |[{"ply": 12, "shortComment": "..."}]
             |Rules:
             |- Only comment on moves present in the payload.
             |- Keep comments short and factual.
             |- Reference moves by standard notation (e.g. '13. Qe2').
             |Payload: $payload""".stripMargin
        
        val body = s"""{"contents":[{"parts":[{"text":${quote(prompt)}}]}], "generationConfig":{"responseMimeType":"application/json"}}"""
        
        buildAndSend(key, body, "[llm-shortComments]") { text =>
          Future.successful(Some(parseShortCommentsJson(text)))
        }.map(_.getOrElse(Map.empty))

  private def parseShortCommentsJson(text: String): Map[Int, String] =
    try
      val arr = ujson.read(text).arr
      arr.flatMap { v =>
        for
          ply <- v.obj.get("ply").map(_.num.toInt)
          comment <- v.obj.get("shortComment").map(_.str)
        yield ply -> comment
      }.toMap
    catch case _: Throwable => Map.empty

  case class CriticalAnnotation(main: String, variations: Map[String, String])

  def criticalComments(payload: String): Future[Map[Int, CriticalAnnotation]] =
    apiKey match
      case None => Future.successful(Map.empty)
      case Some(key) =>
        val prompt =
          s"""You are a friendly but sharp Chess Coach. Analyze these critical moments and provide a "Coach's Insight" for each.
             |**STYLE RULES:**
             |- Write like a chess book, not a computer report.
             |- ALWAYS cite specific moves (e.g., "15.Nxe5") and squares, but ONLY if they are explicitly in the source data.
             |- **NO ORIGIN ASSUMPTIONS**: If input says "Nxe5", do NOT write "Knight from f3 captures..." unless you know it was on f3.
             |- Use natural chess language: "wins material", "creates threats", "gains space".
             |- NEVER use: win percentages, "multiPV", "eval", "tags" as words.
             |- **NATURALIZE TAGS**: Translate technical tags (e.g., "Pawn Storm", "King Safety Crisis") into rich, descriptive prose (e.g., "a furious pawn avalanche", "the king is in mortal danger"). NEVER output the raw tag name like "Tactical Storm".
             |
             |Your Goal:
             |Explain *why* the played move was a mistake and *what* the student should have played instead, citing the Best Move's specific continuation.
             |**ALSO:** Provide a brief, 1-sentence comment for the alternative variations (Best Move, etc.) to explain their point.
             |
             |**SAFETY RULES (CRITICAL):**
             |1. **NO GEOMETRY GUESSES**: Do NOT claim a piece attacks/defends a square unless it is OBVIOUS from the PV.
             |2. **NO ORIGIN ASSUMPTIONS**: Never say "removed the knight from f6" unless the move is explicitly "Nxd5".
             |3. **PV INTEGRITY**: When citing variations, use the exact moves provided. Do not invent move numbers.
             |
             |Input Payload: $payload
             |
             |Output Format:
             |JSON array: [{"ply": 12, "main": "Coach comment on the played move...", "variations": {"Best Move": "Explanation of why this wins...", "Hypothesis: ...": "Refutation or reason..."}}]
             |""".stripMargin
        
        val body = s"""{"contents":[{"parts":[{"text":${quote(prompt)}}]}], "generationConfig":{"responseMimeType":"application/json"}}"""
        
        buildAndSend(key, body, "[llm-critical]") { text =>
          Future.successful(Some(parseCriticalJson(text)))
        }.map(_.getOrElse(Map.empty))

  private def parseCriticalJson(text: String): Map[Int, CriticalAnnotation] =
    try
      val arr = ujson.read(text).arr
      arr.flatMap { v =>
        for
          ply <- v.obj.get("ply").map(_.num.toInt)
          main <- v.obj.get("main").map(_.str).orElse(v.obj.get("comment").map(_.str))
          vars = v.obj.get("variations").flatMap(_.objOpt).map(_.toMap.mapValues(_.str).toMap).getOrElse(Map.empty)
        yield ply -> CriticalAnnotation(main, vars)
      }.toMap
    catch case _: Throwable => Map.empty

  def studyChapterComments(payload: String): Future[Map[String, ChapterAnnotation]] =
    apiKey match
      case None => Future.successful(Map.empty)
      case Some(key) =>
        val hasHypothesis = payload.contains("Hypothesis") || payload.contains("Greedy Capture")
        val hypothesisInstruction = if (hasHypothesis) then
          """
          |   - **Hypothesis Analysis (CRITICAL):**
          |     * You will see lines labeled "Hypothesis" (e.g., "Hypothesis: Nf6").
          |     * You MUST explain WHY this move is bad (refutation).
          |     * Use the phrase: "Black might consider [Move], but it fails to [Refutation]..."
          |     * If a line is labeled "Greedy Capture", explain why taking the material is a mistake.
          |""" else ""

        val hypothesisTemplate = if (hasHypothesis) then
          """
          |2. (Hypothesis/Trap): "Black might consider **[Hypothesis Move]**, but it fails to **[Refutation Move]**. After the dust settles, White is winning because..."
          |""" else ""

        val prompt =
          s"""You are a Professional Chess Author writing a high-quality game collection book.
             |
             |PAYLOAD (per chapter):
             |$payload
             |
             |GOAL:
             |Write a "Mini-Chapter" that feels like a polished chess book.
             |
             |**STRICT EDITORIAL RULES:**
             |1. **Tone & Style (Data-Driven):**
             |   - **CRITICAL:** Use the tags provided in the data (e.g., "adj:Fatal", "mood:Critical") to determine the intensity of your language.
             |   - If the tag is "adj:Fatal", use words like "catastrophic", "deadly".
             |   - If the tag is "adj:Solid", use words like "steady", "sensible".
             |   - **DO NOT** invent your own intensity judgment. Trust the tags.
             |$hypothesisInstruction
             |3. **Glossary Enforcement:**
             |   - Use "Space Advantage" (not "big space").
             |   - Use "Initiative" (not "attack power").
             |   - Use "Playable" (not "okay move").
             |4. **Formatting:**
             |   - **BOLD** ALL algebraic notation moves (e.g., **Nf3**, **e4**, **15...Re8**).
             |
             |**OUTPUT FORMAT:**
             |Return a JSON ARRAY of objects (one for each studyChapter provided):
             |[
             |  {
             |    "miniChapter": {
             |      "id": "Copy the EXACT 'id' from the input chapter",
             |      "title": "A short, punchy title (e.g., The Greedy Trap)",
             |      "summary": "A 2-3 sentence narrative summary. Focus on the psychological or strategic context.",
             |      "keyMoves": [
             |         { "move": "15...Nf6", "commentary": "..." }
             |      ]
             |    }
             |  }
             |]
             |
             |**STYLE TEMPLATES:**
             |1. (Lesson): "**[Move]** is a strong idea. It addresses strategic concerns..."
             |$hypothesisTemplate
             |
             |**CRITICAL SAFETY RAIL (HALLUCINATION CHECK):**
             | - Do NOT invent functionality or rules not present in chess.
             | - **Length Guideline:** Keep comments concise (1-3 sentences) for normal moves. However, for **Hypothesis Refutations** or **Critical Turning Points**, you may write up to 5-6 sentences to fully explain the strategic nuance. Depth is allowed where necessary.
             |""".stripMargin

        val fullPrompt = s"$prompt\n\nPayload: $payload"
        val body = s"""{"contents":[{"parts":[{"text":${quote(fullPrompt)}}]}], "generationConfig":{"responseMimeType":"application/json"}}"""
        
        buildAndSend(key, body, "[llm-study]") { text =>
          Future.successful(Some(parseChapterAnnotationJson(text)))
        }.map(_.getOrElse(Map.empty))

  case class ChapterAnnotation(title: String, summary: String, keyMoves: Map[(Int, String), String])

  private def parseChapterAnnotationJson(body: String): Map[String, ChapterAnnotation] =
    try
      val parsed = ujson.read(body)
      val arr = parsed match
        case a: ujson.Arr => a.arr
        case o: ujson.Obj => scala.collection.mutable.ArrayBuffer(o)
        case _ => scala.collection.mutable.ArrayBuffer.empty
      
      arr.iterator.flatMap { v =>
        val obj = if (v.obj.contains("miniChapter")) v("miniChapter").obj else v.obj
        val idOpt = obj.get("id").map(_.str)
        val titleOpt = obj.get("title").map(_.str).orElse(Some("Chapter"))
        val summaryOpt = obj.get("summary").map(_.str)
        
        val keyMovesMap = obj.get("keyMoves")
          .flatMap(_.arrOpt)
          .map { movesArr =>
            movesArr.flatMap { m =>
              for
                ply <- m.obj.get("ply").map(_.num.toInt)
                san <- m.obj.get("san").map(_.str)
                comment <- m.obj.get("comment").map(_.str)
              yield (ply, san) -> comment
            }.toMap
          }.getOrElse(Map.empty[(Int, String), String])

        for
          id <- idOpt
          title <- titleOpt
          summary <- summaryOpt
          if !detectHallucination(summary)
        yield id -> ChapterAnnotation(title, summary, keyMovesMap)
      }.toMap
    catch
      case e: Throwable =>
        System.err.println(s"[llm-study] failed to parse JSON body: ${e.getMessage}")
        Map.empty

  private def detectHallucination(text: String): Boolean =
    val lower = text.toLowerCase
    val suspiciousPatterns = List("kasparov", "carlsen", "fischer", "karpov")
    val hasSuspicious = suspiciousPatterns.exists(p => lower.contains(p))
    val movePattern = """\d+\.\s*[a-zA-Z][a-zA-Z0-9+#=]*""".r
    val tooManyMoves = movePattern.findAllMatchIn(text).size > 12
    if hasSuspicious || tooManyMoves then
      System.err.println(s"[llm-validation] Detected hallucination in: $text")
      true
    else false

  case class SectionNarrative(narrative: String, metadata: Option[BookModel.SectionMetadata], title: Option[String] = None)

  def bookSectionNarrative(prompt: String): Future[Option[SectionNarrative]] =
    apiKey match
      case None => Future.successful(None)
      case Some(key) =>
        val body = s"""{"contents":[{"parts":[{"text":${quote(prompt)}}]}], "generationConfig":{"responseMimeType":"application/json"}}"""
        
        buildAndSend(key, body, "[llm-section]") { text =>
          if detectHallucination(text) then Future.successful(None)
          else Future.successful(parseSectionJson(text))
        }

  private def parseSectionJson(jsonStr: String): Option[SectionNarrative] =
    try
      val json = ujson.read(jsonStr)
      val narrative = json.obj.get("narrative").map(_.str).getOrElse("")
      
      val theme = json.obj.get("theme").map(_.str).getOrElse("Strategy")
      val atmosphere = json.obj.get("atmosphere").map(_.str).getOrElse("Neutral")
      val contextMap = json.obj.get("context").flatMap(_.objOpt).map(_.toMap.mapValues {
        case ujson.Str(s) => s
        case ujson.Num(n) => if (n % 1 == 0) n.toInt.toString else n.toString
        case ujson.Bool(b) => b.toString
        case other => other.toString
      }.toMap).getOrElse(Map.empty)
      
      val title = json.obj.get("title").map(_.str).filter(_.nonEmpty)
      val meta = BookModel.SectionMetadata(theme, atmosphere, contextMap)
      
      if narrative.nonEmpty then Some(SectionNarrative(narrative, Some(meta), title))
      else None
    catch
      case e: Throwable =>
        System.err.println(s"[llm-parseSection] Failed to parse JSON: ${e.getMessage}")
        if jsonStr.trim.startsWith("{") then None else Some(SectionNarrative(jsonStr, None))
