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
   `CommentaryIdeaSurface`, but only for admitted local-fact families; castling,
   capture, and endgame basic descriptions remain fallback surfaces unless a
   separate typed owner exists. Endgame fallback may render typed
   `Fact.KingActivity` or `Fact.Opposition` fields carried by canonical fact
   evidence, and scoped checked-line takeaways may surface the admitted
   local-fact anchors, but that does not promote the fallback into planner
   authority.
   Castling fallback may render after-FEN castling side, king square, rook
   square, and king-safety feature refs; scoped checked-line takeaways may use
   those anchors. It remains `castling_safety` fallback, not a broader opening
   or tactical owner.
   Opening-goal producers may use post-move board evidence plus played-first
   legal PV witnesses for activation or comparable development. Role-specific
   opening triggers must validate the moved piece on the post-move board. A
   strict PV-coupled `opening_goal` fact may enter CausalFrame as opening-goal
   authority, but not as plan, pressure, timing, or tactical authority.
   Author questions may open neutral question slots from candidate/PV signals,
   but candidate `downstreamTactic` strings and motif-derived labels are not
   converted into tactical names, line-consequence prose, timing, or positional
   change claims.
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
  comparison, forced-defense witness,
  `only_move_defense`, timing witness,
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
When that timing owner also has an admitted supported-local move-local packet, a
board-checked certified position probe, or an admitted `CertifiedStrategyDelta`
local fact, the planner may use that local reason while keeping
`only_move_defense` as the timing authority.
For urgent-threat ownership, `ThreatAnalyzer` detail is preserved through
`ThreatRow` into the planner: target pieces and verified attack squares may
shape bounded threat-class text, while motif names remain diagnostic/bounded and
do not become standalone tactical prose. Public fork, pin, skewer,
discovered-attack, deflection, decoy, overload, interference, or zwischenzug
labels need a separate typed tactical relation/local fact.
In MoveReview, an urgent-threat owner also needs the analyzer `bestDefense` to
match the reviewed move; otherwise the threat row stays support-only or
diagnostic.

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
The same selection rule applies to MoveReview line-consequence candidates:
when a preview-only first ref would otherwise hide a replay-validated
reviewed-move continuation, `LineConsequenceEvaluator` may choose the concrete
played-first packet, but only under an explicit truth admission: tactical or
strategic-support-blocked truth, or a best quiet-technical/no-clear-plan move
whose consequence packet is exchange or material-transition evidence. It still
enters only as typed `LineConsequenceEvidence`.
When a line-consequence CausalFrame needs a branch evidence hook, the hook must
prefer the admitted packet's replay-validated `lineId` over planner/author
branch text.
The same packet-line binding is reused by MoveReview player payload carriers:
fallback checked-line rows and strategic-ledger engine-path rows may prefer the
admitted surface-ready `LineConsequenceEvidence.lineId` only after the matching
ref variation replays from the current FEN and starts with the reviewed move.
For capture-started reviewed lines, a replayed multi-capture sequence classifies
as exchange consequence before incidental central-pawn movement.
Immediate opponent target-pressure consequence is a reviewed-move-only packet:
after a replayed quiet reviewed move, the opponent's next quiet reply must add a
new static `FactExtractor` target/hanging attack by the reply destination on a
non-king target near the reviewed side's king. It needs bad non-best truth, is
not a generic `surfaceCandidate` or role-aware comparison source, and ranks
behind concrete delayed pawn-capture consequence.

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
Post-move target pressure may include a knight move that directly attacks an
undefended pawn target only when the static board fact is current-move-owned,
or a pawn move that directly attacks a non-pawn piece target, or a bishop move
that directly attacks a knight target. The static board fact must be
current-move-owned, PV-coupled, and no played-move line-consequence surface may
already be available; prose may name the target square and role only after the
post-move board confirms that enemy piece is still on the target. Scoped
checked-line takeaways may reuse only the admitted `target_square`/`target_role`
anchors, not generic `fact_square` refs. Bishop-knight target pressure also
stays behind certified practical-position support.
A non-capturing reviewed move that gives check may enter as a bounded
`tactical_motif` threat fact only when the current-move `Motif.Check` is
PV-coupled and no stronger played-move line-consequence, relation witness, or
target pressure/defense fact already owns the explanation.
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

