# Commentary Pipeline SSoT

This document is the current runtime map for Chesstory commentary. It is a
boundary map, not a preservation order. Live code, typed models, call sites, and
runtime replay behavior remain the final authority.

## Runtime Path

MoveReview commentary flows through one path:

1. `NarrativeContext` and `MoveReviewRefs` carry board, move, PV, and replay
   context.
2. `DecisiveTruthContract` classifies evaluation risk and truth boundaries.
3. producers build typed evidence packets: tactical motif, forced-line truth,
   relation witness, defense, line consequence, timing, alternative comparison,
   plan support, pressure, opening, endgame, or factual fallback.
4. `QuestionPlannerInputsBuilder` gathers those packets without turning prose
   into authority. It may carry a filtered local-fact result from
   `CommentaryIdeaSurface`, but only for causal fact families; opening,
   castling, capture, and endgame basic descriptions remain fallback surfaces
   unless a separate typed owner exists.
5. `QuestionFirstCommentaryPlanner` selects a question and planner owner from
   typed evidence.
6. `MoveReviewCausalClaim` builds the CausalFrame and typed evidence list.
7. `MoveReviewLocalFact` admits a local fact family and authority.
8. `MoveReviewCompressionPolicy` renders only the surface allowed by the
   CausalFrame/local fact contract. If planner selection does not render but a
   basic idea descriptor carries an eligible causal local fact, the policy first
   re-admits that typed fact through CausalFrame; direct basic rendering is only
   the non-causal or rejected fallback lane.
9. API/front-end payloads expose the selected surface and diagnostics.

There is no parallel MoveReview explanation path. Exact factual fallback is the
fail-closed renderer lane when no typed causal authority is admitted.

## Authority Model

The pipeline separates five concepts.

`RiskGate`

- Examples: blunder, missed win, `blocksStrategicSupport`, severe
  counterfactual, bad tactical refutation.
- Role: suppress or demote unsafe positive explanation.
- Non-role: it cannot create tactical, line, timing, alternative, or defense
  authority by itself.

`CausalMechanism`

- Examples: concrete tactical motif, defensive necessity, line consequence,
  alternative role difference, timing window, compensation failure, strategic
  concession, factual fallback.
- Role: says what kind of chess reason the evidence supports.

`EvidenceAuthority`

- Examples: motif detector, PV-coupled line consequence, role-aware decision
  comparison, forced-defense witness, `only_move_defense`, timing witness,
  supported local packet, exact factual replay.
- Role: says where the claim permission came from.

`SurfacePermission`

- Examples: may say tactical, forced, only move, timing, alternative, checked
  line, what-if-not, or factual-only.
- Role: limits renderer language.

`SceneSummary`

- Examples: `concrete_tactical`, `forcing_defense`, `line_consequence`,
  `alternative_comparison`, `plan_clash`, `opening_relation`,
  `endgame_transition`, `transition_conversion`, `quiet_improvement`.
- Role: summarize selected mechanism for diagnostics.
- Non-role: scene summary must not be the original source of sentence
  authority.

## Planner Owners

The planner owner families are mechanism labels, not prose templates:

- `ConcreteTactical`: current-move-owned tactical proof or motif-backed threat.
- `ForcingDefense`: a typed defensive need, urgent threat, only-move window, or
  allowed prevented resource.
- `LineConsequence`: a played-move-owned PV/replay consequence.
- `AlternativeComparison`: a branch-scoped comparison between played move and
  alternative/best move.
- `DecisionTiming`: typed timing witness or admissible timing contrast.
- `PlanRace`: certified own plan, opponent pressure, and timing anchor.
- `MoveDelta`: move-owned state or plan-support change.
- `PositionProbe`, `OpeningRelation`, `EndgameTransition`: domain-specific
  owners with their own support gates.

`truth_contract` alone is not a planner owner. It may close unsafe surfaces, but
it may not invent a causal mechanism.
Only a certified only-move defensive proof may project the positive
`only_move_defense` source, and it may own played-move timing only when the
reviewed move matches the chosen best move. A missed benchmark move remains an
alternative/comparison fact, not a played-move `WhyNow` owner.

`pv_coupled_plan_support` can support a move-delta plan claim only when the
truth/risk gate does not block strategic support. When admitted, it is a
`plan_support` local fact with `pv_coupled_line` authority and
`certified_strategy_delta` producer, and its checked line must start with the
reviewed move, contain a meaningful checked continuation, and match at least one
plan-anchor SAN token from the plan evidence. A one-ply PV/ref that only
restates the reviewed move, or a checked line that does not hit the plan anchor,
is not enough to certify plan viability. Under a blocking truth gate, it must
not render positive plan-viability prose.
Candidate evidence must prefer a replay-validated reviewed-move continuation
from `MoveReviewPvLine.firstCoupled` over a raw first variation that only
contains the reviewed move. This keeps coverage tied to existing refs without
letting one-ply source labels certify plan viability.

Unmatched PV-coupled plan-support candidates may remain diagnostics, but they
must not become visible `plan_support` authority or a WhatChanged primary
surface. If surface-ready `LineConsequenceEvidence` exists, WhatChanged may use
that typed line-consequence producer instead of falling back to an opening or
generic move-delta shell.

