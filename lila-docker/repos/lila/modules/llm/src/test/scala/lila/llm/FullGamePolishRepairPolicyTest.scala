package lila.llm

import munit.FunSuite

class FullGamePolishRepairPolicyTest extends FunSuite:

  test("segment repair is bypassed for low-yield structural failures") {
    assert(!FullGamePolishRepairPolicy.shouldAttemptSegmentRepair(List("anchor_missing", "anchor_order_violation")))
    assert(!FullGamePolishRepairPolicy.shouldAttemptSegmentRepair(List("truncated_output")))
    assert(!FullGamePolishRepairPolicy.shouldAttemptSegmentRepair(List("san_order_violation", "count_budget_exceeded")))
  }

  test("segment repair still runs for semantic failures") {
    assert(FullGamePolishRepairPolicy.shouldAttemptSegmentRepair(List("san_core_missing")))
    assert(FullGamePolishRepairPolicy.shouldAttemptSegmentRepair(List("strategy_coverage_low")))
  }

  test("whole-prose retry is skipped for purely structural merged failures once soft repair was applied") {
    assert(!FullGamePolishRepairPolicy.shouldRetryWholeProseAfterMergedFailure(List("san_order_violation"), softRepairApplied = true))
    assert(!FullGamePolishRepairPolicy.shouldRetryWholeProseAfterMergedFailure(List("anchor_missing"), softRepairApplied = false))
  }

  test("whole-prose retry remains available for merged strategy failures") {
    assert(FullGamePolishRepairPolicy.shouldRetryWholeProseAfterMergedFailure(List("strategy_coverage_low"), softRepairApplied = false))
    assert(FullGamePolishRepairPolicy.shouldRetryWholeProseAfterMergedFailure(List("strategy_coverage_low", "san_core_missing"), softRepairApplied = false))
  }
end FullGamePolishRepairPolicyTest
