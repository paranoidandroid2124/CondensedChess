# Agent Instructions

## Commentary Authority

For Chesstory commentary-analysis work on this branch, use the current worktree
authority docs under `lila-docker/repos/lila/modules/commentary/docs`:

- `CommentaryProgramMap.md` - onboarding, current status, active CQF map
- `CommentaryPipelineSSOT.md` - canonical runtime audit
- `CommentaryTruthBoundary.md` - canonical signoff and truth boundary
- `CommentaryTrustBoundary.md` - trust-risk map, CTH audit baseline, defer
  rationale, and trust-hardening priorities

Do not cite branch-external or branch-removed documentation as live authority.
Do not redo the full `producer -> carrier/model -> builder -> outline ->
renderer -> API -> frontend` trace when these master docs already answer the
request.

Relevant scope includes commentary helper modules and consumption paths across
strategic, semantic, endgame, opening, probe, plan, pawn, structure, threat,
practicality, counterfactual, authoring, outline, renderer, API, and frontend.

Re-audit the master commentary docs only when:

- the user explicitly asks for a fresh audit,
- relevant code changed after the documented snapshot in
  `lila-docker/repos/lila/modules/commentary/src/main`,
  `lila-docker/repos/lila/app/controllers`, or
  `lila-docker/repos/lila/ui/analyse/src`,
- or the task introduces a runtime path not covered by the master SSoT.

## Required Doc Sync

- Audited pipeline changes must update `CommentaryPipelineSSOT.md`.
- Trust-relevant behavior changes must update `CommentaryTrustBoundary.md`.
  This includes fallback truth projection or rewrite behavior, cross-surface
  contract consumption, support-only carrier exposure that can alter
  user-facing implication, lexicon/template authority boundaries, and
  lesson-readiness guards or defer rationale.
- Truth/signoff behavior changes must update `CommentaryTruthBoundary.md`.
- Canonical helper names, audited runtime package paths, or directory ownership
  changes must update the relevant master docs in the same change.

## Hard Guardrails

- Board truth first: CTH, strategic-discovery, and trust-hardening claims must
  be anchored to exact corpus rows, PGN move indexes, or FEN-backed positions.
  If best-defense, persistence, continuation, or release-risk matters, use the
  best available engine/probe/PV evidence. Without exact-position support, keep
  the claim `deferred`, support-only, or negative-first.
- Centralize exact-slice policy: owner source, witness extraction,
  continuation/persistence/release rules, and scope overrides should live in a
  canonical runtime boundary first. Downstream planner and renderer code should
  consume the certified result instead of repeating ad hoc string checks.
- Do not admit planner ownership from broad scope alone. For example,
  `PositionLocal` by itself must not admit `WhatMattersHere`; use a certified
  slice/source predicate.
- Reuse existing commentary planner/build/replay architecture. Do not introduce
  parallel runtime paths for CQF or commentary-analysis helpers.
- Keep runtime and tooling separate: live planner/build/replay helpers belong
  under active `src/main/...`; corpus evaluation, scoring, report generation,
  and CQF runners belong under active `src/test/...`.
- Use stable role-based names for source modules, packages, classes, objects,
  runners, and helpers. Avoid temporary rollout labels such as `Track`, `Phase`,
  `Frontier`, `Prototype`, `Kickoff`, `V2`, or `V3` unless they are real
  long-lived domain terms.
- For package or naming cleanup, define the target boundary map before moving
  files. Fix package declarations, imports, `runMain` strings, and doc
  references as part of the same cleanup.
- Run compile plus targeted tests after package moves or naming cleanup before
  reporting `boundary cleanup + verified compile/test`. Do not run multiple
  `sbt`, `testOnly`, or `runMain` commands in parallel against the same
  worktree. If verification is not run, report the result as `boundary cleanup
  only`.
- Do not claim quality gain, acceptance, or signoff from a mixed-worktree diff.
  Separate target slice vs aggregate evidence, runtime behavior vs tooling
  artifacts, and replay-layer issues vs upstream input issues.
