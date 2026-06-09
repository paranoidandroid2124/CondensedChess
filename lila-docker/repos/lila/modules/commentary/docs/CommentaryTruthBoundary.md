# Commentary Truth Boundary

This file defines the chess-truth signoff boundary for Chesstory commentary.
It complements `CommentaryPipelineSSOT.md`, which maps the runtime pipeline.

## Core Rule

Truth comes before prose richness. If Chesstory cannot verify a decisive claim,
benchmark, compensation story, strategic plan, or lesson-like implication, the
surface must become less specific or fail closed.

## Signoff Surface

MoveReview is the only user-facing commentary truth surface. Removed
Chronicle/Active surfaces follow the canonical boundary in
`CommentaryPipelineSSOT.md`: they are excluded from signoff inputs, Chronicle
fallback semantics are gone, and released truth paths consume only the current
MoveReview runtime models and proof-contract projections.

Future `SupportedLocal` or scoped-takeaway expansion must enter through
MoveReview first and must preserve the audited payload contract.

## Truth Stack

The runtime must resolve truth in this order:

1. Build `MoveTruthFrame` before prose exists.
2. Classify move quality, benchmark truth, tactics, material economics,
   strategic ownership, punishment/conversion, and difficulty/novelty.
3. Run selective deep verification for truth-critical moves.
4. Synthesize commitment, maintenance, and conversion chains.
5. Project `DecisiveTruthContract`.
6. Resolve ownership, visibility, surface mode, and exemplar role.
7. Admit claims only through the claim-authority kernel and proof contracts.
8. Render and validate prose after truth and authority are fixed.

Serialized/debug fields may aid audit, but they may not recreate ownership when
canonical truth data is available.

## Authority And Truth Interaction

`CertifiedOwner` and `SupportedLocal` are authority decisions, not chess-truth
shortcuts. They are legal only when the current board truth, proof contract,
and tactical veto checks agree.
Increasing the number of projected `SupportedLocal` rows only exposes more
already-admitted rows; it does not make an unadmitted packet, typed surface, or
fallback carrier a truth owner.

`DiagnosticOnly`, `Suppressed`, support-only, deferred, latent, and fallback
carriers may not emit owner claims. They may remain in internal reports or
debug traces.

`QuestionPlan.claim` is not a truth owner. For MoveReview causal question
kinds, planner selection becomes releasable prose only after
`MoveReviewCausalClaim` confirms a concrete causal anchor and typed support
evidence. A failed causal gate leaves the planner row diagnostic and forces the
surface back to existing exact factual, basic, or thematic fallback behavior.
Renderer templates may express the certified claim but cannot supply missing
causality.
Support evidence and causal role are separate truth checks. Branch/PV evidence
or coda text can be retained as support, but `WhyThis` truth needs either an
admissible alternative-contrast relation or a played-move-owned consequence
relation before renderer prose can say why this move.
When that relation is admissible alternative contrast, the truth-bearing surface
claim is the typed contrast sentence itself; raw planner wording is diagnostic
unless it is also the certified claim.
When that relation is played-move-owned line consequence, the truth-bearing
surface claim may be the replayed line-consequence narrative itself, not the raw
planner wording. That promotion requires a non-`PreviewOnly`
`LineConsequenceEvaluator.surfaceCandidate` from FEN-validated `MoveReviewRefs`;
engine-only `ReplayBackedInternal` evidence and preview-only legal prefixes are
not truth-owner material for this surface.
For `WhatChanged`, a certified coda or branch line is support only unless the
claim also has played-move-owned consequence authority or admissible contrast.
It cannot by itself own the assertion that the reviewed move changed the
position.
For `WhyNow`, a generic urgency/tension sentence is support only. The claim that
the reviewed move had to happen now requires typed timing authority from
`QuestionPlanTimingWitness` or from an admissible `explicit_reply_loss`
contrast trace.
Opening-relation `WhyThis` claims also need admissible contrastive support; a
branch line or PV continuation does not by itself prove why the played move is
preferable to, or causally different from, the named alternative.
The causal gate emits diagnostic truth-boundary trace, not a new truth owner.
`MoveReviewCompressionPolicy.causalClaimTrace` and corpus
`moveReviewCausalClaim*` fields may be used to count accepted relation kinds and
rejected reasons in PGN/engine runs, but prose must still be admitted by the
certified claim path or an existing fallback truth boundary. Downstream consumers
must not parse fallback prose or trace strings to reconstruct causal truth.
For support-required planner questions, the certified claim must also carry
`MoveReviewLocalFact` admission. That fact classifies the rendered planner surface as
timing, defense, threat, pressure, plan-support, or line-consequence with a
typed authority before the renderer sees the string. Missing admission rejects
the claim; accepted planner rows expose the family through
`moveReviewLocalFact*` as rendered evidence, not as fallback truth recovery.
The family classification is not a prose parser. Surface words such as
`pressure`, `target`, `threat`, `plan`, or `supports` do not create truth.
Planner admission reads typed owner/source evidence such as timing witnesses,
forcing-defense relations, `LineConsequenceEvaluator` surface candidates, and
typed `pv_delta` move-local changes. A certified coda string without one of
those owners remains insufficient for local-fact truth.

The basic/scoped fallback boundary is now typed as well. A fallback sentence is
not a truth owner merely because `CommentaryIdeaSurface` can assemble a purpose
string from a PV. MoveReview basic descriptors must first carry
`MoveReviewLocalFact` admission with an admitted
`local_fact_family` and `local_fact_authority`. When the causal gate rejects a
high-risk planner row, strict local-fact mode admits only strict-eligible local
facts; soft line-only, plan-support, or other non-strict candidates fall through
to exact factual fallback. Canonical pressure/threat facts need played-move
ownership, defensive facts need only-move defense truth, and endgame facts need
the reviewed move to own the endgame square/role. Corpus `moveReviewLocalFact*`
fields are diagnostic trace from typed admission for this fallback truth
boundary. `reasonTags` remain display/compatibility tags and may not be used to
recover a stronger causal claim downstream.

Exact/basic fallback prose remains non-owner truth. It may report the reviewed
SAN, the next checked-line move, and the local evidence category, but it cannot
create target, forced-response, timing, or PV-verification truth by phrasing.
Those relationships require the same typed causal claim or exact evidence owner
that would be required outside fallback. Generic support anchors such as
`plan activation lane`, `plan`, or `main plan` remain diagnostic labels and are
not truth objects for fallback subject wording.

`ReplayBackedInternal` line-consequence evidence is not a truth-owner
promotion. It means the PV was legally replayed for bounded narrative or ledger
use, but it cannot emit player decision-strip claims unless the line also comes
from `MoveReviewRefs` as a `SurfaceCandidate`. A replay-observed central pawn
advance without `CentralBreakTimingWitness.exact` is only a local line
transition, not the `central_break_timing` proof family.
Likewise, a replayed exchange can support `ExchangeSequence` or
`MaterialTransition` wording without becoming defender-trade or bad-piece
liquidation truth. Those favorable-exchange owner paths require their own
board-backed witness: real defender-target relation removal for defender trade,
and current-board bishop constraint plus legal capture/recapture for bad-piece
liquidation.

Strategic plan refutation must be exact-board/probe truth. Sibling plan ranking
or alternative-dominance metadata can demote selection, but it is not a chess
refutation and must not be reported as `Refuted`.

Strategic plan promotion and demotion must be consumed through the typed
`StrategicPlanEvidenceView` derived from `PlanEvidenceEvaluator`. Raw
`StrategicPlanExperiment.evidenceTier` strings, compatibility markers, and
subplan ids cannot by themselves certify truth, refutation, provenance,
quantifier, stability, or exact-family ownership.
Board-bound probe truth requires the returned probe certificate to echo the
requested FEN and probed move; missing or malformed values are not truth. Both
echoed FEN strings must parse and echoed moves must be valid UCI. For
multi-move requests, the certified move must be one of the requested moves.
Unknown request purposes have no truth contract and cannot be rescued by
explicit signal names or by the result's own purpose. Unknown-purpose probes
also cannot be treated as refutations through the default cp-loss bound.
Refutation probes are negative evidence only: if they do not refute a plan,
they clear that specific punishment path but do not certify the plan as true.

Exact-family strategic truth remains witness-bound. A plan taxonomy label such
as `central_break_timing` does not certify the exact central-break proof family
unless the exact board witness is present. A product surface row is allowed
only after that exact witness and a `SupportedLocal` packet both survive the
runtime boundary; this does not make the family `CertifiedOwner`. For this
SupportedLocal row, equal or near-equal PV scores and missing two-move branch
keys do not by themselves refute the witness; the required truth is the legal
non-capturing same-file d/e pawn advance to `d4`, `e4`, `d5`, or `e5`, plus a
board-backed link from the played move. The packet witness terms must mirror
the typed `CentralBreakTiming` proof's `break_move`, `break_token`, and central
break square, so a bare typed proof cannot be attached to unrelated packet
terms. The played move itself can be that exact witness when it legally makes
the same central break; top-PV omission is
not by itself a truth failure for this bounded support row. Diagonal captures
and prep/challenge pawn moves, including Sicilian c-pawn and King's Gambit
f-pawn opening breaks, are not truth for the product-visible `Central break`
row. Those opening-break rows may surface only as lower-authority practical
support when the reviewed pawn move legally replays, the post-move opening goal
is achieved or partial, the selected `OpeningGoals` trigger matches that played
move, and the legal top PV starts with that same played move.
The current product-surface central-break fixture is the Maderna exact scene:
from `nrb1r1k1/1pqn1pbp/p2p2p1/P1pP4/2N1PP2/2N2B2/1P4PP/R1BQR1K1 w - - 3 17`,
played `e4e5` projects only as a MoveReview `Central break` row after the
typed `PlayerFacingExactSliceProof.CentralBreakTiming("e4e5", "e5", "e4-e5")`
matches the packet source/family and mirrored break-token terms. The
source-catalog Maderna row remains deferred unless source review independently
admits the owner/proof contract.
Central-break runtime truth is generic board/PV truth only. Historical source
rows may remain in test/tooling precedent catalogs, but runtime witness
construction must not stamp source-specific FEN markers into `sourceTags`,
owner seeds, or release terms.

Opening-family player-surface truth is bounded support only.
`OpeningFamilyClaimResolver` may return `SupportedLocal` only for a structured
catalog family wire key whose opening label and static `OpeningNameLookup` FEN
result both match the same family inside the opening proof window. The FEN
result may match through a same-EPD static book alias retained for
transposition-aware canonicalization, but not through board-shape predicates,
strategic transposition proofs, or rendered prose.
`MoveReviewPlayerPayloadBuilder` may emit an `Opening family` support row only
after that resolver decision; label-only opening data, broad opening phase, raw
rendered prose, or FEN lookup without the structured opening label must remain
suppressed. The row is not a `CertifiedOwner` claim and does not by itself
certify a strategic target square. The row may carry a target square only as
bounded metadata from same-family legal route evidence plus target-mode board
validation and catalog allowlisting; this remains support metadata, not a
standalone exact-slice owner claim. It may also carry sanitized `openingBook`
metadata from the admitted `OpeningReference` aggregate (ECO, total games, and
top SAN moves), but this is display support only and is not master-game truth,
family admission, or source provenance. Expanding `openings.tsv` with common
variation endpoints only broadens the static FEN/name lookup pool; it does not
lower this truth boundary.

