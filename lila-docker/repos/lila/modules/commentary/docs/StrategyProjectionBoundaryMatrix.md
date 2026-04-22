# Strategy Projection Boundary Matrix

This document freezes the semantic boundary of the `25` strategy projection
bands.

It does **not** make `Projection` a truth-owning layer.

It fixes only:

- what each `Sxx` is allowed to mean
- what minimum lower-layer carriers must support it
- which rival bands it must remain separable from
- which shortcuts are forbidden

Where a lower family below is descriptive rather than a current runtime id, it
is a boundary placeholder for later lower-layer freezing.

## Global Rules

- A strategy band is admitted only from certified `Object`, `Delta`, or
  `Certification` carriers, with exact `Witness` support where needed.
- A strategy band may overlap another band on the same board, but it must not
  collapse into a pure wording variant of that rival band.
- A legacy inventory label is never enough by itself to admit a strategy band.
- A raw exact tactic is never enough by itself to admit a strategic conversion
  band.
- `Renderer` wording, planner lane choice, and LLM phrasing never supply
  missing strategy semantics.

## Boundary Matrix

| ID | Core semantic claim | Minimum lower carriers / gates | Must stay separate from | Forbidden shortcut |
| --- | --- | --- | --- | --- |
| `S01` | owner-side pawn storm directed at the enemy king wing, not merely a surviving release resource | same-wing `available_lever_trigger`, `AttackScaffold`, and `certified_king_safety_edge`, with opposite-wing castling language treated as downstream projection context only | `S05`, `S21` | castling-side wording alone; `kingside` / `queenside` shell alone |
| `S02` | direct piece concentration whose primary target is the king ring | `AttackScaffold` plus `certified_king_safety_edge`; lane or access families may strengthen it but do not admit it by themselves | `S03`, `S04`, `S09` | `king attack` label alone; battery or rook-lift wording alone |
| `S03` | king attack whose decisive carrier is a king-facing diagonal entry complex | `AttackScaffold`, `certified_king_safety_edge`, `comparative_king_fragility`, and king-theater-linked `diagonal_lane_only` access support | `S02`, `S12` | `bishop_pair_state` alone; non-king diagonal pressure alone |
| `S04` | destruction of the king shelter itself rather than mere attacking presence | `KingSafetyShell`, a shelter-breach or support-break carrier, and certified king-safety deterioration | `S01`, `S02`, `S03`, `S24` | `king shelter` wording alone; generic attack pressure alone |
| `S05` | central opening or rupture used as the main strategic release | `center` scope shell, central `available_lever_trigger`, and a certified center-opening release gate; `central tension` is support only | `S06`, `S14`, `S21` | `open center` wording alone; `center` shell alone |
| `S06` | structurally anchored space bind that restricts rival piece freedom | `structural_space_claim`, plus restriction or outpost support and a certified bind/persistence gate; `closed center` / `fixed chain` are context only, not admission by themselves | `S05`, `S20` | `space gain` wording alone; `mobility_comparison` alone |
| `S07` | development edge converted into move-right initiative | `development_comparison` plus `InitiativeWindow` | `S08` | development lead wording alone |
| `S08` | denial of the rival's release resources or counterplay | an `InitiativeWindow` or equivalent denial certification, plus suppression of an exact rival counterplay source or break resource | `S07`, `S21`, `S20` | generic "no counterplay" wording alone |
| `S09` | exploitation of open or semi-open lanes to penetrate | `file_lane_state`, plus either `rook_on_open_file_state` or `open line` support, and a certified entry or penetration gate; generic rank-access families are support only outside the separate `S25` rank-corridor slice | `S02`, `S23`, `S25` | `rook_on_open_file_state` alone; `open/semi-open file` alone |
| `S10` | durable occupation of a certified outpost square | `weak_outpost_square_state`, actual occupancy such as `knight_on_outpost_square`, and a certified durability gate against easy eviction | `S12` | good-piece wording alone |
| `S11` | persistent fixation and attack of a concrete weak pawn target | `weak_pawn_target_state`, same-target fixation or repeated pressure support, and a certified persistence gate on that exact target | `S13`, `S14` | `weak_pawn_target_state` alone |
| `S12` | domination built from chronic weak-square access or diagonal-lane control | `weak_outpost_square_state` or king-independent `diagonal_lane_only` support, plus restriction reinforcement and a certified local access-superiority gate | `S03`, `S10`, `S20` | weak-square wording alone; diagonal wording alone |
| `S13` | use of wing-side pawn-count asymmetry to damage a stronger pawn mass before any single weak pawn need be fixed | `sector_asymmetry_state`, `available_lever_trigger`, and a certified wing-damage route; a pre-existing `weak_pawn_target_state` is optional support, not admission | `S11`, `S14`, `S15` | `majority/minority asymmetry` alone |
| `S14` | attack on the base anchor of a pawn chain | base-contact `available_lever_trigger`, a base-targeted structural-damage route, and certified damage persistence; `fixed chain` is context only, not admission by itself | `S05`, `S11`, `S13` | `fixed chain` alone |
| `S15` | creation of a new passed-pawn or passer complex as the strategic target | `create passer` shell, `candidate_passer`-level support, and a certified passer-creation gate; `PasserComplex` and `ConversionFunnel` are upper consequences only | `S13`, `S14`, `S16` | `passed_pawn_entity_state` alone; passer wording alone |
| `S16` | suppression of an enemy passer through blockade, restriction, or race denial | enemy `passed_pawn_entity_state`, blockade or restriction support, and a non-losing promotion-race or holding gate | `S15`, `S22`, `S23` | enemy passer presence alone |
| `S17` | improvement or exchange of the worst minor piece to repair a concrete liability | live lower support includes `same_piece_liability_anchor_seed` plus either `same_piece_repair_route_seed` or `same_piece_exchange_relief_seed`; start-ready handoff is now frozen through `StrategyProjectionAdmission` with `liability_relief_certified` evidence bound to the same piece; broad `minor_piece_liability` language is support only | `S18`, `S20` | `bad piece` wording alone; generic improving move wording alone |
| `S18` | conversion of an already favorable bishop-pair or minor-piece relation | `bishop_pair_state`, plus a certified conversion path into initiative, structure or mobility (`MobilityComparison`), material, or endgame edge; broad `favorable_minor_piece_relation` wording is support only | `S17`, `S12`, `S20` | `bishop_pair_state` alone |
| `S19` | simplification because the exact trade task stays favorable under the current lower law | `TradeInvariant` plus a same-task certification currently limited to `MaterialHarvest` on the move board or `FortressDrawCertification` on the post-trade hold board; `FortressHoldingShell` is object support only and never the certification itself; result-only simplification remains non-counting until a same-task result-certification law exists | `S22`, `S24` | trade wording alone |
| `S20` | comparative mobility collapse or domination of the rival army | `mobility_comparison` plus concrete restriction support such as `short_run_slider_gate_restriction` or defender-starvation geometry | `S06`, `S12`, `S17` | `mobility edge` wording alone |
| `S21` | surviving release resource in the center or on the far wing against the rival plan | exact `pawn_push_break_contact_source` plus a certified counterplay-survival gate; center or wing scope is contextual support only | `S01`, `S05`, `S08` | `open center` wording alone; asymmetry wording alone |
| `S22` | holding, neutralization, or fortress-style consolidation | `FortressHoldingShell` plus a certified hold verdict from the `perpetual/fortress` family; `TradeInvariant` is optional support only | `S19`, `S23` | `draw/hold` wording alone |
| `S23` | king activity, opposition, or penetration as the main endgame plan | live lower support includes `king_entry_square_seed`, `king_access_route_seed`, and `king_opposition_contact_seed`; start-ready handoff is now frozen through `StrategyProjectionAdmission` with either same-entry `king_entry_conversion_certified` evidence or same-contact `king_opposition_certified` evidence | `S09`, `S16`, `S22` | endgame wording alone; `whole-board` shell alone |
| `S24` | tactical realization of a target that was already strategically prepared | live lower support includes same-piece-square `target_resource_dependency_seed` and `target_attack_convergence_seed`; the lower prepared-target package stays tactic-free, and start-ready handoff is now frozen through same-target `same_target_forcing_realization` plus `same_target_conversion_certified` evidence | `S04`, `S19` | `pin` / `fork` / `skewer` / `overload` alone; tactical wording alone |
| `S25` | current-board horizontal rank access that creates a legal same-rank cross-wing switch, not file penetration, king activity, weak-square access, or move-history rook-lift narration | live lower support includes `rank_corridor_state_seed`; start-ready handoff is frozen through `StrategyProjectionAdmission` with `rank_access_consequence_certified` evidence bound to the same source anchor, `entry_square`, and `cross_wing_rank_switch` kind | `S02`, `S03`, `S04`, `S09`, `S12`, `S23`, `S24` | piece on a rank alone; generic rook-lift wording; open-file substrate alone; back-rank defense alone; king activity alone; tactic/result alone |

