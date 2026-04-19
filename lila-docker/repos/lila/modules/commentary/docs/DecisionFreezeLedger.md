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

It is a separate side evidence channel consumed only at:

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

## Strategy Boundary Freeze

`S01-S24` is frozen as a projection-band vocabulary, not as a truth-owning
layer.

The projection freeze means:

- each `Sxx` must remain semantically separable from rival bands
- each `Sxx` must be constructible from lower certified carriers
- no strategy band may admit from renderer wording, planner choice, or legacy
  label alone

Detailed owner:

- [StrategyProjectionBoundaryMatrix.md](/C:/Codes/CondensedChess/lila-docker/repos/lila/modules/commentary/docs/StrategyProjectionBoundaryMatrix.md:1)
- [ValidationMethodology.md](/C:/Codes/CondensedChess/lila-docker/repos/lila/modules/commentary/docs/ValidationMethodology.md:149)

## Blocked Strategy Bands

Most strategy bands have a frozen boundary.

Two remain blocked for live admission:

| Band | Current status | Why blocked |
| --- | --- | --- |
| `S23 king activation / opposition / penetration` | blocked for live admission | missing exact lower king-activity support seeds |
| `S24 tactical conversion of a prepared target` | blocked for live admission | missing exact lower prepared-target support seeds |

Detailed owner:

- [StrategyProjectionBoundaryMatrix.md](/C:/Codes/CondensedChess/lila-docker/repos/lila/modules/commentary/docs/StrategyProjectionBoundaryMatrix.md:56)

## Frozen Future Seed Families

The branch now freezes the required future seed families for the blocked
strategy bands.

These are frozen as required future contracts, not as active runtime contracts.

| Strategy band | Frozen future seeds |
| --- | --- |
| `S23` | `king_entry_square_seed`, `king_access_route_seed`, `king_opposition_contact_seed` |
| `S24` | `target_resource_dependency_seed`, `target_attack_convergence_seed` |

The intended role split is:

- `S23`
  - destination truth
  - route truth
  - king-vs-king relation truth
- `S24`
  - prepared-resource dependency truth
  - target-centered attack-convergence truth

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
  - object admission remains deferred until canonical `central-sector mask`
    and `front-connectivity` helpers are frozen

The practical implementation bans are:

- do not use `phase_gate` as `U` admission
- do not treat `contested_sectors` as a standalone lower witness
- do not let illustrative lower-support lists harden into required `U` bundles
- do not let blocked strategy support seeds (`S23`, `S24`) leak downward into
  `U`

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

- [CommentaryCoreSSOT.md](/C:/Codes/CondensedChess/lila-docker/repos/lila/modules/commentary/docs/CommentaryCoreSSOT.md:690)
- [ValidationMethodology.md](/C:/Codes/CondensedChess/lila-docker/repos/lila/modules/commentary/docs/ValidationMethodology.md:122)

## Work Sequencing Freeze

The branch sequencing is now:

1. freeze root, witness, ownership, and strategy boundaries
2. finish `U` work without reopening projection semantics
3. implement future lower/support seeds for blocked strategy bands
4. only then allow blocked `S` bands to become live projections

This means `S` is currently frozen mainly as boundary and dependency inventory,
not as a live implementation target.

## Not Yet Frozen As Live Runtime

The following are intentionally not yet live runtime contracts:

- `S23` live projection admission
- `S24` live projection admission
- the future seed families listed above
- full projection carrier corpora
- planner ranking between competing strategy bands
- renderer wording policy beyond current projection boundaries

## How To Use This Ledger

- Use this file first when you need the current decision summary.
- Use the linked detailed docs for exact contract text.
- If a future change contradicts this ledger, update the detailed owner doc and
  this ledger in the same change.
