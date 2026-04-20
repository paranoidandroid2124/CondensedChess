# Strategy Projection Boundary Matrix

This document freezes the semantic boundary of the `24` strategy projection
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
| `S09` | exploitation of open or semi-open lanes to penetrate | `file_lane_state`, plus either `rook_on_open_file_state` or `open line` support, and a certified entry or penetration gate; deferred rank-access families are support only | `S02`, `S23` | `rook_on_open_file_state` alone; `open/semi-open file` alone |
| `S10` | durable occupation of a certified outpost square | `weak_outpost_square_state`, actual occupancy such as `knight_on_outpost_square`, and a certified durability gate against easy eviction | `S12` | good-piece wording alone |
| `S11` | persistent fixation and attack of a concrete weak pawn target | `weak_pawn_target_state`, same-target fixation or repeated pressure support, and a certified persistence gate on that exact target | `S13`, `S14` | `weak_pawn_target_state` alone |
| `S12` | domination built from chronic weak-square access or diagonal-lane control | `weak_outpost_square_state` or king-independent `diagonal_lane_only` support, plus restriction reinforcement and a certified local access-superiority gate | `S03`, `S10`, `S20` | weak-square wording alone; diagonal wording alone |
| `S13` | use of wing-side pawn-count asymmetry to damage a stronger pawn mass before any single weak pawn need be fixed | `sector_asymmetry_state`, `available_lever_trigger`, and a certified wing-damage route; a pre-existing `weak_pawn_target_state` is optional support, not admission | `S11`, `S14`, `S15` | `majority/minority asymmetry` alone |
| `S14` | attack on the base anchor of a pawn chain | base-contact `available_lever_trigger`, a base-targeted structural-damage route, and certified damage persistence; `fixed chain` is context only, not admission by itself | `S05`, `S11`, `S13` | `fixed chain` alone |
| `S15` | creation of a new passed-pawn or passer complex as the strategic target | `create passer` shell, `candidate_passer`-level support, and a certified passer-creation gate; `PasserComplex` and `ConversionFunnel` are upper consequences only | `S13`, `S16` | `passed_pawn_entity_state` alone; passer wording alone |
| `S16` | suppression of an enemy passer through blockade, restriction, or race denial | enemy `passed_pawn_entity_state`, blockade or restriction support, and a non-losing promotion-race or holding gate | `S15`, `S22`, `S23` | enemy passer presence alone |
| `S17` | improvement or exchange of the worst minor piece to repair a concrete liability | currently blocked for start-ready handoff until the `same_piece_liability_anchor_seed` / `same_piece_repair_route_seed` / `same_piece_exchange_relief_seed` family is frozen; broad `minor_piece_liability` language is support only | `S18`, `S20` | `bad piece` wording alone; generic improving move wording alone |
| `S18` | conversion of an already favorable bishop-pair or minor-piece relation | `bishop_pair_state`, plus a certified conversion path into initiative, structure, material, or endgame edge; broad `favorable_minor_piece_relation` wording is support only | `S17`, `S12`, `S20` | `bishop_pair_state` alone |
| `S19` | simplification because the resulting trade tree stays favorable | `TradeInvariant` plus a favorable downstream certification such as `winning_endgame`, `FortressHoldingShell`, or `material_harvest` support | `S22`, `S24` | trade wording alone |
| `S20` | comparative mobility collapse or domination of the rival army | `mobility_comparison` plus concrete restriction support such as `short_run_slider_gate_restriction` or defender-starvation geometry | `S06`, `S12`, `S17` | `mobility edge` wording alone |
| `S21` | surviving release resource in the center or on the far wing against the rival plan | exact `pawn_push_break_contact_source` plus a certified counterplay-survival gate; center or wing scope is contextual support only | `S01`, `S05`, `S08` | `open center` wording alone; asymmetry wording alone |
| `S22` | holding, neutralization, or fortress-style consolidation | `FortressHoldingShell` plus a certified hold verdict from the `perpetual/fortress` family; `TradeInvariant` is optional support only | `S19`, `S23` | `draw/hold` wording alone |
| `S23` | king activity, opposition, or penetration as the main endgame plan | currently blocked for live admission until the `king_entry_square_seed` / `king_access_route_seed` / `king_opposition_contact_seed` family is frozen; any future admission must also carry a certified entry or conversion gate | `S09`, `S16`, `S22` | endgame wording alone; `whole-board` shell alone |
| `S24` | tactical realization of a target that was already strategically prepared | currently blocked for live admission until the `target_resource_dependency_seed` / `target_attack_convergence_seed` family is frozen; that lower prepared-target package must stay tactic-free, and any later `S24` realization must separately carry an exact tactic or forcing family plus conversion certification | `S04`, `S19` | `pin` / `fork` / `skewer` / `overload` alone; tactical wording alone |