## Current High-Risk Boundary Clusters

- `S01` / `S05` / `S21`: release vs storm vs counterplay wording can drift if
  wing/center carriers are not explicit.
- `S02` / `S03` / `S04`: king-attack wording can collapse without separate
  access, color-complex, and shelter-breach gates.
- `S11` / `S13` / `S14` / `S15` / `S16`: pawn-structure bands need target-type
  separation, not just shared pawn activity.
- `S17` / `S18` / `S20`: piece-quality, piece-edge conversion, and mobility
  domination must not be treated as one evaluative bucket.
- `S09` / `S23` / `S25`: file penetration, king penetration, and same-rank
  cross-wing access must stay separate even when the same heavy piece later
  participates in more than one plan.
- `S19` / `S22` / `S23` / `S24`: simplification, holding, king activity, and
  tactical conversion need different means-vs-result boundaries.

## Freeze Meaning

The boundary matrix above freezes semantic boundaries only.

It does not yet prove:

- full carrier completeness for every `Sxx`
- planner ranking between competing `Sxx`
- renderer wording policy
- corpus sufficiency

Rows that still name a `pending ... support seed` are intentionally frozen as
semantic boundaries rather than implementation-ready lower contracts.

Rows that say `currently blocked for live admission` are projection bands whose
semantic boundary is frozen, but whose lower carrier package is not yet allowed
to release a live strategy claim.

Those remain later work.

The later handoff section below freezes projection start-ready contract status
without turning projection into live runtime.

## Projection Handoff Freeze

For projection-start work on this branch, a band is `start-ready` only when all
of the following are frozen:

- the semantic boundary row above
- one canonical exact helper / gate package
- the optional strengthening carriers that may not admit the band by themselves
- the exact projection validation scaffold
- one explicit promotion criterion or one explicit blocker

`start-ready` here means ready for projection implementation and corpus authoring.

It does **not** mean live runtime already exists.

Executable live runtime start-ready status is narrower than this handoff
matrix. The only current live admission bands are
`S17`, `S23`, `S24`, and `S25`, as frozen by
`StrategyProjectionScopeContract.startReadyBandIds` and enforced by
`StrategyProjectionAdmission`.

All other `S01-S25` bands have semantic, row-burden, helper-law, lower-carrier,
and exact-validation freezes only. They are coverage-only in
`StrategyProjectionCoverageContract`; current live `StrategyProjectionAdmission`
must reject them until a later runtime admission boundary is explicitly
authored. Older "start-ready" handoff prose for non-`S17`/`S23`/`S24`/`S25`
bands is not runtime authority.

### Shared Projection Carrier Laws

- same-anchor contact-target law
  - exact basis: same-anchor `available_lever_trigger` plus
    `pawn_push_break_contact_source`
  - frozen target law: projection may use only exact payload target squares from
    `pawn_push_break_contact_source`, with target roles recomputed from the
    same board, especially
    `center_pawn_target`, `chain_base_target`, `phalanx_edge_target`, and
    `structurally_burdened_target`
  - `center`, `kingside`, and `queenside` shells remain contextual support only
- anchor-aligned restriction law
  - exact basis: anchor-aligned `short_run_slider_gate_restriction`,
    `duty_bound_defender`, `weak_outpost_square_state`, and
    same-beneficiary `MobilityComparison`
  - frozen law: restriction support may strengthen `S06`, `S10`, `S12`, and
    `S16`, but no raw restriction witness admits those bands by itself
- same-file continuation law
  - exact basis: `file_lane_state` plus `rook_on_open_file_state` or `open line`
    shell support, with same-file continuation carriers
  - frozen law: file substrate and shell wording stay support only until one
    same-file entry or penetration consequence is present; generic rank-access
    families remain non-admitting outside the separate `S25` rank-corridor law
