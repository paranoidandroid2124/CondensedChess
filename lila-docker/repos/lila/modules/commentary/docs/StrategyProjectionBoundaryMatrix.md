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

- A strategy band is admitted only from the row-frozen lower carriers it names:
  exact `Witness` / support-seed, `Object`, `Delta`, or `Certification`
  carriers as applicable, plus the required row-specific projection evidence
  mirror for live runtime rows.
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
| `S11` | persistent fixation and attack of a concrete weak pawn target | `weak_pawn_target_state`, same-target fixation or repeated pressure support, and current-board fixed-pawn persistence proof on that exact target; certification families are support-only, not S11 truth owners | `S13`, `S14` | `weak_pawn_target_state` alone |
| `S12` | domination built from chronic weak-square access or diagonal-lane control | `weak_outpost_square_state` or king-independent `diagonal_lane_only` support, plus restriction reinforcement and a certified local access-superiority gate | `S03`, `S10`, `S20` | weak-square wording alone; diagonal wording alone |
| `S13` | use of wing-side pawn-count asymmetry to damage a stronger pawn mass before any single weak pawn need be fixed | `sector_asymmetry_state`, same-wing `available_lever_trigger`, and a certified wing-damage route; a pre-existing `weak_pawn_target_state` is optional support, not admission | `S11`, `S14`, `S15` | `majority/minority asymmetry` alone |
| `S14` | attack on the base anchor of a pawn chain | base-contact `available_lever_trigger`, a base-targeted structural-damage route, and certified damage persistence; `fixed chain` is context only, not admission by itself | `S05`, `S11`, `S13`, `S15` | `fixed chain` alone |
| `S15` | creation of a new passed-pawn or passer complex as the strategic target | live `candidate_passer` root support on the same pawn as the S13/S14 contact source plus `passer_creation_route_certified` evidence for an exact same-candidate S13 wing-damage or S14 chain-base route; `create passer`, `PasserComplex`, `ConversionFunnel`, and `PromotionRace` are support-only consequences | `S13`, `S14`, `S16` | `candidate_passer` alone; `create passer` shell alone; `passed_pawn_entity_state` alone; split-anchor route; passer wording alone |
| `S16` | suppression of an enemy passer through blockade, restriction, or race denial | enemy `passed_pawn_entity_state`, blockade or restriction support, and a non-losing promotion-race or holding gate | `S15`, `S22`, `S23` | enemy passer presence alone |
| `S17` | improvement or exchange of the worst minor piece to repair a concrete liability | live lower support includes `same_piece_liability_anchor_seed` plus either `same_piece_repair_route_seed` or `same_piece_exchange_relief_seed`; start-ready handoff is now frozen through `StrategyProjectionAdmission` with `liability_relief_certified` evidence bound to the same piece; broad `minor_piece_liability` language is support only | `S18`, `S20` | `bad piece` wording alone; generic improving move wording alone |
| `S18` | conversion of an already favorable bishop-pair or minor-piece relation | active same-owner current-board `bishop_pair_state`, plus a certified current-board conversion path through `InitiativeWindow`, `MobilityComparison`, or active bishop-member `MaterialHarvest`; S18 evidence must mirror the lower certification family, exact target squares, and bishop-pair members; `WinningEndgame` is future support only, and broad `favorable_minor_piece_relation` wording is support only | `S17`, `S12`, `S20` | `bishop_pair_state` alone |
| `S19` | simplification because the exact trade task stays favorable under the current lower law | `TradeInvariant` plus a same-task certification currently limited to `MaterialHarvest` on the move board or `FortressDrawCertification` on the post-trade hold board; `FortressHoldingShell` is object support only and never the certification itself; result-only simplification remains non-counting until a same-task result-certification law exists | `S22`, `S24` | trade wording alone |
| `S20` | comparative mobility collapse or domination of the rival army | `mobility_comparison` plus concrete restriction support such as `short_run_slider_gate_restriction` or defender-starvation geometry | `S06`, `S12`, `S17` | `mobility edge` wording alone |
| `S21` | surviving release resource in the center or on the far wing against the rival plan | exact `pawn_push_break_contact_source` plus a certified counterplay-survival gate; center or wing scope is contextual support only | `S01`, `S05`, `S08` | `open center` wording alone; asymmetry wording alone |
| `S22` | holding, neutralization, or fortress-style consolidation | same-holder `FortressHoldingShell` plus certified `FortressDrawCertification`, or certified current-board `PerpetualCheckHolding`; `TradeInvariant` is optional support only, and `SupportOnly` / `Deferred` hold claims remain non-admitting | `S19`, `S23` | `draw/hold` wording alone; shell-only or checking-sequence wording alone |
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
`S05`, `S06`, `S07`, `S08`, `S11`, `S13`, `S14`, `S15`, `S16`, `S17`, `S18`, `S19`, `S21`, `S22`, `S23`, `S24`, and `S25`, as frozen by
`StrategyProjectionScopeContract.startReadyBandIds` and enforced by
`StrategyProjectionAdmission`.

