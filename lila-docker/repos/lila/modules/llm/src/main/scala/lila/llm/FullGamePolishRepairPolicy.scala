package lila.llm

private[llm] object FullGamePolishRepairPolicy:

  private val LowYieldSegmentRepairReasons = Set(
    "anchor_missing",
    "anchor_order_violation",
    "json_wrapper_unparsed",
    "truncated_output",
    "leak_token_detected",
    "length_ratio_out_of_bounds"
  )

  private val LowYieldSegmentRepairReasonCombos = Set(
    Set("san_order_violation", "count_budget_exceeded")
  )

  private val LowYieldWholeProseRetryReasons = Set(
    "anchor_missing",
    "anchor_order_violation",
    "san_core_missing",
    "san_order_violation",
    "count_budget_exceeded",
    "numbering_missing",
    "marker_style_mismatch",
    "eval_missing",
    "eval_count_budget_exceeded",
    "eval_order_violation",
    "variation_branch_count_exceeded",
    "variation_branch_violation"
  )

  def shouldAttemptSegmentRepair(reasons: List[String]): Boolean =
    val normalized = normalize(reasons)
    normalized.nonEmpty &&
      !normalized.forall(LowYieldSegmentRepairReasons.contains) &&
      !LowYieldSegmentRepairReasonCombos.exists(combo =>
        combo.subsetOf(normalized) && normalized.subsetOf(combo ++ LowYieldSegmentRepairReasons)
      )

  def shouldRetryWholeProseAfterMergedFailure(
      reasons: List[String],
      softRepairApplied: Boolean
  ): Boolean =
    val normalized = normalize(reasons)
    if normalized.isEmpty then false
    else if softRepairApplied then false
    else !normalized.forall(LowYieldWholeProseRetryReasons.contains)

  private def normalize(reasons: List[String]): Set[String] =
    reasons
      .map(_.trim)
      .filter(_.nonEmpty)
      .toSet
