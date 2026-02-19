package lila.llm

import com.softwaremill.macwire.*
import play.api.libs.ws.StandaloneWSClient
import lila.llm.analysis.OpeningExplorerClient

@Module
final class Env(
    ws: StandaloneWSClient,
    db: lila.db.Db
)(using Executor):

  private val geminiConfig = GeminiConfig.fromEnv
  lazy val geminiClient    = GeminiClient(ws, geminiConfig)

  lazy val commentaryCache = CommentaryCache()

  private lazy val creditColl = db(lila.core.config.CollName("llm_credits"))
  lazy val creditApi           = CreditApi(creditColl)

  private val analysisThreadCount = Math.max(1, Runtime.getRuntime.availableProcessors() - 1)
  private val analysisService     = java.util.concurrent.Executors.newFixedThreadPool(analysisThreadCount)
  lazy val analysisExecutor: Executor =
    scala.concurrent.ExecutionContext.fromExecutor(analysisService)

  private lazy val openingExplorer = OpeningExplorerClient(ws)
  lazy val api: LlmApi = LlmApi(openingExplorer, geminiClient, commentaryCache)(using analysisExecutor)