Exact-slice signoff must consume the typed `PlayerFacingExactSliceProof`
created by the board/probe witness branch. Owner, anchor, structure,
continuation, and prose terms may explain or diagnose the claim, but they are
not truth objects and must not be parsed back into exact proof.
Structural practical-plan context is a lower-authority display boundary.
`MoveReviewPlayerPayloadBuilder` may mention the current structure in a
`Practical plan` row only from a typed non-`Unknown` `StructureProfileInfo`
with confidence at or above 0.70 plus an `OnBook` or `Playable`
`PlanAlignmentInfo` whose matched plan or narrative intent also matches the
bounded practical plan, or from a prose-eligible, plan-matched
`StructurePlanArcBuilder` arc. Plan matching is exact against normalized plan
text and typed matched-plan ids; prose containment still requires the contained
phrase to have at least three words, so broad labels such as `pressure`,
`attack`, `break`, or `queenside pressure` remain too generic. This gives the
player a board-structural reading such as
why a practical plan fits the pawn shape, but it is not `SupportedLocal`, does
not publish `authority.target`, and does not satisfy any exact
`PlayerFacingExactSliceProof` contract.
The guarded profile fallback may also mention typed `centerState` as
current-board context. That phrase describes the pawn-shape environment only and
does not create center-control, break-timing, or exact route authority.
The same plan-matched, prose-eligible arc may support a lower-authority
`Practical route` advanced row through `supportPrimaryText`. Its truth source is
the current board plus semantic piece-activity route safety, not a certified
line replay or exact route proof; exact file-entry, outpost, and target claims
must still enter through their own typed proof contracts.
`Practical move` is even narrower on this path: the reviewed `playedMove` must
legally replay for one ply from the current FEN, start from the matched
structure arc's deployment origin, and land on one of that arc's route squares.
This is current-board move-to-route context, not move-owner, PV, same-file
reinforcement, adjacent guard, or exact route authority.
`Practical restraint` is available on this same lower-authority path only when
the arc's prophylaxis support comes from a current-board `PreventedPlanInfo`
with `sourceScope = Now` and a positive `counterplayScoreDrop` or negative
`mobilityDelta`. Citation or threat-line prophylaxis support, and default-current
prevented plans without an impact signal, are not board-structural truth objects
for this fallback row and must not be surfaced as if a continuation witness had
been admitted.
`Practical fit` is also lower-authority context. It may surface only
non-positive plan-alignment reasons already produced by `PlanAlignmentInfo` and
humanized by `StructurePlanArcBuilder`, for example missing structural
preconditions. It is a caution that the structure fit is partial, not proof that
the motif is owned or that the plan is public.
`Practical task` may surface only from `StructurePlanArc.practicalCoda` after
the same plan-matched, prose-eligible structure arc has already passed. The
practical verdict is attached context, not an independent board witness, motif
owner, target, or exact-proof authority.
Typed structural-idea practical rows may expose up to three distinct row labels
for one move, but that only broadens lower-authority educational context. It
does not convert source/prose/motif text into `SupportedLocal`, does not publish
target or proof metadata, and does not bypass the exact-slice proof contract.
`Practical pressure` may surface from `StrategyPack` target-fixing ideas only
when typed strategy evidence refs already carry the Carlsbad fixation profile or
minority-attack semantic source plus target-pressure facts, or
`source:minority_attack_support` plus a `minority_attack_support_<flank>` fact
and a valid structural target square, or a current-board
weakness source with its matching fact: `source:enemy_weak_square` plus
`enemy_weak_square_<square>`, or `source:weak_complex_fixation` plus a
`weak_complex_*` fact such as `weak_complex_isolated_pawn` or
`weak_complex_backward_pawn` and a valid focus square, or
`source:doubled_pawn_pressure_motif` plus `doubled_pawn_pressure_shape`,
`doubled_pawn_file_<file>`, a valid focus file, Build readiness, pack-side
owner match, and typed confidence, or compensation target pressure with
`source:compensation_target_fixation`, the matching `compensation_target_fixation`
fact, `material_deficit_compensation`, a valid focus square, pack-side owner
match, and typed confidence. The weakness-source branch also requires pack-side owner
match, and its named target list excludes squares already eligible for the
separate `Practical target` row on the same target square. Exact `Fixed target`
or `Minority attack` rows remove the same square from every target-naming
pressure branch; an eligible target plan or exact target row on another square
does not close this lower-authority context. A mixed target list may still
publish only the remaining typed focus squares. This target-square filtering
does not close doubled-pawn pressure, which is file-scoped by
`doubled_pawn_file_<file>` and a valid focus file. It is support-only pressure context with no
target metadata; generic target-fixing plan text, focus-square lists,
plan-match bridges, color-complex-only weakness tags, directional target access
without board weakness corroboration, source-only weak-square tags,
source-only/no-square weak-complex motif tags, source-only/no-target minority
support, source-only/shape-only/no-file doubled-pawn pressure tags, and
source-only/no-fact compensation target fixation are not enough.
`Practical space` may surface from `StrategyPack` space/restriction ideas only
when typed strategy evidence refs already carry a narrow structure-backed
profile: color-complex clamp plus enemy-color-complex weakness, a concrete
dark/light complex token, and at least two valid focus squares. A single
dark/light token names the lower-authority complex and overrides stale
`focusZone` text; absent or ambiguous dark/light tokens keep the color-complex
clamp row closed. Maroczy-bind
profile plus `structure_maroczy_bind` for White with pack-side owner match; or
IQP central presence plus `structure_iqp_white`, `iqp_central_presence_shape`,
and focus square `d4` for White with pack-side owner match;
or IQP space bridge plus `source:iqp_space_bridge` and `structure_iqp_white`
for White, center focus, `Build` readiness, pack-side owner match, and the
producer confidence floor;
or locked-center bind source plus `structure_locked_center`, center focus,
pack-side owner match, and typed locked-center confidence. The locked-center
source itself requires a locked center with same-side space edge or
color-complex clamp support. It may also use typed central-space edge support
only when refs include `source:central_space_edge` and `central_space_edge_shape`,
with `Ready` readiness, center focus, pack-side owner match, and the producer
confidence floor. It is support-only structure context with no target metadata;
it may also use current-board space-advantage motif support only when refs
include `source:space_advantage_motif`, `space_advantage_motif_shape`, and a
`space_pawn_delta_<n>` fact with `n >= 2`, with `Ready` readiness, center focus,
pack-side owner match, and motif confidence;
it may also use current-board central pawn-advance motif support only when refs
include `source:central_pawn_advance_motif`,
`central_pawn_advance_shape`, a matching central `central_pawn_file_<file>` fact,
and `central_pawn_to_rank_<n>` with `n >= 4`, with center focus, `Build`
readiness, pack-side owner match, and the motif confidence floor;
it may also use current-board pawn-chain motif support only when refs include
`source:pawn_chain_space_motif`, `pawn_chain_space_shape`, and a
`pawn_chain_<base>_<tip>` fact, with at least two valid focus files, `Build`
readiness, pack-side owner match, and the pawn-chain confidence floor;
it may also use typed aggregate mobility-restriction support only when refs
include `source:mobility_restriction` and `mobility_restriction_shape`, center focus,
pack-side owner match, and confidence at least `0.72`, which corresponds to at
least a two-piece low-mobility gap in the current board-derived producer.
Source-only central-space, source-only or one-piece mobility restriction,
source-only or weak-delta space-advantage motif support, generic locked-center,
source-only or non-central-file central pawn advance motif support,
source-only or shape-only pawn-chain motif support, pawn-chain support without
two valid files, source-only IQP space bridge, or plan-match space carriers
remain too generic.
`Practical break` may surface from `StrategyPack` pawn-break ideas only when
typed strategy evidence refs already carry pawn-analysis or pawn-play
break-ready source, the matching break-ready fact, `Ready` readiness, a valid
focus file, and high confidence, or when refs carry the French Advance ...f6
seed (`source:french_f6_break_seed`, `french_f6_break_seed_shape`, `white_e5_chain`,
`black_f7_break_pawn`) with Black owner, f-file focus, e5/f6 focus squares, and
high confidence. A file-opening consequence can surface only with
`source:file_opening_consequence` and a matching `contested_file_<file>`
focus-file fact. Central-tension break support can surface only with
`source:central_break_tension`, a valid central focus file, center focus,
owner/pack side agreement, typed confidence, and either `locked_center` or
`Ready` readiness. Current pawn-break motif support can surface only with
`source:pawn_break_motif`, `pawn_break_motif_shape`, and a `break_file_<file>`
fact matching the focus file, with `Build` readiness, owner/pack side agreement,
and the motif confidence floor. It is support-only pawn-structure context
with no route/token metadata; plan-match break preparation, counter-break race,
source-only central-break tension, source-only/shape-only pawn-break motif
carriers, non-central-file tension, and broad French
profile carriers remain too generic for this row and do not satisfy exact
central-break authority.
`Practical restraint` may surface from `StrategyPack` counterplay-suppression
ideas only when typed strategy evidence refs already carry Hedgehog or Maroczy
break-denial board geometry: Hedgehog requires
`source:hedgehog_break_denial_geometry`, `structure_hedgehog`,
`hedgehog_break_denial_shape`, White owner, b/d file focus, queenside zone,
and high confidence; Maroczy requires `source:maroczy_break_denial_geometry`,
`structure_maroczy_bind`, `maroczy_break_denial_shape`, White owner, c/d file
focus, center zone, and high confidence. It may also surface an
opponent-counterbreak denial bridge only when typed refs include
`source:opponent_counterbreak_denial`, the surviving `opponent_counter_break`
fact, a concrete focus file, `Ready` readiness, pack-side owner match, and the
producer confidence floor. It may also surface current-board counterplay
suppression only when typed refs include `source:counterplay_suppression`,
`counterplay_suppression_shape`, the single-evidence `counterplay_break_denial` marker,
`break_neutralized`, and `denied_break_resource`, with a concrete focus file,
`Ready` readiness, pack-side owner match, and the producer confidence floor. It
may also surface current-board passer-blockade motif support only when typed refs
include `source:passer_blockade_motif`, `passer_blockade_shape`,
`blockade_square_<square>`, and `blockaded_pawn_<square>`, with `Ready`
readiness, pack-side owner match, and the motif confidence floor. This remains a
lower-authority practical brake on the named passed pawn and does not satisfy the
exact `Passer blockade` top-PV/public row. It may also surface compensation
counterplay denial only when typed refs include
`source:compensation_counterplay_denial`, `material_deficit_compensation`,
and `break_neutralized`, with a concrete focus file, pack-side owner match, and
the compensation producer confidence floor. The row closes when exact `Counterplay break`, `Counterplay restraint`,
`Break and entry`, or `Route denial` rows are already visible. It is
support-only structure context with no route/token metadata; broad
containment/suppression profiles, source-only opponent-counterbreak rows,
source-only/generic counterplay suppression, source-only/generic compensation
counterplay denial, source-only/shape-only/no-pawn passer-blockade motif rows,
compensation denied-square-only rows, merged-ref counterplay
suppression, counterplay-score-drop-only rows, and plan-match suppression remain too generic for this row and do not satisfy
exact counterplay-restraint authority.
When multiple selector carriers merge into one idea, surface projection may keep
the source, shape, and witness facts needed by these support-only gates; keeping
those refs visible does not convert the row into exact proof authority.
`Practical line` may surface from `StrategyPack` line-occupation ideas only when
typed strategy evidence refs already carry current occupied major-piece line support:
`source:occupied_line_control`, an `occupied_r_<square>` or `occupied_q_<square>`
fact matching a focus square, and an open/semi-open file fact matching the focus
file; queen support may also use `occupied_seventh_rank`, or
doubled-rook file support with `source:doubled_rooks` and a
`doubled_rooks_<file>` fact matching a focus file, including current
`Motif.DoubledPieces` carriers when the role is rook, or seventh-rank rook support
including seventh-rank invasion motif carriers, with `source:rook_on_seventh`,
the `rook_on_seventh_shape` fact, and a rook beneficiary, or current
open-file major-piece support, including open-file motif carriers, with
`source:open_file_control`, an `open_file_<file>` fact matching a focus file,
and a rook/queen beneficiary, or current semi-open-file motif support with
`source:semi_open_file_control`, a `semi_open_file_<file>` fact matching a focus
file, and a rook/queen beneficiary, or connected-rooks
coordination support with `source:connected_rooks`, the `connected_rooks_shape` fact,
and a rook beneficiary, or directional rook-line support with
`source:directional_line_access`, `directional_line_access_shape`, a valid focus
square, and an open/semi-open file fact matching a focus file, or exact-route
line-control support with `source:line_control_features`, `line_control_shape`,
`source:route_line_access`, `route_surface_exact`, a valid focus square, and an
open/semi-open file fact matching a focus file, or compensation line support with
`source:compensation_open_lines`, `compensation_open_lines_shape`,
`material_deficit_compensation`, and an open/semi-open file fact matching a focus
file, or delayed-recovery line support with `source:delayed_recovery_window`,
`delayed_material_recovery`, `development_lead_compensation`,
`material_deficit_compensation`, and an open/semi-open file fact matching a focus
file. The idea must name the matching rook or queen
beneficiary, match the pack side, and carry typed confidence; queen,
doubled-rook support must also be `Ready`, seventh-rank support must be `Ready`
with confidence at least `0.72`, open-file support must be `Ready` with
confidence at least `0.80`, semi-open-file motif support must be `Ready` with
confidence at least `0.78`, while connected-rook support may be `Build` or
`Ready`, directional rook-line
support must be `Build` with a rook beneficiary and confidence at least `0.50`,
exact-route line-control support must be `Ready` with a rook beneficiary and
confidence at least `0.60`,
compensation line support must
be `Build` with confidence at least `0.70`, and delayed-recovery support must be
`Build` with confidence at least `0.74`.
The row closes when exact file-entry or doubled-rooks support is already
visible on the same file, or when exact seventh-rank-entry or connected-rooks
support is already visible. It is support-only current-board line context with no target/token
metadata; source-only open-file tags, open-file tags without a major-piece
beneficiary, source-only/no-file semi-open-file tags, semi-open-file tags
without a major-piece beneficiary, source-only occupied-line tags, source-only
doubled-rook tags, non-rook doubled-piece motif carriers, source-only
seventh-rank tags, source-only connected-rook tags, connected-rook tags without
a rook beneficiary, source-only/no-route
line-control, non-exact route line access, source-only, no-file, queen, or
low-confidence directional line access,
source-only or no-file compensation line/delayed
recovery carriers, plan-match line carriers, and source-only open-file tags remain too generic for
this row and do not satisfy exact local-file authority.
`Practical outpost` may surface from `StrategyPack` outpost ideas only when
typed strategy evidence refs already carry `source:outpost_tag` plus an
`outpost_<square>` fact matching a valid focus square, including current
minor-piece `Motif.Outpost` carriers, or occupied
strong-knight support with `source:strong_knight` plus a
`strong_knight_<square>` fact matching a valid focus square and a knight
beneficiary, or exact route-outpost support with `source:route_outpost_access`,
`route_outpost_access_shape`, and `route_surface_exact` aimed at a valid focus square
by a minor piece, or directional outpost support with
`source:directional_outpost_access`, `directional_outpost_access_shape`, and a valid
focus square aimed at by a minor piece. The idea must match the pack side and
carry typed confidence for the underlying outpost/strong-knight/route/directional
bridge; tag, strong-knight, and exact-route support must be `Ready`, while
directional outpost support must be `Build` with confidence at least `0.64`.
The row must have no
already visible exact `Opening outpost` or `Knight outpost` row. It is
support-only current-board outpost context with no target/token metadata;
unoccupied/build strong-knight, entrenched-piece, non-exact route access,
source-only, non-minor, non-`Build`, or low-confidence directional outpost
access, opening-goal outpost, and plan-match outpost
carriers remain too generic for this row and do not satisfy exact
`OutpostOccupation` authority.
`Practical trade` may surface from `StrategyPack` favorable-trade/transformation
ideas only when typed strategy evidence refs already carry
`source:iqp_simplification_profile`, `structure_iqp_black`, and either
`capture_or_exchange` or `iqp_trade_down_plan`, with White owner, pack-side
owner match, high typed confidence, and no already visible exact
`Simplification` row. IQP exchange-availability support may also surface only
when refs carry `source:exchange_availability_bridge` and `structure_iqp_black`,
with White owner, `Build` readiness, pack-side owner match, and the
exchange-availability confidence floor. It is support-only IQP trade context
with no target/token metadata; classification windows, generic capture/exchange,
source-only exchange-availability bridges, plan-match transformation, soft
transformation plan support, and non-IQP transformation carriers remain too
generic for this row and do not satisfy exact `SimplificationWindow` authority.
`Practical conversion` may surface from `StrategyPack`
favorable-trade/transformation ideas only when typed strategy evidence refs
already carry both `source:winning_endgame_transition` and
`winning_endgame_transition_shape`, with `Ready` readiness, pack-side owner match,
winning-endgame confidence, and no already visible exact `Technical conversion`
row, or when refs already carry current rook-endgame motif support with
`source:rook_endgame_pattern`, `rook_endgame_pattern_shape`, and either
`rook_behind_passed_pawn` or `king_cut_off`, with `Build` readiness, pack-side
owner match, confidence at least `0.72`, and no exact `Technical conversion` row
already visible; rook-endgame wording names a specific pattern only when exactly
one rook-endgame pattern fact survives, otherwise the row stays rook-endgame-map
neutral, or when refs already carry current endgame-technique motif support
with `source:endgame_technique_motif`, `endgame_technique_shape`, an
`opposition_direct`, `opposition_distant`, `opposition_diagonal`, or
`zugzwang_shape` fact, or endgame-phase `Motif.KingStep.Activation` support with
`king_activity_shape`, endgame focus, `Build` readiness, pack-side owner match,
confidence at least `0.72`, and no exact `Technical conversion` row already
visible; endgame-technique wording names a specific technique only when exactly
one technique fact survives, otherwise the row stays endgame-technique-map
neutral, or when refs
already carry current passed-pawn motif support
with `source:passed_pawn_conversion_motif`, `passed_pawn_conversion_shape`, and
a `passed_pawn_<square>` fact matching a valid focus square, including
`Motif.PassedPawnPush` carriers that also keep `passed_pawn_push` and advanced
rank support when applicable, and `Motif.PawnPromotion` carriers that also keep
`pawn_promotion`, `promotion_piece_<role>`, and optional `underpromotion`
support, with `Build` readiness, pack-side owner match, confidence at least
`0.72`, and no exact `Technical conversion` row already visible. It is
support-only endgame or passer-structure context with no
target/token metadata; classification-only transformation windows, source-only
or shape-only rook-endgame pattern carriers, source-only or shape-only
endgame-technique motif carriers, endgame-technique motif carriers without
opposition, zugzwang, or endgame king-activity facts, source-only or shape-only
passed-pawn motif carriers, passed-pawn carriers without a matching square fact,
soft plan matches, and generic exchange carriers remain too generic for this row
and do not satisfy restricted-defense `Technical conversion` authority.
`Practical minor` may surface from `StrategyPack` minor-piece ideas only when
typed strategy evidence refs carry a concrete minor-piece bridge:
`source:strong_knight_vs_bad_bishop` plus a `strong_knight_<square>` fact
matching a focus square, or `source:piece_activity_bad_bishop` plus an
`enemy_bad_bishop_<square>` fact matching a focus square, current-board
enemy-bad-bishop support with `source:enemy_bad_bishop` plus the
`enemy_bad_bishop_shape` fact, current-board
bishop-pair support with `source:bishop_pair_advantage` plus the
`bishop_pair_advantage_shape` fact, current-board opposite-colored-bishop support with
`source:opposite_color_bishops` plus the `opposite_color_bishops_shape` fact, or a
current-board good-bishop count edge carrying `source:good_bishop`, `good_bishop_shape`, `source:minor_piece_count_imbalance`,
`minor_piece_count_imbalance_shape`, and `good_bishop_count_edge`, or current
minor-piece centralization motif support with `source:piece_centralization_motif`,
`piece_centralization_shape`, and a `centralized_piece_<square>` fact matching a
focus square, current minor-piece maneuver support with
`source:piece_maneuver_motif`, `piece_maneuver_shape`, and a minor beneficiary,
or current knight-vs-bishop motif support with
`source:knight_vs_bishop_motif`, `knight_vs_bishop_motif_shape`, and
`knight_preferred_over_bishop`. French Advance profile refs keep the existing
French-specific wording; non-French bridge rows use current minor-piece-map
wording. The idea must name the relevant minor beneficiary, match the pack side,
and carry typed bridge confidence; non-French strong-knight and bishop-pair rows
must also be `Ready`, bishop-pair support closes when an exact `Bishop pair`
row is already visible, and opposite-colored-bishop support closes when an exact
`Opposite-color bishops` row is already visible. It is support-only
current-structure minor-piece context with no target/token metadata; French
profile alone, source-only bishop pair, source-only good bishop, count imbalance without the good-bishop count-edge pair, opposite-color
bishop source-only carriers, source-only or no-square/no-minor centralization
motif carriers, source-only/no-minor maneuver motif carriers,
source-only/shape-only knight-vs-bishop motif carriers, and plan-match minor-piece carriers remain too
generic for this row and do not satisfy exact outpost, bishop-pair,
opposite-color-bishops, or supported-local authority.
`Practical prophylaxis` may surface from `StrategyPack` prophylaxis ideas only
when typed strategy evidence refs already carry a board-pattern prophylaxis
source and matching focus geometry: `source:bishop_pin_watch` with a valid
`g4`/`g5` focus square, or `source:queenside_counterbreak_watch` with a `b`-file
focus. It also requires pack-side owner match, typed board-pattern confidence,
and no already visible promoted exact/probe-backed `Prophylaxis` row. It is
support-only current-board prophylaxis context with no target/token metadata;
plan-match prophylaxis, threat-analysis prophylaxis, prevented-plan,
counterbreak-watch, raw prophylaxis plans, and probe-backed prophylaxis rows
remain too generic for this row and do not satisfy exact prophylaxis-restraint
authority.
`Practical attack` may surface from `StrategyPack` king-attack ideas only when
typed strategy evidence refs already carry the full fianchetto assault profile:
`source:fianchetto_assault_profile`, `source:opposite_side_storm`, and
`structure_fianchetto_shell`, with kingside focus, `Build` readiness, pack-side
owner match, and typed fianchetto-assault confidence; or when refs already carry
current king-ring pressure with `source:king_ring_pressure` plus the
`king_ring_pressure_shape` fact, a concrete enemy-king focus zone, `Build` readiness,
pack-side owner match, and the producer confidence floor; or when refs already
carry enemy king central-exposure support with `source:enemy_king_stuck_center`
plus `enemy_king_central_exposure`, a concrete enemy-king focus zone, `Build`
readiness, pack-side owner match, and the producer confidence floor; or when
refs already carry enemy weak-back-rank support, including weak-back-rank motif
carriers, with `source:enemy_weak_back_rank` plus
`enemy_weak_back_rank_shape`, a concrete enemy-king focus zone, `Build`
readiness, pack-side owner match, and the producer confidence floor; or when refs already
carry current flank-pawn hook pressure with `source:flank_pawn_pressure` plus
`hook_creation_chance`, a concrete enemy-king focus zone, `Build` readiness,
pack-side owner match, and the producer confidence floor, unless an exact
`Hook creation` or `Rook-pawn march` row is already visible; or when refs
already carry flank pawn-advance motif support with
`source:flank_pawn_advance_motif`, `flank_pawn_advance_shape`, a matching
`flank_pawn_file_*` fact on the a/b/g/h files, a `flank_pawn_to_rank_*` fact at
relative rank four or beyond, a concrete flank focus zone, `Build` readiness,
pack-side owner match, and the motif confidence floor, unless an exact
`Hook creation` or `Rook-pawn march` row is already visible; or when refs
already carry `source:compensation_diagonal_battery` and
`compensation_diagonal_battery`, `material_deficit_compensation`, with B/Q
beneficiary pieces, king-side/center attack focus, pack-side owner match, and
the compensation-battery confidence floor; or when refs already carry
`source:compensation_development_lead`, `development_lead_compensation`, and
`material_deficit_compensation`, a concrete enemy-king focus zone, `Build`
readiness, pack-side owner match, and the producer confidence floor; or when
refs already carry `source:compensation_king_window`,
`uncastled_or_unsettled_king_window`, and `material_deficit_compensation`, a
concrete enemy-king focus zone, `Build` readiness, pack-side owner match, and
the producer confidence floor; or when
refs already carry exact route-attack support with
`source:route_attack_lane`, `route_attack_lane_shape`, a concrete enemy-king
focus square and zone, a beneficiary piece, `Ready`
readiness, pack-side owner match, and the route-attack confidence floor; or
when refs already carry directional attack-lane support with
`source:directional_attack_lane`, `directional_attack_lane_shape`, a concrete
enemy-king focus square and zone, a beneficiary piece, `Build` readiness,
pack-side owner match, and the directional producer confidence floor; or when
refs already carry current motif-battery support with `source:motif_battery`, a
diagonal/file battery-axis fact, two concrete battery squares, two beneficiary
pieces, a concrete enemy-king focus zone, `Build` readiness, pack-side owner
match, and the motif-battery confidence floor, unless an exact `Battery
pressure` row is already visible; or when refs already carry current rook-lift
motif support with `source:motif_rook_lift`, a concrete focus file, rook
beneficiary, concrete enemy-king focus zone, `Build` readiness, pack-side owner
match, and the motif-rook-lift confidence floor, unless an exact `Rook lift` row
is already visible; or when refs already carry current piece-lift motif support
with `source:motif_piece_lift` plus `motif_piece_lift_shape`, a beneficiary
piece, concrete enemy-king focus zone, `Build` readiness, pack-side owner match,
and the motif-piece-lift confidence floor; or when refs already carry current
normal/discovered check-motif support with `source:motif_check_pressure`, a
`check_type_normal` or `check_type_discovered` fact, a concrete enemy-king focus
square and zone, a beneficiary piece, `Ready` readiness, pack-side owner match,
and the motif-check confidence floor; or when refs already carry current
fianchetto motif support with `source:fianchetto_motif`,
`fianchetto_motif_shape`, a `fianchetto_side_kingside` or
`fianchetto_side_queenside` fact, a concrete focus zone, bishop beneficiary,
`Build` readiness, pack-side owner match, and the motif confidence floor; or
when refs already carry current initiative motif support with
`source:initiative_motif`, `initiative_motif_shape`, a concrete enemy-king focus
zone, `Build` readiness, pack-side owner match, the motif confidence floor, and
an `initiative_score_*` fact at ten or above. It is
support-only current-structure
attack context with no target/token metadata; it closes when an exact back-rank
mate, mate-net, or Greek-gift row is already visible. Mate-net, source-only king-ring pressure, source-only
enemy king central exposure, source-only weak back rank, source-only flank-pawn
pressure, rook-pawn-ready-only flank pressure, attacking-threat analysis,
source-only/flank-shape-only flank pawn-advance motif carriers, non-flank
pawn-advance motif carriers, motif attack lanes, source-only or non-exact route-attack lanes, directional
attack-lane source-only rows, source-only compensation development, source-only compensation
king-window, source-only or axis-only/no-square motif battery, source-only or
no-file/no-rook motif rook lift, source-only or no-zone/no-piece motif piece
lift, source-only or no-square/no-zone/no-piece motif check pressure,
mate/double/smothered check motifs, source-only/no-bishop/mirrored fianchetto
motif carriers, source-only/no-zone/low-score initiative motif carriers,
plan-match king-attack carriers, and
opposite-side storm alone remain too
generic for this row and do not satisfy exact tactical authority.
Weakness target truth is centralized in `WeaknessTargetProfile`, which reads
the current legal FEN and reuses board-level pawn primitives for backward,
isolated, IQP, doubled, and fixed pawn targets. A generic exact target-fixation
witness may bind only a square present in that profile for the pressure side.
This profile is a board fact source; it is not by itself a certified strategic
claim. It may support a lower-authority `Practical target` row for current-board
structure context without a best-line persistence witness, but the row must not
publish `authority.target` or use checked-line proof wording. That current-board
row may name only profile-provided non-generic structure context as educational
prefix text; generic center classifications such as Open/Locked/Fluid/Symmetric
stay unspoken, and the structure name is not owner, motif, or target truth. When
a best line is available and legal replay shows defender liquidation, the
practical row is suppressed. A target that exists only at the
checked-line endpoint can surface
only with an exact square target hint and a trusted endpoint or five-ply horizon
boundary. Bounded practical plans can bind a specific display target only
with exact square evidence tokens
`weakness_target:<square>`, `fixed_target:<square>`,
`coordinated_target:<square>`, exact `target:<square>`, `target_fixing:<square>`,
`enemy_weak_square:<square>`, or `weak_complex:<square>`; role-bearing target
tokens such as `target:e5:queen` are relation/material facts, not weakness
target truth. Pressure-side capture marks the target resolved, and defender
liquidation suppresses the target row instead of treating a vanished pawn as a
fixed objective. Shorter persistent lines may still support current-board
context, but they are not enough to create a new endpoint-only checked-line
target. Best-vs-chosen
target comparison metadata follows the same continuation boundary: an empty
best PV cannot create a target contrast from `resultingFen` alone.
Transposition-aligned strategic truth is a separate main-plan provenance, not
`ProbeBacked` truth. `TranspositionPvAligner` must legally replay the supplied
PV from the current FEN, reject illegal or too-shallow lines, recompute the
terminal `WeaknessTargetProfile`, bind only an expected target square carried
by the plan evidence as an exact `target:<square>`,
`weakness_target:<square>`, `fixed_target:<square>`, or
`coordinated_target:<square>` token, or a route/weak-complex exact square token
such as `target_fixing:<square>`, `enemy_weak_square:<square>`, or
`weak_complex:<square>`, require positive attacker-minus-defender
control on that target, and veto mate or large mover-loss lines. Passing this
boundary can admit a main strategic plan with `transposition_aligned`
provenance, but it does not satisfy exact-slice proof contracts,
`check_qualifying`, or probe-backed claim provenance.
Proof checks that bind a move to a square must use the chess UCI parser and the
typed destination square, not string slices over move text. SAN remains display
text.
MoveReview branch proof follows the same rule. Best-defense branch keys,
exact-slice continuation terms, favorable-exchange witnesses, queen-trade
shield checks, simplification exchange squares, and IQP inducement prefixes are
accepted only from bounded legal replay over the current FEN. Best-defense move
terms are likewise derived from the replayed legal move. Raw
`VariationLine.moves` is the preferred UCI input to that replay; parsed PV
metadata is a fallback only when raw moves are absent and cannot override the
engine line. Raw move text by itself can still support display and citation
wording, but it cannot certify same-branch state, continuation persistence, or
pawn-structure transition truth without legal replay.
Author evidence and forced-line truth use the same legal post-move replay for
reviewed, best, and branch-entry UCI moves; illegal or stale UCI fails closed
instead of reusing the original FEN as a synthetic post-move board.
Quiet-move intent is also display support, not truth signoff. Public
`Piece improvement`, `King safety`, or `Technical conversion` rows require the
existing user-facing packet gate plus legal replay of the reviewed UCI move and
a matching square anchor. The packet proof family, proof source, and ontology
family must also match the quiet-intent class, so a mismatched support packet
cannot relabel itself as a different practical row. They keep `PracticalPlan`
authority with no token and do not become owner truth, lesson readiness,
opening-family truth, central-break truth, or counterplay/prophylaxis truth.
Tactical truth vetoes and release risks fail closed.
Decision comparison and branch-citation wording use the same identity rule:
stale parsed SAN cannot rename a raw engine PV move or create a different
alternative branch label.
PV-derived resource-removal support also uses legal replayed origin and
destination squares for break or denied-square hits; generic threat words in PV
move text do not certify resource loss.
Generic exchange-forcing support must see a replayed legal capture on the
anchored branch. Parsed capture flags or same-destination text are insufficient
without legal replay from the current FEN.
Bad-bishop truth requires both the same-color central pawn constraint and a
mobility or own-pawn ray-blocker constraint. The strategic activity analyzer
reuses that rule for support signals; central-pawn count alone is not board
truth.
Defender-trade truth cannot originate in `PlanMatcher` from a raw
`RemovingTheDefender` motif or broad text labels. Those are generic exchange
support only; defender-trade ownership requires the replayed defense-relation
branch or typed semantic observation described above.
Broad defender/trade prose also cannot create `trade_key_defender` ownership in
MoveReview truth mode. When it is paired with a legal immediate
capture/recapture, move-linked exchange cue, and narrative anchor, it may speak
only as the softer `simplification_window` exchange sequence.
Raw `RemovingTheDefender` positional tags also cannot surface as a precise
concept-summary claim; they degrade to generic `Exchange pressure` unless the
replay-backed branch supplies defender-trade truth.
MoveReview basic surface recapture and center-capture labels are also board
truth. They require legal replay from the coupled PV FEN; SAN capture markers
or UCI file deltas are insufficient. Pin/skewer continuation confirmation is
also replay-bound; a sliced UCI coordinate cannot certify that the line touches
the pinned, behind, front, or back square.
Line-consequence wording keeps the public/internal split. Public player-surface
line consequences require a `MoveReviewRefs` line that passes strict legal FEN
replay. Engine-only `VariationLine` material may support internal narrative or
ledger wording from a legal concrete prefix, but stale tail moves are not proof,
are not kept as continuation evidence, and cannot promote mate/check metadata.
If the legal prefix is only a preview, the engine-only consequence stays
diagnostic.
Weakness-target practical guidance is lower authority, but it still cannot use
a stale or non-replayable `resultingFen` as board truth. Persistence or
liquidation display requires legal replay to the claimed board state, or the row
falls back/suppresses.
Tactical-window gates in MoveReview, including sacrifice settlement, forcing
proof availability, and IQP tactical-first veto, derive capture/check counts
from legal replay. Mate or forced-line metadata does not become tactical proof
when the PV branch itself cannot be replayed from the current FEN. Line-only
tactical proof follows the same boundary: candidate SAN text with capture/check
glyphs cannot stand in for a replayed engine PV. Exchange-forcing line evidence
requires a replay-backed exchange packet with branch and continuation/structure
witnesses; SAN or prose exchange words do not prove the line.
Neutralize-key-break exact readiness also requires a replayed best-defense
branch key; raw PV move text cannot satisfy the branch witness.
Break-prevention route proof cannot be built from an invalid played+PV splice:
candidate route lines must replay legally from the current FEN. Central-break
best-branch and PV transition terms, quiet-move best-defense metadata, and
L3 threat attack-square/capture evidence follow the same board-replay rule.
Structure route-contribution prose is support text, but it is still
board-bound: stale UCI coordinates cannot claim that the reviewed move started
or connected a deployment route unless that move legally replays.
ResourceRemoval owner truth cannot come from `citationLine`, `whyNot`, or
rendered resource phrases; it requires a replayed branch touching the denied
square, break file, or resource square. Resource-like resolved-threat or
lost-motif prose is a counterplay-reduction hint, not ResourceRemoval truth,
unless that replayed hit is present.
ExchangeForcing truth follows the same boundary: exchange/trade/simplification
phrases in active delta text are practical hints only unless a legal replayed
exchange witness proves the branch. Line-scoped strategic support may recognize
concrete SAN capture tokens only after the replay-backed exchange packet is
present. Defender-trade truth additionally requires a declared focal or
structural weakness target; the exchanged square and arbitrary defended pieces
cannot stand in as the removed target.
For prophylactic restraint, exact-slice truth is limited to a square/route or
typed denied-resource token derived from runtime resource classification.
English plan labels and generic counterplay phrases do not certify the exact
resource.
Named route-network public truth is limited to the certified
`RouteNetworkBindProof.SurfaceNetwork` tuple selected from legal replay and
probe-backed route evidence. The MoveReview `Route denial` row may state only
that the checked line keeps the entry square closed, removes the named file,
and cuts off the reroute square. Intermediate route chains, heavy-piece blocked
local-bind shells, tactical truth modes, and raw route prose do not certify a
public route-network claim.
Dual-axis bind public truth is limited to a certified
`TwoAxisBindProof.Contract` whose scope is `dual_axis_local`, archetype is
`break_plus_entry`, reinflation risk is `bounded_dual_axis_only`, and whose
contract carries both a measured break axis and an independent entry axis. The
MoveReview `Break and entry` row may state only that checked pair, not broad
local-bind, global squeeze, or "no counterplay" truth. Heavy-piece blocked
local-bind shells, tactical truth modes, uncertified contracts, and raw
hypothesis prose do not certify this row.
The current public dual-axis witness is the queenless late-middlegame fixture
`2r2rk1/pp3pp1/2n1p2p/3p4/1p1P1P2/P1P1PN1P/1P4P1/2R2RK1 w - - 0 24`:
`TwoAxisBindProof` certifies the `...c5` break axis and independent `b4` entry
axis only after direct best defense `f8e8`, branch key `f8e8`,
future-snapshot persistence, and bounded continuation visibility remain true.
IQP inducement public truth is a bounded structure-transition statement. Legal
FEN/PV replay must show that the reviewed move or checked prefix creates a new
opponent isolated central pawn, and the packet must carry
`PlayerFacingExactSliceProof.IqpInducement(targetSquare, lineMoves)` plus the
induced target terms `after_isolated:<square>` and `isolated_pawn:<square>`,
the `central_isolated_pawn` marker, and the checked line moves. The public `IQP target`
row may name only that square, and its public authority may carry only that
same square as the `PracticalPlan` target. Generic IQP wording, source
precedent, an existing isolated pawn without the induced transition, or
transition terms without the typed exact-slice proof are not truth for this row.
`source-karpov-andersson-1975-iqp-inducement` is public only because ply 49
`cxd5` from `bq1rrbk1/3n1pp1/pp2pn1p/3p4/2P1P3/P1N1BP2/1P1NBQPP/2RR3K w - - 0 25`
legally reaches the induced `isolated_pawn:d5` line under the matching typed
`IqpInducement` packet and contract.
Simplification-window public truth is limited to a typed replayed exchange-square
continuation. Legal branch/persistence truth must already satisfy the existing
`simplification_window` packet contract, and the public row also requires a
matching `PlayerFacingExactSliceProof.SimplificationWindow(exchangeSquare)` plus
an `exchange_square:<square>` term from that checked continuation. Generic
favorable-exchange wording, source precedent, and "same local edge" prose are
not truth for the public `Simplification` row without that exchange-square
witness. `source-botvinnik-vidmar-1936-simplification-window` is public only
because ply 31 `Nxd5` carries that exact replayed `exchange_square:d5` witness;
Salov-Ljubojevic remains suppressed without owner proof. The row may carry only
that same exchange square as the `PracticalPlan` target.
Carlsbad fixed-target truth is mirror-slice based: White-side pressure targets
Black `c6` only when the board has the `c6`/`d5` enemy chain, a friendly `d4`
pawn, and a friendly minority pawn on `b2`, `b4`, or `b5`; Black-side pressure
targets White `c3` only when the mirrored `c3`/`d4` enemy chain, a friendly `d5`
pawn, and a friendly minority pawn on `b7`, `b5`, or `b4` are present. Both
sides still require minority-attack semantic consequence and typed
`CarlsbadFixedTarget` proof. Public MoveReview row and position-probe main-claim
wording may call that exact slice a bounded `Minority attack` target /
minority-attack fixed target only when the typed proof carries
`minoritySupport = true`; generic minority-attack semantics remain support-only
and do not open the standalone `minority_attack_fixation` proof family.
Lower-authority MoveReview practical rows may still name the current board as a
Carlsbad-type pawn shape for structural-only Carlsbad/minority-attack or
backward-pawn-targeting plans, or for a structural-only plan whose exact
`fixed_target:<c6|c3>` hint matches the same board predicate. That is
educational structure context only. It does not carry a target authority, does
not say the checked line keeps or proves the target, and does not relax the typed
`CarlsbadFixedTarget` boundary above.
MoveReview position-probe rows consume the same exact-slice truth boundary:
`ExactTargetFixation`, `CarlsbadFixedTarget`, `TargetFocusedCoordination`, and
`ColorComplexSqueeze` rows require a PositionLocal packet admitted by
`decidePositionProbe` plus the matching typed proof case. They cannot be minted
from focus-square prose, route text, coordination terms, or MoveLocal target
packets. The typed target must also be mirrored by the packet witness terms:
`ExactTargetFixation` and `CarlsbadFixedTarget` require
`fixed_target:<square>`, while `TargetFocusedCoordination` requires
`coordinated_target:<square>`. Exact fixed-target and Carlsbad rows may expose
only that same typed square as practical-plan target metadata.
The current public target-coordination witnesses are the K09 fixtures:
`K09A-certified-coordination` from
`r2qr1k1/pp2bpp1/2n1bn1p/3p4/3N4/2N1B1P1/PP2PPBP/2RQ1RK1 w - - 4 13`
with checked PV prefix `d1b3 d8d7 f1d1`, and
`K09D-certified-coordination` from
`1r1q1rk1/pp3ppp/2n2n2/3p4/3P2b1/2N2N2/PP2BPPP/2RQ1RK1 w - - 3 13`
with checked PV prefix `h2h3 g4f3 e2f3`. They are public only as
`PositionLocal`/`CertifiedOwner` rows after the typed
`PlayerFacingExactSliceProof.TargetFocusedCoordination("c6", supportFromSquares, targetPieces)`
matches the packet source/family and mirrored `coordinated_target:c6` terms.
The MoveReview `Target coordination` row may carry only that same coordinated
target square as its `PracticalPlan` target.
Opening-route target-fixation truth is board/PV based. The reviewed UCI/PV
replay must legally satisfy an `OpeningRouteCatalog` row through
`PieceRouteEvidence`; `KnightRouteEvidence` is only the knight-route
compatibility facade. Route replay uses raw
`VariationLine.moves` before stale parsed metadata and carries the replayed
terminal `Position`; that terminal board must satisfy the row's `target_mode`
through `OpeningRouteTargetEvidence.checkRouteEvidence`.
`attack_weak_pawn` requires role-correct attack geometry from the route
destination to an enemy weak/fixed pawn target; `occupy_target` requires the
route role to occupy the target square. `PlayerFacingTruthModePolicy.findRouteWitness` scans catalog
rows, but if the slice declares a focal, directional, idea-focus, move-ref,
semantic structural-weakness, or root-board weak-pawn target, the selected route
target must match that declared target. If `openingData.name` or an
`OpeningEvent.Intro` name resolves to a family, the route must also belong to
that same `OpeningFamilyCatalog` family and target allowlist; positions without
an opening family label keep the legal-replay and target gates. Benoni `d6`, reversed Benoni `d3`, and King's Indian `c5` use
the same truth path. The route alone is not a truth claim without the slice
board witness. Starter routes for
Caro-Kann, French, Open Games, Sicilian, Queen's
Gambit, Slav/Semi-Slav, Nimzo-Indian, King's Indian, Gruenfeld, English, Reti,
Dutch, Scandinavian, Pirc/Austrian, Alekhine, Nimzowitsch, Catalan, London,
Bird, Queen's Indian, Bogo-Indian, and King's Gambit are data coverage
for legal replay support, not automatic public truth promotion. Queen's Indian
and Bogo-Indian direct `Nf6-e4` descriptors are still route support, not owner
truth. The current route catalog contains 52 descriptors, including 4 bishop
fianchetto descriptors that may enter exact target-fixation only through the
same legal replay, family/target allowlist, declared-target, and terminal
target-mode witness gates. The mined additions
come from master-backed opening rows with at least five row witnesses, while
lower-support route candidates stay deferred. Each route target is also present
in the matching opening-family target allowlist, but that allowlist only permits
bounded target metadata after legal route evidence and board target-mode
validation. Likewise, the `openings.tsv`
static book pool and player target projection samples expand resolver lookup
coverage only; removed broad-variation Scala fixtures are not coverage
authority, and FEN substrings or fixed branch-key text are not truth.
The current opening pool is pruned to 1276 rows that replay against captured
Lichess masters evidence as `master-backed`; 438 live-audited
`not-found-in-masters` expansion rows were removed. This proves master DB
occurrence for those endpoints, not strategic truth beyond the label-plus-FEN
resolver boundary.
Opening pool rows are not master-game truth unless the tooling-only
`OpeningMasterDbAuditRunner` has live OAuth-backed masters evidence for the
normalized endpoint or play sequence from its configured masters endpoint
(`--base-url`, defaulting to Lichess; `--since`/`--until` only when that endpoint
accepts date windows), or a replayable JSONL cache produced from that same
masters response.
`book-canonical`, `unverified`,
`not-found-in-masters`, `transposition-duplicate`, `master-fetch-error`, and
`quarantine` rows can support lookup cleanup but must not be cited as actual
master-game occurrence.
Opening-goal prose support is separate from truth signoff. `OpeningGoals`
evaluations such as `GruenfeldCenterChallenge`, `SlavFreeingBreak`,
`DutchE4Outpost`, `QueensIndianE4Outpost`, `BogoIndianE4Outpost`,
`CatalanTensionRelease`, `OpenCatalanPawnRecovery`, `SicilianC5Challenge`, and
`KingsGambitF4Break` are admitted from the parsed post-move board pattern plus
available engine score, then consumed through `openingGoalEvaluation`; they
are gated to opening phase/event context or the early opening-data window, so
opening labels alone cannot keep producing goal prose in later
middlegame/endgame slices. They are not `CertifiedOwner`, target-fixation
truth, exact central-break timing truth, or a replacement for the label-plus-FEN
opening-family resolver. Achieved or partial opening-break goals may surface
only as lower-authority `Opening break` practical rows with `PracticalPlan`
authority, no token, a matching selected `OpeningGoals` trigger, a legal top-PV
played-move witness, and the same tactical-truth veto used for practical central
rows. Achieved Dutch, Queen's Indian, and Bogo-Indian e4 outpost goals may
surface only as a separate lower-authority `Opening outpost` practical row when
the goal evidence names the outpost square and the reviewed UCI move legally
replays as a knight move to that square, with the legal top PV starting from
that same reviewed move; they do not mint central-break, opening-family, target,
or lesson truth.

