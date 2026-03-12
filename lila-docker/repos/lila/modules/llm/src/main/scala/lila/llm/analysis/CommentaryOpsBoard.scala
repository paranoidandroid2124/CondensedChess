package lila.llm.analysis

import play.api.libs.json.*

object CommentaryOpsBoard:

  final case class Sample(
      kind: String,
      capturedAtMs: Long,
      fields: Map[String, String]
  )
  object Sample:
    given Writes[Sample] = Json.writes[Sample]

  final case class BookmakerMetrics(
      requests: Long,
      polishAttempts: Long,
      polishAccepted: Long,
      polishFallbackRate: Double,
      softRepairAnyRate: Double,
      softRepairMaterialRate: Double,
      compareObserved: Long,
      compareConsistencyRate: Double,
      avgCostUsd: Double
  )
  object BookmakerMetrics:
    given Writes[BookmakerMetrics] = Json.writes[BookmakerMetrics]

  final case class FullGameMetrics(
      compareObserved: Long,
      compareConsistencyRate: Double
  )
  object FullGameMetrics:
    given Writes[FullGameMetrics] = Json.writes[FullGameMetrics]

  final case class ActiveMetrics(
      selectedMoments: Long,
      attempts: Long,
      attached: Long,
      omitted: Long,
      primaryAccepted: Long,
      repairAttempts: Long,
      repairRecovered: Long,
      attachRate: Double,
      thesisAgreementRate: Double,
      dossierAttachRate: Double,
      dossierCompareRate: Double,
      dossierRouteRefRate: Double,
      dossierReferenceFailureRate: Double,
      provider: Option[String],
      configuredModel: Option[String],
      fallbackModel: Option[String],
      reasoningEffort: Option[String],
      observedModelDistribution: Map[String, Long],
      omitReasons: Map[String, Long],
      warningReasons: Map[String, Long],
      routeRedeployCount: Long,
      routeMoveRefCount: Long,
      routeHiddenSafetyCount: Long,
      routeTowardOnlyCount: Long,
      routeExactSurfaceCount: Long,
      routeOpponentHiddenCount: Long
  )
  object ActiveMetrics:
    given Writes[ActiveMetrics] = Json.writes[ActiveMetrics]

  final case class PromptUsageMetrics(
      attempts: Long,
      cacheHits: Long,
      promptTokens: Long,
      cachedTokens: Long,
      completionTokens: Long,
      estimatedCostUsd: Double
  )
  object PromptUsageMetrics:
    given Writes[PromptUsageMetrics] = Json.writes[PromptUsageMetrics]

  final case class Snapshot(
      generatedAtMs: Long,
      bookmaker: BookmakerMetrics,
      fullgame: FullGameMetrics,
      active: ActiveMetrics,
      promptUsage: Map[String, PromptUsageMetrics],
      recentSamples: List[Sample]
  )
  object Snapshot:
    given Writes[Snapshot] = Json.writes[Snapshot]

final class CommentaryOpsBoard(maxSamples: Int = 100):

  import CommentaryOpsBoard.Sample

  private val lock = new Object
  private var samples = Vector.empty[Sample]

  def recordSample(kind: String, fields: Map[String, String]): Unit =
    val sample = Sample(kind = kind, capturedAtMs = System.currentTimeMillis(), fields = fields)
    lock.synchronized {
      val next = samples :+ sample
      samples =
        if next.size <= maxSamples then next
        else next.takeRight(maxSamples)
    }

  def recentSamples(limit: Int): List[Sample] =
    val bounded = limit.max(1).min(maxSamples)
    lock.synchronized {
      samples.takeRight(bounded).reverse.toList
    }
