package lila.llm

import com.softwaremill.macwire.*
import play.api.libs.ws.StandaloneWSClient
import lila.llm.analysis.OpeningExplorerClient

@Module
final class Env(
    ws: StandaloneWSClient
)(using Executor):

  private val geminiConfig = GeminiConfig.fromEnv
  lazy val geminiClient    = GeminiClient(ws, geminiConfig)
  private val llmConfig    = LlmConfig.fromEnv

  lazy val commentaryCache = CommentaryCache()

  private val analysisThreadCount = Math.max(1, Runtime.getRuntime.availableProcessors() - 1)
  private val analysisService     = java.util.concurrent.Executors.newFixedThreadPool(analysisThreadCount)
  lazy val analysisExecutor: Executor =
    scala.concurrent.ExecutionContext.fromExecutor(analysisService)

  private lazy val openingExplorer = OpeningExplorerClient(ws)
  lazy val api: LlmApi = LlmApi(openingExplorer, geminiClient, commentaryCache, llmConfig)(using analysisExecutor)