All other `S01-S25` bands have semantic, row-burden, helper-law, lower-carrier,
and exact-validation freezes only. They are coverage-only in
`StrategyProjectionCoverageContract`; current live `StrategyProjectionAdmission`
must reject them until a later runtime admission boundary is explicitly
authored. Older "start-ready" handoff prose for
non-`S05`/`S06`/`S07`/`S08`/`S11`/`S13`/`S14`/`S15`/`S16`/`S17`/`S18`/`S19`/`S21`/`S22`/`S23`/`S24`/`S25` bands is not runtime authority.

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
| `S05` | live central `available_lever_trigger` plus same-anchor `pawn_push_break_contact_source` with a recomputed center-file source, center-file target, and `center_release_route_certified` evidence that mirrors the same source, target, and route (`center_pawn_target` or `central_axis_continuation`) | `file_lane_state` on files `c-f`; `AttackScaffold`; `InitiativeWindow` as certification support only; `CentralContactFront` as object support only; none are truth owners | `projection-expectations.jsonl`; exact/near/nasty rows vs `S06`, `S14`, and `S21`; `StrategyProjectionAdmissionTest` stale/wrong-source/wrong-target/wrong-route boundaries | `start-ready` | promoted only for exact current-board same-anchor central release rows; open-center wording, center shell, optional strengthening, stale evidence, adjacent S06/S14/S21 rivals, wrong source, wrong target, non-center source, or wrong route remain fail-closed |
| `S06` | live `structural_space_claim` plus route-linked `knight_on_outpost_square` or `short_run_slider_gate_restriction`, support-only `SpaceBindRestrictionCertification` exposing the same route-host link, and `space_bind_restriction_route_certified` evidence mirroring route anchor, host, sector, and route-specific restriction/outpost square | `weak_outpost_square_state`; `MobilityComparison`; `InitiativeWindow`; `SpaceBindRestrictionCertification` remains lower support only, not projection truth | projection core matrix; `contrastive` vs `S05` and `S20`; `negative` for space wording or `MobilityComparison` alone; exact S05 center-release and exact S20 domination false-rival rows must stay non-admitting | `start-ready` | S06 live runtime admission is promoted only for exact current-board same-host space-bind routes whose outpost attacks, or restriction gate touches, the structural host claim; structural-space wording, mobility/certification alone, optional strengthening, stale evidence, wrong route anchor/host/sector, and adjacent S05/S20 rivals remain fail-closed |
| `S07` | live same-owner `DevelopmentComparison` plus certified same-board `InitiativeWindow`, with board-anchored `initiative_conversion_route_certified` evidence whose `initiative_conversion_route` is exactly `development_led_window` or `move_right_window`; `OpeningDevelopmentRegime` remains support for `DevelopmentComparison`, not projection truth | `OpeningDevelopmentRegime` object support; development wording and initiative wording alone; adjacent S08 rival-source denial and owner `weak_pawn_target_state` remain non-admitting | `projection-expectations.jsonl`; exact/near/nasty rows vs `S08`; `StrategyProjectionAdmissionTest` stale evidence/certification, wrong route, missing lower certification, and S08 false-rival boundaries | `start-ready` | S07 live runtime admission is promoted only for the exact current-board initiative-conversion slice; lower Object/Certification carriers remain lower truth owners, and optional strengthening, development-only, initiative-only, stale evidence, wrong route, target-state leakage, or S08 denial rows remain fail-closed |
| `S08` | live exact rival `pawn_push_break_contact_source` with beneficiary `InitiativeWindow`, a current-board rival release reserve, no owner-side S21 survival carrier, and `counterplay_denial_route_certified` evidence mirroring the same rival source, target, denial route, and certification family | `MobilityComparison`; `short_run_slider_gate_restriction`; `InitiativeWindow` remains lower Certification support, not projection truth | projection core matrix; `contrastive` vs `S07`, `S21`, and `S20`; `negative` for generic "no counterplay" wording; same rival source, target, and denial route mirroring plus wrong-source/wrong-target/wrong-route/stale-evidence rejection | `start-ready` | S08 live runtime admission is promoted only for the exact current-board rival-source denial slice; `InitiativeWindow` alone, mobility/restriction support, owner-side S21 survival carriers, S07/S20/S21 rivals, stale evidence, wrong source, wrong target, or wrong route remain fail-closed |
| `S09` | `file_lane_state` plus `rook_on_open_file_state` or `open line` shell support, with one same-file entry or penetration consequence | `AttackScaffold`; `CertifiedKingSafetyEdge`; `MaterialHarvest`; `WinningEndgame` | projection core matrix; `contrastive` vs `S02`, `S23`, and `S25`; `negative` for open-file substrate or `rook_on_open_file_state` alone | `coverage-complete` | broad coverage only; same-file lane substrate and same-file continuation consequence are frozen together, while generic rank-access stays non-admitting outside `S25` |
| `S10` | `weak_outpost_square_state` plus exact `knight_on_outpost_square` occupancy and same-anchor durability proof | `short_run_slider_gate_restriction`; `MobilityComparison` | projection core matrix; `contrastive` vs `S12`; `negative` for good-piece or occupancy wording alone | `coverage-complete` | broad coverage only; current broad scope is knight-only on a certified outpost square, with durability still required and explicit |
| `S11` | live `weak_pawn_target_state` on the same target square plus current-board same-target owner pressure and fixed-pawn persistence proof; `weak_pawn_target_pressure_persistence_certified` evidence must mirror the target, route, pressure sources, and fixed persistence | `MobilityComparison`; `InitiativeWindow` when the denial burden stays on the same target; no support seed, Object, Delta, or Certification truth owner | `projection-expectations.jsonl`; exact/false-rival/nasty rows vs `S13` and `S14`; `StrategyProjectionAdmissionTest` stale-evidence boundary | `start-ready` | promoted only for exact current-board same-target fixation or repeated-pressure weak-pawn rows; weak-pawn presence alone, target swaps, optional strengthening, stale evidence, and adjacent S13/S14 rivals remain fail-closed |
| `S12` | `weak_outpost_square_state` or king-independent `diagonal_lane_only` support plus local restriction reinforcement and local access-superiority proof | `short_run_slider_gate_restriction`; `MobilityComparison` | projection core matrix; `contrastive` vs `S03`, `S10`, and `S20`; `negative` for weak-square or diagonal wording alone | `coverage-complete` | broad coverage only; access proof stays local and anchor-bound rather than broadening into board-wide domination prose |
| `S13` | live `sector_asymmetry_state` plus same-wing-sector `available_lever_trigger` and same-anchor `pawn_push_break_contact_source` against structurally burdened or non-chain-base phalanx-edge targets on the stronger pawn mass; `wing_damage_route_certified` evidence mirrors source, target, sector, and route | `weak_pawn_target_state`; `file_lane_state`; no support seed, Object, Delta, or Certification truth owner | `projection-expectations.jsonl`; exact/false-rival/nasty rows vs `S11`, `S14`, and `S15`; `StrategyProjectionAdmissionTest` stale-evidence boundary | `start-ready` | promoted only for exact current-board same-wing-sector wing-damage contact routes; center-sector damage, sector asymmetry alone, pre-existing weak-pawn support, optional strengthening, stale evidence, and adjacent S11/S14/S15 rivals remain fail-closed |
| `S14` | live base-contact `available_lever_trigger` plus same-anchor `pawn_push_break_contact_source` whose exact non-center target pawn square is recomputed as a chain base with forward support; `base_contact_continuation` additionally requires that forward-supported pawn to continue the defender chain; `chain_base_contact_route_certified` evidence mirrors source, target, route, and forward-support squares | `weak_pawn_target_state`; `structural damage` shell context only; no support seed, Object, Delta, or Certification truth owner | `projection-expectations.jsonl`; exact/false-rival/nasty rows vs `S05`, `S11`, `S13`, and `S15`; `StrategyProjectionAdmissionTest` stale-evidence boundary | `start-ready` | promoted only for exact current-board same-anchor non-center chain-base contact rows; route tokens must match the recomputed exact-board route, and fixed-chain shells, generic structural damage, weak-pawn support, optional strengthening, stale evidence, and adjacent S05/S11/S13/S15 rivals remain fail-closed |
| `S15` | live `candidate_passer` support on the same pawn as an exact S13 wing-damage or S14 chain-base contact source; `passer_creation_route_certified` evidence must mirror the candidate, target, creation route, and route-specific carrier fields | `create passer` shell; `PasserComplex`; `ConversionFunnel`; `PromotionRace` as support only, not truth owners | `projection-expectations.jsonl`; exact/false-rival/nasty rows vs `S13`, `S14`, and `S16`; `StrategyProjectionAdmissionTest` stale-evidence boundary; `negative` for `create passer`, `candidate_passer`, `passed_pawn_entity_state`, or split-anchor route alone | `start-ready` | promoted only for exact current-board same-candidate passer-creation routes; shell-only wording, candidate-only support, an already existing passer entity, split anchors, optional strengthening, stale evidence, and adjacent S13/S14/S16 rivals remain fail-closed |
| `S16` | live enemy `passed_pawn_entity_state` plus route-specific proof: owner blocker on the enemy passer front square with certified `FortressDrawCertification` whose support targets the same passer/blocker shell for `blockade_hold`, the same blocker exposed as an owner `short_run_slider_gate_restriction` gate square plus certified same-board `PerpetualCheckHolding` for `restriction_hold`, or certified `PromotionRace` whose support targets include the enemy passer for `non_losing_race`; `passer_suppression_route_certified` evidence must mirror the same enemy passer, route, blocker when applicable, and certification family | `short_run_slider_gate_restriction`; `PromotionRace`; `FortressDrawCertification`; `PerpetualCheckHolding`; `FortressHoldingShell` / `EndgameRaceScaffold` as lower support only, never projection truth owners | `projection-expectations.jsonl`; exact/false-rival/nasty rows vs `S15`, `S22`, and `S23`; `negative` for enemy passer presence alone or blocker picture alone; `StrategyProjectionAdmissionTest` stale-evidence and adjacent-rival boundary | `start-ready` | wave-6 exact-board coverage rows remain countable; live admission is limited to exact current-board same-enemy-passer suppression routes, and raw passer/blocker/restriction/hold/race pictures, stale evidence, optional strengthening, or adjacent S15/S22/S23 rivals remain fail-closed |
| `S17` | live `same_piece_liability_anchor_seed` plus either live `same_piece_repair_route_seed` or live `same_piece_exchange_relief_seed`, with `liability_relief_certified` evidence on the same piece and a matching `relief_kind` | current live anchor seed reads `loose_piece_target_state`, `pinned_piece`, and `trapped_piece`; `duty_bound_defender`, `weak_outpost_square_state`, `TradeInvariant`, and `MobilityComparison` remain strengthening only | `projection-expectations.jsonl`; exact/near/nasty/rival rows vs `S18` and `S20`; `StrategyProjectionAdmissionTest` stale-evidence boundary | `start-ready` | promoted once the same-piece liability anchor, same-piece relief seed, and exact relief evidence are bound to the same piece; seed presence alone still does not release `S17` |
| `S18` | live same-owner current-board `bishop_pair_state` with at least one active member plus a certified same-board conversion route: `InitiativeWindow`, `MobilityComparison`, or active bishop-member `MaterialHarvest`; matching S18 projection evidence must mirror the lower certification family, exact target squares, and bishop-pair members, and must be present on the board for initiative/structure or on the exact harvesting bishop for material | `WinningEndgame` is support-only future carrier because it cannot coexist with current-board bishops; `DevelopmentComparison`, `short_run_slider_gate_restriction`, favorable-minor wording, and adjacent `S12`/`S17`/`S20` facts remain non-admitting | `projection-expectations.jsonl`; exact/near/nasty/rival rows vs `S12`, `S17`, and `S20`; `StrategyProjectionAdmissionTest` stale evidence/certification boundary | `start-ready` | promoted only for exact current-board bishop-pair conversion routes with `bishop_pair_initiative_conversion_certified`, `bishop_pair_structure_conversion_certified`, or `bishop_pair_material_conversion_certified`; bishop-pair state alone, optional strengthening, stale evidence, target/task drift, and adjacent rivals remain fail-closed |
| `S19` | canonical `TradeInvariant` transition plus same-task `MaterialHarvest` on the move board or `FortressDrawCertification` on the post-trade hold board | `WinningEndgame`, `FortressHoldingShell`, and prepared-target support remain non-admitting unless the exact S19 delta/certification route is satisfied | `projection-expectations.jsonl`; exact/near/nasty rows vs `S22` and `S24`; `StrategyProjectionAdmissionTest` stale delta/certification boundary | `start-ready` | promoted only for the delta-backed simplification routes; result-only, material-only, trade wording, stale delta, and adjacent S22/S24 rivals remain fail-closed |
| `S21` | live exact non-center owner `pawn_push_break_contact_source` into a center-file target, or exact non-center owner source into a non-center target, plus certified `InitiativeWindow` on the same board; `counterplay_survival_route_certified` evidence mirrors the same owner source, target, route, and `InitiativeWindow` certification family | `available_lever_trigger`; `center` shell or `sector_asymmetry_state`; `AttackScaffold`; `EndgameRaceScaffold`; `DevelopmentComparison` and `InitiativeWindow` remain lower support/certification, not projection truth owners | `projection-expectations.jsonl`; exact/near/nasty rows vs `S01`, `S05`, and `S08`; `StrategyProjectionAdmissionTest` stale evidence/certification boundary; `negative` for exact source alone, center-source/center-target S05-shaped release, certified `InitiativeWindow` alone, or exact source with `InitiativeWindow` still `Deferred` / `Rejected` | `start-ready` | S21 live runtime admission is promoted only for exact current-board same-owner source-survival rows that do not use S05's center-source/center-target release shape; the counterplay survival route is start-ready, while source-only, initiative-only, open-center/asymmetry wording, stale evidence, optional strengthening, and adjacent S01/S05/S08 rivals remain fail-closed |
| `S22` | same-root `FortressHoldingShell` plus certified `FortressDrawCertification` bound to the same holder king, or certified current-board `PerpetualCheckHolding` with live S22 evidence | `TradeInvariant`, king activity, and material/result families remain support or rival context only; object shell, `SupportOnly`, and `Deferred` certification claims do not admit the band | `projection-expectations.jsonl`; exact/near/nasty/negative rows vs `S19` and `S23`; `StrategyProjectionAdmissionTest` stale-evidence boundary | `start-ready` | promoted once `fortress_hold_certified` or `perpetual_hold_certified` evidence is bound to the same root and the lower object/certification extractor revalidates the certified hold from an external same-root `CertificationEvidenceBundle`; projection admission must not synthesize best-defense proof; shell presence, checking-sequence wording, stale evidence, and adjacent S19/S23 rivals remain fail-closed |
| `S23` | live `king_entry_square_seed` / `king_access_route_seed` sharing the same entry square, or live `king_opposition_contact_seed` sharing the same contact square, plus matching projection evidence | existing `file_lane_state`, `WinningEndgame`, and `PromotionRace` remain support only unless tied through the explicit `StrategyProjectionAdmission` evidence kinds | `projection-expectations.jsonl`; exact/near/nasty/rival rows vs `S09`, `S16`, and `S22`; `StrategyProjectionAdmissionTest` | `start-ready` | promoted once entry+route+entry-conversion evidence share one entry square, or opposition+opposition evidence share one contact square; seed presence alone still does not release `S23` |
| `S24` | live `target_resource_dependency_seed` and live `target_attack_convergence_seed` on the same target, plus same-target forcing and conversion evidence | exact tactics and conversion certifications remain external realization companions only and must bind to the prepared target through `StrategyProjectionAdmission` | `projection-expectations.jsonl`; exact/near/nasty/rival rows vs `S04` and `S19`; `StrategyProjectionAdmissionTest` | `start-ready` | promoted once dependency, convergence, forcing realization, and conversion certification all name the same target; seed presence or tactic/result wording alone still does not release `S24` |
| `S25` | live `rank_corridor_state_seed` plus `rank_access_consequence_certified` evidence on the same source anchor, same `entry_square`, and `cross_wing_rank_switch` kind | `AttackScaffold`, `CertifiedKingSafetyEdge`, `file_lane_state`, `diagonal_lane_only`, `weak_outpost_square_state`, and exact tactic/certification families remain support only unless the explicit rank-corridor law is satisfied | `projection-expectations.jsonl`; exact/near/nasty/rival rows vs `S02`, `S03`, `S04`, `S09`, `S12`, `S23`, and `S24`; `StrategyProjectionAdmissionTest`; `HorizontalRankAccessSupportSeedRulesTest` | `start-ready` | promoted once legal current-board cross-wing rank switching is anchored to one rook/queen source and one entry square; seed presence alone, piece-on-rank pictures, file substrate, king activity, weak-square access, tactics, or rook-lift prose still do not release `S25` |

