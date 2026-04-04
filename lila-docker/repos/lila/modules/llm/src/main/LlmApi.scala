package lila.llm

final class LlmApi(
    openingExplorer: OpeningExplorerClient,
    geminiClient: GeminiClient,
    openAiClient: OpenAiClient,
    commentaryCache: CommentaryCache,
    llmConfig: LlmConfig = LlmConfig.fromEnv,
    providerConfig: LlmProviderConfig = LlmProviderConfig.fromEnv,
    ccaHistoryRepo: Option[CcaHistoryRepo] = None
)(using Executor):

  val northStarDoc =
    "modules/llm/docs/StrategicObjectModel.md"
  val roadmapDoc =
    "modules/llm/docs/StrategicObjectRoadmap.md"
  val demolitionStatus =
    "legacy commentary-analysis API topology removed; strategic-object rebuild pending"
