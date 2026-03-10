package lila.llm

import com.softwaremill.macwire.*
import play.api.Configuration
import play.api.libs.ws.StandaloneWSClient
import lila.llm.analysis.OpeningExplorerClient
import lila.core.config.CollName

@Module
final class Env(
    db: lila.db.Db,
    appConfig: Configuration,
    ws: StandaloneWSClient
)(using Executor):

  private val geminiConfig = GeminiConfig.fromEnv
  lazy val geminiClient    = GeminiClient(ws, geminiConfig)
  private val openAiConfig = OpenAiConfig.fromEnv
  lazy val openAiClient    = OpenAiClient(ws, openAiConfig)
  private val providerConfig = LlmProviderConfig.fromEnv
  private val llmConfig    = LlmConfig.fromEnv

  lazy val commentaryCache = CommentaryCache()

  lazy val ccaHistoryRepo = CcaHistoryRepo(db(CollName("cca_history")))

  private val analysisThreadCount = Math.max(1, Runtime.getRuntime.availableProcessors() - 1)
  private val analysisService     = java.util.concurrent.Executors.newFixedThreadPool(analysisThreadCount)
  lazy val analysisExecutor: Executor =
    scala.concurrent.ExecutionContext.fromExecutor(analysisService)

  private val configuredExplorerBase =
    appConfig
      .getOptional[String]("explorer.internal_endpoint")
      .orElse(appConfig.getOptional[String]("explorer.endpoint"))
      .map(_.trim)
      .filter(_.nonEmpty)
  private lazy val openingExplorer = OpeningExplorerClient(ws, explorerBaseConfig = configuredExplorerBase)
  lazy val api: LlmApi =
    LlmApi(openingExplorer, geminiClient, openAiClient, commentaryCache, llmConfig, providerConfig, Some(ccaHistoryRepo))(using analysisExecutor)
