package lila.commentary.analysis

import play.api.libs.json.*

object CommentaryOpsBoard:

  final case class Sample(
      kind: String,
      capturedAtMs: Long,
      fields: Map[String, String]
  )
  object Sample:
    given Writes[Sample] = Json.writes[Sample]

  final case class MoveReviewMetrics(
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
  object MoveReviewMetrics:
    given Writes[MoveReviewMetrics] = Json.writes[MoveReviewMetrics]

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
      moveReview: MoveReviewMetrics,
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
