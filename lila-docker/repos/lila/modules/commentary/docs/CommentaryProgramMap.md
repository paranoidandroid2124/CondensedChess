# Commentary Program Map

This is the short onboarding map for the current Chesstory commentary worktree.
Use it to find likely boundaries and runtime paths. Live code, typed models,
tests, replay output, and corpus rows are stronger evidence than this document.

## Operating Goal

MoveReview quality work should improve producer skill and evidence paths, not
only hide bad sentences. When a chess claim is wrong or too strong, trace the
claim family and evidence path first, then either rebuild the claim from typed
board/PV/eval/probe/tablebase/analyzer evidence or demote it to support-only or
diagnostic output.

Renderer, template, fallback, and frontend code are presentation layers. They
may arrange typed payloads, but they do not create new chess meaning from prose,
row labels, diagnostic strings, source strings, or tags.

## Reference Documents

Read the documents in this order:

1. `CommentaryProgramMap.md` - navigation and operating goal.
2. `CommentaryPipelineSSOT.md` - compact runtime path and surface map.
3. `CommentaryTruthBoundary.md` - public truth standards.
4. `CommentaryTrustBoundary.md` - user-facing trust-risk standards.

Do not use branch-external or branch-removed documents as live reference for
this worktree.

## Runtime Shape

The intended single path is:

`producer -> typed local fact / CausalFrame / source payload -> planner /
carrier -> MoveReviewPlayerSurface -> renderer / frontend`

The main current surface contract is:

- `decisionComparison`: verdict.
- `summaryRows`: main "why" explanation.
- `advancedRows`: next plan or follow-up direction.
- `refSans` plus `refs`: replay line and board sync.
- `probeRows` and `authorRows`: follow-up checks or unresolved questions.

If a resolved `WhyThis` answer is supposed to be the main player-facing reason,
it belongs in `summaryRows` or `advancedRows`, not only in `authorRows`.

## Cleanup Policy

The docs are reference maps, not preservation orders. If live code shows
duplicated wrappers, stale rollout names, or parallel helper paths, prefer
deletion, in-place simplification, or reuse of an existing boundary.

Before adding a new module, helper, abstraction, or file:

- search related source, types, constants, tests, and call sites;
- reuse or consolidate existing assets when possible;
- keep runtime helpers under active `src/main` paths and corpus/report tooling
  under active `src/test` paths;
- update these docs only when leaving them unchanged would mislead future work.

## Expansion Direction

Prefer work that strengthens existing producers and analyzers. Important areas
include tactical ownership, positional mistake decomposition, line consequence,
opening-to-middlegame plans, material compensation, closed-position
breakthroughs, and endgame conversion/draw boundaries.

Do not add a new boundary split just because a sentence is bad. First decide
whether the idea is true, which claim family it belongs to, and what typed
evidence is missing.