Only `S05`, `S06`, `S07`, `S08`, `S11`, `S13`, `S14`, `S15`, `S16`, `S17`, `S18`, `S19`, `S21`, `S22`, `S23`, `S24`, and `S25` are current live-runtime start-ready
rows.
The compact runtime-key form is
`S05`/`S06`/`S07`/`S08`/`S11`/`S13`/`S14`/`S15`/`S16`/`S17`/`S18`/`S19`/`S21`/`S22`/`S23`/`S24`/`S25`.
Coverage-complete rows authorize corpus validation only. They do **not**
authorize broad deployment or live runtime admission.

S06 live runtime admission: `space_bind_restriction_route_certified` is in
`StrategyProjectionScopeContract.requiredEvidenceKindsByBand` for `S06`.
Admission requires exact current-board `structural_space_claim`, an outpost or
short-run restriction linked to that host through the certification
`route_host_links`, support-only `SpaceBindRestrictionCertification`,
same-task evidence mirroring of route / anchor / host / restriction, and exact
S05 center-release and exact S20 domination false-rival rejection. S06 broad
coverage rows remain countable, but only those exact live evidence rows are
runtime authority.

Current broader-deployment caution remains highest on all completed staged
bands, because coverage-complete corpus rows still do not make any projection
band broad-deployed or live runtime by themselves. `S15` is live only through
its explicit same-candidate admission branch, and `S16` is live only through
its explicit same-enemy-passer suppression branch.

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
(`S05`, `S06`, `S07`, `S08`, `S11`, `S13`, `S14`, `S15`, `S16`, `S17`, `S18`, `S19`, `S21`, `S22`, `S23`, `S24`, and `S25`), and `StrategyProjectionAdmission` must
fail-closed for those coverage-only bands.

