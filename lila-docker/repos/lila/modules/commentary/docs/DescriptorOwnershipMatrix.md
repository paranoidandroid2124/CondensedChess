# Descriptor Ownership Matrix

This document freezes the final primary owner-layer map for the `61`-descriptor
inventory that sits above the `2891`-dimensional root-state vector.

It is the canonical ownership matrix for this branch.

`upper-layer` is retired as a canonical row class.

It survives only as a historical umbrella phrase for older review notes.

## Macro Stair

The frozen macro staircase is:

`Root -> Witness -> Object -> Delta -> Certification -> Projection -> Renderer`

The separate side evidence channel is:

`Engine / Probe -> Delta and Certification only`

Layer rules:

| Layer | Owns truth? | Input | Output | Engine / probe allowed? |
| --- | --- | --- | --- | --- |
| `Root` | yes | board state + aux move state | exact-board atoms | no |
| `Witness` | yes | `phi(R)` only | typed witness or attached descriptor | no |
| `Object` | yes | witnesses | stable strategic object | no |
| `Delta` | yes | objects + before/after or scope relation | typed delta | yes, as side evidence only |
| `Certification` | yes | objects, deltas, side evidence | certified / support-only / deferred verdict | yes, as side evidence only |
| `Projection` | no | certified objects and deltas | strategy vocabulary bands | no new truth |
| `Renderer` | no | certified claim + projection | wording | no new truth |

Guardrails:

- `Projection` and `Renderer` do not own truth.
- `Engine / Probe` never participates in root extraction or witness admission.
- Counts below are by **primary owner layer per inventory row**, not by the
  number of linked homes.
- A split row such as `king safety edge` still counts as one `Certification`
  row even if it links multiple homes.

## Count Freeze

| Primary owner layer | Count |
| --- | ---: |
| `Witness / U-primary` | 18 |
| `Witness / U-attached` | 11 |
| `Object` | 7 |
| `Delta` | 2 |
| `Certification` | 10 |
| `Projection` | 13 |
| `Renderer` | 0 |
| `Total descriptor inventory` | 61 |

Derived historical rollup:

- former `upper-layer` umbrella = `Object 7 + Delta 2 + Certification 10 + Projection 13 = 32`

## Canonical Row Matrix