Typed tactical relation witnesses from `MoveReviewExchangeAnalyzer` enter the
planner through the local-fact result carrier and become CausalFrame
`relation_witness` evidence carrying a `MoveReviewLocalFact` candidate. They may
create a `ConcreteTactical` owner only for attack/threat families, or for a
pressure relation whose cataloged surface is `TacticalRelation`, with a
played-first PV/replay binding. Line-geometry pressure such as x-ray, clearance,
battery, pin, skewer, or interference stays pressure/move-delta evidence unless
it separately targets the king as an attack. Player/support rows or raw relation
labels must not become causal authority.
The cataloged relation surface is preserved as a typed local-fact/CausalFrame
field; `relation_surface:*` guardrails are diagnostics and must not be parsed as
the planner authority.
Cataloged `MobilityRestriction` relation witnesses become pressure local facts,
and cataloged `MoveOrder` relation witnesses become timing local facts. These
two surfaces may suppress the more generic post-move target-pressure descriptor
for the same offensive target so that domination/trapped-mobility and
zwischenzug evidence is not reduced to a plain target fact. They must not hide
defensive only-move target truth. Tactical relation witnesses do not get this
generic preemption; tactical ownership still depends on attack/threat families
or a typed `TacticalRelation` pressure witness.
Cataloged draw-resource relation witnesses (`StalemateTrap` and
`PerpetualCheck`) become defense local facts only when the replay/PV starts
with the reviewed move and the analyzer proves a draw-stable resource. They may
enter the main builder from the engine top PV, validated MoveReview refs, or
validated root probe reply lines. A typed draw-resource relation outranks a
generic forcing-check line consequence for the same checked line, so the
surface remains a defensive resource claim rather than a broad forcing-sequence
claim. Terminal one-move draw-resource lines may use a played-move line proof
only after the typed draw-resource witness exists; ordinary line-proof
admission still requires the usual reply-coupled PV.

`ForcedLineTruth.detect` is a high-precision tactical producer, not just an
author-question seed. When it validates a trap or tactical pattern for the
reviewed move, it enters through the same local-fact result carrier as
`forced_line_truth` evidence with `pv_coupled_line` authority. Mate themes map
to attack facts, material-winning themes map to threat facts, and draw-resource
themes remain defensive facts.

The same boundary applies to the player payload surface. `PvCoupledOnly`
evaluated plans are not enough to create "Practical plan" summary rows or
practical objective/step rows. Those rows require a stronger surface authority,
such as structural-only evidence, promoted probe/transposition evidence, a
supported-local row, or a CausalFrame plan-support local fact that has matched
its plan anchor.
Move-local exact-slice packets reuse the same supported-local row builder as an
internal gate for strict fallback eligibility. The row builder supplies the
typed proof/authority/target check; CausalFrame still receives a local fact, not
raw row prose.
Local file-entry exact proofs are part of this packet path. A
`LocalFileEntryBind`/`HalfOpenFilePressure` proof may seed move-local anchors
and owner proof, but only the exact proof packet and supported-local admission
can reach CausalFrame; "File entry" row text remains surface output.

`StrategyPack` practical ideas may enter MoveReview prose only through
`CommentaryIdeaSurface.practicalPositionFacts`. That projection creates
`practical_position_support` descriptors carrying `certified_strategy_delta`
local facts for defense, pressure, or plan-support families. The idea must be
owned by the played side, have evidence refs, meet its readiness/confidence
gate, touch the reviewed move through a target square, focus file, route, or
move ref, and remain tied to a played-first checked PV. The raw practical row or
idea label is not a renderer authority.

## Line Consequence

Line consequence authority requires a typed `LineConsequenceEvidence` packet
whose line is FEN/replay validated and owned by the reviewed move or by an
admissible branch comparison. Current consequence kinds include exchange,
forcing-check, central-break/central-pawn, material transition, passed-pawn
creation, and promotion-race evidence.

Played-move line consequence does not depend on a broad risk gate. If the
packet is surface-ready, non-preview, and owned by the reviewed move, it can
enter the planner as `LineConsequence`; the renderer still needs the
CausalFrame/local fact admission before using checked-line consequence prose.
When the same checked line also has a typed draw-resource relation witness, the
draw-resource local fact takes precedence over generic forcing-check consequence
wording.

For a missed benchmark `OnlyMoveDefense`, the admissible branch comparison is
the verified best/benchmark move against the played move. Both branches need
role-scoped line evidence, and a concrete played branch needs a meaningful
checked evaluation gap; otherwise the row remains fallback or support-only. This
does not create played-move `WhyNow` or `ForcingDefense` ownership.