General outpost occupation truth is a separate exact-slice path, not an opening
goal shortcut. `OutpostEntrenchment` can surface only as supported-local
MoveReview `Knight outpost` support when the packet carries
`PlayerFacingExactSliceProof.OutpostOccupation("knight", square)`, the reviewed
UCI move legally replays from the current FEN as the moving side's knight move
to that square, the legal top PV starts with that reviewed move, and
`Fact.Outpost(square, Knight, Now)` is present in current/main-PV facts. The
same square, `piece:knight`, and `outpost_occupation:knight:<square>` terms
must be mirrored in packet witnesses, and release still needs proven same
branch plus stable persistence. Broad outpost prose, semantic tags, route access
evidence, and opening-goal evidence remain support-only for this exact-slice
truth boundary.
The current public outpost witness is
`6k1/8/8/8/3P4/5N2/8/6K1 w - - 0 1`, played `f3e5`, checked top-PV line
`f3e5 g8f8 d4d5 f8e8`, and current `Fact.Outpost(e5, Knight, Now)`. Its packet source and
family are both `outpost_entrenchment`, and its exact proof is
`PlayerFacingExactSliceProof.OutpostOccupation("knight", "e5")`; this is only
`SupportedLocal` practical-plan truth, not broad outpost or piece-improvement
authority. The row may carry only that same outpost square as the
`PracticalPlan` target.
Opening-relation planner ownership also cannot rest on
a self-only `OpeningReference.sampleGames` replay; replay support needs at least
two total/sample games and an opening event or the early opening-data window.
Generic exact target-fixation witnesses must also bind the chosen square back
to the same target-fixing idea. If an idea lists several focus squares, runtime
may not select the only current `WeaknessTargetProfile` square unless the idea
id or typed evidence refs name that exact target; broad focus lists remain
support-only and fail closed.

