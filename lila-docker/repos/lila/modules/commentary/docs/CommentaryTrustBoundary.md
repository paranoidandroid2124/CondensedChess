# Commentary Trust Boundary

This file is the canonical trust-risk map for Chesstory commentary. Use it for
false positives, overclaim control, support-only reinflation, fallback rewrites,
surface-consumption risk, and lesson-readiness gating.

It complements `CommentaryPipelineSSOT.md` and `CommentaryTruthBoundary.md`.

## Current State

The live product trust boundary is MoveReview-only. Removed Chronicle/Active
surfaces follow the canonical boundary in `CommentaryPipelineSSOT.md`; runtime
trust/signoff must not depend on Chronicle/Active DTOs, replay carriers, or
fallback semantics. Any legacy replay code must use non-authority carriers such
as `DecisionFrameCarrierInput` and `DecisionFrameDossierInput` directly.

Current operating posture:

- maintain existing exact-board promoted slices
- keep broad strategic expansion closed
- allow new runtime authority only through proof contracts and the
  claim-authority kernel
- keep support-only/deferred/latent carriers internal
- keep broad lesson authority closed

## Authority Ladder

Internal trust decisions use this ladder:

| tier | user-facing meaning | allowed use |
| --- | --- | --- |
| `CertifiedOwner` | exact proof may own the claim | main MoveReview explanation |
| `SupportedLocal` | bounded local evidence may speak | qualified local reading/support |
| `DiagnosticOnly` | useful for review, not release | traces, corpus review, tests |
| `Suppressed` | blocked | no released claim |

`ClaimAuthorityResolver` owns final authority decisions from packet, plan, and
truth contract; planner call sites consume it directly.

The planner may rank and select questions, but it must not decide proof-family,
source, scope, tactical-veto, or fallback authority by itself.

MoveReview causal prose has a separate release gate. A selected
`QuestionPlan.claim` is still planner text until `MoveReviewCausalClaim`
certifies it for rendering. `WhyThis`, `WhyNow`, `WhatChanged`,
`WhatMustBeStopped`, and `WhosePlanIsFaster` require a concrete causal anchor
and typed support evidence before `MoveReviewCompressionPolicy` may project them
into `MoveReviewPolishSlots`. If the gate fails, the selected planner question
remains trace material only and MoveReview must use an existing fallback lane.
Templates and renderers are therefore surface realizers, not causal authority.
The gate distinguishes typed support from typed causal relation. A branch line,
PV citation, or certified coda can support a claim, but it does not authorize a
`WhyThis` role unless the claim also has admissible alternative contrast or a
played-move-owned consequence relation.
For `WhyThis` alternative contrast, the certified contrast sentence owns the
surface claim. A template/planner sentence may not remain the lead claim merely
because the typed contrast appears later as support.
For played-move-owned line consequence, the same surface-ownership rule is
narrower than generic branch support: only a non-`PreviewOnly`
`LineConsequenceEvaluator.surfaceCandidate` from FEN-validated `MoveReviewRefs`
may replace the raw planner claim, and only for `WhyThis` or `WhatChanged`.
Engine-only `ReplayBackedInternal` evidence, preview-only legal prefixes, and
plain branch text remain support/diagnostic material and cannot open this
surface by themselves.
For `WhatChanged`, `change_consequence` is not a coda-only release. It requires
either the same played-move-owned consequence relation or admissible contrast;
branch/coda support without that role authority falls back.
For `WhyNow`, `timing_constraint` is not opened by a generic urgency or tension
sentence. It requires typed timing support: either `QuestionPlanTimingWitness`
or an admissible contrast trace with source kind `explicit_reply_loss`. Generic
urgency text may remain support only after that typed relation is admitted.
For `opening_relation_translator`, `WhyThis` release additionally requires
admissible contrastive support. A branch line, PV citation, or opening-script
sentence cannot by itself justify a causal explanation that compares the played
move with an alternative or a checked continuation.
The release gate is auditable without reading prose. `MoveReviewCompressionPolicy`
projects the decision through `causalClaimTrace`, and tooling copies it into
`MoveReviewPlannerTrace` / corpus `moveReviewCausalClaim*` fields. Those fields
are diagnostic measurements of accepted/rejected authority, relation kinds, and
reject reasons, plus typed evidence source, subject, and line-binding
measurements; they do not create a second surface or allow downstream code to
recover causal authority from fallback wording.
Accepted planner claims also carry `MoveReviewLocalFact` admission. Planner
evidence/relation inputs are submitted to `MoveReviewLocalFact.admitPlanner`,
which owns family, authority, and strict fallback classification. The certified
claim records a `local_fact_family`, `local_fact_authority`, strict flag,
guardrails, and line binding before the renderer receives the surface text. For
support-required questions, absence of that local fact is a release failure
(`local_fact_admission_missing`). Corpus `moveReviewLocalFact*` fields report
planner-owned rows as `emitted` only from this causal claim admission, not from
basic fallback candidates that did not render.
This admission must not be reconstructed from the rendered sentence. A claim
that says `supports the plan`, `keeps pressure`, `target`, or `threat` is still
untrusted unless a typed owner/source opens the matching family. In the current
runtime, line-consequence planner facts come from `LineConsequenceEvaluator`
surface candidates or typed `pv_delta`; generic `QuestionPlanConsequence` text
and claim wording are not family evidence.

Fallback text is inside the same trust boundary. `basic_move_explanation` and
the local surface/takeaway renderers may identify the played move and admitted
local evidence role, but they must not promote PV reply order, target-creation,
forced-reply, timing, plan-support, or PV-verified causal wording without typed
authority. `CommentaryIdeaSurface` now submits `MoveReviewLocalFact` candidates
and attaches admitted `local_fact_family`, `local_fact_authority`, and
strict-fallback eligibility to basic descriptors before prose is rendered.
Candidate inputs can carry source, subject, anchors, line binding, and evidence
refs, but renderers consume only the admitted fact and scoped takeaway.
If the selected high-risk planner claim is rejected by `MoveReviewCausalClaim`,
the basic builder runs in strict local-fact mode: soft line-only and
strategic-plan descriptors fall through to exact factual fallback, target
pressure requires the reviewed move to own the target fact, defensive wording
requires only-move defense truth, and endgame wording requires the played move
to own the endgame fact. Scoped takeaways consume that admitted local fact
metadata, require a purpose-family match and `pv_coupled` line binding, and
fail closed rather than using a line-only compatibility admission, raw opponent
question, reply-role classifier, or tension classifier to invent tactical or
defensive interpretation.
Generic support anchors such as `plan activation lane`, `plan`, or `main plan`
are not player-facing subject authority and are skipped before a fallback
sentence chooses its displayed subject.

The local-fact fallback gate is auditable without prose parsing.
`MoveReviewCoverageDiagnostics` and corpus `moveReviewLocalFact*` fields record
whether a local fact was emitted, remained a candidate, or was strict-rejected,
plus its family, authority, strict flag, and reject reasons. These fields are
diagnostic measurements from the typed causal claim or
`MoveReviewPolishSlots.localFact` carrier; `reasonTags` are compatibility
surface tags and are not a local-fact authority source. These fields do not
create a second renderer or truth owner.

## Plan Promotion Trust Boundary

`PlanEvidenceEvaluator` owns strategic plan promotion. `ProbeBacked`,
`TranspositionAligned`, `StructuralOnly`, `PvCoupledOnly`, `Deferred`, and
`Refuted` are typed evaluation outcomes, not downstream string matches.

