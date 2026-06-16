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
   `Fact.KingActivity`, `Fact.Opposition`, or `Fact.RuleOfSquare` fields
   carried by canonical fact evidence only when the played move owns the typed
   square anchor; role-only king or pawn movement does not admit opposition or
   rule-of-square claims.
   Scoped checked-line takeaways may surface the admitted local-fact anchors,
   but that does not promote the fallback into planner
   authority. Oracle-only endgame outcome hints are not public result truth;
   without tablebase/eval/PV result evidence they leave public
   `theoreticalOutcomeHint` and transition task wording at `Unclear`.
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
   change claims. Conversion author-question seeds remain reply-line checks;
   classification and eval advantage do not by themselves assert that
   conversion, draw, or simplification has been achieved.
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
The same replay rule covers line-consequence candidates, branch hooks, and
player-payload checked-line rows: a packet may reuse an admitted `lineId` only
after the referenced variation replays from the current FEN and starts with the
reviewed move. `LineConsequenceEvaluator` may prefer a concrete played-first
packet over a preview-only first ref only under an existing truth admission and
still emits typed `LineConsequenceEvidence`, not planner text. Capture-started
multi-capture lines classify as exchange consequence before incidental pawn
structure movement.

Unmatched PV-coupled plan-support candidates may remain diagnostics, but they
must not become visible `plan_support` authority or a WhatChanged primary
surface. If surface-ready `LineConsequenceEvidence` exists, WhatChanged may use
that typed line-consequence producer instead of an opening or generic
move-delta shell.

Typed tactical relation witnesses from `MoveReviewExchangeAnalyzer` enter
through the local-fact result carrier. Their cataloged surface stays typed data:
attack/threat families and `TacticalRelation` pressure may own concrete tactics
with played-first replay binding; mobility restriction stays pressure, move
order stays timing, and draw-resource surfaces stay defense. Diagnostic
guardrails, support rows, raw relation labels, and line-geometry words do not
create causal authority by themselves.

Board-backed target, defense, check, capture, and forced-line facts remain
narrow local facts. Static target pressure, current-move check, target defense,
fork-entry defense, capture fallback, draw-resource defense, and
`forced_line_truth` may surface only from their typed producer payloads, board
anchors, evidence refs, and played-first PV/replay binding. They are not generic
tactical, pressure, or plan prose.

The same boundary applies to the player payload surface. `PvCoupledOnly`
evaluated plans are not enough to create player-facing plan rows, and
structural-only evaluated plans may surface only as weak `Structure support`
or through typed target/route/support anchors. Raw hypothesis preconditions or
execution steps do not create practical objective/step rows. Stronger plan
rows require promoted probe/transposition evidence, a supported-local row, or
a CausalFrame plan-support local fact that has matched its plan anchor.
Practical support rows may render existing `PracticalInfo` bias factors, but
`PracticalInfo.verdict` prose is not reopened as plan, pressure, or
counterplay authority.

Move-local exact-slice packets reuse the supported-local row builder only as a
proof gate. CausalFrame receives a typed local fact carrying proof family,
anchors, evidence refs, and checked-line binding; row labels, diagnostic refs,
and renderer prose remain output. Durable structural claims such as file entry,
outpost occupation, target fixation, IQP inducement, color-complex pressure,
central-break timing, counterplay clamp, or bad-piece liquidation may name their
object only when the producer proves the reviewed-move ownership on the
after-board and, when persistence matters, the played-first checked PV keeps the
object and pressure/resource relation alive. Transient, pre-existing, or
unpressured shapes stay support-only or diagnostic.
Frontend MoveReview coach surface consumes only typed player-surface fields.
Section labels may improve player hierarchy, but row label/text/diagnostics do
not create chess families. Move chips consume `refSans`/resolved refs for
displayed SAN while preserving order and repeated SAN tokens. A chip may carry
the exact preview board payload only when it is derived from the same resolved
ref; authoring row labels are not move identity authority. The interactive
review player derives Verdict/Why/Plan/Try/Remember scenes from that same
player surface and resolved-ref index; scene board payloads, target squares, and
chapter labels are UI state for board synchronization, not additional claim
authority.
Until the backend emits native review scenes, the frontend scene contract is a
derived UI model. A future backend `ReviewScene` payload should provide stable
scene key/order, title/kicker/body plus optional detail groups, active
`refId`/line refs or SAN line, exact board payload derived from a resolved ref,
focus square/target anchors, and a fallback reason when a scene cannot bind to a
board. These fields are routing and synchronization data only; every chess claim
inside them must still come from the typed player surface or the resolved ref
that supplies the board.
Opening-goal outpost context does not create a separate player-facing opening
outpost family. Its `Achieved` status requires post-move board proof of the
pawn-supported e4 outpost square; if the durable outpost proof succeeds, it
reuses the `Knight outpost` row and typed target.
Opening-goal break context likewise renders through the checked played pawn
route, not the catalog/opening-goal label.
Opening precedent and relation text is reference-game context; branch labels and
sample-line mechanisms do not become current-position plan, compensation, or
pressure authority.
If a reviewed central pawn move is itself a board-backed
`CentralBreakTimingWitness` and a same-destination `neutralize_key_break`
candidate also appears, the central-break packet owns the move unless an
explicit break-prevention rival is carried by the runtime anchors.

`StrategyPack` practical ideas may enter MoveReview prose only through
`CommentaryIdeaSurface.practicalPositionFacts`. That projection creates typed
`practical_position_support` facts with `certified_strategy_delta` evidence for
defense, pressure, or plan-support families. The idea must be owned by the
played side, carry evidence refs, meet readiness/confidence gates, touch the
reviewed move through a typed focus/route/target/move ref, and remain tied to a
played-first checked PV. Public practical rows may consume only producer-supplied
anchors such as line file, pawn-break file, space-gain file, target-fixing
square, or attack-lane square; broad strategy shells and row text are not
authority. Central practical challenge and bad-piece liquidation use their
specific typed proof packets, not labels. Quiet support pressure uses
`MoveDelta.pv_delta` target anchors, not `NarrativeSignalDigest.practicalVerdict`
text.
Strategic feature extraction does not copy endgame oracle pattern or technique
labels into `conceptSummary` or candidate immediate-intent text; those labels
remain `EndgameInfo` context until a typed endgame local fact or replay-backed
transition claim admits them. Outline motif collection likewise does not convert
primary pattern names or raw `EndgameInfo` flags into standalone canonical motif
claims, and the MoveReview strategic ledger does not use those pattern or
transition strings as structured motif tokens or carry-over emitters. Lexicon
endgame-pattern prefix templates and positional-tag motif promotion for
rook-behind-passer / king-cut-off labels are not public authority paths.
Endgame continuity/transition prose renders board-structure anchors rather than
the raw theorem or oracle pattern id. Player-surface `Endgame cue` rows are
support-only context and do not carry `practical_plan` authority.
The MoveReview strategic ledger also keeps compensation and conversion stages
separate: invested-material or compensation summaries can select a compensation
motif, but `Convert` needs a conversion trigger or conversion-family
plan-fruition signal.
Route and directional target endpoints are not king-attack lanes by proximity
alone. `KingAttackEvidenceProducer` may emit `source:route_attack_lane` or
`source:directional_attack_lane` only when `StrategicIdeaEvidenceSupport` proves
that the endpoint piece attacks the enemy king on the board and carries
`attack_lane_board_attack`; otherwise the route/target remains structural or
diagnostic support.

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