| Band | Required broad-ready `coverageAxis` / `coverageBucket` rows | Residual blocker / decision before promotion |
| --- | --- | --- |
| `S01` | `storm_route`: `same_wing_contact`, `attack_edge_same_king`; `same_cluster_near_miss`: `vs_s05`, `vs_s21`; `shortcut_negative`: `castling_side_only`, `wing_shell_only` | wave-4 exact-board coverage rows are present and green; runtime projection admission remains non-live, and storm coverage stays tied to same-wing contact plus attack and king-safety carriers, not castling-side prose |
| `S02` | `attack_concentration_route`: `direct_piece_concentration`, `lane_strengthened_concentration`; `same_cluster_near_miss`: `vs_s03`, `vs_s04`, `vs_s09`; `shortcut_negative`: `king_attack_label_only`, `battery_or_lift_only` | wave-4 exact-board coverage rows are present and green; runtime projection admission remains non-live, and direct king-ring concentration must not collapse into diagonal, shelter-breach, or file-penetration meaning |
| `S03` | `diagonal_attack_route`: `king_facing_diagonal_entry`, `fragility_linked_diagonal`; `same_cluster_near_miss`: `vs_s02`, `vs_s12`; `shortcut_negative`: `bishop_pair_only`, `non_king_diagonal_pressure` | wave-4 exact-board coverage rows are present and green; runtime projection admission remains non-live, and the decisive carrier must remain king-facing rather than broad bishop or weak-square pressure |
| `S04` | `shelter_breach_route`: `shell_payload_breach`, `support_break_breach`; `same_cluster_near_miss`: `vs_s01`, `vs_s02`, `vs_s03`, `vs_s24`; `shortcut_negative`: `king_shelter_wording_only`, `generic_attack_pressure` | wave-4 exact-board coverage rows are present and green; runtime projection admission remains non-live, and breach plus king-safety deterioration must bind to the same defender king |
| `S05` | `center_release_route`: `center_pawn_target`, `central_axis_continuation`; `same_cluster_near_miss`: `vs_s06`, `vs_s14`, `vs_s21`; `shortcut_negative`: `open_center_wording_only`, `center_shell_only` | wave-3 exact-board coverage rows remain countable; live runtime admission is limited to exact same-anchor center-file source/center-file target release routes with `center_release_route_certified` |
| `S06` | `restriction_route`: `outpost_anchor`, `non_outpost_space_bind`; `same_cluster_near_miss`: `vs_s05`, `vs_s20`; `shortcut_negative`: `space_wording_only`, `mobility_comparison_only` | exact-board coverage rows remain countable; live admission is limited to the exact same-host route evidence and support-only `SpaceBindRestrictionCertification` with a matching `route_host_links` payload |
| `S07` | `initiative_conversion_route`: `development_led_window`, `move_right_window`; `same_cluster_near_miss`: `vs_s08`; `shortcut_negative`: `development_lead_only`, `initiative_wording_only` | wave-3 exact-board coverage rows remain countable; S07 live runtime admission now uses `initiative_conversion_route_certified` while rejecting development-only, initiative-only, stale, and S08 false-rival rows |
| `S08` | `denial_route`: `rival_break_source_suppressed`, `rival_counterplay_source_suppressed`; `same_cluster_near_miss`: `vs_s07`, `vs_s20`, `vs_s21`; `shortcut_negative`: `no_counterplay_wording_only`, `initiative_window_only` | wave-3 exact-board coverage rows remain countable; live runtime admission is limited to exact rival-source denial with `counterplay_denial_route_certified`, same-board `InitiativeWindow` support, no owner-side S21 survival carrier, and same-source/target/route evidence |
| `S09` | `penetration_route`: `open_file_entry`, `semi_open_file_entry`, `same_file_penetration`; `same_cluster_near_miss`: `vs_s02`, `vs_s23`, `vs_s25`; `shortcut_negative`: `file_substrate_only`, `rook_on_file_only` | initial exact-board coverage rows are present and green; S09 still must keep lane substrate and continuation on the same file, and rank-access support remains non-admitting unless the separate `S25` rank-corridor law is satisfied |
| `S10` | `occupancy_scope`: `knight_only`; `durability_route`: `same_anchor_eviction_denial`; `same_cluster_near_miss`: `vs_s12`; `shortcut_negative`: `good_piece_wording_only`, `occupancy_without_durability`, `non_knight_occupancy_without_freeze` | initial exact-board coverage rows are present and green; legacy scope blocker remains closed as current-branch broad `S10` is knight-only, and any non-knight outpost concept requires a later separate lower-support freeze |
| `S11` | `target_pressure_route`: `same_target_fixation`, `same_target_repeated_pressure`; `same_cluster_near_miss`: `vs_s13`, `vs_s14`; `shortcut_negative`: `weak_pawn_target_only`, `target_swap_by_prose` | wave-5 exact-board coverage rows remain countable; live admission is limited to same-target weak-pawn pressure plus current fixed-pawn persistence evidence, and weak-pawn identity must stay stable across target, pressure, and proof |
| `S12` | `access_route`: `weak_square_route`, `diagonal_lane_route`; `same_cluster_near_miss`: `vs_s03`, `vs_s10`, `vs_s20`; `shortcut_negative`: `weak_square_wording_only`, `diagonal_wording_only` | initial exact-board coverage rows are present and green; runtime projection admission remains non-live |
| `S13` | `wing_damage_route`: `phalanx_edge_target`, `structurally_burdened_target`; `same_cluster_near_miss`: `vs_s11`, `vs_s14`, `vs_s15`; `shortcut_negative`: `sector_asymmetry_only`, `preexisting_weak_pawn_only` | wave-5 exact-board coverage rows remain countable; live admission is limited to same-wing-sector wing-damage contact against an exact non-chain-base phalanx-edge or structurally burdened target, and pawn-count asymmetry must tie to one exact wing damage route against the stronger mass |
| `S14` | `chain_base_route`: `chain_base_target`, `base_contact_continuation`; `same_cluster_near_miss`: `vs_s05`, `vs_s11`, `vs_s13`, `vs_s15`; `shortcut_negative`: `fixed_chain_only`, `generic_structural_damage` | wave-5 exact-board coverage rows remain countable; live admission is limited to same-anchor non-center chain-base contact whose evidence mirrors contact source, target, route, and exact forward-support squares; `fixed chain` stays context only |
| `S15` | `creation_route`: `s13_wing_damage`, `s14_chain_base`; `same_cluster_near_miss`: `vs_s13`, `vs_s14`, `vs_s16`; `shortcut_negative`: `candidate_passer_only`, `create_passer_shell_only`, `existing_passer_entity_only`, `split_anchor_creation_route` | wave-6 exact-board coverage rows remain countable; live admission is limited to one same-candidate creation route per position, with `passer_creation_route_certified` evidence mirroring the candidate/contact source, target, route token, and S13/S14 route-specific carrier fields |
| `S16` | `suppression_route`: `blockade_hold`, `restriction_hold`, `non_losing_race`; `same_cluster_near_miss`: `vs_s15`, `vs_s22`, `vs_s23`; `shortcut_negative`: `enemy_passer_presence_only`, `blocker_picture_only` | wave-6 exact-board coverage rows are present and green: suppression requires certified race or hold proof, and enemy passer presence plus blocker pictures cannot admit the band; `passer_suppression_route_certified` is live as the same-passer evidence name and admits only when current-board lower extraction revalidates the same enemy passer and route-specific proof |
| `S17` | `liability_relief_route`: `repair_route`, `exchange_relief`; `same_cluster_near_miss`: `vs_s18`, `vs_s20`; `shortcut_negative`: `bad_piece_wording_only`, `generic_improving_move` | must keep liability anchor, relief seed, and relief certification on the same piece |
| `S18` | `minor_edge_conversion_route`: `bishop_pair_to_initiative`, `bishop_pair_to_structure`, `bishop_pair_to_material`; `same_cluster_near_miss`: `vs_s12`, `vs_s17`, `vs_s20`; `shortcut_negative`: `bishop_pair_state_only`, `minor_edge_label_only` | must prove conversion of the favorable relation, not the relation alone |
| `S19` | `simplification_route`: `trade_invariant_to_hold`, `trade_invariant_to_material`; `same_cluster_near_miss`: `vs_s22`, `vs_s24`; `shortcut_negative`: `trade_wording_only`, `material_reduction_only` | must keep same-task certification attached to the exact trade invariant; result-only simplification remains fail-closed |
| `S20` | `domination_route`: `mobility_plus_restriction`, `defender_starvation`; `same_cluster_near_miss`: `vs_s06`, `vs_s12`, `vs_s17`; `shortcut_negative`: `mobility_edge_only`, `restriction_without_comparison` | initial exact-board coverage rows are present and green; positive domination routes require actual `MobilityComparison` plus concrete restriction or duty-bound defender support, not generic mobility-edge wording |
| `S21` | `counterplay_survival_route`: `center_source_survives`, `far_wing_source_survives`; `same_cluster_near_miss`: `vs_s01`, `vs_s05`, `vs_s08`; `shortcut_negative`: `break_source_only`, `deferred_initiative_only` | wave-3 exact-board coverage rows remain countable; live admission is limited to exact non-center owner `pawn_push_break_contact_source` plus same-board certified `InitiativeWindow` and `counterplay_survival_route_certified` evidence mirroring the same source, target, route, and certification family; center-source/center-target release remains S05-adjacent, and certified `InitiativeWindow` alone is not S21 proof |
| `S22` | `hold_route`: `fortress_draw_hold`, `perpetual_hold`; `same_cluster_near_miss`: `vs_s19`, `vs_s23`; `shortcut_negative`: `draw_wording_only`, `fortress_shell_only`, `checking_sequence_wording`, `perpetual_deferred` | must use certified hold verdicts; same-holder fortress support and certification must bind to the holder king, perpetual support must be certified on the current board, and consolidation wording, shell presence, loose checking sequences, or deferred hold evidence stay below projection |
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