When admitted, the owner is `LineConsequence` or `AlternativeComparison`, and
the local fact family remains `line_consequence`. Played-move line consequence
surfaces use the `line_consequence` producer over `pv_coupled_line` authority;
they must not fall back to generic planner authority when the typed evidence is
present. Role-aware comparison uses `alternative_comparison` local fact
authority/producer while preserving engine-best and played branch roles in
guardrails and requiring a concrete SAN branch line. The internal comparison
carrier preserves the engine-best and played `LineConsequenceEvidence` packets
so admissibility and CausalFrame evidence refs come from typed branch data, not
from reparsing the comparative sentence. It is not tactical motif authority,
forced-reply authority, or timing authority.
The player decision strip consumes the same enriched comparison and the admitted
MoveReview local fact when a role-aware alternative comparison has already been
accepted. It falls back to the older digest plus surface-line path only when no
such admitted CausalFrame/local-fact authority is present.

## Concrete Tactics

Concrete tactical authority requires current-move ownership from a tactical
claim or a motif-backed threat/counterfactual packet. Broad tactical wording,
sacrifice wording, tactical truth mode, or `blocksStrategicSupport` is not
enough.

Move-local tactical motif admission currently covers fork, pin, skewer,
discovered attack, and trapped-piece evidence. The motif must be owned by the
reviewed move, carry concrete square anchors, and be tied to a checked PV. A
trapped-piece claim also requires the replayed post-move board to show the
played piece attacking the trapped target; stale motif text alone is not
authority.

Typed tactical relation witnesses may also own a basic causal tactical surface
before strict fallback when the relation projection has valid kind-specific
details, the relation catalog marks the kind as `TacticalRelation`, and the
replayed line starts with the reviewed move. Non-tactical relation witnesses
remain strict-fallback or support-only evidence; line-geometry pressure can
explain pressure but must not classify the scene as `ConcreteTactical` merely
because it came from a relation witness.

Relation witnesses are exported as typed local-fact evidence through
`QuestionPlannerInputs`, not as player/support row text. The planner and
CausalFrame consume the relation candidate, family, anchors, line binding,
evidence refs, and typed relation surface; the relation sentence is only the
surface realization of that typed packet.
Mobility-restriction relation witnesses remain pressure evidence, not broad
plan or evaluation truth. Move-order relation witnesses remain timing evidence,
even when the move is an in-between check; the check target does not by itself
turn the packet into attack authority. A move-order relation may own a `WhyNow`
timing surface only when the typed local fact family is `timing`, the producer
is `relation_witness`, the relation surface is `MoveOrder`, and the packet
stays tied to the played-first PV/replay evidence.

When `ConcreteTactical` owns the surface, the rendered claim itself must carry
a concrete causal anchor such as a checked line, move, threat, tactic, reply,
or material consequence. A generic tactical label plus a cited line is not
enough for direct planner prose.

## Forced Reply And Timing

Explicit reply-loss and delayed-only-move contrasts enter CausalFrame as
`forced_reply` local fact authority, not as free-form contrast prose. A unique
engine-backed reply may open `surface_forced=true`; square-only anchors or
multiple defensive replies remain timing/defense evidence but do not authorize a
singular forced-reply surface.
When the reply evidence is only a UCI token and no board SAN is available, the
renderer may name the destination as a destination, not as if the square itself
were the move.

## Supported Local Packets

Supported-local claim packets may enter CausalFrame only after the claim
authority resolver admits their proof contract. When their family/source pair is
already supported by a mechanism-specific producer, use that producer; for
example stable move-local defense, pressure, plan-support, and line-consequence
packets use `certified_strategy_delta` rather than a generic promotion label.
When a packet contains a typed exact-slice proof, the proof-specific local fact
family is the promoted family. Coarser move-delta classes such as `NewAccess`
must not override exact proof families such as local file-entry pressure.
Central-break timing uses the same packet path: a
`CentralBreakTimingWitness` packet may promote to a `timing` local fact only
when the current plan source carries `central_break_timing` and the existing
supported-local admission accepts the exact proof. It does not let generic
plan-advance wording create timing authority.

## Rendering

Renderers consume CausalFrame/local fact contracts. They do not combine raw
planner labels into new chess meaning. If a required typed relation is absent,
MoveReview falls back to exact factual or bounded support-only wording.
Scoped takeaway rendering treats `certified_strategy_delta` as positional
pressure, counterplay restraint, plan support, or strategic consequence instead
of tactical prose, and it excludes raw strategy-pack text.
For relation witnesses, renderer wording is bounded by the typed local fact
family: mobility restrictions render as local pressure, move-order witnesses as
local timing, and diagnostic `relation_surface:*` strings cannot change that
family.
When an admitted typed local fact is rendered as the claim itself, the renderer
may treat that typed fact as embedded support. This does not let raw planner
text self-certify; the CausalFrame/local fact admission must already have
accepted the typed evidence packet.
If that admitted packet is a played-move causal owner that may use checked-line
surface, the existing reviewed-move short-line citation can remain as an
evidence hook. Timing-only `WhyNow` packets do not gain checked-line renderer
authority from this citation path.

## Diagnostics

Replay and QC outputs should expose mechanism-specific fields such as
`plannerConcreteTacticalSources`, `plannerLineConsequenceSources`, and
`plannerAlternativeComparisonSources`. Diagnostics should stay mechanism-specific
and must not collapse these families into a broad tactical bucket.
