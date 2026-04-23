# Decision Freeze Ledger

This document records the branch-level design decisions that are already
frozen for `codex/24-61-3016+35-structural-experiments`.

It is a decision ledger, not a replacement for the detailed SSOT documents.

Use it as the quickest authoritative summary of:

- what was actually decided
- what remains blocked
- which detailed document owns each frozen area

## Canonical Authority

The canonical rewrite authority on this branch lives under
`modules/commentary/docs/*`.

The removed `modules/llm/docs/*` material is not live semantic authority on
this branch.

Detailed owner:

- [CommentaryCoreSSOT.md](/C:/Codes/CondensedChess/lila-docker/repos/lila/modules/commentary/docs/CommentaryCoreSSOT.md)

## Frozen Macro Stair

The macro semantic staircase is frozen as:

`Root -> Witness -> Object -> Delta -> Certification -> Projection -> Renderer`

`Projection` and `Renderer` do not own truth.

Engine / probe is not a layer.

It is a separate side evidence channel consumed only in validation-side checks at:

- `Root` broad-validation confound filtering for selected engine-required rows
- `Delta`
- `Certification`

Detailed owner:

- [CommentaryCoreSSOT.md](/C:/Codes/CondensedChess/lila-docker/repos/lila/modules/commentary/docs/CommentaryCoreSSOT.md:28)
- [DescriptorOwnershipMatrix.md](/C:/Codes/CondensedChess/lila-docker/repos/lila/modules/commentary/docs/DescriptorOwnershipMatrix.md:12)

## Count Freeze

The low-layer count is frozen as:

- `R0-R3 root atoms = 2856`
- `Aux state atoms = 35`
- `root-state vector = 2891`
- descriptor inventory = `61`

The historical `3016` proposal is not the live root contract anymore.

Detailed owner:

- [CommentaryCoreSSOT.md](/C:/Codes/CondensedChess/lila-docker/repos/lila/modules/commentary/docs/CommentaryCoreSSOT.md:39)

## Root Broad-Confidence-Green Freeze

  Current root `broad-confidence-green` set:

  - `piece_on`
  - `controlled_by`
  - `pawn_controlled_by`
  - `contested`
  - `open_file`
  - `half_open_file`
  - `king_ring_square`
  - `weak_square`
  - `isolated_pawn`
  - `backward_pawn`
  - `doubled_file`
  - `candidate_passer`
  - `fixed_pawn`
  - `en_passant_state`
  - `lever_available`
  - `loose_piece`
  - `overloaded_piece`
  - `outpost_square`
  - `pinned_piece`
  - `passed_pawn`
  - `trapped_piece`
  - `xray_target`
  - `king_shelter_hole`
  - `side_to_move`
  - `castling_rights`

A future root `broad-confidence-green` claim must close all of:

- the schema-local breadth buckets and minimum floor frozen in
  `RootCoverageMatrix.scala` and mirrored in `root-coverage-matrix.md`
- the unchanged exact schema meaning frozen in
  [RootAtoms.md](/C:/Codes/CondensedChess/lila-docker/repos/lila/modules/commentary/docs/RootAtoms.md)
- the root engine-confound rule in
  [ValidationMethodology.md](/C:/Codes/CondensedChess/lila-docker/repos/lila/modules/commentary/docs/ValidationMethodology.md)

Current root status freeze:

  - `broad-confidence-green`:
    - `piece_on`
    - `controlled_by`
    - `pawn_controlled_by`
    - `contested`
    - `open_file`
    - `half_open_file`
    - `king_ring_square`
    - `weak_square`
    - `isolated_pawn`
    - `backward_pawn`
    - `doubled_file`
    - `candidate_passer`
    - `fixed_pawn`
    - `en_passant_state`
    - `lever_available`
    - `loose_piece`
    - `overloaded_piece`
    - `outpost_square`
    - `pinned_piece`
    - `passed_pawn`
    - `trapped_piece`
    - `xray_target`
    - `king_shelter_hole`
    - `side_to_move`
    - `castling_rights`
- no root schema remains `thin` under the current R broad-validation contract

Root engine use stays validation-side only.

It may reject a confounded row, but it may not create or widen a root atom.

`outpost_square`, `candidate_passer`, `trapped_piece`, and
`king_shelter_hole` are the engine-required root schemas currently promoted to
`broad-confidence-green`.

Their green claims remain tied to the selected root `r-*` probe buckets frozen
in `RootCoverageMatrix.scala`, not to exhaustive engine probing of every row for
the schema.

No engine-required root schema remains in `close` after the
`king_shelter_hole` promotion. Any future engine-required root expansion must
still close schema-local buckets and selected calm-probe rows before promotion.

## 61 Ownership Freeze

The `61` rows are not one flat witness class.

The primary owner-layer freeze is:

| Owner layer | Count |
| --- | ---: |
| `Witness / U-primary` | 18 |
| `Witness / U-attached` | 11 |
| `Object` | 7 |
| `Delta` | 2 |
| `Certification` | 10 |
| `Projection` | 13 |
| `Renderer` | 0 |
| `Total` | 61 |

`upper-layer` is retired as a canonical row class.

It survives only as a historical umbrella phrase.

Detailed owner:

- [Witnesses61.md](/C:/Codes/CondensedChess/lila-docker/repos/lila/modules/commentary/docs/Witnesses61.md:1)
- [DescriptorOwnershipMatrix.md](/C:/Codes/CondensedChess/lila-docker/repos/lila/modules/commentary/docs/DescriptorOwnershipMatrix.md:34)

Attached implementation freeze:

- active attached runtime ids are frozen to `structural_space_claim` only
- the remaining `10` attached rows are shell-only and code-frozen out of
  runtime registration

## Strategy Boundary Freeze

`S01-S25` is frozen as a projection-band vocabulary, not as a truth-owning
layer.

The projection freeze means:

- each `Sxx` must remain semantically separable from rival bands
- each `Sxx` must be constructible from lower certified carriers
- no strategy band may admit from renderer wording, planner choice, or legacy
  label alone

Detailed owner:

- [StrategyProjectionBoundaryMatrix.md](/C:/Codes/CondensedChess/lila-docker/repos/lila/modules/commentary/docs/StrategyProjectionBoundaryMatrix.md:1)
- [ValidationMethodology.md](/C:/Codes/CondensedChess/lila-docker/repos/lila/modules/commentary/docs/ValidationMethodology.md:423)

## Projection Start-Ready / Broad-Ready Freeze

This branch now distinguishes three projection states:

- semantic boundary frozen
- start-ready for projection work
- blocked for live admission

`start-ready` is a contract state only.

It does **not** authorize broad runtime behavior beyond the explicitly listed
narrow admission slices.

It authorizes only narrow first-slice projection work plus corpus authoring.

It does **not** yet prove broader deployment.

No coverage-complete-only projection band is broad-deployed on the current
branch. The staged wave-1 through wave-6 set `S01`, `S02`, `S03`,
`S04`, `S05`, `S06`, `S07`, `S08`, `S09`, `S10`, `S11`, `S12`, `S13`, `S14`,
`S15`, `S16`, `S17`, `S18`, `S19`, `S20`, `S21`, `S22`, `S23`, `S24`, and
`S25` is coverage-complete under the current JSONL/test broad-coverage gates.
`S01`, `S02`, `S03`, `S04`, `S05`, `S06`, `S07`, `S08`, `S09`, `S10`, `S11`, `S12`, `S13`, `S14`, `S15`, `S16`, `S17`, `S18`, `S19`, `S20`, `S21`, `S22`, `S23`, `S24`, and `S25` retain only their
current narrow live admission slices; `S04` is live only through its explicit
same-defender king-shelter breach runtime branch, `S02` is live only through
its explicit king-ring concentration runtime branch, and `S03` is live only
through its explicit diagonal king-attack runtime branch.

Any future `broad-ready` claim must close both:

- the `coverageAxis` / `coverageBucket` scaffold in
  [ValidationMethodology.md](/C:/Codes/CondensedChess/lila-docker/repos/lila/modules/commentary/docs/ValidationMethodology.md:444)
- the band-local breadth gates in
  [StrategyProjectionBoundaryMatrix.md](/C:/Codes/CondensedChess/lila-docker/repos/lila/modules/commentary/docs/StrategyProjectionBoundaryMatrix.md:214)

The band-local breadth gates are now frozen for `S01-S25`.
`StrategyProjectionCoverageContract` is the global executable owner for the
row-specific coverage gates, lower-carrier ownership, helper/admission laws,
and exact-validation scaffold for every `S01-S25` band.

This is a gate freeze plus executable corpus-coverage state only. It does not
promote any coverage-only band to live runtime projection or broad deployment.
The projection corpus validator now computes the missing required bucket pairs
for the staged wave-1 through wave-6 executable gate-band set (`S01`, `S02`,
`S03`, `S04`, `S05`, `S06`, `S07`, `S08`, `S09`, `S10`, `S11`, `S12`, `S13`,
`S14`, `S15`, `S16`, `S17`, `S18`, `S19`, `S20`, `S21`, `S22`, `S23`, `S24`,
and `S25`) from the JSONL corpus and keeps this state executable rather than
implied.
It also counts coverage only when the row burden matches the coverage axis, so
positive route buckets cannot be filled by rejected rows and negative buckets
cannot be filled by admitted positives.
Current JSONL coverage now completes `S01`, `S02`, `S03`, `S04`, `S05`, `S06`,
`S07`, `S08`, `S09`, `S10`, `S11`, `S12`, `S13`, `S14`, `S15`, `S16`, `S17`,
`S18`, `S19`, `S20`, `S21`, `S22`, `S23`, `S24`, and `S25` against those frozen
staged gates.
That completion is corpus/validation-only and does not add broad deployment for
those bands; for `S01`, `S02`, `S03`, `S04`, `S05`, `S06`, `S07`, `S08`, `S09`, `S10`, `S11`, `S12`, `S13`, `S14`, `S15`, `S16`, `S17`, `S18`, `S19`, `S20`, `S21`, `S22`, `S23`, `S24`, and `S25` it
does not widen the existing narrow live admission slices.

S07 live runtime admission is closed for the narrow exact initiative-conversion
slice.
`StrategyProjectionCoverageContract` owns the row-specific burden
`initiative_conversion_route_requires_opening_development_regime_and_same_owner_development_comparison`,
`initiative_conversion_route_requires_same_owner_certified_initiative_window`,
`projection_evidence_must_bind_same_owner_board_and_initiative_conversion_route`,
and
`development_only_initiative_only_s08_denial_or_optional_strengthening_is_non_admitting`.
The live projection evidence kind is
`initiative_conversion_route_certified`. `OpeningDevelopmentRegime` is
support-only, and `DevelopmentComparison` plus certified `InitiativeWindow`
remain lower Certification truth owners. S07 is present in
`StrategyProjectionScopeContract.startReadyBandIds` and live
`requiredEvidenceKindsByBand`; `StrategyProjectionAdmission` admits only
board-anchored `development_led_window` / `move_right_window` evidence with
same-owner certified lower carriers, while S08 remains a separate live
rival-source denial branch.

S13 has a live runtime-admission slice for exact wing structural damage only.
The row-specific admission burden is frozen as
`wing_damage_route_requires_same_sector_asymmetry_and_same_anchor_contact_source`,
`wing_damage_route_excludes_center_sector_targets`,
`phalanx_edge_route_requires_recomputed_phalanx_edge_target`,
`phalanx_edge_route_excludes_chain_base_targets`,
`burdened_target_route_requires_recomputed_structurally_burdened_target`,
`projection_evidence_mirrors_owner_source_target_sector_and_damage_route`, and
`asymmetry_weak_pawn_adjacent_rival_or_optional_strengthening_is_non_admitting`.
The projection evidence kind is live as
`wing_damage_route_certified`, with helper / scaffold boundary tokens
`same_task_projection_evidence_must_mirror_s13_source_target_sector_and_route_law`
and `stale_or_adjacent_runtime_evidence_not_counted_before_live_admission`.
S13 is now in `StrategyProjectionScopeContract.startReadyBandIds`, has a
`requiredEvidenceKindsByBand` entry for `wing_damage_route_certified`, and
`StrategyProjectionAdmission` revalidates the current-board lower carriers
before admitting. Sector asymmetry alone, pre-existing weak-pawn support,
stale evidence, adjacent S11/S14/S15 rivals, and optional strengthening remain
non-admitting.

S14 has a live runtime-admission freeze. It is in
`StrategyProjectionScopeContract.startReadyBandIds`, has the
`StrategyProjectionScopeContract.requiredEvidenceKindsByBand` entry
`chain_base_contact_route_certified`, and `StrategyProjectionAdmission`
revalidates the exact current-board lower carrier before admitting. The
row-specific burden is frozen as
`chain_base_route_requires_same_anchor_lever_and_contact_source`,
`chain_base_route_requires_recomputed_non_center_chain_base_target`,
`base_contact_continuation_requires_same_source_target_pair`,
`projection_evidence_mirrors_owner_source_target_and_chain_base_route`, and
`fixed_chain_structural_shell_adjacent_rival_or_optional_strengthening_is_non_admitting`.
The live projection evidence kind is
`chain_base_contact_route_certified`. Helper / law tokens are
`same_anchor_chain_base_contact_law`,
`non_center_chain_base_role_recomputed_from_exact_board_law`,
`base_contact_must_bind_same_source_and_target_law`,
`same_task_projection_evidence_must_mirror_s14_source_target_and_route_law`, and
`fixed_chain_or_structural_damage_shell_is_not_truth_owner_law`. Lower
ownership stays with `Witness:available_lever_trigger(base_contact)`,
`Witness:pawn_push_break_contact_source(same_anchor_chain_base_target)`,
`ProjectionValidation:non_center_chain_base_target_recomputed|base_contact_continuation`,
and `SupportOnly:weak_pawn_target_state|structural_damage_shell|fixed_chain_context`.
Exact validation uses `base_contact_lever_and_break_contact_present`,
`contact_target_role_is_non_center_chain_base`,
`base_contact_continuation_same_anchor_present`,
`fixed_chain_or_structural_damage_shell_only_not_counted`, and
`stale_or_adjacent_runtime_evidence_not_counted_before_live_admission`.
S14 admission remains narrow: same-anchor base contact, non-center chain-base
target recomputation, and projection evidence that mirrors contact source,
target, route token, and exact chain-base forward support squares are required.
`base_contact_continuation` is a separate exact-board route: the target's
forward-supported pawn must itself support the next defender pawn, so a
`chain_base_target` carrier cannot be relabeled by evidence token alone.
Fixed-chain shells, weak-pawn support, optional strengthening, stale evidence,
adjacent S05/S11/S13/S15 rivals, and Object / Delta / Certification families
remain non-admitting.

The previous high-risk broad blocker set is closed at gate-decision level:

- `S06` restriction-route and same-cluster near-miss buckets are frozen, and
  initial matching coverage rows are present.
- S06 live runtime admission is now closed for the narrow exact-board slice:
  `space_bind_restriction_route_certified` is the required evidence kind,
  support-only `SpaceBindRestrictionCertification` must certify the same
  current-board space-bind route and expose the same `route_host_links` binding,
  exact S05 center-release and exact S20 domination false-rival rows remain
  non-admitting, and coverage rows remain countable without becoming broad
  runtime authority.
- `S10` is explicitly knight-only for current-branch broad scope; non-knight
  occupancy requires a later lower-support freeze.
- `S12` local access-route and same-cluster near-miss buckets are frozen, and
  initial matching coverage rows are present.