Board-backed non-pawn moves that add the played piece as a defender to an
already attacked own non-king target may enter as `target_defense` defense
local facts only with post-move static board proof and played-first PV coupling.
Pawn-chain reinforcement stays with the more specific line-consequence,
opening, or central-challenge producers.
Board-backed non-pawn, non-capture moves may enter as `fork_entry_defense` only
when the played destination newly defends an enemy knight entry square that
would fork the king and a rook or queen. This is post-move static defense
evidence plus played-first PV coupling, not generic threat prose.
Reviewed captures that would otherwise be an unverified target-pressure surface
may fall back to a `capture_sequence` local fact. That fact carries only
captured square/role anchors, checked-line scope, and a replayed
`followup_queen_trade_square` anchor for adjacent legal checked-line queen
capture/recapture pairs unless a stronger line-consequence or
alternative-comparison owner is available.

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
Outpost-occupation exact proofs follow the same packet path. For MoveReview
exact outpost wording, the reviewed knight move must be replayed onto the
destination, the post-move board must show friendly pawn support, no enemy pawn
attack, no enemy minor-piece attack on the square, and the checked top-PV
window must keep that knight on the square. Existing outpost facts or practical
outpost rows may support search, but they do not by themselves authorize exact
outpost wording. When admitted as a move-local certified-strategy fact, this
proof may add `outpost_square` and `outpost_piece_role` anchors and matching
evidence refs so the scoped renderer can name the knight outpost without
reparsing proof-family strings or player-row prose.
The player-payload practical `Knight outpost` fallback uses the same board/PV
durability checks for pawn support, enemy pawn/minor attacks, and immediate
top-PV persistence before it may publish outpost prose.
The opening-outpost fallback uses the same durability checks. Tag-only
`source:outpost_tag` or `source:strong_knight` strategic ideas stay backend
context and do not publish `Practical outpost` advanced rows; route/directional
outpost cues may surface only as non-durable plan-recognition support.
Exact target-fixation support may name a target only when the exact proof square
is board-proved after the reviewed move as an enemy pawn target attacked by the
moved piece. Proof labels, diagnostic `fixed_target:*` refs, or row text are
not enough; without board target proof the surface must remain generic. When
the named target is structural, the producer reuses the existing
`WeaknessTargetProfile`/`PawnStructureTargets` target profile and requires the
played-first checked PV horizon to keep that pawn target with pressure-side
attack still present before it may prefer the pawn target over a broader route
or open-file label. Move-local exact target payloads preserve that proof with
`target_persistent_after_line:<square>` and
`target_attacked_after_line:<square>` evidence refs.
If the fixed-target row names a pawn defender, that defender must be read from
the after-board as a same-color pawn attacking the target pawn.
IQP-inducement exact proofs require the newly isolated central pawn target to
persist at the checked PV horizon and be attacked there by the pressure side.
The proof packet must carry `target_pressure:<square>` along with the isolation
transition terms, and its `lineMoves` are the replayed horizon continuation used
for that persistence/pressure proof. Prefix-only isolated-pawn creation that is
exchanged, moved away, or left unpressured inside the checked PV is rejected
before row or CausalFrame admission.
Color-complex position probes remain position-local support unless MoveReview
can prove played-move ownership. A `ColorComplexSqueeze` packet may become a
`supported_local_position_probe` local fact only when the reviewed move places
the proof bishop/knight on the proof square and the after-FEN board confirms
that piece attacks the proof target square. The checked played-first PV window
must then keep that same minor piece on the proof square attacking the target;
if the line captures, trades, or moves the proof piece, the claim is withheld.
The producer checks the reviewed move's post-move destination first; pre-move
minor-piece attackers remain backend diagnostic context only, so MoveReview
withholds the player support row and cannot authorize a played-move pressure
claim.
Counterplay-axis suppression uses existing break-clamp materialization, but a
pawn capture whose destination is the reviewed move's origin square is not a
structural break clamp. That case is a capture threat on the moved piece and
must not produce `neutralize_key_break` authority or a `Counterplay break`
player row.

