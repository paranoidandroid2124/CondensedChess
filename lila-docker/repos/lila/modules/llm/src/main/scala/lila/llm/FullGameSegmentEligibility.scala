package lila.llm

private[llm] object FullGameSegmentEligibility:

  final case class SegmentStats(
      totalWords: Int,
      proseWords: Int,
      protectedTokenCount: Int
  ):
    def protectedShare: Double =
      if totalWords <= 0 then 1.0
      else protectedTokenCount.toDouble / totalWords.toDouble

  private val MinTotalWords = 12
  private val MinProseWords = 8
  private val MinProseWordsWithLocks = 10
  private val MaxProtectedShare = 0.28

  def stats(segmentText: String): SegmentStats =
    val masked = PolishSegmenter.maskStructuralTokens(segmentText)
    val totalWords = wordCount(segmentText)
    val proseWords =
      if masked.hasLocks then proseWordCount(masked.maskedText)
      else proseWordCount(segmentText)
    SegmentStats(
      totalWords = totalWords,
      proseWords = proseWords,
      protectedTokenCount = masked.expectedOrder.size
    )

  def isEligible(segmentText: String): Boolean =
    isEligible(stats(segmentText))

  def isEligible(stats: SegmentStats): Boolean =
    stats.totalWords >= MinTotalWords &&
    stats.proseWords >= MinProseWords &&
    (!hasHeavyProtectedDensity(stats)) &&
    (!hasSparseProseWithLocks(stats))

  private def hasHeavyProtectedDensity(stats: SegmentStats): Boolean =
    stats.protectedTokenCount > 0 && stats.protectedShare > MaxProtectedShare

  private def hasSparseProseWithLocks(stats: SegmentStats): Boolean =
    stats.protectedTokenCount > 0 && stats.proseWords < MinProseWordsWithLocks

  private def wordCount(text: String): Int =
    Option(text).getOrElse("").split("\\s+").count(_.nonEmpty)

  private def proseWordCount(text: String): Int =
    Option(text)
      .getOrElse("")
      .split("\\s+")
      .count { word =>
        val trimmed = word.trim
        trimmed.nonEmpty &&
        !trimmed.startsWith("[[LK_") &&
        trimmed.exists(_.isLetter)
      }

