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

When paired before/after scores are unavailable, move-quality truth is derived
from verified cp-loss, not from the absolute best-line evaluation.

## What Truth Contract Cannot Prove Alone

Truth contract alone cannot prove:

- a concrete tactical motif;
- a forced reply count or forced reply role;
- a line consequence;
- a role-aware alternative comparison;
- a timing window;
- a strategic plan or concession;
- an author-question tactical label or candidate `downstreamTactic` string;
- renderer wording beyond safe risk/factual boundaries.

`blocksStrategicSupport` is a truth/risk gate, not a positive explanation
authority.

## Positive Authority Requirements

Positive causal prose requires a typed producer packet:

- tactical prose: current-move tactical ownership or motif-backed threat with
  concrete square anchors and checked-PV support. Discovered-attack and
  trapped-piece claims need the same ownership proof. Skewer claims must prove
  the post-move attacker/front/back geometry, a PV reply that moves the front
  piece without capturing the attacker, and a checked-PV capture of the back
  target by that attacker; pawn-back or direct-capture-outweighed skewers do
  not supply tactical truth. Fork claims must prove post-move attack geometry
  and either a non-pawn material target or a checked-PV capture of a non-king
  fork target by the attacker; king-plus-pawn check/fork shapes do not supply
  fork truth without that material follow-up. Trapped-piece claims also need
  replayed board
  confirmation that the played piece attacks the trapped target. Admitted
  `tactical_motif` facts may carry motif-specific square/role
  anchors for base prose and scoped checked-line takeaways; renderer code must
  not derive those targets from prose or generic evidence-ref parsing. A typed
  tactical relation witness may also authorize tactical prose when its
  projection is kind-valid, its local-fact relation surface is
  `TacticalRelation`, and it is tied to a played-first replayed line.
  Capture moves need the same proof and are admitted only for a typed
  `DiscoveredAttack` whose cleared square is the reviewed move's origin.
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
  attack prose by themselves. A non-capturing current-move `Motif.Check` may
  authorize only a bounded `tactical_motif` threat fact with played-first PV
  coupling, and only when no stronger line-consequence, relation, target
  pressure, or target-defense fact owns the surface. A check-only tactical fact
  is support-only when a certified role-aware alternative comparison is
  available, so the comparison remains the scene owner. A strict PV-coupled typed
  local attack/threat/pressure fact can narrow an unanchored concrete-tactical
  planner claim to its own target surface, but cannot certify the broader
  tactical label;
- pressure prose: exact target-pressure local facts may name a target square and
  role only after the post-move board still contains that enemy piece. They may
  carry `target_square`/`target_role` anchors for scoped checked-line takeaways;
  renderer code must not infer the target by parsing generic `fact_square` refs.
  They may
  include a reviewed
  knight move attacking an undefended pawn target or a reviewed pawn move
  directly attacking a non-pawn piece target, or a reviewed bishop move directly
  attacking a knight target, but not when a stronger played-move
  line-consequence surface already owns the explanation. Bishop-knight pressure
  does not preempt certified practical-position support;
- defensive prose: defensive witness, urgent threat, only-move proof, or a
  board-backed `target_defense` local fact where the reviewed non-pawn move adds
  the defender to an already attacked own target and remains PV-coupled. An
  urgent-threat owner in MoveReview must match the reviewed move to the
  analyzer `bestDefense`. `ThreatRow.motifs` are not tactical truth authority:
  without a separate typed tactical relation/local fact, forcing-defense prose
  must stay at material/capture/passed-pawn/king-threat class wording instead of
  naming fork, pin, skewer, discovered attack, deflection, decoy, overload,
  interference, or zwischenzug. A `fork_entry_defense` fact must prove the
  newly defended knight-entry fork square from board state and PV coupling;
- played-move only-move timing: only when the truth contract also says the
  reviewed move matches the chosen best move. Its timing sentence may name an
  additional local idea only from an admitted supported-local move-local packet,
  a board-checked certified position probe used as support context, or an
  admitted `CertifiedStrategyDelta` local fact; the timing authority remains
  `only_move_defense`;
- forced-reply prose: explicit reply-loss or delayed-only-move evidence must
  certify a `forced_reply` local fact. Non-unique or square-only replies may
  support timing/defense, but they must not become singular forced-reply prose.
  A UCI-only reply anchor may preserve the destination as evidence, but renderer
  prose must not describe that destination square as if it were a SAN move;
