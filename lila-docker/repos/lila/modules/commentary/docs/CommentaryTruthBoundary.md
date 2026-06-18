# Commentary Truth Boundary

This is a compact reference for what may become public chess truth in MoveReview
commentary. It is not a substitute for live producers, typed models, replay, or
corpus evidence.

## Truth Contract

`DecisiveTruthContract` may prove broad move-quality and risk facts:

- move quality class;
- verified best move and eval risk;
- whether broad support should be blocked;
- whether wording should be safer, weaker, factual, or silent.

Truth contract alone does not prove:

- tactical motif ownership;
- forced reply or only-move uniqueness;
- line consequence;
- role-aware alternative comparison;
- timing window;
- plan or strategic concession;
- opening/endgame technique;
- conversion, draw, or win result;
- renderer or frontend wording beyond safe risk/factual boundaries.

Risk gates close doors. They do not create positive chess reasons.

## Public Claim Requirements

A public chess claim should have:

- claim family: the exact kind of chess idea asserted;
- current-position anchor: board state, move order, piece placement, pawn
  structure, legal line, or exact square/piece role;
- evidence source: board/PV/eval/probe/tablebase/analyzer-backed typed evidence;
- surface tier: conclusion, weak claim, support-only, diagnostic, or silence;
- source path: producer/local fact/CausalFrame/planner carrier path that can be
  traced without using prose as evidence.

If any part is missing, weaken the claim or keep it support-only. Do not repair
missing evidence only by adding renderer gates, prompt hints, or frontend copy.

## Family Separation

Families should not collapse into each other:

- tactical ownership needs current-move tactical proof, board geometry, or a
  typed motif/threat packet with checked-line support;
- defense and forcedness need typed defensive-resource, reply, or alternative
  evidence;
- timing needs a typed why-now or move-order witness;
- alternative comparison needs branch roles and checked comparison evidence;
- line consequence needs replay/PV-bound consequence evidence;
- plan support needs a current-position mechanism, not only a plan label;
- opening and endgame names are context until current-position evidence proves
  the claimed idea;
- objective eval and practical difficulty are separate claims.

Specific motif rules belong in code and targeted tests. This file should keep
the standards compact enough that it does not become a second implementation.

## Fallback Truth

Fallback may state exact factual anchors already typed and owned by the reviewed
position. It may also say that a stronger reason is unavailable.

Fallback should not convert generic labels, row text, diagnostic strings, source
names, or prose into causal explanation.

## Surface Check

Before a claim is public, ask:

- Is the family correct?
- Is the evidence current-position evidence?
- Is the surface tier correct for the evidence strength?
- Is the frontend receiving the row in the right field?
- Would the sentence still be valid if labels and prose were removed and only
  typed evidence remained?

If not, fix the producer/evidence path or lower the claim.
