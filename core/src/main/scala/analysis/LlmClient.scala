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
           |- Use only moves/evals given. Do NOT invent moves/evals or move numbers.
           |- When referencing moves, quote the provided label exactly (e.g., "13. Qe2", "12...Ba6"); never renumber.
           |- Use semanticTags (facts like open file, outpost, weak pawn) and mistakeCategory (tactical_miss/greedy/positional_trade_error/ignored_threat) to explain *why*.
           |- Mention novelty/book briefly using openingStats if present (bookPly/novelty, games, topMoves, topGames players/elos).
           |- Avoid generic advice; ground every claim in the provided tags/evals/branches.
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

  private def generateJson(promptPayload: String, field: String): Map[Int, String] =
    apiKey.flatMap { key =>
      val prompt =
        s"""You are a chess coach. For each item, return JSON array of {"ply":number,"label":string,"$field":string}.
           |- Use only moves/evals provided. Do NOT invent moves/evals or move numbers.
           |- Always reference the provided label (e.g., "13. Qe2", "12...Ba6"); do not fabricate ply indices.
           |- Leverage mistakeCategory (tactical_miss/greedy/positional_trade_error/ignored_threat) and semanticTags (facts like open_h_file, weak_f7, outpost_f5, back_rank_weak) to ground the explanation.
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
