package lila.llm

import com.softwaremill.macwire.*
import play.api.libs.ws.StandaloneWSClient
import lila.llm.analysis.OpeningExplorerClient

@Module
final class Env(
    ws: StandaloneWSClient,
    db: lila.db.Db
)(using Executor):

  // ── Gemini ─────────────────────────────────────────────────────────────
  private val geminiConfig = GeminiConfig.fromEnv
  lazy val geminiClient    = GeminiClient(ws, geminiConfig)

  // ── Caching ────────────────────────────────────────────────────────────
  lazy val commentaryCache = CommentaryCache()

  // ── Credits (MongoDB) ──────────────────────────────────────────────────
  private lazy val creditColl = db(lila.core.config.CollName("llm_credits"))
  lazy val creditApi           = CreditApi(creditColl)

  // ── API ────────────────────────────────────────────────────────────────
  private lazy val openingExplorer = OpeningExplorerClient(ws)
  lazy val api: LlmApi = LlmApi(openingExplorer, geminiClient, commentaryCache)
