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
    val originalMap = rewrites.map(rewrite => rewrite.id -> rewrite.rewritten).toMap
    val baselineText = segmentation.merge(originalMap)
    val baselineValidation = PolishValidation.validatePolishedCommentary(baselineText, originalProse, allowedSans)

    if baselineValidation.isValid then
      RepairResult(
        text = baselineText,
        applied = false,
        actions = Nil,
        reasons = Nil
      )
    else
      val orderedRewrites =
        rewrites.sortBy(rewrite => (-protectedTokenCount(rewrite.original), -wordCount(rewrite.original), rewrite.id))

      val repaired =
        orderedRewrites.foldLeft(
          (originalMap, baselineText, baselineValidation.reasons, List.empty[String])
        ) { case ((currentMap, currentText, currentReasons, currentActions), rewrite) =>
          if currentReasons.isEmpty then (currentMap, currentText, currentReasons, currentActions)
          else
            val candidateMap = currentMap - rewrite.id
            val candidateText = segmentation.merge(candidateMap)
            val candidateReasons =
              PolishValidation
                .validatePolishedCommentary(candidateText, originalProse, allowedSans)
                .reasons

            if candidateReasons.isEmpty || candidateReasons.size < currentReasons.size then
              (candidateMap, candidateText, candidateReasons, currentActions :+ s"revert_${rewrite.id}")
            else (currentMap, currentText, currentReasons, currentActions)
        }

      RepairResult(
        text = repaired._2,
        applied = repaired._4.nonEmpty,
        actions = repaired._4,
        reasons = repaired._3
      )

  private def protectedTokenCount(text: String): Int =
    PolishSegmenter.maskStructuralTokens(text).expectedOrder.size

  private def wordCount(text: String): Int =
    Option(text).getOrElse("").split("\\s+").count(_.nonEmpty)
