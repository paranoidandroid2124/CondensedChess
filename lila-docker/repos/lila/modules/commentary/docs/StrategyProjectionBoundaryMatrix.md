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
| `S03` | king attack whose decisive carrier is a king-facing diagonal entry complex | `AttackScaffold`, `certified_king_safety_edge`, `comparative_king_fragility`, and `king_file_diagonal_entry_axis` hosted on `diagonal_lane_only` | `S02`, `S12` | `bishop_pair_state` alone; non-king diagonal pressure alone |
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
| `S17` | improvement or exchange of the worst minor piece to repair a concrete liability | a narrow anchored worst-piece support seed, plus a certified improvement or exchange route that relieves that exact liability; broad `minor_piece_liability` language is support only | `S18`, `S20` | `bad piece` wording alone; generic improving move wording alone |
| `S18` | conversion of an already favorable bishop-pair or minor-piece relation | `bishop_pair_state`, plus a certified conversion path into initiative, structure, material, or endgame edge; broad `favorable_minor_piece_relation` wording is support only | `S17`, `S12`, `S20` | `bishop_pair_state` alone |
| `S19` | simplification because the resulting trade tree stays favorable | `TradeInvariant` plus a favorable downstream certification such as `winning_endgame`, `FortressHoldingShell`, or `material_harvest` support | `S22`, `S24` | trade wording alone |
| `S20` | comparative mobility collapse or domination of the rival army | `mobility_comparison` plus concrete restriction support such as `short_run_slider_gate_restriction` or defender-starvation geometry | `S06`, `S12`, `S17` | `mobility edge` wording alone |
| `S21` | surviving release resource in the center or on the far wing against the rival plan | exact `pawn_push_break_contact_source` plus a certified counterplay-survival gate; center or wing scope is contextual support only | `S01`, `S05`, `S08` | `open center` wording alone; asymmetry wording alone |
| `S22` | holding, neutralization, or fortress-style consolidation | `FortressHoldingShell` plus a certified hold verdict from the `perpetual/fortress` family; `TradeInvariant` is optional support only | `S19`, `S23` | `draw/hold` wording alone |
| `S23` | king activity, opposition, or penetration as the main endgame plan | currently blocked for live admission until the `king_entry_square_seed` / `king_access_route_seed` / `king_opposition_contact_seed` family is frozen; any future admission must also carry a certified entry or conversion gate | `S09`, `S16`, `S22` | endgame wording alone; `whole-board` shell alone |
| `S24` | tactical realization of a target that was already strategically prepared | currently blocked for live admission until the `target_resource_dependency_seed` / `target_attack_convergence_seed` family is frozen; any future admission must also carry an exact tactic or forcing family plus conversion certification | `S04`, `S19` | `pin` / `fork` / `skewer` / `overload` alone; tactical wording alone |

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

This matrix freezes semantic boundaries only.

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