Downstream user-facing consumers must read the typed
`StrategicPlanEvidenceView` projected into `NarrativeContext`. Raw
`StrategicPlanExperiment.evidenceTier` strings and the
`probe_backed:validated_support` marker are diagnostic/compatibility carriers,
not release, provenance, quantifier, stability, or outline-selection
authority.
Only typed `ProbeBacked` and `TranspositionAligned` evaluated plans can enter
selected main-plan authority. `TranspositionAligned` is a separate provenance:
it must come from legal PV replay through `TranspositionPvAligner`, a terminal
`WeaknessTargetProfile` match for an expected target square, positive
attacker-minus-defender control, sufficient line horizon, exact
`target:<square>`/`weakness_target:<square>`/`fixed_target:<square>`/
`coordinated_target:<square>`/`target_fixing:<square>`/
`enemy_weak_square:<square>`/`weak_complex:<square>` evidence-token parsing, and
mover-loss/mate veto checks. It must not be rewritten as `ProbeBacked` and does
not satisfy exact proof contracts or `check_qualifying`. `StructuralOnly` and
`PvCoupledOnly` may remain in diagnostics or support context, but they cannot
own a main claim or satisfy `check_qualifying`. Structural-only plans must also
remain structural-only in compatibility carriers; they must not serialize as
`evidence_backed`.
`PlanEvidenceEvaluator.isMainAdmittedPlan` is the shared typed predicate for
selected main-plan admission and downstream payload/sanitizer sanity checks;
`TranspositionAligned` requires both a transposition proof id and
`transposition_aligned` typed provenance. A raw proof id, evidence tier, or
compatibility marker alone is not enough.
The outline strategic-stack context sentence reads
`StrategicPlanEvidenceView.mainAdmittedPlanHypotheses`, while probe-only helper
methods remain reserved for consumers that specifically require probe-backed
proof contracts.
`UserFacingPayloadSanitizer` also treats that marker as non-authoritative:
MoveReview plan payloads are retained only when `CommentaryApi` passes a
matching typed `EvaluatedPlan` whose eligibility is main-admitted and whose
evidence id set is non-empty: support probe ids for `ProbeBacked`, or
transposition proof ids plus `transposition_aligned` provenance for
`TranspositionAligned`. Cached/default sanitizer paths have no typed admission
carrier and therefore fail closed for `mainStrategicPlans` and their plan-experiment
metadata.
`StructuralOnly` and `PvCoupledOnly` evaluated plans may speak only through
bounded practical-guidance rows on `moveReviewPlayerSurface`; they do not
become selected main plans, `check_qualifying` inputs, retained plan metadata,
or owner claims. Their preconditions and execution steps may also appear as
`Practical objective` and `Practical steps` advanced rows when sanitized
surface capacity allows, but those rows remain practical guidance rather than
proof authority. Those practical advanced rows are deduplicated against
promoted plan siblings: a matching top-level theme or at least 70% overlap with
the practical plan's execution steps keeps the softer row silent.
Structural-only summary rows may name the current structure only when the typed
semantic structure profile is confident enough, is not `Unknown`, and the typed
plan-alignment band is `OnBook` or `Playable`, with the matched alignment plan
or narrative intent also matching the bounded practical plan, or when an
existing `StructurePlanArcBuilder` arc is prose-eligible and plan-matched. Plan
matching is exact against normalized plan text and typed matched-plan ids; prose
containment still requires the contained phrase to have at least three words,
keeping broad labels such as `pressure`, `attack`, `break`, or `queenside
pressure` from opening this path. This is
board-structural context for the practical row, not public motif admission:
`StructuralPlaybook` or `StructureProfile` alone must not become
`SupportedLocal`, owner, token, target, or exact-proof authority.
The same guarded profile fallback may mention the typed `centerState`; that
adds current-board context only and does not make the center state a proof
object.
That same plan-matched, prose-eligible arc may feed a lower-authority
`Practical route` advanced row from its existing deployment prose. The row is
allowed only as practical guidance and must not be read as exact route,
file-entry, outpost, target, or owner authority.
`Practical move` is narrower: the reviewed move must legally replay for one ply
from the current FEN, start from the arc's deployment origin, and land on the
arc's route. It is a lower-authority explanation of how the played move starts
or directly reaches that route, not a line proof, move-owner admission,
same-file reinforcement, or adjacent-route guard.
The arc may also feed `Practical restraint` only when its prophylaxis support
comes from the current-board first `PreventedPlanInfo` (`sourceScope = Now`).
That plan must also carry a positive `counterplayScoreDrop` or negative
`mobilityDelta`; default-current prevented plans without an impact signal stay
closed. Threat-line or citation-based prophylaxis support stays closed on this
lower authority route because it would otherwise borrow continuation authority.
`Practical fit` may expose only cautionary, non-positive structure-alignment
reasons such as missing preconditions. `PA_MATCH` alone stays silent, and the
row must not be treated as owner, motif, target, or exact-proof authority.
`Practical task` may use `StructurePlanArc.practicalCoda` only after the same
plan-matched, prose-eligible structure arc has already opened the lower-authority
structure path. The verdict is attached practical context, not a standalone
structural witness or authority source.
`Practical pressure` may use an existing `StrategyPack` target-fixing idea only
when typed evidence refs include `source:carlsbad_fixation_profile` plus a
`target_pressure_semantic` / `target_pressure_*` fact and pack-side owner match, or
`source:minority_attack_semantic` plus `target_pressure_semantic` /
`target_pressure_*` facts and pack-side owner match, or `source:minority_attack_support` plus a
`minority_attack_support_<flank>` fact and a valid structural target square, or
a current-board weakness source with its matching
fact: `source:enemy_weak_square` plus `enemy_weak_square_<square>`, or
`source:weak_complex_fixation` plus a `weak_complex_*` fact such as
`weak_complex_isolated_pawn` or `weak_complex_backward_pawn` and a valid focus
square, or `source:doubled_pawn_pressure_motif` plus
`doubled_pawn_pressure_shape`, `doubled_pawn_file_<file>`, a valid focus file,
Build readiness, pack-side owner match, and typed confidence, or compensation
target pressure with `source:compensation_target_fixation`, the matching
`compensation_target_fixation` fact, `material_deficit_compensation`, a valid
focus square, pack-side owner match, and typed confidence. The weakness-source
branch also requires pack-side owner match, and its named target list excludes
squares already eligible for the separate `Practical target` row on the same
target square. Exact `Fixed target` / `Minority attack` rows with builder-owned
exact wording and practical-plan target metadata remove the same target square
from every target-naming pressure branch. An eligible target plan or exact
target row on a different square does not close this pressure context; only the
remaining typed focus squares can be named. That square-target filtering does
not close the doubled-pawn pressure branch, which is file-scoped through
`doubled_pawn_file_<file>` and a valid focus file; emitted file text is taken
from that matched file fact.
The row carries `PracticalPlan` with no target metadata. Generic target-fixing ideas,
focus-square lists, plan-match bridges, color-complex-only weakness tags,
directional target access without board weakness corroboration, source-only
weak-square tags, source-only/no-target minority support, and source-only
or no-square weak-complex motif tags, source-only/shape-only/no-file
doubled-pawn pressure tags, and source-only compensation target fixation stay
closed for this row.
`Practical space` may use an existing `StrategyPack` space/restriction idea
only when typed evidence refs include a narrow structure-backed profile:
`source:color_complex_clamp`, `enemy_color_complex_weakness`, and a concrete
dark/light complex token with at least two valid focus squares and pack-side
owner match; a single dark/light token names the complex, overrides stale
`focusZone` text, and is sufficient even when `focusZone` is absent; absent or
ambiguous dark/light tokens keep the color-complex clamp row closed;
`source:maroczy_bind_profile` plus `structure_maroczy_bind` for White with
pack-side owner match; or
`source:iqp_central_presence` plus `structure_iqp_white`,
`iqp_central_presence_shape`, and focus square `d4` for White with pack-side
owner match; or
`source:iqp_space_bridge` plus `structure_iqp_white` for White, center focus,
`Build` readiness, pack-side owner match, and the producer confidence floor; or
`source:locked_center_bind` plus `structure_locked_center`, center focus,
pack-side owner match, and typed locked-center confidence. The locked-center
source is emitted only from a locked center with same-side space edge or
color-complex clamp support. It may also use typed central-space edge support
only when refs include `source:central_space_edge` and `central_space_edge_shape`,
with `Ready` readiness, center focus, pack-side owner match, and the producer
confidence floor. The row carries `PracticalPlan` with no target metadata.
For color-complex support, this is lower-authority structure context only: an
already visible exact `Color complex` row with builder-owned bishop/knight
wording, `PracticalPlan` target authority, and the same dark/light complex
suppresses the lower-authority `Practical space` row, while stale labels,
non-minor-piece prose, or targetless approximate wording do not.
It may also use current-board space-advantage motif support only when refs
include `source:space_advantage_motif`, `space_advantage_motif_shape`, and a
`space_pawn_delta_<n>` fact with `n >= 2`, with `Ready` readiness, center focus,
pack-side owner match, and motif confidence.
It may also use current-board central pawn-advance motif support only when refs
include `source:central_pawn_advance_motif`,
`central_pawn_advance_shape`, a matching central `central_pawn_file_<file>` fact,
and `central_pawn_to_rank_<n>` with `n >= 4`, with center focus, `Build`
readiness, pack-side owner match, and the motif confidence floor; the emitted
file token is the matched central file fact, not merely the first focus file.
It may also use current-board pawn-chain motif support only when refs include
`source:pawn_chain_space_motif`, `pawn_chain_space_shape`, and a
`pawn_chain_<base>_<tip>` fact, with at least two valid focus files, `Build`
readiness, pack-side owner match, and the pawn-chain confidence floor. The row
names kingside/queenside space only when the pawn-chain facts resolve to one
flank zone; mixed-flank or center-only chain facts stay flank-space-neutral.
It may also use typed aggregate mobility-restriction support only when refs
include `source:mobility_restriction` and `mobility_restriction_shape`, center focus,
pack-side owner match, and confidence at least `0.72`, which corresponds to at
least a two-piece low-mobility gap in the current board-derived producer.
Source-only central-space, source-only or one-piece mobility restriction,
source-only or weak-delta space-advantage motif support, generic locked-center,
source-only or non-central-file central pawn advance motif support,
source-only or shape-only pawn-chain motif support, pawn-chain support without
two valid files, source-only IQP space bridge, and plan-match space carriers stay
closed for this row.
`Practical break` may use an existing `StrategyPack` pawn-break idea only when
typed evidence refs include `source:pawn_analysis_break_ready` or
`source:pawn_play_break_ready`, the matching `pawn_analysis_break_ready_shape`
or `pawn_play_break_ready_shape` fact, `Ready` readiness, a valid focus file,
and high confidence; when a matching `break_file_<file>` fact is present, the
emitted break file uses that fact. It may also use the French Advance
...f6 seed (`source:french_f6_break_seed`, `french_f6_break_seed_shape`,
`white_e5_chain`, `black_f7_break_pawn`) with Black owner, f-file focus, e5/f6
focus squares, and high confidence. It may also use a file-opening consequence
only when refs carry `source:file_opening_consequence` and a
`contested_file_<file>` fact matching the focus file, with owner/pack side
agreement and typed confidence; the emitted opening file is the matched
contested-file fact. It may also use central-tension break support
only when refs carry `source:central_break_tension`, a valid central focus file,
center focus, owner/pack side agreement, typed confidence, and either
`locked_center` or `Ready` readiness; because this source carries no
file-specific fact, the row names a break file only when the merged idea has
exactly one valid central focus file, otherwise it stays
central-break-cue-neutral. It may also use current pawn-break motif
support only when refs carry `source:pawn_break_motif`,
`pawn_break_motif_shape`, and a `break_file_<file>` fact matching the focus file,
with `Build` readiness, owner/pack side agreement, and the motif confidence
floor; the emitted break file is the matched file fact. The row carries
`PracticalPlan` with no route/token metadata. Plan-match break preparation,
counter-break race, source-only central-break tension, source-only/shape-only
pawn-break motif carriers, non-central-file tension,
and broad French profile carriers stay closed for this row and do not become
`CentralBreakTiming`.
`Practical restraint` may use an existing `StrategyPack` counterplay-suppression
idea only when typed evidence refs include Hedgehog break-denial geometry
(`source:hedgehog_break_denial_geometry`, `structure_hedgehog`,
`hedgehog_break_denial_shape`, White owner, b/d file focus, queenside zone,
high confidence) or Maroczy break-denial geometry
(`source:maroczy_break_denial_geometry`, `structure_maroczy_bind`,
`maroczy_break_denial_shape`, White owner, c/d file focus, center zone, high
confidence). The row carries `PracticalPlan` with no route/token metadata.
It may also use an opponent-counterbreak denial bridge only when refs include
`source:opponent_counterbreak_denial`, the surviving `opponent_counter_break`
fact, a concrete focus file, `Ready` readiness, pack-side owner match, and the
producer confidence floor. It may also use current-board counterplay
suppression only when refs include `source:counterplay_suppression`,
`counterplay_suppression_shape`, the single-evidence `counterplay_break_denial` marker,
`break_neutralized`, and `denied_break_resource`, with a concrete focus file,
`Ready` readiness, pack-side owner match, and the producer confidence floor. It
may also use current-board passer-blockade motif support only when refs include
`source:passer_blockade_motif`, `passer_blockade_shape`,
`blockade_square_<square>`, and `blockaded_pawn_<square>`, with `Ready`
readiness, pack-side owner match, and the motif confidence floor. This remains a
lower-authority practical brake on the named passed pawn and does not satisfy the
exact `Passer blockade` top-PV/public row. It may also use compensation
counterplay denial only when refs include
`source:compensation_counterplay_denial`, `material_deficit_compensation`,
and `break_neutralized`, with a concrete focus file, pack-side owner match, and
the compensation producer confidence floor. These bridge rows name a break file
only when the merged idea has exactly one valid focus file; multi-file focus
stays break-cue-neutral. Duplicate suppression is
subtype-specific: exact `Counterplay break`, `Counterplay restraint`, `Break and
entry`, or `Route denial` rows close break/counterplay-denial practical context,
but do not close passer-blockade practical context. For this duplicate
suppression, exact means the builder-owned generated wording plus the
corresponding public authority: `CounterplayBreak` token authority for
`Counterplay break`, or `PracticalPlan` authority for `Counterplay restraint`,
`Break and entry`, and `Route denial`. Stale labels or approximate route-denial
prose do not close the matching lower-authority row. Broad
Hedgehog containment, broad Maroczy counterplay suppression, source-only
opponent-counterbreak rows, source-only/generic counterplay suppression,
source-only/shape-only/no-pawn passer-blockade motif rows,
source-only/generic compensation counterplay denial, compensation denied-square-only rows,
merged-ref counterplay suppression, counterplay-score-drop-only rows, and plan-match suppression carriers stay
closed for this row and do not become `CounterplayBreak` or `CounterplayRestraint`.
Merged selector ideas preserve enough source, shape, and witness refs for these
support-only gates to evaluate typed carriers; this projection remains
support-only and does not relax proof-owner admission.
`Practical line` may use an existing `StrategyPack` line-occupation idea only
when typed evidence refs include current occupied major-piece line support:
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
file. Broad route, directional, compensation, and delayed-recovery line rows
name a file only when the merged idea has exactly one open/semi-open file fact;
multi-file focus stays line-access-neutral. The idea must name the matching rook or queen
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
The no-duplication gate is subtype-specific and file-specific: exact
`File entry` closes file/post/open-file line context only for the same file
named by the practical row, exact `Seventh-rank entry` closes rook seventh-rank
context only for the exact row's generated rook wording, exact `Connected rooks`
closes connected-rooks context, and exact
`Doubled rooks` closes doubled-rooks context only for the same file named by the
practical row. A different exact line subtype, exact `File entry` on a different
file, exact `Doubled rooks` on a different file, or exact rook seventh-rank
entry beside a queen seventh-rank cue does not suppress unrelated practical line
context. The gate requires current builder-owned exact wording
and public authority: practical-plan target metadata plus the same generated
`through <file>-file` wording for `File entry`, where the target square is on
that named file, or `PracticalPlan` authority plus the generated top-PV wording
for `Seventh-rank entry`, `Connected rooks`, and `Doubled rooks`;
seventh-rank suppression is role-aware. Exact-looking
stale labels with lower-authority line prose do not suppress this context row.
The row carries `PracticalPlan` with no
target/token metadata.
Source-only open-file tags, open-file tags without a major-piece beneficiary,
source-only/no-file semi-open-file tags, semi-open-file tags without a
major-piece beneficiary,
source-only occupied-line tags, source-only doubled-rook tags,
non-rook doubled-piece motif carriers, source-only seventh-rank tags,
source-only connected-rook tags, connected-rook
tags without a rook beneficiary, source-only/no-route line-control, non-exact
route line access, source-only, no-file, queen, or low-confidence directional
line access, source-only or no-file compensation line/delayed recovery carriers,
plan-match line carriers, and source-only open-file tags stay closed for this
row and do not become `local_file_entry_bind` or `half_open_file_pressure`.
`Practical outpost` may use an existing `StrategyPack` outpost idea only when
typed evidence refs include `source:outpost_tag` and an `outpost_<square>` fact
matching a valid focus square, including current minor-piece `Motif.Outpost`
carriers, or occupied strong-knight support with
`source:strong_knight` plus a `strong_knight_<square>` fact matching a valid
focus square and a knight beneficiary, or exact route-outpost support with
`source:route_outpost_access`, `route_outpost_access_shape`, and
`route_surface_exact` aimed at a valid focus square by a minor piece; the
outpost square is named only when the merged idea carries a single focus square,
otherwise the practical row stays outpost-cue-neutral; or
directional outpost support with `source:directional_outpost_access`,
`directional_outpost_access_shape`, and a valid focus square aimed at by a minor
piece; the outpost square is named only when the merged idea carries a single
focus square, otherwise the practical row stays outpost-cue-neutral. The idea
must match the pack side and carry typed confidence for the
underlying outpost/strong-knight/route/directional bridge; tag, strong-knight,
and exact-route support must be `Ready`, while directional outpost support must
be `Build` with confidence at least `0.64`. The row is suppressed only when an
already visible exact `Opening outpost` or `Knight outpost` row certifies the
same outpost square the practical row would name; merged multi-square
route/directional outpost support stays open only when at least one focus square
is not already exact-certified. For `Knight outpost`, the
no-duplication gate requires current exact row wording plus same-square
practical-plan target metadata; exact-looking stale labels with lower-authority
outpost prose do not suppress this context row. For `Opening outpost`, the
no-duplication gate likewise requires opening-goal builder wording with
`PracticalPlan` authority and the same extracted outpost square; stale,
lower-authority, or different-square `Opening outpost` labels do not suppress
this context row. The row carries `PracticalPlan`
with no target/token metadata. Unoccupied/build strong-knight, entrenched-piece,
non-exact route access, source-only, non-minor, non-`Build`, or low-confidence
directional outpost access, opening-goal outpost, and
plan-match outpost carriers stay closed for this row and do not become exact
`OutpostOccupation` authority.
`Practical trade` may use an existing `StrategyPack` favorable-trade or
transformation idea only when typed evidence refs include
`source:iqp_simplification_profile`, `structure_iqp_black`, and either
`capture_or_exchange` or `iqp_trade_down_plan`, with White owner, pack-side
owner match, high typed confidence, and no already visible exact
`Simplification` row. The no-duplication gate requires current exact row
wording plus same-square practical-plan target metadata; exact-looking stale
`Simplification` labels with lower-authority exchange prose do not suppress this
context row. The exchange square is named only when the idea carries a single
focus square; multi-target exchange support stays trade-cue-neutral because no
square-specific fact is published for this branch. It may also use IQP exchange-availability support only
when refs include `source:exchange_availability_bridge` and
`structure_iqp_black`, with White owner, `Build` readiness, pack-side owner
match, and the exchange-availability confidence floor. The row carries
`PracticalPlan` with no target/token metadata. Classification windows, generic
capture/exchange, source-only exchange-availability bridges, plan-match
transformation, soft transformation plan support, and non-IQP transformation
carriers stay closed for this row and do not become exact `SimplificationWindow`
authority.
`Practical conversion` may use an existing `StrategyPack` favorable-trade or
transformation idea only when typed evidence refs include both
`source:winning_endgame_transition` and `winning_endgame_transition_shape`, with
`Ready` readiness, pack-side owner match, winning-endgame confidence, and no
already visible exact `Technical conversion` row; because the producer may
publish up to three key-square focus cues under one shape fact, this row names
a square only when the merged idea has exactly one valid focus square,
otherwise it stays conversion-cue-neutral. It may also use typed evidence refs
that include current rook-endgame motif support with `source:rook_endgame_pattern`,
`rook_endgame_pattern_shape`, and either `rook_behind_passed_pawn` or
`king_cut_off`, with `Build` readiness, pack-side owner match, confidence at
least `0.72`, and no already visible exact `Technical conversion` row; rook-endgame
wording names a specific pattern only when exactly one rook-endgame pattern fact
survives, otherwise the row stays rook-endgame-map neutral, or when
typed evidence refs include current endgame-technique motif support with
`source:endgame_technique_motif`, `endgame_technique_shape`, an
`opposition_direct`, `opposition_distant`, `opposition_diagonal`, or
`zugzwang_shape` fact, or endgame-phase `Motif.KingStep.Activation` support with
`king_activity_shape`, endgame focus, `Build` readiness, pack-side owner match,
confidence at least `0.72`, and no already visible exact `Technical conversion`
row; endgame-technique wording names a specific technique only when exactly one
technique fact survives, otherwise the row stays endgame-technique-map neutral,
or when
typed evidence refs include current passed-pawn motif support with
`source:passed_pawn_conversion_motif`, `passed_pawn_conversion_shape`, and a
`passed_pawn_<square>` fact matching a valid focus square, including
`Motif.PassedPawnPush` carriers that also keep `passed_pawn_push` and advanced
rank support when applicable, and `Motif.PawnPromotion` carriers that also keep
`pawn_promotion`, `promotion_piece_<role>`, and optional `underpromotion`
support, with `Build` readiness, pack-side owner match, confidence at least
`0.72`, and no already visible exact `Technical conversion` row; when the row
names a passed-pawn or promotion square, the emitted square is the matching
`passed_pawn_<square>` fact, not merely the first focus square. The row
carries `PracticalPlan` with no target/token metadata. Exact `Technical
conversion` suppression means the restricted-defense conversion row's
builder-owned wording with `PracticalPlan` authority; a stale or lower-authority
`Technical conversion` label does not close practical conversion context.
Classification-only
transformation windows, source-only or shape-only rook-endgame pattern carriers,
source-only or shape-only endgame-technique motif carriers, endgame-technique
motif carriers without opposition, zugzwang, or endgame king-activity facts,
source-only or shape-only passed-pawn motif carriers, passed-pawn carriers
without a matching square fact, soft plan matches, and generic exchange carriers
stay closed for this row and do not become restricted-defense `Technical
conversion` authority.
`Practical minor` may use an existing `StrategyPack` minor-piece idea only when
typed evidence refs include a concrete minor-piece bridge:
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
wording. Centralization rows name a concrete role/square pair only when the
merged idea has exactly one minor beneficiary and exactly one centralized-piece
fact; merged multi-piece or multi-square centralization stays
minor-piece-neutral. The idea must name the relevant minor beneficiary, match the pack side,
and carry typed bridge confidence; non-French strong-knight and bishop-pair rows
must also be `Ready`, bishop-pair support closes when an exact `Bishop pair`
row is already visible, and opposite-colored-bishop support closes when an exact
`Opposite-color bishops` row is already visible. For those bishop rows, exact
means the builder-owned capture wording with `PracticalPlan` authority; a stale
or lower-authority `Bishop pair`/`Opposite-color bishops` label does not close
the practical minor-piece context. The row carries `PracticalPlan` with no
target/token metadata. French profile alone, source-only bishop pair,
source-only good bishop, count imbalance without the good-bishop count-edge
pair, source-only opposite-color bishop carriers, source-only or
no-square/no-minor centralization motif carriers, source-only/shape-only
knight-vs-bishop motif carriers, source-only/no-minor maneuver motif
carriers, and plan-match minor-piece
carriers stay closed for this row and do not become exact outpost, bishop-pair,
opposite-color-bishops, or supported-local authority.
`Practical prophylaxis` may use an existing `StrategyPack` prophylaxis idea
only when typed evidence refs include a board-pattern prophylaxis source and
matching focus geometry: `source:bishop_pin_watch` with a valid `g4`/`g5` focus
square, or `source:queenside_counterbreak_watch` with a `b`-file focus. It also
requires pack-side owner match, typed board-pattern confidence, and no promoted
exact/probe-backed `Prophylaxis` row already visible. The row carries
`PracticalPlan` with no target/token metadata. Plan-match prophylaxis,
threat-analysis prophylaxis, prevented-plan, counterbreak-watch, raw
prophylaxis plans, and probe-backed prophylaxis rows stay closed for this row
and do not become exact prophylaxis-restraint authority.
`Practical attack` may use an existing `StrategyPack` king-attack idea only
when typed evidence refs include the full fianchetto assault profile:
`source:fianchetto_assault_profile`, `source:opposite_side_storm`, and
`structure_fianchetto_shell`, with kingside focus, `Build` readiness, pack-side
owner match, and typed fianchetto-assault confidence; or when typed evidence
refs include current king-ring pressure with `source:king_ring_pressure` plus
the `king_ring_pressure_shape` fact, a concrete enemy-king focus zone, `Build`
readiness, pack-side owner match, and the producer confidence floor; or when
typed evidence refs include enemy king central-exposure support with
`source:enemy_king_stuck_center` plus `enemy_king_central_exposure`, a concrete
enemy-king focus zone, `Build` readiness, pack-side owner match, and the
producer confidence floor; or when typed evidence refs include enemy
weak-back-rank support, including weak-back-rank motif carriers, with
`source:enemy_weak_back_rank` plus `enemy_weak_back_rank_shape`, a concrete
enemy-king focus zone, `Build` readiness, pack-side owner match, and the
producer confidence floor; or when
typed evidence refs include current flank-pawn hook pressure with
`source:flank_pawn_pressure` plus `hook_creation_chance`, a concrete enemy-king
focus zone, `Build` readiness, pack-side owner match, and the producer
confidence floor, unless an exact `Hook creation` or `Rook-pawn march` row is
already visible; or when typed evidence refs include flank pawn-advance motif
support with `source:flank_pawn_advance_motif`,
`flank_pawn_advance_shape`, a matching `flank_pawn_file_*` fact on the
a/b/g/h files, a `flank_pawn_to_rank_*` fact at relative rank four or beyond,
`Build` readiness, pack-side owner match, and the motif confidence floor, unless
an exact `Hook creation` or `Rook-pawn march` row is already visible. Pawn-file
wording is used only when exactly one flank-pawn file fact survives;
kingside/queenside wording is derived only from the surviving flank-pawn file
facts, and mixed-flank facts stay attack-cue-neutral; or compensation diagonal-battery support when typed evidence refs include `source:compensation_diagonal_battery`,
`compensation_diagonal_battery`, `material_deficit_compensation`, B/Q
beneficiary pieces, king-side/center attack focus, pack-side owner match, and
the compensation-battery confidence floor; or when typed evidence refs include
`source:compensation_development_lead`, `development_lead_compensation`, and
`material_deficit_compensation`, a concrete enemy-king focus zone, `Build`
readiness, pack-side owner match, and the producer confidence floor; or when
typed evidence refs include `source:compensation_king_window`,
`uncastled_or_unsettled_king_window`, and `material_deficit_compensation`, a
concrete enemy-king focus zone, `Build` readiness, pack-side owner match, and
the producer confidence floor; or
when typed evidence refs include exact route-attack support with
`source:route_attack_lane`, `route_attack_lane_shape`, a concrete enemy-king
focus square and zone, a beneficiary piece, `Ready`
readiness, pack-side owner match, and the route-attack confidence floor; the
route square is named only when the merged idea carries a single focus square,
otherwise the practical row stays route-neutral; or
when typed evidence refs include directional attack-lane support with
`source:directional_attack_lane`, `directional_attack_lane_shape`, a concrete
enemy-king focus square and zone, a beneficiary piece, `Build` readiness,
pack-side owner match, and the directional producer confidence floor; the target
square is named only when the merged idea carries a single focus square,
otherwise the practical row stays target-map-neutral; or when
typed evidence refs include current motif-battery support with
`source:motif_battery`, a diagonal/file battery-axis fact, two concrete battery
squares, two beneficiary pieces, a concrete enemy-king focus zone, `Build`
readiness, pack-side owner match, and the motif-battery confidence floor, unless
an exact `Battery pressure` row is already visible; battery-axis wording is used
only when exactly one battery-axis fact survives, otherwise the row stays
battery-neutral; or when typed evidence refs
include current rook-lift motif support with `source:motif_rook_lift`, a
concrete focus file, rook beneficiary, concrete enemy-king focus zone, `Build`
readiness, pack-side owner match, and the motif-rook-lift confidence floor,
unless an exact `Rook lift` row is already visible; because this motif source
carries no file-specific fact, the row names a file only when the merged idea
has exactly one valid focus file, otherwise it stays rook-lift-cue-neutral; or
when typed evidence refs
include current piece-lift motif support with `source:motif_piece_lift` plus
`motif_piece_lift_shape`, a beneficiary piece, concrete enemy-king focus zone,
`Build` readiness, pack-side owner match, and the motif-piece-lift confidence
floor. Because this motif source carries no role-specific fact, the row names a
lifted piece only when the merged idea has exactly one beneficiary piece,
otherwise it stays piece-lift-neutral; or when typed evidence refs include current normal/discovered check-motif
support with `source:motif_check_pressure`, a `check_type_normal` or
`check_type_discovered` fact, a concrete enemy-king focus square and zone, a
beneficiary piece, `Ready` readiness, pack-side owner match, and the
motif-check confidence floor; the checked square is named only when the merged
idea carries a single focus square, and the checking piece is named only when
the merged idea has exactly one beneficiary piece, otherwise the practical row
stays check-motif-neutral; or when typed evidence refs include current
fianchetto motif support with `source:fianchetto_motif`,
`fianchetto_motif_shape`, a `fianchetto_side_kingside` or
`fianchetto_side_queenside` fact, a concrete focus zone, bishop beneficiary,
`Build` readiness, pack-side owner match, and the motif confidence floor; or
when typed evidence refs include current initiative motif support with
`source:initiative_motif`, `initiative_motif_shape`, a concrete enemy-king focus
zone, `Build` readiness, pack-side owner match, the motif confidence floor, and
an `initiative_score_*` fact at ten or above. Duplicate suppression is
subtype-specific: exact back-rank mate, mate-net, or Greek-gift rows close
generic attack context, exact `Hook creation` / `Rook-pawn march` rows close
flank-hook and flank-pawn advance context, exact `Battery pressure` closes
battery context, and exact `Rook lift` closes rook-lift context. A different
exact attack subtype does not suppress unrelated practical attack context. The
row carries `PracticalPlan` with no target/token metadata. Mate-net, source-only king-ring pressure,
source-only enemy king central exposure, source-only weak back rank, source-only
flank-pawn pressure, rook-pawn-ready-only flank pressure, attacking-threat
analysis, source-only/flank-shape-only flank pawn-advance motif carriers,
non-flank pawn-advance motif carriers, motif attack lanes, source-only or non-exact route-attack lanes,
source-only directional attack lanes, source-only compensation development, source-only
compensation king-window, source-only or axis-only/no-square motif battery,
source-only or no-file/no-rook motif rook lift, source-only or no-zone/no-piece
motif piece lift, source-only or no-square/no-zone/no-piece motif check
pressure, mate/double/smothered check motifs, source-only/no-bishop/mirrored
fianchetto motif carriers, source-only/no-zone/low-score initiative motif
carriers, plan-match king-attack carriers,
and opposite-side storm alone
stay closed for this row and do not become exact tactical authority.
For duplicate suppression, exact attack/flank/battery/rook-lift rows mean the
builder-owned generated wording plus `PracticalPlan` authority for
`Back-rank mate`, `Mate net`, `Greek gift`, `Hook creation`, `Rook-pawn march`,
`Battery pressure`, or `Rook lift`. A stale label, approximate prose, or row
without that authority does not close the matching lower-authority
`Practical attack` construction.
The one cache-hit exception is a previously sanitized MoveReview response:
`sanitizeCachedMoveReview` may preserve cached `mainStrategicPlans`,
matching plan experiments, continuity token, and ledger only when the cached
response already has `moveReviewPlayerSurface` and all retained plan evidence
source markers have been removed. Marker-bearing cached plans still fail
closed.