- S12 now has a narrow live runtime admission freeze:
  `local_access_superiority_route_certified` is the live projection evidence kind,
  `access_route_requires_same_anchor_weak_outpost_or_diagonal_lane`,
  `access_route_requires_support_linked_short_run_restriction_reinforcement`,
  `local_access_superiority_requires_same_owner_anchor_route_and_support`,
  `same_task_projection_evidence_must_mirror_s12_owner_anchor_route_and_support`,
  `s03_diagonal_attack_s10_outpost_s20_mobility_or_optional_strengthening_is_non_admitting`,
  and
  `wrong_owner_wrong_anchor_wrong_route_stale_or_support_only_evidence_is_non_admitting`
  are frozen as row burden for the live S12 branch.
  `CertificationSupportOnly:MobilityComparison(non_truth_owner)` remains support
  only, and
  `wrong_owner_wrong_anchor_wrong_route_stale_or_support_only_evidence_not_counted`
  is part of the exact validation scaffold.
- `S15` route-completeness is live as per-position disjunction but
  broad-coverage conjunction over `s13_wing_damage` and `s14_chain_base`, with
  same-candidate contact-source law, false-rival coverage against `S13`,
  `S14`, and `S16`, shortcut negatives for candidate-only, create-passer
  shell-only, existing-passer-only, and split-anchor creation routes, and a
  live projection evidence kind freeze named
  `passer_creation_route_certified`.
- `S16` suppression completeness is frozen as broad-coverage conjunction over
  `blockade_hold`, `restriction_hold`, and `non_losing_race`, with certified
  race or hold proof required before suppression can count. S16 now has a
  narrow live runtime branch for those exact same-enemy-passer routes.

These closures do not count as broad deployment by themselves.
`S06`, `S10`, `S12`, `S15`, and `S16` now have separate matching corpus rows.
`S15` and `S16` are completed wave-6 broad-ready coverage bands with narrow
live runtime admission branches. S16 admission remains limited to the exact
same-enemy-passer suppression routes named above.
`S09`, `S20`, and `S23` are staged into wave 1
alongside `S06`, `S10`, `S12`, and `S25`; `S09`, `S23`, and `S25` now have
matching coverage rows, while `S25` remains limited to
`rank_access_route = cross_wing_rank_switch` for current broad counting.

Executable start-ready projection bands in the current runtime scaffold:

- `S01`
- `S02`
- `S03`
- `S05`
- `S06`
- `S07`
- `S08`
- `S09`
- `S10`
- `S11`
- `S12`
- `S13`
- `S14`
- `S15`
- `S16`
- `S17`
- `S18`
- `S19`
- `S20`
- `S21`
- `S22`
- `S23`
- `S24`
- `S25`

Coverage-complete rows no longer remain in the executable coverage-only set.

`S04` now has a narrow live runtime-admission freeze. It is listed in
`StrategyProjectionScopeContract.startReadyBandIds` and
`StrategyProjectionScopeContract.requiredEvidenceKindsByBand`, while
`StrategyProjectionCoverageContract` freezes:

- row-specific burden:
  `king_shelter_breach_requires_same_defender_king_safety_shell`,
  `king_shelter_breach_requires_shell_payload_or_support_break_route`,
  `king_shelter_breach_requires_same_defender_certified_king_safety_edge`,
  `same_task_projection_evidence_must_mirror_s04_owner_defending_king_shell_anchor_breach_squares_and_route`,
  `s01_storm_s02_concentration_s03_diagonal_s24_tactic_or_optional_strengthening_is_non_admitting`,
  and
  `wrong_owner_wrong_defender_wrong_shell_wrong_route_stale_or_support_only_evidence_is_non_admitting`
- lower ownership:
  `ObjectSupportOnly:KingSafetyShell(non_truth_owner)`,
  `ProjectionValidation:shell_payload_breach|support_break_breach(same_defender_king)`,
  `CertificationSupportOnly:CertifiedKingSafetyEdge(non_truth_owner)`,
  and
  `ProjectionEvidence:king_shelter_breach_route_certified`;
  S04 positive broad rows declare `KingSafetyShell` as required defender lower
  object support, not optional strengthening, and
  `shell_payload_and_support_break_use_distinct_declared_support_burdens`
- runtime boundary:
  broad S04 rows remain countable, but only exact rows carrying
  `king_shelter_breach_route_certified` evidence are live runtime authority;
  stale / wrong-owner / wrong-defender / wrong-shell / wrong-route /
  support-only / optional-strengthening / adjacent S01/S02/S03/S24 rows remain
  fail-closed

`S02` now has a live runtime admission freeze. The frozen burden is exact
direct king-ring concentration:
`king_ring_concentration_requires_same_defending_king_attack_scaffold`,
`king_ring_concentration_requires_certified_same_owner_king_safety_edge`,
`same_task_projection_evidence_must_mirror_s02_owner_defending_king_ring_targets_source_set_and_route`,
`s03_diagonal_s04_shell_s09_file_penetration_or_optional_strengthening_is_non_admitting`,
and
`wrong_owner_wrong_king_wrong_targets_wrong_sources_wrong_route_stale_or_support_only_evidence_is_non_admitting`.
The live evidence name is `king_ring_concentration_route_certified`; it is
allowed only for S02 and must mirror the same defending king, source set,
king-ring target set, and route.

All non-`S01`/`S02`/`S03`/`S04`/`S05`/`S06`/`S07`/`S08`/`S09`/`S10`/`S11`/`S12`/`S13`/`S14`/`S15`/`S16`/`S17`/`S18`/`S19`/`S20`/`S21`/`S22`/`S23`/`S24`/`S25` S-bands remain outside
`StrategyProjectionAdmission` unless a later runtime boundary explicitly adds
them. The current closure proves the runtime start-ready scaffold only for
`S01`/`S02`/`S03`/`S04`/`S05`/`S06`/`S07`/`S08`/`S09`/`S10`/`S11`/`S12`/`S13`/`S14`/`S15`/`S16`/`S17`/`S18`/`S19`/`S20`/`S21`/`S22`/`S23`/`S24`/`S25` and proves staged
wave-1 through wave-6 JSONL coverage gates for `S01-S25`; it is not a branch-wide
live-admission green list.
All non-`S01`/`S02`/`S03`/`S04`/`S05`/`S06`/`S07`/`S08`/`S09`/`S10`/`S11`/`S12`/`S13`/`S14`/`S15`/`S16`/`S17`/`S18`/`S19`/`S20`/`S21`/`S22`/`S23`/`S24`/`S25` bands remain outside live projection admission.

Broad-coverage corpus debt for the staged wave-1 through wave-6 projection
clusters is closed. Any future debt is outside the current frozen broad-ready
gate set.

Detailed owner:

- [StrategyProjectionBoundaryMatrix.md](/C:/Codes/CondensedChess/lila-docker/repos/lila/modules/commentary/docs/StrategyProjectionBoundaryMatrix.md:205)
- [ValidationMethodology.md](/C:/Codes/CondensedChess/lila-docker/repos/lila/modules/commentary/docs/ValidationMethodology.md:533)

## Blocked Strategy Bands

Most strategy bands have a frozen boundary.

No S01/S02/S03/S04/S05/S06/S07/S08/S09/S10/S11/S12/S13/S14/S15/S16/S17/S18/S19/S20/S21/S22/S23/S24/S25 blocker remains at the start-ready handoff boundary.

This does not authorize broad deployment or runtime behavior outside those
explicit narrow admission slices. It means the missing admission companions and
exact validation scaffold are now frozen through
`StrategyProjectionAdmission` and `projection-expectations.jsonl`.
All non-`S01`/`S02`/`S03`/`S04`/`S05`/`S06`/`S07`/`S08`/`S09`/`S10`/`S11`/`S12`/`S13`/`S14`/`S15`/`S16`/`S17`/`S18`/`S19`/`S20`/`S21`/`S22`/`S23`/`S24`/`S25` S-bands remain outside
live runtime admission.
The non-`S01`/`S02`/`S03`/`S04`/`S05`/`S06`/`S07`/`S08`/`S09`/`S10`/`S11`/`S12`/`S13`/`S14`/`S15`/`S16`/`S17`/`S18`/`S19`/`S20`/`S21`/`S22`/`S23`/`S24`/`S25` bands
stay fail-closed.