| Descriptor | Family | Primary owner layer | Canonical owner home | Linked homes / bands | Notes |
| --- | --- | --- | --- | --- | --- |
| `opening-tempo` | `U1` | `Object` | `OpeningDevelopmentRegime` | `phase_gate` release context only | object admission now uses `opening_development_window` with home-minor reserve plus home-rook reserve; `phase_gate` stays release-only |
| `middlegame-positional` | `U1` | `Object` | `DistributedContactRegime` | `contested_sectors`, phase continuity only | object admission now uses `distributed_contact_spread` over `sector_mask` + `front_connectivity`; `contested_sectors` stays payload-only |
| `transition-liquidation` | `U1` | `Delta` | `TradeCompressionCorridor` | `TradeInvariant`, `MoveLocal`, `TransitionBridge` | live delta contract; runtime registers corridor before invariant and the corridor row remains the canonical delta-side owner |
| `endgame-race` | `U1` | `Object` | `EndgameRaceScaffold` | `promotion_race`, `PasserComplex` | object admission now uses `dual_run_endgame_trigger` over advanced runners plus current forward-clear geometry; `promotion_race` stays certification-only |
| `king attack` | `U2` | `Object` | `AttackScaffold` | `S02`, `S03` | object admission now uses `attack_host_core` + `king_theater_link`; lone hole-only support is still insufficient and the label remains a projection alias, not a `U` witness |
| `material gain` | `U2` | `Witness / U-attached` | `material gain` shell | `material_harvest`, `winning_endgame` | code-frozen host-scoped objective shell only |
| `structural damage` | `U2` | `Witness / U-attached` | `structural damage` shell | lower structural-cause roots | code-frozen host-scoped objective shell only |
| `space gain` | `U2` | `Witness / U-attached` | `structural_space_claim` | `closed center`, `fixed chain` | runtime-closed attached contract |
| `promotion/passer` | `U2` | `Projection` | `S15 passer creation` | `PasserComplex`, `ConversionFunnel`, `promotion_race` | projection alias only; no single upper truth owner |
| `draw/hold` | `U2` | `Object` | `FortressHoldingShell` | `TradeInvariant`, `S22` | object admission now uses `fortress_entry_denial_shell`; same-file attacker rook/queen pressure and same-or-adjacent-file passer pressure still reject, `TradeInvariant` stays optional support, and `perpetual/fortress` stays certification |
| `center` | `U3` | `Witness / U-attached` | `center` shell | `S05`, `S21` | code-frozen neutral theater shell only |
| `kingside` | `U3` | `Witness / U-attached` | `kingside` shell | king-theater consumers above `U` | code-frozen neutral theater shell only |
| `queenside` | `U3` | `Witness / U-attached` | `queenside` shell | wing-play consumers above `U` | code-frozen neutral theater shell only |
| `open/semi-open file` | `U3` | `Witness / U-primary` | `file_lane_state` | `open_file_state`, `semi_open_file_state` | pure file substrate row |
| `diagonal/color complex` | `U3` | `Witness / U-primary` | `diagonal_lane_only` | `color_complex_only` deferred outside `U` | exact diagonal lane only |
| `whole-board` | `U3` | `Witness / U-attached` | `whole-board` shell | broad whole-board consumers above `U` | code-frozen neutral theater shell only |
| `king shelter` | `U4` | `Object` | `KingSafetyShell` | root `king_shelter_hole`, king-safety certification | object admission now uses `home_shelter_shell` with a home-wing king proxy (`c/g` on the home rank); exact shelter fact remains in `R` |
| `weak pawn` | `U4` | `Witness / U-primary` | `weak_pawn_target_state` | `fixed` / `backward` / `isolated` roots | beneficiary local liability |
| `passed pawn` | `U4` | `Witness / U-primary` | `passed_pawn_entity_state` | `candidate_passer` support stays below `U` | owner-side exact entity |
| `weak square/outpost` | `U4` | `Witness / U-primary` | `weak_outpost_square_state` | `outpost_square_state` priority variant | exact square-row family |
| `key file/rank` | `U4` | `Projection` | `S09 open or semi-open file penetration` | `file_lane_state`, `horizontal_rank_access` | mixed label survives as projection alias only |
| `bad piece` | `U4` | `Projection` | `S17 worst-piece improvement / bad-piece exchange` | `minor_piece_liability`, `favorable_minor_piece_relation` support | evaluative piece-quality label only |
| `loose/overloaded piece` | `U4` | `Witness / U-primary` | `loose_piece_target_state` | overload meaning stays outside this row | exact loose-target row |
| `development lag` | `U4` | `Certification` | `development_comparison` | `S07` | comparative development verdict |
| `counterplay source/break-point` | `U4` | `Witness / U-primary` | `pawn_push_break_contact_source` | break-point squares stay payload only | exact counterplay-source row |
| `open center` | `U5` | `Projection` | `S05 central break` | `S21` | projection-only central-openness vocabulary |
| `closed center` | `U5` | `Witness / U-attached` | `closed center` host shell | `structural_space_claim` | code-frozen host vocabulary only; no standalone exact witness survives |
| `fixed chain` | `U5` | `Witness / U-attached` | `fixed chain` host shell | `structural_space_claim` | code-frozen host vocabulary only; chain-topology witness does not survive in `U` |
| `majority/minority asymmetry` | `U5` | `Witness / U-primary` | `sector_asymmetry_state` | `S13` | count-only sector imbalance |
| `available lever` | `U5` | `Witness / U-primary` | `available_lever_trigger` | `single_push_lever_state`, `double_push_lever_state` | immediate pawn trigger only |
| `opposite-side castling/wing asymmetry` | `U5` | `Projection` | `S01 opposite-side castling pawn storm` | `S21` | projection alias only; no single upper truth owner |
| `rook on open file` | `U6` | `Witness / U-primary` | `rook_on_open_file_state` | `S09` as downstream projection only | exact configuration row |
| `bishop pair` | `U6` | `Witness / U-primary` | `bishop_pair_state` | `S18` downstream only | pure board configuration |
| `knight outpost` | `U6` | `Witness / U-primary` | `knight_on_outpost_square` | `S10` downstream only | owner-side occupancy row |
| `queen-bishop battery` | `U6` | `Projection` | `queen-bishop battery` projection alias | `qb_diagonal_alignment_seed` deferred, `S02`, `S03` | split projection consumers only; no single truth owner survives |
| `rook lift` | `U6` | `Projection` | `rook lift` projection alias | `RedeploymentRoute`, `AccessNetwork`, `AttackScaffold`, `S09` | split projection consumers only; no single truth owner survives |
| `defender shortage` | `U6` | `Witness / U-primary` | `duty_bound_defender` | shortage wording stays above `U` | exact defender-duty row |
| `domination net/restriction geometry` | `U6` | `Witness / U-primary` | `short_run_slider_gate_restriction` | domination wording is projection only | exact restriction row |
| `initiative` | `U7` | `Certification` | `InitiativeWindow` | `S07`, `S08` | persistence / denial verdict only |
| `development lead` | `U7` | `Certification` | `development_comparison` | `InitiativeWindow`, `S07` | comparative development verdict |
| `central tension` | `U7` | `Object` | `CentralContactFront` | `available_lever_trigger`, `open center`, `S05`, `S21` | object admission now uses `central_contact_front_state` over `central_sector_mask` + `front_connectivity` |
| `mobility edge` | `U7` | `Certification` | `mobility_comparison` | `S20` | comparative mobility verdict |
| `king safety edge` | `U7` | `Certification` | `comparative_king_fragility` + `certified_king_safety_edge` | `AttackScaffold`, `S02`, `S03` | split certification row by contract |
| `open line` | `U8` | `Witness / U-attached` | `open line` shell | `file_lane_state`, `rook_on_open_file_state`, `king_file_diagonal_entry_axis` | code-frozen host-scoped transformation shell only |
| `exchange defender` | `U8` | `Projection` | `S24 tactical conversion of a prepared target` | `DefenderDependencyNetwork`, `TradeInvariant` | split upper family; label is projection alias only |
| `simplify` | `U8` | `Delta` | `TradeInvariant` | `S19` | live delta contract; runtime registers corridor before invariant and `simplify` is the canonical delta-side owner for bounded favorable simplification |
| `create passer` | `U8` | `Witness / U-attached` | `create passer` shell | `candidate_passer`, `PasserComplex`, `S15` | code-frozen host-scoped transformation shell only |
| `improve worst piece` | `U8` | `Projection` | `S17 worst-piece improvement / bad-piece exchange` | `improve_worst_piece`, `minor_piece_liability` | broad improvement label remains projection alias |
| `pin` | `U9` | `Witness / U-primary` | `pin` | exact ray geometry only | admitted exact tactic |
| `fork` | `U9` | `Witness / U-primary` | `fork` | exact attack-fan geometry only | admitted exact tactic |
| `skewer` | `U9` | `Witness / U-primary` | `skewer` | exact ordered-exposure geometry only | admitted exact tactic |
| `overload` | `U9` | `Witness / U-primary` | `overload` | exact duty overload only | admitted exact tactic |
| `deflection/decoy` | `U9` | `Projection` | `S24 tactical conversion of a prepared target` | heuristic forcing / lure families remain distinct | projection alias only |
| `interference` | `U9` | `Projection` | `S24 tactical conversion of a prepared target` | heuristic interference families remain distinct | projection alias only |
| `clearance` | `U9` | `Projection` | `S24 tactical conversion of a prepared target` | heuristic clearance families remain distinct | projection alias only |
| `demolition/undermining` | `U9` | `Projection` | `S24 tactical conversion of a prepared target` | heuristic support-break families remain distinct | projection alias only |
| `mate net` | `U10` | `Certification` | `mate_net_certification` | `AttackScaffold`, king-safety certification | certified forcing-conversion verdict |
| `material harvest` | `U10` | `Certification` | `material_harvest` | `winning_endgame`, `S24` | realized conversion/result verdict |
| `winning endgame` | `U10` | `Certification` | `winning_endgame` | `TradeInvariant`, `S22` | certified conversion/result verdict |
| `perpetual/fortress` | `U10` | `Certification` | `fortress_draw_certification` + `perpetual_check_holding` | `FortressHoldingShell`, `S22` | split certification row; fortress side links holding object |
| `promotion race` | `U10` | `Certification` | `promotion_race` | `PasserComplex`, `ConversionFunnel`, `S15` | certified conversion race verdict |

## Certification Runtime Split Note

Current-worktree ownership remains:

- `Certification` inventory rows: `10`

First-live runtime-family plan is frozen to `11` families because:

- `development lag` and `development lead` share one comparative owner:
  `DevelopmentComparison`
- `king safety edge` splits into:
  - `ComparativeKingFragility`
  - `CertifiedKingSafetyEdge`
- `perpetual/fortress` splits into:
  - `FortressDrawCertification`
  - `PerpetualCheckHolding`

This is an ownership and runtime-shape note only.

It does not claim that a live certification runtime already exists in
`src/main`.
