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
  private val apiKey = sys.env.get("GEMINI_API_KEY")
  if apiKey.isEmpty then System.err.println("[llm] GEMINI_API_KEY not set; LlmClient is disabled")

  private val model = sys.env.getOrElse("GEMINI_MODEL", "gemini-3-pro-preview")
  private val endpoint = s"https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent?key="
  private val http = HttpClient.newHttpClient()

  private def sendWithRetry(req: HttpRequest, retries: Int = 3, delayMs: Int = 2000): HttpResponse[String] =
    try
      val res = http.send(req, HttpResponse.BodyHandlers.ofString())
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
      val candidates = json("candidates").arr
      candidates.headOption.flatMap { cand =>
        val parts = cand("content")("parts").arr
        parts.headOption.flatMap(part => part.obj.get("text").map(_.str))
      }
    catch
      case e: Throwable =>
        System.err.println(s"[llm] failed to parse response: ${e.getMessage}")
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
               case e: Throwable => Map.empty
          }
        else None
      catch
        case e: Throwable => None
    }.getOrElse(Map.empty)

  def shortComments(payload: String): Map[Int, String] =
    generateJson(payload, "shortComment")

  def criticalComments(payload: String): Map[Int, String] =
    generateJson(payload, "comment")

  def studyChapterComments(payload: String): Map[String, ChapterAnnotation] =
    apiKey.flatMap { key =>
      val prompt =
        s"""You are a chess book author. For each study chapter, return JSON array of objects with:
           |- "id": string (chapter id)
           |- "title": string (3-5 words, catchy chapter title)
           |- "summary": string (2-3 sentences, the chapter narrative)
           |- "keyMoves": array of {"ply": number, "san": string, "comment": string} (optional, for specific moves inside the chapter)
           |
           |PAYLOAD STRUCTURE (per chapter):
           |- id, anchorPly, label, turn, played, best?, deltaWinPct, winPctBefore/After, phase, studyScore, tags, lines[{label,pv,winPct}], practicality{overall,categoryGlobal,categoryPersonal?}
           |
           |CRITICAL CONSTRAINTS (STRICT MODE):
           |1. MOVES: Use ONLY moves from 'played', 'best', or the provided 'lines'. Do NOT invent other moves.
           |2. VARIATIONS: Do NOT extend PV beyond the given 'lines.pv' (max 2-3 plies). No new lines.
           |3. REFERENCES: Do NOT reference external games/players/theory.
           |4. TAGS ONLY: Base strategic commentary on provided tags/phase/practicality; do NOT invent concepts.
           |5. EVAL ONLY: Use ONLY winPctBefore/After/deltaWinPct/studyScore; do NOT estimate more evals.
           |6. NO SPECULATION: Avoid "might have", "could consider", "in theory", etc.
           |
           |WRITING INSTRUCTIONS:
           |1. **Title**:
           |   - Create a short, engaging title based on the key theme (e.g., "The King Hunt", "Missed Opportunity", "Endgame Precision").
           |
           |2. **Summary**:
           |   - State WHY this chapter matters (use deltaWinPct/studyScore/practicality/tags/phase).
           |   - Contrast played vs best if they differ.
           |   - Use a short theme label and a one-line takeaway.
           |   - Keep tone educational and grounded.
           |
           |3. **Key Moves (Comments)**:
           |   - Provide comments for ANY move within this chapter's scope (mainline or variations) where you can add educational value.
           |   - Do not artificially limit the number of comments; if a sequence is interesting, explain it.
           |   - Focus on "why" (plans, threats, practical choices), not just "what" (notation).
           |   - If a line is labeled 'practical', explain why it is easier/safer.
           |   - Keep comments concise (1-2 sentences).
           |   - **IMPORTANT**: You MUST include the "san" (Standard Algebraic Notation) for each move to identify it correctly (e.g., "Nf3", "e5").
           |
           |Payload: $payload""".stripMargin
      val body =
        s"""{
           |  "contents":[{"parts":[{"text":${quote(prompt)}}]}],
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
      val arr = ujson.read(body).arr
      arr.iterator.flatMap { v =>
        val obj = v.obj
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
  private def detectHallucination(text: String): Boolean =
    val lower = text.toLowerCase
    val suspiciousPatterns = List(
      "reminds me of",
      "reminiscent of",
      "famous game",
      "classic game",
      "in theory",
      "theoretically",
      "might have",
      "could have considered",
      "should have seen",
      "kasparov",
      "carlsen",
      "fischer",
      "karpov"
    )
    val hasSuspicious = suspiciousPatterns.exists(p => lower.contains(p))
    
    // Detect overly long variations (more than 3 move pairs like "1. e4 e5 2. Nf3 Nc6 3. Bb5")
    val movePattern = """\d+\.\s*[a-zA-Z][a-zA-Z0-9+#=]*""".r
    val moveCount = movePattern.findAllMatchIn(text).size
    val tooManyMoves = moveCount > 6 // More than ~3 move pairs suggests hallucination
    
    if hasSuspicious || tooManyMoves then
      System.err.println(s"[llm-validation] Detected hallucination in: ${text.take(100)}...")
      true
    else
      false
