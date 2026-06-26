# Chesstory Domain Probe Semantic Survival Work Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Preserve general chess meaning units from calibration positions as graph semantics without hardcoding opening-specific question labels.

**Architecture:** Keep the existing `RelativeCauseFact -> PositionPlanTechnique -> view/audit` spine. Improve middle-layer preservation through axis labels, object bindings, semantic detail tokens, and audit-only probe matching.

**Tech Stack:** Scala 3, sbt, chessJudgmentCore.

---

## Summary

- Opening-specific examples like Ruy, Catalan, Lucena, and Philidor must not become production constants.
- General meanings such as center tension, break timing, counterplay race, long diagonal pressure, outpost, bad bishop, IQP compensation, and rook ending technique should survive through graph semantics.
- Do not broaden `PositionPlanTechnique` frame attachment or reintroduce source-evidence-only cause/frame matching.

## Tasks

### Task 1: Calibration Input

**Files:**
- Create generated/untracked: `target/domain-probe-calibration/round1_questions.json`

- [ ] Read `Q01-Q12` from git history commit `a379bbe2cf`.
- [ ] Save the questions as untracked calibration input under `target/domain-probe-calibration/`.
- [ ] Use the calibration set only as a probe source; do not reference `Q01-Q12` in production code.

### Task 2: Audit Probe Tests

**Files:**
- Modify: `lila-docker/repos/lila/modules/chessJudgmentCore/src/test/scala/lila/chessjudgment/analysis/qc/MoveReviewPhase3AuditRunnerTest.scala`

- [ ] Add failing tests showing explicit `EndgameTechniqueRecipe` expected slots can match view-surfaced Lucena/Philidor semantic anchors.
- [ ] Add a failing test showing unit-only expected slots still do not become object-bound.
- [ ] Add a failing test showing pawn-break cause-owned probes require co-located detail tokens and cause evidence.

### Task 3: QC Expected-Slot Expansion

**Files:**
- Modify: `lila-docker/repos/lila/modules/chessJudgmentCore/src/test/scala/lila/chessjudgment/analysis/qc/MoveReviewPhase3AuditMetrics.scala`

- [ ] Let `expectedSemanticSlots` match explicit domain probes outside `primaryActionableStructuralOpportunity`.
- [ ] Only include view-only/domain rows when the expected slot has real semantic requirements such as axis key, mechanism/cause/root kinds, detail tokens, semantic anchor tokens, object binding tokens, or a terminal stage beyond unit detection.
- [ ] Leave `semanticRubricFunnelJson` primary structural metrics unchanged.

### Task 4: Pawn Tension And Break Specificity

**Files:**
- Modify: `lila-docker/repos/lila/modules/chessJudgmentCore/src/main/scala/lila/chessjudgment/analysis/singlePosition/SinglePositionAssessment.scala`
- Modify: `lila-docker/repos/lila/modules/chessJudgmentCore/src/main/scala/lila/chessjudgment/analysis/singlePosition/PawnPlayAssessor.scala`
- Modify: `lila-docker/repos/lila/modules/chessJudgmentCore/src/main/scala/lila/chessjudgment/model/judgment/EvidenceGraph.scala`
- Modify: `lila-docker/repos/lila/modules/chessJudgmentCore/src/main/scala/lila/chessjudgment/model/judgment/PositionPlanTechniqueProjection.scala`
- Modify: `lila-docker/repos/lila/modules/chessJudgmentCore/src/main/scala/lila/chessjudgment/analysis/qc/JudgmentQualityMetrics.scala`
- Modify: `lila-docker/repos/lila/modules/chessJudgmentCore/src/test/scala/lila/chessjudgment/analysis/qc/MoveReviewPhase3AuditViewJson.scala`

- [ ] Add defaulted `tensionEdges` and `counterBreakFiles` fields.
- [ ] Populate `tensionEdges` as origin-target pawn tension edges.
- [ ] Populate `counterBreakFiles` from opponent `PawnBreak` motifs.
- [ ] Make `pawnPlayAxis` labels stable and semantic while preserving existing axis kind/polarity.
- [ ] Expose tokens as `tensionEdge:*` and `counterBreakFile:*`.

### Task 5: Concrete Object Ownership

**Files:**
- Modify: `lila-docker/repos/lila/modules/chessJudgmentCore/src/main/scala/lila/chessjudgment/analysis/position/PositionFactNormalizer.scala`
- Modify: `lila-docker/repos/lila/modules/chessJudgmentCore/src/main/scala/lila/chessjudgment/model/judgment/PositionPlanTechniqueProjection.scala`

- [ ] Set weak-square and outpost `targetSquare` when a concrete square exists.
- [ ] Let resource-contest details use concrete file, weak-square, outpost, or battery anchors.
- [ ] Do not treat side-only `CounterplayRestraint` as concrete object ownership.

### Task 6: Rook Ending Technique Geometry

**Files:**
- Modify: `lila-docker/repos/lila/modules/chessJudgmentCore/src/main/scala/lila/chessjudgment/model/Fact.scala`
- Modify: `lila-docker/repos/lila/modules/chessJudgmentCore/src/main/scala/lila/chessjudgment/analysis/position/FactExtractor.scala`
- Modify: `lila-docker/repos/lila/modules/chessJudgmentCore/src/main/scala/lila/chessjudgment/analysis/position/PositionFactNormalizer.scala`

- [ ] Extend `Fact.RookEndgamePattern` with defaulted `anchorSquares: List[Square] = Nil`.
- [ ] Derive anchor squares for detected rook endgame patterns from kings, rooks, passed pawns, and promotion squares.
- [ ] Preserve these squares as `requiredSquare:*` through existing `EndgameTechniqueRecipe` projection.

### Task 7: Verification

- [ ] Run `sbt --no-server "chessJudgmentCore/testOnly lila.chessjudgment.analysis.qc.MoveReviewPhase3AuditRunnerTest"` from `lila-docker/repos/lila`.
- [ ] Run `sbt --no-server "chessJudgmentCore/testOnly lila.chessjudgment.model.judgment.MoveJudgmentViewTest"` if projection/object binding tests are added there.
- [ ] Run `sbt --no-server "chessJudgmentCore/test"` from `lila-docker/repos/lila`.
- [ ] Verify production source does not reference `Q01`, `Q12`, or calibration question IDs.
- [ ] Verify generated calibration files remain untracked.
