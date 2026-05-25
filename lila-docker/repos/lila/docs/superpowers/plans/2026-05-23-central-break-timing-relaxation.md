# Central Break Timing Relaxation Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Relax `central_break_timing` support rows so exact board-backed central breaks are not blocked by best-line exclusivity or branch-key bookkeeping.

**Architecture:** Keep the slice boundary centralized in `CentralBreakTimingWitness` and existing claim-authority paths. Remove PV-gap and branch-key hard gates only for this SupportedLocal exact slice, keep board-link, source/family, packet, and tactical-veto requirements.

**Tech Stack:** Scala 3, munit, sbt `commentaryTools/testOnly`, existing MoveReview corpus/audit tooling.

---

### Task 1: Red Tests

**Files:**
- Modify: `modules/commentaryTools/src/test/scala/lila/commentary/analysis/MoveReviewSupportedLocalSurfaceRowsTest.scala`
- Modify: `modules/commentaryTools/src/test/scala/lila/commentary/analysis/CentralBreakTimingPolicyTest.scala`

- [ ] Change the below-40cp tests to expect central release/support instead of suppression.
- [ ] Add a one-ply top-PV fixture to show that missing branch key must not suppress a board-backed central break.
- [ ] Update surface text expectations from `e-break`/`d-break` to route-shaped tokens such as `e4-e5` and `d4-d5`.
- [ ] Run `sbt "commentaryTools/testOnly lila.commentary.analysis.MoveReviewSupportedLocalSurfaceRowsTest lila.commentary.analysis.CentralBreakTimingPolicyTest"` and confirm the new expectations fail.

### Task 2: Runtime Relaxation

**Files:**
- Modify: `modules/commentaryCore/src/main/scala/lila/commentary/analysis/CentralBreakTimingWitness.scala`
- Modify: `modules/commentaryCore/src/main/scala/lila/commentary/analysis/MoveReviewPlayerPayloadBuilder.scala`
- Modify: `modules/commentaryCore/src/main/scala/lila/commentary/analysis/PlayerFacingTruthModePolicy.scala`

- [ ] In `CentralBreakTimingWitness`, keep `pvGapCp` as diagnostic evidence but stop requiring it to be `>= 40`.
- [ ] Stop treating missing branch key as a blocker when a legal central break and board link are present.
- [ ] Produce route-shaped `breakToken` values from the legal move (`e4-e5`, `...d6-d5`) while keeping existing `breakSquare` and `breakMove`.
- [ ] In the central surface gate, accept route-shaped central break tokens and render neutral wording: `On the checked line, this keeps the <token> break available on this branch.`
- [ ] In `PlayerFacingTruthModePolicy`, remove the central-only branch-key requirement for weak-main/support-local release while preserving same-branch and persistence checks derived from the exact witness.

### Task 3: Diagnostics And Docs

**Files:**
- Modify: `modules/commentaryTools/src/test/scala/lila/commentary/tools/moveReview/MoveReviewCoverageDiagnostics.scala`
- Modify: `modules/commentaryTools/src/test/scala/lila/commentary/tools/quality/MoveReviewEvidenceCoverageAudit.scala`
- Modify: `modules/commentary/docs/CommentaryPipelineSSOT.md`
- Modify: `modules/commentary/docs/CommentaryTrustBoundary.md`
- Modify: `modules/commentary/docs/CommentaryTruthBoundary.md`

- [ ] Ensure diagnostics still count central rows only when the exact witness and surface token gate pass.
- [ ] Update audit token recognition for route-shaped central break text and keep generic fallback count at zero for valid visible rows.
- [ ] Update docs to say PV gap and branch key are diagnostics for this SupportedLocal row, not hard release gates.

### Task 4: Verification

- [ ] Run targeted central tests.
- [ ] Run diagnostics/audit tests touched by the wording and token changes.
- [ ] Run full `sbt "commentaryTools/test"`.
- [ ] Run the current MoveReview corpus/audit when full tests are green and report central row count, named-token count, generic fallback count, and tactical visible rows.