## Current High-Risk Boundary Clusters

- `S01` / `S05` / `S21`: release vs storm vs counterplay wording can drift if
  wing/center carriers are not explicit.
- `S02` / `S03` / `S04`: king-attack wording can collapse without separate
  access, color-complex, and shelter-breach gates.
- `S11` / `S13` / `S14` / `S15` / `S16`: pawn-structure bands need target-type
  separation, not just shared pawn activity.
- `S17` / `S18` / `S20`: piece-quality, piece-edge conversion, and mobility
  domination must not be treated as one evaluative bucket.
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

Bands already treated as start-ready by the current authority and left unchanged
in this pass are:

- `S02`
- `S03`
- `S07`
- `S18`
- `S19`
- `S20`
- `S22`

### Shared Projection Carrier Laws

- same-anchor contact-target law
  - exact basis: same-anchor `available_lever_trigger` plus
    `pawn_push_break_contact_source`
  - frozen target law: projection may use only the exact payload target roles
    already frozen on `pawn_push_break_contact_source`, especially
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
    same-file entry or penetration consequence is present; deferred
    rank-access families remain strengthening only
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
    additionally needs the future `same_piece_liability_anchor_seed` plus one
    same-piece relief seed (`same_piece_repair_route_seed` or
    `same_piece_exchange_relief_seed`)

### Start-Ready Handoff Status

The rows below add the missing start-ready contract pieces for the current
working set only. Core meaning, minimum carriers, rivals, and forbidden
shortcuts remain the rows in the boundary matrix above.

