# Commentary Program Map

This is the short onboarding map for the current Chesstory commentary worktree.
Use it to find the live authority documents, not to freeze the current
implementation shape.

## Current State

MoveReview commentary now uses a single authority path:

1. producers emit typed evidence, exact facts, truth/risk gates, line
   consequences, timing witnesses, or supported local packets;
2. `QuestionFirstCommentaryPlanner` ranks question candidates and records
   mechanism-specific owner traces;
3. `MoveReviewCausalClaim` builds the CausalFrame and checks typed support,
   causal role, surface permission, and local fact admission;
4. `MoveReviewLocalFact` owns the typed fact-family vocabulary;
5. the renderer realizes admitted facts as prose and falls back to exact local
   facts when no causal authority survives.

The active mechanism families are `ConcreteTactical`, `ForcingDefense`,
`LineConsequence`, `AlternativeComparison`, `DecisionTiming`, `PlanRace`,
`MoveDelta`, `PositionProbe`, `OpeningRelation`, and `EndgameTransition`.
Broad tactical buckets are not part of the live authority model.

## Document Roles

Read the documents in this order:

1. `CommentaryProgramMap.md` - navigation and current state.
2. `CommentaryPipelineSSOT.md` - runtime path, ownership, diagnostics fields.
3. `CommentaryTruthBoundary.md` - chess-truth signoff and truth/risk limits.
4. `CommentaryTrustBoundary.md` - trust risks, renderer permissions, disallowed
   legacy flows.

Do not use branch-external or branch-removed documents as authority for this
worktree.

## Cleanup Policy

The docs are boundaries, not preservation orders. If live code shows duplicated
wrappers, stale rollout names, or parallel helper paths, prefer deletion,
in-place simplification, or reuse of an existing boundary.

Before adding a new module/helper/abstraction:

- search the relevant source, types, constants, tests, and call sites;
- reuse or consolidate existing assets when possible;
- keep runtime helpers under active `src/main` paths and corpus/report tooling
  under active `src/test` paths;
- update the authority docs only when runtime truth, trust, path, package, or
  diagnostic contracts change.

## Current Refactor Boundary

The current MoveReview refactor is considered structurally aligned when:

- `truth_contract` and high-risk gates can close unsafe surfaces but cannot
  invent tactical, plan, timing, or alternative prose;
- certified only-move defense may surface as `only_move_defense`, but raw
  `truth_contract` remains a negative gate, not a prose source;
- `pv_coupled_plan_support` cannot render positive plan viability under a
  truth/risk gate that blocks strategic support;
- `ConcreteTactical` surface prose requires concrete current-move tactical
  evidence and a concrete anchor in the rendered claim;
- line consequence and alternative comparison remain separate owner/fact
  families instead of flowing through a broad tactical owner;
- replay/QC output exposes mechanism-specific diagnostics such as
  `plannerConcreteTacticalSources`, `plannerLineConsequenceSources`, and
  `plannerAlternativeComparisonSources`.

## Next Expansion Direction

The remaining work is producer quality, not another authority split:

- strengthen tactical motif producers so pins, forks, discovered attacks,
  trapped pieces, and forced replies carry board-backed evidence;
- strengthen defensive/timing producers so forcing-defense prose names the
  concrete threat, reply, or lost resource rather than generic risk;
- strengthen plan-support producers so opening/quiet rows do not collapse into
  repeated checked-line template wording;
- improve line-consequence rendering so replayed material/structure changes are
  concise and role-aware.
