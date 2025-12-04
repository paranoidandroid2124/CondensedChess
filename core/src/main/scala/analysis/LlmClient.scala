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
        val res = http.send(req, HttpResponse.BodyHandlers.ofString())
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

  def shortComments(payload: String): Map[Int, String] =
    generateJson(payload, "shortComment")

  def criticalComments(payload: String): Map[Int, String] =
    generateJson(payload, "comment")

  def treeComments(payload: String): Map[Int, String] =
    generateJson(payload, "comment")

  def studyChapterComments(payload: String): Map[String, String] =
    apiKey.flatMap { key =>
      val prompt =
        s"""You are a chess book author. For each study chapter, return JSON array of {"id":string,"summary":string}.

PAYLOAD STRUCTURE (per chapter):
- id, anchorPly, label, turn, played, best?, deltaWinPct, winPctBefore/After, phase, studyScore, tags, lines[{label,pv,winPct}], practicality{overall,categoryGlobal,categoryPersonal?}

CRITICAL CONSTRAINTS (STRICT MODE):
1. MOVES: Use ONLY moves from 'played', 'best', or the provided 'lines'. Do NOT invent other moves.
2. VARIATIONS: Do NOT extend PV beyond the given 'lines.pv' (max 2-3 plies). No new lines.
3. REFERENCES: Do NOT reference external games/players/theory.
4. TAGS ONLY: Base strategic commentary on provided tags/phase/practicality; do NOT invent concepts.
5. EVAL ONLY: Use ONLY winPctBefore/After/deltaWinPct/studyScore; do NOT estimate more evals.
6. NO SPECULATION: Avoid "might have", "could consider", "in theory", etc.

WRITING INSTRUCTIONS (keep to 2-3 sentences, ~70-110 words):
- State WHY this chapter matters (use deltaWinPct/studyScore/practicality/tags/phase).
- Contrast played vs best if they differ, referencing label/turn for clarity.
- Use a short theme label (from tags/phase) and a one-line takeaway (plan/outcome).
- If tags imply danger/only-move (king_exposed, conversion_difficulty, plan_change, fortress_building, etc.), call it out directly.
- Keep tone educational, specific, and grounded in provided data.

Payload: $payload""".stripMargin
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
        val res = http.send(req, HttpResponse.BodyHandlers.ofString())
        if res.statusCode() >= 200 && res.statusCode() < 300 then
          extractText(res.body()).map(parseIdSummaryJson)
        else
          System.err.println(s"[llm-study] status=${res.statusCode()} body=${res.body()}")
          None
      catch
        case e: Throwable =>
          System.err.println(s"[llm-study] request failed: ${e.getMessage}")
          None
    }.getOrElse(Map.empty)

  private def generateJson(promptPayload: String, field: String): Map[Int, String] =
    apiKey.flatMap { key =>
      val prompt =
        s"""You are a chess coach. For each item, return JSON array of {"ply":number,"label":string,"$field":string}.
           |- Payload includes an "instructions" block and data nodes; follow that guidance while applying the rules below.
           |- Use only moves/evals provided. Do NOT invent moves/evals or move numbers.
           |- Prefer the provided label field when present (e.g., "13. Qe2", "12...Ba6"); do not fabricate ply indices.
           |- Leverage mistakeCategory (tactical_miss/greedy/positional_trade_error/ignored_threat) and semanticTags (facts like open_h_file, weak_f7, outpost_f5, weak_back_rank) to ground the explanation.
           |- Mention forced/only-move or big best-vs-second gaps when legalMoves<=1 or bestVsSecondGap is large.
           |- Use conceptShift or tags to explain *why* (kingSafety/pawnStorm/rookActivity changes, etc.) without inventing moves.
           |- Do NOT call a move a blunder/mistake unless its judgement or delta clearly indicates it; stay consistent with provided tags/delta.
           |- If generating critical move comments, include: (a) a short heading (3-6 words), (b) 1-2 sentences on why (based on given judgement/delta/pv/tags), (c) 1-2 refutation moves using provided pv/branches. Keep it concise and factual.
           |- If payload has pv, you can briefly mention the idea, but keep comments specific to the given move/judgement.
           |Payload: $promptPayload""".stripMargin
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
        val res = http.send(req, HttpResponse.BodyHandlers.ofString())
        if res.statusCode() >= 200 && res.statusCode() < 300 then
          extractText(res.body()).map(parsePlyMapJson(_, field))
        else
          System.err.println(s"[llm-${field}] status=${res.statusCode()} body=${res.body()}")
          None
      catch
        case e: Throwable =>
          System.err.println(s"[llm-${field}] request failed: ${e.getMessage}")
          None
    }.getOrElse(Map.empty)

  private def parsePlyMapJson(body: String, field: String): Map[Int, String] =
    try
      val arr = ujson.read(body).arr
      arr.iterator.flatMap { v =>
        val obj = v.obj
        for
          ply <- obj.get("ply").map(_.num.toInt)
          value <- obj.get(field).map(_.str)
        yield ply -> value
      }.toMap
    catch
      case e: Throwable =>
        System.err.println(s"[llm-$field] failed to parse JSON body: ${e.getMessage}")
        Map.empty

  private def parseIdSummaryJson(body: String): Map[String, String] =
    try
      val arr = ujson.read(body).arr
      arr.iterator.flatMap { v =>
        val obj = v.obj
        for
          id <- obj.get("id").map(_.str)
          summary <- obj.get("summary").map(_.str)
          if !detectHallucination(summary) // Filter out hallucinated summaries
        yield id -> summary
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