| Band | Canonical exact carrier bundle | Optional strengthening carriers | Required validation scaffold | Status | Promotion criterion / blocker |
| --- | --- | --- | --- | --- | --- |
| `S01` | same-wing `available_lever_trigger` plus same-anchor `pawn_push_break_contact_source` on the attacked king wing, together with `AttackScaffold` and `CertifiedKingSafetyEdge` on the same defending king | `ComparativeKingFragility`; king-theater-linked `file_lane_state` / `diagonal_lane_only` | projection core matrix; `contrastive` vs `S05` and `S21`; `negative` for castling-side wording or shell-only wing context | `start-ready` | promoted once same-wing alignment, attack host, and king-safety edge are all frozen on one defending king without using castling wording as proof |
| `S04` | `KingSafetyShell` plus same-king shelter-breach evidence from the shell payload and `CertifiedKingSafetyEdge` on that same king | `AttackScaffold`; same-anchor contact-target support; `ComparativeKingFragility` | projection core matrix; `contrastive` vs `S01`, `S02`, `S03`, and `S24`; `negative` for shell-only king weakness with no breach gate | `start-ready` | promoted once shelter-breach proof is pinned to one `KingSafetyShell` host and the king-safety deterioration stays on that same defender |
| `S05` | `center` shell plus central `available_lever_trigger`, same-anchor `pawn_push_break_contact_source` with a `center_pawn_target`, and one central-axis continuation carrier | `file_lane_state` on files `c-f`; `AttackScaffold`; `InitiativeWindow`; `CentralContactFront` support only | projection core matrix; `contrastive` vs `S06`, `S14`, and `S21`; `negative` for `open center` wording or `center` shell alone | `start-ready` | promoted once the center-target role and central-axis continuation carrier are frozen as one fail-closed bundle rather than open-center narration |
| `S06` | `structural_space_claim` plus anchor-aligned restriction or outpost support and same-beneficiary persistence proof | `knight_on_outpost_square`; `MobilityComparison`; `InitiativeWindow` | projection core matrix; `contrastive` vs `S05` and `S20`; `negative` for space wording or `MobilityComparison` alone | `start-ready` | promoted once space claim, restriction anchor, and persistence proof stay aligned without admitting from broad `closed center` / `fixed chain` vocabulary |
| `S08` | exact rival `pawn_push_break_contact_source` or rival release-contact bundle together with beneficiary `InitiativeWindow` | `MobilityComparison`; `short_run_slider_gate_restriction` | projection core matrix; `contrastive` vs `S07`, `S21`, and `S20`; `negative` for generic “no counterplay” wording | `start-ready` | promoted once denial is frozen against one exact rival source rather than against broad counterplay prose |
| `S09` | `file_lane_state` plus `rook_on_open_file_state` or `open line` shell support, with one same-file entry or penetration consequence | `AttackScaffold`; `CertifiedKingSafetyEdge`; `MaterialHarvest`; `WinningEndgame` | projection core matrix; `contrastive` vs `S02` and `S23`; `negative` for open-file substrate or `rook_on_open_file_state` alone | `start-ready` | promoted once same-file lane substrate and same-file continuation consequence are frozen together; deferred rank-access stays support only |
| `S10` | `weak_outpost_square_state` plus exact `knight_on_outpost_square` occupancy and same-anchor durability proof | `short_run_slider_gate_restriction`; `MobilityComparison` | projection core matrix; `contrastive` vs `S12`; `negative` for good-piece or occupancy wording alone | `start-ready` | promoted with a first live slice narrowed to exact knight occupancy on a certified outpost square, with durability still required and explicit |
| `S11` | `weak_pawn_target_state` plus same-target pressure support and same-target persistence proof | `MobilityComparison`; `InitiativeWindow` when the denial burden stays on the same target | projection core matrix; `contrastive` vs `S13` and `S14`; `negative` for weak-pawn target alone | `start-ready` | promoted once target identity is frozen across the pressure bundle and the band can no longer drift into generic pawn-quality narration |
| `S12` | `weak_outpost_square_state` or king-independent `diagonal_lane_only` support plus local restriction reinforcement and local access-superiority proof | `short_run_slider_gate_restriction`; `MobilityComparison` | projection core matrix; `contrastive` vs `S03`, `S10`, and `S20`; `negative` for weak-square or diagonal wording alone | `start-ready` | promoted once the access proof stays local and anchor-bound rather than broadening into board-wide domination prose |
| `S13` | `sector_asymmetry_state` plus same-sector `available_lever_trigger` and same-anchor `pawn_push_break_contact_source` against structurally burdened or phalanx-edge targets on the stronger pawn mass | `weak_pawn_target_state`; `file_lane_state` | projection core matrix; `contrastive` vs `S11`, `S14`, and `S15`; `negative` for asymmetry wording alone | `start-ready` | promoted once wing asymmetry is tied to one exact damage route and does not require a pre-existing weak pawn to admit |
| `S14` | base-contact `available_lever_trigger` plus same-anchor `pawn_push_break_contact_source` whose payload explicitly names a `chain_base_target` | `weak_pawn_target_state`; `structural damage` shell context only | projection core matrix; `contrastive` vs `S05`, `S11`, and `S13`; `negative` for `fixed chain` vocabulary alone | `start-ready` | promoted once chain-base targeting is frozen on the exact payload role; `fixed chain` remains context and never the admitting proof |
| `S15` | `create passer` shell plus `candidate_passer` support and one exact creation route already frozen through `S13` or `S14` on the same board | `PasserComplex`; `ConversionFunnel`; `PromotionRace` | projection core matrix; `contrastive` vs `S13` and `S16`; `negative` for `create passer`, `candidate_passer`, or `passed_pawn_entity_state` alone | `start-ready` | promoted once creation is frozen on exact support truth plus an exact creation route, without admitting from an already existing passer entity |
| `S16` | enemy `passed_pawn_entity_state` plus blockade or restriction support and non-losing race / holding proof | `short_run_slider_gate_restriction`; `PromotionRace`; `FortressDrawCertification`; `PerpetualCheckHolding` | projection core matrix; `contrastive` vs `S15`, `S22`, and `S23`; `negative` for enemy passer presence alone | `start-ready` | promoted once suppression must either certify the race or certify the hold; raw blocker pictures are not enough |
| `S17` | future `same_piece_liability_anchor_seed` plus either future `same_piece_repair_route_seed` or future `same_piece_exchange_relief_seed` | anchored liability evidence from `loose_piece_target_state`, `duty_bound_defender`, `weak_outpost_square_state`, `pinned_piece`, and `trapped_piece`; `TradeInvariant` and `MobilityComparison` remain strengthening only | blocked seed matrix plus future projection core matrix with exact rival rows vs `S18` and `S20` | `still not start-ready` | blocker: the future same-piece liability / relief seed family is now frozen in inventory, but none of those lower contracts is live yet; current exact liabilities still do not identify same-piece repair or exchange relief by themselves |
| `S21` | exact owner `pawn_push_break_contact_source` on the center or far wing plus certified `InitiativeWindow` on the same board, with its current `DevelopmentComparison` support and `counterplay_denial_window` / `rival_counterplay_source` / `move_order_relevance_gate` burdens | `available_lever_trigger`; `center` shell or `sector_asymmetry_state`; `AttackScaffold`; `EndgameRaceScaffold` | projection core matrix; `contrastive` vs `S01`, `S05`, and `S08`; `negative` for exact source alone, `InitiativeWindow` alone, or exact source with `InitiativeWindow` still `Deferred` / `Rejected` | `start-ready` | promoted once the owner source is tied to the current development-led `InitiativeWindow` slice so best-defense-fragile counterplay stays out and openness / asymmetry wording cannot backfill survival semantics |
| `S23` | future `king_entry_square_seed` / `king_access_route_seed` / `king_opposition_contact_seed` package only | existing `file_lane_state`, `WinningEndgame`, and `PromotionRace` remain support only | blocked seed matrix plus future projection core matrix | `blocked` | blocker: frozen future support seeds are still missing as live lower contracts |
| `S24` | future `target_resource_dependency_seed` / `target_attack_convergence_seed` package only | exact tactics and conversion certifications remain external realization companions only | blocked seed matrix plus future projection core matrix | `blocked` | blocker: frozen future support seeds are still missing as live lower contracts |