- legal cross-wing rank-corridor law
  - exact basis: live `rank_corridor_state_seed` on a rook or queen with a
    legal current-board same-rank move from one wing sector to the nearest
    opposite-wing entry square through the center files
  - frozen law: `S25` may use only same-source `rank_access_consequence_certified`
    evidence whose `entry_square` and `corridor_kind` match that seed; rook-lift
    move history, rank occupation, back-rank defense, file substrate, weak-square
    access, king activity, and raw tactics never admit this band
- same-target identity law
  - exact basis: `weak_pawn_target_state` plus same-target pressure support such
    as `pawn_push_break_contact_source`, `file_lane_state`,
    `rook_on_open_file_state`, `pin`, or `duty_bound_defender`
  - frozen law: target identity must remain the same exact pawn across the whole
    package; a new target may not be swapped in by prose
- passer semantics law
  - exact basis: `create passer` shell plus `candidate_passer` support or enemy
    `passed_pawn_entity_state`, with optional race / holding certification on
    the same board
  - frozen law: `passed_pawn_entity_state`, `PromotionRace`, or result wording
    may strengthen passer play, but they never backfill missing creation or
    suppression semantics
- counterplay survival law
  - exact basis: owner `pawn_push_break_contact_source` plus certified
    `InitiativeWindow` on the same board, with that certification's current
    `DevelopmentComparison` support already satisfied
  - frozen law: the current first live slice stays on development-led
    initiative only, so source presence alone or a `Deferred` / `Rejected`
    initiative verdict cannot admit `S21`; `available_lever_trigger`,
    `AttackScaffold`, `EndgameRaceScaffold`, and theater shells remain
    strengthening only
- anchored minor-piece liability law
  - exact basis: anchored liability support drawn from
    `loose_piece_target_state`, `duty_bound_defender`,
    `weak_outpost_square_state`, `pinned_piece`, and `trapped_piece`
  - frozen law: this support is still insufficient by itself because `S17`
    additionally needs the live `same_piece_liability_anchor_seed` plus one
    same-piece relief seed (`same_piece_repair_route_seed` or
    `same_piece_exchange_relief_seed`)

### Start-Ready Handoff Status

The rows below add or freeze the current projection handoff contract pieces for
the current working set only. Core meaning, minimum carriers, rivals, and
forbidden shortcuts remain the rows in the boundary matrix above.

