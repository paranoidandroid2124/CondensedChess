# Strategy Projection Boundary Matrix

This document freezes the semantic boundary of the `25` strategy projection
bands and the validation scaffold used to test those boundaries.

It does **not** make `Projection` a truth-owning layer.

Production public admission is narrower than this matrix:

`StrategyRuntimeKProducer -> CertifiedTruth(K) -> StrategyGeometryEngine -> StrategyProjectionAdmissionProducer -> EvidenceClaimProducer`

The old raw `StrategyProjectionAdmission` matcher is test-only validation
scaffold. It produces `LegacyValidationScaffold` admissions, which cannot lower
to public claims.

Terminology authority is
[CommentaryCoreSSOT.md](/C:/Codes/CondensedChess/lila-docker/repos/lila/modules/commentary/docs/CommentaryCoreSSOT.md):
`semantic boundary` is meaning/rival separation, `validation scaffold` is
test-only exact-board coverage, and `production public admission` is
descriptor/K-only with `DescriptorCertifiedRuntime` authority.
`StrategyProjectionAdmissionProducer` consumes only `runtimeKCandidates` and
the resulting producer-minted `RuntimeK` tokens; caller-supplied pre-minted K
and validation-scaffold admissions are not production public input.

## Global Rules

- A strategy band must be supported by exact lower carriers: `Witness`,
  support seed, `Object`, `Delta`, or `Certification` as specified by the row.
- Validation scaffold rows prove exact-board admission and false-rival behavior;
  they are not production public admission.
- Production migration requires a descriptor-owned K-backed admitted region.
- A band may overlap a rival on the same board, but it must not collapse into a
  wording variant of that rival.
- Legacy inventory labels, raw tactics, renderer wording, planner lanes, and
  LLM phrasing never supply missing strategy semantics.
- For Sxx projection, `start-ready`, `live admission`, `runtime admission`, and
  `live runtime` are not production-public terms.

## Boundary And Migration Table