The wave-5 JSONL rows are now authored and countable. This keeps all three
bands broad-ready for coverage; `S11`, `S13`, and `S14` have narrow live
`StrategyProjectionAdmission` branches:

- row-specific burden gates are the broad-ready gate table rows for `S11`,
  `S13`, and `S14`
- shared helper law stays exact-board and fail-closed:
  - `S11`: `weak_pawn_target_state`, pressure, and the current fixed-pawn
    persistence proof must stay on the same weak-pawn square; target swaps by
    prose do not count
  - `S13`: `sector_asymmetry_state`, same-wing-sector lever, and same-anchor
    `pawn_push_break_contact_source` must bind to a target role recomputed from
    the exact board as non-chain-base `phalanx_edge_target` or
    `structurally_burdened_target`; asymmetry or a pre-existing weak pawn alone
    does not count
  - `S14`: base-contact lever and same-anchor contact source must bind to a
    non-center target square recomputed from the exact board as
    `chain_base_target`; `base_contact_continuation` additionally requires the
    target's forward-supported pawn to support another defender pawn on the
    same board;
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
  - `S13`: sector asymmetry, same-wing-sector lever/contact source, exact target
    role as non-chain-base phalanx-edge or structurally burdened, and
    weak-pawn/asymmetry-only non-counting. The executable scaffold token set is
    `sector_asymmetry_present`,
    `same_sector_lever_and_break_contact_present`,
    `wing_sector_damage_route_present`,
    `contact_target_role_is_non_chain_base_phalanx_edge_or_structurally_burdened`,
    `weak_pawn_or_asymmetry_only_not_counted`, and
    `stale_or_adjacent_runtime_evidence_not_counted_before_live_admission`
  - `S14`: base-contact lever/contact source, exact non-center chain-base
    target role, same-anchor base-contact continuation with a further defender
    chain link, fixed-chain / structural-shell-only non-counting, and stale/adjacent runtime evidence
    non-counting. The executable scaffold token set is
    `base_contact_lever_and_break_contact_present`,
    `contact_target_role_is_non_center_chain_base`,
    `base_contact_continuation_same_anchor_present`,
    `fixed_chain_or_structural_damage_shell_only_not_counted`, and
    `stale_or_adjacent_runtime_evidence_not_counted_before_live_admission`