Probe validation separates chess evidence from bookkeeping:

- hard failures: missing or mismatched FEN/probed move/id for a board-bound
  request, malformed FEN or probed move echo, missing required board signal,
  missing purpose contract, depth-floor missing or unmet,
  mate/refutation/cp-loss beyond contract
- soft diagnostics: purpose/objective label drift and hash/fingerprint echo
  drift

Soft diagnostics may remain in audit output, but they must not by themselves
block a board-valid supporting probe. `alternativeDominance` is ranking
metadata and must not be reported as refutation.
Probe request generation that needs an after-played or after-best board must
derive that board by legal replay from the current FEN; illegal candidate text
does not fall back to the original FEN and cannot seed branch probes.
Multi-move probe requests are board-bound only when the result certifies a
`probedMove` or candidate move that is one of the requested moves. Unknown
request purposes have no authority contract and fail closed even when explicit
required signals are present; unknown-purpose results must not refute plans
through the default 0cp bound. Probe requests with explicit plan id, seed, or
plan name must not relink by substring or by one matching sibling field. If
more than one explicit binding is supplied, every supplied binding must match
the same plan hypothesis. Refutation-purpose probes are negative tests: they
may mark a plan `Refuted`, but a non-refuting result does not become positive
`ProbeBacked` support without a separate affirmative support probe.
Client-generated `keyMotifs`, `l1Delta`, and `futureSnapshot` values marked by
`generatedRequiredSignals` or `motifInferenceMode=purpose_only` /
`purpose_plus_compat` do not satisfy required authority signals. Unknown
required probe signals and unknown/no-contract probe purposes fail closed.
`keyMotifs` remain display/diagnostic text. Forcing-modality promotion may only
consume canonical `motifTags` values such as `forcing`, `exchange`, `trade`, or
`simplification`; absent tags fail closed for motif-based forcing.

Exact-family trust is witness-bound. A subplan id such as
`central_break_timing` may explain the plan taxonomy, but it must not open the
exact central-break owner path unless the exact witness is present; otherwise
the typed evidence may only support the bounded generic plan-advance path.

## Proof And Packet Boundary

`PlayerFacingClaimPacket` is the runtime carrier for exact claim candidates.
`ProofContractRules` decides which proof families can be certified or supported
locally. For `ExactSlice`, the trust boundary is the typed
`PlayerFacingProofPathWitness.exactSliceProof` ADT produced by the
board/probe witness path. Generic owner, anchor, structure, continuation, or
prose terms are diagnostics and surface wording support only; they cannot be
parsed or promoted into exact-slice proof authority.