| Band | Canonical exact carrier bundle | Optional strengthening carriers | Required validation scaffold | Status | Promotion criterion / blocker |
| --- | --- | --- | --- | --- | --- |
| `S01` | same-wing `available_lever_trigger` plus same-anchor `pawn_push_break_contact_source` on the attacked king wing, together with `AttackScaffold` and `CertifiedKingSafetyEdge` on the same defending king | `ComparativeKingFragility`; king-theater-linked `file_lane_state` / `diagonal_lane_only` | projection core matrix; `contrastive` vs `S05` and `S21`; `negative` for castling-side wording or shell-only wing context | `coverage-complete` | broad coverage only; same-wing alignment, attack host, and king-safety edge are frozen without using castling wording as proof |
| `S04` | `KingSafetyShell` plus same-king shelter-breach evidence from the shell payload and `CertifiedKingSafetyEdge` on that same king | `AttackScaffold`; same-anchor contact-target support; `ComparativeKingFragility` | projection core matrix; `contrastive` vs `S01`, `S02`, `S03`, and `S24`; `negative` for shell-only king weakness with no breach gate | `coverage-complete` | broad coverage only; shelter-breach proof is pinned to one `KingSafetyShell` host and same-defender king-safety deterioration |
| `S05` | `center` shell plus central `available_lever_trigger`, same-anchor `pawn_push_break_contact_source` with a `center_pawn_target`, and one central-axis continuation carrier | `file_lane_state` on files `c-f`; `AttackScaffold`; `InitiativeWindow`; `CentralContactFront` support only | projection core matrix; `contrastive` vs `S06`, `S14`, and `S21`; `negative` for `open center` wording or `center` shell alone | `coverage-complete` | broad coverage only; center-target role and central-axis continuation carrier are frozen as one fail-closed bundle rather than open-center narration |
| `S06` | `structural_space_claim` plus anchor-aligned restriction or outpost support and same-beneficiary persistence proof | `knight_on_outpost_square`; `MobilityComparison`; `InitiativeWindow` | projection core matrix; `contrastive` vs `S05` and `S20`; `negative` for space wording or `MobilityComparison` alone | `coverage-complete` | broad coverage only; space claim, restriction anchor, and persistence proof stay aligned without admitting from broad `closed center` / `fixed chain` vocabulary |
| `S08` | exact rival `pawn_push_break_contact_source` or rival release-contact bundle together with beneficiary `InitiativeWindow` | `MobilityComparison`; `short_run_slider_gate_restriction` | projection core matrix; `contrastive` vs `S07`, `S21`, and `S20`; `negative` for generic "no counterplay" wording | `coverage-complete` | broad coverage only; denial is frozen against one exact rival source rather than against broad counterplay prose |
| `S09` | `file_lane_state` plus `rook_on_open_file_state` or `open line` shell support, with one same-file entry or penetration consequence | `AttackScaffold`; `CertifiedKingSafetyEdge`; `MaterialHarvest`; `WinningEndgame` | projection core matrix; `contrastive` vs `S02`, `S23`, and `S25`; `negative` for open-file substrate or `rook_on_open_file_state` alone | `coverage-complete` | broad coverage only; same-file lane substrate and same-file continuation consequence are frozen together, while generic rank-access stays non-admitting outside `S25` |
| `S10` | `weak_outpost_square_state` plus exact `knight_on_outpost_square` occupancy and same-anchor durability proof | `short_run_slider_gate_restriction`; `MobilityComparison` | projection core matrix; `contrastive` vs `S12`; `negative` for good-piece or occupancy wording alone | `coverage-complete` | broad coverage only; current broad scope is knight-only on a certified outpost square, with durability still required and explicit |
| `S11` | `weak_pawn_target_state` plus same-target pressure support and same-target persistence proof | `MobilityComparison`; `InitiativeWindow` when the denial burden stays on the same target | projection core matrix; `contrastive` vs `S13` and `S14`; `negative` for weak-pawn target alone | `coverage-complete` | broad coverage only; target identity is frozen across the pressure bundle and cannot drift into generic pawn-quality narration |
| `S12` | `weak_outpost_square_state` or king-independent `diagonal_lane_only` support plus local restriction reinforcement and local access-superiority proof | `short_run_slider_gate_restriction`; `MobilityComparison` | projection core matrix; `contrastive` vs `S03`, `S10`, and `S20`; `negative` for weak-square or diagonal wording alone | `coverage-complete` | broad coverage only; access proof stays local and anchor-bound rather than broadening into board-wide domination prose |
| `S13` | `sector_asymmetry_state` plus same-sector `available_lever_trigger` and same-anchor `pawn_push_break_contact_source` against structurally burdened or phalanx-edge targets on the stronger pawn mass | `weak_pawn_target_state`; `file_lane_state` | projection core matrix; `contrastive` vs `S11`, `S14`, and `S15`; `negative` for asymmetry wording alone | `coverage-complete` | broad coverage only; wing asymmetry is tied to one exact damage route and does not require a pre-existing weak pawn to admit |
| `S14` | base-contact `available_lever_trigger` plus same-anchor `pawn_push_break_contact_source` whose exact target pawn square is recomputed as a `chain_base_target` on the board | `weak_pawn_target_state`; `structural damage` shell context only | projection core matrix; `contrastive` vs `S05`, `S11`, and `S13`; `negative` for `fixed chain` vocabulary alone | `coverage-complete` | broad coverage only; chain-base targeting is frozen on the exact target square and `fixed chain` remains context |
| `S15` | `create passer` shell plus `candidate_passer` support and one exact creation route already frozen through `S13` or `S14` on the same board | `PasserComplex`; `ConversionFunnel`; `PromotionRace` | `projection-expectations.jsonl`; exact/false-rival/nasty rows vs `S13`, `S14`, and `S16`; `negative` for `create passer`, `candidate_passer`, or `passed_pawn_entity_state` alone | `coverage-complete` | wave-6 exact-board coverage rows are present and green; creation must stay on exact support truth plus an exact creation route and must not admit from an already existing passer entity; runtime projection admission remains non-live |
| `S16` | enemy `passed_pawn_entity_state` plus blockade or restriction support and non-losing race / holding proof | `short_run_slider_gate_restriction`; `PromotionRace`; `FortressDrawCertification`; `PerpetualCheckHolding` | `projection-expectations.jsonl`; exact/false-rival/nasty rows vs `S15`, `S22`, and `S23`; `negative` for enemy passer presence alone or blocker picture alone | `coverage-complete` | wave-6 exact-board coverage rows are present and green; suppression must either certify the race or certify the hold, and raw passer/blocker pictures are not enough; runtime projection admission remains non-live |
| `S17` | live `same_piece_liability_anchor_seed` plus either live `same_piece_repair_route_seed` or live `same_piece_exchange_relief_seed`, with `liability_relief_certified` evidence on the same piece and a matching `relief_kind` | current live anchor seed reads `loose_piece_target_state`, `pinned_piece`, and `trapped_piece`; `duty_bound_defender`, `weak_outpost_square_state`, `TradeInvariant`, and `MobilityComparison` remain strengthening only | `projection-expectations.jsonl`; exact/near/nasty/rival rows vs `S18` and `S20`; `StrategyProjectionAdmissionTest` stale-evidence boundary | `start-ready` | promoted once the same-piece liability anchor, same-piece relief seed, and exact relief evidence are bound to the same piece; seed presence alone still does not release `S17` |
| `S21` | exact owner `pawn_push_break_contact_source` on the center or far wing plus certified `InitiativeWindow` on the same board, with its current `DevelopmentComparison` support and `counterplay_denial_window` / `rival_counterplay_source` / `move_order_relevance_gate` burdens | `available_lever_trigger`; `center` shell or `sector_asymmetry_state`; `AttackScaffold`; `EndgameRaceScaffold` | projection core matrix; `contrastive` vs `S01`, `S05`, and `S08`; `negative` for exact source alone, `InitiativeWindow` alone, or exact source with `InitiativeWindow` still `Deferred` / `Rejected` | `coverage-complete` | broad coverage only; owner source is tied to the current development-led `InitiativeWindow` slice so openness / asymmetry wording cannot backfill survival semantics |
| `S23` | live `king_entry_square_seed` / `king_access_route_seed` sharing the same entry square, or live `king_opposition_contact_seed` sharing the same contact square, plus matching projection evidence | existing `file_lane_state`, `WinningEndgame`, and `PromotionRace` remain support only unless tied through the explicit `StrategyProjectionAdmission` evidence kinds | `projection-expectations.jsonl`; exact/near/nasty/rival rows vs `S09`, `S16`, and `S22`; `StrategyProjectionAdmissionTest` | `start-ready` | promoted once entry+route+entry-conversion evidence share one entry square, or opposition+opposition evidence share one contact square; seed presence alone still does not release `S23` |
| `S24` | live `target_resource_dependency_seed` and live `target_attack_convergence_seed` on the same target, plus same-target forcing and conversion evidence | exact tactics and conversion certifications remain external realization companions only and must bind to the prepared target through `StrategyProjectionAdmission` | `projection-expectations.jsonl`; exact/near/nasty/rival rows vs `S04` and `S19`; `StrategyProjectionAdmissionTest` | `start-ready` | promoted once dependency, convergence, forcing realization, and conversion certification all name the same target; seed presence or tactic/result wording alone still does not release `S24` |
| `S25` | live `rank_corridor_state_seed` plus `rank_access_consequence_certified` evidence on the same source anchor, same `entry_square`, and `cross_wing_rank_switch` kind | `AttackScaffold`, `CertifiedKingSafetyEdge`, `file_lane_state`, `diagonal_lane_only`, `weak_outpost_square_state`, and exact tactic/certification families remain support only unless the explicit rank-corridor law is satisfied | `projection-expectations.jsonl`; exact/near/nasty/rival rows vs `S02`, `S03`, `S04`, `S09`, `S12`, `S23`, and `S24`; `StrategyProjectionAdmissionTest`; `HorizontalRankAccessSupportSeedRulesTest` | `start-ready` | promoted once legal current-board cross-wing rank switching is anchored to one rook/queen source and one entry square; seed presence alone, piece-on-rank pictures, file substrate, king activity, weak-square access, tactics, or rook-lift prose still do not release `S25` |