- S13 now has a live runtime-admission slice:
  - row-specific burden names:
    `wing_damage_route_requires_same_sector_asymmetry_and_same_anchor_contact_source`,
    `wing_damage_route_excludes_center_sector_targets`,
    `phalanx_edge_route_requires_recomputed_phalanx_edge_target`,
    `phalanx_edge_route_excludes_chain_base_targets`,
    `burdened_target_route_requires_recomputed_structurally_burdened_target`,
    `projection_evidence_mirrors_owner_source_target_sector_and_damage_route`,
    and
    `asymmetry_weak_pawn_adjacent_rival_or_optional_strengthening_is_non_admitting`
  - live projection evidence kind:
    `wing_damage_route_certified`
  - helper law names:
    `same_sector_asymmetry_plus_contact_source_law`,
    `wing_damage_target_role_recomputed_from_exact_board_law`,
    `phalanx_edge_target_excludes_chain_base_law`,
    `center_sector_damage_is_not_wing_damage_law`,
    `same_task_projection_evidence_must_mirror_s13_source_target_sector_and_route_law`,
    and `asymmetry_or_preexisting_weak_pawn_is_not_damage_law`
  - lower-carrier ownership remains
    `Witness:sector_asymmetry_state`,
    `Witness:available_lever_trigger(same_wing_sector)`,
    `Witness:pawn_push_break_contact_source(same_anchor_stronger_wing_mass_target)`,
    `ProjectionValidation:wing_sector_non_chain_base_phalanx_edge_target|structurally_burdened_target`,
    and `SupportOnly:weak_pawn_target_state|file_lane_state`
- runtime boundary remains fail-closed for adjacent rows:
  `S16` admits only through its explicit same-enemy-passer start-ready branch,
  `S14` admits only through its explicit start-ready branch, and `S15` admits only through same-candidate S13/S14 creation
  evidence rather than through passer wording or shell support.

S14 now has a live runtime-admission freeze. The row-specific burden names are
`chain_base_route_requires_same_anchor_lever_and_contact_source`,
`chain_base_route_requires_recomputed_non_center_chain_base_target`,
`base_contact_continuation_requires_same_source_target_pair`,
`projection_evidence_mirrors_owner_source_target_and_chain_base_route`, and
`fixed_chain_structural_shell_adjacent_rival_or_optional_strengthening_is_non_admitting`.
The live projection evidence kind is
`chain_base_contact_route_certified`; it is the S14 entry in
`StrategyProjectionScopeContract.requiredEvidenceKindsByBand`. Helper/law names are
`same_anchor_chain_base_contact_law`,
`non_center_chain_base_role_recomputed_from_exact_board_law`,
`base_contact_must_bind_same_source_and_target_law`,
`same_task_projection_evidence_must_mirror_s14_source_target_and_route_law`,
and `fixed_chain_or_structural_damage_shell_is_not_truth_owner_law`. Lower
ownership stays with `Witness:available_lever_trigger(base_contact)`,
`Witness:pawn_push_break_contact_source(same_anchor_chain_base_target)`,
`ProjectionValidation:non_center_chain_base_target_recomputed|base_contact_continuation`,
and `SupportOnly:weak_pawn_target_state|structural_damage_shell|fixed_chain_context`.
This freeze is narrow by design: non-center chain-base contact support may be
used as a projection carrier only when `StrategyProjectionAdmission` recomputes
the same exact carrier and the projection evidence mirrors the contact source,
target, route token, and exact chain-base forward support squares. Fixed-chain
shells, weak-pawn support, optional strengthening, adjacent S05/S11/S13/S15
rivals, stale evidence, Object / Delta / Certification families, and
renderer/synthesis consequences remain non-admitting.

S11 now has a live runtime-admission slice. This closes the pre-live blocker by
adding `S11` to `StrategyProjectionScopeContract.startReadyBandIds`, adding the
S11 evidence kind to
`StrategyProjectionScopeContract.requiredEvidenceKindsByBand`, and revalidating
the lower carrier from the current root in `StrategyProjectionAdmission`:

- row-specific burden names:
  - `target_pressure_route_requires_same_square_weak_pawn_target_and_owner_pressure`
  - `fixation_route_requires_current_fixed_pawn_persistence_on_same_target`
  - `repeated_pressure_route_requires_two_owner_attackers_on_same_target`
  - `projection_evidence_mirrors_weak_pawn_target_pressure_and_persistence`
  - `weak_pawn_only_target_swap_adjacent_rival_or_optional_strengthening_is_non_admitting`