Required proof witnesses are fail-closed contract predicates, not descriptive
labels. `NoTacticalVeto` fails when tactical veto or missing tactical-context
codes are present, and `ClaimOnlySurface` fails unless the packet remains in the
bounded weak-main claim surface. A required witness that is absent or contradicted
must produce a failure code before any supported-local or certified admission.
`ProphylacticRestraint` exact proof uses board/resource tokens such as a square
or `denied_resource:<class>` from the prevented-plan resource class. Arbitrary
plan labels such as "counterplay window" are prose only and fail the exact-slice
contract. The typed resource token must also be mirrored in the packet witness
terms; those terms are an integrity check, not a source from which to rebuild the
proof token.
MoveReview player-surface projection consumes that proof only through
`ClaimAuthorityResolver.supportedLocalMoveDeltaPacketDecision`: a
`Counterplay restraint` row requires a matching `prophylactic_move` /
`counterplay_restraint` packet, the typed `ProphylacticRestraint` exact proof,
and no tactical veto or release risk. The row may name only the prevented
resource token; it cannot widen into global "no counterplay" or generic
prophylaxis authority.
Named route-network bind has a separate existing typed carrier:
`RouteNetworkBindProof.SurfaceNetwork`. MoveReview player-surface projection
may consume that carrier only through
`ClaimAuthorityResolver.namedRouteNetworkSurfaceDecision`; tactical vetoes,
`heavyPieceLocalBindBlocked`, and intermediate route chains suppress release.
The row can name only the certified file, entry square, and reroute square. It
does not open broad heavy-piece/local-bind, route-chain, or no-counterplay
authority.
Dual-axis bind has a separate typed contract carrier:
`TwoAxisBindProof.Contract`. MoveReview player-surface projection may consume
that carrier only through `ClaimAuthorityResolver.dualAxisBindSurfaceDecision`;
tactical vetoes, `heavyPieceLocalBindBlocked`, uncertified contracts, wrong
scope/archetype, high reinflation risk, or missing break/entry axes suppress
release. The row can name only the certified break axis and independent entry
axis. It does not open broad local-bind/global-squeeze or no-counterplay
authority, and raw hypothesis prose is not a source for row text.
The current public witness is the queenless late-middlegame fixture
`2r2rk1/pp3pp1/2n1p2p/3p4/1p1P1P2/P1P1PN1P/1P4P1/2R2RK1 w - - 0 24`,
where the certified contract keeps direct best defense `f8e8`, branch key
`f8e8`, future persistence, and bounded continuation visibility tied to the
`...c5` break plus `b4` entry axes.
Restricted-defense conversion has a separate typed contract carrier:
`RestrictedDefenseConversionProof.Contract`. MoveReview player-surface
projection may consume that carrier only through
`ClaimAuthorityResolver.restrictedDefenseConversionSurfaceDecision`; tactical
vetoes, uncertified contracts, missing direct best defense, missing same-branch
persistence, broad defender resources, non-persistent route pressure, or
move-order fragility suppress release. The row is bounded to technical
conversion support after the checked best defense and cannot certify global
conversion, investment ownership, or compensation truth.
IQP inducement is SupportedLocal only. Its public row may consume only an
`iqp_inducement_probe` / `iqp_inducement` packet admitted by the resolver and
carrying `PlayerFacingExactSliceProof.IqpInducement(targetSquare, lineMoves)`
with matching induced isolated-pawn transition and checked-line move terms. A
generic IQP label, structural tag, source-catalog fixture name, or target term
without the typed exact-slice proof is not enough to release a player-facing
row. The public authority remains `PracticalPlan`; backend sanitizer and
frontend decoder preserve a `target` square only on the `IQP target` row and
only after the typed packet already matched. The current Karpov-Andersson IQP
source fixture is public only because ply 49 `cxd5` has exact replay, top-PV identity,
`iqp_inducement_probe` source/family, the induced `isolated_pawn:d5`
continuation, and no contract failures.
Simplification-window support is also resolver-bound. A public
`Simplification` row may consume only a `simplification_window` packet whose
contract witnesses are admitted, whose typed exact proof is
`PlayerFacingExactSliceProof.SimplificationWindow(exchangeSquare)`, and whose
checked continuation names the matching `exchange_square:<square>` witness.
Favorable-exchange prose, source labels, and generic conversion wording do not
release the row. The current public
source fixture is `source-botvinnik-vidmar-1936-simplification-window`; the
same source family still suppresses Salov-Ljubojevic because the owner proof is
missing. The public authority remains `PracticalPlan`; backend sanitizer and
frontend decoder preserve a `target` square only on the exact `Simplification`
row after the typed packet has already matched, and approximate labels such as
`Simplification window` remain non-authoritative.
For outpost support, backend sanitizer and frontend decoder likewise preserve
a `target` square only on the exact `Knight outpost` row after the typed
`OutpostOccupation` packet has already matched and the row uses the
builder-owned `knight` wording; non-knight outpost prose keeps at most
untargeted practical-plan authority.
For favorable-exchange move-local packets, `defender_trade` and
`bad_piece_liquidation` must also carry board-replayed structure-transition
witnesses and matching typed exact-slice proof before `SupportedLocal`
admission. Their proof contracts also require the same branch to be proven and
persistence to remain stable; branch-shaped terms alone are not enough when the
typed proof is missing, `sameBranchState` is weak, or persistence is weak. A
defender-trade packet must prove
that the exchanged defender actually defended a non-exchange target and that the
defense relation disappears after the replayed recapture with the target's
defender count strictly reduced. If the specific defender is gone but a newly
opened replacement line preserves the defender count, exact defender-trade
ownership stays closed and the line may only degrade to generic exchange or
material-transition guidance. The target set is limited to declared
focal/weakness targets; arbitrary defended pieces and the exchange square are
not fallback targets. A bad-piece liquidation packet must prove a constrained
bishop on the current board and a legal bishop capture followed by recapture.
If these fail, the owner/support packet is suppressed; safe exchange prose can
only degrade to line-consequence or practical guidance.
MoveReview player-surface projection does not relax this: `Defender trade` and
`Bad piece trade` rows consume only packets admitted by
`ClaimAuthorityResolver.supportedLocalMoveDeltaPacketDecision`. Defender/bad-piece
projection validates the typed exact-slice proof
(`DefenderTrade` or `BadPieceLiquidation`) against the packet source/family and
analyzer branch fact terms before using its squares; queen-trade projection
validates the same boundary for `QueenTradeShield` line moves. The current
public bad-piece source row is
`source-capablanca-golombek-1939-bad-piece-liquidation`; it stays
`SupportedLocal` because ply 43 `Bxd6` has exact replay, top-PV identity,
`bad_piece_liquidation` source/family, and no contract failures, while the
controlled diagnostic lane remains review-only.
Raw favorable-exchange labels, missing or malformed structure-transition
witnesses, exact-proof/term
mismatches, or tactical truth-mode packets produce no exchange ownership row.
Support-level activity analysis uses the same constrained-bishop rule, so
same-color central pawn count alone is no longer trusted even for bad-bishop
ranking hints.