Only `S17`, `S23`, `S24`, and `S25` are current live-runtime start-ready rows.
Coverage-complete rows authorize corpus validation only. They do **not**
authorize broad deployment or live runtime admission.

Current broader-deployment caution remains highest on:

- `S15` and `S16`, whose wave-6 exact-board coverage rows are now authored and
  complete, but remain coverage-only rather than broad-deployed or live runtime
  admission
- all completed staged bands, because coverage-complete corpus rows still do
  not make any projection band broad-deployed or live runtime by themselves

`S06`, `S09`, `S10`, `S12`, `S20`, `S23`, and `S25` now have matching exact
route rows, same-cluster near-miss rows, and shortcut-negative rows for their
current frozen breadth gates. This is broad-coverage completion only; it does
not add new live runtime projection admission or broad deployment for those
bands. `S23` and `S25` keep only their already-authored narrow live admission
slices.

### Broad-Ready Promotion Freeze

No coverage-complete-only strategy band is broad-deployed or live as runtime
projection on the current branch.

`S06`, `S09`, `S10`, `S12`, `S20`, `S23`, and `S25` are coverage-complete
against the current frozen wave-1 broad-ready gate table. That status is
corpus/validation-only and remains separate from runtime admission and broad
deployment; for `S23` and `S25` it does not widen the existing narrow live
admission slices.

A future `broad-ready` claim must stay fail-closed on two contracts:

- the projection core matrix plus `coverageAxis` / `coverageBucket` rows in
  `projection-expectations.jsonl`
- the band-local breadth gates frozen below

Wave-7 global freeze owner: `StrategyProjectionCoverageContract` owns
`coverageGatesByBand`, `lowerCarrierOwnersByBand`, `helperLawsByBand`, and
`exactValidationScaffoldByBand` for every `S01-S25` band. Its
`globalClosureBroadReadyCoverageBandIds` set is exactly the full `S01-S25`
range. The executable corpus must derive `requiredCoveragePairsFor` from that
contract, not from a corpus-local alternate table. `coverageOnlyBandIds` is
every frozen band except the live `StrategyProjectionScopeContract.startReadyBandIds`
(`S17`, `S23`, `S24`, and `S25`), and `StrategyProjectionAdmission` must
fail-closed for those coverage-only bands.