- live projection evidence-kind name:
  - `weak_pawn_target_pressure_persistence_certified`
- helper and validation law names:
  - `fixed_pawn_persistence_is_current_board_carrier_not_certification_truth_law`
  - `same_task_projection_evidence_must_mirror_s11_target_pressure_and_persistence_law`
  - `pressure_without_fixed_persistence_not_counted`
- lower-carrier ownership:
  `weak_pawn_target_state` remains the current-board witness carrier;
  same-target pressure and persistence remain projection-validation carriers;
  `MobilityComparison` and `InitiativeWindow` are support-only and never become
  S11 truth owners; S11 has no support seed, Object, Delta, or Certification
  truth-owner carrier at this live boundary.

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

The wave-3 JSONL rows are now authored and countable. This promotes `S05`,
`S07`, `S08`, and `S21` to narrow live `StrategyProjectionAdmission` branches.

- row-specific burden gates are the broad-ready gate table rows for `S05`,
  `S07`, `S08`, and `S21`
- shared helper law stays exact-board and fail-closed:
  - `S05`: the same-anchor contact source and target must both be center-file
    squares; `center_pawn_target` and `central_axis_continuation` are route
    tokens certified by projection evidence, while `CentralContactFront`,
    `InitiativeWindow`, and file support remain non-truth-owner support
  - `S07`: `DevelopmentComparison` and certified `InitiativeWindow` must both
    be present for the same owner; `OpeningDevelopmentRegime` is support-only.
    S07 live runtime admission uses board-anchored
    `initiative_conversion_route_certified` evidence, with the route task
    limited to `development_led_window` or `move_right_window`. That evidence
    kind is listed in
    `StrategyProjectionScopeContract.requiredEvidenceKindsByBand`.
  - `S08`: denial must be frozen against one exact rival release source and
    must fail closed when the same board carries an owner-side S21 survival source;
    `InitiativeWindow` by itself is support for the denial route, not the
    projection truth owner. S08 live runtime admission uses
    `counterplay_denial_route_certified`, and that evidence must bind the same
    rival source, target, and denial route.
  - `S21`: exact owner `pawn_push_break_contact_source` and same-board certified
    `InitiativeWindow` must travel together; source presence alone is not
    counterplay survival, and certified `InitiativeWindow` alone is not S21
    proof
- lower-carrier ownership is frozen as witness / object-support /
  certification / projection-validation ownership, not as projection-owned
  lower truth
- exact validation scaffold is now complete for the authored rows:
  positive route buckets count only `exact` + `admitted` rows, same-cluster
  buckets count only rejected near-miss / contrastive /
  `comparative_false_rival` rows, and shortcut buckets count only rejected
  nasty-negative / negative rows
- runtime boundary remains fail-closed:
  `S05` admits only through its explicit center-release branch; `S07` admits
  only through exact initiative-conversion route evidence plus lower
  certifications; `S08` admits only through exact rival-source denial evidence
  plus same-board InitiativeWindow support; `S21` admits only through its
  explicit counterplay-survival branch
- S07 live runtime admission keeps `DevelopmentComparison` and certified
  `InitiativeWindow` as lower Certification truth owners. S07 projection
  cannot admit from those lower families alone, from initiative wording, from
  development lead alone, from owner target-state leakage, or from S08 exact
  rival-source denial.
- S21 live runtime admission uses
  `counterplay_survival_route_certified`, listed in
  `StrategyProjectionScopeContract.requiredEvidenceKindsByBand`, and the
  admitted S21 coverage rows carry exact runtime evidence claims
- `S05` additionally has a closed live-admission freeze:
  row-specific burden
  `center_release_route_requires_same_anchor_lever_and_contact_source`,
  `center_release_route_requires_recomputed_center_pawn_target`,
  `central_axis_continuation_requires_same_source_target_pair`,
  `projection_evidence_mirrors_owner_source_target_and_center_release_route`,
  and
  `open_center_wording_center_shell_support_object_optional_strengthening_or_adjacent_rival_is_non_admitting`;
  the frozen live evidence kind is `center_release_route_certified` and is
  listed in `StrategyProjectionScopeContract.requiredEvidenceKindsByBand`

`ProjectionExpectationCorpus.requiredCoveragePairsFor`,
`coveragePairsByBand`, `missingRequiredCoveragePairsByBand`, and
`bandsWithCompleteBroadReadyCoverage` now include these four bands in the
counted broad-ready gate. The authored rows keep lower-carrier ownership in the
lower witness/object/certification layer; only the exact S05 evidence-backed
rows create live projection admission.

### Conversion / Simplification / Holding Coverage Rows

The current coverage freeze for `S17`, `S18`, `S19`, `S22`, and `S24` is
executable in
`modules/commentary/src/main/scala/lila/commentary/projection/StrategyProjectionCoverageContract.scala`
and verified by `StrategyProjectionCoverageContractTest` plus
`ProjectionExpectationCorpusTest`.

The wave-2 JSONL rows are now authored and countable. The freeze closes these
blocker causes while separating exact live admission rows from coverage-only
rows at this boundary:

- row-specific burden gates are the broad-ready gate table rows for `S17`,
  `S18`, `S19`, `S22`, and `S24`
- shared helper law stays same-anchor / same-piece / same-target:
  - `S17`: liability anchor, relief seed, and `liability_relief_certified`
    must name the same piece
  - `S18`: `bishop_pair_state` is substrate only; conversion certification
    must supply the burden, with the structure route owned by
    `MobilityComparison` rather than by projection prose. The live admission
    freeze names no support seed, object, or delta carrier for S18; its lower
    carrier ownership is `bishop_pair_state` on the same owner current board
    plus lower certification truth from `InitiativeWindow`,
    `MobilityComparison`, or `MaterialHarvest`; `WinningEndgame` remains a
    future support-only carrier because it cannot coexist with same-board
    bishops under current lower law
  - `S19`: `TradeInvariant` is the delta task carrier; countable material or
    hold certification must stay attached to that same task endpoint
  - `S22`: `FortressHoldingShell` is same-holder object support;
    certified hold verdicts remain `FortressDrawCertification` or
    `PerpetualCheckHolding`; `SupportOnly` / `Deferred` hold evidence remains
    fail-closed
  - `S24`: dependency, convergence, forcing realization, and conversion
    certification must name the same target piece-square
- lower-carrier ownership is frozen as support / object / delta /
  certification ownership, not as projection-owned truth
