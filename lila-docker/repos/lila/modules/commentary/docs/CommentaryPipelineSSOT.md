# Commentary Pipeline Reference

This is a compact runtime map for Chesstory commentary. It is not a source of
truth by itself. Live code, typed models, call sites, replay output, corpus rows,
and tests override this document when they disagree.

Use this file to orient a change, not to justify preserving an implementation
shape.

## Runtime Path

MoveReview commentary is intended to flow through one path:

`NarrativeContext / MoveReviewRefs -> producers and analyzers -> typed evidence
/ local facts / CausalFrame inputs -> planner and carrier -> MoveReviewPlayerSurface
-> renderer and frontend`

Avoid parallel explanation paths. If typed evidence does not survive this path,
the public surface should be factual, support-only, diagnostic, or silent.

## Evidence Roles

Keep these roles separate:

- `RiskGate`: suppresses or demotes risky explanation. It does not create a
  chess reason.
- `CausalMechanism`: the claim family, such as tactical ownership, defense,
  timing, line consequence, alternative comparison, plan support, opening
  context, endgame context, or factual fallback.
- `EvidenceSource`: board state, legal move/replay, PV/eval/probe/tablebase
  data, analyzer packet, admitted local fact, or CausalFrame input.
- `SurfaceTier`: public conclusion, weak claim, support-only detail,
  diagnostic, or silence.
- `SceneSummary`: grouping and diagnostics. It is not evidence.

Prose, row labels, opening/endgame names, diagnostics, source strings, renderer
text, and frontend tags are not evidence.

## Player Surface

`MoveReviewPlayerSurface` carries meaning by field:

- `decisionComparison`: verdict layer.
- `summaryRows`: main explanation layer.
- `advancedRows`: plan or follow-up layer.
- `refSans` and `refs`: replay/eval/board synchronization.
- `probeRows` and `authorRows`: support, checks, or unresolved questions.

Putting the right claim in the wrong field changes user-facing strength. Fix
field placement in the backend instead of compensating in renderer or frontend
copy.

## Planner And Builder Notes

Planner owners are mechanism labels, not prose templates. They should be chosen
from typed evidence, not from scene names or fallback text.

General priorities:

- prefer exact local facts, replay/PV-bound evidence, and verified branch
  comparisons over broad strategy-pack context;
- keep only-move, forcedness, tactical ownership, line consequence, plan
  support, opening, and endgame families distinct;
- keep support-only facts out of conclusion fields unless another typed path
  upgrades them;
- preserve checked-line order and SAN meaning when presenting replay support;
- keep author prompts and diagnostics out of player-facing conclusions unless a
  typed evidence path resolves them.

Detailed row-specific behavior belongs in code, fixtures, or replay reports, not
in this reference file.

## Renderer And Frontend

Rendering may format admitted payloads, arrange scenes, show board/replay cues,
and hide malformed or unsupported details.

Rendering should not infer chess meaning from prose, labels, diagnostics, source
names, CSS classes, or tags. It should not reorder checked lines or promote
support rows into conclusions.

## Diagnostics

Diagnostics are for tracing why a claim was admitted, weakened, demoted, or
closed. Add diagnostic fields only when they explain a live evidence path.

Avoid phase, rollout, audit-trail, or temporary labels unless they are durable
runtime concepts already present in code.
