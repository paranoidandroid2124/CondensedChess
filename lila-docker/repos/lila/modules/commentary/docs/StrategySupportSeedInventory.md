# Strategy Support Seed Inventory

This document freezes the future lower/support seed families that must exist
before selected strategy bands may be released as live projections.

It does **not** introduce a new semantic layer.

Every row below is intended to land inside the existing
`Witness -> Object -> Delta -> Certification -> Projection` staircase.

The purpose is to prevent post-`U` work from improvising missing lower
contracts at the moment of projection release.

## Current Scope

The current branch freezes missing seed families for the remaining
non-start-ready or blocked strategy bands:

- `S17 worst-piece improvement / bad-piece exchange`
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
  the reviewed but non-standalone king-theater access composition.

## S17 Seed Family

`S17` is still not start-ready today because the branch has no exact lower
contract for same-piece liability repair or same-piece liability-relief
exchange.

Existing rows such as `loose_piece_target_state`, `duty_bound_defender`,
`weak_outpost_square_state`, `pinned_piece`, and `trapped_piece` identify
liability only.

Existing rows such as `TradeInvariant`, `MobilityComparison`,
`bishop_pair_state`, or `exchange defender` are related context only.

They do not identify the same piece, the same-piece relieving route, or the
same-piece exchange that removes that liability.

### Selected Future Seeds

| Seed family | Intended owner layer | Anchor | Standalone | Local meaning | Negative boundary | Future consumers |
| --- | --- | --- | --- | --- | --- | --- |
| `same_piece_liability_anchor_seed` | future `Witness` fragment | `piece-square` | yes | one concrete beneficiary minor piece is fixed as the exact liability carrier, with its liability bundle drawn from current exact support on that same piece | generic `bad piece` wording; liability spread across multiple pieces; bishop-pair or mobility-edge meaning; any claim that the anchored piece can change by prose | `S17`, future minor-piece-liability object |
| `same_piece_repair_route_seed` | future `Witness` fragment | `piece-square -> relieving-square route` | no; host-scoped to `same_piece_liability_anchor_seed` | the same anchored piece has an exact route to a relieving square or function that removes or materially relieves the anchored liability without relying on generic redeployment or access wording | route on a different piece; generic redeployment/access meaning; route requiring non-admitted tactical cleanup; `MobilityComparison` alone; arrival square that leaves the same liability intact | `S17`, future liability-relief certification |
| `same_piece_exchange_relief_seed` | future `Witness` fragment | `piece-square + exact exchange contact` | no; host-scoped to `same_piece_liability_anchor_seed` | the same anchored piece can be exchanged on exact contact terms that remove the anchored liability without collapsing into broad simplification, tactical conversion, or harvest wording | any exchange not involving the anchored piece; `TradeInvariant` alone; `exchange defender` alone; `material_harvest` alone; exact tactic family alone | `S17`, future liability-relief certification |

### Why These Three

- `same_piece_liability_anchor_seed` fixes which minor piece is actually the
  worst-piece carrier.
- `same_piece_repair_route_seed` gives `S17` exact same-piece repair truth.
- `same_piece_exchange_relief_seed` gives `S17` exact same-piece exchange-relief
  truth that cannot be reduced to simplification or tactics.

`S17` should later admit only from either:

- `same_piece_liability_anchor_seed` plus
  `same_piece_repair_route_seed`, or
- `same_piece_liability_anchor_seed` plus
  `same_piece_exchange_relief_seed`

In both cases the projection corpus must still keep `S18` and `S20` out on
exact rival rows.

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
| `target_resource_dependency_seed` | future `Witness` fragment | `target piece-square` or `target square` | yes | a concrete target resource depends on a narrow supporting defender, blocker, or protection link whose failure would expose the target | generic weak piece wording; `duty_bound_defender` alone; `exchange square` alone; `defended resource` alone | `S24`, future defender-dependency object, future prepared-target package |
| `target_attack_convergence_seed` | future `Witness` fragment | `target resource + beneficiary attack footprint` | no; host-scoped to `target_resource_dependency_seed` | the current board already contains a concrete beneficiary-side attack geometry centered on the named target, without yet claiming defender failure, tactic family, or harvest result | any exact tactic alone; generic pressure or tactical smell; geometry not tied to the named target; dependency wording alone; result wording such as `material_harvest` or `mate net`; unsupported “prepared target” phrasing | `S24`, future prepared-target object, future prepared-target package |

### Why These Two

- `target_resource_dependency_seed` identifies what is actually prepared.
- `target_attack_convergence_seed` identifies the target-centered attack
  geometry that can later be realized tactically
  before the tactic is played.

The lower prepared-target package should later freeze only from:

- `target_resource_dependency_seed`
- `target_attack_convergence_seed`

Any later `S24` projection realization would then need, outside that lower
package:

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