`StrategyPack` practical ideas may enter MoveReview prose only through
`CommentaryIdeaSurface.practicalPositionFacts`. That projection creates
`practical_position_support` descriptors carrying `certified_strategy_delta`
local facts for defense, pressure, or plan-support families. The idea must be
owned by the played side, have evidence refs, meet its readiness/confidence
gate, touch the reviewed move through a target square, focus file, route, or
move ref, and remain tied to a played-first checked PV. The raw practical row or
idea label is not a renderer authority.
Public practical attack-lane rows require the selected `KingAttackBuildUp` idea
to carry one valid focus square. Route or directional target-map ideas with
ambiguous focus squares stay hidden instead of falling through to broader attack
cue text.
Generic king-ring, weak-back-rank, and compensation king-window strategy shells
remain diagnostic support unless another concrete attacking mechanism is present
for the public row, such as a route, battery, check, threat, central exposure, or
named flank hook.
For `LineOccupation` practical ideas, this projection may add `line_file`,
`line_file_status`, and optional `line_target` anchors only when a rook or queen
lands on the certified focus/anchor file and the strategy evidence carries an
open or semi-open file ref for that file. These anchors come from the producer's
typed strategy evidence and focus fields, not from row labels or renderer prose.
For `PawnBreak` practical ideas, it may add `pawn_break_file` and optional
`pawn_break_contested_file` anchors only when the reviewed move is a non-capture
pawn advance on that file and the strategy evidence carries both a break-ready
source and `break_file_*` proof for the played file.
For `SpaceGainOrRestriction` practical ideas, it may add `space_gain_file`,
`space_gain_side`, and `space_gain_pawn` anchors only when pawn-chain-space
evidence is present, the reviewed move is a non-capture rook-pawn advance on a
strategy focus file, and the after-FEN board confirms the moved pawn on its
destination square.
For `TargetFixing` practical ideas, it may add `target_fixing_square`,
`target_fixing_target_kind`, `target_fixing_attacker_square`, and
`target_fixing_attacker_role` anchors only when the after-FEN board confirms the
reviewed bishop, rook, or queen move attacks a strategy target square carried by
enemy-weak-square, directional-target, or target-fixing evidence. Without that
board attack proof, the fact remains bounded generic pressure.
For `KingAttackBuildUp` practical ideas, it may add `attack_lane_square`,
`attack_lane_axis`, `attack_lane_attacker_square`, and
`attack_lane_attacker_role` anchors only when directional-attack-lane evidence
is present and the after-FEN board confirms the reviewed bishop, rook, or queen
move attacks that non-origin strategy lane square. Without that board attack
proof, the fact remains bounded generic pressure.
Played-first rook-pawn state deltas may use the same descriptor only when board
state before/after the reviewed move proves hook creation or rook-pawn march;
the practical row text is still renderer output, not authority.
If no stronger descriptor survives, a played central prep/challenge pawn move
may use `CentralBreakTimingWitness.practical` as a `line_consequence` local
fact with `pv_coupled_line` authority. This is central-challenge evidence only,
not central-break timing authority, and it stays behind certified strategy,
relation, and replayed line-consequence descriptors.
Move-local `BadPieceLiquidation` exact-slice proof may also enter as a strict
`line_consequence` local fact when the existing supported-local row proves the
bad piece and exchange square. The rendered reason comes from that typed exact
proof, not from a raw practical label.

## Line Consequence

Line consequence authority requires a typed `LineConsequenceEvidence` packet
whose line is FEN/replay validated and owned by the reviewed move or by an
admissible branch comparison. Current consequence kinds include exchange,
forcing-check, central-break/central-pawn, material transition, delayed
pawn-capture, immediate opponent pawn-capture, origin-square clearance,
passed-pawn creation, and promotion-race evidence.
Exchange-sequence structure wording may use only `LineStructureDetail` carried
by that packet: persistent replay-window board diffs for local pawn
moves/captures, such as IQP, isolated/backward/doubled pawns, or open/semi-open
files. Without that typed detail, the surface must not claim a changed pawn
structure. Generic isolated-pawn details surface as isolated pawns, not targets;
target wording belongs to separate pressure evidence or the exact IQP
persistence/pressure proof.
Delayed pawn-capture evidence is intentionally narrow: the reviewed move must
be the replayed first move, be a non-capturing bishop move, and the checked PV
must later show an opponent pawn capture.
Immediate opponent pawn-capture evidence is narrower still: the reviewed move
must be a non-capturing played-first move, the opponent's next replayed reply
must capture a pawn, and MoveReview admission requires a bad non-best truth
contract so best-move gambit or compensation lines do not get re-owned by the
pawn-capture surface.
Played-move target-pressure evidence is reviewed-move-only: a replayed
non-capturing queen or rook move must directly attack an advanced enemy pawn,
the same moved piece must continue later in the checked PV, and MoveReview
admission requires non-bad truth that does not block strategic support. It is
not a generic surface or role-comparison source.
Origin-square clearance evidence is also replay-bound: the reviewed move must
be the played-first non-capture by a non-pawn, non-king piece, and the checked
PV must later show a different same-side non-pawn, non-king piece making the
first non-capturing use of that vacated origin square. Same-piece returns and
capture/exchange lines do not certify clearance.