- line consequence prose: FEN/replay-validated `LineConsequenceEvidence`
  admitted as a `line_consequence` local fact from the `line_consequence`
  producer. The packet may own played-move consequence prose without a separate
  risk-gate trigger when it is surface-ready, non-preview, and played-first.
  If the CausalFrame surfaces a branch evidence hook for that packet, the hook
  must use the packet's replay-validated line id before any planner/author
  branch text.
  MoveReview player payload support rows and strategic-ledger engine-path rows
  may reuse the same packet line only when the packet is surface-ready and the
  referenced variation replays from the current FEN with the reviewed move
  first; row labels or shallow ref order do not choose the line.
  Exchange-sequence pawn-structure claims require persistent replay-window
  `LineStructureDetail` from local pawn moves or pawn captures; generic
  exchange prose cannot infer a changed pawn structure. A generic isolated-pawn
  `LineStructureDetail` may name the isolated pawn, but it must not call that
  pawn a target unless a separate target-pressure or exact IQP proof supplies
  that authority.
  A best quiet-technical/no-clear-plan truth contract may admit only
  exchange/material-transition reviewed-branch packets; it does not prove a
  consequence without the replayed packet.
  Delayed pawn-capture prose additionally requires a replayed played-first
  non-capturing bishop move and a later opponent pawn capture in the checked
  PV.
  Immediate opponent pawn-capture prose additionally requires a replayed
  played-first non-capture, an opponent pawn capture on the next reply, and a
  bad non-best truth contract.
  Immediate opponent target-pressure prose additionally requires a replayed
  played-first non-capture, a quiet opponent reply that newly attacks a
  non-king static target/hanging piece near the reviewed side's king from the
  reply destination, and a bad non-best truth contract. This packet is
  reviewed-move-only: generic surface candidates and role-aware comparison do
  not consume it, and delayed pawn capture remains the stronger consequence.
  Played-move target-pressure prose additionally requires a replayed
  played-first non-capturing queen or rook move, a direct attack on an advanced
  enemy pawn by the moved piece, and a later checked-PV continuation by that
  same piece. It needs non-bad truth that does not block strategic support and
  cannot be consumed as generic surface or role-comparison authority.
  Origin-square clearance prose additionally requires a replayed played-first
  non-capture by a non-pawn, non-king piece and a later checked-PV move where a
  different same-side non-pawn, non-king piece is the first non-capturing user
  of the reviewed move's vacated origin square. Same-piece returns, captures,
  and exchange lines do not supply clearance truth.
  A practical central-challenge pawn move may use the same local-fact family
  only when `CentralBreakTimingWitness.practical` verifies a played challenge
  token and a checked PV starts with that move; this does not authorize
  central-break timing prose. A strict PV-coupled `line_consequence` local fact
  may also enter CausalFrame from the basic lane before direct basic rendering.
  `BadPieceLiquidation` wording needs the exact-slice proof of the bad piece
  and exchange square, carried as a supported-local line-consequence fact;
- capture fallback prose: when no stronger causal owner survives, a
  PV-coupled `capture_sequence` local fact may name only the captured square and
  role carried by the legal reviewed capture, plus a replayed
  `followup_queen_trade_square` when adjacent checked-line plies legally
  capture and recapture queens on that square. It remains bounded capture
  truth, not pressure, plan, or broad tactical authority;
- alternative prose: branch-scoped role comparison admitted as
  `alternative_comparison` local fact authority with a concrete SAN branch
  line; for a missed benchmark only-move, this means
  verified best/benchmark branch versus played branch, not played-move timing
  authority. Acceptable style-choice or narrow-choice tactical comparisons
  additionally require typed branch evidence where the verified-best branch has
  a concrete consequence and the played branch is preview-only; UCI branch
  identity wins over same-SAN text when UCI is present;
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
  Played-first rook-pawn hook/march support may use this carrier only when the
  board state before/after the reviewed move proves the state delta; practical
  row text cannot create that proof.

If the typed packet is absent, the truth contract may still close unsafe
surfaces, but it must not fill the missing chess reason.