The shared favorable-exchange replay boundary is `MoveReviewExchangeAnalyzer`.
Its PV identity source is the raw engine `VariationLine.moves` list; parsed PV
metadata is accepted only as a no-raw-moves fallback and cannot supersede a
supplied engine line.
The owner witnesses consume only proof-sized legal prefixes from that replay:
three plies for defender trade, the direct or delayed legal bishop
capture/recapture prefix for bad-piece liquidation, and the legal queen-capture
plus king-recapture prefix for `queen_trade_shield`. Illegal or stale PV text
after that proven prefix is not trusted or emitted as continuation proof, but it
does not silence the already proven local witness. Illegal text before the
required prefix still fails closed.
The current public `queen_trade_shield` source row is
`source-carlsen-anand-2014-g6-queen-trade-completion`: ply 17 `Qxd8+` from
`r1bqk2r/1p3ppp/p1p1pn2/8/1bP1P3/2NQ4/PP3PPP/R1B1KB1R w KQkq - 0 9`, with
legal prefix `d3d8 e8d8` and `SupportedLocal` release. The adjacent ply 15 row
remains secondary support for the same queenless-branch witness shape.
Player-facing SAN citations, decision comparisons, and close-alternative labels
share that boundary: they may render SAN from the raw UCI line, but stale
`parsedMoves` cannot rename the engine's first move or preview branch.
`StrategicSemanticObservationPipeline` may expose defender-trade or
bad-piece-liquidation observations as typed selector evidence when that analyzer
can replay the branch legally and the semantic context supplies the reviewed
`playedMove`; a missing played move fails closed instead of borrowing the top
PV first move. Those observations are not proof packets.
The same analyzer-owned `RelationWitness` read-model also carries the
implemented relation observations inventoried in `CommentaryPipelineSSOT.md`.
These rows are selector/support evidence only: they prove local board relations
for ranking and softer phrasing, but they do not create `CertifiedOwner` or
`SupportedLocal` authority unless a separate proof contract admits that family.
The shared semantic producer context owns the six-ply standard top-PV replay
plus the dedicated twelve-ply/probe-PV draw-resource exception and assembles one
relation-witness list; the relation producer consumes only implemented catalog
kinds through that shared context, so trust decisions do not depend on separate
standard replay, duplicate draw-resource projection, or local kind-filtering
attempts.
When an explicit target list is present, the relation witness is target-bound and may not fall
through to a material-target fallback, including discovered-attack target
selection. Double-check support requires a replayed reviewed move that leaves
the opposing king checked by at least two checker squares, and explicit target
lists close this king-targeted relation instead of acting as fallback targets.
Back-rank mate support reuses the existing tactical pattern detector and
requires the replayed reviewed move to end in `checkMate` with the defender's
king still on the back rank; explicit target lists close this king-targeted
relation instead of acting as fallback targets.
Mate-net support reuses the existing mate-required tactical pattern detectors
other than back-rank mate, requires the replayed reviewed move to end in
`checkMate`, and keeps the detector id as support-only evidence; explicit
target lists close this king-targeted relation instead of acting as fallback
targets.
Greek-gift support reuses the existing tactical pattern detector, requires a
legal reviewed bishop sacrifice on h7/h2 that gives check, and accepts only
immediate kingside support or support that appears by replaying the supplied PV
continuation. Explicit target lists close this pattern relation instead of
acting as fallback targets.
Fork support requires a replayed move attacking at least two bound targets.
Hanging-piece support requires the replayed mover to attack an
undefended bound target. Pin and skewer support use the same replayed relation boundary and
require long-range ray geometry through the pinned/front piece and the piece
behind it. Invalid
explicit target tokens close the candidate rather than
reopening structural or material fallback. Downstream selection consumes relation
rows through `RelationObservationCatalog`, which centralizes the mapping from
witness kind to semantic id, selector source, strategic idea kind, readiness, and
confidence, bounded public row/text labels, and producer admission. The runtime
relation inventory keeps a non-public deferred list for motif families that are
present in adjacent model or detector assets but lack a board-replayed relation
witness; that list is currently empty. Deferred entries, when present, are not
cataloged selector rows, not relation producer inputs, and not frontend
authority tokens. Raw helper or motif tags still need an analyzer-owned
relation witness before they may expose relation evidence or release authority.
For implemented descriptors whose raw motif tags are witness-only or PV
draw-resource only (`trapped_piece`, `domination`, `zwischenzug`,
`stalemate_trap`, and `perpetual_check`), that witness must carry matching typed
`RelationDetails`; raw focus/fact strings fail closed before semantic relation
evidence or player-facing relation rows.
`trapped_piece` is implemented only through a target-bound board witness: the
reviewed move must legally replay, the moved attacker must attack a valid
explicit target, the target must be a higher-value enemy non-pawn/non-king
piece, and that target must have no legal escape square that preserves the piece
outside attack by the pressure side. Generic trapped-piece activity, raw motif text, source
strings, and helper notation remain support or sanitizer inputs, not release
authority.
`domination` is implemented only through a target-bound board witness for
same/lower-value targets: the reviewed move must legally replay, the moved
attacker must attack a valid explicit target, all pseudo-escape squares for the
target must be controlled by the pressure side, the target must have no legal
safe escape, and ray pin/skewer geometry is left to the line-relation witnesses.
Generic domination motifs, raw helper notation, and restriction prose remain
below release authority.
`trapped_piece`, `domination`, and `zwischenzug` may project supported-local
`Trapped piece`, `Domination`, and `Zwischenzug` rows only after those exact
relation witnesses exist. This does not open broad mobility-cage or move-order
authority, and it does not convert raw mobility prose into release evidence.
Within the bounded MoveReview relation row cap, admitted `trapped_piece`,
`domination`, and `zwischenzug` witnesses sort ahead of generic relation rows;
that priority is display ordering only and does not change their proof gate.
`zwischenzug` is implemented only through a target-bound board witness: the
reviewed move must legally replay as the first move, explicit targets must parse
to a non-empty expected recapture square set, the side to move must have had a
legal non-pawn capture on that square before the reviewed move, the reviewed
move must not be that capture, and the moved piece must give direct check. Raw
`Zwischenzug(...)` helper notation may be cleaned to `move-order caution`, but
that cleanup is not release authority.
PV-backed draw-resource relations use the detailed gates in
`CommentaryTruthBoundary.md`: played-move-bound legal replay, draw-stable score,
and the relation-specific stalemate or repeated-check witness. The trust rule is
that raw draw-resource motif text, detector labels, unvalidated or unbound probe
lines, and helper notation remain support-only. Validated root `ProbeResult`
reply PVs may feed relation metadata through that truth boundary, but only the
top-PV witness can project the product summary rows `Stalemate resource` and
`Perpetual check`.
Legacy motif-prefix prose, theme-keyword and canonical motif-term prose, and
motif appears/fades delta prose consume that same catalog boundary:
deferred practical/thematic fallbacks use softer wording when present, while
witness-only raw relation tags such as `trapped_piece`, `domination`,
`zwischenzug`, `stalemate_trap`, and `perpetual_check` produce no prefix, theme
keyword, canonical term, or delta motif sentence.
Deferred motif names and witness-only raw relation tags therefore cannot bypass relation
admission through generic narrative text or `conceptSummary` aliasing. The
legacy plan matcher still falls back to `key-square restriction` for raw
domination-style plan evidence instead of emitting `domination` as a plan
evidence term.
That fallback label comes from the implemented relation descriptor's
witness-only fallback field; adding or changing such labels must happen in the
catalog descriptor rather than in consumer-local string branches.
`UserFacingSignalSanitizer` applies the same rule to leaked helper notation:
`Zwischenzug(...)` is rewritten to `move-order caution`, PV-only
`StalemateTrap(...)` and `PerpetualCheck(...)` helpers plus mate-pattern-only
`SmotheredMate(...)` are removed rather than named, and raw relation cleanup
does not grant relation authority. `UserFacingPayloadSanitizer` filters raw
deferred, witness-only, and PV draw-resource helper tags out of strategy-pack
public support metadata before fallback-text cleanup, then keeps the post-clean
denylist, so helper notation cannot survive there as generic support text.
`NarrativeContextBuilder` candidate tactic evidence also consumes catalog-owned
witness-only fallback labels for raw `Domination` and `TrappedPiece` motifs, so
annotation prose cannot reintroduce those raw relation names without a board
witness.
MoveReview no longer has a public thematic fallback prose lane. If planner,
typed basic, and exact factual fallback authority all fail, the slot is omitted
rather than filled by active-plan-theme prose. Fresh MoveReview early-opening
prose clamping still consumes the truth contract before compact intro-only
wording can shorten tactical or critical prose.
Threat-summary labels in `NarrativeContextBuilder` consume the same catalog
fallback, so relation-shaped motifs from `ThreatAnalysis` do not surface as raw
key-threat labels, and PV-only draw-resource tags do not become public threat
summary names.
Strategy-pack and structure-arc piece-activity evidence no longer converts
generic trapped-piece activity into a catalog fallback evidence term; raw
`trapped_piece` relation labels require the board-replayed relation witness. The
user-facing sanitizer applies the same catalog check to legacy strategy-pack
support metadata and move-review ledger prerequisites, removing deferred
relation motif terms, stale `deferred_*` fallback tokens, and witness-only raw
relation tags from evidence, long-term focus, side-plan priorities and risk
triggers, and directional target reasons and prerequisites while preserving
ordinary non-deferred support terms.
The same catalog check blocks deferred relation tags and witness-only raw
relation tags from raising the context beat into high-tension tactical
opening tone; that tone still needs an implemented relation witness,
non-deferred motif, or concrete threat evidence.
The generic motif-prefix allowlist also checks the deferred relation catalog
and witness-only relation boundary before accepting a motif signal, so later
prefix consumers inherit the same non-relation boundary.
Generic fact-corroboration helpers share that catalog check: a tactical fact can
still support implemented or generic motifs such as battery, but cannot certify
a witness-only raw relation tag such as `trapped_piece`.
The catalog keeps implemented and deferred motif names in one inventory, but
only implemented descriptors can satisfy runtime evidence admission.
`StrategicIdeaEvidence` also strips unknown or uncataloged relation names
instead of preserving them as relation candidates. Unknown relation witnesses fail
closed at semantic observation emission instead of falling back to
target-pressure or other selector sources.
The relation semantic producer uses the implemented catalog relation set rather
than maintaining a separate relation-kind allowlist.
The analyzer-owned relation target is preserved as `targetSquare` through the
semantic observation, selector evidence, and selected `StrategyIdeaSignal`;
the selected idea also keeps the representative `relationKind`, so public
projection does not re-pick a different relation merely because of catalog
ordering.
If a selected relation carrier lacks an analyzer target, public target fallback
also comes from that relation's catalog descriptor; the payload builder does not
own motif-specific target order rules. Evidence-only carriers without a
selected `relationKind` cannot promote their generic `targetSquare` or source
fact pair into a public relation target or row.
The selector prioritizes that relation's source ref, semantic fact, and
descriptor-owned analyzer witness fact before generic evidence truncation,
preserving the catalog admission triple needed for public projection.
Catalog-order fallback is closed: evidence without a selected `relationKind`
remains internal support and emits no public relation row. A named relation with
mismatched source/semantic/witness evidence likewise remains internal support
and emits no public relation row.
The selector also preserves `relationFocusSquares`; surface projection uses
only that relation-specific focus and never borrows merged idea focus. If a
selected carrier lacks relation-specific focus, the public relation row stays
silent. A selected relation target is used only if it is still present in the
relation focus squares, then falls back to the catalog descriptor's target
policy over that same relation focus. Selector merge does not upgrade ordinary focus squares
into relation focus or ordinary target squares into relation target after
evidence grouping.
`StrategyPackBuilder` may keep a strategy pack alive from relation-only
selector ideas, but only after that same replay-backed semantic producer and
catalog mapping have emitted typed evidence. The pack's existence is therefore
support carriage, not proof authority.
They may improve practical ranking or softer labels; owner release still
requires the contract witnesses and authority resolver admission above.
For defender-trade and bad-piece owner witnesses, `PlayerFacingTruthModePolicy`
now uses the analyzer `RelationWitness` typed details and facts as the shared
branch read-model before adding owner-only transition terms, so semantic support
rows and owner packets are not built from divergent branch strings. Policy code
consumes analyzer branch-extraction helpers instead of reading relation
detail internals directly; the policy also forwards those typed branch details
as exact-slice proof payloads for player-row validation without upgrading the
packets beyond SupportedLocal. Shared owner-seed and transition term expansion also
comes from the analyzer relation projection rather than policy-local reads of
relation facts or focus squares. Other implemented relation witnesses also carry typed king,
attacker, defender-or-cleared-square, mate-pattern, material-target, line-target,
or lure-and-win details;
they remain support authority unless a separate proof contract admits them, but
downstream code must still consume those details through analyzer helpers
instead of reparsing support facts.
Branch keys and `branch:*` fact tokens are formatted by
`MoveReviewExchangeAnalyzer` helpers as well, so owner packets do not rebuild
the replay branch with policy-local UCI slicing or ad hoc pipe-joined strings.
Semantic observation projection now goes through the analyzer-owned
`relationProjectionFromWitness` carrier for relation kind, focus, target, and
support fact terms, so support rows cannot be steered by stale raw focus/target
values or fact strings. Every implemented relation projection requires matching
typed relation details; missing details and mismatched details both fail closed
before semantic emission rather than being softened through raw witness fields.
Semantic fact projection and owner/transition term expansion also use the
analyzer relation projection, preferring canonical detail terms over raw witness
facts. Missing or mismatched typed details emit no branch, owner-seed, or
transition terms.
Discovered-attack support keeps enough selector priority to survive alongside
the generic clearance signal that often shares the same cleared ray; this is a
support-label preservation rule only and does not promote discovered attack to
owner proof.
When these rows reach `moveReviewPlayerSurface`, the only public row authority
is `strategic_relation`: a sanitized relation key plus a required square target.
It is support metadata for badges and chips, not a proof packet and not a raw
carrier that the frontend may reinterpret as ownership. The backend projection
requires the catalog source ref, matching semantic observation fact, and
descriptor-owned analyzer witness fact, so a source-only or
source-plus-semantic-only hint cannot mint a public relation row.
Descriptor-specific relation rows, including tactical, draw-resource,
move-order, and mobility-restriction rows, may name only the
analyzer-projected focus-order squares already carried by that witness.
That display detail is not permission to recover piece roles, branch ownership,
or conversion truth from raw strings. Defender-trade or bad-piece wording in
this row remains support-only; exact owner proof still requires the dedicated
typed owner packet.
Line-geometry relation rows may likewise name focus-order squares as
role-neutral geometry only; pin/skewer rows do not claim absolute pins,
material wins, axes, or piece roles unless another typed public contract admits
that stronger claim.
`MoveReviewPlayerPayloadBuilder` may carry up to four such catalog-approved
relation rows in `advancedRows`; this is a display-cap expansion only and does
not change catalog admission, target requirements, or owner proof status.
The outer player-surface advanced-row cap is sized to preserve the relation,
compensation, and typed structural practical caps together before lower-priority
plan-detail rows; this affects visibility only, not authority admission.
If a supported-local practical row already names the same relation kind from a
board/PV witness, the builder withholds the duplicate `strategic_relation`
advanced row for that kind. Exact defender-trade and bad-piece-liquidation owner
packets may expose the cataloged relation token on that summary row only after
the resolver admits the packet and the typed exact-slice proof matches its
source/family terms. This avoids presenting the same witness once as a specific
practical line and again as generic relation support; it does not relax catalog
admission for other relations or let prose mint relation authority.
The cached-payload sanitizer applies the same cross-row suppression after
sanitizing summary rows, and the suppressing row must still carry practical-plan
authority or exact defender-trade / bad-piece-liquidation summary
`strategic_relation` authority. A stale summary label without authority is not
trusted to remove a valid advanced relation row.
That admission check is centralized in `RelationObservationCatalog`, including
the rule that a selected `relationKind` must match its own source/semantic/witness triple and
must not fall through to another catalog entry. The descriptor itself exposes
the source ref, semantic fact, and required witness fact, so selector prioritization, payload
projection, surface priority, and tests share the same admission tokens.
PV-backed stalemate-trap and perpetual-check draw-resource rows may be ordered
ahead of generic relation rows under the same four-row display cap, but this
priority does not change their required legal replay, repeated-check or
stalemate, and draw-stable score gates. Standard relations keep the six-ply
replay prefix; only draw-resource stalemate-trap and perpetual-check witnesses
may use the twelve-ply top-PV prefix.
Supplemental analyzer witness facts cannot mint catalog authority: dynamic
facts reject semantic observation ids, evidence-source ids, proof-source ids,
proof-family ids, and `source:` wire keys, leaving only the descriptor-owned
source/semantic/witness triple as admission evidence.
The final user-facing sanitizer also strips raw top-level
`strategyPack.strategicIdeas`, so relation evidence does not leak as an
alternate public carrier beside the bounded surface row.
Both backend and frontend sanitizers gate `strategic_relation` tokens against
the implemented relation catalog; syntactically valid but uncataloged relation
keys are not trusted, and untargeted relation tokens lose structured authority.
The frontend allowlist is expected to mirror that full implemented catalog,
including trapped-piece, domination, zwischenzug, stalemate-trap, and
perpetual-check authority metadata projected from backend witnesses.
Backend sanitization also limits `strategic_relation` authority to
`moveReviewPlayerSurface.advancedRows` plus exact defender-trade /
bad-piece-liquidation supported-local summary rows whose label, cataloged token,
and square target match that exchange owner family; probe or author relation
authorities are treated as malformed public metadata and dropped. The analyse
frontend decoder mirrors that row-lane boundary for cached or legacy payloads.
`PlayerProseBoundary` may use the full relation inventory only as a negative
helper-symbol denylist, rejecting leaked helper calls such as
`Overload(...)` or `Zwischenzug(...)`; that prose gate does not admit relation
authority or soften raw helper notation into relation status.
The same reviewed-move identity rule applies to `PlayerFacingTruthModePolicy`
owner witnesses for defender trade, bad-piece liquidation, and IQP inducement:
legal top-line replay is necessary but not sufficient when the reviewed move is
absent. Defender-trade owner visibility also ignores raw carrier prose:
`defender`/`guard` plus `trade`/`remove` text may feed support or practical
context, but exact defender-trade ownership needs typed family context and the
replayed defender-target witness. Generic plan-owner fallback must also strip
the exact defender-trade, bad-piece-liquidation, and queen-trade-shield
families unless the corresponding replay witness is present; those family names
alone are not release authority.
`queen_trade_shield` and bounded simplification exchange-square witnesses also
consume this analyzer: legal queen capture plus king recapture is required for
queen-trade shield, and legal immediate capture plus recapture is required for a
bounded simplification exchange square. Raw matching destination strings are
not trust evidence. For these replay-backed favorable-exchange owner packets,
the branch key is taken from the replayed witness line. Generic MoveReview
best-defense branch keys, best-defense move terms, exact-slice continuation
terms, and IQP inducement prefixes also use bounded legal replay; raw PV move
strings may remain display text, but cannot create branch proof, continuation
proof, isolated-pawn transition evidence, or tactical line-only proof. SAN
capture/check glyphs in candidate text are display hints unless a replayed
engine PV satisfies the tactical window. Exchange-forcing line evidence also
requires a replay-backed exchange packet with branch and continuation/structure
witnesses; line text containing `x`, `trade`, or `exchange` cannot create that
line hook. After the packet is replay-backed, concrete SAN capture tokens such
as `Bxc6`, `dxc6`, or `Qxd8` can satisfy the line's exchange cue. Strategic
line-scoped support consumes the same `lineShowsMainPathDelta` gate as
engine-generated move-delta evidence instead of a separate line-support
mini-gate, while active strategic delta text containing exchange/trade/
simplification language still cannot open `ExchangeForcing` truth mode without
one of the replay-backed exchange witnesses.
The same rule now covers quiet-move best-defense metadata and central-break
timing branch/PV witness terms. Break-prevention route evidence accepts only a
legal replayed played line, best-line prefix, or played+continuation splice;
invalid splices are dropped instead of becoming route proof. `L3.ThreatAnalyzer`
can use MultiPV score gaps as numeric context, but capture-derived threat
claims, attack squares, and best-defense UCI metadata must come from a legal
first-step replay. `StructurePlanArcBuilder` route-contribution prose also
requires the reviewed move to replay from the current FEN before it can say the
move started, reached, guarded, or connected a deployment route. Resource-removal line authority is also replay-only:
`citationLine`, `whyNot`, and rendered resource phrases cannot promote a
`ResourceRemoval` owner without a replayed break/file/square hit. Active
resolved-threat/lost-motif text about resources or defensive cover is therefore
only a softer counterplay-reduction signal unless replay supplies that hit.
Plan-level favorable-exchange matching is intentionally softer: raw
`RemovingTheDefender` motifs and broad defender/removal reason-code text cannot
mint a `defender_trade` subplan. They remain generic simplification support
unless the replay-backed semantic producer or owner witness proves the branch.
MoveReview owner seeding applies the same boundary. Broad defender/trade prose
does not mint `trade_key_defender`; a legal immediate exchange may only degrade
to `simplification_window` when it also has a move-linked exchange cue and a
narrative anchor.
The same rule applies to concept-summary exposure: raw
`RemovingTheDefender` positional tags are downgraded to generic `Exchange
pressure` instead of exposing a defender-removal concept without replay proof.
MoveReview basic-lane prose follows the same physical-board boundary for
capture semantics. A reply is a recapture, and a continuation is a center
capture, only when replay from the coupled line FEN says the move captures on
that square. Pin/skewer line confirmation likewise requires a replayed
continuation move that actually touches the relevant tactical square. SAN `x`
and UCI file-change patterns cannot create those labels.
Practical weakness-target rows are support-only, but their branch outcome still
must be board-coherent: a cached `resultingFen` is trusted only when the PV
legally replays to the same board state.

Opening-family prose claims are also kept out of the API presentation layer.
`OpeningFamilyClaimResolver` owns the claim-boundary decision from a structured
catalog family wire key plus `OpeningFamilyMatchProof` (`opening`, phase, ply,
FEN). The legacy `OpeningFamilyId` enum is a compatibility facade only; new
catalog family rows must not require resolver enum edits.
A structured family claim is `SupportedLocal` only when the opening label and
static `OpeningNameLookup` ECO/opening-book FEN result both match the requested
family. Static book matching may use same-EPD aliases retained by
`OpeningNameLookup.lookupAll`, so move-order transposition endpoints are not
lost when canonical display lookup picks a different row. `OpeningFamilyCatalog`
owns aliases, display labels, and target-square allowlists as main-resource TSV
data; malformed or unknown family/target pairs fail closed in the sanitizer and
unknown claim keys fail closed in the resolver. Shallow piece-square structure
predicates and strategic `TranspositionPvAligner` proofs are not used as opening
truth and cannot independently certify transpositions or coincidental later
positions.
Raw rendered sentences are not parsed for opening-family authority or
post-render suppression. `CommentaryApi` no longer splits rendered prose into
sentences or rewrites unsupported opening-family text after rendering; family
mismatch must be excluded or suppressed before surface prose is built.
The MoveReview public surface may expose an `Opening family` support row only
through `MoveReviewPlayerPayloadBuilder`, after the structured opening name is
matched through `OpeningFamilyCatalog` and the resolver admits the same family
as `SupportedLocal` for phase, ply, and FEN. This prevents stale explorer
labels, broad opening-phase text, or cached prose from becoming row-level
authority. Static book expansion is intentionally data-only: broader
`openings.tsv` variation coverage may make more real positions eligible for
the same resolver decision. The removed broad-variation Scala fixture floor is
not coverage authority; runtime rows still must not bypass the label-plus-FEN
proof pair or infer target authority from the variation name. Optional
`openingBook` metadata on that row is sanitized aggregate display data only:
ECO, positive total-game count, and up to three SAN top moves from
`OpeningReference`. It is allowed only on `opening_family` authority; sanitizer
and frontend decoder drop it from other authority shapes and never expose raw
explorer responses, sample games, source ids, or audit-cache provenance.
`OpeningReference.sampleGames` may support internal opening-relation replay via
`OpeningPrecedentBranching`, but that support is not public authority. Full PGN
samples are reduced to SAN route tokens after tag-pair lines and game results
are stripped; PGN headers, event labels, source ids, and catch-all macro-family
keys such as `other` or `unknown` must not become precedent moves or peer-match
authority. Planner-owned opening-relation replay additionally needs enough
precedent (`totalGames >= 2` or at least two sample games) and either an
explicit opening event or the early opening-data ply window. A self-only corpus
fallback can remain diagnostic/support material, but it must not own an
opening-relation claim.
Static opening expansion is paused while the pool is provenance-cleaned.
`OpeningPoolAudit` and `OpeningMasterDbAuditRunner` classify malformed PGN
tails, normalized endpoint transposition duplicates, and optional masters
evidence from the configured endpoint (`--base-url`, defaulting to Lichess).
The current pool is pruned to 1276 rows that replay against captured Lichess
masters evidence as `master-backed`; 438 live-audited `not-found-in-masters`
expansion rows were removed rather than treated as real-game occurrence.
The query window may be pinned with `--since`/`--until` only for endpoints that
accept date windows; current Lichess `/masters` live audit should normally run
without them because date-windowed master queries can return `HTTP 400`. Live
master evidence requires OAuth; live runs may write replayable raw-response
JSONL with `--write-evidence-cache`, and cached evidence must be a replayable
JSONL row containing the same masters response keyed by endpoint-stable audit
`rowId`; legacy line-number rowIds are replay-only compatibility. Fetch or parse
failures are `master-fetch-error`, not clean absence evidence. Both evidence
forms remain tooling support; without them, rows are `unverified`,
`not-found-in-masters`, `transposition-duplicate`, `master-fetch-error`, or
`quarantine`, not player-facing truth. `provenanceStatusCounts` and
`--only-status` are cleanup triage aids only.
`opening_families.tsv` aliases, including Benko/Volga labels under the Benoni
family, only broaden catalog matching for this `SupportedLocal` resolver path;
they do not create board truth or target proof by themselves.
Opening-goal prose expansion is also bounded to the existing carrier. New
`OpeningGoals` entries for Gruenfeld `...d5`, Slav/Semi-Slav `...e5`, Dutch
`...Ne4`, Queen's Indian `...Ne4`, Bogo-Indian `...Ne4`, Catalan `dxc5`
tension release, Open Catalan `c4` pawn recovery, Sicilian `...c5` c-pawn
challenge, and King's Gambit `f4` break may influence outline/explanation
wording only after the post-move board pattern and engine score produce
`openingGoalEvaluation`. Player-facing MoveReview title/prose uses bounded
opening-idea phrases from that carrier and keeps raw catalog goal names in typed
fragments/tags rather than in public sentences. Achieved or partial c/f-pawn and
other board-pattern opening-break goals may also surface as `Opening break`
practical support rows through `MoveReviewSupportedLocalSurfaceRows`; the row
uses `PracticalPlan` authority with no token, requires the reviewed pawn move to
match the selected `OpeningGoals` trigger and the legal top PV first move,
remains blocked by tactical truth vetoes, and does not become opening-family,
target, exact central-break, or lesson authority. Achieved Dutch, Queen's
Indian, and Bogo-Indian `...Ne4`
outpost goals may surface as a separate `Opening outpost` practical row only
when the goal evidence names the outpost square and the reviewed move legally
replays as a knight move to that square and the legal top PV starts from that
same reviewed move. A bare `OpeningReference` can
keep that carrier open
only inside the early opening-data window; later non-opening phases need an
opening phase or explicit opening event, so stale opening labels do not
reclassify middlegame/endgame moves as opening-goal prose. Opening goals must
not act as family admission, target authority, exact central-break timing
authority, or truth-contract evidence.