Break/file-axis truth must come from structured square or file-marker evidence
parsed by the shared runtime boundary, not from incidental letters inside
English prose or underscore-separated plan ids. Heavy-piece release truth must
come from exact branch replay: queen centralization, one check, an unrecaptured
rook capture, or a back-rank rook shuffle is not enough to refute a local bind.
Heavy-piece file/entry persistence truth must not be parsed from English
denial phrases; it requires typed route-validation purpose, exact counterplay
resolution, exact denied file removal, and exact denied-square removal signals.
Defender-trade truth must not fall back to the exchange square as its target:
the removed piece must have defended a separate board square before the legal
exchange, and that defense relation must disappear after the recapture while
the target's defender count is strictly reduced. If the exchange merely swaps
one defender for a newly opened replacement defender, exact defender-trade truth
remains closed and the replayed exchange can only support lower-authority
exchange/material wording.
Bad-piece-liquidation truth must not classify a bishop from central pawn counts
alone; the current board must also show low bishop mobility or a central
own-pawn diagonal blocker before the legal liquidation branch can own the claim.
Forced-line trap truth may not release a named trap from a legal move string
alone. A named opening trap requires the reviewed move to be the trap entry and
a coupled PV/variation line to confirm the remaining sequence before the line is
replayed. Non-mating trap lines must also show a material-balance gain for the
side that makes the final material-winning move in the verified line.

