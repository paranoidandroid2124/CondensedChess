package lila.llm

private[llm] object FullGameSegmentSoftRepair:

  final case class Rewrite(
      id: String,
      rewritten: String,
      original: String
  )

  final case class RepairResult(
      text: String,
      applied: Boolean,
      actions: List[String],
      reasons: List[String]
  ):
    def isValid: Boolean = reasons.isEmpty

  def repairMerged(
      segmentation: PolishSegmenter.Segmentation,
      rewrites: List[Rewrite],
      originalProse: String,
      allowedSans: List[String]
  ): RepairResult =
    val _ = originalProse
    val _ = allowedSans
    val originalMap = rewrites.map(rewrite => rewrite.id -> rewrite.rewritten).toMap
    val baselineText = segmentation.merge(originalMap)
    RepairResult(
      text = baselineText,
      applied = false,
      actions = Nil,
      reasons = Nil
    )

  private def protectedTokenCount(text: String): Int =
    PolishSegmenter.maskStructuralTokens(text).expectedOrder.size

  private def wordCount(text: String): Int =
    Option(text).getOrElse("").split("\\s+").count(_.nonEmpty)
