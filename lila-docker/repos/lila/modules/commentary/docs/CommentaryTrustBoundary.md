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
  concrete causal anchor in the rendered claim itself. Fork, pin, skewer,
  discovered-attack, and trapped-piece prose require concrete square anchors
  plus checked-PV ownership; trapped-piece prose also needs replayed board
  confirmation that the played piece attacks the trapped target. A typed
  relation witness can own tactical prose before strict fallback only when its
  typed projection is valid, the typed local-fact relation surface from the
  relation catalog is `TacticalRelation`, and the replayed line is played-first.
  A typed `forced_line_truth` producer
  can own tactical prose when `ForcedLineTruth.detect` validates the reviewed
  move and the result is carried as a `pv_coupled_line` local fact. Non-tactical
  relation witnesses remain strict-fallback or support-only. Line-geometry
  pressure from relation witnesses may support pressure wording, but it cannot
  create `ConcreteTactical` ownership unless it separately targets the king as an
  attack. `MobilityRestriction` relation witnesses are bounded pressure facts,
  and `MoveOrder` relation witnesses are bounded timing facts; either may
  suppress a generic same-target offensive post-move pressure descriptor so the
  renderer does not flatten domination or zwischenzug evidence into a plain
  target fact. They do not suppress defensive only-move target truth;
- forced/only move: only with `ForcingDefense` and defensive witness; certified
  only-move truth must enter as `only_move_defense`, not raw `truth_contract`;
  missed benchmark-only moves cannot own a timing claim for the played move.
  Explicit reply-loss and delayed-only-move evidence must enter through
  `forced_reply` local fact authority. A unique reply can authorize forced
  wording; UCI anchors without board-derived SAN may only be described by their
  destination, and square-only or non-unique replies stay bounded as
  timing/defense evidence;
- timing/now: only with typed timing witness or admissible timing contrast;
- alternative/better: only with `AlternativeComparison` and branch role proof;
- checked line/result: only with `LineConsequence` or line-backed
  `AlternativeComparison`. A reviewed-move `LineConsequenceEvidence` packet can
  become planner authority whenever it is surface-ready, non-preview, and
  played-move-owned; this admission is packet-based, not a side effect of a
  broad risk gate;
- plan viability: only when the plan-support packet survives the truth/risk
  gate and is emitted as a `plan_support` local fact by
  `certified_strategy_delta` over a played-first `pv_coupled_line` with a
  meaningful checked continuation and at least one matched plan-anchor SAN
  token; one-ply refs that only repeat the reviewed move, and checked lines that
  do not hit the plan anchor, remain fallback or diagnostic-only. High-risk rows
  must not say the bad move keeps a plan viable. Player-surface "Practical
  plan" rows and practical objective/step rows follow the same rule:
  `PvCoupledOnly` evaluated plans alone are diagnostic, not public plan
  authority;
- practical position support: only when a `StrategyPack` idea is projected as a
  `practical_position_support` local fact by `certified_strategy_delta`. The
  projected fact must match the played side, carry evidence refs, pass
  readiness/confidence gating, touch the reviewed move through a target square,
  focus file, route, or move ref, and be rendered with a played-first checked
  PV. This can authorize bounded pressure, counterplay-restraint, or
  plan-support wording; it does not authorize raw practical-row prose or broad
  plan causality;
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

Filtered local-fact results from the basic idea surface may enter CausalFrame
only for causal families such as threat, attack, pressure, plan support,
defense, and timing. Opening, castling, capture, and endgame basic descriptors
remain fallback render surfaces unless another typed owner admits them.
When planner selection leaves such a causal local fact unrended, the fallback
lane must try CausalFrame admission before direct basic rendering. That bridge
does not create a new chess reason; it carries the existing local fact family,
producer, anchors, evidence refs, and line binding into the same authority
model used by planner-owned claims.

