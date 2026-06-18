# Agent Instructions

## Commentary Reference

For Chesstory commentary-analysis work on this branch, use the current worktree
reference docs under `lila-docker/repos/lila/modules/commentary/docs`:

- `CommentaryProgramMap.md` - onboarding, current status, active CQF map
- `CommentaryPipelineSSOT.md` - compact runtime path and surface map
- `CommentaryTruthBoundary.md` - public truth standards
- `CommentaryTrustBoundary.md` - trust-risk map, CTH audit baseline, defer
  rationale, and trust-hardening priorities

Do not cite branch-external or branch-removed documentation as live reference.
Do not redo the full `producer -> carrier/model -> builder -> outline ->
renderer -> API -> frontend` trace when these reference docs already answer the
request.

Treat these documents as a current-state map and safety boundary, not as a
mandate to preserve every current implementation shape. For cleanup,
deduplication, or suspected AI-generated over-abstraction, inspect the live code
and call sites first; if the code shows duplicated or unnecessary wrappers,
prefer simplifying the implementation while preserving the documented runtime
truth/trust contract. Do not use a doc phrase by itself to justify adding a new
module, helper, test fixture, or parallel boundary.

Relevant scope includes commentary helper modules and consumption paths across
strategic, semantic, endgame, opening, probe, plan, pawn, structure, threat,
practicality, counterfactual, authoring, outline, renderer, API, and frontend.

Re-audit the commentary reference docs only when:

- the user explicitly asks for a fresh audit,
- relevant code changed after the documented snapshot in
  `lila-docker/repos/lila/modules/commentary/src/main`,
  `lila-docker/repos/lila/app/controllers`, or
  `lila-docker/repos/lila/ui/analyse/src`,
- or the task introduces a runtime path not covered by the reference map.

## Minimal Doc Sync

- Update docs only when leaving them unchanged would mislead future work about a
  durable runtime, trust, truth, or ownership boundary.
- Pipeline reference changes belong in `CommentaryPipelineSSOT.md`.
- Trust-relevant reference changes belong in `CommentaryTrustBoundary.md`.
  This includes fallback truth projection or rewrite behavior, cross-surface
  contract consumption, support-only carrier exposure that can alter
  user-facing implication, lexicon/template claim boundaries, and
  lesson-readiness guards or defer rationale.
- Truth/signoff reference changes belong in `CommentaryTruthBoundary.md`.
- Durable helper names, audited runtime package paths, or directory ownership
  changes can update the relevant reference docs in the same change.
- Documentation wording changes that only clarify agent workflow do not require
  broad runtime doc rewrites. Keep them narrow and avoid turning cleanup notes
  into new doc rules.

## Documentation Hygiene

The commentary reference docs are boundary maps, not a work log. Do not add
slice-by-slice implementation history, post-hoc success reports, replay output
dumps, row-by-row fixture notes, test transcripts, TODO journals, or agent
process commentary to these docs.

Only update a reference doc when a durable runtime contract, trust boundary,
truth/signoff rule, package ownership boundary, or durable helper name changed
and the existing doc would otherwise mislead the next change. In that case:

- describe the stable contract, not the patch that introduced it;
- merge the new rule into the existing section instead of appending another
  dated or narrative block;
- replace or delete stale wording rather than preserving every previous note;
- keep examples short and representative, not a corpus ledger;
- avoid temporary labels such as `Phase`, `Step`, `V2`, `temporary`,
  `follow-up`, `slice`, `audit`, or `gate run` unless they are durable product
  terms already present in live code;
- do not update docs only to justify a code change, a fixture row, a QC run, or
  a cleanup pass.

If a change is tooling-only, test-only, or cleanup-only and does not alter a
documented runtime/trust/truth contract, report it in the final answer rather
than adding it to the reference docs.

## Hard Guardrails

- Board truth first: CTH, strategic-discovery, and trust-hardening claims must
  be anchored to exact corpus rows, PGN move indexes, or FEN-backed positions.
  If best-defense, persistence, continuation, or release-risk matters, use the
  best available engine/probe/PV evidence. Without exact-position support, keep
  the claim `deferred`, support-only, or negative-first.
- Centralize exact-slice policy: owner source, witness extraction,
  continuation/persistence/release rules, and scope overrides should live in a
  shared runtime boundary first. Downstream planner and renderer code should
  consume the certified result instead of repeating ad hoc string checks.
- Do not admit planner ownership from broad scope alone. For example,
  `PositionLocal` by itself must not admit `WhatMattersHere`; use a certified
  slice/source predicate.
- Reuse existing commentary planner/build/replay architecture. Do not introduce
  parallel runtime paths for CQF or commentary-analysis helpers.
- Prefer deletion, in-place simplification, or reuse of an existing boundary
  over adding another `Evidence`, `Support`, `Boundary`, `Contract`, or
  `Policy` object. If such a file already duplicates another concept, identify
  the overlap explicitly before extending it.
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
