# MoveReview Evaluation Boundary Stabilization Design

## Objective

Stabilize MoveReview practical evaluation by separating three concerns that are currently easy to mix together:

- regression safety gates for truth, evidence, and surface ownership
- quality scoring for useful player-facing explanation
- coverage diagnostics that explain why a strong chess reason did or did not surface

The first implementation should be behavior-preserving and limited to the existing commentary tool/test surface. It should not change runtime commentary generation, user-facing API payloads, or frontend rendering.

## Problem Statement

Naive MoveReview evaluation is producing poor and unstable signals because "bad output" can mean several different things:

- a dangerous factual or chess overclaim
- a wrong family or wrong tier explanation
- a truthful but weak explanation
- a good reason hidden by fallback or surface ownership gates
- a missing evidence bridge from local facts into the player-facing narrative

When these signals are reported through one quality lens, the refactor target becomes unclear. The codebase also has a history of test-shaped constraints, duplicate boundary logic, and parallel helper growth. This design keeps the first cleanup focused on boundary clarity rather than new scoring ambition.

## Existing Assets Searched

These assets are the starting authority for the first implementation:

- `lila-docker/repos/lila/modules/commentary/docs/CommentaryProgramMap.md`
- `lila-docker/repos/lila/modules/commentary/docs/CommentaryPipelineSSOT.md`
- `lila-docker/repos/lila/modules/commentary/docs/CommentaryTruthBoundary.md`
- `lila-docker/repos/lila/modules/commentary/docs/CommentaryTrustBoundary.md`
- `lila-docker/repos/lila/modules/commentaryTools/src/test/scala/lila/commentary/tools/quality/CommentaryQualityContrastSupport.scala`
- `lila-docker/repos/lila/modules/commentaryTools/src/test/scala/lila/commentary/tools/quality/CommentaryQualitySupport.scala`
- `lila-docker/repos/lila/modules/commentaryTools/src/test/scala/lila/commentary/tools/quality/CommentaryQualityQuietSupport.scala`
- `lila-docker/repos/lila/modules/commentaryTools/src/test/scala/lila/commentary/tools/review/CommentaryPlayerQcSupport.scala`
- `lila-docker/repos/lila/modules/commentaryTools/src/test/scala/lila/commentary/tools/moveReview/MoveReviewCorpusRunner.scala`
- `lila-docker/repos/lila/modules/commentaryTools/src/test/scala/lila/commentary/tools/quality/MoveReviewCoverageDiagnostics.scala`
- `lila-docker/repos/lila/modules/commentaryTools/src/test/scala/lila/commentary/tools/quality/MoveReviewEvidenceCoverageAudit.scala`
- `lila-docker/repos/lila/modules/commentaryTools/src/test/scala/lila/commentary/tools/quality/MoveReviewLocalFactualFallbackCorpusAudit.scala`

The search showed that harmful overclaim, wrong-family, wrong-tier, critical-surface, and evidence reference gates are concentrated in `CommentaryQualityContrastSupport.scala`, while coverage and fallback audit logic already exists in separate tool files. The first cleanup should reuse those assets instead of adding a parallel evaluator.

## Boundary Model

### Regression Gate

The regression gate answers whether an output is unsafe or structurally invalid for the claimed surface. It owns:

- harmful overclaim detection
- wrong family detection
- wrong tier detection
- critical surface admission checks
- truth and evidence reference checks used to decide whether a row should fail a gate

This logic remains in the existing quality contrast support path for the first implementation. The cleanup target is to make the boundary explicit inside existing code and remove duplicated local calculations where they are already repeatable in the same file.

### Quality Score

The quality score answers whether an output is helpful, clear, and educational after it passes safety constraints. It should not become the source of truth for chess legality, exact-position support, or surface ownership.

For the first implementation, no rubric changes are made. Existing quality scoring output remains behaviorally stable.

### Coverage Diagnostic

The coverage diagnostic answers why an expected chess reason did or did not appear in the final MoveReview output. It owns:

- local fact coverage
- supported local surface coverage
- evidence reference presence
- fallback mode visibility
- demotion or defer rationale in corpus/tool reports

The first implementation should align naming and local helper usage around existing diagnostic fields. It should not add a new corpus schema unless a later runtime or reporting change requires it.

### Fallback Audit

The fallback audit answers whether exact factual fallback is being used as a safety mechanism rather than as a substitute for unsupported planner ownership. It remains a distinct tool concern because fallback behavior can make a row safe while still exposing a quality or coverage deficit.

## Implementation Scope

The first implementation is intentionally narrow:

- modify existing `commentaryTools/src/test` support files only
- preserve public output schemas and existing runner field names
- avoid new runtime files, public APIs, corpus fixtures, and test files
- prefer deletion, in-place consolidation, and local reuse over new abstractions
- update commentary reference docs only if a durable runtime, trust, or truth contract changes

The expected first code target is `CommentaryQualityContrastSupport.scala` because it currently contains the densest mix of gate, score, evidence, and fallback calculations. If inspection shows that a smaller existing file owns the duplicate more directly, the implementation should pivot to that file.

## Non-Goals

- Do not change MoveReview player-facing behavior.
- Do not introduce a new MoveReview evaluator, score model, or runtime policy.
- Do not make old tests the source of implementation shape.
- Do not add new tests only to lock in current overfit behavior.
- Do not rewrite commentary docs to justify a tool-only cleanup.
- Do not solve corpus quality, chess engine evidence, and narrative generation in this first implementation.

## Verification

Use existing targeted tests and compile checks for the touched tool surface. The likely verification set is:

- `commentaryTools/testOnly lila.commentary.tools.quality.CommentaryQualityContrastSupportTest`
- `commentaryTools/testOnly lila.commentary.tools.quality.MoveReviewEvidenceCoverageAuditTest`
- `commentaryTools/testOnly lila.commentary.tools.quality.MoveReviewLocalFactualFallbackCorpusAuditTest`
- `commentaryTools/testOnly lila.commentary.tools.quality.MoveReviewCoverageDiagnosticsTest`
- `commentaryTools/testOnly lila.commentary.tools.review.CommentaryPlayerQcSupportTest`

If a command cannot run in this workspace, report the exact command and failure rather than claiming verification.

## Success Criteria

- Regression gates, quality scoring, and coverage diagnostics are distinguishable in the touched code.
- Duplicate calculations in the touched file are reduced or clearly identified for a later larger move.
- Net code size does not grow except for the plan/spec files.
- Existing behavior is preserved unless a change is explicitly called out before editing.
- The final report lists searched assets, duplicated logic addressed, helper additions, behavior changes, net line diff, and existing tests run.