Current strict rules:

- `PositionLocal` scope alone never admits `WhatMattersHere`. A position probe
  must be a certified exact-slice packet or a supported-local packet with an
  accepted contract and no contract failure codes. `experimentConfidence` is
  not an admission bypass.
- exact owner slices require certified source/family predicates plus a typed
  `PlayerFacingExactSliceProof`; generic witness strings in anchor or
  structure terms do not satisfy `ExactSlice`, and downstream policy must fail
  closed instead of reconstructing a proof object from those strings.
- outpost occupation is exact-slice only: a supported-local `Knight outpost` row needs
  `OutpostOccupation("knight", square)`, legal reviewed-move replay to that
  outpost square, top-PV identity, mirrored `piece:knight` and
  `outpost_occupation:knight:<square>` terms, proven branch, and stable
  persistence. Its public `PracticalPlan` authority may preserve only that same
  outpost square as `target` on the builder-owned `knight` wording; approximate
  or non-knight outpost labels remain
  non-authoritative.
  Current witness: `6k1/8/8/8/3P4/5N2/8/6K1 w - - 0 1`, played `f3e5`, top-PV
  line `f3e5 g8f8 d4d5 f8e8`, `Fact.Outpost(e5, Knight, Now)`, and
  `OutpostOccupation("knight", "e5")` in the `outpost_entrenchment` packet.
- favorable-exchange labels do not prove their own owner path. Defender-trade
  and bad-piece-liquidation claims require bounded legal PV replay plus the
  structure-transition witness described above; otherwise they may remain
  diagnostic or degrade to `ExchangeSequence`, `MaterialTransition`, practical
  target, or omitted MoveReview prose when tactical truth allows no stronger
  surface.
- polish and repair may paraphrase the slot claim's opening clause for natural
  prose only while preserving concrete slot facts, SAN tokens, move numbers, and
  square anchors. They must not replace an admitted slot with a generic
  restatement or reintroduce sidecar/system language. Repair may keep a
  paraphrased first paragraph when the reviewed move anchor and a bounded claim
  fact remain present, instead of restoring the deterministic claim solely
  because the prefix words changed. Polish validation may canonicalize SAN case,
  tolerate duplicate draft SAN mentions being stated once, and allow equivalent
  or omitted eval tokens; invented SAN, order inversion, wrong move-number style,
  changed eval values, placeholder, system-language, and unanchored abstract
  claims still fail closed. Anchored ordinary chess phrases such as counterplay
  restraint, coordinated pieces, or holding a position together are not rejected
  solely by phrase match.
- break/file-axis admission uses the centralized `BreakFileToken` parser; a
  plain prose word or incidental `a`-`h` letter is not evidence for a file.
- position-probe question seeds must use the exact FEN being generated. The
  Carlsbad fixed-target seed remains closed unless the exact mirror board
  target (`c6` for White pressure, `c3` for Black pressure) and
  minority-support predicate both pass. That predicate allows the b-pawn to be
  on the original or advanced minority-attack squares (`b2`/`b4`/`b5` for White,
  `b7`/`b5`/`b4` for Black) but still requires the matching d-pawn chain support.
- dynamic weakness targets are board facts, not owner claims by themselves. The
  canonical `WeaknessTargetProfile` may feed bounded practical target rows,
  guard exact target-fixation witnesses, and serve as the endpoint fact for
  `TranspositionAligned` only after legal PV replay and veto checks pass. Target
  text, broad focus-square lists, or `targetPressureDelta` do not certify a
  claim without an accepted typed proof path. A generic exact target-fixation
  surface with multiple focus squares must name the selected square in the same
  idea id or typed evidence refs before it can become public owner evidence.
- support material never enters the owner pool directly.
- tactical truth veto outranks strategic authority. The only soft path is a
  non-tactical surface with a present narrative context, a present truth
  contract, no tactical-failure contract, no severe counterfactual, and an
  observed win-percentage drop of < 10.0pp (with further limits down to 5.0pp
  for forcing moves). Missing narrative context fails closed for supported-local
  position probes and tactical-vetoable surfaces; a missing truth contract is
  not by itself tactical-veto evidence when a narrative context is present, but
  it also cannot open the soft-veto path.
- line-scoped claims may survive only as subordinate evidence unless a main
  path strategic claim is independently admitted.
- support-only carriers may not re-inflate after certification failed closed.
- timing-witness admission is structured-token only. UCI moves, exact board
  squares, and piece-square anchors may couple non-neutralize timing witnesses
  only when their proof contract explicitly allows it. For
  `neutralize_key_break` / `counterplay_axis_suppression`, only the planner
  named-break token and typed exact-slice packet token may match; packet owner,
  anchor, structure, continuation, and raw claim terms are diagnostics, not
  authority.

Heavy-piece local-bind release vetoes are exact-replay risks, not generic
heavy-piece movement heuristics. A release may be signaled by a true deep queen
infiltration, a rook lift/switch away from the back-rank shuffle case,
repeated heavy-piece checks, or an exchange sacrifice where a rook captures
lower material and is then recaptured on the replayed branch. A queen
centralization/single check, an unrecaptured rook capture, or a back-rank rook
shuffle must not by itself create `heavy_piece_release_illusion`. File and
entry persistence must come from typed route validation signals: validation
purpose plus exact `Counterplay` resolution, exact denied file removal, and
exact denied square removal for the entry. English phrases in `keyMotifs`,
`planBlockersRemoved`, `planPrereqsMet`, or target text are diagnostics only.

Color-complex squeeze is promoted only through an exact board-backed position
probe:

- proof family: `color_complex_squeeze`
- proof source: `color_complex_squeeze_probe`
- status: `Releasable`
- certified eligible: true
- supported-local eligible: true
- default failure: `color_complex_authority_closed`
- requirement: FEN parses; the opponent owns the semantic weak square; the
  weakness is color-complex/hole/fianchetto tagged; a friendly bishop or knight
  actually attacks that weak square on the board; surface/semantic evidence
  points to the same square; the typed exact proof's declared color and
  bishop/knight geometry match that target; best-defense branch, same-branch
  proof, and stable persistence survive the packet boundary; packet witness
  terms mirror the same weak square, color-complex token, role-specific
  `minor_piece:<role>_<square>` term, and minor-piece attack pair. Coordinate
  and minor-piece words are trace terms only, not authority.
- MoveReview player-surface projection is limited to the same admitted packet:
  `MoveReviewSupportedLocalSurfaceRows` emits a `Color complex` summary row only
  after `ClaimAuthorityResolver.decidePositionProbe` returns `CertifiedOwner` or
  `SupportedLocal` without vetoes and the typed exact proof plus mirrored
  witness terms match the packet. Term-only, term-mismatched, and tactical
  truth-mode packets stay closed. Backend sanitization and frontend decoding may
  preserve only the same typed weak-square target on the exact `Color complex`
  row when the row keeps builder-owned bishop/knight and dark/light
  color-complex wording and the target-square color plus minor-piece attack
  geometry still match; approximate, non-minor-piece, invalid-geometry, or
  malformed complex labels remain non-authoritative.
- The lower-authority color-complex `Practical space` context row does not carry
  target metadata and is suppressed only by an exact same-complex `Color complex`
  row with builder-owned bishop/knight wording plus target authority.

Readiness scans and exact-FEN review artifacts are local evidence for future
authority review, not runtime admission.

## Render Trust Boundary

`FragmentAuthority` owns render-release tags:

- render-only text may release
- support-only text may release only when sufficiently grounded
- unsafe truth text is dropped
- unsafe lesson text is dropped
- future-lesson candidates are dropped
- move-linked-anchor fragments require their anchor

`NarrativeOutlineBuilder` assembles beats; it does not own release legality.
`NarrativeOutlineValidator` remains the final scrubber.
`UserFacingSignalSanitizer` strips raw diagnostic labels such as `theme:` and
`subplan:`, but preserves canonical player-facing headings such as
`Key theme:`. `FullGameDraftNormalizer` may rewrite sanitized key-theme
scaffolding into prose; that rewrite is cleanup only and does not create
authority.

## API And Frontend Trust Boundary

`CommentaryApi` must serialize typed surviving payloads without recomputing
authority.

MoveReview fallback/retry handling is backend-owned. `CommentaryController`
emits `diagnostics.status` and `diagnostics.sourceModeReason` from
`MoveReviewResponseDiagnostics`, which evaluates final fallback prose through
`PlayerProseBoundary` and existing polish/source-mode codes. The frontend uses
`diagnostics.status == retryable_fallback` to retry or ignore a fallback
response; it must not parse commentary prose, English phrases, helper labels,
or source-mode prefixes to decide retryability.
Template-quality polish skipping is language-aware. English marker scoring may
skip polish only for English requests; non-English text is not judged by the
English marker list.