| Band | Required broad-ready `coverageAxis` / `coverageBucket` rows | Residual blocker / decision before promotion |
| --- | --- | --- |
| `S01` | `storm_route`: `same_wing_contact`, `attack_edge_same_king`; `same_cluster_near_miss`: `vs_s05`, `vs_s21`; `shortcut_negative`: `castling_side_only`, `wing_shell_only` | wave-4 exact-board coverage rows are present and green; runtime projection admission remains non-live, and storm coverage stays tied to same-wing contact plus attack and king-safety carriers, not castling-side prose |
| `S02` | `attack_concentration_route`: `direct_piece_concentration`, `lane_strengthened_concentration`; `same_cluster_near_miss`: `vs_s03`, `vs_s04`, `vs_s09`; `shortcut_negative`: `king_attack_label_only`, `battery_or_lift_only` | wave-4 exact-board coverage rows are present and green; runtime projection admission remains non-live, and direct king-ring concentration must not collapse into diagonal, shelter-breach, or file-penetration meaning |
| `S03` | `diagonal_attack_route`: `king_facing_diagonal_entry`, `fragility_linked_diagonal`; `same_cluster_near_miss`: `vs_s02`, `vs_s12`; `shortcut_negative`: `bishop_pair_only`, `non_king_diagonal_pressure` | wave-4 exact-board coverage rows are present and green; runtime projection admission remains non-live, and the decisive carrier must remain king-facing rather than broad bishop or weak-square pressure |
| `S04` | `shelter_breach_route`: `shell_payload_breach`, `support_break_breach`; `same_cluster_near_miss`: `vs_s01`, `vs_s02`, `vs_s03`, `vs_s24`; `shortcut_negative`: `king_shelter_wording_only`, `generic_attack_pressure` | wave-4 exact-board coverage rows are present and green; runtime projection admission remains non-live, and breach plus king-safety deterioration must bind to the same defender king |
| `S05` | `center_release_route`: `center_pawn_target`, `central_axis_continuation`; `same_cluster_near_miss`: `vs_s06`, `vs_s14`, `vs_s21`; `shortcut_negative`: `open_center_wording_only`, `center_shell_only` | wave-3 exact-board coverage rows are present and green; runtime projection admission remains non-live |
| `S06` | `restriction_route`: `outpost_anchor`, `non_outpost_space_bind`; `same_cluster_near_miss`: `vs_s05`, `vs_s20`; `shortcut_negative`: `space_wording_only`, `mobility_comparison_only` | initial exact-board coverage rows are present and green; runtime projection admission remains non-live |
| `S07` | `initiative_conversion_route`: `development_led_window`, `move_right_window`; `same_cluster_near_miss`: `vs_s08`; `shortcut_negative`: `development_lead_only`, `initiative_wording_only` | wave-3 exact-board coverage rows are present and green; runtime projection admission remains non-live |
| `S08` | `denial_route`: `rival_break_source_suppressed`, `rival_counterplay_source_suppressed`; `same_cluster_near_miss`: `vs_s07`, `vs_s20`, `vs_s21`; `shortcut_negative`: `no_counterplay_wording_only`, `initiative_window_only` | wave-3 exact-board coverage rows are present and green; runtime projection admission remains non-live |
| `S09` | `penetration_route`: `open_file_entry`, `semi_open_file_entry`, `same_file_penetration`; `same_cluster_near_miss`: `vs_s02`, `vs_s23`, `vs_s25`; `shortcut_negative`: `file_substrate_only`, `rook_on_file_only` | initial exact-board coverage rows are present and green; S09 still must keep lane substrate and continuation on the same file, and rank-access support remains non-admitting unless the separate `S25` rank-corridor law is satisfied |
| `S10` | `occupancy_scope`: `knight_only`; `durability_route`: `same_anchor_eviction_denial`; `same_cluster_near_miss`: `vs_s12`; `shortcut_negative`: `good_piece_wording_only`, `occupancy_without_durability`, `non_knight_occupancy_without_freeze` | initial exact-board coverage rows are present and green; legacy scope blocker remains closed as current-branch broad `S10` is knight-only, and any non-knight outpost concept requires a later separate lower-support freeze |
| `S11` | `target_pressure_route`: `same_target_fixation`, `same_target_repeated_pressure`; `same_cluster_near_miss`: `vs_s13`, `vs_s14`; `shortcut_negative`: `weak_pawn_target_only`, `target_swap_by_prose` | wave-5 exact-board coverage rows are present and green; runtime projection admission remains non-live, and weak-pawn identity must stay stable across target, pressure, and fixed-pawn persistence proof |
| `S12` | `access_route`: `weak_square_route`, `diagonal_lane_route`; `same_cluster_near_miss`: `vs_s03`, `vs_s10`, `vs_s20`; `shortcut_negative`: `weak_square_wording_only`, `diagonal_wording_only` | initial exact-board coverage rows are present and green; runtime projection admission remains non-live |
| `S13` | `wing_damage_route`: `phalanx_edge_target`, `structurally_burdened_target`; `same_cluster_near_miss`: `vs_s11`, `vs_s14`, `vs_s15`; `shortcut_negative`: `sector_asymmetry_only`, `preexisting_weak_pawn_only` | wave-5 exact-board coverage rows are present and green; runtime projection admission remains non-live, and pawn-count asymmetry must tie to one exact damage route against the stronger mass |
| `S14` | `chain_base_route`: `chain_base_target`, `base_contact_continuation`; `same_cluster_near_miss`: `vs_s05`, `vs_s11`, `vs_s13`; `shortcut_negative`: `fixed_chain_only`, `generic_structural_damage` | wave-5 exact-board coverage rows are present and green; runtime projection admission remains non-live, and chain-base target identity must be recomputed from the exact board and contact target square; `fixed chain` stays context only |
| `S15` | `creation_route`: `s13_wing_damage`, `s14_chain_base`; `same_cluster_near_miss`: `vs_s13`, `vs_s14`, `vs_s16`; `shortcut_negative`: `candidate_passer_only`, `existing_passer_entity_only` | wave-6 exact-board coverage rows are present and green: per-position admission may use either creation route, but broad-ready coverage proves both frozen route buckets plus S13/S14/S16 false-rival boundaries; runtime admission remains fail-closed |
| `S16` | `suppression_route`: `blockade_hold`, `restriction_hold`, `non_losing_race`; `same_cluster_near_miss`: `vs_s15`, `vs_s22`, `vs_s23`; `shortcut_negative`: `enemy_passer_presence_only`, `blocker_picture_only` | wave-6 exact-board coverage rows are present and green: suppression requires certified race or hold proof, and enemy passer presence plus blocker pictures cannot admit the band; runtime admission remains fail-closed |
| `S17` | `liability_relief_route`: `repair_route`, `exchange_relief`; `same_cluster_near_miss`: `vs_s18`, `vs_s20`; `shortcut_negative`: `bad_piece_wording_only`, `generic_improving_move` | must keep liability anchor, relief seed, and relief certification on the same piece |
| `S18` | `minor_edge_conversion_route`: `bishop_pair_to_initiative`, `bishop_pair_to_structure`, `bishop_pair_to_material_or_endgame`; `same_cluster_near_miss`: `vs_s12`, `vs_s17`, `vs_s20`; `shortcut_negative`: `bishop_pair_state_only`, `minor_edge_label_only` | must prove conversion of the favorable relation, not the relation alone |
| `S19` | `simplification_route`: `trade_invariant_to_hold`, `trade_invariant_to_material`; `same_cluster_near_miss`: `vs_s22`, `vs_s24`; `shortcut_negative`: `trade_wording_only`, `material_reduction_only` | must keep same-task certification attached to the exact trade invariant; result-only simplification remains fail-closed |
| `S20` | `domination_route`: `mobility_plus_restriction`, `defender_starvation`; `same_cluster_near_miss`: `vs_s06`, `vs_s12`, `vs_s17`; `shortcut_negative`: `mobility_edge_only`, `restriction_without_comparison` | initial exact-board coverage rows are present and green; positive domination routes require actual `MobilityComparison` plus concrete restriction or duty-bound defender support, not generic mobility-edge wording |
| `S21` | `counterplay_survival_route`: `center_source_survives`, `far_wing_source_survives`; `same_cluster_near_miss`: `vs_s01`, `vs_s05`, `vs_s08`; `shortcut_negative`: `break_source_only`, `deferred_initiative_only` | wave-3 exact-board coverage rows are present and green; runtime projection admission remains non-live |
| `S22` | `hold_route`: `fortress_draw_hold`, `perpetual_hold`; `same_cluster_near_miss`: `vs_s19`, `vs_s23`; `shortcut_negative`: `draw_wording_only`, `fortress_shell_only` | must use certified hold verdicts; consolidation wording and shell presence alone stay below projection |
| `S23` | `king_activity_route`: `same_entry_route`, `same_contact_opposition`; `same_cluster_near_miss`: `vs_s09`, `vs_s16`, `vs_s22`; `shortcut_negative`: `centralization_only`, `king_proximity_only` | initial exact-board coverage rows are present and green; S23 still must keep entry, route, and certification on the same square, or opposition and certification on the same contact |
| `S24` | `prepared_target_route`: `same_target_forcing_conversion`; `same_cluster_near_miss`: `vs_s04`, `vs_s19`; `shortcut_negative`: `raw_tactic_only`, `trade_or_result_only` | must keep dependency, convergence, forcing realization, and conversion certification on the same target |
| `S25` | `rank_access_route`: `cross_wing_rank_switch`; `same_cluster_near_miss`: `vs_s02`, `vs_s03`, `vs_s04`, `vs_s09`, `vs_s12`, `vs_s23`, `vs_s24`; `shortcut_negative`: `piece_on_rank_only`, `generic_rook_lift`, `file_substrate_only`, `back_rank_defense_only`, `king_activity_only`, `tactic_only` | initial exact-board coverage rows are present and green; S25 still must prove legal current-board same-rank cross-wing access from one rook/queen source to one entry square, while broader horizontal-rank meanings remain future scope |

This table freezes the broad-ready breadth gates. A band is broad-ready only
when `projection-expectations.jsonl` has countable exact-board rows for every
required `coverageAxis` / `coverageBucket` pair and validation is green.

### Pawn-Target / Structural Damage Coverage Rows