Detailed owner:

- [StrategyProjectionBoundaryMatrix.md](/C:/Codes/CondensedChess/lila-docker/repos/lila/modules/commentary/docs/StrategyProjectionBoundaryMatrix.md:197)

## Live Strategy Support Seed Families

The branch tracks required seed families for projection bands whose lower
support needed explicit runtime contracts.

`S17`, `S23`, `S24`, and `S25` seeds are now live Witness-boundary support
seeds.

Detailed seed names, role splits, and negative boundaries are owned by
`StrategySupportSeedInventory.md`.

| Strategy band | Inventory owner |
| --- | --- |
| `S17` | `S17 Seed Family` section |
| `S23` | `S23 Seed Family` section |
| `S24` | `S24 Seed Family` section |
| `S25` | `S25 Seed Family` section |

Detailed owner:

- [StrategySupportSeedInventory.md](/C:/Codes/CondensedChess/lila-docker/repos/lila/modules/commentary/docs/StrategySupportSeedInventory.md:43)

## Blocked U-Primary Redesign Inventory

The branch records the historical redesign attempts for the former `7`
blocked `U-primary` rows, but their discharge path is now rehome rather than
forced exactification inside `U`.

Canonical discharge outcome:

- `opening-tempo` leaves `U` for object-side `OpeningDevelopmentRegime`
- `middlegame-positional` leaves `U` for object-side
  `DistributedContactRegime`
- `transition-liquidation` leaves `U` for delta-side
  `TradeCompressionCorridor`
- `endgame-race` leaves `U` for object-side `EndgameRaceScaffold`
- `closed center` leaves `U-primary` and survives only as `U-attached`
  `structural_space_claim` host vocabulary
- `fixed chain` leaves `U-primary` and survives only as `U-attached`
  `structural_space_claim` host vocabulary
- `central tension` leaves `U` for object-side `CentralContactFront`

Historical redesign candidates remain archived for reference only.

Detailed owner:

- [BlockedUPrimaryDiscriminatorInventory.md](/C:/Codes/CondensedChess/lila-docker/repos/lila/modules/commentary/docs/BlockedUPrimaryDiscriminatorInventory.md:1)

## U Implementation Start Gate

The current branch does **not** authorize partial `U` implementation from a
mixed ready/blocked state.

Branch rule:

- `U` implementation may begin only when the effective `U` blocker count is
  `0`
- an accepted redesign candidate is still a blocker if it remains non-live
- a provisional placeholder is a blocker
- a runtime-id-pending legacy witness is a blocker
- an attached row whose host/admission boundary is still unresolved is a
  blocker

Implication:

- the previously identified ready subset inside `U-primary` did **not** by
  itself authorize implementation start
- the user-level start criterion remains full blocker discharge across the `U`
  inventory, not partial readiness
- after the current rehome discharge, the effective `U` blocker count is
  treated as `0`

## U Broad-Validation Freeze

U broad validation is now frozen as a separate confidence contract from U
implementation start.

Current U broad scope:

- active `U-primary 18`
- active `U-attached 1`
  - `structural_space_claim`

Current U broad status:

- `broad-confidence-green`:
  - `available_lever_trigger`
  - `bishop_pair_state`
  - `diagonal_lane_only`
  - `duty_bound_defender`
  - `file_lane_state`
  - `fork`
  - `knight_on_outpost_square`
  - `loose_piece_target_state`
  - `overload`
  - `pawn_push_break_contact_source`
  - `passed_pawn_entity_state`
  - `pin`
  - `rook_on_open_file_state`
  - `sector_asymmetry_state`
  - `short_run_slider_gate_restriction`
  - `skewer`
  - `structural_space_claim`
  - `weak_outpost_square_state`
  - `weak_pawn_target_state`
- all live U descriptor R dependency gates are closed by the Root 25/25
  `broad-confidence-green` result
- no live U descriptor remains `thin` or root-blocked under the current
  descriptor-local broad corpus

The owner for the broad audit table is:

- `modules/commentary/src/test/scala/lila/commentary/witness/u/UBroadCoverageMatrix.scala`

The tracked snapshot is:

- `modules/commentary/src/test/resources/commentary-corpus/u-coverage-matrix.md`

The formal U corpus owner is:

- `modules/commentary/src/test/resources/commentary-corpus/witness-expectations.jsonl`

That corpus has `218` descriptor-local broad rows. Existing U rule tests
remain unit-level evidence only and do not promote any descriptor to U
`broad-confidence-green`. Formal rows must use active runtime `descriptorId`
values and satisfy descriptor-local counts plus frozen `coverageAxis` /
`coverageBucket` breadth tags in the U broad matrix; aggregate corpus size alone
is not a promotion signal.

Engine remains outside U truth. U may consume only deterministic root-state
facts and formulas over `R`; engine sanity belongs to root confound filtering or
Object/Certification validation.

## Upper-to-U Backflow Findings

The branch now explicitly records the upper-layer backflow findings that were
used to justify the rehome discharge.

These are not optional style notes.

They are non-regression guardrails for future `U` work:

- `opening-tempo`, `middlegame-positional`, and `endgame-race` must not
  re-enter `U` as raw phase witnesses
  - they were discharged because no honest exact lower witness contract
    survived
- `transition-liquidation` must not re-enter `U` through vague compression or
  liquidation wording
  - the rejected failure mode is any board-unclean replacement for the
    `compressed enough` gate
- `closed center` and `fixed chain` must remain host-shell vocabulary only
  - they must not be revived as standalone `U-primary` witnesses unless a new
    exact slice is frozen explicitly
- `central tension` must not be revived as a raw `U-primary` witness
  - continuity now lives in object-side `CentralContactFront`
  - object admission now stays on the frozen `central_sector_mask` and
    `front_connectivity` helpers rather than on raw posture wording

The practical implementation bans are:

- do not use `phase_gate` as `U` admission
- do not treat `contested_sectors` as a standalone lower witness
- do not let illustrative lower-support lists harden into required `U` bundles
- do not let strategy support seeds (`S17`, `S23`, `S24`) leak downward into `U`

## Object 7 Runtime Closure

The branch has now discharged the `Object 7` start gate with live runtime code
in `modules/commentary/src/main/scala/lila/commentary/strategic`.

The helper and admission laws below stay frozen, but they are no longer design
only. Current-worktree extraction is live and validated against the exact-board
object corpus.

Shared helper freeze:

- `sector_mask(sector, square)` follows the canonical file split already
  implied by `WitnessSector`
- `contact_square(square)` means a square is either currently `contested` or
  currently occupied and directly attacked by the opponent of the occupant
- `front_connectivity(square_a, square_b)` means the two squares lie in the
  same maximal orthogonally connected component of `contact_square` inside the
  chosen mask
- `central_sector_mask(square)` is the extended center band on files `c-f` and
  ranks `3-6`
- `king_theater_link(fragment, defending_king)` remains the canonical
  king-theater gate reused by `AttackScaffold`
- `KingSafetyShell` stays on `home_shelter_mask` plus its home-wing king proxy
  rather than the broader `king_theater_link`

Frozen object-side admission helpers:

- `opening_development_window`
- `distributed_contact_spread`
- `dual_run_endgame_trigger`
- `attack_host_core`
- `fortress_entry_denial_shell`
- `home_shelter_shell`
- `central_contact_front_state`

Runtime-closure rule:

- `Object 7` runtime must keep the frozen helper/admission contracts above
- `object-expectations.jsonl` must carry `exact`, `near_miss`, and
  `nasty_negative` rows for all seven `Object 7` families
- `engine-probe-expectations.jsonl` must keep a local Stockfish sanity row for
  every `Object 7` board; minimum burden is no mate within the configured
  short horizon, with optional eval bounds for calmness-sensitive rows
- object admission may not borrow `TradeInvariant`, `promotion_race`, or
  `certified_king_safety_edge` as direct proof of object presence