Greek Gift truth must be board-backed by the reviewed move: the move must
actually capture on `h7`/`h2` with a bishop, the captured square must have held
the opponent pawn before the move, the resulting position must be check, and
kingside support geometry must be visible either immediately or after legal PV
replay from the post-sacrifice position. This allows the standard timing where
`Bxh7+` is followed by `...Kxh7 Ng5+` and then `Qh5`, while still rejecting a
queen, rook, or knight check on the same square as Greek Gift truth.
Named mate and tactical pattern labels are detected by ordered
`TacticalPatternDetector` implementations. The polymorphic shape is an
extension boundary only; each detector still has to read the parsed before/after
board state and may not infer truth from motif text.

Color-complex squeeze truth is exact-board only. The live proof source is
`color_complex_squeeze_probe`, and release requires a parsed FEN, an
opponent-owned semantic weak square, matching color-complex/hole/fianchetto
evidence, and a friendly bishop or knight that actually attacks that square on
the board. Coordinate and minor-piece words in packet terms are trace labels
only; they cannot certify truth. Final release still requires proven
same-branch state and stable persistence.
The MoveReview `Color complex` summary row is a consumer of that exact proof,
not a separate truth source: it requires position-probe admission plus a typed
`PlayerFacingExactSliceProof.ColorComplexSqueeze` matching the packet
source/family. The typed proof must also be internally coherent: the declared
light/dark complex must match the target square, and the named bishop/knight
geometry must attack that target. The packet witness terms must mirror the same
weak square, color-complex token, role-specific
`minor_piece:<role>_<square>` term, and minor-piece attack pair, so a bare typed
proof case cannot be spliced onto unrelated packet evidence. Term-only,
malformed, term-mismatched, or tactical-vetoed packets remain non-public.
The current public source witnesses for this family are
`source-botvinnik-vidmar-1936-flank-clamp`: ply 25 `Ne5` from
`r2q1rk1/pp1bbppp/4pn2/3n2B1/3P4/1BNQ1N2/PP3PPP/R4RK1 w - - 5 13`, and
`source-botvinnik-vidmar-1936-e4-color-complex-squeeze`: ply 30 `...Bxd5`
from
`r2q1rk1/pp2bppp/2b1pn2/4N1B1/1n1P4/1BN4Q/PP3PPP/3R1RK1 b - - 10 15`,
`source-camara-bazan-1960-d5-color-complex-squeeze`: ply 27 `Bb5` from
`1rbqr1k1/pp1n1pbp/3p2p1/2pP4/1n2PP2/2NB3P/PP2N1P1/R1BQ1R1K w - - 3 14`,
and `source-pfleger-maalouf-1961-d5-color-complex-squeeze`: ply 33 `a5` from
`r2qr1k1/1p3pb1/pn1p1npp/2pP4/P3P3/2NQ1N2/1P1B1PPP/R3R1K1 w - - 0 17`.
They are public only because legal replay, top-PV identity, packet
source/family, and `PlayerFacingExactSliceProofFacts.matchesPacket` all agree.
The public MoveReview `Color complex` row may carry only the same typed weak
square as `PracticalPlan.target`; broader color-complex prose, source-window
labels, or approximate row names do not create target authority.

