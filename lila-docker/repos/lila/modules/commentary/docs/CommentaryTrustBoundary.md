# Commentary Trust Boundary

This document defines what user-facing MoveReview prose may trust. It is kept
short so cleanup and refactoring work are not blocked by outdated rollout
details.

## Core Rule

Renderer prose may only express claims that have typed evidence authority.
Planner scene names, source strings, and truth-contract risk labels are
diagnostics unless a CausalFrame/local fact contract admits them.

## Risk Gates

`DecisiveTruthContract.blocksStrategicSupport` and related tactical-risk signals
are negative gates. They may suppress opening, strategic, practical, or broad
positive prose. They do not authorize:

- tactical motif claims,
- forced-reply claims,
- timing claims,
- alternative comparison claims,
- line-consequence claims,
- or strategic concession claims.

The trusted path is risk gate plus typed mechanism evidence, not risk gate alone.

## Surface Permissions

The surface renderer may say:

- tactical: only with `ConcreteTactical`, motif/current-move evidence, and a
  concrete causal anchor in the rendered claim itself;
- forced/only move: only with `ForcingDefense` and defensive witness; certified
  only-move truth must enter as `only_move_defense`, not raw `truth_contract`;
  missed benchmark-only moves cannot own a timing claim for the played move;
- timing/now: only with typed timing witness or admissible timing contrast;
- alternative/better: only with `AlternativeComparison` and branch role proof;
- checked line/result: only with `LineConsequence` or line-backed
  `AlternativeComparison`;
- plan viability: only when the plan-support packet survives the truth/risk
  gate; high-risk rows must not say the bad move keeps a plan viable;
- factual-only: when no causal authority survives.

## Local Fact Families

Typed local fact families are the trusted renderer input:

- `threat`
- `defense`
- `line_consequence`
- `timing`
- `pressure`
- `plan_support`
- `opening_goal`
- `endgame`
- exact factual fallback

A family may only be emitted by an authority that supports that family. For
example, a line consequence packet cannot certify a fork or pin unless a
separate tactical motif authority exists.

## Alternative Comparison

Role-aware comparison can compare the played move and engine-best/alternative
branch only when the branch roles are explicit. The trusted surface is the
comparison itself. It does not become tactical authority unless a concrete
tactical packet also exists.

Missed benchmark-only rows may use this path only as a verified-best versus
played-branch comparison with role-scoped line evidence. They may not be
rendered as played-move timing, forced-reply, or generic better-move prose.

## Line Consequence

Line consequence surface requires FEN/replay validation and a played-move or
branch-role binding. Preview-only, engine-only, or raw branch prose stays
support/diagnostic.

## Fallback

Fallback renderers may state exact move facts and short checked-line previews.
They must not infer tactics, plans, timing, or alternatives from missing typed
evidence.

## Disallowed Legacy Flow

The following flows are not trusted:

- `truth_contract -> tactical owner`
- `truth_contract -> only-move prose`
- `blocksStrategicSupport -> tactical scene`
- `line_consequence -> tactical owner`
- `role_aware_line_consequence -> tactical owner`
- broad source-string checks as threat authority
- raw template composition as causal authority