- object admission may not use upper-layer row labels as positive or negative
  guards; exclusion and admission must stay board-exact
- shell helpers must define king-centered masks and entry squares explicitly
- `opening_development_window` keeps home-minor reserve, home-rook reserve,
  and closed `d/e` files centralized in one helper
- `fortress_entry_denial_shell` treats same-file and adjacent-file attacker
  passers as denial pressure
- `home_shelter_shell` uses a home-wing king proxy on files `c` or `g` so the
  shell object stays off central or uncastled home-rank kings
  rather than through prose-only region wording
- central-contact runtime must choose one canonical qualifying component rather
  than merge disconnected fronts under one sector identity
- fortress file-pressure rejection must stay on live shell-entry geometry, not
  mere neighboring-file major-piece presence
- live extraction must stay aligned with
  `CommentaryCoreBoundaryTest`,
  `StrategicObject7RuleTest`,
  and
  `StrategicObjectCorpusRuntimeTest`

## Delta 2 Runtime Closure

`Delta 2` now has live runtime code on the current worktree. The branch has the
two delta rows registered together and the corridor row remains ordered before
invariant.

Frozen delta rows:

- `TradeCompressionCorridor`
- `TradeInvariant`

Shared boundary rules:

- both rows are `move_local` only on the first live slice
- both rows are `board`-anchored
- delta truth must be computed from exact before/after positions plus one board-coherent
  `playedMove`
- `CommentaryCore` now exposes delta extraction entrypoints for both object
  extraction input and before/after `Fen` plus `playedMove`
- fail-closed delta extraction overloads are live
- both families landed together

Frozen row-specific helper/law contracts:

- `TradeCompressionCorridor`
  - helper `reciprocal_exchange_corridor`
  - helper `compressed_trade_window`
  - helper `trade_compression_transition`
    - first live slice requires:
      - a board-coherent non-king capture on the played move
    - no queens on the after-board
    - at most `4` total non-king non-pawn pieces on the after-board
    - one canonical opposing non-king pair that currently attacks each other
      along one shared file or diagonal corridor on the after-board
    - the before-board failed either the corridor predicate or the compressed
      window
    - forbidden-rival rejection must follow the actual current-worktree
      `TradeInvariant` first slice, not raw `EndgameRaceScaffold`
      persistence by itself
- `TradeInvariant`
  - helper `bounded_material_reduction`
  - helper `persistent_object_carrier`
  - helper `trade_invariant_transition`
    - first live slice requires:
      - a board-coherent non-king capture on the played move
    - total non-king non-pawn material count drops by exactly `1`
    - one same-family same-anchor object persists from before-board to
      after-board
    - the mover-side clear-run carrier must stay continuous across the move:
      - either the same clear runner remains on the same square
      - or the moving pawn itself remains the clear runner on its destination
  - the current-worktree first live slice only admits
    `EndgameRaceScaffold` persistence on the `board` anchor with mover-side
    clear-run carrier continuity
  - `FortressHoldingShell`, `AttackScaffold`, and `KingSafetyShell`
    generalization remain deferred until separate delta corpus rows exist

Frozen validation scaffold:

- `delta-expectations.jsonl` must now carry:
  - `exact`
  - `near_miss`
  - `nasty_negative`
  - `move_local_false_witness`
  rows for both delta families
- every delta row must keep:
  - `fenBefore`
  - `playedMove`
  - `fenAfter`
  - `family`
  - `owner`
  - `scope`
  - `deltaTag`
  - `anchor`
  - `pressureTarget`
  - `helpers`
- `TradeCompressionCorridor` rows must additionally declare the canonical
  after-board corridor pair when one exists
- `TradeInvariant` rows must additionally declare the persistent carrier family
  and anchor for the first live slice
- every delta row must also declare its forbidden rival family
- `DeltaExpectationCorpusTest` now asserts live runtime extraction against the
  delta corpus rows and confirms the board-coherent move-transition contract

Live delta tests:

- `TradeCompressionCorridorRuleTest`
- `TradeInvariantRuleTest`
- `StrategicDeltaBoundaryTest`
- `DeltaExpectationCorpusTest`
- `CommentaryCoreBoundaryTest`

## Certification Boundary Freeze

Current-worktree certification status:

- docs/scaffold frozen
- live runtime package under
  `modules/commentary/src/main/scala/lila/commentary/certification`
- `CommentaryCore` now exposes `activeCertificationFamilyIds` plus typed and
  fail-closed certification extraction helpers because a fail-closed extractor
  now exists

Ownership stays frozen at `10` certification inventory rows mapped to `11`
runtime families:

- `DevelopmentComparison`
- `InitiativeWindow`
- `MobilityComparison`
- `ComparativeKingFragility`
- `CertifiedKingSafetyEdge`
- `MateNetCertification`
- `MaterialHarvest`
- `WinningEndgame`
- `FortressDrawCertification`
- `PerpetualCheckHolding`
- `PromotionRace`

Split-row mapping stays frozen as:

- `development lag` and `development lead` share `DevelopmentComparison`
- `king safety edge` splits into:
  - `ComparativeKingFragility`
  - `CertifiedKingSafetyEdge`
- `perpetual/fortress` splits into:
  - `FortressDrawCertification`
  - `PerpetualCheckHolding`

Certification runtime must consume only:

- `StrategicObjectExtraction`
- `StrategicDeltaExtraction`
- explicit certification evidence bundles, with
  `CertificationEvidenceBundle.empty` as the explicit unbound fail-closed
  sentinel and any non-empty bundle created by `forObjectExtraction` or
  `forDeltaExtraction` bound to the same current root state
- live certification extraction must reject any non-empty evidence bundle
  whose bound root state does not exactly match the current extraction

Live probe adapter status:

- `CertificationEngineEvidence.fromProbe(...)` remains fail-closed empty
- probe usage is currently validation-side scaffold only

Certification runtime may not:

- reopen root or witness admission
- create projection truth
- revive `SupportOnly` or `Deferred` rows into planner or renderer truth

## Certification Verdict Freeze

The certification verdict lattice is frozen to:

- `Certified`
- `SupportOnly`
- `Deferred`
- `Rejected`

Meaning:

- `SupportOnly` is a real endpoint
- `Deferred` is a fail-closed endpoint
- neither may be revived by planner, projection, or wording

## Certification First-Live Slice Freeze

The current-worktree first live certification slices are intentionally narrow:

- `DevelopmentComparison`:
  - `OpeningDevelopmentRegime`-backed comparative development superiority only
- `InitiativeWindow`:
  - development-led initiative only
- `MobilityComparison`:
  - restriction-backed comparative mobility only
- `ComparativeKingFragility`:
  - home-wing king-theater asymmetry only
- `CertifiedKingSafetyEdge`:
  - `AttackScaffold` plus comparative king fragility plus host/budget/
    move-order/best-defense burden
- `MateNetCertification`:
  - forcing mate-net certification only
- `MaterialHarvest`:
  - realized non-king material conversion only
- `WinningEndgame`:
  - certified conversion/result verdict only, currently narrowed to a single
    non-rook-pawn runner with owner king support, owner to move, and no rival
    pawn counterplay
- `FortressDrawCertification`:
  - `FortressHoldingShell`-backed hold certification only, with the draw
    burden still carried by explicit best-defense evidence rather than shell
    presence alone, and with the validation corpus kept inside explicit
    drawish `maxAbsCp` budgets
- `PerpetualCheckHolding`:
  - stable perpetual-check hold only
- `PromotionRace`:
  - kings-and-pawns-only clear-run promotion-race certification on top of
    `EndgameRaceScaffold`, using tempo plus rival-king-distance burden

These rows must reject their broad rivals:

- `DevelopmentComparison` may not certify from opening regime or phase wording
  alone
- `InitiativeWindow` may not certify from `DevelopmentComparison`,
  `AttackScaffold`, or counterplay wording alone