| Band | Validation scaffold coverage | Semantic boundary | Production migration status |
| --- | --- | --- | --- |
| `S01` | `storm_route`: `same_wing_contact`, `attack_edge_same_king`; `same_cluster_near_miss`: `vs_s05`, `vs_s21`; `shortcut_negative`: `castling_side_only`, `wing_shell_only`, `optional_strengthening_only`, `support_only_attack_certification`, `support_only_lane_evidence` | owner-side pawn storm against the enemy king wing, not surviving release resource | test-only scaffold; production descriptor/K region pending; scaffold evidence `king_wing_storm_route_certified` |
| `S02` | `attack_concentration_route`: `direct_piece_concentration`, `lane_strengthened_concentration`; `same_cluster_near_miss`: `vs_s03`, `vs_s04`, `vs_s09`; `shortcut_negative`: `king_attack_label_only`, `battery_or_lift_only` | direct piece concentration on the king ring | test-only scaffold; production descriptor/K region pending; scaffold evidence `king_ring_concentration_route_certified` |
| `S03` | `diagonal_attack_route`: `king_facing_diagonal_entry`, `fragility_linked_diagonal`; `same_cluster_near_miss`: `vs_s02`, `vs_s12`; `shortcut_negative`: `bishop_pair_only`, `non_king_diagonal_pressure`, `attack_scaffold_only`, `certification_support_only`, `diagonal_certification_without_scaffold`, `scaffold_certification_without_diagonal` | king attack carried by a king-facing diagonal complex | test-only scaffold; production descriptor/K region pending; scaffold evidence `diagonal_king_attack_route_certified` |
| `S04` | `shelter_breach_route`: `shell_payload_breach`, `support_break_breach`; `same_cluster_near_miss`: `vs_s01`, `vs_s02`, `vs_s03`, `vs_s24`; `shortcut_negative`: `king_shelter_wording_only`, `generic_attack_pressure`, `optional_strengthening_only`, `support_only_shell_or_certification`, `forged_wrong_shell_evidence`, `forged_wrong_route_evidence` | destruction of king shelter, not generic attacking presence | test-only scaffold; production descriptor/K region pending; scaffold evidence `king_shelter_breach_route_certified` |
| `S05` | `center_release_route`: `center_pawn_target`, `central_axis_continuation`; `same_cluster_near_miss`: `vs_s06`, `vs_s14`, `vs_s21`; `shortcut_negative`: `open_center_wording_only`, `center_shell_only` | central opening or rupture as the strategic release | test-only scaffold; production descriptor/K region pending; scaffold evidence `center_release_route_certified`; requires center-file source, center-file target, and center-release route |
| `S06` | `restriction_route`: `outpost_anchor`, `non_outpost_space_bind`; `same_cluster_near_miss`: `vs_s05`, `vs_s20`; `shortcut_negative`: `space_wording_only`, `mobility_comparison_only` | structural space bind restricting rival mobility | test-only scaffold; production descriptor/K region pending; scaffold evidence `space_bind_restriction_route_certified`; support-only lower certification `SpaceBindRestrictionCertification`; keeps exact S05 center-release and exact S20 domination false-rival rows separate |
| `S07` | `initiative_conversion_route`: `development_led_window`, `move_right_window`; `same_cluster_near_miss`: `vs_s08`; `shortcut_negative`: `development_lead_only`, `initiative_wording_only` | development edge converted into initiative | test-only scaffold; production descriptor/K region pending; scaffold evidence `initiative_conversion_route_certified`; `OpeningDevelopmentRegime` is support-only |
| `S08` | `denial_route`: `rival_break_source_suppressed`, `rival_counterplay_source_suppressed`; `same_cluster_near_miss`: `vs_s07`, `vs_s20`, `vs_s21`; `shortcut_negative`: `no_counterplay_wording_only`, `initiative_window_only` | denial of exact rival release or counterplay resources | test-only scaffold; production descriptor/K region pending; scaffold evidence `counterplay_denial_route_certified`; same rival source, target, and denial route with wrong-source/wrong-target/wrong-route/stale-evidence rejection; `InitiativeWindow` remains lower Certification support, not projection truth |
| `S09` | `penetration_route`: `open_file_entry`, `semi_open_file_entry`, `same_file_penetration`; `same_cluster_near_miss`: `vs_s02`, `vs_s23`, `vs_s25`; `shortcut_negative`: `file_substrate_only`, `rook_on_file_only`, `support_only_attack_shell`, `certification_support_only` | open or semi-open file penetration | test-only scaffold; production descriptor/K region pending; scaffold evidence `file_penetration_route_certified` |
| `S10` | `occupancy_scope`: `knight_only`; `durability_route`: `same_anchor_eviction_denial`; `same_cluster_near_miss`: `vs_s12`; `shortcut_negative`: `good_piece_wording_only`, `occupancy_without_durability`, `non_knight_occupancy_without_outpost_role` | durable knight occupation of a certified outpost | test-only scaffold; production descriptor/K region pending; scaffold evidence `outpost_occupation_route_certified` |
| `S11` | `target_pressure_route`: `same_target_fixation`, `same_target_repeated_pressure`; `same_cluster_near_miss`: `vs_s13`, `vs_s14`; `shortcut_negative`: `weak_pawn_target_only`, `target_swap_by_prose` | persistent fixation and pressure on one weak pawn target | test-only scaffold; production descriptor/K region pending; scaffold evidence `weak_pawn_target_pressure_persistence_certified` |
| `S12` | `access_route`: `weak_square_route`, `diagonal_lane_route`; `same_cluster_near_miss`: `vs_s03`, `vs_s10`, `vs_s20`; `shortcut_negative`: `weak_square_wording_only`, `diagonal_wording_only` | weak-square or diagonal access domination | test-only scaffold; production descriptor/K region pending; scaffold evidence `local_access_superiority_route_certified` |
| `S13` | `wing_damage_route`: `phalanx_edge_target`, `structurally_burdened_target`; `same_cluster_near_miss`: `vs_s11`, `vs_s14`, `vs_s15`; `shortcut_negative`: `sector_asymmetry_only`, `preexisting_weak_pawn_only` | wing-side asymmetry used to damage a stronger pawn mass | test-only scaffold; production descriptor/K region pending; scaffold evidence `wing_damage_route_certified` |
| `S14` | `chain_base_route`: `chain_base_target`, `base_contact_continuation`; `same_cluster_near_miss`: `vs_s05`, `vs_s11`, `vs_s13`, `vs_s15`; `shortcut_negative`: `fixed_chain_only`, `generic_structural_damage` | attack on the base anchor of a pawn chain | test-only scaffold; production descriptor/K region pending; scaffold evidence `chain_base_contact_route_certified` |
| `S15` | `creation_route`: `s13_wing_damage`, `s14_chain_base`; `same_cluster_near_miss`: `vs_s13`, `vs_s14`, `vs_s16`; `shortcut_negative`: `candidate_passer_only`, `create_passer_shell_only`, `existing_passer_entity_only`, `split_anchor_creation_route` | creation of a new passed-pawn or passer complex | test-only scaffold; production descriptor/K region pending; scaffold evidence `passer_creation_route_certified` |
| `S16` | `suppression_route`: `blockade_hold`, `restriction_hold`, `non_losing_race`; `same_cluster_near_miss`: `vs_s15`, `vs_s22`, `vs_s23`; `shortcut_negative`: `enemy_passer_presence_only`, `blocker_picture_only` | enemy passer suppression through blockade, restriction, or race denial | test-only scaffold; production descriptor/K region pending; scaffold evidence `passer_suppression_route_certified` |
| `S17` | `liability_relief_route`: `repair_route`, `exchange_relief`; `same_cluster_near_miss`: `vs_s18`, `vs_s20`; `shortcut_negative`: `bad_piece_wording_only`, `generic_improving_move` | worst-piece repair or exchange relief for one concrete liability | test-only scaffold; production descriptor/K region pending; scaffold evidence `liability_relief_certified` |
| `S18` | `minor_edge_conversion_route`: `bishop_pair_to_initiative`, `bishop_pair_to_structure`, `bishop_pair_to_material`; `same_cluster_near_miss`: `vs_s12`, `vs_s17`, `vs_s20`; `shortcut_negative`: `bishop_pair_state_only`, `minor_edge_label_only` | conversion of a bishop-pair or minor-piece edge | test-only scaffold; production descriptor/K region pending; scaffold evidence `bishop_pair_initiative_conversion_certified`, `bishop_pair_structure_conversion_certified`, `bishop_pair_material_conversion_certified` |
| `S19` | `simplification_route`: `trade_invariant_to_hold`, `trade_invariant_to_material`; `same_cluster_near_miss`: `vs_s22`, `vs_s24`; `shortcut_negative`: `trade_wording_only`, `material_reduction_only` | favorable simplification tied to the exact trade task | test-only scaffold; production descriptor/K region pending; scaffold evidence `trade_invariant_material_simplification_certified`, `trade_invariant_hold_simplification_certified` |
| `S20` | `domination_route`: `mobility_plus_restriction`, `defender_starvation`; `same_cluster_near_miss`: `vs_s06`, `vs_s12`, `vs_s17`; `shortcut_negative`: `mobility_edge_only`, `restriction_without_comparison` | comparative mobility collapse or domination | test-only scaffold; production descriptor/K region pending; scaffold evidence `mobility_domination_route_certified` |
| `S21` | `counterplay_survival_route`: `center_source_survives`, `far_wing_source_survives`; `same_cluster_near_miss`: `vs_s01`, `vs_s05`, `vs_s08`; `shortcut_negative`: `break_source_only`, `deferred_initiative_only` | surviving release resource against the rival plan | test-only scaffold; production descriptor/K region pending; scaffold evidence `counterplay_survival_route_certified`; counterplay survival route is validation-ready in scaffold; certified `InitiativeWindow` alone is not S21 proof |
| `S22` | `hold_route`: `fortress_draw_hold`, `perpetual_hold`; `same_cluster_near_miss`: `vs_s19`, `vs_s23`; `shortcut_negative`: `draw_wording_only`, `fortress_shell_only`, `checking_sequence_wording`, `perpetual_deferred` | holding, neutralization, or fortress-style consolidation | test-only scaffold; production descriptor/K region pending; scaffold evidence `fortress_hold_certified`, `perpetual_hold_certified` |
| `S23` | `king_activity_route`: `same_entry_route`, `same_contact_opposition`; `same_cluster_near_miss`: `vs_s09`, `vs_s16`, `vs_s22`; `shortcut_negative`: `centralization_only`, `king_proximity_only` | king activity, opposition, or penetration as the endgame plan | production thin slice exists: `A_S23_endgame_entry_or_opposition` and `A_S23_line_access_activity`; default disabled; public lowering requires `DescriptorCertifiedRuntime`; scaffold evidence `king_entry_conversion_certified`, `king_opposition_certified` |
| `S24` | `prepared_target_route`: `same_target_forcing_conversion`; `same_cluster_near_miss`: `vs_s04`, `vs_s19`; `shortcut_negative`: `raw_tactic_only`, `trade_or_result_only` | tactical realization of a strategically prepared target | test-only scaffold; production descriptor/K region pending; scaffold evidence `same_target_forcing_realization`, `same_target_conversion_certified` |
| `S25` | `rank_access_route`: `cross_wing_rank_switch`; `same_cluster_near_miss`: `vs_s02`, `vs_s03`, `vs_s04`, `vs_s09`, `vs_s12`, `vs_s23`, `vs_s24`; `shortcut_negative`: `piece_on_rank_only`, `generic_rook_lift`, `file_substrate_only`, `back_rank_defense_only`, `king_activity_only`, `tactic_only` | legal same-rank cross-wing access, not file penetration or rook-lift narration | test-only scaffold; production descriptor/K region pending; scaffold evidence `rank_access_consequence_certified` |

