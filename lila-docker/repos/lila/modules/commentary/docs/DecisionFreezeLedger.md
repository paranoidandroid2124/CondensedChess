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

It does **not** mean live projection runtime already exists.

It authorizes narrow first-slice projection work plus corpus authoring only.

It does **not** yet prove broader deployment.

No coverage-complete-only projection band is broad-deployed or live as runtime
projection on the current branch. The staged wave-1 through wave-6 set `S01`, `S02`, `S03`,
`S04`, `S05`, `S06`, `S07`, `S08`, `S09`, `S10`, `S11`, `S12`, `S13`, `S14`,
`S15`, `S16`, `S17`, `S18`, `S19`, `S20`, `S21`, `S22`, `S23`, `S24`, and
`S25` is coverage-complete only under the current JSONL/test broad-coverage
gates. `S17`, `S23`, `S24`, and `S25` retain only their already-authored narrow
live admission slices.

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
That completion is corpus/validation-only and does not add live runtime
projection admission or broad deployment for those bands; for `S17`, `S23`,
`S24`, and `S25` it does not widen the existing narrow live admission slices.

The previous high-risk broad blocker set is closed at gate-decision level:

- `S06` restriction-route and same-cluster near-miss buckets are frozen, and
  initial matching coverage rows are present.
- `S10` is explicitly knight-only for current-branch broad scope; non-knight
  occupancy requires a later lower-support freeze.
- `S12` local access-route and same-cluster near-miss buckets are frozen, and
  initial matching coverage rows are present.
- `S15` route-completeness is frozen as per-position disjunction but
  broad-coverage conjunction over `s13_wing_damage` and `s14_chain_base`, with
  false-rival coverage against `S13`, `S14`, and `S16`.
- `S16` suppression completeness is frozen as broad-coverage conjunction over
  `blockade_hold`, `restriction_hold`, and `non_losing_race`, with certified
  race or hold proof required before suppression can count.

These closures do not count as live admission by themselves.
`S06`, `S10`, `S12`, `S15`, and `S16` now have separate matching corpus rows.
`S15` and `S16` are completed wave-6 broad-ready coverage-only bands, not live
runtime admission bands. `S09`, `S20`, and `S23` are staged into wave 1
alongside `S06`, `S10`, `S12`, and `S25`; `S09`, `S23`, and `S25` now have
matching coverage rows, while `S25` remains limited to
`rank_access_route = cross_wing_rank_switch` for current broad counting.

Executable start-ready projection bands in the current runtime scaffold:

- `S17`
- `S23`
- `S24`
- `S25`

Coverage-complete but still fail-closed for live projection admission:

- all non-`S17`/`S23`/`S24`/`S25` `S01-S25` bands, including:
- `S01`
- `S02`
- `S03`
- `S04`
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
- `S18`
- `S19`
- `S20`
- `S21`
- `S15`
- `S16`
- `S22`

All non-`S17`/`S23`/`S24`/`S25` S-bands remain outside
`StrategyProjectionAdmission` unless a later runtime boundary explicitly adds
them. The current closure proves the runtime start-ready scaffold only for
`S17` / `S23` / `S24` / `S25` and proves staged wave-1 through wave-6 JSONL
coverage gates for `S01-S25`; it is not a branch-wide live-admission green
list.

Broad-coverage corpus debt for the staged wave-1 through wave-6 projection
clusters is closed. Any future debt is outside the current frozen broad-ready
gate set.

Detailed owner:

- [StrategyProjectionBoundaryMatrix.md](/C:/Codes/CondensedChess/lila-docker/repos/lila/modules/commentary/docs/StrategyProjectionBoundaryMatrix.md:205)
- [ValidationMethodology.md](/C:/Codes/CondensedChess/lila-docker/repos/lila/modules/commentary/docs/ValidationMethodology.md:533)

## Blocked Strategy Bands

Most strategy bands have a frozen boundary.

No S17/S23/S24/S25 blocker remains at the start-ready handoff boundary.

This does not mean live projection runtime exists. It means the missing
admission companions and exact validation scaffold are now frozen through
`StrategyProjectionAdmission` and `projection-expectations.jsonl`.

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
`S08`, and `S21` is now a broad-ready coverage closure only.

It freezes:

- row-specific burden gates
- shared helper laws
- lower-carrier ownership
- exact validation scaffold counting over authored JSONL rows
- fail-closed separation from live `StrategyProjectionAdmission`

It authors broad-ready JSONL rows and adds the bands to the broad-ready
coverage-completion gate, but it does not add live runtime projection admission
for these bands. The executable owner is
`StrategyProjectionCoverageContract`, with parity tests in
`StrategyProjectionCoverageContractTest` and `ProjectionExpectationCorpusTest`.

## Not Yet Frozen As Live Runtime

The following are intentionally not yet live runtime contracts:

- `S05`, `S07`, `S08`, and `S21` projection admission
- all coverage-complete-only projection bands outside the narrow live
  `S17` / `S23` / `S24` / `S25` start-ready slices
- broad projection carrier corpora
- planner ranking between competing strategy bands
- renderer wording policy beyond current projection boundaries

`S17`, `S23`, `S24`, and `S25` keep their existing narrow live admission
contracts only. The broad-ready coverage freeze for any of those bands must not
be read as a wider live projection extraction or carrier runtime contract.

## How To Use This Ledger

- Use this file first when you need the current decision summary.
- Use the linked detailed docs for exact contract text.
- If a future change contradicts this ledger, update the detailed owner doc and
  this ledger in the same change.