- `MobilityComparison` may not certify from restriction or bad-piece wording
  alone
- `ComparativeKingFragility` may not certify from `KingSafetyShell`, hole count,
  or generic attack wording alone
- `CertifiedKingSafetyEdge` may not certify from `AttackScaffold`,
  comparative fragility, or phase proxy alone
- `MateNetCertification` may not certify from attack or mate-threat wording
  alone
- `MaterialHarvest` may not certify from `material gain` shell wording,
  tactical smell, or result wording alone
- `WinningEndgame` may not certify from `TradeInvariant`,
  `FortressHoldingShell`, or material-edge wording alone
- `FortressDrawCertification` may not certify from `FortressHoldingShell`,
  `TradeInvariant`, or draw wording alone
- `PerpetualCheckHolding` may not certify from checking-sequence wording,
  `AttackScaffold`, or draw wording alone
- `PromotionRace` may not certify from `EndgameRaceScaffold`,
  `PasserComplex`, or `ConversionFunnel` wording alone

## Certification Validation Scaffold Freeze

- `certification-expectations.jsonl` is now live as a row-local certification
  scaffold corpus
- every certification family must carry:
  - `exact`
  - `near_miss`
  - `nasty_negative`
  - `best_defense_breaks_claim`
  rows
- every certification row must declare:
  - `family`
  - `owner`
  - `scope`
  - `anchor`
  - `burdenTag`
  - `helpers`
  - `requiredSupportFamilies` when the first live slice depends on lower family
    support
  - `engineRequirement`
  - `enginePurposes`
  - `forbiddenShortcuts`
- certification-side support-family presence is satisfied only by a live
  non-`Rejected` claim of the required family for the same owner polarity
- every engine-required certification row must have a matching
  `engine-probe-expectations.jsonl` row with the same `id`
- `CertificationExpectationCorpusTest` remains the current-worktree scaffold
  test
- live certification extraction now exists under the canonical certification
  package; corpus validation must stay separate from runtime-boundary and
  explicit-evidence-bundle validation

## Engine Boundary Freeze

Engine/probe may later certify:

- survival
- best defense
- comparative burden
- conversion viability

Engine/probe may not:

- create root atoms
- create witnesses
- serve as support seeds
- admit strategy bands by itself

Detailed owner:

- [CommentaryCoreSSOT.md](/C:/Codes/CondensedChess/lila-docker/repos/lila/modules/commentary/docs/CommentaryCoreSSOT.md:740)

## External Consumption Evidence

The current worktree now carries external-consumer evidence beyond commentary's
own witness tests.

- public boundary:
  [CommentaryCore.scala](/C:/Codes/CondensedChess/lila-docker/repos/lila/modules/commentary/src/main/scala/lila/commentary/CommentaryCore.scala)
- tracked external-consumer artifact:
  [CommentaryCoreBoundaryTest.scala](/C:/Codes/CondensedChess/lila-docker/repos/lila/modules/commentary/src/test/scala/lila/commentary/CommentaryCoreBoundaryTest.scala)
- verification ledger:
  [ExternalConsumptionAuditEvidence.md](/C:/Codes/CondensedChess/lila-docker/repos/lila/modules/commentary/docs/ExternalConsumptionAuditEvidence.md)

This discharges the earlier gap where `U-primary 18` had only internal
commentary-module test evidence.

The same public-boundary evidence now covers the one live `U-attached`
`structural_space_claim` contract and the live `Delta 2` pair
`TradeCompressionCorridor` / `TradeInvariant`.

It does **not** upgrade the shell-only attached `10` rows to public-boundary
runtime.

- [ValidationMethodology.md](/C:/Codes/CondensedChess/lila-docker/repos/lila/modules/commentary/docs/ValidationMethodology.md:394)

## Work Sequencing Freeze

The branch sequencing is now:

1. freeze root, witness, ownership, and strategy boundaries
2. finish `U` work without reopening projection semantics
3. implement future lower/support seeds for blocked strategy bands
4. only then allow formerly blocked `S` bands to become start-ready projection
   implementation targets

This means `S` is currently frozen mainly as boundary and dependency inventory,
not as a live implementation target.

## Projection Coverage Broad-Ready Boundary

The initiative / release / counterplay blocker closure for `S05`, `S07`,
`S08`, and `S21` is now broad-ready for coverage; `S05`, `S07`, `S08`, and `S21`
also have narrow live runtime admission.

It freezes:

- row-specific burden gates
- shared helper laws
- lower-carrier ownership
- exact validation scaffold counting over authored JSONL rows
- fail-closed separation from live `StrategyProjectionAdmission` for any
  non-live coverage-only row
- S07 live evidence kind `initiative_conversion_route_certified` listed in live
  `requiredEvidenceKindsByBand`
- S21 live evidence kind `counterplay_survival_route_certified` listed in live
  `requiredEvidenceKindsByBand`

S05 now has an additional live-admission freeze:

- row-specific admission burden requires same-anchor lever and contact source,
  a recomputed center-pawn target, same source/target central-axis
  continuation, projection evidence mirroring the same center-release route, and
  fail-closed rejection for open-center wording, center shell, support objects,
  optional strengthening, or adjacent rivals
- shared helper law requires same-source / same-target / same-task evidence
  mirroring without promoting lower support into projection truth
- lower-carrier ownership keeps `CentralContactFront`, `InitiativeWindow`, and
  `file_lane_state` support-only
- exact validation scaffold includes wrong-source, wrong-target, wrong-axis,
  stale-evidence, and adjacent-evidence rejection families
- the live evidence kind is `center_release_route_certified`, and it is listed
  in `StrategyProjectionScopeContract.requiredEvidenceKindsByBand`

It authors broad-ready JSONL rows and adds the bands to the broad-ready
coverage-completion gate. S05, S07, S08, and S21 additionally have live runtime
projection admission branches. The executable owners are
`StrategyProjectionCoverageContract` and `StrategyProjectionAdmission`, with
parity tests in `StrategyProjectionCoverageContractTest`,
`StrategyProjectionAdmissionTest`, and `ProjectionExpectationCorpusTest`.

S07 additionally has a live runtime admission branch:

- row-specific admission burden requires same-owner certified
  `DevelopmentComparison`, same-owner certified board-anchored
  `InitiativeWindow`, non-empty initiative support targets, and projection
  evidence mirroring the exact `development_led_window` or `move_right_window`
  route
- shared helper law requires same-owner / same-board / same-task evidence
  mirroring without promoting `OpeningDevelopmentRegime`,
  `DevelopmentComparison`, or `InitiativeWindow` into projection truth
- lower-carrier ownership keeps `OpeningDevelopmentRegime` support-only and
  `DevelopmentComparison` plus certified `InitiativeWindow` as lower
  Certification truth owners
- exact validation scaffold includes development-only, initiative-only,
  wording-only, wrong-route, stale-evidence, and adjacent S08 denial-rival
  rejection families
- runtime boundary remains fail-closed: S07 admits only exact
  `initiative_conversion_route_certified` evidence on the current board; S08
  rival-source denial remains a separate live branch with a distinct evidence
  kind and route task

S21 additionally has a live runtime admission branch:

- row-specific admission burden requires exact owner
  `pawn_push_break_contact_source`, same-board certified `InitiativeWindow`,
  later projection evidence mirroring the same source, target, route, and
  certification, and fail-closed rejection for source-only, certified
  `InitiativeWindow`-only, deferred initiative, or adjacent S01/S05/S08 rivals
- shared helper law requires same-source / same-target / same-task evidence
  mirroring without promoting the lower `InitiativeWindow` certification into
  projection truth
- lower-carrier ownership keeps `DevelopmentComparison`,
  `InitiativeWindow`, `available_lever_trigger`, theater shells,
  `AttackScaffold`, and `EndgameRaceScaffold` below projection truth ownership
- exact validation scaffold names wrong-source, wrong-target, wrong-route,
  stale evidence, certified-initiative-only, and adjacent-rival rejection
  families for the live slice