## Coverage Contract

The table above is the compact coverage-completion contract. A band is
coverage-complete only when `projection-expectations.jsonl` has countable exact
rows for every listed `coverageAxis` / `coverageBucket` pair and validation is
green.

`StrategyProjectionCoverageContract.scala` owns the executable lists:

- `coverageGatesByBand`
- `lowerCarrierOwnersByBand`
- `helperLawsByBand`
- `exactValidationScaffoldByBand`
- `rowSpecificAdmissionBurdensByBand`
- `declaredProjectionEvidenceKindsByBand`

`StrategyProjectionScopeContract.startReadyBandIds` and the test-only
`StrategyProjectionAdmissionScaffold` still validate S01-S25 exact rows, but
that validation path is not production public admission.

`S01`, `S02`, `S03`, `S04`, `S05`, `S06`, `S07`, `S08`, `S09`, `S10`, `S11`,
`S12`, `S13`, `S14`, `S15`, `S16`, `S17`, `S18`, `S19`, `S20`, `S21`, `S22`,
`S23`, `S24`, and `S25` are the current validation-scaffold set.

Compact slash form:
`S01`/`S02`/`S03`/`S04`/`S05`/`S06`/`S07`/`S08`/`S09`/`S10`/`S11`/`S12`/`S13`/`S14`/`S15`/`S16`/`S17`/`S18`/`S19`/`S20`/`S21`/`S22`/`S23`/`S24`/`S25`.

Rows outside S01-S25 remain outside even the validation scaffold:
non-`S01`/`S02`/`S03`/`S04`/`S05`/`S06`/`S07`/`S08`/`S09`/`S10`/`S11`/`S12`/`S13`/`S14`/`S15`/`S16`/`S17`/`S18`/`S19`/`S20`/`S21`/`S22`/`S23`/`S24`/`S25` bands do not admit.

Coverage completion authorizes corpus validation only. It does not authorize
broad deployment, public production, or renderer wording.

## High-Risk Rival Clusters

- `S01` / `S05` / `S21`: storm, center release, and counterplay survival.
- `S02` / `S03` / `S04`: direct attack, diagonal attack, and shelter breach.
- `S11` / `S13` / `S14` / `S15` / `S16`: pawn target, wing damage, chain base,
  passer creation, and passer suppression.
- `S17` / `S18` / `S20`: piece repair, minor-edge conversion, and mobility
  domination.
- `S09` / `S23` / `S25`: file penetration, king activity, and rank access.
- `S19` / `S22` / `S23` / `S24`: simplification, holding, king activity, and
  prepared-target conversion.