The current broad-ready coverage for `S11`, `S13`, and `S14` is executable in
`modules/commentary/src/main/scala/lila/commentary/projection/StrategyProjectionCoverageContract.scala`
and verified by `StrategyProjectionCoverageContractTest` plus
`ProjectionExpectationCorpusTest`.

The wave-5 JSONL rows are now authored and countable. This promotes the three
bands to broad-ready coverage completeness only; it does not expand live
`StrategyProjectionAdmission`:

- row-specific burden gates are the broad-ready gate table rows for `S11`,
  `S13`, and `S14`
- shared helper law stays exact-board and fail-closed:
  - `S11`: `weak_pawn_target_state`, pressure, and the current fixed-pawn
    persistence proof must stay on the same weak-pawn square; target swaps by
    prose do not count
  - `S13`: `sector_asymmetry_state`, same-sector lever, and same-anchor
    `pawn_push_break_contact_source` must bind to a target role recomputed from
    the exact board as `phalanx_edge_target` or
    `structurally_burdened_target`; asymmetry or a pre-existing weak pawn alone
    does not count
  - `S14`: base-contact lever and same-anchor contact source must bind to a
    target square recomputed from the exact board as `chain_base_target`;
    `fixed chain` and `structural damage` shell wording remain context only
- lower-carrier ownership is frozen as witness, certification-support,
  support-only, or projection-validation ownership. Projection does not become
  the truth owner for `weak_pawn_target_state`, `sector_asymmetry_state`,
  `available_lever_trigger`, `pawn_push_break_contact_source`,
  `MobilityComparison`, `InitiativeWindow`, or structural shell vocabulary
- exact validation scaffold names are frozen before row authoring:
  - `S11`: same-square weak-pawn target, same-target pressure or repeated
    pressure, same-target fixed-pawn persistence proof, and target-swap
    non-counting
  - `S13`: sector asymmetry, same-sector lever/contact source, exact target
    role as phalanx-edge or structurally burdened, and weak-pawn/asymmetry-only
    non-counting
  - `S14`: base-contact lever/contact source, exact chain-base target role,
    same-anchor base-contact continuation, and fixed-chain/structural-shell-only
    non-counting
- runtime boundary remains fail-closed:
  `S11`, `S13`, and `S14` are broad-ready coverage-only bands, not live
  admission bands. `StrategyProjectionAdmission` must still reject them until a
  separate runtime admission boundary is explicitly authored.

`ProjectionExpectationCorpus.requiredCoveragePairsFor` and
`coveragePairsByBand`, `missingRequiredCoveragePairsByBand`, and
`bandsWithCompleteBroadReadyCoverage` now include these three bands in the
counted broad-ready gate. The authored rows keep lower-carrier ownership in the
lower witness/certification-support layer and do not create live projection
admission.

This section does not create live runtime projection admission and does not
extend S-layer meaning into synthesis or renderer behavior.

### King-Attack Family Coverage Rows

The current broad-ready coverage for `S01`, `S02`, `S03`, and `S04` is
executable in
`modules/commentary/src/main/scala/lila/commentary/projection/StrategyProjectionCoverageContract.scala`
and verified by `StrategyProjectionCoverageContractTest` plus
`ProjectionExpectationCorpusTest`.

The wave-4 JSONL rows are now authored and countable. This promotes the four
bands to broad-ready coverage completeness only; it does not expand live
`StrategyProjectionAdmission`:

- row-specific burden gates are the broad-ready gate table rows for `S01`,
  `S02`, `S03`, and `S04`
- shared helper law stays exact-board and fail-closed:
  - `S01`: same-wing owner contact and attack edge must travel with the same
    defending king; castling context is not storm proof
  - `S02`: direct king-ring concentration is the projection validation burden;
    lane support can strengthen it but cannot admit the band
  - `S03`: `diagonal_lane_only` must be king-theater-linked and paired with
    same-king fragility / king-safety certification; bishop-pair state is not
    diagonal-attack truth
  - `S04`: same-king shell breach must be proved from the `KingSafetyShell`
    payload or a same-king support-break route; shell presence alone is not
    demolition truth
- lower-carrier ownership is frozen as witness, object-support,
  certification-support, support-only, or projection-validation ownership:
  projection does not become the truth owner for `AttackScaffold`,
  `KingSafetyShell`, `ComparativeKingFragility`, or `CertifiedKingSafetyEdge`
- exact validation scaffold is complete for the authored rows:
  - `S01`: same-wing owner contact, same-defender `AttackScaffold`, and
    same-owner certified king-safety edge, with castling-side context ignored
  - `S02`: same-defender `AttackScaffold`, same-owner certified king-safety
    edge, and direct-concentration proof that is not file-only or diagonal-only
  - `S03`: king-theater `diagonal_lane_only`, comparative king fragility, and
    same-owner certified king-safety edge, with bishop-pair state non-counting
  - `S04`: same-defender `KingSafetyShell`, shell-payload or support-break
    breach, and same-defender certified king-safety edge, with shell-only rows
    non-counting
- runtime boundary remains fail-closed:
  `S01`, `S02`, `S03`, and `S04` are broad-ready coverage-only bands, not live
  admission bands. They are included in
  `ProjectionExpectationCorpus.requiredCoveragePairsFor`,
  `coveragePairsByBand`, `missingRequiredCoveragePairsByBand`, and
  `bandsWithCompleteBroadReadyCoverage` once the exact-board JSONL rows are
  loaded. `StrategyProjectionAdmission` must still reject them as unsupported
  live admission bands.

This section does not create live runtime projection admission and does not
extend S-layer meaning into synthesis or renderer behavior.

### Initiative / Release / Counterplay Coverage Rows

The current broad-ready coverage for `S05`, `S07`, `S08`, and `S21` is
executable in
`modules/commentary/src/main/scala/lila/commentary/projection/StrategyProjectionCoverageContract.scala`
and verified by `StrategyProjectionCoverageContractTest` plus
`ProjectionExpectationCorpusTest`.

The wave-3 JSONL rows are now authored and countable. This promotes the four
bands to broad-ready coverage completeness only; it does not expand live
`StrategyProjectionAdmission`.

- row-specific burden gates are the broad-ready gate table rows for `S05`,
  `S07`, `S08`, and `S21`
