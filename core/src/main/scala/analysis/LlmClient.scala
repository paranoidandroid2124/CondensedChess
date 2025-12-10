package chess
package analysis

import java.net.http.{ HttpClient, HttpRequest, HttpResponse }
import java.net.URI

import ujson.*

/** Minimal Gemini text client using REST API.
  *
  * Requires GEMINI_API_KEY in env. Model defaults to gemini-1.5-flash.
  */
object LlmClient:
  private val apiKey = EnvLoader.get("GEMINI_API_KEY").orElse(sys.props.get("GEMINI_API_KEY"))
  if apiKey.isEmpty then System.err.println("[llm] GEMINI_API_KEY not set; LlmClient is disabled")

  private val model = EnvLoader.getOrElse("GEMINI_MODEL", "gemini-2.5-flash")
  private val endpoint = s"https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent?key="
  private val http = HttpClient.newHttpClient()

  private def sendWithRetry(req: HttpRequest, retries: Int = 3, delayMs: Int = 2000): HttpResponse[String] =
    try
      val start = System.currentTimeMillis()
      val res = http.send(req, HttpResponse.BodyHandlers.ofString())
      val duration = System.currentTimeMillis() - start
      if duration > 2000 then
        System.err.println(s"[LlmClient] Request took ${duration}ms (status ${res.statusCode()})")
      
      if (res.statusCode() == 429 || res.statusCode() >= 500) && retries > 0 then
        System.err.println(s"[LlmClient] Rate limit/Server error (${res.statusCode()}). Retrying in ${delayMs}ms...")
        Thread.sleep(delayMs)
        sendWithRetry(req, retries - 1, delayMs * 2)
      else
        res
    catch
      case e: Throwable if retries > 0 =>
        System.err.println(s"[LlmClient] Network error: ${e.getMessage}. Retrying in ${delayMs}ms...")
        Thread.sleep(delayMs)
        sendWithRetry(req, retries - 1, delayMs * 2)
      case e: Throwable => throw e

  def summarize(json: String): Option[String] =
    apiKey.flatMap { key =>
      val prompt =
        s"""You are a chess coach. Summarize this chess review JSON for a human in 3-4 sentences.
           |- Payload contains an "instructions" block; follow it plus these rules.
           |- Use only moves/evals given. Do NOT invent moves/evals or move numbers.
           |- Do NOT use the word 'ply'; refer to moves by standard notation (e.g., "13. Qe2").
           |- When referencing moves, quote the provided label exactly (e.g., "13. Qe2", "12...Ba6"); never renumber.
           |- Use keySwings/critical arrays to anchor the story. Mention forced/only-move flags, legalMoves, and best-vs-second gaps when present.
           |- Lean on semanticTags/mistakeCategory (tactical_miss/greedy/positional_trade_error/ignored_threat) and conceptShift to explain *why* the eval moved.
           |- Mention novelty/book briefly using openingStats if present (bookPly/novelty, games, topMoves, topGames players/elos), and include opening.summary or bookExitComment when provided.
           |- Avoid generic advice; ground every claim in the provided tags/evals/branches/oppositeColorBishops flags.
           |JSON: $json""".stripMargin
      val body =
        s"""{"contents":[{"parts":[{"text":${quote(prompt)}}]}]}"""
      val req = HttpRequest
        .newBuilder()
        .uri(URI.create(endpoint + key))
        .header("Content-Type", "application/json")
        .POST(HttpRequest.BodyPublishers.ofString(body))
        .build()
      try
        val res = sendWithRetry(req)
        if res.statusCode() >= 200 && res.statusCode() < 300 then
          extractText(res.body())
        else
          System.err.println(s"[llm-summarize] status=${res.statusCode()} body=${res.body()}")
          None
      catch
        case e: Throwable =>
          System.err.println(s"[llm-summarize] request failed: ${e.getMessage}")
          None
    }

  private def extractText(response: String): Option[String] =
    try
      val json = ujson.read(response)
      val candidates = json.obj.get("candidates").flatMap(_.arrOpt).getOrElse(scala.collection.mutable.ArrayBuffer.empty)
      
      candidates.headOption.flatMap { cand =>
        // Check for finishReason to debug safety blocks
        val finishReason = cand.obj.get("finishReason").map(_.str).getOrElse("UNKNOWN")
        if finishReason != "STOP" then
          System.err.println(s"[llm] finishReason: $finishReason")

        for
          content <- cand.obj.get("content")
          parts <- content.obj.get("parts").flatMap(_.arrOpt)
          firstPart <- parts.headOption
          text <- firstPart.obj.get("text").map(_.str)
        yield {
          System.err.println(s"[llm] extracted text length: ${text.length}")
          System.err.println(s"[llm] extracted text length: ${text.length}")
          System.err.println(s"[llm] extracted text: $text")
          text
        }
      }
    catch
      case e: Throwable =>
        System.err.println(s"[llm] failed to parse response: ${e.getMessage}")
        System.err.println(s"[llm] response body: $response")
        None

  def quote(in: String): String =
    "\"" + in.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n") + "\""

  private def generateJson(payload: String, field: String): Map[Int, String] =
    apiKey.flatMap { key =>
      val prompt =
        s"""You are a chess engine. Analyze the JSON payload and return a JSON array of objects:
           |[{"ply": 12, "$field": "..."}]
           |
           |Rules:
           |- Only comment on moves present in the payload.
           |- Keep comments short and factual.
           |- Reference moves by standard notation (e.g. '13. Qe2'), never by ply number.
           |
           |Payload: $payload""".stripMargin
      val body =
        s"""{"contents":[{"parts":[{"text":${quote(prompt)}}]}],
           |  "generationConfig":{"responseMimeType":"application/json"}
           |}""".stripMargin
      val req = HttpRequest
        .newBuilder()
        .uri(URI.create(endpoint + key))
        .header("Content-Type", "application/json")
        .POST(HttpRequest.BodyPublishers.ofString(body))
        .build()
      try
        val res = sendWithRetry(req)
        if res.statusCode() >= 200 && res.statusCode() < 300 then
          extractText(res.body()).map { text =>
             try
               val arr = ujson.read(text).arr
               arr.flatMap { v =>
                 for
                   ply <- v.obj.get("ply").map(_.num.toInt)
                   comment <- v.obj.get(field).map(_.str)
                 yield ply -> comment
               }.toMap
             catch
               case _: Throwable => Map.empty
          }
        else None
      catch
        case _: Throwable => None
    }.getOrElse(Map.empty)

  def shortComments(payload: String): Map[Int, String] =
    generateJson(payload, "shortComment")

  def criticalComments(payload: String): Map[Int, String] =
    apiKey.flatMap { key =>
      val prompt =
        s"""You are a friendly but sharp Chess Coach. Analyze these critical moments and provide a "Coach's Insight" for each.
           |
           |Payload (JSON array of critical nodes):
           |- "ply", "label" (Played Move), "reason", "deltaWinPct"
           |- "branches": Array of { "label" (e.g. Best Move), "move", "pv" (SAN line), "winPct" }
           |- "conceptShift", "mistakeCategory", "tags"
           |
           |Your Goal:
           |Explain *why* the played move was a mistake and *what* the student should have played instead, citing the Best Move's specific continuation.
           |
           |CRITICAL SAFETY RAIL (HALLUCINATION CHECK):
           |1. **NO GEOMETRY GUESSES**: Do NOT claim a piece attacks/defends a square unless it is OBVIOUS from the PV (e.g. if PV has 'Nxe5', you can say 'it captures on e5').
           |   - BAD: "Knight on d7 attacks f6" (You cannot see the board).
           |   - GOOD: "This move allows White to play h5" (If h5 is in the PV).
           |2. **TRUST DATA**: Use 'conceptShift' (e.g. 'badBishop', 'tacticalDepth') as the source of truth for your explanation.
           |
           |Style Templates (Adapt these tones, do not copy verbatim):
           |1. (Single Move): "[Move] is a strong/weak idea. It creates a [Tactical Advantage/Strategic Bind]. The resulting structure restricts..."
           |2. (Mistake): "[Move] works poorly. It concedes [Tactical Control/Key Squares]. From this moment, Black operates under pressure..."
           |3. (Engine View): "Older engines saw this as equal, but modern evaluation highlights the [Long-term Compensation/Tactical Risk]..."
           |4. (Plan): "White's plan is straightforward: consolidate and improve piece placement..."
           |
           |Style Guide:
           |1. **Direct & Concrete**: Use the templates but fill them with strict facts from the PV and Tags.
           |2. **Use the PV**: You MUST explicitly mention the "Best Move" and its key follow-up (PV). E.g., "Review Alternative: capture on e5 (Nxe5)..."
           |3. **Concise**: Keep it under 2 sentences.
           |4. **Vocabulary**: Use the provided 'conceptShift' names (e.g. "Tactical Complexity", "Dynamic Sharpness") directly in your text.
           |
           |Input Payload: $payload
           |
           |Output Format:
           |JSON array: [{"ply": 12, "comment": "..."}]""".stripMargin
      val body =
        s"""{"contents":[{"parts":[{"text":${quote(prompt)}}]}],
           |  "generationConfig":{"responseMimeType":"application/json"}
           |}""".stripMargin
      
      // Reuse the request logic from generateJson but with our custom body
      val req = HttpRequest
        .newBuilder()
        .uri(URI.create(endpoint + key))
        .header("Content-Type", "application/json")
        .POST(HttpRequest.BodyPublishers.ofString(body))
        .build()
      
      try
         val res = sendWithRetry(req)
         if res.statusCode() >= 200 && res.statusCode() < 300 then
           extractText(res.body()).map { text =>
              try
                val arr = ujson.read(text).arr
                arr.flatMap { v =>
                  for
                    ply <- v.obj.get("ply").map(_.num.toInt)
                    comment <- v.obj.get("comment").map(_.str)
                  yield ply -> comment
                }.toMap
              catch
                case _: Throwable => Map.empty
           }
         else None
      catch
        case _: Throwable => None
    }.getOrElse(Map.empty)

  def studyChapterComments(payload: String): Map[String, ChapterAnnotation] =
    apiKey.flatMap { key =>
       // Dynamic Prompt Construction
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

       val fullPrompt = s"""$prompt
            |
            |Payload: $payload""".stripMargin

       val body =
         s"""{
            |  "contents":[{"parts":[{"text":${quote(fullPrompt)}}]}],
            |  "generationConfig":{"responseMimeType":"application/json"}
            |}""".stripMargin
       val req = HttpRequest
         .newBuilder()
         .uri(URI.create(endpoint + key))
         .header("Content-Type", "application/json")
         .POST(HttpRequest.BodyPublishers.ofString(body))
         .build()
       try
         val res = sendWithRetry(req)
         if res.statusCode() >= 200 && res.statusCode() < 300 then
           extractText(res.body()).map(parseChapterAnnotationJson)
         else
           System.err.println(s"[llm-study] status=${res.statusCode()} body=${res.body()}")
           None
       catch
         case e: Throwable =>
           System.err.println(s"[llm-study] request failed: ${e.getMessage}")
           None
     }.getOrElse(Map.empty)

  case class ChapterAnnotation(title: String, summary: String, keyMoves: Map[(Int, String), String])

  private def parseChapterAnnotationJson(body: String): Map[String, ChapterAnnotation] =
    try
      val parsed = ujson.read(body)
      val arr = parsed match
        case a: ujson.Arr => a.arr
        case o: ujson.Obj => scala.collection.mutable.ArrayBuffer(o)
        case _ => scala.collection.mutable.ArrayBuffer.empty

      
      arr.iterator.flatMap { v =>
        // Sometimes wrapped in "miniChapter" object or similar key
        val obj = if (v.obj.contains("miniChapter")) v("miniChapter").obj else v.obj
        val idOpt = obj.get("id").map(_.str)
        val titleOpt = obj.get("title").map(_.str).orElse(Some("Chapter"))
        val summaryOpt = obj.get("summary").map(_.str)
        
        // Parse keyMoves
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

  /** Detects common hallucination patterns in LLM-generated text */
  /** Detects common hallucination patterns in LLM-generated text */
  private def detectHallucination(text: String): Boolean =
    val lower = text.toLowerCase
    // Relaxed patterns: removing generic terms like "in theory" or "should have seen" which are valid coaching
    val suspiciousPatterns = List(
      "kasparov",
      "carlsen",
      "fischer",
      "karpov"
    )
    val hasSuspicious = suspiciousPatterns.exists(p => lower.contains(p))
    
    // Detect overly long variations (more than 6 move pairs)
    // Relaxed from 3 to 6
    val movePattern = """\d+\.\s*[a-zA-Z][a-zA-Z0-9+#=]*""".r
    val moveCount = movePattern.findAllMatchIn(text).size
    val tooManyMoves = moveCount > 12 // Allow up to ~6 full moves (12 plies)
    
    if hasSuspicious || tooManyMoves then
      System.err.println(s"[llm-validation] Detected hallucination in: $text")
      true
    else
      false

  case class SectionNarrative(narrative: String, metadata: Option[BookModel.SectionMetadata])

  def bookSectionNarrative(prompt: String): Option[SectionNarrative] =
    apiKey.flatMap { key =>
      val body =
        s"""{"contents":[{"parts":[{"text":${quote(prompt)}}]}],
           |  "generationConfig":{"responseMimeType":"application/json"}
           |}""".stripMargin
      val req = HttpRequest
        .newBuilder()
        .uri(URI.create(endpoint + key))
        .header("Content-Type", "application/json")
        .POST(HttpRequest.BodyPublishers.ofString(body))
        .build()
      
      try
        val res = sendWithRetry(req)
        if res.statusCode() >= 200 && res.statusCode() < 300 then
           extractText(res.body()).filter(!detectHallucination(_)).flatMap(parseSectionJson)
        else
           System.err.println(s"[llm-section] status=${res.statusCode()} body=${res.body()}")
           None
      catch
        case e: Throwable =>
           System.err.println(s"[llm-section] request failed: ${e.getMessage}")
           None
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
      
      val meta = BookModel.SectionMetadata(theme, atmosphere, contextMap)
      
      if narrative.nonEmpty then Some(SectionNarrative(narrative, Some(meta)))
      else None
    catch
      case e: Throwable =>
        System.err.println(s"[llm-parseSection] Failed to parse JSON: ${e.getMessage}")
        if jsonStr.trim.startsWith("{") then None else Some(SectionNarrative(jsonStr, None))
