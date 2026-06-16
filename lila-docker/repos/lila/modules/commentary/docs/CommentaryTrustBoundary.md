# Commentary Trust Boundary

This document defines what user-facing MoveReview prose may trust. It is kept
short so cleanup and refactoring work are not blocked by outdated rollout
details.

## Core Rule

Renderer prose may only express claims that have typed evidence authority.
Planner scene names, source strings, and truth-contract risk labels are
diagnostics unless a CausalFrame/local fact contract admits them.
Author-question rows may seed a question slot from candidate/PV signals, but
candidate `downstreamTactic` strings and motif-derived labels are not authority
for tactical names, line consequences, timing, or positional change prose.

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
  plus checked-PV ownership. Skewer prose additionally requires replayed
  post-move geometry, a PV reply that moves the front piece away without
  capturing the attacker, and a checked-PV capture of the back target by that
  attacker; pawn-back or direct-capture-outweighed skewers are not trusted
  tactical ownership. Fork prose additionally requires replayed post-move
  attack geometry and either a non-pawn material target in the fork set or a
  checked-PV capture of a non-king fork target by the attacker; king-plus-pawn
  check/fork shapes are not trusted fork ownership without that follow-up.
  Trapped-piece prose also needs replayed board
  confirmation that the played piece attacks the trapped target. Admitted
  `tactical_motif` facts may expose motif-specific square/role anchors to base
  prose and scoped checked-line takeaways; renderer code must not recover those
  targets from prose or generic evidence refs. A typed
  relation witness can own tactical prose before strict fallback only when its
  typed projection is valid, the typed local-fact relation surface from the
  relation catalog is `TacticalRelation`, and the replayed line is played-first.
  For capture moves, this remains closed except for a typed `DiscoveredAttack`
  witness whose cleared square is the reviewed move's origin.
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
  target fact. They do not suppress defensive only-move target truth. A strict
  PV-coupled attack/threat/pressure local fact may replace an unanchored broad
  tactical planner claim with its own typed target surface, but it does not make
  the broad label true;
- target pressure: post-move static targets are authority only for a reviewed
  knight move attacking an undefended pawn target, or a reviewed pawn move
  directly attacking a non-pawn piece target, or a reviewed bishop move directly
  attacking a knight target, and only when no played-move line-consequence
  surface already owns the explanation. Surface prose may name the target only
  after the post-move board confirms that the square still holds an enemy piece
  of the fact role. Scoped checked-line takeaways may consume admitted
  `target_square`/`target_role` anchors, not generic square refs. Bishop-knight
  pressure does not preempt certified practical-position support;
- current-move check: a non-capturing `Motif.Check` may be a bounded
  `tactical_motif` threat fact only with played-first PV coupling, and only
  when line consequence, relation witness, target pressure, or target defense
  does not already provide the stronger typed surface. If a certified
  role-aware alternative comparison is available, a check-only tactical local
  fact is support-only and cannot classify the scene as `ConcreteTactical`;
- forced/only move: only with `ForcingDefense` and defensive witness; certified
  only-move truth must enter as `only_move_defense`, not raw `truth_contract`;
  missed benchmark-only moves cannot own a timing claim for the played move.
  An only-move timing sentence may reuse an admitted supported-local move-local
  packet, a board-checked certified position probe, or an admitted
  `CertifiedStrategyDelta` local fact as its local reason, but the only-move
  authority still comes from `only_move_defense`.
  Explicit reply-loss and delayed-only-move evidence must enter through
  `forced_reply` local fact authority. A unique reply can authorize forced
  wording; UCI anchors without board-derived SAN may only be described by their
  destination, and square-only or non-unique replies stay bounded as
  timing/defense evidence. Threat detail may use only `ThreatRow` fields
  produced by the analyzer and preserved through the board-verified threat gate:
  target pieces and verified attack squares. `ThreatRow.motifs` may help choose
  bounded threat class wording such as capture or passed-pawn pressure, but
  they do not authorize public fork, pin, skewer, discovered-attack,
  deflection, decoy, overload, interference, or zwischenzug labels without a
  separate typed tactical relation/local fact. In MoveReview, urgent-threat
  ownership additionally requires the analyzer `bestDefense` to match the
  reviewed move;
- target defense: only a post-move static board fact where the reviewed
  non-pawn move becomes the defender of an already attacked own target may enter
  as `target_defense`; `fork_entry_defense` is limited to a board-proved newly
  defended enemy knight-entry square that would fork the king and a rook or
  queen. Row labels or generic defensive prose are not authority;