MoveReview player-visible support UI is owned by the backend-certified
`MoveReviewPlayerSurface` payload built by `MoveReviewPlayerPayloadBuilder`.
The public MoveReview wire is built by `MoveReviewResponsePayload` and no
longer emits raw `strategyPack`, top-level `signalDigest`, author
question/evidence summaries, concepts, plan-tier/commentary-mode controls,
full `mainStrategicPlans`, or `strategicPlanExperiments`. It exposes
`mainStrategicPlanCount` for UI metadata instead of plan arrays. `probeRequests`
remain a compatibility array on the public schema, but the current sanitized
MoveReview response emits it empty; public MoveReview does not expose raw probe
orchestration requests. Public polish metadata excludes validation reasons,
token counts, cost estimates, and strategy-coverage diagnostics; fallback/retry
state is owned by `diagnostics.status/sourceModeReason` instead. Frontend
decoding and stored snapshot restore strip those internal polish fields from
stale payloads before they can become DOM metadata.
`MoveReviewPlayerPayloadBuilder` must not read raw `strategyPack`, raw
`signalDigest`, or outbound `probeRequests` to create product rows; it may
project only already bounded surface inputs such as selected evaluated plans,
the certified explanation, the strategic ledger, refs, and authoring summaries.
Fresh API construction passes the derived `DecisiveTruthContract` into the
builder. If that contract indicates a blunder, missed win, bad tactical
refutation, or tactical failure mode, strategic relation rows, practical plan
rows, opening-family support, compensation support, and supported-local rows
are suppressed instead of appearing beside failure prose. The planner owner
trace consumes the same `DecisiveTruthContract.blocksStrategicSupport` boundary,
so tactical-failure mode cannot be masked by quiet-intent, opening, endgame, or
strategic-plan scene translators. Fresh MoveReview prose compression applies
the same boundary to the basic-explanation lane: positive opening/strategy/basic
sources are skipped under strategic-support blocks, forcing tactical or exact
factual fallback instead of a quiet positive explanation.
Decision-comparison UI follows the same boundary. `LineConsequenceEvaluator`
may summarize a PV/ref line, but only `MoveReviewRefs` lines that pass
`MoveReviewPvLine` legal FEN replay can become `SurfaceCandidate`. Legal
engine-only `VariationLine` summaries can become `ReplayBackedInternal` for
internal decision evidence, narrative hooks, and ledger notes, while remaining
blocked from the product decision strip by release type and the engine-only
surface-blocking reason. The ref path stays strict across the checked line.
Engine-only internal summaries may use only the legal prefix that already proves
a concrete local consequence such as an exchange sequence, material transition,
central pawn advance, or replayed checks. Stale tail moves are removed from the
internal evidence and cannot carry mate/check proof; a legal prefix that is only
preview text remains diagnostic-only. Probe-request reminder prose such as
`Further probe work still targets ...` is also not branch-scoped line evidence;
the planner must not wrap it as a `One concrete line...` citation.
Close-candidate alternative PV prose may support MoveReview text only through
`AlternativeNarrativeSupport` plus `ContrastiveSupportAdmissibility`. The
admission boundary requires an enriched comparative sentence (`while`,
`whereas`, or `both`) backed by exact-FEN replayed branches; raw close-candidate
support remains rejected and cannot re-inflate into an owner claim. Enriched
close-candidate prose must preserve the near-top/two-good-moves boundary by
describing viable branch contrast instead of projecting a hard `best`/inferior
claim from support-only MultiPV material; planner shadow traces label enriched
close candidates separately from raw close alternatives.
`MoveReviewPlayerPayloadBuilder.decisionComparisonSurface` is the release gate
for `moveReviewPlayerSurface.decisionComparison`; it requires two comparable
move labels or a same-first-move comparison with a typed later consequence, a
>=35cp gap or exact comparative/practical alternative reason, a
surface-candidate line consequence, and no surface-blocking line reject reasons.
Gaps below 60cp are labeled as slight rather than as a clear engine preference.
It does not expose deferred moves or raw proof/source metadata, and it does not
promote plan authority. It may attach `targetComparison` metadata only after
the decision strip is already admitted. That metadata compares the engine-best
line and reviewed/chosen line endpoints through `WeaknessTargetProfile`, and
requires a non-empty engine-best move head rather than a bare `resultingFen`;
it is not a proof packet and not a new source of certified plan ownership. Backend
and frontend parsing require legal square tokens plus authority-key-shaped
target kinds; malformed target comparison data is dropped for that optional
field rather than crashing the MoveReview surface or synthesizing substitute
guidance. Frontend rendering may use the validated metadata only as a compact
subordinate endpoint-target note inside the already admitted decision strip; it
must not create a plan claim or recover missing target comparison from raw
engine fields, prose, or fallback carriers.
Practical-guidance rows are lower authority than supported-local exact rows.
They may describe structural or PV-coupled practical plans, central
liquidation, or central challenge with cautious wording, but they cannot label
themselves as `CertifiedOwner`, `SupportedLocal`, `Central break`, or a main
plan. The frontend may style practical row tone and authority kind differently,
but that styling is a display affordance rather than a proof reconstruction
path.
`Practical target` rows are in the same lower-authority family: they can name a
board-derived weak pawn target, but they keep `practical_plan` authority and do
not publish `authority.target`. Current-board target context may publish without
a best-line persistence witness, and may mention only profile-provided
non-generic structure context as an educational prefix. Generic center
classifications such as Open/Locked/Fluid/Symmetric stay unspoken. That prefix
is not public motif or target authority. The row is suppressed when the best
available line legally liquidates that target by the defending side. When the
target is not yet in the current profile, the row may use a checked-line endpoint
target only with an exact square target hint and a trusted endpoint or at least
five legal UCI plies.
A bounded practical plan may steer this row only with an exact square target hint: `weakness_target:<square>`,
`fixed_target:<square>`, `coordinated_target:<square>`, exact
`target:<square>`, `target_fixing:<square>`, `enemy_weak_square:<square>`, or
`weak_complex:<square>`. The hinted square must appear in the current or
checked-line endpoint `WeaknessTargetProfile`; material/relation role tokens such as
`target:e5:queen` are rejected instead of being truncated into a weakness
target. They are suppressed when the best available line shows the target being
liquidated by the defender. If the pressure side captures the target, the
target is considered resolved rather than transient. Short persistent lines may
support current-board context, but they still cannot create a new endpoint-only
checked-line target.
The current supported-local product projections are narrow.
`MoveReviewSupportedLocalSurfaceRows` may surface at most three independently
admitted exact/supported-local summary rows for one move. This broadens product
coverage when, for example, a main-path counterplay-break packet and a
position-probe packet both carry valid typed proof, but it does not relax any
per-row admission gate, tactical veto, typed-proof shape, or proof-contract
failure rule. A route-network, dual-axis-bind, or restricted-defense typed
surface that passes resolver admission may reserve remaining slots after the
leading packet/plan row, so it is not silently hidden behind multiple packet
rows. If no exact/supported-local
row survives, the lower-authority practical fallback path stays bounded: typed
structural-idea rows may keep at most three distinct practical labels, while other
practical fallback chains remain single-row.
The detailed fallback family catalog and per-row board/PV gates are canonical in
`CommentaryPipelineSSOT.md`; this trust boundary records only the risk rule:
fallback rows are board-state guidance, require the legal top-PV reviewed move
plus the tactical-veto boundary, and do not create proof-contract, token/target,
owner, or strategic-relation authority. Mate/tactical/relation fallback rows are
evaluated before broader positional fallback rows so a general opening,
outpost, rook-lift, file-entry, or endgame support row does not suppress a more
specific top-PV tactical witness. Mate-net rows may narrow their public label to
`Smothered mate`, `Arabian mate`, `Boden's mate`, `Anastasia's mate`, `Hook
mate`, or `Corner mate`, but only from the matched `TacticalPatternDetectors` id
carried by the `MateNet` typed details; these labels do not create new proof
authority. The fallback cache consumes the first legal `BoundedReplayStep` after
matching the normalized reviewed UCI, instead of reparsing raw `playedMove` text
as a second authority check. Generic clearance remains an advanced
`strategic_relation` signal only; the practical-row no-duplication rule is
canonical in `CommentaryPipelineSSOT.md`.
Defender-trade and bad-piece trade fallback rows may use the analyzer-owned
typed relation witnesses, but they remain lower-authority practical support:
defender trade still needs a declared focal or weakness-profile target and a
strictly reduced defender count after the replayed recapture, while bad-piece
trade still needs the constrained bad-bishop liquidation branch. These fallback
rows do not relax favorable-exchange proof contracts or create exact owner
authority.
Queen-trade fallback rows use the existing analyzer legal replay helper and
remain in the same lower-authority lane: the first two top-PV plies must be the
reviewed queen capture and the opponent king recapturing on that square. They
name only the queenless branch and do not relax the `QueenTradeShield` exact
proof contract.
`neutralize_key_break`: `MoveReviewSupportedLocalSurfaceRows` can add a
`Counterplay break` summary row only from the same planner runtime used to
build prose, and only after `ClaimAuthorityResolver` returns `SupportedLocal`
for a `neutralize_key_break` timing plan or main-path packet claim from
`counterplay_axis_suppression` with an exact owner path and no tactical veto.
This is a bounded local-reading support row, not a `CertifiedOwner` upgrade,
not a proof-contract expansion, and not a tactical-veto relaxation. The row
must be built from the timing witness named-break token and/or the packet's
typed `PlayerFacingExactSliceProof.CounterplayAxisSuppression` token. On the
plan+packet path, those two structured tokens must match; on the packet-only
path, the typed token must still be mirrored in packet terms, but packet terms
are not parsed to recover a token. Raw claim prose and packet
owner/structure/anchor terms are not token authority. If
`BreakClampMaterializer` proves that the played move occupies the opponent
break destination, the trusted witness is the full route token (`e4-e5`,
`...b5-b4`), not the self-referential destination square. Tokenless packets,
generic fallback wording, and single-square tokens that collide with the played
move must not become product-visible `Counterplay break` rows. Collision
checking uses legal UCI replay from the current FEN and `move.dest.key`; SAN is
display-only, and missing or illegal UCI fails closed for single-square tokens.
The row must not
expose the internal `SupportedLocal`/local-reading label or raw proof
family/source metadata through public row `source`; it may expose only the
public row authority `{ kind: "counterplay_break", token: ... }`.
`central_break_timing`: product-visible `Central break` rows require the exact
runtime witness, including a non-capturing same-file d/e pawn advance to
`d4`, `e4`, `d5`, or `e5`, board link, source/family match, route-shaped break
token, and an admitted `SupportedLocal` main-path packet. Diagonal captures
(`d4-e5`, `...e5-d4`) and prep/challenge pushes (`...d7-d6`, `e2-e3`) are
separate reject shapes, not product-visible `Central break` rows; Sicilian
`...c5` and King's Gambit `f4` remain opening-goal prose support only. Public
`Opening break` practical rows additionally require the legal top PV to start
with the reviewed pawn move and the selected `OpeningGoals` trigger to match
that move, so an after-FEN opening goal cannot surface by itself or through a
different pawn break. The reviewed played move may seed the exact central-break witness when
it is itself that same legal
central break, even if the top PV does not replay it; this path still requires
the legal board move and the same packet/source/family boundary. PV gap and
two-move branch key are diagnostics for this row, not proof of monopoly value
and not hard release gates. Taxonomy, strategy-pack labels, raw claim prose,
and signal-digest text do not admit the row. The packet terms must mirror the
typed `CentralBreakTiming` proof's `break_move`, `break_token`, and central
break square before projection. The public row authority is limited
to `{ kind: "central_break", token: ... }`; raw proof ids stay private.
The Maderna exact scene is the current product-surface witness for this row:
FEN `nrb1r1k1/1pqn1pbp/p2p2p1/P1pP4/2N1PP2/2N2B2/1P4PP/R1BQR1K1 w - - 3 17`,
played `e4e5`, and route token `e4-e5`. This does not promote the separate
source-catalog Maderna row, which remains blocked in source review until its
owner/proof contract is admitted there.
`quietIntent`: product-visible `Piece improvement`, `King safety`, and
`Technical conversion` rows are practical support only. They require the
existing weak-main packet gate, no suppression or release risk, MoveLocal
scope, quiet-intent class alignment with the packet proof family, proof source,
and ontology family, legal replay of the reviewed UCI move from the current FEN,
and a square anchor that matches the replayed destination. SAN shape, raw
quiet-intent prose, mismatched support packets, or unanchored owner terms cannot
produce the row. Tactical truth modes suppress it, and counterplay-restraint
quiet intents remain internal to avoid competing with the exact
prophylaxis/counterplay public rows.
Forcing-defense scenes may only receive subordinate support wording; tactical
truth veto remains higher priority.

Compensation subtype matching is fail-closed for unknown subtype dimensions.
Generic compensation prose can still mention the theme, but it cannot satisfy a
specific subtype contract unless the theater, mode, recovery policy, and
stability class are recognized and the text carries the matching subtype
anchors. Delayed/deferred compensation requires an explicit recovery/defer
anchor rather than the word `compensation` alone. Target-fixing compensation
cannot be authorized by generic `pressure` or file-occupation route prose; it
requires target/fixed-pawn/weak-pawn language or typed target evidence, coupled
with FEN-based structural verification of target board states. Shared pawn
targets are resolved by `structure.PawnStructureTargets`, not by duplicate
Carlsbad or Benoni pawn-shape checks inside the compensation interpreter.
Player-surface compensation rows are allowed only from the same resolved
display boundary: `StrategyPackSurface.strictCompensationPosition`,
`compensationContractResolved`, a durable-pressure strict subtype, a strict
subtype label, and `CompensationDisplayPhrasing.compensationNarrationEligible`
must all pass, and a present `DecisiveTruthContract` with
`compensationProseAllowed=false` closes the row gate. They are lower-authority
`practical_plan` advanced rows, not
compensation-owner proof. The condition row prefers resolved persistence
wording over broader objective wording, and they do not expose raw
`signalDigest` fields.
Generic initiative shells, unresolved path/payoff subtype conflicts,
transition-only conversion stories, non-durable tactical windows, or unanchored
compensation text stay internal.
Opening-route target fixation additionally requires `OpeningRouteCatalog`
descriptor data and `PieceRouteEvidence` legal UCI/PV support. Route evidence
uses raw `VariationLine.moves` before parsed metadata and carries the replayed
terminal `Position`; `KnightRouteEvidence` remains only a knight-route
compatibility facade. `OpeningRouteTargetEvidence.checkRouteEvidence` validates the row's
`target_mode` on that terminal board, not on the root board before the route is
completed. If the slice declares a focal, directional, idea-focus, move-ref,
semantic structural-weakness, or root-board weak-pawn target, `findRouteWitness`
may only select catalog rows for that declared target. When the current
`openingData.name` or `OpeningEvent.Intro` resolves to an opening family,
route-witness admission is further limited to that same family and its target
allowlist; unlabeled positions still rely on legal replay plus target
allowlisting. Benoni `d6`, reversed Benoni `d3`, and King's
Indian `c5` routes reuse the same witness path; FEN substrings and fixed branch-key text are not
admission gates. The starter route pack extends data coverage for major
openings including Sicilian, Queen's Gambit, Slav/Semi-Slav, Nimzo-Indian,
English, Dutch, Scandinavian, Pirc/Austrian, Catalan, London, Bird, Queen's
Indian, Bogo-Indian, King's Gambit, Caro-Kann, French, Open Games, Gruenfeld,
Alekhine, and Nimzowitsch. The current catalog has 52 descriptors, including 4
bishop fianchetto descriptors that may enter exact target-fixation only when
the same legal replay, family/target allowlist, declared-target, and terminal
target-mode gates pass. Mined
additions require at least five master-backed opening-row witnesses, and
lower-support route candidates remain deferred. Route targets are mirrored in
the family target allowlist so public target metadata is not blocked by stale
catalog data after legal route evidence passes. Direct Queen's Indian and
Bogo-Indian `Nf6-e4` rows handle positions where book proof starts after
`...Nf6`, but route catalog membership
remains support evidence, not standalone claim authority. Battery
formation predicates require the moved piece and partner to share the declared
line with no blocker between them.
`CommentaryApi` passes those same selected evaluated plans into
`UserFacingPayloadSanitizer`; sanitizer does not admit strategic plans from
`probe_backed:validated_support` or `StrategicPlanExperiment.evidenceTier`
strings. Strategic-plan experiments may survive only in the internal sanitized
response model when their plan key matches a retained typed-admitted plan; they
are not emitted on the public MoveReview wire.
On cache hits, `CommentaryApi` uses the cache-specific sanitizer path so the
already sanitized fresh response does not lose continuity state or certified
ledger data on read, while legacy/marker-bearing strategic metadata remains
closed.
When authoring summaries were assembled from pending probe requests, raw request
purpose/objective/plan/seed metadata remains support-only and must not be
rendered as player authoring meta. Probe-backed and authoring-backed ledger
rows may use certified line/eval evidence, but not request/result
purpose/objective metadata, raw source IDs, row provenance/source metadata, or
`signalDigest` decision fallback text. Deferred decision moves are also not
admitted to the player surface; sanitizer and frontend decoding must ignore
them.
`MoveReviewPlayerPayloadBuilder.ledgerRows` treats the strategic ledger as a
bounded source of player-surface probe rows only when each line has a sanctioned
source (`probe`, `decision_compare`, `variation`, or `authoring`) and non-empty
SAN moves. Malformed ledger lines are dropped rather than repaired from raw
probe, authoring, signal-digest, or prose data.
The frontend must not decode raw probe or authoring carriers for orchestration
fallback. Public `probeRequests` stay an empty compatibility field, and the
post-response refined probe fetch path is closed; player support, advanced,
probe, authoring, and decision-comparison sections are not rebuilt from raw
carriers.
No Gzip/Base64 opaque strategic token is treated as a security or trust
boundary. Structured continuity tokens remain compatibility state until a
server-signed, versioned, expiring, request-bound token contract exists.
QC/report queue tooling follows the same rule: when `moveReviewPlayerSurface`
exists, support rows come from that surface rather than raw `signalDigest`,
`mainStrategicPlans`, or `strategicPlanExperiments`. In that tooling path,
`summaryRows` plus backend `decisionComparison` form support rows, while
`advancedRows`, `probeRows`, and author rows remain advanced review details.

Frontend code must not rebuild strategic meaning from:

- `topEngineMove`
- `cpLossVsChosen`
- latent/deferred fields
- support-only carriers
- raw `strategyPack`
- raw `signalDigest`
- raw `authorEvidence`, `probeRequests`, or `mainStrategicPlans`
- omitted decision-comparison data
- free-form helper prose

