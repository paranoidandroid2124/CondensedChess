# MoveReview Evaluation Boundary Stabilization Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make the MoveReview regression gate summary code distinguish gate metrics from quality scoring and coverage diagnostics while preserving existing behavior.

**Architecture:** Keep the first change inside the existing `commentaryTools` support surface. Consolidate repeated before/after/delta gate metric calculations in `CommentaryQualityContrastSupport.scala` with one private same-file helper, then verify through existing tests.

**Tech Stack:** Scala 3, Play JSON, lila `commentaryTools` test module, sbt.

---

## File Structure

- Modify: `lila-docker/repos/lila/modules/commentaryTools/src/test/scala/lila/commentary/tools/quality/CommentaryQualityContrastSupport.scala`
  - Responsibility: existing contrast report and MoveReview regression gate support.
  - Change: add a small private metric value/helper and rewrite the gate summary local calculations to use it.
- Test: `lila-docker/repos/lila/modules/commentaryTools/src/test/scala/lila/commentary/tools/quality/CommentaryQualityContrastSupportTest.scala`
  - Responsibility: existing gate behavior assertions. No new tests are added for this behavior-preserving cleanup.
- No runtime files, public APIs, schemas, runner output names, corpus fixtures, or commentary reference docs are changed.

## Project-Specific Constraints

- This is behavior-preserving cleanup, not TDD feature work.
- Do not create new files or public helpers for runtime use.
- Do not commit from the dirty worktree unless the user explicitly asks.
- Treat tests as regression signals, not as authority for preserving overfit implementation shape.

### Task 1: Consolidate Gate Summary Metric Calculation

**Files:**
- Modify: `lila-docker/repos/lila/modules/commentaryTools/src/test/scala/lila/commentary/tools/quality/CommentaryQualityContrastSupport.scala`
- Test: `lila-docker/repos/lila/modules/commentaryTools/src/test/scala/lila/commentary/tools/quality/CommentaryQualityContrastSupportTest.scala`

- [ ] **Step 1: Add a private same-file gate metric helper**

Insert this helper after `private final case class MoveReviewGateSignals` and before `private def moveReviewGateSignals`:

```scala
  private final case class MoveReviewGateMetric(before: Int, after: Int):
    val delta: Int = after - before

  private def moveReviewGateMetric(
      rows: List[MoveReviewQualityGateRow]
  )(
      before: MoveReviewQualityGateRow => Boolean,
      after: MoveReviewQualityGateRow => Boolean
  ): MoveReviewGateMetric =
    MoveReviewGateMetric(rows.count(before), rows.count(after))
```

This helper is allowed by the cleanup rules because the same before/after/delta calculation is repeated across the gate summary for every MoveReview gate metric, and no existing helper covers it.

- [ ] **Step 2: Replace the repeated count and delta locals**

Inside `buildMoveReviewQualityGateSummary`, replace the block from `val harmfulBefore = ...` through `val criticalOverstrongDelta = ...` with:

```scala
    val harmful = moveReviewGateMetric(rows)(_.beforeHarmfulOverclaimRisk, _.afterHarmfulOverclaimRisk)
    val wrongFamily = moveReviewGateMetric(rows)(_.beforeWrongFamilyRisk, _.afterWrongFamilyRisk)
    val wrongTier = moveReviewGateMetric(rows)(_.beforeWrongTierRisk, _.afterWrongTierRisk)
    val generic = moveReviewGateMetric(rows)(_.beforeGenericFallback, _.afterGenericFallback)
    val exact = moveReviewGateMetric(rows)(_.beforeExactEvidenceSurface, _.afterExactEvidenceSurface)
    val evidence = moveReviewGateMetric(rows)(_.beforeEvidenceBoundSurface, _.afterEvidenceBoundSurface)
    val criticalTruth = moveReviewGateMetric(rows)(_.beforeCriticalTruthRow, _.afterCriticalTruthRow)
    val criticalPresent = moveReviewGateMetric(rows)(_.beforeCriticalSurfacePresent, _.afterCriticalSurfacePresent)
    val criticalMissing = moveReviewGateMetric(rows)(_.beforeCriticalSurfaceMissing, _.afterCriticalSurfaceMissing)
    val criticalGeneric = moveReviewGateMetric(rows)(_.beforeCriticalSurfaceGeneric, _.afterCriticalSurfaceGeneric)
    val criticalWrongFamily = moveReviewGateMetric(rows)(_.beforeCriticalSurfaceWrongFamily, _.afterCriticalSurfaceWrongFamily)
    val criticalOverstrong = moveReviewGateMetric(rows)(_.beforeCriticalSurfaceOverstrong, _.afterCriticalSurfaceOverstrong)
```