Local-file entry truth is exact file/entry-pair only. The live proof source is
`local_file_entry_bind` under `half_open_file_pressure`; release requires the
typed `PlayerFacingExactSliceProof.LocalFileEntryBind(file, entrySquare)` to
match the packet source/family and the same owner/continuation/branch/stable
persistence witnesses used by other exact move-delta packets. The packet witness
terms must also carry the same file/entry pair as a single
`file-entry:<file>:<square>` or `file_entry:<file>:<square>` witness term. Split
file and entry terms under the `local_file_entry_bind` marker are not public
release proof. The entry square must be on the claimed file. Public release
also requires context-aware board support from the current FEN and legal
reviewed-move replay: a major piece must be on the claimed file, and the named
entry square must be occupied or controlled by the moving side on the before or
after board. The FEN-less pair extractor is not truth authority. The MoveReview
`File entry` summary row consumes the certified proof through supported
move-delta admission and may only state pressure through the named file on a
same-file entry square. It cannot certify broader heavy-piece bind, global
squeeze, or no-counterplay truth. The row may expose only that same typed entry
square as practical-plan target metadata.
The current planner witness is the `prophylactic_cut` local-file scene:
from `2r2rk1/pp3pp1/2n1p2p/3p4/3P1P2/2P1PN1P/PP4P1/2R2RK1 w - - 0 23`,
played `a2a3` certifies only the `c-file`/`b4` pair through
`LocalFileEntryProof.certifiedSurfacePair(ctx, ...)` after board support from
the current FEN/replayed move. Because `b4` is not on the `c-file`, that pair is
support-only for planner wording and does not become a public `File entry`
MoveReview row under the typed `LocalFileEntryBind` proof. `OpenFilePressure`,
`KeySquareDenial`, and `RookFileTransfer` may consume only a same-file exact
pair through the shared `local_file_entry_bind` / `half_open_file_pressure`
contract.

Counterplay-restraint truth is named-resource only. The live exact proof is
`PlayerFacingExactSliceProof.ProphylacticRestraint`, and release requires the
`prophylactic_move` / `counterplay_restraint` packet path plus the same
owner/continuation/branch/stable persistence checks used by exact move-delta
packets. The proof token must be a board/resource token such as a square, route,
or `denied_resource:<class>`; free-form labels are not truth. The packet witness
terms must mirror that typed resource token before a surface row can consume the
proof. The MoveReview `Counterplay restraint` row consumes only that admitted
packet and may state that the named resource is restrained, not that all
counterplay is gone.

