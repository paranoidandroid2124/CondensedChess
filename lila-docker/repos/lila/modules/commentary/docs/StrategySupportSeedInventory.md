# Strategy Support Seed Inventory

This document freezes the future lower/support seed families that must exist
before selected strategy bands may be released as live projections.

It does **not** introduce a new semantic layer.

Every row below is intended to land inside the existing
`Witness -> Object -> Delta -> Certification -> Projection` staircase.

The purpose is to prevent post-`U` work from improvising missing lower
contracts at the moment of projection release.

## Current Scope

The current branch freezes missing seed families only for the blocked strategy
bands:

- `S23 king activation / opposition / penetration`
- `S24 tactical conversion of a prepared target`

## Global Rules

- A support seed is not a renderer phrase, planner hint, or engine score.
- A support seed must carry an exact anchor, local meaning, and negative
  boundary.
- Engine/probe may later certify whether the seed-bearing plan survives best
  defense, but engine/probe does not create the seed.
- Where a row below is marked `future Witness fragment`, it is a lower support
  contract below the `61` inventory, similar in status to
  `king_file_diagonal_entry_axis`.

## S23 Seed Family

`S23` is blocked today because the branch has no exact lower contract for king
activity itself.

Existing rows such as `endgame-race`, `whole-board`, `file_lane_state`, or
`winning_endgame` are related context only.

They do not identify a king route, king-entry square, or opposition relation.

### Selected Future Seeds

| Seed family | Intended owner layer | Anchor | Standalone | Local meaning | Negative boundary | Future consumers |
| --- | --- | --- | --- | --- | --- | --- |
| `king_entry_square_seed` | future `Witness` fragment | `square` | yes | a concrete square whose occupation by the beneficiary king yields an admitted endgame penetration or decisive approach point | any generic central square; `whole-board` scope only; rook/file penetration not involving the king | `S23`, future king-activity object, endgame entry certification |
| `king_access_route_seed` | future `Witness` fragment | `king-square -> entry-square route` | no; host-scoped to `king_entry_square_seed` | the beneficiary king has an exact traversable route from its current square to an admitted `king_entry_square_seed` | hand-wavy king activity; blocked route; route requiring non-admitted tactical cleanup; lane access owned by pieces rather than the king | `S23`, future endgame entry certification |
| `king_opposition_contact_seed` | future `Witness` fragment | `king-square pair` | yes | the kings stand in an exact opposition/contact relation that creates or preserves endgame access, restraint, or zugzwang pressure | generic king proximity; mere adjacency; broad endgame advantage wording; any claim that opposition alone already proves winning | `S23`, future opposition/endgame certification |

### Why These Three

- `king_entry_square_seed` gives `S23` a destination truth.
- `king_access_route_seed` gives `S23` a route truth.
- `king_opposition_contact_seed` gives `S23` the king-vs-king relation truth
  that cannot be reduced to route or lane language.

`S23` should later admit from either:

- `king_opposition_contact_seed` plus endgame certification, or
- `king_entry_square_seed` plus `king_access_route_seed` plus entry or
  conversion certification.

## S24 Seed Family

`S24` is blocked today because the branch has no exact lower contract for a
prepared target.

Existing rows such as `pin`, `fork`, `skewer`, `overload`,
`material_harvest`, or `exchange defender` are related but insufficient.

They either describe the eventual tactic/result, or they remain broad surface
labels with no distinct exact discriminator.

### Selected Future Seeds

| Seed family | Intended owner layer | Anchor | Standalone | Local meaning | Negative boundary | Future consumers |
| --- | --- | --- | --- | --- | --- | --- |
| `target_resource_dependency_seed` | future `Witness` fragment | `target piece-square` or `target square` | yes | a concrete target resource depends on a narrow supporting defender, blocker, or protection link whose failure would expose the target | generic weak piece wording; `duty_bound_defender` alone; `exchange square` alone; `defended resource` alone | `S24`, future defender-dependency object, tactical conversion certification |
| `target_attack_convergence_seed` | future `Witness` fragment | `target resource + beneficiary attack footprint` | no; host-scoped to `target_resource_dependency_seed` | the current board already contains a concrete beneficiary-side attack geometry centered on the named target, without yet claiming defender failure, tactic family, or harvest result | any exact tactic alone; generic pressure or tactical smell; geometry not tied to the named target; dependency wording alone; result wording such as `material_harvest` or `mate net`; unsupported “prepared target” phrasing | `S24`, future prepared-target object, tactical conversion certification |

### Why These Two

- `target_resource_dependency_seed` identifies what is actually prepared.
- `target_attack_convergence_seed` identifies the target-centered attack
  geometry that can later be realized tactically
  before the tactic is played.

`S24` should later admit only from:

- `target_resource_dependency_seed`
- `target_attack_convergence_seed`
- an admitted exact tactic or forcing family
- conversion certification such as `material_harvest` or `mate_net_certification`

## Existing Nearby Rows That Are Not Enough

| Existing row / family | Why it is not the missing seed |
| --- | --- |
| `endgame-race` | phase/race trigger only; not king activity truth |
| `whole-board` | theater shell only |
| `TradeInvariant` | delta/result-side simplification lane, not target preparation |
| `DefenderDependencyNetwork` | upper family name, not a frozen lower contract |
| `pin` / `fork` / `skewer` / `overload` | tactic means only, not prepared-target truth |
| `material_harvest` / `winning_endgame` / `mate_net_certification` | result/certification, not lower seed |

## Freeze Meaning

These seed families are frozen as required future contracts.

They are **not** active runtime contracts yet.

Nothing in this file authorizes projection release before the future lower
contracts and their corpora are actually built.