Current `start-ready` rows authorize narrow first-slice projection work and
projection corpus authoring only.

They do **not** yet prove broader deployment.

Current broader-coverage caution remains highest on:

- `S06` and `S12`, where restriction/access proof still needs denser same-cluster
  near-miss pressure
- `S10`, whose current first slice is exact `knight_on_outpost_square` occupancy
  only
- `S15`, whose current first slice recognizes only the exact creation routes
  already frozen through `S13` and `S14`

### Broad-Ready Promotion Freeze

No strategy band is `broad-ready` on the current branch yet.

A future `broad-ready` claim must stay fail-closed on two contracts:

- the projection core matrix plus `coverageAxis` / `coverageBucket` rows in
  `projection-expectations.jsonl`
- the band-local breadth gates frozen below

| Band | Broad-ready coverage gate / blocker |
| --- | --- |
| `S06` | requires `coverageAxis = restriction_route` with buckets `outpost_anchor` and `non_outpost_space_bind`, plus `coverageAxis = same_cluster_near_miss` with buckets `vs_s05` and `vs_s20` |
| `S10` | broad-ready blocked until occupancy scope is frozen beyond the current `knight_only` first slice, or the branch explicitly freezes `knight_only` as the permanent scope of `S10` |
| `S12` | requires `coverageAxis = access_route` with buckets `weak_square_route` and `diagonal_lane_route`, plus repeated `same_cluster_near_miss` buckets against `S03`, `S10`, and `S20` |
| `S15` | current creation-route inventory is frozen to `coverageAxis = creation_route` with buckets `s13_wing_damage` and `s14_chain_base`; broad-ready stays blocked until that route inventory is either proven complete for the band or extended by a separate lower-support freeze |

All other `start-ready` bands remain narrow-only until their own breadth gates
are frozen explicitly.
