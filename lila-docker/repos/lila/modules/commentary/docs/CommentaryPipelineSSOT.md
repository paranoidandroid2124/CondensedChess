# Commentary Pipeline SSoT

This document is the current runtime map for Chesstory commentary. It is a
boundary map, not a preservation order. Live code, typed models, call sites, and
runtime replay behavior remain the final authority.

## Runtime Path

MoveReview commentary flows through one path:

1. `NarrativeContext` and `MoveReviewRefs` carry board, move, PV, eval, and
   replay context.
2. Producers and analyzers build typed evidence packets, local facts,
   CausalFrame inputs, source payloads, and support diagnostics.
3. Planner/carrier code selects the claim family and surface permission from
   typed evidence. It may close or demote unsupported claims.
4. `MoveReviewPlayerSurface` exposes the selected player-facing structure.
5. Renderer and frontend code realize that structure as prose, scenes, board
   sync, and replay controls.

There is no parallel MoveReview explanation path. If no typed causal authority
survives, the public surface must stay factual, support-only, diagnostic, or
silent.

## Authority Model

Keep these concepts separate:

- `RiskGate`: suppresses or demotes unsafe positive explanation. It does not
  create a chess reason.
- `CausalMechanism`: the claim family, such as tactical ownership, defensive
  necessity, line consequence, alternative comparison, timing, plan support,
  opening context, endgame transition, or factual fallback.
- `EvidenceAuthority`: the typed source that permits the claim, such as board
  geometry, PV/replay binding, eval gap, probe result, tablebase result,
  analyzer packet, or admitted local fact.
- `SurfacePermission`: the allowed public strength and wording.
- `SceneSummary`: diagnostic or UI grouping only. It is not source authority.

Source prose, row labels, opening names, endgame names, diagnostic strings,
renderer text, and frontend tags are not evidence authority.

## MoveReviewPlayerSurface

The player surface is part of the trust boundary:

- `decisionComparison` is the verdict layer.
- `summaryRows` are the main "why this mattered" layer.
- `advancedRows` are the plan/follow-up layer.
- `refSans` plus `refs` drive replay, eval display, and board synchronization.
- `probeRows` and `authorRows` are follow-up checks, unresolved questions, or
  support details.

Choosing the wrong surface tier is a behavior bug. A resolved main reason should
not be hidden only in `authorRows`. A support-only fact should not be promoted
to `summaryRows` if it would read as a public conclusion.

The frontend may use typed fields for board cues, replay, and layout. It must
not infer attack, plan, timing, forcedness, compensation, conversion, draw/win,
or opening/endgame technique from label or prose text.

## Planner Owners

Planner owners are mechanism labels, not prose templates. They should be
admitted only when the matching producer evidence exists. Current broad families
include:

- concrete tactical ownership;
- forcing defense or only-move defense;
- line consequence;
- alternative comparison;
- timing / why-now;
- plan race or plan support;
- move-local or position-local delta;
- opening context or opening-to-middlegame support;
- endgame transition or conversion/draw support;
- exact factual fallback.

When a family is uncertain, weaken the claim family before weakening the
evidence standard. For example, a line can support pressure without proving a
tactical motif, and an opening label can supply context without proving a
current plan.

## Rendering

Rendering consumes admitted payloads. It may:

- escape and format text;
- arrange scene hierarchy;
- show board and replay cues from `refs`;
- expose exact support details;
- stay silent when a payload is not admitted.

Rendering must not:

- parse prose to recover chess meaning;
- upgrade support rows into conclusions;
- create fallback attack/plan/timing/alternative prose;
- dedupe or reorder checked SAN lines in a way that changes the line meaning;
- use diagnostics as public chess proof.

## Diagnostics

Diagnostics should explain why a claim was admitted, weakened, demoted, or
closed. They are useful for development and replay review, but are not public
claim authority.

Add diagnostic fields only when they help trace a live evidence path. Avoid
phase, rollout, audit-trail, or temporary labels that do not describe a durable
runtime concept.