Played-move line consequence does not depend on a broad risk gate. If the
packet is surface-ready, non-preview, and owned by the reviewed move, it can
enter the planner as `LineConsequence`; the renderer still needs the
CausalFrame/local fact admission before using checked-line consequence prose.
If the basic lane already admitted a strict PV-coupled `line_consequence` local
fact, the CausalFrame bridge may preserve that typed authority before direct
basic rendering.
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
Acceptable style-choice or narrow-choice tactical rows may use the same carrier
only when the verified-best branch has a concrete line-consequence packet, the
played branch is preview-only, and the checked branch gap is material. When
branch UCI evidence is available, move identity is checked by UCI before SAN so
same-SAN opposite-side moves do not erase the contrast.
`MinorPieceReroute` is produced only for role-aware comparison from replayed
refs: a non-capturing knight or bishop on the engine-best branch must continue
with the same piece soon in the checked branch, the played branch must remain
preview-only, and the checked gap must be material. It is carried as
`alternative_comparison`, not as a generic played-move line-consequence surface.
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
authority. The admitted local fact may carry motif-specific square/role anchors
such as fork targets, pinned/behind pieces, skewer front/back pieces, revealed
attackers, trapped pieces, or checked king squares; base prose and scoped
checked-line takeaways may surface only those typed anchors.
Skewer admission is stricter than square-touching PV support: the post-move
board must still contain the attacker/front/back geometry, the PV reply must
move the front piece away without capturing the attacker, and the checked PV
must show the attacker winning the back target. A pawn as the back target, or a
reviewed move that already captured an equal-or-higher-value piece than the
back target, cannot become primary skewer authority; the existing capture
sequence producer should own the local fact when direct capture evidence is the
real move-local tactic.
Fork admission also requires more than a motif label plus a legal reply. The
post-move board must still show the attacker hitting the declared target
squares, and the target set must contain at least one non-pawn material target
or the checked PV must show the attacker capturing a non-king fork target after
the forced response. A king-plus-pawn check/fork shape remains closed as fork
authority unless that material follow-up is proved.
Current-move check facts remain bounded tactical facts, but a check-only local
fact is support-only when a certified role-aware alternative comparison is
available; the decision comparison owns the scene and the check may only remain
as local checked-line support.

Typed tactical relation witnesses may also own a basic causal tactical surface
before strict fallback when the relation projection has valid kind-specific
details, the relation catalog marks the kind as `TacticalRelation`, and the
replayed line starts with the reviewed move. For capture moves, this path is
limited to a typed `DiscoveredAttack` witness whose cleared square is the
reviewed move's origin. Non-tactical relation witnesses remain strict-fallback
or support-only evidence; line-geometry pressure can explain pressure but must
not classify the scene as `ConcreteTactical` merely because it came from a
relation witness.

Relation witnesses are exported as typed local-fact evidence through
`QuestionPlannerInputs`, not as player/support row text. The planner and
CausalFrame consume the relation candidate, family, anchors, line binding,
evidence refs, and typed relation surface; the relation sentence is only the
surface realization of that typed packet.
Tactical relation details may add relation-specific anchors, such as
discovered-attack attacker/target squares or overload defender/duty squares.
Scoped checked-line takeaways may surface those anchors, not relation prose.
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
enough for direct planner prose. If a strict PV-coupled typed local fact supplies
the concrete attack/threat/pressure target proof, CausalFrame narrows the claim
to that typed fact surface instead of releasing the broader tactical label.

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
If such a timing local fact is the surviving basic-lane fact, it is re-admitted
as a `WhyNow` CausalFrame timing witness before direct basic rendering.

## Rendering

Renderers consume CausalFrame/local fact contracts. They do not combine raw
planner labels into new chess meaning. If a required typed relation is absent,
MoveReview falls back to exact factual or bounded support-only wording.
Scoped takeaway rendering treats `certified_strategy_delta` as positional
pressure, counterplay restraint, plan support, or strategic consequence instead
of tactical prose, and it excludes raw strategy-pack text. For practical line
occupation, it may name only admitted `line_file`, `line_file_status`, and
`line_target` anchors. For practical pawn breaks, it may name only admitted
`pawn_break_file` anchors.
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
