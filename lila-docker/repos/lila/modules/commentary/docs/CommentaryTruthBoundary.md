# Commentary Truth Boundary

This document states what `DecisiveTruthContract` can and cannot prove for
MoveReview commentary.

## What Truth Contract Can Prove

`DecisiveTruthContract` may prove:

- move quality class, such as best, acceptable, inaccuracy, mistake, blunder,
  missed win, or investment class;
- reason family, such as tactical refutation, only-move defense, investment
  sacrifice, quiet technical move, or conversion;
- failure mode, such as tactical refutation, only-move failure, quiet
  positional collapse, speculative investment failed, or no clear plan;
- verified best move and evaluation risk;
- whether broad strategic support should be blocked.

## What Truth Contract Cannot Prove Alone

Truth contract alone cannot prove:

- a concrete tactical motif;
- a forced reply count or forced reply role;
- a line consequence;
- a role-aware alternative comparison;
- a timing window;
- a strategic plan or concession;
- renderer wording beyond safe risk/factual boundaries.

`blocksStrategicSupport` is a truth/risk gate, not a positive explanation
authority.

## Positive Authority Requirements

Positive causal prose requires a typed producer packet:

- tactical prose: current-move tactical ownership or motif-backed threat with
  concrete square anchors and checked-PV support. Discovered-attack and
  trapped-piece claims need the same ownership proof; trapped-piece claims also
  need replayed board confirmation that the played piece attacks the trapped
  target. A typed tactical relation witness may also authorize tactical prose
  when its projection is kind-valid, its local-fact relation surface is
  `TacticalRelation`, and it is tied to a played-first replayed line.
  `relation_surface:*` guardrails are diagnostics, not truth authority.
  Line-geometry pressure from a relation
  witness is pressure evidence, not tactical truth, unless the projection
  independently targets the king as an attack. `ForcedLineTruth.detect` may
  authorize tactical prose only when it validates the reviewed move and is
  admitted as a `forced_line_truth` local fact over a played-first
  `pv_coupled_line`;
  typed mobility-restriction and move-order relation witnesses may replace a
  generic target-pressure surface, but only by narrowing the claim to pressure
  or timing respectively. They do not authorize broad evaluation, plan, or
  attack prose by themselves;
- defensive prose: defensive witness, urgent threat, or only-move proof;
- played-move only-move timing: only when the truth contract also says the
  reviewed move matches the chosen best move;
- forced-reply prose: explicit reply-loss or delayed-only-move evidence must
  certify a `forced_reply` local fact. Non-unique or square-only replies may
  support timing/defense, but they must not become singular forced-reply prose.
  A UCI-only reply anchor may preserve the destination as evidence, but renderer
  prose must not describe that destination square as if it were a SAN move;
- line consequence prose: FEN/replay-validated `LineConsequenceEvidence`
  admitted as a `line_consequence` local fact from the `line_consequence`
  producer. The packet may own played-move consequence prose without a separate
  risk-gate trigger when it is surface-ready, non-preview, and played-first;
- alternative prose: branch-scoped role comparison admitted as
  `alternative_comparison` local fact authority with a concrete SAN branch
  line; for a missed benchmark only-move, this means verified best/benchmark
  branch versus played branch, not played-move timing authority;
- timing prose: timing witness or admissible timing contrast;
- plan/pressure prose: certified local packet that passes claim authority
  gates. PV-coupled plan support must surface as a `plan_support` fact from
  `certified_strategy_delta` with a played-first checked line and a meaningful
  continuation that matches a plan-anchor SAN token. A one-ply line that only
  names the reviewed move, or a line that does not hit the plan anchor, must not
  become plan-viability authority. The player payload follows this boundary:
  `PvCoupledOnly` evaluated plans cannot by themselves publish practical-plan
  rows or practical objective/step rows. Strategy-pack practical ideas may
  authorize only bounded defense, pressure, or plan-support prose after they are
  projected to a `practical_position_support` local fact with played-side
  ownership, evidence refs, readiness/confidence gating, a move-touching anchor,
  and a played-first checked PV.

If the typed packet is absent, the truth contract may still close unsafe
surfaces, but it must not fill the missing chess reason.

Filtered local-fact projection is evidence transport, not a prose shortcut.
When `CommentaryIdeaSurface` supplies a causal local fact to the planner, the
family, producer, anchors, line binding, and evidence refs are the authority.
The basic explanation sentence remains a renderer surface and must not promote
opening/castling/capture/endgame prose into causal claims by itself.
Raw strategy-pack or practical-row wording follows the same rule: it is not
truth authority unless the typed local fact projection supplies the family,
producer, anchors, line binding, and guardrails.
Move-local exact-slice packets can strengthen strict fallback eligibility only
through the existing supported-local row proof gate. The exact proof and
authority metadata may be reused; the row text itself remains renderer output.
For local file-entry pressure, truth authority is the
`LocalFileEntryBind` exact proof inside a `HalfOpenFilePressure` packet. The
exact proof family supplies the typed local fact family; support-row prose and
coarse move-delta labels cannot widen it.
For central-break timing, truth authority is the
`CentralBreakTimingWitness` exact proof inside a supported-local packet. It may
promote to a timing local fact only through the claim-authority admission tied
to `central_break_timing`; plan-advance labels and prose are not enough.
For relation witnesses, the typed relation surface supplies the family boundary:
mobility restriction is pressure and move order is timing. Diagnostic
`relation_surface:*` guardrails cannot widen or change that boundary.
Draw-resource relation surfaces are defense truth only when a typed relation
witness proves a draw-stable stalemate or perpetual-check resource on a
played-first replay/PV. Generic forcing-check consequence text is not
draw-resource truth; when both are available, the typed draw-resource relation
is the narrower authority.
Move-order timing may become a `WhyNow` claim only through the typed timing local
fact carried by a relation witness and played-first PV/replay binding. If the
typed fact sentence is the rendered claim, it counts as embedded support only
after CausalFrame/local fact admission; prose alone and guardrail strings remain
non-authoritative.
Reviewed-move short-line support can accompany embedded typed-fact claims only
when the admitted relation allows checked-line surface; it is not independent
truth authority for timing-only `WhyNow` claims.
Role-aware alternative comparison truth comes from the preserved engine-best
and played `LineConsequenceEvidence` packets carried by the comparison. The
comparative sentence and branch-role labels may render that contrast, but they
do not by themselves certify branch evidence, timing, forced-reply, or tactical
authority.
The player decision strip may reuse that role-aware truth only through the
enriched runtime comparison plus the admitted `alternative_comparison`
local fact. Rebuilding a shallow digest or reading branch prose is not enough.

## Scene Summary

Scene summaries are post-classification diagnostics. They are not truth
contracts. A `concrete_tactical` scene summarizes admitted concrete tactical
authority; a `line_consequence` scene summarizes line authority; an
`alternative_comparison` scene summarizes branch-comparison authority.

No broad tactical scene is part of the current truth boundary.

## Fallback Truth

Exact factual fallback may state:

- played move,
- destination square,
- capture/check basics when exact,
- short checked-line prefix,
- absence of admitted causal authority.

It may not state a cause, threat, plan, timing, or alternative unless the
corresponding typed evidence was admitted.