- runtime boundary remains fail-closed: S21 admits only exact
  `counterplay_survival_route_certified` evidence with same-board certified
  `InitiativeWindow`; source-only, initiative-only, optional strengthening, and
  adjacent S01/S05/S08 rivals reject

## Not Yet Frozen As Live Runtime

The following are intentionally not yet live runtime contracts:

- all coverage-complete-only projection bands outside the narrow live
  `S01` / `S02` / `S03` / `S04` / `S05` / `S06` / `S07` / `S08` / `S09` / `S10` / `S11` / `S12` / `S13` / `S14` / `S15` / `S16` / `S17` / `S18` / `S19` / `S20` / `S21` / `S22` / `S23` / `S24` / `S25` start-ready slices
- broad projection carrier corpora
- planner ranking between competing strategy bands
- renderer wording policy beyond current projection boundaries

`S01`, `S02`, `S03`, `S04`, `S05`, `S06`, `S07`, `S08`, `S09`, `S10`, `S11`, `S12`, `S13`, `S14`, `S15`, `S16`, `S17`, `S18`, `S19`, `S20`, `S21`, `S22`, `S23`, `S24`, and `S25` keep their existing
narrow live admission contracts only. The broad-ready coverage freeze for any
of those bands must not be read as a wider live projection extraction or
carrier runtime contract.

S08 now has a live runtime admission branch:

- row-specific burden names exact same-board `InitiativeWindow` support, exact
  rival `pawn_push_break_contact_source`, a current-board rival release
  reserve, projection evidence mirroring the same rival source, target, and
  denial route, and rejection of
  `InitiativeWindow` alone, S07 initiative conversion, S21 source survival,
  S20 domination/restriction, no-counterplay wording, stale evidence, wrong
  source, wrong target, or wrong route
- shared helper law requires same rival source / same target / same denial
  task; `InitiativeWindow`, `MobilityComparison`, and
  `short_run_slider_gate_restriction` remain support, not S-layer truth owners
- lower-carrier ownership keeps `pawn_push_break_contact_source` as witness
  support and `InitiativeWindow` as lower Certification support; the live
  projection evidence kind is `counterplay_denial_route_certified`
- exact validation scaffold keeps current coverage rows countable while
  naming wrong-source, wrong-target, wrong-route, stale-evidence,
  adjacent-rival, and wording-only fail-closed boundaries
- runtime boundary remains fail-closed: S08 admits only exact
  `counterplay_denial_route_certified` evidence with same-board
  `InitiativeWindow`; wrong-source/wrong-target/wrong-route/stale-evidence
  rejection remains explicit, and S07/S20/S21 rivals reject

S16 now has a narrow live runtime-admission freeze and branch. It has left the
coverage-only set only for exact same-enemy-passer suppression routes:

- row-specific burden names:
  - `suppression_route_requires_same_enemy_passed_pawn_anchor`
  - `blockade_route_requires_owner_blocker_on_enemy_passer_front_square`
  - `restriction_hold_route_requires_same_enemy_passer_blocker_owner_restriction_and_certified_hold`
  - `race_route_requires_enemy_passer_and_same_board_non_losing_race_certification`
  - `projection_evidence_mirrors_enemy_passer_route_and_route_specific_proof`
  - `enemy_passer_blocker_restriction_hold_race_or_adjacent_rival_alone_is_non_admitting`
- live projection evidence-kind name:
  - `passer_suppression_route_certified`
- runtime boundary: S16 is present in
  `StrategyProjectionScopeContract.startReadyBandIds`, its only
  `StrategyProjectionScopeContract.requiredEvidenceKindsByBand` entry is
  `passer_suppression_route_certified`, and live `StrategyProjectionAdmission`
  must still fail-closed rather than admit from enemy-passer, blocker,
  restriction, hold, race, S15, S22, or S23 support alone
- ownership boundary: enemy `passed_pawn_entity_state` remains the exact lower
  entity carrier; route proof is projection-validation only; lower
  `PromotionRace`, `FortressDrawCertification`, `PerpetualCheckHolding`,
  `FortressHoldingShell`, and `EndgameRaceScaffold` remain lower
  Certification/Object truth owners or support and cannot become S16 projection
  truth owners
- live route boundary:
  - `blockade_hold` requires an owner blocker on the enemy passer's front
    square plus certified `FortressDrawCertification` whose support targets
    the same passer/blocker shell
  - `restriction_hold` requires the same enemy passer/blocker relation, the
    blocker square exposed in an owner `short_run_slider_gate_restriction`
    gate-square payload on the same current board, and certified
    `PerpetualCheckHolding`
  - `non_losing_race` requires certified `PromotionRace` whose support targets
    include the same enemy passed-pawn square
  - the evidence payload must mirror the same `passer_square`,
    `suppression_route`, route-specific `blocker_square` when applicable, and
    `certification_family`

S11 now has narrow live runtime admission for exact same-target weak-pawn
pressure only. It is present in `StrategyProjectionScopeContract.startReadyBandIds`,
has a single `StrategyProjectionScopeContract.requiredEvidenceKindsByBand`
entry, and is revalidated in `StrategyProjectionAdmission`:

- row-specific burden names:
  - `target_pressure_route_requires_same_square_weak_pawn_target_and_owner_pressure`
  - `fixation_route_requires_current_fixed_pawn_persistence_on_same_target`
  - `repeated_pressure_route_requires_two_owner_attackers_on_same_target`
  - `projection_evidence_mirrors_weak_pawn_target_pressure_and_persistence`
  - `weak_pawn_only_target_swap_adjacent_rival_or_optional_strengthening_is_non_admitting`
- live evidence kind:
  - `weak_pawn_target_pressure_persistence_certified`
- law / scaffold additions:
  - `fixed_pawn_persistence_is_current_board_carrier_not_certification_truth_law`
  - `same_task_projection_evidence_must_mirror_s11_target_pressure_and_persistence_law`
  - `pressure_without_fixed_persistence_not_counted`
- ownership boundary:
  `weak_pawn_target_state` remains witness truth; same-target pressure and
  fixed-pawn persistence are projection-validation carriers; S11 has no support
  seed, Object, Delta, or Certification truth-owner carrier. `MobilityComparison`
  and `InitiativeWindow` remain support-only optional strengthening.

S18 now has narrow live runtime admission for exact current-board minor
conversion only:

- row-specific burden names:
  - `initiative_route_requires_bishop_pair_state_and_same_board_initiative_window`
  - `structure_route_requires_bishop_pair_state_and_same_board_mobility_comparison`
  - `material_route_requires_bishop_pair_member_material_harvest`
  - `projection_evidence_mirrors_conversion_family_targets_and_bishop_pair`
  - `relation_only_optional_strengthening_or_adjacent_rival_is_non_admitting`
- frozen live projection evidence-kind names:
  - `bishop_pair_initiative_conversion_certified`
  - `bishop_pair_structure_conversion_certified`
  - `bishop_pair_material_conversion_certified`
- helper and exact-scaffold names include
  `same_current_board_bishop_pair_conversion_law`,
  `s18_projection_evidence_same_task_same_target_law`,
  `conversion_certification_is_lower_truth_owner_law`,
  `active_bishop_pair_member_present`,
  `same_current_board_conversion_certification_present`,
  `projection_evidence_mirrors_conversion_family_targets_and_bishop_pair`,
  `material_conversion_bishop_pair_member_capture_present`,
  `initiative_or_structure_conversion_support_targets_present`, and
  `bishop_pair_minor_edge_or_optional_strengthening_only_not_counted`
- runtime boundary: S18 is inside
  `StrategyProjectionScopeContract.startReadyBandIds`, its
  `StrategyProjectionScopeContract.requiredEvidenceKindsByBand` entry is the
  three evidence kinds above, and live `StrategyProjectionAdmission` must keep
  rejecting shortcut, adjacent-rival, stale, or lower-carrier-only bundles