- timing/now: only with typed timing witness or admissible timing contrast;
- alternative/better: only with `AlternativeComparison` and branch role proof;
- checked line/result: only with `LineConsequence` or line-backed
  `AlternativeComparison`. A reviewed-move `LineConsequenceEvidence` packet can
  become planner authority whenever it is surface-ready, non-preview, and
  played-move-owned; this admission is packet-based, not a side effect of a
  broad risk gate. Exchange-sequence pawn-structure prose may name only
  persistent `LineStructureDetail` produced from replayed local pawn moves or
  pawn captures; if no such detail exists, the renderer may describe only the
  bounded exchange/material settlement. Generic isolated-pawn structure details
  are structure evidence, not target-pressure evidence; "target" wording needs
  a separate pressure or exact IQP proof. Immediate opponent target pressure is
  admitted only as reviewed-move line consequence under bad non-best truth: the
  replayed opponent reply must add a new non-king static target/hanging attack
  near the reviewed side's king. It is excluded from generic surface candidates
  and role-aware comparison so weaker pressure evidence cannot overwrite
  existing delayed-capture or comparison surfaces. Strict PV-coupled
  `line_consequence` local facts may be preserved through the CausalFrame bridge
  before direct basic rendering. Branch evidence hooks for admitted
  line-consequence claims must use the replay-validated packet `lineId` before
  planner/author branch text. Player payload checked-line rows and
  strategic-ledger engine-path rows may use that same packet `lineId` only when
  the packet is surface-ready and the ref variation replays from the current FEN
  with the reviewed move first; otherwise they fall back to the existing
  reviewed-ref or engine path. Bad-piece liquidation wording is trusted only
  from a move-local exact-slice proof that names the bad piece and exchange
  square, not from a practical label alone;
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
  plan causality. `LineOccupation` facts may name an open or semi-open file
  only from admitted `line_file`/`line_file_status` anchors, and may name a
  target square only from an admitted `line_target` anchor produced from the
  strategy focus fields. `PawnBreak` facts may name a file only from an admitted
  `pawn_break_file` anchor produced from a played-pawn advance, break-ready
  evidence, and matching `break_file_*` proof. `SpaceGainOrRestriction` facts
  may name a flank space gain only from admitted `space_gain_file`,
  `space_gain_side`, and `space_gain_pawn` anchors produced from pawn-chain-space
  evidence, a reviewed rook-pawn advance on a strategy focus file, and
  after-FEN board proof of the moved pawn. `TargetFixing` facts may name a
  weak or target square only from admitted `target_fixing_square` and
  `target_fixing_target_kind` anchors, and those anchors require after-FEN board
  proof that the reviewed bishop, rook, or queen move attacks that strategy
  target square. `KingAttackBuildUp` facts may name an attack lane only from
  admitted `attack_lane_square` and `attack_lane_axis` anchors, and those
  anchors require directional-attack-lane evidence plus after-FEN board proof
  that the reviewed bishop, rook, or queen move attacks a non-origin strategy
  lane square. Public practical attack-lane advanced rows likewise require a
  single valid focus square; an ambiguous route/target-map idea stays hidden
  instead of falling back to broader attack cues. Generic king-ring,
  weak-back-rank, and compensation king-window shells are diagnostic support;
  without a concrete route, battery, check, threat, central exposure, or other
  named attacking mechanism they do not publish public `Practical attack` rows.
  Route and directional endpoints become `source:route_attack_lane` or
  `source:directional_attack_lane` only when the producer proves that the
  endpoint piece attacks the enemy king on the board and carries
  `attack_lane_board_attack`; king proximity of a target candidate is not
  trusted input. When a reviewed move is itself a board-backed central break,
  same-destination `neutralize_key_break` support cannot relabel it as a public
  defensive-resource claim unless an explicit break-prevention rival survives in
  the runtime anchors.
  Played-first rook-pawn hook/march facts may enter here only from board state
  before/after the reviewed move, not from practical-row text;
- practical central challenge: only when `CentralBreakTimingWitness.practical`
  verifies the reviewed move as a central prep/challenge pawn move and a
  played-first checked PV exists. It may carry a `line_consequence` local fact
  for bounded central-challenge wording, but it is not central-break timing
  authority and must not preempt stronger certified strategy, relation, or
  replayed line-consequence evidence;
- factual-only: when no causal authority survives.
- bounded capture fallback: when no stronger causal owner survives, a
  PV-coupled `capture_sequence` local fact may render only the reviewed
  capture's carried square/role anchors, checked-line scope, and a replayed
  `followup_queen_trade_square` anchor when adjacent checked-line plies
  legally capture and recapture queens on that square. It must not be promoted
  into pressure, plan, or broad tactical authority.

## Local Fact Families

Typed local fact families are the trusted renderer input: `threat`, `defense`,
`line_consequence`, `timing`, `pressure`, `plan_support`, `opening_goal`,
`endgame`, and exact factual fallback. A family may only be emitted by an
authority that supports that family. For example, a line-consequence packet
cannot certify a fork, pin, or plan unless a separate tactical or plan authority
exists.