- [ ] **Step 3: Rewrite `metricGain` to use the metric values**

Replace the existing `metricGain` expression with:

```scala
    val metricGain =
      (-harmful.delta).max(0) +
        (-wrongFamily.delta).max(0) +
        (-wrongTier.delta).max(0) +
        (-generic.delta).max(0) +
        exact.delta.max(0) +
        evidence.delta.max(0) +
        criticalTruth.delta.max(0) +
        criticalPresent.delta.max(0) +
        (-criticalMissing.delta).max(0) +
        (-criticalGeneric.delta).max(0) +
        (-criticalWrongFamily.delta).max(0) +
        (-criticalOverstrong.delta).max(0)
```

- [ ] **Step 4: Rewrite blocking reason checks to use the metric values**

Replace the existing metric-dependent `Option.when` calls inside `blockingReasons` with:

```scala
        Option.when(sourceIdentityMismatches.nonEmpty)("source_identity_mismatch"),
        Option.when(harmful.delta > 0)("harmful_overclaim_increased"),
        Option.when(wrongFamily.delta > 0)("wrong_family_risk_increased"),
        Option.when(wrongTier.delta > 0)("wrong_tier_risk_increased"),
        Option.when(exact.delta < 0)("exact_evidence_surface_decreased"),
        Option.when(evidence.delta < 0)("evidence_bound_surface_delta_negative"),
        Option.when(generic.delta > 0 && exact.delta <= 0)("generic_fallback_increased_without_exact_evidence_gain"),
        Option.when(criticalTruth.delta < 0)("critical_truth_rows_decreased"),
        Option.when(criticalPresent.delta < 0)("critical_surface_present_decreased"),
        Option.when(criticalMissing.delta > 0)("critical_surface_missing_increased"),
        Option.when(criticalGeneric.delta > 0)("critical_surface_generic_increased"),
        Option.when(criticalWrongFamily.delta > 0)("critical_surface_wrong_family_increased"),
        Option.when(criticalOverstrong.delta > 0)("critical_surface_overstrong_increased"),
        Option.when(codeCost.exists(_.netLineDelta > 0) && metricGain == 0)("net_code_growth_without_gate_metric_gain")
```

- [ ] **Step 5: Rewrite summary field assignments to use the metric values**

Replace only the gate metric field assignments in the `MoveReviewQualityGateSummary(...)` call with:

```scala
      harmfulOverclaimBefore = harmful.before,
      harmfulOverclaimAfter = harmful.after,
      harmfulOverclaimDelta = harmful.delta,
      wrongFamilyRiskBefore = wrongFamily.before,
      wrongFamilyRiskAfter = wrongFamily.after,
      wrongFamilyRiskDelta = wrongFamily.delta,
      wrongTierRiskBefore = wrongTier.before,
      wrongTierRiskAfter = wrongTier.after,
      wrongTierRiskDelta = wrongTier.delta,
      genericFallbackBefore = generic.before,
      genericFallbackAfter = generic.after,
      genericFallbackDelta = generic.delta,
      exactEvidenceSurfaceBefore = exact.before,
      exactEvidenceSurfaceAfter = exact.after,
      exactEvidenceSurfaceDelta = exact.delta,
      evidenceBoundSurfaceBefore = evidence.before,
      evidenceBoundSurfaceAfter = evidence.after,
      evidenceBoundSurfaceDelta = evidence.delta,
      criticalTruthRowsBefore = criticalTruth.before,
      criticalTruthRowsAfter = criticalTruth.after,
      criticalTruthRowsDelta = criticalTruth.delta,
      criticalSurfacePresentBefore = criticalPresent.before,
      criticalSurfacePresentAfter = criticalPresent.after,
      criticalSurfacePresentDelta = criticalPresent.delta,
      criticalSurfaceMissingBefore = criticalMissing.before,
      criticalSurfaceMissingAfter = criticalMissing.after,
      criticalSurfaceMissingDelta = criticalMissing.delta,
      criticalSurfaceGenericBefore = criticalGeneric.before,
      criticalSurfaceGenericAfter = criticalGeneric.after,
      criticalSurfaceGenericDelta = criticalGeneric.delta,
      criticalSurfaceWrongFamilyBefore = criticalWrongFamily.before,
      criticalSurfaceWrongFamilyAfter = criticalWrongFamily.after,
      criticalSurfaceWrongFamilyDelta = criticalWrongFamily.delta,
      criticalSurfaceOverstrongBefore = criticalOverstrong.before,
      criticalSurfaceOverstrongAfter = criticalOverstrong.after,
      criticalSurfaceOverstrongDelta = criticalOverstrong.delta,
```

- [ ] **Step 6: Inspect the diff**

Run from the repository root:

```powershell
git diff -- lila-docker/repos/lila/modules/commentaryTools/src/test/scala/lila/commentary/tools/quality/CommentaryQualityContrastSupport.scala
```

Expected: the diff contains only the private helper addition and the gate summary calculation rewrite. There are no schema, JSON format, runner output, or gate reason string changes.

### Task 2: Verify Existing Behavior

**Files:**
- Test: `lila-docker/repos/lila/modules/commentaryTools/src/test/scala/lila/commentary/tools/quality/CommentaryQualityContrastSupportTest.scala`

- [ ] **Step 1: Run the targeted existing gate test**

Run from `lila-docker/repos/lila`:

```powershell
sbt "commentaryTools/testOnly lila.commentary.tools.quality.CommentaryQualityContrastSupportTest"
```

Expected: all tests in `CommentaryQualityContrastSupportTest` pass. If sbt is unavailable, dependency resolution is blocked, or the command fails because of pre-existing dirty worktree issues, report the exact failure.

- [ ] **Step 2: Run a syntax/diff sanity check**

Run from the repository root:

```powershell
git diff --check -- lila-docker/repos/lila/modules/commentaryTools/src/test/scala/lila/commentary/tools/quality/CommentaryQualityContrastSupport.scala
```

Expected: no whitespace errors.

- [ ] **Step 3: Record cleanup accounting**

Run from the repository root:

```powershell
git diff --numstat -- docs/superpowers/specs/2026-06-18-move-review-evaluation-boundary-stabilization-design.md docs/superpowers/plans/2026-06-18-move-review-evaluation-boundary-stabilization.md lila-docker/repos/lila/modules/commentaryTools/src/test/scala/lila/commentary/tools/quality/CommentaryQualityContrastSupport.scala
```

Expected: output gives the net line diff for the spec, plan, and code files touched by this work. Use the code-file row when reporting cleanup net diff; mention the spec/plan rows separately.

## Self-Review

- Spec coverage: this plan implements the narrow first cleanup from the design by touching only the existing quality contrast support file and using the existing gate test.
- Placeholder scan: no placeholder tasks are present.
- Type consistency: `MoveReviewGateMetric` uses `Int` before/after counts and a derived `delta`, matching the existing `MoveReviewQualityGateSummary` integer fields.
- Behavior scope: output schema, reason strings, gate status logic, and report file names are unchanged.