Favorable-exchange truth is replay-bound. `MoveReviewExchangeAnalyzer` is the
shared board engine for `defender_trade`, `bad_piece_liquidation`,
`queen_trade_shield`, and bounded simplification exchange-square checks: every
owner witness must come from legal FEN/PV replay, not UCI string geometry.
For defender trade and bad-piece liquidation, the truth unit is the legal
proof-sized prefix, not the entire displayed PV tail. A stale tail after the
three-ply defender-trade exchange or after the legal bishop capture/recapture
cannot add proof, but it also does not suppress the already proven local
witness. If the required prefix itself fails to replay, the owner path remains
closed.
Defender-trade truth cannot fall back to the exchange square as target; it must
show a real defended target whose defender relation is removed and whose
defender count is reduced. Replacement-defender geometry is not exact
defender-trade truth even though the captured piece no longer defends the
target. Bad-piece liquidation truth cannot come from same-color central pawn
counts alone; it must show a constrained bishop and a legal branch where that
bishop is traded away after entering the line. Queen-trade shield truth requires
a queen capture and king recapture on the replayed board; same-destination move
strings are not truth. Replay-backed favorable-exchange branch keys are
witness-line facts, not raw PV display strings. Semantic observations from the
same analyzer are selector evidence only unless the proof contract admits the
owner packet, and they require the reviewed played move identity rather than
inferring it from the engine top line.
The MoveReview `Defender trade`, `Bad piece trade`, and `Queen trade` summary
rows consume those admitted packets only. Defender/bad-piece rows require the
structure-transition witness, proven same-branch state, and stable persistence
to satisfy the proof contract, plus matching typed exact-slice proof and
analyzer branch fact terms
(`defender_trade_branch` with defender, exchange,
and defended-target terms; `bad_piece_liquidation_branch` with bad-piece and
exchange terms). Policy-built defender/bad-piece packets additionally carry
typed exact-slice proof payloads (`DefenderTrade` or `BadPieceLiquidation`),
and the row builder validates those payloads against packet source/family and
witness terms before using their squares. Packets without the matching typed
exact proof fail supported-local exchange-ownership admission and do not project
to public rows. The current public bad-piece source witness is
`source-capablanca-golombek-1939-bad-piece-liquidation`: ply 43 `Bxd6` from
`r2qr1k1/pp3pn1/2pb2pp/3pB3/NP1P4/3QP2P/P4PP1/1RR3K1 w - - 1 22` is public
only because the legal top-PV prefix `e5d6 d8d6` matches the typed
`BadPieceLiquidation` packet and its contract.
public exchange-ownership rows; exact-proof/term mismatches do not fall back to
generic exchange ownership. Queen-trade rows require the typed
`PlayerFacingExactSliceProof.QueenTradeShield` to match the packet and mirror
every typed line move in packet witness terms; that proof may come from the
legal proof-sized PV prefix, not from a full-tail replay requirement. These rows
cannot certify truth from raw exchange labels, generic branch labels,
term-mismatched packet evidence, malformed square-prefixed branch terms, or
tactical truth-mode packets. Defender/bad-piece row wording may repeat only the
parsed exchange, defender/target, and bad-piece squares from the typed proof
terms.
The current public queen-trade source witness is
`source-carlsen-anand-2014-g6-queen-trade-completion`: ply 17 `Qxd8+` from
`r1bqk2r/1p3ppp/p1p1pn2/8/1bP1P3/2NQ4/PP3PPP/R1B1KB1R w KQkq - 0 9` is
`SupportedLocal` only because the legal prefix `d3d8 e8d8` proves the queen
capture plus king recapture and the packet source/family is `queen_trade_shield`.
The shared `RelationWitness` read-model is not a new truth shortcut. The
implemented relation observations inventoried in `CommentaryPipelineSSOT.md`
may support selector ranking or practical prose only after legal replay over
the current FEN proves the local attack/defense relation. Semantic relation
producers share a context-owned relation-witness list assembled from the
six-ply standard top-PV replay plus the dedicated twelve-ply/probe-PV
draw-resource exception, so relation support rows are not produced from
divergent per-producer replay, duplicate draw-resource projection, or local
kind-filtering attempts. They do not certify owner
truth without a separate proof contract. When an explicit target set is
supplied, the relation witness must prove that target and may not fall back to
an arbitrary material piece; discovered-attack witnesses follow the same target
binding. Double-check support requires a replayed reviewed move that leaves the
opposing king checked by at least two checker squares, and explicit target
lists close this king-targeted relation instead of reopening material fallback.
Back-rank mate support reuses the existing tactical pattern detector and
requires the replayed reviewed move to end in `checkMate` with the defender's
king still on the back rank; explicit target lists close this king-targeted
relation instead of reopening material fallback.
Mate-net support reuses the existing mate-required tactical pattern detectors
other than back-rank mate, requires the replayed reviewed move to end in
`checkMate`, and keeps the detector id as support-only evidence; explicit
target lists close this king-targeted relation instead of reopening material
fallback.
Greek-gift support reuses the existing tactical pattern detector, requires a
legal reviewed bishop sacrifice on h7/h2 that gives check, and accepts only
immediate kingside support or support that appears by replaying the supplied PV
continuation. Explicit target lists close this pattern relation instead of
reopening material fallback.
Fork support requires a replayed move attacking at least two bound targets.
Hanging-piece support requires the replayed mover to attack an
undefended bound target. Pin and skewer support also stay on this boundary: a long-range
replayed move must create actual ray geometry through the pinned/front piece and
the piece behind it. Invalid explicit target tokens close the relation witness instead of
triggering structural or material fallback. `RelationObservationCatalog` is a routing catalog,
not a truth source: it can map implemented relation witnesses into
semantic/source/idea metadata, bounded public relation labels, and relation
producer admission. The runtime inventory keeps a non-public deferred list for
motif families that are present in adjacent model or detector assets but lack a
board-replayed relation witness; that list is currently empty. Deferred entries,
when present, are not cataloged selector rows, not relation producer inputs, and
not frontend authority tokens. Fallback projection is not truth admission.
`stalemate_trap` is a public relation only through its
analyzer-owned PV witness: the played move must start the bounded legal replay,
the replay must end in actual stalemate, and the engine score must stay
draw-stable with no mate score. `perpetual_check` is a public relation only
through its analyzer-owned PV witness: the played move must start as a legal
check, the line must contain at least three checks by the same side, a
repetition-compatible position key must return inside the replay, and the engine
score must stay draw-stable with no mate score. Raw draw-resource motif text,
detector labels, unvalidated probe lines, unbound probe candidate moves, and
helper notation remain PV-only support and are not truth admission. A validated
root `ProbeResult` reply PV can feed this draw-resource witness only when its
FEN and probed/candidate move bind to the reviewed position and played move. The
top-PV version of those draw-resource witnesses may also project bounded
MoveReview summary support rows (`Stalemate resource` or `Perpetual check`);
validated probe reply PVs remain relation metadata unless another product path
consumes them. The
`trapped_piece` relation is public only through its analyzer-owned board
witness: the reviewed move must legally replay, the moved attacker must attack a
valid explicit target, the target must be a higher-value enemy non-pawn/non-king
piece, and the target must have no legal escape that preserves the piece outside
attack by the pressure side. Generic trapped-piece activity, raw motif text, and helper
notation remain below truth admission.
For these implemented relation descriptors whose raw motif tags are
witness-only, the analyzer projection boundary also requires typed
`RelationDetails`; raw focus/fact strings on a `RelationWitness` cannot become
semantic relation evidence or a player-facing relation row.
The `domination` relation is public only through an analyzer-owned board witness:
the reviewed move must legally replay, the moved attacker must attack a valid
explicit same/lower-value target, every pseudo-escape square must be controlled
by the pressure side, the target must have no legal safe escape, and ray
pin/skewer geometry remains owned by line-relation witnesses. Raw domination
motifs, helper notation, and generic restriction prose are not truth admission.
The supported-local `Trapped piece`, `Domination`, and `Zwischenzug` rows are
only surface labels over those existing relation witnesses; they do not admit a
generic mobility-cage or move-order claim. Generic advanced relation metadata
may still describe the same relation family, but a practical supported-local
row for the same relation kind suppresses that duplicate advanced row.
The `zwischenzug` relation is public only through an analyzer-owned board
witness: the reviewed move must legally replay as the first move, explicit
targets must parse to a non-empty expected recapture square set, the side to
move must have had a legal non-pawn capture on that square before the reviewed
move, the reviewed move must not be that capture, and the moved piece must give
direct check. Broader `MoveAnalyzer.detectZwischenzug` motif output and raw
`Zwischenzug(...)` helper notation are not truth admission.
Mate-net practical labels are also only surface labels over analyzer-owned
relation witnesses. A `Smothered mate`, `Arabian mate`, `Boden's mate`,
`Anastasia's mate`, `Hook mate`, or `Corner mate` row requires the same legal
top-PV `MateNet` witness and the matching `TacticalPatternDetectors` id in the
typed mate-pattern details; raw detector vocabulary or motif notation is not
truth admission, and the label does not create a new proof family.
Discovered-attack practical rows follow the same analyzer-owned boundary: the
reviewed move must be the legal first top-PV move, the typed witness must name
the long-range attacker, cleared square, and newly attacked target, and the row
does not create owner proof.
Defender-trade and bad-piece trade practical rows also follow that
analyzer-owned boundary. A defender-trade fallback row needs the legal top-PV
capture/capture/recapture branch, a declared focal or current weakness-profile
target that is not the exchange square, and typed proof that the defender count
for that target is strictly reduced after the recapture. A bad-piece trade
fallback row needs the legal top-PV bad-bishop liquidation branch and the typed
bad-piece details. A queen-trade fallback row needs the legal first two top-PV
plies to be the reviewed queen capture and the opponent king's recapture on
that same square. These rows are lower-authority `PracticalPlan` support; they
do not certify exact owner truth for the exchange family.
The legacy motif-prefix table, theme-keyword path, canonical motif-term path, and
motif appears/fades delta prose use the same catalog boundary, emitting
only softer non-relation practical/thematic text for deferred lanes when present
or suppressing witness-only raw relation tags such as `trapped_piece`,
`domination`, `zwischenzug`, `stalemate_trap`, and `perpetual_check`, so
deferred relation names and witness-only raw relation tags cannot become
truth claims through prose or `conceptSummary` aliasing.
Legacy plan evidence still falls back to `key-square restriction` for raw
domination-style material instead of naming `domination` as a plan proof term.
That fallback label is catalog-owned through the implemented descriptor's
witness-only fallback field. User-facing helper-notation cleanup follows the
same boundary: deferred practical/thematic helpers are rewritten to fallback
wording when present, `Zwischenzug(...)` is rewritten to the descriptor's
`move-order caution` fallback, and PV-only `StalemateTrap(...)` and
`PerpetualCheck(...)` helpers plus mate-pattern-only `SmotheredMate(...)` are
suppressed rather than softened into named public relation labels.
Threat-summary labels follow that catalog boundary as well: deferred relation
motifs from `ThreatAnalysis` may only surface as softer fallback wording when
available, not as raw relation names. PV-only draw-resource motif tags such as
`stalemate_trap` and `perpetual_check` do not get raw public threat-summary
labels.
Candidate tactic evidence uses the same catalog-owned witness-only fallback
labels for raw `Domination` and `TrappedPiece` motifs; those annotations are
support prose only and do not create relation truth.
Strategy-pack and structure-arc piece-activity evidence does not convert generic
trapped-piece activity into a relation fallback term;
raw `trapped_piece` relation labels require the board-replayed relation witness.
Cached or legacy strategy-pack support metadata and move-review ledger
prerequisites receive the same sanitizer treatment: deferred relation motif
terms, stale `deferred_*` fallback tokens, and witness-only raw relation tags
are stripped from evidence lists, long-term focus, side-plan priorities and risk
triggers, and directional target reasons and prerequisites, while ordinary
non-deferred support evidence can remain lower authority.
Deferred relation tags and witness-only raw relation tags also cannot raise
generic context prose into a high-tension tactical opening frame without an
implemented relation witness, non-deferred motif, or actual threat evidence, and
they are not generic motif-prefix signals for future consumers. Generic fact
corroboration follows the same boundary, so an ordinary
tactical fact cannot certify a witness-only raw relation tag as board-proven motif
support. The witness-only flag and any short fallback label used by threat
summaries are descriptor-owned catalog data; consumers must not maintain their
own relation label lists. The
catalog inventory can name both implemented and deferred motif families, but
truth admission is still descriptor-based and descriptor lookup resolves only
implemented board-replayed relations. The selector evidence carrier mirrors
that rule by preserving relation identity/focus only for implemented catalog
kinds; unknown or uncataloged names cannot survive into a relation candidate.
Unknown relation witnesses do not become target-pressure or other selector
evidence; missing catalog entries fail closed.
The relation semantic producer consumes the implemented catalog relation set, so
producer reachability follows the routing catalog.
`StrategyPackBuilder` may preserve a relation-only selected idea so the support
metadata reaches the MoveReview surface, but that carrier does not promote the
relation into proof/source/family truth.
When a supported-local practical row already exposes the same relation kind,
`moveReviewPlayerSurface` treats that practical row as the concrete display and
does not duplicate it with a generic strategic-relation advanced row. Exact
defender-trade and bad-piece-liquidation owner packets may expose that relation
metadata on the summary row after resolver admission and exact-slice packet
matching. This is a surface precedence rule only; it does not alter relation
truth, owner proof, or catalog admission.
Cached MoveReview payloads pass through the same backend sanitizer rule after
summary-row sanitization: only practical-plan authority or exact defender-trade
/ bad-piece-liquidation summary `strategic_relation` authority with a matching
label, catalog token, and square target can suppress a matching advanced
`strategic_relation` token, so stale unauthoritative labels do not erase valid
relation metadata.
Move-local defender-trade and bad-piece owner witnesses reuse the analyzer
`RelationWitness` typed details and fact vocabulary for branch, exchange, target,
and PV terms; policy-local extras can add owner transition wording but cannot
replace the board-replayed relation facts. The analyzer owns decoding of those
details back into branch fields and expanding shared owner/transition terms
through `relationProjectionFromWitness`; policy code does not parse raw detail
keys or read relation facts/focus squares directly for these owner packets. A
typed-detail mismatch emits no owner-seed or transition terms.
Branch-key construction and `branch:*` fact projection are also analyzer-owned;
policy code consumes the helper output from replayed witness line moves instead
of rebuilding branch identity with UCI substring or local pipe-join logic.
Other implemented relation witnesses also carry typed relation details for their
replayed king, attacker, defender or cleared square, mate pattern, material or
line target, lure/execution, and front/back or pinned/behind coordinates. Those
details do not grant owner truth by themselves; they preserve the board witness
so later certified or softer-label consumers do not recreate the relation from
strings.
Every implemented relation support row requires matching typed relation
details. If details are missing, or present but not for the same relation kind,
the relation support row is rejected; raw witness focus, target, or fact fields
are not a projection fallback for implemented relation kinds.
Semantic observation admission consumes the analyzer-owned
`relationProjectionFromWitness` carrier after that check, so downstream
relation rows receive one canonical kind/focus/target/fact projection instead
of reconstructing truth from raw witness strings.
`moveReviewPlayerSurface` may expose those implemented relation rows only as
`strategic_relation` support metadata. The relation token and required target
square are public display metadata, not proof-family/source truth and not a
frontend permission to rebuild omitted relation claims from prose. Projection
requires the catalog source ref, the matching semantic observation fact, the
descriptor-owned analyzer witness fact, and a relation-focus-derived target; a
source-only or source-plus-semantic-only idea remains internal support.
The surface may show up to four implemented relation rows, but row count is not
truth evidence; each row still needs its own catalog source/semantic/witness
admission triple, relation-specific focus, and bounded analyzer witness.
The row sentence may describe draw resources, move-order turns, key-square
restriction, line geometry, or a tactical motif, but that wording is not a new
truth source and cannot widen the catalog token, focus, or target.
`RelationObservationCatalog` owns that source/semantic/witness admission helper and the
selected-`relationKind` no-fallback rule, so downstream builders do not repeat
or widen relation matching. It also owns public target fallback for selected
relations that lack an analyzer target, so surface projection does not recreate
motif-specific target order from raw relation strings or promote a generic
`targetSquare` into relation target. Each relation descriptor exposes its own source ref,
semantic fact, and required witness fact, keeping selector prioritization, surface priority, and
public projection on the same catalog admission tokens. The catalog may order
PV-backed draw-resource relations ahead of generic relation rows inside the
display cap so stalemate-trap and perpetual-check support is not hidden, but
this ordering is not truth signoff and does not bypass the legal top-PV replay
or draw-stability gates. Practical fallback replay uses the normalized reviewed
UCI and the already-certified first `BoundedReplayStep`; raw `playedMove` text
is not reparsed as a stricter or looser truth source after the replay match.
Standard relation witnesses remain bounded to the
six-ply relation replay prefix; the only deeper relation replay exception is
the twelve-ply draw-resource prefix for stalemate-trap and perpetual-check
witnesses.
Relation row prose selection is also descriptor-typed: the row kind, not the
display label, chooses draw-resource, move-order, mobility-restriction,
line-geometry, or tactical-relation wording. Descriptor-specific relation rows
may name only the analyzer-projected focus-order squares already carried by the
selected relation witness; that display detail does not recover piece roles,
branch ownership, or conversion truth from raw strings. Defender-trade or
bad-piece wording in this row remains support-only; exact owner proof still
requires the dedicated typed owner packet.
Line-geometry relation rows may likewise name focus-order squares as
role-neutral geometry only; pin/skewer rows do not claim absolute pins,
material wins, axes, or piece roles unless another typed public contract admits
that stronger claim.
Relation witness support facts cannot manufacture catalog source or semantic admission tokens:
dynamic facts reject semantic observation ids, evidence-source ids,
proof-source ids, proof-family ids, and `source:` wire keys. Only the catalog
descriptor may provide the relation source ref, matching semantic fact, and
required witness fact.
When the analyzer supplies a relation target, that square is carried through
semantic evidence and selected idea metadata to the surface row only while it
remains bound to the relation focus squares; descriptor target fallback may use
that same relation-focus ordering, but generic idea focus and legacy
`targetSquare` metadata are not truth evidence.
When multiple relation observations merge into one selected idea, the selector
keeps a representative `relationKind`; the surface row uses that kind only with
its matching catalog source, semantic fact, and required witness fact, so
catalog ordering cannot invent a different public relation. The selector also preserves relation-specific focus
squares for that representative relation, so public relation prose does not use
focus squares borrowed from another merged relation witness. A selected relation
kind without relation-specific focus remains unprojected instead of borrowing
merged idea focus. Selector merge does not synthesize relation focus from
generic `focusSquares` or relation target from generic `targetSquare`; missing
relation-focus stays missing, while missing relation-target evidence may use
catalog fallback over the selected relation's own focus at the public surface.
It also prioritizes the representative relation's source ref, semantic fact, and
required witness fact before evidence truncation, keeping the catalog admission
triple intact while other support refs remain lower priority.
The same selected-`relationKind`, catalog-evidence, raw-carrier stripping, and
target-required lane rules apply at backend sanitization and cached/legacy
frontend decoding; the summary-lane exception is limited to exact defender-trade
and bad-piece-liquidation owner rows, so frontend code neither widens nor
narrows backend relation authority.
Move-local owner witnesses follow that same rule: defender trade, bad-piece
liquidation, and IQP inducement fail closed when the reviewed played move is
missing, even if the top engine line legally replays.
Raw carrier prose does not identify the defender-trade family: text mentioning
defenders, guards, trades, or removal is not truth for the exact owner path
without typed family context and the replayed defender-target relation.
Generic plan-owner fallback is subject to the same boundary: exact
defender-trade, bad-piece-liquidation, and queen-trade-shield families are
stripped unless their board-replayed witness exists, so a broad plan label
cannot re-promote after the exact witness fails closed.

