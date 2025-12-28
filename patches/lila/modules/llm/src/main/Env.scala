package lila.llm

import com.softwaremill.macwire.*
import play.api.libs.ws.StandaloneWSClient

@Module
final class Env(
    ws: StandaloneWSClient
)(using scala.concurrent.ExecutionContext):

  private val config = LlmConfig.fromEnv

  lazy val client: LlmClient = LlmClient(ws, config)

  lazy val api: LlmApi = LlmApi(client)

  if config.enabled then
    lila.log("llm").info(s"LLM module enabled with model: ${config.model}")
  else
    lila.log("llm").warn("LLM module disabled: GEMINI_API_KEY not set")