MoveReview and narrative views render only typed payload fields that survived
backend authority. For MoveReview support panels, that typed field is
`moveReviewPlayerSurface` schema `chesstory.move_review.player_surface.v2`.
Backend sanitization treats the schema as a structured identifier and preserves
the exact dot-separated v1/v2 value instead of prose-sanitizing it.
Rows may carry sanitized `authority` with only `kind`, `token`,
`openingFamily`, and `target`; malformed authority is removed by the backend
sanitizer, and the frontend decoder downgrades unsupported or malformed
authority shapes from cached/stale surfaces while preserving the row text.
Only `counterplay_break` may carry a square token; `central_break`,
`central_liquidation`, and `central_challenge` require route-shaped tokens.
Planner-owned threat-defense wording must not expose raw UCI coordinates as
player prose: where the current context can identify the played move it renders
the played SAN, and context-free contrast support lowers UCI-only defense
anchors to a generic defensive reply.
Cached v1 rows decode with no authority. Opening-family
authority may keep `openingFamily` only for sanctioned key shapes and may keep
`target` only for backend allowlist pairs from `OpeningFamilyCatalog`;
unsupported targets are stripped while the opening row may remain. The current
builder projection emits target metadata only when same-family legal route
evidence satisfies `OpeningRouteCatalog`, `PieceRouteEvidence`, and
`OpeningRouteTargetEvidence.checkRouteEvidence`; stale cached target metadata that
does not pass shape/allowlist checks is still downgraded. Practical-plan target
metadata is similarly retained only when the exact target label and
builder-owned row wording match the same square; exact-looking labels with
lower-authority context prose are downgraded to untargeted practical authority.
During fresh
MoveReview construction, `MoveReviewPlayerPayloadBuilder` may project the
current backend-built `moveReviewExplanation.shortLine` and bounded PV learning
point into an authority-free `Checked line` summary row; this is not a cached
top-level explanation reconstruction path. If that explanation line is absent,
the builder may use the preferred current-move `MoveReviewRefs` variation for
the same authority-free row instead of parsing planner prose. Legacy top-level
`moveReviewExplanation` is not a public fact-fragment authority: backend
sanitization strips `factFragments`, and frontend decoding ignores that field.
Legacy top-level `moveReviewLedger` may provide only metadata/signal attributes
after schema/key validation; malformed line rows are dropped and no top-level
ledger line renderer reconstructs probe/support rows. If the surface is
missing, no support panel is reconstructed.

MoveReview corpus and QC reports must measure the same player surface. When
`moveReviewPlayerSurface` is present, performance rows come from that field
only; raw carrier reconstruction is not used for MoveReview QC support rows.
Tactical-failure `neutralize_key_break` diagnostics must remain rejected or
veto-bucketed in QC and must not count as admitted product-visible support.
Tokenless or played-move-collision `neutralize_key_break` diagnostics are also
QC rejections, not admitted product-visible support.
Missing MoveReview raw artifacts or missing canonical surfaces do not authorize
raw-carrier fallback for MoveReview support rows.

Local-file entry bind promotion is exact-slice only. `HalfOpenFilePressure`
packets from `local_file_entry_bind` require the typed
`PlayerFacingExactSliceProof.LocalFileEntryBind(file, entrySquare)` plus the
owner/continuation/branch/persistence contract witnesses and matching board
support from the current FEN plus legal reviewed-move replay. The typed proof is
only attached when the entry square is on the claimed file; off-file certified
pairs remain planner support-only. Admission-unit coverage may queue
`OpenFilePressure`, `KeySquareDenial`, and `RookFileTransfer`, but the trusted
boundary remains the shared `local_file_entry_bind` exact slice rather than a
raw plan-kind source. The
MoveReview row is bounded to the named file and entry square; broad local-bind,
global-squeeze, and no-counterplay wording stays closed. The exact row may
expose only the typed entry square as practical-plan target metadata when the
public wording names the entry square's own file; practical top-PV file-entry
prose and off-file wording remain untargeted. Field-level mirror requirements
are owned by `CommentaryPipelineSSOT.md` and
`CommentaryTruthBoundary.md`.
The `prophylactic_cut` local-file scene is the current planner witness:
FEN `2r2rk1/pp3pp1/2n1p2p/3p4/3P1P2/2P1PN1P/PP4P1/2R2RK1 w - - 0 23`,
played `a2a3`, certified pair `c-file`/`b4`. That off-file pair is not public
MoveReview `LocalFileEntryBind` authority. Standalone key-square denial or
rook-transfer prose remains closed unless it remaps to that same certified
file-entry pair and matching typed proof.

Position-probe player rows are exact-slice consumers, not new proof lanes.
`ExactTargetFixation`, `CarlsbadFixedTarget`, `TargetFocusedCoordination`, and
`ColorComplexSqueeze` rows require a PositionLocal packet admitted by
`ClaimAuthorityResolver.decidePositionProbe` plus a matching typed proof case.
MoveLocal exact-target packets are not surfaced as position-probe rows, and raw
focus/coordination/color-complex terms without the typed proof stay closed. The
Carlsbad row and position-probe main claim may use bounded minority-attack
wording only from `CarlsbadFixedTarget(..., minoritySupport = true)`;
standalone minority-attack semantics stay support-only. Exact fixed-target and
Carlsbad rows may expose only the typed proof square as target metadata.
Detailed witness-term mirror rules remain in the truth and pipeline docs.
The separate MoveReview practical-context row may describe a Carlsbad-type pawn
shape as a natural minority-attack target for structural-only
Carlsbad/minority-attack or backward-pawn-targeting plans, or for a
structural-only plan whose exact `fixed_target:<c6|c3>` hint matches the
predicate's mirrored `c6`/`c3` shape. That row carries `PracticalPlan` with no
target metadata and must avoid checked-line, persistence, proof, or force
wording; it is not a public authority shortcut for `minority_attack_fixation`.
The current public `target_focused_coordination` rows are
`K09A-certified-coordination` and `K09D-certified-coordination`. Both stay
`CertifiedOwner` only because the fixture FEN plus legal PV replay establishes
coordinated pressure on `c6`, the packet is `PositionLocal`, and the typed
`TargetFocusedCoordination` proof mirrors `coordinated_target:c6`; tactical-veto
or term-mismatch variants remain closed. The public `PracticalPlan` authority
may preserve only that same `c6` target on the exact `Target coordination` row;
cached public wording must still show two distinct support squares, and
approximate or duplicate-support coordination labels remain non-authoritative.
Admission-unit coverage may queue plan-kind work units only when they map onto
an existing public runtime contract: `StaticWeaknessFixation`,
`BackwardPawnTargeting`, `FlankClamp`, `OutpostEntrenchment`, `IQPInducement`,
`SimplificationWindow`, `DefenderTrade`, `QueenTradeShield`,
`BadPieceLiquidation`, and `CentralBreakTiming` use their current proof
source/family contracts. `target_focused_coordination` remains a runtime proof
family, not a `PlanTaxonomy.PlanKind` admission unit. `FlankClamp` still opens
only through the existing `color_complex_squeeze_probe` /
`ColorComplexSqueeze` exact slice; generic `color_complex_clamp`,
`central_space_bind`, and `mobility_suppression` remain selector/support
evidence rather than public proof sources.
The admitted `source-botvinnik-vidmar-1936-flank-clamp`,
`source-botvinnik-vidmar-1936-e4-color-complex-squeeze`,
`source-camara-bazan-1960-d5-color-complex-squeeze`, and
`source-pfleger-maalouf-1961-d5-color-complex-squeeze` surfaces are therefore
narrow source-window witnesses, not a broad flank-clamp relaxation: they require
legal exact-ply replay, top-PV identity, `PositionLocal` admission, and matching
`ColorComplexSqueeze` proof facts before the ledger may project
`CertifiedOwner`.
Admission-unit reports must keep source-review surface authority separate from
proof-contract eligibility: exact position-probe units may admit
`CertifiedOwner`, while move-delta admission units such as
`SimplificationWindow`, `CounterplayRestraint`, and `HalfOpenFilePressure`
remain bounded `SupportedLocal` source-review surfaces unless the product
runtime exposes a distinct certified owner path.
Named route-network player rows consume only the certified
`RouteNetworkBindProof.SurfaceNetwork` already selected by
`QuestionPlannerInputsBuilder`. They require resolver admission, stay silent
for heavy-piece blocked or tactical positions, and keep intermediate route-chain
surfaces backend-only.
IQP, simplification, counterplay-restraint, local-file, outpost, and exchange
ownership rows all stay on the same SupportedLocal move-delta surface: the
resolver owns admission, typed exact-slice proof stays mandatory where the
contract requires it, and row text is bounded to the admitted board/PV fact.
Admission-unit or source-review coverage may queue these families, but it does
not widen owner authority or let labels replace packet proof.

## Current Risk Map

| risk | current control |
| --- | --- |
| support-only becomes owner | claim-authority kernel, proof contracts, planner adapter |
| fallback truth rewrite | truth contract first; no-contract fallback is failure-only |
| broad strategic overclaim | exact packet/certified slice required |
| plan promotion blocked by bookkeeping drift | hard/soft probe validation split in `PlanEvidenceEvaluator` |
| raw evidence-tier string becomes authority | `StrategicPlanEvidenceView` is the current runtime read-model |
| sibling score treated as refutation | `alternativeDominance` remains ranking metadata, not `Refuted` |
| renderer leaks unsafe prose | `FragmentAuthority` plus validator scrub |
| favorable-exchange label becomes owner | bounded legal replay plus defense/mobility structure-transition witness, matching typed exact-slice proof, proven branch, stable persistence, and supported move-delta admission |
| frontend rebuilds omitted meaning | `moveReviewPlayerSurface` for MoveReview product UI; no raw-carrier reconstruction |
| frontend parses fallback prose for retry state | backend `diagnostics.status/sourceModeReason`; no prose regex gate |
| QC reports measure a virtual raw surface | `buildMoveReviewRows` uses `moveReviewPlayerSurface`; absent surface yields no MoveReview support rows |
| tactical neutralize support leaks through diagnostics | `ClaimAuthorityResolver` tactical veto plus QC veto rejection for `neutralize_key_break` |
| generic or self-referential break support leaks | typed-proof/named-token surface gate rejects tokenless, term-only, mismatch, and played-move-collision `neutralize_key_break` rows |
| counterplay restraint inflates into no-counterplay claim | `ProphylacticRestraint` exact proof plus supported move-delta admission; row wording is limited to the prevented resource token |
| local-file bind inflates into global squeeze | `LocalFileEntryBind` exact proof plus FEN-backed file/entry board witness and supported move-delta admission; row wording is limited to file and entry square |
| named route network inflates into whole-route squeeze | `RouteNetworkBindProof.SurfaceNetwork` plus resolver admission; intermediate chains stay closed and row wording is limited to file/entry/reroute |
| dual-axis bind inflates into no-counterplay or whole-position squeeze | `TwoAxisBindProof.Contract` plus resolver admission; FEN-backed best-defense, future-persistence, and bounded-continuation evidence are required, and row wording is limited to the break/entry pair |
| restricted-defense conversion inflates into winning-plan proof | `RestrictedDefenseConversionProof.Contract` plus resolver admission; row wording is limited to a bounded technical-conversion support claim after the checked best defense |
| semantic outpost prose becomes owner truth | `OutpostOccupation` exact proof plus legal reviewed-move replay, top-PV identity, `Fact.Outpost` board fact, proven branch, and stable persistence |
| IQP prose becomes target truth | `iqp_inducement_probe` plus resolver admission, `IqpInducement` exact-slice proof, matching checked-line moves, and induced-pawn transition terms; row wording is limited to the isolated pawn square |
| simplification prose becomes conversion proof | `simplification_window` packet plus resolver admission and `exchange_square` continuation term; row wording is limited to the exchange square |
| position-probe target prose bypasses owner proof | PositionLocal packet plus `decidePositionProbe` admission and matching typed exact-slice proof are required |
| relation row prose widens relation truth | relation rows keep catalog token/focus/target authority; row wording varies by descriptor row kind, not display label, and cannot create proof/source/family truth |
| color-complex premature release | typed exact-slice proof plus authority-closed failure; MoveReview row projection also requires `decidePositionProbe` admission |
| lesson overgeneralization | broad lesson authority closed; scoped takeaway only |

## Expansion Naming Risk

Mixed expansion names are a trust risk because they can make a breadth label
look like proof authority. Treat these labels as non-authority aliases:

| alias | trust interpretation |
| --- | --- |
| broad heavy-piece/local-bind/global-squeeze expansion | umbrella for route/resource restriction work; not a proof family or module base |
| B7/B8 broad expansion | historical frontier shorthand; test/diagnostic language only |
| broad color-complex expansion | closed generic expansion; only `color_complex_squeeze_probe` can open `ColorComplexSqueeze` authority; lower-authority `Practical space` text does not publish target/proof metadata |
| mobility-cage expansion | broad catch-all remains closed; only cataloged `trapped_piece`/`domination` relation witnesses currently project a mobility-restriction row |
| broad lesson authority | broad lesson release, still closed; scoped takeaway remains the only local instruction lane |
| Chronicle/Active runtime reopening | closed under the removed-surface boundary in `CommentaryPipelineSSOT.md` |

Runtime implementation names should identify the chess asset and proof lane:
`LocalFileEntryBind`, `CounterplayAxisSuppression`,
`ProphylacticRestraint`, `ColorComplexSqueeze`, or a cataloged relation witness.
Names that describe rollout state or breadth, such as `broad`, `global`,
`Track`, `Frontier`, `B7`, `B8`, `Active`, and `Chronicle`, must not become new
proof families, public authority tokens, package names, or product row kinds.
If legacy terms remain in tests or diagnostics, the consuming runtime path must
translate them to a stable domain/proof boundary before any authority decision.

## CTH Priority Summary

Detailed historical B-frontier logs are no longer canonical in this file. The
current conclusion is:

- B1/B2/B3 exact slices remain maintained.
- B4/B5/B6 remain narrow bounded-scope results only.
- B7/B8 and broad color-complex, heavy-piece, mobility-cage, or global squeeze
  expansion remain design/recon territory.
- New authority must start from exact board positions, exact witness extraction,
  best-defense evidence where relevant, and proof-contract promotion.

Underlying evidence lives in local generated artifacts and targeted test/tool
reports. Those artifacts are evidence, not authority predicates.

## Broad Lesson Defer Rationale

Broad lesson authority remains closed because current local proof can validate
only a reviewed move, FEN, branch, and evidence tier. It cannot safely state a
general chess lesson without additional corpus coverage, exception handling,
and user-facing scope wording.

Allowed today:

- exact factual fallback
- bounded `SupportedLocal` phrasing
- `MoveReviewScopedTakeaway` tied to the reviewed move and branch

Not allowed today:

- broad rules such as "always" or "in every position"
- shared-lesson helper labels
- whole-position strategic truth from local support rows
- lesson claims from color-complex readiness rows

## Maintenance Triggers

Update this file in the same change when trust-relevant behavior changes in:

- fallback truth projection or rewrite behavior
- cross-surface contract consumption
- support-only carrier exposure
- proof-contract eligibility
- claim-authority resolution
- planner admission
- renderer release tags
- lexicon/template authority boundaries
- frontend support rendering
- scoped takeaway or lesson-readiness guards

Lexicon rule tables are wording infrastructure only. Adding or reordering
`NarrativeMotifPrefixTable` templates must not be treated as new proof authority
unless the upstream detector, proof contract, and surface gate are updated in
the same change. `NarrativeLexicon.getMotifPrefix` is only the consumer of that
table, not a branching authority surface.

Report future cleanup as either `boundary cleanup only` or
`boundary cleanup + verified compile/test`; do not claim product quality gains
from mixed or unverified diffs.