Resolved compensation display may appear in `moveReviewPlayerSurface` only as
bounded `practical_plan` support. The truth object is the resolved compensation
display contract from `StrategyPackSurface` plus
`CompensationDisplayPhrasing`'s strong-anchor eligibility, not raw
`signalDigest.compensation`, the word `initiative`, or prose about material.
Those rows may explain why material can wait and what concrete pressure must
remain only when the strict subtype is durable pressure, but they do not certify
investment ownership, conversion truth, strategic-plan truth, or compensation
after a blunder/failed-truth boundary. The support condition row prefers the
resolved persistence wording over the broader objective fallback. When a
`DecisiveTruthContract` is
present with `compensationProseAllowed=false`, compensation rows stay closed
even if raw strategy-pack data still contains a resolved compensation surface.
Unresolved subtype paths,
transition-only conversion stories, non-durable tactical windows, and generic
compensation shells stay silent on the player surface.
Restricted-defense conversion display may appear in `moveReviewPlayerSurface`
only as bounded `practical_plan` support from a certified
`RestrictedDefenseConversionProof.Contract` admitted by
`ClaimAuthorityResolver.restrictedDefenseConversionSurfaceDecision`. The truth
object is the typed contract's direct best-defense, same-branch persistence,
compressed defender-resource, and continuing suppression facts, not the plan
name, generic conversion prose, or the engine PV alone. This row does not
certify global conversion truth, compensation truth, or investment ownership.
The same bad-truth surface veto applies to MoveReview player support rows:
blunder, missed-win, bad tactical-refutation, or tactical-failure contracts
close strategic relation, practical plan, opening-family, compensation, and
supported-local support rows instead of mixing plan-positive support with a
failure explanation. `DecisiveTruthContract.blocksStrategicSupport` is the
runtime predicate for that boundary and for tactical-failure planner scene
ownership. The MoveReview prose compression basic-explanation lane also honors
that predicate: positive `opening_goal`, `certified_strategy_support`, and
`basic_move_explanation` sources do not surface when the contract blocks
strategic support, so fallback prose remains tactical or exact-board factual.

## No-Go Truth Failures

These are release blockers:

- verified-best played move is called wrong, loose, inaccurate, or blunder-like
- benchmark move is named when it is not verified best
- blunder truth is softened into compensation-positive framing
- compensation-positive prose appears without investment ownership
- maintenance is narrated as the original investment
- conversion is narrated as compensation
- support-only or deferred carriers revive owner claims after certification
  failed closed
- fallback projection reconstructs investment, maintenance, conversion, or
  strategic plan truth from serialized strings
- frontend fallback retry/drop behavior is inferred from commentary prose
  rather than backend `diagnostics.status`
- broad lesson or rule wording appears without local scoped-takeaway authority
- decision comparison is rebuilt from `topEngineMove`, `cpLossVsChosen`, or
  latent/deferred carriers
- line/PV consequence wording is surfaced from engine-only `VariationLine`
  interpretation or failed ref replay instead of a `MoveReviewRefs` line that
  passed legal FEN replay
- strategic plan refutation is inferred from sibling score/ranking rather than
  exact board/probe evidence
- strategic plan truth is inferred from raw `evidenceTier`, compatibility
  markers, or subplan ids instead of typed evaluator evidence plus exact
  witness where required
- local bind truth is rejected from broad heavy-piece movement labels without
  exact replay evidence of a real release
- MoveReview strategic support, probe, authoring, or decision-comparison UI is
  rebuilt from raw `strategyPack`, `signalDigest`, `authorEvidence`,
  `probeRequests`, full `mainStrategicPlans`, or strategic-plan experiment
  metadata instead of the backend-certified `moveReviewPlayerSurface`
- MoveReview player support projects strategic/practical support beside a
  blunder, missed-win, bad tactical-refutation, or tactical-failure truth
  contract
- strategic intent is invented for a low-confidence or tactical-first failure

## Scoped Takeaway Boundary

`MoveReviewScopedTakeaway` is local instruction only. It must be tied to:

- reviewed UCI
- post-move FEN
- same coupled PV branch
- evidence tier
- guardrail tags

It may project into the existing MoveReview learning-point compatibility field,
but it is not broad lesson authority and must not generalize into broad rules.

## Audit Gate

Automatic checks are necessary but not sufficient. Truth signoff requires:

- targeted compile/tests for touched runtime paths
- family-wise truth metrics
- GM-style manual review on the master audit corpus when release signoff is in
  scope
- explicit separation of target slice, aggregate behavior, runtime behavior,
  and test/tooling artifacts

Primary truth metrics:

- verified-best mislabeled count
- wrong benchmark naming count
- blunder softening count
- fake compensation count
- missed commitment count
- maintenance-as-commitment count
- conversion-as-compensation count
- visible exemplar lost count

Health signals such as parity, release-gate flags, provider-none stability, and
cross-surface agreement remain secondary to chess truth.

## Maintenance Triggers

Update this file when any change affects:

- truth/signoff behavior
- fallback truth projection
- claim authority signoff
- benchmark naming
- compensation/investment/conversion ownership
- scoped takeaway or lesson-readiness guards
- frontend/API behavior that could alter user-facing truth