- shared helper law stays exact-board and fail-closed:
  - `S05`: the same-anchor contact target must be an exact
    `center_pawn_target`, and `central_axis_continuation` remains a validation
    law rather than an object/certification truth owner
  - `S07`: `DevelopmentComparison` and certified `InitiativeWindow` must both
    be present for the same owner; `OpeningDevelopmentRegime` is support only
  - `S08`: denial must be frozen against one exact rival release source;
    `InitiativeWindow` by itself is support for the denial route, not the
    projection truth owner
  - `S21`: exact owner `pawn_push_break_contact_source` and same-board certified
    `InitiativeWindow` must travel together; source presence alone is not
    counterplay survival
- lower-carrier ownership is frozen as witness / object-support /
  certification / projection-validation ownership, not as projection-owned
  lower truth
- exact validation scaffold is now complete for the authored rows:
  positive route buckets count only `exact` + `admitted` rows, same-cluster
  buckets count only rejected near-miss / contrastive /
  `comparative_false_rival` rows, and shortcut buckets count only rejected
  nasty-negative / negative rows
- runtime boundary remains fail-closed:
  `S05`, `S07`, `S08`, and `S21` are broad-ready coverage-only bands, not live
  admission bands; `StrategyProjectionAdmission` must still reject them until a
  separate runtime admission boundary is explicitly authored

`ProjectionExpectationCorpus.requiredCoveragePairsFor`,
`coveragePairsByBand`, `missingRequiredCoveragePairsByBand`, and
`bandsWithCompleteBroadReadyCoverage` now include these four bands in the
counted broad-ready gate. The authored rows keep lower-carrier ownership in the
lower witness/object/certification layer and do not create live projection
admission.

### Conversion / Simplification / Holding Coverage Rows

The current coverage freeze for `S17`, `S18`, `S19`, `S22`, and `S24` is
executable in
`modules/commentary/src/main/scala/lila/commentary/projection/StrategyProjectionCoverageContract.scala`
and verified by `StrategyProjectionCoverageContractTest` plus
`ProjectionExpectationCorpusTest`.

The wave-2 JSONL rows are now authored and countable. The freeze closes these
blocker causes without expanding live admission:

- row-specific burden gates are the broad-ready gate table rows for `S17`,
  `S18`, `S19`, `S22`, and `S24`
- shared helper law stays same-anchor / same-piece / same-target:
  - `S17`: liability anchor, relief seed, and `liability_relief_certified`
    must name the same piece
  - `S18`: `bishop_pair_state` is substrate only; conversion certification
    must supply the burden, with the structure route owned by
    `MobilityComparison` rather than by projection prose
  - `S19`: `TradeInvariant` is the delta task carrier; countable material or
    hold certification must stay attached to that same task endpoint
  - `S22`: `FortressHoldingShell` is object support; certified hold verdicts
    remain `FortressDrawCertification` or `PerpetualCheckHolding`
  - `S24`: dependency, convergence, forcing realization, and conversion
    certification must name the same target piece-square
- lower-carrier ownership is frozen as support / object / delta /
  certification ownership, not as projection-owned truth
- exact validation scaffolds must use exact FEN rows, before/after FEN plus
  played move where a delta is involved, and engine/probe evidence only where
  certification burdens require best-defense, route-survival, hold, or tactical
  release evidence
- broad coverage candidates are not live runtime admission bands:
  `S18`, `S19`, and `S22` are coverage-only at this boundary, while `S17` and
  `S24` reuse the existing start-ready admission scaffold
- `S19` coverage rows carry exact `fenBefore` / `playedMove` / `fenAfter`
  companions for `TradeInvariant`; material and hold certification remain
  lower-layer ownership on an exact task endpoint and cannot be inferred from
  trade wording; result-only simplification is deliberately non-counting here

`ProjectionExpectationCorpusTest` now computes the required bucket presence for
the wave-7 global closure set over the staged wave-1 through wave-6 executable
gate-band rows (`S01`, `S02`, `S03`, `S04`, `S05`, `S06`, `S07`, `S08`, `S09`,
`S10`, `S11`, `S12`, `S13`, `S14`, `S15`, `S16`, `S17`, `S18`, `S19`, `S20`,
`S21`, `S22`, `S23`, `S24`, and `S25`) against the JSONL corpus. It reports all
twenty-five staged bands as coverage-complete.
Countable coverage is axis-specific: route/scope/durability/access/creation/
suppression buckets require `exact` + `admitted` rows, same-cluster near-miss
buckets require rejected near-miss / contrastive / `comparative_false_rival`
rows, and shortcut-negative buckets require rejected nasty-negative / negative
rows.

The previous broad blocker set `S06` / `S10` / `S12` / `S15` is closed only at
the gate-decision level, and `S15` / `S16` are now closed at wave-6
coverage-complete level:

- `S06` now has frozen restriction-route and same-cluster near-miss gates, with
  initial matching coverage rows present.
- `S10` is explicitly knight-only for the current branch's broad scope.
- `S12` now has frozen local access-route and same-cluster near-miss gates, with
  initial matching coverage rows present.
- `S15` now has a frozen route-completeness law: per-position route admission
  is disjunctive, while broad-ready coverage is conjunctive over
  `s13_wing_damage` and `s14_chain_base`, with false-rival rows against
  `S13`, `S14`, and `S16`.
- `S16` now has a frozen suppression law: enemy passer presence must be tied to
  `blockade_hold`, `restriction_hold`, or `non_losing_race`, with certified
  race or hold proof.

Those closures do not provide live admission by themselves. `S15` and `S16`
now have separate matching exact-board corpus rows, and
`ProjectionExpectationCorpus` includes their required pairs in
`broadReadyCoverageGateBands` with empty
`missingRequiredCoveragePairsByBand` for both bands.

`S09`, `S20`, `S23`, and `S25` are in the staged wave-1 executable broad-ready
gate-band set; `S17`, `S18`, `S19`, `S22`, and `S24` are in the staged wave-2
set; `S05`, `S07`, `S08`, and `S21` are in the staged wave-3 set; `S01`,
`S02`, `S03`, and `S04` are in the staged wave-4 set; `S11`, `S13`, and
`S14` are in the staged wave-5 coverage-only set; and `S15` and `S16` are in
the staged wave-6 coverage-only set. `S25` remains limited to
`rank_access_route = cross_wing_rank_switch`; wider rank-access meanings still
require later exact-board coverage. The staged completed sets have matching
JSONL coverage rows and green validation. No staged band is
broad-deployed or newly live as runtime projection because of corpus state
alone.