Filtered local-fact results from the basic idea surface may enter CausalFrame
only for admitted families and only with their producer, anchors, evidence refs,
guardrails, and line binding intact. Castling, capture, endgame, and opening
descriptors remain bounded fallback surfaces unless their typed facts provide
the canonical board fields they need. The fallback lane may bridge an unrended
causal local fact into CausalFrame, but it does not create a new chess reason.
Oracle-only endgame outcome hints are diagnostic/support material, not public
win/draw or conversion authority; public context, rows, and transition prose
must stay result-neutral without tablebase/eval/PV result evidence.

Supported-local and exact-slice packets are proof gates, not independent chess
mechanisms. After claim-authority admission they must resolve to the strongest
existing producer that supports the family/source pair. The renderer may consume
only admitted anchors and checked-line bindings; row labels, diagnostic refs,
basic prose, and practical-row text are not causal authority.

Durable structural claims require producer-owned board/PV proof. File entry,
outpost occupation, target fixation, IQP inducement, color-complex pressure,
central-break timing, counterplay clamps, and bad-piece liquidation may name an
object only when the typed proof shows reviewed-move ownership on the after-board
and, where persistence matters, the played-first checked PV keeps the object and
pressure/resource relation alive. Transient, pre-existing, or unpressured shapes
remain support-only or diagnostic.
For target fixation, a board-only witness without typed target focus is trusted
only for the Carlsbad c-pawn target profile; generic weak-pawn pressure needs a
typed idea, moveRef, or route hint before it can become a public claim.

Strategy-pack practical ideas follow the same rule. Only projected typed local
facts from `CommentaryIdeaSurface.practicalPositionFacts` are trusted, and only
their admitted anchors may appear in public wording. Broad idea labels, attack
shells, practical rows, and renderer text do not authorize line occupation,
pawn-break, space-gain, target-fixing, attack-lane, or central-challenge claims.

Relation witnesses carry their cataloged surface as typed data. That surface may
narrow the family: mobility restriction stays pressure, move order stays timing,
draw resource stays defense, and an in-between check does not become attack
prose unless separate attack/threat authority exists. Checked-line support is
still separately gated and does not widen a timing-only or support-only fact.

Displayed SAN refs preserve legal line order, including repeated SAN tokens.
Deduplicating a repeated capture, recapture, or castling token would change the
evidence line rather than merely cleaning prose.

Alternative comparison and the player decision strip follow the same
preservation requirement: role-aware comparison text needs admitted
engine-best/played `LineConsequenceEvidence` packets. Shallow branch labels,
comparative prose, and enriched carrier text do not certify local-fact evidence
refs or guardrails.

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
Acceptable style-choice or narrow-choice tactical rows may also use this path
only when typed branch evidence shows a concrete verified-best consequence
against a preview-only played branch with a real checked gap. UCI branch moves,
when present, define move identity before SAN. This remains alternative
comparison authority, not tactical motif ownership.
Minor-piece reroute comparison is limited to this role-aware path: the
engine-best branch must be replayed as `MinorPieceReroute` from a non-capturing
knight or bishop whose same piece continues soon in the checked branch, the
played branch must remain preview-only, and the checked gap must be material.
It does not create generic line-consequence, plan, timing, or tactical
authority.

## Line Consequence

Line consequence surface requires FEN/replay validation and a played-move or
branch-role binding. A played-move line consequence that owns the visible claim
must be emitted by the `line_consequence` producer over `pv_coupled_line`
authority. Preview-only, engine-only, or raw branch prose stays
support/diagnostic.
If the first reviewed ref is preview-only, a later replay-validated
reviewed-move proof line may supply that packet only through
`LineConsequenceEvaluator`; broad source labels or engine-only tails still
cannot create the surface.
For a best quiet-technical move, that reviewed proof may own only exchange or
material-transition consequence packets, and only when the truth contract says
the played move matches best with no clear-plan failure. Truth still does not
invent the consequence without the replayed packet.
Capture-started exchange consequence requires a replayed played-first line with
multiple captures; incidental central-pawn movement must not take ownership from
the played capture.
Delayed pawn-capture consequence is limited to a replayed played-first
non-capturing bishop move with a later opponent pawn capture; broader later
pawn captures stay preview/support unless another typed producer owns them.
Immediate opponent pawn-capture consequence is limited to a replayed
played-first non-capture where the opponent's next reply captures a pawn, and
it may own MoveReview prose only for bad non-best reviewed moves.
Played-move target-pressure consequence is limited to a replayed played-first
non-capture by a queen or rook that directly attacks an advanced enemy pawn,
with the same moved piece continuing later in the checked PV. It may own
MoveReview prose only under non-bad truth that does not block strategic
support, and it is excluded from generic surface candidates and role-aware
comparison.
Origin-square clearance consequence is limited to a replayed played-first
non-capture by a non-pawn, non-king piece, followed in the checked PV by the
first non-capturing use of that origin square by a different same-side
non-pawn, non-king piece. Same-piece returns, captures, and exchange lines stay
out of this clearance authority.

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