Filtered local-fact projection is evidence transport, not a prose shortcut.
When `CommentaryIdeaSurface` supplies a causal local fact to the planner, the
family, producer, anchors, line binding, and evidence refs are the authority.
The basic explanation sentence remains a renderer surface and must not promote
castling/capture/endgame prose into causal claims by itself.
Endgame fallback prose may name `KingActivity` or `Opposition` fields already
present in canonical fact evidence; those typed fields remain fallback truth
projection unless a separate causal owner admits them. Scoped checked-line
takeaways may use the admitted local-fact anchors for the same fields, not row
text or guardrail strings. Oracle-only theoretical endgame outcome hints are
not public win/draw truth; absent tablebase/eval/PV result evidence, public
context and transition prose must expose the result side as `Unclear`.
Castling fallback prose follows the same rule: it may name the after-FEN
castling side, king square, rook square, and king-safety feature refs, but it
does not create opening-plan or tactical truth. Scoped checked-line takeaways
may use those admitted anchors under the same boundary.
Opening-goal truth may come from post-move board state plus played-first legal
PV witnesses for activation or comparable development. Role-specific opening
triggers must match the actual moved piece on the post-move board. A strict
PV-coupled `opening_goal` fact may enter CausalFrame as opening-goal authority,
but it cannot supply plan, pressure, timing, or tactical truth on its own.
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
For outpost occupation, truth authority is the `OutpostOccupation` exact proof
inside the packet. In MoveReview, exact outpost wording additionally requires
replayed board/PV proof that the reviewed knight lands on the square, is
friendly-pawn-supported, is not attacked by enemy pawns or enemy minor pieces,
and remains there through the immediate checked top-PV window. Existing outpost
facts and practical outpost rows cannot supply that truth by themselves.
The practical `Knight outpost` player row uses the same durability gate, so a
transient knight landing that can be attacked by an enemy minor or immediately
removed in the checked PV stays outpost-silent.
`source:outpost_tag` and `source:strong_knight` strategic-idea signals are not
truth authority and no longer publish `Practical outpost` advanced rows by
themselves; only explicit minor-piece route/directional access cues may surface
as non-durable plan-recognition support.
For exact target fixation, truth authority can name only a board-proved enemy
pawn target attacked by the moved piece after the reviewed move. Proof labels,
diagnostic target refs, and row prose cannot turn a square into a fixed target.
For structural pawn targets, truth authority must reuse the existing
`WeaknessTargetProfile`/`PawnStructureTargets` target profile and keep the pawn
target present and still attacked by the pressure side at the played-first
checked PV horizon. Move-local exact target facts carry
`target_persistent_after_line:<square>` and
`target_attacked_after_line:<square>` refs when that replay proof exists.
Without a typed target-fixing idea, moveRef, or route hint, board-only exact
target fixation is limited to the Carlsbad c-pawn target profile; generic weak
pawn pressure stays closed until producer evidence names the target.
If public wording names a pawn defender of that fixed target, the defender must
be a same-color pawn proved from the after-board.
For IQP inducement, truth authority requires the newly isolated central pawn
target to persist at the checked PV horizon and be attacked there by the
pressure side. The packet must carry a matching `target_pressure:<square>` term.
Its exact proof line must be the checked horizon line that proved both
persistence and pressure, not only the inducing prefix.
If the checked line creates and then resolves the IQP by exchange or pawn
movement, or leaves no board-proved pressure on the isolated pawn, the exact
proof is withheld.
For color-complex probes, truth authority is position-local unless the reviewed
move itself places the proof bishop/knight on the proof square and the
after-FEN board confirms that piece attacks the proof target. The trusted proof
square is the reviewed move's post-move destination when that destination
attacks the target, and the checked played-first PV window must keep the same
minor piece on that square attacking the target. An unmoved minor piece, an
immediate exchange, or a proof-piece reroute can remain backend diagnostic
context, but it cannot supply MoveReview player support or played-move pressure
truth.
For neutralize-key-break truth, a legal null-turn pawn capture against the
reviewed piece's origin square is not a pawn-break truth source. It represents
a capture threat on the moved piece, so it is excluded before
`CounterplayAxisSuppression` exact proof or `Counterplay break` surface truth
can be produced.
For central-break timing, truth authority is the
`CentralBreakTimingWitness` exact proof inside a supported-local packet. It may
promote to a timing local fact only through the claim-authority admission tied
to `central_break_timing`; plan-advance labels and prose are not enough.
When this is the surviving basic-lane fact, the truth carrier remains the
timing local fact and enters CausalFrame as `WhyNow` timing evidence.
For relation witnesses, the typed relation surface supplies the family boundary:
mobility restriction is pressure and move order is timing. Diagnostic
`relation_surface:*` guardrails cannot widen or change that boundary.
Tactical relation witnesses may carry relation-specific anchors such as
discovered-attack attacker/target squares or overload defender/duty squares;
renderer prose may surface those anchors without reparsing relation text.
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
Alternative comparison truth comes from the preserved engine-best and played
typed comparison carrier: role-aware comparisons preserve
`LineConsequenceEvidence`. The comparative sentence and branch-role labels may
render that contrast, but they do not by themselves certify branch evidence,
timing, forced-reply, or tactical authority.
Minor-piece reroute contrast is truth-preserving only as branch-scoped
alternative comparison: the replayed engine-best packet must be
`MinorPieceReroute`, the played packet must remain preview-only, and the checked
eval gap must be material.
The player decision strip may reuse that alternative-comparison truth only
through the enriched runtime comparison plus the admitted
`alternative_comparison` local fact. Rebuilding a shallow digest or reading
branch prose is not enough.

## Scene Summary

Scene summaries are post-classification diagnostics. They are not truth
contracts. A `concrete_tactical` scene summarizes admitted concrete tactical
authority; a `line_consequence` scene summarizes line authority; an
`alternative_comparison` scene summarizes branch-comparison authority.

No broad tactical scene is part of the current truth boundary.

## Fallback Truth

Exact factual fallback may state:

- played move,
- legal current-move origin and destination squares,
- capture/check basics when exact,
- short checked-line prefix,
- absence of admitted causal authority.

It may not state a cause, threat, plan, timing, or alternative unless the
corresponding typed evidence was admitted.
