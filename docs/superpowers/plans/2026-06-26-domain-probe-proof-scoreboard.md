# Domain Probe Proof Scoreboard Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Prove how far each domain probe survives through Chesstory graph semantics, rather than inferring improvement from view/detail exposure.

**Architecture:** Keep Q01-Q12 as ignored calibration artifacts under `target/`. Attach audit-only `expectedSemanticSlots` to generated replay input, run the existing phase3 audit runner, and summarize each slot by terminal survival stage, missing status, and strict lineage/borrow signals.

**Tech Stack:** Scala 3, sbt, ignored JSONL/JSON artifacts under `target/domain-probe-calibration`.

---

### Task 1: Build Expected Slot Calibration Input

**Files:**
- Generated only: `target/domain-probe-calibration/round1_expected_slots_input.jsonl`

- [ ] Read `target/domain-probe-calibration/round1_audit_input.jsonl`.
- [ ] Add at least one `expectedSemanticSlot` for each of the 22 existing probe IDs.
- [ ] Keep `expectedQuestionIds` as grouping metadata only.
- [ ] Do not add Q IDs or domain-probe sample IDs to production/test source.

### Task 2: Run Audit And Scoreboard

**Files:**
- Generated only: `target/domain-probe-calibration/round1_expected_slots_output.jsonl`
- Generated only: `target/domain-probe-calibration/round1_slot_scoreboard.json`

- [ ] Run `MoveReviewPhase3AuditRunner` on the expected-slot input.
- [ ] Classify every slot into `missing`, `semantic_detected`, `view_surfaced`, `owned_cause_linked`, `clustered_coherent`, or `borrow_false_positive`.
- [ ] Include per-probe and aggregate counts in the scoreboard.

### Task 3: Missing Slot Fix Triage

**Files:**
- Modify existing source/test files only if a missing slot reflects a representational gap in the existing spine.

- [ ] Inspect missing slots in this order: Q04 `pawn_break_timing`, Q08 `flexible_break_timing`, Q09 `dynamic_center_break`, Q10 `iqp_transition`.
- [ ] Prefer existing `PawnPlayAssessor`, `PositionFactNormalizer`, `EvidenceGraph`, and `PositionPlanTechniqueProjection` paths.
- [ ] Do not add a new production module unless the existing model cannot represent the concept.

### Task 4: Verification

- [ ] Run `sbt --no-server "chessJudgmentCore/testOnly lila.chessjudgment.analysis.qc.MoveReviewPhase3AuditRunnerTest"`.
- [ ] Run `sbt --no-server "chessJudgmentCore/testOnly lila.chessjudgment.model.judgment.MoveJudgmentViewTest"`.
- [ ] Run `sbt --no-server "chessJudgmentCore/test"`.
- [ ] Verify source has no `Q01`, `Q12`, or `domain-probe-Q` references.
- [ ] Verify target calibration files remain ignored.