Supported-local packets are not a separate chess mechanism by themselves. After
claim-authority admission, they must resolve to the strongest existing producer
that supports their family/source pair, such as `certified_strategy_delta` for
stable move-local defense, pressure, plan-support, and line-consequence facts.
Strategy-pack practical ideas follow the same rule: only the typed local fact,
anchors, guardrails, evidence refs, and checked-line binding are trusted. The
renderer must not reuse raw practical-row text as causal authority.
Relation-witness local facts also carry their cataloged relation surface as
typed data. Planner ownership may consume that field; diagnostic
`relation_surface:*` guardrails are not authority.
The typed surface can narrow the family: mobility restriction stays pressure,
move order stays timing, and an in-between check does not become attack prose
unless a separate attack/threat authority is admitted.
Draw-resource relation surfaces stay defense facts. Stalemate/perpetual wording
requires a typed `relation_witness` local fact with `relationSurface=DrawResource`,
played-first PV/replay binding, and the analyzer's draw-stable resource proof;
renderer text and generic forcing-check line consequence cannot infer that
defensive resource on their own. When this typed draw-resource fact is present,
it may preempt the generic forcing-check consequence surface for the same line.
A move-order relation can own `WhyNow` only as a typed timing packet:
`family=timing`, `producer=relation_witness`, `relationSurface=MoveOrder`, and
played-first PV/replay binding must all survive into CausalFrame. If that typed
fact is rendered as the claim itself, the renderer may mark support as embedded;
raw planner text or diagnostic guardrails still cannot create timing authority.
Checked-line support remains separately gated: the reviewed-move short line may
be attached to embedded typed-fact claims only for played-move causal owners
that already allow checked-line surface. It does not widen timing-only `WhyNow`
claims.
Role-aware alternative comparison has the same preservation requirement: the
engine-best and played branch `LineConsequenceEvidence` packets must survive
inside the comparison carrier. Shallow branch labels and comparative prose are
not enough to certify local-fact evidence refs or guardrails.
The player decision strip follows that boundary. It may render role-aware
comparison text from the enriched runtime carrier only after the visible
MoveReview result has admitted an `alternative_comparison` local fact over a
PV-coupled `line_consequence`; otherwise it uses the older bounded digest plus
surface-line path and does not infer role-aware authority.
Move-local exact-slice packets may be strict fallback candidates only when the
existing supported-local row builder can project the same packet through its
typed exact proof. This reuses row authority/target/proof metadata without
making player-facing row prose a causal source.
Local file-entry pressure follows the same rule: the trusted input is the
`LocalFileEntryBind` exact proof carried by a `HalfOpenFilePressure` packet,
not the "File entry" support row or prevented-plan wording. Exact proof family
classification wins over coarse move-delta labels during CausalFrame
promotion.
Central-break timing follows the same supported-local gate. The trusted input
is a `CentralBreakTimingWitness` exact proof packet admitted while the current
plan source is `central_break_timing`; generic plan-advance labels or surface
phrases cannot create timing authority.

## Alternative Comparison

Role-aware comparison can compare the played move and engine-best/alternative
branch only when the branch roles are explicit. The trusted surface is the
comparison itself, carried as a `line_consequence` family fact with
`alternative_comparison` authority/producer and engine-best/played branch
guardrails. It must include a concrete SAN branch line. It does not become
tactical authority unless a concrete tactical packet also exists.

Missed benchmark-only rows may use this path only as a verified-best versus
played-branch comparison with role-scoped line evidence. They may not be
rendered as played-move timing, forced-reply, or generic better-move prose.

## Line Consequence

Line consequence surface requires FEN/replay validation and a played-move or
branch-role binding. A played-move line consequence that owns the visible claim
must be emitted by the `line_consequence` producer over `pv_coupled_line`
authority. Preview-only, engine-only, or raw branch prose stays
support/diagnostic.

Line consequence can own WhatChanged when surface-ready typed evidence exists;
unmatched plan-support candidates must not suppress that producer or promote an
opening/generic shell.

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