- exact validation scaffolds must use exact FEN rows, before/after FEN plus
  played move where a delta is involved, and engine/probe evidence only where
  certification burdens require best-defense, route-survival, hold, or tactical
  release evidence; `S22` additionally freezes a live `TradeInvariant` S19
  rival row, a checking-sequence shortcut negative, and a deferred perpetual
  hold negative
- runtime boundary remains fail-closed outside explicit live rows:
  coverage-only rows remain outside `StrategyProjectionAdmission`
- `S05`, `S06`, `S07`, `S08`, `S11`, `S13`, `S14`, `S15`, `S16`, `S17`, `S18`, `S19`, `S21`, `S22`, `S23`, `S24`, and `S25` use the start-ready admission
  scaffold
- `S18` has narrow live runtime admission for exact current-board minor
  conversion only:
  - `initiative_route_requires_bishop_pair_state_and_same_board_initiative_window`
  - `structure_route_requires_bishop_pair_state_and_same_board_mobility_comparison`
  - `material_route_requires_bishop_pair_member_material_harvest`
  - `projection_evidence_mirrors_conversion_family_targets_and_bishop_pair`
  - `relation_only_optional_strengthening_or_adjacent_rival_is_non_admitting`
  - live projection evidence-kind names:
    `bishop_pair_initiative_conversion_certified`,
    `bishop_pair_structure_conversion_certified`, and
    `bishop_pair_material_conversion_certified`
  - shared helper law names:
    `same_current_board_bishop_pair_conversion_law`,
    `s18_projection_evidence_same_task_same_target_law`,
    `conversion_certification_is_lower_truth_owner_law`,
    `minor_edge_relation_without_conversion_is_non_admitting_law`, and
    `weak_square_liability_or_mobility_rival_is_not_conversion_law`
  - exact scaffold names:
    `same_current_board_conversion_certification_present`,
    `active_bishop_pair_member_present`,
    `projection_evidence_mirrors_conversion_family_targets_and_bishop_pair`,
    `material_conversion_bishop_pair_member_capture_present`,
    `initiative_or_structure_conversion_support_targets_present`, and
    `bishop_pair_minor_edge_or_optional_strengthening_only_not_counted`
- `S19` now has live delta-backed admission with the frozen burden:
  - `material_route_requires_canonical_trade_invariant_and_same_move_material_harvest`
  - `hold_route_requires_canonical_trade_invariant_and_post_trade_fortress_certification`
  - `result_only_material_only_or_adjacent_rival_is_non_admitting`
  - the live projection evidence-kind names are
    `trade_invariant_material_simplification_certified` and
    `trade_invariant_hold_simplification_certified`; they exactly match
    `StrategyProjectionScopeContract.requiredEvidenceKindsByBand` for S19
- adjacent `S22` and `S24` live status is independent of S19: their
  `requiredEvidenceKindsByBand` entries remain `fortress_hold_certified` /
  `perpetual_hold_certified` and `same_target_forcing_realization` /
  `same_target_conversion_certified`, and S19 evidence kinds must stay disjoint
- `S22` evidence-kind names are frozen as `fortress_hold_certified` and
  `perpetual_hold_certified` in `StrategyProjectionCoverageContract`; they must
  exactly match `StrategyProjectionScopeContract.requiredEvidenceKindsByBand`
  for live `StrategyProjectionAdmission`, while lower hold certification still
  comes from a same-root `CertificationEvidenceBundle`
- `S19` coverage rows carry exact `fenBefore` / `playedMove` / `fenAfter`
  companions for `TradeInvariant`; material and hold certification remain
  lower-layer ownership on an exact task endpoint, `MaterialHarvest` must bind
  `capture_from` / `capture_to` to that exact played move on the move board, and
  the hold route must certify the post-trade endpoint; neither can be inferred
  from trade wording; result-only simplification is deliberately non-counting
  here

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

The previous broad blocker set `S06` / `S10` / `S12` / `S15` is closed at
the gate-decision level, and `S15` / `S16` are now closed at wave-6
coverage-complete level; both have narrow live admission branches:

- `S06` now has frozen restriction-route and same-cluster near-miss gates, with
  initial matching coverage rows present.
- `S10` is explicitly knight-only for the current branch's broad scope.
- `S12` now has frozen local access-route and same-cluster near-miss gates, with
  initial matching coverage rows present.
- `S15` now has a live route-completeness law: per-position route admission
  is disjunctive, while broad-ready coverage is conjunctive over
  `s13_wing_damage` and `s14_chain_base`, with the candidate passer required
  to be the same pawn as the S13/S14 contact source. The live
  `passer_creation_route_certified` evidence kind is frozen in
  `StrategyProjectionScopeContract.requiredEvidenceKindsByBand`, and
  false-rival / shortcut rows cover `S13`, `S14`, `S16`,
  candidate-only, create-passer-shell-only, existing-passer-only, and
  split-anchor route negatives.
- `S16` now has a live suppression law: enemy passer presence must be tied to
  `blockade_hold`, `restriction_hold`, or `non_losing_race`, with certified
  race or hold proof and `passer_suppression_route_certified` evidence that
  mirrors the same enemy passer, route, route-specific blocker when applicable,
  and certification family. For `restriction_hold`, the blocker must also be
  present in the owner `short_run_slider_gate_restriction` gate-square payload.

Those closures do not provide broad deployment by themselves. `S15` and `S16`
now have separate matching exact-board corpus rows, and
`ProjectionExpectationCorpus` includes their required pairs in
`broadReadyCoverageGateBands` with empty
`missingRequiredCoveragePairsByBand` for both bands. `S15` is live only through
its explicit same-candidate branch; `S16` admits only through its explicit
same-enemy-passer suppression branch.

`S09`, `S20`, `S23`, and `S25` are in the staged wave-1 executable broad-ready
gate-band set; `S17`, `S18`, `S19`, `S22`, and `S24` are in the staged wave-2
set; `S05`, `S07`, `S08`, and `S21` are in the staged wave-3 set, with all four
promoted to narrow live admission; `S01`,
`S02`, `S03`, and `S04` are in the staged wave-4 set; `S11`, `S13`, and
`S14` are in the staged wave-5 broad-ready set, with all three promoted
to narrow live admission branches; and `S15` and `S16` are in the staged wave-6
set, with both promoted to narrow live admission branches. `S25` remains limited to
`rank_access_route = cross_wing_rank_switch`; wider rank-access meanings still
require later exact-board coverage. The staged completed sets have matching
JSONL coverage rows and green validation. No staged band is broad-deployed
because of corpus state alone; live runtime status remains only the explicit
`StrategyProjectionScopeContract.startReadyBandIds` set: `S05`, `S06`, `S07`, `S08`, `S11`, `S13`,
`S14`, `S15`, `S16`, `S17`, `S18`, `S19`, `S21`, `S22`, `S23`, `S24`, and `S25`.
