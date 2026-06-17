# Commentary Truth Boundary

This document states what may become public chess truth in MoveReview
commentary. It intentionally avoids slice-by-slice implementation rules; live
producers and typed models own those details.

## Truth Contract

`DecisiveTruthContract` may prove broad move-quality and risk facts:

- move quality class;
- verified best move and eval risk;
- whether broad strategic support should be blocked;
- whether the position needs safer, weaker, or factual wording.

Truth contract alone cannot prove:

- a tactical motif;
- a forced reply;
- a line consequence;
- a role-aware alternative comparison;
- a timing window;
- a plan or strategic concession;
- opening/endgame technique;
- conversion, draw, or win result;
- renderer or frontend wording beyond safe risk/factual boundaries.

Risk gates close doors. They do not create positive chess reasons.

## Public Claim Requirements

A public chess claim needs all of the following:

- claim family: the exact kind of chess idea being asserted;
- current-position anchor: board state, move order, piece placement, pawn
  structure, legal line, or exact square/piece role;
- evidence authority: board/PV/eval/probe/tablebase/analyzer-backed typed
  evidence;
- surface strength: public conclusion, weak claim, support-only fact,
  diagnostic, or silence;
- source path: producer/local fact/CausalFrame/planner carrier path that can be
  traced without using prose as evidence.

If any part is missing, weaken the claim or keep it support-only. Do not repair
missing evidence by adding renderer gates, prompt hints, or frontend wording
rules alone.

## Claim Families

Families must not collapse into each other:

- tactical ownership needs current-move tactical proof, board geometry, or a
  typed motif/threat packet with checked-line support;
- defense and forcedness need typed defensive-resource or reply evidence;
- timing needs a typed why-now or move-order witness;
- alternative comparison needs branch roles and checked comparison evidence;
- line consequence needs replay/PV-bound consequence evidence;
- plan support needs a current-position mechanism, not only a plan label;
- opening and endgame names are context until current-position evidence proves
  the claimed idea;
- objective eval and practical difficulty are separate claims.

These bullets describe standards, not a complete list of producers. Prefer
extending an existing producer/evidence path over adding a parallel authority
object.

## Opening And Endgame

Opening catalog, route, and name lookup data may provide context, candidate
ideas, and route/access hints. They do not by themselves prove a current attack,
outpost, plan, compensation, or timing claim.

Endgame pattern/oracle data may provide typed anchors such as king squares,
passer squares, rook activity anchors, pawn-race facts, or tablebase/eval/PV
result support. Technique or result claims such as conversion, draw, or win
require current-position evidence at the appropriate strength.

## Fallback Truth

Fallback may state exact factual anchors that are already typed and owned by the
reviewed position. It may also say that a stronger reason is unavailable.

Fallback must not convert generic labels, row text, diagnostic strings, or
source prose into causal explanation.

## Surface Signoff

Before a claim is public, check:

- Is the family correct?
- Is the evidence current-position evidence?
- Is the surface tier correct for the evidence strength?
- Is the frontend receiving the row in the right field?
- Would the sentence still be valid if all labels and prose were removed and
  only typed evidence remained?

If the answer is no, fix the producer/evidence path or lower the claim.