- ownership boundary: `bishop_pair_state` is same-owner current-board witness
  substrate with at least one active bishop-pair member; S18 has no support seed,
  object, or delta carrier; lower
  `InitiativeWindow`, `MobilityComparison`, and `MaterialHarvest`
  certification carriers remain truth owners rather than projection-owned
  facts; `WinningEndgame` is support-only future carrier because current lower
  law cannot certify it on a board that still has bishops

S19 now has narrow live runtime admission. The admission remains delta-backed
and route-specific:

- row-specific burden names:
  - `material_route_requires_canonical_trade_invariant_and_same_move_material_harvest`
  - `hold_route_requires_canonical_trade_invariant_and_post_trade_fortress_certification`
  - `result_only_material_only_or_adjacent_rival_is_non_admitting`
- live projection evidence-kind names:
  - `trade_invariant_material_simplification_certified`
  - `trade_invariant_hold_simplification_certified`
- runtime boundary: S19 is in
  `StrategyProjectionScopeContract.startReadyBandIds`, has exactly the two
  `StrategyProjectionScopeContract.requiredEvidenceKindsByBand` entries above,
  and `StrategyProjectionAdmission` rejects missing, stale, or shortcut lower
  carrier bundles
- adjacent boundary: S22 and S24 keep their independent live runtime evidence
  kinds and do not inherit S19 simplification evidence; S19 promotion must not
  move any coverage-only band into live admission
- ownership boundary: `TradeInvariant`, `MaterialHarvest`,
  `FortressDrawCertification`, and `FortressHoldingShell` remain lower
  Object/Delta/Certification truth owners; projection evidence is an admission
  companion and must not synthesize lower truth

S09 now has a narrow live runtime admission branch. It is listed in
`StrategyProjectionScopeContract.startReadyBandIds` and
`StrategyProjectionScopeContract.requiredEvidenceKindsByBand` only for exact
same-file penetration routes:

- live evidence kind:
  - `file_penetration_route_certified`
- admission burden names:
  - `file_penetration_route_requires_owner_file_lane_state`
  - `file_penetration_route_requires_same_file_entry_or_penetration_consequence`
  - `same_task_projection_evidence_must_mirror_s09_owner_file_source_entry_and_route`
  - `s02_attack_s23_king_activity_s25_rank_access_or_optional_strengthening_is_non_admitting`
  - `wrong_file_wrong_source_wrong_entry_wrong_route_stale_or_support_only_evidence_is_non_admitting`
- helper and scaffold names include
  `same_task_projection_evidence_must_mirror_s09_owner_file_source_entry_and_route_law`
  and
  `wrong_file_wrong_source_wrong_entry_wrong_route_stale_or_support_only_evidence_not_counted`
- ownership boundary:
  `ProjectionEvidence:file_penetration_route_certified`,
  `file_lane_state`, and `rook_on_open_file_state` are not projection truth
  owners for file penetration by themselves; `MaterialHarvest`,
  `WinningEndgame`, `AttackScaffold`, and `CertifiedKingSafetyEdge` remain
  support-only
- runtime boundary: broad S09 rows remain countable, but only exact
  `file_penetration_route_certified` evidence rows are live admission
  authority; `S02`, `S23`, and `S25` adjacent rivals retain independent
  meanings, `S03` keeps its own live diagonal king-attack branch, `S04` keeps
  its own live shelter-breach branch, and `S02` uses its own live king-ring
  concentration branch
  fail-closed rows

S01 now has a narrow live runtime admission branch. It is listed in
`StrategyProjectionScopeContract.startReadyBandIds` and
`StrategyProjectionScopeContract.requiredEvidenceKindsByBand` only for exact
current-board king-wing storm routes:

- live evidence kind:
  - `king_wing_storm_route_certified`
- admission burden names:
  - `king_wing_storm_route_requires_same_anchor_lever_and_contact_source`
  - `king_wing_storm_route_requires_non_center_owner_wing_contact_source_and_target`
  - `king_wing_storm_route_requires_same_defending_king_attack_scaffold_and_certified_edge`
  - `same_task_projection_evidence_must_mirror_s01_source_target_defending_king_and_route`
  - `s05_center_release_s21_counterplay_castling_shell_or_optional_strengthening_is_non_admitting`
  - `wrong_source_wrong_target_wrong_king_wrong_route_stale_or_support_only_evidence_is_non_admitting`
- helper and scaffold names include
  `same_wing_contact_plus_attack_edge_law`,
  `same_defending_king_safety_edge_law`,
  `same_task_projection_evidence_must_mirror_s01_source_target_defending_king_and_route_law`,
  `s05_center_release_or_s21_counterplay_survival_is_not_s01_storm_law`,
  `castling_context_is_not_storm_proof_law`, and
  `wrong_source_wrong_target_wrong_king_wrong_route_stale_or_support_only_evidence_not_counted`
- ownership boundary:
  `ProjectionEvidence:king_wing_storm_route_certified`,
  `AttackScaffold`, and `CertifiedKingSafetyEdge` are not projection truth
  owners by themselves; lower object/certification support must be revalidated
  on the exact current board
- runtime boundary: broad S01 rows remain countable, but only exact
  `king_wing_storm_route_certified` evidence rows are live admission
  authority; adjacent `S03`, `S05`, and `S21` rows retain independent meanings,
  and `S04` keeps its separate exact `king_shelter_breach_route_certified`
  branch

S03 now has a narrow live runtime-admission branch. It is listed in
`StrategyProjectionScopeContract.startReadyBandIds` and
`StrategyProjectionScopeContract.requiredEvidenceKindsByBand` only for exact
current-board diagonal king-attack routes:

- live evidence kind:
  - `diagonal_king_attack_route_certified`
- row-specific admission burden names:
  - `diagonal_king_attack_requires_king_theater_diagonal_lane`
  - `diagonal_king_attack_requires_same_defending_king_attack_scaffold`
  - `diagonal_king_attack_requires_comparative_fragility_and_certified_edge`
  - `same_task_projection_evidence_must_mirror_s03_owner_defending_king_diagonal_source_entry_squares_and_route`
  - `s02_concentration_s12_local_access_bishop_pair_or_non_king_diagonal_is_non_admitting`
  - `attack_scaffold_certification_diagonal_only_stale_or_wrong_task_evidence_is_non_admitting`
- runtime boundary names:
  - stale, wrong-owner, wrong-king, wrong-source, wrong-route, support-only,
    object-only, certification-only, bishop-pair-only, S02-concentration, and
    S12-local-access rows reject unless exact S03 evidence mirrors the current
    board carrier
- ownership boundary:
  `ObjectSupportOnly:AttackScaffold(non_truth_owner)` and
  `CertificationSupportOnly:ComparativeKingFragility|CertifiedKingSafetyEdge(non_truth_owner)`
  remain lower support, not projection truth owners.

Executable start-ready projection bands are now `S01`, `S02`, `S03`, `S04`, `S05`, `S06`, `S07`,
`S08`, `S09`, `S10`, `S11`, `S12`, `S13`, `S14`, `S15`, `S16`, `S17`, `S18`,
`S19`, `S20`, `S21`, `S22`, `S23`, `S24`, and `S25`.
`S01`/`S02`/`S03`/`S04`/`S05`/`S06`/`S07`/`S08`/`S09`/`S10`/`S11`/`S12`/`S13`/`S14`/`S15`/`S16`/`S17`/`S18`/`S19`/`S20`/`S21`/`S22`/`S23`/`S24`/`S25`.
All non-`S01`/`S02`/`S03`/`S04`/`S05`/`S06`/`S07`/`S08`/`S09`/`S10`/`S11`/`S12`/`S13`/`S14`/`S15`/`S16`/`S17`/`S18`/`S19`/`S20`/`S21`/`S22`/`S23`/`S24`/`S25` S-bands remain outside
live projection admission.

## How To Use This Ledger

- Use this file first when you need the current decision summary.
- Use the linked detailed docs for exact contract text.
- If a future change contradicts this ledger, update the detailed owner doc and
  this ledger in the same change.
